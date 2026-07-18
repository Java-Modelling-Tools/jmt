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
 * This class is a generic job information storage class used by processor-
 * sharing.
 * @see PSJobInfoList
 * @author Lulai Zhu
 */
public class PSJobInfo extends JobInfo {

	private double serviceTime;
	private double residualServiceTime;
	private double aheadServiceTimeInLastQuantum;

	/**
	 * Creates a new instance of JobInfo object.
	 * @param job Job referenced by this JobInfo.
	 */
	public PSJobInfo(Job job) {
		super(job);
		aheadServiceTimeInLastQuantum = 0.0;
	}

	/**
	 * Gets the service time that the job must receive.
	 * @return Service time that the job must receive.
	 */
	public double getServiceTime() {
		return serviceTime;
	}

	/**
	 * Sets the service time that the job must receive.
	 * @param serviceTime Service time that the job must receive.
	 */
	public void setServiceTime(double serviceTime) {
		this.serviceTime = serviceTime;
	}

	/**
	 * Gets the residual service time that the job must receive.
	 * @return Residual service time that the job must receive.
	 */
	public double getResidualServiceTime() {
		return residualServiceTime;
	}

	/**
	 * Sets the residual service time that the job must receive.
	 * @param residualServiceTime Residual service time that the job must receive.
	 */
	public void setResidualServiceTime(double residualServiceTime) {
		this.residualServiceTime = residualServiceTime;
	}

	/**
	 * Performs the service time that the job has received.
	 * @param serviceTime Service time that the job has received.
	 */
	public void performServiceTime(double serviceTime) {
		residualServiceTime -= serviceTime;
	}

	/**
	 * Gets the Ahead service time in last quantum.
	 * @return Ahead service time in last quantum.
	 */
	public double getAheadServiceTimeInLastQuantum() {
		return aheadServiceTimeInLastQuantum;
	}

	/**
	 * Sets the ahead service time in last quantum.
	 * @param aheadServiceTimeInLastQuantum ahead service time in last quantum.
	 */
	public void setAheadServiceTimeInLastQuantum(double aheadServiceTimeInLastQuantum) {
		this.aheadServiceTimeInLastQuantum = aheadServiceTimeInLastQuantum;
	}
}
