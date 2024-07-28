package jmt.engine.NetStrategies.CacheStrategies;

import jmt.engine.NetStrategies.CacheStrategy;
import jmt.engine.QueueNet.CacheItem;

import java.util.LinkedList;

public class RandomCache extends CacheStrategy {

	public RandomCache(){}

	/**
	 * For RandomCache is to randomly choose a cache item in the cached items list.
	 * @param caches the list of the cached items.
	 * @return
	 */
	@Override
	public CacheItem getRemoveItem(LinkedList<CacheItem> caches) {
		return caches.get((int)Math.floor(engine.raw() * caches.size()));
	}

//	@Override
//	public boolean needToCache (NetNode ownerNode){
//		RandomEngine randomEngine = ownerNode.getNetSystem().getEngine();
//		int temp = randomEngine.nextInt();
//		return (temp&0x01) == 1;
//	}
//
//	@Override
//	public Job getRemoveJob(NetNode ownerNode) {return null;}

}
