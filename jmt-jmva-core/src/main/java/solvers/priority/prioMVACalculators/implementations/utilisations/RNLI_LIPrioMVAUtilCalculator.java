package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import solvers.priority.interpolators.ReflectedNonLinearInterpolator;

public class RNLI_LIPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - Linear Interpolate
    public RNLI_LIPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), LINEAR_INTERPOLATE);
    }
}
