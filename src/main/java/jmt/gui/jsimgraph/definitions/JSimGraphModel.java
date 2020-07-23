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

package jmt.gui.jsimgraph.definitions;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.*;

import jmt.engine.log.LoggerParameters;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.distributions.Distribution;
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
import jmt.gui.jsimgraph.JSimGraphConstants;

/**
 * <p>Title: JMODEL-Model </p>
 * <p>Description: Main data structure used to store model definition data,
 * to save and load model and to compile xml files for the simulator and
 * as a bridge to connect JMODEL with JSIM and JMVA</p>
 *
 * @author Bertoli Marco
 * Date: 2-giu-2005
 * Time: 10.27.54
 *
 *
 * Modified by Francesco D'Aquino 11/11/2005
 */
//todo MMM JMVA bridge, undo/redo
public class JSimGraphModel extends CommonModel implements JmodelClassDefinition, JmodelStationDefinition, JmodelBlockingRegionDefinition, JSimGraphConstants {

	// ----- Variables -------------------------------------------------------------
	protected HashMap<String, Integer> objectNumber = new HashMap<String, Integer>(); // Used to generate progressive unique default names
	protected HashMap<Object, Color> classColor = new HashMap<Object, Color>(); // Used to store classes color
	protected HashMap<Object, JMTPoint> stationPositions = new HashMap<Object, JMTPoint>(); // Used to store station positions
	protected HashMap<Object, HashMap<Object,JMTPath>> connectionShapes = new HashMap<Object, HashMap<Object,JMTPath>>(); // Used to store connection shapes
	protected HashMap<Object, HashMap<Object, ArrayList<Point2D>>> absoluteControlPoints = new HashMap<Object, HashMap<Object,ArrayList<Point2D>>>(); // Used to store absolute values of displayed bezier shapes
	protected Color[] defaultColor = new Color[] { Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.CYAN, Color.YELLOW }; // Defaults color prompted when inserting new class (next ones are generated random)

	protected boolean animationEnabled = Defaults.getAsBoolean("isWithAnimation").booleanValue();

	// ----- Methods----------------------------------------------------------------
	/**
	 * Default constructor, creates a new instance of <code>JMODELModel</code>
	 */
	public JSimGraphModel() {
		super();
	}

	// ----- Class Definition Methods ---------------------------------------------------------------------
	/**
	 * Adds a new class to the model and sets all the parameters by default.
	 * @return search key for the new class.
	 */
	@Override
	public Object addClass() {
		int num = 0;
		if (!objectNumber.containsKey(Defaults.get("className"))) {
			num = classesKeyset.size();
		} else {
			num = objectNumber.get(Defaults.get("className")).intValue();
		}
		objectNumber.put(Defaults.get("className"), new Integer(++num));
		return addClass(Defaults.get("className") + num,
				Defaults.getAsInteger("classType").intValue(), Defaults.getAsInteger("classPriority"),
				Defaults.getAsInteger("classPopulation"), Defaults.getAsNewInstance("classDistribution"));
	}

	/**
	 * Adds a new class to the model and sets all the parameters at once.
	 * @param name name of the new class.
	 * @param type type of the new class.
	 * @param priority priority of the new class.
	 * @param population population of the new class.
	 * @param distribution inter-arrival time distribution of the new class.
	 * @return search key for the new class.
	 */
	@Override
	public Object addClass(String name, int type, Integer priority, Integer population, Object distribution) {
		Object key = super.addClass(name, type, priority, population, distribution);
		setClassColor(key, getNewColor());
		if (type == CLASS_TYPE_OPEN && !sourcesKeyset.isEmpty()) {
			setClassRefStation(key, sourcesKeyset.get(0));
		}
		return key;
	}

	/**
	 * Deletes a class from the model, given the search key.
	 */
	@Override
	public void deleteClass(Object key) {
		super.deleteClass(key);
		classColor.remove(key);
	}

	/**
	 * Sets type of the class, given the search key.
	 */
	@Override
	public void setClassType(Object key, int type) {
		if (getClassType(key) == type) {
			return;
		}
		super.setClassType(key, type);
		if (type == CLASS_TYPE_OPEN && !sourcesKeyset.isEmpty()) {
			setClassRefStation(key, sourcesKeyset.get(0));
		}
	}

	/**
	 * Returns color of the class, given the search key.
	 */
	@Override
	public Color getClassColor(Object key) {
		return classColor.get(key);
	}

