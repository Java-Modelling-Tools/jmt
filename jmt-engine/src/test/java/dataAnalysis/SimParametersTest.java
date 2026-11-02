package dataAnalysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimParametersTest {

	@Test
	void defaultsMatchTheDocumentedValues() {
		SimParameters p = new SimParameters();
		assertEquals(128, p.getNumBatch());
		assertEquals(8, p.getBatchLen());
		assertEquals(0.01, p.getNullTestRate());
		assertEquals(0.005, p.getNullTestAlfa());
		assertEquals(QueueNet.SimulationDefaults.MAX_SAMPLES, p.getMaxSamples());
		assertEquals(0, p.getMinSamples());
		assertEquals(-1.0, p.getMaxSimulatedTime());
		assertEquals(-1, p.getMaxProcessedEvents());
		assertFalse(p.isDisableStatisticStop());
		assertNull(p.getLogPath());
		assertNull(p.getLogDelimiter());
		assertNull(p.getLogDecimalSeparator());
		assertNull(p.getLogReplaceMode());
		assertNull(p.getTimestampValue());
	}

	@Test
	void settersStoreValues() {
		SimParameters p = new SimParameters();
		p.setNumBatch(64);
		p.setBatchLen(4);
		p.setNullTestRate(0.02);
		p.setNullTestAlfa(0.01);
		p.setMaxSamples(5000);
		p.setMinSamples(100);
		p.setMaxSimulatedTime(12.5);
		p.setMaxProcessedEvents(99);
		p.setDisableStatisticStop(true);
		p.setLogPath("/tmp/x");
		p.setLogDelimiter(";");
		p.setLogDecimalSeparator(",");
		p.setLogReplaceMode("1");
		p.setTimestampValue("t");
		assertEquals(64, p.getNumBatch());
		assertEquals(4, p.getBatchLen());
		assertEquals(0.02, p.getNullTestRate());
		assertEquals(0.01, p.getNullTestAlfa());
		assertEquals(5000, p.getMaxSamples());
		assertEquals(100, p.getMinSamples());
		assertEquals(12.5, p.getMaxSimulatedTime());
		assertEquals(99, p.getMaxProcessedEvents());
		assertTrue(p.isDisableStatisticStop());
		assertEquals("/tmp/x", p.getLogPath());
		assertEquals(";", p.getLogDelimiter());
		assertEquals(",", p.getLogDecimalSeparator());
		assertEquals("1", p.getLogReplaceMode());
		assertEquals("t", p.getTimestampValue());
	}
}
