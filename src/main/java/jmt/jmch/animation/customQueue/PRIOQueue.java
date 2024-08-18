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

import java.util.ArrayList;
import java.util.Comparator;

/**
 * ArrayList that implements the CustomCollection interface, to act as a Priority Queue.
 * This queue needs a Comparator when instantiated, so all the queues that are not LIFO or FIFO can be implemented with this class.
 * 
 * @author Lorenzo Torri
 * Date: 25-mar-2024
 * Time: 13.20
 */
public class PRIOQueue<E> extends ArrayList implements CustomCollection{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Comparator<? super E> comparator;
	
	public PRIOQueue(Comparator<? super E> comp) {
		super();
		comparator = comp;
	}

	@Override
	public boolean addNew(Object o) {
		int index = findIndex((E) o);
		super.add(index, (E) o);
		return true;
	}
	
	/**
	 * This method is used to find the position in which adding the new element 
	 * @param element that needs to be added
	 * @return the index inside the collection
	 */
	private int findIndex(E element) {
		int i = 0;
		for(Object e: this) {
			if(comparator.compare(element, (E) e) < 0) {
				break;
			}
			i++;
		}
		return i;
	}
	
	@Override
	public boolean removeHead() {
		super.remove(0); //always sure the remove is done correctly
		return true;
	}

	@Override
	public Object first() {
		return super.get(0);
	}

	@Override
	public boolean removeObject(Object e) {
		super.remove(e);
		return true;
	}
}
