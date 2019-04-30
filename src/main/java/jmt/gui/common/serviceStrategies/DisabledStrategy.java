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

package jmt.gui.common.serviceStrategies;

/**
 * <p><b>Name:</b> DisabledStrategy</p> 
 * <p><b>Description: This is a special service strategy that raises a warning when a job requires service.</b> 
 * 
 * </p>
 * <p><b>Date:</b> 04-nov-2017
 * <b>Time:</b> 18.05.40</p>
 * @author Giuliano Casale
 * @version 1.0
 */
public class DisabledStrategy implements ServiceStrategy {

	/**
	 * Returns engine classpath for Disabled Service strategy
	 * @return engine classpath for Disabled Service strategy
	 */
	public static String getEngineClassPath() {
		return "jmt.engine.NetStrategies.ServiceStrategies.DisabledServiceTimeStrategy";
	}

	/**
	 * Clones this strategy. In this case it simply returns a new DisabledStrategy
	 * @see java.lang.Object#clone()
	 */
	@Override
	public DisabledStrategy clone() {
		return new DisabledStrategy();
	}

	/**
	 * Returns the value of this strategy
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Unspecified";
	}

}
