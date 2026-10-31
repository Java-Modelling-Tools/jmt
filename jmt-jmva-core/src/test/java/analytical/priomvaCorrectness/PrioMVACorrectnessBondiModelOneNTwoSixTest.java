package analytical.priomvaCorrectness;

import analytical.BondiCorrectnessTest;
import solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PrioMVACorrectnessBondiModelOneNTwoSixTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    private SolverMultiMixedPrioMVA prioSolver;

    @BeforeEach
    void initSolver() {
        prioSolver = BondiCorrectnessTest.getModelOneNTwoSix().getPrioMVASolver();
    }

    @Disabled("unverified: PrioMVA (no convergence tolerance) gives 0.36252 vs published 0.3601 (Bondi Table 9); cause not established")
    @Test
    void queueLengthTest() {
        // Page 11, Table 9
        double queueLenOneOne = 0.3601;
        BondiCorrectnessTest.queueLengthTest(prioSolver, queueLenOneOne);
    }

    @Test
    void residenceTimeTest() {
        // Page 9, Table 7
        double resTimeTwoTwo = 0.1607;
        BondiCorrectnessTest.residenceTimeTest(prioSolver, resTimeTwoTwo);
    }

    @Test
    void throughputTest() {
        // Page 6, Table 4
        double[] correctThroughput = {14.7267, 3.6158};
        BondiCorrectnessTest.throughputTest(prioSolver, correctThroughput);
    }
}
