package common.definitions;

import static common.definitions.ModelFixtures.addClass;
import static common.definitions.ModelFixtures.addStation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import common.CommonConstants;
import common.Defaults;

/** Characterization of CommonModel measure management. */
class CommonModelMeasuresTest {

	@Test
	void emptyModelHasNoMeasures() {
		assertTrue(ModelFixtures.emptyModel().getMeasureKeys().isEmpty());
	}

	@Test
	void unknownMeasureKeyGivesNeutralValues() {
		CommonModel m = ModelFixtures.emptyModel();
		Object bogus = Long.valueOf(999);
		assertNull(m.getMeasureType(bogus));
		assertNull(m.getMeasureStation(bogus));
		assertNull(m.getMeasureClass(bogus));
		assertNull(m.getMeasureAlpha(bogus));
		assertNull(m.getMeasurePrecision(bogus));
		m.setMeasureType("x", bogus);
		m.removeMeasure(bogus);
		assertFalse(m.toBeSaved());
	}

	@Test
	void addMeasureUsesDefaultAlphaAndPrecision() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		Object k = m.addMeasure(SimulationDefinition.MEASURE_U, q, c);
		assertEquals(SimulationDefinition.MEASURE_U, m.getMeasureType(k));
		assertSame(q, m.getMeasureStation(k));
		assertSame(c, m.getMeasureClass(k));
		assertEquals(Defaults.getAsDouble("measureAlpha"), m.getMeasureAlpha(k));
		assertEquals(Defaults.getAsDouble("measurePrecision"), m.getMeasurePrecision(k));
		assertFalse(m.getMeasureLog(k));
		assertEquals(2, m.getMeasureKeys().size());
	}

	@Test
	void addMeasureWithExplicitParameters() {
		CommonModel m = ModelFixtures.openOneStation();
		Object k = m.addMeasure(SimulationDefinition.MEASURE_RP, m.getStationKeysServer().get(0), m.getClassKeys().get(0),
				Double.valueOf(0.9), Double.valueOf(0.1), true);
		assertEquals(Double.valueOf(0.9), m.getMeasureAlpha(k));
		assertEquals(Double.valueOf(0.1), m.getMeasurePrecision(k));
		assertTrue(m.getMeasureLog(k));
	}

	@Test
	void measureSettersRoundTripAndMarkModelDirty() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		Object k = m.getMeasureKeys().get(0);
		Object q2 = addStation(m, "Q2", CommonConstants.STATION_TYPE_SERVER);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		m.resetSaveState();
		m.setMeasureType(SimulationDefinition.MEASURE_QT, k);
		assertEquals(SimulationDefinition.MEASURE_QT, m.getMeasureType(k));
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setMeasureType(SimulationDefinition.MEASURE_QT, k); // same value: not dirty
		assertFalse(m.toBeSaved());
		m.setMeasureStation(q2, k);
		m.setMeasureClass(c2, k);
		m.setMeasureAlpha(0.8, k);
		m.setMeasurePrecision(0.2, k);
		m.setMeasureLog(true, k);
		assertSame(q2, m.getMeasureStation(k));
		assertSame(c2, m.getMeasureClass(k));
		assertEquals(Double.valueOf(0.8), m.getMeasureAlpha(k));
		assertEquals(Double.valueOf(0.2), m.getMeasurePrecision(k));
		assertTrue(m.getMeasureLog(k));
		assertFalse(q == q2 || c == c2);
	}

	@Test
	void removeMeasure() {
		CommonModel m = ModelFixtures.openOneStation();
		Object k = m.getMeasureKeys().get(0);
		m.removeMeasure(k);
		assertTrue(m.getMeasureKeys().isEmpty());
		assertNull(m.getMeasureType(k));
		m.removeMeasure(k); // removing twice is harmless
	}

	@Test
	void deletingClassOrStationDropsOnlyTheirMeasures() {
		CommonModel m = ModelFixtures.openOneStation();
		Object q = m.getStationKeysServer().get(0);
		Object c = m.getClassKeys().get(0);
		Object q2 = addStation(m, "Q2", CommonConstants.STATION_TYPE_SERVER);
		Object c2 = addClass(m, "C2", CommonConstants.CLASS_TYPE_OPEN);
		Object onQ2C = m.addMeasure(SimulationDefinition.MEASURE_X, q2, c);
		Object onQC2 = m.addMeasure(SimulationDefinition.MEASURE_X, q, c2);
		Object onQ2C2 = m.addMeasure(SimulationDefinition.MEASURE_X, q2, c2);
		assertEquals(4, m.getMeasureKeys().size());
		m.deleteStation(q);
		assertEquals(java.util.Arrays.asList(onQ2C, onQ2C2), m.getMeasureKeys());
		m.deleteClass(c2);
		assertEquals(java.util.Arrays.asList(onQ2C), m.getMeasureKeys());
		assertNull(m.getMeasureType(onQC2));
	}

	@Test
	void globalMeasuresHaveNoStationOrClassRequirement() {
		CommonModel m = ModelFixtures.openOneStation();
		Object k = m.addMeasure(SimulationDefinition.MEASURE_S_RP, null, null);
		assertNull(m.getMeasureStation(k));
		assertNull(m.getMeasureClass(k));
		// null-station measures survive station deletion
		m.deleteStation(m.getStationKeysServer().get(0));
		assertTrue(m.getMeasureKeys().contains(k));
	}

	@Test
	void measureTypeClassification() {
		CommonModel m = ModelFixtures.emptyModel();
		String[] global = { SimulationDefinition.MEASURE_S_X, SimulationDefinition.MEASURE_S_RP, SimulationDefinition.MEASURE_S_CN,
				SimulationDefinition.MEASURE_S_DR, SimulationDefinition.MEASURE_S_BR, SimulationDefinition.MEASURE_S_RN,
				SimulationDefinition.MEASURE_S_RT, SimulationDefinition.MEASURE_S_P, SimulationDefinition.MEASURE_S_T,
				SimulationDefinition.MEASURE_S_E, SimulationDefinition.MEASURE_S_L };
		for (String t : global) {
			assertTrue(m.isGlobalMeasure(t), t);
			assertFalse(m.isSinkMeasure(t) || m.isFCRMeasure(t) || m.isFJMeasure(t), t);
		}
		assertTrue(m.isSinkMeasure(SimulationDefinition.MEASURE_RP_PER_SINK));
		assertTrue(m.isSinkMeasure(SimulationDefinition.MEASURE_X_PER_SINK));
		assertTrue(m.isFCRMeasure(SimulationDefinition.MEASURE_FCR_TW));
		assertTrue(m.isFCRMeasure(SimulationDefinition.MEASURE_FCR_MO));
		assertTrue(m.isFJMeasure(SimulationDefinition.MEASURE_FJ_CN));
		assertTrue(m.isFJMeasure(SimulationDefinition.MEASURE_FJ_RP));
		for (String t : new String[] { SimulationDefinition.MEASURE_QL, SimulationDefinition.MEASURE_U, SimulationDefinition.MEASURE_X }) {
			assertFalse(m.isGlobalMeasure(t) || m.isSinkMeasure(t) || m.isFCRMeasure(t) || m.isFJMeasure(t), t);
		}
	}
}
