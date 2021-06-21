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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.TreeNode;

import jmt.gui.common.JMTImageLoader;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.Port;

/**
 * vertex cell for jmt
 *
 * @author Federico Granata
 * Date: 11-lug-2003
 * Time: 13.48.34
 *
 * @author Bertoli Marco
 */
public abstract class JmtCell extends DefaultGraphCell {

	private static final long serialVersionUID = 1L;

	private Dimension imageDimension;
	// Used to determine if parent of this cell has changed (to detect enter and exit from
	// blocking regions)
	private TreeNode parentRef;

	/**
	 * Different kind of nodes.
	 */
	public static final int SOURCE = 0;
	public static final int SINK = 1;
	public static final int TERMINAL = 2;
	public static final int ROUTER = 3;
	public static final int DELAY = 4;
	public static final int SERVER = 5;
	public static final int FORK = 6;
	public static final int JOIN = 7;
	public static final int LOGGER = 8;
	public static final int CLASSSWITCH = 9;
	public static final int SEMAPHORE = 10;
	public static final int SCALER = 11;
	public static final int PLACE = 12;
	public static final int TRANSITION = 13;

	private Port[] ports;

	// Giuseppe De Cicco & Fabio Granara
	public boolean seen = false;
	public int in = 0;
	public int out = 0;
	public boolean okin = false;
	public boolean okout = false;
	public int sons = 1;
	public int type;
	private boolean leftInputCell = true;
	private int standardIconWidth;
	private int standardIconHeight;

	/**
	 * Tells if this component can be placed on JGraph or has been disabled. This is
	 * useful to load old models with disabled components (like terminals)
	 */
	public static final boolean canBePlaced = true;

	private boolean isFreeRotationAllowed =true;
	private double rotationAngle =0;

	/**
	 * Creates a graph cell and initializes it with the specified user object.
	 *
	 * @param userObject an Object provided by the user that constitutes
	 *                   the cell's data
	 *
	 * Conti Andrea  01-09-2003
	 * Bertoli Marco 04-giu-2005
	 */
	public JmtCell(String icon, Object userObject) {
		super(userObject);
		ImageIcon ico = JMTImageLoader.loadImage(icon);
		GraphConstants.setIcon(attributes, ico);
		standardIconHeight = ico.getIconHeight();
        standardIconWidth = ico.getIconWidth();
		imageDimension = new Dimension(ico.getIconWidth(), ico.getIconHeight());
		GraphConstants.setSizeable(attributes, false);
		GraphConstants.setSize(attributes, imageDimension);
	}

	// Giuseppe De Cicco & Fabio Granara
	public void AddIn() {
		in += 1;
	}

	// Giuseppe De Cicco & Fabio Granara
	public void SubIn() {
		in -= 1;
	}

	// Giuseppe De Cicco & Fabio Granara
	public void AddOut() {
		out += 1;
	}

	// Giuseppe De Cicco & Fabio Granara
	public void SubOut() {
		out -= 1;
	}

	/**
	 * Returns Cell's real size (this method considers Icon and name size)
	 *
	 * @param graph <code>JGraph</code> object to retrieve font dimension informations
	 * @return cell's real size
	 *
	 * Bertoli Marco  4-giu-2005
	 */
	public Dimension getSize(JGraph graph) {
		Dimension cellDimension = (Dimension) imageDimension.clone();
		// Gets the graph font
		Font font = graph.getFont();
		// Gets the graphical context
		Graphics2D g2D = (Graphics2D) graph.getGraphics();
		// Gets the bounds of the cell name
		FontRenderContext frc = g2D.getFontRenderContext();
		Rectangle r = font.getStringBounds(getUserObject().toString(), frc).getBounds();
		// Sets the cell dimension
		cellDimension.height += r.height + 5;
		cellDimension.width = Math.max(cellDimension.width, r.width + 10);
		return cellDimension;
	}

	/**
	 * Creates the ports for this vertex
	 *
	 * @return array of ports
	 */
	public abstract Port[] createPorts();

	/**
	 * Sets all the attributes like background colour, dimensions, port number
	 * & position
	 *
	 * @param pt
	 * @return created map
	 */
	public Hashtable<Object, Map> setAttributes(Point2D pt, JGraph graph) {
		//contains attributes of the cell & ports
		Hashtable<Object, Map> nest = new Hashtable<Object, Map>();

		Dimension cellDimension = getSize(graph);
		//contains attributes of the cell
		Map attr = getAttributes();
		GraphConstants.setBounds(attr, new Rectangle2D.Double(pt.getX(), pt.getY(), cellDimension.getWidth(), cellDimension.getHeight()));
		GraphConstants.setEditable(attr, false);
		GraphConstants.setBackground(attr, graph.getBackground());
		nest.put(this, attr);

		//create ports
		ports = createPorts();
		Icon icon = GraphConstants.getIcon(attr);
		updatePortPositions(nest, icon, cellDimension);
		for (Port port : ports) {
			add((DefaultPort) port);
		}
		return nest;
	}

	public void updatePortPositions(Map<Object, Map> nest, Icon icon, Dimension cellDimension) {
		for (Port port : ports) {
			Map attr = new Hashtable();
			if (port instanceof InputPort && isLeftInputCell() || port instanceof OutputPort && !isLeftInputCell()) {
				GraphConstants.setOffset(attr, getInPortOffset(icon, cellDimension));
			} else {
				GraphConstants.setOffset(attr, getOutPortOffset(icon, cellDimension));
			}
			nest.put(port, attr);
		}
	}

