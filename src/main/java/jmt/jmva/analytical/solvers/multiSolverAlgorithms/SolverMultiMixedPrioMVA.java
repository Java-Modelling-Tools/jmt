package jmt.jmva.analytical.solvers.multiSolverAlgorithms;

import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.*;
import jmt.jmva.analytical.solvers.utilities.*;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.PrioMVAClosedQueueLengthCalculator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.PrioMVAClosedResidenceTimeCalculator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.PrioMVAOpenQueueLengthCalculator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.PrioMVAThroughputCalculator;

import java.util.*;


public class SolverMultiMixedPrioMVA extends SolverMulti {

    private int[] classTypes;

    /**
     * Each of the inputs and outputs are split into their class types.
     * The classes are sorted by priority in the arrays.
     * Descending order so the highest priority is at index 0
     */
    private int numOpenClasses;
    private int numClosedClasses;

    private double[][][] opServTime;
    private double[][][] clServTime;

    private double[][] opVisits;
    private double[][] clVisits;

    private double[][] opResidenceTimes;
    private double[][] clResidenceTimes;

    // Only used at the end, use queue length map for calculations
    private double[][] opQueueLength;
    private double[][] clQueueLength;

    private double[] clThroughput;
    private double[][] opFinalThroughput;
    private double[][] clFinalThroughput;

    private int[] opPriorities;
    private int[] clPriorities;

    private double[][] opUtilization;
    private double[] opUtilizationSums;
    // Only used at the end, use utilisation map for calculations
    private double[][] clUtilization;

    /**
     * Class data
     */
    private double[] opArrivalRate;
    private int[] clPopulation;

    private final PerformanceValuePerStationPerClassCalculator closedResidenceCalculator;
    private final ThroughputCalculator throughputCalculator;
    private final QueueLengthCalculator closedQueueLengthCalculator;
    private final PerformanceValuePerStationCalculator openQueueLengthCalculator;

    /**
     * A map from the index in the op/cl arrays to the original mixed arrays (servTime, visits)
     */
    private int[] opInternalIndexToOriginal;
    private int[] clInternalIndexToOriginal;

    /**
     * YMCB calculates residence time from lowest to highest priority, but
     * PrioMVA calculates it from the highest to lowest priority.
     * highestPriorityToLowestPriority determines
     * in what order they are calculated
     */
    private final boolean highestPriorityToLowestPriority;


    /**
     * Total population of all closed classes
     */
    private int closedClassesTotalPop;

    /**
     * Map from vector population to array of queue lengths.
     * Queue lengths are length of queues at a station for a given class
     * (Average population of a class at a station)
     * int[classes] -> double[stations][classes]
     * MVAPopulation is a wrapper for int[classes]
     * double[s][c] = "average" number of jobs of type c at station s
     * <p>
     * Index order of queueLength array is closed classed followed by open classes
     * so indices 0...(numClosedClasses-1) are for closed classes and indices
     * numClosedClasses...(numClosedClasses+numOpenClasses-1) are for open classes
     * <p>
     * Queue Length includes jobs being served
     */
    private Map<MVAPopulation, double[][]> queueLengthMap;

    /**
     * Map from vector population to array of utilizations.
     * int[classes] -> double[stations][classes]
     * MVAPopulation is a wrapper for int[classes]
     * double[s][c] = "average" utilization at station s for jobs of type c
     */
    private Map<MVAPopulation, double[][]> utilizationMap;

    /**
     * Defaults to CL variation (The MVA Priority Approximation)
     *
     * @param classes  number of classes.
     * @param stations number of stations.
     */
    public SolverMultiMixedPrioMVA(int classes, int stations) {
        this(classes, stations, new PrioMVAClosedResidenceTimeCalculator(),
                new PrioMVAThroughputCalculator(), new PrioMVAClosedQueueLengthCalculator(),
                new PrioMVAOpenQueueLengthCalculator(), true);
    }

    /**
     * @param classes        number of classes.
     * @param stations       number of stations.
     * @param utilCalculator To allow for the different variations so CL, BKT and T
     */
    public SolverMultiMixedPrioMVA(int classes, int stations, UtilizationCalculator utilCalculator) {
        this(classes, stations, new PrioMVAClosedResidenceTimeCalculator(utilCalculator),
                new PrioMVAThroughputCalculator(), new PrioMVAClosedQueueLengthCalculator(),
                new PrioMVAOpenQueueLengthCalculator(utilCalculator), true);
    }

