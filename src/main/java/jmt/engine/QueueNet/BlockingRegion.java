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

package jmt.engine.QueueNet;

import java.util.ArrayList;
import java.util.List;

import jmt.engine.dataAnalysis.Measure;
import jmt.engine.simEngine.Simulation;

/**
 * This class represents a blocking region, that is a region with a finite capacity.
 * <br>
 * The constraint can be global (the constraint is shared, i.e. relative to the total
 * occupation of jobs, without considering their own job class)
 * or relative to each class (in this case it may happen that the
 * jobs of one class are blocked, while the job of another one are not blocked).
 * <br>
 * If other jobs arrive when the region has already reached one or more constraints,
 * the jobs may be blocked or dropped. Each class can decide its own drop strategy,
 * which will be used when a constraint (global or class) is violated.
 * <br>
 * Each class can have a different weight, i.e. jobs of different class may require
 * different capacity.
 *
 *
 * @author Stefano Omini
 *
 */
public class BlockingRegion {

	/** The name of the blocking region */
	protected String name;

	/** the simulation */
	protected Simulation sim;
	
	/** the queue network the region belongs to */
	protected QueueNetwork network;

	/** the name of the nodes contained in the blocking region */
	protected String[] regionNodeNames;
	
	/** the nodes contained in the blocking region */
	protected NetNode[] regionNodes;

	/** reference to the fictitious station which controls arrivals to the blocking region */
	protected NetNode inputStation;

	/** the maximum number of jobs that can be accommodated */
	protected double maxCapacity;

	/** the maximum size of memory that can be allocated */
	protected double maxMemory;

	/** the maximum number of jobs that can be accommodated for each class */
	protected double[] maxCapacityPerClass;

	/** the maximum size of memory that can be allocated for each class */
	protected double[] maxMemoryPerClass;

	/** the number of jobs that are currently accommodated */
	protected double actualOccupation;

	/** the size of memory that are currently allocated */
	protected double actualMemoryOccupation;

	/** the number of jobs that are currently accommodated for each class */
	protected double[] actualOccupationPerClass;

	/** the size of memory that are currently allocated for each class */
	protected double[] actualMemoryOccupationPerClass;

	/** true if the specified class in excess must be dropped */
	protected boolean[] dropClass;

	/** true if there is a global constraint */
	protected boolean globalConstraintDefined;

	/** true if there is a global memory constraint */
	protected boolean globalMemoryConstraintDefined;

	/** true if the specified class has a constraint */
	protected boolean classConstraintDefined[];

	/** true if the specified class has a memory constraint */
	protected boolean classMemoryConstraintDefined[];

	protected double[] classWeights;
	protected double[] classSizes;

	private Measure weightMeasure, sizeMeasure;
	private double lastTimeModified;
	private double lastWeightOccupation, lastSizeOccupation;

	protected List<GroupInfo> groupInfo;

	/**
	 * Creates a blocking region with global, class and group constraints
	 */
	public BlockingRegion(String regionName, double maxCapacity, double maxMemory, double[] maxCapacityPerClass, double[] maxMemoryPerClass, boolean[] drop, double[] classWeights, double[] classSizes,
			String[] groupNames, double[] maxCapacityPerGroup, double[] maxMemoryPerGroup, List<List<Integer>> groupClassIndexLists, Simulation sim, String[] regionNodeNames) {
		//No control on input dimensions.

		//-----------------REGION PROPERTIES----------------------//
		//region name
		this.name = regionName;
		//sets owner simulation
		this.sim = sim;
		//before sim.initialize() method, network has not been set yet
		this.network = null;
		//before sim.initialize() method, net nodes has not been set yet
		this.regionNodeNames = regionNodeNames;
		this.regionNodes = null;

		//------------------GLOBAL CONSTRAINT--------------------//
		this.maxCapacity = maxCapacity;
		if (maxCapacity < 0) {
			//no global constraint
			this.globalConstraintDefined = false;
		} else {
			//global constraint
			this.globalConstraintDefined = true;
		}

		this.maxMemory = maxMemory;
		if (maxMemory < 0) {
			//no global memory constraint
			this.globalMemoryConstraintDefined = false;
		} else {
			//global memory constraint
			this.globalMemoryConstraintDefined = true;
		}

		//init actual occupation
		this.actualOccupation = 0;
		//init actual memory occupation
		this.actualMemoryOccupation = 0;

		//------------------CLASS CONSTRAINT--------------------//
		int classNumber = sim.getClasses().length;
		classConstraintDefined = new boolean[classNumber];
		//class constraint defined
		this.maxCapacityPerClass = maxCapacityPerClass;
		for (int c = 0; c < classNumber; c++) {
			if (maxCapacityPerClass[c] < 0) {
				//no constraint on this class
				classConstraintDefined[c] = false;
			} else {
				//constraint on this class
				classConstraintDefined[c] = true;
			}
		}

		classMemoryConstraintDefined = new boolean[classNumber];
		//class memory constraint defined
		this.maxMemoryPerClass = maxMemoryPerClass;
		for (int c = 0; c < classNumber; c++) {
			if (maxMemoryPerClass[c] < 0) {
				//no memory constraint on this class
				classMemoryConstraintDefined[c] = false;
			} else {
				//memory constraint on this class
				classMemoryConstraintDefined[c] = true;
			}
		}

		//init actual occupation per class
		this.actualOccupationPerClass = new double[classNumber];
		for (int c = 0; c < classNumber; c++) {
			//init actual occupation
			this.actualOccupationPerClass[c] = 0;
		}

		//init actual occupation per class
		this.actualMemoryOccupationPerClass = new double[classNumber];
		for (int c = 0; c < classNumber; c++) {
			//init actual memory occupation
			this.actualMemoryOccupationPerClass[c] = 0;
		}

		//------------------DROP RULE-----------------------//
		this.dropClass = drop;

		//------------------CLASS WEIGHT--------------------//
		this.classWeights = classWeights;

		//------------------CLASS SIZE---------------------//
		this.classSizes = classSizes;

		//------------------GROUP CONSTRAINT----------------------//
		groupInfo = new ArrayList<GroupInfo>();
		for (int i = 0; i < groupNames.length; i++) {
			groupInfo.add(new GroupInfo(groupNames[i], maxCapacityPerGroup[i], maxMemoryPerGroup[i], groupClassIndexLists.get(i)));
		}
	}

