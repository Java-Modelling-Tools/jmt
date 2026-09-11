package solvers.priority.prioMVACalculators;

import solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import solvers.utilities.MVAPopulation;

public interface QueueLengthCalculator {

    double getQueueLengths(MVAPopulation population, int numCustomers, int station, int clas);

    void initialise(SolverMultiMixedPrioMVA solver);

    void iterateClassUpdate(SolverMultiMixedPrioMVA solver);

    void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver);
}
