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

	/** Measure identifier: arrival rate of the node */
	public static final int ARRIVAL_RATE = 4;

	/** Measure identifier: throughput of the node */
	public static final int THROUGHPUT = 5;

	/** Measure identifier: utilization of the node */
	public static final int UTILIZATION = 6;

	/** Measure identifier: effective utilization of the node */
	public static final int EFFECTIVE_UTILIZATION = 7;

	/** Measure identifier: drop rate of the node */
	public static final int DROP_RATE = 8;

	/** Measure identifier: reneging rate of the node */
	public static final int RENEGING_RATE = 9;

	/** Measure identifier: reneging rate of the node */
	public static final int BALKING_RATE = 10;

	/** Measure identifier: retrial attempts rate of the node */
	public static final int RETRIAL_ATTEMPTS_RATE = 11;

	/** Measure identifier: number of jobs in the retrial orbit of the node */
	public static final int RETRIAL_ORBIT_SIZE = 12;

	/** Measure identifier: time of a job in the retrial orbit of the node */
	public static final int RETRIAL_ORBIT_TIME = 13;

	/** Measure identifier: number of jobs in the system */
	public static final int SYSTEM_NUMBER_OF_JOBS = 14;

	/** Measure identifier: response time of the system*/
	public static final int SYSTEM_RESPONSE_TIME = 15;

	/** Measure identifier: throughput of the system */
	public static final int SYSTEM_THROUGHPUT = 16;

	/** Measure identifier: drop rate of the system */
	public static final int SYSTEM_DROP_RATE = 17;

	/** Measure identifier: reneging rate of the system */
	public static final int SYSTEM_RENEGING_RATE = 18;

	/** Measure identifier: reneging rate of the system */
	public static final int SYSTEM_BALKING_RATE = 19;

	/** Measure identifier: retrial attempts rate of the system */
	public static final int SYSTEM_RETRIAL_ATTEMPTS_RATE = 20;

	/** Measure identifier: power of the system */
	public static final int SYSTEM_POWER = 21;

	/** Measure identifier: response time of the sink */
	public static final int RESPONSE_TIME_PER_SINK = 22;

	/** Measure identifier: throughput of the sink */
	public static final int THROUGHPUT_PER_SINK = 23;

	/** Measure identifier: total weight of the blocking region. */
	public static final int FCR_TOTAL_WEIGHT = 24;

	/** Measure identifier: memory occupation of the blocking region. */
	public static final int FCR_MEMORY_OCCUPATION = 25;

	/** Measure identifier: number of jobs in the fork/join section. */
	public static final int FORK_JOIN_NUMBER_OF_JOBS = 26;

	/** Measure identifier: response time of the fork/join section. */
	public static final int FORK_JOIN_RESPONSE_TIME = 27;

	/** Measure identifier: firing throughput of the transition. */
	public static final int FIRING_THROUGHPUT = 28;

	/** Measure identifier: tardiness of the system. */
	public static final int SYSTEM_TARDINESS = 29;

	/** Measure identifier: tardiness of the node. */
	public static final int TARDINESS = 30;

	/** Measure identifier: earliness of the system. */
	public static final int SYSTEM_EARLINESS = 31;

	/** Measure identifier: earliness of the node. */
	public static final int EARLINESS = 32;

	/** Measure identifier: lateness of the system. */
	public static final int SYSTEM_LATENESS = 33;

	/** Measure identifier: lateness of the node. */
	public static final int LATENESS = 34;

        public static final int CACHE_HIT_RATE = 35;
	
	/** Measure identifier: number of active servers of the station*/
	public static final int NUMBER_OF_ACTIVE_SERVERS = 36;

	//-------------------- end SIMULATION MEASURE IDENTIFIERS -------------------------//

	//-------------------- JOB LIST MEASURE IDENTIFIERS ----------------------------//

	/** Measure identifier: number of jobs in the list */
	public static final int LIST_NUMBER_OF_JOBS = 100;

	/** Measure identifier: number of jobs in service in the list */
	public static final int LIST_NUMBER_OF_JOBS_IN_SERVICE = 101;

	/** Measure identifier: response time of the list */
	public static final int LIST_RESPONSE_TIME = 102;

	/** Measure identifier: residence time of the list */
	public static final int LIST_RESIDENCE_TIME = 103;

	/** Measure identifier: arrival rate of the list */
	public static final int LIST_ARRIVAL_RATE = 104;

	/** Measure identifier: throughput of the list */
	public static final int LIST_THROUGHPUT = 105;

	/** Measure identifier: drop rate of the list */
	public static final int LIST_DROP_RATE = 106;

	/** Measure identifier: reneging rate of the list */
	public static final int LIST_BALKING_RATE = 108;

	/** Measure identifier: reneging rate of the list */
	public static final int LIST_RENEGING_RATE = 107;

	/** Measure identifier: drop rate of the list */
	public static final int LIST_RETRIAL_RATE = 109;

	/** Measure identifier: number of jobs in the retrial orbit of the list */
	public static final int LIST_RETRIAL_ORBIT_SIZE = 110;

	/** Measure identifier: time of a job in the retrial orbit of the list */
	public static final int LIST_RETRIAL_ORBIT_TIME = 111;

        public static final int LIST_CACHE_HIT_RATE = 112;


	//-------------------- end JOB LIST MEASURE IDENTIFIERS -------------------------//

	/** To be used for a blocking region measure */
	public static final String NODE_TYPE_REGION = "region";

	/** To be used for a station measure */
	public static final String NODE_TYPE_STATION = "station";

}
