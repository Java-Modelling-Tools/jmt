package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.Solver;

import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.math.MathContext;

/**
 * Multiplicative logistic transformed version of the Logistic Core.
 * Also returns the correct logistic function
 */
public class LogisticCoreMult extends LogisticCore {

	public LogisticCoreMult(double[][] demands, int[] serv, double[] delays, int[] population, double epsilon, int max_samples, int nthreads, MathContext mc) throws InternalErrorException {
		super(demands, serv, delays, population, epsilon, max_samples, mc, nthreads);
	}

	public LogisticCoreMult(double[][] demands, int[] serv, double[] delays, int[] population, double epsilon, int max_samples) throws InternalErrorException {
		this(demands, serv, delays, population, epsilon, max_samples, 1, MathContext.DECIMAL128);
	}

	/**
	 * Fetch the correct logistic functions to be used in integration
	 * @return
	 * @throws InternalErrorException
	 */
	protected LogisticFunctionBase initialize_logistic_function() throws InternalErrorException {
		if (has_delay)
			return new LogisticFunctionMultDelay(this.K, this.pop, this.demands, this.delays, epsilon, MC);
		if (has_multi) // TODO: (future work) implement multiplicative multi-server logistic function
			throw new InternalErrorException("Multiserver not supported for multiplicative transform!");
		return new LogisticFunctionMult(this.K - 1, this.pop, this.demands, epsilon, MC);
	}

}
