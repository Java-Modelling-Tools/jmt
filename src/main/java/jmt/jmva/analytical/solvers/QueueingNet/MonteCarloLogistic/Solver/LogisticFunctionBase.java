package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;
import jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration.SafeFunction;

/**
 * Extension of the SafFunction base class. Provides basic, reusable structure of logistic functions
 */
public abstract class LogisticFunctionBase extends SafeFunction {

	protected DoubleMatrix1D u_stat;
	protected double v_stat = 0;
	protected DoubleMatrix1D x_stat;
	protected DoubleMatrix2D hess_stat;

	public LogisticFunctionBase(int ndim) {
		super(ndim);
	}

	public abstract void calculate_stationary_point() throws InternalErrorException;

	public abstract void calculate_hessian() throws InternalErrorException;

	public DoubleMatrix1D getU_stat() { return u_stat.copy(); }

	public double getV_stat() { return v_stat; }

	public DoubleMatrix1D getX_stat() { return x_stat.copy(); }

	public DoubleMatrix2D getHess_stat() { return hess_stat.copy(); }

}
