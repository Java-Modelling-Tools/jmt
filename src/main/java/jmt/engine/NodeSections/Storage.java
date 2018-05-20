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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueueGetStrategy;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.TransitionUtilities.TransitionVector;
import jmt.engine.QueueNet.BlockingRegion;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.QueueNet.WaitingRequest;
import jmt.engine.random.engine.RandomEngine;

/**
 * <p>Title: Storage</p>
 * <p>Description: This class implements the storage section.</p>
 *
 * @author Lulai Zhu
 * Date: 15-07-2016
 * Time: 21.00.00
 */
public class Storage extends InputSection {

	public static final String FINITE_DROP = "drop";
	public static final String FINITE_BLOCK = "BAS blocking";
	public static final String FINITE_WAITING = "waiting queue";

	private int totalCapacity;
	private int[] capacities;
	private boolean[] drop;
	private boolean[] block;
	private QueueGetStrategy getStrategy;
	private QueuePutStrategy[] putStrategies;

	private JobClassList jobClasses;
	private JobInfoList nodeJobsList;
	private GlobalJobInfoList netJobsList;
	private TransitionVector storageVector;
	private TransitionVector nextStorageVector;
	private boolean waitingRelease;
	private boolean acceptedRelease;
	private int droppedJobs;
	private int[] droppedJobsPerClass;
	private LinkedList<WaitingRequest> waitingRequests;

	//--------------------BLOCKING REGION PROPERTIES--------------------//
	//true if the redirection behaviour is turned on
	private boolean redirectionON;
	//the blocking region that the owner node of this section belongs to
	private BlockingRegion myRegion;
	//the input station of the blocking region
	private NetNode regionInputStation;
	//------------------------------------------------------------------//

	/**
	 * Creates a new instance of the storage section.
	 */
	public Storage(Integer totalCapacity, Integer[] capacities, String[] dropRules, QueueGetStrategy getStrategy,
			QueuePutStrategy[] putStrategies) {
		super(false);
		this.totalCapacity = totalCapacity.intValue();
		this.capacities = new int[capacities.length];
		for (int i = 0; i < capacities.length; i++) {
			this.capacities[i] = capacities[i].intValue();
		}
		drop = new boolean[dropRules.length];
		block = new boolean[dropRules.length];
		for (int i = 0; i < dropRules.length; i++) {
			if (dropRules[i].equals(FINITE_DROP)) {
				drop[i] = true;
				block[i] = false;
			} else if (dropRules[i].equals(FINITE_BLOCK)) {
				drop[i] = false;
				block[i] = true;
			} else if (dropRules[i].equals(FINITE_WAITING)) {
				drop[i] = false;
				block[i] = false;
			} else {
				drop[i] = true;
				block[i] = false;
			}
		}
		this.getStrategy = getStrategy;
		this.putStrategies = putStrategies;
		redirectionTurnOFF();
	}

	/**
	 * Tells if the redirection behaviour is turned on.
	 */
	public boolean isRedirectionON() {
		return redirectionON;
	}

	/**
	 * Turns off the redirection behaviour.
	 */
	public void redirectionTurnOFF() {
		redirectionON = false;
		myRegion = null;
		regionInputStation = null;
	}

	/**
	 * Turns on the redirection behaviour.
	 */
	public void redirectionTurnON(BlockingRegion region) {
		redirectionON = true;
		myRegion = region;
		regionInputStation = myRegion.getInputStation();
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		jobClasses = getJobClasses();
		nodeJobsList = node.getJobInfoList();
		storageVector = new TransitionVector(node.getName(), jobClasses.size());
		nextStorageVector = new TransitionVector(node.getName(), jobClasses.size());
		waitingRelease = false;
		acceptedRelease = false;
		droppedJobs = 0;
		droppedJobsPerClass = new int[jobClasses.size()];
		Arrays.fill(droppedJobsPerClass, 0);
		waitingRequests = new LinkedList<WaitingRequest>();
	}

