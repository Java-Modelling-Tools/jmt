package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Distributions;

import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

/**
 * Interface for use in the MonteCarloLogistic Solver. Enables easy switching and cloning
 * of distributions. offers methods to generate samples which are of specified dimensionality
 * and to calculate pdf of a point belonging in the domain of the distribution
 */
public interface MultiVariateRealDistribution {

	int getDim();

	/**
	 * Get sample from this distribution. Since it is multivariate, return type is an array
	 * @return double array - sample from this distribution
	 */
	double[] getSample();

	/**
	 * pdf of specified point in this distribution
	 * @param point point to evaluate pdf at
	 * @return double which is the pdf
	 * @throws InternalErrorException
	 */
	double pdf(double[] point) throws InternalErrorException;

	/**
	 * required interface to copy distribution object
	 * @return a MVRD
	 * @throws InternalErrorException
	 */
	MultiVariateRealDistribution copy() throws InternalErrorException;

	/**
	 * copy distribution with specified integer seed
	 * @param seed integer to seed the distribution with
	 * @return a copy of the distribution, with a different seed
	 * @throws InternalErrorException
	 */
	MultiVariateRealDistribution copy(int seed) throws InternalErrorException;

}
