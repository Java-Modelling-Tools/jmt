package common.definitions;

import java.util.ArrayList;

import common.CommonConstants;
import common.Defaults;
import common.distributions.Exponential;

/** Small populated CommonModel builders for headless tests; every call returns a fresh, independent model. */
public final class ModelFixtures {

	private ModelFixtures() {
	}

	public static CommonModel emptyModel() {
		return new CommonModel();
	}

	public static Object addClass(CommonModel m, String name, int type) {
		Integer population = type == CommonConstants.CLASS_TYPE_CLOSED ? Integer.valueOf(5) : null;
		return m.addClass(name, type, Integer.valueOf(0), Double.valueOf(0), population, new Exponential());
	}

	public static Object addStation(CommonModel m, String name, String type) {
		return m.addStation(name, type, 1, new ArrayList<ServerType>());
	}

	public static void link(CommonModel m, Object from, Object to) {
		if (!m.setConnected(from, to, true)) {
			throw new IllegalStateException("cannot connect " + m.getStationName(from) + " -> " + m.getStationName(to));
		}
	}

	/** Open class "C"; Source -> Queue -> Sink. */
	public static CommonModel openOneStation() {
		CommonModel m = emptyModel();
		Object cls = addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object src = addStation(m, "Src", CommonConstants.STATION_TYPE_SOURCE);
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		m.setClassRefStation(cls, src);
		m.addMeasure(SimulationDefinition.MEASURE_QL, q, cls);
		Object out = addStation(m, "Out", CommonConstants.STATION_TYPE_SINK);
		link(m, src, q);
		link(m, q, out);
		return m;
	}

	/** Closed class "C" (population 5); Delay -> Queue -> Delay. */
	public static CommonModel closedTwoStations() {
		CommonModel m = emptyModel();
		Object cls = addClass(m, "C", CommonConstants.CLASS_TYPE_CLOSED);
		Object d = addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		Object q = addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		m.setClassRefStation(cls, d);
		m.addMeasure(SimulationDefinition.MEASURE_QL, q, cls);
		link(m, d, q);
		link(m, q, d);
		return m;
	}

	/** Open class; Source -> Fork -> (Q1, Q2) -> Join -> Sink. */
	public static CommonModel withForkJoin() {
		CommonModel m = emptyModel();
		Object cls = addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object src = addStation(m, "Src", CommonConstants.STATION_TYPE_SOURCE);
		Object fork = addStation(m, "F", CommonConstants.STATION_TYPE_FORK);
		Object q1 = addStation(m, "Q1", CommonConstants.STATION_TYPE_SERVER);
		Object q2 = addStation(m, "Q2", CommonConstants.STATION_TYPE_SERVER);
		Object join = addStation(m, "J", CommonConstants.STATION_TYPE_JOIN);
		Object out = addStation(m, "Out", CommonConstants.STATION_TYPE_SINK);
		m.setClassRefStation(cls, src);
		m.addMeasure(SimulationDefinition.MEASURE_QL, q1, cls);
		link(m, src, fork);
		link(m, fork, q1);
		link(m, fork, q2);
		link(m, q1, join);
		link(m, q2, join);
		link(m, join, out);
		return m;
	}

	/** openOneStation() plus one finite-capacity region containing the queue. */
	public static CommonModel withBlockingRegion() {
		CommonModel m = openOneStation();
		Object region = m.addBlockingRegion("R", Defaults.get("blockingRegionType"));
		if (!m.addRegionStation(region, m.getStationKeysServer().get(0))) {
			throw new IllegalStateException("queue not accepted by region");
		}
		return m;
	}
}
