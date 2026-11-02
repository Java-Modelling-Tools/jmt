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

package QueueNet;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import NetStrategies.QueueGetStrategies.FCFSstrategy;
import NetStrategies.QueuePutStrategies.TailStrategy;
import NetStrategies.QueuePutStrategy;
import NetStrategies.ServiceStrategy;
import NetStrategies.TransitionUtilities.TransitionMatrix;
import NetStrategies.TransitionUtilities.TransitionVector;
import NodeSections.Enabling;
import NodeSections.Firing;
import NodeSections.Linkage;
import NodeSections.ServiceTunnel;
import NodeSections.Storage;
import NodeSections.Timing;
import jmt.common.ErrorReporters;

/** A place -> transition -> place net wired by hand: the transition consumes 2 tokens and produces 2. */
class PetriStationsTest {
	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private static Storage place(int capacity) {
		return new Storage(capacity, new Integer[] { capacity }, new String[] { Storage.FINITE_DROP }, new FCFSstrategy(),
				new QueuePutStrategy[] { new TailStrategy() });
	}

	private static TransitionMatrix matrix(String station, int tokens) {
		return new TransitionMatrix(new TransitionVector[] { new TransitionVector(station, new Integer[] { tokens }) });
	}

	private NetFixtures.Mini m;
	private NetNode p1;
	private NetNode p2;

	private void wire(int consume, int inhibit, int produce, double firingTime) throws Exception {
		m = new NetFixtures.Mini(0);
		p1 = m.station("p1", place(-1), new ServiceTunnel(), new Linkage());
		p2 = m.station("p2", place(-1), new ServiceTunnel(), new Linkage());
		Enabling enabling = new Enabling(new TransitionMatrix[] { matrix("p1", consume) }, new TransitionMatrix[] { matrix("p1", inhibit) });
		Timing timing = new Timing(new String[] { "mode" }, new Integer[] { 1 },
				new ServiceStrategy[] { new StrategyFixtures.ScriptedService(firingTime) }, new Integer[] { -1 }, new Double[] { 1.0 });
		Firing firing = new Firing(new TransitionMatrix[] { matrix("p2", produce) });
		NetNode t = m.station("t", enabling, timing, firing);
		m.connect(m.feeder, p1);
		m.connect(p1, t);
		m.connect(t, p2);
	}

	@Test
	void transitionMovesTokensWhenEnabled() throws Exception {
		wire(2, 0, 2, 3.0);
		m.inject(p1, 0, 0.0);
		m.inject(p1, 0, 0.5);
		m.run(10000);
		assertEquals(0, p1.getJobInfoList().size());
		assertEquals(2, p2.getJobInfoList().size());
	}

	@Test
	void transitionStaysDisabledWithTooFewTokens() throws Exception {
		wire(2, 0, 2, 3.0);
		m.inject(p1, 0, 0.0);
		m.run(10000);
		assertEquals(1, p1.getJobInfoList().size());
		assertEquals(0, p2.getJobInfoList().size());
	}

	@Test
	void inhibitorArcBlocksTheTransition() throws Exception {
		wire(1, 1, 1, 1.0);
		m.inject(p1, 0, 0.0);
		m.run(10000);
		assertEquals(1, p1.getJobInfoList().size());
		assertEquals(0, p2.getJobInfoList().size());
	}

	private NetNode lonelyPlace(Storage place) throws Exception {
		m = new NetFixtures.Mini(0);
		NetNode p = m.station("p", place, new ServiceTunnel(), new Linkage());
		m.connect(m.feeder, p);
		return p;
	}

	@Test
	void fullPlaceDropsExtraTokens() throws Exception {
		NetNode p = lonelyPlace(new Storage(2, new Integer[] { 2 }, new String[] { Storage.FINITE_DROP }, new FCFSstrategy(),
				new QueuePutStrategy[] { new TailStrategy() }));
		for (int i = 0; i < 4; i++) {
			m.inject(p, 0, 0.1 * i);
		}
		m.run(1000);
		assertEquals(2, p.getJobInfoList().size());
	}

	@Test
	void placeWithWaitingPolicyKeepsEveryToken() throws Exception {
		NetNode p = lonelyPlace(new Storage(2, new Integer[] { 2 }, new String[] { Storage.FINITE_WAITING }, new FCFSstrategy(),
				new QueuePutStrategy[] { new TailStrategy() }));
		for (int i = 0; i < 4; i++) {
			m.inject(p, 0, 0.1 * i);
		}
		m.run(1000);
		assertTrue(p.getJobInfoList().size() >= 2);
	}

	@Test
	void placeWithBlockingPolicyAcceptsAndHoldsTokens() throws Exception {
		NetNode p = lonelyPlace(new Storage(2, new Integer[] { 2 }, new String[] { Storage.FINITE_BLOCK }, new FCFSstrategy(),
				new QueuePutStrategy[] { new TailStrategy() }));
		for (int i = 0; i < 3; i++) {
			m.inject(p, 0, 0.1 * i);
		}
		m.run(1000);
		assertTrue(p.getJobInfoList().size() >= 2);
	}

	@Test
	void preloadedTokensAreResidentInThePlace() throws Exception {
		Storage place = place(-1);
		NetNode p = lonelyPlace(place);
		m.ready();
		place.preloadJobs(new int[] { 3 });
		m.run(1000);
		assertEquals(3, p.getJobInfoList().size());
	}
}
