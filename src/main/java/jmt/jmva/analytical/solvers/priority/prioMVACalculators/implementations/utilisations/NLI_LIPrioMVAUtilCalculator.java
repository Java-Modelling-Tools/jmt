package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import jmt.jmva.analytical.solvers.priority.interpolators.NonLinearInterpolator;

public class NLI_LIPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Non-Linear Interpolator - Linear Interpolate
    public NLI_LIPrioMVAUtilCalculator() {
       super(new NonLinearInterpolator(), LINEAR_INTERPOLATE);
    }

}
