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
 * <p>Title: MAP Distribution</p>
 * <p>Description: MAP distribution data structure</p>
 * 
 * @author Lulai Zhu
 * @date   December 26, 2017
 */
public class MAP extends Distribution {

	/**
	 * Constructs a new MAP distribution
	 */
	public MAP() {
		super("Burst (MAP)", "jmt.engine.random.MAPDistr", "jmt.engine.random.MAPPar", "MAP distribution");
		hasMean = false;
		hasC = false;
		isNestable = false;
		setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Object[][] D0 = (Object[][]) parameters[0].getValue();
				if (!checkTransitionRateMatrix(D0)) {
					precondition = "Hidden Transition Rate Matrix D0: the diagnal may only contain negative entries;\n"
							+ "the entries in each row must sum up to a non-positive value.";
					return false;
				}
				Object[][] D1 = (Object[][]) parameters[1].getValue();
				if (!checkInfinitesimalGeneratorMatrix(D0, D1)) {
					precondition = "Infinitesimal Generator Matrix D0 + D1: the entries in each row must sum up to zero.";
					return false;
				}
				return true;
			}
		});
	}

	/**
	 * Used to set parameters of this distribution.
	 * @return distribution parameters
	 */
	@Override
	protected Parameter[] setParameters() {
		// Creates parameter array
		Parameter[] parameters = new Parameter[2];

		// Sets parameter D0
		Object[][] D0 = new Object[2][2];
		D0[0][0] = Double.valueOf(-1.0);
		D0[0][1] = Double.valueOf(0.4);
		D0[1][0] = Double.valueOf(0.6);
		D0[1][1] = Double.valueOf(-1.0);
		parameters[0] = new Parameter("D0", "Hidden Transition Rate Matrix D0", Double.class, D0);
		// Checks diagonal entries of D0 must be less than or equal to 0
		// Checks non-diagonal entries of D0 must be greater than or equal to 0
		parameters[0].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Object[][] m = (Object[][]) value;
				for (int i = 0; i < m.length; i++) {
					for (int j = 0; j < m[i].length; j++) {
						Double d = (Double) m[i][j];
						if (i == j && d.doubleValue() > 0.0) {
							return false;
						}
						if (i != j && d.doubleValue() < 0.0) {
							return false;
						}
					}
				}
				return true;
			}
		});

		// Sets parameter D1
		Object[][] D1 = new Object[2][2];
		D1[0][0] = Double.valueOf(0.4);
		D1[0][1] = Double.valueOf(0.2);
		D1[1][0] = Double.valueOf(0.0);
		D1[1][1] = Double.valueOf(0.4);
		parameters[1] = new Parameter("D1", "Observable Transition Rate Matrix D1", Double.class, D1);
		// Checks entries of D1 must be greater than or equal to 0
		parameters[1].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Object[][] m = (Object[][]) value;
				for (int i = 0; i < m.length; i++) {
					for (int j = 0; j < m[i].length; j++) {
						Double d = (Double) m[i][j];
						if (d.doubleValue() < 0.0) {
							return false;
						}
					}
				}
				return true;
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
		return JMTImageLoader.loadImage("MAP");
	}

	/**
	 * Returns this distribution's short description
	 * @return distribution's short description
	 */
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("map([");
		Object[][] D0 = (Object[][]) parameters[0].getValue();
		for (int i = 0; i < D0.length; i++) {
			for (int j = 0; j < D0[i].length; j++) {
				Double d = (Double) D0[i][j];
				buff.append(formatNumber(d.doubleValue()));
				if (j < D0[i].length - 1) {
					buff.append(",");
				}
			}
			if (i < D0.length - 1) {
				buff.append(";");
			}
		}
		buff.append("],[");
		Object[][] D1 = (Object[][]) parameters[1].getValue();
		for (int i = 0; i < D1.length; i++) {
			for (int j = 0; j < D1[i].length; j++) {
				Double d = (Double) D1[i][j];
				buff.append(formatNumber(d.doubleValue()));
				if (j < D1[i].length - 1) {
					buff.append(",");
				}
			}
			if (i < D1.length - 1) {
				buff.append(";");
			}
		}
		buff.append("])");
		return buff.toString();
	}

}
