package jmt.engine.NetStrategies.QueueGetStrategies;

import jmt.engine.NetStrategies.ServiceStrategies.ZeroServiceTimeStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.QueueNet.*;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;

import javax.swing.*;

/**
 * Implements Limited Polling Get Strategy
 * @author Ahmed Salem
 */

public class LimitedPollingGetStrategy extends PollingGetStrategy {


	private int currentQueue;
	private final int k;
	private int count;
	private JobClassList jobClassList;
	private JobClass jobClass;
	ServiceStrategy[] switchoverStrategies;

	public LimitedPollingGetStrategy(Integer pollingK, ServiceStrategy[] switchoverStrategies) {
		currentQueue = 0;
		k = pollingK;
		count = 0;
		jobClassList = null;
		jobClass = null;
		this.switchoverStrategies = switchoverStrategies;
	}

	public void setPollingQueues(JobClassList jobClassList) {
		this.jobClassList = jobClassList;
		jobClass = jobClassList.get(0);
	}

	public Job nextJob(JobInfoList jobsList){
		if (count < k) {
			if (jobsList.getInternalJobInfoList(jobClass).size() > 0) {
				Job nextJob = jobsList.removeFirst(jobClass).getJob();
				count++;
				return nextJob;
			}
		}

		currentQueue = (currentQueue + 1) % jobClassList.size();
		jobClass = jobClassList.get(currentQueue);
		count = 0;
		return null;
	}

	public Job get(JobInfoList queue) {
		Job job = nextJob(queue);
		for (int i = 0;
			 i < jobClassList.size() && job == null && switchoverStrategies[currentQueue] instanceof ZeroServiceTimeStrategy;
			 i++) {
			job = nextJob(queue);
		}

		return job;
	}

	public Job get(JobInfoList queue, JobClass jobClass) {
		return queue.removeFirst(jobClass).getJob();
	}
	
	@Override
	public JobInfo peek(JobInfoList queue) {
		return queue.getFirstJob();
	}

}
