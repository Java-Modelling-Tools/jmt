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
 * This is the parameter that should be passed to the Deterministic
 * distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class DeterministicDistrPar extends AbstractParameter implements Parameter {

	private double t;

	/**
	 * It creates a new deterministic parameter. It accepts a double greater
	 * than zero and uses this number as the constant value that will be returned.
	 *
	 * @param t Parameter of the deterministic distribution. It is the value which is always returned.
	 * @throws IncorrectDistributionParameterException if the provided parameter is not greater than zero.
	 */
	public DeterministicDistrPar(double t) throws IncorrectDistributionParameterException {
		this.t = t;
		if (!check()) {
			throw new IncorrectDistributionParameterException("t <= 0");
		}
	}

	/**
	 * It creates a new deterministic parameter. It accepts a Double greater
	 * than zero and uses this number as the constant value that will be returned.
	 *
	 * @param t Parameter of the deterministic distribution. It is the value which is always returned.
	 * @throws IncorrectDistributionParameterException if the provided parameter is not greater than zero.
	 */
	public DeterministicDistrPar(Double t) throws IncorrectDistributionParameterException {
		this(t.doubleValue());
	}

	/**
	 * It controls if the parameter is correct or not. For the deterministic distribution,
	 * the parameter is correct if it is greater than zero.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if (t <= 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It returns the value of the parameter of the deterministic distribution, that is
	 * the constant value provided by the user.
	 *
	 * @return the value always returned by the distribution.
	 *
	 */
	public double getParameterValue() {
		return t;
	}

	/**
	 * It allows the user to change the value of the parameter of the deterministic distribution.
	 * It verifies if the new value is correct (must be greater than zero).
	 *
	 * @param t double indicating the new value of the parameter.
	 * @throws IncorrectDistributionParameterException if the provided parameter is not greater than zero.
	 *
	 */
	public void setParameterValue(double t) throws IncorrectDistributionParameterException {
		this.t = t;
		if (!check()) {
			throw new IncorrectDistributionParameterException("t <= 0");
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
		t = value;
	}

} // end DeterministicDistrPar
