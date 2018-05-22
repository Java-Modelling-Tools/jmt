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
import jmt.common.exception.NetException;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NodeSection;

/**
 * Use this class to implement a specific queue put strategy. A queue put
 * strategy is a rule which adds a job to a queue.
 * @author Francesco Radaelli
 */
public abstract class QueuePutStrategy implements AutoCheck {

	/**
	 * This method should be overridden to implement a specific job strategy.
	 * @param job Job to be added to the queue.
	 * @param queue Job queue.
	 * @param nodeSection Node section which applies this strategy.
	 * @throws NetException
	 */
	public abstract void put(Job job, JobInfoList queue, NodeSection nodeSection) throws NetException;

	public boolean check() {
		return true;
	}

}
