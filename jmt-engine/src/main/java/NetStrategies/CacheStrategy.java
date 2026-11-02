package NetStrategies;


import java.util.LinkedList;

import jmt.common.AutoCheck;
import QueueNet.CacheItem;
import QueueNet.NetSystem;
import random.engine.RandomEngine;

/**
 * Base class of the cache eviction strategies of the JSIM engine: it decides which cached item is removed when the cache is full.
 */
/**
 * Base class of the cache eviction strategies of the JSIM engine: it decides which cached item is removed when the cache is full.
 */
/**
 * Base class of the cache eviction strategies of the JSIM engine: it decides which cached item is removed when the cache is full.
 */
/**
 * Base class of the cache eviction strategies of the JSIM engine: it decides which cached item is removed when the cache is full.
 */
public abstract class CacheStrategy implements AutoCheck{

//	public abstract boolean needToCache(NetNode ownerNode);

//	public boolean getToCache(NodeSection section){
//		return needToCache(section.getOwnerNode());
//	}
	protected RandomEngine engine;

	protected NetSystem netSystem;

	// 0: the removed item is added to the previous position of the job. 
	// 1: the removed item is added to the front of the previous cache list.
	// 2: the removed item is not added back
	protected int removeItemMode = 0;   

	public void setRandomEngine(RandomEngine engine) {
		this.engine = engine;
	}

	public void setNetSystem(NetSystem netSystem) { this.netSystem = netSystem; }
	
	public abstract CacheItem getRemoveItem(LinkedList<CacheItem> caches);

	public boolean check() { return true; }

	public int getRemoveItemMode() {
		return removeItemMode;
	}

	public int setRemoveItemMode(int removeItemMode) {
		this.removeItemMode = removeItemMode;
		return removeItemMode;
	}
}