	/**
	 * Sets color of the class, given the search key.
	 */
	@Override
	public void setClassColor(Object key, Color color) {
		classColor.put(key, color);
	}

	/**
	 * Returns a new color.
	 */
	public Color getNewColor() {
		int num = 0;
		if (objectNumber.containsKey(COLOR_NAME)) {
			num = objectNumber.get(COLOR_NAME).intValue();
		}
		objectNumber.put(COLOR_NAME, new Integer(++num));
		if (num <= defaultColor.length) {
			return defaultColor[num - 1];
		} else {
			return new Color((int) Math.floor(Math.random() * 256),
					(int) Math.floor(Math.random() * 256),
					(int) Math.floor(Math.random() * 256));
		}
	}

	/**
	 * Returns serialized form of a class.
	 * @param key search key for the class.
	 * @return serialized form of the class.
	 */
	@Override
	public Object serializeClass(Object key) {
		return new SerializedClass(key);
	}

	/**
	 * Inserts a new class according to its serialized form.
	 * @param Class serialized form of the new class.
	 * @return search key for the new class.
	 */
	@Override
	public Object deserializeClass(Object Class) {
		SerializedClass sc = (SerializedClass) Class;
		Object key = addClass(sc.data.name, sc.data.type, sc.data.priority, sc.data.population, null);
		if (sc.data.distribution != null) {
			setClassDistribution(key, ((Distribution) sc.data.distribution).clone());
		}
		if (stationsKeyset.contains(sc.data.refStation)) {
			setClassRefStation(key, sc.data.refStation);
		}
		return key;
	}

	/**
	 * Object returned when asking for SerializedClass.
	 */
	public class SerializedClass {
		public Object key;
		public ClassData data;

		/**
		 * Constructs a new serialized form of specified class.
		 * @param key search key for specified class.
		 */
		public SerializedClass(Object key) {
			if (!classesKeyset.contains(key)) {
				return;
			}
			this.key = key;
			data = (ClassData) ((ClassData) classDataHM.get(key)).clone();
		}
	}
	// ----------------------------------------------------------------------------------------------------

	// ----- Station Definition Methods -------------------------------------------------------------------
	/**
	 * Adds a new station to the model, given the station type.
	 * @param type type of the new station.
	 * @return search key for the new station.
	 */
	@Override
	public Object addStation(String type) {
		int num = 0;
		if (!objectNumber.containsKey(type)) {
			for (Object stationKey : stationsKeyset) {
				if (getStationType(stationKey).equals(type)) {
					num++;
				}
			}
		} else {
			num = objectNumber.get(type).intValue();
		}
		objectNumber.put(type, new Integer(++num));
		return super.addStation(STATION_NAMES.get(type) + " " + num, type);
	}

	/**
	 * Deletes a station from the model, given the search key.
	 */
	@Override
	public void deleteStation(Object key) {
		super.deleteStation(key);
		stationPositions.remove(key);
	}

	/**
	 * Returns name of the next station, given the station type.
	 */
	@Override
	public String previewStationName(String type) {
		int num = 0;
		if (!objectNumber.containsKey(type)) {
			for (Object stationKey : stationsKeyset) {
				if (getStationType(stationKey).equals(type)) {
					num++;
				}
			}
		} else {
			num = objectNumber.get(type).intValue();
		}
		return getUniqueStationName(STATION_NAMES.get(type) + " " + (++num));
	}

	/**
	 * Sets position of the station, given the search key.
	 */
	@Override
	public void setStationPosition(Object key, JMTPoint position) {
		stationPositions.put(key, position);
	}

	/**
	 * Returns position of the station, given the search key.
	 */
	@Override
	public JMTPoint getStationPosition(Object key) {
		return stationPositions.get(key);
	}

	/**
	 * Sets shape of a connection, given the search key.
	 */
	public void setConnectionShape(Object sourceKey,Object targetKey, JMTPath path) {
		if(connectionShapes.containsKey(sourceKey)){
			connectionShapes.get(sourceKey).put(targetKey, path);
		}
		else {
			connectionShapes.put(sourceKey, new HashMap<Object, JMTPath>());
			connectionShapes.get(sourceKey).put(targetKey, path);
		}
	}

	/**
	 * Delete shape of a connection, given the search key.
	 */
	public void deleteConnectionShape(Object sourceKey,Object targetKey) {
		if(connectionShapes.containsKey(sourceKey)){
			connectionShapes.get(sourceKey).remove(targetKey);
		}
	}

