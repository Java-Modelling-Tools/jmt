package common.controller;

import static common.controller.CheckerSupport.firstClass;
import static common.controller.CheckerSupport.firstQueue;
import static common.controller.CheckerSupport.jmva;
import static common.controller.CheckerSupport.normal;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.definitions.ModelFixtures;
import common.definitions.parametric.ParametricAnalysis;
import common.definitions.parametric.ParametricAnalysisModelFactory;
import common.distributions.Deterministic;
import common.distributions.Exponential;
import common.distributions.Pareto;
import common.routingStrategies.ClassSwitchRouting;
import common.routingStrategies.ShortestQueueLengthRouting;
import common.serviceStrategies.LDStrategy;

/** Characterization of the warning checks of ModelChecker (normal and BCMP/JMVA mode). Pins booleans only. */
class ModelCheckerWarningsTest {

	private static CommonModel open() {
		return ModelFixtures.openOneStation();
	}

	private static CommonModel closed() {
		return ModelFixtures.closedTwoStations();
	}

	// ---- normal mode ----

	@Test
	void noOptionalLink() {
		CommonModel m = open();
		assertFalse(normal(m).isThereNoOptionalLinkWarning());
		ModelFixtures.addStation(m, "Orphan", CommonConstants.STATION_TYPE_SERVER);
		assertTrue(normal(m).isThereNoOptionalLinkWarning());
		CommonModel t = open();
		ModelFixtures.addStation(t, "T", CommonConstants.STATION_TYPE_TRANSITION);
		assertFalse(normal(t).isThereNoOptionalLinkWarning(), "transitions are exempt");
	}

	@Test
	void forkWithoutJoin() {
		CommonModel m = open();
		ModelFixtures.addStation(m, "F", CommonConstants.STATION_TYPE_FORK);
		assertTrue(normal(m).isThereForkWithoutJoinWarning());
		assertFalse(normal(ModelFixtures.withForkJoin()).isThereForkWithoutJoinWarning());
		assertFalse(normal(open()).isThereForkWithoutJoinWarning());
	}

	private static CommonModel withParametric(CommonModel m) {
		m.setParametricAnalysisModel(ParametricAnalysisModelFactory.createParametricAnalysisModel(
				ParametricAnalysis.PA_TYPE_NUMBER_OF_CUSTOMERS, m, m, m));
		m.setParametricAnalysisEnabled(true);
		return m;
	}

	@Test
	void parametricAnalysisModelModified() {
		CommonModel m = withParametric(closed());
		assertFalse(normal(m).isThereParametricAnalysisModelModifiedWarning());
		m.setClassPopulation(firstClass(m), Integer.valueOf(9));
		assertTrue(normal(m).isThereParametricAnalysisModelModifiedWarning());
		CommonModel disabled = closed();
		disabled.setParametricAnalysisEnabled(false);
		assertFalse(normal(disabled).isThereParametricAnalysisModelModifiedWarning());
	}

	@Test
	void parametricAnalysisNotAvailable() {
		CommonModel m = withParametric(closed());
		assertFalse(normal(m).isThereParametricAnalysisNotAvailableWarning());
		m.deleteClass(firstClass(m));
		assertTrue(normal(m).isThereParametricAnalysisNotAvailableWarning());
		assertFalse(normal(closed()).isThereParametricAnalysisNotAvailableWarning(), "not enabled");
	}

	@Test
	void zeroPopulation() {
		CommonModel m = closed();
		Object c2 = ModelFixtures.addClass(m, "C2", CommonConstants.CLASS_TYPE_CLOSED);
		m.setClassRefStation(c2, m.getStationKeysDelay().get(0));
		assertFalse(normal(m).isThereZeroPopulationWarning());
		m.setClassPopulation(c2, Integer.valueOf(0));
		assertTrue(normal(m).isThereZeroPopulationWarning());
		ModelFixtures.addStation(m, "CS", CommonConstants.STATION_TYPE_CLASSSWITCH);
		assertFalse(normal(m).isThereZeroPopulationWarning(), "suppressed when a class switch exists");
	}

	@Test
	void zeroPopulationNotRaisedWhenTotalIsZero() {
		CommonModel m = closed();
		m.setClassPopulation(firstClass(m), Integer.valueOf(0));
		assertFalse(normal(m).isThereZeroPopulationWarning());
	}

	@Test
	void zeroTotalPopulation() {
		CommonModel m = closed();
		assertFalse(normal(m).isThereZeroTotalPopulationWarning());
		m.setClassPopulation(firstClass(m), Integer.valueOf(0));
		assertTrue(normal(m).isThereZeroTotalPopulationWarning());
		assertFalse(normal(open()).isThereZeroTotalPopulationWarning(), "no closed class");
	}

