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

package jmt.engine.NetStrategies.QueueGetStrategies;

import jmt.engine.NetStrategies.QueueGetStrategy;
import jmt.engine.QueueNet.*;

import java.util.List;

/**
 * This class implements a LCFS strategy (Last Come First Served).
 * @author Francesco Radaelli
 */
public class LCFSstrategy extends QueueGetStrategy {

	/**
	 * Implements the LCFS strategy.
	 * @param queue Job queue.
	 * @return Last job in the queue.
	 */
	public Job get(JobInfoList queue) {
		return queue.removeLast().getJob();
	}

	@Override
	public Job get(JobInfoList queue, NetNode ownerNode) {
		JobInfo job = ownerNode.getServer().findNextJobToProcessLCFS(queue);

		if(ownerNode.getServer().getNumOfFreeServersForClass(job.getJob().getJobClass().getId()) > 0){
			queue.remove(job);
			return job.getJob();
		}

		return null;
	}

	/**
	 * Implements the LCFS strategy.
	 * @param queue Job queue.
	 * @param jobClass Job class.
	 * @return Last job of specified class in the queue.
	 */
	public Job get(JobInfoList queue, JobClass jobClass) {
		return queue.removeLast(jobClass).getJob();
	}

	public int GetJobClassIdOfNextJob(JobInfoList queue) {
		List<JobInfo> list = queue.getInternalJobInfoList();
		if(list != null && !list.isEmpty()){
			JobInfo jobInfo = list.get(list.size() - 1);
			if(jobInfo != null){
				return jobInfo.getJob().getJobClass().getId();
			}
		}

		return -1;
	}

}