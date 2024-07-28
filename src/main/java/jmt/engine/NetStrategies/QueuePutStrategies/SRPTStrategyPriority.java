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

import java.util.Iterator;
import java.util.List;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NodeSections.Server;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NodeSection;

/**
 * This class implements a specific preemptive strategy: all arriving jobs
 * are ordered according to their priorities. Jobs with the same priority
 * are put in a Shortest-Remaining-Processing-Time manner.
 * @author Lulai Zhu
 */
public class SRPTStrategyPriority extends QueuePutStrategy implements PreemptiveStrategy {

	public void put(Job job, JobInfoList queue, NodeSection nodeSection) throws NetException {
		int priority = job.getJobClass().getPriority();
		double serviceTime = job.getServiceTime();
		if (serviceTime < 0.0) {
			Server server = (Server) nodeSection.getOwnerNode().getSection(NodeSection.SERVICE);
			ServiceStrategy[] strategies = (ServiceStrategy[]) server.getServiceStrategies();
			serviceTime = strategies[job.getJobClass().getId()].wait(server, job.getJobClass());
			job.setServiceTime(serviceTime);
		}

		List<JobInfo> list = queue.getInternalJobInfoList();
		if (list.size() == 0) {
			queue.addFirst(new JobInfo(job));
			return;
		}

		Job current = null;
		int currentPriority = 0;
		Iterator<JobInfo> it = list.iterator();
		int index = -1;
		while (it.hasNext()) {
			current = it.next().getJob();
			index++;
			currentPriority = current.getJobClass().getPriority();
			if (currentPriority <= priority) {
				break;
			}
		}
		if (currentPriority > priority) {
			queue.addLast(new JobInfo(job));
			return;
		} else if (currentPriority < priority) {
			queue.add(index, new JobInfo(job));
			return;
		}

		double remainingTime = job.getRemainingServiceTime();
		double currentRemainingTime = current.getRemainingServiceTime();
		if (currentRemainingTime > remainingTime) {
			queue.add(index, new JobInfo(job));
			return;
		}
		while (it.hasNext()) {
			current = it.next().getJob();
			index++;
			currentPriority = current.getJobClass().getPriority();
			currentRemainingTime = current.getRemainingServiceTime();
			if (currentPriority < priority || currentRemainingTime > remainingTime) {
				queue.add(index, new JobInfo(job));
				return;
			}
		}
		queue.addLast(new JobInfo(job));
	}

	public int compare(Job job1, Job job2) {
		int priority1 = job1.getJobClass().getPriority();
		int priority2 = job2.getJobClass().getPriority();
		if (priority1 < priority2) {
			return -1;
		} else if (priority1 > priority2) {
			return 1;
		} else {
			double remainingTime1 = job1.getRemainingServiceTime();
			double remainingTime2 = job2.getRemainingServiceTime();
			if (remainingTime1 < remainingTime2) {
				return 1;
			} else if (remainingTime1 > remainingTime2) {
				return -1;
			} else {
				return -1;
			}
		}
	}

}
