package jmt.jmva.analytical.solvers.priority.prioMVACalculators;

import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;

public interface QueueLengthCalculator {

    double getQueueLengths(MVAPopulation population, int numCustomers, int station, int clas);

    void initialise(SolverMultiMixedPrioMVA solver);

    void iterateClassUpdate(SolverMultiMixedPrioMVA solver);

    void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver);
}
