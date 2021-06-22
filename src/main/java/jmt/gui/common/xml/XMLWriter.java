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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import jmt.engine.NetStrategies.ImpatienceStrategies.*;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.forkStrategies.ProbabilitiesFork;
import jmt.gui.common.forkStrategies.CombFork;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.joinStrategies.NormalJoin;
import jmt.gui.common.joinStrategies.PartialJoin;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jmt.engine.log.LoggerParameters;
import jmt.engine.random.EmpiricalEntry;
import jmt.framework.data.MacroReplacer;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Distribution.Parameter;
import jmt.gui.common.forkStrategies.ClassSwitchFork;
import jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.joinStrategies.NormalJoin;
import jmt.gui.common.joinStrategies.PartialJoin;
import jmt.gui.common.routingStrategies.*;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.serviceStrategies.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import java.io.File;
import java.io.OutputStream;
import java.util.*;

/**
 * <p>Title: XML Writer</p>
 * <p>Description: Writes model information to an XML file. This class provides
 * methods for model save.</p>
 * 
 * @author Bertoli Marco
 *         Date: 15-lug-2005
 *         Time: 10.56.01
 */
public class XMLWriter implements CommonConstants, XMLConstantNames {

	/**
	 * defines matching between gui representation and engine names for queue
	 * strategies, e.g. FCFS = TailStrategy.
	 */
	protected static final Map<String, String> PS_STRATEGIES_MAPPING;
	protected static final Map<String, String> QUEUE_PUT_MAPPING;
	protected static final Map<String, String> PRIORITY_QUEUE_PUT_MAPPING;
	protected static final Map<String, String> DROP_RULES_MAPPING;
	protected static final Map<String, String> POLLING_TYPE_MAPPING;

	static {
		HashMap<String, String> temp = null;

		temp = new HashMap<String, String>();
		temp.put(QUEUE_STRATEGY_PS, "EPSStrategy");
		temp.put(QUEUE_STRATEGY_GPS, "GPSStrategy");
		temp.put(QUEUE_STRATEGY_DPS, "DPSStrategy");
		PS_STRATEGIES_MAPPING = Collections.unmodifiableMap(temp);

		temp = new HashMap<String, String>();
		temp.put(QUEUE_STRATEGY_FCFS, "TailStrategy");
		temp.put(QUEUE_STRATEGY_LCFS, "HeadStrategy");
		temp.put(QUEUE_STRATEGY_LCFS_PR, "LCFSPRStrategy");
		temp.put(QUEUE_STRATEGY_RAND, "RandStrategy");
		temp.put(QUEUE_STRATEGY_SJF, "SJFStrategy");
		temp.put(QUEUE_STRATEGY_LJF, "LJFStrategy");
		temp.put(QUEUE_STRATEGY_SEPT, "SEPTStrategy");
		temp.put(QUEUE_STRATEGY_LEPT, "LEPTStrategy");
		QUEUE_PUT_MAPPING = Collections.unmodifiableMap(temp);

		temp = new HashMap<String, String>();
		temp.put(QUEUE_STRATEGY_FCFS, "TailStrategyPriority");
		temp.put(QUEUE_STRATEGY_LCFS, "HeadStrategyPriority");
		temp.put(QUEUE_STRATEGY_RAND, "RandStrategyPriority");
		temp.put(QUEUE_STRATEGY_SJF, "SJFStrategyPriority");
		temp.put(QUEUE_STRATEGY_LJF, "LJFStrategyPriority");
		temp.put(QUEUE_STRATEGY_SEPT, "SEPTStrategyPriority");
		temp.put(QUEUE_STRATEGY_LEPT, "LEPTStrategyPriority");
		PRIORITY_QUEUE_PUT_MAPPING = Collections.unmodifiableMap(temp);

		temp = new HashMap<String, String>();
		temp.put(FINITE_DROP, "drop");
		temp.put(FINITE_BLOCK, "BAS blocking");
		temp.put(FINITE_WAITING, "waiting queue");
		temp.put(FINITE_RETRIAL, "retrial");
		DROP_RULES_MAPPING = Collections.unmodifiableMap(temp);

		temp = new HashMap<String, String>();
		temp.put(STATION_QUEUE_STRATEGY_POLLING_LIMITED, CLASSNAME_LKPSERVER);
		temp.put(STATION_QUEUE_STRATEGY_POLLING_GATED, CLASSNAME_GATEDPSERVER);
		temp.put(STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE, CLASSNAME_EXHAUSTIVEPSERVER);
		POLLING_TYPE_MAPPING = Collections.unmodifiableMap(temp);
	}

	public static final String strategiesClasspathBase = "jmt.engine.NetStrategies.";
	public static final String psStrategiesSuffix = "PSStrategies.";
	public static final String queueGetStrategiesSuffix = "QueueGetStrategies.";
	public static final String queuePutStrategiesSuffix = "QueuePutStrategies.";
	public static final String serviceStrategiesSuffix = "ServiceStrategies.";
	public static final String routingStrategiesSuffix = "RoutingStrategies.";
	public static final String transitionUtilitiesSuffix = "TransitionUtilities.";
	public static final String distributionContainerClasspath = "jmt.engine.random.DistributionContainer";

	private static final String SERVICE_TIME_STRATEGY_PATH = strategiesClasspathBase + serviceStrategiesSuffix + "ServiceTimeStrategy";
	public static void writeXML(String fileName, CommonModel model) {
		writeToResult(new StreamResult(new File(fileName)), model, fileName);
	}

	public static void writeXML(File xmlFile, CommonModel model) {
		writeToResult(new StreamResult(xmlFile), model, xmlFile.getName());
	}

	public static void writeXML(OutputStream out, CommonModel model) {
		writeToResult(new StreamResult(out), model, "model");
	}

