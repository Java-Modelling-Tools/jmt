package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.priority.interpolators.NonLinearInterpolator;
import solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class NLI_YPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Non-Linear Interpolator - Y1
    public NLI_YPrioMVAUtilCalculator() {
       super(new NonLinearInterpolator(), RETURN_Y_ONE);
    }

}
