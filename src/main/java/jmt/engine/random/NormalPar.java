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
 * This is the parameter that should be passed to the Normal
 * distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class NormalPar extends AbstractParameter implements Parameter {

	private double mean;
	private double standardDeviation;

	/**
	 * It creates a new normal parameter according to the data provided by the user:
	 * the mean and the standard deviation of the normal distribution.
	 *
	 * @param mean double containing the mean of the normal distribution.
	 * @param standardDeviation double containing the standard deviation of the
	 * normal distribution.
	 * @throws IncorrectDistributionParameterException if the standard deviation is
	 * not greater than zero.
	 *
	 */
	public NormalPar(double mean, double standardDeviation) throws IncorrectDistributionParameterException {
		if (standardDeviation <= 0) {
			throw new IncorrectDistributionParameterException("standardDeviation must be gtz");
		} else {
			this.mean = mean;
			this.standardDeviation = standardDeviation;
		}
	}

	public NormalPar(Double wmean, Double wstandardDeviation) throws IncorrectDistributionParameterException {
		this(wmean.doubleValue(), wstandardDeviation.doubleValue());
	}

	/**
	 * It verifies that the parameter is correct. For the normal distribution,
	 * the parameter is right if the standard deviation is greater than zero.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if (standardDeviation <= 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It returns the value of the mean of the normal distribution.
	 *
	 * @return double with the mean of the normal distribution.
	 *
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * It returns the value of the standard deviation of the normal distribution.
	 *
	 * @return double with the standard deviation of the normal distribution.
	 *
	 */
	public double getStandardDeviation() {
		return standardDeviation;
	}

	/**
	 * It allows the user to change the value of the mean of the normal distribution.
	 *
	 * @param mean double indicating the mean of the normal distribution.
	 * @throws IncorrectDistributionParameterException
	 *
	 */
	public void setMean(double mean) throws IncorrectDistributionParameterException {
		this.mean = mean;
	}

	/**
	 * It allows the user to change the value of the standard deviation of the
	 * normal distribution and verifies if it is greater than zero.
	 *
	 * @param standardDeviation double indicating the standard deviation of the
	 * normal distribution.
	 * @throws IncorrectDistributionParameterException if the standard deviation is
	 * not greater than zero.
	 *
	 */
	public void setStandardDeviation(double standardDeviation) throws IncorrectDistributionParameterException {
		if (standardDeviation <= 0) {
			throw new IncorrectDistributionParameterException("standardDeviation must be gtz");
		} else {
			this.standardDeviation = standardDeviation;
		}
	}

} // end NormalPar
