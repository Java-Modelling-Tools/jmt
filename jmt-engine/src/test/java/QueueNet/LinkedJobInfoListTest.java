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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkedJobInfoListTest {
	private StrategyFixtures fx;
	private JobInfoList list;

	@BeforeEach
	void setUp() throws Exception {
		fx = new StrategyFixtures(StrategyFixtures.classes(0, 0), new NetStrategies.ServiceStrategy[] {
				new StrategyFixtures.ScriptedService(1), new StrategyFixtures.ScriptedService(1) });
		list = fx.queue();
	}

	private JobInfo info(int cls) {
		return new JobInfo(fx.job(cls));
	}

	@Test
	void emptyListHasNoJobs() {
		assertEquals(0, list.size());
		assertEquals(0, list.size(fx.classes[0]));
		assertNull(list.lookFor(fx.job(0)));
		assertEquals(0, list.getJobsIn());
		assertEquals(0, list.getJobsOut());
		assertEquals(0, list.getJobsInService());
	}

	@Test
	void addLastKeepsOrderAndCountsPerClass() {
		JobInfo a = info(0), b = info(1), c = info(0);
		list.addLast(a);
		list.addLast(b);
		list.addLast(c);
		assertEquals(3, list.size());
		assertEquals(2, list.size(fx.classes[0]));
		assertEquals(1, list.size(fx.classes[1]));
		assertEquals(3, list.getJobsIn());
		assertEquals(2, list.getJobsInPerClass(fx.classes[0]));
		assertSame(a, list.getJob(0));
		assertSame(c, list.getJob(2));
		assertSame(b, list.lookFor(b.getJob()));
		assertEquals(2, list.getInternalJobInfoList(fx.classes[0]).size());
	}

	@Test
	void addFirstPutsJobAtHead() {
		JobInfo a = info(0), b = info(0);
		list.addLast(a);
		list.addFirst(b);
		assertSame(b, list.getJob(0));
		assertSame(a, list.getJob(1));
	}

	@Test
	void addAtIndexInsertsInPlace() {
		JobInfo a = info(0), b = info(0), c = info(1);
		list.addLast(a);
		list.addLast(b);
		list.add(1, c);
		assertSame(c, list.getJob(1));
		assertEquals(3, list.size());
	}

	@Test
	void removeFirstAndLastReturnEnds() {
		JobInfo a = info(0), b = info(1), c = info(0);
		list.addLast(a);
		list.addLast(b);
		list.addLast(c);
		assertSame(a, list.removeFirst());
		assertSame(c, list.removeLast());
		assertEquals(1, list.size());
		assertSame(b, list.removeFirst(fx.classes[1]));
		assertEquals(0, list.size());
	}

	@Test
	void removeFirstAndLastOfClassSkipOtherClasses() {
		JobInfo a = info(0), b = info(1), c = info(1);
		list.addLast(a);
		list.addLast(b);
		list.addLast(c);
		assertSame(b, list.removeFirst(fx.classes[1]));
		assertSame(c, list.removeLast(fx.classes[1]));
		assertEquals(1, list.size());
	}

	@Test
	void removeTakesJobOutAndCountsDeparture() {
		JobInfo a = info(0);
		list.addLast(a);
		list.remove(a);
		assertEquals(0, list.size());
		assertEquals(1, list.getJobsOut());
		assertEquals(1, list.getJobsOutPerClass(fx.classes[0]));
	}

	@Test
	void removeOnlyAlsoCountsDeparture() {
		JobInfo a = info(0);
		list.addLast(a);
		list.removeOnly(a);
		assertEquals(0, list.size());
		assertEquals(1, list.getJobsOut());
	}

	@Test
	void removeJobDropsJobWithoutCountingDeparture() {
		JobInfo a = info(0);
		list.addLast(a);
		list.removeJob(a);
		assertEquals(0, list.size());
		assertEquals(0, list.getJobsOut());
	}

	@Test
	void startAndEndJobTrackServiceCount() {
		JobInfo a = info(0);
		list.addLast(a);
		list.startJob(a);
		assertEquals(1, list.getJobsInService());
		assertEquals(1, list.getJobsInServicePerClass(fx.classes[0]));
		list.endJob(a);
		assertEquals(0, list.getJobsInService());
	}

	@Test
	void dropBalkRenegeSwitchRetryProduceConsumeTouchEventTimes() {
		JobInfo a = info(0);
		list.addLast(a);
		list.startJob(a);
		list.dropJob(a);
		list.balkJob(a);
		list.renegeJob(a);
		list.switchJob(a);
		list.retryJob(a);
		list.produceJob(a);
		list.consumeJob(a);
		list.redirectJob(a);
		assertEquals(0.0, list.getLastJobDropTime());
		assertEquals(0.0, list.getLastJobBalkingTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobRenegingTime());
		assertEquals(0.0, list.getLastJobSwitchTimePerClass(fx.classes[0]));
	}

	@Test
	void lastTimesAreZeroAtTimeZeroAfterTraffic() {
		JobInfo a = info(0);
		list.addLast(a);
		list.startJob(a);
		list.endJob(a);
		list.removeJob(a);
		assertEquals(0.0, list.getLastJobInTime());
		assertEquals(0.0, list.getLastJobOutTime());
		assertEquals(0.0, list.getLastJobStartTime());
		assertEquals(0.0, list.getLastJobEndTime());
		assertEquals(0.0, list.getLastModifyTime());
		assertEquals(0.0, list.getLastServiceModifyTime());
		assertEquals(0.0, list.getLastJobSojournTime());
		assertEquals(0.0, list.getTotalSojournTime());
		assertEquals(0.0, list.getTotalSojournTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobInTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobOutTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobStartTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobEndTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastModifyTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastServiceModifyTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobSojournTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobDropTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobRenegingTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastJobBalkingTime());
		assertEquals(0.0, list.getLastJobSwitchTime());
		assertEquals(0.0, list.getLastJobRetrialAttemptTime());
		assertEquals(0.0, list.getLastJobRetrialTimePerClass(fx.classes[0]));
		assertEquals(0.0, list.getLastRetrialOrbitModifyTime());
		assertEquals(0.0, list.getLastRetrialOrbitModifyTimePerClass(fx.classes[0]));
	}

	@Test
	void retrialOrbitAddsAndRemoves() {
		Job j = fx.job(0);
		list.addToRetrialOrbit(j);
		assertFalse(list.getRetrialOrbit().isEmpty());
		list.removeFromRetrialOrbit(j);
		int left = 0;
		for (java.util.List<Double> l : list.getRetrialOrbit().values()) {
			left += l.size();
		}
		assertEquals(0, left);
	}

	@Test
	void cacheCountersSplitHitsAndMisses() {
		fx.classes[0].setCachePairClass(fx.classes[1]);
		fx.classes[1].setCachePairClass(fx.classes[0]);
		list.CacheJob(fx.classes[0], true);
		list.CacheJob(fx.classes[0], true);
		list.CacheJob(fx.classes[1], false);
		assertEquals(2, list.getJobsTotalCacheHitCount());
		assertEquals(1, list.getJobsTotalCacheMissCount());
		assertEquals(2, list.getJobsCacheCountPerClass(fx.classes[0]));
	}
}
