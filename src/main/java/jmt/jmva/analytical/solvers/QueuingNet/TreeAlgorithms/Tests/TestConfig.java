package jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.Tests;

import jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.Tests.TestUtils.SolverType;

/**
 * Test configuration specifying the solvers to test against each other
 * and the number of threads to use.
 * @author Ben Homer, 2014.
 */
public class TestConfig {
	
	/** The first solver type. */
	public SolverType t1;
	
	/** The second solver type. */
	public SolverType t2;
	
	/** The number of threads to run each solver with. */
	public int numThreads;
	
	/**
	 * Constructor.
	 * @param t1 The first solver type to test with.
	 * @param t2 The second solver type to test with.
	 * @param numThreads The number of threads to run each solver with.
	 */
	public TestConfig(SolverType t1, SolverType t2, int numThreads) {
		this.t1 = t1;
		this.t2 = t2;
		this.numThreads = numThreads;
	}
}
