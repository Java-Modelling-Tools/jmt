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

package jmt.engine.NetStrategies.TransitionUtilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: Transition Matrix</p>
 * <p>Description: This class implements the transition matrix.</p>
 *
 * @author Lulai Zhu
 * Date: 06-10-2016
 * Time: 14.00.00
 */
public class TransitionMatrix {

	private Map<String, TransitionVector> matrix;
	private int total;
	private int[] totals;

	public TransitionMatrix(int size) {
		matrix = new HashMap<String, TransitionVector>();
		total = 0;
		totals = new int[size];
		Arrays.fill(totals, 0);
	}

	public TransitionMatrix(TransitionVector[] vectors) {
		this(vectors.length == 0 ? 0 : vectors[0].size());
		for (int i = 0; i < vectors.length; i++) {
			matrix.put(vectors[i].getKey(), vectors[i]);
			total += vectors[i].getTotal();
			for (int j = 0; j < vectors[i].size(); j++) {
				totals[j] += vectors[i].getEntry(j);
			}
		}
	}

	public Set<String> keySet() {
		return matrix.keySet();
	}

	public int getEntry(String key, int index) {
		return matrix.get(key).getEntry(index);
	}

	public void setEntry(String key, int index, int value) {
		matrix.get(key).setEntry(index, value);
		total = -1;
		totals[index] = -1;
	}

	public TransitionVector getVector(String key) {
		return matrix.get(key);
	}

	public void setVector(TransitionVector vector) {
		matrix.put(vector.getKey(), vector);
		total = -1;
		Arrays.fill(totals, -1);
	}

	public int getTotal() {
		if (total < 0) {
			total = 0;
			for (TransitionVector vector : matrix.values()) {
				total += vector.getTotal();
			}
		}
		return total;
	}

	public int getTotal(String key) {
		return matrix.get(key).getTotal();
	}

	public int getTotal(int index) {
		if (totals.length == 0) {
			return 0;
		}
		if (totals[index] < 0) {
			totals[index] = 0;
			for (TransitionVector vector : matrix.values()) {
				totals[index] += vector.getEntry(index);
			}
		}
		return totals[index];
	}

}
