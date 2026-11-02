package solvers.singleSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.singleSolverAlgorithms.SolverFixtures.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import solvers.Solver;
import solvers.SolverSingleClosedMonteCarloLogistic;
import solvers.exceptions.InternalErrorException;
import solvers.singleSolverAlgorithms.SolverFixtures.Model;

/**
 * Monte Carlo logistic sampling is randomised: its generators are seeded from the current date inside the library, so a
 * seed cannot be fixed from outside. The tests are tagged slow and use a loose tolerance on well-conditioned models.
 */
@Tag("slow")
class SolverSingleClosedMonteCarloLogisticTest {

	/** Balanced K=3, D=2, N=4 (closed form: Q = 4/3, U = 2/3) within the sampling error (observed error about 0.1 on Q, U inside 0.1 in 14 runs; tolerance 0.2 on Q, 0.15 on U). */
	@Test
	void balancedNetworkWithinSamplingError() throws Exception {
		Solver s = solve(MONTE_CARLO, balanced(4, 3, 2));
		for (int i = 0; i < 3; i++) {
			assertEquals(4.0 / 3, s.getQueueLen(i), 0.2, "Q" + i);
			assertEquals(2.0 / 3, s.getUtilization(i), 0.15, "U" + i);
		}
	}

	/** Machine repairman N=3, D=1, Z=2: Q = (27/19, 30/19), U = X = 15/19. */
	@Test
	void repairmanWithinSamplingError() throws Exception {
		Model m = repairman(3, 1, 2);
		Solver s = solve(MONTE_CARLO, m);
		assertEquals(27.0 / 19, s.getQueueLen(0), 0.2);
		assertEquals(30.0 / 19, s.getQueueLen(1), 0.2);
		assertEquals(15.0 / 19, s.getUtilization(0), 0.15);
		assertPopulationIdentity("MonteCarlo", s, m, 0.25); // X and Q are estimated separately, so only roughly
	}

	/** Parameter checks: at least 20 samples and 8 digits of precision. */
	@Test
	void invalidParametersAreRejected() {
		assertThrows(InternalErrorException.class, () -> new SolverSingleClosedMonteCarloLogistic(3, 2, 19, 32));
		assertThrows(InternalErrorException.class, () -> new SolverSingleClosedMonteCarloLogistic(3, 2, 1000, 7));
	}
}
