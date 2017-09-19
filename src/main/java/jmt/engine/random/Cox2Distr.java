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
 * This is the Cox2 distribution.
 * 
 * <br><br>Copyright (c) 2017
 * <br>Imperial College London - Department of Computing
 * @author Giuliano Casale
 * 
 */
public class Cox2Distr extends AbstractDistribution implements Distribution {

	private int curState;
	protected Exponential expDistr;

	public Cox2Distr() {
		curState = 0;
		engine = RandomEngine.makeDefault();
		expDistr = new Exponential();
	}

	public int getCurState() {
		return curState;
	}

	public void setCurState(int newState) {
		curState = newState;
	}

	public double pdf(double x, Parameter p) { //other implementation may use p.check()
		return 0.0;
	}

	public double cdf(double x, Parameter p) { //other implementation may use p.check()
		return 0.0;
	}

	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		double l0 = ((Cox2Par) p).getLambda0();
		double l1 = ((Cox2Par) p).getLambda1();
		double phi = ((Cox2Par) p).getPhi0();
		
		return 2/l1 - 1/l0 - phi/l1;
	}

	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		return 0.0;
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the hyper exponential distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */

	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		double r = 0.0;
		if (p.check()) 
		{
			if (this.getCurState() == 0) {
				r = r + expDistr.nextRand(((Cox2Par) p).getExpParam0());
				if (engine.nextDouble() > ((Cox2Par) p).getPhi0()) {
					this.setCurState(1);
					r = r + nextRand(p);
				}
				return r;
			} else {
				r = r + expDistr.nextRand(((Cox2Par) p).getExpParam1());
				this.setCurState(0);
				return r;
			}
		} else {
			throw new IncorrectDistributionParameterException("Exception in generation of Cox2 random numbers");
		}
	}

} // end Cox2Distr