	/**
	 * Returns shape of a connection, given the search key.
	 */
	public JMTPath getConnectionShape(Object sourceKey,Object targetKey) {
		return connectionShapes.get(sourceKey).get(targetKey);
	}

	/**
	 * Returns shape of a connection, given the search key.
	 */
	public HashMap<Object, JMTPath> getConnectionShapesFromSource(Object sourceKey) {
		return connectionShapes.get(sourceKey);
	}

	/**
	 * Returns shape of a connection, given the search key.
	 */
	public HashMap<Object, JMTPath> getConnectionShapesFromTarget(Object targetKey) {
		HashMap<Object, JMTPath> res = new HashMap<>();
		for (Object key : connectionShapes.keySet()) {
			if(connectionShapes.get(key).containsKey(targetKey)){
				res.put(targetKey, connectionShapes.get(key).get(targetKey));
			}
		}
		return res;
	}

	/**
	 * Returns true if a connection shape exists, given the search key.
	 */
	public boolean hasConnectionShape(Object sourceKey,Object targetKey) {
		if(connectionShapes.containsKey(sourceKey)){
			return connectionShapes.get(sourceKey).containsKey(targetKey);
		}
		else{
			return false;
		}
	}

	/**
	 * Returns true if a connection shape is defined in the model
	 */
	public boolean hasConnectionShape() {
		boolean res =false;
		for (Object key : connectionShapes.keySet()) {
			res=!(connectionShapes.get(key).isEmpty());
		}
		return res;
	}
	/**
	 * Returns serialized form of a station.
	 * @param key search key for the station.
	 * @return serialized form of the station.
	 */
	@Override
	public Object serializeStation(Object key) {
		return new SerializedStation(key);
	}

	/**
	 * Adds a new station with name and type from its serialized form.
	 * @param station serialized form of the new station.
	 * @return search key for the new station.
	 */
	@Override
	public Object addStation(Object station) {
		SerializedStation ss = (SerializedStation) station;
		return addStation(ss.data.name, ss.data.type);
	}

