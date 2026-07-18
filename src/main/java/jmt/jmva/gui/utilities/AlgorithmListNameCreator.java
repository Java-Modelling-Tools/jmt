package jmt.jmva.gui.utilities;

import jmt.jmva.analytical.solvers.SolverAlgorithm;

import java.util.ArrayList;
import java.util.List;

public class AlgorithmListNameCreator {

    // TODO: Dynamically calculate number of dashes required for the separators based of the longest algorithm name?
    private static final String SEPARATOR_EXACT = "----------- Exact -----------";
    private static final String SEPARATOR_APPROXIMATE = "------- Approximate -------";
    private static final String SEPARATOR_PRIORITY = "----- Approximate (Prio) -----";

    public static String[] createAlgoNameList(boolean isClosed, boolean isOpen, boolean isLD, boolean isPriority) {
        List<SolverAlgorithm> allValues = getRequiredAlgorithms(isClosed, isOpen, isLD, isPriority);
        List<SolverAlgorithm> exactAlgos = SolverAlgorithmCounter.getExactAlgorithms(allValues, true, false);
        List<SolverAlgorithm> approxAlgos = SolverAlgorithmCounter.getApproximateAlgorithms(allValues, true, false);

        List<SolverAlgorithm> priorityAlgos = new ArrayList<>();
        if (isPriority) {
            priorityAlgos = SolverAlgorithmCounter.getPriorityAlgorithms(allValues, false);
        }

        int numSections = exactAlgos.size() == 0 ? 0 : 1;
        numSections += approxAlgos.size() == 0 ? 0 : 1;
        numSections += priorityAlgos.size() == 0 ? 0 : 1;

        String[] allList = new String[allValues.size() + numSections];
        int listIndex = 0;

        // Add exact
        if (exactAlgos.size() != 0) {
            allList[listIndex] = SEPARATOR_EXACT;
            listIndex++;
            listIndex = addAlgosToList(allList, exactAlgos, listIndex);
        }

        // Add Approximate
        if (approxAlgos.size() != 0) {
            allList[listIndex] = SEPARATOR_APPROXIMATE;
            listIndex++;
            listIndex = addAlgosToList(allList, approxAlgos, listIndex);
        }

        // Add Priorities
        if (priorityAlgos.size() != 0) {
            allList[listIndex] = SEPARATOR_PRIORITY;
            listIndex++;
            listIndex = addAlgosToList(allList, priorityAlgos, listIndex);
        }

        // TODO: Separate out load-dependent algorithms too?
        // I don't think there's many of them right now though

        return allList;
    }

    private static int addAlgosToList(String[] allList, List<SolverAlgorithm> algorithms, int listIndex) {
        for (SolverAlgorithm algorithm : algorithms) {
            allList[listIndex] = algorithm.toString();
            listIndex++;
        }
        return listIndex;
    }

    private static List<SolverAlgorithm> getRequiredAlgorithms(boolean isClosed, boolean isOpen, boolean isLD, boolean isPriority) {
        List<SolverAlgorithm> newAlgorithms = new ArrayList<>();
        SolverAlgorithm[] allAlgorithms = SolverAlgorithm.values();
        for (SolverAlgorithm algorithm : allAlgorithms) {
            // !Closed = Open or Mixed model
            if (!isClosed && !algorithm.supportsOpenClasses()) {
                continue;
            }
            // !Open = Closed or Mixed model
            if (!isOpen && !algorithm.supportsClosedClasses()) {
                continue;
            }
            // Load Dependent model
            if (isLD && !algorithm.supportsLoadDependent()) {
                continue;
            }
            // Priority model
            if (isPriority && !algorithm.supportsPriorities()) {
                continue;
            }
            newAlgorithms.add(algorithm);
        }
        return newAlgorithms;
    }
}
