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


package jmt.jmva.analytical.solvers;

import java.lang.Math;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static jmt.jmva.analytical.solvers.Util.*;

/**
 * Solves a multiclass model, with both open and closed classes.
 * @author Federico Granata, Stefano Omini
 */
public class SolverMultiMixed extends SolverMulti {

	//TODO add check on saturation
	//TODO: add calculation of response time at each station

	/** array with class types (0 for open, 1 for closed) */
	protected int[] classType;

	/** matrix with service demands */
	protected double[][] demand;

	/** array describing the classes: each element can be either an
	 *  arrival rate (for open classes) or a population (for closed ones),
	 *  according to the class type  */
	protected double[] popPar;

	// arrival rate vector
	private double[] lambda;

	// population vector
	private double[] N;

	private double[][] mu;

	// map of population vector to queue length vector
	private Map<Integer, double[][]> Q;
	// TODO: maybe use array instead of a map

	// all populations required for MVA algorithm
	private int[][] populations;

	// Effective capacity
	private int Nt;
	private double[][] EC;
	private double[][] E;
	private double[][] Eprime;

	private int closed;
	// maximum population of closed classes
	private int maxPop;
	// index of class with maximum population
	private int maxIndex;
	//sum af all populations of closed classes
	private int totPop;

	private double[][] Dc;
	private int[] Nc;

	// performance measures for closed classes
	private double[][] closedResidTime;
	private double[] closedThroughput;
	private double[][] closedUtilisation;

	// used for computing performance measures for LI stations
	private double[] openTotalUtilisation;
	private double[] closedTotalQueueLength;

	// queue length time proportions
	// for the final population with lastClass-1
	// needed for calculating utilisations of last closed class
	private double[][] lastClassPc;



	/**
	 * Constructor
	 * @param classes number of classes
	 * @param stations number of stations
	 */
	public SolverMultiMixed(int classes, int stations) {
		super(classes, stations);
	}

	/** initializes the Multi class solver with the system parameters.
	 *  @param  n   array of names of service centers.
	 *  @param  t   array of the types (LD or LI) of service centers.
	 *  @param  s   matrix of service time of the service centers.
	 *  @param  v   matrix of visits to the service centers.
	 *  @param popPar array describing the classes: each element can be either an
	 *  arrival rate (for open classes) or a population (for closed ones),
	 *  according to the class type
	 *  @param classType array of class types (open or closed)
	 *  @return true if the operation has been completed with success
	 */
	public boolean input(String[] n, int[] t, double[][][] s, double[][] v, double[] popPar, int[] classType) {
		if (super.input(n, t, s, v)) {
			this.popPar = popPar;
			this.classType = classType;
			computeMaxAndTotPop();

			//moves in the last position the class with maximum population
			//TODO: why? (to optimize the memory consumption moving intelligently on the state space)
			int lastClass = classes - 1;
			double temp = popPar[lastClass];
			this.popPar[lastClass] = popPar[maxIndex];
			this.popPar[maxIndex] = temp;

			int tempType = classType[lastClass];
			this.classType[lastClass] = classType[maxIndex];
			this.classType[maxIndex] = tempType;

			lastClassPc = new double[stations][totPop+1];

			for (int i = 0; i < stations; i++) {
				System.arraycopy(v[i], 0, visits[i], 0, lastClass);
				visits[i][lastClass] = v[i][maxIndex];
				visits[i][maxIndex] = v[i][lastClass];
				if (type[i] == LD) {
					System.arraycopy(s[i][maxIndex], 0, servTime[i][lastClass], 0, s[i][maxIndex].length);
					System.arraycopy(s[i][lastClass], 0, servTime[i][maxIndex], 0, s[i][lastClass].length);
				}
				else {
					System.arraycopy(s[i][maxIndex], 0, servTime[i][lastClass], 0, s[i][maxIndex].length);
					System.arraycopy(s[i][lastClass], 0, servTime[i][maxIndex], 0, s[i][lastClass].length);
				}
			}

			computeNumClosed();
			computeLambdaN();
			computeClosedPops();
			initialiseDemands();
			initialiseEC();
			populations = generatePopulations(Nc);
			initialiseClosedPerfIndices();
			computeOpenTotalUtilisation();
			return true;
		}
		return false;
	}

