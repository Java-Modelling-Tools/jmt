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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.JoinStrategies.GuardJoin;
import jmt.engine.NetStrategies.JoinStrategies.NormalJoin;
import jmt.engine.NetStrategies.JoinStrategy;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.LinkedJobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.QueueNet.SimConstants;
import jmt.engine.dataAnalysis.Measure;

/**
 * <p>Title: Join</p>
 * <p>Description: This class is a Join input section, used to re-assemble previously
 * forked jobs. Jobs are kept until all fragments are collected. If a non-fragmented
 * job is received, it is simply routed to service section.</p>
 *
 * @author Bertoli Marco
 *         Date: 13-mar-2006
 *         Time: 16.19.58
 * 
 * Modified by J. Shuai & M. Cazzoli, implemented join strategies
 */
public class Join extends InputSection {

	/** Data structure used to store received tasks for each job */
	private HashMap<Job, List<ForkJob>> forkJobs;
	private HashMap<Job, Integer> total;
	private HashMap<Job, Integer> required;
	private HashMap<Job, Integer[]> requiredPerClass;
	private HashMap<Job, Boolean> hasFired;
	private HashMap<Job, NetNode> forkNode;

	private JoinStrategy[] joinStrategies;

	private JobClassList jobClasses;
	private JobInfoList nodeJobsList;
	private JobInfoList FJList;

	// --- Constructors -----------------------------------------------------------------------------
	/**
	 * Constructs a new Join.
	 */
	public Join() {
		// Disables automatic handling of jobinfolists
		super(false);
		forkJobs = new HashMap<Job, List<ForkJob>>();
		total = new HashMap<Job, Integer>();
		required = new HashMap<Job, Integer>();
		requiredPerClass = new HashMap<Job, Integer[]>();
		hasFired = new HashMap<Job, Boolean>();
	}

	public Join(JoinStrategy[] joinStrategies) {
		this();
		this.joinStrategies = joinStrategies;
	}

