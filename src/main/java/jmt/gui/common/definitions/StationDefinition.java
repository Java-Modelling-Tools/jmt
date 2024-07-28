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
import java.util.Map;
import java.util.Vector;

import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceParameter;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.log.LoggerParameters;

/**
 * Created by IntelliJ IDEA.
 * User: OrsotronIII
 * Date: 27-mag-2005
 * Time: 9.52.46
 * This interface provides methods for editing of set of stations for JSIM models.
 * Each station is assigned a search key that can be used to retrieve each parameter.
 */
public interface StationDefinition {

	/**Code for station name retrieval*/
	public static final int STATION_NAME = 0;

	/**Code for station type retrieval*/
	public static final int STATION_TYPE = 1;

	/**Code for station type retrieval*/
	public static final int STATION_QUEUE_CAPACITY = 2;

	/**Code for station type retrieval*/
	public static final int STATION_NUMBER_OF_SERVERS = 3;

	/**
	 * This method returns the key set of all stations.
	 */
	public Vector<Object> getStationKeys();

	//Michalis

	/**
	 * This method returns server type key given name and its station
	 */
	public Object getServerTypeKey(Object stationKey, String name);

	/**
	 * This method sets scheduling policy of a station
	 */
	public void setStationSchedulingPolicy(Object stationKey, String schedulingPolicy);

	/**
	 * This method returns scheduling policy of a station
	 */
	public String getStationSchedulingPolicy(Object stationKey);

	/**
	 * This method returns the number of different server types
	 */
	public int getNumberOfDifferentServerTypes(Object stationKey);

	/**
	 * This method returns all server types of station
	 */
	public List<ServerType> getServerTypes(Object StationKey);

	/**
	 * This method returns a serverType given its station key
	 * and its order amongst the server types of that station
	 */
	public ServerType getInfoForServerType(Object stationKey, int index);


	/**
	 * This method returns a server type name with respect to its station
	 * given its key
	 */
	public String getServerTypeStationName(Object serverTypeKey);

	/**
	 * This method returns Server Type given its key
	 */
	public ServerType getServerType(Object serverTypeKey);


	/**
	 * This method returns true if it is a serverTypeKey
	 */
	public boolean isServerTypeKey(Object serverTypeKey);


	/**
	 * This method adds a server type
	 */
	public void addServerType(Object StationKey, int change);

	/**
	 * This method deletes a server type
	 */
	public void deleteServerType(Object StationKey, int index);

	/**
	 * This method updates number of servers of different server types in station
	 */
	public void updateNumOfServers(Object stationKey, int newTotal);

	/**
	 * This method returns the names of different server types
	 */
	public List<String> getServerTypeNames(Object stationKey);

	public Boolean getHeterogeneousServersEnabled(Object stationKey);

	public void setHeterogeneousServersEnabled(Object stationKey, Boolean enabled);

	///

	/**
	 * This method returns the key set of server Types.
	 */
	public Vector<Object> getServerTypeKeys();

	/**
	 * This method returns the key set of sources.
	 */
	public Vector<Object> getStationKeysSource();

	/**
	 * This method returns the key set of sinks.
	 */
	public Vector<Object> getStationKeysSink();

	/**
	 * This method returns the key set of terminals.
	 */
	public Vector<Object> getStationKeysTerminal();

	/**
	 * This method returns the key set of routers.
	 */
	public Vector<Object> getStationKeysRouter();

	/**
	 * This method returns the key set of delays.
	 */
	public Vector<Object> getStationKeysDelay();

	/**
	 * This method returns the key set of servers.
	 */
	public Vector<Object> getStationKeysServer();

	/**
	 * This method returns the key set of forks.
	 */
	public Vector<Object> getStationKeysFork();

	/**
	 * This method returns the key set of joins.
	 */
	public Vector<Object> getStationKeysJoin();

	/**
	 * This method returns the key set of loggers.
	 */
	public Vector<Object> getStationKeysLogger();

	/**
	 * This method returns the key set of class switches.
	 */
	public Vector<Object> getStationKeysClassSwitch();

