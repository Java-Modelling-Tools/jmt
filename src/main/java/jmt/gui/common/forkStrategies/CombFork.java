package jmt.gui.common.forkStrategies;

import java.util.HashMap;
import java.util.Map;

public class CombFork extends ForkStrategy {
	private Map<Object, Double> outProbabilities = new HashMap<Object, Double>();
	
	@Override
	public String getName() {
                description = "The tasks will be generated in a subset of the output links (one on each output link of the subset). The dimension of the subset is defined by a probability distribution, "
                                + "the output links that will form the subset are selected randomly.";
		return "Random Subset";
	}

	@Override
	public Map<Object, Double> getOutDetails() {
		return outProbabilities;
	}

	@Override
	public ForkStrategy clone() {
		CombFork cf = new CombFork();
		cf.outProbabilities = new HashMap<Object, Double>(outProbabilities);
		return cf;
	}

	@Override
	public String getClassPath() {
		return "jmt.engine.NetStrategies.ForkStrategies.CombFork";
	}

	@Override
	public boolean isModelStateDependent() {
		return false;
	}

        @Override
        public void removeStation(Object stationKey) {
                outProbabilities.remove(Integer.toString(outProbabilities.size()));
        }

}