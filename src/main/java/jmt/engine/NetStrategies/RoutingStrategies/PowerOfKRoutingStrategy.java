/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.engine.NetStrategies.RoutingStrategies;

import jmt.common.exception.NetException;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeList;

import java.util.*;

/**
 * This strategy picks, at random, k resources and sends jobs to the resource with the shortest queue.
 *
 * @author Vaibhav Krishnakumar, Bertoli Marco
 */
public class PowerOfKRoutingStrategy extends PropertyBasedStrategy {

    private final Integer k;
    private Map<NetNode, Integer> memory = null;

    /**
     * Creates a Power of K Routing Strategy with the passed parameter k.
     *
     * @param k the number of output nodes to chose from
     */
    public PowerOfKRoutingStrategy(Integer k, Boolean withMemory) {
        this.k = k;
        if (withMemory) {
            this.memory = new HashMap<>();
        }
    }

    /**
     * This strategy selects the resource with the shortest queue
     * among k output nodes selected at random.
     *
     * @param nodes    List of nodes.
     * @param jobClass class of current job to be routed
     * @return Selected node.
     */
    @Override
	public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		NodeList nodes = ownerNode.getOutputNodes();
        if (nodes.size() == 0) {
            return null;
        }

        NodeList selectedNodes = getSelectedNodes(nodes);
        boolean isClosedClass = jobClass.getType() == JobClass.CLOSED_CLASS;
        if (memory != null) {
            // Initialise memory count on first access
            if (memory.isEmpty()) {
                ListIterator<NetNode> iterator = nodes.listIterator();
                while (iterator.hasNext()) {
                    memory.put(iterator.next(), 0);
                }
            }
            updateMemory(selectedNodes, isClosedClass);
            NetNode out = getBestNode();
            incrementMemory(out);
            return out;
        }

        return super.getOutNode(ownerNode, jobClass);

    }

    @Override
    protected double calculateProperty(NetNode node, JobClass jobClass) throws NetException {
        return calculateQueueLength(node, jobClass.getType() == JobClass.CLOSED_CLASS);
    }

    private void updateMemory(NodeList nodes, boolean isClosedClass) {
        ListIterator<NetNode> iterator = nodes.listIterator();
        try {
            while (iterator.hasNext()) {
                NetNode node = iterator.next();
                memory.put(node, calculateQueueLength(node, isClosedClass));
            }
        } catch (NetException e) {
            System.out.println("Shortest Queue Routing Error: Cannot read queue length from output node");
            e.printStackTrace();
        }
    }

    private NetNode getBestNode() {
        Integer minValue = Collections.min(memory.values());
        NodeList bestNodes = new NodeList();
        for (NetNode node : memory.keySet()) {
            if (memory.get(node).equals(minValue)) {
                bestNodes.add(node);
            }
        }
        int index = getRandomIndexBelow(bestNodes.size());
        return bestNodes.get(index);
    }

    private void incrementMemory(NetNode out) {
        memory.put(out, memory.get(out) + 1);
    }

    /**
     * Selects, at random, k nodes from the given list of nodes.
     * If there are fewer than k nodes given, the list is returned unmodified.
     */
    private NodeList getSelectedNodes(final NodeList nodes) {
        if (nodes.size() <= k) {
            return nodes;
        }

        // Create a set of k unique indices which will be selected
        Set<Integer> indices = new HashSet<>();
        do {
            int index = getRandomIndexBelow(nodes.size());
            indices.add(index);
        } while (indices.size() < k);

        // Create new NodeList with selected nodes
        NodeList selectedNodes = new NodeList();
        for (int index : indices) {
            selectedNodes.add(nodes.get(index));
        }
        return selectedNodes;
    }
}
