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

public class TestUtil {

	private static List<String> testJSIMGraph(File testModel) {

		try {

			// Create test instance with overridden checkForSave
			JSIMGraphMain jsimGraphMain = new JSIMGraphMain();

			// Get Mediator
			Field mediatorField = JSIMGraphMain.class.getDeclaredField("mediator");
			mediatorField.setAccessible(true);
			Mediator mediator = (Mediator) mediatorField.get(jsimGraphMain);

			mediator.openModel(testModel);

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

			String[] folderHierarchy = { "linetesting", "output", parentFolderName };
			File modelOutputFile = FileUtil.createFileInTestResources(folderHierarchy, "testedModel.jsimg");

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

			// Closes everything gui (sadly it opens automatically)
			Method doClose = JSIMGraphMain.class.getDeclaredMethod("doClose");
			doClose.setAccessible(true);
			doClose.invoke(jsimGraphMain);
			jsimGraphMain.dispose();

			return finalValues;

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InterruptedException
				| NoSuchFieldException | IOException | XPathExpressionException | SAXException
				| ParserConfigurationException | NoSuchMethodException | InvocationTargetException e) {
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
				System.out.println("finalValue: " + finalValue);
				System.out.println("meanValue: " + meanValue);
				System.out.println("match: " + finalValue.equals(meanValue));
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

}
