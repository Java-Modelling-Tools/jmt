package jmt.jmva.analytical.solvers.multiSolverAlgorithms;

import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.SolverMultiIterative;
import jmt.jmva.analytical.solvers.priority.PriorityToNonPriorityConvertor;
import jmt.jmva.analytical.solvers.priority.rwaCalculators.ReducedServiceTimeCalculator;
import jmt.jmva.analytical.solvers.priority.rwaCalculators.implementations.SCAServiceTimeCalculator;
import jmt.jmva.analytical.solvers.utilities.ProcessingCapacityChecker;

import java.util.*;

/**
 * Reduced Work Rate Approximation
 */

public class SolverMultiMixedRWA extends SolverMultiIterative {

    /**
     * Array with class types
     */
    private int[] classTypes;

    /**
     * Array describing the classes: each element can be either an
     * arrival rate (for open classes) or a population (for closed ones),
     * according to the class type
     */
    private double[] classData;

    private SolverMulti nonPrioritySolver;
    private ReducedServiceTimeCalculator reducedServiceTimeCalculator;

    /**
     * Number of PRS or HOL stations in original model
     */
    private int numPriorityStations;
    /**
     * Number of LI, LD, Delay stations in the original model
     */
    private int numNonPriorityStations;
    /**
     * Number of shadow servers created
     */
    private int numConvertedStations;

    /**
     * Ordered list of priorities
     */
    private List<Integer> uniquePriorities;
    /**
     * numPriorities = length(uniquePriorities)
     */
    private int numPriorities;
    /**
     * Map from class index to index in the list uniquePriorities
     */
    private Map<Integer, Integer> classToPriorityIndex;

    /**
     * The inputs of the shadow model created
     */
    private int[] convertedStationTypes;
    private double[][][] convertedServiceTimes;
    private double[][] convertedVisits;

    /**
     * Map from the shadow model to the original model
     * so index of converted arrays above to the index
     * in the main arrays such as "throughput"
     */
    private Map<Integer, Integer> shadowIndexToOldIndex;

    /**
     * The algorithm checks for convergence via the throughput
     * but only for the shadow servers
     */
    private double[][] lastConvergenceCheckThroughput;
    /**
     * It can oscillate between two solutions instead of
     * settling on one so the last two results need to be stored
     */
    private double[][] secondLastConvergenceCheckThroughput;
    private boolean oscillatingSolution;

    /**
     * When calculating new service times, the times can become negative. If
     * this occurs, the algorithm should stop
     */
    private boolean forceStop;

    /**
     * @param classes  number of classes.
     * @param stations number of stations.
     */
    public SolverMultiMixedRWA(int classes, int stations, ReducedServiceTimeCalculator reducedServiceTimeCalculator) {
        super(classes, stations);
        this.classTypes = new int[classes];
        this.classData = new double[classes];

        this.reducedServiceTimeCalculator = reducedServiceTimeCalculator;
    }

    // Defaults to SCA, if Kaufman (ESA) or HOL extension is added then update
    public SolverMultiMixedRWA(int classes, int stations) {
        this(classes, stations, new SCAServiceTimeCalculator());
    }


    /**
     * initializes the solver with the system parameters.
     *
     * @param n          array of names of service centers.
     * @param t          array of the types (LD, LI, Delay, PRS, HOL) of service centers.
     * @param s          matrix of service time of the service centers.
     * @param v          matrix of visits to the service centers.
     * @param classData  array describing the classes: each element can be either an
     *                   arrival rate (for open classes) or a population (for closed ones),
     *                   according to the class type
     * @param classTypes array of class types (open or closed)
     * @param p          array of priorities of the classes.
     * @param tolerance  terminates if |last result - current result| < tolerance
     * @return true if the operation has been completed with success
     */
    public boolean input(String[] n, int[] t, double[][][] s, double[][] v, double[] classData, int[] classTypes,
                         int[] p, double tolerance) {
        if (classData.length != classes || classTypes.length != classes) {
            return false;
        }
        if (!input(n, t, s, v, p)) {
            // Failed loading
            return false;
        }
        setTolerance(tolerance);

        System.arraycopy(classData, 0, this.classData, 0, classes);
        System.arraycopy(classTypes, 0, this.classTypes, 0, classes);

        Set<Integer> uniquePrioritiesSet = new HashSet<>();
        for (int clas = 0; clas < classes; clas++) {
            uniquePrioritiesSet.add(priorities[clas]);
        }
        uniquePriorities = new ArrayList<>(uniquePrioritiesSet);
        // Sorts from the highest value to the lowest
        Collections.sort(uniquePriorities, new Comparator<Integer>() {
            @Override
            public int compare(Integer cl1, Integer cl2) {
                return cl2 - cl1;
            }
        });
        numPriorities = uniquePriorities.size();

        return true;
    }

