package common.controller;

import static common.controller.CheckerSupport.firstClass;
import static common.controller.CheckerSupport.firstQueue;
import static common.controller.CheckerSupport.jmva;
import static common.controller.CheckerSupport.normal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.definitions.ModelFixtures;

/** Aggregate flags of ModelChecker against the individual checks, plus degenerate inputs. */
class ModelCheckerSummaryTest {

	/** True if any normal-mode error check fires. */
	private static boolean anyError(ModelChecker c) {
		return c.isThereNoClassError() || c.isThereNoStationError() || c.isThereNoMeasureError()
				|| c.isThereOpenClassButNoSourceError() || c.isThereOpenClassButNoSinkError()
				|| c.isThereSourceWithoutOpenClassError() || c.isThereSinkButNoOpenClassError()
				|| c.isThereNoReferenceStationError() || c.isThereNoEssentialLinkError()
				|| c.isThereInvalidMeasureError() || c.isThereRedundantMeasureError()
				|| c.isThereInconsistentQueueStrategyError() || c.isThereUnpredictableServiceError()
				|| c.isThereJoinWithoutForkError() || c.isThereForkJoinRoutingError()
				|| c.isThereEmptyBlockingRegionError() || c.isThereBlockingRegionCapacityOverloadError()
				|| c.isThereBlockingRegionMemoryOverloadError() || c.isThereClassSwitchReferenceStationError()
				|| c.isThereZeroGuardStrategyError() || c.isThereSemaphoreNotBetweenForkJoinError()
				|| c.isThereScalerNotBetweenForkJoinError() || c.isThereTransitionInfiniteEnablingDegreeError()
				|| c.isThereDropEnabledBetweenForkJoinError() || c.isThereImpatienceEnabledBetweenForkJoinError();
	}

	/** True if any normal-mode warning check fires (ZeroTotalPopulation is excluded: it is stored as an error). */
	private static boolean anyNormalWarning(ModelChecker c) {
		return c.isThereNoOptionalLinkWarning() || c.isThereForkWithoutJoinWarning()
				|| c.isThereParametricAnalysisModelModifiedWarning() || c.isThereParametricAnalysisNotAvailableWarning()
				|| c.isThereZeroPopulationWarning() || c.isThereClassSwitchBetweenForkJoinWarning()
				|| c.isThereClassSwitchRoutingBetweenForkJoinWarning() || c.isThereSchedulingSamePriorityWarning()
				|| c.isThereTransitionBetweenForkJoinWarning() || c.isThereTransitionConstantEnablingDegreeWarning()
				|| c.isThereTransitionInvalidInputConditionWarning() || c.isThereTransitionNoFiringOutcomeWarning();
	}

	private static boolean anyJmvaError(ModelChecker c) {
		return c.isThereNoClassError() || c.isThereNoStationError() || c.isThereOpenClassButNoSourceError()
				|| c.isThereNoReferenceStationError();
	}

	private static boolean anyJmvaWarning(ModelChecker c) {
		return c.isThereBcmpDifferentQueueStrategiesWarning() || c.isThereBcmpDifferentServiceStrategiesWarning()
				|| c.isThereBcmpFcfsNonExponentialDistributionWarning() || c.isThereBcmpFcfsDifferentServiceTimesWarning()
				|| c.isThereBcmpPsNonRationalDistributionWarning() || c.isThereBcmpLcfsPrNonRationalDistributionWarning()
				|| c.isThereBcmpDelayNonRationalDistributionWarning() || c.isThereBcmpStateDependentRoutingStrategyWarning()
				|| c.isThereBcmpAsymmetricSchedulingPolicyWarning();
	}

	/** The normal-mode aggregates must agree with the individual checks. */
	private static void assertNormalConsistent(ModelChecker c) {
		assertEquals(!anyError(c), c.isErrorFreeNormal(), "isErrorFreeNormal");
		assertEquals(!anyNormalWarning(c), c.isWarningFreeNormal(), "isWarningFreeNormal");
		assertEquals(c.isErrorFreeNormal() && c.isWarningFreeNormal(), c.isEverythingOkNormal(), "isEverythingOkNormal");
	}

