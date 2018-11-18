package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.jet.math.Functions;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration.Function;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Utils;
import jmt.jmva.analytical.solvers.Utilities.MiscFunctions;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Implements the Logistic Function for single-server only networks, under the multiplicative transform
 */
public class LogisticFunctionMult extends LogisticFunction {

	public LogisticFunctionMult(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim, population, demands, epsilon, MC);
	}

	public LogisticFunctionMult(int ndim, int[] population, double[][] demands, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim, population, demands, epsilon, MC);
	}

	public LogisticFunctionMult(int ndim, int[] population, double[][] demands, double epsilon) throws InternalErrorException {
		this(ndim, population, demands, epsilon, MathContext.DECIMAL128);
	}

	/*Copy constructor*/
	public LogisticFunctionMult(LogisticFunctionMult LF) throws InternalErrorException {
		super(LF);
	}

	@Override
	public Function copy() throws InternalErrorException {
		return new LogisticFunctionMult(this);
	}

	@Override
	protected BigDecimal safeEvaluate(double[] point) throws InternalErrorException {
		// TODO: review the feasibility of computing the exponent purely with double precision
		DoubleMatrix1D x = new DenseDoubleMatrix1D(point);
		DoubleMatrix1D ex= new DenseDoubleMatrix1D(point);
		ex.assign(Functions.exp);

		double h = (1+epsilon*N)*x.zSum();
		DoubleMatrix1D y;

		y = new DenseDoubleMatrix1D(K);
		for (int k=0; k<K-1; k++) {
			y.set(k,1.);
			for (int i=k+1; i<K-1; i++)
				y.set(k, y.get(k)*(1+ex.get(i)));
			y.set(k, y.get(k)*ex.get(k));
		}
		y.set(K-1,1.);

		y = A.mult(A.transpose(demands), y.copy());
		y.assign(Functions.log);

		h += A.mult(pop, y);

		y = ex.copy();
		y.assign(Functions.plus(1));
		y.assign(Functions.log);
		h -= y.zSum()*N;

		y = ex.copy();
		y.assign(Functions.plus(1));
		y.assign(Functions.log);
		for (int k=0; k<K-1; k++)
			y.set(k, y.get(k)*(k-K)*(1+epsilon*N));
		h += y.zSum();

		if (Double.isInfinite(h) || Double.isNaN(h))
			return BigDecimal.ZERO;

		return MiscFunctions.exp(h, this.MC);
	}

	public void calculate_stationary_point() {
		super.calculate_stationary_point();
		x_stat = Utils.logistic_transform_mult(u_stat);
	}

	public void calculate_hessian() {
		super.calculate_hessian(); //Calculate Hessian in additive
		// transform it into multiplicative form
		DoubleMatrix2D J = Utils.compute_JacobianAdd2Mult(u_stat);
		DoubleMatrix2D hess_add = hess_stat.copy();
		hess_stat = this.A.mult(this.A.transpose(J), hess_add);
		hess_stat = this.A.mult(hess_stat, J);
	}

}
