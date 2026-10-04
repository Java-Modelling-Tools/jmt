package solvers.dispatchers.wrappers;

import analytical.ExactModel;
import jmt.common.exception.SolverException;
import solvers.SolverAlgorithm;
import solvers.SolverMultiIterative;
import solvers.exceptions.InternalErrorException;

/**
 * Wrapper class for single-class queueing models
 * Uses multi-class algorithm to solve the single-class models
 *
 * @author by Ben Pahnke
 */

public class IterativeClosedMultiToSingleSolverWrapper extends ClosedMultiToSingleSolverWrapper {

    protected double tolerance;

    public IterativeClosedMultiToSingleSolverWrapper(SolverAlgorithm alg, ExactModel model, double tol ) throws SolverException, InternalErrorException {
        super(alg, model);
        this.tolerance = tol;
        ((SolverMultiIterative) solver).setTolerance(tolerance);
    }

    public void setMaxIterations(int maxIterations) {
        ((SolverMultiIterative) solver).setMaxIterations(maxIterations);
    }

    // get algorithm iterations
    public int getIterations() {
        return ((SolverMultiIterative) solver).getIterations();
    }

}
