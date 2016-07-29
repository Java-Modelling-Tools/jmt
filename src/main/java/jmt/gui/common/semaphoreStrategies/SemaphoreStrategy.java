package jmt.gui.common.semaphoreStrategies;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.OutPath;

public abstract class SemaphoreStrategy {
	protected static SemaphoreStrategy[] all = null;
	protected static SemaphoreStrategy[] allForSource = null;
	
	protected String description;

	public abstract String getName();

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return getName();
	}
	
	public abstract int getSemaphoreThres();
	public abstract void setSemaphoreThres(int i);
	
	@Override
	public abstract SemaphoreStrategy clone();

	@Override
	public boolean equals(Object o) {
		if (o instanceof SemaphoreStrategy) {
			return this.getName().equals(((SemaphoreStrategy) o).getName());
		} else {
			return false;
		}
	}

	public abstract String getClassPath();
	
	public static SemaphoreStrategy[] findAll() {
		if (all != null) {
			return all;
		}
		ArrayList<SemaphoreStrategy> strategies = new ArrayList<SemaphoreStrategy>();
		Field[] fields = jmt.gui.jsimgraph.JSimGraphConstants.class.getFields();
		try {
			for (Field field : fields) {
				if (field.getName().startsWith("SEMAPHORE_")) {
					strategies.add((SemaphoreStrategy) field.get(null));
				}
			}
		} catch (IllegalAccessException ex) {
			System.err.println("A security manager has blocked reflection");
			ex.printStackTrace();
		}
		SemaphoreStrategy[] ret = new SemaphoreStrategy[strategies.size()];
		for (int i = 0; i < strategies.size(); i++) {
			ret[i] = strategies.get(i);
		}
		all = ret;
		return ret;
	}
	//TODO:modify this piece of code
	public static SemaphoreStrategy[] findAllForSource() {
		if (allForSource != null) {
			return allForSource;
		}
		ArrayList<SemaphoreStrategy> strategies = new ArrayList<SemaphoreStrategy>();
		Field[] fields = jmt.gui.jsimgraph.JSimGraphConstants.class.getFields();
		try {
			for (Field field : fields) {
				if (field.getName().startsWith("Semaphore_") && !field.getName().endsWith("LOADDEPENDANT") ) {
					strategies.add((SemaphoreStrategy) field.get(null));
				} 
			}
		} catch (IllegalAccessException ex) {
			System.err.println("A security manager has blocked reflection");
			ex.printStackTrace();
		}
		SemaphoreStrategy[] ret = new SemaphoreStrategy[strategies.size()];
		for (int i = 0; i < strategies.size(); i++) {
			ret[i] = strategies.get(i);
		}
		allForSource = ret;
		return ret;
	}
	
	public abstract boolean isModelStateDependent();
}
