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

package NetStrategies.TransitionUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import QueueNet.ForkJobInfo;
import QueueNet.JobInfo;

class TransitionUtilitiesTest {

	@Test
	void vectorFromEntriesClampsNegativesAndTotals() {
		Integer[] raw = { 2, -3, 4 };
		TransitionVector v = new TransitionVector("k", raw);
		assertEquals("k", v.getKey());
		assertEquals(3, v.size());
		assertEquals(0, v.getEntry(1));
		assertEquals(6, v.getTotal());
		v.setKey("z");
		assertEquals("z", v.getKey());
	}

	@Test
	void vectorSetEntryClampsAndInvalidatesTotal() {
		TransitionVector v = new TransitionVector("k", 3);
		assertEquals(0, v.getTotal());
		v.setEntry(0, 5);
		v.setEntry(1, -2);
		assertEquals(0, v.getEntry(1));
		assertEquals(5, v.getTotal());
	}

	@Test
	void matrixAggregatesRowsAndColumns() {
		TransitionMatrix m = new TransitionMatrix(new TransitionVector[] { new TransitionVector("a", new Integer[] { 1, 2 }),
				new TransitionVector("b", new Integer[] { 3, 4 }) });
		assertEquals(10, m.getTotal());
		assertEquals(3, m.getTotal("a"));
		assertEquals(4, m.getTotal(0));
		assertEquals(6, m.getTotal(1));
		assertEquals(2, m.keySet().size());
		assertEquals(4, m.getEntry("b", 1));
		assertEquals("a", m.getVector("a").getKey());
	}

	@Test
	void matrixRecomputesTotalsAfterModification() {
		TransitionMatrix m = new TransitionMatrix(new TransitionVector[] { new TransitionVector("a", new Integer[] { 1, 2 }) });
		m.setEntry("a", 0, 10);
		assertEquals(12, m.getTotal());
		assertEquals(10, m.getTotal(0));
		assertEquals(12, m.getTotal("a"));
		m.setVector(new TransitionVector("b", new Integer[] { 5, 5 }));
		assertEquals(22, m.getTotal());
		assertEquals(15, m.getTotal(0));
		assertEquals(7, m.getTotal(1));
	}

	@Test
	void emptyMatrixHasZeroTotals() {
		TransitionMatrix empty = new TransitionMatrix(new TransitionVector[0]);
		assertEquals(0, empty.getTotal());
		assertEquals(0, empty.getTotal(0));
		TransitionMatrix sized = new TransitionMatrix(2);
		assertEquals(0, sized.getTotal());
		assertTrue(sized.keySet().isEmpty());
	}

	@Test
	void packetsExposeTheirFields() {
		EnablingPacket e = new EnablingPacket(2, 7);
		assertEquals(2, e.getModeIndex());
		assertEquals(7, e.getEnablingDegree());

		TimingPacket t = new TimingPacket(1, 2.5, 3, 0.5);
		assertEquals(1, t.getModeIndex());
		assertEquals(2.5, t.getFiringDelay(), 0.0);
		assertEquals(3, t.getFiringPriority());
		assertEquals(0.5, t.getFiringWeight(), 0.0);
		assertNull(t.getTimingToken());
		t.setTimingToken(null);
		TimingPacket simple = new TimingPacket(4.0);
		assertEquals(0, simple.getModeIndex());
		assertEquals(4.0, simple.getFiringDelay(), 0.0);
		assertEquals(1, simple.getFiringPriority());
		assertEquals(1.0, simple.getFiringWeight(), 0.0);

		@SuppressWarnings("unchecked")
		List<ForkJobInfo>[] lists = new List[0];
		double[] lifetimes = { 1.0 };
		FiringPacket fp = new FiringPacket(3, "mode", lifetimes, lists);
		assertEquals(3, fp.getModeIndex());
		assertEquals("mode", fp.getModeName());
		assertSame(lifetimes, fp.getTotalLifetimes());
		assertSame(lists, fp.getForkJobInfoLists());

		List<JobInfo> jobs = new ArrayList<JobInfo>();
		EventFinishPacket ef = new EventFinishPacket(2, jobs);
		assertEquals(2, ef.numberOfRequestsToNodes);
		assertSame(jobs, ef.list);

		TransitionVector enabling = new TransitionVector("e", 1);
		TransitionVector resource = new TransitionVector("r", 1);
		VectorsForServerRequest v = new VectorsForServerRequest(enabling, resource, 5);
		assertSame(enabling, v.getEnabling());
		assertSame(resource, v.getResource());
		assertEquals(5, v.getRequestsToNodes());
	}
}