	/**
	 * Changes the bounds of the cell
	 *
	 * @param icon icon of the cell
	 * @param graph
	 */
	public void updateCellSize(Icon icon, JGraph graph) {
		imageDimension = new Dimension(icon.getIconWidth(), icon.getIconHeight());
		Map attr = this.getAttributes();

		//Get old size
		Rectangle2D oldBounds = GraphConstants.getBounds(attr);

		//Get new cell dimension (icon + name)
		Dimension newCellDimension = this.getSize(graph);
		//set new bounds
		GraphConstants.setBounds(attr, new Rectangle2D.Double(oldBounds.getX(), oldBounds.getY(), newCellDimension.getWidth(), newCellDimension.getHeight()));
	}

    /**
     * Returns the offset of the input port
     *
     * @param icon icon of the cell
     * @param cellDimension dimension of the cell
     *
     * Modified by Emma Bortone to update the port offset in the case of free rotation
     */
	private Point getInPortOffset(Icon icon, Dimension cellDimension) {
		int iconHeight = icon.getIconHeight();
		int iconWidth = icon.getIconWidth();
		double radianRotationAngle = Math.toRadians(rotationAngle);
		int xOff, yOff;

		double x=(int) ((standardIconWidth/2)*Math.cos(radianRotationAngle+Math.PI)+iconWidth/2);
		double y=(int) ((standardIconWidth/2)*Math.sin(radianRotationAngle+Math.PI)+iconHeight/2);

		xOff =(int)  ((cellDimension.width - iconWidth) / 2 + x )* 1000 / cellDimension.width;
		yOff =(int) y * 1000 / cellDimension.height;

		return new Point(xOff, yOff);
	}

    /**
     * Returns the offset of the output port
     *
     * @param icon icon of the cell
     * @param cellDimension dimension of the cell
     *
     * Modified by Emma Bortone to update the port offset in the case of free rotation
     */
	private Point getOutPortOffset(Icon icon, Dimension cellDimension) {
		int iconHeight = icon.getIconHeight();
		int iconWidth = icon.getIconWidth();
        double radianRotationAngle = Math.toRadians(rotationAngle);
		int xOff, yOff;

		double x=(int) ((standardIconWidth/2)*Math.cos(radianRotationAngle)+iconWidth/2);
		double y=(int) ((standardIconWidth/2)*Math.sin(radianRotationAngle)+iconHeight/2);

		xOff =(int)  ((cellDimension.width - iconWidth) / 2 + x )* 1000 / cellDimension.width;
		yOff =(int) y * 1000 / cellDimension.height;

		return new Point(xOff, yOff);
	}

	/**
	 * Tells if the InputPort of this cell is on the left side
	 * @return true if the InputPort of this cell is on the left side
	 */
	// Giuseppe De Cicco & Fabio Granara
	public boolean isLeftInputCell() {
		return leftInputCell;
	}


	// Giuseppe De Cicco & Fabio Granara
	public void setLeftInputCell(boolean state) {
		leftInputCell = state;
	}

	/**
	 * Resets stored parent information for this cell
	 */
	public void resetParent() {
		parentRef = getParent();
	}

	/**
	 * Tells if this cell parent was changed since last call to resetParent() method
	 *
	 * @return true if parent changed, false otherwise
	 * @see #resetParent()
	 */
	public boolean parentChanged() {
		if (parentRef == null && getParent() == null) {
			return false;
		} else if (parentRef == null || getParent() == null) {
			return true;
		} else {
			return !parentRef.equals(getParent());
		}
	}

	/**
	 * Tells if this station generates or destroys jobs (useful for blocking region
	 * management)
	 *
	 * @return true if this station generates or destroy jobs, false otherwise
	 */
	public boolean generateOrDestroyJobs() {
		return false;
	}

	/**
	 * Returns previous parent of this cell (the one present when resetParent()
	 * method was called)
	 *
	 * @return previous parent of this cell
	 * @see #resetParent()
	 */
	public TreeNode getPrevParent() {
		return parentRef;
	}

	/**
	 * Returns the name of the icon of this cell
	 *
	 * @return the name of the icon of this cell
	 */
	public abstract String getIcon();

	/**
	 * Sets the value of the attribute isFreeRotationAllowed
	 * @param value : tells if the free rotation is allowed
	 */
	public void setFreeRotationAllowed(boolean value){
		isFreeRotationAllowed=value;
		if (!isFreeRotationAllowed){
			this.setRotationAngle(0);
		}
	}

	/**
	 * Tells if the free rotation is allowed for this cell
	 * @return true if the cell is only linked to bezier curves
	 */
	public boolean isFreeRotationAllowed(){
		return isFreeRotationAllowed;
	}

	/**
	 * Sets the angle of rotation of the cell
	 * @param value : value of the rotation angle.
	 */
	public void setRotationAngle(double value){
		rotationAngle=value;
	}

	/**
	 * @return the angle of rotation of the cell (in degrees)
	 */
	public double getRotationAngle(){
		return rotationAngle;
	}

	/**
	 * Sets the attributes that have an impact on the rendering of the cell Image :
	 *
	 * @param isMirrored :  tells if the InputPort of this cell is on the left side
	 * @param isFreeRotationAllowed : tells if the free rotation is allowed
	 * @param angle : value of the rotation angle.
	 */
	public void setIconModifiers(boolean isMirrored, boolean isFreeRotationAllowed, double angle){
		this.setLeftInputCell(!isMirrored);
		this.setFreeRotationAllowed(isFreeRotationAllowed);
		this.setRotationAngle(angle);
	}

	/**
	 * return the dimension of the icon
	 * @return Dimension of the icon
	 */
	public Dimension getIconDimension() {
		return imageDimension;
	}

	/**
	 * return the dimension of the icon
	 * @return Dimension of the icon
	 */
	public Dimension getStandardIconDimension(){
		return new Dimension(standardIconWidth, standardIconHeight) ;
	}
}
