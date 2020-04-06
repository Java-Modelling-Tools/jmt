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
import java.util.List;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.TransitionUtilities.EnablingPacket;
import jmt.engine.NetStrategies.TransitionUtilities.FiringPacket;
import jmt.engine.NetStrategies.TransitionUtilities.TransitionMatrix;
import jmt.engine.NetStrategies.TransitionUtilities.TransitionVector;
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
	private TransitionMatrix[] firingOutcomes;

	private JobClassList jobClasses;
	private JobInfoList nodeJobsList;
	private GlobalJobInfoList netJobsList;
	private TransitionMatrix storageSituation;
	private int[] enablingDegrees;
	private int currentModeIndex;
	private String currentModeName;
	private int unfinishedRequestNumber;
	private double[] totalLifetimes;
	private List<ForkJobInfo>[] forkJobInfoLists;

	/**
	 * Creates a new instance of the enabling section.
	 */
	public Enabling(TransitionMatrix[] enablingConditions, TransitionMatrix[] inhibitingConditions) {
		this.enablingConditions = enablingConditions;
		this.inhibitingConditions = inhibitingConditions;
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		firingOutcomes = ((Firing) node.getSection(NodeSection.OUTPUT)).getFiringOutcomes();
		jobClasses = getJobClasses();
		nodeJobsList = node.getJobInfoList();
		storageSituation = new TransitionMatrix(jobClasses.size());
		for (String nodeName : enablingConditions[0].keySet()) {
			storageSituation.setVector(new TransitionVector(nodeName, jobClasses.size()));
		}
		enablingDegrees = new int[enablingConditions.length];
		Arrays.fill(enablingDegrees, 0);
		currentModeIndex = -1;
		currentModeName = null;
		unfinishedRequestNumber = 0;
		totalLifetimes = new double[jobClasses.size()];
		Arrays.fill(totalLifetimes, 0.0);
		forkJobInfoLists = new List[jobClasses.size()];
		for (int i = 0; i < forkJobInfoLists.length; i++) {
			forkJobInfoLists[i] = new ArrayList<ForkJobInfo>();
		}
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Object data = message.getData();
		NetNode source = message.getSource();		
        NetSystem netSystem = source.getNetSystem();

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
		{
			netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
			sendMe(NetEvent.EVENT_JOB_CHANGE, null, 0.0);
			break;
		}

		case NetEvent.EVENT_JOB:
		{
			Job job = (Job) data;
			if (job instanceof ForkJob) {
				int jobClassId = job.getJobClass().getId();
				forkJobInfoLists[jobClassId].add(((ForkJob) job).getForkJobInfo());
			}
			break;
		}

		case NetEvent.EVENT_ACK:
			return MSG_NOT_PROCESSED;

		case NetEvent.EVENT_JOB_CHANGE:
		{
			if (data != null) {
				TransitionVector storageVector = (TransitionVector) data;
				for (int i = 0; i < jobClasses.size(); i++) {
					storageSituation.setEntry(storageVector.getKey(), i, storageVector.getEntry(i));
				}
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
							int availableJobNumber = storageSituation.getEntry(nodeName, j);
							int requiredJobNumber = enablingConditions[i].getEntry(nodeName, j);
							if (requiredJobNumber > 0) {
								if (enablingDegree < 0) {
									enablingDegree = availableJobNumber / requiredJobNumber;
								} else {
									enablingDegree = Math.min(enablingDegree, availableJobNumber / requiredJobNumber);
								}
								if (enablingDegree == 0) {
									break OUTER_LOOP;
								}
							}
						}
					}
					if (inhibitingConditions[i].getTotal(nodeName) > 0) {
						for (int j = 0; j < jobClasses.size(); j++) {
							int availableJobNumber = storageSituation.getEntry(nodeName, j);
							int requiredJobNumber = inhibitingConditions[i].getEntry(nodeName, j);
							if (requiredJobNumber > 0 && availableJobNumber >= requiredJobNumber) {
								enablingDegree = 0;
								break OUTER_LOOP;
							}
						}
					}
				}

				if (enablingDegree != enablingDegrees[i]) {
					enablingDegrees[i] = enablingDegree;
					EnablingPacket packet = new EnablingPacket(i, enablingDegree);
					sendForward(NetEvent.EVENT_ENABLING, packet, 0.0);
				}
			}
			break;
		}

		case NetEvent.EVENT_FIRING:
		{
			int modeIndex = ((FiringPacket) data).getModeIndex();
			String modeName = ((FiringPacket) data).getModeName();
			if (enablingDegrees[modeIndex] < 0) {
				FiringPacket packet = new FiringPacket(modeIndex, modeName, totalLifetimes, forkJobInfoLists);
				sendForward(NetEvent.EVENT_FIRING, packet, 0.0);
			} else if (enablingDegrees[modeIndex] == 0) {
				return MSG_NOT_PROCESSED;
			} else {
				enablingDegrees[modeIndex]--;
				currentModeIndex = modeIndex;
				currentModeName = modeName;
				for (String nodeName : enablingConditions[modeIndex].keySet()) {
					int requiredJobNumberFromNode = enablingConditions[modeIndex].getTotal(nodeName);
					if (requiredJobNumberFromNode > 0) {
						send(NetEvent.EVENT_JOB_REQUEST, enablingConditions[modeIndex].getVector(nodeName), 0.0,
									NodeSection.OUTPUT, netSystem.getNode(nodeName));
						unfinishedRequestNumber++;
					}
				}
			}

			Arrays.fill(totalLifetimes, 0.0);
			for (int i = 0; i < forkJobInfoLists.length; i++) {
				forkJobInfoLists[i].clear();
			}
			break;
		}

		case NetEvent.EVENT_JOB_FINISH:
		{
			if (unfinishedRequestNumber <= 0) {
				return MSG_NOT_PROCESSED;
			}

			unfinishedRequestNumber--;
			if (unfinishedRequestNumber == 0) {
				List<JobInfo> jobInfoList = jobsList.getInternalJobInfoList();
				while (!jobInfoList.isEmpty()) {
					JobInfo jobInfo = jobInfoList.get(0);
					Job job = jobInfo.getJob();
					JobInfo nodeJobInfo = nodeJobsList.lookFor(job);
					jobsList.consumeJob(jobInfo);
					nodeJobsList.consumeJob(nodeJobInfo);
					if (!(job instanceof ForkJob)) {
						int jobClassId = job.getJobClass().getId();
						if (firingOutcomes[currentModeIndex].getTotal(jobClassId) <= forkJobInfoLists[jobClassId].size()) {
							netJobsList.removeJob(job);
						} else {
							totalLifetimes[jobClassId] += netSystem.getTime() - job.getSystemEnteringTime();
							netJobsList.consumeJob(job);
						}
					}
				}

				FiringPacket packet = new FiringPacket(currentModeIndex, currentModeName, totalLifetimes, forkJobInfoLists);
				sendForward(NetEvent.EVENT_FIRING, packet, 0.0);
				currentModeIndex = -1;
				currentModeName = null;
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
	 * Gets the enabling conditions of this section.
	 * @return the enabling conditions of this section.
	 */
	public TransitionMatrix[] getEnablingConditions() {
		return enablingConditions;
	}

}