	/**
	 * This method returns the key set of semaphores.
	 */
	public Vector<Object> getStationKeysSemaphore();

	/**
	 * This method returns the key set of scalers.
	 */
	public Vector<Object> getStationKeysScaler();

	/**
	 * This method returns the key set of places.
	 */
	public Vector<Object> getStationKeysPlace();

	/**
	 * This method returns the key set of transitions.
	 */
	public Vector<Object> getStationKeysTransition();

	/**
	 * This method returns the key set of preloadable stations.
	 */
	public Vector<Object> getStationKeysPreloadable();

	/**
	 * This method returns all station (except sources and sinks) keys.
	 */
	public Vector<Object> getStationKeysNoSourceSink();

	/**
	 * This method returns reference station keys.
	 */
	public Vector<Object> getStationKeysRefStation();

	/**
	 * This method returns fork/join section keys.
	 */
	public Vector<Object> getFJKeys();

	/**
	 * This method returns the key set of blocking regions.
	 */
	public Vector<Object> getFCRegionKeys();

	/**
	 * This method returns all station (except sources and sinks) and blocking region keys.
	 */
	public Vector<Object> getStationRegionKeysNoSourceSink();

	/**
	 * Returns name of the station, given the search key.
	 */
	public String getStationName(Object key);

	/**
	 * Sets name of the station, given the search key.
	 */
	public void setStationName(Object key, String name);

	/**
	 * Returns type of the station, given the search key.
	 */
	public String getStationType(Object key);

	/**
	 * Sets type of the station, given the search key.
	 */
	public void setStationType(Object key, String type);

	/**
	 * Returns queue capacity of the station, given the search key.
	 */
	public Integer getStationQueueCapacity(Object key);

	/**
	 * Sets queue capacity of the station, given the search key.
	 */
	public void setStationQueueCapacity(Object key, Integer queueCapacity);

	/**
	 * Returns queue strategy of the station, given the search key.
	 */
	public String getStationQueueStrategy(Object key);

	/**
	 * Sets queue strategy of the station, given the search key.
	 */
	public void setStationQueueStrategy(Object key, String queueStrategy);

	/**
	 * Returns number of servers of the station, given the search key.
	 */
	public Integer getStationNumberOfServers(Object key);

	/**
	 * Sets number of servers of the station, given the search key.
	 */
	public void setStationNumberOfServers(Object key, Integer numberOfServers);

	/**
	 * Returns max running jobs of the station, given the search key.
	 */
	public Integer getStationMaxRunningJobs(Object key);

	/**
	 * Sets max running jobs of the station, given the search key.
	 */
	public void setStationMaxRunningJobs(Object key, Integer maxRunningJobs);

	/**
	 * Returns total capacity of the station, given the search key.
	 */
	public Integer getStationTotalCapacity(Object key);

	/**
	 * Sets total capacity of the station, given the search key.
	 */
	public void setStationTotalCapacity(Object key, Integer totalStationCapacity);

	/**
	 * Returns type of polling server, given the search key.
	 */
	public String getStationPollingServerType(Object key);

	/**
	 * Sets type of polling server, given the search key.
	 */
	public void setStationPollingServerType(Object key, String PollingType);

	/**
	 * Returns k value for K polling servers, given the search key.
	 */
	public Integer getStationPollingServerKValue(Object key);

	/**
	 * Sets k value for K polling servers, given the search key.
	 */
	public void setStationPollingServerKValue(Object key, Integer k);

	/**
	 * Gets whether setup times are enabled, given the search key.
	 */
	public Boolean getSwitchoverTimesEnabled(Object key);

	/**
	 * Sets whether setup times are enabled, given the search key.
	 */
	public void setSwitchoverTimesEnabled(Object key, Boolean enabled);

	/**
	 * Gets whether delay off times are enabled, given the search key.
	 */
	public Boolean getDelayOffTimesEnabled(Object key);

	/**
	 * Sets whether delay off times are enabled, given the search key.
	 */
	public void setDelayOffTimesEnabled(Object key, Boolean enabled);

