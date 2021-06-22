/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.engine.NodeSections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import java.util.List;
import java.util.Map;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.PSStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NetStrategies.PSStrategies.EPSStrategy;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.PSJobInfo;
import jmt.engine.QueueNet.PSJobInfoList;
import jmt.engine.simEngine.RemoveToken;

/**
 * <p><b>Name:</b> PSServer</p> 
 * <p><b>Description:</b> 
 * This class implements a multi-class processor sharing server.
 * </p>
 * <p><b>Date:</b> 04/ott/2009
 * <b>Time:</b> 13.45.37</p>
 * @author Bertoli Marco
 * @version 1.0
 */
public class PSServer extends ServiceSection {

	private int numberOfServers;
	private ServiceStrategy[] serviceStrategies;
	private PSStrategy[] psStrategies;
	private double[] serviceWeights;

	/** Tells which inner event we are processing */
	private enum PSEvent {
		JOB_IN, JOB_OUT
	}

	private JobClassList jobClasses;
	private PSJobInfoList psJobsList;
	private double[] serviceFractions;

	private RemoveToken messageToken;
	private PSJobInfo messageJobInfo;
	private double messageSendTime;

	// The owner NetNode
	private NetNode ownerNode;
	// The JobInfoList of the owner NetNode
	private JobInfoList nodeJobsList;
	// The JobInfoList of the global Network
	private GlobalJobInfoList netJobsList;

	// Used to record jobs that will be reneged in each round of process()
	List<Job> jobsToBeReneged = new ArrayList<>();

	public PSServer(Integer numberOfServers, ServiceStrategy[] serviceStrategies) {
		this(numberOfServers, false, serviceStrategies);
	}

	/**
	 * Creates a new instance of PSServer.
	 * @param numberOfServers Number of jobs which can be served simultaneously.
	 * @param serviceStrategies Array of service strategies, one per class.
	 */
	public PSServer(Integer numberOfServers, Boolean serverPreemptive, ServiceStrategy[] serviceStrategies) {
		super(false);
		this.numberOfServers = numberOfServers.intValue();
		this.serviceStrategies = serviceStrategies;
		this.psStrategies = new PSStrategy[serviceStrategies.length];
		for (int i = 0; i < serviceStrategies.length; i++) {
			this.psStrategies[i] = new EPSStrategy();
		}
		this.serviceWeights = new double[serviceStrategies.length];
		for (int i = 0; i < serviceStrategies.length; i++) {
			this.serviceWeights[i] = 1.0;
		}
	}

	public PSServer(Integer numberOfServers, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies) {
		this(numberOfServers, false, serviceStrategies);
	}

	/**
	 * Creates a new instance of PSServer.
	 * @param numberOfServers Number of jobs which can be served simultaneously.
	 * @param numberOfVisitsPerClass Number of job visits per class: if null
	 * the server will be single visit.
	 * @param serviceStrategies Array of service strategies, one per class.
	 */
	public PSServer(Integer numberOfServers, Boolean serverPreemptive, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies) {
		this(numberOfServers, false, serviceStrategies);
	}

	public PSServer(Integer numberOfServers, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies,
									PSStrategy[] psStrategies, Double[] serviceWeights) {
		this(numberOfServers, false, numberOfVisitsPerClass, serviceStrategies, psStrategies, serviceWeights);
	}

	/**
	 * Creates a new instance of PSServer.
	 * @param numberOfServers Number of jobs which can be served simultaneously.
	 * @param numberOfVisitsPerClass Number of job visits per class: if null
	 * the server will be single visit.
	 * @param serviceStrategies Array of service strategies, one per class.
	 * @param psStrategies Array of PS strategies, one per class.
	 * @param serviceWeights Array of service weights, one per class.
	 */
	public PSServer(Integer numberOfServers, Boolean serverPreemptive, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies,
									PSStrategy[] psStrategies, Double[] serviceWeights) {
		this(numberOfServers, false, numberOfVisitsPerClass, serviceStrategies);
		this.psStrategies = psStrategies;
		this.serviceWeights = new double[serviceWeights.length];
		for (int i = 0; i < serviceWeights.length; i++) {
			this.serviceWeights[i] = serviceWeights[i].doubleValue();
		}
	}

	@Override
	public double getDoubleSectionProperty(int id) throws NetException {
		switch (id) {
			case PROPERTY_ID_UTILIZATION:
				double sum = 0.0;
				for (int i = 0; i < jobClasses.size(); i++) {
					JobClass jobClass = jobClasses.get(i);
					sum += psJobsList.size(jobClass) * serviceFractions[jobClass.getId()];
				}
				return sum;
			case PROPERTY_ID_AVERAGE_UTILIZATION:
				return (getTime() <= 0.0) ? 0.0
								: psJobsList.getTotalSojournTime() / getTime() / numberOfServers;
			default:
				return super.getDoubleSectionProperty(id);
		}
	}

