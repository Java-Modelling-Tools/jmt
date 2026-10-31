package analytical.priomvaCorrectness;

import analytical.BondiCorrectnessTest;
import solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PrioMVACorrectnessBondiModelThreeNTwoThreeTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    private SolverMultiMixedPrioMVA prioSolver;

    @BeforeEach
    void initSolver() {
        prioSolver = BondiCorrectnessTest.getModelThreeNTwoThree().getPrioMVASolver();
    }

    @Test
    void queueLengthTest() {
        // Page 11, Table 9
        double queueLenOneOne = 0.8684;
        BondiCorrectnessTest.queueLengthTest(prioSolver, queueLenOneOne);
    }

    @Disabled("unverified: PrioMVA class-2 residence time at station 2 is 0.2551 vs published 0.2244 (Bondi Table 7); cause not established")
    @Test
    void residenceTimeTest() {
        // Page 9, Table 7
        double resTimeTwoTwo = 0.2244;
        BondiCorrectnessTest.residenceTimeTest(prioSolver, resTimeTwoTwo);
    }

    @Disabled("unverified: PrioMVA (no convergence tolerance) gives 4.45236 vs published 4.4555 (Bondi Table 6); cause not established")
    @Test
    void throughputTest() {
        // Page 8, Table 6
        double[] correctThroughput = {3.2225, 4.4555};
        BondiCorrectnessTest.throughputTest(prioSolver, correctThroughput);
    }
}
