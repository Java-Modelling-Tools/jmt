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

package jmt.engine.NodeSections;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueueGetStrategy;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.QueueGetStrategies.FCFSstrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy;
import jmt.engine.QueueNet.BlockingRegion;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.LinkedJobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.QueueNet.SimConstants;
import jmt.engine.QueueNet.WaitingRequest;
import jmt.engine.dataAnalysis.Measure;

/**
 * This class implements a generic finite/infinite queue. In finite queue, if
 * the queue is full, new jobs could be dropped or not. It could implement
 * different job strategy and/or waiting requests strategy.
 *
 * <br><br>
 * It can also define the queue of a station which is inside a blocking region.
 * When a job arrives at this node section, the source node of the message is found out.
 * If the source node is inside the same region, there are no problems and the message
 * is processed as usual.
 * Otherwise, if the source node is outside the blocking region, this message is not
 * processed but redirected to the fictitious station (called "region input station")
 * which controls the access to the blocking region.
 * <br><br>
 *
 * The class has different constructors to create a generic queue or a redirecting queue.
 * <br>
 * However it is also possible to create a generic queue and then to turn on/off the
 * "redirecting queue" behaviour using the <tt>redirectionTurnON(..)</tt> and
 * <tt>redirectionTurnOFF()</tt> methods.
 *
 * @author Francesco Radaelli, Stefano Omini, Bertoli Marco
 * 
 * Modified by Ashanka (Oct 2009) for FCR Bug fix: Events are created with job instead of null for EVENT_JOB_OUT_OF_REGION
 */
/**
 * <p><b>Name:</b> Queue</p> 
 * <p><b>Description:</b> 
 * 
 * </p>
 * <p><b>Date:</b> 15/nov/2009
 * <b>Time:</b> 23.08.16</p>
 * @author Bertoli Marco [marco.bertoli@neptuny.com]
 * @version 3.0
 */
public class Queue extends InputSection {

	public static final String FINITE_DROP = "drop";
	public static final String FINITE_BLOCK = "BAS blocking";
	public static final String FINITE_WAITING = "waiting queue";

	private int size;
	private int maxRunning;

	//coolStart is true if there are no waiting jobs when the queue is started
	private boolean coolStart;
	private boolean infinite;
	private boolean serviceCapacityInfinite;

	private boolean preemption;

	private boolean[] drop;
	private boolean[] block;

	private JobClassList jobClasses;

	//the JobInfoList of the owner NetNode
	private JobInfoList nodeJobsList;
	//the JobInfoList of the global Network
	private GlobalJobInfoList netJobsList;
	//the JobInfoList used for Fork nodes
	private JobInfoList FJList;

	private List<WaitingRequest> waitingRequests;

	private QueueGetStrategy getStrategy;

	private QueuePutStrategy[] putStrategies;

	//--------------------BLOCKING REGION PROPERTIES--------------------//
	//true if the redirection behaviour is turned on
	private boolean redirectionON;
	//the blocking region that the owner node of this section belongs to
	private BlockingRegion myRegion;
	//the input station of the blocking region
	private NetNode regionInputStation;
	//------------------------------------------------------------------//

	/**
	 * Creates a new instance of finite Queue.
	 * @param size Queue size (-1 = infinite queue).
	 * @param drop True if the queue should rejects new jobs when it is full,
	 * false otherwise.
	 * @param getStrategy Queue get strategy: if null FCFS strategy is used.
	 * @param putStrategies Queue put strategy per class: if null Tail strategy is used.
	 */
	public Queue(int size, int maxRunning, boolean serverPreemptive, boolean drop, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		//auto = false, otherwise when a JOB message is received,
		//the corresponding Job object is automatically added to
		//JobInfoList
		super(false);

		if (size == -1) {
			this.size = -1;
			infinite = true;
		} else {
			this.size = size;
			infinite = false;
		}
		if (maxRunning == -1) {
			this.maxRunning = -1;
			serviceCapacityInfinite = true;
		} else {
			this.maxRunning = maxRunning;
			serviceCapacityInfinite = false;
		}
		this.preemption = serverPreemptive;
		if (getStrategy == null) {
			this.getStrategy = new FCFSstrategy();
		} else {
			this.getStrategy = getStrategy;
		}
		this.putStrategies = putStrategies;
		// Uses putstrategies.length to estimate number of classes. It is a bit
		// unclean but we are forced for compatibility.
		this.drop = new boolean[putStrategies.length];
		this.block = new boolean[putStrategies.length];
		Arrays.fill(this.drop, drop);
		Arrays.fill(this.block, false);
		coolStart = true;

		//this node does not belong to any blocking region
		redirectionON = false;
		myRegion = null;
		regionInputStation = null;
	}

