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

package jmt.engine.dataAnalysis;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.SimConstants;
import jmt.engine.QueueNet.SimulationOutput;
import jmt.engine.log.JSimLogger;
import jmt.engine.math.SampleStatistics;
import jmt.engine.simEngine.EngineUtils;
import jmt.engine.simEngine.Simulation;
import jmt.gui.common.definitions.StatisticalOutputsLoader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Saves all the measure outputs in a xml file.
 *
 * @author Stefano, Bertoli Marco
 * @version 13-dic-2004 17.34.35
 * Modified by Bertoli Marco 01-jan-2006 --> BugFixed for linux
 * 8-mar-2006 Added support for global measures
 *       
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public class XMLSimulationOutput extends SimulationOutput {

	private static final String SEPARATOR = System.getProperty("file.separator");
	private static final boolean DEBUG = false;

	private JSimLogger logger = JSimLogger.getLogger();

	//the root element of results xml
	Element root;
	//the Document object corresponding to the results xml
	Document doc;
	//the File containing the simulation results
	File resultsFile;

	//the File containing the original model definition
	File mvaModelDefinition;

	//the File containing the original model definition
	File simModelDefinition;

	private String logDecimalSeparator;
	private String logDelimiter;
	private boolean isTerminalSimulation;

	public XMLSimulationOutput(Simulation simulation) {
		super(simulation);

		this.isTerminalSimulation = simulation.isTerminalSimulation();

		try {
			/////////////////////////////
			//Creating an empty XML Document

			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();

			DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
			doc = docBuilder.newDocument();

			////////////////////////
			//Creating the XML tree

			//create the root element and add it to the document
			root = doc.createElement("solutions");
			doc.appendChild(root);

			//sets the attribute of the root
			if (sim.getName() != null) {
				root.setAttribute("modelName", sim.getName());
			} else {
				root.setAttribute("modelName", "Unknown Model Name");
			}
			root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			//sets the xsd schema of the results xml file
			root.setAttribute("xsi:noNamespaceSchemaLocation", "SIMmodeloutput.xsd");
			//these results have been obtained through a simulation
			root.setAttribute("solutionMethod", "simulation");

			//in the results file, we need the name of the xml file containing
			//the model definition
			String name = sim.getXmlModelDefPath();
			if (name != null) {
				mvaModelDefinition = new File(name);
			}

			//only file name is passed, not the absolute path
			//in fact definition and results will be put in the same directory

			//root.setAttribute("modelDefinitionPath", simModelDefinition.getName());
			if (name != null) {
				root.setAttribute("modelDefinitionPath", name);
			} else {
				root.setAttribute("modelDefinitionPath", ".");
			}

			// Write CSV definition for verbose measures load.
			logDecimalSeparator = simulation.getSimParameters().getLogDecimalSeparator();
			logDelimiter = simulation.getSimParameters().getLogDelimiter();
			root.setAttribute("logDecimalSeparator", simulation.getSimParameters().getLogDecimalSeparator());
			root.setAttribute("logDelimiter", simulation.getSimParameters().getLogDelimiter());
			root.setAttribute("logPath", simulation.getSimParameters().getLogPath());
		} catch (Exception e) {
			logger.error("Error while writing simulator output XML", e);
		}
	}

	/**
	 * Writes the output of the specified measure.
	 */
	private void writeMeasure(Measure measure) {
		Element elem = doc.createElement("measure");
		root.appendChild(elem);

		DynamicDataAnalyzer analyzer = measure.getAnalyzer();

		// Checks null values to avoid problems under linux
		if (measure.getNodeName() == null || measure.getNodeName().equals("")) {
			// aggregate measure
			elem.setAttribute("station", "");
			elem.setAttribute("nodeType", "");
		} else {
			NetNode node = measure.getNetwork().getNode(measure.getNodeName());
			if (node.isBlockingRegionInputStation()) {
				// region measure
				elem.setAttribute("station", node.getBlockingRegionInputStation().getName());
				elem.setAttribute("nodeType", SimConstants.NODE_TYPE_REGION);
			} else {
				// station measure
				elem.setAttribute("station", measure.getNodeName());
				elem.setAttribute("nodeType", SimConstants.NODE_TYPE_STATION);
			}
		}
		// Checks null values to avoid problems under linux
		if (measure.getJobClassName() == null || measure.getJobClassName().equals("")) {
			//aggregate measure
			elem.setAttribute("class", "");
		} else {
			//class measure
			elem.setAttribute("class", measure.getJobClassName());
		}

		//finds and sets measure type
		String type = EngineUtils.encodeMeasureType(measure.getMeasureType());
		elem.setAttribute("measureType", type);

		//analyzer confidence requirements
		elem.setAttribute("maxSamples", Integer.toString(analyzer.getMaxData()));
		elem.setAttribute("precision", Double.toString(analyzer.getPrecision()));
		elem.setAttribute("alfa", Double.toString(analyzer.getAlfa()));

		//analyzer has been successful?
		boolean success = analyzer.getSuccess();
		elem.setAttribute("successful", Boolean.toString(success));

		//number of analyzed and discarded samples
		elem.setAttribute("analyzedSamples", Integer.toString(measure.getAnalyzedSamples()));
		elem.setAttribute("discardedSamples", Integer.toString(measure.getDiscardedSamples()));

		//this is the estimated mean, but it may be wrong
		elem.setAttribute("meanValue", Double.toString(measure.getEstimatedMeanValue()));
		elem.setAttribute("upperLimit", Double.toString(measure.getUpperLimit()));
		elem.setAttribute("lowerLimit", Double.toString(measure.getLowerLimit()));

		//log file if verbose measure was selected
		if (measure.getOutput() != null && measure.getOutput().getOutputFile() != null) {
			elem.setAttribute("logFile", measure.getOutput().getOutputFile().getAbsolutePath());
			if (isTerminalSimulation) {
				try {
					StatisticalOutputsLoader loader = new StatisticalOutputsLoader(measure, logDecimalSeparator, logDelimiter);
					SampleStatistics stat = loader.getStatistics();
					elem.setAttribute("mean", Double.toString(stat.getMean()));
					elem.setAttribute("variance", Double.toString(stat.getVariance()));
					elem.setAttribute("standardDeviation", Double.toString(stat.getStandardDeviation()));
					elem.setAttribute("coefficientOfVariation", Double.toString(stat.getCoefficienfOfVariation()));
					elem.setAttribute("skeweness", Double.toString(stat.getSkew()));
					elem.setAttribute("kurtosis", Double.toString(stat.getKurtosis()));
					elem.setAttribute("firstPowerMoment", Double.toString(stat.getMean()));
					elem.setAttribute("secondPowerMoment", Double.toString(stat.getMoment2()));
					elem.setAttribute("thirdPowerMoment", Double.toString(stat.getMoment3()));
					elem.setAttribute("fourthPowerMoment", Double.toString(stat.getMoment4()));
					elem.setAttribute("minValue", Double.toString(stat.getMin()));
					elem.setAttribute("maxValue", Double.toString(stat.getMax()));
					elem.setAttribute("minSimulationTime", Double.toString(stat.getMinSimTime()));
					elem.setAttribute("maxSimulationTime", Double.toString(stat.getMaxSimTime()));
				} catch (IOException | ParseException ex) {
					Logger.getLogger(XMLSimulationOutput.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	public File writeAllMeasures() {
		for (Measure element : measureList) {
			//writes all the measures in a Document
			writeMeasure(element);
		}

		/////////////////
		//Output the XML
		try {
			//set up a transformer
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

			//we want to save the xml results in the same directory of the model definition file
			String parent = null;
			String xmlResultsName = null;

			if (mvaModelDefinition != null) {
				//mvaModelDefinition defined
				if (mvaModelDefinition.isAbsolute()) {
					//the passed filename is absolute
					parent = mvaModelDefinition.getParent();
					if (parent.endsWith(SEPARATOR)) {
						xmlResultsName = parent + "res_sim_" + mvaModelDefinition.getName();
					} else {
						xmlResultsName = parent + SEPARATOR + "res_sim_" + mvaModelDefinition.getName();
					}
				} else {
					//the passed filename is not absolute
					xmlResultsName = SEPARATOR + "res_sim_" + mvaModelDefinition.getName();
				}
			} else {
				//mvaModelDefinition not defined
				//use sim model path
				String simModelPath = sim.getXmlSimModelDefPath();
				File simModelFile;

				if (simModelPath != null) {
					//sim model file exist
					simModelFile = new File(simModelPath);
					xmlResultsName = simModelFile.getParent() + SEPARATOR + "res_sim_" + sim.getName();
				} else {
					//get the user dir
					String curDir = System.getProperty("user.dir");
					String name;
					if (sim.getName() == null) {
						name = "";
					} else {
						name = "_" + sim.getName();
					}
					xmlResultsName = curDir + SEPARATOR + "res_sim_JMT" + name;
				}
			}

			//creates the results file
			resultsFile = new File(xmlResultsName);
			if (DEBUG) {
				System.out.println(resultsFile.getAbsolutePath());
			}

			DOMSource source = new DOMSource(doc);

			// Prepare the output file
			File temp = File.createTempFile("~jmt_sim_output", ".xml", resultsFile.getParentFile());
			StreamResult result = new StreamResult(temp);

			// Write the DOM document to the file
			trans.transform(source, result);

			// commit
			if (resultsFile.exists()) {
				resultsFile.delete();
			}

			temp.renameTo(resultsFile);

			// Check because sometimes rename fails...
			if (resultsFile.exists()) {
				return resultsFile;
			} else {
				return temp;
			}
		} catch (javax.xml.transform.TransformerConfigurationException exc) {
			exc.printStackTrace();
		} catch (javax.xml.transform.TransformerException exc) {
			exc.printStackTrace();
		} catch (java.io.IOException exc) {
			exc.printStackTrace();
		} catch (NullPointerException exc) {
			exc.printStackTrace();
		}
		return null;
	}

}
