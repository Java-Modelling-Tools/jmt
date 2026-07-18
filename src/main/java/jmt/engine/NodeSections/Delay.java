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

import java.util.*;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NetStrategies.TransitionUtilities.TimingPacket;
import jmt.engine.QueueNet.*;
import jmt.engine.random.engine.RandomEngine;
import jmt.engine.simEngine.RemoveToken;

/**
 * This class implements a multi-class delay service. Every class has a
 * specific distribution and a own set of statistical parameters. The delay
 * service processes a job without being busy.
 * @author Francesco Radaelli
 */
public class Delay extends ServiceSection {

	private LinkedList<Job> waitingJobs;

	private boolean coolStart;

	private ServiceStrategy[] serviceStrategies;

	private LinkedList<Job> jobsToFire;

	/**
	 * Creates a new instance of Delay. Use this constructor to create a delay
	 * service for a open/closed network.
	 * @param serviceStrategies Array of service strategies, one per class.
	 */
	public Delay(ServiceStrategy[] serviceStrategies) {
		this.serviceStrategies = serviceStrategies;
		waitingJobs = new LinkedList<Job>();
		coolStart = true;
		jobsToFire = new LinkedList<Job>();
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Job job;
		Object data = message.getData();
		NetSystem netSystem = getOwnerNode().getNetSystem();
		RandomEngine randomEngine = getOwnerNode().getNetSystem().getEngine();
		boolean workingAsTransition = ((Queue)getOwnerNode().getSection(NodeSection.INPUT)).isStationWorkingAsATransition();

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
			break;

		case NetEvent.EVENT_JOB:

			//EVENT_JOB
			//If the message has been sent by this section, it means that the job
			//has been already delayed. Therefore, if there are no waiting jobs (coolStart true)
			//the job is forwarded and coolStart becomes false, otherwise the job is added to
			//the existing waiting jobs.
			//
			//If the message has been sent by the input section, delay section sends to
			//itself a message with the job and with a delay calculated using the
			//service strategy; then an ack is sent to the message source.

			// Gets the job from the message
			job = message.getJob();
			// If the message source is this section, the job has been
			// delayed and it should be forwarded to the next section
			if (isMine(message)) {
				if (coolStart) {
					if(workingAsTransition) {
						jobsToFire.add(job);
						sendTimingMessage(job);
					}else {
						// Sends job
						sendForward(job, 0.0);
						coolStart = false;
					}
				} else {
					waitingJobs.add(job);
				}
			}
			// else delays the job
			else {
				// Gets the class of the job
				int c = job.getJobClass().getId();
				// Calculates the service time of job
				double serviceTime;
				if(workingAsTransition) {
					serviceTime = getJobServingTime(job);
				}else{
					serviceTime = serviceStrategies[c].wait(this, job.getJobClass());
				}
				// Sends to itself the job with delay equal to "serviceTime"
				RemoveToken servingMessage = sendMe(job, serviceTime);
				job.setServiceStartTime(netSystem.getTime());
				job.setServingMessage(servingMessage);
				// Sends backward the job ack
				sendBackward(NetEvent.EVENT_ACK, job, 0.0);
			}
			break;

		case NetEvent.EVENT_ACK:

			//EVENT_ACK
			//If there are waiting jobs, the first is get and forwarded,
			//otherwise coolStart is set to true.

			if (waitingJobs.size() != 0) {
				job = waitingJobs.removeFirst();
				if(workingAsTransition) {
					jobsToFire.add(job);
					sendTimingMessage(job);
				}else{
					sendForward(job, 0.0);
				}
			} else {
				coolStart = true;
			}
			break;

		case NetEvent.EVENT_ENABLING:{
			int enablingDegree = (int) message.getData();
			int numberOfActiveJobs = jobsList.size();

			if (enablingDegree != 0) {
				if(numberOfActiveJobs == 0) {
					sendBackward(NetEvent.EVENT_ACK, null, 0.0);
					coolStart = true;
				}
			}else{
				for(int i = 0; i < numberOfActiveJobs; i++){
					preemptJob();
				}
				for(int i = 0; i < waitingJobs.size(); i++){
					preemptJobFromWaitingList();
				}

				coolStart = false;
			}
			break;
		}

		case NetEvent.EVENT_FIRING:{
			if(jobsToFire.size() > 0) {
				Job j = jobsToFire.removeFirst();
				// Sends job
				sendForward(j, 0.0);
				coolStart = false;
			}
			break;
		}

		case NetEvent.EVENT_STOP:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}


	private void preemptJob() throws NetException{
		JobInfo lastJobInfo = getLastJobInfo();
		Job lastJob = lastJobInfo.getJob();
		RemoveToken lastJobServingMessage = lastJob.getServingMessage();
		removeMessage(lastJobServingMessage);
		lastJob.setServingMessage(null);
		double lastJobStartTime = lastJob.getServiceStartTime();
		if (lastJobStartTime >= 0.0) {
			double lastJobRemainingTime = lastJob.getRemainingServiceTime();
			lastJob.setServiceTime(lastJobRemainingTime);
			lastJob.setServiceStartTime(-1.0);
		}
		jobsList.remove(lastJobInfo);

		sendBackward(NetEvent.EVENT_PREEMPTED_JOB, lastJob, 0.0);
	}

	private void preemptJobFromWaitingList() throws NetException{
		Job job = waitingJobs.removeFirst();
		JobInfo jobInfo = jobsList.lookFor(job);
		job.setServiceTime(0);

		jobsList.remove(jobInfo);
		sendBackward(NetEvent.EVENT_PREEMPTED_JOB, job, 0.0);
	}

	public JobInfo getLastJobInfo() {
		return jobsList.getInternalJobInfoList().get(jobsList.size() - 1);
	}

	private void sendTimingMessage(Job job){
		TimingPacket packet = new TimingPacket(0, 0.0, 1, 1);
		sendBackward(NetEvent.EVENT_TIMING, packet, 0.0);
	}

	private double getJobServingTime(Job job) throws NetException{
		int c = job.getJobClass().getId();
		double serviceTime = job.getServiceTime();
		if (serviceTime < 0.0) {
			serviceTime = serviceStrategies[c].wait(this, job.getJobClass());
			job.setServiceTime(serviceTime);
		}
		return serviceTime;
	}
}
