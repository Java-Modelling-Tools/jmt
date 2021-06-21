package jmt.engine.NetStrategies.QueueGetStrategies;

import jmt.engine.NetStrategies.QueueGetStrategy;
import jmt.engine.QueueNet.JobClassList;


/**
 * Polling Get Strategies
 * @author Ahmed Salem
 */

public abstract class PollingGetStrategy extends QueueGetStrategy {
    public abstract void setPollingQueues(JobClassList jobClassList);
}
