package linetesting;

import static linetesting.TestConstants.JMVA_TESTS_FOLDER_NAME;
import static linetesting.TestConstants.JSIMWIZ_TESTS_FOLDER_NAME;
import static linetesting.TestConstants.JSIM_TESTS_FOLDER_NAME;

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
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.xml.ModelLoader;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;
import jmt.gui.jsimgraph.mainGui.JSIMGraphMain;
import jmt.gui.jsimwiz.JSIMWizMain;
import jmt.gui.jsimwiz.definitions.JSIMModel;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.gui.JMVAWizard;

public class TestUtil {

	private static File createOutputFile(File testModel, String toolFolderName) throws IOException {
		String parentFolderName = FileUtil.getParentFolderName(testModel);

		if (parentFolderName == null) {
			throw new RuntimeException("Line test model does not have parent folder");
		}

		List<String> folderHierarchy = new ArrayList<>();
		folderHierarchy.add("linetesting");
		folderHierarchy.add("output");
		folderHierarchy.add(toolFolderName);

		if (!parentFolderName.equals(toolFolderName)) {
			folderHierarchy.add(parentFolderName);
		}

		File modelOutputFile = FileUtil.createFileInTestResources(folderHierarchy, testModel.getName());

		if (!modelOutputFile.exists()) {
			throw new FileNotFoundException("Could not create model output file");
		}

		return modelOutputFile;
	}

