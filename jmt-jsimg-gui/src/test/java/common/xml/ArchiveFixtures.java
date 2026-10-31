package common.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.distributions.Exponential;

/** Shared helpers for the archive (XMLArchiver) tests: example discovery, load, save-to-string, a tiny model, DOM diff. */
public final class ArchiveFixtures {

	private ArchiveFixtures() {
	}

	/** Repo examples/ dir, found by walking up from the working dir until examples/jsim exists. */
	public static File examplesDir() {
		File d = new File(System.getProperty("user.dir")).getAbsoluteFile();
		while (d != null) {
			File ex = new File(d, "examples");
			if (new File(ex, "jsim").isDirectory()) {
				return ex;
			}
			d = d.getParentFile();
		}
		throw new IllegalStateException("examples/jsim not found above " + System.getProperty("user.dir"));
	}

	/** Sorted tracked archives: examples/jsim/**&#47;*.jsimg. */
	public static List<File> listArchives() {
		Path root = new File(examplesDir(), "jsim").toPath();
		try (Stream<Path> s = Files.walk(root)) {
			List<File> r = new ArrayList<File>();
			s.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".jsimg")).forEach(p -> r.add(p.toFile()));
			Collections.sort(r);
			return r;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static CommonModel load(File f) {
		CommonModel m = new CommonModel();
		XMLArchiver.loadModel(f, m);
		return m;
	}

	/** Like {@link #load}, but into a JSimGraphModel, the only model type for which the archive GUI part is read and written. */
	public static jsimgraph.definitions.JSimGraphModel loadGui(File f) {
		jsimgraph.definitions.JSimGraphModel m = new jsimgraph.definitions.JSimGraphModel(null);
		XMLArchiver.loadModel(f, m);
		return m;
	}

	public static String toString(Document d) {
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			StringWriter w = new StringWriter();
			t.transform(new DOMSource(d), new StreamResult(w));
			return w.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static String saveToString(CommonModel m) {
		return toString(XMLArchiver.getDocument(m, "test.jsimg"));
	}

	public static Document parse(String xml) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** One open class "C", Source "Src" -> Server "S" -> Sink "Out", built through the CommonModel API. */
	public static CommonModel simpleModel() {
		CommonModel m = new CommonModel();
		m.addClass("C", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		Object src = m.addStation("Src", CommonConstants.STATION_TYPE_SOURCE, 1, new ArrayList<common.definitions.ServerType>());
		Object srv = m.addStation("S", CommonConstants.STATION_TYPE_SERVER, 1, new ArrayList<common.definitions.ServerType>());
		Object out = m.addStation("Out", CommonConstants.STATION_TYPE_SINK, 1, new ArrayList<common.definitions.ServerType>());
		m.setConnected(src, srv, true);
		m.setConnected(srv, out, true);
		return m;
	}

	/** First difference between two DOM trees, or null; ignores archive-root timestamp/name, logPath, and whitespace/comments. */
	public static String firstDiff(Node a, Node b, String path) {
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

	public static boolean domEquals(Node a, Node b) {
		return firstDiff(a, b, "") == null;
	}

	private static Map<String, String> attrs(Node n) {
		Map<String, String> m = new TreeMap<String, String>();
		NamedNodeMap nm = n.getAttributes();
		for (int i = 0; nm != null && i < nm.getLength(); i++) {
			Attr at = (Attr) nm.item(i);
			String name = at.getName();
			// timestamp/name only on the archive root (they vary per save); station/class/measure names must be compared.
			// logPath is a per-run temp path, ignored as in file-handler's ExamplesRoundTripTest.firstDiff.
			boolean rootVolatile = n.getNodeName().equals("archive") && (name.equals("timestamp") || name.equals("name"));
			if (!rootVolatile && !name.equals("logPath")) {
				m.put(name, at.getValue());
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
}
