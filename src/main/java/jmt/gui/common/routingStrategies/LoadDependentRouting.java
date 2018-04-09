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

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ashanka
 * Date: 9/1/11
 * Time: 12:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoadDependentRouting extends RoutingStrategy {

	Map<Integer, Map<Object, Double>> values = new HashMap<Integer, Map<Object, Double>>();

	public LoadDependentRouting() {
		description = "Customers of each class are routed depending on their number on the selected station. "
				+ "The outgoing paths must have associated probabilities that should sum to 1 for each range.";
	}

	@Override
	public String getName() {
		return "Load Dependent Routing";
	}

	@Override
	@Deprecated
	public Map<Object, Double> getValues() {
		return null;
	}

	@Override
	public RoutingStrategy clone() {
		LoadDependentRouting ldr = new LoadDependentRouting();
		for (Integer from : values.keySet()) {
			Map<Object, Double> entries = values.get(from);
			for (Object stationKey : entries.keySet()) {
				ldr.addEmpiricalEntry(from, stationKey, entries.get(stationKey));
			}
		}
		return ldr;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.RoutingStrategies.LoadDependentRoutingStrategy";
	}

	public Map<Integer, Map<Object, Double>> getAllEmpiricalEntries() {
		return values;
	}

	public Map<Object, Double> getEmpiricalEntriesForFrom(Integer from) {
		return values.get(from);
	}

	public void removeEmpiricalEntriesForFrom(Integer from) {
		values.remove(from);
	}

	public void addEmpiricalEntry(Integer from, Object stationKey, Double probability) {
		Map<Object, Double> entries = values.get(from);
		if (entries == null) {
			entries = new HashMap<Object, Double>();
			values.put(from, entries);
		}
		entries.put(stationKey, probability);
	}

	/**
	 * Validates the Routing to see if all the probabilities sum to 1.
	 * If there is no error then it returns List with size 0.
	 * @return
	 */
	public List<String> validate() {
		List<String> errors = new ArrayList<String>();
		if (values.size() == 0) {
			errors.add("No routing defined");
		} else {
			List<Integer> fromAsList = new ArrayList<Integer>();
			fromAsList.addAll(values.keySet());
			Collections.sort(fromAsList);
			for (Integer from : fromAsList) {
				Map<Object, Double> entries = values.get(from);
				double sum = 0.0;
				for (Double value : entries.values()) {
					sum = sum + value.doubleValue();
				}
				if (sum - 1.0 < -1e-6) {
					if (errors.isEmpty()) {
						errors.add("Normalize sum of probabilities to 1.0 for range(s):\n\"From\" : \"" + from + "\"");
					} else {
						errors.add("\"From\" : \"" + from + "\"");
					}
				} else if (sum - 1.0 > 1e-6) {
					if (errors.isEmpty()) {
						errors.add("Normalize sum of probabilities to 1.0 for range(s):\n\"From\" : \"" + from + "\"");
					} else {
						errors.add("\"From\" : \"" + from + "\"");
					}
				}
			}
		}
		return errors;
	}

	public void refreshRouting(Vector<Object> stationKeys, Vector<Object> validStationKeys) {
		for (Integer from : values.keySet()) {
			Map<Object, Double> entries = values.get(from);
			Map<Object, Double> temp = new HashMap<Object, Double>(entries);
			entries.clear();
			for (Object stationKey : stationKeys) {
				if (temp.containsKey(stationKey)) {
					entries.put(stationKey, temp.get(stationKey));
				} else {
					entries.put(stationKey, new Double(0.0));
				}
				if (!validStationKeys.contains(stationKey)) {
					entries.put(stationKey, new Double(0.0));
				}
			}
		}
	}

	/**
	 * Returns true if the routing strategy is dependent on the state of
	 * the model
	 * @return true if the routing strategy is dependent on the state of
	 * the model
	 *
	 * Author: Francesco D'Aquino
	 */
	@Override
	public boolean isModelStateDependent() {
		return false;
	}

	@Override
	public void addStation(Object stationKey) {
		for (Integer from : values.keySet()) {
			values.get(from).put(stationKey, new Double(0.0));
		}
	}

	@Override
	public void removeStation(Object stationKey) {
		for (Integer from : values.keySet()) {
			values.get(from).remove(stationKey);
		}
	}

}
