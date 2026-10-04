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

package jmt.jmch.animation.customQueue;

import java.util.Iterator;

/**
 * Interface that simulates the beheviour of the Collection interface in Java.
 * This interface acts as a supertype for all the types of queue used in the station, like FIFO, LIFO, Priority
 *
 * @author Lorenzo Torri
 * Date: 25-mar-2024
 * Time: 13.10
 */
public interface CustomCollection<E> extends Iterable<E>{
	/**
	 * Add a new element to the Collection, how to add and where to add it, it will be handled by the classes that implements this interface
	 * @param o Object to add
	 * @return true if the adding was successful
	 */
	public boolean addNew(Object o);
	
	/**
	 * Remove the head of the Collection
	 * @return true if the head is removed correctly
	 */
	public boolean removeHead();
	
	/**
	 * Retrieve the first element of the Collection
	 * @return the first element of the Collection, null otherwise
	 */
	public Object first();
	
	/**
	 * This is the method of the Iterable<E> interface, needed for iterating over the Collection
	 */
	public Iterator<E> iterator();

	/**
	 * Get the size of the Collection
	 * @return size of the collection
	 */
	public int size();

	/**
	 * Remove the Object o from the list. Used for specific cases like BoxStation in PS that has to remove the job from the queue
	 * @param o the object
	 * @return true if it has removed the object correctly
	 */
	public boolean removeObject(Object o);
}
