package jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.Tests;

import static org.junit.Assert.*;

import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.Exceptions.InputFileParserException;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueuingNet.ConvolutionSolver;
import jmt.jmva.analytical.solvers.QueuingNet.QNSolver;
import jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.TMVA.TreeMVASolver;
import jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.TreeConvolution.IThreadedTCSolver;
import jmt.jmva.analytical.solvers.QueuingNet.TreeAlgorithms.TreeConvolution.TreeConvolutionSolver;

/**
 * Test utilities for comparing the results given by any two solver types.
 * @author Ben Homer, 2014.
 */
public class TestUtils {

	/** The allowed error margin. */
	private static final double ERROR = 0.00000001;
	
	/** Whether to check throughput results. */
	private static final boolean CHECK_X = true;
	
	/** Whether to check mean queue length time results. */
	private static final boolean CHECK_Q = true;
	
	/** Solver types that can be used when testing = Convolution, MVA, Tree Convolution, Tree-MVA */
	public static enum SolverType { C, MVA, TC, TMVA };
	
	/** 
	 * Tests the result consistency for specified model (loaded from file) and solvers in the test configuration.
	 * @param fn The model's filename.
	 * @param t The test configuration.
	 */
	public static void TestResultConsistencyForModelFile(String fn, TestConfig t) {
		QNModel qnm;
		try {
			qnm = new QNModel(fn);
			TestResultsConsistencyForModel(qnm, t);
		} catch (InputFileParserException e) {
			e.printStackTrace();
		}
	}
	
	/** 
	 * Tests the result consistency for specified model and solvers in the test configuration.
	 * @param qnm The model object.
	 * @param t The test configuration.
	 */
	public static void TestResultsConsistencyForModel(QNModel qnm1, TestConfig t) {
		try {
			QNModel qnm2 = new QNModel(qnm1);
			TestTwoSolversGiveSameResults(t.t1, t.t2, qnm1, qnm2, t.numThreads);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Test whether two solvers, of the specified types, give the same results.
	 * @param s1 The type of the first solver.
	 * @param s2 The type of the second solver.
	 * @param qnm1 The first model object.
	 * @param qnm2 The second model object.
	 * @param numThreads The number of threads to use.
	 */
	public static void TestTwoSolversGiveSameResults(SolverType s1, SolverType s2, QNModel qnm1, QNModel qnm2, int numThreads) {
		try {
			if (s1.equals(s2)) {
				fail("Two solvers under test are of the same type.");
			}
			QNSolver solver1 = createSolver(qnm1, s1, numThreads);
			QNSolver solver2 = createSolver(qnm2, s2, numThreads);
			
			String g1, g2;
			long t1, t2;
			g1 = g2 = "N/A";
			t1 = t2 = 0;
			
			int cCount = 0;
			if (s1.equals(SolverType.C) || s1.equals(SolverType.TC)) {
				solver1.computeNormalisingConstant();
				g1 = solver1.getNormalisingConstant();
				cCount++;
				t1 = (s1.equals(SolverType.C)) 
						? solver1.totalTimer.getInterval() : IThreadedTCSolver.normalisationConstantTimer.getInterval();
			}
			
			if (s2.equals(SolverType.C) || s2.equals(SolverType.TC)) {
				solver2.computeNormalisingConstant();
				g2 = solver2.getNormalisingConstant();
				cCount++;
				t2 = (s2.equals(SolverType.C)) 
						? solver2.totalTimer.getInterval() : IThreadedTCSolver.normalisationConstantTimer.getInterval();
			}
			
			System.out.println(s1.toString() + " = " + g1 + " " + s2.toString() + " = " + g2);
			
			if (s1.equals(SolverType.C) && g1.equals("0")) return;
			if (s2.equals(SolverType.C) && g2.equals("0")) return;
			
			if (cCount == 2) {
				equalsString(g1, g2);
			}
			
			solver1.computePerformanceMeasures();
			solver2.computePerformanceMeasures();
			long t1_ = solver1.totalTimer.getInterval();
			long t2_ = solver2.totalTimer.getInterval();
			
			printSummary(s1, t1, t1_);
			printSummary(s2, t2, t2_);
			
			// Check throughputs.
			if (CHECK_X) {
				double[] X1 = qnm1.getMeanThroughputsAsDoubles();
				double[] X2 = qnm2.getMeanThroughputsAsDoubles();
				assertEquals(X1.length, X2.length);
				for (int i = 0; i < X1.length; i++) {
					assertEquals(X1[i], X2[i], ERROR);
				}
			}
			
			// Check queue lengths.
			if (CHECK_Q) {
				double[][] Q1 = qnm1.getMeanQueueLengthsAsDoubles();
				double[][] Q2 = qnm2.getMeanQueueLengthsAsDoubles();
				assertEquals(Q1.length, Q2.length);
				assertEquals(Q1[0].length, Q2[0].length);
				for (int i = 0; i < Q1.length; i++) {
					for (int j = 0; j < Q1[0].length; j++) {
						assertEquals(Q1[i][j], Q2[i][j], ERROR);
					}
				}
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a solver depending on the specified mode, solver type and number of threads.
	 * @param qnm The model object.
	 * @param t The type of the solver to instantiate.
	 * @param numThreads The number of threads.
	 * @return The instantiated QNSolver object.
	 */
	private static QNSolver createSolver(QNModel qnm, SolverType t, int numThreads) {
		try {
			switch (t) {
			case C:
				return new ConvolutionSolver(qnm);
			case MVA:	
				return new TreeMVASolver(qnm, numThreads, true);
			case TC:
				return new TreeConvolutionSolver(qnm, numThreads);
			case TMVA:
				return new TreeMVASolver(qnm, numThreads, false);
			default:
				return null;
			}
		} catch (InternalErrorException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Prints a summary for the specified solver type.
	 * @param type The solver type.
	 * @param ntime The time to calculate the normalisation constant.
	 * @param ptime The time to calculate the performance measures.
	 */
	private static void printSummary(SolverType type, long ntime, long ptime) {
		String normalizationTime = "N/A";
		switch (type) {
		case C:
			System.out.println(" --- Convolution ---");
			normalizationTime = toPrettyInterval(ntime);
			break;
		case TC:
			System.out.println(" --- Tree Convolution ---");
			normalizationTime = toPrettyInterval(ntime);
			break;
		case MVA:
			System.out.println(" --- MVA ---");
			break;
		case TMVA:
			System.out.println(" --- Tree MVA ---");
			break;
		}
		System.out.println("Normalisation constant time: "
				+ normalizationTime + ", Perf. Measures: " + toPrettyInterval(ptime)
				+ ", Total: " + toPrettyInterval(ntime+ptime));
	}
	
	/** Converts a time gives as a long to a pretty string for console display
	 * @param t The interval as a long.
	 * @return The pretty string.
	 */
    public static String toPrettyInterval(long t) {
        if (t < 1000) {
            return (t + " msec");
        } else if (t < 60000) {
            return (t / 1000.0 + " sec");
        } else {
            return ((t / 60000) + " min " + ((t - (t / 60000) * 60000) / 1000.0) + " sec");
        }
    }
    
    public static void equalsString(String s1, String s2) {
    	int max = s1.length() > 10 ? 10 : s1.length();
    	assertTrue(s1.substring(0, max).equals(s2.substring(0, max)));
    }
}