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

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Vector;

import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.EdgeRenderer;
import org.jgraph.graph.GraphConstants;

/**
 * <p>Title: JmtEdgeRenderer </p>
 * @author Giuseppe De Cicco & Fabio Granara
 * 		Date: 15-ott-2006
 * 
 */
public class JmtEdgeRenderer extends EdgeRenderer {

	private static final long serialVersionUID = 1L;

	@Override
	protected Shape createShape() {
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
							Vector pointOnFirstPoints = new Vector();
							for (int q = 0; q < cell.intersectionEdgePoint.size(); q++) {
								if ((int) p[0].getY() == (int) cell.intersectionEdgePoint.get(q).getY()) {
									pointOnFirstPoints.add(cell.intersectionEdgePoint.get(q));
								}
							}

							for (int j = 0; j < pointOnFirstPoints.size(); j++) {
								if (p[1].getX() - p[0].getX() > 0) {
									if ((int) p[0].getY() == (int) ((Point2D) pointOnFirstPoints.get(j)).getY()) {
										if (p[0].getY() == ((Point2D) pointOnFirstPoints.get(j)).getY()
												&& (int) p[0].getX() < (int) ((Point2D) pointOnFirstPoints.get(j)).getX()
												&& (int) ((Point2D) pointOnFirstPoints.get(j)).getX() < (int) p[1].getX()) {
											if ((j + 1) < pointOnFirstPoints.size()
													&& ((Point2D) pointOnFirstPoints.get(j + 1)).getX() > ((Point2D) pointOnFirstPoints.get(j)).getX()
													&& ((Point2D) pointOnFirstPoints.get(j + 1)).getX() - ((Point2D) pointOnFirstPoints.get(j)).getX() < 10
													&& ((Point2D) pointOnFirstPoints.get(j)).getY() == ((Point2D) pointOnFirstPoints.get(j + 1)).getY()) {
												double middleX = (((Point2D) pointOnFirstPoints.get(j + 1)).getX() + ((Point2D) pointOnFirstPoints.get(j)).getX()) / 2;
												view.sharedPath.lineTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() - 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() - 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												view.sharedPath.curveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() - 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY(), (float) middleX,
														(float) (((Point2D) pointOnFirstPoints.get(j)).getY() - 10),
														(float) (((Point2D) pointOnFirstPoints.get(j + 1)).getX() + 5),
														(float) ((Point2D) pointOnFirstPoints.get(j + 1)).getY());
												view.sharedPath.moveTo((float) (((Point2D) pointOnFirstPoints.get(j + 1)).getX() + 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												j++;
												done = true;
											} else {
												view.sharedPath.lineTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() - 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() - 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												view.sharedPath.curveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() - 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY(),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getX(),
														(float) (((Point2D) pointOnFirstPoints.get(j)).getY() - 10),
														(float) (((Point2D) pointOnFirstPoints.get(j)).getX() + 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() + 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												done = true;
											}
										}
									}
								} else {
									Vector temp = new Vector();
									for (int z = 0; z < pointOnFirstPoints.size(); z++) {
										temp.add(pointOnFirstPoints.get(pointOnFirstPoints.size() - z - 1));
									}
									pointOnFirstPoints = temp;

									boolean largearc = false;
									if ((int) p[0].getY() == (int) ((Point2D) pointOnFirstPoints.get(j)).getY()
											&& (int) p[0].getX() > (int) ((Point2D) pointOnFirstPoints.get(j)).getX()
											&& (int) ((Point2D) pointOnFirstPoints.get(j)).getX() > (int) p[1].getX()) {
										if ((j + 1) < pointOnFirstPoints.size()
												&& ((Point2D) pointOnFirstPoints.get(j + 1)).getX() < ((Point2D) pointOnFirstPoints.get(j)).getX()
												&& ((Point2D) pointOnFirstPoints.get(j)).getX() - ((Point2D) pointOnFirstPoints.get(j + 1)).getX() < 10) {
											double middleX = ((int) ((Point2D) pointOnFirstPoints.get(j + 1)).getX() + (int) ((Point2D) pointOnFirstPoints.get(j)).getX()) / 2;
											view.sharedPath.lineTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() + 5), (float) p[0].getY());
											view.sharedPath.moveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() + 5), (float) p[0].getY());
											view.sharedPath.curveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() + 5),
													(float) p[0].getY(), (float) middleX, (float) (p[0].getY() - 10),
													(float) (((Point2D) pointOnFirstPoints.get(j + 1)).getX() - 5), (float) p[0].getY());
											view.sharedPath.moveTo((float) (((Point2D) pointOnFirstPoints.get(j + 1)).getX() - 5), (float) p[0].getY());
											j = j + 1;
											largearc = true;
											done = true;
										} else {
											if (!largearc) {
												view.sharedPath.lineTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() + 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() + 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												view.sharedPath.curveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() + 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY(),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getX(),
														(float) (((Point2D) pointOnFirstPoints.get(j)).getY() - 10),
														(float) (((Point2D) pointOnFirstPoints.get(j)).getX() - 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												view.sharedPath.moveTo((float) (((Point2D) pointOnFirstPoints.get(j)).getX() - 5),
														(float) ((Point2D) pointOnFirstPoints.get(j)).getY());
												done = true;
											}
										}
									}

									Vector temp2 = new Vector();
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
		}
		return view.sharedPath;
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

}
