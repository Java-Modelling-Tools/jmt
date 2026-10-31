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

package NetStrategies.ForkStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.common.exception.LoadException;
import NetStrategies.JoinStrategies.GuardJoin;
import NetStrategies.JoinStrategies.NormalJoin;
import NetStrategies.JoinStrategies.PartialJoin;
import NetStrategies.SemaphoreStrategies.NormalSemaphore;
import NetStrategies.ServiceStrategy;
import QueueNet.JobClass;
import QueueNet.NetNode;
import QueueNet.NodeListWithJobNum;
import QueueNet.StrategyFixtures;
import QueueNet.StrategyFixtures.ScriptedService;
import random.EmpiricalEntry;

class ForkStrategiesTest {

	private StrategyFixtures f;
	private NetNode a;
	private NetNode b;
	private NetNode c;

	@BeforeEach
	void setUp() throws Exception {
		f = new StrategyFixtures(StrategyFixtures.classes(0, 0), new ServiceStrategy[] { new ScriptedService(1), new ScriptedService(1) });
		a = f.addOutput("a");
		b = f.addOutput("b");
		c = f.addOutput("c");
	}

	@Test
	void classSwitchForkSendsConfiguredJobsPerClassToEachNode() {
		ClassJobNum toA = new ClassJobNum("a", new String[] { "c0", "c1" }, new Integer[] { 2, 1 });
		ClassJobNum toB = new ClassJobNum("b", new String[] { "c1" }, new String[] { "3" });
		assertEquals("a", toA.getName());
		assertEquals(2, toA.getClasses().length);
		assertEquals(Integer.valueOf(3), toB.getNumbers()[0]);
		ClassSwitchFork s = new ClassSwitchFork(new ClassJobNum[] { toA, toB });
		NodeListWithJobNum out = s.getOutNodes(f.node, f.classes[0]);
		assertEquals(2, out.getNodeList().size());
		assertEquals(6, out.getTotalNum());
		assertEquals(3, out.getTotalNum(a));
		assertEquals(3, out.getTotalNum(b));
		assertEquals(Integer.valueOf(2), out.getJobNumPerClass(a).get(f.classes[0]));
		assertEquals(Integer.valueOf(1), out.getJobNumPerClass(a).get(f.classes[1]));
		assertEquals(0, out.getTotalNum(c));
		assertTrue(out.getJobNumPerClass(c).isEmpty());
		// the second call reuses the resolved nodes and classes
		assertEquals(6, s.getOutNodes(f.server, f.classes[1]).getTotalNum());
		assertTrue(s.check());
		assertEquals(3, new MultiBranchClassSwitchFork(new ClassJobNum[] { toB }).getOutNodes(f.node, f.classes[0]).getTotalNum());
	}

	@Test
	void combForkPicksTheConfiguredNumberOfOutputs() throws Exception {
		CombFork s = new CombFork(new EmpiricalEntry[] { new EmpiricalEntry("0", 0.0), new EmpiricalEntry("2", 1.0) });
		s.setRandomEngine(f.system.getEngine());
		Set<NetNode> seen = new HashSet<NetNode>();
		for (int i = 0; i < 60; i++) {
			NodeListWithJobNum out = s.getOutNodes(f.node, f.classes[0]);
			assertEquals(2, out.getNodeList().size());
			assertEquals(2, out.getTotalNum());
			assertEquals(Integer.valueOf(1), out.getJobNumPerClass(out.getNodeList().get(0)).get(f.classes[0]));
			for (int j = 0; j < 2; j++) {
				seen.add(out.getNodeList().get(j));
			}
		}
		assertEquals(3, seen.size());
		assertTrue(seen.contains(a) && seen.contains(b) && seen.contains(c));
	}

	@Test
	void combForkAllOutputsWhenCountEqualsOutputs() throws Exception {
		CombFork s = new CombFork(new EmpiricalEntry[] { new EmpiricalEntry("3", 1.0) });
		s.setRandomEngine(f.system.getEngine());
		assertEquals(3, s.getOutNodes(f.node, f.classes[0]).getNodeList().size());
	}

