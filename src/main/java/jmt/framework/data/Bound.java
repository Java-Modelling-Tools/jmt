package jmt.framework.data;

/**
 * Data structure for storing performance measure bounds.
 * @author Ben Homer, 2014.
 */
public class Bound {

	/** The lower bound. */
	public double LB;
	
	/** The upper bound. */
	public double UB;
	
	public Bound() {
		this.LB = 0;
		this.UB = 0;
	}
	
	public Bound(double LB, double UB) {
		this.LB = LB;
		this.UB = UB;
	}
	
	public double[] toArray() {
		double[] arr = {LB, UB};
		return arr;
	}
}
