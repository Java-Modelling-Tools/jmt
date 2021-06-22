package jmt.engine.NetStrategies.QueueGetStrategies;

import jmt.engine.NetStrategies.ServiceStrategies.ZeroServiceTimeStrategy;
import jmt.engine.QueueNet.*;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements Gated Polling Get Strategy
 * @author Ahmed Salem
 */

public class GatedPollingGetStrategy extends PollingGetStrategy {


	private int currentQueue;
	private List<JobInfo> queueJobInfoList;
	private JobClassList jobClassList;
	private JobClass jobClass;
	private ServiceStrategy[] switchoverStrategies;

	public GatedPollingGetStrategy(ServiceStrategy[] switchoverStrategies) {
		currentQueue = 0;
		queueJobInfoList = null;
		jobClassList = null;
		jobClass = null;
		this.switchoverStrategies = switchoverStrategies;

	}

	public Job nextJob(JobInfoList jobsList) {

		if (queueJobInfoList == null) {
			jobClass = jobClassList.get(currentQueue);
			queueJobInfoList = new ArrayList<>(jobsList.getInternalJobInfoList(jobClass));
		}

		if (queueJobInfoList.size() > 0) {
			JobInfo jobInfo = queueJobInfoList.get(0);
			jobsList.remove(jobInfo);
			queueJobInfoList.remove(0);
			return jobInfo.getJob();
		}

		currentQueue = (currentQueue + 1) % jobClassList.size();
		jobClass = jobClassList.get(currentQueue);

		queueJobInfoList = null;

		return null;

	}

	public void setPollingQueues(JobClassList jobClassList) {
		this.jobClassList = jobClassList;
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
