package random.discrete;

import jmt.common.exception.IncorrectDistributionParameterException;
import random.AbstractParameter;
import random.Parameter;

public class ShiftedGeometricPar extends AbstractParameter implements Parameter {

	private double p;

	public ShiftedGeometricPar(double p) throws IncorrectDistributionParameterException {
		if ( p<0 || p>1 ) {
			throw new IncorrectDistributionParameterException("The probability should belong to [0,1]");
		} else {
			this.p = p;
		}
	}

	public ShiftedGeometricPar(Double p) throws IncorrectDistributionParameterException {
		this(p.doubleValue());
	}

	@Override
	public boolean check(){
		return p>=0 && p<=1;
	}

	public double getProbability(){
		return this.p;
	}

	public void setProbability(double p) throws IncorrectDistributionParameterException {
		if ( p<0 || p>1 ) {
			throw new IncorrectDistributionParameterException("The probability should belong to [0,1]");
		} else {
			this.p = p;
		}
	}

	@Override
	public void setMean(double value) throws IncorrectDistributionParameterException {
		if ( value<1 ) {
			throw new IncorrectDistributionParameterException("The mean should be greater than or equal to 1");
		} else {
			this.p = 1 / value;
		}
	}
}
