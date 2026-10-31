package analytical.scaCorrectness;

import analytical.BondiCorrectnessTest;
import solvers.multiSolverAlgorithms.SolverMultiMixedRWA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SCACorrectnessBondiModelThreeNTwoThreeTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    // given
    private SolverMultiMixedRWA rwaSolver;

    @BeforeEach
    void initSolver() {
        rwaSolver = BondiCorrectnessTest.getModelThreeNTwoThree().getRWASolver();
    }

    @Test
    void queueLengthTest() {
        // Page 11, Table 9
        double queueLenOneOne = 0.8060;
        BondiCorrectnessTest.queueLengthTest(rwaSolver, queueLenOneOne, BondiCorrectnessTest.TOLERANCE);
    }

    @Test
    void residenceTimeTest() {
        // Page 9, Table 7
        double resTimeTwoTwo = 0.3661;
        BondiCorrectnessTest.residenceTimeTest(rwaSolver, resTimeTwoTwo, BondiCorrectnessTest.TOLERANCE);
    }

    @Test
    void throughputTest() {
        // Page 8, Table 6
        double[] correctThroughput = {2.9292, 6.9352};
        BondiCorrectnessTest.throughputTest(rwaSolver, correctThroughput, BondiCorrectnessTest.TOLERANCE);
    }
}
