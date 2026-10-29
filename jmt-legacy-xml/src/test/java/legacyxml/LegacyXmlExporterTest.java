package legacyxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class LegacyXmlExporterTest {

	private static final File OLD_FORMAT_DIR = new File("../jmt-app/src/test/resources/commandline");
	private static final File CURRENT_FORMAT_SAMPLE = new File(
			"../jmt-integration-tests/src/test/resources/jmt/integration/input/jsim/1Fork2JoinOK.jsimg");

	@Test
	void mapsEachRootBothWays() {
		assertEquals("jmt.engine.random.Exponential", LegacyXmlExporter.convertClassName("random.Exponential", XmlFormat.V1_4_0));
		assertEquals("jmt.engine.NetStrategies.QueuePutStrategies.TailDrop",
				LegacyXmlExporter.convertClassName("NetStrategies.QueuePutStrategies.TailDrop", XmlFormat.V1_4_0));
		assertEquals("jmt.gui.common.definitions.parametric.SeedParametricAnalysis",
				LegacyXmlExporter.convertClassName("common.definitions.parametric.SeedParametricAnalysis", XmlFormat.V1_4_0));
		assertEquals("random.Exponential", LegacyXmlExporter.convertClassName("jmt.engine.random.Exponential", XmlFormat.CURRENT));
		assertEquals("common.definitions.parametric.SeedParametricAnalysis",
				LegacyXmlExporter.convertClassName("jmt.gui.common.definitions.parametric.SeedParametricAnalysis", XmlFormat.CURRENT));
	}

	@Test
	void leavesJdkAndAlreadyConvertedNamesAlone() {
		assertEquals("java.lang.Double", LegacyXmlExporter.convertClassName("java.lang.Double", XmlFormat.V1_4_0));
		assertEquals("jmt.engine.random.Exponential", LegacyXmlExporter.convertClassName("jmt.engine.random.Exponential", XmlFormat.V1_4_0));
		assertEquals("random.Exponential", LegacyXmlExporter.convertClassName("random.Exponential", XmlFormat.CURRENT));
	}

	@Test
	void resolvesReleaseNames() {
		assertEquals(XmlFormat.V1_4_0, XmlFormat.fromName("1.4.0"));
		assertEquals(XmlFormat.V1_4_0, XmlFormat.fromName("1.3.1-BETA2"));
		assertEquals(XmlFormat.CURRENT, XmlFormat.fromName("CURRENT"));
	}

	/** Every fixture written by an old release must survive old -> current -> old unchanged. */
	@Test
	void roundTripsEveryOldReleaseFixture() throws Exception {
		List<File> files;
		try (Stream<File> s = Stream.of(OLD_FORMAT_DIR.listFiles((d, n) -> n.endsWith("-input.xml")))) {
			files = s.collect(Collectors.toList());
		}
		assertFalse(files.isEmpty());
		for (File f : files) {
			List<String> original = classPaths(parse(f));
			Document doc = parse(f);
			LegacyXmlExporter.convert(doc, XmlFormat.CURRENT);
			LegacyXmlExporter.convert(doc, XmlFormat.V1_4_0);
			assertEquals(original, classPaths(doc), f.getName());
		}
	}

	/** A file saved by the current code must come out with no unprefixed JMT class names. */
	@Test
	void exportsCurrentFileForOldReleases() throws Exception {
		File out = File.createTempFile("legacy", ".jsimg");
		try {
			LegacyXmlExporter.export(CURRENT_FORMAT_SAMPLE, out, XmlFormat.V1_4_0);
			List<String> paths = classPaths(parse(out));
			assertFalse(paths.isEmpty());
			for (String p : paths) {
				assertTrue(p.startsWith("java.") || p.startsWith("jmt."), p);
			}
			// same content otherwise: converting back reproduces the source class names
			Document back = parse(out);
			LegacyXmlExporter.convert(back, XmlFormat.CURRENT);
			assertEquals(classPaths(parse(CURRENT_FORMAT_SAMPLE)), classPaths(back));
		} finally {
			Files.deleteIfExists(out.toPath());
		}
	}

	@Test
	void legacyFileSitsNextToTheCurrentOne() {
		assertEquals(new File("/m/net.legacy.jsimg"), LegacyXmlExporter.legacyFileFor(new File("/m/net.jsimg")));
		assertEquals(new File("/m/net.legacy"), LegacyXmlExporter.legacyFileFor(new File("/m/net")));
	}

	/** The save hook: current document untouched, legacy copy written beside it. */
	@Test
	void saveLegacyCopyLeavesCurrentDocumentUntouched() throws Exception {
		File dir = Files.createTempDirectory("legacysave").toFile();
		File current = new File(dir, "net.jsimg");
		File legacy = new File(dir, "net.legacy.jsimg");
		try {
			Document doc = parse(CURRENT_FORMAT_SAMPLE);
			List<String> before = classPaths(doc);
			assertEquals(legacy, LegacyXmlExporter.saveLegacyCopy(doc, current));
			assertEquals(before, classPaths(doc));
			Document written = parse(legacy);
			assertEquals("net.legacy.jsimg", written.getDocumentElement().getAttribute("name"));
			for (String p : classPaths(written)) {
				assertTrue(p.startsWith("java.") || p.startsWith("jmt."), p);
			}
		} finally {
			Files.deleteIfExists(legacy.toPath());
			Files.deleteIfExists(dir.toPath());
		}
	}

	/** Old schemas reject unknown attributes, so the version moves from an attribute to a comment. */
	@Test
	void legacyCopyDropsVersionAttributeButKeepsItInAComment() throws Exception {
		Document doc = parse(CURRENT_FORMAT_SAMPLE);
		doc.getDocumentElement().setAttribute("jmtVersion", "9.9.9");
		LegacyXmlExporter.convert(doc, XmlFormat.V1_4_0);
		assertFalse(doc.getDocumentElement().hasAttribute("jmtVersion"));
		assertEquals(org.w3c.dom.Node.COMMENT_NODE, doc.getDocumentElement().getFirstChild().getNodeType());
		assertTrue(doc.getDocumentElement().getFirstChild().getNodeValue().contains("JMT 9.9.9"));
	}

	@Test
	void currentFormatKeepsVersionAttribute() throws Exception {
		Document doc = parse(CURRENT_FORMAT_SAMPLE);
		doc.getDocumentElement().setAttribute("jmtVersion", "9.9.9");
		LegacyXmlExporter.convert(doc, XmlFormat.CURRENT);
		assertEquals("9.9.9", doc.getDocumentElement().getAttribute("jmtVersion"));
	}

	private static Document parse(File f) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		return dbf.newDocumentBuilder().parse(f);
	}

	private static List<String> classPaths(Document d) throws IOException {
		List<String> out = new ArrayList<>();
		NodeList all = d.getElementsByTagName("*");
		for (int i = 0; i < all.getLength(); i++) {
			Element e = (Element) all.item(i);
			if (e.hasAttribute("classPath")) {
				out.add(e.getAttribute("classPath"));
			}
		}
		return out;
	}
}
