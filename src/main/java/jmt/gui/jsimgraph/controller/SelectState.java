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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jmt.gui.jsimgraph.JGraphMod.*;

import jmt.gui.jsimgraph.UtilPoint;
import jmt.gui.jsimgraph.definitions.JMTPath;
import org.jgraph.graph.*;

/**
 * Handles all the events when the user is in the select mode

 * @author dekkar (Federico Granata)
 * Date: Jun 20, 2003
 * Time: 10:42:28 AM

 * Modified by Bertoli Marco 28-giu-2005

 */
public class SelectState extends UIStateDefault {

	private Object[] cells = null;
	private Integer[] Xmin = null;
	private Integer[] Ymin = null;
	//private Integer[] Xmax = null;
	//private Integer[] Ymax = null;

	private boolean moved = false;

	protected GraphMouseListener ml;//reference to mouse listener


	protected HashMap<Object,Point2D> initialCellPositions;
	protected HashMap<Object,Point2D> finalCellPositions;


    /** Creates the select state
	 *
	 * @param mediator
	 * @param ml
	 */
	public SelectState(Mediator mediator, GraphMouseListener ml) {
		super(mediator);
		this.ml = ml;
		initialCellPositions = new HashMap<Object, Point2D>();
		finalCellPositions = new HashMap<Object, Point2D>();
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
		ml.setHandler(null);
		if (!e.isConsumed() && mediator.isGraphEnabled()) {
			mediator.graphRequestFocus();
			int s = mediator.getTolerance();
			Rectangle2D r = mediator.fromScreen(new Rectangle(e.getX() - s, e.getY() - s, 2 * s, 2 * s));
			Point2D point = mediator.fromScreen(new Point(e.getPoint()));
			if (ml.getFocus() != null && (!mediator.containsCell(ml.getFocus().getCell())
					|| !ml.getFocus().intersects(mediator.getGraph(), r))) {
						ml.setFocus(null);

			}
			// Avoid toggling of selection between inner components and blocking region
			CellView next = mediator.getNextViewAt(ml.getFocus(), point.getX(), point.getY());
			if (next != null && next.getCell() != null) {
				if (ml.getFocus() == null || !(next.getCell() instanceof BlockingRegion)) {
					ml.setCell(next);
				}
			}
			if (ml.getFocus() == null) {
				ml.setFocus(ml.getCell());
			}

			if (!mediator.isForceMarqueeEvent(e)) {
				if (e.getClickCount() == mediator.getEditClickCount() && ml.getFocus() != null
						//&& ml.getFocus().isLeaf()
						//&& ml.getFocus().getParentView() == null
						) {
					// Start Editing Only if cell is editable - BERTOLI MARCO
					if (mediator.isCellEditable(ml.getFocus().getCell())) {
						ml.handleEditTrigger(ml.getFocus().getCell());
						e.consume();
						ml.setCell(null);
					} // Otherwise do nothing - BERTOLI MARCO

					else {
						e.consume();
					}
				} else if (!mediator.isToggleSelectionEvent(e)) {

					if (ml.getHandle() != null) {
						ml.setHandler(ml.getHandle());
						ml.getHandle().mousePressed(e);
					}
					//If a Bezier edge is selected, then switch state to go in BezierEdgeModificationState
					if (ml.getCell() != null) {
						if (ml.getCell().getCell() instanceof JmtEdge) {
							if (mediator.getModel().hasConnectionShape(((JmtEdge) ml.getCell().getCell()).getSourceKey(),
									((JmtEdge) ml.getCell().getCell()).getTargetKey())) {
								mediator.selectCellForEvent(ml.getCell().getCell(), e);

								ml.setBezierEdgeModificationState(((JmtEdge) ml.getCell().getCell()));
								e.consume();
								ml.setCell(null);
							}
						}
					}

					// Immediate Selection
					if (!e.isConsumed() && ml.getCell() != null && !mediator.isCellSelected(ml.getCell())) {
						mediator.selectCellForEvent(ml.getCell().getCell(), e);

						ml.setFocus(ml.getCell());

						if (ml.getHandle() != null) {
							ml.getHandle().mousePressed(e);
							ml.setHandler(ml.getHandle());
						}


						e.consume();
						ml.setCell(null);
					}
				}
			}

			//Marquee Selection
			if (!e.isConsumed() && (!mediator.isToggleSelectionEvent(e) || ml.getFocus() == null)) {
				if (ml.getMarquee() != null) {
					ml.getMarquee().mousePressed(e);
					ml.setHandler(ml.getMarquee());
				}
			}
		}
		JmtJGraph graphtmp = (JmtJGraph)mediator.getGraph();
		initialCellPositions = new HashMap<Object, Point2D>();
		finalCellPositions = new HashMap<Object, Point2D>();
		updateInitialPosition(graphtmp.getSelectionCells());
	}



