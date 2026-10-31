package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import common.definitions.CommonModel;
import jsimgraph.definitions.JSimGraphModel;

/** GuiXMLWriter: document content, schema validity, determinism, file/stream output. */
class GuiXMLWriterTest {

	@TempDir
	File tmp;

	private static Element byAttr(Document d, String tag, String attr, String value) {
		NodeList l = d.getElementsByTagName(tag);
		for (int i = 0; i < l.getLength(); i++) {
			if (value.equals(((Element) l.item(i)).getAttribute(attr))) {
				return (Element) l.item(i);
			}
		}
		return null;
	}

	@Test
	void everyStationKindIsAddedAndWrittenWithItsPosition() throws Exception {
		JSimGraphModel m = GuiXmlFixtures.fullModel();
		assertEquals(GuiXmlFixtures.KINDS.length, m.getStationKeys().size(), "all station kinds must be constructible");
		Document d = GuiXMLWriter.getDocument(m);
		assertEquals("jmodel", d.getDocumentElement().getNodeName());
		assertEquals(GuiXmlFixtures.KINDS.length, d.getElementsByTagName("station").getLength());
		int i = 0;
		for (Object k : m.getStationKeys()) {
			Element st = byAttr(d, "station", "name", m.getStationName(k));
			Element pos = (Element) st.getElementsByTagName("position").item(0);
			assertEquals(String.valueOf(10.5 + 100 * i), pos.getAttribute("x"));
			assertEquals(String.valueOf(20.25 + 7 * i), pos.getAttribute("y"));
			assertEquals(String.valueOf(i % 2 == 1), pos.getAttribute("rotate"));
			assertEquals(String.valueOf(i % 2 == 1 ? 90.0 : 0.0), pos.getAttribute("angle"));
			i++;
		}
	}

	@Test
	void classColoursAreUpperCaseHexWithAlphaPrefix() {
		Document d = GuiXMLWriter.getDocument(GuiXmlFixtures.fullModel());
		assertEquals("#FF123456", byAttr(d, "userClass", "name", "A").getAttribute("color"));
		assertEquals("#FFFEDCBA", byAttr(d, "userClass", "name", "B").getAttribute("color"));
	}

	@Test
	void connectionShapeIsWrittenArcByArc() {
		JSimGraphModel m = GuiXmlFixtures.fullModel();
		Document d = GuiXMLWriter.getDocument(m);
		assertEquals(1, d.getElementsByTagName("connectionShape").getLength());
		Element cs = (Element) d.getElementsByTagName("connectionShape").item(0);
		assertEquals(m.getStationName(m.getStationKeys().get(0)), cs.getAttribute("source"));
		assertEquals(m.getStationName(m.getStationKeys().get(1)), cs.getAttribute("target"));
		NodeList arcs = cs.getElementsByTagName("arc");
		assertEquals(2, arcs.getLength());
		Element a1 = (Element) arcs.item(1);
		Element src = (Element) a1.getElementsByTagName("source").item(0);
		assertEquals("7.0", src.getAttribute("x"));
		assertEquals("8.0", src.getAttribute("y"));
		NodeList pts = a1.getElementsByTagName("point");
		assertEquals("9.5", ((Element) pts.item(0)).getAttribute("x"));
		assertEquals("12.5", ((Element) pts.item(1)).getAttribute("y"));
		Element tgt = (Element) a1.getElementsByTagName("target").item(0);
		assertEquals("13.0", tgt.getAttribute("x"));
	}

	@Test
	void childOrderMatchesSchemaAndDocumentValidates() throws Exception {
		Document d = GuiXMLWriter.getDocument(GuiXmlFixtures.fullModel());
		assertEquals(java.util.Collections.<String>emptyList(), GuiXmlFixtures.validate(d));
		// classes, then stations, then connection shapes
		String order = "";
		for (org.w3c.dom.Node n = d.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling()) {
			String nm = n.getNodeName();
			if (!order.endsWith(nm)) {
				order += (order.isEmpty() ? "" : ",") + nm;
			}
		}
		assertEquals("userClass,station,connectionShape", order);
	}

	@Test
	void sameModelGivesIdenticalDocuments() {
		JSimGraphModel m = GuiXmlFixtures.fullModel();
		assertEquals(ArchiveFixtures.toString(GuiXMLWriter.getDocument(m)), ArchiveFixtures.toString(GuiXMLWriter.getDocument(m)));
	}

	@Test
	void emptyModelGivesEmptyValidRoot() throws Exception {
		Document d = GuiXMLWriter.getDocument(GuiXmlFixtures.empty());
		assertEquals(0, d.getDocumentElement().getChildNodes().getLength());
		assertEquals(java.util.Collections.<String>emptyList(), GuiXmlFixtures.validate(d));
	}

	@Test
	void plainCommonModelGetsOnlyTheRoot() {
		Document d = GuiXMLWriter.getDocument(ArchiveFixtures.simpleModel());
		assertEquals("jmodel", d.getDocumentElement().getNodeName());
		assertEquals(0, d.getDocumentElement().getChildNodes().getLength(), "no GUI state is written for a non-JSimGraphModel");
	}

	@Test
	void parametricAnalysisOffWritesNoParametricElement() {
		Document d = GuiXMLWriter.getDocument(GuiXmlFixtures.fullModel());
		assertEquals(0, d.getElementsByTagName("parametric").getLength());
	}

	@Test
	void parametricAnalysisOnWritesClassPathAndFields() throws Exception {
		CommonModel loaded = ArchiveFixtures.load(new File(ArchiveFixtures.examplesDir(), "jsim/qn_models/open_2class_3stat_fork_whatif.jsimg"));
		assertTrue(loaded.isParametricAnalysisEnabled());
		JSimGraphModel m = new JSimGraphModel(null);
		XMLArchiver.loadModel(new File(ArchiveFixtures.examplesDir(), "jsim/qn_models/open_2class_3stat_fork_whatif.jsimg"), m);
		assertTrue(m.isParametricAnalysisEnabled());
		Document d = GuiXMLWriter.getDocument(m);
		Element p = (Element) d.getElementsByTagName("parametric").item(0);
		assertEquals("true", p.getAttribute("enabled"));
		assertEquals(m.getParametricAnalysisModel().getClass().getName(), p.getAttribute("classPath"));
		assertEquals(m.getParametricAnalysisModel().getProperties().size(), p.getElementsByTagName("field").getLength());
		assertEquals(java.util.Collections.<String>emptyList(), GuiXmlFixtures.validate(d));
	}

	@Test
	void writeXmlToFileAndStreamProduceTheDocument() throws Exception {
		JSimGraphModel m = GuiXmlFixtures.fullModel();
		File byFile = new File(tmp, "a.xml");
		File byName = new File(tmp, "b.xml");
		GuiXMLWriter.writeXML(byFile, m);
		GuiXMLWriter.writeXML(byName.getPath(), m);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GuiXMLWriter.writeXML(out, m);
		String expected = new String(out.toByteArray(), "ISO-8859-1");
		assertEquals(expected, new String(java.nio.file.Files.readAllBytes(byFile.toPath()), "ISO-8859-1"));
		assertEquals(expected, new String(java.nio.file.Files.readAllBytes(byName.toPath()), "ISO-8859-1"));
		assertTrue(expected.contains("<jmodel"));
	}
}
