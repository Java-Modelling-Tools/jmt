/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package QueueNet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dataAnalysis.SimParameters;
import log.LoggerParameters;
import NetStrategies.QueueGetStrategies.FCFSstrategy;
import NetStrategies.QueuePutStrategies.TailStrategy;
import NetStrategies.QueuePutStrategy;
import NetStrategies.RoutingStrategies.RandomStrategy;
import NetStrategies.RoutingStrategy;
import NetStrategies.ServiceStrategy;
import NodeSections.ClassSwitch;
import NodeSections.LogTunnel;
import NodeSections.Queue;
import NodeSections.RandomSource;
import NodeSections.Router;
import NodeSections.Server;
import NodeSections.ServiceTunnel;
import NodeSections.Terminal;
import jmt.common.ErrorReporters;

/** Job generators (open source, closed terminal), class switching and the logger tunnel. */
class SourcesAndSwitchTest {
	@TempDir
	Path dir;

	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private static RoutingStrategy[] random(int n) {
		RoutingStrategy[] r = new RoutingStrategy[n];
		for (int i = 0; i < n; i++) {
			r[i] = new RandomStrategy();
		}
		return r;
	}

	private static ServiceStrategy[] svc(int n, double t) {
		ServiceStrategy[] s = new ServiceStrategy[n];
		for (int i = 0; i < n; i++) {
			s[i] = new StrategyFixtures.ScriptedService(t);
		}
		return s;
	}

	private static Queue queue(int n) throws Exception {
		String[] d = new String[n];
		java.util.Arrays.fill(d, Queue.FINITE_DROP);
		QueuePutStrategy[] p = new QueuePutStrategy[n];
		for (int i = 0; i < n; i++) {
			p[i] = new TailStrategy();
		}
		return new Queue(-1, d, new FCFSstrategy(), p);
	}

	@Test
	void randomSourceEmitsOneJobPerServiceTime() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(0);
		NetNode src = m.station("src", new RandomSource(svc(1, 2.0)), new ServiceTunnel(), new Router(random(1)));
		RecordingSink out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(src, sink);
		m.run(40);
		assertTrue(out.ids.size() >= 3, "got " + out.ids.size());
		assertEquals(2.0, out.times.get(0), 1e-9);
		assertEquals(4.0, out.times.get(1), 1e-9);
		assertEquals(6.0, out.times.get(2), 1e-9);
	}

	@Test
	void terminalCyclesItsClosedPopulationThroughTheServer() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(new int[] { 0 }, new int[] { JobClass.CLOSED_CLASS });
		NetNode term = m.station("term", new Terminal(new int[] { 3 }), new ServiceTunnel(), new Router(random(1)));
		NetNode st = m.station("st", queue(1), new Server(1, new Integer[] { 1 }, svc(1, 1.0)), new Router(random(1)));
		m.connect(term, st);
		m.connect(st, term);
		m.run(300);
		assertTrue(st.getJobInfoList().getJobsIn() > 3, "jobs came back from the terminal: " + st.getJobInfoList().getJobsIn());
		assertEquals(st.getJobInfoList().getJobsOut() + st.getJobInfoList().size(), st.getJobInfoList().getJobsIn());
	}

	@Test
	void terminalOfIntegerArrayBehavesTheSame() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(new int[] { 0 }, new int[] { JobClass.CLOSED_CLASS });
		NetNode term = m.station("term", new Terminal(new Integer[] { 2 }), new ServiceTunnel(), new Router(random(1)));
		NetNode st = m.station("st", queue(1), new Server(1, new Integer[] { 1 }, svc(1, 1.0)), new Router(random(1)));
		m.connect(term, st);
		m.connect(st, term);
		m.run(200);
		assertTrue(st.getJobInfoList().getJobsIn() > 2);
	}

	@Test
	void closedJobsCannotLeaveToASinkAndNothingIsDelivered() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(new int[] { 0 }, new int[] { JobClass.CLOSED_CLASS });
		NetNode term = m.station("term", new Terminal(new int[] { 1 }), new ServiceTunnel(), new Router(random(1)));
		RecordingSink out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(term, sink);
		m.run(1000);
		assertTrue(out.ids.isEmpty());
	}

	@Test
	void classSwitchMovesJobsToTheTargetClass() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(0, 0);
		Object[] matrix = new Object[] { new Float[] { 0f, 1f }, new Float[] { 0f, 1f } };
		NetNode st = m.station("st", queue(2), new ClassSwitch(matrix), new Router(random(2)));
		RecordingSink out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
		m.inject(st, 0, 0.0);
		m.inject(st, 1, 1.0);
		m.run(1000);
		assertEquals(2, out.ids.size());
		assertEquals(java.util.Arrays.asList(1, 1), out.classIds);
	}

	@Test
	void classSwitchWithIdentityMatrixKeepsClasses() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(0, 0);
		Object[] matrix = new Object[] { new Float[] { 1f, 0f }, new Float[] { 0f, 1f } };
		NetNode st = m.station("st", queue(2), new ClassSwitch(matrix), new Router(random(2)));
		RecordingSink out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
		m.inject(st, 0, 0.0);
		m.inject(st, 1, 1.0);
		m.run(1000);
		assertEquals(java.util.Arrays.asList(0, 1), out.classIds);
	}

	@Test
	void multiClassServerServesEachClassWithItsOwnTime() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(0, 0);
		ServiceStrategy[] s = new ServiceStrategy[] { new StrategyFixtures.ScriptedService(1.0), new StrategyFixtures.ScriptedService(4.0) };
		NetNode st = m.station("st", queue(2), new Server(1, new Integer[] { 1, 1 }, s), new Router(random(2)));
		RecordingSink out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
		m.inject(st, 0, 0.0);
		m.inject(st, 1, 10.0);
		m.run(1000);
		assertEquals(1.0, out.times.get(0), 1e-9);
		assertEquals(14.0, out.times.get(1), 1e-9);
	}

	@Test
	void loggerTunnelWritesAHeaderAndOneRowPerJob() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(0);
		LogTunnel log = new LogTunnel("log.csv", dir.toString(), true, true, true, true, true, true, true, 1);
		NetNode st = m.station("logger", queue(1), log, new Router(random(1)));
		RecordingSink out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
		SimParameters sp = new SimParameters();
		sp.setLogPath(dir.toString());
		sp.setLogDelimiter(",");
		sp.setLogDecimalSeparator(".");
		sp.setLogReplaceMode(String.valueOf(LoggerParameters.LOGGER_AR_REPLACE));
		sp.setTimestampValue("t0");
		st.setSimParameters(sp);
		m.inject(st, 0, 0.0);
		m.inject(st, 0, 1.0);
		m.run(1000);
		m.stop();
		assertEquals(2, out.ids.size());
		File csv = dir.resolve("log.csv").toFile();
		assertTrue(csv.exists());
		assertTrue(Files.readAllLines(csv.toPath()).size() >= 3);
	}
}
