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
import jmt.engine.NetStrategies.SemaphoreStrategy;
import jmt.engine.NetStrategies.SemaphoreStrategies.NormalSemaphore;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;

/**
 * <p>Title: Semaphore</p>
 * <p>Description: This class is a Semaphore input section, used to gather previously
 *  forked tasks. Tasks are kept until the threshold is reached. If a non-fragmented
 *  job is received, it is simply routed to service section.</p>
 *
 * @author Vitor S. Lopes
 *         Date: 25-jul-2016
 *         Time: 11.37.58
 */
public class Semaphore extends InputSection {

	/** Data structure used to store received tasks for each job */
	private HashMap<Job, List<ForkJob>> forkJobs;
	private HashMap<Job, Integer> total;
	private HashMap<Job, Integer> required;
	private HashMap<Job, Boolean> hasFired;

	private SemaphoreStrategy[] semaphoreStrategies;

	// --- Constructors -----------------------------------------------------------------------------

	/**
	 * Constructs a new Semaphore.
	 */
	public Semaphore() {
		// Disables automatic handling of jobinfolist
		super(false);
		forkJobs = new HashMap<Job, List<ForkJob>>();
		total = new HashMap<Job, Integer>();
		required = new HashMap<Job, Integer>();
		hasFired = new HashMap<Job, Boolean>();
	}

	public Semaphore(SemaphoreStrategy[] semaphoreStrategies) {
		this();
		this.semaphoreStrategies = semaphoreStrategies;
	}

	// ----------------------------------------------------------------------------------------------

	/**
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
				boolean isFired = false;
				if (isFirst) {
					forked = fJob.getForkedNum();
					needed = ((NormalSemaphore) semaphoreStrategies[fJob.getForkedJob().getJobClass().getId()]).getThreshold();
					if (needed > forked) {
						needed = forked;
					}
					hasFired.put(fJob.getForkedJob(), false);
				} else {
					forked = total.get(fJob.getForkedJob());
					needed = required.get(fJob.getForkedJob());
					isFired = hasFired.get(fJob.getForkedJob());
				}

				if (isFired) {
					sendForward(fJob, 0.0);
				} else {
					needed--;
					required.put(fJob.getForkedJob(), needed);

					if (needed == 0) {
						for (ForkJob j : forkJobs.get(fJob.getForkedJob())) {
							sendForward(j, 0.0);
						}
						hasFired.put(fJob.getForkedJob(), true);
					}
				}

				if (forked > 0) {
					total.put(fJob.getForkedJob(), forked);
				} else {
					forkJobs.remove(fJob.getForkedJob());
					total.remove(fJob.getForkedJob());
					required.remove(fJob.getForkedJob());
					hasFired.remove(fJob.getForkedJob());
				}
			} else {
				sendForward(job, 0.0);
			}
			send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
			break;

		case NetEvent.EVENT_ACK:
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

}
