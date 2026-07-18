package jmt.jmva.analytical.solvers.priority.rwaCalculators.implementations;

import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedRWA;
import jmt.jmva.analytical.solvers.priority.PriorityToNonPriorityConvertor;
import jmt.jmva.analytical.solvers.priority.rwaCalculators.ReducedServiceTimeCalculator;
import jmt.jmva.analytical.solvers.priority.rwaCalculators.UtilizationSumCalculator;

import java.util.Map;

public class SCAServiceTimeCalculator implements ReducedServiceTimeCalculator {

    /** Used to allow different variations,
     * SCA is directly summing the utilizations,
     * ESA scales each utilization in the sum */
    private final UtilizationSumCalculator sumCalculator;

    /** [num priority station] [class]
     * All shadow stations of a station are grouped
     * as the original station as each class only accesses one
     * shadow station */
    private double[][] originalServTime;
    /** The results from the last calculation */
    private double[][] lastServTime;

    private int numPriorityStations;
    private int classes;

    /** Higher number = Higher Priority
     * [class] */
    private int[] priorities;

    public SCAServiceTimeCalculator(UtilizationSumCalculator sumCalculator) {
        this.sumCalculator = sumCalculator;
    }

    public SCAServiceTimeCalculator() {
        this(new SCAUtilSumCalculator());
    }

    /**
     * new s_{i,c} = (original s_{i, c}) / (1 - sum utilizations of higher priorities)
     * The sum is calculated via a calculator to allow for different variations such as ESA
     */
    @Override
    public double[][] calculateNewServiceTimes(SolverMultiMixedRWA solver) {
        double[][] utilizations =  calculateNewUtilizations(solver);
        double[][] newServTime = new double[numPriorityStations][classes];
        // Priority -> Station -> Sum
        Map<Integer, double[]> priorityToHigherPriorityUtilSum = sumCalculator.calculateUtilSums(utilizations, solver);
        for (int station = 0; station < numPriorityStations; station++) {
            for (int clas = 0; clas < classes; clas++) {
                double oneMinusHigherUtil = 1.0 - priorityToHigherPriorityUtilSum.get(priorities[clas])[station];
                if (oneMinusHigherUtil <= 0) {
                    solver.forceStop();
                    return null;
                }
                newServTime[station][clas] = originalServTime[station][clas] / oneMinusHigherUtil;
            }
        }
        lastServTime = newServTime;
        return newServTime;
    }

    private double[][] calculateNewUtilizations(SolverMultiMixedRWA solver) {
        double[][] throughputs = solver.getLastConvergenceCheckThroughput();
        double[][] utilizations = new double[numPriorityStations][classes];
        for (int station = 0; station < numPriorityStations; station++) {
            for (int clas = 0; clas < classes; clas++) {
                utilizations[station][clas] = throughputs[station][clas] * lastServTime[station][clas];
            }
        }
        return utilizations;
    }

    @Override
    public void initialise(SolverMultiMixedRWA solver) {
        classes = solver.getClasses();
        numPriorityStations = solver.getNumPriorityStations();
        priorities = solver.getPriorities();
        originalServTime = new double[numPriorityStations][classes];
        double[][][] servTime = solver.getServTime();
        int[] type = solver.getStationTypes();
        int stations = solver.getStations();
        int priorityIndex = 0;
        for (int station = 0; station < stations; station++) {
            if (PriorityToNonPriorityConvertor.stationIsPriority(type[station])) {
                for (int clas = 0; clas < classes; clas++) {
                    originalServTime[priorityIndex][clas] = servTime[station][clas][0];
                }
                priorityIndex++;
            }
        }
        lastServTime = originalServTime;

        sumCalculator.initialise(solver);
    }

}
