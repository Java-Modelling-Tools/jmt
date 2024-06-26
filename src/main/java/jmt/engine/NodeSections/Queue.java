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

import java.util.*;

import jmt.common.exception.IncorrectDistributionParameterException;
import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ImpatienceStrategies.Balking;
import jmt.engine.NetStrategies.ImpatienceStrategies.Impatience;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.BooleanValueImpatienceMeasurement;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceMeasurement.DoubleValueImpatienceMeasurement;
import jmt.engine.NetStrategies.ImpatienceStrategies.ImpatienceType;
import jmt.engine.NetStrategies.QueueGetStrategies.PollingGetStrategy;
import jmt.engine.NetStrategies.QueueGetStrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.PreemptiveStrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy;
import jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy;
import jmt.engine.NetStrategies.TransitionUtilities.TransitionMatrix;
import jmt.engine.NetStrategies.TransitionUtilities.TransitionVector;
import jmt.engine.QueueNet.*;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.dataAnalysis.Measure;
import jmt.engine.simEngine.RemoveToken;
import jmt.gui.common.distributions.Distribution;

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

	private int capacity;
	private boolean infinite;

	private boolean coolStart;
	private boolean switchOverRequest;

	private boolean[] drop;
	private boolean[] block;
	private boolean[] waiting;
	private boolean[] retrial;
	private boolean delayOffEnabled;

	private JobClassList jobClasses;

	//the JobInfoList of the owner NetNode
	private JobInfoList nodeJobsList;
	//the JobInfoList of the global Network -- model level
	private GlobalJobInfoList netJobsList;
	//the JobInfoList used for Fork nodes
	private JobInfoList FJList;
	// Backup buffer when the main jobsList is full
	private JobInfoList waitingRequests;

	// Retrial distribution for each class
	private ServiceStrategy[] retrialDistributions;
	// Impatience strategy for each class
	private Impatience[] impatienceStrategies;

	// Get and put strategies for the Queue
	private QueueGetStrategy getStrategy;
	private QueuePutStrategy[] putStrategies;
	// Class soft deadlines for the overall station
	private double[] softDeadlines;
	private ServiceTimeStrategy[] delayOffDistributions;
	private ServiceTimeStrategy[] setUpDistributions;
	private boolean workingAsTransition;
	private TransitionMatrix[] enablingConditions;
	private TransitionMatrix[] inhibitingConditions;
	private TransitionMatrix[] resourceConditions;
	private TransitionMatrix storageSituation;
	List<NetMessage> jobsFromPlaces;
	private int savedEnablingDegree;
	private int unfinishedRequestNumber;

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
		JOBS_LIST_FIRST,
		JOBS_LIST_LAST,
		WAITING_REQUESTS,
	}

	public Queue(Integer capacity, String[] dropStrategies, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies) {
		this(capacity, dropStrategies, null, getStrategy, putStrategies, null,
				false, null, null, null);
	}

	public Queue(Integer capacity, String[] dropStrategies, ServiceStrategy[] retrialDistributions, QueueGetStrategy getStrategy,
				 QueuePutStrategy[] putStrategies) {
		this(capacity, dropStrategies, retrialDistributions, getStrategy, putStrategies, null,
				false, null, null, null);
	}

	public Queue(Integer capacity, String[] dropStrategies, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies,
				 Impatience[] impatienceStrategies) {
		this(capacity, dropStrategies, null, getStrategy, putStrategies, impatienceStrategies,
				false, null, null, null);
	}

	public Queue(Integer capacity, String[] dropStrategies, QueueGetStrategy getStrategy, QueuePutStrategy[] putStrategies,
				 TransitionMatrix[] enablingConditions, TransitionMatrix[] inhibitingConditions, TransitionMatrix[] resourceConditions){
		this(capacity, dropStrategies, null, getStrategy, putStrategies, null, true,
				enablingConditions, inhibitingConditions, resourceConditions);
	}

	/**
	 * Creates a new instance of finite Queue. This is the newest constructor that supports
	 * different drop strategies. Other constructors are left for compatibility.
	 * @param capacity Station capacity (-1 = infinite capacity).
	 * @param dropStrategies Drop strategy per class: FINITE_DROP || FINITE_BLOCK
	 * || FINITE_WAITING || FINITE_RETRIAL.
	 * @param retrialDistributions Retrial distribution per class.
	 * @param getStrategy Queue get strategy.
	 * @param putStrategies Queue put strategy per class.
	 * @param impatienceStrategies Impatience strategy per class.
	 */
	public Queue(Integer capacity, String[] dropStrategies, ServiceStrategy[] retrialDistributions, QueueGetStrategy getStrategy,
				 QueuePutStrategy[] putStrategies, Impatience[] impatienceStrategies, Boolean workingAsTransition,
				 TransitionMatrix[] enablingConditions, TransitionMatrix[] inhibitingConditions, TransitionMatrix[] resourceConditions) {
		//auto = false, otherwise when a JOB message is received,
		//the corresponding Job object is automatically added to
		//JobInfoList
		super(false);
		this.capacity = capacity.intValue();
		this.infinite = this.capacity <= 0;
		// Decodes drop strategies
		this.drop = new boolean[dropStrategies.length];
		this.block = new boolean[dropStrategies.length];
		this.waiting = new boolean[dropStrategies.length];
		this.retrial = new boolean[dropStrategies.length];
		for (int i = 0; i < dropStrategies.length; i++) {
			switch (dropStrategies[i]) {
				case FINITE_DROP:
					drop[i] = true;
					break;
				case FINITE_BLOCK:
					block[i] = true;
					break;
				case FINITE_WAITING:
					waiting[i] = true;
					break;
				case FINITE_RETRIAL:
					retrial[i] = true;
					break;
				default:
					break;
			}
		}
		this.getStrategy = getStrategy;
		this.putStrategies = putStrategies;
		this.retrialDistributions = retrialDistributions;
		this.impatienceStrategies = impatienceStrategies;

		this.workingAsTransition = workingAsTransition;
		this.enablingConditions = enablingConditions;
		this.inhibitingConditions = inhibitingConditions;
		this.resourceConditions = resourceConditions;

		coolStart = true;
		switchOverRequest = false;
		//this node does not belong to any blocking region
		redirectionTurnOFF();
	}

	/**
	 * Tells whether the "redirecting queue" behaviour has been turned on.
	 * @return true, if the "redirecting queue" behaviour is on; false otherwise.
	 */
	public boolean isRedirectionON() {
		return redirectionON;
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

	@Override
	protected void nodeLinked(NetNode node) throws NetException {
		// for easier reference
		jobClasses = getJobClasses();
		nodeJobsList = node.getJobInfoList();

		waitingRequests = new LinkedJobInfoList(jobClasses, getClassCompatibilities(),getOwnerNode().getName(), getServerTypes());
		waitingRequests.setNetSystem(node.getNetSystem());

		if (putStrategies == null) {
			putStrategies = new QueuePutStrategy[jobClasses.size()];
			Arrays.fill(putStrategies, new TailStrategy());
		}

		if (getStrategy instanceof PollingGetStrategy) {
			((PollingGetStrategy) getStrategy).setPollingQueues(jobClasses);
		}

		if (node.getSection(NodeSection.OUTPUT) instanceof Fork) {
			FJList = new LinkedJobInfoList(jobClasses, getClassCompatibilities(),getOwnerNode().getName(), getServerTypes());
			FJList.setNetSystem(node.getNetSystem());
		}

		if(workingAsTransition) {
			this.storageSituation = new TransitionMatrix(jobClasses.size());
			for (String nodeName : enablingConditions[0].keySet()) {
				storageSituation.setVector(new TransitionVector(nodeName, jobClasses.size()));
			}
			this.savedEnablingDegree = 0;
			this.unfinishedRequestNumber = 0;
			this.jobsFromPlaces = new LinkedList<NetMessage>();
		}
	}

	/**
	 * Preloads the specified numbers of jobs for each class.
	 * @param jobsPerClass the specified numbers of jobs for each class.
	 * @throws NetException
	 */
	public void preloadJobs(int[] jobsPerClass) throws NetException {
		netJobsList = getOwnerNode().getQueueNet().getJobInfoList();
		NetSystem netSystem = getOwnerNode().getNetSystem();
		int totalJobs = 0;
		for (int i = 0; i < jobsPerClass.length; i++) {
			totalJobs += jobsPerClass[i];
		}
		Job[] jobPermutation = new Job[totalJobs];
		int index = 0;
		for (int i = 0; i < jobsPerClass.length; i++) {
			for (int j = 0; j < jobsPerClass[i]; j++) {
				Job job = new Job(jobClasses.get(i), netJobsList);
				job.initialize(netSystem);
				updateVisitPath(job);
				jobPermutation[index] = job;
				index++;
			}
		}

        /* Commented to ensure a deterministic initialization
		// Durstenfeld Shuffle
		RandomEngine randomEngine = netSystem.getEngine();
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
	protected int process(NetMessage message) throws NetException, IncorrectDistributionParameterException {
		NetSystem netSystem = getOwnerNode().getNetSystem();
		Job job = message.getJob();
		JobInfo jobInfo = null;
		int jobClass = -1;
		Job jobSent = null;
		boolean isRetrialJob = false;
		int mode = 0;
		NetNode sourceNode = message.getSource();
		int sourceSection = message.getSourceSection();
		boolean comingFromStorageSection = sourceNode != null && sourceSection > 0 && (sourceNode.getSection(sourceSection) instanceof Linkage);

		switch (message.getEvent()) {

			case NetEvent.EVENT_START: {

				//EVENT_START
				//If there are jobs in queue, the first (chosen using the specified
				//get strategy) is forwarded and coolStart becomes false.

				netJobsList = getOwnerNode().getQueueNet().getJobInfoList();

				if (!(workingAsTransition && savedEnablingDegree == 0)) {
					if (jobsList.size() > 0) {
						//the first job is forwarded to service section
						jobSent = getStrategy.get(jobsList);
						if (getStrategy instanceof PollingGetStrategy) {
							if (jobSent == null) {
								sendForward(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0);
								switchOverRequest = true;
							} else {
								sendForward(jobSent, 0.0);
							}
						} else {
							if (getOwnerNode().getServer() != null) {
								jobSent = getStrategy.get(jobsList, getOwnerNode());
								if (jobSent != null) {
									sendForward(jobSent, 0.0);
								}
							} else{
								sendForward(jobSent, 0.0);
							}
						}
						coolStart = false;

						Iterator<JobInfo> it = jobsList.getInternalJobInfoList().iterator();
						while (it.hasNext()) {
							createRenegingEvent(it.next().getJob());
						}
					}
				}

				break;
			}

			case NetEvent.EVENT_RETRIAL:
				double randomDelay = retrialDistributions[job.getJobClass().getId()].wait(this, job.getJobClass());
				nodeJobsList.addToRetrialOrbit(job);
				sendMe(NetEvent.EVENT_RETRIAL_JOB, job, randomDelay);
				break;

			case NetEvent.EVENT_RETRIAL_JOB:
				jobInfo = nodeJobsList.lookFor(job);
				nodeJobsList.retryJob(jobInfo);
				if (!(job instanceof ForkJob)) {
					netJobsList.retryJob(job);
				}
				isRetrialJob = true;
				break;

			case NetEvent.EVENT_JOB: {
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

				if(comingFromStorageSection){
					jobsFromPlaces.add(message);
					break;
				}

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
				jobClass = job.getJobClass().getId();
				boolean isJobInQueue = true;
				if (infinite || nodeJobsList.size() <= capacity) {
					// Queue is not full. Okay.
					if (nodeJobsList.getRetrialOrbit().containsKey(job.getId())) {
						nodeJobsList.removeFromRetrialOrbit(job);
					}
					// If parent node is a fork node adds job to FJ info list
					if (getOwnerNode().getSection(NodeSection.OUTPUT) instanceof Fork) {
						addJobToBuffer(job, message, BufferType.FJ_LIST);
					}

					if (softDeadlines != null) {
						job.setCurrentStationSoftDeadline(softDeadlines[job.getJobClass().getId()]
								+ getOwnerNode().getNetSystem().getTime());
					}

					putStrategies[jobClass].put(job, jobsList, this);

					Server server = getOwnerNode().getServer();

					if(!(workingAsTransition && savedEnablingDegree == 0)) {
						if (coolStart) {
							if (getStrategy instanceof PollingGetStrategy) {
								jobSent = getStrategy.get(jobsList);
								if (!switchOverRequest) {
									if (jobSent == null) {
										sendForward(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0.0);
										switchOverRequest = true;
									} else {
										sendForward(jobSent, 0.0);
										isJobInQueue = false;
									}
								}
							} else {
								if (server != null) {
									jobSent = getStrategy.get(jobsList, getOwnerNode());
									if (jobSent != null) {
										sendForward(jobSent, 0.0);
									}
								} else {
									jobSent = getStrategy.get(jobsList);
									sendForward(jobSent, 0.0);
								}
								isJobInQueue = false;
							}

							coolStart = false;
						} else {
							if (putStrategies[jobClass] instanceof PreemptiveStrategy) {
								PreemptiveStrategy preemptiveStrategy = (PreemptiveStrategy) putStrategies[jobClass];
								Job lastJobInService = ((PreemptiveServer) getOwnerNode().getSection(NodeSection.SERVICE)).getLastJobInfo().getJob();
								if (preemptiveStrategy.compare(job, lastJobInService) > 0) {
									if(server != null) {
										if (jobsList.size() > 0) {
											jobSent = getStrategy.get(jobsList, getOwnerNode());
											if (jobSent != null) {
												sendForward(jobSent, 0.0);
											}
										} else if (server.getNumOfFreeServersForClass(job.getJobClass().getId()) > 0) {
											sendForward(job, 0.0);
										}
									} else {
										jobSent = getStrategy.get(jobsList);
										sendForward(jobSent, 0.0);
										isJobInQueue = false;
									}
								}
							} else if (server != null && server.hasFreeServers()) {
								if (jobsList.size() > 0) {
									jobSent = getStrategy.get(jobsList, getOwnerNode());
									if (jobSent != null) {
										sendForward(jobSent, 0.0);
									}
								} else if (server.getNumOfFreeServersForClass(job.getJobClass().getId()) > 0) {
									sendForward(job, 0.0);
								}
							}
						}
					}

					// sends an ACK backward
					if (!isRetrialJob) {
						send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
					}

				} else {
					// Queue is full. Now we use an additional queue or drop.
					// if the job has been sent by the owner node of this queue section
					if (isMyOwnerNode(message.getSource()) && !isRetrialJob) {
						// job sent by the node itself (corner case) -- should always be successful
						addJobToBuffer(job, message, BufferType.WAITING_REQUESTS);
						send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
					} else if (drop[jobClass]) {
						// otherwise if the drop strategy is enabled
						if (!(job instanceof ForkJob)) {
							netJobsList.dropJob(job);
						}
						sendAckAfterDrop(job, 0.0, message.getSourceSection(), message.getSource());
						if (redirectionON) {
							myRegion.decreaseOccupation(job.getJobClass());
							send(NetEvent.EVENT_JOB_OUT_OF_REGION, job, 0.0, NodeSection.INPUT, regionInputStation);
						}
						isJobInQueue = false;
					} else if (block[jobClass]) {
						// otherwise if the blocking strategy is enabled
						addJobToBuffer(job, message, BufferType.WAITING_REQUESTS);
					} else if (waiting[jobClass]) {
						// otherwise if the waiting strategy is enabled
						addJobToBuffer(job, message, BufferType.WAITING_REQUESTS);
						send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
					} else if (retrial[jobClass]) {
						// otherwise if the retrial strategy is enabled
						sendMe(NetEvent.EVENT_RETRIAL, job, 0.0);
						if (!isRetrialJob) {
							send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
						}
						isJobInQueue = false;
					} else {
						// otherwise
						return MSG_NOT_PROCESSED;
					}
				}
				if (isJobInQueue) {
					createRenegingEvent(job);
				}

				break;
			}

			case NetEvent.EVENT_POLLING_SERVER_READY:
				if (jobsList.size() > 0) {
					jobSent = getStrategy.get(jobsList);
					if (jobSent == null) {
						sendForward(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0.0);
					} else {
						sendForward(jobSent, 0.0);
						cancelRenegingEvent(jobSent);
						switchOverRequest = false;
					}
				} else {
					coolStart = true;
					switchOverRequest = false;
				}
				break;

			case NetEvent.EVENT_ACK:
				//EVENT_ACK
				//If there are waiting requests, the first is taken (if the source node of this request
				//is the owner node of this section, an ack message is sent).
				//The job contained is put into the queue using the specified put strategy.
				//At this point, if there are jobs in queue, the first is taken (using the
				//specified get strategy) and forwarded. Otherwise, if there are no jobs,
				//coolStart is set true.

				if (waitingRequests.size() > 0) {
					WaitingRequest wr = (WaitingRequest) waitingRequests.removeFirst();
					if (!isMyOwnerNode(wr.getSource()) && block[wr.getJob().getJobClass().getId()]) {
						send(NetEvent.EVENT_ACK, wr.getJob(), 0.0, wr.getSourceSection(), wr.getSource());
					}
					addJobToBuffer(wr.getJob(), message, BufferType.JOBS_LIST);
				}

				if (jobsList.size() > 0) {
					if(getOwnerNode().getServer() != null) {
						jobSent = getStrategy.get(jobsList, getOwnerNode());
						if (jobSent != null) {
							sendForward(jobSent, 0.0);
							cancelRenegingEvent(jobSent);
						} else {
							coolStart = true;
						}
					} else {
						jobSent = getStrategy.get(jobsList);
						if (getStrategy instanceof PollingGetStrategy) {
							if (!switchOverRequest) {
								if (jobSent == null) {
									sendForward(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0.0);
									switchOverRequest = true;
								} else {
									sendForward(jobSent, 0.0);
									cancelRenegingEvent(jobSent);
								}
							}
						} else {
							sendForward(jobSent, 0.0);
							cancelRenegingEvent(jobSent);
						}
						coolStart = false;
					}
				} else {
					// GC: new section, if it is not execute then it will be "wait-and-see" polling
					if (getStrategy instanceof PollingGetStrategy) {
						if (!switchOverRequest) {
							sendForward(NetEvent.EVENT_POLLING_SERVER_NEXT, null, 0.0);
							switchOverRequest = true;
						}
					}
					coolStart = true;
				}

				break;

			case NetEvent.EVENT_PREEMPTED_JOB:
				addJobToBuffer(job, message, BufferType.JOBS_LIST_FIRST);
				createRenegingEvent(job);
				break;

			case NetEvent.EVENT_JOIN:
				jobInfo = getJobInfoFromBuffer(job, FJList);
				if (jobInfo != null) {
					FJList.remove(jobInfo);
				}
				break;

			case NetEvent.EVENT_RENEGE:
				if (getJobInfoFromBuffer(job, waitingRequests) != null) {
					performRenegingOperations(job, waitingRequests);
				}
				if (getJobInfoFromBuffer(job, jobsList) != null) {
					performRenegingOperations(job, jobsList);
					if (waitingRequests.size() > 0) {
						WaitingRequest wr = (WaitingRequest) waitingRequests.removeFirst();
						if (!isMyOwnerNode(wr.getSource()) && block[wr.getJob().getJobClass().getId()]) {
							send(NetEvent.EVENT_ACK, wr.getJob(), 0.0, wr.getSourceSection(), wr.getSource());
						}
						addJobToBuffer(wr.getJob(), message, BufferType.JOBS_LIST);
					}
				}

			case NetEvent.EVENT_JOB_RELEASE:
				break;

			case NetEvent.EVENT_STOP:
				break;

			case NetEvent.EVENT_JOB_CHANGE: {
				//Calcolo enabling degree
				Object data = message.getData();
				if(data != null){
					TransitionVector storageVector = (TransitionVector) data;
					for(int i = 0; i < jobClasses.size(); i++) {
						storageSituation.setEntry(storageVector.getKey(), i, storageVector.getEntry(i));
					}
				}

				int enablingDegree = -1;

				OUTER_LOOP:
				for(String nodeName : enablingConditions[mode].keySet()){
					if(enablingConditions[mode].getTotal(nodeName) > 0){
						for(int j = 0; j < jobClasses.size(); j++){
							int availableJobNumber = storageSituation.getEntry(nodeName, j);
							int requiredJobNumber = enablingConditions[mode].getEntry(nodeName, j);
							if(requiredJobNumber > 0){
								if(enablingDegree < 0){
									enablingDegree = availableJobNumber / requiredJobNumber;
								}else{
									enablingDegree = Math.min(enablingDegree, availableJobNumber / requiredJobNumber);
								}
								if(enablingDegree == 0){
									break OUTER_LOOP;
								}
							}
						}
					}

					if(inhibitingConditions[mode].getTotal(nodeName) > 0){
						for(int j = 0; j < jobClasses.size(); j++){
							int availableJobNumber = storageSituation.getEntry(nodeName, j);
							int requiredJobNumber = inhibitingConditions[mode].getEntry(nodeName, j);
							if(requiredJobNumber > 0 && availableJobNumber >= requiredJobNumber){
								enablingDegree = 0;
								break OUTER_LOOP;
							}
						}
					}
				}

				savedEnablingDegree = enablingDegree;
				sendForward(NetEvent.EVENT_ENABLING, enablingDegree, 0.0);

				break;
			}

			case NetEvent.EVENT_TIMING:{
				for(String nodeName : enablingConditions[mode].keySet()){
					int enablingJobNumberFromNode = enablingConditions[mode].getTotal(nodeName);
					int resourceJobNumberFromNode = resourceConditions[mode].getTotal(nodeName);
					if(enablingJobNumberFromNode - resourceJobNumberFromNode > 0){
						send(NetEvent.EVENT_JOB_REQUEST_FROM_SERVER, new RequestToPlace(
								enablingConditions[mode].getVector(nodeName),
										resourceConditions[mode].getVector(nodeName)),
								0.0, NodeSection.OUTPUT, netSystem.getNode(nodeName)
						);

						unfinishedRequestNumber++;
					}
				}

				if(unfinishedRequestNumber == 0){
					sendForward(NetEvent.EVENT_FIRING, null, 0.0);
				}
				break;
			}

			case NetEvent.EVENT_JOB_FINISH:{
				if(unfinishedRequestNumber <= 0){
					return MSG_NOT_PROCESSED;
				}

				unfinishedRequestNumber--;
				if(unfinishedRequestNumber == 0){
					while(!jobsFromPlaces.isEmpty()){
						NetMessage m = jobsFromPlaces.remove(0);
						Job j = m.getJob();
						if(j != null){
							netJobsList.consumeJob(j);
						}
					}

					sendForward(NetEvent.EVENT_FIRING, null, 0.0);
				}

				break;
			}

			case NetEvent.EVENT_RESET_COOLSTART:{
				coolStart = (boolean) message.getData();
				break;
			}

			default:
				return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	/**
	 * This method returns the capacity of the station (queue+server).
	 */
	public int getStationCapacity() {
		return capacity;
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
			case JOBS_LIST_FIRST:
				jobsList.addFirst(new JobInfo(job));
				break;
			case JOBS_LIST_LAST:
				jobsList.addLast(new JobInfo(job));
				break;
			case WAITING_REQUESTS:
				JobInfo waitingRequestInfo = new WaitingRequest(job, message.getSourceSection(), message.getSource());
				waitingRequests.add(waitingRequestInfo);
				break;
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
			boolean priorityActivated = ((Balking) impatienceStrategies[jobClassId]).isPriorityActivated();
			int jobClassPriority = job.getJobClass().getPriority();
			BooleanValueImpatienceMeasurement balkingStatus =
					new BooleanValueImpatienceMeasurement(jobsList, waitingRequests, jobClassPriority, priorityActivated);
			if (balkingStatus.getQueueLength() <= 0) {
				return false;
			}
			impatienceStrategies[jobClassId].generateImpatience(balkingStatus);
			return balkingStatus.getBooleanValue();
		}
		return false;
	}

	/**
	 * Creates the reneging event of the job and updates the reneging message accordingly.
	 *
	 * @param job the job being processed.
	 */
	private void createRenegingEvent(Job job) {
		int jobClassId = getJobClassId(job);
		// Allows the job to renege only if the impatience strategy for the station-class is Reneging
		if (jobClassHasRequiredImpatience(jobClassId, ImpatienceType.RENEGING)) {
			DoubleValueImpatienceMeasurement renegingDelay = new DoubleValueImpatienceMeasurement();
			impatienceStrategies[jobClassId].generateImpatience(renegingDelay);
			RemoveToken renegingMessage = sendMe(NetEvent.EVENT_RENEGE, job, renegingDelay.doubleValue());
			job.setRenegingMessage(renegingMessage);
		}
	}

	/**
	 * Cancel the reneging event of the job and updates the reneging message accordingly.
	 *
	 * @param job the job being processed.
	 */
	private void cancelRenegingEvent(Job job) {
		int jobClassId = getJobClassId(job);
		// Allows the job to renege only if the impatience strategy for the station-class is Reneging
		if (jobClassHasRequiredImpatience(jobClassId, ImpatienceType.RENEGING)) {
			RemoveToken renegingMessage = job.getRenegingMessage();
			removeMessage(renegingMessage);
			job.setRenegingMessage(null);
		}
	}

	/**
	 * Checks if the particular job class has an Impatience object attached to it.
	 */
	private boolean jobClassHasRequiredImpatience(int jobClassId, ImpatienceType requiredImpatience) {
		return impatienceStrategies != null && impatienceStrategies[jobClassId] != null
				&& impatienceStrategies[jobClassId].isImpatienceType(requiredImpatience);
	}

	/**
	 * Balks the job from the Queue, NetNode, and System. Thereafter, updates the counters for balking.
	 *
	 * @param job the job being balked.
	 */
	private void performBalkingOperations(Job job) {
		JobInfo jobInfoInNetNode = getJobInfoFromBuffer(job, nodeJobsList);
		nodeJobsList.balkJob(jobInfoInNetNode);
		if (!(job instanceof ForkJob)) {
			netJobsList.balkJob(job);
		}
	}

	/**
	 * Reneges the job from the Queue, NetNode, and System. Thereafter, updates the counters for reneging.
	 *
	 * @param job         the job being reneged.
	 * @param jobsInQueue the JobInfoList belonging to the Queue, from which the job will renege.
	 */
	private void performRenegingOperations(Job job, JobInfoList jobsInQueue) {
		JobInfo jobInfoInQueueClass = getJobInfoFromBuffer(job, jobsInQueue);
		JobInfo jobInfoInNetNode = getJobInfoFromBuffer(job, nodeJobsList);
		jobsInQueue.renegeJob(jobInfoInQueueClass);
		nodeJobsList.renegeJob(jobInfoInNetNode);
		if (!(job instanceof ForkJob)) {
			netJobsList.renegeJob(job);
		}
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
				FJList.analyzeQueueLength(jobClass, measurement, false, 0);
				break;
			case SimConstants.FORK_JOIN_RESPONSE_TIME:
				FJList.analyzeResponseTime(jobClass, measurement, false, 0);
				break;
			default:
				throw new NetException(this, EXCEPTION_MEASURE_DOES_NOT_EXIST, "required analyzer does not exist!");
		}
	}

	public boolean isStationWorkingAsATransition(){ return workingAsTransition; }

	public void setSoftDeadlines(double[] softDeadlines) {
		this.softDeadlines = softDeadlines;
	}

}
