package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Distributions.MultiVariateStudentT;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration.MCIntegrator;
import jmt.jmva.analytical.solvers.Utilities.MiscFunctions;

import java.math.BigDecimal;
import java.math.MathContext;

import static jmt.engine.math.GammaFun.lnGamma;
import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.check_inputs;
import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.isInvalidModel;

/**
 * Implements the core Logistic Sampling algorithm by calling the required
 * stationary point calculations, Hessian calculations, and the correct
 * integration scheme for solution of the model, to calculate the NC.
 * See Ong 2018.
 * Subclassed by the additive and multiplicative core, which are responsible
 * for choosing the correct logistic functions for use by the core algorithm
 */
public abstract class LogisticCore {

	/* model input */
	protected DoubleMatrix2D demands;
	protected DoubleMatrix1D delays;
	protected boolean has_delay;
	protected boolean has_multi; //is multi server
	protected double sigma; //sum of delays
	protected DoubleMatrix1D pop;
	protected DoubleMatrix1D serv;
	protected int N;
	protected int K;
	protected int R;

	/* solver specific input */
	protected double epsilon;

	/* derived properties */
	protected DoubleMatrix1D x_stat;
	protected DoubleMatrix2D hess;
	protected DoubleMatrix2D cov;

	/* others */
	protected boolean invalid_model;
	protected boolean zero_population;
	protected Algebra A;
	protected MCIntegrator MCI;
	protected LogisticFunctionBase F;
	protected MathContext MC;

	public LogisticCore(double[][] demands, int[] serv, double[] delays, int[] population, double epsilon, int max_samples, MathContext mc, int nthreads) throws InternalErrorException {
		this.MC = mc;
		this.initialize(demands, serv, delays, population, epsilon);

		if (this.invalid_model || this.zero_population)
			return;

		/* decide what the required computations are */
		if (this.sigma > 0) {
			/* non-zero delay */
			if (this.serv.zSum() > K) /* no support for both delay and multi-server */
				throw new InternalErrorException("Cannot support delays and multiservers! Use an N-server station instead!");
			this.has_delay = true;
		} else {
			if (this.serv.zSum() > K)
				this.has_multi = true;
			this.has_delay = false;
		}

		// Fetch the correct logistic function, and calculate stationary points and Hessians
		F = this.initialize_logistic_function();
		F.calculate_stationary_point();
		F.calculate_hessian();
		//fetch the results
		this.x_stat = F.getX_stat();
		this.hess = F.getHess_stat();
		//calculate covariance
		this.cov = this.hess.copy();
		this.cov = this.A.inverse(this.cov.copy());
		// "symmetrize" the covariance
		this.cov.assign(this.A.transpose(this.cov.copy()), Functions.plus);
		this.cov.assign(Functions.div(2.));

		check_transformed_matrices();

		// initialize the Integrator stationary point and calculated covariance
		this.MCI = new MCIntegrator(
				F,
				new MultiVariateStudentT(this.x_stat.toArray(), cov.toArray(), 4),
				max_samples,
				MC,
				nthreads
				);
	}

	private void initialize(double[][] demands, int[] serv, double[] delays, int[] population, double epsilon) throws InternalErrorException {
		// convert all model parameters into DoubleMatrix format, perform checks
		this.A = new Algebra();

		try {
			this.pop = new DenseDoubleMatrix1D(population.length);
			for (int i=0; i<population.length; i++)
				this.pop.set(i, (double)population[i]);
			this.delays = new DenseDoubleMatrix1D(delays);
			this.demands = new DenseDoubleMatrix2D(demands);
			this.serv = new DenseDoubleMatrix1D(demands.length);
			this.serv.assign(1);
			for (int i=0; i<serv.length; i++)
				this.serv.set(i, (double)serv[i]);
		} catch (IllegalArgumentException e) {
			throw new InternalErrorException("Demands needs to be a rectangular 2D array!");
		}

		this.N = (int)this.pop.zSum();
		this.K = this.demands.rows();
		this.R = this.demands.columns();

		this.sigma = this.delays.zSum();

		this.epsilon = epsilon;

		check_inputs(this.pop, this.demands, this.serv, this.delays, this.epsilon);
		if (isInvalidModel(this.demands, this.delays)) {
			this.invalid_model = true;
			return;
		}

		sanitize_model();
		if (N==0)
			this.zero_population = true;
	}

	/*=========== abstract functions to be implemented by subclass =============*/
	protected abstract LogisticFunctionBase initialize_logistic_function () throws InternalErrorException;

	/*================ sanity checks after applying transforms =================*/
	private void check_transformed_matrices() throws InternalErrorException {
		int D;
		if (has_delay)
			D = K;
		else
			D = K-1;
		if ((x_stat.size() != D) || (hess.rows() != D) || (hess.columns() != D))
			throw new InternalErrorException("Transformed matrices not corect dimensions!");
	}

	private void sanitize_model() {
		//perform sanitization, and update number of servers, as this could have changed
		//this.demands = sanitize_demands(this.demands);
		//this.K = this.demands.rows(); //number of servers may have changed

		// perform population sanitization
		IntArrayList indexList = new IntArrayList();
		DoubleArrayList valueList = new DoubleArrayList();
		pop.getNonZeros(indexList, valueList);
		DoubleMatrix1D pop_s = new DenseDoubleMatrix1D(indexList.size());
		DoubleMatrix2D demands_s = new DenseDoubleMatrix2D(K, indexList.size());
		DoubleMatrix1D delays_s = new DenseDoubleMatrix1D(indexList.size());

		int i = 0;

		for (int r=0; r<R; r++) {
			if (pop.get(r) == 0)
				continue;

			pop_s.set(i, pop.get(r));
			delays_s.set(i, delays.get(r));
			for (int k=0; k<K; k++) {
				demands_s.set(k,i, demands.get(k,r));
			}
			i++;
		}

		/* replace with sanitized */
		pop = pop_s;
		demands = demands_s;
		delays = delays_s;
		N = (int)pop.zSum();
		R = pop.size();
	}

	/*================ result calculations =================*/

	/**
	 * Calculate the NC, and multiply by the appropriate coefficients. Details see Casale 2017 and Ong 2018
	 * @return The Normalising constant in BigDecimal form
	 * @throws InternalErrorException
	 */
	public BigDecimal calculateNC() throws InternalErrorException {
		if (this.invalid_model)
			return BigDecimal.ZERO;

		if (this.zero_population)
			return BigDecimal.ONE;

		double logC = 0;
		if (!has_multi && !has_delay) {
			double eta = (double) this.N + this.K * (1 + this.epsilon * (double) this.N);
			logC = lnGamma(eta);
		}

		if (!has_multi)
			logC -= this.K*lnGamma(1. + this.epsilon*this.N);

		for (int i=0; i<this.pop.size(); i++)
			logC -= lnGamma(this.pop.get(i) + 1);

		BigDecimal I = this.MCI.compute();

		return I.multiply(MiscFunctions.exp(logC, MC));
	}

	/**
	 * Get Hessian
	 * @return
	 */
	public double[][] getHessian() {
		return this.hess.toArray();
	}

	public double[][] getCovariance() {
		return this.cov.toArray();
	}

	public double[] getStationaryPoint() {
		return this.x_stat.toArray();
	}

	public double[] getStationaryPointSimplex() { return F.getU_stat().toArray(); }

	public boolean is_invalid_model() {
		return invalid_model;
	}

}
