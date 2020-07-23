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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Vector;

import jmt.engine.jaba.DPoint;
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
	 * @throws Exception
	 *
	 * Modified in 2020 by Emma Bortone to include free rotation
	 */
	public void draw(ModelSnapshot queueState, ModelSnapshot utilizationState) throws Exception {
		int MAX_NUMBER_OF_CLASSES_QUEUE = Defaults.getAsInteger("representableClasses").intValue();
		double nClasses = mediator.getClassDefinition().getClassKeys().size();
		double maxNJobs = queueState.getMaxValue();

		//used to avoid extreme queue length variation when the number of jobs inside the system is very small
		if (maxNJobs < 10) {
			maxNJobs = 10;
		}


		//FOR THE SOURCES ***********************************************
		double SOURCE_SIZE = (JMTImageLoader.loadImage("Source")).getIconWidth();
		double SOURCE_MARGIN = 9;
		double source_usable_space = SOURCE_SIZE - SOURCE_MARGIN * 2;
		// *************************************************************

		//FOR THE QUEUES ***********************************************
		double QUEUE_STATION_LENGTH =  (JMTImageLoader.loadImage("Queue")).getIconWidth(); //length of the whole station
		double QUEUE_STATION_HEIGHT =  (JMTImageLoader.loadImage("Queue")).getIconHeight(); //height of the whole station

		double QUEUE_BORDER = 1; // border of the queue station (looks like a shadow on the image)

		double QUEUE_LENGTH = (QUEUE_STATION_LENGTH-QUEUE_STATION_HEIGHT)-2*QUEUE_BORDER;  //length of only the queue (without the shadow borders)
		double QUEUE_HEIGHT =QUEUE_STATION_HEIGHT-2*QUEUE_BORDER;  //height of only the queue (without the shadow borders)
		// *************************************************************

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

			Point2D center = getCenter(cell);
			double xStart = center.getX() - source_usable_space/2;
			double yStart = center.getY() - source_usable_space/2;

			//draw whole source in grey
			drawClassRectangle( cell, g, Color.lightGray,   center,  xStart,   yStart, source_usable_space, source_usable_space);

			//if the number of class is less than the maximum representable
			if (classCount < MAX_NUMBER_OF_CLASSES_QUEUE) {
				double classSpace = source_usable_space / classCount;
				for (int j = 0; j < classCount; j++) {
					Object thisClass = refClasses.get(j);

					drawClassRectangle( cell, g, mediator.getClassDefinition().getClassColor(thisClass),   center,  xStart,   yStart, classSpace, source_usable_space);
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

					drawClassRectangle( cell, g, mediator.getClassDefinition().getClassColor(thisClass),   center,  xStart,   yStart, classSpace, source_usable_space);
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
			double yStart;
			double xStart;

			Point2D center =getCenter(cell);


			xStart = center.getX()-QUEUE_STATION_LENGTH/2;
			yStart= center.getY()-QUEUE_STATION_HEIGHT/2;

			//draw whole queue in  grey
			drawClassRectangle(cell, g, Color.lightGray, center, xStart, yStart, QUEUE_LENGTH, QUEUE_HEIGHT);

			//First draw the queues ....
			//if the number of classes is less than MAX_NUMBER_OF_CLASSES_QUEUE
			if (nClasses <= MAX_NUMBER_OF_CLASSES_QUEUE) {

				for (int j = 0; j < nClasses; j++) {

					double nJobs = queueState.getValue(servers.get(i), classes.get(j));
					double thisWidth = (nJobs / maxNJobs) * QUEUE_LENGTH;
					double thisHeight = (QUEUE_STATION_HEIGHT) / nClasses;

					xStart = center.getX()-QUEUE_STATION_LENGTH/2+(QUEUE_LENGTH-thisWidth);

					Color color = mediator.getClassDefinition().getClassColor(classes.get(j));
					drawClassRectangle(cell, g, color, center, xStart, yStart, thisWidth, thisHeight);
					yStart+=thisHeight;

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
						double thisWidth = (nJobs / maxNJobs) * QUEUE_LENGTH;
						double thisHeight = QUEUE_STATION_HEIGHT / MAX_NUMBER_OF_CLASSES_QUEUE;

						xStart = center.getX()-QUEUE_STATION_LENGTH/2+(QUEUE_LENGTH-thisWidth);

						Color color = mediator.getClassDefinition().getClassColor(thisStation);

						drawClassRectangle(cell, g, color, center, xStart, yStart, thisWidth, thisHeight);
						yStart+=thisHeight;
					}
				}
			}

			//then draw the utilization
			//if the number of classes is less than MAX_NUMBER_OF_CLASSES_QUEUE
			double startAngle=0.0;
			xStart= center.getX()+QUEUE_STATION_LENGTH/2-QUEUE_STATION_HEIGHT+QUEUE_BORDER;
			yStart = center.getY()-QUEUE_STATION_HEIGHT/2+0.0;
			drawUtilization(cell, g, Color.lightGray, center,xStart, yStart,  QUEUE_HEIGHT,startAngle,360);

			for (int j = 0; j < nClasses; j++) {
				double thisAngle = utilizationState.getValue(servers.get(i), classes.get(j)) * 360;
				Color color = mediator.getClassDefinition().getClassColor(classes.get(j));

				drawUtilization(cell, g,color, center, xStart, yStart,  QUEUE_HEIGHT,startAngle,thisAngle);
				startAngle+=thisAngle;
			}
		}
	}

	/**
	 * This function is used to find the center of a given station
	 * @param station station for which we want the center
	 * @return Point at the center of the station's icon
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	private Point2D getCenter(JmtCell station){
		Dimension stationIconDimension =station.getIconDimension(); // full icon of the station
		Dimension stationDimension =station.getSize(mediator.getGraph()); // dimension of the station (includes title)
		Point2D stationPosition =  mediator.getCellCoordinates(station); // position of the station
		//rectangle including the full icon of the station (includes blank spaces if station is rotated)
		Rectangle2D wideStationIconRectangle = new Rectangle2D.Double(stationPosition.getX()+(stationDimension.getWidth()-stationIconDimension.getWidth())/2, stationPosition.getY(),stationIconDimension.getWidth(), stationIconDimension.getHeight());
		Point2D centerStationRegion = new Point2D.Double(wideStationIconRectangle.getCenterX(),wideStationIconRectangle.getCenterY());
		return (centerStationRegion);
	}

	/**
	 * this function draws a rectangle representing the occupation of a queue station or of a Source station
	 * @param station station for which we draw the queue/source
	 * @param g graphics
	 * @param color color in which to draw
	 * @param rotationCenter center of rotation
	 * @param xStart x-axis of the top left point of the rectangle
	 * @param yStart y-axis of the top left point of the rectangle
	 * @param length width of the rectangle
	 * @param height height of the rectangle
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	private void drawClassRectangle( JmtCell station, Graphics2D g, Color color,   Point2D rotationCenter, double xStart,  double yStart, double length, double height){

		//rectangle including only the standard icon of the station  (if rotation angle =0 then stationIconRegion and stationRegion are equal)
		Rectangle2D QueueRectangle 	= new Rectangle2D.Double(xStart, yStart,length, height);

		//Rotate rectangle by angle
		Shape rotatedQueueRectangle= rotateShape( station.isLeftInputCell(), station.getRotationAngle(), rotationCenter, QueueRectangle);

		//draw rectangle
		g.setColor(color);
		g.fill(rotatedQueueRectangle);
	}

	/**
	 * this function draws the utilization of a queue station
	 * @param station station for which we draw the utilization
	 * @param g graphics
	 * @param color  color of in which the utilization will be drawn
	 * @param rotationCenter center of rotation
	 * @param xStart x-axis of the starting point of the arc representing the utilization
	 * @param yStart y-axis of the starting point of the arc representing the utilization
	 * @param arcDiameter diameter of the arc representing the utilization
	 * @param startAngle initial angle of the arc representing the utilization
	 * @param endAngle final angle of the arc representing the utilization
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	private void drawUtilization( JmtCell station, Graphics2D g, Color color, Point2D rotationCenter,double xStart,  double yStart, double arcDiameter, double startAngle, double endAngle){
		Arc2D utilizationCircle = new Arc2D.Double(xStart, yStart, arcDiameter, arcDiameter, startAngle, endAngle, Arc2D.PIE);
		Shape rotatedUtilisationCircle= rotateShape( station.isLeftInputCell(), station.getRotationAngle(), rotationCenter, utilizationCircle);

		g.setColor(color);
		g.fill(rotatedUtilisationCircle);

	}

	/**
	 * Rotate a given shape by a given angle around a given anchor point
	 * @param isLeftInputCell true if the cell has been mirrored, false otherwise
	 * @param angle angle of rotation
	 * @param center center of rotation
	 * @param shape shape to be rotated
	 * @return rotated shape
	 *
	 * @author Emma Bortone
	 * Date 2020
	 */
	private Shape rotateShape(boolean isLeftInputCell, double angle, Point2D center, Shape shape){
		AffineTransform tx2 = new AffineTransform();
		if (isLeftInputCell) {
			tx2.rotate(Math.toRadians(angle),center.getX(), center.getY());
		}
		else {
			tx2.rotate(Math.toRadians(angle)+Math.PI,center.getX(), center.getY());
		}

		Shape rotatedQueueRectangle = tx2.createTransformedShape(shape);
		return rotatedQueueRectangle;
	}

}
