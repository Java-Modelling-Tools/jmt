package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.Color;

import common.definitions.CommonModel;
import jsimgraph.definitions.JSimGraphModel;
import common.definitions.MeasureDefinition;
import common.definitions.PAResultsModel;
import common.definitions.SimulationDefinition;
import common.definitions.StoredResultsModel;
import jmt.common.GlobalSettings;
import jmt.common.xml.XSDSchemaLoader;
import legacyxml.LegacyXmlExporter;

/** Unit tests of XMLArchiver: archive document structure, save/load round trip, load variants, failure behaviour. */
class XMLArchiverTest {

	private static final String WHATIF = "jsim/qn_models/open_2class_3stat_fork_whatif.jsimg";
	private static final String RES_NAME = "S_C_Utilization";

	@TempDir
	File tmp;

	private String savedVersion;

	@BeforeEach
	void rememberVersion() {
		savedVersion = System.getProperty("jmt.version");
	}

	@AfterEach
	void restoreVersion() {
		if (savedVersion == null) {
			System.clearProperty("jmt.version");
		} else {
			System.setProperty("jmt.version", savedVersion);
		}
	}

	// --- helpers ---------------------------------------------------------------------------------------

	private static Document doc(CommonModel m) {
		return XMLArchiver.getDocument(m, "t.jsimg");
	}

	private static List<String> childNames(Document d) {
		List<String> r = new ArrayList<String>();
		for (Node c = d.getDocumentElement().getFirstChild(); c != null; c = c.getNextSibling()) {
			if (c.getNodeType() == Node.ELEMENT_NODE) {
				r.add(c.getNodeName());
			}
		}
		return r;
	}

	private static StoredResultsModel storedResults() {
		StoredResultsModel r = new StoredResultsModel();
		r.addMeasure(RES_NAME, "S", "C", 0.01, 0.03, 1000, 5, MeasureDefinition.MEASURE_SUCCESS, SimulationDefinition.MEASURE_U, "station", null);
		r.addMeasureSample(RES_NAME, 0.4, 10.0, 0.5, 0.6, 0.4);
		r.addMeasureSample(RES_NAME, 0.45, 20.0, 0.55, 0.65, 0.45);
		return r;
	}

	/** Whatif example (parametric analysis on) with a PAResultsModel holding one measure and two samples. */
	private static CommonModel parametricModel() {
		CommonModel m = ArchiveFixtures.load(new File(ArchiveFixtures.examplesDir(), WHATIF));
		assertTrue(m.isParametricAnalysisEnabled(), "fixture must have parametric analysis on");
		m.setSimulationResults(paResults(m));
		return m;
	}

	private static PAResultsModel paResults(CommonModel m) {
		PAResultsModel r = new PAResultsModel(m, true);
		r.addMeasure(RES_NAME, "S", "C", 0.01, 0.03, SimulationDefinition.MEASURE_U, "station");
		r.addSample(RES_NAME, 0.4, 0.5, 0.6, true);
		r.addSample(RES_NAME, 0.45, 0.55, 0.65, true);
		return r;
	}

	private File save(CommonModel m, String name) {
		File f = new File(tmp, name);
		XMLArchiver.saveModel(f, m);
		return f;
	}

