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

class WeightedMeanVarTest {

	private static final double EPS = 1e-9;
	// samples 1 (w=1) and 3 (w=3): mean = (1+9)/4 = 2.5, E[x^2] = (1+27)/4 = 7, var = 7 - 6.25 = 0.75
	private static final double[] X = { 1, 3 };
	private static final double[] W = { 1, 3 };

	@Test
	void weightedMeanAndVarianceMatchHandComputation() {
		WeightedMeanVar w = new WeightedMeanVar(X, W);
		assertEquals(2.5, w.getMean(), EPS);
		assertEquals(0.75, w.getVar(), EPS);
		assertEquals(3.0, w.getMax(), EPS);
		assertEquals(1.0, w.getMin(), EPS);
	}

	@Test
	void firstSampleSetsMinAndMaxEvenWhenNegative() {
		WeightedMeanVar w = new WeightedMeanVar();
		w.putNewSample(-4, 2);
		assertEquals(-4.0, w.getMax(), EPS);
		assertEquals(-4.0, w.getMin(), EPS);
		assertEquals(0.0, w.getVar(), EPS);
	}

	@Test
	void initializeWithDataReplacesPreviousState() {
		WeightedMeanVar w = new WeightedMeanVar(new double[] { 100 }, new double[] { 1 });
		w.initialize(X, W);
		assertEquals(2.5, w.getMean(), EPS);
	}

	@Test
	void initializeWithoutDataClearsState() {
		WeightedMeanVar w = new WeightedMeanVar(X, W);
		w.initialize();
		assertEquals(0.0, w.getMean(), EPS);
		assertEquals(0.0, w.getVar(), EPS);
		assertEquals(0.0, w.getMax(), EPS);
	}
}
