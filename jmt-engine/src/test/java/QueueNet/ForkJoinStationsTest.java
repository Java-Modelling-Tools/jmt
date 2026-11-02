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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import NetStrategies.ForkStrategies.OutPath;
import NetStrategies.ForkStrategies.ProbabilitiesFork;
import NetStrategies.ForkStrategy;
import NetStrategies.JoinStrategies.NormalJoin;
import NetStrategies.JoinStrategies.PartialJoin;
import NetStrategies.JoinStrategy;
import NetStrategies.QueueGetStrategies.FCFSstrategy;
import NetStrategies.QueuePutStrategies.TailStrategy;
import NetStrategies.QueuePutStrategy;
import NetStrategies.RoutingStrategies.RandomStrategy;
import NetStrategies.RoutingStrategy;
import NetStrategies.ServiceStrategy;
import NodeSections.Fork;
import NodeSections.Join;
import NodeSections.Queue;
import NodeSections.Router;
import NodeSections.Server;
import NodeSections.ServiceTunnel;
import jmt.common.ErrorReporters;
import random.EmpiricalEntry;

/** A fork into two branches of different speed, merged by a join, wired by hand. */
class ForkJoinStationsTest {
	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private static Queue queue() throws Exception {
		return new Queue(-1, new String[] { Queue.FINITE_DROP }, new FCFSstrategy(), new QueuePutStrategy[] { new TailStrategy() });
	}

	private static Server server(double t) throws Exception {
		return new Server(1, new Integer[] { 1 }, new ServiceStrategy[] { new StrategyFixtures.ScriptedService(t) });
	}

	private static Router router() {
		return new Router(new RoutingStrategy[] { new RandomStrategy() });
	}

	private NetFixtures.Mini m;
	private RecordingSink out;
	private NetNode fork;
	private NetNode join;

	private void wire(JoinStrategy joinStrategy) throws Exception {
		m = new NetFixtures.Mini(0);
		OutPath toA = new OutPath(new EmpiricalEntry("a", 1.0), new EmpiricalEntry[] { new EmpiricalEntry("1", 1.0) });
		OutPath toB = new OutPath(new EmpiricalEntry("b", 1.0), new EmpiricalEntry[] { new EmpiricalEntry("1", 1.0) });
		ProbabilitiesFork strategy = new ProbabilitiesFork(new OutPath[] { toA, toB });
		strategy.setRandomEngine(m.system.getEngine());
		fork = m.station("fork", queue(), new ServiceTunnel(), new Fork(1, -1, true, new ForkStrategy[] { strategy }));
		NetNode a = m.station("a", queue(), server(2.0), router());
		NetNode b = m.station("b", queue(), server(5.0), router());
		join = m.station("join", new Join(new JoinStrategy[] { joinStrategy }), new ServiceTunnel(), router());
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, fork);
		m.connect(fork, a);
		m.connect(fork, b);
		m.connect(a, join);
		m.connect(b, join);
		m.connect(join, sink);
	}

	@Test
	void normalJoinWaitsForBothBranches() throws Exception {
		wire(new NormalJoin(-1));
		m.inject(fork, 0, 0.0);
		m.run(10000);
		assertEquals(1, out.ids.size());
		assertEquals(5.0, out.times.get(0), 1e-9);
	}

	@Test
	void partialJoinReleasesAfterFirstBranch() throws Exception {
		wire(new PartialJoin(1));
		m.inject(fork, 0, 0.0);
		m.run(10000);
		assertEquals(1, out.ids.size());
		assertEquals(2.0, out.times.get(0), 1e-9);
	}

	@Test
	void severalForkedJobsAreJoinedIndependently() throws Exception {
		wire(new NormalJoin(-1));
		m.inject(fork, 0, 0.0);
		m.inject(fork, 0, 1.0);
		m.run(10000);
		assertEquals(2, out.ids.size());
		assertTrue(out.times.get(1) > out.times.get(0));
	}

	@Test
	void forkJoinResponseTimeIsMeasured() throws Exception {
		wire(new NormalJoin(-1));
		RecordingMeasure fj = m.measure(join, SimConstants.LIST_RESPONSE_TIME, 0);
		m.inject(fork, 0, 0.0);
		m.run(10000);
		assertEquals(1, out.ids.size());
		assertNotNull(fj);
	}

	private void wireSemaphore(int threshold) throws Exception {
		m = new NetFixtures.Mini(0);
		OutPath toA = new OutPath(new EmpiricalEntry("a", 1.0), new EmpiricalEntry[] { new EmpiricalEntry("1", 1.0) });
		OutPath toB = new OutPath(new EmpiricalEntry("b", 1.0), new EmpiricalEntry[] { new EmpiricalEntry("1", 1.0) });
		ProbabilitiesFork strategy = new ProbabilitiesFork(new OutPath[] { toA, toB });
		strategy.setRandomEngine(m.system.getEngine());
		fork = m.station("fork", queue(), new ServiceTunnel(), new Fork(1, -1, true, new ForkStrategy[] { strategy }));
		NetNode a = m.station("a", queue(), server(2.0), router());
		NetNode b = m.station("b", queue(), server(5.0), router());
		NetNode sem = m.station("sem", new NodeSections.Semaphore(new NetStrategies.SemaphoreStrategy[] {
				new NetStrategies.SemaphoreStrategies.NormalSemaphore(threshold) }), new ServiceTunnel(), router());
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, fork);
		m.connect(fork, a);
		m.connect(fork, b);
		m.connect(a, sem);
		m.connect(b, sem);
		m.connect(sem, sink);
	}

	@Test
	void semaphoreReleasesAllForkedTasksOnceThresholdIsReached() throws Exception {
		wireSemaphore(2);
		m.inject(fork, 0, 0.0);
		m.run(10000);
		assertEquals(2, out.ids.size());
		assertEquals(5.0, out.times.get(0), 1e-9, "held until the slower task arrives");
		assertEquals(5.0, out.times.get(1), 1e-9);
	}

	@Test
	void semaphoreWithThresholdOneLetsTasksThroughImmediately() throws Exception {
		wireSemaphore(1);
		m.inject(fork, 0, 0.0);
		m.run(10000);
		assertEquals(2, out.ids.size());
		assertEquals(2.0, out.times.get(0), 1e-9);
		assertEquals(5.0, out.times.get(1), 1e-9);
	}
}
