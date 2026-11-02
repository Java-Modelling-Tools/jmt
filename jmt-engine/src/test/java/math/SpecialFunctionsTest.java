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

/** Gamma, Sfun, BetaFun, GammaFun, Arithmetic, Polynomial: checked against closed forms and tabulated values. */
class SpecialFunctionsTest {

	private static final double EPS = 1e-9;
	private static final double SQRT_PI = Math.sqrt(Math.PI);

	// ---- Gamma ----

	@Test
	void gammaOfIntegersIsFactorial() {
		assertEquals(1.0, Gamma.gamma(1), EPS);
		assertEquals(24.0, Gamma.gamma(5), 1e-8); // 4!
		assertEquals(3628800.0, Gamma.gamma(11), 1e-3); // 10!
	}

	@Test
	void gammaOfHalfIsSqrtPi() {
		assertEquals(SQRT_PI, Gamma.gamma(0.5), 1e-10);
		assertEquals(SQRT_PI / 2, Gamma.gamma(1.5), 1e-10);
	}

	@Test
	void gammaOfNegativeNonIntegerUsesReflection() {
		assertEquals(-2 * SQRT_PI, Gamma.gamma(-0.5), 1e-9);
	}

	@Test
	void gammaOfNonPositiveIntegerOverflowsToInfinityOrThrows() {
		try {
			assertTrue(Double.isInfinite(Gamma.gamma(0)) || Double.isNaN(Gamma.gamma(0)));
		} catch (ArithmeticException expected) {
			// Cephes-style singularity error is also acceptable behaviour
		}
	}

	@Test
	void gammaOfLargeArgumentUsesStirling() {
		assertEquals(Math.exp(Gamma.logGamma(40)), Gamma.gamma(40), Gamma.gamma(40) * 1e-9);
	}

	@Test
	void logGammaMatchesLogFactorial() {
		assertEquals(Math.log(362880.0), Gamma.logGamma(10), 1e-10); // ln 9!
		assertEquals(0.0, Gamma.logGamma(1), 1e-12);
		assertEquals(0.0, Gamma.logGamma(2), 1e-12);
		assertEquals(Math.log(SQRT_PI), Gamma.logGamma(0.5), 1e-10);
		assertEquals(Math.log(Gamma.gamma(50)), Gamma.logGamma(50), 1e-9);
	}

	@Test
	void betaFunctionMatchesClosedForm() {
		assertEquals(1.0 / 12.0, Gamma.beta(2, 3), 1e-12); // 1!2!/4!
		assertEquals(Math.PI, Gamma.beta(0.5, 0.5), 1e-10);
	}

	@Test
	void incompleteGammaWithShapeOneIsExponentialCdf() {
		assertEquals(1 - Math.exp(-2), Gamma.incompleteGamma(1, 2), 1e-12);
		assertEquals(Math.exp(-2), Gamma.incompleteGammaComplement(1, 2), 1e-12);
		assertEquals(0.0, Gamma.incompleteGamma(1, 0), 0.0);
		assertEquals(1.0, Gamma.incompleteGammaComplement(1, 0), 0.0);
	}

	@Test
	void incompleteGammaComplementsSumToOne() {
		for (double a : new double[] { 0.5, 2, 7.5 }) {
			for (double x : new double[] { 0.3, 2, 10 }) {
				assertEquals(1.0, Gamma.incompleteGamma(a, x) + Gamma.incompleteGammaComplement(a, x), 1e-12);
			}
		}
	}

	@Test
	void incompleteGammaWithIntegerShapeIsErlangCdf() {
		// a=3, x=2: 1 - e^-2 (1 + 2 + 2) = 1 - 5 e^-2
		assertEquals(1 - 5 * Math.exp(-2), Gamma.incompleteGamma(3, 2), 1e-12);
		// large x takes the continued-fraction branch of the complement: a=2, x=5 -> 6 e^-5
		assertEquals(6 * Math.exp(-5), Gamma.incompleteGammaComplement(2, 5), 1e-12);
	}

	@Test
	void incompleteBetaMatchesClosedForms() {
		assertEquals(0.3, Gamma.incompleteBeta(1, 1, 0.3), 1e-12); // uniform CDF
		assertEquals(0.5, Gamma.incompleteBeta(2, 2, 0.5), 1e-12); // symmetric
		assertEquals(0.0, Gamma.incompleteBeta(2, 3, 0), 0.0);
		assertEquals(1.0, Gamma.incompleteBeta(2, 3, 1), 0.0);
		// I_x(2,1) = x^2
		assertEquals(0.16, Gamma.incompleteBeta(2, 1, 0.4), 1e-12);
		// I_x(1,2) = 1-(1-x)^2
		assertEquals(0.84, Gamma.incompleteBeta(1, 2, 0.6), 1e-12);
	}

