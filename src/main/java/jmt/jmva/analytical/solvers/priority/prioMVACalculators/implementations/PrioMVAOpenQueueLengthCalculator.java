package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations;

import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.PerformanceValuePerStationCalculator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.UtilizationCalculator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations.CLPrioMVAUtilCalculator;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.PrioResidenceTimeHelper;

import java.util.Map;

/**
 * Class to calculate the open queue lengths during PrioMVA
 */
public class PrioMVAOpenQueueLengthCalculator extends PerformanceValuePerStationCalculator {

    private final UtilizationCalculator utilizationCalculator;

    private int numOpenClasses;
    private int numClosedClasses;

    private double[][] opUtilization;
    private double[] opUtilizationSums;
    private double[] opArrivalRates;
    private double[][][] opServTime;
    private double[][] opVisits;

    private Map<MVAPopulation, double[][]> queueLengthMap;

    private PrioResidenceTimeHelper residenceHelper;

    public PrioMVAOpenQueueLengthCalculator(UtilizationCalculator utilizationCalculator) {
        this.utilizationCalculator = utilizationCalculator;
    }

    public PrioMVAOpenQueueLengthCalculator() {
       this(new CLPrioMVAUtilCalculator());
    }

    @Override
    public double[] computeForLDStation(MVAPopulation population, int numCustomers, int station) {
        throwStationNotImplementedError("Load Dependent");
        // Unreachable
        return null;
    }

    @Override
    public double[] computeForLIStation(MVAPopulation population, int numCustomers, int station) {
        double[] queueLengths = new double[numOpenClasses];
        double closedQueueLengthSum = 0;
        for (int clClas = 0; clClas < numClosedClasses; clClas++) {
            closedQueueLengthSum += queueLengthMap.get(population)[station][clClas];
        }
        double oneMinusUtilSum = 1.0 - opUtilizationSums[station];
        for (int clas = 0; clas < numOpenClasses; clas++) {
            queueLengths[clas] = (opUtilization[station][clas] * (1.0 + closedQueueLengthSum)) / oneMinusUtilSum;
        }
        return queueLengths;
    }

    @Override
    public double[] computeForDelayStation(MVAPopulation population, int numCustomers, int station) {
        double[] queueLengths = new double[numOpenClasses];
        System.arraycopy(opUtilization[station], 0, queueLengths, 0, numOpenClasses);
        return queueLengths;
    }

    @Override
    public double[] computeForPRSStation(MVAPopulation population, int numCustomers, int station) {
        double[] openResidenceTime = calculateOpenResidenceTimePRS(population, numCustomers, station);
        return calculatePriorityQueueLength(station, openResidenceTime);
    }

    @Override
    public double[] computeForHOLStation(MVAPopulation population, int numCustomers, int station) {
        double[] openResidenceTime = calculateOpenResidenceTimeHOL(population, numCustomers, station);
        return calculatePriorityQueueLength(station, openResidenceTime);
    }

    private double[] calculatePriorityQueueLength(int station, double[] openResidenceTime) {
        double[] queueLength = new double[numOpenClasses];
        for (int opClass = 0; opClass < numOpenClasses; opClass++) {
            queueLength[opClass] = opArrivalRates[opClass] * opVisits[station][opClass] * openResidenceTime[opClass];
        }
        return queueLength;
    }

    private double[] calculateOpenResidenceTimePRS(MVAPopulation population, int numCustomers, int station) {
        double[] openResidenceTimes = new double[numOpenClasses];
        for (int opClass = 0; opClass < numOpenClasses; opClass++) {
            double higherResidenceTime = residenceHelper.getHigherPriorityResidenceTime(population, station, opClass, SolverMulti.OPEN_CLASS);
            double higherUtil = residenceHelper.getOneMinusHigherAndEqualUtil(population, numCustomers, station, opClass, SolverMulti.OPEN_CLASS);
            openResidenceTimes[opClass] = (opServTime[station][opClass][0] + higherResidenceTime) / higherUtil;
        }
        return openResidenceTimes;
    }

    private double[] calculateOpenResidenceTimeHOL(MVAPopulation population, int numCustomers, int station) {
        double[] openResidenceTimes = new double[numOpenClasses];
        for (int opClass = 0; opClass < numOpenClasses; opClass++) {
            // TODO: Calculate higher and higher & equal util at the same time
            // or cache the results
            double higherUtil = residenceHelper.getOneMinusHigherUtil(population, numCustomers, station, opClass, SolverMulti.OPEN_CLASS);
            double servTimeMultUtil = opServTime[station][opClass][0] * higherUtil;
            double higherResidenceTime = residenceHelper.getHigherPriorityResidenceTime(population, station, opClass, SolverMulti.OPEN_CLASS);
            double lowerServerUsage = residenceHelper.getLowerPriorityServerUsage(population, station, opClass, SolverMulti.OPEN_CLASS);
            double higherAndEqualUtil = residenceHelper.getOneMinusHigherAndEqualUtil(population, numCustomers, station, opClass, SolverMulti.OPEN_CLASS);
            openResidenceTimes[opClass] = (servTimeMultUtil + higherResidenceTime + lowerServerUsage) / higherAndEqualUtil;
        }
        return openResidenceTimes;
    }



    @Override
    public void initialise(SolverMultiMixedPrioMVA solver) {
        this.numOpenClasses = solver.getNumOpenClasses();
        this.numClosedClasses = solver.getNumClosedClasses();

        this.opUtilization = solver.getOpUtilization();
        this.opUtilizationSums = solver.getOpUtilizationSums();
        this.queueLengthMap = solver.getQueueLengthMap();
        this.opArrivalRates = solver.getOpArrivalRate();
        this.opServTime = solver.getOpServTime();
        this.opVisits = solver.getOpVisits();

        this.residenceHelper = new PrioResidenceTimeHelper(solver.getOpPriorities(), solver.getClPriorities(),
                opServTime, solver.getClServTime(), queueLengthMap, solver.getUtilizationMap(), utilizationCalculator,
                solver.getOpUtilization());

        this.utilizationCalculator.initialise(solver);
    }

    @Override
    public void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver) {
        // "this" does not need to update any values each iteration as the recursive values required
        // are accessible via maps however Util Calc may need to
        this.utilizationCalculator.iterateClassUpdate(solver);
    }
}
