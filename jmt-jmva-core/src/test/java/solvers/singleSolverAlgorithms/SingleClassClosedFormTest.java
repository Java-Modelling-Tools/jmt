package solvers.singleSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.singleSolverAlgorithms.SolverFixtures.*;

import org.junit.jupiter.api.Test;

import solvers.Solver;
import solvers.singleSolverAlgorithms.SolverFixtures.Algo;
import solvers.singleSolverAlgorithms.SolverFixtures.Model;

/**
 * Single-class algorithms against closed forms worked out by hand.
 * X is the system throughput, Q the queue length, R the residence time (= Q/X), U the utilisation (= X*D).
 * Models without delay stations and with unit visits run on MVA, convolution and RECAL; the others on MVA and RECAL
 * (convolution ignores delay stations and misreports visits, CoMoM cannot run a single class: SingleClassBugsTest).
 */
class SingleClassClosedFormTest {

	private static void check(Algo a, Solver s, double x, double[] q, double[] r, double[] u) {
		assertMeasures(a, s, sysX(a, x), q, r, u);
	}

	/** One queueing station, N customers, demand D: the station is never idle: X = 1/D, Q = N, R = N*D, U = 1. */
	@Test
	void singleStationHoldsEveryone() throws Exception {
		Model m = liModel(5, new int[] { Solver.LI }, 2.0);
		for (Algo a : LI_ONLY) {
			check(a, solve(a, m), 0.5, new double[] { 5 }, new double[] { 10 }, new double[] { 1 });
		}
	}

	/**
	 * Machine repairman N=2, D=1, Z=1. Birth-death: p_n ~ N!/(N-n)! (D/Z)^n = 1, 2, 2 so p0 = 1/5.
	 * U = 1-p0 = 0.8, X = U/D = 0.8, Q_delay = X*Z = 0.8, Q_station = N - X*Z = 1.2, R_station = Q/X = 1.5.
	 */
	@Test
	void machineRepairmanTwoCustomers() throws Exception {
		Model m = repairman(2, 1, 1);
		for (Algo a : WORKING) {
			Solver s = solve(a, m);
			check(a, s, 0.8, new double[] { 1.2, 0.8 }, new double[] { 1.5, 1.0 }, new double[] { 0.8, 0.8 });
			assertPopulationIdentity(a.name(), s, m, a.tol());
		}
	}

	/**
	 * Machine repairman N=3, D=1, Z=2. Terms N!/(N-n)! (D/Z)^n = 1, 1.5, 1.5, 0.75, sum 4.75, p0 = 4/19.
	 * U = X = 15/19, Q_delay = X*Z = 30/19, Q_station = 3 - 30/19 = 27/19, R_station = Q/X = 27/15 = 1.8.
	 * The delay "utilisation" is X*Z (mean busy servers), not capped at 1.
	 */
	@Test
	void machineRepairmanThreeCustomers() throws Exception {
		Model m = repairman(3, 1, 2);
		double x = 15.0 / 19;
		for (Algo a : WORKING) {
			Solver s = solve(a, m);
			check(a, s, x, new double[] { 27.0 / 19, 30.0 / 19 }, new double[] { 1.8, 2.0 }, new double[] { x, x * 2 });
			assertPopulationIdentity(a.name(), s, m, a.tol());
		}
	}

	/** Repairman closed form over a grid: p0 = 1 / sum_n N!/(N-n)! (D/Z)^n, X = (1-p0)/D, sum(Q) = N - X*Z. */
	@Test
	void machineRepairmanOverGrid() throws Exception {
		double d = 0.5;
		double z = 1.5;
		for (int n = 1; n <= 9; n += 2) {
			double term = 1;
			double sum = 1;
			for (int j = 1; j <= n; j++) {
				term *= (n - j + 1) * d / z;
				sum += term;
			}
			double x = (1 - 1 / sum) / d;
			Model m = repairman(n, d, z);
			for (Algo a : WORKING) {
				Solver s = solve(a, m);
				check(a, s, x, new double[] { n - x * z, x * z }, new double[] { (n - x * z) / x, z }, new double[] { x * d, x * z });
				assertPopulationIdentity(a.name(), s, m, a.tol());
			}
		}
	}

