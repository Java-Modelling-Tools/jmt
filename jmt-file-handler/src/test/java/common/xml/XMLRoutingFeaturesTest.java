package common.xml;

import static common.xml.AdvancedFeatureSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.distributions.Exponential;
import common.routingStrategies.ClassSwitchRouting;
import common.routingStrategies.LoadDependentRouting;
import common.routingStrategies.PowerOfKRouting;
import common.routingStrategies.ProbabilityRouting;
import common.routingStrategies.RoutingStrategy;
import common.routingStrategies.WeightedRoundRobinRouting;

/** Router strategies no bundled example uses, set on S which fans out to S2 and S3 (classes C and D). */
class XMLRoutingFeaturesTest {

	private static CommonModel fanOut() {
		CommonModel m = base();
		Object d = m.addClass("D", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Double.valueOf(0), null, new Exponential());
		m.setClassRefStation(d, st(m, "Src"));
		m.setConnected(st(m, "S"), st(m, "Out"), false);
		add(m, "S2", SERVER);
		add(m, "S3", SERVER);
		connect(m, "S", "S2");
		connect(m, "S", "S3");
		connect(m, "S2", "Out");
		connect(m, "S3", "Out");
		return m;
	}

	/** Sets a fresh strategy of the given kind for class C on S, with both targets registered; returns the stored object. */
	private static RoutingStrategy install(CommonModel m, RoutingStrategy proto) {
		RoutingStrategy rs = proto.clone();
		rs.addStation(st(m, "S2"));
		rs.addStation(st(m, "S3"));
		m.setRoutingStrategy(st(m, "S"), cl(m, "C"), rs);
		return (RoutingStrategy) m.getRoutingStrategy(st(m, "S"), cl(m, "C"));
	}

	private static RoutingStrategy routingOf(CommonModel m) {
		return (RoutingStrategy) m.getRoutingStrategy(st(m, "S"), cl(m, "C"));
	}

	@Test
	void probabilityRoutingKeepsPerTargetProbabilities() throws Exception {
		CommonModel m = fanOut();
		ProbabilityRouting rs = (ProbabilityRouting) install(m, CommonConstants.ROUTING_PROBABILITIES);
		rs.getValues().put(st(m, "S2"), 0.3);
		rs.getValues().put(st(m, "S3"), 0.7);

		Element r = section(doc(m), "S", "Router");
		assertNotNull(param(r, "EmpiricalEntryArray"));
		CommonModel back = roundTrip(m);
		ProbabilityRouting got = (ProbabilityRouting) routingOf(back);
		assertEquals(0.3, got.getValues().get(st(back, "S2")), 1e-12);
		assertEquals(0.7, got.getValues().get(st(back, "S3")), 1e-12);
	}

	@Test
	void weightedRoundRobinKeepsWeights() throws Exception {
		CommonModel m = fanOut();
		WeightedRoundRobinRouting rs = (WeightedRoundRobinRouting) install(m, CommonConstants.ROUTING_WEIGHTED_ROUNDROBIN);
		Map<Object, Integer> w = new HashMap<Object, Integer>();
		w.put(st(m, "S2"), 3);
		w.put(st(m, "S3"), 5);
		rs.setWeights(w);

		assertNotNull(param(section(doc(m), "S", "Router"), "WeightEntryArray"));
		CommonModel back = roundTrip(m);
		WeightedRoundRobinRouting got = (WeightedRoundRobinRouting) routingOf(back);
		assertEquals(3, got.getWeights().get(st(back, "S2")).intValue());
		assertEquals(5, got.getWeights().get(st(back, "S3")).intValue());
	}

	@Test
	void powerOfKKeepsKAndMemoryFlag() throws Exception {
		CommonModel m = fanOut();
		PowerOfKRouting rs = (PowerOfKRouting) install(m, CommonConstants.ROUTING_POWER_OF_K);
		rs.setK(Integer.valueOf(2));
		rs.setWithMemory(Boolean.TRUE);

		Element r = section(doc(m), "S", "Router");
		assertEquals("2", value(r, "k"));
		assertEquals("true", value(r, "withMemory"));
		PowerOfKRouting got = (PowerOfKRouting) routingOf(roundTrip(m));
		assertEquals(Integer.valueOf(2), got.getK());
		assertEquals(Boolean.TRUE, got.isWithMemory());
	}

