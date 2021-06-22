package jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement;

import java.util.ArrayList;
import java.util.List;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.WaitingRequest;

public class BooleanValueImpatienceMeasurement implements ImpatienceMeasurement {
  private int queueLength;
  private boolean booleanValue = false;

  public BooleanValueImpatienceMeasurement(JobInfoList jobsList,  JobInfoList waitingRequests,
      int jobClassPriority, boolean priorityActivated) {
    setQueueLength(jobsList, waitingRequests, jobClassPriority, priorityActivated);
  }

  public int getQueueLength() { return queueLength; }

  /**
   * Calculates the queue length the job will face depending on whether priority is activated or not.
   */
  private void setQueueLength(JobInfoList jobsList,  JobInfoList waitingRequests,
      int jobClassPriority, boolean priorityActivated) {
    if (priorityActivated) {
      List<JobInfo> jobsListBuffer = jobsList.getInternalJobInfoList();
      List<JobInfo> waitingRequestsBuffer = waitingRequests.getInternalJobInfoList();
      List<JobInfo> combinedBuffer = new ArrayList<>();
      combinedBuffer.addAll(jobsListBuffer);
      combinedBuffer.addAll(waitingRequestsBuffer);

      queueLength = 0;
      for (JobInfo jobInfo : combinedBuffer) {
        JobClass jobClass = jobInfo.getJob().getJobClass();
        int priority = jobClass.getPriority();

        if (priority >= jobClassPriority) {
          queueLength++;
        }
      }
    } else {
      queueLength = jobsList.size() + waitingRequests.size();
    }
  }

  public void setBooleanValue (boolean status) { booleanValue = status; }

  public boolean getBooleanValue() { return booleanValue; }
}
