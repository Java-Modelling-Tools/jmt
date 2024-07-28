package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.interpolators.LinearInterpolator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class CL_YPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Named after: Chandy and Lakshmi
    // C & L had nothing to do with this variation though
    // When x1 = 0, it doesn't interpolate, it just returns y1

    public CL_YPrioMVAUtilCalculator() {
        super(new LinearInterpolator(), RETURN_Y_ONE);
    }
}
