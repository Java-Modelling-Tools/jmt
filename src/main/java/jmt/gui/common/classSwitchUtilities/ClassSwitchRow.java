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

package jmt.gui.common.classSwitchUtilities;

import java.util.HashMap;
import java.util.Map;

public class ClassSwitchRow {

	private Object classInKey;
	private Map<Object, Float> rowValues;

	public ClassSwitchRow() {
		this.classInKey = null;
		this.rowValues = new HashMap<Object, Float>();
	}

	@Override
	public Object clone() {
		ClassSwitchRow csr = new ClassSwitchRow();
		csr.classInKey = classInKey;
		for (Object classOutKey : rowValues.keySet()) {
			csr.rowValues.put(classOutKey, rowValues.get(classOutKey));
		}
		return csr;
	}

	public float getValue(Object classInKey, Object classOutKey) {
		if (this.classInKey == null) {
			this.classInKey = classInKey;
			this.rowValues.put(classInKey, new Float(1.0f));
		}
		if (!this.rowValues.containsKey(classOutKey)) {
			return 0.0f;
		}
		return this.rowValues.get(classOutKey);
	}

	public void setValue(Object classInKey, Object classOutKey, float value) {
		if (this.classInKey == null) {
			this.classInKey = classInKey;
			this.rowValues.put(classInKey, new Float(1.0f));
		}
		if (value <= 0.0f) {
			this.rowValues.remove(classOutKey);
			return;
		}
		this.rowValues.put(classOutKey, new Float(value));
	}

}
