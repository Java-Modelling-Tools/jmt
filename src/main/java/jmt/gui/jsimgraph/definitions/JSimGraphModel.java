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
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Vector;

import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.routingStrategies.RoutingStrategy;
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
public class JSimGraphModel extends CommonModel implements JmodelClassDefinition, JmodelStationDefinition, JSimGraphConstants {

	// ----- Variables -------------------------------------------------------------
	protected HashMap<String, Long> objectNumber = new HashMap<String, Long>(); // Used to generate progressive unique default names
	protected HashMap<Object, Color> classColor = new HashMap<Object, Color>(); // Used to store classes color
	protected HashMap<Object, JMTPoint> stationPositions = new HashMap<Object, JMTPoint>(); // Used to store station positions
	protected Color[] defaultColor = new Color[] { Color.blue, Color.red, Color.green, Color.magenta, Color.orange, Color.cyan, Color.yellow }; // Defaults color prompted when inserting new class (next ones are generated random)

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
	public Object addClass() {
		Long num;
		if (!objectNumber.containsKey(Defaults.get("className"))) {
			num = new Long(1L);
		} else {
			num = objectNumber.get(Defaults.get("className"));
			num = new Long(num.longValue() + 1);
		}
		objectNumber.put(Defaults.get("className"), num);
		return addClass(getUniqueClassName(Defaults.get("className") + num.toString()),
				Defaults.getAsInteger("classType").intValue(), Defaults.getAsInteger("classPriority"),
				Defaults.getAsInteger("classPopulation"), Defaults.getAsNewInstance("classDistribution"));
	}

	/**
	 * Adds a new class to the model and sets all the parameters at once.
	 * @param name name of the class.
	 * @param type type of the class.
	 * @param priority priority of the class.
	 * @param population population of the class.
	 * @param distribution inter-arrival time distribution of the class.
	 * @return search key for the new class.
	 * Overrides default one to add "Color".
	 */
	@Override
	public Object addClass(String name, int type, Integer priority, Integer population, Object distribution) {
		Object key = super.addClass(getUniqueClassName(name), type, priority, population, distribution);
		setClassColor(key, getNewColor());
		Vector<Object> sources = getStationKeysSource();
		if (type == CLASS_TYPE_OPEN && !sources.isEmpty()) {
			setClassRefStation(key, sources.get(0));
		}
		return key;
	}

	/**
	 * Deletes a class from the model, given the search key.
	 * Overrides default one to remove "Color".
	 */
	@Override
	public void deleteClass(Object key) {
		super.deleteClass(key);
		classColor.remove(key);
	}

	/**
	 * Sets name of the class, given the search key.
	 */
	@Override
	public void setClassName(Object key, String name) {
		if (getClassName(key).equals(name)) {
			return;
		}
		super.setClassName(key, getUniqueClassName(name));
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
		Vector<Object> sources = getStationKeysSource();
		if (type == CLASS_TYPE_OPEN && !sources.isEmpty()) {
			setClassRefStation(key, sources.get(0));
		}
	}

	/**
	 * Returns color of the class, given the search key.
	 */
	public Color getClassColor(Object key) {
		return classColor.get(key);
	}

	/**
	 * Sets color of the class, given the search key.
	 */
	public void setClassColor(Object key, Color color) {
		classColor.put(key, color);
	}

	/**
	 * Generates color for a new class.
	 */
	public Color getNewColor() {
		Long num;
		if (!objectNumber.containsKey(COLOR_NAME)) {
			num = new Long(0L);
		} else {
			num = objectNumber.get(COLOR_NAME);
			num = new Long(num.longValue() + 1);
		}
		objectNumber.put(COLOR_NAME, num);
		if (num.longValue() < defaultColor.length) {
			return defaultColor[num.intValue()];
		} else {
			return new Color((int) Math.floor(Math.random() * 256), (int) Math.floor(Math.random() * 256), (int) Math.floor(Math.random() * 256));
		}
	}

	/**
	 * Returns a serialized object for a given class, used to support cut/paste,
	 * undo/redo operations
	 * @param classKey Search's key for station
	 * @return Serialized class object
	 */
	public Object serializeClass(Object classKey) {
		return new SerializedClass(classKey);
	}

	/**
	 * Deserializes given class
	 * @param serializedForm class's Serialized form got from <code>serializeClass</code>
	 * method.
	 * @return search's key for inserted class
	 */
	public Object deserializeClass(Object serializedForm) {
		SerializedClass sc = (SerializedClass) serializedForm;
		Object newkey = addClass(sc.name, sc.type, sc.priority, sc.population, sc.distribution);
		setClassColor(newkey, sc.color);
		setClassRefStation(newkey, sc.refStation);
		return newkey;
	}

