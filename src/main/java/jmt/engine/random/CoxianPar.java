/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.engine.random;

import jmt.common.exception.IncorrectDistributionParameterException;

/**
 * 
 * This is the parameter that should be passed to the Coxian
 * distribution.
 * 
 * <br><br>Copyright (c) 2017
 * <br>Imperial College London - Department of Computing
 * @author Giuliano Casale
 * 
 */
public class CoxianPar extends AbstractParameter implements Parameter {

	private double lambda0;
	private double lambda1;
	private double phi0;
	private double mean;
	private double var;
	private ExponentialPar expParam0;
	private ExponentialPar expParam1;

	/**
	 * It creates a new coxian parameter based on the data provided by the user.
	 * This constructor takes three parameters and creates two instances of
	 * exponential distribution which are exited with probability phi0 provided.
	 *
	 * @param lambda0 double containing the value of lambda for the 1st exponential.
	 * @param lambda1 double containing the value of lambda for the 2nd exponential.
	 * @param phi0 double containing the probability to exit one of the two exponential.
	 * @throws IncorrectDistributionParameterException if lambda0 and lambda1 are not
	 * both greater than zero or if phi0 is not between zero and one.
	 *
	 */
	public CoxianPar(double lambda0, double lambda1, double phi0) throws IncorrectDistributionParameterException {
		this.lambda0 = lambda0;
		this.lambda1 = lambda1;
		this.phi0 = phi0;
		testParameters();
		mean = 1 / lambda0 + (1 - phi0) / lambda1;
		var = 1 / (lambda0 * lambda0) + (1 - phi0 * phi0) / (lambda1 * lambda1);
		// creates 2 ExponentialPar objects
		expParam0 = new ExponentialPar(lambda0);
		expParam1 = new ExponentialPar(lambda1);
	}

	public CoxianPar(Double wlambda0, Double wlambda1, Double wphi0) throws IncorrectDistributionParameterException {
		this(wlambda0.doubleValue(), wlambda1.doubleValue(), wphi0.doubleValue());
	}

	/**
	 * Tests the parameters for the constructor requiring lambda0, lambda1 and phi0.
	 *
	 * @throws IncorrectDistributionParameterException if lambda0 and lambda1 are not
	 * both gtz or if phi0 is not between 0 and 1.
	 *
	 */
	private void testParameters() throws IncorrectDistributionParameterException {
		if (lambda0 <= 0) {
			throw new IncorrectDistributionParameterException("Error: lambda0 must be > 0");
		}
		if (lambda1 <= 0) {
			throw new IncorrectDistributionParameterException("Error: lambda1 must be > 0");
		}
		if (phi0 < 0 || phi0 > 1) {
			throw new IncorrectDistributionParameterException("Error: phi0 must be >= 0 and <= 1");
		}
	}

	/**
	 * It verifies if the parameter is correct. For the coxian distribution,
	 * the parameter is right if both the lambda values are gtz and phi0
	 * probability is between 0 and 1.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if ((lambda0 <= 0) || (lambda1 <= 0) || (phi0 < 0 || phi0 > 1)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It returns the value of the parameter lambda0, the parameter lambda for the
	 * 1st exponential distribution created by the coxian parameter.
	 *
	 * @return double with lambda0 the value of lambda for the 1st exponential distribution.
	 *
	 */
	public double getLambda0() {
		return lambda0;
	}

	/**
	 * It returns the value of the parameter lambda1, the parameter lambda for the
	 * 2nd exponential distribution created by the coxian parameter.
	 *
	 * @return double with lambda1 the value of lambda for the 2nd exponential distribution.
	 *
	 */
	public double getLambda1() {
		return lambda1;
	}

	/**
	 * It returns the value of the parameter phi0, the probability to exit one of the
	 * exponential distribution.
	 *
	 * @return double with phi0 the probability to exit one of the exponential distribution.
	 *
	 */
	public double getPhi0() {
		return phi0;
	}

	/**
	 * It returns the value of the mean of the coxian distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the mean of the coxian distribution.
	 *
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * It returns the value of the variance of the coxian distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the variance of the coxian distribution.
	 *
	 */
	public double getVar() {
		return var;
	}

	/**
	 * It returns the parameter used to create the 1st exponential distribution used by
	 * the coxian distribution.
	 *
	 * @return exponentialPar with expParam0 the parameter of the 1st exponential distribution.
	 *
	 */
	public ExponentialPar getExpParam0() {
		return expParam0;
	}

	/**
	 * It returns the parameter used to create the 2nd exponential distribution used by
	 * the coxian distribution.
	 *
	 * @return exponentialPar with expParam1 the parameter of the 2nd exponential distribution.
	 *
	 */
	public ExponentialPar getExpParam1() {
		return expParam1;
	}

	/**
	 * Sets mean for a given distribution parameter. This method is required to adjust the mean value
	 * of a distribution into Load Dependent Service Time Strategy.
	 * <br>Author: Bertoli Marco
	 * @param value New mean value for this distribution.
	 * @throws IncorrectDistributionParameterException if mean value is invalid for this distribution.
	 */
	@Override
	public void setMean(double value) throws IncorrectDistributionParameterException {
		if (value <= 0) {
			throw new IncorrectDistributionParameterException("Mean value must be greater than zero");
		}
		// Keeps variation coefficient c constant
		double c = Math.sqrt(var) / mean;
		if (c < 1) {
			lambda0 = (2 / value) / (1 + Math.sqrt(1 + 2 * (c * c - 1)));
			lambda1 = (2 / value) / (1 - Math.sqrt(1 + 2 * (c * c - 1)));
			phi0 = 0;
		} else if (c == 1) {
			lambda0 = 1 / value;
			lambda1 = 1 / value;
			phi0 = 1;
		} else {
			lambda0 = 2 / value;
			lambda1 = 1 / (value * c * c);
			phi0 = 1 - 1 / (2 * c * c);
		}
		mean = value;
		var = value * value * c * c;
		expParam0.setLambda(lambda0);
		expParam1.setLambda(lambda1);
	}

} // end CoxianPar
