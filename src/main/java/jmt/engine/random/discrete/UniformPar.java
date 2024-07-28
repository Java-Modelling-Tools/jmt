package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.AbstractParameter;

public class UniformPar extends AbstractParameter {

	private int min;
	private int max;

	public UniformPar(int a, int b) throws IncorrectDistributionParameterException {
		if (a >= b) {
			throw new IncorrectDistributionParameterException("min(a) must less than max(b)");
		}

		this.min = a;
		this.max = b;
	}

	public UniformPar(Integer a, Integer b) throws IncorrectDistributionParameterException {
		this(a.intValue(), b.intValue());
	}

	@Override
	public boolean check(){
		return min < max;
	}

	public int getMin(){
		return this.min;
	}

	public int getMax(){
		return this.max;
	}

	public void setMin(int a) throws IncorrectDistributionParameterException{
		if (a >= max) {
			throw new IncorrectDistributionParameterException("min(a) must less than max(b)");
		}
		this.min = a;
	}

	public void setMax(int b) throws IncorrectDistributionParameterException{
		if (min >= b) {
			throw new IncorrectDistributionParameterException("max(b) must greater than min(a)");
		}
		this.max = b;
	}
}