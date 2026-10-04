package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import solvers.priority.interpolators.NonLinearInterpolator;

public class NLI_LIPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Non-Linear Interpolator - Linear Interpolate
    public NLI_LIPrioMVAUtilCalculator() {
       super(new NonLinearInterpolator(), LINEAR_INTERPOLATE);
    }

}