	/**
	 * Loads parameters of a station from its serialized form.
	 * @param station serialized form of the station.
	 * @param key search key for the station.
	 * @param classes map of serialized forms of classes.
	 * @param stationKeyMap map of search keys for stations.
	 */
	@Override
	public void LoadStation(Object station, Object key, Map<Object, Object> classes, Map<Object, Object> stationKeyMap) {
		SerializedStation ss = (SerializedStation) station;
		setStationQueueCapacity(key, ss.data.queueCapacity);
		setStationQueueStrategy(key, ss.data.queueStrategy);
		setStationNumberOfServers(key, ss.data.numberOfServers);
		setForkBlock(key, ss.data.forkBlock);
		setIsSimplifiedFork(key, ss.data.isSimplifiedFork);
		if (ss.data.loggerParameters != null) {
			setLoggingParameters(key, (LoggerParameters) ss.data.loggerParameters.clone());
		}
		Vector<Object> oldClassKeys = new Vector<Object>();
		for (Object classKey : classesKeyset) {
			if (classes.containsKey(classKey) && ((SerializedClass) classes.get(classKey)).data.type == getClassType(classKey)) {
				oldClassKeys.add(classKey);
			}
		}
		if (ss.data.transitionModeList != null) {
			deleteTransitionMode(key, 0);
			for (int i = 0; i < ss.data.transitionModeList.size(); i++) {
				TransitionModeData oldTmd = ss.data.transitionModeList.get(i);
				addTransitionMode(key, oldTmd.name);
				for (Object newStationInKey : getBackwardConnections(key)) {
					Object oldStationInKey = stationKeyMap.get(newStationInKey);
					for (Object oldClassKey : oldClassKeys) {
						setEnablingCondition(key, i, newStationInKey, oldClassKey, oldTmd.enablingCondition.getEntry(oldStationInKey, oldClassKey));
						setInhibitingCondition(key, i, newStationInKey, oldClassKey, oldTmd.inhibitingCondition.getEntry(oldStationInKey, oldClassKey));
					}
				}
				setNumberOfServers(key, i, oldTmd.numberOfServers);
				setFiringTimeDistribution(key, i, ((ServiceStrategy) oldTmd.firingTimeDistribution).clone());
				setFiringPriority(key, i, oldTmd.firingPriority);
				setFiringWeight(key, i, oldTmd.firingWeight);
				for (Object newStationOutKey : getForwardConnections(key)) {
					Object oldStationOutKey = stationKeyMap.get(newStationOutKey);
					for (Object oldClassKey : oldClassKeys) {
						setFiringOutcome(key, i, newStationOutKey, oldClassKey, oldTmd.firingOutcome.getEntry(oldStationOutKey, oldClassKey));
					}
				}
			}
		}
		for (Object oldClassKey : oldClassKeys) {
			StationClassData scd = ss.classData.get(oldClassKey);
			setQueueCapacity(key, oldClassKey, scd.queueCapacity);
			setQueueStrategy(key, oldClassKey, scd.queueStrategy);
			setDropRule(key, oldClassKey, scd.dropRule);
			setServiceWeight(key, oldClassKey, scd.serviceWeight);
			if (scd.serviceDistribution != null) {
				setServiceTimeDistribution(key, oldClassKey, ((ServiceStrategy) scd.serviceDistribution).clone());
			}
			if (scd.routingStrategy != null) {
				if (scd.routingStrategy instanceof ProbabilityRouting) {
					setRoutingStrategy(key, oldClassKey, new ProbabilityRouting());
					Map<Object, Double> oldValues = ((ProbabilityRouting) scd.routingStrategy).getValues();
					Map<Object, Double> newValues = ((ProbabilityRouting) getRoutingStrategy(key, oldClassKey)).getValues();
					for (Object newStationOutKey : getForwardConnections(key)) {
						Object oldStationOutKey = stationKeyMap.get(newStationOutKey);
						newValues.put(newStationOutKey, oldValues.get(oldStationOutKey));
					}
				} else if (scd.routingStrategy instanceof LoadDependentRouting) {
					setRoutingStrategy(key, oldClassKey, new LoadDependentRouting());
					Map<Integer, Map<Object, Double>> oldAllEntries = ((LoadDependentRouting) scd.routingStrategy).getAllEmpiricalEntries();
					Map<Integer, Map<Object, Double>> newAllEntries = ((LoadDependentRouting) getRoutingStrategy(key, oldClassKey)).getAllEmpiricalEntries();
					for (Integer from : oldAllEntries.keySet()) {
						newAllEntries.put(from, new HashMap<Object, Double>());
						Map<Object, Double> oldEntries = oldAllEntries.get(from);
						Map<Object, Double> newEntries = newAllEntries.get(from);
						for (Object newStationOutKey : getForwardConnections(key)) {
							Object oldStationOutKey = stationKeyMap.get(newStationOutKey);
							newEntries.put(newStationOutKey, oldEntries.get(oldStationOutKey));
						}
					}
				} else {
					setRoutingStrategy(key, oldClassKey, ((RoutingStrategy) scd.routingStrategy).clone());
				}
			}
			if (scd.joinStrategy != null) {
				if (scd.joinStrategy instanceof GuardJoin) {
					setJoinStrategy(key, oldClassKey, new GuardJoin());
					Map<Object, Integer> oldGuard = ((GuardJoin) scd.joinStrategy).getGuard();
					Map<Object, Integer> newGuard = ((GuardJoin) getJoinStrategy(key, oldClassKey)).getGuard();
					for (Object classKey : classesKeyset) {
						if (oldClassKeys.contains(classKey)) {
							newGuard.put(classKey, oldGuard.get(classKey));
						} else {
							newGuard.put(classKey, Integer.valueOf(0));
						}
					}
				} else {
					setJoinStrategy(key, oldClassKey, ((JoinStrategy) scd.joinStrategy).clone());
				}
			}
			if (scd.forkStrategy != null) {
				if (scd.forkStrategy instanceof ProbabilitiesFork) {
					setForkStrategy(key, oldClassKey, new ProbabilitiesFork());
					Map<Object, OutPath> oldOutDetails = ((ProbabilitiesFork) scd.forkStrategy).getOutDetails();
					Map<Object, OutPath> newOutDetails = ((ProbabilitiesFork) getForkStrategy(key, oldClassKey)).getOutDetails();
					for (Object newStationOutKey : getForwardConnections(key)) {
						newOutDetails.put(newStationOutKey, new OutPath());
						Object oldStationOutKey = stationKeyMap.get(newStationOutKey);
						OutPath oldOutPath = oldOutDetails.get(oldStationOutKey);
						OutPath newOutPath = newOutDetails.get(newStationOutKey);
						newOutPath.setProbability(oldOutPath.getProbability());
						Map<Object, Object> oldOutParams = oldOutPath.getOutParameters();
						Map<Object, Object> newOutParams = newOutPath.getOutParameters();
						for (Object numberOfTasks : oldOutParams.keySet()) {
							newOutParams.put(numberOfTasks, oldOutParams.get(numberOfTasks));
						}
					}
				} else if (scd.forkStrategy instanceof CombFork) {
					setForkStrategy(key, oldClassKey, new CombFork());
					Map<Object, Double> oldOutDetails = ((CombFork) scd.forkStrategy).getOutDetails();
					Map<Object, Double> newOutDetails = ((CombFork) getForkStrategy(key, oldClassKey)).getOutDetails();
					int totalNumberOfBranches = getForwardConnections(key).size();
					for (int i = 0; i < totalNumberOfBranches; i++) {
						newOutDetails.put(Integer.toString(i + 1), oldOutDetails.get(Integer.toString(i + 1)));
					}
				} else if (scd.forkStrategy instanceof ClassSwitchFork || scd.forkStrategy instanceof MultiBranchClassSwitchFork) {
					try {
						setForkStrategy(key, oldClassKey, scd.forkStrategy.getClass().newInstance());
					} catch (Exception e) {
					}
					Map<Object, OutPath> oldOutDetails = (Map<Object, OutPath>) ((ForkStrategy) scd.forkStrategy).getOutDetails();
					Map<Object, OutPath> newOutDetails = (Map<Object, OutPath>) ((ForkStrategy) getForkStrategy(key, oldClassKey)).getOutDetails();
					for (Object newStationOutKey : getForwardConnections(key)) {
						newOutDetails.put(newStationOutKey, new OutPath());
						Object oldStationOutKey = stationKeyMap.get(newStationOutKey);
						OutPath oldOutPath = oldOutDetails.get(oldStationOutKey);
						OutPath newOutPath = newOutDetails.get(newStationOutKey);
						Map<Object, Object> oldOutParams = oldOutPath.getOutParameters();
						Map<Object, Object> newOutParams = newOutPath.getOutParameters();
						for (Object classKey : classesKeyset) {
							if (oldClassKeys.contains(classKey)) {
								newOutParams.put(classKey, oldOutParams.get(classKey));
							} else {
								newOutParams.put(classKey, Integer.valueOf(0));
							}
						}
					}
				} else {
					setForkStrategy(key, oldClassKey, ((ForkStrategy) scd.forkStrategy).clone());
				}
			}
			if (scd.semaphoreStrategy != null) {
				setSemaphoreStrategy(key, oldClassKey, ((SemaphoreStrategy) scd.semaphoreStrategy).clone());
			}
			if (scd.classSwitchProb != null) {
				for (Object oldClassOutKey : oldClassKeys) {
					setClassSwitchMatrix(key, oldClassKey, oldClassOutKey, scd.classSwitchProb.getValue(oldClassKey, oldClassOutKey));
				}
			}
		}
	}

