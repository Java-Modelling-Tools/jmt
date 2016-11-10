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

import jmt.common.xml.XSDSchemaLoader;
import jmt.engine.QueueNet.SimConstants;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.definitions.PAResultsModel;
import jmt.gui.common.definitions.StoredResultsModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * <p>Title: XML Results Reader</p>
 * <p>Description: Reads stored results information from an XML file. This class is designed
 * to work both with <code>XMLResultsWriter</code> and with engine's generated output files.
 * Obviously the first one will provide much more informations.</p>
 * 
 * @author Bertoli Marco
 *         Date: 3-ott-2005
 *         Time: 12.27.00
 * 
 * Modified by Ashanka (Aug 09):
 * Desc: The code to include the changes for label changes from 
 *       1. Queue Length to Customer Number 
 *       2. Number of Customers to System Customer Number 
 * 
 * Modified by Ashanka (Sep 09):
 * Desc: The code to include the changes for label changes from 
 *       1. Customer Number to Number of Customers
 *       2. System Customer Number to System Number of Customers.
 */
public class XMLResultsReader implements XMLResultsConstants {

	/**
	 * Loads stored GUI results into specified StoredResultsModel
	 * @param archiveName name of the archive to be opened
	 * @param model CommonModel where all loaded informations should be stored
	 */
	public static void loadGuiModel(StoredResultsModel model, File archiveName) {
		Document doc = XMLReader.loadXML(archiveName.getAbsolutePath(), XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_GUI_RESULTS));
		parseXML(doc, model);
	}

	/**
	 * Loads stored engine results into specified StoredResultsModel
	 * @param archiveName name of the archive to be opened
	 * @param model CommonModel where all loaded informations should be stored
	 */
	public static void loadModel(StoredResultsModel model, File archiveName) {
		Document doc = XMLReader.loadXML(archiveName.getAbsolutePath(), XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_MODEL_RESULTS));
		parseXML(doc, model);
	}

	/**
	 * Parses given <code>XMLResultsWriter</code> or engine's generated output XML Document.
	 * @param xml Document to be parsed
	 * @param model data model to be elaborated
	 */
	public static void parseXML(Document xml, MeasureDefinition model) {
		parseXML(xml.getDocumentElement(), model);
	}

	/**
	 * Parses given <code>XMLResultsWriter</code> or engine's generated output XML Document.
	 * @param root xml document root to be parsed
	 * @param model data model to be elaborated
	 */
	public static void parseXML(Element root, MeasureDefinition model) {
		if (model instanceof PAResultsModel) {
			if (root.getNodeName().equals(XML_DOCUMENT_ROOT)) {
				setMeasures(root, (PAResultsModel) model);
			}
		} else {
			if (root.getNodeName().equals(XML_DOCUMENT_ROOT)) {
				// XMLResultsWriter output file
				setMeasures(root, (StoredResultsModel) model);
			} else if (root.getNodeName().equals(XML_DOCUMENT_O_ROOT)) {
				// Engine's output file
				setEngineMeasures(root, (StoredResultsModel) model);
			}
		}
	}

	/**
	 * Loads measures data from saved XML document. Used only for parametric
	 * analysis results.
	 * @param root root element of xml document
	 * @param model data structure
	 */
	private static void setMeasures(Element root, PAResultsModel model) {
		String logDecimalSeparator = root.getAttribute(XML_A_ROOT_LOG_DECIMAL_SEP);
		model.setLogDecimalSeparator(logDecimalSeparator);
		String logCsvDelimiter = root.getAttribute(XML_A_ROOT_LOG_DELIMITER);
		model.setLogCsvDelimiter(logCsvDelimiter);

		NodeList measures = root.getElementsByTagName(XML_E_MEASURE);
		for (int i = 0; i < measures.getLength(); i++) {
			Element current = (Element) measures.item(i);
			String measureName = current.getAttribute(XML_A_MEASURE_NAME);
			// Adds its samples
			NodeList samples = current.getElementsByTagName(XML_E_SAMPLE);
			model.addMeasure(measureName, current.getAttribute(XML_A_MEASURE_STATION), current.getAttribute(XML_A_MEASURE_CLASS), Double
					.parseDouble(current.getAttribute(XML_A_MEASURE_ALPHA)), Double.parseDouble(current.getAttribute(XML_A_MEASURE_PRECISION)),
					Integer.parseInt(current.getAttribute(XML_A_MEASURE_TYPE)), current.getAttribute(XML_A_MEASURE_NODETYPE));
			for (int j = 0; j < samples.getLength(); j++) {
				Element sample = (Element) samples.item(j);
				double mean = 0;
				double upper = 0;
				double lower = 0;
				boolean valid;

				valid = Boolean.valueOf(sample.getAttribute(XML_A_SAMPLE_VALIDITY)).booleanValue();
				mean = Double.parseDouble(sample.getAttribute(XML_A_SAMPLE_MEAN));
				// If the sample was calculated with the requested precision simply parse
				if (valid) {
					upper = Double.parseDouble(sample.getAttribute(XML_A_SAMPLE_UPPERBOUND));
					lower = Double.parseDouble(sample.getAttribute(XML_A_SAMPLE_LOWERBOUND));
				}
				//else check or fields equal to the String "Infinity"
				else {
					String u = sample.getAttribute(XML_A_SAMPLE_UPPERBOUND);
					if (u.equals("Infinity")) {
						upper = Double.POSITIVE_INFINITY;
					} else {
						upper = Double.parseDouble(sample.getAttribute(XML_A_SAMPLE_UPPERBOUND));
					}
					String l = sample.getAttribute(XML_A_SAMPLE_LOWERBOUND);
					if (l.equals("Infinity")) {
						lower = Double.POSITIVE_INFINITY;
					} else {
						lower = Double.parseDouble(sample.getAttribute(XML_A_SAMPLE_LOWERBOUND));
					}
				}
				model.addSample(measureName, lower, mean, upper, valid);
			}
		}
	}

	/**
	 * Loads measures data from saved XML document
	 * @param root root element of xml document
	 * @param model data structure
	 */
	private static void setMeasures(Element root, StoredResultsModel model) {
		double polling = Double.parseDouble(root.getAttribute(XML_A_ROOT_POLLING));
		String logDecimalSeparator = root.getAttribute(XML_A_ROOT_LOG_DECIMAL_SEP);
		model.setLogDecimalSeparator(logDecimalSeparator);
		String logCsvDelimiter = root.getAttribute(XML_A_ROOT_LOG_DELIMITER);
		model.setLogCsvDelimiter(logCsvDelimiter);

		model.setPollingInterval(polling);
		NodeList measures = root.getElementsByTagName(XML_E_MEASURE);
		for (int i = 0; i < measures.getLength(); i++) {
			Element current = (Element) measures.item(i);
			String measureName = current.getAttribute(XML_A_MEASURE_NAME);
			String fileName = current.getAttribute(XML_A_MEASURE_LOG);
			if (fileName != null && fileName.length() == 0) {
				fileName = null;
			}
			// Add measure
			String discarded = current.getAttribute(XML_A_MEASURE_DISCARDEDSAMPLES);
			model.addMeasure(measureName, current.getAttribute(XML_A_MEASURE_STATION), 
					current.getAttribute(XML_A_MEASURE_CLASS), 
					Double.parseDouble(current.getAttribute(XML_A_MEASURE_ALPHA)), 
					Double.parseDouble(current.getAttribute(XML_A_MEASURE_PRECISION)),
					Integer.parseInt(current.getAttribute(XML_A_MEASURE_SAMPLES)),
					discarded == null || discarded.length() == 0 ? 0 : Integer.parseInt(discarded),
							Integer.parseInt(current.getAttribute(XML_A_MEASURE_STATE)),
							Integer.parseInt(current.getAttribute(XML_A_MEASURE_TYPE)), 
							current.getAttribute(XML_A_MEASURE_NODETYPE), fileName);

			// Adds its samples
			NodeList samples = current.getElementsByTagName(XML_E_SAMPLE);
			for (int j = 0; j < samples.getLength(); j++) {
				Element sample = (Element) samples.item(j);
				double mean = 0;
				double upper = 0;
				double lower = 0;
				double lastIntervalAvgValue = 0;
				double simulationTime = 0;

				mean = Double.parseDouble(sample.getAttribute(XML_A_SAMPLE_MEAN));
				if (sample.getAttribute(XML_A_LAST_INTERVAL_AVG_VALUE) != null && sample.getAttribute(XML_A_LAST_INTERVAL_AVG_VALUE)!="") {
					lastIntervalAvgValue= Double.parseDouble(sample.getAttribute(XML_A_LAST_INTERVAL_AVG_VALUE));
				}
				if (sample.getAttribute(XML_A_TIME) != null && sample.getAttribute(XML_A_TIME)!="") {
					simulationTime= Double.parseDouble(sample.getAttribute(XML_A_TIME));
				}
				// Gets upperBound if specified
				if (sample.getAttribute(XML_A_SAMPLE_UPPERBOUND) != null && sample.getAttribute(XML_A_SAMPLE_UPPERBOUND) != "") {
					upper = Double.parseDouble(sample.getAttribute(XML_A_SAMPLE_UPPERBOUND));
				}
				// Gets lowerBound if specified
				if (sample.getAttribute(XML_A_SAMPLE_LOWERBOUND) != null && sample.getAttribute(XML_A_SAMPLE_LOWERBOUND) != "") {
					lower = Double.parseDouble(sample.getAttribute(XML_A_SAMPLE_LOWERBOUND));
				}
				// Adds sample
				model.addMeasureSample(measureName, lastIntervalAvgValue, simulationTime, mean, upper, lower);
			}
		}
	}

	/**
	 * Loads measures data from XML document generated by engine
	 * @param root root element of xml document
	 * @param model data structure
	 */
	private static void setEngineMeasures(Element root, StoredResultsModel model) {
		String logDecimalSeparator = root.getAttribute(XML_A_ROOT_LOG_DECIMAL_SEP);
		model.setLogDecimalSeparator(logDecimalSeparator);
		String logCsvDelimiter = root.getAttribute(XML_A_ROOT_LOG_DELIMITER);
		model.setLogCsvDelimiter(logCsvDelimiter);

		NodeList measures = root.getElementsByTagName(XML_EO_MEASURE);
		for (int i = 0; i < measures.getLength(); i++) {
			Element current = (Element) measures.item(i);
			String fileName = current.getAttribute(XML_A_MEASURE_LOG);
			if (fileName != null && fileName.length() == 0) {
				fileName = null;
			}
			String type = current.getAttribute(XML_AO_MEASURE_TYPE);
			String stationName = current.getAttribute(XML_AO_MEASURE_STATION).isEmpty() ? "Network" : current.getAttribute(XML_AO_MEASURE_STATION);
			String className;
			String lowerType = type.toLowerCase();
			if (current.getAttribute(XML_AO_MEASURE_CLASS).isEmpty()) {
				className = (lowerType.startsWith("firing") && lowerType.endsWith("throughput")) ? "All modes" : "All classes";
			} else {
				className = current.getAttribute(XML_AO_MEASURE_CLASS);
			}

			String success = current.getAttribute(XML_AO_MEASURE_SUCCESSFUL);
			boolean successful = success.toLowerCase().equals("true");
			// Optional Elements
			String attr;
			attr = current.getAttribute(XML_AO_MEASURE_MEAN);
			double mean = 0;
			if (attr != null && attr != "") {
				mean = Double.parseDouble(attr);
			}
			double lastIntervalAvgValue = 0;
			if (attr != null && attr != "") {
				lastIntervalAvgValue = Double.parseDouble(attr);
			}
			double simulationTime = 0;
			if (attr != null && attr != "") {
				simulationTime = Double.parseDouble(attr);
			}
			String nodeType = current.getAttribute(XML_AO_MEASURE_NODETYPE);
			attr = current.getAttribute(XML_AO_MEASURE_UPPER);
			double upper = 0;
			if (attr != null && attr != "") {
				upper = Double.parseDouble(attr);
			}
			attr = current.getAttribute(XML_AO_MEASURE_LOWER);
			double lower = 0;
			if (attr != null && attr != "") {
				lower = Double.parseDouble(attr);
			}
			attr = current.getAttribute(XML_AO_MEASURE_SAMPLES);
			int samples = 0;
			if (attr != null && attr != "") {
				samples = Integer.parseInt(attr);
			}
			attr = current.getAttribute(XML_AO_MEASURE_DISCARDEDSAMPLES);
			int discarded = 0;
			if (attr != null && attr != "") {
				discarded = Integer.parseInt(attr);
			}
			attr = current.getAttribute(XML_AO_MEASURE_ALPHA);
			double alpha = 0;
			// Inverts alpha
			if (attr != null && attr != "") {
				alpha = 1 - Double.parseDouble(attr);
			}
			attr = current.getAttribute(XML_AO_MEASURE_PRECISION);
			double precision = 0;
			if (attr != null && attr != "") {
				precision = Double.parseDouble(attr);
			}

			// Gets measure state
			int state;
			if (samples == 0) {
				state = MeasureDefinition.MEASURE_NO_SAMPLES;
			} else if (successful) {
				state = MeasureDefinition.MEASURE_SUCCESS;
			} else {
				state = MeasureDefinition.MEASURE_FAILED;
			}

			// Creates unique measure name
			String Name = stationName + "_" + className + "_" + type;

			// Decodes measure type
			int numType = 0;
			if ((lowerType.startsWith("number") && lowerType.endsWith("customers"))
					|| (lowerType.startsWith("queue") && lowerType.endsWith("length"))
					|| (lowerType.startsWith("customer") && lowerType.endsWith("number") && !"".equalsIgnoreCase(stationName))) {
				numType = SimConstants.QUEUE_LENGTH;
			} else if (lowerType.startsWith("queue") && lowerType.endsWith("time")) {
				numType = SimConstants.QUEUE_TIME;
			} else if (lowerType.startsWith("response") && lowerType.endsWith("time")) {
				numType = SimConstants.RESPONSE_TIME;
			} else if (lowerType.startsWith("residence") && lowerType.endsWith("time")) {
				numType = SimConstants.RESIDENCE_TIME;
			} else if (lowerType.startsWith("utilization") && type.endsWith("utilization")) {
				numType = SimConstants.UTILIZATION;
			} else if (lowerType.startsWith("throughput") && type.endsWith("throughput")) {
				numType = SimConstants.THROUGHPUT;
			} else if (lowerType.startsWith("drop") && lowerType.endsWith("rate")) {
				numType = SimConstants.DROP_RATE;
			} else if (lowerType.startsWith("system") && lowerType.endsWith("time")) {
				numType = SimConstants.SYSTEM_RESPONSE_TIME;
			} else if (lowerType.startsWith("system") && lowerType.endsWith("throughput")) {
				numType = SimConstants.SYSTEM_THROUGHPUT;
			} else if ((lowerType.startsWith("system") && lowerType.endsWith("customers"))
					|| (lowerType.startsWith("system") && lowerType.endsWith("number"))
					|| (lowerType.startsWith("customer") && lowerType.endsWith("number") && "".equalsIgnoreCase(stationName))) {
				numType = SimConstants.SYSTEM_NUMBER_OF_JOBS;
			} else if (lowerType.startsWith("system") && lowerType.endsWith("rate")) {
				numType = SimConstants.SYSTEM_DROP_RATE;
			} else if (lowerType.startsWith("system") && lowerType.endsWith("power")) {
				numType = SimConstants.SYSTEM_POWER;
			} else if (lowerType.startsWith("response") && lowerType.endsWith("sink")) {
				numType = SimConstants.RESPONSE_TIME_PER_SINK;
			} else if (lowerType.startsWith("throughput") && lowerType.endsWith("sink")) {
				numType = SimConstants.THROUGHPUT_PER_SINK;
			} else if (lowerType.startsWith("fcr") && lowerType.endsWith("weight")) {
				numType = SimConstants.FCR_TOTAL_WEIGHT;
			} else if (lowerType.startsWith("fcr") && lowerType.endsWith("occupation")) {
				numType = SimConstants.FCR_MEMORY_OCCUPATION;
			} else if (lowerType.startsWith("fork") && lowerType.endsWith("customers")) {
				numType = SimConstants.FORK_JOIN_NUMBER_OF_JOBS;
			} else if (lowerType.startsWith("fork") && lowerType.endsWith("time")) {
				numType = SimConstants.FORK_JOIN_RESPONSE_TIME;
			} else if (lowerType.startsWith("firing") && lowerType.endsWith("throughput")) {
				numType = SimConstants.FIRING_THROUGHPUT;
			}
			// Adds loaded informations into model data structure
			model.addMeasure(Name, stationName, className, alpha, precision, samples, discarded, state, numType, nodeType, fileName);
			model.addMeasureSample(Name, mean, lastIntervalAvgValue, simulationTime, upper, lower);
		}
	}

}