	/**
	 * Tells if a fork is blocking
	 * <br>Author: Bertoli Marco
	 * @param key: search's key for fork
	 * @return maximum number of jobs allowed in the fork-join
	 * region (-1 is infinity)
	 */
	public Integer getForkBlock(Object key);

	/**
	 * Sets if a fork is blocking
	 * <br>Author: Bertoli Marco
	 * @param key: search's key for fork
	 * @param value: maximum number of jobs allowed in the fork-join
	 * region (-1 is infinity)
	 */
	public void setForkBlock(Object key, Integer value);

	/**
	 * Tells if a fork is simplified
	 * <br>Author: Bertoli Marco
	 * @param key: search's key for fork
	 * @return true if the fork is simplified, false otherwise
	 */
	public Boolean getIsSimplifiedFork(Object key);

	/**
	 * Sets if a fork is simplified
	 * <br>Author: Bertoli Marco
	 * @param key: search's key for fork
	 * @param value: true if the fork is simplified, false otherwise
	 */
	public void setIsSimplifiedFork(Object key, Boolean value);

	/**
	 * Adds a new station to the model, given the station name and type.
	 * @param name name of the new station.
	 * @param type type of the new station.
	 * @return search key for the new new station.
	 */
	public Object addStation(String name, String type, int nextServerNum, List<ServerType> servers);

	/**
	 * Deletes a station from the model, given the search key.
	 */
	public void deleteStation(Object key);

	/*------------------------------------------------------------------------------
	 *---------------- Methods for setup of class-station parameters ---------------
	 *------------------------------------------------------------------------------*/

	public Double getClassStationSoftDeadline(Object stationKey, Object classKey);

	public void setClassStationSoftDeadline(Object stationKey, Object classKey, Double softDeadline);

	/**
	 * Returns queue capacity for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return integer for queue capacity.
	 */
	public Integer getQueueCapacity(Object stationKey, Object classKey);

	/**
	 * Sets queue capacity for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return queueCapacity: integer for queue capacity.
	 */
	public void setQueueCapacity(Object stationKey, Object classKey, Integer queueCapacity);

	/**
	 * Returns queue strategy for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return string name for queue strategy.
	 */
	public String getQueueStrategy(Object stationKey, Object classKey);

	/**
	 * Sets queue strategy for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param queueStrategy: string name for queue strategy.
	 */
	public void setQueueStrategy(Object stationKey, Object classKey, String queueStrategy);

	/**
	 * Returns drop rule associated with given station queue section if capacity is finite.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return FINITE_DROP || FINITE_BLOCK || FINITE_WAITING || FINITE_RETRIAL
	 */
	public String getDropRule(Object stationKey, Object classKey);

	/**
	 * Sets drop rule associated with given station queue section if capacity is finite.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param dropRule: FINITE_DROP || FINITE_BLOCK || FINITE_WAITING || FINITE_RETRIAL
	 */
	public void setDropRule(Object stationKey, Object classKey, String dropRule);

	/**
	 * Returns impatience type associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return NONE || BALKING || RENEGING
	 */
	ImpatienceType getImpatienceType(Object stationKey, Object classKey);

	/**
	 * Sets impatience type associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return NONE || BALKING || RENEGING
	 */
	void setImpatienceType(Object stationKey, Object classKey, ImpatienceType impatienceType);

	/**
	 * Returns impatience parameter associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return impatience parameter for station and class.
	 */
	ImpatienceParameter getImpatienceParameter(Object stationKey, Object classKey);

	/**
	 * Sets impatience parameter associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param impatienceParameter parameter for station and class.
	 */
	void setImpatienceParameter(Object stationKey, Object classKey, ImpatienceParameter impatienceParameter);

	/**
	 * Resets impatience strategy associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 */
	void resetImpatience(Object stationKey, Object classKey);

	/**
	 * Updates balking parameter associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param serverStationQueuePolicy: queue policy for station.
	 */
	void updateBalkingParameter(Object stationKey, Object classKey, String serverStationQueuePolicy);

	/**
	 * Returns retrial rate distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return distribution for specified class and station.
	 */
	public Object getRetrialDistribution(Object stationKey, Object classKey);