	/**
	 * Object returned when asking for SerializedStation.
	 */
	public class SerializedStation {
		public Object key;
		public StationData data;
		public Map<Object, StationClassData> classData;

		/**
		 * Constructs a new serialized form of specified station.
		 * @param key search key for specified station.
		 */
		public SerializedStation(Object key) {
			if (!stationsKeyset.contains(key)) {
				return;
			}
			this.key = key;
			data = (StationData) ((StationData) stationDataHM.get(key)).clone();
			classData = new HashMap<Object, StationClassData>();
			for (Object classKey : classesKeyset) {
				classData.put(classKey, (StationClassData) ((StationClassData) stationDetailsBDM.get(classKey, key)).clone());
			}
		}
	}
	// ----------------------------------------------------------------------------------------------------

	// ----- Blocking Region Definition Methods -----------------------------------------------------------
	/**
	 * Adds a new blocking region to the model.
	 * @return search key for the new region.
	 */
	@Override
	public Object addBlockingRegion() {
		int num = 0;
		if (!objectNumber.containsKey(Defaults.get("blockingRegionName"))) {
			num = blockingRegionsKeyset.size();
		} else {
			num = objectNumber.get(Defaults.get("blockingRegionName")).intValue();
		}
		objectNumber.put(Defaults.get("blockingRegionName"), new Integer(++num));
		return addBlockingRegion(Defaults.get("blockingRegionName") + num, Defaults.get("blockingRegionType"));
	}

