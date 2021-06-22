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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import jmt.engine.dataAnalysis.InverseMeasure;
import jmt.engine.dataAnalysis.Measure;

/**
 * <p>Title: Global Job Info List</p>
 * <p>Description: This class provides a global "job info list" to be used during
 * simulation to compute global measures. This is designed to be associated to a
 * simulation object.</p>
 *
 * @author Bertoli Marco
 *         Date: 8-mar-2006
 *         Time: 12.27.42
 *         
 *  Modified by Ashanka (Feb 2010)
 *  Desc: Modified the logic of System Power calculation. Earlier the logic was to capture the 
 *  	  response time (rn) of each job when it was entering the sink and to capture the time 
 *  	  of throughput (xn) for each job. Then x1/r1 ... x2/r2 ... x3/r3 ...
 *        was sent for System Power Simulation Engine. This logic was modified into:
 *        x1/r1 .. [x1/r1 + (x1+x2)/(r1+r2)] .. [x1/r1 + (x1+x2)/(r1+r2) + (x1+x2+x3)/(r1+r2+r3)] ..
 */
public class GlobalJobInfoList {

	private int classNum;
	private boolean hasClassSwitch;

	private double lastJobOutTime;
	private double[] lastJobOutTimePerClass;

	private double lastJobDropTime;
	private double[] lastJobDropTimePerClass;

	private double lastJobRenegeTime;
	private double[] lastJobRenegeTimePerClass;

	private double lastJobBalkTime;
	private double[] lastJobBalkTimePerClass;

	private double lastJobRetrialTime;
	private double[] lastJobRetrialTimePerClass;
	private double lastModifyNumber;
	private double[] lastModifyNumberPerClass;

	private int jobs;
	private int[] jobsPerClass;

	private Measure jobNum;
	private Measure[] jobNumPerClass;

	private Measure responseTime;
	private Measure[] responseTimePerClass;

	private InverseMeasure dropRate;
	private InverseMeasure[] dropRatePerClass;

	private InverseMeasure renegingRate;
	private InverseMeasure[] renegingRatePerClass;

	private InverseMeasure balkingRate;
	private InverseMeasure[] balkingRatePerClass;

	private InverseMeasure retrialAttemptsRate;
	private InverseMeasure[] retrialAttemptsRatePerClass;
	private InverseMeasure throughput;
	private InverseMeasure[] throughputPerClass;

	private InverseMeasure systemPower;
	private InverseMeasure[] systemPowerPerClass;

	//Variables for the System Power calculation: New Modified version.
	double systemPowerSamples;
	double systemPowerSamplesClass[];

	double sampling_SystemResponseSum;
	double sampling_SystemThroughputSum;
	double samplingClass_SystemThroughputSum[];
	
	private Set<Pair<NetNode, JobClass>> pairSet;
	private Set<Pair<Pair<NetNode, JobClass>, Pair<NetNode, JobClass>>> linkSet;
	private Set<Set<Pair<NetNode, JobClass>>> chainSet; 
	private Map<Pair<NetNode, JobClass>, Set<Pair<NetNode, JobClass>>> pairChainMap;
	private Map<Set<Pair<NetNode, JobClass>>, Integer> visitCountPerChain;
	private Integer totalVisitCount;
	
	private NetSystem netSystem;

	/**
	 * Creates a new GlobalJobInfoList
	 * @param classNum number of classes in current network model
	 */
	public GlobalJobInfoList(int classNum) {
		initialize(classNum);
	}

