package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.interpolators.NonLinearInterpolator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class NLI_YPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Non-Linear Interpolator - Y1
    public NLI_YPrioMVAUtilCalculator() {
       super(new NonLinearInterpolator(), RETURN_Y_ONE);
    }

}
