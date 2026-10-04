package solvers.priority.prioMVACalculators.implementations.utilisations;

import solvers.utilities.MVAPopulation;
import solvers.priority.prioMVACalculators.BasePrioMVAUtilCalculator;

public class TPrioMVAUtilCalculator extends BasePrioMVAUtilCalculator {

    /**
     * $\rho_{i, cl}(\vn) = \rho_{i, cl}(\vn - \vOne_k)$
     * Named after: Teunissen
     */

    @Override
    public double getNonEmptyClosedUtilisation(MVAPopulation mvaPopulation, int numCustomers, int station, int clas) {

        mvaPopulation.decClassPopulation(clas);
        double result = utilMap.get(mvaPopulation)[station][clas];
        mvaPopulation.incClassPopulation(clas);

        return result;
    }

}
