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

package jmt.engine.NetStrategies;

import jmt.common.AutoCheck;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfoList;

/**
 * Use this class to implement a specific PS strategy. A PS strategy is a
 * rule which shares limited service capacity among all present jobs.
 * @author Lulai Zhu
 */
public abstract class PSStrategy implements AutoCheck {

	/**
	 * This method should be overridden to implement a specific PS strategy.
	 * @param list Job info list.
	 * @param classes Job class list.
	 * @param weights Service weights.
	 * @param saturated Service saturated.
	 * @param jobClass Job class.
	 * @return Service fraction received by a job of the given class.
	 */
	public abstract double slice(JobInfoList list, JobClassList classes, double[] weights, boolean[] saturated, JobClass jobClass);

	public boolean check() {
		return true;
	}

}
