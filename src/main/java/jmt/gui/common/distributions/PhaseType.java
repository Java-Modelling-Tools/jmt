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
 * <p>Title: Phase-Type Distribution</p>
 * <p>Description: Phase-Type distribution data structure</p>
 * 
 * @author Lulai Zhu
 * @date   December 26, 2017
 */
public class PhaseType extends Distribution {

	/**
	 * Constructs a new Phase-Type distribution
	 */
	public PhaseType() {
		super("Phase-Type", "jmt.engine.random.PhaseTypeDistr", "jmt.engine.random.PhaseTypePar", "Phase-Type distribution");
		hasMean = false;
		hasC = false;
		isNestable = false;
		setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Object[][] alpha = (Object[][]) parameters[0].getValue();
				if (!checkProbabilityVector(alpha)) {
					normalizeProbabilityVector(alpha);
					precondition = "Initial Probability Vector \u03B1: the entries have been normalized to sum up to one.";
					return false;
				}
				Object[][] T = (Object[][]) parameters[1].getValue();
				if (!checkTransitionRateMatrix(T)) {
					precondition = "Transition Rate Matrix T: the diagnal may only contain negative entries;\n"
							+ "the entries in each row must sum up to a non-positive value.";
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

		// Sets parameter alpha
		Object[][] alpha = new Object[1][2];
		alpha[0][0] = Double.valueOf(0.5);
		alpha[0][1] = Double.valueOf(0.5);
		parameters[0] = new Parameter("alpha", "Initial Probability Vector \u03B1", Double.class, alpha);
		// Checks entries of alpha must be greater than or equal to 0
		parameters[0].setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				Object[][] v = (Object[][]) value;
				for (int i = 0; i < v[0].length; i++) {
					Double d = (Double) v[0][i];
					if (d.doubleValue() < 0.0) {
						return false;
					}
				}
				return true;
			}
		});

		// Sets parameter T
		Object[][] T = new Object[2][2];
		T[0][0] = Double.valueOf(-1.0);
		T[0][1] = Double.valueOf(0.4);
		T[1][0] = Double.valueOf(0.6);
		T[1][1] = Double.valueOf(-1.0);
		parameters[1] = new Parameter("T", "Transition Rate Matrix T", Double.class, T);
		// Checks diagonal entries of T must be less than or equal to 0
		// Checks non-diagonal entries of T must be greater than or equal to 0
		parameters[1].setValueChecker(new ValueChecker() {
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

		return parameters;
	}

	/**
	 * Sets illustrating figure in distribution panel
	 * @return illustrating figure
	 */
	@Override
	protected ImageIcon setImage() {
		return JMTImageLoader.loadImage("PhaseType");
	}

	/**
	 * Returns this distribution's short description
	 * @return distribution's short description
	 */
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("ph([");
		Object[][] alpha = (Object[][]) parameters[0].getValue();
		for (int i = 0; i < alpha[0].length; i++) {
			Double d = (Double) alpha[0][i];
			buff.append(formatNumber(d.doubleValue()));
			if (i < alpha[0].length - 1) {
				buff.append(",");
			}
		}
		buff.append("],[");
		Object[][] T = (Object[][]) parameters[1].getValue();
		for (int i = 0; i < T.length; i++) {
			for (int j = 0; j < T[i].length; j++) {
				Double d = (Double) T[i][j];
				buff.append(formatNumber(d.doubleValue()));
				if (j < T[i].length - 1) {
					buff.append(",");
				}
			}
			if (i < T.length - 1) {
				buff.append(";");
			}
		}
		buff.append("])");
		return buff.toString();
	}

}
