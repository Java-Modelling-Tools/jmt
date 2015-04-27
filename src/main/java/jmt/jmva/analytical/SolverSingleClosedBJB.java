package jmt.jmva.analytical;

import jmt.framework.data.Bound;

/**
 * Class that implements the Balanced Job Bounding methods for a single-class closed models.
 * If useAverageDemand=true, uses the average of service demands method for tighter bounds on single-class models.
 * If useAverageDemand=false, uses the minimum/maximum of service demands method which can also potentially be
 * used for multi-class models (see SolverMultiClosedBJB for implementation of multi-class BJB solver).
 * See Lazowska's book "Quantitative System Performance" chapter 5 for more information.
 * @author Ben Homer, 2014.
 */
public final class SolverSingleClosedBJB extends BoundsSolver {
	
	/** Variable for calculating algorithm runtime. */
	protected long timer;
	
	/** The number of delay stations. */
	private int delayStations;
	
	/** Whether to compute throughput bounds using the average service
	 * demands method (for tighter bounds in single-class case). */
	private boolean useAverageDemand;
	
	/** Whether to calculate bounds for all performance indices or just throughput bounds.
	 *  If we only need throughput bounds, e.g. in GB solver, then setting this to
	 *  false improves efficiency. */
	private boolean calculateAll;
	
	/**
	 * Creates a new SolverSingleClosedBJB
	 *  @param  population Total number of customers
	 *  @param  stations Total number of service centers (stations)
	 */
	public SolverSingleClosedBJB(int population, int stations, boolean useAverageDemand) {
		super(population, stations);
		this.useAverageDemand = useAverageDemand;
		this.calculateAll = true;
	}
	
	/** Sets whether to calculate all indicies or only throughput bounds.
	 * If we only need the throughput bounds, e.g. in the GB solver, then
	 * setting calculateAll to false ensures that we do not do any additional
	 * calculation, hence improving efficiency.
	 * @param calculateAll Whether to calculate all performance bounds or only throughput bounds.
	 */
	public void setCalculateAll(boolean calculateAll) {
		this.calculateAll = calculateAll;
	}
	
	/**
	 * Sets the initial values required before bounds can be calculated.
	 * @param Dmin The minumum demand.
	 * @param Dmax The maximum demand.
	 * @param Dave The average demand.
	 * @param Dsum The sum of the demands.
	 * @param Z The total delay.
	 */
	public void setInitialMeasures(double Dmin, double Dmax, double Dave, double Dsum, double Z) {
		this.Dmin = Dmin;
		this.Dmax = Dmax;
		this.Dave = Dave;
		this.Dsum = Dsum;
		this.Z = Z;
	}
	
	/**
	 * Solves the model to get the bounds on all performance measures.
	 */
	@Override
	public void solve() {
		long start = System.nanoTime();
		initialSetup();
		
		if (useAverageDemand) {
			throughputBounds[stations] = calculateThroughputBJBsAvgDemand(population);
		} else {
			throughputBounds[stations] = calculateThroughputBJBsMinDemand(population);
		}
		
		if (calculateAll) {
			calculateAdditionalBounds(population);
			long end = System.nanoTime();
			timer = end - start;
			printResults();
		}
	}
	
	/** 
	 * Initial calculation of required values.
	 */
	private void initialSetup() {
		if (calculateAll) {
			Dmax = Dsum = Z = 0;
			Dmin = Double.MAX_VALUE;
			D = new double[stations];
			if (stations == 0) return;
			
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
					if (D[k] < Dmin) {
						Dmin = D[k];
					}
				}
			}
		
			Dave = Dsum / (stations-delayStations);
			if (delayStations == stations) {
				throw new UnsupportedOperationException("Must be at least one non-delay station.");
			}
		}
		
		Xmax = (Dmax == 0) ? 0.0 : 1.0/Dmax;
		Xmin = 0.0;
	}
	
	/** 
	 * Calculates throughput average BJBs.
	 * @param N The population.
	 */
	private Bound calculateThroughputBJBsAvgDemand(int N) {
		if (N < 1) return new Bound();

		double lb = N/(Dsum + Z + (((N-1)*Dmax) / (1 + Z/(N*Dsum))));
		double ub = Math.min(Xmax, N/(Dsum + Z + (((N-1)*Dave) / (1 + (Z/Dsum)))));
		
		Bound Xtmp = new Bound(lb, ub);
		return Xtmp;
	}
	
	/**
	 *  Calculates throughput BJBs using min/max service demands method.
	 *  @param N The population.
	 */
	private Bound calculateThroughputBJBsMinDemand(int N) {
		if (N < 1) return new Bound();
		// BJBs from Casale's paper.
		double lb = N/(Z + Dsum + Dmax*(N - 1 - Z*Xmin));
		double ub = Math.min(Xmax, N/(Z + Dsum + Dsum*(N - 1 - Z*Xmax)/(stations-delayStations)));
		Bound Xtmp = new Bound(lb, ub);
		return Xtmp;
	}
	
	/**
	 *  Calculates throughput BJBs using min/max service demands method.
	 *  @param N The population.
	 */
	private Bound calculateResponseTimeBJBs(int N) {
		if (N < 1) return new Bound();
		double lb = Math.max(N*Dmax - Z, Dsum + ((N-1)*Dave)/(1 + Z / Dsum));
		double ub = Dsum + ( ((N-1)*Dmax) / (1 + Z/(N*Dsum)) ); 
		lb += Z;
		ub += Z;
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
			/* BJBs do not provide bounds on queue-lengths or residence times of single queues. */
			residenceTimeBounds[k] = new Bound(-1, -1);
			
			// Per station queue length bounds
			queueLengthBounds[k] = new Bound(-1, -1);
		}
		
		// Aggregate utilisation bounds are not possible.
		utilisationBounds[stations] = new Bound(-1, -1);
				
		residenceTimeBounds[stations] = new Bound();
		residenceTimeBounds[stations] = calculateResponseTimeBJBs(N);

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
			System.out.println("\n --- BJB results (N = " + population + ") Using avg demand: " + useAverageDemand + " --- ");
			System.out.println("Runtime: " + timer + " nanoseconds");
			System.out.println("Throughput bounds: " + throughputBounds[stations].LB + " <= X(N) <= " + throughputBounds[stations].UB + "\n");
			System.out.println("Response time bounds: " + residenceTimeBounds[stations].LB + " <= R(N) <= " + residenceTimeBounds[stations].UB + "\n");
			System.out.println("Queue length bounds: " + queueLengthBounds[stations].LB + " <= Q(N) <= " + queueLengthBounds[stations].UB + "\n");
			
			System.out.println("Throughput bounds per station:");
			for (int k = 0; k < stations; k++) {
				System.out.println("Station " + k + ": " + throughputBounds[k].LB + " <= X_" + k + "(N) <= " + throughputBounds[k].UB);
			}
			
			System.out.println("\nUtilsation bounds per station:");
			for (int k = 0; k < stations; k++) {
				System.out.println("Station " + k + ": " + utilisationBounds[k].LB + " <= U_" + k + "(N) <= " + utilisationBounds[k].UB);
			}
			
			System.out.println();
		}
	}
}