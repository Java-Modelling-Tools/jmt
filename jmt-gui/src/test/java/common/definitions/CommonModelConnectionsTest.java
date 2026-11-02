package common.definitions;

import static common.definitions.ModelFixtures.addClass;
import static common.definitions.ModelFixtures.addStation;
import static common.definitions.ModelFixtures.link;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import common.CommonConstants;
import common.forkStrategies.OutPath;
import common.forkStrategies.ProbabilitiesFork;
import common.routingStrategies.ProbabilityRouting;
import common.routingStrategies.RandomRouting;

/** Characterization of CommonModel connections and the routing/fork data that follows them. */
class CommonModelConnectionsTest {

	private static final String SRC = CommonConstants.STATION_TYPE_SOURCE;
	private static final String SINK = CommonConstants.STATION_TYPE_SINK;
	private static final String Q = CommonConstants.STATION_TYPE_SERVER;

	private static CommonModel two(String from, String to, Object[] out) {
		CommonModel m = ModelFixtures.emptyModel();
		addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		out[0] = addStation(m, "A", from);
		out[1] = addStation(m, "B", to);
		return m;
	}

	private static boolean connectable(String from, String to) {
		Object[] k = new Object[2];
		CommonModel m = two(from, to, k);
		return m.areConnectable(k[0], k[1]);
	}

	@Test
	void connectabilityRules() {
		// sources are never targets, sinks never sources
		assertFalse(connectable(Q, SRC));
		assertFalse(connectable(SINK, Q));
		// sources, sinks and terminals are not connectable among themselves
		assertFalse(connectable(SRC, SINK));
		assertFalse(connectable(SRC, CommonConstants.STATION_TYPE_TERMINAL));
		// ordinary flow
		assertTrue(connectable(SRC, Q));
		assertTrue(connectable(Q, SINK));
		assertTrue(connectable(Q, CommonConstants.STATION_TYPE_DELAY));
		assertTrue(connectable(CommonConstants.STATION_TYPE_ROUTER, Q));
		// places connect only to transitions, queues and delays; transitions are fed only by places
		assertTrue(connectable(CommonConstants.STATION_TYPE_PLACE, CommonConstants.STATION_TYPE_TRANSITION));
		assertTrue(connectable(CommonConstants.STATION_TYPE_PLACE, Q));
		assertFalse(connectable(CommonConstants.STATION_TYPE_PLACE, CommonConstants.STATION_TYPE_ROUTER));
		assertFalse(connectable(Q, CommonConstants.STATION_TYPE_TRANSITION));
	}

	@Test
	void selfLoopRules() {
		CommonModel m = ModelFixtures.emptyModel();
		addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		Object q = addStation(m, "Q", Q);
		Object d = addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		Object r = addStation(m, "R", CommonConstants.STATION_TYPE_ROUTER);
		Object f = addStation(m, "F", CommonConstants.STATION_TYPE_FORK);
		Object j = addStation(m, "J", CommonConstants.STATION_TYPE_JOIN);
		Object src = addStation(m, "S", SRC);
		assertTrue(m.areConnectable(q, q));
		assertTrue(m.areConnectable(d, d));
		assertFalse(m.areConnectable(r, r));
		assertFalse(m.areConnectable(f, f));
		assertFalse(m.areConnectable(j, j));
		assertFalse(m.areConnectable(src, src));
		assertFalse(m.setConnected(r, r, true));
		assertTrue(m.setConnected(q, q, true));
		assertTrue(m.areConnected(q, q));
		assertEquals(Arrays.asList(q), m.getForwardConnections(q));
		assertEquals(Arrays.asList(q), m.getBackwardConnections(q));
	}

	@Test
	void linkAndUnlink() {
		CommonModel m = ModelFixtures.openOneStation();
		Object src = m.getStationKeysSource().get(0);
		Object q = m.getStationKeysServer().get(0);
		Object out = m.getStationKeysSink().get(0);
		assertEquals(Arrays.asList(q), m.getForwardConnections(src));
		assertEquals(Arrays.asList(src), m.getBackwardConnections(q));
		assertEquals(Arrays.asList(out), m.getForwardConnections(q));
		assertFalse(m.areConnected(q, src)); // direction matters
		m.resetSaveState();
		assertTrue(m.setConnected(src, q, false));
		assertTrue(m.toBeSaved());
		assertFalse(m.areConnected(src, q));
		assertTrue(m.areConnectable(src, q)); // still connectable after unlinking
		assertTrue(m.getForwardConnections(src).isEmpty());
		assertTrue(m.getBackwardConnections(q).isEmpty());
	}

	@Test
	void settingTheCurrentStateOrAnImpossibleLinkReturnsFalseAndChangesNothing() {
		CommonModel m = ModelFixtures.openOneStation();
		Object src = m.getStationKeysSource().get(0);
		Object q = m.getStationKeysServer().get(0);
		Object out = m.getStationKeysSink().get(0);
		m.resetSaveState();
		assertFalse(m.setConnected(src, q, true)); // already connected
		assertFalse(m.setConnected(src, out, false)); // already not connected
		assertFalse(m.setConnected(q, src, true)); // not connectable
		assertFalse(m.setConnected(out, q, true)); // not connectable
		assertFalse(m.setConnected(src, Long.valueOf(999), true)); // unknown station
		assertFalse(m.toBeSaved());
	}

