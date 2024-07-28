<<<<<<< HEAD
package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import jmt.jmva.analytical.solvers.priority.interpolators.NonLinearInterpolator;

public class NLI_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Non-Linear Interpolator - Zero
    public NLI_ZPrioMVAUtilCalculator() {
       super(new NonLinearInterpolator(), RETURN_ZERO);
    }

}
=======
package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.prioMVACalculators.InterpolatedPrioMVAUtilCalculator;
import jmt.jmva.analytical.solvers.priority.interpolators.NonLinearInterpolator;

public class NLI_ZPrioMVAUtilCalculator extends InterpolatedPrioMVAUtilCalculator {

    // Non-Linear Interpolator - Zero
    public NLI_ZPrioMVAUtilCalculator() {
       super(new NonLinearInterpolator(), RETURN_ZERO);
    }

}
>>>>>>> b0a37c45 (Initial commit at SVN r1153.)
