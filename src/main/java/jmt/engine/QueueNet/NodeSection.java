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

import org.apache.commons.math3.util.Pair;

import jmt.common.exception.NetException;
import jmt.engine.dataAnalysis.InverseMeasure;
import jmt.engine.dataAnalysis.Measure;
import jmt.engine.log.JSimLogger;
import jmt.engine.random.Parameter;
import jmt.engine.simEngine.RemoveToken;

/**
 * This class implements a generic section of a NetNode.
 * @author Francesco Radaelli, Stefano Omini
 */
public abstract class NodeSection {

	/*------------------------------ADDRESSES---------------------------------*/

	/** Section without address.*/
	public static final byte NO_ADDRESS = 0x00;
	/** Input section address */
	public static final byte INPUT = 0x01;
	/** Service section address. */
	public static final byte SERVICE = 0x02;
	/** Output section address.*/
	public static final byte OUTPUT = 0x03;

	/*-----------------------NODE SECTION RETURNED VALUES---------------------*/

	/** Message passed has not been processed (general event). */
	public static final int MSG_NOT_PROCESSED = 0x0000;

	/** Message passed has been processed (all events). */
	public static final int MSG_PROCESSED = 0x0001;

	/*------------------------------BIT MASK----------------------------------*/

	static final int SOURCE_MASK = 0xFF000000;

	static final int DESTINATION_MASK = 0x00FF0000;

	static final int SOURCE_SHIFT = 24;

	static final int DESTINATION_SHIFT = 16;

	/*------------------------------------------------------------------------*/

	/** Exception ID: Required measure does not exist. */
	public static final int EXCEPTION_MEASURE_DOES_NOT_EXIST = 0x0001;
	/** Exception ID: Required property is not available. */
	public static final int EXCEPTION_PROPERTY_NOT_AVAILABLE = 0x0002;

	//TODO: warning, not all properties make sense for all section types

	/** Property ID: number of jobs arriving at this section */
	public static final int PROPERTY_ID_ARRIVING_JOBS = 0x0001;
	/** Property ID: number of jobs departing from this section */
	public static final int PROPERTY_ID_DEPARTING_JOBS = 0x0002;
	/** Property ID: number of jobs inside this section */
	public static final int PROPERTY_ID_RESIDENT_JOBS = 0x0003;
	/** Property ID: residence time */
	public static final int PROPERTY_ID_RESIDENCE_TIME = 0x0004;
	/** Property ID: utilization */
	public static final int PROPERTY_ID_UTILIZATION = 0x0005;
	/** Property ID: average utilization */
	public static final int PROPERTY_ID_AVERAGE_UTILIZATION = 0x0006;

	//TODO: make more efficient the mechanism to determine the ownerNode (to avoid too many getNode)
	/** Owner of this NodeSection. */
	private NetNode ownerNode;

	/** Identifier of this NodeSection. */
	private byte sectionID;

	protected JobInfoList jobsList;

	protected JSimLogger logger = JSimLogger.getLogger(this.getClass());

	private boolean auto; //auto refresh of the jobsList attribute.
	private boolean nodeAuto; // auto refresh the joblist attribute at node level.

	/**
	 * Creates a new instance of this NodeSection.
	 * Note that, while building a new node section, node owner informations are
	 * not available. To set node section properties depending on the owner node
	 * ones, the "nodeLiked(...)" protected method should be used.
	 * @param id NodeSection identifier.
	 */
	public NodeSection(byte id) {
		this.sectionID = id;
		this.auto = true;
		this.nodeAuto = true;
	}

	/**
	 * Creates a new instance of this NodeSection.
	 * Note that, while building a new node section, node owner informations are
	 * not available. To set node section properties depending on the owner node
	 * ones, the "nodeLiked(...)" protected method should be used.
	 * @param id NodeSection identifier.
	 * @param auto Auto refresh of the jobsList attribute.
	 */
	public NodeSection(byte id, boolean auto) {
		this.sectionID = id;
		this.auto = auto;
		this.nodeAuto = true;
	}

