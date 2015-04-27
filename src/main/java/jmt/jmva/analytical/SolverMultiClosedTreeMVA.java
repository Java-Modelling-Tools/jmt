package jmt.jmva.analytical;

import javax.naming.OperationNotSupportedException;

import QueuingNet.TreeAlgorithms.TMVA.TreeMVASolver;
import Exceptions.InternalErrorException;
import DataStructures.QNModel;
import Exceptions.BTFMatrixErrorException;
import Exceptions.InconsistentLinearSystemException;

/**
 * This class implements the API to allow use by other applications. One can
 * define the queueing network model details, call the solvers transparently and,
 * afterwards, read the results.
 * @author Ben Homer, 2014.
 */
public class SolverMultiClosedTreeMVA extends SolverMulti {
	
	/** Array containing population for each class. */
	protected int[] population;
	
	/** Contains the queueing network model. */
	protected QNModel qnm;
	
	/** Number of threads that the MoMSolver should use. */
	protected int nThreads;
	
	/** Array containing the station/queue types. */
	protected int[] types;

	/** Creates new SolverMultiClosedTreeConvolution
	 *  @param  stations    number of service centers
	 *  @param  classes     number o classes of classes
	 *  @param  population     array of population classes
	 */
	public SolverMultiClosedTreeMVA(int classes, int stations, int[] population) {
		super(classes, stations);
		this.classes = classes;
		this.stations = stations;
		this.population = population;
	}

	/** Initializes the solver with the system parameters.
	 * It must be called before trying to solve the model.
	 *  @param  t   array of the types (LD or LI) of service centers
	 *  @param  s   matrix of service times of the service centers
	 *  @param  v   array of visits to the service centers
	 *  @param  nThreads The number of threads the solver should use. If nThreads > 2 then the parallel algorithm is used.
	 *  @return True if the operation is completed with success
	 */
	public boolean input(int[] t, double[][][] s, double[][] v, int nThreads) {
		if ((t.length > stations) || (s.length > stations) || (v.length > stations) || nThreads < 0) {
			// wrong input.
			return false;
		}
		
		types = new int[t.length];
		System.arraycopy(t, 0, types, 0, t.length);
		
		this.nThreads = nThreads;

		visits = new double[stations][classes];
		for (int i = 0; i < stations; i++) {
			System.arraycopy(v[i], 0, visits[i], 0, classes);
		}

		servTime = new double[stations][classes][1];
		for (int i = 0; i < stations; i++) {
			for (int r = 0; r < classes; r++) {
				servTime[i][r][0]=s[i][r][0]; // * 1000000;
			}
		}

		try {
			int M = 0, R = this.classes;
			double[] Z = new double[classes];
			for (int r = 0; r < R; r++) {
				Z[r] = 0;
			}

			// Discover delay times (think times)
			for (int i = 0; i < stations; i++) {
				if (t[i] == LI) {
					M++;
				} 
				/*else if (t[i] == LD) { // LD stations not allowed.
					M++;
				}*/
				else if (t[i] == DELAY) {
					M++;
					for (int r = 0; r < classes; r++) {
						Z[r] += (servTime[i][r][0] * visits[i][r]);
					}
				} else {
					return false;
				}
			}
			// Now Z contains the delay times

			// Discover service demands
			double[][] D = new double[M][R];
			int mIndex = 0; // current queue
			for (int i = 0; i < stations; i++) {
				for (int r = 0; r < classes; r++) {
					D[mIndex][r] = servTime[i][r][0] * visits[i][r];
				}
				mIndex++;
			}
			// Now D contains service demands

			// Create queue multiplicities array
			// All multiplicities are set to 1, as JMT does not seem to use queue multiplicities
			// If this array is instantiated properly, the rest of the MoMSolver can support them
			Integer[] multiplicities = new Integer[M];
			for (int m = 0; m < M; m++) {
				multiplicities[m] = 1;
			}

			// Transform population from int[] to Integer[]
			Integer[] N = new Integer[R];
			for (int r = 0; r < R; r++) {
				N[r] = population[r];
			}

			// Instantiate queuing network model
			qnm = new QNModel(R, M, t, N, Z, multiplicities, D);
			/*for (int m = 0; m < qnm.M; m++) {
				for (int r = 0; r < qnm.R; r++) {
					System.out.println("Z["+r+"]=" + qnm.getDelay(r));
					System.out.println("D["+m+","+r+"]=" + qnm.getDemand(m, r));
					System.out.println("Q["+m+","+r+"]=" + queueLen[m][r]);
					System.out.println("X["+r+"]=" + clsThroughput[r]);
				}
			}*/

		} catch (Exception ex) {
			ex.printStackTrace();
			// Return false if initialisation fails for any reason.
			return false;
		}
		return true;
	}

	/**
	 * Solves the system through the MoM algorithm.
	 * input(...) must have been called first.
	 *
	 * @throws InternalErrorException Thrown when any error is encountered during computations, i.e. due to linear system singularities
	 * @throws BTFMatrixErrorException 
	 * @throws InconsistentLinearSystemException 
	 * @throws OperationNotSupportedException 
	 */
	@Override
	public void solve() {
		QueuingNet.QNSolver c = null;
		
		try {
			c = new TreeMVASolver(qnm, nThreads, false);
			c.computePerformanceMeasures();

			clsThroughput = qnm.getMeanThroughputsAsDoubles();
			queueLen = qnm.getMeanQueueLengthsAsDoubles();
					
			throughput = new double[stations][classes];
			utilization = new double[stations][classes];
			scThroughput = new double[stations];
			scUtilization = new double[stations];
			scResidTime = new double[stations];
			scQueueLen = new double[stations];
			for (int m = 0; m < qnm.M; m++) {
				for (int r = 0; r < qnm.R; r++) {
					throughput[m][r] = clsThroughput[r] * visits[m][r];
					utilization[m][r] = throughput[m][r] * servTime[m][r][0]; // Umc=Xmc*Smc
					residenceTime[m][r] = queueLen[m][r] / clsThroughput[r];
					scThroughput[m] += throughput[m][r];
					scUtilization[m] += utilization[m][r];
					scResidTime[m] += residenceTime[m][r];
					scQueueLen[m] += queueLen[m][r];
				}
			}
	
			sysResponseTime = 0;
			sysNumJobs = 0;
			sysThroughput = sysNumJobs / sysResponseTime;
		} catch (Exception e) {
			e.printStackTrace();
			throw new NullPointerException(e.getMessage());
		}
	}

	@Override
	public boolean hasSufficientProcessingCapacity() {
		// only closed class - no saturation problem
		return true;
	}
}