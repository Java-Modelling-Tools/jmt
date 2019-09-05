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

/**
 * <p>Title: Fork Job Info</p>
 * <p>Description: This class implements the fork job info.</p>
 *
 * @author Lulai Zhu
 * Date: 06-10-2016
 * Time: 14.00.00
 */
public class ForkJobInfo {

	/** Reference to parent job */
	protected Job forkedJob;
	/** Number of child jobs */
	protected int forkedNum;
	/** Reference to fork node */
	protected NetNode forkNode;
	/** Reference to original job */
	protected Job originalJob;

	/**
	 * Creates a new ForkJobInfo, given parent job, number of child jobs and fork node.
	 * @param forkedJob parent job that was forked.
	 * @param forkedNum number of child jobs forked from the parent job.
	 * @param forkNode fork node that forked the parent job.
	 */
	public ForkJobInfo(Job forkedJob, int forkedNum, NetNode forkNode) {
		this.forkedJob = forkedJob;
		this.forkedNum = forkedNum;
		this.forkNode = forkNode;
		if (forkedJob instanceof ForkJob) {
			this.originalJob = ((ForkJob) forkedJob).getOriginalJob();
		} else {
			this.originalJob = forkedJob;
		}
	}

	/**
	 * Gets parent job that was forked.
	 * @return parent job that was forked.
	 */
	public Job getForkedJob() {
		return forkedJob;
	}

	/**
	 * Gets number of child jobs forked from the parent job.
	 * @return number of child jobs forked from the parent job.
	 */
	public int getForkedNum() {
		return forkedNum;
	}

	/**
	 * Gets fork node that forked the parent job.
	 * @return fork node that forked the parent job.
	 */
	public NetNode getForkNode() {
		return forkNode;
	}

	/**
	 * Gets original job that was forked.
	 * @return original job that was forked.
	 */
	public Job getOriginalJob() {
		return originalJob;
	}

}
