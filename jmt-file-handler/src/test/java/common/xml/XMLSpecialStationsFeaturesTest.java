package common.xml;

import static common.xml.AdvancedFeatureSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.distributions.Exponential;
import common.semaphoreStrategies.NormalSemaphore;
import common.semaphoreStrategies.SemaphoreStrategy;
import log.LoggerParameters;

/** Logger, semaphore, terminal, scaler and class-switch stations, none of which a tracked example uses. */
class XMLSpecialStationsFeaturesTest {

	/** Src -> S -> X -> Out with a station of the given type inserted after S. */
	private static CommonModel withStationAfterServer(String name, String type) {
		CommonModel m = base();
		m.setConnected(st(m, "S"), st(m, "Out"), false);
		add(m, name, type);
		connect(m, "S", name);
		connect(m, name, "Out");
		return m;
	}

	@Test
	void loggerStationKeepsItsParameters(@org.junit.jupiter.api.io.TempDir java.io.File tmp) throws Exception {
		CommonModel m = withStationAfterServer("Log", CommonConstants.STATION_TYPE_LOGGER);
		LoggerParameters lp = new LoggerParameters("trace.csv", "/tmp/", Boolean.TRUE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE,
				Boolean.TRUE, Boolean.FALSE, Boolean.TRUE);
		m.setLoggingGlbParameter("path", tmp.getAbsolutePath());
		m.setLoggingParameters(st(m, "Log"), lp);

		Element e = section(doc(m), "Log", "LogTunnel");
		assertNotNull(e);
		assertEquals("trace.csv", value(e, "logfileName"));
		assertEquals("true", value(e, "logExecTimestamp"));
		assertEquals("false", value(e, "logLoggerName"));
		assertEquals("1", value(e, "numClasses"));

		CommonModel back = roundTrip(m);
		LoggerParameters got = (LoggerParameters) back.getLoggingParameters(st(back, "Log"));
		assertEquals("trace.csv", got.name);
		assertTrue(got.boolExecTimestamp);
		assertFalse(got.boolLoggername);
		assertTrue(got.boolTimeStamp);
		assertFalse(got.boolJobID);
		assertTrue(got.boolJobClass);
		assertFalse(got.boolTimeSameClass);
		assertTrue(got.boolTimeAnyClass);
	}

	@Test
	void loggerGlobalOptionsAreWrittenAsRootAttributes(@org.junit.jupiter.api.io.TempDir java.io.File tmp) throws Exception {
		CommonModel m = withStationAfterServer("Log", CommonConstants.STATION_TYPE_LOGGER);
		m.setLoggingGlbParameter("path", tmp.getAbsolutePath());
		m.setLoggingGlbParameter("delim", ";");
		m.setLoggingGlbParameter("decimalSeparator", ",");
		m.setLoggingGlbParameter("autoAppend", String.valueOf(LoggerParameters.LOGGER_AR_APPEND));
		Element root = doc(m).getDocumentElement();
		assertEquals(";", root.getAttribute("logDelimiter"));
		assertEquals(",", root.getAttribute("logDecimalSeparator"));
		assertEquals("2", root.getAttribute("logReplaceMode"));
		CommonModel back = roundTrip(m);
		assertEquals(";", back.getLoggingGlbParameter("delim"));
		assertEquals(",", back.getLoggingGlbParameter("decimalSeparator"));
		assertEquals("2", back.getLoggingGlbParameter("autoAppend"));
	}

	@Test
	void semaphoreStationKeepsThreshold() throws Exception {
		CommonModel m = withStationAfterServer("Sem", CommonConstants.STATION_TYPE_SEMAPHORE);
		m.setSemaphoreThreshold(st(m, "Sem"), cl(m, "C"), Integer.valueOf(4));

		Element e = section(doc(m), "Sem", "Semaphore");
		assertNotNull(e);
		assertEquals("4", value(e, "threshold"));
		assertEquals(java.util.Arrays.asList("Semaphore", "ServiceTunnel", "Router"), sectionNamesOf(doc(m), "Sem"));

		CommonModel back = roundTrip(m);
		SemaphoreStrategy got = (SemaphoreStrategy) back.getSemaphoreStrategy(st(back, "Sem"), cl(back, "C"));
		assertInstanceOf(NormalSemaphore.class, got);
		assertEquals(4, got.getThreshold());
	}

