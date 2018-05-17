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
 * This is the parameter that should be passed to the Exponential
 * distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class ExponentialPar extends AbstractParameter implements Parameter {

	private double lambda;

	/**
	 * It creates a new empty exponential parameter that must be set to be used.
	 *
	 */
	public ExponentialPar() {
	}

	/**
	 * It creates a new exponential parameter using the value provided by the user.
	 *
	 * @param lambda double containing the parameter lambda.
	 * @throws IncorrectDistributionParameterException if the value provided is not
	 * greater than zero.
	 *
	 */
	public ExponentialPar(double lambda) throws IncorrectDistributionParameterException {
		setLambda(lambda);
	}

	public ExponentialPar(Double wlambda) throws IncorrectDistributionParameterException {
		this(wlambda.doubleValue());
	}

	/**
	 * It returns the value of the parameter lambda of the exponential distribution.
	 *
	 * @return double with lambda.
	 *
	 */
	public double getLambda() {
		return lambda;
	}

	/**
	 * It allows the user to modify the value of the parameter lambda of the
	 * exponential distribution and verify if the value is correct that is if it is
	 * greater than zero.
	 *
	 * @param lambda double indicating the new value of the parameter lambda.
	 * @throws IncorrectDistributionParameterException if the provided value is not
	 * greater than zero.
	 *
	 */
	public void setLambda(double lambda) throws IncorrectDistributionParameterException {
		if (lambda > 0) {
			this.lambda = lambda;
		} else {
			throw new IncorrectDistributionParameterException("Remember: parameter lambda must be gtz");
		}
	}

	/**
	 * It controls if the parameter is correct or not. For the exponential distribution,
	 * the parameter is right if lambda is greater than zero.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if (lambda > 0) {
			return true;
		} else {
			return false;
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
		lambda = 1 / value;
	}

} // end ExponentialPar
