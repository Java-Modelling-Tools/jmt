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
package random.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class MersenneTwisterTest {

	private static double[] doubles(RandomEngine e, int n) {
		double[] a = new double[n];
		for (int i = 0; i < n; i++) {
			a[i] = e.nextDouble();
		}
		return a;
	}

	@Test
	void sameSeedSameSequenceAcrossAllGenerators() {
		MersenneTwister a = new MersenneTwister(42);
		MersenneTwister b = new MersenneTwister(42);
		for (int i = 0; i < 2000; i++) { // crosses the 624-word regeneration boundary
			assertEquals(a.nextInt(), b.nextInt());
			assertEquals(a.nextLong(), b.nextLong());
			assertEquals(a.nextLong64(), b.nextLong64());
			assertEquals(a.nextDouble(), b.nextDouble(), 0.0);
			assertEquals(a.raw(), b.raw(), 0.0);
			assertEquals(a.raw2(), b.raw2(), 0.0);
		}
	}

	@Test
	void differentSeedsDiffer() {
		assertFalse(Arrays.equals(doubles(new MersenneTwister(1), 50), doubles(new MersenneTwister(2), 50)));
	}

	@Test
	void setNewSeedRestartsTheSequence() {
		MersenneTwister e = new MersenneTwister(7);
		double[] first = doubles(e, 700);
		e.setNewSeed(7);
		assertArrayEquals(first, doubles(e, 700), 0.0);
		assertArrayEquals(first, doubles(new MersenneTwister(7), 700), 0.0);
	}

	@Test
	void firstOutputsMatchReferenceMt19937() {
		// reference outputs of genrand_int32 for init_genrand(5489), the standard MT19937 default seed
		MersenneTwister e = new MersenneTwister(5489);
		assertEquals(3499211612L, e.nextInt() & 0xffffffffL);
		assertEquals(581869302L, e.nextInt() & 0xffffffffL);
		assertEquals(3890346734L, e.nextInt() & 0xffffffffL);
	}

	@Test
	void defaultConstructorAndFactoryProduceUsableEngines() {
		RandomEngine e = RandomEngine.makeDefault();
		assertTrue(e instanceof MersenneTwister);
		double d = e.nextDouble();
		assertTrue(d >= 0.0 && d < 1.0);
		assertNotNull(new MersenneTwister());
	}

	@Test
	void doublesAreInRangeWithExpectedMean() {
		MersenneTwister e = new MersenneTwister(99);
		double sumRaw = 0, sumDouble = 0;
		int n = 100000;
		for (int i = 0; i < n; i++) {
			double r = e.raw();
			assertTrue(r > 0.0 && r < 1.0, "raw() is in the open interval");
			sumRaw += r;
			double d = e.nextDouble();
			assertTrue(d >= 0.0 && d < 1.0);
			sumDouble += d;
		}
		assertEquals(0.5, sumRaw / n, 0.015);
		assertEquals(0.5, sumDouble / n, 0.015);
	}

	@Test
	void integerGeneratorsCoverBothSigns() {
		MersenneTwister e = new MersenneTwister(3);
		boolean neg = false, pos = false, bigLong = false;
		for (int i = 0; i < 500; i++) {
			int v = e.nextInt();
			neg |= v < 0;
			pos |= v > 0;
			long l = e.nextLong();
			assertTrue(l >= 0 && l <= 0xffffffffL);
			bigLong |= l > Integer.MAX_VALUE;
			e.nextLong64();
		}
		assertTrue(neg && pos && bigLong);
	}

	@Test
	void builtInDemoPrintsSamplesAndRestoresNothingGlobal() {
		java.io.PrintStream original = System.out;
		java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
		try {
			System.setOut(new java.io.PrintStream(buf, true));
			MersenneTwister.test();
		} finally {
			System.setOut(original);
		}
		assertTrue(buf.size() > 0);
		assertSame(original, System.out);
	}
}
