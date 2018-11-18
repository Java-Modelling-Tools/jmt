package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration.Function;
import jmt.jmva.analytical.solvers.Utilities.MiscFunctions;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.check_inputs;
import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.logistic_transform_add;

/**
 * Implementation of logistic function for single + delay servers under the additive transform
 */
public class LogisticFunctionDelay extends LogisticFunctionBase {

	protected int N;
	protected int R;
	protected int K;
	protected double eta;
	protected double epsilon;
	protected DoubleMatrix2D demands;
	protected DoubleMatrix2D demands_k; /*queues 1 to K-1*/
	protected DoubleMatrix1D demands_K; /*queues K*/
	protected DoubleMatrix1D delays;
	protected DoubleMatrix1D pop;
	protected Algebra A;
	protected MathContext MC;

	@Override
	protected BigDecimal safeEvaluate(double[] point)  throws InternalErrorException {
		// TODO: review the feasibility of computing the exponent purely with double precision
		DoubleMatrix1D x;
		if (point.length == 1)
			x = new DenseDoubleMatrix1D(0);
		else
			x = new DenseDoubleMatrix1D(Arrays.copyOfRange(point, 1, K));

		DoubleMatrix1D ex = x.copy();
		ex.assign(Functions.exp);
		double x0 = point[0];
		double ex0 = Math.exp(x0);

		/*temporary variables*/
		DoubleMatrix1D y;
		DoubleMatrix1D z;

		y = this.A.mult(this.A.transpose(this.demands_k), ex);
		y.assign(Functions.mult(ex0));
		z = this.delays.copy();
		z.assign(Functions.mult(ex.zSum()));
		if (y.size()==0)
			y = z.copy();
		else
			y.assign(z, Functions.plus);

		z = this.demands_K.copy();
		z.assign(Functions.mult(ex0));
		y.assign(z, Functions.plus);

		y.assign(this.delays, Functions.plus);

		y.assign(Functions.log);

		double h = this.A.mult(this.pop, y);
		h -= this.eta*Math.log(1 + ex.zSum());
		h += (1+this.epsilon*(double)this.N)*x.zSum();
		h += K*(1+epsilon*N)*x0 - ex0;

		if (Double.isInfinite(h) || Double.isNaN(h))
			return BigDecimal.ZERO;

		return MiscFunctions.exp(h, this.MC);
	}

	@Override
	protected void checkInDomain(double[] point) throws InternalErrorException {
		return;
	}

	public LogisticFunctionDelay(int ndim,  int[] population, double[][] demands, double[] delays, double epsilon) throws InternalErrorException {
		this(ndim, population, demands, delays, epsilon, MathContext.DECIMAL128);
	}

	public LogisticFunctionDelay(int ndim,  int[] population, double[][] demands, double[] delays, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim);

		/*convert array into matrix objects*/
		DoubleMatrix1D pop_temp;
		DoubleMatrix2D demands_temp;
		DoubleMatrix1D delays_temp;

		try {
			pop_temp = new DenseDoubleMatrix1D(population.length);
			for (int i=0; i<population.length; i++)
				pop_temp.set(i, (double)population[i]);
			demands_temp = new DenseDoubleMatrix2D(demands);
			delays_temp = new DenseDoubleMatrix1D(delays);
		} catch (IllegalArgumentException e) {
			throw new InternalErrorException("Demands needs to be a rectangular 2D array!");
		}

