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
 * Predicates are used to select events from the deferred queue.
 * This class is abstract and must be subclassed when writing new
 * predicate.
 */

public abstract class SimPredicate {
	/**
	 * The match function which must be overridden when writing a new
	 * predicate. The function is called with each event in the deferred
	 * queue as its parameter when a <tt>SimSystem.simSelect()</tt>
	 * call is made by the user.
	 * @param event The event to test for a match.
	 * @return The function should return <tt>true</tt> if the
	 *         event matches and shoult be selected, of <tt>false</tt>
	 *         if it does not
	 */
	public abstract boolean match(SimEvent event);
}
