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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
import jmt.gui.common.routingStrategies.LoadDependentRouting;
import jmt.gui.common.routingStrategies.ProbabilityRouting;
import jmt.gui.common.routingStrategies.RoutingStrategy;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.transitionUtilities.TransitionMatrix;

/**
 * Created by IntelliJ IDEA.
 * User: OrsotronIII
 * Date: 24-mag-2005
 * Time: 9.46.43
 * Modified by Bertoli Marco
 * 
 * Modified by Francesco D'Aquino
 * 
 * Modified by Ashanka (May 2010):
 * Patch: Multi-Sink Perf. Index
 * Description: Added new Performance index for the capturing the 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 * Hence added a code to fetch the sinkkeys and if a measure is sink perf measure. 
 * 
 * Modified by Ashanka (June 2010):
 * Updated the Manage Probabilities. Closed Classes routed to Sink with probability <> 0.0 should show warning to Users.
 * 
 * Modified by Ashanka (July 2010)
 * Desc: Added new defaults control of a Random CheckBox.
 * 
 * Modified by Ashanka (Nov 2012)
 * Desc: Added refresh of LoadDependent Routing on deletion of station.
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
	protected Double pollingInterval;
	protected MeasureDefinition results;
	protected LoggerGlobalParameters loggerGlbParams;
	// Used to tell if model have to be saved
	protected boolean save = false;
	private static JSimLogger debugLog = JSimLogger.getRootLogger();

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
	//protected BDMap connectionsBDM = new CachedBDMap(),
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
	private boolean sinkProbabilityUpdate;//Probability Routing Error.
	private Vector<String> sinkProbabilityUpdateClasses = new Vector<String>();
	private Vector<String> sinkProbabilityUpdateStations = new Vector<String>();
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
		disableStatistic = Boolean.FALSE;
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
		if (cd.name == null || !cd.name.equals(name)) {
			cd.name = name;
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
			save = true;
			if (type == CLASS_TYPE_OPEN) {
				cd.population = null;
				cd.distribution = Defaults.getAsNewInstance("classDistribution");
				cd.refStation = null;
				openClassesKeyset.add(key);
				closedClassesKeyset.remove(key);
			} else {
				cd.population = Defaults.getAsInteger("classPopulation");
				cd.distribution = null;
				cd.refStation = null;
				openClassesKeyset.remove(key);
				closedClassesKeyset.add(key);
			}

			for (Object stationKey : stationsKeyset) {
				for (Object classKey : classesKeyset) {
					Object strategy = getForkStrategy(stationKey, classKey);
					if (strategy instanceof ClassSwitchFork) {
						Map<Object, OutPath> m = ((ClassSwitchFork) strategy).getOutDetails();
						for (OutPath o : m.values()) {
							o.getOutParameters().put(key, 0);
						}
					}
					if (strategy instanceof MultiBranchClassSwitchFork) {
						Map<Object, OutPath> m = ((MultiBranchClassSwitchFork) strategy).getOutDetails();
						for (OutPath o : m.values()) {
							o.getOutParameters().put(key, 0);
						}
					}
					strategy = getJoinStrategy(stationKey, classKey);
					if (strategy instanceof GuardJoin) {
						((GuardJoin) strategy).getGuard().put(key, 0);
					}
				}
			}
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
	 * @param name name of the class.
	 * @param type type of the class.
	 * @param priority priority of the class.
	 * @param population population of the class.
	 * @param distribution inter-arrival time distribution of the class.
	 * @return search key for the new class.
	 */
	@Override
	public Object addClass(String name, int type, Integer priority, Integer population, Object distribution) {
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
		for (Object stationKey : stationsKeyset) {
			for (Object classKey : classesKeyset) {
				Object strategy = getForkStrategy(stationKey, classKey);
				if (strategy instanceof ClassSwitchFork) {
					Map<Object, OutPath> m = ((ClassSwitchFork) strategy).getOutDetails();
					for (OutPath o : m.values()) {
						o.getOutParameters().put(key, 0);
					}
				}
				if (strategy instanceof MultiBranchClassSwitchFork) {
					Map<Object, OutPath> m = ((MultiBranchClassSwitchFork) strategy).getOutDetails();
					for (OutPath o : m.values()) {
						o.getOutParameters().put(key, 0);
					}
				}
				strategy = getJoinStrategy(stationKey, classKey);
				if (strategy instanceof GuardJoin) {
					((GuardJoin) strategy).getGuard().put(key, 0);
				}
			}
		}
		save = true;
		return key;
	}

	/**
	 * Deletes a class from the model, given the search key.
	 */
	@Override
	public void deleteClass(Object key) {
		if (classesKeyset.contains(key)) {
			classesKeyset.remove(key);
			classDataHM.remove(key);
			openClassesKeyset.remove(key);
			closedClassesKeyset.remove(key);
			deleteStationDetailsForClass(key);
			deleteBlockingDetailsForClass(key);
			deleteClassMeasures(key);
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
	 * This method returns reference source keys.
	 */
	@Override
	public Vector<Object> getStationKeysRefSource() {
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
		if (sd.name == null || !sd.name.equals(name)) {
			sd.name = name;
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
			sd.type = type;
			save = true;
			setDefaultsForStation(sd);
			addNewStationDetails(key);
			deleteStationConnections(key);
			addNewStationConnections(key);
			deleteStationMeasures(key);
			if (type.equals(STATION_TYPE_SOURCE)) {
				sourcesKeyset.add(key);
			} else {
				sourcesKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_SINK)) {
				sinksKeyset.add(key);
			} else {
				sinksKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_TERMINAL)) {
				terminalsKeyset.add(key);
			} else {
				terminalsKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_ROUTER)) {
				routersKeyset.add(key);
			} else {
				routersKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_DELAY)) {
				delaysKeyset.add(key);
			} else {
				delaysKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_SERVER)) {
				serversKeyset.add(key);
			} else {
				serversKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_FORK)) {
				forksKeyset.add(key);
			} else {
				forksKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_JOIN)) {
				joinsKeyset.add(key);
			} else {
				joinsKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_LOGGER)) {
				loggersKeyset.add(key);
			} else {
				loggersKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_CLASSSWITCH)) {
				classSwitchesKeyset.add(key);
			} else {
				classSwitchesKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_SEMAPHORE)) {
				semaphoresKeyset.add(key);
			} else {
				semaphoresKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_SCALER)) {
				scalersKeyset.add(key);
			} else {
				scalersKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_PLACE)) {
				placesKeyset.add(key);
			} else {
				placesKeyset.remove(key);
			}
			if (type.equals(STATION_TYPE_TRANSITION)) {
				transitionsKeyset.add(key);
			} else {
				transitionsKeyset.remove(key);
			}
			if (isStationTypePreloadable(type)) {
				if (!preloadableKeyset.contains(key)) {
					preloadableKeyset.add(key);
				}
			} else {
				preloadableKeyset.remove(key);
			}
			if (sd.blockingRegion != null) {
				if (!canStationTypeBeBlocked(type)) {
					removeRegionStation(sd.blockingRegion, key);
				}
			} else {
				if (canStationTypeBeBlocked(type)) {
					if (!blockableKeyset.contains(key)) {
						blockableKeyset.add(key);
					}
				} else {
					blockableKeyset.remove(key);
				}
			}
			manageRefStations();
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
		return sd.numOfServers;
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
		if (sd.numOfServers == null || !sd.numOfServers.equals(numberOfServers)) {
			sd.numOfServers = numberOfServers;
			save = true;
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
	 * @param name name of the station.
	 * @param type type of the station.
	 * @return search key for the new station.
	 */
	@Override
	public Object addStation(String name, String type) {
		Object key = new Long(++incrementalKey);
		stationsKeyset.add(key);
		StationData sd = new StationData(name, type);
		stationDataHM.put(key, sd);
		setDefaultsForStation(sd);
		addNewStationDetails(key);
		addNewStationConnections(key);
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
			Vector<Object> stationKeys = getBackwardConnections(key);
			for (Object stationKey : stationKeys) {
				for (Object classKey : classesKeyset) {
					RoutingStrategy rs = (RoutingStrategy) getRoutingStrategy(stationKey, classKey);
					ForkStrategy fs = (ForkStrategy) getForkStrategy(stationKey, classKey);
					if (rs instanceof LoadDependentRouting) {
						LoadDependentRouting ld = (LoadDependentRouting) rs;
						ld.refreshRoutingOnStationDeletion(getStationName(key));
					}
					if (fs != null) {
						fs.removeStation(key);
					}
				}
			}

			StationData sd = (StationData) stationDataHM.get(key);
			if (sd.blockingRegion != null) {
				removeRegionStation(sd.blockingRegion, key);
			}
			stationsKeyset.remove(key);
			stationDataHM.remove(key);
			deleteStationConnections(key);
			deleteStationDetails(key);
			deleteStationMeasures(key);
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
	 * Sets the station parameters by default.
	 */
	private void setDefaultsForStation(StationData sd) {
		String queueStrategy = QUEUE_STRATEGY_STATION_QUEUE;
		if (sd.type.equals(STATION_TYPE_SERVER)) {
			queueStrategy = Defaults.get("stationStationQueueStrategy");
		} else if (sd.type.equals(STATION_TYPE_FORK)) {
			queueStrategy = Defaults.get("stationStationQueueStrategy");
			if (queueStrategy.equals(QUEUE_STRATEGY_STATION_PS) || queueStrategy.equals(QUEUE_STRATEGY_STATION_SRPT)) {
				queueStrategy = QUEUE_STRATEGY_STATION_QUEUE;
			}
		}

		if (sd.type.equals(STATION_TYPE_SOURCE)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_SINK)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_TERMINAL)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_ROUTER)) {
			sd.queueCapacity = Integer.valueOf(-1);
			sd.queueStrategy = queueStrategy;
		} else if (sd.type.equals(STATION_TYPE_DELAY)) {
			sd.queueCapacity = Integer.valueOf(-1);
			sd.queueStrategy = queueStrategy;
			sd.numOfServers = Integer.valueOf(-1);
		} else if (sd.type.equals(STATION_TYPE_SERVER)) {
			sd.queueCapacity = Defaults.getAsInteger("stationCapacity");
			sd.queueStrategy = queueStrategy;
			sd.numOfServers = Defaults.getAsInteger("stationServers");
		} else if (sd.type.equals(STATION_TYPE_FORK)) {
			sd.queueCapacity = Defaults.getAsInteger("stationCapacity");
			sd.queueStrategy = queueStrategy;
			sd.forkBlock = Defaults.getAsInteger("forkBlock");
			sd.isSimplifiedFork = Boolean.parseBoolean(Defaults.get("isSimplifiedFork"));
			sd.numOfServers = Defaults.getAsInteger("forkJobsPerLink");
		} else if (sd.type.equals(STATION_TYPE_JOIN)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_LOGGER)) {
			sd.queueCapacity = Integer.valueOf(-1);
			sd.queueStrategy = queueStrategy;
		} else if (sd.type.equals(STATION_TYPE_CLASSSWITCH)) {
			sd.queueCapacity = Integer.valueOf(-1);
			sd.queueStrategy = queueStrategy;
		} else if (sd.type.equals(STATION_TYPE_SEMAPHORE)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_SCALER)) {
			sd.forkBlock = Defaults.getAsInteger("forkBlock");
			sd.isSimplifiedFork = Boolean.parseBoolean(Defaults.get("isSimplifiedFork"));
			sd.numOfServers = Defaults.getAsInteger("forkJobsPerLink");
		} else if (sd.type.equals(STATION_TYPE_PLACE)) {
			sd.queueCapacity = Defaults.getAsInteger("placeCapacity");
			sd.queueStrategy = queueStrategy;
		} else if (sd.type.equals(STATION_TYPE_TRANSITION)) {
			sd.transitionModeList = new ArrayList<Object>();
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
	 * @return FINITE_DROP || FINITE_BLOCK || FINITE_WAITING
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
	 * @param dropRule: FINITE_DROP || FINITE_BLOCK || FINITE_WAITING
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
	public Object getLoggingParameters(Object stationKey) {
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
	public void setLoggingParameters(Object stationKey, Object loggerParameters) {
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

		if (selector.equalsIgnoreCase("path") == true) {
			return loggerGlbParams.path;
		} else if (selector.equalsIgnoreCase("delim") == true) {
			return loggerGlbParams.delimiter;
		} else if (selector.equalsIgnoreCase("decimalSeparator") == true) {
			return loggerGlbParams.decimalSeparator;
		} else if (selector.equalsIgnoreCase("autoAppend") == true) {
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

		// debugLog.debug("glbParameter <" + selector + "=" + value + "> assigned by " + (new Exception().getStackTrace()[1]).toString().substring(19));
		debugLog.debug("glbParameter <" + selector + "=" + value + ">");

		if (selector.equalsIgnoreCase("path") == true) {
			loggerGlbParams.path = value;
		} else if (selector.equalsIgnoreCase("decimalSeparator") == true) {
			loggerGlbParams.decimalSeparator = value;
		} else if (selector.equalsIgnoreCase("delim") == true) {
			loggerGlbParams.delimiter = value;
		} else if (selector.equalsIgnoreCase("autoAppend") == true) {
			loggerGlbParams.autoAppendMode = new Integer(value);
		} else {
			debugLog.error("No such selector " + selector + " for " + new Exception().getStackTrace()[0] + "\n" + new Exception().getStackTrace()[1]);
			return;
		}
		save = true;
	}

	/**
	 * Used by jmodel.controller.Mediator to get the <i>files that could be overwritten</i>,
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
			LoggerParameters lp = (LoggerParameters) getLoggingParameters(loggerKey);
			if (lp.name.equalsIgnoreCase("global") == false) {
				lknames[size++] = lp.name;
			} else if ((lp.name.equalsIgnoreCase("global") == true) && (globalWasProcessed == false)) {
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
	public void normalizeRoutingProbabilities(Object stationKey, Object classKey, Map<Object,Double> values) {
		Vector<Object> outputs = getForwardConnections(stationKey);
		Vector<Object> normalOutputs = new Vector<Object>();
		normalOutputs.addAll(outputs);
		if (getClassType(classKey) == CLASS_TYPE_CLOSED) {
			boolean allSink = true;
			for (int i = 0; i < outputs.size(); i++) {
				if (getStationType(outputs.get(i)).equals(STATION_TYPE_SINK)) {
					normalOutputs.remove(outputs.get(i));
				} else {
					allSink = false;
				}
			}
			if (allSink) {
				normalOutputs.addAll(outputs);
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
				if (probabilities[i].doubleValue() > 0.0) {
					probabilities[i] = new Double(0.0);
					String className = getClassName(classKey);
					String stationName = getStationName(stationKey);
					if (!sinkProbabilityUpdateClasses.contains(className)) {
						sinkProbabilityUpdateClasses.add(className);
					}
					if (!sinkProbabilityUpdateStations.contains(stationName)) {
						sinkProbabilityUpdateStations.add(stationName);
					}	        	
					sinkProbabilityUpdate = true;
				}
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
			if (probabilities[i] != null) {
				totalSum += probabilities[i].doubleValue();
			}
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
	 * Adds station details for a new station, given the search key.
	 */
	protected void addNewStationDetails(Object key) {
		StationData sd = (StationData) stationDataHM.get(key);
		for (Object classKey : classesKeyset) {
			stationDetailsBDM.put(classKey, key, getDefaultDetailsForStationType(sd));
		}
	}

	/**
	 * Adds station details for a new class, given the search key.
	 */
	protected void addStationDetailsForClass(Object key) {
		for (Object stationKey : stationsKeyset) {
			StationData sd = (StationData) stationDataHM.get(stationKey);
			StationClassData scd = getDefaultDetailsForStationType(sd);
			stationDetailsBDM.put(key, stationKey, scd);
		}
	}

	/**
	 * Deletes station details for a station, given the search key.
	 */
	protected void deleteStationDetails(Object key) {
		stationDetailsBDM.remove(key, BDMap.Y);
	}

	/**
	 * Deletes station details for a class, given the search key.
	 */
	protected void deleteStationDetailsForClass(Object key) {
		stationDetailsBDM.remove(key, BDMap.X);
		for (Object stationKey : stationsKeyset) {
			for (Object classKey : classesKeyset) {
				Object strategy = getForkStrategy(stationKey, classKey);
				if (strategy instanceof ClassSwitchFork) {
					Map<Object, OutPath> m = ((ClassSwitchFork) strategy).getOutDetails();
					for (OutPath o : m.values()) {
						o.getOutParameters().remove(key);
					}
				}
				if (strategy instanceof MultiBranchClassSwitchFork) {
					Map<Object, OutPath> m = ((MultiBranchClassSwitchFork) strategy).getOutDetails();
					for (OutPath o : m.values()) {
						o.getOutParameters().remove(key);
					}
				}
				strategy = getJoinStrategy(stationKey, classKey);
				if (strategy instanceof GuardJoin) {
					((GuardJoin) strategy).getGuard().remove(key);
				}
			}
		}
	}

	/**
	 * Generates a station-class data structure by default.
	 */
	private StationClassData getDefaultDetailsForStationType(StationData sd) {
		String queueStrategy = QUEUE_STRATEGY_FCFS;
		if (sd.type.equals(STATION_TYPE_SERVER)) {
			queueStrategy = Defaults.get("stationQueueStrategy");
		} else if (sd.type.equals(STATION_TYPE_FORK)) {
			queueStrategy = Defaults.get("stationQueueStrategy");
			if (queueStrategy.equals(QUEUE_STRATEGY_SJF) || queueStrategy.equals(QUEUE_STRATEGY_LJF)) {
				queueStrategy = QUEUE_STRATEGY_FCFS;
			}
		}

		StationClassData scd = new StationClassData();
		if (sd.type.equals(STATION_TYPE_SOURCE)) {
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
		} else if (sd.type.equals(STATION_TYPE_SINK)) {
			// Do nothing
		} else if (sd.type.equals(STATION_TYPE_TERMINAL)) {
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
		} else if (sd.type.equals(STATION_TYPE_ROUTER)) {
			scd.queueStrategy = queueStrategy;
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.dropRule = Defaults.get("dropRule");
		} else if (sd.type.equals(STATION_TYPE_DELAY)) {
			scd.queueStrategy = queueStrategy;
			scd.serviceDistribution = Defaults.getAsNewInstance("stationDelayServiceStrategy");
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.dropRule = Defaults.get("dropRule");
		} else if (sd.type.equals(STATION_TYPE_SERVER)) {
			scd.queueStrategy = queueStrategy;
			scd.serviceDistribution = Defaults.getAsNewInstance("stationServiceStrategy");
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.dropRule = Defaults.get("dropRule");
		} else if (sd.type.equals(STATION_TYPE_FORK)) {
			scd.queueStrategy = queueStrategy;
			scd.dropRule = Defaults.get("dropRule");
			scd.forkStrategy = Defaults.getAsNewInstance("stationForkStrategy");
		} else if (sd.type.equals(STATION_TYPE_JOIN)) {
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.joinStrategy = Defaults.getAsNewInstance("stationJoinStrategy");
		} else if (sd.type.equals(STATION_TYPE_LOGGER)) {
			scd.queueStrategy = queueStrategy;
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.dropRule = Defaults.get("dropRule");
		} else if (sd.type.equals(STATION_TYPE_CLASSSWITCH)) {
			scd.queueStrategy = queueStrategy;
			scd.routingStrategy = Defaults.getAsNewInstance("stationRoutingStrategy");
			scd.classSwitchProb = new ClassSwitchRow();
			scd.dropRule = Defaults.get("dropRule");
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
		return scd;
	}

	/**
	 * Manages reference station for every class.
	 */
	private void manageRefStations() {
		Vector<Object> openRefCandidateKeys = getStationKeysRefSource();
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
		//element does not exist, no further operation possible, return.
		if (toSet == null) {
			return false;
		}
		//exists but is not connectable, return.
		if (!toSet.isConnectable) {
			return false;
		}
		//has already been connected: cannot be connected twice, but can be disconnected
		if (toSet.isConnected) {
			//must disconnect
			if (!areConnected) {
				toSet.isConnected = false;
				save = true;
				for (Object classKey: classesKeyset) {
					ForkStrategy fs = (ForkStrategy) getForkStrategy(sourceKey, classKey);
					if (fs != null) {
						fs.removeStation(targetKey);
					}
				}
				return true;
			}
		} else {
			//must connect
			if (areConnected) {
				toSet.isConnected = true;
				save = true;
				for (Object classKey: classesKeyset) {
					ForkStrategy fs = (ForkStrategy) getForkStrategy(sourceKey, classKey);
					if (fs != null) {
						fs.addStation(getStationName(targetKey), targetKey, classKey, classesKeyset);
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
	 * @param stationKey: source station for which (target)connected stations must be
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
	 * @param stationKey: source station for which (source)connected stations must be
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
		//no outgoing connections from sink
		if (source.type.equals(STATION_TYPE_SINK)) {
			return false;
		}
		// No connection between a router, fork, join, logger, semaphore, scaler, place or transition and itself
		if (sourceKey == targetKey
				&& (source.type.equals(STATION_TYPE_ROUTER) || source.type.equals(STATION_TYPE_FORK) || source.type.equals(STATION_TYPE_JOIN)
						|| source.type.equals(STATION_TYPE_LOGGER) || source.type.equals(STATION_TYPE_SEMAPHORE) || source.type.equals(STATION_TYPE_SCALER)
						|| source.type.equals(STATION_TYPE_PLACE) || source.type.equals(STATION_TYPE_TRANSITION))) {
			return false;
		}
		//no direct connections from source to sink
		//no loops on terminal
		//no direct connections between terminal and source or sink
		if ((source.type.equals(STATION_TYPE_SOURCE) || source.type.equals(STATION_TYPE_SINK) || source.type.equals(STATION_TYPE_TERMINAL))
				&& (target.type.equals(STATION_TYPE_SOURCE) || target.type.equals(STATION_TYPE_SINK) || target.type.equals(STATION_TYPE_TERMINAL))) {
			return false;
		}
		// A place can only be connected to a transition.
		// A transition can only be connected from a place.
		if ((source.type.equals(STATION_TYPE_PLACE) && !target.type.equals(STATION_TYPE_TRANSITION))
				|| (target.type.equals(STATION_TYPE_TRANSITION) && !source.type.equals(STATION_TYPE_PLACE))) {
			return false;
		}
		return true;
	}

	/*Creates all of the entries inside connection data repository for a new station.*/
	private void addNewStationConnections(Object stationKey) {
		//first add all of the forward connections
		//picking all possible targets
		Vector<Object> keys = new Vector<Object>(connectionsBDM.keySet(BDMap.X));
		int size = keys.size();
		Map<Object, Connection> conns = new HashMap<Object, Connection>();
		//building set of forward connections
		for (int i = 0; i < size; i++) {
			//current station key
			Object current = keys.get(i);
			conns.put(current, new Connection(canBeConnected(stationKey, current), false));
		}
		connectionsBDM.put(stationKey, BDMap.Y, conns);
		//reset all data structures for bw connections addition
		//picking all possible sources
		keys = new Vector<Object>(connectionsBDM.keySet(BDMap.Y));
		size = keys.size();
		conns = new HashMap<Object, Connection>();
		//building set of backward connections
		for (int i = 0; i < size; i++) {
			//current station key
			Object current = keys.get(i);
			conns.put(current, new Connection(canBeConnected(current, stationKey), false));
		}
		connectionsBDM.put(stationKey, BDMap.X, conns);
	}

	//deletes connections for deleted station
	private void deleteStationConnections(Object stationKey) {
		connectionsBDM.remove(stationKey, BDMap.X);
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
		MeasureData md = new MeasureData(stationKey, classKey, type, alpha, precision, log);
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

	private void deleteClassMeasures(Object key) {
		for (Object measureKey : measuresKeyset.toArray()) {
			if (getMeasureClass(measureKey) == key) {
				removeMeasure(measureKey);
			}
		}
	}

	private void deleteStationMeasures(Object key) {
		for (Object measureKey : measuresKeyset.toArray()) {
			if (getMeasureStation(measureKey) == key) {
				removeMeasure(measureKey);
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

	/**
	 * Tells if a given measure is global
	 * @param key search's key for given measure
	 * @return true if measure is global
	 */
	@Override
	public boolean isGlobalMeasure(Object key) {
		String type = getMeasureType(key);
		return type.equals(MEASURE_S_X) || type.equals(MEASURE_S_RP) || type.equals(MEASURE_S_CN)
				|| type.equals(MEASURE_S_DR) || type.equals(MEASURE_S_P);
	}

	/**
	 * Tells if a given measure is for sinks only
	 * @param key search's key for given measure
	 * @return true if measure is for sinks only
	 */
	@Override
	public boolean isSinkMeasure(Object key) {
		String type = getMeasureType(key);
		return type.equals(MEASURE_RP_PER_SINK) || type.equals(MEASURE_X_PER_SINK);
	}

	/**
	 * Tells if a given measure is for blocking regions only
	 * @param key search's key for given measure
	 * @return true if measure is for blocking regions only
	 */
	@Override
	public boolean isFCRMeasure(Object key) {
		String type = getMeasureType(key);
		return type.equals(MEASURE_FCR_TW) || type.equals(MEASURE_FCR_MO);
	}

	/**
	 * Tells if a given measure is for fork/join sections only
	 * @param key search's key for given measure
	 * @return true if measure is for fork/join sections only
	 */
	@Override
	public boolean isFJMeasure(Object key) {
		String type = getMeasureType(key);
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
	 *
	 * @param name name of new blocking region
	 * @param type type of new blocking region
	 * @return search's key for new blocking region
	 */
	@Override
	public Object addBlockingRegion(String name, String type) {
		Object key = new Long(++incrementalKey);
		//If this blocking region has already been created, do not add search key to the list.
		if (!blockingRegionsKeyset.contains(key)) {
			blockingRegionsKeyset.add(key);
		}
		BlockingRegionData newBlocking = new BlockingRegionData(name, type, Defaults.getAsInteger("blockingMaxJobs"), Defaults.getAsInteger("blockingMaxMemory"));
		blockingDataHM.put(key, newBlocking);
		// Adds region-class specific data
		addBlockingDetails(key);
		save = true;
		return key;
	}

	/**
	 * Removes a blocking region from the model
	 *
	 * @param key Search's key for region to be deleted
	 */
	@Override
	public void deleteBlockingRegion(Object key) {
		if (blockingRegionsKeyset.contains(key)) {
			BlockingRegionData bd = (BlockingRegionData) blockingDataHM.remove(key);
			// Removes blocking region from station reference
			Iterator<Object> it = bd.stations.iterator();
			while (it.hasNext()) {
				Object stationKey = it.next();
				((StationData) stationDataHM.get(stationKey)).blockingRegion = null;
				// Adds this station to blockable ones
				if (canStationTypeBeBlocked(((StationData) stationDataHM.get(stationKey)).type)) {
					blockableKeyset.add(stationKey);
				}
			}
			blockingRegionsKeyset.remove(key);
			deleteBlockingDetails(key);
			save = true;
		}
	}

	/**
	 * Adds a new station to specified blocking region
	 *
	 * @param regionKey  Search's key for region
	 * @param stationKey Search's key for station
	 * @return true if station can be put inside specified region
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
	 * Sets the name of a blocking region
	 *
	 * @param regionKey Search's key for region
	 * @param name      name of the blocking region
	 */
	@Override
	public void setRegionName(Object regionKey, String name) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			if (!((BlockingRegionData) blockingDataHM.get(regionKey)).name.equals(name)) {
				save = true;
			}
			((BlockingRegionData) blockingDataHM.get(regionKey)).name = name;
		}
	}

	/**
	 * Returns the name of a blocking region
	 *
	 * @param regionKey Search's key for region
	 * @return name of the blocking region
	 */
	@Override
	public String getRegionName(Object regionKey) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			return ((BlockingRegionData) blockingDataHM.get(regionKey)).name;
		} else {
			return null;
		}
	}

	/**
	 * Sets the type of a blocking region
	 *
	 * @param regionKey Search's key for region
	 * @param type      type of the blocking region
	 */
	@Override
	public void setRegionType(Object regionKey, String type) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			if (!((BlockingRegionData) blockingDataHM.get(regionKey)).type.equals(type)) {
				save = true;
			}
			((BlockingRegionData) blockingDataHM.get(regionKey)).type = type;
		}
	}

	/**
	 * Returns the type of a blocking region
	 *
	 * @param regionKey Search's key for region
	 * @return type of the blocking region
	 */
	@Override
	public String getRegionType(Object regionKey) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			return ((BlockingRegionData) blockingDataHM.get(regionKey)).type;
		} else {
			return null;
		}
	}

	/**
	 * Tells if a station can be added to specified blocking region
	 * (all stations that creates or destroy jobs cannot be added)
	 * @param regionKey  Search's key for region
	 * @param stationKey Search's key for station
	 * @return true if station can be put inside specified region
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
	 * Tells if a given station type can be added to a blocking region
	 * @param stationType type of station to be added
	 * @return true if station of that type can be added, false otherwise
	 */
	protected boolean canStationTypeBeBlocked(String stationType) {
		return !stationType.equals(STATION_TYPE_SOURCE) && !stationType.equals(STATION_TYPE_SINK)
				&& !stationType.equals(STATION_TYPE_TERMINAL) && !stationType.equals(STATION_TYPE_FORK)
				&& !stationType.equals(STATION_TYPE_JOIN) && !stationType.equals(STATION_TYPE_CLASSSWITCH)
				&& !stationType.equals(STATION_TYPE_SEMAPHORE) && !stationType.equals(STATION_TYPE_SCALER)
				&& !stationType.equals(STATION_TYPE_TRANSITION);
	}

	/**
	 * Removes a station from a specified blocking region
	 *
	 * @param regionKey  Search's key for region
	 * @param stationKey Search's key for station
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
	 * Returns the entire set of blocking region keys
	 *
	 * @return the entire set of blocking region keys
	 */
	@Override
	public Vector<Object> getRegionKeys() {
		return blockingRegionsKeyset;
	}

	/**
	 * Sets a customer number constraint for a given region and an user class
	 *
	 * @param regionKey       search's key for blocking region
	 * @param classKey        search's key for customer class
	 * @param maxJobsPerClass maximum number of allowed customer. -1 means infinity.
	 */
	@Override
	public void setRegionClassCustomerConstraint(Object regionKey, Object classKey, Integer maxJobsPerClass) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (!((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs.equals(maxJobsPerClass)) {
			save = true;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs = maxJobsPerClass;
	}

	/**
	 * Sets a drop rule for a given region and an user class
	 *
	 * @param regionKey search's key for blocking region
	 * @param classKey  search's key for customer class
	 * @param drop      true if jobs of specified class can be dropped, false otherwise
	 */
	@Override
	public void setRegionClassDropRule(Object regionKey, Object classKey, Boolean drop) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (!((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).drop.equals(drop)) {
			save = true;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).drop = drop;
	}

	/**
	 * Sets a global customer number constraint for a given region
	 *
	 * @param regionKey       search's key for blocking region
	 * @param maxJobs maximum number of allowed customer. -1 means infinity.
	 */
	@Override
	public void setRegionCustomerConstraint(Object regionKey, Integer maxJobs) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (!((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs.equals(maxJobs)) {
			save = true;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs = maxJobs;
	}

	/**
	 * Gets customer number constraint for a given region and an user class
	 *
	 * @param regionKey search's key for blocking region
	 * @param classKey  search's key for customer class
	 * @return maximum number of allowed customer. -1 means infinity.
	 */
	@Override
	public Integer getRegionClassCustomerConstraint(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).maxJobs;
	}

	/**
	 * Gets drop rule for a given region and an user class
	 *
	 * @param regionKey search's key for blocking region
	 * @param classKey  search's key for customer class
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
	 * Gets global customer number constraint for a given region
	 *
	 * @param regionKey search's key for blocking region
	 * @return maximum number of allowed customer. -1 means infinity.
	 */
	@Override
	public Integer getRegionCustomerConstraint(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).maxJobs;
	}

	/**
	 * Returns a list of every station inside a blocking region
	 *
	 * @param regionKey search's key for given blocking region
	 * @return a set with every station inside given blocking region
	 */
	@Override
	public Set<Object> getBlockingRegionStations(Object regionKey) {
		if (blockingRegionsKeyset.contains(regionKey)) {
			return ((BlockingRegionData) blockingDataHM.get(regionKey)).stations;
		} else {
			return null;
		}
	}

	/**
	 * Gets blocking region of a given station
	 *
	 * @param stationKey search's key for given station
	 * @return search's key for its blocking region or null if it is undefined
	 */
	@Override
	public Object getStationBlockingRegion(Object stationKey) {
		if (stationsKeyset.contains(stationKey)) {
			return ((StationData) stationDataHM.get(stationKey)).blockingRegion;
		} else {
			return null;
		}
	}

	/**
	 * Gets a vector with every station that can be added to a blocking region
	 *
	 * @return a vector of search's keys of every station that can be added
	 * to a blocking region
	 */
	@Override
	public Vector<Object> getBlockableStationKeys() {
		return blockableKeyset;
	}

	/**
	 * Adds default values for newly created class
	 * @param classKey search's key for created class
	 */
	protected void addBlockingDetailsForClass(Object classKey) {
		for (Object regionKey : blockingRegionsKeyset) {
			blockingDetailsBDM.put(classKey, regionKey, new BlockingClassData(Defaults.getAsInteger("blockingMaxJobsPerClass"),
					Defaults.getAsBoolean("blockingDropPerClass"), Defaults.getAsInteger("weight"),Defaults.getAsInteger("size")));
		}
	}

	/**
	 * Adds default values for newly created blocking regions
	 * @param blockingRegionKey search's key for created blocking region
	 */
	protected void addBlockingDetails(Object blockingRegionKey) {
		for (Object classKey : classesKeyset) {
			blockingDetailsBDM.put(classKey, blockingRegionKey, new BlockingClassData(Defaults.getAsInteger("blockingMaxJobsPerClass"),
					Defaults.getAsBoolean("blockingDropPerClass"), Defaults.getAsInteger("weight"), Defaults.getAsInteger("size")));
		}
	}

	/**
	 * Deletes blocking region details about a certain class, given its search key
	 */
	protected void deleteBlockingDetailsForClass(Object classKey) {
		blockingDetailsBDM.remove(classKey, BDMap.X);
	}

	/**
	 * Deletes blocking region details from repository, given station search key.
	 */
	protected void deleteBlockingDetails(Object blockingRegionKey) {
		blockingDetailsBDM.remove(blockingRegionKey, BDMap.Y);
	}

	@Override
	public Integer getRegionMemorySize(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory;
	}

	@Override
	public void setRegionMemorySize(Object regionKey, Integer memorySize) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (!((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory.equals(memorySize)) {
			save = true;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).maxMemory = memorySize;
	}

	@Override 
	public Integer getRegionClassWeight(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight;
	}

	@Override
	public void setRegionClassWeight(Object regionKey, Object classKey, Integer weight) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (!((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight.equals(weight)) {
			save = true;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).weight = weight;
	}

	@Override 
	public Integer getRegionClassSize(Object regionKey, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return null;
		}
		return ((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size;
	}

	@Override
	public void setRegionClassSize(Object regionKey, Object classKey, Integer size) {
		if (!blockingRegionsKeyset.contains(regionKey) || !classesKeyset.contains(classKey)) {
			return;
		}
		if (!((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size.equals(size)) {
			save = true;
		}
		((BlockingClassData) blockingDetailsBDM.get(classKey, regionKey)).size = size;
	}

	@Override
	public List<BlockingGroupData> getRegionGroupList(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList;
	}

	@Override
	public void addRegionGroup(Object regionKey, String name, Integer capacity, String strategy) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.add(new BlockingGroupData(name, capacity, strategy));
		save = true;
	}

	@Override
	public void deleteRegionGroup(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.remove(index);
		save = true;
	}

	@Override
	public void deleteAllRegionGroups(Object regionKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.clear();
		save = true;
	}

	@Override
	public String getRegionGroupName(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).name;
	}

	@Override
	public void setRegionGroupName(Object regionKey, int index, String name) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).name = name;
		save = true;
	}

	@Override
	public Integer getRegionGroupCapacity(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).maxJobs;
	}

	@Override
	public void setRegionGroupCapacity(Object regionKey, int index, Integer maxJobs) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).maxJobs = maxJobs;
		save = true;
	}

	@Override
	public String getRegionGroupStrategy(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).strategy;
	}

	@Override
	public void setRegionGroupStrategy(Object regionKey, int index, String strategy) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).strategy = strategy;
		save = true;
	}

	@Override
	public List<Object> getRegionGroupClassList(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return null;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return null;
		}
		return ((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList;
	}

	@Override
	public void addClassIntoRegionGroup(Object regionKey, int index, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.contains(classKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.add(classKey);
		save = true;
	}

	@Override
	public void deleteClassFromRegionGroup(Object regionKey, int index, Object classKey) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		if (!((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.contains(classKey)) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.remove(classKey);
		save = true;
	}

	@Override
	public void deleteAllClassesFromRegionGroup(Object regionKey, int index) {
		if (!blockingRegionsKeyset.contains(regionKey)) {
			return;
		}
		if (((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.size() <= index) {
			return;
		}
		((BlockingRegionData) blockingDataHM.get(regionKey)).GroupList.get(index).classList.clear();
		save = true;
	}

	// --- end Blocking Region Definition ------------------------------------------------------------

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
		public String queueStrategy;
		public Integer numOfServers;
		public Integer forkBlock;
		public Boolean isSimplifiedFork;
		public Object loggerParameters;
		public List<Object> transitionModeList;
		/** Reference to owner blocking station or null */
		public Object blockingRegion;

		public StationData(String name, String type) {
			this.name = name;
			this.type = type;
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
	}

	/**
	 * This class is used to store blocking region data
	 */
	protected class BlockingRegionData {
		public String name;
		public String type;
		public Integer maxJobs;
		public Integer maxMemory;
		public List<BlockingGroupData> GroupList;

		/** Reference to owned stations */
		public HashSet<Object> stations;

		public BlockingRegionData(String name, String type, Integer maxJobs, Integer maxMemory) {
			this.name = name;
			this.type = type;
			this.maxJobs = maxJobs;
			this.maxMemory = maxMemory;
			stations = new HashSet<Object>();
			GroupList = new ArrayList<BlockingGroupData>();
		}
	}

	/**
	 * This class is used to store blocking region / group data
	 */
	public class BlockingGroupData {
		public String name;
		public Integer maxJobs;
		public String strategy;
		public List<Object> classList;

		public BlockingGroupData(String name,Integer maxJobs,String strategy) {
			this.name = name;
			this.maxJobs = maxJobs;
			this.strategy = strategy;
			classList = new ArrayList<Object>();
		}
	}

	/**
	 * This class is used to store blocking region / class data
	 */
	protected class BlockingClassData {
		public Integer maxJobs;
		public Boolean drop;
		public Integer weight;
		public Integer size;

		public BlockingClassData(Integer maxJobs, Boolean drop, Integer weight, Integer size) {
			this.maxJobs = maxJobs;
			this.drop = drop;
			this.weight = weight;
			this.size = size;
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

	/**This class contains all of the data which can be defined for a combination of
	 * class and station. These data include queue policy, service time distribution
	 * and routing strategy*/
	protected class StationClassData {
		public Integer queueCapacity;
		public String queueStrategy;
		public String dropRule;
		public Object serviceDistribution;
		public Object routingStrategy;
		public Integer preload;
		public Object classSwitchProb;
		public Object forkStrategy;
		public Object joinStrategy;
		public Object semaphoreStrategy;

		public StationClassData() {
			this.preload = new Integer(0);
		}
	}

	/**This class contains all of the parameters to define a simulation measure, e.g.
	 * reference userClass, reference station, type (Throughput, Residence Time, ecc)
	 * precision, confidence interval */
	protected class MeasureData {
		public Object stationKey;
		public Object classKey;
		public String type;
		public Double precision;
		public Double alpha;
		public boolean log; 

		public MeasureData(Object station, Object userClass, String measureType, Double alpha, Double precision, boolean log) {
			stationKey = station;
			classKey = userClass;
			type = measureType;
			this.precision = precision;
			this.alpha = alpha;
			this.log = log ; 
		}
	}

	protected class LoggerGlobalParameters {
		public String path;
		public String delimiter;
		public String decimalSeparator;
		public Integer autoAppendMode;

		LoggerGlobalParameters() {
			path = MacroReplacer.replace(MacroReplacer.MACRO_WORKDIR);
			delimiter = Defaults.get("loggerDelimiter");
			decimalSeparator = Defaults.get("loggerDecimalSeparator");
			autoAppendMode = Defaults.getAsInteger("loggerAutoAppend");
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
			enablingCondition = new TransitionMatrix();
			inhibitingCondition = new TransitionMatrix();
			numberOfServers = Defaults.getAsInteger("transitionNumberOfServers");
			firingTimeDistribution = Defaults.getAsNewInstance("transitionFiringTimeDistribution");
			firingPriority = Defaults.getAsInteger("transitionFiringPriority");
			firingWeight = Defaults.getAsDouble("transitionFiringWeight");
			firingOutcome = new TransitionMatrix();
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

	@Override
	public boolean isSinkProbabilityUpdated() {
		return sinkProbabilityUpdate;
	}

	@Override
	public void setSinkProbabilityUpdatedVar(boolean param) {
		sinkProbabilityUpdate = param;
	}

	@Override
	public Vector<String> getSinkProbabilityUpdateClasses() {
		return sinkProbabilityUpdateClasses;
	}

	@Override
	public Vector<String> getSinkProbabilityUpdateStations() {
		return sinkProbabilityUpdateStations;
	}

	@Override
	public void resetSinkProbabilityUpdateStations() {
		sinkProbabilityUpdateStations = new Vector<String>();
	}

	@Override
	public void resetSinkProbabilityUpdateClasses() {
		sinkProbabilityUpdateClasses = new Vector<String>();
	}

	/**
	 * Returns the cell (<code>classInKey</code>, <code>classOutKey</code>) of the class
	 * switch matrix for station <code>stationKey</code>.
	 */
	@Override
	public float getClassSwitchMatrix(Object stationKey, Object classInKey, Object classOutKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classInKey, stationKey);
		ClassSwitchRow row = (ClassSwitchRow) scd.classSwitchProb;
		return row.getValue(classInKey, classOutKey);
	}

	/**
	 * Sets the cell (<code>classInKey</code>, <code>classOutKey</code>) of the class
	 * switch matrix for station <code>stationKey</code>.
	 */
	@Override
	public void setClassSwitchMatrix(Object stationKey, Object classInKey, Object classOutKey, float value) {
		if (value != getClassSwitchMatrix(stationKey, classInKey, classOutKey))
			save = true;
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classInKey, stationKey);
		ClassSwitchRow row = (ClassSwitchRow) scd.classSwitchProb;
		row.setValue(classInKey, classOutKey, value);
	}

	/**
	 * Returns the threshold for semaphore <code>stationKey</code> and class <code>classKey</code>.
	 */
	@Override
	public Integer getSemaphoreThreshold(Object stationKey, Object classKey) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		SemaphoreStrategy ss =  (SemaphoreStrategy) scd.semaphoreStrategy;
		return Integer.valueOf(ss.getThreshold());
	}

	/**
	 * Sets the threshold for semaphore <code>stationKey</code> and class <code>classKey</code>.
	 */
	@Override
	public void setSemaphoreThreshold(Object stationKey, Object classKey, Integer threshold) {
		StationClassData scd = (StationClassData) stationDetailsBDM.get(classKey, stationKey);
		SemaphoreStrategy ss =  (SemaphoreStrategy) scd.semaphoreStrategy;
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		return tmd.name;
	}

	/**
	 * Returns the names of all the modes for transition <code>stationKey</code>.
	 */
	@Override
	public List<String> getAllTransitionModeNames(Object stationKey) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < sd.transitionModeList.size(); i++) {
			list.add(((TransitionModeData) sd.transitionModeList.get(i)).name);
		}
		return list;
	}

	/**
	 * Sets the name of mode <code>modeIndex</code> for transition <code>stationKey</code>.
	 */
	@Override
	public void setTransitionModeName(Object stationKey, int modeIndex, String name) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		if (!tmd.name.equals(name)) {
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		return tmd.enablingCondition.getEntry(stationInKey, classKey);
	}

	/**
	 * Sets the entry (<code>stationInKey</code>, <code>classKey</code>) of the enabling
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public void setEnablingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey, Integer value) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		return tmd.inhibitingCondition.getEntry(stationInKey, classKey);
	}

	/**
	 * Sets the entry (<code>stationInKey</code>, <code>classKey</code>) of the inhibiting
	 * condition for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public void setInhibitingCondition(Object stationKey, int modeIndex, Object stationInKey, Object classKey, Integer value) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		return tmd.numberOfServers;
	}

	/**
	 * Sets the number of servers for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public void setNumberOfServers(Object stationKey, int modeIndex, Integer number) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		return tmd.firingTimeDistribution;
	}

	/**
	 * Sets the firing time distribution for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public void setFiringTimeDistribution(Object stationKey, int modeIndex, Object distribution) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		return tmd.firingPriority;
	}

	/**
	 * Sets the firing priority for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public void setFiringPriority(Object stationKey, int modeIndex, Integer priority) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		return tmd.firingWeight;
	}

	/**
	 * Sets the firing weight for transition <code>stationKey</code> and mode
	 * <code>modeIndex</code>.
	 */
	@Override
	public void setFiringWeight(Object stationKey, int modeIndex, Double weight) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
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
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		return tmd.firingOutcome.getEntry(stationOutKey, classKey);
	}

	/**
	 * Sets the entry (<code>stationOutKey</code>, <code>classKey</code>) of the firing
	 * outcome for transition <code>stationKey</code> and mode <code>modeIndex</code>.
	 */
	@Override
	public void setFiringOutcome(Object stationKey, int modeIndex, Object stationOutKey, Object classKey, Integer value) {
		StationData sd = (StationData) stationDataHM.get(stationKey);
		TransitionModeData tmd = (TransitionModeData) sd.transitionModeList.get(modeIndex);
		if (!tmd.firingOutcome.getEntry(stationOutKey, classKey).equals(value)) {
			tmd.firingOutcome.setEntry(stationOutKey, classKey, value);
			save = true;
		}
	}

	/**
	 * Returns given name if a mode with the same name does not exist or makes it unique
	 * for a transition
	 * @param key station key
	 * @param name mode name
	 * @return unique name
	 */
	private String getUniqueTransitionModeName(Object key, String name) {
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

}
