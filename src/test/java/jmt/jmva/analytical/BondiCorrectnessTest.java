package jmt.jmva.analytical;

import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedRWA;

public class BondiCorrectnessTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    // Tolerance for SCA
    public static final double TOLERANCE = 0.005;
    // The number of decimal points the results are given to
    public static final double PRECISION_OF_RESULTS = 0.0001;

    public static void queueLengthTest(SolverMulti solver, double queueLenOneOne) {
        // then
        double[][] queueLengths = solver.getQueueLen();

        // Page 11, Table 9
        assert (Math.abs(queueLenOneOne - queueLengths[0][0]) < PRECISION_OF_RESULTS);
    }

    public static void residenceTimeTest(SolverMulti solver, double resTimeTwoTwo) {
        // then
        double[][] residenceTimes = solver.getResTime();
        // Page 9, Table 7
        assert (Math.abs(resTimeTwoTwo - residenceTimes[1][1]) < PRECISION_OF_RESULTS);
    }

    public static void throughputTest(SolverMulti solver, double[] correctThroughput) {
        // then
        double[][] throughput = solver.getThroughput();
        double[][] visits = solver.getVisits();
        double[] clsThroughput = new double[throughput[0].length];
        for (int clas = 0;  clas < throughput[0].length; clas++) {
            // Forced Flow Law
            clsThroughput[clas] = throughput[0][clas] / visits[0][clas];
        }

        // Page 8, Table 6
        for (int clas = 0;  clas < throughput[0].length; clas++) {
            assert (Math.abs(clsThroughput[clas] - correctThroughput[clas]) < PRECISION_OF_RESULTS);
        }
    }

    public static BondiSolverGenerator getModelOneNTwoSix() {
        return new BondiSolverGenerator(6,
                new double[][][] {{{0.02}, {0.20}}, {{0.05}, {0.05}}});
    }

    public static BondiSolverGenerator getModelTwoNTwoOne() {
        return new BondiSolverGenerator(1,
                new double[][][] {{{0.10}, {0.10}}, {{0.10}, {0.10}}});
    }

    public static BondiSolverGenerator getModelThreeNTwoThree() {
        return new BondiSolverGenerator(3,
                // [station][class]
                new double[][][] {{{0.20}, {0.02}}, {{0.10}, {0.10}}});
    }

    public static class BondiSolverGenerator {
        private final int stations = 2;
        private final int classes = 2;
        // Class 1 has the highest priority
        private final int[] priorities = {5, 4};
        private final int[] classTypes = {SolverMulti.CLOSED_CLASS, SolverMulti.CLOSED_CLASS};
        private final int[] stationTypes = {SolverMulti.PRS, SolverMulti.LI};
        private final double[][] visits = {{1, 1}, {1, 1}};
        private final String[] stationNames = {"S1", "S2"};

        private final double[] classData;
        private final double[][][] servTime;

        private BondiSolverGenerator(double classTwoPop, double[][][] servTime) {
            this.classData = new double[classes];
            classData[0] = 2;
            classData[1] = classTwoPop;
            this.servTime = servTime;
        }

        // CL variation
        public SolverMultiMixedPrioMVA getPrioMVASolver() {
            SolverMultiMixedPrioMVA prioMVA = new SolverMultiMixedPrioMVA(classes, stations);
            prioMVA.input(stationNames, stationTypes, servTime, visits, classData, classTypes, priorities);
            prioMVA.solve();

            return prioMVA;
        }

        // SCA
        public SolverMultiMixedRWA getRWASolver() {
            SolverMultiMixedRWA rwaSolver = new SolverMultiMixedRWA(classes, stations);
            rwaSolver.input(stationNames, stationTypes, servTime, visits, classData, classTypes, priorities,
                    TOLERANCE);
            rwaSolver.solve();

            return rwaSolver;
        }
    }
}
