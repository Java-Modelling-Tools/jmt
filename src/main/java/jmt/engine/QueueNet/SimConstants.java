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
 * Constants used by QueueNet package.
 * 
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public interface SimConstants {

	//-------------------- SIMULATION MEASURE IDENTIFIERS ----------------------------//

	/** Measure identifier: queue length of the node */
	public static final int QUEUE_LENGTH = 0;

	/** Measure identifier: queue time of the node */
	public static final int QUEUE_TIME = 1;

	/** Measure identifier: response time of the node */
	public static final int RESPONSE_TIME = 2;

	/** Measure identifier: residence time of the node */
	public static final int RESIDENCE_TIME = 3;

	/** Measure identifier: utilization of the node */
	public static final int UTILIZATION = 4;

	/** Measure identifier: throughput of the node */
	public static final int THROUGHPUT = 5;

	/** Measure identifier: drop rate of the node */
	public static final int DROP_RATE = 6;

	/** Measure identifier: number of jobs in the system */
	public static final int SYSTEM_NUMBER_OF_JOBS = 7;

	/** Measure identifier: response time of the system*/
	public static final int SYSTEM_RESPONSE_TIME = 8;

	/** Measure identifier: throughput of the system */
	public static final int SYSTEM_THROUGHPUT = 9;

	/** Measure identifier: drop rate of the system */
	public static final int SYSTEM_DROP_RATE = 10;

	/** Measure identifier: power of the system */
	public static final int SYSTEM_POWER = 11;

	/** Measure identifier: response time of the sink */
	public static final int RESPONSE_TIME_PER_SINK = 12;

	/** Measure identifier: throughput of the sink */
	public static final int THROUGHPUT_PER_SINK = 13;

	/** Measure identifier: total weight of the blocking region. */
	public static final int FCR_TOTAL_WEIGHT = 14;

	/** Measure identifier: memory occupation of the blocking region. */
	public static final int FCR_MEMORY_OCCUPATION = 15;

	/** Measure identifier: number of jobs in the fork/join section. */
	public static final int FORK_JOIN_NUMBER_OF_JOBS = 16;

	/** Measure identifier: response time of the fork/join section. */
	public static final int FORK_JOIN_RESPONSE_TIME = 17;

	/** Measure identifier: firing throughput of the transition. */
	public static final int FIRING_THROUGHPUT = 18;


	/** Measure identifier: reneging rate of the system */
	public static final int SYSTEM_RENEGING_RATE = 19;

	/** Measure identifier: reneging rate of the node */
	public static final int RENEGING_RATE = 20;

	/** Measure identifier: reneging rate of the system */
	public static final int SYSTEM_BALKING_RATE = 21;

	/** Measure identifier: reneging rate of the node */
	public static final int BALKING_RATE = 22;

	/** Measure identifier: retrial attempts rate of the node */
	public static final int RETRIAL_ATTEMPTS_RATE = 30;

	/** Measure identifier: retrial attempts rate of the system */
	public static final int SYSTEM_RETRIAL_ATTEMPTS_RATE = 31;

	/** Measure identifier: number of jobs in the retrial orbit of the node */
	public static final int RETRIAL_ORBIT_SIZE = 33;

	/** Measure identifier: (total) waiting time of a job of a node (system entering time - time when job is pass to serving section) */
	public static final int WAITING_TIME = 36;

	//public static final int NUM_BUSY_SERVERS = 39;

	//-------------------- end SIMULATION MEASURE IDENTIFIERS -------------------------//

	//-------------------- JOB LIST MEASURE IDENTIFIERS ----------------------------//

	/** Measure identifier: number of jobs in the list */
	public static final int LIST_NUMBER_OF_JOBS = 23;

	/** Measure identifier: response time of the list */
	public static final int LIST_RESPONSE_TIME = 24;

	/** Measure identifier: residence time of the list */
	public static final int LIST_RESIDENCE_TIME = 25;

	/** Measure identifier: throughput of the list */
	public static final int LIST_THROUGHPUT = 26;

	/** Measure identifier: drop rate of the list */
	public static final int LIST_DROP_RATE = 27;

	/** Measure identifier: reneging rate of the list */
	public static final int LIST_RENEGING_RATE = 28;

	/** Measure identifier: reneging rate of the list */
	public static final int LIST_BALKING_RATE = 29;

	/** Measure identifier: drop rate of the list */
	public static final int LIST_RETRIAL_RATE = 32;

	/** Measure identifier: number of jobs in the retrial orbit in the list */
	public static final int LIST_RETRIAL_ORBIT_SIZE = 35;

	/** Measure identifier: (total) waiting time of a job of the list (system entering time - time when job is pass to serving section) */
	public static final int LIST_WAITING_TIME = 38;

	public static final int LIST_BUSY_SERVERS = 40;

	//-------------------- end JOB LIST MEASURE IDENTIFIERS -------------------------//

	/** To be used for a blocking region measure */
	public static final String NODE_TYPE_REGION = "region";

	/** To be used for a station measure */
	public static final String NODE_TYPE_STATION = "station";

}
