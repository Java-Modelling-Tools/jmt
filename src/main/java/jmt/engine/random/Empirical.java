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
 * This is the Empirical distribution, which is constructed with
 * data provided by the user.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class Empirical extends AbstractDistribution implements Distribution {

	/**
	 * This is the constructor. It creates a new empirical distribution.
	 */
	public Empirical() {
	}

	/**
	 * This method is used to obtain from the distribution its probability distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the pdf.
	 * @param p parameter of the empirical distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the probability distribution function evaluated in x.
	 */
	public double pdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			int k = (int) x;
			double[] pdf = ((EmpiricalPar) p).getPDF();
			//if the given x exceeds the pdf array limits, there may be problems!!
			if (k < 0 || k > pdf.length - 1) {
				//pdf is defined only for K = 0, 1, 2 ... (length-1)
				return 0.0;
			} else {
				return pdf[k];
			}
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: all the probability values given must be equal to or greater than zero and the sum of all the values must be one or zero");
		}
	}

	/**
	 * This method is used to obtain from the distribution its cumulative distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the cdf.
	 * @param p parameter of the empirical distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the cumulative distribution function evaluated in x.
	 */
	public double cdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			int k = (int) x;
			double[] cdf = ((EmpiricalPar) p).getCDF();
			if (k < 0) {
				return 0.0;
			}
			if (k >= cdf.length - 1) {
				return 1.0;
			}
			return cdf[k];
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: all the probability values given must be equal to or greater than zero and the sum of all the values must be one or zero");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic mean, which is is calculated from the data provided by the user
	 * (contained in the parameter p) as the sum of all the data value, divided by
	 * the number of data.
	 *
	 * @param p parameter of the empirical distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic mean of the distribution.
	 *
	 */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double[] pdf = ((EmpiricalPar) p).getPDF();
			double mean = 0.0;
			for (int ptn = 0; ptn < pdf.length; ptn++) {
				mean += pdf[ptn] * ptn;
			}
			return mean;
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: all the probability values given must be equal to or greater than zero and the sum of all the values must be one or zero");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic variance which is calculated from the data provided by the user
	 * (contained in the parameter p) as the sum of all the squares of the
	 * differences between each data value and the theoretic mean, divided by
	 * the number of data.
	 *
	 * @param p parameter of the empirical distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic variance of the distribution.
	 *
	 */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double[] pdf = ((EmpiricalPar) p).getPDF();
			double mean = theorMean(p);
			double variance = 0.0;
			for (int ptn = 0; ptn < pdf.length; ptn++) {
				variance += (((ptn - mean) * (ptn - mean)) * pdf[ptn]);
			}
			return variance;
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: all the probability values given must be equal to or greater than zero and the sum of all the values must be one or zero");
		}
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the empirical distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double rand = engine.raw();
			double[] cdf = ((EmpiricalPar) p).getCDF();
			for (int ptn = 0; ptn < cdf.length - 1; ptn++) {
				if (cdf[ptn + 1] >= rand) {
					return ptn;
				}
			}
			double[] pdf = ((EmpiricalPar) p).getPDF();
			for (int ptn = pdf.length - 1; ptn > 0; ptn--) {
				if (pdf[ptn] > 0.0) {
					return ptn;
				}
			}
			return -1.0;
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: all the probability values given must be equal to or greater than zero and the sum of all the values must be one or zero");
		}
	}

} // end Empirical
