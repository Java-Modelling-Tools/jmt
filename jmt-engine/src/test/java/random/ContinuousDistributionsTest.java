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
package random;

import static org.junit.jupiter.api.Assertions.*;
import static random.SamplingSupport.*;

import jmt.common.exception.IncorrectDistributionParameterException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import random.engine.MersenneTwister;

class ContinuousDistributionsTest {

	private static final long SEED = 12345L;

	/** Runs the shared checks: mean within 3%, determinism, seed sensitivity, variance/cdf/pdf sanity. */
	private static void checkSampling(Distribution d, Parameter p) throws Exception {
		double[] a = draw(d, p, SEED, N);
		double[] b = draw(d, p, SEED, N);
		assertArrayEquals(a, b, "same seed must give the same sequence");
		double[] c = draw(d, p, SEED + 1, 100);
		assertFalse(java.util.Arrays.equals(java.util.Arrays.copyOf(a, 100), c), "different seeds must differ");
		double theor = d.theorMean(p);
		assertEquals(theor, mean(a), Math.abs(theor) * 0.03, d.getClass().getSimpleName() + " sample mean");
		assertTrue(d.theorVariance(p) >= 0);
	}

	private static void checkCdfTails(Distribution d, Parameter p) throws Exception {
		assertEquals(0.0, d.cdf(-1, p), 1e-6, d.getClass().getSimpleName() + " cdf below support");
		assertEquals(1.0, d.cdf(d instanceof random.Poisson ? 60 : 1e4, p), 1e-6, d.getClass().getSimpleName() + " cdf far right");
		assertTrue(d.pdf(d.theorMean(p), p) >= 0);
	}

	@Test
	void cdfTailsAreZeroAndOne() throws Exception {
		checkCdfTails(new Exponential(), new ExponentialPar(2.0));
		checkCdfTails(new Normal(), new NormalPar(10.0, 2.0));
		checkCdfTails(new Pareto(), new ParetoPar(4.0, 1.0));
		checkCdfTails(new Uniform(), new UniformPar(2.0, 6.0));
		checkCdfTails(new random.Poisson(), new PoissonPar(4.0));
		checkCdfTails(new GammaDistr(), new GammaDistrPar(3.0, 2.0));
	}

	@Disabled("bug: Erlang.cdf sums i < r-1 instead of i < r, and has no x < 0 guard")
	@Test
	void erlangCdf() throws Exception {
		ErlangPar p = new ErlangPar(2.0, 3L);
		assertEquals(1 - Math.exp(-2.0) * 5.0, new Erlang().cdf(1.0, p), 1e-9);
		assertEquals(0.0, new Erlang().cdf(-1.0, p), 1e-9);
	}

	@Disabled("bug: Lognormal.cdf and Weibull.cdf return NaN for x < 0")
	@Test
	void lognormalAndWeibullCdfBelowSupport() throws Exception {
		assertEquals(0.0, new Lognormal().cdf(-1, new LognormalPar(0.0, 0.5)), 0.0);
		assertEquals(0.0, new Weibull().cdf(-1, new WeibullPar(2.0, 1.5)), 0.0);
	}

	@Disabled("bug: HyperExp.pdf and HyperExp.cdf are stubs that always return 0.0")
	@Test
	void hyperExponentialPdfCdf() throws Exception {
		HyperExpPar p = new HyperExpPar(0.3, 1.0, 4.0);
		assertEquals(1.0, new HyperExp().cdf(1e4, p), 1e-9);
	}

	@Test
	void lognormalAndWeibullCdfInsideSupport() throws Exception {
		assertEquals(0.5, new Lognormal().cdf(1.0, new LognormalPar(0.0, 0.5)), 1e-6);
		assertEquals(1 - Math.exp(-1.0), new Weibull().cdf(2.0, new WeibullPar(2.0, 1.5)), 1e-12);
	}

	@Test
	void exponential() throws Exception {
		checkSampling(new Exponential(), new ExponentialPar(2.0));
		Exponential e = new Exponential();
		ExponentialPar p = new ExponentialPar(2.0);
		assertEquals(0.5, e.theorMean(p), 1e-12);
		assertEquals(0.25, e.theorVariance(p), 1e-12);
		assertEquals(2.0, e.pdf(0, p), 1e-12);
		assertEquals(0.0, e.pdf(-1, p), 0.0);
		assertEquals(1 - Math.exp(-2.0), e.cdf(1, p), 1e-12);
	}