	private static void assertJmvaConsistent(ModelChecker c) {
		assertEquals(!anyJmvaError(c), c.isErrorFreeToJMVA(), "isErrorFreeToJMVA");
		assertEquals(!anyJmvaWarning(c), c.isWarningFreeToJMVA(), "isWarningFreeToJMVA");
		assertEquals(c.isErrorFreeToJMVA() && c.isWarningFreeToJMVA(), c.isEverythingOkToJMVA(), "isEverythingOkToJMVA");
	}

	private static CommonModel[] cleanModels() {
		return new CommonModel[] { ModelFixtures.openOneStation(), ModelFixtures.closedTwoStations(),
				ModelFixtures.withForkJoin(), ModelFixtures.withBlockingRegion() };
	}

	@Test
	void cleanModelsAreOkInNormalMode() {
		for (CommonModel m : cleanModels()) {
			ModelChecker c = normal(m);
			assertTrue(c.isErrorFreeNormal());
			assertTrue(c.isWarningFreeNormal());
			assertTrue(c.isEverythingOkNormal());
			assertNormalConsistent(c);
			assertFalse(c.isToJMVA());
		}
	}

	@Test
	void cleanModelsAreOkForJmva() {
		for (CommonModel m : cleanModels()) {
			ModelChecker c = jmva(m);
			assertTrue(c.isToJMVA());
			assertTrue(c.isErrorFreeToJMVA());
			assertTrue(c.isWarningFreeToJMVA());
			assertTrue(c.isEverythingOkToJMVA());
			assertJmvaConsistent(c);
		}
	}

	@Test
	void oneErrorOnly() {
		CommonModel m = ModelFixtures.openOneStation();
		m.removeMeasure(m.getMeasureKeys().get(0));
		ModelChecker c = normal(m);
		assertTrue(c.isThereNoMeasureError());
		assertFalse(c.isErrorFreeNormal());
		assertTrue(c.isWarningFreeNormal());
		assertFalse(c.isEverythingOkNormal());
		assertNormalConsistent(c);
		ModelChecker j = jmva(m);
		assertTrue(j.isErrorFreeToJMVA(), "JMVA does not require measures");
		assertTrue(j.isEverythingOkToJMVA());
	}

	@Test
	void oneErrorOnlyForJmva() {
		CommonModel m = ModelFixtures.openOneStation();
		ModelFixtures.addClass(m, "NoRef", CommonConstants.CLASS_TYPE_OPEN);
		ModelChecker j = jmva(m);
		assertTrue(j.isThereNoReferenceStationError());
		assertFalse(j.isErrorFreeToJMVA());
		assertFalse(j.isEverythingOkToJMVA());
		assertJmvaConsistent(j);
		assertFalse(normal(m).isErrorFreeNormal());
	}

	@Test
	void oneWarningOnly() {
		CommonModel m = ModelFixtures.closedTwoStations();
		Object c2 = ModelFixtures.addClass(m, "C2", CommonConstants.CLASS_TYPE_CLOSED);
		m.setClassRefStation(c2, m.getStationKeysDelay().get(0));
		m.setClassPopulation(c2, Integer.valueOf(0));
		ModelChecker c = normal(m);
		assertTrue(c.isThereZeroPopulationWarning());
		assertTrue(c.isErrorFreeNormal());
		assertFalse(c.isWarningFreeNormal());
		assertFalse(c.isEverythingOkNormal());
		assertNormalConsistent(c);
	}

	@Test
	void oneWarningOnlyForJmva() {
		CommonModel m = ModelFixtures.closedTwoStations();
		m.setQueueStrategy(firstQueue(m), firstClass(m), CommonConstants.QUEUE_STRATEGY_LCFS);
		ModelChecker j = jmva(m);
		assertTrue(j.isThereBcmpAsymmetricSchedulingPolicyWarning());
		assertTrue(j.isErrorFreeToJMVA());
		assertFalse(j.isWarningFreeToJMVA());
		assertFalse(j.isEverythingOkToJMVA());
		assertJmvaConsistent(j);
	}

