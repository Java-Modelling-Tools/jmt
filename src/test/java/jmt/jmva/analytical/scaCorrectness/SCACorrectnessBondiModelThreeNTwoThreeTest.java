package jmt.jmva.analytical.scaCorrectness;

import jmt.jmva.analytical.BondiCorrectnessTest;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedRWA;
import org.junit.Before;
import org.junit.Test;

public class SCACorrectnessBondiModelThreeNTwoThreeTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    // given
    private SolverMultiMixedRWA rwaSolver;

    @Before
    public void initSolver() {
        rwaSolver = BondiCorrectnessTest.getModelThreeNTwoThree().getRWASolver();
    }

    @Test
    public void queueLengthTest() {
        // Page 11, Table 9
        double queueLenOneOne = 0.8060;
        BondiCorrectnessTest.queueLengthTest(rwaSolver, queueLenOneOne);
    }

    @Test
    public void residenceTimeTest() {
        // Page 9, Table 7
        double resTimeTwoTwo = 0.3661;
        BondiCorrectnessTest.residenceTimeTest(rwaSolver, resTimeTwoTwo);
    }

    @Test
    public void throughputTest() {
        // Page 8, Table 6
        double[] correctThroughput = {2.9292, 6.9352};
        BondiCorrectnessTest.throughputTest(rwaSolver, correctThroughput);
    }
}
