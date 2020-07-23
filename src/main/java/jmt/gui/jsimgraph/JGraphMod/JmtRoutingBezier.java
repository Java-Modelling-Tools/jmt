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

import jmt.gui.jsimgraph.UtilPoint;
import jmt.gui.jsimgraph.definitions.JMTArc;
import jmt.gui.jsimgraph.definitions.JMTPath;
import org.jgraph.graph.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**

 * @author Emma Bortone
 * Date: 24-March-2020
 * Time: 14.57.00

 * This function returns a list containing the routing points for a Bezier edge.
 * A Bezier conneciton is a path is composed of several arcs
 * The list contains the source, target and control points for each arc of the path.
 */
public class JmtRoutingBezier implements Edge.Routing {

	private static final long serialVersionUID = 1L;

	public List<Point2D> route(GraphLayoutCache cache, EdgeView edgeView) {
		JMTPath path = ((JmtEdgeView) edgeView).getPath();
		List<Point2D> list = new ArrayList<Point2D>();

		int n = edgeView.getPointCount();
		Point2D from = (Point2D) edgeView.getPoint(0);
		if (edgeView.getSource() instanceof PortView) {
			from = (Point2D) ((PortView) edgeView.getSource()).getLocation();
		}
		Point2D to = (Point2D) edgeView.getPoint(n - 1);
		CellView trg = edgeView.getTarget();
		if (trg instanceof PortView) {
			to = (Point2D) ((PortView) trg).getLocation();
		}

		if (from != null && to != null) {

			int nbArcs = path.getArcsNb();
			Point2D sourceArc, controlPointSource, targetArc, controlPointTarget;

			// for every arc in the path except the last one :
			for (int i = 0; i < nbArcs - 1; i++) {
				sourceArc = UtilPoint.addPoints(path.getArc(i).getSource(),from);
				targetArc = UtilPoint.addPoints(path.getArc(i).getTarget(),from);
				controlPointSource = UtilPoint.addPoints(path.getArc(i).getArcPoints().get(0),sourceArc);
				controlPointTarget = UtilPoint.addPoints(path.getArc(i).getArcPoints().get(1),targetArc);

				list.add(sourceArc);
				list.add(controlPointSource);
				list.add(controlPointTarget);
				list.add(targetArc);

			}
			//for last arc
			sourceArc = UtilPoint.addPoints(path.getArc(nbArcs-1).getSource(),from);
			targetArc = UtilPoint.addPoints(path.getArc(nbArcs-1).getTarget(),to);
			controlPointSource = UtilPoint.addPoints(path.getArc(nbArcs-1).getArcPoints().get(0),sourceArc);
			controlPointTarget = UtilPoint.addPoints(path.getArc(nbArcs-1).getArcPoints().get(1),targetArc);

			list.add(sourceArc);
			list.add(controlPointSource);
			list.add(controlPointTarget);
			list.add(targetArc);
		}
		return list;
	}

	public int getPreferredLineStyle(EdgeView edgeView) {
		return GraphConstants.STYLE_BEZIER;
	}

}
