package linetesting;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jmt.gui.common.controller.DispatcherThread;
import jmt.gui.common.xml.ModelLoader;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;
import jmt.gui.jsimgraph.mainGui.JSIMGraphMain;

public class LineTest {

	@Test
	void testLine() {
		Path resourcePath;
		try {
			resourcePath = Paths.get(
					getClass().getResource("/linetesting/input/tpb80d3b67_44ae_4d6c_8a83_fa3b14eb9397/model.jsim").toURI());

			File resourceFile = resourcePath.toFile();

			assertTrue(resourceFile.exists());

			// Create test instance with overridden checkForSave
			JSIMGraphMain jsimGraphMain = new JSIMGraphMain();

			// Get Mediator
			Field mediatorField = JSIMGraphMain.class.getDeclaredField("mediator");
			mediatorField.setAccessible(true);
			Mediator mediator = (Mediator) mediatorField.get(jsimGraphMain);

			mediator.openModel(resourceFile);

			mediator.startSimulation();

			System.out.println("Simulation truly started");

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

			System.out.println("Simulation truly finished");

			// Get the model file parent folder name
			String parentFolderName = resourcePath.getParent().getFileName().toString();

			String[] folderHierarchy = { "linetesting", "output", parentFolderName };
			File modelOutputFile = FileUtil.createFileInTestResources(folderHierarchy, "testedModel.jsimg",
					"Hierarchical content!");

			assertTrue(modelOutputFile.exists());

			System.out.println("File created at: " + modelOutputFile.getAbsolutePath());

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

			System.out.println("finalValues: " + finalValues);

			// === Parse second file: model.jsim-result.jsim ===
			Path resultsFilePath = Paths.get(getClass()
					.getResource("/linetesting/input/tpb80d3b67_44ae_4d6c_8a83_fa3b14eb9397/model.jsim-result.jsim").toURI());

			File resultsFile = resultsFilePath.toFile();

			assertTrue(resultsFile.exists());

			Document solutionDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resultsFile);
			solutionDoc.getDocumentElement().normalize();

			NodeList solutionNodes = (NodeList) xPath.evaluate("//solutions/measure", solutionDoc, XPathConstants.NODESET);

			for (int i = 0; i < solutionNodes.getLength(); i++) {
				Element solution = (Element) solutionNodes.item(i);
				String meanValue = solution.getAttribute("meanValue");
				String finalValue = finalValues.get(i);
				System.out.println("finalValue: " + finalValue);
				System.out.println("meanValue: " + meanValue);
				assertEquals(finalValue, meanValue);
			}

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}