	@Test
	void probabilityRoutingLearnsLinkAdditionsAndRemovals() {
		CommonModel m = ModelFixtures.openOneStation();
		Object c = m.getClassKeys().get(0);
		Object src = m.getStationKeysSource().get(0);
		Object q = m.getStationKeysServer().get(0);
		m.setRoutingStrategy(q, c, new ProbabilityRouting());
		// the strategy was installed after linking: it learns about links added later
		Object d = addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		link(m, q, d);
		Map<Object, Double> v = ((ProbabilityRouting) m.getRoutingStrategy(q, c)).getValues();
		assertTrue(v.containsKey(d));
		m.setConnected(q, d, false);
		assertFalse(((ProbabilityRouting) m.getRoutingStrategy(q, c)).getValues().containsKey(d));
		assertNotNull(m.getRoutingStrategy(src, c));
	}

	@Test
	void probabilityRoutingNormalisesToOneOverCurrentLinks() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object r = addStation(m, "R", CommonConstants.STATION_TYPE_ROUTER);
		Object a = addStation(m, "A", Q);
		Object b = addStation(m, "B", Q);
		link(m, r, a);
		link(m, r, b);
		m.setRoutingStrategy(r, c, new ProbabilityRouting());
		Map<Object, Double> v = new HashMap<Object, Double>();
		v.put(a, 1.0);
		v.put(b, 3.0);
		m.normalizeRoutingProbabilities(r, c, v);
		assertEquals(0.25, v.get(a), 1e-12);
		assertEquals(0.75, v.get(b), 1e-12);
		// all-zero input becomes uniform
		v.clear();
		m.normalizeRoutingProbabilities(r, c, v);
		assertEquals(0.5, v.get(a), 1e-12);
		assertEquals(0.5, v.get(b), 1e-12);
	}

	@Test
	void closedClassNormalisationGivesSinksZeroProbability() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		Object r = addStation(m, "R", CommonConstants.STATION_TYPE_ROUTER);
		Object a = addStation(m, "A", Q);
		Object out = addStation(m, "Out", SINK);
		link(m, r, a);
		link(m, r, out);
		Map<Object, Double> v = new HashMap<Object, Double>();
		v.put(a, 1.0);
		v.put(out, 1.0);
		m.normalizeRoutingProbabilities(r, c, v);
		assertEquals(1.0, v.get(a), 1e-12);
		assertEquals(0.0, v.get(out), 1e-12);
	}

	@Test
	void manageProbabilitiesNormalisesEveryProbabilityRouting() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object r = addStation(m, "R", CommonConstants.STATION_TYPE_ROUTER);
		Object a = addStation(m, "A", Q);
		Object b = addStation(m, "B", Q);
		link(m, r, a);
		link(m, r, b);
		ProbabilityRouting pr = new ProbabilityRouting();
		m.setRoutingStrategy(r, c, pr);
		pr.getValues().put(a, 2.0);
		pr.getValues().put(b, 2.0);
		m.manageProbabilities();
		assertEquals(0.5, pr.getValues().get(a), 1e-12);
		assertEquals(0.5, pr.getValues().get(b), 1e-12);
	}

	@Test
	void forkLinksArePropagatedToForkStrategy() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object c = m.getClassKeys().get(0);
		Object fork = m.getStationKeysFork().get(0);
		Object q1 = m.getStationKeysServer().get(0);
		Object q2 = m.getStationKeysServer().get(1);
		ProbabilitiesFork pf = (ProbabilitiesFork) m.getForkStrategy(fork, c);
		assertEquals(2, pf.getOutDetails().size());
		assertTrue(pf.getOutDetails().containsKey(q1));
		assertTrue(pf.getOutDetails().containsKey(q2));
		m.setConnected(fork, q2, false);
		assertEquals(1, ((ProbabilitiesFork) m.getForkStrategy(fork, c)).getOutDetails().size());
		OutPath p = ((ProbabilitiesFork) m.getForkStrategy(fork, c)).getOutDetails().get(q1);
		assertNotNull(p);
	}

	@Test
	void classAddedAfterLinksKeepsLinksAndGetsDefaultRouting() {
		CommonModel m = ModelFixtures.openOneStation();
		Object src = m.getStationKeysSource().get(0);
		Object q = m.getStationKeysServer().get(0);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		assertTrue(m.areConnected(src, q));
		assertTrue(m.getRoutingStrategy(q, c2) instanceof RandomRouting);
		m.setRoutingStrategy(q, c2, new ProbabilityRouting());
		// pinned quirk: a strategy installed with setRoutingStrategy does not learn the existing links by itself
		assertTrue(((ProbabilityRouting) m.getRoutingStrategy(q, c2)).getValues().isEmpty());
	}

	@Test
	void unlinkingEveryPairLeavesNoConnections() {
		CommonModel m = ModelFixtures.openOneStation();
		for (Object s : m.getStationKeys()) {
			for (Object t : m.getStationKeys()) {
				m.setConnected(s, t, false);
			}
		}
		for (Object s : m.getStationKeys()) {
			assertTrue(m.getForwardConnections(s).isEmpty());
			assertTrue(m.getBackwardConnections(s).isEmpty());
		}
	}
}
