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

package jmt.engine.QueueNet;

import java.util.HashMap;
import java.util.Map;

public class NodeListWithJobNum {

	private NodeList nodeList;
	private Map<NetNode, Map<JobClass, Integer>> jobNumPerNode;
	private int totalNum;
	private Map<NetNode, Integer> totalNumPerNode;

	public NodeListWithJobNum() {
		nodeList = new NodeList();
		jobNumPerNode= new HashMap<NetNode, Map<JobClass, Integer>>();
		totalNum = 0;
		totalNumPerNode = new HashMap<NetNode, Integer>();
	}

	public void add(NetNode node, Map<JobClass, Integer> map) {
		nodeList.add(node);
		jobNumPerNode.put(node, map);
		int sum = 0;
		for (Integer num : map.values()) {
			sum += num.intValue();
		}
		totalNum += sum;
		totalNumPerNode.put(node, Integer.valueOf(sum));
	}

	public NodeList getNodeList() {
		return nodeList;
	}

	public Map<JobClass, Integer> getJobNumPerClass(NetNode node) {
		if (!jobNumPerNode.containsKey(node)) {
			return new HashMap<JobClass, Integer>();
		}
		return jobNumPerNode.get(node);
	}

	public int getTotalNum() {
		return totalNum;
	}

	public int getTotalNum(NetNode node) {
		if (!totalNumPerNode.containsKey(node)) {
			return 0;
		}
		return totalNumPerNode.get(node).intValue();
	}

}
