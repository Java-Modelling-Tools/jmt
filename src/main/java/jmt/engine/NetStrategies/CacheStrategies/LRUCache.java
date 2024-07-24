package jmt.engine.NetStrategies.CacheStrategies;

import jmt.engine.NetStrategies.CacheStrategy;
import jmt.engine.QueueNet.CacheItem;

import java.util.LinkedList;

public class LRUCache extends CacheStrategy{

	public LRUCache(){
		setRemoveItemMode(2);   // removeItemMode = 1 for LRU(m). removeItemMode = 2 for h-LRU(m)
	}

	/**
	 * Cache item linked list has an access time list which record each request timestamp.
	 * For LRU (Least Recent Used) is to find the cache item that has the smallest timestamp for last access.
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

//	@Override
//	public boolean needToCache (NetNode ownerNode){
//		return true;
//	}
//
//	@Override
//	public Job getRemoveJob(NetNode ownerNode) {return null;}

}
