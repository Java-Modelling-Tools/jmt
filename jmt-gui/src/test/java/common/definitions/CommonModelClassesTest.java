package common.definitions;

import static common.definitions.ModelFixtures.addClass;
import static common.definitions.ModelFixtures.addStation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import common.CommonConstants;
import common.Defaults;
import common.distributions.Exponential;
import common.routingStrategies.ProbabilityRouting;

/** Characterization of CommonModel class management. */
class CommonModelClassesTest {

	@Test
	void emptyModelHasNoClasses() {
		CommonModel m = ModelFixtures.emptyModel();
		assertTrue(m.getClassKeys().isEmpty());
		assertTrue(m.getOpenClassKeys().isEmpty());
		assertTrue(m.getClosedClassKeys().isEmpty());
		assertEquals(0, m.getTotalClosedClassPopulation());
		assertFalse(m.toBeSaved());
	}

	@Test
	void unknownClassKeyGivesNeutralValues() {
		CommonModel m = ModelFixtures.emptyModel();
		Object bogus = Long.valueOf(999);
		assertNull(m.getClassName(bogus));
		assertEquals(-1, m.getClassType(bogus));
		assertNull(m.getClassPopulation(bogus));
		assertNull(m.getClassByName("nope"));
		m.setClassName(bogus, "x");
		m.deleteClass(bogus);
		assertFalse(m.toBeSaved());
	}

	@Test
	void addOpenClassKeepsParameters() {
		CommonModel m = ModelFixtures.emptyModel();
		Exponential d = new Exponential();
		Object k = m.addClass("Open", CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(3), Double.valueOf(1.5), Integer.valueOf(7), d);
		assertEquals("Open", m.getClassName(k));
		assertEquals(CommonConstants.CLASS_TYPE_OPEN, m.getClassType(k));
		assertEquals(Integer.valueOf(3), m.getClassPriority(k));
		assertEquals(Double.valueOf(1.5), m.getClassSoftDeadline(k));
		assertNull(m.getClassPopulation(k)); // population is dropped for open classes
		assertSame(d, m.getClassDistribution(k));
		assertNull(m.getClassRefStation(k));
		assertEquals(1, m.getOpenClassKeys().size());
		assertTrue(m.getClosedClassKeys().isEmpty());
		assertTrue(m.toBeSaved());
		assertSame(k, m.getClassByName("Open"));
	}

	@Test
	void addClosedClassKeepsParameters() {
		CommonModel m = ModelFixtures.emptyModel();
		Object k = m.addClass("Closed", CommonConstants.CLASS_TYPE_CLOSED, Integer.valueOf(0), Double.valueOf(0), Integer.valueOf(7), new Exponential());
		assertEquals(Integer.valueOf(7), m.getClassPopulation(k));
		assertNull(m.getClassDistribution(k)); // distribution is dropped for closed classes
		assertEquals(1, m.getClosedClassKeys().size());
		assertEquals(7, m.getTotalClosedClassPopulation());
	}

	@Test
	void keysAreUniqueAndOrderIsInsertionOrder() {
		CommonModel m = ModelFixtures.emptyModel();
		Object a = addClass(m, "A", CommonConstants.CLASS_TYPE_OPEN);
		Object s = addStation(m, "S", CommonConstants.STATION_TYPE_SERVER);
		Object b = addClass(m, "B", CommonConstants.CLASS_TYPE_CLOSED);
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		assertEquals(java.util.Arrays.asList(a, b, c), m.getClassKeys());
		assertEquals(java.util.Arrays.asList(a, c), m.getOpenClassKeys());
		assertEquals(java.util.Arrays.asList(b), m.getClosedClassKeys());
		// classes, stations and measures share one key counter
		assertTrue(!a.equals(s) && !s.equals(b));
	}

	@Test
	void duplicateClassNamesAreMadeUnique() {
		CommonModel m = ModelFixtures.emptyModel();
		Object a = addClass(m, "Class", CommonConstants.CLASS_TYPE_OPEN);
		Object b = addClass(m, "Class", CommonConstants.CLASS_TYPE_OPEN);
		Object c = addClass(m, "Class", CommonConstants.CLASS_TYPE_OPEN);
		assertEquals("Class", m.getClassName(a));
		assertEquals("Class_1", m.getClassName(b));
		assertEquals("Class_2", m.getClassName(c));
	}

	@Test
	void renameClass() {
		CommonModel m = ModelFixtures.emptyModel();
		Object a = addClass(m, "A", CommonConstants.CLASS_TYPE_OPEN);
		Object b = addClass(m, "B", CommonConstants.CLASS_TYPE_OPEN);
		m.resetSaveState();
		m.setClassName(a, "A2");
		assertEquals("A2", m.getClassName(a));
		assertSame(a, m.getClassByName("A2"));
		assertNull(m.getClassByName("A"));
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setClassName(a, "A2"); // same name: no change
		m.setClassName(a, ""); // empty name ignored
		assertEquals("A2", m.getClassName(a));
		assertFalse(m.toBeSaved());
		m.setClassName(a, "B"); // clash is made unique
		assertEquals("B_1", m.getClassName(a));
		assertEquals("B", m.getClassName(b));
	}

