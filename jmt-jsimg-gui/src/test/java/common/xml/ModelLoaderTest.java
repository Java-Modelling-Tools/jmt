package common.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import analytical.ExactModel;
import common.CommonConstants;
import common.definitions.CommonModel;
import common.definitions.MeasureDefinition;
import common.definitions.ServerType;
import common.definitions.SimulationDefinition;
import common.definitions.StoredResultsModel;
import jaba.JabaModel;
import jmt.framework.xml.XMLUtils;
import jsimgraph.definitions.JSimGraphModel;
import legacyxml.LegacyXmlExporter;

/**
 * Unit tests of ModelLoader on the paths that open no dialog (file != null, parent == null), and of the FileFormat constants.
 * Dialog paths (file == null, wrong extension on save, newer-version prompt) are not exercised.
 */
class ModelLoaderTest {

	@TempDir
	File tmp;

	private static final String JMVA_EXAMPLE = "jmva/1cl_1stat_mm1.jmva";
	private static final String JABA_EXAMPLE = "jaba/2cl_3stat_1commonsat.jaba";

	// --- helpers ---------------------------------------------------------------------------------------

	private static ModelLoader sim() {
		return new ModelLoader(ModelLoader.JSIM, ModelLoader.JSIM_SAVE, new HeadlessModelFileView());
	}

	private static ModelLoader graph() {
		return new ModelLoader(ModelLoader.JMODEL, ModelLoader.JMODEL_SAVE, new HeadlessModelFileView());
	}

	private static ModelLoader mva() {
		return new ModelLoader(ModelLoader.JMVA, ModelLoader.JMVA_SAVE, new HeadlessModelFileView());
	}

	private static ModelLoader jaba() {
		return new ModelLoader(ModelLoader.JABA, ModelLoader.JABA_SAVE, new HeadlessModelFileView());
	}

