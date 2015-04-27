package jmt.jmva.analytical;

import java.io.PrintWriter;

import jmt.framework.data.Bound;

/**
 * Solver is an abstract class, which contains the methods to initialise and 
 * calculate performance bounds for models with multiple classes.<br>
 * @author Ben Homer, 2014.
 */
public abstract class BoundsSolverMulti {

	/**---------------CONSTANTS DEFINITION------------------------*/

	/** constant for Load Dependent service center  */
	public final static int LD = 0;

	/** constant for Load Independent service center  */
	public final static int LI = 1;

	/** constant for delay center*/
	public final static int DELAY = 2;

	/** constant for Open Class */
	public final static int OPEN_CLASS = 0;

	/** constant for Closed Class  */
	public final static int CLOSED_CLASS = 1;
	
	/** Determines whether the results are printed to the console */
	public final static boolean PRINT_OUTPUT = false;

	/**---------------MODEL DEFINITION------------------------*/

	/** number of service centers */
	protected int stations = 0;

	/** number of classes  */
	protected int classes = 0;

	/** array of names of service centers*/
	protected String[] name;

	/** array of types of service centers */
	protected int[] type;

	/** array of number of servers for each service center */
	protected int[] servers;

	/** service times for each service station, class, population<br>
	 * [station] [class] [population] */
	protected double[][][] servTime;

	/** visits for each service station, class<br>
	 * [station] [class] */
	protected double[][] visits;
	
	/** The per class max populations. <br> [class] */
	protected int[] populations;

	/**---------------MODEL SOLUTION------------------------*/

	/** Bounds on throughput for each service station, class. Final values in array are aggregates.<br>
	 * [station] [class] */
	protected Bound[][] throughputBounds;

	/** Bounds on utilization for each service station, class. Final values in array are aggregates.<br>
	 * [station] [class] */
	protected Bound[][] utilizationBounds;

	/** Bounds on queue length for each service station, class. Final values in array are aggregates.<br>
	 * [station] [class] */
	protected Bound[][] queueLengthBounds;

	/** Bounds on residence time for each service station, class. Final values in array are aggregates.<br>
	 * [station] [class] */
	protected Bound[][] residenceTimeBounds;

	/** Printer writer: print formatted representations of objects to a text-output stream */
	protected PrintWriter pw = new PrintWriter(System.out, true);
	
	/** Total populations over all classes. */
	protected double Ntot;
	
	/** Total delay over all classes. */
	protected double Ztot;
	
	/** Total demand over all classes. */
	protected double Dtot;
	
	/** Min. demand per class. */
	protected double Dmin[];
	
	/** Max. demand per class. */
	protected double Dmax[]; // per class
	
	/** Sum of demands per class. */
	protected double Dsum[];
	
	/** Min. throughput per class. */
	protected double Xmin[];
	
	/** Max/ throughput per class. */
	protected double Xmax[];
	
	/** Average demand per class. */
	protected double Dave[];
	
	/** Delay per class. */
	protected double Z[];
	
	/** Demands per station per class. */
	protected double D[][];
	

	/** Returns the throughput bounds of the given class for the requested station
	 *  @param  cent    the number of the service center
	 *  @param  cls    the customer class
	 *  @return the throughput bounds
	 */
	public Bound getThroughputBounds(int cent, int cls) {
		return throughputBounds[cent][cls];
	}

	/**
	 * Returns the throughput bounds of each class for each station
	 * @return the throughput[center][class] bounds matrix for the system
	 */
	public Bound[][] getThroughputBounds() {
		return throughputBounds;
	}

	/** Returns the utilization bounds of the given class for the requested station
	 *  @param  cent    the number of the service center
	 *  @param  cls    the customer class
	 *  @return the utilization bounds
	 */
	public Bound getUtilizationBounds(int cent, int cls) {
		return utilizationBounds[cent][cls];
	}

	/**
	 * Returns the throughput bounds of each class for each station
	 * @return the utilization[center][class] bounds matrix for the system
	 */
	public Bound[][] getUtilizationBounds() {
		return utilizationBounds;
	}

	/** Returns the queue length bounds of the given class for the requested station
	 *  @param  cent    the number of the service center
	 *  @param  cls    the customer class
	 *  @return the queue length bounds
	 */
	public Bound getQueueLenBounds(int cent, int cls) {
		return queueLengthBounds[cent][cls];
	}

	/**
	 *  Returns the queue length bounds of each class for each station
	 *  @return the queue length [center][class] bounds matrix for the system
	 */
	public Bound[][] getQueueLenBounds() {
		return queueLengthBounds;
	}

	/** Returns the residence time bounds of the given class for the requested station
	 *  @param  cent    the number of the service center
	 *  @param  cls    the customer class
	 *  @return the residence time bounds
	 */
	public Bound getResTimeBounds(int cent, int cls) {
		return residenceTimeBounds[cent][cls];
	}

	/**
	 * Returns the residence time bounds of each class for each station
	 * @return the residence times[center][class] bounds matrix for the system
	 */
	public Bound[][] getResTimeBounds() {
		return residenceTimeBounds;
	}

	/**
	 * Creates a multiclasss solver
	 * @param stations number of stations.
	 * @param classes number of classes.
	 */
	public BoundsSolverMulti(int stations, int classes) {
		this.stations = stations;
		this.classes = classes;
		
		populations = new int[classes];
		name = new String[stations];
		type = new int[stations];
		servTime = new double[stations][classes][];
		visits = new double[stations][classes];

		throughputBounds = new Bound[stations+1][classes+1];
		queueLengthBounds = new Bound[stations+1][classes+1];
		utilizationBounds = new Bound[stations+1][classes+1];
		residenceTimeBounds = new Bound[stations+1][classes+1];
	}

	/** Initializes the Multiclass solver with the model parameters.
	 *  @param  n   array of names of service centers.
	 *  @param  t   array of the types (LD or LI) of service centers.
	 *  @param  s   matrix of service time of the service centers.
	 *  @param  v   matrix of visits to the service centers.
	 *  @param pop  array of populations of each class
	 *  @return true if the operation is completed with success
	 */
	public boolean input(String[] n, int[] t, double[][][] s, double[][] v, int[] pop) {

		if ((n.length != stations) || (t.length != stations) || (s.length != stations) || (v.length != stations)) {
			return false; // wrong input.
		}

		System.arraycopy(n, 0, name, 0, stations);
		System.arraycopy(t, 0, type, 0, stations);

		for (int i = 0; i < stations; i++) {

			System.arraycopy(v[i], 0, visits[i], 0, classes);

			for (int j = 0; j < classes; j++) {
				if (t[i] != LD) {
					servTime[i][j] = new double[1];
				}
				System.arraycopy(s[i][j], 0, servTime[i][j], 0, s[i][j].length);
			}
		}
		
		System.arraycopy(pop, 0, populations, 0, pop.length);
		return true;
	}

	/**
	 * Must be implemented to create a multi class model solver.
	 */
	public abstract void solve();

	/**
	 * Prints the results to the console.
	 */
	public abstract void printResults();
}