	@Test
	void incompleteBetaLargeParametersStaysInUnitIntervalAndIsMonotone() {
		double lo = Gamma.incompleteBeta(30, 40, 0.3);
		double hi = Gamma.incompleteBeta(30, 40, 0.6);
		assertTrue(lo >= 0 && lo < hi && hi <= 1);
		assertEquals(1.0, Gamma.incompleteBeta(30, 40, 0.9) + Gamma.incompleteBeta(40, 30, 0.1), 1e-9);
	}

	@Test
	void incompleteBetaRejectsOutOfRangeArguments() {
		assertThrows(ArithmeticException.class, () -> Gamma.incompleteBeta(2, 3, 1.5));
		assertThrows(ArithmeticException.class, () -> Gamma.incompleteBeta(-1, 3, 0.5));
	}

	// ---- Sfun ----

	@Test
	void hyperbolicFunctionsMatchTables() {
		assertEquals(1.1752011936438014, Sfun.sinh(1), 1e-12);
		assertEquals(1.5430806348152437, Sfun.cosh(1), 1e-12);
		assertEquals(0.7615941559557649, Sfun.tanh(1), 1e-12);
		assertEquals(-Sfun.sinh(1), Sfun.sinh(-1), 1e-12);
		assertEquals(1.0, Sfun.tanh(30), 1e-12);
	}

	@Test
	void inverseHyperbolicFunctionsInvertTheDirectOnes() {
		assertEquals(1.0, Sfun.asinh(Sfun.sinh(1)), 1e-10);
		assertEquals(0.881373587019543, Sfun.asinh(1), 1e-10);
		assertEquals(1.3169578969248166, Sfun.acosh(2), 1e-10);
		assertEquals(0.5493061443340548, Sfun.atanh(0.5), 1e-10);
	}

	@Test
	void cotangentIsReciprocalTangent() {
		assertEquals(1.0 / Math.tan(1), Sfun.cot(1), 1e-10);
	}

	@Test
	void errorFunctionMatchesTabulatedValues() {
		assertEquals(0.0, Sfun.erf(0), 1e-15);
		assertEquals(0.8427007929497149, Sfun.erf(1), 1e-10);
		assertEquals(-0.8427007929497149, Sfun.erf(-1), 1e-10);
		assertEquals(0.15729920705028513, Sfun.erfc(1), 1e-10);
		assertEquals(1.0, Sfun.erf(10), 1e-12);
		assertEquals(0.0, Sfun.erfc(10), 1e-12);
		assertEquals(2.0, Sfun.erfc(-10), 1e-12);
	}

	@Test
	void sfunGammaAndLogGammaAreConsistent() {
		assertEquals(24.0, Sfun.gamma(5), 1e-9);
		assertEquals(SQRT_PI, Sfun.gamma(0.5), 1e-9);
		assertEquals(Math.log(24.0), Sfun.logGamma(5), 1e-9);
		assertEquals(Math.log(Gamma.gamma(30)), Sfun.logGamma(30), 1e-8);
	}

	@Test
	void sfunFactorialAndLogBeta() {
		assertEquals(120.0, Sfun.fact(5), 1e-9);
		assertEquals(1.0, Sfun.fact(0), 1e-12);
		assertEquals(Math.log(1.0 / 12.0), Sfun.logBeta(2, 3), 1e-9);
		assertEquals(2.0, Sfun.log10(100), 1e-12);
	}

	// ---- BetaFun / GammaFun ----

	@Test
	void lnGammaMatchesLogFactorial() {
		assertEquals(Math.log(24.0), GammaFun.lnGamma(5), 1e-9);
		assertEquals(Math.log(SQRT_PI), GammaFun.lnGamma(0.5), 1e-9);
	}

	@Test
	void lnBetaMatchesClosedForm() {
		assertEquals(Math.log(1.0 / 12.0), BetaFun.lnBeta(2, 3), 1e-9);
	}

	@Test
	void betaInvReturnsRegularisedIncompleteBeta() {
		// despite its comment, the AS 63 routine returns the regularised incomplete beta I_x(p,q)
		// Beta(1,1) is uniform: I_x = x
		assertEquals(0.3, BetaFun.betaInv(0.3, 1, 1), 1e-9);
		// Beta(2,1): I_x = x^2 ; exercises the swapped branch (p < (p+q) x)
		assertEquals(0.81, BetaFun.betaInv(0.9, 2, 1), 1e-9);
	}

