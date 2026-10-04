package NetStrategies.CacheStrategies;

import java.util.LinkedList;

import NetStrategies.CacheStrategy;
import QueueNet.CacheItem;


public class HLRUCache extends LRUCache{

	public HLRUCache(){
		setRemoveItemMode(2);   // removeItemMode = 1 for LRU(m). removeItemMode = 2 for h-LRU(m)
	}

}
