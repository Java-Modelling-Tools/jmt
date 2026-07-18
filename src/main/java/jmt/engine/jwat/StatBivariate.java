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
package jmt.engine.jwat;

/**
 * Class representing bivariate statistics
 * @author Brambilla Davide, Fumagalli Claudio
 * @version 1.0
 */
public class StatBivariate {
	/**
* Class builder
* @param allVar array of variables
	 */
	public StatBivariate(VariableNumber[] allVar) {
		var = allVar;
		covariance = new double[allVar.length][];
		covDone = new boolean[allVar.length][allVar.length + 1];
	}

	/**
* Returns the value of covariance(v1,v2)
* @param ind1 index of the first variable
* @param ind2 index of the second variable
	 */
	public double getCovariance(int ind1, int ind2) {
//Check if the covariance vector exists for ind1
		if (!covDone[ind1][0]) {
			covariance[ind1] = new double[var.length];
			covDone[ind1][0] = true;
		}
//Check if the searched covariance is already calculated
		if (!covDone[ind1][ind2 + 1]) {
			calcCovariance(ind1, ind2);
		}
		return covariance[ind1][ind2];
	}

//Calculate the value of the convariance of the two requested variables
	private void calcCovariance(int ind1, int ind2) {
		double cov = 0;
		for (int i = 0; i < var[0].Size(); i++) {
			cov += (var[ind1].getValue(i) - var[ind1].getUniStats().getMean()) * (var[ind2].getValue(i) - var[ind2].getUniStats().getMean());
		}
		cov /= var[0].Size();
		covariance[ind1][ind2] = cov / (var[ind1].getUniStats().getVariance() * var[ind2].getUniStats().getVariance());
		covDone[ind1][ind2 + 1] = true;
	}

	public void resetVar(int indVar) {
		int i;

		for (i = 0; i < var.length; i++) {
			covDone[indVar][i + 1] = false;
			covDone[i][indVar + 1] = false;
		}
	}

/* Array of variables */
	private VariableNumber[] var;
	/*Covariance*/
	private double[][] covariance;
	//Il primo elemento di ogni riga contiene se esiste il vettore delle cov per la variabile
	private boolean[][] covDone;

}