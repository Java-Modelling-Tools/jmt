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

class SampleMeanVarTest {

	private static final double EPS = 1e-9;
	// data -2,0,2,4 : mean 1, sum of squares about mean = 9+1+1+9 = 20, sample variance 20/3
	private static final double[] DATA = { -2, 0, 2, 4 };

	@Test
	void emptyAccumulatorIsAllZero() {
		SampleMeanVar s = new SampleMeanVar();
		assertEquals(0.0, s.getMean(), EPS);
		assertEquals(0.0, s.getVar(), EPS);
		assertEquals(0.0, s.getMax(), EPS);
		assertEquals(0.0, s.getMin(), EPS);
	}

	@Test
	void singleSampleHasZeroVariance() {
		SampleMeanVar s = new SampleMeanVar();
		s.putNewSample(7);
		assertEquals(7.0, s.getMean(), EPS);
		assertEquals(0.0, s.getVar(), EPS);
	}

	@Test
	void meanAndSampleVarianceMatchHandComputation() {
		SampleMeanVar s = new SampleMeanVar(DATA);
		assertEquals(1.0, s.getMean(), EPS);
		assertEquals(20.0 / 3.0, s.getVar(), EPS);
		assertEquals(4.0, s.getMax(), EPS);
		assertEquals(-2.0, s.getMin(), EPS);
	}

	@Test
	void initializeResetsState() {
		SampleMeanVar s = new SampleMeanVar(DATA);
		s.initialize();
		assertEquals(0.0, s.getMean(), EPS);
		assertEquals(0.0, s.getVar(), EPS);
		s.putNewSample(5);
		assertEquals(5.0, s.getMean(), EPS);
	}

	@Test
	void confidenceIntervalIsPositiveAndShrinksWithLowerConfidence() {
		SampleMeanVar s = new SampleMeanVar(DATA);
		double c95 = s.getConfInt(95);
		double c90 = s.getConfInt(90);
		assertTrue(c95 > c90 && c90 > 0);
	}

	@Test
	void toStringReportsSampleCountAndMean() {
		String text = new SampleMeanVar(DATA).toString();
		assertTrue(text.contains("Number of samples : 4.0"));
		assertTrue(text.contains("Sample mean       : 1.0"));
	}
}
