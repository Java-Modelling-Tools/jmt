/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SampleStatisticsTest {

	private static final double EPS = 1e-9;

	private static SampleStatistics sample(double... xs) {
		SampleStatistics s = new SampleStatistics();
		double t = 1;
		for (double x : xs) {
			s.putNewSample(x, 1, t++);
		}
		return s;
	}

	@Test
	void emptyStatisticsReportZeroMinAndTimes() {
		SampleStatistics s = new SampleStatistics();
		assertEquals(0.0, s.getSamples(), EPS);
		assertEquals(0.0, s.getMin(), EPS);
		assertEquals(0.0, s.getMinSimTime(), EPS);
		assertEquals(0.0, s.getMaxSimTime(), EPS);
	}

	@Test
	void momentsMatchHandComputation() {
		// samples 1,2,3,4 with unit weights: m1=2.5 m2=7.5 m3=25 m4=88.5 var=1.25
		SampleStatistics s = sample(1, 2, 3, 4);
		assertEquals(4.0, s.getSamples(), EPS);
		assertEquals(2.5, s.getMean(), EPS);
		assertEquals(7.5, s.getMoment2(), EPS);
		assertEquals(25.0, s.getMoment3(), EPS);
		assertEquals(88.5, s.getMoment4(), EPS);
		assertEquals(1.25, s.getVariance(), EPS);
		assertEquals(Math.sqrt(1.25), s.getStandardDeviation(), EPS);
		assertEquals(Math.sqrt(1.25) / 2.5, s.getCoefficienfOfVariation(), EPS);
		assertEquals(4.0, s.getMax(), EPS);
		assertEquals(1.0, s.getMin(), EPS);
		assertEquals(1.0, s.getMinSimTime(), EPS);
		assertEquals(4.0, s.getMaxSimTime(), EPS);
	}

	@Test
	void symmetricDataHaveZeroSkewAndNegativeExcessKurtosis() {
		SampleStatistics s = sample(1, 2, 3, 4);
		assertEquals(0.0, s.getSkew(), EPS);
		// uniform 4-point: m4 central = (1.5^4*2+0.5^4*2)/4 = 2.5625; var^2 = 1.5625; kurt = 1.64 - 3
		assertEquals(2.5625 / 1.5625 - 3, s.getKurtosis(), EPS);
	}

	@Test
	void constantSamplesHaveNaNSkewAndKurtosis() {
		SampleStatistics s = sample(5, 5, 5);
		assertEquals(0.0, s.getVariance(), EPS);
		assertTrue(Double.isNaN(s.getSkew()));
		assertTrue(Double.isNaN(s.getKurtosis()));
	}

	@Test
	void zeroWeightSamplesAreIgnored() {
		SampleStatistics s = new SampleStatistics();
		s.putNewSample(10, 0, 1);
		assertEquals(0.0, s.getSamples(), EPS);
	}

	@Test
	void weightsScaleTheMean() {
		SampleStatistics s = new SampleStatistics();
		s.putNewSample(1, 1, 1);
		s.putNewSample(3, 3, 2);
		assertEquals(2.5, s.getMean(), EPS);
	}

	@Test
	void distributionIsNullUntilInitialised() {
		SampleStatistics s = new SampleStatistics();
		assertFalse(s.isDistributionAvailable());
		assertNull(s.getDistribution(SampleStatistics.DistributionType.FREQUENCY));
	}

	@Test
	void distributionBucketsFrequenciesAndCumulative() {
		SampleStatistics s = new SampleStatistics();
		s.initializeDistrubution(0, 4, 4);
		assertTrue(s.isDistributionAvailable());
		// buckets [0,1) [1,2) [2,3) [3,4]; value 4 falls in last bucket (upper edge)
		s.putNewSample(0.5, 1, 1);
		s.putNewSample(1.5, 1, 2);
		s.putNewSample(1.7, 1, 3);
		s.putNewSample(4.0, 1, 4);
		assertArrayEquals(new double[] { 0.25, 0.5, 0, 0.25 }, s.getDistribution(SampleStatistics.DistributionType.FREQUENCY), EPS);
		assertArrayEquals(new double[] { 0.25, 0.75, 0.75, 1.0 }, s.getDistribution(SampleStatistics.DistributionType.CUMULATIVE), EPS);
		assertArrayEquals(new double[] { 0, 1, 2, 3 }, s.getDistributionIntervalsStart(), EPS);
		assertArrayEquals(new double[] { 1, 2, 3, 4 }, s.getDistriburionIntervalsEnd(), EPS);
		assertEquals(1.0, s.getIntervalWidth(), EPS);
		assertEquals(0.0, s.getIntervalMin(), EPS);
		assertEquals(4.0, s.getIntervalMax(), EPS);
	}

	@Test
	void samplesOutsideIntervalsAreNotBucketed() {
		SampleStatistics s = new SampleStatistics();
		s.initializeDistrubution(0, 2, 2);
		s.putNewSample(5, 1, 1);
		s.putNewSample(0.5, 1, 2);
		assertArrayEquals(new double[] { 1, 0 }, s.getDistribution(SampleStatistics.DistributionType.FREQUENCY), EPS);
	}

	@Test
	void initialisingDistributionAfterSamplesThrows() {
		SampleStatistics s = sample(1);
		assertThrows(IllegalStateException.class, () -> s.initializeDistrubution(0, 1, 2));
	}

	@Test
	void toStringListsMainFigures() {
		String text = sample(1, 2, 3, 4).toString();
		assertTrue(text.contains("Number of samples : 4.0"));
		assertTrue(text.contains("Mean of samples   : 2.5"));
		new SampleStatistics().finalizeLoading(); // base implementation is a no-op
	}
}
