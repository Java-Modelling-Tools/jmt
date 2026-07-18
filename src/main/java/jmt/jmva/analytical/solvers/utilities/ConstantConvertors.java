package jmt.jmva.analytical.solvers.utilities;

import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.solvers.Solver;
import jmt.jmva.analytical.solvers.SolverMulti;

public class ConstantConvertors {

    // TODO: Merge SolverMulti and SolverSingle Constants
    // they have the same values but have just been declared twice

    /**
     * Map class types from model constants to solver constants
     */
    public static int[] mapClassTypes(int[] classTypes) {
        int len = classTypes.length;
        int[] res = new int[len];
        for (int i = 0; i < len; i++) {
            switch (classTypes[i]) {
            case ExactConstants.CLASS_OPEN:
                res[i] = SolverMulti.OPEN_CLASS;
                break;
            case ExactConstants.CLASS_CLOSED:
                res[i] = SolverMulti.CLOSED_CLASS;
                break;
            default:
                res[i] = -1;
            }
        }
        return res;
    }

    /**
     * Map station types from model constants to solver constants
     */
    public static int[] mapStationTypes(int[] stationTypes, boolean multiClass) {
        int len = stationTypes.length;
        int[] res = new int[len];
        for (int i = 0; i < len; i++) {
            switch (stationTypes[i]) {
            case ExactConstants.STATION_LD:
                res[i] = multiClass ? SolverMulti.LD : Solver.LD;
                break;
            case ExactConstants.STATION_LI:
                res[i] = multiClass ? SolverMulti.LI : Solver.LI;
                break;
            case ExactConstants.STATION_DELAY:
                res[i] = multiClass ? SolverMulti.DELAY : Solver.DELAY;
                break;
            case ExactConstants.STATION_PRS:
                res[i] = multiClass ? SolverMulti.PRS : Solver.PRS;
                break;
            case ExactConstants.STATION_HOL:
                res[i] = multiClass ? SolverMulti.HOL : Solver.HOL;
                break;
            default:
                res[i] = -1;
            }
        }
        return res;
    }

}
