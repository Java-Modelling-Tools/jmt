package jmt.test.analytical;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import jmt.jmva.analytical.BoundsSolverMulti;
import jmt.jmva.analytical.SolverMultiClosedMVA;
import jmt.jmva.analytical.SolverMultiClosedAB;
import jmt.jmva.analytical.SolverMultiClosedBJB;
import jmt.framework.data.Bound;

/** 
 * Unit tests for the single-class bounding solvers.
 * @author Ben Homer, 2014.
 * N.B. load independent stations = 1, delay stations = 2.
 */
public class TestBoundingSolversMulti {
	
	/** Tolerance of the checks. */
	private static final double TOLERANCE = 0.0000005;
	
	/** Random number generator. */
	private Random rand;
	
	/** Enum representing the multi-class solver types implemented. */
	enum SolverTypeMulti { AB, BJB};
	
	/** Which solvers to test (checks all if null). */
	SolverTypeMulti[] underTest = null;
	
	@Test
	public void CheckSimpleModel() {
		// 4 stations, 2 classes
		int population[] = {100, 50};
		int types[] = {1, 1, 1, 1}; // All LI
		double[][][] s = {
				{{100.1}, {10.0}}, {{5.0}, {4.5}},
				 {{50.3}, {60}}, {{1.5}, {11.0}}
		};
		double[][] v = {
				{1.0, 1.0}, {1.0, 1.0},
				{1.0, 1.0}, {1.0, 1.0}
		};
		CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
	}
	
	@Test
	public void TestNoDelays() {
		rand = new Random();
		int[] p = { 50, 999};
		int types[] = {1, 1, 1, 1, 1};
		double[][][] small = {{{0.1}, {0.3}}, {{0.5}, {0.1}}, {{0.6}, {0.2}}, {{1.1}, {0.8}}, {{0.7}, {0.5}}};
		double[][][] large = {{{100}, {120}}, {{200}, {190}}, {{70}, {111}}, {{140}, {90}}, {{131.5}, {178.3}}};
		double[][][] multipleMax = {{{200}, {200}}, {{200}, {200}}, {{7}, {9}}, {{140}, {115}}, {{9}, {60}}};
		double[][][] wideRange = {{{600}, {1}}, {{0.5}, {900}}, {{10}, {400}}, {{530}, {0.1}}, {{240}, {920}}};
		double[][] v = {{1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}};
		
		CheckBoundsAreAccurateForAllSolvers(types, large, v, p);
		CheckBoundsAreAccurateForAllSolvers(types, small, v, p);
		CheckBoundsAreAccurateForAllSolvers(types, multipleMax, v, p);
		CheckBoundsAreAccurateForAllSolvers(types, wideRange, v, p);
	}
	
	@Test
	public void TestWithDelays() {
		rand = new Random();
		int[] p = { 50, 999};
		int types[] = {1, 2, 1, 2, 1};
		double[][][] small = {{{0.1}, {0.3}}, {{0.5}, {0.1}}, {{0.6}, {0.2}}, {{1.1}, {0.8}}, {{0.7}, {0.5}}};
		double[][][] large = {{{100}, {120}}, {{200}, {190}}, {{70}, {111}}, {{140}, {90}}, {{131.5}, {178.3}}};
		double[][][] multipleMax = {{{200}, {200}}, {{200}, {200}}, {{7}, {9}}, {{140}, {115}}, {{9}, {60}}};
		double[][][] wideRange = {{{600}, {1}}, {{0.5}, {900}}, {{10}, {400}}, {{530}, {0.1}}, {{240}, {920}}};
		double[][] v = {{1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}};
		
		CheckBoundsAreAccurateForAllSolvers(types, large, v, p);
		CheckBoundsAreAccurateForAllSolvers(types, small, v, p);
		CheckBoundsAreAccurateForAllSolvers(types, multipleMax, v, p);
		CheckBoundsAreAccurateForAllSolvers(types, wideRange, v, p);
	}
	
	@Test
	public void Test1() {
		rand = new Random();
		int[] p = { 91, 70 };
		int types[] = {1};
		double[][][] s = {{{96.753258}, {8.5216034}}};
		double[][] v = {{1.0, 1.0}};
		
		CheckBoundsAreAccurateForAllSolvers(types, s, v, p);
	}
	
	@Test
	public void Test2() {
		rand = new Random();
		int[] p = { 75, 1 };
		int types[] = {1, 2, 1, 1, 1, 1};
		double[][][] s = {{{0.010257}, {0.008872}}, 
						  {{88.723102}, {3.423465}},
						  {{77.408716}, {0.711315}},
						  {{0.525151}, {0.051811}},
						  {{0.076574}, {3.422575}},
						  {{0.127162}, {0.533359}}};
		double[][] v = {{1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}, {1.0, 1.0}};
		
		CheckBoundsAreAccurateForAllSolvers(types, s, v, p);
	}
	
	@Test
	public void TestRandomMultiple() {
		for (int n = 0; n < 10; n++) {
			rand = new Random();
			int stations = 4; //rand.nextInt(5)+1;
			int classes = 3; //rand.nextInt(3)+1;
			int[] population = new int[classes];
			int[] types = new int[stations];
			double[][][] s = new double[stations][classes][1];
			double[][] v = new double[stations][classes];
			for (int j = 0; j < classes; j++) {
				population[j] = rand.nextInt(100)+1;
				for (int i = 0; i < stations; i++) {
					types[i] = rand.nextInt(2)+1;
					s[i][j][0] = rand.nextDouble()*100;
					v[i][j] = 1.0;
				}
			}
			
			boolean allDelayStations = true;
			for (int i = 0; i < stations; i++) {
				if (types[i] == 1) {
					allDelayStations = false;
					break;
				}
			}
			
			if (allDelayStations) {
				types[0] = 1;
			}
			
			CheckBoundsAreAccurateForAllSolvers(types, s, v, population);
		}
	}
	
