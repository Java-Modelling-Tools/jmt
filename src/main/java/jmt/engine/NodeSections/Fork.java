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

import java.util.HashSet;
import java.util.Map.Entry;

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

	/** Number of jobs to be routed on each link */
	private int jobsPerLink;

	/** Current jobs inside a fork-join region */
	private HashSet<Job> jobs;

	private Boolean isSimplifiedFork;

	private ForkStrategy[] forkStrategies;

	private JobInfoList nodeJobsList;
	private GlobalJobInfoList netJobsList;

	// --- Constructors -----------------------------------------------------------------------------
	/**
	 * Constructs a new Fork without blocking and with 1 job per link.
	 */
	public Fork() {
		this(1, -1);
	}

	/**
	 * Construct a new Fork node.
	 * @param jobsPerLink number of jobs to be routed on each link.
	 * @param block maximum number of jobs allowed in a fork-join
	 * region (-1 or 0 is infinity).
	 */
	public Fork(Integer jobsPerLink, Integer block) {
		this(jobsPerLink.intValue(), block.intValue());
	}

	/**
	 * Construct a new Fork node.
	 * @param jobsPerLink number of jobs to be routed on each link.
	 * @param block maximum number of jobs allowed in a fork-join
	 * region (-1 or 0 is infinity).
	 */
	public Fork(int jobsPerLink, int block) {
		// Disables automatic handling of jobinfolist
		super(false);
		this.block = block;
		this.jobsPerLink = jobsPerLink;
		jobs = new HashSet<Job>();
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
	protected void nodeLinked(NetNode node) throws NetException {
		nodeJobsList = node.getJobInfoList();	
	}

	/**
	 * Splits input job on every output link and waits if 'block' job number are
	 * not joined if and only if block is enabled.
	 *
	 * @param message message to be processed.
	 * @throws NetException if something goes wrong.
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
			NodeList outNodeList = null;
			int totalOutNum = 0;
			NodeListWithJobNum outputInfo = forkStrategies[jobClass.getId()].getOutNodes(this, jobClass);
			if (isSimplifiedFork) {
				outNodeList = getOwnerNode().getOutputNodes();
				totalOutNum = jobsPerLink * outNodeList.size();
			} else {
				outNodeList = outputInfo.getNodeList();
				totalOutNum = outputInfo.getTotalNum();
			}

			if (totalOutNum > 0) {
				//removes job from node jobInfoList
				JobInfo jobData = nodeJobsList.lookFor(job);
				if (jobData != null) {
					nodeJobsList.remove(jobData);
				}
			} else {
				if (!(job instanceof ForkJob)) {
					//drops job from global jobInfoList
					netJobsList.dropJob(job);
				}
				//drops job from node jobInfoList and then sends ack back
				sendAckAfterDrop(job, 0.0, NodeSection.SERVICE, getOwnerNode());
				break;
			}

			ForkJobInfo newJobInfo = new ForkJobInfo(job, totalOutNum, getOwnerNode());
			RandomEngine randomEngine = this.getNetSytem().getEngine();
			// Durstenfeld Shuffle
			NetNode[] outNodePermutation = outNodeList.toArray();
			for (int i = outNodePermutation.length - 1; i > 0; i--) {
				int j = (int) Math.floor(randomEngine.raw() * (i + 1));
				NetNode outNode = outNodePermutation[i];
				outNodePermutation[i] = outNodePermutation[j];
				outNodePermutation[j] = outNode;
			}
			// Sends "jobsPerLink" jobs on each output link
			for (NetNode outNode : outNodePermutation) {
				if (isSimplifiedFork) {
					for (int n = 0; n < jobsPerLink; n++) {
						ForkJob newJob = new ForkJob(jobClass, newJobInfo);
							newJob.initialize(outNode.getNetSystem()); 
						// Sends new job to the following station
						send(newJob, 0.0, outNode);
					}
				} else {
					// Releases new jobs in the random order
					ForkJob[] newJobPermutation = new ForkJob[outputInfo.getTotalNum(outNode)];
					int index = 0;
					for (Entry<JobClass, Integer> entry : outputInfo.getJobNumPerClass(outNode).entrySet()) {
						JobClass newJobClass = entry.getKey();
						int newJobNum = entry.getValue().intValue();
						for (int n = 0; n < newJobNum; n++) {
							ForkJob newJob =  new ForkJob(newJobClass, newJobInfo);
                            newJob.initialize(outNode.getNetSystem()); 
							newJobPermutation[index] = newJob;
							index++;
						}
					}

					// Durstenfeld Shuffle                    
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
			jobs.add(job);
			// If this fork does not block, sends ack back
			if (block < 0 || jobs.size() < block) {
				sendBackward(NetEvent.EVENT_ACK, job, 0.0);
			}
			break;

		case NetEvent.EVENT_ACK:
			break;

		case NetEvent.EVENT_JOIN:
			job = (Job) message.getData();
			if (jobs.contains(job)) {
				// If this fork blocks, finally sends ack back and unlocks it
				if (block >= 0 && jobs.size() >= block) {
					sendBackward(NetEvent.EVENT_ACK, job, 0.0);
				}
				jobs.remove(job);
				send(NetEvent.EVENT_JOIN, job, 0.0, NodeSection.INPUT, getOwnerNode());
			}
			break;

		case NetEvent.EVENT_STOP:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		// Everything is okay
		return MSG_PROCESSED;
	}

}
