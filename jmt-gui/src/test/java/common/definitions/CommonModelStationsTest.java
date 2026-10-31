package common.definitions;

import static common.definitions.ModelFixtures.addClass;
import static common.definitions.ModelFixtures.addStation;
import static common.definitions.ModelFixtures.link;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import common.CommonConstants;
import common.Defaults;

/** Characterization of CommonModel station management and the per-type key sets. */
class CommonModelStationsTest {

	private static final String[] TYPES = { CommonConstants.STATION_TYPE_SOURCE, CommonConstants.STATION_TYPE_SINK,
			CommonConstants.STATION_TYPE_TERMINAL, CommonConstants.STATION_TYPE_ROUTER, CommonConstants.STATION_TYPE_DELAY,
			CommonConstants.STATION_TYPE_SERVER, CommonConstants.STATION_TYPE_FORK, CommonConstants.STATION_TYPE_JOIN,
			CommonConstants.STATION_TYPE_LOGGER, CommonConstants.STATION_TYPE_CLASSSWITCH, CommonConstants.STATION_TYPE_SEMAPHORE,
			CommonConstants.STATION_TYPE_SCALER, CommonConstants.STATION_TYPE_PLACE, CommonConstants.STATION_TYPE_TRANSITION };

	/** The per-type key set a station of the given type must be listed in (and no other). */
	private static Vector<Object> keysOfType(CommonModel m, String type) {
		switch (type) {
		case CommonConstants.STATION_TYPE_SOURCE: return m.getStationKeysSource();
		case CommonConstants.STATION_TYPE_SINK: return m.getStationKeysSink();
		case CommonConstants.STATION_TYPE_TERMINAL: return m.getStationKeysTerminal();
		case CommonConstants.STATION_TYPE_ROUTER: return m.getStationKeysRouter();
		case CommonConstants.STATION_TYPE_DELAY: return m.getStationKeysDelay();
		case CommonConstants.STATION_TYPE_SERVER: return m.getStationKeysServer();
		case CommonConstants.STATION_TYPE_FORK: return m.getStationKeysFork();
		case CommonConstants.STATION_TYPE_JOIN: return m.getStationKeysJoin();
		case CommonConstants.STATION_TYPE_LOGGER: return m.getStationKeysLogger();
		case CommonConstants.STATION_TYPE_CLASSSWITCH: return m.getStationKeysClassSwitch();
		case CommonConstants.STATION_TYPE_SEMAPHORE: return m.getStationKeysSemaphore();
		case CommonConstants.STATION_TYPE_SCALER: return m.getStationKeysScaler();
		case CommonConstants.STATION_TYPE_PLACE: return m.getStationKeysPlace();
		default: return m.getStationKeysTransition();
		}
	}

	@Test
	void emptyModelHasNoStations() {
		CommonModel m = ModelFixtures.emptyModel();
		assertTrue(m.getStationKeys().isEmpty());
		for (String t : TYPES) {
			assertTrue(keysOfType(m, t).isEmpty(), t);
		}
		assertTrue(m.getStationKeysPreloadable().isEmpty());
		assertTrue(m.getStationKeysNoSourceSink().isEmpty());
		assertTrue(m.getStationKeysRefStation().isEmpty());
		assertTrue(m.getBlockableStationKeys().isEmpty());
		assertTrue(m.getFJKeys().isEmpty());
	}

	@Test
	void unknownStationKeyGivesNeutralValues() {
		CommonModel m = ModelFixtures.emptyModel();
		Object bogus = Long.valueOf(999);
		assertNull(m.getStationName(bogus));
		assertNull(m.getStationType(bogus));
		assertNull(m.getStationByName("nope"));
		m.setStationName(bogus, "x");
		m.setStationType(bogus, CommonConstants.STATION_TYPE_SERVER);
		m.deleteStation(bogus);
		assertTrue(m.getStationKeys().isEmpty());
		assertFalse(m.toBeSaved());
	}

	@Test
	void everyTypeLandsInItsOwnKeySetOnly() {
		for (String type : TYPES) {
			CommonModel m = ModelFixtures.emptyModel();
			Object k = addStation(m, "S", type);
			assertEquals(type, m.getStationType(k));
			assertEquals(Arrays.asList(k), m.getStationKeys(), type);
			for (String other : TYPES) {
				assertEquals(other.equals(type), keysOfType(m, other).contains(k), type + " in " + other);
			}
			assertSame(k, m.getStationByName("S"));
		}
	}

