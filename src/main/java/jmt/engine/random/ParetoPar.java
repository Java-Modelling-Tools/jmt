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
 * This is the parameter that should be passed to the Pareto
 * distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class ParetoPar extends AbstractParameter implements Parameter {

	private double alpha;
	private double k;

	/**
	 * It creates a new pareto parameter according to the data provided by the user:
	 * the parameter alpha and k of the pareto distribution.
	 *
	 * @param alpha double containing the parameter alpha of the pareto distribution.
	 * @param k double containing the parameter k of the pareto distribution.
	 * @throws IncorrectDistributionParameterException if alpha is not greater than two
	 * or k is not greater than zero.
	 *
	 */
	public ParetoPar(double alpha, double k) throws IncorrectDistributionParameterException {
		this.alpha = alpha;
		this.k = k;
		if (alpha <= 2) {
			throw new IncorrectDistributionParameterException("alpha must be > 2");
		}
		if (k <= 0) {
			throw new IncorrectDistributionParameterException("k must be > 0");
		}
	}

	public ParetoPar(Double walpha, Double wk) throws IncorrectDistributionParameterException {
		this(walpha.doubleValue(), wk.doubleValue());
	}

	/**
	 * It controls if the parameter is correct. For the pareto distribution,
	 * the parameter is right if alpha is greater than two and k is greater
	 * than zero.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	@Override
	public boolean check() {
		if ((alpha <= 2) || (k <= 0)) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * It returns the value of the parameter alpha of the pareto distribution.
	 *
	 * @return double with the parameter alpha of the pareto distribution.
	 *
	 */
	public double getAlpha() {
		return alpha;
	}

	/**
	 * It returns the value of the parameter k of the pareto distribution.
	 *
	 * @return double with the parameter k of the pareto distribution.
	 *
	 */
	public double getK() {
		return k;
	}

	/**
	 * It allows the user to change the value of the parameter alpha of the pareto
	 * distribution and verifies if alpha is greater than two.
	 *
	 * @param alpha double indicating the parameter alpha of the pareto distribution.
	 * @throws IncorrectDistributionParameterException if alpha is not greater than
	 * two.
	 *
	 */
	public void setAlpha(double alpha) throws IncorrectDistributionParameterException {
		if (alpha <= 2) {
			throw new IncorrectDistributionParameterException("alpha must be > 2");
		} else {
			this.alpha = alpha;
		}
	}

	/**
	 * It allows the user to change the value of the parameter k of the pareto
	 * distribution. It verifies if k is greater than zero.
	 *
	 * @param k double indicating the parameter k of the pareto distribution.
	 * @throws IncorrectDistributionParameterException if k is not greater than
	 * zero.
	 *
	 */
	public void setK(double k) throws IncorrectDistributionParameterException {
		if (k <= 0) {
			throw new IncorrectDistributionParameterException("k must be > 0");
		} else {
			this.k = k;
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
		k = value - value / alpha;
	}

} // end ParetoPar
