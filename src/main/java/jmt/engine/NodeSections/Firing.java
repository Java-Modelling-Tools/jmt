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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
	private TransitionMatrix[] enablingConditions;

	private JobClassList jobClasses;
	private JobInfoList nodeJobsList;
	private GlobalJobInfoList netJobsList;
	private List<Job>[] newJobLists;
	private Iterator<Job>[] newJobIterators;
	private String[] nodeNamePermutation;	

	private boolean[] isForkJobNumWarningShown;

	/**
	 * Creates a new instance of the firing section.
	 */
	public Firing(TransitionMatrix[] firingOutcomes) {
		this.firingOutcomes = firingOutcomes;
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		enablingConditions = ((Enabling) node.getSection(NodeSection.INPUT)).getEnablingConditions();
		jobClasses = getJobClasses();
		nodeJobsList = node.getJobInfoList();
		newJobLists = new List[jobClasses.size()];
		for (int i = 0; i < newJobLists.length; i++) {
			newJobLists[i] = new ArrayList<Job>();
		}
		newJobIterators = new Iterator[jobClasses.size()];
		Arrays.fill(newJobIterators, null);
		nodeNamePermutation = firingOutcomes[0].keySet().toArray(new String[0]);		
		isForkJobNumWarningShown = new boolean[firingOutcomes.length];
		Arrays.fill(isForkJobNumWarningShown, false);
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Object data = message.getData();
        NetSystem netSystem = message.getDestination().getNetSystem();

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
			String modeName = ((FiringPacket) data).getModeName();
			double[] totalLifetimes = ((FiringPacket) data).getTotalLifetimes();
			List<ForkJobInfo>[] forkJobInfoLists = ((FiringPacket) data).getForkJobInfoLists();
			for (int i = 0; i < jobClasses.size(); i++) {
				int releasedJobNumberForClass = firingOutcomes[modeIndex].getTotal(i);
				if (releasedJobNumberForClass < forkJobInfoLists[i].size()) {
					// Reservoir Sampling
					ForkJobInfo[] pickedForkJobInfos = new ForkJobInfo[releasedJobNumberForClass];
					for (int j = 0; j < releasedJobNumberForClass; j++) {
						pickedForkJobInfos[j] = forkJobInfoLists[i].get(j);
					}
					RandomEngine randomEngine = this.getNetSytem().getEngine();
					for (int j = releasedJobNumberForClass; j < forkJobInfoLists[i].size(); j++) {
						int k = (int) Math.floor(randomEngine.raw() * (j + 1));
						if (k < releasedJobNumberForClass) {
							pickedForkJobInfos[k] = forkJobInfoLists[i].get(j);
						}
					}

					for (int j = 0; j < releasedJobNumberForClass; j++) {
						ForkJob newForkJob = new ForkJob(jobClasses.get(i), pickedForkJobInfos[j]);
                        newForkJob.initialize(netSystem); 
						newJobLists[i].add(newForkJob);
					}
					showForkJobNumWarning(modeIndex, modeName);
				} else if (releasedJobNumberForClass == forkJobInfoLists[i].size()) {
					for (int j = 0; j < releasedJobNumberForClass; j++) {
						ForkJob newForkJob = new ForkJob(jobClasses.get(i), forkJobInfoLists[i].get(j));
                        newForkJob.initialize(netSystem); 
						newJobLists[i].add(newForkJob);
					}
				} else {
					for (int j = 0; j < forkJobInfoLists[i].size(); j++) {
						ForkJob newForkJob = new ForkJob(jobClasses.get(i), forkJobInfoLists[i].get(j));
                        newForkJob.initialize(netSystem); 
						newJobLists[i].add(newForkJob);
					}

					double averageEnteringTime = netSystem.getTime()
							- (totalLifetimes[i] / (releasedJobNumberForClass - forkJobInfoLists[i].size()));
					for (int j = forkJobInfoLists[i].size(); j < releasedJobNumberForClass; j++) {
						Job newJob = new Job(jobClasses.get(i), netJobsList);
                        newJob.initialize(netSystem); 
						updateVisitPath(newJob);
						newJob.setSystemEnteringTime(averageEnteringTime);
						newJobLists[i].add(newJob);
					}
				}

				if (forkJobInfoLists[i].size() > 0) {
					RandomEngine randomEngine = this.getNetSytem().getEngine();
					// Durstenfeld Shuffle
					for (int j = newJobLists[i].size() - 1; j > 0; j--) {
						int k = (int) Math.floor(randomEngine.raw() * (j + 1));
						Job newJob = (Job) newJobLists[i].get(j);
						newJobLists[i].set(j, newJobLists[i].get(k));
						newJobLists[i].set(k, newJob);
					}
				}
				newJobIterators[i] = newJobLists[i].iterator();
			}

			RandomEngine randomEngine = this.getNetSytem().getEngine();
			// Durstenfeld Shuffle
			for (int i = nodeNamePermutation.length - 1; i > 0; i--) {
				int j = (int) Math.floor(randomEngine.raw() * (i + 1));
				String nodeName = nodeNamePermutation[i];
				nodeNamePermutation[i] = nodeNamePermutation[j];
				nodeNamePermutation[j] = nodeName;
			}
			for (String nodeName : nodeNamePermutation) {
				int releasedJobNumberToNode = firingOutcomes[modeIndex].getTotal(nodeName);
				if (releasedJobNumberToNode <= 0) {
					continue;
				}
				send(NetEvent.EVENT_JOB_RELEASE, null, 0.0, NodeSection.INPUT, netSystem.getNode(nodeName));
				Job[] jobPermutation = new Job[releasedJobNumberToNode];                
				int index = 0;
				for (int i = 0; i < jobClasses.size(); i++) {
					int releasedJobNumber = firingOutcomes[modeIndex].getEntry(nodeName, i);
					for (int j = 0; j < releasedJobNumber; j++) {
						Job job = newJobIterators[i].next();
                        jobPermutation[index] = job;
						index++;
					}
				}
                
				// Durstenfeld Shuffle
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
						int jobClassId = job.getJobClass().getId();
						if (enablingConditions[modeIndex].getTotal(jobClassId) <= forkJobInfoLists[jobClassId].size()) {
							netJobsList.addJob(job);
						} else {
							netJobsList.produceJob(job);
						}
					}
					send(job, 0.0, netSystem.getNode(nodeName));
				}
				send(NetEvent.EVENT_JOB_FINISH, null, 0.0, NodeSection.INPUT, netSystem.getNode(nodeName));
			}

			for (int i = 0; i < newJobLists.length; i++) {
				newJobLists[i].clear();
			}
			Arrays.fill(newJobIterators, null);
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
	private void showForkJobNumWarning(int modeIndex, String modeName) {
		if (!isForkJobNumWarningShown[modeIndex]) {
			if (getOwnerNode().getQueueNet().isTerminalSimulation()) {
				System.out.println("JSIMengine - Warning: " + getOwnerNode().getName()
						+ " failed to maintain the equilibrium between input and output tasks in "
						+ modeName + ".");
			} else {
				class OneShotTask implements Runnable {
					String modeName;
					public OneShotTask(String modeName) {
						this.modeName = modeName;
					}
					@Override
					public void run() {
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
						}
						JOptionPane.showMessageDialog(null, getOwnerNode().getName()
								+ " failed to maintain the equilibrium between input and output tasks in "
								+ modeName + ".", "JSIMengine - Warning", JOptionPane.WARNING_MESSAGE);
					}
				}
				(new Thread(new OneShotTask(modeName))).start();
			}
			isForkJobNumWarningShown[modeIndex] = true;
		}
	}

	/**
	 * Gets the firing outcomes of this section.
	 * @return the firing outcomes of this section.
	 */
	public TransitionMatrix[] getFiringOutcomes() {
		return firingOutcomes;
	}

}