	/**
	 * Resets this info list.
	 * @param classNum number of classes in current network model.
	 */
	private void initialize(int classNum) {
		this.classNum = classNum;
		lastJobOutTime = 0.0;
		lastJobOutTimePerClass = new double[classNum];
		lastJobDropTime = 0.0;
		lastJobDropTimePerClass = new double[classNum];
		lastJobRenegeTime = 0.0;
		lastJobRenegeTimePerClass = new double[classNum];
		lastJobBalkTime = 0.0;
		lastJobBalkTimePerClass = new double[classNum];
		lastJobRetrialTime = 0.0;
		lastJobRetrialTimePerClass = new double[classNum];
		lastModifyNumber = 0.0;
		lastModifyNumberPerClass = new double[classNum];
		jobs = 0;
		jobsPerClass = new int[classNum];

		jobNum = null;
		jobNumPerClass = null;
		responseTime = null;
		responseTimePerClass = null;
		dropRate = null;
		dropRatePerClass = null;
		renegingRate = null;
		renegingRatePerClass = null;
		retrialAttemptsRate = null;
		retrialAttemptsRatePerClass = null;
		throughput = null;
		throughputPerClass = null;
		systemPower = null;
		systemPowerPerClass = null;

		systemPowerSamples = 0.0;
		systemPowerSamplesClass = new double[classNum];

		sampling_SystemResponseSum = 0.0;
		sampling_SystemThroughputSum = 0.0;
		samplingClass_SystemThroughputSum = new double[classNum];

		pairSet = new HashSet<Pair<NetNode, JobClass>>();
		linkSet = new HashSet<Pair<Pair<NetNode, JobClass>, Pair<NetNode, JobClass>>>();
		chainSet = new HashSet<Set<Pair<NetNode, JobClass>>>();
		pairChainMap = new HashMap<Pair<NetNode, JobClass>, Set<Pair<NetNode, JobClass>>>();
		visitCountPerChain = new HashMap<Set<Pair<NetNode, JobClass>>, Integer>();
		totalVisitCount = Integer.valueOf(0);
	}

	// --- Methods to be called on job events ---------------------------------------------
	/**
	 * This method MUST be called each time a new job is added to the network.
	 * @param job identifier of added job.
	 */
	public void addJob(Job job) {
		job.reset();
		updateJobNumber(job);
		// Updates job number data structures
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = netSystem.getTime();
		jobs++;
		jobsPerClass[job.getJobClass().getId()]++;
	}

	/**
	 * This method MUST be called each time a job is removed from the network.
	 * @param job identifier of removed job.
	 */
	public void removeJob(Job job) {
		updateJobNumber(job);
		updateResponseTime(job);
		updateThroughput(job);
		updateSystemPower(job);
		updateVisitCountPerChain(job);

		// Updates jobs number and throughput data structures
		jobs--;
		jobsPerClass[job.getJobClass().getId()]--;
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = netSystem.getTime();
		lastJobOutTimePerClass[job.getJobClass().getId()] = lastJobOutTime = netSystem.getTime();
	}

	/**
	 * This method MUST be called each time a job cycles in its reference station.
	 * @param job identifier of cycling job.
	 */
	public void recycleJob(Job job) {
		updateJobNumber(job);
		updateResponseTime(job);
		updateThroughput(job);
		updateSystemPower(job);
		updateVisitCountPerChain(job);

		// Updates jobs number and throughput data structures
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = netSystem.getTime();
		lastJobOutTimePerClass[job.getJobClass().getId()] = lastJobOutTime = netSystem.getTime();

		job.reset();
	}

	/**
	 * This method must be called each time a job is dropped by a queue, a place or
	 * a blocking region.
	 * @param job identifier of dropped job.
	 */
	public void dropJob(Job job) {
		updateJobNumber(job);
		updateDropRate(job);

		// Updates jobs number and drop rate data structures
		jobs--;
		jobsPerClass[job.getJobClass().getId()]--;
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = getTime();
		lastJobDropTimePerClass[job.getJobClass().getId()] = lastJobDropTime = getTime();
	}

	/**
	 * This method MUST be called each time a job is produced by a transition.
	 * @param job identifier of produced job.
	 */
	public void renegeJob(Job job) {
		updateJobNumber(job);
		updateRenegingRate(job);

		// Updates jobs number and reneging rate data structures
		jobs--;
		jobsPerClass[job.getJobClass().getId()]--;
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = getTime();
		lastJobRenegeTimePerClass[job.getJobClass().getId()] = lastJobRenegeTime = getTime();
	}

