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

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 7-lug-2005
 * Time: 15.25.12
 * To change this template use Options | File Templates.
 */
public class ProbabilityRouting extends RoutingStrategy {

	private Map<Object, Double> values = new HashMap<Object, Double>();

	public ProbabilityRouting() {
		description = "Jobs are routed to stations connected to the current one according to the specified probabilities. "
				+ "If the sum of the probabilities is different from 1, all the values will be scaled to sum 1.";
	}

	@Override
	public String getName() {
		return "Probabilities";
	}

	@Override
	public Map<Object, Double> getValues() {
		return values;
	}

	@Override
	public ProbabilityRouting clone() {
		ProbabilityRouting pr = new ProbabilityRouting();
		for (Object stationKey : values.keySet()) {
			pr.values.put(stationKey, values.get(stationKey));
		}
		return pr;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.RoutingStrategies.EmpiricalStrategy";
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
		values.put(stationKey, new Double(0.0));
	}

	@Override
	public void removeStation(Object stationKey) {
		values.remove(stationKey);
	}

}
