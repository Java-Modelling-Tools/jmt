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

import jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy;
import jmt.engine.simEngine.RemoveToken;
import org.apache.commons.math3.util.Pair;

/**
 *	This class implements a generic job of a queue network.
 * 	@author Francesco Radaelli, Marco Bertoli
 */
public class Job implements Cloneable {

	//counter used to generate id
	//private int counter;
	//job ID
	private int Id;

	//class of this job
	private JobClass jobClass;
	//next class of this job
	private JobClass nextJobClass;
	//network system
	protected NetSystem netSystem;
	//used to compute system response time
	protected double systemEnteringTime;

	private double totalQueueTime = 0.0;

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
	private double serviceStartTime = -1.0;
	private RemoveToken servingMessage = null;
	private RemoveToken renegingMessage = null;

	private boolean isToken;

	protected GlobalJobInfoList globalJobInfoList = null;
	protected Pair<NetNode, JobClass> lastVisitedPair = null;

	/* Note that this is stored as an absolute time, rather than
	the relative time specified by the user. */
	private double currentStationSoftDeadline = -1.0;

	/**
	 * Creates a new instance of Job.
	 * @param jobClass Reference to the class of the job.
	 * @param netJobsList Reference to the global jobInfoList.
	 */
	public Job(JobClass jobClass, GlobalJobInfoList globalJobInfoList) {
		this.jobClass = jobClass;
		this.nextJobClass = jobClass;
		this.globalJobInfoList = globalJobInfoList;
		this.isToken = false;
	}

	/**
	 * Initializes this job.
	 * @param the network system.
	 */
	public void initialize(NetSystem netSystem) {
		this.netSystem = netSystem;
		// Job Id is used only for logging
		this.Id = netSystem.nextjobNumber();
		resetSystemEnteringTime();
	}

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
	 * Gets the next class of this job.
	 * @return the next class of this job.
	 */
	public JobClass getNextJobClass() {
		return nextJobClass;
	}

	/**
	 * Sets the next class of this job.
	 * @param jobClass the next class of this job.
	 */
	public void setNextJobClass(JobClass jobClass) {
		this.nextJobClass = jobClass;
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
	 * Resets the system entering time of this job.
	 * @param time the system entering time of this job.
	 */
	public void resetSystemEnteringTime() {
		systemEnteringTime = netSystem.getTime();
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
	 * @param node the destination node of this redirected job.
	 */
	public void setOriginalDestinationNode(NetNode node) {
		this.originalDestinationNode = node;
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
	 * Gets the service start time of this job.
	 * @return the service start time of this job.
	 */
	public double getServiceStartTime() {
		return serviceStartTime;
	}

	/**
	 * Sets the service start time of this job.
	 * @param time the service start time of this job.
	 */
	public void setServiceStartTime(double time) {
		this.serviceStartTime = time;
	}

	/**
	 * Gets the serving message of this job.
	 * @return the serving message of this job.
	 */
	public RemoveToken getServingMessage() {
		return servingMessage;
	}

	/**
	 * Sets the serving message of this job.
	 * @param message the serving message of this job.
	 */
	public void setServingMessage(RemoveToken message) {
		this.servingMessage = message;
	}

	/**
	 * Gets the remaining service time of this job.
	 * @return the remaining service time of this job.
	 */
	public double getRemainingServiceTime() {
		if (serviceStartTime < 0.0) {
			return serviceTime;
		} else {
			return serviceTime - (netSystem.getTime() - serviceStartTime);
		}
	}

	/**
	 * Gets the reneging message of this job.
	 * @return the reneging message of this job.
	 */
	public RemoveToken getRenegingMessage() {
		return renegingMessage;
	}

	/**
	 * Sets the reneging message of this job.
	 * @param message the reneging message of this job.
	 */
	public void setRenegingMessage(RemoveToken message) {
		this.renegingMessage = message;
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

	/**
	 * Adds a pair to the visit path of this job.
	 * @param pair the pair to be added to the visit path.
	 */
	public void AddToVisitPath(Pair<NetNode, JobClass> pair) {
		globalJobInfoList.updateChainGraph(lastVisitedPair, pair);
		lastVisitedPair = pair;
	}

	public double getTotalQueueTime() {
		return totalQueueTime;
	}

	public void addQueueTime(double time) {
		totalQueueTime += time;
	}

	public void setTotalQueueTime(double time) {
		totalQueueTime = time;
	}

	public NetSystem getNetSystem() {
		return netSystem;
	}

	public double getCurrentStationSoftDeadline() {
		return currentStationSoftDeadline;
	}

	public void setCurrentStationSoftDeadline(double currentStationSoftDeadline) {
		this.currentStationSoftDeadline = currentStationSoftDeadline;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Job)) {
			return false;
		}
		Job that = (Job) obj;
		return this == that || this.Id == that.getId();
	}

	@Override
	public int hashCode() {
		return getId();
	}

}