	/**
	 * K=3 identical stations, D=2, N=4: X = N/(D(N+K-1)) = 4/12 = 1/3, Q = N/K = 4/3 each,
	 * R = Q/X = D(N+K-1)/K = 4, U = X*D = 2/3.
	 */
	@Test
	void balancedNetwork() throws Exception {
		Model m = balanced(4, 3, 2);
		double[] q = { 4.0 / 3, 4.0 / 3, 4.0 / 3 };
		for (Algo a : LI_ONLY) {
			Solver s = solve(a, m);
			check(a, s, 1.0 / 3, q, new double[] { 4, 4, 4 }, new double[] { 2.0 / 3, 2.0 / 3, 2.0 / 3 });
			assertPopulationIdentity(a.name(), s, m, a.tol());
		}
	}

	/** Balanced formula over a range of K and N: X = N/(D(N+K-1)), Q = N/K, U = N/(N+K-1). */
	@Test
	void balancedFormulaOverGrid() throws Exception {
		double d = 0.75;
		for (int k = 1; k <= 4; k++) {
			for (int n = 1; n <= 8; n += 3) {
				Model m = balanced(n, k, d);
				double x = n / (d * (n + k - 1));
				for (Algo a : LI_ONLY) {
					Solver s = solve(a, m);
					for (int i = 0; i < k; i++) {
						assertEquals((double) n / k, s.getQueueLen(i), a.tol(), a + " Q, K=" + k + " N=" + n);
						assertEquals((double) n / (n + k - 1), s.getUtilization(i), a.tol(), a + " U, K=" + k + " N=" + n);
						assertEquals(x, s.getThroughput(i), a.tol(), a + " station throughput, K=" + k + " N=" + n);
					}
					if (!Double.isNaN(sysX(a, x))) {
						assertEquals(x, s.getTotThroughput(), a.tol(), a + " X, K=" + k + " N=" + n);
					}
				}
			}
		}
	}

	/** One customer never queues: X = 1/sum(D), R_k = D_k, Q_k = U_k = D_k/sum(D). Includes a delay station. */
	@Test
	void singleCustomer() throws Exception {
		Model m = liModel(1, new int[] { Solver.LI, Solver.LI, Solver.DELAY }, 1, 2, 3);
		double[] share = { 1.0 / 6, 2.0 / 6, 3.0 / 6 };
		for (Algo a : WORKING) {
			Solver s = solve(a, m);
			check(a, s, 1.0 / 6, share, new double[] { 1, 2, 3 }, share);
			assertPopulationIdentity(a.name(), s, m, a.tol());
		}
	}

	/**
	 * One customer and queueing stations only, convolution included. The slowest station comes first: convolution
	 * moves it to index 0 and does not move it back (SingleClassBugsTest).
	 */
	@Test
	void singleCustomerQueueingOnly() throws Exception {
		Model m = liModel(1, new int[] { Solver.LI, Solver.LI }, 3, 1);
		for (Algo a : LI_ONLY) {
			check(a, solve(a, m), 0.25, new double[] { 0.75, 0.25 }, new double[] { 3, 1 }, new double[] { 0.75, 0.25 });
		}
	}

	/** A station with zero demand is never loaded: Q = R = U = 0 and the rest of the network is unchanged. */
	@Test
	void zeroDemandStationChangesNothing() throws Exception {
		Model m = liModel(3, new int[] { Solver.LI, Solver.DELAY, Solver.LI }, 1, 2, 0);
		double x = 15.0 / 19;
		for (Algo a : WORKING) {
			check(a, solve(a, m), x, new double[] { 27.0 / 19, 30.0 / 19, 0 }, new double[] { 1.8, 2.0, 0 }, new double[] { x, 2 * x, 0 });
		}
	}

