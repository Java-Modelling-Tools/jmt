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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jmt.common.xml.XSDSchemaLoader;
import jmt.engine.NetStrategies.ImpatienceStrategies.*;
import jmt.engine.log.JSimLogger;
import jmt.engine.log.LoggerParameters;
import jmt.framework.data.MacroReplacer;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.forkStrategies.ClassSwitchFork;
import jmt.gui.common.forkStrategies.CombFork;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.forkStrategies.ProbabilitiesFork;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.joinStrategies.PartialJoin;
import jmt.gui.common.routingStrategies.*;
import jmt.gui.common.semaphoreStrategies.NormalSemaphore;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;

import org.apache.xerces.dom.CharacterDataImpl;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <p>Title: XML Reader</p>
 * <p>Description: Reads model information from an XML file. This class provides
 * methods for model load.</p>
 *
 * @author Bertoli Marco
 *         Date: 27-lug-2005
 *         Time: 13.59.48
 */
public class XMLReader implements XMLConstantNames, CommonConstants {

	protected static TreeMap<String, Object> classes; // Data structure used to map between class name and its key
	protected static TreeMap<String, Object> stations; // Data structure used to map between station name and its key
	protected static TreeMap<String, Object> regions; // Data structure used to map between region name and its key
	protected static HashMap<Object, String> refStations; // Data structure used to hold classes' reference stations
	protected static HashMap<Object, List<String>> modeNameLists; // Data structure used to hold transitions' mode name lists
	protected static HashMap<Object[], Map<String, Double>> empiricalRouting;
	protected static HashMap<Object[], Map<String, Double>> empiricalLDRouting;
	protected static HashMap<Object[], Map<String, Integer>> wrrRouting;
	protected static HashMap<Object[], Map<String, OutPath>> empiricalFork;
	protected static HashMap<Object[], Map<String, Double>> combFork;
	protected static HashMap<Object[], Integer> enablingConditionMap;
	protected static HashMap<Object[], Integer> inhibitingConditionMap;
	protected static HashMap<Object[], Integer> firingOutcomeMap;

	/**defines the default logger (used to report errors and information for debugging purposes).*/
	private static final jmt.engine.log.JSimLogger debugLog = jmt.engine.log.JSimLogger.getLogger(JSimLogger.STD_LOGGER);

	/**defines matching between engine representation and gui names for drop rules.*/
	protected static final Map<String, String> DROP_RULES_MAPPING;

	static {
		HashMap<String, String> temp = new HashMap<String, String>();
		temp.put("drop", FINITE_DROP);
		temp.put("BAS blocking", FINITE_BLOCK);
		temp.put("waiting queue", FINITE_WAITING);
		temp.put("retrial", FINITE_RETRIAL);
		DROP_RULES_MAPPING = Collections.unmodifiableMap(temp);
	}

	// Variables used with caching purpose to improve reading speed
	protected static Map<String, Distribution> engineToGuiDistr = null;
	protected static Map<String, RoutingStrategy> engineToGuiRouting = null;

	protected static Map<String, ForkStrategy> engineToGuiFork = null;
	protected static Map<String, JoinStrategy> engineToGuiJoin = null;
	protected static Map<String, SemaphoreStrategy> engineToGuiSemaphore = null;

	protected static final String psStrategy = "jmt.engine.NetStrategies.PSStrategy";
	protected static final String queuePut = "jmt.engine.NetStrategies.QueuePutStrategy";
	protected static final String queueGet = "jmt.engine.NetStrategies.QueueGetStrategy";
	protected static final String serviceStrategy = "jmt.engine.NetStrategies.ServiceStrategy";
	protected static final String distributionContainer = "jmt.engine.random.DistributionContainer";