	/**
	 * Creates a new instance of finite Queue.
	 * @param size Queue size (-1 = infinite queue).
	 * @param drop True if the queue should rejects new jobs when it is full,
	 * false otherwise.
	 * @param getStrategy Queue get strategy: if null FCFS strategy is used.
	 * @param putStrategies Queue put strategy per class: if null Tail strategy is used.
	 */
	public Queue(Integer size, Integer maxRunning, Boolean serverPreemptive, Boolean drop, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		this(size.intValue(), maxRunning.intValue(), serverPreemptive.booleanValue(), drop.booleanValue(), getStrategy, putStrategies);
	}

	/**
	 * Creates a new instance of finite redirecting Queue.
	 * @param size Queue size (-1 = infinite queue).
	 * @param drop True if the queue should rejects new jobs when it is full,
	 * false otherwise.
	 * @param getStrategy Queue get strategy: if null FCFS strategy is used.
	 * @param putStrategies Queue put strategy per class: if null Tail strategy is used.
	 * @param myReg the blocking region to which the owner node of this queue
	 * belongs.
	 */
	public Queue(int size, int maxRunning, boolean serverPreemptive, boolean drop, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies, BlockingRegion myReg) {
		//uses constructor for generic queue
		this(size, maxRunning, serverPreemptive, drop, getStrategy, putStrategies);

		//sets blocking region properties
		redirectionON = true;
		myRegion = myReg;
		regionInputStation = myRegion.getInputStation();
	}

	/**
	 * Creates a new instance of finite redirecting Queue.
	 * @param size Queue size (-1 = infinite queue).
	 * @param drop True if the queue should rejects new jobs when it is full,
	 * false otherwise.
	 * @param getStrategy Queue get strategy: if null FCFS strategy is used.
	 * @param putStrategies Queue put strategy per class: if null Tail strategy is used.
	 * @param myReg the blocking region to which the owner node of this queue
	 * belongs.
	 */
	public Queue(Integer size, Integer maxRunning, Boolean serverPreemptive, Boolean drop, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies, BlockingRegion myReg) {
		this(size.intValue(), maxRunning.intValue(), serverPreemptive.booleanValue(), drop.booleanValue(), getStrategy, putStrategies, myReg);
	}

	/**
	 * Creates a new instance of finite Queue. This is the newest constructor that supports
	 * different drop strategies. Other constructors are left for compatibility.
	 * @param size Queue size (-1 = infinite queue).
	 * @param dropStrategies Drop strategy per class: FINITE_DROP || FINITE_BLOCK
	 * || FINITE_WAITING.
	 * @param getStrategy Queue get strategy: if null FCFS strategy is used.
	 * @param putStrategies Queue put strategy per class: if null Tail strategy is used.
	 */
	public Queue(Integer size, Integer maxRunning, Boolean serverPreemptive, String[] dropStrategies, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		this(size.intValue(), maxRunning.intValue(), serverPreemptive.booleanValue(), false, getStrategy, putStrategies);
		// Decodes drop strategies
		for (int i = 0; i < dropStrategies.length; i++) {
			if (dropStrategies[i].equals(FINITE_DROP)) {
				drop[i] = true;
				block[i] = false;
			} else if (dropStrategies[i].equals(FINITE_BLOCK)) {
				drop[i] = false;
				block[i] = true;
			} else if (dropStrategies[i].equals(FINITE_WAITING)) {
				drop[i] = false;
				block[i] = false;
			}
		}
	}
	
	public Queue(Integer size, String[] dropStrategies, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		this(size.intValue(), -1, false, dropStrategies, getStrategy, putStrategies);
	}

