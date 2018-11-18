package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration;

import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.math.BigDecimal;

/**
 * Abstract class that implements the Function interface. Requires that dimensionality and the location of
 * the point in the domain be enforced before a safe evaluation of the function is performed
 */
public abstract class SafeFunction implements Function {

	protected final int ndim;

	/**
	 * Initialized the SafeFunction object
	 * @param ndim dimensionality of the function
	 */
	public SafeFunction(int ndim) {
		this.ndim = ndim;
	}

	/**
	 * Checks that the provided point has length equal the function domain dimensionality
	 * @param point Point to be checked
	 * @throws InternalErrorException When point is invalid
	 */
	protected void checkPoint(double[] point) throws InternalErrorException {
		if (point.length != this.ndim) throw new InternalErrorException("Dimensionality of integrated point invalid!");
	}

	/**
	 * Function that checks point in within the domain of the function
	 * @param point Point to be checked
	 * @throws InternalErrorException When point is invalid
	 */
	protected abstract void checkInDomain(double[] point) throws InternalErrorException;

	/**
	 * Implements the Function evaluate interface with a safe one
	 * @param point Array representing point to be evaluated
	 * @return Function value at point
	 * @throws InternalErrorException When point is not in function domain
	 */
	@Override
	public BigDecimal evaluate(double[] point) throws InternalErrorException {
		this.checkPoint(point);
		this.checkInDomain(point);
		return this.safeEvaluate(point);
	}

	/**
	 * The evaluation of the function, given that the point is within the domain
	 * @param point Array representing point to be evaluated
	 * @return Function value at point
	 */
	protected abstract BigDecimal safeEvaluate(double[] point) throws InternalErrorException;

}
