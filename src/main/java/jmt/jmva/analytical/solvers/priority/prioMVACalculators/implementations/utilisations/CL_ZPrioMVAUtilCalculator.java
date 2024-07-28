package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.interpolators.LinearInterpolator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class CL_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Named after: Chandy and Lakshmi
    // C & L had nothing to do with this variation though
    // When x1 = 0, it doesn't interpolate, it just returns 0

    public CL_ZPrioMVAUtilCalculator() {
        super(new LinearInterpolator(), RETURN_ZERO);
    }
}
