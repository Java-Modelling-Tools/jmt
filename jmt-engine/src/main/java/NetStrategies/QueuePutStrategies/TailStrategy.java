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

package NetStrategies.QueuePutStrategies;

import jmt.common.exception.NetException;
import NetStrategies.QueuePutStrategy;
import QueueNet.Job;
import QueueNet.JobInfo;
import QueueNet.JobInfoList;
import QueueNet.NodeSection;

/**
 * This class implements a specific queue put strategy: all arriving jobs
 * are put at the tail of the queue.
 * @author Francesco Radaelli
 */
public class TailStrategy extends QueuePutStrategy {

	public void put(Job job, JobInfoList queue, NodeSection nodeSection) throws NetException {
		queue.addLast(new JobInfo(job));
	}

}
