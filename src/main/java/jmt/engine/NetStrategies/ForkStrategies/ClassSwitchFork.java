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

package jmt.engine.NetStrategies.ForkStrategies;

import java.util.HashMap;
import java.util.Map;
import jmt.engine.NetStrategies.ForkStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.QueueNet.NodeListWithJobNum;
import jmt.engine.QueueNet.NodeSection;

/**
 *
 * @author mattia
 */
public class ClassSwitchFork extends ForkStrategy {

	private ClassJobNum jobsPerClass[];
	private String nodeNames[];
	private NetNode[] nodes;
	private JobClassList classList;

	public ClassSwitchFork(ClassJobNum jobsPerClass[]) {
		this.jobsPerClass = jobsPerClass;
		nodeNames = new String[jobsPerClass.length];
		for (int i = 0; i < jobsPerClass.length; i++) {
			nodeNames[i] = jobsPerClass[i].getName();
		}
	}

	@Override
		public NodeListWithJobNum getOutNodes(NetNode ownerNode, JobClass jobClass) {
		//NodeList nodes = ownerNode.getOutputNodes();
		if (nodes == null) {
			//it is the first execution of this method: find the NetNode objects
			nodes = new NetNode[nodeNames.length];
			for (int i = 0; i < nodeNames.length; i++) {
				nodes[i] = ownerNode.getNetSystem().getNode(nodeNames[i]);
			}
		}
		if (classList == null) {
			//it is the first execution of this method: find the JobClassList object
			classList = ownerNode.getJobClasses();
		}

		NodeListWithJobNum nl = new NodeListWithJobNum();
		for (int i = 0; i < jobsPerClass.length; i++) {
			Map<JobClass, Integer> map = new HashMap<JobClass, Integer>();
			String[] classes = jobsPerClass[i].getClasses();
			Integer[] numbers = jobsPerClass[i].getNumbers();
			for (int j = 0; j < classes.length; j++) {
				map.put(classList.get(classes[j]), numbers[j]);
			}
			nl.add(nodes[i], map);
		}
		return nl;
	}

}
