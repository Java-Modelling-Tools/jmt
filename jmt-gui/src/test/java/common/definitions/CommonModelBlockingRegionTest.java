package common.definitions;

import static common.definitions.ModelFixtures.addClass;
import static common.definitions.ModelFixtures.addStation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import common.CommonConstants;
import common.Defaults;

/** Characterization of CommonModel blocking (finite capacity) regions. */
class CommonModelBlockingRegionTest {

	private static final String TYPE = Defaults.get("blockingRegionType");

	@Test
	void emptyModelHasNoRegionsAndUnknownKeysAreHarmless() {
		CommonModel m = ModelFixtures.emptyModel();
		Object bogus = Long.valueOf(999);
		assertTrue(m.getRegionKeys().isEmpty());
		assertNull(m.getRegionName(bogus));
		assertNull(m.getRegionType(bogus));
		assertNull(m.getRegionCustomerConstraint(bogus));
		assertNull(m.getBlockingRegionStations(bogus));
		assertNull(m.getStationBlockingRegion(bogus));
		assertNull(m.getRegionGroupList(bogus));
		assertFalse(m.canRegionStationBeAdded(bogus, bogus));
		assertFalse(m.addRegionStation(bogus, bogus));
		m.setRegionName(bogus, "x");
		m.deleteBlockingRegion(bogus);
		assertFalse(m.toBeSaved());
	}

	@Test
	void newRegionTakesDefaults() {
		CommonModel m = ModelFixtures.openOneStation();
		Object c = m.getClassKeys().get(0);
		Object r = m.addBlockingRegion("R", TYPE);
		assertEquals(Arrays.asList(r), m.getRegionKeys());
		assertEquals("R", m.getRegionName(r));
		assertEquals(TYPE, m.getRegionType(r));
		assertEquals(Defaults.getAsInteger("blockingMaxJobs"), m.getRegionCustomerConstraint(r));
		assertEquals(Defaults.getAsInteger("blockingMaxMemory"), m.getRegionMemorySize(r));
		assertEquals(Defaults.getAsInteger("blockingMaxJobsPerClass"), m.getRegionClassCustomerConstraint(r, c));
		assertEquals(Defaults.getAsInteger("blockingMaxMemoryPerClass"), m.getRegionClassMemorySize(r, c));
		assertEquals(Defaults.getAsBoolean("blockingDropPerClass"), m.getRegionClassDropRule(r, c));
		assertEquals(Defaults.getAsInteger("blockingWeightPerClass"), m.getRegionClassWeight(r, c));
		assertEquals(Defaults.getAsInteger("blockingSizePerClass"), m.getRegionClassSize(r, c));
		assertEquals(Defaults.getAsDouble("classSoftDeadline"), m.getRegionClassSoftDeadline(r, c));
		assertTrue(m.getBlockingRegionStations(r).isEmpty());
		assertTrue(m.getRegionGroupList(r).isEmpty());
	}

	@Test
	void duplicateRegionNamesAreMadeUnique() {
		CommonModel m = ModelFixtures.emptyModel();
		Object a = m.addBlockingRegion("R", TYPE);
		Object b = m.addBlockingRegion("R", TYPE);
		assertEquals("R", m.getRegionName(a));
		assertEquals("R_1", m.getRegionName(b));
		m.setRegionName(b, "R");
		assertFalse("R".equals(m.getRegionName(b)));
		assertTrue(m.getRegionName(b).startsWith("R_"));
		m.setRegionName(b, "Z");
		assertEquals("Z", m.getRegionName(b));
	}

