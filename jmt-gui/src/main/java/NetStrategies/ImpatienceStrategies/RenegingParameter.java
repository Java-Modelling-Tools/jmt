package NetStrategies.ImpatienceStrategies;

import common.distributions.Distribution;

public class RenegingParameter implements ImpatienceParameter {
  Distribution distribution;

  public RenegingParameter() {}

  public RenegingParameter(Distribution distribution) {
    this.distribution = distribution;
  }

  public Distribution getDistribution() {
    return distribution;
  }

  public void setDistribution(Distribution distribution) {
    this.distribution = distribution;
  }
}
