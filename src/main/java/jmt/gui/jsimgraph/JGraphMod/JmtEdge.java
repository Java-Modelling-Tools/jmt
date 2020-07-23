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

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import jmt.gui.jsimgraph.controller.Mediator;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;

/**
 * <p>Title: JmtEdge connection structure</p>
 * <p>Description: This class is used to connect two elements into JmtGraph. It is designed to
 * store keys of source and target stations that are used when deleting or copying a connection</p>
 * 
 * @author Bertoli Marco
 *         Date: 17-giu-2005
 *         Time: 19.25.45
 * 
 * @author Giuseppe De Cicco & Fabio Granara
 * 		Date: 23-sett-2006
 * 
 */
public class JmtEdge extends DefaultEdge {

	private static final long serialVersionUID = 1L;
	protected Object sourceKey;
	protected Object targetKey;

	// These variables are used to know where the edge intersect the cell
	protected boolean upperSide = false;
	protected boolean lowerSide = false;
	protected boolean rightSide = false;
	protected boolean isRing = false;
	protected boolean leftSide = false;
	protected boolean pointSharedModified = false;
	// ______________________end____________________ 

	protected ArrayList<Point2D> intersectionVertexPoint = null;
	protected Point2D intersectionPoint = null;
	private Mediator mediator;
	public ArrayList<Point2D> intersectionEdgePoint = new ArrayList<Point2D>();

	private boolean isBezier = false;


	public JmtEdge() {
		super("");
	}

	public JmtEdge(Object o) {
		super(o);
	}

	/**
	 * Creates a new JmtEdge connecting source to target
	 * @param sourceKey key of source station
	 * @param targetKey key of target station
	 * @param mediator 
	 */
	public JmtEdge(Object sourceKey, Object targetKey, Mediator mediator) {

		this();
		this.sourceKey = sourceKey;
		this.targetKey = targetKey;
		this.mediator = mediator;
	}

	/**
	 * Creates a new JmtEdge connecting source to target
	 * @param sourceKey key of source station
	 * @param targetKey key of target station
	 * @param isBezier
	 * @param mediator
	 */
	public JmtEdge(Object sourceKey, Object targetKey, Boolean isBezier, Mediator mediator) {

		this();
		this.sourceKey = sourceKey;
		this.targetKey = targetKey;
		this.mediator = mediator;
		this.isBezier = isBezier;
	}

	/**
	 * Gets source station search key
	 * @return source key
	 */
	public Object getSourceKey() {
		return sourceKey;
	}

	/**
	 * Gets target station search key
	 * @return target key
	 */
	public Object getTargetKey() {
		return targetKey;
	}

	/**
	 * Set the attribute isBezier
	 */
	public  Boolean getIsBezier() {
		return isBezier;
	}

	// Giuseppe De Cicco & Fabio Granara
	public ArrayList<Point2D> getIntersectionVertexPoint() {
		return intersectionVertexPoint;
	}