	@Test
	void regionSettersRoundTrip() {
		CommonModel m = ModelFixtures.openOneStation();
		Object c = m.getClassKeys().get(0);
		Object r = m.addBlockingRegion("R", TYPE);
		m.resetSaveState();
		m.setRegionCustomerConstraint(r, Integer.valueOf(5));
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setRegionCustomerConstraint(r, Integer.valueOf(5)); // unchanged: not dirty
		assertFalse(m.toBeSaved());
		m.setRegionMemorySize(r, Integer.valueOf(6));
		m.setRegionClassCustomerConstraint(r, c, Integer.valueOf(7));
		m.setRegionClassMemorySize(r, c, Integer.valueOf(8));
		m.setRegionClassDropRule(r, c, Boolean.TRUE);
		m.setRegionClassWeight(r, c, Integer.valueOf(9));
		m.setRegionClassSize(r, c, Integer.valueOf(10));
		m.setRegionClassSoftDeadline(r, c, Double.valueOf(1.5));
		m.setRegionType(r, TYPE + "2");
		assertEquals(Integer.valueOf(5), m.getRegionCustomerConstraint(r));
		assertEquals(Integer.valueOf(6), m.getRegionMemorySize(r));
		assertEquals(Integer.valueOf(7), m.getRegionClassCustomerConstraint(r, c));
		assertEquals(Integer.valueOf(8), m.getRegionClassMemorySize(r, c));
		assertEquals(Boolean.TRUE, m.getRegionClassDropRule(r, c));
		assertEquals(Integer.valueOf(9), m.getRegionClassWeight(r, c));
		assertEquals(Integer.valueOf(10), m.getRegionClassSize(r, c));
		assertEquals(Double.valueOf(1.5), m.getRegionClassSoftDeadline(r, c));
		assertEquals(TYPE + "2", m.getRegionType(r));
	}

	@Test
	void onlyBlockableStationTypesCanJoinARegion() {
		CommonModel m = ModelFixtures.emptyModel();
		Object r = m.addBlockingRegion("R", TYPE);
		String[] ok = { CommonConstants.STATION_TYPE_SERVER, CommonConstants.STATION_TYPE_DELAY, CommonConstants.STATION_TYPE_ROUTER,
				CommonConstants.STATION_TYPE_LOGGER, CommonConstants.STATION_TYPE_PLACE };
		String[] no = { CommonConstants.STATION_TYPE_SOURCE, CommonConstants.STATION_TYPE_SINK, CommonConstants.STATION_TYPE_TERMINAL,
				CommonConstants.STATION_TYPE_FORK, CommonConstants.STATION_TYPE_JOIN, CommonConstants.STATION_TYPE_CLASSSWITCH,
				CommonConstants.STATION_TYPE_SEMAPHORE, CommonConstants.STATION_TYPE_SCALER, CommonConstants.STATION_TYPE_TRANSITION };
		for (String t : ok) {
			assertTrue(m.canRegionStationBeAdded(r, addStation(m, t, t)), t);
		}
		for (String t : no) {
			Object s = addStation(m, t, t);
			assertFalse(m.canRegionStationBeAdded(r, s), t);
			assertFalse(m.addRegionStation(r, s), t);
		}
	}

