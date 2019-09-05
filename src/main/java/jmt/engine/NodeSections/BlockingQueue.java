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

package jmt.engine.NodeSections;

import java.util.ListIterator;

import jmt.common.exception.NetException;
import jmt.engine.QueueNet.BlockingRegion;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.SimConstants;
import jmt.engine.dataAnalysis.Measure;

/**
 * This class implements the queue of a region input station, that is
 * the node which controls the access to a blocking region.
 *
 * @author Stefano Omini
 *
 * Modifief by Bertoli Marco 31-03-2006 (my birthday!!!) 
 * 
 * Modified by Ashanka (Oct 2009):
 * Desc: FCR(Finite Capacity Region) is a blocking region and it was measuring wrong 
 * 		 values for the Performance Indices. This was because the Jobs in FCR were not
 * 		 considering the time spent in the Queueing Stations in the FCR and simply reporting
 *       time spent in Queueing in FCR. This behavior is incorrect as Performance Indices for 
 *       FCR should also contain the time spent in the FCR and not only the time spent queueing.
 *       
 *       Approach taken presently to rectify is to delay the capture of the performance indices 
 *       until the job is out of the FCR. So the job dropping are manually handled for Blocking region
 *       at Node level.
 */
public class BlockingQueue extends InputSection {

	private BlockingRegion blockingRegion;

	/** For each class, true if jobs in excess must be dropped */
	protected boolean[] classDrop;

	/** For each class, true if jobs in excess must be dropped */
	protected double[] classWeights;

	/** job info list of the owner node: used to remove job after drop */
	private JobInfoList jobsList_node;

	/**
	 * Creates a new instance of infinite BlockingQueue.
	 */
	public BlockingQueue(BlockingRegion myRegion) {
		super(false);
		//sets the blocking region owner of this blocking queue
		this.blockingRegion = myRegion;
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		// Sets netnode dependent properties
		jobsList_node = node.getJobInfoList();

		//copies drop properties from blocking region
		//and initializes dropped jobs
		classDrop = new boolean[getJobClasses().size()];
		for (int i = 0; i < classDrop.length; i++) {
			classDrop[i] = blockingRegion.getClassDrop(i);
		}
	}

	/**
	 * This method implements a blocking queue.
	 * @param message message to be processed.
	 * @throws NetException
	 */
	@Override
	protected int process(NetMessage message) throws NetException {
		Job job;
		JobClass jobClass;

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
			break;

		case NetEvent.EVENT_JOB:

			//EVENT_JOB
			job = message.getJob();

			//no ack must be sent to the source of message (the redirecting queue of a node
			//inside the region)
			jobClass = job.getJobClass();

			//this job has been received by an internal node
			//the region input station will have to send it back
			NetNode realDestination = message.getSource();

			//adds a JobInfo object after modifying the job with redirection informations
			job.setOriginalDestinationNode(realDestination);

			//checks whether the region is blocked for this class
			if (blockingRegion.isBlocked(jobClass)) {
				//the region is already blocked
				//the job must be dropped?
				if (classDrop[jobClass.getId()]) {
					//drop job
					drop(job);
				} else {
					//otherwise the job remains blocked in queue
					jobsList.add(new JobInfo(job));
				}
				break;
			}

			//the region is not blocked for this class
			//adds the job to update the job info list
			jobsList.add(new JobInfo(job));
			//the job is sent to service section
			send(job);
			blockingRegion.increaseOccupation(jobClass);
			break;

		case NetEvent.EVENT_ACK:
			break;

		case NetEvent.EVENT_JOB_OUT_OF_REGION:

			//FCR Bug fix:
			//The dropping of the job was postponed as the time 
			//spent in the Queueing station was not taken in consideration.
			//Secondly point to be noted is that I am 
			//only dropping Jobs at Node level as the node section jobs 
			//are automatically dropped but the node level jobs are
			//dropped manually.
			job = (Job) message.getData();
			JobInfo jobInfo_node = jobsList_node.lookFor(job);
			if (jobInfo_node != null) {
				jobsList_node.remove(jobInfo_node);
			}

			//checks whether there are jobs in queue
			if (jobsList.size() <= 0) {
				//no jobs in queue
				break;
			}

			//search for the next job which is not blocked (if exists)
			//and forward it
			job = getNextNotBlockedJob();

			if (job != null) {
				jobClass = job.getJobClass();
				send(job);
				blockingRegion.increaseOccupation(jobClass);
			} else {
				//all jobs in queue are blocked
				//nothing else to do
			}
			break;

		case NetEvent.EVENT_STOP:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	/**
	 * Gets, if exists, the next job in queue that is not class blocked and has an higher priority.
	 * @return the next job in queue that is not class blocked; null otherwise.
	 */
	private Job getNextNotBlockedJob() {
		ListIterator<JobInfo> it = jobsList.getInternalJobInfoList().listIterator();
		Job nextJob = null;
		int nextJobPriority = 0;
		while (it.hasNext()) {
			JobInfo jobInfo = it.next();
			JobClass jobClass = jobInfo.getJob().getJobClass();
			if (nextJob == null || nextJobPriority < jobClass.getPriority()) {
				if (!blockingRegion.isBlocked(jobClass)) {
					nextJob = jobInfo.getJob();
					nextJobPriority = jobClass.getPriority();
				}
			}
		}
		return nextJob;
	}

	/**
	 * Send the specified job, removing it from the job info lists.
	 * @param job the job to be sent.
	 */
	private void send(Job job) {
		JobInfo jobInfo = jobsList.lookFor(job);
		if (jobInfo != null) {
			//removes job from the section jobInfoList
			jobsList.remove(jobInfo);
		}
		sendForward(job, 0.0);
	}

	/**
	 * Drop the specified job, removing it from the job info lists.
	 * @param job the job to be dropped.
	 */
	private void drop(Job job) {
		JobInfo jobInfo_node = jobsList_node.lookFor(job);
		if (jobInfo_node != null) {
			//drops job from the node jobInfoList
			jobsList_node.dropJob(jobInfo_node);
		}

		if (!(job instanceof ForkJob)) {
			//drops job from the global jobInfoList
			getOwnerNode().getQueueNet().getJobInfoList().dropJob(job);
		}
	}

	@Override
	public void analyzeFCR(int name, Measure measurement) throws NetException {
		switch (name) {
		case SimConstants.FCR_TOTAL_WEIGHT:
			blockingRegion.setWeightMeasure(measurement);
			break;
		case SimConstants.FCR_MEMORY_OCCUPATION:
			blockingRegion.setSizeMeasure(measurement);
			break;
		default:
			throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
		}
	}

}