	public static void writeXML(String fileName, Document doc) {
		if (doc == null) {
			return;
		}
		try {
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(doc), new StreamResult(
					new File(fileName)));
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	public static Document getDocument(CommonModel model, String modelName) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		}
		Document modelDoc = docBuilder.newDocument();
		writeModel(modelDoc, model, modelName);
		return modelDoc;
	}

	private static void writeToResult(Result result, CommonModel model,
			String modelName) {
		Document modelDoc = getDocument(model, modelName);
		if (modelDoc == null) {
			return;
		}
		try {
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(modelDoc), result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	static protected void writeModel(Document modelDoc, CommonModel model,
			String modelName) {
		Element elem = modelDoc.createElement(XML_DOCUMENT_ROOT);
		modelDoc.appendChild(elem);
		elem.setAttribute(XML_A_ROOT_NAME, modelName);
		elem.setAttribute("xsi:noNamespaceSchemaLocation", XML_DOCUMENT_XSD);
		elem.setAttribute("xmlns:xsi",
				"http://www.w3.org/2001/XMLSchema-instance");
		// Simulation seed
		if (!model.getUseRandomSeed().booleanValue()) {
			elem.setAttribute(XML_A_ROOT_SEED, model.getSimulationSeed()
					.toString());
		}
		// Max simulation time
		if (model.getMaximumDuration().doubleValue() > 0) {
			elem.setAttribute(XML_A_ROOT_DURATION, model.getMaximumDuration()
					.toString());
		}
		// Max simulated time (not real time, but time in system of the simulated process)
		if (model.getMaxSimulatedTime().doubleValue() > 0) {
			elem.setAttribute(XML_A_ROOT_SIMULATED, model.getMaxSimulatedTime()
					.toString());
		}
		// Polling interval
		elem.setAttribute(XML_A_ROOT_POLLING, model
				.getPollingInterval().toString());
		// Max samples
		elem.setAttribute(XML_A_ROOT_MAXSAMPLES, model
				.getMaxSimulationSamples().toString());
		// Disable statistic
		elem.setAttribute(XML_A_ROOT_DISABLESTATISTIC, model
				.getDisableStatistic().toString());
		// Max events
		elem.setAttribute(XML_A_ROOT_MAXEVENTS, model
				.getMaxSimulationEvents().toString());
		// Write attributes used by the logs - Michael Fercu
		elem.setAttribute(XML_A_ROOT_LOGPATH,
				MacroReplacer.replace(model.getLoggingGlbParameter("path")));
		elem.setAttribute(XML_A_ROOT_LOGREPLACE,
				model.getLoggingGlbParameter("autoAppend"));
		elem.setAttribute(XML_A_ROOT_LOGDELIM,
				model.getLoggingGlbParameter("delim"));
		elem.setAttribute(XML_A_ROOT_LOGDECIMALSEPARATOR,
				model.getLoggingGlbParameter("decimalSeparator"));
		// Manage probabilities
		model.manageProbabilities();
		// Write all elements
		writeClasses(modelDoc, elem, model);
		writeStations(modelDoc, elem, model);
		writeMeasures(modelDoc, elem, model);
		writeConnections(modelDoc, elem, model);
		writeBlockingRegions(modelDoc, elem, model);
		writePreload(modelDoc, elem, model);
	}

	/*-----------------------------------------------------------------------------------
	 *--------------------- Methods for construction of user classes ---------------------
	 *-----------------------------------------------------------------------------------*/

	static protected void writeClasses(Document doc, Node simNode,
			CommonModel model) {
		for (Object classKey : model.getClassKeys()) {
			String classType = (model.getClassType(classKey) == CLASS_TYPE_OPEN) ?
					"open" : "closed";
			Element userClass = doc.createElement(XML_E_CLASS);
			String[] attrsNames = new String[] { XML_A_CLASS_NAME,
					XML_A_CLASS_TYPE, XML_A_CLASS_PRIORITY,
					XML_A_CLASS_CUSTOMERS, XML_A_CLASS_REFSOURCE };
			String[] attrsValues = new String[] { model.getClassName(classKey),
					classType,
					String.valueOf(model.getClassPriority(classKey)),
					String.valueOf(model.getClassPopulation(classKey)),
					getSourceNameForClass(classKey, doc, model) };
			for (int j = 0; j < attrsNames.length; j++) {
				if (attrsValues[j] != null && !"null".equals(attrsValues[j])) {
					userClass.setAttribute(attrsNames[j], attrsValues[j]);
				}
			}
			simNode.appendChild(userClass);
		}
	}

	/**
	 * This method returns the name of the reference source for a class to be
	 * inserted into userclass element's attribute.
	 */
	static protected String getSourceNameForClass(Object classKey,
			Document doc, CommonModel model) {
		Object refStationKey = model.getClassRefStation(classKey);
		if (STATION_TYPE_FORK.equals(refStationKey)
				|| STATION_TYPE_CLASSSWITCH.equals(refStationKey)
				|| STATION_TYPE_SCALER.equals(refStationKey)
				|| STATION_TYPE_TRANSITION.equals(refStationKey)) {
			return (String) refStationKey;
		} else {
			return model.getStationName(model.getClassRefStation(classKey));
		}
	}

	/*-----------------------------------------------------------------------------------
	 *----------------------- Methods for construction of stations -----------------------
	 *-----------------------------------------------------------------------------------*/

	static protected void writeStations(Document doc, Node simNode,
			CommonModel model) {
		Vector<Object> stations = model.getStationKeys();
		Element elem;
		for (int i = 0; i < stations.size(); i++) {
			elem = doc.createElement(XML_E_STATION);
			elem.setAttribute(XML_A_STATION_NAME,
					model.getStationName(stations.get(i)));
			Object stationKey = stations.get(i);
			String stationType = model.getStationType(stationKey);
			if (STATION_TYPE_SOURCE.equals(stationType)) {
				writeSourceSection(doc, elem, model, stationKey);
				writeTunnelSection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_SINK.equals(stationType)) {
				writeSinkSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_TERMINAL.equals(stationType)) {
				writeTerminalSection(doc, elem, model, stationKey);
				writeTunnelSection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_ROUTER.equals(stationType)) {
				writeQueueSection(doc, elem, model, stationKey);
				writeTunnelSection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_DELAY.equals(stationType)) {
				writeQueueSection(doc, elem, model, stationKey);
				writeDelaySection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_SERVER.equals(stationType)) {
				writeQueueSection(doc, elem, model, stationKey);
				writeServerSection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_FORK.equals(stationType)) {
				writeQueueSection(doc, elem, model, stationKey);
				writeTunnelSection(doc, elem, model, stationKey);
				writeForkSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_JOIN.equals(stationType)) {
				writeJoinSection(doc, elem, model, stationKey);
				writeTunnelSection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_LOGGER.equals(stationType)) {
				writeQueueSection(doc, elem, model, stationKey);
				writeLoggerSection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_CLASSSWITCH.equals(stationType)) {
				writeQueueSection(doc, elem, model, stationKey);
				writeClassSwitchSection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_SEMAPHORE.equals(stationType)) {
				writeSemaphoreSection(doc, elem, model, stationKey);
				writeTunnelSection(doc, elem, model, stationKey);
				writeRouterSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_SCALER.equals(stationType)) {
				writeJoinSection(doc, elem, model, stationKey);
				writeTunnelSection(doc, elem, model, stationKey);
				writeForkSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_PLACE.equals(stationType)) {
				writeStorageSection(doc, elem, model, stationKey);
				writeTunnelSection(doc, elem, model, stationKey);
				writeLinkageSection(doc, elem, model, stationKey);
			} else if (STATION_TYPE_TRANSITION.equals(stationType)) {
				writeEnablingSection(doc, elem, model, stationKey);
				writeTimingSection(doc, elem, model, stationKey);
				writeFiringSection(doc, elem, model, stationKey);
			}
			simNode.appendChild(elem);
		}
	}

	/**
	 * Writes a Fork output section <br>
	 * Author: Bertoli Marco
	 *
	 * @param doc
	 *            document root
	 * @param node
	 *            node where created section should be appended
	 * @param model
	 *            data structure
	 * @param stationKey
	 *            search's key for fork
	 */
	private static void writeForkSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element fork = doc.createElement(XML_E_STATION_SECTION);
		fork.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_FORK);
		node.appendChild(fork);
		// Creates jobsPerLink parameter
		XMLParameter jobsPerLink = new XMLParameter("jobsPerLink", model
				.getStationNumberOfServers(stationKey).getClass().getName(),
				null, model.getStationNumberOfServers(stationKey).toString(),
				false);
		// Creates block parameter
		XMLParameter block = new XMLParameter("block", model
				.getForkBlock(stationKey).getClass().getName(), null, model
				.getForkBlock(stationKey).toString(), false);
		// ...and adds them as fork's children
		XMLParameter isSim = new XMLParameter("isSimplifiedFork", model
				.getIsSimplifiedFork(stationKey).getClass().getName(), null,
				model.getIsSimplifiedFork(stationKey).toString(), false);
		jobsPerLink.appendParameterElement(doc, fork);
		block.appendParameterElement(doc, fork);
		isSim.appendParameterElement(doc, fork);

		Vector<Object> classes = model.getClassKeys();
		XMLParameter[] forkStrats = new XMLParameter[classes.size()];
		Object currentClass;
		for (int i = 0; i < forkStrats.length; i++) {
			currentClass = classes.get(i);
			forkStrats[i] = ForkStrategyWriter
					.getForkStrategyParameter((ForkStrategy) model
							.getForkStrategy(stationKey, currentClass),
							model, currentClass, stationKey);
		}
		XMLParameter globalFork = new XMLParameter("ForkStrategy",
				strategiesClasspathBase + "ForkStrategy", null, forkStrats,
				false);
		globalFork.appendParameterElement(doc, fork);
	}

	/**
	 * Writes a Join input section <br>
	 * Author: Bertoli Marco
	 *
	 * @param doc
	 *            document root
	 * @param node
	 *            node where created section should be appended
	 * @param model
	 *            data structure
	 * @param stationKey
	 *            search's key for join
	 */
	private static void writeJoinSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element join = doc.createElement(XML_E_STATION_SECTION);
		join.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_JOIN);
		node.appendChild(join);

		Vector<Object> classes = model.getClassKeys();
		XMLParameter[] joinStrats = new XMLParameter[classes.size()];
		Object currentClass;
		for (int i = 0; i < joinStrats.length; i++) {
			currentClass = classes.get(i);
			joinStrats[i] = JoinStrategyWriter.getJoinStrategyParameter(
					(JoinStrategy) model.getJoinStrategy(stationKey,
							currentClass), model, currentClass, stationKey);
		}
		XMLParameter globalJoin = new XMLParameter("JoinStrategy",
				strategiesClasspathBase + "JoinStrategy", null, joinStrats,
				false);
		globalJoin.appendParameterElement(doc, join);
	}

	/**
	 * Writes a Semaphore input section <br>
	 * Author: Vitor S. Lopes
	 *
	 * @param doc
	 *            document root
	 * @param node
	 *            node where created section should be appended
	 * @param model
	 *            data structure
	 * @param stationKey
	 *            search's key for semaphore
	 */
	private static void writeSemaphoreSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element semaphore = doc.createElement(XML_E_STATION_SECTION);
		semaphore.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_SEMAPHORE);
		node.appendChild(semaphore);

		Vector<Object> classes = model.getClassKeys();
		XMLParameter[] semaphoreStrats = new XMLParameter[classes.size()];
		Object currentClass;
		for (int i = 0; i < semaphoreStrats.length; i++) {
			currentClass = classes.get(i);
			semaphoreStrats[i] = SemaphoreStrategyWriter.getSemaphoreStrategyParameter(
					(SemaphoreStrategy) model.getSemaphoreStrategy(stationKey,
							currentClass), model, currentClass, stationKey);
		}
		XMLParameter globalSemaphore = new XMLParameter("SemaphoreStrategy",
				strategiesClasspathBase + "SemaphoreStrategy", null, semaphoreStrats,
				false);
		globalSemaphore.appendParameterElement(doc, semaphore);
	}

	static protected void writeSourceSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_SOURCE);
		node.appendChild(elem);
		Vector<Object> classes = model.getClassKeys();
		// obtain classes that must be generated by this source
		Vector<Object> refClasses = getClassesForSource(model, stationKey);
		XMLParameter[] distrParams = new XMLParameter[classes.size()];
		for (int i = 0; i < distrParams.length; i++) {
			// if current class must be generated by this source
			Object currentClass = classes.get(i);
			if (refClasses.contains(currentClass)) {
				distrParams[i] = DistributionWriter
						.getDistributionParameter((Distribution) model
								.getClassDistribution(currentClass), model,
								currentClass, "ServiceTimeStrategy", SERVICE_TIME_STRATEGY_PATH);
			} else {
				// otherwise write a null parameter
				String name = "ServiceTimeStrategy";
				distrParams[i] = new XMLParameter(name, strategiesClasspathBase
						+ serviceStrategiesSuffix + name,
						model.getClassName(currentClass), "null", true);
			}
		}
		// creating global service strategy parameter
		String gspName = "ServiceStrategy";
		XMLParameter globalStrategyParameter = new XMLParameter(gspName,
				strategiesClasspathBase + gspName, null, distrParams, false);
		// finally, create node from parameters and append it to the section
		// element
		globalStrategyParameter.appendParameterElement(doc, elem);
	}

	static protected void writeSinkSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_SINK);
		node.appendChild(elem);
	}

	static protected void writeQueueSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		// creating element representing queue section
		Element queue = doc.createElement(XML_E_STATION_SECTION);
		queue.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_QUEUE);
		node.appendChild(queue);
		// creating queue inner parameters
		// first create size node element
		String capacity = model.getStationQueueCapacity(stationKey) == null ? "-1"
				: model.getStationQueueCapacity(stationKey).toString();
		XMLParameter size = new XMLParameter("size", "java.lang.Integer", null,
				capacity, false);
		// ...and add it to queue element's children
		size.appendParameterElement(doc, queue);

		String serviceCapacity = model.getStationMaxRunning(stationKey) == null ? "-1"
				: model.getStationMaxRunning(stationKey).toString();
		XMLParameter maxRunning = new XMLParameter("maxRunning", "java.lang.Integer", null,
				serviceCapacity, false);
		// ...and add it to queue element's children
		//maxRunning.appendParameterElement(doc, queue); // uncomment this to save maxRunning

		String servicePre = model.getStationServerPreemptive(stationKey) == null ? "false"
				: model.getStationServerPreemptive(stationKey).toString();
		XMLParameter serverPreemptive = new XMLParameter("serverPreemptive", "java.lang.Boolean", null,
				servicePre, false);



		// Drop policies. They are different for each customer class
		Vector<Object> classes = model.getClassKeys();
		XMLParameter[] dropStrategy = new XMLParameter[classes.size()];
		XMLParameter dropStrategies = new XMLParameter("dropStrategies",
				String.class.getName(), null, dropStrategy, false);
		XMLParameter[] rd = new XMLParameter[classes.size()];
		XMLParameter retrialDistributions = new XMLParameter("retrialDistributions",
																												 strategiesClasspathBase + "ServiceStrategy",
																												 null, rd, false);
		for (int i = 0; i < dropStrategy.length; i++) {
			String strategy = model.getDropRule(stationKey, classes.get(i));
			dropStrategy[i] = new XMLParameter("dropStrategy",
					String.class.getName(), model.getClassName(classes.get(i)),
					DROP_RULES_MAPPING.get(strategy), true);
			rd[i] = DistributionWriter
							.getDistributionParameter((Distribution) model.getRetrialDistribution(stationKey, classes.get(i)),
																				model, classes.get(i), "ServiceTimeStrategy", SERVICE_TIME_STRATEGY_PATH);
		}
		dropStrategies.appendParameterElement(doc, queue);
		retrialDistributions.appendParameterElement(doc, queue);

		/*
		 * queue get strategy, which is fixed to FCFS, as difference between
		 * strategies can be resolved by queue put strategies
		 */

		String getStrategy = "FCFSstrategy";
		Boolean isServerPollingBool = model.getStationPollingServerBoolean(stationKey);
		boolean isServerPolling = isServerPollingBool != null && isServerPollingBool;
		XMLParameter switchoverParam = null;
		if (isServerPolling) {
			if (model.getStationPollingServerType(stationKey).equals(STATION_QUEUE_STRATEGY_POLLING_LIMITED)) {
				getStrategy = "LimitedPollingGetStrategy";
			} else if (model.getStationPollingServerType(stationKey).equals(STATION_QUEUE_STRATEGY_POLLING_GATED)) {
				getStrategy = "GatedPollingGetStrategy";
			} else if (model.getStationPollingServerType(stationKey).equals(STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE)) {
				getStrategy = "ExhaustivePollingGetStrategy";
			}
			switchoverParam = SwitchoverStrategyWriter.getServiceSection(model, stationKey);
			switchoverParam.parameterName = "switchoverStrategy";
			switchoverParam.isSubParameter = true;
		}

		XMLParameter queueGetStrategy = new XMLParameter("FCFSstrategy",
				strategiesClasspathBase + queueGetStrategiesSuffix
				+ getStrategy, null, (String) null, false);

		if (isServerPolling) {
		    XMLParameter pollingParams[] =  {switchoverParam};;
				if (model.getStationPollingServerType(stationKey).equals(STATION_QUEUE_STRATEGY_POLLING_LIMITED)) {
					Integer kValueInt = model.getStationPollingServerKValue(stationKey);
					pollingParams = new XMLParameter[]{
							new XMLParameter("pollingKValue", "java.lang.Integer", null, kValueInt.toString(), true),
							switchoverParam};
				}
			queueGetStrategy = new XMLParameter("FCFSstrategy",
					strategiesClasspathBase + queueGetStrategiesSuffix + getStrategy, null, pollingParams, false);
			queueGetStrategy.parameterArray = "false";

		}

		queueGetStrategy.appendParameterElement(doc, queue);


		/*
		 * At last, queue put parameter, which can be defined differently for
		 * each customer class, so a more complex parameter structure is required
		 */
		XMLParameter[] queuePutStrategy = new XMLParameter[classes.size()];
		XMLParameter queuePutStrategies = new XMLParameter("QueuePutStrategy",
				strategiesClasspathBase + "QueuePutStrategy", null,
				queuePutStrategy, false);
		String queueStrategy = model.getStationQueueStrategy(stationKey);
		for (int i = 0; i < queuePutStrategy.length; i++) {

			boolean ps_strategy =  PS_STRATEGIES_MAPPING.containsKey(model.getQueueStrategy(stationKey, classes.get(i)));
			String strategy = (queueStrategy.equals(STATION_QUEUE_STRATEGY_PSSERVER)
					&& ps_strategy)
					? QUEUE_STRATEGY_FCFS : model.getQueueStrategy(stationKey,
					classes.get(i));
			Map<String, String> strategyMapping = queueStrategy
					.equals(STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY)
					? PRIORITY_QUEUE_PUT_MAPPING : QUEUE_PUT_MAPPING;
			String strategyName = strategyMapping.get(strategy);
			queuePutStrategy[i] = new XMLParameter(strategyName,
					strategiesClasspathBase + queuePutStrategiesSuffix
					+ strategyName, model.getClassName(classes.get(i)),
					(String) null, true);
		}
		queuePutStrategies.appendParameterElement(doc, queue);
		writeImpatienceParameters(doc, model, stationKey, queue, classes);
	}

	/*
	 * Use the model to write the reneging strategies for each station and each class as XML parameters.
	 * Modelled after the "ServiceStrategy" parameter for RandomSource.
	 */
	private static void writeImpatienceParameters(Document doc, CommonModel model, Object stationKey,
			Element queue, Vector<Object> classes) {
		String impatienceClassName = Impatience.class.getSimpleName();
		String impatiencePath = Impatience.class.getCanonicalName();
		XMLParameter[] impatienceParams = new XMLParameter[classes.size()];
		XMLParameter impatienceParam = new XMLParameter(impatienceClassName,
				impatiencePath, null, impatienceParams, false);

		for (int i = 0; i < impatienceParams.length; i++) {
			// Get the current class and use it to retrieve its impatience type
			Object classKey = classes.get(i);
			ImpatienceType impatienceType = model.getImpatienceType(stationKey, classKey);
			if (impatienceType != null) { // handling null pointer
				// Based on the ImpatienceStrategy, retrieve the object from the model and write it to XML
				switch (impatienceType) {
					case RENEGING:
						impatienceParams[i] = createRenegingParameter(model, stationKey, classKey);
						break;

					case BALKING:
						impatienceParams[i] = createBalkingParameter(model, stationKey, classKey, classes.size(), i);
						break;

					default:
						// Write a null parameter when no impatience strategy was selected
						impatienceParams[i] = new XMLParameter(impatienceClassName, impatiencePath,
																									 model.getClassName(classKey), "null", true);
						break;
				}
			} else {
				// Write a null parameter when no impatience strategy was selected
				impatienceParams[i] = new XMLParameter(impatienceClassName, impatiencePath,
																							 model.getClassName(classKey), "null", true);
			}
		}
		// finally, create node from parameters and append it to the section element
		impatienceParam.appendParameterElement(doc, queue);
	}

	private static XMLParameter createRenegingParameter(CommonModel model, Object stationKey,	Object classKey) {
		String renegingClassName = Reneging.class.getSimpleName();
		String renegingPath = Reneging.class.getCanonicalName();
		RenegingParameter renegingParameter = (RenegingParameter) model.getImpatienceParameter(stationKey, classKey);
		Distribution renegingDistribution = renegingParameter.getDistribution();

		return DistributionWriter
				.getDistributionParameter(renegingDistribution, model, classKey, renegingClassName, renegingPath);
	}

	private static XMLParameter createBalkingParameter(CommonModel model, Object stationKey,
			Object classKey, int numOfClasses, int classIndex) {
		String balkingClassName = Balking.class.getSimpleName();
		String balkingPath = Balking.class.getCanonicalName();

		BalkingParameter balkingParameter = (BalkingParameter) model.getImpatienceParameter(stationKey, classKey);
		LDStrategy strategy = balkingParameter.getLdStrategy();
		boolean priorityActivated = balkingParameter.getPriorityActivated();

		// --- Load Dependent Service Strategy (borrowed from getServiceSection()) --- //
		XMLParameter[] ranges = createRangeParameterArray(strategy);

		// Creates LDParameter array
		XMLParameter LDParameter = createLDParameterArray(ranges);

		// Creates service strategy (Parameter #1 of Balking class)
		XMLParameter loadDependentStrategyParameter = createLoadDependentStrategyParameter(LDParameter);

		// Creates the boolean parameter for the priorityActivated field (Parameter #2 of Balking class)
		XMLParameter booleanParameter = new XMLParameter("priorityActivated",
				Boolean.class.getName(),	null, String.valueOf(priorityActivated), true);

		// Finally, create the balkingParameter and set the array to be false
		XMLParameter xmlBalkingParameter = new XMLParameter(balkingClassName,
				balkingPath, model.getClassName(classKey),
				new XMLParameter[] {loadDependentStrategyParameter, booleanParameter}, true);
		xmlBalkingParameter.parameterArray = "false";
		// --- End --- //

		return xmlBalkingParameter;
  }

	private static XMLParameter[] createRangeParameterArray(LDStrategy strategy) {
		XMLParameter[] ranges = new XMLParameter[strategy.getRangeNumber()];
		Object[] rangeKeys = strategy.getAllRanges();

		for (int j = 0; j < ranges.length; j++) {
			// Creates "from" parameter
			XMLParameter from = new XMLParameter("from",
					Integer.class.getName(), null,
					Integer.toString(strategy
							.getRangeFrom(rangeKeys[j])), true);
			// Creates "distribution" parameter
			XMLParameter[] distr = DistributionWriter
					.getDistributionParameter(strategy
							.getRangeDistribution(rangeKeys[j]));
			// Creates "function" parameter (mean value of the distribution)
			XMLParameter function = new XMLParameter("function",
					String.class.getName(), null,
					strategy.getRangeDistributionMean(rangeKeys[j]),
					true);
			ranges[j] = new XMLParameter("LDParameter",
					strategiesClasspathBase + serviceStrategiesSuffix
							+ "LDParameter", null, new XMLParameter[]{
					from, distr[0], distr[1],
					function}, true);
			// Sets array = false as it is not an array of equal elements
			ranges[j].parameterArray = "false";
		}
		return ranges;
	}

	private static XMLParameter createLDParameterArray(XMLParameter[] ranges) {
		return new XMLParameter("LDParameter",
				strategiesClasspathBase + serviceStrategiesSuffix
						+ "LDParameter", null, ranges, true);
	}

	private static XMLParameter createLoadDependentStrategyParameter(XMLParameter LDParameter) {
		XMLParameter loadDependentStrategyParam = new XMLParameter("LoadDependentStrategy",
				strategiesClasspathBase + serviceStrategiesSuffix
						+ "LoadDependentStrategy",
				null,
				new XMLParameter[] { LDParameter }, true);
		loadDependentStrategyParam.parameterArray = "false";

		return loadDependentStrategyParam;
	}

	static protected void writeTerminalSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_TERMINAL);
		node.appendChild(elem);
		Vector<Object> classes = model.getClassKeys();
		// obtain classes that must be generated by this terminal
		Vector<Object> refClasses = getClassesForTerminal(model, stationKey);
		XMLParameter[] distrParams = new XMLParameter[classes.size()];
		for (int i = 0; i < distrParams.length; i++) {
			// if current class must be generated by this terminal
			Object currentClass = classes.get(i);
			if (refClasses.contains(currentClass)) {
				distrParams[i] = new XMLParameter("numberOfJobs",
						"java.lang.Integer", model.getClassName(currentClass),
						model.getClassPopulation(currentClass).toString(), true);
			} else {
				// otherwise write a null parameter
				distrParams[i] = new XMLParameter("numberOfJobs",
						"java.lang.Integer", model.getClassName(currentClass),
						"-1", true);
			}
		}
		// creating global population parameter
		XMLParameter globalStrategyParameter = new XMLParameter("NumberOfJobs",
				"java.lang.Integer", null, distrParams, false);
		// finally, create node from parameters and append it to the section
		// element
		globalStrategyParameter.appendParameterElement(doc, elem);
	}

	static protected void writeServerSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		String queueStrategy = model.getStationQueueStrategy(stationKey);
		boolean hasKValue = false;
		if (queueStrategy.equals(STATION_QUEUE_STRATEGY_PSSERVER)) {
				elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_PSSERVER);
		} else if (queueStrategy.equals(STATION_QUEUE_STRATEGY_POLLING)) {
		    String pollingServer = POLLING_TYPE_MAPPING.get(model.getStationPollingServerType(stationKey));
			elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, pollingServer);
			hasKValue = pollingServer.equals(CLASSNAME_LKPSERVER);

		} else if (queueStrategy.equals(STATION_QUEUE_STRATEGY_PREEMPTIVE)){
			elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_PREEMPTIVESERVER);
		} else {
			elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_SERVER);
		}

		node.appendChild(elem);

		Vector<Object> classes = model.getClassKeys();
		// creating number of servers node element
		Integer number = model.getStationNumberOfServers(stationKey);
		XMLParameter numberOfServers = new XMLParameter("maxJobs",
				"java.lang.Integer", null, number.toString(), false);
		numberOfServers.appendParameterElement(doc, elem);

		String servicePre = model.getStationServerPreemptive(stationKey) == null ? "false"
				: model.getStationServerPreemptive(stationKey).toString();
		XMLParameter serverPreemptive = new XMLParameter("serverPreemptive", "java.lang.Boolean", null,
				servicePre, false);
		// ...and add it to queue element's children
		//serverPreemptive.appendParameterElement(doc, elem); //remove to re-enable serverPreemptive for LCFS-PR


		// creating numbers of visits node element
		XMLParameter[] numberOfVisits = new XMLParameter[classes.size()];
		for (int i = 0; i < numberOfVisits.length; i++) {
			numberOfVisits[i] = new XMLParameter("numberOfVisits",
					"java.lang.Integer", model.getClassName(classes.get(i)),
					"1", true);
		}
		XMLParameter numbersOfVisits = new XMLParameter("numberOfVisits",
				"java.lang.Integer", null, numberOfVisits, false);
		numbersOfVisits.appendParameterElement(doc, elem);
		ServiceStrategyWriter.getServiceSection(model, stationKey).appendParameterElement(doc, elem);

		if (queueStrategy.equals(STATION_QUEUE_STRATEGY_PSSERVER)) {
			// creating PS strategies node element
			XMLParameter[] psStrategy = new XMLParameter[classes.size()];
			for (int i = 0; i < psStrategy.length; i++) {
				String strategy = model.getQueueStrategy(stationKey,
						classes.get(i));
				String strategyName = PS_STRATEGIES_MAPPING.get(strategy);
				psStrategy[i] = new XMLParameter(strategyName,
						strategiesClasspathBase + psStrategiesSuffix
						+ strategyName, model.getClassName(classes.get(i)),
						(String) null, true);
			}
			XMLParameter psStrategies = new XMLParameter("PSStrategy",
					strategiesClasspathBase + "PSStrategy", null,
					psStrategy, false);
			psStrategies.appendParameterElement(doc, elem);
			// creating service weights node element
			XMLParameter[] serviceWeight = new XMLParameter[classes.size()];
			for (int i = 0; i < serviceWeight.length; i++) {
				Double weight = model.getServiceWeight(stationKey,
						classes.get(i));
				serviceWeight[i] = new XMLParameter("serviceWeight",
						"java.lang.Double", model.getClassName(classes.get(i)),
						weight.toString(), true);
			}
			XMLParameter serviceWeights = new XMLParameter("serviceWeights",
					"java.lang.Double", null, serviceWeight, false);
			serviceWeights.appendParameterElement(doc, elem);
		}

		if (queueStrategy.equals(STATION_QUEUE_STRATEGY_POLLING)) {

			XMLParameter distParameter = SwitchoverStrategyWriter.getServiceSection(model, stationKey);
			distParameter.parameterName = "switchoverStrategy";
			distParameter.appendParameterElement(doc, elem);

			if (hasKValue) {
				Integer kValueInt = model.getStationPollingServerKValue(stationKey);
				XMLParameter kValue = new XMLParameter("pollingKValue",
						"java.lang.Integer", null, kValueInt.toString(), false );
				kValue.appendParameterElement(doc, elem);
			}

		}

	}

	protected interface DistributionStrategyGetter {
		public Object get(CommonModel model, Object stationKey, Object classKey);
	}

	protected static class ServiceStrategyWriter implements DistributionStrategyGetter {
		private static ServiceStrategyWriter INSTANCE = new ServiceStrategyWriter();
		private ServiceStrategyWriter(){};

		public static XMLParameter getServiceSection(CommonModel model, Object stationKey) {
			return XMLWriter.getServiceSection(model, stationKey, INSTANCE);
		}

		public Object get(CommonModel model, Object stationKey, Object classKey) {
			return model.getServiceTimeDistribution(stationKey, classKey);
		}
		public String getName() {return "ServerStrategy";}
	}

	protected static class SwitchoverStrategyWriter implements DistributionStrategyGetter {
		private static SwitchoverStrategyWriter INSTANCE = new SwitchoverStrategyWriter();
		private SwitchoverStrategyWriter(){};
		public static XMLParameter getServiceSection(CommonModel model, Object stationKey) {
			return XMLWriter.getServiceSection(model, stationKey, INSTANCE);

		}
		public Object get(CommonModel model, Object stationKey, Object classKey) {
			return model.getStationPollingSwitchoverDistribution(stationKey, classKey);
		}

	}

	/**
	 * Returns a service section representation in XMLParameter format
	 *
	 * @param model
	 *            model data structure
	 * @param stationKey
	 *            search's key for current station
	 * @return service section representation in XMLParameter format Author:
	 *         Bertoli Marco
	 */
	protected static XMLParameter getServiceSection(CommonModel model,
			Object stationKey, DistributionStrategyGetter distributionGetter) {
		Vector<Object> classes = model.getClassKeys();
		// creating set of service time distributions
		XMLParameter[] distrParams = new XMLParameter[classes.size()];
		Object currentClass;
		for (int i = 0; i < distrParams.length; i++) {
			currentClass = classes.get(i);
			Object serviceDistribution = distributionGetter.get(model, stationKey, currentClass);
			if (serviceDistribution instanceof Distribution) {
				// Service time is a Distribution and not a LDService
				distrParams[i] = DistributionWriter
						.getDistributionParameter(
								(Distribution) serviceDistribution, model,
								currentClass,  "ServiceTimeStrategy", SERVICE_TIME_STRATEGY_PATH);
			} else if (serviceDistribution instanceof ZeroStrategy) {
				// Zero Service Time Strategy
				distrParams[i] = new XMLParameter("ZeroServiceTimeStrategy",
						ZeroStrategy.getEngineClassPath(),
						model.getClassName(currentClass), (String) null, true);
			} else if (serviceDistribution instanceof DisabledStrategy) {
				// Disabled Service Time Strategy
				distrParams[i] = new XMLParameter("DisabledServiceTimeStrategy",
						DisabledStrategy.getEngineClassPath(),
						model.getClassName(currentClass), (String) null, true);
			} else {
				// Load Dependent Service Strategy
				LDStrategy strategy = (LDStrategy) serviceDistribution;
				XMLParameter[] ranges;
					// Creates "function" parameter (mean value of the
					// distribution)
				ranges = createRangeParameterArray(strategy);										
				
				// Creates LDParameter array
				XMLParameter LDParameter = createLDParameterArray(ranges);
				// Creates service strategy
				distrParams[i] = createLoadDependentStrategyParameter(LDParameter);
			}
		}
       XMLParameter globalDistr = new XMLParameter("ServerStrategy",			strategiesClasspathBase + "ServiceStrategy", null, distrParams, false);
		return globalDistr;
	}

	static protected void writeTunnelSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_TUNNEL);
		node.appendChild(elem);
	}

	/**
	 * Write all parameters for a Logger section.
	 *
	 * @param doc
	 *            XML document
	 * @param node
	 *            XML hierarchy node
	 * @param model
	 *            link to data structure
	 * @param stationKey
	 *            key of search for this source station into data structure
	 * @author Michael Fercu (Bertoli Marco) Date: 08-aug-2008
	 * @see jmt.engine.log.LoggerParameters LoggerParameters
	 * @see jmt.gui.common.definitions.CommonModel#getLoggingParameters
	 *      CommonModel.getLoggingParameters()
	 * @see jmt.gui.common.xml.XMLReader XMLReader.parseLogger()
	 * @see jmt.engine.NodeSections.LogTunnel LogTunnel
	 */
	static protected void writeLoggerSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_LOGGER);
		node.appendChild(elem);

		// Get this station's logger parameters
		LoggerParameters loggerParameters = (LoggerParameters) model
				.getLoggingParameters(stationKey);

		XMLParameter name = new XMLParameter(XML_LOG_FILENAME,
				"java.lang.String", null, loggerParameters.name.toString(),
				false);
		name.appendParameterElement(doc, elem);
		// temporary fix
		loggerParameters.path = MacroReplacer.replace(model.getLoggingGlbParameter("path"));
		XMLParameter path = new XMLParameter(XML_LOG_FILEPATH,
				"java.lang.String", null,
				loggerParameters.path, false);
		path.appendParameterElement(doc, elem);
		XMLParameter logExecTimestamp = new XMLParameter(
				XML_LOG_B_EXECTIMESTAMP, "java.lang.Boolean", null,
				loggerParameters.boolExecTimestamp.toString(), false);
		logExecTimestamp.appendParameterElement(doc, elem);
		XMLParameter logLoggerName = new XMLParameter(XML_LOG_B_LOGGERNAME,
				"java.lang.Boolean", null,
				loggerParameters.boolLoggername.toString(), false);
		logLoggerName.appendParameterElement(doc, elem);
		XMLParameter logTimeStamp = new XMLParameter(XML_LOG_B_TIMESTAMP,
				"java.lang.Boolean", null,
				loggerParameters.boolTimeStamp.toString(), false);
		logTimeStamp.appendParameterElement(doc, elem);
		XMLParameter logJobID = new XMLParameter(XML_LOG_B_JOBID,
				"java.lang.Boolean", null,
				loggerParameters.boolJobID.toString(), false);
		logJobID.appendParameterElement(doc, elem);
		XMLParameter logJobClass = new XMLParameter(XML_LOG_B_JOBCLASS,
				"java.lang.Boolean", null,
				loggerParameters.boolJobClass.toString(), false);
		logJobClass.appendParameterElement(doc, elem);
		XMLParameter logTimeSameClass = new XMLParameter(XML_LOG_B_TIMESAMECLS,
				"java.lang.Boolean", null,
				loggerParameters.boolTimeSameClass.toString(), false);
		logTimeSameClass.appendParameterElement(doc, elem);
		XMLParameter logTimeAnyClass = new XMLParameter(XML_LOG_B_TIMEANYCLS,
				"java.lang.Boolean", null,
				loggerParameters.boolTimeAnyClass.toString(), false);
		logTimeAnyClass.appendParameterElement(doc, elem);
		XMLParameter classSize = new XMLParameter("numClasses",
				"java.lang.Integer", null, new Integer(model.getClassKeys()
						.size()).toString(), false);
		classSize.appendParameterElement(doc, elem);
	}

	static protected void writeDelaySection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_DELAY);
		node.appendChild(elem);
		ServiceStrategyWriter.getServiceSection(model, stationKey).appendParameterElement(doc, elem);
	}

	static protected void writeRouterSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_ROUTER);
		node.appendChild(elem);
		// creating list of parameters for each single routing strategy
		Vector<Object> classes = model.getClassKeys();
		XMLParameter[] routingStrats = new XMLParameter[classes.size()];
		Object currentClass;
		for (int i = 0; i < routingStrats.length; i++) {
			currentClass = classes.get(i);
			routingStrats[i] = RoutingStrategyWriter
					.getRoutingStrategyParameter((RoutingStrategy) model
							.getRoutingStrategy(stationKey, currentClass),
							model, currentClass, stationKey);
		}
		XMLParameter globalRouting = new XMLParameter("RoutingStrategy",
				strategiesClasspathBase + "RoutingStrategy", null,
				routingStrats, false);
		globalRouting.appendParameterElement(doc, elem);
	}

	/**
	 * Adds to @doc the tags to store a class switch section.
	 * @param doc the xml where you want to add class switch section
	 * @param node the node owner of the class switch section
	 * @param model the data structure of the model
	 * @param stationKey the station implementing class switching
	 */
	static protected void writeClassSwitchSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_CLASSSWITCH);
		node.appendChild(elem);
		//Data-structure to xml
		float 	value;
		Object 	classInKey;
		Object 	classOutKey;
		XMLParameter matrix;
		XMLParameter rows[];
		Vector<Object> classes;

		classes = model.getClassKeys();
		rows = new XMLParameter[classes.size()]; 
		for (int i = 0; i < classes.size(); i++) {
			XMLParameter cells[] = new XMLParameter[classes.size()]; 
			classInKey = classes.get(i);
			for (int j = 0; j < classes.size(); j++) {
				classOutKey = classes.get(j);
				value = model.getClassSwitchMatrix(stationKey, classInKey, classOutKey);
				cells[j] = new XMLParameter("cell", "java.lang.Float", model.getClassName(classOutKey), Float.toString(value), true);
			}
			rows[i] = new XMLParameter("row", "java.lang.Float", model.getClassName(classInKey), cells, true);
		}
		matrix = new XMLParameter("matrix", "java.lang.Object", null, rows, false);
		matrix.appendParameterElement(doc, elem);
	}

	static protected void writeStorageSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		// Storage Section
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_STORAGE);
		node.appendChild(elem);

		// Total Capacity
		Integer totalCap = model.getStationQueueCapacity(stationKey);
		XMLParameter totalCapacity = new XMLParameter("totalCapacity",
				Integer.class.getName(), null, totalCap.toString(), false);
		totalCapacity.appendParameterElement(doc, elem);

		// Capacities
		Vector<Object> classes = model.getClassKeys();
		XMLParameter[] capacity = new XMLParameter[classes.size()];
		for (int i = 0; i < capacity.length; i++) {
			Integer cap = model.getQueueCapacity(stationKey, classes.get(i));
			capacity[i] = new XMLParameter("capacity",
					Integer.class.getName(), model.getClassName(classes.get(i)),
					cap.toString(), true);
		}
		XMLParameter capacities = new XMLParameter("capacities",
				Integer.class.getName(), null, capacity, false);
		capacities.appendParameterElement(doc, elem);

		// Drop Rules
		XMLParameter[] dropRule = new XMLParameter[classes.size()];
		for (int i = 0; i < dropRule.length; i++) {
			String rule = model.getDropRule(stationKey, classes.get(i));
			dropRule[i] = new XMLParameter("dropRule",
					String.class.getName(), model.getClassName(classes.get(i)),
					DROP_RULES_MAPPING.get(rule), true);
		}
		XMLParameter dropRules = new XMLParameter("dropRules",
				String.class.getName(), null, dropRule, false);
		dropRules.appendParameterElement(doc, elem);

		// Get Strategy
		XMLParameter getStrategy = new XMLParameter("getStrategy",
				strategiesClasspathBase + queueGetStrategiesSuffix + "FCFSstrategy",
				null, (String) null, false);
		getStrategy.appendParameterElement(doc, elem);

		// Put Strategies
		XMLParameter[] putStrategy = new XMLParameter[classes.size()];
		for (int i = 0; i < putStrategy.length; i++) {
			String strategy = QUEUE_PUT_MAPPING.get(
					model.getQueueStrategy(stationKey, classes.get(i)));
			putStrategy[i] = new XMLParameter("putStrategy",
					strategiesClasspathBase + queuePutStrategiesSuffix + strategy,
					model.getClassName(classes.get(i)), (String) null, true);
		}
		XMLParameter putStrategies = new XMLParameter("putStrategies",
				strategiesClasspathBase + "QueuePutStrategy",
				null, putStrategy, false);
		putStrategies.appendParameterElement(doc, elem);
	}

	static protected void writeLinkageSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		// Linkage Section
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_LINKAGE);
		node.appendChild(elem);
	}

	static protected void writeEnablingSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		// Enabling Section
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_ENABLING);
		node.appendChild(elem);

		// Enabling Conditions
		Vector<Object> stationInKeys = model.getBackwardConnections(stationKey);
		Vector<Object> classKeys = model.getClassKeys();
		XMLParameter[] enablingCondition = new XMLParameter[model.getTransitionModeListSize(stationKey)];
		for (int i = 0; i < enablingCondition.length; i++) {
			XMLParameter[] enablingVector = new XMLParameter[stationInKeys.size()];
			for (int j = 0; j < enablingVector.length; j++) {
				XMLParameter[] enablingEntry = new XMLParameter[classKeys.size()];
				for (int k = 0; k < enablingEntry.length; k++) {
					Integer ivalue = model.getEnablingCondition(stationKey, i,
							stationInKeys.get(j), classKeys.get(k));
					enablingEntry[k] = new XMLParameter("enablingEntry",
							Integer.class.getName(), model.getClassName(classKeys.get(k)),
							ivalue.toString(), true);
				}
				XMLParameter stationName = new XMLParameter("stationName",
						String.class.getName(), null, model.getStationName(stationInKeys.get(j)), true);
				XMLParameter enablingEntries = new XMLParameter("enablingEntries",
						Integer.class.getName(), null, enablingEntry, true);
				enablingVector[j] = new XMLParameter("enablingVector",
						strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionVector",
						null, new XMLParameter[] { stationName, enablingEntries }, true);
				enablingVector[j].parameterArray = "false";
			}
			XMLParameter enablingVectors = new XMLParameter("enablingVectors",
					strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionVector",
					null, enablingVector, true);
			enablingCondition[i] = new XMLParameter("enablingCondition",
					strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionMatrix",
					null, new XMLParameter[] { enablingVectors }, true);
			enablingCondition[i].parameterArray = "false";
		}
		XMLParameter enablingConditions = new XMLParameter("enablingConditions",
				strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionMatrix",
				null, enablingCondition, false);
		enablingConditions.appendParameterElement(doc, elem);

		// Inhibiting Conditions
		XMLParameter[] inhibitingCondition = new XMLParameter[model.getTransitionModeListSize(stationKey)];
		for (int i = 0; i < inhibitingCondition.length; i++) {
			XMLParameter[] inhibitingVector = new XMLParameter[stationInKeys.size()];
			for (int j = 0; j < inhibitingVector.length; j++) {
				XMLParameter[] inhibitingEntry = new XMLParameter[classKeys.size()];
				for (int k = 0; k < inhibitingEntry.length; k++) {
					Integer ivalue = model.getInhibitingCondition(stationKey, i,
							stationInKeys.get(j), classKeys.get(k));
					inhibitingEntry[k] = new XMLParameter("inhibitingEntry",
							Integer.class.getName(), model.getClassName(classKeys.get(k)),
							ivalue.toString(), true);
				}
				XMLParameter stationName = new XMLParameter("stationName",
						String.class.getName(), null, model.getStationName(stationInKeys.get(j)), true);
				XMLParameter inhibitingEntries = new XMLParameter("inhibitingEntries",
						Integer.class.getName(), null, inhibitingEntry, true);
				inhibitingVector[j] = new XMLParameter("inhibitingVector",
						strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionVector",
						null, new XMLParameter[] { stationName, inhibitingEntries }, true);
				inhibitingVector[j].parameterArray = "false";
			}
			XMLParameter inhibitingVectors = new XMLParameter("inhibitingVectors",
					strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionVector",
					null, inhibitingVector, true);
			inhibitingCondition[i] = new XMLParameter("inhibitingCondition",
					strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionMatrix",
					null, new XMLParameter[] { inhibitingVectors }, true);
			inhibitingCondition[i].parameterArray = "false";
		}
		XMLParameter inhibitingConditions = new XMLParameter("inhibitingConditions",
				strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionMatrix",
				null, inhibitingCondition, false);
		inhibitingConditions.appendParameterElement(doc, elem);
	}

	static protected void writeTimingSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		// Timing Section
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_TIMING);
		node.appendChild(elem);

		// Modes Names
		XMLParameter[] modeName = new XMLParameter[model.getTransitionModeListSize(stationKey)];
		for (int i = 0; i < modeName.length; i++) {
			String name = model.getTransitionModeName(stationKey, i);
			modeName[i] = new XMLParameter("modeName",
					String.class.getName(), null, name, true);
		}
		XMLParameter modeNames = new XMLParameter("modeNames",
				String.class.getName(), null, modeName, false);
		modeNames.appendParameterElement(doc, elem);

		// Numbers of Servers
		XMLParameter[] numberOfServers = new XMLParameter[model.getTransitionModeListSize(stationKey)];
		for (int i = 0; i < numberOfServers.length; i++) {
			Integer number = model.getNumberOfServers(stationKey, i);
			numberOfServers[i] = new XMLParameter("numberOfServers",
					Integer.class.getName(), null, number.toString(), true);
		}
		XMLParameter numbersOfServers = new XMLParameter("numbersOfServers",
				Integer.class.getName(), null, numberOfServers, false);
		numbersOfServers.appendParameterElement(doc, elem);

		// Timing Strategies
		XMLParameter[] timingStrategy = new XMLParameter[model.getTransitionModeListSize(stationKey)];
		for (int i = 0; i < timingStrategy.length; i++) {
			Object strategy = model.getFiringTimeDistribution(stationKey, i);
			if (strategy instanceof Distribution) {
				timingStrategy[i] = DistributionWriter.getDistributionParameter(
						(Distribution) strategy, model, null, "ServiceTimeStrategy",
						strategiesClasspathBase + serviceStrategiesSuffix + "ServiceTimeStrategy");
				timingStrategy[i].parameterName = "timingStrategy";
			} else {
				timingStrategy[i] = new XMLParameter("timingStrategy",
						ZeroStrategy.getEngineClassPath(), null, (String) null, true);
			}
		}
		XMLParameter timingStrategies = new XMLParameter("timingStrategies",
				strategiesClasspathBase + "ServiceStrategy", null, timingStrategy, false);
		timingStrategies.appendParameterElement(doc, elem);

		// Firing Priorities
		XMLParameter[] firingPriority = new XMLParameter[model.getTransitionModeListSize(stationKey)];
		for (int i = 0; i < firingPriority.length; i++) {
			Integer priority = model.getFiringPriority(stationKey, i);
			firingPriority[i] = new XMLParameter("firingPriority",
					Integer.class.getName(), null, priority.toString(), true);
		}
		XMLParameter firingPriorities = new XMLParameter("firingPriorities",
				Integer.class.getName(), null, firingPriority, false);
		firingPriorities.appendParameterElement(doc, elem);

		// Firing Weights
		XMLParameter[] firingWeight = new XMLParameter[model.getTransitionModeListSize(stationKey)];
		for (int i = 0; i < firingWeight.length; i++) {
			Double weight = model.getFiringWeight(stationKey, i);
			firingWeight[i] = new XMLParameter("firingWeight",
					Double.class.getName(), null, weight.toString(), true);
		}
		XMLParameter firingWeights = new XMLParameter("firingWeights",
				Double.class.getName(), null, firingWeight, false);
		firingWeights.appendParameterElement(doc, elem);
	}

	static protected void writeFiringSection(Document doc, Node node,
			CommonModel model, Object stationKey) {
		// Firing Section
		Element elem = doc.createElement(XML_E_STATION_SECTION);
		elem.setAttribute(XML_A_STATION_SECTION_CLASSNAME, CLASSNAME_FIRING);
		node.appendChild(elem);

		// Firing Outcomes
		Vector<Object> stationOutKeys = model.getForwardConnections(stationKey);
		Vector<Object> classKeys = model.getClassKeys();
		XMLParameter[] firingOutcome = new XMLParameter[model.getTransitionModeListSize(stationKey)];
		for (int i = 0; i < firingOutcome.length; i++) {
			XMLParameter[] firingVector = new XMLParameter[stationOutKeys.size()];
			for (int j = 0; j < firingVector.length; j++) {
				XMLParameter[] firingEntry = new XMLParameter[classKeys.size()];
				for (int k = 0; k < firingEntry.length; k++) {
					Integer ivalue = model.getFiringOutcome(stationKey, i,
							stationOutKeys.get(j), classKeys.get(k));
					firingEntry[k] = new XMLParameter("firingEntry",
							Integer.class.getName(), model.getClassName(classKeys.get(k)),
							ivalue.toString(), true);
				}
				XMLParameter stationName = new XMLParameter("stationName",
						String.class.getName(), null, model.getStationName(stationOutKeys.get(j)), true);
				XMLParameter firingEntries = new XMLParameter("firingEntries",
						Integer.class.getName(), null, firingEntry, true);
				firingVector[j] = new XMLParameter("firingVector",
						strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionVector",
						null, new XMLParameter[] { stationName, firingEntries }, true);
				firingVector[j].parameterArray = "false";
			}
			XMLParameter firingVectors = new XMLParameter("firingVectors",
					strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionVector",
					null, firingVector, true);
			firingOutcome[i] = new XMLParameter("firingOutcome",
					strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionMatrix",
					null, new XMLParameter[] { firingVectors }, true);
			firingOutcome[i].parameterArray = "false";
		}
		XMLParameter firingOutcomes = new XMLParameter("firingOutcomes",
				strategiesClasspathBase + transitionUtilitiesSuffix + "TransitionMatrix",
				null, firingOutcome, false);
		firingOutcomes.appendParameterElement(doc, elem);
	}

	// returns a list of keys for customer classes generated by a specific
	// source station
	static protected Vector<Object> getClassesForSource(CommonModel model,
			Object stationKey) {
		Vector<Object> classes = new Vector<Object>();
		for (Object key : model.getOpenClassKeys()) {
			if (model.getClassRefStation(key) == stationKey) {
				classes.add(key);
			}
		}
		return classes;
	}

	// returns a list of keys for customer classes generated by a specific
	// terminal station
	static protected Vector<Object> getClassesForTerminal(CommonModel model,
			Object stationKey) {
		Vector<Object> classes = new Vector<Object>();
		for (Object key : model.getClosedClassKeys()) {
			if (model.getClassRefStation(key) == stationKey) {
				classes.add(key);
			}
		}
		return classes;
	}

	/*-----------------------------------------------------------------------------------
	 *----------------------- Methods for construction of measures -----------------------
	 *-----------------------------------------------------------------------------------*/
	static protected void writeMeasures(Document doc, Node simNode,
			CommonModel model) {
		Vector<Object> v = model.getMeasureKeys();
		for (int i = 0; i < v.size(); i++) {
			Element elem = doc.createElement(XML_E_MEASURE);
			Object key = v.get(i);
			String type = model.getMeasureType(key);
			elem.setAttribute(XML_A_MEASURE_TYPE, type);

			String name = "";
			Object stationKey = model.getMeasureStation(key);
			String stationName = null;
			String nodeType = null;
			if (stationKey == null) {
				elem.setAttribute(XML_A_MEASURE_STATION, "");
				elem.setAttribute(XML_A_MEASURE_NODETYPE, "");
			} else {
				if (model.getRegionKeys().contains(stationKey)) {
					stationName = model.getRegionName(stationKey);
					nodeType = NODETYPE_REGION;
				} else {
					stationName = model.getStationName(stationKey);
					nodeType = NODETYPE_STATION;
				}
				elem.setAttribute(XML_A_MEASURE_STATION, stationName);
				elem.setAttribute(XML_A_MEASURE_NODETYPE, nodeType);
				name += stationName + "_";
			}
			Object classKey = model.getMeasureClass(key);
			String className = null;
			if (classKey == null) {
				elem.setAttribute(XML_A_MEASURE_CLASS, "");
			} else {
				if (type.equals(SimulationDefinition.MEASURE_FX)) {
					className = (String) classKey;
				} else {
					className = model.getClassName(classKey);
				}
				elem.setAttribute(XML_A_MEASURE_CLASS, className);
				name += className + "_";
			}
			name += type;

			// Inverts alpha and keeps only 10 decimal cifres
			double alpha = 1 - model.getMeasureAlpha(key).doubleValue();
			alpha = Math.rint(alpha * 1e10) / 1e10;
			elem.setAttribute(XML_A_MEASURE_ALPHA, Double.toString(alpha));
			elem.setAttribute(XML_A_MEASURE_PRECISION, model.getMeasurePrecision(key).toString());
			elem.setAttribute(XML_A_MEASURE_NAME, name);
			elem.setAttribute(XML_A_MEASURE_VERBOSE, Boolean.toString(model.getMeasureLog(key)));
			simNode.appendChild(elem);
		}
	}

	/*-----------------------------------------------------------------------------------
	 *--------------------- Methods for construction of connections ----------------------
	 *-----------------------------------------------------------------------------------*/
	static protected void writeConnections(Document doc, Node simNode,
			CommonModel model) {
		Vector<Object> stations = model.getStationKeys();
		String[] stationNames = new String[stations.size()];
		for (int i = 0; i < stations.size(); i++) {
			stationNames[i] = model.getStationName(stations.get(i));
		}
		for (int i = 0; i < stations.size(); i++) {
			for (int j = 0; j < stations.size(); j++) {
				if (model.areConnected(stations.get(i), stations.get(j))) {
					Element elem = doc.createElement(XML_E_CONNECTION);
					elem.setAttribute(XML_A_CONNECTION_SOURCE, stationNames[i]);
					elem.setAttribute(XML_A_CONNECTION_TARGET, stationNames[j]);
					simNode.appendChild(elem);
				}
			}
		}
	}

	/*-----------------------------------------------------------------------------------
	--------------------- Methods for construction of preload data --- Bertoli Marco ----
	------------------------------------------------------------------------------------*/
	static protected void writePreload(Document doc, Node simNode,
			CommonModel model) {
		// Finds if and where preloading is needed
		Vector<Object> stations = model.getStationKeys();
		Vector<Object> classes = model.getClassKeys();
		// A map containing all stations that need preloading
		Vector<Element> p_stations = new Vector<Element>();
		for (int stat = 0; stat < stations.size(); stat++) {
			Object key = stations.get(stat);
			Vector<Element> p_class = new Vector<Element>();
			for (int i = 0; i < classes.size(); i++) {
				Object classKey = classes.get(i);
				Integer jobs = model.getPreloadedJobs(key, classKey);
				if (jobs.intValue() > 0) {
					Element elem = doc.createElement(XML_E_CLASSPOPULATION);
					elem.setAttribute(XML_A_CLASSPOPULATION_NAME,
							model.getClassName(classKey));
					elem.setAttribute(XML_A_CLASSPOPULATION_POPULATION,
							jobs.toString());
					p_class.add(elem);
				}
			}
			// If any preload is provided for this station, creates its element
			// and adds it to p_stations
			if (!p_class.isEmpty()) {
				Element elem = doc.createElement(XML_E_STATIONPOPULATIONS);
				elem.setAttribute(XML_A_PRELOADSTATION_NAME,
						model.getStationName(key));
				while (!p_class.isEmpty()) {
					elem.appendChild(p_class.remove(0));
				}
				p_stations.add(elem);
			}
		}
		// If p_stations is not empty, creates a preload section for stations in
		// p_stations
		if (!p_stations.isEmpty()) {
			Element preload = doc.createElement(XML_E_PRELOAD);
			while (!p_stations.isEmpty()) {
				preload.appendChild(p_stations.remove(0));
			}
			simNode.appendChild(preload);
		}
	}

	/*-----------------------------------------------------------------------------------
	------------------- Methods for construction of blocking regions --------------------
	--------------------------------- Bertoli Marco ------------------------------------*/
	static protected void writeBlockingRegions(Document doc, Node simNode,
			CommonModel model) {
		Vector<Object> regions = model.getRegionKeys();
		for (int reg = 0; reg < regions.size(); reg++) {
			Object key = regions.get(reg);
			Element region = doc.createElement(XML_E_REGION);

			// Sets name attribute
			region.setAttribute(XML_A_REGION_NAME, model.getRegionName(key));
			// Sets type attribute (optional)
			region.setAttribute(XML_A_REGION_TYPE, model.getRegionType(key));

			// Adds nodes (stations) to current region
			Iterator<Object> stations = model.getBlockingRegionStations(key).iterator();
			while (stations.hasNext()) {
				Element node = doc.createElement(XML_E_REGIONNODE);
				node.setAttribute(XML_A_REGIONNODE_NAME,
						model.getStationName(stations.next()));
				region.appendChild(node);
			}

			// Adds global constraint
			Element globalConstraint = doc.createElement(XML_E_GLOBALCONSTRAINT);
			globalConstraint.setAttribute(XML_A_GLOBALCONSTRAINT_MAXJOBS,
					model.getRegionCustomerConstraint(key).toString());
			region.appendChild(globalConstraint);

			Element globalMemoryConstraint = doc.createElement(XML_E_GLOBALMEMORYCONSTRAINT);
			globalMemoryConstraint.setAttribute(XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY,
					model.getRegionMemorySize(key).toString());
			region.appendChild(globalMemoryConstraint);

			// Adds class constraints
			Iterator<Object> classes = model.getClassKeys().iterator();
			while (classes.hasNext()) {
				Object classKey = classes.next();
				Element classConstraint = doc.createElement(XML_E_CLASSCONSTRAINT);
				classConstraint.setAttribute(XML_A_CLASSCONSTRAINT_CLASS,
						model.getClassName(classKey));
				classConstraint.setAttribute(XML_A_CLASSCONSTRAINT_MAXJOBS,
						model.getRegionClassCustomerConstraint(key, classKey).toString());
				region.appendChild(classConstraint);
			}

			classes = model.getClassKeys().iterator();
			while (classes.hasNext()) {
				Object classKey = classes.next();
				Element classMemoryConstraint = doc.createElement(XML_E_CLASSMEMORYCONSTRAINT);
				classMemoryConstraint.setAttribute(XML_A_CLASSMEMORYCONSTRAINT_CLASS,
						model.getClassName(classKey));
				classMemoryConstraint.setAttribute(XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY,
						model.getRegionClassMemorySize(key, classKey).toString());
				region.appendChild(classMemoryConstraint);
			}

			classes = model.getClassKeys().iterator();
			while (classes.hasNext()) {
				Object classKey = classes.next();
				Element dropRule = doc.createElement(XML_E_DROPRULES);
				dropRule.setAttribute(XML_A_DROPRULE_CLASS,
						model.getClassName(classKey));
				dropRule.setAttribute(XML_A_DROPRULE_DROP,
						model.getRegionClassDropRule(key, classKey).toString());
				region.appendChild(dropRule);
			}

			classes = model.getClassKeys().iterator();
			while (classes.hasNext()) {
				Object classKey = classes.next();
				Element classWeight = doc.createElement(XML_E_CLASSWEIGHT);
				classWeight.setAttribute(XML_A_CLASSWEIGHT_CLASS,
						model.getClassName(classKey));
				classWeight.setAttribute(XML_A_CLASSWEIGHT_WEIGHT,
						model.getRegionClassWeight(key, classKey).toString());
				region.appendChild(classWeight);
			}

			classes = model.getClassKeys().iterator();
			while (classes.hasNext()) {
				Object classKey = classes.next();
				Element classSize = doc.createElement(XML_E_CLASSSIZE);
				classSize.setAttribute(XML_A_CLASSSIZE_CLASS,
						model.getClassName(classKey));
				classSize.setAttribute(XML_A_CLASSSIZE_SIZE,
						model.getRegionClassSize(key, classKey).toString());
				region.appendChild(classSize);
			}

			// Adds group constraints
			for (int i = 0; i < model.getRegionGroupList(key).size(); i++) {
				Element groupConstraint = doc.createElement(XML_E_GROUPCONSTRAINT);
				groupConstraint.setAttribute(XML_A_GROUPCONSTRAINT_GROUP,
						model.getRegionGroupName(key, i));
				groupConstraint.setAttribute(XML_A_GROUPCONSTRAINT_MAXJOBS,
						model.getRegionGroupCustomerConstraint(key, i).toString());
				region.appendChild(groupConstraint);
			}

			for (int i = 0; i < model.getRegionGroupList(key).size(); i++) {
				Element groupMemoryConstraint = doc.createElement(XML_E_GROUPMEMORYCONSTRAINT);
				groupMemoryConstraint.setAttribute(XML_A_GROUPMEMORYCONSTRAINT_GROUP,
						model.getRegionGroupName(key, i));
				groupMemoryConstraint.setAttribute(XML_A_GROUPMEMORYCONSTRAINT_MAXMEMORY,
						model.getRegionGroupMemorySize(key, i).toString());
				region.appendChild(groupMemoryConstraint);
			}

			for (int i = 0; i < model.getRegionGroupList(key).size(); i++) {
				Element groupClassList = doc.createElement(XML_E_GROUPCLASSLIST);
				classes = model.getRegionGroupClassList(key, i).iterator();
				while (classes.hasNext()) {
					Object classKey = classes.next();
					Element groupClass = doc
							.createElement(XML_E_GROUPCLASS);
					groupClass.setAttribute(XML_A_GROUPCLASS_GROUP,
							model.getRegionGroupName(key, i));
					groupClass.setAttribute(XML_A_GROUPCLASS_CLASS,
							model.getClassName(classKey));
					groupClassList.appendChild(groupClass);
				}
				region.appendChild(groupClassList);
			}

			simNode.appendChild(region);
		}
	}

	/*-----------------------------------------------------------------------------------
	------------------------ Inner classes for more ease of use -------------------------
	------------------------------------------------------------------------------------*/
	protected static class XMLParameter {

		public boolean isSubParameter = false;

		public String parameterName;
		public String parameterClasspath;
		public String parameterRefClass;
		public String parameterValue;
		public String parameterArray;
		public XMLParameter[] parameters;

		public XMLParameter(String name, String classpath, String refClass,
				String value, boolean isSubParameter) {
			this(name, classpath, refClass, value, null, isSubParameter);
			parameterArray = "false";
		}

		public XMLParameter(String name, String classpath, String refClass,
				XMLParameter[] parameters, boolean isSubParameter) {
			this(name, classpath, refClass, null, parameters, isSubParameter);
			parameterArray = "true";
		}

		private XMLParameter(String name, String classpath, String refClass,
				String value, XMLParameter[] parameters, boolean isSubParameter) {
			parameterName = name;
			parameterClasspath = classpath;
			parameterRefClass = refClass;
			parameterValue = value;
			this.parameters = parameters;
			this.isSubParameter = isSubParameter;
			if (parameters != null) {
				if (parameters.length > 1) {
					parameterArray = "false";
				} else {
					parameterArray = "true";
				}
			} else {
				parameterArray = "false";
			}
		}

		public void appendParameterElement(Document doc, Element scope) {
			// creating inner element containing queue length
			Element parameter = doc
					.createElement(isSubParameter ? XML_E_SUBPARAMETER
							: XML_E_PARAMETER);
			if (parameterClasspath != null) {
				parameter.setAttribute(XML_A_PARAMETER_CLASSPATH,
						parameterClasspath);
			}
			if (parameterName != null) {
				parameter.setAttribute(XML_A_PARAMETER_NAME, parameterName);
			}
			if (parameterArray != null && "true".equals(parameterArray)) {
				parameter.setAttribute(XML_A_PARAMETER_ARRAY, parameterArray);
			}

			// adding element refclass for this parameter
			if (parameterRefClass != null) {
				Element refclass = doc.createElement(XML_E_PARAMETER_REFCLASS);
				refclass.appendChild(doc.createTextNode(parameterRefClass));
				scope.appendChild(refclass);
			}
			// adding element value of parameter
			if (parameterValue != null) {
				Element value = doc.createElement(XML_E_PARAMETER_VALUE);
				value.appendChild(doc.createTextNode(parameterValue));
				parameter.appendChild(value);
			}
			if (parameters != null) {
				for (XMLParameter parameter2 : parameters) {
					if (parameter2 != null) {
						parameter2.appendParameterElement(doc, parameter);
					}
				}
			}
			scope.appendChild(parameter);
		}

	}

	/**
	 * This class provides a simple method to obtain XMLparameter representation
	 * of a distribution object. Creation of a distribution parameter is a bit
	 * awkward, so I'll explain it as best as I can as it follows. generally a
	 * distribution is associated to a service time strategy, either it is an
	 * interarrival distribution for open classes job generation, or a proper
	 * service time distribution for a certain station. As a result,
	 * distribution parameter is inserted in a ServiceTimeStrategy parameter
	 * which is the one userclass is associated to. Inside this parameter node
	 * should be inserted 2 subParameter nodes: <br>
	 * -One for distribution description(containing distribution classpath and
	 * name) <br>
	 * - One containing all of the distribution constructor parameters. <br>
	 * The first one has null value, the second contains a list of parameters
	 * which, as they are different from each other, they are not considered as
	 * array. Then, the node which contains them has no value for array
	 * attribute.
	 */
	protected static class DistributionWriter {

		/*
		 * returns a distribution in XMLParameter format, to allow nesting it in
		 * other parameters.
		 */
		static XMLParameter getDistributionParameter(Distribution distr,
				CommonModel model, Object classKey, String className, String classPath) {
			XMLParameter[] distribution = getDistributionParameter(distr);
			XMLParameter returnValue = new XMLParameter(className,
					classPath,
					model.getClassName(classKey), new XMLParameter[] {
							distribution[0], distribution[1] }, true);
			/*
			 * although this parameter contains several others, array attribute
			 * must be set to "false", as their type are not necessarily equal
			 */
			returnValue.parameterArray = "false";
			return returnValue;
		}

		/**
		 * Returns a Distribution in XMLParameter format without refclass. This
		 * is used to write load dependent service section distributions
		 * 
		 * @param distr
		 *            distribution to be written
		 * @return the two object to represent a distribution: distribution and
		 *         its parameter object Author: Bertoli Marco
		 */
		static XMLParameter[] getDistributionParameter(Distribution distr) {
			// a list of direct parameter -> parameter which must be passed
			// directly to the distribution object
			List<XMLParameter> directParams = new Vector<XMLParameter>();
			// a list of parameters which are passed to the distribution
			// parameter
			List<XMLParameter> nonDirectParams = new Vector<XMLParameter>();

			Parameter distrPar;
			// Object valueObj;

			// parse over all parameters and add them to the appropriate list
			for (int i = 0; i < distr.getNumberOfParameters(); i++) {
				distrPar = distr.getParameter(i);
				if (distrPar.isDirectParameter()) {
					directParams.add(getParameter(distrPar));
				} else {
					nonDirectParams.add(getParameter(distrPar));
				}
			}

			// get an array of the direct parameters
			XMLParameter[] directPars = new XMLParameter[directParams.size()];
			for (int i = 0; i < directPars.length; i++) {
				directPars[i] = directParams.get(i);
			}

			// get an array of the non direct parameters
			XMLParameter[] nonDirectPars = new XMLParameter[nonDirectParams
			                                                .size()];
			for (int i = 0; i < nonDirectPars.length; i++) {
				nonDirectPars[i] = nonDirectParams.get(i);
			}

			// create the distribution parameter with the direct parameters
			XMLParameter[] ret = new XMLParameter[2];
			ret[0] = new XMLParameter(distr.getName(), distr.getClassPath(),
					(String) null, directPars, true);
			// create the distribution parameter with the non direct parameters
			ret[1] = new XMLParameter("distrPar",
					distr.getParameterClassPath(), null, nonDirectPars, true);
			ret[0].parameterArray = "false";
			ret[1].parameterArray = "false";
			return ret;
		}

		/**
		 * Helper method to extract an XMLParameter from a Distribution
		 * parameter
		 * 
		 * @param distrPar
		 *            the distribution parameter
		 * @return the created XML Parameter
		 */
		static XMLParameter getParameter(Parameter distrPar) {
			Object valueObj = distrPar.getValue();
			if (valueObj != null) {
				if (valueObj instanceof Distribution) {
					XMLParameter[] distribution = getDistributionParameter(
							(Distribution) valueObj);
					XMLParameter returnValue = new XMLParameter(
							distrPar.getName(),
							distributionContainerClasspath, null,
							new XMLParameter[] { distribution[0],
									distribution[1] }, true);
					/*
					 * although this parameter contains several others, array
					 * attribute must be set to "false", as their type are not
					 * necessarily equal
					 */
					returnValue.parameterArray = "false";
					return returnValue;
				} else if (valueObj instanceof Object[][]) {
					Object[][] value = (Object[][]) valueObj;
					XMLParameter[] vector = new XMLParameter[value.length];
					for (int i = 0; i < vector.length; i++) {
						XMLParameter[] entry = new XMLParameter[value[i].length];
						for (int j = 0; j < entry.length; j++) {
							entry[j] = new XMLParameter("entry",
									distrPar.getValueClass().getName(),
									null, value[i][j].toString(), true);
						}
						vector[i] = new XMLParameter("vector",
								Object.class.getName(),
								null, entry, true);
					}
					return new XMLParameter(distrPar.getName(),
							Object.class.getName(),
							null, vector, true);
				} else {
					return new XMLParameter(distrPar.getName(),
							distrPar.getValueClass().getName(),
							null, valueObj.toString(), true);
				}
			}
			return null;
		}

	}

	/**
	 * This class creates an xml parameter node given a
	 * jmt.gui.common.RoutingStrategy object.
	 */
	protected static class RoutingStrategyWriter {

		static XMLParameter getRoutingStrategyParameter(
				RoutingStrategy routingStrat, CommonModel model,
				Object classKey, Object stationKey) {
			// parameter containing array of empirical entries
			XMLParameter[] innerRoutingPar = null;
			if (routingStrat instanceof ProbabilityRouting) {
				Vector<Object> outputs = model.getForwardConnections(stationKey);
				Map<Object, Double> values = routingStrat.getValues();
				XMLParameter probRoutingPar = null;
				XMLParameter[] empiricalEntries = new XMLParameter[outputs.size()];
				for (int i = 0; i < empiricalEntries.length; i++) {
					XMLParameter stationDest = new XMLParameter("stationName",
							String.class.getName(), null,
							model.getStationName(outputs.get(i)), true);
					String prob = values.get(outputs.get(i)).toString();
					XMLParameter routProb = new XMLParameter("probability",
							Double.class.getName(), null, prob, true);
					empiricalEntries[i] = new XMLParameter("EmpiricalEntry",
							EmpiricalEntry.class.getName(), null,
							new XMLParameter[] { stationDest, routProb }, true);
					empiricalEntries[i].parameterArray = "false";
				}
				probRoutingPar = new XMLParameter("EmpiricalEntryArray",
						EmpiricalEntry.class.getName(), null, empiricalEntries,
						true);
				innerRoutingPar = new XMLParameter[] { probRoutingPar };
			} else if (routingStrat instanceof LoadDependentRouting) {
				Vector<Object> outputs = model.getForwardConnections(stationKey);
				LoadDependentRouting routing = (LoadDependentRouting) routingStrat;
				XMLParameter ldRoutingPar = null;
				XMLParameter[] ranges = new XMLParameter[routing.getAllEmpiricalEntries().size()];
				List<Integer> fromAsList = new ArrayList<Integer>();
				fromAsList.addAll(routing.getAllEmpiricalEntries().keySet());
				Collections.sort(fromAsList);
				int countersRange = 0;
				for (Integer from : fromAsList) {
					XMLParameter fromEntry = new XMLParameter("from", Integer.class.getName(), null, from.toString(), true);
					XMLParameter probLDRoutingPar = null;
					XMLParameter[] empiricalEntries = new XMLParameter[routing.getAllEmpiricalEntries().get(from).size()];
					for (int i = 0; i < empiricalEntries.length; i++) {
						String station = model.getStationName(outputs.get(i));
						XMLParameter stationDest = new XMLParameter("stationName", String.class.getName(), null, station, true);
						Double probability = routing.getAllEmpiricalEntries().get(from).get(outputs.get(i));
						XMLParameter routProb = new XMLParameter("probability", Double.class.getName(), null, probability.toString(), true);
						empiricalEntries[i] = new XMLParameter("EmpiricalEntry", EmpiricalEntry.class.getName(),null, new XMLParameter[] { stationDest,routProb }, true);
						empiricalEntries[i].parameterArray = "false";
					}
					probLDRoutingPar = new XMLParameter("EmpiricalEntryArray", EmpiricalEntry.class.getName(), null, empiricalEntries, true);
					ranges[countersRange] = new XMLParameter("LoadDependentRoutingParameter", strategiesClasspathBase + routingStrategiesSuffix + "LoadDependentRoutingParameter", null,
							new XMLParameter[] { fromEntry,probLDRoutingPar }, true);
					ranges[countersRange].parameterArray = "false";
					countersRange = countersRange + 1;
				}
				ldRoutingPar = new XMLParameter("LoadDependentRoutingParameter", strategiesClasspathBase + routingStrategiesSuffix + "LoadDependentRoutingParameter", null, ranges, true);
				innerRoutingPar = new XMLParameter[] { ldRoutingPar };
			} else if (routingStrat instanceof PowerOfKRouting) {
				PowerOfKRouting rs = ((PowerOfKRouting) routingStrat);
				Integer k = rs.getK();
				Boolean withMemory = rs.isWithMemory();
				XMLParameter param1 = new XMLParameter("k", Integer.class.getName(), null, k.toString(), true);
				XMLParameter param2 = new XMLParameter("withMemory", Boolean.class.getName(), null, withMemory.toString(), true);
				innerRoutingPar = new XMLParameter[] { param1, param2 };
			} else if (routingStrat instanceof WeightedRoundRobinRouting) {
				Vector<Object> outputs = model.getForwardConnections(stationKey);
				Map<Object, Integer> values = ((WeightedRoundRobinRouting) routingStrat).getWeights();
				XMLParameter[] weights = new XMLParameter[outputs.size()];
				for (int i = 0; i < weights.length; i++) {
					XMLParameter stationDest = new XMLParameter("stationName",
							String.class.getName(), null,
							model.getStationName(outputs.get(i)), true);
					String w = values.get(outputs.get(i)).toString();
					XMLParameter weight = new XMLParameter("weight",
							Integer.class.getName(), null, w, true);
					weights[i] = new XMLParameter("WeightEntry",
							strategiesClasspathBase + routingStrategiesSuffix + "WeightEntry",
							 null, new XMLParameter[] { stationDest, weight }, true);
					weights[i].parameterArray = "false";
				}
				XMLParameter weightsParam = new XMLParameter("WeightEntryArray",
						strategiesClasspathBase + routingStrategiesSuffix + "WeightEntry",
						 null, weights, true);
				innerRoutingPar = new XMLParameter[] { weightsParam };
			}
			// creating parameter for empirical strategy: must be null if
			// routing is empirical
			XMLParameter routingStrategy = new XMLParameter(
					routingStrat.getName(), routingStrat.getClassPath(),
					model.getClassName(classKey), innerRoutingPar, true);
			routingStrategy.parameterArray = "false";
			return routingStrategy;
		}

	}

	/**
	 * This class creates an xml parameter node given a
	 * jmt.gui.common.ForkStrategy object.
	 */
	protected static class ForkStrategyWriter {

		static XMLParameter getForkStrategyParameter(
				ForkStrategy forkStrat, CommonModel model, Object classKey,
				Object stationKey) {
			if (forkStrat instanceof ProbabilitiesFork) {
				// parameter containing array of empirical entries
				ArrayList<XMLParameter> outPathEntries = new ArrayList<XMLParameter>();
				XMLParameter[] innerForkPar = null;
				if (forkStrat.getOutDetails() != null) {
					Map<Object, OutPath> outPaths = (Map<Object, OutPath>) forkStrat.getOutDetails();
					for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
						Map<Object, Double> map = (Map) entry.getValue().getOutParameters();
						ArrayList<XMLParameter> empiricalEntries = new ArrayList<XMLParameter>();

						XMLParameter stationDest = new XMLParameter(
								"stationName", String.class.getName(), null,
								model.getStationName(entry.getKey()), true);
						String outProb = entry.getValue().getProbability().toString();
						XMLParameter routProb = new XMLParameter("probability",
								Double.class.getName(), null, outProb, true);
						XMLParameter outUnitProb = new XMLParameter(
								"outUnitProbability",
								EmpiricalEntry.class.getName(), null,
								new XMLParameter[] { stationDest, routProb },
								true);
						outUnitProb.parameterArray = "false";
						for (Map.Entry<Object, Double> subEntry : map
								.entrySet()) {
							if (subEntry.getKey() != null) {
								XMLParameter numOfJobs = new XMLParameter(
										"numbers", String.class.getName(), null,
										subEntry.getKey().toString(), true);
								String numProb = subEntry.getValue().toString();
								XMLParameter forkNumProb = new XMLParameter(
										"probability", Double.class.getName(),
										null, numProb, true);
								XMLParameter numEntry = new XMLParameter(
										"EmpiricalEntry",
										EmpiricalEntry.class.getName(),
										null,
										new XMLParameter[] { numOfJobs, forkNumProb },
										true);
								numEntry.parameterArray = "false";
								empiricalEntries.add(numEntry);
							}
						}
						XMLParameter JobsPerLinkDis = new XMLParameter(
								"JobsPerLinkDis",
								EmpiricalEntry.class.getName(),
								null,
								empiricalEntries
								.toArray(new XMLParameter[empiricalEntries
								                          .size()]), true);
						XMLParameter outPathEntry = new XMLParameter(
								"OutPathEntry",
								jmt.engine.NetStrategies.ForkStrategies.OutPath.class
								.getName(), null, new XMLParameter[] {
										outUnitProb, JobsPerLinkDis }, true);
						outPathEntry.parameterArray = "false";
						outPathEntries.add(outPathEntry);
					}

					XMLParameter probForkPar = new XMLParameter(
							"EmpiricalEntryArray",
							jmt.engine.NetStrategies.ForkStrategies.OutPath.class
							.getName(), null, outPathEntries
							.toArray(new XMLParameter[outPathEntries
							                          .size()]), true);
					innerForkPar = new XMLParameter[] { probForkPar };
				}
				// creating parameter for empirical strategy: must be null if
				// routing is empirical
				XMLParameter forkStrategy = new XMLParameter(
						forkStrat.getName(), forkStrat.getClassPath(),
						model.getClassName(classKey), innerForkPar, true);
				forkStrategy.parameterArray = "false";
				return forkStrategy;
			} else if (forkStrat instanceof CombFork) {
				Map<Object, Double> probs = (Map<Object, Double>) forkStrat.getOutDetails();
				XMLParameter[] combParam = new XMLParameter[probs.size()];
				for (int i = 0; i < probs.size(); i++) {
					XMLParameter stationDest = new XMLParameter(
							"Number of Branches", String.class.getName(), null,
							Integer.toString(i + 1), true);
					String outProb = probs.get(Integer.toString(i + 1)).toString();
					XMLParameter routProb = new XMLParameter("probability",
							Double.class.getName(), null, outProb, true);
					XMLParameter outUnitProb = new XMLParameter(
							"outUnitProbability",
							EmpiricalEntry.class.getName(), null,
							new XMLParameter[] { stationDest, routProb }, true);
					outUnitProb.parameterArray = "false";
					combParam[i] = outUnitProb;
				}
				XMLParameter entryArray = new XMLParameter("EmpiricalEntry",
						EmpiricalEntry.class.getName(), null, combParam, true);
				XMLParameter forkStrategy = new XMLParameter(
						forkStrat.getName(), forkStrat.getClassPath(),
						model.getClassName(classKey),
						new XMLParameter[] { entryArray }, true);
				forkStrategy.parameterArray = "false";
				return forkStrategy;
			} else if (forkStrat instanceof ClassSwitchFork
					|| forkStrat instanceof MultiBranchClassSwitchFork) {
				// parameter containing array of empirical entries
				XMLParameter[] innerForkPar = null;
				if (forkStrat.getOutDetails() != null) {
					Map<Object, OutPath> outPaths = (Map<Object, OutPath>) forkStrat.getOutDetails();
					ArrayList<XMLParameter> classNumArray = new ArrayList<XMLParameter>();
					for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
						Map<Object, Integer> map = (Map) entry.getValue().getOutParameters();
						ArrayList<XMLParameter> classes = new ArrayList<XMLParameter>();
						ArrayList<XMLParameter> numbers = new ArrayList<XMLParameter>();

						XMLParameter stationDest = new XMLParameter(
								"stationName", String.class.getName(), null,
								model.getStationName(entry.getKey()), true);
						for (Object c: model.getClassKeys()) {
							if (map.get(c) != null) {
								XMLParameter jobClass = new XMLParameter(
										"class", String.class.getName(), null,
										model.getClassName(c), true);
								classes.add(jobClass);
								XMLParameter jobNum = new XMLParameter(
										"numberOfJobs", String.class.getName(), null,
										map.get(c).toString(), true);
								numbers.add(jobNum);
							}
						}
						XMLParameter classesPar = new XMLParameter(
								"Classes",
								String.class.getName(),
								null,
								classes
								.toArray(new XMLParameter[classes
								                          .size()]), true);
						XMLParameter NumbersPar = new XMLParameter(
								"Numbers",
								String.class.getName(),
								null,
								numbers
								.toArray(new XMLParameter[numbers
								                          .size()]), true);
						XMLParameter classJobNumEntry = new XMLParameter(
								"OutPathEntry",
								jmt.engine.NetStrategies.ForkStrategies.ClassJobNum.class
								.getName(), null, new XMLParameter[] {
										stationDest, classesPar, NumbersPar }, true);
						classJobNumEntry.parameterArray = "false";
						classNumArray.add(classJobNumEntry);
					}

					XMLParameter classSwitchForkPar = new XMLParameter(
							"ClassJobNumArray",
							jmt.engine.NetStrategies.ForkStrategies.ClassJobNum.class
							.getName(), null, classNumArray
							.toArray(new XMLParameter[classNumArray
							                          .size()]), true);
					innerForkPar = new XMLParameter[] { classSwitchForkPar };
				}
				// creating parameter for empirical strategy: must be null if
				// routing is empirical
				XMLParameter forkStrategy = new XMLParameter(
						forkStrat.getName(), forkStrat.getClassPath(),
						model.getClassName(classKey), innerForkPar, true);
				forkStrategy.parameterArray = "false";
				return forkStrategy;
			} else {
				return null;
			}
		}

	}

	/**
	 * This class creates an xml parameter node given a
	 * jmt.gui.common.JoinStrategy object.
	 */
	protected static class JoinStrategyWriter {

		static XMLParameter getJoinStrategyParameter(JoinStrategy joinStrat,
				CommonModel model, Object classKey, Object stationKey) {
			if (joinStrat instanceof PartialJoin || joinStrat instanceof NormalJoin) {
				XMLParameter numRequired = new XMLParameter("numRequired",
						Integer.class.getName(), null,
						((Integer) joinStrat.getRequiredNum()).toString(), true);
				String name = joinStrat.getName();
				String classPath = joinStrat.getClassPath();
				XMLParameter joinStrategy = new XMLParameter(
						name, classPath, model.getClassName(classKey),
						new XMLParameter[] { numRequired }, true);
				joinStrategy.parameterArray = "false";
				return joinStrategy;
			} else if (joinStrat instanceof GuardJoin) {
				String name = joinStrat.getName();
				String classPath = joinStrat.getClassPath();
				Map<Object, Integer> map = ((GuardJoin) joinStrat).getGuard();
				ArrayList<XMLParameter> classes = new ArrayList<XMLParameter>();
				ArrayList<XMLParameter> numbers = new ArrayList<XMLParameter>();
				for (Object c: model.getClassKeys()) {
					if (map.get(c) != null) {
						XMLParameter jobClass = new XMLParameter(
								"class", String.class.getName(), null,
								model.getClassName(c), true);
						classes.add(jobClass);
						XMLParameter jobNum = new XMLParameter(
								"required", String.class.getName(), null,
								map.get(c).toString(), true);
						numbers.add(jobNum);
					}
				}

				XMLParameter classesPar = new XMLParameter(
						"Classes",
						String.class.getName(),
						null,
						classes.toArray(new XMLParameter[classes.size()]),
						true);
				XMLParameter numbersPar = new XMLParameter(
						"Numbers",
						String.class.getName(),
						null,
						numbers.toArray(new XMLParameter[numbers.size()]),
						true);

				XMLParameter joinStrategy = new XMLParameter(
						name, classPath, model.getClassName(classKey),
						new XMLParameter[] { classesPar, numbersPar }, true);
				joinStrategy.parameterArray = "false";
				return joinStrategy;
			} else {
				return null;
			}
		}

	}

	/**
	 * This class creates an xml parameter node given a
	 * jmt.gui.common.SemaphoreStrategy object.
	 */
	protected static class SemaphoreStrategyWriter {

		static XMLParameter getSemaphoreStrategyParameter(SemaphoreStrategy semaphoreStrat,
				CommonModel model, Object classKey, Object stationKey) {
			XMLParameter threshold = new XMLParameter("threshold",
					Integer.class.getName(), null,
					((Integer) semaphoreStrat.getThreshold()).toString(), true);
			String name = semaphoreStrat.getName();
			String classPath = semaphoreStrat.getClassPath();
			XMLParameter semaphoreStrategy = new XMLParameter(
					name, classPath, model.getClassName(classKey),
					new XMLParameter[] { threshold }, true);
			semaphoreStrategy.parameterArray = "false";
			return semaphoreStrategy;
		}

	}

}
