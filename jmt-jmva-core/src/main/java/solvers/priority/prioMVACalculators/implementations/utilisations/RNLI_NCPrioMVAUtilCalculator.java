package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.interpolators.ReflectedNonLinearInterpolator;
import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class RNLI_NCPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - No Change
    public RNLI_NCPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), NO_CHANGE);
    }
}
