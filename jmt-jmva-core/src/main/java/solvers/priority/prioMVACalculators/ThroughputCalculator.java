package solvers.priority.prioMVACalculators;

import solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import solvers.utilities.MVAPopulation;

/**
 * Throughput calculator of the priority MVA algorithms of JMVA.
 */
/**
 * Throughput calculator of the priority MVA algorithms of JMVA.
 */
/**
 * Throughput calculator of the priority MVA algorithms of JMVA.
 */
/**
 * Throughput calculator of the priority MVA algorithms of JMVA.
 */
public interface ThroughputCalculator {
    double getClassThroughput(MVAPopulation population, int numCustomers, int clas);

    void initialise(SolverMultiMixedPrioMVA solver);

    void iterateClassUpdate(SolverMultiMixedPrioMVA solver);

    void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver);
}
