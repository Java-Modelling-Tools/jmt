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

import Jama.Matrix;
import jmt.common.exception.IncorrectDistributionParameterException;

/**
 * 
 * This is the parameter that should be passed to the Phase-Type
 * distribution.
 * 
 * <br><br>Copyright (c) 2017
 * <br>Imperial College London - Department of Computing
 * @author Lulai Zhu
 * 
 */
public class PhaseTypePar extends AbstractParameter implements Parameter {

	private double[] alpha;
	private double[][] T;
	private double mean;
	private double var;
	private double[] lambda;
	private double[][] P;

	/**
	 * It creates a new phase-type parameter based on the data provided by the user.
	 *
	 * @param alpha 2D array of Object containing the initial probability vector.
	 * @param T 2D array of Object containing the transition rate matrix.
	 * @throws IncorrectDistributionParameterException if alpha or T is incorrect
	 * or if alpha and T are inconsistent.
	 *
	 */
	public PhaseTypePar(Object[] alpha, Object[] T) throws IncorrectDistributionParameterException {
		this.alpha = new double[((Object[]) alpha[0]).length];
		for (int i = 0; i < ((Object[]) alpha[0]).length; i++) {
			Double d = (Double) ((Object[]) alpha[0])[i];
			this.alpha[i] = d.doubleValue();
		}
		this.T = new double[T.length][];
		for (int i = 0; i < T.length; i++) {
			this.T[i] = new double[((Object[]) T[i]).length];
			for (int j = 0; j < ((Object[]) T[i]).length; j++) {
				Double d = (Double) ((Object[]) T[i])[j];
				this.T[i][j] = d.doubleValue();
			}
		}
		testParameters();

		int order = this.alpha.length;
		Matrix alfa = new Matrix(this.alpha, 1);
		Matrix M = new Matrix(this.T).inverse().uminus();
		Matrix one = new Matrix(order, 1, 1.0);
		double moment1 = alfa.times(M).times(one).get(0, 0);
		double moment2 = 2 * alfa.times(M).times(M).times(one).get(0, 0);
		mean = moment1;
		var = moment2 - moment1 * moment1;

		lambda = new double[order];
		for (int i = 0; i < order; i++) {
			lambda[i] = -this.T[i][i];
		}
		P = new double[order][order];
		for (int i = 0; i < order; i++) {
			for (int j = 0; j < order; j++) {
				if (i == j) {
					P[i][j] = 0.0;
				} else {
					P[i][j] = this.T[i][j] / lambda[i];
				}
			}
		}
	}

	/**
	 * Tests the parameters for the constructor requiring alpha and T.
	 *
	 * @throws IncorrectDistributionParameterException if alpha or T is incorrect
	 * or if alpha and T are inconsistent.
	 *
	 */
	private void testParameters() throws IncorrectDistributionParameterException {
		if (!checkAlpha()) {
			throw new IncorrectDistributionParameterException("Error: alpha is incorrect");
		}
		if (!checkT()) {
			throw new IncorrectDistributionParameterException("Error: T is incorrect");
		}
		if (alpha.length != T.length) {
			throw new IncorrectDistributionParameterException("Error: alpha and T are inconsistent");
		}
	}

	/**
	 * It verifies if the parameter is correct. For the phase-type distribution,
	 * the parameter is right if the entries of alpha are greater or equal to
	 * zero and sum up to one.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	private boolean checkAlpha() {
		double sum = 0.0;
		for (int i = 0; i < alpha.length; i++) {
			if (alpha[i] < 0.0) {
				return false;
			}
			sum += alpha[i];
		}
		if (Math.abs(sum - 1.0) > 1e-6) {
			return false;
		}
		return true;
	}

	/**
	 * It verifies if the parameter is correct. For the phase-type distribution,
	 * the parameter is right if T is a square matrix such that the diagonal
	 * entries are less than zero, the non-diagonal entries are greater than or
	 * equal to zero and the entries in each row sum up to a non-positive value.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	private boolean checkT() {
		for (int i = 0; i < T.length; i++) {
			if (T.length != T[i].length) {
				return false;
			}
			double sum = 0.0;
			for (int j = 0; j < T[i].length; j++) {
				if (i == j && T[i][j] >= 0.0) {
					return false;
				}
				if (i != j && T[i][j] < 0.0) {
					return false;
				}
				sum += T[i][j];
			}
			if (sum > 1e-6) {
				return false;
			}
		}
		return true;
	}

	/**
	 * It returns the value of the mean of the phase-type distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the mean of the phase-type distribution.
	 *
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * It returns the value of the variance of the phase-type distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the variance of the phase-type distribution.
	 *
	 */
	public double getVar() {
		return var;
	}

	/**
	 * It returns the value of the parameter alpha, the initial probability vector
	 * of the phase-type distribution.
	 *
	 * @return array of double with alpha the initial probability vector of the
	 * phase-type distribution.
	 *
	 */
	public double[] getAlpha() {
		return alpha;
	}

	/**
	 * It returns the value of the parameter lambda, the completion rate vector
	 * of the phase-type distribution.
	 *
	 * @return array of double with lambda the completion rate vector of the
	 * phase-type distribution.
	 *
	 */
	public double[] getLambda() {
		return lambda;
	}

	/**
	 * It returns the value of the parameter P, the transition probability matrix
	 * of the phase-type distribution.
	 *
	 * @return 2D array of double with P the transition probability matrix of the
	 * phase-type distribution.
	 *
	 */
	public double[][] getP() {
		return P;
	}

} // end PhaseTypePar