	@Test
	void derivedKeySets() {
		CommonModel m = ModelFixtures.emptyModel();
		Object src = addStation(m, "Src", CommonConstants.STATION_TYPE_SOURCE);
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		Object fork = addStation(m, "F", CommonConstants.STATION_TYPE_FORK);
		Object join = addStation(m, "J", CommonConstants.STATION_TYPE_JOIN);
		Object sc = addStation(m, "Sc", CommonConstants.STATION_TYPE_SCALER);
		Object out = addStation(m, "Out", CommonConstants.STATION_TYPE_SINK);
		assertEquals(Arrays.asList(q, fork, join, sc), m.getStationKeysNoSourceSink());
		// preloadable: not source, sink, terminal, join, semaphore, scaler, transition
		assertEquals(Arrays.asList(q, fork), m.getStationKeysPreloadable());
		// blockable: not source, sink, terminal, fork, join, class switch, semaphore, scaler, transition
		assertEquals(Arrays.asList(q), m.getBlockableStationKeys());
		assertEquals(Arrays.asList(fork, sc), m.getFJKeys());
		// reference station candidates for open classes: sources plus the pseudo-keys of fork/scaler
		assertEquals(Arrays.<Object>asList(src, CommonConstants.STATION_TYPE_FORK, CommonConstants.STATION_TYPE_SCALER), m.getStationKeysRefStation());
		assertFalse(m.getStationKeysRefStation().contains(out));
	}

	@Test
	void duplicateAndReservedStationNamesAreMadeUnique() {
		CommonModel m = ModelFixtures.emptyModel();
		Object a = addStation(m, "Station", CommonConstants.STATION_TYPE_SERVER);
		Object b = addStation(m, "Station", CommonConstants.STATION_TYPE_SERVER);
		Object c = addStation(m, "Fork", CommonConstants.STATION_TYPE_SERVER); // "Fork" is reserved for the reference-station key
		assertEquals("Station", m.getStationName(a));
		assertEquals("Station_1", m.getStationName(b));
		assertEquals("Fork_1", m.getStationName(c));
	}

	@Test
	void renameStation() {
		CommonModel m = ModelFixtures.emptyModel();
		Object a = addStation(m, "A", CommonConstants.STATION_TYPE_SERVER);
		addStation(m, "B", CommonConstants.STATION_TYPE_SERVER);
		m.resetSaveState();
		m.setStationName(a, "");
		m.setStationName(a, "A");
		assertFalse(m.toBeSaved());
		m.setStationName(a, "A2");
		assertEquals("A2", m.getStationName(a));
		assertSame(a, m.getStationByName("A2"));
		assertNull(m.getStationByName("A"));
		assertTrue(m.toBeSaved());
		m.setStationName(a, "B");
		assertEquals("B_1", m.getStationName(a));
	}

	@Test
	void serverDefaultsComeFromDefaults() {
		CommonModel m = ModelFixtures.emptyModel();
		Object k = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		assertEquals(Defaults.getAsInteger("stationCapacity"), m.getStationQueueCapacity(k));
		assertEquals(Defaults.get("stationStationQueueStrategy"), m.getStationQueueStrategy(k));
		assertEquals(Defaults.getAsInteger("stationServers"), m.getStationNumberOfServers(k));
		assertEquals(Defaults.getAsInteger("stationRunningJobs"), m.getStationMaxRunningJobs(k));
		assertEquals(Integer.valueOf(m.getStationQueueCapacity(k) + m.getStationMaxRunningJobs(k)), m.getStationTotalCapacity(k));
		assertEquals(Boolean.FALSE, m.getHeterogeneousServersEnabled(k));
		assertEquals(Boolean.FALSE, m.getSwitchoverTimesEnabled(k));
		assertEquals(Boolean.FALSE, m.getDelayOffTimesEnabled(k));
		assertEquals(Integer.valueOf(1), m.getStationPollingServerKValue(k));
		assertEquals(CommonConstants.STATION_QUEUE_STRATEGY_POLLING_LIMITED, m.getStationPollingServerType(k));
		assertEquals(CommonConstants.STATION_SCHEDULING_POLICY_ALIS, m.getStationSchedulingPolicy(k));
		assertEquals(1, m.getTransitionModeListSize(k));
		assertEquals(Defaults.get("transitionModeName") + 1, m.getTransitionModeName(k, 0));
	}

	@Test
	void delayRouterLoggerDefaultsAreInfiniteNonPreemptive() {
		CommonModel m = ModelFixtures.emptyModel();
		Object d = addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		Object r = addStation(m, "R", CommonConstants.STATION_TYPE_ROUTER);
		Object l = addStation(m, "L", CommonConstants.STATION_TYPE_LOGGER);
		for (Object k : new Object[] { d, r, l }) {
			assertEquals(Integer.valueOf(-1), m.getStationQueueCapacity(k));
			assertEquals(CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE, m.getStationQueueStrategy(k));
		}
		assertEquals(Integer.valueOf(-1), m.getStationNumberOfServers(d));
		assertNotNull(m.getLoggingParameters(l));
		assertNull(m.getLoggingParameters(d));
	}

