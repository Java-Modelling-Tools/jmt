package solvers.multiSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solvers.multiSolverAlgorithms.MultiFixtures.DELAY;
import static solvers.multiSolverAlgorithms.MultiFixtures.LI;
import static solvers.multiSolverAlgorithms.MultiFixtures.closed;
import static solvers.multiSolverAlgorithms.MultiFixtures.solve;

import jmt.common.exception.InputDataException;
import jmt.common.exception.SolverException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import solvers.SolverAlgorithm;
import solvers.multiSolverAlgorithms.MultiFixtures.Result;

/**
 * Closed multiclass models, always solved through {@code SolverMultiDispatcher} as production does.
 *
 * Oracles (derived by hand in the comments): class-independent demands collapse to a single-class network,
 * a single queue plus delay is a small birth-death chain, and the exact algorithms must agree with each other.
 * Exact algorithms: EXACT (MVA) is the reference (1e-12). RECAL and CoMoM scale the service times by 1e5 and
 * truncate them to integers, so they agree with MVA only to about 1e-6 relative (observed 6e-7), hence 1e-5.
 */
class MultiClassClosedTest {

	private static final double EXACT_EPS = 1e-12;
	private static final double SCALED_EPS = 1e-5;

	private static final SolverAlgorithm[] SCALED_EXACT = { SolverAlgorithm.RECAL, SolverAlgorithm.COMOM };

	/**
	 * One queue (demands 1 and 2) and one delay (think times 1 and 3), one customer per class. Product form,
	 * state weights: both think = 1*3, only class 1 queued = D1*Z2 = 3, only class 2 queued = D2*Z1 = 2,
	 * both queued = 2!/(1!1!) * D1*D2 = 4; G(1,1) = 12. G(0,1) = D2 + Z2 = 5, G(1,0) = D1 + Z1 = 2.
	 * X1 = G(0,1)/G = 5/12, X2 = G(1,0)/G = 1/6, queue population of class 1 = (3+4)/12, class 2 = (2+4)/12.
	 */
	private static void assertQueueAndDelayHandValues(Result r, double eps) {
		assertEquals(5.0 / 12, r.x[0][0], eps);
		assertEquals(1.0 / 6, r.x[0][1], eps);
		assertEquals(7.0 / 12, r.q[0][0], eps);
		assertEquals(0.5, r.q[0][1], eps);
		// delay: Q = X*Z
		assertEquals(5.0 / 12, r.q[1][0], eps);
		assertEquals(0.5, r.q[1][1], eps);
		// utilisation law U = X*D at the queue, residence R = Q/X
		assertEquals(5.0 / 12, r.u[0][0], eps);
		assertEquals(1.0 / 3, r.u[0][1], eps);
		assertEquals(1.4, r.r[0][0], eps);
		assertEquals(3.0, r.r[0][1], eps);
		assertEquals(3.0, r.r[1][1], eps);
	}

	private static Result queueAndDelay(SolverAlgorithm alg) throws Exception {
		return solve(closed(new int[] { LI, DELAY }, new double[] { 1, 1 }, new double[][] { { 1, 2 }, { 1, 3 } }), alg);
	}

	@Test
	void mvaMatchesHandComputedQueueAndDelay() throws Exception {
		assertQueueAndDelayHandValues(queueAndDelay(SolverAlgorithm.EXACT), EXACT_EPS);
	}

	@Test
	void recalAndCoMoMMatchHandComputedQueueAndDelay() throws Exception {
		for (SolverAlgorithm a : SCALED_EXACT) {
			assertQueueAndDelayHandValues(queueAndDelay(a), SCALED_EPS);
		}
	}

	/** Class-independent demands, K = 2 identical queues, N = (2,1): single class X = N/(D(N+K-1)) = 3/4. */
	@Test
	void classIndependentBalancedNetworkReducesToSingleClass() throws Exception {
		SolverAlgorithm[] algs = { SolverAlgorithm.EXACT, SolverAlgorithm.RECAL, SolverAlgorithm.COMOM, SolverAlgorithm.TREE_MVA };
		for (SolverAlgorithm a : algs) {
			Result r = solve(closed(new int[] { LI, LI }, new double[] { 2, 1 }, new double[][] { { 1, 1 }, { 1, 1 } }), a);
			double eps = epsFor(a);
			for (int s = 0; s < 2; s++) {
				// throughput splits in proportion to the populations, Q_c = N_c / K
				assertEquals(0.5, r.x[s][0], eps, a + " X0");
				assertEquals(0.25, r.x[s][1], eps, a + " X1");
				assertEquals(1.0, r.q[s][0], eps, a + " Q0");
				assertEquals(0.5, r.q[s][1], eps, a + " Q1");
				assertEquals(2.0, r.r[s][0], eps, a + " R0");
			}
		}
	}

