package jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.Tests;

import org.junit.Test;

import jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.Tests.TestUtils.SolverType;

/**
 * Runs tests, comparing the results from different solvers.
 * Does this for each permutation of the Sequential-MVA, Tree-MVA, Convolution and Tree-Convolution Solvers.
 * @author Ben Homer, 2014.
 */
public class AllTests {

	/** Maximum number of threads that can be run simultaneously. */
	private int max_threads = Runtime.getRuntime().availableProcessors();
	
	/** Which tests to run for each solver permutation. */
	private void toRun(GeneralTests t) {
		t.runAll();
	}
	
	/** SINGLE THREAD TESTS **/
	
	/** Tests Sequential Convolution against Sequential MVA, single-threaded. */
	@Test
	public void C_vs_MVA() {
		TestConfig tc = new TestConfig(SolverType.C, SolverType.MVA, 1);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Sequential Convolution against Tree Convolution, single-threaded. */
	@Test
	public void C_vs_TC() {
		TestConfig tc = new TestConfig(SolverType.C, SolverType.TC, 1);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Sequential Convolution against Tree MVA, single-threaded. */
	@Test
	public void C_vs_TMVA() {
		TestConfig tc = new TestConfig(SolverType.C, SolverType.TMVA, 1);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Sequential MVA against Tree Convolution, single-threaded. */
	@Test
	public void MVA_vs_TC() {
		TestConfig tc = new TestConfig(SolverType.MVA, SolverType.TC, 1);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Sequential MVA against Tree MVA, single-threaded. */
	@Test
	public void MVA_vs_TMVA() {
		TestConfig tc = new TestConfig(SolverType.MVA, SolverType.TMVA, 1);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Tree Convolution against Tree MVA, single-threaded. */
	@Test
	public void TC_vs_TMVA() {
		TestConfig tc = new TestConfig(SolverType.TC, SolverType.TMVA, 1);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** MULTI THREAD TESTS **/

	/** Tests Sequential Convolution against Tree Convolution, multi-threaded. */
	@Test
	public void C_vs_TC_Multi() {
		TestConfig tc = new TestConfig(SolverType.C, SolverType.TC, max_threads);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Sequential Convolution against Tree MVA, multi-threaded. */
	@Test
	public void C_vs_TMVA_Multi() {
		TestConfig tc = new TestConfig(SolverType.C, SolverType.TMVA, max_threads);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Sequential MVA against Tree Convolution, multi-threaded. */
	@Test
	public void MVA_vs_TC_Multi() {
		TestConfig tc = new TestConfig(SolverType.MVA, SolverType.TC, max_threads);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Sequential MVA against Tree MVA, multi-threaded. */
	@Test
	public void MVA_vs_TMVA_Multi() {
		TestConfig tc = new TestConfig(SolverType.MVA, SolverType.TMVA, max_threads);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
	
	/** Tests Tree Convolution against Tree MVA, multi-threaded. */
	@Test
	public void TC_vs_TMVA_Multi() {
		TestConfig tc = new TestConfig(SolverType.TC, SolverType.TMVA, max_threads);
		GeneralTests t = new GeneralTests(tc);
		toRun(t);
	}
}