		this.initialize(ndim, pop_temp, demands_temp, delays_temp, epsilon, MC);
	}

	public LogisticFunctionDelay(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D delays, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim);
		this.initialize(ndim, population, demands, delays, epsilon, MC);
	}

	/*Copy constructor*/
	public LogisticFunctionDelay(LogisticFunctionDelay LFD) {
		super(LFD.ndim);
		N = LFD.N;
		R = LFD.R;
		K = LFD.K;
		eta = LFD.eta;
		epsilon = LFD.epsilon;
		demands = LFD.demands.copy();
		demands_k = LFD.demands_k.copy(); /*queues 1 to K-1*/
		demands_K = LFD.demands_K.copy(); /*queues K*/
		pop = LFD.pop.copy();
		delays = LFD.delays.copy();
		A = (Algebra)LFD.A.clone();
		MC = LFD.MC;
	}

	/* This seems like witchcraft */
	@Override
	public Function copy() throws InternalErrorException {
		return new LogisticFunctionDelay(this);
	}

	private void initialize(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D delays, double epsilon, MathContext MC) throws InternalErrorException {
		this.A = new Algebra();
		this.MC = MC;

		this.pop = population.copy();
		this.demands = demands.copy();
		this.delays = delays.copy();

		if (ndim != this.demands.rows())
			throw new InternalErrorException("Number of integrated dimensions must be one equal number of queues!");

		this.epsilon = epsilon;

		check_inputs(this.pop, this.demands, this.delays, this.epsilon);

		this.N = (int)this.pop.zSum();
		this.K = this.demands.rows();
		this.R = this.demands.columns();
		this.eta = (double)this.N + this.K*(1+this.epsilon*(double)this.N);

		if (this.K == 1)
			this.demands_k = new DenseDoubleMatrix2D(0,0); //empty matrix
		else
			this.demands_k = this.A.subMatrix(this.demands, 0, this.K-2, 0, this.R-1);
		this.demands_K = this.demands.viewRow(this.K-1).copy();
	}

	/**
	 * Iterative stationary point calculations for single + delay server networks.
	 */
	public void calculate_stationary_point() {
		int j=0;
		DoubleMatrix1D u0= new DenseDoubleMatrix1D(this.K);
		DoubleMatrix1D u1= new DenseDoubleMatrix1D(this.K);
		double v0 = 1e-05;
		double v1;
		DoubleMatrix1D xi;
		DoubleMatrix1D du = new DenseDoubleMatrix1D(this.K);
		double dv;
		double eta = ((double)this.N) + ((double)this.K)*(1 + epsilon*((double)this.N));

		u0.assign(1e-05);

		/* temporary variables */
		DoubleMatrix1D y;

		do {
			xi = A.mult(A.transpose(this.demands), u0);
			xi.assign(Functions.mult(v0));
			xi.assign(this.delays, Functions.plus);
			xi.assign(Functions.pow(-1));
			xi.assign(this.pop, Functions.mult);

			for (int i=0; i<u1.size()-1; i++) {
				y = this.demands.viewRow(i).copy();
				y.assign(Functions.mult(v0));
				y.assign(this.delays, Functions.plus);
				u1.set(i, (1./eta)*( 1 + this.epsilon*this.N + u0.get(i)*this.A.mult(xi, y) ));
			}
			u1.set(u1.size()-1, 0);
			u1.set(u1.size()-1, 1-u1.copy().zSum());
			v1 = eta + 1 - this.A.mult(xi, this.delays);
			j++;

			du.assign(u0);
			du.assign(u1, Functions.minus);
			dv = v0-v1;
			u0.assign(u1);
			v0 = v1;
		} while ((Math.abs(du.zSum()) + Math.abs(dv) > 1e-10) || (j<1000));

		this.u_stat = u0.copy();
		this.v_stat = v1;
		this.x_stat = logistic_transform_add(u_stat, v_stat);
	}

	/**
	 * Stationary point Hessian calculations for single + delay server networks
	 */
	public void calculate_hessian() {
		DoubleMatrix1D xi;
		xi = A.mult(A.transpose(this.demands), u_stat);
		xi.assign(Functions.mult(v_stat));
		xi.assign(this.delays, Functions.plus);
		xi.assign(Functions.pow(-1));
		xi.assign(this.pop, Functions.mult);

		DoubleMatrix1D xi2 = xi.copy();
		xi2.assign(Functions.pow(2));

		double eta = ((double)this.N) + ((double)this.K)*(1 + epsilon*((double)this.N));

		this.hess_stat = new DenseDoubleMatrix2D(K+1,K+1);

		DoubleMatrix2D demand_hat = this.demands.copy();
		demand_hat.assign(Functions.mult(v_stat));
		for (int k=0; k<K; k++) {
			demand_hat.viewRow(k).assign(delays, Functions.plus);
		}

		/* set the Hessian for the v dimension*/
		/* temporary variables */
		DoubleMatrix1D y;
		DoubleMatrix1D z;
		int l;
		for (int i=1; i<K+1; i++) {
			l = i-1;

			y = xi2.copy();
			y.assign(pop, Functions.div);
			y.assign(demand_hat.viewRow(l), Functions.mult);

			z = this.A.mult(this.A.transpose(demands), u_stat);
			y.assign(z, Functions.mult);

			z = demands.viewRow(l).copy();
			z.assign(xi, Functions.mult);
			y.assign(z, Functions.minus);

			this.hess_stat.set(0, i, u_stat.get(l)*v_stat*y.zSum());
			this.hess_stat.set(i, 0, this.hess_stat.get(0,i));
		}
		y = xi2.copy();
		y.assign(pop, Functions.div);
		y.assign(delays, Functions.mult);
		z = this.A.mult(this.A.transpose(demands), u_stat);
		y.assign(z, Functions.mult);
		this.hess_stat.set(0, 0, v_stat*(1-y.zSum()));

		/* calculate Hessian for the u dimensions */
		DoubleMatrix2D hess_u = new DenseDoubleMatrix2D(K,K);
		hess_u.assign(0);
		for (int i=0; i<K-1; i++) {
			for (int j=i+1; j<K; j++) {
				y = xi2.copy();
				y.assign(pop, Functions.div);
				y.assign(demand_hat.viewRow(i), Functions.mult);
				y.assign(demand_hat.viewRow(j), Functions.mult);
				hess_u.set(i,j, u_stat.get(i)*u_stat.get(j)*(y.zSum()-eta));
				hess_u.set(j,i, hess_u.get(i,j));
			}
		}
		for (int i=0; i<K; i++) {
			hess_u.set(i, i, -1*hess_u.viewRow(i).copy().zSum());
		}

		for (int i=0; i<K; i++) {
			for (int j=0; j<K; j++) {
				this.hess_stat.set(i+1, j+1, hess_u.get(i,j));
			}
		}

		this.hess_stat = this.A.subMatrix(this.hess_stat,0,this.K-1,0,this.K-1);
	}

}
