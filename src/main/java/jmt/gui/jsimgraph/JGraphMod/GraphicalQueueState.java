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

package jmt.gui.jsimgraph.JGraphMod;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Vector;

import jmt.gui.common.Defaults;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.controller.ModelSnapshot;

/**
 * <p>Title: GraphicalQueueState</p>
 * <p>Description: this class is used to graphically update the state of queues
 * and utilizations</p>
 *
 * @author Francesco D'Aquino
 *         Date: 22-feb-2006
 *         Time: 09.09.21

 * Modified by Bertoli Marco to support JGraph 5.8 - 21/mar/2006
 * Modified by Bertoli Marco to greatly speed up everything - 18/may/2006

 */
public class GraphicalQueueState {

	private Mediator mediator;
	private HashMap<Object, JmtCell> tm; // Used to map station keys to their cells

	public GraphicalQueueState(Mediator m) {
		mediator = m;
		tm = new HashMap<Object, JmtCell>();
		// Initialize mapping between station keys and their cell components
		// All cells (included blocking regions children)
		Object[] cells = m.getGraph().getDescendants(m.getGraph().getRoots());
		for (Object tempCell : cells) {
			if (tempCell instanceof JmtCell) {
				JmtCell cell = (JmtCell) tempCell;
				Object key = ((CellComponent) cell.getUserObject()).getKey();
				tm.put(key, cell);
			}
		}
	}