	@Override
	public void solve() {
		//tests if all the resources, stations, are load independent
		boolean loadIndep = true;
		for (int i = 0; i < stations; i++) {
			if (type[i] == LD) {
				loadIndep = false;
				break;
			}
		}

		if (loadIndep) {
			solveLI();
		} else {
			solveLD();
		}
	}

	/**
	 * Compute total number of customers and the maximum population
	 */
	private void computeMaxAndTotPop() {
		maxIndex = 0;
		maxPop = 0;
		totPop = 0;
		int n;
		for (int c = 0; c < classes; c++) {
			if (classType[c] == CLOSED_CLASS) {
				n = (int) popPar[c];
				totPop += n;
				if (n > maxPop)
					maxPop = n;
				maxIndex = c;
			}
		}
	}

	/**
	 * Xompute the total number of closed customers
	 */
	private void computeNumClosed() {
		closed = 0;
		for (int i = 0; i < classes; i++) {
			closed += classType[i];
		}
	}

	/**
	 * Initialise arrival rates and customer population
	 */
	private void computeLambdaN() {
		lambda = new double[classes];
		N = new double[classes];
		for (int i = 0; i < classes; i++) {
			if (classType[i] == OPEN_CLASS) {
				lambda[i] = popPar[i];
				N[i] = Double.POSITIVE_INFINITY;
			} else { // if closed class
				lambda[i] = 0;
				N[i] = popPar[i];
			}
		}
	}

	/**
	 * Initialise closed population vector
	 */
	private void computeClosedPops() {
		Nc = new int[closed];
		int c = 0;
		int n;
		for (int i = 0; i < classes; i++) {
			if (classType[i] == CLOSED_CLASS) {
				n = (int) popPar[i];
				Nc[c] = n;
				c++;
			}
		}
	}

	/**
	 * Initialise demands and mu
	 */
	private void initialiseDemands() {
		demand = new double[stations][classes];
		Dc = new double[stations][closed];
		mu = new double[stations][totPop];
		int closedIndex;
		for (int m = 0; m < stations; m++) {
			closedIndex = 0;
			for (int c = 0; c < classes; c++) {
				demand[m][c] = visits[m][c] * servTime[m][c][0];
				if (classType[c] == CLOSED_CLASS) {
					Dc[m][closedIndex] = demand[m][c];
					closedIndex++;
				}
			}
			for (int i = 0; i < totPop; i++) {
				mu[m][i] =  type[m] == LD ? servTime[m][0][0] / servTime[m][0][i] : 1;
			}
		}
	}

	/**
	 * Initialise effective capacity
	 */
	private void initialiseEC() {
		Nt = totPop+1 ;
		EC = new double[stations][Nt];
		E = new double[stations][1+Nt];
		Eprime = new double[stations][1+Nt];
	}

	/**
	 * Initialise closed perfomance indices
	 */
	private void initialiseClosedPerfIndices() {
		closedResidTime = new double[stations][closed];
		closedThroughput = new double[closed];
		closedUtilisation = new double[stations][closed];
	}

	/**
	 * Compute initial total utilisation of open classes
	 */
	private void computeOpenTotalUtilisation() {
		openTotalUtilisation = new double[stations];
		for (int m = 0; m < stations; m++) {
			openTotalUtilisation[m] = 0;
			for (int c = 0; c < classes; c++) {
				if (classType[c] == OPEN_CLASS) {
					openTotalUtilisation[m] += lambda[c] * demand[m][c];
				}
			}
		}
	}


	public void solveLD() {

		initialiseQueueMap();
		computeEffectiveCapacity();

		for (int i = 1; i < populations.length; i++) {
			computeResidenceTime(populations[i]);
			computeThroughput(populations[i]);
			computeMeanQueueLengths(populations[i]);
		}
		computeClosedUtilisation();
		copyClosedPerfIndices();
		computeOpenPerfIndices();
		computeFinalThroughput();
		restoreClassOrder();
		computeStationParams();
		computeSystemParams();

	}

	/**
	 * Initialises a map of population to an array of queue lengths, Qk, for load independent stations
	 * and an array of proportion of times, pk(j|n), for load dependent stations
	 */
	private void initialiseQueueMap() {
		Q = new HashMap<>();
		for (int[] pop : populations) {
			Q.put(popHashCode(pop, maxPop), new double[stations][totPop+1]);
		}
		for (int m = 0; m < stations; m++) {
			if (type[m] == LD) {
				Q.get(popHashCode(populations[0], maxPop))[m][0] = 1;
			}
		}
	}

