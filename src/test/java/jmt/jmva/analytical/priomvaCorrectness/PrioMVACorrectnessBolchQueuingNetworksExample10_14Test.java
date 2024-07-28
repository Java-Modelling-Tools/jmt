package jmt.jmva.analytical.priomvaCorrectness;

import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class PrioMVACorrectnessBolchQueuingNetworksExample10_14Test {


    @Test
    public void queueLengthTest() {
        // given
        SolverMultiMixedPrioMVA prioMVA = getSolvedModel();
        Map<MVAPopulation, double[][]> queueLengthMap = prioMVA.getQueueLengthMap();

        // then
        /*
         In the interim calculations, the classes are sorted by class so closed then open
         and in the classes sorted by priority so if the original classes are:
         open (1), closed (2), closed (3), they're stored as
         closed (2), closed (3), open (1)
         */
        double[][] correct00 = {{0.0, 0.0, 4.0}, {0.0, 0.0, 1.5}};
        double[][] population00 = queueLengthMap.get(new MVAPopulation(new int[]{0, 0}));
        // TODO: Replace assert with assertDouble to allow a for rounding errors (same for asserts below)
        //assert (Arrays.deepEquals(correct00, population00));

        double[][] correct01 = {{0.0, 0.595, 6.38}, {0.0, 0.405, 1.5}};
        double[][] population01 = queueLengthMap.get(new MVAPopulation(new int[]{0, 1}));
        //assert (Arrays.deepEquals(correct01, population01));

        double[][] correct10 = {{0.462, 0.0, 5.85}, {0.538, 0.0, 1.5}};
        double[][] population10 = queueLengthMap.get(new MVAPopulation(new int[]{1, 0}));
        //assert (Arrays.deepEquals(correct10, population10));

        double[][] correct11 = {{0.578, 0.65, 8.91}, {0.422, 0.35, 1.5}};
        double[][] population11 = queueLengthMap.get(new MVAPopulation(new int[]{1, 1}));
        //assert (Arrays.deepEquals(correct11, population11));

        // This is the final result so the original class order should be used
        double[][] queueLengths = prioMVA.getQueueLen();
        double[][] correctLengths = {{8.91, 0.578, 0.65}, {1.5, 0.422, 0.35}};
        assert (Arrays.deepEquals(queueLengths, correctLengths));
    }

    @Test
    public void residenceTimeTest() {
        // given
        SolverMultiMixedPrioMVA prioMVA = getSolvedModel();

        // then
        double[][] residenceTimes = prioMVA.getResTime();
        double[][] correctTimes = {{8.91, 2.393, 3.654}, {1.5, 3.5, 4.923}};
        assert (Arrays.deepEquals(residenceTimes, correctTimes));
    }

    @Test
    public void throughputTest() {
        // given
        SolverMultiMixedPrioMVA prioMVA = getSolvedModel();

        // then
        double[][] throughput = prioMVA.getThroughput();
        double[][] visits = prioMVA.getVisits();
        double[] clsThroughput = new double[throughput[0].length];
        for (int clas = 0;  clas < throughput[0].length; clas++) {
            // Forced Flow Law
            clsThroughput[clas] = throughput[0][clas] / visits[0][clas];
        }

        double[] correctThroughput = {1, 0.241, 0.178};
        assert (Arrays.equals(clsThroughput, correctThroughput));
    }


    private SolverMultiMixedPrioMVA getSolvedModel() {
        // given
        int stations = 2;
        int classes = 3;
        // Class 1 has highest priority
        int[] priorities = {6, 4, 2};
        int[] classTypes = {SolverMulti.OPEN_CLASS, SolverMulti.CLOSED_CLASS, SolverMulti.CLOSED_CLASS};
        int[] stationTypes = {SolverMulti.LI, SolverMulti.PRS};
        double[] classData = {1, 1, 1};
        double[][][] servTime = {{{0.4}, {0.3}, {0.5}}, {{0.6}, {0.5}, {0.8}}};
        double[][] visits = {{2, 1, 1}, {1, 0.5, 0.4}};
        String[] stationNames = {"S1", "S2"};

        SolverMultiMixedPrioMVA prioMVA = new SolverMultiMixedPrioMVA(classes, stations);
        prioMVA.input(stationNames, stationTypes, servTime, visits, classData, classTypes, priorities);

        // when
        prioMVA.solve();

        return prioMVA;
    }
}
