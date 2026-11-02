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

import jmt.common.exception.IncorrectDistributionParameterException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DistributionParametersTest {

	private static final double NAN = Double.NaN;

	private static void rejects(Executable e) {
		assertThrows(IncorrectDistributionParameterException.class, e);
	}

	@Test
	void zeroAndNegativeAreRejectedByConstructors() {
		rejects(() -> new ExponentialPar(0.0));
		rejects(() -> new ExponentialPar(-1.0));
		rejects(() -> new ErlangPar(0.0, 2L));
		rejects(() -> new ErlangPar(1.0, 0L));
		rejects(() -> new ErlangPar(-1.0, 2L));
		rejects(() -> new GammaDistrPar(0.0, 1.0));
		rejects(() -> new GammaDistrPar(1.0, -1.0));
		rejects(() -> new HyperExpPar(1.5, 1.0, 1.0));
		rejects(() -> new HyperExpPar(0.5, 0.0, 1.0));
		rejects(() -> new HyperExpPar(0.5, 1.0, -1.0));
		rejects(() -> new LognormalPar(0.0, 0.0));
		rejects(() -> new NormalPar(0.0, 0.0));
		rejects(() -> new NormalPar(0.0, -1.0));
		rejects(() -> new ParetoPar(0.0, 1.0));
		rejects(() -> new ParetoPar(2.0, -1.0));
		rejects(() -> new PoissonPar(0.0));
		rejects(() -> new PoissonPar(-2.0));
		rejects(() -> new StudentTPar(0));
		rejects(() -> new StudentTPar(1.5));
		rejects(() -> new UniformPar(3.0, 3.0));
		rejects(() -> new UniformPar(4.0, 3.0));
		rejects(() -> new WeibullPar(0.0, 1.0));
		rejects(() -> new WeibullPar(1.0, -1.0));
		rejects(() -> new DeterministicDistrPar(0.0));
		rejects(() -> new DeterministicDistrPar(-1.0));
	}

	@Disabled("bug: StudentTPar and UniformPar accept NaN (check is written as the negation of a comparison)")
	@Test
	void nanIsRejectedByNegatedChecks() {
		rejects(() -> new StudentTPar(NAN));
		rejects(() -> new UniformPar(NAN, 1.0));
		rejects(() -> new UniformPar(0.0, NAN));
	}

	@Disabled("bug: parameter checks written as 'x <= 0' accept NaN (Exponential, Erlang, Gamma, Poisson, Normal, Weibull, Pareto, Lognormal, Deterministic)")
	@Test
	void nanIsRejectedByComparisonStyleChecks() {
		rejects(() -> new ExponentialPar(NAN));
		rejects(() -> new ErlangPar(NAN, 2L));
		rejects(() -> new GammaDistrPar(NAN, 1.0));
		rejects(() -> new PoissonPar(NAN));
		rejects(() -> new NormalPar(0.0, NAN));
		rejects(() -> new WeibullPar(NAN, 1.0));
		rejects(() -> new ParetoPar(NAN, 1.0));
		rejects(() -> new LognormalPar(0.0, NAN));
		rejects(() -> new DeterministicDistrPar(NAN));
	}

	@Disabled("bug: ErlangPar.setAlpha/setR (and DeterministicDistrPar.setParameterValue, StudentTPar.setFreedom) store the invalid value before throwing")
	@Test
	void failedSetterLeavesParameterUnchanged() throws Exception {
		ErlangPar er = new ErlangPar(2.0, 3L);
		rejects(() -> er.setAlpha(-1.0));
		assertEquals(2.0, er.getAlpha(), 0.0);
		DeterministicDistrPar d = new DeterministicDistrPar(1.0);
		rejects(() -> d.setParameterValue(0.0));
		assertEquals(1.0, d.getParameterValue(), 0.0);
		StudentTPar t = new StudentTPar(3.0);
		rejects(() -> t.setFreedom(0.0));
		assertEquals(3.0, t.getFreedom(), 0.0);
	}

	@Disabled("bug: GammaDistr.theorMean (alpha/lambda) disagrees with GammaDistrPar.setMean and nextRand (alpha*lambda)")
	@Test
	void gammaSetMeanMatchesTheorMean() throws Exception {
		GammaDistrPar g = new GammaDistrPar(2.0, 2.0);
		g.setMean(10.0);
		assertEquals(10.0, new GammaDistr().theorMean(g), 1e-9);
	}

	@Test
	void erlangSettersRejectBadValues() throws Exception {
		rejects(() -> new ErlangPar(2.0, 3L).setAlpha(-1.0));
		rejects(() -> new ErlangPar(2.0, 3L).setR(0L));
	}

	@Test
	void settersRejectBadValuesAndKeepOldOnes() throws Exception {
		ExponentialPar e = new ExponentialPar(2.0);
		rejects(() -> e.setLambda(0.0));
		rejects(() -> e.setMean(-1.0));
		assertEquals(2.0, e.getLambda(), 0.0);
		e.setMean(4.0);
		assertEquals(0.25, e.getLambda(), 1e-12);
		assertTrue(e.check());
		assertFalse(new ExponentialPar().check());

		ErlangPar er = new ErlangPar(2.0, 3L);
		rejects(() -> er.setMean(0.0));
		er.setMean(3.0);
		assertEquals(3.0, new Erlang().theorMean(er), 1e-9);

		GammaDistrPar g = new GammaDistrPar(2.0, 2.0);
		rejects(() -> g.setAlpha(0.0));
		rejects(() -> g.setLambda(0.0));
		rejects(() -> g.setMean(0.0));
		g.setMean(10.0);
		assertEquals(5.0, g.getLambda(), 0.0);

		NormalPar n = new NormalPar(1.0, 2.0);
		rejects(() -> n.setStandardDeviation(0.0));
		n.setMean(5.0);
		assertEquals(5.0, n.getMean(), 0.0);

		LognormalPar l = new LognormalPar(1.0, 2.0);
		rejects(() -> l.setSigma(0.0));
		l.setMu(0.5);
		assertEquals(0.5, l.getMu(), 0.0);

		ParetoPar pa = new ParetoPar(3.0, 1.0);
		rejects(() -> pa.setAlpha(0.0));
		rejects(() -> pa.setK(0.0));
		rejects(() -> pa.setMean(0.0));

		WeibullPar w = new WeibullPar(1.0, 1.0);
		rejects(() -> w.setAlpha(0.0));
		rejects(() -> w.setR(0.0));
		rejects(() -> w.setMean(0.0));

		PoissonPar po = new PoissonPar(1.0);
		rejects(() -> po.setLambda(0.0));
		rejects(() -> po.setMean(0.0));
		po.setMean(7.0);
		assertEquals(7.0, po.getLambda(), 0.0);

		StudentTPar t = new StudentTPar(3.0);
		rejects(() -> t.setFreedom(0.0));

		UniformPar u = new UniformPar(0.0, 1.0);
		rejects(() -> u.setMin(2.0));
		rejects(() -> u.setMax(-1.0));
		rejects(() -> u.setMean(0.0));

		DeterministicDistrPar d = new DeterministicDistrPar(1.0);
		rejects(() -> d.setParameterValue(0.0));
		rejects(() -> d.setMean(0.0));
		d.setMean(2.0);
		assertEquals(2.0, d.getParameterValue(), 0.0);
	}
}
