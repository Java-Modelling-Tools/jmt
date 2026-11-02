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
 * Earliest Deadline First queue insertion strategy, preemptive variant of the earliest due date discipline.
 */
/**
 * Earliest Deadline First queue insertion strategy, preemptive variant of the earliest due date discipline.
 */
/**
 * Earliest Deadline First queue insertion strategy, preemptive variant of the earliest due date discipline.
 */
/**
 * Earliest Deadline First queue insertion strategy, preemptive variant of the earliest due date discipline.
 */
public class EDFStrategy extends EDDStrategy implements PreemptiveStrategy {

  @Override
  public int compare(Job job1, Job job2) {
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
