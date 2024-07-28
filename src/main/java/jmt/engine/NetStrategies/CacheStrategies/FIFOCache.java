package jmt.engine.NetStrategies.CacheStrategies;

import jmt.engine.NetStrategies.CacheStrategy;
import jmt.engine.QueueNet.CacheItem;

import java.util.LinkedList;

// set removeItemMode = 1 for strict FIFO(m)
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