	@Override
	public void handleMove(MouseEvent e) {
		if (ml.getPreviousCursor() == null) {
			ml.setPreviousCursor(mediator.getGraphCursor());
		}
		if (mediator.isGraphEnabled()) {
			if (ml.getMarquee() != null) {
				ml.getMarquee().mouseMoved(e);
			}
			if (ml.getHandle() != null) {
				ml.getHandle().mouseMoved(e);
			}
			if (!e.isConsumed() && ml.getPreviousCursor() != null) {
				mediator.setGraphCursor(ml.getPreviousCursor());
				ml.setPreviousCursor(null);
			}
		}
		e.consume();
	}

	@Override
	public void handleDrag(MouseEvent e) {
		mediator.setIsReleased(false);
		mediator.autoscroll(e.getPoint());
		if (ml.getHandler() != null && ml.getHandler() == ml.getMarquee()) {
			ml.getMarquee().mouseDragged(e);
		}
		else if (ml.getHandler() == null && !mediator.isGraphEditing() && ml.getFocus() != null) {
			if (!mediator.isCellSelected(ml.getFocus().getCell())) {
				mediator.selectCellForEvent(ml.getFocus().getCell(), e);
				ml.setCell(null);
			}
			if (ml.getHandle() != null) {
				ml.getHandle().mousePressed(e);
			}
			ml.setHandler(ml.getHandle());
		}
		if (ml.getHandle() != null && ml.getHandler() == ml.getHandle()) {
			Point2D current = mediator.snap(e.getPoint());
			// BERTOLI MARCO - Added to avoid dragging of unselected elements (caused bugs)
			JmtJGraph graphtmp = (JmtJGraph) mediator.getGraph();
			cells = graphtmp.getSelectionCells();
			if (cells.length > 0) {
				boolean wereOnGrid = true;
				Xmin = new Integer[cells.length];
				Ymin = new Integer[cells.length];

				for (int i = 0; i < cells.length; i++) {
					if ((cells[i] instanceof JmtCell )&& !(cells[i] instanceof JmtEdge)) {
						if (mediator.snapCellByPort((JmtCell) cells[i])){//returns true if the cell was snapped to the grid
							wereOnGrid = false;
						}
					}
				}
				if(wereOnGrid){
					ml.getHandle().mouseDragged(e);
					//Xmax = new Integer[cells.length];
					//Ymax = new Integer[cells.length];
					for (int i = 0; i < cells.length; i++) {
						if (cells[i] instanceof JmtCell) {
                            Object key = ((CellComponent) ((JmtCell)cells[i]).getUserObject()).getKey();
							Rectangle2D rett = GraphConstants.getBounds(((JmtCell) cells[i]).getAttributes());
							Xmin[i] = new Integer((int) rett.getMinX());
							Ymin[i] = new Integer((int) rett.getMinY());
							//Xmax[i] = new Integer((int) rett.getMaxX());
							//Ymax[i] = new Integer((int) rett.getMaxY());
							moved = true;
						}
						if (cells[i] instanceof BlockingRegion) {
							CellView groupview = (graphtmp.getGraphLayoutCache()).getMapping(cells[i], false);
							Rectangle2D rett2 = groupview.getBounds();
							if (rett2 != null) {
								Xmin[i] = new Integer((int) rett2.getMinX());
								Ymin[i] = new Integer((int) rett2.getMinY());
								//Xmax[i] = new Integer((int) rett2.getMaxX());
								//Ymax[i] = new Integer((int) rett2.getMaxY());
								moved = true;
							}
						}

					}

				}
				else {
					mediator.setIsReleased(true);
					try {
						if (e != null && !e.isConsumed()) {
							if (ml.getHandler() == ml.getMarquee() && ml.getMarquee() != null) {
								ml.getMarquee().mouseReleased(e);
							} else if (ml.getHandler() == ml.getHandle() && ml.getHandle() != null) {
								ml.getHandle().mouseReleased(e);
							}
							if (ml.isDescendant(ml.getCell(), ml.getFocus()) && e.getModifiers() != 0) {
								ml.setCell(ml.getFocus());
							}
							mediator.handlesBlockingRegionDrag();
						}
					} finally {
						ml.setHandler(null);
						ml.setCell(null);
					}
				}
			}
		}
	}

