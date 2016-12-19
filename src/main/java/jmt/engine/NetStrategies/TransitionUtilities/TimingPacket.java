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

package jmt.engine.NetStrategies.TransitionUtilities;

import jmt.engine.simEngine.RemoveToken;

/**
 * <p>Title: Timing Packet</p>
 * <p>Description: This class implements the timing packet.</p>
 *
 * @author Lulai Zhu
 * Date: 06-10-2016
 * Time: 14.00.00
 */
public class TimingPacket {

	private int modeIndex;
	private double firingDelay;
	private int firingPriority;
	private double firingWeight;
	private RemoveToken timingToken;

	public TimingPacket(int modeIndex, double firingDelay, int firingPriority, double firingWeight) {
		this.modeIndex = modeIndex;
		this.firingDelay = firingDelay;
		this.firingPriority = firingPriority;
		this.firingWeight = firingWeight;
		this.timingToken = null;
	}

	public int getModeIndex() {
		return modeIndex;
	}

	public double getFiringDelay() {
		return firingDelay;
	}

	public int getFiringPriority() {
		return firingPriority;
	}

	public double getFiringWeight() {
		return firingWeight;
	}

	public RemoveToken getTimingToken() {
		return timingToken;
	}

	public void setTimingToken(RemoveToken timingToken) {
		this.timingToken = timingToken;
	}

}
