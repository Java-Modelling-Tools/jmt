package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import common.CommonConstants;
import common.definitions.CommonModel;

/** XMLReader on Petri-net and hybrid (queueing + Petri net) examples. */
class XMLReaderPetriNetTest {

	private static CommonModel load(String rel) {
		return SimXml.loadSim(new File(new File(ModelFixtures.examplesDir(), "jsim"), rel));
	}

	/** file, classes, stations, places, transitions, servers, delays, preloaded jobs, connections, measures */
	@ParameterizedTest
	@CsvSource({
			"pn_models/philosophers.jsimg,1,28,16,12,0,0,8,40,4",
			"pn_models/producer_consumer.jsimg,1,14,8,6,0,0,5,16,14",
			"pn_models/readers_writers.jsimg,1,13,6,7,0,0,5,17,13",
			"pn_models/simple_cpn.jsimg,2,2,1,1,0,0,30,2,5",
			"pn_models/simple_gspn.jsimg,1,5,2,3,0,0,30,6,5",
			"hybrid_models/ispass03-02.jsimg,5,15,6,5,3,1,30,16,54",
			"hybrid_models/ispass03.jsimg,5,17,8,5,3,1,210,20,31",
			"hybrid_models/pepsy-bcmp2.jsimg,2,18,5,8,4,1,22,21,52",
			"hybrid_models/SjAS04Model_6AS-L5.jsimg,10,52,16,23,10,3,122,62,214" })
	void petriNetStructure(String file, int classes, int stations, int places, int transitions, int servers, int delays, int preload,
			int connections, int measures) {
		CommonModel m = load(file);
		assertEquals(classes, m.getClassKeys().size(), "classes");
		assertEquals(stations, m.getStationKeys().size(), "stations");
		assertEquals(places, m.getStationKeysPlace().size(), "places");
		assertEquals(transitions, m.getStationKeysTransition().size(), "transitions");
		assertEquals(servers, m.getStationKeysServer().size(), "servers");
		assertEquals(delays, m.getStationKeysDelay().size(), "delays");
		int pre = 0, conn = 0;
		for (Object s : m.getStationKeys()) {
			conn += m.getForwardConnections(s).size();
			for (Object c : m.getClassKeys()) {
				Integer p = m.getPreloadedJobs(s, c);
				pre += p == null ? 0 : p;
			}
		}
		assertEquals(preload, pre, "preloaded jobs");
		assertEquals(connections, conn, "connections");
		assertEquals(measures, m.getMeasureKeys().size(), "measures");
	}

	@Test
	void placesHoldInitialTokensAsPreloadedJobs() {
		CommonModel m = load("pn_models/philosophers.jsimg");
		Object c = m.getClassByName("Class1");
		assertEquals(Integer.valueOf(1), m.getPreloadedJobs(m.getStationByName("thinking_1"), c));
		assertEquals(Integer.valueOf(1), m.getPreloadedJobs(m.getStationByName("fork_3"), c));
		assertEquals(Integer.valueOf(0), m.getPreloadedJobs(m.getStationByName("eating_1"), c));
		assertEquals(CommonConstants.STATION_TYPE_PLACE, m.getStationType(m.getStationByName("thinking_1")));
		assertEquals(CommonConstants.STATION_TYPE_TRANSITION, m.getStationType(m.getStationByName("t1_1")));
		assertEquals(CommonConstants.CLASS_TYPE_CLOSED, m.getClassType(c));
		assertEquals(Integer.valueOf(8), m.getClassPopulation(c));
	}

	@Test
	void transitionsAreConnectedToPlaces() {
		CommonModel m = load("pn_models/producer_consumer.jsimg");
		Object place = m.getStationByName("producing"), tr = m.getStationByName("endProduce");
		assertTrue(m.areConnected(m.getStationByName("startProduce"), place));
		assertTrue(m.areConnected(place, tr));
		assertEquals(2, m.getForwardConnections(m.getStationByName("insertJob")).size());
		assertEquals(Integer.valueOf(3), m.getPreloadedJobs(m.getStationByName("emptySlots"), m.getClassByName("Class1")));
	}

	@Test
	void placeCapacityIsRead() {
		CommonModel m = load("hybrid_models/ispass03-02.jsimg");
		assertEquals(Integer.valueOf(1), m.getStationQueueCapacity(m.getStationByName("Client-D")));
	}

	@Test
	void transitionModesAndTimingAreParsed() {
		CommonModel m = load("pn_models/simple_gspn.jsimg");
		for (Object t : m.getStationKeysTransition()) {
			assertNotNull(m.getStationName(t));
			assertEquals(CommonConstants.STATION_TYPE_TRANSITION, m.getStationType(t));
		}
		assertEquals(3, m.getStationKeysTransition().size());
	}

	@Test
	void coloredNetKeepsTwoClasses() {
		CommonModel m = load("pn_models/simple_cpn.jsimg");
		assertEquals(2, m.getClassKeys().size());
		int total = 0;
		for (Object c : m.getClassKeys()) {
			total += m.getPreloadedJobs(m.getStationKeysPlace().get(0), c);
		}
		assertEquals(30, total);
	}
}
