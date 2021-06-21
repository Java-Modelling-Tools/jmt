package jmt.engine.NodeSections;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ServiceStrategies.ZeroServiceTimeStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.QueueNet.*;
import jmt.engine.simEngine.RemoveToken;
import java.util.*;

/**
 * This class implements a polling server.
 * Every class has a specific distribution and a own set of statistical
 * parameters.
 * Every class has walk over (switchover) period
 * @author  Ahmed Salem
 */
public class PollingServer extends ServiceSection {

	private int numberOfServers;
	private int[] numberOfVisitsPerClass;
	private ServiceStrategy[] serviceStrategies;

	private double changeTime = 0;

	private Job jobInService = new Job(null, null);
	private JobClass[] jobsInService;
	private Map<Job, Integer> jobServerMap;
	private List<Boolean> occupiedServers;
	private ServiceStrategy[] switchOverStrategies;
	private JobClass classInService;

	private int busyCounter;

	/**
	 * Creates a new instance of PollingServer.
	 * @param numberOfServers Number of jobs which can be served simultaneously.
	 * @param numberOfVisitsPerClass Number of job visits per class: if null
	 * the server will be single visit.
	 * @param serviceStrategies Array of service strategies, one per class.
	 * @param switchOverStrategies Array of switchover strategies, one per class
	 */
	public PollingServer(Integer numberOfServers, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies, ServiceStrategy[] switchOverStrategies) {
		if (numberOfVisitsPerClass == null) {
			this.numberOfServers = numberOfServers.intValue();
			this.numberOfVisitsPerClass = null;
			this.serviceStrategies = serviceStrategies;
			busyCounter = 0;
		} else {
			this.numberOfServers = numberOfServers.intValue();
			this.numberOfVisitsPerClass = new int[numberOfVisitsPerClass.length];
			for (int i = 0; i < numberOfVisitsPerClass.length; i++) {
				this.numberOfVisitsPerClass[i] = numberOfVisitsPerClass[i].intValue();
			}
			this.serviceStrategies = serviceStrategies;
			busyCounter = 0;
		}
		this.switchOverStrategies = switchOverStrategies;
		classInService = null;
		this.jobsInService = new JobClass[numberOfServers];
		occupiedServers = new ArrayList<>(Collections.nCopies(numberOfServers, false));
		jobServerMap = new HashMap<>();
	}

	@Override
	public double getDoubleSectionProperty(int id) throws NetException {
		switch (id) {
		case PROPERTY_ID_UTILIZATION:
			return jobsList.size() / numberOfServers;
		case PROPERTY_ID_AVERAGE_UTILIZATION:
			return (getTime() <= 0.0) ? 0.0
					: jobsList.getTotalSojournTime() / getTime() / numberOfServers;
		default:
			return super.getDoubleSectionProperty(id);
		}
	}

	@Override
	public double getDoubleSectionProperty(int id, JobClass jobClass) throws NetException {
		switch (id) {
		case PROPERTY_ID_UTILIZATION:
			return jobsList.size(jobClass) / numberOfServers;
		case PROPERTY_ID_AVERAGE_UTILIZATION:
			return (getTime() <= 0.0) ? 0.0
					: jobsList.getTotalSojournTimePerClass(jobClass) / getTime() / numberOfServers;
		default:
			return super.getDoubleSectionProperty(id, jobClass);
		}
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		jobsList.setNumberOfServers(numberOfServers);
		classInService = getJobClasses().get(0);
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		switch (message.getEvent()) {

			case NetEvent.EVENT_START:
				break;

			case NetEvent.EVENT_JOB:

				Job job = message.getJob();

				if (isMine(message)) {
					sendForward(job, 0.0);
					busyCounter--;
					sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
	 				if (busyCounter == numberOfServers) {
						busyCounter = 0;
						waitSwitchover(true);
					}

				} else {
					if (busyCounter < numberOfServers) {
						busyCounter++;

						RemoveToken jobInServiceMessage;
						double serviceTime = serviceStrategies[job.getJobClass().getId()].wait(this, job.getJobClass());
						job.setServiceTime(serviceTime);
						job.setServiceArrivalTime(this.getTime());

						classInService = job.getJobClass();
						jobInServiceMessage = sendMe(job, serviceTime);

						if (busyCounter < numberOfServers) {
							sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
						}

					} else {
						//server is busy
						return MSG_NOT_PROCESSED;
					}
				}
				break;

			case NetEvent.EVENT_POLLING_SERVER_NEXT:

				if (isMine(message)) {
					sendBackward(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0);
				} else if (busyCounter == 0) {
					waitSwitchover(false);
				} else {
					busyCounter = busyCounter+numberOfServers;
				}

				break;

			case NetEvent.EVENT_ACK:
				break;


			case NetEvent.EVENT_STOP:
				break;

			default:
				return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	private void waitSwitchover(boolean skip) throws NetException {
		int walkClassID = (classInService.getId() + 1) % switchOverStrategies.length;
		if (!skip) {
			for (int i = 0; i < switchOverStrategies.length && switchOverStrategies[walkClassID] instanceof ZeroServiceTimeStrategy; i++) {
				walkClassID = (walkClassID + 1) % switchOverStrategies.length;
			}

			if (switchOverStrategies[walkClassID] instanceof ZeroServiceTimeStrategy) {
				classInService = getJobClasses().get(walkClassID);
				sendBackward(NetEvent.EVENT_POLLING_SERVER_READY, null, 0);
				return;
			}
		}

		classInService = getJobClasses().get(walkClassID);
		double waitTime = switchOverStrategies[walkClassID].wait(this, classInService);
		changeTime = getTime();
		sendMe(NetEvent.EVENT_POLLING_SERVER_NEXT, null, waitTime);
	}

	/**
	 * Gets the service strategy for each class.
	 * @return the service strategy for each class.
	 */
	public ServiceStrategy[] getServiceStrategies() {
		return serviceStrategies;
	}
	
	private double getTime(){
		return this.getOwnerNode().getNetSystem().getTime();
	}
}