	/**
	 * Computes the residence time for Load Independent stations
	 * @param c - customer class
	 * @param m - service station
	 * @param pop - population array
	 */
	private void computeResidenceTimeLI(int c, int m, int[] pop) {
		pop[c]--;
		closedResidTime[m][c] = Dc[m][c] / (1 - openTotalUtilisation[m]) * (1.0 + Q.get(popHashCode(pop, maxPop))[m][0]);
		pop[c]++;
	}

	/**
	 * Computes the residence time for Load Dependent stations
	 * @param c - customer class
	 * @param m - service station
	 * @param pop - population array
	 * @param nCust - total number of customers in pop
	 */
	private void computeResidenceTimeLD(int c, int m, int[] pop, int nCust) {
		double residTimeSum;
		pop[c]--;
		residTimeSum = 0;
		for (int i = 1; i <= nCust; i++) {
			// servTime should be demand?
			residTimeSum += i * Dc[m][c] * EC[m][i-1] * Q.get(popHashCode(pop, maxPop))[m][i-1];
		}
		closedResidTime[m][c] = visits[m][c] * residTimeSum;
		pop[c]++;
	}

	/**
	 * Computes the residence times for all stations and classes
	 * @param pop - population array
	 */
	private void computeResidenceTime(int[] pop) {
		int nCust = numberOfCustomers(pop);
		for (int c = 0; c < closed; c++) {
			for (int m = 0; m < stations; m++) {
				if (pop[c] > 0) {
					if (type[m] == LI) computeResidenceTimeLI(c, m, pop);
					else if (type[m] == LD) computeResidenceTimeLD(c, m, pop, nCust);
					else closedResidTime[m][c] = Dc[m][c];
				}
				else closedResidTime[m][c] = 0;
			}
		}
	}

	/**
	 * Computes throughput of each class
	 * @param pop - population array
	 */
	private void computeThroughput(int[] pop) {
		double residenceTimeSum;
		for (int c = 0; c < closed; c++) {
			residenceTimeSum = 0;
			for (int m = 0; m < stations; m++) {
				residenceTimeSum += closedResidTime[m][c];
			}
			if (residenceTimeSum != 0 && pop[c] > 0) {
				closedThroughput[c] = pop[c] / residenceTimeSum;
			} else {
				closedThroughput[c] = 0;
			}

		}
	}

	/**
	 * Computes proportion of time the centre m has i customers present
	 * given there are nCust customers in total. (Load Dependent stations)
	 * @param m - service station
	 * @param pop - population array
	 * @param nCust - total number of customers
	 */
	private void computeTimeProportions(int m, int[] pop, int nCust) {
		double pSum = 0;
		double cSum;
		double[] tempQ = new double[totPop+1];
		for (int i = 1; i <= nCust; i++) {
			cSum = 0;
			for (int c = 0; c < closed; c++) {
				if (pop[c] > 0) {
					pop[c]--;
					cSum += closedThroughput[c] * Dc[m][c] * EC[m][i-1] * Q.get(popHashCode(pop, maxPop))[m][i-1];
					pop[c]++;
				}
			}
			tempQ[i] = cSum;
			pSum += cSum;
		}
		tempQ[0] = 1 - pSum;
		if (Arrays.equals(Nc, pop)) {
			lastClassPc[m] = Q.get(popHashCode(pop, maxPop))[m];
		}
		Q.get(popHashCode(pop, maxPop))[m] = tempQ;
	}

	/**
	 * Computes mean queue length of a station. (Load Independent Stations)
	 * @param m - service station
	 * @param pop - population array
	 */
	private void computeMeanQueueLength(int m, int[] pop) {
		double queueLengthSum = 0;
		for (int c = 0; c < closed; c++) {
			queueLengthSum += closedThroughput[c] * closedResidTime[m][c];
		}
		Q.get(popHashCode(pop, maxPop))[m][0] = queueLengthSum;
	}

	/**
	 * Computes mean queue length of each station
	 * @param pop - population array
	 */
	private void computeMeanQueueLengths(int[] pop) {
		int nCust = numberOfCustomers(pop);
		for (int m = 0; m < stations; m++) {
			if (type[m] == LD) computeTimeProportions(m, pop, nCust);
			else computeMeanQueueLength(m, pop);
		}
	}