	/**
	 * Creates a new instance of this NodeSection.
	 * Note that, while building a new node section, node owner informations are
	 * not available. To set node section properties depending on the owner node
	 * ones, the "nodeLiked(...)" protected method should be used.
	 * @param id NodeSection identifier.
	 * @param auto Auto refresh of the jobsList attribute.
	 * @param nodeAuto Auto refresh the jobsList attribute at node level
	 */
	public NodeSection(byte id, boolean auto, boolean nodeAuto) {
		this.sectionID = id;
		this.auto = auto;
		this.nodeAuto = nodeAuto;
	}

	/**
	 * This method should be overridden to implement a specific behaviour of the
	 * node section when the node section itself is linked to the owner node.
	 * This method should be used to set node section properties depending on
	 * the owner node ones.
	 * @param node Owner node.
	 * @throws NetException
	 */
	protected void nodeLinked(NetNode node) throws NetException {
	}

	/**
	 * Gets the id of the node section.
	 * @return node section identifier.
	 */
	public byte getSectionID() {
		return sectionID;
	}

	//TODO: alternatively one can override particular components
	/**
	 * Analyzes a measure in the node section. Override this method to analyze a
	 * measure depending on the node section implementation. Note that the first
	 * 256 identifiers are reserved by NodeSection class.
	 * @param name name of the measure to be activated (see measures constants).
	 * @param jobClass Job class to be analyzed.
	 * @param measurement Set of measure to be activated.
	 * @throws NetException
	 */
	public void analyze(int name, JobClass jobClass, Measure measurement) throws NetException {
		switch (name) {
		case SimConstants.LIST_NUMBER_OF_JOBS:
			jobsList.analyzeUtilization(jobClass, measurement);
			break;
		case SimConstants.LIST_RESIDENCE_TIME:
			jobsList.analyzeResidenceTime(jobClass, measurement);
			break;
		case SimConstants.LIST_THROUGHPUT:
			jobsList.analyzeThroughput(jobClass, (InverseMeasure) measurement);
			break;
		case SimConstants.LIST_DROP_RATE:
			jobsList.analyzeDropRate(jobClass, (InverseMeasure) measurement);
			break;
		default:
			throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
		}
	}

	/**
	 * Checks if the specified node is the owner node of this section. Note that
	 * this method could be called <b>only after</b> that the owner node has
	 * been linked to this section.
	 * @param node node to be checked.
	 * @return True if the node is the owner node, false otherwise.
	 */
	public boolean isMyOwnerNode(NetNode node) {
		return node == this.ownerNode;
	}

	/**
	 * Gets owner node of this section.
	 * @return Owner node.
	 */
	public NetNode getOwnerNode() {
		return ownerNode;
	}

	/**
	 * Gets the list of the job classes of the owner queue network.
	 * @return Queue network job classes.
	 */
	public JobClassList getJobClasses() {
		return ownerNode.getJobClasses();
	}