	@Test
	void semaphoreWithTwoClassesWritesOneStrategyPerClass() throws Exception {
		CommonModel m = base();
		Object d = m.addClass("D", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		m.setClassRefStation(d, st(m, "Src"));
		m.setConnected(st(m, "S"), st(m, "Out"), false);
		add(m, "Sem", CommonConstants.STATION_TYPE_SEMAPHORE);
		connect(m, "S", "Sem");
		connect(m, "Sem", "Out");
		m.setSemaphoreThreshold(st(m, "Sem"), cl(m, "C"), Integer.valueOf(2));
		m.setSemaphoreThreshold(st(m, "Sem"), d, Integer.valueOf(5));
		CommonModel back = roundTrip(m);
		assertEquals(2, ((SemaphoreStrategy) back.getSemaphoreStrategy(st(back, "Sem"), cl(back, "C"))).getThreshold());
		assertEquals(5, ((SemaphoreStrategy) back.getSemaphoreStrategy(st(back, "Sem"), cl(back, "D"))).getThreshold());
	}

	@Test
	void classSwitchStationKeepsMatrix() throws Exception {
		CommonModel m = base();
		Object d = m.addClass("D", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		m.setClassRefStation(d, st(m, "Src"));
		m.setConnected(st(m, "S"), st(m, "Out"), false);
		add(m, "CS", CommonConstants.STATION_TYPE_CLASSSWITCH);
		connect(m, "S", "CS");
		connect(m, "CS", "Out");
		m.setClassSwitchMatrix(st(m, "CS"), cl(m, "C"), cl(m, "C"), 0.25f);
		m.setClassSwitchMatrix(st(m, "CS"), cl(m, "C"), d, 0.75f);
		m.setClassSwitchMatrix(st(m, "CS"), d, d, 1f);
		m.setClassSwitchMatrix(st(m, "CS"), d, cl(m, "C"), 0f);

		assertEquals(java.util.Arrays.asList("Queue", "ClassSwitch", "Router"), sectionNamesOf(doc(m), "CS"));
		CommonModel back = roundTrip(m);
		assertEquals(0.75f, back.getClassSwitchMatrix(st(back, "CS"), cl(back, "C"), cl(back, "D")), 1e-6);
		assertEquals(0.25f, back.getClassSwitchMatrix(st(back, "CS"), cl(back, "C"), cl(back, "C")), 1e-6);
	}

	@Test
	void terminalStationGeneratesAClosedClass() throws Exception {
		CommonModel m = base();
		Object t = add(m, "Term", CommonConstants.STATION_TYPE_TERMINAL);
		Object k = m.addClass("Cl", CommonConstants.CLASS_TYPE_CLOSED, Integer.valueOf(0), Double.valueOf(0), Integer.valueOf(3), null);
		m.setClassRefStation(k, t);
		connect(m, "Term", "S");
		connect(m, "S", "Term");

		Element e = section(doc(m), "Term", "Terminal");
		assertNotNull(e);
		org.w3c.dom.NodeList jobs = e.getElementsByTagName("subParameter");
		java.util.List<String> numbers = new java.util.ArrayList<String>();
		for (int i = 0; i < jobs.getLength(); i++) {
			if ("numberOfJobs".equals(((Element) jobs.item(i)).getAttribute("name"))) {
				numbers.add(((Element) jobs.item(i)).getElementsByTagName("value").item(0).getTextContent());
			}
		}
		assertEquals(java.util.Arrays.asList("-1", "3"), numbers, "-1 marks the class this terminal does not generate");
		assertEquals(java.util.Arrays.asList("Terminal", "ServiceTunnel", "Router"), sectionNamesOf(doc(m), "Term"));
		CommonModel back = roundTrip(m);
		assertEquals(CommonConstants.STATION_TYPE_TERMINAL, back.getStationType(st(back, "Term")));
		assertEquals(Integer.valueOf(3), back.getClassPopulation(cl(back, "Cl")));
	}

	@Test
	void scalerStationHasJoinTunnelAndForkSections() throws Exception {
		CommonModel m = withStationAfterServer("Sc", CommonConstants.STATION_TYPE_SCALER);
		assertEquals(java.util.Arrays.asList("Join", "ServiceTunnel", "Fork"), sectionNamesOf(doc(m), "Sc"));
		CommonModel back = roundTrip(m);
		assertEquals(CommonConstants.STATION_TYPE_SCALER, back.getStationType(st(back, "Sc")));
	}

	@Test
	void routerStationIsWrittenWithQueueTunnelAndRouter() throws Exception {
		CommonModel m = withStationAfterServer("R", CommonConstants.STATION_TYPE_ROUTER);
		assertEquals(java.util.Arrays.asList("Queue", "ServiceTunnel", "Router"), sectionNamesOf(doc(m), "R"));
		assertEquals(CommonConstants.STATION_TYPE_ROUTER, roundTrip(m).getStationType(st(roundTrip(m), "R")));
	}

	private static java.util.List<String> sectionNamesOf(org.w3c.dom.Document d, String station) {
		java.util.List<String> r = new java.util.ArrayList<String>();
		org.w3c.dom.NodeList l = named(d, "node", station).getElementsByTagName("section");
		for (int i = 0; i < l.getLength(); i++) {
			r.add(((Element) l.item(i)).getAttribute("className"));
		}
		return r;
	}
}
