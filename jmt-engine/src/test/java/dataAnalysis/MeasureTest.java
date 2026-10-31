package dataAnalysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import QueueNet.NetSystem;
import QueueNet.QueueNetwork;
import QueueNet.SimConstants;

class MeasureTest {

	/** Output that just records what the measure sends to it. */
	static class RecordingOutput extends MeasureOutput {
		final List<double[]> samples = new ArrayList<double[]>();
		int finalized;

		RecordingOutput(Measure m) {
			super(m);
		}

		@Override
		public void write(double sample, double weight) {
			samples.add(new double[] { sample, weight });
		}

		@Override
		public void finalizeMeasure() {
			finalized++;
		}

		@Override
		public File getOutputFile() {
			return null;
		}
	}

	private QueueNetwork network;

	@BeforeEach
	void setUp() {
		network = new QueueNetwork("net");
		network.setNetSystem(new NetSystem());
	}

	private Measure measure(boolean verbose) {
		Measure m = new Measure("m", 0.05, 0.05, 100000, verbose, null);
		m.setNetwork(network);
		return m;
	}

	/** Feeds exponential(mean 2) samples until the measure finishes; returns the number fed. */
	private static int feedUntilDone(Measure m, long seed) {
		Random r = new Random(seed);
		int n = 0;
		while (!m.update(-Math.log(1 - r.nextDouble()) * 2, 1) && n < 100000) {
			n++;
		}
		return n + 1;
	}

	@Test
	void constructorChoosesAnalyzerByQuantileRequest() {
		assertTrue(new Measure("a", 0.05, 0.05, 10000, false, null).getAnalyzer() instanceof NewDynamicDataAnalyzer);
		assertTrue(new Measure("a", 0.05, 0.05, 10000, false, new double[0]).getAnalyzer() instanceof NewDynamicDataAnalyzer);
		Measure q = new Measure("a", 0.05, 0.05, 10000, false, new double[] { 0.5 });
		assertTrue(q.getAnalyzer() instanceof QuantileDataAnalyzer);
		assertNull(new Measure("a", 0.05, 0.05, 10000, false, null).getQuantileResults());
	}

	@Test
	void storesTargetNetworkAndSimpleProperties() {
		Measure m = measure(true);
		m.measureTarget("node", "class", SimConstants.QUEUE_LENGTH);
		assertEquals("m", m.getName());
		assertEquals("node", m.getNodeName());
		assertEquals("class", m.getJobClassName());
		assertEquals(SimConstants.QUEUE_LENGTH, m.getMeasureType());
		assertTrue(m.getVerbose());
		assertSame(network, m.getNetwork());
		assertSame(network.getNetSystem(), m.getNetSystem());
		assertEquals(100000, m.getMaxSamples());
		assertEquals(0.0, m.getSamplesAnalyzedPercentage());
		assertFalse(m.hasFinished());
		assertFalse(m.hasBeenAborted());
		assertFalse(m.receivedNoSamples());
	}

	@Test
	void quantileMeasureReportsRequestedQuantiles() {
		Measure m = new Measure("q", 0.05, 0.5, 100000, false, new double[] { 0.5 });
		m.setNetwork(network);
		feedUntilDone(m, 42);
		double[] q = m.getQuantileResults();
		assertEquals(1, q.length);
		assertEquals(Math.log(2) * 2, q[0], 0.3); // median of Exp(mean 2)
	}

	@Test
	void creatingAnOutputRegistersItOnTheMeasure() {
		Measure m = measure(false);
		assertNull(m.getOutput());
		RecordingOutput out = new RecordingOutput(m);
		assertSame(out, m.getOutput());
	}

	@Test
	void beforeAnySampleLimitsAndValuesAreZero() {
		Measure m = measure(false);
		assertEquals(0.0, m.getLowerLimit());
		assertEquals(0.0, m.getUpperLimit());
		assertEquals(0.0, m.getMeanValue());
		assertEquals(0.0, m.getLastIntervalAvgValue());
		assertEquals(0.0, m.getMaxPrecisionPercentage());
		assertFalse(m.getSuccess());
	}

	@Test
	void updateRunsUntilConfidenceIsReachedThenReportsMeanAndInterval() {
		Measure m = measure(false);
		int fed = feedUntilDone(m, 42);
		assertTrue(m.hasFinished());
		assertTrue(m.getSuccess());
		assertEquals(2.0, m.getMeanValue(), 0.15);
		assertEquals(m.getMeanValue(), m.getEstimatedMeanValue(), 1e-12);
		assertTrue(m.getLowerLimit() < m.getMeanValue());
		assertTrue(m.getUpperLimit() > m.getMeanValue());
		assertEquals(m.getAnalyzedSamples() + m.getDiscardedSamples() > 0, true);
		assertEquals((double) m.getAnalyzedSamples() / m.getMaxSamples(), m.getSamplesAnalyzedPercentage(), 1e-12);
		assertTrue(m.getMaxPrecisionPercentage() > 1.0, "precision reached is better than requested");
		assertTrue(fed < 100000);
		// further samples are ignored
		double mean = m.getMeanValue();
		assertTrue(m.update(1000, 1));
		assertEquals(mean, m.getMeanValue(), 0.0);
	}

	@Test
	void verboseMeasureForwardsEverySampleAndFinalizesOnce() {
		Measure m = measure(true);
		RecordingOutput out = new RecordingOutput(m);
		int fed = feedUntilDone(m, 42);
		assertEquals(fed, out.samples.size());
		assertEquals(1, out.finalized);
		assertEquals(1.0, out.samples.get(0)[1]);
		m.update(5, 1);
		assertEquals(fed, out.samples.size(), "no more writes after the measure finished");
		assertEquals(1, out.finalized);
	}

