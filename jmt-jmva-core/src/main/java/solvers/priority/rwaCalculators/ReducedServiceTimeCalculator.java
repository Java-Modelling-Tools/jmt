package solvers.priority.rwaCalculators;

import solvers.multiSolverAlgorithms.SolverMultiMixedRWA;

public interface ReducedServiceTimeCalculator {

    /**
     * Calculates the new service times during RWA
     * @param solver Used to get the data from
     * @return [num Priority stations][classes]
     * All of the shadow stations are stored as one station
     * as each class is only served at one shadow station
     */
    double[][] calculateNewServiceTimes(SolverMultiMixedRWA solver);

    void initialise(SolverMultiMixedRWA solver);
}