	@Test
	void combForkRejectsNonStringNodeCount() {
		EmpiricalEntry bad = new EmpiricalEntry("1", 1.0);
		bad.setValue(Integer.valueOf(1));
		assertThrows(LoadException.class, () -> new CombFork(new EmpiricalEntry[] { bad }));
	}

	@Test
	void probabilitiesForkForksToChosenBranches() throws Exception {
		OutPath toA = new OutPath(new EmpiricalEntry("a", 1.0), new EmpiricalEntry[] { new EmpiricalEntry("2", 1.0) });
		OutPath toB = new OutPath(new EmpiricalEntry("b", 0.0), new EmpiricalEntry[] { new EmpiricalEntry("1", 1.0) });
		ProbabilitiesFork s = new ProbabilitiesFork(new OutPath[] { toA, toB });
		s.setRandomEngine(f.system.getEngine());
		for (int i = 0; i < 5; i++) {
			NodeListWithJobNum out = s.getOutNodes(f.node, f.classes[0]);
			assertEquals(1, out.getNodeList().size());
			assertSame(a, out.getNodeList().get(0));
			assertEquals(2, out.getTotalNum());
		}
	}

	@Test
	void probabilitiesForkRejectsNonStringNodeName() throws Exception {
		OutPath p = new OutPath(new EmpiricalEntry("a", 1.0), new EmpiricalEntry[] { new EmpiricalEntry("1", 1.0) });
		p.getProbabilityOfChosen().setValue(Integer.valueOf(4));
		assertThrows(LoadException.class, () -> new ProbabilitiesFork(new OutPath[] { p }));
	}

	@Test
	void outPathValidatesItsProbabilities() throws Exception {
		EmpiricalEntry one = new EmpiricalEntry("1", 1.0);
		OutPath p = new OutPath(new EmpiricalEntry("a", 0.5), new EmpiricalEntry[] { one });
		assertEquals("a", p.getName());
		p.setName("z");
		assertEquals("z", p.getName());
		assertEquals(1, p.getJobsPerLinkDis().length);
		assertTrue(p.checkProb(new EmpiricalEntry[] { new EmpiricalEntry("1", 0.4), new EmpiricalEntry("2", 0.6) }));
		assertEquals(false, p.checkProb(new EmpiricalEntry[] { new EmpiricalEntry("1", 0.4) }));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setProbabilityOfChosen(new EmpiricalEntry("a", 1.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setProbabilityOfChosen(new EmpiricalEntry("a", -0.1)));
		assertThrows(IncorrectDistributionParameterException.class,
				() -> p.setJobsPerLinkDis(new EmpiricalEntry[] { new EmpiricalEntry("1", 0.5) }));
		assertSame(one, p.getJobsPerLinkDis()[0]);
	}

	@Test
	void joinStrategiesReportRequiredJobs() {
		assertEquals(4, new NormalJoin(4).getRequiredNum());
		assertEquals(2, new PartialJoin(2).getRequiredNum());
		GuardJoin g = new GuardJoin(new String[] { "c0", "c1" }, new String[] { "2", "3" });
		assertEquals(5, g.getRequiredNum());
		Integer[] per = g.getRequiredPerClass();
		assertEquals(Integer.valueOf(3), per[1]);
		per[1] = 99; // a defensive copy is returned
		assertEquals(Integer.valueOf(3), g.getRequiredPerClass()[1]);
		assertTrue(g.check() && new NormalJoin(1).check() && new PartialJoin(1).check());
	}

	@Test
	void semaphoreReportsThreshold() {
		assertEquals(7, new NormalSemaphore(7).getThreshold());
		assertTrue(new NormalSemaphore(1).check());
	}

	@Test
	void forkStrategyHoldsItsRandomEngine() {
		ClassSwitchFork s = new ClassSwitchFork(new ClassJobNum[0]);
		s.setRandomEngine(f.system.getEngine());
		assertSame(f.system.getEngine(), s.getRandomEngine());
		JobClass unused = f.classes[0];
		assertTrue(unused != null);
	}
}