	@Test
	void stationBelongsToAtMostOneRegion() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object r1 = m.addBlockingRegion("R1", TYPE);
		Object r2 = m.addBlockingRegion("R2", TYPE);
		assertTrue(m.getBlockableStationKeys().contains(q));
		assertTrue(m.addRegionStation(r1, q));
		assertSame(r1, m.getStationBlockingRegion(q));
		assertTrue(m.getBlockingRegionStations(r1).contains(q));
		assertFalse(m.getBlockableStationKeys().contains(q));
		assertFalse(m.canRegionStationBeAdded(r2, q));
		assertFalse(m.addRegionStation(r2, q));
		assertTrue(m.canRegionStationBeAdded(r1, q)); // already a member of this very region
		m.removeRegionStation(r1, q);
		assertNull(m.getStationBlockingRegion(q));
		assertTrue(m.getBlockableStationKeys().contains(q));
		assertTrue(m.addRegionStation(r2, q));
	}

	@Test
	void deleteRegionReleasesItsStationsAndDetails() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		Object r = m.getRegionKeys().get(0);
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		m.deleteBlockingRegion(r);
		assertTrue(m.getRegionKeys().isEmpty());
		assertNull(m.getStationBlockingRegion(q));
		assertTrue(m.getBlockableStationKeys().contains(q));
		assertNull(m.getRegionName(r));
		assertNull(m.getRegionClassCustomerConstraint(r, c));
		assertEquals(3, m.getStationKeys().size());
	}

	@Test
	void regionAddedAfterClassesAndClassAddedAfterRegionsBothGetPerClassDetails() {
		CommonModel m = ModelFixtures.openOneStation();
		Object r = m.addBlockingRegion("R", TYPE);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		assertEquals(Defaults.getAsInteger("blockingMaxJobsPerClass"), m.getRegionClassCustomerConstraint(r, c2));
		assertEquals(Defaults.getAsInteger("blockingWeightPerClass"), m.getRegionClassWeight(r, c2));
	}

	@Test
	void changeClassTypeResetsRegionDetailsOfTheClass() {
		CommonModel m = ModelFixtures.openOneStation();
		Object c = m.getClassKeys().get(0);
		Object r = m.addBlockingRegion("R", TYPE);
		m.setRegionClassWeight(r, c, Integer.valueOf(9));
		m.setClassType(c, CommonConstants.CLASS_TYPE_CLOSED);
		assertEquals(Defaults.getAsInteger("blockingWeightPerClass"), m.getRegionClassWeight(r, c));
	}

	@Test
	void groupsLifecycle() {
		CommonModel m = ModelFixtures.openOneStation();
		Object c1 = m.getClassKeys().get(0);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		Object r = m.addBlockingRegion("R", TYPE);
		m.addRegionGroup(r, "G1");
		m.addRegionGroup(r, "G2");
		assertEquals(2, m.getRegionGroupList(r).size());
		assertEquals("G1", m.getRegionGroupName(r, 0));
		assertEquals(Defaults.getAsInteger("blockingGroupMaxJobs"), m.getRegionGroupCustomerConstraint(r, 0));
		assertEquals(Defaults.getAsInteger("blockingGroupMaxMemory"), m.getRegionGroupMemorySize(r, 0));
		m.setRegionGroupName(r, 1, "Second");
		m.setRegionGroupCustomerConstraint(r, 1, Integer.valueOf(3));
		m.setRegionGroupMemorySize(r, 1, Integer.valueOf(4));
		assertEquals("Second", m.getRegionGroupName(r, 1));
		assertEquals(Integer.valueOf(3), m.getRegionGroupCustomerConstraint(r, 1));
		assertEquals(Integer.valueOf(4), m.getRegionGroupMemorySize(r, 1));
		m.addClassIntoRegionGroup(r, 0, c1);
		m.addClassIntoRegionGroup(r, 0, c1); // adding twice is a no-op
		m.addClassIntoRegionGroup(r, 0, c2);
		assertEquals(Arrays.asList(c1, c2), m.getRegionGroupClassList(r, 0));
		m.removeClassFromRegionGroup(r, 0, c1);
		assertEquals(Arrays.asList(c2), m.getRegionGroupClassList(r, 0));
		m.removeAllClassesFromRegionGroup(r, 0);
		assertTrue(m.getRegionGroupClassList(r, 0).isEmpty());
		m.deleteRegionGroup(r, 0);
		assertEquals(1, m.getRegionGroupList(r).size());
		assertEquals("Second", m.getRegionGroupName(r, 0));
		m.deleteAllRegionGroups(r);
		assertTrue(m.getRegionGroupList(r).isEmpty());
	}

	@Test
	void outOfRangeGroupIndexIsIgnored() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		Object r = m.getRegionKeys().get(0);
		m.resetSaveState();
		assertNull(m.getRegionGroupName(r, 0));
		assertNull(m.getRegionGroupCustomerConstraint(r, 3));
		assertNull(m.getRegionGroupClassList(r, 0));
		m.setRegionGroupName(r, 0, "x");
		m.deleteRegionGroup(r, 0);
		m.addClassIntoRegionGroup(r, 0, m.getClassKeys().get(0));
		assertFalse(m.toBeSaved());
	}

	@Test
	void regionOnlyModelHasNoClassesOrStations() {
		CommonModel m = ModelFixtures.emptyModel();
		Object r = m.addBlockingRegion("R", TYPE);
		assertEquals(Arrays.asList(r), m.getRegionKeys());
		assertTrue(m.getClassKeys().isEmpty());
		assertTrue(m.getFCRegionKeys().contains(r));
		assertTrue(m.getStationRegionKeysNoSourceSink().contains(r));
	}
}
