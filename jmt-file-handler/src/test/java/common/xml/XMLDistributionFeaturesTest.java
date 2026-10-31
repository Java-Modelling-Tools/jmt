package common.xml;

import static common.xml.AdvancedFeatureSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import common.definitions.CommonModel;
import common.distributions.Burst;
import common.distributions.Distribution;
import common.distributions.Erlang;
import common.distributions.MAP;
import common.distributions.MMPP2;
import common.distributions.PhaseType;
import common.distributions.Replayer;

/** Distribution kinds with nested, matrix-valued or file parameters, as service times of the base server. */
class XMLDistributionFeaturesTest {

	private static Distribution make(String kind) {
		switch (kind) {
			case "Burst":
				return new Burst();
			case "MAP":
				return new MAP();
			case "PhaseType":
				return new PhaseType();
			case "MMPP2":
				return new MMPP2();
			case "Replayer":
				return new Replayer();
			case "Erlang":
				return new Erlang();
			default:
				throw new IllegalArgumentException(kind);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "Burst", "MAP", "PhaseType", "MMPP2", "Replayer", "Erlang" })
	void serviceDistributionKindRoundTripsAndKeepsItsName(String kind) throws Exception {
		CommonModel m = base();
		Distribution d = make(kind);
		m.setServiceTimeDistribution(st(m, "S"), cl(m, "C"), d);
		CommonModel back = roundTrip(m);
		Distribution got = (Distribution) back.getServiceTimeDistribution(st(back, "S"), cl(back, "C"));
		assertNotNull(got, kind);
		assertEquals(d.getName(), got.getName());
		assertEquals(d.getNumberOfParameters(), got.getNumberOfParameters());
	}

	@Test
	void sourceInterarrivalCanBeANestedDistribution() throws Exception {
		CommonModel m = base();
		Burst b = new Burst();
		m.setClassDistribution(cl(m, "C"), b);
		CommonModel back = roundTrip(m);
		assertTrue(((Distribution) back.getClassDistribution(cl(back, "C"))).getName().startsWith("Burst"));
	}
}
