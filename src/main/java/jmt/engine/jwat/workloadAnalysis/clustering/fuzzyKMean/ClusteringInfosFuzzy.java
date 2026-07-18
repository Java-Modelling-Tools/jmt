package jmt.engine.jwat.workloadAnalysis.clustering.fuzzyKMean;

import jmt.engine.jwat.MatrixObservations;
import jmt.engine.jwat.Observation;
import jmt.engine.jwat.workloadAnalysis.clustering.ClusteringInfos;

public class ClusteringInfosFuzzy implements ClusteringInfos {

	public ClusterInfoFuzzy[] infoCluster;
	private double[][] assignment = null;
	private double err;
	private int numCluster;
	private short assignedToClust[];

	public int[] numElem;
	public double[] percent; // percentuale sul totale

	public String centers; // contiene i centers dei cluster
	public String log; // informazioni generali

	public ClusteringInfosFuzzy(double[][] ass, int nCluster) {
		assignment = ass;
		numCluster = nCluster;
		err = -1;
		percent = new double[nCluster + 1];
		assignedToClust = new short[assignment[0].length];
		infoCluster = new ClusterInfoFuzzy[nCluster];
	}

	public int getNumClusters() {
		return numCluster;
	}

	public ClusterInfoFuzzy getCluster(int index) {
		if (err != -1 && index < numCluster) {
			return infoCluster[index];
		}
		return null;
	}

	public short[] getAssignment() {
		return assignedToClust;
	}

	public void setError(MatrixObservations m, double err) {
		if (this.err == err) {
			return;
		}
		this.err = err;
		int i, j;
		boolean assigned;
		numElem = new int[numCluster + 1];
		//Create InfoCluster
		for (i = 0; i < assignment[0].length; i++) {
			assigned = false;
			for (j = 0; j < assignment.length; j++) {
				if (assignment[j][i] >= err) {
					numElem[j]++;
					if (!assigned) {
						assignedToClust[i] = (short) (j + 2);
					} else {
						assignedToClust[i] = 1;
					}
					assigned = true;
				}
			}
			if (!assigned) {
				assignedToClust[i] = 0;
				numElem[numCluster]++;
			}
		}
		for (i = 0; i < numCluster + 1; i++) {
			percent[i] = (double) numElem[i] / assignment[0].length;
		}
		InitInfo(m.getNumVariables());
		DoStat(m);
	}

	public double getError() {
		return err;
	}

	private void InitInfo(int nVar) {
		for (int i = 0; i < infoCluster.length; i++) {
			infoCluster[i] = new ClusterInfoFuzzy(nVar);
		}
	}