	@Test
	void classSettersRoundTrip() {
		CommonModel m = ModelFixtures.emptyModel();
		Object k = addClass(m, "A", CommonConstants.CLASS_TYPE_CLOSED);
		m.setClassPriority(k, Integer.valueOf(4));
		m.setClassSoftDeadline(k, Double.valueOf(2.5));
		m.setClassPopulation(k, Integer.valueOf(11));
		assertEquals(Integer.valueOf(4), m.getClassPriority(k));
		assertEquals(Double.valueOf(2.5), m.getClassSoftDeadline(k));
		assertEquals(Integer.valueOf(11), m.getClassPopulation(k));
		assertEquals(11, m.getTotalClosedClassPopulation());
		Object d = addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		m.setClassRefStation(k, d);
		assertSame(d, m.getClassRefStation(k));
	}

	@Test
	void changeClassTypeMovesKeyAndResetsTypeSpecificFields() {
		CommonModel m = ModelFixtures.emptyModel();
		Object k = addClass(m, "A", CommonConstants.CLASS_TYPE_CLOSED);
		Object d = addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		m.setClassRefStation(k, d);
		m.addMeasure(SimulationDefinition.MEASURE_QL, d, k);
		m.setClassType(k, CommonConstants.CLASS_TYPE_OPEN);
		assertEquals(CommonConstants.CLASS_TYPE_OPEN, m.getClassType(k));
		assertEquals(1, m.getOpenClassKeys().size());
		assertTrue(m.getClosedClassKeys().isEmpty());
		assertNull(m.getClassPopulation(k));
		assertNotNull(m.getClassDistribution(k));
		assertNull(m.getClassRefStation(k));
		assertTrue(m.getMeasureKeys().isEmpty()); // measures of the class are dropped
		m.setClassType(k, CommonConstants.CLASS_TYPE_CLOSED);
		assertEquals(Defaults.getAsInteger("classPopulation"), m.getClassPopulation(k));
		assertNull(m.getClassDistribution(k));
		assertEquals(1, m.getClosedClassKeys().size());
	}

	@Test
	void deleteClassRemovesMeasuresAndPerClassParameters() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c1 = m.getClassKeys().get(0);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		m.addMeasure(SimulationDefinition.MEASURE_X, q, c2);
		assertEquals(2, m.getMeasureKeys().size());
		assertNotNull(m.getServiceTimeDistribution(q, c2));
		m.deleteClass(c2);
		assertEquals(1, m.getClassKeys().size());
		assertEquals(1, m.getOpenClassKeys().size());
		assertEquals(1, m.getMeasureKeys().size());
		assertSame(c1, m.getMeasureClass(m.getMeasureKeys().get(0)));
		assertNull(m.getServiceTimeDistribution(q, c2));
		assertNull(m.getRoutingStrategy(q, c2));
		assertNull(m.getClassName(c2));
		assertNull(m.getClassByName("C2"));
		// stations and links survive
		assertEquals(3, m.getStationKeys().size());
		assertTrue(m.areConnected(m.getStationKeysSource().get(0), q));
	}

	@Test
	void deleteClassCleansUpProbabilityRoutingOfOtherClasses() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		m.setRoutingStrategy(q, c2, new ProbabilityRouting());
		m.deleteClass(c2);
		assertEquals(1, m.getClassKeys().size());
		assertNotNull(m.getRoutingStrategy(q, m.getClassKeys().get(0)));
	}

	@Test
	void deleteLastClassLeavesEmptyClassSet() {
		CommonModel m = ModelFixtures.openOneStation();
		m.deleteClass(m.getClassKeys().get(0));
		assertTrue(m.getClassKeys().isEmpty());
		assertTrue(m.getMeasureKeys().isEmpty());
		assertEquals(3, m.getStationKeys().size());
	}

	@Test
	void deleteClassRemovesItFromBlockingRegionGroupsAndDetails() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		Object region = m.getRegionKeys().get(0);
		Object c = m.getClassKeys().get(0);
		m.addRegionGroup(region, "G");
		m.addClassIntoRegionGroup(region, 0, c);
		assertEquals(1, m.getRegionGroupClassList(region, 0).size());
		m.deleteClass(c);
		assertTrue(m.getRegionGroupClassList(region, 0).isEmpty());
		assertNull(m.getRegionClassCustomerConstraint(region, c));
	}

	@Test
	void classAddedAfterStationsGetsDefaultStationParameters() {
		CommonModel m = ModelFixtures.emptyModel();
		Object src = addStation(m, "Src", CommonConstants.STATION_TYPE_SOURCE);
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		Object c = addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		assertTrue(m.getRoutingStrategy(src, c) instanceof common.routingStrategies.RandomRouting);
		assertTrue(m.getServiceTimeDistribution(q, c) instanceof Exponential);
		assertEquals(Integer.valueOf(0), m.getPreloadedJobs(q, c));
	}

	@Test
	void oneClassOnlyModelHasNoStations() {
		CommonModel m = ModelFixtures.emptyModel();
		addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		assertTrue(m.getStationKeys().isEmpty());
		assertEquals(1, m.getClassKeys().size());
	}
}
