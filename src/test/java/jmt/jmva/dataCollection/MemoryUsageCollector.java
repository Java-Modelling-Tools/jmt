package jmt.jmva.dataCollection;

public class MemoryUsageCollector {


    public static final int NUM_REPEATS = 2000;
    private static boolean VERBOSE = true;

    private static final String CL_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/MemoryUsage/PriorityModelCL.xml";
    private static final String SCA_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/MemoryUsage/PriorityModelSCA.xml";
    private static final String MVA_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/MemoryUsage/PriorityModelMVA.xml";
    private static final String MIXED_FN = "/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/MemoryUsage/PriorityModelMVA.xml";

    public static void main(String[] args) {
        //solveNTimes(CL_FN, "CL", NUM_REPEATS, VERBOSE);
        //solveNTimes(SCA_FN, "SCA", NUM_REPEATS, VERBOSE);
        //solveNTimes(MVA_FN, "MVA", NUM_REPEATS, VERBOSE);
        //solveNTimes(MIXED_FN, "Mixed", NUM_REPEATS, VERBOSE);
    }

    // MVA
    //int pop = population[0];
    //if (population[0] == 5) {
    //	pop = population[1];
    //}
    //String filename = String.format("/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/MemoryUsage/MVA/MemUsageMVA_%d_%d.csv",
    //		10000, pop);
    //MemoryUsageUtils.appendMemoryUsageToFile(filename);

    // CL
    //String filename = String.format("/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/MemoryUsage/CL/MemUsageCL_%d_%d.csv",
    //        10000, clPopulation[0]);
    //    MemoryUsageUtils.appendMemoryUsageToFile(filename);

    // SCA
    //String filename = String.format("/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/MemoryUsage/SCA/MemUsageSCA_%d_%d.csv",
    //        10000, (int) classData[1]);
    //    MemoryUsageUtils.appendMemoryUsageToFile(filename);
    //System.out.printf("%e,%d%n", tolerance, iterations);

    //Mixed
    //String filename = String.format("/home/ben_pahnke/IndProj/individualproject/WriteUp/Code/MemoryUsage/Mixed/MemUsageMixed_%d_%d.csv",
    //        10000, (int) popPar[1]);
	//	MemoryUsageUtils.appendMemoryUsageToFile(filename);
}