	/**
	 * Preloads the specified numbers of jobs for each class.
	 * @param jobsPerClass the specified numbers of jobs for each class.
	 * @throws NetException
	 */
	public void preloadJobs(int[] jobsPerClass) throws NetException {
		int totalJobs = 0;
		for (int i = 0; i < jobsPerClass.length; i++) {
			totalJobs += jobsPerClass[i];
		}
		Job[] jobPermutation = new Job[totalJobs];
		int index = 0;
		for (int i = 0; i < jobsPerClass.length; i++) {
			for (int j = 0; j < jobsPerClass[i]; j++) {
				jobPermutation[index] = new Job(jobClasses.get(i), jobClasses.size());
				index++;
			}
		}

		/* //Commented to ensure a deterministic initialization
		RandomEngine randomEngine = RandomEngine.makeDefault();
		for (int i = jobPermutation.length - 1; i > 0; i--) {
			int j = (int) Math.floor(randomEngine.raw() * (i + 1));
			Job job = jobPermutation[i];
			jobPermutation[i] = jobPermutation[j];
			jobPermutation[j] = job;
		}
		*/

		netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
		for (int i = 0; i < jobPermutation.length; i++) {
			Job job = jobPermutation[i];
			JobInfo jobInfo = new JobInfo(job);
			putStrategies[job.getJobClass().getId()].put(job, jobsList, this);
			nodeJobsList.add(jobInfo);
			netJobsList.addJob(job);
			if (redirectionON) {
				myRegion.increaseOccupation(job.getJobClass());
			}
		}
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Object data = message.getData();
		NetNode source = message.getSource();
		byte sourceSection = message.getSourceSection();

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
		{
			netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
			if (jobsList.size() > 0) {
				for (int i = 0; i < jobClasses.size(); i++) {
					storageVector.setEntry(i, jobsList.size(jobClasses.get(i)));
				}
				sendForward(NetEvent.EVENT_JOB_CHANGE, storageVector, 0.0);
			}
			break;
		}

		case NetEvent.EVENT_JOB:
		{
			Job job = message.getJob();
			JobClass jobClass = job.getJobClass();
			if (redirectionON && source != regionInputStation && !myRegion.belongsToRegion(source)) {
				redirect(job, 0.0, NodeSection.INPUT, regionInputStation);
				send(NetEvent.EVENT_ACK, job, 0.0, sourceSection, source);
				break;
			}

			if ((totalCapacity < 0 || jobsList.size() < totalCapacity)
					&& (capacities[jobClass.getId()] < 0 || jobsList.size(jobClass) < capacities[jobClass.getId()])) {
				putStrategies[jobClass.getId()].put(job, jobsList, this);
				send(NetEvent.EVENT_ACK, job, 0.0, sourceSection, source);

				if (waitingRelease) {
					acceptedRelease = true;
				} else {
					for (int i = 0; i < jobClasses.size(); i++) {
						storageVector.setEntry(i, jobsList.size(jobClasses.get(i)));
					}
					sendForward(NetEvent.EVENT_JOB_CHANGE, storageVector, 0.0);
				}
			} else if (drop[jobClass.getId()]) {
				if (!(job instanceof ForkJob)) {
					netJobsList.dropJob(job);
					droppedJobs++;
					droppedJobsPerClass[jobClass.getId()]++;
				}
				sendAckAfterDrop(job, 0.0, sourceSection, source);
				if (redirectionON) {
					myRegion.decreaseOccupation(jobClass);
					send(NetEvent.EVENT_JOB_OUT_OF_REGION, job, 0.0, NodeSection.INPUT, regionInputStation);
				}
			} else if (block[jobClass.getId()]) {
				waitingRequests.add(new WaitingRequest(job, sourceSection, source));
			} else {
				waitingRequests.add(new WaitingRequest(job, sourceSection, source));
				send(NetEvent.EVENT_ACK, job, 0.0, sourceSection, source);
			}
			break;
		}

		case NetEvent.EVENT_ACK:
			break;

		case NetEvent.EVENT_JOB_REQUEST:
		{
			TransitionVector enablingVector = (TransitionVector) data;
			for (int i = 0; i < jobClasses.size(); i++) {
				int requestedJobs = enablingVector.getEntry(i);
				if (requestedJobs > jobsList.size(jobClasses.get(i))) {
					return MSG_NOT_PROCESSED;
				}
			}
			for (int i = 0; i < jobClasses.size(); i++) {
				int requestedJobs = enablingVector.getEntry(i);
				for (int j = 0; j < requestedJobs; j++) {
					sendForward(getStrategy.get(jobsList, jobClasses.get(i)), 0.0);
				}
			}
			for (int i = 0; i < jobClasses.size(); i++) {
				storageVector.setEntry(i, jobsList.size(jobClasses.get(i)));
			}
			sendForward(NetEvent.EVENT_JOB_CHANGE, storageVector, 0.0);
			sendForward(NetEvent.EVENT_JOB_FINISH, null, 0.0);

			if (waitingRequests.size() > 0) {
				boolean nextJobChange = false;
				Iterator<WaitingRequest> it = waitingRequests.iterator();
				while (it.hasNext()) {
					WaitingRequest request = it.next();
					Job job = request.getJob();
					JobClass jobClass = job.getJobClass();
					if (capacities[jobClass.getId()] >= 0 && jobsList.size(jobClass) >= capacities[jobClass.getId()]) {
						continue;
					}
					it.remove();
					putStrategies[jobClass.getId()].put(job, jobsList, this);
					if (block[jobClass.getId()]) {
						send(NetEvent.EVENT_ACK, job, 0.0, request.getSourceSection(), request.getSource());
					}
					nextJobChange = true;
					if (totalCapacity >= 0 && jobsList.size() >= totalCapacity) {
						break;
					}
				}

				if (nextJobChange) {
					for (int i = 0; i < jobClasses.size(); i++) {
						nextStorageVector.setEntry(i, jobsList.size(jobClasses.get(i)));
					}
					sendForward(NetEvent.EVENT_JOB_CHANGE, nextStorageVector, 0.0);
				}
			}
			break;
		}

		case NetEvent.EVENT_JOB_RELEASE:
		{
			waitingRelease = true;
			break;
		}

		case NetEvent.EVENT_JOB_FINISH:
		{
			waitingRelease = false;
			if (acceptedRelease) {
				for (int i = 0; i < jobClasses.size(); i++) {
					storageVector.setEntry(i, jobsList.size(jobClasses.get(i)));
				}
				sendForward(NetEvent.EVENT_JOB_CHANGE, storageVector, 0.0);
				acceptedRelease = false;
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

	/**
	 * Gets the total number of dropped jobs.
	 * @return the total number of dropped jobs.
	 */
	public int getDroppedJobs() {
		return droppedJobs;
	}

	/**
	 * Gets the number of dropped jobs for the specified class.
	 * @param classIndex the index of the job class.
	 * @return the number of dropped jobs for the specified class.
	 */
	public int getDroppedJobsPerClass(int classIndex) {
		return droppedJobsPerClass[classIndex];
	}

}
