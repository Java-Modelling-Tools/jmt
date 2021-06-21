package jmt.engine.NetStrategies.QueuePutStrategies;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NodeSection;

public class LCFSPRStrategy extends QueuePutStrategy implements PreemptiveStrategy {
    public void put(Job job, JobInfoList queue, NodeSection nodeSection) throws NetException {
        queue.addFirst(new JobInfo(job));
    }
}