	@Disabled("bug: ZERO_TOTAL_POPULATION_WARNING is written to and read from the errors[] array (index 14), so a zero closed population also raises isThereForkJoinRoutingError and clears isErrorFreeNormal")
	@Test
	void zeroTotalPopulationIsOnlyAWarning() {
		CommonModel m = closed();
		m.setClassPopulation(firstClass(m), Integer.valueOf(0));
		ModelChecker c = normal(m);
		assertTrue(c.isThereZeroTotalPopulationWarning());
		assertFalse(c.isThereForkJoinRoutingError());
		assertTrue(c.isErrorFreeNormal());
	}

	@Test
	void classSwitchBetweenForkJoin() {
		assertTrue(normal(CheckerSupport.forkJoinAround(CommonConstants.STATION_TYPE_CLASSSWITCH))
				.isThereClassSwitchBetweenForkJoinWarning());
		CommonModel outside = open();
		ModelFixtures.addStation(outside, "CS", CommonConstants.STATION_TYPE_CLASSSWITCH);
		assertFalse(normal(outside).isThereClassSwitchBetweenForkJoinWarning());
		assertFalse(normal(ModelFixtures.withForkJoin()).isThereClassSwitchBetweenForkJoinWarning());
	}

	@Test
	void classSwitchRoutingBetweenForkJoin() {
		CommonModel m = ModelFixtures.withForkJoin();
		assertFalse(normal(m).isThereClassSwitchRoutingBetweenForkJoinWarning());
		m.setRoutingStrategy(m.getStationKeysServer().get(0), firstClass(m), new ClassSwitchRouting());
		assertTrue(normal(m).isThereClassSwitchRoutingBetweenForkJoinWarning());
		CommonModel outside = open();
		outside.setRoutingStrategy(firstQueue(outside), firstClass(outside), new ClassSwitchRouting());
		assertFalse(normal(outside).isThereClassSwitchRoutingBetweenForkJoinWarning());
	}

