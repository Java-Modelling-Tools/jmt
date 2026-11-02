package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import common.definitions.CommonModel;

/** Spike: archives load and save headless, and the fixtures behave. */
class ArchiveFixturesTest {

	@TempDir
	File tmp;

	@Test
	void examplesAreFound() {
		assertFalse(ArchiveFixtures.listArchives().isEmpty());
	}

	@Test
	void simpleModelSavesAsArchiveWithVersion() {
		CommonModel m = ArchiveFixtures.simpleModel();
		assertEquals(3, m.getStationKeys().size());
		assertEquals(1, m.getClassKeys().size());
		Document d = ArchiveFixtures.parse(ArchiveFixtures.saveToString(m));
		assertEquals("archive", d.getDocumentElement().getNodeName());
		assertFalse(d.getDocumentElement().getAttribute("jmtVersion").isEmpty());
	}

	@Test
	void exampleLoadsSavesAndReloadsHeadless() {
		File f = ArchiveFixtures.listArchives().get(0);
		CommonModel m1 = ArchiveFixtures.load(f);
		assertFalse(m1.getStationKeys().isEmpty());
		Document d1 = XMLArchiver.getDocument(m1, "x.jsimg");
		Document d2 = XMLArchiver.getDocument(ArchiveFixtures.load(f), "x.jsimg");
		assertNull(ArchiveFixtures.firstDiff(d1, d2, ""));
		CommonModel m2 = new CommonModel();
		XMLArchiver.loadModel(writeTemp(ArchiveFixtures.toString(d1)), m2);
		assertEquals(m1.getStationKeys().size(), m2.getStationKeys().size());
		assertEquals(m1.getClassKeys().size(), m2.getClassKeys().size());
	}

	@Test
	void firstDiffIgnoresTimestampAndNameButSeesRealChanges() {
		Document a = ArchiveFixtures.parse("<archive timestamp=\"1\" name=\"x\"><b k=\"1\"/></archive>");
		Document same = ArchiveFixtures.parse("<archive timestamp=\"2\" name=\"y\"><b k=\"1\"/></archive>");
		Document diff = ArchiveFixtures.parse("<archive><b k=\"2\"/></archive>");
		assertNull(ArchiveFixtures.firstDiff(a, same, ""));
		assertNotNull(ArchiveFixtures.firstDiff(a, diff, ""));
	}

	@Test
	void firstDiffDetectsChangedStationName() {
		CommonModel m = ArchiveFixtures.simpleModel();
		Document before = ArchiveFixtures.parse(ArchiveFixtures.saveToString(m));
		m.setStationName(m.getStationByName("S"), "Renamed");
		Document after = ArchiveFixtures.parse(ArchiveFixtures.saveToString(m));
		assertNotNull(ArchiveFixtures.firstDiff(before, after, ""));
	}

	private File writeTemp(String xml) {
		try {
			File f = new File(tmp, "archive.jsimg");
			java.nio.file.Files.write(f.toPath(), xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return f;
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
