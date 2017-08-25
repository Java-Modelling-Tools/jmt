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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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

	protected int numberOfJobClasses;

	//contain JobInfo objects
	protected LinkedList<JobInfo> list;

	protected LinkedList<JobInfo> listPerClass[];

	//arrivals and completions
	protected int jobsIn;

	protected int jobsInPerClass[];

	protected int jobsOut;

	protected int jobsOutPerClass[];

	protected double busyTime;

	protected double busyTimePerClass[];

	protected double lastJobOutTime;

	protected double lastJobOutTimePerClass[];

	protected double lastJobInTime;

	protected double lastJobInTimePerClass[];

	protected double lastJobDropTime;

	protected double lastJobDropTimePerClass[];

	protected Measure queueLength;

	protected Measure queueLengthPerClass[];

	protected Measure responseTime;

	protected Measure responseTimePerClass[];

	protected Measure residenceTime;

	protected Measure residenceTimePerClass[];

	protected Measure utilization;

	protected Measure utilizationPerClass[];

	protected Measure utilizationJoin;

	protected Measure utilizationPerClassJoin[];

	protected InverseMeasure throughput;

	protected InverseMeasure throughputPerClass[];

	protected InverseMeasure dropRate;

	protected InverseMeasure dropRatePerClass[];

	protected Measure responseTimePerSink;

	protected Measure responseTimePerSinkPerClass[];

	protected InverseMeasure throughputPerSink;

	protected InverseMeasure throughputPerSinkPerClass[];

	/** The number of servers to estimate Utilization measure on multiserver environments */
	protected int numberOfServers = 1;

	/**
	 * Creates a new JobInfoList instance.
	 * @param numberOfJobClasses number of job classes.
	 */
	@SuppressWarnings("unchecked")
	public LinkedJobInfoList(int numberOfJobClasses) {
		this.numberOfJobClasses = numberOfJobClasses;
		list = new LinkedList<JobInfo>();
		listPerClass = new LinkedList[numberOfJobClasses];
		for (int i = 0; i < numberOfJobClasses; i++) {
			listPerClass[i] = new LinkedList<JobInfo>();
		}
		jobsInPerClass = new int[numberOfJobClasses];
		jobsOutPerClass = new int[numberOfJobClasses];
		busyTimePerClass = new double[numberOfJobClasses];
		lastJobInTimePerClass = new double[numberOfJobClasses];
		lastJobOutTimePerClass = new double[numberOfJobClasses];
		lastJobDropTimePerClass = new double[numberOfJobClasses];
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
	 * @see jmt.engine.QueueNet.JobInfoList#getBusyTime()
	 */
	public double getBusyTime() {
		return busyTime;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getBusyTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getBusyTimePerClass(JobClass jobClass) {
		return busyTimePerClass[jobClass.getId()];
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
	 * @see jmt.engine.QueueNet.JobInfoList#getLastModifyTime()
	 */
	public double getLastModifyTime() {
		if (lastJobOutTime >= lastJobInTime && lastJobOutTime >= lastJobDropTime) {
			return lastJobOutTime;
		} else if (lastJobInTime >= lastJobOutTime && lastJobInTime >= lastJobDropTime) {
			return lastJobInTime;
		} else {
			return lastJobDropTime;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#getLastModifyTimePerClass(jmt.engine.QueueNet.JobClass)
	 */
	public double getLastModifyTimePerClass(JobClass jobClass) {
		if (lastJobOutTimePerClass[jobClass.getId()] >= lastJobInTimePerClass[jobClass.getId()]
				&& lastJobOutTimePerClass[jobClass.getId()] >= lastJobDropTimePerClass[jobClass.getId()]) {
			return lastJobOutTimePerClass[jobClass.getId()];
		} else if (lastJobInTimePerClass[jobClass.getId()] >= lastJobOutTimePerClass[jobClass.getId()]
				&& lastJobInTimePerClass[jobClass.getId()] >= lastJobDropTimePerClass[jobClass.getId()]) {
			return lastJobInTimePerClass[jobClass.getId()];
		} else {
			return lastJobDropTimePerClass[jobClass.getId()];
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#lookFor(jmt.engine.QueueNet.Job)
	 */
	public JobInfo lookFor(Job job) {
		ListIterator<JobInfo> it = list.listIterator();
		JobInfo jobInfo = null;
		while (it.hasNext()) {
			jobInfo = it.next();
			if (jobInfo.getJob() == job) {
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
		updateUtilization(jobInfo);
		updateUtilizationJoin(jobInfo);
		jobsIn++;
		jobsInPerClass[c]++;
		lastJobInTime = NetSystem.getTime();
		lastJobInTimePerClass[c] = NetSystem.getTime();
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#remove(jmt.engine.QueueNet.JobInfo)
	 */
	public void remove(JobInfo jobInfo) {
		doRemove(jobInfo, 0, 0);
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeFirst()
	 */
	public JobInfo removeFirst() {
		JobInfo jobInfo = list.getFirst();
		if (jobInfo != null) {
			doRemove(jobInfo, 1, 1);
			return jobInfo;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeFirst(jmt.engine.QueueNet.JobClass)
	 */
	public JobInfo removeFirst(JobClass jobClass) {
		int c = jobClass.getId();
		JobInfo jobInfo = listPerClass[c].getFirst();
		if (jobInfo != null) {
			doRemove(jobInfo, 0, 1);
			return jobInfo;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeLast()
	 */
	public JobInfo removeLast() {
		JobInfo jobInfo = list.getLast();
		if (jobInfo != null) {
			doRemove(jobInfo, 2, 2);
			return jobInfo;
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#removeLast(jmt.engine.QueueNet.JobClass)
	 */
	public JobInfo removeLast(JobClass jobClass) {
		int c = jobClass.getId();
		JobInfo jobInfo = listPerClass[c].getLast();
		if (jobInfo != null) {
			doRemove(jobInfo, 0, 2);
			return jobInfo;
		} else {
			return null;
		}
	}

	protected void doRemove(JobInfo jobInfo, int position, int perClassPosition) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateQueueLength(jobInfo);
		updateResidenceTime(jobInfo);
		updateResponseTime(jobInfo);
		updateUtilization(jobInfo);
		updateUtilizationJoin(jobInfo);
		updateThroughput(jobInfo);
		finalRemove(jobInfo, list, position);
		finalRemove(jobInfo, listPerClass[c], perClassPosition);
		jobsOut++;
		jobsOutPerClass[c]++;
		lastJobOutTime = NetSystem.getTime();
		lastJobOutTimePerClass[c] = NetSystem.getTime();
		busyTime += NetSystem.getTime() - jobInfo.getEnteringTime();
		busyTimePerClass[c] += NetSystem.getTime() - jobInfo.getEnteringTime();
	}

	protected void finalRemove(JobInfo what, LinkedList<JobInfo> list, int position) {
		switch (position) {
		case 1:
			list.removeFirst();
			break;
		case 2:
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
	 * @see jmt.engine.QueueNet.JobInfoList#analyzeResponseTimePerSink(jmt.engine.QueueNet.JobClass, jmt.engine.dataAnalysis.InverseMeasure)
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

	protected void updateQueueLength(JobInfo jobInfo) {
		if (queueLengthPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = queueLengthPerClass[c];
			if (m != null) {
				m.update(listPerClass[c].size(), NetSystem.getTime() - getLastModifyTimePerClass(jobClass));
			}
		}
		if (queueLength != null) {
			queueLength.update(list.size(), NetSystem.getTime() - getLastModifyTime());
		}
	}

	protected void updateResponseTime(JobInfo jobInfo) {
		if (responseTimePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = responseTimePerClass[c];
			if (m != null) {
				m.update(NetSystem.getTime() - jobInfo.getEnteringTime(), 1.0);
			}
		}
		if (responseTime != null) {
			responseTime.update(NetSystem.getTime() - jobInfo.getEnteringTime(), 1.0);
		}
	}

	protected void updateResidenceTime(JobInfo jobInfo) {
		if (residenceTimePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = residenceTimePerClass[c];
			if (m != null) {
				m.update(NetSystem.getTime() - jobInfo.getEnteringTime(), 1.0);
			}
		}
		if (residenceTime != null) {
			residenceTime.update(NetSystem.getTime() - jobInfo.getEnteringTime(), 1.0);
		}
	}

	protected void updateUtilization(JobInfo jobInfo) {
		if (utilizationPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = utilizationPerClass[c];
			if (m != null) {
				m.update(listPerClass[c].size() / numberOfServers, NetSystem.getTime() - getLastModifyTimePerClass(jobClass));
			}
		}
		if (utilization != null) {
			utilization.update(list.size() / numberOfServers, NetSystem.getTime() - getLastModifyTime());
		}
	}

	protected void updateUtilizationJoin(JobInfo jobInfo) {
		if (utilizationPerClassJoin != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = utilizationPerClassJoin[c];
			if (m != null) {
				if (listPerClass[c].size() > 0) {
					m.update(1.0, NetSystem.getTime() - getLastModifyTimePerClass(jobClass));
				} else {
					m.update(0.0, NetSystem.getTime() - getLastModifyTimePerClass(jobClass));
				}
			}
		}
		if (utilizationJoin != null) {
			if (list.size() > 0) {
				utilizationJoin.update(1.0, NetSystem.getTime() - getLastModifyTime());
			} else {
				utilizationJoin.update(0.0, NetSystem.getTime() - getLastModifyTime());
			}
		}
	}

	protected void updateThroughput(JobInfo jobInfo) {
		if (throughputPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = throughputPerClass[c];
			if (m != null) {
				m.update(NetSystem.getTime() - getLastJobOutTimePerClass(jobClass), 1.0);
			}
		}
		if (throughput != null) {
			throughput.update(NetSystem.getTime() - getLastJobOutTime(), 1.0);
		}
	}

	protected void updateDropRate(JobInfo jobInfo) {
		if (dropRatePerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = dropRatePerClass[c];
			if (m != null) {
				m.update(NetSystem.getTime() - getLastJobDropTimePerClass(jobClass), 1.0);
			}
		}
		if (dropRate != null) {
			dropRate.update(NetSystem.getTime() - getLastJobDropTime(), 1.0);
		}
	}

	public void updateResponseTimePerSink(JobInfo jobInfo) {
		if (responseTimePerSinkPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			Measure m = responseTimePerSinkPerClass[c];
			if (m != null) {
				m.update(NetSystem.getTime() - jobInfo.getJob().getSystemEnteringTime(), 1.0);
			}
		}
		if (responseTimePerSink != null) {
			responseTimePerSink.update(NetSystem.getTime() - jobInfo.getJob().getSystemEnteringTime(), 1.0);
		}
	}

	public void updateThroughputPerSink(JobInfo jobInfo) {
		if (throughputPerSinkPerClass != null) {
			JobClass jobClass = jobInfo.getJob().getJobClass();
			int c = jobClass.getId();
			InverseMeasure m = throughputPerSinkPerClass[c];
			if (m != null) {
				m.update(NetSystem.getTime() - getLastJobOutTimePerClass(jobClass), 1.0);
			}
		}
		if (throughputPerSink != null) {
			throughputPerSink.update(NetSystem.getTime() - getLastJobOutTime(), 1.0);
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
		lastJobOutTime = NetSystem.getTime();
		lastJobOutTimePerClass[c] = NetSystem.getTime();
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
	 * @see jmt.engine.QueueNet.JobInfoList#dropJob(jmt.engine.QueueNet.JobInfo)
	 */
	public void dropJob(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		updateDropRate(jobInfo);
		list.remove(jobInfo);
		listPerClass[c].remove(jobInfo);
		lastJobDropTime = NetSystem.getTime();
		lastJobDropTimePerClass[c] = NetSystem.getTime();
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

	/* (non-Javadoc)
	 * @see jmt.engine.QueueNet.JobInfoList#setNumberOfServers(int)
	 */
	public void setNumberOfServers(int numberOfServers) {
		this.numberOfServers = numberOfServers;
	}

}
