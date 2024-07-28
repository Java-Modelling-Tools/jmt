package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.Parameter;

public class FixProbability extends DiscreteDistribution{

	private double[] p;
	private final int size;
	private int total;

	public FixProbability(Object[] count){
		this.size = count.length;
		this.p = new double[this.size];

		for(int i=0; i<this.size; i++){
			this.total += ((Integer)(count[i])).intValue();
		}
		for(int i=0; i<this.size; i++){
			this.p[i] = ((Integer)(count[i])).doubleValue() / this.total;
		}
		outdated();
		this.cached = true;
	}

	@Override
	public boolean updatePar(Parameter p) throws IncorrectDistributionParameterException {
		return false;
	}

	@Override
	public int getUpper() {
		return this.size;
	}

	@Override
	public int getlower() {
		return 1;
	}

	@Override
	public int nextRand() {
		return this.inverseTransformSampling(1, this.size);
	}

	@Override
	public double pmf(int x) {
		return p[x];
	}

	@Override
	public double cdf(int x) {
		double cdf = 0.0;
		for(int i=0; i<x; i++){
			cdf += p[i];
		}
		return cdf;
	}

	@Override
	public double theorMean() {
		double mean = 0.0;
		for( int i=0; i<this.size; i++){
			mean += p[i]*(i+1);
		}
		return mean;
	}

	@Override
	public double theorVariance() {
		double var = 0.0;
		double mean = theorMean();
		for( int i=0; i<this.size; i++){
			var += Math.pow(p[i] - mean, 2) * total;
		}
		return var;
	}

	@Override
	public int nextRand(Parameter p) throws IncorrectDistributionParameterException {
		return 0;
	}

	@Override
	public double pmf(int x, Parameter p) throws IncorrectDistributionParameterException {
		return 0;
	}

	@Override
	public double cdf(int x, Parameter p) throws IncorrectDistributionParameterException {
		return 0;
	}

	@Override
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		return 0;
	}

	@Override
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		return 0;
	}
}