	/**
	 * Draws the queues and the utilizations over the graph
	 * @param queueState a <code> SimulationSnapshot </code> containing information about the queues
	 * @param utilizationState a <code> SimulationSnapshot </code> containing information about the utilizations
	 * @param detectChanges 
	 * @throws Exception 
	 */
	public void draw(ModelSnapshot queueState, ModelSnapshot utilizationState) throws Exception {
		int MAX_NUMBER_OF_CLASSES_QUEUE = Defaults.getAsInteger("representableClasses").intValue();

		double nClasses = mediator.getClassDefinition().getClassKeys().size();
		double maxNJobs = queueState.getMaxValue();

		//used to avoid extreme queue length variation when the number of jobs inside the system is very small
		if (maxNJobs < 10) {
			maxNJobs = 10;
		}

		double QUEUE_HEIGHT = (JMTImageLoader.loadImage("queue")).getIconHeight();
		double QUEUE_WIDTH = (JMTImageLoader.loadImage("queue")).getIconWidth() - QUEUE_HEIGHT;

		double SOURCE_SIZE = (JMTImageLoader.loadImage("source")).getIconWidth();
		double SOURCE_MARGIN = 9;
		double source_usable_space = SOURCE_SIZE - SOURCE_MARGIN * 2;

		double H_MARGIN = 2;
		double V_MARGIN = 2;

		double width = QUEUE_WIDTH - 2 * H_MARGIN;
		double height = QUEUE_HEIGHT - 2 * V_MARGIN;

		double H_FREE_SPACE = 2;
		double V_FREE_SPACE = 2;

		Graphics2D g = mediator.getGraphGraphics();

		Vector<Object> sources = mediator.getStationDefinition().getStationKeysSource();
		Vector<Object> openClasses = mediator.getClassDefinition().getOpenClassKeys();
		for (int i = 0; i < sources.size(); i++) {
			Vector<Object> refClasses = new Vector<Object>(0, 1);
			Object thisSource = sources.get(i);
			JmtCell cell = tm.get(thisSource);
			// Skips special or additional sources
			if (cell == null) {
				continue;
			}
			int classCount = 0;
			for (int j = 0; j < openClasses.size(); j++) {
				Object thisClass = openClasses.get(j);
				if (mediator.getClassDefinition().getClassRefStation(thisClass) == thisSource) {
					refClasses.add(thisClass);
					classCount++;
				}
			}
			Point2D p = mediator.getCellCoordinates(cell);

			double xStart = p.getX() + (mediator.getCellDimension(cell).getWidth() - SOURCE_SIZE + 2 * SOURCE_MARGIN) / 2;
			double yStart = p.getY() + SOURCE_MARGIN;
			//if the number of class is less than the maximum representable
			if (classCount < MAX_NUMBER_OF_CLASSES_QUEUE) {
				double classSpace = source_usable_space / classCount;
				for (int j = 0; j < classCount; j++) {
					Object thisClass = refClasses.get(j);
					g.setColor(mediator.getClassDefinition().getClassColor(thisClass));
					g.fill(new Rectangle2D.Double(xStart, yStart, classSpace, source_usable_space));
					xStart += classSpace;
				}
			}
			//else..
			else {
				Vector<Object> classesReplication = new Vector<Object>(refClasses);
				Vector<Object> classesToBeDrawn = new Vector<Object>(0, 1);
				//... find the first MAX_NUMBER_OF_CLASSES_QUEUE with the smallest mean value
				for (int j = 0; j < MAX_NUMBER_OF_CLASSES_QUEUE; j++) {
					double min = Double.MAX_VALUE;
					double mean;
					int index = 0;

					for (int k = 0; k < classesReplication.size(); k++) {
						Object thisClass = classesReplication.get(k);
						Object temp = mediator.getClassDefinition().getClassDistribution(thisClass);
						if (temp instanceof Distribution) {
							if (((Distribution) temp).hasMean()) {
								mean = ((Distribution) temp).getMean();
								if (mean < min) {
									min = mean;
									index = k;
								}
							}
						}
					}
					classesToBeDrawn.add(classesReplication.get(index));
					classesReplication.remove(classesReplication.get(index));
				}
				//if less than MAX_NUMBER_OF_CLASSES_QUEUE classes where found
				//add the ( MAX_NUMBER_OF_CLASSES_QUEUE - classesToBeDrawn.size() )
				//with the highest priority
				if (classesToBeDrawn.size() < MAX_NUMBER_OF_CLASSES_QUEUE) {
					int left = MAX_NUMBER_OF_CLASSES_QUEUE - classesToBeDrawn.size();
					for (int j = 0; j < left; j++) {
						int max = Integer.MIN_VALUE;
						int priority;
						int index = 0;

						for (int k = 0; k < classesReplication.size(); k++) {
							Object thisClass = classesReplication.get(k);
							priority = mediator.getClassDefinition().getClassPriority(thisClass).intValue();
							if (priority > max) {
								max = priority;
								index = k;
							}
						}
						classesToBeDrawn.add(classesReplication.get(index));
						classesReplication.remove(classesReplication.get(index));
					}
				}
				//draw the classes inside the source
				double classSpace = source_usable_space / MAX_NUMBER_OF_CLASSES_QUEUE;
				for (int j = 0; j < MAX_NUMBER_OF_CLASSES_QUEUE; j++) {
					Object thisClass = classesToBeDrawn.get(j);
					g.setColor(mediator.getClassDefinition().getClassColor(thisClass));
					g.fill(new Rectangle2D.Double(xStart, yStart, classSpace, source_usable_space));
					xStart += classSpace;
				}
			}
		}

		//Now draw station's queue
		Vector<Object> servers = mediator.getStationDefinition().getStationKeysServer();
		Vector<Object> classes = mediator.getClassDefinition().getClassKeys();

		//for each server ...
		for (int i = 0; i < servers.size(); i++) {
			JmtCell cell = tm.get(servers.get(i));
			// Skips special or additional servers
			if (cell == null) {
				continue;
			}
			Point2D p = mediator.getCellCoordinates(cell);
			double offset;
			double xStart;
			double yStart;
			g.setColor(Color.LIGHT_GRAY);
			offset = (mediator.getCellDimension(cell).getWidth() - QUEUE_WIDTH - QUEUE_HEIGHT) / 2;
			if (cell.isLeftInputCell()) {
				g.fill(new Rectangle2D.Double(p.getX()  + offset, p.getY() + V_FREE_SPACE, width , height));
			}
			else {
				g.fill(new Rectangle2D.Double(p.getX() + QUEUE_HEIGHT + offset, p.getY() + V_FREE_SPACE, width, height));
			}
			yStart = p.getY() + V_FREE_SPACE;

			//First draw the queues ....
			//if the number of classes is less then MAX_NUMBER_OF_CLASSES_QUEUE
			if (nClasses <= MAX_NUMBER_OF_CLASSES_QUEUE) {
				for (int j = 0; j < nClasses; j++) {
					double nJobs = queueState.getValue(servers.get(i), classes.get(j));
					double thisWidth = (nJobs / maxNJobs) * width;
					double thisHeight = height / nClasses;

					Color color = mediator.getClassDefinition().getClassColor(classes.get(j));
					g.setColor(color);
					if (cell.isLeftInputCell()) {
						xStart = p.getX() + offset + (width - thisWidth) + H_FREE_SPACE;
						g.fill(new Rectangle2D.Double(xStart, yStart, thisWidth, thisHeight));
					}
					else {
						xStart = p.getX() + offset + QUEUE_HEIGHT + H_FREE_SPACE;
						g.fill(new Rectangle2D.Double(xStart, yStart, thisWidth, thisHeight));
					}
					yStart += thisHeight;
				}
			}
			// else draw only the first MAX_NUMBER_OF_CLASSES_QUEUE with the greatest
			// number of job inside thisStation
			else {
				Vector<Object> classesReplication = new Vector<Object>(classes);
				Vector<Object> classesToBeDrawn = new Vector<Object>(0, 1);
				for (int j = 0; j < MAX_NUMBER_OF_CLASSES_QUEUE; j++) {
					double max = Double.MIN_VALUE;
					double nJobs;
					int index = 0;

					for (int k = 0; k < classesReplication.size(); k++) {
						Object thisClass = classesReplication.get(k);
						nJobs = queueState.getValue(servers.get(i), thisClass);
						if (nJobs > max) {
							max = nJobs;
							index = k;
						}
					}
					classesToBeDrawn.add(classesReplication.get(index));
					classesReplication.remove(classesReplication.get(index));
				}

				for (int j = 0; j < nClasses; j++) {
					Object thisStation = classes.get(j);
					if (classesToBeDrawn.contains(thisStation)) {
						double nJobs = queueState.getValue(servers.get(i), thisStation);
						double thisWidth = (nJobs / maxNJobs) * width;
						double thisHeight = height / MAX_NUMBER_OF_CLASSES_QUEUE;

						Color color = mediator.getClassDefinition().getClassColor(thisStation);
						g.setColor(color);
						if (cell.isLeftInputCell()) {
							xStart = p.getX() + offset + (width - thisWidth) + H_FREE_SPACE;
							g.fill(new Rectangle2D.Double(xStart, yStart, thisWidth, thisHeight));
						}
						else {
							xStart = p.getX() + offset + QUEUE_HEIGHT + H_FREE_SPACE;
							g.fill(new Rectangle2D.Double(xStart, yStart, thisWidth, thisHeight));
						}
						yStart += thisHeight;
					}
				}
			}

			//then draw the utilization
			//if the number of classes is less then MAX_NUMBER_OF_CLASSES_QUEUE
			double startAngle = 0;
			double arcDiameter = QUEUE_HEIGHT - 2;
			if (cell.isLeftInputCell()) {
				xStart = p.getX() + offset + QUEUE_WIDTH + 1;
			}
			else {
				xStart = p.getX() + offset + 1;
			}
			yStart = p.getY() + 1;
			g.setColor(Color.LIGHT_GRAY);
			g.fill(new Arc2D.Double(xStart, yStart, arcDiameter, arcDiameter, 0, 360, Arc2D.PIE));

			for (int j = 0; j < nClasses; j++) {
				double utilization = utilizationState.getValue(servers.get(i), classes.get(j));
				if (mediator.getStationDefinition().getStationNumberOfServers(servers.get(i)).intValue() > 1) {
					utilization = utilization / (mediator.getStationDefinition().getStationNumberOfServers(servers.get(i))).doubleValue();
				}
				double thisAngle = utilization * 360;

				Color color = mediator.getClassDefinition().getClassColor(classes.get(j));
				g.setColor(color);
				g.fill(new Arc2D.Double(xStart, yStart, arcDiameter, arcDiameter, startAngle, thisAngle, Arc2D.PIE));
				startAngle += thisAngle;
			}
		}
	}

}