	@Test
	void forkAndScalerDefaultsComeFromDefaults() {
		CommonModel m = ModelFixtures.emptyModel();
		Object f = addStation(m, "F", CommonConstants.STATION_TYPE_FORK);
		Object s = addStation(m, "Sc", CommonConstants.STATION_TYPE_SCALER);
		assertEquals(Defaults.getAsInteger("forkBlock"), m.getForkBlock(f));
		assertEquals(Integer.valueOf(-1), m.getForkBlock(s));
		assertEquals(Boolean.valueOf(Defaults.get("isSimplifiedFork")), m.getIsSimplifiedFork(f));
		assertEquals(Defaults.getAsInteger("forkJobsPerLink"), m.getStationNumberOfServers(f));
		assertEquals(Defaults.getAsInteger("forkJobsPerLink"), m.getStationNumberOfServers(s));
	}

	@Test
	void placeDefaultsComeFromDefaults() {
		CommonModel m = ModelFixtures.emptyModel();
		Object p = addStation(m, "P", CommonConstants.STATION_TYPE_PLACE);
		addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		assertEquals(Defaults.getAsInteger("placeCapacity"), m.getStationQueueCapacity(p));
		Object c = m.getClassKeys().get(0);
		assertEquals(Defaults.getAsInteger("placeQueueCapacity"), m.getQueueCapacity(p, c));
		assertEquals(Defaults.get("placeQueueStrategy"), m.getQueueStrategy(p, c));
		assertEquals(Defaults.get("placeDropRule"), m.getDropRule(p, c));
	}

	@Test
	void stationSettersRoundTrip() {
		CommonModel m = ModelFixtures.emptyModel();
		Object k = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		m.setStationQueueCapacity(k, Integer.valueOf(10));
		m.setStationNumberOfServers(k, Integer.valueOf(3));
		m.setStationMaxRunningJobs(k, Integer.valueOf(4));
		m.setStationTotalCapacity(k, Integer.valueOf(20));
		m.setStationQueueStrategy(k, CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE);
		m.setQuantumSize(k, Double.valueOf(0.5));
		m.setQuantumSwitchoverTime(k, Double.valueOf(0.1));
		m.setStationPollingServerKValue(k, Integer.valueOf(2));
		m.setSwitchoverTimesEnabled(k, Boolean.TRUE);
		m.setDelayOffTimesEnabled(k, Boolean.TRUE);
		assertEquals(Integer.valueOf(10), m.getStationQueueCapacity(k));
		assertEquals(Integer.valueOf(3), m.getStationNumberOfServers(k));
		assertEquals(Integer.valueOf(4), m.getStationMaxRunningJobs(k));
		assertEquals(Integer.valueOf(20), m.getStationTotalCapacity(k));
		assertEquals(CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE, m.getStationQueueStrategy(k));
		assertEquals(Double.valueOf(0.5), m.getQuantumSize(k));
		assertEquals(Double.valueOf(0.1), m.getQuantumSwitchoverTime(k));
		assertEquals(Integer.valueOf(2), m.getStationPollingServerKValue(k));
		assertEquals(Boolean.TRUE, m.getSwitchoverTimesEnabled(k));
		assertEquals(Boolean.TRUE, m.getDelayOffTimesEnabled(k));
	}

