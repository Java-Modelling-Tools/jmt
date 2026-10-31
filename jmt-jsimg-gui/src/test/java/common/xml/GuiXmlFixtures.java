package common.xml;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;

import common.CommonConstants;
import common.distributions.Exponential;
import jmt.common.xml.XSDSchemaLoader;
import jsimgraph.definitions.JMTArc;
import jsimgraph.definitions.JMTPath;
import jsimgraph.definitions.JMTPoint;
import jsimgraph.definitions.JSimGraphModel;

/** Builders and a snapshot of the GUI state for the GuiXMLWriter/GuiXMLReader tests. */
final class GuiXmlFixtures {

	/** Station kinds the GUI can add, in the order they are chained by {@link #fullModel()}. */
	static final String[] KINDS = { CommonConstants.STATION_TYPE_SOURCE, CommonConstants.STATION_TYPE_ROUTER, CommonConstants.STATION_TYPE_FORK,
			CommonConstants.STATION_TYPE_SERVER, CommonConstants.STATION_TYPE_JOIN, CommonConstants.STATION_TYPE_DELAY,
			CommonConstants.STATION_TYPE_CLASSSWITCH, CommonConstants.STATION_TYPE_LOGGER, CommonConstants.STATION_TYPE_SINK };

	private GuiXmlFixtures() {
	}

	static JSimGraphModel empty() {
		return new JSimGraphModel(null);
	}

	/** Two coloured open classes plus one station of each kind, positioned, chained, with one bent connection. */
	static JSimGraphModel fullModel() {
		JSimGraphModel m = empty();
		Object c1 = m.addClass("A", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		Object c2 = m.addClass("B", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		m.setClassColor(c1, new Color(0x12, 0x34, 0x56));
		m.setClassColor(c2, new Color(0xFE, 0xDC, 0xBA));
		Object prev = null;
		int i = 0;
		for (String kind : KINDS) {
			Object k = m.addStation(kind);
			m.setStationPosition(k, new JMTPoint(10.5 + 100 * i, 20.25 + 7 * i, i % 2 == 1, i % 2 == 1 ? 90.0 : 0.0));
			if (prev != null) {
				m.setConnected(prev, k, true);
			}
			prev = k;
			i++;
		}
		List<Object> ks = m.getStationKeys();
		JMTArc a0 = new JMTArc(new Point2D.Double(1, 2), pts(3, 4, 5, 6), new Point2D.Double(7, 8));
		JMTArc a1 = new JMTArc(new Point2D.Double(7, 8), pts(9.5, 10, 11, 12.5), new Point2D.Double(13, 14));
		ArrayList<JMTArc> arcs = new ArrayList<JMTArc>();
		arcs.add(a0);
		arcs.add(a1);
		m.setConnectionShape(ks.get(0), ks.get(1), new JMTPath(arcs));
		return m;
	}

	static ArrayList<Point2D> pts(double x0, double y0, double x1, double y1) {
		ArrayList<Point2D> l = new ArrayList<Point2D>();
		l.add(new Point2D.Double(x0, y0));
		l.add(new Point2D.Double(x1, y1));
		return l;
	}

	/** Same stations (by name, same kinds, same connections) and classes as {@link #fullModel()}, but with no GUI state. */
	static JSimGraphModel bareCopyOf(JSimGraphModel src) {
		JSimGraphModel m = empty();
		for (Object k : src.getClassKeys()) {
			m.addClass(src.getClassName(k), CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		}
		Map<Object, Object> map = new LinkedHashMap<Object, Object>();
		for (Object k : src.getStationKeys()) {
			Object n = m.addStation(src.getStationType(k));
			m.setStationName(n, src.getStationName(k));
			map.put(k, n);
		}
		for (Object a : src.getStationKeys()) {
			for (Object b : src.getStationKeys()) {
				if (src.areConnected(a, b)) {
					m.setConnected(map.get(a), map.get(b), true);
				}
			}
		}
		return m;
	}

	/** Comparable GUI state: class colours, station positions, connection shapes, all keyed by name. */
	static Map<String, String> snapshot(JSimGraphModel m) {
		Map<String, String> s = new LinkedHashMap<String, String>();
		for (Object k : m.getClassKeys()) {
			Color c = m.getClassColor(k);
			s.put("class " + m.getClassName(k), c == null ? "null" : Integer.toHexString(c.getRGB()));
		}
		List<Object> ks = m.getStationKeys();
		for (Object k : ks) {
			JMTPoint p = m.getStationPosition(k);
			s.put("station " + m.getStationName(k), p == null ? "null" : p.getX() + "," + p.getY() + "," + p.isRotate() + "," + p.getAngle());
		}
		for (Object a : ks) {
			for (Object b : ks) {
				if (m.hasConnectionShape(a, b)) {
					StringBuilder sb = new StringBuilder();
					JMTPath p = m.getConnectionShape(a, b);
					for (int i = 0; i < p.getArcsNb(); i++) {
						JMTArc r = p.getArc(i);
						sb.append(r.getSource()).append(r.getArcPoints()).append(r.getTarget()).append(';');
					}
					s.put("shape " + m.getStationName(a) + "->" + m.getStationName(b), sb.toString());
				}
			}
		}
		return s;
	}

	/** Schema errors of a GUI document (JModelGUI.xsd); empty means valid. */
	static List<String> validate(Document d) throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(new java.net.URL(XSDSchemaLoader.loadSchema(XSDSchemaLoader.JMODEL_GUI_DEFINITION)));
		final List<String> errs = new ArrayList<String>();
		javax.xml.validation.Validator v = schema.newValidator();
		v.setErrorHandler(new org.xml.sax.ErrorHandler() {
			public void warning(org.xml.sax.SAXParseException e) {
			}

			public void error(org.xml.sax.SAXParseException e) {
				errs.add(e.getMessage());
			}

			public void fatalError(org.xml.sax.SAXParseException e) {
				errs.add(e.getMessage());
			}
		});
		v.validate(new StreamSource(new ByteArrayInputStream(ArchiveFixtures.toString(d).getBytes(StandardCharsets.UTF_8))));
		return errs;
	}
}
