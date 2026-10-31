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

import NetStrategies.CacheStrategy;
import NetStrategies.CacheStrategies.FIFOCache;
import NetStrategies.CacheStrategies.LRUCache;
import NetStrategies.QueueGetStrategies.FCFSstrategy;
import NetStrategies.QueuePutStrategies.TailStrategy;
import NetStrategies.QueuePutStrategy;
import NetStrategies.RoutingStrategies.RandomStrategy;
import NetStrategies.RoutingStrategy;
import NodeSections.Cache;
import NodeSections.Queue;
import NodeSections.Router;
import jmt.common.ErrorReporters;
import random.discrete.DiscreteDistribution;
import random.discrete.Uniform;

/** A cache station: class 0 requests become class 1 on a hit and class 2 on a miss. */
class CacheStationTest {
	@AfterEach
	void restore() {
		ErrorReporters.set(null);
	}

	private NetFixtures.Mini m;
	private RecordingSink out;
	private NetNode st;

	private void wire(CacheStrategy policy, int items, int capacity, int firstItem, int lastItem) throws Exception {
		m = new NetFixtures.Mini(0, 0, 0);
		JobClass[] c = m.classes;
		c[0].setCachePairClass(c[0]);
		c[1].setCachePairClass(c[0]);
		c[2].setCachePairClass(c[0]);
		DiscreteDistribution[] pop = new DiscreteDistribution[] { new Uniform(firstItem, lastItem), null, null };
		Cache cache = new Cache(items, new Integer[] { capacity }, new Object[] { new Float[] { 1f } },
				new JobClass[] { c[0] }, new JobClass[] { c[1] }, new JobClass[] { c[2] }, policy, pop);
		String[] drops = { Queue.FINITE_DROP, Queue.FINITE_DROP, Queue.FINITE_DROP };
		QueuePutStrategy[] put = { new TailStrategy(), new TailStrategy(), new TailStrategy() };
		st = m.station("cache", new Queue(-1, drops, new FCFSstrategy(), put), cache,
				new Router(new RoutingStrategy[] { new RandomStrategy(), new RandomStrategy(), new RandomStrategy() }));
		out = new RecordingSink();
		NetNode sink = m.station("sink", out, null, null);
		m.connect(m.feeder, st);
		m.connect(st, sink);
	}

	@Test
	void eachItemMissesOnceWhenTheCacheHoldsAllOfThem() throws Exception {
		wire(new FIFOCache(), 2, 2, 1, 2);
		for (int i = 0; i < 12; i++) {
			m.inject(st, 0, i);
		}
		m.run(10000);
		assertEquals(2, out.classIds.get(0).intValue(), "the very first request is a miss");
		assertEquals(2, java.util.Collections.frequency(out.classIds, 2), "one miss per distinct item");
		assertEquals(10, java.util.Collections.frequency(out.classIds, 1));
	}

	@Test
	void smallCacheServesSomeHitsAndSomeMisses() throws Exception {
		wire(new LRUCache(), 3, 1, 1, 3);
		
		for (int i = 0; i < 30; i++) {
			m.inject(st, 0, i);
		}
		m.run(100000);
		assertEquals(30, out.ids.size());
		assertTrue(out.classIds.contains(1), "some hits");
		assertTrue(out.classIds.contains(2), "some misses");
		assertFalse(out.classIds.contains(0), "every request is classified");
	}

	@Test
	void cacheHitRateMeasureIsFed() throws Exception {
		wire(new FIFOCache(), 2, 2, 1, 2);
		RecordingMeasure hit = m.sectionMeasure(st.getSection(NodeSection.SERVICE), SimConstants.CACHE_HIT_RATE, 0);
		for (int i = 0; i < 4; i++) {
			m.inject(st, 0, i);
		}
		m.run(10000);
		assertFalse(hit.samples.isEmpty());
	}

	@Test
	void requestOutsideTheMappingPassesThroughUnclassified() throws Exception {
		wire(new FIFOCache(), 2, 2, 1, 2);
		m.inject(st, 1, 0.0);
		m.run(10000);
		assertEquals(java.util.Arrays.asList(1), out.classIds);
	}
}
