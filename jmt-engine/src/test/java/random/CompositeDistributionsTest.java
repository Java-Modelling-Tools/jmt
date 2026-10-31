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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import jmt.common.exception.IncorrectDistributionParameterException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import random.discrete.Uniform;
import random.discrete.UniformPar;
import random.engine.MersenneTwister;

/** Phase-type style and trace/table driven distributions: Coxian, PhaseType, MMPP2, MAP, Empirical, Batch, Replayer. */
class CompositeDistributionsTest {

	private static final long SEED = 777L;

	private static void checkMean(Distribution d, Parameter p) throws Exception {
		double[] a = draw(d, p, SEED, N);
		double theor = d.theorMean(p);
		assertEquals(theor, mean(a), theor * 0.03, d.getClass().getSimpleName() + " sample mean");
		// a fresh instance with the same seed replays the same sequence
		Distribution twin = d.getClass().newInstance();
		assertArrayEquals(java.util.Arrays.copyOf(a, 500), draw(twin, p, SEED, 500));
		assertFalse(java.util.Arrays.equals(java.util.Arrays.copyOf(a, 500), draw(d.getClass().newInstance(), p, SEED + 1, 500)));
	}

	private static Object[] row(double... v) {
		Object[] r = new Object[v.length];
		for (int i = 0; i < v.length; i++) {
			r[i] = Double.valueOf(v[i]);
		}
		return r;
	}

	// ---- Coxian ----

	@Test
	void coxianMeanAndValidation() throws Exception {
		CoxianPar p = new CoxianPar(2.0, 1.0, 0.25);
		assertEquals(0.5 + 0.75, p.getMean(), 1e-12);
		assertTrue(p.getVar() > 0);
		assertEquals(2.0, p.getLambda0(), 0.0);
		assertEquals(1.0, p.getLambda1(), 0.0);
		assertEquals(0.25, p.getPhi0(), 0.0);
		assertEquals(2.0, p.getExpParam0().getLambda(), 0.0);
		assertEquals(1.0, p.getExpParam1().getLambda(), 0.0);
		assertEquals(p.getMean(), new CoxianDistr().theorMean(p), 0.0);
		assertEquals(p.getVar(), new CoxianDistr().theorVariance(p), 0.0);
		checkMean(new CoxianDistr(), p);
		assertEquals(0.0, new CoxianDistr().pdf(1, p), 0.0);
		assertEquals(0.0, new CoxianDistr().cdf(1, p), 0.0);
		assertEquals(5.0, new CoxianPar(Double.valueOf(2.0), Double.valueOf(1.0), Double.valueOf(0.0)).getMean() + 3.5, 1e-12);
		assertThrows(IncorrectDistributionParameterException.class, () -> new CoxianPar(0.0, 1.0, 0.5));
		assertThrows(IncorrectDistributionParameterException.class, () -> new CoxianPar(1.0, -1.0, 0.5));
		assertThrows(IncorrectDistributionParameterException.class, () -> new CoxianPar(1.0, 1.0, 1.5));
		assertThrows(IncorrectDistributionParameterException.class, () -> new CoxianPar(1.0, 1.0, -0.5));
	}

	@Test
	void coxianSetMeanKeepsParametersValid() throws Exception {
		CoxianPar high = new CoxianPar(2.0, 1.0, 0.25); // squared cv > 1 branch
		high.setMean(10.0);
		assertEquals(10.0, high.getMean(), 0.0);
		assertTrue(high.check());
		CoxianPar one = new CoxianPar(1.0, 1.0, 1.0); // exponential: c == 1
		one.setMean(4.0);
		assertEquals(0.25, one.getLambda0(), 1e-12);
		assertTrue(one.check());
		CoxianPar low = new CoxianPar(1.0, 1.0, 0.0); // c < 1 branch
		low.setMean(4.0);
		assertEquals(4.0, low.getMean(), 0.0);
		assertThrows(IncorrectDistributionParameterException.class, () -> low.setMean(0.0));
	}

	// ---- Phase type ----

