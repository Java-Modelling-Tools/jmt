package jmt.jmva.analytical.solvers;

public abstract class SolverMultiIterative extends SolverMulti {

    public static final double DEFAULT_TOLERANCE = Math.pow(10, -7);
    protected double tolerance = DEFAULT_TOLERANCE;
    protected int MAX_ITERATIONS = Integer.MAX_VALUE;
    protected int iterations = 0; // algorithm iterations

    /**
     * @param classes  number of classes.
     * @param stations number of stations.
     */
    public SolverMultiIterative(int classes, int stations) {
        super(classes, stations);
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public void setMaxIterations(int maxIterations) {
        MAX_ITERATIONS = maxIterations;
    }

    public int getIterations() {
        return iterations;
    }

}