    public SolverMultiMixedPrioMVA(int classes, int stations, PerformanceValuePerStationPerClassCalculator closedResidenceCalculator,
                                   ThroughputCalculator throughputCalculator, QueueLengthCalculator closedQueueLengthCalculator,
                                   PerformanceValuePerStationCalculator openQueueLengthCalculator,
                                   boolean highestPriorityToLowestPriority) {
        super(classes, stations);
        this.closedResidenceCalculator = closedResidenceCalculator;
        this.throughputCalculator = throughputCalculator;
        this.closedQueueLengthCalculator = closedQueueLengthCalculator;
        this.openQueueLengthCalculator = openQueueLengthCalculator;
        this.highestPriorityToLowestPriority = highestPriorityToLowestPriority;
        this.classTypes = new int[classes];
    }

    /**
     * initializes the solver with the system parameters.
     *
     * @param n         array of names of service centers.
     * @param t         array of the types (LD, LI, Delay, PRS, HOL) of service centers.
     * @param s         matrix of service time of the service centers.
     * @param v         matrix of visits to the service centers.
     * @param classData array describing the classes: each element can be either an
     *                  arrival rate (for open classes) or a population (for closed ones),
     *                  according to the class type
     * @param classType array of class types (open or closed)
     * @param p         array of priorities of the classes.
     * @return true if the operation has been completed with success
     */
    public boolean input(String[] n, int[] t, double[][][] s, double[][] v, double[] classData, int[] classType,
                         int[] p) {
        if (!input(n, t, s, v, p)) {
            // Failed loading
            return false;
        }
        System.arraycopy(classType, 0, this.classTypes, 0, classes);

        List<Integer> openClasses = new ArrayList<>();
        List<Integer> closedClasses = new ArrayList<>();

        if (!generateClassLists(openClasses, closedClasses, classType)) {
            return false;
        }
        numOpenClasses = openClasses.size();
        numClosedClasses = closedClasses.size();

        sortClassListsByPriority(openClasses, closedClasses);

        generateClassIndexMaps(openClasses, closedClasses);

        initialiseSeparateOpenArrays(openClasses, classData);
        initialiseSeparateClosedArrays(closedClasses, classData);
        return true;
    }

    /**
     * This generates a map from the class index of the internal structure
     * to the original indices of the arrays passed in so it can be correctly converted backwards afterwards
     */
    private void generateClassIndexMaps(List<Integer> openClasses, List<Integer> closedClasses) {
        opInternalIndexToOriginal = new int[openClasses.size()];
        for (int i = 0; i < openClasses.size(); i++) {
            opInternalIndexToOriginal[i] = openClasses.get(i);
        }

        clInternalIndexToOriginal = new int[closedClasses.size()];
        for (int i = 0; i < closedClasses.size(); i++) {
            clInternalIndexToOriginal[i] = closedClasses.get(i);
        }
    }

    /**
     * Sorts classes by class types into the respective lists
     */
    private boolean generateClassLists(List<Integer> openClasses, List<Integer> closedClasses, int[] classType) {
        for (int i = 0; i < classes; i++) {
            if (classType[i] == CLOSED_CLASS) {
                closedClasses.add(i);
            } else if (classType[i] == OPEN_CLASS) {
                openClasses.add(i);
            } else {
                // Unknown class type
                return false;
            }
        }
        return true;
    }

    private void sortClassListsByPriority(List<Integer> openClasses, List<Integer> closedClasses) {
        sortClassListByPriority(openClasses);
        sortClassListByPriority(closedClasses);
    }

    /**
     * Higher Number = Higher Priority
     */
    private void sortClassListByPriority(List<Integer> classes) {
        // Don't replace with lambda due to java version
        Collections.sort(classes, new Comparator<Integer>() {
            @Override
            public int compare(Integer cl1, Integer cl2) {
                return priorities[cl2] - priorities[cl1];
            }
        });
    }

