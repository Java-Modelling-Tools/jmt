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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BDMapImplTest {

	private static BDMapImpl grid() {
		BDMapImpl m = new BDMapImpl();
		m.put("x1", "y1", "a");
		m.put("x2", "y1", "b");
		m.put("x1", "y2", "c");
		m.put("x2", "y2", "d");
		return m;
	}

	@Test
	void newMap_isEmptyWithNoKeys() {
		BDMapImpl m = new BDMapImpl();
		assertTrue(m.isEmpty());
		assertEquals(0, m.size(BDMap.X));
		assertEquals(0, m.size(BDMap.Y));
		assertNull(m.get("x", "y"));
	}

	@Test
	void put_thenGetByBothKeys() {
		BDMapImpl m = grid();
		assertFalse(m.isEmpty());
		assertEquals("a", m.get("x1", "y1"));
		assertEquals("b", m.get("x2", "y1"));
		assertEquals("c", m.get("x1", "y2"));
		assertEquals("d", m.get("x2", "y2"));
		assertEquals(2, m.size(BDMap.X));
		assertEquals(2, m.size(BDMap.Y));
	}

	@Test
	void put_overwritesExistingCell() {
		BDMapImpl m = grid();
		m.put("x1", "y1", "z");
		assertEquals("z", m.get("x1", "y1"));
		assertEquals(2, m.size(BDMap.X));
	}

	@Test
	void get_unknownKeyReturnsNull() {
		BDMapImpl m = grid();
		assertNull(m.get("x9", "y1"));
		assertNull(m.get("x1", "y9"));
		assertNull(m.get("x9", BDMap.X));
		assertNull(m.get("y9", BDMap.Y));
		assertNull(m.get("x1", 7));
	}

	@Test
	void get_byCoordinateReturnsColumnAndRow() {
		BDMapImpl m = grid();
		Map col = m.get("x1", BDMap.X);
		assertEquals("a", col.get("y1"));
		assertEquals("c", col.get("y2"));
		Map row = m.get("y2", BDMap.Y);
		assertEquals("c", row.get("x1"));
		assertEquals("d", row.get("x2"));
	}

	@Test
	void keySetAndContainsKey_perCoordinate() {
		BDMapImpl m = grid();
		assertEquals(2, m.keySet(BDMap.X).size());
		assertTrue(m.keySet(BDMap.Y).contains("y2"));
		assertNull(m.keySet(5));
		assertTrue(m.containsKey("x1", BDMap.X));
		assertTrue(m.containsKey("y1", BDMap.Y));
		assertFalse(m.containsKey("y1", BDMap.X));
		assertFalse(m.containsKey("x1", 5));
	}

	@Test
	void size_unknownCoordinateIsZero() {
		assertEquals(0, grid().size(9));
	}

	@Test
	void putMapAlongX_addsColumnAndReplacesExisting() {
		BDMapImpl m = grid();
		Map<Object, Object> col = new HashMap<Object, Object>();
		col.put("y1", "n1");
		col.put("y2", "n2");
		m.put("x3", BDMap.X, col);
		assertEquals(3, m.size(BDMap.X));
		assertEquals("n2", m.get("x3", "y2"));
		col.put("y1", "r1");
		m.put("x1", BDMap.X, col);
		assertEquals("r1", m.get("x1", "y1"));
		assertEquals(3, m.size(BDMap.X));
	}

	@Test
	void putMapAlongY_addsRowAndReplacesExisting() {
		BDMapImpl m = grid();
		Map<Object, Object> row = new HashMap<Object, Object>();
		row.put("x1", "n1");
		row.put("x2", "n2");
		m.put("y3", BDMap.Y, row);
		assertEquals(3, m.size(BDMap.Y));
		assertEquals("n1", m.get("x1", "y3"));
		row.put("x1", "r1");
		m.put("y1", BDMap.Y, row);
		assertEquals("r1", m.get("x1", "y1"));
		assertEquals("n2", m.get("x2", "y1"));
		assertEquals(3, m.size(BDMap.Y));
	}

	@Test
	void putMap_unknownCoordinateIsIgnored() {
		BDMapImpl m = grid();
		m.put("k", 9, new HashMap());
		assertEquals(2, m.size(BDMap.X));
		assertEquals(2, m.size(BDMap.Y));
	}

	@Test
	void remove_cellReturnsOldValueAndClearsIt() {
		BDMapImpl m = grid();
		assertEquals("a", m.remove("x1", "y1"));
		assertNull(m.get("x1", "y1"));
		assertNull(m.remove("x9", "y1"));
		assertEquals(2, m.size(BDMap.X));
	}

	@Test
	void remove_columnDropsKeyAndReturnsContents() {
		BDMapImpl m = grid();
		Map removed = m.remove("x1", BDMap.X);
		assertEquals("a", removed.get("y1"));
		assertEquals("c", removed.get("y2"));
		assertEquals(1, m.size(BDMap.X));
		assertFalse(m.containsKey("x1", BDMap.X));
		assertEquals("b", m.get("x2", "y1"));
		assertNull(m.remove("x1", BDMap.X));
	}

	@Test
	void remove_rowDropsKeyAndReturnsContents() {
		BDMapImpl m = grid();
		Map removed = m.remove("y1", BDMap.Y);
		assertEquals("a", removed.get("x1"));
		assertEquals("b", removed.get("x2"));
		assertEquals(1, m.size(BDMap.Y));
		assertEquals("d", m.get("x2", "y2"));
		assertNull(m.remove("y1", BDMap.Y));
		assertNull(m.remove("x", 9));
	}
}
