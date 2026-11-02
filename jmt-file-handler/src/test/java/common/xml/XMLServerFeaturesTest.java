package common.xml;

import static common.xml.AdvancedFeatureSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.definitions.VacationParameters;
import common.distributions.Deterministic;
import common.distributions.Distribution;
import common.distributions.Exponential;
import common.serviceStrategies.DisabledStrategy;
import common.serviceStrategies.LDStrategy;
import common.serviceStrategies.ZeroStrategy;

/** Server-section features no bundled example uses: vacations, set-up / delay-off / switch-over, server types, service strategies. */
class XMLServerFeaturesTest {

	private static Distribution exp(double lambda) {
		Exponential e = new Exponential();
		e.getParameter("lambda").setValue(Double.valueOf(lambda));
		return e;
	}

	private static double lambdaOf(Object distribution) {
		return (Double) ((Distribution) distribution).getParameter("lambda").getValue();
	}

	@ParameterizedTest
	@CsvSource({
			"Single Vacation when Idle,VacationWhenIdleStrategy,0",
			"Multiple Vacation when Idle,VacationWhenIdleStrategy,1",
			"Bernoulli Vacation,BernoulliVacationStrategy,-" })
	void vacationPoliciesKeepTheirDistributionAndParameters(String policy, String engineClass, String type) throws Exception {
		CommonModel m = base();
		m.setStationVacationPolicy(st(m, "S"), policy);
		VacationParameters vp = m.getStationVacationParameters(st(m, "S"));
		vp.setVacationTimeDistribution(exp(4.0));
		vp.setBernoulliP(Double.valueOf(0.3));

		Element v = param(section(doc(m), "S", "Server"), "vacationStrategy");
		assertEquals("NetStrategies.VacationStrategies." + engineClass, v.getAttribute("classPath"));
		if (!type.equals("-")) {
			assertEquals(type, value(v, "type"));
		} else {
			assertEquals("0.3", value(v, "p"));
		}
		CommonModel back = roundTrip(m);
		assertEquals(policy, back.getStationVacationPolicy(st(back, "S")));
		assertEquals(4.0, lambdaOf(back.getStationVacationParameters(st(back, "S")).getVacationTimeDistribution()), 1e-12);
		if (type.equals("-")) {
			assertEquals(0.3, back.getStationVacationParameters(st(back, "S")).getBernoulliP(), 1e-12);
		}
	}

	@Test
	void noVacationIsTheDefaultAndWritesTheEmptyStrategy() throws Exception {
		CommonModel m = base();
		Element v = param(section(doc(m), "S", "Server"), "vacationStrategy");
		assertEquals("NetStrategies.VacationStrategies.NoVacationStrategy", v.getAttribute("classPath"));
		assertEquals(CommonConstants.STATION_VACATION_POLICY_NONE, roundTrip(m).getStationVacationPolicy(st(roundTrip(m), "S")));
	}

