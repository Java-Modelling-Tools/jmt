package jmt.jmva.analytical.solvers.utilities;

import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.priority.PriorityToNonPriorityConvertor;

import java.util.Arrays;

public class ProcessingCapacityChecker {

    //@author Stefano Omini
    //modified Ben Pahnke
    //TODO aggiungere controllo su processing capacity
    //TODO translated: add control over processing capacity

    /**
     * A system is said to have sufficient capacity to process a given load
     * <tt>lambda</tt> if no service center is saturated as a result of the combined loads
     * of all the open classes.
     * <br>
     * WARNING: This method should be called before solving the system.
     *
     * @param stationType Eg. LI, LD, Delay, PRS, HOL
     * @param visits      [station] [class]
     * @param servTime    [station] [class] [population]
     * @param classData   Population for closed classes, arrival rate for open classes
     * @param classType   Closed or Open (constants found in SolverMulti)
     * @param servers     Number of servers at each station
     * @param priorities  [class] Priority of each class
     * @return true if sufficient capacity exists for the given workload, false otherwise
     */
    public static boolean mixedHasSufficientProcessingCapacity(int[] stationType, double[][] visits, double[][][] servTime,
                                                               double[] classData, int[] classType, int[] servers, int[] priorities) {
        if (onlyClosedClasses(classType)) {
            return true;
        }

        //the maximum aggregate utilization between all the stations must be < 1
        //otherwise the system has no sufficient processing capacity
        for (int i = 0; i < stationType.length; i++) {
            if (stationType[i] == SolverMulti.DELAY) {
                //delay station: do not check saturation
                continue;
            }
            if (PriorityToNonPriorityConvertor.stationIsPriority(stationType[i])) {
                if (!priorityStationHasCapacity(i, visits, servTime, classData, classType, servers, priorities)) {
                    return false;
                }
            } else if (!nonPriorityStationHasCapacity(i, visits, servTime, classData, classType, servers)) {
                return false;
            }
        }
        //there are no stations with aggregate utilization >= number of servers
        return true;
    }

    private static boolean onlyClosedClasses(int[] classTypes) {
        for (int type : classTypes) {
            if (type == SolverMulti.OPEN_CLASS) {
                return false;
            }
        }
        return true;
    }

    private static boolean nonPriorityStationHasCapacity(int station, double[][] visits,
                                                         double[][][] servTime, double[] classData,
                                                         int[] classTypes, int[] servers) {
        //utiliz is the aggregate utilization for station j
        double utiliz = 0;
        for (int j = 0; j < classTypes.length; j++) {
            //consider only open classes
            if (classTypes[j] == SolverMulti.OPEN_CLASS) {
                utiliz += classData[j] * visits[station][j] * servTime[station][j][0];
            }
        }
        return utiliz < servers[station];
    }

    private static boolean priorityStationHasCapacity(int station, double[][] visits,
                                                      double[][][] servTime, double[] classData,
                                                      int[] classTypes, int[] servers, int[] priorities) {
        int highestPriorityClass = getHighestPriorityClass(station, priorities, servTime);
        double arrivalRateSum = 0;
        for (int clas = 0; clas < classTypes.length; clas++) {
            if (classTypes[clas] != SolverMulti.OPEN_CLASS) {
                continue;
            }
            arrivalRateSum += classData[clas] * visits[station][clas];

        }
        if (servers[station] / servTime[station][highestPriorityClass][0] <= arrivalRateSum) {
            return false;
        }
        return nonPriorityStationHasCapacity(station, visits, servTime, classData, classTypes, servers);
    }

    private static int getHighestPriorityClass(int station, int[] priorities, double[][][] servTime) {
        int highestPriorityClass = 0;
        int highestPriority = priorities[highestPriorityClass];
        for (int clas = 1; clas < priorities.length; clas++) {
            if ((priorities[clas] > highestPriority) ||
                    (priorities[clas] == highestPriority &&
                            servTime[station][clas][0] > servTime[station][highestPriorityClass][0])) {
                highestPriorityClass = clas;
                highestPriority = priorities[highestPriorityClass];
            }
        }
        return highestPriorityClass;
    }

    /**
     * Below are methods to allow for processing capacity to be checked with varying amounts of data.
     * E.g. number of servers can be missing or priorities can be missing
     */

    public static boolean mixedHasSufficientProcessingCapacity(int[] stationType, double[][] visits, double[][][] servTime,
                                                               double[] classData, int[] classType, int[] servers) {
        int[] priorities = new int[classData.length];
        Arrays.fill(priorities, ExactConstants.DEFAULT_CLASS_PRIORITY);
        return mixedHasSufficientProcessingCapacity(stationType, visits, servTime, classData, classType, servers, priorities);
    }

    public static boolean mixedHasSufficientProcessingCapacity(int[] stationType, double[][] visits,
                                                               double[][][] servTime, double[] classData, int[] classTypes) {
        int[] servers = new int[stationType.length];
        Arrays.fill(servers, 1);
        return mixedHasSufficientProcessingCapacity(stationType, visits, servTime, classData, classTypes, servers);
    }

    public static boolean openHasSufficientProcessingCapacity(int[] stationType, double[][] visits,
                                                              double[][][] servTime, double[] arrivalRates, int[] servers) {
        int[] classTypes = new int[arrivalRates.length];
        Arrays.fill(classTypes, SolverMulti.OPEN_CLASS);
        return mixedHasSufficientProcessingCapacity(stationType, visits, servTime, arrivalRates, classTypes, servers);
    }

    public static boolean openHasSufficientProcessingCapacity(int[] stationType, double[][] visits,
                                                              double[][][] servTime, double[] arrivalRates) {
        int[] servers = new int[stationType.length];
        Arrays.fill(servers, 1);
        return openHasSufficientProcessingCapacity(stationType, visits, servTime, arrivalRates, servers);
    }
}
