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

import org.junit.jupiter.api.Test;

/** Copy, delete, extract, CSV, equality and toString helpers of {@link ArrayUtils}. */
class ArrayUtilsCopyDeleteTest {

	private static final double D = 0.0;

	@Test
	void copy_oneDimensionalVariantsAreIndependentCopies() {
		String[] s = { "a", "b" };
		int[] i = { 1, 2 };
		double[] d = { 1.5, 2.5 };
		String[] s2 = ArrayUtils.copy(s);
		int[] i2 = ArrayUtils.copy(i);
		double[] d2 = ArrayUtils.copy(d);
		assertArrayEquals(s, s2);
		assertArrayEquals(i, i2);
		assertArrayEquals(d, d2, D);
		assertNotSame(s, s2);
		d2[0] = 9;
		assertEquals(1.5, d[0], D);
	}

	@Test
	void copy2_and_copy3_deepCopyIncludingRaggedRows() {
		double[][] a = { { 1 }, { 2, 3 } };
		double[][] a2 = ArrayUtils.copy2(a);
		a2[1][1] = 9;
		assertEquals(3, a[1][1], D);
		assertEquals(1, a2[0].length);
		double[][][] b = { { { 1 }, { 2, 3 } }, { { 4 } } };
		double[][][] b2 = ArrayUtils.copy3(b);
		b2[0][1][0] = 9;
		assertEquals(2, b[0][1][0], D);
		assertEquals(4, b2[1][0][0], D);
	}

	@Test
	void copy3per2_scalesEachRowByFactor() {
		double[][][] a = { { { 1, 2 }, { 3 } }, { { 4 } } };
		double[][] f = { { 2, 10 }, { 3 } };
		double[][][] r = ArrayUtils.copy3per2(a, f);
		assertEquals(2, r[0][0][0], D);
		assertEquals(4, r[0][0][1], D);
		assertEquals(30, r[0][1][0], D);
		assertEquals(12, r[1][0][0], D);
	}

	@Test
	void copy3per2_fallsBackToPlainCopyWhenFactorTooSmall() {
		double[][][] a = { { { 1 } }, { { 4 } } };
		assertTrue(ArrayUtils.equals3(a, ArrayUtils.copy3per2(a, new double[][] { { 2 } })));
		assertTrue(ArrayUtils.equals3(a, ArrayUtils.copy3per2(a, new double[][] { { 2 }, {} })));
	}

	@Test
	void copyInto_variantsFillDestination() {
		String[] s = new String[2];
		ArrayUtils.copy(new String[] { "a", "b" }, s);
		assertArrayEquals(new String[] { "a", "b" }, s);
		int[] i = new int[2];
		ArrayUtils.copy(new int[] { 1, 2 }, i);
		assertArrayEquals(new int[] { 1, 2 }, i);
		double[] d = new double[2];
		ArrayUtils.copy(new double[] { 1, 2 }, d);
		assertArrayEquals(new double[] { 1, 2 }, d, D);
		double[][] d2 = { { 0, 0 }, { 0, 0 } };
		ArrayUtils.copy2(new double[][] { { 1, 2 }, { 3, 4 } }, d2);
		assertEquals(4, d2[1][1], D);
		double[][][] d3 = { { { 0 } } };
		ArrayUtils.copy3(new double[][][] { { { 7 } } }, d3);
		assertEquals(7, d3[0][0][0], D);
	}

	@Test
	void delete_removesFirstMiddleLastForEachType() {
		assertArrayEquals(new String[] { "b", "c" }, ArrayUtils.delete(new String[] { "a", "b", "c" }, 0));
		assertArrayEquals(new String[] { "a", "c" }, ArrayUtils.delete(new String[] { "a", "b", "c" }, 1));
		assertArrayEquals(new String[] { "a", "b" }, ArrayUtils.delete(new String[] { "a", "b", "c" }, 2));
		assertArrayEquals(new int[] { 1, 3 }, ArrayUtils.delete(new int[] { 1, 2, 3 }, 1));
		assertArrayEquals(new int[] { 2, 3 }, ArrayUtils.delete(new int[] { 1, 2, 3 }, 0));
		assertArrayEquals(new int[] { 1, 2 }, ArrayUtils.delete(new int[] { 1, 2, 3 }, 2));
		assertArrayEquals(new double[] { 1, 3 }, ArrayUtils.delete(new double[] { 1, 2, 3 }, 1), D);
		assertArrayEquals(new double[] { 2, 3 }, ArrayUtils.delete(new double[] { 1, 2, 3 }, 0), D);
		assertArrayEquals(new double[] { 1, 2 }, ArrayUtils.delete(new double[] { 1, 2, 3 }, 2), D);
	}

	@Test
	void delete_fromEmptyArrayThrows() {
		assertThrows(IllegalArgumentException.class, () -> ArrayUtils.delete(new String[0], 0));
		assertThrows(IllegalArgumentException.class, () -> ArrayUtils.delete(new int[0], 0));
		assertThrows(IllegalArgumentException.class, () -> ArrayUtils.delete(new double[0], 0));
		assertThrows(IllegalArgumentException.class, () -> ArrayUtils.delete2_1(new double[0][], 0));
		assertThrows(IllegalArgumentException.class, () -> ArrayUtils.delete3_1(new double[0][][], 0));
	}

