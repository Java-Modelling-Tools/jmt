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
import java.util.Vector;

/**
 *
 * @author mattia
 */
public class ClassSwitchFork extends ForkStrategy {

	private Map<Object, OutPath> outPaths = new HashMap<Object, OutPath>();

	public ClassSwitchFork() {
		description = "Generates tasks of multiple classes (the same number on each output link).";
	}

	@Override
	public String getName() {
		return "Class Switch";
	}

	@Override
	public Map<Object, OutPath> getOutDetails() {
		return outPaths;
	}

	@Override
	public ForkStrategy clone() {
		ClassSwitchFork csf = new ClassSwitchFork();
		for (Object stationKey : outPaths.keySet()) {
			csf.outPaths.put(stationKey, (OutPath) (outPaths.get(stationKey)).clone());
		}
		return csf;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.ForkStrategies.ClassSwitchFork";
	}

	@Override
	public boolean isModelStateDependent() {
		return false;
	}

	@Override
	public void addStation(Object stationKey, Object classKey, Vector<Object> classKeys) {
		OutPath tempPath = new OutPath();
		if (!outPaths.values().isEmpty()) {
			OutPath path = outPaths.values().iterator().next();
			for (Object o : path.getOutParameters().keySet()) {
				tempPath.putEntry(o, path.getOutParameters().get(o));
			}
		} else {
			for (Object o : classKeys) {
				if (o == classKey) {
					tempPath.putEntry(o, 1);
				} else {
					tempPath.putEntry(o, 0);
				}                                
			}
		}
		outPaths.put(stationKey, tempPath);
	}

	@Override
	public void removeStation(Object stationKey) {
		outPaths.remove(stationKey);
	}

}
