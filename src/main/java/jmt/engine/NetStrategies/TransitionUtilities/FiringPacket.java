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

import java.util.List;

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
	private String modeName;
	private double[] totalLifetimes;
	private List<ForkJobInfo>[] forkJobInfoLists;

	public FiringPacket(int modeIndex, String modeName, double[] totalLifetimes, List<ForkJobInfo>[] forkJobInfoLists) {
		this.modeIndex = modeIndex;
		this.modeName = modeName;
		this.totalLifetimes = totalLifetimes;
		this.forkJobInfoLists = forkJobInfoLists;
	}

	public int getModeIndex() {
		return modeIndex;
	}

	public String getModeName() {
		return modeName;
	}

	public double[] getTotalLifetimes() {
		return totalLifetimes;
	}

	public List<ForkJobInfo>[] getForkJobInfoLists() {
		return forkJobInfoLists;
	}

}
