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

package NetStrategies.QueuePutStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jmt.common.exception.NetException;
import NetStrategies.QueuePutStrategy;
import NetStrategies.ServiceStrategy;
import QueueNet.Job;
import QueueNet.JobInfoList;
import QueueNet.StrategyFixtures;
import QueueNet.StrategyFixtures.ScriptedService;

class QueuePutStrategiesTest {

	/** Fixture with classes of the given priorities, all sharing one scripted service strategy. */
	private static StrategyFixtures fixture(int[] priorities, double... script) throws NetException {
		ScriptedService s = new ScriptedService(script);
		ServiceStrategy[] services = new ServiceStrategy[priorities.length];
		Arrays.fill(services, s);
		return new StrategyFixtures(StrategyFixtures.classes(priorities), services);
	}

	/** Fixture where class i has expected service time means[i]. */
	private static StrategyFixtures meanFixture(int[] priorities, double... means) throws NetException {
		ServiceStrategy[] services = new ServiceStrategy[means.length];
		for (int i = 0; i < means.length; i++) {
			services[i] = new ScriptedService(means[i]);
		}
		return new StrategyFixtures(StrategyFixtures.classes(priorities), services);
	}

	/** Puts one job per entry of classIdx (job id = position) and returns the resulting id order. */
	private static List<Integer> run(QueuePutStrategy s, StrategyFixtures f, int... classIdx) throws NetException {
		JobInfoList q = f.queue();
		for (int c : classIdx) {
			s.put(f.job(c), q, f.server);
		}
		return StrategyFixtures.ids(q);
	}

	private static final int[] ONE = { 1 };
	private static final int[] TWO = { 1, 2 };

	@Test
	void headPutsLastArrivalFirst() throws Exception {
		assertEquals(Arrays.asList(2, 1, 0), run(new HeadStrategy(), fixture(ONE, 1), 0, 0, 0));
	}

	@Test
	void tailPutsLastArrivalLast() throws Exception {
		assertEquals(Arrays.asList(0, 1, 2), run(new TailStrategy(), fixture(ONE, 1), 0, 0, 0));
	}

	@Test
	void headPriorityOrdersByPriorityThenLifo() throws Exception {
		// classes: 0 -> priority 1 (low), 1 -> priority 2 (high)
		assertEquals(Arrays.asList(3, 1, 2, 0), run(new HeadStrategyPriority(), fixture(TWO, 1), 0, 1, 0, 1));
	}

	@Test
	void tailPriorityOrdersByPriorityThenFifo() throws Exception {
		assertEquals(Arrays.asList(1, 3, 0, 2), run(new TailStrategyPriority(), fixture(TWO, 1), 0, 1, 0, 1));
	}

	@Test
	void fcfsAndLcfsPreemptiveComparisons() throws Exception {
		StrategyFixtures f = fixture(TWO, 1);
		Job low = f.job(0);
		Job high = f.job(1);
		assertEquals(-1, new FCFSPRStrategy().compare(low, high));
		assertEquals(1, new LCFSPRStrategy().compare(low, high));
		assertEquals(-1, new FCFSPRStrategyPriority().compare(low, high));
		assertEquals(1, new FCFSPRStrategyPriority().compare(high, low));
		assertEquals(-1, new FCFSPRStrategyPriority().compare(low, f.job(0)));
		assertEquals(-1, new LCFSPRStrategyPriority().compare(low, high));
		assertEquals(1, new LCFSPRStrategyPriority().compare(high, low));
		assertEquals(1, new LCFSPRStrategyPriority().compare(low, f.job(0)));
		// the put behaviour is inherited from head/tail
		assertEquals(Arrays.asList(1, 0), run(new LCFSPRStrategy(), fixture(ONE, 1), 0, 0));
		assertEquals(Arrays.asList(0, 1), run(new FCFSPRStrategy(), fixture(ONE, 1), 0, 0));
		assertEquals(Arrays.asList(1, 0), run(new LCFSPRStrategyPriority(), fixture(ONE, 1), 0, 0));
		assertEquals(Arrays.asList(0, 1), run(new FCFSPRStrategyPriority(), fixture(ONE, 1), 0, 0));
	}

