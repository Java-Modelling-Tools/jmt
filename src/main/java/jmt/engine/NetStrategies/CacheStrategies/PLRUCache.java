package jmt.engine.NetStrategies.CacheStrategies;

import jmt.engine.NetStrategies.CacheStrategy;
import jmt.engine.QueueNet.CacheItem;

import java.util.LinkedList;

public class PLRUCache extends CacheStrategy{

	public PLRUCache(){}

	/**
	 * For CPU caches with large associativity (generally > four ways), the implementation cost of LRU becomes prohibitive.
	 * PLRU (Pseudo-LRU) approximates the behavior of the more computationally expensive Least Recently Used (LRU) algorithm but with lower implementation complexity.
	 * @param caches the list of the cached items.
	 * @return
	 */
	@Override
	public CacheItem getRemoveItem(LinkedList<CacheItem> caches) {
		double leastAccessTime = Double.MAX_VALUE;
		int index = -1;
		for(int i=0; i<caches.size(); i++){
			double temp = caches.get(i).getLastAccessTime();
			if( temp < leastAccessTime ){
				leastAccessTime = temp;
				index = i;
			}
		}
		return caches.get(index);
	}

}
