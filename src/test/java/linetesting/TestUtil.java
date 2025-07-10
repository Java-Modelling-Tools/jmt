package linetesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jmt.gui.common.controller.DispatcherThread;
import jmt.gui.common.xml.ModelLoader;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;
import jmt.gui.jsimgraph.mainGui.JSIMGraphMain;

public class TestUtil {

	final static String TEST_FILES_BASE_FOLDER_PATH = "/linetesting/input/";
	final static String TEST_MODEL_FILE_NAME = "model.jsim";
	final static String TEST_MODEL_RESULTS_FILE_NAME = "model.jsim-result.jsim";

	public static boolean testJSIMGraph(String testFolderName) {

		try {

			String testFolder = TEST_FILES_BASE_FOLDER_PATH + testFolderName + "/";
			String testFilePath = testFolder + TEST_MODEL_FILE_NAME;
			String testResultFilePath = testFolder + TEST_MODEL_RESULTS_FILE_NAME;

			Path resourcePath = Paths.get(TestUtil.class.getResource(testFilePath).toURI());

			File resourceFile = resourcePath.toFile();

			if (!resourceFile.exists()) {
				throw new FileNotFoundException("Test file not found");
			}

			// Create test instance with overridden checkForSave
			JSIMGraphMain jsimGraphMain = new JSIMGraphMain();

			// Get Mediator
			Field mediatorField = JSIMGraphMain.class.getDeclaredField("mediator");
			mediatorField.setAccessible(true);
			Mediator mediator = (Mediator) mediatorField.get(jsimGraphMain);

			mediator.openModel(resourceFile);

			mediator.startSimulation();

			// System.out.println("Simulation truly started");

			// Get Dispatcher
			Field dispatcherField = Mediator.class.getDeclaredField("dispatcher");
			dispatcherField.setAccessible(true);
			DispatcherThread dispatcherThread = (DispatcherThread) dispatcherField.get(mediator);

			// Get the poller thread
			Field pollerField = DispatcherThread.class.getDeclaredField("poller");
			pollerField.setAccessible(true);
			Thread pollerThread = (Thread) pollerField.get(dispatcherThread);

			// System.out.println("poller isAlive: " + pollerThread.isAlive());

			// Now join on the real simulation thread
			pollerThread.join();

			// System.out.println("Simulation truly finished");

			// Get the model file parent folder name
			String parentFolderName = resourcePath.getParent().getFileName().toString();

			String[] folderHierarchy = { "linetesting", "output", parentFolderName };
			File modelOutputFile = FileUtil.createFileInTestResources(folderHierarchy, "testedModel.jsimg",
					"Hierarchical content!");

			if (!modelOutputFile.exists()) {
				throw new FileNotFoundException("Could not create model output file");
			}

			// System.out.println("File created at: " + modelOutputFile.getAbsolutePath());

			// Get ModelLoader
			Field modelLoaderField = Mediator.class.getDeclaredField("modelLoader");
			modelLoaderField.setAccessible(true);
			ModelLoader modelLoader = (ModelLoader) modelLoaderField.get(mediator);

			// Get Model
			Field modelField = Mediator.class.getDeclaredField("model");
			modelField.setAccessible(true);
			JSimGraphModel model = (JSimGraphModel) modelField.get(mediator);

			// Updates station positions into data structure - otherwise next instruction
			// gives error
			mediator.updateStationPositions();
			modelLoader.saveModel(model, jsimGraphMain, modelOutputFile);

			// === Parse first file: testfile.jsimg ===
			Document measuresDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(modelOutputFile);

			// Normalize the XML structure
			measuresDoc.getDocumentElement().normalize();

			// Use XPath to find the <results> element
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList measureNodes = (NodeList) xPath.evaluate("//results/measure", measuresDoc, XPathConstants.NODESET);

			List<String> finalValues = new ArrayList<String>();
			for (int i = 0; i < measureNodes.getLength(); i++) {
				Element measure = (Element) measureNodes.item(i);
				String finalValue = measure.getAttribute("finalValue");
				finalValues.add(finalValue);
			}

			// System.out.println("finalValues: " + finalValues);

			// === Parse second file: model.jsim-result.jsim ===
			Path resultsFilePath = Paths.get(TestUtil.class.getResource(testResultFilePath).toURI());

			File resultsFile = resultsFilePath.toFile();

			if (!resultsFile.exists()) {
				throw new FileNotFoundException("Test results file not found");
			}

			Document solutionDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resultsFile);
			solutionDoc.getDocumentElement().normalize();

			NodeList solutionNodes = (NodeList) xPath.evaluate("//solutions/measure", solutionDoc,
					XPathConstants.NODESET);

			boolean isDoResultsMatch = true;
			int i = 0;

			while (isDoResultsMatch && i < solutionNodes.getLength()) {
				Element solution = (Element) solutionNodes.item(i);
				String meanValue = solution.getAttribute("meanValue");
				String finalValue = finalValues.get(i);
				// System.out.println("finalValue: " + finalValue);
				// System.out.println("meanValue: " + meanValue);
				// System.out.println("match: " + finalValue.equals(meanValue));
				isDoResultsMatch = finalValue.equals(meanValue);
				i++;
			}

			return isDoResultsMatch;

		} catch (URISyntaxException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InterruptedException | NoSuchFieldException | IOException | XPathExpressionException | SAXException
				| ParserConfigurationException e) {
			e.printStackTrace();
			return false;
		}

	}

}
