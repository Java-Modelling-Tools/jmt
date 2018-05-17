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
import jmt.engine.math.Arithmetic;
import jmt.engine.math.Gamma;

/**
 * 
 * This is the Erlang distribution.
 * 
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 * @author Fabrizio Frontera - ffrontera@yahoo.it
 * @author Modified by Stefano Omini, 7/5/2004
 * 
 */
public class Erlang extends AbstractDistribution implements Distribution {

	/**
	 * This is the constructor. It creates a new erlang distribution which
	 * is defined from pdf:
	 * <pre>           (alpha^r)       (r-1)     (-alpha*x)
	 * pdf(x) = -------------  * x       * e
	 *           gammaFun(r)</pre>
	 * where r is the "shape" parameter, alpha is the "scale" parameter and
	 * gammaFun is the "Eulero" function.
	 */
	public Erlang() {
	}

	/**
	 * This method is used to obtain from the distribution its probability distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the pdf.
	 * @param p parameter of the erlang distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the probability distribution function evaluated in x.
	 */
	public double pdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((ErlangPar) p).getAlpha();
			long r = ((ErlangPar) p).getR();
			return (Math.pow(alpha, r) / Gamma.gamma(r)) * Math.pow(x, r - 1) * Math.exp(-alpha * x);
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution its cumulative distribution
	 * function evaluated where required by the user.
	 *
	 * @param x double indicating where to evaluate the cdf.
	 * @param p parameter of the erlang distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the cumulative distribution function evaluated in x.
	 */
	public double cdf(double x, Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((ErlangPar) p).getAlpha();
			long r = ((ErlangPar) p).getR();
			double sum = 0.0;
			double a; // service variable to calculate sum
			for (int i = 0; i < r - 1; i++) {
				a = Math.pow((alpha * x), i) / Arithmetic.factorial(i);
				sum = sum + a;
			}
			return 1.0 - Math.exp(-alpha * x) * sum;
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic mean.
	 *
	 * @param p parameter of the erlang distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic mean of the distribution.
	 *
	 * The theoretic mean is calculated as r/alpha.
	 */
	public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((ErlangPar) p).getAlpha();
			long r = ((ErlangPar) p).getR();
			return r / alpha;
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the value of its own
	 * theoretic variance.
	 *
	 * @param p parameter of the erlang distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the theoretic variance of the distribution.
	 *
	 * The theoretic variance is calculated as r/(alpha^2).
	 */
	public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((ErlangPar) p).getAlpha();
			long r = ((ErlangPar) p).getR();
			return r / (alpha * alpha);
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

	/**
	 * This method is used to obtain from the distribution the next number distributed
	 * according to the distribution parameter.
	 *
	 * @param p parameter of the erlang distribution.
	 * @throws IncorrectDistributionParameterException
	 * @return double with the next random number of this distribution.
	 */
	public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
		if (p.check()) {
			double alpha = ((ErlangPar) p).getAlpha();
			long r = ((ErlangPar) p).getR(); 
			double par = 1.0;
			for (int i = 0; i < r; i++) {
				par *= engine.raw();
			}
			return (-1.0 / alpha) * (Math.log(par));
		} else {
			throw new IncorrectDistributionParameterException("Remember: alpha and r must be gtz");
		}
	}

} // end Erlang
