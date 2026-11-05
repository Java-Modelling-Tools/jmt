package jwat.workloadAnalysis.clustering;

/**
 * Clustering algorithm of JWAT for identifying similarities in the workload data.
 */
/**
 * Clustering algorithm of JWAT for identifying similarities in the workload data.
 */
/**
 * Clustering algorithm of JWAT for identifying similarities in the workload data.
 */
/**
 * Clustering algorithm of JWAT for identifying similarities in the workload data.
 */
public interface Clustering {
	public int getNumCluster();

	public String getName();

	public ClusteringInfos getClusteringInfos(int numCluster);

	public int getClusteringType(); //UPDATE 03/11/06

	public int[] getVarClust();
}
