package jmt.engine.NetStrategies.CacheStrategies;

import jmt.engine.NetStrategies.CacheStrategy;
import jmt.engine.QueueNet.CacheItem;

import java.util.LinkedList;

public class LIFOCache extends CacheStrategy {

	public LIFOCache(){}

	/**
	 * LIFO cache works by finding the item that last join the cached item list.
	 * @param caches the list of the cached items.
	 * @return
	 */
	@Override
	public CacheItem getRemoveItem(LinkedList<CacheItem> caches) {
		return caches.getLast();
	}
}
