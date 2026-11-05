package NetStrategies.QueuePutStrategies;

import java.util.Iterator;
import java.util.List;
import jmt.common.exception.NetException;
import NetStrategies.QueuePutStrategy;
import QueueNet.Job;
import QueueNet.JobInfo;
import QueueNet.JobInfoList;
import QueueNet.NodeSection;

/**
 * Earliest Deadline First queue insertion strategy with priorities.
 */
/**
 * Earliest Deadline First queue insertion strategy with priorities.
 */
/**
 * Earliest Deadline First queue insertion strategy with priorities.
 */
/**
 * Earliest Deadline First queue insertion strategy with priorities.
 */
public class EDFStrategyPriority extends EDDStrategyPriority implements PreemptiveStrategy {

  @Override
  public int compare(Job job1, Job job2) {
    int priority1 = job1.getJobClass().getPriority();
    int priority2 = job2.getJobClass().getPriority();
    if (priority1 > priority2) {
      return 1;
    } else if (priority1 < priority2) {
      return -1;
    }

    double softDeadline1 = job1.getCurrentStationSoftDeadline();
    double softDeadline2 = job2.getCurrentStationSoftDeadline();

    if (softDeadline1 < softDeadline2) {
      return 1;
    } else if (softDeadline1 > softDeadline2) {
      return -1;
    } else {
      return -1;
    }
  }
}
