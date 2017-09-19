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
 * This is the parameter that should be passed to the MAP
 * distribution.
 *
 * <br><br>Copyright (c) 2006
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Giuliano Casale
 *
 */
public class MAPPar extends AbstractParameter implements Parameter {

	private double mean;
	private double var;
	private double sigma0;
	private double sigma1;
	private double lambda0;
	private double lambda1;
	protected ExponentialPar expParam0;
	protected ExponentialPar expParam1;

	public MAPPar(Double lambda0, Double lambda1, Double sigma0, Double sigma1) throws IncorrectDistributionParameterException {
		this.sigma0 = sigma0.doubleValue();
		this.sigma1 = sigma1.doubleValue();
		this.lambda0 = lambda0.doubleValue();
		this.lambda1 = lambda1.doubleValue();
		testParameters();
		this.mean = (this.sigma0 + this.sigma1) / (this.sigma1 * this.lambda0 + this.sigma0 * this.lambda1);
		this.var = 0;
		// creates 2 ExponentialPar objects
		expParam0 = new ExponentialPar(this.lambda0 + this.sigma0);
		expParam1 = new ExponentialPar(this.lambda1 + this.sigma1);
	}

	/**
	 * Tests the parameters for the constructor requiring p, lambda1 and lambda0.
	 *
	 * @throws IncorrectDistributionParameterException if p is not between zero and one or if lambda0 and lambda1 are not both greater than zero.
	 *
	 */
	private void testParameters() throws IncorrectDistributionParameterException {
		if (sigma0 <= 0) {
			throw new IncorrectDistributionParameterException("Error: sigma0 must be >= 0 ");
		}
		if (sigma1 <= 0) {
			throw new IncorrectDistributionParameterException("Error: sigma1 must be >= 0 ");
		}
		if (lambda0 < 0) {
			throw new IncorrectDistributionParameterException("Error: lambda0 must be >= 0");
		}
		if (lambda1 < 0) {
			throw new IncorrectDistributionParameterException("Error: lambda1 must be >= 0");
		}
	}

	/**
	 * It verifies if the parameter is correct. For the MAP
	 * distribution, the parameter is right if the mean calculated is gtz,
	 * the variance is gtz, p probability is between 0 and 1 and both the lambda value are gtz.
	 *
	 * @return boolean, indicating whether the parameter is correct or not.
	 *
	 */

	@Override
	public boolean check() {
		if ((sigma0 <= 0) || (sigma1 <= 0) || (lambda0 < 0) || (lambda1 < 0)) {
			return false;
		} else {
			return true;
		}
	}

	public double getSigma0() {
		return sigma0;
	}

	public double getSigma1() {
		return sigma1;
	}

	/**
	 * It returns the value of the parameter lambda0, the parameter lambda for the
	 * 1st exponential distribution created by the MAP parameter.
	 *
	 * @return double with lambda0 the value of lambda for the 1st exponential distribution.
	 *
	 */

	public double getLambda0() {
		return lambda0;
	}

	/**
	 * It returns the value of the parameter lambda1, the parameter lambda for the
	 * 2nd exponential distribution created by the MAP parameter.
	 *
	 * @return double with lambda1 the value of lambda for the 2nd exponential distribution.
	 *
	 */

	public double getLambda1() {
		return lambda1;
	}

	/**
	 * It returns the value of the mean of the MAP distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the mean of the MAP distribution.
	 *
	 */

	public double getMean() {
		return mean;
	}

	/**
	 * It returns the value of the variance of the MAP distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the variance of the MAP distribution.
	 *
	 */

	public double getVar() {
		return var;
	}

	/**
	 * It returns the parameter used to create the 1st exponential
	 * distribution used by the MAP distribution.
	 *
	 * @return exponentialPar with expParam0, the parameter of the 1st exponential distribution.
	 *
	 */

	public ExponentialPar getExpParam0() {
		return expParam0;
	}

	/**
	 * It returns the parameter used to create the 2nd the exponential
	 * distribution used by the MAP distribution.
	 *
	 * @return exponentialPar with expParam1, the parameter of the 2nd exponential distribution.
	 *
	 */

	public ExponentialPar getExpParam1() {
		return expParam1;
	}

} // end MAPPar
