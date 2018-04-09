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
 * <p>Title: MMPP2 Distribution</p>
 * <p>Description: MMPP2 distribution data structure</p>
 * 
 * @author Casale Giuliano
 * @date   October 24, 2008
 */
public class MMPP2 extends Distribution {

	/**
	 * Constructs a new MMPP2 distribution
	 */
	public MMPP2() {
		super("Burst (MMPP2)", "jmt.engine.random.MMPP2Distr", "jmt.engine.random.MMPP2Par", "MMPP2 distribution");
		hasMean = false;
		hasC = false;
		isNestable = false;
	}

	/**
	 * Used to set parameters of this distribution.
	 * @return distribution parameters
	 */
	@Override
	protected Parameter[] setParameters() {
		// Creates parameter array
		Parameter[] parameters = new Parameter[4];

		// Sets parameter lambda0
		parameters[0] = new Parameter("lambda0", "\u03BB0", Double.class, new Double(12.0));
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
		parameters[1] = new Parameter("lambda1", "\u03BB1", Double.class, new Double(0.0872));
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

		// Sets parameter sigma0
		parameters[2] = new Parameter("sigma0", "\u03C30", Double.class, new Double(0.0098));
		// Checks value of sigma0 must greater than 0
		parameters[2].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() > 0.0) {
					return true;
				} else {
					return false;
				}
			}
		});

		// Sets parameter sigma1
		parameters[3] = new Parameter("sigma1", "\u03C31", Double.class, new Double(0.0008));
		// Checks value of sigma1 must be greater than 0
		parameters[3].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Double d = (Double) value;
				if (d.doubleValue() > 0.0) {
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
		return JMTImageLoader.loadImage("MMPP2");
	}

	/**
	 * Return this distribution's short description
	 * @return distribution's short description
	 */
	@Override
	public String toString() {
		return "mmpp2(" + formatNumber(((Double) parameters[0].getValue()).doubleValue()) + ","
				+ formatNumber(((Double) parameters[1].getValue()).doubleValue()) + ","
				+ formatNumber(((Double) parameters[2].getValue()).doubleValue()) + ","
				+ formatNumber(((Double) parameters[3].getValue()).doubleValue()) + ")";
	}

}
