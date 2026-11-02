package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import jmt.common.xml.XSDSchemaLoader;
import jsimgraph.definitions.JSimGraphModel;
import legacyxml.LegacyXmlExporter;

/**
 * Safety net over every bundled examples/jsim .jsimg through the production archive path (XMLArchiver, JSimGraphModel so the
 * GUI part is exercised): load -> getDocument -> save -> reload into a fresh model -> getDocument must be DOM-equal, each
 * written archive and its sim part must validate, and the legacy copy must be produced.
 */
@Tag("slow")
class ArchiveExamplesRoundTripTest {

	/** file name -> one-line reason for a genuine difference or failure (kept visible, never normalised away). */
	static final Map<String, String> KNOWN_DIFFERENCES = new TreeMap<String, String>();

	@TempDir
	File tmp;

	private static String rel(File f) {
		return f.getAbsolutePath().substring(ArchiveFixtures.examplesDir().getAbsolutePath().length() + 1);
	}

	private static List<String> validate(Document d, String xsd) throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new java.net.URL(XSDSchemaLoader.loadSchema(xsd)));
		final List<String> errs = new ArrayList<String>();
		Validator v = schema.newValidator();
		v.setErrorHandler(new ErrorHandler() {
			public void warning(SAXParseException e) {
			}

			public void error(SAXParseException e) {
				errs.add(e.getMessage());
			}

			public void fatalError(SAXParseException e) {
				errs.add(e.getMessage());
			}
		});
		v.validate(new StreamSource(new ByteArrayInputStream(ArchiveFixtures.toString(d).getBytes(StandardCharsets.UTF_8))));
		return errs;
	}

	@Test
	void everyExampleArchiveRoundTrips() throws Exception {
		List<File> all = ArchiveFixtures.listArchives();
		assertFalse(all.isEmpty(), "no .jsimg under examples/jsim");
		List<String> bad = new ArrayList<String>();
		int i = 0;
		long t0 = System.nanoTime();
		for (File f : all) {
			if (KNOWN_DIFFERENCES.containsKey(f.getName())) {
				continue;
			}
			try {
				Document first = XMLArchiver.getDocument(ArchiveFixtures.loadGui(f), f.getName());
				assertEquals("jmodel", XMLArchiver.getGuiFromArchiveDocument(first).getNodeName(), f.getName() + ": GUI part missing");
				File out = new File(new File(tmp, String.valueOf(i++)), "m.jsimg");
				assertTrue(out.getParentFile().mkdirs());
				XMLArchiver.saveModel(out, ArchiveFixtures.loadGui(f));
				if (!out.isFile() || !LegacyXmlExporter.legacyFileFor(out).isFile()) {
					bad.add(rel(f) + ": archive or legacy copy not written");
					continue;
				}
				JSimGraphModel back = new JSimGraphModel(null);
				XMLArchiver.loadModel(out, back);
				Document second = XMLArchiver.getDocument(back, f.getName());
				String d = ArchiveFixtures.firstDiff(first.getDocumentElement(), second.getDocumentElement(), "");
				if (d != null) {
					bad.add(rel(f) + " -> " + d);
				}
				List<String> e = validate(XMLArchiver.getDocument(back, out.getName()), XSDSchemaLoader.JMT_ARCHIVE);
				if (!e.isEmpty()) {
					bad.add(rel(f) + " archive schema: " + e.get(0));
				}
				Document sim = ArchiveFixtures.parse("<x/>");
				sim.replaceChild(sim.importNode(XMLArchiver.getSimFromArchiveDocument(second), true), sim.getDocumentElement());
				e = validate(sim, XSDSchemaLoader.JSIM_MODEL_DEFINITION);
				if (!e.isEmpty()) {
					bad.add(rel(f) + " sim schema: " + e.get(0));
				}
			} catch (Throwable t) {
				bad.add(rel(f) + " threw " + t);
			}
		}
		System.out.println("ArchiveExamplesRoundTripTest: " + all.size() + " archives, " + bad.size() + " failures, "
				+ (System.nanoTime() - t0) / 1000000 + " ms");
		assertEquals(new ArrayList<String>(), bad, all.size() + " examples checked");
	}

	@Test
	void forkExamplesLoadThroughArchiver() {
		int n = 0;
		for (File f : ArchiveFixtures.listArchives()) {
			if (f.getName().startsWith("open_1class_3stat_") && f.getName().endsWith("fork.jsimg")) {
				n++;
				assertFalse(ArchiveFixtures.loadGui(f).getStationKeys().isEmpty(), f.getName());
			}
		}
		assertEquals(2, n, "the two #34 fork examples");
	}
}
