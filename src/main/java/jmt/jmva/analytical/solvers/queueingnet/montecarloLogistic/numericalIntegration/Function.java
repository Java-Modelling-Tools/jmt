package jmt.jmva.analytical.solvers.queueingnet.montecarloLogistic.numericalIntegration;

import java.math.BigDecimal;

import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;

/**
 * Interface for a function over a multidimensional domain to be integrated
 */
public interface Function {

	/**
	 * Evaluate the function at a specified point in the domain
	 * 
	 * @param point Array representing point to be evaluated
	 * @return a BigDecimal object representing value of that point
	 * @throws InternalErrorException when evaluation of function is impossible at
	 *                                the specified point
	 */
	BigDecimal evaluate(double[] point) throws InternalErrorException;

	Function copy() throws InternalErrorException;

}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