	/**
	 * Class-independent repairman, D = 1, Z = 2, N = 3 split as (2,1). Single class chain p_n ~ N!/(N-n)! (D/Z)^n:
	 * weights 1, 3/2, 3/2, 3/4 (sum 19/4), U = 15/19, X = U/D, queue population 27/19.
	 */
	@Test
	void classIndependentRepairmanReducesToSingleClass() throws Exception {
		SolverAlgorithm[] algs = { SolverAlgorithm.EXACT, SolverAlgorithm.RECAL, SolverAlgorithm.COMOM };
		for (SolverAlgorithm a : algs) {
			Result r = solve(closed(new int[] { LI, DELAY }, new double[] { 2, 1 }, new double[][] { { 1, 1 }, { 2, 2 } }), a);
			double eps = a == SolverAlgorithm.EXACT ? EXACT_EPS : SCALED_EPS;
			double xTot = 15.0 / 19;
			assertEquals(xTot * 2 / 3, r.x[0][0], eps, a + " X0");
			assertEquals(xTot / 3, r.x[0][1], eps, a + " X1");
			assertEquals(27.0 / 19, r.q[0][0] + r.q[0][1], eps, a + " Q");
			assertEquals(r.q[0][0] / 2, r.q[0][1], eps, a + " Q proportional to population");
		}
	}

	/**
	 * Tolerance for the exact algorithms on models with identical stations: MVA and Tree MVA are exact, RECAL
	 * is within its integer scaling, CoMoM is non-deterministic across JVM runs on identical stations (errors up to 2.1e-3 seen, see {@link #coMoMIsExactOnSymmetricNetwork}), so 5e-3.
	 */
	private static double epsFor(SolverAlgorithm a) {
		switch (a) {
		case EXACT:
		case TREE_MVA:
			return EXACT_EPS;
		case COMOM:
			return 5e-3;
		default:
			return SCALED_EPS;
		}
	}

	/**
	 * unverified: CoMoM returns X=0.333183 instead of 1/3 for two identical queues and two identical classes
	 * (N=(1,1), D=1); RECAL and MVA return 1/3. The CoMoM result is non-deterministic across JVM runs (errors up
	 * to 2.1e-3 seen on R, so the 5e-4 here is only one sample). Networks whose stations differ (D=1 and 2) come out right, so the
	 * error is tied to equal stations, but the cause in the CoMoM basis code was not found.
	 */
	@Disabled("unverified: CoMoM is non-deterministic on identical stations, errors up to 2.1e-3 seen")
	@Test
	void coMoMIsExactOnSymmetricNetwork() throws Exception {
		Result r = solve(closed(new int[] { LI, LI }, new double[] { 1, 1 }, new double[][] { { 1, 1 }, { 1, 1 } }), SolverAlgorithm.COMOM);
		assertEquals(1.0 / 3, r.x[0][0], SCALED_EPS);
	}

	/** Without delay stations Tree MVA is exact; with them it is wrong, see {@link #treeMvaIgnoresThinkTime}. */
	@Test
	void treeMvaMatchesMvaWithoutDelayStations() throws Exception {
		double[][] d = { { 1, 2 }, { 3, 1 }, { 0.5, 0.7 } };
		int[] t = { LI, LI, LI };
		Result a = solve(closed(t, new double[] { 3, 2 }, d), SolverAlgorithm.EXACT);
		Result b = solve(closed(t, new double[] { 3, 2 }, d), SolverAlgorithm.TREE_MVA);
		assertSame(a, b, EXACT_EPS * 1e3);
	}

	/**
	 * bug: SolverMultiClosedTreeMVA is reached by the dispatcher for TREE_MVA and DELAY stations become the think time Z of the QNModel,
	 * but nothing under queueingnet/treeAlgorithms reads Z, so the think time never delays the customers
	 * (grep -w Z finds no use); with a delay the queueing is dropped (N=(1,1), D=1,2, Z=1,3 gives X=(0.5,0.25), exact 5/12, 1/6).
	 */
	@Disabled("bug: Tree MVA ignores the think time of delay stations (TreeMVA core never reads QNModel.Z)")
	@Test
	void treeMvaIgnoresThinkTime() throws Exception {
		assertQueueAndDelayHandValues(queueAndDelay(SolverAlgorithm.TREE_MVA), EXACT_EPS);
	}

