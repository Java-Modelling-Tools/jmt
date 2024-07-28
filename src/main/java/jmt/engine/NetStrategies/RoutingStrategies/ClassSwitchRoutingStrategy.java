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

package jmt.engine.NetStrategies.RoutingStrategies;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.engine.NetStrategies.RoutingStrategy;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.random.Empirical;
import jmt.engine.random.EmpiricalPar;
import jmt.engine.random.engine.RandomEngine;

public class ClassSwitchRoutingStrategy extends RoutingStrategy {

	// the stations that a job can be routed to
	private ClassSwitchRoutingParameter[] stations;
	// station count
	private int stationCount;
	// the empirical distribution
	private Empirical distribution;
	// the parameter of the empirical distribution
	private EmpiricalPar param;
	// whether routing is disabled (routing probabilities sum to 0)
	private boolean disabled;

	public ClassSwitchRoutingStrategy(ClassSwitchRoutingParameter[] entries) throws IncorrectDistributionParameterException {
		this.stations = entries;
		this.stationCount = stations.length;
		double[] probabilities = new double[entries.length];
		double probabilitySum = 0.0;
		for (int i = 0; i < entries.length; i++) {
			ClassSwitchRoutingParameter entry = entries[i];
			probabilities[i] = entry.getDestProbability();
			probabilitySum += probabilities[i];
		}

		// check if probabilities sum to zero (if so, routing is disabled for the class)
		disabled = (probabilitySum == 0.0 || stationCount == 0);
		// creates distribution to map to the next station chosen
		distribution = new Empirical();
		param = new EmpiricalPar(probabilities);
	}

	/**
	 * This method should be overridden to implement a specific strategy.
	 * @param ownerNode Owner node of the output section.
	 * @param jobClass Class of current job to be routed.
	 * @return Selected node.
	 */
	@Override
	@Deprecated
	public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		return null;
	}

	@Override
	public NetNode getOutNode(NodeSection section, Job job) {
		// no possible out node if routing disabled
		if (disabled) {
			return null;
		}

		ClassSwitchRoutingParameter pathStation = null;
		// pick a path based on the probabilities
		try {
			// the empirical distribution returns the position of the chosen output node
			if (stationCount == 1) {
				pathStation = stations[0];
			} else {
				int nodePos = (int) Math.round(distribution.nextRand(param));
				pathStation = stations[nodePos];
			}
		} catch (IncorrectDistributionParameterException e) {
			e.printStackTrace();
		}

		// now switch the job class before passing on the job
		JobClass fromClass = job.getJobClass();
		JobClass toClass = section.getJobClasses().get(pathStation.getToClass());
		NetNode node = section.getOwnerNode();
		if (fromClass != toClass) {
			job.setNextJobClass(toClass);
			JobInfoList jobsList = section.getJobInfoList();
			JobInfo jobInfo = jobsList.lookFor(job);
			jobsList.switchJob(jobInfo);
			JobInfoList nodeJobsList = node.getJobInfoList();
			JobInfo nodeJobInfo = nodeJobsList.lookFor(job);
			nodeJobsList.switchJob(nodeJobInfo);
			if (!(job instanceof ForkJob)) {
				GlobalJobInfoList netJobsList = node.getQueueNet().getJobInfoList();
				netJobsList.performJobClassSwitch(fromClass, toClass);
			}
			job.setJobClass(toClass);
		}

		return node.getNetSystem().getNode(pathStation.getDestNode());
	}

	public void setRandomEngine(RandomEngine randomEngine) {
		distribution.setRandomEngine(randomEngine);
		// Propagate random engine to the CSRP nodes contained
		for (ClassSwitchRoutingParameter station : stations) {
			station.setRandomEngine(randomEngine);
		}
	}

}