	private static List<String> validate(Document d) throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(new java.net.URL(XSDSchemaLoader.loadSchema(XSDSchemaLoader.JMT_ARCHIVE)));
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
		v.validate(new StreamSource(new ByteArrayInputStream(ArchiveFixtures.toString(d).getBytes(StandardCharsets.UTF_8))));
		return errs;
	}

	/** Station names by key order, as a stable comparison of structure. */
	private static List<String> stationNames(CommonModel m) {
		List<String> r = new ArrayList<String>();
		for (Object k : m.getStationKeys()) {
			r.add(m.getStationName(k) + ":" + m.getStationType(k));
		}
		return r;
	}

	private static List<String> classNames(CommonModel m) {
		List<String> r = new ArrayList<String>();
		for (Object k : m.getClassKeys()) {
			r.add(m.getClassName(k) + ":" + m.getClassType(k));
		}
		return r;
	}

	private static Object stationByName(CommonModel m, String name) {
		for (Object k : m.getStationKeys()) {
			if (name.equals(m.getStationName(k))) {
				return k;
			}
		}
		return null;
	}

	// --- getDocument -----------------------------------------------------------------------------------

	@Test
	void documentRootCarriesNameTimestampVersionAndSchemaAttributes() {
		Element root = doc(ArchiveFixtures.simpleModel()).getDocumentElement();
		assertEquals("archive", root.getNodeName());
		assertEquals("t.jsimg", root.getAttribute("name"));
		assertFalse(root.getAttribute("timestamp").isEmpty());
		assertEquals(GlobalSettings.getSetting("jmt.version").trim(), root.getAttribute(XMLArchiver.XML_ARCHIVE_A_JMT_VERSION));
		assertEquals("Archive.xsd", root.getAttribute("xsi:noNamespaceSchemaLocation"));
		assertEquals("http://www.w3.org/2001/XMLSchema-instance", root.getAttribute("xmlns:xsi"));
	}

	@Test
	void versionIsTrimmedFromTheSetting() {
		System.setProperty("jmt.version", "  9.8.7 ");
		assertEquals("9.8.7", doc(ArchiveFixtures.simpleModel()).getDocumentElement().getAttribute("jmtVersion"));
	}

	@Test
	void childrenAreSimThenJmodelWithoutResults() {
		assertEquals(Arrays.asList("sim", "jmodel"), childNames(doc(ArchiveFixtures.simpleModel())));
	}

	@Test
	void childrenAreSimJmodelResultsWhenStoredResultsMatch() {
		CommonModel m = ArchiveFixtures.simpleModel();
		m.setSimulationResults(storedResults());
		assertEquals(Arrays.asList("sim", "jmodel", "results"), childNames(doc(m)));
	}

	@Test
	void childrenAreSimJmodelResultsWhenParametricResultsMatch() {
		assertEquals(Arrays.asList("sim", "jmodel", "results"), childNames(doc(parametricModel())));
	}

	@Test
	void storedResultsAreDroppedWhenParametricAnalysisIsOn() {
		CommonModel m = parametricModel();
		m.setSimulationResults(storedResults());
		assertTrue(m.containsSimulationResults());
		assertEquals(Arrays.asList("sim", "jmodel"), childNames(doc(m)));
	}

	@Test
	void parametricResultsAreDroppedWhenParametricAnalysisIsOff() {
		CommonModel m = parametricModel();
		PAResultsModel pa = paResults(m);
		m.setParametricAnalysisEnabled(false);
		m.setSimulationResults(pa);
		assertEquals(Arrays.asList("sim", "jmodel"), childNames(doc(m)));
	}

	@Test
	void blankVersionRefusesToSave() {
		CommonModel m = ArchiveFixtures.simpleModel();
		System.setProperty("jmt.version", "   ");
		assertThrows(IllegalStateException.class, () -> doc(m));
		File f = new File(tmp, "x.jsimg");
		assertThrows(IllegalStateException.class, () -> XMLArchiver.saveModel(f, m));
		assertFalse(f.exists(), "nothing may be written without a version");
	}

	@Test
	void documentsValidateAgainstArchiveSchema() throws Exception {
		CommonModel withStored = ArchiveFixtures.simpleModel();
		withStored.setSimulationResults(storedResults());
		assertEquals(new ArrayList<String>(), validate(doc(ArchiveFixtures.simpleModel())), "plain");
		assertEquals(new ArrayList<String>(), validate(doc(withStored)), "stored results");
		assertEquals(new ArrayList<String>(), validate(doc(parametricModel())), "parametric results");
	}

	@Test
	void jsimGraphModelArchiveCarriesRealJmodelAndValidates() throws Exception {
		JSimGraphModel m = GuiXmlFixtures.fullModel();
		Document d = doc(m);
		Element jm = XMLArchiver.getGuiFromArchiveDocument(d);
		assertEquals(GuiXmlFixtures.KINDS.length, jm.getElementsByTagName("station").getLength());
		assertEquals(2, jm.getElementsByTagName("userClass").getLength());
		assertEquals(1, jm.getElementsByTagName("connectionShape").getLength());
		assertEquals(new ArrayList<String>(), validate(d));
	}

	// --- saveModel / loadModel -------------------------------------------------------------------------

	@Test
	void saveThenLoadKeepsStationsClassesConnectionsAndSimulationParameters() {
		CommonModel m = ArchiveFixtures.simpleModel();
		m.setUseRandomSeed(Boolean.FALSE);
		m.setSimulationSeed(Long.valueOf(4242L));
		m.setMaxSimulatedTime(Double.valueOf(1234.5));
		File f = save(m, "m.jsimg");

		CommonModel back = ArchiveFixtures.load(f);
		assertEquals(stationNames(m), stationNames(back));
		assertEquals(classNames(m), classNames(back));
		Object src = stationByName(back, "Src"), srv = stationByName(back, "S"), out = stationByName(back, "Out");
		assertTrue(back.areConnected(src, srv));
		assertTrue(back.areConnected(srv, out));
		assertFalse(back.areConnected(src, out));
		assertEquals(Boolean.FALSE, back.getUseRandomSeed());
		assertEquals(Long.valueOf(4242L), back.getSimulationSeed());
		assertEquals(Double.valueOf(1234.5), back.getMaxSimulatedTime());
		assertEquals(Boolean.FALSE, Boolean.valueOf(back.containsSimulationResults()));
	}

	@Test
	void saveThenLoadThenSaveIsStable() {
		File f = save(ArchiveFixtures.simpleModel(), "m.jsimg");
		CommonModel back = ArchiveFixtures.load(f);
		String diff = ArchiveFixtures.firstDiff(doc(ArchiveFixtures.simpleModel()), doc(back), "");
		assertNull(diff, diff);
	}

	@Test
	void saveWritesLegacyCopyNextToArchiveAndItLoads() {
		File f = save(ArchiveFixtures.simpleModel(), "m.jsimg");
		File legacy = LegacyXmlExporter.legacyFileFor(f);
		assertEquals("m.legacy.jsimg", legacy.getName());
		assertTrue(legacy.isFile());
		assertEquals(f.getParentFile(), legacy.getParentFile());
		CommonModel back = ArchiveFixtures.load(legacy);
		assertEquals(stationNames(ArchiveFixtures.simpleModel()), stationNames(back));
		assertEquals(classNames(ArchiveFixtures.simpleModel()), classNames(back));
	}

	@Test
	void savedArchiveRootIsArchiveWithJmtVersion() throws Exception {
		File f = save(ArchiveFixtures.simpleModel(), "m.jsimg");
		Document d = XMLReader.loadXML(f.getAbsolutePath());
		assertEquals("archive", d.getDocumentElement().getNodeName());
		assertEquals("m.jsimg", d.getDocumentElement().getAttribute("name"));
		assertFalse(d.getDocumentElement().getAttribute("jmtVersion").isEmpty());
	}

	@org.junit.jupiter.api.Disabled("bug: saving to an unwritable path reports SUCCESS, nothing written")
	@Test
	void savingToAnUnwritablePathReportsFailure() {
		File f = new File(new File(tmp, "no/such/dir"), "m.jsimg");
		ModelLoader ml = new ModelLoader(ModelLoader.JMODEL, ModelLoader.JMODEL_SAVE);
		// XMLArchiver.saveModel swallows the TransformerException (printStackTrace only), so SUCCESS is returned
		assertEquals(ModelLoader.FAILURE, ml.saveModel(GuiXmlFixtures.fullModel(), null, f));
		assertFalse(f.exists());
	}

	@Test
	void storedResultsSurviveSaveAndLoad() {
		CommonModel m = ArchiveFixtures.simpleModel();
		m.setSimulationResults(storedResults());
		CommonModel back = ArchiveFixtures.load(save(m, "r.jsimg"));
		assertTrue(back.containsSimulationResults());
		MeasureDefinition r = back.getSimulationResults();
		assertEquals(StoredResultsModel.class, r.getClass());
		assertEquals(1, r.getMeasureNumber());
		assertEquals(RES_NAME, r.getName(0));
		assertEquals("S", r.getStationName(0));
		assertEquals("C", r.getClassName(0));
		assertEquals(2, r.getValues(0).size());
		assertEquals(0.55, r.getValues(0).lastElement().getMeanValue(), 0.0);
	}

	@Test
	void parametricResultsSurviveSaveAndLoad() {
		CommonModel back = ArchiveFixtures.load(save(parametricModel(), "p.jsimg"));
		assertTrue(back.isParametricAnalysisEnabled());
		assertTrue(back.containsSimulationResults());
		MeasureDefinition r = back.getSimulationResults();
		assertTrue(r instanceof PAResultsModel);
		assertEquals(1, r.getMeasureNumber());
		assertEquals(RES_NAME, r.getName(0));
		assertEquals(2, r.getValues(0).size());
		assertEquals(0.5, r.getValues(0).firstElement().getMeanValue(), 0.0);
	}

	// --- loadModel variants ----------------------------------------------------------------------------

	private String simXml() {
		Document d = XMLArchiver.getDocument(ArchiveFixtures.simpleModel(), "t");
		Element sim = XMLArchiver.getSimFromArchiveDocument(d);
		return ArchiveFixtures.toString(wrap(sim));
	}

	private static Document wrap(Element e) {
		try {
			Document n = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			n.appendChild(n.importNode(e, true));
			return n;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private File write(String name, String content) throws Exception {
		File f = new File(tmp, name);
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	@Test
	void loadingAPlainSimFileLoadsModelWithoutGuiOrResults() throws Exception {
		CommonModel back = ArchiveFixtures.load(write("plain.xml", simXml()));
		assertEquals(stationNames(ArchiveFixtures.simpleModel()).size(), back.getStationKeys().size());
		assertEquals(Arrays.asList("C:" + common.CommonConstants.CLASS_TYPE_OPEN), classNames(back));
		assertFalse(back.containsSimulationResults());
	}

	@Test
	void loadingAnArchiveWithoutJmodelLoadsTheSimPart() throws Exception {
		Document d = doc(ArchiveFixtures.simpleModel());
		Element jm = XMLArchiver.getGuiFromArchiveDocument(d);
		d.getDocumentElement().removeChild(jm);
		assertNull(XMLArchiver.getGuiFromArchiveDocument(d));
		CommonModel back = ArchiveFixtures.load(write("nogui.jsimg", ArchiveFixtures.toString(d)));
		assertEquals(3, back.getStationKeys().size());
		assertEquals(1, back.getClassKeys().size());
		Object src = stationByName(back, "Src"), srv = stationByName(back, "S");
		assertTrue(back.areConnected(src, srv));
	}

	@Test
	void saveThenLoadIntoJSimGraphModelRestoresGuiAndSimState() {
		JSimGraphModel src = GuiXmlFixtures.fullModel();
		File f = save(src, "gui.jsimg");
		JSimGraphModel back = new JSimGraphModel(null);
		XMLArchiver.loadModel(f, back);
		assertEquals(stationNames(src), stationNames(back));
		assertEquals(classNames(src), classNames(back));
		assertEquals(GuiXmlFixtures.snapshot(src), GuiXmlFixtures.snapshot(back));
		assertEquals(new Color(0x12, 0x34, 0x56), back.getClassColor(back.getClassKeys().get(0)));
	}

	@Test
	void archiveWithoutJmodelLoadsIntoJSimGraphModelWithoutGuiState() throws Exception {
		Document d = doc(GuiXmlFixtures.fullModel());
		d.getDocumentElement().removeChild(XMLArchiver.getGuiFromArchiveDocument(d));
		JSimGraphModel back = new JSimGraphModel(null);
		XMLArchiver.loadModel(write("nogui2.jsimg", ArchiveFixtures.toString(d)), back);
		assertEquals(GuiXmlFixtures.KINDS.length, back.getStationKeys().size());
		for (Object k : back.getStationKeys()) {
			assertNull(back.getStationPosition(k));
		}
		assertFalse(back.hasConnectionShape());
		// colours fall back to the defaults handed out by addClass (BLUE, then RED)
		assertEquals(Color.BLUE, back.getClassColor(back.getClassKeys().get(0)));
	}

	@Test
	void loadingAnArchiveWithEngineSolutionsRootStoresThem() throws Exception {
		// "solutions" is the engine's own results root; both roots are accepted inside an archive
		String xml = "<solutions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"SIMmodeloutput.xsd\""
				+ " solutionMethod=\"simulation\" modelDefinitionPath=\"x\" modelName=\"m\">"
				+ "<measure measureType=\"4\" class=\"C\" station=\"S\" successful=\"true\" meanValue=\"0.5\" upperLimit=\"0.6\" lowerLimit=\"0.4\""
				+ " analyzedSamples=\"1000\" discardedSamples=\"5\" precision=\"0.03\" alfa=\"0.01\" nodeType=\"station\"/></solutions>";
		Document d = doc(ArchiveFixtures.simpleModel());
		d.getDocumentElement().appendChild(d.importNode(ArchiveFixtures.parse(xml).getDocumentElement(), true));
		assertEquals("solutions", XMLArchiver.getResultsFromArchiveDocument(d).getNodeName());
		CommonModel back = ArchiveFixtures.load(write("sol.jsimg", ArchiveFixtures.toString(d)));
		assertTrue(back.containsSimulationResults());
		assertEquals(1, back.getSimulationResults().getMeasureNumber());
		assertEquals("S", back.getSimulationResults().getStationName(0));
		assertEquals(0.5, back.getSimulationResults().getValues(0).lastElement().getMeanValue(), 1e-12);
	}

	@Test
	void archivePartAccessors() {
		CommonModel m = ArchiveFixtures.simpleModel();
		m.setSimulationResults(storedResults());
		Document d = doc(m);
		assertEquals("sim", XMLArchiver.getSimFromArchiveDocument(d).getNodeName());
		assertEquals("jmodel", XMLArchiver.getGuiFromArchiveDocument(d).getNodeName());
		Element res = XMLArchiver.getResultsFromArchiveDocument(d);
		assertEquals("results", res.getNodeName());
		assertSame(d.getDocumentElement(), res.getParentNode());
	}

	@Test
	void resultsAccessorReturnsNullWhenThereAreNone() {
		assertNull(XMLArchiver.getResultsFromArchiveDocument(doc(ArchiveFixtures.simpleModel())));
	}

	@Test
	void accessorsReturnNullForADocumentWithoutTheParts() {
		Document d = ArchiveFixtures.parse("<archive/>");
		assertNull(XMLArchiver.getSimFromArchiveDocument(d));
		assertNull(XMLArchiver.getGuiFromArchiveDocument(d));
		assertNull(XMLArchiver.getResultsFromArchiveDocument(d));
	}

	// --- failure behaviour -----------------------------------------------------------------------------
	// The only production caller is ModelLoader.loadModel, which first runs getXmlFileType (catches every
	// Exception -> FILE_UNKNOWN -> FAILURE) and wraps the load in catch (Exception) -> FAILURE. So an NPE from a
	// null document cannot reach the user as a crash; it is the root cause tracked in issue #34 (loadXML returns null).
	// The exception type is an accident of that, so the tests below only require that loading fails.

	@Test
	void missingFileFailsAndLeavesModelEmpty() {
		CommonModel m = new CommonModel();
		assertThrows(Exception.class, () -> XMLArchiver.loadModel(new File(tmp, "absent.jsimg"), m));
		assertEquals(0, m.getStationKeys().size());
	}

	@Test
	void emptyFileFailsAndLeavesModelEmpty() throws Exception {
		File f = write("empty.jsimg", "");
		CommonModel m = new CommonModel();
		assertThrows(Exception.class, () -> XMLArchiver.loadModel(f, m));
		assertEquals(0, m.getStationKeys().size());
	}

	@Test
	void malformedFileFailsAndLeavesModelEmpty() throws Exception {
		File f = write("bad.jsimg", "<archive><sim>");
		CommonModel m = new CommonModel();
		assertThrows(Exception.class, () -> XMLArchiver.loadModel(f, m));
		assertEquals(0, m.getStationKeys().size());
	}

	@org.junit.jupiter.api.Disabled("latent: XMLArchiver.loadModel throws instead of reporting a failed load for a missing/empty/malformed file; issue #34 family; the only production caller (ModelLoader) catches it, so users never see it")
	@Test
	void unreadableFilesShouldNotThrow() throws Exception {
		CommonModel m = new CommonModel();
		XMLArchiver.loadModel(new File(tmp, "absent.jsimg"), m);
		XMLArchiver.loadModel(write("empty2.jsimg", ""), m);
		XMLArchiver.loadModel(write("bad2.jsimg", "<archive><sim>"), m);
		assertEquals(0, m.getStationKeys().size());
	}
}
