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

package jmt.gui.jsimgraph.definitions;

import java.util.Map;

import jmt.gui.common.definitions.StationDefinition;

/**
 * <p>Title: Jmodel Station Definition Interface</p>
 * <p>Description: This interface provides methods for editing stations for JMODEL models.
 * It actually extends JSIM StationDefinition interface to provide a compatibility layer.</p>
 * 
 * @author Bertoli Marco
 *         Date: 3-giu-2005
 *         Time: 10.08.11
 */
public interface JmodelStationDefinition extends StationDefinition {

	/**
	 * Adds a new station to the model, given the station type.
	 * @param type type of the new station.
	 * @return search key for the new station.
	 */
	public Object addStation(String type);

	/**
	 * Deletes a station from the model, given the search key.
	 */
	public void deleteStation(Object key);

	/**
	 * Returns name of the next station, given the station type.
	 */
	public String previewStationName(String type);

	/**
	 * Sets position of the station, given the search key.
	 */
	public void setStationPosition(Object key, JMTPoint position);

	/**
	 * Returns position of the station, given the search key.
	 */
	public JMTPoint getStationPosition(Object key);

	/**
	 * Returns serialized form of a station.
	 * @param key search key for the station.
	 * @return serialized form of the station.
	 */
	public Object serializeStation(Object key);

	/**
	 * Adds a new station with name and type from its serialized form.
	 * @param station serialized form of the new station.
	 * @return search key for the new station.
	 */
	public Object addStation(Object station);

	/**
	 * Loads parameters of a station from its serialized form.
	 * @param station serialized form of the station.
	 * @param key search key for the station.
	 * @param classMap map of serialized forms for the station.
	 * @param stationKeyMap map of search keys for stations.
	 */
	public void LoadStation(Object station, Object key, Map<Object, Object> classMap, Map<Object, Object> stationKeyMap);

}
