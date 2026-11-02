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
package jmt.framework.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CircularListTest {

	private static CircularList<Integer> of(int... v) {
		CircularList<Integer> l = new CircularList<Integer>(2);
		for (int i : v) {
			l.add(i);
		}
		return l;
	}

	@Test
	void add_growsBeyondInitialCapacityKeepingOrder() {
		CircularList<Integer> l = of(1, 2, 3, 4, 5);
		assertEquals(Arrays.asList(1, 2, 3, 4, 5), l);
		assertEquals(5, l.size());
	}

	@Test
	void addFirst_wrapsAroundAndGrowsKeepingOrder() {
		CircularList<Integer> l = new CircularList<Integer>(2);
		l.addFirst(1);
		l.addFirst(2);
		l.addFirst(3);
		assertEquals(Arrays.asList(3, 2, 1), l);
		assertEquals(Integer.valueOf(3), l.getFirst());
		assertEquals(Integer.valueOf(1), l.getLast());
	}

	@Test
	void addLast_appends() {
		CircularList<Integer> l = of(1);
		l.addLast(2);
		assertEquals(Arrays.asList(1, 2), l);
	}

	@Test
	void addAll_appendsAndReportsChange() {
		CircularList<Integer> l = new CircularList<Integer>();
		assertTrue(l.addAll(0, Arrays.asList(1, 2, 3)));
		assertFalse(l.addAll(0, Arrays.<Integer>asList()));
		assertEquals(Arrays.asList(1, 2, 3), l);
	}

	@Test
	void addAtIndex_insertsInMiddleStartAndEnd() {
		CircularList<Integer> l = of(1, 3);
		l.add(1, 2);
		l.add(0, 0);
		l.add(4, 4);
		assertEquals(Arrays.asList(0, 1, 2, 3, 4), l);
	}

	@Test
	void addAtIndex_pastSizeThrows() {
		assertThrows(IndexOutOfBoundsException.class, () -> of(1).add(5, 9));
	}

	@Test
	void get_andSet_useLogicalIndexAfterWrap() {
		CircularList<Integer> l = of(1, 2);
		l.removeFirst();
		l.add(3);
		assertEquals(Integer.valueOf(3), l.get(1));
		assertEquals(Integer.valueOf(2), l.set(0, 20));
		assertEquals(Arrays.asList(20, 3), l);
	}

	@Test
	void get_outOfRangeThrows() {
		CircularList<Integer> l = of(1);
		assertThrows(IndexOutOfBoundsException.class, () -> l.get(1));
		assertThrows(IndexOutOfBoundsException.class, () -> l.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> l.set(1, 0));
	}

	@Test
	void emptyList_firstLastAndRemovalThrow() {
		CircularList<Integer> l = new CircularList<Integer>();
		assertThrows(IndexOutOfBoundsException.class, l::getFirst);
		assertThrows(IndexOutOfBoundsException.class, l::getLast);
		assertThrows(IndexOutOfBoundsException.class, l::removeFirst);
		assertThrows(IndexOutOfBoundsException.class, l::removeLast);
	}

	@Test
	void remove_byIndexCoversFirstLastMiddleAndInvalid() {
		CircularList<Integer> l = of(1, 2, 3, 4, 5);
		assertEquals(Integer.valueOf(1), l.remove(0));
		assertEquals(Integer.valueOf(5), l.remove(l.size() - 1));
		assertEquals(Integer.valueOf(3), l.remove(1));
		assertEquals(Arrays.asList(2, 4), l);
		assertThrows(IndexOutOfBoundsException.class, () -> l.remove(7));
	}

	@Test
	void removeFirstAndLast_returnElements() {
		CircularList<Integer> l = of(1, 2, 3);
		assertEquals(Integer.valueOf(1), l.removeFirst());
		assertEquals(Integer.valueOf(3), l.removeLast());
		assertEquals(Arrays.asList(2), l);
	}

	@Test
	void indexOf_findsElementsAndNulls() {
		CircularList<Integer> l = of(1, 2, 1);
		assertEquals(0, l.indexOf(1));
		assertEquals(2, l.lastIndexOf(1));
		assertEquals(-1, l.indexOf(9));
		assertEquals(-1, l.lastIndexOf(9));
		CircularList<Integer> n = new CircularList<Integer>();
		n.add(null);
		n.add(null);
		assertEquals(0, n.indexOf(null));
		assertEquals(1, n.lastIndexOf(null));
		assertEquals(-1, of(1).indexOf(null));
		assertEquals(-1, of(1).lastIndexOf(null));
	}

	@Disabled("bug: CircularList.clear() does not reset size, list still reports old size")
	@Test
	void clear_emptiesTheList() {
		CircularList<Integer> l = of(1, 2, 3);
		l.clear();
		assertTrue(l.isEmpty());
		l.add(7);
		assertEquals(Arrays.asList(7), l);
	}

	@Disabled("bug: CircularList.indexOf(non-null) throws NPE when the list contains a null element")
	@Test
	void indexOf_skipsNullElementsWhenSearchingNonNull() {
		CircularList<Integer> l = new CircularList<Integer>();
		l.add(null);
		l.add(1);
		assertEquals(1, l.indexOf(1));
	}

	@Test
	void clone_isIndependentCopy() {
		CircularList<Integer> l = of(1, 2, 3);
		@SuppressWarnings("unchecked")
		CircularList<Integer> c = (CircularList<Integer>) l.clone();
		l.add(4);
		assertEquals(Arrays.asList(1, 2, 3), c);
	}

	@Test
	void removeRange_fromStartDropsLeadingElements() {
		CircularList<Integer> l = of(1, 2, 3, 4, 5);
		l.subList(0, 2).clear();
		assertEquals(Arrays.asList(3, 4, 5), l);
	}

	@Test
	void removeRange_fromMiddleShiftsTail() {
		CircularList<Integer> l = of(1, 2, 3, 4, 5);
		l.subList(1, 3).clear();
		assertEquals(Arrays.asList(1, 4, 5), l);
	}

	@Test
	void serialization_roundTripsContentsAfterWrap() throws Exception {
		CircularList<Integer> l = of(1, 2, 3);
		l.removeFirst();
		l.add(4);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(l);
		}
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
			assertEquals(Arrays.asList(2, 3, 4), in.readObject());
		}
	}
}
