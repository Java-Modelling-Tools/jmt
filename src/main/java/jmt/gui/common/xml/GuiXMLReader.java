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

package jmt.gui.common.xml;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JOptionPane;

import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.jsimgraph.definitions.JMTArc;
import jmt.gui.jsimgraph.definitions.JMTPath;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;
import jmt.gui.jsimgraph.definitions.JMTPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <p>Title: Gui XML Reader</p>
 * <p>Description: Reads JModel GUI specific information from an XML file. This
 * class provides methods for model load.</p>
 *
 * @author Bertoli Marco
 *         Date: 27-lug-2005
 *         Time: 11.04.13
 */
public class GuiXMLReader implements GuiXMLConstants {

	/**
	 * Parses given Gui XML Document to reconstruct all gui related stuff. This method
	 * is designed to be called ONLY when all simulation's data has already been put into
	 * data structure (as will never create new user classes or stations)
	 * @param xml document to be parsed
	 * @param model data model to be elaborated
	 */
	public static void parseXML(Document xml, CommonModel model) {
		parseXML(xml.getDocumentElement(), model);
	}

	/**
	 * Parses given Gui XML Document to reconstruct all gui related stuff. This method
	 * is designed to be called ONLY when all simulation's data has already been put into
	 * data structure (as will never create new user classes or stations)
	 * @param root root element of Document to be parsed
	 * @param model data model to be elaborated
	 */
	public static void parseXML(Element root, CommonModel model) {
		//System.out.println("GuiXMLReader -> parseXML -> root"+ root);
		//System.out.println("GuiXMLReader -> parseXML -> model"+ model);
		if (model instanceof JSimGraphModel) {
			setUserClasses(root, (JSimGraphModel) model);
			setStations(root, (JSimGraphModel) model);

			/* Modification by Emma */
			if(root.getElementsByTagName(XML_E_CONNECTION_SHAPE).getLength() > 0){
				//System.out.println("GuiXMLReader -> parseXML -> CONNECTION SHAPE DETECTED");
				setConnectionShapes(root, (JSimGraphModel) model);
			}
		}
		setParametricAnalysis(root, model);
	}


	protected static void setConnectionShapes(Element root, JSimGraphModel model) {
		NodeList connectionShapes = root.getElementsByTagName(XML_E_CONNECTION_SHAPE);
		// Build a map of station name -> station key
		HashMap<String, Object> names = new HashMap<String, Object>();
		Vector<Object> keys = model.getStationKeys();
		for (int i = 0; i < keys.size(); i++) {
			names.put(model.getStationName(keys.get(i)), keys.get(i));
		}

		Element connectionShape, arc , SourcePoint,point0 , point1, targetPoint;
		String target, source;
		NodeList arcs ,SourceNode, points , targetNode ;
		double source_x, source_y, x0, y0, x1, y1, target_x, target_y;
		ArrayList pointsList;
		ArrayList<JMTArc> arcsList;
		// For each stored Station
		for (int i = 0; i < connectionShapes.getLength(); i++) {
			connectionShape = (Element) connectionShapes.item(i);
			target = connectionShape.getAttribute(XML_A_CONNECTION_TARGET);
			source = connectionShape.getAttribute(XML_A_CONNECTION_SOURCE);
			if (names.containsKey(source) && names.containsKey(target) && model.areConnected(names.get(source),names.get(target))) {
				// For each arc of the path
				arcsList = new ArrayList<JMTArc>();
				arcs = connectionShape.getElementsByTagName(XML_E_ARC);
				for(int j = 0; j < arcs.getLength();j++){
					pointsList = new ArrayList<JMTPoint>();
					arc = (Element) arcs.item(j);

					// Extract the source point from the arc *************************
					SourceNode = arc.getElementsByTagName(XML_E_SOURCE);
					SourcePoint = (Element) SourceNode.item(0);

					source_x = Double.parseDouble(SourcePoint.getAttribute(XML_A_SOURCE_X));
					source_y = Double.parseDouble(SourcePoint.getAttribute(XML_A_SOURCE_Y));

					// Extract control points from the arc ***************************
					points = arc.getElementsByTagName(XML_E_POINT);
					point0 = (Element) points.item(0);
					point1 = (Element) points.item(1);

					x0 = Double.parseDouble(point0.getAttribute(XML_A_POINT_X));
					x1 = Double.parseDouble(point1.getAttribute(XML_A_POINT_X));
					y0 = Double.parseDouble(point0.getAttribute(XML_A_POINT_Y));
					y1 = Double.parseDouble(point1.getAttribute(XML_A_POINT_Y));
					pointsList.add(new Point2D.Double(x0, y0));
					pointsList.add(new Point2D.Double(x1,y1));

					// Extract the target point from the arc ****************************
					targetNode = arc.getElementsByTagName(XML_E_TARGET);
					targetPoint = (Element) targetNode.item(0);

					target_x = Double.parseDouble(targetPoint.getAttribute(XML_A_TARGET_X));
					target_y = Double.parseDouble(targetPoint.getAttribute(XML_A_TARGET_Y));

					// Add the arc to the arcs list **************************************
					arcsList.add(new JMTArc(new Point2D.Double(source_x,source_y), pointsList,new Point2D.Double(target_x,target_y)));
				}
				model.setConnectionShape(names.get(source),names.get(target),new JMTPath(arcsList));
			} else {
				System.out.println("Error - Found connection shape info for the connection '" + source + "' to '"+target+"' " +
						"which is not present in the model.");
			}
		}
	}





