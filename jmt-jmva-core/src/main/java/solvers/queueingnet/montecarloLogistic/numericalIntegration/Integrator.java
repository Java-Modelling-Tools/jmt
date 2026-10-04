package solvers.queueingnet.montecarloLogistic.numericalIntegration;

import java.math.BigDecimal;

import solvers.exceptions.InternalErrorException;

/**
 * Interface to an integrator. Integrates a function over the domain.
 * Integration to be performed in the compute() method
 */
public interface Integrator {

	void initialise(Function function);

	BigDecimal compute() throws InternalErrorException;

}
