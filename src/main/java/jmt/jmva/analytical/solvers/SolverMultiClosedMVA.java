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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static jmt.jmva.analytical.solvers.Util.generatePopulations;
import static jmt.jmva.analytical.solvers.Util.popHashCode;

/**
 * Solves a multiclass closed model, using MVA algorithm.
 * @author  Federico Granata, Stefano Omini
 */
public class SolverMultiClosedMVA extends SolverMulti {

	public final boolean DEBUG = false;

	//array of population for every class
	private int[] population;

	//sum af all populations
	private int totPop = 0;

	private int maxIndex = -1;

	//number of states
	private int memory = 1;

	// all populations required for MVA algorithm
	private int[][] populations;

	// demands vector
	private double[][] demand;

	// Map of population vector to queue length vector
	private Map<Integer, double[][]> Q;
	// TODO: maybe use array instead of a map

	// Maximum population
	private int maxPop;

	// queue length time proportions
	// for the final population with lastClass-1
	// needed for calculating utilisations of last closed class
	private double[][] lastClassPc;

	/** Creates new SolverMultiClosedMVA
	 *  @param  stations    number of service centers
	 *  @param  classes     number of classes
	 */
	public SolverMultiClosedMVA(int classes, int stations) {
		super(classes, stations);
		population = new int[classes];
	}

	//TODO: warning, it cannot handle classes with zero jobs
	/** initializes the Multi class solver with the system parameters.
	 * @param  n   array of names of the service centers.
	 * @param  t   array of types (LD or LI) of the service centers.
	 * @param  s   matrix of service time of the service centers.
	 * @param  v   matrix of visits to the service centers.
	 * @param pop  array of populations of each class
	 * @return true if the operation is completed with success
	 */
	public boolean input(String[] n, int[] t, double[][][] s, double[][] v, int[] pop) {
		maxPop = 0;
		int lastClass = classes - 1;

		if ((n.length != stations) || (t.length != stations) || (s.length != stations) || (v.length != stations)) {
			return false; // wrong input.
		}

		System.arraycopy(n, 0, name, 0, stations);
		System.arraycopy(t, 0, type, 0, stations);

		//OLD
		//for (int c = 0; c < classes; c++)
		//	totPop += pop[c];

		//NEW
		//@author Stefano Omini
		for (int c = 0; c < classes; c++) {
			if (pop[c] > 0) {
				totPop += pop[c];
			} else {
				System.out.println("Error: class population must be greater than 0.");
				return false;
			}

		}
		//end NEW

		for (int i = 0; i < stations; i++) {
			//DEK (Federico Granata) 26-09-2003
			//for (int j = 0; j < classes; j++)
			//	servTime[i][j] = new double[totPop];
			if (t[i] == LD) {
				for (int j = 0; j < classes; j++) {
					servTime[i][j] = new double[totPop+1];
				}
			} else {
				for (int j = 0; j < classes; j++) {
					servTime[i][j] = new double[1];
					//end
				}
			}
		}

		System.arraycopy(pop, 0, population, 0, pop.length);
		//finds out the class with maximum population
		for (int c = 0; c < classes; c++) {
			//OLD
			//if (population[c] > maxPop) {
			if (population[c] >= maxPop) {
				maxPop = population[c];
				maxIndex = c;
			}
		}

		//moves in the last position the class with maximum population
		//TODO: why? (to optimize the memory consumption moving intelligently on the state space)
		population[lastClass] = pop[maxIndex];
		population[maxIndex] = pop[lastClass];

		//computes the number of states
		for (int c = 0; c < classes; c++) {
			if (c != maxIndex) {
				memory *= (pop[c] + 1);
			}
		}

		for (int i = 0; i < stations; i++) {
			System.arraycopy(v[i], 0, visits[i], 0, lastClass);
			visits[i][lastClass] = v[i][maxIndex];
			visits[i][maxIndex] = v[i][lastClass];
			if (type[i] == LD) {
				for (int j = 0; j < lastClass; j++) {
					System.arraycopy(s[i][j], 0, servTime[i][j], 1, s[i][j].length);
				}
				System.arraycopy(s[i][maxIndex], 0, servTime[i][lastClass], 1, s[i][maxIndex].length);
				System.arraycopy(s[i][lastClass], 0, servTime[i][maxIndex], 1, s[i][lastClass].length);

			}
			else {
				for (int j = 0; j < lastClass; j++) {
					System.arraycopy(s[i][j], 0, servTime[i][j], 0, s[i][j].length);
				}
				System.arraycopy(s[i][maxIndex], 0, servTime[i][lastClass], 0, s[i][maxIndex].length);
				System.arraycopy(s[i][lastClass], 0, servTime[i][maxIndex], 0, s[i][lastClass].length);
			}


		}
		lastClassPc = new double[stations][totPop+1];

		populations = generatePopulations(population);
		computeDemands();
		initialiseQueueMap();
		return true;
	}

