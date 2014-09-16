package jmt.engine.math;

public class DistributionDensity {

	private double max;//max value required by the user
	private double min;//min value required by the user
	private int intervals;//number of intervals required by the user
	private double intwidth;//width of each interval
	private double[] distarray;//array containing probability for each interval
	private double sumweight;//sum of weights of samples within the range specified by the user
	private int type;//type of graph required by the user

	/* Constants for type */
	public final static int TYPE_NONE = 0;
	public final static int TYPE_DENSITY_LINE = 1;
	public final static int TYPE_DENSITY_BAR = 2;
	public final static int TYPE_DISTRIBUTION = 3;

	/** Creates a new instance of Distribution_Density class.
	 * @param min the minimum value required by the user
	 * @param max the maximum value specified by the user
	 * @param intervals the number of intervals specified by the user
	 * @param type the type of graph specified by the user
	 */		
	public DistributionDensity(double min, double max, int intervals, int type) {
		this.type = type;
		this.min = min;
		this.max = max;
		this.intervals = intervals;
		sumweight = 0;
		//calculate the width of each interval
		intwidth = (max-min)/intervals;
		distarray = new double[intervals];
		//initializes the probabilities to zero
		for(int i=0; i<intervals; i++) {
			distarray[i] = 0;
		}					
	}

	/** Updates data. Called for each sample. Sample is added only if it is
	 * within the range defined by the user 
	 * @param sample sample to be added.
	 * @param weight sample weight.
	 */
	public void addSample(double sample, double weight) {
		if(sample>=min && sample<max) {
			distarray[(int) ((sample - min)/intwidth)] += weight;
			sumweight += weight;
		}
	}		

	/** Returns maximum value specified by the user.
	 * @return maximum value specified by the user.
	 */
	public double getMax() {
		return max;
	}

	/** Returns minimum value specified by the user.
	 * @return minimum value specified by the user.
	 */
	public double getMin() {
		return min;
	}

	/** Returns number of intervals specified by the user.
	 * @return number of intervals specified by the user.
	 */
	public int getIntervals() {
		return intervals;
	}

	/** Returns type of graph specified by the user.
	 * @return type of graph specified by the user.
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Sets the type of distribution
	 * @param type the type of distribution
	 */
	public void setType(int type) {
		this.type = type;
	}

	/** Returns the width of each interval.
	 * @return the width of each interval.
	 */
	public double getIntWidth() {
		return intwidth;
	}

	/** Returns the array containing probabilities.
	 * @return the array containing probabilities.
	 */
	public double[] getDistArray() {
		double[] ret = new double[intervals];
		for(int i=0; i<intervals; i++) {
			ret[i] = distarray[i]/sumweight;
			
			// Handle distrubution type
			if (type == TYPE_DISTRIBUTION && i > 0) {
				ret[i] += ret[i-1];
			}
		}
		return ret;
	}
}