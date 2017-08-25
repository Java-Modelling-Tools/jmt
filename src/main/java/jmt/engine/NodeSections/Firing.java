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

import javax.swing.JOptionPane;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.TransitionUtilities.FiringPacket;
import jmt.engine.NetStrategies.TransitionUtilities.TransitionMatrix;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.ForkJobInfo;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.random.engine.RandomEngine;

/**
 * <p>Title: Firing</p>
 * <p>Description: This class implements the firing section.</p>
 *
 * @author Lulai Zhu
 * Date: 15-07-2016
 * Time: 21.00.00
 */
public class Firing extends OutputSection {

	private TransitionMatrix[] firingOutcomes;

	private JobClassList jobClasses;
	private JobInfoList nodeJobsList;
	private GlobalJobInfoList netJobsList;
	private boolean isForkJobNumWarningShown;
	private RandomEngine randomEngine;
	private int producedJobs;
	private int[] producedJobsPerClass;

	/**
	 * Creates a new instance of the firing section.
	 */
	public Firing(TransitionMatrix[] firingOutcomes) {
		super(true);
		this.firingOutcomes = firingOutcomes;
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		jobClasses = getJobClasses();
		nodeJobsList = node.getJobInfoList();
		isForkJobNumWarningShown = false;
		randomEngine = RandomEngine.makeDefault();
		producedJobs = 0;
		producedJobsPerClass = new int[jobClasses.size()];
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Object data = message.getData();

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
		{
			netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
			break;
		}

		case NetEvent.EVENT_JOB:
			return MSG_NOT_PROCESSED;

		case NetEvent.EVENT_ACK:
			break;

		case NetEvent.EVENT_FIRING:
		{
			int modeIndex = ((FiringPacket) data).getModeIndex();
			double[] totalLifetime = ((FiringPacket) data).getTotalLifetime();
			ForkJobInfo[] forkJobInfo = ((FiringPacket) data).getForkJobInfo();
			int[] forkJobNum = ((FiringPacket) data).getForkJobNum();
			double[] avgEnteringTime = new double[jobClasses.size()];
			for (int i = 0; i < jobClasses.size(); i++) {
				int releasedJobs = firingOutcomes[modeIndex].getTotal(i);
				avgEnteringTime[i] = NetSystem.getTime() - (totalLifetime[i] / releasedJobs);
				if (forkJobNum[i] > 0 && releasedJobs != forkJobNum[i]) {
					showForkJobNumWarning();
				}
			}

			for (String nodeName : firingOutcomes[modeIndex].keySet()) {
				int releasedJobsToNode = firingOutcomes[modeIndex].getTotal(nodeName);
				if (releasedJobsToNode > 0) {
					send(NetEvent.EVENT_JOB_RELEASE, null, 0.0, NodeSection.INPUT, NetSystem.getNode(nodeName));
					// Releases jobs in the random order
					Job[] jobPermutation = new Job[releasedJobsToNode];
					int index = 0;
					for (int i = 0; i < jobClasses.size(); i++) {
						int releasedJobs = firingOutcomes[modeIndex].getEntry(nodeName, i);
						for (int j = 0; j < releasedJobs; j++) {
							Job job = null;
							if (forkJobInfo[i] != null) {
								job = new ForkJob(jobClasses.get(i), jobClasses.size(), forkJobInfo[i]);
							} else {
								job = new Job(jobClasses.get(i), jobClasses.size());
							}
							job.setSystemEnteringTime(avgEnteringTime[i]);
							jobPermutation[index] = job;
							index++;
						}
					}

					for (int i = jobPermutation.length - 1; i > 0; i--) {
						int j = (int) Math.floor(randomEngine.raw() * (i + 1));
						Job job = jobPermutation[i];
						jobPermutation[i] = jobPermutation[j];
						jobPermutation[j] = job;
					}

					for (int i = 0; i < jobPermutation.length; i++) {
						Job job = jobPermutation[i];
						JobInfo jobInfo = new JobInfo(job);
						jobsList.produceJob(jobInfo);
						nodeJobsList.produceJob(jobInfo);
						if (!(job instanceof ForkJob)) {
							netJobsList.produceJob(job);
							producedJobs++;
							producedJobsPerClass[job.getJobClass().getId()]++;
						}
						send(job, 0.0, NetSystem.getNode(nodeName));
					}
					send(NetEvent.EVENT_JOB_FINISH, null, 0.0, NodeSection.INPUT, NetSystem.getNode(nodeName));
				}
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
	 * Shows the fork job number warning.
	 */
	private void showForkJobNumWarning() {
		if (!isForkJobNumWarningShown) {
			if (getOwnerNode().getQueueNet().isTerminalSimulation()) {
				System.out.println("JMT - Warning: " + getOwnerNode().getName()
						+ " failed to maintain the equilibrium between input and output tasks.");
			} else {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
						}
						JOptionPane.showMessageDialog(null, getOwnerNode().getName()
								+ " failed to maintain the equilibrium between input and output tasks.",
								"JMT - Warning", JOptionPane.WARNING_MESSAGE);
					}
				}).start();
			}
			isForkJobNumWarningShown = true;
		}
	}

	/**
	 * Gets the total number of produced jobs.
	 * @return the total number of produced jobs.
	 */
	public int getProducedJobs() {
		return producedJobs;
	}

	/**
	 * Gets the number of produced jobs for the specified class.
	 * @param classIndex the index of the job class.
	 * @return the number of produced jobs for the specified class.
	 */
	public int getProducedJobsPerClass(int jobClass) {
		return producedJobsPerClass[jobClass];
	}

}
