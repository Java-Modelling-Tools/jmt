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

import java.util.Map;

import jmt.gui.common.definitions.BlockingRegionDefinition;

/**
 * <p>Title: Jmodel Blocking Region Definition Interface</p>
 * <p>Description: This interface provides methods for editing blocking regions for JMODEL models.
 * It actually extends JSIM BlockingRegionDefinition interface to provide a compatibility layer.</p>
 * 
 * @author Lulai Zhu
 *         Date: 02-04-2017
 *         Time: 20.00.00
 */
public interface JmodelBlockingRegionDefinition extends BlockingRegionDefinition {

	/**
	 * Adds a new blocking region to the model.
	 * @return search key for the new blocking region.
	 */
	public Object addBlockingRegion();

	/**
	 * Returns serialized form of a blocking region.
	 * @param key search key for the region.
	 * @return serialized form of the region.
	 */
	public Object serializeBlockingRegion(Object key);

	/**
	 * Inserts a new blocking region according to its serialized form.
	 * @param region serialized form of the new region.
	 * @param classes map of serialized forms of classes.
	 * @return search key for the new region.
	 */
	public Object deserializeBlockingRegion(Object region, Map<Object, Object> classes);

}
