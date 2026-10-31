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
import NetStrategies.QueueGetStrategies.LCFSstrategy;
import NetStrategies.QueueGetStrategy;
import NetStrategies.QueuePutStrategies.TailStrategy;
import NetStrategies.RoutingStrategies.RandomStrategy;
import NetStrategies.RoutingStrategies.RoundRobinStrategy;
import NetStrategies.RoutingStrategy;
import NetStrategies.ServiceStrategy;
import NodeSections.Delay;
import NodeSections.PSServer;
import NodeSections.Queue;
import NodeSections.Router;
import NodeSections.Server;
import jmt.common.ErrorReporters;

/** One station at a time, wired by hand (NetFixtures.Mini) and stepped deterministically. */
class StationsTest {
	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private static String[] drops(int n, String policy) {
		String[] d = new String[n];
		java.util.Arrays.fill(d, policy);
		return d;
	}

	private static ServiceStrategy[] svc(int n, double t) {
		ServiceStrategy[] s = new ServiceStrategy[n];
		for (int i = 0; i < n; i++) {
			s[i] = new StrategyFixtures.ScriptedService(t);
		}
		return s;
	}

	private static RoutingStrategy[] random(int n) {
		RoutingStrategy[] r = new RoutingStrategy[n];
		for (int i = 0; i < n; i++) {
			r[i] = new RandomStrategy();
		}
		return r;
	}

	private static Queue queue(int classes, int capacity, String policy, QueueGetStrategy get) throws Exception {
		return new Queue(capacity, drops(classes, policy), get, putTails(classes));
	}

	private static NetStrategies.QueuePutStrategy[] putTails(int n) {
		NetStrategies.QueuePutStrategy[] p = new NetStrategies.QueuePutStrategy[n];
		for (int i = 0; i < n; i++) {
			p[i] = new TailStrategy();
		}
		return p;
	}

	private NetFixtures.Mini m;
	private RecordingSink out;
	private NetNode st;

	private void wire(Queue q, NodeSections.ServiceSection service, int classes) throws Exception {
		m = new NetFixtures.Mini(new int[classes]);
		st = m.station("st", q, service, new Router(random(classes)));
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
	}

