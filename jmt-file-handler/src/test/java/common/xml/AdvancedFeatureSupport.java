package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import common.CommonConstants;
import common.definitions.CommonModel;

/** Shared helpers for the hand-made-model tests: build a model, write it, validate, reload, compare. */
final class AdvancedFeatureSupport {

	private AdvancedFeatureSupport() {
	}

	/** Open class C: Src -> S (queue + 2 servers) -> Out, as in XMLWriterTest.BASE. */
	static CommonModel base() {
		return SimXml.parseSim(XMLWriterTest.BASE);
	}

	static Object st(CommonModel m, String name) {
		return m.getStationByName(name);
	}

	static Object cl(CommonModel m, String name) {
		return m.getClassByName(name);
	}

	/** Adds a station and returns its key. */
	static Object add(CommonModel m, String name, String type) {
		return m.addStation(name, type, 1, new ArrayList<common.definitions.ServerType>());
	}

	static void connect(CommonModel m, String from, String to) {
		m.setConnected(st(m, from), st(m, to), true);
	}

	static Document doc(CommonModel m) {
		return XMLWriter.getDocument(m, "x");
	}

	static String xml(CommonModel m) {
		return ModelFixtures.writeToString(m);
	}

	static String simTail(String xml) {
		return xml.substring(xml.indexOf("<sim"));
	}

	/**
	 * Writes the model, asserts it is schema valid, reloads it through the reader and asserts that writing the reloaded
	 * model gives a DOM-equal document (same normaliser as the examples round trip; logPath ignored). Returns the reloaded model.
	 */
	static CommonModel roundTrip(CommonModel m) throws Exception {
		String first = xml(m);
		assertEquals(new ArrayList<String>(), XMLWriterTest.validate(first), "first write must validate");
		CommonModel back = SimXml.parseSim(simTail(first));
		String second = xml(back);
		assertNull(ExamplesRoundTripTest.firstDiff(SimXml.parseDoc(first).getDocumentElement(), SimXml.parseDoc(second).getDocumentElement(), ""),
				"write -> reload -> write differs");
		return back;
	}

	/** Like roundTrip but only the DOM equality (for models whose first write is known not to validate). */
	static CommonModel reloadOnly(CommonModel m) {
		String first = xml(m);
		CommonModel back = SimXml.parseSim(simTail(first));
		assertNull(ExamplesRoundTripTest.firstDiff(SimXml.parseDoc(first).getDocumentElement(), SimXml.parseDoc(xml(back)).getDocumentElement(), ""),
				"write -> reload -> write differs");
		return back;
	}

	/** The first element with the given tag and name attribute, or null. */
	static Element named(Document d, String tag, String name) {
		return XMLWriterTest.byName(d, tag, name);
	}

	/** Section element of a node by className, or null. */
	static Element section(Document d, String station, String className) {
		NodeList l = named(d, "node", station).getElementsByTagName("section");
		for (int i = 0; i < l.getLength(); i++) {
			if (className.equals(((Element) l.item(i)).getAttribute("className"))) {
				return (Element) l.item(i);
			}
		}
		return null;
	}

	/** First descendant parameter or subParameter with this name, or null. */
	static Element param(Element parent, String name) {
		for (String tag : new String[] { "parameter", "subParameter" }) {
			NodeList l = parent.getElementsByTagName(tag);
			for (int i = 0; i < l.getLength(); i++) {
				if (name.equals(((Element) l.item(i)).getAttribute("name"))) {
					return (Element) l.item(i);
				}
			}
		}
		return null;
	}

	static String value(Element parent, String name) {
		Element p = param(parent, name);
		return p == null ? null : p.getElementsByTagName("value").item(0).getTextContent();
	}

	static final String SERVER = CommonConstants.STATION_TYPE_SERVER;
}
