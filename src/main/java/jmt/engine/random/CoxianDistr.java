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
 * This is the Coxian distribution.
 * 
 * <br><br>Copyright (c) 2017
 * <br>Imperial College London - Department of Computing
 * @author Giuliano Casale
 * 
 */
public class CoxianDistr extends AbstractDistribution implements Distribution {

	private Exponential expDistr;

	/**
	 * This is the constructor. It creates a new coxian distribution which
	 * is constituted by two serial exponential phases.
	 *
	 */
	public CoxianDistr() {
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
	 * @param p parameter of the coxian distribution.
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
	 * @param p parameter of the coxian distribution.
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
	 * @param p parameter of the coxian distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic mean of the distribution.
	 */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			return ((CoxianPar) p).getMean();
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter lambda0 and lambda1 must be gtz; phi0 must be a number between 0 and 1");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic variance.
	 *
	 * @param p parameter of the coxian distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic variance of the distribution.
	 */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			return ((CoxianPar) p).getVar();
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter lambda0 and lambda1 must be gtz; phi0 must be a number between 0 and 1");
		}	
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the coxian distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double sample = expDistr.nextRand(((CoxianPar) p).getExpParam0());
			if (engine.nextDouble() > ((CoxianPar) p).getPhi0()) {
				sample += expDistr.nextRand(((CoxianPar) p).getExpParam1());
			}
			return sample;
		} else {
			throw new IncorrectDistributionParameterException(
					"Remember: parameter lambda0 and lambda1 must be gtz; phi0 must be a number between 0 and 1");
		}
	}

} // end CoxianDistr
