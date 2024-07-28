package jmt.engine.jwat.workloadAnalysis.clustering.kMean;

import jmt.engine.jwat.MatrixObservations;
import jmt.engine.jwat.Observation;
import jmt.engine.jwat.workloadAnalysis.clustering.ClusteringInfos;
import jmt.engine.jwat.workloadAnalysis.clustering.kMean.KMeanClusteringEngine.TempClusterStatistics;

public class ClusteringInfosKMean implements ClusteringInfos {

	public ClusterInfoKMean[] infoCluster;

public int isGoodCluster; // -1 not present; 0 not great; 1 excellent
public double omsr; // overall
public double ratio; // ratio

public int[] numElem; // number of elements of each single cluster
public double[] percent; // percentage of the total

	public int numCluster;

	public double passw;

	public ClusteringInfosKMean(int numCluster, int nvars) {
		ratio = -1;
		isGoodCluster = 0;
		numElem = new int[numCluster + 1];
		infoCluster = new ClusterInfoKMean[numCluster + 1];
		for (int i = 0; i < numCluster + 1; i++) {
			infoCluster[i] = new ClusterInfoKMean(nvars);
		}
		percent = new double[numCluster + 1];
		this.numCluster = numCluster;
	}

	public void Output(int[] varSel, TempClusterStatistics[][] sum, short[] clusAssign, MatrixObservations m, double oldPassw) //Deve valere assw precedentemente calcolato 
	{

		passw = oldPassw;

double ssb; // variance between classes on variable j
double ssw; // internal variance on variable j
double dfw = 0; // degree of internal freedom between classes
double assw = 0;// total error in partition

		double sd, sc, r[];
		r = new double[varSel.length];
		for (int i = 0; i < varSel.length; i++) {
			r[i] = 0;
		}

		double th = Math.pow(10, -10);
		double dfb = numCluster + 1;

		for (int j = 0; j < varSel.length; j++) {

			sd = 0;
			sc = 0;
			ssb = 0;
			ssw = 0;
			for (int k = 0; k <= numCluster; k++) {
				sd += (sum[j][k].Media * sum[j][k].numOs);
				ssb += (Math.pow(sum[j][k].Media, 2) * sum[j][k].numOs);
				ssw += sum[j][k].SSDev;
				sc += sum[j][k].numOs;
			}

			dfw = sc - dfb - 2;
			if (sc == 0.) {
				sc = th;
			}
			if (dfw == 0.) {
				dfw = th;
			}
			if (dfb == 0.) {
				dfb = th;
			}
			assw += ssw;
			ssb -= (Math.pow(sd, 2) / sc);
			ssb /= dfb;
			ssw /= dfw;
			if (ssw == 0.) {
				ssw = th;
			}

ratio = 0; // variance ratio for variable j
if (ssw != 0) { // I don't think it is needed..
				ratio = (r[j] / ssw - 1) * (1 + dfw) + 1;
				r[j] = ssw;

			}
		}

		if (assw == 0) {
			omsr = 0;
		} else {
			omsr = ((passw) / assw - 1) * dfw;
		}

		passw = assw;

/* Determine for each cluster the number of points that fall into each cluster */
		for (int xx = 0; xx < m.getNumOfObs(); xx++) {
			numElem[clusAssign[xx]]++;
		}
		for (int xx = 0; xx < numCluster + 1; xx++) {
			percent[xx] = (double) numElem[xx] / (double) m.getNumOfObs();
		}

	}

	public void DOStat(int[] varSel, short[] clusAssign, MatrixObservations m) {
		boolean[] bValidVar = new boolean[m.getNumVariables()];

		int df = 0;
		for (int sk = 0; sk < bValidVar.length; sk++) {
			if (varSel[df] == sk) {
				bValidVar[sk] = true;
				df++;
				if (df == varSel.length) {
					break;
				}
			} else {
				bValidVar[sk] = false;
			}
		}
		DoStat(m, varSel.length, clusAssign, bValidVar);
	}