	@Test
	void nonVerboseMeasureOnlyWritesTheLastSample() {
		Measure m = measure(false);
		RecordingOutput out = new RecordingOutput(m);
		feedUntilDone(m, 42);
		assertEquals(1, out.samples.size());
		assertEquals(1, out.finalized);
	}

	@Test
	void zeroSamplesProduceAZeroMeasure() {
		Measure m = measure(false);
		int fed = 0;
		while (!m.update(0, 1)) {
			fed++;
		}
		assertEquals(1000, fed + 1);
		assertTrue(m.getAnalyzer().isZero());
		assertTrue(m.getSuccess());
		assertEquals(0.0, m.getLowerLimit());
		assertEquals(0.0, m.getUpperLimit());
		assertEquals(0.0, m.getMeanValue());
	}

	@Test
	void lastIntervalAverageIsWeightedAndResets() {
		Measure m = measure(false);
		m.update(2, 1);
		m.update(4, 3);
		assertEquals(3.5, m.getLastIntervalAvgValue(), 1e-12);
		assertEquals(0.0, m.getLastIntervalAvgValue(), 0.0);
		assertEquals(0.0, m.getSimTime());
	}

	@Test
	void abortingStopsTheMeasureOnlyOnce() {
		Measure m = measure(false);
		assertTrue(m.abortMeasure());
		assertTrue(m.hasBeenAborted());
		assertTrue(m.hasFinished());
		assertFalse(m.abortMeasure());
	}

	@Test
	void abortingAFinishedMeasureDoesNothing() {
		Measure m = measure(false);
		feedUntilDone(m, 42);
		assertFalse(m.abortMeasure());
		assertFalse(m.hasBeenAborted());
	}

	@Test
	void measureIsDeadAfterThreeConsecutiveChecksWithoutSamples() {
		Measure m = measure(false);
		assertFalse(m.testDeadMeasure());
		assertFalse(m.testDeadMeasure());
		m.update(1, 1); // a sample resets the dead counter
		assertFalse(m.testDeadMeasure());
		assertFalse(m.testDeadMeasure());
		assertTrue(m.testDeadMeasure());
		assertTrue(m.receivedNoSamples());
		assertTrue(m.hasFinished());
		assertTrue(m.getSuccess());
		assertEquals(0.0, m.getMeanValue());
		assertEquals(0.0, m.getEstimatedMeanValue());
	}

	@Test
	void setSimParametersReachesTheAnalyzer() {
		Measure m = measure(false);
		SimParameters p = new SimParameters();
		p.setMaxSamples(7777);
		m.setSimParameters(p);
		assertEquals(7777, m.getMaxSamples());
	}

	@Test
	void residenceTimeMeasureWithoutVisitsKeepsUnitScaleFactor() throws Exception {
		QueueNet.StrategyFixtures f = new QueueNet.StrategyFixtures(QueueNet.StrategyFixtures.classes(0),
				new NetStrategies.ServiceStrategy[] { new QueueNet.StrategyFixtures.ScriptedService(1) });
		Measure m = new Measure("rd", 0.05, 0.05, 10000, false, null);
		f.network.addMeasure(m);
		m.measureTarget("station", null, SimConstants.RESIDENCE_TIME);
		assertFalse(m.update(3.0, 1, null, 0));
		assertFalse(m.update(5.0, 1, "station", 7));
		assertEquals(4.0, m.getEstimatedMeanValue(), 1e-12);
	}

	@Test
	void inverseMeasureReportsReciprocalOfTheAnalyzedMean() {
		InverseMeasure m = new InverseMeasure("x", 0.05, 0.05, 100000, false);
		m.setNetwork(network);
		assertEquals(0.0, m.getMeanValue());
		assertEquals(0.0, m.getEstimatedMeanValue());
		Random r = new Random(5);
		while (!m.update(0.5 * (1 + 0.2 * r.nextGaussian()), 1)) {
			// feed
		}
		assertTrue(m.getSuccess());
		assertEquals(2.0, m.getMeanValue(), 0.1);
		assertEquals(2.0, m.getEstimatedMeanValue(), 0.1);
		assertTrue(m.getLowerLimit() < m.getMeanValue());
		assertTrue(m.getUpperLimit() > m.getMeanValue());
		assertEquals(m.getAnalyzer().getMean(), 1.0 / m.getMeanValue(), 1e-12);
	}

	@Test
	void inverseMeasureLastIntervalIsReciprocalOfAverage() {
		InverseMeasure m = new InverseMeasure("x", 0.05, 0.05, 10000, false);
		m.setNetwork(network);
		m.update(0.25, 1);
		m.update(0.25, 1);
		assertEquals(4.0, m.getLastIntervalAvgValue(), 1e-12);
	}

	@Test
	void inverseMeasureWithNoSamplesIsZeroEverywhere() {
		InverseMeasure m = new InverseMeasure("x", 0.05, 0.05, 10000, false);
		m.stop_NoSamples();
		assertEquals(0.0, m.getMeanValue());
		assertEquals(0.0, m.getLowerLimit());
		assertEquals(0.0, m.getUpperLimit());
		assertEquals(0.0, m.getLastIntervalAvgValue());
	}

	@Test
	void abortedInverseMeasureWithoutDataHasZeroMean() {
		InverseMeasure m = new InverseMeasure("x", 0.05, 0.05, 10000, false);
		m.abortMeasure();
		assertEquals(0.0, m.getMeanValue());
	}
}
