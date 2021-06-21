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

package jmt.gui.common.joinStrategies;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mattia
 */
public class GuardJoin extends JoinStrategy {

	private Map<Object, Integer> guard = new HashMap<Object, Integer>();

	public GuardJoin() {
		description = "Fires when a subset of class-specific tasks forked from the same job arrive at the join. "
				+ "Pending tasks are discarded upon arrival.";
	}

	@Override
	public String getName() {
		return "Guard";
	}

	@Override
	public int getRequiredNum() {
		int sum = 0;
		for (Integer i: guard.values())
			sum += i;
		return sum;
	}

	@Override
	public void setRequiredNum(int i) {
		return;
	}

	public Map<Object, Integer> getGuard() {
		return guard;
	}

	public void setGuard(Map<Object, Integer> mix) {
		this.guard = mix;
	}

	@Override
	public JoinStrategy clone() {
		GuardJoin gj = new GuardJoin();
		for (Object classKey : guard.keySet()) {
			gj.guard.put(classKey, guard.get(classKey));
		}
		return gj;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.JoinStrategies.GuardJoin";
	}

	@Override
	public boolean isModelStateDependent() {
		return false;
	}

}
