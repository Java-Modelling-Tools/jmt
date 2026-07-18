package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.interpolators.ReflectedNonLinearInterpolator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class RNLI_NCPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - No Change
    public RNLI_NCPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), NO_CHANGE);
    }
}
