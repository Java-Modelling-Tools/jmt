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

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: OrsotronIII
 * Date: 23-mag-2005
 * Time: 9.34.34
 * This interface provides methods for editation of model's classes details. In the implementing
 * class of this interface, model's userclasses must be accessed through a research key that must
 * be represented by a number. This gives possibility of a smarter implementation for search and
 * editing methods than a name-based search modality.
 * 
 * Modified by Francesco D'Aquino 21/11/2005
 */
public interface ClassDefinition {

	/**
	 * This method returns the key set of all classes.
	 */
	public Vector<Object> getClassKeys();

	/**
	 * This method returns the key set of open classes.
	 */
	public Vector<Object> getOpenClassKeys();

	/**
	 * This method returns the key set of closed classes.
	 */
	public Vector<Object> getClosedClassKeys();

	/**
	 * Returns name of the class, given the search key.
	 */
	public String getClassName(Object key);

	/**
	 * Sets name of the class, given the search key.
	 */
	public void setClassName(Object key, String name);

	/**
	 * Returns type of the class, given the search key.
	 */
	public int getClassType(Object key);

	/**
	 * Sets type of the class, given the search key.
	 */
	public void setClassType(Object key, int type);

	/**
	 * Returns priority of the class, given the search key.
	 */
	public Integer getClassPriority(Object key);

	/**
	 * Sets priority of the class, given the search key.
	 */
	public void setClassPriority(Object key, Integer priority);

	/**
	 * Returns population of the class, given the search key.
	 */
	public Integer getClassPopulation(Object key);

	/**
	 * Sets population of the class, given the search key.
	 */
	public void setClassPopulation(Object key, Integer population);

	/**
	 * Returns inter-arrival time distribution of the class, given the search key.
	 */
	public Object getClassDistribution(Object key);

	/**
	 * Sets inter-arrival time distribution of the class, given the search key.
	 */
	public void setClassDistribution(Object key, Object distribution);

	/**
	 * Returns reference station of a class, given the search key.
	 */
	public Object getClassRefStation(Object key);

	/**
	 * Sets reference station of a class, given the search key.
	 */
	public void setClassRefStation(Object key, Object refStation);

	/**
	 * Adds a new class to the model and sets all class parameters at once.
	 * @param name name of the new class.
	 * @param type type of the new class.
	 * @param priority priority of the new class.
	 * @param population population of the new class.
	 * @param distribution inter-arrival time distribution of the new class.
	 * @return search key for the new class.
	 */
	public Object addClass(String name, int type, Integer priority, Integer population, Object distribution);

	/**
	 * Deletes a class from the model, given the search key.
	 */
	public void deleteClass(Object key);

	/**
	 * Returns the search key for the class, given the class name.
	 */
	public Object getClassByName(String className);

	/**
	 * Returns total population of closed classes
	 */
	public int getTotalClosedClassPopulation();

}
