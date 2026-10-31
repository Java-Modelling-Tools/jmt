package solvers.singleSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.singleSolverAlgorithms.SolverFixtures.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import solvers.Solver;
import solvers.singleSolverAlgorithms.SolverFixtures.Model;

/**
 * Closed-form expectations that the single-class algorithms currently fail. Each test asserts the correct value
 * worked out by hand and is disabled with the cause found by reading the code. Most of them cover classes the
 * dispatcher never builds (SolverSingleClosedExact, marked NOT USED), so they are "unused-code defect"s, not shipped bugs.
 * Labels: "unused-code defect" (class unreachable from production), "latent" (reachable, but no caller reads the result).
 * Not tested: SolverSingleClosedCoMoM is unreachable (the dispatcher maps single-class CoMoM to RECAL) and cannot run
 * a single class (CoMoMBasis.generate calls binomialCoefficient(i-1, i) for R = 1); N = 0 is rejected by the dispatcher.
 */
class SingleClassBugsTest {

	/** System throughput and response time of repairman N=2, D=1, Z=1: X = 0.8, N/X = 2.5, through RECAL and CoMoM. */
	@Disabled("latent: SolverMultiClosedRECAL/CoMoM.solve end with sysThroughput = sysNumJobs / sysResponseTime where both are the "
			+ "zero they were initialised to (the accumulation loop is commented out): getTotThroughput() is NaN, getTotResTime() 0; no production code reads either getter")
	@Test
	void recalReportsSystemThroughputAndResponseTime() throws Exception {
		Solver s = solve(RECAL, repairman(2, 1, 1));
		assertEquals(0.8, s.getTotThroughput(), RECAL.tol());
		assertEquals(2.5, s.getTotResTime(), RECAL.tol());
	}

	/** Convolution on the repairman N=2, D=1, Z=1: the delay carries 0.8 customers: X = 0.8, Q = (1.2, 0.8). */
	@Disabled("unused-code defect: SolverSingleClosedExact.solve only multiplies LI and LD stations into G, so DELAY stations are silently ignored "
			+ "(the model is solved as the queueing station alone: X = 1, Q = 2)")
	@Test
	void convolutionAccountsForDelayStations() throws Exception {
		Solver s = solve(CONVOLUTION, repairman(2, 1, 1));
		assertMeasures(CONVOLUTION, s, 0.8, new double[] { 1.2, 0.8 }, new double[] { 1.5, 1.0 }, new double[] { 0.8, 0.8 });
	}

	/** D = (1, 3), N = 1: Q = (0.25, 0.75) in the input order. */
	@Disabled("unused-code defect: SolverSingleClosedExact.staticScale/reorder swaps the slowest station into index 0 and nothing restores the "
			+ "order (SolverSingleClosedMVA.reorder does), while the names array has no public getter: results come back permuted")
	@Test
	void convolutionKeepsStationOrder() throws Exception {
		Solver s = solve(CONVOLUTION, liModel(1, new int[] { Solver.LI, Solver.LI }, 1, 3));
		assertEquals(0.25, s.getQueueLen(0), 1e-12);
		assertEquals(0.75, s.getQueueLen(1), 1e-12);
	}

	/**
	 * Two queueing stations, V=(2, 1), S=(0.5, 0.5), so D=(1, 0.5), N=2. MVA by hand: n=1: R=(1, 0.5), X=2/3, Q=(2/3, 1/3);
	 * n=2: R=(1*(1+2/3), 0.5*(1+1/3)) = (5/3, 2/3), X=2/(7/3)=6/7, Q=(10/7, 4/7), U=X*D=(6/7, 3/7).
	 */
	@Disabled("unused-code defect: SolverSingleClosedExact.indices computes residenceTime = Q / (V*G(N-1)/G(N)) per visit but totRespTime sums these "
			+ "without the visit counts, so X = N / sum(R) is wrong whenever V != 1")
	@Test
	void convolutionHonoursVisitCounts() throws Exception {
		Model m = new Model(2, new int[] { Solver.LI, Solver.LI }, new double[][] { li(0.5), li(0.5) }, new double[] { 2, 1 });
		Solver s = solve(CONVOLUTION, m);
		assertMeasures(CONVOLUTION, s, 6.0 / 7, new double[] { 10.0 / 7, 4.0 / 7 }, null, new double[] { 6.0 / 7, 3.0 / 7 });
	}

}