	/**
	 * This method must be called each time a job balks from a queue
	 * @param job identifier of reneged job
	 */
	public void balkJob(Job job) {
		updateJobNumber(job);
		updateBalkingRate(job);
		// Updates jobs number and balking rate data structures
		jobs--;
		jobsPerClass[job.getJobClass().getId()]--;
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = getTime();

		lastJobBalkTimePerClass[job.getJobClass().getId()] = lastJobBalkTime = getTime();
	}

	/**
	 * This method must be called each time a job retries joining a queue
	 * @param job identifier of reneged job
	 */
	public void retryJob(Job job) {
		updateRetrialAttemptsRate(job);
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = getTime();
		lastJobRetrialTimePerClass[job.getJobClass().getId()] = lastJobRetrialTime = getTime();
	}
	public void produceJob(Job job) {
		updateJobNumber(job);
		// Updates job number data structure only
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = getTime();
		jobs++;
		jobsPerClass[job.getJobClass().getId()]++;
	}

	/**
	 * This method MUST be called each time a job is consumed by a transition.
	 * @param job identifier of consumed job.
	 */
	public void consumeJob(Job job) {
		updateJobNumber(job);
		// Updates job number data structure only
		jobs--;
		jobsPerClass[job.getJobClass().getId()]--;
		lastModifyNumberPerClass[job.getJobClass().getId()] = lastModifyNumber = getTime();
	}

	// ------------------------------------------------------------------------------------

	// --- Methods to specify measures to be analyzed -------------------------------------

	/**
	 * Analyzes System Number of Jobs for a specific job class or for every class.
	 * @param jobClass specified job class. If null measure will be job independent.
	 * @param Measure reference to a Measure object.
	 */
	public void analyzeJobNumber(JobClass jobClass, Measure Measure) {
		if (jobClass != null) {
			if (jobNumPerClass == null) {
				jobNumPerClass = new Measure[classNum];
			}
			jobNumPerClass[jobClass.getId()] = Measure;
		} else {
			jobNum = Measure;
		}
	}

	/**
	 * Analyzes System Response Time for a specific job class or for every class.
	 * @param jobClass specified job class. If null measure will be job independent.
	 * @param measure reference to a Measure object.
	 */
	public void analyzeResponseTime(JobClass jobClass, Measure measure) {
		if (jobClass != null) {
			if (responseTimePerClass == null) {
				responseTimePerClass = new Measure[classNum];
			}
			responseTimePerClass[jobClass.getId()] = measure;
		} else {
			responseTime = measure;
		}
	}

	/**
	 * Analyzes System Throughput for a specific job class or for every class.
	 * @param jobClass specified job class. If null measure will be job independent.
	 * @param Measure reference to a Measure object.
	 */
	public void analyzeThroughput(JobClass jobClass, Measure Measure) {
		if (jobClass != null) {
			if (throughputPerClass == null) {
				throughputPerClass = new InverseMeasure[classNum];
			}
			throughputPerClass[jobClass.getId()] = (InverseMeasure) Measure;
		} else {
			throughput = (InverseMeasure) Measure;
		}
	}

	/**
	 * Analyzes Drop Rate for a specific job class or for every class.
	 * @param jobClass specified job class. If null measure will be job independent.
	 * @param Measure reference to a Measure object.
	 */
	public void analyzeDropRate(JobClass jobClass, Measure Measure) {
		if (jobClass != null) {
			if (dropRatePerClass == null) {
				dropRatePerClass = new InverseMeasure[classNum];
			}
			dropRatePerClass[jobClass.getId()] = (InverseMeasure) Measure;
		} else {
			dropRate = (InverseMeasure) Measure;
		}
	}

	/**
	 * Analyzes System Power for a specific job class or for every class.
	 * @param jobClass specified job class. If null measure will be job independent.
	 * @param Measure reference to a Measure object.
	 */
	public void analyzeSystemPower(JobClass jobClass, Measure Measure) {
		if (jobClass != null) {
			if (systemPowerPerClass == null) {
				systemPowerPerClass = new InverseMeasure[classNum];
			}
			systemPowerPerClass[jobClass.getId()] = (InverseMeasure) Measure;
		} else {
			systemPower = (InverseMeasure) Measure;
		}
	}

