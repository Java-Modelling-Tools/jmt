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

package jmt.engine.simEngine;

import jmt.engine.QueueNet.SimConstants;
import jmt.gui.common.definitions.SimulationDefinition;

/**
 * Utility class used by the simulation engine
 * @author Marco Bertoli
 *
 */
public abstract class EngineUtils {

	/**
	 * Encodes the type of a measure
	 * @param type number type of the measure
	 * @return text type of the measure
	 */
	static public String encodeMeasureType(int type) {
		switch (type) {
			case SimConstants.QUEUE_LENGTH:
				return SimulationDefinition.MEASURE_QL;
			case SimConstants.QUEUE_TIME:
				return SimulationDefinition.MEASURE_QT;
			case SimConstants.RESPONSE_TIME:
				return SimulationDefinition.MEASURE_RP;
			case SimConstants.RESIDENCE_TIME:
				return SimulationDefinition.MEASURE_RD;
			case SimConstants.UTILIZATION:
				return SimulationDefinition.MEASURE_U;
			case SimConstants.THROUGHPUT:
				return SimulationDefinition.MEASURE_X;
			case SimConstants.DROP_RATE:
				return SimulationDefinition.MEASURE_DR;
			case SimConstants.RENEGING_RATE:
				return SimulationDefinition.MEASURE_RR;
			case SimConstants.BALKING_RATE:
				return SimulationDefinition.MEASURE_BR;
			//case SimConstants.NUM_BUSY_SERVERS:
			//	return SimulationDefinition.MEASURE_BS;
			case SimConstants.RETRIAL_ATTEMPTS_RATE:
				return SimulationDefinition.MEASURE_R;
			case SimConstants.RETRIAL_ORBIT_SIZE:
				return SimulationDefinition.MEASURE_RS;
			case SimConstants.WAITING_TIME:
				return SimulationDefinition.MEASURE_OT;
			case SimConstants.SYSTEM_NUMBER_OF_JOBS:
				return SimulationDefinition.MEASURE_S_CN;
			case SimConstants.SYSTEM_RESPONSE_TIME:
				return SimulationDefinition.MEASURE_S_RP;
			case SimConstants.SYSTEM_THROUGHPUT:
				return SimulationDefinition.MEASURE_S_X;
			case SimConstants.SYSTEM_DROP_RATE:
				return SimulationDefinition.MEASURE_S_DR;
			case SimConstants.SYSTEM_RENEGING_RATE:
				return SimulationDefinition.MEASURE_S_RR;
			case SimConstants.SYSTEM_BALKING_RATE:
				return SimulationDefinition.MEASURE_S_BR;
			case SimConstants.SYSTEM_RETRIAL_ATTEMPTS_RATE:
				return SimulationDefinition.MEASURE_S_R;
			case SimConstants.SYSTEM_POWER:
				return SimulationDefinition.MEASURE_S_P;
			case SimConstants.RESPONSE_TIME_PER_SINK:
				return SimulationDefinition.MEASURE_RP_PER_SINK;
			case SimConstants.THROUGHPUT_PER_SINK:
				return SimulationDefinition.MEASURE_X_PER_SINK;
			case SimConstants.FCR_TOTAL_WEIGHT:
				return SimulationDefinition.MEASURE_FCR_TW;
			case SimConstants.FCR_MEMORY_OCCUPATION:
				return SimulationDefinition.MEASURE_FCR_MO;
			case SimConstants.FORK_JOIN_NUMBER_OF_JOBS:
				return SimulationDefinition.MEASURE_FJ_CN;
			case SimConstants.FORK_JOIN_RESPONSE_TIME:
				return SimulationDefinition.MEASURE_FJ_RP;
			case SimConstants.FIRING_THROUGHPUT:
				return SimulationDefinition.MEASURE_FX;
			default:
				return SimulationDefinition.MEASURE_QL;
		}
	}

