package jmt.engine.QueueNet;

import java.util.LinkedList;

public class CacheItem {

	private int id;

	private LinkedList<Double> accessTimes;

	// Time to alive
	private double ttl;

	private boolean isCached;

	public CacheItem(int id) {
		this.accessTimes = new LinkedList<Double>();
		this.id = id;
		this.ttl = -1;
	}

	public int getId() {
		return id;
	}

//	public void setId(int id) {
//		this.id = id;
//	}

	public double getFirstAccessTime() {
		return accessTimes.getFirst();
	}

	public double getLastAccessTime() {
		return accessTimes.getLast();
	}

	public boolean isCached() {
		return isCached;
	}

	private void setCached(boolean cached) {
		isCached = cached;
	}

	public int getNumberOfAccess() {
		return accessTimes.size();
	}

	public LinkedList<Double> getAccessTimes() {
		return accessTimes;
	}

	public void access(double time) {
		if(!isCached){
			setCached(true);
		}
		this.accessTimes.addLast(time);
	}

	public void clear() {
		setCached(false);
		this.accessTimes.clear();
	}

	public double getTTL() {
		return ttl;
	}

	public void setTTL(double ttl) {
		this.ttl = ttl;
	}
}
