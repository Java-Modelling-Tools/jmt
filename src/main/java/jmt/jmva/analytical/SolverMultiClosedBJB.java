package jmt.jmva.analytical;

import java.util.Arrays;

import jmt.framework.data.Bound;

/**
 * Class that implements the Balanced Job Bounding methods for multi-class closed models.
 * Uses the the minimum/maximum of service demands method (using average of service demands
 * does not work for multi-class models).
 * See Lazowska's book "Quantitative System Performance" chapter 5 for more information.
 * @author Ben Homer, 2014.
 * N.B. Currently only reliable for upto 1 delay station.
 */
public final class SolverMultiClosedBJB extends BoundsSolverMulti {
	
	/** The number of delay stations. */
	private int delayStations;
	
	/**
	 * Creates a new SolverMultiClosedBJB
	 *  @param  stations Total number of service centers (stations)
	 *  @param  classes Total number of classes
	 */
	public SolverMultiClosedBJB(int stations, int classes) {
		super(stations, classes);
	}
	
	/**
	 * Solves the model to get the bounds on all performance measures.
	 */
	@Override
	public void solve() {
		initialSetup();
		
		throughputBounds[stations] = calculatePerClassThroughputBJBs(populations);
		calculateAdditionalBounds(populations);
		printResults();
	}
	
	/** 
	 * Initial calculation of required values.
	 */
	private void initialSetup() {
		Dmin = new double[classes];
		Dmax = new double[classes];
		Dsum = new double[classes];
		Dave = new double[classes];
		Xmin = new double[classes];
		Xmax = new double[classes];
		D = new double[stations][classes];
		Z = new double[classes];
		Ntot = 0;
		Ztot = 0;
		
		for (int c = 0; c < classes; c++) {
			Dmin[c] = Double.MAX_VALUE;
			Dmax[c] = 0;
		}
		
		for (int c = 0; c < classes; c++) {
			Ntot += populations[c];
			for (int k = 0; k < stations; k++) {
			
				D[k][c] = visits[k][c] * servTime[k][c][0];
			
				if (type[k] == DELAY) {
					Z[c] += D[k][c];
					Ztot += Z[c];
				} else {
					Dtot += D[k][c];
					Dsum[c] += D[k][c];
					if (D[k][c] > Dmax[c]) {
						Dmax[c] = D[k][c];
					}
					if (D[k][c] < Dmin[c]) {
						Dmin[c] = D[k][c];
					}
				}
			}
		}
		
		for (int k = 0; k < stations; k++) {
			if (type[k] == DELAY) {
				delayStations++;
			}
		}
		
		if (delayStations == stations) {
			throw new UnsupportedOperationException("Must be at least one non-delay station.");
		}
		
		for (int c = 0; c < classes; c++) {
			Dave[c] = Dsum[c] / (stations-delayStations);
			Xmax[c] = (Dmax[c] == 0) ? 0.0 : 1.0/Dmax[c];
			Xmin[c] = (Dmin[c] == 0) ? 0.0 : 1.0/Dmin[c];
		}
	}
	
	/**
	 *  Calculates throughput BJBs using min/max service demands method.
	 *  @param N The populations per class.
	 */
	private Bound[] calculatePerClassThroughputBJBs(int[] N) {
		Bound[] Xtmp = new Bound[classes+1];
		Xtmp[classes] = new Bound(-1, -1);
		
		for (int c = 0; c < classes; c++) {
			if (N[c] < 1) {
				Xtmp[c] = new Bound();
				continue;
			}
			double lb = N[c] / (Dsum[c] + Z[c] + Dmax[c]*(Ntot-1));
			double ub = Math.min(1/Dmax[c], N[c] / (Dsum[c] + Z[c] + Dmin[c]*(Ntot-1)));
			System.out.println("BJB LB: " + lb);
			System.out.println("BJB UB: " + ub);
			Xtmp[c] = new Bound(lb, ub);
		}
		return Xtmp;
	}
	
	/**
	 *  Calculates throughput BJBs using min/max service demands method.
	 *  @param N The populations per class.
	 */
	private Bound[] calculatePerClassResponseTimeBJBs(int[] N) {
		Bound[] Xtmp = new Bound[classes];
		for (int c = 0; c < classes; c++) {
			if (N[c] < 1) {
				Xtmp[c] = new Bound();
				continue;
			}
			
			double lb = Math.max(N[c]*Dmax[c] - Z[c], Dsum[c] + ((Ntot-1)*Dave[c])/(1 + Z[c] / Dsum[c]));
			double ub = Dsum[c] + ( ((Ntot-1)*Dmax[c]) / (1 + Z[c]/(N[c]*Dsum[c])) ); 
			lb += Ztot;
			ub += Ztot;
			
			Xtmp[c] = new Bound(lb, ub);
		}
		return Xtmp;
	}
	
