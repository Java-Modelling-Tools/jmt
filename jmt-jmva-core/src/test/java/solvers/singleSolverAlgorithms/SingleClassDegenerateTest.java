package solvers.singleSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.singleSolverAlgorithms.SolverFixtures.*;

import org.junit.jupiter.api.Test;

import solvers.Solver;
import solvers.singleSolverAlgorithms.SolverFixtures.Algo;
import solvers.singleSolverAlgorithms.SolverFixtures.Model;

/** Degenerate populations and demands: results must stay finite and non-negative, and equal the obvious limit. */
class SingleClassDegenerateTest {

	/** No customers: nothing is queued, nothing is served. R is 0/0 and not asserted. */
	@Test
	void emptyNetwork() throws Exception {
		Model m = repairman(0, 1, 2);
		for (Algo a : WORKING) {
			Solver s = solve(a, m);
			for (int i = 0; i < m.stations(); i++) {
				assertEquals(0.0, s.getQueueLen(i), a.tol(), a + " Q" + i);
				assertEquals(0.0, s.getUtilization(i), a.tol(), a + " U" + i);
				assertEquals(0.0, s.getThroughput(i), a.tol(), a + " X" + i);
			}
		}
		assertEquals(0.0, solve(MVA, m).getTotThroughput(), 0.0);
	}

	/** All demands zero: customers finish instantly, X = 0 (the solver defines it that way), no NaN in Q, U, X. */
	@Test
	void allDemandsZero() throws Exception {
		Model m = liModel(3, new int[] { Solver.LI, Solver.LI }, 0, 0);
		Solver s = solve(MVA, m);
		assertEquals(0.0, s.getTotThroughput(), 0.0);
		for (int i = 0; i < 2; i++) {
			assertEquals(0.0, s.getQueueLen(i), 0.0);
			assertEquals(0.0, s.getUtilization(i), 0.0);
			assertEquals(0.0, s.getThroughput(i), 0.0);
		}
	}

	/** One customer, one station: R = D, U = 1 on every working algorithm and on convolution. */
	@Test
	void oneCustomerOneStation() throws Exception {
		Model m = liModel(1, new int[] { Solver.LI }, 4);
		for (Algo a : LI_ONLY) {
			assertMeasures(a, solve(a, m), sysX(a, 0.25), new double[] { 1 }, new double[] { 4 }, new double[] { 1 });
		}
	}

	/** A delay-only network: X = N/Z, nobody queues anywhere. */
	@Test
	void delayOnlyNetwork() throws Exception {
		Model m = liModel(4, new int[] { Solver.DELAY }, 2);
		Solver s = solve(MVA, m);
		assertEquals(2.0, s.getTotThroughput(), 1e-12);
		assertEquals(4.0, s.getQueueLen(0), 1e-12);
		assertEquals(2.0, s.getResTime(0), 1e-12);
	}

	/** Very large demand ratio keeps the results finite: a 1e6:1 imbalance, X -> 1/Dmax. */
	@Test
	void extremeDemandRatio() throws Exception {
		Model m = liModel(10, new int[] { Solver.LI, Solver.LI }, 1000, 0.001);
		for (Algo a : new Algo[] { MVA, CONVOLUTION }) {
			Solver s = solve(a, m);
			assertEquals(1e-3, s.getThroughput(0), 1e-9, a + " X");
			assertEquals(10.0, s.getQueueLen(0), 1e-5, a + " all customers at the slow station");
			assertEquals(0.0, s.getQueueLen(1), 1e-5, a + " none at the fast one");
		}
	}

	/** Wrong station counts are refused by every algorithm and nothing is solved. */
	@Test
	void inputWithWrongStationCountIsRejected() throws Exception {
		Model m = balanced(2, 3, 1);
		String[] names = { "a", "b" };
		int[] types = { Solver.LI, Solver.LI };
		double[][] serv = { li(1), li(1) };
		double[] visits = { 1, 1 };
		for (Algo a : new Algo[] { CONVOLUTION, COMOM, RECAL }) {
			assertFalse(a.make(m.customers, m.stations()).input(names, types, serv, visits), a.name());
		}
		assertFalse(MVA.make(2, 1).input(names, types, serv, visits), "MVA refuses more stations than it was built for");
	}

	/** LD stations need N+1 service times in MVA; any other length is rejected. */
	@Test
	void loadDependentServiceArrayLengthIsChecked() throws Exception {
		Solver s = MVA.make(3, 1);
		assertFalse(s.input(new String[] { "a" }, new int[] { Solver.LD }, new double[][] { { 0, 1, 1 } }, new double[] { 1 }));
		assertTrue(s.input(new String[] { "a" }, new int[] { Solver.LD }, new double[][] { { 0, 1, 1, 1 } }, new double[] { 1 }));
		// the plain single-class base input does not support LD at all
		assertFalse(CONVOLUTION.make(3, 1).input(new String[] { "a" }, new int[] { Solver.LD }, new double[][] { { 0, 1, 1, 1 } }, new double[] { 1 }));
	}
}
