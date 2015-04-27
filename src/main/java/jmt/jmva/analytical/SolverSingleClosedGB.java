package jmt.jmva.analytical;

import java.util.ArrayList;

import jmt.framework.data.Bound;

/**
 * Class that implements the Geometric Bounding methods for a single-class closed models.
 * See G.Casale's 2008 paper - "Geometric Bounds: A Noniterative Analysis Technique for Closed Queueing Networks"
 * for more information.
 * Queue length bounds and throughput bounds are calculated as described in the paper.
 * Other bounds are then calculated using the MVA equations.
 * @author Ben Homer, 2014.
 */
public final class SolverSingleClosedGB extends BoundsSolver {

	/** Variable for calculating algorithm runtime. */
	protected long timer;
	
	/** The number of stations with the maximum service demand **/
	protected double mmax;
	
	/** Stores min throughput for population N-1. */
	protected double Xmin1;
	
	/** Stores max throughput for population N-1. */
	protected double Xmax1;
	
	/** The number of delay stations. */
	private int delayStations;
	
	/**
	 * Creates a new SolverSingleClosedGB
	 *  @param  population Total number of customers
	 *  @param  stations Total number of service centers (stations)
	 */
	public SolverSingleClosedGB(int population, int stations) {
		super(population, stations);
	}
	
	/**
	 * Solves the model to get the bounds on all performance measures.
	 */
	@Override
	public void solve() {
		long start = System.nanoTime();
		initialSetup();
		
		queueLengthBounds = calculatePerStationQueueLengthGBs(population);
		//if (Z==0) {
			throughputBounds[stations] = calculateSystemThroughputGBs(population);
		//} else {
			//throughputBounds = calculateSystemThroughputGSRBs(population);
		//}
		calculateAdditionalBounds(population);
		
		long end = System.nanoTime();
		timer = end - start;
		printResults();
	}
	
	/** 
	 * Initial calculation of required values.
	 */
	private void initialSetup() {
		// N.B. we compute Dmin and Dave so doesn't have to be recomputed in BJB solver,
		// to get min and max throughputs.
		Dmax = Dsum = Z = 0;
		Dmin = Double.MAX_VALUE;
		D = new double[stations];
		for (int k = 0; k < stations; k++) {
			D[k] = visits[k] * servTime[k][0];
			
			if (type[k] == DELAY) {
				Z += D[k];
				delayStations++;
			} else { 
				Dsum = (Dsum*1000 + D[k]*1000) / 1000;
				if (D[k] > Dmax) {
					Dmax = D[k];
				}
				if (D[k] < Dmin) {
					Dmin = D[k];
				}
			}
		}
		
		Dave = Dsum / (stations-delayStations);
		
		// Calculate number of stations with max service demand.
		mmax = 0;
		for (int k = 0; k < stations; k++) {
			if (D[k] == Dmax && type[k] != DELAY) mmax++;
		}
		assert mmax > 0;
		
		// X(N) <= X+ <= Xmax and 0 <= X- <= X(N). Set X+ using BJB (average version).
		SolverSingleClosedBJB BJBSolver = new SolverSingleClosedBJB(population, stations, true);
		boolean success = BJBSolver.input(super.name, super.type, super.servTime, super.visits);
		if (success) {
			// Only use BJBSolver to calculate throughput bounds.
			BJBSolver.setCalculateAll(false);
			BJBSolver.setInitialMeasures(Dmin, Dmax, Dave, Dsum, Z);
			BJBSolver.solve();
			Xmin = BJBSolver.throughputBounds[stations].LB;
			Xmax = BJBSolver.throughputBounds[stations].UB;
			BJBSolver.population--;
			BJBSolver.solve();
			Xmin1 = BJBSolver.throughputBounds[stations].LB;
			Xmax1 = BJBSolver.throughputBounds[stations].UB;
		}
		else {
			Xmax = 1.0/Dmax;
			Xmin = 0;
		}
	}
	
