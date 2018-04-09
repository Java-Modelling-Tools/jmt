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
 * <p>Title: Burst Distribution</p>
 * <p>Description: Burst distribution data structure</p>
 * 
 * @author Peter Parapatics
 *         Date: 12-dic-2007
 */
public class Burst extends Distribution {

	/**
	 * Constructs a new Burst distribution
	 */
	public Burst() {
		super("Burst (General)", "jmt.engine.random.Burst", "jmt.engine.random.BurstPar", "Burst distribution");
		hasC = false;
		hasMean = false;
		isNestable = false;
		setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				if (parameters[1].getValue() == null || parameters[2].getValue() == null
						|| parameters[3].getValue() == null || parameters[4].getValue() == null) {
					precondition = "All interval-length and value distributions have to be defined.";
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
		Parameter[] parameters = new Parameter[6];

		//Set the probability parameter
		//This parameter has to be added directly to the Distribution in the XML
		parameters[0] = new Parameter("Probability", "Probability", Double.class, new Double(0.5), true);

		//Set the interval length distribution of interval A
		//This parameter has to be added directly to the Distribution in the XML
		parameters[1] = new Parameter("Length-Distribution_IntervalA", "Interval A - Length Distribution", Distribution.class, null, true);

		//Set the value distribution of interval A
		parameters[2] = new Parameter("Value-Distribution_IntervalA", "Interval A - Value Distribution", Distribution.class, null);

		//Set the interval length distribution of interval B
		//This parameter has to be added directly to the Distribution in the XML
		parameters[3] = new Parameter("Length-Distribution_IntervalB", "Interval B - Length Distribution", Distribution.class, null, true);

		//Set the value distribution of interval B
		parameters[4] = new Parameter("Value-Distribution_IntervalB", "Interval B - Value Distribution", Distribution.class, null);

		//Set the round-robin parameter
		//This parameter has to be added directly to the Distribution in the XML
		parameters[5] = new Parameter("Round-Robin", "Round-Robin", Boolean.class, new Boolean(false), true);

		return parameters;
	}

	/**
	 * Sets illustrating figure in distribution panel
	 * @return illustrating figure
	 */
	@Override
	protected ImageIcon setImage() {
		return JMTImageLoader.loadImage("Burst");
	}

	/**
	 * Returns this distribution's short description
	 * @return distribution's short description
	 */
	@Override
	public String toString() {
		return "burst(" + formatNumber(((Double) parameters[0].getValue()).doubleValue()) + ","
				+ parameters[2].getValue() + "," + parameters[4].getValue() + ")";
	}

}
