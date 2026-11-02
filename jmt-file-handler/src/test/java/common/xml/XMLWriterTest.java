package common.xml;

import static common.xml.SimXml.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.definitions.SimulationDefinition;
import common.distributions.Exponential;
import jmt.common.xml.XSDSchemaLoader;

/** XMLWriter on small models built from inline {@code <sim>} documents and mutated through the CommonModel API. */
class XMLWriterTest {

	static final String BASE = sim("", userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"")
			+ source("Src", "C", distr("Exponential", "ExponentialPar", "lambda", "1"))
			+ server("S", "C", "TailStrategy", 2, 7, distr("Exponential", "ExponentialPar", "lambda", "3")) + sink("Out")
			+ connection("Src", "S") + connection("S", "Out"));

	/** Returns validation errors of the serialized document against the sim model schema (empty if valid). */
	static List<String> validate(String xml) throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(new java.net.URL(XSDSchemaLoader.loadSchema(XSDSchemaLoader.JSIM_MODEL_DEFINITION)));
		final List<String> errs = new ArrayList<String>();
		javax.xml.validation.Validator v = schema.newValidator();
		v.setErrorHandler(new org.xml.sax.ErrorHandler() {
			public void warning(org.xml.sax.SAXParseException e) {
			}

			public void error(org.xml.sax.SAXParseException e) {
				errs.add(e.getMessage());
			}

			public void fatalError(org.xml.sax.SAXParseException e) {
				errs.add(e.getMessage());
			}
		});
		v.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
		return errs;
	}

	static void assertValid(CommonModel m) throws Exception {
		String xml = ModelFixtures.writeToString(m);
		assertEquals(new ArrayList<String>(), validate(xml), "schema errors in written model");
	}

	static Element byName(Document d, String tag, String name) {
		NodeList l = d.getElementsByTagName(tag);
		for (int i = 0; i < l.getLength(); i++) {
			if (name.equals(((Element) l.item(i)).getAttribute("name"))) {
				return (Element) l.item(i);
			}
		}
		return null;
	}

	/** Names of the direct element children of the root, in document order. */
	static List<String> rootChildren(Document d) {
		List<String> r = new ArrayList<String>();
		for (org.w3c.dom.Node n = d.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element) {
				r.add(n.getNodeName());
			}
		}
		return r;
	}

	@Test
	void rootElementAndAttributes() {
		CommonModel m = parseSim(BASE);
		m.setUseRandomSeed(Boolean.FALSE);
		m.setSimulationSeed(Long.valueOf(77));
		m.setMaximumDuration(Double.valueOf(250.5));
		m.setMaxSimulationSamples(Integer.valueOf(4321));
		m.setPollingInterval(Double.valueOf(2.0));
		Document d = XMLWriter.getDocument(m, "mymodel");
		Element root = d.getDocumentElement();
		assertEquals("sim", root.getTagName());
		assertEquals("mymodel", root.getAttribute("name"));
		assertEquals("77", root.getAttribute("seed"));
		assertEquals("250.5", root.getAttribute("maxTime"));
		assertEquals("4321", root.getAttribute("maxSamples"));
		assertEquals("2.0", root.getAttribute("polling"));
		assertTrue(root.getAttribute("xsi:noNamespaceSchemaLocation").length() > 0);
	}

	@Test
	void randomSeedAndNoDurationOmitAttributes() {
		CommonModel m = parseSim(BASE);
		m.setUseRandomSeed(Boolean.TRUE);
		Element root = XMLWriter.getDocument(m, "x").getDocumentElement();
		assertEquals("", root.getAttribute("seed"));
		assertEquals("", root.getAttribute("maxTime"));
	}

	@Test
	void topLevelOrderIsClassesStationsMeasuresConnections() {
		CommonModel m = parseSim(BASE);
		m.addMeasure(SimulationDefinition.MEASURE_U, m.getStationByName("S"), m.getClassByName("C"));
		List<String> order = rootChildren(XMLWriter.getDocument(m, "x"));
		int lastClass = order.lastIndexOf("userClass");
		int firstNode = order.indexOf("node");
		int lastNode = order.lastIndexOf("node");
		int firstMeasure = order.indexOf("measure");
		int firstConn = order.indexOf("connection");
		assertTrue(lastClass >= 0 && lastClass < firstNode);
		assertTrue(lastNode < firstMeasure);
		assertTrue(firstMeasure < firstConn);
	}

	@Test
	void classesAreWrittenWithTypeAndReferenceSource() {
		CommonModel m = parseSim(BASE);
		Document d = XMLWriter.getDocument(m, "x");
		Element c = byName(d, "userClass", "C");
		assertNotNull(c);
		assertEquals("open", c.getAttribute("type"));
		assertEquals("0", c.getAttribute("priority"));
		assertEquals("Src", c.getAttribute("referenceSource"));
	}

	@Test
	void addedClassIsWrittenAndModelStaysValid() throws Exception {
		CommonModel m = parseSim(BASE);
		Object k = m.addClass("Extra", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(3), Double.valueOf(0), null, new Exponential());
		m.setClassRefStation(k, m.getStationByName("Src"));
		Document d = XMLWriter.getDocument(m, "x");
		Element e = byName(d, "userClass", "Extra");
		assertNotNull(e);
		assertEquals("3", e.getAttribute("priority"));
		assertEquals("Src", e.getAttribute("referenceSource"));
		assertValid(m);
	}

	@Test
	void stationsAreWrittenWithSectionsInOrder() {
		Document d = XMLWriter.getDocument(parseSim(BASE), "x");
		Element s = byName(d, "node", "S");
		assertEquals(java.util.Arrays.asList("Queue", "Server", "Router"), sectionNames(s));
		assertEquals(java.util.Arrays.asList("RandomSource", "ServiceTunnel", "Router"), sectionNames(byName(d, "node", "Src")));
		assertEquals(java.util.Arrays.asList("JobSink"), sectionNames(byName(d, "node", "Out")));
	}

	private static List<String> sectionNames(Element node) {
		List<String> r = new ArrayList<String>();
		NodeList l = node.getElementsByTagName("section");
		for (int i = 0; i < l.getLength(); i++) {
			if (l.item(i).getParentNode() == node) {
				r.add(((Element) l.item(i)).getAttribute("className"));
			}
		}
		return r;
	}

	@Test
	void serverParametersAreWritten() {
		Document d = XMLWriter.getDocument(parseSim(BASE), "x");
		Element server = null;
		NodeList l = byName(d, "node", "S").getElementsByTagName("section");
		for (int i = 0; i < l.getLength(); i++) {
			if ("Server".equals(((Element) l.item(i)).getAttribute("className"))) {
				server = (Element) l.item(i);
			}
		}
		assertNotNull(server);
		Element maxJobs = firstParam(server, "maxJobs");
		assertEquals("2", maxJobs.getElementsByTagName("value").item(0).getTextContent());
		Element queue = (Element) byName(d, "node", "S").getElementsByTagName("section").item(0);
		assertEquals("7", firstParam(queue, "size").getElementsByTagName("value").item(0).getTextContent());
	}

	private static Element firstParam(Element parent, String name) {
		NodeList l = parent.getElementsByTagName("parameter");
		for (int i = 0; i < l.getLength(); i++) {
			if (name.equals(((Element) l.item(i)).getAttribute("name"))) {
				return (Element) l.item(i);
			}
		}
		return null;
	}

	@Test
	void changedServiceDistributionIsWritten() throws Exception {
		CommonModel m = parseSim(BASE);
		Exponential e = new Exponential();
		e.getParameter("lambda").setValue(Double.valueOf(9.5));
		m.setServiceTimeDistribution(m.getStationByName("S"), m.getClassByName("C"), e);
		String xml = ModelFixtures.writeToString(m);
		assertTrue(xml.contains("9.5"), xml);
		assertEquals(new ArrayList<String>(), validate(xml));
		// reload through the reader: the new value survives
		CommonModel back = parseSim(stripToSim(xml));
		common.distributions.Distribution dd = (common.distributions.Distribution) back
				.getServiceTimeDistribution(back.getStationByName("S"), back.getClassByName("C"));
		assertEquals(9.5, (Double) dd.getParameter("lambda").getValue(), 1e-12);
	}

	private static String stripToSim(String xml) {
		return xml.substring(xml.indexOf("<sim"));
	}

	@Test
	void measuresAreWritten() throws Exception {
		CommonModel m = parseSim(BASE);
		m.addMeasure(SimulationDefinition.MEASURE_U, m.getStationByName("S"), m.getClassByName("C"));
		m.addMeasure(SimulationDefinition.MEASURE_X, m.getStationByName("S"), m.getClassByName("C"), Double.valueOf(0.9),
				Double.valueOf(0.05), true);
		Document d = XMLWriter.getDocument(m, "x");
		NodeList l = d.getElementsByTagName("measure");
		assertEquals(2, l.getLength());
		Element u = (Element) l.item(0);
		assertEquals("Utilization", u.getAttribute("type"));
		assertEquals("S", u.getAttribute("referenceNode"));
		assertEquals("C", u.getAttribute("referenceUserClass"));
		assertEquals("station", u.getAttribute("nodeType"));
		assertEquals("S_C_Utilization", u.getAttribute("name"));
		Element x = (Element) l.item(1);
		assertEquals("0.1", x.getAttribute("alpha"), "alpha is written inverted (1 - confidence)");
		assertEquals("0.05", x.getAttribute("precision"));
		assertEquals("true", x.getAttribute("verbose"));
		assertValid(m);
	}

	@Test
	void connectionsAreWrittenInStationOrder() {
		Document d = XMLWriter.getDocument(parseSim(BASE), "x");
		NodeList l = d.getElementsByTagName("connection");
		assertEquals(2, l.getLength());
		assertEquals("Src", ((Element) l.item(0)).getAttribute("source"));
		assertEquals("S", ((Element) l.item(0)).getAttribute("target"));
		assertEquals("S", ((Element) l.item(1)).getAttribute("source"));
		assertEquals("Out", ((Element) l.item(1)).getAttribute("target"));
	}

	@Test
	void removedConnectionDisappears() {
		CommonModel m = parseSim(BASE);
		m.setConnected(m.getStationByName("S"), m.getStationByName("Out"), false);
		assertEquals(1, XMLWriter.getDocument(m, "x").getElementsByTagName("connection").getLength());
	}

	@Test
	void baseModelIsSchemaValid() throws Exception {
		assertValid(parseSim(BASE));
	}

	@Test
	void writeXMLToFileAndOutputStreamAgree(@TempDir File tmp) throws Exception {
		CommonModel m = parseSim(BASE);
		File f = new File(tmp, "m.xml");
		XMLWriter.writeXML(f, m);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLWriter.writeXML(out, m);
		String a = new String(Files.readAllBytes(f.toPath()), StandardCharsets.ISO_8859_1);
		assertTrue(a.contains("name=\"m.xml\""), "file name is used as model name");
		assertTrue(new String(out.toByteArray(), StandardCharsets.ISO_8859_1).contains("name=\"model\""));
		File g = new File(tmp, "g.xml");
		XMLWriter.writeXML(g.getPath(), m);
		assertTrue(g.length() > 0);
		File h = new File(tmp, "h.xml");
		XMLWriter.writeXML(h.getPath(), XMLWriter.getDocument(m, "doc"));
		assertTrue(h.length() > 0);
		XMLWriter.writeXML(new File(tmp, "none.xml").getPath(), (Document) null);
		assertTrue(!new File(tmp, "none.xml").exists());
	}

	@Test
	void logPathReloadsWithoutTrailingSlashOrFallsBackToWorkdir(@TempDir File tmp) {
		String withSlash = tmp.getAbsolutePath() + File.separator;
		String xml = ModelFixtures.writeToString(parseSim(BASE));
		String existing = parseSim(stripToSim(xml.replaceAll("logPath=\"[^\"]*\"", "logPath=\"" + withSlash + "\""))).getLoggingGlbParameter("path");
		assertEquals(tmp.getAbsolutePath(), existing, "existing dir: File.getAbsolutePath() drops the trailing slash");
		String missing = new File(tmp, "nope").getAbsolutePath();
		String fallback = parseSim(stripToSim(xml.replaceAll("logPath=\"[^\"]*\"", "logPath=\"" + missing + "\""))).getLoggingGlbParameter("path");
		assertTrue(fallback != null && !fallback.isEmpty() && !fallback.equals(missing), "nonexistent dir falls back to the workdir default");
	}

	@Test
	void secondWriteAfterReloadIsStable() {
		CommonModel m = parseSim(BASE);
		m.addMeasure(SimulationDefinition.MEASURE_U, m.getStationByName("S"), m.getClassByName("C"));
		String first = stripName(ModelFixtures.writeToString(m));
		String second = stripName(ModelFixtures.writeToString(parseSim(stripToSim(first))));
		assertEquals(first, second);
	}

	/** logPath loses its trailing slash on reload (oddity, see report); ignore it here. */
	private static String stripName(String s) {
		return s.replaceAll("logPath=\"[^\"]*\"", "");
	}
}
