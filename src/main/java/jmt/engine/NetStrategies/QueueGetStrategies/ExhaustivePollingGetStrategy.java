package jmt.engine.NetStrategies.QueueGetStrategies;

import jmt.engine.NetStrategies.ServiceStrategies.ZeroServiceTimeStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.QueueNet.*;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Implements Exhaustive Polling Get Strategy
 * @author Ahmed Salem
 */

public class ExhaustivePollingGetStrategy extends PollingGetStrategy {

	private int currentQueue;
	private Iterator<JobInfo> iterator;
	private JobClassList jobClassList;
	private JobClass jobClass;
	ServiceStrategy[] switchoverStrategies;

	public ExhaustivePollingGetStrategy(ServiceStrategy[] switchoverStrategies) {
		currentQueue = -1;
		iterator = new ArrayList<JobInfo>().listIterator();
		jobClassList = null;
		jobClass = null;
		this.switchoverStrategies = switchoverStrategies;

	}

	public Job nextJob(JobInfoList jobsList) {
	    if (jobClass != null) {
			if (jobsList.getInternalJobInfoList(jobClass).size() > 0) {
				Job job = jobsList.removeFirst(jobClass).getJob();
				return job;
			}
		}

		currentQueue = (currentQueue + 1) % jobClassList.size();
		jobClass = jobClassList.get(currentQueue);
		return null;
	}


	public void setPollingQueues(JobClassList jobClassList) {
		this.jobClassList = jobClassList;
		jobClass = jobClassList.get(0);
		currentQueue = 0;
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