	@Test
	void delete2_and_delete3_removeAlongRequestedDimension() {
		double[][] a = { { 1 }, { 2 }, { 3 } };
		assertEquals(2, ArrayUtils.delete2_1(a, 0).length);
		assertEquals(3, ArrayUtils.delete2_1(a, 1)[1][0], D);
		assertEquals(2, ArrayUtils.delete2_1(a, 2)[1][0], D);
		double[][] b = { { 1, 2 }, { 3, 4 } };
		assertArrayEquals(new double[] { 2 }, ArrayUtils.delete2_2(b, 0)[0], D);
		double[][][] c = { { { 1 } }, { { 2 } }, { { 3 } } };
		assertEquals(2, ArrayUtils.delete3_1(c, 0).length);
		assertEquals(3, ArrayUtils.delete3_1(c, 1)[1][0][0], D);
		assertEquals(2, ArrayUtils.delete3_1(c, 2)[1][0][0], D);
		double[][][] d = { { { 1 }, { 2 } } };
		assertEquals(1, ArrayUtils.delete3_2(d, 1)[0].length);
		double[][][] e = { { { 1, 2 } } };
		assertArrayEquals(new double[] { 2 }, ArrayUtils.delete3_3(e, 0)[0][0], D);
	}

	@Test
	void extract_pullsColumns() {
		double[][][] a = { { { 1 }, { 2 } }, { { 3 }, { 4 } } };
		double[][] r = ArrayUtils.extract13(a, 1);
		assertEquals(2, r[0][0], D);
		assertEquals(4, r[1][0], D);
		assertArrayEquals(new double[] { 2, 4 }, ArrayUtils.extract1(new double[][] { { 1, 2 }, { 3, 4 } }, 1), D);
	}

	@Test
	void makeFilled_fillsOrLeavesZeros() {
		assertEquals(2.5, ArrayUtils.makeFilled(2, 3, 2.5)[1][2], D);
		assertEquals(0, ArrayUtils.makeFilled(2, 3, 0)[1][2], D);
		assertEquals(2, ArrayUtils.makeFilled(2, 3, 0).length);
	}

	@Test
	void insert1_writesColumn() {
		double[][] r = new double[2][2];
		ArrayUtils.insert1(r, new double[] { 5, 6 }, 1);
		assertEquals(5, r[0][1], D);
		assertEquals(6, r[1][1], D);
	}

	@Test
	void toInt_truncatesTowardZero() {
		assertArrayEquals(new int[] { 1, -1, 0 }, ArrayUtils.toInt(new double[] { 1.9, -1.9, 0.2 }));
	}

	@Test
	void toCSV_joinsWithSemicolons() {
		assertEquals("", ArrayUtils.toCSV(new double[0]));
		assertEquals("1.0;2.5", ArrayUtils.toCSV(new double[] { 1, 2.5 }));
		assertEquals("", ArrayUtils.toCSV3_2(new double[][][] { {} }, 0, 0));
		assertEquals("2.0;4.0", ArrayUtils.toCSV3_2(new double[][][] { { { 1, 2 }, { 3, 4 } } }, 0, 1));
	}

	@Test
	void fromCSV_parsesOrRejectsSizeMismatch() {
		double[] arr = new double[2];
		assertTrue(ArrayUtils.fromCSV(arr, "1.5;2"));
		assertArrayEquals(new double[] { 1.5, 2 }, arr, D);
		assertFalse(ArrayUtils.fromCSV(arr, "1"));
		assertArrayEquals(new double[] { 1.5, 3 }, ArrayUtils.fromCSV("1.5;3"), D);
		assertThrows(NumberFormatException.class, () -> ArrayUtils.fromCSV("x"));
		double[][][] c = new double[1][2][2];
		assertTrue(ArrayUtils.fromCSV3_2(c, 0, 1, "7;8"));
		assertEquals(8, c[0][1][1], D);
		assertFalse(ArrayUtils.fromCSV3_2(c, 0, 1, "7"));
	}

	@Test
	void equals2_and_equals3_compareShapeAndValues() {
		double[][] a = { { 1, 2 }, { 3 } };
		assertTrue(ArrayUtils.equals2(a, new double[][] { { 1, 2 }, { 3 } }));
		assertFalse(ArrayUtils.equals2(a, new double[][] { { 1, 2 } }));
		assertFalse(ArrayUtils.equals2(a, new double[][] { { 1, 2 }, { 4 } }));
		double[][][] b = { a };
		assertTrue(ArrayUtils.equals3(b, new double[][][] { { { 1, 2 }, { 3 } } }));
		assertFalse(ArrayUtils.equals3(b, new double[][][] {}));
		assertFalse(ArrayUtils.equals3(b, new double[][][] { { { 1, 2 }, { 4 } } }));
	}

	@Test
	void toString_variantsFormatElements() {
		assertEquals("[ a null ]", ArrayUtils.toString(new Object[] { "a", null }));
		assertEquals("[ 1 2 ]", ArrayUtils.toString(new int[] { 1, 2 }));
		assertEquals("[ 1.0 ]", ArrayUtils.toString(new double[] { 1 }));
		assertEquals("[ [ 1.0 ] [ 2.0 ] ]", ArrayUtils.toString2(new double[][] { { 1 }, { 2 } }));
		assertEquals("[ [ [ 1.0 ] ] ]", ArrayUtils.toString3(new double[][][] { { { 1 } } }));
	}

	@Test
	void prependMultiplyCopy2to3() {
		assertArrayEquals(new double[] { 0, 1, 2 }, ArrayUtils.prepend0(new double[] { 1, 2 }), D);
		assertArrayEquals(new double[] { 2, 4 }, ArrayUtils.multiply(new double[] { 1, 2 }, 2), D);
		double[][][] dest = new double[1][2][2];
		ArrayUtils.copy2to3(new double[][] { { 5, 6 } }, dest, 1);
		assertEquals(6, dest[0][1][1], D);
		assertEquals(0, dest[0][1][0], D);
	}
}
