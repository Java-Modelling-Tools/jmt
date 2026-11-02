package NetStrategies.CacheStrategies;

import NetStrategies.CacheStrategy;
import QueueNet.CacheItem;

import java.util.LinkedList;

/**
 * LFU (Least Frequently Used) cache eviction strategy: removes the item with the smallest number of accesses.
 */
/**
 * LFU (Least Frequently Used) cache eviction strategy: removes the item with the smallest number of accesses.
 */
/**
 * LFU (Least Frequently Used) cache eviction strategy: removes the item with the smallest number of accesses.
 */
/**
 * LFU (Least Frequently Used) cache eviction strategy: removes the item with the smallest number of accesses.
 */
public class LFUCache extends CacheStrategy {

	public LFUCache(){}

	@Override
	public CacheItem getRemoveItem(LinkedList<CacheItem> caches) {
		int numberOfAccess = Integer.MAX_VALUE;
		int index = -1;
		for(int i=0; i<caches.size(); i++){
			int temp = caches.get(i).getNumberOfAccess();
			if( temp < numberOfAccess ){
				numberOfAccess = temp;
				index = i;
			}
		}
		return caches.get(index);
	}

}
