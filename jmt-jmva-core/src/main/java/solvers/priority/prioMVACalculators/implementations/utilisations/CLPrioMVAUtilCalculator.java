package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import solvers.priority.interpolators.LinearInterpolator;

/**
 * Provides the CL Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the CL Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the CL Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the CL Prio MVA Util Calculator functionality of the utilisations.
 */
public class CLPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    /**
     * $\rho_{i, cl}(\vn) = \rho_{i, cl}(\vn - \vQ_{i,cl})$
     * where $\vQ_{i,cl} = \vOne_{cl} \cdot \oQ_{i, cl}(\vn)$
     * Named after: Chandy and Lakshmi
     */

    public CLPrioMVAUtilCalculator() {
        super(new LinearInterpolator(), NO_CHANGE);
    }
}
