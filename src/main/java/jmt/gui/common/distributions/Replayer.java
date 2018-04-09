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

import java.io.File;

import javax.swing.ImageIcon;

import jmt.gui.common.JMTImageLoader;

/**
 * <p>Title: Replayer virtual distribution</p>
 * <p>Description: This distribution will repeat previously generated data
 * reading them cyclically from a data file.</p>
 * 
 * @author Bertoli Marco
 *         Date: 12-lug-2005
 *         Time: 11.29.26
 */
public class Replayer extends Distribution {

	public static final String FILE_NAME_PARAMETER = "fileName";

	/**
	 * Constructs a new Replayer Distribution
	 */
	public Replayer() {
		super("Replayer", "jmt.engine.random.Replayer", "jmt.engine.random.ReplayerPar", "Replayer distribution");
		hasMean = false;
		hasC = false;
		isNestable = true;
		setValueChecker(new ValueChecker() {
			public boolean checkValue(Object value) {
				if (!(new File((String) parameters[0].getValue())).exists()) {
					precondition = "Specified 'fileName' does not exist. Try to provide a correct path.";
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
		Parameter[] parameters = new Parameter[1];
		// Sets parameter fileName
		parameters[0] = new Parameter(FILE_NAME_PARAMETER, FILE_NAME_PARAMETER, String.class, "");
		return parameters;
	}

	/**
	 * Sets illustrating figure in distribution panel
	 * @return illustrating figure
	 */
	@Override
	protected ImageIcon setImage() {
		return JMTImageLoader.loadImage("Replayer");
	}

	/**
	 * Returns this distribution's short description
	 * @return distribution's short description
	 */
	@Override
	public String toString() {
		return "Replayer";
	}

}
