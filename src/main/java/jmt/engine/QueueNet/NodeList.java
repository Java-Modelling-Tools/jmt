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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/** This class implements a list of nodes. Note that only classes of QueueNet
 * package can add or remove objects to/from the list.
 * @author Francesco Radaelli
 */
public class NodeList {

	private List<NetNode> nodes;
	private Map<String, NetNode> nodeMap;

	/** Creates a new instance of NodeList class.
	 */
	public NodeList() {
		nodes = new ArrayList<NetNode>();
		nodeMap = new HashMap<String, NetNode>();
	}

	/** Adds a new node to the list.
	 * @param node Reference to the node.
	 */
	public void add(NetNode node) {
		nodes.add(node);
		nodeMap.put(node.getName(), node);
	}

	/** Removes a node from the list.
	 * @param node Reference to the node.
	 */
	public void remove(NetNode node) {
		nodes.remove(node);
		nodeMap.remove(node.getName());
	}

	/** Gets the first node in the list.
	 * @return Reference to the first node.
	 */
	public NetNode getFirst() {
		return nodes.get(0);
	}

	/** Gets the last node in the list.
	 * @return Reference to the last node.
	 */
	public NetNode getLast() {
		return nodes.get(nodes.size() - 1);
	}

	/** Gets the i-th node in the list.
	 * @param index Index of the node.
	 * @return Reference to the i-th node.
	 */
	public NetNode get(int index) {
		return nodes.get(index);
	}

	/** Gets the node with a specific name in the list.
	 * @param name Name of the node.
	 * @return Reference to the node.
	 */
	public NetNode get(String name) {
		return nodeMap.get(name);
	}

	/** Gets the size of the list.
	 * @return Size of the list.
	 */
	public int size() {
		return nodes.size();
	}

	/** Gets an iterator of the list.
	 * @return Iterator of the list.
	 */
	public ListIterator<NetNode> listIterator() {
		return nodes.listIterator();
	}

	/** Converts the list into an array of nodes.
	 * @return Array of nodes in the list.
	 */
	public NetNode[] toArray() {
		return nodes.toArray(new NetNode[nodes.size()]);
	}

	/** Gets the index of a node.
	 * @param node Reference to the node.
	 * @return Index of the node.
	 */
	public int indexOf(NetNode node) {
		return nodes.indexOf(node);
	}

	/** Used to know if a node is in the list.
	 * @param node Reference to the node.
	 * @return true if the node is in the list.
	 */
	public boolean contains(NetNode node) {
		return nodes.contains(node);
	}

}