	@Test
	void phaseTypeErlang2() throws Exception {
		// Erlang-2 with rate 2 in each phase: mean 1, variance 0.5
		PhaseTypePar p = new PhaseTypePar(new Object[] { row(1.0, 0.0) }, new Object[] { row(-2.0, 2.0), row(0.0, -2.0) });
		assertEquals(1.0, p.getMean(), 1e-9);
		assertEquals(0.5, p.getVar(), 1e-9);
		assertEquals(2, p.getAlpha().length);
		assertEquals(2.0, p.getLambda()[0], 0.0);
		assertEquals(1.0, p.getP()[0][1], 1e-12);
		assertEquals(1.0, new PhaseTypeDistr().theorMean(p), 1e-9);
		assertEquals(0.5, new PhaseTypeDistr().theorVariance(p), 1e-9);
		assertEquals(0.0, new PhaseTypeDistr().pdf(1, p), 0.0);
		assertEquals(0.0, new PhaseTypeDistr().cdf(1, p), 0.0);
		checkMean(new PhaseTypeDistr(), p);
	}

	@Test
	void phaseTypeValidation() {
		Object[] goodT = { row(-2.0, 2.0), row(0.0, -2.0) };
		assertThrows(IncorrectDistributionParameterException.class, () -> new PhaseTypePar(new Object[] { row(0.5, 0.0) }, goodT));
		assertThrows(IncorrectDistributionParameterException.class, () -> new PhaseTypePar(new Object[] { row(-0.5, 1.5) }, goodT));
		assertThrows(IncorrectDistributionParameterException.class,
				() -> new PhaseTypePar(new Object[] { row(1.0, 0.0) }, new Object[] { row(2.0, -2.0), row(0.0, -2.0) }));
		assertThrows(IncorrectDistributionParameterException.class,
				() -> new PhaseTypePar(new Object[] { row(1.0, 0.0) }, new Object[] { row(-2.0, -1.0), row(0.0, -2.0) }));
		assertThrows(IncorrectDistributionParameterException.class,
				() -> new PhaseTypePar(new Object[] { row(1.0, 0.0) }, new Object[] { row(-2.0, 3.0), row(0.0, -2.0) }));
		assertThrows(IncorrectDistributionParameterException.class,
				() -> new PhaseTypePar(new Object[] { row(1.0, 0.0, 0.0) }, goodT));
		assertThrows(IncorrectDistributionParameterException.class,
				() -> new PhaseTypePar(new Object[] { row(1.0, 0.0) }, new Object[] { row(-2.0), row(0.0, -2.0) }));
	}

	// ---- MMPP2 ----

