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

package jmt.gui.jsimgraph.controller;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import jmt.gui.jsimgraph.JGraphMod.BlockingRegion;
import jmt.gui.jsimgraph.JGraphMod.CellComponent;
import jmt.gui.jsimgraph.JGraphMod.JmtCell;
import jmt.gui.jsimgraph.JGraphMod.JmtEdge;
import jmt.gui.jsimgraph.definitions.JMTPoint;
import jmt.gui.jsimgraph.definitions.JmodelBlockingRegionDefinition;
import jmt.gui.jsimgraph.definitions.JmodelClassDefinition;
import jmt.gui.jsimgraph.definitions.JmodelStationDefinition;

/**
 * <p>Title: Jmt Clipboard</p>
 * <p>Description: This class provides clipboard facility to copy and paste selected sections
 * of current model. It will operate both on data structure (JmodelStationDefinition) end and
 * on graphic representation (Jgraph through mediator calls).</p>
 * 
 * @author Bertoli Marco
 *         Date: 21-giu-2005
 *         Time: 11.49.57
 */
public class JmtClipboard {

	protected static final int move = 20;

	protected Mediator mediator;
	protected HashMap<Object, Object> classes;
	protected HashMap<Object, Object> stations;
	protected HashMap<Object, JMTPoint> stationPositions;
	protected Vector<Connection> connections;
	protected HashMap<Object, Object> regions;
	protected HashMap<Object, HashSet<Object>> regionStations;
	protected Point2D zero;

	/**
	 * Initializes a new JmtClipboard with the given mediator.
	 * @param mediator mediator to be referenced
	 */
	public JmtClipboard(Mediator mediator) {
		this.mediator = mediator;
	}

	/**
	 * Flushes the clipboard.
	 */
	public void flush() {
		classes = new HashMap<Object, Object>();
		stations = new HashMap<Object, Object>();
		stationPositions = new HashMap<Object, JMTPoint>();
		connections = new Vector<Connection>();
		regions = new HashMap<Object, Object>();
		regionStations = new HashMap<Object, HashSet<Object>>();
		zero = null;
	}

	/**
	 * Copies the given elements into clipboard and deletes them from graph.
	 */
	public void cut(Object[] cells) {
		copy(cells);
		mediator.delete(cells);
	}

	/**
	 * Copies the given elements into clipboard.
	 */
	public void copy(Object[] cells) {
		flush();
		JmodelClassDefinition cd = mediator.getClassDefinition();
		JmodelStationDefinition sd = mediator.getStationDefinition();
		JmodelBlockingRegionDefinition brd = mediator.getBlockingRegionDefinition();
		// Saves the classes into data structure
		for (Object classKey : cd.getClassKeys()) {
			classes.put(classKey, cd.serializeClass(classKey));
		}
		// Saves the stations, connections and blocking regions into data structure
		for (Object cell : cells) {
			if (cell instanceof JmtCell) {
				Object stationKey = ((CellComponent) ((JmtCell) cell).getUserObject()).getKey();
				stations.put(stationKey, sd.serializeStation(stationKey));
				JMTPoint position = new JMTPoint(mediator.getCellCoordinates((JmtCell) cell),
						!((JmtCell) cell).isLeftInputCell());
				stationPositions.put(stationKey, position);
				if (zero == null) {
					zero = new Point2D.Double(position.getX(), position.getY());
				}
				if (zero.getX() > position.getX()) {
					zero.setLocation(position.getX(), zero.getY());
				}
				if (zero.getY() > position.getY()) {
					zero.setLocation(zero.getX(), position.getY());
				}
			} else if (cell instanceof JmtEdge) {
				Object sourceKey = ((JmtEdge) cell).getSourceKey();
				Object targetKey = ((JmtEdge) cell).getTargetKey();
				connections.add(new Connection(sourceKey, targetKey));
			} else if (cell instanceof BlockingRegion) {
				Object regionKey = ((BlockingRegion) cell).getKey();
				regions.put(regionKey, brd.serializeBlockingRegion(regionKey));
				regionStations.put(regionKey, new HashSet<Object>(brd.getBlockingRegionStations(regionKey)));
			}
		}
	}

	/**
	 * Pastes previously copied elements into graph at the original point.
	 */
	public void paste() {
		paste(zero);
	}