	//-----------------------CONSTRAINTS METHODS---------------------------\\

	/** True if there is a global constraint */
	public boolean hasGlobalConstraint() {
		return globalConstraintDefined;
	}

	/** Gets the number of jobs currently inside the blocking region */
	public double getActualOccupation() {
		return actualOccupation;
	}

	/** Gets the number of jobs of the specified class currently inside the blocking region
	 * @param jobClass the specified class
	 */
	public double getActualOccupation(JobClass jobClass) {
		if (jobClass != null) {
			return actualOccupationPerClass[jobClass.getId()];
		} else {
			return 0;
		}
	}

	/** Increases both the number of total jobs currently inside the region
	 * and also the number of jobs of the specified class currently inside
	 * the region.
	 */
	public void increaseOccupation(JobClass jobClass) {
		int c = jobClass.getId();
		actualOccupation += classWeights[c];
		actualMemoryOccupation += classSizes[c];
		actualOccupationPerClass[c] += classWeights[c];
		actualMemoryOccupationPerClass[c] += classSizes[c];
		for (int i = 0; i < groupInfo.size(); i++) {
			if (groupInfo.get(i).classIndexList.contains(c)) {
				groupInfo.get(i).actualOccupation += classWeights[c];
				groupInfo.get(i).actualMemoryOccupation += classSizes[c];
				break;
			}
		}

		double currentTime = network.getNetSystem().getTime();
		if (weightMeasure != null) {
			weightMeasure.update(lastWeightOccupation, currentTime - lastTimeModified);
			lastWeightOccupation = actualOccupation;
		}
		if (sizeMeasure != null) {
			sizeMeasure.update(lastSizeOccupation, currentTime - lastTimeModified);
			lastSizeOccupation = actualMemoryOccupation;
		}
		lastTimeModified = currentTime;
		return;
	}

	/** Decreases the number of jobs currently inside the region. */
	public void decreaseOccupation(JobClass jobClass) {
		int c = jobClass.getId();
		actualOccupation -= classWeights[c];
		actualMemoryOccupation -= classSizes[c];
		actualOccupationPerClass[c] -= classWeights[c];
		actualMemoryOccupationPerClass[c] -= classSizes[c];
		for (int i = 0; i < groupInfo.size(); i++) {
			if (groupInfo.get(i).classIndexList.contains(c)) {
				groupInfo.get(i).actualOccupation -= classWeights[c];
				groupInfo.get(i).actualMemoryOccupation -= classSizes[c];
				break;
			}
		}

		double currentTime = network.getNetSystem().getTime();
		if (weightMeasure != null) {
			weightMeasure.update(lastWeightOccupation, currentTime - lastTimeModified);
			lastWeightOccupation = actualOccupation;
		}
		if (sizeMeasure != null) {
			sizeMeasure.update(lastSizeOccupation, currentTime - lastTimeModified);
			lastSizeOccupation = actualMemoryOccupation;
		}
		lastTimeModified = currentTime;
		return;
	}

	/** Gets the drop property relative to a class constraint: if
	 * true, jobs in excess of this class will be dropped */
	public boolean getClassDrop(JobClass jobClass) {
		return dropClass[jobClass.getId()];
	}

	/** Gets the drop property relative to a class constraint: if
	 * true, jobs in excess of this class will be dropped */
	public boolean getClassDrop(int classIndex) {
		return dropClass[classIndex];
	}

