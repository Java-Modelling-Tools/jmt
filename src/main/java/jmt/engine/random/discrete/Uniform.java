package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.Parameter;

// extend the `AbstractDistribution` to get the random engine.
// implement the `DiscreteDistribution` to define the behavior of this distribution.
public class Uniform extends DiscreteDistribution {

	private int min;
	private int max;

	// Constructor with NO parameter, because the XML initialized the distribution parameter through Parameter.
	public Uniform(){ super(); }

	public Uniform(int a, int b) throws IncorrectDistributionParameterException {
		if (a >= b) {
			throw new IncorrectDistributionParameterException("min(a) must less than max(b)");
		}

		outdated();
		this.min = a;
		this.max = b;
		this.cached = true;
	}

	public Uniform(Integer a, Integer b) throws IncorrectDistributionParameterException {
		this(a.intValue(), b.intValue());
	}

	public boolean updatePar(int a, int b) throws IncorrectDistributionParameterException {
		if (a >= b) {
			throw new IncorrectDistributionParameterException("min(a) must less than max(b)");
		}
		outdated();
		this.min = a;
		this.max = b;
		this.cached = true;
		return true;
	}

	@Override
	public boolean updatePar(Parameter p) throws IncorrectDistributionParameterException{
		if (p instanceof UniformPar && p.check()){
			return updatePar(((UniformPar) p).getMin(), ((UniformPar) p).getMax());
		}
		return false;
	}

	@Override
	public int getUpper() {
		if(cached){
			return max;
		}
		return -1;
	}

	@Override
	public int getlower() {
		if(cached){
			return min;
		}
		return -1;
	}

	public boolean nextBoolean() {
		return engine.raw() > 0.5;
	}

	/**
	 * Uniform distribution do not need to use inverse Transform Sampling.
	 * The random variable can directly generated from the inverse function.
	 */
	@Override
	public int nextRand() {
		if(cached){
			return (int) Math.ceil((min - 1) + ((1 + max - min) * engine.raw()));
		}
		return -1;
	}

	@Override
	public double pmf(int x) {
		if(cached){
			if (x < min || x > max) {
				return 0.0;  //if x is out of bound return 0
			}
			return 1.0 / (max - min + 1);
		}
		return -1.0;
	}

	@Override
	public double cdf(int x) {
		if(cached){
			if (x <= min) {
				return 0.0;  //if x is lower than the min bound return 0
			} else if(x >= max){
				return 1.0;  //if x is greater than the max bound return 1
			}
			return (double) (x - min + 1) / (double)(max - min + 1);
		}
		return -1.0;
	}

	@Override
	public double theorMean() {
		if(cached){
			return (max + min) / 2.0;
		}
		return -1.0;
	}

	@Override
	public double theorVariance() {
		if(cached){
			return (Math.pow((max - min + 1), 2.0) - 1) / 12.0;
		}
		return -1.0;
	}


	@Override
	public int nextRand(Parameter p) throws IncorrectDistributionParameterException{
		if(p instanceof UniformPar && p.check()){
			UniformPar up = (UniformPar) p;
			double min = (double) up.getMin();
			double max = (double) up.getMax();
			return (int) Math.ceil((min - 1) + ((1 + max - min) * engine.raw()));
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: the *max* parameter must be greater than the *min* one\n"+
					"Error: the Parameter must be the `UniformPar`");
		}
	}

	@Override
	public double pmf(int x, Parameter p) throws IncorrectDistributionParameterException{
		if(p instanceof UniformPar && p.check()){
			UniformPar up = (UniformPar) p;
			double min = (double) up.getMin();
			double max = (double) up.getMax();
			if (x < min || x > max) {
				return 0.0;  //if x is out of bound return 0
			}
			return 1.0 / (max - min + 1);
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: the *max* parameter must be greater than the *min* one\n"+
					"Error: the Parameter must be the `UniformPar`");
		}
	}

	@Override
	public double cdf(int x, Parameter p) throws IncorrectDistributionParameterException{
		if(p instanceof UniformPar && p.check()){
			UniformPar up = (UniformPar) p;
			double min = (double) up.getMin();
			double max = (double) up.getMax();
			if (x <= min) {
				return 0.0;  //if x is lower than the min bound return 0
			} else if(x >= max){
				return 1.0;  //if x is greater than the max bound return 1
			}
			return  (x - min + 1) / (max - min + 1);
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: the *max* parameter must be greater than the *min* one\n"+
					"Error: the Parameter must be the `UniformPar`");
		}
	}

	@Override
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException{
		if(p instanceof UniformPar && p.check()){
			UniformPar up = (UniformPar) p;
			double min = (double) up.getMin();
			double max = (double) up.getMax();
			return (max + min) / 2.0;
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: the *max* parameter must be greater than the *min* one\n"+
					"Error: the Parameter must be the `UniformPar`");
		}
	}

	@Override
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException{
		if(p instanceof UniformPar && p.check()){
			UniformPar up = (UniformPar) p;
			double min = (double) up.getMin();
			double max = (double) up.getMax();
			return (Math.pow((max - min + 1), 2.0) - 1) / 12.0;
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: the *max* parameter must be greater than the *min* one\n"+
					"Error: the Parameter must be the `UniformPar`");
		}
	}
}
