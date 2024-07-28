package jmt.engine.random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.random.AbstractParameter;

public class ZipfPar extends AbstractParameter {

	private int numberOfElements;
	private double alpha;

	public ZipfPar(int numberOfElements, double alpha) throws IncorrectDistributionParameterException {
		if ( numberOfElements<=0) {
			throw new IncorrectDistributionParameterException("`numberOfElements` should be positive.");
		}
		if ( alpha<=0) {
			throw new IncorrectDistributionParameterException("`alpha` should be positive.");
		}

		this.numberOfElements = numberOfElements;
		this.alpha = alpha;
	}

	public ZipfPar(Integer numberOfElements, Double alpha) throws IncorrectDistributionParameterException {
		this(numberOfElements.intValue(), alpha.doubleValue());
	}

	public ZipfPar(Double alpha, Integer numberOfElements) throws IncorrectDistributionParameterException {
		this(numberOfElements.intValue(), alpha.doubleValue());
	}

	@Override
	public boolean check(){
		return numberOfElements>0 && alpha>0;
	}

	public int getNumberOfElements(){
		return numberOfElements;
	}

	public double getAlpha(){
		return alpha;
	}

	public void setMax(int numberOfElements ) throws IncorrectDistributionParameterException {
		if ( numberOfElements<=0) {
			throw new IncorrectDistributionParameterException("`numberOfElements` should be positive.");
		}
		this.numberOfElements = numberOfElements;
	}

	public void setAlpha(double alpha ) throws IncorrectDistributionParameterException {
		if ( alpha<=0) {
			throw new IncorrectDistributionParameterException("`alpha` should be positive.");
		}
		this.alpha = alpha;
	}
}
