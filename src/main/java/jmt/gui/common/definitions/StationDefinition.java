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
import jmt.engine.NetStrategies.ImpatienceStrategies.Retrial;
import jmt.engine.log.LoggerParameters;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.serviceStrategies.LDStrategy;

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
	 * Returns in service capacity of the station, given the search key.
	 */
	public Integer getStationMaxRunning(Object key);

	/**
	 * Sets in service capacity of the station, given the search key.
	 */
	public void setStationMaxRunning(Object key, Integer maxRunning);

	/**
	 * Gets whether server will use preemption
	 */
	public Boolean getStationServerPreemptive(Object key);

	/**
	 * Sets whether server will use preemption.
	 */
	public void setStationServerPreemptive(Object key, Boolean serverPreemption);

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
	 * Sets polling type, given the search key
	 */
	public void setStationPollingServerType(Object key, String PollingType);

	/**
	 * Returns type of polling server
	 */
	public String getStationPollingServerType(Object key);

	/**
	 * Sets k value for K polling servers
	 */

	public void setStationPollingServerKValue(Object key, Integer k);

	/**
	 * Returns k value for K polling servers
	 */

	public Integer getStationPollingServerKValue(Object key);

	/**
	 * Sets value of whether a server is polling
	 */
	public void setStationPollingServerBoolean(Object key, Boolean serverPolling);

	/**
	 * Returns value of whether a server is polling
	 */

	public Boolean getStationPollingServerBoolean(Object key);

	/**
	 * Set switchover period distribution
	 */

	public void setStationPollingSwitchoverDistribution(Object stationKey, Object classKey, Object distribution);

	/**
	 * Get switchover period distribution
	 */

	public Object getStationPollingSwitchoverDistribution(Object stationKey, Object classKey);

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
	public Object addStation(String name, String type);

	/**
	 * Deletes a station from the model, given the search key.
	 */
	public void deleteStation(Object key);

	/*------------------------------------------------------------------------------
	 *---------------- Methods for setup of class-station parameters ---------------
	 *------------------------------------------------------------------------------*/

	/**
	 * Returns queue capacity for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return integer for queue capacity.
	 */
	public Integer getQueueCapacity(Object stationKey, Object classKey);

	/**
	 * Sets queue capacity for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return queueCapacity: integer for queue capacity.
	 */
	public void setQueueCapacity(Object stationKey, Object classKey, Integer queueCapacity);

	/**
	 * Returns queue strategy for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return string name for queue strategy.
	 */
	public String getQueueStrategy(Object stationKey, Object classKey);

	/**
	 * Sets queue strategy for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
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
	 * Sets the retrial rate with given station queue section if capacity is finite.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param retrialRate: a double that dictates the time distribution of each retrial job
	 */

	/**
	 * Sets retrial rate distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param distribution: distribution to be set for specified class and station.
	 */
	public void setRetrialDistribution(Object stationKey, Object classKey, Object distribution);

	/**
	 * Returns retrial rate distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return distribution for specified class and station.
	 */
	public Object getRetrialDistribution(Object stationKey, Object classKey);

	/**
	 * Returns impatience strategy associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return BALKING || RENEGING || RETRIAL
	 */
	ImpatienceType getImpatienceType(Object stationKey, Object classKey);

	/**
	 * Sets drop rule associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return BALKING || RENEGING || RETRIAL
	 */
	void setImpatienceType(Object stationKey, Object classKey, ImpatienceType impatienceType);

	void resetImpatience(Object stationKey, Object classKey);

	ImpatienceParameter getImpatienceParameter(Object stationKey, Object classKey);

	void setImpatienceParameter(Object stationKey, Object classKey, ImpatienceParameter impatienceParameter);

	/**
	 * Update the BalkingParameter whenever the queue policy changes from non-priority to priority and vice versa.
	 */
	void updateBalkingParameter(Object stationKey, Object classKey, String serverStationQueuePolicy);

	/**
	 * Returns service weight for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return weight for specified class and station.
	 */
	public Double getServiceWeight(Object stationKey, Object classKey);

	/**
	 * Sets service weight for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param weight: weight to be set for specified class and station.
	 */
	public void setServiceWeight(Object stationKey, Object classKey, Double weight);

	/**
	 * Returns service time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return distribution for specified class and station.
	 */
	public Object getServiceTimeDistribution(Object stationKey, Object classKey);

	/**
	 * Sets service time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param distribution: distribution to be set for specified class and station.
	 */
	public void setServiceTimeDistribution(Object stationKey, Object classKey, Object distribution);

	/**
	 * Returns routing strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return routing strategy for specified class and station.
	 */
	public Object getRoutingStrategy(Object stationKey, Object classKey);

	/**
	 * Sets routing strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param routingStrategy: routing strategy to be set for specified class and station.
	 */
	public void setRoutingStrategy(Object stationKey, Object classKey, Object routingStrategy);

	/**
	 * Returns fork strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return fork strategy for specified class and station.
	 */
	public Object getForkStrategy(Object stationKey, Object classKey);

	/**
	 * Sets fork strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param forkStrategy: fork strategy to be set for specified class and station.
	 */
	public void setForkStrategy(Object stationKey, Object classKey, Object forkStrategy);

	/**
	 * Returns join strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return join strategy for specified class and station.
	 */
	public Object getJoinStrategy(Object stationKey, Object classKey);

	/**
	 * Sets join strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param joinStrategy: join strategy to be set for specified class and station.
	 */
	public void setJoinStrategy(Object stationKey, Object classKey, Object joinStrategy);

	/**
	 * Returns semaphore strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return semaphore strategy for specified class and station.
	 */
	public Object getSemaphoreStrategy(Object stationKey, Object classKey);

	/**
	 * Sets semaphore strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
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
	 * Normalizes the fork probabilities.
	 */
	public void normalizeForkProbabilities(Map<Object, Double> values);

	/*------------------------------------------------------------------------------
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

}