	@Test
	void switchoverTimesAreWrittenPerClassPairForNonPreemptiveStations() throws Exception {
		CommonModel m = base();
		Object d = m.addClass("D", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		m.setClassRefStation(d, st(m, "Src"));
		m.setStationQueueStrategy(st(m, "S"), CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
		m.setSwitchoverTimesEnabled(st(m, "S"), Boolean.TRUE);
		m.setSwitchoverTimeDistribution(st(m, "S"), cl(m, "C"), d, exp(8.0));
		m.setSwitchoverTimeDistribution(st(m, "S"), d, cl(m, "C"), new Deterministic());

		assertNotNull(param(section(doc(m), "S", "Server"), "SwitchoverStrategy"));
		CommonModel back = roundTrip(m);
		assertEquals(Boolean.TRUE, back.getSwitchoverTimesEnabled(st(back, "S")));
		assertEquals(8.0, lambdaOf(back.getSwitchoverTimeDistribution(st(back, "S"), cl(back, "C"), cl(back, "D"))), 1e-12);
		assertEquals("Deterministic", ((Distribution) back.getSwitchoverTimeDistribution(st(back, "S"), cl(back, "D"), cl(back, "C"))).getName());
	}

	@Test
	void switchoverStrategiesAreOmittedWhenDisabled() {
		CommonModel m = base();
		m.setStationQueueStrategy(st(m, "S"), CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
		assertNull(param(section(doc(m), "S", "Server"), "SwitchoverStrategy"));
	}

	@Test
	void delayOffAndSetupTimesAreWrittenTogether() throws Exception {
		CommonModel m = base();
		m.setDelayOffTimesEnabled(st(m, "S"), Boolean.TRUE);
		m.setDelayOffTimeDistribution(st(m, "S"), cl(m, "C"), exp(2.0));
		m.setSetupTimeDistribution(st(m, "S"), cl(m, "C"), exp(6.0));

		Element server = section(doc(m), "S", "Server");
		assertNotNull(param(server, "delayOffTime"));
		assertNotNull(param(server, "setUpTime"));
		assertEquals(new java.util.ArrayList<String>(), XMLWriterTest.validate(xml(m)));
		CommonModel back = SimXml.parseSim(simTail(xml(m)));
		assertEquals(Boolean.TRUE, back.getDelayOffTimesEnabled(st(back, "S")));
	}

	@Test
	void delayOffAndSetupTimesSurviveReload() throws Exception {
		CommonModel m = base();
		m.setDelayOffTimesEnabled(st(m, "S"), Boolean.TRUE);
		m.setDelayOffTimeDistribution(st(m, "S"), cl(m, "C"), exp(2.0));
		m.setSetupTimeDistribution(st(m, "S"), cl(m, "C"), exp(6.0));
		CommonModel back = roundTrip(m);
		assertEquals(2.0, lambdaOf(back.getDelayOffTimeDistribution(st(back, "S"), cl(back, "C"))), 1e-12);
		assertEquals(6.0, lambdaOf(back.getSetupTimeDistribution(st(back, "S"), cl(back, "C"))), 1e-12);
	}

	@Test
	void serverNumRequiredIsKept() throws Exception {
		CommonModel m = base();
		m.setServerNumRequired(st(m, "S"), cl(m, "C"), Integer.valueOf(2));
		assertEquals("2", value(section(doc(m), "S", "Server"), "serverParallelism"));
		CommonModel back = roundTrip(m);
		assertEquals(Integer.valueOf(2), back.getServerNumRequired(st(back, "S"), cl(back, "C")));
	}

	@Test
	void schedulingPolicyIsWritten() throws Exception {
		CommonModel m = base();
		m.setStationSchedulingPolicy(st(m, "S"), CommonConstants.STATION_SCHEDULING_POLICY_FSF);
		assertEquals(CommonConstants.STATION_SCHEDULING_POLICY_FSF, value(section(doc(m), "S", "Server"), "schedulingPolicy"));
		assertEquals(new java.util.ArrayList<String>(), XMLWriterTest.validate(xml(m)));
	}

	@Test
	void schedulingPolicySurvivesReload() {
		CommonModel m = base();
		m.setStationSchedulingPolicy(st(m, "S"), CommonConstants.STATION_SCHEDULING_POLICY_FSF);
		CommonModel back = reloadOnly(m);
		assertEquals(CommonConstants.STATION_SCHEDULING_POLICY_FSF, back.getStationSchedulingPolicy(st(back, "S")));
	}

	@Test
	void heterogeneousServerTypesKeepNamesCountsAndPerTypeServiceTimes() throws Exception {
		CommonModel m = base();
		Object s = st(m, "S");
		m.addServerType(s, "Fast", 1, Arrays.asList(Boolean.TRUE), false);
		m.addServerType(s, "Slow", 3, Arrays.asList(Boolean.TRUE), false);
		m.setHeterogeneousServersEnabled(s, Boolean.TRUE);
		for (common.definitions.ServerType t : m.getServerTypes(s)) {
			m.setServiceTimeDistribution(s, cl(m, "C"), t.getServerKey(), exp(t.getName().equals("Fast") ? 10.0 : 1.0));
		}

		Element server = section(doc(m), "S", "Server");
		assertEquals("S - Fast", value(server, "serverTypesNames"));
		CommonModel back = roundTrip(m);
		Object bs = st(back, "S");
		assertEquals(Boolean.TRUE, back.getHeterogeneousServersEnabled(bs));
		assertEquals(Arrays.asList("Fast", "Slow"), back.getServerTypeNames(bs));
		double fast = 0, slow = 0;
		for (common.definitions.ServerType t : back.getServerTypes(bs)) {
			double l = lambdaOf(back.getServiceTimeDistribution(bs, cl(back, "C"), t.getServerKey()));
			if (t.getName().equals("Fast")) {
				fast = l;
				assertEquals(1, t.getNumOfServers());
			} else {
				slow = l;
				assertEquals(3, t.getNumOfServers());
			}
		}
		assertEquals(10.0, fast, 1e-12);
		assertEquals(1.0, slow, 1e-12);
	}

	@Test
	void zeroAndDisabledServiceStrategiesAreWrittenByClassPath() throws Exception {
		CommonModel m = base();
		Object d = m.addClass("D", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		m.setClassRefStation(d, st(m, "Src"));
		m.setServiceTimeDistribution(st(m, "S"), cl(m, "C"), new ZeroStrategy());
		m.setServiceTimeDistribution(st(m, "S"), d, new DisabledStrategy());

		Element server = section(doc(m), "S", "Server");
		assertNotNull(param(server, "ZeroServiceTimeStrategy"));
		assertNotNull(param(server, "DisabledServiceTimeStrategy"));
		CommonModel back = roundTrip(m);
		assertInstanceOf(ZeroStrategy.class, back.getServiceTimeDistribution(st(back, "S"), cl(back, "C")));
		assertInstanceOf(DisabledStrategy.class, back.getServiceTimeDistribution(st(back, "S"), cl(back, "D")));
	}

	@Test
	void loadDependentServiceKeepsItsRanges() throws Exception {
		CommonModel m = base();
		LDStrategy ld = new LDStrategy();
		Object r = ld.addRange();
		r = ld.setRangeFrom(r, 3); // returns the (possibly re-created) range key
		ld.setRangeDistribution(r, new Deterministic());
		ld.setRangeDistributionMean(r, "0.5");
		m.setServiceTimeDistribution(st(m, "S"), cl(m, "C"), ld);

		assertNotNull(param(section(doc(m), "S", "Server"), "LoadDependentStrategy"));
		CommonModel back = roundTrip(m);
		LDStrategy got = (LDStrategy) back.getServiceTimeDistribution(st(back, "S"), cl(back, "C"));
		assertEquals(ld.getRangeNumber(), got.getRangeNumber());
		boolean deterministic = false;
		for (Object k : got.getAllRanges()) {
			deterministic |= got.getRangeDistribution(k) instanceof Deterministic;
		}
		assertTrue(deterministic);
	}

	@Test
	void quantumAndSoftDeadlinesAreWrittenForServersAndDelays() throws Exception {
		CommonModel m = base();
		m.setQuantumSize(st(m, "S"), Double.valueOf(0.5));
		m.setQuantumSwitchoverTime(st(m, "S"), Double.valueOf(0.1));
		m.setClassStationSoftDeadline(st(m, "S"), cl(m, "C"), Double.valueOf(9.0));
		Element node = named(doc(m), "node", "S");
		assertEquals("0.5", node.getElementsByTagName("quantaSize").item(0).getTextContent());
		assertEquals("9.0", node.getElementsByTagName("softDeadline").item(0).getTextContent());
		CommonModel back = roundTrip(m);
		assertEquals(0.5, back.getQuantumSize(st(back, "S")), 1e-12);
		assertEquals(0.1, back.getQuantumSwitchoverTime(st(back, "S")), 1e-12);
		assertEquals(9.0, back.getClassStationSoftDeadline(st(back, "S"), cl(back, "C")), 1e-12);
	}

	@Test
	void delayStationWritesQueueAndDelaySections() throws Exception {
		CommonModel m = base();
		m.setConnected(st(m, "S"), st(m, "Out"), false);
		add(m, "Dl", CommonConstants.STATION_TYPE_DELAY);
		connect(m, "S", "Dl");
		connect(m, "Dl", "Out");
		m.setServiceTimeDistribution(st(m, "Dl"), cl(m, "C"), exp(0.2));
		m.setClassStationSoftDeadline(st(m, "Dl"), cl(m, "C"), Double.valueOf(3.0));
		assertEquals(Arrays.asList("Queue", "Delay", "Router"), Arrays.asList(
				((Element) named(doc(m), "node", "Dl").getElementsByTagName("section").item(0)).getAttribute("className"),
				((Element) named(doc(m), "node", "Dl").getElementsByTagName("section").item(1)).getAttribute("className"),
				((Element) named(doc(m), "node", "Dl").getElementsByTagName("section").item(2)).getAttribute("className")));
		CommonModel back = roundTrip(m);
		assertEquals(0.2, lambdaOf(back.getServiceTimeDistribution(st(back, "Dl"), cl(back, "C"))), 1e-12);
		assertEquals(3.0, back.getClassStationSoftDeadline(st(back, "Dl"), cl(back, "C")), 1e-12);
	}
}
