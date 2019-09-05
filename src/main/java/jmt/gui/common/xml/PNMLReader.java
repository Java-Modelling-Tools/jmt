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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.xerces.parsers.DOMParser;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ch.qos.logback.classic.LoggerContext;
import fr.lip6.move.pnml.framework.general.PNType;
import fr.lip6.move.pnml.framework.hlapi.HLAPIRootClass;
import fr.lip6.move.pnml.framework.utils.PNMLUtils;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Distribution.Parameter;
import jmt.gui.common.serviceStrategies.ZeroStrategy;
import jmt.gui.jsimgraph.definitions.JMTPoint;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;

/**
 * <p>Title: PNML Reader</p>
 * <p>Description: Reads model information from a PNML file. This class provides
 * methods for model import.</p>
 *
 * @author Lulai Zhu
 * Date: 25-01-2017
 * Time: 12.00.00
 */
public class PNMLReader implements PNMLConstants, CommonConstants {

	private static DOMParser parser;
	private static Map<String, Distribution> distributionMap;

	private static String referenceNodeName;
	private static String arcType;

	/**
	 * Imports an external model from a PNML file
	 * @param file file that the model is imported from
	 * @param model data structure of the model
	 * @return true if the model was imported, false otherwise
	 */
	public static boolean importModel(File file, CommonModel model) throws Exception {
		try {
			if (parser == null) {
				parser = new DOMParser();
			}
			if (distributionMap == null) {
				distributionMap = new TreeMap<String, Distribution>();
				Distribution[] all = Distribution.findAll();
				for (Distribution d : all) {
					distributionMap.put(d.getName(), d);
				}
			}

			HLAPIRootClass docApi = PNMLUtils.importPnmlDocument(file, false);
			PNType type = PNMLUtils.determinePNType(docApi);
			switch (type) {
			case COREMODEL:
				readCoreModel((fr.lip6.move.pnml.pnmlcoremodel.hlapi.PetriNetDocHLAPI) docApi, model);
				break;
			case PTNET:
				readPTNet((fr.lip6.move.pnml.ptnet.hlapi.PetriNetDocHLAPI) docApi, model);
				break;
			default:
				((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
				return false;
			}
			((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
			return true;
		} catch (Exception e) {
			((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
			throw e;
		}
	}

	/**
	 * Reads an external model from a PNML core model document
	 * @param doc PNML document that the model is read from
	 * @param model data structure of the model
	 */
	private static void readCoreModel(fr.lip6.move.pnml.pnmlcoremodel.hlapi.PetriNetDocHLAPI doc, CommonModel model)
			throws SAXException, IOException {
		fr.lip6.move.pnml.pnmlcoremodel.hlapi.PetriNetHLAPI net = doc.getNetsHLAPI().get(0);
		fr.lip6.move.pnml.pnmlcoremodel.hlapi.PageHLAPI page = net.getPagesHLAPI().get(0);
		List<fr.lip6.move.pnml.pnmlcoremodel.hlapi.PlaceHLAPI> places = page.getObjects_PlaceHLAPI();
		List<fr.lip6.move.pnml.pnmlcoremodel.hlapi.TransitionHLAPI> transitions = page.getObjects_TransitionHLAPI();
		List<fr.lip6.move.pnml.pnmlcoremodel.hlapi.ArcHLAPI> arcs = page.getObjects_ArcHLAPI();

		Map<String, Object> nodeMap = new HashMap<String, Object>();
		for (fr.lip6.move.pnml.pnmlcoremodel.hlapi.PlaceHLAPI p : places) {
			String name = (p.getName() != null) ? p.getName().getText() : p.getId();
			Object key = model.addStation(name, STATION_TYPE_PLACE);
			nodeMap.put(p.getId(), key);

			if (model instanceof JSimGraphModel) {
				List<fr.lip6.move.pnml.pnmlcoremodel.hlapi.ToolInfoHLAPI> specifics = p.getToolspecificsHLAPI();
				for (fr.lip6.move.pnml.pnmlcoremodel.hlapi.ToolInfoHLAPI s : specifics) {
					Element elem = createSpecificRootElement(s);
					if (PNML_JSIM.equals(s.getTool())) {
						parseSpecificNodeGraphics(elem, (JSimGraphModel) model, key);
					}
				}

				fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeGraphicsHLAPI graphics = p.getNodegraphicsHLAPI();
				if (graphics != null) {
					JMTPoint point = ((JSimGraphModel) model).getStationPosition(key);
					if (point == null) {
						point = new JMTPoint(0, 0, false);
						((JSimGraphModel) model).setStationPosition(key, point);
					}
					point.setX(graphics.getPositionHLAPI().getX().doubleValue());
					point.setY(graphics.getPositionHLAPI().getY().doubleValue());
				}
			}
		}

		for (fr.lip6.move.pnml.pnmlcoremodel.hlapi.TransitionHLAPI t : transitions) {
			String name = (t.getName() != null) ? t.getName().getText() : t.getId();
			Object key = model.addStation(name, STATION_TYPE_TRANSITION);
			nodeMap.put(t.getId(), key);

			if (model instanceof JSimGraphModel) {
				List<fr.lip6.move.pnml.pnmlcoremodel.hlapi.ToolInfoHLAPI> specifics = t.getToolspecificsHLAPI();
				for (fr.lip6.move.pnml.pnmlcoremodel.hlapi.ToolInfoHLAPI s : specifics) {
					Element elem = createSpecificRootElement(s);
					if (PNML_JSIM.equals(s.getTool())) {
						parseSpecificNodeGraphics(elem, (JSimGraphModel) model, key);
					}
				}

				fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeGraphicsHLAPI graphics = t.getNodegraphicsHLAPI();
				if (graphics != null) {
					JMTPoint point = ((JSimGraphModel) model).getStationPosition(key);
					if (point == null) {
						point = new JMTPoint(0, 0, false);
						((JSimGraphModel) model).setStationPosition(key, point);
					}
					point.setX(graphics.getPositionHLAPI().getX().doubleValue());
					point.setY(graphics.getPositionHLAPI().getY().doubleValue());
				}
			}
		}

		for (fr.lip6.move.pnml.pnmlcoremodel.hlapi.ArcHLAPI a : arcs) {
			Object sourceKey = nodeMap.get(a.getSourceHLAPI().getId());
			Object targetKey = nodeMap.get(a.getTargetHLAPI().getId());
			if (sourceKey != null && targetKey != null) {
				model.setConnected(sourceKey, targetKey, true);
			}
		}
	}

	/**
	 * Reads an external model from a PNML P/T net document
	 * @param doc PNML document that the model is read from
	 * @param model data structure of the model
	 */
	private static void readPTNet(fr.lip6.move.pnml.ptnet.hlapi.PetriNetDocHLAPI doc, CommonModel model)
			throws SAXException, IOException {
		fr.lip6.move.pnml.ptnet.hlapi.PetriNetHLAPI net = doc.getNetsHLAPI().get(0);
		fr.lip6.move.pnml.ptnet.hlapi.PageHLAPI page = net.getPagesHLAPI().get(0);
		List<fr.lip6.move.pnml.ptnet.hlapi.PlaceHLAPI> places = page.getObjects_PlaceHLAPI();
		List<fr.lip6.move.pnml.ptnet.hlapi.TransitionHLAPI> transitions = page.getObjects_TransitionHLAPI();
		List<fr.lip6.move.pnml.ptnet.hlapi.ArcHLAPI> arcs = page.getObjects_ArcHLAPI();

		Object classKey = model.addClass("Token", Defaults.getAsInteger("classType").intValue(),
				Defaults.getAsInteger("classPriority"), Defaults.getAsInteger("classPopulation"),
				Defaults.getAsNewInstance("classDistribution"));
		referenceNodeName = null;
		List<fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI> specifics = net.getToolspecificsHLAPI();
		for (fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI s : specifics) {
			Element elem = createSpecificRootElement(s);
			if (PNML_JSIM.equals(s.getTool())) {
				parseSpecificNetParameters(elem, model);
			}
		}
		if (model.getClassType(classKey) == CLASS_TYPE_CLOSED) {
			int population = 0;
			for (fr.lip6.move.pnml.ptnet.hlapi.PlaceHLAPI p : places) {
				population += (p.getInitialMarking() != null) ? p.getInitialMarking().getText().intValue() : 0;
			}
			model.setClassPopulation(classKey, Integer.valueOf(population));
		}

		Map<String, Object> nodeMap = new HashMap<String, Object>();
		for (fr.lip6.move.pnml.ptnet.hlapi.PlaceHLAPI p : places) {
			String name = (p.getName() != null) ? p.getName().getText() : p.getId();
			Object key = model.addStation(name, STATION_TYPE_PLACE);
			nodeMap.put(p.getId(), key);

			int marking = (p.getInitialMarking() != null) ? p.getInitialMarking().getText().intValue() : 0;
			model.setPreloadedJobs(key, classKey, Integer.valueOf(marking));

			specifics = p.getToolspecificsHLAPI();
			for (fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI s : specifics) {
				Element elem = createSpecificRootElement(s);
				if (PNML_JSIM.equals(s.getTool())) {
					parseSpecificPlaceParameters(elem, model, key);
					if (model instanceof JSimGraphModel) {
						parseSpecificNodeGraphics(elem, (JSimGraphModel) model, key);
					}
				}
			}

			if (model instanceof JSimGraphModel) {
				fr.lip6.move.pnml.ptnet.hlapi.NodeGraphicsHLAPI graphics = p.getNodegraphicsHLAPI();
				if (graphics != null) {
					JMTPoint point = ((JSimGraphModel) model).getStationPosition(key);
					if (point == null) {
						point = new JMTPoint(0, 0, false);
						((JSimGraphModel) model).setStationPosition(key, point);
					}
					point.setX(graphics.getPositionHLAPI().getX().doubleValue());
					point.setY(graphics.getPositionHLAPI().getY().doubleValue());
				}
			}
		}

		for (fr.lip6.move.pnml.ptnet.hlapi.TransitionHLAPI t : transitions) {
			String name = (t.getName() != null) ? t.getName().getText() : t.getId();
			Object key = model.addStation(name, STATION_TYPE_TRANSITION);
			nodeMap.put(t.getId(), key);

			specifics = t.getToolspecificsHLAPI();
			for (fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI s : specifics) {
				Element elem = createSpecificRootElement(s);
				if (PNML_JSIM.equals(s.getTool())) {
					parseSpecificTransitionParameters(elem, model, key);
					if (model instanceof JSimGraphModel) {
						parseSpecificNodeGraphics(elem, (JSimGraphModel) model, key);
					}
				}
			}

			if (model instanceof JSimGraphModel) {
				fr.lip6.move.pnml.ptnet.hlapi.NodeGraphicsHLAPI graphics = t.getNodegraphicsHLAPI();
				if (graphics != null) {
					JMTPoint point = ((JSimGraphModel) model).getStationPosition(key);
					if (point == null) {
						point = new JMTPoint(0, 0, false);
						((JSimGraphModel) model).setStationPosition(key, point);
					}
					point.setX(graphics.getPositionHLAPI().getX().doubleValue());
					point.setY(graphics.getPositionHLAPI().getY().doubleValue());
				}
			}
		}

		if (STATION_TYPE_TRANSITION.equals(referenceNodeName)) {
			model.setClassRefStation(classKey, STATION_TYPE_TRANSITION);
		} else {
			model.setClassRefStation(classKey, model.getStationByName(referenceNodeName));
		}

		for (fr.lip6.move.pnml.ptnet.hlapi.ArcHLAPI a : arcs) {
			Object sourceKey = nodeMap.get(a.getSourceHLAPI().getId());
			Object targetKey = nodeMap.get(a.getTargetHLAPI().getId());
			if (sourceKey != null && targetKey != null) {
				model.setConnected(sourceKey, targetKey, true);

				arcType = PNML_JSIM_V_ARC_TYPE_NORMAL;
				specifics = a.getToolspecificsHLAPI();
				for (fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI s : specifics) {
					Element elem = createSpecificRootElement(s);
					if (PNML_JSIM.equals(s.getTool())) {
						parseSpecificArcParameters(elem);
					}
				}

				int inscription = (a.getInscriptionHLAPI() != null) ? a.getInscriptionHLAPI().getText().intValue() : 1;
				if (STATION_TYPE_TRANSITION.equals(model.getStationType(targetKey))) {
					if (PNML_JSIM_V_ARC_TYPE_NORMAL.equals(arcType)) {
						model.setEnablingCondition(targetKey, 0, sourceKey, classKey, Integer.valueOf(inscription));
					} else {
						model.setInhibitingCondition(targetKey, 0, sourceKey, classKey, Integer.valueOf(inscription));
					}
				} else {
					model.setFiringOutcome(sourceKey, 0, targetKey, classKey, Integer.valueOf(inscription));
				}
			}
		}
	}

	/**
	 * Creates an root element representing a specific
	 * @param specific data structure of the specific
	 * @return element representing the specific
	 */
	private static Element createSpecificRootElement(fr.lip6.move.pnml.pnmlcoremodel.hlapi.ToolInfoHLAPI specific)
			throws SAXException, IOException {
		StringBuffer buffer = specific.getFormattedXMLBuffer();
		parser.parse(new InputSource(new StringReader(buffer.toString())));
		return parser.getDocument().getDocumentElement();
	}

	/**
	 * Creates an root element representing a specific
	 * @param specific data structure of the specific
	 * @return element representing the specific
	 */
	private static Element createSpecificRootElement(fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI specific)
			throws SAXException, IOException {
		StringBuffer buffer = specific.getFormattedXMLBuffer();
		parser.parse(new InputSource(new StringReader(buffer.toString())));
		return parser.getDocument().getDocumentElement();
	}

	/**
	 * Parses the specific graphics of a node from a root element
	 * @param specific specific that the graphics are parsed from
	 * @param model data structure of the model
	 * @param node search key for the node
	 */
	private static void parseSpecificNodeGraphics(Element elem, JSimGraphModel model, Object node) {
		Element graphics = getFirstChildElementByTagName(elem, PNML_JSIM_E_NODE_GRAPHICS);
		if (graphics != null) {
			Element rotate = getFirstChildElementByTagName(graphics, PNML_JSIM_E_NODE_GRAPHICS_ROTATE);
			if (rotate != null) {
				JMTPoint point = ((JSimGraphModel) model).getStationPosition(node);
				if (point == null) {
					point = new JMTPoint(0, 0, false);
					((JSimGraphModel) model).setStationPosition(node, point);
				}
				point.setRotate(Boolean.parseBoolean(rotate.getTextContent()));
			}
		}
	}

	/**
	 * Parses the specific parameters of a net from a root element
	 * @param elem element that the parameters are parsed from
	 * @param model data structure of the model
	 */
	private static void parseSpecificNetParameters(Element elem, CommonModel model) {
		Object classKey = model.getClassKeys().get(0);
		Element tokens = getFirstChildElementByTagName(elem, PNML_JSIM_E_NET_TOKENS);
		if (tokens != null) {
			Element name = getFirstChildElementByTagName(tokens, PNML_JSIM_E_NET_TOKENS_CLASS_NAME);
			if (name != null) {
				model.setClassName(classKey, name.getTextContent());
			}

			Element type = getFirstChildElementByTagName(tokens, PNML_JSIM_E_NET_TOKENS_CLASS_TYPE);
			if (type != null) {
				if (PNML_JSIM_V_NET_TOKENS_CLASS_TYPE_OPEN.equals(type.getTextContent())) {
					model.setClassType(classKey, CLASS_TYPE_OPEN);
				} else {
					model.setClassType(classKey, CLASS_TYPE_CLOSED);
				}
			}

			Element referenceNode = getFirstChildElementByTagName(tokens, PNML_JSIM_E_NET_TOKENS_REFERENCE_NODE);
			if (referenceNode != null) {
				referenceNodeName = referenceNode.getTextContent();
			}

			if (model instanceof JSimGraphModel) {
				Element graphics = getFirstChildElementByTagName(tokens, PNML_JSIM_E_NET_TOKENS_GRAPHICS);
				if (graphics != null) {
					Element color = getFirstChildElementByTagName(graphics, PNML_JSIM_E_NET_TOKENS_GRAPHICS_COLOR);
					if (color != null) {
						String rgbaCode = color.getTextContent();
						if (rgbaCode.startsWith("#")) {
							rgbaCode = rgbaCode.substring(1, rgbaCode.length());
						}
						int rgbaValue = (int) Long.parseLong(rgbaCode, 16);
						((JSimGraphModel) model).setClassColor(classKey, new Color(rgbaValue, true));
					}
				}
			}
		}
	}

	/**
	 * Parses the specific parameters of a place from a root element
	 * @param elem element that the parameters are parsed from
	 * @param model data structure of the model
	 * @param place search key for the place
	 */
	private static void parseSpecificPlaceParameters(Element elem, CommonModel model, Object place) {
		Object classKey = model.getClassKeys().get(0);
		Element capacity = getFirstChildElementByTagName(elem, PNML_JSIM_E_PLACE_CAPACITY);
		if (capacity != null) {
			model.setQueueCapacity(place, classKey, Integer.valueOf(capacity.getTextContent()));
		}
	}

	/**
	 * Parses the specific parameters of a transition from a root element
	 * @param elem element that the parameters are parsed from
	 * @param model data structure of the model
	 * @param transition search key for the transition
	 */
	private static void parseSpecificTransitionParameters(Element elem, CommonModel model, Object transition) {
		Element numberOfServers = getFirstChildElementByTagName(elem, PNML_JSIM_E_TRANSITION_NUMBER_OF_SERVERS);
		if (numberOfServers != null) {
			model.setNumberOfServers(transition, 0, Integer.valueOf(numberOfServers.getTextContent()));
		}

		Element timingStrategy = getFirstChildElementByTagName(elem, PNML_JSIM_E_TRANSITION_TIMING_STRATEGY);
		if (timingStrategy != null) {
			if (PNML_JSIM_V_TRANSITION_TIMING_STRATEGY_TIMED.equals(timingStrategy.getTextContent())) {
				Element firingTimeDistribution = getFirstChildElementByTagName(elem, PNML_JSIM_E_TRANSITION_FIRING_TIME_DISTRIBUTION);
				if (firingTimeDistribution != null) {
					Element distribution = getFirstChildElementByTagName(firingTimeDistribution, PNML_JSIM_E_DISTRIBUTION);
					model.setFiringTimeDistribution(transition, 0, parseDistribution(distribution));
				}

				model.setFiringPriority(transition, 0, Integer.valueOf(-1));
				model.setFiringWeight(transition, 0, Double.valueOf(1.0));
			} else {
				model.setFiringTimeDistribution(transition, 0, new ZeroStrategy());

				Element firingPriority = getFirstChildElementByTagName(elem, PNML_JSIM_E_TRANSITION_FIRING_PRIORITY);
				if (firingPriority != null) {
					model.setFiringPriority(transition, 0, Integer.valueOf(firingPriority.getTextContent()));
				} else {
					model.setFiringPriority(transition, 0, Defaults.getAsInteger("transitionFiringPriority"));
				}

				Element firingWeight = getFirstChildElementByTagName(elem, PNML_JSIM_E_TRANSITION_FIRING_WEIGHT);
				if (firingWeight != null) {
					model.setFiringWeight(transition, 0, Double.valueOf(firingWeight.getTextContent()));
				} else {
					model.setFiringWeight(transition, 0, Defaults.getAsDouble("transitionFiringWeight"));
				}
			}
		}
	}

	/**
	 * Parses the specific parameters of an arc from a root element
	 * @param elem element that the parameters are parsed from
	 */
	private static void parseSpecificArcParameters(Element elem) {
		Element type = getFirstChildElementByTagName(elem, PNML_JSIM_E_ARC_TYPE);
		if (type != null) {
			arcType = type.getTextContent();
		}
	}

	/**
	 * Parses an element representing a distribution
	 * @param distribution element representing a distribution
	 * @return data structure of the distribution
	 */
	private static Distribution parseDistribution(Element distribution) {
		Element type = getFirstChildElementByTagName(distribution, PNML_JSIM_E_DISTRIBUTION_TYPE);
		Distribution d = distributionMap.get(type.getTextContent()).clone();
		List<Element> parameters = getChildElementsByTagName(distribution, PNML_JSIM_E_DISTRIBUTION_PARAMETER);
		for (int i = 0; i < parameters.size(); i++) {
			Element parameter = parameters.get(i);
			Element name = getFirstChildElementByTagName(parameter, PNML_JSIM_E_DISTRIBUTION_PARAMETER_NAME);
			Parameter p = d.getParameter(name.getTextContent());
			Element value = getFirstChildElementByTagName(parameter, PNML_JSIM_E_DISTRIBUTION_PARAMETER_VALUE);
			Element nestedDistribution = getFirstChildElementByTagName(value, PNML_JSIM_E_DISTRIBUTION);
			Element matrix = getFirstChildElementByTagName(value, PNML_JSIM_E_MATRIX);
			if (nestedDistribution != null) {
				p.setValue(parseDistribution(nestedDistribution));
			} else if (matrix != null) {
				Object[][] m = null;
				List<Element> vectors = getChildElementsByTagName(matrix, PNML_JSIM_E_MATRIX_VECTOR);
				for (int j = 0; j < vectors.size(); j++) {
					if (j == 0) {
						m = new Object[vectors.size()][];
					}
					Element vector = vectors.get(j);
					List<Element> entries = getChildElementsByTagName(vector, PNML_JSIM_E_MATRIX_VECTOR_ENTRY);
					for (int k = 0; k < entries.size(); k++) {
						if (k == 0) {
							m[j] = new Object[entries.size()];
						}
						Element entry = entries.get(k);
						m[j][k] = p.parseValue(entry.getTextContent());
						if (m[j][k] == null) {
							m[j][k] = p.parseValue("0");
						}
					}
				}
				p.setValue(m);
			} else {
				p.setValue(value.getTextContent());
			}
		}
		d.updateCM();
		return d;
	}

	/**
	 * Gets the first child element of an element by a tag name
	 * @param elem element whose child nodes are traversed
	 * @param name name of the tag that is of interest
	 * @return first child element with the tag name
	 */
	private static Element getFirstChildElementByTagName(Element elem, String name) {
		NodeList childNodes = elem.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node cn = childNodes.item(i);
			if (cn.getNodeType() == Node.ELEMENT_NODE && cn.getNodeName().equals(name)) {
				return (Element) cn;
			}
		}
		return null;
	}

	/**
	 * Gets the child elements of an element by a tag name
	 * @param elem element whose child nodes are traversed
	 * @param name name of the tag that is of interest
	 * @return child elements with the tag name
	 */
	private static List<Element> getChildElementsByTagName(Element elem, String name) {
		List<Element> childElems = new ArrayList<Element>();
		NodeList childNodes = elem.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node cn = childNodes.item(i);
			if (cn.getNodeType() == Node.ELEMENT_NODE && cn.getNodeName().equals(name)) {
				childElems.add((Element) cn);
			}
		}
		return childElems;
	}

}
