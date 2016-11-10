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
 * <p>Title: Fork Job</p>
 * <p>Description: This class implements the fork job.</p>
 *
 * @author Lulai Zhu
 * Date: 06-10-2016
 * Time: 14.00.00
 */
public class ForkJob extends Job {

	/** Reference to fork job info */
	private ForkJobInfo forkJobInfo;

	/**
	 * Creates a new ForkJob, given job class and shared information
	 * @param jobClass class of the fork job
	 * @param forkJobInfo info of the fork job
	 */
	public ForkJob(JobClass jobClass, ForkJobInfo forkJobInfo) {
		super(jobClass);
		systemEnteringTime = forkJobInfo.getForkedJob().systemEnteringTime;
		this.forkJobInfo = forkJobInfo;
	}

	/**
	 * Gets original job that was forked
	 * @return original job that was forked
	 */
	public Job getForkedJob() {
		return forkJobInfo.getForkedJob();
	}

	/**
	 * Gets number of child jobs forked from the original job
	 * @return number of child jobs forked from the original job
	 */
	public int getForkedNum() {
		return forkJobInfo.getForkedNum();
	}

	/**
	 * Gets fork node that forked the original job
	 * @return fork node that forked the original job
	 */
	public NetNode getForkNode() {
		return forkJobInfo.getForkNode();
	}

	/**
	 * Gets info of the fork job
	 * @return info of the fork job
	 */
	public ForkJobInfo getForkJobInfo() {
		return forkJobInfo;
	}

}