	/**
	 * Object returned when asking for serializedClass
	 */
	public class SerializedClass {
		// Attributes of this class
		public Object key;
		public String name;
		public Color color;
		public int type;
		public Integer priority;
		public Integer population;
		public Object distribution;
		public Object refStation;

		/**
		 * Constructs a new serialized form of specified class
		 * @param classkey key of search for specified class
		 */
		public SerializedClass(Object classkey) {
			key = classkey;
			name = getClassName(classkey);
			color = getClassColor(classkey);
			type = getClassType(classkey);
			priority = getClassPriority(classkey);
			population = getClassPopulation(classkey);
			distribution = getClassDistribution(classkey);
			refStation = getClassRefStation(classkey);
		}
	}

	/**
	 * Returns given name if a class with the same name does not exist or makes it unique
	 * @param name class name
	 * @return unique name
	 */
	private String getUniqueClassName(String name) {
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
	// ----------------------------------------------------------------------------------------------------

	// ----- Station Definition Methods -------------------------------------------------------------------
	/**
	 * Sets name of the station, given the search key.
	 */
	@Override
	public void setStationName(Object key, String name) {
		if (name.equals(getStationName(key)) || !checkStationName(name)) {
			return;
		}
		super.setStationName(key, getUniqueStationName(name));
	}

	/**
	 * Adds a new station to the model, given the station type.
	 * @param type type of the station.
	 * @return search key for the new station.
	 */
	public Object addStation(String type) {
		Long num;
		if (!objectNumber.containsKey(type)) {
			num = new Long(1L);
		} else {
			num = objectNumber.get(type);
			num = new Long(num.longValue() + 1);
		}
		objectNumber.put(type, num);
		return super.addStation(getUniqueStationName(STATION_NAMES.get(type) + " " + num), type);
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
	 * Sets position for a target station into jgraph window. This method is used to
	 * implement save routine
	 * @param key key of search for target station
	 * @param position position where target station is placed
	 */
	public void setStationPosition(Object key, JMTPoint position) {
		stationPositions.put(key, position);
	}

	/**
	 * Returns stored position for a given station into jgraph window
	 * (Note that this is stored position, not original position. This method
	 * is used to implement load routine)
	 * @param key key of search for target station
	 * @return position where target station has to be placed
	 */
	public JMTPoint getStationPosition(Object key) {
		return stationPositions.get(key);
	}

	/**
	 * Gets a preview of name that will be assigned automatically to new created object of
	 * specified type. This method is used to calculate dimensions of caption to be displayed
	 * under component on jgraph window
	 * @param type Constant string from JMODELConstants interface.
	 * @return name assigned to next station of 'type' type.
	 */
	public String previewStationName(String type) {
		Long num;
		if (!objectNumber.containsKey(type)) {
			num = new Long(1L);
		} else {
			num = objectNumber.get(type);
			num = new Long(num.longValue() + 1);
		}
		return getUniqueStationName(STATION_NAMES.get(type) + " " + num);
	}

	/**
	 * Returns a serialized form of a given station to support cut/copy, undo/redo operations
	 * @param key Search's key for station
	 * @return serialized station or null if station key does not exists
	 */
	public Object serializeStation(Object key) {
		return new SerializedStation(key);
	}

	/**
	 * Inserts a serialized station into data structure, used to support paste operation.
	 * Note that Empirical routing strategy can be inconsistent with station's current connections.
	 * (will be resolved in <code>JmtClipboard</code>
	 * @param serializedForm station's Serialized form got from <code>serializeStation</code>
	 * method.
	 * @return search's key for inserted station
	 */
	public Object deserializeStation(Object serializedForm) {
		SerializedStation ss = (SerializedStation) ((SerializedStation) serializedForm).clone();
		ss.name = getUniqueStationName(ss.name);
		Object key = addStation(ss.name, ss.type);
		setStationQueueCapacity(key, ss.queueCapacity);
		setStationQueueStrategy(key, ss.queueStrategy);
		setStationNumberOfServers(key, ss.numOfServers);
		// Now sets class-dependent parameters ONLY if stored class exists (so has not been deleted
		// in the meantime)
		Object[] classKeys = ss.queueStrategies.keySet().toArray();
		Vector<Object> currentClassKeys = getClassKeys();
		for (Object classKey : classKeys) {
			if (currentClassKeys.contains(classKey)) {
				setQueueStrategy(key, classKey, ss.queueStrategies.get(classKey));
				setDropRule(key, classKey, ss.dropRules.get(classKey));
				setServiceTimeDistribution(key, classKey, ss.serviceStrategies.get(classKey));
				setRoutingStrategy(key, classKey, ss.routingStrategies.get(classKey));
			}
		}
		return key;
	}

	/**
	 * Object returned when asking for serializedStation
	 */
	public class SerializedStation {
		// Attributes of this station only
		protected Object key;
		public String name;
		public String type;
		public Integer queueCapacity;
		public String queueStrategy;
		public Integer numOfServers;
		// Attributes of the tuple (class, this station)
		public HashMap<Object, String> queueStrategies = new HashMap<Object, String>();
		public HashMap<Object, String> dropRules = new HashMap<Object, String>();
		public HashMap<Object, ServiceStrategy> serviceStrategies = new HashMap<Object, ServiceStrategy>();
		public HashMap<Object, RoutingStrategy> routingStrategies = new HashMap<Object, RoutingStrategy>();

		public SerializedStation() {
			return;
		}

		public SerializedStation(Object key) {
			this.key = key;
			name = getStationName(key);
			type = getStationType(key);
			queueCapacity = getStationQueueCapacity(key);
			queueStrategy = getStationQueueStrategy(key);
			numOfServers = getStationNumberOfServers(key);
			// Now sets all attributes of the (station/class) tuple
			Vector<Object> classKeys = getClassKeys();
			for (Object classKey : classKeys) {
				queueStrategies.put(classKey, getQueueStrategy(key, classKey));
				dropRules.put(classKey, getDropRule(key, classKey));
				if (getServiceTimeDistribution(key, classKey) != null) {
					serviceStrategies.put(classKey, ((ServiceStrategy) getServiceTimeDistribution(key, classKey)).clone());
				} else {
					serviceStrategies.put(classKey, null);
				}
				if (getRoutingStrategy(key, classKey) != null) {
					routingStrategies.put(classKey, ((RoutingStrategy) getRoutingStrategy(key, classKey)).clone());
				} else {
					routingStrategies.put(classKey, null);
				}
			}
		}

		/**
		 * Returns a deep copy of this object
		 * @return A deep copy of this object
		 */
		@Override
		public Object clone() {
			SerializedStation ss = new SerializedStation();
			ss.key = key;
			ss.name = name;
			ss.type = type;
			ss.queueCapacity = queueCapacity;
			ss.queueStrategy = queueStrategy;
			ss.numOfServers = numOfServers;
			ss.queueStrategies = new HashMap<Object, String>(queueStrategies);
			ss.dropRules = new HashMap<Object, String>(dropRules);
			Object[] classKeys = serviceStrategies.keySet().toArray();
			for (Object classKey : classKeys) {
				if (serviceStrategies.get(classKey) != null) {
					ss.serviceStrategies.put(classKey, serviceStrategies.get(classKey).clone());
				} else {
					ss.serviceStrategies.put(classKey, null);
				}
				if (routingStrategies.get(classKey) != null) {
					ss.routingStrategies.put(classKey, routingStrategies.get(classKey).clone());
				} else {
					ss.routingStrategies.put(classKey, null);
				}
			}
			return ss;
		}
	}

	/**
	 * Returns true if the station name is valid, false otherwise
	 * @param name station name
	 * @return true if the station name is valid, false otherwise
	 */
	private boolean checkStationName(String name) {
		return !name.equals(STATION_TYPE_FORK) && !name.equals(STATION_TYPE_CLASSSWITCH)
				&& !name.equals(STATION_TYPE_SCALER) && !name.equals(STATION_TYPE_TRANSITION);
	}

	/**
	 * Returns given name if a station with the same name does not exist or makes it unique
	 * @param name station name
	 * @return unique name
	 */
	private String getUniqueStationName(String name) {
		// Map of all unique names with their first users
		TreeSet<String> names = new TreeSet<String>();
		Vector<Object> keys = getStationKeys();
		// Finds all used names
		for (int i = 0; i < keys.size(); i++) {
			names.add(getStationName(keys.get(i)));
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
	// ----------------------------------------------------------------------------------------------------

	// ----- Blocking Region Definition Methods -----------------------------------------------------------
	/**
	 * Adds a new blocking region with unique name to current model
	 * @return search's key for created blocking region
	 */
	public Object addBlockingRegion() {
		long num = 1L;
		String baseName = Defaults.get("blockingRegionName");

		// Gets all blocking region names to find an unique name
		TreeSet<String> names = new TreeSet<String>();
		Vector<Object> keys = getRegionKeys();
		for (int i = 0; i < keys.size(); i++) {
			names.add(getRegionName(keys.get(i)));
		}

		String name = baseName + num;
		while (names.contains(name)) {
			name = baseName + (++num);
		}

		return addBlockingRegion(name, Defaults.get("blockingRegionType"));
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
	public void setAnimationEnabled(boolean isEnabled) {
		animationEnabled = isEnabled;
	}

}
