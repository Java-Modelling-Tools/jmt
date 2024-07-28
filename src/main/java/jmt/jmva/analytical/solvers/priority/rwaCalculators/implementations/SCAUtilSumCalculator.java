package jmt.jmva.analytical.solvers.priority.rwaCalculators.implementations;

import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedRWA;
import jmt.jmva.analytical.solvers.priority.rwaCalculators.UtilizationSumCalculator;

import java.util.HashMap;
import java.util.Map;

public class SCAUtilSumCalculator implements UtilizationSumCalculator {

    private int classes;
    private int numPriorityStations;
    private int[] priorities;

    /**
     * @param utilizations [station][class] utilizations of shadow servers
     * @param solver from which the utilizations are from
     * @return The sum of higher priority utilizations, not scaled
     * Map<Priority, double[stations]>
     */
    @Override
    public Map<Integer, double[]> calculateUtilSums(double[][] utilizations, SolverMultiMixedRWA solver) {
        Map<Integer, double[]> priorityToHigherPriorityUtilSum = new HashMap<>();
        for (int clas = 0; clas < classes; clas++) {
            if (priorityToHigherPriorityUtilSum.containsKey(priorities[clas])) {
                continue;
            }
            double[] stationToUtilSum = new double[numPriorityStations];
            for (int station = 0; station < numPriorityStations; station++) {
                double utilSum = 0;
                for (int sumClas = 0; sumClas < classes; sumClas++) {
                    if (priorities[clas] < priorities[sumClas]) {
                        utilSum += utilizations[station][sumClas];
                    }
                }
                stationToUtilSum[station] = utilSum;
            }
            priorityToHigherPriorityUtilSum.put(priorities[clas], stationToUtilSum);
        }
        return priorityToHigherPriorityUtilSum;
    }

    @Override
    public void initialise(SolverMultiMixedRWA solver) {
        this.classes = solver.getClasses();
        this.numPriorityStations = solver.getNumPriorityStations();
        this.priorities = solver.getPriorities();
    }

}
