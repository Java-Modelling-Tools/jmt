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

	private Map<NetNode, Map<JobClass, Integer>> nodesWithJobs;
	private NodeList nodes;
	private int total;
	private Map<NetNode, Integer> nodesWithTotals;

	public NodeListWithJobNum() {
		nodesWithJobs= new HashMap<NetNode, Map<JobClass, Integer>>();
		nodes = new NodeList();
		total = 0;
		nodesWithTotals = new HashMap<NetNode, Integer>();
	}

	public void add(NetNode netNode, Map<JobClass, Integer> num) {
		nodesWithJobs.put(netNode, num);
		nodes.add(netNode);
		int sum = 0;
		for (Integer i : num.values()) {
			sum += i.intValue();
		}
		total += sum;
		nodesWithTotals.put(netNode, Integer.valueOf(sum));
	}

	public NodeList getNodeList() {
		return this.nodes;
	}

	public Map<JobClass, Integer> getClassesWithNum(NetNode netNode) {
		return nodesWithJobs.get(netNode);
	}

	public int getTotalNum() {
		return total;
	}

	public int getTotalNum(NetNode netNode) {
		return nodesWithTotals.get(netNode).intValue();
	}

}
