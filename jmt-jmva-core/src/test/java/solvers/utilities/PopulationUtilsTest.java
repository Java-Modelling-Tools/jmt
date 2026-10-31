package solvers.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import solvers.SolverMulti;

/** {@link MVAPopulation} and {@link MVAPopulationUtil}. */
class PopulationUtilsTest {

	@Test
	void populationMutatorsAndEmptiness() {
		MVAPopulation p = new MVAPopulation(new int[] { 2, 0 });
		assertFalse(p.emptyClass(0));
		assertTrue(p.emptyClass(1));
		p.decClassPopulation(0);
		p.decClassPopulation(0, 1);
		assertTrue(p.emptyClass(0));
		p.incClassPopulation(1);
		p.incClassPopulation(1, 2);
		p.incClassPopulation(1, 2.9); // double increments truncate
		p.decClassPopulation(1, 1.5);
		assertArrayEquals(new int[] { 0, 4 }, p.getPopulation()); // 0+1+2+2-1
		p.decClassPopulation(0, 3.0);
		assertEquals(-3, p.getPopulation()[0]);
	}

	@Test
	void populationEqualsAndHashCode() {
		MVAPopulation a = new MVAPopulation(new int[] { 1, 2 });
		MVAPopulation b = new MVAPopulation(new int[] { 1, 2 });
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
		assertNotEquals(a, new MVAPopulation(new int[] { 2, 1 }));
		assertNotEquals(a, null);
		assertNotEquals(a, "x");
		b.incClassPopulation(0);
		assertNotEquals(a, b);
	}

	@Test
	void generatesEveryPopulationBelowBoundExactlyOnceSorted() {
		int[][] pops = MVAPopulationUtil.generatePopulations(new int[] { 2, 3, 1 });
		assertEquals(3 * 4 * 2, pops.length);
		Set<String> seen = new HashSet<>();
		for (int[] p : pops) {
			assertTrue(p[0] <= 2 && p[1] <= 3 && p[2] <= 1);
			assertTrue(seen.add(java.util.Arrays.toString(p)), "duplicate " + java.util.Arrays.toString(p));
		}
		assertArrayEquals(new int[] { 0, 0, 0 }, pops[0]);
		assertArrayEquals(new int[] { 2, 3, 1 }, pops[pops.length - 1]);
		// last class is the most significant key
		for (int i = 1; i < pops.length; i++) {
			assertTrue(key(pops[i - 1]) < key(pops[i]));
		}
	}

	private static int key(int[] p) {
		return p[2] * 100 + p[1] * 10 + p[0];
	}

	@Test
	void generatesDegenerateShapes() {
		assertEquals(1, MVAPopulationUtil.generatePopulations(new int[] { 0 }).length);
		assertEquals(1, MVAPopulationUtil.generatePopulations(new int[] { 0, 0 }).length);
		int[][] one = MVAPopulationUtil.generatePopulations(new int[] { 1 });
		assertEquals(2, one.length);
		assertArrayEquals(new int[] { 1 }, one[1]);
		assertEquals(6, MVAPopulationUtil.generatePopulations(new int[] { 5 }).length);
		assertEquals(1, MVAPopulationUtil.generatePopulations(new int[0]).length);
	}

	@Test
	void integerPowerAgainstMathPow() {
		for (int a = 0; a <= 6; a++) {
			for (int b = 0; b <= 8; b++) {
				assertEquals((int) Math.pow(a, b), MVAPopulationUtil.pow(a, b), a + "^" + b);
			}
		}
	}

	@Test
	void hashCodeIsInjectiveMixedRadixOnPopulationBox() {
		int max = 3;
		Set<Integer> seen = new HashSet<>();
		for (int a = 0; a <= max; a++) {
			for (int b = 0; b <= max; b++) {
				for (int c = 0; c <= max; c++) {
					assertTrue(seen.add(MVAPopulationUtil.popHashCode(new int[] { a, b, c }, max)));
				}
			}
		}
		assertEquals(0, MVAPopulationUtil.popHashCode(new int[] { 0, 0, 0 }, max));
		assertEquals(4 * 4 * 3 + 4 * 2 + 1, MVAPopulationUtil.popHashCode(new int[] { 1, 2, 3 }, max));
		assertEquals(0, MVAPopulationUtil.popHashCode(new int[0], max));
	}

	@Test
	void emptyQueueMapHasOneZeroMatrixPerPopulationAndLdSeed() {
		int[][] pops = MVAPopulationUtil.generatePopulations(new int[] { 1, 1 });
		int[] types = { SolverMulti.LI, SolverMulti.LD };
		Map<Integer, double[][]> q = MVAPopulationUtil.getNewEmptyQueueMapNonPriority(pops, 1, 2, types);
		assertEquals(4, q.size());
		int zeroKey = MVAPopulationUtil.popHashCode(new int[] { 0, 0 }, 1);
		for (double[][] m : q.values()) {
			assertEquals(2, m.length);
			assertEquals(3, m[0].length);
		}
		// only the load-dependent station at the empty population has probability 1 of 0 customers
		assertEquals(1.0, q.get(zeroKey)[1][0], 0.0);
		assertEquals(0.0, q.get(zeroKey)[0][0], 0.0);
		int fullKey = MVAPopulationUtil.popHashCode(new int[] { 1, 1 }, 1);
		assertEquals(0.0, q.get(fullKey)[1][0], 0.0);
	}
}