	/**
	 * Calculates the additional bounds on performance measures.
	 * @param N The population.
	 * N.B. Only results that actually matter are per station per class results as all aggregate results
	 * are recomputed on client side (in ExactModel).
	 */
	private void calculateAdditionalBounds(int[] N) {
		for (int k = 0; k < stations; k++) {
			utilizationBounds[k][classes] = new Bound();
			throughputBounds[k][classes] = new Bound();
			residenceTimeBounds[k][classes] = new Bound(-1, -1); // N/A to BJBs
			queueLengthBounds[k][classes] = new Bound(-1, -1); // N/A to BJBs
			
			for (int c = 0; c < classes; c++) {
				// Per station utilisation bounds
				utilizationBounds[k][c] = new Bound(
						D[k][c]*throughputBounds[stations][c].LB, D[k][c]*throughputBounds[stations][c].UB);
				
				utilizationBounds[k][classes].LB += utilizationBounds[k][c].LB;
				utilizationBounds[k][classes].UB += utilizationBounds[k][c].UB;
				
				utilizationBounds[stations][c] = new Bound(-1, -1);
				
				// Per station throughput bounds (using Forced Flow Law)
				throughputBounds[k][c] = new Bound(
						visits[k][c]*throughputBounds[stations][c].LB,
						visits[k][c]*throughputBounds[stations][c].UB);
				
				throughputBounds[k][classes].LB += throughputBounds[k][c].LB;
				throughputBounds[k][classes].UB += throughputBounds[k][c].UB;
				
				// Per station residence time bounds
				residenceTimeBounds[k][c] = new Bound(-1, -1);
				
				// Per station queue length bounds
				queueLengthBounds[k][c] = new Bound(-1, -1);
			}
		}
		
		utilizationBounds[stations][classes] = new Bound(-1, -1);
		
		Bound[] clsR = calculatePerClassResponseTimeBJBs(N);
		for (int c = 0; c < classes; c++) {
			residenceTimeBounds[stations][c] = clsR[c];
			queueLengthBounds[stations][c] = new Bound();
			queueLengthBounds[stations][c].LB = throughputBounds[stations][c].LB * clsR[c].LB;
			queueLengthBounds[stations][c].UB = throughputBounds[stations][c].UB * clsR[c].UB;
		}
		
		throughputBounds[stations][classes] = new Bound();
		residenceTimeBounds[stations][classes] = new Bound();
		queueLengthBounds[stations][classes] = new Bound(); 
		
		for (int c = 0; c < classes; c++) {
			throughputBounds[stations][classes].LB += throughputBounds[stations][c].LB;
			throughputBounds[stations][classes].UB += throughputBounds[stations][c].UB;
			
			queueLengthBounds[stations][classes].LB += queueLengthBounds[stations][c].LB;
			queueLengthBounds[stations][classes].UB += queueLengthBounds[stations][c].UB;
			
		}
		
		residenceTimeBounds[stations][classes].LB = Ntot/throughputBounds[stations][classes].UB;
		residenceTimeBounds[stations][classes].UB = Ntot/throughputBounds[stations][classes].LB;
	}
	
	/**
	 * Prints the results to the console.
	 */
	public void printResults() {
		if (PRINT_OUTPUT) {
			System.out.println("\n --- BJB multi-class results (N = " + Arrays.toString(populations) + ") --- ");
			System.out.println("Throughput bounds: " + throughputBounds[stations][classes].LB + " <= X(N) <= " + throughputBounds[stations][classes].UB + "\n");
			System.out.println("Response time bounds: " + residenceTimeBounds[stations][classes].LB + " <= R(N) <= " + residenceTimeBounds[stations][classes].UB + "\n");
			System.out.println("Queue length bounds: " + queueLengthBounds[stations][classes].LB + " <= Q(N) <= " + queueLengthBounds[stations][classes].UB + "\n");
			
			System.out.println("Throughput bounds per station per class:");
			for (int k = 0; k < stations; k++) {
				for (int c = 0; c < classes; c++) {
					System.out.println("Station " + k + " Class " + c + ": " + throughputBounds[k][c].LB 
							+ " <= X_" + k + "," + c + "(N) <= " + throughputBounds[k][c].UB);
				}
			}
			
			System.out.println("\nUtilsation bounds per station per class:");
			for (int k = 0; k < stations; k++) {
				for (int c = 0; c < classes; c++) {
					System.out.println("Station " + k + " Class " + c + ": " + utilizationBounds[k][c].LB 
							+ " <= U_" + k + "," + c + "(N) <= " + utilizationBounds[k][c].UB);
				}
			}
		}
	}
}