package common.controller;

import static common.controller.CheckerSupport.firstClass;
import static common.controller.CheckerSupport.firstQueue;
import static common.controller.CheckerSupport.normal;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import NetStrategies.ImpatienceStrategies.ImpatienceType;
import common.CommonConstants;
import common.definitions.CommonModel;
import common.definitions.ModelFixtures;
import common.definitions.SimulationDefinition;
import common.joinStrategies.GuardJoin;
import common.serviceStrategies.LDStrategy;

/** Characterization of the error checks of ModelChecker in normal (non-JMVA) mode. Pins booleans only. */
class ModelCheckerErrorsTest {

	private static CommonModel open() {
		return ModelFixtures.openOneStation();
	}

	@Test
	void noClass() {
		assertTrue(normal(ModelFixtures.emptyModel()).isThereNoClassError());
		assertFalse(normal(open()).isThereNoClassError());
	}

	@Test
	void noStation() {
		assertTrue(normal(ModelFixtures.emptyModel()).isThereNoStationError());
		CommonModel onlySourceSink = ModelFixtures.emptyModel();
		ModelFixtures.addStation(onlySourceSink, "S", CommonConstants.STATION_TYPE_SOURCE);
		assertTrue(normal(onlySourceSink).isThereNoStationError(), "sources and sinks do not count");
		assertFalse(normal(open()).isThereNoStationError());
	}

	@Test
	void noMeasure() {
		CommonModel m = open();
		assertFalse(normal(m).isThereNoMeasureError());
		m.removeMeasure(m.getMeasureKeys().get(0));
		assertTrue(normal(m).isThereNoMeasureError());
	}

	@Test
	void openClassButNoSource() {
		CommonModel m = ModelFixtures.emptyModel();
		ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		ModelFixtures.addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		assertTrue(normal(m).isThereOpenClassButNoSourceError());
		assertFalse(normal(open()).isThereOpenClassButNoSourceError());
		assertFalse(normal(ModelFixtures.closedTwoStations()).isThereOpenClassButNoSourceError());
	}

	@Test
	void openClassButNoSinkWithoutAnyServer() {
		CommonModel m = ModelFixtures.emptyModel();
		ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		ModelFixtures.addStation(m, "S", CommonConstants.STATION_TYPE_SOURCE);
		assertTrue(normal(m).isThereOpenClassButNoSinkError());
		assertFalse(normal(open()).isThereOpenClassButNoSinkError());
		assertFalse(normal(ModelFixtures.closedTwoStations()).isThereOpenClassButNoSinkError());
	}

	@Disabled("bug: a stray ';' after the 'if' in checkForOpenClassButNoSinkError makes serverWorkingAsTransition true whenever any server exists, so an open model with a queue but no sink is never flagged")
	@Test
	void openClassButNoSinkWithServer() {
		CommonModel m = ModelFixtures.emptyModel();
		Object cls = ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object src = ModelFixtures.addStation(m, "S", CommonConstants.STATION_TYPE_SOURCE);
		Object q = ModelFixtures.addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		m.setClassRefStation(cls, src);
		ModelFixtures.link(m, src, q);
		assertTrue(normal(m).isThereOpenClassButNoSinkError());
	}

	@Test
	void sourceWithoutOpenClass() {
		CommonModel m = open();
		assertFalse(normal(m).isThereSourceWithoutOpenClassError());
		ModelFixtures.addStation(m, "Src2", CommonConstants.STATION_TYPE_SOURCE);
		assertTrue(normal(m).isThereSourceWithoutOpenClassError());
	}

	@Test
	void sinkButNoOpenClass() {
		CommonModel m = ModelFixtures.closedTwoStations();
		assertFalse(normal(m).isThereSinkButNoOpenClassError());
		ModelFixtures.addStation(m, "Out", CommonConstants.STATION_TYPE_SINK);
		assertTrue(normal(m).isThereSinkButNoOpenClassError());
		assertFalse(normal(open()).isThereSinkButNoOpenClassError());
	}

	@Test
	void noReferenceStation() {
		CommonModel m = open();
		assertFalse(normal(m).isThereNoReferenceStationError());
		ModelFixtures.addClass(m, "NoRef", CommonConstants.CLASS_TYPE_OPEN);
		assertTrue(normal(m).isThereNoReferenceStationError());
	}