	private static File testJSIMGraph(File testModel) {

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

			// Get Dispatcher
			Field dispatcherField = Mediator.class.getDeclaredField("dispatcher");
			dispatcherField.setAccessible(true);
			DispatcherThread dispatcherThread = (DispatcherThread) dispatcherField.get(mediator);

			// Get the poller thread
			Field pollerField = DispatcherThread.class.getDeclaredField("poller");
			pollerField.setAccessible(true);
			Thread pollerThread = (Thread) pollerField.get(dispatcherThread);

			awaitSimulationEnd(pollerThread);

			File modelOutputFile = createOutputFile(testModel, JSIM_TESTS_FOLDER_NAME);

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

			return modelOutputFile;

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException
				| IOException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
			// System.exit(0);
			throw new RuntimeException("Error executing JSIMGraph test", e);
		}

	}

	private static File testJMVA(File testModel) {

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

			// solve() is synchronous from the caller’s point of view, even though it
			// internally uses a background thread.

			File modelOutputFile = createOutputFile(testModel, JMVA_TESTS_FOLDER_NAME);

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

			return modelOutputFile;

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException
				| IOException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
			// System.exit(0);
			throw new RuntimeException("Error executing JMVA test", e);
		}

	}

	private static File testJSIMWiz(File testModel) {

		try {

			// Create test instance with overridden checkForSave
			JSIMWizMain jsimWizMain = new JSIMWizMain();

			// Get Mediator
			Field modelLoaderField = JSIMWizMain.class.getDeclaredField("modelLoader");
			modelLoaderField.setAccessible(true);
			ModelLoader modelLoader = (ModelLoader) modelLoaderField.get(jsimWizMain);

			JSIMModel tmpmodel = new JSIMModel();

			Field currentFileField = JSIMWizMain.class.getDeclaredField("currentFile");
			currentFileField.setAccessible(true);

			Field jsimWizModelField = JSIMWizMain.class.getDeclaredField("model");
			jsimWizModelField.setAccessible(true);
			CommonModel model = (CommonModel) jsimWizModelField.get(jsimWizMain);

			int state = modelLoader.loadModel(tmpmodel, jsimWizMain, testModel);

			if (state == ModelLoader.SUCCESS || state == ModelLoader.WARNING) {

				currentFileField.set(jsimWizMain, modelLoader.getSelectedFile());

				jsimWizModelField.set(jsimWizMain, tmpmodel);

				model.resetSaveState();
			} else {
				throw new RuntimeException("Error loading JSIMWIZ test");
			}

			// Corrects eventual problems on preloading for closed classes
			model.manageJobs();

			jsimWizMain.launchSimulation();

			// Get Dispatcher
			Field dispatcherField = JSIMWizMain.class.getDeclaredField("dispatcher");
			dispatcherField.setAccessible(true);
			DispatcherThread dispatcherThread = (DispatcherThread) dispatcherField.get(jsimWizMain);

			// Get the poller thread
			Field pollerField = DispatcherThread.class.getDeclaredField("poller");
			pollerField.setAccessible(true);
			Thread pollerThread = (Thread) pollerField.get(dispatcherThread);

			awaitSimulationEnd(pollerThread);

			File modelOutputFile = createOutputFile(testModel, JSIMWIZ_TESTS_FOLDER_NAME);

			// System.out.println("File created at: " + modelOutputFile.getAbsolutePath());

			modelLoader.saveModel(model, jsimWizMain, modelOutputFile);

			// Closes everything gui (sadly it opens automatically)
			Method doClose = JSIMWizMain.class.getDeclaredMethod("doClose");
			doClose.setAccessible(true);
			doClose.invoke(jsimWizMain);
			jsimWizMain.dispose();

			return modelOutputFile;

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException
				| IOException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
			// System.exit(0);
			throw new RuntimeException("Error executing JSIMGraph test", e);
		}

	}

	private static void awaitSimulationEnd(Thread pollerThread) {
		try {
			// System.out.println("Simulation truly started");

			while (pollerThread.getState() == Thread.State.NEW) {
				Thread.sleep(10); // Wait until it starts
			}

//							System.out.println("dispatcherThread: " + dispatcherThread);
//							System.out.println("pollerThread: " + pollerThread);
//							System.out.println("pollerThread isAlive: " + pollerThread.isAlive());
//							System.out.println("pollerThread state: " + pollerThread.getState());

			// Now join on the real simulation thread
			if (pollerThread.isAlive()) {
//								long startTime = System.currentTimeMillis();
				pollerThread.join();
//								long endTime = System.currentTimeMillis();
//								System.out.println("Time taken: " + (endTime - startTime) + " ms");
			} else {
				System.out.println("pollerThread already finished, join returns immediately.");
			}

			// System.out.println("Simulation truly finished");
		} catch (SecurityException | IllegalArgumentException | InterruptedException e) {
			e.printStackTrace();
			// System.exit(0);
			throw new RuntimeException("Error waiting for simulation to end", e);
		}
	}

	private static List<String> getJMVAModelFinalValues(File jmvaGraphModelFile) {
		try {
			// Parse file
			Document measuresDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(jmvaGraphModelFile);

			// Normalize the XML structure
			measuresDoc.getDocumentElement().normalize();

			// Use XPath to find the <measure> elements within <solutions>
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList measureNodes = (NodeList) xPath.evaluate("//solutions//measure", measuresDoc,
					XPathConstants.NODESET);

			List<String> finalValues = new ArrayList<>();
			for (int i = 0; i < measureNodes.getLength(); i++) {
				Element measure = (Element) measureNodes.item(i);
				// JMVA uses 'meanValue', not 'finalValue'
				String finalValue = measure.getAttribute("meanValue");
				finalValues.add(finalValue);
			}

			return finalValues;
		} catch (Exception e) {
			throw new RuntimeException("Error parsing JMVA results from file: " + jmvaGraphModelFile.getName(), e);
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
			// System.exit(0);
			throw new RuntimeException("Error parsing JSIM results", e);
		}
	}

	private static List<String> getJSIMWizModelFinalValues(File jsimwModelFile) {
		// the results structure of the .jsimw XML is exactly the same as the .jsim (JSIMGraph) structure
		return getJSIMGraphModelFinalValues(jsimwModelFile);
	}

	private static List<String> getExpectedMeanValues(File testResults) throws Exception {
		Document solutionDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(testResults);
		solutionDoc.getDocumentElement().normalize();

		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList solutionNodes = (NodeList) xPath.evaluate("//solutions/measure", solutionDoc, XPathConstants.NODESET);

		List<String> meanValues = new ArrayList<>();
		for (int i = 0; i < solutionNodes.getLength(); i++) {
			Element solution = (Element) solutionNodes.item(i);
			meanValues.add(solution.getAttribute("meanValue"));
		}
		return meanValues;
	}

	private static String getAlgorithmName(File modelFile) {
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(modelFile);
			doc.getDocumentElement().normalize();

			XPath xPath = XPathFactory.newInstance().newXPath();
			// Fetches the 'name' attribute from <algorithm> inside <solutions>
			String algName = (String) xPath.evaluate("//solutions/algorithm/@name", doc, XPathConstants.STRING);

			return algName != null && !algName.isEmpty() ? algName : null;
		} catch (Exception e) {
			System.err.println("Could not parse algorithm name from: " + modelFile.getName());
			return null;
		}
	}

	public static boolean checkResults(List<String> testModelFinalValues, List<String> simulatedModelFinalValues) {

		// Bulletproof size check
		if (testModelFinalValues.size() != simulatedModelFinalValues.size()) {
			System.err.println("Mismatch in number of results! Expected: " + testModelFinalValues.size() + ", Got: "
					+ simulatedModelFinalValues.size());
			return false;
		}

		int i = 0;

		for (String testModelFinalValue : testModelFinalValues) {
			String simulatedModelFinalValue = simulatedModelFinalValues.get(i);
//			System.out.println("testModelFinalValue: " + testModelFinalValue);
//			System.out.println("simulatedModelFinalValue: " + simulatedModelFinalValue);
//			System.out.println("match: " + testModelFinalValue.equals(simulatedModelFinalValue));
			if (!testModelFinalValue.equals(simulatedModelFinalValue)) {
				return false;
			}
			i++;
		}

		return true;

	}

	public static boolean testLineJSIMGraphModel(File testModel, File testResults) {
		try {
			File simulatedModelFile = testJSIMGraph(testModel);

			List<String> actualValues = getJSIMGraphModelFinalValues(simulatedModelFile);

			List<String> expectedMeanValues = getExpectedMeanValues(testResults);

			return checkResults(expectedMeanValues, actualValues);

		} catch (Exception e) {
			e.printStackTrace();
			// Returning false causes the assertTrue() in LineTest to fail gracefully
			return false;
		}
	}

	public static boolean testJSIMGraphModel(File testModel) {

		File simulatedModelFile = testJSIMGraph(testModel);

		List<String> expectedValues = getJSIMGraphModelFinalValues(testModel);
		List<String> actualValues = getJSIMGraphModelFinalValues(simulatedModelFile);

		return checkResults(expectedValues, actualValues);
	}

	public static boolean testJMVAModel(File testModel) {

		File simulatedModelFile = testJMVA(testModel);

		String expectedAlg = getAlgorithmName(testModel);
		String actualAlg = getAlgorithmName(simulatedModelFile);

		if (expectedAlg != null && actualAlg != null && !expectedAlg.equals(actualAlg)) {
			System.err.println("Algorithm mismatch! Expected: " + expectedAlg + ", Got: " + actualAlg);
			return false;
		}

		List<String> expectedValues = getJMVAModelFinalValues(testModel);
		List<String> actualValues = getJMVAModelFinalValues(simulatedModelFile);

		return checkResults(expectedValues, actualValues);

	}

	public static boolean testJSIMWizModel(File testModel) {

		File simulatedModelFile = testJSIMWiz(testModel);

		List<String> expectedValues = getJSIMWizModelFinalValues(testModel);
		List<String> actualValues = getJSIMWizModelFinalValues(simulatedModelFile);

		return checkResults(expectedValues, actualValues);

	}

}
