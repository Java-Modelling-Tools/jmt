/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.engine.NodeSections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.CacheStrategies.TTLCache;
import jmt.engine.NetStrategies.CacheStrategy;
import jmt.engine.QueueNet.CacheItem;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.random.discrete.DiscreteDistribution;
import jmt.engine.random.engine.RandomEngine;

/**
 * This class implements a class switch.
 *
 * @author Sebatiano Spicuglia, Arif Canakoglu
 */
public class Cache extends ServiceSection {
	private int maxItems;
	private Integer[] cacheCapacity;
	private Float[][] cacheMatrix;
	private int numOfJobClasses;

	private ArrayList<CacheItem> items;		// once created, only retrieve and update. all possible items that can be stored in the cache
	private LinkedList<CacheItem>[] caches;	// since we need to insert or delete it frequently.representing the items currently stored in the cache.

	private CacheStrategy replacePolicy;
	private DiscreteDistribution[] popularity;

	private JobInfoList nodeJobsList;		// JobInfoList used to calculate the node measurement.

	// also have a field `jobsList` inherited from NodeSection to calculate the section measurement.

	TreeMap<Integer, Integer> count;		// variable used for test

	// Use two HashMaps to store the mappings
	private Map<JobClass, JobClass> jobClassToHitClass = new HashMap<>();
	private Map<JobClass, JobClass> jobClassToMissClass = new HashMap<>();

	// Method to add a job class mapping
	public void addJobClassMapping(JobClass jobClass, JobClass hitClass, JobClass missClass) {
		jobClassToHitClass.put(jobClass, hitClass);
		jobClassToMissClass.put(jobClass, missClass);
	}

	// Method to get the hit class for a given job class
	public JobClass getHitClass(JobClass jobClass) {
		return jobClassToHitClass.get(jobClass); // Return null if no mapping found
	}

	// Method to get the miss class for a given job class
	public JobClass getMissClass(JobClass jobClass) {
		return jobClassToMissClass.get(jobClass); // Return null if no mapping found
	}

	/* 
	// Define a class to represent the job class mapping
    private static class JobClassMapping {
        private JobClass jobClass;
        private JobClass hitClass;
        private JobClass missClass;

        public JobClassMapping(JobClass jobClass, JobClass hitClass, JobClass missClass) {
            this.jobClass = jobClass;
            this.hitClass = hitClass;
            this.missClass = missClass;
        }
    }

	// Use a HashMap to store the mappings
	private Map<JobClass, JobClassMapping> jobClassMappings = new HashMap<>();

	// Method to add a job class mapping
	public void addJobClassMapping(JobClass jobClass, JobClass hitClass, JobClass missClass) {
		jobClassMappings.put(jobClass, new JobClassMapping(jobClass, hitClass, missClass));
	}

	// Method to get the hit class for a given job class
	public JobClass getHitClass(JobClass jobClass) {
		JobClassMapping mapping = jobClassMappings.get(jobClass);
		return (mapping != null) ? mapping.hitClass : null; // Return null if no mapping found
	}

	// Method to get the miss class for a given job class
	public JobClass getMissClass(JobClass jobClass) {
		JobClassMapping mapping = jobClassMappings.get(jobClass);
		return (mapping != null) ? mapping.missClass : null; // Return null if no mapping found
	}
	*/



	public Cache(Integer maxItems, Integer[] cacheCapacity, Object[] matrix, JobClass[] jobClasses, JobClass[] hitClasses, JobClass[] missClasses, CacheStrategy replacePolicy, DiscreteDistribution[] pop) {
		this.maxItems = maxItems;
		this.cacheCapacity = cacheCapacity;
		this.cacheMatrix = new Float[matrix.length][matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			Float[] row = (Float[]) matrix[i];
			for (int j = 0; j < row.length; j++) {
				this.cacheMatrix[i][j] = row[j];
			}
		}
		this.replacePolicy = replacePolicy;
		this.popularity = pop;

		for (int i = 0; i < jobClasses.length; i++) {
			addJobClassMapping(jobClasses[i], hitClasses[i], missClasses[i]);
		}
		this.count = new TreeMap<>();		// variable used for test
	}

	