	@Override
	public double getDoubleSectionProperty(int id, JobClass jobClass) throws NetException {
		switch (id) {
			case PROPERTY_ID_UTILIZATION:
				return psJobsList.size(jobClass) * serviceFractions[jobClass.getId()];
			case PROPERTY_ID_AVERAGE_UTILIZATION:
				return (getTime() <= 0.0) ? 0.0
								: psJobsList.getTotalSojournTimePerClass(jobClass) / getTime() / numberOfServers;
			default:
				return super.getDoubleSectionProperty(id, jobClass);
		}
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		jobClasses = getJobClasses();
		jobsList = psJobsList = new PSJobInfoList(jobClasses.size());
		jobsList.setNetSystem(node.getNetSystem());
		psJobsList.setNetSystem(node.getNetSystem());
		psJobsList.setNumberOfServers(numberOfServers);
		serviceFractions = new double[jobClasses.size()];
		Arrays.fill(serviceFractions, 0.0);

		// Retrieve the nodeJobsList from the owner NetNode
		ownerNode = node;
		nodeJobsList = ownerNode.getJobInfoList();
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		// Clear jobsToBeReneged at the start of every process()
		jobsToBeReneged.clear();

		switch (message.getEvent()) {

			case NetEvent.EVENT_START:
				// Retrieve the netJobsList from the QueueNetwork (this is only possible at EVENT_START onwards)
				netJobsList = ownerNode.getGlobalJobInfoList();
				break;

			case NetEvent.EVENT_JOB:
				Job job = message.getJob();
				if (isMine(message)) {
					performServiceTimes(messageSendTime);
					performRenegingOperations(jobsToBeReneged);
					handleJobInfoList(messageJobInfo, PSEvent.JOB_OUT);
					messageToken = null;
					if (!jobDidRenege(job)) {
						sendForward(job, 0.0);
						sendBackward(NetEvent.EVENT_JOB_COMPLETED, job, 0.0);
					}
				} else {
					if (messageToken != null) {
						removeMessage(messageToken);
						performServiceTimes(messageSendTime);
						performRenegingOperations(jobsToBeReneged);
						messageToken = null;
					}
					// To support LD strategy, update jobInfoList before obtaining serviceTime
					PSJobInfo jobInfo = new PSJobInfo(job);
					handleJobInfoList(jobInfo, PSEvent.JOB_IN);
					double serviceTime = serviceStrategies[job.getJobClass().getId()].wait(this, job.getJobClass());
					jobInfo.setServiceTime(serviceTime);
					jobInfo.setResidualServiceTime(serviceTime);
					sendBackward(NetEvent.EVENT_ACK, job, 0.0);
				}
				updateServiceFractions();
				serviceJobs();
				break;

			case NetEvent.EVENT_RENEGE:
				Map<Job, Double> jobToRemainingTimeMap = (Map<Job, Double>) message.getData();
				Job jobReceived = null;
				Double timeLeftToRenege = null;
				for (Map.Entry<Job, Double> entry : jobToRemainingTimeMap.entrySet()) {
					jobReceived = entry.getKey();
					timeLeftToRenege = entry.getValue();
				}
				// Rose: so this is essentially getting the last item in the map? or is there ever only one job?
				// in that case we could just use a Pair(?)

				// Add the extra information (impatienceType and timeLeftToRenege) to the PSJobInfo of jobReceived
				PSJobInfo psJobInfo = (PSJobInfo) psJobsList.lookFor(jobReceived);
				psJobInfo.setImpatienceType(ImpatienceType.RENEGING);
				psJobInfo.setRenegingDelay(timeLeftToRenege);
				break;

			case NetEvent.EVENT_ACK:
				break;

			case NetEvent.EVENT_STOP:
				break;

			default:
				return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	private boolean jobDidRenege(Job job) {
		return jobsToBeReneged.contains(job);
	}

	private void performServiceTimes(double startTime) {
		Iterator<JobInfo> it = psJobsList.getInternalJobInfoList().iterator();
		double waitingTime = 0.0;

		while (it.hasNext()) {
			PSJobInfo jobInfo = (PSJobInfo) it.next();
			int jobClassID = jobInfo.getJob().getJobClass().getId();

			// If the job can renege, subtract the waitingTime from its renegingDelay
			if (jobInfo.hasImpatienceType(ImpatienceType.RENEGING)) {
				jobInfo.subtractFromRenegingDelay(waitingTime);

				// If renegingDelay <= 0.0, add the job to jobsToBeReneged so that it can later be reneged
				if (jobInfo.getRenegingDelay() <= 0.0) {
					jobsToBeReneged.add(jobInfo.getJob());
					continue;
				}
			}

			// Perform service for the job
			double serviceTime = (getTime() - startTime) * numberOfServers * serviceFractions[jobClassID];
			jobInfo.performServiceTime(serviceTime);

			waitingTime += serviceTime;
		}
	}

	/**
	 * Reneges the job from the PSServer, NetNode, and System.
	 *
	 * @param jobsToBeReneged the list of jobs being reneged.
	 */
	private void performRenegingOperations(List<Job> jobsToBeReneged) {
		for (Job job : jobsToBeReneged) {
			JobInfo jobInfoInPSJobsList = getJobInfoFromBuffer(job, psJobsList);
			JobInfo jobInfoInNetNode = getJobInfoFromBuffer(job, nodeJobsList);

			// Renege jobs from jobsInQueue (Queue item), nodeJobsList (NetNode item) and netJobsList (Global item)
			psJobsList.renegeJob(jobInfoInPSJobsList);
			nodeJobsList.renegeJob(jobInfoInNetNode);
			netJobsList.renegeJob(job);
		}
	}

	private JobInfo getJobInfoFromBuffer(Job job, JobInfoList buffer) {
		return buffer.lookFor(job);
	}

	private void handleJobInfoList(PSJobInfo jobInfo, PSEvent event) {
		JobClass jobClass = jobInfo.getJob().getJobClass();
		if (event == PSEvent.JOB_IN) {
			psJobsList.psUpdateUtilization(jobClass, serviceFractions);
			psJobsList.add(jobInfo);
		} else {
			double queueTime = (getTime() - jobInfo.getEnteringTime()) - jobInfo.getServiceTime();
			if (queueTime < 0.0) {
				queueTime = 0.0;
			}
			psJobsList.psUpdateSojournTime(jobClass, jobInfo.getServiceTime());
			psJobsList.psUpdateQueueTime(jobClass, queueTime);
			psJobsList.psUpdateUtilization(jobClass, serviceFractions);
			psJobsList.remove(jobInfo);
		}
	}

	private void updateServiceFractions() {
		if (psJobsList.size() <= numberOfServers) {
			for (int i = 0; i < jobClasses.size(); i++) {
				JobClass jobClass = jobClasses.get(i);
				if (psJobsList.size(jobClass) <= 0) {
					serviceFractions[i] = 0.0;
				} else {
					serviceFractions[i] = 1.0 / numberOfServers;
				}
			}
		} else {
			boolean[] serviceSaturated = new boolean[jobClasses.size()];
			Arrays.fill(serviceSaturated, false);
			double residualCapacity = numberOfServers;
			boolean allocationComplete = false;
			while (!allocationComplete) {
				for (int i = 0; i < jobClasses.size(); i++) {
					if (!serviceSaturated[i]) {
						JobClass jobClass = jobClasses.get(i);
						serviceFractions[i] = psStrategies[i].slice(psJobsList, jobClasses, serviceWeights, serviceSaturated, jobClass)
										* (residualCapacity / numberOfServers);
					}
				}
				allocationComplete = true;
				for (int i = 0; i < jobClasses.size(); i++) {
					if (serviceFractions[i] > 1.0 / numberOfServers) {
						JobClass jobClass = jobClasses.get(i);
						serviceFractions[i] = 1.0 / numberOfServers;
						residualCapacity -= psJobsList.size(jobClass);
						serviceSaturated[i] = true;
						allocationComplete = false;
					}
				}
			}
		}
	}

	private void serviceJobs() {
		if (psJobsList.size() > 0) {
			double minWaitTime = Double.MAX_VALUE;
			Iterator<JobInfo> it = psJobsList.getInternalJobInfoList().iterator();
			while (it.hasNext()) {
				PSJobInfo jobInfo = (PSJobInfo) it.next();
				int jobClassID = jobInfo.getJob().getJobClass().getId();
				double waitTime = jobInfo.getResidualServiceTime() / (numberOfServers * serviceFractions[jobClassID]);
				if (waitTime < minWaitTime) {
					messageJobInfo = jobInfo;
					minWaitTime = waitTime;
				}
			}
			messageToken = sendMe(messageJobInfo.getJob(), minWaitTime);
			messageSendTime = getTime();
		}
	}

	private double getTime() {
		return this.getOwnerNode().getNetSystem().getTime();
	}

}
