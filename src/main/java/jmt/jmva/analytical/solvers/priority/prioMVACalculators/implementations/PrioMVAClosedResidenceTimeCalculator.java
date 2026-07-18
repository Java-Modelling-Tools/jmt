package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations;

import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.PerformanceValuePerStationPerClassCalculator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.PrioResidenceTimeHelper;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.UtilizationCalculator;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations.utilisations.CLPrioMVAUtilCalculator;

import java.util.Map;

public class PrioMVAClosedResidenceTimeCalculator extends PerformanceValuePerStationPerClassCalculator {

    /** Used to calculate the rho' in the formula as
     * the correct utilisation is not known */
    private final UtilizationCalculator utilizationCalculator;

    private int numClosedClasses;
    private double[][][] clServTime;
    private double[] opUtilizationSums;
    private Map<MVAPopulation, double[][]> queueLengthMap;

    private PrioResidenceTimeHelper residenceHelper;

    public PrioMVAClosedResidenceTimeCalculator(UtilizationCalculator utilizationCalculator) {
        this.utilizationCalculator = utilizationCalculator;
    }

    public PrioMVAClosedResidenceTimeCalculator() {
        this(new CLPrioMVAUtilCalculator());
    }

    @Override
    public double computeForLDStation(MVAPopulation population, int totCustomers, int station, int clas) {
        // TODO
        throwStationNotImplementedError("Load Dependent");
        // Unreachable
        return 0.0;
    }

    @Override
    public double computeForLIStation(MVAPopulation population, int numCustomers, int station, int clas) {
        if (population.emptyClass(clas)) {
            return 0.0;
        }
        population.decClassPopulation(clas);
        double closedQueueLengths = 0;
        double[] queueLengths = queueLengthMap.get(population)[station];
        for (int clClass = 0; clClass < numClosedClasses; clClass++) {
           closedQueueLengths += queueLengths[clClass];
        }
        population.incClassPopulation(clas);
        return clServTime[station][clas][0] / (1.0 - opUtilizationSums[station]) * (1.0 + closedQueueLengths);
    }

    @Override
    public double computeForDelayStation(MVAPopulation population, int numCustomers, int station, int clas) {
        if (population.emptyClass(clas)) {
            return 0.0;
        }
        return clServTime[station][clas][0];
    }

    @Override
    public double computeForPRSStation(MVAPopulation population, int numCustomers, int station, int clas) {
        if (population.emptyClass(clas)) {
            return 0.0;
        }
        double higherResidenceTime = getHigherPriorityResidenceTimeClosedClass(population, station, clas);
        double higherUtil = residenceHelper.getOneMinusHigherUtil(population, numCustomers, station, clas, SolverMulti.CLOSED_CLASS);
        return (clServTime[station][clas][0] + higherResidenceTime) / higherUtil;
    }

    @Override
    public double computeForHOLStation(MVAPopulation population, int numCustomers, int station, int clas) {
        if (population.emptyClass(clas)) {
            return 0.0;
        }
        double higherResidenceTime = getHigherPriorityResidenceTimeClosedClass(population, station, clas);
        double lowerServerUsage = getLowerPriorityServerUsage(population, station, clas);
        double higherUtil = residenceHelper.getOneMinusHigherUtil(population, numCustomers, station, clas, SolverMulti.CLOSED_CLASS);
        return clServTime[station][clas][0] + ((higherResidenceTime + lowerServerUsage) / higherUtil);
    }

    private double getHigherPriorityResidenceTimeClosedClass(MVAPopulation population, int station, int clClass) {
        population.decClassPopulation(clClass);
        double higherResidenceTime = residenceHelper.getHigherAndEqualPriorityResidenceTime(population, station, clClass, SolverMulti.CLOSED_CLASS);
        population.incClassPopulation(clClass);
        return higherResidenceTime;
    }

    private double getLowerPriorityServerUsage(MVAPopulation population, int station, int clClass) {
        population.decClassPopulation(clClass);
        double lowerServerUsage = residenceHelper.getLowerPriorityServerUsage(population, station, clClass, SolverMulti.CLOSED_CLASS);
        population.incClassPopulation(clClass);
        return lowerServerUsage;
    }

    @Override
    public void initialise(SolverMultiMixedPrioMVA solver) {
        this.numClosedClasses = solver.getNumClosedClasses();
        this.clServTime = solver.getClServTime();
        this.queueLengthMap = solver.getQueueLengthMap();
        this.opUtilizationSums = solver.getOpUtilizationSums();

        this.residenceHelper = new PrioResidenceTimeHelper(solver.getOpPriorities(), solver.getClPriorities(),
                solver.getOpServTime(), clServTime, queueLengthMap, solver.getUtilizationMap(), utilizationCalculator,
                solver.getOpUtilization());

        this.utilizationCalculator.initialise(solver);
    }

    @Override
    public void iterateClassUpdate(SolverMultiMixedPrioMVA solver) {
        // "this" does not need to update any values each iteration as the recursive values required
        // are accessible via maps however Util Calc may need to
        this.utilizationCalculator.iterateClassUpdate(solver);
    }

    @Override
    public void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver) {
        this.utilizationCalculator.iteratePopulationUpdate(solver);
    }
}
