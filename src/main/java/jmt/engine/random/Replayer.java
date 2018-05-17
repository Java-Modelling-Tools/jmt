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
 * Replays cyclically data, previously generated, reading them from a file.

 * @author Federico Granata
 * @version Date: 23-lug-2003 Time: 17.28.35
 * @author Modified by Stefano Omini, 12/5/2004

 */
public class Replayer extends AbstractDistribution implements Distribution {

	private double valueSum;
	private int valueCount;

	/**
	 *  Constructs a new distribution with standard random engine.
	 */
	public Replayer() {
		valueSum = 0.0;
		valueCount = 0;
	}

	/**
	 * Reads a new data from the file.
	 *
	 * @param p parameter of the distribution.
	 * @throws IncorrectDistributionParameterException when the parameters r not
	 * consistent.
	 * @return double with the next random number from the sequence.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p instanceof ReplayerPar) {
			double value = ((ReplayerPar) p).getNext();
			valueSum += value;
			valueCount += 1;
			return value;
		}
		throw new IncorrectDistributionParameterException("The class of the parameter is not correct");
	}

	/**
	 * This method is used to obtain from the distribution its probability distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the pdf.
	 * @param p parameter of the constant distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the probability distribution function evaluated in x.
	 */
	public double pdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		throw new IncorrectDistributionParameterException("Method not implemented yet");
	}

	/**
	 * This method is used to obtain from the distribution its cumulative distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the cdf.
	 * @param p parameter of the constant distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the cumulative distribution function evaluated in x.
	 */
	public double cdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		throw new IncorrectDistributionParameterException("Method not implemented yet");
	}

	/**
	  * This method is used to obtain from the distribution the value of its own
	  * theoretic mean.
	  *
	  * @param p parameter of the constant distribution.
	  * @throws IncorrectDistributionParameterException
	  * @return double with the theoretic mean of the distribution.
	  */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		return valueSum / valueCount;
	}

	/**
	  * This method is used to obtain from the distribution the value of its own
	  * theoretic variance.
	  *
	  * @param p parameter of the constant distribution.
	  * @throws IncorrectDistributionParameterException
	  * @return double with the theoretic variance of the distribution.
	  */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		throw new IncorrectDistributionParameterException("Method not implemented yet");
	}

}
