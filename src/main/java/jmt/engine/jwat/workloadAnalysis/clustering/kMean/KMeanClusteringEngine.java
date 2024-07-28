/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.engine.jwat.workloadAnalysis.clustering.kMean;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import jmt.engine.jwat.MatrixObservations;
import jmt.engine.jwat.Observation;
import jmt.engine.jwat.TimeConsumingWorker;
import jmt.engine.jwat.VariableNumber;

public class KMeanClusteringEngine {
	private TimeConsumingWorker worker;
	private KMean clustering;
	private MatrixObservations m;
	private Observation[] obsVal;
	private VariableNumber[] listAllVar; //Pointers to all variables
	private VariableNumber[] listSelVar; //Pointers to all variables
	private int indexSelVar[];

private int numMaxClust; //Maximum number of clusters to consider
private int numMaxIter; //Maximum number of iterations
private int numTotVar; //Total number of variables
private int numVarSel; //Total number of variables

private boolean[] isSelectedVar; //Mark the variables involved in clustering within the total of variables

public TempClusterStatistics[][] sum; //Contains the statistics of the various clusters
private double[] omsr; //Total variance report per partition

private int[] nn; //Used to contain the number of obs.per

public String m_strKMLog; //Log to add to session log
//public Vector nclusArray; //Vector containing a vector for each cluster //Array containing the current cluster of the variable
public short[][] nclust; //Matrix containing current observation cluster, for each n-clustering

NumberFormat Floatformatter = new DecimalFormat("###.###E0");

public class TempClusterStatistics {
public double Coord; // 1 coordinate
public int numOs; // 2 # of observations
public double Media; // 3 medium
public double stdDv; // 4 std. deviation
public double minOs; // 5 minimum values
public double maxOs; // 6 maximum values
public double Skewn; // 7 skewness
public double Kurto; // 8 Kurtosis
public double Varnz; // 9 variance
public double SSDev; // 10 sum of square deviations from center of cluster

public double Somma; // 1 sum of values
public double SumQd; // 2 sum of squares
public double SumTz; // 3 sum of the third powers
public double SumQt; // 4 sum of the fourth powers

public double VVdi5; // 5
public double VVdi6; // 6
public double VVdi7; // 7	
}

	public KMeanClusteringEngine(KMean clustering, TimeConsumingWorker worker) {
		this.worker = worker;
		this.clustering = clustering;
	}

	public boolean PrepClustering(MatrixObservations m, int numberMaximumCluster, int numberMaximumIterations, int chosenVariables[]) {
		int j = 0, i = 0;
		this.m = m;
/* Enter for TEST */
numMaxClust = numberMaximumCluster; // Maximum number of clusters to consider
numMaxIter = numberMaximumIterations; // Max number of iterations
/* Save total number of variables */
numTotVar = m.getNumVariables(); //Total number of variables
numVarSel = chosenVariables.length; //Total number of variables

obsVal = m.getVariables()[0].getCurObs();
/* Initialize array normalized variables and pointers to variables */
listAllVar = m.getVariables();
listSelVar = new VariableNumber[numVarSel];
indexSelVar = chosenVariables;
/* Setting the variables that are involved in clustering */

		isSelectedVar = new boolean[numTotVar];
		sum = new TempClusterStatistics[numTotVar][numMaxClust];
		omsr = new double[numMaxClust];
		nn = new int[numMaxClust];

		for (i = 0; i < numTotVar; i++) {
			isSelectedVar[i] = false;
		}

		for (i = 0; i < numTotVar; i++) {
			if (chosenVariables[j] == i) {
				isSelectedVar[i] = true;
				listSelVar[j] = listAllVar[i];
				j++;
				if (j == numVarSel) {
					break;
				}
			}
		}
		InitData();
		return true;

	}

