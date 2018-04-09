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
 * This is the parameter that should be passed to the MMPP2
 * distribution.
 * 
 * <br><br>Copyright (c) 2008
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Giuliano Casale
 * 
 */
public class MMPP2Par extends AbstractParameter implements Parameter {

	private double sigma0;
	private double sigma1;
	private double lambda0;
	private double lambda1;
	private double mean;
	private double var;
	private ExponentialPar expParam0;
	private ExponentialPar expParam1;

	/**
	 * It creates a new MMPP2 parameter based on the data provided by the user.
	 *
	 * @param lambda0 double containing the value of lambda for the 1st exponential.
	 * @param lambda1 double containing the value of lambda for the 2st exponential.
	 * @param sigma0 double containing the value of sigma for the 1st exponential.
	 * @param sigma1 double containing the value of sigma for the 2st exponential.
	 * @throws IncorrectDistributionParameterException if sigma0 and sigma1 are not
	 * both greater than zero or if lambda0 and lambda1 are not both greater than
	 * zero.
	 *
	 */
	public MMPP2Par(double lambda0, double lambda1, double sigma0, double sigma1) throws IncorrectDistributionParameterException {
		this.sigma0 = sigma0;
		this.sigma1 = sigma1;
		this.lambda0 = lambda0;
		this.lambda1 = lambda1;
		testParameters();
		mean = (sigma0 + sigma1) / (sigma0 * lambda1 + sigma1 * lambda0);
		var = 2 * ((sigma0 + sigma1) * (sigma0 + sigma1) + sigma0 * lambda0 + sigma1 * lambda1)
				/ ((sigma0 * lambda1 + sigma1 * lambda0) * (sigma0 * lambda1 + sigma1 * lambda0) + sigma0 * lambda0 * lambda1 * lambda1 + sigma1 * lambda0 * lambda0 * lambda1)
				- mean * mean;
		// creates 2 ExponentialPar objects
		expParam0 = new ExponentialPar(lambda0 + sigma0);
		expParam1 = new ExponentialPar(lambda1 + sigma1);
	}

	public MMPP2Par(Double wlambda0, Double wlambda1, Double wsigma0, Double wsigma1) throws IncorrectDistributionParameterException {
		this(wlambda0.doubleValue(), wlambda1.doubleValue(), wsigma0.doubleValue(), wsigma1.doubleValue());
	}

	/**
	 * Tests the parameters for the constructor requiring sigma0, sigma1, lambda0
	 * and lambda1.
	 *
	 * @throws IncorrectDistributionParameterException if sigma0 and sigma1 are not
	 * both gtz or if lambda0 and lambda1 are not both gtz.
	 *
	 */
	private void testParameters() throws IncorrectDistributionParameterException {
		if (sigma0 <= 0) {
			throw new IncorrectDistributionParameterException("Error: sigma0 must be > 0");
		}
		if (sigma1 <= 0) {
			throw new IncorrectDistributionParameterException("Error: sigma1 must be > 0");
		}
		if (lambda0 <= 0) {
			throw new IncorrectDistributionParameterException("Error: lambda0 must be > 0");
		}
		if (lambda1 <= 0) {
			throw new IncorrectDistributionParameterException("Error: lambda1 must be > 0");
		}
	}

	/**
	 * It verifies if the parameter is correct. For the MMPP2 distribution,
	 * the parameter is right if both the sigma values are gtz and both the
	 * lambda values are gtz.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if ((sigma0 <= 0) || (sigma1 <= 0) || (lambda0 <= 0) || (lambda1 <= 0)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It returns the value of the parameter sigma0, the parameter sigma for the
	 * 1st exponential distribution created by the MMPP2 parameter.
	 *
	 * @return double with sigma0 the value of sigma for the 1st exponential distribution.
	 *
	 */
	public double getSigma0() {
		return sigma0;
	}

	/**
	 * It returns the value of the parameter sigma1, the parameter sigma for the
	 * 2nd exponential distribution created by the MMPP2 parameter.
	 *
	 * @return double with sigma1 the value of sigma for the 2nd exponential distribution.
	 *
	 */
	public double getSigma1() {
		return sigma1;
	}

	/**
	 * It returns the value of the parameter lambda0, the parameter lambda for the
	 * 1st exponential distribution created by the MMPP2 parameter.
	 *
	 * @return double with lambda0 the value of lambda for the 1st exponential distribution.
	 *
	 */
	public double getLambda0() {
		return lambda0;
	}

	/**
	 * It returns the value of the parameter lambda1, the parameter lambda for the
	 * 2nd exponential distribution created by the MMPP2 parameter.
	 *
	 * @return double with lambda1 the value of lambda for the 2nd exponential distribution.
	 *
	 */
	public double getLambda1() {
		return lambda1;
	}

	/**
	 * It returns the value of the mean of the MMPP2 distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the mean of the MMPP2 distribution.
	 *
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * It returns the value of the variance of the MMPP2 distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the variance of the MMPP2 distribution.
	 *
	 */
	public double getVar() {
		return var;
	}

	/**
	 * It returns the parameter used to create the 1st exponential distribution used by
	 * the MMPP2 distribution.
	 *
	 * @return exponentialPar with expParam0 the parameter of the 1st exponential distribution.
	 *
	 */
	public ExponentialPar getExpParam0() {
		return expParam0;
	}

	/**
	 * It returns the parameter used to create the 2nd exponential distribution used by
	 * the MMPP2 distribution.
	 *
	 * @return exponentialPar with expParam1 the parameter of the 2nd exponential distribution.
	 *
	 */
	public ExponentialPar getExpParam1() {
		return expParam1;
	}

} // end MMPP2Par