	/**
	 * Computes utilisation for closed classes
	 */
	private void computeClosedUtilisation() {
		int c;
		for (c = 0; c < closed-1; c++) {
			Nc[c]--;
			for (int m = 0; m < stations; m++) {
				if (type[m] == LD) {
					closedUtilisation[m][c] = 0;
					for (int n = 1; n <= totPop; n++) {
						closedUtilisation[m][c] += Dc[m][c] * closedThroughput[c] * Eprime[m][n-1] / E[m][n-1] * Q.get(popHashCode(Nc, maxPop))[m][n-1];
					}
				} else {
					closedUtilisation[m][c] = Dc[m][c] * closedThroughput[c];
				}
			}
			Nc[c]++;
		}
		// compute closedUtilisation for the last class
		for (int m = 0; m < stations; m++) {
			if (type[m] == LD) {
				closedUtilisation[m][c] = 0;
				for (int n = 1; n <= totPop; n++) {
					closedUtilisation[m][c] += Dc[m][c] * closedThroughput[c] * Eprime[m][n-1] / E[m][n-1] * lastClassPc[m][n-1];
				}
			} else {
				closedUtilisation[m][c] = Dc[m][c] * closedThroughput[c];
			}
		}
	}

	/**
	 * Copies over closed performance indices
	 */
	private void copyClosedPerfIndices() {
		int closedIndex = 0;
		closedTotalQueueLength = new double[stations];
		for (int c = 0; c < classes; c++) {
			if (classType[c] == CLOSED_CLASS) {
				clsThroughput[c] = closedThroughput[closedIndex];
				for (int m = 0; m < stations; m++) {
					utilization[m][c] = closedUtilisation[m][closedIndex];
					residenceTime[m][c] = closedResidTime[m][closedIndex];
					queueLen[m][c] = clsThroughput[c] * residenceTime[m][c];
					closedTotalQueueLength[m] += queueLen[m][c];
				}
				closedIndex++;
			}
		}
	}

	/**
	 * Computes open performance indices
	 */
	private void computeOpenPerfIndices() {
		for (int c = 0; c < classes; c++) {
			if (classType[c] == OPEN_CLASS) {
				clsThroughput[c] = lambda[c];
				for (int m = 0; m < stations; m++) {
					if (type[m] == LD) {
						queueLen[m][c] = 0;
						utilization[m][c] = 0;
						for (int n = 0; n <= totPop; n++) {
							queueLen[m][c] += lambda[c] * demand[m][c] * (n+1) * EC[m][n] * Q.get(popHashCode(Nc, maxPop))[m][n];
							utilization[m][c] += lambda[c] * Eprime[m][n+1] / E[m][n+1] * Q.get(popHashCode(Nc, maxPop))[m][n];
						}
					} else {
						queueLen[m][c] = lambda[c] * demand[m][c] * (1 + closedTotalQueueLength[m]) / (1 - openTotalUtilisation[m]);
						utilization[m][c] = lambda[c] * demand[m][c];
					}
					residenceTime[m][c] = queueLen[m][c] / lambda[c];
				}
			}
		}
	}

	/**
	 * Computes final throughput
	 */
	private void computeFinalThroughput(){
		for (int c = 0; c < classes; c++) {
			for (int m = 0; m < stations; m++) {
				throughput[m][c] = clsThroughput[c] * visits[m][c];
			}
		}
	}

	/**
	 * Computes stations specific parameters after MVA has terminated
	 */
	private void computeStationParams() {
		for (int c = 0; c < classes; c++) {
			for (int m = 0; m < stations; m++) {
				scThroughput[m] += throughput[m][c];
				scUtilization[m] += utilization[m][c];
				scResidTime[m] += residenceTime[m][c];
				scQueueLen[m] += queueLen[m][c];
			}
		}
	}

	/**
	 * Computes system parameters after MVA has terminated
	 */
	private void computeSystemParams() {
		sysResponseTime = 0;
		sysNumJobs = 0;

		for (int c = 0; c < classes; c++) {
			for (int m = 0; m < stations; m++) {
				clsRespTime[c] += residenceTime[m][c];
				sysNumJobs += queueLen[m][c];
			}
			sysResponseTime += clsRespTime[c];
		}

		sysThroughput = sysNumJobs / sysResponseTime;
	}