	// Giuseppe De Cicco & Fabio Granara
	public boolean intersects(EdgeView viewtmp, Rectangle2D rett) {
		double maxX = (int) rett.getMaxX();
		double maxY = (int) rett.getMaxY();
		double minX = (int) rett.getMinX();
		double minY = (int) rett.getMinY();
		int controlPoint = viewtmp.getPointCount();
		boolean intersecato = false;
		leftSide = false;
		upperSide = false;
		rightSide = false;
		lowerSide = false;
		intersectionVertexPoint = new ArrayList<Point2D>();
		Point2D upperPoint = null;
		Point2D leftPoint = null;
		Point2D rightPoint = null;
		Point2D lowPoint = null;
		JmtCell source = (JmtCell) ((DefaultPort) this.getSource()).getParent();
		JmtCell target = (JmtCell) ((DefaultPort) this.getTarget()).getParent();
		Rectangle boundsSource = GraphConstants.getBounds(source.getAttributes()).getBounds();
		Rectangle boundsTarget = GraphConstants.getBounds(target.getAttributes()).getBounds();
		if (boundsSource.equals(rett) || (boundsTarget.equals(rett))) {
			return false;
		}

		Point2D[] controlPoints = new Point2D[controlPoint];
		for (int i = 0; i < controlPoint; i++) {
			controlPoints[i] = viewtmp.getPoint(i);
			if ((controlPoints[i] == null)) {
				return false;
			}
			controlPoints[i].setLocation((int) controlPoints[i].getX(), (int) controlPoints[i].getY());
		}

		for (int i = 0; i < (controlPoint - 1); i++) {
			Point2D tmpMin = controlPoints[i];
			Point2D tmpMax = controlPoints[i + 1];
			if ((tmpMin.getX() > tmpMax.getX()) || (tmpMin.getY() > tmpMax.getY())) {
				tmpMin = controlPoints[i + 1];
				tmpMax = controlPoints[i];
			}

			if (!((maxX < tmpMin.getX()) || (minX > tmpMax.getX()) || (minY > tmpMax.getY()) || (maxY < tmpMin.getY()))) {
				intersecato = true;
				if (!(i % 2 == 0)) {
					if (minY < tmpMax.getY() && minY > tmpMin.getY()) {
						upperSide = true;
						upperPoint = new Point2D.Double((int) tmpMin.getX(), (int) minY);
						intersectionVertexPoint.add(upperPoint);
					}
					if (maxY < tmpMax.getY() && maxY > tmpMin.getY()) {
						lowerSide = true;
						lowPoint = new Point2D.Double((int) tmpMin.getX(), (int) maxY);
						intersectionVertexPoint.add(lowPoint);
					}
				} else {
					if (minX < tmpMax.getX() && minX > tmpMin.getX()) {
						leftSide = true;
						leftPoint = new Point2D.Double((int) minX, (int) tmpMin.getY());
						intersectionVertexPoint.add(leftPoint);
					}
					if (maxX < tmpMax.getX() && maxX > tmpMin.getX()) {
						rightSide = true;
						rightPoint = new Point2D.Double((int) maxX, (int) tmpMin.getY());
						intersectionVertexPoint.add(rightPoint);
					}
				}

			}
		}

		if (leftSide && upperSide) {
			intersectionVertexPoint.set(0, upperPoint);
			intersectionVertexPoint.set(1, leftPoint);
		} else if (upperSide && rightSide) {
			intersectionVertexPoint.set(0, upperPoint);
			intersectionVertexPoint.set(1, rightPoint);
		} else if (lowerSide && rightSide) {
			intersectionVertexPoint.set(0, rightPoint);
			intersectionVertexPoint.set(1, lowPoint);
		} else if (leftSide && rightSide) {
			intersectionVertexPoint.set(0, leftPoint);
			intersectionVertexPoint.set(1, rightPoint);
		} else if (leftSide && lowerSide) {
			intersectionVertexPoint.set(0, lowPoint);
			intersectionVertexPoint.set(1, leftPoint);
		} else if (upperSide && lowerSide) {
			intersectionVertexPoint.set(0, upperPoint);
			intersectionVertexPoint.set(1, lowPoint);
		}

		return intersecato;
	}

