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

import jmt.engine.QueueNet.ForkJobInfo;

/**
 * <p>Title: Firing Packet</p>
 * <p>Description: This class implements the firing packet.</p>
 *
 * @author Lulai Zhu
 * Date: 06-10-2016
 * Time: 14.00.00
 */
public class FiringPacket {

	private int modeIndex;
	private double[] totalLifetime;
	private ForkJobInfo[] forkJobInfo;
	private int[] forkJobNum;

	public FiringPacket(int modeIndex, double[] totalLifetime, ForkJobInfo[] forkJobInfo, int[] forkJobNum) {
		this.modeIndex = modeIndex;
		this.totalLifetime = totalLifetime;
		this.forkJobInfo = forkJobInfo;
		this.forkJobNum = forkJobNum;
	}

	public int getModeIndex() {
		return modeIndex;
	}

	public double[] getTotalLifetime() {
		return totalLifetime;
	}

	public ForkJobInfo[] getForkJobInfo() {
		return forkJobInfo;
	}

	public int[] getForkJobNum() {
		return forkJobNum;
	}

}
