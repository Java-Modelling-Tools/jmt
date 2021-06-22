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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import jmt.engine.NetStrategies.ImpatienceStrategies.BalkingParameter;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceParameter;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.ImpatienceStrategies.Retrial;
import jmt.engine.log.JSimLogger;
import jmt.engine.log.LoggerParameters;
import jmt.framework.data.BDMap;
import jmt.framework.data.BDMapImpl;
import jmt.framework.data.CachedHashMap;
import jmt.framework.data.MacroReplacer;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.classSwitchUtilities.ClassSwitchRow;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.common.forkStrategies.ClassSwitchFork;
import jmt.gui.common.forkStrategies.CombFork;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.forkStrategies.ProbabilitiesFork;
import jmt.gui.common.joinStrategies.GuardJoin;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.routingStrategies.LoadDependentRouting;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.serviceStrategies.ServiceStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;
import jmt.gui.common.transitionUtilities.TransitionMatrix;

/**
 * Created by IntelliJ IDEA.
 * User: OrsotronIII
 * Date: 24-mag-2005
 * Time: 9.46.43
 */
public class CommonModel implements CommonConstants, ClassDefinition, StationDefinition, SimulationDefinition, BlockingRegionDefinition {

	//key generator
	protected long incrementalKey = 0;
	protected Long seed;
	protected Boolean useRandomSeed;
	protected Double maxDuration;
	protected Double maxSimulatedTime;
	protected Integer maxSamples;
	protected Boolean disableStatistic;
	protected Integer maxEvents;
	protected Double pollingInterval;
	protected MeasureDefinition results;
	protected LoggerGlobalParameters loggerGlbParams;
	// Used to tell if model have to be saved
	protected boolean save = false;

	protected static Comparator<Object> keyComparator = new KeyComparator();
	protected static JSimLogger debugLog = JSimLogger.getRootLogger();

	/**
	 * search key set of all classes
	 */
	protected Vector<Object> classesKeyset = new Vector<Object>();

	/**
	 * search key set of open classes
	 */
	protected Vector<Object> openClassesKeyset = new Vector<Object>();

	/**
	 * search key set of closed classes
	 */
	protected Vector<Object> closedClassesKeyset = new Vector<Object>();

	/**
	 * search key set of all stations
	 */
	protected Vector<Object> stationsKeyset = new Vector<Object>();

	/**
	 * search key set of sources
	 */
	protected Vector<Object> sourcesKeyset = new Vector<Object>();

	/**
	 * search key set of sinks
	 */
	protected Vector<Object> sinksKeyset = new Vector<Object>();

	/**
	 * search key set of terminals
	 */
	protected Vector<Object> terminalsKeyset = new Vector<Object>();

	/**
	 * search key set of routers
	 */
	protected Vector<Object> routersKeyset = new Vector<Object>();

	/**
	 * search key set of delays
	 */
	protected Vector<Object> delaysKeyset = new Vector<Object>();

	/**
	 * search key set of servers
	 */
	protected Vector<Object> serversKeyset = new Vector<Object>();

	/**
	 * search key set of forks
	 */
	protected Vector<Object> forksKeyset = new Vector<Object>();

	/**
	 * search key set of joins
	 */
	protected Vector<Object> joinsKeyset = new Vector<Object>();

	/**
	 * search key set of loggers
	 */
	protected Vector<Object> loggersKeyset = new Vector<Object>();

	/**
	 * search key set of class switches
	 */
	protected Vector<Object> classSwitchesKeyset = new Vector<Object>();

	/**
	 * search key set of semaphores
	 */
	protected Vector<Object> semaphoresKeyset = new Vector<Object>();

	/**
	 * search key set of scalers
	 */
	protected Vector<Object> scalersKeyset = new Vector<Object>();

	/**
	 * search key set of places
	 */
	protected Vector<Object> placesKeyset = new Vector<Object>();

	/**
	 * search key set of transitions
	 */
	protected Vector<Object> transitionsKeyset = new Vector<Object>();

	/**
	 * search key set of preloadable stations
	 */
	protected Vector<Object> preloadableKeyset = new Vector<Object>();

	/**
	 * search key set of blocking regions
	 */
	protected Vector<Object> blockingRegionsKeyset = new Vector<Object>();

	/**
	 * search key set of blockable stations
	 */
	protected Vector<Object> blockableKeyset = new Vector<Object>();

	/**
	 * search key set of measures
	 */
	protected Vector<Object> measuresKeyset = new Vector<Object>();

	/**
	 * Hashmap for classes parameters
	 */
	protected HashMap<Object, Object> classDataHM = new CachedHashMap<Object, Object>();

	/**
	 * Hashmap containing station parameters
	 */
	protected HashMap<Object, Object> stationDataHM = new CachedHashMap<Object, Object>();

	/**
	 * Hashmap containing measure parameters
	 */
	protected HashMap<Object, MeasureData> measureDataHM = new CachedHashMap<Object, MeasureData>();

	/**
	 * Hashmap containing blocking region parameters
	 */
	protected HashMap<Object, Object> blockingDataHM = new CachedHashMap<Object, Object>();

	/**
	 * BDMap containing station connections. On X coordinate will be put stationKeys for
	 * connection targets, on Y for sources.
	 */
	protected BDMap connectionsBDM = new BDMapImpl();

	/**
	 * BDMap containing a set of station details for each station and each class.
	 * on X coordinate must be put class search keys, on Y station search keys.
	 */
	protected BDMap stationDetailsBDM = new BDMapImpl();


	/**
	 * BDMap containing a set of blocking region details for each blocking region and each class.
	 * on X coordinate must be put class search keys, on Y blocking region search keys.
	 */
	protected BDMap blockingDetailsBDM = new BDMapImpl();

	// ------------------ Francesco D'Aquino ----------------------
	private ParametricAnalysisDefinition parametricAnalysisModel;
	private boolean parametricAnalysisEnabled;
	// -------------- end Francesco D'Aquino ----------------------

	/**
	 * Tells if current model is to be saved
	 * @return true if model is to be saved
	 */
	public boolean toBeSaved() {
		return save;
	}

	/**
	 * Resets save state. This MUST be called each time a model is saved
	 */
	public void resetSaveState() {
		save = false;
	}

	/**Creates a new instance of <code>CommonModel</code>*/
	public CommonModel() {
		seed = Defaults.getAsLong("simulationSeed");
		useRandomSeed = Defaults.getAsBoolean("isSimulationSeedRandom");
		maxDuration = Defaults.getAsDouble("simulationMaxDuration");
		maxSimulatedTime = Defaults.getAsDouble("maxSimulatedTime");
		maxSamples = Defaults.getAsInteger("maxSimulationSamples");
		disableStatistic = Defaults.getAsBoolean("isStatisticDisabled");
		maxEvents = Defaults.getAsInteger("maxSimulationEvents");
		pollingInterval = Defaults.getAsDouble("simulationPolling");
		//TODO: eventually load the parametricAnalysisEnabled state from default
		parametricAnalysisEnabled = false;
	}

	//model class definition methods
	/**
	 * This method returns the key set of all classes.
	 */
	@Override
	public Vector<Object> getClassKeys() {
		return classesKeyset;
	}

	/**
	 * This method returns the key set of open classes.
	 */
	@Override
	public Vector<Object> getOpenClassKeys() {
		return openClassesKeyset;
	}

	/**
	 * This method returns the key set of closed classes.
	 */
	@Override
	public Vector<Object> getClosedClassKeys() {
		return closedClassesKeyset;
	}

