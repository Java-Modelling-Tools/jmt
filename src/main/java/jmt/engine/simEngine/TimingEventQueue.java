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

package jmt.engine.simEngine;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import jmt.engine.NetStrategies.TransitionUtilities.TimingPacket;
import jmt.engine.random.engine.RandomEngine;
import jmt.framework.data.CircularList;

/**
 * <p>Title: Timing Event Queue</p>
 * <p>Description: This class implements the timing event queue.</p>
 *
 * @author Lulai Zhu
 * Date: 10-09-2016
 * Time: 14.00.00
 */
public class TimingEventQueue implements EventQueue {

	private static final int DEFAULT_INITIAL_CAPACITY = 111;

	private TimingEventComparator comparator;
	private CircularList<SimEvent> current;
	private PriorityQueue<SimEvent> future;
	private RandomEngine randomEngine;

	public TimingEventQueue() {
		comparator = new TimingEventComparator();
		current = new CircularList<SimEvent>();
		future = new PriorityQueue<SimEvent>(DEFAULT_INITIAL_CAPACITY, comparator);
	}

	@Override
	public int size() {
		return current.size() + future.size();
	}

	@Override
	public boolean add(SimEvent event) {
		handleCurrent();
		if (current.size() == 0) {
			current.add(event);
			return true;
		}

		int result = comparator.compare(event, current.getFirst());
		if (result == 0) {
			current.add(event);
		} else if (result > 0) {
			future.add(event);
		} else {
			moveCurrentToFuture();
			current.add(event);
		}
		return true;
	}

	@Override
	public SimEvent pop() {
		handleCurrent();
		if (current.size() == 0) {
			return null;
		}

		double totalWeight = 0.0;
		Iterator<SimEvent> it = current.iterator();
		SimEvent event = null;
		TimingPacket packet = null;
		while (it.hasNext()) {
			event = it.next();
			packet = (TimingPacket) event.getData();
			totalWeight += packet.getFiringWeight();
		}
		double summedWeight = 0.0;
		double randomIndex = randomEngine.nextDouble() * totalWeight;
		it = current.iterator();
		while (summedWeight <= randomIndex) {
			event = it.next();
			packet = (TimingPacket) event.getData();
			summedWeight += packet.getFiringWeight();
		}
		it.remove();
		return event;
	}

	@Override
	public SimEvent peek() {
		handleCurrent();
		if (current.size() > 0) {
			return current.getFirst();
		} else {
			return null;
		}
	}

	@Override
	public boolean remove(SimEvent event) {
		handleCurrent();
		if (current.size() == 0) {
			return false;
		}

		if (comparator.compare(event, current.getFirst()) == 0) {
			return current.remove(event);
		} else {
			return future.remove(event);
		}
	}

	@Override
	public void clear() {
		current.clear();
		future.clear();
	}

	@Override
	public Iterator<SimEvent> iterator() {
		return new Iter();
	}

	private void moveCurrentToFuture() {
		while (current.size() > 0) {
			future.add(current.removeFirst());
		}
	}

	private void handleCurrent() {
		if (current.size() == 0 && future.size() > 0) {
			SimEvent first = future.remove();
			current.add(first);
			while (future.size() > 0 && comparator.compare(future.peek(), first) == 0) {
				current.add(future.remove());
			}
		}
	}

	private static class TimingEventComparator implements Comparator<SimEvent> {

		public int compare(SimEvent e1, SimEvent e2) {
			double time1 = e1.eventTime();
			double time2 = e2.eventTime();
			if (time1 > time2) {
				return 1;
			} else if (time1 < time2) {
				return -1;
			}
			TimingPacket packet1 = (TimingPacket) e1.getData();
			TimingPacket packet2 = (TimingPacket) e2.getData();
			double delay1 = packet1.getFiringDelay();
			double delay2 = packet2.getFiringDelay();
			if (delay1 > delay2) {
				return 1;
			} else if (delay1 < delay2) {
				return -1;
			}
			int priority1 = packet1.getFiringPriority();
			int priority2 = packet2.getFiringPriority();
			if (priority1 > priority2) {
				return -1;
			} else if (priority1 < priority2) {
				return 1;
			} else {
				return 0;
			}
		}

	}

	private class Iter implements Iterator<SimEvent> {

		private Iterator<SimEvent> currentIter = current.iterator();
		private Iterator<SimEvent> futureIter = future.iterator();
		private boolean isCurrent = true;

		public boolean hasNext() {
			return currentIter.hasNext() || futureIter.hasNext();
		}

		public SimEvent next() {
			if (currentIter.hasNext()) {
				isCurrent = true;
				return currentIter.next();
			} else {
				isCurrent = false;
				return futureIter.next();
			}
		}

		public void remove() {
			if (isCurrent) {
				currentIter.remove();
			} else {
				futureIter.remove();
			}
		}

	}
	public void setEngine(RandomEngine engine){
		this.randomEngine = engine;
	}


}