	// Giuseppe De Cicco & Fabio Granara
	public boolean intersectsEdge() {
		Object[] cells = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots());
		JmtEdge cell = this;
		boolean intersecato = false;
		this.intersectionEdgePoint = new ArrayList<Point2D>();
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof JmtEdge && !(cells[i].equals(cell)) ) {
				if (!(((JmtEdge) cells[i]).getIsBezier())) {
					JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(cells[i], false);
					if (viewtmp != null) {
						if (cell.intersectsEdge(viewtmp)) {
							intersecato = true;
						}
					}
				}
			}
		}
		return intersecato;
	}

	// Giuseppe De Cicco & Fabio Granara
	public boolean intersectsEdge(EdgeView otherEdgeView) {
		boolean sourcesame = false;
		EdgeView edgeView = (EdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(this, false);
		JmtEdge edge2 = (JmtEdge) otherEdgeView.getCell();

		JmtCell sourceOfEdge1 = null;
		JmtCell targetOfEdge1 = null;
		JmtCell sourceOfEdge2 = null;
		JmtCell targetOfEdge2 = null;

		if ((DefaultPort) edge2.getSource() != null) {
			sourceOfEdge2 = (JmtCell) ((DefaultPort) edge2.getSource()).getParent();
		}
		if ((DefaultPort) edge2.getTarget() != null) {
			targetOfEdge2 = (JmtCell) ((DefaultPort) edge2.getTarget()).getParent();
		}
		if ((DefaultPort) this.getSource() != null) {
			sourceOfEdge1 = (JmtCell) ((DefaultPort) this.getSource()).getParent();
		}
		if ((DefaultPort) this.getTarget() != null) {
			targetOfEdge1 = (JmtCell) ((DefaultPort) this.getTarget()).getParent();
		}

		if (edgeView == null) {
			return false;
		}

		int controlPoint = edgeView.getPointCount();
		int controlPoint2 = otherEdgeView.getPointCount();

		Point2D[] controlPoints = new Point2D[controlPoint];
		for (int i = 0; i < controlPoint; i++) {
			controlPoints[i] = edgeView.getPoint(i);
			if ((controlPoints[i] == null)) {
				return false;
			}
			double x = Math.floor(controlPoints[i].getX());
			double y = Math.floor(controlPoints[i].getY());
			controlPoints[i].setLocation(x, y);
		}

		boolean intersecato = false;
		boolean sharedTheSameSourceTarget = false;
		if ((sourceOfEdge2 != null && sourceOfEdge1 != null) && (sourceOfEdge2.equals(sourceOfEdge1) || targetOfEdge1.equals(targetOfEdge2))) {
			sharedTheSameSourceTarget = true;
			GraphModel graphmodel = mediator.getGraph().getModel();
			Object[] listEdgesIn = null;
			Object[] listEdgesOut = null;
			if (targetOfEdge1.equals(targetOfEdge2)) {
				sourcesame = true;
				listEdgesIn = DefaultGraphModel.getEdges(graphmodel, targetOfEdge1, true);
				for (Object element : listEdgesIn) {
					JmtEdgeView edgeView1 = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(element, false);
					int controlPoint3 = edgeView1.getPointCount();
					Point2D[] controlPoints3 = new Point2D[controlPoint3];
					for (int x = 0; x < controlPoint3; x++) {
						controlPoints3[x] = edgeView1.getPoint(x);
						if ((controlPoints3[x] == null)) {
							return false;
						}
						double y = Math.floor(controlPoints3[x].getY());
						double x1 = Math.floor(controlPoints3[x].getX());
						controlPoints3[x].setLocation(x1, y);
					}

					if (((controlPoints3[controlPoints3.length - 2].getX() > controlPoints[controlPoints.length - 1].getX()) && (controlPoints[controlPoints.length - 2]
							.getX() > controlPoints3[controlPoints3.length - 2].getX()))
							|| (controlPoints3[controlPoints3.length - 2].getX() < controlPoints[controlPoints.length - 1].getX())
							&& (controlPoints[controlPoints.length - 2].getX() < controlPoints3[controlPoints3.length - 2].getX())) {
						boolean contiene = false;
						if (intersectionEdgePoint.contains(controlPoints3[controlPoints3.length - 2])) {
							contiene = true;
						}
						if (intersectionEdgePoint == null || !contiene) {
							intersectionEdgePoint.add(controlPoints3[controlPoints3.length - 2]);
							intersecato = true;
						}
					}
				}
			} else if (sourceOfEdge2.equals(sourceOfEdge1)) {
				sourcesame = true;
				listEdgesOut = DefaultGraphModel.getEdges(graphmodel, sourceOfEdge1, false);
				for (Object element : listEdgesOut) {
					JmtEdgeView edgeView1 = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(element, false);
					int controlPoint3 = edgeView1.getPointCount();
					Point2D[] controlPoints3 = new Point2D[controlPoint3];
					for (int x = 0; x < controlPoint3; x++) {
						controlPoints3[x] = edgeView1.getPoint(x);
						if ((controlPoints3[x] == null)) {
							return false;
						}
						double y = Math.floor(controlPoints3[x].getY());
						double x1 = Math.floor(controlPoints3[x].getX());
						controlPoints3[x].setLocation(x1, y);
					}

					for (int x = 0; x < controlPoints.length - 1; x++) {
						if (x % 2 == 0 && x < controlPoints3.length) {
							if ((controlPoints[x].getX() > controlPoints3[x + 1].getX())
									&& (controlPoints[x + 1].getX() < controlPoints3[x + 1].getX())
									&& ((int) controlPoints3[x].getY() == (int) controlPoints[x].getY())) {
								boolean contiene = false;
								if (intersectionEdgePoint.contains(controlPoints3[x + 1])) {
									contiene = true;
								}
								if (intersectionEdgePoint == null || !contiene) {
									intersectionEdgePoint.add(controlPoints3[x + 1]);
									intersecato = true;
								}
							} else if ((controlPoints[x + 1].getX() > controlPoints3[x + 1].getX())
									&& (controlPoints[x].getX() < controlPoints3[x + 1].getX())
									&& (controlPoints[x].getY() == controlPoints[x + 1].getY())) {
								boolean contiene = false;
								if (intersectionEdgePoint.contains(controlPoints3[x + 1])) {
									contiene = true;
								}
								if (intersectionEdgePoint == null || !contiene) {
									intersectionEdgePoint.add(controlPoints3[x + 1]);
									intersecato = true;
								}
							}
						}
					}
				}
			}
		}

		Point2D[] controlPoints2 = new Point2D[controlPoint2];
		for (int i = 0; i < controlPoint2; i++) {
			controlPoints2[i] = otherEdgeView.getPoint(i);
			if ((controlPoints2[i] == null)) {
				return false;
			}
			double y = Math.floor(controlPoints2[i].getY());
			double x = Math.floor(controlPoints2[i].getX());
			controlPoints2[i].setLocation(x, y);
		}

		boolean xPosition = true;
		for (int i = 0; i < controlPoint - 1; i++) {
			if (xPosition) {
				if (i == 0 && sharedTheSameSourceTarget) {
					Point2D tmpMin = controlPoints[i];
					Point2D tmpMax = controlPoints[i + 1];
					if ((tmpMin.getX() > tmpMax.getX())) {
						tmpMin = controlPoints[i + 1];
						tmpMax = controlPoints[i];
					}
					for (int q = 0; q < controlPoints2.length - 1; q++) {
						if (!(q % 2 == 0)) {
							Point2D tmpMin2 = controlPoints2[q];
							Point2D tmpMax2 = controlPoints2[q + 1];
							if ((tmpMin2.getY() > tmpMax2.getY())) {
								tmpMin2 = controlPoints2[q + 1];
								tmpMax2 = controlPoints2[q];
							}
							if ((tmpMin2.getY() < tmpMin.getY()) && (tmpMax2.getY() > tmpMin.getY()) && (tmpMin2.getX() > tmpMin.getX())
									&& (tmpMin2.getX() < tmpMax.getX())) {
								Point2D tm1 = new Point2D.Double(tmpMin2.getX(), tmpMin.getY());
								if (!intersectionEdgePoint.contains(tm1)) {
									intersectionEdgePoint.add(tm1);
									intersecato = true;
								}
							}
						}
					}
				} else if (!sharedTheSameSourceTarget || sourcesame) {
					Point2D tmpMin = controlPoints[i];
					Point2D tmpMax = controlPoints[i + 1];
					if ((tmpMin.getX() > tmpMax.getX())) {
						tmpMin = controlPoints[i + 1];
						tmpMax = controlPoints[i];
					}
					for (int q = 0; q < controlPoints2.length - 1; q++) {
						if (!(q % 2 == 0)) {
							Point2D tmpMin2 = controlPoints2[q];
							Point2D tmpMax2 = controlPoints2[q + 1];
							if ((tmpMin2.getY() > tmpMax2.getY())) {
								tmpMin2 = controlPoints2[q + 1];
								tmpMax2 = controlPoints2[q];
							}
							if ((tmpMin2.getY() < tmpMin.getY()) && (tmpMax2.getY() > tmpMin.getY()) && (tmpMin2.getX() > tmpMin.getX())
									&& (tmpMin2.getX() < tmpMax.getX())) {
								Point2D tm1 = new Point2D.Double(tmpMin2.getX(), tmpMin.getY());
								if (!intersectionEdgePoint.contains(tm1)) {
									intersectionEdgePoint.add(tm1);
									intersecato = true;
								}
							}
						}
					}
				}
			}
			xPosition = !xPosition;
		}

		return intersecato;
	}


	// Giuseppe De Cicco & Fabio Granara
	public boolean getLeftSideIntersaction() {
		return leftSide;
	}

	// Giuseppe De Cicco & Fabio Granara
	public boolean getRightSideIntersaction() {
		return rightSide;
	}

	// Giuseppe De Cicco & Fabio Granara
	public boolean getLowerSideIntersaction() {
		return lowerSide;
	}

	// Giuseppe De Cicco & Fabio Granara
	public boolean getUpperSideIntersaction() {
		return upperSide;
	}

	// Giuseppe De Cicco & Fabio Granara
	public Point2D getIntersactionPoint() {
		return intersectionPoint;
	}

	// Giuseppe De Cicco & Fabio Granara
	public ArrayList<Point2D> getIntersactionEdgePoint() {
		return intersectionEdgePoint;
	}

	// Giuseppe De Cicco & Fabio Granara
	public boolean isRing() {
		return isRing;
	}

	// Giuseppe De Cicco & Fabio Granara
	public void setIsRing(boolean value) {
		isRing = value;
	}

	// Giuseppe De Cicco & Fabio Granara
	public boolean isPointSharedModified() {
		return pointSharedModified;
	}

	// Giuseppe De Cicco & Fabio Granara
	public void setPointShareModified(boolean value) {
		pointSharedModified = value;
	}

	// Giuseppe De Cicco & Fabio Granara
	public int getOffset() {
		JmtCell sourceOfEdge = (JmtCell) ((DefaultPort) this.getSource()).getParent();
		Rectangle boundsSource = GraphConstants.getBounds(sourceOfEdge.getAttributes()).getBounds();
		JmtCell targetOfEdge = (JmtCell) ((DefaultPort) this.getTarget()).getParent();
		Rectangle boundsTarget = GraphConstants.getBounds(targetOfEdge.getAttributes()).getBounds();
		GraphModel graphmodel = mediator.getGraph().getModel();
		Object[] fathers = (DefaultGraphModel.getIncomingEdges(graphmodel, targetOfEdge));
		int max = (int) boundsSource.getMaxX();
		for (Object father : fathers) {
			if (father instanceof JmtEdge) {
				JmtCell sourceOfEdge2 = (JmtCell) ((DefaultPort) ((JmtEdge) father).getSource()).getParent();
				Rectangle boundsSource2 = GraphConstants.getBounds(sourceOfEdge2.getAttributes()).getBounds();
				if (sourceOfEdge != sourceOfEdge2 && boundsSource.getMaxX() < boundsTarget.getMinX() - 5
						&& boundsSource2.getMaxX() < boundsTarget.getMinX() - 5) {
					if (max < boundsSource2.getMaxX() && (int) boundsSource.getMaxX() > (int) boundsSource2.getMinX()) {
						max = (int) boundsSource2.getMaxX();
					}
				}
			}
		}
		return (int) (max - boundsSource.getMaxX());
	}

}
