package jmt.jmva.gui.utilities;

import jmt.jmva.analytical.solvers.SolverAlgorithm;

import java.util.ArrayList;
import java.util.List;

public class SolverAlgorithmCounter {


    public static int numExactAlgs() {
        return countExact(SolverAlgorithm.values());
    }

    public static int numClosedExactAlgorithms() {
        return countExact(SolverAlgorithm.closedValues());
    }

    public static int numOpenExactAlgorithms() {
        return countExact(SolverAlgorithm.openValues());
    }

    public static int numMixedExactAlgorithms() {
        return countExact(SolverAlgorithm.mixedValues());
    }

    public static int countExact(SolverAlgorithm[] algorithms) {
        int exact = 0;
        for (SolverAlgorithm algo : algorithms) {
            if (algo.isExact()) {
                exact++;
            }
        }
        return exact;
    }

    public static int numPriorityAlgs() {
        return countPriority(SolverAlgorithm.values());
    }

    public static int numClosedPriorityAlgorithms() {
        return countPriority(SolverAlgorithm.closedValues());
    }

    public static int numOpenPriorityAlgorithms() {
        return countPriority(SolverAlgorithm.openValues());
    }

    public static int numMixedPriorityAlgorithms() {
        return countPriority(SolverAlgorithm.mixedValues());
    }

    public static int countPriority(SolverAlgorithm[] algorithms) {
        int priorities = 0;
        for (SolverAlgorithm algo : algorithms) {
            if (algo.supportsPriorities()) {
                priorities++;
            }
        }
        return priorities;
    }

    public static List<SolverAlgorithm> getExactAlgorithms(List<SolverAlgorithm> algorithms, boolean excludePriority, boolean excludeLD) {
        List<SolverAlgorithm> exact = new ArrayList<>();
        for (SolverAlgorithm algo : algorithms) {
            if (excludePriority && algo.supportsPriorities()) {
                continue;
            }
            if (excludeLD && algo.supportsLoadDependent()) {
                continue;
            }
            if (algo.isExact()) {
                exact.add(algo);
            }
        }
        return exact;
    }

    public static List<SolverAlgorithm> getApproximateAlgorithms(List<SolverAlgorithm> algorithms, boolean excludePriority, boolean excludeLD) {
        List<SolverAlgorithm> approximate = new ArrayList<>();
        for (SolverAlgorithm algo : algorithms) {
            if (excludePriority && algo.supportsPriorities()) {
                continue;
            }
            if (excludeLD && algo.supportsLoadDependent()) {
                continue;
            }
            if (!algo.isExact()) {
                approximate.add(algo);
            }
        }
        return approximate;
    }

    public static List<SolverAlgorithm> getPriorityAlgorithms(List<SolverAlgorithm> algorithms, boolean excludeLD) {
        List<SolverAlgorithm> priorities = new ArrayList<>();
        for (SolverAlgorithm algo : algorithms) {
            if (excludeLD && algo.supportsLoadDependent()) {
                continue;
            }
            if (algo.supportsPriorities()) {
                priorities.add(algo);
            }
        }
        return priorities;
    }
}
