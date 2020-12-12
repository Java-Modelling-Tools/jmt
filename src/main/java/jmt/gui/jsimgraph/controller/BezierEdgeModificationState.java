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

import jmt.gui.jsimgraph.JGraphMod.*;
import jmt.gui.jsimgraph.UtilPoint;
import jmt.gui.jsimgraph.definitions.JMTArc;
import jmt.gui.jsimgraph.definitions.JMTPath;
import jmt.gui.jsimgraph.definitions.JMTPoint;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.PortView;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.ArrayList;
/**
 * <p>Title:  Handles all the events when the user modifies a Bezier connection </p>
 * <p>Description: This class handles all the all the possible actions when modifying a Bezier connection</p>
 *
 * @author Emma Bortone
 *         Date: 2020
 *
 */

public class BezierEdgeModificationState extends UIStateDefault {

	private JmtEdge edge = null;
	private  java.util.List controlPoints = new ArrayList<>();


	private Point2D selectedPoint =null; //control point where the user clicked
	private GraphMouseListener ml;//reference to mouse listener

	private Graphics2D dashedGraphics =null;
	protected JMTPath path; //edge path (modified when validating an action)
	protected  Point2D source;
	protected Point2D target;
	private JMTPath path_tmp; //temporary edge path
	private String indexCurrentArc; //index of the arc where the user clicked
	private int typeControlPoint; //type of the control point (0 = source; 1 = first tangent; 2 = scd tangent; 3 = target)
	private boolean lockedTangent; //true if tangent is locked
	private boolean brokenArc; //true if the arc is broken

	protected String state="SELECT"; //can be SELECT; ADD_TGTS; DEL_POINT; UNLOCK; BREAK; ADD_POINT
	protected Point2D current; // current position of the mouse


	/** Creates the select state
	 *
	 * @param mediator
	 * @param ml
	 */
	public BezierEdgeModificationState(Mediator mediator, GraphMouseListener ml) {
		super(mediator);
		this.ml = ml;
	}

	/**
	 * Handles press event, it selects the cell that is under the pointer
	 * If there is no cell deselects. There is also the possibility of
	 * activating the marquee handler
	 *
	 * @param e press mouse event
	 */
	@Override
	public void handlePress(MouseEvent e) {
//		System.out.println("BezierEdgeModificationState -> handlePress click on x="+e.getX()+"  y="+e.getY());
		if (state.equals("SELECT") || state.equals("DEL_POINT") || state.equals("UNLOCK") || state.equals("BREAK") || state.equals("ADD_TGTS")) {
			if (selectedPoint == null && (!e.isConsumed()) ){
				boolean pressedOnControlPoint = false;
				if (selectControlPoint(e.getPoint())){
					pressedOnControlPoint =true;

					if (state.equals("UNLOCK") && (typeControlPoint==1 || typeControlPoint ==2)){
						lockedTangent=false;
					}
				}
				if (!pressedOnControlPoint){
					mediator.hideBezierEditingPanel();
					ml.setSelectState();
					ml.setHandle(null);
					ml.getCurrentState().handlePress(e);
				}else {
					e.consume();
				}
			}
		}
		if(state.equals("ADD_POINT")){
			if (!e.isConsumed()){
				boolean pressedOnArc = false;
				if (selectArc(e.getPoint())){
					pressedOnArc =true;
				}
				if (!pressedOnArc){
					ml.setSelectState();
					mediator.hideBezierEditingPanel();
					ml.setHandle(null);
					ml.getCurrentState().handlePress(e);
				}else {
					e.consume();
				}
			}
		}
	}

	@Override
	public void handleMove(MouseEvent e) {
		if (!e.isConsumed()) {
			if (state.equals("SELECT") || state.equals("DEL_POINT")) {
				Graphics2D g = mediator.getGraphGraphics();
				highlightPoints(g, e.getPoint());

				current = mediator.snap(e.getPoint());
			}
			if (state.equals("UNLOCK")) {
				Graphics2D g = mediator.getGraphGraphics();
				highlightTangentsPoints(g, e.getPoint());

				current = mediator.snap(e.getPoint());
			}
			if (state.equals("BREAK")){
				Graphics2D g = mediator.getGraphGraphics();
				highlightIntermediaryPoints(g, e.getPoint());

				current = mediator.snap(e.getPoint());
			}
			if (state.equals("ADD_TGTS")){
				Graphics2D g = mediator.getGraphGraphics();
				highlightSharpIntermediaryPoint(g, e.getPoint());

				current = mediator.snap(e.getPoint());
			}
			if (state.equals("ADD_POINT")){
				Graphics2D g = mediator.getGraphGraphics();
				highlightArc(g, e.getPoint());

				current = mediator.snap(e.getPoint());
			}
			e.consume();
		}
	}

	@Override
	public void handleDrag(MouseEvent e) {
		if (indexCurrentArc!=null && selectedPoint != null){
			if (state.equals("SELECT")){
				if (!isFirstPoint(typeControlPoint, Integer.parseInt(indexCurrentArc)) && !isLastPoint(typeControlPoint, Integer.parseInt(indexCurrentArc))) {
					if (!e.isConsumed()) {
						Graphics2D g = mediator.getGraphGraphics();
						//If broken arc, highlight other point of the broken piece
						highlightBrokenArcMergingPoint(g, e.getPoint());

						drawOverlay(g);

						current = mediator.snap(e.getPoint());

						moveControlPoints();

						drawOverlay(g);
						e.consume();
					}
				}
				else {
					if (!e.isConsumed()) {
						Graphics2D g = mediator.getGraphGraphics();

						current = mediator.snap(e.getPoint());
						snapMouseInCellRegion(g,e.isShiftDown());

						drawOverlay(g);

						moveControlPoints();

						drawOverlay(g);
						e.consume();
					}
				}
			}
		}
		if (state.equals("UNLOCK")) {
			if (!e.isConsumed()) {
				if ((selectedPoint != null)  && (indexCurrentArc != null) && (typeControlPoint==1 || typeControlPoint ==2)) {

					Graphics2D g = mediator.getGraphGraphics();
					drawOverlay(g);

					current = mediator.snap(e.getPoint());
					moveControlPoints();

					drawOverlay(g);
				}
				e.consume();
			}
		}
	}