	private static CommonModel twoOpenClasses(Integer p1, Integer p2) {
		CommonModel m = open();
		Object c2 = ModelFixtures.addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		m.setClassRefStation(c2, m.getStationKeysSource().get(0));
		m.setClassPriority(firstClass(m), p1);
		m.setClassPriority(c2, p2);
		m.setStationQueueStrategy(firstQueue(m), CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
		return m;
	}

	@Test
	void schedulingSamePriority() {
		assertTrue(normal(twoOpenClasses(1, 1)).isThereSchedulingSamePriorityWarning());
		assertFalse(normal(twoOpenClasses(1, 2)).isThereSchedulingSamePriorityWarning());
		assertFalse(normal(open()).isThereSchedulingSamePriorityWarning(), "single class");
		CommonModel noPriorityStrategy = twoOpenClasses(1, 1);
		noPriorityStrategy.setStationQueueStrategy(firstQueue(noPriorityStrategy),
				CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE);
		assertFalse(normal(noPriorityStrategy).isThereSchedulingSamePriorityWarning());
	}

	/** Src -> Fork -> Place -> Transition -> Place -> Queue -> Join -> Sink. */
	private static CommonModel transitionInsideForkJoin() {
		CommonModel m = ModelFixtures.emptyModel();
		Object cls = ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object src = ModelFixtures.addStation(m, "Src", CommonConstants.STATION_TYPE_SOURCE);
		Object fork = ModelFixtures.addStation(m, "F", CommonConstants.STATION_TYPE_FORK);
		Object p1 = ModelFixtures.addStation(m, "P1", CommonConstants.STATION_TYPE_PLACE);
		Object t = ModelFixtures.addStation(m, "T", CommonConstants.STATION_TYPE_TRANSITION);
		Object p2 = ModelFixtures.addStation(m, "P2", CommonConstants.STATION_TYPE_PLACE);
		Object q = ModelFixtures.addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		Object join = ModelFixtures.addStation(m, "J", CommonConstants.STATION_TYPE_JOIN);
		Object out = ModelFixtures.addStation(m, "Out", CommonConstants.STATION_TYPE_SINK);
		m.setClassRefStation(cls, src);
		ModelFixtures.link(m, src, fork);
		ModelFixtures.link(m, fork, p1);
		ModelFixtures.link(m, p1, t);
		ModelFixtures.link(m, t, p2);
		ModelFixtures.link(m, p2, q);
		ModelFixtures.link(m, q, join);
		ModelFixtures.link(m, join, out);
		return m;
	}

	@Test
	void transitionBetweenForkJoin() {
		assertTrue(normal(transitionInsideForkJoin()).isThereTransitionBetweenForkJoinWarning());
		CommonModel outside = open();
		ModelFixtures.addStation(outside, "T", CommonConstants.STATION_TYPE_TRANSITION);
		assertFalse(normal(outside).isThereTransitionBetweenForkJoinWarning());
	}

	private static CommonModel loneTransition(int servers) {
		CommonModel m = open();
		Object t = ModelFixtures.addStation(m, "T", CommonConstants.STATION_TYPE_TRANSITION);
		m.setNumberOfServers(t, 0, Integer.valueOf(servers));
		return m;
	}

	@Test
	void transitionConstantEnablingDegree() {
		assertTrue(normal(loneTransition(2)).isThereTransitionConstantEnablingDegreeWarning());
		assertFalse(normal(loneTransition(0)).isThereTransitionConstantEnablingDegreeWarning());
		assertFalse(normal(open()).isThereTransitionConstantEnablingDegreeWarning());
	}

	/** Place -> Transition -> Place with the given enabling / inhibiting values on mode 0. */
	private static CommonModel placeTransitionPlace(int enabling, int inhibiting, int firing) {
		CommonModel m = ModelFixtures.emptyModel();
		Object cls = ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		Object p1 = ModelFixtures.addStation(m, "P1", CommonConstants.STATION_TYPE_PLACE);
		Object t = ModelFixtures.addStation(m, "T", CommonConstants.STATION_TYPE_TRANSITION);
		Object p2 = ModelFixtures.addStation(m, "P2", CommonConstants.STATION_TYPE_PLACE);
		m.setClassRefStation(cls, p1);
		ModelFixtures.link(m, p1, t);
		ModelFixtures.link(m, t, p2);
		m.setEnablingCondition(t, 0, p1, cls, Integer.valueOf(enabling));
		m.setInhibitingCondition(t, 0, p1, cls, Integer.valueOf(inhibiting));
		m.setFiringOutcome(t, 0, p2, cls, Integer.valueOf(firing));
		return m;
	}

	@Test
	void transitionInvalidInputCondition() {
		assertTrue(normal(placeTransitionPlace(2, 1, 1)).isThereTransitionInvalidInputConditionWarning());
		assertFalse(normal(placeTransitionPlace(1, 2, 1)).isThereTransitionInvalidInputConditionWarning());
		assertFalse(normal(placeTransitionPlace(1, 0, 1)).isThereTransitionInvalidInputConditionWarning(),
				"zero inhibiting value means no inhibitor");
	}

	@Test
	void transitionNoFiringOutcome() {
		assertTrue(normal(placeTransitionPlace(1, 0, 0)).isThereTransitionNoFiringOutcomeWarning());
		assertFalse(normal(placeTransitionPlace(1, 0, 1)).isThereTransitionNoFiringOutcomeWarning());
		assertTrue(normal(loneTransition(1)).isThereTransitionNoFiringOutcomeWarning(), "no outgoing link");
		assertFalse(normal(open()).isThereTransitionNoFiringOutcomeWarning());
	}

	// ---- BCMP warnings (JMVA mode) ----

	private static CommonModel twoClosedClassesOnQueue() {
		CommonModel m = closed();
		Object c2 = ModelFixtures.addClass(m, "C2", CommonConstants.CLASS_TYPE_CLOSED);
		m.setClassRefStation(c2, m.getStationKeysDelay().get(0));
		return m;
	}

	private static void setAll(CommonModel m, Object station, String strategy) {
		for (Object c : m.getClassKeys()) {
			m.setQueueStrategy(station, c, strategy);
		}
	}

	@Test
	void bcmpDifferentQueueStrategies() {
		CommonModel m = twoClosedClassesOnQueue();
		setAll(m, firstQueue(m), CommonConstants.QUEUE_STRATEGY_FCFS);
		assertFalse(jmva(m).isThereBcmpDifferentQueueStrategiesWarning());
		m.setQueueStrategy(firstQueue(m), m.getClassKeys().get(1), CommonConstants.QUEUE_STRATEGY_PS);
		assertTrue(jmva(m).isThereBcmpDifferentQueueStrategiesWarning());
	}

	@Test
	void bcmpFcfsDifferentServiceStrategies() {
		CommonModel m = twoClosedClassesOnQueue();
		setAll(m, firstQueue(m), CommonConstants.QUEUE_STRATEGY_FCFS);
		assertFalse(jmva(m).isThereBcmpDifferentServiceStrategiesWarning());
		m.setServiceTimeDistribution(firstQueue(m), m.getClassKeys().get(1), new LDStrategy());
		assertTrue(jmva(m).isThereBcmpDifferentServiceStrategiesWarning());
		setAll(m, firstQueue(m), CommonConstants.QUEUE_STRATEGY_PS);
		assertFalse(jmva(m).isThereBcmpDifferentServiceStrategiesWarning(), "only FCFS servers are checked");
	}

	@Test
	void bcmpFcfsNonExponentialDistribution() {
		CommonModel m = closed();
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_FCFS);
		m.setServiceTimeDistribution(firstQueue(m), firstClass(m), new Exponential());
		assertFalse(jmva(m).isThereBcmpFcfsNonExponentialDistributionWarning());
		m.setServiceTimeDistribution(firstQueue(m), firstClass(m), new Deterministic());
		assertTrue(jmva(m).isThereBcmpFcfsNonExponentialDistributionWarning());
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_PS);
		assertFalse(jmva(m).isThereBcmpFcfsNonExponentialDistributionWarning(), "PS is not FCFS");
	}

	@Test
	void bcmpFcfsDifferentServiceTimes() {
		CommonModel m = twoClosedClassesOnQueue();
		setAll(m, firstQueue(m), CommonConstants.QUEUE_STRATEGY_FCFS);
		Exponential e1 = new Exponential();
		Exponential e2 = new Exponential();
		e1.setMean(1.0);
		e2.setMean(1.0);
		m.setServiceTimeDistribution(firstQueue(m), m.getClassKeys().get(0), e1);
		m.setServiceTimeDistribution(firstQueue(m), m.getClassKeys().get(1), e2);
		assertFalse(jmva(m).isThereBcmpFcfsDifferentServiceTimesWarning());
		Exponential e3 = new Exponential();
		e3.setMean(3.0);
		m.setServiceTimeDistribution(firstQueue(m), m.getClassKeys().get(1), e3);
		assertTrue(jmva(m).isThereBcmpFcfsDifferentServiceTimesWarning());
	}

	@Test
	void bcmpPsNonRationalDistribution() {
		CommonModel m = closed();
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_PS);
		assertFalse(jmva(m).isThereBcmpPsNonRationalDistributionWarning());
		m.setServiceTimeDistribution(firstQueue(m), firstClass(m), new Pareto());
		assertTrue(jmva(m).isThereBcmpPsNonRationalDistributionWarning());
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_FCFS);
		assertFalse(jmva(m).isThereBcmpPsNonRationalDistributionWarning(), "FCFS is not PS");
	}

	@Test
	void bcmpLcfsPrNonRationalDistribution() {
		CommonModel m = closed();
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_LCFS_PR);
		assertFalse(jmva(m).isThereBcmpLcfsPrNonRationalDistributionWarning());
		m.setServiceTimeDistribution(firstQueue(m), firstClass(m), new Pareto());
		assertTrue(jmva(m).isThereBcmpLcfsPrNonRationalDistributionWarning());
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_FCFS);
		assertFalse(jmva(m).isThereBcmpLcfsPrNonRationalDistributionWarning());
	}

	@Test
	void bcmpDelayNonRationalDistribution() {
		CommonModel m = closed();
		Object delay = m.getStationKeysDelay().get(0);
		assertFalse(jmva(m).isThereBcmpDelayNonRationalDistributionWarning());
		m.setServiceTimeDistribution(delay, firstClass(m), new Pareto());
		assertTrue(jmva(m).isThereBcmpDelayNonRationalDistributionWarning());
	}

	@Test
	void bcmpStateDependentRoutingStrategy() {
		CommonModel m = closed();
		assertFalse(jmva(m).isThereBcmpStateDependentRoutingStrategyWarning());
		m.setRoutingStrategy(firstQueue(m), firstClass(m), new ShortestQueueLengthRouting());
		assertTrue(jmva(m).isThereBcmpStateDependentRoutingStrategyWarning());
	}

	@Test
	void bcmpAsymmetricSchedulingPolicy() {
		CommonModel m = closed();
		for (String ok : new String[] { CommonConstants.QUEUE_STRATEGY_FCFS, CommonConstants.QUEUE_STRATEGY_PS,
				CommonConstants.QUEUE_STRATEGY_RAND }) {
			m.setQueueStrategy(firstQueue(m), firstClass(m), ok);
			assertFalse(jmva(m).isThereBcmpAsymmetricSchedulingPolicyWarning(), ok);
		}
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_LCFS);
		assertTrue(jmva(m).isThereBcmpAsymmetricSchedulingPolicyWarning());
	}

	@Test
	void bcmpPrioritySchedulingPolicy() {
		CommonModel m = closed();
		assertFalse(jmva(m).isThereBcmpPrioritySchedulingPolicyWarning());
		m.setStationQueueStrategy(firstQueue(m), CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
		assertTrue(jmva(m).isThereBcmpPrioritySchedulingPolicyWarning());
	}

	@Test
	void bcmpWarningsAreOnlyComputedInJmvaMode() {
		CommonModel m = closed();
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_LCFS);
		assertTrue(jmva(m).isThereBcmpAsymmetricSchedulingPolicyWarning());
		assertFalse(normal(m).isThereBcmpAsymmetricSchedulingPolicyWarning());
	}
}
