package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import jmt.jmva.analytical.solvers.priority.interpolators.LinearInterpolator;

public class CLPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    /**
     * $\rho_{i, cl}(\vn) = \rho_{i, cl}(\vn - \vQ_{i,cl})$
     * where $\vQ_{i,cl} = \vOne_{cl} \cdot \oQ_{i, cl}(\vn)$
     * Named after: Chandy and Lakshmi
     */

    public CLPrioMVAUtilCalculator() {
        super(new LinearInterpolator(), NO_CHANGE);
    }
}
