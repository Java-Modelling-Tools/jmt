package jmt.engine.NetStrategies.RoutingStrategies;

import jmt.engine.NetStrategies.RoutingStrategy;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeList;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WeightedRoundRobinStrategy extends RoutingStrategy {

    private int counter = -1;
    private int currentWeight = 0;
    // Map key is node name, not NetNode - see EmpiricalStrategy for details
    private Map<String, Integer> weights;
    private int max;
    private int gcd;

    public WeightedRoundRobinStrategy(WeightEntry[] entries) {
        populateMap(entries);
        max = Collections.max(weights.values());
        gcd = calculateGCDofWeights();
    }

    private void populateMap(WeightEntry[] entries) {
        weights = new HashMap<>();
        for (WeightEntry entry : entries) {
            if (entry.getValue() instanceof String) {
                String nodeName = (String) entry.getValue();
                Integer weight = entry.getWeight();
                weights.put(nodeName, weight);
            }
        }
    }

    @Override
	public NetNode getOutNode(NetNode ownerNode, JobClass jobClass) {
		NodeList nodes = ownerNode.getOutputNodes();
        int n = nodes.size();
        if (n == 0 || n != weights.size() || max == 0) {
            return null;
        }

        while (true) {
            counter = (counter + 1) % n;
            if (counter == 0) {
                updateCurrentWeight();
            }
			NetNode candidate = nodes.get(counter);
			if (weights.get(candidate.getName()) >= currentWeight) {
			    return candidate;
			}
        }
    }

    private void updateCurrentWeight() {
		currentWeight -= gcd;
		if (currentWeight <= 0) {
		    currentWeight = max;
        }
    }

    private int calculateGCDofWeights() {
        Iterator<Integer> iterator = weights.values().iterator();
        int gcd = iterator.next();
        while (iterator.hasNext()) {
            gcd = gcd(iterator.next(), gcd);
        }
        return gcd;
    }

    private int gcd(Integer a, Integer b) {
        BigInteger bigA = BigInteger.valueOf(a);
        BigInteger bigB = BigInteger.valueOf(b);
        return bigA.gcd(bigB).intValue();
    }
}
