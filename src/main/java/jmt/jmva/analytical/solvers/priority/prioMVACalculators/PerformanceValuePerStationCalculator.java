package jmt.jmva.analytical.solvers.priority.prioMVACalculators;

import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;


/**
 * A class to calculate a metric based off of the station type
 * Calculates the value for all classes at the same time
 */

public abstract class PerformanceValuePerStationCalculator {

    public double[][] calculatePerformanceValue(MVAPopulation population, int numCustomers, int[] type){
        int stations = type.length;

        double[][] performanceValue = new double[stations][];
        for (int station = 0; station < stations; station++) {
            if (type[station] == SolverMulti.LD) {
                performanceValue[station] = computeForLDStation(population, numCustomers, station);
            } else if (type[station] == SolverMulti.LI) {
                performanceValue[station] = computeForLIStation(population, numCustomers, station);
            } else if (type[station] == SolverMulti.DELAY) {
                performanceValue[station] = computeForDelayStation(population, numCustomers, station);
            } else if (type[station] == SolverMulti.PRS) {
                performanceValue[station] = computeForPRSStation(population, numCustomers, station);
            } else if (type[station] == SolverMulti.HOL) {
                performanceValue[station] = computeForHOLStation(population, numCustomers, station);
            }
        }

        return performanceValue;
    }


    protected void throwStationNotImplementedError(String stationType) {
        throw new RuntimeException(String.format("%s Stations are not implemented for this calculator", stationType));
    }

    public abstract double[] computeForLDStation(MVAPopulation population, int numCustomers, int station);

    public abstract double[] computeForLIStation(MVAPopulation population, int numCustomers, int station);

    public abstract double[] computeForDelayStation(MVAPopulation population, int numCustomers, int station);

    public abstract double[] computeForPRSStation(MVAPopulation population, int numCustomers, int station);

    public abstract double[] computeForHOLStation(MVAPopulation population, int numCustomers, int station);

    public abstract void initialise(SolverMultiMixedPrioMVA solver);

    public abstract void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver);
}