	// ------------------------------------------------------------------------------------

	// --- Methods to update measures -----------------------------------------------------

	/**
	public void analyzeRenegingRate(JobClass jobClass, Measure Measure) {
		if (jobClass != null) {
			if (renegingRatePerClass == null) {
				renegingRatePerClass = new InverseMeasure[classNum];
			}
			renegingRatePerClass[jobClass.getId()] = (InverseMeasure) Measure;
		} else {
			renegingRate = (InverseMeasure) Measure;
		}
	}
    **/

	public void analyzeRenegingRate(JobClass jobClass, Measure Measure) {
		if (jobClass != null) {
			if (renegingRatePerClass == null) {
				renegingRatePerClass = new InverseMeasure[classNum];
			}
			renegingRatePerClass[jobClass.getId()] = (InverseMeasure) Measure;
		} else {
			renegingRate = (InverseMeasure) Measure;
		}
	}

	/**
	 * Analyzes Balking Rate for a specific job class or for every class
	 * @param jobClass specified job class. If null measure will be job independent
	 * @param Measure reference to a Measure object
	 */
	public void analyzeBalkingRate(JobClass jobClass, Measure Measure) {
		if (jobClass != null) {
			if (balkingRatePerClass == null) {
				balkingRatePerClass = new InverseMeasure[classNum];
			}
			balkingRatePerClass[jobClass.getId()] = (InverseMeasure) Measure;
		} else {
			balkingRate = (InverseMeasure) Measure;
		}
	}

	/**
	 * Analyzes System Number of retrials for a specific job class or for every class.
	 * @param jobClass specified job class. If null measure will be job independent.
	 * @param Measure reference to a Measure object.
	 */
	public void analyzeRetrialAttemptsRate(JobClass jobClass, Measure Measure) {
		if (jobClass != null) {
			if (retrialAttemptsRatePerClass == null) {
				retrialAttemptsRatePerClass = new InverseMeasure[classNum];
			}
			retrialAttemptsRatePerClass[jobClass.getId()] = (InverseMeasure) Measure;
		} else {
			retrialAttemptsRate = (InverseMeasure) Measure;
		}
	}
    /*
	 * Updates System Job Number measures.
	 * @param job current job.
	 */
	private void updateJobNumber(Job job) {
		if (jobNumPerClass != null) {
			int index = job.getJobClass().getId();
			Measure m = jobNumPerClass[index];
			if (m != null) {
				m.update(jobsPerClass[index], getTime() - lastModifyNumberPerClass[index]);
			}
		}
		if (jobNum != null) {
			jobNum.update(jobs, getTime() - lastModifyNumber);
		}
	}

	/**
	 * Updates System Response Time measures.
	 * @param job current job.
	 */
	private void updateResponseTime(Job job) {
		if (responseTimePerClass != null) {
			int index = job.getJobClass().getId();
			Measure m = responseTimePerClass[index];
			if (m != null) {
				m.update(netSystem.getTime() - job.getSystemEnteringTime(), 1.0);
			}
		}
		if (responseTime != null) {
			responseTime.update(getTime() - job.getSystemEnteringTime(), 1.0);
		}
	}

	/**
	 * Updates System Throughput measures.
	 * @param job current job.
	 */
	private void updateThroughput(Job job) {
		if (throughputPerClass != null) {
			int index = job.getJobClass().getId();
			InverseMeasure m = throughputPerClass[index];
			if (m != null) {
				m.update(getTime() - lastJobOutTimePerClass[index], 1.0);
			}
		}
		if (throughput != null) {
			throughput.update(getTime() - lastJobOutTime, 1.0);
		}
	}

	/**
	 * Updates System Drop Rate measures.
	 * @param job current job.
	 */
	private void updateDropRate(Job job) {
		if (dropRatePerClass != null) {
			int index = job.getJobClass().getId();
			Measure m = dropRatePerClass[index];
			if (m != null) {
				m.update(getTime() - lastJobDropTimePerClass[index], 1.0);
			}
		}
		if (dropRate != null) {
			dropRate.update(getTime() - lastJobDropTime, 1.0);
		}
	}