	/**
	 * Single queue, N = (1,1), D = (1,2): the states are two orderings of the two jobs, G(1,1) = 2*D1*D2,
	 * G(0,1) = D2, so X1 = 1/(2*D1) = 1/2, X2 = 1/(2*D2) = 1/4, utilisation 1, Q_c = N_c.
	 */
	@Test
	void singleStationSaturates() throws Exception {
		// Chow is excluded: it underestimates the throughput at one customer per class (X=1/3, 1/6)
		SolverAlgorithm[] algs = { SolverAlgorithm.EXACT, SolverAlgorithm.RECAL, SolverAlgorithm.COMOM, SolverAlgorithm.TREE_MVA,
				SolverAlgorithm.BARD_SCHWEITZER, SolverAlgorithm.AQL, SolverAlgorithm.LINEARIZER };
		for (SolverAlgorithm a : algs) {
			Result r = solve(closed(new int[] { LI }, new double[] { 1, 1 }, new double[][] { { 1, 2 } }), a);
			// approximate algorithms are inexact with one customer per class: 5% on X
			double eps = a == SolverAlgorithm.EXACT || a == SolverAlgorithm.TREE_MVA ? EXACT_EPS
					: a == SolverAlgorithm.RECAL || a == SolverAlgorithm.COMOM ? SCALED_EPS : 0.05;
			assertEquals(0.5, r.x[0][0], eps, a + " X0");
			assertEquals(0.25, r.x[0][1], eps, a + " X1");
			assertEquals(1.0, r.u[0][0] + r.u[0][1], eps, a + " U");
			assertEquals(1.0, r.q[0][0], eps, a + " Q0");
			assertEquals(1.0, r.q[0][1], eps, a + " Q1");
		}
	}

	/** Delay only: nobody waits, X_c = N_c/Z_c, Q_c = N_c, R = Z. The AMVA solvers and MVA accept it. */
	@Test
	void delayOnlyNetwork() throws Exception {
		SolverAlgorithm[] algs = { SolverAlgorithm.EXACT, SolverAlgorithm.CHOW, SolverAlgorithm.BARD_SCHWEITZER, SolverAlgorithm.AQL,
				SolverAlgorithm.LINEARIZER, SolverAlgorithm.DESOUZA_MUNTZ_LINEARIZER };
		for (SolverAlgorithm a : algs) {
			Result r = solve(closed(new int[] { DELAY }, new double[] { 2, 3 }, new double[][] { { 1.5, 2 } }), a);
			assertEquals(2 / 1.5, r.x[0][0], 1e-9, a + " X0");
			assertEquals(3 / 2.0, r.x[0][1], 1e-9, a + " X1");
			assertEquals(2.0, r.q[0][0], 1e-9, a + " Q0");
			assertEquals(3.0, r.q[0][1], 1e-9, a + " Q1");
			assertEquals(1.5, r.r[0][0], 1e-9, a + " R0");
		}
	}

	/**
	 * Pinned only as current behaviour: RECAL, CoMoM, Tree MVA and logistic sampling reject a network with no queueing
	 * station (input() fails). A delay-only closed model is legitimate, so the rejection may itself be a defect.
	 */
	@Test
	void delayOnlyNetworkIsRejectedByQueueBasedSolvers() {
		SolverAlgorithm[] algs = { SolverAlgorithm.RECAL, SolverAlgorithm.COMOM, SolverAlgorithm.TREE_MVA, SolverAlgorithm.MONTE_CARLO_LOGISTIC };
		for (SolverAlgorithm a : algs) {
			assertThrows(SolverException.class,
					() -> solve(closed(new int[] { DELAY }, new double[] { 2, 3 }, new double[][] { { 1.5, 2 } }), a), a.toString());
		}
	}

