package solvers.multiSolverAlgorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import solvers.SolverMulti;
import solvers.SolverMultiClosedMoM;

/**
 * Multiclass solvers that SolverMultiAlgorithmSelector does not build: MoM, RGF, Tree Convolution and plain
 * Monte Carlo are commented out there ("buggy"). Built directly; the oracle is the hand-solved
 * N=(1,1), D=(1,2) queue + think times (1,3) of {@link MultiClassClosedTest} (X = 5/12, 1/6) and the balanced
 * two-queue network (X = 1/3 per class, Q = 1/2 per cell).
 */
class DirectSolversTest {

	private static final int[] TYPES = { SolverMulti.LI, SolverMulti.DELAY };
	private static final double[][][] ST = { { { 1 }, { 2 } }, { { 1 }, { 3 } } };
	private static final double[][] V = { { 1, 1 }, { 1, 1 } };
	private static final int[] POP = { 1, 1 };

	private static final int[] BAL_TYPES = { SolverMulti.LI, SolverMulti.LI };
	private static final double[][][] BAL_ST = { { { 1 }, { 1 } }, { { 1 }, { 1 } } };

	/** MoM is accurate (it scales demands like RECAL), so 1e-5. Not reachable from the dispatcher. */
	@Test
	void methodOfMomentsMatchesHandValues() {
		SolverMultiClosedMoM s = new SolverMultiClosedMoM(2, 2, POP);
		assertTrue(s.input(TYPES, ST, V, 1));
		s.solve();
		assertEquals(5.0 / 12, s.getThroughput()[0][0], 1e-5);
		assertEquals(1.0 / 6, s.getThroughput()[0][1], 1e-5);
		assertEquals(7.0 / 12, s.getQueueLen()[0][0], 1e-5);
		assertEquals(0.5, s.getQueueLen()[0][1], 1e-5);
	}

	/** unused-code defect: RGF accepts the model but solve() throws 'Normalising Constant has not been computed yet'; the dispatcher has it commented out as 'Looks buggy'. */
	@Disabled("unused-code defect: SolverMultiClosedRGF.solve throws UnsupportedOperationException on a valid model (class not built by the dispatcher)")
	@Test
	void rgfSolvesBalancedNetwork() {
		SolverMultiClosedRGF s = new SolverMultiClosedRGF(2, 2, POP);
		assertTrue(s.input(BAL_TYPES, BAL_ST, V, 1));
		s.solve();
		assertEquals(1.0 / 3, s.getThroughput()[0][0], 1e-5);
	}

	/** unused-code defect: Tree Convolution input() succeeds but solve() fails with a NullPointerException (treeRoot is never set). */
	@Disabled("unused-code defect: SolverMultiClosedTreeConvolution.solve throws NullPointerException (treeRoot null; class not built by the dispatcher)")
	@Test
	void treeConvolutionSolvesBalancedNetwork() {
		SolverMultiClosedTreeConvolution s = new SolverMultiClosedTreeConvolution(2, 2, POP);
		assertTrue(s.input(BAL_TYPES, BAL_ST, V));
		s.solve();
		assertEquals(1.0 / 3, s.getThroughput()[0][0], 1e-5);
	}

	/**
	 * Plain Monte Carlo is randomised (unseeded) so it is slow-tagged with a wide tolerance; exact X = 1/3.
	 * (It rejects a model containing a delay station: input() returns false for the queue + think-time model.)
	 */
	@Tag("slow")
	@Test
	void monteCarloSolvesBalancedNetwork() {
		SolverMultiClosedMonteCarlo s = new SolverMultiClosedMonteCarlo(2, 2, POP);
		assertTrue(s.input(BAL_TYPES, BAL_ST, V));
		s.solve();
		assertEquals(1.0 / 3, s.getThroughput()[0][0], 0.1);
	}
}