	/**
	 *  when the node section itself is linked to the owner node.
	 * 	This method should be used to set node section properties depending on
	 *  the owner node ones.
	 * @param ownerNode
	 * @throws NetException
	 */
	@Override
	public void nodeLinked(NetNode ownerNode) throws NetException {
		this.numOfJobClasses = getJobClasses().size();

		RandomEngine engine = ownerNode.getNetSystem().getEngine();
		this.replacePolicy.setRandomEngine(engine);
		this.replacePolicy.setNetSystem(ownerNode.getNetSystem());		// TTL replace policy need to know the system time.
		//		this.replacePolicy.initilize(cacheCapacity);					// Currently no replace Policy need to initialize

		// initialize the TTL replace policy.
		double ttl = -1.0;
		if (replacePolicy instanceof TTLCache) {
			ttl = ((TTLCache) replacePolicy).getTTL();
			((TTLCache) replacePolicy).getReplacePolicy().setRandomEngine(engine);
		}

		// initialize the popularity for different class.
		for(int i=0; i<numOfJobClasses; i++){
			if(popularity[i]!=null){
				popularity[i].setRandomEngine(engine);
			}
		}

		nodeJobsList = ownerNode.getJobInfoList();
		// System.out.println(replacePolicy.toString());

		// Initialize the cache items lists.
		items = new ArrayList<CacheItem>(maxItems);
		for(int i=1; i<=maxItems; i++){
			CacheItem cacheItem = new CacheItem(i);
			items.add(cacheItem);
			if(ttl>0){
				cacheItem.setTTL(ttl);
			}
		}


		//this.caches = new LinkedList<CacheItem>[cacheCapacity.length];
		caches = (LinkedList<CacheItem>[]) new LinkedList<?>[cacheCapacity.length];
		for (int i = 0; i < cacheCapacity.length; i++) {
			caches[i] = new LinkedList<CacheItem>();
		}
		//TestDistribution(popularity);		// method used for test
	}

