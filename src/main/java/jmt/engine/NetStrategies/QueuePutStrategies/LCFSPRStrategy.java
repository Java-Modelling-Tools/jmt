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
 * are put to the queue in a Last-Come-First-Served manner.
 * @author Lulai Zhu
 */
public class LCFSPRStrategy extends HeadStrategy implements PreemptiveStrategy {

	public int compare(Job job1, Job job2) {
		return 1;
	}

}
