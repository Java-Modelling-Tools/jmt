package solvers.priority.prioMVACalculators;

import solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import solvers.utilities.MVAPopulation;

/**
 * Queue length calculator of the priority MVA algorithms of JMVA.
 */
/**
 * Queue length calculator of the priority MVA algorithms of JMVA.
 */
/**
 * Queue length calculator of the priority MVA algorithms of JMVA.
 */
/**
 * Queue length calculator of the priority MVA algorithms of JMVA.
 */
public interface QueueLengthCalculator {

    double getQueueLengths(MVAPopulation population, int numCustomers, int station, int clas);

    void initialise(SolverMultiMixedPrioMVA solver);

    void iterateClassUpdate(SolverMultiMixedPrioMVA solver);

    void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver);
}
