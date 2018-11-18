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
import java.util.Arrays;

/**
 * Implements the Logistic Function for delay + single server networks under the multiplicative transform
 */
public class LogisticFunctionMultDelay extends LogisticFunctionDelay {

	public LogisticFunctionMultDelay(int ndim, DoubleMatrix1D population, DoubleMatrix2D demands, DoubleMatrix1D delays, double epsilon, MathContext MC) throws InternalErrorException {
		super(ndim, population, demands, delays, epsilon, MC);
	}

	public LogisticFunctionMultDelay(int ndim, int[] population, double[][] demands, double[] delays, double epsilon) throws InternalErrorException {
		super(ndim, population, demands, delays, epsilon, MathContext.DECIMAL128);
	}

	/*Copy constructor*/
	public LogisticFunctionMultDelay(LogisticFunctionMultDelay LFD) throws InternalErrorException {
		super(LFD);
	}

	@Override
	public Function copy() throws InternalErrorException {
		return new LogisticFunctionMultDelay(this);
	}

	@Override
	protected BigDecimal safeEvaluate(double[] point) throws InternalErrorException {
		DoubleMatrix1D x;
		if (point.length == 1)
			x = new DenseDoubleMatrix1D(0);
		else
			x = new DenseDoubleMatrix1D(Arrays.copyOfRange(point, 1, K));

		DoubleMatrix1D ex = x.copy();
		ex.assign(Functions.exp);
		double x0 = point[0];
		double ex0 = Math.exp(x0);

		double h = (1+epsilon*N)*x.zSum() - ex0 + K*(1+epsilon*N)*x0;

		DoubleMatrix1D y = ex.copy();
		y.assign(Functions.plus(1));
		y.assign(Functions.log);
		h -= y.zSum()*N;

		y = new DenseDoubleMatrix1D(K);
		for (int k=0; k<K-1; k++) {
			y.set(k,1.);
			for (int i=k+1; i<K-1; i++)
				y.set(k, y.get(k)*(1+ex.get(i)));
			y.set(k, y.get(k)*ex.get(k));
		}
		y.set(K-1,1.);

		y = A.mult(A.transpose(demands), y.copy());
		y.assign(Functions.mult(ex0));

		double g = 1.;
		for (int i=0; i<K-1; i++)
			g *= 1+ex.get(i);
		DoubleMatrix1D z = delays.copy();
		z.assign(Functions.mult(g));
		y.assign(z, Functions.plus);
		y.assign(Functions.log);
		h += A.mult(pop, y);

		y = ex.copy();
		y.assign(Functions.plus(1));
		y.assign(Functions.log);
		for (int i=0; i<K-1; i++)
			y.set(i, y.get(i)*(i-K)*(1+N*epsilon));
		h += y.zSum();

		if (Double.isInfinite(h) || Double.isNaN(h))
			return BigDecimal.ZERO;

		return MiscFunctions.exp(h, this.MC);
	}

	public void calculate_stationary_point() {
		super.calculate_stationary_point();
		x_stat = Utils.logistic_transform_mult(u_stat, v_stat);
	}

	public void calculate_hessian() {
		super.calculate_hessian(); //calculate in additive
		// transform it into multiplicative form
		DoubleMatrix2D J = Utils.compute_JacobianAdd2Mult(u_stat,v_stat);
		DoubleMatrix2D hess_add = hess_stat.copy();
		hess_stat = this.A.mult(this.A.transpose(J), hess_add);
		hess_stat = this.A.mult(hess_stat, J);
	}

}
