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

package jmt.gui.common.xml;

/**
 * <p>Title: PNML Constants</p>
 * <p>Description: Constants used by <code>PNMLReader</code> and <code>PNMLWriter</code>
 * to read and write PNML files.</p>
 * 
 * @author Lulai Zhu
 * Date: 25-01-2017
 * Time: 12.00.00
 */
public interface PNMLConstants {

	public static final String PNML_JSIM = "Java Modelling Tools - JSIM";
	public static final String PNML_JSIM_E_NODE_GRAPHICS = "graphics";
	public static final String PNML_JSIM_E_NODE_GRAPHICS_ROTATE = "rotate";
	public static final String PNML_JSIM_E_NET_TOKENS = "tokens";
	public static final String PNML_JSIM_E_NET_TOKENS_CLASS_NAME = "className";
	public static final String PNML_JSIM_E_NET_TOKENS_CLASS_TYPE = "classType";
	public static final String PNML_JSIM_V_NET_TOKENS_CLASS_TYPE_OPEN = "Open";
	public static final String PNML_JSIM_V_NET_TOKENS_CLASS_TYPE_CLOSED = "Closed";
	public static final String PNML_JSIM_E_NET_TOKENS_REFERENCE_NODE = "referenceNode";
	public static final String PNML_JSIM_E_NET_TOKENS_GRAPHICS = "graphics";
	public static final String PNML_JSIM_E_NET_TOKENS_GRAPHICS_COLOR = "color";
	public static final String PNML_JSIM_E_PLACE_CAPACITY = "capacity";
	public static final String PNML_JSIM_E_TRANSITION_NUMBER_OF_SERVERS = "numberOfServers";
	public static final String PNML_JSIM_E_TRANSITION_TIMING_STRATEGY = "timingStrategy";
	public static final String PNML_JSIM_V_TRANSITION_TIMING_STRATEGY_TIMED = "Timed";
	public static final String PNML_JSIM_V_TRANSITION_TIMING_STRATEGY_IMMEDIATE = "Immediate";
	public static final String PNML_JSIM_E_TRANSITION_FIRING_TIME_DISTRIBUTION = "firingTimeDistribution";
	public static final String PNML_JSIM_E_TRANSITION_FIRING_PRIORITY = "firingPriority";
	public static final String PNML_JSIM_E_TRANSITION_FIRING_WEIGHT = "firingWeight";
	public static final String PNML_JSIM_E_ARC_TYPE = "type";
	public static final String PNML_JSIM_V_ARC_TYPE_NORMAL = "Normal";
	public static final String PNML_JSIM_V_ARC_TYPE_INHIBITOR = "Inhibitor";
	public static final String PNML_JSIM_E_DISTRIBUTION = "distribution";
	public static final String PNML_JSIM_E_DISTRIBUTION_TYPE = "type";
	public static final String PNML_JSIM_E_DISTRIBUTION_PARAMETER = "parameter";
	public static final String PNML_JSIM_E_DISTRIBUTION_PARAMETER_NAME = "name";
	public static final String PNML_JSIM_E_DISTRIBUTION_PARAMETER_VALUE = "value";
	public static final String PNML_JSIM_E_MATRIX = "matrix";
	public static final String PNML_JSIM_E_MATRIX_VECTOR = "vector";
	public static final String PNML_JSIM_E_MATRIX_VECTOR_ENTRY = "entry";

}