	/**
	 * Turns on the "redirecting queue" behaviour.
	 * @param region the blocking region to which the owner node of this queue
	 * belongs.
	 */
	public void redirectionTurnON(BlockingRegion region) {
		//sets blocking region properties
		redirectionON = true;
		myRegion = region;
		regionInputStation = myRegion.getInputStation();
	}

	/**
	 * Turns off the "redirecting queue" behaviour.
	 */
	public void redirectionTurnOFF() {
		//sets blocking region properties
		redirectionON = false;
		myRegion = null;
		regionInputStation = null;
	}

	/**
	 * Tells whether the "redirecting queue" behaviour has been turned on.
	 * @return true, if the "redirecting queue" behaviour is on; false otherwise.
	 */
	public boolean isRedirectionON() {
		return redirectionON;
	}

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		// Sets netnode dependent properties
		jobClasses = getJobClasses();
		waitingRequests = new LinkedList<WaitingRequest>();
        //waitingRequests.setNetSystem(node.getNetSystem());
		if (putStrategies == null) {
			putStrategies = new QueuePutStrategy[jobClasses.size()];
			for (int i = 0; i < putStrategies.length; i++) {
				putStrategies[i] = new TailStrategy();
			}
		}
		//retrieves the job info list of the owner node
		nodeJobsList = node.getJobInfoList();
		if (node.getSection(NodeSection.OUTPUT) instanceof Fork) {
			FJList = new LinkedJobInfoList(jobClasses.size());
            FJList.setNetSystem(node.getNetSystem()); 
		}
	}

	/**
	 * Preloads the specified numbers of jobs for each class.
	 * @param jobsPerClass the specified numbers of jobs for each class.
	 * @throws NetException
	 */
	public void preloadJobs(int[] jobsPerClass) throws NetException {
		netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
		int totalJobs = 0;
		for (int i = 0; i < jobsPerClass.length; i++) {
			totalJobs += jobsPerClass[i];
		}
		Job[] jobPermutation = new Job[totalJobs];
		int index = 0;
		for (int i = 0; i < jobsPerClass.length; i++) {
			for (int j = 0; j < jobsPerClass[i]; j++) {
				Job job = new Job(jobClasses.get(i), netJobsList);
                job.initialize(this.getOwnerNode().getNetSystem());
				updateVisitPath(job);
				jobPermutation[index] = job;
				index++;
			}
		}

		/* Commented to ensure a deterministic initialization
		// Durstenfeld Shuffle
		RandomEngine randomEngine = RandomEngine.makeDefault();
		for (int i = jobPermutation.length - 1; i > 0; i--) {
			int j = (int) Math.floor(randomEngine.raw() * (i + 1));
			Job job = jobPermutation[i];
			jobPermutation[i] = jobPermutation[j];
			jobPermutation[j] = job;
		}*/

		for (int i = 0; i < jobPermutation.length; i++) {
			Job job = jobPermutation[i];
			JobInfo jobInfo = new JobInfo(job);
			putStrategies[job.getJobClass().getId()].put(job, jobsList, this);
			if (getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork) {
				FJList.add(jobInfo);
			}
			nodeJobsList.add(jobInfo);
			netJobsList.addJob(job);
			if (redirectionON) {
				myRegion.increaseOccupation(job.getJobClass());
			}
		}
	}

	/**
	 * This method implements a generic finite/infinite queue.
	 * @param message message to be processed.
	 * @throws NetException
	 */
	@Override
	protected int process(NetMessage message) throws NetException {
		Job job;

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:

			//EVENT_START
			//If there are jobs in queue, the first (chosen using the specified
			//get strategy) is forwarded and coolStart becomes false.

			netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
			if (jobsList.size() > 0) {
				//the first job is forwarded to service section
				sendForward(getStrategy.get(jobsList), 0.0);
				coolStart = false;
			}
			break;

		case NetEvent.EVENT_JOB:

			//EVENT_JOB
			//If the queue is a redirecting queue, jobs arriving from the outside of
			//the blocking region must be redirected to the region input station
			//
			//Otherwise the job is processed as usual.
			//
			//If coolStart is true, the queue is empty, so the job is added to the job list
			//and immediately forwarded to the next section. An ack is sent and coolStart is
			//set to false.
			//
			//If the queue is not empty, it should be distinguished between finite/infinite queue.
			//
			//If the queue is finite, checks the size: if it is not full the job is put into the
			//queue and an ack is sent. Else, if it is full, checks the owner node: if the
			//source node is the owner node of this section, an ack is sent and a waiting
			//request is created. If the source is another node the waiting request is created
			//only if drop is false, otherwise an ack is sent but the job is rejected.
			//
			//If the queue is infinite, the job is put into the queue and an ack is sent

			job = message.getJob();

			//----REDIRECTION BEHAVIOUR----------//
			if (redirectionON) {
				NetNode source = message.getSource();
				if (!myRegion.belongsToRegion(source)) {
					//this message has arrived from the outside of the blocking region
					if ((source != regionInputStation)) {
						//the external source is not the input station
						//the message must be redirected to the input station,
						//without processing it

						//redirects the message to the inputStation
						redirect(job, 0.0, NodeSection.INPUT, regionInputStation);
						//send an ack to the source
						send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
						break;
					}
				}
			}
			//----END REDIRECTION BEHAVIOUR-------//

			//
			//two possible cases:
			//1 - the queue is a generic queue (!redirectionOn)
			//2 - the queue is a redirecting queue, but the message has arrived
			//from the inside of the region or from the inputStation:
			//in this case the redirecting queue acts as a normal queue
			//
			//therefore in both cases the behaviour is the same
			//

			// Check if there is still capacity.
			// <= size because the arriving job has not been inserted in Queue
			// job list but has been inserted in NetNode job list !!
			if (infinite || nodeJobsList.size() <= size) {
				// Queue is not full. Okay.

				// If parent node is a fork node adds job to FJ info list
				if (getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork) {
					FJList.add(new JobInfo(job));
				}

				// If coolStart is true, this is the first job received or the
				// queue was empty: this job is sent immediately to the next
				// section and coolStart set to false.
				if (preemption) {
						sendForward(job, 0.0);
				} else {
					int jobsInService = getOwnerNode().getSection(NodeSection.SERVICE).getIntSectionProperty(NodeSection.PROPERTY_ID_RESIDENT_JOBS);
					if (coolStart && (jobsInService < maxRunning || serviceCapacityInfinite)) {
						// No jobs in queue: Refresh jobsList and sends job (do not use put strategy, because queue is empty)
						if (jobsList.size() <= 0) {
							jobsList.add(new JobInfo(job));
							// forward without any delay
							sendForward(jobsList.removeFirst().getJob(), 0.0);
						} else {
							putStrategies[job.getJobClass().getId()].put(job, jobsList, this);
							Job jobSent = getStrategy.get(jobsList);
							sendForward(jobSent, 0.0);
						}
						coolStart = false;
					} else {
						putStrategies[job.getJobClass().getId()].put(job, jobsList, this);
					}
				}
				// sends an ACK backward
				send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
			} else {
				// Queue is full. Now we use an additional queue or drop.

				// if the job has been sent by the owner node of this queue section
				if (isMyOwnerNode(message.getSource())) {
					send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());

					waitingRequests.add(new WaitingRequest(job, message.getSourceSection(), message.getSource()));
				}
				// otherwise if job has been sent by another node
				else if (!drop[job.getJobClass().getId()]) {
					// if drop is true reject the job, else add the job to waitingRequests
					waitingRequests.add(new WaitingRequest(job, message.getSourceSection(), message.getSource()));
					// if blocking is disabled, sends ack otherwise router of the previous node remains busy
					if (!block[job.getJobClass().getId()]) {
						send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
					}
				} else {
					//after arriving to this section, the job has been inserted in the job
					//lists of both node section and node.
					//If drop = true, the job must be removed if the queue is full.
					//Using the "general" send method, however, the dropped job was not removed
					//from the job info list of node section and of node, then it was
					//sent later, after receiving one or more ack.

					if (!(job instanceof ForkJob)) {
						//drops job from global jobInfoList
						netJobsList.dropJob(job);
					}
					//drops job from node jobInfoList and then sends ack back
					sendAckAfterDrop(job, 0.0, message.getSourceSection(), message.getSource());

					//if the queue is inside a blocking region, the jobs
					//counter must be decreased
					if (redirectionON) {
						//decrease the number of jobs
						myRegion.decreaseOccupation(job.getJobClass());
						//sends an event to the input station (which may be blocked)
						send(NetEvent.EVENT_JOB_OUT_OF_REGION, job, 0.0, NodeSection.INPUT, regionInputStation);
						//Since now for blocking regions the job dropping is handles manually at node 
						//level hence need to create events with Jobs ..Modified for FCR Bug Fix
					}
				}
			}
			break;

		case NetEvent.EVENT_ACK:

			//EVENT_ACK
			//If there are waiting requests, the first is taken (if the source node of this request
			//is the owner node of this section, an ack message is sent).
			//The job contained is put into the queue using the specified put strategy.
			//
			//At this point, if there are jobs in queue, the first is taken (using the
			//specified get strategy) and forwarded. Otherwise, if there are no jobs, coolStart
			//is set true.

			// if there is a waiting request send ack to the first node
			//(note that with infinite queue there are no waiting requests)
			if (waitingRequests.size() > 0) {
				WaitingRequest wr = waitingRequests.remove(0);

				// If the source is not the owner node sends ack if blocking is enabled. Otherwise 
				// ack was already sent.
				if (!isMyOwnerNode(wr.getSource()) && block[wr.getJob().getJobClass().getId()]) {
					send(NetEvent.EVENT_ACK, wr.getJob(), 0.0, wr.getSourceSection(), wr.getSource());
				}

				//the class ID of this job
				int c = wr.getJob().getJobClass().getId();
				//the job is put into the queue according to its own class put strategy
				putStrategies[c].put(wr.getJob(), jobsList, this);
			}

			// if there is at least one job, sends it
			int jobsInService = getOwnerNode().getSection(NodeSection.SERVICE).getIntSectionProperty(NodeSection.PROPERTY_ID_RESIDENT_JOBS);
			if (jobsList.size() > 0  && (jobsInService < maxRunning || serviceCapacityInfinite)) {
				// Gets job using a specific strategy and sends job
				Job jobSent = getStrategy.get(jobsList);
				sendForward(jobSent, 0.0);
			} else {
			// else set coolStart to true
				coolStart = true;
			}
			break;

		case NetEvent.EVENT_JOB_COMPLETED:
			jobsInService = getOwnerNode().getSection(NodeSection.SERVICE).getIntSectionProperty(NodeSection.PROPERTY_ID_RESIDENT_JOBS);
			if (jobsList.size() > 0 && (jobsInService < maxRunning || serviceCapacityInfinite)) {
				// Gets job using a specific strategy and sends job
				Job jobSent = getStrategy.get(jobsList);
				sendForward(jobSent, 0.0);
			} else {
				// else set coolStart to true
				coolStart = true;
			}
			break;
		case NetEvent.EVENT_PREEMPTED_JOB:
			job = message.getJob();
			putStrategies[job.getJobClass().getId()].put(job, jobsList, this);
			break;

		case NetEvent.EVENT_JOIN:
			if (!(getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork)) {
				return MSG_NOT_PROCESSED;
			}
			job = (Job) message.getData();
			JobInfo jobInfo = FJList.lookFor(job);
			if (jobInfo != null) {
				FJList.remove(jobInfo);
			}
			break;

		case NetEvent.EVENT_JOB_RELEASE:
			break;

		case NetEvent.EVENT_JOB_FINISH:
			break;

		case NetEvent.EVENT_STOP:
			break;


		default:
			return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	@Override
	public void analyzeFJ(int name, JobClass jobClass, Measure measurement) throws NetException {
		switch (name) {
		case SimConstants.FORK_JOIN_NUMBER_OF_JOBS:
			FJList.analyzeQueueLength(jobClass, measurement);
			break;
		case SimConstants.FORK_JOIN_RESPONSE_TIME:
			FJList.analyzeResponseTime(jobClass, measurement);
			break;
		default:
			throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
		}
	}

}
