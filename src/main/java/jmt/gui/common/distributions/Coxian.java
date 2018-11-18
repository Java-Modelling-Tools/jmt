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

import jmt.gui.common.JMTImageLoader;

/**
 * <p>Title: Coxian Distribution</p>
 * <p>Description: Coxian distribution data structure</p>
 * 
 * @author Casale Giuliano
 * @date   September 9, 2017
 */
public class Coxian extends Distribution {

	/**
	 * Constructs a new Coxian distribution
	 */
	public Coxian() {
		super("Coxian", "jmt.engine.random.CoxianDistr", "jmt.engine.random.CoxianPar", "Coxian distribution");
		hasMean = true;
		hasC = true;
		isNestable = true;
	}

	/**
	 * Used to set parameters of this distribution.
	 * @return distribution parameters
	 */
	@Override
	protected Parameter[] setParameters() {
		// Creates parameter array
		Parameter[] parameters = new Parameter[3];

		// Sets parameter lambda0
		parameters[0] = new Parameter("lambda0", "\u03BB0", Double.class, new Double(1.0));
		// Checks value of lambda0 must be greater than 0
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

		// Sets parameter lambda1
		parameters[1] = new Parameter("lambda1", "\u03BB1", Double.class, new Double(0.125));
		// Checks value of lambda1 must be greater than 0
		parameters[1].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() > 0.0) {
					return true;
				} else {
					return false;
				}
			}
		});

		// Sets parameter phi0
		parameters[2] = new Parameter("phi0", "p0", Double.class, new Double(0.875));
		// Checks value of phi0 must be between 0 and 1
		parameters[2].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() >= 0.0 && d.doubleValue() <= 1.0) {
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
		return JMTImageLoader.loadImage("Coxian");
	}

	/**
	 * Returns this distribution's short description
	 * @return distribution's short description
	 */
	@Override
	public String toString() {
		return "cox(" + formatNumber(((Double) parameters[0].getValue()).doubleValue()) + ","
				+ formatNumber(((Double) parameters[1].getValue()).doubleValue()) + ","
				+ formatNumber(((Double) parameters[2].getValue()).doubleValue()) + ")";
	}

	/**
	 * Sets the mean for this distribution
	 * @param value mean value
	 */
	@Override
	public void setMean(double value) {
		setCM(value, c);
	}

	/**
	 * Sets the variation coefficient C for this distribution
	 * @param value variation coefficient C value
	 */
	@Override
	public void setC(double value) {
		setCM(mean, value);
	}

	/**
	 * Sets Mean and C values
	 * @param mean mean value
	 * @param c c value
	 */
	protected void setCM(double mean, double c) {
		if (c * c < 0.5) {
			return;
		}

		double l0, l1, p0;
		if (c < 1) {
			l0 = (2 / mean) / (1 + Math.sqrt(1 + 2 * (c * c - 1)));
			l1 = (2 / mean) / (1 - Math.sqrt(1 + 2 * (c * c - 1)));
			p0 = 0;
		} else if (c == 1) {
			l0 = 1 / mean;
			l1 = 1 / mean;			
			p0 = 1;
		} else {
			l0 = 2 / mean;
			l1 = 1 / (mean * c * c);
			p0 = 1 - 1 / (2 * c * c);
		}

		getParameter(0).setValue(new Double(l0));
		getParameter(1).setValue(new Double(l1));
		getParameter(2).setValue(new Double(p0));
		this.mean = mean;
		this.c = c;
	}

	/**
	 * This method is called whenever a parameter changes and <code>hasMean</code> or
	 * <code>hasC</code> is true
	 */
	@Override
	public void updateCM() {
		double l0, l1, p0;
		l0 = ((Double) getParameter(0).getValue()).doubleValue();
		l1 = ((Double) getParameter(1).getValue()).doubleValue();
		p0 = ((Double) getParameter(2).getValue()).doubleValue();
		mean = 1 / l0 + (1 - p0) / l1;
		c = Math.sqrt(1 / (l0 * l0) + (1 - p0 * p0) / (l1 * l1)) / mean;
	}

}
