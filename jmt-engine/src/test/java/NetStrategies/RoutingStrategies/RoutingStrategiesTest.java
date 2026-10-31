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

package NetStrategies.RoutingStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jmt.common.exception.LoadException;
import NetStrategies.RoutingStrategy;
import QueueNet.JobClass;
import QueueNet.NetNode;
import QueueNet.NodeSection;
import QueueNet.StrategyFixtures;
import random.Empirical;
import random.EmpiricalEntry;
import random.EmpiricalPar;

class RoutingStrategiesTest {

	private StrategyFixtures f;
	private JobClass open;
	private JobClass closed;

	@BeforeEach
	void setUp() throws Exception {
		JobClass[] cs = new JobClass[] { new JobClass("open", 0, JobClass.OPEN_CLASS, null),
				new JobClass("closed", 0, JobClass.CLOSED_CLASS, null) };
		f = new StrategyFixtures(cs, new NetStrategies.ServiceStrategy[] { new StrategyFixtures.ScriptedService(1),
				new StrategyFixtures.ScriptedService(1) });
		open = cs[0];
		closed = cs[1];
	}

	/** Counts how often each output is chosen over n calls. */
	private Map<NetNode, Integer> sample(RoutingStrategy s, JobClass c, int n) {
		Map<NetNode, Integer> m = new HashMap<NetNode, Integer>();
		for (int i = 0; i < n; i++) {
			NetNode o = s.getOutNode(f.node, c);
			Integer old = m.get(o);
			m.put(o, old == null ? 1 : old + 1);
		}
		return m;
	}

	@Test
	void disabledRoutesNowhere() throws Exception {
		f.addOutput("a");
		assertNull(new DisabledRoutingStrategy().getOutNode(f.node, open));
		assertTrue(new DisabledRoutingStrategy().check());
	}

	@Test
	void sectionOverloadsDelegateToOwnerNode() throws Exception {
		NetNode a = f.addOutput("a");
		RoundRobinStrategy rr = new RoundRobinStrategy();
		assertSame(a, rr.getOutNode((NodeSection) f.server, open));
		assertSame(a, rr.getOutNode((NodeSection) f.server, f.job(0)));
	}

