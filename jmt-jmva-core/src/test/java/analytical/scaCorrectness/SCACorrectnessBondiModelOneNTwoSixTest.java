package analytical.scaCorrectness;

import analytical.BondiCorrectnessTest;
import solvers.multiSolverAlgorithms.SolverMultiMixedRWA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SCACorrectnessBondiModelOneNTwoSixTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    private SolverMultiMixedRWA rwaSolver;

    @BeforeEach
    void initSolver() {
        // given
        rwaSolver = BondiCorrectnessTest.getModelOneNTwoSix().getRWASolver();
    }

    @Test
    void queueLengthTest() {
        // Page 11, Table 9
        double queueLenOneOne = 0.3814;
        BondiCorrectnessTest.queueLengthTest(rwaSolver, queueLenOneOne, BondiCorrectnessTest.TOLERANCE);
    }

    @Test
    void residenceTimeTest() {
        // Page 9, Table 7
        double resTimeTwoTwo = 0.1583;
        BondiCorrectnessTest.residenceTimeTest(rwaSolver, resTimeTwoTwo, BondiCorrectnessTest.TOLERANCE);
    }

    @Test
    void throughputTest() {
        // Page 6, Table 4
        double[] correctThroughput = {15.2737, 3.4716};
        BondiCorrectnessTest.throughputTest(rwaSolver, correctThroughput, BondiCorrectnessTest.TOLERANCE);
    }
}
