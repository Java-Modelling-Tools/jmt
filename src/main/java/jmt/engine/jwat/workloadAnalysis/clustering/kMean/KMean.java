package jmt.engine.jwat.workloadAnalysis.clustering.kMean;

import jmt.engine.jwat.MatrixObservations;
import jmt.engine.jwat.workloadAnalysis.clustering.Clustering;
import jmt.engine.jwat.workloadAnalysis.clustering.ClusteringInfos;
import jmt.engine.jwat.workloadAnalysis.clustering.kMean.KMeanClusteringEngine.TempClusterStatistics;
import jmt.gui.jwat.JWATConstants;

public class KMean implements Clustering, JWATConstants {

	private ClusteringInfosKMean[] results;
	private short[][] clustAssign = null;
	//private Vector clusterAssignemnt=null; //Vector of Vector
	private int[] varSel;

	public KMean(int numClust, int[] varSel) {
		this.varSel = varSel;
		results = new ClusteringInfosKMean[numClust];
		clustAssign = new short[numClust][];
	}

	public KMean(ClusteringInfosKMean[] res, int[] varSel, short clustAssign[][]) {
		this.varSel = varSel;
		results = res;
		this.clustAssign = clustAssign;
	}

	public String getName() {
		return "k-Means";
	}

	public int getNumCluster() {
		return results.length;
	}

	public ClusteringInfos getClusteringInfos(int numCluster) {
		return results[numCluster];
	}

	public void calcClusteringInfo(int numCluster, TempClusterStatistics[][] sum, short[] cAss, MatrixObservations m) {
		//clusterAssignemnt.add(clusAssign);
		clustAssign[numCluster] = cAss;
		results[numCluster] = new ClusteringInfosKMean(numCluster, m.getNumVariables());
		if (numCluster != 0) {
			results[numCluster].Output(varSel, sum, cAss, m, results[numCluster - 1].passw);
		} else {
			results[numCluster].Output(varSel, sum, cAss, m, 0);
		}
	}

	public void setRatio(int endClust) {
		if (endClust > 2) {
//Show cluster summary
/* Calculate an index showing the validity of a clustering */
			for (int i = 1; i < (endClust - 1); i++) {
				if (results[i + 1].omsr != 0) {
					results[i].ratio = results[i].omsr / results[i + 1].omsr;
				}
			}
//Calculate if there are valid clusters
			for (int i = 1; i < (endClust - 1); i++) {
				if (results[i].ratio >= 1.5) {
					if (results[i].omsr > 10) {
						results[i].isGoodCluster = 1;
					}
				}
			}
			results[endClust - 1].ratio = 0;
		}
	}

	/*
	public Vector getClusteringAssignment()
	{
		return clusterAssignemnt;
	}
	*/
	/*
	public void setClusteringAssignment(Vector clusterAssign)
	{
		clusterAssignemnt=clusterAssign;
	}
	*/
	public int getClusteringType() {
		return KMEANS;
	}

//UPDATE 02/11/2006: + array of assignments
// + assignment array getter
// + clustered variable getter
	public short[][] getAsseg() {
		return clustAssign;
	}

	public int[] getVarClust() {
		return varSel;
	}

	public ClusteringInfosKMean[] getResults() {
		return results;
	}
}