	/**
	 * Decodes the type of a measure
	 *
	 * @param type text type of the measure
	 * @return number type of the measure
	 */
	static public int decodeMeasureType(String type) {
		if (type.equals(SimulationDefinition.MEASURE_QL)) {
			return SimConstants.QUEUE_LENGTH;
		} else if (type.equals(SimulationDefinition.MEASURE_QT)) {
			return SimConstants.QUEUE_TIME;
		} else if (type.equals(SimulationDefinition.MEASURE_RP)) {
			return SimConstants.RESPONSE_TIME;
		} else if (type.equals(SimulationDefinition.MEASURE_RD)) {
			return SimConstants.RESIDENCE_TIME;
		} else if (type.equals(SimulationDefinition.MEASURE_U)) {
			return SimConstants.UTILIZATION;
		} else if (type.equals(SimulationDefinition.MEASURE_X)) {
			return SimConstants.THROUGHPUT;
		} else if (type.equals(SimulationDefinition.MEASURE_DR)) {
			return SimConstants.DROP_RATE;
		} else if (type.equals(SimulationDefinition.MEASURE_RR)) {
			return SimConstants.RENEGING_RATE;
		} else if (type.equals(SimulationDefinition.MEASURE_BR)) {
			return SimConstants.BALKING_RATE;
		} else if (type.equals(SimulationDefinition.MEASURE_R)) {
			return SimConstants.RETRIAL_ATTEMPTS_RATE;
		} else if (type.equals(SimulationDefinition.MEASURE_RS)) {
			return SimConstants.RETRIAL_ORBIT_SIZE;
		} else if (type.equals(SimulationDefinition.MEASURE_OT)) {
			return SimConstants.WAITING_TIME;
		//} else if (type.equals(SimulationDefinition.MEASURE_BS)) {
			//return SimConstants.NUM_BUSY_SERVERS;
		} else if (type.equals(SimulationDefinition.MEASURE_S_CN)) {
			return SimConstants.SYSTEM_NUMBER_OF_JOBS;
		} else if (type.equals(SimulationDefinition.MEASURE_S_RP)) {
			return SimConstants.SYSTEM_RESPONSE_TIME;
		} else if (type.equals(SimulationDefinition.MEASURE_S_X)) {
			return SimConstants.SYSTEM_THROUGHPUT;
		} else if (type.equals(SimulationDefinition.MEASURE_S_DR)) {
			return SimConstants.SYSTEM_DROP_RATE;
		} else if (type.equals(SimulationDefinition.MEASURE_S_RR)) {
			return SimConstants.SYSTEM_RENEGING_RATE;
		} else if (type.equals(SimulationDefinition.MEASURE_S_BR)) {
			return SimConstants.SYSTEM_BALKING_RATE;
		} else if (type.equals(SimulationDefinition.MEASURE_S_P)) {
			return SimConstants.SYSTEM_POWER;
		} else if (type.equals(SimulationDefinition.MEASURE_S_R)) {
			return SimConstants.SYSTEM_RETRIAL_ATTEMPTS_RATE;
		} else if (type.equals(SimulationDefinition.MEASURE_RP_PER_SINK)) {
			return SimConstants.RESPONSE_TIME_PER_SINK;
		} else if (type.equals(SimulationDefinition.MEASURE_X_PER_SINK)) {
			return SimConstants.THROUGHPUT_PER_SINK;
		} else if (type.equals(SimulationDefinition.MEASURE_FCR_TW)) {
			return SimConstants.FCR_TOTAL_WEIGHT;
		} else if (type.equals(SimulationDefinition.MEASURE_FCR_MO)) {
			return SimConstants.FCR_MEMORY_OCCUPATION;
		} else if (type.equals(SimulationDefinition.MEASURE_FJ_CN)) {
			return SimConstants.FORK_JOIN_NUMBER_OF_JOBS;
		} else if (type.equals(SimulationDefinition.MEASURE_FJ_RP)) {
			return SimConstants.FORK_JOIN_RESPONSE_TIME;
		} else if (type.equals(SimulationDefinition.MEASURE_FX)) {
			return SimConstants.FIRING_THROUGHPUT;
		} else {
			return SimConstants.QUEUE_LENGTH;
		}
	}

	/**
	 * Returns true if a measure is inverse or false otherwise
	 *
	 * @param measureType the type of measure
	 * @return true if it is inverse, false otherwise.
	 */
	public static boolean isInverseMeasure(int measureType) {
		return measureType == SimConstants.THROUGHPUT
						|| measureType == SimConstants.DROP_RATE
						|| measureType == SimConstants.RENEGING_RATE
						|| measureType == SimConstants.BALKING_RATE
						|| measureType == SimConstants.RETRIAL_ATTEMPTS_RATE
						|| measureType == SimConstants.SYSTEM_THROUGHPUT
						|| measureType == SimConstants.SYSTEM_DROP_RATE
						|| measureType == SimConstants.SYSTEM_RENEGING_RATE
						|| measureType == SimConstants.SYSTEM_BALKING_RATE
						|| measureType == SimConstants.SYSTEM_POWER
						|| measureType == SimConstants.SYSTEM_RETRIAL_ATTEMPTS_RATE
						|| measureType == SimConstants.THROUGHPUT_PER_SINK
						|| measureType == SimConstants.FIRING_THROUGHPUT
						;
	}

}
