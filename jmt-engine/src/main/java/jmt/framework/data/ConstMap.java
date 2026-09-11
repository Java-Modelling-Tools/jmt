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

package jmt.framework.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p><b>Name:</b> ConstMap</p> 
 * <p><b>Description:</b> 
 * A data structure suitable to create constant maps. Its values cannot be changed.
 * </p>
 * <p><b>Date:</b> 09/ott/2008
 *    <b>Time:</b> 18:57:49</p>
 * @author Bertoli Marco
 * @version 1.0
 */
public abstract class ConstMap<K, V> {

	private Map<K, V> map = new LinkedHashMap<K, V>();

	/**
	 * Fills this constant map with values. Call put method to fill.
	 * @see #put(Object, Object)
	 */
	protected abstract void fill();

	public ConstMap() {
		fill();
	}

	/**
	 * Puts a value in this map. May be called only from the fill method.
	 * @param key the key for the value
	 * @param value the value
	 */
	protected void put(K key, V value) {
		map.put(key, value);
	}

	/**
	 * Returns a value in this map.
	 * @param key the key for the value
	 * @return the value
	 */
	public V get(Object key) {
		return map.get(key);
	}

	/**
	 * Returns the key set of this map.
	 * @return the key set
	 */
	public Set<K> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

}
