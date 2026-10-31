package solvers.dispatchers.wrappers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import analytical.ExactModel;
import solvers.Solver;
import solvers.SolverAlgorithm;
import solvers.multiSolverAlgorithms.MultiFixtures;

/** Two queues D=1,2 and N=2: R=(4/3,10/3), X=3/7, Q=(4/7,10/7), U=(3/7,6/7). */
class SingleClassWrappersTest {

	private static ExactModel model() {
		return MultiFixtures.closed(new int[] { MultiFixtures.LI, MultiFixtures.LI }, new double[] { 2 }, new double[][] { { 1 }, { 2 } });
	}

	private static void feed(Solver s) {
		assertTrue(s.input(new String[] { "a", "b" }, new int[] { Solver.LI, Solver.LI }, new double[][] { { 1 }, { 2 } }, new double[] { 1, 1 }));
	}

	private static void check(Solver s, double tol) {
		assertTrue(s.hasSufficientProcessingCapacity());
		s.solve();
		assertEquals(4.0 / 7, s.getQueueLen()[0], tol);
		assertEquals(10.0 / 7, s.getQueueLen()[1], tol);
		assertEquals(3.0 / 7, s.getThroughput()[0], tol);
		assertEquals(4.0 / 3, s.getResTime()[0], 4 * tol);
		assertEquals(6.0 / 7, s.getUtilization()[1], tol);
	}

	@Test
	void exactMultiSolverBehindTheSingleClassInterface() throws Exception {
		ClosedMultiToSingleSolverWrapper w = new ClosedMultiToSingleSolverWrapper(SolverAlgorithm.TREE_MVA, model());
		feed(w);
		check(w, 1e-9);
	}

	@Test
	void iterativeWrapperReportsIterationsAndHonoursTheCap() throws Exception {
		IterativeClosedMultiToSingleSolverWrapper w = new IterativeClosedMultiToSingleSolverWrapper(SolverAlgorithm.BARD_SCHWEITZER, model(), 1e-10);
		feed(w);
		check(w, 0.05);
		int full = w.getIterations();
		assertTrue(full > 1, "iterations " + full);

		IterativeClosedMultiToSingleSolverWrapper capped = new IterativeClosedMultiToSingleSolverWrapper(SolverAlgorithm.BARD_SCHWEITZER, model(), 1e-10);
		capped.setMaxIterations(1);
		feed(capped);
		capped.solve();
		assertTrue(capped.getIterations() <= 1, "cap respected: " + capped.getIterations());
	}

	@Test
	void inputRejectsInconsistentArrays() throws Exception {
		ClosedMultiToSingleSolverWrapper w = new ClosedMultiToSingleSolverWrapper(SolverAlgorithm.TREE_MVA, model());
		// wrong visits length: the base class refuses
		assertFalse(w.input(new String[] { "a", "b" }, new int[] { Solver.LI, Solver.LI }, new double[][] { { 1 }, { 2 } }, new double[] { 1 }));
	}
}
