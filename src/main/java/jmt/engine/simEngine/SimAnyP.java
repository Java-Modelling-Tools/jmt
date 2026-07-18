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

package jmt.engine.simEngine;

/**
 * A predicate which will match any event on the deferred event queue.
 * There is a publicly accessible instance of this predicate in the
 * Sim_system class, called Sim_system.SIM_ANY, so the user does
 * not need to create any new instances.
 * @see         eduni.simjava.Sim_predicate
 * @see         eduni.simjava.Sim_system
 * @version     1.0, 4 September 1996
 * @author      Ross McNab
 */

public class SimAnyP extends SimPredicate {
	/** Constructor.
	 */
	public SimAnyP() {
	};

	/** The match function called by Sim_system.simSelect(),
	 * not used directly by the user
	 */
	@Override
	public boolean match(SimEvent ev) {
		return true;
	}
}
