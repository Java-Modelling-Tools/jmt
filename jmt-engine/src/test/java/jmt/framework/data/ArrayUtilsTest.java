package jmt.framework.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ArrayUtilsTest {

	private static final String MSG = "newsize must be >=0";

	// generic overload (two-argument)
	@Test
	void resize_sameSizeReturnsSameInstance() {
		Integer[] a = { 1, 2 };
		assertSame(a, ArrayUtils.resize(a, 2));
	}

	@Test
	void resize_growPadsWithNull() {
		assertArrayEquals(new Integer[] { 1, 2, null, null }, ArrayUtils.resize(new Integer[] { 1, 2 }, 4));
	}

	@Test
	void resize_shrinkTruncates() {
		assertArrayEquals(new Integer[] { 1 }, ArrayUtils.resize(new Integer[] { 1, 2 }, 1));
	}

	@Test
	void resize_toZeroGivesEmptyArray() {
		assertEquals(0, ArrayUtils.resize(new Integer[] { 1, 2 }, 0).length);
	}

	@Test
	void resize_negativeSizeThrows() {
		assertEquals(MSG, assertThrows(RuntimeException.class, () -> ArrayUtils.resize(new Integer[] { 1 }, -1)).getMessage());
	}

	@Test
	void resize_genericKeepsComponentType() {
		assertEquals(Integer[].class, ArrayUtils.resize(new Integer[] { 1 }, 3).getClass());
	}

	// int[]
	@Test
	void resizeInt_sameSizeReturnsSameInstance() {
		int[] a = { 1, 2 };
		assertSame(a, ArrayUtils.resize(a, 2, 9));
	}

	@Test
	void resizeInt_growPadsWithPadding() {
		assertArrayEquals(new int[] { 1, 2, 9, 9 }, ArrayUtils.resize(new int[] { 1, 2 }, 4, 9));
	}

	@Test
	void resizeInt_growWithZeroPadding() {
		assertArrayEquals(new int[] { 1, 0 }, ArrayUtils.resize(new int[] { 1 }, 2, 0));
	}

	@Test
	void resizeInt_shrinkTruncates() {
		assertArrayEquals(new int[] { 1 }, ArrayUtils.resize(new int[] { 1, 2 }, 1, 9));
	}

	@Test
	void resizeInt_toZeroGivesEmptyArray() {
		assertEquals(0, ArrayUtils.resize(new int[] { 1 }, 0, 9).length);
	}

	@Test
	void resizeInt_negativeSizeThrows() {
		assertEquals(MSG, assertThrows(RuntimeException.class, () -> ArrayUtils.resize(new int[] { 1 }, -1, 0)).getMessage());
	}

	// double[]
	@Test
	void resizeDouble_sameSizeReturnsSameInstance() {
		double[] a = { 1, 2 };
		assertSame(a, ArrayUtils.resize(a, 2, 9.0));
	}

	@Test
	void resizeDouble_growPadsWithPadding() {
		assertArrayEquals(new double[] { 1, 2.5, 9, 9 }, ArrayUtils.resize(new double[] { 1, 2.5 }, 4, 9.0), 0.0);
	}

	@Test
	void resizeDouble_shrinkTruncates() {
		assertArrayEquals(new double[] { 1 }, ArrayUtils.resize(new double[] { 1, 2 }, 1, 9.0), 0.0);
	}

	@Test
	void resizeDouble_toZeroGivesEmptyArray() {
		assertEquals(0, ArrayUtils.resize(new double[] { 1 }, 0, 9.0).length);
	}

	@Test
	void resizeDouble_negativeSizeThrows() {
		assertEquals(MSG, assertThrows(RuntimeException.class, () -> ArrayUtils.resize(new double[] { 1 }, -1, 0.0)).getMessage());
	}

	// String[] with padding
	@Test
	void resizeString_sameSizeReturnsSameInstance() {
		String[] a = { "a", "b" };
		assertSame(a, ArrayUtils.resize(a, 2, "x"));
	}

	@Test
	void resizeString_growPadsWithPadding() {
		assertArrayEquals(new String[] { "a", "x", "x" }, ArrayUtils.resize(new String[] { "a" }, 3, "x"));
	}

	@Test
	void resizeString_growWithNullPaddingLeavesNulls() {
		assertArrayEquals(new String[] { "a", null }, ArrayUtils.resize(new String[] { "a" }, 2, null));
	}

	@Test
	void resizeString_shrinkTruncates() {
		assertArrayEquals(new String[] { "a" }, ArrayUtils.resize(new String[] { "a", "b" }, 1, "x"));
	}

	@Test
	void resizeString_toZeroGivesEmptyArray() {
		assertEquals(0, ArrayUtils.resize(new String[] { "a" }, 0, "x").length);
	}

	@Test
	void resizeString_negativeSizeThrows() {
		assertEquals(MSG, assertThrows(RuntimeException.class, () -> ArrayUtils.resize(new String[] { "a" }, -1, "x")).getMessage());
	}

	// resize2
	@Test
	void resize2_sameSizeReturnsSameInstance() {
		double[][] a = { { 1, 2 }, { 3, 4 } };
		assertSame(a, ArrayUtils.resize2(a, 2, 2, 9.0));
	}

	@Test
	void resize2_growPadsBothDimensions() {
		double[][] r = ArrayUtils.resize2(new double[][] { { 1 } }, 2, 2, 9.0);
		assertArrayEquals(new double[] { 1, 9 }, r[0], 0.0);
		assertArrayEquals(new double[] { 9, 9 }, r[1], 0.0);
	}

	@Test
	void resize2_shrinkTruncates() {
		double[][] r = ArrayUtils.resize2(new double[][] { { 1, 2 }, { 3, 4 } }, 1, 1, 9.0);
		assertEquals(1, r.length);
		assertArrayEquals(new double[] { 1 }, r[0], 0.0);
	}

	@Test
	void resize2_toZeroGivesEmptyArray() {
		assertEquals(0, ArrayUtils.resize2(new double[][] { { 1 } }, 0, 0, 9.0).length);
	}

	@Test
	void resize2_negativeSizeThrows() {
		assertEquals("newsizes must be >=0",
				assertThrows(RuntimeException.class, () -> ArrayUtils.resize2(new double[][] { { 1 } }, -1, 1, 0.0)).getMessage());
	}

	// resize3var
	@Test
	void resize3var_growPadsAndShrinksPerRow() {
		double[][][] a = { { { 1, 2 } }, { { 3 } } };
		double[][][] r = ArrayUtils.resize3var(a, 3, 1, new int[] { 1, 2, 2 }, 9.0);
		assertEquals(3, r.length);
		assertArrayEquals(new double[] { 1 }, r[0][0], 0.0);
		assertArrayEquals(new double[] { 3, 9 }, r[1][0], 0.0);
		assertArrayEquals(new double[] { 9, 9 }, r[2][0], 0.0);
	}

	@Test
	void resize3var_negativeSizeThrows() {
		assertEquals("newsizes must be >=0",
				assertThrows(RuntimeException.class, () -> ArrayUtils.resize3var(new double[][][] {}, 1, -1, new int[] { 1 }, 0.0)).getMessage());
	}
}
