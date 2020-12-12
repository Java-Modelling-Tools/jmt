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
 * This is the MAP distribution.
 * 
 * <br><br>Copyright (c) 2017
 * <br>Imperial Collge London - Department of Computing
 * @author Lulai Zhu
 * 
 */
public class MAPDistr extends AbstractDistribution implements Distribution {

	private int curState;
	private Exponential expDistr;
	private ExponentialPar expPar;

	/**
	 * This is the constructor. It creates a new MAP distribution which
	 * is constituted by arbitrary exponential phases.
	 *
	 */
	public MAPDistr() {
		curState = -1;
		expDistr = new Exponential();
		expPar = new ExponentialPar();
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
	 * @param p parameter of the MAP distribution.
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
	 * @param p parameter of the MAP distribution.
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
	 * @param p parameter of the MAP distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic mean of the distribution.
	 */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		return ((MAPPar) p).getMean();
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic variance.
	 *
	 * @param p parameter of the MAP distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic variance of the distribution.
	 */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		return ((MAPPar) p).getVar();
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the MAP distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		double sample = 0.0;
		double[] pi = ((MAPPar) p).getPi();
		double[] lambda = ((MAPPar) p).getLambda();
		double[][] P0 = ((MAPPar) p).getP0();
		double[][] P1 = ((MAPPar) p).getP1();

		if (curState < 0) {
			double rand = engine.raw();
			double sum = 0.0;
			for (int i = 0; i < pi.length; i++) {
				sum += pi[i];
				if (sum >= rand) {
					curState = i;
					break;
				}
			}
		}

		OUTER_LOOP:
		while (true) {
			expPar.setLambda(lambda[curState]);
			sample += expDistr.nextRand(expPar);

			double rand = engine.raw();
			double sum = 0.0;
			for (int i = 0; i < P0[curState].length; i++) {
				sum += P0[curState][i];
				if (sum >= rand) {
					curState = i;
					continue OUTER_LOOP;
				}
			}
			for (int i = 0; i < P1[curState].length; i++) {
				sum += P1[curState][i];
				if (sum >= rand) {
					curState = i;
					break OUTER_LOOP;
				}
			}
		}

		return sample;
	}

} // end MAPDistr
