package jmt.jmva.analytical.solvers;

import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.DataStructures.QNModel.QueueType;
import jmt.jmva.analytical.solvers.Exceptions.BTFMatrixErrorException;
import jmt.jmva.analytical.solvers.Exceptions.InconsistentLinearSystemException;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.QNSolver;
import jmt.jmva.analytical.solvers.QueueingNet.TreeAlgorithms.TMVA.TreeMVASolver;

/**
 * Created by gcasale on 15 Aug 2018.
 */
public class SolverMultiClosedTreeMVA extends SolverMulti {

	//Array containing population for each class
	protected int[] population;

	/**
	 * Contains the queueing network model
	 */
	protected QNModel qnm;

	/** Creates new SolverMultiClosedMonteCarlo
	 *  @param  stations    number of service centers
	 *  @param  classes     number of classes
	 *  @param  population  array of class populations
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
	 *  @return True if the operation is completed with success
	 */
	public boolean input(int[] t, double[][][] s, double[][] v) {
		if ((t.length > stations) || (s.length > stations) || (v.length > stations)) {
			// wrong input.
			return false;
		}

		visits = new double[stations][classes];
		for (int i = 0; i < stations; i++) {
			System.arraycopy(v[i], 0, visits[i], 0, classes);
		}

		servTime = new double[stations][classes][1];
		for (int i = 0; i < stations; i++) {
			for (int r = 0; r < classes; r++) {
				servTime[i][r][0] = s[i][r][0];
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
				} else if (t[i] == DELAY) {
					for (int r = 0; r < classes; r++) {
						Z[r] += (int) (servTime[i][r][0] * visits[i][r]);
					}
				} else {
					return false;
				}
			}

			// Discover service demands
			double[][] D = new double[M][R];
			int mIndex = 0; // current queue
			for (int i = 0; i < stations; i++) {
				if (t[i] == LI) {
					for (int r = 0; r < classes; r++) {
						D[mIndex][r] = (int) (servTime[i][r][0] * visits[i][r]);
					}
					mIndex++;
				}
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

			// Instantiate queueing network model
			qnm = new QNModel(R, M, t, N, Z, multiplicities, D);
		} catch (Exception ex) {
			ex.printStackTrace();
			// Return false if initialisation fails for any reason.
			return false;
		}
		return true;
	}

	/**
	 * Solves the system through the TreeMVA algorithm.
	 * input(...) must have been called first.
	 *
	 * @throws InternalErrorException
	 * @throws BTFMatrixErrorException
	 * @throws InconsistentLinearSystemException
	 * @throws OperationNotSupportedException
	 */
	@Override
	public void solve() {
		QNSolver c = null;
		try {
			int numThreads = 1;
			c = new TreeMVASolver(qnm, numThreads, false);
			c.computePerformanceMeasures();
		} catch (InternalErrorException e) {
			e.printStackTrace();
		}

		clsThroughput = qnm.getMeanThroughputsAsDoubles();
		queueLen = qnm.getMeanQueueLengthsAsDoubles();

		throughput = new double[stations][classes];
		utilization = new double[stations][classes];
		scThroughput = new double[stations];
		scUtilization = new double[stations];
		scResidTime = new double[stations];
		scQueueLen = new double[stations];
		double scale = 1.0;
		double[][] clsQueueLen = new double[stations][classes];
		int shift = 0;
		for (int m = 0; m < stations; m++) {
			if (qnm.getQueueType(m) == QueueType.LI) {
				for (int r = 0; r < classes; r++) {
					throughput[m][r] = clsThroughput[r] * visits[m][r] * scale;
					utilization[m][r] = throughput[m][r] * servTime[m][r][0] / scale; // Umc=Xmc*Smc
					residenceTime[m][r] = queueLen[m-shift][r] / clsThroughput[r] / scale;
					clsQueueLen[m][r] = queueLen[m-shift][r];
				}
			} else if (qnm.getQueueType(m) == QueueType.DELAY) {
				shift = shift + 1;
				for (int r = 0; r < classes; r++) {
					throughput[m][r] = clsThroughput[r] * visits[m][r] * scale;
					utilization[m][r] = throughput[m][r] * servTime[m][r][0] / scale; // Umc=Xmc*Smc
					clsQueueLen[m][r] = utilization[m][r];
					residenceTime[m][r] = servTime[m][r][0] / scale;
				}
			}
			for (int r = 0; r < classes; r++) {
				scThroughput[m] += throughput[m][r];
				scUtilization[m] += utilization[m][r];
				scResidTime[m] += residenceTime[m][r];
				scQueueLen[m] += clsQueueLen[m][r];
			}
		}
		queueLen = clsQueueLen;

		//NEW
		//@author Stefano Omini
		//compute system parameters
		sysResponseTime = 0;
		sysNumJobs = 0;

		//for (c = 0; c < classes; c++) {
		//	for (m = 0; m < stations; m++) {
		//		clsRespTime[c] += residenceTime[m][c];
		//		sysNumJobs += queueLen[m][c];
		//	}
		//	sysResponseTime += clsRespTime[c];
		//}

		sysThroughput = sysNumJobs / sysResponseTime;
		//end NEW
	}

	@Override
	public boolean hasSufficientProcessingCapacity() {
		// only closed class - no saturation problem
		return true;
	}

}
