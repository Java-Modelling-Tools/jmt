package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.math.MathContext;

/**
 * Additive logistic transformed version of the Logistic Core.
 * Also returns the correct logistic function
 */
public class LogisticCoreAdd extends LogisticCore {

	public LogisticCoreAdd(double[][] demands, int[] serv, double[] delays, int[] population, double epsilon, int max_samples, int nthreads, MathContext mc) throws InternalErrorException {
		super(demands, serv, delays, population, epsilon, max_samples, mc, nthreads);
	}

	public LogisticCoreAdd(double[][] demands, int[] serv, double[] delays, int[] population, double epsilon, int max_samples) throws InternalErrorException {
		this(demands, serv, delays, population, epsilon, max_samples, 1, MathContext.DECIMAL128);
	}

	/**
	 * Fetch the correct logistic functions to be used in integration
	 * @return Function interface to the correct logistic function
	 * @throws InternalErrorException
	 */
	protected LogisticFunctionBase initialize_logistic_function() throws InternalErrorException {
		if (has_delay)
			return new LogisticFunctionDelay(this.K, this.pop, this.demands, this.delays, epsilon, MC);
		if (has_multi)
			return new LogisticFunctionMSBD(this.K - 1, this.pop, this.demands, serv, epsilon, MC);
		return new LogisticFunction(this.K - 1, this.pop, this.demands, epsilon, MC);
	}

}
