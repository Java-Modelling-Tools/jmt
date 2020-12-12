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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Vector;

import jmt.gui.jsimgraph.controller.Mediator;

import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;

/**
 * <p>Title: JmtOverlapping </p>
 * @author Giuseppe De Cicco & Fabio Granara
 * 		Date: 15-nov-2006
 * 
 */
public class JmtOverlapping {

	private Mediator mediator;

	public JmtOverlapping(Mediator mediator) {
		this.mediator = mediator;
	}

	public void avoidOverlappingCell(Object[] cells2) {
		Object[] listEdges = null;
		ArrayList<Object> listEdges2 = new ArrayList<Object>();
		for (int i = 0; i < cells2.length; i++) {
			if (!(cells2[i] instanceof JmtEdge)) {
				if (cells2[i] instanceof BlockingRegion) {
					Object[] tmp = new Object[1];
					tmp[0] = cells2[i];
					Object[] children = mediator.getGraph().getDescendants(tmp);
					boolean passato = false;
					for (Object element : children) {
						if (element instanceof JmtCell && !passato) {
							passato = true;
							cells2[i] = element;
						}
					}
				}
				Object[] listEdgesIn = null;
				Object[] listEdgesOut = null;
				GraphModel graphmodel = mediator.getGraph().getModel();
				listEdgesIn = DefaultGraphModel.getEdges(graphmodel, cells2[i], true);
				listEdgesOut = DefaultGraphModel.getEdges(graphmodel, cells2[i], false);

				for (int j = 0; j < listEdgesIn.length; j++) {
					if (!listEdges2.contains(listEdgesIn[j])) {
						listEdges2.add(listEdgesIn[j]);
					}
				}
				for (int j = 0; j < listEdgesOut.length; j++) {
					if (!listEdges2.contains(listEdgesOut[j])) {
						listEdges2.add(listEdgesOut[j]);
					}
				}
			}
		}

		listEdges = listEdges2.toArray();
		for (Object listEdge : listEdges) {
			Object[] celle = null;
			EdgeView edgeView = (EdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(listEdge, false);
			Rectangle2D rett = edgeView.getBounds();
			celle = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(rett.getBounds()));
			ArrayList<Object> celle2 = new ArrayList<Object>();
			JmtCell sourceOfEdge = (JmtCell) ((DefaultPort) ((JmtEdge) listEdge).getSource()).getParent();
			JmtCell targetOfEdge = (JmtCell) ((DefaultPort) ((JmtEdge) listEdge).getTarget()).getParent();
			for (int j = 0; j < celle.length; j++) {
				if (celle[j] instanceof JmtCell && !(celle[j] == sourceOfEdge) && !(celle[j] == targetOfEdge)) {
					celle2.add(celle[j]);
				}
			}

			Object[] cellsfinal = celle2.toArray();
			for (Object element : cellsfinal) {
				Rectangle2D cellBound = GraphConstants.getBounds(((JmtCell) element).getAttributes());
				EdgeView viewtmp = (EdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(listEdge, false);
				if (((JmtEdge) listEdge).intersects(viewtmp, cellBound)) {
//					System.out.println("JmtOverlapping -> fct avoidOverlappingCell -> IS BEZIER :"+(((JmtEdge) listEdge).getIsBezier()));
					if  (!(((JmtEdge) listEdge).getIsBezier())) {
//						System.out.println("JmtOverlapping -> fct avoidOverlappingCell -> if intersects");
						double vertexMinX = (int) cellBound.getMinX();
						double vertexMinY = (int) cellBound.getMinY();
						double vertexMaxX = (int) cellBound.getMaxX();
						double vertexMaxY = (int) cellBound.getMaxY();
						double vertexHeight = (int) cellBound.getHeight();
						double vertexWidth = (int) cellBound.getWidth();
						Rectangle cellBounds = cellBound.getBounds();
						ArrayList<Point2D> intersectionPoints = ((JmtEdge) listEdge).getIntersectionVertexPoint();
						boolean upperSideIntersaction = ((JmtEdge) listEdge).getUpperSideIntersaction();
						boolean lowerSideIntersaction = ((JmtEdge) listEdge).getLowerSideIntersaction();
						boolean leftSideIntersaction = ((JmtEdge) listEdge).getLeftSideIntersaction();
						boolean rightSideIntersaction = ((JmtEdge) listEdge).getRightSideIntersaction();

						if (upperSideIntersaction && lowerSideIntersaction) {
							Point2D tmp = (intersectionPoints.get(0));
							int valoreIntermedio = ((int) vertexMaxX - (int) (vertexWidth / 2));
							if ((int) tmp.getX() < valoreIntermedio) {
								Point newPosition = findFreePosition((JmtEdge) listEdge, (JmtCell) element, cellBound, tmp, false, false, true, false);
								cellBounds.setLocation(newPosition);
								GraphConstants.setBounds(((JmtCell) element).getAttributes(), cellBounds);
							} else {
								Point newPosition = findFreePosition((JmtEdge) listEdge, (JmtCell) element, cellBound, tmp, false, false, false, true);
								cellBounds.setLocation(newPosition);
								GraphConstants.setBounds(((JmtCell) element).getAttributes(), cellBounds);
							}
						} else if (leftSideIntersaction && rightSideIntersaction) {
							Point2D tmp = (intersectionPoints.get(0));
							int valoreIntermedio = ((int) vertexMaxY - (int) (vertexHeight / 2));
							if ((int) tmp.getY() < valoreIntermedio) {
								Point newPosition = findFreePosition((JmtEdge) listEdge, (JmtCell) element, cellBound, tmp, false, true, false, false);
								cellBounds.setLocation(newPosition);
								GraphConstants.setBounds(((JmtCell) element).getAttributes(), cellBounds);
							} else {
								Point newPosition = findFreePosition((JmtEdge) listEdge, (JmtCell) element, cellBound, tmp, true, false, false, false);
								cellBounds.setLocation(newPosition);
								GraphConstants.setBounds(((JmtCell) element).getAttributes(), cellBounds);
							}
						} else if (upperSideIntersaction && rightSideIntersaction) {
							Point2D tmp = (intersectionPoints.get(0));
							Point newPosition = findFreePosition((JmtEdge) listEdge, (JmtCell) element, cellBound, tmp, false, false, false, true);
							cellBounds.setLocation(newPosition);
							GraphConstants.setBounds(((JmtCell) element).getAttributes(), cellBounds);
						} else if (upperSideIntersaction && leftSideIntersaction) {
							Point2D tmp = (intersectionPoints.get(0));
							Point newPosition = findFreePosition((JmtEdge) listEdge, (JmtCell) element, cellBound, tmp, false, false, true, false);
							cellBounds.setLocation(newPosition);
							GraphConstants.setBounds(((JmtCell) element).getAttributes(), cellBounds);
						} else if (lowerSideIntersaction && rightSideIntersaction) {
							Point2D tmp = (intersectionPoints.get(1));
							Point newPosition = findFreePosition((JmtEdge) listEdge, (JmtCell) element, cellBound, tmp, false, false, false, true);
							cellBounds.setLocation(newPosition);
							GraphConstants.setBounds(((JmtCell) element).getAttributes(), cellBounds);
						} else if (lowerSideIntersaction && leftSideIntersaction) {
							Point2D tmp = (intersectionPoints.get(0));
							Point newPosition = findFreePosition((JmtEdge) listEdge, (JmtCell) element, cellBound, tmp, false, false, true, false);
							cellBounds.setLocation(newPosition);
							GraphConstants.setBounds(((JmtCell) element).getAttributes(), cellBounds);
						}
					}
				}
			}
		}
	}

	public Point findFreePosition(JmtEdge edge, JmtCell vertex, Rectangle2D cellBound, Point2D tmp, boolean up, boolean down, boolean right, boolean left) {
		double vertexMinX = (int) cellBound.getMinX();
		double vertexMinY = (int) cellBound.getMinY();
		double vertexMaxX = (int) cellBound.getMaxX();
		double vertexMaxY = (int) cellBound.getMaxY();
		double vertexHeight = (int) cellBound.getHeight();
		double vertexWidth = (int) cellBound.getWidth();
		Rectangle cellBounds = cellBound.getBounds();
		Point pointBack = null;

		Object[] latiIn = (DefaultGraphModel.getIncomingEdges(mediator.getGraph().getModel(), vertex));
		Object[] latiOut = (DefaultGraphModel.getOutgoingEdges(mediator.getGraph().getModel(), vertex));
		Vector<Object> handledEdges = new Vector<Object>();
		if (latiIn.length > 0) {
			for (Object element : latiIn) {
				handledEdges.add(element);
			}
		}
		if (latiOut.length > 0) {
			for (Object element : latiOut) {
				handledEdges.add(element);
			}
		}

		Rectangle newRettDown = new Rectangle((int) vertexMinX, ((int) (tmp.getY() + 5)), (int) vertexWidth, (int) vertexHeight);
		Rectangle newRettRight = new Rectangle((int) (tmp.getX() + 5), (int) vertexMinY, (int) vertexWidth, (int) vertexHeight);
		int daScalare2 = (int) vertexMaxY - (int) tmp.getY();
		int pointYToMove2 = (int) vertexMinY - daScalare2;
		if (pointYToMove2 < 0) {
			pointYToMove2 = (int) (tmp.getY() + 4);
		}
		Rectangle newRettUp = new Rectangle((int) vertexMinX, pointYToMove2 - 10, (int) vertexWidth, (int) vertexHeight);

		int pointXToMove3 = (int) (vertexMaxX - tmp.getX());
		int point2XToMove3 = (int) (vertexMinX - pointXToMove3) - 12;
		if (point2XToMove3 < 0) {
			pointXToMove3 = (int) (tmp.getX() + 12);
		}
		Rectangle newRettLeft = new Rectangle(point2XToMove3, (int) vertexMinY, (int) vertexWidth, (int) vertexHeight);

		//Begin if to move the vertex down
		if (down && !up && !right && !left) {
            //System.out.println("JmtOverlapping findFreePosition Begin if to move the vertex down");

			int pointYToMove = (int) (tmp.getY() + 3);
			Rectangle newRett = new Rectangle((int) vertexMinX, pointYToMove, (int) vertexWidth, (int) vertexHeight);
            //System.out.println("JmtOverlapping findFreePosition center newRett y:"+newRett.getMinY());
			Object[] celletmp = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(newRett));
			Vector<Object> celle = new Vector<Object>();
            //System.out.println("JmtOverlapping findFreePosition celletmp.length"+celletmp.length);
			for (int x = 0; x < celletmp.length; x++) {
				if (!(celletmp[x] == vertex) && !((vertex.getChildren()).contains(celletmp[x])) && !(celletmp[x] == edge)) {
                   // System.out.println("JmtOverlapping findFreePosition ->>> !(celletmp[x] == vertex) && !((vertex.getChildren()).contains(celletmp[x])) && !(celletmp[x] == edge)");
					if (celletmp[x] instanceof JmtEdge) {
						JmtCell source = (JmtCell) ((DefaultPort) ((JmtEdge) celletmp[x]).getSource()).getParent();
						JmtCell target = (JmtCell) ((DefaultPort) ((JmtEdge) celletmp[x]).getTarget()).getParent();
						JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp[x])), false);
						if (((handledEdges.size() == 0) || (handledEdges.size() > 0 && !handledEdges.contains(celletmp[x])))
								&& ((JmtEdge) celletmp[x]).intersects(viewtmp, newRett)) {
							celle.add(celletmp[x]);
						}
					} else {
						celle.add(celletmp[x]);
					}
				}
			}

			Object[] celletmp3 = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(newRettUp));
			Vector<Object> celle2 = new Vector<Object>();
          //  System.out.println("JmtOverlapping findFreePosition celletmp3.length"+celletmp3.length);
			for (int x = 0; x < celletmp3.length; x++) {
				if (!(celletmp3[x] == vertex) && !((vertex.getChildren()).contains(celletmp3[x])) && !(celletmp3[x] == edge)) {
                  //  System.out.println("JmtOverlapping findFreePosition ->>> !(celletmp3[x] == vertex) && !((vertex.getChildren()).contains(celletmp3[x])) && !(celletmp3[x] == edge)");
					if (celletmp3[x] instanceof JmtEdge) {
                      //  System.out.println("JmtOverlapping findFreePosition instanceof JmtEdge isbezier"+ ((JmtEdge) celletmp3[x]).getIsBezier());
                        if (!((JmtEdge) celletmp3[x]).getIsBezier()){
                            JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp3[x])), false);
                            if (((handledEdges.size() == 0) || (handledEdges.size() > 0 && !handledEdges.contains(celletmp3[x])))
                                    && ((JmtEdge) celletmp3[x]).intersects(viewtmp, newRettUp)) {
                                celle2.add(celletmp3[x]);
                            }
                        }
					} else {
						celle2.add(celletmp3[x]);
					}
				}
			}

			if (celle.size() > 0 && celle2.size() == 0) {
               // System.out.println("JmtOverlapping findFreePosition (celle.size() > 0 && celle2.size() == 0)");
				if (((int) newRettUp.getMinX() < 20) || ((int) newRettUp.getMinY() < 0)) {
                    System.out.println("JmtOverlapping findFreePosition (int) cellBound.getMinY() : "+(int) cellBound.getMinY());
					return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
				}
               // System.out.println("JmtOverlapping findFreePosition newRettUp.getMinY() : "+(int) newRettUp.getMinY());
				return new Point((int) newRettUp.getMinX(), (int) newRettUp.getMinY());



			} else if (celle.size() == 0) {
                //System.out.println("JmtOverlapping findFreePosition (celle.size() == 0)");
                //System.out.println("JmtOverlapping findFreePosition ("+celle.size()+" == 0 && celle2: "+celle2.size());
				if (((int) newRettDown.getMinX() < 20) || ((int) newRettDown.getMinY() < 0)) {
                    //System.out.println("JmtOverlapping findFreePosition (int) cellBound.getMinY() : "+(int) cellBound.getMinY());
					return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
				}
               // System.out.println("JmtOverlapping findFreePosition newRettDown.getMinY() : "+(int) newRettDown.getMinY());
				return new Point((int) newRettDown.getMinX(), (int) newRettDown.getMinY());


			} else if (celle.size() > 0 && celle2.size() > 0) {
                //System.out.println("JmtOverlapping findFreePosition (celle.size() > 0 && celle2.size() > 0)");
                //System.out.println("JmtOverlapping findFreePosition ("+celle.size()+" > 0 && "+celle2.size()+" > 0)");
               // System.out.println("JmtOverlapping findFreePosition (celle.size() > 0 && celle2.size() > 0)");
                return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
			}

            //System.out.println("JmtOverlapping findFreePosition celle.size() : "+celle.size());
			while (celle.size() > 0) {
				pointYToMove = pointYToMove + 59;
				Rectangle new2rett = new Rectangle((int) vertexMinX, pointYToMove, (int) vertexWidth, (int) vertexHeight);
				Object[] celletmp2 = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(new2rett));
				celle = new Vector<Object>();
				for (int x = 0; x < celletmp2.length; x++) {
					if (!(celletmp2[x] == vertex) && !((vertex.getChildren()).contains(celletmp[x])) && !(celletmp2[x] == edge)) {
						if (celletmp2[x] instanceof JmtEdge) {
							JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp2[x])), false);
							if (((JmtEdge) celletmp2[x]).intersects(viewtmp, new2rett)) {
								celle.add(celletmp2[x]);
							}
						} else {
							celle.add(celletmp2[x]);
						}
					}
				}
			}
            //System.out.println("JmtOverlapping findFreePosition pointYToMove : "+ pointYToMove);
			pointBack = new Point((int) vertexMinX, pointYToMove);
            //System.out.println("JmtOverlapping findFreePosition PointBack y : "+pointBack.getY());

		} //End if to move the vertex down

		//Begin if to move the vertex up
		else if (!down && up && !right && !left) {
			int daScalare = (int) vertexMaxY - (int) tmp.getY();
			int pointYToMove = (int) vertexMinY - daScalare;
			Rectangle newRett = new Rectangle((int) vertexMinX, pointYToMove, (int) vertexWidth, (int) vertexHeight);
			Object[] celletmp = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(newRett));
			Vector<Object> celle = new Vector<Object>();
			for (int x = 0; x < celletmp.length; x++) {
				if (!(celletmp[x] == vertex) && !((vertex.getChildren()).contains(celletmp[x])) && !(celletmp[x] == edge)) {
					Object[] listEdgesIn = null;
					Object[] listEdgesOut = null;
					GraphModel graphmodel = mediator.getGraph().getModel();
					listEdgesIn = DefaultGraphModel.getEdges(graphmodel, vertex, true);
					listEdgesOut = DefaultGraphModel.getEdges(graphmodel, vertex, false);
					boolean edgeIn = false;
					boolean edgeOut = false;
					for (Object element : listEdgesIn) {
						if (celletmp[x] instanceof JmtEdge && element.equals(celletmp[x])) {
							edgeIn = true;
						}
					}
					for (Object element : listEdgesOut) {
						if (celletmp[x] instanceof JmtEdge && element.equals(celletmp[x])) {
							edgeIn = true;
						}
					}
					if ((listEdgesIn.length == 0 && listEdgesOut.length == 0) || (!edgeIn && !edgeOut)) {
						if (celletmp[x] instanceof JmtEdge) {
							JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp[x])), false);
							if (((JmtEdge) celletmp[x]).intersects(viewtmp, newRett)) {
								celle.add(celletmp[x]);
							}
						} else {
							celle.add(celletmp[x]);
						}
					}
				}
			}

			Object[] celletmp3 = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(newRettDown));
			Vector<Object> celle2 = new Vector<Object>();
			for (int x = 0; x < celletmp3.length; x++) {
				if (!(celletmp3[x] == vertex) && !((vertex.getChildren()).contains(celletmp3[x])) && !(celletmp3[x] == edge)) {
					if (celletmp3[x] instanceof JmtEdge) {
                        if (!((JmtEdge) celletmp3[x]).getIsBezier()) {
                            JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp3[x])), false);
                            if (((handledEdges.size() == 0) || (handledEdges.size() > 0 && !handledEdges.contains(celletmp3[x])))
                                    && ((JmtEdge) celletmp3[x]).intersects(viewtmp, newRettDown)) {
                                celle2.add(celletmp3[x]);
                            }
                        }
					} else {
						celle2.add(celletmp3[x]);
					}
				}
			}

			if (celle.size() > 0 && celle2.size() == 0) {
				if (((int) newRettDown.getMinX() < 20) || ((int) newRettDown.getMinY() < 0)) {
					return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
				}
				return new Point((int) newRettDown.getMinX(), (int) newRettDown.getMinY());


			} else if (celle.size() == 0) {
				if (((int) newRettUp.getMinX() < 20) || ((int) newRettUp.getMinY() < 0)) {
					return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
				}
				return new Point((int) newRettUp.getMinX(), (int) newRettUp.getMinY());


			} else if (celle.size() > 0 && celle2.size() > 0) {
                return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
			}
			pointBack = new Point((int) vertexMinX, pointYToMove);

		} //End if to move the vertex up

		//Begin if to move the vertex right
		else if (!down && !up && !left && right) {
			int pointXToMove = (int) (tmp.getX() + 12);
			Rectangle newRett = new Rectangle(pointXToMove, (int) vertexMinY, (int) vertexWidth, (int) vertexHeight);
			Object[] celletmp = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(newRett));
			Vector<Object> celle = new Vector<Object>();

			for (int x = 0; x < celletmp.length; x++) {
				if (!(celletmp[x] == vertex) && !((vertex.getChildren()).contains(celletmp[x])) && !(celletmp[x] == edge)) {
					Object[] listEdgesIn = null;
					Object[] listEdgesOut = null;
					GraphModel graphmodel = mediator.getGraph().getModel();
					listEdgesIn = DefaultGraphModel.getEdges(graphmodel, vertex, true);
					listEdgesOut = DefaultGraphModel.getEdges(graphmodel, vertex, false);
					boolean edgeIn = false;
					boolean edgeOut = false;

					for (Object element : listEdgesIn) {
						if (celletmp[x] instanceof JmtEdge && element.equals(celletmp[x])) {
							edgeIn = true;
						}
					}
					for (Object element : listEdgesOut) {
						if (celletmp[x] instanceof JmtEdge && element.equals(celletmp[x])) {
							edgeIn = true;
						}
					}

					if ((listEdgesIn.length == 0 && listEdgesOut.length == 0) || (!edgeIn && !edgeOut)) {
						if (!(celletmp[x] == vertex) && !((vertex.getChildren()).contains(celletmp[x])) && !(celletmp[x] == edge)) {
							if (celletmp[x] instanceof JmtEdge) {
								if (!(((JmtEdge) celletmp[x]).getIsBezier())) {
									JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp[x])), false);
									if (((handledEdges.size() == 0) || (handledEdges.size() > 0 && !handledEdges.contains(celletmp[x])))
											&& ((JmtEdge) celletmp[x]).intersects(viewtmp, newRett)) {
										celle.add(celletmp[x]);
									}
								}
							} else {
								celle.add(celletmp[x]);
							}
						}
					}
				}
			}

			Object[] celletmp3 = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(newRettLeft));
			Vector<Object> celle3 = new Vector<Object>();
			for (int x = 0; x < celletmp3.length; x++) {
				if (!(celletmp3[x] == vertex) && !((vertex.getChildren()).contains(celletmp3[x])) && !(celletmp3[x] == edge)) {
					if (celletmp3[x] instanceof JmtEdge) {
						JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp3[x])), false);
						if (((handledEdges.size() == 0) || (handledEdges.size() > 0 && !handledEdges.contains(celletmp3[x])))
								&& ((JmtEdge) celletmp3[x]).intersects(viewtmp, newRettLeft)) {
							celle3.add(celletmp3[x]);
						}
					} else {
						celle3.add(celletmp3[x]);
					}
				}
			}

			if (celle.size() > 0 && celle3.size() == 0) {
				if (((int) cellBound.getMinX() < 20) || ((int) cellBound.getMinY() < 0)) {
					return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
				}
				return new Point((int) newRettLeft.getMinX(), (int) newRettLeft.getMinY());


			} else if (celle.size() == 0) {
				if (((int) cellBound.getMinX() < 20) || ((int) cellBound.getMinY() < 0)) {
					return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
				}
				return new Point((int) newRett.getMinX(), (int) newRett.getMinY());


			} else if (celle.size() > 0 && celle3.size() > 0) {
                return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
			}
			pointBack = new Point(pointXToMove, (int) vertexMinY);
		} //End if to move the vertex right

		//Begin if to move the vertex left
		else if (!down && !up && left && !right) {
			int pointXToMove = (int) (vertexMaxX - tmp.getX());
			int point2XToMove = (int) (vertexMinX - pointXToMove) - 12;
			if (point2XToMove < 0) {
				point2XToMove = (int) (tmp.getX() + 12);
			}
			Rectangle newRett = new Rectangle(point2XToMove, (int) vertexMinY, (int) vertexWidth, (int) vertexHeight);
			Object[] celletmp = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(newRett));
			Vector<Object> celle = new Vector<Object>();

			for (int x = 0; x < celletmp.length; x++) {
				if (!(celletmp[x] == vertex) && !((vertex.getChildren()).contains(celletmp[x])) && !(celletmp[x] == edge)) {
					Object[] listEdgesIn = null;
					Object[] listEdgesOut = null;
					GraphModel graphmodel = mediator.getGraph().getModel();
					listEdgesIn = DefaultGraphModel.getEdges(graphmodel, vertex, true);
					listEdgesOut = DefaultGraphModel.getEdges(graphmodel, vertex, false);
					boolean edgeIn = false;
					boolean edgeOut = false;

					for (Object element : listEdgesIn) {
						if (celletmp[x] instanceof JmtEdge && element.equals(celletmp[x])) {
							edgeIn = true;
						}
					}
					for (Object element : listEdgesOut) {
						if (celletmp[x] instanceof JmtEdge && element.equals(celletmp[x])) {
							edgeIn = true;
						}
					}

					if ((listEdgesIn.length == 0 && listEdgesOut.length == 0) || (!edgeIn && !edgeOut)) {
						if (!(celletmp[x] == vertex) && !((vertex.getChildren()).contains(celletmp[x])) && !(celletmp[x] == edge)) {
							if (celletmp[x] instanceof JmtEdge) {
                                if ( !(((JmtEdge) celletmp[x]).getIsBezier()) ) {
									JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp[x])), false);
									if (((handledEdges.size() == 0) || (handledEdges.size() > 0 && !handledEdges.contains(celletmp[x])))
											&& ((JmtEdge) celletmp[x]).intersects(viewtmp, newRett)) {
										celle.add(celletmp[x]);
									}
								}
							} else {
								celle.add(celletmp[x]);
							}
						}
					}
				}
			}

			Object[] celletmp4 = (mediator.getGraph()).getDescendants(mediator.getGraph().getRoots(newRettRight));
			Vector<Object> celle4 = new Vector<Object>();
			for (int x = 0; x < celletmp4.length; x++) {
				if (!(celletmp4[x] == vertex) && !((vertex.getChildren()).contains(celletmp4[x])) && !(celletmp4[x] == edge)) {
					if (celletmp4[x] instanceof JmtEdge) {
						JmtEdgeView viewtmp = (JmtEdgeView) (mediator.getGraph().getGraphLayoutCache()).getMapping(((celletmp4[x])), false);
						if (((handledEdges.size() == 0) || (handledEdges.size() > 0 && !handledEdges.contains(celletmp4[x])))
								&& ((JmtEdge) celletmp4[x]).intersects(viewtmp, newRettRight)) {
							celle4.add(celletmp4[x]);
						}
					} else {
						celle4.add(celletmp4[x]);
					}
				}
			}

			if (celle.size() > 0 && celle4.size() == 0) {
				if (((int) newRettRight.getMinX() < 20) || ((int) newRettRight.getMinY() < 0)) {
					return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
				}
				return new Point((int) newRettRight.getMinX(), (int) newRettRight.getMinY());
			} else if (celle.size() == 0) {
				if (((int) newRett.getMinX() < 20) || ((int) newRett.getMinY() < 0)) {
					return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
				}
				return new Point((int) newRett.getMinX(), (int) newRett.getMinY());
			} else if (celle.size() > 0 && celle4.size() > 0) {
                return new Point((int) cellBound.getMinX(), (int) cellBound.getMinY());
			}

			pointBack = new Point(point2XToMove, (int) vertexMinY);
		} //End if to move the vertex left

		return pointBack;

	}

}