	@Test
	void loadDependentRoutingKeepsRanges() throws Exception {
		CommonModel m = fanOut();
		LoadDependentRouting rs = (LoadDependentRouting) install(m, CommonConstants.ROUTING_LOADDEPENDENT);
		rs.addEmpiricalEntry(0, st(m, "S2"), 1.0);
		rs.addEmpiricalEntry(0, st(m, "S3"), 0.0);
		rs.addEmpiricalEntry(5, st(m, "S2"), 0.2);
		rs.addEmpiricalEntry(5, st(m, "S3"), 0.8);

		assertNotNull(param(section(doc(m), "S", "Router"), "LoadDependentRoutingParameter"));
		CommonModel back = roundTrip(m);
		LoadDependentRouting got = (LoadDependentRouting) routingOf(back);
		assertEquals(2, got.getAllEmpiricalEntries().size());
		assertEquals(0.8, got.getEmpiricalEntriesForFrom(5).get(st(back, "S3")), 1e-12);
	}

	@Test
	void classSwitchRoutingKeepsTargetAndClassProbabilities() throws Exception {
		CommonModel m = fanOut();
		ClassSwitchRouting rs = (ClassSwitchRouting) install(m, CommonConstants.ROUTING_CLASS_SWITCH);
		rs.getValues().put(st(m, "S2"), 0.6);
		rs.getValues().put(st(m, "S3"), 0.4);
		for (String target : new String[] { "S2", "S3" }) {
			Map<Object, Double> perClass = new HashMap<Object, Double>();
			perClass.put(cl(m, "C"), target.equals("S2") ? 1.0 : 0.25);
			perClass.put(cl(m, "D"), target.equals("S2") ? 0.0 : 0.75);
			rs.getOutPaths().put(st(m, target), perClass);
		}

		assertNotNull(param(section(doc(m), "S", "Router"), "ClassSwitchRoutingParameterArray"));
		CommonModel back = roundTrip(m);
		ClassSwitchRouting got = (ClassSwitchRouting) routingOf(back);
		assertEquals(0.6, got.getValues().get(st(back, "S2")), 1e-12);
		assertEquals(0.75, got.getOutPaths().get(st(back, "S3")).get(cl(back, "D")), 1e-12);
	}

	@ParameterizedTest
	@CsvSource({
			"Round Robin,RoundRobinStrategy",
			"Join the Shortest Queue (JSQ),ShortestQueueLengthRoutingStrategy",
			"Shortest Response Time,ShortestResponseTimeRoutingStrategy",
			"Least Utilization,LeastUtilizationRoutingStrategy",
			"Fastest Service,FastestServiceRoutingStrategy",
			"Disabled,DisabledRoutingStrategy" })
	void parameterlessRoutingStrategiesRoundTrip(String name, String engineSimpleName) throws Exception {
		RoutingStrategy proto = null;
		for (RoutingStrategy rs : new RoutingStrategy[] { CommonConstants.ROUTING_ROUNDROBIN, CommonConstants.ROUTING_SHORTESTQL,
				CommonConstants.ROUTING_SHORTESTRT, CommonConstants.ROUTING_LEASTUTILIZATION, CommonConstants.ROUTING_FASTESTSERVICE,
				CommonConstants.ROUTING_DISABLED }) {
			if (rs.getName().equals(name)) {
				proto = rs;
			}
		}
		assertNotNull(proto, name);
		CommonModel m = fanOut();
		install(m, proto);
		Element r = section(doc(m), "S", "Router");
		assertNotNull(r);
		CommonModel back = roundTrip(m);
		assertInstanceOf(proto.getClass(), routingOf(back));
		assertEquals(name, routingOf(back).getName());
	}
}
