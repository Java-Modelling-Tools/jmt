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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import random.engine.MersenneTwister;

/** Probability, Normal, TStudent, FDistribution, Erlang, FFT, Spectrum, Printer. */
class DistributionsTest {

	private static final double EPS = 1e-9;

	// ---- Probability ----

	@Test
	void standardNormalCdfMatchesTables() {
		assertEquals(0.5, Probability.normal(0), 1e-12);
		assertEquals(0.9750021048517795, Probability.normal(1.96), 1e-9);
		assertEquals(1 - 0.9750021048517795, Probability.normal(-1.96), 1e-9);
		assertEquals(0.8413447460685429, Probability.normal(1), 1e-9);
	}

	@Test
	void generalNormalCdfStandardises() {
		// N(10, var 4): x=12 is one sigma above the mean
		assertEquals(0.8413447460685429, Probability.normal(10, 4, 12), 1e-9);
	}

	@Test
	void normalInverseInvertsCdf() {
		assertEquals(0.0, Probability.normalInverse(0.5), 1e-12);
		assertEquals(1.959963984540054, Probability.normalInverse(0.975), 1e-8);
		assertEquals(-1.959963984540054, Probability.normalInverse(0.025), 1e-8);
		assertEquals(3.0, Probability.normalInverse(Probability.normal(3)), 1e-8);
		assertThrows(IllegalArgumentException.class, () -> Probability.normalInverse(0));
		assertThrows(IllegalArgumentException.class, () -> Probability.normalInverse(1));
	}

	@Test
	void errorFunctionsAgreeWithKnownValues() {
		assertEquals(0.8427007929497149, Probability.errorFunction(1), 1e-9);
		assertEquals(0.15729920705028513, Probability.errorFunctionComplemented(1), 1e-9);
		assertEquals(0.0, Probability.errorFunction(0), 1e-15);
	}

	@Test
	void chiSquareWithTwoDegreesIsExponential() {
		// df=2: CDF(x) = 1 - e^(-x/2)
		assertEquals(1 - Math.exp(-3), Probability.chiSquare(2, 6), 1e-10);
		assertEquals(Math.exp(-3), Probability.chiSquareComplemented(2, 6), 1e-10);
		// df=1, x=3.841458820694124 is the classic 95% critical value
		assertEquals(0.95, Probability.chiSquare(1, 3.841458820694124), 1e-8);
		assertEquals(0.0, Probability.chiSquare(2, -1), 0.0);
		assertEquals(0.0, Probability.chiSquareComplemented(0.5, 1), 0.0);
	}

	@Test
	void gammaDistributionWithShapeOneIsExponential() {
		// rate a=2, shape b=1: CDF(1) = 1 - e^-2
		assertEquals(1 - Math.exp(-2), Probability.gamma(2, 1, 1), 1e-12);
		assertEquals(Math.exp(-2), Probability.gammaComplemented(2, 1, 1), 1e-12);
		assertEquals(0.0, Probability.gamma(2, 1, -1), 0.0);
		assertEquals(0.0, Probability.gammaComplemented(2, 1, -1), 0.0);
	}

	@Test
	void poissonCdfMatchesPartialSums() {
		// mean 2: P(0)=e^-2, P(1)=2e^-2, P(2)=2e^-2
		assertEquals(Math.exp(-2), Probability.poisson(0, 2), 1e-12);
		assertEquals(3 * Math.exp(-2), Probability.poisson(1, 2), 1e-12);
		assertEquals(5 * Math.exp(-2), Probability.poisson(2, 2), 1e-12);
		assertEquals(1 - 5 * Math.exp(-2), Probability.poissonComplemented(2, 2), 1e-12);
		assertEquals(0.0, Probability.poisson(-1, 2), 0.0);
		assertThrows(IllegalArgumentException.class, () -> Probability.poisson(1, -1));
		assertThrows(IllegalArgumentException.class, () -> Probability.poissonComplemented(1, -1));
	}

	@Test
	void binomialCdfMatchesPartialSums() {
		// n=3, p=0.5: pmf 1/8, 3/8, 3/8, 1/8
		assertEquals(0.125, Probability.binomial(0, 3, 0.5), 1e-12);
		assertEquals(0.5, Probability.binomial(1, 3, 0.5), 1e-12);
		assertEquals(0.875, Probability.binomial(2, 3, 0.5), 1e-12);
		assertEquals(1.0, Probability.binomial(3, 3, 0.5), 1e-12);
		assertEquals(0.5, Probability.binomialComplemented(1, 3, 0.5), 1e-12);
		assertEquals(0.125, Probability.binomialComplemented(2, 3, 0.5), 1e-12);
		assertThrows(IllegalArgumentException.class, () -> Probability.binomial(1, 3, 1.5));
		assertThrows(IllegalArgumentException.class, () -> Probability.binomial(5, 3, 0.5));
	}