    private void initialiseSeparateOpenArrays(List<Integer> openClasses, double[] classData) {
        opArrivalRate = new double[numOpenClasses];
        opVisits = new double[stations][];
        opResidenceTimes = new double[stations][];
        opServTime = new double[stations][][];
        opUtilization = new double[stations][];
        opUtilizationSums = new double[stations];
        opPriorities = new int[numOpenClasses];

        for (int station = 0; station < stations; station++) {
            opUtilization[station] = new double[numOpenClasses];
            opVisits[station] = new double[numOpenClasses];
            opResidenceTimes[station] = new double[numOpenClasses];
            opServTime[station] = new double[numOpenClasses][];
            for (int i = 0; i < numOpenClasses; i++) {
                int clas = openClasses.get(i);

                int servLength = servTime[station][clas].length;
                opServTime[station][i] = new double[servLength];
                System.arraycopy(servTime[station][clas], 0, opServTime[station][i], 0, servLength);

                opVisits[station][i] = visits[station][clas];
            }
        }

        for (int i = 0; i < numOpenClasses; i++) {
            int clas = openClasses.get(i);
            opPriorities[i] = priorities[clas];
            opArrivalRate[i] = classData[clas];
        }

    }

    private void initialiseSeparateClosedArrays(List<Integer> closedClasses, double[] classData) {
        calculateClosedPopulationValues(closedClasses, classData);

        clPopulation = new int[numClosedClasses];
        clVisits = new double[stations][];
        clResidenceTimes = new double[stations][];
        clServTime = new double[stations][][];
        clPriorities = new int[numClosedClasses];
        clThroughput = new double[numClosedClasses];

        for (int station = 0; station < stations; station++) {
            clVisits[station] = new double[numClosedClasses];
            clResidenceTimes[station] = new double[numClosedClasses];
            clServTime[station] = new double[numClosedClasses][];

            for (int i = 0; i < numClosedClasses; i++) {
                int clas = closedClasses.get(i);
                if (type[station] == SolverMulti.LD) {
                    clServTime[station][i] = new double[closedClassesTotalPop];
                    System.arraycopy(servTime[station][clas], 0, clServTime[station][i], 0, closedClassesTotalPop);
                } else {
                    clServTime[station][i] = new double[1];
                    clServTime[station][i][0] = servTime[station][clas][0];
                }

                clVisits[station][i] = visits[station][clas];
            }
        }

        for (int i = 0; i < numClosedClasses; i++) {
            int clas = closedClasses.get(i);
            clPopulation[i] = (int) classData[clas];
            clPriorities[i] = priorities[clas];
        }

    }

    /**
     * Calculates Total Population of closed classes
     */
    private void calculateClosedPopulationValues(List<Integer> closedClasses, double[] classData) {
        closedClassesTotalPop = 0;
        for (Integer clas : closedClasses) {
            closedClassesTotalPop += classData[clas];
        }
    }

    @Override
    public void solve() {
        calculateUtilizationsForOpenClasses();
        int[][] iteratorPops = MVAPopulationUtil.generatePopulations(clPopulation);
        initializeQueueLengthAndUtilMap();
        initialiseCalculators();

        // i = 0 is the zero population
        // Only the open queue lengths needs calculating for the zero population
        openQueueLengthCalculator.iteratePopulationUpdate(this);
        calculateQueueLengthForOpenClasses(new MVAPopulation(iteratorPops[0]),
                MiscMathsFunctions.sum(iteratorPops[0]));

        // classes are sorted from highest to lowest priority
        int startIndex = 0;
        int step = 1;
        if (!highestPriorityToLowestPriority) {
            startIndex = numClosedClasses - 1;
            step = -1;
        }

        for (int i = 1; i < iteratorPops.length; i++) {
            int numCustomers = MiscMathsFunctions.sum(iteratorPops[i]);
            MVAPopulation population = new MVAPopulation(iteratorPops[i]);
            updateUtilAndQueueMapForPopulation(population);
            updateCalculatorsForPopulation();

            for (int clClass = startIndex;
                 (clClass < numClosedClasses && highestPriorityToLowestPriority) ||
                         (clClass >= 0 && !highestPriorityToLowestPriority);
                 clClass += step) {
                closedResidenceCalculator.iterateClassUpdate(this);
                calculateResidenceTimesForClosedClasses(population, numCustomers, clClass);

                throughputCalculator.iterateClassUpdate(this);
                calculateThroughputForClosedClasses(population, numCustomers, clClass);

                updateClosedUtilizationMap(population, clClass);

                closedQueueLengthCalculator.iterateClassUpdate(this);
                calculateQueueLengthForClosedClasses(population, numCustomers, clClass);
            }

            openQueueLengthCalculator.iteratePopulationUpdate(this);
            calculateQueueLengthForOpenClasses(population, numCustomers);
        }
        setFinalQueueLengthAndUtilization();
        calculateThroughputPerStation();
        calculateFinalResidenceTimeForOpenClasses();
        mapSplitArraysBackToNonSplitArrays();
    }

