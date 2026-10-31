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

import NetStrategies.QueueGetStrategies.FCFSstrategy;
import NetStrategies.QueuePutStrategies.TailStrategy;
import NetStrategies.QueuePutStrategy;
import NetStrategies.RoutingStrategies.RandomStrategy;
import NetStrategies.RoutingStrategy;
import NetStrategies.ServiceStrategy;
import NodeSections.Queue;
import NodeSections.RandomSource;
import NodeSections.Router;
import NodeSections.Server;
import NodeSections.ServiceTunnel;
import jmt.common.ErrorReporters;
import jmt.common.exception.NetException;

/** NetSystem and NetController driven through NetSystem.start on a tiny hand-wired network. */
class NetSystemTest {
	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private NetFixtures.Mini m;
	private RecordingSink out;
	private NetNode st;

	private void wire() throws Exception {
		m = new NetFixtures.Mini(0);
		st = m.station("st", new Queue(-1, new String[] { Queue.FINITE_DROP }, new FCFSstrategy(), new QueuePutStrategy[] { new TailStrategy() }),
				new Server(1, new Integer[] { 1 }, new ServiceStrategy[] { new StrategyFixtures.ScriptedService(2.0) }),
				new Router(new RoutingStrategy[] { new RandomStrategy() }));
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
	}

	@Test
	void startRunsAFiniteNetworkToCompletion() throws Exception {
		wire();
		m.inject(st, 0, 0.0);
		m.inject(st, 0, 1.0);
		m.startSystem();
		assertEquals(2, out.ids.size());
		assertEquals(2.0, out.times.get(0), 1e-9);
		assertEquals(4.0, out.times.get(1), 1e-9);
		assertEquals(QueueNetwork.STATE_FINAL, m.network.getState(), "a network whose controller has ended reports FINAL");
	}

	@Test
	void nodesCanBeLookedUpByNameAndId() throws Exception {
		wire();
		assertSame(st, m.system.getNode("st"));
		assertSame(st, m.system.getNode(st.getId()));
		assertNull(m.system.getNode("nobody"));
		assertSame(m.system.getSimSystem(), st.getNetSystem().getSimSystem());
	}

	@Test
	void jobNumbersAreSequential() throws Exception {
		wire();
		int a = m.system.nextjobNumber();
		assertEquals(a + 1, m.system.nextjobNumber());
	}

	@Test
	void progressOfMissingNetworkIsRejected() throws Exception {
		wire();
		NetException e = assertThrows(NetException.class, () -> m.system.checkProgress(null));
		assertEquals("Cannot measure progress of a network which does not exist.", e.getMessage());
		NetException e2 = assertThrows(NetException.class, () -> m.system.getTempMeasures(null));
		assertEquals("Cannot get measures of a network which does not exist.", e2.getMessage());
	}

	@Test
	void progressIsOneWhenAllMeasuresFinishedAndFractionOtherwise() throws Exception {
		wire();
		assertTrue(m.system.checkProgress(m.network) <= 1.0, "progress is a fraction");
		assertEquals(0.0, m.system.getTempMeasures(m.network), 1e-12, "no measure has finished yet");
		m.network.abortAllMeasures();
		assertEquals(1.0, m.system.getTempMeasures(m.network), 1e-12);
	}

	@Test
	void maxSimulatedTimeStopsAnEndlessSource() throws Exception {
		m = new NetFixtures.Mini(0);
		NetNode src = m.station("src", new RandomSource(new ServiceStrategy[] { new StrategyFixtures.ScriptedService(1.0) }), new ServiceTunnel(),
				new Router(new RoutingStrategy[] { new RandomStrategy() }));
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(src, sink);
		m.system.setMaxSimulatedTime(5.0);
		m.startSystem();
		assertTrue(out.ids.size() >= 4 && out.ids.size() <= 8, "stopped soon after t=5, got " + out.ids.size());
		assertTrue(m.network.getMeasures().get(0).hasFinished() || m.network.getMeasures().get(0).hasBeenAborted());
	}
}
