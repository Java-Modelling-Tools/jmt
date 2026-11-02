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
package math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ListsTest {

	private static final double EPS = 1e-12;

	// ---- DoubleArrayList ----

	@Test
	void doubleArrayListStoresAndReadsInOrder() {
		DoubleArrayList l = new DoubleArrayList(4);
		l.add(1);
		l.add(2);
		l.add(3);
		assertEquals(3, l.getSize());
		assertEquals(1.0, l.get(0), EPS);
		assertEquals(3.0, l.get(2), EPS);
	}

	@Test
	void doubleArrayListGrowsBeyondInitialCapacity() {
		DoubleArrayList l = new DoubleArrayList(2);
		for (int i = 0; i < 5; i++) {
			l.add(i);
		}
		assertEquals(5, l.getSize());
		assertEquals(4.0, l.get(4), EPS);
	}

	@Test
	void doubleArrayListFromArrayCopiesData() {
		double[] src = { 5, 6, 7 };
		DoubleArrayList l = new DoubleArrayList(src);
		src[0] = 99;
		assertEquals(5.0, l.get(0), EPS);
		assertEquals(3, l.getSize());
	}

	@Test
	void doubleArrayListSetOverwritesElement() {
		DoubleArrayList l = new DoubleArrayList(new double[] { 1, 2, 3 });
		l.set(1, 20);
		assertEquals(20.0, l.get(1), EPS);
		assertThrows(IndexOutOfBoundsException.class, () -> l.set(3, 0));
	}

	@Test
	void doubleArrayListGetOutOfRangeThrows() {
		DoubleArrayList l = new DoubleArrayList(new double[] { 1 });
		assertThrows(IndexOutOfBoundsException.class, () -> l.get(1));
	}

	@Test
	void doubleArrayListDelFirstDropsOldestElements() {
		DoubleArrayList l = new DoubleArrayList(new double[] { 1, 2, 3, 4 });
		l.delFirst(2);
		assertEquals(2, l.getSize());
		assertEquals(3.0, l.get(0), EPS);
		l.delFirst(2);
		assertEquals(0, l.getSize());
		assertThrows(IndexOutOfBoundsException.class, () -> l.delFirst(1));
	}

	@Test
	void doubleArrayListToArrayReturnsInclusiveRange() {
		DoubleArrayList l = new DoubleArrayList(new double[] { 1, 2, 3, 4 });
		assertArrayEquals(new double[] { 2, 3, 4 }, l.toArray(1, 3), EPS);
		assertThrows(IndexOutOfBoundsException.class, () -> l.toArray(3, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> l.toArray(5, 6));
	}

	@Test
	void doubleArrayListClearEmptiesList() {
		DoubleArrayList l = new DoubleArrayList(new double[] { 1, 2 });
		l.clear();
		assertEquals(0, l.getSize());
	}

	@Test
	void doubleArrayListEnsureCapacityRejectsSmallerThanSize() {
		DoubleArrayList l = new DoubleArrayList(new double[] { 1, 2, 3 });
		assertThrows(IndexOutOfBoundsException.class, () -> l.ensureCapacity(2));
	}

	// ---- DirectCircularList ----

	@Test
	void circularListReturnsStoredValuesAndNullForMissing() {
		DirectCircularList<String> l = new DirectCircularList<String>(3);
		l.set(0, "a");
		l.set(1, "b");
		assertEquals("a", l.get(0));
		assertEquals("b", l.get(1));
		assertNull(l.get(2));
		assertTrue(l.exists(1));
		assertFalse(l.exists(2));
		assertEquals(3, l.getSize());
	}

	@Test
	void circularListOverwritesOldestWhenWrapping() {
		DirectCircularList<String> l = new DirectCircularList<String>(3);
		for (int i = 0; i < 5; i++) {
			l.set(i, "v" + i);
		}
		assertNull(l.get(1)); // fell out of the window
		assertEquals("v2", l.get(2));
		assertEquals("v4", l.get(4));
	}

	@Test
	void circularListSkippingAheadInvalidatesGap() {
		DirectCircularList<String> l = new DirectCircularList<String>(4);
		l.set(0, "a");
		l.set(2, "c");
		assertEquals("a", l.get(0));
		assertNull(l.get(1));
		assertEquals("c", l.get(2));
	}

	@Test
	void circularListSkippingAheadWithWrapInvalidatesGap() {
		DirectCircularList<String> l = new DirectCircularList<String>(4);
		l.set(0, "a");
		l.set(2, "c");
		l.set(3, "d");
		l.set(5, "f"); // pos 1, wraps; index 4 is skipped
		assertNull(l.get(4));
		assertEquals("f", l.get(5));
		assertEquals("c", l.get(2));
	}

	@Test
	void circularListJumpFarAheadClearsEverything() {
		DirectCircularList<String> l = new DirectCircularList<String>(3);
		l.set(0, "a");
		l.set(1, "b");
		l.set(10, "z");
		assertNull(l.get(0));
		assertNull(l.get(1));
		assertEquals("z", l.get(10));
	}

	@Test
	void circularListWritingBelowWindowMovesWindowDown() {
		DirectCircularList<String> l = new DirectCircularList<String>(3);
		l.set(6, "g");
		l.set(7, "h");
		l.set(8, "i");
		l.set(3, "d"); // below the current window [6,8]
		assertEquals("d", l.get(3));
		assertNull(l.get(8));
	}

	@Test
	void circularListToStringShowsAllSlots() {
		DirectCircularList<String> l = new DirectCircularList<String>(2);
		l.set(1, "x");
		String s = l.toString();
		assertTrue(s.contains("1: x"));
		assertTrue(s.contains("0: null"));
	}

	// ---- BatchCircularList ----

	@Test
	void batchListAccumulatesWeightedBatches() {
		BatchCircularList b = new BatchCircularList(2, 3);
		b.add(1, 1);
		b.add(3, 1); // batch 0 : samples 4, weights 2
		b.add(5, 2);
		b.add(1, 2); // batch 1 : samples 12, weights 4
		assertEquals(4.0, b.getBatchSample(0), EPS);
		assertEquals(2.0, b.getBatchWeight(0), EPS);
		assertEquals(12.0, b.getBatchSample(1), EPS);
		assertEquals(4.0, b.getBatchWeight(1), EPS);
		assertEquals(2, b.getNumBatches());
		assertEquals(2, b.getBatchSize());
		assertEquals(16.0 / 6.0, b.getMeanValue(), EPS);
		assertEquals(0, b.getDiscarded());
	}

	@Test
	void batchListDiscardsOldestBatchWhenFull() {
		BatchCircularList b = new BatchCircularList(1, 2); // 3 slots, oldest dropped on the 3rd sample
		b.add(1, 1);
		b.add(2, 1);
		b.add(3, 1);
		assertEquals(1, b.getDiscarded());
		assertEquals(2.5, b.getMeanValue(), EPS); // mean of 2 and 3
	}

	@Test
	void batchListDeleteFirstBatchesRemovesThemFromMean() {
		BatchCircularList b = new BatchCircularList(1, 4);
		b.add(1, 1);
		b.add(2, 1);
		b.add(6, 1);
		b.deleteFirstBatches(1);
		assertEquals(1, b.getDiscarded());
		assertEquals(4.0, b.getMeanValue(), EPS); // mean of 2 and 6
		assertEquals(2.0, b.getBatchSample(0), EPS);
	}

	@Test
	void batchListDeleteFirstBatchesAcrossWrapAround() {
		BatchCircularList b = new BatchCircularList(1, 2); // 3 slots
		b.add(1, 1);
		b.add(2, 1);
		b.add(3, 1); // start moves to 1
		b.add(4, 1); // start moves to 2
		b.deleteFirstBatches(1); // start is on the last slot, so it wraps to 0
		assertEquals(3, b.getDiscarded()); // 2 dropped on overflow + 1
		assertEquals(4.0, b.getMeanValue(), EPS); // only sample 4 is left
	}

	@Test
	void batchListGetBatchWrapsAroundStart() {
		BatchCircularList b = new BatchCircularList(1, 2);
		b.add(1, 1);
		b.add(2, 1);
		b.add(3, 1);
		b.add(4, 1); // start = 2: index 0 is slot 2 (sample 3), index 1 wraps to slot 0 (sample 4)
		assertEquals(3.0, b.getBatchSample(0), EPS);
		assertEquals(4.0, b.getBatchSample(1), EPS);
		assertEquals(1.0, b.getBatchWeight(1), EPS);
	}

	@Test
	void batchListCleanReleasesStorage() {
		BatchCircularList b = new BatchCircularList(1, 2);
		b.clean();
		assertThrows(NullPointerException.class, () -> b.add(1, 1));
	}
}