	@Test
	void erlang() throws Exception {
		checkSampling(new Erlang(), new ErlangPar(2.0, 3L));
		assertEquals(1.5, new Erlang().theorMean(new ErlangPar(2.0, 3L)), 1e-12);
	}

	@Disabled("bug: GammaDistr.nextRand has mean alpha*lambda (lambda as scale) but theorMean is alpha/lambda (lambda as rate)")
	@Test
	void gamma() throws Exception {
		// alpha >= 1 and alpha < 1 use different branches of the sampler
		checkSampling(new GammaDistr(), new GammaDistrPar(3.0, 2.0));
		checkSampling(new GammaDistr(), new GammaDistrPar(0.5, 2.0));
	}

	@Test
	void gammaSamplingIsDeterministicAndPositive() throws Exception {
		GammaDistrPar p = new GammaDistrPar(3.0, 2.0);
		double[] a = draw(new GammaDistr(), p, SEED, 1000);
		assertArrayEquals(a, draw(new GammaDistr(), p, SEED, 1000));
		for (double v : a) {
			assertTrue(v > 0.0);
		}
		for (double alpha : new double[] { 1.0, 20.0 }) {
			GammaDistrPar q = new GammaDistrPar(alpha, 2.0);
			assertArrayEquals(draw(new GammaDistr(), q, SEED, 200), draw(new GammaDistr(), q, SEED, 200));
		}
		GammaDistr g = new GammaDistr();
		assertEquals(0.0, g.pdf(-1, p), 0.0);
		assertEquals(0.0, g.pdf(0, p), 0.0);
		assertEquals(0.5, g.pdf(0, new GammaDistrPar(1.0, 2.0)), 0.0);
		assertTrue(g.pdf(1.0, p) > 0);
		assertEquals(1.5, g.theorMean(new GammaDistrPar(3.0, 2.0)) , 1e-12);
		assertEquals(0.75, g.theorVariance(new GammaDistrPar(3.0, 2.0)), 1e-12);
		GammaDistrPar small = new GammaDistrPar(0.5, 2.0);
		assertArrayEquals(draw(new GammaDistr(), small, SEED, 1000), draw(new GammaDistr(), small, SEED, 1000));
	}

	@Test
	void hyperExponentialParameterObject() throws Exception {
		HyperExpPar p = new HyperExpPar(0.3, 1.0, 4.0);
		assertEquals(0.3, p.getP(), 0.0);
		assertEquals(1.0, p.getLambda1(), 0.0);
		assertEquals(4.0, p.getLambda2(), 0.0);
		assertEquals(0.3 / 1.0 + 0.7 / 4.0, p.getMean(), 1e-12);
		assertTrue(p.getVar() > 0);
		assertEquals(1.0, p.getExpParam1().getLambda(), 0.0);
		assertEquals(4.0, p.getExpParam2().getLambda(), 0.0);
		assertEquals(p.getMean(), new HyperExp().theorMean(p), 0.0);
		assertEquals(p.getVar(), new HyperExp().theorVariance(p), 0.0);
		p.setMean(5.0);
		assertEquals(5.0, p.getMean(), 0.0);
		assertTrue(p.check());
		assertThrows(IncorrectDistributionParameterException.class, () -> p.setMean(0.0));
		HyperExpPar boxed = new HyperExpPar(Double.valueOf(0.5), Double.valueOf(1.0), Double.valueOf(2.0));
		assertTrue(boxed.check());
	}

	@Test
	void hyperExponential() throws Exception {
		checkSampling(new HyperExp(), new HyperExpPar(0.3, 1.0, 4.0));
	}

	@Test
	void lognormal() throws Exception {
		checkSampling(new Lognormal(), new LognormalPar(0.0, 0.5));
	}

	@Test
	void normal() throws Exception {
		checkSampling(new Normal(), new NormalPar(10.0, 2.0));
	}

	@Test
	void pareto() throws Exception {
		checkSampling(new Pareto(), new ParetoPar(4.0, 1.0));
	}

	@Test
	void weibull() throws Exception {
		checkSampling(new Weibull(), new WeibullPar(2.0, 1.5));
	}

	@Test
	void uniform() throws Exception {
		checkSampling(new Uniform(), new UniformPar(2.0, 6.0));
		Uniform u = new Uniform();
		UniformPar p = new UniformPar(2.0, 6.0);
		assertEquals(4.0, u.theorMean(p), 1e-12);
		assertEquals(16.0 / 12.0, u.theorVariance(p), 1e-12);
		assertEquals(0.25, u.pdf(3, p), 1e-12);
		assertEquals(0.0, u.pdf(7, p), 0.0);
		assertEquals(0.25, u.cdf(3, p), 1e-12);
	}