	/** Calculates per station queue length GBs as described in paper.
	 *  @param N The population.
	 */
	private Bound[] calculatePerStationQueueLengthGBs(int N) {
		if (N != population && N != population-1) {
			throw new UnsupportedOperationException("Cannot compute queue lengths for population " + N);
		}
		
		// Get min/max throughputs depending on if calculating for population N or N-1.
		double xmax = (N == this.population) ? Xmax : Xmax1;
		double xmin = (N == this.population) ? Xmin : Xmin1;
		
		Bound[] Qtmp = new Bound[stations+1];
		Qtmp[stations] = new Bound(-1, -1);
		double Qu_sum = 0.0;
		double Ql_sum = 0.0;
		ArrayList<Integer> maxStationIdxs = new ArrayList<Integer>();
		
		for (int k = 0; k < stations; k++) {
			if (type[k] == DELAY) {
				double R = D[k];
//				double x = N/(Z + R);
				Qtmp[k] = new Bound(xmin*R, xmax*R);
			}
			else if (D[k] < Dmax) {
				double ykn = D[k]*N/(Z + Dsum + Dmax*N);
				double Ykn = D[k]*xmax;
				Qtmp[k] = new Bound();
				Qtmp[k].LB = (ykn - Math.pow(ykn, N+1))/(1-ykn); // LB
				Qtmp[k].UB = (Ykn - Math.pow(Ykn, N+1))/(1-Ykn); // UB
				Ql_sum += Qtmp[k].LB;
				Qu_sum += Qtmp[k].UB;
			}
			else { // D_k == D_max
				// Prestore indices for stations with max service demand to make next pass faster.
				maxStationIdxs.add(k);
			}
		}
		
		// Calculate queue length bounds for stations with max service demand.
		if (!maxStationIdxs.isEmpty()) {
			double maxLB = (N - Z*xmax - Qu_sum)/mmax;
			double maxUB = (N - Z*xmin - Ql_sum)/mmax;
			if (maxLB < 0 ) maxLB = 0;
			for (int k : maxStationIdxs) {
				Qtmp[k] = new Bound(maxLB, maxUB);
			}
		}
		
		// Replace any NaNs with 0s.
		Qtmp = replaceNaNs(Qtmp);
		return Qtmp;
	}
	
	/** Calculates whole system throughput GBs as described in paper.
	 *  @param N Number of customers.
	 */
	private Bound calculateSystemThroughputGBs(int N) {
		if (N != population) {
			throw new UnsupportedOperationException("Cannot compute throughputs for population " + N);
		}
		Bound Xtmp = new Bound();
		double sumLB = 0.0;
		double sumUB = 0.0;
		
		Bound[] Qtmp = calculatePerStationQueueLengthGBs(N-1);
		for (int k = 0; k < stations; k++) {
			if (type[k] == DELAY) continue;
			double diff = (Dmax*1000 - D[k]*1000)/1000;
			sumLB += diff*Qtmp[k].LB;
			sumUB += diff*Qtmp[k].UB;
		}
		
		Xtmp.LB = N/(Z + Dsum + Dmax*(N - 1 - Z*Xmin1) - sumLB); // LB
		Xtmp.UB = N/(Z + Dsum + Dmax*(N - 1 - Z*Xmax1) - sumUB); // UB
		
		// Replace any NaNs with 0s.
		Xtmp = replaceNaNs(Xtmp);
		return Xtmp;
	}
	
	/**
	 * Computes system throughput Geometric Square-Roots Bounds.
	 * To be used when delay Z > 0.
	 * N.B. Unused as does not give better results than standard bounds at the moment.
	 * @param N The system population.
	 * @return The systems throughput bounds.
	 */
	@SuppressWarnings("unused")
	private Bound calculateSystemThroughputGSRBs(int N) {
		assert N >= 0;
		Bound Xtmp = new Bound();
		double sumLB = 0.0;
		double sumUB = 0.0;
		
		Bound[] Qtmp = calculatePerStationQueueLengthGBs(N-1);
		
		for (int k = 0; k < stations; k++) {
			if (type[k] == DELAY) continue;
			if (D[k] < Dmax) {
				double d = Dmax - D[k];
				sumLB += d * Qtmp[k].LB;
				sumUB += d * Qtmp[k].UB;
			}
		}
		
		double b = Z + Dsum + Dmax*(N-1);
		double bnLB = b - sumLB;
		double bnUB = b - sumUB;
		
		Xtmp.LB = 2*N/(bnLB + Math.sqrt(Math.pow(bnLB, 2) - 4*Z*Dmax*(N-1)));
		Xtmp.UB = 2*N/(bnUB + Math.sqrt(Math.pow(bnUB, 2) - 4*Z*Dmax*N));
		
		// Replace any NaNs with 0s.
		Xtmp = replaceNaNs(Xtmp);
		return Xtmp;
	}
	
