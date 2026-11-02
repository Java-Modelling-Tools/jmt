package solvers.priority.prioMVACalculators;

import solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import solvers.utilities.MVAPopulation;

import java.util.Map;

/**
 * Base implementation of the priority MVA utilization calculators.
 */
public abstract class BasePrioMVAUtilCalculator implements UtilizationCalculator {

    protected Map<MVAPopulation, double[][]> queueLengthMap;
    protected Map<MVAPopulation, double[][]> utilMap;

    protected double[][] opUtilization;

    /**
     * Returns the open utilisation.
     * @return The utilization as it's pre-determined by the arrival rate of the open class
     * @param population the population
     * @param numCustomers the num customers
     * @param station the station
     * @param clas the clas
     */
    @Override
    public double getOpenUtilisation(MVAPopulation population, int numCustomers, int station, int clas) {
        return opUtilization[station][clas];
    }

    @Override
    public double getClosedUtilisation(MVAPopulation population, int numCustomers, int station, int clas) {
        if (population.emptyClass(clas)) {
            return 0.0;
        }
        return getNonEmptyClosedUtilisation(population, numCustomers, station, clas);
    }

    public abstract double getNonEmptyClosedUtilisation(MVAPopulation population, int numCustomers, int station, int clas);

    @Override
    public void initialise(SolverMultiMixedPrioMVA solver) {
        this.queueLengthMap = solver.getQueueLengthMap();
        this.utilMap = solver.getUtilizationMap();
        this.opUtilization = solver.getOpUtilization();
    }

    @Override
    public void iterateClassUpdate(SolverMultiMixedPrioMVA solver) {

    }

    @Override
    public void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver) {

    }
}
