package jmt.jmva.analytical;

import java.util.Arrays;

import jmt.framework.data.Bound;

/**
 * Class that implements the Asymptotic Bounding methods for multi-class closed models.
 * See Lazowska's book "Quantitative System Performance" chapter 5 for more information.
 * @author Ben Homer, 2014.
 * N.B. Currently only reliable for upto 1 delay station.
 */
public final class SolverMultiClosedAB extends BoundsSolverMulti {

	/**
	 * Creates a new SolverSingleClosedAB
	 *  @param  stations Total number of service centers (stations)
	 *  @param  classes Total number of classes
	 */
	public SolverMultiClosedAB(int stations, int classes) {
		super(stations, classes);
	}
	
	/**
	 * Solves the model to get the bounds on all performance measures.
	 */
	@Override
	public void solve() {
		initialSetup();
		
		Ntot = computeNtot(populations);
		throughputBounds[stations] = calculatePerClassThroughputABs(populations);
		calculateAdditionalBounds(populations);
		
		printResults();
	}
	
	/** 
	 * Initial calculation of required values. 
	 */
	private void initialSetup() {
		Dmax = new double[classes];
		Dsum = new double[classes];
		Xmax = new double[classes];
		D = new double[stations][classes];
		Z = new double[classes];
		
		for (int c = 0; c < classes; c++) {
			Dmax[c] = 0;
		}
		
		for (int c = 0; c < classes; c++) {
			for (int k = 0; k < stations; k++) {
			
				D[k][c] = visits[k][c] * servTime[k][c][0];
			
				if (type[k] == DELAY) {
					Z[c] += D[k][c];
				} else {
					Dtot += D[k][c];
					Dsum[c] += D[k][c];
					if (D[k][c] > Dmax[c]) {
						Dmax[c] = D[k][c];
					}
				}
			}
		}
		
		boolean allDelay = true;
		for (int k = 0; k < stations; k++) {
			if (type[k] != DELAY) {
				allDelay = false;
				break;
			}
		}
		
		if (allDelay) {
			throw new UnsupportedOperationException("Must be at least one non-delay station.");
		}
		
		for (int c = 0; c < classes; c++) {
			Xmax[c] = (Dmax[c] == 0) ? 1.0 : 1.0/Dmax[c];
		}
	}
	
	/** 
	 * Calculates per class throughput ABs.
	 * @param N The populations per class.
	 */
	private Bound[] calculatePerClassThroughputABs(int[] N) {
		Bound[] Xtmp = new Bound[classes+1];
		Xtmp[classes] = new Bound(-1, -1);
		for (int c = 0; c < classes; c++) {
			if (N[c] < 1) {
				Xtmp[c] = new Bound();
				continue;
			}
					
			double lb = N[c] / (Z[c] + Ntot*Dsum[c]);
			double ub = 1/Dmax[c];
			Xtmp[c] = new Bound(lb, ub);
		}
		return Xtmp;
	}
	
	/**
	 * Calculates the additional bounds on performance measures.
	 * @param N The populations per class.
	 * N.B. Only results that actually matter are per station per class results as all aggregate results
	 * are recomputed on client side (in ExactModel).
	 */
	private void calculateAdditionalBounds(int[] N) {
		
		throughputBounds[stations][classes] = new Bound();
		for (int c = 0; c < classes; c++) {
			throughputBounds[stations][classes].LB += throughputBounds[stations][c].LB;
			throughputBounds[stations][classes].UB += throughputBounds[stations][c].UB;
		}
		
		for (int k = 0; k < stations; k++) {
			utilizationBounds[k][classes] = new Bound();
			throughputBounds[k][classes] = new Bound();
			residenceTimeBounds[k][classes] = new Bound();
			queueLengthBounds[k][classes] = new Bound();
			
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
				
				throughputBounds[k][classes].LB += Math.max(throughputBounds[k][c].LB - Z[c], 0);
				throughputBounds[k][classes].UB += throughputBounds[k][c].UB + Z[c];
				
				// Per station residence time bounds
				/* AB are obtained by considering limiting queue values of 0 or Nr for
					class r (total population). They are trivial bounds but technically
					still bounds. Residence times should be obtained by the MVA formula,
					plugging in this special value of the queue lengths.*/
				residenceTimeBounds[k][c] = new Bound(
						type[k] == DELAY ? D[k][c] : D[k][c], 
						type[k] == DELAY ? D[k][c] : D[k][c] * Ntot);
				
				residenceTimeBounds[k][classes].LB += 
						(residenceTimeBounds[k][c].LB*throughputBounds[stations][c].LB)/throughputBounds[stations][classes].LB;
				residenceTimeBounds[k][classes].UB +=
						(residenceTimeBounds[k][c].UB*throughputBounds[stations][c].UB)/throughputBounds[stations][classes].UB;
			
				// Per station queue length bounds
				queueLengthBounds[k][c] = new Bound(
						throughputBounds[stations][c].LB * residenceTimeBounds[k][c].LB,
						throughputBounds[stations][c].UB * residenceTimeBounds[k][c].UB);
				
				queueLengthBounds[k][classes].LB += queueLengthBounds[k][c].LB;
				queueLengthBounds[k][classes].UB += queueLengthBounds[k][c].UB;
			}
		}
		
