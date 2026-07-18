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
 * <br><br>Copyright (c) 2021
 * <br>Imperial College London
 * @author Giuliano Casale, g.casale@imperial.ac.uk
 * 
 */
public class LognormalPar extends AbstractParameter implements Parameter {

	private double mu;
	private double sigma;

	/**
	 * It creates a new normal parameter according to the data provided by the user:
	 * the mean and the standard deviation of the normal distribution.
	 *
	 * @param mu double containing the mean of the normal distribution.
	 * @param sigma double containing the standard deviation of the
	 * normal distribution.
	 * @throws IncorrectDistributionParameterException if the standard deviation is
	 * not greater than zero.
	 *
	 */
	public LognormalPar(double mu, double sigma) throws IncorrectDistributionParameterException {
		if (sigma <= 0) {
			throw new IncorrectDistributionParameterException("sigma must be gtz");
		} else {
			this.mu = mu;
			this.sigma = sigma;
		}
	}

	public LognormalPar(Double wmu, Double wsigma) throws IncorrectDistributionParameterException {
		this(wmu.doubleValue(), wsigma.doubleValue());
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
		if (sigma <= 0) {
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
	public double getMu() {
		return mu;
	}

	/**
	 * It returns the value of the standard deviation of the normal distribution.
	 *
	 * @return double with the standard deviation of the normal distribution.
	 *
	 */
	public double getSigma() {
		return sigma;
	}
	
	/**
	 * It allows the user to change the value of the mean of the normal distribution.
	 *
	 * @param mu double indicating the mean of the normal distribution.
	 * @throws IncorrectDistributionParameterException
	 *
	 */
	public void setMu(double mu) throws IncorrectDistributionParameterException {
		this.mu = mu;
	}

	/**
	 * It allows the user to change the value of the standard deviation of the
	 * normal distribution and verifies if it is greater than zero.
	 *
	 * @param sigma double indicating the standard deviation of the
	 * normal distribution.
	 * @throws IncorrectDistributionParameterException if the standard deviation is
	 * not greater than zero.
	 *
	 */
	public void setSigma(double sigma) throws IncorrectDistributionParameterException {
		if (sigma <= 0) {
			throw new IncorrectDistributionParameterException("standardDeviation must be gtz");
		} else {
			this.sigma = sigma;
		}
	}

} // end NormalPar
