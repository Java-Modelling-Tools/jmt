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

package jmt.gui.common.forkStrategies;

import java.util.HashMap;
import java.util.Map;

public class OutPath {

	private Double probability;
	private Map<Object, Object> outParameters;

	public OutPath() {
		probability = 0.0;
		outParameters = new HashMap<Object, Object>();
	}

	@Override
	public Object clone() {
		OutPath op = new OutPath();
		op.probability = probability;
		for (Object key : outParameters.keySet()) {
			op.outParameters.put(key, outParameters.get(key));
		}
		return op;
	}

	public Double getProbability() {
		return probability;
	}

	public void setProbability(Double probability) {
		this.probability = probability;
	}

	public Map<Object, Object> getOutParameters() {
		return outParameters;
	}

	public void setOutParameters(Map<Object, Object> outParameters) {
		this.outParameters = outParameters;
	}

	public void putEntry(Object key, Object value) {
		outParameters.put(key, value);
	}

}
