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

import java.util.ArrayDeque;

/**
 * ArrayDeque that implements the CustomCollection interface, to act as a LIFO Queue
 * 
 * @author Lorenzo Torri
 * Date: 25-mar-2024
 * Time: 13.32
 */
@SuppressWarnings("rawtypes")
public class LIFOQueue<E> extends ArrayDeque implements CustomCollection{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean addNew(Object o) {
		super.addFirst(o);
		return true;
	}
	
	@Override
	public boolean removeHead() {
		super.removeFirst();
		return true;
	}

	@Override
	public Object first() {
		return super.peek();
	}

	@Override
	public boolean removeObject(Object e) {
		super.remove(e);
		return true;
	}
}
