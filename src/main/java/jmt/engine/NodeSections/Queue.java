/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.engine.NodeSections;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ImpatienceStrategies.Balking;
import jmt.engine.NetStrategies.ImpatienceStrategies.Impatience;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.BooleanValueImpatienceMeasurement;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.DoubleValueImpatienceMeasurement;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.QueueGetStrategies.FCFSstrategy;
import jmt.engine.NetStrategies.QueueGetStrategies.PollingGetStrategy;
import jmt.engine.NetStrategies.QueueGetStrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.PreemptiveStrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.QueueNet.*;
import jmt.engine.dataAnalysis.Measure;

import java.util.*;

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
 * <p>
 * The class has different constructors to create a generic queue or a redirecting queue.
 * <br>
 * However it is also possible to create a generic queue and then to turn on/off the
 * "redirecting queue" behaviour using the <tt>redirectionTurnON(..)</tt> and
 * <tt>redirectionTurnOFF()</tt> methods.
 *
 * @author Francesco Radaelli, Stefano Omini, Bertoli Marco
 * <p>
 * Modified by Ashanka (Oct 2009) for FCR Bug fix: Events are created with job instead of null for EVENT_JOB_OUT_OF_REGION
 */

/**
 * <p><b>Name:</b> Queue</p>
 * <p><b>Description:</b>
 *
 * </p>
 * <p><b>Date:</b> 15/nov/2009
 * <b>Time:</b> 23.08.16</p>
 *
 * @author Bertoli Marco [marco.bertoli@neptuny.com]
 * @version 3.0
 */
public class Queue extends InputSection {

	public static final String FINITE_DROP = "drop";
	public static final String FINITE_BLOCK = "BAS blocking";
	public static final String FINITE_WAITING = "waiting queue";
	public static final String FINITE_RETRIAL = "retrial";

	private String[] dropStrategies;

	private ServiceStrategy[] retrialDistributionStrategies;

	private int size; // queue + servers
	private int maxRunning;

	/* Temp solution for Limited Polling */
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
	//the JobInfoList of the global Network -- model level
	private GlobalJobInfoList netJobsList;
	//the JobInfoList used for Fork nodes
	private JobInfoList FJList;
	// Backup buffer when the main jobsList is full
	private JobInfoList waitingRequests;

	private boolean isRetrialDR = false;

	// TODO rose: Suggest to change to pair
	// Used only if the job is forwarded to a PSServer
	private Map<Job, Double> renegingDelayPerJob = new HashMap<>();


	// Mapping such that each Queue and each JobClass has its own Reneging object
	private Impatience[] impatienceStrategyPerStationClass;
	private boolean linkedToPSServer = false;

	// Get and put strategies for the Queue
	private QueueGetStrategy getStrategy;

	private Boolean switchOverRequest;

	private QueuePutStrategy[] putStrategies;

	//--------------------BLOCKING REGION PROPERTIES--------------------//
	//true if the redirection behaviour is turned on
	private boolean redirectionON;
	//the blocking region that the owner node of this section belongs to
	private BlockingRegion myRegion;
	//the input station of the blocking region
	private NetNode regionInputStation;
	//------------------------------------------------------------------//

	private enum BufferType {
		FJ_LIST,
		JOBS_LIST,
		WAITING_REQUESTS
	}

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
		this.size = size;
		infinite = size == -1;
		this.maxRunning = maxRunning;
		serviceCapacityInfinite = maxRunning == -1;
		this.preemption = serverPreemptive;
		this.getStrategy = getStrategy == null ? new FCFSstrategy() : getStrategy;
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

		switchOverRequest = false;
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
	 * || FINITE_WAITING || FINITE_RETRIAL.
	 * @param getStrategy Queue get strategy: if null FCFS strategy is used.
	 * @param putStrategies Queue put strategy per class: if null Tail strategy is used.
	 */
	public Queue(Integer size, Integer maxRunning, Boolean serverPreemptive, String[] dropStrategies, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		this(size.intValue(), maxRunning.intValue(), serverPreemptive.booleanValue(), false, getStrategy, putStrategies);
		// Decodes drop strategies
		this.dropStrategies = dropStrategies;
		for (int i = 0; i < dropStrategies.length; i++) {
			switch (dropStrategies[i]) {
				case FINITE_DROP:
					drop[i] = true;
					block[i] = false;
					break;
				case FINITE_BLOCK:
					drop[i] = false;
					block[i] = true;
					break;
				case FINITE_RETRIAL:
					isRetrialDR = true;
				case FINITE_WAITING:
				default:
					drop[i] = false;
					block[i] = false;
			}
		}
	}

