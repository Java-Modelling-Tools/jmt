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

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.RoutingStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.random.engine.RandomEngine;

/**
 * This strategy sends jobs to the resource with the shortest response time.
 *
 * Date: 13-nov-2005
 * @author Bertoli Marco
 */
public class ShortestResponseTimeRoutingStrategy extends RoutingStrategy {

	private int CLOSED_CLASS = JobClass.CLOSED_CLASS;
	private int infinityQ = Integer.MAX_VALUE;
	private double infinityR = Double.POSITIVE_INFINITY;
	private byte inputSection = NodeSection.INPUT;
	private byte serviceSection = NodeSection.SERVICE;
	private int propertyQ = NodeSection.PROPERTY_ID_RESIDENT_JOBS;
	private int propertyR = NetNode.PROPERTY_ID_RESIDENCE_TIME;
	private double epsilon = 1e-14; // Used to make equality checks (100 times machine precision)
	private RandomEngine random = RandomEngine.makeDefault();

	/**
	 * This strategy selects the resource with the shortest response time for this jobClass
	 * among the output nodes.
	 * @param nodeList List of the output nodes.
	 * @param jobClass Class of current job to be routed.
	 * @return Selected node.
	 */
	@Override
	public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		NodeList nodeList = ownerNode.getOutputNodes();
		if (nodeList.size() == 0) {
			return null;
		}

		// output node candidates... Holds an array of them as on equality of response time they
		// will be chosen randomly
		NetNode[] output = new NetNode[nodeList.size()];
		int length = 0;
		boolean foundZeroQueue = false;
		double leastRTime = infinityR;
		try {
			for (int i = 0; i < nodeList.size(); i++) {
				NetNode node = nodeList.get(i);
				int tmpQ = 0;
				double tmpR = 0.0;
				// Checks if output is a sink... If class is open, sinks are preferred as have
				// 0 queue length and response time. If class is closed, avoid it
				if (node.isSink()) {
					if (jobClass.getType() == CLOSED_CLASS) {
						tmpQ = infinityQ;
						tmpR = infinityR;
					} else {
						tmpQ = 0;
						tmpR = 0.0;
					}
				} else {
					tmpQ = node.getSection(inputSection).getIntSectionProperty(propertyQ)
							+ node.getSection(serviceSection).getIntSectionProperty(propertyQ);
					tmpR = node.getDoubleNodeProperty(propertyR, jobClass);
				}

				if (tmpQ == 0 && !foundZeroQueue) {
					// Zero queue found, put it in output and reset length
					foundZeroQueue = true;
					leastRTime = tmpR;
					output[0] = node;
					length = 1;
				} else if ((tmpQ == 0 && foundZeroQueue) || (tmpQ != 0 && !foundZeroQueue)) {
					if (Math.abs(tmpR - leastRTime) < epsilon) {
						// This is minimum too, put it in output and increment length
						output[length] = node;
						length++;
					} else if (tmpR < leastRTime) {
						// New minimum value found, put it in output and reset length
						leastRTime = tmpR;
						output[0] = node;
						length = 1;
					}
				}
			}
		} catch (NetException e) {
			System.out.println("Shortest Response Time Routing Error: Cannot read queue length or response time from output node");
			e.printStackTrace();
			return null;
		}
		if (leastRTime == infinityR) {
			return null;
		} else if (length == 1) {
			return output[0];
		} else {
			return output[(int) Math.floor(random.raw() * length)];
		}
	}

}