	@Test
	void noEssentialLink() {
		CommonModel m = open();
		assertFalse(normal(m).isThereNoEssentialLinkError());
		ModelFixtures.addStation(m, "Dangling", CommonConstants.STATION_TYPE_SERVER);
		assertTrue(normal(m).isThereNoEssentialLinkError(), "queue without outgoing link");
		CommonModel sink = open();
		ModelFixtures.addStation(sink, "Out2", CommonConstants.STATION_TYPE_SINK);
		assertTrue(normal(sink).isThereNoEssentialLinkError(), "sink without incoming link");
		CommonModel t = open();
		ModelFixtures.addStation(t, "T", CommonConstants.STATION_TYPE_TRANSITION);
		assertTrue(normal(t).isThereNoEssentialLinkError(), "transition with no link at all");
	}

	@Test
	void invalidMeasure() {
		assertFalse(normal(open()).isThereInvalidMeasureError());
		CommonModel m = open();
		m.addMeasure(SimulationDefinition.MEASURE_QL, null, firstClass(m));
		assertTrue(normal(m).isThereInvalidMeasureError(), "non-global measure without station");
	}

	@Test
	void redundantMeasure() {
		CommonModel m = open();
		assertFalse(normal(m).isThereRedundantMeasureError());
		m.addMeasure(SimulationDefinition.MEASURE_QL, firstQueue(m), firstClass(m));
		assertTrue(normal(m).isThereRedundantMeasureError());
		m.addMeasure(SimulationDefinition.MEASURE_U, firstQueue(m), firstClass(m));
		assertTrue(normal(m).isThereRedundantMeasureError(), "still one duplicate");
	}

	@Test
	void inconsistentQueueStrategy() {
		CommonModel m = ModelFixtures.closedTwoStations();
		ModelFixtures.addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		assertFalse(normal(m).isThereInconsistentQueueStrategyError());
		Object c2 = m.getClassKeys().get(1);
		String other = CommonConstants.QUEUE_STRATEGY_FCFS.equals(m.getQueueStrategy(firstQueue(m), c2))
				? CommonConstants.QUEUE_STRATEGY_LCFS : CommonConstants.QUEUE_STRATEGY_FCFS;
		m.setQueueStrategy(firstQueue(m), c2, other);
		assertTrue(normal(m).isThereInconsistentQueueStrategyError());
	}

	private static CommonModel twoClassQueue(String strategyOfSecond) {
		CommonModel m = ModelFixtures.closedTwoStations();
		Object c2 = ModelFixtures.addClass(m, "C2", CommonConstants.CLASS_TYPE_CLOSED);
		m.setClassRefStation(c2, m.getStationKeysDelay().get(0));
		m.setQueueStrategy(firstQueue(m), c2, strategyOfSecond);
		return m;
	}

