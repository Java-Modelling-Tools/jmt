package jmt.jmva.analytical.solvers.priority.rwaCalculators.implementations;

import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedRWA;
import jmt.jmva.analytical.solvers.priority.rwaCalculators.UtilizationSumCalculator;

import java.util.Map;

public class ESAUtilSumCalculator implements UtilizationSumCalculator {

    private final SCAUtilSumCalculator scaCalculator;

    public ESAUtilSumCalculator(SCAUtilSumCalculator scaCalculator) {
        this.scaCalculator = scaCalculator;
    }

    @Override
    public Map<Integer, double[]> calculateUtilSums(double[][] utilizations, SolverMultiMixedRWA solver) {
        Map<Integer, double[]> scaSums = scaCalculator.calculateUtilSums(utilizations, solver);
        // TODO calculate scalar factor for the sums
        // result_i,j = sum_{s=1}^{(priority of j) -1} (1/scalar factor_i,s) * utilization_i,s
        return null;
    }

    @Override
    public void initialise(SolverMultiMixedRWA solver) {
        scaCalculator.initialise(solver);
    }
}
