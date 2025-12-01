package linetesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.gui.JMVAWizard;

public class TestUtil {

	final static String JSIM_TESTS_FOLDER_NAME = "jsim"; // must be same as in LineTest
	final static String JMVA_TESTS_FOLDER_NAME = "jmva"; // must be same as in LineTest

	private static List<String> testJSIMGraph(File testModel) {

		try {

			// Create test instance with overridden checkForSave
			JSIMGraphMain jsimGraphMain = new JSIMGraphMain();

			// Get Mediator
			Field mediatorField = JSIMGraphMain.class.getDeclaredField("mediator");
			mediatorField.setAccessible(true);
			Mediator mediator = (Mediator) mediatorField.get(jsimGraphMain);

			mediator.openModel(testModel);

			// Get Model
			Field modelField = Mediator.class.getDeclaredField("model");
			modelField.setAccessible(true);
			JSimGraphModel model = (JSimGraphModel) modelField.get(mediator);

			// Reset results in order to avoid "This operation will overwrite old simulation
			// results. " confirm dialog
			model.setSimulationResults(null);

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

			while (pollerThread.getState() == Thread.State.NEW) {
				Thread.sleep(10); // Wait until it starts
			}

//					System.out.println("dispatcherThread: " + dispatcherThread);
//					System.out.println("pollerThread: " + pollerThread);
//					System.out.println("pollerThread isAlive: " + pollerThread.isAlive());
//					System.out.println("pollerThread state: " + pollerThread.getState());

			// Now join on the real simulation thread
			if (pollerThread.isAlive()) {
//						long startTime = System.currentTimeMillis();
				pollerThread.join();
//						long endTime = System.currentTimeMillis();
//						System.out.println("Time taken: " + (endTime - startTime) + " ms");
			} else {
				System.out.println("pollerThread already finished, join returns immediately.");
			}

			// System.out.println("Simulation truly finished");

			// Get the model file parent folder name
			String parentFolderName = FileUtil.getParentFolderName(testModel);

			if (parentFolderName == null) {
				throw new RuntimeException("Line test model does not have parent folder");
			}

			List<String> folderHierarchy = new ArrayList<String>();
			folderHierarchy.add("linetesting");
			folderHierarchy.add("output");
			folderHierarchy.add(JSIM_TESTS_FOLDER_NAME);
			if (!parentFolderName.equals(JSIM_TESTS_FOLDER_NAME)) {
				folderHierarchy.add(parentFolderName);
			}
			File modelOutputFile = FileUtil.createFileInTestResources(folderHierarchy, testModel.getName());

			if (!modelOutputFile.exists()) {
				throw new FileNotFoundException("Could not create model output file");
			}

			// System.out.println("File created at: " + modelOutputFile.getAbsolutePath());

			// Get ModelLoader
			Field modelLoaderField = Mediator.class.getDeclaredField("modelLoader");
			modelLoaderField.setAccessible(true);
			ModelLoader modelLoader = (ModelLoader) modelLoaderField.get(mediator);

			// Updates station positions into data structure - otherwise next instruction
			// gives error
			mediator.updateStationPositions();
			modelLoader.saveModel(model, jsimGraphMain, modelOutputFile);

			// Closes everything gui (sadly it opens automatically)
			Method doClose = JSIMGraphMain.class.getDeclaredMethod("doClose");
			doClose.setAccessible(true);
			doClose.invoke(jsimGraphMain);
			jsimGraphMain.dispose();

			return getJSIMGraphModelFinalValues(modelOutputFile);

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InterruptedException
				| NoSuchFieldException | IOException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
			System.exit(0);
			return new ArrayList<String>();
		}

	}
	
