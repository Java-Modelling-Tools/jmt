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
package random.discrete;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import jmt.common.exception.IncorrectDistributionParameterException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import random.engine.MersenneTwister;

class DiscreteDistributionsTest {

	private static final long SEED = 2024L;
	private static final int N = 100000;

	private interface IntSource {
		int next() throws Exception;
	}

	private static int[] draw(IntSource s, int n) throws Exception {
		int[] out = new int[n];
		for (int i = 0; i < n; i++) {
			out[i] = s.next();
		}
		return out;
	}

	private static double mean(int[] x) {
		double s = 0;
		for (int v : x) {
			s += v;
		}
		return s / x.length;
	}

	private static MersenneTwister mt(long seed) {
		return new MersenneTwister(seed);
	}

	private static void assertMean(double expected, int[] sample) {
		assertEquals(expected, mean(sample), Math.abs(expected) * 0.03);
	}

	// ---- Bernoulli ----

	@Test
	void bernoulliCachedAndParameterisedAgree() throws Exception {
		Bernoulli b = new Bernoulli(0.3);
		BernoulliPar par = new BernoulliPar(0.3);
		b.setRandomEngine(mt(SEED));
		int[] a = draw(b::nextRand, N);
		assertMean(0.3, a);
		b.setRandomEngine(mt(SEED));
		assertArrayEquals(a, draw(() -> b.nextRand(par), N), "same seed, same sequence for both entry points");
		b.setRandomEngine(mt(SEED + 1));
		assertFalse(Arrays.equals(Arrays.copyOf(a, 200), Arrays.copyOf(draw(b::nextRand, 200), 200)));
		assertEquals(0.3, b.theorMean(), 0.0);
		assertEquals(0.21, b.theorVariance(), 1e-12);
		assertEquals(0.3, b.theorMean(par), 0.0);
		assertEquals(0.21, b.theorVariance(par), 1e-12);
		assertEquals(0, b.getlower());
		assertEquals(1, b.getUpper());
	}

	@Test
	void bernoulliPmfCdf() throws Exception {
		Bernoulli b = new Bernoulli(0.3);
		BernoulliPar par = new BernoulliPar(0.3);
		for (int k = 0; k < 2; k++) {
			assertEquals(0.7, k == 0 ? b.pmf(0) : b.pmf(0, par), 1e-12);
		}
		assertEquals(0.3, b.pmf(1), 1e-12);
		assertEquals(0.3, b.pmf(1, par), 1e-12);
		assertEquals(0.0, b.pmf(2), 0.0);
		assertEquals(0.0, b.pmf(2, par), 0.0);
		assertEquals(0.0, b.cdf(-1), 0.0);
		assertEquals(0.7, b.cdf(0), 1e-12);
		assertEquals(1.0, b.cdf(1), 0.0);
		assertEquals(0.0, b.cdf(-1, par), 0.0);
		assertEquals(0.7, b.cdf(0, par), 1e-12);
		assertEquals(1.0, b.cdf(5, par), 0.0);
	}

