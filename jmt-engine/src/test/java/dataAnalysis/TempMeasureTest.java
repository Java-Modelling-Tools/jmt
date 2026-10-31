package dataAnalysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import QueueNet.NetSystem;
import QueueNet.QueueNetwork;
import QueueNet.SimConstants;

class TempMeasureTest {

	private Measure measure;

	@BeforeEach
	void setUp() {
		QueueNetwork network = new QueueNetwork("n");
		network.setNetSystem(new NetSystem());
		measure = new Measure("rt", 0.05, 0.05, 100000, false, null);
		measure.setNetwork(network);
	}

	@Test
	void snapshotsStaticPropertiesOfTheMeasure() {
		measure.measureTarget("st", "cl", SimConstants.RESPONSE_TIME);
		TempMeasure t = new TempMeasure(measure);
		assertEquals("rt", t.getName());
		assertEquals("st", t.getNodeName());
		assertEquals(SimConstants.NODE_TYPE_STATION, t.getNodeType());
		assertEquals("cl", t.getJobClass());
		assertEquals(SimConstants.MEASURE_RP, t.getMeasureType());
		assertEquals(0.05, t.getAlpha());
		assertEquals(0.05, t.getPrecision());
		assertNull(t.getLogFileName());
		assertFalse(t.isFinished());
		assertFalse(t.isSuccessful());
		assertFalse(t.receivedNoSamples());
		assertEquals(0, t.getNsamples());
	}

	@Test
	void measuresWithoutTargetHaveEmptyNodeAndClass() {
		TempMeasure t = new TempMeasure(measure);
		assertEquals("", t.getNodeName());
		assertEquals("", t.getNodeType());
		assertEquals("", t.getJobClass());
	}

	@Test
	void logFileNameComesFromTheMeasureOutput() {
		final File f = new File("some/where.csv");
		new MeasureOutput(measure) {
			public void write(double s, double w) {
			}

			public void finalizeMeasure() {
			}

			public File getOutputFile() {
				return f;
			}
		};
		assertEquals(f.getAbsolutePath(), new TempMeasure(measure).getLogFileName());
	}

	@Test
	void refreshCopiesRunningValuesAndFreezesOnceFinished() {
		TempMeasure t = new TempMeasure(measure);
		measure.update(2, 1);
		measure.update(4, 1);
		t.refreshMeasure();
		assertEquals(3.0, t.getTempMean(), 1e-12);
		assertEquals(2, t.getNsamples());
		assertEquals(0, t.getDiscarded());
		assertEquals(3.0, t.getLastIntervalAvgValue(), 1e-12);
		assertEquals(0.0, t.getSimTime());
		assertFalse(t.isFinished());

		Random r = new Random(42);
		while (!measure.update(-Math.log(1 - r.nextDouble()) * 2, 1)) {
			// feed
		}
		t.refreshMeasure();
		assertTrue(t.isFinished());
		assertTrue(t.isSuccessful());
		assertTrue(t.getUpperBound() > t.getTempMean());
		assertTrue(t.getLowerBound() < t.getTempMean());
		double frozen = t.getTempMean();
		measure.getAnalyzer().stopMeasure(false);
		t.refreshMeasure();
		assertEquals(frozen, t.getTempMean());
	}

	@Test
	void abortDelegatesToTheMeasure() {
		TempMeasure t = new TempMeasure(measure);
		assertTrue(t.abort());
		assertTrue(measure.hasBeenAborted());
		assertFalse(t.abort());
	}

	@Test
	void finishedByNoSamplesIsFlagged() {
		TempMeasure t = new TempMeasure(measure);
		measure.stop_NoSamples();
		t.refreshMeasure();
		assertTrue(t.isFinished());
		assertTrue(t.receivedNoSamples());
	}
}