	private File write(String name, String content) throws Exception {
		File f = new File(tmp, name);
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	private static File example(String rel) {
		File f = new File(ArchiveFixtures.examplesDir(), rel);
		assertTrue(f.isFile(), "missing example " + f);
		return f;
	}

	private File plainSimFile(String name) {
		File f = new File(tmp, name);
		XMLWriter.writeXML(f, ArchiveFixtures.simpleModel());
		return f;
	}

	private static org.w3c.dom.Document jmvaDoc() {
		try {
			return new XMLUtils().loadXML(example(JMVA_EXAMPLE));
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

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
			r.add(m.getClassName(k));
		}
		return r;
	}

	/** Engine-output style results file (root "solutions"). */
	private File solutionsFile() throws Exception {
		return write("out.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<solutions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"SIMmodeloutput.xsd\""
				+ " solutionMethod=\"simulation\" modelDefinitionPath=\"m.jsimg\" logDecimalSeparator=\".\" logDelimiter=\";\">"
				+ "<measure station=\"Q1\" class=\"C1\" type=\"" + SimulationDefinition.MEASURE_QL + "\" successful=\"true\" meanValue=\"1.5\""
				+ " lowerLimit=\"1.0\" upperLimit=\"2.0\" analyzedSamples=\"100\" discardedSamples=\"3\" alfa=\"0.01\" precision=\"0.03\""
				+ " maxSamples=\"1000\" nodeType=\"station\"/></solutions>");
	}

	/** GUI-style results file (root "results"). */
	private File guiResultsFile() {
		StoredResultsModel r = new StoredResultsModel();
		r.addMeasure("S_C_Utilization", "S", "C", 0.01, 0.03, 1000, 5, MeasureDefinition.MEASURE_SUCCESS, SimulationDefinition.MEASURE_U, "station", null);
		r.addMeasureSample("S_C_Utilization", 0.4, 10.0, 0.5, 0.6, 0.4);
		File f = new File(tmp, "res.xml");
		XMLResultsWriter.writeXML(f, r);
		return f;
	}

	private static final String PNML_MIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<pnml xmlns=\"http://www.pnml.org/version-2009/grammar/pnml\"><net id=\"n1\" type=\"http://www.pnml.org/version-2009/grammar/pnmlcoremodel\">"
			+ "<page id=\"pg\"><place id=\"p1\"><name><text>P1</text></name></place><transition id=\"t1\"><name><text>T1</text></name></transition>"
			+ "<arc id=\"a1\" source=\"p1\" target=\"t1\"/></page></net></pnml>";

	/** One class, one place, one transition, connected: exportable to PNML without JSimGraphModel positions. */
	private static CommonModel petriModel() {
		CommonModel m = new CommonModel();
		m.addClass("C", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new common.distributions.Exponential());
		Object p = m.addStation("P1", CommonConstants.STATION_TYPE_PLACE, 1, new ArrayList<ServerType>());
		Object t = m.addStation("T1", CommonConstants.STATION_TYPE_TRANSITION, 1, new ArrayList<ServerType>());
		m.setConnected(p, t, true);
		return m;
	}

	// --- FileFormat constants (filter behaviour is covered by FileFormatTest) ----------------------------

	@Test
	void modelLoaderSourceHasNoSwingImports() throws Exception {
		String src = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/common/xml/ModelLoader.java")), "UTF-8");
		assertFalse(src.matches("(?s).*import\\s+(java\\.awt|javax\\.swing)[.;].*"));
	}

	@Test
	void staticFiltersHaveTheDocumentedExtensions() {
		assertEquals(Arrays.asList(".jsimg", ".jsimw", ".jmva", ".jaba", ".jsim", ".jmodel", ".pnml"), Arrays.asList(ModelLoader.ALL.getExtensions()));
		assertEquals(Arrays.asList(".jsimg", ".jsimw", ".jmva", ".jsim", ".jmodel", ".pnml"), Arrays.asList(ModelLoader.ALL_NOTJABA.getExtensions()));
		assertEquals(Arrays.asList(".jsimg", ".jmodel", ".pnml"), Arrays.asList(ModelLoader.JMODEL.getExtensions()));
		assertEquals(Arrays.asList(".jsimw", ".jsim", ".pnml"), Arrays.asList(ModelLoader.JSIM.getExtensions()));
		assertEquals(Arrays.asList(".jsimg", ".pnml"), Arrays.asList(ModelLoader.JMODEL_SAVE.getExtensions()));
		assertEquals(Arrays.asList(".jsimw", ".pnml"), Arrays.asList(ModelLoader.JSIM_SAVE.getExtensions()));
		assertEquals(Arrays.asList(".jmva"), Arrays.asList(ModelLoader.JMVA.getExtensions()));
		assertEquals(Arrays.asList(".jmva"), Arrays.asList(ModelLoader.JMVA_SAVE.getExtensions()));
		assertEquals(Arrays.asList(".jaba"), Arrays.asList(ModelLoader.JABA.getExtensions()));
		assertEquals(Arrays.asList(".jaba"), Arrays.asList(ModelLoader.JABA_SAVE.getExtensions()));
		assertEquals("JMVA data file (*.jmva)", ModelLoader.JMVA.getDescription());
		assertTrue(ModelLoader.ALL.accepts("x.PNML"));
		assertFalse(ModelLoader.ALL_NOTJABA.accepts("x.jaba"));
		assertTrue(ModelLoader.ALL.accepts("x.jaba"));
	}

	// --- loadModel: JSIMwiz / JSIMgraph filters --------------------------------------------------------

	@Test
	void loadsArchiveIntoJSimGraphModel() {
		JSimGraphModel src = GuiXmlFixtures.fullModel();
		File f = new File(tmp, "m.jsimg");
		XMLArchiver.saveModel(f, src);
		ModelLoader ml = graph();
		JSimGraphModel dst = GuiXmlFixtures.empty();
		assertEquals(ModelLoader.SUCCESS, ml.loadModel(dst, null, f));
		assertEquals(CommonConstants.JSIM, ml.getInputFileFormat());
		assertTrue(ml.getLastWarnings().isEmpty());
		assertEquals(stationNames(src), stationNames(dst));
		assertEquals(classNames(src), classNames(dst));
		assertEquals(GuiXmlFixtures.snapshot(src), GuiXmlFixtures.snapshot(dst));
		assertEquals(f, ml.getSelectedFile());
	}

	@Test
	void loadsPlainSimFileAsSimengineFormat() {
		File f = plainSimFile("m.jsimw");
		ModelLoader ml = sim();
		CommonModel dst = new CommonModel();
		assertEquals(ModelLoader.SUCCESS, ml.loadModel(dst, null, f));
		assertEquals(CommonConstants.SIMENGINE, ml.getInputFileFormat());
		assertEquals(stationNames(ArchiveFixtures.simpleModel()), stationNames(dst));
		assertEquals(classNames(ArchiveFixtures.simpleModel()), classNames(dst));
	}

	@Test
	void formatIsSniffedFromContentNotExtension() throws Exception {
		File f = plainSimFile("m.jsimg");
		ModelLoader ml = graph();
		assertEquals(ModelLoader.SUCCESS, ml.loadModel(new CommonModel(), null, f));
		assertEquals(CommonConstants.SIMENGINE, ml.getInputFileFormat());
	}

	@Test
	void jmvaFileLoadedInSimFilterIsConvertedAndFormatIsJmva() {
		ModelLoader ml = sim();
		CommonModel dst = new CommonModel();
		int r = ml.loadModel(dst, null, example(JMVA_EXAMPLE));
		assertTrue(r == ModelLoader.SUCCESS || r == ModelLoader.WARNING, "status " + r);
		assertEquals(CommonConstants.JMVA, ml.getInputFileFormat());
		assertEquals(r == ModelLoader.WARNING, !ml.getLastWarnings().isEmpty());
		ExactModel src = new ExactModel();
		assertTrue(src.loadDocument(jmvaDoc()));
		assertEquals(src.getClasses(), dst.getClassKeys().size());
		assertTrue(dst.getStationKeys().size() >= src.getStations());
	}

	@Test
	void jabaFileInSimFilterFailsWithConversionMotivation() throws Exception {
		ModelLoader ml = sim();
		assertEquals(ModelLoader.FAILURE, ml.loadModel(new CommonModel(), null, example(JABA_EXAMPLE)));
		assertTrue(ml.getFailureMotivation().startsWith(ModelLoader_FAIL_CONVERSION()));
		assertTrue(ml.getFailureMotivation().endsWith("JABA."));
		assertEquals(CommonConstants.JABA, ml.getInputFileFormat());
	}

	private static String ModelLoader_FAIL_CONVERSION() {
		return "Input file is recognized but cannot be converted to work within this application. Please open it with ";
	}

	@Test
	void engineResultsFileLoadsAsResultsOnlyWithWarning() throws Exception {
		ModelLoader ml = sim();
		CommonModel dst = new CommonModel();
		assertEquals(ModelLoader.WARNING, ml.loadModel(dst, null, solutionsFile()));
		assertEquals(CommonConstants.SIMENGINE, ml.getInputFileFormat());
		assertEquals(1, ml.getLastWarnings().size());
		assertTrue(ml.getLastWarnings().get(0).contains("simulation results only"));
		assertNotNull(dst.getSimulationResults());
		assertEquals(1, dst.getSimulationResults().getMeasureNumber());
		assertEquals("Q1", dst.getSimulationResults().getStationName(0));
	}

	@Test
	void guiResultsFileLoadsAsResultsOnlyWithWarning() {
		ModelLoader ml = graph();
		CommonModel dst = new CommonModel();
		assertEquals(ModelLoader.WARNING, ml.loadModel(dst, null, guiResultsFile()));
		assertEquals(CommonConstants.SIMENGINE, ml.getInputFileFormat());
		assertEquals(1, ml.getLastWarnings().size());
		assertEquals(1, dst.getSimulationResults().getMeasureNumber());
		assertEquals("S", dst.getSimulationResults().getStationName(0));
	}

	@Test
	void warningsAreClearedByTheNextLoad() throws Exception {
		ModelLoader ml = sim();
		assertEquals(ModelLoader.WARNING, ml.loadModel(new CommonModel(), null, solutionsFile()));
		assertFalse(ml.getLastWarnings().isEmpty());
		assertEquals(ModelLoader.SUCCESS, ml.loadModel(new CommonModel(), null, plainSimFile("m.jsimw")));
		assertTrue(ml.getLastWarnings().isEmpty());
	}

	@Disabled("latent: JDK 11+ only (works on Java 8, which README/CONTRIBUTING name as the supported runtime): PNMLReader -> axiom needs javax.activation.DataSource, absent from the JDK since 11, from every pom and from the singlejar; ModelLoader catches Exception only so NoClassDefFoundError escapes")
	@Test
	void pnmlImportsPlacesAndTransitions() throws Exception {
		ModelLoader ml = sim();
		CommonModel dst = new CommonModel();
		int r = ml.loadModel(dst, null, write("net.pnml", PNML_MIN));
		assertEquals(ModelLoader.SUCCESS, r, ml.getFailureMotivation());
		assertEquals(CommonConstants.JSIM, ml.getInputFileFormat());
		assertEquals(Arrays.asList("P1:" + CommonConstants.STATION_TYPE_PLACE, "T1:" + CommonConstants.STATION_TYPE_TRANSITION), stationNames(dst));
	}

	@Test
	void unknownGarbageEmptyAndMissingFilesFailAsUnknownFormat() throws Exception {
		File[] files = { write("a.xml", "<foo/>"), write("b.jsimg", "this is not xml"), write("c.jsimg", ""), new File(tmp, "missing.jsimg") };
		ModelLoader[] loaders = { sim(), graph(), mva(), jaba() };
		Object[] models = { new CommonModel(), new CommonModel(), new ExactModel(), new JabaModel() };
		for (int i = 0; i < loaders.length; i++) {
			for (File f : files) {
				assertEquals(ModelLoader.FAILURE, loaders[i].loadModel(models[i], null, f), loaders[i].toString() + " " + f.getName());
				assertEquals("Unknown input file format", loaders[i].getFailureMotivation());
			}
		}
	}

	/** A loader that sniffs every file as an archive, so the archive load itself is what fails. */
	private static ModelLoader archiveSniffing(FileFormat filter) {
		return new ModelLoader(filter, ModelLoader.JMODEL_SAVE, new HeadlessModelFileView()) {
			@Override
			protected int getXmlFileType(String fileName) {
				return XML_ARCHIVE;
			}
		};
	}

	@Test
	void unreadableArchiveFailsInJsimAndJmvaLoaders() throws Exception {
		File[] files = { new File(tmp, "gone.jsimg"), write("empty.jsimg", ""), write("bad.jsimg", "<archive><sim>") };
		for (File f : files) {
			ModelLoader jsim = archiveSniffing(ModelLoader.JMODEL);
			CommonModel m = new CommonModel();
			assertEquals(ModelLoader.FAILURE, jsim.loadModel(m, null, f), f.getName());
			assertTrue(jsim.getFailureMotivation().contains(f.getName()), jsim.getFailureMotivation());
			assertEquals(0, m.getStationKeys().size());
			ModelLoader jmva = archiveSniffing(ModelLoader.JMVA);
			assertEquals(ModelLoader.FAILURE, jmva.loadModel(new ExactModel(), null, f), f.getName());
			assertTrue(jmva.getFailureMotivation().contains(f.getName()), jmva.getFailureMotivation());
		}
	}

	@Test
	void wrongModelTypeIsReportedAsFailure() {
		ModelLoader ml = sim();
		assertEquals(ModelLoader.FAILURE, ml.loadModel("not a model", null, plainSimFile("m.jsimw")));
		assertNotNull(ml.getFailureMotivation());
	}

	// --- loadModel: JMVA filter ------------------------------------------------------------------------

	@Test
	void loadsJmvaExampleIntoExactModel() {
		ModelLoader ml = mva();
		ExactModel dst = new ExactModel();
		assertEquals(ModelLoader.SUCCESS, ml.loadModel(dst, null, example(JMVA_EXAMPLE)));
		assertEquals(CommonConstants.JMVA, ml.getInputFileFormat());
		ExactModel ref = new ExactModel();
		assertTrue(ref.loadDocument(jmvaDoc()));
		assertEquals(ref.getStations(), dst.getStations());
		assertArrayEquals(ref.getClassNames(), dst.getClassNames());
		assertArrayEquals(ref.getStationNames(), dst.getStationNames());
	}

	@Test
	void jmvaFilterConvertsSimAndArchiveFiles() {
		ModelLoader ml = mva();
		ExactModel dst = new ExactModel();
		int r = ml.loadModel(dst, null, plainSimFile("m.jsimw"));
		assertTrue(r == ModelLoader.SUCCESS || r == ModelLoader.WARNING, "status " + r + " " + ml.getFailureMotivation());
		assertEquals(CommonConstants.SIMENGINE, ml.getInputFileFormat());
		assertEquals(1, dst.getClasses());

		File arch = new File(tmp, "m.jsimg");
		XMLArchiver.saveModel(arch, ArchiveFixtures.simpleModel());
		ExactModel dst2 = new ExactModel();
		r = ml.loadModel(dst2, null, arch);
		assertTrue(r == ModelLoader.SUCCESS || r == ModelLoader.WARNING, "status " + r + " " + ml.getFailureMotivation());
		assertEquals(CommonConstants.JSIM, ml.getInputFileFormat());
		assertEquals(1, dst2.getClasses());
	}

	@Test
	void jmvaFilterRejectsJabaAndResults() throws Exception {
		ModelLoader ml = mva();
		assertEquals(ModelLoader.FAILURE, ml.loadModel(new ExactModel(), null, example(JABA_EXAMPLE)));
		assertTrue(ml.getFailureMotivation().endsWith("JABA."));
		assertEquals(CommonConstants.JABA, ml.getInputFileFormat());

		assertEquals(ModelLoader.FAILURE, ml.loadModel(new ExactModel(), null, solutionsFile()));
		assertTrue(ml.getFailureMotivation().endsWith("JSIM or JMODEL."));
		assertEquals(CommonConstants.SIMENGINE, ml.getInputFileFormat());

		assertEquals(ModelLoader.FAILURE, ml.loadModel(new ExactModel(), null, guiResultsFile()));
		assertTrue(ml.getFailureMotivation().endsWith("JSIM or JMODEL."));
	}

	@Test
	void jmvaFilterOnPnmlIsUnknownFormat() throws Exception {
		ModelLoader ml = mva();
		assertEquals(ModelLoader.FAILURE, ml.loadModel(new ExactModel(), null, write("net.pnml", PNML_MIN)));
		assertEquals("Unknown input file format", ml.getFailureMotivation());
	}

	// --- loadModel: JABA filter ------------------------------------------------------------------------

	@Test
	void loadsJabaExampleIntoJabaModel() {
		ModelLoader ml = jaba();
		JabaModel dst = new JabaModel();
		assertEquals(ModelLoader.SUCCESS, ml.loadModel(dst, null, example(JABA_EXAMPLE)));
		assertEquals(2, dst.getClasses());
		assertEquals(3, dst.getStations());
	}

	@Test
	void jabaFilterRejectsOtherFormatsWithConversionMotivation() throws Exception {
		ModelLoader ml = jaba();
		JabaModel m = new JabaModel();
		assertEquals(ModelLoader.FAILURE, ml.loadModel(m, null, plainSimFile("m.jsimw")));
		assertTrue(ml.getFailureMotivation().endsWith("JSIM or JMODEL."));
		File arch = new File(tmp, "m.jsimg");
		XMLArchiver.saveModel(arch, ArchiveFixtures.simpleModel());
		assertEquals(ModelLoader.FAILURE, ml.loadModel(m, null, arch));
		assertTrue(ml.getFailureMotivation().endsWith("JSIM or JMODEL."));
		assertEquals(ModelLoader.FAILURE, ml.loadModel(m, null, example(JMVA_EXAMPLE)));
		assertTrue(ml.getFailureMotivation().endsWith("JMVA."));
		assertEquals(ModelLoader.FAILURE, ml.loadModel(m, null, solutionsFile()));
		assertTrue(ml.getFailureMotivation().endsWith("JSIM or JMODEL."));
		assertEquals(ModelLoader.FAILURE, ml.loadModel(m, null, guiResultsFile()));
		assertTrue(ml.getFailureMotivation().endsWith("JSIM or JMODEL."));
	}

	// --- saveModel -------------------------------------------------------------------------------------

	@Test
	void savingAnArchiveToAnUnwritablePathFailsWithAMotivation() {
		ModelLoader ml = graph();
		File f = new File(new File(tmp, "no/such/dir"), "m.jsimg");
		assertEquals(ModelLoader.FAILURE, ml.saveModel(GuiXmlFixtures.fullModel(), null, f));
		assertNotNull(ml.getFailureMotivation());
		assertFalse(f.exists());
	}

	@Test
	void savesArchiveWithLegacyCopyAndItReloads() {
		JSimGraphModel src = GuiXmlFixtures.fullModel();
		File f = new File(tmp, "m.jsimg");
		ModelLoader ml = graph();
		assertEquals(ModelLoader.SUCCESS, ml.saveModel(src, null, f));
		assertTrue(f.isFile());
		File legacy = LegacyXmlExporter.legacyFileFor(f);
		assertEquals("m.legacy.jsimg", legacy.getName());
		assertTrue(legacy.isFile());

		JSimGraphModel back = GuiXmlFixtures.empty();
		assertEquals(ModelLoader.SUCCESS, graph().loadModel(back, null, f));
		assertEquals(GuiXmlFixtures.snapshot(src), GuiXmlFixtures.snapshot(back));
		assertEquals(stationNames(src), stationNames(back));

		CommonModel fromLegacy = new CommonModel();
		assertEquals(ModelLoader.SUCCESS, graph().loadModel(fromLegacy, null, legacy));
		assertEquals(stationNames(src), stationNames(fromLegacy));
		assertEquals(classNames(src), classNames(fromLegacy));
	}

	@Test
	// Writing an <archive> through the .jsimw filter is current behaviour, not a documented contract.
	void jsimwExtensionIsAcceptedAndReloadsAsJsim() throws Exception {
		File f = new File(tmp, "m.jsimw");
		ModelLoader ml = sim();
		assertEquals(ModelLoader.SUCCESS, ml.saveModel(ArchiveFixtures.simpleModel(), null, f));
		CommonModel back = new CommonModel();
		ModelLoader rl = sim();
		assertEquals(ModelLoader.SUCCESS, rl.loadModel(back, null, f));
		assertEquals(CommonConstants.JSIM, rl.getInputFileFormat());
		assertEquals(stationNames(ArchiveFixtures.simpleModel()), stationNames(back));
	}

	@Test
	void savesJmvaAndReloadsEqualState() {
		ExactModel src = new ExactModel();
		assertTrue(src.loadDocument(jmvaDoc()));
		File f = new File(tmp, "m.jmva");
		assertEquals(ModelLoader.SUCCESS, mva().saveModel(src, null, f));
		assertTrue(f.length() > 0);
		ExactModel back = new ExactModel();
		assertEquals(ModelLoader.SUCCESS, mva().loadModel(back, null, f));
		assertNull(ArchiveFixtures.firstDiff(src.createDocument(), back.createDocument(), ""));
	}

	@Test
	void savesJabaAndReloadsEqualState() {
		JabaModel src = new JabaModel();
		assertEquals(ModelLoader.SUCCESS, jaba().loadModel(src, null, example(JABA_EXAMPLE)));
		File f = new File(tmp, "m.jaba");
		assertEquals(ModelLoader.SUCCESS, jaba().saveModel(src, null, f));
		JabaModel back = new JabaModel();
		assertEquals(ModelLoader.SUCCESS, jaba().loadModel(back, null, f));
		assertEquals(src.getClasses(), back.getClasses());
		assertArrayEquals(src.getStationNames(), back.getStationNames());
		assertArrayEquals(src.getClassNames(), back.getClassNames());
		assertNull(ArchiveFixtures.firstDiff(src.createDocument(), back.createDocument(), ""));
	}

	@Test
	void exportsPnmlWithPlacesAndTransitions() throws Exception {
		File f = new File(tmp, "net.pnml");
		ModelLoader ml = sim();
		assertEquals(ModelLoader.SUCCESS, ml.saveModel(petriModel(), null, f), ml.getFailureMotivation());
		org.w3c.dom.Document d = XMLReader.loadXML(f.getAbsolutePath());
		assertEquals("pnml", d.getDocumentElement().getNodeName());
		assertEquals(1, d.getElementsByTagName("place").getLength());
		assertEquals(1, d.getElementsByTagName("transition").getLength());
		String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
		assertTrue(text.contains("P1") && text.contains("T1"));
	}

	@Disabled("latent: JDK 11+ only (works on Java 8, which README/CONTRIBUTING name as the supported runtime): PNMLReader -> axiom needs javax.activation.DataSource, absent from the JDK since 11, from every pom and from the singlejar; ModelLoader catches Exception only so NoClassDefFoundError escapes")
	@Test
	void exportsPnmlAndReimportsIt() {
		CommonModel src = petriModel();
		File f = new File(tmp, "net.pnml");
		ModelLoader ml = sim();
		assertEquals(ModelLoader.SUCCESS, ml.saveModel(src, null, f), ml.getFailureMotivation());
		assertTrue(f.length() > 0);
		CommonModel back = new CommonModel();
		ModelLoader rl = sim();
		int r = rl.loadModel(back, null, f);
		assertEquals(ModelLoader.SUCCESS, r, rl.getFailureMotivation());
		assertEquals(stationNames(src), stationNames(back));
	}

	@Test
	void pnmlExportRefusesMultipleClasses() {
		CommonModel m = petriModel();
		m.addClass("D", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new common.distributions.Exponential());
		File f = new File(tmp, "net.pnml");
		ModelLoader ml = sim();
		assertEquals(ModelLoader.FAILURE, ml.saveModel(m, null, f));
		assertEquals("The model cannot be exported to a PNML file: multiple classes detected.", ml.getFailureMotivation());
		assertFalse(f.exists());
	}

	@Test
	void pnmlExportRefusesNonPetriNetStations() {
		CommonModel m = ArchiveFixtures.simpleModel();
		File f = new File(tmp, "net.pnml");
		ModelLoader ml = sim();
		assertEquals(ModelLoader.FAILURE, ml.saveModel(m, null, f));
		assertEquals("The model cannot be exported to a PNML file: non Petri net stations detected.", ml.getFailureMotivation());
		assertFalse(f.exists());
	}

	@Test
	void saveToUnwritablePathWritesNothing() {
		File f = new File(new File(tmp, "no/such/dir"), "m.jmva");
		ExactModel src = new ExactModel();
		assertTrue(src.loadDocument(jmvaDoc()));
		assertEquals(ModelLoader.FAILURE, mva().saveModel(src, null, f));
		assertFalse(f.exists());
	}

	@Test
	void saveWithWrongModelTypeIsReportedAsFailure() {
		ModelLoader ml = mva();
		assertEquals(ModelLoader.FAILURE, ml.saveModel("nope", null, new File(tmp, "m.jmva")));
		assertNotNull(ml.getFailureMotivation());
	}

	@Test
	void getSelectedFileFollowsTheLastLoadedFile() {
		ModelLoader ml = sim();
		File f = plainSimFile("m.jsimw");
		ml.loadModel(new CommonModel(), null, f);
		assertEquals(f, ml.getSelectedFile());
	}
}