	/**
	 * Restores class order for correct output of computed parameters
	 */
	private void restoreClassOrder() {
		double temp;
		int lastClass = classes - 1;
		for (int m = 0; m < stations; m++) {
			temp = throughput[m][maxIndex];
			throughput[m][maxIndex] = throughput[m][lastClass];
			throughput[m][lastClass] = temp;
			temp = utilization[m][maxIndex];
			utilization[m][maxIndex] = utilization[m][lastClass];
			utilization[m][lastClass] = temp;
			temp = queueLen[m][maxIndex];
			queueLen[m][maxIndex] = queueLen[m][lastClass];
			queueLen[m][lastClass] = temp;
			temp = residenceTime[m][maxIndex];
			residenceTime[m][maxIndex] = residenceTime[m][lastClass];
			residenceTime[m][lastClass] = temp;
		}
		temp  = popPar[maxIndex];
		popPar[maxIndex] = popPar[lastClass];
		popPar[lastClass] = temp;

		int tempType = classType[lastClass];
		classType[lastClass] = classType[maxIndex];
		classType[maxIndex] = tempType;

	}


	/**
	 * Computes the total number of customers for a particular population
	 * @param pop - population array, number of customers in each customer class
	 * @return the total number of customers
	 */
	private int numberOfCustomers(int[] pop) {
		int nCust = 0;
		for (int i = 0; i < closed; i++) {
			nCust += pop[i];
		}
		return nCust;
	}

	/**
	 * Computes effective capacity
	 */
	private void computeEffectiveCapacity() {
		// load brought to stations by open class jobs
		double[] Lo = new double[stations];
		for (int i = 0; i < stations; i++) {
			Lo[i] = 0;
			for (int j = 0; j < classes; j++) {
				Lo[i] += lambda[j] * demand[i][j];
			}
		}

		// limited load dependence level
		int[] b = new int[stations];
		for (int m = 0; m < stations; m++) {
			int i = 1;
			while (i < mu[m].length && mu[m][i] != mu[m][i-1]) {
				i++;
			}
			b[m] = i;
		}

		expandMu();


		double[][] C = new double[stations][mu[0].length];

		for (int m = 0; m < stations; m++) {
			for (int i = 0; i < mu[0].length; i++) {
				C[m][i] = 1 / mu[m][i];
			}
		}

		for (int i = 0; i < stations; i++) {
			double[] E1 = new double[1 + Nt];
			double[] E2 = new double[1 + Nt];
			double[] E3 = new double[1 + Nt];
			double[][] F2 = new double[1 + Nt][1 + b[i] - 2];
			double[][] F3 = new double[1 + Nt][1 + b[i] - 2];

			double[] E2prime = new double[1 + Nt];
			double[][] F2prime = new double[1 + Nt][1 + b[i] - 2];

			for (int n = 0; n <= Nt; n++) {
				if (n >= b[i]) {
					E[i][n] = 1 / Math.pow((1 - Lo[i] * C[i][b[i] - 1]), (n + 1));
					Eprime[i][n] = C[i][b[i] - 1] * E[i][n];
				} else {
					// compute E1
					if (n == 0) {
						E1[n] = 1 / (1 - Lo[i] * C[i][b[i] - 1]);
						for (int j = 0; j < b[i] - 1; j++) {
							E1[n] *= (C[i][j] / C[i][b[i] - 1]);
						}
					} else {
						E1[n] = (1 / (1 - Lo[i] * C[i][b[i] - 1])) * (C[i][b[i] - 1] / C[i][n - 1]) * E1[n - 1];
					}

					// compute F2
					for (int n0 = 0; n0 < b[i] - 1; n0++) {
						if (n0 == 0) {
							F2[n][0] = 1;
						} else {
							F2[n][n0] = ((n + n0) / (double) n0) * Lo[i] * C[i][n + n0 - 1] * F2[n][n0 - 1];
						}
					}

					// compute E2
					for (int j = 0; j < b[i] - 1; j++) {
						E2[n] += F2[n][j];
					}

					// compute F3
					for (int n0 = 0; n0 < b[i] - 1; n0++) {
						if (n == 0 && n0 == 0) {
							F3[n][n0] = 1;
							for (int j = 0; j < b[i] - 1; j++) {
								F3[n][n0] *= C[i][j] / C[i][b[i] - 1];
							}
						} else if (n > 0 && n0 == 0) {
							F3[n][n0] = C[i][b[i] - 1] / C[i][n - 1] * F3[n - 1][0];
						} else {
							F3[n][n0] = (n + n0) / (double) n0 * Lo[i] * C[i][b[i] - 1] * F3[n][n0 - 1];
						}
					}

					// compute E3
					for (int j = 0; j < b[i] - 1; j++) {
						E3[n] += F3[n][j];
					}

					// compute F2prime
					for (int n0 = 0; n0 < b[i] - 1; n0++) {
						if (n0 == 0) {
							F2prime[n][0] = C[i][n];
						} else {
							F2prime[n][n0] = (n + n0) / (double) n0 * Lo[i] * C[i][n + n0] * F2prime[n][n0 - 1];
						}
					}

					// compute E2prime
					for (int j = 0; j < b[i] - 1; j++) {
						E2prime[n] += F2prime[n][j];
					}

					// finally, compute E, Eprime
					E[i][n] = E1[n] + E2[n] - E3[n];
					if (n < b[i] - 1) {
						Eprime[i][n] = C[i][b[i] - 1] * E1[n] + E2prime[n] - C[i][b[i] - 1] * E3[n];
					} else {
						Eprime[i][n] = C[i][b[i] - 1] * E[i][n];
					}
				}
			}
			// compute EC
			for (int n = 0; n < Nt; n++) {
				EC[i][n] = C[i][n] * E[i][1 + n] / E[i][n];
			}
		}
	}

