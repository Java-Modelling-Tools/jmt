package analytical.priomvaCorrectness;

import analytical.BondiCorrectnessTest;
import solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrioMVACorrectnessBondiModelTwoNTwoOneTest {

    /**
     * Results are from the paper:
     * André B Bondi and Yie-Min Chuang. A new mva-based approximation for closed
     * queueing networks with a preemptive priority server. Performance Evaluation,
     * 8(3):195–221, 1988.
     */

    private SolverMultiMixedPrioMVA prioSolver;

    @BeforeEach
    void initSolver() {
        prioSolver = BondiCorrectnessTest.getModelTwoNTwoOne().getPrioMVASolver();
    }

    @Test
    void queueLengthTest() {
        // Page 11, Table 9
        double queueLenOneOne = 0.8159;
        BondiCorrectnessTest.queueLengthTest(prioSolver, queueLenOneOne);
    }

    @Test
    void residenceTimeTest() {
        // Page 9, Table 7
        double resTimeTwoTwo = 0.2000;
        BondiCorrectnessTest.residenceTimeTest(prioSolver, resTimeTwoTwo);
    }

    @Test
    void throughputTest() {
        // Page 7, Table 5
        double[] correctThroughput = {5.8278, 1.8079};
        BondiCorrectnessTest.throughputTest(prioSolver, correctThroughput);
    }
}
