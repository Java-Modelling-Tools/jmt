package jmt.jmva.analytical.solvers.dispatchers;

import jmt.common.exception.InputDataException;
import jmt.common.exception.SolverException;
import jmt.common.exception.UnsupportedModelException;
import jmt.framework.data.ArrayUtils;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.*;
import jmt.jmva.analytical.solvers.dataStructures.BigRational;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.*;

public class SolverMultiDispatcher {

    public static void solveMulti(ExactModel model, int iteration) throws UnsupportedModelException, InputDataException, SolverException {
        SolverAlgorithm algorithmType = model.getAlgorithmType();

        ModelSolverCompatibilityChecker.checkCompatibility(model, model.getClassData(), algorithmType);

        /* init */
        SolverMultiAlgorithmSelector selector = new SolverMultiAlgorithmSelector(model);
        SolverMulti solver = null;
        try {
            if (model.isOpen()) {
                solver = selector.selectOpenSolver(algorithmType);
            } else {
                if (model.isClosed()) {
                    solver = selector.selectClosedSolver(algorithmType);
                } else {
		    if (model.isLd()) {
			   throw new InputDataException("Load-dependent mixed models are presently not supported.");
	                   // solver = selector.selectMixedSolver(algorithmType);
		    } else {
        	            // model is multiclass mixed
	                    solver = selector.selectMixedSolver(algorithmType);
		    }
                }
            }
        } catch (Exception e) {
            DispatcherUtil.fail("Error initializing Multiclass solver", e);
        }

        if (!solver.hasSufficientProcessingCapacity()) {
            throw new InputDataException("One or more resources are in saturation. Decrease arrival rates or service demands.");
        }

        /* solution */
        solver.solve();

        setModelResults(solver, model, algorithmType, iteration);
    }

    private static void setModelResults(SolverMulti solver, ExactModel model, SolverAlgorithm algorithmType, int iteration) {
        int algIterations = getAlgIterations(solver);
        double logNC = calculateLogNC(solver);

        int classes = model.getClasses();
        int stations = model.getStations();

        double[][] ql = ArrayUtils.resize2(solver.getQueueLen(), stations, classes, 0);
        double[][] tp = ArrayUtils.resize2(solver.getThroughput(), stations, classes, 0);
        double[][] rt = ArrayUtils.resize2(solver.getResTime(), stations, classes, 0);
        double[][] util = ArrayUtils.resize2(solver.getUtilization(), stations, classes, 0);

        model.setResults(algorithmType, algIterations, ql, tp, rt, util, logNC, iteration);
    }

    private static int getAlgIterations(SolverMulti solver) {
        int algIterations = 0;
        if (solver instanceof SolverMultiClosedAMVA) {
            algIterations = ((SolverMultiClosedAMVA) solver).getIterations();
        }
        return algIterations;
    }

    private static double calculateLogNC(SolverMulti solver) {
        double logNC = Double.NaN;
        if (solver instanceof SolverMultiClosedRECAL) {
            BigRational G = ((SolverMultiClosedRECAL) solver).qnm.getNormalisingConstant();
            logNC = G.log() - 5 * Math.log(10) * ((SolverMultiClosedRECAL) solver).qnm.N.sum();
        } else if (solver instanceof SolverMultiClosedCoMoM) {
            BigRational G = ((SolverMultiClosedCoMoM) solver).qnm.getNormalisingConstant();
            logNC = G.log() - 5 * Math.log(10) * ((SolverMultiClosedCoMoM) solver).qnm.N.sum();
        } else if (solver instanceof SolverMultiClosedMonteCarloLogistic) {
            BigRational G = ((SolverMultiClosedMonteCarloLogistic) solver).qnm.getNormalisingConstant();
            logNC = G.log();
        }
        return logNC;
    }
}
