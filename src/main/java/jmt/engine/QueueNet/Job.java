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

import jmt.engine.simEngine.RemoveToken;
import org.apache.commons.math3.util.Pair;

import java.util.Objects;
/**
 *	This class implements a generic job of a queue network.
 * 	@author Francesco Radaelli, Marco Bertoli
 */
public class Job implements Cloneable {

	//counter used to generate id
	//private int counter;
	//job ID
	private int Id;

	private double serviceArrivalTime;
	private boolean jobIsInService;
	private boolean isPreemptedJob;
	private RemoveToken servingMessage;

	//class of this job
	private JobClass jobClass;
	//used to compute system response time
	protected double systemEnteringTime;

	/*
	This field is used with blocking region.
	The presence of an input station, in fact, modifies the route of some jobs:
	instead of being processed directly by the destination node, they are first
	redirected to the region input station (which check the capability of the
	blocking region) and then returned to the destination node, using the
	informations contained in this object.
	 */

	//the original destination of the job message
	private NetNode originalDestinationNode = null;

	private double serviceTime = -1.0;
	
    private NetSystem netSystem;

	protected GlobalJobInfoList globalJobInfoList = null;
	protected Pair<NetNode, JobClass> lastVisitedPair = null;

	/**
	 * Creates a new instance of Job.
	 * @param jobClass Reference to the class of the job.
	 * @param netJobsList Reference to the global jobInfoList.
	 */
	public Job(JobClass jobClass, GlobalJobInfoList globalJobInfoList) {
		//this.Id = counter++;
		this.jobClass = jobClass;
		this.globalJobInfoList = globalJobInfoList;
		//reset();
        serviceArrivalTime = 0;
		isPreemptedJob = false;
		jobIsInService = false;
	}

	/**
	 * Resets the counter of Job.
	 */
	//public static void resetCounter() {
	//	counter = 0;
	//}

	/**
	 * Gets the ID of this job.
	 * @return the ID of this job.
	 */
	public int getId() {
		return Id;
	}

	/**
	 * Gets the class of this job.
	 * @return the class of this job.
	 */
	public JobClass getJobClass() {
		return jobClass;
	}

	/**
	 * Sets the class of this job.
	 * @param jobClass the class of this job.
	 */
	public void setJobClass(JobClass jobClass) {
		this.jobClass = jobClass;
	}

	/**
	 * Resets this job.
	 */
	public void reset() {
		systemEnteringTime = netSystem.getTime();
	}

	/**
	 * Gets the system entering time of this job.
	 * @return the system entering time of this job.
	 */
	public double getSystemEnteringTime() {
		return systemEnteringTime;
	}

	/**
	 * Sets the system entering time of this job.
	 * @param time the system entering time of this job.
	 */
	public void setSystemEnteringTime(double time) {
		this.systemEnteringTime = time;
	}

	/**
	 * Gets the destination node of this redirected job.
	 * @return the destination node of this redirected job.
	 */
	public NetNode getOriginalDestinationNode() {
		return originalDestinationNode;
	}

	/**
	 * Sets the destination node of this redirected job.
	 * @param originalDestinationNode the destination node of this redirected job.
	 */
	public void setOriginalDestinationNode(NetNode originalDestinationNode) {
		this.originalDestinationNode = originalDestinationNode;
	}

	/**
	 * Gets the service time of this job.
	 * @return the service time of this job.
	 */
	public double getServiceTime() {
		return serviceTime;
	}

	/**
	 * Sets the service time of this job.
	 * @param time the service time of this job.
	 */
	public void setServiceTime(double time) {
		this.serviceTime = time;
	}

	/**
	 * Gets the last pair visited by this job.
	 * @return the last pair visited by this job.
	 */
	public Pair<NetNode, JobClass> getLastVisitedPair() {
		return lastVisitedPair;
	}

	/**
	 * Sets the last pair visited by this job.
	 * @param pair the last pair visited by this job.
	 */
	public void setLastVisitedPair(Pair<NetNode, JobClass> pair) {
		this.lastVisitedPair = pair;
	}

	public NetSystem getNetSystem(){
		return netSystem;
	}	
	
	/**
	 * Adds a pair to the visit path of this job.
	 * @param pair the pair to be added to the visit path.
	 */
	public void AddToVisitPath(Pair<NetNode, JobClass> pair) {
		globalJobInfoList.updateChainGraph(lastVisitedPair, pair);
		lastVisitedPair = pair;
	}

	public double getServiceArrivalTime() {
		return serviceArrivalTime;
	}

	public void setServiceArrivalTime(double serviceArrivalTime) {
		this.serviceArrivalTime = serviceArrivalTime;
	}

	public boolean getIsPreemptedJob() {
		return isPreemptedJob;
	}

	public void setIsPreemptedJob(boolean isPreemptedJob) {
		this.isPreemptedJob = isPreemptedJob;
	}

	public boolean getIsJobInService() {
		return jobIsInService;
	}

	public void setIsJobInService(boolean jobIsInService) {
		this.jobIsInService = jobIsInService;
	}
	public RemoveToken getServingMessage() {
		return servingMessage;
	}

	public void setServingMessage(RemoveToken servingMessage) {
		this.servingMessage = servingMessage;
	}
	
	public void initialize(NetSystem netSystem){
		this.netSystem = netSystem;
		// Job Id is used only for logging
		this.Id = netSystem.nextjobNumber();
		resetSystemEnteringTime();
	}
	
	public void resetSystemEnteringTime() {
		this.systemEnteringTime = netSystem.getTime();
		//resetClass();
	}

	@Override
	public boolean equals(Object that) {
		if (!(that instanceof Job)) {
			return false;
		}
		Job a = (Job) that;
		return a.getId() == this.Id;
	}

	@Override
	public int hashCode() {
		return getId();
	}
}