	/**
	 * Pastes previously copied elements into graph at the given point.
	 * @param where point where pasted elements will be put.
	 * @return array of pasted elements.
	 */
	public Object[] paste(Point2D where) {
		if (stations == null || stations.size() == 0) {
			return null;
		}

		JmodelStationDefinition sd = mediator.getStationDefinition();
		JmodelBlockingRegionDefinition brd = mediator.getBlockingRegionDefinition();
		HashMap<Object, Object> stationKeyMap = new HashMap<Object, Object>();
		HashMap<Object, Object> reverseStationKeyMap = new HashMap<Object, Object>();
		HashMap<Object, JmtCell> stationCellMap = new HashMap<Object, JmtCell>();
		ArrayList<Object> cellList = new ArrayList<Object>();

		// Adds every station with name and type from its serialized form
		for (Object oldStationKey : stations.keySet()) {
			Object newStationKey = sd.addStation(stations.get(oldStationKey));
			JmtCell newStationCell = mediator.getCellFactory().createStationCell(newStationKey);
			JMTPoint oldPosition = stationPositions.get(oldStationKey);
			JMTPoint newPosition = new JMTPoint(where.getX() + oldPosition.getX() - zero.getX(),
					where.getY() + oldPosition.getY() - zero.getY(), oldPosition.isRotate());
			while (mediator.overlapCells(newPosition, newStationCell)) {
				newPosition.setLocation(newPosition.getX() + move, newPosition.getY() + move);
			}
			mediator.InsertCell(newPosition, newStationCell);
			if (newPosition.isRotate()) {
				mediator.rotateComponent(new Object[] { newStationCell });
			}
			stationKeyMap.put(oldStationKey, newStationKey);
			reverseStationKeyMap.put(newStationKey, oldStationKey);
			stationCellMap.put(newStationKey, newStationCell);
			cellList.add(newStationCell);
		}

		// Creates every connection
		for (Connection connection : connections) {
			Object sourceKey = stationKeyMap.get(connection.sourceKey);
			Object targetKey = stationKeyMap.get(connection.targetKey);
			JmtEdge newEdgeCell = mediator.connect(stationCellMap.get(sourceKey), stationCellMap.get(targetKey));
			if (newEdgeCell != null) {
				cellList.add(newEdgeCell);
			}
		}

		// Loads parameters of every station from its serialized form
		for (Object oldStationKey : stations.keySet()) {
			sd.LoadStation(stations.get(oldStationKey), stationKeyMap.get(oldStationKey), classes, reverseStationKeyMap);
		}

		// Inserts every blocking region according to its serialized form
		for (Object oldRegionKey : regions.keySet()) {
			Object newRegionKey = brd.deserializeBlockingRegion(regions.get(oldRegionKey), classes);
			HashMap<Object, JmtCell> regionStationCellMap = new HashMap<Object, JmtCell>();
			for (Object oldStationKey : regionStations.get(oldRegionKey)) {
				Object newStationKey = stationKeyMap.get(oldStationKey);
				regionStationCellMap.put(newStationKey, stationCellMap.get(newStationKey));
			}
			for (Object newStationKey : regionStationCellMap.keySet()) {
				brd.addRegionStation(newRegionKey, newStationKey);
			}
			BlockingRegion newRegionCell = new BlockingRegion(mediator, newRegionKey);
			newRegionCell.addStations(regionStationCellMap.values().toArray());
			cellList.add(newRegionCell);
		}

		// Selects every inserted element
		Object[] cellArray = cellList.toArray();
		mediator.getGraph().clearSelection();
		mediator.getGraph().addSelectionCells(cellArray);
		return cellArray;
	}

	/**
	 * Inner class used to store pairs of sourceKey/targetKey objects.
	 */
	protected class Connection {
		protected Object sourceKey;
		protected Object targetKey;

		public Connection(Object sourceKey, Object targetKey) {
			this.sourceKey = sourceKey;
			this.targetKey = targetKey;
		}
	}

	/**
	 * Copies the given station into clipboard.
	 */
	public void copyCell(JmtCell cell) {
		flush();
		copy(new Object[] { cell });
	}

	/**
	 * Pastes a previously copied station into graph at the given point.
	 * @param where point where the pasted station will be put.
	 * @return pasted station.
	 */
	public JmtCell pasteCell(Point2D where) {
		Object[] cells = paste(where);
		for (Object cell : cells) {
			if (cell instanceof JmtCell) {
				return (JmtCell) cell;
			}
		}
		return null;
	}

}