	// Heavily modified by Giuseppe De Cicco & Fabio Granara
	@Override
	public void handleRelease(MouseEvent e) {
		mediator.setIsReleased(true);
		try {
			if (e != null && !e.isConsumed()) {
				if (ml.getHandler() == ml.getMarquee() && ml.getMarquee() != null) {
					ml.getMarquee().mouseReleased(e);
				} else if (ml.getHandler() == ml.getHandle() && ml.getHandle() != null) {
					ml.getHandle().mouseReleased(e);
				}
				if (ml.isDescendant(ml.getCell(), ml.getFocus()) && e.getModifiers() != 0) {
					// Do not switch to parent if Special Selection
					ml.setCell(ml.getFocus());
				}

				// Puts selected cells in good place to avoid overlapping
				if (moved && Xmin.length > 0 && Ymin.length > 0) {
					mediator.putSelectedCellsInGoodPlace(cells, Xmin, Ymin);
					mediator.avoidOverlappingCell(cells);
					moved = false;


				}

				if (!e.isConsumed() && ml.getCell() != null) {
					Object tmp = ml.getCell().getCell();
					boolean wasSelected = mediator.isCellSelected(tmp);
					mediator.selectCellForEvent(tmp, e);
					ml.setFocus(ml.getCell());
					ml.postProcessSelection(e, tmp, wasSelected);
				}


				// Update final position
				JmtJGraph graphtmp = (JmtJGraph)mediator.getGraph();
				updateFinalPosition(graphtmp.getSelectionCells());
				mediator.updateBezierPath(computeOffsetByCell());

				mediator.updateLabelAfterMovingCell(cells);

				// Notify mediator that object can have been placed inside or
				// outside a blocking region
				mediator.handlesBlockingRegionDrag();
			}
		} finally {
			ml.setHandler(null);
			ml.setCell(null);
		}

	}

	private HashMap<Object, Point2D> computeOffsetByCell() {
		HashMap<Object, Point2D> offsetByCell = new HashMap<Object,Point2D>();
		Set correctKeys = new HashSet<>(finalCellPositions.keySet());
		correctKeys.retainAll(initialCellPositions.keySet());
		for (Object key : correctKeys) {
			offsetByCell.put(key,UtilPoint.subtractPoints(finalCellPositions.get(key),initialCellPositions.get(key)));
		}
		return offsetByCell;
	}

	@Override
	public void handleEnter(MouseEvent e) {
		mediator.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	private void updateInitialPosition(Object[] selectionCells) {
		for(Object cell : selectionCells){
			if(cell instanceof JmtCell){
				Object key = ((CellComponent) ((JmtCell)cell).getUserObject()).getKey();
				//Point2D pos = mediator.getModel().getStationPosition(key);
				Rectangle2D rett = GraphConstants.getBounds(((JmtCell) cell).getAttributes());
				Integer x = new Integer((int) rett.getMinX());
				Integer y = new Integer((int) rett.getMinY());
				initialCellPositions.put(key,new Point2D.Double(x,y));
			}
			if(cell instanceof BlockingRegion) {
				for (Object c : ((BlockingRegion) cell).getChildren()) {
					if (c instanceof JmtCell) {
						Object key = ((CellComponent) ((JmtCell) c).getUserObject()).getKey();
						Rectangle2D rett = GraphConstants.getBounds(((JmtCell) c).getAttributes());
						Integer x = new Integer((int) rett.getMinX());
						Integer y = new Integer((int) rett.getMinY());
						initialCellPositions.put(key,new Point2D.Double(x,y));
					}
				}
			}
		}
	}

	private void updateFinalPosition(Object[] selectionCells) {
		for(Object cell : selectionCells){
			if(cell instanceof JmtCell){
				Object key = ((CellComponent) ((JmtCell)cell).getUserObject()).getKey();
				Rectangle2D rett = GraphConstants.getBounds(((JmtCell) cell).getAttributes());
				Integer x = new Integer((int) rett.getMinX());
				Integer y = new Integer((int) rett.getMinY());
				finalCellPositions.put(key,new Point2D.Double(x,y));
			}
			if(cell instanceof BlockingRegion) {
				for (Object c : ((BlockingRegion) cell).getChildren()) {
					if (c instanceof JmtCell) {
						Object key = ((CellComponent) ((JmtCell) c).getUserObject()).getKey();
						Rectangle2D rett = GraphConstants.getBounds(((JmtCell) c).getAttributes());
						Integer x = new Integer((int) rett.getMinX());
						Integer y = new Integer((int) rett.getMinY());
						finalCellPositions.put(key,new Point2D.Double(x,y));
					}
				}
			}
		}
	}







}
