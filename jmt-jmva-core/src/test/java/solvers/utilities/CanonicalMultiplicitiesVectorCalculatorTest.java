package solvers.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import solvers.dataStructures.MultiplicitiesVector;
import solvers.dataStructures.QNModel;
import solvers.exceptions.InternalErrorException;

class CanonicalMultiplicitiesVectorCalculatorTest {

	private static QNModel model(int r, int m) throws InternalErrorException {
		int[] t = new int[m];
		Integer[] n = new Integer[r];
		Integer[] mult = new Integer[m];
		double[][] d = new double[m][r];
		for (int i = 0; i < m; i++) {
			t[i] = 1;
			mult[i] = 1;
			for (int j = 0; j < r; j++) {
				d[i][j] = 1;
			}
		}
		for (int j = 0; j < r; j++) {
			n[j] = 1;
		}
		return new QNModel(r, m, t, n, new double[r], mult, d);
	}

	/** Number of vectors in N^m summing exactly to level: C(level+m-1, m-1). */
	private static int compositions(int level, int m) {
		return MiscMathsFunctions.binomialCoefficient(level + m - 1, m - 1);
	}

	@Test
	void levelSizesAreStarsAndBars() throws Exception {
		for (int m = 1; m <= 4; m++) {
			CanonicalMultiplicitiesVectorCalculator c = new CanonicalMultiplicitiesVectorCalculator(model(2, m));
			for (int level = 0; level <= 4; level++) {
				List<MultiplicitiesVector> vs = c.findMulVectorsSummingExactlyTo(level);
				assertEquals(compositions(level, m), vs.size(), "m=" + m + " level=" + level);
				Set<MultiplicitiesVector> distinct = new HashSet<>(vs);
				assertEquals(vs.size(), distinct.size());
				for (MultiplicitiesVector v : vs) {
					assertEquals(level, v.sum());
					assertEquals(m, v.size());
				}
			}
		}
	}

	@Test
	void upToLevelIsConcatenationOfExactLevels() throws Exception {
		CanonicalMultiplicitiesVectorCalculator c = new CanonicalMultiplicitiesVectorCalculator(model(2, 3));
		int expected = 0;
		for (int l = 0; l <= 3; l++) {
			expected += compositions(l, 3);
		}
		assertEquals(expected, c.findMulVectorsSummingUpTo(3).size());
		// asking for a smaller level after a larger one gives the cached prefix
		assertEquals(1, c.findMulVectorsSummingUpTo(0).size());
		assertEquals(1 + 3, c.findMulVectorsSummingUpTo(1).size());
		assertTrue(c.findMulVectorsSummingExactlyTo(0).get(0).isZeroVector());
	}

	@Test
	void negativeLevelIsRejected() throws Exception {
		CanonicalMultiplicitiesVectorCalculator c = new CanonicalMultiplicitiesVectorCalculator(model(1, 2));
		assertThrows(UnsupportedOperationException.class, () -> c.findMulVectorsSummingUpTo(-1));
	}

	@Test
	void vectointIsInverseOfInttovec() throws Exception {
		QNModel qnm = model(2, 3);
		CanonicalMultiplicitiesVectorCalculator c = new CanonicalMultiplicitiesVectorCalculator(qnm);
		int total = 0;
		for (int l = 0; l <= qnm.R; l++) {
			total += compositions(l, qnm.M);
		}
		for (int i = 0; i < total; i++) {
			assertEquals(i, c.vectoint(c.inttovec(i)));
		}
		final int past = total;
		assertThrows(InternalErrorException.class, () -> c.inttovec(past));
		assertEquals(0, c.vectoint(new MultiplicitiesVector(0, 3)));
	}
}