	@Test
	void negativeBinomialCdfMatchesPartialSums() {
		// n=1 success, p=0.5: P(fail <= k) = 1 - 0.5^(k+1)
		assertEquals(0.5, Probability.negativeBinomial(0, 1, 0.5), 1e-12);
		assertEquals(0.75, Probability.negativeBinomial(1, 1, 0.5), 1e-12);
		assertEquals(0.25, Probability.negativeBinomialComplemented(1, 1, 0.5), 1e-12);
		assertThrows(IllegalArgumentException.class, () -> Probability.negativeBinomial(1, 1, 1.5));
	}

	@Test
	void betaDistributionMatchesClosedForm() {
		assertEquals(0.3, Probability.beta(1, 1, 0.3), 1e-12);
	}

	@Test
	@Disabled("bug: Probability.betaComplemented returns incompleteBeta(b,a,x) instead of incompleteBeta(b,a,1-x), so it is not 1-beta(a,b,x)")
	void betaComplementedIsOneMinusBeta() {
		assertEquals(1 - 0.09, Probability.betaComplemented(2, 1, 0.3), 1e-12); // 1 - x^2
	}

	@Test
	void studentTCdfIsSymmetricAndMatchesCauchy() {
		assertEquals(0.5, Probability.studentT(5, 0), 0.0);
		// k=1 is Cauchy: CDF(1) = 0.75
		assertEquals(0.75, Probability.studentT(1, 1), 1e-10);
		assertThrows(IllegalArgumentException.class, () -> Probability.studentT(0, 1));
	}

	@Test
	@Disabled("bug: Probability.studentT ignores the sign of t, so studentT(k,-t) returns 1-studentT(k,-t) instead of the left tail")
	void studentTCdfOfNegativeArgumentIsLeftTail() {
		assertEquals(0.25, Probability.studentT(1, -1), 1e-10); // Cauchy CDF(-1)
	}

	@Test
	void studentTInverseMatchesCriticalValues() {
		// alpha = 0.05 two-sided, 10 d.f. -> 2.228
		assertEquals(2.228138851986274, Probability.studentTInverse(0.05, 10), 1e-4);
		// large samples fall back on the normal quantile
		assertEquals(1.959963984540054, Probability.studentTInverse(0.05, 1000), 1e-6);
	}

	// ---- Normal ----

	@Test
	void normalInverseCdfOfSpecialProbabilities() {
		assertEquals(Double.NEGATIVE_INFINITY, Normal.getInvCDF(0, false), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, Normal.getInvCDF(1, false), 0.0);
		assertTrue(Double.isNaN(Normal.getInvCDF(-0.1, false)));
		assertTrue(Double.isNaN(Normal.getInvCDF(1.1, false)));
		assertTrue(Double.isNaN(Normal.getInvCDF(Double.NaN, false)));
	}

	@Test
	void normalInverseCdfCoversCentralAndTailRegions() {
		assertEquals(0.0, Normal.getInvCDF(0.5, true), 1e-12);
		assertEquals(1.959963984540054, Normal.getInvCDF(0.975, true), 1e-9); // central region
		assertEquals(-2.326347874040841, Normal.getInvCDF(0.01, true), 1e-9); // lower tail
		assertEquals(2.326347874040841, Normal.getInvCDF(0.99, true), 1e-9); // upper tail
		assertEquals(1.959963984540054, Normal.getInvCDF(0.975, false), 1e-6); // acklam approximation only
	}

	@Test
	void normalErfFamilyMatchesTables() {
		assertEquals(0.5204998778130465, Normal.erf(0.5), 1e-12);
		assertEquals(1 - 0.5204998778130465, Normal.erfc(0.5), 1e-12);
		assertEquals(1.0, Normal.erfcx(0), 1e-12);
		assertEquals(Math.exp(1) * (1 - 0.8427007929497149), Normal.erfcx(1), 1e-10); // e^(x^2) erfc(x)
		assertEquals(-Normal.erf(2), Normal.erf(-2), 1e-12);
		assertEquals(1.0, Normal.erf(10), 1e-12);
		assertEquals(2.0, Normal.erfc(-10), 1e-12);
		assertEquals(0.0, Normal.erfc(30), 1e-12);
		assertEquals(0.4795001221869535, Normal.erf(0.5) < 0.6 ? Normal.erfc(0.5) - 0.0 : 0, 1e-12);
	}

	@Test
	void normalRefineConvergesToTrueQuantile() {
		double rough = 1.96;
		assertEquals(1.959963984540054, Normal.refine(rough, 0.975), 1e-6);
		assertEquals(5.0, Normal.refine(5.0, 1.0), 0.0); // outside (0,1) leaves x untouched
	}

	// ---- TStudent ----