	@Test
	void randKeepsAllJobsAndPriorityPrefixIsRespected() throws Exception {
		List<Integer> r = run(new RandStrategy(), fixture(ONE, 1), 0, 0, 0, 0, 0);
		assertEquals(5, r.size());
		assertTrue(r.containsAll(Arrays.asList(0, 1, 2, 3, 4)));

		// priority variant: the two high-priority jobs (ids 1 and 3) always precede the low ones
		for (int i = 0; i < 20; i++) {
			List<Integer> p = run(new RandStrategyPriority(), fixture(TWO, 1), 0, 1, 0, 1, 0);
			assertEquals(5, p.size());
			assertEquals(Arrays.asList(1, 3), p.subList(0, 2).stream().sorted().collect(java.util.stream.Collectors.toList()));
		}
	}

	@Test
	void sjfOrdersByDrawnServiceTime() throws Exception {
		assertEquals(Arrays.asList(1, 2, 0), run(new SJFStrategy(), fixture(ONE, 5, 3, 4), 0, 0, 0));
		assertEquals(Arrays.asList(0, 1, 2), run(new SJFStrategy(), fixture(ONE, 1, 1, 1), 0, 0, 0));
		assertEquals(Arrays.asList(0, 1), run(new SJFStrategy(), fixture(ONE, 1, 9), 0, 0));
	}

	@Test
	void ljfOrdersByDrawnServiceTimeDescending() throws Exception {
		assertEquals(Arrays.asList(0, 2, 1), run(new LJFStrategy(), fixture(ONE, 5, 3, 4), 0, 0, 0));
		assertEquals(Arrays.asList(1, 0), run(new LJFStrategy(), fixture(ONE, 1, 9), 0, 0));
		assertEquals(Arrays.asList(0, 1), run(new LJFStrategy(), fixture(ONE, 1, 1), 0, 0));
	}

	@Test
	void sjfPriorityRanksPriorityFirstThenServiceTime() throws Exception {
		// jobs: 0(low,5) 1(high,9) 2(low,3) 3(high,2) 4(low,4)
		assertEquals(Arrays.asList(3, 1, 2, 4, 0),
				run(new SJFStrategyPriority(), fixture(TWO, 5, 9, 3, 2, 4), 0, 1, 0, 1, 0));
		// equal service time keeps arrival order among equals; a lower priority arrival goes last
		assertEquals(Arrays.asList(0, 1, 2), run(new SJFStrategyPriority(), fixture(TWO, 1, 1, 1), 1, 1, 0));
	}

	@Test
	void ljfPriorityRanksPriorityFirstThenServiceTimeDescending() throws Exception {
		assertEquals(Arrays.asList(1, 3, 0, 4, 2),
				run(new LJFStrategyPriority(), fixture(TWO, 5, 9, 3, 2, 4), 0, 1, 0, 1, 0));
		assertEquals(Arrays.asList(0, 1, 2), run(new LJFStrategyPriority(), fixture(TWO, 1, 1, 1), 1, 1, 0));
	}

	@Test
	void septOrdersByClassMeanAscending() throws Exception {
		StrategyFixtures f = meanFixture(new int[] { 1, 1, 1 }, 5, 2, 3);
		assertEquals(Arrays.asList(1, 2, 0), run(new SEPTStrategy(), f, 0, 1, 2));
		assertEquals(Arrays.asList(0, 1), run(new SEPTStrategy(), meanFixture(new int[] { 1 }, 2), 0, 0));
	}

	@Test
	void leptOrdersByClassMeanDescending() throws Exception {
		StrategyFixtures f = meanFixture(new int[] { 1, 1, 1 }, 5, 2, 3);
		assertEquals(Arrays.asList(0, 2, 1), run(new LEPTStrategy(), f, 0, 1, 2));
		assertEquals(Arrays.asList(0, 1), run(new LEPTStrategy(), meanFixture(new int[] { 1 }, 2), 0, 0));
	}

