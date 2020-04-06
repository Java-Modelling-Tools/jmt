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

package jmt.engine.NodeSections;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NetStrategies.TransitionUtilities.EnablingPacket;
import jmt.engine.NetStrategies.TransitionUtilities.FiringPacket;
import jmt.engine.NetStrategies.TransitionUtilities.TimingPacket;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.SimConstants;
import jmt.engine.dataAnalysis.InverseMeasure;
import jmt.engine.dataAnalysis.Measure;
import jmt.engine.random.engine.RandomEngine;
import jmt.engine.simEngine.RemoveToken;

/**
 * <p>Title: Timing</p>
 * <p>Description: This class implements the timing section.</p>
 *
 * @author Lulai Zhu
 * Date: 15-07-2016
 * Time: 21.00.00
 */
public class Timing extends ServiceSection {

	private String[] modeNames;
	private int[] numbersOfServers;
	private ServiceStrategy[] timingStrategies;
	private int[] firingPriorities;
	private double[] firingWeights;

	private List<RemoveToken>[] timingTokenLists;
	private int[] extraEnablingDegrees;
	private InverseMeasure firingThroughput;
	private InverseMeasure[] firingThroughputs;
	private double lastFiringTime;
	private double[] lastFiringTimes;

	/**
	 * Creates a new instance of the timing section.
	 */
	public Timing(String[] modeNames, Integer[] numbersOfServers, ServiceStrategy[] timingStrategies,
			Integer[] firingPriorities, Double[] firingWeights) {
		super(true);
		this.modeNames = modeNames;
		this.numbersOfServers = new int[numbersOfServers.length];
		for (int i = 0; i < numbersOfServers.length; i++) {
			this.numbersOfServers[i] = numbersOfServers[i].intValue();
		}
		this.timingStrategies = timingStrategies;
		this.firingPriorities = new int[firingPriorities.length];
		for (int i = 0; i < firingPriorities.length; i++) {
			this.firingPriorities[i] = firingPriorities[i].intValue();
		}
		this.firingWeights = new double[firingWeights.length];
		for (int i = 0; i < firingWeights.length; i++) {
			this.firingWeights[i] = firingWeights[i].doubleValue();
		}
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		timingTokenLists = new List[modeNames.length];
		for (int i = 0; i < timingTokenLists.length; i++) {
			timingTokenLists[i] = new LinkedList<RemoveToken>();
		}
		extraEnablingDegrees = new int[modeNames.length];
		Arrays.fill(extraEnablingDegrees, 0);
		lastFiringTime = 0.0;
		lastFiringTimes = new double[modeNames.length];
		Arrays.fill(lastFiringTimes, 0.0);
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		Object data = message.getData();

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:
			break;

		case NetEvent.EVENT_JOB:
			return MSG_NOT_PROCESSED;

		case NetEvent.EVENT_ACK:
			return MSG_NOT_PROCESSED;

		case NetEvent.EVENT_ENABLING:
		{
			int modeIndex = ((EnablingPacket) data).getModeIndex();
			int enablingDegree = ((EnablingPacket) data).getEnablingDegree();
			int timingEvents = timingTokenLists[modeIndex].size();
			int deltaTimingEvents = 0;
			if (enablingDegree < 0) {
				if (numbersOfServers[modeIndex] < 0) {
					return MSG_NOT_PROCESSED;
				} else {
					deltaTimingEvents = numbersOfServers[modeIndex] - timingEvents;
					extraEnablingDegrees[modeIndex] = -1;
				}
			} else {
				if (numbersOfServers[modeIndex] < 0) {
					deltaTimingEvents = enablingDegree - timingEvents;
				} else {
					if (enablingDegree <= numbersOfServers[modeIndex]) {
						deltaTimingEvents = enablingDegree - timingEvents;
						extraEnablingDegrees[modeIndex] = 0;
					} else {
						deltaTimingEvents = numbersOfServers[modeIndex] - timingEvents;
						extraEnablingDegrees[modeIndex] = enablingDegree - numbersOfServers[modeIndex];
					}
				}
			}
			RandomEngine randomEngine = this.getNetSytem().getEngine();
			if (deltaTimingEvents >= 0) {
				for (int i = 0; i < deltaTimingEvents; i++) {
					double firingDelay = timingStrategies[modeIndex].wait(this, null);
					TimingPacket packet = new TimingPacket(modeIndex, firingDelay, firingPriorities[modeIndex],
							firingWeights[modeIndex]);
					RemoveToken token = sendMe(NetEvent.EVENT_TIMING, packet, firingDelay);
					packet.setTimingToken(token);
					int index = (int) (randomEngine.raw() * (timingTokenLists[modeIndex].size() + 1));
					timingTokenLists[modeIndex].add(index, token);
				}
			} else {
				for (int i = 0; i > deltaTimingEvents; i--) {
					RemoveToken token = (RemoveToken) timingTokenLists[modeIndex].remove(0);
					removeMessage(token);
				}
			}
			break;
		}

		case NetEvent.EVENT_TIMING:
		{
			int modeIndex = ((TimingPacket) data).getModeIndex();
			RemoveToken token = ((TimingPacket) data).getTimingToken();
			timingTokenLists[modeIndex].remove(token);
			FiringPacket firingPacket = new FiringPacket(modeIndex, modeNames[modeIndex], null, null);
			sendBackward(NetEvent.EVENT_FIRING, firingPacket, 0.0);
	        RandomEngine randomEngine = this.getNetSytem().getEngine();
			if (extraEnablingDegrees[modeIndex] != 0) {
				double firingDelay = timingStrategies[modeIndex].wait(this, null);
				TimingPacket timingPacket = new TimingPacket(modeIndex, firingDelay, firingPriorities[modeIndex],
						firingWeights[modeIndex]);
				token = sendMe(NetEvent.EVENT_TIMING, timingPacket, firingDelay);
				timingPacket.setTimingToken(token);
				int index = (int) (randomEngine.raw() * (timingTokenLists[modeIndex].size() + 1));
				timingTokenLists[modeIndex].add(index, token);
				if (extraEnablingDegrees[modeIndex] > 0) {
					extraEnablingDegrees[modeIndex]--;
				}
			}
			break;
		}

		case NetEvent.EVENT_FIRING:
		{
			int modeIndex = ((FiringPacket) data).getModeIndex();
			sendForward(NetEvent.EVENT_FIRING, data, 0.0);
			updateFiringThroughput(modeIndex);
			lastFiringTime = getTime();
			lastFiringTimes[modeIndex] = getTime();
			break;
		}

		case NetEvent.EVENT_STOP:
			break;

		default:
			return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	public void analyzeTransition(int name, String modeName, Measure measurement) throws NetException {
		switch (name) {
		case SimConstants.FIRING_THROUGHPUT:
			if (!analyzeFiringThroughput(modeName, (InverseMeasure) measurement)) {
				throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
			}
			break;
		default:
			throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
		}
	}

	private boolean analyzeFiringThroughput(String modeName, InverseMeasure measurement) {
		if (modeName != "") {
			int modeIndex = getModeIndexByName(modeName);
			if (modeIndex < 0) {
				return false;
			}
			if (firingThroughputs == null) {
				firingThroughputs = new InverseMeasure[modeNames.length];
			}
			firingThroughputs[modeIndex] = measurement;
		} else {
			firingThroughput = measurement;
		}
		return true;
	}

	private void updateFiringThroughput(int modeIndex) {
		if (firingThroughputs != null) {
			Measure m = firingThroughputs[modeIndex];
			if (m != null) {
				m.update(getTime() - lastFiringTimes[modeIndex], 1.0);
			}
		}
		if (firingThroughput != null) {
			firingThroughput.update(getTime() - lastFiringTime, 1.0);
		}
	}

	private int getModeIndexByName(String modeName) {
		for (int i = 0; i < modeNames.length; i++) {
			if (modeNames[i].equals(modeName)) {
				return i;
			}
		}
		return -1;
	}
	
	private double getTime(){
		return this.getOwnerNode().getNetSystem().getTime();
	}

}