	/**
	 * Sets retrial rate distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param distribution: distribution to be set for specified class and station.
	 */
	public void setRetrialDistribution(Object stationKey, Object classKey, Object distribution);

	/**
	 * Returns service weight for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return weight for specified class and station.
	 */
	public Double getServiceWeight(Object stationKey, Object classKey);

	/**
	 * Sets service weight for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param weight: weight to be set for specified class and station.
	 */
	public void setServiceWeight(Object stationKey, Object classKey, Double weight);

	/**
	 * Returns service time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return distribution for specified class and station.
	 */
	public Object getServiceTimeDistribution(Object stationKey, Object classKey);

	/**
	 * Returns service time distribution for class, station and server type, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param serverTypeKey: search key for server type.
	 * @return distribution for specified class and station.
	 */
	public Object getServiceTimeDistribution(Object stationKey, Object classKey, Object serverTypeKey);

	/**
	 * Sets service time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param distribution: distribution to be set for specified class and station.
	 */
	public void setServiceTimeDistribution(Object stationKey, Object classKey, Object distribution);

	/**
	 * Sets service time distribution for class, station and server type, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param serverTypeKey: search key for server type.
	 * @param distribution: distribution to be set for specified class and station.
	 */
	public void setServiceTimeDistribution(Object stationKey, Object classKey, Object serverTypeKey, Object distribution);

	/**
	 * Returns service time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return server number required for specified class and station.
	 */
	public Integer getServerNumRequired(Object stationKey, Object classKey);

	/**
	 * Sets service time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param serverNumRequired: server number required to be set for specified class and station.
	 */
	public void setServerNumRequired(Object stationKey, Object classKey, Integer serverNumRequired);

	/**
	 * Returns switchover period distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return distribution for specified class and station.
	 */
	public Object getPollingSwitchoverDistribution(Object stationKey, Object classKey);

	/**
	 * Sets switchover period distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param distribution: distribution to be set for specified class and station.
	 */
	public void setPollingSwitchoverDistribution(Object stationKey, Object classKey, Object distribution);

	/**
	 * Returns setup time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param fromClassKey: search key for current class.
	 * @param toClassKey: search key for next class.
	 * @return distribution for the specified class and station.
	 */
	public Object getSwitchoverTimeDistribution(Object stationKey, Object fromClassKey, Object toClassKey);

	/**
	 * Sets setup time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param fromClassKey: search key for current class from.
	 * @param toClassKey: search key for next class.
	 * @param distribution: distribution to be set for specified class and station.
	 */
	public void setSwitchoverTimeDistribution(Object stationKey, Object fromClassKey, Object toClassKey, Object distribution);

	public Object getDelayOffTimeDistribution(Object stationKey, Object classKey);

	public void setDelayOffTimeDistribution(Object stationKey, Object classKey, Object distribution);

	public Object getSetupTimeDistribution(Object stationKey, Object classKey);

	public void setSetupTimeDistribution(Object stationKey, Object classKey, Object distribution);

	/**
	 * Returns routing strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return routing strategy for specified class and station.
	 */
	public Object getRoutingStrategy(Object stationKey, Object classKey);

	/**
	 * Sets routing strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param routingStrategy: routing strategy to be set for specified class and station.
	 */
	public void setRoutingStrategy(Object stationKey, Object classKey, Object routingStrategy);

	/**
	 * Returns fork strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return fork strategy for specified class and station.
	 */
	public Object getForkStrategy(Object stationKey, Object classKey);

	/**
	 * Sets fork strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param forkStrategy: fork strategy to be set for specified class and station.
	 */
	public void setForkStrategy(Object stationKey, Object classKey, Object forkStrategy);

	/**
	 * Returns join strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return join strategy for specified class and station.
	 */
	public Object getJoinStrategy(Object stationKey, Object classKey);

	/**
	 * Sets join strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param joinStrategy: join strategy to be set for specified class and station.
	 */
	public void setJoinStrategy(Object stationKey, Object classKey, Object joinStrategy);

	/**
	 * Returns semaphore strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return semaphore strategy for specified class and station.
	 */
	public Object getSemaphoreStrategy(Object stationKey, Object classKey);