	private void InitData() {
		int i = 0;
/* Initialize the variable statistics vector */
		for (i = 0; i < numMaxClust; i++) {
			for (int j = 0; j < numVarSel; j++) {
				sum[j][i] = new TempClusterStatistics();
				sum[j][i].Coord = 0;
				sum[j][i].Kurto = 0;
				sum[j][i].maxOs = 0;
				sum[j][i].Media = listSelVar[j].getUniStats().getMinValue();
				sum[j][i].minOs = 0;
				sum[j][i].numOs = 0;
				sum[j][i].Skewn = 0;
				sum[j][i].SSDev = 0;
				sum[j][i].stdDv = 0;
				sum[j][i].Varnz = 0;

				sum[j][i].Somma = 0;
				sum[j][i].SumQd = 0;
				sum[j][i].SumTz = 0;
				sum[j][i].SumQt = 0;
				sum[j][i].VVdi5 = 0;
				sum[j][i].VVdi6 = 0;
				sum[j][i].VVdi7 = 0;
			}
/* Initialize the array containing the number of points per cluster */
nn[i] = 0;
/* Clear array of total variance ratio per partition */
			omsr[i] = 0;
		}
	}

	public boolean DoClustering() {

/* Indicate the starting cluster */
		int endclust, startClust = 0;
		//nclusArray= new Vector();
		nclust = new short[numMaxClust][obsVal.length];
/* Call the function that does clustering */
		endclust = Build(startClust);
		if (endclust != -1) {
			clustering.setRatio(endclust + 1);
			return true;
		} else {
			return false;
		}
	}

