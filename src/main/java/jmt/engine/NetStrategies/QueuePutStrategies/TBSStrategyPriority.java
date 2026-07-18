package jmt.engine.NetStrategies.QueuePutStrategies;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NodeSections.Server;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NodeSection;

/**
 * This is an adaptation of the Total Bandwidth Server scheduling algorithm. We deviate from the
 * original in that job classes with higher priority than the job to be scheduled are considered
 * "periodic" task streams. We take a stochastic
 * estimate of U_p, utilisation of periodic jobs, instead of assuming this as known.
 *
 * @author Ashton Choy
 */
public class TBSStrategyPriority extends EDFStrategy implements PreemptiveStrategy {

  /* The current deadline is shared across all instances of TBSStrategyPriority. */
  static double currentDeadlineAbsolute;

  public TBSStrategyPriority() {
    currentDeadlineAbsolute = 0;
  }

  @Override
  public int compare(Job job1, Job job2) {
    // Precondition: job has been assigned a new deadline by calling put().
    return super.compare(job1, job2);
  }

  @Override
  public void put(Job job, JobInfoList queue, NodeSection nodeSection) throws NetException {
    double serviceTime = job.getServiceTime();
    if (serviceTime < 0.0) {
      Server server = (Server) nodeSection.getOwnerNode().getSection(NodeSection.SERVICE);
      ServiceStrategy[] strategies = server.getServiceStrategies();
      serviceTime = strategies[job.getJobClass().getId()].wait(server, job.getJobClass());
      job.setServiceTime(serviceTime);
    }

    int priority = job.getJobClass().getPriority();

    // Calculate utilization of all "periodic" (higher priority) job classes.
    double u_p = 0;
    int nClasses = nodeSection.getJobClasses().size();
    for (int i = 0; i < nClasses; i++) {
      JobClass cls = nodeSection.getJobClasses().get(i);
      if (cls.getPriority() > priority) {
        u_p += nodeSection.getOwnerNode().getSection(NodeSection.SERVICE)
            .getDoubleSectionProperty(NodeSection.PROPERTY_ID_AVERAGE_UTILIZATION, cls);
      }
    }

    double newDeadlineAbsolute = Math.max(job.getNetSystem().getTime(), currentDeadlineAbsolute)
        + (serviceTime/(1-u_p));

    currentDeadlineAbsolute = newDeadlineAbsolute;

    // Absolute job soft deadlines are stored, NOT relative from when the job enters the station.
    job.setCurrentStationSoftDeadline(newDeadlineAbsolute);


    // Note here that priorities are ignored by TBS, i.e. lower priority "aperiodic" jobs will be
    // scheduled before higher priority "periodic" jobs with lower soft deadlines.
    // If we don't want this, we could respect priorities by extending
    // EDFStrategyPriority instead of EDFStrategy.
    super.put(job, queue, nodeSection);
  }
}