	/** Two identical queues and identical classes (symmetric): every Q, X, R is the same in each cell. */
	@Test
	void identicalStationsGiveIdenticalMeasures() throws Exception {
		for (SolverAlgorithm a : new SolverAlgorithm[] { SolverAlgorithm.EXACT, SolverAlgorithm.RECAL, SolverAlgorithm.COMOM,
				SolverAlgorithm.TREE_MVA, SolverAlgorithm.BARD_SCHWEITZER }) {
			Result r = solve(closed(new int[] { LI, LI, LI }, new double[] { 2, 2 }, new double[][] { { 1, 1 }, { 1, 1 }, { 1, 1 } }), a);
			// N = 4 customers over 3 equal queues, single class: X = N/(D(N+K-1)) = 4/6, Q = 4/3 per queue
			// Bard-Schweitzer: observed error is below 1e-12 here (the symmetric fixed point is exact), so 1e-9
			double eps = a == SolverAlgorithm.BARD_SCHWEITZER ? 1e-9 : epsFor(a);
			for (int s = 0; s < 3; s++) {
				for (int c = 0; c < 2; c++) {
					assertEquals(1.0 / 3, r.x[s][c], eps, a + " X");
					assertEquals(2.0 / 3, r.q[s][c], eps, a + " Q");
				}
			}
		}
	}

	/** Pinned: the dispatcher refuses a class with zero population before any solver runs. */
	@Test
	void zeroPopulationClassIsRejected() {
		InputDataException e = assertThrows(InputDataException.class,
				() -> solve(closed(new int[] { LI }, new double[] { 0, 2 }, new double[][] { { 1, 2 } }), SolverAlgorithm.EXACT));
		assertEquals("Populations must be greater than zero", e.getMessage());
	}

	// ----- exact algorithms agree with each other; invariants -----

	private static final int[] THREE_STATIONS = { LI, LI, DELAY };
	private static final double[][] DEMAND_3x3 = { { 0.4, 0.2, 0.3 }, { 0.3, 0.5, 0.1 }, { 1.0, 2.0, 1.5 } };

	@Test
	void exactAlgorithmsAgreeOnThreeClasses() throws Exception {
		double[] pops = { 3, 2, 2 };
		Result mva = solve(closed(THREE_STATIONS, pops, DEMAND_3x3), SolverAlgorithm.EXACT);
		for (SolverAlgorithm a : SCALED_EXACT) {
			assertSame(mva, solve(closed(THREE_STATIONS, pops, DEMAND_3x3), a), SCALED_EPS);
		}
	}

	@Test
	void exactAlgorithmsAgreeWithoutDelay() throws Exception {
		int[] t = { LI, LI, LI };
		double[] pops = { 3, 2, 2 };
		Result mva = solve(closed(t, pops, DEMAND_3x3), SolverAlgorithm.EXACT);
		for (SolverAlgorithm a : new SolverAlgorithm[] { SolverAlgorithm.RECAL, SolverAlgorithm.COMOM, SolverAlgorithm.TREE_MVA }) {
			double eps = a == SolverAlgorithm.TREE_MVA ? 1e-9 : SCALED_EPS;
			assertSame(mva, solve(closed(t, pops, DEMAND_3x3), a), eps);
		}
	}

	/** Utilisation law, Little's law per class (sum of Q + X*Z = N), forced flow. Holds for MVA exactly. */
	@Test
	void operationalLawsHoldForMva() throws Exception {
		double[] pops = { 3, 2, 2 };
		Result r = solve(closed(THREE_STATIONS, pops, DEMAND_3x3), SolverAlgorithm.EXACT);
		for (int c = 0; c < 3; c++) {
			double total = 0;
			for (int s = 0; s < 3; s++) {
				total += r.q[s][c];
				assertEquals(r.x[s][c] * DEMAND_3x3[s][c], r.u[s][c], 1e-12, "U=XD s" + s + " c" + c);
				assertEquals(r.q[s][c], r.x[s][c] * r.r[s][c], 1e-12, "Little s" + s + " c" + c);
				assertTrue(r.q[s][c] >= 0 && !Double.isNaN(r.q[s][c]));
			}
			assertEquals(pops[c], total, 1e-12, "population of class " + c);
		}
		// unit visits: all stations of a class see the same throughput
		for (int c = 0; c < 3; c++) {
			assertEquals(r.x[0][c], r.x[1][c], 1e-12);
			assertEquals(r.x[0][c], r.x[2][c], 1e-12);
		}
		for (int s = 0; s < 2; s++) {
			assertTrue(r.u[s][0] + r.u[s][1] + r.u[s][2] <= 1 + 1e-12);
		}
	}