	/**
	 * Sets semaphore strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param semaphoreStrategy: semaphore strategy to be set for specified class and station.
	 */
	public void setSemaphoreStrategy(Object stationKey, Object classKey, Object semaphoreStrategy);

	/**
	 * Returns logging parameters for the logger. <I>MF'08 0.7.4</I>
	 * @param stationKey: search key for station.
	 */
	public LoggerParameters getLoggingParameters(Object stationKey);

	/**
	 * Sets logging parameters for the logger. <I>MF'08 0.7.4</I>
	 * @param stationKey: search key for station.
	 * @param loggerParameters: local LoggerParameters.
	 */
	public void setLoggingParameters(Object stationKey, LoggerParameters loggerParameters);

	/**
	 * Returns logging parameters for the Global logfile. <I>MF'08 0.7.4</I>
	 * @param selector: either "path", "delim", or "autoAppend"
	 */
	public String getLoggingGlbParameter(String selector);

	/**
	 * Sets logging parameters for the Global logfile. <I>MF'08 0.7.4</I>
	 * @param selector: either "path", "delim", "decimalSeparator", or "autoAppend"
	 * @param value: String to assign to variable named by selector.
	 */
	public void setLoggingGlbParameter(String selector, String value);

	/**
	 * Manages the routing and fork probabilities.
	 */
	public void manageProbabilities();

	/**
	 * Normalizes the routing probabilities.
	 */
	public void normalizeRoutingProbabilities(Object stationKey, Object classKey, Map<Object, Double> values);

	/**
	 * Normalizes the class switch probabilities.
	 */
	public void normalizeClassSwitchProbabilities(Object classKey, Map values, Number example);

	/**
	 * Normalizes the fork probabilities.
	 */
	public void normalizeForkProbabilities(Map<Object, Double> values);

	/*-------------------------------------------------------------------------------
	 *-------------  methods for inter-station connections definition  --------------
	 *-------------------------------------------------------------------------------*/
	/**Adds a connection between two stations in this model, given search keys of
	 * source and target stations. If connection could not be created (if, for example,
	 * target station's type is "Source")false value is returned.
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @param areConnected: true if stations must be connected, false otherwise.
	 * @return : true if connection was created, false otherwise.
	 * */
	public boolean setConnected(Object sourceKey, Object targetKey, boolean areConnected);

	/**Tells whether two stations are connected
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @return : true if stations are connected, false otherwise.
	 */
	public boolean areConnected(Object sourceKey, Object targetKey);

	/**Tells whether two stations can be connected
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @return : true if stations are connectable, false otherwise.
	 */
	public boolean areConnectable(Object sourceKey, Object targetKey);

	/**Returns a set of station keys specified station is connected to as a source.
	 * @param stationKey: source station for which (target) connected stations must be
	 * returned.
	 * @return Vector containing keys for connected stations.
	 */
	public Vector<Object> getForwardConnections(Object stationKey);

	/**Returns a set of station keys specified station is connected to as a target.
	 * @param stationKey: source station for which (source) connected stations must be
	 * returned.
	 * @return Vector containing keys for connected stations.
	 */
	public Vector<Object> getBackwardConnections(Object stationKey);

	/**
	 * @param stationKey: source station for which connected places must be returned
	 * @return Vector containing input connected places
	 */
	public Vector<Object> getBackwardConnectedPlaces(Object stationKey);

	/**
	 * Returns the search key for the station, given the station name.
	 */
	public Object getStationByName(String stationName);

	/**
	 * Returns the cell (<code>classInKey</code>, <code>classOutKey</code>) of the class
	 * switch matrix for station <code>stationKey</code>.
	 */
	public float getClassSwitchMatrix(Object stationKey, Object classInKey, Object classOutKey);

	/**
	 * Sets the cell (<code>classInKey</code>, <code>classOutKey</code>) of the class
	 * switch matrix for station <code>stationKey</code>.
	 */
	public void setClassSwitchMatrix(Object stationKey, Object classInKey, Object classOutKey, float value);

