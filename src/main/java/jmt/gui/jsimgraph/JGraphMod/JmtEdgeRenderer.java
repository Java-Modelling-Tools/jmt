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
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import jmt.gui.jsimgraph.UtilPoint;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.definitions.JMTArc;
import jmt.gui.jsimgraph.definitions.JMTPath;
import org.apache.xpath.operations.Bool;
import org.jgraph.JGraph;
import org.jgraph.graph.*;

import javax.swing.*;

/**
 * <p>Title: JmtEdgeRenderer </p>
 * @author Giuseppe De Cicco & Fabio Granara
 * 		Date: 15-ott-2006
 * 
 */
public class JmtEdgeRenderer extends EdgeRenderer implements CellViewRenderer {

	private static final long serialVersionUID = 1L;
	private Mediator mediator;

	public JmtEdgeRenderer(Mediator factory) {
		this.mediator = factory;
	}

	@Override
	protected Shape createShape() {
		GeneralPath ret = new GeneralPath();
		try {
		ArrayList tmp1 = new ArrayList();
		boolean passed = false;
		int n = view.getPointCount();
		if (n > 1) {
			JmtEdgeView tmp = (JmtEdgeView) view;
			JmtEdge cell = (JmtEdge) tmp.getCell();
			if (cell != null) {
				Point2D[] p = new Point2D[n];
				for (int i = 0; i < n; i++) {
					Point2D pt = tmp.getPoint(i);
					if (pt == null) {
						return null; // exit
					}
					int x = (int) pt.getX();
					int y = (int) pt.getY();
					p[i] = new Point2D.Double(x, y);
				}

				if (view.sharedPath == null) {
					view.sharedPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);
				} else {
					view.sharedPath.reset();
				}
				view.beginShape = view.lineShape = view.endShape = null;

				if (endDeco != GraphConstants.ARROW_NONE) {
					if ((DefaultPort) cell.getTarget() != null) {
						JmtCell targetOfEdge1 = (JmtCell) ((DefaultPort) cell.getTarget()).getParent();
						if (!targetOfEdge1.isLeftInputCell()) {
							view.endShape = createLineEnd(endSize, endDeco, new Point2D.Double(p[n - 1].getX() + 10, p[n - 1].getY()), p[n - 1]);
						} else {
							view.endShape = createLineEnd(endSize, endDeco, new Point2D.Double(p[n - 1].getX() - 10, p[n - 1].getY()), p[n - 1]);
						}
					}
				}
				view.sharedPath.moveTo((float) p[0].getX(), (float) p[0].getY());

				cell.intersectionEdgePoint = null;
				if (cell.intersectsEdge()) {
					for (int i = 0; i < cell.intersectionEdgePoint.size(); i++) {
						for (int j = i; j < cell.intersectionEdgePoint.size(); j++) {
							if (i < j) {
								if ((int) cell.intersectionEdgePoint.get(j).getX() < (int) cell.intersectionEdgePoint.get(i).getX()) {
									Point2D tmp2 = cell.intersectionEdgePoint.get(j);
									cell.intersectionEdgePoint.set(j, cell.intersectionEdgePoint.get(i));
									cell.intersectionEdgePoint.set(i, tmp2);
								}
							}
						}
					}
				}

				for (int i = 0; i < n - 1; i++) {
					if (i % 2 == 0 && i != 0) {
						view.sharedPath.lineTo((float) p[i].getX(), (float) p[i].getY());
						if (cell.intersectionEdgePoint.size() > 0) {
							for (int j = 0; j < cell.intersectionEdgePoint.size(); j++) {
								if (cell.isRing && i == 4) {
									if ((int) p[i].getY() == (int) cell.intersectionEdgePoint.get(j).getY()
											&& (int) p[i].getX() < (int) cell.intersectionEdgePoint.get(j).getX()) {
										int intersectionPoint = (int) cell.intersectionEdgePoint.get(j).getX();
										if (intersectionPoint < (int) p[5].getX() && p[5].getX() > p[4].getX()) {
											view.sharedPath.curveTo((float) p[4].getX(), (float) p[4].getY(), (float) ((p[4].getX() + p[5]
													.getX()) / 2), (float) (p[4].getY() - 10), (float) p[5].getX(), (float) p[5].getY());
											view.sharedPath.moveTo((float) p[5].getX(), (float) p[5].getY());
										}
									} else if ((int) p[i].getY() == (int) cell.intersectionEdgePoint.get(j).getY()
											&& (int) p[i].getX() > (int) cell.intersectionEdgePoint.get(j).getX()) {
										int intersectionPoint = (int) cell.intersectionEdgePoint.get(j).getX();
										if (p[5].getX() < p[4].getX() && intersectionPoint > (int) p[5].getX()) {
											view.sharedPath.curveTo((float) p[4].getX(), (float) p[4].getY(), (float) ((p[4].getX() + p[5]
													.getX()) / 2), (float) (p[4].getY() - 10), (float) p[5].getX(), (float) p[5].getY());
											view.sharedPath.moveTo((float) p[5].getX(), (float) p[5].getY());
										}
									}
								} else {
									if ((int) p[i + 1].getY() == (int) cell.intersectionEdgePoint.get(j).getY()
											&& (int) p[i].getY() == (int) cell.intersectionEdgePoint.get(j).getY()
											&& (int) p[i].getX() < (int) cell.intersectionEdgePoint.get(j).getX()
											&& (int) p[i + 1].getX() > (int) cell.intersectionEdgePoint.get(j).getX()) {
										if ((j + 1) < cell.intersectionEdgePoint.size()
												&& cell.intersectionEdgePoint.get(j + 1).getX() > cell.intersectionEdgePoint.get(j).getX()
												&& cell.intersectionEdgePoint.get(j + 1).getX() - cell.intersectionEdgePoint.get(j).getX() < 10
												&& cell.intersectionEdgePoint.get(j).getY() == cell.intersectionEdgePoint.get(j + 1).getY()) {
											double middleX = (cell.intersectionEdgePoint.get(j + 1).getX() + cell.intersectionEdgePoint.get(j).getX()) / 2;
											view.sharedPath.lineTo((float) (cell.intersectionEdgePoint.get(j).getX() - 5),
													(float) cell.intersectionEdgePoint.get(j).getY());
											view.sharedPath.moveTo((float) (cell.intersectionEdgePoint.get(j).getX() - 5),
													(float) cell.intersectionEdgePoint.get(j).getY());
											view.sharedPath.curveTo((float) (cell.intersectionEdgePoint.get(j).getX() - 5),
													(float) cell.intersectionEdgePoint.get(j).getY(), (float) middleX,
													(float) (cell.intersectionEdgePoint.get(j).getY() - 10),
													(float) (cell.intersectionEdgePoint.get(j + 1).getX() + 5),
													(float) cell.intersectionEdgePoint.get(j + 1).getY());
											view.sharedPath.moveTo((float) (cell.intersectionEdgePoint.get(j + 1).getX() + 5),
													(float) cell.intersectionEdgePoint.get(j).getY());
											j++;
										} else {
											view.sharedPath.lineTo((float) (cell.intersectionEdgePoint.get(j).getX() - 5),
													(float) cell.intersectionEdgePoint.get(j).getY());
											view.sharedPath.moveTo((float) (cell.intersectionEdgePoint.get(j).getX() - 5),
													(float) cell.intersectionEdgePoint.get(j).getY());
											view.sharedPath.curveTo((float) (cell.intersectionEdgePoint.get(j).getX() - 5),
													(float) cell.intersectionEdgePoint.get(j).getY(),
													(float) cell.intersectionEdgePoint.get(j).getX(),
													(float) (cell.intersectionEdgePoint.get(j).getY() - 10),
													(float) (cell.intersectionEdgePoint.get(j).getX() + 5),
													(float) cell.intersectionEdgePoint.get(j).getY());
											view.sharedPath.moveTo((float) (cell.intersectionEdgePoint.get(j).getX() + 5),
													(float) cell.intersectionEdgePoint.get(j).getY());
										}
									} else if ((!passed || (passed && tmp1.size() > 0 && !tmp1.contains(cell.intersectionEdgePoint.get(j))))
											&& (int) p[i].getY() == (int) cell.intersectionEdgePoint.get(j).getY()
											&& (int) p[i].getX() > (int) p[i + 1].getX()
											&& (int) p[i + 1].getY() == (int) cell.intersectionEdgePoint.get(j).getY()
											&& (int) p[i + 1].getX() < (int) cell.intersectionEdgePoint.get(j).getX()) {
										passed = true;
										for (int q = 0; q < cell.intersectionEdgePoint.size(); q++) {
											if (cell.intersectionEdgePoint.get(q).getY() == cell.intersectionEdgePoint.get(j).getY()) {
												tmp1.add(cell.intersectionEdgePoint.get(q).clone());
											}
										}

										for (int q = 0; q < tmp1.size(); q++) {
											for (int k = q; k < tmp1.size(); k++) {
												int valore2X = (int) ((Point2D) tmp1.get(k)).getX();
												int valore1X = (int) ((Point2D) (tmp1).get(q)).getX();
												if (valore1X < valore2X) {
													Point2D tmp2 = (Point2D) tmp1.get(k);
													tmp1.set(k, tmp1.get(q));
													tmp1.set(q, tmp2);
												}
											}
										}

										for (int x = 0; x < tmp1.size(); x++) {
											if ((x + 1) < tmp1.size() && ((Point2D) tmp1.get(x)).getX() - ((Point2D) tmp1.get(x + 1)).getX() < 10) {
												double middleX = (((Point2D) tmp1.get(x + 1)).getX() + ((Point2D) tmp1.get(x)).getX()) / 2;
												view.sharedPath.lineTo((float) (((Point2D) tmp1.get(x)).getX() + 5),
														(float) ((Point2D) tmp1.get(x)).getY());
												view.sharedPath.moveTo((float) (((Point2D) tmp1.get(x)).getX() + 5),
														(float) ((Point2D) tmp1.get(x)).getY());
												view.sharedPath.curveTo((float) (((Point2D) tmp1.get(x)).getX() + 5),
														(float) ((Point2D) tmp1.get(x)).getY(), (float) middleX,
														(float) (((Point2D) tmp1.get(x)).getY() - 10),
														(float) (((Point2D) tmp1.get(x + 1)).getX() - 5),
														(float) ((Point2D) tmp1.get(x + 1)).getY());
												view.sharedPath.moveTo((float) (((Point2D) tmp1.get(x + 1)).getX() - 5),
														(float) ((Point2D) tmp1.get(x)).getY());
												x++;
											} else {
												if ((int) p[i].getY() == (int) ((Point2D) tmp1.get(x)).getY()) {
													view.sharedPath.lineTo((float) (((Point2D) tmp1.get(x)).getX() + 5),
															(float) ((Point2D) tmp1.get(x)).getY());
													view.sharedPath.moveTo((float) (((Point2D) tmp1.get(x)).getX() + 5),
															(float) ((Point2D) tmp1.get(x)).getY());
													view.sharedPath.curveTo((float) (((Point2D) tmp1.get(x)).getX() + 5),
															(float) ((Point2D) tmp1.get(x)).getY(),
															(float) ((Point2D) tmp1.get(x)).getX(),
															(float) (((Point2D) tmp1.get(x)).getY() - 10),
															(float) (((Point2D) tmp1.get(x)).getX() - 5),
															(float) ((Point2D) tmp1.get(x)).getY());
													view.sharedPath.moveTo(((float) ((Point2D) tmp1.get(x)).getX() - 5),
															(float) ((Point2D) tmp1.get(x)).getY());
												}
											}
										}
									}
								}
							}
						}
					} else {
						boolean done = false;
						if (i == 0) {
							Vector<Point2D> pointOnFirstPoints = new Vector<Point2D>();
							for (int q = 0; q < cell.intersectionEdgePoint.size(); q++) {
								if ((int) p[0].getY() == (int) cell.intersectionEdgePoint.get(q).getY()) {
									pointOnFirstPoints.add(cell.intersectionEdgePoint.get(q));
								}
							}

							for (int j = 0; j < pointOnFirstPoints.size(); j++) {
								if (p[1].getX() - p[0].getX() > 0) {
									if ((int) p[0].getY() == (int) (pointOnFirstPoints.get(j)).getY()) {
										if (p[0].getY() == (pointOnFirstPoints.get(j)).getY()
												&& (int) p[0].getX() < (int) (pointOnFirstPoints.get(j)).getX()
												&& (int) (pointOnFirstPoints.get(j)).getX() < (int) p[1].getX()) {
											if ((j + 1) < pointOnFirstPoints.size()
													&& (pointOnFirstPoints.get(j + 1)).getX() > (pointOnFirstPoints.get(j)).getX()
													&& (pointOnFirstPoints.get(j + 1)).getX() - (pointOnFirstPoints.get(j)).getX() < 10
													&& (pointOnFirstPoints.get(j)).getY() == (pointOnFirstPoints.get(j + 1)).getY()) {
												double middleX = ((pointOnFirstPoints.get(j + 1)).getX() + (pointOnFirstPoints.get(j)).getX()) / 2;
												view.sharedPath.lineTo((float) ((pointOnFirstPoints.get(j)).getX() - 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) ((pointOnFirstPoints.get(j)).getX() - 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												view.sharedPath.curveTo((float) ((pointOnFirstPoints.get(j)).getX() - 5),
														(float) (pointOnFirstPoints.get(j)).getY(), (float) middleX,
														(float) ((pointOnFirstPoints.get(j)).getY() - 10),
														(float) ((pointOnFirstPoints.get(j + 1)).getX() + 5),
														(float) (pointOnFirstPoints.get(j + 1)).getY());
												view.sharedPath.moveTo((float) ((pointOnFirstPoints.get(j + 1)).getX() + 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												j++;
												done = true;
											} else {
												view.sharedPath.lineTo((float) ((pointOnFirstPoints.get(j)).getX() - 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) ((pointOnFirstPoints.get(j)).getX() - 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												view.sharedPath.curveTo((float) ((pointOnFirstPoints.get(j)).getX() - 5),
														(float) (pointOnFirstPoints.get(j)).getY(),
														(float) (pointOnFirstPoints.get(j)).getX(),
														(float) ((pointOnFirstPoints.get(j)).getY() - 10),
														(float) ((pointOnFirstPoints.get(j)).getX() + 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) ((pointOnFirstPoints.get(j)).getX() + 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												done = true;
											}
										}
									}
								} else {
									Vector<Point2D> temp = new Vector<>();
									for (int z = 0; z < pointOnFirstPoints.size(); z++) {
										temp.add(pointOnFirstPoints.get(pointOnFirstPoints.size() - z - 1));
									}
									pointOnFirstPoints = temp;

									boolean largeArc = false;
									if ((int) p[0].getY() == (int) (pointOnFirstPoints.get(j)).getY()
											&& (int) p[0].getX() > (int) (pointOnFirstPoints.get(j)).getX()
											&& (int) (pointOnFirstPoints.get(j)).getX() > (int) p[1].getX()) {
										if ((j + 1) < pointOnFirstPoints.size()
												&& (pointOnFirstPoints.get(j + 1)).getX() < (pointOnFirstPoints.get(j)).getX()
												&& (pointOnFirstPoints.get(j)).getX() - (pointOnFirstPoints.get(j + 1)).getX() < 10) {
											double middleX = ((int) (pointOnFirstPoints.get(j + 1)).getX() + (int) (pointOnFirstPoints.get(j)).getX()) / 2;
											view.sharedPath.lineTo((float) ((pointOnFirstPoints.get(j)).getX() + 5), (float) p[0].getY());
											view.sharedPath.moveTo((float) ((pointOnFirstPoints.get(j)).getX() + 5), (float) p[0].getY());
											view.sharedPath.curveTo((float) ((pointOnFirstPoints.get(j)).getX() + 5),
													(float) p[0].getY(), (float) middleX, (float) (p[0].getY() - 10),
													(float) ((pointOnFirstPoints.get(j + 1)).getX() - 5), (float) p[0].getY());
											view.sharedPath.moveTo((float) ((pointOnFirstPoints.get(j + 1)).getX() - 5), (float) p[0].getY());
											j = j + 1;
											largeArc = true;
											done = true;
										} else {
											if (!largeArc) {
												view.sharedPath.lineTo((float) ((pointOnFirstPoints.get(j)).getX() + 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) ((pointOnFirstPoints.get(j)).getX() + 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												view.sharedPath.curveTo((float) ((pointOnFirstPoints.get(j)).getX() + 5),
														(float) (pointOnFirstPoints.get(j)).getY(),
														(float) (pointOnFirstPoints.get(j)).getX(),
														(float) ((pointOnFirstPoints.get(j)).getY() - 10),
														(float) ((pointOnFirstPoints.get(j)).getX() - 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) ((pointOnFirstPoints.get(j)).getX() - 5),
														(float) (pointOnFirstPoints.get(j)).getY());
												done = true;
											}
										}
									}

									Vector<Point2D> temp2 = new Vector<Point2D>();
									for (int z = 0; z < pointOnFirstPoints.size(); z++) {
										temp2.add(pointOnFirstPoints.get(pointOnFirstPoints.size() - z - 1));
									}
									pointOnFirstPoints = temp2;
								}
							}
							view.sharedPath.lineTo((float) p[1].getX(), (float) p[1].getY());
							view.sharedPath.moveTo((float) p[1].getX(), (float) p[1].getY());
							done = true;
						} else if (i == 1 && done) {
							done = false;
						} else if (i == 1 && !done) {
							view.sharedPath.lineTo((float) p[i].getX(), (float) p[i].getY());
							view.sharedPath.moveTo((float) p[i].getX(), (float) p[i].getY());
						} else {
							view.sharedPath.lineTo((float) p[i].getX(), (float) p[i].getY());
							view.sharedPath.moveTo((float) p[i].getX(), (float) p[i].getY());
						}
					}
				}

				view.sharedPath.lineTo((float) p[n - 1].getX(), (float) p[n - 1].getY());
				view.sharedPath.moveTo((float) p[n - 1].getX(), (float) p[n - 1].getY());

				view.lineShape = (GeneralPath) view.sharedPath.clone();
				if (view.endShape != null) {
					view.sharedPath.append(view.endShape, true);
				}
				if (view.beginShape != null) {
					view.sharedPath.append(view.beginShape, true);
				}
			}
		  ret = view.sharedPath;
		}
		} catch (Exception e) {
			System.out.println("Warning: exception in jmt.gui.jsimgraph.JGraphMod.JmtEdgeRenderer.createShape");
		}
		return ret;
	}

	protected Shape createShape2() {
		int n = view.getPointCount();
		if (n > 1) {
			JmtEdgeView tmp = (JmtEdgeView) view;
			JmtEdge cell = (JmtEdge) tmp.getCell();
			Point2D[] p = new Point2D[n];
			for (int i = 0; i < n; i++) {
				Point2D pt = tmp.getPoint(i);
				if (pt == null) {
					return null; // exit
				}
				int x = (int) pt.getX();
				int y = (int) pt.getY();
				p[i] = new Point2D.Double(x, y);
			}

			if (view.sharedPath == null) {
				view.sharedPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);
			} else {
				view.sharedPath.reset();
			}
			view.beginShape = view.lineShape = view.endShape = null;

			if (endDeco != GraphConstants.ARROW_NONE) {
				JmtCell targetOfEdge1 = (JmtCell) ((DefaultPort) cell.getTarget()).getParent();
				if (!targetOfEdge1.isLeftInputCell()) {
					view.endShape = createLineEnd(endSize, endDeco, new Point2D.Double(p[n - 1].getX() + 10, p[n - 1].getY()), p[n - 1]);
				} else {
					view.endShape = createLineEnd(endSize, endDeco, new Point2D.Double(p[n - 1].getX() - 10, p[n - 1].getY()), p[n - 1]);
				}
			}
			view.sharedPath.moveTo((float) p[0].getX(), (float) p[0].getY());

			for (int i = 0; i < n - 1; i++) {
				view.sharedPath.lineTo((float) p[i].getX(), (float) p[i].getY());
				view.sharedPath.moveTo((float) p[i].getX(), (float) p[i].getY());
			}

			view.sharedPath.lineTo((float) p[n - 1].getX(), (float) p[n - 1].getY());
			view.sharedPath.moveTo((float) p[n - 1].getX(), (float) p[n - 1].getY());

			view.lineShape = (GeneralPath) view.sharedPath.clone();
			if (view.endShape != null) {
				view.sharedPath.append(view.endShape, true);
			}
			if (view.beginShape != null) {
				view.sharedPath.append(view.beginShape, true);
			}
		}
		return view.sharedPath;
	}

	protected Shape createShapeBezier(JMTPath path) {
		int n = path.getArcsNb();
		JmtEdgeView edgeView = (JmtEdgeView) view;

		List points = edgeView.getPoints();
		JmtEdge cell = (JmtEdge) edgeView.getCell();

		if (view.sharedPath == null) {
			view.sharedPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);
		} else {
			view.sharedPath.reset();
		}
		view.beginShape = view.lineShape = view.endShape = null;
		Point2D source = (Point2D) edgeView.getPoint(0).clone();

		Point2D target = (Point2D) edgeView.getPoint(edgeView.getPointCount() - 1).clone();

		if (points.get(0) instanceof PortView) {
			points.set(0, UtilPoint.addPoints(path.getArc(0).getSource(),source));
		}
		if (points.get(points.size()-1) instanceof PortView) {
			points.set(points.size()-1, UtilPoint.addPoints(path.getArc(n-1).getTarget(),target));
		}


		Point2D controlPointSource, targetArc, controlPointTarget, sourceArc;
		Shape EndShape;
		JMTArc lastArc = path.getArcs().get(path.getArcs().size() - 1);

		//End Line decoration
		if (endDeco != GraphConstants.ARROW_NONE) {
			if (cell.getTarget() != null) {
				double theta = rotationAngle(lastArc, source, target);
				EndShape = createLineEnd(endSize, endDeco,
						new Point2D.Double(((Point2D)points.get(points.size()-1)).getX() - 10 * Math.cos(theta), ((Point2D)points.get(points.size()-1)).getY() - 10 * Math.sin(theta))
						, (Point2D) ((Point2D)points.get(points.size()-1)).clone());
				view.endShape = EndShape;

			}
		}

		// for all the arcs
		for (int i = 0; i < n; i++) {
			sourceArc = (Point2D) points.get(4 * i);
			controlPointSource = (Point2D) points.get(4 * i + 1);
			controlPointTarget = (Point2D) points.get(4 * i + 2);
			targetArc = (Point2D) points.get(4 * i + 3);
			view.sharedPath.moveTo((float) sourceArc.getX(), (float) sourceArc.getY());
			view.sharedPath.curveTo((float) controlPointSource.getX(), (float) controlPointSource.getY()
					, (float) controlPointTarget.getX(), (float) controlPointTarget.getY()
					, (float) targetArc.getX(), (float) targetArc.getY());
			view.sharedPath.moveTo((float) targetArc.getX(), (float) targetArc.getY());
		}

		view.lineShape = (GeneralPath) view.sharedPath.clone();
		if (view.endShape != null) {
			view.sharedPath.append(view.endShape, true);
		}
		if (view.beginShape != null) {
			view.sharedPath.append(view.beginShape, true);
		}

		return view.sharedPath;

	}


	/**
	 * Calculates the rotation angle for the lineEnd
	 *
	 * @param arc        an instance of JMT Arc representing the last arc of the connection shape path
	 * @param sourcePath an instance of Point2D representing the source of the connection shape path
	 * @param targetPath an instance of Point2D representing the target of the connection shape path
	 * @return theta the angle in radians
	 * @author Emma Bortone
	 */
	private double rotationAngle(JMTArc arc, Point2D sourcePath, Point2D targetPath) {
		double theta, x, y;
		Point2D controlPointTarget, controlPointSource, sourceArc, targetArc;

		sourceArc = arc.getSource();
		targetArc = arc.getTarget();
		controlPointTarget = arc.getArcPoints().get(1);
		controlPointSource = arc.getArcPoints().get(0);

		//If the control point at the target is different than 0, we use that for calculating the rotation angle
		if (controlPointTarget.getX() != 0 || controlPointTarget.getY() != 0) {
			y = -controlPointTarget.getY();
			x = -controlPointTarget.getX();
		}
		//If the control point at the target is equal to 0, then we check if we can use the control point at the source
		//of the arc
		else if (controlPointSource.getX() != 0 || controlPointSource.getY() != 0) {
			//If the control point at the source of the arc is different than 0, we use that for calculating the
			//rotation angle
			y = ((targetPath.getY() + targetArc.getY()) - (sourcePath.getY() + sourceArc.getY() + controlPointSource.getY()));
			x = ((targetPath.getX() + targetArc.getX()) - (sourcePath.getX() + sourceArc.getX() + controlPointSource.getX()));
		} else {
			//If the control point at the source of the arc is equal to 0, the rotation angle is calculated using the
			//position of the source and target of the arc.
			y = ((targetPath.getY() + targetArc.getY()) - (sourcePath.getY() + sourceArc.getY()));
			x = ((targetPath.getX() + targetArc.getX()) - (sourcePath.getX() + sourceArc.getX()));
		}
		theta = Math.atan2(y, x);
		return theta;
	}

	/**
	 * This function extends the function "paint" in order to make the tangents and control points
	 * visible when the connection is selected
	*/
	@Override
	public void paint(Graphics var1) {
		try {
		super.paint(var1);
		if (this.selected && mediator.getModel().hasConnectionShape(((JmtEdge) view.getCell()).getSourceKey(),
				((JmtEdge) view.getCell()).getTargetKey())) {
			Graphics2D var3 = (Graphics2D) var1;
			var3.setStroke(GraphConstants.SELECTION_STROKE);
			var3.setColor(this.highlightColor);
			drawTangents(var3);
			drawControlPoints(var3);
		}} catch (Exception e) {
			System.out.println("Warning: exception in jmt.gui.jsimgraph.JGraphMod.JmtEdgeRenderer.paint");
		}
	}

	/**
	 * This function draws the tangents lines of a Bezier connection
	 */
	private void drawTangents(Graphics2D var3) {
		JMTPath path = mediator.getModel().getConnectionShape(((JmtEdge) view.getCell()).getSourceKey(), ((JmtEdge) view.getCell()).getTargetKey());
		//draw tangents

		Point2D sourceArc, controlPointSource, targetArc, controlPointTarget;
		int n = path.getArcsNb();
		JmtEdgeView edgeView = (JmtEdgeView) view;
		List points = edgeView.getPoints();
		// for all the arcs
		for (int i = 0; i < n; i++) {
			sourceArc = (Point2D) points.get(i * 4);
			controlPointSource = (Point2D) points.get(4 * i + 1);
			controlPointTarget = (Point2D) points.get(4 * i + 2);
			targetArc = (Point2D) points.get(4 * i + 3);

			var3.draw(new Line2D.Double(controlPointTarget.getX(), controlPointTarget.getY(), targetArc.getX(), targetArc.getY()));
			var3.draw(new Line2D.Double(controlPointSource.getX(), controlPointSource.getY(), sourceArc.getX(), sourceArc.getY()));
		}
	}

	/**
	 * This function draws the the control points of a Bezier connection
	 */
	private void drawControlPoints(Graphics2D var3) {
		var3.setStroke(new BasicStroke(this.lineWidth));
		var3.setColor(Color.black);
		JmtEdgeView edgeView = (JmtEdgeView) view;
		List points = edgeView.getPoints();
		int side = 6;
		for (Object point : points) {
			drawRectangle(var3, side, (Point2D) point);
		}
	}

	/**
	 * This function draws a rectangle given a side size and the position of the top left edge
	 */
	private void drawRectangle(Graphics2D var3, int side, Point2D topLeftPoint) {
		Rectangle2D rect = new Rectangle2D.Double(
				topLeftPoint.getX() - side / 2.0, topLeftPoint.getY() - side / 2.0,
				side, side);
		var3.draw(rect);
		var3.setColor(this.highlightColor);
		var3.fill(rect);
		var3.setColor(Color.black);
	}

	/**
	 *This function is used to display ths hape of a bezier connection while its source or target station (or both) is
	 * being dragged
	 **/
	protected Shape createShapeBezierWhileDragged(JMTPath path, List pointsActual) {
		int n = path.getArcsNb();
		JmtEdgeView edgeView = (JmtEdgeView) view;

		List points = edgeView.getPoints();
		JmtEdge cell = (JmtEdge) edgeView.getCell();

		boolean isSourceMoving = !(points.get(0).equals(pointsActual.get(0)));
		boolean isTargetMoving = !(points.get(points.size()-1).equals(pointsActual.get(pointsActual.size()-1)));
		boolean isSourceMovingAlone = isSourceMoving & !isTargetMoving;
		if(isSourceMovingAlone) {
			//Select points to update
			for (int i = 2; i < pointsActual.size(); i++) {
				points.set(i, pointsActual.get(i));
			}
		}

		if (view.sharedPath == null) {
			view.sharedPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);
		} else {
			view.sharedPath.reset();
		}
		view.beginShape = view.lineShape = view.endShape = null;
		Point2D source = (Point2D) edgeView.getPoint(0).clone();

		Point2D target = (Point2D) edgeView.getPoint(edgeView.getPointCount() - 1).clone();

		if (points.get(0) instanceof PortView) {
			points.set(0, UtilPoint.addPoints(path.getArc(0).getSource(),source));
		}
		if (points.get(points.size()-1) instanceof PortView) {
			points.set(points.size()-1, UtilPoint.addPoints(path.getArc(n-1).getTarget(),target));
		}


		Point2D controlPointSource, targetArc, controlPointTarget, sourceArc;
		Shape EndShape;
		JMTArc lastArc = path.getArcs().get(path.getArcs().size() - 1);

		if(!isSourceMovingAlone) {
			//End Line decoration
			if (endDeco != GraphConstants.ARROW_NONE) {
				if (cell.getTarget() != null) {
					double theta = rotationAngle(lastArc, source, target);
					EndShape = createLineEnd(endSize, endDeco,
							new Point2D.Double(((Point2D) points.get(points.size() - 1)).getX() - 10 * Math.cos(theta), ((Point2D) points.get(points.size() - 1)).getY() - 10 * Math.sin(theta))
							, (Point2D) ((Point2D) points.get(points.size() - 1)).clone());
					view.endShape = EndShape;

				}
			}
		}

		// for all the arcs
		for (int i = 0; i < n; i++) {
			sourceArc = (Point2D) points.get(4 * i);
			controlPointSource = (Point2D) points.get(4 * i + 1);
			controlPointTarget = (Point2D) points.get(4 * i + 2);
			targetArc = (Point2D) points.get(4 * i + 3);
			view.sharedPath.moveTo((float) sourceArc.getX(), (float) sourceArc.getY());
			view.sharedPath.curveTo((float) controlPointSource.getX(), (float) controlPointSource.getY()
					, (float) controlPointTarget.getX(), (float) controlPointTarget.getY()
					, (float) targetArc.getX(), (float) targetArc.getY());
			view.sharedPath.moveTo((float) targetArc.getX(), (float) targetArc.getY());
		}

		view.lineShape = (GeneralPath) view.sharedPath.clone();
		if (view.endShape != null) {
			view.sharedPath.append(view.endShape, true);
		}
		if (view.beginShape != null) {
			view.sharedPath.append(view.beginShape, true);
		}


		return view.sharedPath;
	}


}
