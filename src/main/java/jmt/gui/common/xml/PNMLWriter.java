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

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import ch.qos.logback.classic.LoggerContext;
import fr.lip6.move.pnml.framework.hlapi.HLAPIRootClass;
import fr.lip6.move.pnml.framework.utils.ModelRepository;
import fr.lip6.move.pnml.framework.utils.exception.InvalidIDException;
import fr.lip6.move.pnml.framework.utils.exception.VoidRepositoryException;
import jmt.common.GlobalSettings;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Distribution.Parameter;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;

/**
 * <p>Title: PNML Writer</p>
 * <p>Description: Writes model information to a PNML file. This class provides
 * methods for model export.</p>
 *
 * @author Lulai Zhu
 * Date: 25-01-2017
 * Time: 12.00.00
 */
public class PNMLWriter implements PNMLConstants, CommonConstants {

	public static final int ALL_RIGHT = 0;
	public static final int MULTIPLE_CLASSES = 1;
	public static final int NON_PETRI_NET_STATIONS = 2;

	private static DocumentBuilder docBuilder;
	private static Document elemCreator;
	private static Transformer transformer;

	/**
	 * Checks if an internal model can be exported to a PNML file
	 * @param model data structure of the model
	 * @return integer indicating the result of the check
	 */
	public static int checkModel(CommonModel model) {
		if (model.getClassKeys().size() > 1) {
			return MULTIPLE_CLASSES;
		}
		Vector<Object> stationKeys = model.getStationKeys();
		Vector<Object> placeKeys = model.getStationKeysPlace();
		Vector<Object> transitionKeys = model.getStationKeysTransition();
		if (stationKeys.size() > placeKeys.size() + transitionKeys.size()) {
			return NON_PETRI_NET_STATIONS;
		}
		return ALL_RIGHT;
	}