	/**
	 * Expand load dependent factors array
	 * Needed for computing effective caparity
	 */
	private void expandMu() {
		double[][] MU = new double[stations][(mu[0].length+1) * 2];
		for (int m = 0; m < stations; m++) {
			for (int i = 0; i < mu[m].length; i++) {
				MU[m][i] = mu[m][i];
			}
			for (int i = mu[m].length; i < MU[m].length; i++) {
				MU[m][i] = mu[m][mu[m].length-1];
			}
		}
		mu = MU;
	}


	/**
	 * Solves the system.
	 * <br><br>
	 * See:
	 * <br>
	 * <em>
	 * E.D. Lazowska, J. Zahorjan, G.S. Graham, K. Sevcik<br>
	 * Quantitative System Performance,<br>
	 * Prentice Hall, 1984<br>
	 * </em>
	 */
	public void solveLI() {
		//class parameters: population (for closed classes), arrival rate (for open classes)
		int[] closedClasses;
		double[] openClasses;

		//take in consideraton the presence of the open classes only.
		//we solve easily the problem as an open system.
		//the new utilization will reduce the power of the system
		//considering the effect of the open classes.
		//then we solve the close system
		for (int i = 0; i < stations; i++) {
			//NEW
			//@author Stefano Omini
			//initializes service center solution values
			scUtilization[i] = 0;
			scQueueLen[i] = 0;
			scResidTime[i] = 0;
			scThroughput[i] = 0;
			//end NEW

			for (int j = 0; j < classes; j++) {
				demand[i][j] = visits[i][j] * servTime[i][j][0];
				if (classType[j] == OPEN_CLASS) {
					utilization[i][j] = demand[i][j] * popPar[j];
					scUtilization[i] += utilization[i][j];
				}
			}
		}

		//counts open classes
		int oCounter = 0;
		for (int j = 0; j < classes; j++) {
			if (classType[j] == OPEN_CLASS) {
				oCounter++;
			}
		}

		//counts closed classes
		int cCounter = classes - oCounter;

		//allocates an array with the appropriate size for each kind of class
		openClasses = new double[oCounter];
		closedClasses = new int[cCounter];

		//fills the arrays containing only open parameters (i.e. lambda)
		//and only closed parameters (i.e. population)
		oCounter = 0;
		cCounter = 0;

		//demands for open classes do not change
		double[][][] closedServTime = new double[stations][closedClasses.length][1];
		double[][] closedVisits = new double[stations][closedClasses.length];

		for (int j = 0; j < classes; j++) {
			if (classType[j] == OPEN_CLASS) {
				//this parameter is a lambda
				openClasses[oCounter] = popPar[j];
				oCounter++;
			} else {
				//this parameter is a population
				closedClasses[cCounter] = (int) Math.ceil(popPar[j]);
				for (int i = 0; i < stations; i++) {
					/*
					the effect of the open classes on closed class performance is
					represented by "inflating" the service demands of the closed class
					at all devices.
					        (D*)c,k = (D)c,k / ( 1 - (U)open,k )
					The inflaction factor used is ( 1 - (U)open,k ) which is the
					percentage of time that the processor is not in use by the open classes.
					This technique allows to reduce model complexity by eliminating
					open classes while still incorporating their effects on performance.
					 */

					//these are the S* and V* of the inflated service demands
					//OLD
					//closedVisits[i][cCounter] = visits[i][j] / (1 - scUtilization[i]);
					//closedServTime[i][cCounter] = servTime[i][j];
					
					closedVisits[i][cCounter] = visits[i][j];
					if (type[i] == SolverMulti.DELAY) {
						closedServTime[i][cCounter][0] = servTime[i][j][0];
					} else {
						closedServTime[i][cCounter][0] = servTime[i][j][0] / (1 - scUtilization[i]);
					}
				}
				cCounter++;
			}
		}

		if (cCounter > 1) {
			//more than 1 closed class

			//solves the model consisting of only closed classes (with inflated D*)
			//X, Q and R obtained are valid also for the mixed model
			//U can be computed by applying the utilization law with the original set of D
			SolverMultiClosedMVA clSolver = new SolverMultiClosedMVA(cCounter, stations);
			clSolver.input(name, type, closedServTime, closedVisits, closedClasses);
			clSolver.solve();

			oCounter = 0;
			cCounter = 0;

			for (int i = 0; i < stations; i++) {
				//this value of the closed model is used to compute open classes parameters
				//see Lazowska
				scQueueLen[i] = clSolver.getAggrQueueLen(i);

				cCounter = 0;
				for (int j = 0; j < classes; j++) {
					if (classType[j] == CLOSED_CLASS) {
						//closed class
						queueLen[i][j] = clSolver.getQueueLen(i, cCounter);
						residenceTime[i][j] = clSolver.getResTime(i, cCounter);

						//OLD
						//throughput[i][j] = queueLen[i][j] / residenceTime[i][j];
						//utilization[i][j] = throughput[i][j] * closedServTime[i][cCounter][0];

						//NEW
						//@author Stefano Omini
						throughput[i][j] = clSolver.getThroughput(i, cCounter);

						//utilization must be computed (using the utilization law) with
						//the original service demand
						utilization[i][j] = throughput[i][j] * servTime[i][j][0];
						//end NEW

						clsRespTime[j] += residenceTime[i][j];
						cCounter++;
					} else {
						//open class
						//utilizations have been already computed
						if (type[i] == SolverMulti.DELAY) {
							residenceTime[i][j] = demand[i][j];
						} else {
							residenceTime[i][j] = demand[i][j] * (1 + scQueueLen[i]) / (1 - scUtilization[i]);
						}

						queueLen[i][j] = popPar[j] * residenceTime[i][j];
						//OLD
						//throughput[i][j] = queueLen[i][j] / residenceTime[i][j];
						//NEW
						//@author Stefano Omini
						throughput[i][j] = popPar[j] * visits[i][j];
						//end NEW
						clsRespTime[j] += residenceTime[i][j];
					}
					scThroughput[i] += throughput[i][j];
					clsNumJobs[j] += queueLen[i][j];
				}
			}

			for (int j = 0; j < classes; j++) {
				clsThroughput[j] = clsNumJobs[j] / clsRespTime[j];
				sysNumJobs += clsNumJobs[j];
			}

			sysResponseTime = 0;

			for (int i = 0; i < stations; i++) {
				scResidTime[i] = 0;
				scUtilization[i] = 0;
				for (int j = 0; j < classes; j++) {
					scUtilization[i] += utilization[i][j];
					if (classType[j] != CLOSED_CLASS) {
						//queue length of closed classes has been alredy considered
						scQueueLen[i] += queueLen[i][j];
					}
				}
				scResidTime[i] = scQueueLen[i] / scThroughput[i];
				sysResponseTime += scResidTime[i];

			}
			sysThroughput = sysNumJobs / sysResponseTime;
		} else {
			//only 1 closed class
			SolverSingleClosedMVA clSolver = new SolverSingleClosedMVA(closedClasses[0], stations);
			double[][] tempServTime = new double[closedServTime.length][];
			double[] tempVisits = new double[closedServTime.length];

			for (int i = 0; i < tempVisits.length; i++) {
				/*
				the effect of the open classes on closed class performance is
				represented by "inflating" the service demands of the closed class
				at all devices.
				(D*)c,k = (D)c,k / ( 1 - (U)open,k )
				The inflaction factor used is ( 1 - (U)open,k ) which is the
				percentage of time that the processor is not in use by the open classes.
				This technique allows to reduce model complexity by eliminating
				open classes while still incorporating their effects on performance.
				 */

				//these are the S* and V* of the inflated service demands
				tempVisits[i] = closedVisits[i][0];
				tempServTime[i] = closedServTime[i][0];
			}
			//solves the model consisting of only the closed class (with inflated D*)
			//X, Q and R obtained are valid also for the mixed model
			//U can be computed by applying the utilization law with the original set of D
			clSolver.input(name, type, tempServTime, tempVisits);
			clSolver.solve();

			oCounter = 0;
			cCounter = 0;

			for (int i = 0; i < stations; i++) {
				scQueueLen[i] = clSolver.getQueueLen(i);

				cCounter = 0;
				for (int j = 0; j < classes; j++) {
					if (classType[j] == CLOSED_CLASS) {
						//closed class
						queueLen[i][j] = clSolver.getQueueLen(i);
						residenceTime[i][j] = clSolver.getResTime(i);
						//OLD
						//throughput[i][j] = queueLen[i][j] / residenceTime[i][j];
						//utilization[i][j] = throughput[i][j] * closedServTime[i][cCounter][0];
						//must be multiplicated for visits number!!

						//OLD
						//throughput[i][j] = queueLen[i][j] * closedVisits[i][cCounter] / residenceTime[i][j];
						//NEW
						//@author Stefano Omini
						//TODO: verify correctness
						throughput[i][j] = clSolver.getThroughput(i);
						//utilization must be computed (using the utilization law) with the original
						//service demand
						utilization[i][j] = throughput[i][j] * servTime[i][j][0];
						//end NEW

						clsRespTime[j] += residenceTime[i][j];
						cCounter++;
					} else {
						//open class
						if (type[i] == SolverMulti.DELAY) {
							residenceTime[i][j] = demand[i][j];
						} else {
							residenceTime[i][j] = demand[i][j] * (1 + scQueueLen[i]) / (1 - scUtilization[i]);
						}						
						queueLen[i][j] = popPar[j] * residenceTime[i][j];
						//OLD
						//throughput[i][j] = queueLen[i][j] / residenceTime[i][j];
						//NEW
						//@author Stefano Omini
						throughput[i][j] = popPar[j] * visits[i][j];
						//end NEW
						clsRespTime[j] += residenceTime[i][j];
					}
					scThroughput[i] += throughput[i][j];
					clsNumJobs[j] += queueLen[i][j];
				}
			}

			for (int j = 0; j < classes; j++) {
				clsThroughput[j] = clsNumJobs[j] / clsRespTime[j];
				sysNumJobs += clsNumJobs[j];
			}

			restoreClassOrder();

			sysResponseTime = 0;

			for (int i = 0; i < stations; i++) {
				scResidTime[i] = 0;
				scUtilization[i] = 0;
				for (int j = 0; j < classes; j++) {

					scUtilization[i] += utilization[i][j];
					if (classType[j] != CLOSED_CLASS) {
						scQueueLen[i] += queueLen[i][j];
					}
				}
				scResidTime[i] = scQueueLen[i] / scThroughput[i];
				sysResponseTime += scResidTime[i];
			}
			sysThroughput = sysNumJobs / sysResponseTime;
		}
	}

	//NEW
	//@author Stefano Omini
	//TODO aggiungere controllo su processing capacity
	/**
	 * A system is said to have sufficient capacity to process a given load
	 * <tt>lambda</tt> if no service center is saturated as a result of the combined loads
	 * of all the open classes.
	 * <br>
	 * WARNING: This method should be called before solving the system.
	 * @return true if sufficient capacity exists for the given workload, false otherwise
	 */
	@Override
	public boolean hasSufficientProcessingCapacity() {
		//the maximum aggregate utilization between all the stations must be < 1
		//otherwise the system has no sufficient processing capacity
		for (int i = 0; i < stations; i++) {
			if (type[i] == SolverMulti.DELAY) {
				//delay station: do not check saturation
				continue;
			}

			//utiliz is the aggregate utilization for station j
			double utiliz = 0;
			for (int j = 0; j < classes; j++) {
				//consider only open classes
				if (classType[j] == OPEN_CLASS) {
					utiliz += popPar[j] * visits[i][j] * servTime[i][j][0];
				}
			}
			if (utiliz >= 1) {
				return false;
			}
		}
		//there are no stations with aggregate utilization >= 1
		return true;
	}
	//end NEW

}
