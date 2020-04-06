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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jmt.common.exception.NetException;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.WaitingRequest;
import jmt.engine.random.engine.RandomEngine;

/**
 * This class implements a terminal: each class has a finite number of customers in
 * the system.
 * @author  Stefano Omini
 */
public class Terminal extends InputSection {

	private boolean coolStart; //when is true the waitingjobs queue is void

	private List<WaitingRequest> waitingRequests;

	private int[] jobsPerClass; //number of jobs per class

	private JobClassList jobClasses;

	/**
	 * Creates a terminal.
	 * @param jobsPerClass number of jobs for each class.
	 */
	public Terminal(int[] jobsPerClass) {
		coolStart = true;
		this.jobsPerClass = new int[jobsPerClass.length];
		for (int c = 0; c < this.jobsPerClass.length; c++) {
			this.jobsPerClass[c] = jobsPerClass[c];
		}
	}

	/**
	 * Creates a terminal.
	 * @param jobsPerClass number of jobs for each class.
	 */
	public Terminal(Integer[] jobsPerClass) {
		coolStart = true;
		this.jobsPerClass = new int[jobsPerClass.length];
		for (int c = 0; c < this.jobsPerClass.length; c++) {
			this.jobsPerClass[c] = jobsPerClass[c].intValue();
		}
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		// Sets netnode dependent properties
		jobClasses = getJobClasses();
		waitingRequests = new LinkedList<WaitingRequest>();
        //waitingRequests.setNetSystem(node.getNetSystem()); 
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Job job;

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:

			//case EVENT_START:
			//the terminal creates all the jobs requested by each class.
			//for each job created, it sends to itself a message with delay 0

			ListIterator<JobClass> it = jobClasses.listIterator();

			//generator of random numbers (uses the same engine of
			//distributions and strategies) used to mix the order of
			//leaving jobs, otherwise they leave in order of class
			//(i.e. c1, c1, c1, ...c2, c2, c2, ... c3....)
			RandomEngine randomEng = this.getNetSystem().getEngine();

			//delay used to mix leaving order
			double mixRandomDelay = 0.0;

			//global jobInfoList used to create jobs
			GlobalJobInfoList netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
			while (it.hasNext()) {
				JobClass jobClass = it.next();

				if (jobClass.getType() == JobClass.OPEN_CLASS) {
					//open class: no jobs to be created
					continue;
				}

				int c = jobClass.getId();

				if (jobsPerClass != null) {
					//terminal of a closed system
					//creates all the jobs
					for (int i = 0; i < jobsPerClass[c]; i++) {
						//note that if jobsPerClass[c] = -1 (open class) the instructions
						//of this for are not performed

						//each job is created and sent to the terminal itself
						job = new Job(jobClass, netJobsList);
                        job.initialize(getNetSystem()); 
						updateVisitPath(job);
						netJobsList.addJob(job);

						//sets a random (very small) delay to mix the jobs
						mixRandomDelay = (randomEng.raw()) * 0.00001;

						sendMe(job, mixRandomDelay);
					}
				}
			}
			break;

		case NetEvent.EVENT_JOB:

			//case EVENT_JOB
			//if coolStart=false adds the job to the list of waiting jobs.
			//
			//if coolStart=true (no waiting jobs) checks the source
			//If the message has been received from the terminal itself, the job's
			//bornTime is set, then the job is forwarded. Otherwise, if it has been
			//received from the outside, an ack message is sent to the source of the
			//message, the job's bornTime is set, then the job is forwarded.
			//

			// Gets the job from the message
			job = message.getJob();

			if (coolStart) {
				//the queue of waiting jobs is empty

				if (message.getSource() == getOwnerNode() && message.getSourceSection() == getSectionID()) {
					//message sent by the terminal itself
					sendForward(job, 0.0);

					coolStart = false;
				} else {
					//job received from the outside

					//send an ack
					send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());

					//job goes on
					sendForward(job, 0.0);

					coolStart = false;
				}
			} else {
				//coolStart is false: there are waiting jobs. Add the received job.
				waitingRequests.add(new WaitingRequest(job, message.getSourceSection(), message.getSource()));
			}
			break;

		case NetEvent.EVENT_ACK:

			//case EVENT_ACK:
			//if there are waiting jobs, takes the first, set its bornTime and
			//forwards it to the service section.
			//then it creates a new job and sends to itself a message whose delay is the time of
			//departure of that job.
			//otherwise, if there are no waiting jobs, sets coolstart=true

			if (waitingRequests.size() > 0) {
				WaitingRequest wr = waitingRequests.remove(0);

				if (!isMyOwnerNode(wr.getSource())) {
					send(NetEvent.EVENT_ACK, wr.getJob(), 0.0, wr.getSourceSection(), wr.getSource());
				}

				Job jobSent = wr.getJob();
				sendForward(jobSent, 0.0);
			} else {
				coolStart = true;
			}
			break;

		case NetEvent.EVENT_STOP:
			break;

		case NetEvent.EVENT_JOB_RELEASE:
			break;

		case NetEvent.EVENT_JOB_FINISH:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}
	
	private NetSystem getNetSystem() {
		return this.getOwnerNode().getNetSystem();
	}


}