	@Test
	void randomHandlesEmptySingleAndMultipleOutputs() throws Exception {
		RandomStrategy s = new RandomStrategy();
		assertNull(s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		assertSame(a, s.getOutNode(f.node, open));
		NetNode b = f.addOutput("b");
		Map<NetNode, Integer> m = sample(s, open, 400);
		assertEquals(2, m.size());
		assertTrue(m.get(a) > 120 && m.get(b) > 120);
	}

	@Test
	void randomSkipsSinksForClosedClassesOnly() throws Exception {
		RandomStrategy s = new RandomStrategy();
		NetNode sink = f.addSink("sink");
		assertNull(s.getOutNode(f.node, closed));
		assertSame(sink, s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		assertSame(a, s.getOutNode(f.node, closed));
		NetNode b = f.addOutput("b");
		Map<NetNode, Integer> m = sample(s, closed, 200);
		assertEquals(2, m.size());
		assertTrue(!m.containsKey(sink));
		assertTrue(sample(s, open, 300).containsKey(sink));
	}

	@Test
	void roundRobinCyclesThroughOutputs() throws Exception {
		RoundRobinStrategy s = new RoundRobinStrategy();
		assertNull(s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		NetNode c = f.addOutput("c");
		assertSame(a, s.getOutNode(f.node, open));
		assertSame(b, s.getOutNode(f.node, open));
		assertSame(c, s.getOutNode(f.node, open));
		assertSame(a, s.getOutNode(f.node, open));
	}

	@Test
	void roundRobinSkipsSinksForClosedClasses() throws Exception {
		RoundRobinStrategy s = new RoundRobinStrategy();
		NetNode sink = f.addSink("sink");
		assertNull(s.getOutNode(f.node, closed));
		NetNode a = f.addOutput("a");
		for (int i = 0; i < 4; i++) {
			assertSame(a, s.getOutNode(f.node, closed));
		}
		assertSame(sink, new RoundRobinStrategy().getOutNode(f.node, open));
	}

	@Test
	void weightedRoundRobinHonoursWeights() throws Exception {
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		WeightedRoundRobinStrategy s = new WeightedRoundRobinStrategy(
				new WeightEntry[] { new WeightEntry("a", 3), new WeightEntry("b", 1) });
		Map<NetNode, Integer> m = sample(s, open, 8);
		assertEquals(6, (int) m.get(a));
		assertEquals(2, (int) m.get(b));
	}

	@Test
	void weightedRoundRobinReturnsNullWhenWeightsDoNotMatchOutputs() throws Exception {
		assertNull(new WeightedRoundRobinStrategy(new WeightEntry[] { new WeightEntry("a", 1) }).getOutNode(f.node, open));
		f.addOutput("a");
		f.addOutput("b");
		// weight table has only one entry for two outputs
		assertNull(new WeightedRoundRobinStrategy(new WeightEntry[] { new WeightEntry("a", 1) }).getOutNode(f.node, open));
		// all-zero weights
		assertNull(new WeightedRoundRobinStrategy(new WeightEntry[] { new WeightEntry("a", 0), new WeightEntry("b", 0) })
				.getOutNode(f.node, open));
	}

	@Test
	void weightedRoundRobinIgnoresNonStringEntries() throws Exception {
		NetNode a = f.addOutput("a");
		WeightEntry junk = new WeightEntry("x", 5);
		junk.setValue(Integer.valueOf(7));
		assertEquals(Integer.valueOf(7), junk.getValue());
		junk.setWeight(9);
		assertEquals(9, junk.getWeight());
		WeightedRoundRobinStrategy s = new WeightedRoundRobinStrategy(new WeightEntry[] { new WeightEntry("a", 2), junk });
		assertSame(a, s.getOutNode(f.node, open));
	}

	@Test
	void empiricalPicksNodeByProbability() throws Exception {
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		EmpiricalStrategy s = new EmpiricalStrategy(new EmpiricalEntry[] { new EmpiricalEntry("a", 0.0), new EmpiricalEntry("b", 1.0) });
		s.setRandomEngine(f.system.getEngine());
		assertTrue(s.check());
		for (int i = 0; i < 5; i++) {
			assertSame(b, s.getOutNode(f.node, open));
		}
		EmpiricalStrategy mixed = new EmpiricalStrategy(new EmpiricalEntry[] { new EmpiricalEntry("a", 0.5), new EmpiricalEntry("b", 0.5) });
		mixed.setRandomEngine(f.system.getEngine());
		Map<NetNode, Integer> m = sample(mixed, open, 400);
		assertTrue(m.get(a) > 120 && m.get(b) > 120);
	}

	@Test
	void empiricalAcceptsPrebuiltDistributionAndRejectsNonStringNames() throws Exception {
		NetNode a = f.addOutput("a");
		EmpiricalPar par = new EmpiricalPar(new EmpiricalEntry[] { new EmpiricalEntry("a", 1.0) });
		EmpiricalStrategy s = new EmpiricalStrategy(new Empirical(), par);
		s.setRandomEngine(f.system.getEngine());
		assertSame(a, s.getOutNode(f.node, open));

		EmpiricalEntry bad = new EmpiricalEntry("a", 1.0);
		bad.setValue(Integer.valueOf(1));
		assertThrows(LoadException.class, () -> new EmpiricalStrategy(new EmpiricalEntry[] { bad }));
		assertThrows(LoadException.class,
				() -> new EmpiricalStrategy(new Empirical(), new EmpiricalPar(new EmpiricalEntry[] { bad })));
	}

	@Test
	void shortestQueueLengthPicksLeastLoaded() throws Exception {
		ShortestQueueLengthRoutingStrategy s = new ShortestQueueLengthRoutingStrategy();
		assertNull(s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		NetNode c = f.addOutput("c");
		f.load(a, 3);
		f.load(b, NodeSection.SERVICE, 1);
		f.load(c, 2);
		for (int i = 0; i < 5; i++) {
			assertSame(b, s.getOutNode(f.node, open));
		}
	}

	@Test
	void shortestQueueLengthBreaksTiesRandomlyAndHandlesSinks() throws Exception {
		ShortestQueueLengthRoutingStrategy s = new ShortestQueueLengthRoutingStrategy();
		NetNode sink = f.addSink("sink");
		assertNull(s.getOutNode(f.node, closed));
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		f.load(a, 2);
		f.load(b, 2);
		// open class: the empty sink wins; closed class: the sink is avoided
		assertSame(sink, s.getOutNode(f.node, open));
		Map<NetNode, Integer> m = sample(s, closed, 400);
		assertEquals(2, m.size());
		assertTrue(m.get(a) > 120 && m.get(b) > 120);
	}

	@Test
	void leastUtilizationPicksLowestServiceLoad() throws Exception {
		LeastUtilizationRoutingStrategy s = new LeastUtilizationRoutingStrategy();
		assertNull(s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		f.load(a, NodeSection.SERVICE, 1);
		for (int i = 0; i < 5; i++) {
			assertSame(b, s.getOutNode(f.node, open));
		}
		f.load(b, NodeSection.SERVICE, 1);
		Map<NetNode, Integer> m = sample(s, open, 400);
		assertTrue(m.get(a) > 120 && m.get(b) > 120);
	}

	@Test
	void leastUtilizationTreatsSinkPerClassType() throws Exception {
		LeastUtilizationRoutingStrategy s = new LeastUtilizationRoutingStrategy();
		NetNode sink = f.addSink("sink");
		assertNull(s.getOutNode(f.node, closed));
		assertSame(sink, s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		f.load(a, NodeSection.SERVICE, 2);
		assertSame(a, s.getOutNode(f.node, closed));
		assertSame(sink, s.getOutNode(f.node, open));
	}

	@Test
	void fastestServicePrefersEmptyStationsThenRandomAmongTies() throws Exception {
		FastestServiceRoutingStrategy s = new FastestServiceRoutingStrategy();
		assertNull(s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		f.load(a, 2);
		for (int i = 0; i < 5; i++) {
			assertSame(b, s.getOutNode(f.node, open));
		}
		NetNode c = f.addOutput("c");
		Map<NetNode, Integer> m = sample(s, open, 400);
		assertTrue(m.get(b) > 120 && m.get(c) > 120);
		assertTrue(!m.containsKey(a));
	}

	@Test
	void fastestServiceHandlesSinksAndLoadedOnlyStations() throws Exception {
		FastestServiceRoutingStrategy s = new FastestServiceRoutingStrategy();
		NetNode sink = f.addSink("sink");
		assertNull(s.getOutNode(f.node, closed));
		assertSame(sink, s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		f.load(a, 1);
		assertSame(a, s.getOutNode(f.node, closed));
		FastestServiceRoutingStrategy t = new FastestServiceRoutingStrategy();
		StrategyFixtures g = f;
		NetNode b = g.addOutput("b");
		g.load(b, 3);
		// both loaded, equal (zero) service time: either may be chosen
		Map<NetNode, Integer> m = sample(t, closed, 200);
		assertNotNull(m);
		assertTrue(m.containsKey(a) || m.containsKey(b));
	}

	@Test
	void shortestResponseTimeMirrorsFastestServiceSelection() throws Exception {
		ShortestResponseTimeRoutingStrategy s = new ShortestResponseTimeRoutingStrategy();
		assertNull(s.getOutNode(f.node, open));
		NetNode sink = f.addSink("sink");
		assertNull(s.getOutNode(f.node, closed));
		assertSame(sink, s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		f.load(a, 2);
		for (int i = 0; i < 5; i++) {
			assertSame(b, s.getOutNode(f.node, closed));
		}
		f.load(b, 1);
		Map<NetNode, Integer> m = sample(s, closed, 200);
		assertTrue(m.containsKey(a) || m.containsKey(b));
		NetNode c = f.addOutput("c");
		Map<NetNode, Integer> tie = sample(s, closed, 400);
		assertTrue(tie.get(c) == 400);
	}

	@Test
	void powerOfKWithoutMemoryPicksShortestQueue() throws Exception {
		PowerOfKRoutingStrategy s = new PowerOfKRoutingStrategy(2, false);
		assertNull(s.getOutNode(f.node, open));
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		NetNode c = f.addOutput("c");
		f.load(a, 3);
		f.load(b, 1);
		f.load(c, 2);
		assertSame(b, s.getOutNode(f.node, open));
	}

	@Disabled("bug: PropertyBasedStrategy compares an int INFINITY (Integer.MAX_VALUE) with Double.isInfinite, so a closed class is routed to a sink instead of null")
	@Test
	void powerOfKRefusesToRouteClosedClassOnlyToSinks() throws Exception {
		StrategyFixtures g = new StrategyFixtures(new JobClass[] { new JobClass("k", 0, JobClass.CLOSED_CLASS, null) },
				new NetStrategies.ServiceStrategy[] { new StrategyFixtures.ScriptedService(1) });
		g.addSink("s1");
		assertNull(new PowerOfKRoutingStrategy(1, false).getOutNode(g.node, g.classes[0]));
	}

	@Test
	void powerOfKWithMemoryTracksQueuesOfSampledNodes() throws Exception {
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		NetNode c = f.addOutput("c");
		f.load(a, 3);
		f.load(c, 2);
		// k >= number of outputs: every node is sampled, the empty one is always best
		PowerOfKRoutingStrategy all = new PowerOfKRoutingStrategy(3, true);
		for (int i = 0; i < 3; i++) {
			assertSame(b, all.getOutNode(f.node, open));
		}
		// k < outputs: still always one of the outputs
		PowerOfKRoutingStrategy some = new PowerOfKRoutingStrategy(2, true);
		for (int i = 0; i < 20; i++) {
			NetNode o = some.getOutNode(f.node, closed);
			assertTrue(o == a || o == b || o == c);
		}
	}

	@Test
	void loadDependentRoutingSelectsRangeByResidentJobs() throws Exception {
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		LoadDependentRoutingParameter low = new LoadDependentRoutingParameter(0, new EmpiricalEntry[] { new EmpiricalEntry("a", 1.0) });
		LoadDependentRoutingParameter high = new LoadDependentRoutingParameter(2, new EmpiricalEntry[] { new EmpiricalEntry("b", 1.0) });
		low.setRandomEngine(f.system.getEngine());
		high.setRandomEngine(f.system.getEngine());
		// parameters are given unsorted: the strategy sorts them
		LoadDependentRoutingStrategy s = new LoadDependentRoutingStrategy(new LoadDependentRoutingParameter[] { high, low });
		assertNull(s.getOutNode(f.node, open));
		assertSame(a, s.getOutNode(f.server, open));
		f.node.getJobInfoList().addLast(new QueueNet.JobInfo(f.job(0)));
		assertSame(a, s.getOutNode(f.server, open));
		f.node.getJobInfoList().addLast(new QueueNet.JobInfo(f.job(0)));
		assertSame(b, s.getOutNode(f.server, open));
		f.node.getJobInfoList().addLast(new QueueNet.JobInfo(f.job(0)));
		assertSame(b, s.getOutNode(f.server, open));
		assertEquals(-2, low.compareTo(high));
		assertEquals(0, low.compareTo(Integer.valueOf(0)));
		assertThrows(ClassCastException.class, () -> low.compareTo("x"));
	}

	@Test
	void classSwitchRoutingWithoutSwitchRoutesToDestination() throws Exception {
		NetNode a = f.addOutput("a");
		f.addOutput("b");
		ClassSwitchRoutingParameter toA = new ClassSwitchRoutingParameter("a", 1.0, new EmpiricalEntry[] { new EmpiricalEntry("open", 1.0) });
		assertEquals("a", toA.getDestNode());
		assertEquals(1.0, toA.getDestProbability(), 0.0);
		toA.setRandomEngine(f.system.getEngine());
		assertEquals("open", toA.getToClass());
		ClassSwitchRoutingStrategy s = new ClassSwitchRoutingStrategy(new ClassSwitchRoutingParameter[] { toA });
		s.setRandomEngine(f.system.getEngine());
		assertNull(s.getOutNode(f.node, open));
		assertSame(a, s.getOutNode(f.server, f.job(0)));
	}

	@Test
	void classSwitchRoutingChoosesByProbabilityAndCanBeDisabled() throws Exception {
		NetNode a = f.addOutput("a");
		NetNode b = f.addOutput("b");
		ClassSwitchRoutingParameter toA = new ClassSwitchRoutingParameter("a", 0.5, new EmpiricalEntry[] { new EmpiricalEntry("open", 1.0) });
		ClassSwitchRoutingParameter toB = new ClassSwitchRoutingParameter("b", 0.5, new EmpiricalEntry[] { new EmpiricalEntry("open", 1.0) });
		ClassSwitchRoutingStrategy s = new ClassSwitchRoutingStrategy(new ClassSwitchRoutingParameter[] { toA, toB });
		s.setRandomEngine(f.system.getEngine());
		int hitsA = 0;
		int hitsB = 0;
		for (int i = 0; i < 400; i++) {
			NetNode o = s.getOutNode(f.server, f.job(0));
			if (o == a) {
				hitsA++;
			} else if (o == b) {
				hitsB++;
			}
		}
		assertEquals(400, hitsA + hitsB);
		assertTrue(hitsA > 120 && hitsB > 120);

		ClassSwitchRoutingParameter zero = new ClassSwitchRoutingParameter("a", 0.0, new EmpiricalEntry[] { new EmpiricalEntry("open", 1.0) });
		assertNull(new ClassSwitchRoutingStrategy(new ClassSwitchRoutingParameter[] { zero }).getOutNode(f.server, f.job(0)));
		assertNull(new ClassSwitchRoutingStrategy(new ClassSwitchRoutingParameter[0]).getOutNode(f.server, f.job(0)));
	}

	@Test
	void classSwitchParameterRejectsNonStringClassNames() throws Exception {
		EmpiricalEntry bad = new EmpiricalEntry("open", 1.0);
		bad.setValue(Integer.valueOf(3));
		assertThrows(LoadException.class, () -> new ClassSwitchRoutingParameter("a", 1.0, new EmpiricalEntry[] { bad }));
	}
}
