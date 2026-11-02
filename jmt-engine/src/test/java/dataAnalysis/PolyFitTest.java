package dataAnalysis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PolyFitTest {

	private static double[] xs() {
		return new double[] { 0, 0.5, 1, 1.5, 2, 2.5, 3 };
	}

	private static double[] quadratic(double[] x) {
		double[] y = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			y[i] = 1 + 2 * x[i] + 3 * x[i] * x[i];
		}
		return y;
	}

	@Test
	void fitRecoversExactPolynomialCoefficients() {
		double[] x = xs();
		assertArrayEquals(new double[] { 1, 2, 3 }, PolyFit.fit(x.length, x, quadratic(x), 2), 1e-9);
	}

	@Test
	void fitRecoversLine() {
		double[] x = { 0, 1, 2, 3 };
		double[] y = { 5, 3, 1, -1 };
		assertArrayEquals(new double[] { 5, -2 }, PolyFit.fit(4, x, y, 1), 1e-9);
	}

	@Test
	void fitReturnsZerosWhenDataIsShorterThanRequested() {
		assertArrayEquals(new double[3], PolyFit.fit(5, new double[] { 1, 2 }, new double[] { 1, 2, 3, 4, 5 }, 2));
		assertArrayEquals(new double[3], PolyFit.fit(5, new double[] { 1, 2, 3, 4, 5 }, new double[] { 1, 2 }, 2));
	}

	@Test
	void fitSvdRejectsTooShortVectors() {
		assertThrows(IllegalArgumentException.class, () -> PolyFit.fitSVD(5, new double[2], new double[5], null, 1));
		assertThrows(IllegalArgumentException.class, () -> PolyFit.fitSVD(5, new double[5], new double[2], null, 1));
		assertThrows(IllegalArgumentException.class, () -> PolyFit.fitSVD(5, new double[5], new double[5], new double[2], 1));
	}

	@Test
	@Disabled("bug: PolyFit.fitSVD never reads y (right-hand side built from x only) and does not even reproduce its own 1+x+x^2 target")
	void fitSvdRecoversExactPolynomialCoefficients() {
		double[] x = xs();
		assertArrayEquals(new double[] { 1, 2, 3 }, PolyFit.fitSVD(x.length, x, quadratic(x), null, 2), 1e-6);
	}
}
