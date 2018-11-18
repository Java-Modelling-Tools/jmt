package jmt.jmva.analytical.solvers.QueueingNet.MonteCarloLogistic.NumericalIntegration;

import jmt.jmva.analytical.solvers.Exceptions.InternalErrorException;

import java.math.BigDecimal;

/**
 * Interface to an integrator. Integrates a function over the domain.
 * Integration to be performed in the compute() method
 */
public interface Integrator {

	void initialise(Function function);

	BigDecimal compute() throws InternalErrorException;

}
