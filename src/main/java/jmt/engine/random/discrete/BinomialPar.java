package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.AbstractParameter;

public class BinomialPar extends AbstractParameter {

	private int n;
	private double probability;

	public BinomialPar(int n, double probability) throws IncorrectDistributionParameterException {
		if ( n<0) {
			throw new IncorrectDistributionParameterException("`numberOfElements` should be positive.");
		}
		if (probability<0 || probability>1) {
			throw new IncorrectDistributionParameterException("`probability` should be in range [0, 1]");
		}

		this.n = n;
		this.probability = probability;
	}

	// Because the input is number of items, and the input of Binomial distribution is number of experiments.
	public BinomialPar(Integer numberOfItems, Double probability) throws IncorrectDistributionParameterException {
		this(numberOfItems.intValue()-1, probability.doubleValue());
	}
	public BinomialPar(Double probability, Integer numberOfItems) throws IncorrectDistributionParameterException {
		this(numberOfItems.intValue()-1, probability.doubleValue());
	}

	@Override
	public boolean check(){
		return n>=0 && probability>=0 && probability<=1;
	}

	public int getNumberOfExperiment(){
		return n;
	}

	public double getProbability(){
		return probability;
	}

	public void setNumberOfExperiment(int n ) throws IncorrectDistributionParameterException {
		if ( n<0) {
			throw new IncorrectDistributionParameterException("`numberOfElements` should be positive.");
		}
		this.n = n;
	}

	public void setProbability(double probability ) throws IncorrectDistributionParameterException {
		if (probability<0 || probability>1) {
			throw new IncorrectDistributionParameterException("`probability` should be in range [0, 1]");
		}
		this.probability = probability;
	}
}
