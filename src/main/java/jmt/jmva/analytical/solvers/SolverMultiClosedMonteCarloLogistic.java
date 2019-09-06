package jmt.jmva.analytical.solvers;

import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver.MonteCarloLogisticSolver;
import jmt.jmva.analytical.solvers.QueueingNet.QNSolver;

import java.math.BigDecimal;
import java.util.Arrays;

import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.arrays2qnm;

/**
 * Solves a multiclass closed model, using Monte Carlo Logistic algorithm.
 * @author  Ong Wai Hong
 */
public class SolverMultiClosedMonteCarloLogistic extends SolverMulti {

	public static final int DEFAULT_MAX_SAMPLES = 10000;
	public static final int DEFAULT_PRECISION = 32;
	protected int maxSamples = DEFAULT_MAX_SAMPLES;
	protected int precision = DEFAULT_PRECISION;
	protected boolean isLD;
	public QNModel qnm;
	protected int[] population;
	protected int[] servNum;

	/** Creates new SolverMultiClosedMonteCarlo
	 *  @param  stations    number of service centers
	 *  @param  classes     number of classes
	 *  @param  population  array of class populations
	 */
	public SolverMultiClosedMonteCarloLogistic(int classes, int stations, int[] population) {
		super(classes, stations);
		this.classes = classes;
		this.stations = stations;
		this.population = population;
	}

    public SolverMultiClosedMonteCarloLogistic(int classes, int stations, int[] population, int max_samples, int precision) throws InternalErrorException {
        this(classes, stations, population);
		if (max_samples<20)
			throw new InternalErrorException("Max samples has to greater than 20!");
		System.out.print("<Logistic Sampling> Using max samples: "); System.out.println(max_samples);
		this.maxSamples = max_samples;
        if (precision<8)
            throw new InternalErrorException("Precision has to be greater than 8 digits!");
        System.out.print("<Logistic Sampling> Using precision: "); System.out.println(precision);
        this.precision = precision;
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

		isLD = false;
		for (int i = 0; i < stations; i++) {
			if (t[i] == LD) {
				isLD = true;
				break;
			}
		}

		if (isLD) {
			try {
				qnm = arrays2qnm(t, s, v, population);
				servNum = new int[stations];
				for (int i = 0; i < stations; i++) {
					servNum[i] = qnm.getNumberOfServers(i);
				}
				System.out.println("<Logistic Sampling> Multi-server model detected, server count vector:");
				System.out.println(Arrays.toString(servNum));
			} catch (Exception ex) {
				ex.printStackTrace();
				// Return false if initialisation fails for any reason.
				return false;
			}
			return true;
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
						Z[r] += servTime[i][r][0] * visits[i][r];
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
				if (t[i] == LI) {
					for (int r = 0; r < classes; r++) {
						D[mIndex][r] = servTime[i][r][0] * visits[i][r];
					}
					mIndex++;
				}
			}
			// Now D contains service demands

			// Create queue multiplicities array
			// All multiplicities are set to 1, as Logistic Sampling does not use this
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
			servNum = new int[M];
			for (int i = 0; i < M; i++) {
				servNum[i] = qnm.getNumberOfServers(i);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			// Return false if initialisation fails for any reason.
			return false;
		}
		return true;
	}

	@Override
	public void solve() {
		QNSolver c = null;

		throughput = new double[stations][classes];
		utilization = new double[stations][classes];
		scThroughput = new double[stations];
		scUtilization = new double[stations];
		scResidTime = new double[stations];

		try {
			System.out.println("<Logistic Sampling> Initializing ...");
			c = new MonteCarloLogisticSolver(qnm, 0.0, maxSamples, precision, 1);
			c.computeNormalisingConstant();
			BigDecimal G = (((MonteCarloLogisticSolver) c).getNormalisingConstantAsBigDecimal());
			System.out.println("<Logistic Sampling> Normalising constant:"); System.out.println(G);
			c.computePerformanceMeasures();
			c.printTimeStatistics();
		} catch (InternalErrorException e) {
			System.err.println(e.getMessage());
			return;
		} catch (ArithmeticException | NumberFormatException e) {
			System.err.println("Arithmetic exception occured! Can be due to precision issues.");
			e.printStackTrace();
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		clsThroughput = qnm.getMeanThroughputsAsDoubles();
		queueLen = qnm.getMeanQueueLengthsAsDoubles();

		double[][] clsQueueLen = new double[stations][classes];
		int shift = 0;
		for (int m = 0; m < stations; m++) {
			if (isLD) {
				int servers = (qnm.getQueueType(m) == QNModel.QueueType.DELAY) ? 1 : servNum[m];
				for (int r = 0; r < classes; r++) {
					throughput[m][r] = clsThroughput[r] * visits[m][r];
					utilization[m][r] = throughput[m][r] * servTime[m][r][0] / servers; // Umc=Xmc*Smc/Cm
					residenceTime[m][r] = queueLen[m][r] / clsThroughput[r];
					clsQueueLen[m][r] = queueLen[m][r];
				}
			} else {
				if (qnm.getQueueType(m) == QNModel.QueueType.LI) {
					for (int r = 0; r < classes; r++) {
						throughput[m][r] = clsThroughput[r] * visits[m][r];
						utilization[m][r] = throughput[m][r] * servTime[m][r][0]; // Umc=Xmc*Smc
						residenceTime[m][r] = queueLen[m-shift][r] / clsThroughput[r];
						clsQueueLen[m][r] = queueLen[m-shift][r];
					}
				} else if (qnm.getQueueType(m) == QNModel.QueueType.DELAY) {
					shift = shift + 1;
					for (int r = 0; r < classes; r++) {
						throughput[m][r] = clsThroughput[r] * visits[m][r];
						utilization[m][r] = throughput[m][r] * servTime[m][r][0]; // Umc=Xmc*Smc
						clsQueueLen[m][r] = utilization[m][r];
						residenceTime[m][r] = servTime[m][r][0];
					}
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

		sysResponseTime = 0;
		sysNumJobs = 0;

		sysThroughput = sysNumJobs / sysResponseTime;
	}

	public static Integer validateMaxSamples(String maxSamples) {
		try {
			Integer value = Integer.parseInt(maxSamples);
			return (value < 20) ? null : value;
		} catch (Exception ex) {
			return null;
		}
	}

	public static Integer validatePrecision(String precision) {
		try {
			Integer value = Integer.parseInt(precision);
			return (value < 8) ? null : value;
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public boolean hasSufficientProcessingCapacity() {
		// only closed class - no saturation problem
		return true;
	}

}
