package jmt.analytical;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import jmt.jmva.analytical.BoundsSolver;
import jmt.jmva.analytical.SolverSingleClosedAB;
import jmt.jmva.analytical.SolverSingleClosedBJB;
import jmt.jmva.analytical.SolverSingleClosedGB;
import jmt.jmva.analytical.SolverSingleClosedMVA;
import jmt.framework.data.Bound;

/** 
 * Unit tests for the single-class bounding solvers.
 * @author Ben Homer, 2014.
 * N.B. load independent stations = 1, delay stations = 2.
 */
public class TestBoundingSolvers {
	
	/** Tolerance of the checks. */
	private static final double TOLERANCE = 0.0000005;
	
	/** Random number generator. */
	private Random rand;
	
	/** Enum representing the single-class solver types implemented. */
	private enum SolverType { AB, BJBave, BJBmin, GB };
	
	/** Which solvers to test (checks all if null). */
	SolverType[] underTest = null;
	
	@Test
	public void TestBalancedModel() {
		int population = 5;
		int types[] = {1, 1, 1}; // All LI
		double[][] s = {{4.0}, {5.0}, {6.0}};
		double[] v = {1.0, 1.0, 1.0};
		CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
	}
	
	@Test
	public void CheckSimpleModel() {
		int population = 100;
		int types[] = {1, 1, 1, 1}; // All LI
		double[][] s = {{100.1}, {5.0}, {50.3}, {1.5}};
		double[] v = {1.0, 1.0, 1.0, 1.0};
		CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
	}
	
	@Test
	public void TestNoDelays() {
		rand = new Random();
		int[] populations = { 50, 100, 999 };
		int types[] = {1, 1, 1, 1, 1};
		double[][] small = {{0.1}, {0.5}, {0.6}, {1.1}, {0.7}};
		double[][] large = {{100}, {200}, {70}, {140}, {131.5}};
		double[][] multipleMax = {{200}, {200}, {7}, {140}, {9}};
		double[][] wideRange = {{600}, {0.5}, {10}, {530}, {240}};
		double[] v = {1.0, 1.0, 1.0, 1.0, 1.0};
		
		for (int p : populations) {
			CheckBoundsAreAccurateForAllSolvers(types, small, v, p);
			CheckBoundsAreAccurateForAllSolvers(types, large, v, p);
			CheckBoundsAreAccurateForAllSolvers(types, multipleMax, v, p);
			CheckBoundsAreAccurateForAllSolvers(types, wideRange, v, p);
		}
	}
	
	@Test
	public void TestWithDelays() {
		rand = new Random();
		int[] populations = { 50, 100, 999 };
		int types[][] = {{2, 1, 1, 1, 1}, {2, 1, 1, 1, 1}, {2, 2, 1, 1, 1}, {2, 1, 1, 2, 2}}; //, {2, 2, 2, 2, 2}};
		double[][] small = {{0.1}, {0.5}, {0.6}, {1.1}, {0.7}};
		double[][] large = {{100}, {200}, {70}, {140}, {131.5}};
		double[][] multipleMax = {{200}, {200}, {7}, {140}, {9}};
		double[][] wideRange = {{600}, {0.5}, {10}, {530}, {240}};
		double[] v = {1.0, 1.0, 1.0, 1.0, 1.0};
		
		for (int p : populations) {
			for (int[] type : types) {
				CheckBoundsAreAccurateForAllSolvers(type, small, v, p);
				CheckBoundsAreAccurateForAllSolvers(type, large, v, p);
				CheckBoundsAreAccurateForAllSolvers(type, multipleMax, v, p);
				CheckBoundsAreAccurateForAllSolvers(type, wideRange, v, p);
			}
		}
	}
	
	/** Expected results in table IV in Casale' GB paper. */
	@Test
	public void TestGBPaper1() {
		int[] ps = {2, 5, 10, 15, 20, 30, 40, 60, 80};
		for (int population : ps) {
			int types[] = {1, 1, 1, 1}; // All LI
			double[][] s = {{0.1}, {0.1}, {0.09}, {0.08}};
			double[] v = {1.0, 1.0, 1.0, 1.0};
			CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
		}
	}
	
	/** Expected results in table V in Casale' GB paper. */
	@Test
	public void TestGBPaper2() {
		int[] ps = {2, 5, 10, 15, 20, 30, 40, 60, 80};
		for (int population : ps) {
			int types[] = {1, 1, 1, 1, 2}; // All LI
			double[][] s = {{0.1}, {0.1}, {0.09}, {0.08}, {1.0}};
			double[] v = {1.0, 1.0, 1.0, 1.0, 1.0};
			CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
		}
	}
	
	/** Expected results in table VI in Casale' GB paper. */
	@Test
	public void TestGBPaper3() {
		int[] ps = {2, 5, 10, 15, 20, 30, 40, 60, 80};
		for (int population : ps) {
			int types[] = {1, 1, 1, 1}; // All LI
			double[][] s = {{0.1}, {0.1}, {0.05}, {0.04}};
			double[] v = {1.0, 1.0, 1.0, 1.0};
			CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
		}
	}
	
	/** Expected results in table VI in Casale' GB paper. */
	@Test
	public void TestGBPaper4() {
		int[] ps = {2, 5, 10, 15, 20, 30, 40, 60, 80};
		for (int population : ps) {
			int types[] = {1, 1, 1, 1, 2};
			double[][] s = {{0.1}, {0.1}, {0.05}, {0.04}, {1.0}};
			double[] v = {1.0, 1.0, 1.0, 1.0, 1.0};
			CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
		}
	}
	
