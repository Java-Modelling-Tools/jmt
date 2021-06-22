/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.engine.QueueNet;

import java.util.ListIterator;

import jmt.common.exception.NetException;
import jmt.engine.NodeSections.Queue;
import jmt.engine.NodeSections.Storage;
import jmt.engine.dataAnalysis.InverseMeasure;
import jmt.engine.dataAnalysis.Measure;
import jmt.engine.dataAnalysis.SimParameters;
import jmt.engine.log.JSimLogger;
import jmt.engine.simEngine.*;

import java.util.ListIterator;

/**
 * This class implements a generic QueueNetwork node.
 * @author Francesco Radaelli, Stefano Omini, Marco Bertoli
 * 
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public class NetNode extends SimEntity {

	//these constants are used in exception messages

	/** Unable to broadcast a message */
	public static final int EXCEPTION_UNABLE_TO_BROADCAST = 0x0001;
	/** Required measure does not exist */
	public static final int EXCEPTION_MEASURE_DOES_NOT_EXIST = 0x0002;
	/** Input section has been already defined. */
	public static final int EXCEPTION_INPUT_SECTION_ALREADY_DEFINED = 0x0003;
	/** Service section has been already defined.  */
	public static final int EXCEPTION_SERVICE_SECTION_ALREADY_DEFINED = 0x0004;
	/** Output section has been already defined. */
	public static final int EXCEPTION_OUTPUT_SECTION_ALREADY_DEFINED = 0x0005;
	/** Required property is not available */
	public static final int EXCEPTION_PROPERTY_NOT_AVAILABLE = 0x0006;

	//ID of properties

	/** Property ID: number of jobs arriving at this node */
	public static final int PROPERTY_ID_ARRIVING_JOBS = 0x0001;
	/** Property ID: number of jobs departing from this node */
	public static final int PROPERTY_ID_DEPARTING_JOBS = 0x0002;
	/** Property ID: number of jobs inside this node */
	public static final int PROPERTY_ID_RESIDENT_JOBS = 0x0003;
	/** Property ID: residence time */
	public static final int PROPERTY_ID_RESIDENCE_TIME = 0x0004;

	private BlockingRegion region; // This is set only if this station is input station of a blocking region

	/** A logger */
	protected JSimLogger logger = JSimLogger.getLogger(this.getClass());

	/**
	 * The QueueNetwork which this NetNode belong to.
	 */
	protected QueueNetwork network;

	private JobInfoList jobsList;

	/**
	 * Input section of the NetNode.
	 */
	protected NodeSection inputSection;

	/**
	 * Service section of the NetNode.
	 */
	protected NodeSection serviceSection;

	/**
	 * Output section of the NetNode.
	 */
	protected NodeSection outputSection;

	private SimEvent receiveBuffer;

	//temp variables to contain message and message event type
	private NetMessage message;
	private int eventType;

	private boolean stopped;

	private NodeList InputNodes;

	private NodeList OutputNodes;

	private SimParameters simParameters;

	/**
	 * Creates a new instance of NetNode.
	 * @param name Name of the NetNode.
	 */
	public NetNode(String name) {
		super(name);
		InputNodes = new NodeList();
		OutputNodes = new NodeList();
		inputSection = serviceSection = outputSection = null;
		receiveBuffer = new SimEvent();
		stopped = false;
	}

	/**
	 * Adds a section to the node.
	 * @param section Reference to the section to be added.
	 * @throws NetException if the section has already been defined.
	 */
	public void addSection(NodeSection section) throws NetException {
		switch (section.getSectionID()) {
		case NodeSection.INPUT:
			if (inputSection == null) {
				this.inputSection = section;
			} else {
				throw new NetException(this, EXCEPTION_INPUT_SECTION_ALREADY_DEFINED,
						"input section has been already defined");
			}
			break;
		case NodeSection.SERVICE:
			if (serviceSection == null) {
				this.serviceSection = section;
			} else {
				throw new NetException(this, EXCEPTION_SERVICE_SECTION_ALREADY_DEFINED,
						"service section has been already defined");
			}
			break;
		case NodeSection.OUTPUT:
			if (outputSection == null) {
				this.outputSection = section;
			} else {
				throw new NetException(this, EXCEPTION_OUTPUT_SECTION_ALREADY_DEFINED,
						"output section has been already defined");
			}
			break;
		}
	}

	/**
	 * Connects the output of this netNode to the input of an another
	 * netNode.
	 * @param netNode netNode to be linked.
	 */
	public void connect(NetNode netNode) {
		OutputNodes.add(netNode);
		netNode.InputNodes.add(this);
	}

	/**
	 * Gets the JobInfoList of this node.
	 */
	public JobInfoList getJobInfoList() {
		return jobsList;
	}

	/**
	 * Gets input nodes.
	 * @return Input nodes linked to this node.
	 */
	public NodeList getInputNodes() {
		return InputNodes;
	}

	/**
	 * Gets output nodes.
	 * @return Output nodes linked to this node.
	 */
	public NodeList getOutputNodes() {
		return OutputNodes;
	}

	/**
	 * Gets an integer type property related to this node.
	 * @param Id Property identifier.
	 * @return Property value.
	 * @throws NetException if the property is not available.
	 */
	public int getIntNodeProperty(int Id) throws NetException {
		switch (Id) {
		case PROPERTY_ID_ARRIVING_JOBS:
			return jobsList.getJobsIn();
		case PROPERTY_ID_DEPARTING_JOBS:
			return jobsList.getJobsOut();
		case PROPERTY_ID_RESIDENT_JOBS:
			return jobsList.size();
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets an integer type property of this node related to a specified
	 * job class.
	 * @param Id Property identifier.
	 * @param JobClass JobClass.
	 * @return Property value.
	 * @throws NetException if the property is not available.
	 */
	public int getIntNodeProperty(int Id, JobClass JobClass) throws NetException {
		switch (Id) {
		case PROPERTY_ID_ARRIVING_JOBS:
			return jobsList.getJobsInPerClass(JobClass);
		case PROPERTY_ID_DEPARTING_JOBS:
			return jobsList.getJobsOutPerClass(JobClass);
		case PROPERTY_ID_RESIDENT_JOBS:
			return jobsList.size(JobClass);
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a double type property of this node.
	 * @param Id Property identifier.
	 * @return Property value.
	 * @throws NetException if the property is not available.
	 */
	public double getDoubleNodeProperty(int Id) throws NetException {
		switch (Id) {
		case PROPERTY_ID_RESIDENCE_TIME:
			return jobsList.getLastJobSojournTime();
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a double type property of this node related to a specified
	 * job class.
	 * @param Id Property identifier.
	 * @param JobClass JobClass.
	 * @return Property value.
	 * @throws NetException if the property is not available.
	 */
	public double getDoubleNodeProperty(int Id, JobClass JobClass) throws NetException {
		switch (Id) {
		case PROPERTY_ID_RESIDENCE_TIME:
			return jobsList.getLastJobSojournTimePerClass(JobClass);
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a generic object type property of this node.
	 * @param Id Property identifier.
	 * @return Property value.
	 * @throws NetException if the property is not available.
	 */
	public Object getObjectNodeProperty(int Id) throws NetException {
		switch (Id) {
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a generic object type property of this node related to a
	 * specified job class.
	 * @param Id Property identifier.
	 * @param JobClass JobClass.
	 * @return Property value.
	 * @throws NetException if the property is not available.
	 */
	public Object getObjectNodeProperty(int Id, JobClass JobClass) throws NetException {
		switch (Id) {
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a section of the node.
	 * @param Id Section identifier (constants defined in NodeSection).
	 * @return Node section.
	 * @throws NetException
	 */
	public NodeSection getSection(int Id) throws NetException {
		switch (Id) {
		case NodeSection.INPUT:
			return inputSection;
		case NodeSection.SERVICE:
			return serviceSection;
		case NodeSection.OUTPUT:
			return outputSection;
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required section is not available.");
		}
	}

	/**
	 * Gets the Network owner of this node.
	 * @return Value of property Network.
	 */
	public QueueNetwork getQueueNet() {
		return network;
	}

	public GlobalJobInfoList getGlobalJobInfoList() {
		return network.getJobInfoList();
	}

	/**
	 * Analyzes a measure in the node.
	 * @param measureName name of the measure to be activated.
	 * @param jobClass Job class to be analyzed.
	 * @param measurement measure to be activated.
	 * @throws NetException
	 */
	public void analyze(int measureName, JobClass jobClass, Measure measurement) throws NetException {
		switch (measureName) {
		case SimConstants.LIST_NUMBER_OF_JOBS:
			jobsList.analyzeQueueLength(jobClass, measurement);
			break;
		case SimConstants.LIST_RESPONSE_TIME:
			jobsList.analyzeResponseTime(jobClass, measurement);
			break;
		case SimConstants.LIST_RESIDENCE_TIME:
			jobsList.analyzeResidenceTime(jobClass, measurement);
			break;
		case SimConstants.LIST_DROP_RATE:
			jobsList.analyzeDropRate(jobClass, (InverseMeasure) measurement);
			break;
			case SimConstants.LIST_RENEGING_RATE:
				jobsList.analyzeRenegingRate(jobClass, (InverseMeasure) measurement);
				break;
			case SimConstants.LIST_BALKING_RATE:
				jobsList.analyzeBalkingRate(jobClass, (InverseMeasure) measurement);
				break;
			case SimConstants.LIST_RETRIAL_RATE:
				jobsList.analyzeRetrialAttemptsRate(jobClass, (InverseMeasure) measurement);
				break;
			case SimConstants.LIST_RETRIAL_ORBIT_SIZE:
				jobsList.analyzeRetrialOrbitSize(jobClass, measurement);
				break;
			case SimConstants.LIST_WAITING_TIME:
				jobsList.analyzeWaitingTime(jobClass, measurement);
				break;
			case SimConstants.LIST_BUSY_SERVERS:
				jobsList.analyzeBusyServersVar(jobClass, measurement);
				break;
		case SimConstants.RESPONSE_TIME_PER_SINK:
			jobsList.analyzeResponseTimePerSink(jobClass, measurement);
			break;
		case SimConstants.THROUGHPUT_PER_SINK:
			jobsList.analyzeThroughputPerSink(jobClass, (InverseMeasure) measurement);
			break;
		default:
			throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
		}
	}

	public boolean isRunning() {
		return (state != SimEntity.FINISHED);
	}

	/**
	 * Gets the list of the job classes of the owner queue network.
	 * @return Queue network job classes.
	 */
	public JobClassList getJobClasses() {
		return network.getJobClasses();
	}

	/**
	 * Gets the first NetMessage in the queue which validates a specific
	 * predicate.
	 * @param message Reference to a NetMessage buffer to be filled with
	 * the message information.
	 */
	protected void receive(NetMessage message) {
		//We want to set some properties of NetMessage parameter using the
		//information contained in a tag of the received SimEvent
		//Remember that:
		//a >> b means to "right-shift" of "b" positions the bits of "a"
		//a << b means to "left-shift" of "b" positions the bits of "a"
		//
		//int type is made of 32 bit (= 4 Bytes = 8 words)
		//
		//
		//EVENT_MASK        = 0x0000FFFF;    (as defined in NetEvent)
		//SOURCE_MASK       = 0xFF000000;    (as defined in NodeSection)
		//DESTINATION_MASK  = 0x00FF0000;    (as defined in NodeSection)
		//
		//SOURCE_SHIFT      = 24;            (as defined in NodeSection)
		//DESTINATION_SHIFT = 16;            (as defined in NodeSection)

		//receiveBuffer, that is the received SimEvent, has a tag with this structure:
		//      0xSSDDEEEE
		// where    SS -> 1 byte (=2 words) referring to source
		//          DD -> 1 byte (=2 words) referring to destination
		//          EEEE -> 2 bytes (=4 words) referring to event
		//This tag contains some useful informations.

		//To set event, sourceSection and destinationSection of the parameter NetMessage
		//some binary shifts have to be realized (corresponding bits must be recognized
		//in simEvent tag using the correct mask and then they must be shifted as required by the
		//definitions of these properties)

		//set event (int)
		int section;
		message.setEvent(receiveBuffer.getTag() & NetEvent.EVENT_MASK);

		//set sourceSection and destinationSection (byte)
		section = receiveBuffer.getTag() & NodeSection.SOURCE_MASK;
		section >>= NodeSection.SOURCE_SHIFT;
		message.setSourceSection((byte) section);

		section = receiveBuffer.getTag() & NodeSection.DESTINATION_MASK;
		section >>= NodeSection.DESTINATION_SHIFT;
		message.setDestinationSection((byte) section);

		//set other properties
		message.setData(receiveBuffer.getData());
		message.setTime(receiveBuffer.eventTime());
		message.setSource(getNetSystem().getNode(receiveBuffer.getSrc()));
		message.setDestination(getNetSystem().getNode(receiveBuffer.getDest()));

		//Look if message is a job message and if job is arriving at this node (from the node
		//itself or from another node)
				int event = message.getEvent();
		Job job = message.getJob();
		switch(event) {
			case NetEvent.EVENT_JOB:
			if ((message.getSource() != this) || ((message.getSource() == this)
					&& (message.getSourceSection() == NodeSection.OUTPUT && message.getDestinationSection() == NodeSection.INPUT))) {
				inputSection.updateVisitPath(job);
				if (inputSection.automaticUpdateNodeJobinfolist()) {
					jobsList.add(new JobInfo(job));
				}
			}
			break;
			case NetEvent.EVENT_RETRIAL:
				inputSection.updateVisitPath(job);
				break;
			case NetEvent.EVENT_RETRIAL_JOB:
				jobsList.add(new JobInfo(job));
				break;

		}
	}

	/**
	 * This method implements the body of a NetNode.
	 */
	@Override
	public final void body() {
		message = new NetMessage();
		try {
			receiveBuffer = getEvbuf();

			//receive a new message
			receive(message);
			eventType = message.getEvent();

			//if the deferred queue (where we put messages when the node is busy)
			//contains an abort event then poison the node.
			if (simWaiting(new SimTypeP(NetEvent.EVENT_ABORT)) > 0) {
				poison();
			}

			//process last event
			if (eventType == NetEvent.EVENT_KEEP_AWAKE) {
				poison();
			}

			//receive a stop event
			if (eventType == NetEvent.EVENT_STOP) {
				handleStopMessages();
			}

			// if this node has been stopped sends automatically acks
			// to the node which sent job to this one.
			if (stopped) {
				if (eventType == NetEvent.EVENT_JOB) {
					send(NetEvent.EVENT_ACK, message.getJob(), 0.0, NodeSection.NO_ADDRESS, message.getSourceSection(), message.getSource());
				}
			} else {
				dispatch(message);
			}

			simGetNext(SimSystem.SIM_ANY);
		} catch (NetException ex) {
			ex.printStackTrace();
		}
	}

	private void handleStopMessages() throws NetException {
		if (!stopped) {
			stopped = true;
			NetMessage msg = new NetMessage();
			msg.setEvent(NetEvent.EVENT_STOP);
			msg.setDestination(this);
			msg.setSource(this);
			msg.setSourceSection(NodeSection.NO_ADDRESS);
			msg.setTime(getNetSystem().getTime());
			msg.setDestinationSection(NodeSection.INPUT);
			dispatch(msg);
			msg = (NetMessage) message.clone();
			msg.setDestinationSection(NodeSection.SERVICE);
			dispatch(msg);
			msg = (NetMessage) message.clone();
			msg.setDestinationSection(NodeSection.OUTPUT);
			dispatch(msg);
		}
	}

	/**
	 * This method should be overridden to implement an own dispatch.
	 * This method implements the events dispacther of the NetNode; it
	 * <b><u>should never</u></b> remains busy for a long time.
	 * Remember to call the superclass dispatch method if you want to
	 * inherit all the superclass handled events.
	 */
	protected void dispatch(NetMessage message) throws NetException {
		int processed = NodeSection.MSG_NOT_PROCESSED;
		NetNode source = message.getSource();
		byte sourceSection = message.getSourceSection();
		byte destinationSection = message.getDestinationSection();

		//message goes through this switch depending on the destination section
		//if the message is correctly processed by the appropriate node section
		//then the processed variable contains a value different from the initial
		//MSG_NOT_PROCESSED.
		switch (destinationSection) {
		case NodeSection.INPUT:
			// checks jobs routing
			if (message.getEvent() == NetEvent.EVENT_JOB) {
				if (source != this) {
					if (sourceSection != NodeSection.OUTPUT) {
						//the exterior source section can be the input section only
						//in case of redirecting queue
						NodeSection sourceSect = source.getSection(sourceSection);

						//check if this job has been redirected, otherwise the message
						//cannot be dispatched
						boolean redirectedByQueue = (sourceSect instanceof Queue) && (((Queue) sourceSect).isRedirectionON());
						boolean redirectedByStorage = (sourceSect instanceof Storage) && (((Storage) sourceSect).isRedirectionON());
						if (!redirectedByQueue && !redirectedByStorage) {
							break;
						}
					}
				}
			}

			// If this message is JOB and we are reference node for that job, signals it
			// This is needed for global measures - Bertoli Marco
			if (sourceSection == NodeSection.OUTPUT && message.getEvent() == NetEvent.EVENT_JOB) {
				Job job = message.getJob();
				if (job.getJobClass().getReferenceNodeName().equals(getName()) && !(job instanceof ForkJob)) {
					getQueueNet().getJobInfoList().recycleJob(job);
				}
			}

			if (inputSection != null) {
				processed = inputSection.receive(message);
			} else if (sourceSection == NodeSection.NO_ADDRESS) {
				processed = NodeSection.MSG_PROCESSED;
			}
			break;

		case NodeSection.SERVICE:
			// checks jobs routing
			if (message.getEvent() == NetEvent.EVENT_JOB) {
				if (source != this) {
					break;
				}
			}
			if (serviceSection != null) {
				processed = serviceSection.receive(message);
			} else if (sourceSection == NodeSection.NO_ADDRESS) {
				processed = NodeSection.MSG_PROCESSED;
			}
			break;

		case NodeSection.OUTPUT:
			// checks jobs routing
			if (message.getEvent() == NetEvent.EVENT_JOB) {
				if (source != this) {
					break;
				} else if (sourceSection == NodeSection.INPUT) {
					break;
				}
			}
			if (outputSection != null) {
				processed = outputSection.receive(message);
			} else if (sourceSection == NodeSection.NO_ADDRESS) {
				processed = NodeSection.MSG_PROCESSED;
			}
			break;
		}

		// This situation should be avoided as it is probably an error.
		if (processed == NodeSection.MSG_NOT_PROCESSED) {
			String src = message.getSource().getName() + ":" + message.getSourceSection();
			String dst = message.getDestination().getName() + ":" + message.getDestinationSection();
			String job = "";
			if (message.getJob() != null) {
				job = " Attached job with id: " + message.getJob().getId() + " class: " + message.getJob().getJobClass().getName();
			}
			logger.warn(getNetSystem().getTime() + " - Message " + message.getEvent() + " from " + src + " to " + dst + " not processed." + job);
		}
	}

	/**
	 * Sends a message to a NetNode.
	 * @param event Event tag.
	 * @param data  Data to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param sourceSection The source section.
	 * @param destinationSection The destination section.
	 * @param destination The destination node.
	 * @return a token to remove sent event.
	 */
	RemoveToken send(int event, Object data, double delay, byte sourceSection, byte destinationSection, NetNode destination) {
		//Look if message is a job message and if job is leaving this node
		if (event == NetEvent.EVENT_JOB) {
			if ((destination != this) || ((destination == this)
					&& (sourceSection == NodeSection.OUTPUT && destinationSection == NodeSection.INPUT))) {
				if (outputSection.automaticUpdateNodeJobinfolist()) {
					removeJobFromNodeJobsList((Job) data);
				}
			}
		}
		 if (event == NetEvent.EVENT_RETRIAL) {
			 removeJobFromNodeJobsListOnly((Job) data, event);
		}

		//
		//EVENT_MASK        = 0x0000FFFF;
		//SOURCE_MASK       = 0xFF000000;
		//DESTINATION_MASK  = 0x00FF0000;
		//
		//SOURCE_SHIFT = 24;
		//DESTINATION_SHIFT = 16;
		//it is the opposite than receive(...): in this case we have event, source
		//and destination sections and we have to summarize them into a single tag
		//of a simEvent which will be scheduled by the simulator
		int tag;
		tag = event & NetEvent.EVENT_MASK;
		tag += sourceSection << NodeSection.SOURCE_SHIFT;
		tag += destinationSection << NodeSection.DESTINATION_SHIFT;
		return simSchedule(destination.getId(), delay, tag, data);
	}

	/**
	 * Helper function to remove job from the NodeJobsList
	 * @param data the jobInfo data looking to be removed.
	 */
	private boolean removeJobFromNodeJobsList(Job data) {
		Job job = data;
		JobInfo jobInfo = jobsList.lookFor(job);
		if (jobInfo != null) {
			jobsList.remove(jobInfo);
			return true;
		}
		return false;
	}

		/**
	 * Helper function to remove job from the NodeJobsList
	 * @param data the jobInfo data looking to be removed.
	 */
	public boolean removeJobFromNodeJobsListOnly(Job data, int event) {
		Job job = data;
		JobInfo jobInfo = jobsList.lookFor(job);
		if (jobInfo != null && event == NetEvent.EVENT_RETRIAL) {
			jobsList.removeOnly(jobInfo);
			return true;
		}
		return false;
	}

	/**
	 * Unschedules a message given a remove token.
	 * @param token the token to remove the message.
	 * @return true if unschedule was successful, false otherwise.
	 */
	boolean removeMessage(RemoveToken token) {
		return simUnschedule(token);
	}

	//TODO: not used
	/**
	 * Sends a message to a section of all the NetNodes of the QueueNetwork.
	 * @param event Event tag.
	 * @param data  Data to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param sourceSection The source section.
	 * @param destinationSection The destination section.
	 * @param nodeType Type of the node (reference or not: see constants in QueueNetwork).
	 * @throws NetException
	 */
	void sendBroadcast(int event, Object data, double delay, byte sourceSection, byte destinationSection, int nodeType)
			throws NetException {
		int tag;
		ListIterator<NetNode> iterator;
		if (event == NetEvent.EVENT_JOB || event == NetEvent.EVENT_ACK) {
			throw new NetException(this, EXCEPTION_UNABLE_TO_BROADCAST, "message could not be broadcasted.");
		}
		tag = event & NetEvent.EVENT_MASK;
		tag += sourceSection << NodeSection.SOURCE_SHIFT;
		tag += destinationSection << NodeSection.DESTINATION_SHIFT;

		switch (nodeType) {
		case QueueNetwork.REFERENCE_NODE:
			iterator = network.getReferenceNodes().listIterator();
			while (iterator.hasNext()) {
				simSchedule(iterator.next().getId(), delay, tag, data);
			}
			break;
		case QueueNetwork.NODE:
			iterator = network.getNodes().listIterator();
			while (iterator.hasNext()) {
				simSchedule(iterator.next().getId(), delay, tag, data);
			}
			break;
		}
	}

	void setNetwork(QueueNetwork network) {
		// Sets in this method queue network dependent properties
		this.network = network;
		// When a node is linked to a network, connections between nodes must
		// have been already done!!!
		jobsList = new LinkedJobInfoList(getJobClasses().size());
        jobsList.setNetSystem(network.getNetSystem()); 
	}

	@Override
	public void start() {
		simSchedule(getId(), Double.MAX_VALUE, NetEvent.EVENT_KEEP_AWAKE, null);
		simGetNext(SimSystem.SIM_ANY);
	}

	/**
	 * Restarts the entity after an hold period: implements the method restart() of SimEntity.
	 */
	@Override
	public void restart() {
		//TODO: it correct that this method does nothing?
	}

	@Override
	public void poison() {
		state = SimEntity.FINISHED;
		try {
			handleStopMessages();
		} catch (NetException ex) {
			logger.error("Unexpected error: " + ex.getMessage());
			logger.error(ex);
		}
	}

	/**
	 * Redirects a job to a NetNode without updating measures.
	 * @param job job to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param sourceSection The source section.
	 * @param destinationSection The destination section.
	 * @param destination The destination node.
	 * @return a token to remove sent event.
	 * @throws NetException
	 */
	RemoveToken redirect(Job job, double delay, byte sourceSection, byte destinationSection, NetNode destination)
			throws NetException {
		NodeSection section = getSection(sourceSection);
		if (section.automaticUpdateNodeJobinfolist()) {
			JobInfo JobInfo = jobsList.lookFor(job);
			if (JobInfo != null) {
				jobsList.redirectJob(JobInfo);
			}
		}

		int tag = NetEvent.EVENT_JOB & NetEvent.EVENT_MASK;
		tag += sourceSection << NodeSection.SOURCE_SHIFT;
		tag += destinationSection << NodeSection.DESTINATION_SHIFT;
		return simSchedule(destination.getId(), delay, tag, job);
	}

	/**
	 * Sends an "ack" message to a NetNode to inform it that
	 * the job previously sent has been dropped.
	 * @param job job to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param sourceSection The source section.
	 * @param destinationSection The destination section.
	 * @param destination The destination node.
	 * @return a token to remove sent event.
	 * @throws NetException
	 */
	RemoveToken sendAckAfterDrop(Job job, double delay, byte sourceSection, byte destinationSection, NetNode destination)
			throws NetException {
		NodeSection section = getSection(sourceSection);
		if (section.automaticUpdateNodeJobinfolist()) {
			JobInfo JobInfo = jobsList.lookFor(job);
			if (JobInfo != null) {
				jobsList.dropJob(JobInfo);
			}
		}

		int tag = NetEvent.EVENT_ACK & NetEvent.EVENT_MASK;
		tag += sourceSection << NodeSection.SOURCE_SHIFT;
		tag += destinationSection << NodeSection.DESTINATION_SHIFT;
		return simSchedule(destination.getId(), delay, tag, job);
	}

	/**
	 * Returns true if this NetNode is a sink. This is extremely useful as sinks does not have
	 * service section and output section, causing a lot of null pointer exception in routing
	 * strategies if not treated correctly.
	 * Author: Bertoli Marco
	 * @return true if this node does not have service section and output section.
	 */
	public boolean isSink() {
		// WARNING: to avoid circular dependency and to speed up this method,
		// input section instanceof JobSink is not checked
		return (serviceSection == null && outputSection == null);
	}

	/**
	 * @return true if this is a blocking region input station.
	 */
	public boolean isBlockingRegionInputStation() {
		return region != null;
	}

	/**
	 * @param true if this is a blocking region input station.
	 */
	public void setBlockingRegionInputStation(BlockingRegion region) {
		this.region = region;
	}

	/**
	 * @return blocking region, only if this is a region input station.
	 */
	public BlockingRegion getBlockingRegionInputStation() {
		return region;
	}

	/**
	 * @return the simulation parameters.
	 */
	public SimParameters getSimParameters() {
		return simParameters;
	}

	/**
	 * @param simParameters reads the simulation parameters.
	 */
	public void setSimParameters(SimParameters simParameters) {
		this.simParameters = simParameters;
	}

	/**
	 * This method is called after all the sections are added.
	 * @throws NetException
	 */
	public void initializeSections() throws NetException {
		if (inputSection != null) {
			inputSection.setOwnerNode(this);
		}
		if (serviceSection != null) {
			serviceSection.setOwnerNode(this);
		}
		if (outputSection != null) {
			outputSection.setOwnerNode(this);
		}
	}

	public NetSystem getNetSystem(){
		return network.getNetSystem();
	}
}
