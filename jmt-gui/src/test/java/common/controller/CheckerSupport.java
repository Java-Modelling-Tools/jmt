package common.controller;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.definitions.ModelFixtures;

/** Shared builders for the ModelChecker characterization tests. */
final class CheckerSupport {

	private CheckerSupport() {
	}

	static ModelChecker normal(CommonModel m) {
		return new ModelChecker(m, m, m, m, false);
	}

	static ModelChecker jmva(CommonModel m) {
		return new ModelChecker(m, m, m, m, true);
	}

	static Object firstClass(CommonModel m) {
		return m.getClassKeys().get(0);
	}

	static Object firstQueue(CommonModel m) {
		return m.getStationKeysServer().get(0);
	}

	/** Open class; Source -> Fork -> mid -> Join -> Sink, no measures. */
	static CommonModel forkJoinAround(String midType) {
		CommonModel m = ModelFixtures.emptyModel();
		Object cls = ModelFixtures.addClass(m, "C", CommonConstants.CLASS_TYPE_OPEN);
		Object src = ModelFixtures.addStation(m, "Src", CommonConstants.STATION_TYPE_SOURCE);
		Object fork = ModelFixtures.addStation(m, "F", CommonConstants.STATION_TYPE_FORK);
		Object mid = ModelFixtures.addStation(m, "Mid", midType);
		Object join = ModelFixtures.addStation(m, "J", CommonConstants.STATION_TYPE_JOIN);
		Object out = ModelFixtures.addStation(m, "Out", CommonConstants.STATION_TYPE_SINK);
		m.setClassRefStation(cls, src);
		ModelFixtures.link(m, src, fork);
		ModelFixtures.link(m, fork, mid);
		ModelFixtures.link(m, mid, join);
		ModelFixtures.link(m, join, out);
		return m;
	}

	/** n closed classes (reference station D); D -> Q -> D. */
	static CommonModel closedWithClasses(int n) {
		CommonModel m = ModelFixtures.emptyModel();
		Object d = ModelFixtures.addStation(m, "D", CommonConstants.STATION_TYPE_DELAY);
		Object q = ModelFixtures.addStation(m, "Q", CommonConstants.STATION_TYPE_SERVER);
		for (int i = 0; i < n; i++) {
			Object c = ModelFixtures.addClass(m, "C" + i, CommonConstants.CLASS_TYPE_CLOSED);
			m.setClassRefStation(c, d);
		}
		ModelFixtures.link(m, d, q);
		ModelFixtures.link(m, q, d);
		return m;
	}
}
