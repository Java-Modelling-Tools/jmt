package NetStrategies.CacheStrategies;

import java.util.LinkedList;

import NetStrategies.CacheStrategy;
import QueueNet.CacheItem;


/**
 * h-LRU cache eviction strategy, the hybrid variant of LRU used for cached items.
 */
/**
 * h-LRU cache eviction strategy, the hybrid variant of LRU used for cached items.
 */
/**
 * h-LRU cache eviction strategy, the hybrid variant of LRU used for cached items.
 */
/**
 * h-LRU cache eviction strategy, the hybrid variant of LRU used for cached items.
 */
public class HLRUCache extends LRUCache{

	public HLRUCache(){
		setRemoveItemMode(2);   // removeItemMode = 1 for LRU(m). removeItemMode = 2 for h-LRU(m)
	}

}
