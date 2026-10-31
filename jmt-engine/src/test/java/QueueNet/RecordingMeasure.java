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

package QueueNet;

import java.util.ArrayList;
import java.util.List;

import dataAnalysis.Measure;

/** Test measure that keeps every (sample, weight) it is given instead of analyzing it. */
public class RecordingMeasure extends Measure {
	public final List<Double> samples = new ArrayList<Double>();
	public final List<Double> weights = new ArrayList<Double>();

	public RecordingMeasure() {
		super("rec", 0.05, 0.05, 10000, false, null);
	}

	@Override
	public synchronized boolean update(double sample, double weight, String nodeName, int numOfVisits) {
		samples.add(sample);
		weights.add(weight);
		return false;
	}
}