	/** Zero demand among queueing stations only (runs on convolution too): same as the network without it. */
	@Test
	void zeroDemandQueueingStation() throws Exception {
		// two queueing stations D=2,1, N=1: X = 1/3; a third station with D=0 changes nothing
		Model m = liModel(1, new int[] { Solver.LI, Solver.LI, Solver.LI }, 2, 1, 0);
		for (Algo a : LI_ONLY) {
			check(a, solve(a, m), 1.0 / 3, new double[] { 2.0 / 3, 1.0 / 3, 0 }, new double[] { 2, 1, 0 }, new double[] { 2.0 / 3, 1.0 / 3, 0 });
		}
	}

	/** Visits scale demand: V=2, S=0.5 behaves like D=1, and the per-station throughput is X*V. */
	@Test
	void visitsMultiplyServiceTime() throws Exception {
		Model m = new Model(2, new int[] { Solver.LI, Solver.DELAY }, new double[][] { li(0.5), li(1) }, new double[] { 2, 1 });
		for (Algo a : WORKING) {
			Solver s = solve(a, m);
			check(a, s, 0.8, new double[] { 1.2, 0.8 }, new double[] { 1.5, 1.0 }, new double[] { 0.8, 0.8 });
			assertEquals(1.6, s.getThroughput(0), a.tol(), a + " throughput of the station visited twice");
			assertEquals(0.8, s.getThroughput(1), a.tol(), a + " throughput of the station visited once");
		}
	}

	/** Results are finite, non-negative and utilisation of queueing stations is at most 1 for several shapes. */
	@Test
	void resultsAreFiniteAndNonNegative() throws Exception {
		Model[] models = { balanced(1, 1, 1), repairman(1, 1, 5), repairman(7, 0.1, 0.1), balanced(6, 4, 3) };
		for (Model m : models) {
			for (Algo a : WORKING) {
				Solver s = solve(a, m);
				for (int i = 0; i < m.stations(); i++) {
					for (double v : new double[] { s.getQueueLen(i), s.getResTime(i), s.getUtilization(i), s.getThroughput(i) }) {
						assertTrue(v >= 0 && !Double.isInfinite(v) && !Double.isNaN(v), a + " station " + i + " value " + v);
					}
					if (m.types[i] == Solver.LI) {
						assertTrue(s.getUtilization(i) <= 1 + a.tol(), a + " utilisation <= 1");
					}
				}
			}
		}
	}

	/** At high population the slowest station dominates: X -> 1/Dmax, U_max -> 1, U_other -> Dother/Dmax. */
	@Test
	void bottleneckSaturates() throws Exception {
		Model m = liModel(60, new int[] { Solver.LI, Solver.LI }, 1.0, 0.5);
		for (Algo a : LI_ONLY) {
			Solver s = solve(a, m);
			assertEquals(1.0, s.getUtilization(0), 1e-6, a + " U bottleneck");
			assertEquals(0.5, s.getUtilization(1), 1e-6, a + " U other");
			assertEquals(1.0, s.getThroughput(0), 1e-5, a + " X -> 1/Dmax");
		}
	}

	/** MVA and RECAL agree on an unbalanced network with a delay that has no handy closed form. */
	@Test
	void mvaAndRecalAgreeOnUnbalancedModel() throws Exception {
		Model m = liModel(6, new int[] { Solver.LI, Solver.LI, Solver.DELAY, Solver.LI }, 1.3, 0.7, 4.0, 2.1);
		Solver ref = solve(MVA, m);
		Solver rec = solve(RECAL, m);
		assertMeasures(RECAL, rec, Double.NaN, ref.getQueueLen(), ref.getResTime(), ref.getUtilization());
		assertPopulationIdentity("MVA", ref, m, 1e-9);
		assertPopulationIdentity("RECAL", rec, m, 1e-5);
	}
}