	/**
	 * Returns serialized form of a blocking region.
	 * @param key search key for the region.
	 * @return serialized form of the region.
	 */
	@Override
	public Object serializeBlockingRegion(Object key) {
		return new SerializedBlockingRegion(key);
	}

	/**
	 * Inserts a new blocking region according to its serialized form.
	 * @param region serialized form of the new region.
	 * @param classes map of serialized forms of classes.
	 * @return search key for the new region.
	 */
	@Override
	public Object deserializeBlockingRegion(Object region, Map<Object, Object> classes) {
		SerializedBlockingRegion sbr = (SerializedBlockingRegion) region;
		Object key = addBlockingRegion(sbr.data.name, sbr.data.type);
		Vector<Object> oldClassKeys = new Vector<Object>();
		for (Object classKey : classesKeyset) {
			if (classes.containsKey(classKey) && ((SerializedClass) classes.get(classKey)).data.type == getClassType(classKey)) {
				oldClassKeys.add(classKey);
			}
		}
		setRegionCustomerConstraint(key, sbr.data.maxJobs);
		setRegionMemorySize(key, sbr.data.maxMemory);
		for (int i = 0; i < sbr.data.groupList.size(); i++) {
			BlockingGroupData bgd = sbr.data.groupList.get(i);
			addRegionGroup(key, bgd.name);
			setRegionGroupCustomerConstraint(key, i, bgd.maxJobs);
			setRegionGroupMemorySize(key, i, bgd.maxMemory);
			for (Object classKey : bgd.classList) {
				if (oldClassKeys.contains(classKey)) {
					addClassIntoRegionGroup(key, i, classKey);
				}
			}
		}
		for (Object oldClassKey : oldClassKeys) {
			BlockingClassData bcd = sbr.classData.get(oldClassKey);
			setRegionClassCustomerConstraint(key, oldClassKey, bcd.maxJobs);
			setRegionClassMemorySize(key, oldClassKey, bcd.maxMemory);
			setRegionClassDropRule(key, oldClassKey, bcd.drop);
			setRegionClassWeight(key, oldClassKey, bcd.weight);
			setRegionClassSize(key, oldClassKey, bcd.size);
		}
		return key;
	}

	/**
	 * Object returned when asking for SerializedBlockingRegion.
	 */
	public class SerializedBlockingRegion {
		public Object key;
		public BlockingRegionData data;
		public Map<Object, BlockingClassData> classData;

		/**
		 * Constructs a new serialized form of specified blocking region.
		 * @param key search key for specified blocking region.
		 */
		public SerializedBlockingRegion(Object key) {
			if (!blockingRegionsKeyset.contains(key)) {
				return;
			}
			this.key = key;
			data = (BlockingRegionData) ((BlockingRegionData) blockingDataHM.get(key)).clone();
			classData = new HashMap<Object, BlockingClassData>();
			for (Object classKey : classesKeyset) {
				classData.put(classKey, (BlockingClassData) ((BlockingClassData) blockingDetailsBDM.get(classKey, key)).clone());
			}
		}
	}
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Tells if queue animation is enabled
	 * @return true if the animation is enabled
	 */
	@Override
	public boolean isAnimationEnabled() {
		return animationEnabled;
	}

	/**
	 * Enable / disable queue animation
	 * @param enabled - set it to true to enable queue animation
	 */
	@Override
	public void setAnimationEnabled(boolean enabled) {
		animationEnabled = enabled;
	}


	/**
	 * Sets absolute value of a connection, given the search key.
	 */
	public void setAbsoluteControlPoints(Object sourceKey,Object targetKey, ArrayList<Point2D> points) {
		if(absoluteControlPoints.containsKey(sourceKey)){
			absoluteControlPoints.get(sourceKey).put(targetKey, points);
		}
		else {
			absoluteControlPoints.put(sourceKey, new HashMap<Object, ArrayList<Point2D>>());
			absoluteControlPoints.get(sourceKey).put(targetKey, points);
		}
	}

	/**
	 * Delete  absolute value of a connection, given the search key.
	 */
	public void deleteAbsoluteControlPoints(Object sourceKey,Object targetKey) {
		if(absoluteControlPoints.containsKey(sourceKey)){
			absoluteControlPoints.get(sourceKey).remove(targetKey);
		}
	}

	/**
	 * Returns shape of a connection, given the search key.
	 */
	public List getAbsoluteControlPoints(Object sourceKey,Object targetKey) {
		return absoluteControlPoints.get(sourceKey).get(targetKey);
	}

}