	/**
	 * Exports an internal model to a PNML file
	 * @param file file that the model is exported to
	 * @param model data structure of the model
	 * @return true if the model was exported, false otherwise
	 */
	public static boolean exportModel(File file, CommonModel model) throws Exception {
		try {
			if (docBuilder == null) {
				docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			}
			if (elemCreator == null) {
				elemCreator = docBuilder.newDocument();
			}
			if (transformer == null) {
				transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}

			ModelRepository.getInstance().createDocumentWorkspace("w-" + UUID.randomUUID().toString());
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			HLAPIRootClass docApi = null;
			String modelName = file.getName().substring(0, file.getName().lastIndexOf(".pnml"));
			int numberOfClasses = model.getClassKeys().size();
			switch (numberOfClasses) {
			case 0:
			{
				docApi = new fr.lip6.move.pnml.pnmlcoremodel.hlapi.PetriNetDocHLAPI();
				writeCoreModel((fr.lip6.move.pnml.pnmlcoremodel.hlapi.PetriNetDocHLAPI) docApi, modelName, model);
				break;
			}
			case 1:
			{
				docApi = new fr.lip6.move.pnml.ptnet.hlapi.PetriNetDocHLAPI();
				writePTNet((fr.lip6.move.pnml.ptnet.hlapi.PetriNetDocHLAPI) docApi, modelName, model);
				break;
			}
			default:
			{
				ModelRepository.getInstance().destroyCurrentWorkspace();
				((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
				return false;
			}
			}

			String pnml = docApi.toPNML();
			Document pnmlDoc = docBuilder.parse(new InputSource(new StringReader(pnml)));
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(pnmlDoc), new StreamResult(file));
			ModelRepository.getInstance().destroyCurrentWorkspace();
			((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
			return true;
		} catch (Exception e) {
			ModelRepository.getInstance().destroyCurrentWorkspace();
			((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
			throw e;
		}
	}

	/**
	 * Writes an internal model to a PNML core model document
	 * @param doc PNML document that the model is written to
	 * @param modelName name of the model
	 * @param model data structure of the model
	 */
	private static void writeCoreModel(fr.lip6.move.pnml.pnmlcoremodel.hlapi.PetriNetDocHLAPI doc,
			String modelName, CommonModel model)
					throws InvalidIDException, VoidRepositoryException, TransformerException {
		fr.lip6.move.pnml.pnmlcoremodel.hlapi.PetriNetHLAPI net =
				new fr.lip6.move.pnml.pnmlcoremodel.hlapi.PetriNetHLAPI(
						"n-" + UUID.randomUUID().toString(),
						fr.lip6.move.pnml.pnmlcoremodel.hlapi.PNTypeHLAPI.COREMODEL, doc);
		new fr.lip6.move.pnml.pnmlcoremodel.hlapi.NameHLAPI(modelName, net);

		fr.lip6.move.pnml.pnmlcoremodel.hlapi.PageHLAPI page =
				new fr.lip6.move.pnml.pnmlcoremodel.hlapi.PageHLAPI(
						"g-" + UUID.randomUUID().toString(), net);
		new fr.lip6.move.pnml.pnmlcoremodel.hlapi.NameHLAPI("top-level", page);

		Map<String, fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeHLAPI> nodeMap =
				new HashMap<String, fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeHLAPI>();

		Vector<Object> placeKeys = model.getStationKeysPlace();
		for (Object pk : placeKeys) {
			fr.lip6.move.pnml.pnmlcoremodel.hlapi.PlaceHLAPI place =
					new fr.lip6.move.pnml.pnmlcoremodel.hlapi.PlaceHLAPI(
							"p-" + UUID.randomUUID().toString(), page);
			new fr.lip6.move.pnml.pnmlcoremodel.hlapi.NameHLAPI(
					model.getStationName(pk), place);
			nodeMap.put(model.getStationName(pk), place);

			if (model instanceof JSimGraphModel) {
				StringWriter writer = new StringWriter();
				appendSpecificNodeGraphfics(writer, (JSimGraphModel) model, pk);
				new fr.lip6.move.pnml.pnmlcoremodel.hlapi.ToolInfoHLAPI(
						PNML_JSIM, GlobalSettings.getSetting(GlobalSettings.VERSION),
						writer.getBuffer(), null, null, place);

				fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeGraphicsHLAPI graphics =
						new fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeGraphicsHLAPI(place);
				new fr.lip6.move.pnml.pnmlcoremodel.hlapi.PositionHLAPI(
						Integer.valueOf((int) ((JSimGraphModel) model).getStationPosition(pk).getX()),
						Integer.valueOf((int) ((JSimGraphModel) model).getStationPosition(pk).getY()),
						graphics);
			}
		}

		Vector<Object> transitionKeys = model.getStationKeysTransition();
		for (Object tk : transitionKeys) {
			fr.lip6.move.pnml.pnmlcoremodel.hlapi.TransitionHLAPI transition =
					new fr.lip6.move.pnml.pnmlcoremodel.hlapi.TransitionHLAPI(
							"t-" + UUID.randomUUID().toString(), page);
			new fr.lip6.move.pnml.pnmlcoremodel.hlapi.NameHLAPI(
					model.getStationName(tk), transition);
			nodeMap.put(model.getStationName(tk), transition);

			if (model instanceof JSimGraphModel) {
				StringWriter writer = new StringWriter();
				appendSpecificNodeGraphfics(writer, (JSimGraphModel) model, tk);
				new fr.lip6.move.pnml.pnmlcoremodel.hlapi.ToolInfoHLAPI(
						PNML_JSIM, GlobalSettings.getSetting(GlobalSettings.VERSION),
						writer.getBuffer(), null, null, transition);

				fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeGraphicsHLAPI graphics =
						new fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeGraphicsHLAPI(transition);
				new fr.lip6.move.pnml.pnmlcoremodel.hlapi.PositionHLAPI(
						Integer.valueOf((int) ((JSimGraphModel) model).getStationPosition(tk).getX()),
						Integer.valueOf((int) ((JSimGraphModel) model).getStationPosition(tk).getY()),
						graphics);
			}
		}

		Vector<Object> nodeKeys = new Vector<Object>();
		nodeKeys.addAll(placeKeys);
		nodeKeys.addAll(transitionKeys);
		for (int i = 0; i < nodeKeys.size(); i++) {
			for (int j = 0; j < nodeKeys.size(); j++) {
				Object sourceKey = nodeKeys.get(i);
				Object targetKey = nodeKeys.get(j);
				if (model.areConnected(sourceKey, targetKey)) {
					String sourceName = model.getStationName(sourceKey);
					String targetName = model.getStationName(targetKey);
					fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeHLAPI source = nodeMap.get(sourceName);
					fr.lip6.move.pnml.pnmlcoremodel.hlapi.NodeHLAPI target = nodeMap.get(targetName);
					new fr.lip6.move.pnml.pnmlcoremodel.hlapi.ArcHLAPI(
							"a-" + UUID.randomUUID().toString(), source, target, page);
				}
			}
		}
	}

	/**
	 * Writes an internal model to a PNML P/T net document
	 * @param doc PNML document that the model is written to
	 * @param modelName name of the model
	 * @param model data structure of the model
	 */
	private static void writePTNet(fr.lip6.move.pnml.ptnet.hlapi.PetriNetDocHLAPI doc,
			String modelName, CommonModel model)
					throws InvalidIDException, VoidRepositoryException, TransformerException {
		fr.lip6.move.pnml.ptnet.hlapi.PetriNetHLAPI net =
				new fr.lip6.move.pnml.ptnet.hlapi.PetriNetHLAPI(
						"n-" + UUID.randomUUID().toString(),
						fr.lip6.move.pnml.ptnet.hlapi.PNTypeHLAPI.PTNET, doc);
		new fr.lip6.move.pnml.ptnet.hlapi.NameHLAPI(modelName, net);
		StringWriter writer = new StringWriter();
		appendSpecificNetParameters(writer, model);
		new fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI(
				PNML_JSIM, GlobalSettings.getSetting(GlobalSettings.VERSION),
				writer.getBuffer(), null, null, net);

		fr.lip6.move.pnml.ptnet.hlapi.PageHLAPI page =
				new fr.lip6.move.pnml.ptnet.hlapi.PageHLAPI(
						"g-" + UUID.randomUUID().toString(), net);
		new fr.lip6.move.pnml.ptnet.hlapi.NameHLAPI("top-level", page);

		Object classKey = model.getClassKeys().get(0);
		Map<String, fr.lip6.move.pnml.ptnet.hlapi.NodeHLAPI> nodeMap =
				new HashMap<String, fr.lip6.move.pnml.ptnet.hlapi.NodeHLAPI>();

		Vector<Object> placeKeys = model.getStationKeysPlace();
		for (Object pk : placeKeys) {
			fr.lip6.move.pnml.ptnet.hlapi.PlaceHLAPI place =
					new fr.lip6.move.pnml.ptnet.hlapi.PlaceHLAPI(
							"p-" + UUID.randomUUID().toString(), page);
			new fr.lip6.move.pnml.ptnet.hlapi.NameHLAPI(
					model.getStationName(pk), place);
			new fr.lip6.move.pnml.ptnet.hlapi.PTMarkingHLAPI(
					Long.valueOf(model.getPreloadedJobs(pk, classKey).longValue()), place);
			nodeMap.put(model.getStationName(pk), place);

			writer = new StringWriter();
			appendSpecificPlaceParameters(writer, model, pk);
			if (model instanceof JSimGraphModel) {
				appendSpecificNodeGraphfics(writer, (JSimGraphModel) model, pk);
			}
			new fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI(
					PNML_JSIM, GlobalSettings.getSetting(GlobalSettings.VERSION),
					writer.getBuffer(), null, null, place);

			if (model instanceof JSimGraphModel) {
				fr.lip6.move.pnml.ptnet.hlapi.NodeGraphicsHLAPI graphics =
						new fr.lip6.move.pnml.ptnet.hlapi.NodeGraphicsHLAPI(place);
				new fr.lip6.move.pnml.ptnet.hlapi.PositionHLAPI(
						Integer.valueOf((int) ((JSimGraphModel) model).getStationPosition(pk).getX()),
						Integer.valueOf((int) ((JSimGraphModel) model).getStationPosition(pk).getY()),
						graphics);
			}
		}

		Vector<Object> transitionKeys = model.getStationKeysTransition();
		for (Object tk : transitionKeys) {
			fr.lip6.move.pnml.ptnet.hlapi.TransitionHLAPI transition =
					new fr.lip6.move.pnml.ptnet.hlapi.TransitionHLAPI(
							"t-" + UUID.randomUUID().toString(), page);
			new fr.lip6.move.pnml.ptnet.hlapi.NameHLAPI(
					model.getStationName(tk), transition);
			nodeMap.put(model.getStationName(tk), transition);

			writer = new StringWriter();
			appendSpecificTransitionParameters(writer, model, tk);
			if (model instanceof JSimGraphModel) {
				appendSpecificNodeGraphfics(writer, (JSimGraphModel) model, tk);
			}
			new fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI(
					PNML_JSIM, GlobalSettings.getSetting(GlobalSettings.VERSION),
					writer.getBuffer(), null, null, transition);

			if (model instanceof JSimGraphModel) {
				fr.lip6.move.pnml.ptnet.hlapi.NodeGraphicsHLAPI graphics =
						new fr.lip6.move.pnml.ptnet.hlapi.NodeGraphicsHLAPI(transition);
				new fr.lip6.move.pnml.ptnet.hlapi.PositionHLAPI(
						Integer.valueOf((int) ((JSimGraphModel) model).getStationPosition(tk).getX()),
						Integer.valueOf((int) ((JSimGraphModel) model).getStationPosition(tk).getY()),
						graphics);
			}
		}

		Vector<Object> nodeKeys = new Vector<Object>();
		nodeKeys.addAll(placeKeys);
		nodeKeys.addAll(transitionKeys);
		for (int i = 0; i < nodeKeys.size(); i++) {
			for (int j = 0; j < nodeKeys.size(); j++) {
				Object sourceKey = nodeKeys.get(i);
				Object targetKey = nodeKeys.get(j);
				if (model.areConnected(sourceKey, targetKey)) {
					int normalInscription = 0;
					int inhibitorInscription = 0;
					if (STATION_TYPE_TRANSITION.equals(model.getStationType(targetKey))) {
						normalInscription = model.getEnablingCondition(targetKey, 0, sourceKey, classKey).intValue();
						inhibitorInscription = model.getInhibitingCondition(targetKey, 0, sourceKey, classKey).intValue();
					} else {
						normalInscription = model.getFiringOutcome(sourceKey, 0, targetKey, classKey).intValue();
					}

					String sourceName = model.getStationName(sourceKey);
					String targetName = model.getStationName(targetKey);
					fr.lip6.move.pnml.ptnet.hlapi.NodeHLAPI source = nodeMap.get(sourceName);
					fr.lip6.move.pnml.ptnet.hlapi.NodeHLAPI target = nodeMap.get(targetName);

					if (normalInscription > 0) {
						fr.lip6.move.pnml.ptnet.hlapi.ArcHLAPI normalArc =
								new fr.lip6.move.pnml.ptnet.hlapi.ArcHLAPI(
										"a-" + UUID.randomUUID().toString(), source, target, page);
						writer = new StringWriter();
						appendSpecificArcParameters(writer, PNML_JSIM_V_ARC_TYPE_NORMAL);
						new fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI(
								PNML_JSIM, GlobalSettings.getSetting(GlobalSettings.VERSION),
								writer.getBuffer(), null, null, normalArc);
						new fr.lip6.move.pnml.ptnet.hlapi.PTArcAnnotationHLAPI(
								Long.valueOf((long) normalInscription), normalArc);
					}

					if (inhibitorInscription > 0) {
						fr.lip6.move.pnml.ptnet.hlapi.ArcHLAPI inhibitorArc =
								new fr.lip6.move.pnml.ptnet.hlapi.ArcHLAPI(
										"a-" + UUID.randomUUID().toString(), source, target, page);
						writer = new StringWriter();
						appendSpecificArcParameters(writer, PNML_JSIM_V_ARC_TYPE_INHIBITOR);
						new fr.lip6.move.pnml.ptnet.hlapi.ToolInfoHLAPI(
								PNML_JSIM, GlobalSettings.getSetting(GlobalSettings.VERSION),
								writer.getBuffer(), null, null, inhibitorArc);
						new fr.lip6.move.pnml.ptnet.hlapi.PTArcAnnotationHLAPI(
								Long.valueOf((long) inhibitorInscription), inhibitorArc);
					}
				}
			}
		}
	}

	/**
	 * Appends the specific graphics of a node to a string writer
	 * @param writer writer that the graphics are appended to
	 * @param model data structure of the model
	 * @param node search key for the node
	 */
	private static void appendSpecificNodeGraphfics(StringWriter writer, JSimGraphModel model, Object node)
			throws TransformerException {
		Element graphics = elemCreator.createElement(PNML_JSIM_E_NODE_GRAPHICS);
		Element rotate = elemCreator.createElement(PNML_JSIM_E_NODE_GRAPHICS_ROTATE);
		String text = String.valueOf(model.getStationPosition(node).isRotate());
		rotate.setTextContent(text);
		graphics.appendChild(rotate);
		transformer.transform(new DOMSource(graphics), new StreamResult(writer));
	}

	/**
	 * Appends the specific parameters of a net to a string writer
	 * @param writer writer that the parameters are appended to
	 * @param model data structure of the model
	 */
	private static void appendSpecificNetParameters(StringWriter writer, CommonModel model)
			throws TransformerException {
		Element tokens = elemCreator.createElement(PNML_JSIM_E_NET_TOKENS);
		Object classKey = model.getClassKeys().get(0);
		Element name = elemCreator.createElement(PNML_JSIM_E_NET_TOKENS_CLASS_NAME);
		String text = model.getClassName(classKey);
		name.setTextContent(text);
		tokens.appendChild(name);

		Element type = elemCreator.createElement(PNML_JSIM_E_NET_TOKENS_CLASS_TYPE);
		if (model.getClassType(classKey) == CLASS_TYPE_OPEN) {
			text = PNML_JSIM_V_NET_TOKENS_CLASS_TYPE_OPEN;
		} else {
			text = PNML_JSIM_V_NET_TOKENS_CLASS_TYPE_CLOSED;
		}
		type.setTextContent(text);
		tokens.appendChild(type);

		Object nodeKey = model.getClassRefStation(classKey);
		if (nodeKey != null) {
			Element referenceNode = elemCreator.createElement(PNML_JSIM_E_NET_TOKENS_REFERENCE_NODE);
			if (STATION_TYPE_TRANSITION.equals(nodeKey)) {
				text = STATION_TYPE_TRANSITION;
			} else {
				text = model.getStationName(nodeKey);
			}
			referenceNode.setTextContent(text);
			tokens.appendChild(referenceNode);
		}

		if (model instanceof JSimGraphModel) {
			Element graphics = elemCreator.createElement(PNML_JSIM_E_NET_TOKENS_GRAPHICS);
			Element color = elemCreator.createElement(PNML_JSIM_E_NET_TOKENS_GRAPHICS_COLOR);
			text = "#" + Integer.toHexString(((JSimGraphModel) model).getClassColor(classKey).getRGB()).toUpperCase();
			color.setTextContent(text);
			graphics.appendChild(color);
			tokens.appendChild(graphics);
		}
		transformer.transform(new DOMSource(tokens), new StreamResult(writer));
	}

	/**
	 * Appends the specific parameters of a place to a string writer
	 * @param writer writer that the parameters are appended to
	 * @param model data structure of the model
	 * @param place search key for the place
	 */
	private static void appendSpecificPlaceParameters(StringWriter writer, CommonModel model, Object place)
			throws TransformerException {
		Element capacity = elemCreator.createElement(PNML_JSIM_E_PLACE_CAPACITY);
		Object classKey = model.getClassKeys().get(0);
		String text = String.valueOf(model.getQueueCapacity(place, classKey));
		capacity.setTextContent(text);
		transformer.transform(new DOMSource(capacity), new StreamResult(writer));
	}

	/**
	 * Appends the specific parameters of a transition to a string writer
	 * @param writer writer that the parameters are appended to
	 * @param model data structure of the model
	 * @param transition search key for the transition
	 */
	private static void appendSpecificTransitionParameters(StringWriter writer, CommonModel model, Object transition)
			throws TransformerException {
		Element numberOfServers = elemCreator.createElement(PNML_JSIM_E_TRANSITION_NUMBER_OF_SERVERS);
		String text = String.valueOf(model.getNumberOfServers(transition, 0));
		numberOfServers.setTextContent(text);
		transformer.transform(new DOMSource(numberOfServers), new StreamResult(writer));

		Object strategy = model.getFiringTimeDistribution(transition, 0);
		if (strategy instanceof Distribution) {
			Element timingStrategy = elemCreator.createElement(PNML_JSIM_E_TRANSITION_TIMING_STRATEGY);
			text = PNML_JSIM_V_TRANSITION_TIMING_STRATEGY_TIMED;
			timingStrategy.setTextContent(text);
			transformer.transform(new DOMSource(timingStrategy), new StreamResult(writer));

			Element firingTimeDistribution = elemCreator.createElement(PNML_JSIM_E_TRANSITION_FIRING_TIME_DISTRIBUTION);
			Element distribution = createDistribution((Distribution) strategy);
			firingTimeDistribution.appendChild(distribution);
			transformer.transform(new DOMSource(firingTimeDistribution), new StreamResult(writer));
		} else {
			Element timingStrategy = elemCreator.createElement(PNML_JSIM_E_TRANSITION_TIMING_STRATEGY);
			text = PNML_JSIM_V_TRANSITION_TIMING_STRATEGY_IMMEDIATE;
			timingStrategy.setTextContent(text);
			transformer.transform(new DOMSource(timingStrategy), new StreamResult(writer));

			Element firingPriority = elemCreator.createElement(PNML_JSIM_E_TRANSITION_FIRING_PRIORITY);
			text = String.valueOf(model.getFiringPriority(transition, 0));
			firingPriority.setTextContent(text);
			transformer.transform(new DOMSource(firingPriority), new StreamResult(writer));

			Element firingWeight = elemCreator.createElement(PNML_JSIM_E_TRANSITION_FIRING_WEIGHT);
			text = String.valueOf(model.getFiringWeight(transition, 0));
			firingWeight.setTextContent(text);
			transformer.transform(new DOMSource(firingWeight), new StreamResult(writer));
		}
	}

	/**
	 * Appends the specific parameters of an arc to a string writer
	 * @param writer writer that the parameters are appended to
	 * @param text text specifying the type of the arc
	 */
	private static void appendSpecificArcParameters(StringWriter writer, String text)
			throws TransformerException {
		Element type = elemCreator.createElement(PNML_JSIM_E_ARC_TYPE);
		type.setTextContent(text);
		transformer.transform(new DOMSource(type), new StreamResult(writer));
	}

	/**
	 * Creates an element representing a distribution
	 * @param d data structure of the distribution
	 * @return element representing the distribution
	 */
	private static Element createDistribution(Distribution d) {
		Element distribution = elemCreator.createElement(PNML_JSIM_E_DISTRIBUTION);
		Element type = elemCreator.createElement(PNML_JSIM_E_DISTRIBUTION_TYPE);
		type.setTextContent(d.getName());
		distribution.appendChild(type);

		for (int i = 0; i < d.getNumberOfParameters(); i++) {
			Parameter p = d.getParameter(i);
			Object v = p.getValue();
			if (v != null) {
				Element parameter = elemCreator.createElement(PNML_JSIM_E_DISTRIBUTION_PARAMETER);
				Element name = elemCreator.createElement(PNML_JSIM_E_DISTRIBUTION_PARAMETER_NAME);
				name.setTextContent(p.getName());
				parameter.appendChild(name);
				Element value = elemCreator.createElement(PNML_JSIM_E_DISTRIBUTION_PARAMETER_VALUE);
				if (v instanceof Distribution) {
					value.appendChild(createDistribution((Distribution) v));
				} else if (v instanceof Object[][]) {
					Object[][] m = (Object[][]) v;
					Element matrix = elemCreator.createElement(PNML_JSIM_E_MATRIX);
					for (int j = 0; j < m.length; j++) {
						Element vector = elemCreator.createElement(PNML_JSIM_E_MATRIX_VECTOR);
						for (int k = 0; k < m[j].length; k++) {
							Element entry = elemCreator.createElement(PNML_JSIM_E_MATRIX_VECTOR_ENTRY);
							entry.setTextContent(m[j][k].toString());
							vector.appendChild(entry);
						}
						matrix.appendChild(vector);
					}
					value.appendChild(matrix);
				} else {
					value.setTextContent(v.toString());
				}
				parameter.appendChild(value);
				distribution.appendChild(parameter);
			}
		}
		return distribution;
	}

}