	@Test
	void bernoulliRejectsBadProbability() throws Exception {
		assertThrows(IncorrectDistributionParameterException.class, () -> new Bernoulli(-0.1));
		assertThrows(IncorrectDistributionParameterException.class, () -> new Bernoulli(1.1));
		assertThrows(IncorrectDistributionParameterException.class, () -> new BernoulliPar(2.0));
		Bernoulli b = new Bernoulli(0.5);
		assertThrows(IncorrectDistributionParameterException.class, () -> b.updatePar(-1.0));
		assertThrows(IncorrectDistributionParameterException.class, () -> b.nextRand(new BinomialPar(3, 0.5)));
		assertFalse(b.updatePar(new BinomialPar(3, 0.5)), "foreign parameter type is ignored");
		assertTrue(b.updatePar(new BernoulliPar(0.9)));
		assertEquals(0.9, b.theorMean(), 0.0);
		BernoulliPar p = new BernoulliPar(0.5);
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setProbability(3));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setMean(-1));
		p.setMean(0.25);
		assertEquals(0.25, p.getProbability(), 0.0);
	}

	@Disabled("bug: discrete parameter checks (p<0||p>1) accept NaN (Bernoulli, ShiftedGeometric, Binomial)")
	@Test
	void discreteProbabilityNaNIsRejected() {
		assertThrows(IncorrectDistributionParameterException.class, () -> new Bernoulli(Double.NaN));
		assertThrows(IncorrectDistributionParameterException.class, () -> new ShiftedGeometric(Double.NaN));
		assertThrows(IncorrectDistributionParameterException.class, () -> new Binomial(Double.NaN, 3));
	}

	@Test
	void uncachedDistributionsReturnSentinels() {
		Bernoulli b = new Bernoulli();
		assertEquals(-1, b.nextRand());
		assertEquals(-1.0, b.pmf(1), 0.0);
		assertEquals(-1.0, b.cdf(1), 0.0);
		assertEquals(-1.0, b.theorMean(), 0.0);
		assertEquals(-1.0, b.theorVariance(), 0.0);
		Binomial bi = new Binomial();
		assertEquals(-1, bi.getUpper());
		assertEquals(-1, bi.getlower());
		assertEquals(-1.0, bi.pmf(1), 0.0);
		assertEquals(-1.0, bi.cdf(1), 0.0);
		assertEquals(-1.0, bi.theorMean(), 0.0);
		assertEquals(-1.0, bi.theorVariance(), 0.0);
		Uniform u = new Uniform();
		assertEquals(-1, u.nextRand());
		assertEquals(-1, u.getUpper());
		assertEquals(-1, u.getlower());
		assertEquals(-1.0, u.pmf(1), 0.0);
		assertEquals(-1.0, u.cdf(1), 0.0);
		assertEquals(-1.0, u.theorMean(), 0.0);
		assertEquals(-1.0, u.theorVariance(), 0.0);
		Zipf z = new Zipf();
		assertEquals(-1, z.getUpper());
		assertEquals(-1, z.getlower());
		assertEquals(-1.0, z.pmf(1), 0.0);
		assertEquals(-1.0, z.cdf(1), 0.0);
		assertEquals(-1.0, z.theorMean(), 0.0);
		assertEquals(-1.0, z.theorVariance(), 0.0);
		Poisson p = new Poisson();
		p.setRandomEngine(mt(1));
		assertEquals(-1, p.nextRand());
		assertEquals(-1.0, p.pmf(1), 0.0);
		ShiftedGeometric g = new ShiftedGeometric();
		g.setRandomEngine(mt(1));
		g.setRandomEngine(mt(1));
		assertEquals(-1, g.nextRand());
		assertEquals(-1.0, g.cdf(1), 0.0);
	}

	// ---- Binomial ----

	@Test
	void binomialValuesAndSampling() throws Exception {
		Binomial b = new Binomial(0.4, 10);
		BinomialPar par = new BinomialPar(10, 0.4);
		assertEquals(4.0, b.theorMean(), 1e-12);
		assertEquals(2.4, b.theorVariance(), 1e-12);
		assertEquals(4.0, b.theorMean(par), 1e-12);
		assertEquals(2.4, b.theorVariance(par), 1e-12);
		assertEquals(0, b.getlower());
		assertEquals(10, b.getUpper());
		assertEquals(Math.pow(0.6, 10), b.pmf(0), 1e-12);
		assertEquals(0.0, b.pmf(11), 0.0);
		assertEquals(b.pmf(3), b.pmf(3, par), 1e-12);
		assertEquals(0.0, b.cdf(-1), 0.0);
		assertEquals(1.0, b.cdf(10), 0.0);
		assertEquals(b.pmf(0) + b.pmf(1) + b.pmf(2), b.cdf(2), 1e-12);
		assertEquals(b.cdf(2), b.cdf(2, par), 1e-12);
		assertEquals(0.0, b.cdf(-1, par), 0.0);
		assertEquals(1.0, b.cdf(10, par), 0.0);
		assertEquals(0.0, b.pmf(-1, par), 0.0);
		// nextRand is shifted by one: values are in 1..n+1 (this is how the model maps it to class indices)
		b.setRandomEngine(mt(SEED));
		int[] a = draw(b::nextRand, N);
		assertMean(b.theorMean() + 1, a);
		for (int v : a) {
			assertTrue(v >= 1 && v <= 11);
		}
		b.setRandomEngine(mt(SEED));
		assertArrayEquals(a, draw(b::nextRand, N));
		b.setRandomEngine(mt(SEED));
		assertArrayEquals(Arrays.copyOf(a, 2000), draw(() -> b.nextRand(par), 2000));
	}

	@Test
	void binomialUpdateAndValidation() throws Exception {
		assertThrows(IncorrectDistributionParameterException.class, () -> new Binomial(0.5, -1));
		assertThrows(IncorrectDistributionParameterException.class, () -> new Binomial(1.5, 3));
		assertThrows(IncorrectDistributionParameterException.class, () -> new BinomialPar(-1, 0.5));
		assertThrows(IncorrectDistributionParameterException.class, () -> new BinomialPar(3, 1.5));
		Binomial b = new Binomial(0.5, 4);
		assertThrows(IncorrectDistributionParameterException.class, () -> b.updatePar(0.5, -2));
		assertThrows(IncorrectDistributionParameterException.class, () -> b.updatePar(-0.5, 2));
		assertTrue(b.updatePar(new BinomialPar(6, 0.5)));
		assertEquals(6, b.getUpper());
		assertFalse(b.updatePar(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> b.pmf(1, new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> b.cdf(1, new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> b.theorMean(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> b.theorVariance(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> b.nextRand(new BernoulliPar(0.5)));
		// the boxed constructors take a number of items, so n = items - 1
		assertEquals(4, new Binomial(Double.valueOf(0.5), Integer.valueOf(5)).getUpper());
		assertEquals(4, new Binomial(Integer.valueOf(5), Double.valueOf(0.5)).getUpper());
		assertEquals(4, new BinomialPar(Integer.valueOf(5), Double.valueOf(0.5)).getNumberOfExperiment());
		assertEquals(4, new BinomialPar(Double.valueOf(0.5), Integer.valueOf(5)).getNumberOfExperiment());
		BinomialPar p = new BinomialPar(3, 0.5);
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setNumberOfExperiment(-1));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setProbability(2));
		p.setNumberOfExperiment(8);
		p.setProbability(0.25);
		assertEquals(8, p.getNumberOfExperiment());
		assertEquals(0.25, p.getProbability(), 0.0);
		assertTrue(p.check());
	}

	// ---- Poisson ----

	@Test
	void poissonSamplingAndValues() throws Exception {
		Poisson d = new Poisson(3.0);
		PoissonPar par = new PoissonPar(3.0);
		assertEquals(3.0, d.theorMean(), 0.0);
		assertEquals(3.0, d.theorVariance(), 0.0);
		assertEquals(3.0, d.theorMean(par), 0.0);
		assertEquals(3.0, d.theorVariance(par), 0.0);
		assertEquals(Math.exp(-3.0), d.pmf(0), 1e-12);
		assertEquals(Math.exp(-3.0) * 3.0, d.pmf(1, par), 1e-12);
		assertEquals(0.0, d.pmf(-1), 0.0);
		assertEquals(0.0, d.cdf(-1), 0.0);
		assertEquals(Math.exp(-3.0) * (1 + 3 + 4.5), d.cdf(2), 1e-12);
		assertEquals(d.cdf(2), d.cdf(2, par), 1e-12);
		assertEquals(0, d.getlower());
		assertEquals(300, d.getUpper());
		d.setRandomEngine(mt(SEED));
		int[] a = draw(d::nextRand, N);
		assertMean(3.0, a);
		d.setRandomEngine(mt(SEED));
		assertArrayEquals(a, draw(() -> d.nextRand(par), N));
		d.setRandomEngine(mt(SEED + 1));
		assertFalse(Arrays.equals(Arrays.copyOf(a, 100), draw(d::nextRand, 100)));
	}

	@Test
	void poissonRejectsBadLambda() throws Exception {
		assertThrows(IncorrectDistributionParameterException.class, () -> new Poisson(0.0));
		assertThrows(IncorrectDistributionParameterException.class, () -> new Poisson(-1.0));
		Poisson d = new Poisson(2.0);
		assertThrows(IncorrectDistributionParameterException.class, () -> d.updatePar(0.0));
		assertFalse(d.updatePar(new BernoulliPar(0.5)));
		assertTrue(d.updatePar(new PoissonPar(5.0)));
		assertEquals(5.0, d.theorMean(), 0.0);
		assertThrows(IncorrectDistributionParameterException.class, () -> d.nextRand(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> d.pmf(1, new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> d.theorMean(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> new PoissonPar(0.0));
		PoissonPar p = new PoissonPar(2.0);
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setLambda(-1));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setMean(0));
	}

	// ---- Uniform ----

	@Test
	void uniformSamplingAndValues() throws Exception {
		Uniform u = new Uniform(1, 6);
		UniformPar par = new UniformPar(1, 6);
		assertEquals(3.5, u.theorMean(), 1e-12);
		assertEquals(35.0 / 12.0, u.theorVariance(), 1e-12);
		assertEquals(3.5, u.theorMean(par), 1e-12);
		assertEquals(35.0 / 12.0, u.theorVariance(par), 1e-12);
		assertEquals(1.0 / 6.0, u.pmf(4), 1e-12);
		assertEquals(0.0, u.pmf(7), 0.0);
		assertEquals(1.0 / 6.0, u.pmf(4, par), 1e-12);
		assertEquals(0.0, u.pmf(0, par), 0.0);
		assertEquals(0.0, u.cdf(1), 0.0);
		assertEquals(1.0, u.cdf(6), 0.0);
		assertEquals(0.5, u.cdf(3), 1e-12);
		assertEquals(0.5, u.cdf(3, par), 1e-12);
		assertEquals(0.0, u.cdf(0, par), 0.0);
		assertEquals(1.0, u.cdf(9, par), 0.0);
		assertEquals(1, u.getlower());
		assertEquals(6, u.getUpper());
		u.setRandomEngine(mt(SEED));
		int[] a = draw(u::nextRand, N);
		for (int v : a) {
			assertTrue(v >= 1 && v <= 6);
		}
		assertMean(3.5, a);
		u.setRandomEngine(mt(SEED));
		assertArrayEquals(a, draw(() -> u.nextRand(par), N));
		u.setRandomEngine(mt(SEED + 1));
		assertFalse(Arrays.equals(Arrays.copyOf(a, 100), draw(u::nextRand, 100)));
		u.setRandomEngine(mt(SEED));
		boolean t = false, f = false;
		for (int i = 0; i < 100; i++) {
			if (u.nextBoolean()) {
				t = true;
			} else {
				f = true;
			}
		}
		assertTrue(t && f);
	}

	@Test
	void uniformValidation() throws Exception {
		assertThrows(IncorrectDistributionParameterException.class, () -> new Uniform(3, 3));
		assertThrows(IncorrectDistributionParameterException.class, () -> new Uniform(4, 3));
		assertThrows(IncorrectDistributionParameterException.class, () -> new UniformPar(5, 2));
		Uniform u = new Uniform(Integer.valueOf(1), Integer.valueOf(3));
		assertThrows(IncorrectDistributionParameterException.class, () -> u.updatePar(2, 2));
		assertTrue(u.updatePar(new UniformPar(Integer.valueOf(2), Integer.valueOf(9))));
		assertEquals(9, u.getUpper());
		assertFalse(u.updatePar(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> u.nextRand(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> u.pmf(1, new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> u.cdf(1, new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> u.theorMean(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> u.theorVariance(new BernoulliPar(0.5)));
		UniformPar p = new UniformPar(1, 5);
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setMin(5));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setMax(1));
		p.setMin(2);
		p.setMax(7);
		assertEquals(2, p.getMin());
		assertEquals(7, p.getMax());
		assertTrue(p.check());
	}

	// ---- ShiftedGeometric ----

	@Test
	void shiftedGeometricSamplingAndValues() throws Exception {
		ShiftedGeometric g = new ShiftedGeometric(0.25);
		ShiftedGeometricPar par = new ShiftedGeometricPar(0.25);
		assertEquals(4.0, g.theorMean(), 1e-12);
		assertEquals(12.0, g.theorVariance(), 1e-12);
		assertEquals(4.0, g.theorMean(par), 1e-12);
		assertEquals(12.0, g.theorVariance(par), 1e-12);
		assertEquals(0.25, g.pmf(1), 1e-12);
		assertEquals(0.25 * 0.75, g.pmf(2, par), 1e-12);
		assertEquals(0.0, g.pmf(0), 0.0);
		assertEquals(0.0, g.cdf(0), 0.0);
		assertEquals(1 - 0.75 * 0.75, g.cdf(2), 1e-12);
		assertEquals(1 - 0.75 * 0.75, g.cdf(2, par), 1e-12);
		assertEquals(1, g.getlower());
		assertEquals(400, g.getUpper());
		g.setRandomEngine(mt(SEED));
		int[] a = draw(g::nextRand, N);
		for (int v : a) {
			assertTrue(v >= 1);
		}
		assertMean(4.0, a);
		g.setRandomEngine(mt(SEED));
		assertArrayEquals(a, draw(() -> g.nextRand(par), N));
		g.setRandomEngine(mt(SEED + 1));
		assertFalse(Arrays.equals(Arrays.copyOf(a, 100), draw(g::nextRand, 100)));
	}

	@Test
	void shiftedGeometricValidation() throws Exception {
		assertThrows(IncorrectDistributionParameterException.class, () -> new ShiftedGeometric(-0.1));
		assertThrows(IncorrectDistributionParameterException.class, () -> new ShiftedGeometric(1.1));
		assertThrows(IncorrectDistributionParameterException.class, () -> new ShiftedGeometricPar(2.0));
		ShiftedGeometric g = new ShiftedGeometric(Double.valueOf(0.5));
		assertThrows(IncorrectDistributionParameterException.class, () -> g.updatePar(7.0));
		assertFalse(g.updatePar(new BernoulliPar(0.5)));
		assertTrue(g.updatePar(new ShiftedGeometricPar(Double.valueOf(0.1))));
		assertEquals(10.0, g.theorMean(), 1e-9);
		assertThrows(IncorrectDistributionParameterException.class, () -> g.nextRand(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> g.pmf(1, new BernoulliPar(0.5)));
		ShiftedGeometricPar p = new ShiftedGeometricPar(0.5);
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setProbability(-1));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setMean(0.5));
		p.setMean(5.0);
		assertEquals(0.2, p.getProbability(), 1e-12);
		assertTrue(p.check());
	}

	// ---- Zipf ----

	@Test
	void zipfValuesAndSampling() throws Exception {
		Zipf z = new Zipf(1.5, 10);
		ZipfPar par = new ZipfPar(10, 1.5);
		double h = 0;
		for (int i = 1; i <= 10; i++) {
			h += Math.pow(i, -1.5);
		}
		assertEquals(1 / h, z.pmf(1), 1e-12);
		assertEquals(1 / (h * Math.pow(3, 1.5)), z.pmf(3, par), 1e-12);
		assertEquals(0.0, z.pmf(0), 0.0);
		assertEquals(0.0, z.pmf(11, par), 0.0);
		assertEquals(0.0, z.cdf(0), 0.0);
		assertEquals(1.0, z.cdf(10), 0.0);
		assertEquals((1 + Math.pow(2, -1.5)) / h, z.cdf(2), 1e-12);
		assertEquals(z.cdf(2), z.cdf(2, par), 1e-12);
		assertEquals(0.0, z.cdf(-1, par), 0.0);
		assertEquals(1.0, z.cdf(10, par), 0.0);
		assertEquals(z.theorMean(), z.theorMean(par), 1e-12);
		assertEquals(z.theorVariance(), z.theorVariance(par), 1e-12);
		assertTrue(z.theorVariance() > 0);
		assertEquals(0, z.getlower());
		assertEquals(10, z.getUpper());
		z.setRandomEngine(mt(SEED));
		int[] a = draw(z::nextRand, N);
		for (int v : a) {
			assertTrue(v >= 1 && v <= 10, "value " + v);
		}
		assertMean(z.theorMean(), a);
		z.setRandomEngine(mt(SEED));
		assertArrayEquals(a, draw(z::nextRand, N));
		z.setRandomEngine(mt(SEED + 1));
		assertFalse(Arrays.equals(Arrays.copyOf(a, 100), draw(z::nextRand, 100)));
	}

	@Test
	void zipfParameterisedSampling() throws Exception {
		Zipf z = new Zipf(Double.valueOf(1.5), Integer.valueOf(10));
		ZipfPar par = new ZipfPar(Double.valueOf(1.5), Integer.valueOf(10));
		z.setRandomEngine(mt(SEED));
		int[] a = draw(() -> z.nextRand(par), 20000);
		assertMean(z.theorMean(par), a);
		z.setRandomEngine(mt(SEED));
		assertArrayEquals(a, draw(() -> z.nextRand(par), 20000));
	}

	@Test
	void zipfValidation() throws Exception {
		assertThrows(IncorrectDistributionParameterException.class, () -> new Zipf(1.0, 0));
		assertThrows(IncorrectDistributionParameterException.class, () -> new Zipf(0.0, 5));
		assertThrows(IncorrectDistributionParameterException.class, () -> new Zipf(-1.0, 5));
		assertThrows(IncorrectDistributionParameterException.class, () -> new ZipfPar(0, 1.0));
		assertThrows(IncorrectDistributionParameterException.class, () -> new ZipfPar(5, 0.0));
		Zipf z = new Zipf(Integer.valueOf(5), Double.valueOf(1.0));
		assertThrows(IncorrectDistributionParameterException.class, () -> z.updatePar(1.0, 0));
		assertThrows(IncorrectDistributionParameterException.class, () -> z.updatePar(0.0, 3));
		assertTrue(z.updatePar(new ZipfPar(7, 2.0)));
		assertEquals(7, z.getUpper());
		assertFalse(z.updatePar(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> z.nextRand(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> z.pmf(1, new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> z.cdf(1, new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> z.theorMean(new BernoulliPar(0.5)));
		assertThrows(IncorrectDistributionParameterException.class, () -> z.theorVariance(new BernoulliPar(0.5)));
		ZipfPar p = new ZipfPar(Integer.valueOf(5), Double.valueOf(1.0));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setMax(0));
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setAlpha(0));
		p.setMax(9);
		p.setAlpha(2.0);
		assertEquals(9, p.getNumberOfElements());
		assertEquals(2.0, p.getAlpha(), 0.0);
		assertTrue(p.check());
	}

	// ---- FixProbability ----

	@Test
	void fixProbabilitySamplesProportionalToCounts() throws Exception {
		FixProbability f = new FixProbability(new Object[] { 1, 3 });
		assertEquals(1, f.getlower());
		assertEquals(2, f.getUpper());
		assertEquals(1.75, f.theorMean(), 1e-12);
		assertEquals(0.25, f.cdf(1), 1e-12);
		assertEquals(0.25, f.pmf(0), 1e-12);
		assertFalse(f.updatePar(new BernoulliPar(0.5)));
		assertEquals(0, f.nextRand(new BernoulliPar(0.5)));
		assertEquals(0.0, f.pmf(1, new BernoulliPar(0.5)), 0.0);
		assertEquals(0.0, f.cdf(1, new BernoulliPar(0.5)), 0.0);
		assertEquals(0.0, f.theorMean(new BernoulliPar(0.5)), 0.0);
		assertEquals(0.0, f.theorVariance(new BernoulliPar(0.5)), 0.0);
		f.theorVariance();
		f.setRandomEngine(mt(SEED));
		int[] a = draw(f::nextRand, N);
		assertMean(1.75, a);
		f.setRandomEngine(mt(SEED));
		assertArrayEquals(a, draw(f::nextRand, N));
	}
}
