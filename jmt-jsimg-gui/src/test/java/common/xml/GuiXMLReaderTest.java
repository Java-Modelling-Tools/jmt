package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import common.CommonConstants;
import common.definitions.CommonModel;
import jsimgraph.definitions.JMTPoint;
import jsimgraph.definitions.JSimGraphModel;

/** GuiXMLReader: applying a GUI document onto a model that already has its classes and stations. */
class GuiXMLReaderTest {

	private static Document doc(String body) {
		return ArchiveFixtures.parse("<jmodel>" + body + "</jmodel>");
	}

	private static String pos(String name, String attrs) {
		return "<station name=\"" + name + "\"><position " + attrs + "/></station>";
	}

	/** Open class "A", stations "Source 1" -> "Queue 1" -> "Sink 1", connected. */
	private static JSimGraphModel threeStations() {
		JSimGraphModel m = GuiXmlFixtures.empty();
		m.addClass("A", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new common.distributions.Exponential());
		Object s = m.addStation(CommonConstants.STATION_TYPE_SOURCE);
		Object q = m.addStation(CommonConstants.STATION_TYPE_SERVER);
		Object k = m.addStation(CommonConstants.STATION_TYPE_SINK);
		m.setConnected(s, q, true);
		m.setConnected(q, k, true);
		return m;
	}

	private static Object station(JSimGraphModel m, String name) {
		for (Object k : m.getStationKeys()) {
			if (m.getStationName(k).equals(name)) {
				return k;
			}
		}
		throw new AssertionError("no station " + name + " in " + m.getStationKeys());
	}

	private static Object cls(JSimGraphModel m, String name) {
		for (Object k : m.getClassKeys()) {
			if (m.getClassName(k).equals(name)) {
				return k;
			}
		}
		throw new AssertionError("no class " + name);
	}

	private static String conn(String src, String tgt, String arcs) {
		return "<connectionShape source=\"" + src + "\" target=\"" + tgt + "\">" + arcs + "</connectionShape>";
	}

	private static final String ARC = "<arc><source x=\"1\" y=\"2\"/><point x=\"3\" y=\"4\"/><point x=\"5\" y=\"6\"/><target x=\"7\" y=\"8\"/></arc>";

