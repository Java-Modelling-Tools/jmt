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

import jmt.engine.QueueNet.*;

/**
 * Implements Exhaustive Polling Get Strategy
 * @author Ahmed Salem
 */
public class ExhaustivePollingGetStrategy extends PollingGetStrategy {

	private int currentQueue;
	private JobClassList jobClassList;
	private JobClass jobClass;

	public ExhaustivePollingGetStrategy() {
		currentQueue = 0;
	}

	public void setPollingQueues(JobClassList jobClassList) {
		this.jobClassList = jobClassList;
		jobClass = jobClassList.get(0);
	}

	public Job get(JobInfoList queue) {
		if (queue.size(jobClass) > 0) {
			Job job = queue.removeFirst(jobClass).getJob();
			return job;
		}

		currentQueue = (currentQueue + 1) % jobClassList.size();
		jobClass = jobClassList.get(currentQueue);
		return null;
	}

	@Override
	public Job get(JobInfoList queue, NetNode ownerNode) {
		return null;
	}

	public Job get(JobInfoList queue, JobClass jobClass) {
		return queue.removeFirst(jobClass).getJob();
	}

	public int GetJobClassIdOfNextJob(JobInfoList queue) {
		return jobClass.getId();
	}

}