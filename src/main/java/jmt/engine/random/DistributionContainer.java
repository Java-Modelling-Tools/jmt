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

/**
 * <p>Title:Distribution Container</p>
 * <p>Description: Data structure that stores a nested distribution</p>
 * @author Peter Parapatics
 *         Date: 17-dec-1007
 */
public class DistributionContainer {
	protected Distribution distribution;
	protected Parameter parameter;

	/**
	 * Constructs a Distribution Container with a given distribution and a parameter
	 * @param distribution the distribution to be stored
	 * @param parameter the parameter of the distribution
	 */
	public DistributionContainer(Distribution distribution, Parameter parameter) {
		this.distribution = distribution;
		this.parameter = parameter;
	}

	/**
	 * Get method for the distribution
	 * @return the distribution
	 */
	public Distribution getDistribution() {
		return distribution;
	}

	/**
	 * Get method for the distribution parameter
	 * @return the parameter belonging to the distribution
	 */
	public Parameter getParameter() {
		return parameter;
	}

}