	private int Build(int startClust) {
//Actually builds the various clusters
//update the state of the calculations on the Status Bar
		int km = 0;
/* create variables of unknown utility */
		int nclus = 0;
		double dclus = 0;
		int retVal = 0;
		int i = 0;
/* Execute the cycle of operations for the number of clusters to be considered chosen in the clustering form*/
		for (int kk = startClust; kk < numMaxClust; kk++) {
/* Execute the cycle of operations for the maximum number of iterations chosen in the clustering form */
			for (int nc = 0; nc < numMaxIter; nc++) {
				if (worker.isCanceled()) {
					return -1;
				}
				worker.updateInfos(2 + nc + (kk * numMaxIter), "Clustering " + (kk + 1) + " Iteration " + (nc + 1), false);
				boolean err = false;
				for (int kkk = 0; kkk <= kk; kkk++) {
					for (int j = 0; j < numVarSel; j++) {
						if ((nc == 0) || (sum[j][kkk].Coord != sum[j][kkk].Media)) {
/* Check that the Coordinate ??? is different from the mean of cluster j */
							err = true;
						}
					}
				}
/* The first loop always enters the if since nc = 0 and err is set to TRUE */
				if (err) {
					for (int kkk = 0; kkk <= kk; kkk++) {
/* Reset the number of observations for cluster kkk */
						nn[kkk] = 0;
/* Re Initialize the sum variable which contains the statistics of each cluster */
						for (int j = 0; j < numVarSel; j++) {
							/* WHY' ???? */
							sum[j][kkk].Coord = sum[j][kkk].Media;
							/* Zero the fields */
							sum[j][kkk].Somma = 0;
							sum[j][kkk].SumQd = 0;
							sum[j][kkk].SumTz = 0;
							sum[j][kkk].SumQt = 0;
							sum[j][kkk].VVdi5 = 0;
							sum[j][kkk].VVdi6 = 0;
							sum[j][kkk].VVdi7 = 0;
						}
					}
					/**************************************** kmeans ********************************************/
/* Retrieve the number of total observations */
					int numObserv = listSelVar[0].Size();
/* Prepare the variable to hold the current cluster */
					//nclusArray.add(new Vector());
					//((Vector)nclusArray.get(kk)).setSize(numObserv);
					for (int xx = 0; xx < numObserv; xx++) {

/* Operations to indicate the current percentage and pace of the kmean algorithm in the bar */

nclus = 0; /* I assume they are the number of clusters ??????????? */
dclus = Double.POSITIVE_INFINITY; /* Indicate the initial difference to consider between clusters ???????????????? */
						/*************************************************/
/* kk is the current number of clusters used
						  /*************************************************/
						for (int j = 0; j <= kk; j++) {
							/*
							 xp = 10^(-10)
							 dd = ????????
							 */
							double xp = Math.pow(10, -10);
							double dd = 0;
							/*
m_Num Norm Number of normalized variables
							 */
							for (int r = 0; r < numVarSel; r++) {
								dd += Math.pow(obsVal[xx].getIndex(indexSelVar[r]) - sum[r][j].Coord, 2);
								xp = xp + 1;
							}
							double temp = dd / xp;
							//double temp = dd/1;
							dd = Math.sqrt(temp);
							//System.err.println("Xp : " + xp + " temp : " + temp + " dclus : " + dclus + " dd : " + dd);
							if (dd <= dclus) {
								dclus = dd;
								nclus = j;
							}
						}

						nn[nclus]++;
						//((Vector)nclusArray.get(kk)).setElementAt(new Integer(nclus),obsVal[xx].getID()-1);
						nclust[kk][obsVal[xx].getID() - 1] = (short) nclus;
						//
						// computation of stat val
						//
						int tempNum = nn[nclus];

						if (tempNum == 0) {
							for (i = 0; i < numVarSel; i++) {
								sum[i][nclus].minOs = 0;
								sum[i][nclus].maxOs = 0;
								sum[i][nclus].Media = 0;
								sum[i][nclus].SSDev = 0;
							}
						} else {
							for (i = 0; i < numVarSel; i++) {
								double curVal = obsVal[xx].getIndex(indexSelVar[i]);
								if (tempNum == 1) {
									sum[i][nclus].Media = curVal;
									sum[i][nclus].SSDev = 0;
									sum[i][nclus].minOs = curVal;
									sum[i][nclus].maxOs = curVal;
								} else {
									if (sum[i][nclus].maxOs <= curVal) {
										sum[i][nclus].maxOs = curVal;
									}
									if (sum[i][nclus].minOs >= curVal) {
										sum[i][nclus].minOs = curVal;
									}
									sum[i][nclus].Media = sum[i][nclus].Media + (curVal - sum[i][nclus].Media) / tempNum;
									sum[i][nclus].SSDev = sum[i][nclus].SSDev + tempNum * Math.pow(curVal - sum[i][nclus].Media, 2) / (tempNum - 1);
								}

								sum[i][nclus].Somma += curVal;
								sum[i][nclus].SumQd += Math.pow(curVal, 2);
								sum[i][nclus].SumTz += Math.pow(curVal, 3);
								sum[i][nclus].SumQt += Math.pow(curVal, 4);
							}
						}
					}

					for (nclus = 0; nclus <= kk; nclus++) {

						int tempNum = nn[nclus];
						// stat2c
						for (i = 0; i < numVarSel; i++) {

							if (tempNum != 0) {
								sum[i][nclus].VVdi5 = Math.pow(sum[i][nclus].Media, 2);
								sum[i][nclus].VVdi6 = Math.pow(sum[i][nclus].Media, 3);
								sum[i][nclus].VVdi7 = Math.pow(sum[i][nclus].Media, 4);

								sum[i][nclus].stdDv = Math.sqrt(sum[i][nclus].SSDev / tempNum);

								if (!((tempNum == 1) || (sum[i][nclus].minOs == sum[i][nclus].maxOs))) {
									sum[i][nclus].Varnz = sum[i][nclus].SumQd - tempNum * sum[i][nclus].VVdi5 / (tempNum - 1);

									sum[i][nclus].Skewn = sum[i][nclus].SumTz;
									sum[i][nclus].Skewn -= 3 * sum[i][nclus].Media * sum[i][nclus].SumQd;
									sum[i][nclus].Skewn += 3 * sum[i][nclus].VVdi5 * sum[i][nclus].Somma;

									sum[i][nclus].Kurto = sum[i][nclus].SumQt;
									sum[i][nclus].Kurto -= 4 * sum[i][nclus].Media * sum[i][nclus].SumTz;
									sum[i][nclus].Kurto += 6 * sum[i][nclus].VVdi5 * sum[i][nclus].SumQd;
									sum[i][nclus].Kurto -= 4 * sum[i][nclus].VVdi6 * sum[i][nclus].Somma;

									double qsum9 = Math.pow(sum[i][nclus].Varnz, 2);
									double csum9 = Math.pow(sum[i][nclus].Varnz, 3 / 2);

									if (csum9 == 0) {
										sum[i][nclus].Skewn = 0;
										sum[i][nclus].Kurto = 0;
									} else {
										sum[i][nclus].Skewn /= tempNum;
										sum[i][nclus].Skewn -= sum[i][nclus].VVdi6 / csum9;

										if (qsum9 == 0) {
											sum[i][nclus].Kurto = 0;
										} else {
											sum[i][nclus].Kurto /= tempNum;
											sum[i][nclus].Kurto += sum[i][nclus].VVdi7;
											sum[i][nclus].Kurto /= qsum9;
											sum[i][nclus].Kurto -= 3;
										}
									}
								} else {
									sum[i][nclus].Varnz = 0;
									sum[i][nclus].stdDv = 0;
									sum[i][nclus].Skewn = 0;
									sum[i][nclus].Kurto = 0;
								}
							} else {
								sum[i][nclus].Varnz = 0;
								sum[i][nclus].stdDv = 0;
								sum[i][nclus].Skewn = 0;
								sum[i][nclus].Kurto = 0;
							}
						}

						for (int j = 0; j < numVarSel; j++) {
							sum[j][nclus].numOs = nn[nclus];

						}//j
					}// nclust
				} else {
					break; //quit for nc
				}// endif (err!=0)
			}// nc

			clustering.calcClusteringInfo(kk, sum,/*(Vector)nclusArray.get(kk),*/nclust[kk], m);

			if (kk != (numMaxClust - 1)) { //was 20
				double sm = 0;
				for (int j = 0; j < numVarSel; j++) {
					for (int kkk = 0; kkk <= kk; kkk++) {
						if (sum[j][kkk].stdDv >= sm) {
							sm = sum[j][kkk].stdDv;
							km = kkk;
						}
					}
				}

				int kn = kk + 1;
				for (int jj = 0; jj < numVarSel; jj++) {
					sum[jj][kn].numOs = 0;
					sum[jj][kn].Media = 0.;
					sum[jj][km].numOs = 0;
					sum[jj][km].Media = 0.;
				}
				// it compute the centroid of...
				for (int xx = 0; xx < listSelVar[0].Size(); xx++) {
					//if (Integer.parseInt(((Vector)nclusArray.get(kk)).get(obsVal[xx].getID()-1).toString()) == km) {
					if (nclust[kk][obsVal[xx].getID() - 1] == km) {
						for (int jj = 0; jj < numVarSel; jj++) {
							if (obsVal[xx].getIndex(indexSelVar[jj]) < sum[jj][km].Coord) {
								sum[jj][km].numOs++;
								sum[jj][km].Media += obsVal[xx].getIndex(indexSelVar[jj]);
							} else {
								sum[jj][kn].numOs++;
								sum[jj][kn].Media += obsVal[xx].getIndex(indexSelVar[jj]);
							}
						}
					}
				}

				for (int jj = 0; jj < numVarSel; jj++) {
					if (sum[jj][km].numOs != 0) {
						sum[jj][km].Media /= sum[jj][km].numOs;
					}

					if (sum[jj][kn].numOs != 0) {
						sum[jj][kn].Media /= sum[jj][kn].numOs;
					}
				}
			}

			retVal = kk;
		}// kk
		return retVal;
	}
}
