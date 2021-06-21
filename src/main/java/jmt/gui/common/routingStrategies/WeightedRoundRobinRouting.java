package jmt.gui.common.routingStrategies;

import java.util.HashMap;
import java.util.Map;

public class WeightedRoundRobinRouting extends RoutingStrategy {

	private Map<Object, Integer> weights = new HashMap<>();

	public WeightedRoundRobinRouting() {
		description = "Jobs are routed to stations connected to the current one " +
						"according to a cyclic algorithm; on each cycle, only stations " +
						"with weights higher than or equal to the current weight are considered. The " +
						"weight is decremented, starting from the max weight for the first cycle.";
	}

	@Override
	public String getName() {
		return "Weighted Round Robin";
	}

	@Override
	public Map<Object, Double> getValues() {
		return null;
	}

	@Override
	public RoutingStrategy clone() {
		WeightedRoundRobinRouting strategy = new WeightedRoundRobinRouting();
		strategy.setWeights(weights);
		return strategy;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.RoutingStrategies" +
						".WeightedRoundRobinStrategy";
	}

	@Override
	public boolean isModelStateDependent() {
		return true;
	}

	@Override
	public void addStation(Object stationKey) {
		weights.put(stationKey, 1);
	}

	@Override
	public void removeStation(Object stationKey) {
		weights.remove(stationKey);
	}

	public void setWeights(Map<Object, Integer> weights) {
		this.weights = weights;
	}

	public Map<Object, Integer> getWeights() {
		return weights;
	}
}