	/**
	 * Updates System Power measures.
	 * @param job current job.
	 */
	private void updateRenegingRate(Job job) {
		if (renegingRatePerClass != null) {
			int index = job.getJobClass().getId();
			Measure m = renegingRatePerClass[index];
			if (m != null) {
				m.update(getTime() - lastJobRenegeTimePerClass[index], 1.0);
			}
		}
		if (renegingRate != null) {
			renegingRate.update(getTime() - lastJobRenegeTime, 1.0);
		}
	}

	/**
	 * Updates System Balking Rate measures.
	 * @param job current job
	 */
	private void updateBalkingRate(Job job) {
		if (balkingRatePerClass != null) {
			int index = job.getJobClass().getId();
			Measure m = balkingRatePerClass[index];
			if (m != null) {
				m.update(getTime() - lastJobBalkTimePerClass[index], 1.0);
			}
		}
		if (balkingRate != null) {
			balkingRate.update(getTime() - lastJobBalkTime, 1.0);
		}
	}

	/**
	 * Updates System Retrial Attempts Rate measures.
	 * @param job current job
	 */
	private void updateRetrialAttemptsRate(Job job) {
		if (retrialAttemptsRatePerClass != null) {
			int index = job.getJobClass().getId();
			Measure m = retrialAttemptsRatePerClass[index];
			if (m != null) {
				m.update(getTime() - lastJobRetrialTimePerClass[index], 1.0);
			}
		}
		if (retrialAttemptsRate != null) {
			retrialAttemptsRate.update(getTime() - lastJobRetrialTime, 1.0);
		}
	}
	private void updateSystemPower(Job job) {
		sampling_SystemThroughputSum = sampling_SystemThroughputSum + (getTime() - lastJobOutTime);
		sampling_SystemResponseSum   = sampling_SystemResponseSum + (getTime() - job.getSystemEnteringTime());
		systemPowerSamples = systemPowerSamples + 1;
		if (systemPowerPerClass != null) {
			int index = job.getJobClass().getId();
			InverseMeasure m = systemPowerPerClass[index];
			samplingClass_SystemThroughputSum[index] = samplingClass_SystemThroughputSum[index] + getTime() - lastJobOutTimePerClass[index];
			systemPowerSamplesClass[index] = systemPowerSamplesClass[index] + 1;
			if (m != null) {
				double temp = (sampling_SystemResponseSum/systemPowerSamples) * (samplingClass_SystemThroughputSum[index] / systemPowerSamplesClass[index]);
				m.update(temp, 1.0);
			}
		}
		if (systemPower != null) {
			double tmp = (sampling_SystemResponseSum/systemPowerSamples) * (sampling_SystemThroughputSum / systemPowerSamples);
			systemPower.update(tmp, 1.0);
		}
	}

