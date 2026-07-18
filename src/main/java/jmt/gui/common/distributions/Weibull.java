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

package jmt.gui.common.distributions;

import javax.swing.ImageIcon;

import jmt.engine.math.Gamma;
import jmt.gui.common.JMTImageLoader;

/**
 * <p>Title: Weibull distribution</p>
 * <p>Description: Weibull distribution data structure</p>
 * 
 * @author Giuliano Casale
 *         Date: 7-Aug-2021
 */
public class Weibull extends Distribution {

	/**
	 * Constructs a new Erlang Distribution
	 */
	public Weibull() {
		super("Weibull", "jmt.engine.random.Weibull", "jmt.engine.random.WeibullPar", "Weibull distribution");
		hasMean = true;
		hasC = true;
		isNestable = true;
	}

	/**
	 * Used to set parameters of this distribution
	 * @return distribution parameters
	 */
	@Override
	protected Parameter[] setParameters() {
		// Creates parameter array
		Parameter[] parameters = new Parameter[2];

		// Sets parameter lambda
		//ARIF: Left parameters name as alpha and r in order to have back-compatibility of the XML files.
		parameters[0] = new Parameter("alpha", "\u03BB", Double.class, new Double(0.445106618845));
		// Checks value of lambda must greater than 0
		parameters[0].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() > 0.0) {
					return true;
				} else {
					return false;
				}
			}
		});

		// Sets parameter k
		parameters[1] = new Parameter("r", "k", Double.class, new Double(0.471065636952));
		// Checks value of k must be greater than 0
		parameters[1].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() > 0) {
					return true;
				} else {
					return false;
				}
			}
		});

		return parameters;
	}

	/**
	 * Sets illustrating figure in distribution panel
	 * @return illustrating figure
	 */
	@Override
	protected ImageIcon setImage() {
		return JMTImageLoader.loadImage("Weibull");
	}

	/**
	 * Returns this distribution's short description
	 * @return distribution's short description
	 */
	@Override
	public String toString() {
		return "weibull(" + formatNumber(((Double) parameters[0].getValue()).doubleValue()) + ","
				+ formatNumber(((Double) parameters[1].getValue()).doubleValue()) + ")";
	}

	/**
	 * Sets the mean for this distribution
	 * @param value mean value
	 */
	@Override
	public void setMean(double value) {
		
		if (Math.abs(value - mean)>1e-12) {
			setCM(value, c);
		}
	}

	/**
	 * Sets the variation coefficient C for this distribution
	 * @param value variation coefficient C value
	 */
	@Override
	public void setC(double value) {
		if (Math.abs(value - c)>1e-12) {
			setCM(mean, value);
		}
	}

	/**
	 * Sets Mean and C values
	 * @param mean mean value
	 * @param c c value
	 */
	protected void setCM(double mean, double c) {
		// k = 1 / (c*c) && lambda = 1 / (mean*c*c)
		// Backups old parameters to restore them upon a false result
		Object oldr = getParameter("r").getValue();
		Object olda = getParameter("alpha").getValue();

		Double r = 0.0;
		Double alpha = 0.0;
		r= Math.pow(c, -1.086); // Justus approximation 1976
		alpha = mean / Gamma.gamma(1+1/r);
		
		if (getParameter("r").setValue(r) && getParameter("alpha").setValue(alpha)) {
			this.mean = mean;
			this.c = c;
		} else {
			getParameter("r").setValue(oldr);
			getParameter("alpha").setValue(olda);
		}
	}

	/**
	 * This method is called whenever a parameter changes and <code>hasMean</code> or
	 * <code>hasC</code> is true
	 */
	@Override
	public void updateCM() {
		double alpha = ((Double) getParameter("alpha").getValue()).doubleValue();
		double r = ((Double) getParameter("r").getValue()).doubleValue();
		mean = alpha * Gamma.gamma(1+1/r);
		c = Math.sqrt(Math.pow(alpha,2)*(Gamma.gamma(1+2/r)-Math.pow(Gamma.gamma(1+1/r),2)))/mean;
	}

}
