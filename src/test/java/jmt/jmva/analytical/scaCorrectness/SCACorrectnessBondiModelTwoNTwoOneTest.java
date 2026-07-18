package jmt.jmva.analytical.scaCorrectness;

import jmt.jmva.analytical.BondiCorrectnessTest;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedRWA;
import org.junit.Before;
import org.junit.Test;

public class SCACorrectnessBondiModelTwoNTwoOneTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    private SolverMultiMixedRWA rwaSolver;

    @Before
    public void initSolver() {
        // given
        rwaSolver = BondiCorrectnessTest.getModelTwoNTwoOne().getRWASolver();
    }

    @Test
    public void queueLengthTest() {
        // Page 11, Table 9
        double queueLenOneOne = 0.8333;
        BondiCorrectnessTest.queueLengthTest(rwaSolver, queueLenOneOne);
    }

    @Test
    public void residenceTimeTest() {
        // Page 9, Table 7
        double resTimeTwoTwo = 0.2000;
        BondiCorrectnessTest.residenceTimeTest(rwaSolver, resTimeTwoTwo);
    }

    @Test
    public void throughputTest() {
        // Page 7, Table 5
        double[] correctThroughput = {5.9175, 2.2475};
        BondiCorrectnessTest.throughputTest(rwaSolver, correctThroughput);
    }
}
