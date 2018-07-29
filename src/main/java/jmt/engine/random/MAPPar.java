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
 * This is the parameter that should be passed to the MAP
 * distribution.
 * 
 * <br><br>Copyright (c) 2017
 * <br>Imperial College London - Department of Computing
 * @author Lulai Zhu
 * 
 */
public class MAPPar extends AbstractParameter implements Parameter {

	private double[][] D0;
	private double[][] D1;
	private double mean;
	private double var;
	private double[] pi;
	private double[] lambda;
	private double[][] P0;
	private double[][] P1;

	/**
	 * It creates a new MAP parameter based on the data provided by the user.
	 *
	 * @param D0 2D array of Object containing the hidden transition rate matrix.
	 * @param D1 2D array of Object containing the observable transition rate matrix.
	 * @throws IncorrectDistributionParameterException if D0 or D1 is incorrect
	 * or if D0 and D1 are inconsistent or if D0 + D1 is incorrect.
	 *
	 */
	public MAPPar(Object[] D0, Object[] D1) throws IncorrectDistributionParameterException {
		this.D0 = new double[D0.length][];
		for (int i = 0; i < D0.length; i++) {
			this.D0[i] = new double[((Object[]) D0[i]).length];
			for (int j = 0; j < ((Object[]) D0[i]).length; j++) {
				Double d = (Double) ((Object[]) D0[i])[j];
				this.D0[i][j] = d.doubleValue();
			}
		}
		this.D1 = new double[D1.length][];
		for (int i = 0; i < D1.length; i++) {
			this.D1[i] = new double[((Object[]) D1[i]).length];
			for (int j = 0; j < ((Object[]) D1[i]).length; j++) {
				Double d = (Double) ((Object[]) D1[i])[j];
				this.D1[i][j] = d.doubleValue();
			}
		}
		testParameters();

		int order = this.D0.length;
		Matrix M = new Matrix(this.D0).inverse().uminus();
		Matrix P = M.times(new Matrix(this.D1));
		Matrix L = P.minus(Matrix.identity(order, order));
		for (int i = 0; i < order; i++) {
			L.set(i, order - 1, 1.0);
		}
		Matrix r = new Matrix(1, order, 0.0);
		r.set(0, order - 1, 1.0);
		Matrix pi = L.solveTranspose(r).transpose();
		Matrix one = new Matrix(order, 1, 1.0);
		double moment1 = pi.times(M).times(one).get(0, 0);
		double moment2 = 2 * pi.times(M).times(M).times(one).get(0, 0);
		mean = moment1;
		var = moment2 - moment1 * moment1;

		this.pi = pi.getArrayCopy()[0];
		lambda = new double[order];
		for (int i = 0; i < order; i++) {
			lambda[i] = -this.D0[i][i];
		}
		P0 = new double[order][order];
		for (int i = 0; i < order; i++) {
			for (int j = 0; j < order; j++) {
				if (i == j) {
					P0[i][j] = 0.0;
				} else {
					P0[i][j] = this.D0[i][j] / lambda[i];
				}
			}
		}
		P1 = new double[order][order];
		for (int i = 0; i < order; i++) {
			for (int j = 0; j < order; j++) {
				P1[i][j] = this.D1[i][j] / lambda[i];
			}
		}
	}

	/**
	 * Tests the parameters for the constructor requiring D0 and D1.
	 *
	 * @throws IncorrectDistributionParameterException if D0 or D1 is incorrect
	 * or if D0 and D1 are inconsistent or if D0 + D1 is incorrect.
	 *
	 */
	private void testParameters() throws IncorrectDistributionParameterException {
		if (!checkD0()) {
			throw new IncorrectDistributionParameterException("Error: D0 is incorrect");
		}
		if (!checkD1()) {
			throw new IncorrectDistributionParameterException("Error: D1 is incorrect");
		}
		if (D0.length != D1.length) {
			throw new IncorrectDistributionParameterException("Error: D0 and D1 are inconsistent");
		}
		if (!checkD0D1()) {
			throw new IncorrectDistributionParameterException("Error: D0 + D1 is incorrect");
		}
	}

	/**
	 * It verifies if the parameter is correct. For the MAP distribution,
	 * the parameter is right if D0 is a square matrix such that the diagonal
	 * entries are less than zero, the non-diagonal entries are greater than or
	 * equal to zero and the entries in each row sum up to a non-positive value.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	private boolean checkD0() {
		for (int i = 0; i < D0.length; i++) {
			if (D0.length != D0[i].length) {
				return false;
			}
			double sum = 0.0;
			for (int j = 0; j < D0[i].length; j++) {
				if (i == j && D0[i][j] >= 0.0) {
					return false;
				}
				if (i != j && D0[i][j] < 0.0) {
					return false;
				}
				sum += D0[i][j];
			}
			if (sum > 1e-6) {
				return false;
			}
		}
		return true;
	}

	/**
	 * It verifies if the parameter is correct. For the MAP distribution,
	 * the parameter is right if D1 is a square matrix such that the entries
	 * are greater than or equal to zero.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	private boolean checkD1() {
		for (int i = 0; i < D1.length; i++) {
			if (D1.length != D1[i].length) {
				return false;
			}
			for (int j = 0; j < D1[i].length; j++) {
				if (D1[i][j] < 0.0) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * It verifies if the parameter is correct. For the MAP distribution,
	 * the parameter is right if the entries in each row of D0 + D1 sum up to
	 * zero.
	 *
	 * @return boolean indicating if the parameter is correct or not.
	 *
	 */
	private boolean checkD0D1() {
		for (int i = 0; i < D0.length; i++) {
			double sum = 0.0;
			for (int j = 0; j < D0[i].length; j++) {
				sum += (D0[i][j] + D1[i][j]);
			}
			if (Math.abs(sum) > 1e-6) {
				return false;
			}
		}
		return true;
	}

	/**
	 * It returns the value of the mean of the MAP distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the mean of the phase-type distribution.
	 *
	 */
	public double getMean() {
		return mean;
	}

	/**
	 * It returns the value of the variance of the MAP distribution which
	 * is provided by the user or evaluated according to other data.
	 *
	 * @return double with the variance of the phase-type distribution.
	 *
	 */
	public double getVar() {
		return var;
	}

	/**
	 * It returns the value of the parameter pi, the stationary probability vector
	 * of the MAP distribution.
	 *
	 * @return array of double with pi the stationary probability vector of the
	 * MAP distribution.
	 *
	 */
	public double[] getPi() {
		return pi;
	}

	/**
	 * It returns the value of the parameter lambda, the completion rate vector
	 * of the MAP distribution.
	 *
	 * @return array of double with lambda the completion rate vector of the
	 * MAP distribution.
	 *
	 */
	public double[] getLambda() {
		return lambda;
	}

	/**
	 * It returns the value of the parameter P0, the hidden transition probability matrix
	 * of the MAP distribution.
	 *
	 * @return 2D array of double with P0 the hidden transition probability matrix of the
	 * phase-type distribution.
	 *
	 */
	public double[][] getP0() {
		return P0;
	}

	/**
	 * It returns the value of the parameter P1, the observable transition probability matrix
	 * of the MAP distribution.
	 *
	 * @return 2D array of double with P1 the observable transition probability matrix of the
	 * MAP distribution.
	 *
	 */
	public double[][] getP1() {
		return P1;
	}

} // end MAPPar