	//NEW
	//@author Stefano Omini

	/**
	 * A system is said to have sufficient capacity to process a given load
	 * <tt>lambda</tt> if no service center is saturated as a result of the combined loads
	 * of all the classes.
	 * <br>
	 * Must be implemented to create a multi class model solver.
	 * <br>
	 * WARNING: This method should be called before solving the system.
	 * @return true if sufficient capacity exists for the given workload, false otherwise
	 *
	 *
	 */
	@Override
	public boolean hasSufficientProcessingCapacity() {
		//only closed class: no saturation
		return true;
	}

	//end NEW

	/**
	 * Solves the model
	 * <br>
	 * "input(...)" method must have been called before solving the model!!<br><br>
	 */
	@Override
	public void solve() {
		long startTime = System.currentTimeMillis();
		for (int i = 1; i < populations.length; i++) {
			computeResidenceTime(populations[i]);
			computeThroughput(populations[i]);
			computeMeanQueueLengths(populations[i]);
		}
		computeFinalThroughputAndQueueLen();
		computeUtilisation();
		computeStationParams();
		restoreClassOrder();
		computeSystemParams();
		long endTime = System.currentTimeMillis();
		System.out.println((double)(endTime - startTime)/1000);
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
	 * Computes demands for each station and customer class for load independent stations
	 */
	private void computeDemands() {
		demand = new double[stations][classes];
		for (int m = 0; m < stations; m++) {
			for (int c = 0; c < classes; c++) {
				if (type[m] == LD) {
					demand[m][c] = visits[m][c] * servTime[m][c][1];
				} else {
					demand[m][c] = visits[m][c] * servTime[m][c][0];
				}
			}
		}
	}

	/**
	 * Computes the total number of customers for a particular population
	 * @param pop - population array, number of customers in each customer class
	 * @return the total number of customers
	 */
	private int numberOfCustomers(int[] pop) {
		int nCust = 0;
		for (int i = 0; i < classes; i++) {
			nCust += pop[i];
		}
		return nCust;
	}

	/**
	 * Computes the residence time for Load Independent stations
	 * @param c - customer class
	 * @param m - service station
	 * @param pop - population array
	 */
	private void computeResidenceTimeLI(int c, int m, int[] pop) {
		pop[c]--;
		residenceTime[m][c] = demand[m][c] * (1.0 + Q.get(popHashCode(pop, maxPop))[m][0]);
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
			residTimeSum += (i * servTime[m][c][i]) * Q.get(popHashCode(pop, maxPop))[m][i-1];
		}
		residenceTime[m][c] = visits[m][c] * residTimeSum;
		pop[c]++;
	}

