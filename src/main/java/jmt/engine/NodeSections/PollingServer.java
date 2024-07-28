package jmt.engine.NodeSections;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.NodeSection;

/**
 * This class implements a polling server.
 * Every class has a specific distribution and a own set of statistical
 * parameters.
 * Every class has walk over (switchover) period
 * @author  Ahmed Salem
 */
public class PollingServer extends ServiceSection {

	private int numberOfServers;
	private int maxRunningJobs;
	private ServiceStrategy[] serviceStrategies;

	private ServiceStrategy[] switchOverStrategies;
	private JobClass classInService;

	private int busyCounter;
	private boolean needSwitchover;

	public PollingServer(Integer numberOfServers, Integer[] numberOfVisitsPerClass, ServiceStrategy[] serviceStrategies, ServiceStrategy[] switchOverStrategies){
		this(numberOfServers,numberOfVisitsPerClass,serviceStrategies,switchOverStrategies,
				null,null,null,null,null);
	}

	/**
	 * Creates a new instance of PollingServer.
	 * @param numberOfServers Number of jobs which can be served simultaneously.
	 * @param numberOfVisits Number of job visits per class (not used).
	 * @param serviceStrategies Array of service strategies, one per class.
	 * @param switchOverStrategies Array of switchover strategies, one per class
	 */
	public PollingServer(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
											 ServiceStrategy[] switchOverStrategies,
											 Integer[] serverNumRequired,
											 String[] serverNames,
											 Integer[] serversPerServerType,
											 Object[] serverCompatibilities,
											 String schedulingPolicy ) {
		this.numberOfServers = numberOfServers.intValue();
		this.serviceStrategies = serviceStrategies;
		this.switchOverStrategies = switchOverStrategies;
		classInService = null;
		busyCounter = 0;
		needSwitchover = false;
	}

	@Override
	public double getDoubleSectionProperty(int id) throws NetException {
		NetSystem netSystem = getOwnerNode().getNetSystem();
		switch (id) {
			case PROPERTY_ID_UTILIZATION:
				return jobsList.size() / numberOfServers;
			case PROPERTY_ID_AVERAGE_UTILIZATION:
				return (netSystem.getTime() <= 0.0) ? 0.0
						: jobsList.getTotalSojournTime() / netSystem.getTime() / numberOfServers;
			default:
				return super.getDoubleSectionProperty(id);
		}
	}

	@Override
	public double getDoubleSectionProperty(int id, JobClass jobClass) throws NetException {
		NetSystem netSystem = getOwnerNode().getNetSystem();
		switch (id) {
			case PROPERTY_ID_UTILIZATION:
				return jobsList.size(jobClass) / numberOfServers;
			case PROPERTY_ID_AVERAGE_UTILIZATION:
				return (netSystem.getTime() <= 0.0) ? 0.0
						: jobsList.getTotalSojournTimePerClass(jobClass) / netSystem.getTime() / numberOfServers;
			default:
				return super.getDoubleSectionProperty(id, jobClass);
		}
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		maxRunningJobs = numberOfServers;
		int stationCapacity = ((Queue) getOwnerNode().getSection(NodeSection.INPUT)).getStationCapacity();
		if (stationCapacity > 0) {
			maxRunningJobs = Math.min(maxRunningJobs, stationCapacity);
		}
		jobsList.setNumberOfServers(numberOfServers);
		classInService = getJobClasses().getFirst();
	}

	@Override
	protected int process(NetMessage message) throws NetException {
		switch (message.getEvent()) {

			case NetEvent.EVENT_START:
				break;

			case NetEvent.EVENT_JOB:
				Job job = message.getJob();
				if (isMine(message)) {
					job.setServiceTime(-1.0);
					sendForward(job, 0.0);
				} else {
					if (busyCounter >= maxRunningJobs) {
						return MSG_NOT_PROCESSED;
					}

					double serviceTime = serviceStrategies[job.getJobClass().getId()].wait(this, job.getJobClass());
					if (serviceTime < 0.0) {
						serviceTime = serviceStrategies[job.getJobClass().getId()].wait(this, job.getJobClass());
						job.setServiceTime(serviceTime);
					}
					classInService = job.getJobClass();
					sendMe(job, serviceTime);

					busyCounter++;
					if (busyCounter < maxRunningJobs) {
						sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
					}
				}
				break;

			case NetEvent.EVENT_POLLING_SERVER_NEXT:
				if (busyCounter <= 0) {
					waitSwitchover();
					needSwitchover = false; // GC added
				} else {
					// GC: this could be a problem for an extension with multiple polling servers
					needSwitchover = true;
				}
				break;

			case NetEvent.EVENT_ACK:
				if (busyCounter <= 0) {
					return MSG_NOT_PROCESSED;
				}
				if (busyCounter >= maxRunningJobs) {
					sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
				}
				busyCounter--;
				if (busyCounter <= 0 && needSwitchover) {
					waitSwitchover();
					needSwitchover = false;
				}
				break;


			case NetEvent.EVENT_STOP:
				break;

			default:
				return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	private void waitSwitchover() throws NetException {
		int walkClassID = (classInService.getId() + 1) % switchOverStrategies.length;
		classInService = getJobClasses().get(walkClassID);
		// the switchover time follows the distribution of the job class that is in service
		// but it is accumulated by the next job
		double waitTime = switchOverStrategies[classInService.getId()].wait(this, getJobClasses().get(walkClassID));
		sendBackward(NetEvent.EVENT_POLLING_SERVER_READY, null, waitTime);
	}

	/**
	 * Gets the service strategy for each class.
	 * @return the service strategy for each class.
	 */
	public ServiceStrategy[] getServiceStrategies() {
		return serviceStrategies;
	}

}