	@Test
	void unpredictableService() {
		// the first class is never inspected (loop starts at 1), so the second class carries the pair
		CommonModel m = twoClassQueue(CommonConstants.QUEUE_STRATEGY_SJF);
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_SJF);
		Object c2 = m.getClassKeys().get(1);
		assertFalse(normal(m).isThereUnpredictableServiceError(), "SJF without LD service");
		m.setServiceTimeDistribution(firstQueue(m), c2, new LDStrategy());
		assertTrue(normal(m).isThereUnpredictableServiceError());
		m.setQueueStrategy(firstQueue(m), c2, CommonConstants.QUEUE_STRATEGY_FCFS);
		assertFalse(normal(m).isThereUnpredictableServiceError(), "LD service with FCFS");
	}

	@Disabled("bug: checkForUnpredictableServiceError starts its class loop at index 1, so the first class (the only one in a single-class model) is never inspected")
	@Test
	void unpredictableServiceFirstClass() {
		CommonModel m = ModelFixtures.closedTwoStations();
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_SJF);
		m.setServiceTimeDistribution(firstQueue(m), firstClass(m), new LDStrategy());
		assertTrue(normal(m).isThereUnpredictableServiceError());
	}

	@Test
	void joinWithoutFork() {
		CommonModel m = open();
		ModelFixtures.addStation(m, "J", CommonConstants.STATION_TYPE_JOIN);
		assertTrue(normal(m).isThereJoinWithoutForkError());
		assertFalse(normal(ModelFixtures.withForkJoin()).isThereJoinWithoutForkError());
		assertFalse(normal(open()).isThereJoinWithoutForkError());
	}

	@Test
	void forkJoinRouting() {
		CommonModel m = ModelFixtures.emptyModel();
		Object cls = ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object src = ModelFixtures.addStation(m, "Src", CommonConstants.STATION_TYPE_SOURCE);
		Object fork = ModelFixtures.addStation(m, "F", CommonConstants.STATION_TYPE_FORK);
		Object q1 = ModelFixtures.addStation(m, "Q1", CommonConstants.STATION_TYPE_SERVER);
		Object q2 = ModelFixtures.addStation(m, "Q2", CommonConstants.STATION_TYPE_SERVER);
		Object j1 = ModelFixtures.addStation(m, "J1", CommonConstants.STATION_TYPE_JOIN);
		Object j2 = ModelFixtures.addStation(m, "J2", CommonConstants.STATION_TYPE_JOIN);
		Object out = ModelFixtures.addStation(m, "Out", CommonConstants.STATION_TYPE_SINK);
		m.setClassRefStation(cls, src);
		ModelFixtures.link(m, src, fork);
		ModelFixtures.link(m, fork, q1);
		ModelFixtures.link(m, fork, q2);
		ModelFixtures.link(m, q1, j1);
		ModelFixtures.link(m, q2, j2);
		ModelFixtures.link(m, j1, out);
		ModelFixtures.link(m, j2, out);
		assertTrue(normal(m).isThereForkJoinRoutingError());
		assertFalse(normal(ModelFixtures.withForkJoin()).isThereForkJoinRoutingError());
	}

	@Test
	void emptyBlockingRegion() {
		CommonModel m = open();
		m.addBlockingRegion("R", common.Defaults.get("blockingRegionType"));
		assertTrue(normal(m).isThereEmptyBlockingRegionError());
		assertFalse(normal(ModelFixtures.withBlockingRegion()).isThereEmptyBlockingRegionError());
		assertFalse(normal(open()).isThereEmptyBlockingRegionError());
	}

	@Test
	void blockingRegionCapacityOverload() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		Object region = m.getRegionKeys().get(0);
		m.setPreloadedJobs(firstQueue(m), firstClass(m), Integer.valueOf(3));
		m.setRegionCustomerConstraint(region, Integer.valueOf(5));
		assertFalse(normal(m).isThereBlockingRegionCapacityOverloadError());
		m.setRegionCustomerConstraint(region, Integer.valueOf(2));
		assertTrue(normal(m).isThereBlockingRegionCapacityOverloadError());
	}

	@Test
	void blockingRegionClassCapacityOverload() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		Object region = m.getRegionKeys().get(0);
		m.setPreloadedJobs(firstQueue(m), firstClass(m), Integer.valueOf(3));
		m.setRegionClassCustomerConstraint(region, firstClass(m), Integer.valueOf(2));
		assertTrue(normal(m).isThereBlockingRegionCapacityOverloadError());
		m.setRegionClassCustomerConstraint(region, firstClass(m), Integer.valueOf(3));
		assertFalse(normal(m).isThereBlockingRegionCapacityOverloadError());
	}

	@Test
	void blockingRegionMemoryOverload() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		Object region = m.getRegionKeys().get(0);
		m.setPreloadedJobs(firstQueue(m), firstClass(m), Integer.valueOf(3));
		m.setRegionMemorySize(region, Integer.valueOf(10));
		assertFalse(normal(m).isThereBlockingRegionMemoryOverloadError());
		m.setRegionMemorySize(region, Integer.valueOf(2));
		assertTrue(normal(m).isThereBlockingRegionMemoryOverloadError());
	}

	@Test
	void classSwitchReferenceStation() {
		CommonModel m = ModelFixtures.closedTwoStations();
		Object c2 = ModelFixtures.addClass(m, "C2", CommonConstants.CLASS_TYPE_CLOSED);
		m.setClassRefStation(c2, m.getStationKeysDelay().get(0));
		ModelFixtures.addStation(m, "CS", CommonConstants.STATION_TYPE_CLASSSWITCH);
		assertFalse(normal(m).isThereClassSwitchReferenceStationError(), "same reference station");
		m.setClassRefStation(c2, firstQueue(m));
		assertTrue(normal(m).isThereClassSwitchReferenceStationError());
		CommonModel noCs = ModelFixtures.closedTwoStations();
		Object c3 = ModelFixtures.addClass(noCs, "C3", CommonConstants.CLASS_TYPE_CLOSED);
		noCs.setClassRefStation(c3, firstQueue(noCs));
		assertFalse(normal(noCs).isThereClassSwitchReferenceStationError(), "needs a class switch");
	}

	@Test
	void zeroGuardStrategy() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object join = m.getStationKeysJoin().get(0);
		assertFalse(normal(m).isThereZeroGuardStrategyError());
		GuardJoin guard = new GuardJoin();
		Map<Object, Integer> zero = new HashMap<Object, Integer>();
		zero.put(firstClass(m), Integer.valueOf(0));
		guard.setGuard(zero);
		m.setJoinStrategy(join, firstClass(m), guard);
		assertTrue(normal(m).isThereZeroGuardStrategyError());
		// setJoinStrategy ignores a replacement with the same strategy name, so edit the stored guard in place
		((GuardJoin) m.getJoinStrategy(join, firstClass(m))).getGuard().put(firstClass(m), Integer.valueOf(1));
		assertFalse(normal(m).isThereZeroGuardStrategyError());
	}

	@Test
	void semaphoreNotBetweenForkJoin() {
		CommonModel outside = open();
		ModelFixtures.addStation(outside, "Sem", CommonConstants.STATION_TYPE_SEMAPHORE);
		assertTrue(normal(outside).isThereSemaphoreNotBetweenForkJoinError());
		assertFalse(normal(CheckerSupport.forkJoinAround(CommonConstants.STATION_TYPE_SEMAPHORE))
				.isThereSemaphoreNotBetweenForkJoinError());
		assertFalse(normal(open()).isThereSemaphoreNotBetweenForkJoinError());
	}

	@Test
	void scalerNotBetweenForkJoin() {
		CommonModel outside = open();
		ModelFixtures.addStation(outside, "Sc", CommonConstants.STATION_TYPE_SCALER);
		assertTrue(normal(outside).isThereScalerNotBetweenForkJoinError());
		assertFalse(normal(CheckerSupport.forkJoinAround(CommonConstants.STATION_TYPE_SCALER))
				.isThereScalerNotBetweenForkJoinError());
		assertFalse(normal(open()).isThereScalerNotBetweenForkJoinError());
	}

	private static CommonModel loneTransition(int servers) {
		CommonModel m = open();
		Object t = ModelFixtures.addStation(m, "T", CommonConstants.STATION_TYPE_TRANSITION);
		m.setNumberOfServers(t, 0, Integer.valueOf(servers));
		return m;
	}

	@Test
	void transitionInfiniteEnablingDegree() {
		assertTrue(normal(loneTransition(0)).isThereTransitionInfiniteEnablingDegreeError());
		assertFalse(normal(loneTransition(2)).isThereTransitionInfiniteEnablingDegreeError());
		assertFalse(normal(open()).isThereTransitionInfiniteEnablingDegreeError());
	}

	@Test
	void dropEnabledBetweenForkJoin() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object q1 = m.getStationKeysServer().get(0);
		assertFalse(normal(m).isThereDropEnabledBetweenForkJoinError());
		m.setQueueCapacity(q1, firstClass(m), Integer.valueOf(3));
		m.setDropRule(q1, firstClass(m), CommonConstants.FINITE_DROP);
		assertTrue(normal(m).isThereDropEnabledBetweenForkJoinError());
		CommonModel outside = open();
		outside.setQueueCapacity(firstQueue(outside), firstClass(outside), Integer.valueOf(3));
		outside.setDropRule(firstQueue(outside), firstClass(outside), CommonConstants.FINITE_DROP);
		assertFalse(normal(outside).isThereDropEnabledBetweenForkJoinError(), "drop outside fork/join is fine");
	}

	@Test
	void impatienceEnabledBetweenForkJoin() {
		CommonModel m = ModelFixtures.withForkJoin();
		Object q1 = m.getStationKeysServer().get(0);
		assertFalse(normal(m).isThereImpatienceEnabledBetweenForkJoinError());
		m.setImpatienceType(q1, firstClass(m), ImpatienceType.BALKING);
		assertTrue(normal(m).isThereImpatienceEnabledBetweenForkJoinError());
		CommonModel outside = open();
		outside.setImpatienceType(firstQueue(outside), firstClass(outside), ImpatienceType.BALKING);
		assertFalse(normal(outside).isThereImpatienceEnabledBetweenForkJoinError());
	}
}