	/**
	 * Computes the residence times for all stations and classes
	 * @param pop - population array
	 */
	private void computeResidenceTime(int[] pop) {
		int nCust = numberOfCustomers(pop);
		for (int c = 0; c < classes; c++) {
			for (int m = 0; m < stations; m++) {
				if (pop[c] > 0) {
					if (type[m] == LI) computeResidenceTimeLI(c, m, pop);
					else if (type[m] == LD) computeResidenceTimeLD(c, m, pop, nCust);
					else residenceTime[m][c] = demand[m][c];
				}
				else residenceTime[m][c] = 0;
			}
		}
	}

	/**
	 * Computes throughput of each class
	 * @param pop - population array
	 */
	private void computeThroughput(int[] pop) {
		double residenceTimeSum;
		for (int c = 0; c < classes; c++) {
			residenceTimeSum = 0;
			for (int m = 0; m < stations; m++) {
				residenceTimeSum += residenceTime[m][c];
			}
			if (residenceTimeSum != 0 && pop[c] > 0) {
				clsThroughput[c] = pop[c] / residenceTimeSum;
			} else {
				clsThroughput[c] = 0;
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
			for (int c = 0; c < classes; c++) {
				if (pop[c] > 0) {
					pop[c]--;
					cSum += (clsThroughput[c] * servTime[m][c][i]) * Q.get(popHashCode(pop, maxPop))[m][i-1];
					pop[c]++;
				}
			}
			tempQ[i] = cSum;
			pSum += cSum;
		}
		tempQ[0] = 1 - pSum;
		if (Arrays.equals(population, pop)) {
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
		for (int c = 0; c < classes; c++) {
			queueLengthSum += clsThroughput[c] * residenceTime[m][c];
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

	private void computeFinalThroughputAndQueueLen() {
		for (int c = 0; c < classes; c++) {
			for (int m = 0; m < stations; m++) {
				throughput[m][c] = clsThroughput[c] * visits[m][c];
				queueLen[m][c] = clsThroughput[c] * residenceTime[m][c];
			}
		}
	}

	private void computeUtilisation() {
		int c;
		for (c = 0; c < classes-1; c++) {
			population[c]--;
			for (int m = 0; m < stations; m++) {
				utilization[m][c] = throughput[m][c] * servTime[m][c][0]; // Umc=Xmc*Smc
				if (type[m] == LD) {
					for (int n = 1; n <= totPop; n++) {
						utilization[m][c] += demand[m][c] * clsThroughput[c] * Q.get(popHashCode(population,maxPop))[m][n-1];
					}
					population[c]++;
				}
			}
			population[c]++;
		}
		for (int m = 0; m < stations; m++) {
			utilization[m][c] = throughput[m][c] * servTime[m][c][0]; // Umc=Xmc*Smc
			if (type[m] == LD) {
				for (int n = 1; n <= totPop; n++) {
					utilization[m][c] += demand[m][c] * clsThroughput[c] * lastClassPc[m][n-1];
				}
			}
		}

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


	/** generates a string with all the calculated indices.
	 * @return the string
	 */
	@Override
	public String toString() {
		return super.toString();
//		if (!intermediate_results) {
//			//if intermediate results have not been saved, do not write them!
//			return super.toString();
//		}
//		StringBuffer buf = new StringBuffer();
//		buf.append("\n------------------------------------");
//		buf.append("\nAnalysis with MVA Multiclass: intermediate results");
//		buf.append("\n\n");
//		return buf.toString();
	}


	public static int[] pprod(int[] N) {
		int[] state = new int[N.length];
		Arrays.fill(state, 0);
		return state;
	}

	public static boolean pprod(int[] n, int[] N) {
		int L = N.length;
		for (int i=0; i<L; i++) {
			if (n[i]!=N[i]) {
				n[i]++;
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		int[] N = new int[]{2,4, 3};
		int[] s = pprod(N);
		System.out.println("(" + s[0] + ", " + s[1] + ", " + s[2] + ")");
		do {
			pprod(s, N);
			System.out.println("(" + s[0] + ", " + s[1] + ", " + s[2] + ")");
		} while (!Arrays.equals(s, N));
	}

}
