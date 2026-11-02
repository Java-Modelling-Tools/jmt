package solvers.priority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solvers.multiSolverAlgorithms.MultiFixtures.CLOSED;
import static solvers.multiSolverAlgorithms.MultiFixtures.DELAY;
import static solvers.multiSolverAlgorithms.MultiFixtures.OPEN;
import static solvers.multiSolverAlgorithms.MultiFixtures.PRS;
import static solvers.multiSolverAlgorithms.MultiFixtures.closed;
import static solvers.multiSolverAlgorithms.MultiFixtures.model;
import static solvers.multiSolverAlgorithms.MultiFixtures.solve;

import analytical.ExactModel;
import jmt.common.exception.UnsupportedModelException;
import org.junit.jupiter.api.Test;
import solvers.SolverAlgorithm;
import solvers.multiSolverAlgorithms.MultiFixtures.Result;

/**
 * Priority algorithms (SCA shadow server, Chandy-Lakshmi, Bryant-Krzesinski-Teunissen) through the dispatcher
 * on preemptive-resume (PRS) stations. Higher priority number = higher priority.
 */
class PriorityDispatchTest {

	private static final SolverAlgorithm[] PRIORITY = { SolverAlgorithm.SCA, SolverAlgorithm.PRIO_MVA_CL, SolverAlgorithm.PRIO_MVA_BKT };

	/** PRS queue D = (0.5, 0.4), delay Z = (1.5, 1); N = (1, 2); class 1 has priority 5, class 2 priority 4. */
	private static ExactModel twoClassPrs() {
		ExactModel m = closed(new int[] { PRS, DELAY }, new double[] { 1, 2 }, new double[][] { { 0.5, 0.4 }, { 1.5, 1 } });
		m.setClassPriorities(new int[] { 5, 4 });
		return m;
	}

	/**
	 * Preemptive priority: the highest class never sees the other class. With one customer it cannot queue behind
	 * itself either, so the cycle is D + Z = 0.5 + 1.5 = 2 and X_1 = 1/2, residence 0.5, Q = (1/4, 3/4), exactly,
	 * for every priority algorithm.
	 */
	@Test
	void highestPriorityClassIgnoresLowerClass() throws Exception {
		for (SolverAlgorithm a : PRIORITY) {
			Result r = solve(twoClassPrs(), a);
			assertEquals(0.5, r.x[0][0], 1e-9, a + " X");
			assertEquals(0.5, r.r[0][0], 1e-9, a + " R");
			assertEquals(0.25, r.q[0][0], 1e-9, a + " Q queue");
			assertEquals(0.75, r.q[1][0], 1e-9, a + " Q delay");
			assertEquals(0.25, r.u[0][0], 1e-9, a + " U");
		}
	}

	/**
	 * The lower class sees a slower server. Bounds that hold for any correct result: it is no faster than
	 * on an empty server (single class N=2, D=0.4, Z=1: weights 1, 0.8, 0.32, X = 1.12/2.12/0.4), and population
	 * balance Q + X*Z = N, total utilisation at most 1, no NaN.
	 */
	@Test
	void lowerPriorityClassIsBoundedByEmptyServer() throws Exception {
		double empty = (1.12 / 2.12) / 0.4;
		for (SolverAlgorithm a : PRIORITY) {
			Result r = solve(twoClassPrs(), a);
			double x2 = r.x[0][1];
			assertTrue(x2 > 0 && x2 < empty, a + " X2=" + x2);
			assertEquals(2.0, r.q[0][1] + r.q[1][1], 1e-6, a + " population");
			assertEquals(x2 * 1.0, r.q[1][1], 1e-6, a + " delay Q = X*Z");
			assertTrue(r.u[0][0] + r.u[0][1] <= 1 + 1e-9, a + " utilisation");
		}
	}

	/**
	 * Shadow-server definition: the lower class sees the server slowed by the higher class utilisation
	 * U_1 = 1/4, i.e. D_2/(1-U_1) = 0.4/0.75 = 8/15. Repairman N=2, D/Z = 8/15: weights 1, 16/15, 128/225, sum 2.63556,
	 * U = 1.63556/2.63556, X_2 = U*15/8 = 1.16357. SCA iterates to a convergence tolerance (0.005, one-sided),
	 * but the higher class throughput is constant so it lands on this value; 1e-4 is checked.
	 */
	@Test
	void shadowServerLowerClassMatchesInflatedRepairman() throws Exception {
		double r = 8.0 / 15;
		double w1 = 2 * r, w2 = 2 * r * r;
		double u = (w1 + w2) / (1 + w1 + w2);
		Result res = solve(twoClassPrs(), SolverAlgorithm.SCA);
		assertEquals(u / r, res.x[0][1], 1e-4);
	}

	/**
	 * Open class with the top priority at a PRS queue and a closed class below: preemption makes the open class an
	 * ordinary M/M/1, R = D/(1-lambda*D) = 0.4/(1-0.5*0.4) = 0.5, Q = lambda*R = 0.25, whatever the closed class does.
	 */
	@Test
	void highestPriorityOpenClassSeesPlainMM1() throws Exception {
		for (SolverAlgorithm a : PRIORITY) {
			ExactModel m = model(new int[] { PRS, DELAY }, new int[] { OPEN, CLOSED }, new double[] { 0.5, 2 },
					new double[][] { { 0.4, 0.3 }, { 0.2, 1 } });
			m.setClassPriorities(new int[] { 5, 4 });
			Result r = solve(m, a);
			assertEquals(0.5, r.r[0][0], 1e-9, a + " R");
			assertEquals(0.25, r.q[0][0], 1e-9, a + " Q");
			assertEquals(0.5, r.x[0][0], 1e-9, a + " X");
		}
	}

	/** Pinned: algorithms without priority support refuse a model that has a priority (PRS) station. */
	@Test
	void nonPriorityAlgorithmsRefusePriorityStations() {
		for (SolverAlgorithm a : new SolverAlgorithm[] { SolverAlgorithm.EXACT, SolverAlgorithm.RECAL, SolverAlgorithm.BARD_SCHWEITZER }) {
			UnsupportedModelException e = assertThrows(UnsupportedModelException.class, () -> solve(twoClassPrs(), a), a.toString());
			assertEquals("The selected solver cannot handle priority stations, please choose another.", e.getMessage());
		}
	}
}
