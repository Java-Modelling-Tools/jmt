package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration.Function;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils;
import jmt.jmva.analytical.solvers.Utilities.MiscFunctions;

import java.math.BigDecimal;
import java.math.MathContext;

import static jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils.check_inputs;

/**
 * Extends the SafeFunction abstract class. This is the logistic function under the
 * additive transform without any delay
 */
public class LogisticFunction extends LogisticFunctionBase {

	protected int N;
	protected int R;
	protected int K;
	protected double eta;
	protected double epsilon;
	protected DoubleMatrix2D demands;
	protected DoubleMatrix2D demands_k; /*queues 1 to K-1*/
	protected DoubleMatrix1D demands_K; /*queues K*/
	protected DoubleMatrix1D pop;
	protected Algebra A;
	protected MathContext MC;

	/**
	 * Implements the safeEvaluate abstract method. Computes the logistic additive function
	 * at the point given by point
	 * @param point Array representing point to be evaluated
	 * @return BigDecimal equal to the function value at that point
	 */
	@Override
	protected BigDecimal safeEvaluate(double[] point) throws InternalErrorException {
		// TODO: review the feasibility of computing the exponent purely with double precision
		DoubleMatrix1D x = new DenseDoubleMatrix1D(point);
		DoubleMatrix1D ex= new DenseDoubleMatrix1D(point);
		ex.assign(Functions.exp);
		DoubleMatrix1D y;

		y = this.A.mult(this.A.transpose(this.demands_k), ex);
		if (y.size()==0)
			y = this.demands_K.copy();
		else
			y.assign(this.demands_K, Functions.plus);
		y.assign(Functions.log);

		double h = this.A.mult(y, this.pop);
		h -= this.eta*Math.log(1 + ex.zSum());
		h += (1+this.epsilon*(double)this.N)*x.zSum();

		if (Double.isInfinite(h) || Double.isNaN(h))
			return BigDecimal.ZERO;

		return MiscFunctions.exp(h, this.MC);
	}

	@Override
	protected void checkInDomain(double[] point) throws InternalErrorException {
		return;
	}

	public LogisticFunction(int ndim, int[] population, double[][] demands, double epsilon) throws InternalErrorException {
		this(ndim, population, demands, epsilon, MathContext.DECIMAL128);
	}

	public LogisticFunction(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim);
		this.initialize(ndim, population, demands, epsilon, MC);
	}

	public LogisticFunction(int ndim, int[] population, double[][] demands, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim);

		/*convert array into matrix objects*/
		DoubleMatrix1D pop_temp;
		DoubleMatrix2D demands_temp;

		try {
			pop_temp = new DenseDoubleMatrix1D(population.length);
			for (int i=0; i<population.length; i++)
				pop_temp.set(i, (double)population[i]);
			demands_temp = new DenseDoubleMatrix2D(demands);
		} catch (IllegalArgumentException e) {
			throw new InternalErrorException("Demands needs to be a rectangular 2D array!");
		}

		this.initialize(ndim, pop_temp, demands_temp, epsilon, MC);
	}

	/*Copy constructor*/
	public LogisticFunction(LogisticFunction LF) throws InternalErrorException {
		super(LF.ndim);
		N = LF.N;
		R = LF.R;
		K = LF.K;
		eta = LF.eta;
		epsilon = LF.epsilon;
		demands = LF.demands.copy();
		demands_k = LF.demands_k.copy(); /*queues 1 to K-1*/
		demands_K = LF.demands_K.copy(); /*queues K*/
		pop = LF.pop.copy();
		A = (Algebra)LF.A.clone();
		MC = LF.MC;
	}

	/**
	 * Implements the copy() function specified in base class
	 * @return copy of this function interface
	 * @throws InternalErrorException
	 */
	@Override
	public Function copy() throws InternalErrorException {
		return new LogisticFunction(this);
	}

	private void initialize(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, double epsilon, MathContext MC) throws InternalErrorException {
		this.A = new Algebra();
		this.MC = MC;

		this.pop = population.copy();
		this.demands = demands.copy();
		this.epsilon = epsilon;

		if (ndim != this.demands.rows()-1)
			throw new InternalErrorException("Number of integrated dimensions must be one less than number of queues!");

		check_inputs(this.pop, this.demands, this.epsilon);

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
	 * Iterative stationary point calculations for single server networks
	 */
	public void calculate_stationary_point() {
		int j=0;
		DoubleMatrix1D u0= new DenseDoubleMatrix1D(this.K);
		DoubleMatrix1D u1= new DenseDoubleMatrix1D(this.K);
		DoubleMatrix1D xi;
		DoubleMatrix1D du= new DenseDoubleMatrix1D(this.K);
		double eta = ((double)this.N) + ((double)this.K)*(1 + epsilon*((double)this.N));

		u0.assign(1e-05);
		do {
			xi = A.mult(A.transpose(this.demands), u0);
			xi.assign(Functions.pow(-1));
			xi.assign(this.pop, Functions.mult);

			for (int i=0; i<(u1.size()-1); i++) {
				u1.set(i, (1./eta)*( 1 + this.epsilon*this.N + u0.get(i)*this.A.mult(this.demands.viewRow(i), xi) ));
			}
			u1.set(u1.size()-1, 0);
			u1.set(u1.size()-1, 1-u1.copy().zSum());
			j++;

			du.assign(u0);
			du.assign(u1, Functions.minus);
			u0.assign(u1);
		} while ((Math.abs(du.zSum()) > 1e-10) || (j<1000));

		this.u_stat = u0.copy();
		this.x_stat = Utils.logistic_transform_add(u_stat);
	}

	/**
	 * Stationary point Hessian calculations for single server networks
	 */
	public void calculate_hessian() {
		DoubleMatrix1D xi;
		xi = A.mult(A.transpose(this.demands), this.u_stat);
		xi.assign(Functions.pow(-1));
		xi.assign(this.pop, Functions.mult);
		DoubleMatrix1D xi2 = xi.copy();
		xi2.assign(Functions.pow(2));
		xi2.assign(this.pop, Functions.div);

		double eta = ((double)this.N) + ((double)this.K)*(1 + epsilon*((double)this.N));

		this.hess_stat = new DenseDoubleMatrix2D(this.u_stat.size(), this.u_stat.size());
		this.hess_stat.assign(0);

		//temporary loop variables
		DoubleMatrix1D t = new DenseDoubleMatrix1D(this.pop.size());
		double q;

		for (int i=0; i<(this.u_stat.size()-1); i++) {
			for (int j=i+1; j<this.u_stat.size(); j++) {
				t.assign(xi2);
				t.assign(this.demands.viewRow(i), Functions.mult);
				q = this.A.mult(t,this.demands.viewRow(j)) - eta;
				q *= this.u_stat.get(i)*this.u_stat.get(j);
				this.hess_stat.set(i,j,q);
				this.hess_stat.set(j,i,q);
			}
		}

		for (int i=0; i<this.u_stat.size(); i++)
			this.hess_stat.set(i,i,-1*this.hess_stat.viewRow(i).zSum());

		if (K==1) {
			this.hess_stat = new DenseDoubleMatrix2D(0, 0);
			return;
		}

		this.hess_stat = this.A.subMatrix(this.hess_stat,0,this.K-2,0,this.K-2);
	}

}
