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

/**
 * This class implements a round robin strategy: all the output nodes
 * are chosen in sequence, one at each time; when all the nodes have been chosen,
 * the sequence restarts.
 * @author Francesco Radaelli, Bertoli Marco
 * Modified by Bertoli Marco to correct behaviour with closed classes and sinks
 */
public class RoundRobinStrategy extends RoutingStrategy {

	private int CLOSED_CLASS = JobClass.CLOSED_CLASS;
	private int counter;

	public RoundRobinStrategy() {
		counter = 0;
	}

	/**
	 * Gets the output node, into which the job must be routed, using a round
	 * robin strategy.
	 * @param nodeList List of the output nodes.
	 * @param jobClass Class of current job to be routed.
	 * @return The selected node.
	 */
	@Override
	public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		NodeList nodeList = ownerNode.getOutputNodes();
		if (nodeList.size() == 0) {
			return null;
		}

		NetNode output = null;
		// Skips sinks for closed classes
		if (jobClass.getType() == CLOSED_CLASS) {
			for (int i = 0; i < nodeList.size(); i++) {
				if (counter == nodeList.size()) {
					counter = 0;
				}
				output = nodeList.get(counter++);
				if (output.isSink()) {
					output = null;
				} else {
					break;
				}
			}
		} else {
			if (counter == nodeList.size()) {
				counter = 0;
			}
			output = nodeList.get(counter++);
		}
		return output;
	}

}
