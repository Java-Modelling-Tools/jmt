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
import jmt.engine.math.Gamma;

/**
 * 
 * This is the Weibull distribution.
 * 
 * <br><br>Copyright (c) 2021
 * <br>Imperial College London
 * @author Giuliano Casale, g.casale@imperial.ac.uk
 * 
 */
public class Weibull extends AbstractDistribution implements Distribution {

	/**
	 * This is the constructor. It creates a new Weibull distribution 
	 */
	public Weibull() {
	}

	/**
	 * This method is used to obtain from the distribution its probability distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the pdf.
	 * @param p parameter of the Weibull distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the probability distribution function evaluated in x.
	 */
	public double pdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((WeibullPar) p).getAlpha(); // scale
			double r = ((WeibullPar) p).getR(); //shape
			if (x<0) {
				return 0.0;
			} else {
				return (r / alpha) * Math.pow(x/alpha, r - 1) * Math.exp(-Math.pow((x/alpha),r));
			}
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution its cumulative distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the cdf.
	 * @param p parameter of the Weibull distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the cumulative distribution function evaluated in x.
	 */
	public double cdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((WeibullPar) p).getAlpha();
			double r = ((WeibullPar) p).getR();
			return 1.0 - Math.exp(-Math.pow((x/alpha),r));
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic mean.
	 *
	 * @param p parameter of the Weibull distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic mean of the distribution.
	 *
	 * The theoretic mean is calculated as r/alpha.
	 */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((WeibullPar) p).getAlpha();
			double r = ((WeibullPar) p).getR();
			return alpha * Gamma.gamma(1+1/r);
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic variance.
	 *
	 * @param p parameter of the Weibull distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic variance of the distribution.
	 *
	 * The theoretic variance is calculated as r/(alpha^2).
	 */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((WeibullPar) p).getAlpha();
			double r = ((WeibullPar) p).getR();
			return Math.pow(alpha,2)*(Gamma.gamma(1+2/r)-Math.pow(Gamma.gamma(1+1/r),2));
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the Weibull distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {			
			double alpha = ((WeibullPar) p).getAlpha();
			double r = ((WeibullPar) p).getR();
			//System.out.println("lambda: " + alpha + " k: " + r + " random: " + (-alpha) * Math.pow(Math.log(1-engine.raw()),1/r));
			return alpha * Math.pow(-Math.log(1-engine.raw()),1/r);
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

} // end Weibull
