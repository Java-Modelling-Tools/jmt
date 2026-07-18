package jmt.jmva.analytical.solvers.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MemoryUsageUtils {

    /**
     * Calculates and returns as a String the actual memory usage inside the JVM.
     * To do so means multiple invocations of the garbage collector.
     *
     * @return A String containing the memory usage
     */
    public static String memoryUsage() {
        long mem;
        // Find memory usage:
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        String res;
        if (mem < 1024) {
            res = mem + " B";
        } else if (mem < 1024 * 1024) {
            res = mem / 1024 + " kB";
        } else if (mem < 1024 * 1024 * 1024) {
            res = mem / 1024 / 1024 + " MB";
        } else {
            res = mem / 1024 / 1024 / 1024 + " GB";
        }
        return res;
    }

    public static void printJVMMemoryStats() {
        int mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        System.out.println(" --- JVM Heap statistics (MB) ---");
        System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);
        System.out.println("Free Memory:" + runtime.freeMemory() / mb);
        System.out.println("Total Memory:" + runtime.totalMemory() / mb);
        System.out.println("Max Memory:" + runtime.maxMemory() / mb);
    }

    public static void appendMemoryUsageToFile(String filename) {
        try {
            File newFile = new File(filename);
            newFile.createNewFile();
            long memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            String content = String.format("%d%n", memory);
            Files.write(Paths.get(filename), content.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
