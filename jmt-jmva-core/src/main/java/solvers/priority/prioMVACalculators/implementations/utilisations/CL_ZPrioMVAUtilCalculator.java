package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.interpolators.LinearInterpolator;
import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

/**
 * Provides the CL_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the CL_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the CL_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the CL_Z Prio MVA Util Calculator functionality of the utilisations.
 */
public class CL_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Named after: Chandy and Lakshmi
    // C & L had nothing to do with this variation though
    // When x1 = 0, it doesn't interpolate, it just returns 0

    public CL_ZPrioMVAUtilCalculator() {
        super(new LinearInterpolator(), RETURN_ZERO);
    }
}
