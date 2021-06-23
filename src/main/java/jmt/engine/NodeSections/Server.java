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

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.QueueNet.*;
import jmt.engine.simEngine.RemoveToken;

import javax.swing.*;

/**
 * This class implements a multi-class, single/multi server service.
 * Every class has a specific distribution and a own set of statistical
 * parameters.
 * A server service remains busy while processing one or more jobs.
 * @author  Francesco Radaelli, Stefano Omini, Bertoli Marco
 */
public class Server extends ServiceSection {

	private int numberOfServers;
	//TODO: use this to correct residence times (R=r*v) or remove them!!
	private int[] numberOfVisitsPerClass;
	private ServiceStrategy[] serviceStrategies;

	private boolean preemption;
	private Job jobInService = new Job(null, null);

	private int busyCounter;

	public Server(Integer numberOfServers, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies) {
		this(numberOfServers, false, numberOfVisitsPerClass, serviceStrategies);	
	}

	/**
	 * Creates a new instance of Server.
	 * @param numberOfServers Number of jobs which can be served simultaneously.
	 * @param numberOfVisitsPerClass Number of job visits per class: if null
	 * the server will be single visit.
	 * @param serviceStrategies Array of service strategies, one per class.
	 */
	public Server(Integer numberOfServers, Boolean serverPreemptive, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies) {
		if (numberOfVisitsPerClass == null) {
			this.numberOfServers = numberOfServers.intValue();
			this.numberOfVisitsPerClass = null;
			this.serviceStrategies = serviceStrategies;
			busyCounter = 0;
		} else {
			this.numberOfServers = numberOfServers.intValue();
			this.numberOfVisitsPerClass = new int[numberOfVisitsPerClass.length];
			for (int i = 0; i < numberOfVisitsPerClass.length; i++) {
				this.numberOfVisitsPerClass[i] = numberOfVisitsPerClass[i].intValue();
			}
			this.serviceStrategies = serviceStrategies;
			busyCounter = 0;
		}
		this.preemption = serverPreemptive.booleanValue();
	}

	@Override
	public double getDoubleSectionProperty(int id) throws NetException {
		switch (id) {
		case PROPERTY_ID_UTILIZATION:
			return jobsList.size() / numberOfServers;
		case PROPERTY_ID_AVERAGE_UTILIZATION:
			return (getTime() <= 0.0) ? 0.0
					: jobsList.getTotalSojournTime() / getTime() / numberOfServers;
		default:
			return super.getDoubleSectionProperty(id);
		}
	}

	@Override
	public double getDoubleSectionProperty(int id, JobClass jobClass) throws NetException {
		switch (id) {
		case PROPERTY_ID_UTILIZATION:
			return jobsList.size(jobClass) / numberOfServers;
		case PROPERTY_ID_AVERAGE_UTILIZATION:
			return (getTime() <= 0.0) ? 0.0
					: jobsList.getTotalSojournTimePerClass(jobClass) / getTime() / numberOfServers;
		default:
			return super.getDoubleSectionProperty(id, jobClass);
		}
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		jobsList.setNumberOfServers(numberOfServers);
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
			break;

		case NetEvent.EVENT_JOB:

			//EVENT_JOB
			//If the message has been sent by the server itself,
			// then the job is forwarded.
			//
			//If the message has been sent by another section, the server, if
			//is not completely busy, sends to itself a message containing the
			//job and with delay equal to the service time calculated using
			//the service strategy.
			//The counter of jobs in service is increased and, if further service
			//capacity is left, an ack is sent to the input section.

			// Gets the job from the message
			Job job = message.getJob();

			if (isMine(message)) {
				// this job has been just served (the message has been sent by the server itself)
				// forwards the job to the output section
				job.setIsJobInService(false);
				job.setServiceTime(-1);
				sendForward(job, 0.0);
			} else {
				//message received from another node section: if the server is not completely busy,
				//it sends itself a message with this job
				if (preemption) {
					if (jobInService.getIsJobInService()) {
						double timeElapsed = this.getTime() - jobInService.getServiceArrivalTime();
						jobInService.setServiceTime(jobInService.getServiceTime() - timeElapsed);
						jobInService.setIsPreemptedJob(true);
						removeMessage(jobInService.getServingMessage());
						jobInService.setIsJobInService(false);
						sendBackward(NetEvent.EVENT_PREEMPTED_JOB, jobInService, 0.0);
						busyCounter--;
						// this is for retrial debugging
						//this.getOwnerNode().getJobInfoList().updateNumberOfBusyServersVar(message.getJob(), busyCounter);
					}
				}
				if (busyCounter < numberOfServers) {
					// Auto-sends the job with delay equal to "serviceTime"
					//message received from another node section: if the server is not completely busy,
					//it sends itself a message with this job
					RemoveToken jobInServiceMessage;
					if (!job.getIsPreemptedJob()) {
						double serviceTime = job.getServiceTime();
						if (serviceTime < 0.0) {
							serviceTime = serviceStrategies[job.getJobClass().getId()].wait(this, job.getJobClass());
							job.setServiceTime(serviceTime);
						} else {
							job.setServiceTime(-1.0);
						}
						job.setServiceArrivalTime(this.getTime());
						jobInServiceMessage = sendMe(job, serviceTime);
					} else {
						job.setIsPreemptedJob(false);
						job.setServiceArrivalTime(this.getTime());
						jobInServiceMessage = sendMe(job, job.getServiceTime());
					}
					if (preemption) {
						jobInService = job;
						jobInService.setServingMessage(jobInServiceMessage);
						jobInService.setIsJobInService(true);
					}

					busyCounter++;
					// this is for retrial debugging
					//this.getOwnerNode().getJobInfoList().updateNumberOfBusyServersVar(job, busyCounter);
					this.getOwnerNode().getJobInfoList().serveJob(job);
					if (busyCounter < numberOfServers) {
						// Sends an ACK to the input section (remember not to propagate
						// this ack again when computation is finished)
						sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
					}
				} else {
					//server is busy
					return MSG_NOT_PROCESSED;
				}
			}
			break;

		case NetEvent.EVENT_ACK:

			//EVENT_ACK
			//If there are no jobs in the service section, message is not processed.
			//Otherwise an ack is sent backward to the input section and
			//the counter of jobs in service is decreased.

			if (busyCounter == 0) {
				//it was not waiting for any job
				return MSG_NOT_PROCESSED;
			} else if (busyCounter == numberOfServers) {
				// Sends a request to the input section
				sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
				busyCounter--;
			} else {
				// Avoid ACK as we already sent ack
				busyCounter--;
			}
			// this is for retrial debugging
			//this.getOwnerNode().getJobInfoList().updateNumberOfBusyServersVar(message.getJob(), busyCounter);
			break;

		case NetEvent.EVENT_STOP:
			break;

			case NetEvent.EVENT_RENEGE:
				break;
		default:
			return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	/**
	 * Gets the service strategy for each class.
	 * @return the service strategy for each class.
	 */
	public ServiceStrategy[] getServiceStrategies() {
		return serviceStrategies;
	}
	
	private double getTime(){
		return this.getOwnerNode().getNetSystem().getTime();
	}

}
