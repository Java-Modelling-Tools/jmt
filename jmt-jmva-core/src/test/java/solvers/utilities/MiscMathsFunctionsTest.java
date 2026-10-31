package solvers.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import solvers.dataStructures.BigRational;
import solvers.exceptions.InternalErrorException;

class MiscMathsFunctionsTest {

	private static BigRational r(int n, int d) {
		return new BigRational(n, d);
	}

	@Test
	void powersOfTwo() {
		for (int k = 0; k < 31; k++) {
			assertTrue(MiscMathsFunctions.isPowerOf2(1 << k), "2^" + k);
		}
		assertFalse(MiscMathsFunctions.isPowerOf2(0));
		assertFalse(MiscMathsFunctions.isPowerOf2(-8));
		assertFalse(MiscMathsFunctions.isPowerOf2(6));
		assertFalse(MiscMathsFunctions.isPowerOf2(Integer.MIN_VALUE));
	}

	@Test
	void binomialCoefficientsMatchPascalRow() {
		int[] row5 = { 1, 5, 10, 10, 5, 1 };
		for (int k = 0; k <= 5; k++) {
			assertEquals(row5[k], MiscMathsFunctions.binomialCoefficient(5, k));
		}
		assertEquals(1, MiscMathsFunctions.binomialCoefficient(0, 0));
		assertEquals(1, MiscMathsFunctions.binomialCoefficient(1, 1));
		assertEquals(184756, MiscMathsFunctions.binomialCoefficient(20, 10));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> MiscMathsFunctions.binomialCoefficient(3, 4));
	}

	@Test
	void factorials() {
		assertEquals(BigRational.ONE, MiscMathsFunctions.computeFactorial(0));
		assertEquals(BigRational.ONE, MiscMathsFunctions.computeFactorial(1));
		assertEquals(new BigRational(120), MiscMathsFunctions.computeFactorial(5));
		assertEquals(0, new BigDecimal("3628800").compareTo(MiscMathsFunctions.computeFactorialAsBigDecimal(10)));
		assertEquals(BigDecimal.ONE, MiscMathsFunctions.computeFactorialAsBigDecimal(0));
		Map<Integer, BigRational> f = MiscMathsFunctions.computeFactorials(6);
		assertEquals(7, f.size());
		assertEquals(new BigRational(720), f.get(6));
		for (int i = 1; i <= 6; i++) {
			assertEquals(f.get(i - 1).multiply(new BigRational(i)), f.get(i));
		}
		Map<Integer, BigDecimal> g = MiscMathsFunctions.computeFactorialsAsBigDecimal(0);
		assertEquals(1, g.size());
		assertEquals(BigDecimal.ONE, g.get(0));
		assertEquals(0, new BigDecimal(120).compareTo(MiscMathsFunctions.computeFactorialsAsBigDecimal(5).get(5)));
	}

	@Test
	void arrayHelpers() {
		assertEquals(3.0, MiscMathsFunctions.max(new double[] { 1, 3, -2 }), 0.0);
		assertEquals(-2.0, MiscMathsFunctions.max(new double[] { -2 }), 0.0);
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> MiscMathsFunctions.max(new double[0]));
		assertEquals(0.0, MiscMathsFunctions.sum(new double[0]), 0.0);
		assertEquals(4.5, MiscMathsFunctions.sum(new double[] { 1.5, 1, 2 }), 0.0);
		assertEquals(0, MiscMathsFunctions.sum(new int[0]));
		assertEquals(6, MiscMathsFunctions.sum(new int[] { 1, 2, 3 }));
	}

	@Test
	void bigRationalArrayFactories() {
		BigRational[] v = MiscMathsFunctions.createBigRationalArray(new int[] { 1, -2, 0 });
		assertArrayEquals(new BigRational[] { r(1, 1), r(-2, 1), BigRational.ZERO }, v);
		BigRational[][] m = MiscMathsFunctions.createBigRationalArray(new int[][] { { 1, 2 }, { 3, 4 } });
		assertEquals(r(3, 1), m[1][0]);
		assertEquals(0, MiscMathsFunctions.createBigRationalArray(new int[0]).length);
	}

	@Test
	void arrayCopyCopiesElementsNotArrays() {
		Object[][] src = { { "a", "b" }, { "c" } };
		Object[][] dst = { new Object[2], new Object[1] };
		MiscMathsFunctions.arrayCopy(src, dst);
		assertArrayEquals(src, dst);
		assertNotSame(src[0], dst[0]);
		Object[] a = { 1, 2 };
		Object[] b = new Object[2];
		MiscMathsFunctions.arrayCopy(a, b);
		assertArrayEquals(a, b);
	}

	@Test
	void matrixVectorMultiplyAgainstHandComputedProduct() {
		BigRational[][] a = { { r(1, 2), r(1, 3) }, { BigRational.ZERO, r(2, 1) } };
		BigRational[] v = { r(2, 1), r(3, 1) };
		BigRational[] c = MiscMathsFunctions.matrixVectorMultiply(a, v);
		assertEquals(r(2, 1), c[0]); // 1 + 1
		assertEquals(r(6, 1), c[1]);
		assertArrayEquals(c, MiscMathsFunctions.matrixVectorMultiplyJ(a, v));
	}

	@Test
	void matrixVectorMultiplyIdentityAndSingleElement() {
		BigRational[][] id = MiscMathsFunctions.createBigRationalArray(new int[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } });
		BigRational[] v = { r(1, 2), r(3, 4), r(5, 6) };
		assertArrayEquals(v, MiscMathsFunctions.matrixVectorMultiply(id, v));
		assertArrayEquals(v, MiscMathsFunctions.matrixVectorMultiplyJ(id, v));
		BigRational[] one = MiscMathsFunctions.matrixVectorMultiply(new BigRational[][] { { r(3, 1) } }, new BigRational[] { r(1, 3) });
		assertEquals(BigRational.ONE, one[0]);
	}

	@Test
	void matrixVectorMultiplyRejectsSizeMismatch() {
		BigRational[][] a = { { BigRational.ONE, BigRational.ONE } };
		BigRational[] v = { BigRational.ONE };
		assertThrows(ArithmeticException.class, () -> MiscMathsFunctions.matrixVectorMultiply(a, v));
		assertThrows(ArithmeticException.class, () -> MiscMathsFunctions.matrixVectorMultiplyJ(a, v));
	}

	@Test
	void matrixVectorMultiplyPropagatesUndefined() {
		BigRational[][] a = { { BigRational.ONE, BigRational.ONE } };
		BigRational u = new BigRational(-1);
		u.makeUndefined();
		BigRational[] c = MiscMathsFunctions.matrixVectorMultiply(a, new BigRational[] { BigRational.ONE, u });
		assertTrue(c[0].isUndefined());
	}

	@Test
	void matrixVectorMultiplyIgnoresNonPositiveEntries() {
		// deliberate contract (callers pass non-negative vectors): zero and negative v[j] contribute nothing
		BigRational[][] a = { { BigRational.ONE, BigRational.ONE, r(2, 1) } };
		BigRational[] v = { r(3, 1), r(-2, 1), BigRational.ZERO };
		assertEquals(r(3, 1), MiscMathsFunctions.matrixVectorMultiply(a, v)[0]); // 1*3, the -2 is ignored
	}

	@Test
	void matrixVectorMultiplyJHandlesNegativeEntries() {
		BigRational[][] a = { { BigRational.ONE, BigRational.ONE } };
		BigRational[] v = { r(3, 1), r(-2, 1) };
		assertEquals(BigRational.ONE, MiscMathsFunctions.matrixVectorMultiplyJ(a, v)[0]);
	}

	@Test
	void expMatchesMathExp() throws InternalErrorException {
		MathContext mc = new MathContext(30);
		assertEquals(Math.E, MiscMathsFunctions.exp(1, mc).doubleValue(), 1e-14);
		assertEquals(1.0, MiscMathsFunctions.exp(0, mc).doubleValue(), 1e-15);
		assertEquals(Math.exp(-3.5), MiscMathsFunctions.exp(-3.5, mc).doubleValue(), 1e-14);
		assertEquals(1.0, MiscMathsFunctions.exp(10.25, mc).doubleValue() / Math.exp(10.25), 1e-13);
	}

	@Test
	void expRangeLimits() throws InternalErrorException {
		MathContext mc = new MathContext(10);
		assertThrows(InternalErrorException.class, () -> MiscMathsFunctions.exp(40000, mc));
		assertEquals(BigDecimal.ZERO, MiscMathsFunctions.exp(-40000, mc));
	}

	@Test
	void logMatchesMathLog() {
		MathContext mc = new MathContext(30);
		assertEquals(Math.log(1000), MiscMathsFunctions.log(new BigDecimal("1000"), mc).doubleValue(), 1e-12);
		assertEquals(Math.log(2.5), MiscMathsFunctions.log(new BigDecimal("2.5"), mc).doubleValue(), 1e-12);
		assertEquals(0.0, MiscMathsFunctions.log(BigDecimal.ONE, mc).doubleValue(), 1e-12);
		assertEquals(Math.log(0.004), MiscMathsFunctions.log(new BigDecimal("0.004"), mc).doubleValue(), 1e-12);
		// beyond double range: ln(10^500) = 500 ln 10
		assertEquals(500 * Math.log(10), MiscMathsFunctions.log(BigDecimal.TEN.pow(500), mc).doubleValue(), 1e-9);
	}

	@Test
	void logGammaMatchesFactorialsAndHalfInteger() {
		// Lanczos approximation, documented accuracy about 2e-10
		assertEquals(0.0, MiscMathsFunctions.logGamma(1), 1e-8);
		assertEquals(0.0, MiscMathsFunctions.logGamma(2), 1e-8);
		assertEquals(Math.log(24), MiscMathsFunctions.logGamma(5), 1e-8);
		assertEquals(Math.log(362880), MiscMathsFunctions.logGamma(10), 1e-8);
		assertEquals(0.5 * Math.log(Math.PI), MiscMathsFunctions.logGamma(0.5), 1e-8);
	}

	@Test
	void printersDoNotThrowOnEmptyAndNormalInput() {
		MiscMathsFunctions.printMatrix(new int[][] { { 1, 2 }, { 3, 4 } });
		MiscMathsFunctions.printMatrix(new int[] { 1, 2 });
		BigRational u = r(1, 2);
		u.makeUndefined();
		MiscMathsFunctions.printMatrix(new BigRational[][] { { u, BigRational.ZERO } });
		MiscMathsFunctions.dotprintMatrix(new BigRational[][] { { u, BigRational.ZERO } });
		MiscMathsFunctions.printPrettyMatrix(new BigRational[][] { { r(1, 4) } });
		MiscMathsFunctions.printMatrix(new BigRational[] { u, r(1, 3) });
		MiscMathsFunctions.printPrettyMatrix(new BigRational[] { r(1, 4) });
	}
}