	/**
	 *  when the node section itself is unlinked to the owner node.
	 *  This method should be used to reset node section properties depending on
	 *  the owner node ones.
	 * @param ownerNode
	 * @throws NetException
	 * modified by Yujiao Xiao
	 */
	@Override
	protected int process(NetMessage message) throws NetException {
		switch (message.getEvent()) {

			case NetEvent.EVENT_START:
				break;

			case NetEvent.EVENT_JOB:
				Job job = message.getJob();
				JobClass originalClass = job.getJobClass();

				//if(job.getJobClass().getCacheHitClassName() == null && job.getJobClass().getCacheMissClassName() == null){
				if(getHitClass(job.getJobClass()) == null && 
						getMissClass(job.getJobClass()) == null){
					sendForward(NetEvent.EVENT_JOB, job, 0.0);
					sendBackward(NetEvent.EVENT_ACK, job, 0.0);
					break;
				}
				

				// get the request item that follow the specific popularity.
				int requsetId = 0;
				CacheItem targetItem = null;

				// Id from 1 to numberOfItems generated by popularity.
				DiscreteDistribution pop = popularity[job.getJobClass().getId()];
				if(pop == null){
					throw new AssertionError("Cannot find the corresponding DiscreteDistribution popularity");
				}
				requsetId = pop.nextRand();
				targetItem = getItemById(items, requsetId);
				if(targetItem == null) {
					throw new AssertionError("Cannot find the corresponding item in the item list." +
							"It may caused by the error popularity parameter: maxItems < numberOfItems of popularity");
				}
				//count = TestNextRand(count, requsetId);		// To test the popularity function.

				Integer[] itemPos = findIteminCache(requsetId);
				// if the items has already cached.
				if (itemPos[0] >= 0) {

					// get the new position of the item in the cache.
					int newItemPos = -1;
					Float[] newPosProb = cacheMatrix[itemPos[0]];
					double randomValue = Math.random();
					for (int i = 0; i < newPosProb.length; i++) {
						if (randomValue < newPosProb[i]) {
							newItemPos = i;
							break;
						}
					}

					switch (replacePolicy.getRemoveItemMode()) {
						case 0:
							if (newItemPos >= 0 && newItemPos != itemPos[0]) {  // if the item need to be moved to another cache.
								// update the cache item position.
								// execute the replace policy
								if (caches[newItemPos].size() == cacheCapacity[newItemPos]) {  // if the target cache list is full, remove the item
									// execute the replace policy
									CacheItem removedItem = replacePolicy.getRemoveItem(caches[newItemPos]);
									//removeItem.clear();				// set isCache false and clear the accessTimes list.
									caches[newItemPos].remove(removedItem);
									caches[itemPos[0]].set(itemPos[1], removedItem);
								}
								caches[newItemPos].add(targetItem);
								break;
							}
							else{
								//remove targetItem from the old cache and add it to the new cache
								caches[itemPos[0]].remove(targetItem);
								caches[newItemPos].add(targetItem);
								break;
								}
						case 1:
							if (newItemPos >= 0 && newItemPos != itemPos[0]) {  // if the item need to be moved to another cache.
								// update the cache item position.
								// execute the replace policy
								if (caches[newItemPos].size() == cacheCapacity[newItemPos]) {  // if the target cache list is full, remove the item
									// execute the replace policy
									CacheItem removedItem = replacePolicy.getRemoveItem(caches[newItemPos]);
									//removeItem.clear();				// set isCache false and clear the accessTimes list.
									caches[newItemPos].remove(removedItem);
									caches[itemPos[0]].remove(targetItem);
									caches[itemPos[0]].add(removedItem);
								}
								caches[newItemPos].add(targetItem);
								break;
							}
							else{
								//remove targetItem from the old cache and add it to the new cache
								caches[itemPos[0]].remove(targetItem);
								caches[newItemPos].add(targetItem);
								break;	
							}
						case 2:
							while (newItemPos >= 0 && newItemPos != itemPos[0]) {  // if the item need to be moved to another cache.
								// update the cache item position.
								// execute the replace policy
								//if target item is in the new cache list, move it to the first place
								if (findIteminCacheList(requsetId,newItemPos) >= 0){
									caches[newItemPos].remove(targetItem);
									caches[newItemPos].add(targetItem);
									caches[itemPos[0]].remove(targetItem);
									caches[itemPos[0]].add(targetItem);
								}
								else{
									if (caches[newItemPos].size() == cacheCapacity[newItemPos]) {  // if the target cache list is full, remove the item
										// execute the replace policy
										CacheItem removedItem = replacePolicy.getRemoveItem(caches[newItemPos]);
										//removeItem.clear();				// set isCache false and clear the accessTimes list.
										caches[newItemPos].remove(removedItem);
										caches[itemPos[0]].remove(targetItem);
										caches[itemPos[0]].add(targetItem);
										caches[newItemPos].add(targetItem);
										if (findIteminCache(removedItem.getId())[0] < 0){
											removedItem.clear();	
										}
									}
									else{
										caches[newItemPos].add(targetItem);
										caches[itemPos[0]].remove(targetItem);
										caches[itemPos[0]].add(targetItem);
									}
								}

								
								Float[] PosProb = cacheMatrix[newItemPos];
								double randValue = Math.random();
								for (int i = 0; i < PosProb.length; i++) {
									if (randValue < PosProb[i]) {
										newItemPos = i;
										break;
									}
								}
								itemPos = new Integer[]{newItemPos, findIteminCacheList(requsetId,newItemPos)};
							}
					}

					/*
					if (newItemPos >= 0 && newItemPos != itemPos[0]) {  // if the item need to be moved to another cache.
						// update the cache item position.
						// execute the replace policy
						if (caches[newItemPos].size() == cacheCapacity[newItemPos]) {  // if the target cache list is full, remove the item
							// execute the replace policy
							CacheItem removedItem = replacePolicy.getRemoveItem(caches[newItemPos]);
							//removeItem.clear();				// set isCache false and clear the accessTimes list.
							switch (replacePolicy.getRemoveItemMode()) {
								case 0:
									caches[newItemPos].remove(removedItem);
									caches[itemPos[0]].set(itemPos[1], removedItem);
									break;
								case 1:
									caches[newItemPos].remove(removedItem);
									caches[itemPos[0]].remove(targetItem);
									caches[itemPos[0]].add(removedItem);
									break;
								case 2:
									//if target item is in the new cache list, move it to the first place
									for(int j = 0; j < caches[newItemPos].size(); j++){
										if(caches[newItemPos].get(j).getId() == requsetId){
											caches[newItemPos].remove(targetItem);
											caches[itemPos[0]].remove(targetItem);
											caches[itemPos[0]].add(targetItem);
											break;
										}
									}
									caches[newItemPos].remove(removedItem);
									caches[itemPos[0]].remove(targetItem);
									caches[itemPos[0]].add(targetItem);
									break;
							}
							caches[newItemPos].add(targetItem);
						}
						else{
							switch (replacePolicy.getRemoveItemMode()) {
								case 2:
									//if target item is in the new cache list, move it to the first place
									for(int j = 0; j < caches[newItemPos].size(); j++){
										if(caches[newItemPos].get(j).getId() == requsetId){
											caches[newItemPos].remove(targetItem);
											caches[itemPos[0]].remove(targetItem);
											caches[itemPos[0]].add(targetItem);
											break;
										}
									}
									break;
								default:
									//remove targetItem from the old cache and add it to the new cache
									caches[itemPos[0]].remove(targetItem);
									caches[newItemPos].add(targetItem);
							}
								
							}
						}	
						*/



					// update cache item information
					targetItem.access(job.getNetSystem().getTime());
					


					//JobClass cacheHitClass = job.getJobClass().getCacheHitClass();
					JobClass cacheHitClass = getHitClass(job.getJobClass());


					// record the cache hit count to the jobListInfo and update hitRate measure.
					jobsList.CacheJob(originalClass, true);

					// switch the class, in section jobs list
					job.setJobClass(cacheHitClass);
					JobInfo jobInfo = jobsList.lookFor(job);
					jobsList.getInternalJobInfoList(cacheHitClass).add(jobInfo);
					jobsList.getInternalJobInfoList(originalClass).remove(jobInfo);
					// in node jobs list
					JobInfo nodeJobInfo = nodeJobsList.lookFor(job);
					nodeJobsList.getInternalJobInfoList(cacheHitClass).add(nodeJobInfo);
					nodeJobsList.getInternalJobInfoList(originalClass).remove(nodeJobInfo);
					// in global jobs list
					if (!(job instanceof ForkJob)) {
						GlobalJobInfoList global = getOwnerNode().getQueueNet().getJobInfoList();
						global.performJobClassSwitch(originalClass, cacheHitClass);
					}
					
				}
				// Cache miss
				else {
					// get the cache Miss Class
					//JobClass cacheMissClass = job.getJobClass().getCacheMissClass();
					JobClass cacheMissClass = getMissClass(job.getJobClass());

					// if it is TTLCache, clean expired items first.
					if(replacePolicy instanceof  TTLCache){
						((TTLCache)replacePolicy).cleanExpiredItem(caches[0]);  //TODO
					}

					// else check if the cache is full, apply the replace policy
					if (caches[0].size() == cacheCapacity[0]) {
						// execute the replace policy
						CacheItem removeItem = replacePolicy.getRemoveItem(caches[0]);
						if (replacePolicy.getRemoveItemMode() != 2){
							removeItem.clear();				// set isCache false and clear the accessTimes list.
						}
						caches[0].remove(removeItem);
						if ((replacePolicy.getRemoveItemMode() == 2) && (findIteminCache(removeItem.getId())[0] < 0)){
							removeItem.clear();	
						}
					}
					// else cache is not full, directly add this new items to cache.
					// update cache item information
					targetItem.access(job.getNetSystem().getTime());
					caches[0].add(targetItem);
					// record the cache miss count to the jobListInfo and update hitRate measure.
					jobsList.CacheJob(originalClass, false);

					// switch the class, in section jobs list
					job.setJobClass(cacheMissClass);
					JobInfo jobInfo = jobsList.lookFor(job);
					jobsList.getInternalJobInfoList(cacheMissClass).add(jobInfo);
					jobsList.getInternalJobInfoList(originalClass).remove(jobInfo);
					// in node jobs list
					JobInfo nodeJobInfo = nodeJobsList.lookFor(job);
					nodeJobsList.getInternalJobInfoList(cacheMissClass).add(nodeJobInfo);
					nodeJobsList.getInternalJobInfoList(originalClass).remove(nodeJobInfo);
					// in global jobs list
					if (!(job instanceof ForkJob)) {
						GlobalJobInfoList global = getOwnerNode().getQueueNet().getJobInfoList();
						global.performJobClassSwitch(originalClass, cacheMissClass);
					}
				}
				sendForward(NetEvent.EVENT_JOB, job, 0.0);
				sendBackward(NetEvent.EVENT_ACK, job, 0.0);
				break;

			case NetEvent.EVENT_ACK:
				break;

			case NetEvent.EVENT_STOP:
				break;

			default:
				return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}


	private CacheItem getItemById(List<CacheItem> itemsList, int id){
		for(CacheItem i: itemsList){
			if(i.getId() == id){
				return i;
			}
		}
		return null;
	}


	private Integer[] findIteminCache(int id){
		for(int i = 0; i < cacheCapacity.length; i++){
			if (getItemById(this.caches[i], id) != null){
				for(int j = 0; j < caches[i].size(); j++){
					if(caches[i].get(j).getId() == id){
						return new Integer[]{i, j};
					}
			}
		}
	}
		return new Integer[]{-1, -1}; 
	}

	private Integer findIteminCacheList(int id, int cacheIndex){
		if (getItemById(this.caches[cacheIndex], id) != null){
			for(int j = 0; j < caches[cacheIndex].size(); j++){
				if(caches[cacheIndex].get(j).getId() == id){
					return j;
				}
			}
		}
		return -1; 
	}


	/**********************************************************
	 *  These are test functions for the popularity module.	  *
	 ***********************************************************/
	private TreeMap<Integer, Integer> TestNextRand(TreeMap<Integer, Integer> count, int nextRand){
		Integer target = count.get(Integer.valueOf(nextRand));
		System.out.println("=== Current request Id is "+nextRand+" ===");
		if (target == null) {
			count.put(nextRand, 1);
		} else {
			count.put(nextRand, count.get(nextRand) + 1);
		}
		Iterator it = count.keySet().iterator();
		while (it.hasNext()) {
			Integer key = (Integer) it.next();
			System.out.println("Key: " + key + " is: " + count.get(key));
		}
		System.out.println("=== END ===");
		return count;
	}

	private void TestDistribution(DiscreteDistribution ds){
		int upper = ds.getUpper();
		int lower = ds.getlower();
		for(int i=lower; i<=upper; i++){
			System.out.println("CDF: "+ds.cdf(i)+" \tPMF: "+ ds.pmf(i));
		}
		System.out.println("=======");
		System.out.println("Mean: "+ds.theorMean());
		System.out.println("Variance: "+ds.theorVariance());
	}
}

