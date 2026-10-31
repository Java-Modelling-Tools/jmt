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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SmallDataClassesTest {

	@Test
	void cachedHashMap_returnsLatestValueAfterOverwritingCachedKey() {
		CachedHashMap<String, Integer> m = new CachedHashMap<String, Integer>();
		m.put("a", 1);
		assertEquals(Integer.valueOf(1), m.get("a"));
		m.put("a", 2);
		assertEquals(Integer.valueOf(2), m.get("a"));
		m.put("b", 3);
		assertEquals(Integer.valueOf(3), m.get("b"));
		assertEquals(Integer.valueOf(2), m.get("a"));
	}

	@Test
	void cachedHashMap_handlesNullKeyAndMissingKey() {
		CachedHashMap<String, Integer> m = new CachedHashMap<String, Integer>();
		m.put(null, 5);
		assertEquals(Integer.valueOf(5), m.get(null));
		assertNull(m.get("missing"));
	}

	@Test
	void constMap_isFilledByConstructorAndKeepsInsertionOrder() {
		ConstMap<String, Integer> m = new ConstMap<String, Integer>() {
			@Override
			protected void fill() {
				put("b", 2);
				put("a", 1);
			}
		};
		assertEquals(Integer.valueOf(1), m.get("a"));
		assertNull(m.get("zzz"));
		assertEquals(Arrays.asList("b", "a"), Arrays.asList(m.keySet().toArray()));
		Set<String> keys = m.keySet();
		assertThrows(UnsupportedOperationException.class, () -> keys.add("c"));
	}

	@Test
	void labelValue_singleArgUsesValueAsLabel() {
		LabelValue lv = new LabelValue("v");
		assertEquals("v", lv.getLabel());
		assertEquals("v", lv.getValue());
		assertEquals("v", lv.toString());
		assertSame(lv, lv.clone());
	}

	@Test
	void labelValue_equalityDependsOnValueOnly() {
		LabelValue a = new LabelValue("L1", "v");
		LabelValue b = new LabelValue("L2", "v");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, new LabelValue("L1", "w"));
		assertNotEquals(a, "v");
		assertEquals("L1", a.toString());
	}

	@Test
	void labelValue_getElementFindsByValueInArrayAndIterable() {
		LabelValue a = new LabelValue("A", "a");
		LabelValue b = new LabelValue("B", "b");
		assertSame(b, LabelValue.getElement(new LabelValue[] { a, b }, "b"));
		assertNull(LabelValue.getElement(new LabelValue[] { a, b }, "z"));
		assertNull(LabelValue.getElement(new LabelValue[] { a, b }, null));
		assertSame(a, LabelValue.getElement(Arrays.asList(a, b), "a"));
		assertNull(LabelValue.getElement(Arrays.asList(a, b), "z"));
		assertNull(LabelValue.getElement(Collections.<LabelValue>emptyList(), "a"));
	}

	@Test
	void labelValue_getElementMatchesNullValue() {
		LabelValue n = new LabelValue("N", null);
		assertSame(n, LabelValue.getElement(new LabelValue[] { n }, null));
		assertSame(n, LabelValue.getElement(Arrays.asList(n), null));
	}
}
