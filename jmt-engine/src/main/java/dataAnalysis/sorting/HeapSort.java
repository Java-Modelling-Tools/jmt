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

package dataAnalysis.sorting;

/**

 * Sorting algorithm for vectors of data.
 * @author Federico Granata
 * @version Date: 24-lug-2003 Time: 14.27.55

 */
public class HeapSort implements SortAlgorithm {

	/**
	 * Sorts data in a data vector & substitutes it.
	 * NOTE: This algorithm treats the array as 1-indexed (ignores index 0).
	 * @param data the array to sort
	 */
	@Override
	public void sort(double[] data) {
		// Need at least elements at index 1 and 2 to sort
		if (data.length < 3) {
			return;
		}

		int left = ((data.length - 1) >> 1) + 1;
		int right = (data.length - 1);
		double temp;

		for (;;) {
			if (left > 1) {
				temp = data[--left];
			} else {
				temp = data[right];
				data[right] = data[1];
				if (--right == 1) {
					data[1] = temp;
					break;
				}
			}

			int parent = left;
			int child = left + left;

			while (child <= right) {
				if (child < right && data[child] < data[child + 1]) {
					child++;
				}
				if (temp < data[child]) {
					data[parent] = data[child];
					parent = child;
					child <<= 1;
				} else {
					break;
				}
			}
			data[parent] = temp;
		}
	}

}
