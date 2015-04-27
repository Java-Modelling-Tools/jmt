package jmt.jmva.analytical;

import jmt.framework.data.Bound;

/**
 * Class that implements the Asymptotic Bounding methods for a single-class closed models.
 * See Lazowska's book "Quantitative System Performance" chapter 5 for more information.
 * @author Ben Homer, 2014.
 */
public final class SolverSingleClosedAB extends BoundsSolver {

	/** Variable for calculating algorithm runtime. */
	protected long timer;
	
	/**
	 * Creates a new SolverSingleClosedAB
	 *  @param  population Total number of customers
	 *  @param  stations Total number of service centers (stations)
	 */
	public SolverSingleClosedAB(int population, int stations) {
		super(population, stations);
	}
	
	/**
	 * Solves the model to get the bounds on all performance measures.
	 */
	@Override
	public void solve() {
		long start = System.nanoTime();
		initialSetup();
		
		throughputBounds[stations] = calculateThroughputABs(population);
		calculateAdditionalBounds(population);
		
		long end = System.nanoTime();
		timer = end - start;
		printResults();
	}
	
	/** 
	 * Initial calculation of required values. 
	 */
	private void initialSetup() {
		Dmax = Dsum = Z = 0;
		int delayStations = 0;
		D = new double[stations];
		for (int k = 0; k < stations; k++) {
			D[k] = visits[k] * servTime[k][0];
			
			if (type[k] == DELAY) {
				Z += D[k];
				delayStations++;
			} else {
				Dsum += D[k];
				if (D[k] > Dmax) {
					Dmax = D[k];
				}
			}
		}
		
		if (delayStations == stations) {
			throw new UnsupportedOperationException("Must be at least one non-delay station.");
		}
		
		Xmax = (Dmax == 0) ? 1.0 : 1.0/Dmax;
	}
	
	/** 
	 * Calculates throughput ABs.
	 * @param N The population.
	 */
	private Bound calculateThroughputABs(int N) {
		if (N < 1) return new Bound();
		double lb = N/(N*Dsum + Z);
		double ub = Math.min(N/(Dsum + Z), Xmax);
		Bound Xtmp = new Bound(lb, ub);
		return Xtmp;
	}
	
	/**
	 * Calculates the additional bounds on performance measures.
	 * @param N The population.
	 * N.B. Only results that actually matter are per station results as all aggregate results
	 * are recomputed on client side (in ExactModel).
	 */
	private void calculateAdditionalBounds(int N) {
		
		for (int k = 0; k < stations; k++) {
			// Per station utilisation bounds
			utilisationBounds[k] = new Bound(D[k]*throughputBounds[stations].LB, D[k]*throughputBounds[stations].UB);
			
			// Per station throughput bounds (using Forced Flow Law)
			throughputBounds[k] = new Bound(visits[k]*throughputBounds[stations].LB, visits[k]*throughputBounds[stations].UB);
			
			// Per station residence time bounds
			/* AB are obtained by considering limiting queue values of 0 or Nr for
				class r (total population). They are trivial bounds but technically
				still bounds. Residence times should be obtained by the MVA formula,
				plugging in this special value of the queue lengths.*/
			residenceTimeBounds[k] = new Bound(
					type[k] == DELAY ? D[k] : D[k],
					type[k] == DELAY ? D[k] : D[k]*N);
		
			// Per station queue length bounds
			queueLengthBounds[k] = new Bound(
					throughputBounds[stations].LB*residenceTimeBounds[k].LB,
					throughputBounds[stations].UB*residenceTimeBounds[k].UB);
		}
		
		// Aggregate utilisation bounds are not possible.
		utilisationBounds[stations] = new Bound(-1, -1);
				
		// System response time bounds (using Response Time Law)
		residenceTimeBounds[stations] = new Bound();
		residenceTimeBounds[stations].LB = N/throughputBounds[stations].UB - Z;
		residenceTimeBounds[stations].UB = N/throughputBounds[stations].LB + Z;
		
		// System queue length bounds
		queueLengthBounds[stations] = new Bound();
		queueLengthBounds[stations].LB = throughputBounds[stations].LB * residenceTimeBounds[stations].LB;
		queueLengthBounds[stations].UB = throughputBounds[stations].UB * residenceTimeBounds[stations].UB;
	}
	
	/**
	 * Prints the results to the console.
	 */
	public void printResults() {
		if (PRINT_OUTPUT) {
			System.out.println("\n --- AB results (N = " + population + ") --- ");
			System.out.println("Runtime: " + timer + " nanoseconds");
			System.out.println("Throughput bounds: " + throughputBounds[stations].LB + " <= X(N) <= " + throughputBounds[stations].UB + "\n");
			System.out.println("Response time bounds: " +  residenceTimeBounds[stations].LB + " <= R(N) <= " +  residenceTimeBounds[stations].UB + "\n");
			System.out.println("Queue length bounds: " + queueLengthBounds[stations].LB + " <= Q(N) <= " + queueLengthBounds[stations].UB + "\n");
			
			System.out.println("Throughput bounds per station:");
			for (int k = 0; k < stations; k++) {
				System.out.println("Station " + k + ": " + throughputBounds[k].LB + " <= X_" + k + "(N) <= " + throughputBounds[k].UB);
			}
			
			System.out.println("\nQueue length bounds per station:");
			for (int k = 0; k < stations; k++) {
				System.out.println("Station " + k + ": " + queueLengthBounds[k].LB + " <= Q_" + k + "(N) <= " + queueLengthBounds[k].UB);
			}
			
			System.out.println("\nUtilsation bounds per station:");
			for (int k = 0; k < stations; k++) {
				System.out.println("Station " + k + ": " + utilisationBounds[k].LB + " <= U_" + k + "(N) <= " + utilisationBounds[k].UB);
			}
			
			System.out.println("\nResidence time bounds per station:");
			for (int k = 0; k < stations; k++) {
				System.out.println("Station " + k + ": " + residenceTimeBounds[k].LB + " <= R_" + k + "(N) <= " + residenceTimeBounds[k].UB);
			}
			System.out.println();
		}
	}
}