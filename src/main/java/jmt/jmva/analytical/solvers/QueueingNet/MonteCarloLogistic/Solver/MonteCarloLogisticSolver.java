package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import jmt.jmva.analytical.solvers.SolverMultiClosedMonteCarloLogistic;
import jmt.jmva.analytical.solvers.DataStructures.BigRational;
import jmt.jmva.analytical.solvers.DataStructures.QNModel;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.QNSolver;
import jmt.jmva.analytical.solvers.Utilities.Timer;

import java.math.BigDecimal;
import java.math.MathContext;

import static jmt.engine.math.GammaFun.lnGamma;
import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.*;
import static jmt.jmva.analytical.solvers.Utilities.MiscFunctions.exp;

public class MonteCarloLogisticSolver extends QNSolver {

	//solver parameters
	private LogisticCore lcore;
	private double epsilon;
	private int max_samples;
	private MathContext MC;
	private int nthread;
	//model parameters
	private double[][] demands;
	private double[] delays;
	private int[] pop;
	private int[] serv;
	//miscellaneous
	private boolean is_nc_computed; //flag to mark if NC is computed
	private boolean is_ld; //flag to mark if multiserver case - QL calculations are different
	private Timer nc_timer, perf_timer;

	public MonteCarloLogisticSolver(QNModel qnm, double epsilon, int max_samples, int precision, int nthread) throws InternalErrorException {
		super(qnm);
		this.epsilon = epsilon;
		this.max_samples = max_samples;
		this.MC = new MathContext(precision);
		this.is_nc_computed = false;
		initialize(qnm);
		this.nthread = nthread;
		// LogisticCore for NC calculation.
		this.lcore = new LogisticCoreAdd(demands, serv, delays, pop, epsilon, max_samples, nthread, MC);
		nc_timer = new Timer();
		perf_timer = new Timer();
	}

	public MonteCarloLogisticSolver(QNModel qnm) throws InternalErrorException {
		this(qnm, 0.0, SolverMultiClosedMonteCarloLogistic.DEFAULT_MAX_SAMPLES, SolverMultiClosedMonteCarloLogistic.DEFAULT_PRECISION, 1);
	}

	private void initialize(QNModel qnm) {
		/**
		 * Initialize the solver by extracting model parameters from QNModel
		 */
		demands = new double[qnm.M][qnm.R];

		for (int k=0; k<qnm.M; k++) {
			for (int r=0; r<qnm.R; r++) {
				demands[k][r] = qnm.getDemand(k,r);
			}
		}

		delays = new double[qnm.R];
		for (int r=0; r<qnm.R; r++)
			delays[r] = qnm.getDelay(r);

		pop = new int[qnm.R];
		for (int r=0; r<qnm.R; r++)
			pop[r] = qnm.N.get(r);

		is_ld = false;
		serv = new int[qnm.M];
		for (int i=0; i<qnm.M; i++) {
			serv[i] = qnm.getNumberOfServers(i);
			if (serv[i]>1)
				is_ld = true;
		}
	}

	@Override
	public void printWelcome() {
		System.out.println("Using Monte Carlo Logistic Solver");
	}

	@Override
	public void computeNormalisingConstant() throws InternalErrorException {
		if (!is_nc_computed) {
			nc_timer.start();
			this.G = new BigRational(this.lcore.calculateNC());
			this.qnm.setNormalisingConstant(this.G);
			is_nc_computed = true;
			nc_timer.pause();
		}
	}

	public BigDecimal getNormalisingConstantAsBigDecimal() throws InternalErrorException {
		if (lcore.is_invalid_model())
			throw new InternalErrorException("Invalid model!");
		if (!is_nc_computed)
			throw new InternalErrorException("Normalising constant not yet computed!");
		return this.G.asBigDecimal();
	}

