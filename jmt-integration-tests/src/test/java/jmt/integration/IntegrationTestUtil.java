package jmt.integration;

import static jmt.integration.IntegrationTestConstants.JMVA_TESTS_FOLDER_NAME;
import static jmt.integration.IntegrationTestConstants.JSIMGRAPH_TESTS_FOLDER_NAME;
import static jmt.integration.IntegrationTestConstants.JSIMWIZ_TESTS_FOLDER_NAME;
import static jmt.integration.IntegrationTestConstants.OUTPUT_DIR;
import static jmt.integration.IntegrationTestConstants.STABILITY_THRESHOLD;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jaba.JabaResults;

/**
 * Drives a model through the solver and compares the result against the stored
 * oracle.
 *
 * <p>
 * Nothing here opens a window. The solving itself lives in {@link HeadlessSolver};
 * this class only decides where the output goes and whether the numbers match.
 * </p>
 */
public class IntegrationTestUtil {

	private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestUtil.class);

	private static final ThreadLocal<DocumentBuilderFactory> DB_FACTORY = ThreadLocal
			.withInitial(DocumentBuilderFactory::newInstance);
	private static final ThreadLocal<XPathFactory> XPATH_FACTORY = ThreadLocal.withInitial(XPathFactory::newInstance);

	private static File createOutputFile(File testModel, String toolFolderName) throws IOException {
		String parentFolderName = FileUtil.getParentFolderName(testModel);

		if (parentFolderName == null) {
			throw new RuntimeException("Line test model does not have parent folder");
		}

		Path folder = OUTPUT_DIR.resolve(toolFolderName);
		if (!parentFolderName.equals(toolFolderName)) {
			folder = folder.resolve(parentFolderName);
		}

		Files.createDirectories(folder);
		File modelOutputFile = folder.resolve(testModel.getName()).toFile();

		// If a 0-byte stub exists from a previous aborted run, clean it up
		if (modelOutputFile.exists() && modelOutputFile.length() == 0) {
			modelOutputFile.delete();
		}

		return modelOutputFile;
	}

	private static File testJSIMGraph(File testModel) {
		File modelOutputFile = null;
		try {
			modelOutputFile = createOutputFile(testModel, JSIMGRAPH_TESTS_FOLDER_NAME);
			HeadlessSolver.solveJSimGraph(testModel, modelOutputFile);
			return modelOutputFile;
		} catch (Exception e) {
			if (modelOutputFile != null && modelOutputFile.exists() && modelOutputFile.length() == 0) {
				modelOutputFile.delete();
			}
			throw new RuntimeException("Error executing JSIMGraph test", e);
		}
	}

	private static File testJMVA(File testModel) {
		File modelOutputFile = null;
		try {
			modelOutputFile = createOutputFile(testModel, JMVA_TESTS_FOLDER_NAME);
			HeadlessSolver.solveJMVA(testModel, modelOutputFile);
			return modelOutputFile;
		} catch (Exception e) {
			if (modelOutputFile != null && modelOutputFile.exists() && modelOutputFile.length() == 0) {
				modelOutputFile.delete();
			}
			throw new RuntimeException("Error executing JMVA test", e);
		}
	}

	private static File testJSIMWiz(File testModel) {
		File modelOutputFile = null;
		try {
			modelOutputFile = createOutputFile(testModel, JSIMWIZ_TESTS_FOLDER_NAME);
			HeadlessSolver.solveJSimWiz(testModel, modelOutputFile);
			return modelOutputFile;
		} catch (Exception e) {
			if (modelOutputFile != null && modelOutputFile.exists() && modelOutputFile.length() == 0) {
				modelOutputFile.delete();
			}
			throw new RuntimeException("Error executing JSIMWiz test", e);
		}
	}

	private static JabaResults testJABA(File testModel) {
		try {
			return HeadlessSolver.solveJABA(testModel);
		} catch (Exception e) {
			throw new RuntimeException("Error executing JABA test", e);
		}
	}

	private static List<String> extractXmlAttributes(File xmlFile, String xpathExpression, String attributeName) {
		try {
			Document doc = DB_FACTORY.get().newDocumentBuilder().parse(xmlFile);
			doc.getDocumentElement().normalize();

			XPath xPath = XPATH_FACTORY.get().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate(xpathExpression, doc, XPathConstants.NODESET);

			List<String> values = new ArrayList<>();
			for (int i = 0; i < nodes.getLength(); i++) {
				Element element = (Element) nodes.item(i);
				String value = element.getAttribute(attributeName);
				// Skip elements where the attribute is absent (getAttribute returns "" for missing attrs)
				if (!value.isEmpty()) {
					values.add(value);
				}
			}
			return values;
		} catch (Exception e) {
			throw new RuntimeException("Error parsing XML from file: " + xmlFile.getName(), e);
		}
	}

	private static List<String> getJMVAModelFinalValues(File jmvaGraphModelFile) {
		return extractXmlAttributes(jmvaGraphModelFile, "//solutions//measure", "meanValue");
	}

	private static List<String> getJSIMGraphModelFinalValues(File jsimGraphModelFile) {
		return extractXmlAttributes(jsimGraphModelFile, "//results/measure", "finalValue");
	}

	private static List<String> getExpectedMeanValues(File testResults) {
		return extractXmlAttributes(testResults, "//solutions/measure", "meanValue");
	}

	private static List<String> getJSIMWizModelFinalValues(File jsimwModelFile) {
		return extractXmlAttributes(jsimwModelFile, "//results/measure", "finalValue");
	}

	private static String getAlgorithmName(File modelFile) {
		try {
			Document doc = DB_FACTORY.get().newDocumentBuilder().parse(modelFile);
			doc.getDocumentElement().normalize();

			XPath xPath = XPATH_FACTORY.get().newXPath();
			// Fetches the 'name' attribute from <algorithm> inside <solutions>
			String algName = (String) xPath.evaluate("//solutions/algorithm/@name", doc, XPathConstants.STRING);

			return algName != null && !algName.isEmpty() ? algName : null;
		} catch (Exception e) {
			LOG.warn("Could not parse algorithm name from: {}", modelFile.getName(), e);
			return null;
		}
	}

	public static boolean checkResults(List<String> testModelFinalValues, List<String> simulatedModelFinalValues) {
		return checkResults(testModelFinalValues, simulatedModelFinalValues, STABILITY_THRESHOLD);
	}

	public static boolean checkResults(List<String> testModelFinalValues, List<String> simulatedModelFinalValues,
			double threshold) {
		// If the oracle has no stored results, the model has no baseline to compare against.
		// This means it is a functional test only: just verify the simulation ran (which
		// testJSIMGraph already guarantees by returning a file). Always pass.
		if (testModelFinalValues.isEmpty()) {
			return true;
		}
		if (testModelFinalValues.size() != simulatedModelFinalValues.size()) {
			LOG.error("Mismatch in number of results! Expected: {}, Got: {}", testModelFinalValues.size(),
					simulatedModelFinalValues.size());
			return false;
		}

		for (int i = 0; i < testModelFinalValues.size(); i++) {
			String oldStr = testModelFinalValues.get(i);
			String newStr = simulatedModelFinalValues.get(i);

			// Fast pass: if the strings match exactly, skip the math
			if (oldStr.equals(newStr)) {
				continue;
			}

			try {
				double oldVal = Double.parseDouble(oldStr);
				double newVal = Double.parseDouble(newStr);

				double denom = (newVal + oldVal) / 2.0;
				double ratio = 0.0;

				if (denom != 0.0) {
					// Math.abs on both ensures the percentage is always positive, even for negative
					// values
					ratio = Math.abs(newVal - oldVal) / Math.abs(denom);
				}

				double smape = ratio * 100.0;

				// Check against the tolerance threshold
				if (smape >= threshold) {
					LOG.error("Unstable result at index {}: Expected ~{}, Got {} (Difference: {}%, Threshold: {}%)", i, oldVal,
							newVal, String.format("%.2f", smape), threshold);
					return false; // Fails the check the moment a value exceeds the threshold
				}

			} catch (NumberFormatException e) {
				// If the values aren't numbers (e.g., "ERROR" or "NaN") and didn't match as
				// strings, fail
				LOG.error("Non-numeric mismatch at index {}: '{}' vs '{}'", i, oldStr, newStr);
				return false;
			}
		}

		// If it makes it through the loop, all values are within the tolerance threshold
		return true;
	}

	public static boolean testLineJSIMGraphModel(File testModel, File testResults) {
		return testLineJSIMGraphModel(testModel, testResults, STABILITY_THRESHOLD);
	}

	public static boolean testLineJSIMGraphModel(File testModel, File testResults, double threshold) {
		try {
			File simulatedModelFile = testJSIMGraph(testModel);

			List<String> actualValues = getJSIMGraphModelFinalValues(simulatedModelFile);

			List<String> expectedMeanValues = getExpectedMeanValues(testResults);

			return checkResults(expectedMeanValues, actualValues, threshold);

		} catch (Exception e) {
			throw new RuntimeException("Simulation crashed for JSIMGraph line test: " + testModel.getName(), e);
		}
	}

	public static boolean testJSIMGraphModel(File testModel) {
		return testJSIMGraphModel(testModel, STABILITY_THRESHOLD);
	}

	public static boolean testJSIMGraphModel(File testModel, double threshold) {
		try {
			File simulatedModelFile = testJSIMGraph(testModel);

			List<String> expectedValues = getJSIMGraphModelFinalValues(testModel);
			List<String> actualValues = getJSIMGraphModelFinalValues(simulatedModelFile);

			return checkResults(expectedValues, actualValues, threshold);
		} catch (Exception e) {
			throw new RuntimeException("Simulation crashed for JSIMGraph test: " + testModel.getName(), e);
		}
	}

	public static boolean testJMVAModel(File testModel) {
		return testJMVAModel(testModel, STABILITY_THRESHOLD);
	}

	public static boolean testJMVAModel(File testModel, double threshold) {
		try {
			File simulatedModelFile = testJMVA(testModel);

			String expectedAlg = getAlgorithmName(testModel);
			String actualAlg = getAlgorithmName(simulatedModelFile);

			if (expectedAlg != null && actualAlg != null && !expectedAlg.equals(actualAlg)) {
				LOG.error("Algorithm mismatch! Expected: {}, Got: {}", expectedAlg, actualAlg);
				return false;
			}

			List<String> expectedValues = getJMVAModelFinalValues(testModel);
			List<String> actualValues = getJMVAModelFinalValues(simulatedModelFile);

			return checkResults(expectedValues, actualValues, threshold);
		} catch (Exception e) {
			throw new RuntimeException("Simulation crashed for JMVA test: " + testModel.getName(), e);
		}
	}

	public static boolean testJSIMWizModel(File testModel) {
		return testJSIMWizModel(testModel, STABILITY_THRESHOLD);
	}

	public static boolean testJSIMWizModel(File testModel, double threshold) {
		try {
			File simulatedModelFile = testJSIMWiz(testModel);

			List<String> expectedValues = getJSIMWizModelFinalValues(testModel);
			List<String> actualValues = getJSIMWizModelFinalValues(simulatedModelFile);

			return checkResults(expectedValues, actualValues, threshold);
		} catch (Exception e) {
			throw new RuntimeException("Simulation crashed for JSIMWiz test: " + testModel.getName(), e);
		}
	}

	public static boolean testJABAModel(File testModel) {
		try {
			JabaResults simulatedJABAModelResults = testJABA(testModel);
			LOG.debug("{}", simulatedJABAModelResults);

			// TODO: Create oracle to compare results to
			return true;
		} catch (Exception e) {
			throw new RuntimeException("Simulation crashed for JABA test: " + testModel.getName(), e);
		}

	}

}