	/**
	 * Sets gui specific user class parameters
	 * @param root GuiXML document root
	 * @param model data structure where properties have to be set
	 */
	protected static void setUserClasses(Element root, JSimGraphModel model) {
		NodeList classes = root.getElementsByTagName(XML_E_CLASS);
		// Build a map of class name -> class key
		HashMap<String, Object> names = new HashMap<String, Object>();
		Vector<Object> keys = model.getClassKeys();
		for (int i = 0; i < keys.size(); i++) {
			names.put(model.getClassName(keys.get(i)), keys.get(i));
		}

		Element userclass;
		String name, str_color;
		Color color;
		// For each stored class
		for (int i = 0; i < classes.getLength(); i++) {
			userclass = (Element) classes.item(i);
			name = userclass.getAttribute(XML_A_CLASS_NAME);
			str_color = userclass.getAttribute(XML_A_CLASS_COLOR);
			// Parses color string (# + RGB representation in HEX format)
			if (str_color.startsWith("#")) {
				str_color = str_color.substring(1, str_color.length());
			} else {
				System.out.println("Warning - Bad color format for class '" + name + "'.");
			}
			try {
				color = new Color((int) Long.parseLong(str_color, 16), true);
			} catch (NumberFormatException e) {
				System.out.println("Warning - Bad color format for class '" + name + "'. " + "Assign a new color to it.");
				color = model.getNewColor();
			}
			// Sets color for current class only if exists in current model
			if (names.containsKey(name)) {
				model.setClassColor(names.get(name), color);
			} else {
				System.out.println("Warning - Found color info for class '" + name + "' which is not present in the model.");
			}
		}
	}

	/**
	 * Sets gui specific station parameters
	 * @param root GuiXML document root
	 * @param model data structure where properties have to be set
	 */
	protected static void setStations(Element root, JSimGraphModel model) {
		NodeList stations = root.getElementsByTagName(XML_E_STATION);
		// Build a map of station name -> station key
		HashMap<String, Object> names = new HashMap<String, Object>();
		Vector<Object> keys = model.getStationKeys();
		for (int i = 0; i < keys.size(); i++) {
			names.put(model.getStationName(keys.get(i)), keys.get(i));
		}

		Element station, position;
		String name;
		double x, y;
		boolean rotate = false;
		double angle = 0;
		// For each stored Station
		for (int i = 0; i < stations.getLength(); i++) {
			station = (Element) stations.item(i);
			name = station.getAttribute(XML_A_STATION_NAME);
			position = (Element) station.getElementsByTagName(XML_E_POSITION).item(0);
			x = Double.parseDouble(position.getAttribute(XML_A_POSITION_X));
			y = Double.parseDouble(position.getAttribute(XML_A_POSITION_Y));
			if (position.hasAttribute(XML_A_POSITION_ROTATE)) {
				rotate = Boolean.valueOf(position.getAttribute(XML_A_POSITION_ROTATE)).booleanValue();
			}
			if (position.hasAttribute(XML_A_POSITION_ANGLE)) {
				angle = Double.parseDouble(position.getAttribute(XML_A_POSITION_ANGLE));
			}
			if (names.containsKey(name)) {
				model.setStationPosition(names.get(name), new JMTPoint(x, y, rotate, angle));
			} else {
				System.out.println("Error - Found position info for station '" + name + "' which is not present in the model.");
			}
		}
	}

	/**
	 * Sets gui specific parametric analysis parameters
	 * @param root GuiXML document root
	 * @param model data structure where properties have to be set
	 */
	protected static void setParametricAnalysis(Element root, CommonModel model) {
		NodeList temp = root.getElementsByTagName(XML_E_PARAMETRIC);
		if (temp.getLength() != 0) {
			Element parametric = (Element) temp.item(0);
			String enabled = parametric.getAttribute(XML_A_PARAMETRIC_ENABLED);
			if (Boolean.valueOf(enabled).booleanValue()) {
				model.setParametricAnalysisEnabled(true);
				String classPath = parametric.getAttribute(XML_A_PARAMETRIC_CLASSPATH);
				try {
					Class<?>[] paramClasses = { ClassDefinition.class, StationDefinition.class, SimulationDefinition.class };
					Object[] params = { model, model, model };
					ParametricAnalysisDefinition pad = (ParametricAnalysisDefinition) Class.forName(classPath).getConstructor(paramClasses)
							.newInstance(params);
					NodeList fields = parametric.getElementsByTagName(XML_E_FIELD);
					for (int i = 0; i < fields.getLength(); i++) {
						Element field = (Element) fields.item(i);
						String fieldName = field.getAttribute(XML_A_FIELD_NAME);
						String fieldValue = field.getAttribute(XML_A_FIELD_VALUE);
						pad.setProperty(fieldName, fieldValue);
					}
					pad.searchForAvailableSteps();
					pad.createValuesSet();
					model.setParametricAnalysisModel(pad);
				} catch (InvocationTargetException ite) {
					JOptionPane.showMessageDialog(null, "Invocation target exception: " + classPath);
				} catch (NoSuchMethodException nsme) {
					JOptionPane.showMessageDialog(null, "No such method: " + classPath);
				} catch (ClassNotFoundException cnfe) {
					JOptionPane.showMessageDialog(null, "Class not found: " + classPath);
				} catch (InstantiationException ie) {
					JOptionPane.showMessageDialog(null, "Instantiation exception: " + classPath);
				} catch (IllegalAccessException iae) {
					JOptionPane.showMessageDialog(null, "Illegal access exception: " + classPath);
				}
			}
		}
	}

}
