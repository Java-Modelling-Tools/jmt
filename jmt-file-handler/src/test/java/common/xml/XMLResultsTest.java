package common.xml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import common.definitions.CommonModel;
import common.definitions.MeasureDefinition;
import common.definitions.PAResultsModel;
import common.definitions.SimulationDefinition;
import common.definitions.StoredResultsModel;
import common.definitions.parametric.SeedParametricAnalysis;
import jmt.framework.gui.graph.MeasureValue;

/** XMLResultsWriter / XMLResultsReader: round trips over StoredResultsModel and PAResultsModel, engine output, malformed input. */
class XMLResultsTest {

	@TempDir
	File tmp;

	private static final String QL = SimulationDefinition.MEASURE_QL;
	private static final String RP = SimulationDefinition.MEASURE_RP;

	private static StoredResultsModel sample() {
		StoredResultsModel m = new StoredResultsModel();
		m.setPollingInterval(2.5);
		m.setElapsedTime(1234L);
		m.setLogDecimalSeparator(",");
		m.setLogCsvDelimiter("|");
		m.addMeasure("Q1_C1_" + QL, "Q1", "C1", 0.99, 0.03, 100, 7, MeasureDefinition.MEASURE_SUCCESS, QL, "station", "q1.csv");
		m.addMeasureSample("Q1_C1_" + QL, 0.5, 10.0, 1.5, 2.0, 1.0);
		m.addMeasureSample("Q1_C1_" + QL, 0.6, 20.0, 1.6, 2.1, 1.1);
		m.addMeasure("Q1_C1_" + RP, "Q1", "C1", 0.95, 0.05, 50, 0, MeasureDefinition.MEASURE_FAILED, RP, "station", null);
		m.addMeasureSample("Q1_C1_" + RP, 0, 5.0, 3.0, 0, 0);
		return m;
	}

