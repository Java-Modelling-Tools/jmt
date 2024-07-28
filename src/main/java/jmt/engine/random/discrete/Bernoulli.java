package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.Parameter;

// extend the `AbstractDistribution` to get the random engine.
// implement the `DiscreteDistribution` to define the behavior of this distribution.
public class Bernoulli extends DiscreteDistribution {

	private double probability;

	// Constructor with NO parameter, because the XML initialized the distribution parameter through Parameter.
	public Bernoulli() { super(); }

	public Bernoulli(double p) throws IncorrectDistributionParameterException {
		if ( p<0 || p>1 ) {
			throw new IncorrectDistributionParameterException("The probability should belong to [0,1]");
		}
		outdated();
		this.probability = p;
		this.cached = true;
	}
	public Bernoulli(Double p) throws IncorrectDistributionParameterException {
		this(p.doubleValue());
	}

	public boolean updatePar(double p) throws IncorrectDistributionParameterException {
		if ( p<0 || p>1 ) {
			throw new IncorrectDistributionParameterException("The probability should belong to [0,1]");
		}
		outdated();
		this.probability = p;
		this.cached = true;
		return true;
	}

	@Override
	public boolean updatePar(Parameter p) throws IncorrectDistributionParameterException{
		if (p instanceof BernoulliPar && p.check()){
			return updatePar(((BernoulliPar) p).getProbability());
		}
		return false;
	}

	@Override
	public int getUpper() {
		return 1;
	}

	@Override
	public int getlower() {
		return 0;
	}


	@Override
	public int nextRand() {
		if(cached){
			return engine.raw()<probability ? 1 : 0;
		}
		return -1;
	}

	@Override
	public double pmf(int x) {
		if(cached){
			if(x==1){
				return probability;
			} else if (x==0){
				return (1-probability);
			} else{
				return 0.0;
			}
		}
		return -1.0;
	}

	@Override
	public double cdf(int x) {
		if(cached){
			if(x<0){
				return 0.0;
			}else if(x<1){		// x belongs to [0,1)
				return (1-probability);
			}else {				// x belongs to [1,infinity)
				return 1.0;
			}
		}
		return -1.0;
	}

	@Override
	public double theorMean() {
		if(cached){
			return probability;
		}
		return -1.0;
	}

	@Override
	public double theorVariance() {
		if(cached){
			return probability * (1 - probability);
		}
		return -1.0;
	}

	@Override
	public int nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if(p instanceof BernoulliPar && p.check()){
			double probability = ((BernoulliPar) p).getProbability();
			return engine.raw()<probability ? 1 : 0;
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: The probability should belong to [0,1]\n"+
					"Error: the Parameter must be the `BernoulliPar`");
		}
	}

	@Override
	public double pmf(int x, Parameter p) throws IncorrectDistributionParameterException {
		if(p instanceof BernoulliPar && p.check()){
			double probability = ((BernoulliPar) p).getProbability();
			if(x==1){
				return probability;
			} else if (x==0){
				return (1-probability);
			} else{
				return 0.0;
			}
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: The probability should belong to [0,1]\n"+
					"Error: the Parameter must be the `BernoulliPar`");
		}
	}

	@Override
	public double cdf(int x, Parameter p) throws IncorrectDistributionParameterException {
		if(p instanceof BernoulliPar && p.check()){
			double probability = ((BernoulliPar) p).getProbability();
			if(x<0){
				return 0.0;
			}else if(x<1){		// x belongs to [0,1)
				return (1-probability);
			}else {				// x belongs to [1,infinity)
				return 1.0;
			}
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: The probability should belong to [0,1]\n"+
					"Error: the Parameter must be the `BernoulliPar`");
		}
	}

	@Override
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if(p instanceof BernoulliPar && p.check()){
			return ((BernoulliPar) p).getProbability();
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: The probability should belong to [0,1]\n"+
					"Error: the Parameter must be the `BernoulliPar`");
		}
	}

	@Override
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if(p instanceof BernoulliPar && p.check()){
			double probability = ((BernoulliPar) p).getProbability();
			return probability * (1 - probability);
		} else {
			throw new IncorrectDistributionParameterException(
					"Error: The probability should belong to [0,1]\n"+
					"Error: the Parameter must be the `BernoulliPar`");
		}
	}
}
