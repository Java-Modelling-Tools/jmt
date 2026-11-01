package common.definitions;

import static common.definitions.ModelFixtures.addClass;
import static common.definitions.ModelFixtures.addStation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import NetStrategies.ImpatienceStrategies.ImpatienceType;
import common.CommonConstants;
import common.Defaults;
import common.distributions.Deterministic;
import common.distributions.Exponential;
import common.forkStrategies.ClassSwitchFork;
import common.forkStrategies.CombFork;
import common.forkStrategies.OutPath;
import common.forkStrategies.ProbabilitiesFork;
import common.joinStrategies.GuardJoin;
import common.joinStrategies.JoinStrategy;
import common.joinStrategies.NormalJoin;
import common.joinStrategies.PartialJoin;
import common.routingStrategies.RandomRouting;
import common.semaphoreStrategies.NormalSemaphore;
import common.serviceStrategies.ZeroStrategy;

/** Characterization of the per-section parameters of CommonModel: queue, service, delay, fork, join, semaphore, logger, class switch. */
class CommonModelSectionsTest {

	// ---- queue / service / delay ----

	@Test
	void queueSectionPerClassSettersRoundTrip() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		m.setQueueCapacity(q, c, Integer.valueOf(8));
		m.setQueueStrategy(q, c, CommonConstants.QUEUE_STRATEGY_LCFS);
		m.setDropRule(q, c, CommonConstants.FINITE_DROP);
		m.setClassStationSoftDeadline(q, c, Double.valueOf(3));
		assertEquals(Integer.valueOf(8), m.getQueueCapacity(q, c));
		assertEquals(CommonConstants.QUEUE_STRATEGY_LCFS, m.getQueueStrategy(q, c));
		assertEquals(CommonConstants.FINITE_DROP, m.getDropRule(q, c));
		assertEquals(Double.valueOf(3), m.getClassStationSoftDeadline(q, c));
	}

	@Test
	void serviceSectionPerClassSettersRoundTrip() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		Deterministic d = new Deterministic();
		m.setServiceTimeDistribution(q, c, d);
		m.setServiceWeight(q, c, Double.valueOf(2));
		m.setServerNumRequired(q, c, Integer.valueOf(2));
		assertSame(d, m.getServiceTimeDistribution(q, c));
		assertEquals(Double.valueOf(2), m.getServiceWeight(q, c));
		assertEquals(Integer.valueOf(2), m.getServerNumRequired(q, c));
	}

	@Test
	void serverNumRequiredIsUnsetUntilSetAndNullMeansOne() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		// pinned quirk: unset until first set; null means 1 on set (harmless; a later stage may default it to 1)
		assertNull(m.getServerNumRequired(q, c));
		m.setServerNumRequired(q, c, null);
		assertEquals(Integer.valueOf(1), m.getServerNumRequired(q, c));
	}

	@Test
	void queueDefaultsForQueueAreCapturedPerClass() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		assertEquals(Defaults.get("stationQueueStrategy"), m.getQueueStrategy(q, c));
		assertEquals(Defaults.get("dropRule"), m.getDropRule(q, c));
		assertEquals(ImpatienceType.NONE, m.getImpatienceType(q, c));
		assertNotNull(m.getRetrialDistribution(q, c));
	}

	@Test
	void impatienceTypeCanBeChangedAndReset() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		m.setImpatienceType(q, c, ImpatienceType.RENEGING);
		assertEquals(ImpatienceType.RENEGING, m.getImpatienceType(q, c));
		m.resetImpatience(q, c);
		// pinned quirk: reset clears the type to null rather than restoring NONE (a later stage may legitimately change it)
		assertNull(m.getImpatienceType(q, c));
		assertNull(m.getImpatienceParameter(q, c));
	}

	@Test
	void retrialAndSwitchoverDistributionsRoundTrip() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c1 = addClass(m, "C1", CommonConstants.CLASS_TYPE_OPEN);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		Deterministic r = new Deterministic();
		m.setRetrialDistribution(q, c1, r);
		assertSame(r, m.getRetrialDistribution(q, c1));
		Deterministic sw = new Deterministic();
		m.setSwitchoverTimeDistribution(q, c1, c2, sw);
		assertSame(sw, m.getSwitchoverTimeDistribution(q, c1, c2));
		Deterministic off = new Deterministic();
		m.setDelayOffTimeDistribution(q, c1, off);
		assertSame(off, m.getDelayOffTimeDistribution(q, c1));
		Deterministic up = new Deterministic();
		m.setSetupTimeDistribution(q, c1, up);
		assertSame(up, m.getSetupTimeDistribution(q, c1));
	}

	@Test
	void deletingAClassKeepsSwitchoverEntriesOfOtherClassesAndForgetsTheDeletedOne() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c1 = addClass(m, "C1", CommonConstants.CLASS_TYPE_OPEN);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		Object c3 = addClass(m, "C3", CommonConstants.CLASS_TYPE_OPEN);
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		Deterministic toC2 = new Deterministic();
		Deterministic toC3 = new Deterministic();
		m.setSwitchoverTimeDistribution(q, c1, c2, toC2);
		m.setSwitchoverTimeDistribution(q, c1, c3, toC3);
		assertSame(toC2, m.getSwitchoverTimeDistribution(q, c1, c2));
		m.deleteClass(c2);
		assertSame(toC3, m.getSwitchoverTimeDistribution(q, c1, c3));
		// pinned quirk: the getter lazily recreates a zero-time entry, so a dropped entry reads as a fresh ZeroStrategy
		// (observable by callers; a later stage may legitimately change it).
		Object after = m.getSwitchoverTimeDistribution(q, c1, c2);
		assertNotSame(toC2, after);
		assertTrue(after instanceof ZeroStrategy);
	}

	@Test
	void delayStationHasDelayServiceDistribution() {
		CommonModel m = ModelFixtures.closedTwoStations();
		Object d = m.getStationKeysDelay().get(0);
		Object c = m.getClassKeys().get(0);
		assertTrue(m.getServiceTimeDistribution(d, c) instanceof Exponential);
		assertEquals(CommonConstants.FINITE_DROP, m.getDropRule(d, c));
		assertEquals(CommonConstants.QUEUE_STRATEGY_FCFS, m.getQueueStrategy(d, c));
	}

	@Test
	void preloadedJobsRoundTripAndTotals() {
		CommonModel m = ModelFixtures.closedTwoStations();
		Object d = m.getStationKeysDelay().get(0);
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		m.setPreloadedJobs(q, c, Integer.valueOf(2));
		m.setPreloadedJobs(d, c, Integer.valueOf(1));
		assertEquals(Integer.valueOf(2), m.getPreloadedJobs(q, c));
		assertEquals(Integer.valueOf(3), m.getPreloadedJobsNumber(c));
	}

	@Test
	void manageJobsAllocatesSparePopulationToReferenceStation() {
		CommonModel m = ModelFixtures.closedTwoStations(); // population 5, reference station D
		Object d = m.getStationKeysDelay().get(0);
		Object c = m.getClassKeys().get(0);
		assertEquals(Integer.valueOf(0), m.getPreloadedJobsNumber(c));
		m.manageJobs();
		assertEquals(Integer.valueOf(5), m.getPreloadedJobs(d, c));
		assertEquals(Integer.valueOf(5), m.getPreloadedJobsNumber(c));
	}

	@Test
	void manageJobsTrimsExcessJobs() {
		CommonModel m = ModelFixtures.closedTwoStations();
		Object d = m.getStationKeysDelay().get(0);
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		m.setPreloadedJobs(d, c, Integer.valueOf(4));
		m.setPreloadedJobs(q, c, Integer.valueOf(4));
		m.manageJobs();
		assertEquals(Integer.valueOf(5), m.getPreloadedJobsNumber(c));
	}

	@Test
	void manageJobsLeavesOpenClassesAlone() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		m.manageJobs();
		assertEquals(Integer.valueOf(0), m.getPreloadedJobs(q, m.getClassKeys().get(0)));
	}

	// ---- fork / join / semaphore ----

	@Test
	void forkStrategyDefaultsToProbabilitiesAndRoundTrips() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object c = m.getClassKeys().get(0);
		Object f = m.getStationKeysFork().get(0);
		assertTrue(m.getForkStrategy(f, c) instanceof ProbabilitiesFork);
		CombFork comb = new CombFork();
		m.setForkStrategy(f, c, comb);
		assertSame(comb, m.getForkStrategy(f, c));
		assertNull(m.getForkStrategy(m.getStationKeysServer().get(0), c));
	}

	@Test
	void forkStrategyEntriesFollowClassSetChanges() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object f = m.getStationKeysFork().get(0);
		Object q1 = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		m.setForkStrategy(f, c, new ClassSwitchFork());
		// pinned quirk: a strategy installed later does not learn existing links; relink to populate it
		m.setConnected(f, q1, false);
		m.setConnected(f, q1, true);
		ClassSwitchFork csf = (ClassSwitchFork) m.getForkStrategy(f, c);
		assertEquals(1, csf.getOutDetails().size());
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		for (OutPath out : csf.getOutDetails().values()) {
			assertTrue(out.getOutParameters().containsKey(c));
			assertTrue(out.getOutParameters().containsKey(c2));
		}
		m.deleteClass(c2);
		for (OutPath out : csf.getOutDetails().values()) {
			assertTrue(out.getOutParameters().containsKey(c));
			assertFalse(out.getOutParameters().containsKey(c2));
		}
	}

	@Test
	void forkParametersRoundTrip() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object f = m.getStationKeysFork().get(0);
		m.setForkBlock(f, Integer.valueOf(3));
		m.setIsSimplifiedFork(f, Boolean.TRUE);
		assertEquals(Integer.valueOf(3), m.getForkBlock(f));
		assertEquals(Boolean.TRUE, m.getIsSimplifiedFork(f));
	}

	@Test
	void joinStrategyDefaultsToNormalAndRoundTrips() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object c = m.getClassKeys().get(0);
		Object j = m.getStationKeysJoin().get(0);
		assertTrue(m.getJoinStrategy(j, c) instanceof NormalJoin);
		PartialJoin pj = new PartialJoin();
		m.setJoinStrategy(j, c, pj);
		assertSame(pj, m.getJoinStrategy(j, c));
		assertNull(m.getJoinStrategy(m.getStationKeysServer().get(0), c));
	}

	@Test
	void guardJoinTracksClassAdditionsAndDeletions() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object c = m.getClassKeys().get(0);
		Object j = m.getStationKeysJoin().get(0);
		m.setJoinStrategy(j, c, new GuardJoin());
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		// the new class is registered in the guard of the existing class' strategy
		GuardJoin g = (GuardJoin) m.getJoinStrategy(j, c);
		assertEquals(Integer.valueOf(0), g.getGuard().get(c2));
		m.deleteClass(c2);
		assertFalse(g.getGuard().containsKey(c2));
	}

	/**
	 * JoinStrategy.equals compares names only, so setJoinStrategy silently ignores a replacement that has the
	 * same name but different parameters (a Quorum join with another required count). Today's callers survive
	 * this: JoinSectionPanel and XMLReader only replace a strategy of a different name and then edit the
	 * installed instance in place, and the default join is "Normal".
	 * pinned quirk: current behavior is pinned here; the @Disabled test below asserts the opposite, so both
	 * tests must be flipped together when the bug is fixed.
	 * The same equals-by-name behavior exists on setRoutingStrategy: JMCH Solver:249-255, JMVA ModelConverter:197-199
	 * and JMVAtoJSIMConvertor:143 install a ProbabilityRouting and then mutate their local copy, which only works
	 * because the default routing is Random (fragile, not a current bug).
	 */
	@Test
	void setJoinStrategyKeepsInstalledStrategyWhenReplacementHasTheSameName() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object c = m.getClassKeys().get(0);
		Object j = m.getStationKeysJoin().get(0);
		PartialJoin first = new PartialJoin();
		first.setRequiredNum(1);
		m.setJoinStrategy(j, c, first);
		PartialJoin second = new PartialJoin();
		second.setRequiredNum(2);
		m.setJoinStrategy(j, c, second);
		assertSame(first, m.getJoinStrategy(j, c));
		assertEquals(1, ((JoinStrategy) m.getJoinStrategy(j, c)).getRequiredNum());
	}

	@Disabled("latent: setJoinStrategy drops a same-name replacement (JoinStrategy.equals compares names only); no current caller replaces a strategy by one of the same name with other parameters")
	@Test
	void setJoinStrategyShouldInstallReplacementWithDifferentParameters() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object c = m.getClassKeys().get(0);
		Object j = m.getStationKeysJoin().get(0);
		PartialJoin first = new PartialJoin();
		first.setRequiredNum(1);
		m.setJoinStrategy(j, c, first);
		PartialJoin second = new PartialJoin();
		second.setRequiredNum(2);
		m.setJoinStrategy(j, c, second);
		assertEquals(2, ((JoinStrategy) m.getJoinStrategy(j, c)).getRequiredNum());
	}

	@Test
	void semaphoreStrategyAndThreshold() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object s = addStation(m, "Sem", CommonConstants.STATION_TYPE_SEMAPHORE);
		assertTrue(m.getSemaphoreStrategy(s, c) instanceof NormalSemaphore);
		assertTrue(m.getRoutingStrategy(s, c) instanceof RandomRouting);
		m.setSemaphoreThreshold(s, c, Integer.valueOf(3));
		assertEquals(Integer.valueOf(3), m.getSemaphoreThreshold(s, c));
		NormalSemaphore replacement = new NormalSemaphore();
		m.setSemaphoreStrategy(s, c, replacement);
		// pinned quirk: same-name replacement is ignored here as well (equals compares names only)
		assertNotSame(replacement, m.getSemaphoreStrategy(s, c));
	}

	// ---- logger, class switch, scaler, heterogeneous servers, transitions ----

	@Test
	void loggerStationParameters() {
		CommonModel m = ModelFixtures.emptyModel();
		Object l = addStation(m, "L", CommonConstants.STATION_TYPE_LOGGER);
		assertNotNull(m.getLoggingParameters(l));
		log.LoggerParameters p = new log.LoggerParameters();
		m.resetSaveState();
		m.setLoggingParameters(l, p);
		assertSame(p, m.getLoggingParameters(l));
		assertTrue(m.toBeSaved());
	}

	@Test
	void globalLoggerParametersRoundTrip() {
		CommonModel m = ModelFixtures.emptyModel();
		m.setLoggingGlbParameter("path", "/tmp/x");
		m.setLoggingGlbParameter("delim", "|");
		m.setLoggingGlbParameter("decimalSeparator", ",");
		m.setLoggingGlbParameter("autoAppend", "2");
		assertEquals("/tmp/x", m.getLoggingGlbParameter("PATH"));
		assertEquals("|", m.getLoggingGlbParameter("delim"));
		assertEquals(",", m.getLoggingGlbParameter("decimalSeparator"));
		assertEquals("2", m.getLoggingGlbParameter("autoAppend"));
		assertNull(m.getLoggingGlbParameter("nonsense"));
	}

	@Test
	void classSwitchMatrixRoundTripAndClassDeletion() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c1 = addClass(m, "C1", CommonConstants.CLASS_TYPE_OPEN);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		Object cs = addStation(m, "CS", CommonConstants.STATION_TYPE_CLASSSWITCH);
		assertEquals(0f, m.getClassSwitchMatrix(cs, c1, c2), 0f);
		m.setClassSwitchMatrix(cs, c1, c2, 0.4f);
		assertEquals(0.4f, m.getClassSwitchMatrix(cs, c1, c2), 0f);
		m.deleteClass(c2);
		assertEquals(0f, m.getClassSwitchMatrix(cs, c1, c2), 0f);
	}

	@Test
	void heterogeneousServerTypesLifecycle() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		m.addServerType(q, 2);
		m.addServerType(q, "Fast", 3, null, false);
		assertEquals(2, m.getNumberOfDifferentServerTypes(q));
		assertEquals(Arrays.asList("Server Type 1", "Fast"), m.getServerTypeNames(q));
		Object fastKey = m.getServerTypeKey(q, "Fast");
		assertTrue(m.isServerTypeKey(fastKey));
		assertEquals(3, m.getServerType(fastKey).getNumOfServers());
		assertTrue(m.getServerType(fastKey).isCompatible(c)); // null compatibilities means "all classes"
		assertEquals("Q - Fast", m.getServerTypeStationName(fastKey));
		assertNull(m.getServerTypeKey(q, "missing"));
		// classes added later become compatible with every server type; deleted ones are removed
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		assertTrue(m.getServerType(fastKey).isCompatible(c2));
		m.deleteClass(c2);
		assertFalse(m.getServerType(fastKey).isCompatible(c2));
		m.deleteServerType(q, 0);
		assertEquals(Collections.singletonList("Fast"), m.getServerTypeNames(q));
		assertEquals(0, m.getInfoForServerType(q, 0).getId()); // ids are renumbered
		// deleting the station forgets its server types
		m.deleteStation(q);
		assertFalse(m.isServerTypeKey(fastKey));
	}

	@Test
	void updateNumOfServersGrowsLastServerTypeOrCreatesOne() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		m.updateNumOfServers(q, 4);
		assertEquals(1, m.getNumberOfDifferentServerTypes(q));
		assertEquals(4, m.getInfoForServerType(q, 0).getNumOfServers());
		m.updateNumOfServers(q, 6);
		assertEquals(6, m.getInfoForServerType(q, 0).getNumOfServers());
		m.updateNumOfServers(q, 2);
		assertEquals(2, m.getInfoForServerType(q, 0).getNumOfServers());
	}

	@Test
	void deletingServerTypeAlsoDropsMeasuresThatReferenceIt() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		m.addServerType(q, "Fast", 3, null, false);
		Object serverKey = m.getServerTypeKey(q, "Fast");
		Object k = m.addMeasure(SimulationDefinition.MEASURE_NS, q, null);
		m.setMeasureServerTypeKey(serverKey, k);
		m.deleteServerType(q, 0);
		assertFalse(m.getMeasureKeys().contains(k));
	}

	@Test
	void vacationPolicyRoundTrip() {
		CommonModel m = ModelFixtures.emptyModel();
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		assertEquals(CommonConstants.STATION_VACATION_POLICY_NONE, m.getStationVacationPolicy(q));
		assertNotNull(m.getStationVacationParameters(q));
		m.setStationVacationPolicy(q, CommonConstants.STATION_VACATION_POLICY_SINGLE);
		assertEquals(CommonConstants.STATION_VACATION_POLICY_SINGLE, m.getStationVacationPolicy(q));
	}

	@Test
	void transitionModesLifecycle() {
		CommonModel m = ModelFixtures.emptyModel();
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		Object p = addStation(m, "P", CommonConstants.STATION_TYPE_PLACE);
		Object t = addStation(m, "T", CommonConstants.STATION_TYPE_TRANSITION);
		ModelFixtures.link(m, p, t);
		ModelFixtures.link(m, t, p);
		String mode1 = Defaults.get("transitionModeName") + 1;
		assertEquals(1, m.getTransitionModeListSize(t));
		assertEquals(Collections.singletonList(mode1), m.getAllTransitionModeNames(t));
		m.addTransitionMode(t, Defaults.get("transitionModeName"));
		assertEquals(2, m.getTransitionModeListSize(t));
		m.addTransitionMode(t, mode1); // name clash is made unique
		assertEquals(3, m.getTransitionModeListSize(t));
		assertEquals(3, new java.util.HashSet<String>(m.getAllTransitionModeNames(t)).size());
		m.setTransitionModeName(t, 0, "Fire");
		assertEquals("Fire", m.getTransitionModeName(t, 0));
		m.setEnablingCondition(t, 0, p, c, Integer.valueOf(2));
		m.setInhibitingCondition(t, 0, p, c, Integer.valueOf(3));
		m.setResourceCondition(t, 0, p, c, Integer.valueOf(1));
		m.setFiringOutcome(t, 0, p, c, Integer.valueOf(4));
		m.setNumberOfServers(t, 0, Integer.valueOf(2));
		m.setFiringPriority(t, 0, Integer.valueOf(7));
		m.setFiringWeight(t, 0, Double.valueOf(0.5));
		Deterministic e = new Deterministic();
		m.setFiringTimeDistribution(t, 0, e);
		assertEquals(Integer.valueOf(2), m.getEnablingCondition(t, 0, p, c));
		assertEquals(Integer.valueOf(3), m.getInhibitingCondition(t, 0, p, c));
		assertEquals(Integer.valueOf(1), m.getResourceCondition(t, 0, p, c));
		assertEquals(Integer.valueOf(4), m.getFiringOutcome(t, 0, p, c));
		assertEquals(Integer.valueOf(2), m.getNumberOfServers(t, 0));
		assertEquals(Integer.valueOf(7), m.getFiringPriority(t, 0));
		assertEquals(Double.valueOf(0.5), m.getFiringWeight(t, 0));
		assertSame(e, m.getFiringTimeDistribution(t, 0));
		// unlinking drops the corresponding rows
		m.setConnected(p, t, false);
		// pinned quirk: a removed row reads as the default 0, not null
		assertEquals(Integer.valueOf(0), m.getEnablingCondition(t, 0, p, c));
		m.deleteTransitionMode(t, 2);
		assertEquals(2, m.getTransitionModeListSize(t));
	}
}