	/**
	 * Calculates the additional bounds on performance measures.
	 * @param N The population.
	 * N.B. Only results that actually matter are per station results as all aggregate results
	 * are recomputed on client side (in ExactModel).
	 */
	private void calculateAdditionalBounds(int N) {
		Bound[] Qtmp = calculatePerStationQueueLengthGBs(N-1);
		
		for (int k = 0; k < stations; k++) {
			// Per station utilisation bounds
			utilisationBounds[k] = new Bound(D[k]*throughputBounds[stations].LB, D[k]*throughputBounds[stations].UB); 
			
			// Per station throughput bounds (using Forced Flow Law)
			throughputBounds[k] = new Bound(visits[k]*throughputBounds[stations].LB, visits[k]*throughputBounds[stations].UB); 

			// Per station residence time bounds
			residenceTimeBounds[k] = new Bound(
					type[k] == DELAY ? D[k] : D[k]*(1 + Qtmp[k].LB),
					type[k] == DELAY ? D[k] : D[k]*(1 + Qtmp[k].UB));

			// Per station residence time bounds
			/* stResidenceTimeBounds[k] = new Bound (
					stQueueLengthBounds[k].LB/throughputBounds.LB, //D[k]*(1 + Qtmp[k][0]);
					stQueueLengthBounds[k].UB/throughputBounds.UB); //D[k]*(1 + Qtmp[k][1]);
			
			responseTimeBounds.LB += stResidenceTimeBounds[k].LB;
			responseTimeBounds.UB += stResidenceTimeBounds[k].UB;
			
			queueLengthBounds.LB += stQueueLengthBounds[k].LB;
			queueLengthBounds.UB += stQueueLengthBounds[k].UB; */
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
		
		// System queue length bounds
		/*queueLengthBounds.LB = throughputBounds.LB * responseTimeBounds.LB;
		queueLengthBounds.UB = throughputBounds.UB * responseTimeBounds.UB;
		queueLengthBounds.LB = N; // - Z*throughputBounds[1];
		queueLengthBounds.UB = N;// - Z*throughputBounds[0]; */
	}
	
	/** Replaces NaN and Infinite values with zeros.
	 * @param bound The bound to check.
	 * @return Tha bound with any NaN or infinite values removed.
	 */
	private Bound replaceNaNs(Bound bound) {
		if (Double.isNaN(bound.LB) || Double.isInfinite(bound.LB)) {
			bound.LB = 0;
		}
		
		if (Double.isNaN(bound.UB) || Double.isInfinite(bound.UB)) {
			bound.UB = 0;
		}
		
		return bound;
	}
	
	/** Replaces NaNs and infinite values in a bound array.
	 * @param arr The array of Bound objects.
	 * @return The Bound array with any NaNs or infinite values removed.
	 */
	private Bound[] replaceNaNs(Bound[] arr) {
		for (int i = 0; i < arr.length; i++) {
			arr[i] = replaceNaNs(arr[i]);
		}
		return arr;
	}
	
	/**
	 * Prints the results to the console.
	 */
	public void printResults() {
		if (PRINT_OUTPUT) {
			System.out.println("\n --- GB results (N = " + population + ") --- ");
			System.out.println("Runtime: " + timer + " nanoseconds");
			System.out.println("Throughput bounds: " + throughputBounds[stations].LB + " <= X(N) <= " + throughputBounds[stations].UB + "\n");
			System.out.println("Response time bounds: " + residenceTimeBounds[stations].LB + " <= R(N) <= " + residenceTimeBounds[stations].UB + "\n");
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