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
 * This class implements a specific queue put strategy: all arriving jobs
 * are ordered according to their priorities. Jobs with the same priority
 * are put in a Shortest-Expected-Processing-Time manner.
 * @author Lulai Zhu
 */
public class SEPTStrategyPriority extends QueuePutStrategy {

	public void put(Job job, JobInfoList queue, NodeSection nodeSection) throws NetException {
		int priority = job.getJobClass().getPriority();
		Server server = (Server) nodeSection.getOwnerNode().getSection(NodeSection.SERVICE);
		ServiceStrategy[] strategies = (ServiceStrategy[]) server.getServiceStrategies();
		double serviceMean = strategies[job.getJobClass().getId()].expect(server, job.getJobClass());

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

		double currentServiceMean = strategies[current.getJobClass().getId()].expect(server, current.getJobClass());
		if (currentServiceMean > serviceMean) {
			queue.add(index, new JobInfo(job));
			return;
		}
		while (it.hasNext()) {
			current = it.next().getJob();
			index++;
			currentPriority = current.getJobClass().getPriority();
			currentServiceMean = strategies[current.getJobClass().getId()].expect(server, current.getJobClass());
			if (currentPriority < priority || currentServiceMean > serviceMean) {
				queue.add(index, new JobInfo(job));
				return;
			}
		}
		queue.addLast(new JobInfo(job));
	}

}
