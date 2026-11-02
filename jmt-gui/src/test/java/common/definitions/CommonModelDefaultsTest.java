package common.definitions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import common.Defaults;

/** Characterization of the simulation parameters and flags of a fresh CommonModel (read back from Defaults). */
class CommonModelDefaultsTest {

	@Test
	void freshModelTakesSimulationParametersFromDefaults() {
		CommonModel m = ModelFixtures.emptyModel();
		assertEquals(Defaults.getAsLong("simulationSeed"), m.getSimulationSeed());
		assertEquals(Defaults.getAsBoolean("isSimulationSeedRandom"), m.getUseRandomSeed());
		assertEquals(Defaults.getAsDouble("simulationMaxDuration"), m.getMaximumDuration());
		assertEquals(Defaults.getAsDouble("maxSimulatedTime"), m.getMaxSimulatedTime());
		assertEquals(Defaults.getAsInteger("maxSimulationSamples"), m.getMaxSimulationSamples());
		assertEquals(Defaults.getAsBoolean("isStatisticDisabled"), m.getDisableStatistic());
		assertEquals(Defaults.getAsInteger("maxSimulationEvents"), m.getMaxSimulationEvents());
		assertEquals(Defaults.getAsDouble("simulationPolling"), m.getPollingInterval());
	}

	@Test
	void freshModelFlags() {
		CommonModel m = ModelFixtures.emptyModel();
		assertFalse(m.toBeSaved());
		assertFalse(m.isParametricAnalysisEnabled());
		assertNull(m.getParametricAnalysisModel());
		assertFalse(m.containsSimulationResults());
		assertNull(m.getSimulationResults());
		assertNotNull(m.getLoggingGlbParameter("path"));
	}

	@Test
	void simulationSettersRoundTripAndMarkDirtyOnlyOnChange() {
		CommonModel m = ModelFixtures.emptyModel();
		m.setSimulationSeed(m.getSimulationSeed());
		m.setUseRandomSeed(m.getUseRandomSeed());
		m.setMaximumDuration(m.getMaximumDuration());
		m.setMaxSimulatedTime(m.getMaxSimulatedTime());
		m.setMaxSimulationSamples(m.getMaxSimulationSamples());
		m.setDisableStatistic(m.getDisableStatistic());
		m.setMaxSimulationEvents(m.getMaxSimulationEvents());
		m.setPollingInterval(m.getPollingInterval());
		assertFalse(m.toBeSaved());

		m.setSimulationSeed(Long.valueOf(42));
		assertEquals(Long.valueOf(42), m.getSimulationSeed());
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		assertFalse(m.toBeSaved());

		m.setUseRandomSeed(!m.getUseRandomSeed());
		m.setMaximumDuration(Double.valueOf(12));
		m.setMaxSimulatedTime(Double.valueOf(34));
		m.setMaxSimulationSamples(Integer.valueOf(56));
		m.setDisableStatistic(!m.getDisableStatistic());
		m.setMaxSimulationEvents(Integer.valueOf(78));
		m.setPollingInterval(Double.valueOf(9));
		assertEquals(Double.valueOf(12), m.getMaximumDuration());
		assertEquals(Double.valueOf(34), m.getMaxSimulatedTime());
		assertEquals(Integer.valueOf(56), m.getMaxSimulationSamples());
		assertEquals(Integer.valueOf(78), m.getMaxSimulationEvents());
		assertEquals(Double.valueOf(9), m.getPollingInterval());
	}

	@Test
	void eachSimulationSetterMarksDirty() {
		CommonModel m = ModelFixtures.emptyModel();
		m.setUseRandomSeed(!m.getUseRandomSeed());
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setMaximumDuration(Double.valueOf(12));
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setMaxSimulatedTime(Double.valueOf(34));
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setMaxSimulationSamples(Integer.valueOf(56));
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setDisableStatistic(!m.getDisableStatistic());
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setMaxSimulationEvents(Integer.valueOf(78));
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setPollingInterval(Double.valueOf(9));
		assertTrue(m.toBeSaved());
	}

	@Test
	void animationAndParametricFlags() {
		CommonModel m = ModelFixtures.emptyModel();
		// pinned quirk: animation is a JSimGraphModel feature: the base class ignores it
		m.setAnimationEnabled(true);
		assertFalse(m.isAnimationEnabled());
		m.setParametricAnalysisEnabled(true);
		assertTrue(m.isParametricAnalysisEnabled());
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		m.setParametricAnalysisEnabled(true);
		assertFalse(m.toBeSaved());
	}

	@Test
	void setSaveChangedAndResetSaveState() {
		CommonModel m = ModelFixtures.emptyModel();
		m.setSaveChanged();
		assertTrue(m.toBeSaved());
		m.resetSaveState();
		assertFalse(m.toBeSaved());
	}

	@Test
	void classDefaultsComeFromDefaults() {
		CommonModel m = ModelFixtures.emptyModel();
		Object k = m.addClass("C", common.CommonConstants.CLASS_TYPE_OPEN, Integer.valueOf(0), Defaults.getAsDouble("classSoftDeadline"),
				null, Defaults.getAsNewInstance("classDistribution"));
		assertEquals(Defaults.getAsDouble("classSoftDeadline"), m.getClassSoftDeadline(k));
		assertEquals(Defaults.getAsNewInstance("classDistribution").getClass(), m.getClassDistribution(k).getClass());
	}
}
