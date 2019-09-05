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

package jmt.gui.common.transitionUtilities;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: Transition Matrix</p>
 * <p>Description: This class implements the transition matrix.</p>
 *
 * @author Lulai Zhu
 * Date: 06-10-2016
 * Time: 14.00.00
 */
public class TransitionMatrix {

	private Map<Object, TransitionVector> matrix;

	public TransitionMatrix() {
		matrix = new HashMap<Object, TransitionVector>();
	}

	@Override
	public Object clone() {
		TransitionMatrix tm = new TransitionMatrix();
		for (Object rowKey : matrix.keySet()) {
			tm.matrix.put(rowKey, (TransitionVector) matrix.get(rowKey).clone());
		}
		return tm;
	}

	public Integer getEntry(Object rowKey, Object colKey) {
		if (!matrix.containsKey(rowKey)) {
			matrix.put(rowKey, new TransitionVector());
		}
		return matrix.get(rowKey).getEntry(colKey);
	}

	public void setEntry(Object rowKey, Object colKey, Integer value) {
		if (!matrix.containsKey(rowKey)) {
			matrix.put(rowKey, new TransitionVector());
		}
		matrix.get(rowKey).setEntry(colKey, value);
	}

	public void removeRow(Object rowKey) {
		matrix.remove(rowKey);
	}

	public void removeColumn(Object colKey) {
		for (Object rowKey : matrix.keySet()) {
			matrix.get(rowKey).setEntry(colKey, Integer.valueOf(0));
		}
	}

}
