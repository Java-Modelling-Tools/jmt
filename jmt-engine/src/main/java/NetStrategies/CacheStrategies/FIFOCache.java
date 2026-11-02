package NetStrategies.CacheStrategies;

import NetStrategies.CacheStrategy;
import QueueNet.CacheItem;

import java.util.LinkedList;

// set removeItemMode = 1 for strict FIFO(m)
/**
 * FIFO (First In First Out) cache eviction strategy: removes the item that first joined the cached item list.
 */
/**
 * FIFO (First In First Out) cache eviction strategy: removes the item that first joined the cached item list.
 */
/**
 * FIFO (First In First Out) cache eviction strategy: removes the item that first joined the cached item list.
 */
public class FIFOCache extends CacheStrategy {

	public FIFOCache(){}

	/**
	 * For FIFO cache is to find the item that first join the cached item list.
	 * @param caches the list of the cached items.
	 * @return
	 */
	@Override
	public CacheItem getRemoveItem(LinkedList<CacheItem> caches) {
		return caches.getFirst();
	}

//	@Override
//	public boolean needToCache(NetNode ownerNode){
//		return true;
//	}
//
//	@Override
//	public Job getRemoveJob(NetNode ownerNode) {return null;}
}
