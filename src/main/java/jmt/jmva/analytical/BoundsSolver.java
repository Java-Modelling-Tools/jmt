package jmt.jmva.analytical;

import jmt.framework.data.Bound;

/**
 * Solver is an abstract class, which contains the methods to initialise and 
 * calculate performance bounds for models with a single class of jobs.<br>
 * @author Ben Homer, 2014.
 */
public abstract class BoundsSolver {

	/**--------------CONSTANTS DEFINITION------------------------*/

	/** constant for Load Dependent service center  */
	public final static int LD = 0;

	/** constant for Load Independent service center  */
	public final static int LI = 1;

	/** constant for delay center*/
	public final static int DELAY = 2;
	
	/** Determines whether the results are printed to the console */
	public final static boolean PRINT_OUTPUT = false;

	/**---------------MODEL DEFINITION------------------------*/

	/**number of resources*/
	protected int stations = 0;

	/** array containing the names of the stations  */
	protected String[] name;

	/** array containing the types of the stations  */
	protected int[] type;

	/** array of array (i.e. matrix) containing the service rates of the service centers
	 * [station] [population]  */
	protected double[][] servTime;

	/** array containing the number of visits to each service center  */
	protected double[] visits;
	
	/** The total number of customers in the system */
	protected int population;
	
	/**---------------CALCULATED VALUES------------------------*/
	
	/** The per station service demands <br>
	 * [station] */
	protected double[] D;
	
	/** The max service demand */
	protected double Dmax;
	
	/** The minimum service demand. */
	protected double Dmin;
	
	/** The sum of the service demands */
	protected double Dsum;
	
	/** The average of the service demands */
	protected double Dave;
	
	/** The total delay from delay servers */
	protected double Z;
	
	/** The max throughput */
	protected double Xmax;
	
	/** The min throughput */
	protected double Xmin;

	/**---------------MODEL SOLUTION------------------------*/
	
	/** Throughput bounds per station, final value in array is aggregate.<br>
	 * [station] */
	protected Bound[] throughputBounds;
	
	/** Residence time bounds per station, final value in array is aggregate.<br>
	 * [station] */
	protected Bound[] residenceTimeBounds;

	/** Queue length bounds per station, final value in array is aggregate.<br>
	 * [station] */
	protected Bound[] queueLengthBounds;
	
	/** Utilisation bounds per station, final value in array is aggregate.<br>
	 * [station] */
	protected Bound[] utilisationBounds;
	
	/** Gets the per station throughput bounds. */
	public Bound[] getThroughputBounds() {
		return throughputBounds;
	}
	
	/** Gets the per station residence time bounds. */
	public Bound[] getResidenceTimeBounds() {
		return residenceTimeBounds;
	}
	
	/** Gets the per station queue length bounds. */
	public Bound[] getQueueLengthBounds() {
		return queueLengthBounds;
	}
	
	/** Gets the per station utilisation bounds. */
	public Bound[] getUtilisationBounds() {
		return utilisationBounds;
	}
	
	/**
	 *  Creates a new BoundsSolver.
	 *  @param  population Total number of customers.
	 *  @param  stations Total number of service centers (stations).
	 */
	public BoundsSolver(int population, int stations) {
		this.population = population;
		this.stations = stations;
		
		name = new String[stations];
		type = new int[stations];
		//one service time for each possible population (from 1 to population)
		//position 0 is used for LI stations
		servTime = new double[stations][population + 1];
		visits = new double[stations];
		
		// Per station measures
		this.throughputBounds = new Bound[stations+1];
		this.residenceTimeBounds = new Bound[stations+1];
		this.queueLengthBounds = new Bound[stations+1];
		this.utilisationBounds = new Bound[stations+1];
	}
	
	/**
	 *  Initializes the solver with the model parameters.
	 *  It must be called before trying to solve the model.
	 *  @param  n   array of n of service centers.
	 *  @param  t   array of the types (LD or LI) of service centers.
	 *  @param  s   matrix of service time of the service centers.
	 *  @param  v   array of visits to the service centers.
	 *  @return true if the operation is completed with success
	 */
	public boolean input(String[] n, int[] t, double[][] s, double[] v) {
		if ((n.length != stations) || (t.length != stations) || (s.length != stations) || (v.length != stations)) {
			return false; // wrong input.
		}
		for (int i = 0; i < stations; i++) {
			name[i] = n[i];
			type[i] = t[i];
			visits[i] = v[i];
			if (type[i] != BoundsSolver.LD) {
				//service centre is LI or DELAY
				servTime[i][0] = s[i][0];
			} else {
				//LD center
				//the input method is overridden in SolverSingleClosedMVA, where LD
				//centers are allowed
				return false;
			}
		}
		return true;
	}

	/**
	 * Must be implemented to create a single class model solver.
	 */
	public abstract void solve();
	
	/**
	 * Prints the results to the console.
	 */
	public abstract void printResults();
}