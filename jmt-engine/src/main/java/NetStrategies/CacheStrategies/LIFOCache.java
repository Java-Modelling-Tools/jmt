package NetStrategies.CacheStrategies;

import NetStrategies.CacheStrategy;
import QueueNet.CacheItem;

import java.util.LinkedList;

/**
 * LIFO (Last In First Out) cache eviction strategy: removes the item that last joined the cached item list.
 */
/**
 * LIFO (Last In First Out) cache eviction strategy: removes the item that last joined the cached item list.
 */
/**
 * LIFO (Last In First Out) cache eviction strategy: removes the item that last joined the cached item list.
 */
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
