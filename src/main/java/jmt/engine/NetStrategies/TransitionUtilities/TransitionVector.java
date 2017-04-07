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

/**
 * <p>Title: Transition Vector</p>
 * <p>Description: This class implements the transition vector.</p>
 *
 * @author Lulai Zhu
 * Date: 15-07-2016
 * Time: 21.00.00
 */
public class TransitionVector {

	private String key;
	private int[] entries;
	private int total;

	public TransitionVector(String key, int size) {
		this.key = key;
		entries = new int[size];
		Arrays.fill(entries, 0);
		total = 0;
	}

	public TransitionVector(String key, Integer[] entries) {
		this(key, entries.length);
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].intValue() < 0) {
				entries[i] = Integer.valueOf(0);
			}
			this.entries[i] = entries[i].intValue();
			total += entries[i].intValue();
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int size() {
		return entries.length;
	}

	public int getEntry(int index) {
		return entries[index];
	}

	public void setEntry(int index, int value) {
		if (value < 0) {
			value = 0;
		}
		entries[index] = value;
		total = -1;
	}

	public int getTotal() {
		if (total < 0) {
			total = 0;
			for (int i = 0; i < entries.length; i++) {
				total += entries[i];
			}
		}
		return total;
	}

}