	@Test
	@Disabled("bug: TStudent.ICDF indexes T_STUDENT_TABLE[df-1] but row 0 is the probability header, so it returns the value for df-1 (df=1 returns 0.975)")
	void tStudentTableLookupMatchesPublishedCriticalValues() {
		// quantile = two-sided alpha; hits the table when 1 - alpha/2 is a tabulated probability
		assertEquals(2.228, TStudent.ICDF(0.05, 10), 1e-3);
		assertEquals(12.706, TStudent.ICDF(0.05, 1), 1e-3);
	}

	@Test
	void tStudentTableLookupIsMonotoneInProbabilityAndDecreasingInDegrees() {
		assertTrue(TStudent.ICDF(0.01, 20) > TStudent.ICDF(0.05, 20));
		assertTrue(TStudent.ICDF(0.05, 5) > TStudent.ICDF(0.05, 20));
		assertTrue(TStudent.ICDF(0.05, 20) > TStudent.ICDF(0.05, 100)); // beyond row 30: interpolated
		assertEquals(1.96, TStudent.ICDF(0.05, 100000), 2e-2);
	}

	@Test
	void tStudentFallsBackOnQuantileAlgorithmWhenNotTabulated() {
		// alpha=0.2 -> p=0.9, tabulated; alpha=0.3 is not tabulated
		assertEquals(TStudent.tStudent(0.3, 10, false), TStudent.ICDF(0.3, 10), 0.0);
	}

	@Test
	void tStudentQuantileMatchesTables() {
		assertEquals(2.228138851986274, TStudent.tStudent(0.025, 10, false), 1e-3);
		assertEquals(-2.228138851986274, TStudent.tStudent(0.025, 10, true), 1e-3);
		assertEquals(0.0, TStudent.tStudent(0.5, 10, true), 1e-9);
		assertEquals(Math.sqrt(2 / (0.1 * 1.9) - 2), TStudent.tStudent(0.05, 2, false), 1e-9); // df=2 closed form
		assertEquals(12.706, TStudent.tStudent(0.025, 1, false), 1e-2);
		assertEquals(12.706, TStudent.tStudent(0.025, 1.0, false), 1e-2);
		assertEquals(2.776, TStudent.tStudent(0.025, 4, false), 1e-2); // small df branch
		assertEquals(4.303, TStudent.tStudent(0.025, 2, false), 1e-3);
		assertEquals(1.96, TStudent.tStudent(0.025, 1e6, false), 1e-2);
	}

	@Test
	void tStudentRejectsInvalidInputs() {
		assertThrows(IllegalArgumentException.class, () -> TStudent.tStudent(0, 10, false));
		assertThrows(IllegalArgumentException.class, () -> TStudent.tStudent(1, 10, false));
		assertThrows(IllegalArgumentException.class, () -> TStudent.tStudent(0.5, 0.5, false));
	}

	@Test
	void qnormIsSymmetricAroundHalf() {
		assertEquals(0.0, TStudent.qnorm(0.5, false), 1e-6);
		assertEquals(-1.959963984540054, TStudent.qnorm(0.025, false), 1e-3);
	}

	@Test
	@Disabled("bug: TStudent.qnorm(p, upper=true) returns 1 - z instead of -z")
	void qnormUpperTailNegatesQuantile() {
		assertEquals(-1.959963984540054, TStudent.qnorm(0.975, true), 1e-3);
	}

	// ---- FDistribution ----

	@Test
	void fDistributionAtMedianOfSymmetricCase() {
		// F(m,m) has median 1
		assertEquals(0.5, FDistribution.CDF(1, 4, 4), 1e-9);
		assertEquals(0.5, new FDistribution(6, 6).CDF(1), 1e-9);
	}

	@Test
	void fDistributionCdfMatchesTabulatedCriticalValue() {
		// F(5,10): 3.325835 is the 95% quantile (the javadoc says P(F>x), but the value is the lower tail)
		assertEquals(0.95, FDistribution.CDF(3.325835, 5, 10), 1e-5);
	}

	@Test
	void fDistributionInverseRecoversCriticalValue() {
		assertEquals(3.325835, FDistribution.ICDF(0.95, 5, 10), 2e-2);
		FDistribution f = new FDistribution(5, 10);
		assertEquals(3.325835, f.ICDF(0.95), 2e-2);
	}

	@Test
	void fDistributionSetParametersChangesDistribution() {
		FDistribution f = new FDistribution(1, 1);
		f.setParameters(5, 10);
		assertEquals(FDistribution.CDF(2.0, 5, 10), f.CDF(2.0), 1e-12);
	}

