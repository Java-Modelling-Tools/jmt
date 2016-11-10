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
 * Description: Added new Performance index for the capturing the 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class. 
 */
public class SimConstants {

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

	/** Measure identifier: response time of the system*/
	public static final int SYSTEM_RESPONSE_TIME = 7;

	/** Measure identifier: throughput of the system */
	public static final int SYSTEM_THROUGHPUT = 8;

	/** Measure identifier: number of jobs in the system */
	public static final int SYSTEM_NUMBER_OF_JOBS = 9;

	/** Measure identifier: drop rate of the system */
	public static final int SYSTEM_DROP_RATE = 10;

	/** Measure identifier: power of the system */
	public static final int SYSTEM_POWER = 11;

	/** Measure identifier: response time of the sink */
	public static final int RESPONSE_TIME_PER_SINK = 12;

	/** Measure identifier: throughput of the sink */
	public static final int THROUGHPUT_PER_SINK = 13;

	//-------------------- end SIMULATION MEASURE IDENTIFIERS -------------------------//

	//-------------------- JOB LIST MEASURE IDENTIFIERS ----------------------------//

	/** Measure identifier: number of jobs in the list */
	public static final int LIST_NUMBER_OF_JOBS = 21;

	/** Measure identifier: response time of the list */
	public static final int LIST_RESPONSE_TIME = 22;

	/** Measure identifier: residence time of the list */
	public static final int LIST_RESIDENCE_TIME = 23;

	/** Measure identifier: throughput of the list */
	public static final int LIST_THROUGHPUT = 24;

	/** Measure identifier: drop rate of the list */
	public static final int LIST_DROP_RATE = 25;

	//-------------------- end JOB LIST MEASURE IDENTIFIERS -------------------------//

	/** To be used for a blocking region measure */
	public static final String NODE_TYPE_REGION = "region";

	/** To be used for a station measure */
	public static final String NODE_TYPE_STATION = "station";

	/** Measure identifier: total weight of a blocking region. */
	public static final int FCR_TOTAL_WEIGHT = 27;

	/** Measure identifier: memory occupation of a blocking region. */
	public static final int FCR_MEMORY_OCCUPATION = 28;

	/** Measure identifier: number of jobs in a fork/join section. */
	public static final int FORK_JOIN_NUMBER_OF_JOBS = 29;

	/** Measure identifier: response time of a fork/join section. */
	public static final int FORK_JOIN_RESPONSE_TIME = 30;

	/** Measure identifier: firing throughput of a transition. */
	public static final int FIRING_THROUGHPUT = 31;

	public static boolean isFJMeasure(int measure) {
		return measure == FORK_JOIN_NUMBER_OF_JOBS || measure == FORK_JOIN_RESPONSE_TIME;
	}

}
