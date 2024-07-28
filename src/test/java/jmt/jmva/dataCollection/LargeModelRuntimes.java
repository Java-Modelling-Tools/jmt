package jmt.jmva.dataCollection;

import jmt.jmva.analytical.solvers.SolverAlgorithm;
import jmt.jmva.gui.utilities.SolverAlgorithmCounter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static jmt.jmva.dataCollection.DataCollectUtils.*;

public class LargeModelRuntimes {
    private static final int NUM_REPEATS = 500;
    private static boolean VERBOSE = true;

    private static final String BASE_LARGE_MODEL_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/LargeModels/LargeModelCL.xml";
    private static final String BASE_NON_PRIORITY_LARGE_MODEL_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/LargeModels/LargeModelNonPriority.xml";
    private static final String RESULTS_LARGE_MODEL_FN = String.format("/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/LargeModels/Results/Results_%d.csv", NUM_REPEATS);

    public static void main(String[] args) {
        getLargeModelRuntimes();
    }

    public static void getLargeModelRuntimes() {
        try {
            Map<String, Long> results = new HashMap<>();
            String fileContent = getFileContent(BASE_LARGE_MODEL_FN);
            for (SolverAlgorithm alg : SolverAlgorithmCounter.getPriorityAlgorithms(
                    Arrays.asList(SolverAlgorithm.closedValues()), false)) {
                String fileName = changeAlgorithmInFile(BASE_LARGE_MODEL_FN, alg, fileContent);
                long elapsed = solveNTimes(fileName, alg.toString(), NUM_REPEATS, VERBOSE);
                results.put(alg.toString(), elapsed);
            }

            SolverAlgorithm alg = SolverAlgorithm.EXACT;
            long elapsed = solveNTimes(BASE_NON_PRIORITY_LARGE_MODEL_FN, alg.toString(), NUM_REPEATS, VERBOSE);
            results.put(alg.toString(), elapsed);
            writeResultsToFile(results, RESULTS_LARGE_MODEL_FN);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }


}