	private static Document toDoc(byte[] xml) throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml));
	}

	private static byte[] bytes(MeasureDefinition m) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLResultsWriter.writeXML(out, m);
		return out.toByteArray();
	}

	private static MeasureValue first(MeasureDefinition m, int i) {
		return m.getValues(i).get(0);
	}

	// ---- writer ----

	@Test
	void getDocumentHasRootAttributesAndMeasures() {
		Document d = XMLResultsWriter.getDocument(sample());
		Element root = d.getDocumentElement();
		assertEquals("results", root.getNodeName());
		assertEquals("2.5", root.getAttribute("pollingInterval"));
		assertEquals("1234", root.getAttribute("elapsedTime"));
		assertEquals(",", root.getAttribute("logDecimalSeparator"));
		assertEquals("|", root.getAttribute("logDelimiter"));
		NodeList ms = root.getElementsByTagName("measure");
		assertEquals(2, ms.getLength());
		Element m0 = (Element) ms.item(0);
		assertEquals("Q1_C1_" + QL, m0.getAttribute("name"));
		assertEquals("q1.csv", m0.getAttribute("logFile"));
		assertEquals("Q1", m0.getAttribute("referenceStation"));
		assertEquals("100", m0.getAttribute("analyzedSamples"));
		assertEquals("7", m0.getAttribute("discardedSamples"));
		assertEquals("1.6", m0.getAttribute("finalValue"));
		assertEquals(2, m0.getElementsByTagName("sample").getLength());
		Element s0 = (Element) m0.getElementsByTagName("sample").item(0);
		assertEquals("1.5", s0.getAttribute("meanValue"));
		assertEquals("2.0", s0.getAttribute("upperBound"));
		assertEquals("1.0", s0.getAttribute("lowerBound"));
		assertEquals("10.0", s0.getAttribute("simulationTime"));
		Element m1 = (Element) ms.item(1);
		assertTrue(!m1.hasAttribute("logFile"));
		// zero bounds are omitted
		Element s1 = (Element) m1.getElementsByTagName("sample").item(0);
		assertTrue(!s1.hasAttribute("upperBound") && !s1.hasAttribute("lowerBound"));
	}

	@Test
	void writeToFileAndStringPathsProduceSameContent() throws Exception {
		StoredResultsModel m = sample();
		File a = new File(tmp, "a.xml");
		File b = new File(tmp, "b.xml");
		XMLResultsWriter.writeXML(a, m);
		XMLResultsWriter.writeXML(b.getAbsolutePath(), m);
		assertTrue(a.length() > 0);
		assertEquals(new String(Files.readAllBytes(a.toPath()), StandardCharsets.ISO_8859_1),
				new String(Files.readAllBytes(b.toPath()), StandardCharsets.ISO_8859_1));
		assertEquals(2, toDoc(Files.readAllBytes(a.toPath())).getElementsByTagName("measure").getLength());
	}

	@Test
	void emptyDefinitionWritesRootOnly() throws Exception {
		Document d = toDoc(bytes(new StoredResultsModel()));
		assertEquals("results", d.getDocumentElement().getNodeName());
		assertEquals(0, d.getElementsByTagName("measure").getLength());
	}

	@Test
	@Disabled("bug: XMLResultsWriter throws NoSuchElementException (values.lastElement()) for a measure that has no samples, although state NO_SAMPLES is a valid state")
	void measureWithoutSamplesIsWritten() {
		StoredResultsModel m = new StoredResultsModel();
		m.addMeasure("Q1_C1_" + QL, "Q1", "C1", 0.99, 0.03, 0, 0, MeasureDefinition.MEASURE_NO_SAMPLES, QL, "station", null);
		assertDoesNotThrow(() -> XMLResultsWriter.getDocument(m));
	}

	// ---- round trip, stored model ----

	@Test
	void storedModelRoundTripsThroughOutputStream() throws Exception {
		StoredResultsModel src = sample();
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.parseXML(toDoc(bytes(src)), dst);

		assertEquals(2.5, dst.getPollingInterval());
		assertEquals(1234L, dst.getElapsedTime());
		assertEquals(",", dst.getLogDecimalSeparator());
		assertEquals("|", dst.getLogCsvDelimiter());
		assertEquals(2, dst.getMeasureNumber());
		assertEquals("Q1_C1_" + QL, dst.getName(0));
		assertEquals("Q1", dst.getStationName(0));
		assertEquals("C1", dst.getClassName(0));
		assertEquals(0.99, dst.getAlpha(0));
		assertEquals(0.03, dst.getPrecision(0));
		assertEquals(100, dst.getAnalyzedSamples(0));
		assertEquals(7, dst.getDiscardedSamples(0));
		assertEquals(MeasureDefinition.MEASURE_SUCCESS, dst.getMeasureState(0));
		assertEquals(QL, dst.getMeasureType(0));
		assertEquals("station", dst.getNodeType(0));
		assertEquals("q1.csv", dst.getLogFileName(0));
		assertEquals(2, dst.getValues(0).size());
		MeasureValue v = first(dst, 0);
		assertEquals(1.5, v.getMeanValue());
		assertEquals(2.0, v.getUpperBound());
		assertEquals(1.0, v.getLowerBound());
		assertEquals(0.5, v.getLastIntervalAvgValue());
		assertEquals(10.0, v.getSimTime());
		assertEquals(1.6, dst.getValues(0).get(1).getMeanValue());

		assertNull(dst.getLogFileName(1));
		assertEquals(MeasureDefinition.MEASURE_FAILED, dst.getMeasureState(1));
		assertEquals(RP, dst.getMeasureType(1));
		assertEquals(0.0, first(dst, 1).getUpperBound());
		assertEquals(0.0, first(dst, 1).getLowerBound());
		assertEquals(1, dst.getQueueLengthMeasures().length);
		assertEquals(1, dst.getResponseTimeMeasures().length);
	}

	@Test
	void emptyDefinitionRoundTripUsesRootDefaults() throws Exception {
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.parseXML(toDoc(bytes(new StoredResultsModel())), dst);
		assertEquals(0, dst.getMeasureNumber());
	}

	@Test
	void rootDefaultsAppliedWhenOptionalAttributesAreEmpty() throws Exception {
		String xml = "<results pollingInterval=\"3\" elapsedTime=\"\" logDecimalSeparator=\"\" logDelimiter=\"\">"
				+ "<measure name=\"Q_C_" + QL + "\" referenceStation=\"Q\" referenceClass=\"C\" analyzedSamples=\"4\" state=\"1\""
				+ " alpha=\"0.9\" precision=\"0.1\" nodeType=\"station\"><sample meanValue=\"2\"/></measure></results>";
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.parseXML(toDoc(xml.getBytes(StandardCharsets.UTF_8)), dst);
		assertEquals(3.0, dst.getPollingInterval());
		assertEquals(0L, dst.getElapsedTime());
		assertEquals(".", dst.getLogDecimalSeparator());
		assertEquals(";", dst.getLogCsvDelimiter());
		assertEquals(0, dst.getDiscardedSamples(0));
		assertNull(dst.getLogFileName(0));
		MeasureValue v = first(dst, 0);
		assertEquals(2.0, v.getMeanValue());
		assertEquals(0.0, v.getUpperBound());
		assertEquals(0.0, v.getLowerBound());
		assertEquals(0.0, v.getSimTime());
	}

	@Test
	void unknownRootIsIgnored() throws Exception {
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.parseXML(toDoc("<other><measure name=\"x\"/></other>".getBytes(StandardCharsets.UTF_8)), dst);
		assertEquals(0, dst.getMeasureNumber());
		PAResultsModel pa = newPa();
		XMLResultsReader.parseXML(toDoc("<other/>".getBytes(StandardCharsets.UTF_8)), pa);
		assertEquals(0, pa.getMeasureNumber());
	}

	@Test
	void malformedNumbersThrowNumberFormatException() throws Exception {
		String xml = "<results pollingInterval=\"abc\"/>";
		Document d = toDoc(xml.getBytes(StandardCharsets.UTF_8));
		assertThrows(NumberFormatException.class, () -> XMLResultsReader.parseXML(d, new StoredResultsModel()));
		Document missing = toDoc(("<results pollingInterval=\"1\"><measure name=\"Q_C_" + QL + "\"/></results>").getBytes(StandardCharsets.UTF_8));
		assertThrows(NumberFormatException.class, () -> XMLResultsReader.parseXML(missing, new StoredResultsModel()));
	}

	// ---- loadModel / loadGuiModel ----

	@Test
	void loadGuiModelReadsWriterOutputFile() throws IOException {
		File f = new File(tmp, "res.jsimg-res.xml");
		XMLResultsWriter.writeXML(f, sample());
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.loadGuiModel(dst, f);
		assertEquals(2, dst.getMeasureNumber());
		assertEquals(1.5, first(dst, 0).getMeanValue());
		assertEquals(1234L, dst.getElapsedTime());
	}

	@Test
	void loadModelReadsEngineOutput() throws IOException {
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
				+ "<solutions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"SIMmodeloutput.xsd\""
				+ " solutionMethod=\"simulation\" modelDefinitionPath=\"m.jsimg\" logDecimalSeparator=\".\" logDelimiter=\";\">"
				+ "<measure station=\"Q1\" class=\"C1\" type=\"" + QL + "\" successful=\"true\" meanValue=\"1.5\" lowerLimit=\"1.0\""
				+ " upperLimit=\"2.0\" analyzedSamples=\"100\" discardedSamples=\"3\" alfa=\"0.01\" precision=\"0.03\" maxSamples=\"1000\" nodeType=\"station\"/>"
				+ "<measure type=\"" + RP + "\" successful=\"false\" analyzedSamples=\"0\" alfa=\"0.05\" precision=\"0.03\" maxSamples=\"1000\"/>"
				+ "</solutions>";
		File f = new File(tmp, "out.xml");
		Files.write(f.toPath(), xml.getBytes(StandardCharsets.ISO_8859_1));
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.loadModel(dst, f);
		assertEquals(2, dst.getMeasureNumber());
		assertEquals("Q1_C1_" + QL, dst.getName(0));
		assertEquals("Q1", dst.getStationName(0));
		assertEquals(0.99, dst.getAlpha(0), 1e-12);
		assertEquals(100, dst.getAnalyzedSamples(0));
		assertEquals(3, dst.getDiscardedSamples(0));
		assertEquals(MeasureDefinition.MEASURE_SUCCESS, dst.getMeasureState(0));
		assertEquals(2.0, first(dst, 0).getUpperBound());
		assertEquals(1.0, first(dst, 0).getLowerBound());
		// missing station/class default to the whole network
		assertEquals("Network_All classes_" + RP, dst.getName(1));
		assertEquals(MeasureDefinition.MEASURE_NO_SAMPLES, dst.getMeasureState(1));
	}

	@Test
	void engineOutputWithSuccessfulFlagMapsToStates() throws Exception {
		String xml = "<solutions><measure station=\"S\" class=\"C\" type=\"" + QL + "\" successful=\"false\" analyzedSamples=\"5\""
				+ " alfa=\"0.01\" precision=\"0.1\"/></solutions>";
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.parseXML(toDoc(xml.getBytes(StandardCharsets.UTF_8)), dst);
		assertEquals(MeasureDefinition.MEASURE_FAILED, dst.getMeasureState(0));
	}

	@Test
	@Disabled("bug: XMLResultsReader.loadGuiModel throws NPE (XMLReader.loadXML returns null) on a missing/malformed file instead of leaving the model empty")
	void loadGuiModelOnMalformedFileLeavesModelEmpty() throws IOException {
		File f = new File(tmp, "bad.xml");
		Files.write(f.toPath(), "<results".getBytes(StandardCharsets.UTF_8));
		StoredResultsModel dst = new StoredResultsModel();
		assertDoesNotThrow(() -> XMLResultsReader.loadGuiModel(dst, f));
		assertEquals(0, dst.getMeasureNumber());
	}

	@Test
	@Disabled("bug: engine-output path (setEngineMeasures) sets lastIntervalAvgValue and simulationTime from the meanValue attribute, which carries neither")
	void engineOutputSampleTimeAndLastIntervalAreNotTheMean() throws Exception {
		String xml = "<solutions><measure station=\"S\" class=\"C\" type=\"" + QL + "\" successful=\"true\" meanValue=\"1.5\""
				+ " analyzedSamples=\"5\" alfa=\"0.01\" precision=\"0.1\"/></solutions>";
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.parseXML(toDoc(xml.getBytes(StandardCharsets.UTF_8)), dst);
		assertEquals(1.5, first(dst, 0).getMeanValue());
		assertEquals(0.0, first(dst, 0).getSimTime());
		assertEquals(0.0, first(dst, 0).getLastIntervalAvgValue());
	}

	@Test
	void writerOmitsNonPositiveAndInfiniteBoundsForStoredModels() {
		StoredResultsModel m = new StoredResultsModel();
		m.addMeasure("Q_C_" + QL, "Q", "C", 0.9, 0.1, 1, 0, MeasureDefinition.MEASURE_SUCCESS, QL, "station", null);
		m.addMeasureSample("Q_C_" + QL, 0, 0, 1.0, Double.POSITIVE_INFINITY, -1.0);
		Element s = (Element) XMLResultsWriter.getDocument(m).getElementsByTagName("sample").item(0);
		assertTrue(!s.hasAttribute("upperBound") && !s.hasAttribute("lowerBound"));
		StoredResultsModel dst = new StoredResultsModel();
		XMLResultsReader.parseXML(XMLResultsWriter.getDocument(m), dst);
		assertEquals(0.0, first(dst, 0).getUpperBound());
		assertEquals(0.0, first(dst, 0).getLowerBound());
	}

	// ---- parametric analysis model ----

	/** PAResultsModel(model, true) only needs a non-null parametric definition (the model itself serves as its own definitions). */
	private static PAResultsModel newPa() {
		CommonModel cm = new CommonModel();
		cm.setParametricAnalysisModel(new SeedParametricAnalysis(cm, cm, cm));
		return new PAResultsModel(cm, true);
	}

	private static PAResultsModel pa() {
		PAResultsModel m = newPa();
		m.setLogDecimalSeparator(".");
		m.setLogCsvDelimiter(";");
		m.addMeasure("Q1_C1_" + QL, "Q1", "C1", 0.99, 0.03, QL, "station");
		m.addSample("Q1_C1_" + QL, 1.0, 1.5, 2.0, true);
		m.addSample("Q1_C1_" + QL, Double.POSITIVE_INFINITY, 3.0, Double.POSITIVE_INFINITY, false);
		return m;
	}

	@Test
	void parametricModelRoundTripsIncludingInfinitiesAndValidity() throws Exception {
		PAResultsModel src = pa();
		Document d = XMLResultsWriter.getDocument(src);
		Element m0 = (Element) d.getElementsByTagName("measure").item(0);
		assertTrue(!m0.hasAttribute("finalValue"));
		assertEquals("Infinity", ((Element) m0.getElementsByTagName("sample").item(1)).getAttribute("upperBound"));

		PAResultsModel dst = newPa();
		XMLResultsReader.parseXML(toDoc(bytes(src)), dst);
		assertEquals(1, dst.getMeasureNumber());
		assertEquals("Q1_C1_" + QL, dst.getName(0));
		assertEquals("Q1", dst.getStationName(0));
		assertEquals("C1", dst.getClassName(0));
		assertEquals(0.99, dst.getAlpha(0));
		assertEquals(0.03, dst.getPrecision(0));
		assertEquals(QL, dst.getMeasureType(0));
		assertEquals("station", dst.getNodeType(0));
		assertEquals(2, dst.getValues(0).size());
		assertEquals(1.5, dst.getValues(0).get(0).getMeanValue());
		assertEquals(1.0, dst.getValues(0).get(0).getLowerBound());
		assertEquals(2.0, dst.getValues(0).get(0).getUpperBound());
		assertTrue(dst.getMeasureState(0, 0));
		assertTrue(!dst.getMeasureState(0, 1));
		assertEquals(Double.POSITIVE_INFINITY, dst.getValues(0).get(1).getUpperBound());
		assertEquals(Double.POSITIVE_INFINITY, dst.getValues(0).get(1).getLowerBound());
		assertEquals(".", dst.getLogDecimalSeparator());
		assertEquals(";", dst.getLogCsvDelimiter());
	}

	@Test
	void parametricModelWithoutMeasuresRoundTrips() throws Exception {
		PAResultsModel dst = newPa();
		PAResultsModel empty = newPa();
		empty.setLogDecimalSeparator(".");
		empty.setLogCsvDelimiter(";");
		XMLResultsReader.parseXML(toDoc(bytes(empty)), dst);
		assertEquals(0, dst.getMeasureNumber());
		assertNotNull(dst.getLogCsvDelimiter());
	}
}
