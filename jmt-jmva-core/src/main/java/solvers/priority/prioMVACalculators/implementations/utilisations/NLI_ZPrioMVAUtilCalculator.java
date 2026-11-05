package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import solvers.priority.interpolators.NonLinearInterpolator;

/**
 * Provides the NLI_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the NLI_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the NLI_Z Prio MVA Util Calculator functionality of the utilisations.
 */
/**
 * Provides the NLI_Z Prio MVA Util Calculator functionality of the utilisations.
 */
public class NLI_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Non-Linear Interpolator - Zero
    public NLI_ZPrioMVAUtilCalculator() {
       super(new NonLinearInterpolator(), RETURN_ZERO);
    }

}
