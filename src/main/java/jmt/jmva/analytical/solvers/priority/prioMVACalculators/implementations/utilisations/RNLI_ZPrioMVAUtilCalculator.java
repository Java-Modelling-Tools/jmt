<<<<<<< HEAD
package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.interpolators.ReflectedNonLinearInterpolator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class RNLI_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - Zero
    public RNLI_ZPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), RETURN_ZERO);
    }
}
=======
package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.interpolators.ReflectedNonLinearInterpolator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;

public class RNLI_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Reflected Non-Linear Interpolator - Zero
    public RNLI_ZPrioMVAUtilCalculator() {
       super(new ReflectedNonLinearInterpolator(), RETURN_ZERO);
    }
}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
