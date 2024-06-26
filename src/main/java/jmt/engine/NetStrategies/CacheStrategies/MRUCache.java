package jmt.engine.NetStrategies.CacheStrategies;

import jmt.engine.NetStrategies.CacheStrategy;
import jmt.engine.QueueNet.CacheItem;

import java.util.LinkedList;

public class MRUCache extends CacheStrategy{

	public MRUCache(){}

	/**
	 * MRU discards the most-recently-used items first.
	 * At the 11th VLDB conference, Chou and DeWitt said: "When a file is being repeatedly scanned in a [looping sequential] reference pattern, MRU is the best replacement algorithm."
	 * It is most useful in situations where the older an item is, the more likely it is to be accessed. 
	 * @param caches the list of the cached items.
	 * @return
	 */
	@Override
	public CacheItem getRemoveItem(LinkedList<CacheItem> caches) {
		double longestAccessTime = Double.MIN_VALUE;
		int index = -1;
		for(int i=0; i<caches.size(); i++){
			double tempAccessTime = caches.get(i).getLastAccessTime();
			if( tempAccessTime > longestAccessTime ){
				longestAccessTime = tempAccessTime;
				index = i;
			}
		}
		return caches.get(index);
	}

}
