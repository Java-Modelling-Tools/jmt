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

package jmt.gui.common.definitions;

import java.util.List;
import java.util.Set;
import java.util.Vector;

import jmt.gui.common.definitions.CommonModel.BlockingGroupData;

/**
 * <p>Title: Blocking Region Definition Interface</p>
 * <p>Description: This interface will enumerate methods used to specify a blocking
 * region on the queue network.</p>
 *
 * @author Bertoli Marco
 *         Date: 26-gen-2006
 *         Time: 16.21.26
 */
public interface BlockingRegionDefinition {

	/**
	 * Adds a new blocking region to the model
	 * @param name name of the new region
	 * @param type type of the new region
	 * @return search's key for new region
	 */
	public Object addBlockingRegion(String name, String type);

	/**
	 * Deletes a blocking region from the model
	 * @param key search's key for region
	 */
	public void deleteBlockingRegion(Object key);

	/**
	 * Returns the entire set of blocking region keys
	 * @return the entire set of blocking region keys
	 */
	public Vector<Object> getRegionKeys();

	/**
	 * Gets the name of a blocking region
	 * @param regionKey search's key for region
	 * @return name of the region
	 */
	public String getRegionName(Object regionKey);

	/**
	 * Sets the name of a blocking region
	 * @param regionKey search's key for region
	 * @param name name of the region
	 */
	public void setRegionName(Object regionKey, String name);

	/**
	 * Gets the type of a blocking region
	 * @param regionKey search's key for region
	 * @return type of the region
	 */
	public String getRegionType(Object regionKey);

	/**
	 * Sets the type of a blocking region
	 * @param regionKey search's key for region
	 * @param type type of the region
	 */
	public void setRegionType(Object regionKey, String type);

	/**
	 * Gets the global customer number constraint for a blocking region
	 * @param regionKey search's key for region
	 * @return maximum number of allowed customers (-1 means infinity)
	 */
	public Integer getRegionCustomerConstraint(Object regionKey);

	/**
	 * Sets the global customer number constraint for a blocking region
	 * @param regionKey search's key for region
	 * @param maxJobs maximum number of allowed customers (-1 means infinity)
	 */
	public void setRegionCustomerConstraint(Object regionKey, Integer maxJobs);

	/**
	 * Gets the global memory size constraint for a blocking region
	 * @param regionKey search's key for region
	 * @return maximum size of allowed memory (-1 means infinity)
	 */
	public Integer getRegionMemorySize(Object regionKey);

	/**
	 * Sets the global memory size constraint for a blocking region
	 * @param regionKey search's key for region
	 * @param maxMemory maximum size of allowed memory (-1 means infinity)
	 */
	public void setRegionMemorySize(Object regionKey, Integer maxMemory);

	/**
	 * Gets the customer number constraint for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return maximum number of allowed customers for specified class (-1 means infinity)
	 */
	public Integer getRegionClassCustomerConstraint(Object regionKey, Object classKey);

	/**
	 * Sets the customer number constraint for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param maxJobs maximum number of allowed customers for specified class (-1 means infinity)
	 */
	public void setRegionClassCustomerConstraint(Object regionKey, Object classKey, Integer maxJobs);

	/**
	 * Gets the memory size constraint for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return maximum size of allowed memory for specified class (-1 means infinity)
	 */
	public Integer getRegionClassMemorySize(Object regionKey, Object classKey);

	/**
	 * Sets the memory size constraint for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param maxMemory maximum size of allowed memory for specified class (-1 means infinity)
	 */
	public void setRegionClassMemorySize(Object regionKey, Object classKey, Integer maxMemory);

	/**
	 * Gets the drop rule for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return true if jobs of specified class can be dropped, false otherwise
	 */
	public Boolean getRegionClassDropRule(Object regionKey, Object classKey);

	/**
	 * Sets the drop rule for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param drop true if jobs of specified class can be dropped, false otherwise
	 */
	public void setRegionClassDropRule(Object regionKey, Object classKey, Boolean drop);

	/**
	 * Gets the weight of each job for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return weight of each job for specified class
	 */
	public Integer getRegionClassWeight(Object regionKey, Object classKey);

	/**
	 * Sets the weight of each job for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param weight weight of each job for specified class
	 */
	public void setRegionClassWeight(Object regionKey, Object classKey, Integer weight);