	@Test
	void septAndLeptPriorityRankPriorityFirst() throws Exception {
		// class 0: p1 mean 5, class 1: p2 mean 2, class 2: p1 mean 3, class 3: p2 mean 6
		int[] pr = { 1, 2, 1, 2 };
		StrategyFixtures f = meanFixture(pr, 5, 2, 3, 6);
		assertEquals(Arrays.asList(1, 3, 2, 0), run(new SEPTStrategyPriority(), f, 0, 1, 2, 3));
		assertEquals(Arrays.asList(3, 1, 0, 2), run(new LEPTStrategyPriority(), meanFixture(pr, 5, 2, 3, 6), 0, 1, 2, 3));
		// a lower-priority job arriving later goes to the back; equal means keep arrival order
		int[] two = { 1, 2 };
		assertEquals(Arrays.asList(0, 1), run(new SEPTStrategyPriority(), meanFixture(two, 4, 4), 1, 0));
		assertEquals(Arrays.asList(0, 1, 2), run(new SEPTStrategyPriority(), meanFixture(two, 4, 4), 1, 1, 0));
		assertEquals(Arrays.asList(0, 1, 2), run(new LEPTStrategyPriority(), meanFixture(two, 4, 4), 1, 1, 0));
	}

	@Test
	void srptOrdersByRemainingTimeAndDrawsOnlyWhenUnset() throws Exception {
		StrategyFixtures f = fixture(ONE, 5, 3, 4);
		assertEquals(Arrays.asList(1, 2, 0), run(new SRPTStrategy(), f, 0, 0, 0));

		// a job whose service time is already set keeps it (no draw)
		StrategyFixtures g = fixture(ONE, 100);
		JobInfoList q = g.queue();
		Job a = g.job(0);
		a.setServiceTime(7);
		Job b = g.job(0);
		b.setServiceTime(2);
		new SRPTStrategy().put(a, q, g.server);
		new SRPTStrategy().put(b, q, g.server);
		assertEquals(Arrays.asList(b.getId(), a.getId()), StrategyFixtures.ids(q));
		assertEquals(7.0, a.getServiceTime(), 0.0);
	}

	@Test
	void srptPriorityRanksPriorityThenRemainingTime() throws Exception {
		StrategyFixtures f = fixture(TWO, 5, 9, 3, 2, 4);
		assertEquals(Arrays.asList(3, 1, 2, 4, 0), run(new SRPTStrategyPriority(), f, 0, 1, 0, 1, 0));
		assertEquals(Arrays.asList(0, 1, 2), run(new SRPTStrategyPriority(), fixture(TWO, 1, 1, 1), 1, 1, 0));
	}

	@Test
	void srptComparisonsPreferShorterRemainingTime() throws Exception {
		StrategyFixtures f = fixture(TWO, 1);
		Job shortJob = f.job(0);
		shortJob.setServiceTime(1);
		Job longJob = f.job(0);
		longJob.setServiceTime(5);
		assertEquals(1, new SRPTStrategy().compare(shortJob, longJob));
		assertEquals(-1, new SRPTStrategy().compare(longJob, shortJob));
		assertEquals(-1, new SRPTStrategy().compare(shortJob, shortJob));
		assertEquals(1, new SRPTStrategyPriority().compare(shortJob, longJob));
		assertEquals(-1, new SRPTStrategyPriority().compare(longJob, shortJob));
		assertEquals(-1, new SRPTStrategyPriority().compare(shortJob, shortJob));
		Job high = f.job(1);
		high.setServiceTime(99);
		assertEquals(-1, new SRPTStrategyPriority().compare(shortJob, high));
		assertEquals(1, new SRPTStrategyPriority().compare(high, shortJob));
	}