	/**
	 * Updates chain graph.
	 * @param sourcePair source pair.
	 * @param targetPair target pair.
	 */
	protected void updateChainGraph(Pair<NetNode, JobClass> sourcePair, Pair<NetNode, JobClass> targetPair) {
		boolean hasNewPair = false;
		boolean hasNewLink = false;

		if (!pairSet.contains(targetPair)) {
			pairSet.add(targetPair);
			hasNewPair = true;
		}
		Pair<Pair<NetNode, JobClass>, Pair<NetNode, JobClass>> edge =
				new Pair<Pair<NetNode, JobClass>, Pair<NetNode, JobClass>>(sourcePair, targetPair);
		if (sourcePair != null && !linkSet.contains(edge)) {
			linkSet.add(edge);
			hasNewLink = true;
		}

		if (hasNewPair && !hasNewLink) {
			Set<Pair<NetNode, JobClass>> chain = new HashSet<Pair<NetNode, JobClass>>();
			chain.add(targetPair);
			chainSet.add(chain);
			pairChainMap.put(targetPair, chain);
			visitCountPerChain.put(chain, Integer.valueOf(0));
		} else if (!hasNewPair && hasNewLink) {
			Set<Pair<NetNode, JobClass>> sourceChain = pairChainMap.get(sourcePair);
			Set<Pair<NetNode, JobClass>> targetChain = pairChainMap.get(targetPair);
			if (sourceChain == targetChain) {
				return;
			}
			Integer sourceVisitCount = visitCountPerChain.remove(sourceChain);
			Integer targetVisitCount = visitCountPerChain.remove(targetChain);
			chainSet.remove(sourceChain);
			chainSet.remove(targetChain);
			sourceChain.addAll(targetChain);
			chainSet.add(sourceChain);
			for (Pair<NetNode, JobClass> pair : targetChain) {
				pairChainMap.put(pair, sourceChain);
			}
			visitCountPerChain.put(sourceChain, sourceVisitCount + targetVisitCount);
		} else if (hasNewPair && hasNewLink) {
			Set<Pair<NetNode, JobClass>> chain = pairChainMap.get(sourcePair);
			Integer visitCount = visitCountPerChain.remove(chain);
			chainSet.remove(chain);
			chain.add(targetPair);
			chainSet.add(chain);
			pairChainMap.put(targetPair, chain);
			visitCountPerChain.put(chain, visitCount);
		} else {
			// Do nothing
		}
	}

	/**
	 * Updates visit count per chain.
	 * @param job current job.
	 */
	private void updateVisitCountPerChain(Job job) {
		Set<Pair<NetNode, JobClass>> chain = pairChainMap.get(job.getLastVisitedPair());
		Integer visitCount = visitCountPerChain.get(chain) + Integer.valueOf(1);
		visitCountPerChain.put(chain, visitCount);
		totalVisitCount += Integer.valueOf(1);
	}

	// ------------------------------------------------------------------------------------

	/**
	 * It changes the class of a job from @oldClass to @newClass.
	 * It also updates the performance indices.
	 * @param oldClass the old class of the job.
	 * @param newClass the new class of the job.
	 */
	public void performJobClassSwitch(JobClass oldClass, JobClass newClass) {
		// Get the identifiers of old and new classes
		int oldClassId = oldClass.getId();
		int newClassId = newClass.getId();

		// Updates old class measure (if not null)
		if (jobNumPerClass != null && jobNumPerClass[oldClassId] != null) {
			jobNumPerClass[oldClassId].update(jobsPerClass[oldClassId], getTime() - lastModifyNumberPerClass[oldClassId]);
		}
		lastModifyNumberPerClass[oldClassId] = getTime();

		// Updates new class measure (if not null)
		if (jobNumPerClass != null && jobNumPerClass[newClassId] != null) {
			jobNumPerClass[newClassId].update(jobsPerClass[newClassId], getTime() - lastModifyNumberPerClass[newClassId]);
		}
		lastModifyNumberPerClass[newClassId] = getTime();

		// Updates jobs per class
		jobsPerClass[oldClassId]--;
		jobsPerClass[newClassId]++;
	}

	
	private double getTime(){
		return netSystem.getTime();
	}
	
	public void setNetSystem(NetSystem netSystem){
		this.netSystem = netSystem;
	}

	/**
	 * Gets chain set.
	 * @return chain set.
	 */
	public Set<Set<Pair<NetNode, JobClass>>> getChainSet() {
		return chainSet;
	}

	/**
	 * Gets pair chain map.
	 * @return pair chain map.
	 */
	public Map<Pair<NetNode, JobClass>, Set<Pair<NetNode, JobClass>>> getPairChainMap() {
		return pairChainMap;
	}

	/**
	 * Gets visit count per chain.
	 * @return visit count per chain.
	 */
	public Map<Set<Pair<NetNode, JobClass>>, Integer> getVisitCountPerChain() {
		return visitCountPerChain;
	}

	/**
	 * Gets total visit count.
	 * @return total visit count.
	 */
	public Integer getTotalVisitCount() {
		return totalVisitCount;
	}

}
