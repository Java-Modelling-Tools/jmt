package jmt.jmva.analytical.solvers.priority.prioMVACalculators;

import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;

/**
 * Used to get approximate values for a utilization in PrioMVA
 */
public interface UtilizationCalculator {

    double getClosedUtilisation(MVAPopulation population, int numCustomers, int station, int clas);

    double getOpenUtilisation(MVAPopulation population, int numCustomers, int station, int clas);

    void initialise(SolverMultiMixedPrioMVA solver);

    void iterateClassUpdate(SolverMultiMixedPrioMVA solver);

    void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver);
}