	/**
	 * Tells whether the region is blocked for this class or
	 * has enough place for a job of this class.
	 * @return true if blocked, false if other capacity is available
	 */
	public boolean isBlocked(JobClass jobClass) {
		int id = jobClass.getId();

		if (globalConstraintDefined) {
			if (actualOccupation + classWeights[id] > maxCapacity) {
				return true;
			}
		}

		if (globalMemoryConstraintDefined) {
			if (actualMemoryOccupation + classSizes[id] > maxMemory) {
				return true;
			}
		}

		if (classConstraintDefined[jobClass.getId()]) {
			if (actualOccupationPerClass[id] + classWeights[id] > maxCapacityPerClass[id]) {
				return true;
			}
		}

		if (classMemoryConstraintDefined[jobClass.getId()]) {
			if (actualMemoryOccupationPerClass[id] + classSizes[id] > maxMemoryPerClass[id]) {
				return true;
			}
		}

		for (int i = 0; i < groupInfo.size(); i++) {
			if (groupInfo.get(i).classIndexList.contains(id)) {
				if (groupInfo.get(i).constraintDefined) {
					if (groupInfo.get(i).actualOccupation + classWeights[id] > groupInfo.get(i).maxCapacity) {
						return true;
					}
				}
				if (groupInfo.get(i).memoryConstraintDefined) {
					if (groupInfo.get(i).actualMemoryOccupation + classSizes[id] > groupInfo.get(i).maxMemory) {
						return true;
					}
				}
				break;
			}
		}

		return false;
	}

	//-------------------- end  CONSTRAINTS METHODS---------------------------\\

	/** Gets the name of the blocking region */
	public String getName() {
		return name;
	}

	/**Gets the names of the region nodes */
	public String[] getRegionNodeNames() {
		return regionNodeNames;
	}

	/**
	 * Finds and sets the netNode objects using the node names
	 */
	private void findRegionNodes() {
		//nodes of the region must be found, using their names
		network = sim.getNetwork();
		regionNodes = new NetNode[regionNodeNames.length];
		for (int i = 0; i < regionNodeNames.length; i++) {
			//TODO: check wrong names...?
			regionNodes[i] = network.getNode(regionNodeNames[i]);
		}
	}

	/**
	 * Tells whether a node is contained in the blocking region
	 * @param nodeName the name of the node
	 * @return true if the specified node is contained in the region
	 */
	public boolean belongsToRegion(String nodeName) {
		for (String regionNodeName : regionNodeNames) {
			if (regionNodeName.equals(nodeName)) {
				//the specified node is contained in the blocking region
				return true;
			}
		}
		//the specified node is not contained in the blocking region
		return false;
	}

	/**
	 * Tells whether a node is contained in the blocking region
	 * @param node the NetNode object
	 * @return true if the specified node is contained in the region
	 */
	public boolean belongsToRegion(NetNode node) {
		if (network == null) {
			//if network has not been set
			//nodes of the region must be found, using their names
			findRegionNodes();
		}
		for (NetNode regionNode : regionNodes) {
			if (regionNode == node) {
				//the specified node is contained in the blocking region
				return true;
			}
		}
		//the specified node is not contained in the blocking region
		return false;
	}

	/**
	 * Returns a node, if contained in the blocking region
	 * @param nodeName the name of the node
	 * @return the NetNode object if the specified node is contained in the region,
	 * null otherwise
	 */
	public NetNode getRegionNode(String nodeName) {
		if (network == null) {
			//before searching a particular node for the first time,
			//nodes of the region must be found, using their names
			findRegionNodes();
		}

		for (int i = 0; i < regionNodeNames.length; i++) {
			if (regionNodeNames[i].equals(nodeName)) {
				//the specified node is contained in the blocking region
				//returns the corresponding NetNode object
				return regionNodes[i];
			}
		}
		//the specified node is not contained in the blocking region
		//returns null
		return null;
	}

	/**
	 * Gets the fictitious station that controls arrivals to the blocking region
	 * @return the fictitious station that controls arrivals to the blocking region
	 */
	public NetNode getInputStation() {
		return inputStation;
	}

	/**
	 * Sets the input station of the blocking region
	 * @param inputStation the input station of the blocking region
	 */
	public void setInputStation(NetNode inputStation) {
		this.inputStation = inputStation;
	}

	/**
	 * @return Returns the classWeights.
	 */
	public double[] getClassWeights() {
		return classWeights;
	}

	/**
	 * @return Returns the maxCapacity.
	 */
	public double getMaxCapacity() {
		return maxCapacity;
	}

	public void setWeightMeasure(Measure wm) {
		this.weightMeasure = wm;
	}
	public void setSizeMeasure(Measure sm) {
		this.sizeMeasure = sm;
	}

	protected class GroupInfo {

		public String name;
		public double maxCapacity;
		public double maxMemory;
		public boolean constraintDefined;
		public boolean memoryConstraintDefined;
		public List<Integer> classIndexList;
		public double actualOccupation;
		public double actualMemoryOccupation;

		public GroupInfo(String name, double maxCapacity, double maxMemory, List<Integer> classIndexList) {
			this.name = name;
			this.maxCapacity = maxCapacity;
			if (maxCapacity < 0) {
				constraintDefined = false;
			} else {
				constraintDefined = true;
			}
			this.maxMemory = maxMemory;
			if (maxMemory < 0) {
				memoryConstraintDefined = false;
			} else {
				memoryConstraintDefined = true;
			}
			this.classIndexList = classIndexList;
			this.actualOccupation = 0;
			this.actualMemoryOccupation = 0;
		}

	}

}
