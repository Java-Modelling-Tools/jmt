package jmt.jmva.analytical.solvers.priority;

import jmt.jmva.analytical.solvers.SolverMulti;

import java.util.List;

/**
 * Alot of this class isn't currently used, however, the functions implemented may be of use at some point
 */
public class PriorityToNonPriorityConvertor {

    public static boolean stationIsPriority(int stationType) {
       return stationType == SolverMulti.PRS || stationType == SolverMulti.HOL ;
    }

    public static int countNumPriorityStations(int[] stationTypes) {
        int numPriority = 0;
        for (int type: stationTypes) {
            if (stationIsPriority(type)) {
                numPriority++;
            }
        }
        return numPriority;
    }

    public static int[] getNonPriorityStationTypes(int[] oldTypes) {
        int[] convertedStationTypes = new int[oldTypes.length];

        for (int station = 0; station < oldTypes.length; station++) {
            if (oldTypes[station] == SolverMulti.PRS || oldTypes[station] == SolverMulti.HOL) {
                convertedStationTypes[station] = SolverMulti.LI;
            } else {
                // LI, LD, Delay
                convertedStationTypes[station] = oldTypes[station];
            }
        }
        return convertedStationTypes;
    }

    public static double[][][] getClosedOnlyServiceTimes(double[][][] servTimes, int[] type, List<Integer> closedClasses, int closedClassesTotalPop) {
        double[][][] closedOnlyServiceTimes = new double[type.length][closedClasses.size()][];

        for (int station = 0; station < type.length; station++) {
            for (int i = 0; i < closedClasses.size(); i++) {
                Integer clas = closedClasses.get(i);
                if (type[station] == SolverMulti.LD) {
                    // Don't need totPop, only closed pop
                    closedOnlyServiceTimes[station][clas] = new double[closedClassesTotalPop];
                } else {
                    // LI, Delay, PRS, HOL
                    closedOnlyServiceTimes[station][clas] = new double[1];
                }

                System.arraycopy(servTimes[station][clas], 0,
                        closedOnlyServiceTimes[station][i], 0,
                        closedClassesTotalPop);
            }
        }

        return closedOnlyServiceTimes;
    }

    public static double[][] getClosedOnlyVisits(double[][] visits, List<Integer> closedClasses) {
        double[][] closedOnlyVisits = new double[visits.length][closedClasses.size()];
        for (int station = 0; station < visits.length; station++) {
            for (int i = 0; i < closedClasses.size(); i++) {
                Integer clas = closedClasses.get(i);
                closedOnlyVisits[station][i] = visits[station][clas];
            }
        }

        return closedOnlyVisits;
    }

    public static int[] getClosedOnlyPopulations(double[] classData, List<Integer> closedClasses) {
        int[] closedOnlyPop = new int[closedClasses.size()];
        for (int i = 0; i < closedClasses.size(); i++) {
            Integer clas = closedClasses.get(i);
            // For closed classes, classData = classPop which should be an int anyway
            closedOnlyPop[i] = (int) classData[clas];
        }

        return closedOnlyPop;
    }

}
