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

package jmt.engine.NetStrategies.PSStrategies;

import jmt.engine.NetStrategies.PSStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfoList;

import java.util.Arrays;

/**
 * This class implements a specific PS strategy: Generalised Processor-
 * Sharing. The service capacity is shared across all nonempty classes in
 * accordance with the respective class-dependent weights.
 * @author Lulai Zhu
 */
public class GPSStrategy extends PSStrategy {

	public double slice(JobInfoList list, JobClassList classes, double[] weights, boolean[] saturated, JobClass jobClass) {
		Boolean[] compatibilities = new Boolean[classes.size()];
		Arrays.fill(compatibilities, true);
		double[] splits = new double[classes.size()];
		Arrays.fill(splits, 1);
		return slice(list, classes, weights, saturated, jobClass, compatibilities, splits);	}

	public double slice(JobInfoList list, JobClassList classes, double[] weights, boolean[] saturated, JobClass jobClass, Boolean[] compatibilities, double[] splits) {
		if (list.size(jobClass) <= 0 || !compatibilities[jobClass.getId()]) {
			return 0.0;
		}
		double total = 0.0;
		for (int i = 0; i < classes.size(); i++) {
			if (list.size(classes.get(i)) > 0 && !saturated[i] && splits[i] > 0) {
				total += weights[i];
			}
		}
		return weights[jobClass.getId()] / total / (list.size(jobClass) * splits[jobClass.getId()]);
	}

}
