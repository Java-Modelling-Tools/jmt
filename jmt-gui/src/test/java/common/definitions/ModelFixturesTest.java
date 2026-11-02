package common.definitions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.CommonConstants;
import common.controller.ModelChecker;
import org.junit.jupiter.api.Test;

class ModelFixturesTest {

	private static void counts(CommonModel m, int classes, int stations, int links) {
		assertEquals(classes, m.getClassKeys().size(), "classes");
		assertEquals(stations, m.getStationKeys().size(), "stations");
		int n = 0;
		for (Object s : m.getStationKeys()) {
			n += m.getForwardConnections(s).size();
		}
		assertEquals(links, n, "links");
	}

	@Test
	void empty() {
		counts(ModelFixtures.emptyModel(), 0, 0, 0);
	}

	@Test
	void openOneStation() {
		CommonModel m = ModelFixtures.openOneStation();
		counts(m, 1, 3, 2);
		assertEquals(1, m.getOpenClassKeys().size());
	}

	@Test
	void closedTwoStations() {
		CommonModel m = ModelFixtures.closedTwoStations();
		counts(m, 1, 2, 2);
		assertEquals(1, m.getClosedClassKeys().size());
	}

	@Test
	void forkJoin() {
		CommonModel m = ModelFixtures.withForkJoin();
		counts(m, 1, 6, 6);
		assertEquals(1, m.getStationKeysFork().size());
		assertEquals(1, m.getStationKeysJoin().size());
	}

	@Test
	void blockingRegion() {
		CommonModel m = ModelFixtures.withBlockingRegion();
		counts(m, 1, 3, 2);
		assertEquals(1, m.getRegionKeys().size());
		assertEquals(1, m.getBlockingRegionStations(m.getRegionKeys().get(0)).size());
	}

	@Test
	void callsAreIndependent() {
		CommonModel a = ModelFixtures.openOneStation();
		CommonModel b = ModelFixtures.openOneStation();
		assertNotSame(a, b);
		ModelFixtures.addClass(a, "Extra", CommonConstants.CLASS_TYPE_OPEN);
		counts(a, 2, 3, 2);
		counts(b, 1, 3, 2);
	}

	private static void assertValid(CommonModel m) {
		ModelChecker c = new ModelChecker(m, m, m, m, false);
		c.checkModel();
		assertTrue(c.isErrorFreeNormal());
	}

	@Test
	void buildersPassModelChecker() {
		assertValid(ModelFixtures.openOneStation());
		assertValid(ModelFixtures.closedTwoStations());
		assertValid(ModelFixtures.withForkJoin());
		assertValid(ModelFixtures.withBlockingRegion());
	}

	@Test
	void closedPopulationAndReferenceStations() {
		CommonModel c = ModelFixtures.closedTwoStations();
		Object cls = c.getClassKeys().get(0);
		assertEquals(Integer.valueOf(5), c.getClassPopulation(cls));
		assertEquals(c.getStationKeysDelay().get(0), c.getClassRefStation(cls));
		CommonModel o = ModelFixtures.openOneStation();
		assertEquals(o.getStationKeysSource().get(0), o.getClassRefStation(o.getClassKeys().get(0)));
	}

	@Test
	void emptyModelReportsMissingParts() {
		CommonModel m = ModelFixtures.emptyModel();
		ModelChecker c = new ModelChecker(m, m, m, m, false);
		c.checkModel();
		assertTrue(c.isThereNoClassError());
		assertTrue(c.isThereNoStationError());
		assertTrue(c.isThereNoMeasureError());
	}
}