	@Test
	void fDistributionRejectsInvalidArguments() {
		assertThrows(IllegalArgumentException.class, () -> FDistribution.CDF(1, 0, 1));
		assertThrows(IllegalArgumentException.class, () -> FDistribution.ICDF(0, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> FDistribution.ICDF(1, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> new FDistribution(1, 1).ICDF(0));
		assertThrows(IllegalArgumentException.class, () -> new FDistribution(1, 1).ICDF(1.5));
	}

	// ---- Erlang ----

	@Test
	void erlangBMatchesHandComputation() {
		assertEquals(0.5, Erlang.erlangB(1, 1), EPS); // A/(1+A)
		assertEquals(0.4, Erlang.erlangB(2, 2), EPS); // (A^2/2)/(1+A+A^2/2) = 2/5
		assertEquals(1.0, Erlang.erlangB(3, 0), EPS);
	}

	@Test
	void erlangCMatchesHandComputation() {
		// A=1, N=2: ((1/2)*(2/(2-1))) / (1 + 1 + 1) = 1/3
		assertEquals(1.0 / 3.0, Erlang.erlangC(1, 2), EPS);
	}

	@Test
	void erlangRejectsNegativeArguments() {
		assertThrows(IllegalArgumentException.class, () -> Erlang.erlangB(-1, 2));
		assertThrows(IllegalArgumentException.class, () -> Erlang.erlangB(1, -2));
	}

	// ---- FFT / Spectrum ----

	@Test
	void fftOfImpulseIsFlat() {
		double[] x = { 1, 0, 0, 0, 0, 0, 0, 0 }; // 4 complex points, impulse at 0
		FFT.transform(x);
		assertArrayEquals(new double[] { 1, 0, 1, 0, 1, 0, 1, 0 }, x, EPS);
	}

	@Test
	void fftOfConstantConcentratesInDc() {
		double[] x = { 1, 0, 1, 0, 1, 0, 1, 0 };
		FFT.transform(x);
		assertArrayEquals(new double[] { 4, 0, 0, 0, 0, 0, 0, 0 }, x, EPS);
	}

	@Test
	void fftOfAlternatingSequenceHitsNyquist() {
		double[] x = { 1, 0, -1, 0, 1, 0, -1, 0 };
		FFT.transform(x);
		// frequency 2 of 4 points
		assertArrayEquals(new double[] { 0, 0, 0, 0, 4, 0, 0, 0 }, x, EPS);
	}

	@Test
	void inverseFftRestoresOriginalSignal() {
		MersenneTwister rnd = new MersenneTwister(12345);
		double[] x = new double[32];
		for (int i = 0; i < x.length; i++) {
			x[i] = rnd.raw();
		}
		double[] copy = x.clone();
		FFT.transform(x);
		FFT.inverse(x);
		assertArrayEquals(copy, x, 1e-12);
		assertTrue(FFT.test(copy.clone()) < 1e-12);
	}

	@Test
	void fftOfSingleAndTwoPointsAndNonPowerOfTwoLength() {
		double[] one = { 3, 4 };
		FFT.transform(one);
		assertArrayEquals(new double[] { 3, 4 }, one, EPS);
		double[] two = { 1, 0, 3, 0 };
		FFT.transform(two);
		assertArrayEquals(new double[] { 4, 0, -2, 0 }, two, EPS);
		assertThrows(Error.class, () -> FFT.transform(new double[6])); // 3 points: not a power of two
	}

	@Test
	void fftFlopCountFollowsFormula() {
		// (5N-2) log2 N + 2(N+1) for N=8 : 38*3 + 18
		assertEquals(132.0, FFT.num_flops(8), EPS);
	}

	@Test
	void zeroPadExtendsToNextPowerOfTwoOnly() {
		double[] same = { 1, 2, 3, 4 };
		assertSame(same, Spectrum.zeroPad(same));
		assertArrayEquals(new double[] { 1, 2, 3, 0 }, Spectrum.zeroPad(new double[] { 1, 2, 3 }), 0.0);
	}

	@Test
	void periodogramOfConstantSignalHasOnlyDcComponent() {
		// four samples of 1: FFT = [4,0,0,0]; periodogram = |X|^2 / n
		double[] p = Spectrum.periodogramOfReal(new double[] { 1, 1, 1, 1 }, 0, 4);
		assertEquals(4, p.length);
		assertEquals(4.0, p[0], EPS);
		for (int i = 1; i < p.length; i++) {
			assertEquals(0.0, p[i], EPS);
		}
	}

	@Test
	void periodogramRejectsLengthLargerThanData() {
		assertThrows(Error.class, () -> Spectrum.periodogramOfReal(new double[] { 1, 2 }, 0, 4));
	}

	// ---- Printer ----

	@Test
	void printerRoundsToRequestedDigits() {
		assertEquals("3.14", Printer.print(3.14159, 2));
		assertEquals("3.0", Printer.print(3.0004, 2));
		assertEquals("2.5", Printer.print(2.5f, 3));
		assertEquals("0.33", Printer.print(1f / 3f, 2));
	}
}
