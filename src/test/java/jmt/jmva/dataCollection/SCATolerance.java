package jmt.jmva.dataCollection;

import java.util.HashMap;
import java.util.Map;

import static jmt.jmva.dataCollection.DataCollectUtils.*;

public class SCATolerance {

    private static final int NUM_REPEATS = 1;
    private static boolean VERBOSE = false;

    private static final int MIN_TOLERANCE = -12;
    private static final int MAX_TOLERANCE = 1;

    private static final String BASE_MODEL_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/SCATolModels/BaseModelT_.xml";
    private static final String RESULTS_FN = String.format("/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/SCATolModels/Results/Results_%d_%d_%d.csv",
            MAX_TOLERANCE, MIN_TOLERANCE, NUM_REPEATS);

    public static void main(String[] args) {
        getSCATolerance();
    }

    private static void getSCATolerance() {
        Map<String, Long> toleranceToTime = new HashMap<>();
        String fileContent = getFileContent(BASE_MODEL_FN);

        for (int t = MAX_TOLERANCE; t >= MIN_TOLERANCE; t--) {
            String strT = String.valueOf(t);
            String filename = changeToleranceInFile(BASE_MODEL_FN, fileContent, Math.pow(10, t));
            long elapsed = solveNTimes(filename, strT, NUM_REPEATS, VERBOSE);
            toleranceToTime.put(strT, elapsed);
        }
        writeResultsToFile(toleranceToTime, RESULTS_FN);
    }

}