	@Test
	void stationPositionRotationAndAngleAreApplied() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc(pos("Queue 1", "x=\"12.5\" y=\"-3\" rotate=\"true\" angle=\"45\"")), m);
		JMTPoint p = m.getStationPosition(station(m, "Queue 1"));
		assertEquals(12.5, p.getX());
		assertEquals(-3.0, p.getY());
		assertTrue(p.isRotate());
		assertEquals(45.0, p.getAngle());
	}

	@Test
	void missingRotateAndAngleDefaultToFalseAndZero() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc(pos("Queue 1", "x=\"1\" y=\"2\"")), m);
		JMTPoint p = m.getStationPosition(station(m, "Queue 1"));
		assertFalse(p.isRotate());
		assertEquals(0.0, p.getAngle());
	}

	@Test
	void stationWithoutGuiInfoHasNoPosition() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc(pos("Queue 1", "x=\"1\" y=\"2\"")), m);
		assertNull(m.getStationPosition(station(m, "Source 1")));
		assertNull(m.getStationPosition(station(m, "Sink 1")));
	}

	@Test
	void unknownStationAndClassNamesAreSkippedWithoutError() {
		JSimGraphModel m = threeStations();
		Object before = m.getClassColor(cls(m, "A"));
		GuiXMLReader.parseXML(doc("<userClass name=\"Ghost\" color=\"#FF0000FF\"/>" + pos("Ghost", "x=\"9\" y=\"9\"")
				+ pos("Queue 1", "x=\"4\" y=\"5\"")), m);
		assertEquals(3, m.getStationKeys().size());
		assertEquals(1, m.getClassKeys().size());
		assertEquals(before, m.getClassColor(cls(m, "A")));
		assertEquals(4.0, m.getStationPosition(station(m, "Queue 1")).getX());
	}

	@Test
	void classColourIsParsedFromHexWithAlpha() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc("<userClass name=\"A\" color=\"#FF123456\"/>"), m);
		assertEquals(new Color(0x12, 0x34, 0x56), m.getClassColor(cls(m, "A")));
	}

	@Test
	void classColourWithoutHashIsStillParsed() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc("<userClass name=\"A\" color=\"FF0000FF\"/>"), m);
		assertEquals(new Color(0x00, 0x00, 0xFF), m.getClassColor(cls(m, "A")));
	}

	@Test
	void unparsableClassColourGetsANewColour() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc("<userClass name=\"A\" color=\"#nothex\"/>"), m);
		// the class got BLUE from addClass; the fallback is the model's next default colour
		assertEquals(Color.RED, m.getClassColor(cls(m, "A")));
	}

	@Test
	void connectionShapeIsAppliedOnlyBetweenConnectedKnownStations() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc(conn("Source 1", "Queue 1", ARC + ARC) + conn("Source 1", "Sink 1", ARC) + conn("Ghost", "Queue 1", ARC)), m);
		Object s = station(m, "Source 1"), q = station(m, "Queue 1"), k = station(m, "Sink 1");
		assertTrue(m.hasConnectionShape(s, q));
		assertEquals(2, m.getConnectionShape(s, q).getArcsNb());
		assertEquals(4.0, m.getConnectionShape(s, q).getArc(0).getArcPoints().get(0).getY());
		assertEquals(7.0, m.getConnectionShape(s, q).getArc(1).getTarget().getX());
		assertFalse(m.hasConnectionShape(s, k), "not connected in the model");
		assertFalse(m.hasConnectionShape(q, s));
	}

	@Test
	void emptyGuiPartLeavesTheModelUntouched() {
		JSimGraphModel m = threeStations();
		Map<String, String> before = GuiXmlFixtures.snapshot(m);
		GuiXMLReader.parseXML(doc(""), m);
		assertEquals(before, GuiXmlFixtures.snapshot(m));
		assertFalse(m.isParametricAnalysisEnabled());
	}

	@Test
	void plainCommonModelIgnoresClassStationAndShapeInfo() {
		CommonModel m = ArchiveFixtures.simpleModel();
		GuiXMLReader.parseXML(doc(pos("S", "x=\"1\" y=\"2\"") + "<userClass name=\"C\" color=\"#FF0000FF\"/>"), m);
		assertFalse(m.isParametricAnalysisEnabled());
	}

	@Test
	void parametricAnalysisDisabledOrAbsentLeavesItOff() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc("<parametric enabled=\"false\" classPath=\"x.Y\"/>"), m);
		assertFalse(m.isParametricAnalysisEnabled());
	}

	@Test
	void parametricAnalysisRestoredFromRealArchive() {
		File f = new File(ArchiveFixtures.examplesDir(), "jsim/qn_models/open_2class_3stat_fork_whatif.jsimg");
		JSimGraphModel m = new JSimGraphModel(null);
		XMLArchiver.loadModel(f, m);
		assertTrue(m.isParametricAnalysisEnabled());
		assertNotNull(m.getParametricAnalysisModel());
	}

	@Test
	void realExamplesGetTheExactPositionsAndColoursWrittenInTheirJmodel() throws Exception {
		java.util.List<File> all = ArchiveFixtures.listArchives();
		for (File f : all.subList(0, 3)) {
			JSimGraphModel m = new JSimGraphModel(null);
			XMLArchiver.loadModel(f, m);
			Document d = ArchiveFixtures.parse(new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8"));
			org.w3c.dom.Element jm = (org.w3c.dom.Element) d.getElementsByTagName("jmodel").item(0);
			org.w3c.dom.NodeList sts = jm.getElementsByTagName("station");
			assertTrue(sts.getLength() > 0, f.getName());
			for (int i = 0; i < sts.getLength(); i++) {
				org.w3c.dom.Element st = (org.w3c.dom.Element) sts.item(i);
				org.w3c.dom.Element pe = (org.w3c.dom.Element) st.getElementsByTagName("position").item(0);
				JMTPoint p = m.getStationPosition(station(m, st.getAttribute("name")));
				assertEquals(Double.parseDouble(pe.getAttribute("x")), p.getX(), f.getName());
				assertEquals(Double.parseDouble(pe.getAttribute("y")), p.getY(), f.getName());
			}
			org.w3c.dom.NodeList cls = jm.getElementsByTagName("userClass");
			for (int i = 0; i < cls.getLength(); i++) {
				org.w3c.dom.Element c = (org.w3c.dom.Element) cls.item(i);
				assertEquals((int) Long.parseLong(c.getAttribute("color").substring(1), 16), m.getClassColor(cls(m, c.getAttribute("name"))).getRGB(), f.getName());
			}
			assertEquals(sts.getLength(), m.getStationKeys().size(), f.getName());
		}
	}

	// ---- failure behaviour (pinned loosely: only that it fails) ----

	@Test
	void stationWithoutPositionElementFails() {
		JSimGraphModel m = threeStations();
		assertThrows(Exception.class, () -> GuiXMLReader.parseXML(doc("<station name=\"Queue 1\"/>"), m));
	}

	@Test
	void nonNumericPositionFails() {
		JSimGraphModel m = threeStations();
		assertThrows(NumberFormatException.class, () -> GuiXMLReader.parseXML(doc(pos("Queue 1", "x=\"abc\" y=\"1\"")), m));
	}

	@Test
	void missingRotateDoesNotInheritFromThePreviousStation() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc(pos("Source 1", "x=\"1\" y=\"2\" rotate=\"true\" angle=\"90\"") + pos("Queue 1", "x=\"3\" y=\"4\"")), m);
		JMTPoint p = m.getStationPosition(station(m, "Queue 1"));
		assertFalse(p.isRotate());
		assertEquals(0.0, p.getAngle());
	}

	@Test
	void connectionShapeWithoutArcsIsStoredAsEmptyPath() {
		JSimGraphModel m = threeStations();
		GuiXMLReader.parseXML(doc(conn("Source 1", "Queue 1", "")), m);
		Object s = station(m, "Source 1"), q = station(m, "Queue 1");
		assertTrue(m.hasConnectionShape(s, q));
		assertEquals(0, m.getConnectionShape(s, q).getArcsNb());
	}

	// ---- round trip ----

	@Test
	void writeThenReadRestoresGuiState() {
		JSimGraphModel src = GuiXmlFixtures.fullModel();
		JSimGraphModel dst = GuiXmlFixtures.bareCopyOf(src);
		assertEquals(src.getStationKeys().size(), dst.getStationKeys().size());
		assertFalse(GuiXmlFixtures.snapshot(src).equals(GuiXmlFixtures.snapshot(dst)), "bare copy must differ before the read");
		GuiXMLReader.parseXML(GuiXMLWriter.getDocument(src), dst);
		assertEquals(GuiXmlFixtures.snapshot(src), GuiXmlFixtures.snapshot(dst));
		assertTrue(dst.hasConnectionShape(dst.getStationKeys().get(0), dst.getStationKeys().get(1)));
	}

	@Test
	void secondWriteAfterRoundTripIsIdentical() {
		JSimGraphModel src = GuiXmlFixtures.fullModel();
		JSimGraphModel dst = GuiXmlFixtures.bareCopyOf(src);
		GuiXMLReader.parseXML(GuiXMLWriter.getDocument(src), dst);
		assertEquals(ArchiveFixtures.toString(GuiXMLWriter.getDocument(src)), ArchiveFixtures.toString(GuiXMLWriter.getDocument(dst)));
	}
}
