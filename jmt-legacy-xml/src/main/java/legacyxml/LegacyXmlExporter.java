package legacyxml;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Rewrites a JMT XML file (any tool that stores <code>classPath</code> attributes: .jsimg,
 * .jsimw, simulation input) between {@link XmlFormat}s. Command line:
 * <code>java -cp jmt-legacy-xml.jar legacyxml.LegacyXmlExporter in.jsimg out.jsimg [version]</code>
 * where version defaults to 1.4.0.
 */
public final class LegacyXmlExporter {

	private static final String CLASS_PATH = "classPath";

	/** Archive root attribute written by common.xml.XMLArchiver (jmt-jsimg-gui). */
	private static final String VERSION_ATTRIBUTE = "jmtVersion";

	/** Top-level packages that used to live under "jmt.engine." (see git history of the module refactoring). */
	private static final List<String> ENGINE_ROOTS = Arrays.asList("NetStrategies.", "NodeSections.", "QueueNet.", "random.", "math.",
			"simEngine.", "dataAnalysis.");

	/** Top-level packages that used to live under "jmt.gui.". */
	private static final List<String> GUI_ROOTS = Arrays.asList("common.");

	private static final String ENGINE_PREFIX = "jmt.engine.";
	private static final String GUI_PREFIX = "jmt.gui.";

	private LegacyXmlExporter() {
	}

	/** Reads <code>in</code> (any format), writes <code>out</code> in <code>target</code> format. */
	public static void export(File in, File out, XmlFormat target) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(in);
		convert(doc, target);
		write(doc, out);
	}

	/**
	 * Rewrites every <code>classPath</code> attribute of the document in place. For {@link XmlFormat#V1_4_0} the
	 * <code>jmtVersion</code> root attribute is also removed (older schemas reject unknown attributes) and its
	 * value is kept in a leading comment.
	 */
	public static void convert(Document doc, XmlFormat target) {
		Element root = doc.getDocumentElement();
		rewrite(root, target);
		if (target == XmlFormat.V1_4_0 && root.hasAttribute(VERSION_ATTRIBUTE)) {
			String version = root.getAttribute(VERSION_ATTRIBUTE);
			root.removeAttribute(VERSION_ATTRIBUTE);
			root.insertBefore(doc.createComment(" Saved by JMT " + version + " in the format of releases up to 1.4.0 "), root.getFirstChild());
		}
	}

	/** Maps one class name to the requested format; names it does not recognise (java.*, already converted) are returned as is. */
	public static String convertClassName(String name, XmlFormat target) {
		if (target == XmlFormat.V1_4_0) {
			for (String root : ENGINE_ROOTS) {
				if (name.startsWith(root)) {
					return ENGINE_PREFIX + name;
				}
			}
			for (String root : GUI_ROOTS) {
				if (name.startsWith(root)) {
					return GUI_PREFIX + name;
				}
			}
			return name;
		}
		if (name.startsWith(ENGINE_PREFIX)) {
			return name.substring(ENGINE_PREFIX.length());
		}
		if (name.startsWith(GUI_PREFIX)) {
			return name.substring(GUI_PREFIX.length());
		}
		return name;
	}

	private static void rewrite(Element e, XmlFormat target) {
		if (e.hasAttribute(CLASS_PATH)) {
			e.setAttribute(CLASS_PATH, convertClassName(e.getAttribute(CLASS_PATH), target));
		}
		NodeList children = e.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node c = children.item(i);
			if (c.getNodeType() == Node.ELEMENT_NODE) {
				rewrite((Element) c, target);
			}
		}
	}

	/** File the legacy copy of <code>current</code> is written to: <code>model.jsimg</code> becomes <code>model.legacy.jsimg</code>. */
	public static File legacyFileFor(File current) {
		String name = current.getName();
		int dot = name.lastIndexOf('.');
		String legacy = dot < 0 ? name + ".legacy" : name.substring(0, dot) + ".legacy" + name.substring(dot);
		return new File(current.getParentFile(), legacy);
	}

	/**
	 * Writes the {@link XmlFormat#V1_4_0} copy of a document that was just saved as <code>currentFile</code>.
	 * Works on a copy, so <code>current</code> keeps the current format. Never throws: the legacy copy is
	 * a courtesy and must not fail the real save.
	 * @return the legacy file, or null if it could not be written
	 */
	public static File saveLegacyCopy(Document current, File currentFile) {
		File legacy = legacyFileFor(currentFile);
		try {
			Document copy = (Document) current.cloneNode(true);
			convert(copy, XmlFormat.V1_4_0);
			if (copy.getDocumentElement().hasAttribute("name")) {
				copy.getDocumentElement().setAttribute("name", legacy.getName());
			}
			write(copy, legacy);
			return legacy;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void write(Document doc, File out) throws Exception {
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.ENCODING, doc.getXmlEncoding() != null ? doc.getXmlEncoding() : "ISO-8859-1");
		t.setOutputProperty(OutputKeys.STANDALONE, "no");
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.transform(new DOMSource(doc), new StreamResult(out));
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args.length > 3) {
			System.err.println("usage: LegacyXmlExporter <in> <out> [version]   (version: current | 1.4.0, default 1.4.0)");
			System.exit(2);
		}
		File in = new File(args[0]);
		File out = new File(args[1]);
		if (!in.isFile()) {
			throw new IOException("No such file: " + in);
		}
		export(in, out, XmlFormat.fromName(args.length == 3 ? args[2] : "1.4.0"));
	}
}
