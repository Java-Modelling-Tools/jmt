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
 * A predicate to select events with specific tags from the deferred
 * event queue.
 * @see         eduni.simjava.Sim_predicate
 * @version     1.0, 4 September 1996
 * @author      Ross McNab
 */

public class SimTypeP extends SimPredicate {
	private int tag1;
	private int tag2;
	private int tag3;
	private int ntags;

	/** Constructor.
	 * @param t1   An event tag value
	 */
	public SimTypeP(int t1) {
		tag1 = t1;
		ntags = 1;
	}

	/** Constructor.
	 * @param t1   An event tag value
	 * @param t2   An event tag value
	 */
	public SimTypeP(int t1, int t2) {
		tag1 = t1;
		tag2 = t2;
		ntags = 2;
	}

	/** Constructor.
	 * @param t1   An event tag value
	 * @param t2   An event tag value
	 * @param t3   An event tag value
	 */
	public SimTypeP(int t1, int t2, int t3) {
		tag1 = t1;
		tag2 = t2;
		tag3 = t3;
		ntags = 3;
	}

	/** The match function called by SimSystem.simSelect(),
	 * not used directly by the user
	 */
	@Override
	public boolean match(SimEvent ev) {
		switch (ntags) {
			case 1:
				return (ev.getTag() == tag1);
			case 2:
				return (ev.getTag() == tag1) || (ev.getTag() == tag2);
			case 3:
				return (ev.getTag() == tag1) || (ev.getTag() == tag2) || (ev.getTag() == tag3);
		}
		return false;
	}
}