	/**
	 * Loads a model saved in an XML file, given the name of the file. If specified file
	 * is a jmodel archive, extracts model informations from it and uses them to reconstruct
	 * the model. This method is provided to be used with JSIM
	 * @param fileName name of the file to be opened
	 * @param model data structure where model should be created (a new data structure
	 * is the best choice)
	 * @return true iff model was recognized and loaded, false otherwise
	 */
	public static boolean loadModel(String fileName, CommonModel model) {
		Document doc = loadXML(fileName, XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_MODEL_DEFINITION));
		if (doc.getElementsByTagName(XML_DOCUMENT_ROOT).getLength() != 0) {
			// Document is a simulation model
			parseXML(doc, model);
			return true;
		} else if (doc.getElementsByTagName(GuiXMLConstants.XML_ARCHIVE_DOCUMENT_ROOT).getLength() != 0) {
			// Document is an archive
			parseXML(XMLArchiver.getSimFromArchiveDocument(doc), model);
			return true;
		}
		return false;
	}

	/**
	 * Loads a model saved in an XML file, given the handler to the file. If specified file
	 * is a jmodel archive, extracts model informations from it and uses them to reconstruct
	 * the model. This method is provided to be used with JSIM
	 * @param xmlFile handler to the file to be opened
	 * @param model data structure where model should be created (a new data structure
	 * is the best choice)
	 * @return true iff model was recognized and loaded, false otherwise
	 */
	public static boolean loadModel(File xmlFile, CommonModel model) {
		return loadModel(xmlFile.getAbsolutePath(), model);
	}

	/**
	 * Parses given XML Document to reconstruct simulation model.
	 * @param root root of document to be parsed
	 * @param model data model to be elaborated
	 */
	public static void parseXML(Element root, CommonModel model) {
		// Gets optional parameter simulation seed
		String seed = root.getAttribute(XML_A_ROOT_SEED);
		if (seed != null && seed != "") {
			model.setUseRandomSeed(Boolean.valueOf(false));
			model.setSimulationSeed(Long.valueOf(seed));
		} else {
			model.setUseRandomSeed(Boolean.valueOf(true));
		}

		// Gets optional parameter maximum time
		String maxTime = root.getAttribute(XML_A_ROOT_DURATION);
		if (maxTime != null && maxTime != "") {
			model.setMaximumDuration(Double.valueOf(maxTime));
		} else {
			model.setMaximumDuration(Double.valueOf(-1));
		}

		// Gets optional parameter maximum simulated time
		String maxSimulated = root.getAttribute(XML_A_ROOT_SIMULATED);
		if (maxSimulated != null && maxSimulated != "") {
			model.setMaxSimulatedTime(Double.valueOf(maxSimulated));
		} else {
			model.setMaxSimulatedTime(Double.valueOf(-1));
		}

		// Gets optional parameter polling interval
		String polling = root.getAttribute(XML_A_ROOT_POLLING);
		if (polling != null && polling != "") {
			model.setPollingInterval(Double.valueOf(polling));
		}

		// Gets optional parameter maximum samples
		String maxSamples = root.getAttribute(XML_A_ROOT_MAXSAMPLES);
		if (maxSamples != null && maxSamples != "") {
			model.setMaxSimulationSamples(Integer.valueOf(maxSamples));
		}

		// Gets optional parameter disable statistic
		String disableStatistic = root.getAttribute(XML_A_ROOT_DISABLESTATISTIC);
		if (disableStatistic != null && disableStatistic != "") {
			model.setDisableStatistic(Boolean.valueOf(disableStatistic));
		}

		// Gets optional parameter maximum events
		String maxEvents = root.getAttribute(XML_A_ROOT_MAXEVENTS);
		if (maxEvents != null && maxEvents != "") {
			model.setMaxSimulationEvents(Integer.valueOf(maxEvents));
		} else {
			model.setMaxSimulationEvents(Integer.valueOf(-1));
		}

		/* Gets optional parameters log path, replace policy, and delimiter
		 * Values here should correspond to SimLoader values (Ctrl+F for them) */
		String logPath = root.getAttribute(XML_A_ROOT_LOGPATH);
		if (logPath != null && logPath != "") {
			File dir = new File(logPath);
			if (dir.isDirectory()) {
				model.setLoggingGlbParameter("path", dir.getAbsolutePath());
			} else {
				model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
			}
		} else {
			model.setLoggingGlbParameter("path", MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR));
		}
		String logReplaceMode = root.getAttribute(XML_A_ROOT_LOGREPLACE);
		if (logReplaceMode != null && logReplaceMode != "") {
			model.setLoggingGlbParameter("autoAppend", logReplaceMode);
		} else {
			model.setLoggingGlbParameter("autoAppend", Defaults.get("loggerAutoAppend"));
		}
		String logDelimiter = root.getAttribute(XML_A_ROOT_LOGDELIM);
		if (logDelimiter != null && logDelimiter != "") {
			model.setLoggingGlbParameter("delim", logDelimiter);
		} else {
			model.setLoggingGlbParameter("delim", Defaults.get("loggerDelimiter"));
		}
		String logDecimalSeparator = root.getAttribute(XML_A_ROOT_LOGDECIMALSEPARATOR);
		if (logDecimalSeparator != null && logDecimalSeparator != "") {
			model.setLoggingGlbParameter("decimalSeparator", logDecimalSeparator);
		} else {
			model.setLoggingGlbParameter("decimalSeparator", Defaults.get("loggerDecimalSeparator"));
		}

		parseClasses(root, model);
		empiricalRouting = new HashMap<Object[], Map<String, Double>>();
		empiricalLDRouting = new HashMap<Object[], Map<String, Double>>();
		wrrRouting = new HashMap<Object[], Map<String, Integer>>();
		empiricalFork = new HashMap<Object[], Map<String, OutPath>>();
		combFork = new HashMap<Object[], Map<String, Double>>();
		enablingConditionMap = new HashMap<Object[], Integer>();
		inhibitingConditionMap = new HashMap<Object[], Integer>();
		firingOutcomeMap = new HashMap<Object[], Integer>();
		parseStations(root, model);
		parseConnections(root, model);
		parseBlockingRegions(root, model);
		parseMeasures(root, model);
		parsePreloading(root, model);
		// Set reference station for each class
		Object[] keys = refStations.keySet().toArray();
		for (Object key : keys) {
			String name = refStations.get(key);
			if (STATION_TYPE_FORK.equals(name)
					|| STATION_TYPE_CLASSSWITCH.equals(name)
					|| STATION_TYPE_SCALER.equals(name)
					|| STATION_TYPE_TRANSITION.equals(name)) {
				model.setClassRefStation(key, name);
			} else {
				model.setClassRefStation(key, stations.get(name));
			}
		}
		// Sets correct station key into every empiricalRouting element
		// Now each key is an Object[] where (0) is station key and (1) class key
		keys = empiricalRouting.keySet().toArray();
		for (Object key : keys) {
			Object[] dualkey = (Object[]) key;
			RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(dualkey[0], dualkey[1]);
			Map<Object, Double> routing = rs.getValues();
			Map<String, Double> values = empiricalRouting.get(key);
			Object[] names = values.keySet().toArray();
			for (Object name : names) {
				routing.put(stations.get(name), values.get(name));
			}
		}
		keys = empiricalLDRouting.keySet().toArray();
		for (Object key : keys) {
			Object[] triplekey = (Object[]) key;
			LoadDependentRouting ldr = (LoadDependentRouting) model.getRoutingStrategy(triplekey[0], triplekey[1]);
			Map<String, Double> values = empiricalLDRouting.get(key);
			Object[] names = values.keySet().toArray();
			for (Object name : names) {
				ldr.addEmpiricalEntry((Integer) triplekey[2], stations.get(name), values.get(name));
			}
		}
		keys = wrrRouting.keySet().toArray();
		for (Object key : keys) {
			Object[] dualkey = (Object[]) key;
			WeightedRoundRobinRouting rs = (WeightedRoundRobinRouting) model.getRoutingStrategy(dualkey[0], dualkey[1]);
			Map<Object, Integer> routing = rs.getWeights();
			Map<String, Integer> values = wrrRouting.get(key);
			Object[] names = values.keySet().toArray();
			for (Object name : names) {
				routing.put(stations.get(name), values.get(name));
			}
		}
		keys = empiricalFork.keySet().toArray();
		for (Object key : keys) {
			Object[] dualkey = (Object[]) key;
			ForkStrategy fs = (ForkStrategy) model.getForkStrategy(dualkey[0], dualkey[1]);
			Map<Object, OutPath> outPaths = (Map<Object, OutPath>) fs.getOutDetails();
			Map<String, OutPath> values = empiricalFork.get(key);
			Object[] names = values.keySet().toArray();
			for (Object name : names) {
				outPaths.put(stations.get(name), values.get(name));
			}
		}
		keys = combFork.keySet().toArray();
		for (Object key : keys) {
			Object[] dualkey = (Object[]) key;
			ForkStrategy fs = (ForkStrategy) model.getForkStrategy(dualkey[0], dualkey[1]);
			Map<Object, Double> fork = (Map<Object, Double>) fs.getOutDetails();
			Map<String, Double> values = combFork.get(key);
			Object[] names = values.keySet().toArray();
			for (Object name : names) {
				fork.put(name, values.get(name));
			}
		}
		keys = enablingConditionMap.keySet().toArray();
		for (Object key : keys) {
			Object[] quadkey = (Object[]) key;
			Integer value = enablingConditionMap.get(key);
			model.setEnablingCondition(quadkey[0], ((Integer) quadkey[1]).intValue(),
					stations.get((String) quadkey[2]), quadkey[3], value);
		}
		keys = inhibitingConditionMap.keySet().toArray();
		for (Object key : keys) {
			Object[] quadkey = (Object[]) key;
			Integer value = inhibitingConditionMap.get(key);
			model.setInhibitingCondition(quadkey[0], ((Integer) quadkey[1]).intValue(),
					stations.get((String) quadkey[2]), quadkey[3], value);
		}
		keys = firingOutcomeMap.keySet().toArray();
		for (Object key : keys) {
			Object[] quadkey = (Object[]) key;
			Integer value = firingOutcomeMap.get(key);
			model.setFiringOutcome(quadkey[0], ((Integer) quadkey[1]).intValue(),
					stations.get((String) quadkey[2]), quadkey[3], value);
		}
	}

	/**
	 * Parses given XML Document to reconstruct simulation model.
	 * @param xml Document to be parsed
	 * @param model data model to be elaborated
	 */
	public static void parseXML(Document xml, CommonModel model) {
		parseXML(xml.getDocumentElement(), model);
	}

	// --- Helper methods ----------------------------------------------------------------------------
	/**
	 * Helper method that searches for first text node, between all children of current node
	 * and returns its value. (This is needed to garbage out all comments)
	 * @param elem root node to begin search
	 * @return parsed text if found, otherwise null
	 */
	protected static String findText(Node elem) {
		NodeList tmp = elem.getChildNodes();
		for (int j = 0; j < tmp.getLength(); j++) {
			if (tmp.item(j).getNodeType() == Node.TEXT_NODE) {
				return tmp.item(j).getNodeValue();
			}
		}
		return null;
	}

	// -----------------------------------------------------------------------------------------------

	// --- Class section -----------------------------------------------------------------------------
	/**
	 * Parses userclasses information. Note that distributions for open class will be set lately
	 * and reference station information is stored into refStations data structure as will
	 * be used later
	 * @param root root element of XML Document
	 * @param model data structure where all properties have to be set
	 */
	protected static void parseClasses(Element root, CommonModel model) {
		// Initialize classes and refStations data structure
		classes = new TreeMap<String, Object>();
		refStations = new HashMap<Object, String>();
		NodeList nodeclasses = root.getElementsByTagName(XML_E_CLASS);
		// Now scans all elements
		Element currclass;
		int type;
		Integer priority;
		Integer customers;
		String name;
		Object key;
		for (int i = 0; i < nodeclasses.getLength(); i++) {
			currclass = (Element) nodeclasses.item(i);
			name = currclass.getAttribute(XML_A_CLASS_NAME);
			type = currclass.getAttribute(XML_A_CLASS_TYPE).equals("open") ? CLASS_TYPE_OPEN : CLASS_TYPE_CLOSED;
			priority = Defaults.getAsInteger("classPriority");
			customers = Defaults.getAsInteger("classPopulation");

			// As these elements are not mandatory, sets them by default, then tries to parses them
			String tmp = currclass.getAttribute(XML_A_CLASS_CUSTOMERS);
			if (tmp != null && tmp != "") {
				customers = Integer.valueOf(tmp);
			}

			tmp = currclass.getAttribute(XML_A_CLASS_PRIORITY);
			if (tmp != null && tmp != "") {
				priority = Integer.valueOf(tmp);
			}

			// Now adds user class. Note that distribution will be set lately.
			key = model.addClass(name, type, priority, customers, Defaults.getAsNewInstance("classDistribution"));
			// Stores reference station as will be set lately (when we will have stations key)
			refStations.put(key, currclass.getAttribute(XML_A_CLASS_REFSOURCE));
			// Creates mapping class-name -> key into stations data structure
			classes.put(name, key);
		}
	}

	// -----------------------------------------------------------------------------------------------

	// --- Station section ---------------------------------------------------------------------------
	/**
	 * Parses all station related informations and puts them into data structure
	 * @param root root element of XML Document
	 * @param model data structure where all properties have to be set
	 */
	protected static void parseStations(Element root, CommonModel model) {
		// Initialize stations and modeNameLists data structure
		stations = new TreeMap<String, Object>();
		modeNameLists = new HashMap<Object, List<String>>();
		NodeList nodestations = root.getElementsByTagName(XML_E_STATION);
		Object key;
		Element station;
		String type, name;
		NodeList sections;
		// For every station, identifies its type and parses its parameters
		for (int i = 0; i < nodestations.getLength(); i++) {
			station = (Element) nodestations.item(i);
			sections = station.getElementsByTagName(XML_E_STATION_SECTION);
			type = getStationType(station);
			name = station.getAttribute(XML_A_STATION_NAME);
			// Puts station into data structure
			key = model.addStation(name, type);
			// Creates mapping station-name -> key into stations data structure
			stations.put(name, key);
			// Handles source (set distribution)
			if (type.equals(STATION_TYPE_SOURCE)) {
				parseSourceSection((Element) sections.item(0), model, key, name);
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_TERMINAL)) {
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_ROUTER)) {
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_DELAY)) {
				parseDelaySection((Element) sections.item(1), model, key);
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_SERVER)) {
				parseQueueSection((Element) sections.item(0), model, key);
				parseServerSection((Element) sections.item(1), model, key);
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_FORK)) {
				parseQueueSection((Element) sections.item(0), model, key);
				parseForkSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_JOIN)) {
				parseJoinSection((Element) sections.item(0), model, key);
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_LOGGER)) {
				parseLoggerSection((Element) sections.item(1), model, key);
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_CLASSSWITCH)) {
				parseQueueSection((Element) sections.item(0), model, key);
				parseClassSwitchSection((Element) sections.item(1), model, key);
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_SEMAPHORE)) {
				parseSemaphoreSection((Element) sections.item(0), model, key);
				parseRouterSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_SCALER)) {
				parseJoinSection((Element) sections.item(0), model, key);
				parseForkSection((Element) sections.item(2), model, key);
			} else if (type.equals(STATION_TYPE_PLACE)) {
				parseStorageSection((Element) sections.item(0), model, key);
			} else if (type.equals(STATION_TYPE_TRANSITION)) {
				parseEnablingSection((Element) sections.item(0), model, key);
				parseTimingSection((Element) sections.item(1), model, key);
				parseFiringSection((Element) sections.item(2), model, key);
			}
		}
	}

	/**
	 * Parses all informations regarding Source section. If this source is reference class
	 * for any kind of open class, uses service time informations stored here to set distribution
	 * for this class.
	 * @param section input section of source station
	 * @param model link to data structure
	 * @param key key of search for this source station into data structure
	 * @param stationName Name of current station. This is used to correctly set reference station
	 * distribution. That cannot be derived from model.getStationName(key) as JSim can change
	 * source name upon opening a model stored with JModel.
	 */
	protected static void parseSourceSection(Element section, CommonModel model, Object key, String stationName) {
		Element parameter = (Element) section.getElementsByTagName(XML_E_PARAMETER).item(0);
		// Now parses Service Distribution
		Map<String, Node> distributions = parseParameterRefclassArray(parameter);
		// Assign distribution for a class only if current source is its reference station
		Set<String> classNames = distributions.keySet();
		for (String className : classNames) {
			// If current class has this station as reference source and is open...
			if (refStations.get(classes.get(className)) != null && refStations.get(classes.get(className)).equals(stationName)
					&& model.getClassType(classes.get(className)) == CLASS_TYPE_OPEN) {
				Object classkey = classes.get(className);
				model.setClassDistribution(classkey, parseServiceStrategy((Element) distributions.get(className)));
				model.setClassRefStation(classkey, key);
				// Removes this class from refStations as it was already handled
				refStations.remove(classkey);
			}
		}
	}

	private static void parseAndSetImpatienceParameters(CommonModel model, Object stationKey,
																											Map<String, Node> classNameToNodeMap) {
		String[] classNames = getClassNamesFromMap(classNameToNodeMap);

		// Sets the impatience strategy in the CommonModel. Implementation borrowed from parseServerSection().
		for (String className : classNames) {
			Object classKey = classes.get(className);
			Element impatienceElement = (Element) classNameToNodeMap.get(className);
			String impatienceString = impatienceElement.getAttribute(XML_A_SUBPARAMETER_NAME);
			ImpatienceType impatienceType = ImpatienceType.getType(impatienceString);

			switch(impatienceType) {
				case RENEGING:
					// Set the reneging strategy in the model
					Distribution renegingDistribution = (Distribution) parseServiceStrategy(impatienceElement);
					RenegingParameter renegingParameter = new RenegingParameter(renegingDistribution);
					setImpatienceParameterAndType(model, stationKey, classKey, renegingParameter, impatienceType);
					break;

				case BALKING:
					// Set the balking strategy in the model
					NodeList nodeList = impatienceElement.getChildNodes();
					if (nodeList instanceof Element) {
						int indexOfLDStrategyNode = 1;
						int indexOfPriorityNode = 3;

						// Extract the values from the XML
						Element ldStrategyElement = (Element) nodeList.item(indexOfLDStrategyNode);
						LDStrategy ldStrategy = (LDStrategy) parseServiceStrategy(ldStrategyElement);
						Element booleanElement = (Element) nodeList.item(indexOfPriorityNode);
						boolean priorityActivated = parseBooleanElement(booleanElement);

						// Set the values in the model
						BalkingParameter balkingParameter = new BalkingParameter(ldStrategy);
						balkingParameter.setPriorityActivated(priorityActivated);
						setImpatienceParameterAndType(model, stationKey, classKey, balkingParameter, impatienceType);
					}
					break;

				case NONE:
					// This case occurs when a particular class has no impatience selected.
					setImpatienceParameterAndType(model, stationKey, classKey, null, impatienceType);
					break;
			}
		}
	}

	private static boolean parseBooleanElement(Element booleanElement) {
		NodeList nodeList = booleanElement.getChildNodes();
		int indexOfValueNode = 1;
		Node booleanNode = nodeList.item(indexOfValueNode);
		CharacterDataImpl dataNode = (CharacterDataImpl) booleanNode.getFirstChild();
		return Boolean.valueOf(dataNode.getData());
	}

	private static String[] getClassNamesFromMap(Map<String, Node> classNameToNodeMap) {
		return classNameToNodeMap.keySet().toArray(new String[classNameToNodeMap.size()]);
	}

	private static void setImpatienceParameterAndType(CommonModel model, Object stationKey,
																										Object classKey, ImpatienceParameter impatienceParameter, ImpatienceType impatienceType) {
		model.setImpatienceParameter(stationKey, classKey, impatienceParameter);
		model.setImpatienceType(stationKey, classKey, impatienceType);
	}
	/**
	 * Parses all informations regarding Queue section.
	 * @param section input section of queue or delay station
	 * @param model link to data structure
	 * @param key key of search for this queue or delay station into data structure
	 */
	protected static void parseQueueSection(Element section, CommonModel model, Object key) {
		NodeList parameters = section.getElementsByTagName(XML_E_PARAMETER);
		Element curr;
		String name, classpath;
		Map<String, Node> putStrategy = null;
		Map<String, Node> dropRules = null;
		Map<String, Node> retrialDistributions = null;
        String pollingType = null;
		for (int i = 0; i < parameters.getLength(); i++) {
			curr = (Element) parameters.item(i);
			name = curr.getAttribute(XML_A_PARAMETER_NAME);
			classpath = curr.getAttribute(XML_A_PARAMETER_CLASSPATH);
			if (classpath.equals(queuePut)) {
				putStrategy = parseParameterRefclassArray(curr);
			} else if (name.equals("FCFSstrategy")) {
				pollingType = name;
				NodeList subparams = curr.getElementsByTagName(XML_E_SUBPARAMETER);
				for (int y = 0; y < subparams.getLength(); y++) {
					if (((Element) subparams.item(y)).getAttribute(XML_A_PARAMETER_NAME).equals("pollingKValue")) {
						Integer pollingKValue = Integer.valueOf(findText(curr.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
						model.setStationPollingServerKValue(key, pollingKValue);
					}
				}
			} else if (name.equals("size")) {
				Integer capacity = Integer.valueOf(findText(curr.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				model.setStationQueueCapacity(key, capacity);
			} else if (name.equals("maxRunning")) {
				Integer maxRunning = Integer.valueOf(findText((curr.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0))));
				model.setStationMaxRunning(key, maxRunning);
			} else if (name.equals("serverPreemptive")) {
				Boolean serverPreemptive = Boolean.valueOf(findText((curr.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0))));
				model.setStationServerPreemptive(key, serverPreemptive);
			} else if (name.equals("pollingKValue")) {
				Integer pollingKValue = Integer.valueOf(findText(curr.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				model.setStationPollingServerKValue(key, pollingKValue);
			} else if (name.equals("dropStrategies")) {
				dropRules = parseParameterRefclassArray(curr);
			} else if (name.equals("retrialDistributions")) {
				retrialDistributions = parseParameterRefclassArray(curr);
			}
			else if (name.equals(ImpatienceType.IMPATIENCE.getDisplayName())) {
				// Retrieves all impatience subParameters and sets them in the model
				Map<String, Node> classNameToNodeMap = parseParameterRefclassArray(curr);
				parseAndSetImpatienceParameters(model, key, classNameToNodeMap);
			}
		}

		if (putStrategy != null) {
			Object[] classNames = putStrategy.keySet().toArray();
			for (Object className : classNames) {
				String strategy = ((Element) putStrategy.get(className)).getAttribute(XML_A_SUBPARAMETER_CLASSPATH);
				// Takes away classpath from put strategy name
				strategy = strategy.substring(strategy.lastIndexOf(".") + 1, strategy.length());
				// Now sets correct queue strategy, given queueput policies
				if (strategy.equals("TailStrategy")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
					model.setQueueStrategy(key, classes.get(className), QUEUE_STRATEGY_FCFS);
				} else if (strategy.equals("TailStrategyPriority")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
					model.setQueueStrategy(key, classes.get(className), QUEUE_STRATEGY_FCFS);
				} else if (strategy.equals("HeadStrategy")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
					model.setQueueStrategy(key, classes.get(className), QUEUE_STRATEGY_LCFS);
				} else if (strategy.equals("HeadStrategyPriority")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
					model.setQueueStrategy(key, classes.get(className), QUEUE_STRATEGY_LCFS);
				} else if (strategy.equals("LCFSPRStrategy")) {
					model.setStationQueueStrategy(key, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE);
					model.setQueueStrategy(key, classes.get(className), CommonConstants.QUEUE_STRATEGY_LCFS_PR);
				} else if (strategy.equals("RandStrategy")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_RAND);
				} else if (strategy.equals("RandStrategyPriority")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_RAND);
				} else if (strategy.equals("SJFStrategy")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_SJF);
				} else if (strategy.equals("SJFStrategyPriority")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_SJF);
				} else if (strategy.equals("LJFStrategy")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_LJF);
				} else if (strategy.equals("LJFStrategyPriority")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_LJF);
				} else if (strategy.equals("SEPTStrategy")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_SEPT);
				} else if (strategy.equals("SEPTStrategyPriority")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_SEPT);
				} else if (strategy.equals("LEPTStrategy")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_LEPT);
				} else if (strategy.equals("LEPTStrategyPriority")) {
					model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
					model.setQueueStrategy(key,classes.get(className), QUEUE_STRATEGY_LEPT);
				}
			}
		}

		if (pollingType != null) {
		    String getStrategy = pollingType.substring(pollingType.lastIndexOf(".") + 1);
			if (getStrategy.equals("LimitedPollingGetStrategy")) {
				model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_POLLING);
				model.setStationPollingServerBoolean(key, true);
				model.setStationPollingServerType(key, STATION_QUEUE_STRATEGY_POLLING_LIMITED);
			} else if (getStrategy.equals("GatedPollingGetStrategy")) {
				model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_POLLING);
				model.setStationPollingServerBoolean(key, true);
				model.setStationPollingServerType(key, STATION_QUEUE_STRATEGY_POLLING_GATED);
			} else if (getStrategy.equals("ExhaustivePollingGetStrategy")) {
				model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_POLLING);
				model.setStationPollingServerBoolean(key, true);
				model.setStationPollingServerType(key, STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);
			}
	}

		// Decodes drop rules
		if (dropRules != null) {
			Object[] classNames = dropRules.keySet().toArray();
			for (Object className : classNames) {
				String rule = findText(((Element) dropRules.get(className)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
				model.setDropRule(key, classes.get(className), DROP_RULES_MAPPING.get(rule));
				if (rule.equals("retrial") && retrialDistributions != null) {
					Distribution distribution = (Distribution) parseServiceStrategy((Element) retrialDistributions.get(className));
					model.setRetrialDistribution(key, classes.get(className), distribution);
				}
			}
		}
	}

	/**
	 * Parses all informations regarding Delay section.
	 * @param section service section of delay station
	 * @param model link to data structure
	 * @param key key of search for this delay station into data structure
	 */
	protected static void parseDelaySection(Element section, CommonModel model, Object key) {
		Element parameter = (Element) section.getElementsByTagName(XML_E_PARAMETER).item(0);
		// Retrieves all distributions subParameters
		Map<String, Node> distributions = parseParameterRefclassArray(parameter);
		Object[] classNames = distributions.keySet().toArray();
		// Sets service time distributions
		for (Object className : classNames) {
			model.setServiceTimeDistribution(key, classes.get(className), parseServiceStrategy((Element) distributions.get(className)));
		}
	}

	/**
	 * Parses all informations regarding Server section.
	 * @param section service section of queue station
	 * @param model link to data structure
	 * @param key key of search for this queue station into data structure
	 */
	protected static void parseServerSection(Element section, CommonModel model, Object key) {
		String sectionName = section.getAttribute(XML_A_STATION_SECTION_CLASSNAME);
		if (CLASSNAME_PSSERVER.equals(sectionName)) {
			model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_PSSERVER);
			for (Object classKey : classes.values()) {
				model.setQueueStrategy(key, classKey, QUEUE_STRATEGY_PS);
			}
		} else if (CLASSNAME_LKPSERVER.equals(sectionName)) {
			model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_POLLING);
			model.setStationPollingServerBoolean(key, true);
			model.setStationPollingServerType(key, STATION_QUEUE_STRATEGY_POLLING_LIMITED);
		} else if (CLASSNAME_GATEDPSERVER.equals(sectionName)) {
			model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_POLLING);
			model.setStationPollingServerBoolean(key, true);
			model.setStationPollingServerType(key, STATION_QUEUE_STRATEGY_POLLING_GATED);
		} else if (CLASSNAME_EXHAUSTIVEPSERVER.equals(sectionName)) {
			model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_POLLING);
			model.setStationPollingServerBoolean(key, true);
			model.setStationPollingServerType(key, STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE);
		} else if (CLASSNAME_PREEMPTIVESERVER.equals(sectionName)) {
			model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_PREEMPTIVE);
		} else if (CLASSNAME_SERVER.equals(sectionName)) {
			model.setStationQueueStrategy(key, STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
		}

		NodeList parameters = section.getElementsByTagName(XML_E_PARAMETER);
		Element curr;
		String name, classpath;
		for (int i = 0; i < parameters.getLength(); i++) {
			curr = (Element) parameters.item(i);
			name = curr.getAttribute(XML_A_PARAMETER_NAME);
			classpath = curr.getAttribute(XML_A_PARAMETER_CLASSPATH);
			if (name.equals("maxJobs")) {
				// Sets number of servers
				Integer number = Integer.valueOf(findText(curr.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				model.setStationNumberOfServers(key, number);
			} else if (name.equals("serverPreemptive")) {
				Boolean serverPreemptive = Boolean.valueOf(findText((curr.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0))));
				model.setStationServerPreemptive(key, serverPreemptive);
			} else if (name.equals("switchoverStrategy")) {
				Map<String, Node> distributions = parseParameterRefclassArray((Element) parameters.item(i));
				Object[] classNames = distributions.keySet().toArray();
				for (Object className : classNames) {
					model.setStationPollingSwitchoverDistribution(key, classes.get(className), parseServiceStrategy((Element) distributions.get(className)));
				}
			} else if (classpath.equals(serviceStrategy)) {
				// Retrieves all distributions subParameters
				Map<String, Node> distributions = parseParameterRefclassArray((Element) parameters.item(i));
				Object[] classNames = distributions.keySet().toArray();
				// Sets service time distributions
				for (Object className : classNames) {
					model.setServiceTimeDistribution(key, classes.get(className), parseServiceStrategy((Element) distributions.get(className)));
				}
			} else if (classpath.equals(psStrategy)) {
				// Retrieves all PS strategies subParameters
				Map<String, Node> psStrategies = parseParameterRefclassArray((Element) parameters.item(i));
				Object[] classNames = psStrategies.keySet().toArray();
				// Sets all PS strategies
				for (Object className : classNames) {
					String strategy = ((Element) psStrategies.get(className)).getAttribute(XML_A_SUBPARAMETER_CLASSPATH);
					strategy = strategy.substring(strategy.lastIndexOf(".") + 1, strategy.length());
					if (strategy.equals("EPSStrategy")) {
						model.setQueueStrategy(key, classes.get(className), QUEUE_STRATEGY_PS);
					} else if (strategy.equals("GPSStrategy")) {
						model.setQueueStrategy(key, classes.get(className), QUEUE_STRATEGY_GPS);
					} else if (strategy.equals("DPSStrategy")) {
						model.setQueueStrategy(key, classes.get(className), QUEUE_STRATEGY_DPS);
					}
				}
			} else if (name.equals("serviceWeights")) {
				// Retrieves all service weights subParameters
				Map<String, Node> serviceWeights = parseParameterRefclassArray((Element) parameters.item(i));
				Object[] classNames = serviceWeights.keySet().toArray();
				// Sets all service weights
				for (Object className : classNames) {
					Double weight = Double.valueOf(findText(((Element) serviceWeights.get(className)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
					model.setServiceWeight(key, classes.get(className), weight);
				}
			}
		}
	}

	/**
	 * Parses a Parameter array and returns a Map of ClassName -> subParameter
	 * @param parameterNode
	 * @return a Map of ClassName -> subParameter
	 */
	protected static Map<String, Node> parseParameterRefclassArray(Element parameterNode) {
		// For some reasons getElementsByTagName returns only first service time strategy.
		// So we need to look every children of parameterNode node.
		TreeMap<String, Node> res = new TreeMap<String, Node>();
		Node child = parameterNode.getFirstChild();
		String refClass;

		// This manual parsing is a bit unclean but works well and it is really fast.
		// I was forced to do in this way for the problem said before.
		while (child != null) {
			while (child != null && (child.getNodeType() != Node.ELEMENT_NODE || !child.getNodeName().equals(XML_E_PARAMETER_REFCLASS))) {
				child = child.getNextSibling();
			}

			if (child == null) {
				break;
			}
			refClass = findText(child);
			// Now finds first subParameter element
			while (child != null && (child.getNodeType() != Node.ELEMENT_NODE || !child.getNodeName().equals(XML_E_SUBPARAMETER))) {
				child = child.getNextSibling();
			}

			if (child == null) {
				break;
			}

			// Puts className and subParameter into destination Map
			res.put(refClass, child);
			child = child.getNextSibling();
		}

		return res;
	}

	/**
	 * Parses a parameter array and returns Vector of found subParameters
	 * @param parameterNode
	 * @return Vector with found subParameters
	 */
	protected static Vector<Node> parseParameterArray(Element parameterNode) {
		Vector<Node> ret = new Vector<Node>();
		Node child = parameterNode.getFirstChild();

		while (child != null) {
			while (child != null && (child.getNodeType() != Node.ELEMENT_NODE || !child.getNodeName().equals(XML_E_SUBPARAMETER))) {
				child = child.getNextSibling();
			}

			if (child == null) {
				break;
			}
			// Puts found subParameter into destination Vector
			ret.add(child);
			child = child.getNextSibling();
		}

		return ret;
	}

	/**
	 * Parses router section
	 * @param section router section
	 * @param model data structure
	 * @param key station's key
	 */
	protected static void parseRouterSection(Element section, CommonModel model, Object key) {
		Element parameter = (Element) section.getElementsByTagName(XML_E_PARAMETER).item(0);
		Map<String, Node> routing = parseParameterRefclassArray(parameter);
		Object[] classNames = routing.keySet().toArray();
		String className;

		// Creates a Map of Name --> Routing Strategy if needed
		if (engineToGuiRouting == null) {
			engineToGuiRouting = new TreeMap<String, RoutingStrategy>();
			RoutingStrategy[] allRS = RoutingStrategy.findAll();
			for (RoutingStrategy element : allRS) {
				engineToGuiRouting.put(element.getClass().getName(), element);
			}
		}

		Object[] routStratKeys = engineToGuiRouting.keySet().toArray();
		for (Object className2 : classNames) {
			className = ((Element) routing.get(className2)).getAttribute(XML_A_SUBPARAMETER_CLASSPATH);
			// Searches all available routing strategy to find the one saved
			for (Object routStratKey : routStratKeys) {
				if (className.equals(engineToGuiRouting.get(routStratKey).getClassPath())) {
					model.setRoutingStrategy(key, classes.get(className2), engineToGuiRouting.get(routStratKey).clone());
				}
			}

			// Treat particular case of Empirical (Probabilities) Routing
			RoutingStrategy rs = (RoutingStrategy) model.getRoutingStrategy(key, classes.get(className2));
			if (rs instanceof ProbabilityRouting) {
				// Creates a Vector of all empirical entris. Could not be done automaticly
				// for the above problem with array (see parseParameterRefclassArray)
				Vector<Node> entries = new Vector<Node>();
				// Finds EntryArray node
				Node entryArray = routing.get(className2).getFirstChild();
				while (entryArray.getNodeType() != Node.ELEMENT_NODE || !entryArray.getNodeName().equals(XML_E_SUBPARAMETER)) {
					entryArray = entryArray.getNextSibling();
				}
				// Now finds every empirical entry
				Node child = entryArray.getFirstChild();
				while (child != null) {
					// Find first subParameter element
					while (child != null && (child.getNodeType() != Node.ELEMENT_NODE || !child.getNodeName().equals(XML_E_SUBPARAMETER))) {
						child = child.getNextSibling();
					}
					if (child != null) {
						entries.add(child);
						child = child.getNextSibling();
					}
				}
				// For each empirical entry get station name and probability
				Map<String, Double> tmp = new TreeMap<String, Double>();
				for (int j = 0; j < entries.size(); j++) {
					NodeList values = ((Element) entries.get(j)).getElementsByTagName(XML_E_SUBPARAMETER);
					String stationName = findText(((Element) values.item(0)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
					Double probability = Double.valueOf(findText(((Element) values.item(1)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
					// Now puts the tuple stationName -> probability into a Map, then adds it
					// to empiricalRouting Map. This is needed as at this
					// point we do not have all station's key so will be adjusted lately
					tmp.put(stationName, probability);
				}
				// Put into empiricalRouting a pair of station key and class key and map with
				// station names instead of station key
				empiricalRouting.put(new Object[] { key, classes.get(className2) }, tmp);
			} else if (rs instanceof LoadDependentRouting) {
				Node entryArray = routing.get(className2).getFirstChild();
				while (entryArray != null && (entryArray.getNodeType() != Node.ELEMENT_NODE || !entryArray.getNodeName().equals(XML_E_SUBPARAMETER))) {
					entryArray = entryArray.getNextSibling();//This is a SubParameter..So next time it wont enter.
				}
				Vector<Node> from = new Vector<Node>();
				while (entryArray.getNodeType() != Node.ELEMENT_NODE || !entryArray.getNodeName().equals(XML_E_SUBPARAMETER)) {
					entryArray = entryArray.getNextSibling();//Did not enter as I am in LDParameter Array
				}
				// Now finds every From entry
				Node child = entryArray.getFirstChild();
				while (child != null) {
					while (child != null && (child.getNodeType() != Node.ELEMENT_NODE || !child.getNodeName().equals(XML_E_SUBPARAMETER))) {
						child = child.getNextSibling();//This is a SubParameter
					}
					if (child != null) {
						from.add(child);
						child = child.getNextSibling();//TXT
					}
				}
				for (int j = 0; j < from.size(); j++) {
					NodeList values = ((Element) from.get(j)).getElementsByTagName(XML_E_SUBPARAMETER);
					String fromValue = findText(((Element) values.item(0)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
					values = ((Element) values.item(1)).getElementsByTagName(XML_E_SUBPARAMETER);
					Map<String, Double> tmp = new TreeMap<String, Double>();
					for (int n = 0; n < values.getLength(); n++) {
						//Empirical Entry..Station Name..probability Keeps repeating..
						String atrName = ((Element) values.item(n)).getAttribute("name");//Empirical Entry..Station Name..probability..for a from.
						if ("EmpiricalEntry".equalsIgnoreCase(atrName)) {
							Node empiricalEntry = values.item(n);
							Node station = empiricalEntry.getFirstChild().getNextSibling();
							String stationName = findText(((Element) station).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
							Node probability = empiricalEntry.getFirstChild().getNextSibling().getNextSibling().getNextSibling();
							String probabilityValue = findText(((Element) probability).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
							tmp.put(stationName, Double.valueOf(probabilityValue));
						}
						empiricalLDRouting.put(new Object[] { key, classes.get(className2), Integer.valueOf(fromValue) }, tmp);
					}
				}
			} else if (rs instanceof PowerOfKRouting) {
			    PowerOfKRouting strategy = (PowerOfKRouting) rs;
				Node child = routing.get(className2).getFirstChild();
				while (child.getNodeType() != Node.ELEMENT_NODE || !child.getNodeName().equals(XML_E_SUBPARAMETER)) {
					child = child.getNextSibling();
				}
				NodeList values = ((Element) child).getElementsByTagName(XML_E_PARAMETER_VALUE);
				Integer k = Integer.valueOf(findText(values.item(0)));
				strategy.setK(k);
				child = child.getNextSibling();
				while (child.getNodeType() != Node.ELEMENT_NODE || !child.getNodeName().equals(XML_E_SUBPARAMETER)) {
					child = child.getNextSibling();
				}
				values = ((Element) child).getElementsByTagName(XML_E_PARAMETER_VALUE);
				Boolean withMemory = Boolean.valueOf(findText(values.item(0)));
				strategy.setWithMemory(withMemory);
			} else if (rs instanceof WeightedRoundRobinRouting) {
				Vector<Node> entries = new Vector<>();
				Node entryArray = routing.get(className2).getFirstChild();
				while (entryArray.getNodeType() != Node.ELEMENT_NODE || !entryArray.getNodeName().equals(XML_E_SUBPARAMETER)) {
					entryArray = entryArray.getNextSibling();
				}
				Node child = entryArray.getFirstChild();
				while (child != null) {
					while (child != null && (child.getNodeType() != Node.ELEMENT_NODE || !child.getNodeName().equals(XML_E_SUBPARAMETER))) {
						child = child.getNextSibling();
					}
					if (child != null) {
						entries.add(child);
						child = child.getNextSibling();
					}
				}
				Map<String, Integer> tmp = new TreeMap<>();
				for (Node entry : entries) {
					NodeList values = ((Element) entry).getElementsByTagName(XML_E_SUBPARAMETER);
					String stationName = findText(((Element) values.item(0)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
					Integer weight = Integer.valueOf(findText(((Element) values.item(1)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
					tmp.put(stationName, weight);
				}
				wrrRouting.put(new Object[] { key, classes.get(className2) }, tmp);
			}
		}
	}

	/**
	 * Parses class switch section
	 * @param section class switch section
	 * @param model data structure
	 * @param key station's key
	 */
	protected static void parseClassSwitchSection(Element section, CommonModel model, Object stationKey) {
		Element matrix = (Element) section.getElementsByTagName(XML_E_PARAMETER).item(0);
		Map<String, Node> rows = parseParameterRefclassArray(matrix);
		Iterator<String> i = rows.keySet().iterator();

		while (i.hasNext()) {
			String classIn = i.next();
			Object classInKey = model.getClassByName(classIn);
			Element row = (Element) rows.get(classIn);
			NodeList rowChild = row.getChildNodes(); 
			for (int j = 0; j < rowChild.getLength();j++) {
				if (rowChild.item(j).getNodeType() == Node.TEXT_NODE ) {
					continue;
				}
				Node refClass = rowChild.item(j);
				String classOut = refClass.getChildNodes().item(0).getNodeValue();
				j++;
				while (rowChild.item(j).getNodeType() == Node.TEXT_NODE) {
					j++;
				}
				Node subParam = rowChild.item(j);
				NodeList subParamChild = subParam.getChildNodes();
				int h = 0;
				while (subParamChild.item(h).getNodeType() == Node.TEXT_NODE) {
					h++;
				}
				String value = subParamChild.item(h).getChildNodes().item(0).getNodeValue();
				Object classOutKey = model.getClassByName(classOut);
				model.setClassSwitchMatrix(stationKey, classInKey, classOutKey, Float.parseFloat(value));
			}
		}
	}

	/**
	 * Parses all parameters for a Logger section from the XML document.
	 * The information from parseLogger is passed to LogTunnel.
	 * 
	 * @param section service section of logger station
	 * @param model link to data structure
	 * @param key key of search for this logger station into data structure
	 * @author Michael Fercu (Bertoli Marco)
	 *		   Date: 08-aug-2008
	 * @see jmt.engine.log.LoggerParameters LoggerParameters
	 * @see jmt.gui.common.XMLWriter#writeLoggerSection XMLWriter.writeLoggerSection()
	 * @see jmt.gui.common.definitions.CommonModel#getLoggingParameters CommonModel.getLoggingParameters()
	 * @see jmt.gui.common.definitions.CommonModel#setLoggingParameters CommonModel.setLoggingParameters()
	 * @see jmt.engine.NodeSections.LogTunnel LogTunnel
	 */
	protected static void parseLoggerSection(Element section, CommonModel model, Object key) {
		NodeList parameters = section.getElementsByTagName(XML_E_PARAMETER);
		LoggerParameters logParams = new LoggerParameters();

		for (int i = 0; i < parameters.getLength(); i++) {
			Element parameter = (Element) parameters.item(i);
			String parameterName = parameter.getAttribute(XML_A_PARAMETER_NAME);
			try {
				// Get the parameters from the XML file
				if (parameterName.equals(XML_LOG_FILENAME)) {
					logParams.name = new String(findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				} else if (parameterName.equals(XML_LOG_FILEPATH)) {
					// temporary fix
					logParams.path = MacroReplacer.replace(model.getLoggingGlbParameter("path"));
				} else if (parameterName.equals(XML_LOG_B_EXECTIMESTAMP)) {
					logParams.boolExecTimestamp = new Boolean(findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				} else if (parameterName.equals(XML_LOG_B_LOGGERNAME)) {
					logParams.boolLoggername = new Boolean(findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				} else if (parameterName.equals(XML_LOG_B_TIMESTAMP)) {
					logParams.boolTimeStamp = new Boolean(findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				} else if (parameterName.equals(XML_LOG_B_JOBID)) {
					logParams.boolJobID = new Boolean(findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				} else if (parameterName.equals(XML_LOG_B_JOBCLASS)) {
					logParams.boolJobClass = new Boolean(findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				} else if (parameterName.equals(XML_LOG_B_TIMESAMECLS)) {
					logParams.boolTimeSameClass = new Boolean(findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				} else if (parameterName.equals(XML_LOG_B_TIMEANYCLS)) {
					logParams.boolTimeAnyClass = new Boolean(findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0)));
				} else if (parameterName.equals("numClasses")) {
					/* No parsing needed for these parameters:
					 * Only useful to (and has already been passed to) the simulator. */;
				} else {
					debugLog.error("XMLReader.parseLogger() - Unknown parameter \"" + parameterName + "\".");
				}
			} catch (Exception e) {
				debugLog.error("XMLreader.parseLogger: " + e.toString());
			}

			model.setLoggingParameters(key, logParams);
		}
	}

	/**
	 * Parses all informations regarding Fork section.
	 * @param section output section of fork station
	 * @param model link to data structure
	 * @param key key of search for this fork station into data structure
	 */
	protected static void parseForkSection(Element section, CommonModel model, Object key) {
		NodeList parameters = section.getElementsByTagName(XML_E_PARAMETER);
		for (int i = 0; i < parameters.getLength(); i++) {
			Element parameter = (Element) parameters.item(i);
			String parameterName = parameter.getAttribute(XML_A_PARAMETER_NAME);
			// Fork number of server is used as number of jobs per link
			if (parameterName.equals("jobsPerLink")) {
				model.setStationNumberOfServers(key, Integer.valueOf(findText(
						parameter.getElementsByTagName(XML_E_PARAMETER_VALUE)
						.item(0))));
			} else if (parameterName.equals("block")) {
				model.setForkBlock(key, Integer.valueOf(findText(parameter
						.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0))));
			} else if (parameterName.equals("isSimplifiedFork")) {
				model.setIsSimplifiedFork(key, Boolean
						.parseBoolean(findText(parameter.getElementsByTagName(
								XML_E_PARAMETER_VALUE).item(0))));
			} else if (parameterName.equals("ForkStrategy")) {
				Map<String, Node> fork = parseParameterRefclassArray(parameter);
				Object[] classNames = fork.keySet().toArray();
				String className;

				if (engineToGuiFork == null) {
					engineToGuiFork = new TreeMap<String, ForkStrategy>();
					ForkStrategy[] allFS = ForkStrategy.findAll();
					for (ForkStrategy element : allFS) {
						engineToGuiFork.put(element.getClass().getName(),
								element);
					}
				}

				Object[] forkStratKeys = engineToGuiFork.keySet().toArray();
				for (Object className2 : classNames) {
					Map<String, OutPath> map = new HashMap<String, OutPath>();
					className = ((Element) fork.get(className2))
							.getAttribute(XML_A_SUBPARAMETER_CLASSPATH);
					for (Object forkStratKey : forkStratKeys) {
						if (className.equals(engineToGuiFork.get(
								forkStratKey).getClassPath())) {
							model.setForkStrategy(key, classes
									.get(className2),
									engineToGuiFork.get(forkStratKey)
									.clone());
						}
					}

					ForkStrategy fs = (ForkStrategy) model.getForkStrategy(
							key, classes.get(className2));
					if (fs instanceof ProbabilitiesFork) {
						ArrayList<Node> entries = new ArrayList<Node>();
						// Finds EntryArray node
						Node entryArray = fork.get(className2)
								.getFirstChild();
						while (entryArray.getNodeType() != Node.ELEMENT_NODE
								|| !entryArray.getNodeName().equals(
										XML_E_SUBPARAMETER)) {
							entryArray = entryArray.getNextSibling();
						}
						// Now finds every outPaths
						Node child = entryArray.getFirstChild();
						while (child != null) {
							// Find first subParameter element
							while (child != null
									&& (child.getNodeType() != Node.ELEMENT_NODE || !child
									.getNodeName().equals(
											XML_E_SUBPARAMETER))) {
								child = child.getNextSibling();
							}
							if (child != null) {
								entries.add(child);
								child = child.getNextSibling();
							}
						}
						// For each empirical entry get station name and
						// probability
						for (int j = 0; j < entries.size(); j++) {
							OutPath op = new OutPath();
							Node temp = entries.get(j).getFirstChild();
							while (temp.getNodeType() != Node.ELEMENT_NODE
									|| !temp.getNodeName().equals(
											XML_E_SUBPARAMETER)) {
								temp = temp.getNextSibling();
							}
							NodeList values = ((Element) temp)
									.getElementsByTagName(XML_E_SUBPARAMETER);
							String stationName = findText(((Element) values
									.item(0)).getElementsByTagName(
											XML_E_PARAMETER_VALUE).item(0));
							Double probability = Double
									.valueOf(findText(((Element) values.item(1))
											.getElementsByTagName(
													XML_E_PARAMETER_VALUE)
											.item(0)));
							op.setProbability(probability);
							temp = temp.getNextSibling();
							while (temp.getNodeType() != Node.ELEMENT_NODE
									|| !temp.getNodeName().equals(
											XML_E_SUBPARAMETER)) {
								temp = temp.getNextSibling();
							}
							ArrayList<Node> probs = new ArrayList<Node>();
							Node temp2 = temp.getFirstChild();
							while (temp2 != null) {
								// Find first subParameter element
								while (temp2 != null
										&& (temp2.getNodeType() != Node.ELEMENT_NODE || !temp2
										.getNodeName().equals(
												XML_E_SUBPARAMETER))) {
									temp2 = temp2.getNextSibling();
								}
								if (temp2 != null) {
									probs.add(temp2);
									temp2 = temp2.getNextSibling();
								}
							}
							Map<Object, Object> tempMap = new HashMap<Object, Object>();
							for (int k = 0; k < probs.size(); k++) {
								Node temp3 = probs.get(k);
								NodeList results = ((Element) temp3)
										.getElementsByTagName(XML_E_SUBPARAMETER);
								Integer num = Integer
										.valueOf(findText(((Element) results
												.item(0)).getElementsByTagName(
														XML_E_PARAMETER_VALUE).item(0)));
								Double prob = Double
										.valueOf(findText(((Element) results
												.item(1)).getElementsByTagName(
														XML_E_PARAMETER_VALUE).item(0)));
								tempMap.put(num, prob);
							}
							op.setOutParameters(tempMap);
							map.put(stationName, op);
						}
						empiricalFork.put(
								new Object[] { key, classes.get(className2) },
								map);
					} else if (fs instanceof CombFork) {
						ArrayList<Node> entries = new ArrayList<Node>();
						// Finds EntryArray node
						Node entryArray = fork.get(className2)
								.getFirstChild();
						while (entryArray.getNodeType() != Node.ELEMENT_NODE
								|| !entryArray.getNodeName().equals(
										XML_E_SUBPARAMETER)) {
							entryArray = entryArray.getNextSibling();
						}
						Node child = entryArray.getFirstChild();
						while (child != null) {
							// Find first subParameter element
							while (child != null
									&& (child.getNodeType() != Node.ELEMENT_NODE || !child
									.getNodeName().equals(
											XML_E_SUBPARAMETER))) {
								child = child.getNextSibling();
							}
							if (child != null) {
								entries.add(child);
								child = child.getNextSibling();
							}
						}
						Map<String, Double> tmp = new TreeMap<String, Double>();
						for (int j = 0; j < entries.size(); j++) {
							NodeList values = ((Element) entries.get(j))
									.getElementsByTagName(XML_E_SUBPARAMETER);
							String num = findText(((Element) values.item(0))
									.getElementsByTagName(XML_E_PARAMETER_VALUE)
									.item(0));
							Double prob = Double
									.valueOf(findText(((Element) values.item(1))
											.getElementsByTagName(XML_E_PARAMETER_VALUE)
											.item(0)));
							tmp.put(num, prob);
						}
						combFork.put(
								new Object[] { key, classes.get(className2) }, tmp);
					} else if (fs instanceof ClassSwitchFork
							|| fs instanceof MultiBranchClassSwitchFork) {
						ArrayList<Node> entries = new ArrayList<Node>();
						// Finds EntryArray node
						Node entryArray = fork.get(className2)
								.getFirstChild();
						while (entryArray.getNodeType() != Node.ELEMENT_NODE
								|| !entryArray.getNodeName().equals(
										XML_E_SUBPARAMETER)) {
							entryArray = entryArray.getNextSibling();
						}
						// Now finds every outPaths
						Node child = entryArray.getFirstChild();
						while (child != null) {
							// Find first subParameter element
							while (child != null
									&& (child.getNodeType() != Node.ELEMENT_NODE || !child
									.getNodeName().equals(
											XML_E_SUBPARAMETER))) {
								child = child.getNextSibling();
							}
							if (child != null) {
								entries.add(child);
								child = child.getNextSibling();
							}
						}
						// For each empirical entry get station name and
						// probability
						for (int j = 0; j < entries.size(); j++) {
							OutPath op = new OutPath();
							Node temp = entries.get(j).getFirstChild();
							while (temp.getNodeType() != Node.ELEMENT_NODE
									|| !temp.getNodeName().equals(
											XML_E_SUBPARAMETER)) {
								temp = temp.getNextSibling();
							}
							String stationName = findText(((Element) temp)
									.getElementsByTagName(
											XML_E_PARAMETER_VALUE).item(0));
							temp = temp.getNextSibling();
							while (temp.getNodeType() != Node.ELEMENT_NODE
									|| !temp.getNodeName().equals(
											XML_E_SUBPARAMETER)) {
								temp = temp.getNextSibling();
							}
							ArrayList<Node> probs = new ArrayList<Node>();
							Node temp2 = temp.getFirstChild();
							while (temp2 != null) {
								// Find first subParameter element
								while (temp2 != null
										&& (temp2.getNodeType() != Node.ELEMENT_NODE || !temp2
										.getNodeName().equals(
												XML_E_SUBPARAMETER))) {
									temp2 = temp2.getNextSibling();
								}
								if (temp2 != null) {
									probs.add(temp2);
									temp2 = temp2.getNextSibling();
								}
							}
							List<String> classes = new ArrayList<String>();
							for (int k = 0; k < probs.size(); k++) {
								String jobClass = findText(((Element) probs.get(k))
										.getElementsByTagName(
												XML_E_PARAMETER_VALUE).item(0));
								classes.add(jobClass);
							}
							temp = temp.getNextSibling();
							while (temp.getNodeType() != Node.ELEMENT_NODE
									|| !temp.getNodeName().equals(
											XML_E_SUBPARAMETER)) {
								temp = temp.getNextSibling();
							}
							probs = new ArrayList<Node>();
							temp2 = temp.getFirstChild();
							while (temp2 != null) {
								// Find first subParameter element
								while (temp2 != null
										&& (temp2.getNodeType() != Node.ELEMENT_NODE || !temp2
										.getNodeName().equals(
												XML_E_SUBPARAMETER))) {
									temp2 = temp2.getNextSibling();
								}
								if (temp2 != null) {
									probs.add(temp2);
									temp2 = temp2.getNextSibling();
								}
							}
							List<Object> numbers = new ArrayList<Object>();
							for (int k = 0; k < probs.size(); k++) {
								Integer number = Integer.parseInt(findText(((Element) probs.get(k))
										.getElementsByTagName(
												XML_E_PARAMETER_VALUE).item(0)));
								numbers.add(number);
							}
							HashMap<Object, Object> tempMap = new HashMap<Object, Object>();
							for (int k = 0; k < classes.size(); k++) {
								tempMap.put(model.getClassByName(classes.get(k)), numbers.get(k));
							}
							op.setOutParameters(tempMap);
							map.put(stationName, op);
						}
						empiricalFork.put(
								new Object[] { key, classes.get(className2) },
								map);
					}

				}
			}
		}
	}

	/**
	 * Parses all informations regarding Join section.
	 * @param section input section of join station
	 * @param model link to data structure
	 * @param key key of search for this join station into data structure
	 */
	protected static void parseJoinSection(Element section, CommonModel model,
			Object key) {
		Element parameter = (Element) section.getElementsByTagName(
				XML_E_PARAMETER).item(0);
		Map<String, Node> join = null;
		if (parameter != null) {
			join = parseParameterRefclassArray(parameter);
			Object[] classNames = join.keySet().toArray();
			String className;

			if (engineToGuiJoin == null) {
				engineToGuiJoin = new TreeMap<String, JoinStrategy>();
				JoinStrategy[] allJS = JoinStrategy.findAll();
				for (JoinStrategy element : allJS) {
					engineToGuiJoin.put(element.getClass().getName(), element);
				}
			}

			Object[] joinStratKeys = engineToGuiJoin.keySet().toArray();
			for (Object className2 : classNames) {
				className = ((Element) join.get(className2))
						.getAttribute(XML_A_SUBPARAMETER_CLASSPATH);

				for (Object joinStratKey : joinStratKeys) {
					if (className.equals(engineToGuiJoin.get(joinStratKey)
							.getClassPath())) {
						model.setJoinStrategy(key, classes.get(className2),
								engineToGuiJoin.get(joinStratKey).clone());
					}
				}

				JoinStrategy js = (JoinStrategy) model.getJoinStrategy(key,
						classes.get(className2));
				if (js instanceof PartialJoin) {
					Element temp = (Element) join.get(className2);
					NodeList temp2 = temp.getElementsByTagName(XML_E_SUBPARAMETER);
					for (int i = 0; i < temp2.getLength(); i++) {
						Element temp3 = (Element) temp2.item(i);
						if (temp3.getAttribute(XML_A_CLASS_NAME).equals(
								"numRequired")) {
							js.setRequiredNum(Integer.parseInt(temp3
									.getElementsByTagName("value").item(0)
									.getFirstChild().getTextContent()));
						}
					}
				} else if (js instanceof GuardJoin) {
					Element temp = (Element) join.get(className2);
					NodeList parList = temp.getElementsByTagName(XML_E_SUBPARAMETER);
					Map<Object, Integer> mix = new HashMap<>();
					List<String> classes = new ArrayList<>();
					List<Integer> numbers = new ArrayList<>();
					for (int i = 0; i < parList.getLength(); i++) {
						Element par = (Element) parList.item(i);
						if (par.getAttribute(XML_A_CLASS_NAME).equals(
								"Classes")) {
							NodeList classList = par.getElementsByTagName(XML_E_SUBPARAMETER);
							for (int j = 0; j < classList.getLength(); j++) {
								Element classPar = (Element) classList.item(j);
								classes.add(classPar.getElementsByTagName("value").item(0).getFirstChild().getTextContent());
							}
						} else if (par.getAttribute(XML_A_CLASS_NAME).equals(
								"Numbers")) {
							NodeList numList = par.getElementsByTagName(XML_E_SUBPARAMETER);
							for (int j = 0; j < numList.getLength(); j++) {
								Element numPar = (Element) numList.item(j);
								numbers.add(Integer.parseInt(numPar.getElementsByTagName("value").item(0).getFirstChild().getTextContent()));
							}
						}
					}
					for (int i = 0; i < classes.size(); i++) {
						mix.put(model.getClassByName(classes.get(i)), numbers.get(i));
					}
					((GuardJoin) js).setGuard(mix);
				}
			}
		} else {
			for (Object o : classes.values()) {
				model.setJoinStrategy(key, o, JOIN_NORMAL.clone());
			}
		}
	}

	/**
	 * Parses all informations regarding Semaphore section.
	 * @param section input section of semaphore station
	 * @param model link to data structure
	 * @param key key of search for this semaphore station into data structure
	 */
	protected static void parseSemaphoreSection(Element section, CommonModel model,
			Object key) {
		Element parameter = (Element) section.getElementsByTagName(
				XML_E_PARAMETER).item(0);
		Map<String, Node> semaphore = null;
		if (parameter != null) {
			semaphore = parseParameterRefclassArray(parameter);
			Object[] classNames = semaphore.keySet().toArray();
			String className;

			if (engineToGuiSemaphore == null) {
				engineToGuiSemaphore = new TreeMap<String, SemaphoreStrategy>();
				SemaphoreStrategy[] allSS = SemaphoreStrategy.findAll();
				for (SemaphoreStrategy element : allSS) {
					engineToGuiSemaphore.put(element.getClass().getName(), element);
				}
			}

			Object[] semaphoreStratKeys = engineToGuiSemaphore.keySet().toArray();
			for (Object className2 : classNames) {
				className = ((Element) semaphore.get(className2))
						.getAttribute(XML_A_SUBPARAMETER_CLASSPATH);

				for (Object semaphoreStratKey : semaphoreStratKeys) {
					if (className.equals(engineToGuiSemaphore.get(semaphoreStratKey)
							.getClassPath())) {
						model.setSemaphoreStrategy(key, classes.get(className2),
								engineToGuiSemaphore.get(semaphoreStratKey).clone());
					}
				}

				SemaphoreStrategy ss = (SemaphoreStrategy) model.getSemaphoreStrategy(key,
						classes.get(className2));
				if (ss instanceof NormalSemaphore) {
					Element temp = (Element) semaphore.get(className2);
					NodeList temp2 = temp.getElementsByTagName(XML_E_SUBPARAMETER);
					for (int i = 0; i < temp2.getLength(); i++) {
						Element temp3 = (Element) temp2.item(i);
						if (temp3.getAttribute(XML_A_CLASS_NAME).equals(
								"threshold")) {
							ss.setThreshold(Integer.parseInt(temp3
									.getElementsByTagName("value").item(0)
									.getFirstChild().getTextContent()));
						}
					}
				}
			}
		} else {
			for (Object o : classes.values()) {
				model.setSemaphoreStrategy(key, o, SEMAPHORE_NORMAL.clone());
			}
		}
	}

	/**
	 * Parses all informations regarding Storage section.
	 * @param section input section of place station
	 * @param model link to data structure
	 * @param key key of search for this place station into data structure
	 */
	protected static void parseStorageSection(Element section, CommonModel model, Object key) {
		NodeList parameters = section.getElementsByTagName(XML_E_PARAMETER);
		Map<String, Node> capacities = null;
		Map<String, Node> dropRules = null;
		Map<String, Node> putStrategies = null;

		for (int i = 0; i < parameters.getLength(); i++) {
			Element parameter = (Element) parameters.item(i);
			String name = parameter.getAttribute(XML_A_PARAMETER_NAME);
			if (name.equals("totalCapacity")) {
				String totalCapacity = findText(parameter.getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
				model.setStationQueueCapacity(key, Integer.valueOf(totalCapacity));
			} else if (name.equals("capacities")) {
				capacities = parseParameterRefclassArray(parameter);
			} else if (name.equals("dropRules")) {
				dropRules = parseParameterRefclassArray(parameter);
			} else if (name.equals("putStrategies")) {
				putStrategies = parseParameterRefclassArray(parameter);
			}
		}

		if (capacities != null) {
			for (Entry<String, Node> cap : capacities.entrySet()) {
				String capacity = findText(((Element) cap.getValue()).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
				model.setQueueCapacity(key, classes.get(cap.getKey()), Integer.valueOf(capacity));
			}
		}

		if (dropRules != null) {
			for (Entry<String, Node> rule : dropRules.entrySet()) {
				String dropRule = findText(((Element) rule.getValue()).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
				model.setDropRule(key, classes.get(rule.getKey()), DROP_RULES_MAPPING.get(dropRule));
			}
		}

		if (putStrategies != null) {
			for (Entry<String, Node> strategy : putStrategies.entrySet()) {
				String putStrategy = ((Element) strategy.getValue()).getAttribute(XML_A_SUBPARAMETER_CLASSPATH);
				putStrategy = putStrategy.substring(putStrategy.lastIndexOf(".") + 1);
				if (putStrategy.equals("TailStrategy")) {
					model.setQueueStrategy(key, classes.get(strategy.getKey()), QUEUE_STRATEGY_FCFS);
				} else if (putStrategy.equals("HeadStrategy")) {
					model.setQueueStrategy(key, classes.get(strategy.getKey()), QUEUE_STRATEGY_LCFS);
				} else if (putStrategy.equals("RandStrategy")) {
					model.setQueueStrategy(key, classes.get(strategy.getKey()), QUEUE_STRATEGY_RAND);
				}
			}
		}
	}

	/**
	 * Parses all informations regarding Enabling section.
	 * @param section input section of transition station
	 * @param model link to data structure
	 * @param key key of search for this transition station into data structure
	 */
	protected static void parseEnablingSection(Element section, CommonModel model, Object key) {
		NodeList parameters = section.getElementsByTagName(XML_E_PARAMETER);
		Vector<Node> enablingConditions = null;
		Vector<Node> inhibitingConditions = null;

		for (int i = 0; i < parameters.getLength(); i++) {
			Element parameter = (Element) parameters.item(i);
			String name = parameter.getAttribute(XML_A_PARAMETER_NAME);
			if (name.equals("enablingConditions")) {
				enablingConditions = parseParameterArray(parameter);
			} else if (name.equals("inhibitingConditions")) {
				inhibitingConditions = parseParameterArray(parameter);
			}
		}

		if (enablingConditions != null) {
			for (int i = 0; i < enablingConditions.size(); i++) {
				Node enablingCondition = enablingConditions.get(i);
				Vector<Node> conditionElements = parseParameterArray((Element) enablingCondition);
				Vector<Node> enablingVectors = parseParameterArray((Element) conditionElements.get(0));
				for (Node enablingVector : enablingVectors) {
					Vector<Node> vectorElements = parseParameterArray((Element) enablingVector);
					String stationName = findText(((Element) vectorElements.get(0)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
					Map<String, Node> enablingEntries = parseParameterRefclassArray((Element) vectorElements.get(1));
					for (Entry<String, Node> enablingEntry : enablingEntries.entrySet()) {
						Object[] quadkey = new Object[] { key, Integer.valueOf(i), stationName, classes.get(enablingEntry.getKey()) };
						String value = findText(((Element) enablingEntry.getValue()).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
						enablingConditionMap.put(quadkey, Integer.valueOf(value));
					}
				}
			}
		}

		if (inhibitingConditions != null) {
			for (int i = 0; i < inhibitingConditions.size(); i++) {
				Node inhibitingCondition = inhibitingConditions.get(i);
				Vector<Node> conditionElements = parseParameterArray((Element) inhibitingCondition);
				Vector<Node> inhibitingVectors = parseParameterArray((Element) conditionElements.get(0));
				for (Node inhibitingVector : inhibitingVectors) {
					Vector<Node> vectorElements = parseParameterArray((Element) inhibitingVector);
					String stationName = findText(((Element) vectorElements.get(0)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
					Map<String, Node> inhibitingEntries = parseParameterRefclassArray((Element) vectorElements.get(1));
					for (Entry<String, Node> inhibitingEntry : inhibitingEntries.entrySet()) {
						Object[] quadkey = new Object[] { key, Integer.valueOf(i), stationName, classes.get(inhibitingEntry.getKey()) };
						String value = findText(((Element) inhibitingEntry.getValue()).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
						inhibitingConditionMap.put(quadkey, Integer.valueOf(value));
					}
				}
			}
		}
	}

	/**
	 * Parses all informations regarding Timing section.
	 * @param section service section of transition station
	 * @param model link to data structure
	 * @param key key of search for this transition station into data structure
	 */
	protected static void parseTimingSection(Element section, CommonModel model, Object key) {
		NodeList parameters = section.getElementsByTagName(XML_E_PARAMETER);
		Vector<Node> modeNames = null;
		Vector<Node> numbersOfServers = null;
		Vector<Node> timingStrategies = null;
		Vector<Node> firingPriorities = null;
		Vector<Node> firingWeights = null;

		for (int i = 0; i < parameters.getLength(); i++) {
			Element parameter = (Element) parameters.item(i);
			String name = parameter.getAttribute(XML_A_PARAMETER_NAME);
			if (name.equals("modeNames")) {
				modeNames = parseParameterArray(parameter);
			} else if (name.equals("numbersOfServers")) {
				numbersOfServers = parseParameterArray(parameter);
			} else if (name.equals("timingStrategies")) {
				timingStrategies = parseParameterArray(parameter);
			} else if (name.equals("firingPriorities")) {
				firingPriorities = parseParameterArray(parameter);
			} else if (name.equals("firingWeights")) {
				firingWeights = parseParameterArray(parameter);
			}
		}

		if (modeNames != null) {
			model.deleteTransitionMode(key, 0);
			List<String> nameList = new ArrayList<String>();
			for (int i = 0; i < modeNames.size(); i++) {
				Node modeName = modeNames.get(i);
				String name = findText(((Element) modeName).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
				model.addTransitionMode(key, name);
				nameList.add(name);
			}
			modeNameLists.put(key, nameList);
		}

		if (numbersOfServers != null) {
			for (int i = 0; i < numbersOfServers.size(); i++) {
				Node numberOfServers = numbersOfServers.get(i);
				String number = findText(((Element) numberOfServers).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
				model.setNumberOfServers(key, i,  Integer.valueOf(number));
			}
		}

		if (timingStrategies != null) {
			for (int i = 0; i < timingStrategies.size(); i++) {
				Node timingStrategy = timingStrategies.get(i);
				Object strategy = parseServiceStrategy((Element) timingStrategy);
				model.setFiringTimeDistribution(key, i, strategy);
			}
		}

		if (firingPriorities != null) {
			for (int i = 0; i < firingPriorities.size(); i++) {
				Node firingPriority = firingPriorities.get(i);
				String priority = findText(((Element) firingPriority).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
				model.setFiringPriority(key, i,  Integer.valueOf(priority));
			}
		}

		if (firingWeights != null) {
			for (int i = 0; i < firingWeights.size(); i++) {
				Node firingWeight = firingWeights.get(i);
				String weight = findText(((Element) firingWeight).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
				model.setFiringWeight(key, i,  Double.valueOf(weight));
			}
		}
	}

	/**
	 * Parses all informations regarding Firing section.
	 * @param section output section of transition station
	 * @param model link to data structure
	 * @param key key of search for this transition station into data structure
	 */
	protected static void parseFiringSection(Element section, CommonModel model, Object key) {
		NodeList parameters = section.getElementsByTagName(XML_E_PARAMETER);
		Vector<Node> firingOutcomes = null;

		for (int i = 0; i < parameters.getLength(); i++) {
			Element parameter = (Element) parameters.item(i);
			String name = parameter.getAttribute(XML_A_PARAMETER_NAME);
			if (name.equals("firingOutcomes")) {
				firingOutcomes = parseParameterArray(parameter);
			}
		}

		if (firingOutcomes != null) {
			for (int i = 0; i < firingOutcomes.size(); i++) {
				Node firingOutcome = firingOutcomes.get(i);
				Vector<Node> outcomeElements = parseParameterArray((Element) firingOutcome);
				Vector<Node> firingVectors = parseParameterArray((Element) outcomeElements.get(0));
				for (Node firingVector : firingVectors) {
					Vector<Node> vectorElements = parseParameterArray((Element) firingVector);
					String stationName = findText(((Element) vectorElements.get(0)).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
					Map<String, Node> firingEntries = parseParameterRefclassArray((Element) vectorElements.get(1));
					for (Entry<String, Node> firingEntry : firingEntries.entrySet()) {
						Object[] quadkey = new Object[] { key, Integer.valueOf(i), stationName, classes.get(firingEntry.getKey()) };
						String value = findText(((Element) firingEntry.getValue()).getElementsByTagName(XML_E_PARAMETER_VALUE).item(0));
						firingOutcomeMap.put(quadkey, Integer.valueOf(value));
					}
				}
			}
		}
	}

	/**
	 * Parses service section informations contained in serviceTimeStrategy element to create a
	 * correct Distribution or LDStrategy object
	 * @param serviceTimeStrategy Element that holds all distribution informations
	 * @return created Distribution or LDStrategy or null if this field is set to null
	 */
	protected static Object parseServiceStrategy(Element serviceTimeStrategy) {
		String serviceClassPath = serviceTimeStrategy.getAttribute(XML_A_SUBPARAMETER_CLASSPATH);
		if (serviceClassPath.equals(ZeroStrategy.getEngineClassPath())) {
			// Zero Service Time Strategy
			return new ZeroStrategy();
		} else if (serviceClassPath.equals(DisabledStrategy.getEngineClassPath())) {
			// Disabled Service Time Strategy
			return new DisabledStrategy();
		} else if (serviceClassPath.equals(LDStrategy.getEngineClassPath())) {
			// Load Dependent Service Strategy
			Element LDParameterArray = (Element) serviceTimeStrategy.getElementsByTagName(XML_E_SUBPARAMETER).item(0);
			LDStrategy strategy = new LDStrategy();
			// Now parses LDStrategy ranges
			Vector<Node> ranges = parseParameterArray(LDParameterArray);
			for (int i = 0; i < ranges.size(); i++) {
				Vector<Node> parameters = parseParameterArray((Element) ranges.get(i));
				int from = Integer.parseInt(findText(((Element) parameters.get(0)).getElementsByTagName(XML_E_SUBPARAMETER_VALUE).item(0)));
				Distribution distr = parseDistribution((Element) parameters.get(1), (Element) parameters.get(2));
				String mean = findText(((Element) parameters.get(3)).getElementsByTagName(XML_E_SUBPARAMETER_VALUE).item(0));
				Object key;
				if (from == 1) {
					// If this is first range
					key = strategy.getAllRanges()[0];
				} else {
					// next ranges
					key = strategy.addRange();
					strategy.setRangeFrom(key, from);
					// This is needed as key will change
					key = strategy.getAllRanges()[strategy.getRangeNumber() - 1];
				}
				strategy.setRangeDistributionNoCheck(key, distr);
				strategy.setRangeDistributionMeanNoCheck(key, mean);
			}
			return strategy;
		} else {
			//use the parseParameterArray function to return only DIRECT subparameters
			Vector<Node> distribution = parseParameterArray(serviceTimeStrategy);
			if (distribution.size() == 0) {
				return null;
			}
			return parseDistribution((Element) distribution.get(0), (Element) distribution.get(1));
		}
	}

	/**
	 * Parses a distribution, given its distribution and distributionPar nodes
	 * @param distr distribution node
	 * @param distrPar distribution's parameter node
	 * @return parsed distribution
	 */
	protected static Distribution parseDistribution(Element distr, Element distrPar) {
		String classname = distr.getAttribute(XML_A_SUBPARAMETER_CLASSPATH);

		//get the subparameter which are directly passed to the distribution
		Vector<Node> distributionParameters = parseParameterArray(distr);
		//add the subparameters which are passed to the distribution parameter
		distributionParameters.addAll(parseParameterArray(distrPar));

		// Creates a map with distribution classpath --> Distribution if needed
		if (engineToGuiDistr == null) {
			Distribution[] allDistr = Distribution.findAll();
			engineToGuiDistr = new TreeMap<String, Distribution>();
			for (Distribution element : allDistr) {
				engineToGuiDistr.put(element.getClassPath(), element);
			}
		}

		// Gets correct instance of distribution
		Distribution dist = engineToGuiDistr.get(classname).clone();
		Element currpar;
		String param_name;
		for (int i = 0; i < distributionParameters.size(); i++) {
			currpar = (Element) distributionParameters.get(i);
			param_name = currpar.getAttribute(XML_A_SUBPARAMETER_NAME);
			//if current parameter is a nested Distribution
			if (currpar.getAttribute(XML_A_SUBPARAMETER_CLASSPATH).equals(distributionContainer)) {
				//parse the currentparameter to get DIRECT subparameters
				Vector<Node> nestedDistr = parseParameterArray(currpar);
				// If distribution is not set, continue
				if (nestedDistr.size() == 0) {
					continue;
				}
				//parse the nested distribution
				Object param_value = parseDistribution((Element) nestedDistr.get(0), (Element) nestedDistr.get(1));
				dist.getParameter(param_name).setValue(param_value);
			} else if (Boolean.parseBoolean(currpar.getAttribute(XML_A_SUBPARAMETER_ARRAY))) {
				Object[][] param_value = null;
				Vector<Node> vectors = parseParameterArray(currpar);
				for (int j = 0; j < vectors.size(); j++) {
					if (j == 0) {
						param_value = new Object[vectors.size()][];
					}
					Element vector = (Element) vectors.get(j);
					Vector<Node> entries = parseParameterArray(vector);
					for (int k = 0; k < entries.size(); k++) {
						if (k == 0) {
							param_value[j] = new Object[entries.size()];
						}
						Element entry = (Element) entries.get(k);
						String value = findText(entry.getElementsByTagName(XML_E_SUBPARAMETER_VALUE).item(0));
						param_value[j][k] = dist.getParameter(param_name).parseValue(value);
						if (param_value[j][k] == null) {
							param_value[j][k] = dist.getParameter(param_name).parseValue("0");
						}
					}
				}
				dist.getParameter(param_name).setValue(param_value);
			} else {
				String param_value = findText(currpar.getElementsByTagName(XML_E_SUBPARAMETER_VALUE).item(0));
				dist.getParameter(param_name).setValue(param_value);
			}
		}
		dist.updateCM(); // Updates values of c and mean
		return dist;
	}

	/**
	 * Returns the type of a station, reconstructing it from section names. This method must be
	 * modified if a new station type is inserted.
	 * @param station element containing sections
	 * @return station type as expected by CommonModel / JMODELModel
	 */
	protected static String getStationType(Element station) {
		NodeList sections = station.getElementsByTagName(XML_E_STATION_SECTION);
		String[] sectionNames = new String[sections.getLength()];

		// Gets all section classnames
		for (int i = 0; i < sectionNames.length; i++) {
			sectionNames[i] = ((Element) sections.item(i)).getAttribute(XML_A_STATION_SECTION_CLASSNAME);
		}

		// Finds station type, basing on section names
		if (sectionNames[0].equals(CLASSNAME_SOURCE)
				&& sectionNames[1].equals(CLASSNAME_TUNNEL)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_SOURCE;
		} else if (sectionNames[0].equals(CLASSNAME_SINK)) {
			return STATION_TYPE_SINK;
		} else if (sectionNames[0].equals(CLASSNAME_TERMINAL)
				&& sectionNames[1].equals(CLASSNAME_TUNNEL)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_TERMINAL;
		} else if (sectionNames[0].equals(CLASSNAME_QUEUE)
				&& sectionNames[1].equals(CLASSNAME_TUNNEL)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_ROUTER;
		} else if (sectionNames[0].equals(CLASSNAME_QUEUE)
				&& sectionNames[1].equals(CLASSNAME_DELAY)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_DELAY;
		} else if (sectionNames[0].equals(CLASSNAME_QUEUE)
				&& (sectionNames[1].equals(CLASSNAME_SERVER)
						|| sectionNames[1].equals(CLASSNAME_PREEMPTIVESERVER)
						|| sectionNames[1].equals(CLASSNAME_PSSERVER))
						|| sectionNames[1].equals(CLASSNAME_EXHAUSTIVEPSERVER)
						|| sectionNames[1].equals(CLASSNAME_LKPSERVER)
						|| sectionNames[1].equals(CLASSNAME_GATEDPSERVER)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_SERVER;
		} else if (sectionNames[0].equals(CLASSNAME_QUEUE)
				&& sectionNames[1].equals(CLASSNAME_TUNNEL)
				&& sectionNames[2].equals(CLASSNAME_FORK)) {
			return STATION_TYPE_FORK;
		} else if (sectionNames[0].equals(CLASSNAME_JOIN)
				&& sectionNames[1].equals(CLASSNAME_TUNNEL)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_JOIN;
		} else if (sectionNames[0].equals(CLASSNAME_QUEUE)
				&& sectionNames[1].equals(CLASSNAME_LOGGER)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_LOGGER;
		} else if (sectionNames[0].equals(CLASSNAME_QUEUE)
				&& sectionNames[1].equals(CLASSNAME_CLASSSWITCH)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_CLASSSWITCH;
		} else if (sectionNames[0].equals(CLASSNAME_SEMAPHORE)
				&& sectionNames[1].equals(CLASSNAME_TUNNEL)
				&& sectionNames[2].equals(CLASSNAME_ROUTER)) {
			return STATION_TYPE_SEMAPHORE;
		} else if (sectionNames[0].equals(CLASSNAME_JOIN)
				&& sectionNames[1].equals(CLASSNAME_TUNNEL)
				&& sectionNames[2].equals(CLASSNAME_FORK)) {
			return STATION_TYPE_SCALER;
		} else if (sectionNames[0].equals(CLASSNAME_STORAGE)
				&& sectionNames[1].equals(CLASSNAME_TUNNEL)
				&& sectionNames[2].equals(CLASSNAME_LINKAGE)) {
			return STATION_TYPE_PLACE;
		} else if (sectionNames[0].equals(CLASSNAME_ENABLING)
				&& sectionNames[1].equals(CLASSNAME_TIMING)
				&& sectionNames[2].equals(CLASSNAME_FIRING)) {
			return STATION_TYPE_TRANSITION;
		}
		return null;
	}

	// -----------------------------------------------------------------------------------------------

	// --- Measure section ---------------------------------------------------------------------------
	/**
	 * Parses all informations on measures to be taken during simulation
	 * @param root root element of XML Document
	 * @param model data structure where all properties have to be set
	 */
	protected static void parseMeasures(Element root, CommonModel model) {
		NodeList measures = root.getElementsByTagName(XML_E_MEASURE);
		Object stationKey, classKey;
		String type;
		Double alpha, precision;
		for (int i = 0; i < measures.getLength(); i++) {
			Element measure = (Element) measures.item(i);
			String stationName = measure.getAttribute(XML_A_MEASURE_STATION);
			String nodeType = measure.getAttribute(XML_A_MEASURE_NODETYPE);
			if (stationName != null && !stationName.equals("")) {
				if (nodeType.equals(NODETYPE_REGION)) {
					stationKey = regions.get(stationName);
				} else {
					stationKey = stations.get(stationName);
				}
			} else {
				stationKey = null;
			}
			type = measure.getAttribute(XML_A_MEASURE_TYPE);
			String className = measure.getAttribute(XML_A_MEASURE_CLASS);
			if (className != null && !className.equals("")) {
				if (type.equals(SimulationDefinition.MEASURE_FX)) {
					List<String> modeNameList = modeNameLists.get(stationKey);
					classKey = modeNameList.get(modeNameList.indexOf(className));
				} else {
					classKey = classes.get(className);
				}
			} else {
				classKey = null;
			}
			//Inverts alpha
			alpha = new Double(1 - Double.parseDouble(measure.getAttribute(XML_A_MEASURE_ALPHA)));
			precision = Double.valueOf(measure.getAttribute(XML_A_MEASURE_PRECISION));
			String verboseStr = measure.getAttribute(XML_A_MEASURE_VERBOSE);
			boolean verbose = Boolean.parseBoolean(verboseStr);

			//Adds measure to the model
			model.addMeasure(type, stationKey, classKey, alpha, precision, verbose);
		}
	}

	// -----------------------------------------------------------------------------------------------

	// --- Connection section ------------------------------------------------------------------------
	/**
	 * Parses all informations on connections to be made into model
	 * @param root root element of XML Document
	 * @param model data structure where all properties have to be set
	 */
	protected static void parseConnections(Element root, CommonModel model) {
		NodeList connections = root.getElementsByTagName(XML_E_CONNECTION);
		Object sourceKey, targetKey;
		for (int i = 0; i < connections.getLength(); i++) {
			sourceKey = stations.get(((Element) connections.item(i)).getAttribute(XML_A_CONNECTION_SOURCE));
			targetKey = stations.get(((Element) connections.item(i)).getAttribute(XML_A_CONNECTION_TARGET));
			// Adds connection to data structure
			model.setConnected(sourceKey, targetKey, true);
		}
	}

	// -----------------------------------------------------------------------------------------------

	// --- Preloading section ------------------------------------------------------------------------
	/**
	 * Parses all informations on preloading to be added to the model
	 * @param root root element of XML Document
	 * @param model data structure where all properties have to be set
	 */
	protected static void parsePreloading(Element root, CommonModel model) {
		NodeList preload = root.getElementsByTagName(XML_E_PRELOAD);
		if (preload.getLength() > 0) {
			// For every station, search for classes and initial jobs in queue
			NodeList station_pop = ((Element) preload.item(0)).getElementsByTagName(XML_E_STATIONPOPULATIONS);
			for (int i = 0; i < station_pop.getLength(); i++) {
				Object stationKey = stations.get(((Element) station_pop.item(i)).getAttribute(XML_A_PRELOADSTATION_NAME));
				NodeList class_pop = ((Element) station_pop.item(i)).getElementsByTagName(XML_E_CLASSPOPULATION);
				for (int j = 0; j < class_pop.getLength(); j++) {
					Object classKey = classes.get(((Element) class_pop.item(j)).getAttribute(XML_A_CLASSPOPULATION_NAME));
					Integer jobs = new Integer(((Element) class_pop.item(j)).getAttribute(XML_A_CLASSPOPULATION_POPULATION));
					// Sets preloading informations
					model.setPreloadedJobs(stationKey, classKey, jobs);
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------

	// --- Blocking regions section ------------------------------------------------------------------
	/**
	 * Parses all informations on blocking regions to be added to the model
	 * @param root root element of XML Document
	 * @param model data structure where all properties have to be set
	 */
	protected static void parseBlockingRegions(Element root, CommonModel model) {
		regions = new TreeMap<String, Object>();
		NodeList regionNodes = root.getElementsByTagName(XML_E_REGION);

		// Creates each region into data structure
		for (int i = 0; i < regionNodes.getLength(); i++) {
			Element region = (Element) regionNodes.item(i);
			String name = region.getAttribute(XML_A_REGION_NAME);
			String type = region.getAttribute(XML_A_REGION_TYPE);
			if (type == null || type.equals("")) {
				type = Defaults.get("blockingRegionType");
			}
			// Adds blocking region to data structure
			Object key = model.addBlockingRegion(name, type);
			regions.put(name, key);

			// Now parses all included stations
			NodeList nodes = region.getElementsByTagName(XML_E_REGIONNODE);
			for (int j = 0; j < nodes.getLength(); j++) {
				String stationName = ((Element) nodes.item(j)).getAttribute(XML_A_REGIONNODE_NAME);
				model.addRegionStation(key, stations.get(stationName));
			}

			// Now parses global constraint
			Element globalConstraint = (Element) region.getElementsByTagName(XML_E_GLOBALCONSTRAINT).item(0);
			model.setRegionCustomerConstraint(key, Integer.valueOf(globalConstraint.getAttribute(XML_A_GLOBALCONSTRAINT_MAXJOBS)));

			Element memoryConstraint = (Element) region.getElementsByTagName(XML_E_GLOBALMEMORYCONSTRAINT).item(0);
			if (memoryConstraint != null) {
				model.setRegionMemorySize(key, Integer.valueOf(memoryConstraint.getAttribute(XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY)));
			}

			// Now parses class constraints
			NodeList classConstraints = region.getElementsByTagName(XML_E_CLASSCONSTRAINT);
			for (int j = 0; j < classConstraints.getLength(); j++) {
				Element classConstraint = (Element) classConstraints.item(j);
				model.setRegionClassCustomerConstraint(key, classes.get(classConstraint.getAttribute(XML_A_CLASSCONSTRAINT_CLASS)),
						Integer.valueOf(classConstraint.getAttribute(XML_A_CLASSCONSTRAINT_MAXJOBS)));
			}

			NodeList classMemoryConstraints = region.getElementsByTagName(XML_E_CLASSMEMORYCONSTRAINT);
			for (int j = 0; j < classMemoryConstraints.getLength(); j++) {
				Element classMemoryConstraint = (Element) classMemoryConstraints.item(j);
				model.setRegionClassMemorySize(key, classes.get(classMemoryConstraint.getAttribute(XML_A_CLASSMEMORYCONSTRAINT_CLASS)),
						Integer.valueOf(classMemoryConstraint.getAttribute(XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY)));
			}

			NodeList dropRules = region.getElementsByTagName(XML_E_DROPRULES);
			for (int j = 0; j < dropRules.getLength(); j++) {
				Element dropRule = (Element) dropRules.item(j);
				model.setRegionClassDropRule(key, classes.get(dropRule.getAttribute(XML_A_DROPRULE_CLASS)),
						Boolean.valueOf(dropRule.getAttribute(XML_A_DROPRULE_DROP)));
			}

			NodeList classWeights = region.getElementsByTagName(XML_E_CLASSWEIGHT);
			for (int j = 0; j < classWeights.getLength(); j++) {
				Element classWeight = (Element) classWeights.item(j);
				model.setRegionClassWeight(key, classes.get(classWeight.getAttribute(XML_A_CLASSWEIGHT_CLASS)),
						Integer.valueOf(classWeight.getAttribute(XML_A_CLASSWEIGHT_WEIGHT)));
			}

			NodeList classSizes = region.getElementsByTagName(XML_E_CLASSSIZE);
			for (int j = 0; j < classSizes.getLength(); j++) {
				Element classSize = (Element) classSizes.item(j);
				model.setRegionClassSize(key, classes.get(classSize.getAttribute(XML_A_CLASSSIZE_CLASS)),
						Integer.valueOf(classSize.getAttribute(XML_A_CLASSSIZE_SIZE)));
			}

			// Now parses group constraints
			NodeList groupConstraints = region.getElementsByTagName(XML_E_GROUPCONSTRAINT);
			for (int j = 0; j < groupConstraints.getLength(); j++) {
				Element groupConstraint = (Element) groupConstraints.item(j);
				model.addRegionGroup(key, groupConstraint.getAttribute(XML_A_GROUPCONSTRAINT_GROUP));
				model.setRegionGroupCustomerConstraint(key, j, Integer.valueOf(groupConstraint.getAttribute(XML_A_GROUPCONSTRAINT_MAXJOBS)));
			}

			NodeList groupMemoryConstraints = region.getElementsByTagName(XML_E_GROUPMEMORYCONSTRAINT);
			for (int j = 0; j < groupMemoryConstraints.getLength(); j++) {
				Element groupMemoryConstraint = (Element) groupMemoryConstraints.item(j);
				model.setRegionGroupMemorySize(key, j, Integer.valueOf(groupMemoryConstraint.getAttribute(XML_A_GROUPMEMORYCONSTRAINT_MAXMEMORY)));
			}

			NodeList groupClassLists = region.getElementsByTagName(XML_E_GROUPCLASSLIST);
			for (int j = 0; j < groupClassLists.getLength(); j++) {
				Element groupClassList = (Element) groupClassLists.item(j);
				NodeList groupClasses = groupClassList.getElementsByTagName(XML_E_GROUPCLASS);
				for (int k = 0; k < groupClasses.getLength(); k++) {
					Element groupClass = (Element) groupClasses.item(k);
					model.addClassIntoRegionGroup(key, j, model.getClassByName(groupClass.getAttribute(XML_A_GROUPCLASS_CLASS)));
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------

	// --- Generic XML Loader ------------------------------------------------------------------------
	/**
	 * Loads an XML file, returning the Document representation of it. This method is generic
	 * and can be used to load every xml file. Actually it is used by <code>XMLReader</code>
	 * and by <code>GuiXMLReader</code>. This method will validate input file.
	 * @param filename name of the file to be loaded
	 * @param schemaSource url of schema to be used to validate the model
	 * @return Document representation of input xml file
	 */
	public static Document loadXML(String filename, String schemaSource) {
		DOMParser parser = new DOMParser();
		try {
			// Sets validation only if needed
			if (schemaSource != null) {
				parser.setFeature(NAMESPACES_FEATURE_ID, true);
				parser.setFeature(VALIDATION_FEATURE_ID, true);
				parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
				parser.setFeature(VALIDATION_DYNAMIC_FEATURE_ID, true);
				parser.setProperty(EXTERNAL_SCHEMA_LOCATION_PROPERTY_ID, schemaSource);
			}

			FileReader fr = new FileReader(filename);
			InputSource in_source = new InputSource(fr);
			//TODO: the parser must first be created
			parser.parse(in_source);
			return parser.getDocument();
		} catch (SAXException e) {
			System.err.println("XMLLoader Error - An error occurs while attempting to parse the document \"" + e.getMessage() + "\".");
			return null;
		} catch (IOException e) {
			System.err.println("XMLLoader Error - An error occurs while attempting to parse the document.");
			return null;
		}
	}

	/**
	 * Loads an XML file, returning the Document representation of it. This method is generic
	 * and can be used to load every xml file. Actually it is used by <code>XMLReader</code>
	 * and by <code>GuiXMLReader</code>. This method will <b>not</b> validate input file.
	 * @param filename name of the file to be loaded
	 * @return Document representation of input xml file
	 */
	public static Document loadXML(String filename) {
		return loadXML(filename, null);
	}

	// -----------------------------------------------------------------------------------------------

	// --- Debug -------------------------------------------------------------------------------------
	/**
	 * This method is used for debug purpose to write a portion of xml on standard output.
	 * This can be removed freely!
	 * @param node node to be written on standard output
	 */
	protected static void write(Node node) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(node), new StreamResult(System.out));
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	// -----------------------------------------------------------------------------------------------

}