		utilizationBounds[stations][classes] = new Bound(-1, -1);
		
		
		for (int c = 0; c < classes; c++) {
			residenceTimeBounds[stations][c] = new Bound();
			queueLengthBounds[stations][c] = new Bound();
			for (int k = 0; k < stations; k++) {
				
				residenceTimeBounds[stations][c].LB += residenceTimeBounds[k][c].LB;
				residenceTimeBounds[stations][c].UB += residenceTimeBounds[k][c].UB;
				
				queueLengthBounds[stations][c].LB += queueLengthBounds[k][c].LB;
				queueLengthBounds[stations][c].UB += queueLengthBounds[k][c].UB;
			}
		}
		
		
		residenceTimeBounds[stations][classes] = new Bound();
		queueLengthBounds[stations][classes] = new Bound();
		
		for (int c = 0; c < classes; c++) {
			queueLengthBounds[stations][classes].LB += queueLengthBounds[stations][c].LB;
			queueLengthBounds[stations][classes].UB += queueLengthBounds[stations][c].UB;
			
		}
		
		residenceTimeBounds[stations][classes].LB = Ntot/throughputBounds[stations][classes].UB;
		residenceTimeBounds[stations][classes].UB = Ntot/throughputBounds[stations][classes].LB;
	}
	
	/** Computes the total population of the system by summing per class populations.
	 * @param N The per class populations.
	 * @return The sum of the populations.
	 */
	private double computeNtot(int[] N) {
		int Nsum = 0;
		for (int c = 0; c < classes; c++) {
			Nsum += N[c];
		}
		return Nsum;
	}
	
	/**
	 * Prints the results to the console.
	 */
	public void printResults() {
		if (PRINT_OUTPUT) {
			System.out.println("\n --- AB multi-class results (N = " + Arrays.toString(populations) + ") --- ");
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
			
			System.out.println("\nQueue length bounds per station per class:");
			for (int k = 0; k < stations; k++) {
				for (int c = 0; c < classes; c++) {
					System.out.println("Station " + k + " Class " + c + ": " + queueLengthBounds[k][c].LB 
							+ " <= Q_" + k + "," + c + "(N) <= " + queueLengthBounds[k][c].UB);
				}
			}
			
			System.out.println("\nUtilsation bounds per station per class:");
			for (int k = 0; k < stations; k++) {
				for (int c = 0; c < classes; c++) {
					System.out.println("Station " + k + " Class " + c + ": " + utilizationBounds[k][c].LB 
							+ " <= U_" + k + "," + c + "(N) <= " + utilizationBounds[k][c].UB);
				}
			}
			
			System.out.println("\nResidence time bounds per station per class:");
			for (int k = 0; k < stations; k++) {
				for (int c = 0; c < classes; c++) {
					System.out.println("Station " + k + " Class " + c + ": " + residenceTimeBounds[k][c].LB 
							+ " <= R_" + k + "," + c + "(N) <= " + residenceTimeBounds[k][c].UB);
				}
			}
			System.out.println();
		}
	}
}