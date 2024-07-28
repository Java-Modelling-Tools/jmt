package jmt.jmva.analytical.priomvaCorrectness;

import jmt.jmva.analytical.BondiCorrectnessTest;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import org.junit.Before;
import org.junit.Test;

public class PrioMVACorrectnessBondiModelThreeNTwoThreeTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    private SolverMultiMixedPrioMVA prioSolver;

    @Before
    public void initSolver() {
        prioSolver = BondiCorrectnessTest.getModelThreeNTwoThree().getPrioMVASolver();
    }

    @Test
    public void queueLengthTest() {
        // Page 11, Table 9
        double queueLenOneOne = 0.8684;
        BondiCorrectnessTest.queueLengthTest(prioSolver, queueLenOneOne);
    }

    @Test
    public void residenceTimeTest() {
        // Page 9, Table 7
        double resTimeTwoTwo = 0.2244;
        BondiCorrectnessTest.residenceTimeTest(prioSolver, resTimeTwoTwo);
    }

    @Test
    public void throughputTest() {
        // Page 8, Table 6
        double[] correctThroughput = {3.2225, 4.4555};
        BondiCorrectnessTest.throughputTest(prioSolver, correctThroughput);
    }
}
