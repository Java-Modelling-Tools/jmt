package jmt.engine.NetStrategies.ImpatienceStrategies;

import jmt.gui.common.distributions.Distribution;

public class RetrialDistribution {

	Distribution distribution;
	public RetrialDistribution(Distribution distribution) {
		this.distribution = distribution;
	}

	public Distribution getDistribution() {
		return distribution;
	}

	public void setDistribution(Distribution distribution) {
		this.distribution = distribution;
	}
}
