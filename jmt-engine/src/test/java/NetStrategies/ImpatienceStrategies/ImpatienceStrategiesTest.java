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

package NetStrategies.ImpatienceStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.BooleanValueImpatienceMeasurement;
import NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.DoubleValueImpatienceMeasurement;
import NetStrategies.ServiceStrategies.LDParameter;
import NetStrategies.ServiceStrategies.LoadDependentStrategy;
import NetStrategies.ServiceStrategy;
import QueueNet.JobInfo;
import QueueNet.JobInfoList;
import QueueNet.StrategyFixtures;
import QueueNet.StrategyFixtures.ScriptedService;
import random.DeterministicDistr;
import random.DeterministicDistrPar;

class ImpatienceStrategiesTest {

	private StrategyFixtures f;

	@BeforeEach
	void setUp() throws Exception {
		// class 0 has priority 1, class 1 has priority 5
		f = new StrategyFixtures(StrategyFixtures.classes(1, 5), new ServiceStrategy[] { new ScriptedService(1), new ScriptedService(1) });
	}

	private JobInfoList list(int... classes) {
		JobInfoList q = f.queue();
		for (int c : classes) {
			q.addLast(new JobInfo(f.job(c)));
		}
		return q;
	}

	private LoadDependentStrategy probabilities(String... fns) throws Exception {
		LDParameter[] ps = new LDParameter[fns.length];
		for (int i = 0; i < fns.length; i++) {
			ps[i] = new LDParameter(i * 2, new DeterministicDistr(), new DeterministicDistrPar(1.0), fns[i]);
		}
		return new LoadDependentStrategy(ps);
	}

	@Test
	void impatienceTypeParsesNamesLeniently() {
		assertEquals(ImpatienceType.BALKING, ImpatienceType.getType("balking"));
		assertEquals(ImpatienceType.RENEGING, ImpatienceType.getType("Reneging"));
		assertEquals(ImpatienceType.NONE, ImpatienceType.getType("none"));
		assertEquals(ImpatienceType.NONE, ImpatienceType.getType("garbage"));
		assertEquals("Balking", ImpatienceType.BALKING.getDisplayName());
	}

	@Test
	void measurementCountsAllJobsWithoutPriority() {
		BooleanValueImpatienceMeasurement m = new BooleanValueImpatienceMeasurement(list(0, 1), list(0), 5, false);
		assertEquals(3, m.getQueueLength());
		assertFalse(m.getBooleanValue());
		m.setBooleanValue(true);
		assertTrue(m.getBooleanValue());
	}

	@Test
	void measurementCountsOnlyJobsOfEqualOrHigherPriorityWithPriority() {
		assertEquals(2, new BooleanValueImpatienceMeasurement(list(0, 1), list(1), 5, true).getQueueLength());
		assertEquals(3, new BooleanValueImpatienceMeasurement(list(0, 1), list(1), 1, true).getQueueLength());
		assertEquals(0, new BooleanValueImpatienceMeasurement(list(), list(), 1, true).getQueueLength());
	}

	@Test
	void balkingNeverBalksOnEmptyQueue() throws Exception {
		Balking b = new Balking(probabilities("1.0"), false, 42L);
		BooleanValueImpatienceMeasurement m = new BooleanValueImpatienceMeasurement(list(), list(), 1, false);
		m.setBooleanValue(true);
		b.generateImpatience(m);
		assertFalse(m.getBooleanValue());
	}

	@Test
	void balkingUsesProbabilityOfTheRangeContainingTheQueueLength() throws Exception {
		// range [0,2) -> 0.0, range [2,4) -> 1.0
		Balking b = new Balking(probabilities("0.0", "1.0"), true, 42L);
		assertTrue(b.isPriorityActivated());
		BooleanValueImpatienceMeasurement low = new BooleanValueImpatienceMeasurement(list(0), list(), 1, false);
		b.generateImpatience(low);
		assertFalse(low.getBooleanValue());
		BooleanValueImpatienceMeasurement high = new BooleanValueImpatienceMeasurement(list(0, 0, 0), list(), 1, false);
		b.generateImpatience(high);
		assertTrue(high.getBooleanValue());
	}

	@Test
	void balkingSamplesAtIntermediateProbability() throws Exception {
		Balking b = new Balking(probabilities("0.5"), false, 42L);
		int balked = 0;
		for (int i = 0; i < 1000; i++) {
			BooleanValueImpatienceMeasurement m = new BooleanValueImpatienceMeasurement(list(0), list(), 1, false);
			b.generateImpatience(m);
			if (m.getBooleanValue()) {
				balked++;
			}
		}
		assertTrue(balked > 400 && balked < 600);
	}

	@Test
	void balkingTypeAndArgumentChecks() throws Exception {
		Balking b = new Balking(probabilities("0.5"), false);
		assertEquals(ImpatienceType.BALKING, b.impatienceType());
		assertTrue(b.isImpatienceType(ImpatienceType.BALKING));
		assertFalse(b.isImpatienceType(ImpatienceType.RENEGING));
		assertThrows(IllegalArgumentException.class, () -> b.generateImpatience(new DoubleValueImpatienceMeasurement()));
		assertNull(new Balking(null, false, 1L).impatienceType());
	}

	@Test
	void renegingDrawsDelayFromDistributionAndClampsNegativeValues() throws Exception {
		Reneging r = new Reneging(new DeterministicDistr(), new DeterministicDistrPar(4.0));
		assertEquals(ImpatienceType.RENEGING, r.impatienceType());
		assertTrue(r.isImpatienceType(ImpatienceType.RENEGING));
		DoubleValueImpatienceMeasurement m = new DoubleValueImpatienceMeasurement();
		r.generateImpatience(m);
		assertEquals(4.0, m.doubleValue(), 0.0);

		DeterministicDistr negativeDistr = new DeterministicDistr() {
			@Override
			public double nextRand(random.Parameter p) {
				return -2.0;
			}
		};
		Reneging negative = new Reneging(negativeDistr, new DeterministicDistrPar(1.0));
		negative.generateImpatience(m);
		assertEquals(0.0, m.doubleValue(), 0.0);
	}

	@Test
	void renegingTypeAndArgumentChecks() throws Exception {
		Reneging r = new Reneging(new DeterministicDistr(), new DeterministicDistrPar(1.0));
		assertThrows(IllegalArgumentException.class,
				() -> r.generateImpatience(new BooleanValueImpatienceMeasurement(list(), list(), 1, false)));
		assertNull(new Reneging(null, null).impatienceType());
	}
}