	@Test
	void mmpp2() throws Exception {
		MMPP2Par p = new MMPP2Par(1.0, 4.0, 0.5, 0.5);
		assertTrue(p.check());
		assertEquals(0.5, p.getSigma0(), 0.0);
		assertEquals(0.5, p.getSigma1(), 0.0);
		assertEquals(1.0, p.getLambda0(), 0.0);
		assertEquals(4.0, p.getLambda1(), 0.0);
		assertEquals(1.5, p.getExpParam0().getLambda(), 0.0);
		assertEquals(4.5, p.getExpParam1().getLambda(), 0.0);
		assertEquals(1.0 / (0.5 * 4 + 0.5 * 1) * 1.0, p.getMean(), 1e-12);
		assertEquals(p.getMean(), new MMPP2Distr().theorMean(p), 0.0);
		assertEquals(p.getVar(), new MMPP2Distr().theorVariance(p), 0.0);
		assertEquals(0.0, new MMPP2Distr().pdf(1, p), 0.0);
		assertEquals(0.0, new MMPP2Distr().cdf(1, p), 0.0);
		MMPP2Distr d = new MMPP2Distr();
		double[] a = draw(d, p, SEED, N);
		for (double v : a) {
			assertTrue(v > 0);
		}
		assertEquals(p.getMean(), mean(a), p.getMean() * 0.05, "MMPP2 sample mean (5%: state-correlated samples)");
		assertArrayEquals(java.util.Arrays.copyOf(a, 500), draw(new MMPP2Distr(), p, SEED, 500));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MMPP2Par(0.0, 1.0, 1.0, 1.0));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MMPP2Par(1.0, 0.0, 1.0, 1.0));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MMPP2Par(1.0, 1.0, 0.0, 1.0));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MMPP2Par(1.0, 1.0, 1.0, -1.0));
		MMPP2Par boxed = new MMPP2Par(Double.valueOf(1.0), Double.valueOf(1.0), Double.valueOf(1.0), Double.valueOf(1.0));
		assertTrue(boxed.check());
	}

	// ---- MAP ----

	@Test
	void map2() throws Exception {
		Object[] d0 = { row(-3.0, 1.0), row(0.5, -2.0) };
		Object[] d1 = { row(1.0, 1.0), row(0.5, 1.0) };
		MAPPar p = new MAPPar(d0, d1);
		assertTrue(p.getMean() > 0);
		assertTrue(p.getVar() > 0);
		assertEquals(1.0, p.getPi()[0] + p.getPi()[1], 1e-9);
		assertEquals(3.0, p.getLambda()[0], 0.0);
		assertEquals(1.0 / 3.0, p.getP0()[0][1], 1e-12);
		assertEquals(1.0 / 3.0, p.getP1()[0][0], 1e-12);
		assertEquals(p.getMean(), new MAPDistr().theorMean(p), 0.0);
		assertEquals(p.getVar(), new MAPDistr().theorVariance(p), 0.0);
		assertEquals(0.0, new MAPDistr().pdf(1, p), 0.0);
		assertEquals(0.0, new MAPDistr().cdf(1, p), 0.0);
		double[] a = draw(new MAPDistr(), p, SEED, N);
		assertEquals(p.getMean(), mean(a), p.getMean() * 0.03);
		assertArrayEquals(java.util.Arrays.copyOf(a, 500), draw(new MAPDistr(), p, SEED, 500));
	}

	@Test
	void mapValidation() {
		Object[] d0 = { row(-3.0, 1.0), row(0.5, -2.0) };
		Object[] d1 = { row(1.0, 1.0), row(0.5, 1.0) };
		assertThrows(IncorrectDistributionParameterException.class, () -> new MAPPar(new Object[] { row(3.0, 1.0), row(0.5, -2.0) }, d1));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MAPPar(new Object[] { row(-3.0, -1.0), row(0.5, -2.0) }, d1));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MAPPar(new Object[] { row(-3.0, 1.0, 0.0), row(0.5, -2.0, 0.0) }, d1));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MAPPar(d0, new Object[] { row(1.0, -1.0), row(0.5, 1.0) }));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MAPPar(d0, new Object[] { row(1.0, 1.0) }));
		assertThrows(IncorrectDistributionParameterException.class, () -> new MAPPar(d0, new Object[] { row(1.0, 1.0), row(5.0, 1.0) }));
		assertThrows(IncorrectDistributionParameterException.class,
				() -> new MAPPar(d0, new Object[] { row(1.0, 1.0, 1.0), row(0.5, 1.0, 0.0) }));
	}

	// ---- Empirical ----

	@Test
	void empirical() throws Exception {
		EmpiricalPar p = new EmpiricalPar(new double[] { 0.1, 0.2, 0.3, 0.4 });
		Empirical e = new Empirical();
		assertTrue(p.check());
		assertFalse(new EmpiricalPar().check());
		assertEquals(0.2, e.pdf(1, p), 1e-12);
		assertEquals(0.0, e.pdf(-1, p), 0.0);
		assertEquals(0.0, e.pdf(9, p), 0.0);
		assertEquals(0.0, e.cdf(-1, p), 0.0);
		assertEquals(1.0, e.cdf(4, p), 0.0);
		assertEquals(2.0, e.theorMean(p), 1e-12);
		assertEquals(1.0, e.theorVariance(p), 1e-12);
		double[] a = draw(e, p, SEED, N);
		double[] counts = new double[4];
		for (double v : a) {
			counts[(int) v]++;
		}
		for (int i = 0; i < 4; i++) {
			assertEquals(p.getPDF()[i], counts[i] / N, 0.01);
		}
		assertEquals(2.0, mean(a), 0.06);
		assertArrayEquals(a, draw(new Empirical(), p, SEED, N), 0.0);
	}

	@Disabled("bug: Empirical.cdf(k) returns P(X < k) (cdf[k] is shifted by one bin) while pdf(k) and nextRand treat k as the k-th bin")
	@Test
	void empiricalCdfIncludesTheBin() throws Exception {
		EmpiricalPar p = new EmpiricalPar(new double[] { 0.1, 0.2, 0.3, 0.4 });
		assertEquals(0.3, new Empirical().cdf(1, p), 1e-12);
		assertEquals(1.0, new Empirical().cdf(3, p), 1e-12);
	}

	@Test
	void empiricalBoxedAndEntryConstructors() throws Exception {
		EmpiricalPar boxed = new EmpiricalPar(new Double[] { 0.5, 0.5 });
		assertEquals(0.5, boxed.getPDF()[1], 0.0);
		EmpiricalPar byEntries = new EmpiricalPar(new EmpiricalEntry[] { new EmpiricalEntry("a", "0.25"),
				new EmpiricalEntry("b", 0.25), new EmpiricalEntry("c", Double.valueOf(0.5)) });
		assertEquals("c", byEntries.getValue(2));
		assertEquals(3, byEntries.getValues().length);
		EmpiricalEntry en = new EmpiricalEntry("x", 0.1);
		en.setValue("y");
		en.setProbability(0.2);
		assertEquals("y", en.getValue());
		assertEquals(0.2, en.getProbability(), 0.0);
		assertTrue(EmpiricalPar.guiCheck("pdf", 0.5));
		assertFalse(EmpiricalPar.guiCheck("pdf", 1.5));
		assertFalse(EmpiricalPar.guiCheck("other", 0.5));
		assertTrue(EmpiricalPar.guiGetErrorMsg("pdf").contains("[0, 1]"));
		assertEquals("", EmpiricalPar.guiGetErrorMsg("other"));
	}

	@Test
	void empiricalValidation() throws Exception {
		assertThrows(IncorrectDistributionParameterException.class, () -> new EmpiricalPar(new double[] { -0.5, 1.5 }));
		assertThrows(IncorrectDistributionParameterException.class, () -> new EmpiricalPar(new double[] { 0.3, 0.3 }));
		Empirical e = new Empirical();
		e.setRandomEngine(new MersenneTwister(1));
		EmpiricalPar unset = new EmpiricalPar();
		assertThrows(IncorrectDistributionParameterException.class, () -> e.pdf(0, unset));
		assertThrows(IncorrectDistributionParameterException.class, () -> e.cdf(0, unset));
		assertThrows(IncorrectDistributionParameterException.class, () -> e.theorMean(unset));
		assertThrows(IncorrectDistributionParameterException.class, () -> e.theorVariance(unset));
		assertThrows(IncorrectDistributionParameterException.class, () -> e.nextRand(unset));
	}

	// ---- Batch ----

	@Test
	void batchWithFixedSizeEmitsOneValueEveryNthCall() throws Exception {
		DistributionContainer base = new DistributionContainer(new DeterministicDistr(), new DeterministicDistrPar(6.0));
		Batch b = new Batch(base, 3, true);
		b.setRandomEngine(new MersenneTwister(1));
		BatchPar bp = new BatchPar(base, null, 3, true);
		assertTrue(bp.check());
		double[] out = new double[6];
		for (int i = 0; i < 6; i++) {
			out[i] = b.nextRand(bp);
		}
		assertArrayEquals(new double[] { 0, 0, 6, 0, 0, 6 }, out, 0.0);
		assertEquals(2.0, b.theorMean(bp), 1e-12);
		assertEquals(0.0, b.theorVariance(bp), 0.0);
		assertEquals(0.0, b.pdf(1, bp), 0.0);
		assertEquals(0.0, b.cdf(1, bp), 0.0);
		assertEquals(3, bp.getFixedBatchSize());
		assertTrue(bp.getIsFixedBatchSize());
	}

	@Test
	void batchWithRandomSize() throws Exception {
		DistributionContainer base = new DistributionContainer(new DeterministicDistr(), new DeterministicDistrPar(6.0));
		Uniform sizeDistr = new Uniform(2, 4);
		sizeDistr.setRandomEngine(new MersenneTwister(5));
		DistributionContainer size = new DistributionContainer(sizeDistr, new UniformPar(2, 4));
		Batch b = new Batch(base, size, null, false);
		BatchPar bp = new BatchPar();
		int emitted = 0;
		for (int i = 0; i < 3000; i++) {
			if (b.nextRand(bp) > 0) {
				emitted++;
			}
		}
		assertEquals(3000.0 / 3.0, emitted, 60.0);
	}

	@Disabled("bug: BatchPar's constructor calls getDistribution() on the batch-size container, so a discrete batch size throws ClassCastException")
	@Test
	void batchParAcceptsDiscreteBatchSize() throws Exception {
		DistributionContainer base = new DistributionContainer(new DeterministicDistr(), new DeterministicDistrPar(6.0));
		DistributionContainer size = new DistributionContainer(new Uniform(2, 4), new UniformPar(2, 4));
		assertTrue(new BatchPar(base, size, null, false).check());
	}

	@Test
	void batchRejectsNonPositiveFixedSize() throws Exception {
		DistributionContainer base = new DistributionContainer(new DeterministicDistr(), new DeterministicDistrPar(6.0));
		Batch b = new Batch(base, 0, true);
		assertThrows(IncorrectDistributionParameterException.class, () -> b.nextRand(new BatchPar()));
		BatchPar bp = new BatchPar();
		bp.setIsFixedBatchSize(true);
		bp.setFixedBatchSize(0);
		assertFalse(bp.check());
		bp.setBase(new DeterministicDistr());
		bp.setBaseParameter(new DeterministicDistrPar(1.0));
		bp.setBatchSize(new Exponential());
		bp.setBatchSizeParameter(new ExponentialPar(1.0));
		assertTrue(bp.getBase() instanceof DeterministicDistr);
		assertEquals(1.0, ((DeterministicDistrPar) bp.getBaseParameter()).getParameterValue(), 0.0);
		assertTrue(bp.getBatchSize() instanceof Exponential);
		assertEquals(1.0, ((ExponentialPar) bp.getBatchSizeParameter()).getLambda(), 0.0);
	}

	// ---- DistributionContainer ----

	@Test
	void distributionContainerChecksTheStoredKind() throws Exception {
		Parameter p = new ExponentialPar(1.0);
		DistributionContainer c = new DistributionContainer(new Exponential(), p);
		assertTrue(c.getDistribution() instanceof Exponential);
		assertSame(p, c.getParameter());
		assertThrows(ClassCastException.class, c::getDiscreteDistribution);
		DistributionContainer d = new DistributionContainer(new Uniform(1, 2), new UniformPar(1, 2));
		assertNotNull(d.getDiscreteDistribution());
		assertThrows(ClassCastException.class, d::getDistribution);
	}

	// ---- Replayer ----

	@Test
	void replayerReadsTraceAndWrapsAround(@TempDir File dir) throws Exception {
		File f = new File(dir, "trace.txt");
		Files.write(f.toPath(), "1.5 2.5\n0\nabc 4\n".getBytes(StandardCharsets.UTF_8));
		ReplayerPar p = new ReplayerPar(f.getAbsolutePath());
		Replayer r = new Replayer();
		double[] got = new double[8];
		for (int i = 0; i < got.length; i++) {
			got[i] = r.nextRand(p);
		}
		// zeros become 1e-6, malformed tokens are skipped, the file is replayed from the start when exhausted
		assertArrayEquals(new double[] { 1.5, 2.5, 1e-6, 4.0, 1.5, 2.5, 1e-6, 4.0 }, got, 0.0);
		assertEquals((1.5 + 2.5 + 1e-6 + 4.0) / 4.0, r.theorMean(p), 1e-9);
		assertThrows(IncorrectDistributionParameterException.class, () -> r.pdf(1, p));
		assertThrows(IncorrectDistributionParameterException.class, () -> r.cdf(1, p));
		assertThrows(IncorrectDistributionParameterException.class, () -> r.theorVariance(p));
		assertThrows(IncorrectDistributionParameterException.class, () -> r.nextRand(new ExponentialPar(1.0)));
	}

	@Test
	void replayerFallsBackToDefaultValueForMissingOrEmptyFile(@TempDir File dir) throws Exception {
		ReplayerPar missing = new ReplayerPar(new File(dir, "nope.txt").getAbsolutePath());
		assertEquals(1e-6, missing.getNext(), 0.0);
		File empty = new File(dir, "empty.txt");
		Files.write(empty.toPath(), new byte[0]);
		assertEquals(1e-6, new ReplayerPar(empty.getAbsolutePath()).getNext(), 0.0);
		File junk = new File(dir, "junk.txt");
		Files.write(junk.toPath(), "x y z\n".getBytes(StandardCharsets.UTF_8));
		assertEquals(1e-6, new ReplayerPar(junk.getAbsolutePath()).getNext(), 0.0);
	}
}
