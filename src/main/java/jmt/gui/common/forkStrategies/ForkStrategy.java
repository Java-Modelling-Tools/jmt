package jmt.gui.common.forkStrategies;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public abstract class ForkStrategy {
	protected static ForkStrategy[] all = null;
	protected static ForkStrategy[] allForSource = null;
	
	protected String description;

	public abstract String getName();

	public String getDescription() {
		return description;
	}
	
	public abstract Map<Object, ? extends Object> getOutDetails();
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public abstract ForkStrategy clone();

	@Override
	public boolean equals(Object o) {
		if (o instanceof ForkStrategy) {
			return this.getName().equals(((ForkStrategy) o).getName());
		} else {
			return false;
		}
	}

	public abstract String getClassPath();
	
	public static ForkStrategy[] findAll() {
		if (all != null) {	
			return all;
		}
		ArrayList<ForkStrategy> strategies = new ArrayList<ForkStrategy>();
		Field[] fields = jmt.gui.jsimgraph.JSimGraphConstants.class.getFields();
		try {
			for (Field field : fields) {
				if (field.getName().startsWith("FORKING_")) {
					strategies.add((ForkStrategy) field.get(null));
				}
			}
		} catch (IllegalAccessException ex) {
			System.err.println("A security manager has blocked reflection");
			ex.printStackTrace();
		}
		ForkStrategy[] ret = new ForkStrategy[strategies.size()];
		for (int i = 0; i < strategies.size(); i++) {
			ret[i] = strategies.get(i);
		}
		all = ret;
		return ret;
	}
	//TODO:modify this piece of code
	public static ForkStrategy[] findAllForSource() {
		if (allForSource != null) {
			return allForSource;
		}
		ArrayList<ForkStrategy> strategies = new ArrayList<ForkStrategy>();
		Field[] fields = jmt.gui.jsimgraph.JSimGraphConstants.class.getFields();
		try {
			for (Field field : fields) {
				if (field.getName().startsWith("Fork_") && !field.getName().endsWith("LOADDEPENDANT") ) {
					strategies.add((ForkStrategy) field.get(null));
				}
			}
		} catch (IllegalAccessException ex) {
			System.err.println("A security manager has blocked reflection");
			ex.printStackTrace();
		}
		ForkStrategy[] ret = new ForkStrategy[strategies.size()];
		for (int i = 0; i < strategies.size(); i++) {
			ret[i] = strategies.get(i);
		}
		allForSource = ret;
		return ret;
	}
        
        public abstract void removeStation(Object stationKey);
        
        public abstract void addStation(String stationName, Object stationKey, Object classKey, Vector<Object> classKeys);
	
	public abstract boolean isModelStateDependent();
	
}