	private void CheckBoundsAreAccurateForAllSolvers(int[] types, double[][][] s, double[][] v, int population[]) {
		if (underTest != null) {
			for (SolverTypeMulti t : underTest) {
				CheckBoundsAreAccurate(t, types, s, v, population);
			}
		} else {
			for (SolverTypeMulti t : SolverTypeMulti.values()) {
				CheckBoundsAreAccurate(t, types, s, v, population);
			}
		}
	}
	
	private void CheckBoundsAreAccurate(SolverTypeMulti solverType, int[] types, double[][][] s, double[][] v, int[] populations) {
		int stations = types.length;
		int classes = populations.length;
		SolverMultiClosedMVA mvaSolver = new SolverMultiClosedMVA(classes, stations);
		String[] names = new String[stations];
		for (int i = 0; i < stations; i++) {
			names[i] = "s" + i;
		}
		
		mvaSolver.input(names, types, s, v, populations);
		mvaSolver.solve();
		//double X = mvaSolver.getTotThroughput();
		double[] Xk = mvaSolver.getAggrThroughput();
		//double[] Xc = mvaSolver.getClsThroughput();
		double[][] Xkc = mvaSolver.getThroughput();
		
		//double Q = mvaSolver.getTotQueueLength();
		double[] Qk = mvaSolver.getAggrQueueLen();
		//double[] Qc = mvaSolver.getClsQueueLen();
		double[][] Qkc = mvaSolver.getQueueLen();
		
		//double R = mvaSolver.getTotResTime();
		double[] Rk = mvaSolver.getAggrResTime();
		//double[] Rc = mvaSolver.getClsResTime();
		double[][] Rkc = mvaSolver.getResTime();
		
		double[] Uk = mvaSolver.getAggrUtilization();
		double[][] Ukc = mvaSolver.getUtilization();
		
		BoundsSolverMulti boundsSolver = null;
		switch (solverType) {
		case AB:
			boundsSolver = new SolverMultiClosedAB(stations, classes);
			break;
		case BJB:
			boundsSolver = new SolverMultiClosedBJB(stations, classes);
			break;
		}
		
		if (!boundsSolver.input(names, types, s, v, populations)) {
			fail();
		}
		boundsSolver.solve();
		Bound[][] Xkcb = boundsSolver.getThroughputBounds();
		
		Bound[][] Qkcb = boundsSolver.getQueueLenBounds();
		
		Bound[][] Rkcb = boundsSolver.getResTimeBounds();
		
		Bound Ukcb[][] = boundsSolver.getUtilizationBounds();
		
		// Check that bounds encompass the actual exact value given by MVA.
		System.out.println("\n--- Solver under test: " + solverType.toString() + " ---");
		System.out.println("Model: types: " + Arrays.toString(types) + " D: " + Arrays.deepToString(s) + " pop: " + Arrays.toString(populations));
		
		System.out.println("--- Per station per class ---");
		for (int k = 0; k < stations; k++) {
			for (int c = 0; c < classes; c++) {
				System.out.println("k = " + k + " c = " + c);
				checkBound(Xkc[k][c], Xkcb[k][c], "X"+k+","+c);
				checkBound(Qkc[k][c], Qkcb[k][c], "Q"+k+","+c);
				checkBound(Rkc[k][c], Rkcb[k][c], "R"+k+","+c);
				checkBound(Ukc[k][c], Ukcb[k][c], "U"+k+","+c);
			}
		}
		
		System.out.println("--- Per station ---");
		for (int k = 0; k < stations; k++) {
			System.out.println("k = " + k);
			checkBound(Qk[k], Qkcb[k][classes], "Q"+k);
			checkBound(Xk[k], Xkcb[k][classes], "X"+k);
			checkBound(Rk[k], Rkcb[k][classes], "R"+k);
			checkBound(Uk[k], Ukcb[k][classes], "U"+k);
		}
		
		/*
		System.out.println("--- Per class ---");
		//N.B. Classes do not get put back in order after MVA has run so skip.
		for (int c = 0; c < classes; c++) {
			System.out.println("c = " + c); 
			checkBound(Qc[c], Qcb[c], "Q"+c); // Bug in MVA multi which means Q not passed correctly??
			checkBound(Xc[c], Xcb[c], "X"+c);
			checkBound(Rc[c], Rcb[c], "R"+c);
		}
		
		// Don't bother checking because MVA gives strange results -> not same when JMT is run so ok.
		System.out.println("--- Whole system ---");
		checkBound(X, Xb, "X");
		checkBound(Q, Qb, "Q");
		checkBound(R, Rb, "R");
		*/
	}
	
	private void checkBound(double val, Bound b, String s) {
		if (b.LB == -1) return;
		System.out.print("Check: " + s + " " + b.LB + " <= " + val + " <= " + b.UB);
		assertTrue(b.LB >= 0 && b.LB <= (val+TOLERANCE) && (val-TOLERANCE) <= b.UB);
		System.out.println(" CORRECT");
	}
	
}