	@Test
	void stationAddedAfterClassesGetsPerClassParameters() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c1 = addClass(m, "C1", CommonConstants.CLASS_TYPE_OPEN);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_CLOSED);
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		for (Object c : new Object[] { c1, c2 }) {
			assertEquals(Defaults.get("stationQueueStrategy"), m.getQueueStrategy(q, c));
			assertEquals(Defaults.get("dropRule"), m.getDropRule(q, c));
			assertEquals(Defaults.getAsDouble("serviceWeight"), m.getServiceWeight(q, c));
			assertEquals(Defaults.getAsDouble("classSoftDeadline"), m.getClassStationSoftDeadline(q, c));
			assertNotNull(m.getServiceTimeDistribution(q, c));
			assertTrue(m.getRoutingStrategy(q, c) instanceof common.routingStrategies.RandomRouting);
			assertEquals(Integer.valueOf(0), m.getPreloadedJobs(q, c));
		}
	}

	@Test
	void sourceAndSinkHaveNoServiceParameters() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object src = addStation(m, "Src", CommonConstants.STATION_TYPE_SOURCE);
		Object out = addStation(m, "Out", CommonConstants.STATION_TYPE_SINK);
		assertNull(m.getServiceTimeDistribution(src, c));
		assertNull(m.getQueueStrategy(src, c));
		assertNotNull(m.getRoutingStrategy(src, c));
		assertNull(m.getRoutingStrategy(out, c));
	}

	@Test
	void deleteStationRemovesMeasuresLinksAndPerClassParameters() {
		CommonModel m = ModelFixtures.openOneStation();
		Object c = m.getClassKeys().get(0);
		Object src = m.getStationKeysSource().get(0);
		Object q = m.getStationKeysServer().get(0);
		Object out = m.getStationKeysSink().get(0);
		assertEquals(1, m.getMeasureKeys().size());
		m.deleteStation(q);
		assertEquals(Arrays.asList(src, out), m.getStationKeys());
		assertTrue(m.getStationKeysServer().isEmpty());
		assertTrue(m.getStationKeysPreloadable().isEmpty());
		assertTrue(m.getBlockableStationKeys().isEmpty());
		assertTrue(m.getMeasureKeys().isEmpty());
		assertNull(m.getStationName(q));
		assertNull(m.getServiceTimeDistribution(q, c));
		assertNull(m.getRoutingStrategy(q, c));
		assertTrue(m.getForwardConnections(src).isEmpty());
		assertTrue(m.getBackwardConnections(out).isEmpty());
		assertFalse(m.areConnected(src, q));
		assertFalse(m.areConnectable(src, q));
	}

	@Test
	void deleteReferenceStationClearsClassReference() {
		CommonModel m = ModelFixtures.openOneStation();
		Object c = m.getClassKeys().get(0);
		assertNotNull(m.getClassRefStation(c));
		m.deleteStation(m.getStationKeysSource().get(0));
		assertNull(m.getClassRefStation(c));
		assertEquals(1, m.getClassKeys().size());
	}

	@Test
	void deleteLastStationLeavesEmptyStationSet() {
		CommonModel m = ModelFixtures.emptyModel();
		addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		m.deleteStation(q);
		assertTrue(m.getStationKeys().isEmpty());
		assertEquals(1, m.getClassKeys().size());
	}

	@Test
	void deleteStationInBlockingRegionEmptiesRegionMembership() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		Object region = m.getRegionKeys().get(0);
		Object q = m.getStationKeysServer().get(0);
		m.deleteStation(q);
		assertTrue(m.getBlockingRegionStations(region).isEmpty());
		assertNull(m.getStationBlockingRegion(q));
		assertEquals(1, m.getRegionKeys().size());
	}

	@Test
	void changeStationTypeKeepsKeyAndNameButDropsLinksAndMeasures() {
		CommonModel m = ModelFixtures.openOneStation();
		Object src = m.getStationKeysSource().get(0);
		Object q = m.getStationKeysServer().get(0);
		m.setStationType(q, CommonConstants.STATION_TYPE_DELAY);
		assertEquals(CommonConstants.STATION_TYPE_DELAY, m.getStationType(q));
		assertEquals("Q", m.getStationName(q));
		assertTrue(m.getStationKeysServer().isEmpty());
		assertEquals(Arrays.asList(q), m.getStationKeysDelay());
		assertTrue(m.getMeasureKeys().isEmpty());
		assertFalse(m.areConnected(src, q));
		assertEquals(Integer.valueOf(-1), m.getStationNumberOfServers(q));
		assertEquals(3, m.getStationKeys().size());
	}

	@Test
	void changeStationTypeToSameTypeIsNoop() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		m.resetSaveState();
		m.setStationType(q, CommonConstants.STATION_TYPE_SERVER);
		assertFalse(m.toBeSaved());
		assertEquals(1, m.getMeasureKeys().size());
	}

	@Test
	void changeStationTypeOutOfBlockingRegionTypeEvictsStation() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		Object region = m.getRegionKeys().get(0);
		Object q = m.getStationKeysServer().get(0);
		m.setStationType(q, CommonConstants.STATION_TYPE_FORK);
		assertTrue(m.getBlockingRegionStations(region).isEmpty());
		assertFalse(m.getBlockableStationKeys().contains(q));
	}

	@Test
	void singleStationModel() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		Object d = addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		m.setClassRefStation(c, d);
		link(m, d, d);
		List<Object> self = Arrays.asList(d);
		assertEquals(self, m.getForwardConnections(d));
		assertEquals(self, m.getBackwardConnections(d));
	}

	@Test
	void serverTypesOfHeterogeneousStations() {
		CommonModel m = ModelFixtures.emptyModel();
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		assertEquals(0, m.getServerTypes(q).size());
		assertEquals(0, m.getNumberOfDifferentServerTypes(q));
	}
}