	/**
	 * Returns the threshold for semaphore <code>stationKey</code> and class <code>classKey</code>.
	 */
	public Integer getSemaphoreThreshold(Object stationKey, Object classKey);

	/**
	 * Sets the threshold for semaphore <code>stationKey</code> and class <code>classKey</code>.
	 */
	public void setSemaphoreThreshold(Object stationKey, Object classKey, Integer threshold);

	/**
	 * Returns the size of the mode list for transition <code>stationKey</code>.
	 */
	public int getTransitionModeListSize(Object stationKey);

	/**
	 * Adds a new mode for transition <code>stationKey</code>.
	 */
	public void addTransitionMode(Object stationKey, String name);

	/**
	 * Deletes a mode for transition <code>stationKey</code>.
	 */
	public void deleteTransitionMode(Object stationKey, int modeIndex);

	/**
	 * Returns the name of mode <code>modeIndex</code> for transition <code>stationKey</code>.
	 */
	public String getTransitionModeName(Object stationKey, int modeIndex);

	/**
	 * Returns the names of all the modes for transition <code>stationKey</code>.
	 */
	public List<String> getAllTransitionModeNames(Object stationKey);

	/**
	 * Sets the name of transition mode <code>modeIndex</code> for station <code>stationKey</code>.
	 */
	public void setTransitionModeName(Object stationKey, int modeIndex, String name);

	/**
	 * Returns the entry (<code>stationInKey</code>, <code>classKey</code>) of the enabling
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	public Integer getEnablingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey);

	/**
	 * Sets the entry (<code>stationInKey</code>, <code>classKey</code>) of the enabling
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	public void setEnablingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey, Integer value);

	/**
	 * Returns the entry (<code>stationInKey</code>, <code>classKey</code>) of the inhibiting
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	public Integer getInhibitingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey);

	/**
	 * Sets the entry (<code>stationInKey</code>, <code>classKey</code>) of the inhibiting
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	public void setInhibitingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey, Integer value);

	/**
	 * Returns the entry (<code>stationInKey</code>, <code>classKey</code>) of the resource
	 * condition for server <code>stationKey</code> and mode<code>modeIndex</code>
	 */
	public Integer getResourceCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey);

	/**
	 * Sets the entry (<code>stationInKey</code>, <code>classKey</code>) of the resource
	 * condition for server <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	public void setResourceCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey, Integer value);

	/**
	 * Returns the number of servers for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	public Integer getNumberOfServers(Object stationKey, int modeIndex);

	/**
	 * Sets the number of servers for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	public void setNumberOfServers(Object stationKey, int modeIndex, Integer number);

	/**
	 * Returns the firing time distribution for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	public Object getFiringTimeDistribution(Object stationKey, int modeIndex);

	/**
	 * Sets the firing time distribution for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	public void setFiringTimeDistribution(Object stationKey, int modeIndex, Object distribution);

	/**
	 * Returns the firing priority for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	public Integer getFiringPriority(Object stationKey, int modeIndex);

	/**
	 * Sets the firing priority for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	public void setFiringPriority(Object stationKey, int modeIndex, Integer priority);

	/**
	 * Returns the firing weight for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	public Double getFiringWeight(Object stationKey, int modeIndex);

	/**
	 * Sets the firing weight for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	public void setFiringWeight(Object stationKey, int modeIndex, Double weight);

	/**
	 * Returns the entry (<code>stationOutKey</code>, <code>classKey</code>) of the firing
	 * outcome for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	public Integer getFiringOutcome(Object stationKey, int modeIndex, Object stationOutKey, Object classKey);

	/**
	 * Sets the entry (<code>stationOutKey</code>, <code>classKey</code>) of the firing
	 * outcome for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	public void setFiringOutcome(Object stationKey, int modeIndex, Object stationOutKey, Object classKey, Integer value);

	public void setQuantumSize(Object stationKey, Double quantaSize);

	public Double getQuantumSize(Object stationKey);

	public void setQuantumSwitchoverTime(Object stationKey, Double switchOverTime);

	public Double getQuantumSwitchoverTime(Object stationKey);

}
