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
import jmt.engine.random.engine.RandomEngine;

/**
 * 
 * This is the Hyperexponential distribution (see the constructor
 * description for details).
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * 
 */
public class HyperExp extends AbstractDistribution implements Distribution {

	private Exponential expDistr;

	/**
	 * This is the constructor. It creates a new hyperexponential distribution which
	 * is constituted by two parallel exponential phases.
	 *
	 */
	public HyperExp() {
		expDistr = new Exponential();
	}
	
	@Override
	public void setRandomEngine(RandomEngine engine){
		this.engine = engine;
		this.expDistr.setRandomEngine(engine);
	}

	/**
	 * This method is used to obtain from the distribution its probability distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the pdf.
	 * @param p parameter of the hyperexponential distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the probability distribution function evaluated in x.
	 */
	public double pdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		return 0.0;
	}

	/**
	 * This method is used to obtain from the distribution its cumulative distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the cdf.
	 * @param p parameter of the hyperexponential distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the cumulative distribution function evaluated in x.
	 */
	public double cdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		return 0.0;
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic mean.
	 *
	 * @param p parameter of the hyperexponential distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic mean of the distribution.
	 */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			return ((HyperExpPar) p).getMean();
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter lambda1 and lambda2 must be gtz; p must be a number between 0 and 1");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic variance.
	 *
	 * @param p parameter of the hyperexponential distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic variance of the distribution.
	 */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			return ((HyperExpPar) p).getVar();
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter lambda1 and lambda2 must be gtz; p must be a number between 0 and 1");
		}
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the hyperexponential distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			if (engine.nextDouble() <= ((HyperExpPar) p).getP()) {
				return expDistr.nextRand(((HyperExpPar) p).getExpParam1());
			} else {
				return expDistr.nextRand(((HyperExpPar) p).getExpParam2());
			}
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter lambda1 and lambda2 must be gtz; p must be a number between 0 and 1");
		}
	}

} // end HyperExp
