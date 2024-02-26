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

import java.util.List;
import java.util.Map;

import jmt.engine.NodeSections.Server;
import jmt.engine.dataAnalysis.InverseMeasure;
import jmt.engine.dataAnalysis.Measure;

/**
 * <p><b>Name:</b> JobInfoList</p>
 * <p><b>Description:</b>
 * A JobInfoList is a list used to update raw sampled performance counters of a node or section.
 * </p>
 * <p><b>Date:</b> 18/giu/2009
 * <b>Time:</b> 17:00:44</p>
 * @author Bertoli Marco
 * @version 1.0
 *
 * Modified by Ashanka (May 2010):
 * Patch: Multi-Sink Perf. Index
 * Description: Added new Performance index for capturing
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public interface JobInfoList {

	/**
	 * Gets the number of jobs.
	 * @return Number of jobs.
	 */
	public abstract int size();

	/**
	 * Gets the number of jobs of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Number of jobs of the specified job class.
	 */
	public abstract int size(JobClass jobClass);

	/**
	 * Gets the retrial orbit.
	 * @return Reference to the retrial orbit.
	 */
	public abstract Map<Integer, List<Double>> getRetrialOrbit();

	/**
	 * Adds a job to the retrial orbit.
	 * @param job Reference to the job.
	 */
	public abstract void addToRetrialOrbit(Job job);

	/**
	 * Removes a job from the retrial orbit.
	 * @param job Reference to the job.
	 */
	public abstract void removeFromRetrialOrbit(Job job);

	/**
	 * Gets the number of added jobs.
	 * @return Number of added jobs.
	 */
	public abstract int getJobsIn();

	/**
	 * Gets the number of added jobs of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Number of added jobs of the specified job class.
	 */
	public abstract int getJobsInPerClass(JobClass jobClass);

	/**
	 * Gets the number of removed jobs.
	 * @return Number of removed jobs.
	 */
	public abstract int getJobsOut();

	/**
	 * Gets the number of removed jobs of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Number of removed jobs of the specified job class.
	 */
	public abstract int getJobsOutPerClass(JobClass jobClass);

	/**
	 * Gets the number of jobs in service.
	 * @return Number of jobs in service.
	 */
	public abstract int getJobsInService();

	/**
	 * Gets the number of jobs in service of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Number of jobs in service of the specified job class.
	 */
	public abstract int getJobsInServicePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last added job.
	 * @return Time of the last added job.
	 */
	public abstract double getLastJobInTime();

	/**
	 * Gets the time of the last added job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last added job of the specified job class.
	 */
	public abstract double getLastJobInTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last removed job.
	 * @return Time of the last removed job.
	 */
	public abstract double getLastJobOutTime();

	/**
	 * Gets the time of the last removed job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last removed job of the specified job class.
	 */
	public abstract double getLastJobOutTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last started job.
	 * @return Time of the last started job.
	 */
	public abstract double getLastJobStartTime();

	/**
	 * Gets the time of the last started job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last started job of the specified job class.
	 */
	public abstract double getLastJobStartTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last ended job.
	 * @return Time of the last ended job.
	 */
	public abstract double getLastJobEndTime();

	/**
	 * Gets the time of the last ended job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last ended job of the specified job class.
	 */
	public abstract double getLastJobEndTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last dropped job.
	 * @return Time of the last dropped job.
	 */
	public abstract double getLastJobDropTime();

	/**
	 * Gets the time of the last dropped job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last dropped job of the specified job class.
	 */
	public abstract double getLastJobDropTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last switched job.
	 * @return Time of the last switched job.
	 */
	public abstract double getLastJobSwitchTime();

	/**
	 * Gets the time of the last switched job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last switched job of the specified job class.
	 */
	public abstract double getLastJobSwitchTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last balked job.
	 * @return Time of the last balked job.
	 */
	public abstract double getLastJobBalkingTime();

	/**
	 * Gets the time of the last balked job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last balked job of the specified job class.
	 */
	public abstract double getLastJobBalkingTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last reneged job.
	 * @return Time of the last reneged job.
	 */
	public abstract double getLastJobRenegingTime();

	/**
	 * Gets the time of the last reneged job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last reneged job of the specified job class.
	 */
	public abstract double getLastJobRenegingTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last retried job.
	 * @return Time of the last retried job.
	 */
	public abstract double getLastJobRetrialAttemptTime();

	/**
	 * Gets the time of the last retried job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last retried job of the specified job class.
	 */
	public abstract double getLastJobRetrialTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last modify.
	 * @return Time of the last modify.
	 */
	public abstract double getLastModifyTime();

	/**
	 * Gets the time of the last modify of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last modify of the specified job class.
	 */
	public abstract double getLastModifyTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last service modify.
	 * @return Time of the last modify.
	 */
	public abstract double getLastServiceModifyTime();

	/**
	 * Gets the time of the last service modify of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last service modify of the specified job class.
	 */
	public abstract double getLastServiceModifyTimePerClass(JobClass jobClass);

	/**
	 * Gets the time of the last retrial orbit modify.
	 * @return Time of the last retrial orbit modify.
	 */
	public abstract double getLastRetrialOrbitModifyTime();

	/**
	 * Gets the time of the last retrial orbit modify of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Time of the last retrial orbit modify of the specified job class.
	 */
	public abstract double getLastRetrialOrbitModifyTimePerClass(JobClass jobClass);

	/**
	 * Gets the total sojourn time.
	 * @return Total sojourn time.
	 */
	public abstract double getTotalSojournTime();

	/**
	 * Gets the total sojourn time of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Total sojourn time of the specified job class.
	 */
	public abstract double getTotalSojournTimePerClass(JobClass jobClass);

	/**
	 * Gets the sojourn time of the last removed job.
	 * @return Sojourn time of the last removed job.
	 */
	public abstract double getLastJobSojournTime();

	/**
	 * Gets the sojourn time of the last removed job of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Sojourn time of the last removed job of the specified job class.
	 */
	public abstract double getLastJobSojournTimePerClass(JobClass jobClass);

	/**
	 * Looks for the job info that references the specified job.
	 * @param job Specified job.
	 * @return Job info that references the specified job.
	 */
	public abstract JobInfo lookFor(Job job);

	/**
	 * Gets the internal job info list.
	 * @return Internal job info list.
	 */
	public abstract List<JobInfo> getInternalJobInfoList();

	/**
	 * Gets the internal job info list of the specified job class.
	 * @param jobClass Specified job class.
	 * @return Internal job info list of the specified job class.
	 */
	public abstract List<JobInfo> getInternalJobInfoList(JobClass jobClass);

	/**
	 * Adds a new job info to the list.
	 * @param jobInfo Reference to the job info to be added.
	 */
	public abstract void add(JobInfo jobInfo);

	/**
	 * Adds a new job info at the start of the list.
	 * @param jobInfo Reference to the job info to be added.
	 */
	public abstract void addFirst(JobInfo jobInfo);

	/**
	 * Adds a new job info at the end of the list.
	 * @param jobInfo Reference to the job info to be added.
	 */
	public abstract void addLast(JobInfo jobInfo);

	/**
	 * Adds a new job info at the specified index.
	 * @param index Specified index.
	 * @param jobInfo Reference to the job info to be added.
	 */
	public abstract void add(int index, JobInfo jobInfo);

	/**
	 * Adds a new job info at the specified index.
	 * @param index Specified index.
	 * @param jobInfo Reference to the job info to be added.
	 * @param isPerClassHead If true, the job info will be put at the start of the
	 * per-class list. Otherwise, it will be put at the end.
	 */
	public abstract void add(int index, JobInfo jobInfo, boolean isPerClassHead);

	/**
	 * Removes a job info from the list.
	 * @param jobInfo Reference to the job info to be removed.
	 */
	public abstract void remove(JobInfo jobInfo);

	/**
	 * Removes a job info from the list.
	 * @param jobInfo Reference to the job info to be removed.
	 */
	public abstract void removeOnly(JobInfo jobInfo);

	/**
	 * Removes a job info at the start of the list.
	 * @return Job info at the start of the list.
	 */
	public abstract JobInfo removeFirst();

	/**
	 * Removes a job info of the specified job class at the start of the list.
	 * @param jobClass Specified job class.
	 * @return Job info of the specified job class at the start of the list.
	 */
	public abstract JobInfo removeFirst(JobClass jobClass);

	/**
	 * Removes a job info at the end of the list.
	 * @return Job info at the end of the list.
	 */
	public abstract JobInfo removeLast();

	/**
	 * Removes a job info of the specified job class at the end of the list.
	 * @param jobClass Specified job class.
	 * @return Job info of the specified job class at the end of the list.
	 */
	public abstract JobInfo removeLast(JobClass jobClass);


	//Michalis

	JobInfo getFirstJob();

	JobInfo getLastJob();

	/**
	 * Returns ith job in queue
	 * @param i - indec
	 */
	public JobInfo getJob(int i);

	/**
	 * Adds serverTypes used for a job
	 * @param job_id
	 * @param busyServers
	 */
	public void addUsedServersForJob(int job_id, List<Server.BusyServer> busyServers);

	/**
	 * Removes job when finished
	 * @param job_id
	 */
	public void removeUsedServerForJob(int job_id);

	//

	/**
	 * Analyzes the queue length for the specified job class.
	 * jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeQueueLength(JobClass jobClass, Measure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the response time for the specified job class.
	 * jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeResponseTime(JobClass jobClass, Measure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the residence time for the specified job class.
	 * jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeResidenceTime(JobClass jobClass, Measure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the arrival rate for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated inverse measure.
	 */
	public abstract void analyzeArrivalRate(JobClass jobClass, InverseMeasure measurement);

	/**
	 * Analyzes the throughput for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated inverse measure.
	 */
	public abstract void analyzeThroughput(JobClass jobClass, InverseMeasure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the utilization for the specified job class.
	 * jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeUtilization(JobClass jobClass, Measure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the utilization of a join for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeUtilizationJoin(JobClass jobClass, Measure measurement);

	/**
	 * Analyzes the effective utilization for the specified job class.
	 * jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeEffectiveUtilization(JobClass jobClass, Measure measurement);

	/**
	 * Analyzes the drop rate for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated inverse measure.
	 */
	public abstract void analyzeDropRate(JobClass jobClass, InverseMeasure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the balking rate for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated inverse measure.
	 */
	public abstract void analyzeBalkingRate(JobClass jobClass, InverseMeasure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the reneging rate for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated inverse measure.
	 */
	public abstract void analyzeRenegingRate(JobClass jobClass, InverseMeasure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the retrial attempts rate for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated inverse measure.
	 */
	public abstract void analyzeRetrialAttemptsRate(JobClass jobClass, InverseMeasure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the retrial orbit size for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeRetrialOrbitSize(JobClass jobClass, Measure measurement, boolean isServerType, int serverTypeId);

	/**
	 * Analyzes the retrial orbit time for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeRetrialOrbitTime(JobClass jobClass, Measure measurement);

	/**
	 * Analyzes the response time of a sink for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated inverse measure.
	 */
	public abstract void analyzeResponseTimePerSink(JobClass jobClass, Measure measurement);

	/**
	 * Analyzes the throughput of a sink for the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated inverse measure.
	 */
	public abstract void analyzeThroughputPerSink(JobClass jobClass, InverseMeasure measurement);

	/**
	 * Analyzes the tardiness of the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeTardiness(JobClass jobClass, Measure measurement);

	/**
	 * Analyzes the earliness of the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeEarliness(JobClass jobClass, Measure measurement);

	/**
	 * Analyzes the lateness of the specified job class.
	 * @param jobClass Specified job class. If null, analysis will be job class independent.
	 * @param measurement Reference to the associated measure.
	 */
	public abstract void analyzeLateness(JobClass jobClass, Measure measurement);

	/**
	 * Analyzes the number of active servers in a Server section
	 * @param measurement Reference to the associated measure
	 */
	public abstract void analyzeNumberOfServers(Measure measurement);


	/**
	 * Removes a job info from the list when a job is removed.
	 * @param jobInfo Reference to the job info to be removed.
	 */
	public abstract void removeJob(JobInfo jobInfo);

	/**
	 * Removes a job info from the list when a job is redirected.
	 * @param jobInfo Reference to the job info to be removed.
	 */
	public abstract void redirectJob(JobInfo jobInfo);

	/**
	 * Changes a job info in the list when a job is started.
	 * @param jobInfo Reference to the job info to be changed.
	 */
	public abstract void startJob(JobInfo jobInfo);

	/**
	 * Changes a job info from the list when a job is ended.
	 * @param jobInfo Reference to the job info to be changed.
	 */
	public abstract void endJob(JobInfo jobInfo);

	/**
	 * Removes a job info from the list when a job is dropped.
	 * @param jobInfo Reference to the job info to be removed.
	 */
	public abstract void dropJob(JobInfo jobInfo);

	/**
	 * Changes a job info in the list when a job is switched.
	 * @param jobInfo Reference to the job info to be changed.
	 */
	public abstract void switchJob(JobInfo jobInfo);

	/**
	 * Removes a job info from the list when a job is balked.
	 * @param jobInfo Reference to the job info to be removed.
	 */
	public abstract void balkJob(JobInfo jobInfo);

	/**
	 * Removes a job info from the list when a job is reneged.
	 * @param jobInfo Reference to the job info to be removed.
	 */
	public abstract void renegeJob(JobInfo jobInfo);

	/**
	 * Adds a job info to the list when a job is retried.
	 * @param jobInfo Reference to the job info to be added.
	 */
	public abstract void retryJob(JobInfo jobInfo);

	/**
	 * Adds a job info to the list when a job is consumed.
	 * @param jobInfo Reference to the job info to be added.
	 */
	public abstract void produceJob(JobInfo jobInfo);

	/**
	 * Removes a job info from the list when a job is consumed.
	 * @param jobInfo Reference to the job info to be removed.
	 */
	public abstract void consumeJob(JobInfo jobInfo);

	/**
	 * Changes the number of active servers when the enabling degree changes
	 */
	public abstract void changeNumberOfActiveServers(int numberOfServers);

	/**
	 * Sets the number of servers. This parameter is used to scale utilization.
	 * @param numberOfServers Number of servers.
	 */
	public abstract void setNumberOfServers(int numberOfServers);

	/**
	 * Sets the number of servers required by a class. This parameter is used to scale utilization.
	 * @param serverNumRequired Number of servers required by a specific class, getting by id.
	 */
	public void setServerNumRequired(int[] serverNumRequired);

	/**
	 * Sets the network system. This parameter is used to get simulation time.
	 * @param netSystem Network system.
	 */
	public abstract void setNetSystem(NetSystem netSystem);

	/**
	 * Sets the region soft deadlines for each class to be used by tardiness measures.
	 * This method should only be called when the relevant station is a blocking region input station.
	 * @param regionSoftDeadlinesByClass Soft deadlines for each class.
	 */
	public abstract void setRegionSoftDeadlinesByClass(double[] regionSoftDeadlinesByClass);


	/**
	 *  Methods to calculate cache performance measures
	 */
	public void analyzeCacheHitRate(JobClass jobClass, Measure measurement);
	public int getJobsTotalCacheMissCount();
	public int getJobsTotalCacheHitCount();
	public int getJobsCacheCountPerClass(JobClass jclass);
	public void CacheJob(JobClass jclass, boolean isHit);


}