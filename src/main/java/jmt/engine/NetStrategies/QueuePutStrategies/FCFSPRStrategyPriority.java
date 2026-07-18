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

package jmt.engine.NetStrategies.QueuePutStrategies;

import jmt.engine.QueueNet.Job;

/**
 * This class implements a specific preemptive strategy: all arriving jobs
 * are ordered according to their priorities. Jobs with the same priority
 * are put in a First-Come-First-Served manner.
 * @author Lulai Zhu
 */
public class FCFSPRStrategyPriority extends TailStrategyPriority implements PreemptiveStrategy {

	public int compare(Job job1, Job job2) {
		int priority1 = job1.getJobClass().getPriority();
		int priority2 = job2.getJobClass().getPriority();
		if (priority1 < priority2) {
			return -1;
		} else if (priority1 > priority2) {
			return 1;
		} else {
			return -1;
		}
	}

}
