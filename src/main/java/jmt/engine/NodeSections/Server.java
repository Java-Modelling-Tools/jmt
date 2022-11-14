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

import java.util.HashMap;
import java.util.PriorityQueue;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.PreemptiveStrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.simEngine.RemoveToken;

/**
 * This class implements a multi-class, single/multi server service.
 * Every class has a specific distribution and a own set of statistical
 * parameters.
 * A server service remains busy while processing one or more jobs.
 * @author  Francesco Radaelli, Stefano Omini, Bertoli Marco
 */
public class Server extends ServiceSection {

	private int numberOfServers;
	private int maxRunningJobs;
	private QueuePutStrategy[] putStrategies;
	private ServiceStrategy[][] setupStrategies;
	private ServiceStrategy[] serviceStrategies;

	private int busyCounter;
	private HashMap<Job, ServerUnit> busyServerMap;
	private PriorityQueue<ServerUnit> idleServerQueue;

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies) {
		this(numberOfServers, numberOfVisits, serviceStrategies, null, null);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
			QueuePutStrategy[] preemptiveStrategies) {
		this(numberOfServers, numberOfVisits, serviceStrategies, preemptiveStrategies, null);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
			Object[] setupStrategies) {
		this(numberOfServers, numberOfVisits, serviceStrategies, null, setupStrategies);
	}

	/**
	 * Creates a new instance of Server.
	 * @param numberOfServers Number of jobs which can be served simultaneously.
	 * @param numberOfVisits Number of job visits per class (not used).
	 * @param serviceStrategies Array of service strategies, one per class.
	 * @param preemptiveStrategies Array of preemptive strategies, one per class.
	 * @param setupStrategies Matrix of setup strategies, one per class pair.
	 */
	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
			QueuePutStrategy[] preemptiveStrategies, Object[] setupStrategies) {
		super(false);
		this.numberOfServers = numberOfServers.intValue();
		this.serviceStrategies = serviceStrategies;
		if (preemptiveStrategies == null) {
			this.putStrategies = new QueuePutStrategy[serviceStrategies.length];
			for (int i = 0; i < serviceStrategies.length; i++) {
				this.putStrategies[i] = new TailStrategy();
			}
		} else {
			this.putStrategies = preemptiveStrategies;
		}
		if (setupStrategies == null) {
			this.setupStrategies = null;
		} else {
			this.setupStrategies = new ServiceStrategy[setupStrategies.length][];
			for (int i = 0; i < setupStrategies.length; i++) {
				this.setupStrategies[i] = (ServiceStrategy[]) setupStrategies[i];
			}
		}
		busyCounter = 0;
	}

	@Override
	public double getDoubleSectionProperty(int id) throws NetException {
		NetSystem netSystem = getOwnerNode().getNetSystem();
		switch (id) {
		case PROPERTY_ID_UTILIZATION:
			return jobsList.size() / numberOfServers;
		case PROPERTY_ID_AVERAGE_UTILIZATION:
			return (netSystem.getTime() <= 0.0) ? 0.0
					: jobsList.getTotalSojournTime() / netSystem.getTime() / numberOfServers;
		default:
			return super.getDoubleSectionProperty(id);
		}
	}

	@Override
	public double getDoubleSectionProperty(int id, JobClass jobClass) throws NetException {
		NetSystem netSystem = getOwnerNode().getNetSystem();
		switch (id) {
		case PROPERTY_ID_UTILIZATION:
			return jobsList.size(jobClass) / numberOfServers;
		case PROPERTY_ID_AVERAGE_UTILIZATION:
			return (netSystem.getTime() <= 0.0) ? 0.0
					: jobsList.getTotalSojournTimePerClass(jobClass) / netSystem.getTime() / numberOfServers;
		default:
			return super.getDoubleSectionProperty(id, jobClass);
		}
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		maxRunningJobs = numberOfServers;
		int stationCapacity = ((Queue) getOwnerNode().getSection(NodeSection.INPUT)).getStationCapacity();
		if (stationCapacity > 0) {
			maxRunningJobs = Math.min(maxRunningJobs, stationCapacity);
		}
		jobsList.setNumberOfServers(numberOfServers);
		if (setupStrategies != null) {
			busyServerMap = new HashMap<Job, ServerUnit>();
			idleServerQueue = new PriorityQueue<ServerUnit>();
		}
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Job job;
		JobInfo jobInfo;

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
			break;

		case NetEvent.EVENT_JOB:

			//EVENT_JOB
			//If the message has been sent by the server itself,
			//then the job is forwarded.
			//
			//If the message has been sent by another section, the server, if
			//is not completely busy, sends to itself a message containing the
			//job and with delay equal to the service time calculated using
			//the service strategy.
			//The counter of jobs in service is increased and, if further service
			//capacity is left, an ack is sent to the input section.

			//gets the job from the message
			job = message.getJob();

			if (isMine(message)) {
				//this job has been just served (the message has been sent by the server itself)
				//forwards the job to the output section
				jobInfo = jobsList.lookFor(job);
				job.setServiceTime(-1.0);
				job.setServiceStartTime(-1.0);
				job.setServingMessage(null);
				jobsList.endJob(jobInfo);
				jobsList.remove(jobInfo);
				if (setupStrategies != null) {
					ServerUnit serverUnit = busyServerMap.remove(job);
					idleServerQueue.offer(serverUnit);
				}
				sendForward(job, 0.0);
			} else {
				//message received from another node section: if the server is not completely busy,
				//it sends itself a message with this job
				int jobClassID = job.getJobClass().getId();
				if (putStrategies[jobClassID] instanceof PreemptiveStrategy && busyCounter == maxRunningJobs) {
					JobInfo lastJobInfo = getLastJobInfo();
					Job lastJob = lastJobInfo.getJob();
					RemoveToken lastJobServingMessage = lastJob.getServingMessage();
					removeMessage(lastJobServingMessage);
					lastJob.setServingMessage(null);
					double lastJobStartTime = lastJob.getServiceStartTime();
					if (lastJobStartTime >= 0.0) {
						double lastJobRemainingTime = job.getRemainingServiceTime();
						lastJob.setServiceTime(lastJobRemainingTime);
						lastJob.setServiceStartTime(-1.0);
						jobsList.endJob(lastJobInfo);
					}
					jobsList.remove(lastJobInfo);
					if (setupStrategies != null) {
						ServerUnit serverUnit = busyServerMap.remove(lastJob);
						idleServerQueue.add(serverUnit);
					}
					sendBackward(NetEvent.EVENT_PREEMPTED_JOB, lastJob, 0.0);
					busyCounter--;
				}

				if (busyCounter >= maxRunningJobs) {
					return MSG_NOT_PROCESSED;
				}
				putStrategies[jobClassID].put(job, jobsList, this);
				jobInfo = jobsList.lookFor(job);

				//sends the job with delay equal to "serviceTime"
				//message received from another node section: if the server is not completely busy,
				//it sends itself a message with this job
				double serviceTime = job.getServiceTime();
				if (serviceTime < 0.0) {
					serviceTime = serviceStrategies[jobClassID].wait(this, job.getJobClass());
					job.setServiceTime(serviceTime);
				}
				NetSystem netSystem = getOwnerNode().getNetSystem();
				if (setupStrategies != null) {
					ServerUnit serverUnit = idleServerQueue.poll();
					if (serverUnit == null) {
						serverUnit = new ServerUnit(busyCounter, jobClassID);
					}
					if (serverUnit.jobClassID == jobClassID) {
						RemoveToken jobServingMessage = sendMe(NetEvent.EVENT_JOB, job, serviceTime);
						job.setServiceStartTime(netSystem.getTime());
						job.setServingMessage(jobServingMessage);
						jobsList.startJob(jobInfo);
					} else {
						ServiceStrategy setupTimeStrategy = setupStrategies[serverUnit.jobClassID][jobClassID];
						double setupTime = setupTimeStrategy.wait(this, job.getJobClass());
						RemoveToken jobServingMessage = sendMe(NetEvent.EVENT_SETUP_JOB, job, setupTime);
						job.setServingMessage(jobServingMessage);
					}
					busyServerMap.put(job, serverUnit);
				} else {
					RemoveToken jobServingMessage = sendMe(NetEvent.EVENT_JOB, job, serviceTime);
					job.setServiceStartTime(netSystem.getTime());
					job.setServingMessage(jobServingMessage);
					jobsList.startJob(jobInfo);
				}

				busyCounter++;
				if (busyCounter < maxRunningJobs) {
					//sends an ack to the input section (remember not to propagate
					//this ack again when computation is finished)
					sendBackward(NetEvent.EVENT_ACK, job, 0.0);
				}
			}
			break;

		case NetEvent.EVENT_SETUP_JOB:
			job = message.getJob();
			jobInfo = jobsList.lookFor(job);
			ServerUnit serverUnit = busyServerMap.get(job);
			int jobClassID = job.getJobClass().getId();
			serverUnit.jobClassID = jobClassID;
			NetSystem netSystem = getOwnerNode().getNetSystem();
			double serviceTime = job.getServiceTime();
			RemoveToken jobServingMessage = sendMe(NetEvent.EVENT_JOB, job, serviceTime);
			job.setServiceStartTime(netSystem.getTime());
			job.setServingMessage(jobServingMessage);
			jobsList.startJob(jobInfo);
			break;

		case NetEvent.EVENT_ACK:

			//EVENT_ACK
			//If there are no jobs in the service section, message is not processed.
			//Otherwise an ack is sent backward to the input section and
			//the counter of jobs in service is decreased.

			if (busyCounter <= 0) {
				return MSG_NOT_PROCESSED;
			}
			if (busyCounter >= maxRunningJobs) {
				sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
			}
			busyCounter--;
			break;

		case NetEvent.EVENT_STOP:
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

	/**
	 * Gets the last job info in the service list.
	 * @return the last job info in the service list.
	 */
	public JobInfo getLastJobInfo() {
		return jobsList.getInternalJobInfoList().get(jobsList.size() - 1);
	}

	private static class ServerUnit implements Comparable<ServerUnit> {

		public int id;
		public int jobClassID;

		public ServerUnit(int id, int jobClassID) {
			this.id = id;
			this.jobClassID = jobClassID;
		}

		@Override
		public int compareTo(ServerUnit that) {
			if (this.id < that.id) {
				return -1;
			} else if (this.id > that.id) {
				return 1;
			} else {
				return 0;
			}
		}

	}

}