	@Override
	public void computePerformanceMeasures() throws InternalErrorException {
		perf_timer.start();
		//Array of Mean Throughputs per class
		BigRational[] X = new BigRational[qnm.R];

		//Array of Mean Queue Lengths
		BigRational[][] Q = new BigRational[qnm.M][qnm.R];

		if (!is_nc_computed)
			computeNormalisingConstant();
		if (lcore.is_invalid_model())
			throw new InternalErrorException("Invalid model!");

		// Throughput calculations
		for (int r = 0; r < qnm.R; r++) {
			if (pop[r] == 0) {
				X[r] = BigRational.ZERO;
				continue;
			}

			int[] pop_minus = pop.clone();
			pop_minus[r]--;

			LogisticCore NCS_minus = new LogisticCoreAdd(demands, serv, delays, pop_minus, epsilon, max_samples, nthread, MC);
			BigDecimal NC_minus = NCS_minus.calculateNC();
			X[r] = new BigRational(NC_minus.divide(qnm.getNormalisingConstant().asBigDecimal(), MC));
		}

		// Queue-length calculations
		if (is_ld) {
			// Calculation of mean QL for LD servers involve computing the marginal probs
			for (int k = 0; k < qnm.M; k++) {
				for (int r = 0; r < qnm.R; r++) {
					double qkr = 0;
					for (int n=1; n<=pop[r]; n++) {
						double p = marginal_prob(k,r,n);
						qkr += n*p;
					}
					Q[k][r] = new BigRational(qkr);
				}
			}
		} else {
			// Mean QL for Single server and Delay models, QL_{kr} = D_{kr}*G^(+k)/G
			for (int r = 0; r < qnm.R; r++) {
				for (int k = 0; k < qnm.M; k++) {
					if (pop[r] == 0) {
						Q[k][r] = BigRational.ZERO;
						continue;
					}

					int[] pop_minus = pop.clone();
					pop_minus[r]--;

					double[][] demands_plus;
					int[] serv_plus;

					demands_plus = augmentDemandsAtServer(qnm.M, qnm.R, demands, k);
					serv_plus = augmentSeverCountAtServer(qnm.M, qnm.R, serv, k);
					LogisticCore NCS_plus = new LogisticCoreAdd(demands_plus, serv_plus, delays, pop_minus, epsilon, max_samples, nthread, MC);

					BigDecimal NC_plus = NCS_plus.calculateNC();

					NC_plus = NC_plus.multiply(new BigDecimal(demands[k][r]));
					Q[k][r] = new BigRational(NC_plus.divide(qnm.getNormalisingConstant().asBigDecimal(), MC));
				}
			}
		}

		qnm.setPerformanceMeasures(Q,X);
		perf_timer.pause();
	}

	private double marginal_prob(int k, int r, int nr) throws InternalErrorException {
		/**
		 * Marginal probability of server k having class r population = nr
		 * for multiserver models, P(n_{kr} = nr | model)
		 * ref: Casale 2009, CoMoM: Efficient Class-Oriented Evaluation of
		 * Multiclass Performance Models
		 */

		// single queue system
		if (qnm.M==1) {
			if (nr == pop[r])
				return 1;
			return 0;
		}

		// Get the population vector for all classes other than r
		int[] Popnotr = new int[qnm.R-1];
		for (int i=0, j=0; i<qnm.R; i++) {
			if (i!=r) {
				Popnotr[j] = pop[i];
				j++;
			}
		}

		// Enumerate all possible states of server k, with specified population of
		// class r.
		BigDecimal P = BigDecimal.ZERO;
		int[] nknotr = pprod(Popnotr);
		do {
			int[] nk = new int[qnm.R];
			int nk_sum = 0;
			double logF = 0;

			int[] pop_minus = pop.clone();
			for (int i=0, j=0; i<qnm.R; i++) {
				if (i==r)
					nk[i] = nr;
				else {
					nk[i] = nknotr[j];
					j++;
				}
				nk_sum += nk[i];

				logF += nk[i]*Math.log(demands[k][i]);
				logF -= lnGamma(nk[i]+1);
				pop_minus[i] -= nk[i];
			}

			logF += lnGamma(nk_sum+1) - logbeta(nk_sum, serv[k]);

			BigDecimal F = exp(logF, MC);

			double[][] demands_minus = removeServer(qnm.M, qnm.R, demands, k);
			int[] serv_minus = removeSeverCountAtServer(qnm.M, qnm.R, serv, k);
			LogisticCore NCS_minus = new LogisticCoreAdd(demands_minus, serv_minus, delays, pop_minus, epsilon, max_samples, nthread, MC);
			BigDecimal NC_minus = NCS_minus.calculateNC();

			// P(n_{kr} = nr) = F_k(n_k)*G(-1_k, N - n_k)/G
			BigDecimal G = F;
			G = G.multiply(NC_minus, MC);
			P = P.add(G, MC);
		} while (pprod(nknotr, Popnotr));

		P = P.divide(qnm.getNormalisingConstant().asBigDecimal(), MC);
		return P.doubleValue();
	}

	private double logbeta(int n, int m) {
		if (n>m)
			return lnGamma(m+1) + (n-m)*Math.log(m);
		return lnGamma(n+1);
	}

	public double[] getStatPoint() {
		return this.lcore.getStationaryPointSimplex();
	}

	@Override
	public void printTimeStatistics() {
		long selfTime = nc_timer.getInterval() + perf_timer.getInterval();

		Timer selfTimer = new Timer(selfTime);

		System.out.println("NC time: " + nc_timer.getPrettyInterval());
		System.out.println("Performance Indices time: " + perf_timer.getPrettyInterval());
		System.out.println("Total time: " + selfTimer.getPrettyInterval());
	}

}
