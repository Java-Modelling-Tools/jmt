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

package jmt.gui.common.routingStrategies;

import java.util.HashMap;
import java.util.Map;

public class ClassSwitchRouting extends RoutingStrategy {

	public Map<Object, Double> values = new HashMap<Object, Double>();
	public Map<Object, Map<Object, Double>> outPaths = new HashMap<Object, Map<Object, Double>>();

	public ClassSwitchRouting() {
		description = "Jobs are routed to next stations and switch between classes according to the specified probabilities. "
				+ "If the sum of the routing or switching probabilities is different from 1, the values will be scaled to sum to 1.";
	}

	@Override
	public String getName() {
		return "Class Switch";
	}

	public Map<Object, Double> getValues() {
		return values;
	}

	public Map<Object, Map<Object, Double>> getOutPaths() {
		return outPaths;
	}

	@Override
	public ClassSwitchRouting clone() {
		ClassSwitchRouting csr = new ClassSwitchRouting();
		csr.values = cloneValues();
		csr.outPaths = cloneOutPaths();
		return csr;
	}

	private Map<Object, Double> cloneValues() {
		Map<Object, Double> bp = new HashMap<>();
		for (Object stationKey : values.keySet()) {
			bp.put(stationKey, values.get(stationKey));
		}
		return bp;
	}

	private Map<Object, Map<Object, Double>> cloneOutPaths() {
		Map<Object, Map<Object, Double>> cp = new HashMap<Object, Map<Object, Double>>();
		for (Object classKey : outPaths.keySet()) {
			cp.put(classKey, outPaths.get(classKey));
		}
		return cp;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.RoutingStrategies.ClassSwitchRoutingStrategy";
	}

	@Override
	public boolean isModelStateDependent() {
		return false;
	}

	@Override
	public void addStation(Object stationKey) {
		if (values.containsKey(stationKey)) {
			return;
		}
		values.put(stationKey, new Double(0.0));
	}

	@Override
	public void removeStation(Object stationKey) {
		values.remove(stationKey);
		outPaths.remove(stationKey);
	}

	public void addClass(Object classKey) {
		for (Object key : outPaths.keySet()) {
			outPaths.get(key).put(classKey, new Double(0.0));
		}
	}

	public void deleteClass(Object classKey) {
		for (Object key : outPaths.keySet()) {
			outPaths.get(key).remove(classKey);
		}
	}

}
