package jmt.jmva.analytical.solvers.priority.rwaCalculators;

import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedRWA;

import java.util.Map;

public interface UtilizationSumCalculator {

    /**
     * @param utilizations [num priority stations][class] utilizations of shadow servers
     * @param solver from which the utilizations are from
     * @return Map from Priority to double[num priority stations] of utilization sum
     */
    Map<Integer, double[]> calculateUtilSums(double[][] utilizations, SolverMultiMixedRWA solver);

    void initialise(SolverMultiMixedRWA solver);
}