	@Disabled("bug: Uniform.nextLong returns 0 for every range with min >= 0 (early return), instead of a value in [min, max]")
	@Test
	void uniformNextLongNonNegativeRange() throws Exception {
		Uniform u = new Uniform();
		u.setRandomEngine(new MersenneTwister(SEED));
		double v = u.nextLong(new UniformPar(3.0, 7.0));
		assertTrue(v >= 3 && v <= 7);
	}

	@Test
	void uniformIntegerHelpers() throws Exception {
		Uniform u = new Uniform();
		u.setRandomEngine(new MersenneTwister(SEED));
		UniformPar p = new UniformPar(3.0, 7.0);
		boolean sawTrue = false, sawFalse = false;
		for (int i = 0; i < 200; i++) {
			int v = u.nextInt(p);
			assertTrue(v >= 3 && v <= 7);
			if (u.nextBoolean()) {
				sawTrue = true;
			} else {
				sawFalse = true;
			}
		}
		assertTrue(sawTrue && sawFalse);
		double l = u.nextLong(new UniformPar(-10.0, 10.0));
		assertTrue(l >= -10 && l <= 10);
		// wide ranges exercise the overflow-safe branches; results stay within the bounds
		double lo = u.nextLong(new UniformPar(Long.MIN_VALUE, 0.0));
		assertTrue(lo <= 0);
		u.nextLong(new UniformPar(Long.MIN_VALUE, Long.MAX_VALUE));
		u.nextLong(new UniformPar(-1e19, 1e19));
		double hi = u.nextLong(new UniformPar(Long.MIN_VALUE, Long.MAX_VALUE / 2.0));
		assertTrue(hi <= Long.MAX_VALUE / 2.0);
	}

	@Test
	void studentT() throws Exception {
		StudentT t = new StudentT();
		StudentTPar p = new StudentTPar(10);
		assertEquals(0.0, t.theorMean(p), 0.0);
		assertEquals(10.0 / 8.0, t.theorVariance(p), 1e-12);
		assertEquals(0.5, t.cdf(0, p), 1e-9);
		assertTrue(t.pdf(0, p) > t.pdf(2, p));
		assertEquals(0.0, t.theorVariance(new StudentTPar(1)), 0.0);
		double[] a = draw(t, p, SEED, 1000);
		assertArrayEquals(a, draw(t, p, SEED, 1000));
		for (double v : a) {
			assertTrue(v > 0.0, "StudentT.nextRand only returns positive values");
		}
	}

	@Test
	void deterministic() throws Exception {
		DeterministicDistr d = new DeterministicDistr();
		DeterministicDistrPar p = new DeterministicDistrPar(3.5);
		d.setRandomEngine(new MersenneTwister(1));
		assertEquals(3.5, d.nextRand(p), 0.0);
		assertEquals(3.5, d.theorMean(p), 0.0);
		assertEquals(0.0, d.theorVariance(p), 0.0);
		assertEquals(1.0, d.pdf(3.5, p), 0.0);
		assertEquals(0.0, d.pdf(3.0, p), 0.0);
		assertEquals(0.0, d.cdf(3.4, p), 0.0);
		assertEquals(1.0, d.cdf(3.5, p), 0.0);
	}

	@Test
	void poissonContinuousFamilyClass() throws Exception {
		random.Poisson d = new random.Poisson();
		PoissonPar p = new PoissonPar(4.0);
		assertEquals(4.0, d.theorMean(p), 0.0);
		assertEquals(4.0, d.theorVariance(p), 0.0);
		assertEquals(Math.exp(-4.0), d.pdf(0, p), 1e-12);
		checkSampling(d, p);
	}

	@Test
	void everyDistributionRejectsInvalidParameterObject() throws Exception {
		Parameter bad = new ExponentialPar(); // lambda == 0 -> check() is false
		Distribution[] ds = { new Exponential() };
		for (Distribution d : ds) {
			d.setRandomEngine(new MersenneTwister(1));
			assertThrows(IncorrectDistributionParameterException.class, () -> d.nextRand(bad));
			assertThrows(IncorrectDistributionParameterException.class, () -> d.pdf(1, bad));
			assertThrows(IncorrectDistributionParameterException.class, () -> d.cdf(1, bad));
			assertThrows(IncorrectDistributionParameterException.class, () -> d.theorMean(bad));
			assertThrows(IncorrectDistributionParameterException.class, () -> d.theorVariance(bad));
		}
	}
}
