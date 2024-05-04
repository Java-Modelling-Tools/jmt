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

import jmt.engine.NodeSections.Server;
import jmt.engine.dataAnalysis.Measure;
import jmt.engine.simEngine.Simulation;

import java.util.List;

/**
 * This class implements a linked job info list used by processor-sharing.
 * @author Lulai Zhu
 */
public class PSJobInfoList extends LinkedJobInfoList {

	/**
	 * Creates a new JobInfoList instance.
	 * @param jobClasses number of job classes.
	 */
	public PSJobInfoList(JobClassList jobClasses) {
		super(jobClasses);
	}

	/**
	 * Creates a new JobInfoList instance.
	 * @param jobClasses job classes.
	 * @param classCompatibilities classcompatibilities for station
	 * @param stationName station name
	 */
	public PSJobInfoList(JobClassList jobClasses, Boolean[][] classCompatibilities, String stationName, List<Server.ServerType> serverTypes
	) {
		super(jobClasses,classCompatibilities,stationName, serverTypes);
	}

	@Override
	protected void updateAdd(JobInfo jobInfo) {
		int c = jobInfo.getJob().getJobClass().getId();
		jobsIn++;
		jobsInPerClass[c]++;
		lastJobInTime = getTime();
		lastJobInTimePerClass[c] = getTime();
	}

	@Override
	protected void doRemove(JobInfo jobInfo, int position, int perClassPosition) {
		int c = jobInfo.getJob().getJobClass().getId();
		finalRemove(jobInfo, list, position);
		finalRemove(jobInfo, listPerClass[c], perClassPosition);
		jobsOut++;
		jobsOutPerClass[c]++;
		lastJobOutTime = getTime();
		lastJobOutTimePerClass[c] = getTime();
	}

	public void psUpdateSojournTime(JobClass jobClass, double serviceTime) {
		int c = jobClass.getId();
		totalSojournTime += serviceTime;
		totalSojournTimePerClass[c] += serviceTime;
		lastJobSojournTime = serviceTime;
		lastJobSojournTimePerClass[c] = serviceTime;
	}

	public void psUpdateQueueTime(JobClass jobClass, double queueTime) {
		if (residenceTimePerClass != null) {
			int c = jobClass.getId();
			Measure m = residenceTimePerClass[c];
			if (m != null) {
				m.update(queueTime, 1.0);
			}
		}
		if (residenceTime != null) {
			residenceTime.update(queueTime, 1.0);
		}
	}

	public void psUpdateUtilization(JobClass jobClass, double[] serviceFractions) {
		if (utilizationPerClass != null) {
			for (int c = 0; c < numberOfJobClasses; c++) {
				Measure m = utilizationPerClass[c];
				if (m != null) {
					if (jobsInPerClass[c] == 0) {
						if (c == jobClass.getId()) {
							m.update(0.0, getTime());
						}
					} else {
						m.update(listPerClass[c].size() * serviceFractions[c], getTime() - getLastModifyTime());
					}
				}
			}
		}
		if (utilization != null) {
			double sum = 0.0;
			for (int c = 0; c < numberOfJobClasses; c++) {
				sum += listPerClass[c].size() * serviceFractions[c];
			}
			utilization.update(sum, getTime() - getLastModifyTime());
		}
	}

	public void psUpdateUtilizationPerServerType(double[][] serviceFractionsPerServerType, int[][] assignedJobs) {
		if (utilizationPerServerType != null) {
			for (int s = 0; s < serviceFractionsPerServerType.length; s++) {
				Measure m = utilizationPerServerType[s];
				if (m != null) {
					double sample = 0;
					for (int c = 0; c < numberOfJobClasses; c++) {
						sample += assignedJobs[s][c] * serviceFractionsPerServerType[s][c];
					}
					m.update(sample, getTime() - getLastModifyTime());
				}
			}
		}

		if (utilizationPerServerTypePerClass != null) {
			for (int s = 0; s < serviceFractionsPerServerType.length; s++) {
				for (int c = 0; c < numberOfJobClasses; c++) {
					Measure m = utilizationPerServerTypePerClass[s][c];
					if (m != null) {
						m.update(assignedJobs[s][c] * serviceFractionsPerServerType[s][c], getTime() - getLastModifyTime());
					}
				}
			}
		}
	}

}