	private void DoStat(MatrixObservations m) {
		int nclus;

		Observation[] currObs = m.getListObs(); //Current value of each variable
		for (int count = 0; count < m.getNumOfObs(); count++) {

			nclus = assignedToClust[count];
			if (nclus < 2) {
				continue;
			}
			nclus -= 2;
			infoCluster[nclus].numObs++;

//Calculate sum, squared sum. etc.
			for (int i = 0; i < m.getNumVariables(); i++) {
				if (currObs[count].getIndex(i) != 0) {
					infoCluster[nclus].statClust[i].iNotZr++;
				}
				if (infoCluster[nclus].numObs == 1) {
					infoCluster[nclus].statClust[i].dMaxObs = currObs[count].getIndex(i);
					infoCluster[nclus].statClust[i].dMinObs = currObs[count].getIndex(i);
				} else {
					if (infoCluster[nclus].statClust[i].dMaxObs < currObs[count].getIndex(i)) {
						infoCluster[nclus].statClust[i].dMaxObs = currObs[count].getIndex(i);
					}
					if (infoCluster[nclus].statClust[i].dMinObs > currObs[count].getIndex(i)) {
						infoCluster[nclus].statClust[i].dMinObs = currObs[count].getIndex(i);
					}
				}
				infoCluster[nclus].statClust[i].dSum += currObs[count].getIndex(i);
				infoCluster[nclus].statClust[i].dSQuad += Math.pow(currObs[count].getIndex(i), 2);
				infoCluster[nclus].statClust[i].dSThird += Math.pow(currObs[count].getIndex(i), 3);
				infoCluster[nclus].statClust[i].dSFourth += Math.pow(currObs[count].getIndex(i), 4);

			}
		}

		double sum_per_var;
//Calculate percentage of variable used in the cluster
		for (int i = 0; i < m.getNumVariables(); i++) {
			sum_per_var = 0;
			for (int l = 0; l < numCluster; l++) {
				sum_per_var += infoCluster[l].statClust[i].dSum;
			}

			for (int l = 0; l < numCluster; l++) {

				infoCluster[l].percVar[i] = infoCluster[l].statClust[i].dSum / sum_per_var;
			}
		}
//Port of stat2cl
//Calculate mean, variance, etc,etc

		for (int l = 0; l < numCluster; l++) {

			for (int i = 0; i < m.getNumVariables(); i++) {
				infoCluster[l].statClust[i].dRange = infoCluster[l].statClust[i].dMaxObs - infoCluster[l].statClust[i].dMinObs;
				infoCluster[l].statClust[i].dMean = infoCluster[l].statClust[i].dSum / infoCluster[l].numObs;

				if (infoCluster[l].numObs != 1) {
					infoCluster[l].statClust[i].dPerc5 = Math.pow(infoCluster[l].statClust[i].dMean, 2);
					infoCluster[l].statClust[i].dPerc6 = Math.pow(infoCluster[l].statClust[i].dMean, 3);
					infoCluster[l].statClust[i].dPerc7 = Math.pow(infoCluster[l].statClust[i].dMean, 4);

					infoCluster[l].statClust[i].dVarnc = (infoCluster[l].statClust[i].dSQuad - infoCluster[l].numObs
							* infoCluster[l].statClust[i].dPerc5)
							/ (infoCluster[l].numObs - 1);

					if (infoCluster[l].statClust[i].dVarnc != 0) {
						infoCluster[l].statClust[i].dStdDv = Math.sqrt(infoCluster[l].statClust[i].dVarnc);
						infoCluster[l].statClust[i].dStdEr = infoCluster[l].statClust[i].dStdDv / Math.sqrt(infoCluster[l].numObs);

						infoCluster[l].statClust[i].dSkewn = infoCluster[l].statClust[i].dSThird - 3 * infoCluster[l].statClust[i].dMean
								* infoCluster[l].statClust[i].dSQuad + 3 * infoCluster[l].statClust[i].dPerc5 * infoCluster[l].statClust[i].dSum;
						infoCluster[l].statClust[i].dSkewn /= infoCluster[l].numObs;
						infoCluster[l].statClust[i].dSkewn -= infoCluster[l].statClust[i].dPerc6;
						infoCluster[l].statClust[i].dSkewn /= Math.pow(infoCluster[l].statClust[i].dVarnc, 1.5);

						infoCluster[l].statClust[i].dKurto = infoCluster[l].statClust[i].dSFourth - 4 * infoCluster[l].statClust[i].dMean
								* infoCluster[l].statClust[i].dSThird + 6 * infoCluster[l].statClust[i].dPerc5 * infoCluster[l].statClust[i].dSQuad
								- 4 * infoCluster[l].statClust[i].dPerc6 * infoCluster[l].statClust[i].dSum;
						infoCluster[l].statClust[i].dKurto /= infoCluster[l].numObs;
						infoCluster[l].statClust[i].dKurto += infoCluster[l].statClust[i].dPerc7;
						infoCluster[l].statClust[i].dKurto /= Math.pow(infoCluster[l].statClust[i].dVarnc, 2);
						infoCluster[l].statClust[i].dKurto -= 3;
					}
				}
			}
		}
	}

}