	private static List<String> testJMVA(File testModel) {

		try {

			JMVAWizard jmvaWizard = new JMVAWizard();

			// Get ModelLoader
			Field modelLoaderField = JMVAWizard.class.getDeclaredField("modelLoader");
			modelLoaderField.setAccessible(true);
			ModelLoader modelLoader = (ModelLoader) modelLoaderField.get(jmvaWizard);

			ExactModel newdata = new ExactModel();
			modelLoader.loadModel(newdata, jmvaWizard, testModel);

			// Get data
			Field dataField = JMVAWizard.class.getDeclaredField("data");
			dataField.setAccessible(true);
			// Set data
			dataField.set(jmvaWizard, newdata);
			
			// Get tempFile
			Field tempFileField = JMVAWizard.class.getDeclaredField("tempFile");
			tempFileField.setAccessible(true);
			// Set tempFile
			tempFileField.set(jmvaWizard, modelLoader.getSelectedFile());
			
			// Solve the model
			Method solve = JMVAWizard.class.getDeclaredMethod("solve");
			solve.setAccessible(true);
			solve.invoke(jmvaWizard);
			
			// solve() is synchronous from the caller’s point of view, even though it internally uses a background thread.
			
			// Get the model file parent folder name
			String parentFolderName = FileUtil.getParentFolderName(testModel);

			if (parentFolderName == null) {
				throw new RuntimeException("Line test model does not have parent folder");
			}

			List<String> folderHierarchy = new ArrayList<String>();
			folderHierarchy.add("linetesting");
			folderHierarchy.add("output");
			folderHierarchy.add(JMVA_TESTS_FOLDER_NAME);
			if (!parentFolderName.equals(JMVA_TESTS_FOLDER_NAME)) {
				folderHierarchy.add(parentFolderName);
			}
			File modelOutputFile = FileUtil.createFileInTestResources(folderHierarchy, testModel.getName());

			if (!modelOutputFile.exists()) {
				throw new FileNotFoundException("Could not create model output file");
			}

			// System.out.println("File created at: " + modelOutputFile.getAbsolutePath());

			// Get data
			ExactModel data = (ExactModel) dataField.get(jmvaWizard);
			
			// Save model
			modelLoader.saveModel(data, jmvaWizard, modelOutputFile);

			// Closes everything gui (sadly it opens automatically)
			Method doClose = JMVAWizard.class.getDeclaredMethod("doClose");
			doClose.setAccessible(true);
			doClose.invoke(jmvaWizard);
			jmvaWizard.dispose();

			return getJMVAModelFinalValues(modelOutputFile);

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException
				| NoSuchFieldException | IOException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
			System.exit(0);
			return new ArrayList<String>();
		}

	}
	
	private static List<String> getJMVAModelFinalValues(File jmvaGraphModelFile) {
		
		//TODO: check how the results are saved in JMVA

		try {
			// Parse file
			Document measuresDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(jmvaGraphModelFile);

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
			return finalValues;
		} catch (SecurityException | IllegalArgumentException | IOException | XPathExpressionException | SAXException
				| ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(0);
			return new ArrayList<String>();
		}
	}

	private static List<String> getJSIMGraphModelFinalValues(File jsimGraphModelFile) {

		try {
			// Parse file
			Document measuresDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(jsimGraphModelFile);

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
			return finalValues;
		} catch (SecurityException | IllegalArgumentException | IOException | XPathExpressionException | SAXException
				| ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(0);
			return new ArrayList<String>();
		}
	}

	public static boolean testLineJSIMGraphModel(File testModel, File testResults) {

		try {

			List<String> finalValues = testJSIMGraph(testModel);

			// === Parse second file: model.jsim-result.jsim ===
			Document solutionDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(testResults);
			solutionDoc.getDocumentElement().normalize();

			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList solutionNodes = (NodeList) xPath.evaluate("//solutions/measure", solutionDoc,
					XPathConstants.NODESET);

			boolean isDoResultsMatch = true;
			int i = 0;

			while (isDoResultsMatch && i < solutionNodes.getLength()) {
				Element solution = (Element) solutionNodes.item(i);
				String meanValue = solution.getAttribute("meanValue");
				String finalValue = finalValues.get(i);
//				System.out.println("finalValue: " + finalValue);
//				System.out.println("meanValue: " + meanValue);
//				System.out.println("match: " + finalValue.equals(meanValue));
				isDoResultsMatch = finalValue.equals(meanValue);
				i++;
			}

			return isDoResultsMatch;

		} catch (SecurityException | IllegalArgumentException | IOException | XPathExpressionException | SAXException
				| ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(0);
			return false;
		}

	}

	public static boolean testJSIMGraphModel(File testModel) {

		List<String> testModelFinalValues = getJSIMGraphModelFinalValues(testModel); // get sim results first before
																						// running simulation, otherwise
																						// results will be overwritten
		List<String> simulatedModelFinalValues = testJSIMGraph(testModel);

		boolean isDoResultsMatch = true;
		int i = 0;

		while (isDoResultsMatch && i < testModelFinalValues.size()) {
			String testModelFinalValue = testModelFinalValues.get(i);
			String simulatedModelFinalValue = simulatedModelFinalValues.get(i);
//			System.out.println("testModelFinalValue: " + testModelFinalValue);
//			System.out.println("simulatedModelFinalValue: " + simulatedModelFinalValue);
//			System.out.println("match: " + testModelFinalValue.equals(simulatedModelFinalValue));
			isDoResultsMatch = testModelFinalValue.equals(simulatedModelFinalValue);
			i++;
		}

		return isDoResultsMatch;

	}

}
