package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import solvers.priority.interpolators.ReflectedNonLinearInterpolator;

public class RNLI_YPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - Y1
    public RNLI_YPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), RETURN_Y_ONE);
    }
}
