package jmt.engine.NetStrategies.QueuePutStrategies;

import java.util.Iterator;
import java.util.List;
import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NodeSection;

public class EDDStrategyPriority extends QueuePutStrategy {

  @Override
  public void put(Job job, JobInfoList queue, NodeSection nodeSection) throws NetException {
    double softDeadline = job.getCurrentStationSoftDeadline();
    if (softDeadline == -1) {
      throw new IllegalArgumentException("Attempting to schedule job with no soft deadline");
    }

    List<JobInfo> list = queue.getInternalJobInfoList();
    if (list.size() == 0) {
      queue.addFirst(new JobInfo(job));
      return;
    }

    int priority = job.getJobClass().getPriority();
    Job current = null;
    int currentPriority = 0;
    Iterator<JobInfo> it = list.iterator();
    int index = -1;
    while (it.hasNext()) {
      current = it.next().getJob();
      index++;
      currentPriority = current.getJobClass().getPriority();
      if (currentPriority <= priority) {
        break;
      }
    }
    if (currentPriority > priority) {
      queue.addLast(new JobInfo(job));
      return;
    } else if (currentPriority < priority) {
      queue.add(index, new JobInfo(job));
      return;
    }

    double currentSoftDeadline = current.getCurrentStationSoftDeadline();
    if (currentSoftDeadline > softDeadline) {
      queue.add(index, new JobInfo(job));
      return;
    }

    while (it.hasNext()) {
      current = it.next().getJob();
      index++;
      currentSoftDeadline = current.getCurrentStationSoftDeadline();
      currentPriority = current.getJobClass().getPriority();
      if (currentPriority < priority || currentSoftDeadline > softDeadline) {
        queue.add(index, new JobInfo(job));
        return;
      }
    }
    queue.addLast(new JobInfo(job));
  }
}
