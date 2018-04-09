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

import javax.swing.JOptionPane;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.TransitionUtilities.EnablingPacket;
import jmt.engine.NetStrategies.TransitionUtilities.FiringPacket;
import jmt.engine.NetStrategies.TransitionUtilities.TransitionMatrix;
import jmt.engine.NetStrategies.TransitionUtilities.TransitionVector;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.ForkJobInfo;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.NodeSection;

/**
 * <p>Title: Enabling</p>
 * <p>Description: This class implements the enabling section.</p>
 *
 * @author Lulai Zhu
 * Date: 15-07-2016
 * Time: 21.00.00
 */
public class Enabling extends InputSection {

	private TransitionMatrix[] enablingConditions;
	private TransitionMatrix[] inhibitingConditions;

	private JobClassList jobClasses;
	private GlobalJobInfoList netJobsList;
	private TransitionMatrix storageSituation;
	private int[] enablingDegrees;
	private int currentModeIndex;
	private int unfinishedRequests;
	private double[] totalLifetime;
	private ForkJobInfo[] forkJobInfo;
	private int[] forkJobNum;
	private boolean isForkJobInfoWarningShown;
	private int consumedJobs;
	private int[] consumedJobsPerClass;

	/**
	 * Creates a new instance of the enabling section.
	 */
	public Enabling(TransitionMatrix[] enablingConditions, TransitionMatrix[] inhibitingConditions) {
		super(true);
		this.enablingConditions = enablingConditions;
		this.inhibitingConditions = inhibitingConditions;
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		jobClasses = getJobClasses();
		storageSituation = new TransitionMatrix(jobClasses.size());
		for (String nodeName : enablingConditions[0].keySet()) {
			storageSituation.setVector(new TransitionVector(nodeName, jobClasses.size()));
		}
		enablingDegrees = new int[enablingConditions.length];
		Arrays.fill(enablingDegrees, 0);
		currentModeIndex = -1;
		unfinishedRequests = 0;
		totalLifetime = new double[jobClasses.size()];
		Arrays.fill(totalLifetime, 0.0);
		forkJobInfo = new ForkJobInfo[jobClasses.size()];
		Arrays.fill(forkJobInfo, null);
		forkJobNum = new int[jobClasses.size()];
		Arrays.fill(forkJobNum, 0);
		isForkJobInfoWarningShown = false;
		consumedJobs = 0;
		consumedJobsPerClass = new int[jobClasses.size()];
		Arrays.fill(consumedJobsPerClass, 0);
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Object data = message.getData();
		NetNode source = message.getSource();

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
		{
			netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
			break;
		}

		case NetEvent.EVENT_JOB:
		{
			Job job = message.getJob();
			JobClass jobClass = job.getJobClass();
			totalLifetime[jobClass.getId()] += NetSystem.getTime() - job.getSystemEnteringTime();
			if (job instanceof ForkJob) {
				ForkJob fJob = (ForkJob) job;
				if (forkJobInfo[jobClass.getId()] == null) {
					forkJobInfo[jobClass.getId()] = fJob.getForkJobInfo();
				} else if (fJob.getForkJobInfo() != forkJobInfo[jobClass.getId()]) {
					showForkJobInfoWarning();
				}
				forkJobNum[jobClass.getId()]++;
			} else {
				netJobsList.consumeJob(job);
				consumedJobs++;
				consumedJobsPerClass[jobClass.getId()]++;
			}
			sendAckAfterConsume(job, 0.0, NodeSection.OUTPUT, source);
			break;
		}

		case NetEvent.EVENT_ACK:
			break;

		case NetEvent.EVENT_JOB_CHANGE:
		{
			TransitionVector storageVector = (TransitionVector) data;
			for (int i = 0; i < jobClasses.size(); i++) {
				storageSituation.setEntry(storageVector.getKey(), i, storageVector.getEntry(i));
			}

			for (int i = 0; i < enablingConditions.length; i++) {
				if (i == currentModeIndex) {
					continue;
				}

				int enablingDegree = -1;
				OUTER_LOOP:
				for (String nodeName : enablingConditions[i].keySet()) {
					if (enablingConditions[i].getTotal(nodeName) > 0) {
						for (int j = 0; j < jobClasses.size(); j++) {
							int availableJobs = storageSituation.getEntry(nodeName, j);
							int requiredJobsForEnabling = enablingConditions[i].getEntry(nodeName, j);
							if (requiredJobsForEnabling > 0) {
								if (enablingDegree < 0) {
									enablingDegree = availableJobs / requiredJobsForEnabling;
								} else {
									enablingDegree = Math.min(enablingDegree, availableJobs / requiredJobsForEnabling);
								}
								if (enablingDegree == 0) {
									break OUTER_LOOP;
								}
							}
						}
					}
					if (inhibitingConditions[i].getTotal(nodeName) > 0) {
						for (int j = 0; j < jobClasses.size(); j++) {
							int availableJobs = storageSituation.getEntry(nodeName, j);
							int requiredJobsForInhibiting = inhibitingConditions[i].getEntry(nodeName, j);
							if (requiredJobsForInhibiting > 0 && availableJobs >= requiredJobsForInhibiting) {
								enablingDegree = 0;
								break OUTER_LOOP;
							}
						}
					}
				}

				if (enablingDegree >= 0 && enablingDegree != enablingDegrees[i]) {
					enablingDegrees[i] = enablingDegree;
					EnablingPacket packet = new EnablingPacket(i, enablingDegree);
					sendForward(NetEvent.EVENT_ENABLING, packet, 0.0);
				}
			}
			break;
		}

		case NetEvent.EVENT_FIRING:
		{
			int modeIndex = ((Integer) data).intValue();
			if (enablingDegrees[modeIndex] <= 0) {
				return MSG_NOT_PROCESSED;
			}
			enablingDegrees[modeIndex]--;
			currentModeIndex = modeIndex;
			for (String nodeName : enablingConditions[modeIndex].keySet()) {
				int requiredJobsFromNode = enablingConditions[modeIndex].getTotal(nodeName);
				if (requiredJobsFromNode > 0) {
					send(NetEvent.EVENT_JOB_REQUEST, enablingConditions[modeIndex].getVector(nodeName), 0.0,
							NodeSection.OUTPUT, NetSystem.getNode(nodeName));
					unfinishedRequests++;
				}
			}
			Arrays.fill(totalLifetime, 0.0);
			Arrays.fill(forkJobInfo, null);
			Arrays.fill(forkJobNum, 0);
			break;
		}

		case NetEvent.EVENT_JOB_FINISH:
		{
			if (unfinishedRequests <= 0) {
				return MSG_NOT_PROCESSED;
			}
			unfinishedRequests--;
			if (unfinishedRequests == 0) {
				FiringPacket packet = new FiringPacket(currentModeIndex, totalLifetime, forkJobInfo, forkJobNum);
				sendForward(NetEvent.EVENT_FIRING, packet, 0.0);
				currentModeIndex = -1;
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
	 * Shows the fork job information warning.
	 */
	private void showForkJobInfoWarning() {
		if (!isForkJobInfoWarningShown) {
			if (getOwnerNode().getQueueNet().isTerminalSimulation()) {
				System.out.println("JSIMengine - Warning: " + getOwnerNode().getName()
						+ " failed to maintain the dependency between input tasks and their parent job.");
			} else {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
						}
						JOptionPane.showMessageDialog(null, getOwnerNode().getName()
								+ " failed to maintain the dependency between input tasks and their parent job.",
								"JSIMengine - Warning", JOptionPane.WARNING_MESSAGE);
					}
				}).start();
			}
			isForkJobInfoWarningShown = true;
		}
	}

	/**
	 * Gets the total number of consumed jobs.
	 * @return the total number of consumed jobs.
	 */
	public int getConsumedJobs() {
		return consumedJobs;
	}

	/**
	 * Gets the number of consumed jobs for the specified class.
	 * @param classIndex the index of the job class.
	 * @return the number of consumed jobs for the specified class.
	 */
	public int getConsumedJobsPerClass(int jobClass) {
		return consumedJobsPerClass[jobClass];
	}

}
