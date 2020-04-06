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
 * This strategy sends jobs to the resource with the least utilization.
 *
 * Date: 12-nov-2005
 * @author Bertoli Marco
 */
public class LeastUtilizationRoutingStrategy extends RoutingStrategy {

	private int CLOSED_CLASS = JobClass.CLOSED_CLASS;
	private double infinity = Double.POSITIVE_INFINITY;
	private byte serviceSection = NodeSection.SERVICE;
	private int property = NodeSection.PROPERTY_ID_UTILIZATION;
	private double epsilon = 1e-14; // Used to make equality checks (100 times machine precision)	

	/**
	 * This strategy selects the resource with the least utilization
	 * among the output nodes.
	 * @param nodeList List of the output nodes.
	 * @param jobClass Class of current job to be routed.
	 * @return Selected node.
	 */
	@Override
	public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		RandomEngine random = ownerNode.getNetSystem().getEngine();
		NodeList nodeList = ownerNode.getOutputNodes();
		if (nodeList.size() == 0) {
			return null;
		}

		// output node candidates... Holds an array of them as on equality of utilization they
		// will be chosen randomly
		NetNode[] output = new NetNode[nodeList.size()];
		int length = 0;
		double leastUtilization = infinity;
		try {
			for (int i = 0; i < nodeList.size(); i++) {
				NetNode node = nodeList.get(i);
				double tmp = 0.0;
				// Checks if output is a sink... If class is open, sinks are preferred as have
				// 0 utilization. If class is closed, avoid it
				if (node.isSink()) {
					if (jobClass.getType() == CLOSED_CLASS) {
						tmp = infinity;
					} else {
						tmp = 0.0;
					}
				} else {
					tmp = node.getSection(serviceSection).getDoubleSectionProperty(property);
				}

				if (Math.abs(tmp - leastUtilization) < epsilon) {
					// This is minimum too, put it in output and increment length
					output[length] = node;
					length++;
				} else if (tmp < leastUtilization) {
					// New minimum value found, put it in output and reset length
					leastUtilization = tmp;
					output[0] = node;
					length = 1;
				}
			}
		} catch (NetException e) {
			System.out.println("Least Utilization Routing Error: Cannot read utilization from output node");
			e.printStackTrace();
			return null;
		}
		if (leastUtilization == infinity) {
			return null;
		} else if (length == 1) {
			return output[0];
		} else {
			return output[(int) Math.floor(random.raw() * length)];
		}
	}

}
