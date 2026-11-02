package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Safety net over every bundled .jsimg: the {@code <sim>} element (what the GUI archiver feeds the reader, since
 * XMLReader.loadModel wrongly parses the whole archive) must load, survive write -> reparse -> write unchanged, and validate.
 */
@Tag("slow")
class ExamplesRoundTripTest {

	/** file name -> one-line reason for a genuine write/reload difference (kept visible, never normalised away). */
	static final Map<String, String> KNOWN_DIFFERENCES = new TreeMap<String, String>();

	private static List<File> jsimg() {
		List<File> r = new ArrayList<File>();
		File jsim = new File(ModelFixtures.examplesDir(), "jsim");
		for (File f : ModelFixtures.listExamples("*.jsimg")) {
			if (f.getAbsolutePath().startsWith(jsim.getAbsolutePath())) {
				r.add(f);
			}
		}
		return r;
	}

	private static String rel(File f) {
		return f.getAbsolutePath().substring(ModelFixtures.examplesDir().getAbsolutePath().length() + 1);
	}

	private static String simTail(String xml) {
		return xml.substring(xml.indexOf("<sim"));
	}

	/** First differing element path, or null if equal. Ignores whitespace-only text, attribute order, comments, logPath. */
	static String firstDiff(Node a, Node b, String path) {
		String p = path + "/" + a.getNodeName();
		if (!a.getNodeName().equals(b.getNodeName())) {
			return path + ": <" + a.getNodeName() + "> vs <" + b.getNodeName() + ">";
		}
		Map<String, String> aa = attrs(a), ba = attrs(b);
		if (!aa.equals(ba)) {
			return p + ": attributes " + aa + " vs " + ba;
		}
		List<Node> ac = kids(a), bc = kids(b);
		for (int i = 0; i < Math.min(ac.size(), bc.size()); i++) {
			Node x = ac.get(i), y = bc.get(i);
			if (x.getNodeType() != Node.ELEMENT_NODE || y.getNodeType() != Node.ELEMENT_NODE) {
				if (x.getNodeType() != y.getNodeType() || !x.getNodeValue().trim().equals(y.getNodeValue().trim())) {
					return p + ": text '" + x.getNodeValue() + "' vs '" + y.getNodeValue() + "'";
				}
				continue;
			}
			String d = firstDiff(x, y, p + "[" + i + "]");
			if (d != null) {
				return d;
			}
		}
		if (ac.size() != bc.size()) {
			return p + ": child count " + ac.size() + " vs " + bc.size();
		}
		return null;
	}

	private static Map<String, String> attrs(Node n) {
		Map<String, String> m = new TreeMap<String, String>();
		NamedNodeMap nm = n.getAttributes();
		for (int i = 0; nm != null && i < nm.getLength(); i++) {
			Attr at = (Attr) nm.item(i);
			if (!at.getName().equals("logPath")) {
				m.put(at.getName(), at.getValue());
			}
		}
		return m;
	}

	private static List<Node> kids(Node n) {
		List<Node> r = new ArrayList<Node>();
		for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
			boolean ws = c.getNodeType() == Node.TEXT_NODE && c.getNodeValue().trim().isEmpty();
			if (!ws && c.getNodeType() != Node.COMMENT_NODE) {
				r.add(c);
			}
		}
		return r;
	}

	@Test
	void normaliserDetectsAndIgnoresTheRightThings() {
		Element a = SimXml.parseDoc("<a x=\"1\" y=\"2\" logPath=\"p\"> <b>t</b><!--c--></a>").getDocumentElement();
		Element same = SimXml.parseDoc("<a logPath=\"q\" y=\"2\" x=\"1\"><b> t </b></a>").getDocumentElement();
		Element diff = SimXml.parseDoc("<a x=\"1\" y=\"2\"><b>u</b></a>").getDocumentElement();
		assertEquals(null, firstDiff(a, same, ""));
		assertEquals("/a[0]/b: text 't' vs 'u'", firstDiff(a, diff, ""));
	}

	@Test
	void everyExampleSimLoads() {
		List<File> all = jsimg();
		assertFalse(all.isEmpty(), "no .jsimg under examples/jsim");
		List<String> bad = new ArrayList<String>();
		for (File f : all) {
			try {
				SimXml.loadSim(f);
			} catch (RuntimeException e) {
				bad.add(rel(f) + ": " + e);
			}
		}
		assertEquals(new ArrayList<String>(), bad, all.size() + " examples checked");
	}

	@Test
	void writeReloadWriteIsDomEqual() {
		List<File> all = jsimg();
		assertFalse(all.isEmpty());
		List<String> bad = new ArrayList<String>();
		for (File f : all) {
			if (KNOWN_DIFFERENCES.containsKey(f.getName())) {
				continue;
			}
			try {
				String first = ModelFixtures.writeToString(SimXml.loadSim(f));
				String second = ModelFixtures.writeToString(SimXml.parseSim(simTail(first)));
				String d = firstDiff(SimXml.parseDoc(first).getDocumentElement(), SimXml.parseDoc(second).getDocumentElement(), "");
				if (d != null) {
					bad.add(rel(f) + " -> " + d);
				}
			} catch (RuntimeException e) {
				bad.add(rel(f) + " threw " + e);
			}
		}
		assertEquals(new ArrayList<String>(), bad, all.size() + " examples checked");
	}

	@Test
	void writtenDocumentsValidateAgainstSchema() throws Exception {
		List<File> all = jsimg();
		assertFalse(all.isEmpty());
		List<String> bad = new ArrayList<String>();
		for (File f : all) {
			try {
				List<String> errs = XMLWriterTest.validate(ModelFixtures.writeToString(SimXml.loadSim(f)));
				if (!errs.isEmpty()) {
					bad.add(rel(f) + ": " + errs.get(0));
				}
			} catch (RuntimeException e) {
				bad.add(rel(f) + " threw " + e);
			}
		}
		assertEquals(new ArrayList<String>(), bad, all.size() + " examples checked");
	}
}
