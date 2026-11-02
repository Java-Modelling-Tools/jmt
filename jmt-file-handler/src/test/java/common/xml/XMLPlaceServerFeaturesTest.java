package common.xml;

import static common.xml.AdvancedFeatureSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import common.CommonConstants;
import common.definitions.CommonModel;

/** A Server (or Delay) fed by a Place works as a transition: enabling, inhibiting and resource conditions are written and read. */
class XMLPlaceServerFeaturesTest {

	/** Src -> S -> Out, plus a Place P feeding S; the server gets one mode with conditions on P. */
	private static CommonModel placeFedServer(String type) {
		CommonModel m = base();
		if (!SERVER.equals(type)) {
			m.setConnected(st(m, "S"), st(m, "Out"), false);
			add(m, "D", type);
			connect(m, "S", "D");
			connect(m, "D", "Out");
		}
		add(m, "P", CommonConstants.STATION_TYPE_PLACE);
		connect(m, "P", SERVER.equals(type) ? "S" : "D");
		return m;
	}

	@Test
	void serverFedByAPlaceKeepsItsConditions() throws Exception {
		CommonModel m = placeFedServer(SERVER);
		Object s = st(m, "S"), p = st(m, "P"), c = cl(m, "C");
		m.setEnablingCondition(s, 0, p, c, 2);
		m.setInhibitingCondition(s, 0, p, c, 5);
		m.setResourceCondition(s, 0, p, c, 3);

		Element queue = section(doc(m), "S", "Queue");
		assertNotNull(param(queue, "enablingConditions"));
		assertNotNull(param(queue, "inhibitingConditions"));
		assertNotNull(param(queue, "resourceConditions"));
		assertEquals("2", value(queue, "enablingEntry"));
		assertEquals("5", value(queue, "inhibitingEntry"));
		assertEquals("3", value(queue, "resourceEntry"));

		CommonModel back = roundTrip(m);
		Object bs = st(back, "S"), bp = st(back, "P"), bc = cl(back, "C");
		assertEquals(Integer.valueOf(2), back.getEnablingCondition(bs, 0, bp, bc));
		assertEquals(Integer.valueOf(5), back.getInhibitingCondition(bs, 0, bp, bc));
		assertEquals(Integer.valueOf(3), back.getResourceCondition(bs, 0, bp, bc));
	}

	@Test
	void delayFedByAPlaceKeepsItsConditions() throws Exception {
		CommonModel m = placeFedServer(CommonConstants.STATION_TYPE_DELAY);
		Object d = st(m, "D"), p = st(m, "P"), c = cl(m, "C");
		m.setEnablingCondition(d, 0, p, c, 4);
		m.setInhibitingCondition(d, 0, p, c, 6);
		m.setResourceCondition(d, 0, p, c, 1);
		CommonModel back = roundTrip(m);
		assertEquals(Integer.valueOf(4), back.getEnablingCondition(st(back, "D"), 0, st(back, "P"), cl(back, "C")));
		assertEquals(Integer.valueOf(6), back.getInhibitingCondition(st(back, "D"), 0, st(back, "P"), cl(back, "C")));
		assertEquals(Integer.valueOf(1), back.getResourceCondition(st(back, "D"), 0, st(back, "P"), cl(back, "C")));
	}
}
