package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration;

import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.math.BigDecimal;

/**
 * Interface for a function over a multidimensional domain to be integrated
 */
public interface Function {

	/**
	 * Evaluate the function at a specified point in the domain
	 * @param point Array representing point to be evaluated
	 * @return a BigDecimal object representing value of that point
	 * @throws InternalErrorException when evaluation of function is impossible at the specified point
	 */
	BigDecimal evaluate(double[] point) throws InternalErrorException;

	Function copy() throws InternalErrorException;

}
