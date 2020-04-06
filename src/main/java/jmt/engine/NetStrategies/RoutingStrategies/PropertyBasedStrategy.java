package jmt.engine.NetStrategies.RoutingStrategies;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.RoutingStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.random.engine.RandomEngine;

import java.util.ArrayList;
import java.util.List;

public abstract class PropertyBasedStrategy extends RoutingStrategy {

    private static final double EPSILON = 1e-14; // Used to make equality checks (100 times machine precision)
	private static final int INFINITY = Integer.MAX_VALUE;
	private RandomEngine random = RandomEngine.makeDefault();

    @Override
	public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		NodeList nodes = ownerNode.getOutputNodes();
        if (nodes.size() == 0) {
            return null;
        }

        // next node candidates... Hold a list of them as on equality of service time they will be chosen randomly
        List<NetNode> next = new ArrayList<>();
        next.add(nodes.get(0));
        double property;

        try {
            // Sets property to first node's value
            property = calculateProperty(next.get(0), jobClass);

            for (int i = 1; i < nodes.size(); i++) {
                NetNode node = nodes.get(i);
                double current = calculateProperty(node, jobClass);

                if (Math.abs(current - property) < EPSILON) {
                    // This is minimum too, so add to list
                    next.add(node);
                } else if (current < property) {
                    // New minimum value found, reset list and put node in
                    property = current;
                    next.clear();
                    next.add(node);
                }
            }
        } catch (NetException e) {
            System.out.println("Property Based Routing Error: Cannot read property from output node");
            e.printStackTrace();
            return null;
        }

        if (Double.isInfinite(property)) {
            return null;
        }
        if (next.size() > 1) {
            return next.get(getRandomIndexBelow(next.size()));
        } else {
            return next.get(0);
        }
    }

    protected abstract double calculateProperty(NetNode node, JobClass jobClass) throws NetException;

	protected final int getRandomIndexBelow(int max) {
		return (int) Math.floor(random.raw() * max);
	}

	protected final int calculateQueueLength(NetNode node, boolean isClosedClass) throws NetException {
		if (node.isSink()) {
			return isClosedClass ? INFINITY : 0;
		} else {
			int property = NodeSection.PROPERTY_ID_RESIDENT_JOBS;
			return node.getSection(NodeSection.INPUT).getIntSectionProperty(property)
					+ node.getSection(NodeSection.SERVICE).getIntSectionProperty(property);
		}
	}

}