    public boolean input(String[] n, int[] t, double[][][] s, double[][] v, double[] classData, int[] classTypes,
                         int[] p) {
        return input(n, t, s, v, classData, classTypes, p, DEFAULT_TOLERANCE);
    }

    @Override
    public void solve() {
        createShadowModel();
        initialiseGlobals();
        iterations = 0;
        boolean converged = false;
        while (iterations < MAX_ITERATIONS && !converged) {
            setNewServTimeForShadowServers();
            if (forceStop || !nonPrioritySolver.hasSufficientProcessingCapacity()) {
                System.out.printf("Shadow model has become unstable after %s iterations%n", iterations);
                break;
            }
            solveShadowModel();
            converged = checkConverged();
            iterations++;
        }
        convertShadowModelToOriginalModel();
    }

    private void convertShadowModelToOriginalModel() {
        for (int station = 0; station < numNonPriorityStations; station++) {
            copyNonPriorityStationAcross(station);
        }
        for (int station = 0; station < numPriorityStations; station++) {
            copyPriorityStationAcross(station);
        }
    }

    /**
     * Needs to copy values from all of the shadow servers associated with the station
     *
     * @param station The priorityIndex
     */
    private void copyPriorityStationAcross(int station) {
        int stationStartIndex = getShadowStationStartIndex(station);
        int originalIndex = shadowIndexToOldIndex.get(stationStartIndex);
        for (int clas = 0; clas < classes; clas++) {
            int shadowStationIndex = stationStartIndex + classToPriorityIndex.get(clas);
            throughput[originalIndex][clas] = nonPrioritySolver.getThroughput()[shadowStationIndex][clas];
            queueLen[originalIndex][clas] = nonPrioritySolver.getQueueLen()[shadowStationIndex][clas];
            utilization[originalIndex][clas] = nonPrioritySolver.getThroughput()[shadowStationIndex][clas] * servTime[originalIndex][clas][0];
            residenceTime[originalIndex][clas] = nonPrioritySolver.getResTime()[shadowStationIndex][clas];
        }
    }

    /**
     * Directly copies from the solution of the multi solver
     *
     * @param shadowIndex the station in the converted model to be copied across
     */
    private void copyNonPriorityStationAcross(int shadowIndex) {
        int originalIndex = shadowIndexToOldIndex.get(shadowIndex);
        System.arraycopy(nonPrioritySolver.getThroughput()[shadowIndex], 0, throughput[originalIndex], 0, classes);
        System.arraycopy(nonPrioritySolver.getUtilization()[shadowIndex], 0, utilization[originalIndex], 0, classes);
        System.arraycopy(nonPrioritySolver.getQueueLen()[shadowIndex], 0, queueLen[originalIndex], 0, classes);
        System.arraycopy(nonPrioritySolver.getResTime()[shadowIndex], 0, residenceTime[originalIndex], 0, classes);
    }

    private boolean checkConverged() {
        double[][] newThroughput = new double[numPriorityStations][classes];
        boolean converged = true;
        double[][] throughputSolution = nonPrioritySolver.getThroughput();
        for (int station = 0; station < numPriorityStations; station++) {
            for (int clas = 0; clas < classes; clas++) {
                int stationStartIndex = getShadowStationStartIndex(station);
                newThroughput[station][clas] = throughputSolution[stationStartIndex + classToPriorityIndex.get(clas)][clas];
                if (newThroughput[station][clas] - lastConvergenceCheckThroughput[station][clas] > tolerance) {
                    converged = false;
                }
            }
        }
        if (converged) {
            return true;
        }
        converged = checkOscillatingSolution(newThroughput);
        secondLastConvergenceCheckThroughput = lastConvergenceCheckThroughput;
        lastConvergenceCheckThroughput = newThroughput;
        return converged;
    }

    private boolean checkOscillatingSolution(double[][] newThroughput) {
        oscillatingSolution = true;
        for (int station = 0; station < numPriorityStations; station++) {
            for (int clas = 0; clas < classes; clas++) {
                if (newThroughput[station][clas] - secondLastConvergenceCheckThroughput[station][clas] > tolerance) {
                    oscillatingSolution = false;
                    break;
                }
            }
        }
        return oscillatingSolution;
    }

    private void initialiseGlobals() {
        reducedServiceTimeCalculator.initialise(this);
        lastConvergenceCheckThroughput = new double[numPriorityStations][classes];
        secondLastConvergenceCheckThroughput = new double[numPriorityStations][classes];
        SolverMultiMixed mixedSolver = new SolverMultiMixed(classes, numConvertedStations);
        // The station names aren't important
        mixedSolver.input(new String[numConvertedStations], convertedStationTypes, convertedServiceTimes, convertedVisits, classData, classTypes);
        nonPrioritySolver = mixedSolver;
        oscillatingSolution = false;
        forceStop = false;
    }

    private void solveShadowModel() {
        nonPrioritySolver.setServTime(convertedServiceTimes);
        nonPrioritySolver.solve();
    }