	// ----------------------------------------------------------------------------------------------

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		jobClasses = getJobClasses();
		nodeJobsList = node.getJobInfoList();
		if (node.getSection(NodeSection.OUTPUT) instanceof Fork) {
			FJList = new LinkedJobInfoList(jobClasses.size());
            FJList.setNetSystem(node.getNetSystem());
			forkNode = new HashMap<Job, NetNode>();
		}
	}

	/**
	 * Assembles split tasks and sends an EVENT_JOIN to reference fork when done.
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
			break;

		case NetEvent.EVENT_JOB:
			job = message.getJob();
			if (job instanceof ForkJob) {
				ForkJob fJob = (ForkJob) job;
				List<ForkJob> fJobs = forkJobs.get(fJob.getForkedJob());
				boolean isFirst = false;
				if (fJobs == null) {
					fJobs = new ArrayList<ForkJob>();
					forkJobs.put(fJob.getForkedJob(), fJobs);
					JobInfo temp = new JobInfo(fJob);
					jobsList.add(temp);
					jobsList.remove(temp);
					isFirst = true;
				}
				fJobs.add(fJob);

				int forked = 0;
				int needed = 0;
				Integer[] neededPerClass = null;
				boolean isFired = false;
				if (isFirst) {
					forked = fJob.getForkedNum();
					needed = joinStrategies[fJob.getForkedJob().getJobClass().getId()].getRequiredNum();
					if (joinStrategies[fJob.getForkedJob().getJobClass().getId()] instanceof GuardJoin) {
						neededPerClass = ((GuardJoin) joinStrategies[fJob.getForkedJob().getJobClass().getId()]).getRequiredPerClass();
						needed = -1;
					}
					requiredPerClass.put(fJob.getForkedJob(), neededPerClass);
					hasFired.put(fJob.getForkedJob(), false);
				} else {
					forked = total.get(fJob.getForkedJob());
					needed = required.get(fJob.getForkedJob());
					neededPerClass = requiredPerClass.get(fJob.getForkedJob());
					isFired = hasFired.get(fJob.getForkedJob());
				}
				forked--;

				if (isFired) {
					JobInfo i = nodeJobsList.lookFor(fJob);
					if (i != null) {
						nodeJobsList.remove(i);
					}
				} else {
					needed--;
					required.put(fJob.getForkedJob(), needed);

					boolean isGuardTrue = false;
					if (neededPerClass != null) {
						neededPerClass[fJob.getJobClass().getId()]--;
						isGuardTrue = true;
						for (Integer i: neededPerClass) {
							isGuardTrue = isGuardTrue && (i <= 0);
						}
					}

					boolean allArrived = (joinStrategies[fJob.getForkedJob().getJobClass().getId()] instanceof NormalJoin)
							&& (forked == 0);
					if (allArrived || needed == 0 || isGuardTrue) {
						for (ForkJob j : forkJobs.get(fJob.getForkedJob())) {
							JobInfo i = nodeJobsList.lookFor(j);
							if (i != null) {
								nodeJobsList.remove(i);
							}
						}

						// Sends job forward
						fJob.getForkedJob().setLastVisitedPair(fJob.getLastVisitedPair());
						sendForward(fJob.getForkedJob(), 0.0);
						hasFired.put(fJob.getForkedJob(), true);

						if (getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork) {
							FJList.add(new JobInfo(fJob.getForkedJob()));
							forkNode.put(fJob.getForkedJob(), fJob.getForkNode());
						} else {
							// Notifies fork that job has been fired
							send(NetEvent.EVENT_JOIN, fJob.getForkedJob(), 0.0, NodeSection.OUTPUT, fJob.getForkNode());
						}
					}
				}

				if (forked > 0) {
					total.put(fJob.getForkedJob(), forked);
				} else {
					forkJobs.remove(fJob.getForkedJob());
					total.remove(fJob.getForkedJob());
					required.remove(fJob.getForkedJob());
					requiredPerClass.remove(fJob.getForkedJob());
					hasFired.remove(fJob.getForkedJob());
				}
			} else {
				// If this is not a fork job, sends it forward
				sendForward(job, 0.0);
				if (getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork) {
					FJList.add(new JobInfo(job));
				}
			}
			send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
			break;

		case NetEvent.EVENT_ACK:
			break;

		case NetEvent.EVENT_JOIN:
			if (!(getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork)) {
				return MSG_NOT_PROCESSED;
			}
			job = (Job) message.getData();
			JobInfo jobInfo = FJList.lookFor(job);
			if (jobInfo != null) {
				FJList.remove(jobInfo);
			}
			NetNode fork = forkNode.get(job);
			if (fork != null) {
				forkNode.remove(job);
				send(NetEvent.EVENT_JOIN, job, 0.0, NodeSection.OUTPUT, fork);
			}
			break;

		case NetEvent.EVENT_JOB_RELEASE:
			break;

		case NetEvent.EVENT_JOB_FINISH:
			break;

		case NetEvent.EVENT_STOP:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		// Everything is okay
		return MSG_PROCESSED;
	}

	@Override
	public void analyzeJoin(int name, JobClass jobClass, Measure measurement) throws NetException {
		switch (name) {
			case SimConstants.UTILIZATION:
				nodeJobsList.analyzeUtilizationJoin(jobClass, measurement);
				break;
			default:
				throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
		}
	}

	@Override
	public void analyzeFJ(int name, JobClass jobClass, Measure measurement) throws NetException {
		switch (name) {
		case SimConstants.FORK_JOIN_NUMBER_OF_JOBS:
			FJList.analyzeQueueLength(jobClass, measurement);
			break;
		case SimConstants.FORK_JOIN_RESPONSE_TIME:
			FJList.analyzeResponseTime(jobClass, measurement);
			break;
		default:
			throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
		}
	}

}
