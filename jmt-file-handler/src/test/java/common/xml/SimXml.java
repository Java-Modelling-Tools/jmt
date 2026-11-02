package common.xml;

import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import common.definitions.CommonModel;

/**
 * Test helpers that feed XMLReader.parseXML(Element, CommonModel) directly with the engine
 * {@code <sim>} element (the way XMLArchiver does), plus small builders for inline documents.
 */
final class SimXml {

	static final String SVC = "jmt.engine.NetStrategies.ServiceStrategy";
	static final String SVC_TIME = "jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy";
	static final String ROUTING = "jmt.engine.NetStrategies.RoutingStrategy";
	static final String ROUTING_PKG = "jmt.engine.NetStrategies.RoutingStrategies.";
	static final String PUT_PKG = "jmt.engine.NetStrategies.QueuePutStrategies.";

	private SimXml() {
	}

	static Document parseDoc(String xml) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** Parses a {@code <sim>} element given as a string. */
	static CommonModel parseSim(String simXml) {
		CommonModel m = new CommonModel();
		XMLReader.parseXML(parseDoc(simXml).getDocumentElement(), m);
		return m;
	}

	/** Loads only the {@code <sim>} part of a .jsimg archive (what the GUI archiver does). */
	static CommonModel loadSim(File jsimg) {
		Document d = XMLReader.loadXML(jsimg.getAbsolutePath());
		CommonModel m = new CommonModel();
		XMLReader.parseXML((Element) d.getElementsByTagName("sim").item(0), m);
		return m;
	}

	static String sim(String attrs, String body) {
		return "<sim " + attrs + ">" + body + "</sim>";
	}

	static String userClass(String name, String type, String extraAttrs) {
		return "<userClass name=\"" + name + "\" type=\"" + type + "\" " + extraAttrs + "/>";
	}

	/** A distribution subParameter pair; kv = name, value, name, value... */
	static String distr(String engineClass, String parClass, String... kv) {
		StringBuilder sb = new StringBuilder();
		sb.append("<subParameter classPath=\"jmt.engine.random.").append(engineClass).append("\" name=\"d\"/>");
		sb.append("<subParameter classPath=\"jmt.engine.random.").append(parClass).append("\" name=\"distrPar\">");
		for (int i = 0; i < kv.length; i += 2) {
			sb.append("<subParameter classPath=\"java.lang.Double\" name=\"").append(kv[i]).append("\"><value>").append(kv[i + 1])
					.append("</value></subParameter>");
		}
		return sb.append("</subParameter>").toString();
	}

	/** Per-class service strategy parameter wrapping a distribution (distr(...) output). */
	static String svcParam(String cls, String distrXml) {
		return "<parameter array=\"true\" classPath=\"" + SVC + "\" name=\"ServiceStrategy\"><refClass>" + cls + "</refClass>"
				+ "<subParameter classPath=\"" + SVC_TIME + "\" name=\"ServiceTimeStrategy\">" + distrXml + "</subParameter></parameter>";
	}

	static String routing(String cls, String strategyClass) {
		return "<section className=\"Router\"><parameter array=\"true\" classPath=\"" + ROUTING + "\" name=\"RoutingStrategy\"><refClass>" + cls
				+ "</refClass><subParameter classPath=\"" + ROUTING_PKG + strategyClass + "\" name=\"r\"/></parameter></section>";
	}

	static String source(String name, String cls, String distrXml) {
		return "<node name=\"" + name + "\"><section className=\"RandomSource\">" + svcParam(cls, distrXml)
				+ "</section><section className=\"ServiceTunnel\"/>" + routing(cls, "RandomStrategy") + "</node>";
	}

	static String sink(String name) {
		return "<node name=\"" + name + "\"><section className=\"JobSink\"/></node>";
	}

	/** Queue + Server station with one class, given queue put strategy simple name (e.g. "TailStrategy"). */
	static String server(String name, String cls, String putStrategy, int servers, int capacity, String distrXml) {
		return "<node name=\"" + name + "\"><section className=\"Queue\">"
				+ "<parameter classPath=\"java.lang.Integer\" name=\"size\"><value>" + capacity + "</value></parameter>"
				+ "<parameter array=\"true\" classPath=\"java.lang.String\" name=\"dropStrategies\"><refClass>" + cls
				+ "</refClass><subParameter classPath=\"java.lang.String\" name=\"dropStrategy\"><value>drop</value></subParameter></parameter>"
				+ "<parameter classPath=\"jmt.engine.NetStrategies.QueueGetStrategies.FCFSstrategy\" name=\"FCFSstrategy\"/>"
				+ "<parameter array=\"true\" classPath=\"jmt.engine.NetStrategies.QueuePutStrategy\" name=\"QueuePutStrategy\"><refClass>" + cls
				+ "</refClass><subParameter classPath=\"" + PUT_PKG + putStrategy + "\" name=\"p\"/></parameter></section>"
				+ "<section className=\"Server\"><parameter classPath=\"java.lang.Integer\" name=\"maxJobs\"><value>" + servers
				+ "</value></parameter>" + svcParam(cls, distrXml) + "</section>" + routing(cls, "RandomStrategy") + "</node>";
	}

	static String connection(String from, String to) {
		return "<connection source=\"" + from + "\" target=\"" + to + "\"/>";
	}

	static String measure(String type, String station, String cls, String alpha, String precision) {
		return "<measure alpha=\"" + alpha + "\" name=\"m\" nodeType=\"station\" precision=\"" + precision + "\" referenceNode=\"" + station
				+ "\" referenceUserClass=\"" + cls + "\" type=\"" + type + "\" verbose=\"false\"/>";
	}
}
