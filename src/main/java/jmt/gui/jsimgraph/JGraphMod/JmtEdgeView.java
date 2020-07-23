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
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

import jmt.gui.jsimgraph.UtilPoint;
import jmt.gui.jsimgraph.controller.Mediator;

import jmt.gui.jsimgraph.definitions.JMTPath;
import org.jgraph.JGraph;
import org.jgraph.graph.*;

/**
 * <p>Title: JmtEdgeView </p>
 * @author Giuseppe De Cicco & Fabio Granara
 * 		Date: 15-ott-2006
 * 
 */
public class JmtEdgeView extends EdgeView {

	private static final long serialVersionUID = 1L;
	private static JmtEdgeRenderer renderer;
	private Mediator mediator;

	public JmtEdgeView() {
		super();
	}

	public JmtEdgeView(Object cell) {
		super(cell);

	}

	public JmtEdgeView(Object cell, Mediator factory) {
		super(cell);
		this.mediator = factory;
		this.renderer = new JmtEdgeRenderer(mediator);
	}

	@Override
	public CellViewRenderer getRenderer() {
		return renderer;
	}


	@Override
	public boolean intersects(JGraph var1, Rectangle2D var2) {
		Rectangle2D var4 = this.getBounds();
		if (var4.intersects(var2)){
			GeneralPath path = (GeneralPath) this.getShape();
			return path.intersects(var2);
		}
		else {
			return false;
		}
	}

	@Override
	public Shape getShape() {
		JmtEdge edge = (JmtEdge) getCell();
		if ((sharedPath != null)) {
			return sharedPath;
		}
		// Test if a User defined path exists
		else if (mediator.getModel().hasConnectionShape(edge.getSourceKey(),edge.getTargetKey()) && (mediator.getIsReleased())) {
			sharedPath = (GeneralPath) renderer.createShapeBezier(this.getPath());
			mediator.getModel().setAbsoluteControlPoints(edge.getSourceKey(),edge.getTargetKey(),(ArrayList<Point2D>) this.getPoints());
			return sharedPath;
		}
		else if (mediator.getModel().hasConnectionShape(edge.getSourceKey(),edge.getTargetKey()) && !(mediator.getIsReleased())) {
			java.util.List pointsActual = mediator.getModel().getAbsoluteControlPoints(edge.getSourceKey(),edge.getTargetKey());
			sharedPath = (GeneralPath) renderer.createShapeBezierWhileDragged(this.getPath(), pointsActual);
			return sharedPath ;
		} else if (mediator.getIsReleased()) {
			return sharedPath = (GeneralPath) renderer.createShape();

		} else {
			return sharedPath = (GeneralPath) renderer.createShape2();
		}
	}

	public JMTPath getPath() {
		JmtEdge edge = (JmtEdge) getCell();
		return mediator.getModel().getConnectionShape(edge.getSourceKey(),edge.getTargetKey());
	}


}

