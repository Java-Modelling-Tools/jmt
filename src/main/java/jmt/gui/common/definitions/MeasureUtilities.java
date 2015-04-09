/**    
  * Copyright (C) 2014, Dipartimento di Elettronica, Informazione e Bioingegneria (DEIB) - Politecnico di Milano

  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.

  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */
package jmt.gui.common.definitions;

import jmt.engine.QueueNet.SimConstants;

/**
 * <p>Title: Utility class to deal with measures</p>
 * <p>Description: An utility class with static helper methods to deal with measures</p>
 * 
 * @author Bertoli Marco
 *         Date: 2014-07-20
 *         
 */
public abstract class MeasureUtilities {

  	/**
  	 * Tells if the given measure type is supported by statistical window
  	 * @param type the measure type
  	 * @return true if supported, false otherwise.
  	 */
  	public static boolean isSupportedByStatisticalWindow(String type){
		return type.equals(SimulationDefinition.MEASURE_RP ) || type.equals(SimulationDefinition.MEASURE_RD) || 
				type.equals(SimulationDefinition.MEASURE_QL) || type.equals(SimulationDefinition.MEASURE_QT) ||
				type.equals(SimulationDefinition.MEASURE_S_RP) || type.equals(SimulationDefinition.MEASURE_S_CN)
				|| type.equals(SimulationDefinition.MEASURE_R_PER_SINK);
  	}
  	
  	/**
  	 * Tells if the given measure type is supported by statistical window
  	 * @param type the measure type
  	 * @return true if supported, false otherwise.
  	 */
  	public static boolean isSupportedByStatisticalWindow(int type){
		return type == SimConstants.RESIDENCE_TIME || type == SimConstants.RESPONSE_TIME || 
				type == SimConstants.QUEUE_LENGTH || type == SimConstants.QUEUE_TIME ||
				type == SimConstants.SYSTEM_RESPONSE_TIME || type == SimConstants.SYSTEM_JOB_NUMBER
				|| type == SimConstants.RESPONSE_TIME_PER_SINK;
  	}
}