	@Test
	void fcfsServesInArrivalOrder() throws Exception {
		wire(queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy()), new Server(1, new Integer[] { 1 }, svc(1, 10.0)), 1);
		Job a = m.inject(st, 0, 0.0), b = m.inject(st, 0, 1.0), c = m.inject(st, 0, 2.0);
		m.run(10000);
		assertEquals(java.util.Arrays.asList(a.getId(), b.getId(), c.getId()), out.ids);
		assertEquals(10.0, out.times.get(0), 1e-9);
		assertEquals(30.0, out.times.get(2), 1e-9);
	}

	@Test
	void lcfsServesNewestWaitingJobFirst() throws Exception {
		wire(queue(1, -1, Queue.FINITE_DROP, new LCFSstrategy()), new Server(1, new Integer[] { 1 }, svc(1, 10.0)), 1);
		Job a = m.inject(st, 0, 0.0), b = m.inject(st, 0, 1.0), c = m.inject(st, 0, 2.0);
		m.run(10000);
		assertEquals(java.util.Arrays.asList(a.getId(), c.getId(), b.getId()), out.ids);
	}

	@Test
	void finiteQueueDropsJobsThatDoNotFit() throws Exception {
		wire(queue(1, 1, Queue.FINITE_DROP, new FCFSstrategy()), new Server(1, new Integer[] { 1 }, svc(1, 10.0)), 1);
		for (int i = 0; i < 4; i++) {
			m.inject(st, 0, 0.1 * i);
		}
		m.run(10000);
		assertTrue(out.ids.size() < 4, "some job dropped, delivered " + out.ids.size());
		assertTrue(out.ids.size() >= 1);
	}

	@Test
	void twoServersServeTwoJobsInParallel() throws Exception {
		wire(queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy()), new Server(2, new Integer[] { 1 }, svc(1, 10.0)), 1);
		m.inject(st, 0, 0.0);
		m.inject(st, 0, 1.0);
		m.run(10000);
		assertEquals(10.0, out.times.get(0), 1e-9);
		assertEquals(11.0, out.times.get(1), 1e-9);
	}

	@Test
	void delayServesAllJobsConcurrently() throws Exception {
		wire(queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy()), new Delay(svc(1, 5.0)), 1);
		for (int i = 0; i < 3; i++) {
			m.inject(st, 0, 0.5 * i);
		}
		m.run(10000);
		assertEquals(3, out.ids.size());
		assertEquals(5.0, out.times.get(0), 1e-9);
		assertEquals(6.0, out.times.get(2), 1e-9);
	}

	@Test
	void processorSharingSlowsJobsDownTogether() throws Exception {
		wire(queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy()), new PSServer(1, new Integer[] { 1 }, svc(1, 4.0)), 1);
		m.inject(st, 0, 0.0);
		m.inject(st, 0, 0.0001);
		m.run(10000);
		assertEquals(2, out.ids.size());
		assertTrue(out.times.get(0) > 4.0 && out.times.get(0) < 8.1, "first finishes while sharing: " + out.times.get(0));
		assertEquals(8.0, out.times.get(1), 1e-3);
	}

	@Test
	void roundRobinSplitsJobsBetweenTwoOutputs() throws Exception {
		m = new NetFixtures.Mini(0);
		st = m.station("st", queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy()), new Server(1, new Integer[] { 1 }, svc(1, 1.0)),
				new Router(new RoutingStrategy[] { new RoundRobinStrategy() }));
		RecordingSink a = new RecordingSink(), b = new RecordingSink();
		NetNode na = m.station("a", a, null, null), nb = m.station("b", b, null, null);
		m.connect(m.feeder, st);
		m.connect(st, na);
		m.connect(st, nb);
		for (int i = 0; i < 6; i++) {
			m.inject(st, 0, 2.0 * i);
		}
		m.run(10000);
		assertEquals(3, a.ids.size());
		assertEquals(3, b.ids.size());
	}

	@Test
	void responseAndResidenceTimesAreMeasuredPerJob() throws Exception {
		wire(queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy()), new Server(1, new Integer[] { 1 }, svc(1, 10.0)), 1);
		RecordingMeasure resp = m.measure(st, SimConstants.LIST_RESPONSE_TIME, 0);
		RecordingMeasure resid = m.measure(st, SimConstants.LIST_RESIDENCE_TIME, 0);
		m.inject(st, 0, 0.0);
		m.inject(st, 0, 1.0);
		m.run(10000);
		assertEquals(2, resp.samples.size());
		assertEquals(10.0, resp.samples.get(0), 1e-9);
		assertEquals(19.0, resp.samples.get(1), 1e-9);
		assertFalse(resid.samples.isEmpty());
	}

	@Test
	void queueLengthAndUtilizationMeasuresReceiveSamples() throws Exception {
		wire(queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy()), new Server(1, new Integer[] { 1 }, svc(1, 10.0)), 1);
		RecordingMeasure len = m.measure(st, SimConstants.LIST_NUMBER_OF_JOBS, 0);
		RecordingMeasure util = m.sectionMeasure(st.getSection(NodeSection.SERVICE), SimConstants.LIST_NUMBER_OF_JOBS, 0);
		RecordingMeasure eff = m.sectionMeasure(st.getSection(NodeSection.SERVICE), SimConstants.LIST_NUMBER_OF_JOBS_IN_SERVICE, 0);
		m.inject(st, 0, 0.0);
		m.inject(st, 0, 1.0);
		m.run(10000);
		assertFalse(len.samples.isEmpty());
		assertFalse(util.samples.isEmpty());
		assertFalse(eff.samples.isEmpty());
		assertEquals(2.0, java.util.Collections.max(len.samples), 1e-9);
	}

	@Test
	void throughputArrivalAndDropRatesReceiveSamples() throws Exception {
		wire(queue(1, 1, Queue.FINITE_DROP, new FCFSstrategy()), new Server(1, new Integer[] { 1 }, svc(1, 10.0)), 1);
		RecordingInverseMeasure thr = m.rate(st, SimConstants.THROUGHPUT, 0);
		RecordingInverseMeasure arr = m.rate(st, SimConstants.LIST_ARRIVAL_RATE, 0);
		RecordingInverseMeasure drop = m.rate(st, SimConstants.LIST_DROP_RATE, 0);
		for (int i = 0; i < 4; i++) {
			m.inject(st, 0, 0.1 * i);
		}
		m.run(10000);
		assertFalse(thr.samples.isEmpty());
		assertFalse(arr.samples.isEmpty());
		assertFalse(drop.samples.isEmpty(), "at least one job was dropped");
	}

	@Test
	void unknownNodeMeasureIsRejected() throws Exception {
		wire(queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy()), new Server(1, new Integer[] { 1 }, svc(1, 10.0)), 1);
		jmt.common.exception.NetException e = assertThrows(jmt.common.exception.NetException.class, () -> m.measure(st, 9999, 0));
		assertTrue(e.getMessage().contains("required analyzer does not exist"));
	}

	private void wireWith(Queue q, int servers, double serviceTime) throws Exception {
		wire(q, new Server(servers, new Integer[] { 1 }, svc(1, serviceTime)), 1);
	}

	@Test
	void waitingQueuePolicyNeverLosesJobs() throws Exception {
		wireWith(queue(1, 1, Queue.FINITE_WAITING, new FCFSstrategy()), 1, 5.0);
		for (int i = 0; i < 4; i++) {
			m.inject(st, 0, 0.1 * i);
		}
		m.run(10000);
		assertEquals(4, out.ids.size());
	}

	@Test
	void blockingPolicyNeverLosesJobs() throws Exception {
		wireWith(queue(1, 1, Queue.FINITE_BLOCK, new FCFSstrategy()), 1, 5.0);
		for (int i = 0; i < 3; i++) {
			m.inject(st, 0, 0.1 * i);
		}
		m.run(10000);
		assertEquals(3, out.ids.size());
	}

	@Test
	void renegingJobLeavesTheQueueAfterItsPatience() throws Exception {
		NetStrategies.ImpatienceStrategies.Impatience[] imp = new NetStrategies.ImpatienceStrategies.Impatience[] {
				new NetStrategies.ImpatienceStrategies.Reneging(new random.DeterministicDistr(), new random.DeterministicDistrPar(3.0)) };
		Queue q = new Queue(-1, drops(1, Queue.FINITE_DROP), new FCFSstrategy(), putTails(1), imp);
		wireWith(q, 1, 10.0);
		RecordingInverseMeasure ren = m.rate(st, SimConstants.LIST_RENEGING_RATE, 0);
		m.inject(st, 0, 0.0);
		m.inject(st, 0, 1.0);
		m.run(10000);
		assertEquals(1, out.ids.size(), "second job reneged before being served");
		assertFalse(ren.samples.isEmpty());
	}

	@Test
	void balkingJobIsRefusedWhenProbabilityIsOne() throws Exception {
		random.DeterministicDistr d = new random.DeterministicDistr();
		NetStrategies.ServiceStrategies.LDParameter p = new NetStrategies.ServiceStrategies.LDParameter(1, d, new random.DeterministicDistrPar(1.0), "1.0");
		NetStrategies.ImpatienceStrategies.Impatience[] imp = new NetStrategies.ImpatienceStrategies.Impatience[] {
				new NetStrategies.ImpatienceStrategies.Balking(new NetStrategies.ServiceStrategies.LoadDependentStrategy(
						new NetStrategies.ServiceStrategies.LDParameter[] { p }), false, 5L) };
		Queue q = new Queue(-1, drops(1, Queue.FINITE_DROP), new FCFSstrategy(), putTails(1), imp);
		wireWith(q, 1, 10.0);
		RecordingInverseMeasure balk = m.rate(st, SimConstants.LIST_BALKING_RATE, 0);
		for (int i = 0; i < 4; i++) {
			m.inject(st, 0, 0.1 * i);
		}
		m.run(10000);
		assertTrue(out.ids.size() < 4, "some jobs balked, delivered " + out.ids.size());
		assertFalse(balk.samples.isEmpty());
	}

	@Test
	void preloadedJobsAreServedWithoutAnyArrival() throws Exception {
		Queue q = queue(1, -1, Queue.FINITE_DROP, new FCFSstrategy());
		wireWith(q, 1, 2.0);
		m.ready();
		q.preloadJobs(new int[] { 3 });
		m.run(10000);
		assertEquals(3, out.ids.size());
		assertEquals(6.0, out.times.get(2), 1e-9);
	}

	@Test
	void priorityProcessorSharingGivesTheHigherPriorityClassTheWholeServer() throws Exception {
		m = new NetFixtures.Mini(0, 5);
		ServiceStrategy[] s = svc(2, 4.0);
		st = m.station("st", queue(2, -1, Queue.FINITE_DROP, new FCFSstrategy()), new NodeSections.PSServerPriority(1, new Integer[] { 1, 1 }, s,
				new NetStrategies.PSStrategy[] { new NetStrategies.PSStrategies.EPSStrategyPriority(), new NetStrategies.PSStrategies.EPSStrategyPriority() },
				new Double[] { 1.0, 1.0 }),
				new Router(random(2)));
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
		m.inject(st, 0, 0.0);
		m.inject(st, 1, 0.001);
		m.run(10000);
		assertEquals(2, out.ids.size());
		assertEquals(java.util.Arrays.asList(1, 0), out.classIds.size() == 2 ? out.classIds : null);
	}

	@Test
	void psServerWithWeightsServesHeavierClassFaster() throws Exception {
		m = new NetFixtures.Mini(0, 0);
		PSServer ps = new PSServer(1, new Integer[] { 1, 1 }, svc(2, 6.0), new NetStrategies.PSStrategy[] { new NetStrategies.PSStrategies.GPSStrategy(),
				new NetStrategies.PSStrategies.GPSStrategy() }, new Double[] { 3.0, 1.0 });
		st = m.station("st", queue(2, -1, Queue.FINITE_DROP, new FCFSstrategy()), ps, new Router(random(2)));
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
		m.inject(st, 0, 0.0);
		m.inject(st, 1, 0.0001);
		m.run(10000);
		assertEquals(2, out.ids.size());
		assertEquals(0, out.classIds.get(0).intValue(), "weight 3 class finishes first");
	}

	private void wirePolling(NodeSections.PollingServer ps, int k) throws Exception {
		m = new NetFixtures.Mini(0, 0);
		Queue q = new Queue(-1, drops(2, Queue.FINITE_DROP), new NetStrategies.QueueGetStrategies.LimitedPollingGetStrategy(k), putTails(2));
		st = m.station("st", q, ps, new Router(random(2)));
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
	}

	@Test
	void pollingServerSwitchesBetweenClassQueues() throws Exception {
		wirePolling(new NodeSections.LimitedPollingServer(1, new Integer[] { 1, 1 }, svc(2, 1.0), svc(2, 0.5)), 1);
		for (int i = 0; i < 4; i++) {
			m.inject(st, i % 2, 0.01 * i);
		}
		m.run(10000);
		assertEquals(4, out.ids.size());
		assertEquals(java.util.Arrays.asList(0, 1, 0, 1), out.classIds, "one job per class queue in turn");
	}

	@Test
	void gatedAndExhaustivePollingServersServeAllJobs() throws Exception {
		wirePolling(new NodeSections.GatedPollingServer(1, new Integer[] { 1, 1 }, svc(2, 1.0), svc(2, 0.5)), 2);
		for (int i = 0; i < 4; i++) {
			m.inject(st, i % 2, 0.01 * i);
		}
		m.run(10000);
		assertEquals(4, out.ids.size());
	}
}
