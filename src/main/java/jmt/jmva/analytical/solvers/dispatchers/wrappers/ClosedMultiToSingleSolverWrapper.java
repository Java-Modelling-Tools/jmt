package jmt.jmva.analytical.solvers.dispatchers.wrappers;

import jmt.common.exception.SolverException;
import jmt.framework.data.ArrayUtils;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.Solver;
import jmt.jmva.analytical.solvers.SolverAlgorithm;
import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.dispatchers.SolverMultiAlgorithmSelector;
import jmt.jmva.analytical.solvers.exceptions.InternalErrorException;

/**
 * Wrapper class for single-class queueing models
 * Uses multi-class algorithm classes for solving the models
 * For iterative algorithms use IterativeClosedMultiToSingleSolverWrapper
 *
 * @author Abhimanyu Chugh
 * Modified by Ben Pahnke
 */

public class ClosedMultiToSingleSolverWrapper extends Solver {
    // the multi-class algorithm solver to use
    protected SolverAlgorithm algorithm;
    protected SolverMulti solver;

    public static final boolean DEBUG = false;

    // number of customers or jobs in the system
    protected int customers = 0;

    protected double[][][] serviceTimes;
    protected double[][] custVisits;


    public ClosedMultiToSingleSolverWrapper(SolverAlgorithm alg, ExactModel model) throws SolverException, InternalErrorException {
        this.customers = (int) model.getClassData()[0];
        this.stations = model.getStations();
        this.algorithm = alg;

        // Finds MultiSolver implementation and sets it to solver
        SolverMultiAlgorithmSelector selector = new SolverMultiAlgorithmSelector(model);
        solver = selector.selectClosedSolver(algorithm);

        name = new String[stations];
        type = new int[stations];
        // one service time for each possible population (from 1 to customers)
        // position 0 is used for LI stations
        servTime = new double[stations][customers + 1];
        visits = new double[stations];

        throughput = new double[stations];
        queueLen = new double[stations];
        utilization = new double[stations];
        residenceTime = new double[stations];
    }

    @Override
    public boolean input(String[] n, int[] t, double[][] s, double[] v) {
        serviceTimes = new double[stations][1][s[0].length];
        custVisits = new double[stations][1];
        if (!super.input(n, t, s, v)) {
            return false;
        }
        for (int i = 0; i < stations; i++) {
            for (int j = 0; j < s[0].length; j++) {
                serviceTimes[i][0][j] = s[i][j];
            }
            custVisits[i][0] = visits[i];
        }
        return solver.input(n, t, serviceTimes, custVisits);
    }

    public void solve() {
        solver.solve();

        totUser = customers;
        totRespTime = solver.getTotResTime();
        totThroughput = solver.getTotThroughput();

        queueLen = ArrayUtils.extract1(solver.getQueueLen(), 0);
        throughput = ArrayUtils.extract1(solver.getThroughput(), 0);
        residenceTime = ArrayUtils.extract1(solver.getResTime(), 0);
        utilization = ArrayUtils.extract1(solver.getUtilization(), 0);
    }

    public boolean hasSufficientProcessingCapacity() {
        return solver.hasSufficientProcessingCapacity();
    }

}
