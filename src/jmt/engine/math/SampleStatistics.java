/**    
  * Copyright (C) 2014, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.

  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */
package jmt.engine.math;

public class SampleStatistics {
	
	private double sumweight;// sum of weights of all data
	private double moment1;//1st moment(mean) of all data
	private double moment2;//2nd moment of all data
	private double moment3;//3rd moment of all data
	private double moment4;//4th moment of all data
	private double var;//variance of all data
	private double sd;//standard deviation of all data
	private double coefvar;//coefficient of variation of all data
	private double skew;//skewness of all data
	private double kurtosis;//kurtosis of all data
	private double n;//number of data
	private double max;//the greatest sample of all data inserted
	private double min;//the smallest sample of all data inserted
	
	private double s1weight;//sum of (sample)*weight
	private double s2weight;//sum of (sample^2)*weight
	private double s3weight;//sum of (sample^3)*weight
	private double s4weight;//sum of (sample^4)*weight
	
	private double minSimTime;
	private double maxSimTime;

	/** Creates a new instance of SampleStatistics */
	public SampleStatistics() {
		initialize();
	}
	
	/** initializes a new SampleStatistics 	 */
	private void initialize() {
		sumweight = 0;
		moment1 = 0;
		moment2 = 0;
		moment3 = 0;
		moment4 = 0;
		var = 0;
		sd = 0;
		coefvar = 0;
		skew = 0;
		kurtosis = 0;
		n = 0;
		max = 0;
		min = Double.MAX_VALUE;
		s1weight = 0;
		s2weight = 0;
		s3weight = 0;
		s4weight = 0;
		minSimTime = Double.MAX_VALUE;
		maxSimTime = 0;
	}
	
	/** updates the max, min, sum of weights with the new value.
	 *  @param  sample the value of the new sample
	 *  @param  weight the weight of the new sample
	 *  @param  simulationTime the simulation time for the new sample
	 */
	
	public void putNewSample(double sample, double weight, double simulationTime) {
		if (weight == 0) {
			return;
		}
		
		// updates the maximum and the minimum value of the sequence
		max= Math.max(max, sample * weight);
		min = Math.min(min, sample * weight);
		// updates the sum of weights of the values of the sequence
		sumweight = sumweight + weight;
		// updates the sum of weights multiplied by appropriate power of samples of the sequence
		s1weight = s1weight + weight*sample;
		s2weight = s2weight + weight*Math.pow(sample,2);
		s3weight = s3weight + weight*Math.pow(sample,3);
		s4weight = s4weight + weight*Math.pow(sample,4);
		//updates the number of samples
		n++;
		//updates the simulation time
		minSimTime = Math.min(minSimTime,simulationTime);
		maxSimTime = Math.max(maxSimTime, simulationTime);
		
		//calculates the statistical parameters until that point
		calcStatistics();
		
	}
	
	/** computes all the statistical parameters, using their respective mathematical formulae
	  */
	private void calcStatistics()
	{
		moment1 = s1weight/sumweight;
		moment2 = s2weight/sumweight;
		moment3 = s3weight/sumweight;
		moment4 = s4weight/sumweight;
		
		var = moment2 - Math.pow(moment1, 2);
		sd = Math.sqrt(var);
		coefvar = sd/moment1;
		skew = (moment3 - (3*moment1*Math.pow(sd, 2)) - Math.pow(moment1, 3))/Math.pow(sd, 3);
		kurtosis = (moment4 - (4*moment3*moment1) + (6*moment2*Math.pow(moment1,2)) - (3*Math.pow(moment1, 4)))/(Math.pow(sd, 4)) - 3;
	}
	
	/** gets the sample mean
	 *  @return the actual sample mean
	 */
	public double getMean() {
		return moment1;
	}
	
	/** gets the sample 2nd moment
	 *  @return the actual 2nd moment
	 */
	public double getMoment2() {
		return moment2;
	}
	
	/** gets the sample 3rd moment
	 *  @return the actual 3rd moment
	 */
	public double getMoment3() {
		return moment3;
	}
	
	/** gets the sample 4th moment
	 *  @return the actual 4th moment
	 */
	public double getMoment4() {
		return moment4;
	}
	
	/** gets the sample variance
	 *  @return the actual variance
	 */
	public double getVariance() {
		return var;
	}
	
	/** gets the sample standard deviation
	 *  @return the actual standard deviation
	 */
	public double getStandardDeviation() {
		return sd;
	}
	
	/** gets the sample coefficient of variation
	 *  @return the actual coefficient of variation
	 */
	public double getCoefficienfOfVariation() {
		return coefvar;
	}
	
	/** gets the sample skewness
	 *  @return the actual skewness
	 */
	public double getSkew() {
		return skew;
	}
	
	/** gets the sample kurtosis
	 *  @return the actual kurtosis
	 */
	public double getKurtosis() {
		return kurtosis;
	}

	/** gets the greatest value
	 *  @return the actual max value
	 */
	public double getMax() {
		return max;
	}

	/** gets the smallest value
	 *  @return the actual min value
	 */
	public double getMin() {
		if (Double.MAX_VALUE == min) {
			return 0.0;
		} else {
			return min;
		}
	}
	
	/** gets the number of samples
	 *  @return the actual number of samples
	 */
	public double getSamples() {
		return n;
	}
	
	/**
	 * @return the minimum simulation time
	 */
	public double getMinSimTime() {
		return minSimTime;
	}
	
	/**
	 * @return the maximum simulation time
	 */
	public double getMaxSimTime() {
		return maxSimTime;
	}
	
	/** creates a string with the calculated parameters of the sequence monitored
	 *  by SampleStatistics
	 *  @return a string
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(getClass().getName());
		buf.append("\n--------------------------");
		buf.append("\nNumber of samples : " + getSamples());
		buf.append("\nLargest Sample    : " + getMax());
		buf.append("\nSmallest Sample   : " + getMin());
		buf.append("\nMean of samples   : " + getMean());
		buf.append("\n2nd Moment of samples   : " + getMoment2());
		buf.append("\n3rd Moment of samples   : " + getMoment3());
		buf.append("\n4th Moment of samples   : " + getMoment4());
		buf.append("\nVariance of samples   : " + getVariance());
		buf.append("\nStandard Deviation of samples   : " + getStandardDeviation());
		buf.append("\nCoefficient of Variation of samples   : " + getCoefficienfOfVariation());
		buf.append("\nSkewness of samples   : " + getSkew());
		buf.append("\nKurtosis of samples   : " + getKurtosis());
		return buf.toString();
	}
	
}