	@Test
	void jmvaPriorityWarningCountsAsWarning() {
		CommonModel m = ModelFixtures.closedTwoStations();
		m.setStationQueueStrategy(firstQueue(m), CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY);
		ModelChecker j = jmva(m);
		assertTrue(j.isThereBcmpPrioritySchedulingPolicyWarning());
		assertFalse(j.isWarningFreeToJMVA());
	}

	@Test
	void zeroTotalPopulationCountsAsWarning() {
		CommonModel m = ModelFixtures.closedTwoStations();
		m.setClassPopulation(firstClass(m), Integer.valueOf(0));
		ModelChecker c = normal(m);
		assertTrue(c.isErrorFreeNormal());
		assertFalse(c.isWarningFreeNormal());
	}

	@Test
	void emptyModel() {
		ModelChecker c = normal(ModelFixtures.emptyModel());
		assertTrue(c.isThereNoClassError());
		assertTrue(c.isThereNoStationError());
		assertTrue(c.isThereNoMeasureError());
		assertFalse(c.isErrorFreeNormal());
		assertTrue(c.isWarningFreeNormal());
		assertFalse(c.isEverythingOkNormal());
		assertNormalConsistent(c);
		ModelChecker j = jmva(ModelFixtures.emptyModel());
		assertFalse(j.isErrorFreeToJMVA());
		assertTrue(j.isWarningFreeToJMVA());
		assertFalse(j.isEverythingOkToJMVA());
		assertJmvaConsistent(j);
	}

	@Test
	void classesButNoStations() {
		CommonModel m = ModelFixtures.emptyModel();
		ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		ModelChecker c = normal(m);
		assertFalse(c.isThereNoClassError());
		assertTrue(c.isThereNoStationError());
		assertFalse(c.isErrorFreeNormal());
		assertNormalConsistent(c);
		assertFalse(jmva(m).isErrorFreeToJMVA());
	}

	@Test
	void stationsButNoClasses() {
		CommonModel m = ModelFixtures.emptyModel();
		ModelFixtures.addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		ModelChecker c = normal(m);
		assertTrue(c.isThereNoClassError());
		assertFalse(c.isThereNoStationError());
		assertFalse(c.isErrorFreeNormal());
		assertNormalConsistent(c);
		ModelChecker j = jmva(m);
		assertTrue(j.isThereNoClassError());
		assertFalse(j.isErrorFreeToJMVA());
		assertJmvaConsistent(j);
	}

	@Test
	void singleStationModel() {
		CommonModel m = ModelFixtures.emptyModel();
		Object cls = ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		Object d = ModelFixtures.addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		m.setClassRefStation(cls, d);
		ModelFixtures.link(m, d, d);
		m.addMeasure(common.definitions.SimulationDefinition.MEASURE_QL, d, cls);
		ModelChecker c = normal(m);
		assertTrue(c.isErrorFreeNormal());
		assertNormalConsistent(c);
		assertTrue(jmva(m).isEverythingOkToJMVA());
	}

	@Test
	void singleUnlinkedStationModel() {
		CommonModel m = ModelFixtures.emptyModel();
		Object cls = ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		Object q = ModelFixtures.addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		m.setClassRefStation(cls, q);
		ModelChecker c = normal(m);
		assertTrue(c.isThereNoEssentialLinkError());
		assertTrue(c.isThereNoOptionalLinkWarning());
		assertNormalConsistent(c);
	}

	@Test
	void recheckAfterFixingTheModel() {
		CommonModel m = ModelFixtures.openOneStation();
		Object extra = ModelFixtures.addClass(m, "NoRef", CommonConstants.CLASS_TYPE_OPEN);
		ModelChecker c = normal(m);
		assertTrue(c.isThereNoReferenceStationError());
		c.checkModel();
		assertTrue(c.isThereNoReferenceStationError(), "checkModel is idempotent");
		m.setClassRefStation(extra, m.getStationKeysSource().get(0));
		c.checkModel();
		assertFalse(c.isThereNoReferenceStationError(), "flags are reset on every checkModel");
		assertTrue(c.isErrorFreeNormal());
	}
}
