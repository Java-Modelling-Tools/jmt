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
 * This is the parameter that should be passed to the Gamma
 * distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class GammaDistrPar extends AbstractParameter implements Parameter {

	private double alpha;
	private double lambda;

	/**
	 * It creates a new gamma parameter according to the value provided by the user.
	 *
	 * @param alpha double containing the parameter alpha of the gamma distribution.
	 * @param lambda double containing the parameter lambda of the gamma distribution.
	 * @throws IncorrectDistributionParameterException if alpha or lambda is not
	 * greater than zero.
	 *
	 */
	public GammaDistrPar(double alpha, double lambda) throws IncorrectDistributionParameterException {
		if ((alpha <= 0) || (lambda <= 0)) {
			throw new IncorrectDistributionParameterException("alpha and lambda must be > 0");
		} else {
			this.alpha = alpha;
			this.lambda = lambda;
		}
	}

	public GammaDistrPar(Double walpha, Double wlambda) throws IncorrectDistributionParameterException {
		this(walpha.doubleValue(), wlambda.doubleValue());
	}

	/**
	 * It controls if the parameters are correct or not. For the gamma distribution,
	 * the parameters are right if they are both greater than zero.
	 *
	 * @return boolean indicating if the parameters are correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if ((alpha <= 0) || (lambda <= 0)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It returns the value of the parameter alpha of the gamma distribution.
	 *
	 * @return double with alpha.
	 *
	 */
	public double getAlpha() {
		return alpha;
	}

	/**
	 * It returns the value of the parameter lambda of the gamma distribution.
	 *
	 * @return double with lambda.
	 *
	 */
	public double getLambda() {
		return lambda;
	}

	/**
	 * It allows the user to modify the value of the parameter alpha of the gamma
	 * distribution. It verifies if the new value is correct, that is if alpha is
	 * greater than zero.
	 *
	 * @param alpha double indicating the new value of the parameter alpha.
	 * @throws IncorrectDistributionParameterException if the value provided is not
	 * greater than zero.
	 *
	 */
	public void setAlpha(double alpha) throws IncorrectDistributionParameterException {
		if (alpha <= 0) {
			throw new IncorrectDistributionParameterException("alpha must be > 0");
		} else {
			this.alpha = alpha;
		}
	}

	/**
	 * It allows the user to modify the value of the parameter lambda of the gamma
	 * distribution. It verifies if the new value is correct, that is if lambda is
	 * greater than zero.
	 *
	 * @param lambda double indicating the new value of the parameter lambda.
	 * @throws IncorrectDistributionParameterException if the value provided is not
	 * greater than zero.
	 *
	 */
	public void setLambda(double lambda) throws IncorrectDistributionParameterException {
		if (lambda <= 0) {
			throw new IncorrectDistributionParameterException("lambda must be > 0");
		} else {
			this.lambda = lambda;
		}
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
		lambda = value / alpha;
	}

} // end GammaDistrPar
