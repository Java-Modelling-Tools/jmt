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

package NetStrategies.PSStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import NetStrategies.PSStrategy;
import QueueNet.JobClassList;
import QueueNet.JobInfo;
import QueueNet.JobInfoList;
import QueueNet.StrategyFixtures;
import QueueNet.StrategyFixtures.ScriptedService;
import NetStrategies.ServiceStrategy;

/** Slices for classes 0 (priority 1, weight 1) and 1 (priority 2, weight 3). */
class PSStrategiesTest {

	private static final double EPS = 1e-12;

	private StrategyFixtures f;
	private JobClassList classes;
	private final double[] weights = { 1, 3 };
	private final boolean[] none = { false, false };
	private final Boolean[] all = { true, true };
	private final double[] ones = { 1, 1 };

	@BeforeEach
	void setUp() throws Exception {
		f = new StrategyFixtures(StrategyFixtures.classes(1, 2), new ServiceStrategy[] { new ScriptedService(1), new ScriptedService(1) });
		classes = f.network.getJobClasses();
	}

	private JobInfoList list(int n0, int n1) {
		JobInfoList q = f.queue();
		for (int i = 0; i < n0; i++) {
			q.addLast(new JobInfo(f.job(0)));
		}
		for (int i = 0; i < n1; i++) {
			q.addLast(new JobInfo(f.job(1)));
		}
		return q;
	}

	private double slice(PSStrategy s, JobInfoList q, int cls) {
		return s.slice(q, classes, weights, none, classes.get(cls));
	}

	@Test
	void epsSharesEquallyAmongAllJobs() {
		JobInfoList q = list(2, 1);
		assertEquals(1.0 / 3, slice(new EPSStrategy(), q, 0), EPS);
		assertEquals(1.0 / 3, slice(new EPSStrategy(), q, 1), EPS);
		assertEquals(0.0, slice(new EPSStrategy(), list(0, 0), 0), EPS);
		assertEquals(0.5, new EPSStrategy().slice(q, classes, weights, none, classes.get(0), all, new double[] { 0.5, 1 }), EPS);
		assertEquals(0.0, new EPSStrategy().slice(q, classes, weights, none, classes.get(0), new Boolean[] { false, true }, ones), EPS);
	}

	@Test
	void qbpsIgnoresSaturatedClasses() {
		JobInfoList q = list(2, 1);
		assertEquals(1.0 / 3, slice(new QBPSStrategy(), q, 0), EPS);
		assertEquals(0.5, new QBPSStrategy().slice(q, classes, weights, new boolean[] { false, true }, classes.get(0), all, ones), EPS);
		assertEquals(0.0, slice(new QBPSStrategy(), list(0, 0), 1), EPS);
		assertEquals(0.0, new QBPSStrategy().slice(q, classes, weights, none, classes.get(1), new Boolean[] { true, false }, ones), EPS);
	}

	@Test
	void gpsSharesByClassWeightsOverPresentClasses() {
		JobInfoList q = list(2, 1);
		assertEquals(0.125, slice(new GPSStrategy(), q, 0), EPS);
		assertEquals(0.75, slice(new GPSStrategy(), q, 1), EPS);
		// only one class present: it gets everything
		assertEquals(0.5, slice(new GPSStrategy(), list(2, 0), 0), EPS);
		assertEquals(0.0, slice(new GPSStrategy(), list(2, 0), 1), EPS);
		// saturated class 1 does not take part in the share
		assertEquals(0.5, new GPSStrategy().slice(q, classes, weights, new boolean[] { false, true }, classes.get(0), all, ones), EPS);
		// zero split excludes the class
		assertEquals(0.5, new GPSStrategy().slice(q, classes, weights, none, classes.get(0), all, new double[] { 1, 0 }), EPS);
		assertEquals(0.0, new GPSStrategy().slice(q, classes, weights, none, classes.get(0), new Boolean[] { false, true }, ones), EPS);
	}

	@Test
	void dpsSharesByWeightTimesJobCount() {
		JobInfoList q = list(2, 1);
		assertEquals(0.2, slice(new DPSStrategy(), q, 0), EPS);
		assertEquals(0.6, slice(new DPSStrategy(), q, 1), EPS);
		assertEquals(0.0, slice(new DPSStrategy(), list(0, 1), 0), EPS);
		assertEquals(0.0, new DPSStrategy().slice(q, classes, weights, none, classes.get(0), new Boolean[] { false, true }, ones), EPS);
		assertEquals(0.5, new DPSStrategy().slice(q, classes, weights, new boolean[] { false, true }, classes.get(0), all, ones), EPS);
	}

	@Test
	void priorityVariantsServeOnlyTheHighestPriorityPresent() {
		JobInfoList q = list(2, 1);
		for (PSStrategy s : new PSStrategy[] { new EPSStrategyPriority(), new QBPSStrategyPriority(), new GPSStrategyPriority(),
				new DPSStrategyPriority() }) {
			assertEquals(0.0, slice(s, q, 0), EPS);
			assertEquals(1.0, slice(s, q, 1), EPS);
			assertEquals(0.0, slice(s, list(0, 0), 0), EPS);
			assertEquals(0.0, s.slice(q, classes, weights, none, classes.get(1), new Boolean[] { true, false }, ones), EPS);
		}
	}

	@Test
	void priorityVariantsShareWithinTheServedPriority() {
		JobInfoList q = list(2, 0);
		assertEquals(0.5, slice(new EPSStrategyPriority(), q, 0), EPS);
		assertEquals(0.5, slice(new QBPSStrategyPriority(), q, 0), EPS);
		assertEquals(0.5, slice(new GPSStrategyPriority(), q, 0), EPS);
		assertEquals(0.5, slice(new DPSStrategyPriority(), q, 0), EPS);
		// several jobs at the top priority (2 jobs of class 1, 1 low-priority job of class 0)
		JobInfoList top = list(1, 2);
		assertEquals(0.5, slice(new EPSStrategyPriority(), top, 1), EPS);
		assertEquals(0.5, slice(new QBPSStrategyPriority(), top, 1), EPS);
		assertEquals(0.5, slice(new GPSStrategyPriority(), top, 1), EPS);
		assertEquals(0.5, slice(new DPSStrategyPriority(), top, 1), EPS);
		assertEquals(0.0, slice(new GPSStrategyPriority(), top, 0), EPS);
	}

	@Test
	void checkIsAlwaysTrue() {
		assertEquals(true, new EPSStrategy().check());
	}
}
