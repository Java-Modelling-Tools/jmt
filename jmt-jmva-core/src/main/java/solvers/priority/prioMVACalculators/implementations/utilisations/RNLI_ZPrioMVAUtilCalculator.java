package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.interpolators.ReflectedNonLinearInterpolator;
import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class RNLI_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - Zero
    public RNLI_ZPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), RETURN_ZERO);
    }
}