	/**
	 * Gets the size of each job for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return size of each job for specified class
	 */
	public Integer getRegionClassSize(Object regionKey, Object classKey);

	/**
	 * Sets the size of each job for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param size size of each job for specified class
	 */
	public void setRegionClassSize(Object regionKey, Object classKey, Integer size);

	/**
	 * Tells if a station can be added to a blocking region
	 * (all the stations that create or destroy jobs cannot be added)
	 * @param regionKey search's key for region
	 * @param stationKey search's key for station
	 * @return true if the station can be added to specified region
	 */
	public boolean canRegionStationBeAdded(Object regionKey, Object stationKey);

	/**
	 * Adds a station to a blocking region
	 * @param regionKey search's key for region
	 * @param stationKey search's key for station
	 * @return true if the station has been added to specified region
	 */
	public boolean addRegionStation(Object regionKey, Object stationKey);

	/**
	 * Removes a station from a blocking region
	 * @param regionKey search's key for region
	 * @param stationKey search's key for station
	 */
	public void removeRegionStation(Object regionKey, Object stationKey);

	/**
	 * Returns a set of all the stations in a blocking region
	 * @param regionKey search's key for region
	 * @return a set of all the stations in specified region
	 */
	public Set<Object> getBlockingRegionStations(Object regionKey);

	/**
	 * Gets the blocking region of a station
	 * @param stationKey search's key for station
	 * @return search's key for region or null if it is undefined
	 */
	public Object getStationBlockingRegion(Object stationKey);

	/**
	 * Gets a vector of stations that can be added to a blocking region
	 * @return a vector of stations that can be added to a blocking region
	 */
	public Vector<Object> getBlockableStationKeys();

	/**
	 * Adds a new group to a blocking region
	 * @param regionKey search's key for region
	 * @param name name of the new group
	 */
	public void addRegionGroup(Object regionKey, String name);

	/**
	 * Deletes a group from a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 */
	public void deleteRegionGroup(Object regionKey,int index);

	/**
	 * Deletes all the groups from a blocking region
	 * @param regionKey search's key for region
	 */
	public void deleteAllRegionGroups(Object regionKey);

	/**
	 * Returns a list of the group data for a blocking region
	 * @param regionKey search's key for region
	 * @return a list of the group data for specified region
	 */
	public List<BlockingGroupData> getRegionGroupList(Object regionKey);

	/**
	 * Gets the name of a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @return name of the group
	 */
	public String getRegionGroupName(Object regionKey, int index);

	/**
	 * Sets the name of a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param name name of the group
	 */
	public void setRegionGroupName(Object regionKey, int index, String name);

	/**
	 * Gets the customer number constraint for a blocking region and a group
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @return maximum number of allowed customers for specified group (-1 means infinity)
	 */
	public Integer getRegionGroupCustomerConstraint(Object regionKey, int index);

	/**
	 * Sets the customer number constraint for a blocking region and a group
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param maxJobs maximum number of allowed customers for specified group (-1 means infinity)
	 */
	public void setRegionGroupCustomerConstraint(Object regionKey, int index, Integer maxJobs);

	/**
	 * Gets the memory size constraint for a blocking region and a group
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @return maximum size of allowed memory for specified group (-1 means infinity)
	 */
	public Integer getRegionGroupMemorySize(Object regionKey, int index);

	/**
	 * Sets the memory size constraint for a blocking region and a group
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param maxMemory maximum size of allowed memory for specified group (-1 means infinity)
	 */
	public void setRegionGroupMemorySize(Object regionKey, int index, Integer maxMemory);

	/**
	 * Adds a class to a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param classKey search's key for class
	 */
	public void addClassIntoRegionGroup(Object regionKey, int index, Object classKey);

	/**
	 * Removes a class from a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param classKey search's key for class
	 */
	public void removeClassFromRegionGroup(Object regionKey, int index, Object classKey);

	/**
	 * Removes all the classes from a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 */
	public void removeAllClassesFromRegionGroup(Object regionKey, int index);

	/**
	 * Returns a list of classes in a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @return a list of classes in specified group
	 */
	public List<Object> getRegionGroupClassList(Object regionKey, int index);

}
