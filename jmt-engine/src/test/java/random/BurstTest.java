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

import org.junit.jupiter.api.Test;

/** Only the parts of Burst that do not need a running simulation (it is a NetNode scheduled by the simulator). */
class BurstTest {

	private static DistributionContainer det(double v) throws Exception {
		return new DistributionContainer(new DeterministicDistr(), new DeterministicDistrPar(v));
	}

	@Test
	void probabilityOutsideUnitIntervalIsRejected() {
		assertThrows(IncorrectDistributionParameterException.class, () -> new Burst(1.5, null, null));
		assertThrows(IncorrectDistributionParameterException.class, () -> new Burst(-0.1, null, null));
	}

	@Test
	void checkAcceptsTheClosedUnitInterval() throws Exception {
		Burst b = new Burst(0.5, det(1.0), det(2.0));
		assertTrue(b.check(0.0));
		assertTrue(b.check(1.0));
		assertFalse(b.check(1.01));
		assertFalse(b.check(-0.01));
	}

	@Test
	void theorMeanWeighsValueMeansByExpectedIntervalLength() throws Exception {
		BurstPar bp = new BurstPar(det(10.0), det(30.0));
		assertSame(bp.getValueDistributionA(), bp.getValueDistributionA());
		assertEquals(10.0, ((DeterministicDistrPar) bp.getValueParameterA()).getParameterValue(), 0.0);
		// probabilistic selection, p(A)=0.25: (0.25*1*10 + 0.75*3*30) / (0.25*1 + 0.75*3)
		Burst random = new Burst(0.25, det(1.0), det(3.0));
		assertEquals((0.25 * 10 + 0.75 * 3 * 30) / (0.25 + 0.75 * 3), random.theorMean(bp), 1e-12);
		// round robin always uses 1/2 each
		Burst rr = new Burst(0.25, det(1.0), det(3.0), true);
		assertEquals((0.5 * 10 + 0.5 * 3 * 30) / (0.5 + 0.5 * 3), rr.theorMean(bp), 1e-12);
		assertEquals(0.0, rr.theorVariance(bp), 0.0);
		assertEquals(0.0, rr.pdf(1, bp), 0.0);
		assertEquals(0.0, rr.cdf(1, bp), 0.0);
	}

	@Test
	void burstParSettersReplaceTheComponents() throws Exception {
		BurstPar bp = new BurstPar(det(1.0), det(2.0));
		Distribution d = new Exponential();
		Parameter p = new ExponentialPar(3.0);
		bp.setValueDistributionA(d);
		bp.setValueParameterA(p);
		bp.setValueDistributionB(d);
		bp.setValueParameterB(p);
		assertSame(d, bp.getValueDistributionA());
		assertSame(p, bp.getValueParameterA());
		assertSame(d, bp.getValueDistributionB());
		assertSame(p, bp.getValueParameterB());
	}
}