    private void calculateFinalResidenceTimeForOpenClasses() {
        // Little's Law
        for (int station = 0; station < stations; station++) {
            for (int opClass = 0; opClass < numOpenClasses; opClass++) {
                opResidenceTimes[station][opClass] = opQueueLength[station][opClass] / opFinalThroughput[station][opClass];
            }
        }
    }

    private void updateCalculatorsForPopulation() {
        closedResidenceCalculator.iteratePopulationUpdate(this);
        throughputCalculator.iteratePopulationUpdate(this);
        closedQueueLengthCalculator.iteratePopulationUpdate(this);
        openQueueLengthCalculator.iteratePopulationUpdate(this);
    }

    private void updateUtilAndQueueMapForPopulation(MVAPopulation population) {
        queueLengthMap.put(population, new double[stations][numClosedClasses + numOpenClasses]);
        utilizationMap.put(population, new double[stations][numClosedClasses]);
    }

    private void initializeQueueLengthAndUtilMap() {
        queueLengthMap = new HashMap<>();
        utilizationMap = new HashMap<>();
        addEmptyPopulationValuesToMaps();
    }

    private void addEmptyPopulationValuesToMaps() {
        // Sets it so when population is all zeroes, the queue length & util is 0
        MVAPopulation emptyPop = new MVAPopulation(new int[numClosedClasses]);
        for (int station = 0; station < stations; station++) {
            queueLengthMap.put(emptyPop, new double[stations][numClosedClasses + numOpenClasses]);
            utilizationMap.put(emptyPop, new double[stations][numClosedClasses]);
        }
    }

    private void calculateThroughputPerStation() {
        opFinalThroughput = new double[stations][numOpenClasses];
        clFinalThroughput = new double[stations][numClosedClasses];

        for (int station = 0; station < stations; station++) {
            for (int opClass = 0; opClass < numOpenClasses; opClass++) {
                // Open arrival rate = throughput
                opFinalThroughput[station][opClass] = opArrivalRate[opClass] * visits[station][opClass];
            }

            for (int clClass = 0; clClass < numClosedClasses; clClass++) {
                // Forced Flow Law
                clFinalThroughput[station][clClass] = clThroughput[clClass] * visits[station][clClass];
            }
        }
    }

    /**
     * Converts the op and cl arrays back into the main
     * arrays in the correct order
     */
    private void mapSplitArraysBackToNonSplitArrays() {
        for (int station = 0; station < stations; station++) {
            for (int opClass = 0; opClass < numOpenClasses; opClass++) {
                throughput[station][opInternalIndexToOriginal[opClass]] = opFinalThroughput[station][opClass];
                utilization[station][opInternalIndexToOriginal[opClass]] = opUtilization[station][opClass];
                residenceTime[station][opInternalIndexToOriginal[opClass]] = opResidenceTimes[station][opClass];
                queueLen[station][opInternalIndexToOriginal[opClass]] = opQueueLength[station][opClass];
            }

            for (int clClass = 0; clClass < numClosedClasses; clClass++) {
                throughput[station][clInternalIndexToOriginal[clClass]] = clFinalThroughput[station][clClass];
                utilization[station][clInternalIndexToOriginal[clClass]] = clUtilization[station][clClass];
                residenceTime[station][clInternalIndexToOriginal[clClass]] = clResidenceTimes[station][clClass];
                queueLen[station][clInternalIndexToOriginal[clClass]] = clQueueLength[station][clClass];
            }
        }
    }

    private void setFinalQueueLengthAndUtilization() {
        // These two values are stored in maps, this function is taking them out of said maps
        setFinalQueueLengths();
        setFinalUtilization();
    }

    private void setFinalUtilization() {
        // clPopulation = final population
        clUtilization = new double[stations][numClosedClasses];
        System.arraycopy(utilizationMap.get(new MVAPopulation(clPopulation)),
                0, clUtilization, 0, stations);
    }

