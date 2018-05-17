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
 * This is the Pareto distribution (see the constructor description
 * for its pdf definition).
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * @author Modified by Stefano Omini, 7/5/2004
 * 
 */
public class Pareto extends AbstractDistribution implements Distribution {

	/**
	 * This is the constructor. It creates a new empty pareto distribution which
	 * is defined from pdf:
	 * <pre>                  alpha      (-(alpha+1))
	 * pdf(x) = alpha * k       *  x</pre>
	 * with alpha > 2 and k > 0 and less than or equal to x.
	 */
	public Pareto() {
	}

	/**
	 * This method is used to obtain from the distribution its probability distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the pdf.
	 * @param p parameter of the pareto distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the probability distribution function evaluated in x.
	 */
	public double pdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alfa = ((ParetoPar) p).getAlpha();
			double k = ((ParetoPar) p).getK();
			if (x <= alfa) {
				return 0.0;
			}
			return (alfa * Math.pow(k, alfa) / Math.pow(x, (alfa + 1.0)));
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter alpha must be > 2; parameter k must be > 0");
		}
	}

	/**
	 * This method is used to obtain from the distribution its cumulative distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the cdf.
	 * @param p parameter of the pareto distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the cumulative distribution function evaluated in x.
	 */
	public double cdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alfa = ((ParetoPar) p).getAlpha();
			double k = ((ParetoPar) p).getK();
			if (x <= alfa) {
				return 0.0;
			}
			return 1.0 - Math.pow((k / x), alfa);
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter alpha must be > 2; parameter k must be > 0");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic mean.
	 *
	 * @param p parameter of the pareto distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic mean of the distribution.
	 *
	 * the mean is calculated as: (k*alpha)/(alpha-1)
	 */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alfa = ((ParetoPar) p).getAlpha();
			double k = ((ParetoPar) p).getK();
			return (alfa * k / (alfa - 1.0));
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter alpha must be > 2; parameter k must be > 0");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic variance.
	 *
	 * @param p parameter of the pareto distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic variance of the distribution.
	 *
	 * the variance is calculated as: (alpha*(k^2))/(((alpha-1)^2)*(alpha-2))
	 */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alfa = ((ParetoPar) p).getAlpha();
			double k = ((ParetoPar) p).getK();
			return (alfa * k * k / ((alfa - 1.0) * (alfa - 1.0) * (alfa - 2.0)));
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter alpha must be > 2; parameter k must be > 0");
		}
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the pareto distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((ParetoPar) p).getAlpha();
			double k = ((ParetoPar) p).getK();
			return Math.pow((1.0 - engine.raw()), (-1.0 / alpha)) * k;
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter alpha must be > 2; parameter k must be > 0");
		}
	}

} // end Pareto
