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

package jmt.engine.QueueNet;

import java.util.*;

import jmt.engine.dataAnalysis.InverseMeasure;
import jmt.engine.dataAnalysis.Measure;

/**
 * This class implements a linked job info list.
 * @author Francesco Radaelli, Stefano Omini
 * 
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public class LinkedJobInfoList implements JobInfoList {

	protected static final int REMOVE_FIRST = 1;
	protected static final int REMOVE_LAST = 2;
	protected static final int REMOVE_SPECIFIC = 0;

	protected int numberOfJobClasses;

	//contain JobInfo objects
	protected LinkedList<JobInfo> list;
	protected LinkedList<JobInfo> listPerClass[];

	//arrivals and completions
	protected Map<Integer, List<Double>> retrialOrbit = new HashMap<>();
	protected Map<Integer, List<Double>> retrialOrbitPerClass[];

	//arrivals and completions
	protected int jobsIn;
	protected int jobsInPerClass[];

	protected int jobsOut;
	protected int jobsOutPerClass[];

	// due to calculate the cache request count for all the class is meaningless
	// we only need to calculate the cache request count for each class.
	protected int jobsTotalCacheHitCount;
	protected int jobsTotalCacheMissCount;
	protected int jobsCacheCountPerClass[];


	protected int jobsInService;
	protected int jobsInServicePerClass[];

	protected int activeServers;

	protected double lastJobInTime;
	protected double lastJobInTimePerClass[];

	// once a job is removed from this list -> call remove related method to update job Out Time.
	// to calculate the 'throughput' and 'throughput per sink'
	protected double lastJobOutTime;
	protected double lastJobOutTimePerClass[];

	protected double lastJobStartTime;
	protected double lastJobStartTimePerClass[];

	protected double lastJobEndTime;
	protected double lastJobEndTimePerClass[];

	protected double lastJobDropTime;
	protected double lastJobDropTimePerClass[];

	protected double lastJobSwitchTime;
	protected double lastJobSwitchTimePerClass[];

	protected double lastJobBalkingTime;
	protected double lastJobBalkingTimePerClass[];

	protected double lastJobRenegingTime;
	protected double lastJobRenegingTimePerClass[];

	protected double lastJobRetrialAttemptTime;
	protected double lastJobRetrialAttemptTimePerClass[];

	protected double lastRetrialOrbitModifyTime;
	protected double lastRetrialOrbitModifyTimePerClass[];

	// The difference between sojourn time and waiting time:
	// Sojourn time is 'current time at job removal - jobInfo.enteringTime'
	// Waiting time is 'current time at job entering queue - job.systemEnteringTime'
	// jobInfo.enteringTime is assigned at each strategies or section creating a jobInfo that containing a job and a time.
	// job.systemEnteringTime is only assigned in job Creation or GlobalJobInfoList adding jobs.
	protected double totalSojournTime;
	protected double totalSojournTimePerClass[];

	protected double lastJobSojournTime;
	protected double lastJobSojournTimePerClass[];

	protected double lastActiveServersTime;

	protected Measure queueLength;
	protected Measure queueLengthPerClass[];

	protected Measure responseTime;
	protected Measure responseTimePerClass[];

	protected Measure residenceTime;
	protected Measure residenceTimePerClass[];

	protected InverseMeasure arrivalRate;
	protected InverseMeasure arrivalRatePerClass[];

	protected InverseMeasure throughput;
	protected InverseMeasure throughputPerClass[];

	protected Measure utilization;
	protected Measure utilizationPerClass[];

	protected Measure utilizationJoin;
	protected Measure utilizationPerClassJoin[];

	protected Measure effectiveUtilization;
	protected Measure effectiveUtilizationPerClass[];

	protected Measure hitRate;
	protected Measure hitRatePerClass[];

	protected InverseMeasure dropRate;
	protected InverseMeasure dropRatePerClass[];

	protected InverseMeasure balkingRate;
	protected InverseMeasure balkingRatePerClass[];

	protected InverseMeasure renegingRate;
	protected InverseMeasure renegingRatePerClass[];

	protected InverseMeasure retrialAttemptsRate;
	protected InverseMeasure retrialAttemptsRatePerClass[];

	protected Measure retrialOrbitSize;
	protected Measure retrialOrbitSizePerClass[];

	protected Measure retrialOrbitTime;
	protected Measure retrialOrbitTimePerClass[];

	protected Measure responseTimePerSink;
	protected Measure responseTimePerSinkPerClass[];

	protected InverseMeasure throughputPerSink;
	protected InverseMeasure throughputPerSinkPerClass[];

	protected Measure tardiness;
	protected Measure[] tardinessPerClass;
	protected Measure earliness;
	protected Measure[] earlinessPerClass;
	protected Measure lateness;
	protected Measure[] latenessPerClass;
	/* Only set if this joblist is for a blocking region input station */
	protected double[] regionSoftDeadlinesByClass;


	protected Measure numberOfActiveServers;

	/** The number of servers to estimate Utilization measure on multiserver environments */
	protected int numberOfServers = 1;

	private NetSystem netSystem;

	/**
	 * Creates a new JobInfoList instance.
	 * @param numberOfJobClasses number of job classes.
	if (preemption) {
	jobInService = job;
	jobInService.setServingMessage(jobInServiceMessage);
	jobInService.setIsJobInService(true);
	}
	 */
	@SuppressWarnings("unchecked")
	public LinkedJobInfoList(int numberOfJobClasses) {
		this.numberOfJobClasses = numberOfJobClasses;
		list = new LinkedList<JobInfo>();
		listPerClass = new LinkedList[numberOfJobClasses];
		retrialOrbitPerClass = new HashMap[numberOfJobClasses];
		for (int i = 0; i < numberOfJobClasses; i++) {
			listPerClass[i] = new LinkedList<JobInfo>();
			retrialOrbitPerClass[i] = new HashMap<Integer, List<Double>>();
		}
		jobsInPerClass = new int[numberOfJobClasses];
		jobsOutPerClass = new int[numberOfJobClasses];
		jobsInServicePerClass = new int[numberOfJobClasses];
		lastJobInTimePerClass = new double[numberOfJobClasses];
		lastJobOutTimePerClass = new double[numberOfJobClasses];
		lastJobStartTimePerClass = new double[numberOfJobClasses];
		lastJobEndTimePerClass = new double[numberOfJobClasses];
		lastJobDropTimePerClass = new double[numberOfJobClasses];
		lastJobSwitchTimePerClass = new double[numberOfJobClasses];
		lastJobBalkingTimePerClass = new double[numberOfJobClasses];
		lastJobRenegingTimePerClass = new double[numberOfJobClasses];
		lastJobRetrialAttemptTimePerClass = new double[numberOfJobClasses];
		lastRetrialOrbitModifyTimePerClass = new double[numberOfJobClasses];
		totalSojournTimePerClass = new double[numberOfJobClasses];
		lastJobSojournTimePerClass = new double[numberOfJobClasses];
	}

	/**---------------------------------------------------------------------
	 *--------------------------- "GET" METHODS ----------------------------
	 *---------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#size()
	 */
	public int size() {
		return list.size();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#size(jmt.engine.QueueNet.JobClass)
	 */
	public int size(JobClass jobClass) {
		return listPerClass[jobClass.getId()].size();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getRetrialOrbit()
	 */
	public Map<Integer, List<Double>> getRetrialOrbit() {
		return retrialOrbit;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#addToRetrialOrbit(jmt.engine.QueueNet.Job)
	 */
	public void addToRetrialOrbit(Job job) {
		int id = job.getId();
		int classID = job.getJobClass().getId();
		updateRetrialOrbitSize(job);
		List<Double> currList = (retrialOrbit.get(id) == null ? new ArrayList<Double>() : retrialOrbit.get(id));
		currList.add(this.getTime());
		retrialOrbit.put(job.getId(), currList);
		retrialOrbitPerClass[classID].put(job.getId(), currList);
		lastRetrialOrbitModifyTime = getTime();
		lastRetrialOrbitModifyTimePerClass[classID] = getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeFromRetrialOrbit(jmt.engine.QueueNet.Job)
	 */
	public void removeFromRetrialOrbit(Job job) {
		int id = job.getId();
		int classID = job.getJobClass().getId();
		updateRetrialOrbitSize(job);
		updateRetrialOrbitTime(job);
		retrialOrbit.remove(id);
		retrialOrbitPerClass[classID].remove(id);
		lastRetrialOrbitModifyTime = getTime();
		lastRetrialOrbitModifyTimePerClass[job.getJobClass().getId()] = getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getJobsIn()
	 */
	public int getJobsIn() {
		return jobsIn;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getJobsInPerClass(jmt.engine.QueueNet.JobClass)
	 */
	public int getJobsInPerClass(JobClass jobClass) {
		return jobsInPerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getJobsOut()
	 */
	public int getJobsOut() {
		return jobsOut;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getJobsOutPerClass(jmt.engine.QueueNet.JobClass)
	 */
	public int getJobsOutPerClass(JobClass jobClass) {
		return jobsOutPerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getJobsInService()
	 */
	public int getJobsInService() {
		return jobsInService;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getJobsInServicePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public int getJobsInServicePerClass(JobClass jobClass) {
		return jobsInServicePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobInTime()
	 */
	public double getLastJobInTime() {
		return lastJobInTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobInTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobInTimePerClass(JobClass jobClass) {
		return lastJobInTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobOutTime()
	 */
	public double getLastJobOutTime() {
		return lastJobOutTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobOutTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobOutTimePerClass(JobClass jobClass) {
		return lastJobOutTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobStartTime()
	 */
	public double getLastJobStartTime() {
		return lastJobStartTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#lastJobStartTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobStartTimePerClass(JobClass jobClass) {
		return lastJobStartTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobEndTime()
	 */
	public double getLastJobEndTime() {
		return lastJobEndTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobEndTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobEndTimePerClass(JobClass jobClass) {
		return lastJobEndTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobDropTime()
	 */
	public double getLastJobDropTime() {
		return lastJobDropTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobDropTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobDropTimePerClass(JobClass jobClass) {
		return lastJobDropTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobSwitchTime()
	 */
	public double getLastJobSwitchTime() {
		return lastJobSwitchTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobSwitchTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobSwitchTimePerClass(JobClass jobClass) {
		return lastJobSwitchTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobBalkingTime()
	 */
	public double getLastJobBalkingTime() {
		return lastJobBalkingTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobBalkingTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobBalkingTimePerClass(JobClass jobClass) {
		return lastJobBalkingTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobRenegingTime()
	 */
	public double getLastJobRenegingTime() {
		return lastJobRenegingTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobRenegingTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobRenegingTimePerClass(JobClass jobClass) {
		return lastJobRenegingTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobRetrialAttemptTime()
	 */
	public double getLastJobRetrialAttemptTime() {
		return lastJobRetrialAttemptTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobRetrialTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobRetrialTimePerClass(JobClass jobClass) {
		return lastJobRetrialAttemptTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastModifyTime()
	 */
	public double getLastModifyTime() {
		double max = 0.0;
		max = Math.max(lastJobInTime, max);
		max = Math.max(lastJobOutTime, max);
		max = Math.max(lastJobDropTime, max);
		max = Math.max(lastJobSwitchTime, max);
		return max;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastModifyTimePerClass(jmt.engine.QueueNet.JobClass)
	 * get the most recent time of a job events (JobOut, JobIn, JobDrop)
	 */
	public double getLastModifyTimePerClass(JobClass jobClass) {
		double max = 0.0;
		max = Math.max(lastJobInTimePerClass[jobClass.getId()], max);
		max = Math.max(lastJobOutTimePerClass[jobClass.getId()], max);
		max = Math.max(lastJobDropTimePerClass[jobClass.getId()], max);
		max = Math.max(lastJobSwitchTimePerClass[jobClass.getId()], max);
		return max;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastServiceModifyTime()
	 */
	public double getLastServiceModifyTime() {
		double max = 0.0;
		max = Math.max(lastJobStartTime, max);
		max = Math.max(lastJobEndTime, max);
		return max;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastServiceModifyTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastServiceModifyTimePerClass(JobClass jobClass) {
		double max = 0.0;
		max = Math.max(lastJobStartTimePerClass[jobClass.getId()], max);
		max = Math.max(lastJobEndTimePerClass[jobClass.getId()], max);
		return max;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastRetrialOrbitModifyTime()
	 */
	public double getLastRetrialOrbitModifyTime() {
		return lastRetrialOrbitModifyTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastRetrialOrbitModifyTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastRetrialOrbitModifyTimePerClass(JobClass jobClass) {
		return lastRetrialOrbitModifyTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getTotalSojournTime()
	 */
	public double getTotalSojournTime() {
		return totalSojournTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getTotalSojournTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getTotalSojournTimePerClass(JobClass jobClass) {
		return totalSojournTimePerClass[jobClass.getId()];
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobSojournTime()
	 */
	public double getLastJobSojournTime() {
		return lastJobSojournTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastJobSojournTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastJobSojournTimePerClass(JobClass jobClass) {
		return lastJobSojournTimePerClass[jobClass.getId()];
	}

	public double getLastActiveServersTime() { return lastActiveServersTime; }

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#lookFor(jmt.engine.QueueNet.Job)
	 */
	public JobInfo lookFor(Job job) {
		ListIterator<JobInfo> it = list.listIterator();
		JobInfo jobInfo = null;
		while (it.hasNext()) {
			jobInfo = it.next();
			if (jobInfo.getJob().equals(job)) {
				return jobInfo;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getInternalJobInfoList()
	 */
	public List<JobInfo> getInternalJobInfoList() {
		return list;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getInternalJobInfoList(jmt.engine.QueueNet.JobClass)
	 */
	public List<JobInfo> getInternalJobInfoList(JobClass jobClass) {
		return listPerClass[jobClass.getId()];
	}

	/**---------------------------------------------------------------------
	 *--------------------- "ADD" AND "REMOVE" METHODS ---------------------
	 *---------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#add(jmt.engine.QueueNet.JobInfo)
	 */
	public void add(JobInfo jobInfo) {
		updateAdd(jobInfo);
		list.add(jobInfo);
		listPerClass[jobInfo.getJob().getJobClass().getId()].add(jobInfo);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#addFirst(jmt.engine.QueueNet.JobInfo)
	 */
	public void addFirst(JobInfo jobInfo) {
		updateAdd(jobInfo);
		list.addFirst(jobInfo);
		listPerClass[jobInfo.getJob().getJobClass().getId()].addFirst(jobInfo);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#addLast(jmt.engine.QueueNet.JobInfo)
	 */
	public void addLast(JobInfo jobInfo) {
		updateAdd(jobInfo);
		list.addLast(jobInfo);
		listPerClass[jobInfo.getJob().getJobClass().getId()].addLast(jobInfo);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#add(int, jmt.engine.QueueNet.JobInfo)
	 */
	public void add(int index, JobInfo jobInfo) {
		updateAdd(jobInfo);
		JobClass jobClass = jobInfo.getJob().getJobClass();
		ListIterator<JobInfo> it = list.listIterator();
		int perClassIndex = 0;
		for (int i = 0; i < index; i++) {
			if (it.next().getJob().getJobClass() == jobClass) {
				perClassIndex++;
			}
		}
		it.add(jobInfo);
		listPerClass[jobClass.getId()].add(perClassIndex, jobInfo);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#add(int, jmt.engine.QueueNet.JobInfo, boolean)
	 */
	public void add(int index, JobInfo jobInfo, boolean isPerClassHead) {
		updateAdd(jobInfo);
		list.add(index, jobInfo);
		if (isPerClassHead) {
			listPerClass[jobInfo.getJob().getJobClass().getId()].addFirst(jobInfo);
		} else {
			listPerClass[jobInfo.getJob().getJobClass().getId()].addLast(jobInfo);
		}
	}

	protected void updateAdd(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateQueueLength(jobInfo);
		updateArrivalRate(jobInfo);
		updateUtilization(jobInfo);
		updateUtilizationJoin(jobInfo);
		jobsIn++;
		jobsInPerClass[c]++;
		lastJobInTime = getTime();
		lastJobInTimePerClass[c] = getTime();
	}

	protected void updateAfterRemoval(JobInfo jobInfo){
		int c = jobInfo.getJob().getJobClass().getId();
		updateQueueLength(jobInfo);
		updateResponseTime(jobInfo);
		updateResidenceTime(jobInfo);
		updateThroughput(jobInfo);
		updateUtilization(jobInfo);
		updateUtilizationJoin(jobInfo);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#remove(jmt.engine.QueueNet.JobInfo)
	 */
	public void remove(JobInfo jobInfo) {
		doRemove(jobInfo, REMOVE_SPECIFIC, REMOVE_SPECIFIC);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeOnly(jmt.engine.QueueNet.JobInfo)
	 * if event type is 'NetEvent.EVENT_RETRIAL' call removeOnly, which will not 'updateResponseTime(jobInfo)' and
	 * 'updateResidenceTime(jobInfo)'
	 */
	public void removeOnly(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateQueueLength(jobInfo);
		updateThroughput(jobInfo);
		updateUtilization(jobInfo);
		updateUtilizationJoin(jobInfo);
		finalRemove(jobInfo, list, REMOVE_SPECIFIC);
		finalRemove(jobInfo, listPerClass[c], REMOVE_SPECIFIC);
		jobsOut++;
		jobsOutPerClass[c]++;
		lastJobOutTime = getTime();
		lastJobOutTimePerClass[c] = getTime();
		totalSojournTime += getTime() - jobInfo.getEnteringTime();
		totalSojournTimePerClass[c] += getTime() - jobInfo.getEnteringTime();
		lastJobSojournTime = getTime() - jobInfo.getEnteringTime();
		lastJobSojournTimePerClass[c] = getTime() - jobInfo.getEnteringTime();
	}

	public void removeWithoutUpdating(JobInfo jobInfo){
		int c = jobInfo.getJob().getJobClass().getId();
		finalRemove(jobInfo, list, 0);
		finalRemove(jobInfo, listPerClass[c], 0);
		jobsOut++;
		jobsOutPerClass[c]++;
		lastJobOutTime = getTime();
		lastJobOutTimePerClass[c] = getTime();
		totalSojournTime += getTime() - jobInfo.getEnteringTime();
		totalSojournTimePerClass[c] += getTime() - jobInfo.getEnteringTime();
		lastJobSojournTime = getTime() - jobInfo.getEnteringTime();
		lastJobSojournTimePerClass[c] = getTime() - jobInfo.getEnteringTime();
	}

	public void updateWithoutRemoving(JobInfo jobInfo){
		updateAfterRemoval(jobInfo);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeFirst()
	 * Since the jobInfo appear the first item in 'list' so it's also the first item in perClass list.
	 */
	public JobInfo removeFirst() {
		JobInfo jobInfo = list.getFirst();
		if (jobInfo != null) {
			doRemove(jobInfo, REMOVE_FIRST, REMOVE_FIRST);
			return jobInfo;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeFirst(jmt.engine.QueueNet.JobClass)
	 * Since we remove the first jobInfo belong to 'jobClass',
	 * we remove exactly itself from 'list' and the first position in perClass list.
	 */
	public JobInfo removeFirst(JobClass jobClass) {
		int c = jobClass.getId();
		JobInfo jobInfo = listPerClass[c].getFirst();
		if (jobInfo != null) {
			doRemove(jobInfo, REMOVE_SPECIFIC, REMOVE_FIRST);
			return jobInfo;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeLast()
	 * Since the jobInfo appear the last item in 'list' so it's also the last item in perClass list.
	 */
	public JobInfo removeLast() {
		JobInfo jobInfo = list.getLast();
		if (jobInfo != null) {
			doRemove(jobInfo, REMOVE_LAST, REMOVE_LAST);
			return jobInfo;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeLast(jmt.engine.QueueNet.JobClass)
	 * Since we remove the last jobInfo belong to 'jobClass',
	 * we remove exactly itself from 'list' and the first position in perClass list.
	 */
	public JobInfo removeLast(JobClass jobClass) {
		int c = jobClass.getId();
		JobInfo jobInfo = listPerClass[c].getLast();
		if (jobInfo != null) {
			doRemove(jobInfo, REMOVE_SPECIFIC, REMOVE_LAST);
			return jobInfo;
		} else {
			return null;
		}
	}

	protected void doRemove(JobInfo jobInfo, int position, int perClassPosition) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateQueueLength(jobInfo);
		updateResponseTime(jobInfo);
		updateResidenceTime(jobInfo);
		updateThroughput(jobInfo);
		updateUtilization(jobInfo);
		updateUtilizationJoin(jobInfo);
		updateTardiness(jobInfo);
		updateEarliness(jobInfo);
		updateLateness(jobInfo);
		finalRemove(jobInfo, list, position);
		finalRemove(jobInfo, listPerClass[c], perClassPosition);
		jobsOut++;
		jobsOutPerClass[c]++;
		lastJobOutTime = getTime();
		lastJobOutTimePerClass[c] = getTime();
		totalSojournTime += getTime() - jobInfo.getEnteringTime();
		totalSojournTimePerClass[c] += getTime() - jobInfo.getEnteringTime();
		lastJobSojournTime = getTime() - jobInfo.getEnteringTime();
		lastJobSojournTimePerClass[c] = getTime() - jobInfo.getEnteringTime();
	}

	protected void finalRemove(JobInfo what, LinkedList<JobInfo> list, int position) {
		switch (position) {
			case REMOVE_FIRST:
				list.removeFirst();
				break;
			case REMOVE_LAST:
				list.removeLast();
				break;
			default:
				list.remove(what);
				break;
		}
	}

	/**---------------------------------------------------------------------
	 *------------------- "ANALYZE" AND "UPDATE" METHODS -------------------
	 *---------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeQueueLength(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	public void analyzeQueueLength(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (queueLengthPerClass == null) {
				queueLengthPerClass = new Measure[numberOfJobClasses];
			}
			queueLengthPerClass[jobClass.getId()] = measurement;
		} else {
			queueLength = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeResponseTime(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	public void analyzeResponseTime(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (responseTimePerClass == null) {
				responseTimePerClass = new Measure[numberOfJobClasses];
			}
			responseTimePerClass[jobClass.getId()] = measurement;
		} else {
			responseTime = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeResidenceTime(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	public void analyzeResidenceTime(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (residenceTimePerClass == null) {
				residenceTimePerClass = new Measure[numberOfJobClasses];
			}
			residenceTimePerClass[jobClass.getId()] = measurement;
		} else {
			residenceTime = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeArrivalRate(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.InverseMeasure)
	 */
	public void analyzeArrivalRate(JobClass jobClass, InverseMeasure measurement) {
		if (jobClass != null) {
			if (arrivalRatePerClass == null) {
				arrivalRatePerClass = new InverseMeasure[numberOfJobClasses];
			}
			arrivalRatePerClass[jobClass.getId()] = measurement;
		} else {
			arrivalRate = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeThroughput(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.InverseMeasure)
	 */
	public void analyzeThroughput(JobClass jobClass, InverseMeasure measurement) {
		if (jobClass != null) {
			if (throughputPerClass == null) {
				throughputPerClass = new InverseMeasure[numberOfJobClasses];
			}
			throughputPerClass[jobClass.getId()] = measurement;
		} else {
			throughput = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeUtilization(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	public void analyzeUtilization(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (utilizationPerClass == null) {
				utilizationPerClass = new Measure[numberOfJobClasses];
			}
			utilizationPerClass[jobClass.getId()] = measurement;
		} else {
			utilization = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeUtilizationJoin(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	public void analyzeUtilizationJoin(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (utilizationPerClassJoin == null) {
				utilizationPerClassJoin = new Measure[numberOfJobClasses];
			}
			utilizationPerClassJoin[jobClass.getId()] = measurement;
		} else {
			utilizationJoin = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeEffectiveUtilization(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	public void analyzeEffectiveUtilization(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (effectiveUtilizationPerClass == null) {
				effectiveUtilizationPerClass = new Measure[numberOfJobClasses];
			}
			effectiveUtilizationPerClass[jobClass.getId()] = measurement;
		} else {
			effectiveUtilization = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeDropRate(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.InverseMeasure)
	 */
	public void analyzeDropRate(JobClass jobClass, InverseMeasure measurement) {
		if (jobClass != null) {
			if (dropRatePerClass == null) {
				dropRatePerClass = new InverseMeasure[numberOfJobClasses];
			}
			dropRatePerClass[jobClass.getId()] = measurement;
		} else {
			dropRate = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeBalkingRate(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.InverseMeasure)
	 */
	public void analyzeBalkingRate(JobClass jobClass, InverseMeasure measurement) {
		if (jobClass != null) {
			if (balkingRatePerClass == null) {
				balkingRatePerClass = new InverseMeasure[numberOfJobClasses];
			}
			balkingRatePerClass[jobClass.getId()] = measurement;
		} else {
			balkingRate = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeRenegingRate(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.InverseMeasure)
	 */
	public void analyzeRenegingRate(JobClass jobClass, InverseMeasure measurement) {
		if (jobClass != null) {
			if (renegingRatePerClass == null) {
				renegingRatePerClass = new InverseMeasure[numberOfJobClasses];
			}
			renegingRatePerClass[jobClass.getId()] = measurement;
		} else {
			renegingRate = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeRetrialAttemptsRate(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.InverseMeasure)
	 */
	@Override
	public void analyzeRetrialAttemptsRate(JobClass jobClass, InverseMeasure measurement) {
		if (jobClass != null) {
			if (retrialAttemptsRatePerClass == null) {
				retrialAttemptsRatePerClass = new InverseMeasure[numberOfJobClasses];
			}
			retrialAttemptsRatePerClass[jobClass.getId()] = measurement;
		} else {
			retrialAttemptsRate = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeRetrialOrbitSize(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	@Override
	public void analyzeRetrialOrbitSize(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (retrialOrbitSizePerClass == null) {
				retrialOrbitSizePerClass = new Measure[numberOfJobClasses];
			}
			retrialOrbitSizePerClass[jobClass.getId()] = measurement;
		} else {
			retrialOrbitSize = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeRetrialOrbitTime(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	@Override
	public void analyzeRetrialOrbitTime(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (retrialOrbitTimePerClass == null) {
				retrialOrbitTimePerClass = new Measure[numberOfJobClasses];
			}
			retrialOrbitTimePerClass[jobClass.getId()] = measurement;
		} else {
			retrialOrbitTime = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeResponseTimePerSink(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.Measure)
	 */
	public void analyzeResponseTimePerSink(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (responseTimePerSinkPerClass == null) {
				responseTimePerSinkPerClass = new Measure[numberOfJobClasses];
			}
			responseTimePerSinkPerClass[jobClass.getId()] = measurement;
		} else {
			responseTimePerSink = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeThroughputPerSink(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.InverseMeasure)
	 */
	public void analyzeThroughputPerSink(JobClass jobClass, InverseMeasure measurement) {
		if (jobClass != null) {
			if (throughputPerSinkPerClass == null) {
				throughputPerSinkPerClass = new InverseMeasure[numberOfJobClasses];
			}
			throughputPerSinkPerClass[jobClass.getId()] = measurement;
		} else {
			throughputPerSink = measurement;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeNumberOfServers(jmt.engine.dataAnalysis.Measure)
	 */
	public void analyzeNumberOfServers(Measure measurement){
		numberOfActiveServers = measurement;
	}


	@Override
	public void analyzeTardiness(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (tardinessPerClass == null) {
				tardinessPerClass = new Measure[numberOfJobClasses];
			}
			tardinessPerClass[jobClass.getId()] = measurement;
		} else {
			tardiness = measurement;
		}
	}

	@Override
	public void analyzeEarliness(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (earlinessPerClass == null) {
				earlinessPerClass = new Measure[numberOfJobClasses];
			}
			earlinessPerClass[jobClass.getId()] = measurement;
		} else {
			earliness = measurement;
		}
	}

	@Override
	public void analyzeLateness(JobClass jobClass, Measure measurement) {
		if (jobClass != null) {
			if (latenessPerClass == null) {
				latenessPerClass = new Measure[numberOfJobClasses];
			}
			latenessPerClass[jobClass.getId()] = measurement;
		} else {
			lateness = measurement;
		}
	}

	protected void updateTardiness(JobInfo jobInfo) {
		JobClass jobClass = jobInfo.getJob().getJobClass();
		int c = jobClass.getId();
		double softDeadline = regionSoftDeadlinesByClass == null ?
				jobInfo.getJob().getCurrentStationSoftDeadline() :
				(regionSoftDeadlinesByClass[c] + jobInfo.getEnteringTime());
		if (tardinessPerClass != null) {
			Measure m = tardinessPerClass[c];
			if (m != null) {
				// The soft deadline retrieved by getCurrentStationSoftDeadline is absolute, so we do not need to
				// offset it as is done in GlobalJobInfoList.
				m.update(Math.max(0, getTime() - softDeadline), 1.0);
			}
		}
		if (tardiness != null) {
			tardiness.update(Math.max(0, getTime() - softDeadline), 1.0);
		}
	}

	protected void updateEarliness(JobInfo jobInfo) {
		JobClass jobClass = jobInfo.getJob().getJobClass();
		int c = jobClass.getId();
		double softDeadline = regionSoftDeadlinesByClass == null ?
				jobInfo.getJob().getCurrentStationSoftDeadline() :
				(regionSoftDeadlinesByClass[c] + jobInfo.getEnteringTime());
		if (earlinessPerClass != null) {
			Measure m = earlinessPerClass[c];
			if (m != null) {
				m.update(Math.max(0, softDeadline - getTime()), 1.0);
			}
		}
		if (earliness != null) {
			earliness.update(Math.max(0, softDeadline - getTime()), 1.0);
		}
	}

	protected void updateLateness(JobInfo jobInfo) {
		JobClass jobClass = jobInfo.getJob().getJobClass();
		int c = jobClass.getId();
		double softDeadline = regionSoftDeadlinesByClass == null ?
				jobInfo.getJob().getCurrentStationSoftDeadline() :
				(regionSoftDeadlinesByClass[c] + jobInfo.getEnteringTime());
		if (latenessPerClass != null) {
			Measure m = latenessPerClass[c];
			if (m != null) {
				m.update(getTime() - softDeadline, 1.0);
			}
		}
		if (lateness != null) {
			lateness.update(getTime() - softDeadline, 1.0);
		}
	}

	protected void updateQueueLength(JobInfo jobInfo) {
		if (queueLengthPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = queueLengthPerClass[c];
			if (m != null) {
				m.update(listPerClass[c].size(), getTime() - getLastModifyTimePerClass(jobClass));
			}
		}
		if (queueLength != null) {
			queueLength.update(list.size(), getTime() - getLastModifyTime());
		}
	}

	protected void updateResponseTime(JobInfo jobInfo) {
		if (responseTimePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = responseTimePerClass[c];
			if (m != null) {
				m.update(getTime() - jobInfo.getEnteringTime(), 1.0);
			}
		}
		if (responseTime != null) {
			responseTime.update(getTime() - jobInfo.getEnteringTime(), 1.0);
		}
	}

	protected void updateResidenceTime(JobInfo jobInfo) {
		if (residenceTimePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = residenceTimePerClass[c];
			if (m != null) {
				m.updateSampleJob(jobInfo.getJob());
				m.update(getTime() - jobInfo.getEnteringTime(), 1.0);
			}
		}
		if (residenceTime != null) {
			residenceTime.updateSampleJob(jobInfo.getJob());
			residenceTime.update(getTime() - jobInfo.getEnteringTime(), 1.0);
		}
	}

	protected void updateArrivalRate(JobInfo jobInfo) {
		if (arrivalRatePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = arrivalRatePerClass[c];
			if (m != null) {
				m.update(getTime() - getLastJobInTimePerClass(jobClass), 1.0);
			}
		}
		if (arrivalRate != null) {
			arrivalRate.update(getTime() - getLastJobInTime(), 1.0);
		}
	}

	protected void updateThroughput(JobInfo jobInfo) {
		if (throughputPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = throughputPerClass[c];
			if (m != null) {
				m.update(getTime() - getLastJobOutTimePerClass(jobClass), 1.0);
			}
		}
		if (throughput != null) {
			throughput.update(getTime() - getLastJobOutTime(), 1.0);
		}
	}

	protected void updateUtilization(JobInfo jobInfo) {
		if (utilizationPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = utilizationPerClass[c];
			if (m != null) {
				m.update((double) listPerClass[c].size() / numberOfServers, getTime() - getLastModifyTimePerClass(jobClass));
			}
		}
		if (utilization != null) {
			utilization.update((double) list.size() / numberOfServers, getTime() - getLastModifyTime());
		}
	}

	protected void updateUtilizationJoin(JobInfo jobInfo) {
		if (utilizationPerClassJoin != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = utilizationPerClassJoin[c];
			if (m != null) {
				if (listPerClass[c].size() > 0) {
					m.update(1.0, getTime() - getLastModifyTimePerClass(jobClass));
				} else {
					m.update(0.0, getTime() - getLastModifyTimePerClass(jobClass));
				}
			}
		}
		if (utilizationJoin != null) {
			if (list.size() > 0) {
				utilizationJoin.update(1.0, getTime() - getLastModifyTime());
			} else {
				utilizationJoin.update(0.0, getTime() - getLastModifyTime());
			}
		}
	}

	protected void updateEffectiveUtilization(JobInfo jobInfo) {
		if (effectiveUtilizationPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = effectiveUtilizationPerClass[c];
			if (m != null) {
				m.update((double) jobsInServicePerClass[c] / numberOfServers, getTime() - getLastServiceModifyTimePerClass(jobClass));
			}
		}
		if (effectiveUtilization != null) {
			effectiveUtilization.update((double) jobsInService / numberOfServers, getTime() - getLastServiceModifyTime());
		}
	}

	protected void updateDropRate(JobInfo jobInfo) {
		if (dropRatePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = dropRatePerClass[c];
			if (m != null) {
				m.update(getTime() - getLastJobDropTimePerClass(jobClass), 1.0);
			}
		}
		if (dropRate != null) {
			dropRate.update(getTime() - getLastJobDropTime(), 1.0);
		}
	}

	protected void updateBalkingRate(JobInfo jobInfo) {
		if (balkingRatePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = balkingRatePerClass[c];
			if (m != null) {
				m.update(getTime() - getLastJobBalkingTimePerClass(jobClass), 1.0);
			}
		}
		if (balkingRate != null) {
			balkingRate.update(getTime() - getLastJobBalkingTime(), 1.0);
		}
	}

	protected void updateRenegingRate(JobInfo jobInfo) {
		if (renegingRatePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = renegingRatePerClass[c];
			if (m != null) {
				m.update(getTime() - getLastJobRenegingTimePerClass(jobClass), 1.0);
			}
		}
		if (renegingRate != null) {
			renegingRate.update(getTime() - getLastJobRenegingTime(), 1.0);
		}
	}

	protected void updateRetrialAttemptsRate(JobInfo jobInfo) {
		if (retrialAttemptsRatePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = retrialAttemptsRatePerClass[c];
			if (m != null) {
				m.update(getTime() - getLastJobRetrialTimePerClass(jobClass), 1.0);
			}
		}
		if (retrialAttemptsRate != null) {
			retrialAttemptsRate.update(getTime() - getLastJobRetrialAttemptTime(), 1.0);
		}
	}

	protected void updateRetrialOrbitSize(Job job) {
		if (retrialOrbitSizePerClass != null) {
			JobClass jobClass = job.getJobClass();
			int c = jobClass.getId();
			Measure m = retrialOrbitSizePerClass[c];
			if (m != null) {
				m.update(retrialOrbitPerClass[c].size(), getTime() - getLastRetrialOrbitModifyTimePerClass(jobClass));
			}
		}
		if (retrialOrbitSize != null) {
			retrialOrbitSize.update(retrialOrbit.size(), getTime() - getLastRetrialOrbitModifyTime());
		}
	}

	protected void updateRetrialOrbitTime(Job job) {
		if (retrialOrbitTimePerClass != null) {
			JobClass jobClass = job.getJobClass();
			int c = jobClass.getId();
			Measure m = retrialOrbitTimePerClass[c];
			if (m != null) {
				m.update(getTime() - retrialOrbitPerClass[c].get(job.getId()).get(0), 1.0);
			}
		}
		if (retrialOrbitTime != null) {
			retrialOrbitTime.update(getTime() - retrialOrbit.get(job.getId()).get(0), 1.0);
		}
	}

	protected void updateResponseTimePerSink(JobInfo jobInfo) {
		if (responseTimePerSinkPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = responseTimePerSinkPerClass[c];
			if (m != null) {
				m.update(getTime() - jobInfo.getJob().getSystemEnteringTime(), 1.0);
			}
		}
		if (responseTimePerSink != null) {
			responseTimePerSink.update(getTime() - jobInfo.getJob().getSystemEnteringTime(), 1.0);
		}
	}

	protected void updateThroughputPerSink(JobInfo jobInfo) {
		if (throughputPerSinkPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			InverseMeasure m = throughputPerSinkPerClass[c];
			if (m != null) {
				m.update(getTime() - getLastJobOutTimePerClass(jobClass), 1.0);
			}
		}
		if (throughputPerSink != null) {
			throughputPerSink.update(getTime() - getLastJobOutTime(), 1.0);
		}
	}

	protected void updateNumberOfServers(){
		if(numberOfActiveServers != null) {
			numberOfActiveServers.update(activeServers, getTime() - getLastActiveServersTime());
		}
	}

	/**---------------------------------------------------------------------
	 *-------------------------- "OTHER" METHODS ---------------------------
	 *---------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void removeJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateResponseTimePerSink(jobInfo);
		updateThroughputPerSink(jobInfo);
		list.remove(jobInfo);
		listPerClass[c].remove(jobInfo);
		lastJobOutTime = getTime();
		lastJobOutTimePerClass[c] = getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#redirectJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void redirectJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		list.remove(jobInfo);
		listPerClass[c].remove(jobInfo);
		//the job has been redirected, so it should not be counted
		jobsIn--;
		jobsInPerClass[c]--;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#startJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void startJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateEffectiveUtilization(jobInfo);
		lastJobStartTime = getTime();
		lastJobStartTimePerClass[c] = getTime();
		jobsInService++;
		jobsInServicePerClass[c]++;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#endJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void endJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateEffectiveUtilization(jobInfo);
		lastJobEndTime = getTime();
		lastJobEndTimePerClass[c] = getTime();
		jobsInService--;
		jobsInServicePerClass[c]--;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#dropJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void dropJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateDropRate(jobInfo);
		list.remove(jobInfo);
		listPerClass[c].remove(jobInfo);
		lastJobDropTime = getTime();
		lastJobDropTimePerClass[c] = getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#switchJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void switchJob(JobInfo jobInfo) {
		int oc = jobInfo.getJob().getJobClass().getId();
		int nc = jobInfo.getJob().getNextJobClass().getId();
		updateQueueLength(jobInfo);
		updateResponseTime(jobInfo);
		updateResidenceTime(jobInfo);
		updateUtilization(jobInfo);
		updateUtilizationJoin(jobInfo);
		updateTardiness(jobInfo);
		updateEarliness(jobInfo);
		updateLateness(jobInfo);
		listPerClass[oc].remove(jobInfo);
		listPerClass[nc].add(jobInfo);
		lastJobSwitchTime = getTime();
		lastJobSwitchTimePerClass[oc] = getTime();
		totalSojournTime += getTime() - jobInfo.getEnteringTime();
		totalSojournTimePerClass[oc] += getTime() - jobInfo.getEnteringTime();
		lastJobSojournTime += getTime() - jobInfo.getEnteringTime();
		lastJobSojournTimePerClass[oc] += getTime() - jobInfo.getEnteringTime();
		jobInfo.resetEnteringTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#balkJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void balkJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateBalkingRate(jobInfo);
		list.remove(jobInfo);
		listPerClass[c].remove(jobInfo);
		lastJobBalkingTime = getTime();
		lastJobBalkingTimePerClass[c] = getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#renegeJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void renegeJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateQueueLength(jobInfo);
		updateRenegingRate(jobInfo);
		list.remove(jobInfo);
		listPerClass[c].remove(jobInfo);
		lastJobRenegingTime = getTime();
		lastJobRenegingTimePerClass[c] = getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#retryJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void retryJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateRetrialAttemptsRate(jobInfo);
		if (lookFor(jobInfo.getJob()) == null) {
			list.add(jobInfo);
		}
		lastJobRetrialAttemptTime = getTime();
		lastJobRetrialAttemptTimePerClass[c] = getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#produceJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void produceJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		list.add(jobInfo);
		listPerClass[c].add(jobInfo);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#consumeJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void consumeJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		list.remove(jobInfo);
		listPerClass[c].remove(jobInfo);
	}

	public void changeNumberOfActiveServers(int numberOfServers){
		updateNumberOfServers();
		activeServers = numberOfServers;
		lastActiveServersTime = getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#setNumberOfServers(int)
	 */
	public void setNumberOfServers(int numberOfServers) {
		this.numberOfServers = numberOfServers;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#setNetSystem(netSystem)
	 */
	public void setNetSystem(NetSystem netSystem) {
		this.netSystem = netSystem;
	}

	public void setRegionSoftDeadlinesByClass(double[] regionSoftDeadlinesByClass) {
		this.regionSoftDeadlinesByClass = regionSoftDeadlinesByClass;
	}

	protected double getTime() {
		return netSystem.getTime();
	}

	/***************************************
	 *  Method to calculate cache hit rate
	 ****************************************/
	@Override
	public void analyzeCacheHitRate(JobClass jobClass, Measure measurement){
		if (jobClass != null) {
			if (hitRatePerClass == null) {
				hitRatePerClass = new Measure[numberOfJobClasses];
			}
			hitRatePerClass[jobClass.getId()] = measurement;
		} else {
			hitRate = measurement;
		}
	}
	@Override
	public int getJobsTotalCacheMissCount() { return jobsTotalCacheMissCount; }

	@Override
	public int getJobsTotalCacheHitCount() { return jobsTotalCacheHitCount; }

	@Override
	public int getJobsCacheCountPerClass(JobClass jclass) { return jobsCacheCountPerClass[jclass.getId()]; }


	protected void updateCacheHitRate(JobClass jclass){
		// To Make sure the input value always be a positive value, to avoid NaN measure reuslt.
		if(jobsCacheCountPerClass[jclass.getId()]==0 || jobsCacheCountPerClass[jclass.getCachePairClass().getId()]==0)
			return;
		if(jobsTotalCacheHitCount==0 || jobsTotalCacheMissCount==0)
			return;

		if (hitRatePerClass != null) {

			// we make the cache class as a pair of hit and miss,
			// if a class 'isCacheHit = true', the opposite class connected to this class is cache Miss class.
			// but usually, cacheHit class is used as the input jClass in Measure.
			int targetClassId = jclass.getId();
			int pairClassId = jclass.getCachePairClass().getId();
			double hitRate = 0.0;

			if(jclass.isCacheHit()){
				hitRate = (double)jobsCacheCountPerClass[targetClassId] /
						(jobsCacheCountPerClass[targetClassId] + jobsCacheCountPerClass[pairClassId]);
			}
			else {
				hitRate = (double)jobsCacheCountPerClass[pairClassId] /
						(double)(jobsCacheCountPerClass[targetClassId] + jobsCacheCountPerClass[pairClassId]);
			}

			Measure m = hitRatePerClass[targetClassId];
			if (m != null) {
				m.update(hitRate, 1.0);
			}
		}
		if (hitRate != null) {
			double tempHitRate = (double)getJobsTotalCacheHitCount()/
					(double) (getJobsTotalCacheHitCount()+getJobsTotalCacheMissCount());
			hitRate.update(tempHitRate, 1.0);
		}
	}

	@Override
	public void CacheJob(JobClass jclass, boolean isHit){
		updateCacheHitRate(jclass);
		if(isHit){
			jobsTotalCacheHitCount++;
		}
		else {
			jobsTotalCacheMissCount++;
		}
		// we divide the hit and miss into two class, so only add to the classID corresponding class.
		jobsCacheCountPerClass[jclass.getId()]++;
	}
}
