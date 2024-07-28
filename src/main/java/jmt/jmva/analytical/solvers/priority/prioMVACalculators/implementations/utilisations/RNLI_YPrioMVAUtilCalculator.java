package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import jmt.jmva.analytical.solvers.priority.interpolators.ReflectedNonLinearInterpolator;

public class RNLI_YPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - Y1
    public RNLI_YPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), RETURN_Y_ONE);
    }
}
