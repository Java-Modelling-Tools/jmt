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

package NetStrategies.QueueGetStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import NetStrategies.ServiceStrategy;
import QueueNet.Job;
import QueueNet.JobInfo;
import QueueNet.JobInfoList;
import QueueNet.StrategyFixtures;
import QueueNet.StrategyFixtures.ScriptedService;

class QueueGetStrategiesTest {

	private StrategyFixtures f;
	private JobInfoList q;

	@BeforeEach
	void setUp() throws Exception {
		f = new StrategyFixtures(StrategyFixtures.classes(0, 0), new ServiceStrategy[] { new ScriptedService(1), new ScriptedService(1) });
		f.node.setServer(f.server);
		q = f.queue();
	}

	private Job add(int cls) {
		Job j = f.job(cls);
		q.addLast(new JobInfo(j));
		return j;
	}

	@Test
	void fcfsTakesOldestJobAndReportsItsClass() throws Exception {
		Job a = add(1);
		Job b = add(0);
		assertTrue(new FCFSstrategy().check());
		assertEquals(1, new FCFSstrategy().GetJobClassIdOfNextJob(q));
		assertSame(a, new FCFSstrategy().get(q));
		assertSame(b, new FCFSstrategy().get(q, f.node));
		assertNull(new FCFSstrategy().get(q, f.node));
		assertEquals(-1, new FCFSstrategy().GetJobClassIdOfNextJob(q));
	}

	@Test
	void fcfsPerClassTakesOldestJobOfThatClass() {
		add(0);
		Job b = add(1);
		Job c = add(1);
		assertSame(b, new FCFSstrategy().get(q, f.classes[1]));
		assertSame(c, new FCFSstrategy().get(q, f.classes[1]));
	}

	@Test
	void lcfsTakesNewestJobAndReportsItsClass() throws Exception {
		Job a = add(0);
		Job b = add(1);
		assertEquals(1, new LCFSstrategy().GetJobClassIdOfNextJob(q));
		assertSame(b, new LCFSstrategy().get(q));
		assertSame(a, new LCFSstrategy().get(q, f.node));
		assertEquals(-1, new LCFSstrategy().GetJobClassIdOfNextJob(q));
	}

	@Test
	void lcfsPerClassTakesNewestJobOfThatClass() {
		Job a = add(0);
		add(1);
		Job c = add(0);
		assertSame(c, new LCFSstrategy().get(q, f.classes[0]));
		assertSame(a, new LCFSstrategy().get(q, f.classes[0]));
	}

	@Test
	void exhaustivePollingDrainsAClassBeforeMovingOn() {
		ExhaustivePollingGetStrategy s = new ExhaustivePollingGetStrategy();
		s.setPollingQueues(f.network.getJobClasses());
		Job a1 = add(0);
		Job a2 = add(0);
		Job b1 = add(1);
		assertEquals(0, s.GetJobClassIdOfNextJob(q));
		assertSame(a1, s.get(q));
		assertSame(a2, s.get(q));
		assertNull(s.get(q));
		assertEquals(1, s.GetJobClassIdOfNextJob(q));
		assertSame(b1, s.get(q));
		assertNull(s.get(q));
		assertEquals(0, s.GetJobClassIdOfNextJob(q));
		assertNull(s.get(q, f.node));
	}

	@Test
	void limitedPollingServesAtMostKJobsPerVisit() {
		LimitedPollingGetStrategy s = new LimitedPollingGetStrategy(1);
		s.setPollingQueues(f.network.getJobClasses());
		Job a1 = add(0);
		Job a2 = add(0);
		Job b1 = add(1);
		assertSame(a1, s.get(q));
		assertNull(s.get(q));
		assertEquals(1, s.GetJobClassIdOfNextJob(q));
		assertSame(b1, s.get(q));
		assertNull(s.get(q));
		assertSame(a2, s.get(q));
		assertNull(s.get(q, f.node));
	}

	@Test
	void gatedPollingServesOnlyJobsPresentWhenTheQueueWasReached() {
		GatedPollingGetStrategy s = new GatedPollingGetStrategy();
		s.setPollingQueues(f.network.getJobClasses());
		Job a1 = add(0);
		assertSame(a1, s.get(q));
		// a job arriving after the gate closed is not served in this visit
		add(0);
		assertNull(s.get(q));
		assertEquals(1, s.GetJobClassIdOfNextJob(q));
		Job b1 = add(1);
		assertSame(b1, s.get(q));
		assertNull(s.get(q));
		assertEquals(0, s.GetJobClassIdOfNextJob(q));
		assertNull(s.get(q, f.node));
		assertSame(q.getInternalJobInfoList().get(0).getJob(), s.get(q, f.classes[0]));
	}
}
