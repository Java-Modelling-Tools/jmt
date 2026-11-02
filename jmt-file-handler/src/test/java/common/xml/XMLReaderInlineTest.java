package common.xml;

import static common.xml.SimXml.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.distributions.Distribution;

/** XMLReader on small inline {@code <sim>} documents: distributions, queue strategies, root attributes, measures. */
class XMLReaderInlineTest {

	private static CommonModel openSource(String distrXml) {
		return parseSim(sim("", userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"") + source("Src", "C", distrXml) + sink("Out")
				+ connection("Src", "Out")));
	}

	private static Distribution classDistr(CommonModel m) {
		return (Distribution) m.getClassDistribution(m.getClassByName("C"));
	}

	@ParameterizedTest
	@CsvSource({
			"Exponential,ExponentialPar,Exponential,lambda,4.0",
			"Exponential,ExponentialPar,Exponential,lambda,0.5",
			"DeterministicDistr,DeterministicDistrPar,Deterministic,t,2.5",
			"Uniform,UniformPar,Uniform,min,1.0",
			"Normal,NormalPar,Normal,mean,3.0",
			"Pareto,ParetoPar,Pareto,alpha,3.5",
			"GammaDistr,GammaDistrPar,Gamma,alpha,2.0",
			"Weibull,WeibullPar,Weibull,alpha,0.7",
			"Lognormal,LognormalPar,Lognormal,mu,0.1" })
	void sourceDistributionKindAndFirstParameter(String engine, String par, String guiName, String param, String value) {
		Distribution d = classDistr(openSource(distr(engine, par, param, value)));
		assertNotNull(d);
		assertEquals(guiName, d.getName());
		assertEquals(Double.valueOf(value), d.getParameter(param).getValue());
	}

	@Test
	void hyperexponentialAndCoxianParameters() {
		Distribution h = classDistr(openSource(distr("HyperExp", "HyperExpPar", "p", "0.3", "lambda1", "2.0", "lambda2", "5.0")));
		assertEquals("Hyperexponential", h.getName());
		assertEquals(0.3, (Double) h.getParameter("p").getValue(), 1e-12);
		assertEquals(2.0, (Double) h.getParameter("lambda1").getValue(), 1e-12);
		assertEquals(5.0, (Double) h.getParameter("lambda2").getValue(), 1e-12);
		assertTrue(h.getMean() > 0, "mean/c are recomputed after parsing");

		Distribution c = classDistr(openSource(distr("CoxianDistr", "CoxianPar", "lambda0", "4.0", "lambda1", "0.5", "phi0", "0.875")));
		assertEquals("Coxian", c.getName());
		assertEquals(0.875, (Double) c.getParameter("phi0").getValue(), 1e-12);
	}

	@Test
	void erlangKeepsLongRParameter() {
		Distribution e = classDistr(openSource(distr("Erlang", "ErlangPar", "alpha", "0.5", "r", "3")));
		assertEquals("Erlang", e.getName());
		assertEquals(Long.valueOf(3), e.getParameter("r").getValue());
	}

	@Test
	void legacyEnginePrefixIsAccepted() {
		// classpath "jmt.engine.random.X" is mapped to the gui "random.X" entry
		assertEquals("Exponential", classDistr(openSource(distr("Exponential", "ExponentialPar", "lambda", "1"))).getName());
	}

	@Test
	void unknownDistributionClassYieldsNullClassDistribution() {
		assertNull(parseDistribution("NoSuchDistr", "NoSuchPar"));
	}

	private static Object parseDistribution(String engine, String par) {
		org.w3c.dom.Element d = (org.w3c.dom.Element) parseDoc("<r>" + distr(engine, par, "x", "1") + "</r>").getDocumentElement()
				.getFirstChild();
		org.w3c.dom.Element p = (org.w3c.dom.Element) d.getNextSibling();
		return XMLReader.parseDistribution(d, p);
	}

	@ParameterizedTest
	@CsvSource({
			"TailStrategy,NON_PREEMPTIVE,FCFS", "HeadStrategy,NON_PREEMPTIVE,LCFS", "RandStrategy,NON_PREEMPTIVE,RAND",
			"SJFStrategy,NON_PREEMPTIVE,SJF", "LJFStrategy,NON_PREEMPTIVE,LJF", "SEPTStrategy,NON_PREEMPTIVE,SEPT",
			"LEPTStrategy,NON_PREEMPTIVE,LEPT", "TailStrategyPriority,NON_PREEMPTIVE_PRIORITY,FCFS",
			"HeadStrategyPriority,NON_PREEMPTIVE_PRIORITY,LCFS", "FCFSPRStrategy,PREEMPTIVE,FCFS-PR",
			"LCFSPRStrategy,PREEMPTIVE,LCFS-PR", "SRPTStrategy,PREEMPTIVE,SRPT", "FCFSPRStrategyPriority,PREEMPTIVE_PRIORITY,FCFS-PR",
			"EDDStrategy,NON_PREEMPTIVE,EDD", "EDFStrategy,PREEMPTIVE,EDF", "TBSStrategyPriority,PREEMPTIVE_PRIORITY,TBS" })
	void queuePutStrategyMapsToStationAndClassStrategy(String put, String stationKind, String classStrategy) {
		String station = stationConstant(stationKind);
		CommonModel m = parseSim(sim("", userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"")
				+ source("Src", "C", distr("Exponential", "ExponentialPar", "lambda", "1")) + server("S", "C", put, 2, 7,
						distr("Exponential", "ExponentialPar", "lambda", "3"))
				+ sink("Out") + connection("Src", "S") + connection("S", "Out")));
		Object s = m.getStationByName("S");
		Object c = m.getClassByName("C");
		assertEquals(station, m.getStationQueueStrategy(s));
		assertEquals(classStrategy, m.getQueueStrategy(s, c));
		assertEquals(Integer.valueOf(2), m.getStationNumberOfServers(s));
		assertEquals(Integer.valueOf(7), m.getStationQueueCapacity(s));
		assertEquals("Server", m.getStationType(s));
	}

	private static String stationConstant(String kind) {
		switch (kind) {
		case "NON_PREEMPTIVE":
			return CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
		case "NON_PREEMPTIVE_PRIORITY":
			return CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY;
		case "PREEMPTIVE":
			return CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE;
		default:
			return CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY;
		}
	}

	@Test
	void rootAttributesWithSeedAndLimits() {
		CommonModel m = parseSim(sim("seed=\"42\" maxTime=\"500.5\" maxSamples=\"1234\" polling=\"3.0\" disableStatisticStop=\"true\" maxEvents=\"99\"",
				userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"") + source("Src", "C", distr("Exponential", "ExponentialPar", "lambda", "1"))));
		assertEquals(Long.valueOf(42), m.getSimulationSeed());
		assertFalse(m.getUseRandomSeed());
		assertEquals(500.5, m.getMaximumDuration(), 1e-12);
		assertEquals(Integer.valueOf(1234), m.getMaxSimulationSamples());
		assertEquals(3.0, m.getPollingInterval(), 1e-12);
		assertTrue(m.getDisableStatistic());
		assertEquals(Integer.valueOf(99), m.getMaxSimulationEvents());
	}

	@Test
	void rootWithoutSeedUsesRandomSeed() {
		CommonModel m = parseSim(sim("", userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"")
				+ source("Src", "C", distr("Exponential", "ExponentialPar", "lambda", "1"))));
		assertTrue(m.getUseRandomSeed());
	}

	@Test
	void classAttributesPriorityPopulationAndDeadline() {
		CommonModel m = parseSim(sim("", userClass("A", "closed", "customers=\"9\" priority=\"3\" referenceSource=\"Src\" softDeadline=\"2.5\"")
				+ userClass("B", "open", "priority=\"1\" referenceSource=\"Src\"")
				+ source("Src", "A", distr("Exponential", "ExponentialPar", "lambda", "1"))));
		Object a = m.getClassByName("A");
		Object b = m.getClassByName("B");
		assertEquals(CommonConstants.CLASS_TYPE_CLOSED, m.getClassType(a));
		assertEquals(CommonConstants.CLASS_TYPE_OPEN, m.getClassType(b));
		assertEquals(Integer.valueOf(9), m.getClassPopulation(a));
		assertEquals(Integer.valueOf(3), m.getClassPriority(a));
		assertEquals(Integer.valueOf(1), m.getClassPriority(b));
		assertEquals(2, m.getClassKeys().size());
		assertEquals(1, m.getClosedClassKeys().size());
		assertEquals(1, m.getOpenClassKeys().size());
	}

	@Test
	void measuresInvertAlphaAndKeepStationAndClass() {
		CommonModel m = parseSim(sim("", userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"")
				+ source("Src", "C", distr("Exponential", "ExponentialPar", "lambda", "1"))
				+ server("S", "C", "TailStrategy", 1, -1, distr("Exponential", "ExponentialPar", "lambda", "3")) + sink("Out")
				+ measure("Response Time", "S", "C", "0.99", "0.05") + measure("Throughput", "S", "", "0.95", "0.1")
				+ measure("System Response Time", "", "", "0.9", "0.2")));
		assertEquals(3, m.getMeasureKeys().size());
		Object k0 = m.getMeasureKeys().get(0);
		assertEquals("Response Time", m.getMeasureType(k0));
		assertEquals(m.getStationByName("S"), m.getMeasureStation(k0));
		assertEquals(m.getClassByName("C"), m.getMeasureClass(k0));
		assertEquals(0.01, m.getMeasureAlpha(k0), 1e-12);
		assertEquals(0.05, m.getMeasurePrecision(k0), 1e-12);
		Object k1 = m.getMeasureKeys().get(1);
		assertNull(m.getMeasureClass(k1));
		assertEquals(0.05, m.getMeasureAlpha(k1), 1e-12);
		Object k2 = m.getMeasureKeys().get(2);
		assertNull(m.getMeasureStation(k2));
		assertTrue(m.isGlobalMeasure(m.getMeasureType(k2)));
	}

	@Test
	void connectionsAreRecorded() {
		CommonModel m = parseSim(sim("", userClass("C", "open", "priority=\"0\" referenceSource=\"Src\"")
				+ source("Src", "C", distr("Exponential", "ExponentialPar", "lambda", "1"))
				+ server("S", "C", "TailStrategy", 1, -1, distr("Exponential", "ExponentialPar", "lambda", "3")) + sink("Out")
				+ connection("Src", "S") + connection("S", "Out")));
		Object src = m.getStationByName("Src"), s = m.getStationByName("S"), out = m.getStationByName("Out");
		assertTrue(m.areConnected(src, s));
		assertTrue(m.areConnected(s, out));
		assertFalse(m.areConnected(src, out));
		assertEquals("Source", m.getStationType(src));
		assertEquals("Sink", m.getStationType(out));
	}

	@Test
	void preloadingIsAppliedPerStationAndClass() {
		String preload = "<preload><stationPopulations stationName=\"S\"><classPopulation population=\"4\" refClass=\"A\"/></stationPopulations></preload>";
		CommonModel m = parseSim(sim("", userClass("A", "closed", "customers=\"4\" priority=\"0\" referenceSource=\"S\"")
				+ server("S", "A", "TailStrategy", 1, -1, distr("Exponential", "ExponentialPar", "lambda", "3")) + preload));
		assertEquals(Integer.valueOf(4), m.getPreloadedJobs(m.getStationByName("S"), m.getClassByName("A")));
	}
}
