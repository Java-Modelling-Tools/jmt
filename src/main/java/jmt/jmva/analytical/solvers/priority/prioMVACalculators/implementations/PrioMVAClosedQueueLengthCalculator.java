package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations;

import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.QueueLengthCalculator;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;

public class PrioMVAClosedQueueLengthCalculator implements QueueLengthCalculator {

    private double[][] clVisits;
    private double[][] clResidenceTime;
    private double[] clThroughput;

    @Override
    public double getQueueLengths(MVAPopulation population, int numCustomers, int station, int clClass) {
        return clThroughput[clClass] * clResidenceTime[station][clClass] * clVisits[station][clClass];
    }

    @Override
    public void initialise(SolverMultiMixedPrioMVA solver) {
        this.clVisits = solver.getClVisits();
    }

    @Override
    public void iterateClassUpdate(SolverMultiMixedPrioMVA solver) {
    }

    @Override
    public void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver) {
        this.clResidenceTime = solver.getClResidenceTimes();
        this.clThroughput = solver.getClThroughput();
    }
}
