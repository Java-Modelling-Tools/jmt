package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations;

import jmt.jmva.analytical.solvers.priority.prioMVACalculators.BasePrioMVAUtilCalculator;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;

public class BKTPrioMVAUtilCalculator extends BasePrioMVAUtilCalculator {

    /**
     * $\rho_{i, cl}(\vn) = \rho_{i, cl}(\vn)$
     * Named after: Bryant, Krzesinski, and Teunissen
     */

    @Override
    public double getNonEmptyClosedUtilisation(MVAPopulation mvaPopulation, int numCustomers, int station, int clas) {
        if (utilMap.containsKey(mvaPopulation)) {
            return utilMap.get(mvaPopulation)[station][clas];
        }

        // Used in the HOL case
        mvaPopulation.decClassPopulation(clas);
        double result = utilMap.get(mvaPopulation)[station][clas];
        mvaPopulation.incClassPopulation(clas);

        return result;
    }

}
