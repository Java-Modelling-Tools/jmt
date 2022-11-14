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
 * This class declares all the constants related to message event types.
 * @author Francesco Radaelli
 */
public class NetEvent {

	/*------------------------------BIT MASK----------------------------------*/

	public static final int EVENT_MASK = 0x0000FFFF;

	/*------------------------------EVENTS------------------------------------*/

	/** Event ID: Aborts the simulation.*/
	public static final int EVENT_ABORT = 0x0000;
	/** Event ID: Stops the simulation.*/
	public static final int EVENT_STOP = 0x0001;
	/** Event ID: Starts the simulation.*/
	public static final int EVENT_START = 0x0002;
	/** Event ID: The event contains a job.*/
	public static final int EVENT_JOB = 0x0003;
	/** Event ID: Job ack event.*/
	public static final int EVENT_ACK = 0x0004;
	/** Event ID: Job was preempted in service event.*/
	public static final int EVENT_PREEMPTED_JOB = 0x0005;
	/** Event ID: Keeps node awake.*/
	public static final int EVENT_KEEP_AWAKE = 0x0006;
	/** Event ID: Join ack event.*/
	public static final int EVENT_JOIN = 0x0007;
	/** Event ID: Job out of region event.*/
	public static final int EVENT_JOB_OUT_OF_REGION = 0x0008;
	/** Event ID: Distribution change event.*/
	public static final int EVENT_DISTRIBUTION_CHANGE = 0x0009;
	/** Event ID: Job change event.*/
	public static final int EVENT_JOB_CHANGE = 0x000A;
	/** Event ID: Enabling event.*/
	public static final int EVENT_ENABLING = 0x000B;
	/** Event ID: Timing event.*/
	public static final int EVENT_TIMING = 0x000C;
	/** Event ID: Firing event.*/
	public static final int EVENT_FIRING = 0x000D;
	/** Event ID: Job request event.*/
	public static final int EVENT_JOB_REQUEST = 0x000E;
	/** Event ID: Job release event.*/
	public static final int EVENT_JOB_RELEASE = 0x000F;
	/** Event ID: Job finish event.*/
	public static final int EVENT_JOB_FINISH = 0x0010;
	/** Event ID: Polling next event. */
	public static final int EVENT_POLLING_SERVER_NEXT = 0x0011;
	/** Event ID: Polling next ready. */
	public static final int EVENT_POLLING_SERVER_READY = 0x0012;
	/** Event ID: Job renege event. */
	public static final int EVENT_RENEGE = 0x0013;
	/** Event ID: Job retrial event. */
	public static final int EVENT_RETRIAL = 0x0014;
	/** Event ID: Job retrial event. */
	public static final int EVENT_RETRIAL_JOB = 0x0015;
	/** Event ID: Job setup event. */
	public static final int EVENT_SETUP_JOB = 0x0016;

}
