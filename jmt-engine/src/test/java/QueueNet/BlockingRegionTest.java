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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import NetStrategies.ServiceStrategy;

/** The capacity bookkeeping of a blocking region, without wiring it into a simulation. */
class BlockingRegionTest {
	private StrategyFixtures fx;

	private BlockingRegion region(double maxCapacity, double maxMemory, double[] perClass, double[] memPerClass, boolean[] drop,
			double[] weights, double[] sizes, String[] groups, double[] groupCap, double[] groupMem, List<List<Integer>> groupClasses) throws Exception {
		fx = new StrategyFixtures(StrategyFixtures.classes(0, 0), new ServiceStrategy[] { new StrategyFixtures.ScriptedService(1),
				new StrategyFixtures.ScriptedService(1) });
		simEngine.Simulation sim = new simEngine.Simulation(1L, "t");
		sim.addClasses(fx.classes);
		BlockingRegion br = new BlockingRegion("reg", maxCapacity, maxMemory, perClass, memPerClass, drop, weights, sizes, new double[] { 0, 0 }, groups, groupCap,
				groupMem, groupClasses, sim, new String[] { "a", "b" });
		br.network = fx.network;
		return br;
	}

	private BlockingRegion simple(double maxCapacity) throws Exception {
		return region(maxCapacity, -1, new double[] { -1, -1 }, new double[] { -1, -1 }, new boolean[] { false, true }, new double[] { 1, 2 },
				new double[] { 1, 1 }, new String[0], new double[0], new double[0], new ArrayList<List<Integer>>());
	}

	@Test
	void globalConstraintBlocksWhenWeightedOccupationWouldExceedCapacity() throws Exception {
		BlockingRegion r = simple(3);
		assertTrue(r.hasGlobalConstraint());
		assertEquals(3.0, r.getMaxCapacity());
		assertFalse(r.isBlocked(fx.classes[0]));
		r.increaseOccupation(fx.classes[0]);
		r.increaseOccupation(fx.classes[0]);
		assertEquals(2.0, r.getActualOccupation(), 1e-12);
		assertFalse(r.isBlocked(fx.classes[0]), "2 + weight 1 fits in 3");
		assertTrue(r.isBlocked(fx.classes[1]), "2 + weight 2 exceeds 3");
		r.decreaseOccupation(fx.classes[0]);
		assertEquals(1.0, r.getActualOccupation(), 1e-12);
		assertFalse(r.isBlocked(fx.classes[1]));
	}

	@Test
	void withoutConstraintsNothingIsBlocked() throws Exception {
		BlockingRegion r = simple(-1);
		assertFalse(r.hasGlobalConstraint());
		for (int i = 0; i < 50; i++) {
			r.increaseOccupation(fx.classes[1]);
		}
		assertFalse(r.isBlocked(fx.classes[1]));
	}

	@Test
	void occupationIsTrackedPerClass() throws Exception {
		BlockingRegion r = simple(-1);
		r.increaseOccupation(fx.classes[0]);
		r.increaseOccupation(fx.classes[1]);
		assertEquals(1.0, r.getActualOccupation(fx.classes[0]), 1e-12);
		assertEquals(2.0, r.getActualOccupation(fx.classes[1]), 1e-12);
		assertEquals(3.0, r.getActualOccupation(), 1e-12);
		assertEquals(0.0, r.getActualOccupation(null), 1e-12, "no class means no per-class count");
	}

	@Test
	void classAndMemoryConstraintsBlockIndependently() throws Exception {
		BlockingRegion r = region(-1, 2, new double[] { 1, -1 }, new double[] { -1, -1 }, new boolean[] { false, false }, new double[] { 1, 1 },
				new double[] { 1, 1 }, new String[0], new double[0], new double[0], new ArrayList<List<Integer>>());
		r.increaseOccupation(fx.classes[0]);
		assertTrue(r.isBlocked(fx.classes[0]), "class 0 limited to one job");
		assertFalse(r.isBlocked(fx.classes[1]));
		r.increaseOccupation(fx.classes[1]);
		assertTrue(r.isBlocked(fx.classes[1]), "memory 2 reached");
	}

	@Test
	void groupConstraintCoversTheClassesOfTheGroup() throws Exception {
		List<List<Integer>> groups = new ArrayList<List<Integer>>();
		groups.add(Arrays.asList(0, 1));
		BlockingRegion r = region(-1, -1, new double[] { -1, -1 }, new double[] { -1, -1 }, new boolean[] { false, false }, new double[] { 1, 1 },
				new double[] { 1, 1 }, new String[] { "g" }, new double[] { 2 }, new double[] { -1 }, groups);
		r.increaseOccupation(fx.classes[0]);
		assertFalse(r.isBlocked(fx.classes[1]));
		r.increaseOccupation(fx.classes[1]);
		assertTrue(r.isBlocked(fx.classes[0]));
		r.decreaseOccupation(fx.classes[1]);
		assertFalse(r.isBlocked(fx.classes[0]));
	}

	@Test
	void dropFlagsAndNamesAreReported() throws Exception {
		BlockingRegion r = simple(1);
		assertEquals("reg", r.getName());
		assertFalse(r.getClassDrop(fx.classes[0]));
		assertTrue(r.getClassDrop(fx.classes[1]));
		assertTrue(r.getClassDrop(1));
		assertArrayEquals(new String[] { "a", "b" }, r.getRegionNodeNames());
		assertTrue(r.belongsToRegion("a"));
		assertFalse(r.belongsToRegion("zzz"));
		assertArrayEquals(new double[] { 1, 2 }, r.getClassWeights(), 1e-12);
		assertArrayEquals(new double[] { 0, 0 }, r.getClassSoftDeadlines(), 1e-12);
	}

	@Test
	void weightAndSizeMeasuresReceiveOccupationSamples() throws Exception {
		BlockingRegion r = simple(-1);
		RecordingMeasure w = new RecordingMeasure();
		RecordingMeasure sz = new RecordingMeasure();
		r.setWeightMeasure(w);
		r.setSizeMeasure(sz);
		r.increaseOccupation(fx.classes[0]);
		r.decreaseOccupation(fx.classes[0]);
		assertFalse(w.samples.isEmpty());
		assertFalse(sz.samples.isEmpty());
	}
}
