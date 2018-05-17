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
 * This is the parameter that should be passed to the Hyperexponential
 * distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class HyperExpPar extends AbstractParameter implements Parameter {

	private double p;
	private double lambda1;
	private double lambda2;
	private double mean;
	private double var;
	private ExponentialPar expParam1;
	private ExponentialPar expParam2;

	/**
	 * It creates a new hyperexponential parameter based on the data provided by the user.
	 * This constructor takes three parameters and creates two instances of
	 * exponential distribution which are selected with probability p provided.
	 *
	 * @param p double containing the probability to select one of the two exponential.
	 * @param lambda1 double containing the value of lambda for the 1st exponential.
	 * @param lambda2 double containing the value of lambda for the 2nd exponential.
	 * @throws IncorrectDistributionParameterException if p is not between zero and one
	 * or if lambda1 and lambda2 are not both greater than zero.
	 *
	 */
	public HyperExpPar(double p, double lambda1, double lambda2) throws IncorrectDistributionParameterException {
		this.p = p;
		this.lambda1 = lambda1;
		this.lambda2 = lambda2;
		testParameters();
		mean = p / lambda1 + (1 - p) / lambda2;
		var = 2 * (p / (lambda1 * lambda1) + (1 - p) / (lambda2 * lambda2)) - mean * mean;
		// creates 2 ExponentialPar objects
		expParam1 = new ExponentialPar(lambda1);
		expParam2 = new ExponentialPar(lambda2);
	}

	public HyperExpPar(Double wp, Double wlambda1, Double wlambda2) throws IncorrectDistributionParameterException {
		this(wp.doubleValue(), wlambda1.doubleValue(), wlambda2.doubleValue());
	}

	/**
	 * Tests the parameters for the constructor requiring p, lambda1 and lambda2.
	 *
	 * @throws IncorrectDistributionParameterException if p is not between 0 and 1
	 * or if lambda1 and lambda2 are not both gtz.
	 *
	 */
	private void testParameters() throws IncorrectDistributionParameterException {
		if (p < 0 || p > 1) {
			throw new IncorrectDistributionParameterException("Error: p must be >= 0 and <= 1");
		}
		if (lambda1 <= 0) {
			throw new IncorrectDistributionParameterException("Error: lambda1 must be > 0");
		}
		if (lambda2 <= 0) {
			throw new IncorrectDistributionParameterException("Error: lambda2 must be > 0");
		}
	}

	/**
	 * It verifies if the parameter is correct. For the hyperexponential
	 * distribution, the parameter is right if p probability is between 0
	 * and 1 and both the lambda values are gtz.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if ((p < 0 || p > 1) || (lambda1 <= 0) || (lambda2 <= 0)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It returns the value of the parameter p, the probability to select one of the
	 * exponential distribution.
	 *
	 * @return double with p the probability to select one of the exponential distribution.
	 *
	 */
	public double getP() {
		return p;
	}

	/**
	 * It returns the value of the parameter lambda1, the parameter lambda for the
	 * 1st exponential distribution created by the hyperexponential parameter.
	 *
	 * @return double with lambda1 the value of lambda for the 1st exponential distribution.
	 *
	 */
	public double getLambda1() {
		return lambda1;
	}

	/**
	 * It returns the value of the parameter lambda2, the parameter lambda for the
	 * 2nd exponential distribution created by the hyperexponential parameter.
	 *
	 * @return double with lambda2 the value of lambda for the 2nd exponential distribution.
	 *
	 */
	public double getLambda2() {
		return lambda2;
	}

	/**
	 * It returns the value of the mean of the hyperexponential distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the mean of the hyperexponential distribution.
	 *
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * It returns the value of the variance of the hyperexponential distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the variance of the hyperexponential distribution.
	 *
	 */
	public double getVar() {
		return var;
	}

	/**
	 * It returns the parameter used to create the 1st exponential distribution used by
	 * the hyperexponential distribution.
	 *
	 * @return exponentialPar with expParam1 the parameter of the 1st exponential distribution.
	 *
	 */
	public ExponentialPar getExpParam1() {
		return expParam1;
	}

	/**
	 * It returns the parameter used to create the 2nd exponential distribution used by
	 * the hyperexponential distribution.
	 *
	 * @return exponentialPar with expParam2 the parameter of the 2nd exponential distribution.
	 *
	 */
	public ExponentialPar getExpParam2() {
		return expParam2;
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
		p = 0.5 * (1 - Math.sqrt((c * c - 1) / (c * c + 1)));
		lambda1 = 2 * p / value;
		lambda2 = 2 * (1 - p) / value;
		mean = value;
		var = value * value * c * c;
		expParam1.setLambda(lambda1);
		expParam2.setLambda(lambda2);
	}

} // end HyperExpPar