	/**
	 * Gets an integer type property of this node. Note that the first 256
	 * identifiers are reserved by NodeSection class.
	 * @param id Property identifier (see properties constants).
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public int getIntSectionProperty(int id) throws NetException {
		switch (id) {
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
	 * Gets an integer type property of this node depending on a specified job
	 * class. Note that the first 256 identifiers are reserved by NodeSection
	 * class.
	 * @param id Property identifier (see properties constants).
	 * @param jobClass jobClass.
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public int getIntSectionProperty(int id, JobClass jobClass) throws NetException {
		switch (id) {
		case PROPERTY_ID_ARRIVING_JOBS:
			return jobsList.getJobsInPerClass(jobClass);
		case PROPERTY_ID_DEPARTING_JOBS:
			return jobsList.getJobsOutPerClass(jobClass);
		case PROPERTY_ID_RESIDENT_JOBS:
			return jobsList.size(jobClass);
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a double type property of this node. Note that the first 256
	 * identifiers are reserved by NodeSection class.
	 * @param id Property identifier (see properties constants).
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public double getDoubleSectionProperty(int id) throws NetException {
		switch (id) {
		case PROPERTY_ID_RESIDENCE_TIME:
			return jobsList.getLastJobSojournTime();
		case PROPERTY_ID_UTILIZATION:
			return jobsList.size();
		case PROPERTY_ID_AVERAGE_UTILIZATION:
			return (getNetSytem().getTime() <= 0.0) ? 0.0
					: jobsList.getTotalSojournTime() / ownerNode.getNetSystem().getTime();
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a double type property of this node depending on a specified job
	 * class. Note that the first 256 identifiers are reserved by NodeSection
	 * class.
	 * @param id Property identifier (see properties constants).
	 * @param jobClass jobClass.
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public double getDoubleSectionProperty(int id, JobClass jobClass) throws NetException {
		switch (id) {
		case PROPERTY_ID_RESIDENCE_TIME:
			return jobsList.getLastJobSojournTimePerClass(jobClass);
		case PROPERTY_ID_UTILIZATION:
			return jobsList.size(jobClass);
		case PROPERTY_ID_AVERAGE_UTILIZATION:
			return (getNetSytem().getTime() <= 0.0) ? 0.0
					: jobsList.getTotalSojournTimePerClass(jobClass) / ownerNode.getNetSystem().getTime();
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a parameter type property of this node. Note that the first 256
	 * identifiers are reserved by NodeSection class.
	 * @param id Property identifier.
	 * @param jobClass jobClass.
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public Parameter getParameter(int id) throws NetException {
		switch (id) {
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets a parameter type property of this node depending on a specified job
	 * class. Note that the first 256 identifiers are reserved by NodeSection
	 * class.
	 * @param id Property identifier.
	 * @param jobClass jobClass.
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public Parameter getParameter(int id, JobClass jobClass) throws NetException {
		switch (id) {
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets an object type property of this node. Note that the first 256
	 * identifiers are reserved by NodeSection class.
	 * @param id Property identifier.
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public Object getObject(int id) throws NetException {
		switch (id) {
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Gets an object type property of this node depending on a specified job
	 * class. Note that the first 256 identifiers are reserved by NodeSection
	 * class.
	 * @param id Property identifier.
	 * @param jobClass the jobClass which property refers to.
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public Object getObject(int id, JobClass jobClass) throws NetException {
		switch (id) {
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Checks if a property of this node is enabled. Note that the first 256
	 * identifiers are reserved by NodeSection class.
	 * @param id Property identifier.
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public boolean isEnabled(int id) throws NetException {
		switch (id) {
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * Checks if a property of this node is enabled depending on a specified job
	 * class. Note that the first 256 identifiers are reserved by NodeSection
	 * class.
	 * @param id Property identifier.
	 * @param jobClass jobClass.
	 * @return Property value.
	 * @throws NetException if the requested property is not available.
	 */
	public boolean isEnabled(int id, JobClass jobClass) throws NetException {
		switch (id) {
		default:
			throw new NetException(this, EXCEPTION_PROPERTY_NOT_AVAILABLE, "required property is not available.");
		}
	}

	/**
	 * This method should be overridden to implement a specific NetMessage
	 * processor.
	 * @param message message to be processed.
	 * @return message processing result.
	 */
	protected abstract int process(NetMessage message) throws NetException;

	/**
	 * Sends an event to a section of a node.
	 * @param event event to be sent.
	 * @param data  data to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destinationSection destination section.
	 * @param destination destination node.
	 * @return a token to remove sent event.
	 */
	protected RemoveToken send(int event, Object data, double delay, byte destinationSection, NetNode destination) {
		if (event == NetEvent.EVENT_JOB || event == NetEvent.EVENT_PREEMPTED_JOB) {
			if ((destination != ownerNode) || (destination == ownerNode && destinationSection != sectionID)) {
				//it is a JOB event and the destination is not the owner node or it is the owner
				//node but the dest section is not this section
				if (auto) {
					updateJobsListAfterSend((Job) data);
				}
			}
		}
		if (event == NetEvent.EVENT_RETRIAL ) {
			updateJobsListAfterSend((Job) data, event);
		}
		return ownerNode.send(event, data, delay, sectionID, destinationSection, destination);
	}