	/**
	 * Creates a new instance of finite Queue. This is the newest constructor that supports retrial queues.
	 *
	 * @param dropStrategies Drop strategy per class: FINITE_DROP || FINITE_BLOCK || FINITE_WAITING || FINITE_RETRIAL.
	 */
	public Queue(Integer size, String[] dropStrategies, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		this(size.intValue(), -1, false, dropStrategies, getStrategy, putStrategies);
	}

	/**
	 * Creates a new instance of finite Queue. This is the newest constructor that supports retrial queues.
	 *
	 * @param dropStrategies Drop strategy per class: FINITE_DROP || FINITE_BLOCK || FINITE_WAITING || FINITE_RETRIAL.
	 */
	public Queue(Integer size, Integer maxRunning, Boolean serverPreemptive, String[] dropStrategies, ServiceStrategy[] retrials, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		this(size.intValue(), maxRunning.intValue(), serverPreemptive.booleanValue(), dropStrategies, getStrategy, putStrategies);
		// Decodes drop strategies
		retrialDistributionStrategies = retrials;

	}

	public Queue(Integer size, String[] dropStrategies, ServiceStrategy[] retrials, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		this(size.intValue(), -1, false, dropStrategies, retrials, getStrategy, putStrategies);
	}

		/**
	 * Creates a new instance of finite Queue with reneging. Calling constructor that supports
	 * different drop strategies and reneging strategies. Other constructors are left for compatibility.
	 *
	 * @param impatienceStrategies Impatience strategies unique to each station and class.
	 */
	public Queue(Integer size, Integer maxRunning, Boolean serverPreemptive, String[] dropStrategies
					, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies, Impatience[] impatienceStrategies) {
		// Called method with decoded drop Strategies
		this(size, maxRunning, serverPreemptive, dropStrategies, getStrategy, putStrategies);
		// Add impatience strategies for each class
		impatienceStrategyPerStationClass = impatienceStrategies;
	}

	public Queue(Integer size, String[] dropStrategies, QueueGetStrategy getStrategy,
							 QueuePutStrategy[] putStrategies, Impatience[] impatienceStrategies) {
		this(size, -1, false, dropStrategies, getStrategy, putStrategies, impatienceStrategies);
	}
	
	/**
	 * Creates a new instance of finite Queue with reneging. Calling constructor that supports
	 * different drop strategies and reneging strategies. Other constructors are left for compatibility.
	 *
	 * @param impatienceStrategies Impatience strategies unique to each station and class.
	 */
	public Queue(Integer size, Integer maxRunning, Boolean serverPreemptive, String[] dropStrategies, ServiceStrategy[] retrials
					, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies, Impatience[] impatienceStrategies) {
		// Called method with decoded drop Strategies
			this(size, maxRunning, serverPreemptive, dropStrategies, getStrategy, putStrategies, impatienceStrategies);
		// Add impatience strategies for each class
		retrialDistributionStrategies = retrials;
	}

	public Queue(Integer size, String[] dropStrategies, ServiceStrategy[] retrials, QueueGetStrategy getStrategy,
							 QueuePutStrategy[] putStrategies, Impatience[] impatienceStrategies) {
		this(size, -1, false, dropStrategies, retrials, getStrategy, putStrategies, impatienceStrategies);
	}

