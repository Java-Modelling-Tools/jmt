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

import jmt.common.exception.NetException;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.random.engine.RandomEngine;

/**
 * This class implements a class switch.
 * 
 * @author Sebatiano Spicuglia, Arif Canakoglu
 */
public class ClassSwitch extends ServiceSection {

	private Float[][] matrix;

	private JobInfoList nodeJobsList;

	public ClassSwitch(Object[] matrix) {
		this.matrix = new Float[matrix.length][matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			Float[] row = (Float[]) matrix[i];
			for (int j = 0; j < row.length; j++) {
				this.matrix[i][j] = row[j];
			}
		}
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		nodeJobsList = node.getJobInfoList();
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
			break;

		case NetEvent.EVENT_JOB:
			Job job = message.getJob();
			JobClass inClass = job.getJobClass();
			int jobClassIn = inClass.getId();
			int jobClassOut = chooseOutClass(matrix[jobClassIn]);
			JobClass outClass = getJobClasses().get(jobClassOut);
			job.setJobClass(outClass);
			JobInfo jobInfo = jobsList.lookFor(job);
			jobsList.getInternalJobInfoList(inClass).remove(jobInfo);
			jobsList.getInternalJobInfoList(outClass).add(jobInfo);
			JobInfo nodeJobInfo = nodeJobsList.lookFor(job);
			nodeJobsList.getInternalJobInfoList(inClass).remove(nodeJobInfo);
			nodeJobsList.getInternalJobInfoList(outClass).add(nodeJobInfo);
			if (!(job instanceof ForkJob)) {
				GlobalJobInfoList global = getOwnerNode().getQueueNet().getJobInfoList();
				global.performJobClassSwitch(inClass, outClass);
			}
			sendForward(NetEvent.EVENT_JOB, job, 0.0);
			sendBackward(NetEvent.EVENT_ACK, job, 0.0);
			break;

		case NetEvent.EVENT_ACK:
			break;

		case NetEvent.EVENT_STOP:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	/**
	 * It choose randomly a position of @row.
	 * Let SUM the sum of all elements of @row.
	 * The probability that this method chooses an
	 * index i is row[i]/SUM.
	 * @param row
	 * @return
	 */
	private int chooseOutClass(Float[] row) {
		float sum;
		float random;
		int i;

		sum = 0;
		for (i = 0; i < row.length; i++) {
			sum += row[i];
		}

        RandomEngine engine = this.getNetSytem().getEngine();
		random = (float) (engine.raw() * sum);
		for (i = 0; i < row.length; i++) {
			random -= row[i];
			if (random <= 0)
				return i;
		}
		return i-1;
	}

}