	/**
	 * Sends an event to the <b>input</b> section of a node.
	 * @param event event to be sent.
	 * @param data  data to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destination destination node.
	 * @return a token to remove sent event.
	 */
	protected RemoveToken send(int event, Object data, double delay, NetNode destination) {
		return send(event, data, delay, NodeSection.INPUT, destination);
	}

	/**
	 * Sends an event to a section of this node.
	 * @param event event to be sent.
	 * @param data  data to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destinationSection Destination section.
	 * @return a token to remove sent event.
	 */
	protected RemoveToken send(int event, Object data, double delay, byte destinationSection) {
		return send(event, data, delay, destinationSection, ownerNode);
	}

	/**
	 * Sends an event to this section.
	 * @param event event to be sent.
	 * @param data  data to be attached to the message.
	 * @param delay Scheduling delay.
	 * @return a token to remove sent event.
	 */
	protected RemoveToken sendMe(int event, Object data, double delay) {
		return send(event, data, delay, sectionID, ownerNode);
	}

	/**
	 * Sends a job to a section of a node.
	 * @param job job to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destinationSection destination section.
	 * @param destination destination node.
	 * @return a token to remove sent event.
	 */
	protected RemoveToken send(Job job, double delay, byte destinationSection, NetNode destination) {
		return send(NetEvent.EVENT_JOB, job, delay, destinationSection, destination);
	}

	/**
	 * Sends a job to the <b>input</b> section of a node.
	 * @param job job to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destination destination node.
	 * @return a token to remove sent event.
	 */
	protected RemoveToken send(Job job, double delay, NetNode destination) {
		return send(NetEvent.EVENT_JOB, job, delay, NodeSection.INPUT, destination);
	}

	/**
	 * Sends a job to a section of this node.
	 * @param job job to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destinationSection Destination section.
	 * @return a token to remove sent event.
	 */
	protected RemoveToken send(Job job, double delay, byte destinationSection) {
		return send(NetEvent.EVENT_JOB, job, delay, destinationSection, ownerNode);
	}

	/**
	 * Sends a job to this section.
	 * @param job job to be attached to the message.
	 * @param delay Scheduling delay.
	 * @return a token to remove sent event.
	 */
	protected RemoveToken sendMe(Job job, double delay) {
		return send(NetEvent.EVENT_JOB, job, delay, sectionID, ownerNode);
	}

	/**
	 * Removes a message previously sent.
	 * @param token the token to remove the message.
	 * @return true if message was removed, false otherwise.
	 */
	protected boolean removeMessage(RemoveToken token) {
		return ownerNode.removeMessage(token);
	}

	//TODO: not used
	/**
	 * Sends a message to a section of all the NetNodes of the QueueNetwork.
	 * @param event event tag.
	 * @param data  data to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destinationSection The destination section.
	 * @param nodeType Type of the node.
	 * @throws NetException
	 */
	protected void sendBroadcast(int event, Object data, double delay, byte destinationSection, int nodeType)
			throws NetException {
		ownerNode.sendBroadcast(event, data, delay, sectionID, destinationSection, nodeType);
	}

	/**
	 * Checks if a message has been sent by this section. Note that this method
	 * could be called only after that the owner node has been linked to this
	 * section.
	 * @param message message to be checked.
	 * @return True if the message has been sent by this section, false
	 * otherwise.
	 */
	protected boolean isMine(NetMessage message) {
		return message.sentBy(sectionID, ownerNode);
	}

	/**
	 * Updates the JobInfoList after sending this job.
	 * @param job job to be sent.
	 */
	private void updateJobsListAfterSend(Job job) {
		JobInfo jobInfo = jobsList.lookFor(job);
		if (jobInfo != null) {
			jobsList.remove(jobInfo);
		}
	}