	/**
	 * Turns on the "redirecting queue" behaviour.
	 *
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
		// for easier reference
		jobClasses = getJobClasses();
		nodeJobsList = node.getJobInfoList();

		waitingRequests = new LinkedJobInfoList(jobClasses.size());
		waitingRequests.setNetSystem(node.getNetSystem());

		if (putStrategies == null) {
			putStrategies = new QueuePutStrategy[jobClasses.size()];
			Arrays.fill(putStrategies, new TailStrategy());
		}

		if (getStrategy instanceof PollingGetStrategy) {
			((PollingGetStrategy) getStrategy).setPollingQueues(jobClasses);
		}
				
		if (node.getSection(NodeSection.OUTPUT) instanceof Fork) {
			FJList = new LinkedJobInfoList(jobClasses.size());
			FJList.setNetSystem(node.getNetSystem());
		}

		// Check if the Queue is connected directly to a PSServer
		if (node.getSection(NodeSection.SERVICE) instanceof PSServer) {
			linkedToPSServer = true;
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
	 *
	 * @param message message to be processed.
	 * @throws NetException
	 */
	@Override
	protected int process(NetMessage message) throws NetException {
		Job job = message.getJob();
		JobInfo jobInfo = null;
		int jobClass = -1;
		if (job != null) {
			jobInfo = nodeJobsList.lookFor(job);
			jobClass = job.getJobClass().getId();
		}
		boolean isRetrialJob = false;

		switch (message.getEvent()) {

		case NetEvent.EVENT_START:

			//EVENT_START
			//If there are jobs in queue, the first (chosen using the specified
			//get strategy) is forwarded and coolStart becomes false.

			netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
			if (jobsList.size() > 0) {
				//the first job is forwarded to service section
				//job = getStrategy.get(jobsList);
				switchOverRequest = job == null;
				if (switchOverRequest && (getStrategy instanceof PollingGetStrategy)) {
					send(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0, NodeSection.SERVICE);
				} else {
					sendForward(getStrategy.get(jobsList), 0.0);
				}
				coolStart = false;
			}
			break;

			case NetEvent.EVENT_RETRIAL:
				/*
				EVENT_RETRIAL
				For this case, the job is resent to the queue (same source and same owner node) with a random delay.
				 */
				double randomDelay = retrialDistributionStrategies[job.getJobClass().getId()].wait(this, job.getJobClass());
				nodeJobsList.addToRetrialOrbit(job);
				sendMe(NetEvent.EVENT_RETRIAL_JOB, job, randomDelay);
				break;

			case NetEvent.EVENT_RETRIAL_JOB:
				nodeJobsList.retryJob(jobInfo);
				netJobsList.retryJob(job);
				isRetrialJob = true;

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
				//therefore in both cases the behaviour is the same
				// Before even entering the queue, we first check if the job will balk.
				// If it balks, do not proceed further with the rest of the code.
				if (jobWillBalk(job)) {
					performBalkingOperations(job);
					sendAckToMessageSource(message, job);
					break;
				}
				// Check if there is still capacity.
				// <= size because the arriving job has not been inserted in Queue
				// job list but has been inserted in NetNode job list !!
				// If true, then retrial will be considered as successful
				if (infinite || nodeJobsList.size() <= size) {
					// Queue is not full. Okay.
					double waitingTime = getNetSytem().getTime() - job.getSystemEnteringTime();
					if (nodeJobsList.getRetrialOrbit().containsKey(job.getId())) {
						waitingTime = getNetSytem().getTime() - nodeJobsList.getRetrialOrbit().get(job.getId()).get(0);
						nodeJobsList.removeFromRetrialOrbit(job);
					}
					nodeJobsList.updateWaitingTime(job, waitingTime);
					// If parent node is a fork node adds job to FJ info list
					if (getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork) {
						addJobToBuffer(job, message, BufferType.FJ_LIST);
					}
					// If coolStart is true, this is the first job received or the queue was empty: this job is sent immediately
					// to the next section and coolStart set to false.
						if (putStrategies[job.getJobClass().getId()] instanceof PreemptiveStrategy) {
						sendForward(job, 0.0);
						setRenegingEvent(job); //rose: if event is processed immediately, should not need to renege
					} else {
						int jobsInService = getOwnerNode().getSection(NodeSection.SERVICE).getIntSectionProperty(NodeSection.PROPERTY_ID_RESIDENT_JOBS);
						if (coolStart && (jobsInService < maxRunning || serviceCapacityInfinite)) {

							// No jobs in queue: Refresh jobsList and sends job (do not use put strategy, because queue is empty)
						if (jobsList.size() <= 0 && !(getStrategy instanceof PollingGetStrategy)) {
								jobsList.add(new JobInfo(job));
								setRenegingEvent(job); //rose: if event is processed immediately, will not renege

								// forward without any delay
								sendForward(jobsList.removeFirst().getJob(), 0.0);

							} else { //queue is not empty (need to queue before service)
								putStrategies[job.getJobClass().getId()].put(job, jobsList, this);
								setRenegingEvent(job);
	if (getStrategy instanceof PollingGetStrategy){
								if (!switchOverRequest) {
									Job jobSent = getStrategy.get(jobsList);
									if (jobSent == null) {
										switchOverRequest = true;
										send(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0, NodeSection.SERVICE);
									} else {
										sendForward(jobSent, 0.0);
									}
								}
							} else {
								Job jobSent = getStrategy.get(jobsList);
								sendForward(jobSent, 0.0);

							}

						}
							forwardRenegingData(job, this.getNetSytem().getTime());
						coolStart = false;
					} else {
						putStrategies[job.getJobClass().getId()].put(job, jobsList, this);
							setRenegingEvent(job);
					}
				}
				// sends an ACK backward
					if (!isRetrialJob) {
				send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
					}
			} else {
				// Queue is full. Now we use an additional queue or drop.

				// if the job has been sent by the owner node of this queue section
					if (isMyOwnerNode(message.getSource()) && !dropStrategies[jobClass].equals(FINITE_RETRIAL)) { // job sent by the node itself (corner case) -- should always be successful
					send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());

						addJobToBuffer(job, message, BufferType.WAITING_REQUESTS);
						setRenegingEvent(job);
					} else if (!drop[jobClass]) { // user did not select drop
				// otherwise if job has been sent by another node
						if (dropStrategies != null && dropStrategies[jobClass].equals(FINITE_RETRIAL)) {
							sendMe(NetEvent.EVENT_RETRIAL, job, 0);
						} else {
							addJobToBuffer(job, message, BufferType.WAITING_REQUESTS);
							setRenegingEvent(job);
						}
					// if blocking is disabled, sends ack otherwise router of the previous node remains busy
						if (!block[job.getJobClass().getId()] && !isRetrialJob) {
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

		case NetEvent.EVENT_POLLING_SERVER_READY:
		    switchOverRequest = false;
		    coolStart = true;
		    break;

		case NetEvent.EVENT_POLLING_SERVER_NEXT:
		    switchOverRequest = false;

			case NetEvent.EVENT_ACK:
				//EVENT_ACK
				//If there are waiting requests, the first is taken (if the source node of this request
				//is the owner node of this section, an ack message is sent).
				//The job contained is put into the queue using the specified put strategy.
				//At this point, if there are jobs in queue, the first is taken (using the
				//specified get strategy) and forwarded. Otherwise, if there are no jobs, coolStart is set true.
				// if there is a waiting request send ack to the first node
				if (waitingRequests.size() > 0) {
					WaitingRequest wr = (WaitingRequest) waitingRequests.removeFirst();

				// If the source is not the owner node sends ack if blocking is enabled. Otherwise 
				// ack was already sent.
				if (!isMyOwnerNode(wr.getSource()) && block[wr.getJob().getJobClass().getId()]) {
					send(NetEvent.EVENT_ACK, wr.getJob(), 0.0, wr.getSourceSection(), wr.getSource());
				}

				//the class ID of this job
					addJobToBuffer(wr.getJob(), message, BufferType.JOBS_LIST);
			}

			// if there is at least one job, sends it
			int jobsInService = getOwnerNode().getSection(NodeSection.SERVICE).getIntSectionProperty(NodeSection.PROPERTY_ID_RESIDENT_JOBS);
			if (getStrategy instanceof PollingGetStrategy) {
				if (!switchOverRequest) {
					Job jobSent = getStrategy.get(jobsList);
					if (jobSent == null) {
						switchOverRequest = true;
						send(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0, NodeSection.SERVICE);
					} else {
						sendForward(jobSent, 0.0);
					}
				}
			} else if (jobsList.size() > 0  && (jobsInService < maxRunning || serviceCapacityInfinite)) {
				// Gets job using a specific strategy and sends job
                    double jobEntryTimeToQueue = getStrategy.peek(jobsList).getEnteringTime();
					Job jobSent = getStrategy.get(jobsList);
					sendForward(jobSent, 0.0);
                    forwardRenegingData(jobSent, jobEntryTimeToQueue);

			} else {
			// else set coolStart to true
				coolStart = true;
			}
			break;

			case NetEvent.EVENT_JOB_COMPLETED:
				jobsInService = getOwnerNode().getSection(NodeSection.SERVICE).getIntSectionProperty(NodeSection.PROPERTY_ID_RESIDENT_JOBS);
				if (getStrategy instanceof PollingGetStrategy) {
				if (!switchOverRequest) {
					Job jobSent = getStrategy.get(jobsList);
					if (jobSent == null) {
						switchOverRequest = true;
						send(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0, NodeSection.SERVICE);
					} else {
						sendForward(jobSent, 0.0);
					}
				}
			} else if (jobsList.size() > 0 && (jobsInService < maxRunning || serviceCapacityInfinite)) {
					// Gets job using a specific strategy and sends job
					Job jobSent = getStrategy.get(jobsList);
					setRenegingEvent(jobSent);
					sendForward(jobSent, 0.0);
					forwardRenegingData(jobSent, this.getNetSytem().getTime());
				} else {
					coolStart = true;
				}
				break;
			case NetEvent.EVENT_PREEMPTED_JOB:
				job = message.getJob();
				addJobToBuffer(job, message, BufferType.JOBS_LIST);
				setRenegingEvent(job);
				break;

			case NetEvent.EVENT_JOIN:
				if (!(getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork)) {
					return MSG_NOT_PROCESSED;
				}
				job = (Job) message.getData();
				jobInfo = getJobInfoFromBuffer(job, FJList);
				if (jobInfo != null) {
					FJList.remove(jobInfo);
				}
				break;

			case NetEvent.EVENT_RENEGE:
				// Find the job in the jobsList, and if it is present, drop the job from the system
				job = (Job) message.getData();
				// Look for the job in either jobsList or in waitingRequests and perform the reneging (if the job can be found).
				// It cannot exist in both places at the same time.
				if (getJobInfoFromBuffer(job, jobsList) != null) {
					performRenegingOperations(job, jobsList);
				} else if (getJobInfoFromBuffer(job, waitingRequests) != null) {
					performRenegingOperations(job, waitingRequests);
				}
				renegingDelayPerJob.remove(job);

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

	/**
	 * This method should be used only in process(), and whenever a job is to be added to a buffer in this class.
	 */
	private void addJobToBuffer(Job job, NetMessage message, BufferType bufferType) throws NetException {
		switch (bufferType) {
			case FJ_LIST:
				FJList.add(new JobInfo(job));
				break;
			case JOBS_LIST:
				putStrategies[getJobClassId(job)].put(job, jobsList, this);
				break;
			case WAITING_REQUESTS:
				JobInfo waitingRequestInfo = new WaitingRequest(job, message.getSourceSection(), message.getSource());
				waitingRequests.add(waitingRequestInfo);
				break;
		}
	}

	/**
	 * Forwards, to the ServiceStation, data on the time left before the job reneges.
	 * Only a PSServer is designed to process this particular event.
	 */
	private void forwardRenegingData(Job job, double jobEntryTimeToQueue) {
		if (jobClassHasRequiredImpatience(getJobClassId(job), ImpatienceType.RENEGING)
						&& linkedToPSServer) {
			double renegingDelay = renegingDelayPerJob.get(job);
			Double remainingTimeBeforeReneging = jobEntryTimeToQueue + renegingDelay - this.getNetSytem().getTime();
			Map<Job, Double> jobToRemainingTimeMap = new HashMap<>();
			jobToRemainingTimeMap.put(job, remainingTimeBeforeReneging);
			sendForward(NetEvent.EVENT_RENEGE, jobToRemainingTimeMap, 0.0);
		}
	}

	private void sendAckToMessageSource(NetMessage message, Job job) {
		send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
	}

	/**
	 * Checks if the job is will balk before even entering the queue.
	 * If it balks, call another method to handle the balking operation.
	 *
	 * @param job the job being processed.
	 */
	private boolean jobWillBalk(Job job) {
		int jobClassId = getJobClassId(job);

		// Allows the job to balk only if the impatience strategy for the station-class is Balking
		if (jobClassHasRequiredImpatience(jobClassId, ImpatienceType.BALKING)) {
			boolean priorityActivated = ((Balking) impatienceStrategyPerStationClass[jobClassId]).isPriorityActivated();
			int jobClassPriority = job.getJobClass().getPriority();
			BooleanValueImpatienceMeasurement balkingStatus =
							new BooleanValueImpatienceMeasurement(jobsList, waitingRequests, jobClassPriority, priorityActivated);
			// If queueLength = 0, it not possible at all for the job to balk
			if (balkingStatus.getQueueLength() <= 0) {
				return false;
			}
			// Modify by reference the boolean value held in balkingStatus
			impatienceStrategyPerStationClass[jobClassId].generateImpatience(balkingStatus);
			return balkingStatus.getBooleanValue();
		}
		return false;
	}

	/**
	 * Gets the reneging time of the job, adds it to the Map and sends a reneging event to itself.
	 *
	 * @param job the job being processed.
	 */
	private void setRenegingEvent(Job job) {
		int jobClassId = getJobClassId(job);
		// Allows the job to renege only if the impatience strategy for the station-class is Reneging
		if (jobClassHasRequiredImpatience(jobClassId, ImpatienceType.RENEGING)) {
			DoubleValueImpatienceMeasurement renegingDelay = new DoubleValueImpatienceMeasurement();
			// Modify by reference the MutableDouble value held in renegingDelay
			impatienceStrategyPerStationClass[jobClassId].generateImpatience(renegingDelay);
			renegingDelayPerJob.put(job, renegingDelay.doubleValue());
			// Set the delay after which the job reneges, and send this event to itself
			sendMe(NetEvent.EVENT_RENEGE, job, renegingDelay.doubleValue());
		}
	}

	/**
	 * Checks if the particular job class has an Impatience object attached to it
	 *
	 * @param jobClassId
	 * @param requiredImpatience
	 * @return
	 */
	private boolean jobClassHasRequiredImpatience(int jobClassId, ImpatienceType requiredImpatience) {
		return impatienceStrategyPerStationClass != null && impatienceStrategyPerStationClass[jobClassId] != null
						&& impatienceStrategyPerStationClass[jobClassId].isImpatienceType(requiredImpatience);
	}


	/**
	 * Reneges the job from the Queue, NetNode, and System. Thereafter, updates the counters for reneging.
	 *
	 * @param job         the job being reneged.
	 * @param jobsInQueue the JobInfoList belonging to the Queue, from which the job will renege
	 */
	private void performRenegingOperations(Job job, JobInfoList jobsInQueue) {
		JobInfo jobInfoInQueueClass = getJobInfoFromBuffer(job, jobsInQueue);
		JobInfo jobInfoInNetNode = getJobInfoFromBuffer(job, nodeJobsList);
		// Renege jobs from jobsInQueue (Queue item), nodeJobsList (NetNode item) and netJobsList (Global item)
		jobsInQueue.renegeJob(jobInfoInQueueClass);
		nodeJobsList.renegeJob(jobInfoInNetNode);
		netJobsList.renegeJob(job);
	}

	/**
	 * Balks the job from the Queue, NetNode, and System. Thereafter, updates the counters for balking.
	 * @param job the job being balked.
	 */
	private void performBalkingOperations(Job job) {
		JobInfo jobInfoInNetNode = getJobInfoFromBuffer(job, nodeJobsList);
		nodeJobsList.balkJob(jobInfoInNetNode);
		netJobsList.balkJob(job);
	}

	private int getJobClassId(Job job) {
		return job.getJobClass().getId();
	}

	private JobInfo getJobInfoFromBuffer(Job job, JobInfoList buffer) {
		return buffer.lookFor(job);
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
