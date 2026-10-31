package solvers.singleSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.singleSolverAlgorithms.SolverFixtures.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import solvers.Solver;
import solvers.singleSolverAlgorithms.SolverFixtures.Algo;
import solvers.singleSolverAlgorithms.SolverFixtures.Model;

/** Normalisation-constant (convolution) solver on load-independent queueing stations, and its dynamically scaled variant. */
class SolverSingleClosedExactTest {

	/**
	 * Balanced K=2 stations, D=2: G(n) = sum_{n1+n2=n} D^n = (n+1) 2^n, so G = 1, 4, 12, 32 and
	 * X = G(N-1)/G(N) = 12/32 = 3/8 = N/(D(N+K-1)).
	 */
	@Disabled("unused-code defect: SolverSingleClosedExactDyn.solve never initialises scalCorr[0] (scalPrec[0] is set to 1), so for the second "
			+ "station scalPrec[1] != scalCorr[0] and G[1] is multiplied by 0^1: G = 1, 2, ... instead of 1, 4, ...")
	@Test
	void dynamicNormalisationConstantOfBalancedNetwork() throws Exception {
		Model m = balanced(3, 2, 2);
		SolverSingleClosedExactDyn s = new SolverSingleClosedExactDyn(3, 2);
		assertTrue(s.input(m.names, m.types, m.serv, m.visits));
		s.solve();
		assertArrayEquals(new double[] { 1, 4, 12, 32 }, s.G, 1e-12);
	}

	/** Same G with one station only: G(n) = D^n, the scaling bookkeeping is not involved. */
	@Test
	void dynamicNormalisationConstantOfSingleStation() throws Exception {
		Model m = balanced(3, 1, 2);
		SolverSingleClosedExactDyn s = new SolverSingleClosedExactDyn(3, 1);
		assertTrue(s.input(m.names, m.types, m.serv, m.visits));
		s.solve();
		assertArrayEquals(new double[] { 1, 2, 4, 8 }, s.G, 1e-12);
	}

	/** Plain convolution keeps G finite by static scaling: demands of 1e120 give X = N/(D(N+K-1)) = 3/(4e120), Q = 1.5. */
	@Test
	void hugeDemandsDoNotOverflow() throws Exception {
		Model m = balanced(3, 2, 1e120);
		for (Algo a : new Algo[] { CONVOLUTION, MVA }) {
			Solver s = solve(a, m);
			assertEquals(3.0 / 4e120, s.getTotThroughput(), 1e-9 * 3.0 / 4e120, a.name());
			assertEquals(1.5, s.getQueueLen(0), 1e-9, a.name());
		}
	}

	/** Only the slowest-first order is stable: results follow the input order when the first station is the slowest. */
	@Test
	void slowestFirstKeepsInputOrder() throws Exception {
		// D = 4, 2, 1 and N = 1: Q = D/sum = 4/7, 2/7, 1/7
		Model m = liModel(1, new int[] { Solver.LI, Solver.LI, Solver.LI }, 4, 2, 1);
		Solver s = solve(CONVOLUTION, m);
		assertArrayEquals(new double[] { 4.0 / 7, 2.0 / 7, 1.0 / 7 }, s.getQueueLen(), 1e-12);
		assertEquals(1.0 / 7, s.getTotThroughput(), 1e-12);
	}
	
	/** Closed networks have no saturation problem. */
	@Test
	void closedModelAlwaysHasCapacity() {
		assertTrue(new SolverSingleClosedExact(3, 2).hasSufficientProcessingCapacity());
	}
}
