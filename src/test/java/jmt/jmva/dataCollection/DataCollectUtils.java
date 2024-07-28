package jmt.jmva.dataCollection;

import jmt.jmva.analytical.CommandLineSolver;
import jmt.jmva.analytical.solvers.SolverAlgorithm;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DataCollectUtils {

    public static String changeAlgorithmInFile(String ogFilename, SolverAlgorithm alg, String ogFileContent) {
        try {
            String fileName = ogFilename.replaceAll("SCA", alg.toString().replaceAll(" ", ""));
            File newFile = new File(fileName);
            newFile.createNewFile();
            String newContent = ogFileContent.replaceAll(SolverAlgorithm.SCA.toString(), alg.toString());
            FileUtils.writeStringToFile(newFile, newContent);
            return fileName;
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return "";
    }

    public static String changeToleranceInFile(String ogFilename, String ogFileContent, double tolerance) {
        try {
            String fileName = ogFilename.replaceAll("T_", String.format("T_%e", tolerance));
            File newFile = new File(fileName);
            newFile.createNewFile();
            String newContent = ogFileContent.replaceAll("tolerance=\"0.1\"", String.format("tolerance=\"%e\"", tolerance));
            FileUtils.writeStringToFile(newFile, newContent);
            return fileName;
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return "";
    }

    public static long solveNTimes(String filename, String algName, int numRepeats, boolean verbose) {
        CommandLineSolver solver = new CommandLineSolver();
        long start = System.nanoTime();
        for (int i = 0; i < numRepeats; i++) {
            System.gc();
            solver.solve(filename);
        }
        long elapsed = System.nanoTime() - start;
        long microSeconds = TimeUnit.MICROSECONDS.convert(elapsed, TimeUnit.NANOSECONDS);
        if (verbose) {
            System.out.println();
            System.out.println(algName);
            System.out.println(microSeconds);
        }
        return microSeconds;
    }

    public static String getFileContent(String filename) {
        try {
            File clFile = new File(filename);
            return new String(Files.readAllBytes(clFile.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return "";
    }

    public static void writeResultsToFile(Map<String, Long> results, String filename) {
        try {
            File resultsFile = new File(filename);
            resultsFile.createNewFile();
            StringBuilder builder = new StringBuilder();
            for (String alg : results.keySet()) {
                builder.append(String.format("%s,%d\n", alg, results.get(alg)));
            }
            FileUtils.writeStringToFile(resultsFile, builder.toString());
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }

}
