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

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ForkStrategy;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.ForkJobInfo;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.QueueNet.NodeListWithJobNum;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.random.engine.RandomEngine;

/**
 * <p>Title: Fork</p>
 * <p>Description: This class is a fork output section, used to split a job on every
 * output link. Split job can be assembled back in a join input section.
 * A maximum number of jobs inside a fork-join section can be specified: when reached
 * this component will block until at least one job is joined.</p>
 *
 * @author Bertoli Marco
 *         Date: 13-mar-2006
 *         Time: 15.23.22
 * 
 * Modified by J. Shuai & M. Cazzoli, implemented fork strategies
 */
public class Fork extends OutputSection {

	/** Maximum number of jobs allowed in a fork-join region (-1 or 0 is infinity) */
	private int block;

	/** Counts received ACK by following nodes */
	private int ackCount;

	/** Number of jobs to be routed on each link */
	private int jobsPerLink;

	/** Number of output nodes */
	private int numOut;

	/** Current number of jobs inside a fork-join region */
	private int jobNum;

	private Boolean isSimplifiedFork;

	private ForkStrategy[] forkStrategies;

	private int droppedJobs;
	private int[] droppedJobsPerClass;
	private int switchedJobs;
	private int[] switchedJobsPerClass; 

	private JobInfoList nodeJobsList;
	private GlobalJobInfoList netJobsList;
	private RandomEngine randomEngine;

	// --- Constructors -----------------------------------------------------------------------------
	/**
	 * Constructs a new Fork without blocking and with 1 job per link
	 */
	public Fork() {
		this(1, -1);
	}

	/**
	 * Construct a new Fork node
	 * @param jobsPerLink number of jobs to be routed on each link
	 * @param block maximum number of jobs allowed in a fork-join
	 * region (-1 or 0 is infinity)
	 */
	public Fork(Integer jobsPerLink, Integer block) {
		this(jobsPerLink.intValue(), block.intValue());
	}

	/**
	 * Construct a new Fork node
	 * @param jobsPerLink number of jobs to be routed on each link
	 * @param block maximum number of jobs allowed in a fork-join
	 * region (-1 or 0 is infinity)
	 */
	public Fork(int jobsPerLink, int block) {
		// Disables automatic handling of jobinfolist
		super(false);
		jobNum = 0;
		this.block = block;
		this.jobsPerLink = jobsPerLink;
	}

	//extend the fork section with fork strategies
	public Fork(Integer jobsPerLink, Integer block, Boolean isSimplifiedFork, ForkStrategy[] forkStrategies) {
		this(jobsPerLink, block);
		//if use the original simple fork
		this.isSimplifiedFork = isSimplifiedFork;
		this.forkStrategies = forkStrategies;
	}

	// ----------------------------------------------------------------------------------------------

	@Override
	protected void nodeLinked(NetNode node) {
		droppedJobs = 0;
		droppedJobsPerClass = new int[getJobClasses().size()];
		Arrays.fill(droppedJobsPerClass, 0);
		switchedJobs = 0;
		switchedJobsPerClass = new int[getJobClasses().size()];
		Arrays.fill(switchedJobsPerClass, 0);
		nodeJobsList = node.getJobInfoList();
		randomEngine = RandomEngine.makeDefault();
	};