	// ----- approximations against exact MVA -----
	//
	// Tolerance per algorithm, relative error of the worst Q and X cell on the models below ((5,3), (2,2) and (12,9)
	// customers on 2 queues + 1 delay). Observed worst case in brackets; the limit is the observed value with a
	// margin of 1.3 to 2, and each is also in line with the literature for the algorithm:
	//  - Chow:             Q 0.25 [0.20], X 0.15 [0.13]: the crudest AMVA, known errors of 10-20% on small populations.
	//  - Bard-Schweitzer:  Q 0.20 [0.154], X 0.06 [0.038]: fixed point (N_c-1)/N_c scaling, queue error up to ~15%.
	//  - AQL:              Q 0.03 [0.015], X 0.01 [0.003]: corrects the Schweitzer scaling, within a few percent.
	//  - Linearizer and De Souza-Muntz: Q 0.05 [0.028], X 0.01 [0.006]: error under ~3% (identical results here).

	private static void approximationWithin(SolverAlgorithm alg, double qTol, double xTol) throws Exception {
		double[][] d = { { 0.4, 0.2 }, { 0.3, 0.5 }, { 1.0, 2.0 } };
		for (double[] pop : new double[][] { { 5, 3 }, { 2, 2 }, { 12, 9 } }) {
			Result ex = solve(closed(THREE_STATIONS, pop, d), SolverAlgorithm.EXACT);
			Result ap = solve(closed(THREE_STATIONS, pop, d), alg);
			for (int s = 0; s < 3; s++) {
				for (int c = 0; c < 2; c++) {
					assertEquals(ex.q[s][c], ap.q[s][c], qTol * ex.q[s][c], alg + " Q s" + s + " c" + c + " pop " + pop[0]);
					assertEquals(ex.x[s][c], ap.x[s][c], xTol * ex.x[s][c], alg + " X s" + s + " c" + c + " pop " + pop[0]);
				}
			}
		}
	}

	@Test
	void chowWithinTolerance() throws Exception {
		approximationWithin(SolverAlgorithm.CHOW, 0.25, 0.15);
	}

	@Test
	void bardSchweitzerWithinTolerance() throws Exception {
		approximationWithin(SolverAlgorithm.BARD_SCHWEITZER, 0.20, 0.06);
	}

	@Test
	void aqlWithinTolerance() throws Exception {
		approximationWithin(SolverAlgorithm.AQL, 0.03, 0.01);
	}

	@Test
	void linearizerWithinTolerance() throws Exception {
		approximationWithin(SolverAlgorithm.LINEARIZER, 0.05, 0.01);
	}

	@Test
	void deSouzaMuntzLinearizerWithinTolerance() throws Exception {
		approximationWithin(SolverAlgorithm.DESOUZA_MUNTZ_LINEARIZER, 0.05, 0.01);
	}

	/**
	 * Logistic sampling is randomised and cannot be seeded from outside (its generators are seeded from the clock),
	 * hence slow-tagged with a wide tolerance. N=(1,1), D=(1,2), single queue: exact X = (1/2, 1/4); observed error 1e-4.
	 */
	@org.junit.jupiter.api.Tag("slow")
	@Test
	void logisticSamplingNearExactOnSingleQueue() throws Exception {
		Result r = solve(closed(new int[] { LI }, new double[] { 1, 1 }, new double[][] { { 1, 2 } }), SolverAlgorithm.MONTE_CARLO_LOGISTIC);
		assertEquals(0.5, r.x[0][0], 0.05);
		assertEquals(0.25, r.x[0][1], 0.05);
	}

	static void assertSame(Result a, Result b, double eps) {
		for (int s = 0; s < a.q.length; s++) {
			for (int c = 0; c < a.q[s].length; c++) {
				// relative: residence times reach 10+, so the scaled algorithms differ by eps*value
				assertEquals(a.q[s][c], b.q[s][c], eps * Math.max(1, a.q[s][c]), "Q s" + s + " c" + c);
				assertEquals(a.x[s][c], b.x[s][c], eps * Math.max(1, a.x[s][c]), "X s" + s + " c" + c);
				assertEquals(a.r[s][c], b.r[s][c], eps * Math.max(1, a.r[s][c]), "R s" + s + " c" + c);
				assertEquals(a.u[s][c], b.u[s][c], eps * Math.max(1, a.u[s][c]), "U s" + s + " c" + c);
			}
		}
	}
}
