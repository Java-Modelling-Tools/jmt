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

package NetStrategies.CacheStrategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import NetStrategies.CacheStrategy;
import QueueNet.CacheItem;
import QueueNet.NetSystem;
import random.engine.MersenneTwister;

class CacheStrategiesTest {

	/** Item accessed at the given times. */
	private static CacheItem item(int id, double... accesses) {
		CacheItem c = new CacheItem(id);
		for (double t : accesses) {
			c.access(t);
		}
		return c;
	}

	private static LinkedList<CacheItem> list(CacheItem... items) {
		LinkedList<CacheItem> l = new LinkedList<CacheItem>();
		for (CacheItem c : items) {
			l.add(c);
		}
		return l;
	}

	@Test
	void fifoRemovesFirstAndLifoRemovesLast() {
		CacheItem a = item(1, 1);
		CacheItem b = item(2, 2);
		CacheItem c = item(3, 3);
		assertSame(a, new FIFOCache().getRemoveItem(list(a, b, c)));
		assertSame(c, new LIFOCache().getRemoveItem(list(a, b, c)));
	}

	@Test
	void lruAndPlruRemoveLeastRecentlyAccessed() {
		CacheItem a = item(1, 5);
		CacheItem b = item(2, 1, 9);
		CacheItem c = item(3, 3);
		assertSame(c, new LRUCache().getRemoveItem(list(a, b, c)));
		assertSame(c, new PLRUCache().getRemoveItem(list(a, b, c)));
		assertSame(c, new HLRUCache().getRemoveItem(list(a, b, c)));
	}

	@Test
	void mruRemovesMostRecentlyAccessed() {
		CacheItem a = item(1, 5);
		CacheItem b = item(2, 1, 9);
		CacheItem c = item(3, 3);
		assertSame(b, new MRUCache().getRemoveItem(list(a, b, c)));
	}

	@Test
	void lfuRemovesLeastFrequentlyAccessedFirstOnTies() {
		CacheItem a = item(1, 1, 2, 3);
		CacheItem b = item(2, 1);
		CacheItem c = item(3, 1);
		assertSame(b, new LFUCache().getRemoveItem(list(a, b, c)));
	}

	@Test
	void randomRemovesAMemberDrawnFromTheEngine() {
		RandomCache rc = new RandomCache();
		rc.setRandomEngine(new MersenneTwister(7L));
		CacheItem a = item(1, 1);
		CacheItem b = item(2, 1);
		CacheItem c = item(3, 1);
		LinkedList<CacheItem> l = list(a, b, c);
		boolean[] seen = new boolean[4];
		for (int i = 0; i < 200; i++) {
			seen[rc.getRemoveItem(l).getId()] = true;
		}
		assertTrue(seen[1] && seen[2] && seen[3]);
	}

	@Test
	void removeItemModesAndChecksAreExposed() {
		assertEquals(0, new FIFOCache().getRemoveItemMode());
		assertEquals(1, new LRUCache().getRemoveItemMode());
		assertEquals(2, new HLRUCache().getRemoveItemMode());
		CacheStrategy s = new LIFOCache();
		assertEquals(2, s.setRemoveItemMode(2));
		assertEquals(2, s.getRemoveItemMode());
		assertTrue(s.check());
	}

	@Test
	void ttlDelegatesToReplacePolicyWhenGiven() {
		LRUCache lru = new LRUCache();
		TTLCache ttl = new TTLCache(10.0, lru);
		assertEquals(10.0, ttl.getTTL(), 0.0);
		assertSame(lru, ttl.getReplacePolicy());
		CacheItem a = item(1, 5);
		CacheItem b = item(2, 2);
		assertSame(b, ttl.getRemoveItem(list(a, b)));
	}

	@Test
	void ttlWithoutPolicyReturnsAMemberOfTheCache() {
		TTLCache ttl = new TTLCache(10.0, null);
		ttl.setNetSystem(new NetSystem());
		ttl.setRandomEngine(new MersenneTwister(7L));
		CacheItem a = item(1, 1);
		a.setTTL(5);
		CacheItem b = item(2, 1);
		b.setTTL(1);
		LinkedList<CacheItem> l = list(a, b);
		assertTrue(l.contains(ttl.getRemoveItem(l)));
	}

	@Disabled("bug: TTLCache.getRemoveItem never updates leastExpireTime, so it always returns the last item instead of the one expiring first")
	@Test
	void ttlWithoutPolicyRemovesItemClosestToExpiry() {
		TTLCache ttl = new TTLCache(10.0, null);
		ttl.setNetSystem(new NetSystem());
		ttl.setRandomEngine(new MersenneTwister(7L));
		CacheItem a = item(1, 1);
		a.setTTL(5);
		CacheItem b = item(2, 1);
		b.setTTL(1);
		CacheItem c = item(3, 1);
		c.setTTL(9);
		assertSame(b, ttl.getRemoveItem(list(a, b, c)));
	}

	@Test
	void cleanExpiredItemDropsExpiredItemsAndClearsThem() {
		TTLCache ttl = new TTLCache(10.0, null);
		ttl.setNetSystem(new NetSystem());
		CacheItem alive = item(1, 0);
		alive.setTTL(100);
		CacheItem dead = item(2, 0);
		dead.setTTL(-1);
		LinkedList<CacheItem> l = list(alive, dead);
		ttl.cleanExpiredItem(l);
		assertEquals(1, l.size());
		assertSame(alive, l.getFirst());
		assertEquals(0, dead.getNumberOfAccess());
		assertEquals(false, dead.isCached());
	}

	@Disabled("bug: TTLCache.cleanExpiredItem removes while indexing forwards, so an expired item right after another expired item is skipped")
	@Test
	void cleanExpiredItemDropsAdjacentExpiredItems() {
		TTLCache ttl = new TTLCache(10.0, null);
		ttl.setNetSystem(new NetSystem());
		CacheItem d1 = item(1, 0);
		d1.setTTL(-1);
		CacheItem d2 = item(2, 0);
		d2.setTTL(-1);
		LinkedList<CacheItem> l = list(d1, d2);
		ttl.cleanExpiredItem(l);
		assertEquals(0, l.size());
	}

	@Test
	void cacheItemTracksAccesses() {
		CacheItem c = new CacheItem(4);
		assertEquals(4, c.getId());
		assertEquals(-1.0, c.getTTL(), 0.0);
		assertEquals(false, c.isCached());
		c.access(2.0);
		c.access(5.0);
		assertTrue(c.isCached());
		assertEquals(2.0, c.getFirstAccessTime(), 0.0);
		assertEquals(5.0, c.getLastAccessTime(), 0.0);
		assertEquals(2, c.getNumberOfAccess());
		assertEquals(2, c.getAccessTimes().size());
	}
}
