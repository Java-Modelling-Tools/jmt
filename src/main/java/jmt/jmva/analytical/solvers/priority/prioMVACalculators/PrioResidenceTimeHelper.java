package jmt.jmva.analytical.solvers.priority.prioMVACalculators;

import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;

import java.util.Map;


public class PrioResidenceTimeHelper {

    private static final boolean HIGHER_PRIORITY_ONlY = false;
    private static final boolean HIGHER_OR_EQUAL_PRIORITY = true;

    private final int numOpenClasses;
    private final int numClosedClasses;

    private final int[] opPriorities;
    private final int[] clPriorities;

    private final double[][][] opServTime;
    private final double[][][] clServTime;

    private final double[][] opUtilization;

    private final Map<MVAPopulation, double[][]> queueLengthMap;
    private final Map<MVAPopulation, double[][]> utilizationMap;

    private final UtilizationCalculator utilCalculator;

    public PrioResidenceTimeHelper(int[] opPriorities, int[] clPriorities, double[][][] opServTime,
                                   double[][][] clServTime, Map<MVAPopulation, double[][]> queueLengthMap, Map<MVAPopulation, double[][]> utilizationMap,
                                   UtilizationCalculator utilCalculator, double[][] opUtilization) {

        this.numOpenClasses = opPriorities.length;
        this.numClosedClasses = clPriorities.length;
        this.opPriorities = opPriorities;
        this.clPriorities = clPriorities;
        this.opServTime = opServTime;
        this.clServTime = clServTime;
        this.queueLengthMap = queueLengthMap;
        this.utilizationMap = utilizationMap;
        this.utilCalculator = utilCalculator;
        this.opUtilization = opUtilization;
    }

    // Used for HOL
    public double getLowerPriorityServerUsage(MVAPopulation population, int station, int clas, int clasType) {
        int clasPriority = getClassPriority(clas, clasType);
        double lowerServerUsage = 0;

        int lowerClosedClass = numClosedClasses - 1;
        while (lowerClosedClass >= 0 && clasPriority > clPriorities[lowerClosedClass]) {
            double util = utilizationMap.get(population)[station][lowerClosedClass];
            lowerServerUsage += util * clServTime[station][lowerClosedClass][0];
            lowerClosedClass--;
        }

        int lowerOpenClass = numOpenClasses - 1;
        while (lowerOpenClass >= 0 && clasPriority > opPriorities[lowerOpenClass]) {
            double util = opUtilization[station][lowerOpenClass];
            //utilCalculator.getOpenUtilisation(population, numCustomers, station, lowerOpenClass);
            //utilizationMap.get(population)[station][numClosedClasses + lowerOpenClass];
            lowerServerUsage += util * opServTime[station][lowerOpenClass][0];
            lowerOpenClass--;
        }

        return lowerServerUsage;
    }

    public double getHigherPriorityResidenceTime(MVAPopulation population, int station, int clas, int clasType) {
        return getResidenceTime(population, station, clas, clasType, HIGHER_PRIORITY_ONlY);
    }

    public double getHigherAndEqualPriorityResidenceTime(MVAPopulation population, int station, int clas, int clasType) {
        return getResidenceTime(population, station, clas, clasType, HIGHER_OR_EQUAL_PRIORITY);
    }


    public double getOneMinusHigherUtil(MVAPopulation population, int numCustomers, int station, int clas, int clasType) {
        return getOneMinusUtil(population, numCustomers, station, clas, clasType, HIGHER_PRIORITY_ONlY);
    }

    public double getOneMinusHigherAndEqualUtil(MVAPopulation population, int numCustomers, int station, int clas, int clasType) {
        return getOneMinusUtil(population, numCustomers, station, clas, clasType, HIGHER_OR_EQUAL_PRIORITY);
    }

    private double getResidenceTime(MVAPopulation population, int station, int clas, int clasType,
                                    boolean higherAndEqual) {
        int clasPriority = getClassPriority(clas, clasType);

        double higherResidenceTime = 0;
        // Classes are sorted in order by priority

        int higherOpenClass = 0;
        while (higherOpenClass < numOpenClasses && ((clasPriority < opPriorities[higherOpenClass])
                || (higherAndEqual && clasPriority == opPriorities[higherOpenClass]))) {
            double queueLength = queueLengthMap.get(population)[station][numClosedClasses + higherOpenClass];
            higherResidenceTime += queueLength * opServTime[station][higherOpenClass][0];
            higherOpenClass++;
        }


        int higherClosedClass = 0;
        while (higherClosedClass < numClosedClasses && ((clasPriority < clPriorities[higherClosedClass])
                || (higherAndEqual && clasPriority == clPriorities[higherClosedClass]))) {
            double queueLength = queueLengthMap.get(population)[station][higherClosedClass];
            higherResidenceTime += queueLength * clServTime[station][higherClosedClass][0];
            higherClosedClass++;
        }

        return higherResidenceTime;
    }

    private double getOneMinusUtil(MVAPopulation population, int numCustomers, int station, int clas, int clasType,
                                   boolean higherAndEqual) {
        int clasPriority = getClassPriority(clas, clasType);
        double higherUtil = 0;

        int higherOpenClass = 0;
        while (higherOpenClass < numOpenClasses && ((clasPriority < opPriorities[higherOpenClass])
                || (higherAndEqual && clasPriority == opPriorities[higherOpenClass]))) {
            higherUtil += utilCalculator.getOpenUtilisation(population, numCustomers, station, higherOpenClass);
            higherOpenClass++;
        }

        int higherClosedClass = 0;
        while (higherClosedClass < numClosedClasses && ((clasPriority < clPriorities[higherClosedClass])
                || (higherAndEqual && clasPriority == clPriorities[higherClosedClass]))) {
            higherUtil += utilCalculator.getClosedUtilisation(population, numCustomers, station, higherClosedClass);
            higherClosedClass++;
        }

        return 1.0 - higherUtil;
    }

    private int getClassPriority(int clas, int clasType) {
        if (clasType == SolverMulti.OPEN_CLASS) {
            return opPriorities[clas];
        } else if (clasType == SolverMulti.CLOSED_CLASS) {
            return clPriorities[clas];
        }
        throw new RuntimeException("Unknown class type: " + clasType);
    }
}
