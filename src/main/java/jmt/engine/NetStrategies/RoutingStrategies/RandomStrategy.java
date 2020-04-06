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

package jmt.engine.NetStrategies.RoutingStrategies;

import jmt.engine.NetStrategies.RoutingStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.random.engine.RandomEngine;

/**
 * This class implements a random strategy: the output node
 * is chosen with a random number
 * @author  Stefano Omini, Bertoli Marco
 * @version 13/11/2005
 * Reimplemented by Bertoli Marco to correct behaviour with closed classes and sinks
 */
public class RandomStrategy extends RoutingStrategy {

	private int CLOSED_CLASS = JobClass.CLOSED_CLASS;

	public RandomStrategy() {
	}

	/**
	 * Gets the output node, into which the job must be routed, using a random
	 * strategy.
	 * @param nodeList List of the output nodes.
	 * @param jobClass Class of current job to be routed.
	 * @return Selected node.
	 */
	@Override
		public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		RandomEngine randomEngine = ownerNode.getNetSystem().getEngine();
		NodeList nodeList = ownerNode.getOutputNodes();
		if (nodeList.size() == 0) {
			return null;
		}

		// Skips sinks for closed classes
		if (jobClass.getType() == CLOSED_CLASS) {
			NetNode[] valid = new NetNode[nodeList.size()];
			int length = 0; // Here length is used as a counter for valid
			for (int i = 0; i < nodeList.size(); i++) {
				if (!nodeList.get(i).isSink()) {
					valid[length++] = nodeList.get(i);
				}
			}
			if (length == 0) {
				return null;
			} else if (length == 1) {
				return valid[0];
			} else {
				return valid[(int) Math.floor(randomEngine.raw() * length)];
			}
		} else {
			int length = nodeList.size();
			if (length == 0) {
				return null;
			} else if (length == 1) {
				return nodeList.getFirst();
			} else {
				return nodeList.get((int) Math.floor(randomEngine.raw() * length));
			}
		}
	}

}
