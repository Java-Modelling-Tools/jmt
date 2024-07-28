package jmt.jmva.analytical.solvers.priority.prioMVACalculators.implementations;

import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiMixedPrioMVA;
import jmt.jmva.analytical.solvers.priority.prioMVACalculators.ThroughputCalculator;
import jmt.jmva.analytical.solvers.utilities.MVAPopulation;

public class PrioMVAThroughputCalculator implements ThroughputCalculator {

    private double[][] visits;
    private double[][] residenceTime;

    @Override
    public double getClassThroughput(MVAPopulation population, int numCustomers, int clas) {
        if (population.emptyClass(clas)) {
            return 0;
        }

        double sumVisitsResidenceTime = 0;
        for (int station = 0; station < visits.length; station++) {
            sumVisitsResidenceTime += visits[station][clas] * residenceTime[station][clas];
        }
        if (sumVisitsResidenceTime != 0) {
            return ((double) population.getPopulation()[clas]) / sumVisitsResidenceTime;
        } else {
            return 0;
        }
    }

    @Override
    public void initialise(SolverMultiMixedPrioMVA solver) {
        this.visits = solver.getClVisits();
    }

    @Override
    public void iterateClassUpdate(SolverMultiMixedPrioMVA solver) {
    }

    @Override
    public void iteratePopulationUpdate(SolverMultiMixedPrioMVA solver) {
        this.residenceTime = solver.getClResidenceTimes();
    }
}
