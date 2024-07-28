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
 * This is the Poisson distribution.
 * 
 * @author Leran Chen - 01/09/2021
 * 
 */
public class Poisson extends AbstractDistribution implements Distribution {

	/**
	 * This is the constructor. It creates a new Poisson distribution.
	 */
	public Poisson() {
	}

	/**
	 * This method is used to obtain from the distribution its probability distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the pdf.
	 * @param p parameter of the Poisson distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the probability distribution function evaluated in x.
	 */
	public double pdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			if (x < 0.0) {
				return 0.0;
			}
			double lambda = ((PoissonPar) p).getLambda();
			int k = (int) Math.floor(x);
			return getPoissonProbability(k, lambda);
		} else {
			throw new IncorrectDistributionParameterException("Remember: parameter lambda must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution its cumulative distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the cdf.
	 * @param p parameter of the Poisson distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the cumulative distribution function evaluated in x.
	 */
	public double cdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			if (x < 0.0) {
				return 0.0;
			}
			double lambda = ((PoissonPar) p).getLambda();
			double sum = 0;
			for (int k = 0; k <= (int) Math.floor(x); k++) {
				sum += getPoissonProbability(k, lambda);
			}
			return sum;
		} else {
			throw new IncorrectDistributionParameterException("Remember: parameter lambda must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic mean.
	 *
	 * @param p parameter of the Poisson distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic mean of the distribution.
	 */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			return ((PoissonPar) p).getLambda();
		} else {
			throw new IncorrectDistributionParameterException("Remember: parameter lambda must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic variance.
	 *
	 * @param p parameter of the Poisson distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic variance of the distribution.
	 */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			return ((PoissonPar) p).getLambda();
		} else {
			throw new IncorrectDistributionParameterException("Remember: parameter lambda must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the Poisson distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double lambda = ((PoissonPar) p).getLambda();
			int k = -1;
			double sum = 0.0;
			double rand = engine.raw();
			while (sum < rand) {
				k++;
				sum += getPoissonProbability(k, lambda);
			}
			return (double) k;
		} else {
			throw new IncorrectDistributionParameterException("Remember: parameter lambda must be gtz");
		}
	}

	/**
	 * This method is used to compute the probability of the Poisson distribution for
	 * an input k given the parameter lambda.
	 *
	 * @param k input of the Poisson distribution.
	 * @param lambda parameter of the Poisson distribution.
	 * @return double with the probability for an input k given the parameter lambda.
	 */
	private double getPoissonProbability(int k, double lambda) {
		double prob = Math.exp(-lambda);
		for (int i = 1; i <= k; i++) {
			prob *= lambda / i;
		}
		return prob;
	}

} // end Poisson
