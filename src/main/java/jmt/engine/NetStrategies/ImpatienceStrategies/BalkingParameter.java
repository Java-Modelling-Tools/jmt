package jmt.engine.NetStrategies.ImpatienceStrategies;

import static jmt.gui.common.CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY;
import static jmt.gui.common.CommonConstants.STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY;

import jmt.gui.common.serviceStrategies.LDStrategy;

public class BalkingParameter implements ImpatienceParameter {
  private LDStrategy ldStrategy;
  private boolean priorityActivated;

  public BalkingParameter(String serverStationQueuePolicy) {
    ldStrategy = new LDStrategy();
    updatePriority(serverStationQueuePolicy);
  }

  public BalkingParameter(LDStrategy ldStrategy) {
    this.ldStrategy = ldStrategy;
  }

  public LDStrategy getLdStrategy() {
    return ldStrategy;
  }

  public void setLdStrategy(LDStrategy ldStrategy) {
    this.ldStrategy = ldStrategy;
  }

  public void updatePriority(String serverStationQueuePolicy) {
    priorityActivated = (serverStationQueuePolicy == STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY
        || serverStationQueuePolicy == STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY);
  }

  public boolean getPriorityActivated() {
    return priorityActivated;
  }

  public void setPriorityActivated(boolean isActivated) {
    priorityActivated = isActivated;
  }
}
