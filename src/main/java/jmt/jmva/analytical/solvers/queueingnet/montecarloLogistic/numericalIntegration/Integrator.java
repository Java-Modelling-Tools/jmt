package jmt.jmva.analytical.solvers.queueingNet.monteCarloLogistic.numericalIntegration;

import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;

import java.math.BigDecimal;

/**
 * Interface to an integrator. Integrates a function over the domain.
 * Integration to be performed in the compute() method
 */
public interface Integrator {

	void initialise(Function function);

	BigDecimal compute() throws InternalErrorException;

}