    private void setFinalQueueLengths() {
        // clPopulation = the last population used in the for loop in the solve method
        double[][] allQueueLengths = queueLengthMap.get(new MVAPopulation(clPopulation));
        opQueueLength = new double[stations][numOpenClasses];
        clQueueLength = new double[stations][numClosedClasses];
        for (int station = 0; station < stations; station++) {
            System.arraycopy(allQueueLengths[station], numClosedClasses, opQueueLength[station], 0, numOpenClasses);
            System.arraycopy(allQueueLengths[station], 0, clQueueLength[station], 0, numClosedClasses);
        }
    }


    private void calculateQueueLengthForOpenClasses(MVAPopulation population, int numCustomers) {
        double[][] queueLength = openQueueLengthCalculator.calculatePerformanceValue(population, numCustomers, type);
        for (int station = 0; station < stations; station++) {
            // Open classes are stored after closed classes
            System.arraycopy(queueLength[station], 0,
                    queueLengthMap.get(population)[station], numClosedClasses,
                    numOpenClasses);
        }
    }

    private void calculateQueueLengthForClosedClasses(MVAPopulation population, int numCustomers, int clClass) {
        for (int station = 0; station < stations; station++) {
            queueLengthMap.get(population)[station][clClass] = closedQueueLengthCalculator.getQueueLengths(population, numCustomers, station, clClass);
        }
    }

    private void initialiseCalculators() {
        closedResidenceCalculator.initialise(this);
        throughputCalculator.initialise(this);
        closedQueueLengthCalculator.initialise(this);
        openQueueLengthCalculator.initialise(this);
    }

    private void updateClosedUtilizationMap(MVAPopulation population, int clClass) {
        for (int station = 0; station < stations; station++) {
            if (type[station] == SolverMulti.LD) {
                // TODO
            } else {
                // Util = throughput * servTime for non-load dependent stations
                utilizationMap.get(population)[station][clClass] = visits[station][clClass] * clThroughput[clClass] * clServTime[station][clClass][0];
            }
        }
    }

    private void calculateThroughputForClosedClasses(MVAPopulation population, int numCustomers, int clas) {
        clThroughput[clas] = throughputCalculator.getClassThroughput(population, numCustomers, clas);
    }

    private void calculateResidenceTimesForClosedClasses(MVAPopulation population, int numCustomers, int clClass) {
        double[] residencesPerStation = closedResidenceCalculator.calculatePerformanceValue(population, numCustomers, clClass, type);
        for (int station = 0; station < stations; station++) {
            clResidenceTimes[station][clClass] = residencesPerStation[station];
        }
    }

    private void calculateUtilizationsForOpenClasses() {
        if (numOpenClasses == 0) {
            return;
        }
        for (int station = 0; station < stations; station++) {
            opUtilizationSums[station] = 0;
            for (int clas = 0; clas < numOpenClasses; clas++) {
                // Assumes not LD service time (I think)
                opUtilization[station][clas] = opServTime[station][clas][0] * opArrivalRate[clas] * opVisits[station][clas];
                opUtilizationSums[station] += opUtilization[station][clas];
            }
        }
    }

    @Override
    public boolean hasSufficientProcessingCapacity() {
        if (numOpenClasses == 0) {
            return true;
        }
        return ProcessingCapacityChecker.openHasSufficientProcessingCapacity(
                type, opVisits, opServTime, opArrivalRate);
    }

    // Getters

    public double[] getOpUtilizationSums() {
        return opUtilizationSums;
    }

    public double[][] getOpUtilization() {
        return opUtilization;
    }

    public double[][] getOpVisits() {
        return opVisits;
    }

    public double[][] getClVisits() {
        return clVisits;
    }

    public double[][][] getOpServTime() {
        return opServTime;
    }

    public double[][][] getClServTime() {
        return clServTime;
    }

    public double[][] getClResidenceTimes() {
        return clResidenceTimes;
    }

    public double[] getClThroughput() {
        return clThroughput;
    }

    public int[] getOpPriorities() {
        return opPriorities;
    }

    public int[] getClPriorities() {
        return clPriorities;
    }

    public Map<MVAPopulation, double[][]> getQueueLengthMap() {
        return queueLengthMap;
    }

    public Map<MVAPopulation, double[][]> getUtilizationMap() {
        return utilizationMap;
    }

    public double[] getOpArrivalRate() {
        return opArrivalRate;
    }

    public int getNumOpenClasses() {
        return numOpenClasses;
    }

    public int getNumClosedClasses() {
        return numClosedClasses;
    }
}
