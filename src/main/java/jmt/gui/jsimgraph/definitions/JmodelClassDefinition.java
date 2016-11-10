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

import jmt.gui.common.definitions.ClassDefinition;

/**
 * <p>Title: Jmodel Class Definition Interface</p>
 * <p>Description: This interface provides methods for editing classes for JMODEL models.
 * It actually extends JSIM ClassDefinition interface to provide a compatibility layer</p>
 * 
 * @author Bertoli Marco
 *         Date: 14-giu-2005
 *         Time: 9.44.32
 */
public interface JmodelClassDefinition extends ClassDefinition {

	/**
	 * Adds a new class to the model and sets all the parameters by default.
	 * @return search key for the new class.
	 */
	public Object addClass();

	/**
	 * Returns color of the class, given the search key.
	 */
	public Color getClassColor(Object key);

	/**
	 * Sets color of the class, given the search key.
	 */
	public void setClassColor(Object key, Color color);

	/**
	 * Returns a serialized object for a given class, used to support cut/paste,
	 * undo/redo operations
	 * @param classKey Search's key for station
	 * @return Serialized class object
	 */
	public Object serializeClass(Object classKey);

	/**
	 * Deserializes given class
	 * @param serializedForm class's Serialized form got from <code>serializeClass</code>
	 * method.
	 * @return search's key for inserted class
	 */
	public Object deserializeClass(Object serializedForm);

}