	@Override
	public void handleRelease(MouseEvent e) {
		if (!e.isConsumed()) {
			if (state.equals("SELECT") || state.equals("UNLOCK") ) {
				if ((selectedPoint != null) && (indexCurrentArc != null)) {
					validateAction();
				}
				e.consume();
				mediator.graphRepaint();
			}
			if (state.equals("DEL_POINT")) {
				if ((selectedPoint != null) && (indexCurrentArc != null) ) {
					deleteControlPoint();
					validateAction();
				}
				e.consume();
				mediator.graphRepaint();
			}
			if (state.equals("BREAK")) {
				if ((selectedPoint != null) && (indexCurrentArc != null) ) {
					breakArc();
					validateAction();
				}
				e.consume();
				mediator.graphRepaint();
			}
			if (state.equals("ADD_TGTS")){
				if ((selectedPoint != null) && (indexCurrentArc != null) ) {
					addTangents();
					validateAction();
				}
				e.consume();
				mediator.graphRepaint();
			}
			if (state.equals("ADD_POINT")){
				if (indexCurrentArc!=  null) {
					addIntermediaryPoint();
					validateAction();
				}
				e.consume();
				mediator.graphRepaint();
			}
		}

	}

	@Override
	public void handleEnter(MouseEvent e) {
		mediator.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	/**
	 * Sets the edge to be modified
	 */
	public void setEdge(JmtEdge edge) {
		//set Edge
		this.edge = edge;

		//set control points
		JmtEdgeView edgeView = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(edge, false);
		controlPoints = new ArrayList();
		controlPoints.addAll(edgeView.getPoints());

		//set path
		path = mediator.getModel().getConnectionShape(edge.getSourceKey(),edge.getTargetKey());
		path_tmp = new JMTPath(path);

		// set source and target
		 source = ((PortView) edgeView.getSource()).getLocation();
		 target =((PortView) edgeView.getTarget()).getLocation();

	}

	/**
	 * Updates the state
	 */
	public void setState(String state){
		this.state=state;
		this.selectedPoint=null;
		this.indexCurrentArc=null;
	};

	/**
	 * This function updates the path according to the temporary path and re-initialize the state
	 * This function is used when the users release the mouse after a modification
	 */
	private void validateAction() {
		path.setArcs(path_tmp.getArcs());
		selectedPoint = null;
		indexCurrentArc =null;
		mediator.editConnectionLabels(edge.getSourceKey(),edge.getTargetKey(), source, target, edge);
		mediator.graphRepaint();
		mediator.getGraph().getGraphLayoutCache().reload();
		ml.setBezierEdgeModificationState(edge);
	}

	/**
	 * Creates tolerance square around a point
	 * @param p  point
	 * @param side side of the square
	 * @return shape of the rectangle
	 */
	private Shape getToleranceRectangle(Point2D p, double side) {
		// Create a small square around the given point.
		return new Rectangle2D.Double(
				p.getX() - side / 2, p.getY() - side / 2,
				side, side);
	}

	/**
	 * Creates the dashed graphics
	 * @param g graphics2D
	 */
	private void createDashedGraphics(Graphics2D g){
		dashedGraphics = (Graphics2D) g.create();
		BasicStroke dashed;
		dashed =new BasicStroke(1.0f,
				BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER,
				10.0f, new float[]{10.0f}, 0.0f);
		dashedGraphics.setStroke(dashed);
	}

	/**
	 * This function defines the attributes :
	 *
	 *  indexCurrentArc //Integer index of the arc where the user clicked
	 * 	typeControlPoint  //Integer type of the control point
	 * 	selectedPoint //Point2D selected point
	 * 	brokenArc //Boolean true if the arc is broken
	 *
	 * @param point //point where the user clicked
	 * @return true if the user clicked on a control point
	 * 		   false otherwise
	 */
	private boolean selectControlPoint(Point2D point){
		boolean res=false;
		Integer indexControlPoint=0;
		for (int i = 0; i < controlPoints.size(); i++) {
			Shape s = getToleranceRectangle((Point2D) controlPoints.get(i), 6);
			if (s.contains(point)) {
				indexControlPoint=i;
				indexCurrentArc = Integer.toString(i *path.getArcsNb()/controlPoints.size());
				typeControlPoint =i%4;
				selectedPoint=(Point2D) controlPoints.get(i);
				brokenArc =false;
				res=true;
				break;
			}
		}
		//if the user clicked on a tangent, check that it is not a null tangent, otherwise select the origin of the tangent instead.
		if ((typeControlPoint!=0 && typeControlPoint!=3) && res) {
			if ((typeControlPoint == 1)) {
				Point2D previousPoint =(Point2D) controlPoints.get(indexControlPoint-1);
				Point2D tangentPoint = path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(0);
				double tol = 0.001;
				if (Math.abs(tangentPoint.getX()) < tol & Math.abs(tangentPoint.getX()) < tol) {
					typeControlPoint = 0;
					selectedPoint = previousPoint;
				} else { // if the tangent is not null, check if it should be locked of not
					lockedTangent = isLockedTangent(typeControlPoint,Integer.parseInt(indexCurrentArc));
				}

			}
			if ((typeControlPoint == 2)) {
				Point2D nextPoint = (Point2D) controlPoints.get(indexControlPoint+1);
				Point2D tangentPoint = path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(1);
				double tol = 0.001;
				if (Math.abs(tangentPoint.getX() )< tol & Math.abs(tangentPoint.getY()) < tol) {
					typeControlPoint = 3;
					selectedPoint = nextPoint;
				} else {// if the tangent is not null, check if it should be locked of not
					lockedTangent = isLockedTangent(typeControlPoint,Integer.parseInt(indexCurrentArc));
				}
			}
		}
		if (res && isBreakPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))){
			brokenArc=true;
		}
		return res;
	}

	/**
	 * This function defines the attribute :
	 *
	 * indexCurrentArc //Integer index of the arc where the user clicked
	 */
	private boolean selectArc(Point2D point){
		boolean res =false;
		int n = path.getArcsNb();
		GeneralPath shape;
		for (int i = 0; i < n; i++) {
			shape = createArcShape(i, path, source,target);
			double side=6;
			if ((shape.intersects(point.getX()-side/2.0,point.getY()-side/2.0,side,side))){
				res= true;
				indexCurrentArc=Integer.toString(i);
			}
		}
		return res;
	}

	/**
	 * This function is used to move the control points of an edge
	 */
	private void moveControlPoints(){
		Point2D newPoint, newTangent, symmetricOffset;

		Point2D offset = UtilPoint.subtractPoints(current,selectedPoint);
		if (typeControlPoint==0){
			if (!brokenArc) {
				// update source :
				newPoint = UtilPoint.addPoints(offset,path.getArc(Integer.parseInt(indexCurrentArc)).getSource());
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setSource(newPoint);
				if (!isFirstPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))){
					//update target :
					path_tmp.getArc(Integer.parseInt(indexCurrentArc)-1).setTarget(newPoint);
				}
			}
			else { //If arc is broken
				newPoint = UtilPoint.addPoints(offset,path.getArc(Integer.parseInt(indexCurrentArc)).getSource());
				//If the user puts the source very close to the previous target, then we assume he tried to un-break the arc
				if (Integer.parseInt(indexCurrentArc)>0 && UtilPoint.equalsWithTolerance(newPoint,path.getArc(Integer.parseInt(indexCurrentArc)-1).getTarget(),6)){
					newPoint=path.getArc(Integer.parseInt(indexCurrentArc)-1).getTarget();
				}
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setSource(newPoint);
			}
		}
		if (typeControlPoint==3){
			if (!brokenArc) {
				// update target :
				newPoint = UtilPoint.addPoints(offset,path.getArc(Integer.parseInt(indexCurrentArc)).getTarget());
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setTarget(newPoint);
				if (!isLastPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))){
					//update source :
					path_tmp.getArc(Integer.parseInt(indexCurrentArc)+1).setSource(newPoint);
				}
			}
			else { //If arc is broken
				newPoint = UtilPoint.addPoints(offset,path.getArc(Integer.parseInt(indexCurrentArc)).getTarget());
				//If the user puts the target very close to the next source, then we assume he tried to un-break the arc
				if (Integer.parseInt(indexCurrentArc)+1<path.getArcsNb() && UtilPoint.equalsWithTolerance(newPoint,path.getArc(Integer.parseInt(indexCurrentArc)+1).getSource(),6)){
					newPoint=path.getArc(Integer.parseInt(indexCurrentArc)+1).getSource();
				}
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setTarget(newPoint);
			}
		}
		if (typeControlPoint==1){
			//Case : modification tangent source
			newTangent = UtilPoint.addPoints(offset,path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(0));
			path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setFirstControlPoint(newTangent);
			if (Integer.parseInt(indexCurrentArc)-1>=0 && lockedTangent){
				//Update symmetrically tangent target of the previous arc
				symmetricOffset=UtilPoint.inversePoint(offset);
				newTangent = UtilPoint.addPoints(symmetricOffset,path.getArc(Integer.parseInt(indexCurrentArc)-1).getArcPoints().get(1));
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)-1).setSecondControlPoint(newTangent);
			}
		}
		if (typeControlPoint==2){
			//Case : modification tangent target
			newTangent = UtilPoint.addPoints(offset,path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(1));
			path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setSecondControlPoint(newTangent);
			if (Integer.parseInt(indexCurrentArc)+1<path.getArcsNb() && lockedTangent){
				//Update symmetrically tangent source of the next arc
				symmetricOffset=UtilPoint.inversePoint(offset);
				newTangent = UtilPoint.addPoints(symmetricOffset,path.getArc(Integer.parseInt(indexCurrentArc)+1).getArcPoints().get(0));
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)+1).setFirstControlPoint(newTangent);
			}
		}
	}

	/**
	 * Used when moving the input or output of an Edge
	 * Snaps the point inside the region of the station
	 */
	private void snapMouseInCellRegion(Graphics2D g, boolean free){
		JmtCell station = null;
		if ( isFirstPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))) {
			station= (JmtCell) ((DefaultPort) edge.getSource()).getParent();
		}
		if ( isLastPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))) {
			station= (JmtCell) ((DefaultPort) edge.getTarget()).getParent();
		}

		Dimension stationIconDimension =station.getIconDimension(); // full icon of the station
		Dimension stationDimension =station.getSize(mediator.getGraph()); // dimension of the station (includes title)
		Point2D stationPosition =  mediator.getCellCoordinates(station); // position of the station

		//rectangle including the full icon of the station (includes blank spaces if station is rotated)
		Rectangle2D stationRegion = new Rectangle2D.Double(stationPosition.getX()+(stationDimension.getWidth()-stationIconDimension.getWidth())/2, stationPosition.getY(),stationIconDimension.getWidth(), stationIconDimension.getHeight());
		Point2D centerStationRegion = new Point2D.Double(stationRegion.getCenterX(),stationRegion.getCenterY());

		//rectangle including only the standard icon of the station  (if rotation angle =0 then stationIconRegion and stationRegion are equal)
		Rectangle2D stationIconRegion = new Rectangle2D.Double(centerStationRegion.getX()-station.getStandardIconDimension().getWidth()/2, centerStationRegion.getY()-station.getStandardIconDimension().getHeight()/2,station.getStandardIconDimension().getWidth(), station.getStandardIconDimension().getHeight());
		//Rotate rectangle by angle
		AffineTransform tx2 = new AffineTransform();
		tx2.rotate(Math.toRadians(station.getRotationAngle()),centerStationRegion.getX(), centerStationRegion.getY());
		Shape rotatedIconRegion = tx2.createTransformedShape(stationIconRegion);

		//draw rectangle
		g.setColor(Color.lightGray);
		g.draw(rotatedIconRegion);

		current = SnappedPointOnRotatedRectangle(current,centerStationRegion,station.getRotationAngle(),station.getStandardIconDimension(), free);

	}


	private static Point2D SnappedPointOnRotatedRectangle(Point2D point, Point2D center, double theta, Dimension standardIcon, boolean free) {
		double radTheta= Math.toRadians(theta);

		// project point in the referential of the rotated rectangle
		Point2D relativePoint = UtilPoint.subtractPoints(point,center);
		Point2D pointRefRectangle = new Point2D.Double(relativePoint.getX()*Math.cos(-radTheta)-relativePoint.getY()*Math.sin(-radTheta),
				relativePoint.getX()*Math.sin(-radTheta)+relativePoint.getY()*Math.cos(-radTheta));

		// RECTANGLE REFERENTIAL ------------------------------------------------------------------------------------
		//Determine distance vector in the referential of the rotated rectangle
		//  O--x
		//  |
		//  y
		//
		//        I   |    II     |  III
		//      ======+==========+======   --yMin
		//            |V.1    V.2 |
		//       IV   |  ___|___  |   VI
		//            |V.3  | V.4 |
		//      ======+==========+======   --yMax
		//       VII  |   VIII    |   IX


		Point2D distanceRefRectangle;
		if (pointRefRectangle.getY()<-standardIcon.getHeight()/2){//ZONES I, II and III
			if (pointRefRectangle.getX()<-standardIcon.getWidth()/2){ // I
				distanceRefRectangle=new Point2D.Double(pointRefRectangle.getX()+standardIcon.getWidth()/2, pointRefRectangle.getY()+standardIcon.getHeight()/2);
			}
			else if (pointRefRectangle.getX()>standardIcon.getWidth()/2){ // III
				distanceRefRectangle=new Point2D.Double(pointRefRectangle.getX()-standardIcon.getWidth()/2, pointRefRectangle.getY()+standardIcon.getHeight()/2);
			}
			else { // II
				distanceRefRectangle=new Point2D.Double(0.0, pointRefRectangle.getY()+standardIcon.getHeight()/2);
			}
		}
		else if (pointRefRectangle.getY()>standardIcon.getHeight()/2){ //ZONES VII, VIII and IX
			if(pointRefRectangle.getX()<-standardIcon.getWidth()/2){ // ZONE VII
				distanceRefRectangle=new Point2D.Double(pointRefRectangle.getX()+standardIcon.getWidth()/2, pointRefRectangle.getY()-standardIcon.getHeight()/2);
			}
			else if (pointRefRectangle.getX()>standardIcon.getWidth()/2){ // ZONE IX
				distanceRefRectangle=new Point2D.Double(pointRefRectangle.getX()-standardIcon.getWidth()/2, pointRefRectangle.getY()-standardIcon.getHeight()/2);
			}
			else{ // ZONE VIII
				distanceRefRectangle=new Point2D.Double(0.0, pointRefRectangle.getY()-standardIcon.getHeight()/2);
			}
		}
		else {
			if(pointRefRectangle.getX()<-standardIcon.getWidth()/2){ // ZONE IV
				distanceRefRectangle=new Point2D.Double(pointRefRectangle.getX()+standardIcon.getWidth()/2, 0.0);
			}
			else if (pointRefRectangle.getX()>standardIcon.getWidth()/2){ // ZONE VI
				distanceRefRectangle=new Point2D.Double(pointRefRectangle.getX()-standardIcon.getWidth()/2, 0.0);
			}
			else{ // ZONE V
				if (free){
					distanceRefRectangle = new Point2D.Double(0.0, 0.0);
				}
				else {
					double ydistance, xdistance;
					if (pointRefRectangle.getX() > 0) { // V.2 and V.4
						if (pointRefRectangle.getY() < 0) { // V.2
							ydistance = standardIcon.getHeight() / 2 + pointRefRectangle.getY();
							xdistance = standardIcon.getWidth() / 2 - pointRefRectangle.getX();
							if (ydistance < xdistance) {
								distanceRefRectangle = new Point2D.Double(0.0, pointRefRectangle.getY() + standardIcon.getHeight() / 2);
							} else {
								distanceRefRectangle = new Point2D.Double(pointRefRectangle.getX() - standardIcon.getWidth() / 2, 0.0);
							}
						} else { //V.4
							ydistance = standardIcon.getHeight() / 2 - pointRefRectangle.getY();
							xdistance = standardIcon.getWidth() / 2 - pointRefRectangle.getX();
							if (ydistance < xdistance) {
								distanceRefRectangle = new Point2D.Double(0.0, pointRefRectangle.getY() - standardIcon.getHeight() / 2);
							} else {
								distanceRefRectangle = new Point2D.Double(pointRefRectangle.getX() - standardIcon.getWidth() / 2, 0.0);
							}
						}
					} else { // V.1 and V.3
						if (pointRefRectangle.getY() < 0) { // V.1
							ydistance = standardIcon.getHeight() / 2 + pointRefRectangle.getY();
							xdistance = standardIcon.getWidth() / 2 + pointRefRectangle.getX();
							if (ydistance < xdistance) {
								distanceRefRectangle = new Point2D.Double(0.0, pointRefRectangle.getY() + standardIcon.getHeight() / 2);
							} else {
								distanceRefRectangle = new Point2D.Double(pointRefRectangle.getX() + standardIcon.getWidth() / 2, 0.0);
							}
						} else { //V.3
							ydistance = standardIcon.getHeight() / 2 - pointRefRectangle.getY();
							xdistance = standardIcon.getWidth() / 2 + pointRefRectangle.getX();
							if (ydistance < xdistance) {
								distanceRefRectangle = new Point2D.Double(0.0, pointRefRectangle.getY() - standardIcon.getHeight() / 2);
							} else {
								distanceRefRectangle = new Point2D.Double(0.0, pointRefRectangle.getY() - standardIcon.getHeight() / 2);
							}
						}
					}
				}
			}
		}

		//snap the point to the boundaries of the rectangle
		Point2D snappedPointRefRectangle = UtilPoint.subtractPoints(pointRefRectangle, distanceRefRectangle);
		//  ----------------------------------------------------------------------------------------------------------

		// project point back in the main referential
		Point2D snappedRelativePoint = new Point2D.Double(snappedPointRefRectangle.getX()*Math.cos(radTheta)-snappedPointRefRectangle.getY()*Math.sin(radTheta),
				snappedPointRefRectangle.getX()*Math.sin(radTheta)+snappedPointRefRectangle.getY()*Math.cos(radTheta));

		return UtilPoint.addPoints(snappedRelativePoint, center);
	}



	/**
	 * This function draws the new form of the edge while the user drags a control point
	 * @param g graphics2D
	 */
	private void drawOverlay(Graphics2D g) {
		//setting environment
		Color bg = mediator.getGraphBackground();
		Color fg = Color.BLUE;
		g.setColor(bg);
		g.setXORMode(fg);
		createDashedGraphics(g);

		//draw the new path
		int n = path_tmp.getArcsNb();

		GeneralPath shape = new GeneralPath();
		for (int i = 0; i < n; i++) {
			shape.append(createArcShape(i, path_tmp, source, target),false);
		}
		dashedGraphics.draw(shape);
		drawTangents(dashedGraphics,source, target);
	}

	/**
	 * This function draws the new form of the tangent at the control point the user is dragging
	 * @param g graphics2D
	 */
	private void drawTangents(Graphics2D g, Point2D source, Point2D target) {
		if (typeControlPoint==0){
			if (!brokenArc && !isFirstPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))){
				Point2D tangentLeft, tangentRight;
				tangentLeft = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(1), current);
				tangentRight = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)+1).getArcPoints().get(0), current);
				g.draw(new Line2D.Double(current.getX(), current.getY(), tangentRight.getX(), tangentRight.getY()));
				g.draw(new Line2D.Double(tangentLeft.getX(), tangentLeft.getY(), current.getX(), current.getY()));
			}
			else {
				Point2D tangentRight = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(0), current);
				g.draw(new Line2D.Double(current.getX(), current.getY(), tangentRight.getX(), tangentRight.getY()));
			}

		}
		if ( typeControlPoint==3 ){
			if (!brokenArc && !isLastPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))) {
				Point2D tangentLeft, tangentRight;
				tangentLeft = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(1), current);
				tangentRight = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc) + 1).getArcPoints().get(0), current);
				g.draw(new Line2D.Double(tangentLeft.getX(), tangentLeft.getY(), current.getX(), current.getY()));
				g.draw(new Line2D.Double(current.getX(), current.getY(), tangentRight.getX(), tangentRight.getY()));
				}
			else {
				Point2D tangentLeft;
				tangentLeft = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(1), current);
				g.draw(new Line2D.Double(tangentLeft.getX(), tangentLeft.getY(), current.getX(), current.getY()));
			}
		}
		else if (typeControlPoint==1){
			Point2D tangentLeft, originPoint;
			originPoint = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)).getSource(), source);
			g.draw(new Line2D.Double(current.getX(), current.getY(), originPoint.getX(), originPoint.getY()));
			if (Integer.parseInt(indexCurrentArc) > 0 && lockedTangent){
				Point2D offset = UtilPoint.inversePoint(UtilPoint.subtractPoints(current,originPoint));
				tangentLeft = UtilPoint.addMultiplePoints(originPoint,offset);
				g.draw(new Line2D.Double(originPoint.getX(), originPoint.getY(), tangentLeft.getX(), tangentLeft.getY()));
			}
		}
		else if (typeControlPoint==2){
			Point2D tangentRight, originPoint;
			if (Integer.parseInt(indexCurrentArc)<path_tmp.getArcsNb()-1) {
				if (lockedTangent) {
					originPoint = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)).getTarget(), source );
					g.draw(new Line2D.Double(current.getX(), current.getY(), originPoint.getX(), originPoint.getY()));

					Point2D offset = UtilPoint.inversePoint(UtilPoint.subtractPoints(current,originPoint));
					tangentRight =  UtilPoint.addMultiplePoints(originPoint,offset);
					g.draw(new Line2D.Double(originPoint.getX(), originPoint.getY(), tangentRight.getX(), tangentRight.getY()));
				}
				else {
					originPoint = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)).getTarget(), source );
					g.draw(new Line2D.Double(current.getX(), current.getY(), originPoint.getX(), originPoint.getY()));
				}
			}
			else {
				originPoint = UtilPoint.addPoints(path_tmp.getArc(Integer.parseInt(indexCurrentArc)).getTarget(), target );
				g.draw(new Line2D.Double(current.getX(), current.getY(), originPoint.getX(), originPoint.getY()));
			}
		}
	}

	/**
	 * This function role is to highlight the control points (intermediary points and tangent points)
	 * 	Used to indicate the points that can be moved around
	 * @param g graphics2D
	 * @param point  point where the mouse is
	 */
	private void highlightPoints(Graphics2D g, Point2D point) {
		for (int i = 0; i < controlPoints.size(); i++) {
			highlightPoint(g, point, (Point2D) controlPoints.get(i));
		}
	}

	/**
	 * This function role is to highlight the tangent points
	 * 	Used to indicate the points where the tangents can be unlocked
	 * @param g graphics2D
	 * @param point  point where the mouse is
	 */
	private void highlightTangentsPoints(Graphics2D g, Point2D point) {
		for (int i = 1; i < controlPoints.size()-1; i++) {
			Point2D tgt = (Point2D) controlPoints.get(i);
			if (i%4 == 1 ){ //If it's a tangent at the source
				Point2D sourceTgt = (Point2D)  controlPoints.get(i-1);
				if (!UtilPoint.equalsWithTolerance(sourceTgt,tgt)){
					highlightPoint(g, point, tgt);
				}
			}
			if (i%4 == 2){ //If it's a tangent at the target
				Point2D sourceTgt = (Point2D)  controlPoints.get(i+1);
				if (!UtilPoint.equalsWithTolerance(sourceTgt,tgt)){
					highlightPoint(g,  point, tgt);
				}
			}
		}
	}

	/**
	 * This function role is to highlight the intermediary points
	 * Used to indicate the points where the edge can be broken
	 * @param g graphics2D
	 * @param point point where the mouse is
	 */
	private void highlightIntermediaryPoints(Graphics2D g, Point2D point) {
		for (int i = 2; i < controlPoints.size()-2; i++) {
			if (i%4 == 0 || i%4 == 3){ //If it's an intermediary point
				highlightPoint(g,  point, (Point2D) controlPoints.get(i));
			}
		}
	}

	/**
	 * This function role is to highlight the control point when a user is going to merge a broken edge back with
	 * its other piece
	 * @param g graphics2D
	 * @param point point where the mouse is
	 */
	private void highlightBrokenArcMergingPoint(Graphics2D g, Point2D point) {
		for (int i = 2; i < controlPoints.size()-2; i++) {
			if (brokenArc && ((typeControlPoint==0  && (i==(Integer.parseInt(indexCurrentArc)-1)*4+3)) || (typeControlPoint==3 && (i==(Integer.parseInt(indexCurrentArc)+1)*4)))){
				highlightPoint(g,  point, (Point2D) controlPoints.get(i));
			}
		}
	}

	/**
	 * This function role is to highlight the intermediary point that have null tangent
	 * @param g graphics2D
	 * @param point point where the mouse is
	 */
	private void highlightSharpIntermediaryPoint(Graphics2D g, Point2D point) {
		Point2D targetTgt, sourceTgt, intPoint;
		//first point
		sourceTgt=(Point2D)  controlPoints.get(1);
		intPoint = (Point2D)  controlPoints.get(0);
		if (UtilPoint.equalsWithTolerance(sourceTgt,intPoint)){
			highlightPoint(g,  point, intPoint);
		}
		//last point
		targetTgt=(Point2D)  controlPoints.get(controlPoints.size()-2);
		intPoint = (Point2D)  controlPoints.get(controlPoints.size()-1);
		if (UtilPoint.equalsWithTolerance(targetTgt,intPoint)){
			highlightPoint(g,  point, intPoint);
		}
		for (int i = 1; i < controlPoints.size()-2; i++) {
			Integer arcIndex = i *path.getArcsNb()/controlPoints.size();
			intPoint = (Point2D)  controlPoints.get(i);
			if (i%4 == 0 ){ //If it's a source point
				sourceTgt=(Point2D)  controlPoints.get(i+1);
				targetTgt=(Point2D)  controlPoints.get(i-2);
				if (!isBreakPoint(i%4,arcIndex)){ //if arc not broken, highlight if at least one of the tgt is null
					if (UtilPoint.equalsWithTolerance(sourceTgt,intPoint) || UtilPoint.equalsWithTolerance(targetTgt,intPoint)){
						highlightPoint(g,  point, intPoint);
					}
				}
				else{//if arc broken, highlight if the tgt at the source is null
					if (UtilPoint.equalsWithTolerance(sourceTgt,intPoint)){
						highlightPoint(g,  point, intPoint);
					}
				}
			}
			if (i%4 == 3 ){ //If it's a target point
				targetTgt=(Point2D)  controlPoints.get(i-1);
				sourceTgt=(Point2D)  controlPoints.get(i+2);
				if (!isBreakPoint(i%4,arcIndex)){ //if arc not broken, highlight if at least one of the tgt is null
					if (UtilPoint.equalsWithTolerance(sourceTgt,intPoint) || UtilPoint.equalsWithTolerance(targetTgt,intPoint)){
						highlightPoint(g,  point, intPoint);
					}
				}
				else{//if arc broken, highlight if the tgt at the target is null
					if (UtilPoint.equalsWithTolerance(targetTgt,intPoint)){
						highlightPoint(g,  point, intPoint);
					}
				}
			}
		}
	}

	/**
	 * This function highlights the given control point if the mouse point is on it
	 * @param g : graphics 2D
	 * @param mousePoint : Point where the mouse is
	 * @param controlPoint : Control Point
	 */
	private void highlightPoint(Graphics2D g, Point2D mousePoint, Point2D controlPoint){
		int side = 6;
		Shape s;
		s = getToleranceRectangle(controlPoint, side);
		if (s.contains(mousePoint)) {
			drawRectangle(g, side,controlPoint, Color.RED);
		} else {
			drawRectangle(g, side, controlPoint, Color.GREEN);
		}
	}

	/**
	 *  this function highlight the portion of edge where the new point will be added
	 */
	private void highlightArc(Graphics2D g, Point2D point) {
		int n = path.getArcsNb();
		GeneralPath shape;
		for (int i = 0; i < n; i++) {
			shape = createArcShape(i, path, source, target);
			double side=6;
			if ((shape.intersects(point.getX()-side/2.0,point.getY()-side/2.0,side,side))){
				Color fg = Color.RED;
				createDashedGraphics(g);
				dashedGraphics.setStroke(GraphConstants.SELECTION_STROKE);
				dashedGraphics.setColor(fg);
				dashedGraphics.draw(shape);
				shape.reset();
			}
			else {
				Color fg = Color.GREEN;
				createDashedGraphics(g);
				dashedGraphics.setStroke(GraphConstants.SELECTION_STROKE);
				dashedGraphics.setColor(fg);
				dashedGraphics.draw(shape);
				shape.reset();
			}
		}
	}

	/**
	 * This function draws a square
	 * @param graphics Graphics2D
	 * @param side size of the sides of the square
	 * @param topLeftPoint top left point of the square
	 * @param color color of the square
	 */
	private void drawRectangle(Graphics2D graphics, int side, Point2D topLeftPoint, Color color) {
		Rectangle2D rect = new Rectangle2D.Double(
				topLeftPoint.getX() - side / 2.0, topLeftPoint.getY() - side / 2.0,
				side, side);
		//Draw in black the outline of the rectangle
		graphics.setColor(Color.BLACK);
		graphics.draw(rect);

		//Fill the rectangle with the chosen color
		graphics.setColor(color);
		graphics.fill(rect);
	}

	/**
	 * This function deletes the selected control point
	 */
	private void deleteControlPoint() {
		if (typeControlPoint==0 && !isFirstPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))){
			// Delete arc
			ArrayList newControlPoints = new ArrayList();
			newControlPoints.add(path.getArc(Integer.parseInt(indexCurrentArc)-1).getArcPoints().get(0));
			newControlPoints.add(path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(1));
			JMTArc newArc= new JMTArc(path.getArc(Integer.parseInt(indexCurrentArc)-1).getSource(),newControlPoints,path.getArc(Integer.parseInt(indexCurrentArc)).getTarget());

			path_tmp.getArcs().remove(Integer.parseInt(indexCurrentArc)-1);
			path_tmp.getArcs().remove(Integer.parseInt(indexCurrentArc)-1);
			path_tmp.getArcs().add(Integer.parseInt(indexCurrentArc)-1,newArc);
		}
		if (typeControlPoint==3 && !isLastPoint(typeControlPoint,Integer.parseInt(indexCurrentArc))){
			// Delete arc
			ArrayList newControlPoints = new ArrayList();
			newControlPoints.add(path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(0));
			newControlPoints.add(path.getArc(Integer.parseInt(indexCurrentArc)+1).getArcPoints().get(1));
			JMTArc newArc= new JMTArc(path.getArc(Integer.parseInt(indexCurrentArc)).getSource(),newControlPoints,path.getArc(Integer.parseInt(indexCurrentArc)+1).getTarget());

			path_tmp.getArcs().remove(Integer.parseInt(indexCurrentArc));
			path_tmp.getArcs().remove(Integer.parseInt(indexCurrentArc));
			path_tmp.getArcs().add(Integer.parseInt(indexCurrentArc),newArc);
		}
		if (typeControlPoint==1){
			path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setFirstControlPoint(new Point2D.Double(0,0));
		}
		if (typeControlPoint==2){
			path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setSecondControlPoint(new Point2D.Double(0,0));
		}
	}

	/**
	 * This function breaks an edge at the selected intermediary point
	 */
	private void breakArc(){
		Point2D offset = new Point2D.Double(20.0,0.0);
//		System.out.println("breakArc");
		if (typeControlPoint==0 && Integer.parseInt(indexCurrentArc)>0){
			Point2D newSource = UtilPoint.addPoints(path.getArc(Integer.parseInt(indexCurrentArc)).getSource(),offset);
			Point2D newTarget = UtilPoint.subtractPoints(path.getArc(Integer.parseInt(indexCurrentArc)-1).getTarget(),offset);
			path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setSource(newSource);
			path_tmp.getArc(Integer.parseInt(indexCurrentArc)-1).setTarget(newTarget);
		}
		if (typeControlPoint==3 && (Integer.parseInt(indexCurrentArc) < path.getArcsNb()-1)){
			Point2D newSource = UtilPoint.addPoints(path.getArc(Integer.parseInt(indexCurrentArc)+1).getSource(),offset);
			Point2D newTarget = UtilPoint.subtractPoints(path.getArc(Integer.parseInt(indexCurrentArc)).getTarget(),offset);
			path_tmp.getArc(Integer.parseInt(indexCurrentArc)+1).setSource(newSource);
			path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setTarget(newTarget);
		}
	}

	/**
	 * This function add tangents to a sharp intermediary point
	 */
	private void addTangents(){
		Point2D offset = new Point2D.Double(50.0,0.0);
//		System.out.println("addTangents");
		if (typeControlPoint==0) {
			if (UtilPoint.equalsWithTolerance(path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(0),new Point2D.Double(0.0,0.0))){
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setFirstControlPoint(offset);
			}
			if (!brokenArc && !isFirstPoint(typeControlPoint,Integer.parseInt(indexCurrentArc)) && UtilPoint.equalsWithTolerance(path.getArc(Integer.parseInt(indexCurrentArc)-1).getArcPoints().get(1),new Point2D.Double(0.0,0.0))){
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)-1).setSecondControlPoint(UtilPoint.inversePoint(offset));
			}
		}
		else if (typeControlPoint==3){
			if (UtilPoint.equalsWithTolerance(path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(1),new Point2D.Double(0.0,0.0))){
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)).setSecondControlPoint(UtilPoint.inversePoint(offset));
			}
			if (!brokenArc && !isLastPoint(typeControlPoint,Integer.parseInt(indexCurrentArc)) && UtilPoint.equalsWithTolerance(path.getArc(Integer.parseInt(indexCurrentArc)+1).getArcPoints().get(0),new Point2D.Double(0.0,0.0))){
				path_tmp.getArc(Integer.parseInt(indexCurrentArc)+1).setFirstControlPoint(offset);
			}
		}
	}

	/**
	 * This function add an intermediary point with null tangent
	 */
	private void addIntermediaryPoint(){
		ArrayList newControlPoints1 = new ArrayList();
		ArrayList newControlPoints2 = new ArrayList();

		Point2D sourceArc = UtilPoint.addPoints(path.getArc(Integer.parseInt(indexCurrentArc)).getSource(),source);
		Point2D targetArc;

		if (Integer.parseInt(indexCurrentArc)==path.getArcsNb()-1){
			 targetArc =  UtilPoint.addPoints(path.getArc(Integer.parseInt(indexCurrentArc)).getTarget(),target);
		}
		else {
			 targetArc =  UtilPoint.addPoints(path.getArc(Integer.parseInt(indexCurrentArc)).getTarget(),source);
		}

		double x_newIntermediaryPoint = Math.abs(sourceArc.getX()-targetArc.getX())/2.0 + Math.min(sourceArc.getX(),targetArc.getX());
		double y_newIntermediaryPoint = Math.abs(sourceArc.getY()-targetArc.getY())/2.0 + Math.min(sourceArc.getY(),targetArc.getY());
		Point2D newIntermediaryPoint = UtilPoint.subtractPoints(new Point2D.Double(x_newIntermediaryPoint,y_newIntermediaryPoint),source);

		newControlPoints1.add(path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(0));
		newControlPoints1.add(new Point2D.Double(0.0,0.0));

		newControlPoints2.add(new Point2D.Double(0.0,0.0));
		newControlPoints2.add(path.getArc(Integer.parseInt(indexCurrentArc)).getArcPoints().get(1));

		JMTArc newArc1= new JMTArc(UtilPoint.subtractPoints(sourceArc,source),newControlPoints1,newIntermediaryPoint);
		JMTArc newArc2;
		if (Integer.parseInt(indexCurrentArc)==path.getArcsNb()-1){
			 newArc2= new JMTArc(newIntermediaryPoint,newControlPoints2,UtilPoint.subtractPoints(targetArc,target));
		}
		else {
			 newArc2= new JMTArc(newIntermediaryPoint,newControlPoints2,UtilPoint.subtractPoints(targetArc,source));
		}

		path_tmp.getArcs().remove(Integer.parseInt(indexCurrentArc));
		path_tmp.getArcs().add(Integer.parseInt(indexCurrentArc),newArc1);
		path_tmp.getArcs().add(Integer.parseInt(indexCurrentArc)+1,newArc2);

	}

	/**
	 * This function returns true if the selected point is the point connected to the source station
	 * @return true if the selected point is the first control point
	 */
	private boolean isFirstPoint(Integer type, Integer arcIndex){
		return (type==0 && arcIndex==0);
	}

	/**
	 * This function returns true if the selected point is the point connected to the target station
	 * @return true if the selected point is the last control point
	 */
	private boolean isLastPoint(Integer type, Integer arcIndex){
		return (type==3 && arcIndex==path.getArcsNb()-1);
	}

	/**
	 * This function returns true if the selected point is the point where an edge break
	 * @return true if the selected point is a break point
	 */
	private boolean isBreakPoint(Integer type, Integer arcIndex){
		boolean res=false;
		Point2D sourcePath = (Point2D) controlPoints.get(0);
		if (type==0  && arcIndex>0){
			Point2D source = UtilPoint.addPoints(sourcePath,path.getArc(arcIndex).getSource());
			Point2D target = UtilPoint.addPoints(sourcePath,path.getArc(arcIndex-1).getTarget());
			res= !UtilPoint.equalsWithTolerance(source, target);
		}
		else if (type==3 && arcIndex<path.getArcsNb()-1){
			Point2D source = UtilPoint.addPoints(sourcePath,path.getArc(arcIndex+1).getSource());
			Point2D target = UtilPoint.addPoints(sourcePath,path.getArc(arcIndex).getTarget());
			res= !UtilPoint.equalsWithTolerance(source, target);
		}
		return (res);
	}

	/**
	 * This function returns true if the selected point is a locked tangent
	 * @return true if the selected point is a locked tangent
	 */
	private boolean isLockedTangent(Integer type, Integer arcIndex){
		boolean res=true;
		if (type == 1 && arcIndex > 0){
			Point2D tangentPoint = path.getArc(arcIndex).getArcPoints().get(0);
			Point2D symmetricTangent= path.getArc(arcIndex - 1).getArcPoints().get(1);
			if (!UtilPoint.equalsWithTolerance(tangentPoint, UtilPoint.inversePoint(symmetricTangent))) {
				res = false;
			}
			//if broken arc, unlock tangent
			if (isBreakPoint(0,arcIndex)){
				res = false;
			}
		}
		else if (type == 2 && arcIndex <path.getArcsNb()-1){
			Point2D tangentPoint = path.getArc(arcIndex).getArcPoints().get(1);
			Point2D symmetricTangent= path.getArc(arcIndex + 1).getArcPoints().get(0);
			if (!UtilPoint.equalsWithTolerance(tangentPoint, UtilPoint.inversePoint(symmetricTangent))) {
				res = false;
			}
			//if broken arc, unlock tangent
			if (isBreakPoint(3,arcIndex)){
				res = false;
			}
		}
		return (res);
	}

	/**Creates the shape of a given arc
	 *
	 * @param indexArc index of the arc
	 * @param jmtPath  path from where the arc must be drawn
	 * @return path of the arc
	 */
	private GeneralPath createArcShape(Integer indexArc, JMTPath jmtPath, Point2D source, Point2D target){
		int n = jmtPath.getArcsNb();
		GeneralPath shape = new GeneralPath();
		Point2D  controlPointSource, controlPointTarget, sourceOfArc, targetOfArc;

		if (indexArc<n-1) {
			sourceOfArc = new Point2D.Double(jmtPath.getArc(indexArc).getSource().getX() + source.getX(), jmtPath.getArc(indexArc).getSource().getY() + source.getY());
			targetOfArc = new Point2D.Double(jmtPath.getArc(indexArc).getTarget().getX() + source.getX(), jmtPath.getArc(indexArc).getTarget().getY() + source.getY());
			controlPointSource = new Point2D.Double(jmtPath.getArc(indexArc).getArcPoints().get(0).getX() + sourceOfArc.getX(), jmtPath.getArc(indexArc).getArcPoints().get(0).getY() + sourceOfArc.getY());
			controlPointTarget = new Point2D.Double(jmtPath.getArc(indexArc).getArcPoints().get(1).getX() + targetOfArc.getX(), jmtPath.getArc(indexArc).getArcPoints().get(1).getY() + targetOfArc.getY());

			shape.moveTo((float) sourceOfArc.getX(), (float) sourceOfArc.getY());
			shape.curveTo((float) controlPointSource.getX(), (float) controlPointSource.getY()
					, (float) controlPointTarget.getX(), (float) controlPointTarget.getY()
					, (float) targetOfArc.getX(), (float) targetOfArc.getY());
			shape.moveTo((float) targetOfArc.getX(), (float) targetOfArc.getY());
		}
		else{
			sourceOfArc = new Point2D.Double(jmtPath.getArc(indexArc).getSource().getX() + source.getX(), jmtPath.getArc(indexArc).getSource().getY() + source.getY());
			targetOfArc = new Point2D.Double(jmtPath.getArc(indexArc).getTarget().getX() + target.getX(), jmtPath.getArc(indexArc).getTarget().getY() + target.getY());
			controlPointSource = new Point2D.Double(jmtPath.getArc(indexArc).getArcPoints().get(0).getX() + sourceOfArc.getX(), jmtPath.getArc(indexArc).getArcPoints().get(0).getY() + sourceOfArc.getY());
			controlPointTarget = new Point2D.Double(jmtPath.getArc(indexArc).getArcPoints().get(1).getX() + targetOfArc.getX(), jmtPath.getArc(indexArc).getArcPoints().get(1).getY() + targetOfArc.getY());

			shape.moveTo((float) sourceOfArc.getX(), (float) sourceOfArc.getY());
			shape.curveTo((float) controlPointSource.getX(), (float) controlPointSource.getY()
					, (float) controlPointTarget.getX(), (float) controlPointTarget.getY()
					, (float) targetOfArc.getX(), (float) targetOfArc.getY());
			shape.moveTo((float) targetOfArc.getX(), (float) targetOfArc.getY());
		}
		return shape;
	}



}