	private void DoStat(MatrixObservations m, int nNumUsed, short[] arrNClus, boolean[] bValidVar) {

		double absoluteVal[][] = new double[arrNClus.length][m.getNumVariables()];

		int nclus;
//Observation[] currObs=m.getListObs(); //Current value of each variable
		Observation[] currObs = m.getVariables()[0].getCurObs();
		for (int count = 0; count < m.getNumOfObs(); count++) {

//nclus = Integer.parseInt(arrNClus.get(count).toString());
nclus = arrNClus[currObs[count].getID() - 1];
infoCluster[nclus].numObs++;

//Port of stat1cl
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
				absoluteVal[nclus][i] += Math.abs(currObs[count].getIndex(i));
				infoCluster[nclus].statClust[i].dSQuad += Math.pow(currObs[count].getIndex(i), 2);
				infoCluster[nclus].statClust[i].dSThird += Math.pow(currObs[count].getIndex(i), 3);
				infoCluster[nclus].statClust[i].dSFourth += Math.pow(currObs[count].getIndex(i), 4);

			}
		}

		double sum_per_var;
		double abs_sum_per_var;
		//Calcola percentuale della variabile usata nel cluster

		for (int i = 0; i < m.getNumVariables(); i++) {
			sum_per_var = 0;
			abs_sum_per_var = 0;
			for (int l = 0; l <= numCluster; l++) {
				sum_per_var += infoCluster[l].statClust[i].dSum;
				abs_sum_per_var += absoluteVal[l][i];
			}

			for (int l = 0; l <= numCluster; l++) {
				infoCluster[l].percVar[i] = absoluteVal[l][i] / abs_sum_per_var;
				//infoCluster[l].percVar[i] = infoCluster[l].statClust[i].dSum / sum_per_var;
			}
		}

		//Porting di stat2cl
		//Calcola media, varianza, etc,etc
		double dPerc5, dPerc6, dPerc7;

		for (int l = 0; l <= numCluster; l++) {

			for (int i = 0; i < m.getNumVariables(); i++) {
				infoCluster[l].statClust[i].dRange = infoCluster[l].statClust[i].dMaxObs - infoCluster[l].statClust[i].dMinObs;
				infoCluster[l].statClust[i].dMean = infoCluster[l].statClust[i].dSum / infoCluster[l].numObs;

				if (infoCluster[l].numObs != 1) {
					dPerc5 = Math.pow(infoCluster[l].statClust[i].dMean, 2);
					dPerc6 = Math.pow(infoCluster[l].statClust[i].dMean, 3);
					dPerc7 = Math.pow(infoCluster[l].statClust[i].dMean, 4);

					infoCluster[l].statClust[i].dVarnc = (infoCluster[l].statClust[i].dSQuad - infoCluster[l].numObs * dPerc5)
							/ (infoCluster[l].numObs - 1);

					if (infoCluster[l].statClust[i].dVarnc != 0) {
						infoCluster[l].statClust[i].dStdDv = Math.sqrt(infoCluster[l].statClust[i].dVarnc);
						infoCluster[l].statClust[i].dStdEr = infoCluster[l].statClust[i].dStdDv / Math.sqrt(infoCluster[l].numObs);

						infoCluster[l].statClust[i].dSkewn = infoCluster[l].statClust[i].dSThird - 3 * infoCluster[l].statClust[i].dMean
								* infoCluster[l].statClust[i].dSQuad + 3 * dPerc5 * infoCluster[l].statClust[i].dSum;
						infoCluster[l].statClust[i].dSkewn /= infoCluster[l].numObs;
						infoCluster[l].statClust[i].dSkewn -= dPerc6;
						infoCluster[l].statClust[i].dSkewn /= Math.pow(infoCluster[l].statClust[i].dVarnc, 1.5);

						infoCluster[l].statClust[i].dKurto = infoCluster[l].statClust[i].dSFourth - 4 * infoCluster[l].statClust[i].dMean
								* infoCluster[l].statClust[i].dSThird + 6 * dPerc5 * infoCluster[l].statClust[i].dSQuad - 4 * dPerc6
								* infoCluster[l].statClust[i].dSum;
						infoCluster[l].statClust[i].dKurto /= infoCluster[l].numObs;
						infoCluster[l].statClust[i].dKurto += dPerc7;
						infoCluster[l].statClust[i].dKurto /= Math.pow(infoCluster[l].statClust[i].dVarnc, 2);
						infoCluster[l].statClust[i].dKurto -= 3;
					}
				}
			}
		}
	}
}