	private static void deadline(Job j, double d) {
		j.setCurrentStationSoftDeadline(d);
	}

	private static List<Integer> runDeadlines(QueuePutStrategy s, StrategyFixtures f, int[] classIdx, double[] deadlines)
			throws NetException {
		JobInfoList q = f.queue();
		for (int i = 0; i < classIdx.length; i++) {
			Job j = f.job(classIdx[i]);
			deadline(j, deadlines[i]);
			s.put(j, q, f.server);
		}
		return StrategyFixtures.ids(q);
	}

	@Test
	void eddOrdersByDeadlineAndRejectsMissingDeadline() throws Exception {
		assertEquals(Arrays.asList(1, 2, 0), runDeadlines(new EDDStrategy(), fixture(ONE, 1), new int[] { 0, 0, 0 },
				new double[] { 9, 2, 5 }));
		assertEquals(Arrays.asList(0, 1), runDeadlines(new EDDStrategy(), fixture(ONE, 1), new int[] { 0, 0 },
				new double[] { 3, 3 }));
		StrategyFixtures f = fixture(ONE, 1);
		assertThrows(IllegalArgumentException.class, () -> new EDDStrategy().put(f.job(0), f.queue(), f.server));
		assertThrows(IllegalArgumentException.class, () -> new EDDStrategyPriority().put(f.job(0), f.queue(), f.server));
		assertThrows(IllegalArgumentException.class, () -> new EDFStrategy().put(f.job(0), f.queue(), f.server));
	}

	@Test
	void eddPriorityRanksPriorityThenDeadline() throws Exception {
		assertEquals(Arrays.asList(3, 1, 2, 4, 0), runDeadlines(new EDDStrategyPriority(), fixture(TWO, 1),
				new int[] { 0, 1, 0, 1, 0 }, new double[] { 9, 8, 2, 1, 5 }));
		assertEquals(Arrays.asList(0, 1, 2), runDeadlines(new EDDStrategyPriority(), fixture(TWO, 1),
				new int[] { 1, 1, 0 }, new double[] { 4, 4, 1 }));
	}

	@Test
	void edfComparisonsPreferEarlierDeadline() throws Exception {
		StrategyFixtures f = fixture(TWO, 1);
		Job early = f.job(0);
		deadline(early, 1);
		Job late = f.job(0);
		deadline(late, 5);
		Job high = f.job(1);
		deadline(high, 99);
		assertEquals(1, new EDFStrategy().compare(early, late));
		assertEquals(-1, new EDFStrategy().compare(late, early));
		assertEquals(-1, new EDFStrategy().compare(early, early));
		assertEquals(1, new EDFStrategyPriority().compare(early, late));
		assertEquals(-1, new EDFStrategyPriority().compare(late, early));
		assertEquals(-1, new EDFStrategyPriority().compare(early, early));
		assertEquals(-1, new EDFStrategyPriority().compare(early, high));
		assertEquals(1, new EDFStrategyPriority().compare(high, early));
	}

	@Test
	void tbsAssignsCumulativeDeadlinesAndOrdersByThem() throws Exception {
		// single priority class: u_p = 0 so deadline = max(now, previous) + service time
		StrategyFixtures f = fixture(ONE, 2, 3, 1);
		TBSStrategyPriority s = new TBSStrategyPriority();
		JobInfoList q = f.queue();
		Job a = f.job(0);
		Job b = f.job(0);
		Job c = f.job(0);
		s.put(a, q, f.server);
		s.put(b, q, f.server);
		s.put(c, q, f.server);
		assertEquals(2.0, a.getCurrentStationSoftDeadline(), 1e-12);
		assertEquals(5.0, b.getCurrentStationSoftDeadline(), 1e-12);
		assertEquals(6.0, c.getCurrentStationSoftDeadline(), 1e-12);
		assertEquals(Arrays.asList(0, 1, 2), StrategyFixtures.ids(q));
		assertEquals(1, s.compare(a, b));
	}
}
