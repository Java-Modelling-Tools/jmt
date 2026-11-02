package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.interpolators.ReflectedNonLinearInterpolator;
import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

/**
 * Provides the RNLI_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the RNLI_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the RNLI_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the RNLI_Z Prio MVA Util Calculator functionality of the utilisations.
 */
public class RNLI_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - Zero
    public RNLI_ZPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), RETURN_ZERO);
    }
}