	private void updateJobsListAfterSend(Job job, int event) {
		JobInfo jobInfo = jobsList.lookFor(job);
		if (jobInfo != null & event == NetEvent.EVENT_RETRIAL) {
			jobsList.removeOnly(jobInfo);
		}
	}
	/**
	 * Updates the JobInfoList after receiving this job.
	 * @param job job to be received.
	 */
	private void updateJobsListAfterReceive(Job job) {
		JobInfo jobInfo = new JobInfo(job);
		jobsList.add(jobInfo);
	}

	void setOwnerNode(NetNode ownerNode) throws NetException {
		this.ownerNode = ownerNode;
		jobsList = new LinkedJobInfoList(getJobClasses().size());
        jobsList.setNetSystem(ownerNode.getNetSystem()); 
		nodeLinked(ownerNode);
	}

	int receive(NetMessage message) throws NetException {
		if (message.getEvent() == NetEvent.EVENT_JOB) {
			if ((message.getSource() != ownerNode) || (message.getSource() == ownerNode && message.getSourceSection() != sectionID)) {
				if (auto) {
					updateJobsListAfterReceive(message.getJob());
				}
			}
		}
		return process(message);
	}

	/**
	 * Redirects a job to a section of a node without updating measures.
	 * @param job job to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destinationSection destination section.
	 * @param destination destination node.
	 * @return a token to remove sent event.
	 * @throws NetException
	 */
	protected RemoveToken redirect(Job job, double delay, byte destinationSection, NetNode destination)
			throws NetException {
		if (auto) {
			updateJobsListAfterRedirect(job);
		}
		return ownerNode.redirect(job, delay, sectionID, destinationSection, destination);
	}

	/**
	 * Updates the JobInfoList after redirecting this job.
	 * @param job job to be redirected.
	 */
	private void updateJobsListAfterRedirect(Job job) {
		JobInfo jobInfo = jobsList.lookFor(job);
		if (jobInfo != null) {
			jobsList.redirectJob(jobInfo);
		}
	}

	/**
	 * Sends an "ack" event to a section of a node to inform it that the job
	 * previously sent has been dropped.
	 * @param job job to be attached to the message.
	 * @param delay Scheduling delay.
	 * @param destinationSection destination section.
	 * @param destination destination node.
	 * @return a token to remove sent event.
	 * @throws NetException
	 */
	protected RemoveToken sendAckAfterDrop(Job job, double delay, byte destinationSection, NetNode destination)
			throws NetException {
		if (auto) {
			updateJobsListAfterDrop(job);
		}
		return ownerNode.sendAckAfterDrop(job, delay, sectionID, destinationSection, destination);
	}

	/**
	 * Updates the JobInfoList after dropping this job.
	 * @param job job to be dropped.
	 */
	private void updateJobsListAfterDrop(Job job) {
		JobInfo jobInfo = jobsList.lookFor(job);
		if (jobInfo != null) {
			jobsList.dropJob(jobInfo);
		}
	}

	/**
	 * Updates the visit path of this job.
	 * @param job job whose visit path is to be updated.
	 */
	protected void updateVisitPath(Job job) {
		Pair<NetNode, JobClass> pair = null;
		if (job instanceof ForkJob) {
			pair = new Pair<NetNode, JobClass>(ownerNode, ((ForkJob) job).getOriginalJob().getJobClass());
		} else {
			pair = new Pair<NetNode, JobClass>(ownerNode, job.getJobClass());
		}
		job.AddToVisitPath(pair);
	}

	/**
	 * Tells if jobinfolist at node section should be updated automatically.
	 * @return true if it should be handled automatically, false otherwise.
	 */
	boolean automaticUpdateNodeJobinfolist() {
		return nodeAuto;
	}

	public void analyzeJoin(int name, JobClass jobClass, Measure measurement) throws NetException {
		throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
	}

	public void analyzeFCR(int name, Measure measurement) throws NetException {
		throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
	}

	public void analyzeFJ(int name, JobClass jobClass, Measure measurement) throws NetException {
		throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
	}

	public void analyzeTransition(int name, String modeName, Measure measurement) throws NetException {
		throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
	}

	public NetSystem getNetSytem() {
		return ownerNode.getNetSystem();
	}

}
