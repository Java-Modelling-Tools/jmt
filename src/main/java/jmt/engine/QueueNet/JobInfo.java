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
 * This class is a generic job information storage class.
 * @see LinkedJobInfoList
 * @author Francesco Radaelli
 */
public class JobInfo {

	private Job job;
	private double enteringTime;

	/**
	 * Creates a new instance of JobInfo object.
	 * @param job Job referenced by this JobInfo.
	 */
	public JobInfo(Job job) {
		this.job = job;
		this.enteringTime = job.getNetSystem().getTime();
	}

	/**
	 * Gets the job referenced by this JobInfo.
	 * @return Job referenced by this JobInfo.
	 */
	public Job getJob() {
		return job;
	}

	/**
	 * Gets the time that the job entered the JobInfoList.
	 * @return Time that the job entered the JobInfoList.
	 */
	public double getEnteringTime() {
		return enteringTime;
	}
	
	public NetSystem getNetSystem(){
		return job.getNetSystem();
	}

}
