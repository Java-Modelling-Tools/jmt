package common.xml;

import static common.xml.AdvancedFeatureSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;

import NetStrategies.ImpatienceStrategies.BalkingParameter;
import NetStrategies.ImpatienceStrategies.ImpatienceType;
import NetStrategies.ImpatienceStrategies.RenegingParameter;
import common.CommonConstants;
import common.definitions.CommonModel;
import common.distributions.Deterministic;
import common.distributions.Exponential;
import common.serviceStrategies.LDStrategy;

/** Queue-section features no bundled example uses: scheduling policies, polling, retrial, reneging and balking. */
class XMLQueueFeaturesTest {

	private static final String PS = CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER;
	private static final String PS_PRIO = CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER_PRIORITY;
	private static final String NP = CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
	private static final String NP_PRIO = CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY;
	private static final String PRE = CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE;
	private static final String PRE_PRIO = CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY;

	private static final String POLLING = CommonConstants.STATION_QUEUE_STRATEGY_POLLING;

	@ParameterizedTest
	@CsvSource({
			"Processor Sharing Scheduling,PS,PS",
			"Processor Sharing Scheduling,GPS,PS",
			"Processor Sharing Scheduling,DPS,PS",
			"Processor Sharing Scheduling (Priority),QBPS,PS",
			"Preemptive Scheduling,FCFS-PR,FCFS-PR",
			"Preemptive Scheduling (Priority),LCFS-PR,LCFS-PR",
			"Preemptive Scheduling,SRPT,SRPT",
			"Non-preemptive Scheduling,LCFS,LCFS",
			"Non-preemptive Scheduling,RAND,RAND",
			"Non-preemptive Scheduling,SJF,SJF",
			"Non-preemptive Scheduling (Priority),LJF,LJF",
			"Non-preemptive Scheduling,SEPT,SEPT",
			"Non-preemptive Scheduling (Priority),LEPT,LEPT" })
	void schedulingPolicyAndClassStrategyRoundTrip(String station, String classStrategy, String reloadedClassStrategy) throws Exception {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), station);
		m.setQueueStrategy(st(m, "S"), cl(m, "C"), classStrategy);
		CommonModel back = roundTrip(m);
		assertEquals(station, back.getStationQueueStrategy(st(back, "S")));
		assertEquals(reloadedClassStrategy.equals("PS") ? classStrategy : reloadedClassStrategy, back.getQueueStrategy(st(back, "S"), cl(back, "C")));
	}

	@Test
	void processorSharingWritesWeightsAndMaxRunningJobs() throws Exception {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), PS);
		m.setQueueStrategy(st(m, "S"), cl(m, "C"), CommonConstants.QUEUE_STRATEGY_PS); // PS stations need a PS class strategy
		m.setServiceWeight(st(m, "S"), cl(m, "C"), Double.valueOf(2.5));
		m.setStationMaxRunningJobs(st(m, "S"), Integer.valueOf(6));
		Element server = section(doc(m), "S", "PSServer");
		assertNotNull(server);
		assertEquals("6", value(server, "maxRunning"));
		assertEquals("2.5", value(server, "serviceWeight"));
		assertEquals("false", value(server, "PSPriorityUsed"));
		CommonModel back = roundTrip(m);
		assertEquals(2.5, back.getServiceWeight(st(back, "S"), cl(back, "C")), 1e-12);
		assertEquals(Integer.valueOf(6), back.getStationMaxRunningJobs(st(back, "S")));
	}

	@Test
	void priorityProcessorSharingMarksPriorityUsed() throws Exception {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), PS_PRIO);
		m.setQueueStrategy(st(m, "S"), cl(m, "C"), CommonConstants.QUEUE_STRATEGY_PS);
		assertEquals("true", value(section(doc(m), "S", "PSServer"), "PSPriorityUsed"));
		assertEquals(PS_PRIO, roundTrip(m).getStationQueueStrategy(st(roundTrip(m), "S")));
	}

	@Test
	void preemptiveStationUsesPreemptiveServerSection() throws Exception {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), PRE);
		m.setQueueStrategy(st(m, "S"), cl(m, "C"), CommonConstants.QUEUE_STRATEGY_LCFS_PR);
		assertNotNull(section(doc(m), "S", "PreemptiveServer"));
		assertNotNull(param(section(doc(m), "S", "PreemptiveServer"), "preemptiveStrategies"));
		assertEquals(CommonConstants.QUEUE_STRATEGY_LCFS_PR, roundTrip(m).getQueueStrategy(st(roundTrip(m), "S"), cl(roundTrip(m), "C")));
	}

	@ParameterizedTest
	@CsvSource({
			"K-Limited,LimitedPollingServer,LimitedPollingGetStrategy",
			"Gated,GatedPollingServer,GatedPollingGetStrategy",
			"Exhaustive,ExhaustivePollingServer,ExhaustivePollingGetStrategy" })
	void pollingServerKindsUseTheirOwnSectionAndGetStrategy(String type, String serverSection, String getStrategy) {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), POLLING);
		m.setStationPollingServerType(st(m, "S"), type);
		m.setStationPollingServerKValue(st(m, "S"), Integer.valueOf(3));
		assertNotNull(section(doc(m), "S", serverSection));
		assertEquals("NetStrategies.QueueGetStrategies." + getStrategy, param(section(doc(m), "S", "Queue"), "FCFSstrategy").getAttribute("classPath"));
		CommonModel back = reloadOnly(m);
		assertEquals(POLLING, back.getStationQueueStrategy(st(back, "S")));
		assertEquals(type, back.getStationPollingServerType(st(back, "S")));
		if (type.equals("K-Limited")) {
			assertEquals(Integer.valueOf(3), back.getStationPollingServerKValue(st(back, "S")));
		}
	}

	@Test
	void pollingSwitchoverTimeIsWrittenAndReloaded() {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), POLLING);
		m.setStationPollingServerType(st(m, "S"), CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
		m.setPollingSwitchoverDistribution(st(m, "S"), cl(m, "C"), new Deterministic());
		CommonModel back = reloadOnly(m);
		assertEquals("Deterministic", ((common.distributions.Distribution) back.getPollingSwitchoverDistribution(st(back, "S"), cl(back, "C"))).getName());
	}

	@Test
	void pollingServerDocumentValidatesAgainstSchema() throws Exception {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), POLLING);
		m.setStationPollingServerType(st(m, "S"), CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
		assertEquals(new ArrayList<String>(), XMLWriterTest.validate(xml(m)));
	}

	@Test
	void pollingSwitchoverWrittenAsPlainParameterByOlderVersionsStillLoads() throws Exception {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), POLLING);
		m.setStationPollingServerType(st(m, "S"), CommonConstants.STATION_QUEUE_STRATEGY_POLLING_GATED);
		m.setPollingSwitchoverDistribution(st(m, "S"), cl(m, "C"), new Deterministic());
		org.w3c.dom.Document d = doc(m);
		Element advanced = param(section(d, "S", "GatedPollingServer"), "advancedFeatures");
		Element sw = (Element) param(advanced, "SwitchoverStrategy");
		d.renameNode(sw, null, "parameter");
		java.io.StringWriter out = new java.io.StringWriter();
		javax.xml.transform.TransformerFactory.newInstance().newTransformer()
				.transform(new javax.xml.transform.dom.DOMSource(d), new javax.xml.transform.stream.StreamResult(out));
		CommonModel back = SimXml.parseSim(simTail(out.toString()));
		assertEquals("Deterministic", ((common.distributions.Distribution) back.getPollingSwitchoverDistribution(st(back, "S"), cl(back, "C"))).getName());
	}

	@Test
	void retrialDropRuleKeepsItsDistribution() throws Exception {
		CommonModel m = base();
		Exponential retry = new Exponential();
		retry.getParameter("lambda").setValue(Double.valueOf(0.5));
		m.setDropRule(st(m, "S"), cl(m, "C"), CommonConstants.FINITE_RETRIAL);
		m.setRetrialDistribution(st(m, "S"), cl(m, "C"), retry);

		Element queue = section(doc(m), "S", "Queue");
		assertEquals("retrial", value(queue, "dropStrategy"));
		assertNotNull(param(queue, "retrialDistributions"));
		CommonModel back = roundTrip(m);
		assertEquals(CommonConstants.FINITE_RETRIAL, back.getDropRule(st(back, "S"), cl(back, "C")));
		common.distributions.Distribution d = (common.distributions.Distribution) back.getRetrialDistribution(st(back, "S"), cl(back, "C"));
		assertEquals(0.5, (Double) d.getParameter("lambda").getValue(), 1e-12);
	}

	@ParameterizedTest
	@CsvSource({ "BAS Blocking,BAS blocking", "Waiting Queue,waiting queue", "Drop,drop" })
	void dropRulesAreWrittenAndReloaded(String rule, String xmlText) throws Exception {
		CommonModel m = base();
		m.setDropRule(st(m, "S"), cl(m, "C"), rule);
		assertEquals(xmlText, value(section(doc(m), "S", "Queue"), "dropStrategy"));
		CommonModel back = roundTrip(m);
		assertEquals(rule, back.getDropRule(st(back, "S"), cl(back, "C")));
		assertNull(param(section(doc(back), "S", "Queue"), "retrialDistributions"));
	}

	@Test
	void renegingImpatienceKeepsItsDistribution() throws Exception {
		CommonModel m = base();
		Exponential patience = new Exponential();
		patience.getParameter("lambda").setValue(Double.valueOf(0.25));
		m.setImpatienceType(st(m, "S"), cl(m, "C"), ImpatienceType.RENEGING);
		m.setImpatienceParameter(st(m, "S"), cl(m, "C"), new RenegingParameter(patience));

		Element impatience = param(section(doc(m), "S", "Queue"), "Impatience");
		assertNotNull(impatience);
		assertNotNull(param(impatience, "Reneging"));
		CommonModel back = roundTrip(m);
		assertEquals(ImpatienceType.RENEGING, back.getImpatienceType(st(back, "S"), cl(back, "C")));
		RenegingParameter got = (RenegingParameter) back.getImpatienceParameter(st(back, "S"), cl(back, "C"));
		assertEquals(0.25, (Double) got.getDistribution().getParameter("lambda").getValue(), 1e-12);
	}

	@Test
	void balkingImpatienceKeepsLoadDependentThresholdsAndPriorityFlag() throws Exception {
		CommonModel m = base();
		LDStrategy ld = new LDStrategy();
		Object r0 = ld.addRange();
		ld.setRangeFrom(r0, 1);
		ld.setRangeDistribution(r0, new Deterministic());
		ld.setRangeDistributionMean(r0, "0.0");
		BalkingParameter balking = new BalkingParameter(ld);
		balking.setPriorityActivated(true);
		m.setImpatienceType(st(m, "S"), cl(m, "C"), ImpatienceType.BALKING);
		m.setImpatienceParameter(st(m, "S"), cl(m, "C"), balking);

		Element impatience = param(section(doc(m), "S", "Queue"), "Impatience");
		assertNotNull(param(impatience, "Balking"));
		assertEquals("true", value(impatience, "priorityActivated"));
		CommonModel back = roundTrip(m);
		assertEquals(ImpatienceType.BALKING, back.getImpatienceType(st(back, "S"), cl(back, "C")));
		BalkingParameter got = (BalkingParameter) back.getImpatienceParameter(st(back, "S"), cl(back, "C"));
		assertTrue(got.getPriorityActivated());
		assertEquals(ld.getRangeNumber(), got.getLdStrategy().getRangeNumber());
		assertEquals(ranges(ld), ranges(got.getLdStrategy()));
		assertTrue(ranges(got.getLdStrategy()).contains("2 Deterministic"), ranges(got.getLdStrategy()).toString());
	}

	/** "from distributionName" of every range, sorted. */
	private static java.util.List<String> ranges(LDStrategy ld) {
		java.util.List<String> r = new ArrayList<String>();
		for (Object k : ld.getAllRanges()) {
			r.add(ld.getRangeFrom(k) + " " + ld.getRangeDistribution(k).getName());
		}
		java.util.Collections.sort(r);
		return r;
	}

	@Test
	void noImpatienceWritesNoImpatienceParameter() throws Exception {
		CommonModel m = base();
		assertNull(param(section(doc(m), "S", "Queue"), "Impatience"));
		CommonModel back = roundTrip(m);
		assertTrue(back.getImpatienceType(st(back, "S"), cl(back, "C")) == null
				|| back.getImpatienceType(st(back, "S"), cl(back, "C")) == ImpatienceType.NONE);
	}
}