	/**
	 * Returns name of the class, given the search key.
	 */
	@Override
	public String getClassName(Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return null;
		}
		return cd.name;
	}

	/**
	 * Sets name of the class, given the search key.
	 */
	@Override
	public void setClassName(Object key, String name) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return;
		}
		if ((cd.name == null || !cd.name.equals(name)) && !name.equals("")) {
			cd.name = getUniqueClassName(name);
			save = true;
		}
	}

	/**
	 * Returns type of the class, given the search key.
	 */
	@Override
	public int getClassType(Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return -1;
		}
		return cd.type;
	}

	/**
	 * Sets type of the class, given the search key.
	 */
	@Override
	public void setClassType(Object key, int type) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = ((ClassData) classDataHM.get(key));
		} else {
			return;
		}

		if (cd.type != type) {
			cd.type = type;
			if (type == CLASS_TYPE_OPEN) {
				cd.population = null;
				cd.distribution = Defaults.getAsNewInstance("classDistribution");
				cd.refStation = null;
				closedClassesKeyset.remove(key);
				openClassesKeyset.add(key);
				Collections.sort(openClassesKeyset, keyComparator);
			} else {
				cd.population = Defaults.getAsInteger("classPopulation");
				cd.distribution = null;
				cd.refStation = null;
				openClassesKeyset.remove(key);
				closedClassesKeyset.add(key);
				Collections.sort(closedClassesKeyset, keyComparator);
			}
			deleteStationDetailsForClass(key);
			deleteBlockingDetailsForClass(key);
			deleteClassMeasures(key);
			addStationDetailsForClass(key);
			addBlockingDetailsForClass(key);
			save = true;
		}
	}

	/**
	 * Returns priority of the class, given the search key.
	 */
	@Override
	public Integer getClassPriority(Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return null;
		}
		return cd.priority;
	}

	/**
	 * Sets priority of the class, given the search key.
	 */
	@Override
	public void setClassPriority(Object key, Integer priority) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return;
		}
		if (cd.priority == null || !cd.priority.equals(priority)) {
			cd.priority = priority;
			save = true;
		}
	}

	/**
	 * Returns population of the class, given the search key.
	 */
	@Override
	public Integer getClassPopulation(Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return null;
		}
		return cd.population;
	}

	/**
	 * Sets population of the class, given the search key.
	 */
	@Override
	public void setClassPopulation(Object key, Integer population) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return;
		}
		if (cd.population == null || !cd.population.equals(population)) {
			cd.population = population;
			save = true;
		}
	}

	/**
	 * Returns inter-arrival time distribution of the class, given the search key.
	 */
	@Override
	public Object getClassDistribution(Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return null;
		}
		return cd.distribution;
	}

	/**
	 * Sets inter-arrival time distribution of the class, given the search key.
	 */
	@Override
	public void setClassDistribution(Object key, Object distribution) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return;
		}
		if (cd.distribution == null || !cd.distribution.equals(distribution)) {
			cd.distribution = distribution;
			save = true;
		}
	}


	/**
	 * Returns reference station of the class, given the search key.
	 */
	@Override
	public Object getClassRefStation(Object key) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return null;
		}
		return cd.refStation;
	}

	/**
	 * Sets reference station of the class, given the search key.
	 */
	@Override
	public void setClassRefStation(Object key, Object refStation) {
		ClassData cd;
		if (classDataHM.containsKey(key)) {
			cd = (ClassData) classDataHM.get(key);
		} else {
			return;
		}
		if (cd.refStation == null || !cd.refStation.equals(refStation)) {
			cd.refStation = refStation;
			save = true;
			if (cd.type == CLASS_TYPE_OPEN) {
				if (cd.distribution == null && (refStation == null
						|| getStationType(refStation).equals(STATION_TYPE_SOURCE))) {
					cd.distribution = Defaults.getAsNewInstance("classDistribution");
				}
				if (cd.distribution != null && (STATION_TYPE_FORK.equals(refStation)
						|| STATION_TYPE_CLASSSWITCH.equals(refStation)
						|| STATION_TYPE_SCALER.equals(refStation)
						|| STATION_TYPE_TRANSITION.equals(refStation))) {
					cd.distribution = null;
				}
			}
		}
	}

	/**
	 * Adds a new class to the model and sets all class parameters at once.
	 * @param name name of the new class.
	 * @param type type of the new class.
	 * @param priority priority of the new class.
	 * @param population population of the new class.
	 * @param distribution inter-arrival time distribution of the new class.
	 * @return search key for the new class.
	 */
	@Override
	public Object addClass(String name, int type, Integer priority, Integer population, Object distribution) {
		name = getUniqueClassName(name);
		Object key = new Long(++incrementalKey);
		classesKeyset.add(key);
		ClassData cd;
		if (type == CLASS_TYPE_OPEN) {
			cd = new ClassData(name, type, priority, null, distribution);
			openClassesKeyset.add(key);
		} else {
			cd = new ClassData(name, type, priority, population, null);
			closedClassesKeyset.add(key);
		}
		classDataHM.put(key, cd);
		addStationDetailsForClass(key);
		addBlockingDetailsForClass(key);
		save = true;
		return key;
	}

	/**
	 * Deletes a class from the model, given the search key.
	 */
	@Override
	public void deleteClass(Object key) {
		if (classesKeyset.contains(key)) {
			deleteStationDetailsForClass(key);
			deleteBlockingDetailsForClass(key);
			deleteClassMeasures(key);
			classDataHM.remove(key);
			classesKeyset.remove(key);
			openClassesKeyset.remove(key);
			closedClassesKeyset.remove(key);
			save = true;
		}
	}

	/*-------------------------------------------------------------------------------
	 *----------------------  methods for station definition  -----------------------
	 *-------------------------------------------------------------------------------*/

	/**
	 * This method returns the key set of all stations.
	 */
	@Override
	public Vector<Object> getStationKeys() {
		return stationsKeyset;
	}

	/**
	 * This method returns the key set of sources.
	 */
	@Override
	public Vector<Object> getStationKeysSource() {
		return sourcesKeyset;
	}

	/**
	 * This method returns the key set of sinks.
	 */
	@Override
	public Vector<Object> getStationKeysSink() {
		return sinksKeyset;
	}

	/**
	 * This method returns the key set of terminals.
	 */
	@Override
	public Vector<Object> getStationKeysTerminal() {
		return terminalsKeyset;
	}

	/**
	 * This method returns the key set of routers.
	 */
	@Override
	public Vector<Object> getStationKeysRouter() {
		return routersKeyset;
	}

	/**
	 * This method returns the key set of delays.
	 */
	@Override
	public Vector<Object> getStationKeysDelay() {
		return delaysKeyset;
	}

	/**
	 * This method returns the key set of servers.
	 */
	@Override
	public Vector<Object> getStationKeysServer() {
		return serversKeyset;
	}

	/**
	 * This method returns the key set of forks.
	 */
	@Override
	public Vector<Object> getStationKeysFork() {
		return forksKeyset;
	}

	/**
	 * This method returns the key set of joins.
	 */
	@Override
	public Vector<Object> getStationKeysJoin() {
		return joinsKeyset;
	}

	/**
	 * This method returns the key set of loggers.
	 */
	@Override
	public Vector<Object> getStationKeysLogger() {
		return loggersKeyset;
	}

	/**
	 * This method returns the key set of class switches.
	 */
	@Override
	public Vector<Object> getStationKeysClassSwitch() {
		return classSwitchesKeyset;
	}

	/**
	 * This method returns the key set of semaphores.
	 */
	@Override
	public Vector<Object> getStationKeysSemaphore() {
		return semaphoresKeyset;
	}

	/**
	 * This method returns the key set of scalers.
	 */
	@Override
	public Vector<Object> getStationKeysScaler() {
		return scalersKeyset;
	}

	/**
	 * This method returns the key set of places.
	 */
	@Override
	public Vector<Object> getStationKeysPlace() {
		return placesKeyset;
	}

	/**
	 * This method returns the key set of transitions.
	 */
	@Override
	public Vector<Object> getStationKeysTransition() {
		return transitionsKeyset;
	}

	/**
	 * This method returns the key set of preloadable stations.
	 */
	@Override
	public Vector<Object> getStationKeysPreloadable() {
		return preloadableKeyset;
	}

	/**
	 * This method returns all station (except sources and sinks) keys.
	 */
	@Override
	public Vector<Object> getStationKeysNoSourceSink() {
		Vector<Object> keyset = new Vector<Object>();
		keyset.addAll(stationsKeyset);
		keyset.removeAll(sourcesKeyset);
		keyset.removeAll(sinksKeyset);
		return keyset;
	}

	/**
	 * This method returns reference station keys.
	 */
	@Override
	public Vector<Object> getStationKeysRefStation() {
		Vector<Object> keyset = new Vector<Object>();
		keyset.addAll(sourcesKeyset);
		if (!forksKeyset.isEmpty()) {
			keyset.add(STATION_TYPE_FORK);
		}
		if (!classSwitchesKeyset.isEmpty()) {
			keyset.add(STATION_TYPE_CLASSSWITCH);
		}
		if (!scalersKeyset.isEmpty()) {
			keyset.add(STATION_TYPE_SCALER);
		}
		if (!transitionsKeyset.isEmpty()) {
			keyset.add(STATION_TYPE_TRANSITION);
		}
		return keyset;
	}

	/**
	 * This method returns fork/join section keys.
	 */
	@Override
	public Vector<Object> getFJKeys() {
		Vector<Object> keyset = new Vector<Object>();
		keyset.addAll(forksKeyset);
		keyset.addAll(scalersKeyset);
		return keyset;
	}

	/**
	 * This method returns the key set of blocking regions.
	 */
	@Override
	public Vector<Object> getFCRegionKeys() {
		return blockingRegionsKeyset;
	}

	/**
	 * This method returns all station (except sources and sinks) and blocking region keys.
	 */
	@Override
	public Vector<Object> getStationRegionKeysNoSourceSink() {
		Vector<Object> keyset = new Vector<Object>();
		keyset.addAll(stationsKeyset);
		keyset.removeAll(sourcesKeyset);
		keyset.removeAll(sinksKeyset);
		keyset.addAll(blockingRegionsKeyset);
		return keyset;
	}

	/**
	 * Returns name of the station, given the search key.
	 */
	@Override
	public String getStationName(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.name;
	}

	/**
	 * Sets name of the station, given the search key.
	 */
	@Override
	public void setStationName(Object key, String name) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if ((sd.name == null || !sd.name.equals(name)) && !name.equals("")) {
			sd.name = getUniqueStationName(name);
			save = true;
		}
	}

	/**
	 * Returns type of the station, given the search key.
	 */
	@Override
	public String getStationType(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.type;
	}

	/**
	 * Sets type of the station, given the search key.
	 */
	@Override
	public void setStationType(Object key, String type) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}

		if (sd.type == null || !sd.type.equals(type)) {
			if (sd.blockingRegion != null) {
				removeRegionStation(sd.blockingRegion, key);
			}
			deleteStationConnections(key);
			deleteStationMeasures(key);
			sd = new StationData(sd.name, type);
			stationDataHM.put(key, sd);
			setDefaultsForStation(sd);
			addStationDetails(key);
			addStationConnections(key);
			if (type.equals(STATION_TYPE_SOURCE)) {
				sourcesKeyset.add(key);
				Collections.sort(sourcesKeyset, keyComparator);
			} else {
				sourcesKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_SINK)) {
				sinksKeyset.add(key);
				Collections.sort(sinksKeyset, keyComparator);
			} else {
				sinksKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_TERMINAL)) {
				terminalsKeyset.add(key);
				Collections.sort(terminalsKeyset, keyComparator);
			} else {
				terminalsKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_ROUTER)) {
				routersKeyset.add(key);
				Collections.sort(routersKeyset, keyComparator);
			} else {
				routersKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_DELAY)) {
				delaysKeyset.add(key);
				Collections.sort(delaysKeyset, keyComparator);
			} else {
				delaysKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_SERVER)) {
				serversKeyset.add(key);
				Collections.sort(serversKeyset, keyComparator);
			} else {
				serversKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_FORK)) {
				forksKeyset.add(key);
				Collections.sort(forksKeyset, keyComparator);
			} else {
				forksKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_JOIN)) {
				joinsKeyset.add(key);
				Collections.sort(joinsKeyset, keyComparator);
			} else {
				joinsKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_LOGGER)) {
				loggersKeyset.add(key);
				Collections.sort(loggersKeyset, keyComparator);
			} else {
				loggersKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_CLASSSWITCH)) {
				classSwitchesKeyset.add(key);
				Collections.sort(classSwitchesKeyset, keyComparator);
			} else {
				classSwitchesKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_SEMAPHORE)) {
				semaphoresKeyset.add(key);
				Collections.sort(semaphoresKeyset, keyComparator);
			} else {
				semaphoresKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_SCALER)) {
				scalersKeyset.add(key);
				Collections.sort(scalersKeyset, keyComparator);
			} else {
				scalersKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_PLACE)) {
				placesKeyset.add(key);
				Collections.sort(placesKeyset, keyComparator);
			} else {
				placesKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_TRANSITION)) {
				transitionsKeyset.add(key);
				Collections.sort(transitionsKeyset, keyComparator);
			} else {
				transitionsKeyset.remove(key);
			}
			if (isStationTypePreloadable(type)) {
				if (!preloadableKeyset.contains(key)) {
					preloadableKeyset.add(key);
					Collections.sort(preloadableKeyset, keyComparator);
				}
			} else {
				preloadableKeyset.remove(key);
			}
			if (canStationTypeBeBlocked(type)) {
				if (!blockableKeyset.contains(key)) {
					blockableKeyset.add(key);
					Collections.sort(blockableKeyset, keyComparator);
				}
			} else {
				blockableKeyset.remove(key);
			}
			manageRefStations();
			save = true;
		}
	}

	/**
	 * Returns queue capacity of the station, given the search key.
	 */
	@Override
	public Integer getStationQueueCapacity(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.queueCapacity;
	}

	/**
	 * Sets queue capacity of the station, given the search key.
	 */
	@Override
	public void setStationQueueCapacity(Object key, Integer queueCapacity) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (sd.queueCapacity == null || !sd.queueCapacity.equals(queueCapacity)) {
			sd.queueCapacity = queueCapacity;
			save = true;
		}
	}

	/**
	 * Returns queue capacity of the station, given the search key.
	 */
	@Override
	public Integer getStationMaxRunning(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.maxRunning;
	}

	/**
	 * Sets in service capacity of the station, given the search key.
	 */
	@Override
	public void setStationMaxRunning(Object key, Integer maxRunning) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (sd.maxRunning == null || !sd.maxRunning.equals(maxRunning)) {
			sd.maxRunning = maxRunning;
			save = true;
		}
	}

	@Override
	public void setStationServerPreemptive(Object key, Boolean serverPreemptive) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (!sd.serverPreemptive == serverPreemptive) {
			sd.serverPreemptive = serverPreemptive;
			save = true;
		}
	}
	@Override
	public Boolean getStationServerPreemptive(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return false;
		}
		return sd.serverPreemptive;
	}
	/**
	 * Returns queue strategy of the station, given the search key.
	 */
	@Override
	public String getStationQueueStrategy(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.queueStrategy;
	}

	/**
	 * Sets queue strategy of the station, given the search key.
	 */
	@Override
	public void setStationQueueStrategy(Object key, String queueStrategy) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (sd.queueStrategy == null || !sd.queueStrategy.equals(queueStrategy)) {
			sd.queueStrategy = queueStrategy;
			save = true;
		}
	}

	/**
	 * Returns number of servers of the station, given the search key.
	 */
	@Override
	public Integer getStationNumberOfServers(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.numberOfServers;
	}

	/**
	 * Sets number of servers of the station, given the search key.
	 */
	@Override
	public void setStationNumberOfServers(Object key, Integer numberOfServers) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (sd.numberOfServers == null || !sd.numberOfServers.equals(numberOfServers)) {
			sd.numberOfServers = numberOfServers;
			save = true;
		}
	}

	/**
	 * Sets the polling type of the station, given the search key.
	 */

	@Override
	public void setStationPollingServerType(Object key, String pollingType) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}

		if (sd.pollingServerType == null || !sd.pollingServerType.equals(pollingType)) {
			sd.pollingServerType = pollingType;
			save = true;
		}
	}

	@Override
	public String getStationPollingServerType(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.pollingServerType;
	}

	@Override
	public void setStationPollingServerBoolean(Object key, Boolean serverPolling) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}

		if (sd.serverPolling == null || !sd.serverPolling.equals(serverPolling)) {
			sd.serverPolling = serverPolling;
			save = true;
		}
	}


	@Override
	public Boolean getStationPollingServerBoolean(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.serverPolling;
	}

	@Override
	public void setStationPollingServerKValue(Object key, Integer k) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}

		if (sd.pollingKValue == null || !sd.pollingKValue.equals(k)) {
			sd.pollingKValue = k;
			save = true;
		}

	}

	@Override
	public Integer getStationPollingServerKValue(Object key) {

		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.pollingKValue;
	}

	@Override
	public void setStationPollingSwitchoverDistribution(Object stationKey, Object classKey, Object distribution) {
		StationData sd;
		if (stationDataHM.containsKey(stationKey)) {
			sd = (StationData) stationDataHM.get(stationKey);
			if (sd.switchoverPeriods == null) {
				sd.switchoverPeriods = new HashMap<>();
			}
		} else {
			return;
		}

		if ( !sd.switchoverPeriods.containsKey(classKey)
				|| !sd.switchoverPeriods.get(classKey).equals(distribution)) {

			sd.switchoverPeriods.put(classKey, distribution);
			save = true;
		}
	}

	@Override
	public Object getStationPollingSwitchoverDistribution(Object stationKey, Object classKey) {
		StationData sd;
		if (stationDataHM.containsKey(stationKey)) {
			sd = (StationData) stationDataHM.get(stationKey);
			if (sd.switchoverPeriods == null)
				sd.switchoverPeriods = new HashMap<>();

			updatePollingServerSwitchoverDistribution(stationKey);
		} else {
			return null;
		}

		return sd.switchoverPeriods.get(classKey);
	}

	protected void updatePollingServerSwitchoverDistribution(Object stationKey) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		Set<Object> switchoverKeys = new HashSet<>(sd.switchoverPeriods.keySet());
		for (Object classKey : classesKeyset) {
			if (switchoverKeys.contains(classKey)) {
				switchoverKeys.remove(classKey);
			} else {
				sd.switchoverPeriods.put(classKey, new ZeroStrategy());
			}
		}

		for (Object key: switchoverKeys) {
			sd.switchoverPeriods.remove(key);
		}

	}



	/**
	 * Tells if a fork is blocking
	 * <br>Author: Bertoli Marco
	 * @param key: search's key for fork
	 * @return maximum number of jobs allowed in the fork-join
	 * region (-1 is infinity)
	 */
	@Override
	public Integer getForkBlock(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.forkBlock;
	}

	/**
	 * Sets if a fork is blocking
	 * <br>Author: Bertoli Marco
	 * @param key: search's key for fork
	 * @param value: maximum number of jobs allowed in the fork-join
	 * region (-1 is infinity)
	 */
	@Override
	public void setForkBlock(Object key, Integer value) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (sd.forkBlock == null || !sd.forkBlock.equals(value)) {
			sd.forkBlock = value;
			save = true;
		}
	}

	/**
	 * Tells if a fork is simplified
	 * <br>Author: Bertoli Marco
	 * @param key: search's key for fork
	 * @return true if the fork is simplified, false otherwise
	 */
	@Override
	public Boolean getIsSimplifiedFork(Object key) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return null;
		}
		return sd.isSimplifiedFork;
	}

	/**
	 * Sets if a fork is simplified
	 * <br>Author: Bertoli Marco
	 * @param key: search's key for fork
	 * @param value: true if the fork is simplified, false otherwise
	 */
	@Override
	public void setIsSimplifiedFork(Object key, Boolean value) {
		StationData sd;
		if (stationDataHM.containsKey(key)) {
			sd = (StationData) stationDataHM.get(key);
		} else {
			return;
		}
		if (sd.isSimplifiedFork == null || !sd.isSimplifiedFork.equals(value)) {
			sd.isSimplifiedFork = value;
			save = true;
		}
	}

	/**
	 * Adds a new station to the model, given the station name and type.
	 * @param name name of the new station.
	 * @param type type of the new station.
	 * @return search key for the new station.
	 */
	@Override
	public Object addStation(String name, String type) {
		name = getUniqueStationName(name);
		Object key = new Long(++incrementalKey);
		stationsKeyset.add(key);
		StationData sd = new StationData(name, type);
		stationDataHM.put(key, sd);
		setDefaultsForStation(sd);
		addStationDetails(key);
		addStationConnections(key);
		if (type.equals(STATION_TYPE_SOURCE)) {
			sourcesKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_SINK)) {
			sinksKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_TERMINAL)) {
			terminalsKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_ROUTER)) {
			routersKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_DELAY)) {
			delaysKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_SERVER)) {
			serversKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_FORK)) {
			forksKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_JOIN)) {
			joinsKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_LOGGER)) {
			loggersKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_CLASSSWITCH)) {
			classSwitchesKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_SEMAPHORE)) {
			semaphoresKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_SCALER)) {
			scalersKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_PLACE)) {
			placesKeyset.add(key);
		}
		if (type.equals(STATION_TYPE_TRANSITION)) {
			transitionsKeyset.add(key);
		}
		if (isStationTypePreloadable(type)) {
			preloadableKeyset.add(key);
		}
		if (canStationTypeBeBlocked(type)) {
			blockableKeyset.add(key);
		}
		save = true;
		return key;
	}

	/**
	 * Deletes a station from the model, given the search key.
	 */
	@Override
	public void deleteStation(Object key) {
		if (stationsKeyset.contains(key)) {
			StationData sd = (StationData) stationDataHM.get(key);
			if (sd.blockingRegion != null) {
				removeRegionStation(sd.blockingRegion, key);
			}
			deleteStationConnections(key);
			deleteStationMeasures(key);
			stationDataHM.remove(key);
			deleteStationDetails(key);
			stationsKeyset.remove(key);
			sourcesKeyset.remove(key);
			sinksKeyset.remove(key);
			terminalsKeyset.remove(key);
			routersKeyset.remove(key);
			delaysKeyset.remove(key);
			serversKeyset.remove(key);
			forksKeyset.remove(key);
			joinsKeyset.remove(key);
			loggersKeyset.remove(key);
			classSwitchesKeyset.remove(key);
			semaphoresKeyset.remove(key);
			scalersKeyset.remove(key);
			placesKeyset.remove(key);
			transitionsKeyset.remove(key);
			preloadableKeyset.remove(key);
			blockableKeyset.remove(key);
			manageRefStations();
			save = true;
		}
	}

	/**
	 * Sets station parameters by default.
	 */
	private void setDefaultsForStation(StationData sd) {
		if (sd.type.equals(STATION_TYPE_SOURCE)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_SINK)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_TERMINAL)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_ROUTER)) {
			sd.queueCapacity = Integer.valueOf(-1);
			sd.maxRunning = Integer.valueOf(-1);
			sd.queueStrategy = STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
			sd.serverPreemptive = false;
		} else if (sd.type.equals(STATION_TYPE_DELAY)) {
			sd.queueCapacity = Integer.valueOf(-1);
			sd.maxRunning = Integer.valueOf(-1);
			sd.queueStrategy = STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
			sd.numberOfServers = Integer.valueOf(-1);
			sd.serverPreemptive = false;
		} else if (sd.type.equals(STATION_TYPE_SERVER)) {
			sd.queueCapacity = Defaults.getAsInteger("stationCapacity");
			sd.maxRunning = Integer.valueOf(-1);
			sd.queueStrategy = Defaults.get("stationStationQueueStrategy");
			sd.numberOfServers = Defaults.getAsInteger("stationServers");
			sd.serverPreemptive = false;
			sd.serverPolling = false;
			sd.pollingKValue = 1;
			sd.pollingServerType = STATION_QUEUE_STRATEGY_POLLING_LIMITED;
			sd.switchoverPeriods = new HashMap<>();
		} else if (sd.type.equals(STATION_TYPE_FORK)) {
			sd.queueCapacity = Defaults.getAsInteger("stationCapacity");
			sd.maxRunning = Integer.valueOf(-1);
			sd.queueStrategy = Defaults.get("stationStationQueueStrategy");
			if (sd.queueStrategy.equals(STATION_QUEUE_STRATEGY_PSSERVER)
					|| sd.queueStrategy.equals(STATION_QUEUE_STRATEGY_PREEMPTIVE)
					|| sd.queueStrategy.equals(STATION_QUEUE_STRATEGY_POLLING)) {
				sd.queueStrategy = STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
			}
			sd.serverPreemptive = false;
			sd.forkBlock = Defaults.getAsInteger("forkBlock");
			sd.isSimplifiedFork = Boolean.parseBoolean(Defaults.get("isSimplifiedFork"));
			sd.numberOfServers = Defaults.getAsInteger("forkJobsPerLink");
		} else if (sd.type.equals(STATION_TYPE_JOIN)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_LOGGER)) {
			sd.queueCapacity = Integer.valueOf(-1);
			sd.maxRunning = Integer.valueOf(-1);
			sd.serverPreemptive = false;
			sd.queueStrategy = STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
		} else if (sd.type.equals(STATION_TYPE_CLASSSWITCH)) {
			sd.queueCapacity = Integer.valueOf(-1);
			sd.maxRunning = Integer.valueOf(-1);
			sd.serverPreemptive = false;
			sd.queueStrategy = STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
		} else if (sd.type.equals(STATION_TYPE_SEMAPHORE)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_SCALER)) {
			sd.forkBlock = Integer.valueOf(-1);
			sd.serverPreemptive = false;
			sd.isSimplifiedFork = Boolean.parseBoolean(Defaults.get("isSimplifiedFork"));
			sd.numberOfServers = Defaults.getAsInteger("forkJobsPerLink");
		} else if (sd.type.equals(STATION_TYPE_PLACE)) {
			sd.queueCapacity = Defaults.getAsInteger("placeCapacity");
			sd.serverPreemptive = false;
			sd.maxRunning = Integer.valueOf(-1);
			sd.queueStrategy = STATION_QUEUE_STRATEGY_NON_PREEMPTIVE;
		} else if (sd.type.equals(STATION_TYPE_TRANSITION)) {
			sd.transitionModeList = new ArrayList<TransitionModeData>();
			sd.transitionModeList.add(new TransitionModeData(Defaults.get("transitionModeName") + 1));
		}
	}

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
	@Override
	public Integer getQueueCapacity(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.queueCapacity;
	}

	/**
	 * Sets queue capacity for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return queueCapacity: integer for queue capacity.
	 */
	@Override
	public void setQueueCapacity(Object stationKey, Object classKey, Integer queueCapacity) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.queueCapacity == null || !scd.queueCapacity.equals(queueCapacity)) {
			scd.queueCapacity = queueCapacity;
			save = true;
		}
	}

	/**
	 * Returns queue strategy for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return string name for queue strategy.
	 */
	@Override
	public String getQueueStrategy(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.queueStrategy;
	}

	/**
	 * Sets queue strategy for a station and a class, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param queueStrategy: string name for queue strategy.
	 */
	@Override
	public void setQueueStrategy(Object stationKey, Object classKey, String queueStrategy) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.queueStrategy == null || !scd.queueStrategy.equals(queueStrategy)) {
			scd.queueStrategy = queueStrategy;
			save = true;
		}
	}

	/**
	 * Returns drop rule associated with given station queue section if capacity is finite.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return FINITE_DROP || FINITE_BLOCK || FINITE_WAITING || FINITE_RETRIAL
	 */
	@Override
	public String getDropRule(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.dropRule;
	}

	/**
	 * Sets drop rule associated with given station queue section if capacity is finite.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param dropRule: FINITE_DROP || FINITE_BLOCK || FINITE_WAITING || FINITE_RETRIAL
	 */
	@Override
	public void setDropRule(Object stationKey, Object classKey, String dropRule) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.dropRule == null || !scd.dropRule.equals(dropRule)) {
			scd.dropRule = dropRule;
			save = true;
		}
	}


	@Override
	public void setRetrialDistribution(Object stationKey, Object classKey, Object distribution) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.retrialDistribution == null || !scd.retrialDistribution.equals(distribution)) {
			scd.retrialDistribution = distribution;
			save = true;
		}
	}

	@Override
	public Object getRetrialDistribution(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}

		return scd.retrialDistribution;
	}

	/**
	 * Returns service weight for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return weight for specified class and station.
	 */
	@Override
	public Double getServiceWeight(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.serviceWeight;
	}

	/**
	 * Sets service weight for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param weight: weight to be set for specified class and station.
	 */
	@Override
	public void setServiceWeight(Object stationKey, Object classKey, Double weight) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.serviceWeight == null || !scd.serviceWeight.equals(weight)) {
			scd.serviceWeight = weight;
			save = true;
		}
	}

	/**
	 * Returns impatience strategy associated with given station queue section.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return BALKING || RENEGING || RETRIAL
	 */
	@Override
	public ImpatienceType getImpatienceType(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.impatienceType;
	}

	/**
	 * Sets drop rule associated with given station queue section if capacity is finite.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param impatienceType: BALKING || RENEGING || RETRIAL
	 */
	@Override
	public void setImpatienceType(Object stationKey, Object classKey, ImpatienceType impatienceType) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.impatienceType == null || !scd.impatienceType.equals(impatienceType)) {
			scd.impatienceType = impatienceType;
			save = true;
		}
	}

	@Override
  public void resetImpatience(Object stationKey, Object classKey) {
	  clearImpatienceType(stationKey, classKey);
	  clearImpatienceParameter(stationKey, classKey);
  }

	private void clearImpatienceType(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		scd.impatienceType = null;
	}

	private void clearImpatienceParameter(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		scd.impatienceParameter = null;
	}

	@Override
	public ImpatienceParameter getImpatienceParameter(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.impatienceParameter;
	}

	@Override
	public void setImpatienceParameter(Object stationKey, Object classKey, ImpatienceParameter impatienceParameter) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		scd.impatienceParameter = impatienceParameter;
		save = true;
	}

	@Override
	public void updateBalkingParameter(Object stationKey, Object classKey,	String serverStationQueuePolicy) {
		ImpatienceParameter impatienceParameter = getImpatienceParameter(stationKey, classKey);

		// We need to verify if the impatienceParameter is of type BalkingParameter
		if (impatienceParameter instanceof BalkingParameter) {
			String classQueueStrategy = getQueueStrategy(stationKey, classKey);
			((BalkingParameter) impatienceParameter).updatePriority(serverStationQueuePolicy, classQueueStrategy);
			save = true;
		}
	}

	/**
	 * Returns service time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return distribution for specified class and station.
	 */
	@Override
	public Object getServiceTimeDistribution(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.serviceDistribution;
	}

	/**
	 * Sets service time distribution for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param distribution: distribution to be set for specified class and station.
	 */
	@Override
	public void setServiceTimeDistribution(Object stationKey, Object classKey, Object distribution) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.serviceDistribution == null || !scd.serviceDistribution.equals(distribution)) {
			scd.serviceDistribution = distribution;
			save = true;
		}
	}
	
	/**
	 * Returns routing strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return routing strategy for specified class and station.
	 */
	@Override
	public Object getRoutingStrategy(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.routingStrategy;
	}

	/**
	 * Sets routing strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param routingStrategy: routing strategy to be set for specified class and station.
	 */
	@Override
	public void setRoutingStrategy(Object stationKey, Object classKey, Object routingStrategy) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.routingStrategy == null || !scd.routingStrategy.equals(routingStrategy)) {
			scd.routingStrategy = routingStrategy;
			save = true;
		}
	}

	/**
	 * Returns fork strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return fork strategy for specified class and station.
	 */
	@Override
	public Object getForkStrategy(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.forkStrategy;
	}

	/**
	 * Sets fork strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param forkStrategy: fork strategy to be set for specified class and station.
	 */
	@Override
	public void setForkStrategy(Object stationKey, Object classKey, Object forkStrategy) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.forkStrategy == null || !scd.forkStrategy.equals(forkStrategy)) {
			scd.forkStrategy = forkStrategy;
			save = true;
		}
	}

	/**
	 * Returns join strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return join strategy for specified class and station.
	 */
	@Override
	public Object getJoinStrategy(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.joinStrategy;
	}

	/**
	 * Sets join strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param joinStrategy: join strategy to be set for specified class and station.
	 */
	@Override
	public void setJoinStrategy(Object stationKey, Object classKey, Object joinStrategy) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.joinStrategy == null || !scd.joinStrategy.equals(joinStrategy)) {
			scd.joinStrategy = joinStrategy;
			save = true;
		}
	}

	/**
	 * Returns semaphore strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, null value is
	 * returned.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @return semaphore strategy for specified class and station.
	 */
	@Override
	public Object getSemaphoreStrategy(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.semaphoreStrategy;
	}

	/**
	 * Sets semaphore strategy for class and station, given their search keys.
	 * If specified station cannot accept this kind of parameter, no value will
	 * be set.
	 * @param stationKey: search key for station.
	 * @param classKey: search key for class.
	 * @param semaphoreStrategy: semaphore strategy to be set for specified class and station.
	 */
	@Override
	public void setSemaphoreStrategy(Object stationKey, Object classKey, Object semaphoreStrategy) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (scd.semaphoreStrategy == null || !scd.semaphoreStrategy.equals(semaphoreStrategy)) {
			scd.semaphoreStrategy = semaphoreStrategy;
			save = true;
		}
	}

	/**
	 * Returns logging parameters for the logger. <I>MF'08 0.7.4</I>
	 * @param stationKey: search key for station.
	 */
	@Override
	public LoggerParameters getLoggingParameters(Object stationKey) {
		StationData sd;
		if (stationDataHM.containsKey(stationKey)) {
			sd = (StationData) stationDataHM.get(stationKey);
		} else {
			return null;
		}
		return (sd.loggerParameters != null) ? sd.loggerParameters : new LoggerParameters();
	}

	/**
	 * Sets logging parameters for the logger. <I>MF'08 0.7.4</I>
	 * @param stationKey: search key for station.
	 * @param loggerParameters: local LoggerParameters.
	 */
	@Override
	public void setLoggingParameters(Object stationKey, LoggerParameters loggerParameters) {
		StationData sd;
		if (stationDataHM.containsKey(stationKey)) {
			sd = (StationData) stationDataHM.get(stationKey);
		} else {
			return;
		}
		if (sd.loggerParameters == null || !sd.loggerParameters.equals(loggerParameters)) {
			sd.loggerParameters = loggerParameters;
			save = true;
		}
	}

	/**
	 * Returns logging parameters for the Global logfile. <I>MF'08 0.7.4</I>
	 * @param selector: either "path", "delim", or "autoAppend" 
	 */
	@Override
	public String getLoggingGlbParameter(String selector) {
		// The global parameters must be instantiated for a new file
		if (loggerGlbParams == null) {
			loggerGlbParams = new LoggerGlobalParameters();
		}

		if (selector.equalsIgnoreCase("path")) {
			return loggerGlbParams.path;
		} else if (selector.equalsIgnoreCase("delim")) {
			return loggerGlbParams.delimiter;
		} else if (selector.equalsIgnoreCase("decimalSeparator")) {
			return loggerGlbParams.decimalSeparator;
		} else if (selector.equalsIgnoreCase("autoAppend")) {
			return loggerGlbParams.autoAppendMode.toString();
		} else {
			debugLog.error("No such selector " + selector + " for " + new Exception().getStackTrace()[0] + "\n" + new Exception().getStackTrace()[1]);
		}
		return null;
	}

	/**
	 * Sets logging parameters for the Global logfile. <I>MF'08 0.7.4</I>
	 * @param selector: either "path", "delim", "decimalSeparator", or "autoAppend" 
	 * @param value: String to assign to variable named by selector.
	 */
	@Override
	public void setLoggingGlbParameter(String selector, String value) {
		// The global parameters must be instantiated for a new file
		if (loggerGlbParams == null) {
			loggerGlbParams = new LoggerGlobalParameters();
		}

		debugLog.debug("glbParameter <" + selector + "=" + value + ">");

		if (selector.equalsIgnoreCase("path")) {
			loggerGlbParams.path = value;
		} else if (selector.equalsIgnoreCase("decimalSeparator")) {
			loggerGlbParams.decimalSeparator = value;
		} else if (selector.equalsIgnoreCase("delim")) {
			loggerGlbParams.delimiter = value;
		} else if (selector.equalsIgnoreCase("autoAppend")) {
			loggerGlbParams.autoAppendMode = new Integer(value);
		} else {
			debugLog.error("No such selector " + selector + " for " + new Exception().getStackTrace()[0] + "\n" + new Exception().getStackTrace()[1]);
			return;
		}
		save = true;
	}

	/**
	 * Used by the Mediator to get the <i>files that could be overwritten</i>,
	 * in order to warn the user with a dialog box. 
	 * @author MF08 0.7.4 - Michael Fercu for Logger (Marco Bertoli)
	 * @return String[] of the filename of all log files that will be written to.
	 */
	public String[] getLoggerNameList() {
		String[] lknames = new String[loggersKeyset.size()];
		int size = 0;
		boolean globalWasProcessed = false;

		// get the list of all filenames from their keys
		for (Object loggerKey : loggersKeyset) {
			LoggerParameters lp = getLoggingParameters(loggerKey);
			if (!lp.name.equalsIgnoreCase("global")) {
				lknames[size++] = lp.name;
			} else if (lp.name.equalsIgnoreCase("global") && !globalWasProcessed) {
				globalWasProcessed = true;
				lknames[size++] = lp.name;
			}
		}

		String[] lknames2 = new String[size];
		for (int i = 0; i < size; i++) {
			lknames2[i] = lknames[i];
		}
		return lknames2;
	}

	/**
	 * Manages the routing and fork probabilities.
	 */
	@Override
	public void manageProbabilities() {
		for (Object stationKey : stationsKeyset) {
			for (Object classKey : classesKeyset) {
				if (getRoutingStrategy(stationKey, classKey) instanceof ProbabilityRouting) {
					ProbabilityRouting pr = (ProbabilityRouting) getRoutingStrategy(stationKey, classKey);
					Map<Object, Double> values = pr.getValues();
					normalizeRoutingProbabilities(stationKey, classKey, values);
				}
				if (getRoutingStrategy(stationKey, classKey) instanceof LoadDependentRouting) {
					LoadDependentRouting ldr = (LoadDependentRouting) getRoutingStrategy(stationKey, classKey);
					for (Map<Object, Double> values : ldr.getAllEmpiricalEntries().values()) {
						normalizeRoutingProbabilities(stationKey, classKey, values);
					}
				}
				if (getForkStrategy(stationKey, classKey) instanceof ProbabilitiesFork) {
					ProbabilitiesFork pf = (ProbabilitiesFork) getForkStrategy(stationKey, classKey);
					Map<Object, OutPath> outPaths = (Map<Object, OutPath>) pf.getOutDetails();
					for (Map.Entry<Object, OutPath> entry : outPaths.entrySet()) {
						Map<Object, Double> values = (Map) entry.getValue().getOutParameters();
						normalizeForkProbabilities(values);
					}
				}
				if (getForkStrategy(stationKey, classKey) instanceof CombFork) {
					CombFork cf = (CombFork) getForkStrategy(stationKey, classKey);
					Map<Object, Double> values = (Map<Object, Double>) cf.getOutDetails();
					normalizeForkProbabilities(values);
				}
			}
		}
	}

	/**
	 * Normalizes the routing probabilities.
	 */
	@Override
	public void normalizeRoutingProbabilities(Object stationKey, Object classKey, Map<Object, Double> values) {
		Vector<Object> outputs = getForwardConnections(stationKey);
		Vector<Object> normalOutputs = new Vector<Object>();
		normalOutputs.addAll(outputs);
		if (getClassType(classKey) == CLASS_TYPE_CLOSED) {
			for (int i = 0; i < outputs.size(); i++) {
				if (getStationType(outputs.get(i)).equals(STATION_TYPE_SINK)) {
					normalOutputs.remove(outputs.get(i));
				}
			}
		}

		Double[] probabilities = new Double[outputs.size()];
		Object[] keys = outputs.toArray();
		for (int i = 0; i < probabilities.length; i++) {
			probabilities[i] = (Double) values.get(keys[i]);
			if (probabilities[i] == null) {
				probabilities[i] = new Double(0.0);
			}
		}
		values.clear();

		double totalSum = 0.0;
		for (int i = 0; i < probabilities.length; i++) {
			if (normalOutputs.contains(keys[i])) {
				totalSum += probabilities[i].doubleValue();
			}
		}
		for (int i = 0; i < probabilities.length; i++) {
			if (normalOutputs.contains(keys[i])) {
				if (totalSum == 0.0) {
					probabilities[i] = new Double(1.0 / normalOutputs.size());
				} else {
					probabilities[i] = new Double(probabilities[i].doubleValue() / totalSum);
				}
			} else {
				probabilities[i] = new Double(0.0);
			}
			values.put(keys[i], probabilities[i]);
		}
	}

	/**
	 * Normalizes the fork probabilities.
	 */
	@Override
	public void normalizeForkProbabilities(Map<Object, Double> values) {
		Double[] probabilities = new Double[values.size()];
		Object[] keys = values.keySet().toArray();
		for (int i = 0; i < probabilities.length; i++) {
			probabilities[i] = (Double) values.get(keys[i]);
			if (probabilities[i] == null) {
				probabilities[i] = new Double(0.0);
			}
		}
		values.clear();

		double totalSum = 0.0;
		for (int i = 0; i < probabilities.length; i++) {
			totalSum += probabilities[i].doubleValue();
		}
		for (int i = 0; i < probabilities.length; i++) {
			if (totalSum == 0.0) {
				probabilities[i] = new Double(1.0 / probabilities.length);
			} else {
				probabilities[i] = new Double(probabilities[i].doubleValue() / totalSum);
			}
			values.put(keys[i], probabilities[i]);
		}
	}

	/**
	 * Adds station details, given the search key.
	 */
	protected void addStationDetails(Object key) {
		for (Object classKey : classesKeyset) {
			StationClassData scd = getDefaultStationDetails(key);
			stationDetailsBDM.put(classKey, key, scd);
		}
	}

	/**
	 * Deletes station details, given the search key.
	 */
	protected void deleteStationDetails(Object key) {
		for (Object classKey : classesKeyset) {
			stationDetailsBDM.remove(classKey, key);
		}
	}

	/**
	 * Adds station details for a class, given the search key.
	 */
	protected void addStationDetailsForClass(Object key) {
		for (Object stationKey : stationsKeyset) {
			StationClassData scd = getDefaultStationDetails(stationKey);
			stationDetailsBDM.put(key, stationKey, scd);
		}

		for (Object stationKey : stationsKeyset) {
			for (Object classKey : classesKeyset) {
				StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
				if (classKey == key) {
					if (scd.routingStrategy != null) {
						Vector<Object> outputs = getForwardConnections(stationKey);
						for (Object out : outputs) {
							((RoutingStrategy) scd.routingStrategy).addStation(out);
						}
					}
					if (scd.forkStrategy != null) {
						Vector<Object> outputs = getForwardConnections(stationKey);
						for (Object out : outputs) {
							((ForkStrategy) scd.forkStrategy).addStation(out, classKey, classesKeyset);
						}
					}
				} else {
					if (scd.forkStrategy instanceof ClassSwitchFork
							|| scd.forkStrategy instanceof MultiBranchClassSwitchFork) {
						Map<Object, OutPath> m = (Map<Object, OutPath>) ((ForkStrategy) scd.forkStrategy).getOutDetails();
						for (OutPath o : m.values()) {
							o.getOutParameters().put(key, 0);
						}
					}
					if (scd.joinStrategy instanceof GuardJoin) {
						((GuardJoin) scd.joinStrategy).getGuard().put(key, 0);
					}
				}
			}
		}
	}

	/**
	 * Deletes station details for a class, given the search key.
	 */

	protected void deleteStationDetailsForClass(Object key) {
		for (Object stationKey : stationsKeyset) {
			stationDetailsBDM.remove(key, stationKey);
		}

		for (Object stationKey : stationsKeyset) {
			StationData sd = (StationData) stationDataHM.get(stationKey);
			if (sd.transitionModeList != null) {
				for (TransitionModeData tmd : sd.transitionModeList) {
					tmd.enablingCondition.removeColumn(key);
					tmd.inhibitingCondition.removeColumn(key);
					tmd.firingOutcome.removeColumn(key);
				}
			}

			for (Object classKey : classesKeyset) {
				if (classKey == key) {
					continue;
				}
				StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
				if (scd.forkStrategy instanceof ClassSwitchFork
						|| scd.forkStrategy instanceof MultiBranchClassSwitchFork) {
					Map<Object, OutPath> m = (Map<Object, OutPath>) ((ForkStrategy) scd.forkStrategy).getOutDetails();
					for (OutPath o : m.values()) {
						o.getOutParameters().remove(key);
					}
				}
				if (scd.joinStrategy instanceof GuardJoin) {
					((GuardJoin) scd.joinStrategy).getGuard().remove(key);
				}
				if (scd.classSwitchProb != null) {
					scd.classSwitchProb.setValue(classKey, key, 0.0f);
				}
			}
		}
	}

	/**
	 * Gets default station details, given the search key.
	 */
	private StationClassData getDefaultStationDetails(Object key) {
		StationData sd = (StationData) stationDataHM.get(key);
		StationClassData scd = new StationClassData();
		if (sd.type.equals(STATION_TYPE_SOURCE)) {
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
		} else if (sd.type.equals(STATION_TYPE_SINK)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_TERMINAL)) {
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
		} else if (sd.type.equals(STATION_TYPE_ROUTER)) {
			scd.queueStrategy = QUEUE_STRATEGY_FCFS;
			scd.dropRule = Defaults.get("dropRule");
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
		} else if (sd.type.equals(STATION_TYPE_DELAY)) {
			scd.queueStrategy = QUEUE_STRATEGY_FCFS;
			scd.dropRule = Defaults.get("dropRule");
			scd.serviceDistribution = Defaults.getAsNewInstance("stationDelayServiceStrategy");
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
		} else if (sd.type.equals(STATION_TYPE_SERVER)) {
			scd.queueStrategy = Defaults.get("stationQueueStrategy");
			if (sd.queueStrategy.equals(STATION_QUEUE_STRATEGY_PSSERVER)) {
				if (scd.queueStrategy.equals(QUEUE_STRATEGY_FCFS)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_LCFS)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_RAND)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_SJF)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_LJF)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_SEPT)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_LEPT)) {
					scd.queueStrategy = QUEUE_STRATEGY_PS;
				}
			} else if (sd.queueStrategy.equals(STATION_QUEUE_STRATEGY_PREEMPTIVE)) {
				if (scd.queueStrategy.equals(QUEUE_STRATEGY_FCFS)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_LCFS)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_RAND)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_SJF)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_LJF)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_SEPT)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_LEPT)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_PS)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_DPS)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_GPS)) {
					scd.queueStrategy = QUEUE_STRATEGY_LCFS_PR;
				}

			} else {
				if (scd.queueStrategy.equals(QUEUE_STRATEGY_PS)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_LCFS_PR)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_GPS)
						|| scd.queueStrategy.equals(QUEUE_STRATEGY_DPS)) {
					scd.queueStrategy = QUEUE_STRATEGY_FCFS;
				}
			}
			scd.dropRule = Defaults.get("dropRule");
			scd.serviceWeight = Defaults.getAsDouble("serviceWeight");
			scd.impatienceType = ImpatienceType.NONE;
			scd.serviceDistribution = Defaults.getAsNewInstance("stationServiceStrategy");
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
		} else if (sd.type.equals(STATION_TYPE_FORK)) {
			scd.queueStrategy = Defaults.get("stationQueueStrategy");
			if (scd.queueStrategy.equals(QUEUE_STRATEGY_PS)
					|| scd.queueStrategy.equals(QUEUE_STRATEGY_GPS)
					|| scd.queueStrategy.equals(QUEUE_STRATEGY_DPS)
					|| scd.queueStrategy.equals(QUEUE_STRATEGY_SJF)
					|| scd.queueStrategy.equals(QUEUE_STRATEGY_LJF)
					|| scd.queueStrategy.equals(QUEUE_STRATEGY_SEPT)
					|| scd.queueStrategy.equals(QUEUE_STRATEGY_LEPT)) {
				scd.queueStrategy = QUEUE_STRATEGY_FCFS;
			}
			scd.dropRule = Defaults.get("dropRule");
			scd.impatienceType = ImpatienceType.NONE;
			scd.forkStrategy = Defaults.getAsNewInstance("stationForkStrategy");
		} else if (sd.type.equals(STATION_TYPE_JOIN)) {
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.joinStrategy = Defaults.getAsNewInstance("stationJoinStrategy");
		} else if (sd.type.equals(STATION_TYPE_LOGGER)) {
			scd.queueStrategy = QUEUE_STRATEGY_FCFS;
			scd.dropRule = Defaults.get("dropRule");
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
		} else if (sd.type.equals(STATION_TYPE_CLASSSWITCH)) {
			scd.queueStrategy = QUEUE_STRATEGY_FCFS;
			scd.dropRule = Defaults.get("dropRule");
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.classSwitchProb = new ClassSwitchRow();
		} else if (sd.type.equals(STATION_TYPE_SEMAPHORE)) {
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.semaphoreStrategy = Defaults.getAsNewInstance("stationSemaphoreStrategy");
		} else if (sd.type.equals(STATION_TYPE_SCALER)) {
			scd.forkStrategy = Defaults.getAsNewInstance("stationForkStrategy");
			scd.joinStrategy = Defaults.getAsNewInstance("stationJoinStrategy");
		} else if (sd.type.equals(STATION_TYPE_PLACE)) {
			scd.queueCapacity = Defaults.getAsInteger("placeQueueCapacity");
			scd.queueStrategy = Defaults.get("placeQueueStrategy");
			scd.dropRule = Defaults.get("placeDropRule");
		} else if (sd.type.equals(STATION_TYPE_TRANSITION)) {
			// Do nothing
		}
		scd.retrialDistribution = Defaults.getAsNewInstance("classDistribution");
		return scd;
	}

	/**
	 * Manages reference station for every class.
	 */
	private void manageRefStations() {
		Vector<Object> openRefCandidateKeys = getStationKeysRefStation();
		Vector<Object> closedRefCandidateKeys = getStationKeysNoSourceSink();
		for (Object classKey : classesKeyset) {
			int classType = getClassType(classKey);
			Object refStationKey = getClassRefStation(classKey);
			if (classType == CLASS_TYPE_OPEN) {
				if (!openRefCandidateKeys.contains(refStationKey)) {
					setClassRefStation(classKey, null);
				}
			} else {
				if (!closedRefCandidateKeys.contains(refStationKey)) {
					setClassRefStation(classKey, null);
				}
			}
		}
	}

	/*-------------------------------------------------------------------------------
	 *-------------  methods for inter-station connections definition  --------------
	 *-------------------------------------------------------------------------------*/
	/**
	 * Adds a connection between two stations in this model, given search keys of
	 * source and target stations.
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @param areConnected: true if stations must be connected, false otherwise.
	 * @return : true if connection status was changed, false otherwise.
	 */
	@Override
	public boolean setConnected(Object sourceKey, Object targetKey, boolean areConnected) {
		Connection toSet = (Connection) connectionsBDM.get(targetKey, sourceKey);
		//connection does not exist, return.
		if (toSet == null) {
			return false;
		}
		//elements are not connectable, return.
		if (!toSet.isConnectable) {
			return false;
		}
		//checks if elements have been connected
		if (!toSet.isConnected) {
			//must connect
			if (areConnected) {
				toSet.isConnected = true;
				save = true;
				for (Object classKey : classesKeyset) {
					StationClassData sscd = (StationClassData) stationDetailsBDM.get(classKey, sourceKey);
					if (sscd.routingStrategy != null) {
						((RoutingStrategy) sscd.routingStrategy).addStation(targetKey);
					}
					if (sscd.forkStrategy != null) {
						((ForkStrategy) sscd.forkStrategy).addStation(targetKey, classKey, classesKeyset);
					}
				}
				return true;
			}
		} else {
			//must disconnect
			if (!areConnected) {
				toSet.isConnected = false;
				save = true;
				StationData ssd = (StationData) stationDataHM.get(sourceKey);
				if (ssd.transitionModeList != null) {
					for (TransitionModeData tmd : ssd.transitionModeList) {
						tmd.firingOutcome.removeRow(targetKey);
					}
				}
				StationData tsd = (StationData) stationDataHM.get(targetKey);
				if (tsd.transitionModeList != null) {
					for (TransitionModeData tmd : tsd.transitionModeList) {
						tmd.enablingCondition.removeRow(sourceKey);
						tmd.inhibitingCondition.removeRow(sourceKey);
					}
				}
				for (Object classKey : classesKeyset) {
					StationClassData sscd = (StationClassData) stationDetailsBDM.get(classKey, sourceKey);
					if (sscd.routingStrategy != null) {
						((RoutingStrategy) sscd.routingStrategy).removeStation(targetKey);
					}
					if (sscd.forkStrategy != null) {
						((ForkStrategy) sscd.forkStrategy).removeStation(targetKey);
					}
				}
				return true;
			}
		}
		return false;
	}

	/**Tells whether two stations are connected
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @return : true if stations are connected, false otherwise.
	 */
	@Override
	public boolean areConnected(Object sourceKey, Object targetKey) {
		Connection conn = (Connection) connectionsBDM.get(targetKey, sourceKey);
		if (conn == null) {
			return false;
		} else {
			return conn.isConnected;
		}
	}

	/**Tells whether two stations can be connected
	 * @param sourceKey: search key for source station
	 * @param targetKey: search key for target station
	 * @return : true if stations are connectable, false otherwise.
	 */
	@Override
	public boolean areConnectable(Object sourceKey, Object targetKey) {
		Connection conn = (Connection) connectionsBDM.get(targetKey, sourceKey);
		if (conn == null) {
			return false;
		} else {
			return conn.isConnectable;
		}
	}

	/**Returns a set of station keys specified station is connected to as a source.
	 * @param stationKey: source station for which (target) connected stations must be
	 * returned.
	 * @return Vector containing keys for connected stations.
	 */
	@Override
	public Vector<Object> getForwardConnections(Object stationKey) {
		//must find entry for row index (e.g. connection sources set)
		Map conns = connectionsBDM.get(stationKey, BDMap.Y);
		return scanForConnections(conns);
	}

	/**Returns a set of station keys specified station is connected to as a target.
	 * @param stationKey: source station for which (source) connected stations must be
	 * returned.
	 * @return Vector containing keys for connected stations.
	 */
	@Override
	public Vector<Object> getBackwardConnections(Object stationKey) {
		//must find entry for row index (e.g. connection targets set)
		Map conns = connectionsBDM.get(stationKey, BDMap.X);
		return scanForConnections(conns);
	}

	/**
	 * Finds out connections, given a connection set
	 */
	private Vector<Object> scanForConnections(Map conns) {
		Vector<Object> retval = new Vector<Object>(0);
		//check returned map not to be null
		if (conns != null) {
			//scan all of the station entries to find out which selected one is connected to
			for (int i = 0; i < stationsKeyset.size(); i++) {
				Connection c = (Connection) conns.get(stationsKeyset.get(i));
				//assure connection is not null, preventing NullPointerException
				if (c != null) {
					//finally, if connection exists and is connected, add key to returned vector
					if (c.isConnected) {
						retval.add(stationsKeyset.get(i));
					}
				}
			}
		}
		return retval;
	}

	/**Tells whether two stations can be connected. Subclasses can override this method to
	 * change behaviour in connection creation.*/
	protected boolean canBeConnected(Object sourceKey, Object targetKey) {
		StationData source = (StationData) stationDataHM.get(sourceKey), target = (StationData) stationDataHM.get(targetKey);
		/*if source and target are both servers or delays or LDServers, at first
		instance, declare them connectable*/
		if ((source.type.equals(STATION_TYPE_SERVER) || source.type.equals(STATION_TYPE_DELAY))
				&& (target.type.equals(STATION_TYPE_SERVER) || target.type.equals(STATION_TYPE_DELAY))) {
			return true;
		}
		//no incoming connection to source
		if (target.type.equals(STATION_TYPE_SOURCE)) {
			return false;
		}
		//no outgoing connection from sink
		if (source.type.equals(STATION_TYPE_SINK)) {
			return false;
		}
		//no connection between a router, fork, join, logger, class switch,
		//semaphore, scaler, place or transition and itself
		if (sourceKey == targetKey
				&& (source.type.equals(STATION_TYPE_ROUTER) || source.type.equals(STATION_TYPE_FORK) || source.type.equals(STATION_TYPE_JOIN)
						|| source.type.equals(STATION_TYPE_LOGGER) || source.type.equals(STATION_TYPE_CLASSSWITCH) || source.type.equals(STATION_TYPE_SEMAPHORE)
						|| source.type.equals(STATION_TYPE_SCALER) || source.type.equals(STATION_TYPE_PLACE) || source.type.equals(STATION_TYPE_TRANSITION))) {
			return false;
		}
		//no connection among sources, sinks or terminals
		if ((source.type.equals(STATION_TYPE_SOURCE) || source.type.equals(STATION_TYPE_SINK) || source.type.equals(STATION_TYPE_TERMINAL))
				&& (target.type.equals(STATION_TYPE_SOURCE) || target.type.equals(STATION_TYPE_SINK) || target.type.equals(STATION_TYPE_TERMINAL))) {
			return false;
		}
		//a place can only be connected to a transition
		//a transition can only be connected from a place
		if ((source.type.equals(STATION_TYPE_PLACE) && !target.type.equals(STATION_TYPE_TRANSITION))
				|| (target.type.equals(STATION_TYPE_TRANSITION) && !source.type.equals(STATION_TYPE_PLACE))) {
			return false;
		}
		return true;
	}

	/**
	 * Adds connections for a station, given the search key.
	 */
	private void addStationConnections(Object stationKey) {
		//building set of backward connections
		Vector<Object> inputs = new Vector<Object>(connectionsBDM.keySet(BDMap.Y));
		Map<Object, Connection> backward = new HashMap<Object, Connection>();
		for (Object in : inputs) {
			backward.put(in, new Connection(canBeConnected(in, stationKey), false));
		}
		connectionsBDM.put(stationKey, BDMap.X, backward);
		//building set of forward connections
		Vector<Object> outputs = new Vector<Object>(connectionsBDM.keySet(BDMap.X));
		Map<Object, Connection> forward = new HashMap<Object, Connection>();
		for (Object out : outputs) {
			forward.put(out, new Connection(canBeConnected(stationKey, out), false));
		}
		connectionsBDM.put(stationKey, BDMap.Y, forward);
	}

	/**
	 * Deletes connections for a station, given the search key.
	 */
	private void deleteStationConnections(Object stationKey) {
		Vector<Object> inputs = getBackwardConnections(stationKey);
		for (Object in : inputs) {
			setConnected(in, stationKey, false);
		}
		connectionsBDM.remove(stationKey, BDMap.X);
		Vector<Object> outputs = getForwardConnections(stationKey);
		for (Object out : outputs) {
			setConnected(stationKey, out, false);
		}
		connectionsBDM.remove(stationKey, BDMap.Y);
	}

	/*------------------------------------------------------------------------------
	 *---------------  methods for simulation parameters definition  ----------------
	 *-------------------------------------------------------------------------------*/
	/* (non-Javadoc)
	 * @see jmt.gui.common.definitions.SimulationDefinition#addMeasure(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object addMeasure(String type, Object stationKey, Object classKey) {
		return addMeasure(type, stationKey, classKey, Defaults.getAsDouble("measureAlpha"), Defaults.getAsDouble("measurePrecision"), false);
	}

	/* (non-Javadoc)
	 * @see jmt.gui.common.definitions.SimulationDefinition#addMeasure(java.lang.String, java.lang.Object, java.lang.Object, java.lang.Double, java.lang.Double, boolean)
	 */
	@Override
	public Object addMeasure(String type, Object stationKey, Object classKey, Double alpha, Double precision, boolean log) {
		Object key = new Long(++incrementalKey);
		measuresKeyset.add(key);
		MeasureData md = new MeasureData(type, stationKey, classKey, alpha, precision, log);
		measureDataHM.put(key, md);
		save = true;
		return key;
	}

	@Override
	public void removeMeasure(Object measureKey) {
		if (measuresKeyset.contains(measureKey)) {
			measuresKeyset.remove(measureKey);
			measureDataHM.remove(measureKey);
			save = true;
		}
	}

	private void deleteClassMeasures(Object classKey) {
		Iterator<Object> it = measuresKeyset.iterator();
		while (it.hasNext()) {
			if (getMeasureClass(it.next()) == classKey) {
				it.remove();
			}
		}
	}

	private void deleteStationMeasures(Object stationKey) {
		Iterator<Object> it = measuresKeyset.iterator();
		while (it.hasNext()) {
			if (getMeasureStation(it.next()) == stationKey) {
				it.remove();
			}
		}
	}

	@Override
	public Vector<Object> getMeasureKeys() {
		return measuresKeyset;
	}

	@Override
	public String getMeasureType(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).type;
		}
	}

	@Override
	public void setMeasureType(String newType, Object measureKey) {
		if (measuresKeyset.contains(measureKey)) {
			String oldType = measureDataHM.get(measureKey).type;
			measureDataHM.get(measureKey).type = newType;
			if (!oldType.equals(newType)) {
				save = true;
			}
		}
	}

	@Override
	public Object getMeasureStation(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).stationKey;
		}
	}

	@Override
	public void setMeasureStation(Object stationKey, Object measureKey) {
		if (measuresKeyset.contains(measureKey)) {
			Object oldKey = measureDataHM.get(measureKey).stationKey;
			measureDataHM.get(measureKey).stationKey = stationKey;
			if (oldKey != stationKey) {
				save = true;
			}
		}
	}

	@Override
	public Object getMeasureClass(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).classKey;
		}
	}

	@Override
	public void setMeasureClass(Object classKey, Object measureKey) {
		if (measuresKeyset.contains(measureKey)) {
			Object oldKey = measureDataHM.get(measureKey).classKey;
			measureDataHM.get(measureKey).classKey = classKey;
			if (oldKey != classKey) {
				save = true;
			}
		}
	}

	@Override
	public Double getMeasureAlpha(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).alpha;
		}
	}

	@Override
	public void setMeasureAlpha(Double alpha, Object measureKey) {
		if (alpha.doubleValue() > 0 && alpha.doubleValue() < 1) {
			if (measuresKeyset.contains(measureKey)) {
				Double oldAlpha = measureDataHM.get(measureKey).alpha;
				measureDataHM.get(measureKey).alpha = alpha;
				if (!oldAlpha.equals(alpha)) {
					save = true;
				}
			}
		}
	}

	@Override
	public Double getMeasurePrecision(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return null;
		} else {
			return measureDataHM.get(measureKey).precision;
		}
	}

	@Override
	public void setMeasurePrecision(Double precision, Object measureKey) {
		if (precision.doubleValue() > 0 && precision.doubleValue() < 1) {
			if (measuresKeyset.contains(measureKey)) {
				Double oldPrecision = measureDataHM.get(measureKey).precision;
				measureDataHM.get(measureKey).precision = precision;
				if (!oldPrecision.equals(precision)) {
					save = true;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jmt.gui.common.definitions.SimulationDefinition#getMeasureLog(java.lang.Object)
	 */
	@Override
	public boolean getMeasureLog(Object measureKey) {
		if (!measuresKeyset.contains(measureKey)) {
			return false;
		} else {
			return measureDataHM.get(measureKey).log;
		}
	}

	/* (non-Javadoc)
	 * @see jmt.gui.common.definitions.SimulationDefinition#setMeasureLog(boolean, java.lang.Object)
	 */
	@Override
	public void setMeasureLog(boolean log, Object measureKey) {
		MeasureData data = measureDataHM.get(measureKey);
		if (data != null && data.log != log) {
			data.log = log;
			save = true;
		}
	}

	/**
	 * Tells if a given measure is global
	 * @param type type of given measure
	 * @return true if measure is global
	 */
	@Override
	public boolean isGlobalMeasure(String type) {
		return type.equals(MEASURE_S_X) || type.equals(MEASURE_S_RP) || type.equals(MEASURE_S_CN)
				|| type.equals(MEASURE_S_DR) || type.equals(MEASURE_S_P) || type.equals(MEASURE_S_R)
				|| type.equals(MEASURE_S_RR) || type.equals(MEASURE_S_BR);
	}

	/**
	 * Tells if a given measure is for sinks only
	 * @param type type of given measure
	 * @return true if measure is for sinks only
	 */
	@Override
	public boolean isSinkMeasure(String type) {
		return type.equals(MEASURE_RP_PER_SINK) || type.equals(MEASURE_X_PER_SINK);
	}

	/**
	 * Tells if a given measure is for blocking regions only
	 * @param type type of given measure
	 * @return true if measure is for blocking regions only
	 */
	@Override
	public boolean isFCRMeasure(String type) {
		return type.equals(MEASURE_FCR_TW) || type.equals(MEASURE_FCR_MO);
	}

	/**
	 * Tells if a given measure is for fork/join sections only
	 * @param type type of given measure
	 * @return true if measure is for fork/join sections only
	 */
	@Override
	public boolean isFJMeasure(String type) {
		return type.equals(MEASURE_FJ_CN) || type.equals(MEASURE_FJ_RP);
	}

	/**
	 * Returns number of preloaded jobs at specified station for specified class.
	 * @param stationKey station at which preloaded jobs are preloaded.
	 * @param classKey class to which preloaded jobs belong.
	 * @return number of preloaded jobs.
	 */
	@Override
	public Integer getPreloadedJobs(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return null;
		}
		return scd.preload;
	}

	/**
	 * Sets number of preloaded jobs at specified station for specified class.
	 * For closed classes, total number of preloaded jobs for a class over all
	 * stations must be equal to class population, otherwise update is canceled.
	 * @param stationKey station at which preloaded jobs are preloaded.
	 * @param classKey class to which preloaded jobs belong.
	 * @param preload number of preloaded jobs.
	 */
	@Override
	public void setPreloadedJobs(Object stationKey, Object classKey, Integer preload) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		if (scd == null) {
			return;
		}
		if (!scd.preload.equals(preload)) {
			scd.preload = preload;
			save = true;
		}
	}

	/**
	 * Returns total number of preloaded jobs for specified class.
	 * @param classKey class to which preloaded jobs belong.
	 * @return total number of preloaded jobs.
	 */
	@Override
	public Integer getPreloadedJobsNumber(Object classKey) {
		int foundJob = 0;
		for (Object stationKey : stationsKeyset) {
			foundJob += getPreloadedJobs(stationKey, classKey).intValue();
		}
		return new Integer(foundJob);
	}

	/**
	 * This method is used to manage number of jobs for every class. If class is closed
	 * all spare jobs will be allocated to its reference source, if for some reasons more
	 * jobs are allocated than max population, they are reduced. Uses this method only
	 * when strictly necessary as it can be slow if the model is big.
	 */
	@Override
	public void manageJobs() {
		for (Object classKey : classesKeyset) {
			manageJobs(classKey);
		}
	}

	/**
	 * This method is used to manage number of jobs for a given class. If class is closed
	 * all spare jobs will be allocated to its reference source (if it is not in a blocking
	 * region, otherwise a different station is chosen), if for some reasons more jobs are
	 * allocated than max population, they are reduced.
	 * @param classKey class to which managed jobs belong.
	 */
	private void manageJobs(Object classKey) {
		if (getClassType(classKey) == CLASS_TYPE_CLOSED) {
			int population = getClassPopulation(classKey).intValue();
			int foundJob = getPreloadedJobsNumber(classKey).intValue();
			if (population > foundJob) {
				Object refStation = getClassRefStation(classKey);
				if (refStation == null) {
					return;
				}
				// If refStation is not preloadable or is inside a blocking region, choose the preloadable station
				// that is not inside any blocking region and has the most preloaded jobs
				if (!isStationTypePreloadable(getStationType(refStation)) || getStationBlockingRegion(refStation) != null) {
					int preloadMax = -1;
					for (Object stationKey : stationsKeyset) {
						if (isStationTypePreloadable(getStationType(stationKey)) && getStationBlockingRegion(stationKey) == null
								&& getPreloadedJobs(stationKey, classKey).intValue() > preloadMax) {
							preloadMax = getPreloadedJobs(stationKey, classKey).intValue();
							refStation = stationKey;
						}
					}
				}
				// Increments amount of jobs in the chosen station to cover all spare ones
				int allocate = population - foundJob + getPreloadedJobs(refStation, classKey).intValue();
				setPreloadedJobs(refStation, classKey, new Integer(allocate));
			} else if (population < foundJob) {
				// Removes jobs from stations until allocated jobs = population
				for (Object stationKey : stationsKeyset) {
					int jobs = getPreloadedJobs(stationKey, classKey).intValue();
					if (jobs < foundJob - population) {
						setPreloadedJobs(stationKey, classKey, new Integer(0));
						foundJob -= jobs;
					} else {
						setPreloadedJobs(stationKey, classKey, new Integer(jobs - (foundJob - population)));
						break;
					}
				}
			}
		}
	}

	/**
	 * Tells if a given station type is preloadable
	 * @param stationType type of station
	 * @return true if station of that type is preloadable, false otherwise
	 */
	protected boolean isStationTypePreloadable(String stationType) {
		return !stationType.equals(STATION_TYPE_SOURCE) && !stationType.equals(STATION_TYPE_SINK)
				&& !stationType.equals(STATION_TYPE_TERMINAL) && !stationType.equals(STATION_TYPE_JOIN)
				&& !stationType.equals(STATION_TYPE_SEMAPHORE) && !stationType.equals(STATION_TYPE_SCALER)
				&& !stationType.equals(STATION_TYPE_TRANSITION);
	}

	/**
	 * Gets seed for simulation.
	 * @return seed for simulation.
	 */
	@Override
	public Long getSimulationSeed() {
		return seed;
	}

	/**
	 * Sets seed for simulation.
	 * @param seed seed for simulation.
	 */
	@Override
	public void setSimulationSeed(Long seed) {
		if (!this.seed.equals(seed)) {
			save = true;
		}
		this.seed = seed;
	}

	/**
	 * Tells if random seed is used.
	 * @return true if random seed is used, false otherwise.
	 */
	@Override
	public Boolean getUseRandomSeed() {
		return useRandomSeed;
	}

	/**
	 * Sets if random seed is used.
	 * @param useRandomSeed true if random seed is used, false otherwise.
	 */
	@Override
	public void setUseRandomSeed(Boolean useRandomSeed) {
		if (!this.useRandomSeed.equals(useRandomSeed)) {
			save = true;
		}
		this.useRandomSeed = useRandomSeed;
	}

	/**
	 * Returns maximum duration for simulation in seconds.
	 * @return seconds of maximum duration for simulation.
	 */
	@Override
	public Double getMaximumDuration() {
		return maxDuration;
	}

	/**
	 * Sets maximum duration for simulation in seconds.
	 * @param durationSeconds seconds of maximum duration for simulation.
	 */
	@Override
	public void setMaximumDuration(Double durationSeconds) {
		if (!this.maxDuration.equals(durationSeconds)) {
			save = true;
		}
		this.maxDuration = durationSeconds;
	}

	/**
	 * Returns maximum duration for simulation in time units.
	 * @return time units of maximum duration for simulation.
	 */
	@Override
	public Double getMaxSimulatedTime()
	{
		return maxSimulatedTime;
	}

	/**
	 * Sets maximum duration for simulation in time units.
	 * @param duration time units of maximum duration for simulation.
	 */
	@Override
	public void setMaxSimulatedTime(Double duration) {
		if (!this.maxSimulatedTime.equals(duration)) {
			save = true;
		}
		this.maxSimulatedTime = duration;
	}

	/**
	 * Returns maximum number of simulation samples.
	 * @return maximum number of simulation samples.
	 */
	@Override
	public Integer getMaxSimulationSamples() {
		return maxSamples;
	}

	/**
	 * Sets maximum number of simulation samples.
	 * @param maxSamples maximum number of simulation samples.
	 */
	@Override
	public void setMaxSimulationSamples(Integer maxSamples) {
		if (!this.maxSamples.equals(maxSamples)) {
			save = true;
		}
		this.maxSamples = maxSamples;
	}

	/**
	 * Tells if statistic check is disabled.
	 * @return true if statistic check is disabled, false otherwise.
	 */
	@Override
	public Boolean getDisableStatistic() {
		return disableStatistic;
	}

	/**
	 * Sets if statistic check is disabled.
	 * @param disableStatistic true if statistic check is disabled, false otherwise.
	 */
	@Override
	public void setDisableStatistic(Boolean disableStatistic) {
		if (!this.disableStatistic.equals(disableStatistic)) {
			save = true;
		}
		this.disableStatistic = disableStatistic;
	}

	/**
	 * Returns maximum number of simulation events.
	 * @return maximum number of simulation events.
	 */
	@Override
	public Integer getMaxSimulationEvents() {
		return maxEvents;
	}

	/**
	 * Sets maximum number of simulation events.
	 * @param maxEvents maximum number of simulation events.
	 */
	@Override
	public void setMaxSimulationEvents(Integer maxEvents) {
		if (!this.maxEvents.equals(maxEvents)) {
			save = true;
		}
		this.maxEvents = maxEvents;
	}

	/**
	 * Returns polling interval for temporary measures.
	 * @return polling interval for temporary measures.
	 */
	@Override
	public Double getPollingInterval() {
		return pollingInterval;
	}

	/**
	 * Sets polling interval for temporary measures.
	 * @param pollingInterval polling interval for temporary measures.
	 */
	@Override
	public void setPollingInterval(Double pollingInterval) {
		if (!this.pollingInterval.equals(pollingInterval)) {
			save = true;
		}
		this.pollingInterval = pollingInterval;
	}

	// --- Methods to manage simulation results -- Bertoli Marco --------------------------------------------
	/**
	 * Returns last simulation results
	 * @return simulation results or null if no simulation was performed
	 */
	@Override
	public MeasureDefinition getSimulationResults() {
		return results;
	}

	/**
	 * Sets simulation results
	 * @param results simulation results data structure
	 */
	@Override
	public void setSimulationResults(MeasureDefinition results) {
		this.results = results;
		save = true;
	}

	/**
	 * Tells if current model contains simulation results
	 * @return true if <code>getSimulationResults()</code> returns a non-null object
	 */
	@Override
	public boolean containsSimulationResults() {
		return (results != null);
	}
	// ------------------------------------------------------------------------------------------------------

	/**
	 * Tells if queue animation is enabled
	 * @return true if the animation is enabled
	 */
	@Override
	public boolean isAnimationEnabled() {
		return false;
	}

	/**
	 * Enable / disable queue animation
	 * @param enabled - set it to true to enable queue animation
	 */
	@Override
	public void setAnimationEnabled(boolean enabled) {
		return;
	}

	/**
	 * Tells if parametric analysis is enabled
	 * @return true if parametric analysis is enabled
	 */
	@Override
	public boolean isParametricAnalysisEnabled() {
		return parametricAnalysisEnabled;
	}

	/**
	 * Enable / disable parametric analysis
	 * @param enabled - set it to true to enable parametric analysis
	 */
	@Override
	public void setParametricAnalysisEnabled(boolean enabled) {
		if (parametricAnalysisEnabled != enabled) {
			save = true;
		}
		parametricAnalysisEnabled = enabled;
	}

	/**
	 * Returns the ParametricAnalysisModel
	 * @return the parametricAnalysisModel
	 */
	@Override
	public ParametricAnalysisDefinition getParametricAnalysisModel() {
		return parametricAnalysisModel;
	}

	/**
	 * Sets the ParametricAnalysisModel
	 * @param pad the parametricAnalysisModel to be set
	 */
	@Override
	public void setParametricAnalysisModel(ParametricAnalysisDefinition pad) {
		parametricAnalysisModel = pad;
	}

	/**
	 * Tells model that some data has been changed and need to be saved. This
	 * is used by Parametric Analysis
	 */
	@Override
	public void setSaveChanged() {
		save = true;
	}

	/**
	 * Returns the search key for the class, given the class name.
	 */
	@Override
	public Object getClassByName(String className) {
		for (Object classKey : classesKeyset) {
			if (getClassName(classKey).equals(className)) {
				return classKey;
			}
		}
		return null;
	}

	/**
	 * Returns the search key for the station, given the station name.
	 */
	@Override
	public Object getStationByName(String stationName) {
		for (Object stationKey : stationsKeyset) {
			if (getStationName(stationKey).equals(stationName)) {
				return stationKey;
			}
		}
		return null;
	}

	// --- Blocking Region Definition --- Bertoli Marco ----------------------------------------------

	/**
	 * Adds a new blocking region to the model
	 * @param name name of the new region
	 * @param type type of the new region
	 * @return search's key for new region
	 */
	@Override
	public Object addBlockingRegion(String name, String type) {
		name = getUniqueRegionName(name);
		Object key = new Long(++incrementalKey);
		blockingRegionsKeyset.add(key);
		blockingDataHM.put(key, new BlockingRegionData(name, type,
				Defaults.getAsInteger("blockingMaxJobs"),
				Defaults.getAsInteger("blockingMaxMemory")));
		// Adds region-class specific data
		addBlockingDetails(key);
		save = true;
		return key;
	}

	/**
	 * Deletes a blocking region from the model
	 * @param key search's key for region
	 */
	@Override
	public void deleteBlockingRegion(Object key) {
		if (blockingRegionsKeyset.contains(key)) {
			BlockingRegionData brd = (BlockingRegionData) blockingDataHM.get(key);
			// Removes blocking region from station reference
			for (Object stationKey : brd.stations) {
				((StationData) stationDataHM.get(stationKey)).blockingRegion = null;
				// Adds this station to blockable ones
				if (canStationTypeBeBlocked(((StationData) stationDataHM.get(stationKey)).type)) {
					blockableKeyset.add(stationKey);
				}
			}
			blockingDataHM.remove(key);
			deleteBlockingDetails(key);
			blockingRegionsKeyset.remove(key);
			save = true;
		}
	}

	/**
	 * Returns the entire set of blocking region keys
	 * @return the entire set of blocking region keys
	 */
	@Override
	public Vector<Object> getRegionKeys() {
		return blockingRegionsKeyset;
	}

	/**
	 * Gets the name of a blocking region
	 * @param regionKey search's key for region
	 * @return name of the region
	 */
	@Override
	public String getRegionName(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).name;
	}

	/**
	 * Sets the name of a blocking region
	 * @param regionKey search's key for region
	 * @param name name of the region
	 */
	@Override
	public void setRegionName(Object regionKey, String name) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).name.equals(name) || name.equals("")) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).name = getUniqueRegionName(name);
		save = true;
	}

	/**
	 * Gets the type of a blocking region
	 * @param regionKey search's key for region
	 * @return type of the region
	 */
	@Override
	public String getRegionType(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).type;
	}

	/**
	 * Sets the type of a blocking region
	 * @param regionKey search's key for region
	 * @param type type of the region
	 */
	@Override
	public void setRegionType(Object regionKey, String type) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).type.equals(type)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).type = type;
		save = true;
	}

	/**
	 * Gets the global customer number constraint for a blocking region
	 * @param regionKey search's key for region
	 * @return maximum number of allowed customers (-1 means infinity)
	 */
	@Override
	public Integer getRegionCustomerConstraint(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs;
	}

	/**
	 * Sets the global customer number constraint for a blocking region
	 * @param regionKey search's key for region
	 * @param maxJobs maximum number of allowed customers (-1 means infinity)
	 */
	@Override
	public void setRegionCustomerConstraint(Object regionKey, Integer maxJobs) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs.equals(maxJobs)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs = maxJobs;
		save = true;
	}

	/**
	 * Gets the global memory size constraint for a blocking region
	 * @param regionKey search's key for region
	 * @return maximum size of allowed memory (-1 means infinity)
	 */
	@Override
	public Integer getRegionMemorySize(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory;
	}

	/**
	 * Sets the global memory size constraint for a blocking region
	 * @param regionKey search's key for region
	 * @param maxMemory maximum size of allowed memory (-1 means infinity)
	 */
	@Override
	public void setRegionMemorySize(Object regionKey, Integer maxMemory) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory.equals(maxMemory)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory = maxMemory;
		save = true;
	}

	/**
	 * Gets the customer number constraint for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return maximum number of allowed customers for specified class (-1 means infinity)
	 */
	@Override
	public Integer getRegionClassCustomerConstraint(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs;
	}

	/**
	 * Sets the customer number constraint for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param maxJobs maximum number of allowed customers for specified class (-1 means infinity)
	 */
	@Override
	public void setRegionClassCustomerConstraint(Object regionKey, Object classKey, Integer maxJobs) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs.equals(maxJobs)) {
			return;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs = maxJobs;
		save = true;
	}

	/**
	 * Gets the memory size constraint for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return maximum size of allowed memory for specified class (-1 means infinity)
	 */
	@Override
	public Integer getRegionClassMemorySize(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxMemory;
	}

	/**
	 * Sets the memory size constraint for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param maxMemory maximum size of allowed memory for specified class (-1 means infinity)
	 */
	@Override
	public void setRegionClassMemorySize(Object regionKey, Object classKey, Integer maxMemory) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxMemory.equals(maxMemory)) {
			return;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxMemory = maxMemory;
		save = true;
	}

	/**
	 * Gets the drop rule for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return true if jobs of specified class can be dropped, false otherwise
	 */
	@Override
	public Boolean getRegionClassDropRule(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).drop;
	}

	/**
	 * Sets the drop rule for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param drop true if jobs of specified class can be dropped, false otherwise
	 */
	@Override
	public void setRegionClassDropRule(Object regionKey, Object classKey, Boolean drop) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).drop.equals(drop)) {
			return;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).drop = drop;
		save = true;
	}

	/**
	 * Gets the weight of each job for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return weight of each job for specified class
	 */
	@Override 
	public Integer getRegionClassWeight(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight;
	}

	/**
	 * Sets the weight of each job for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param weight weight of each job for specified class
	 */
	@Override
	public void setRegionClassWeight(Object regionKey, Object classKey, Integer weight) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight.equals(weight)) {
			return;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight = weight;
		save = true;
	}

	/**
	 * Gets the size of each job for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @return size of each job for specified class
	 */
	@Override 
	public Integer getRegionClassSize(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size;
	}

	/**
	 * Sets the size of each job for a blocking region and a class
	 * @param regionKey search's key for region
	 * @param classKey search's key for class
	 * @param size size of each job for specified class
	 */
	@Override
	public void setRegionClassSize(Object regionKey, Object classKey, Integer size) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size.equals(size)) {
			return;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size = size;
		save = true;
	}

	/**
	 * Tells if a station can be added to a blocking region
	 * (all the stations that create or destroy jobs cannot be added)
	 * @param regionKey search's key for region
	 * @param stationKey search's key for station
	 * @return true if the station can be added to specified region
	 */
	@Override
	public boolean canRegionStationBeAdded(Object regionKey, Object stationKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !stationsKeyset.contains(stationKey)) {
			return false;
		}
		StationData sd = (StationData) stationDataHM.get(stationKey);
		return (sd.blockingRegion == null || sd.blockingRegion == regionKey) && canStationTypeBeBlocked(sd.type);
	}

	/**
	 * Tells if a station of a given type can be added to a blocking region
	 * (all the stations that create or destroy jobs cannot be added)
	 * @param stationType type of station to be added
	 * @return true if a station of that type can be added, false otherwise
	 */
	protected boolean canStationTypeBeBlocked(String stationType) {
		return !stationType.equals(STATION_TYPE_SOURCE) && !stationType.equals(STATION_TYPE_SINK)
				&& !stationType.equals(STATION_TYPE_TERMINAL) && !stationType.equals(STATION_TYPE_FORK)
				&& !stationType.equals(STATION_TYPE_JOIN) && !stationType.equals(STATION_TYPE_CLASSSWITCH)
				&& !stationType.equals(STATION_TYPE_SEMAPHORE) && !stationType.equals(STATION_TYPE_SCALER)
				&& !stationType.equals(STATION_TYPE_TRANSITION);
	}

	/**
	 * Adds a station to a blocking region
	 * @param regionKey search's key for region
	 * @param stationKey search's key for station
	 * @return true if the station has been added to specified region
	 */
	@Override
	public boolean addRegionStation(Object regionKey, Object stationKey) {
		if (!canRegionStationBeAdded(regionKey, stationKey)) {
			return false;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).stations.add(stationKey);
		((StationData) stationDataHM.get(stationKey)).blockingRegion = regionKey;
		// Removes this station from blockable list as it is added to a blocking region
		blockableKeyset.remove(stationKey);
		save = true;
		return true;
	}

	/**
	 * Removes a station from a blocking region
	 * @param regionKey search's key for region
	 * @param stationKey search's key for station
	 */
	@Override
	public void removeRegionStation(Object regionKey, Object stationKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !stationsKeyset.contains(stationKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).stations.remove(stationKey);
		((StationData) stationDataHM.get(stationKey)).blockingRegion = null;
		// If this station can be blocked, adds it to blockable stations vector
		if (canStationTypeBeBlocked(((StationData) stationDataHM.get(stationKey)).type)) {
			blockableKeyset.add(stationKey);
		}
		save = true;
	}

	/**
	 * Returns a set of all the stations in a blocking region
	 * @param regionKey search's key for region
	 * @return a set of all the stations in specified region
	 */
	@Override
	public Set<Object> getBlockingRegionStations(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).stations;
	}

	/**
	 * Gets the blocking region of a station
	 * @param stationKey search's key for station
	 * @return search's key for region or null if it is undefined
	 */
	@Override
	public Object getStationBlockingRegion(Object stationKey) {
		if (!stationsKeyset.contains(stationKey)) {
			return null;
		}
		return ((StationData) stationDataHM.get(stationKey)).blockingRegion;
	}

	/**
	 * Gets a vector of stations that can be added to a blocking region
	 * @return a vector of stations that can be added to a blocking region
	 */
	@Override
	public Vector<Object> getBlockableStationKeys() {
		return blockableKeyset;
	}

	/**
	 * Adds blocking region details, given the search key
	 */
	protected void addBlockingDetails(Object regionKey) {
		for (Object classKey : classesKeyset) {
			blockingDetailsBDM.put(classKey, regionKey, new BlockingClassData(Defaults.getAsInteger("blockingMaxJobsPerClass"),
					Defaults.getAsInteger("blockingMaxMemoryPerClass"), Defaults.getAsBoolean("blockingDropPerClass"),
					Defaults.getAsInteger("blockingWeightPerClass"), Defaults.getAsInteger("blockingSizePerClass")));
		}
	}

	/**
	 * Deletes blocking region details, given the search key
	 */
	protected void deleteBlockingDetails(Object regionKey) {
		for (Object classKey : classesKeyset) {
			blockingDetailsBDM.remove(classKey, regionKey);
		}
	}

	/**
	 * Adds blocking region details for a class, given the search key
	 */
	protected void addBlockingDetailsForClass(Object classKey) {
		for (Object regionKey : blockingRegionsKeyset) {
			blockingDetailsBDM.put(classKey, regionKey, new BlockingClassData(Defaults.getAsInteger("blockingMaxJobsPerClass"),
					Defaults.getAsInteger("blockingMaxMemoryPerClass"), Defaults.getAsBoolean("blockingDropPerClass"),
					Defaults.getAsInteger("blockingWeightPerClass"), Defaults.getAsInteger("blockingSizePerClass")));
		}
	}

	/**
	 * Deletes blocking region details for a class, given the search key
	 */
	protected void deleteBlockingDetailsForClass(Object classKey) {
		for (Object regionKey : blockingRegionsKeyset) {
			blockingDetailsBDM.remove(classKey, regionKey);
		}

		for (Object regionKey : blockingRegionsKeyset) {
			BlockingRegionData brd = (BlockingRegionData) blockingDataHM.get(regionKey);
			for (BlockingGroupData bgd : brd.groupList) {
				if (bgd.classList.contains(classKey)) {
					bgd.classList.remove(classKey);
					break;
				}
			}
		}
	}

	/**
	 * Adds a new group to a blocking region
	 * @param regionKey search's key for region
	 * @param name name of the new group
	 */
	@Override
	public void addRegionGroup(Object regionKey, String name) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.add(new BlockingGroupData(name,
				Defaults.getAsInteger("blockingGroupMaxJobs"), Defaults.getAsInteger("blockingGroupMaxMemory")));
		save = true;
	}

	/**
	 * Deletes a group from a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 */
	@Override
	public void deleteRegionGroup(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.remove(index);
		save = true;
	}

	/**
	 * Deletes all the groups from a blocking region
	 * @param regionKey search's key for region
	 */
	@Override
	public void deleteAllRegionGroups(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.clear();
		save = true;
	}

	/**
	 * Returns a list of the group data for a blocking region
	 * @param regionKey search's key for region
	 * @return a list of the group data for specified region
	 */
	@Override
	public List<BlockingGroupData> getRegionGroupList(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).groupList;
	}

	/**
	 * Gets the name of a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @return name of the group
	 */
	@Override
	public String getRegionGroupName(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).name;
	}

	/**
	 * Sets the name of a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param name name of the group
	 */
	@Override
	public void setRegionGroupName(Object regionKey, int index, String name) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).name = name;
		save = true;
	}

	/**
	 * Gets the customer number constraint for a blocking region and a group
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @return maximum number of allowed customers for specified group (-1 means infinity)
	 */
	@Override
	public Integer getRegionGroupCustomerConstraint(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).maxJobs;
	}

	/**
	 * Sets the customer number constraint for a blocking region and a group
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param maxJobs maximum number of allowed customers for specified group (-1 means infinity)
	 */
	@Override
	public void setRegionGroupCustomerConstraint(Object regionKey, int index, Integer maxJobs) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).maxJobs = maxJobs;
		save = true;
	}

	/**
	 * Gets the memory size constraint for a blocking region and a group
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @return maximum size of allowed memory for specified group (-1 means infinity)
	 */
	@Override
	public Integer getRegionGroupMemorySize(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).maxMemory;
	}

	/**
	 * Sets the memory size constraint for a blocking region and a group
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param maxMemory maximum size of allowed memory for specified group (-1 means infinity)
	 */
	@Override
	public void setRegionGroupMemorySize(Object regionKey, int index, Integer maxMemory) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).maxMemory = maxMemory;
		save = true;
	}

	/**
	 * Adds a class to a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param classKey search's key for class
	 */
	@Override
	public void addClassIntoRegionGroup(Object regionKey, int index, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).classList.contains(classKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).classList.add(classKey);
		save = true;
	}

	/**
	 * Removes a class from a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @param classKey search's key for class
	 */
	@Override
	public void removeClassFromRegionGroup(Object regionKey, int index, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return;
		}
		if (!((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).classList.contains(classKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).classList.remove(classKey);
		save = true;
	}

	/**
	 * Removes all the classes from a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 */
	@Override
	public void removeAllClassesFromRegionGroup(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).classList.clear();
		save = true;
	}

	/**
	 * Returns a list of classes in a group for a blocking region
	 * @param regionKey search's key for region
	 * @param index indexing number for group
	 * @return a list of classes in specified group
	 */
	@Override
	public List<Object> getRegionGroupClassList(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).groupList.get(index).classList;
	}

	// --- end Blocking Region Definition ------------------------------------------------------------

	/**This class implements the comparator for search keys. This can be useful for
	 * sorting a specific search key set.*/
	protected static class KeyComparator implements Comparator<Object> {
		public int compare(Object o1, Object o2) {
			return Long.compare((Long) o1, (Long) o2);
		}
	}

	/**This class packs altogether customer class parameters. This can be useful for
	 * storing them inside an hash map or other data structures.*/
	protected class ClassData {
		public String name;
		public int type;
		public Integer priority;
		public Integer population;
		public Object distribution;
		public Object refStation;

		public ClassData(String name, int type, Integer priority, Integer population, Object distribution) {
			this.name = name;
			this.type = type;
			this.priority = priority;
			this.population = population;
			this.distribution = distribution;
		}

		@Override
		public Object clone() {
			ClassData cd = new ClassData(name, type, priority, population, distribution);
			cd.refStation = refStation;
			return cd;
		}
	}

	/**This class packs altogether station parameters. This can be useful for storing
	 * inside an hash map or other data structures. Data contained inside this class are
	 * defined once per each station. Data which are defined for a couple of one station
	 * and one class (such as service times distribution) or for a couple of stations (
	 * such as connections) need to be stored in other type of data structure and should not
	 * be stored inside this class*/
	protected class StationData {
		public String name;
		public String type;
		public Integer queueCapacity;
		public Integer maxRunning;
		public Boolean serverPreemptive;
		public String queueStrategy;
		public Integer numberOfServers;
		public String pollingServerType;
		public Integer pollingKValue;
		public Boolean serverPolling;
		public HashMap<Object, Object> switchoverPeriods;
		public Integer forkBlock;
		public Boolean isSimplifiedFork;
		public LoggerParameters loggerParameters;
		public List<TransitionModeData> transitionModeList;
		/** Reference to owner blocking region */
		public Object blockingRegion;

		public StationData(String name, String type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public Object clone() {
			StationData sd = new StationData(name, type);
			sd.queueCapacity = queueCapacity;
			sd.queueStrategy = queueStrategy;
			sd.maxRunning = maxRunning;
			sd.serverPreemptive = serverPreemptive;
			sd.numberOfServers = numberOfServers;
			sd.pollingServerType = pollingServerType;
			sd.serverPolling = serverPolling;
			if (switchoverPeriods != null) {
				sd.switchoverPeriods = new HashMap<>();
				for (Object key : switchoverPeriods.keySet()) {
					Object item;
					item = switchoverPeriods.get(key);
					if (item instanceof ServiceStrategy) {
						item = ((ServiceStrategy) item).clone();
					} else {
						item = new ZeroStrategy();
					}
					sd.switchoverPeriods.put(key, item);
				}
			}
			sd.forkBlock = forkBlock;
			sd.isSimplifiedFork = isSimplifiedFork;
			if (loggerParameters != null) {
				sd.loggerParameters = (LoggerParameters) loggerParameters.clone();
			}
			if (transitionModeList != null) {
				sd.transitionModeList = new ArrayList<TransitionModeData>();
				for (TransitionModeData tmd : transitionModeList) {
					sd.transitionModeList.add((TransitionModeData) tmd.clone());
				}
			}
			sd.blockingRegion = blockingRegion;
			return sd;
		}
	}

	/**This class contains all of the data which can be defined for a combination of
	 * class and station. These data include queue policy, service time distribution
	 * and routing strategy*/
	protected class StationClassData {
		public Integer preload;
		public Integer queueCapacity;
		public String queueStrategy;
		public String dropRule;
		public Double serviceWeight;
		public Object serviceDistribution;
		public Object routingStrategy;
		public Object forkStrategy;
		public Object joinStrategy;
		public Object semaphoreStrategy;
		public ClassSwitchRow classSwitchProb;
		private ImpatienceType impatienceType;
		private ImpatienceParameter impatienceParameter;
		public Object retrialDistribution;


		public StationClassData() {
			this.preload = new Integer(0);
		}

		@Override
		public Object clone() {
			StationClassData scd = new StationClassData();
			scd.preload = preload;
			scd.queueCapacity = queueCapacity;
			scd.queueStrategy = queueStrategy;
			scd.dropRule = dropRule;
			scd.serviceWeight = serviceWeight;
			if (serviceDistribution != null) {
				scd.serviceDistribution = ((ServiceStrategy) serviceDistribution).clone();
			}
			if (routingStrategy != null) {
				scd.routingStrategy = ((RoutingStrategy) routingStrategy).clone();
			}
			if (forkStrategy != null) {
				scd.forkStrategy = ((ForkStrategy) forkStrategy).clone();
			}
			if (joinStrategy != null) {
				scd.joinStrategy = ((JoinStrategy) joinStrategy).clone();
			}
			if (semaphoreStrategy != null) {
				scd.semaphoreStrategy = ((SemaphoreStrategy) semaphoreStrategy).clone();
			}
			if (classSwitchProb != null) {
				scd.classSwitchProb = (ClassSwitchRow) classSwitchProb.clone();
			}
			return scd;
		}
	}

	/**
	 * This class is used to store blocking region data
	 */
	protected class BlockingRegionData {
		public String name;
		public String type;
		public Integer maxJobs;
		public Integer maxMemory;
		public List<BlockingGroupData> groupList;
		/** Reference to owned stations */
		public Set<Object> stations;

		public BlockingRegionData(String name, String type, Integer maxJobs, Integer maxMemory) {
			this.name = name;
			this.type = type;
			this.maxJobs = maxJobs;
			this.maxMemory = maxMemory;
			this.groupList = new ArrayList<BlockingGroupData>();
			this.stations = new HashSet<Object>();
		}

		@Override
		public Object clone() {
			BlockingRegionData brd = new BlockingRegionData(name, type, maxJobs, maxMemory);
			for (BlockingGroupData bgd : groupList) {
				brd.groupList.add((BlockingGroupData) bgd.clone());
			}
			for (Object stationKey : stations) {
				brd.stations.add(stationKey);
			}
			return brd;
		}
	}

	/**
	 * This class is used to store blocking region / class data
	 */
	protected class BlockingClassData {
		public Integer maxJobs;
		public Integer maxMemory;
		public Boolean drop;
		public Integer weight;
		public Integer size;

		public BlockingClassData(Integer maxJobs, Integer maxMemory, Boolean drop, Integer weight, Integer size) {
			this.maxJobs = maxJobs;
			this.maxMemory = maxMemory;
			this.drop = drop;
			this.weight = weight;
			this.size = size;
		}

		@Override
		public Object clone() {
			return new BlockingClassData(maxJobs, maxMemory, drop, weight, size);
		}
	}

	/**
	 * This class is used to store blocking region / group data
	 */
	public class BlockingGroupData {
		public String name;
		public Integer maxJobs;
		public Integer maxMemory;
		public List<Object> classList;

		public BlockingGroupData(String name, Integer maxJobs, Integer maxMemory) {
			this.name = name;
			this.maxJobs = maxJobs;
			this.maxMemory = maxMemory;
			this.classList = new ArrayList<Object>();
		}

		@Override
		public Object clone() {
			BlockingGroupData bgd = new BlockingGroupData(name, maxJobs, maxMemory);
			for (Object classKey : classList) {
				bgd.classList.add(classKey);
			}
			return bgd;
		}
	}

	/**This class represents a connection between two stations. Two boolean parameters
	 * are defined. The first, isConnected, tells whether the two stations are connected
	 * The second, isConnectable, tells whether these two stations are connectable.*/
	protected class Connection {
		public boolean isConnected = false;
		public boolean isConnectable = false;

		public Connection(boolean isConnectable, boolean isConnected) {
			this.isConnectable = isConnectable;
			this.isConnected = isConnected;
		}
	}

	/**This class contains all of the parameters to define a simulation measure, e.g.
	 * type (Throughput, Residence Time, etc.), reference station, reference class,
	 * precision, confidence interval, logging flag */
	protected class MeasureData {
		public String type;
		public Object stationKey;
		public Object classKey;
		public Double precision;
		public Double alpha;
		public boolean log;

		public MeasureData(String type, Object stationKey, Object classKey, Double alpha, Double precision, boolean log) {
			this.type = type;
			this.stationKey = stationKey;
			this.classKey = classKey;
			this.precision = precision;
			this.alpha = alpha;
			this.log = log;
		}
	}

	protected class LoggerGlobalParameters {
		public String path;
		public String delimiter;
		public String decimalSeparator;
		public Integer autoAppendMode;

		LoggerGlobalParameters() {
			this.path = MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR);
			this.delimiter = Defaults.get("loggerDelimiter");
			this.decimalSeparator = Defaults.get("loggerDecimalSeparator");
			this.autoAppendMode = Defaults.getAsInteger("loggerAutoAppend");
		}
	}

	public class TransitionModeData {
		public String name;
		public TransitionMatrix enablingCondition;
		public TransitionMatrix inhibitingCondition;
		public Integer numberOfServers;
		public Object firingTimeDistribution;
		public Integer firingPriority;
		public Double firingWeight;
		public TransitionMatrix firingOutcome;

		public TransitionModeData(String name) {
			this.name = name;
			this.enablingCondition = new TransitionMatrix();
			this.inhibitingCondition = new TransitionMatrix();
			this.numberOfServers = Defaults.getAsInteger("transitionNumberOfServers");
			this.firingTimeDistribution = Defaults.getAsNewInstance("transitionFiringTimeDistribution");
			this.firingPriority = Integer.valueOf(-1);
			this.firingWeight = Double.valueOf(1.0);
			this.firingOutcome = new TransitionMatrix();
		}

		@Override
		public Object clone() {
			TransitionModeData tmd = new TransitionModeData(name);
			tmd.enablingCondition = (TransitionMatrix) enablingCondition.clone();
			tmd.inhibitingCondition = (TransitionMatrix) inhibitingCondition.clone();
			tmd.numberOfServers = numberOfServers;
			tmd.firingTimeDistribution = ((ServiceStrategy) firingTimeDistribution).clone();
			tmd.firingPriority = firingPriority;
			tmd.firingWeight = firingWeight;
			tmd.firingOutcome = (TransitionMatrix) firingOutcome.clone();
			return tmd;
		}
	}

	/**
	 * Returns total population of closed classes
	 */
	@Override
	public int getTotalClosedClassPopulation() {
		int sum = 0;
		for (Object closedClassKey : closedClassesKeyset) {
			sum += getClassPopulation(closedClassKey).intValue();
		}
		return sum;
	}

	/**
	 * Returns the cell (<code>classInKey</code>, <code>classOutKey</code>) of the class
	 * switch matrix for station <code>stationKey</code>.
	 */
	@Override
	public float getClassSwitchMatrix(Object stationKey, Object classInKey, Object classOutKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classInKey, stationKey);
		return scd.classSwitchProb.getValue(classInKey, classOutKey);
	}

	/**
	 * Sets the cell (<code>classInKey</code>, <code>classOutKey</code>) of the class
	 * switch matrix for station <code>stationKey</code>.
	 */
	@Override
	public void setClassSwitchMatrix(Object stationKey, Object classInKey, Object classOutKey, float value) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classInKey, stationKey);
		if (scd.classSwitchProb.getValue(classInKey, classOutKey) != value) {
			scd.classSwitchProb.setValue(classInKey, classOutKey, value);
			save = true;
		}
	}

	/**
	 * Returns the threshold for semaphore <code>stationKey</code> and class <code>classKey</code>.
	 */
	@Override
	public Integer getSemaphoreThreshold(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		SemaphoreStrategy ss = (SemaphoreStrategy) scd.semaphoreStrategy;
		return Integer.valueOf(ss.getThreshold());
	}

	/**
	 * Sets the threshold for semaphore <code>stationKey</code> and class <code>classKey</code>.
	 */
	@Override
	public void setSemaphoreThreshold(Object stationKey, Object classKey, Integer threshold) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		SemaphoreStrategy ss = (SemaphoreStrategy) scd.semaphoreStrategy;
		if (ss.getThreshold() != threshold.intValue()) {
			ss.setThreshold(threshold.intValue());
			save = true;
		}
	}

	/**
	 * Returns the size of the mode list for transition <code>stationKey</code>.
	 */
	@Override
	public int getTransitionModeListSize(Object stationKey) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		return sd.transitionModeList.size();
	}

	/**
	 * Adds a new mode for transition <code>stationKey</code>.
	 */
	@Override
	public void addTransitionMode(Object stationKey, String name) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		sd.transitionModeList.add(new TransitionModeData(getUniqueTransitionModeName(stationKey, name)));
		save = true;
	}

	/**
	 * Deletes a mode for transition <code>stationKey</code>.
	 */
	@Override
	public void deleteTransitionMode(Object stationKey, int modeIndex) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		deleteClassMeasures(tmd.name);
		sd.transitionModeList.remove(modeIndex);
		save = true;
	}

	/**
	 * Returns the name of mode <code>modeIndex</code> for transition <code>stationKey</code>.
	 */
	@Override
	public String getTransitionModeName(Object stationKey, int modeIndex) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		return tmd.name;
	}

	/**
	 * Returns the names of all the modes for transition <code>stationKey</code>.
	 */
	@Override
	public List<String> getAllTransitionModeNames(Object stationKey) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		List<String> list = new ArrayList<String>();
		for (TransitionModeData tmd : sd.transitionModeList) {
			list.add(tmd.name);
		}
		return list;
	}

	/**
	 * Sets the name of mode <code>modeIndex</code> for transition <code>stationKey</code>.
	 */
	@Override
	public void setTransitionModeName(Object stationKey, int modeIndex, String name) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		if (!tmd.name.equals(name) && !name.equals("")) {
			name = getUniqueTransitionModeName(stationKey, name);
			for (Object measureKey : measuresKeyset) {
				if (getMeasureClass(measureKey) == tmd.name) {
					setMeasureClass(name, measureKey);
				}
			}
			tmd.name = name;
			save = true;
		}
	}

	/**
	 * Returns the entry (<code>stationInKey</code>, <code>classKey</code>) of the enabling
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public Integer getEnablingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		return tmd.enablingCondition.getEntry(stationInKey, classKey);
	}

	/**
	 * Sets the entry (<code>stationInKey</code>, <code>classKey</code>) of the enabling
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public void setEnablingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey, Integer value) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		if (!tmd.enablingCondition.getEntry(stationInKey, classKey).equals(value)) {
			tmd.enablingCondition.setEntry(stationInKey, classKey, value);
			save = true;
		}
	}

	/**
	 * Returns the entry (<code>stationInKey</code>, <code>classKey</code>) of the inhibiting
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public Integer getInhibitingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		return tmd.inhibitingCondition.getEntry(stationInKey, classKey);
	}

	/**
	 * Sets the entry (<code>stationInKey</code>, <code>classKey</code>) of the inhibiting
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public void setInhibitingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey, Integer value) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		if (!tmd.inhibitingCondition.getEntry(stationInKey, classKey).equals(value)) {
			tmd.inhibitingCondition.setEntry(stationInKey, classKey, value);
			save = true;
		}
	}

	/**
	 * Returns the number of servers for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public Integer getNumberOfServers(Object stationKey, int modeIndex) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		return tmd.numberOfServers;
	}

	/**
	 * Sets the number of servers for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public void setNumberOfServers(Object stationKey, int modeIndex, Integer number) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		if (!tmd.numberOfServers.equals(number)) {
			tmd.numberOfServers = number;
			save = true;
		}
	}

	/**
	 * Returns the firing time distribution for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public Object getFiringTimeDistribution(Object stationKey, int modeIndex) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		return tmd.firingTimeDistribution;
	}

	/**
	 * Sets the firing time distribution for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public void setFiringTimeDistribution(Object stationKey, int modeIndex, Object distribution) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		if (!tmd.firingTimeDistribution.equals(distribution)) {
			tmd.firingTimeDistribution = distribution;
			save = true;
		}
	}

	/**
	 * Returns the firing priority for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public Integer getFiringPriority(Object stationKey, int modeIndex) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		return tmd.firingPriority;
	}

	/**
	 * Sets the firing priority for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public void setFiringPriority(Object stationKey, int modeIndex, Integer priority) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		if (!tmd.firingPriority.equals(priority)) {
			tmd.firingPriority = priority;
			save = true;
		}
	}

	/**
	 * Returns the firing weight for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public Double getFiringWeight(Object stationKey, int modeIndex) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		return tmd.firingWeight;
	}

	/**
	 * Sets the firing weight for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public void setFiringWeight(Object stationKey, int modeIndex, Double weight) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		if (!tmd.firingWeight.equals(weight)) {
			tmd.firingWeight = weight;
			save = true;
		}
	}

	/**
	 * Returns the entry (<code>stationOutKey</code>, <code>classKey</code>) of the firing
	 * outcome for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public Integer getFiringOutcome(Object stationKey, int modeIndex, Object stationOutKey, Object classKey) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		return tmd.firingOutcome.getEntry(stationOutKey, classKey);
	}

	/**
	 * Sets the entry (<code>stationOutKey</code>, <code>classKey</code>) of the firing
	 * outcome for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public void setFiringOutcome(Object stationKey, int modeIndex, Object stationOutKey, Object classKey, Integer value) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = sd.transitionModeList.get(modeIndex);
		if (!tmd.firingOutcome.getEntry(stationOutKey, classKey).equals(value)) {
			tmd.firingOutcome.setEntry(stationOutKey, classKey, value);
			save = true;
		}
	}

	/**
	 * Returns given name if a class with the same name does not exist or makes it unique
	 * @param name class name
	 * @return unique name
	 */
	protected String getUniqueClassName(String name) {
		// Map of all unique names with their first users
		TreeSet<String> names = new TreeSet<String>();
		Vector<Object> keys = getClassKeys();
		// Finds all used names
		for (int i = 0; i < keys.size(); i++) {
			names.add(getClassName(keys.get(i)));
		}

		// If name is new, returns it
		if (!names.contains(name)) {
			return name;
		}

		int num;
		// If format is already '*_[number]' increment number
		char[] charname = name.toCharArray();
		int n = charname.length - 1;
		while (charname[n] >= '0' && charname[n] <= '9' && n > 0) {
			n--;
		}
		if (charname[n] == '_') {
			num = Integer.parseInt(name.substring(n + 1));
			name = name.substring(0, n); // Removes suffix
		}
		// Otherwise uses number 1
		else {
			num = 1;
		}
		// Finds unique number
		while (names.contains(name + "_" + num)) {
			num++;
		}
		return name + "_" + num;
	}

	/**
	 * Returns given name if a station with the same name does not exist or makes it unique
	 * @param name station name
	 * @return unique name
	 */
	protected String getUniqueStationName(String name) {
		// Map of all unique names with their first users
		TreeSet<String> names = new TreeSet<String>();
		Vector<Object> keys = getStationKeys();
		// Finds all used names
		for (int i = 0; i < keys.size(); i++) {
			names.add(getStationName(keys.get(i)));
		}
		names.add(STATION_TYPE_FORK);
		names.add(STATION_TYPE_CLASSSWITCH);
		names.add(STATION_TYPE_SCALER);
		names.add(STATION_TYPE_TRANSITION);

		// If name is new, returns it
		if (!names.contains(name)) {
			return name;
		}

		int num;
		// If format is already '*_[number]' increment number
		char[] charname = name.toCharArray();
		int n = charname.length - 1;
		while (charname[n] >= '0' && charname[n] <= '9' && n > 0) {
			n--;
		}
		if (charname[n] == '_') {
			num = Integer.parseInt(name.substring(n + 1));
			name = name.substring(0, n); // Removes suffix
		}
		// Otherwise uses number 1
		else {
			num = 1;
		}
		// Finds unique number
		while (names.contains(name + "_" + num)) {
			num++;
		}
		return name + "_" + num;
	}

	/**
	 * Returns given name if a blocking region with the same name does not exist or makes it unique
	 * @param name blocking region name
	 * @return unique name
	 */
	protected String getUniqueRegionName(String name) {
		// Map of all unique names with their first users
		TreeSet<String> names = new TreeSet<String>();
		Vector<Object> keys = getRegionKeys();
		// Finds all used names
		for (int i = 0; i < keys.size(); i++) {
			names.add(getRegionName(keys.get(i)));
		}

		// If name is new, returns it
		if (!names.contains(name)) {
			return name;
		}

		int num;
		// If format is already '*_[number]' increment number
		char[] charname = name.toCharArray();
		int n = charname.length - 1;
		while (charname[n] >= '0' && charname[n] <= '9' && n > 0) {
			n--;
		}
		if (charname[n] == '_') {
			num = Integer.parseInt(name.substring(n + 1));
			name = name.substring(0, n); // Removes suffix
		}
		// Otherwise uses number 1
		else {
			num = 1;
		}
		// Finds unique number
		while (names.contains(name + "_" + num)) {
			num++;
		}
		return name + "_" + num;
	}

	/**
	 * Returns given name if a mode with the same name does not exist or makes it unique
	 * for a transition
	 * @param key station key
	 * @param name mode name
	 * @return unique name
	 */
	protected String getUniqueTransitionModeName(Object key, String name) {
		// Map of all unique names with their first users
		List<String> names = getAllTransitionModeNames(key);

		// If name is new, returns it
		if (!names.contains(name)) {
			return name;
		}

		int num;
		// If format is already '*_[number]' increment number
		char[] charname = name.toCharArray();
		int n = charname.length - 1;
		while (charname[n] >= '0' && charname[n] <= '9' && n > 0) {
			n--;
		}
		if (charname[n] == '_') {
			num = Integer.parseInt(name.substring(n + 1));
			name = name.substring(0, n); // Removes suffix
		}
		// Otherwise uses number 1
		else {
			num = 1;
		}
		// Finds unique number
		while (names.contains(name + "_" + num)) {
			num++;
		}
		return name + "_" + num;
	}

	@Override
	public Integer getWhatIfParallelism() {
		return Defaults.getAsInteger("whatIfParallelism");
	}

}
