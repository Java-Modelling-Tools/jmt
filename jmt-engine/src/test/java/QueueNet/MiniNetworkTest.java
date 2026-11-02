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
import NetStrategies.RoutingStrategies.RandomStrategy;
import NetStrategies.RoutingStrategy;
import NetStrategies.ServiceStrategy;
import NodeSections.JobSink;
import NodeSections.Queue;
import NodeSections.Router;
import NodeSections.Server;
import NodeSections.ServiceTunnel;
import jmt.common.ErrorReporters;

/** Single stations wired by hand and stepped directly: Queue -> Server -> Router -> Sink. */
class MiniNetworkTest {
	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private static String[] drops(int n) {
		String[] d = new String[n];
		java.util.Arrays.fill(d, Queue.FINITE_DROP);
		return d;
	}

	@Test
	void jobsFlowThroughQueueServerRouterToSink() throws Exception {
		NetFixtures.Mini m = new NetFixtures.Mini(0);
		NetNode st = m.station("st", new Queue(-1, drops(1), new FCFSstrategy(), null),
				new Server(1, new Integer[] { 1 }, new ServiceStrategy[] { new StrategyFixtures.ScriptedService(2.0) }),
				new Router(new RoutingStrategy[] { new RandomStrategy() }));
		NetNode sink = m.station("sink", new JobSink(), null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
		for (int i = 0; i < 3; i++) {
			m.inject(st, 0, 0.1 * i);
		}
		m.run(10000);
		assertEquals(6.0, m.clock(), 1e-9);
		assertEquals(0, st.getJobInfoList().size());
		assertEquals(3, st.getJobInfoList().getJobsOut());
		assertEquals(3, sink.getJobInfoList().getJobsIn());
	}
}
