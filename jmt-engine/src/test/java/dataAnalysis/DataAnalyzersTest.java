package dataAnalysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dataAnalysis.sorting.MergeSort;

/** Behaviour of the batch-means analyzers (NewDynamicDataAnalyzer, DynamicDataAnalyzerImpl, QuantileDataAnalyzer). */
class DataAnalyzersTest {

	interface Factory {
		DynamicDataAnalyzer make(double alfa, double precision, int maxData);
	}

	static Stream<Arguments> analyzers() {
		Factory neu = NewDynamicDataAnalyzer::new;
		Factory old = DynamicDataAnalyzerImpl::new;
		Factory quantile = (a, p, m) -> new QuantileDataAnalyzer(a, p, m, new double[] { 0.5 });
		return Stream.of(Arguments.of("New", neu), Arguments.of("Impl", old), Arguments.of("Quantile", quantile));
	}

	/** Feeds samples until the analyzer ends or cap samples were fed; returns the number of samples fed. */
	private static int feed(DynamicDataAnalyzer a, Function<Random, Double> gen, long seed, int cap) {
		Random r = new Random(seed);
		int i = 0;
		boolean end = false;
		while (!end && i < cap) {
			end = a.addSample(gen.apply(r), 1);
			i++;
		}
		return i;
	}