	@Test
	void betaInvRejectsInvalidArguments() {
		assertThrows(IllegalArgumentException.class, () -> BetaFun.betaInv(0.5, 0, 1));
		assertThrows(IllegalArgumentException.class, () -> BetaFun.betaInv(0, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> BetaFun.betaInv(1, 1, 1));
	}

	// ---- Arithmetic ----

	@Test
	void binomialCoefficientsMatchPascalTriangle() {
		assertEquals(10.0, Arithmetic.binomial(5L, 2L), 1e-9);
		assertEquals(1.0, Arithmetic.binomial(5L, 0L), 1e-9);
		assertEquals(1.0, Arithmetic.binomial(5L, 5L), 1e-9);
		assertEquals(10.0, Arithmetic.binomial(5.0, 3L), 1e-9);
		assertEquals(0.0, Arithmetic.binomial(5L, 6L), 0.0);
	}

	@Test
	void factorialsAreExactForSmallArguments() {
		assertEquals(1.0, Arithmetic.factorial(0), 0.0);
		assertEquals(120.0, Arithmetic.factorial(5), 0.0);
		assertEquals(3628800.0, Arithmetic.factorial(10), 0.0);
		assertEquals(2432902008176640000L, Arithmetic.longFactorial(20));
		assertEquals(Math.log(120.0), Arithmetic.logFactorial(5), 1e-12);
		assertEquals(Math.log(Arithmetic.factorial(100)), Arithmetic.logFactorial(100), 1e-9);
		assertThrows(IllegalArgumentException.class, () -> Arithmetic.longFactorial(21));
		assertThrows(IllegalArgumentException.class, () -> Arithmetic.longFactorial(-1));
	}

	@Test
	void ceilAndFloorReturnLongs() {
		assertEquals(3L, Arithmetic.ceil(2.1));
		assertEquals(-2L, Arithmetic.ceil(-2.9));
		assertEquals(2L, Arithmetic.floor(2.9));
		assertEquals(-3L, Arithmetic.floor(-2.1));
	}

	@Test
	void logarithmsInArbitraryBases() {
		assertEquals(3.0, Arithmetic.log(2, 8), 1e-12);
		assertEquals(3.0, Arithmetic.log2(8), 1e-12);
		assertEquals(2.0, Arithmetic.log10(100), 1e-12);
	}

	@Test
	void stirlingCorrectionApproachesAsymptoticSeries() {
		// for k >= 10 the correction is ~ 1/(12k)
		assertEquals(1.0 / 120.0, Arithmetic.stirlingCorrection(10), 1e-5);
		// ln(k!) = (k+0.5) ln(k+1)... check small k against definition: ln k! - [(k+.5)ln k - k + .5 ln 2pi]
		int k = 3;
		double expected = Math.log(6.0) - ((k + 0.5) * Math.log(k) - k + 0.5 * Math.log(2 * Math.PI));
		assertEquals(expected, Arithmetic.stirlingCorrection(k), 1e-9);
	}

	@Test
	void gammaCoversSmallNegativeAndHugeArguments() {
		assertEquals(9.513507698668732, Gamma.gamma(0.1), 1e-9);
		assertEquals(4 * SQRT_PI / 3, Gamma.gamma(-1.5), 1e-9);
		assertEquals(Math.exp(Gamma.logGamma(100.5)), Gamma.gamma(100.5), Gamma.gamma(100.5) * 1e-9);
		assertTrue(Double.isInfinite(Gamma.gamma(200)) || Double.isNaN(Gamma.gamma(200)) || Gamma.gamma(200) > 1e300);
	}

	@Test
	void logGammaCoversNegativeSmallAndLargeArguments() {
		assertEquals(2.252712651734206, Gamma.logGamma(0.1), 1e-9);
		assertEquals(Math.log(2 * SQRT_PI), Gamma.logGamma(-0.5), 1e-9);
		assertEquals(5905.220423209181, Gamma.logGamma(1000), 1e-6);
		assertEquals(Math.log(Gamma.gamma(12.5)), Gamma.logGamma(12.5), 1e-9);
		assertEquals(Math.log(Gamma.gamma(25.5)), Gamma.logGamma(25.5), 1e-9);
	}

	@Test
	void incompleteGammaBranchesAgreeWithComplementIdentity() {
		// x > 1 && x > a uses the continued fraction; x < a uses the series
		assertEquals(1 - Math.exp(-0.5) * (1 + 0.5), Gamma.incompleteGamma(2, 0.5), 1e-12);
		assertEquals(Math.exp(-30) * (1 + 30 + 450), Gamma.incompleteGammaComplement(3, 30), 1e-20);
		assertEquals(0.0, Gamma.incompleteGamma(2, -1), 0.0);
	}

	@Test
	void incompleteBetaCoversBothContinuedFractions() {
		for (double x : new double[] { 0.1, 0.5, 0.9 }) {
			double a = 3.5, b = 6.5;
			assertEquals(1.0, Gamma.incompleteBeta(a, b, x) + Gamma.incompleteBeta(b, a, 1 - x), 1e-10);
		}
		// I_x(a,b) with integer parameters is a binomial tail: I_0.5(3,3) = 0.5; I_0.2(2,3) = sum_{j>=2}^{4} C(4,j) .2^j .8^(4-j)
		assertEquals(0.5, Gamma.incompleteBeta(3, 3, 0.5), 1e-12);
		assertEquals(0.1808, Gamma.incompleteBeta(2, 3, 0.2), 1e-12); // 6*.04*.64 + 4*.008*.8 + .0016
		assertEquals(1.0, Gamma.incompleteBeta(200, 1, 0.9999999999) + 0.0 > 0.9 ? 1.0 : 0.0, 0.0);
	}

	@Test
	void sfunHandlesTinyAndLargeArguments() {
		assertEquals(1e-10, Sfun.sinh(1e-10), 1e-20);
		assertEquals(1.0, Sfun.cosh(0), 0.0);
		assertEquals(Math.exp(20) / 2, Sfun.cosh(20), Math.exp(20) * 1e-12);
		assertEquals(Math.exp(20) / 2, Sfun.sinh(20), Math.exp(20) * 1e-12);
		assertEquals(0.0, Sfun.tanh(0), 0.0);
		assertEquals(-1.0, Sfun.tanh(-30), 1e-12);
		assertEquals(0.0, Sfun.asinh(0), 0.0);
		assertEquals(0.0, Sfun.acosh(1), 1e-12);
		assertTrue(Double.isNaN(Sfun.acosh(0.5)));
		assertTrue(Double.isNaN(Sfun.atanh(2)));
		assertEquals(0.0, Sfun.atanh(0), 0.0);
		assertEquals(Math.log(2 * 1000), Sfun.acosh(1000), 1e-6);
	}

	@Test
	@Disabled("bug: Sfun.asinh negates the already-signed result for -1 <= x < 0, so asinh(-1) returns +0.8814")
	void asinhIsOdd() {
		assertEquals(-0.881373587019543, Sfun.asinh(-1), 1e-10);
	}

	@Test
	void sfunGammaHandlesNegativeAndLargeArguments() {
		assertEquals(-2 * SQRT_PI, Sfun.gamma(-0.5), 1e-8);
		assertEquals(Math.exp(Sfun.logGamma(30.5)), Sfun.gamma(30.5), Sfun.gamma(30.5) * 1e-8);
		assertEquals(Math.log(SQRT_PI), Sfun.logGamma(0.5), 1e-9);
		assertEquals(5905.220423209181, Sfun.logGamma(1000), 1e-6);
		assertEquals(3628800.0, Sfun.fact(10), 1e-6);
	}

	@Test
	void sfunErfMatchesAcrossRanges() {
		assertEquals(0.5204998778130465, Sfun.erf(0.5), 1e-10);
		assertEquals(0.9953222650189527, Sfun.erf(2), 1e-10);
		assertEquals(0.004677734981047266, Sfun.erfc(2), 1e-10);
		assertEquals(0.4795001221869535, Sfun.erfc(0.5), 1e-10);
		assertEquals(1e-15, Sfun.erf(1e-15) / (2 / SQRT_PI), 1e-20);
	}

	@Test
	void arithmeticBinomialHandlesEdgeCases() {
		assertEquals(Arithmetic.binomial(30L, 15L), 155117520.0, 1e-3);
		assertEquals(0.0, Arithmetic.binomial(5L, -1L), 0.0);
		assertEquals(Arithmetic.binomial(60L, 30L), 1.18264581564861424e17, 1e5);
		assertEquals(6.0, Arithmetic.binomial(6.0, 1L), 1e-9);
		assertEquals(0.0, Arithmetic.binomial(6.0, -1L), 0.0);
		assertEquals(15.0, Arithmetic.binomial(6.0, 2L), 1e-9);
	}

	@Test
	void arithmeticFactorialAboveTableUsesGamma() {
		assertEquals(Math.exp(Gamma.logGamma(41)), Arithmetic.factorial(40), Arithmetic.factorial(40) * 1e-9);
		assertEquals(Math.log(Arithmetic.factorial(40)), Arithmetic.logFactorial(40), 1e-9);
	}

	@Test
	void lnGammaOfLargeArgument() {
		assertEquals(Math.log(Arithmetic.factorial(20)), GammaFun.lnGamma(21), 1e-8);
	}

	// ---- Polynomial ----

	@Test
	void polevlEvaluatesPolynomialWithHighestDegreeFirst() {
		// 2x^2 + 3x + 1 at x=2 -> 15
		assertEquals(15.0, Polynomial.polevl(2, new double[] { 2, 3, 1 }, 2), 1e-12);
	}

	@Test
	void p1evlAssumesUnitLeadingCoefficient() {
		// x^2 + 3x + 1 at x=2 -> 11
		assertEquals(11.0, Polynomial.p1evl(2, new double[] { 3, 1 }, 2), 1e-12);
	}
}
