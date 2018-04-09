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
 * This is the parameter that should be passed to the Poisson
 * distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class PoissonPar extends AbstractParameter implements Parameter {

	private double mean;

	/**
	 * It creates a new poisson parameter with the mean provided by the user.
	 *
	 * @param mean double containing the mean of the poisson distribution.
	 * @throws IncorrectDistributionParameterException if the mean is not
	 * greater than zero.
	 *
	 */
	public PoissonPar(double mean) throws IncorrectDistributionParameterException {
		if (mean <= 0) {
			throw new IncorrectDistributionParameterException("mean must be gtz");
		} else {
			this.mean = mean;
		}
	}

	public PoissonPar(Double wmean) throws IncorrectDistributionParameterException {
		this(wmean.doubleValue());
	}

	/**
	 * It verifies that the parameter is correct. For the poisson distribution,
	 * the parameter is correct if the mean is greater than zero.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if (mean <= 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It return the value of the mean of the poisson distribution.
	 *
	 * @return double with the value of the mean of the poisson distribution.
	 *
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * It allows the user to change the value of the mean of the poisson
	 * distribution and verifies if it is greater than zero.
	 *
	 * @param mean double indicating the mean of the poisson distribution.
	 * @throws IncorrectDistributionParameterException if the mean is not
	 * greater than zero.
	 *
	 */
	public void setMean(double mean) throws IncorrectDistributionParameterException {
		if (mean <= 0) {
			throw new IncorrectDistributionParameterException("mean must be gtz");
		} else {
			this.mean = mean;
		}
	}

} // end PoissonPar
