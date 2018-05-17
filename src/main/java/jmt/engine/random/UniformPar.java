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
 * This is the parameter that should be passed to the Uniform
 * distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class UniformPar extends AbstractParameter implements Parameter {

	private double min;
	private double max;

	/**
	 * It creates a new uniform parameter using the bounds provided by the user.
	 *
	 * @param min double containing the minimum bound of the uniform distribution.
	 * @param max double containing the maximum bound of the uniform distribution.
	 * @throws IncorrectDistributionParameterException if the bounds are incorrect.
	 *
	 */
	public UniformPar(double min, double max) throws IncorrectDistributionParameterException {
		this.min = min;
		this.max = max;
		if (!check()) {
			throw new IncorrectDistributionParameterException("Error: the *max* parameter must be greater than the *min* one");
		}
	}

	public UniformPar(Double wmin, Double wmax) throws IncorrectDistributionParameterException {
		this(wmin.doubleValue(), wmax.doubleValue());
	}

	/**
	 * It verifies if the parameter is correct. For the uniform distribution the parameter
	 * is right if the bounds are right which means that max must be greater than min.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if (max <= min) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It verifies if the parameter is correct. For the uniform distribution the parameter
	 * is right if the bounds are right which means that max must be greater than min. This
	 * is the same as the method check() but a helper method called only by the constructor.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	private boolean check(double min, double max) {
		if (max <= min) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It returns the value of the minimum bound of the uniform distribution.
	 *
	 * @return double with the minimum bound of the distribution.
	 *
	 */
	public double getMin() {
		return min;
	}

	/**
	 * It returns the value of the maximum bound of the uniform distribution.
	 *
	 * @return double with the maximum bound of the distribution.
	 *
	 */
	public double getMax() {
		return max;
	}

	/**
	 * It allows the user to change the minimum bound of the uniform distribution
	 * and verifies if it is correct.
	 *
	 * @param min double indicating the new value of the minimum bound.
	 * @throws IncorrectDistributionParameterException if the provided value is not
	 * less than the maximum bound value.
	 *
	 */
	public void setMin(double min) throws IncorrectDistributionParameterException {
		if (check(min, max)) {
			this.min = min;
		} else {
			throw new IncorrectDistributionParameterException("Error: the *max* parameter must be greater than the *min* one");
		}
	}

	/**
	 * It allows the user to change the maximum bound of the uniform distribution
	 * and verifies if it is correct.
	 *
	 * @param max double indicating the new value of the maximum bound.
	 * @throws IncorrectDistributionParameterException if the provided value is not
	 * greater than the minimum bound value.
	 *
	 */
	public void setMax(double max) throws IncorrectDistributionParameterException {
		if (check(min, max)) {
			this.max = max;
		} else {
			throw new IncorrectDistributionParameterException("Error: the *max* parameter must be greater than the *min* one");
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
		// Keeps variation coefficient c constant
		double c = (max - min) / (max + min) / Math.sqrt(3);
		min = value - value * c * Math.sqrt(3);
		max = value + value * c * Math.sqrt(3);
	}

} // end UniformPar
