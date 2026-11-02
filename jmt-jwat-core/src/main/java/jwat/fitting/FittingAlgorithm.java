package jwat.fitting;

/**
 * Distribution fitting algorithm of JWAT, applied to the workload data to parameterize the models.
 */
/**
 * Distribution fitting algorithm of JWAT, applied to the workload data to parameterize the models.
 */
/**
 * Distribution fitting algorithm of JWAT, applied to the workload data to parameterize the models.
 */
/**
 * Distribution fitting algorithm of JWAT, applied to the workload data to parameterize the models.
 */
public interface FittingAlgorithm {
	public boolean isFitted();
	public double[] generateQQPlot();
	public boolean isLastRunFitted();
	public double[] getEstimatedParameters();
}