	/**
	 * Splits input job on every output link and waits if 'block' job number are
	 * not joined if and only if block is enabled.
	 *
	 * @param message message to be processed.
	 * @throws NetException if something goes wrong
	 * @return message processing result.
	 */
	@Override
	protected int process(NetMessage message) throws NetException {
		Job job;

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
			netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
			break;

		case NetEvent.EVENT_JOB:
			job = message.getJob();
			JobClass jobClass = job.getJobClass();
			NodeList output = null;
			NodeListWithJobNum outputInfo = forkStrategies[jobClass.getId()].getOutNodes(this, jobClass);
			if (isSimplifiedFork) {
				output = getOwnerNode().getOutputNodes();
				numOut = jobsPerLink * output.size();
			} else {
				output = outputInfo.getNodeList();
				numOut = outputInfo.getTotalNum();
			}
			ackCount = 0;

			if (numOut > 0) {
				//removes job from node jobInfoList
				JobInfo jobData = nodeJobsList.lookFor(job);
				if (jobData != null) {
					nodeJobsList.remove(jobData);
				}
			} else {
				if (!(job instanceof ForkJob)) {
					//drops job from global jobInfoList
					netJobsList.dropJob(job);
					droppedJobs++;
					droppedJobsPerClass[jobClass.getId()]++;
				}
				//drops job from node jobInfoList and then sends ack back
				sendAckAfterDrop(job, 0.0, NodeSection.SERVICE, getOwnerNode());
				break;
			}

			ForkJobInfo newJobInfo = new ForkJobInfo(job, numOut, getOwnerNode());
			// Sends "jobsPerLink" jobs on each output link
			Iterator<NetNode> it = output.randomNodes().iterator();
			while (it.hasNext()) {
				NetNode outNode = it.next();
				if (isSimplifiedFork) {
					for (int n = 0; n < jobsPerLink; n++) {
						ForkJob newJob = new ForkJob(jobClass, newJobInfo);
						// Sends new job to the following station
						send(newJob, 0.0, outNode);
					}
				} else {
					// Releases new jobs in the random order
					ForkJob[] newJobPermutation = new ForkJob[outputInfo.getTotalNum(outNode)];
					int index = 0;
					for (JobClass c : outputInfo.getClassesWithNum(outNode).keySet()) {
						for (int n = 0; n < outputInfo.getClassesWithNum(outNode).get(c).intValue(); n++) {
							newJobPermutation[index] = new ForkJob(c, newJobInfo);
							index++;
						}
						if (outputInfo.getClassesWithNum(outNode).get(c).intValue() > 0 && !hasBeenClass(c, job)) {
							switchedJobs++;
							switchedJobsPerClass[c.getId()]++;
							job.addClass(c);
						}
					}

					for (int i = newJobPermutation.length - 1; i > 0; i--) {
						int j = (int) Math.floor(randomEngine.raw() * (i + 1));
						ForkJob newJob = newJobPermutation[i];
						newJobPermutation[i] = newJobPermutation[j];
						newJobPermutation[j] = newJob;
					}

					for (int i = 0; i < newJobPermutation.length; i++) {
						ForkJob newJob = newJobPermutation[i];
						// Sends new job to the following station
						send(newJob, 0.0, outNode);
					}
				}
			}
			jobNum++;
			break;

		case NetEvent.EVENT_ACK:
			ackCount++;
			// If this fork does not block, sends ack back when all messages are delivered to unlock this fork
			job = ((ForkJob) message.getJob()).getForkedJob();
			if ((jobNum < block || block <= 0) && ackCount == numOut) {
				sendBackward(NetEvent.EVENT_ACK, job, 0.0);
			}
			break;

		case NetEvent.EVENT_JOIN:
			// If this fork blocks, finally sends ack back and unlocks this
			job = (Job) message.getData();
			send(NetEvent.EVENT_JOIN, job, 0.0, NodeSection.INPUT, getOwnerNode());
			if (jobNum == block) {
				sendBackward(NetEvent.EVENT_ACK, job, 0.0);
			}
			jobNum--;
			break;

		case NetEvent.EVENT_STOP:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		// Everything was okay
		return MSG_PROCESSED;
	}

	private boolean hasBeenClass(JobClass jobClass, Job job) {
		if (job instanceof ForkJob)
			return hasBeenClass(jobClass, ((ForkJob) job).getForkedJob());
		return job.hasBeenClass(jobClass);
	}

	/**
	 * Gets the total number of dropped jobs
	 * @return the total number of dropped jobs
	 */
	public int getDroppedJobs() {
		return droppedJobs;
	}

	/**
	 * Gets the number of dropped jobs for the specified class
	 * @param classIndex the index of the job class
	 * @return the number of dropped jobs for the specified class
	 */
	public int getDroppedJobsPerClass(int classIndex) {
		return droppedJobsPerClass[classIndex];
	}

	/**
	 * Gets the total number of switched jobs
	 * @return the total number of switched jobs
	 */
	public int getSwitchedJobs() {
		return switchedJobs;
	}

	/**
	 * Gets the number of switched jobs for the specified class
	 * @param classIndex the index of the job class
	 * @return the number of switched jobs for the specified class
	 */
	public int getSwitchedJobsPerClass(int classIndex) {
		return switchedJobsPerClass[classIndex];
	}

}
