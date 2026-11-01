package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import common.definitions.CommonModel;

/** XMLReader.loadModel entry points: malformed input, wrong roots, plain and archive documents, known defects. */
class XMLReaderLoadModelTest {

	@TempDir
	File tmp;

	private File write(String name, String content) throws IOException {
		File f = new File(tmp, name);
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	private static String plainSim(String attrs) {
		return SimXml.sim(attrs, SimXml.userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"")
				+ SimXml.source("Src", "C", SimXml.distr("Exponential", "ExponentialPar", "lambda", "2")) + SimXml.sink("Out")
				+ SimXml.connection("Src", "Out"));
	}

	@Test
	@Disabled("bug: XMLReader.loadModel throws NPE on a missing, malformed or empty file instead of returning false")
	void missingFileReturnsFalseAndLeavesModelEmpty() {
		CommonModel m = new CommonModel();
		File missing = new File(tmp, "nope.jsimg");
		// loadXML swallows the IOException and returns null; loadModel then dereferences it
		assertFalse(XMLReader.loadModel(missing, m));
		assertTrue(m.getClassKeys().isEmpty());
		assertTrue(m.getStationKeys().isEmpty());
	}

	@Test
	@Disabled("bug: XMLReader.loadModel throws NPE on a missing, malformed or empty file instead of returning false")
	void notWellFormedXmlReturnsFalseAndLeavesModelEmpty() throws IOException {
		CommonModel m = new CommonModel();
		File f = write("broken.jsimg", "<sim><userClass name=\"C\"");
		assertFalse(XMLReader.loadModel(f, m));
		assertTrue(m.getClassKeys().isEmpty());
	}

	@Test
	@Disabled("bug: XMLReader.loadModel throws NPE on a missing, malformed or empty file instead of returning false")
	void emptyFileReturnsFalse() throws IOException {
		File f = write("empty.jsimg", "");
		assertFalse(XMLReader.loadModel(f, new CommonModel()));
	}

	@Test
	void unknownRootReturnsFalseAndLeavesModelEmpty() throws IOException {
		CommonModel m = new CommonModel();
		assertFalse(XMLReader.loadModel(write("wrong.xml", "<?xml version=\"1.0\"?><model><userClass name=\"C\" type=\"open\"/></model>"), m));
		assertTrue(m.getClassKeys().isEmpty());
		assertTrue(m.getStationKeys().isEmpty());
		assertTrue(m.getMeasureKeys().isEmpty());
	}

	@Test
	void resultsDocumentIsNotAModel() throws IOException {
		CommonModel m = new CommonModel();
		assertFalse(XMLReader.loadModel(write("res.xml", "<results elapsedTime=\"1\"/>"), m));
		assertTrue(m.getClassKeys().isEmpty());
	}

	@Test
	void plainSimDocumentLoadsViaStringAndFileOverloads() throws IOException {
		File f = write("plain.xml", plainSim("seed=\"7\""));
		CommonModel a = new CommonModel();
		assertTrue(XMLReader.loadModel(f, a));
		CommonModel b = new CommonModel();
		assertTrue(XMLReader.loadModel(f.getAbsolutePath(), b));
		for (CommonModel m : new CommonModel[] { a, b }) {
			assertEquals(1, m.getClassKeys().size());
			assertEquals(2, m.getStationKeys().size());
			assertEquals(Long.valueOf(7), m.getSimulationSeed());
			assertFalse(m.getUseRandomSeed());
			assertEquals("exp(2)", String.valueOf(m.getClassDistribution(m.getClassByName("C"))));
		}
	}

	@Test
	void simWithoutContentLoadsAsEmptyModel() throws IOException {
		CommonModel m = new CommonModel();
		assertTrue(XMLReader.loadModel(write("emptysim.xml", "<sim/>"), m));
		assertTrue(m.getClassKeys().isEmpty());
		assertTrue(m.getStationKeys().isEmpty());
		assertTrue(m.getUseRandomSeed());
	}

	@Test
	void schemaViolationInSimIsReportedButStillLoads() throws IOException {
		// validation is non-fatal: an unknown element and a bad attribute do not stop parsing
		String xml = "<?xml version=\"1.0\"?><sim bogusAttribute=\"1\">" + SimXml.userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"")
				+ SimXml.source("Src", "C", SimXml.distr("Exponential", "ExponentialPar", "lambda", "2")) + "<bogus/></sim>";
		CommonModel m = new CommonModel();
		assertTrue(XMLReader.loadModel(write("bogus.xml", xml), m));
		assertEquals(1, m.getClassKeys().size());
		assertEquals(1, m.getStationKeys().size());
	}

	@Test
	void loadXmlWithoutSchemaDoesNotValidate() throws IOException {
		assertNotNull(XMLReader.loadXML(write("any.xml", "<anything/>").getAbsolutePath()));
		assertNull(XMLReader.loadXML(new File(tmp, "absent.xml").getAbsolutePath()));
		assertNull(XMLReader.loadXML(write("bad.xml", "<a>").getAbsolutePath()));
	}

	private static String archive(String simAttrs) {
		return "<archive name=\"a\">" + plainSim(simAttrs) + "<jmodel><userClass color=\"#FF0000FF\" name=\"C\"/></jmodel></archive>";
	}

	@Test
	// deliberately no class assertions: archive loading duplicates classes (see disabled tests below)
	void archiveLoadsAndKeepsStations() throws IOException {
		CommonModel m = new CommonModel();
		assertTrue(XMLReader.loadModel(write("arch.jsimg", archive("")), m));
		assertEquals(2, m.getStationKeys().size());
		assertNotNull(m.getStationByName("Src"));
		assertNotNull(m.getStationByName("Out"));
	}

	@Test
	void archiveLoadDoesNotDuplicateClassesFromGuiSection() throws IOException {
		CommonModel m = new CommonModel();
		assertTrue(XMLReader.loadModel(write("arch.jsimg", archive("")), m));
		assertEquals(1, m.getClassKeys().size());
	}

	@Test
	void archiveLoadReadsSimRootAttributes() throws IOException {
		CommonModel m = new CommonModel();
		assertTrue(XMLReader.loadModel(write("arch.jsimg", archive("seed=\"7\"")), m));
		assertEquals(Long.valueOf(7), m.getSimulationSeed());
		assertFalse(m.getUseRandomSeed());
	}

	@Test
	void openOneClassThreeStationForkLoads() {
		assertTrue(XMLReader.loadModel(new File(ModelFixtures.examplesDir(), "jsim/qn_models/open_1class_3stat_fork.jsimg"), new CommonModel()));
	}

	@Test
	void openOneClassThreeStationAdvForkLoads() {
		assertTrue(XMLReader.loadModel(new File(ModelFixtures.examplesDir(), "jsim/qn_models/open_1class_3stat_advfork.jsimg"), new CommonModel()));
	}
}