    private void setNewServTimeForShadowServers() {
        double[][] newServTimes = reducedServiceTimeCalculator.calculateNewServiceTimes(this);
        if (forceStop) {
            return;
        }
        for (int station = 0; station < numPriorityStations; station++) {
            int shadowStart = getShadowStationStartIndex(station);
            for (int priority = 0; priority < numPriorities; priority++) {
                int currentPriority = uniquePriorities.get(priority);
                for (int clas = 0; clas < classes; clas++) {
                    if (priorities[clas] != currentPriority) {
                        convertedServiceTimes[shadowStart + priority][clas][0] = 0.0;
                    } else {
                        convertedServiceTimes[shadowStart + priority][clas][0] = newServTimes[station][clas];
                    }
                }
            }
        }
    }

    /**
     * Stations go to
     * [non-priority stations] ++ [shadow priority stations]
     * The non-priority stations are in the same order but with the priority stations removed
     * Priority station i goes to
     * num non-priority stations + (stations * num priority stations before i in list)
     * and has num priorities shadow servers
     */
    private void createShadowModel() {
        calculateNumberOfShadowStations();
        convertedStationTypes = new int[numConvertedStations];
        convertedVisits = new double[numConvertedStations][classes];
        convertedServiceTimes = new double[numConvertedStations][classes][];

        shadowIndexToOldIndex = new HashMap<>();
        int nonPriorityIndex = 0;
        int priorityIndex = 0;
        for (int station = 0; station < stations; station++) {
            if (PriorityToNonPriorityConvertor.stationIsPriority(type[station])) {
                int startOfShadowStations = getShadowStationStartIndex(priorityIndex);
                initialiseShadowStationsForAPriorityStation(station, startOfShadowStations);
                // Shadow to old index map handled in the intialisation function
                priorityIndex++;
            } else {
                initialiseShadowStationsForANonPriorityStation(station, nonPriorityIndex);
                shadowIndexToOldIndex.put(nonPriorityIndex, station);
                nonPriorityIndex++;
            }
        }

        // TODO: There's got to be better logic than this to do this
        classToPriorityIndex = new HashMap<>();
        for (int clas = 0; clas < classes; clas++) {
            for (int i = 0; i < uniquePriorities.size(); i++) {
                if (uniquePriorities.get(i) == priorities[clas]) {
                    classToPriorityIndex.put(clas, i);
                    break;
                }
            }
        }
    }

    public int getShadowStationStartIndex(int stationIndex) {
        return numNonPriorityStations + stationIndex * numPriorities;
    }

    private void calculateNumberOfShadowStations() {
        numPriorityStations = PriorityToNonPriorityConvertor.countNumPriorityStations(type);
        numNonPriorityStations = stations - numPriorityStations;
        int numShadowStations = numPriorities * numPriorityStations;
        numConvertedStations = numNonPriorityStations + numShadowStations;
    }

    private void initialiseShadowStationsForANonPriorityStation(int station, int nonPriorityIndex) {
        convertedStationTypes[nonPriorityIndex] = type[station];
        convertedServiceTimes[nonPriorityIndex] = servTime[station];
        convertedVisits[nonPriorityIndex] = visits[station];
    }

    private void initialiseShadowStationsForAPriorityStation(int station, int startOfShadowStations) {
        for (int priority = 0; priority < numPriorities; priority++) {
            int currentShadowStation = startOfShadowStations + priority;
            shadowIndexToOldIndex.put(currentShadowStation, station);
            convertedStationTypes[currentShadowStation] = SolverMulti.LI;

            int currentPriority = uniquePriorities.get(priority);
            for (int clas = 0; clas < classes; clas++) {
                convertedServiceTimes[currentShadowStation][clas] = new double[1];
                convertedServiceTimes[currentShadowStation][clas][0] = servTime[station][clas][0];

                if (priorities[clas] == currentPriority) {
                    convertedVisits[currentShadowStation][clas] = visits[station][clas];
                } else {
                    convertedVisits[currentShadowStation][clas] = 0.0;
                }
            }
        }
    }

    @Override
    public boolean hasSufficientProcessingCapacity() {
        return ProcessingCapacityChecker.mixedHasSufficientProcessingCapacity(type, visits, servTime, classData, classTypes);
    }

    public double[][] getLastConvergenceCheckThroughput() {
        return lastConvergenceCheckThroughput;
    }

    public double[][][] getServTime() {
        return servTime;
    }

    public double[][][] getConvertedServiceTimes() {
        return convertedServiceTimes;
    }

    public int[] getStationTypes() {
        return type;
    }

    public int getClasses() {
        return classes;
    }

    public int getNumPriorityStations() {
        return numPriorityStations;
    }

    public int[] getPriorities() {
        return priorities;
    }

    public int getStations() {
        return stations;
    }

    public void forceStop() {
        forceStop = true;
    }
}
