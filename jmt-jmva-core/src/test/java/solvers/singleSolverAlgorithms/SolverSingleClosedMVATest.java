package solvers.singleSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.singleSolverAlgorithms.SolverFixtures.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import solvers.Solver;
import solvers.singleSolverAlgorithms.SolverFixtures.Model;

/** MVA-only features: results for every intermediate population and the printable report. */
class SolverSingleClosedMVATest {

	private static final double TOL = 1e-9;

	/** Balanced K=3, D=2: for every n, X(n) = n/(D(n+K-1)), Q(n) = n/K, R(n) = D(n+K-1)/K, U(n) = n/(n+K-1). */
	@Test
	void intermediateResultsOfBalancedNetwork() throws Exception {
		int k = 3;
		double d = 2;
		Model m = balanced(5, k, d);
		SolverSingleClosedMVA s = new SolverSingleClosedMVA(5, k);
		assertTrue(s.input(m.names, m.types, m.serv, m.visits));
		s.solve(true);
		assertEquals(0.0, s.interm_totThroughput[0], 0.0);
		for (int n = 1; n <= 5; n++) {
			assertEquals(n / (d * (n + k - 1)), s.interm_totThroughput[n], TOL, "X(" + n + ")");
			for (int i = 0; i < k; i++) {
				assertEquals((double) n / k, s.interm_queueLen[n][i], TOL, "Q(" + n + ")");
				assertEquals(d * (n + k - 1) / k, s.interm_residenceTime[n][i], TOL, "R(" + n + ")");
				assertEquals((double) n / (n + k - 1), s.interm_utilization[n][i], TOL, "U(" + n + ")");
			}
		}
		// the final population is also the ordinary result
		assertEquals(s.interm_totThroughput[5], s.getTotThroughput(), TOL);
		assertTrue(s.toString().contains("Queue Length"));
	}

	/** Little's law on the whole network for each intermediate population: total response time = n / X(n). */
	@Disabled("unused-code defect (nothing calls solve(true)): solveSingleLI(true) computes interm_totRespTime[n] = customers / X(n) instead of n / X(n)")
	@Test
	void intermediateResponseTimeFollowsLittlesLaw() throws Exception {
		Model m = balanced(5, 3, 2);
		SolverSingleClosedMVA s = new SolverSingleClosedMVA(5, 3);
		assertTrue(s.input(m.names, m.types, m.serv, m.visits));
		s.solve(true);
		for (int n = 1; n <= 5; n++) {
			assertEquals(n / s.interm_totThroughput[n], s.interm_totRespTime[n], TOL, "R(" + n + ") = n/X(n)");
		}
	}

	/** Zero demand everywhere: solve() reports X = 0; solve(true) must not divide by zero either. */
	@Disabled("unused-code defect (nothing calls solve(true)): solveSingleLI(true) has no 'sum == 0' guard, X = n/0 = Infinity where solveSingleLI() reports 0")
	@Test
	void intermediatePathSurvivesZeroDemand() throws Exception {
		Model m = liModel(3, new int[] { Solver.LI, Solver.LI }, 0, 0);
		SolverSingleClosedMVA s = new SolverSingleClosedMVA(3, 2);
		assertTrue(s.input(m.names, m.types, m.serv, m.visits));
		s.solve(true);
		assertEquals(0.0, s.getTotThroughput(), 0.0);
		assertEquals(0.0, s.getQueueLen(0), 0.0);
	}

	/**
	 * Load-dependent chain with a delay (c=2, S=1, Z=1), hand-solved birth-death chain: X(2) = 1, X(3) = 24/17.
	 * solve(true) must give the same final answer as solve().
	 */
	@Disabled("unfinished (source TODO 'add case intermediate=true for LD'): solveSingleLD(true) sets the delay service time to Z at every load (the 'wcomp / n' line is commented out), "
			+ "so the delay behaves as a single-server queue: X(3) = 0.909 instead of 24/17; it also never fills the interm_* arrays")
	@Test
	void intermediateResultsOfLoadDependentModel() throws Exception {
		double[] dl = { 1, 0, 0, 0 };
		Model m = new Model(3, new int[] { Solver.LD, Solver.DELAY }, new double[][] { multiServer(3, 2, 1), dl }, new double[] { 1, 1 });
		SolverSingleClosedMVA s = new SolverSingleClosedMVA(3, 2);
		assertTrue(s.input(m.names, m.types, m.serv, m.visits));
		s.solve(true);
		assertEquals(24.0 / 17, s.getTotThroughput(), TOL);
		assertEquals(27.0 / 17, s.getQueueLen(0), TOL);
		assertEquals(1.0, s.interm_totThroughput[2], TOL, "X(2)");
	}

	/** With load-dependent and load-independent queues (no delay) solve(true) agrees with solve(). */
	@Test
	void loadDependentIntermediatePathAgreesWithPlainSolve() throws Exception {
		Model m = new Model(3, new int[] { Solver.LI, Solver.LD }, new double[][] { li(1), multiServer(3, 2, 2) }, new double[] { 1, 1 });
		SolverSingleClosedMVA plain = new SolverSingleClosedMVA(3, 2);
		SolverSingleClosedMVA inter = new SolverSingleClosedMVA(3, 2);
		assertTrue(plain.input(m.names, m.types, m.serv, m.visits));
		assertTrue(inter.input(m.names, m.types, m.serv, m.visits));
		plain.solve();
		inter.solve(true);
		assertEquals(plain.getTotThroughput(), inter.getTotThroughput(), TOL);
		for (int i = 0; i < 2; i++) {
			assertEquals(plain.getQueueLen(i), inter.getQueueLen(i), TOL);
			assertEquals(plain.getUtilization(i), inter.getUtilization(i), TOL);
		}
	}

	/** The report lists the four measures; with intermediate results it is longer. */
	@Test
	void reportsListEveryMeasure() throws Exception {
		Model m = repairman(2, 1, 1);
		SolverSingleClosedMVA s = new SolverSingleClosedMVA(2, 2);
		assertTrue(s.input(m.names, m.types, m.serv, m.visits));
		s.solve();
		String plain = s.toString();
		for (String h : new String[] { "Queue Length", "Residence Time", "Throughput", "Utilization" }) {
			assertTrue(plain.contains(h), h);
		}
		SolverSingleClosedMVA t = new SolverSingleClosedMVA(2, 2);
		assertTrue(t.input(m.names, m.types, m.serv, m.visits));
		t.solve(true);
		assertTrue(t.toString().length() > plain.length());
	}

	/** Closed networks have no saturation problem. */
	@Test
	void closedModelAlwaysHasCapacity() {
		assertTrue(new SolverSingleClosedMVA(3, 2).hasSufficientProcessingCapacity());
	}
}
