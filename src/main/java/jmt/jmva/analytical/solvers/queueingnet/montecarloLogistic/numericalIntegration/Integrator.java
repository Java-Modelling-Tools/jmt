package jmt.jmva.analytical.solvers.queueingnet.montecarloLogistic.numericalIntegration;

import java.math.BigDecimal;

import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;

/**
 * Interface to an integrator. Integrates a function over the domain.
 * Integration to be performed in the compute() method
 */
public interface Integrator {

	void initialise(Function function);

	BigDecimal compute() throws InternalErrorException;

}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
