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

package NetStrategies.VacationStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jmt.common.exception.NetException;
import random.DeterministicDistr;
import random.DeterministicDistrPar;
import random.Exponential;
import random.ExponentialPar;

class VacationStrategiesTest {

	@Test
	void noVacationHasZeroLength() {
		assertEquals(0.0, new NoVacationStrategy().nextVacationLength(), 0.0);
	}

	@Test
	void vacationWhenIdleDrawsLengthAndKeepsType() throws Exception {
		VacationWhenIdleStrategy s = new VacationWhenIdleStrategy(VacationWhenIdleStrategy.SINGLE_VACATION, new DeterministicDistr(),
				new DeterministicDistrPar(3.0));
		assertEquals(3.0, s.nextVacationLength(), 0.0);
		assertEquals(VacationWhenIdleStrategy.SINGLE_VACATION, s.getType());
	}

	@Test
	void vacationWhenIdleWrapsInvalidParameters() throws Exception {
		Exponential exp = new Exponential();
		exp.setRandomEngine(new random.engine.MersenneTwister(1L));
		VacationWhenIdleStrategy s = new VacationWhenIdleStrategy(VacationWhenIdleStrategy.MULTIPLE_VACATION, exp, new ExponentialPar());
		NetException e = assertThrows(NetException.class, s::nextVacationLength);
		assertTrue(e.getMessage().contains("VacationWhenIdleStrategy"));
	}

	@Test
	void bernoulliVacationValidatesProbabilityAndDrawsLength() throws Exception {
		assertThrows(NetException.class, () -> new BernoulliVacationStrategy(-0.1, new DeterministicDistr(), new DeterministicDistrPar(1.0)));
		assertThrows(NetException.class, () -> new BernoulliVacationStrategy(1.1, new DeterministicDistr(), new DeterministicDistrPar(1.0)));
		BernoulliVacationStrategy always = new BernoulliVacationStrategy(1.0, new DeterministicDistr(), new DeterministicDistrPar(2.0));
		BernoulliVacationStrategy never = new BernoulliVacationStrategy(0.0, new DeterministicDistr(), new DeterministicDistrPar(2.0));
		assertEquals(2.0, always.nextVacationLength(), 0.0);
		assertEquals(VacationWhenIdleStrategy.MULTIPLE_VACATION, always.getType());
		for (int i = 0; i < 50; i++) {
			assertTrue(always.nextRandBool());
			assertEquals(false, never.nextRandBool());
		}
	}

	@Test
	void queueIdleAckBodyDescribesItself() {
		assertTrue(new QueueIdleAckBody(true).isServerStartsVacation());
		assertEquals("{serverStartsVacation: false}", new QueueIdleAckBody(false).toString());
	}
}