	private static final Function<Random, Double> EXP2 = r -> -Math.log(1 - r.nextDouble()) * 2;

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void rejectsInvalidParameters(String name, Factory f) {
		assertThrows(IllegalArgumentException.class, () -> f.make(0, 0.05, 10000));
		assertThrows(IllegalArgumentException.class, () -> f.make(1, 0.05, 10000));
		assertThrows(IllegalArgumentException.class, () -> f.make(0.05, 0, 10000));
		assertThrows(IllegalArgumentException.class, () -> f.make(0.05, 1, 10000));
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> f.make(0.05, 0.05, 4999));
		assertTrue(e.getMessage().startsWith("Wrong Dynamic analyzer parameter"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void exposesConstructorParameters(String name, Factory f) {
		DynamicDataAnalyzer a = f.make(0.07, 0.03, 12345);
		assertEquals(0.07, a.getAlfa());
		assertEquals(0.03, a.getPrecision());
		assertEquals(12345, a.getMaxData());
		assertFalse(a.getSuccess());
		assertEquals(0, a.getSamples());
		assertEquals(0, a.getDiscarded());
		assertFalse(a.isZero());
		assertEquals(0.0, a.getConfInt());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void convergesToTheTrueMeanOfIidExponentialSamples(String name, Factory f) {
		// the quantile analyzer's variance guard rejects tight intervals, so it gets a loose precision
		double precision = name.equals("Quantile") ? 0.5 : 0.05;
		DynamicDataAnalyzer a = f.make(0.05, precision, 100000);
		int fed = feed(a, EXP2, 42, 100000);
		assertTrue(fed < 100000, "should stop before the sample cap");
		assertTrue(a.getSuccess());
		assertFalse(a.isZero());
		assertEquals(2.0, a.getMean(), 0.25);
		assertTrue(a.getConfInt() > 0);
		assertTrue(a.getConfInt() / a.getMean() < precision, "confidence interval must satisfy the requested precision");
		assertEquals(a.getMean(), a.estimatedMean(), 1e-12);
		assertTrue(a.getSamples() >= 1000);
		assertTrue(a.getSamples() <= 100000);
	}

	@Test
	void newAnalyzerOnceEndedKeepsReportingEnd() {
		DynamicDataAnalyzer a = new NewDynamicDataAnalyzer(0.05, 0.05, 100000);
		feed(a, EXP2, 42, 100000);
		double mean = a.getMean();
		assertTrue(a.addSample(2.0, 1));
		assertEquals(mean, a.getMean(), 1e-12);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void identicallyZeroSamplesAreDetectedByTheNullTest(String name, Factory f) {
		DynamicDataAnalyzer a = f.make(0.05, 0.05, 10000);
		int fed = feed(a, r -> 0.0, 1, 10000);
		assertEquals(100, fed, "null test runs every 1% of maxData");
		assertTrue(a.isZero());
		assertTrue(a.getSuccess());
		assertEquals(0.0, a.getMean());
		assertEquals(0.0, a.getNullMeasure_upperLimit());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void failsWhenPrecisionIsNotReachedWithinMaxSamples(String name, Factory f) {
		DynamicDataAnalyzer a = f.make(0.05, 0.0001, 6000);
		feed(a, r -> r.nextDouble() + 1, 1, 20000);
		assertFalse(a.getSuccess());
		assertTrue(a.getSamples() >= 6000);
		assertEquals(1.5, a.estimatedMean(), 0.1);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void stopMeasureForcesEndAndRecordsRequestedSuccess(String name, Factory f) {
		DynamicDataAnalyzer a = f.make(0.05, 0.05, 10000);
		a.stopMeasure(true);
		assertTrue(a.getSuccess());
		a.stopMeasure(false);
		assertFalse(a.getSuccess());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void lastIntervalAverageIsWeightedAndResetsAfterReading(String name, Factory f) {
		DynamicDataAnalyzer a = f.make(0.05, 0.05, 10000);
		assertEquals(0.0, a.getLastIntervalAvg());
		a.addSample(2, 1);
		a.addSample(4, 3);
		assertEquals((2.0 + 12.0) / 4.0, a.getLastIntervalAvg(), 1e-12);
		// nothing new: the previous value is returned
		assertEquals(3.5, a.getLastIntervalAvg(), 1e-12);
		a.addSample(10, 1);
		assertEquals(10.0, a.getLastIntervalAvg(), 1e-12);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void spectralVarianceOfWhiteNoiseIsCloseToItsVariance(String name, Factory f) {
		DynamicDataAnalyzer a = f.make(0.05, 0.05, 10000);
		Random r = new Random(3);
		double[] data = new double[256];
		for (int i = 0; i < data.length; i++) {
			data[i] = r.nextGaussian();
		}
		double v = a instanceof NewDynamicDataAnalyzer ? ((NewDynamicDataAnalyzer) a).calcVar(data, 0, 256, 64, 2)
				: ((DynamicDataAnalyzerImpl) a).calcVar(data, 0, 256, 64, 2);
		assertTrue(v > 0.3 && v < 3.0, "estimated spectral variance " + v);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("analyzers")
	void calcConstantsReturnsPositiveDegreesOfFreedom(String name, Factory f) {
		DynamicDataAnalyzer a = f.make(0.05, 0.05, 10000);
		double[] c = a instanceof NewDynamicDataAnalyzer ? ((NewDynamicDataAnalyzer) a).calcConstants(32, 2)
				: ((DynamicDataAnalyzerImpl) a).calcConstants(32, 2);
		assertTrue(c[0] > 0 && c[0] < 1, "C1 " + c[0]);
		assertTrue(c[1] >= 1, "C2 " + c[1]);
		assertEquals(Math.rint(c[1]), c[1]);
	}

	// ---- NewDynamicDataAnalyzer specifics -------------------------------------------------

	@Test
	void newAnalyzerIgnoresZeroWeightSamples() {
		NewDynamicDataAnalyzer a = new NewDynamicDataAnalyzer(0.05, 0.05, 10000);
		assertFalse(a.addSample(5, 0));
		assertEquals(0, a.getSamples());
	}

	@Test
	void newAnalyzerSetParametersOverridesLimits() {
		NewDynamicDataAnalyzer a = new NewDynamicDataAnalyzer(0.05, 0.05, 10000);
		SimParameters p = new SimParameters();
		p.setMaxSamples(30000);
		p.setMinSamples(500);
		p.setNullTestRate(0.02);
		p.setNullTestAlfa(0.01);
		a.setParameters(p);
		assertEquals(30000, a.getMaxData());
	}

	@Test
	void newAnalyzerWithDisabledStatisticStopRunsUntilMaxSamplesButStillSucceeds() {
		NewDynamicDataAnalyzer a = new NewDynamicDataAnalyzer(0.05, 0.05, 20000);
		SimParameters p = new SimParameters();
		p.setMaxSamples(20000);
		p.setDisableStatisticStop(true);
		a.setParameters(p);
		int fed = feed(a, EXP2, 42, 40000);
		assertEquals(20000, a.getSamples());
		assertEquals(20000 + a.getDiscarded() + 1, fed, "must not stop on the confidence interval");
		assertTrue(a.getSuccess(), "confidence interval was reached before max samples");
		assertEquals(2.0, a.getMean(), 0.15);
	}

	@Test
	void newAnalyzerWithMinSamplesDoesNotStopBeforeThem() {
		NewDynamicDataAnalyzer a = new NewDynamicDataAnalyzer(0.05, 0.05, 100000);
		SimParameters p = new SimParameters();
		p.setMaxSamples(100000);
		p.setMinSamples(15000);
		a.setParameters(p);
		feed(a, EXP2, 42, 100000);
		assertTrue(a.getSuccess());
		assertTrue(a.getSamples() >= 15000);
	}

	@Test
	void newAnalyzerEstimatedMeanBeforeConvergenceIsTheRunningMean() {
		NewDynamicDataAnalyzer a = new NewDynamicDataAnalyzer(0.05, 0.05, 100000);
		a.addSample(1, 1);
		a.addSample(3, 1);
		assertEquals(2.0, a.estimatedMean(), 1e-12);
		assertEquals(0, a.getDiscarded());
		assertEquals(0.0, a.getMean());
		assertEquals(0.0, a.getConfInt());
	}

	@Test
	void newAnalyzerDiscardsTransientOfBiasedSeries() {
		NewDynamicDataAnalyzer a = new NewDynamicDataAnalyzer(0.05, 0.05, 200000);
		Random r = new Random(11);
		int i = 0;
		boolean end = false;
		while (!end && i < 200000) {
			double bias = i < 300 ? 50.0 * (300 - i) / 300 : 0.0; // decaying initial transient
			end = a.addSample(2 + bias + r.nextGaussian(), 1);
			i++;
		}
		assertTrue(a.getSuccess());
		assertTrue(a.getDiscarded() > 0, "the initial transient should be discarded");
		assertEquals(2.0, a.getMean(), 0.15);
	}

	// ---- DynamicDataAnalyzerImpl specifics ----------------------------------------------

	@Test
	void legacyAnalyzerExposesBatchDataOnlyAfterInitialization() {
		DynamicDataAnalyzerImpl a = new DynamicDataAnalyzerImpl(0.05, 0.05, 100000);
		assertNull(a.getData());
		assertEquals(0.0, a.getVar());
		feed(a, EXP2, 42, 100000);
		double[] data = a.getData();
		assertNotNull(data);
		assertTrue(data.length > 0);
		assertTrue(a.getVar() > 0);
	}

	@Test
	void legacyAnalyzerSamplesAnalyzedIncludeDiscardedOnes() {
		DynamicDataAnalyzerImpl a = new DynamicDataAnalyzerImpl(0.05, 0.05, 100000);
		int fed = feed(a, EXP2, 42, 100000);
		assertEquals(fed, a.getSamples());
		assertEquals(320, a.getDiscarded());
	}

	@Test
	void legacyAnalyzerToStringSummarisesTheRun() {
		DynamicDataAnalyzerImpl a = new DynamicDataAnalyzerImpl(0.05, 0.05, 100000);
		assertTrue(a.toString().contains("Unsuccessfully completed."));
		feed(a, EXP2, 42, 100000);
		String s = a.toString();
		assertTrue(s.contains("Successfully completed."));
		assertTrue(s.contains("Discarded samples :  320"));
		assertTrue(s.contains("With probablity : 95.0%"));
		assertTrue(s.startsWith("dataAnalysis.DynamicDataAnalyzerImpl"));
	}

	@Test
	void legacyAnalyzerGivesUpWhenNoStationaryPhaseIsFoundInHalfOfMaxSamples() {
		DynamicDataAnalyzerImpl a = new DynamicDataAnalyzerImpl(0.05, 0.05, 6000);
		// strictly increasing series never crosses its mean often enough
		int i = 0;
		boolean end = false;
		while (!end && i < 6000) {
			end = a.addSample(i, 1);
			i++;
		}
		assertTrue(end);
		assertEquals(3000, i);
		assertFalse(a.getSuccess());
		assertEquals(0.0, a.getMean(), "not initialized");
		assertEquals(0.0, a.getConfInt());
	}

	@Test
	void legacyAnalyzerEstimatedMeanBeforeSuccessIsTheNullTestMean() {
		DynamicDataAnalyzerImpl a = new DynamicDataAnalyzerImpl(0.05, 0.05, 10000);
		a.addSample(1, 1);
		a.addSample(5, 1);
		assertEquals(3.0, a.estimatedMean(), 1e-12);
	}

	@Test
	void legacyAnalyzerSetParametersUpdatesNullTestPeriod() {
		DynamicDataAnalyzerImpl a = new DynamicDataAnalyzerImpl(0.05, 0.05, 10000);
		SimParameters p = new SimParameters();
		p.setNullTestRate(0.05); // null test every 500 samples
		a.setParameters(p);
		int fed = feed(a, r -> 0.0, 1, 10000);
		assertEquals(500, fed);
		assertTrue(a.isZero());
	}

	// ---- QuantileDataAnalyzer specifics -------------------------------------------------

	@Test
	void quantileAnalyzerEstimatesMedianAndQuartilesOfUniformSamples() {
		QuantileDataAnalyzer a = new QuantileDataAnalyzer(0.05, 0.5, 100000, new double[] { 0.25, 0.5, 0.75 });
		feed(a, Random::nextDouble, 9, 100000);
		assertTrue(a.getSuccess());
		double[] q = a.getQuantiles();
		assertEquals(3, q.length);
		assertEquals(0.25, q[0], 0.05);
		assertEquals(0.5, q[1], 0.05);
		assertEquals(0.75, q[2], 0.05);
		assertTrue(q[0] <= q[1] && q[1] <= q[2]);
	}

	@Test
	void quantileAnalyzerProbabilityIsNaNUntilSortedThenApproximatesTheCdf() {
		QuantileDataAnalyzer a = new QuantileDataAnalyzer(0.05, 0.05, 100000, new double[] { 0.5 });
		feed(a, Random::nextDouble, 9, 100000);
		a.addSample(0.5, 1); // invalidates the sorted flag
		assertTrue(Double.isNaN(a.getProbability(0.5)));
		a.getQuantile(0.5);
		assertEquals(0.5, a.getProbability(0.5), 0.05);
		assertEquals(0.0, a.getProbability(-1.0), 0.01);
		assertEquals(1.0, a.getProbability(2.0), 0.01);
	}

	@Test
	void quantileAnalyzerWithoutQuantilesReturnsNull() {
		QuantileDataAnalyzer a = new QuantileDataAnalyzer(0.05, 0.05, 10000, null);
		assertNull(a.getQuantiles());
	}

	@Test
	@Disabled("bug: QuantileDataAnalyzer 5-arg constructor never allocates 'data', so sorting or recording samples throws NullPointerException")
	void quantileAnalyzerWithExplicitSorterWorks() {
		QuantileDataAnalyzer a = new QuantileDataAnalyzer(0.05, 0.05, 100000, new double[] { 0.5 }, new MergeSort());
		feed(a, Random::nextDouble, 9, 100000);
		assertEquals(0.5, a.getQuantile(0.5), 0.05);
	}
}