	@Test
	public void TestRandom1() {
		int[] ps = {2, 5, 10, 15, 20, 30, 40, 60, 80};
		for (int population : ps) {
			int types[] = {2, 1, 1, 1, 1, 1, 1};
			double[][] s = {{92.182928}, {16.301012}, {0.085682}, {95.94406}, {0.815738}, {0.228551}, {7.877278}};
			double[] v = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
			CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
		}
	}
	
	@Test
	public void TestRandom2() {
		int[] ps = {2, 4, 10, 15, 20, 30, 40, 60, 80};
		for (int population : ps) {
			int types[] = {1, 1, 2, 1, 1};
			double[][] s = {{56.00519}, {0.045407}, {17.138481}, {3.809013}, {14.759516}};
			double[] v = {1.0, 1.0, 1.0, 1.0, 1.0};
			CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
		}
	}
	
	@Test
	public void TestRandomMultiple() {
		for (int n = 0; n < 50; n++) {
			rand = new Random();
			int population = rand.nextInt(1000)+1;
			int stations = rand.nextInt(100)+1;
			int[] types = new int[stations];
			double[][] s = new double[stations][1];
			double[] v = new double[stations];
			boolean allDelayStations = true;
			for (int i = 0; i < stations; i++) {
				types[i] = rand.nextInt(2)+1;
				if (types[i] == 1) allDelayStations = false;
				s[i][0] = rand.nextDouble()*100;
				v[i] = 1.0;
			}
			
			if (allDelayStations) {
				types[0] = 1;
			}
			
			CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
		}
	}
	
	/** Checks that the bounds are accurate when compared to MVA results for each bounds solver
	 * specified by the 'underTest' variable.
	 * @param types The station types.
	 * @param s The service demands.
	 * @param v The visits.
	 * @param population The class population.
	 */
	private void CheckBoundsAreAccurateForAllSolvers(int[] types, double[][] s, double[] v, int population) {
		if (underTest != null) {
			for (SolverType t : underTest) {
				CheckBoundsAreAccurate(t, types, s, v, population);
			}
		} else {
			for (SolverType t : SolverType.values()) {
				CheckBoundsAreAccurate(t, types, s, v, population);
			}
		}
	}
	
	/** Checks that the bounds output by the specified bounds solver bound the exact results 
	 * given by JMVA's MVA solver.
	 * @param solverType The bound solver type to test.
	 * @param types The station types.
	 * @param s The service demands.
	 * @param v The visits.
	 * @param population The class population.
	 */
	private void CheckBoundsAreAccurate(SolverType solverType, int[] types, double[][] s, double[] v, int population) {
		int stations = types.length;
		SolverSingleClosedMVA mvaSolver = new SolverSingleClosedMVA(population, stations);
		String[] names = new String[stations];
		for (int i = 0; i < stations; i++) {
			names[i] = "s" + i;
		}
		
		mvaSolver.input(names, types, s, v);
		mvaSolver.solve();
		double X = mvaSolver.getTotThroughput();
		double[] Xk = mvaSolver.getThroughput();
		double Q = mvaSolver.getTotUser();
		double[] Qk = mvaSolver.getQueueLen();
		double R = mvaSolver.getTotResTime();
		double[] Rk = mvaSolver.getResTime();
		double[] Uk = mvaSolver.getUtilization();
		
		BoundsSolver boundsSolver = null;
		switch(solverType) {
		case AB:
			boundsSolver = new SolverSingleClosedAB(population, stations);
			break;
		case BJBave:
			boundsSolver = new SolverSingleClosedBJB(population, stations, true);
			break;
		case BJBmin:
			boundsSolver = new SolverSingleClosedBJB(population, stations, false);
			break;
		case GB:
			boundsSolver = new SolverSingleClosedGB(population, stations);
			break;
		}
		
		if (!boundsSolver.input(names, types, s, v)) {
			fail();
		}
		boundsSolver.solve();
		Bound[] Xkb = boundsSolver.getThroughputBounds();
		Bound[] Qkb = boundsSolver.getQueueLengthBounds();
		Bound[] Rkb = boundsSolver.getResidenceTimeBounds();
		Bound[] Ukb = boundsSolver.getUtilisationBounds();
		
		// Check that bounds encompass the actual exact value given by MVA.
		System.out.println("\n--- Solver under test: " + solverType.toString() + " ---");
		System.out.println("Model: types: " + Arrays.toString(types) + " D: " + Arrays.deepToString(s) + " pop: " + population);

		System.out.println("--- Whole system ---");
		checkBound(X, Xkb[stations], "X");
		checkBound(Q, Qkb[stations], "Q");
		checkBound(R, Rkb[stations], "R");
		
		System.out.println("--- Per station ---");
		for (int k = 0; k < stations; k++) {
			System.out.println("k = " + k);
			checkBound(Qk[k], Qkb[k], "Q"+k);
			checkBound(Xk[k], Xkb[k], "X"+k);
			checkBound(Rk[k], Rkb[k], "R"+k);
			checkBound(Uk[k], Ukb[k], "U"+k);
		}
	}
	
	/**
	 * Checks that the provided value is bounded by the specified bound.
	 * @param val The exact value.
	 * @param b The calculated bounds.
	 * @param s String to output to console when this check is performed.
	 */
	private void checkBound(double val, Bound b, String s) {
		if (b.LB == -1) return;
		System.out.print("Check: " + s + " " + b.LB + " <= " + val + " <= " + b.UB);
		assertTrue(b.LB >= 0 && b.LB <= (val+TOLERANCE) && (val-TOLERANCE) <= b.UB);
		System.out.println(" CORRECT");
	}
}