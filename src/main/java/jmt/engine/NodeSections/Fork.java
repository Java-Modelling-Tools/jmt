/**
 * Copyright (C) 2006, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package jmt.engine.NodeSections;

import java.util.Iterator;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.ForkStrategies.ClassSwitchFork;
import jmt.engine.NetStrategies.ForkStrategy;
import jmt.engine.QueueNet.BlockingRegion;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.GlobalJobInfoList;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeList;
import jmt.engine.QueueNet.NodeListWithJobNum;
import jmt.engine.QueueNet.NodeSection;
import static jmt.gui.common.CommonConstants.STATION_TYPE_FORK;

/**
 * <p>Title: Fork</p>
 * <p>Description: This class is a fork output section, used to split a job on every
 * output link. Splitted job can be assembled back in a join input section.
 * A maximum number of jobs inside a fork-join section can be specified: when reached
 * this component will block until at least one job is joined.</p>
 *
 * @author Bertoli Marco
 *         Date: 13-mar-2006
 *         Time: 15.23.22
 * 
 * Modified by J. Shuai & M. Cazzoli, implemented fork strategies
 */
public class Fork extends OutputSection {
	/** Maximum number of jobs allowed in a fork-join region (-1 or 0 is infinity) */
	private int block;

	/** Counts received ACK by following nodes */
	private int ackCount;

	/** Number of jobs to be routed on each link */
	private int jobsPerLink;

	/** Number of output nodes */
	private int numOut;

	/** Current number of jobs inside a fork-join region */
	private int jobNum;
        
        private Boolean isSimplifiedFork;

	private ForkStrategy[] forkStrategies;
        
        private int[] switchedClasses; 
	private int totalSwitchedClasses = 0;

	// --- Constructors -----------------------------------------------------------------------------
	/**
	 * Constructs a new Fork without blocking and with 1 job per link
	 */
	public Fork() {
		this(1, -1);
	}

	/**
	 * Construst a new Fork node
	 * @param jobsPerLink number of jobs to be routed on each link
	 * @param block maximum number of jobs allowed in a fork-join
	 * region (-1 or 0 is infinity)
	 */
	public Fork(Integer jobsPerLink, Integer block) {
		this(jobsPerLink.intValue(), block.intValue());
	}

	/**
	 * Construst a new Fork node
	 * @param jobsPerLink number of jobs to be routed on each link
	 * @param block maximum number of jobs allowed in a fork-join
	 * region (-1 or 0 is infinity)
	 */
	public Fork(int jobsPerLink, int block) {
		// Disables automatic handling of jobinfolists
		super(false);
		jobNum = 0;
		this.block = block;
		this.jobsPerLink = jobsPerLink;
	}
        
	//extend the fork section with fork strategies
	public Fork(Integer jobsPerLink, Integer block, Boolean isSimplifiedFork,
			ForkStrategy[] forkStrategies) {
		this(jobsPerLink, block);
		//if use the original simple fork
		this.isSimplifiedFork = isSimplifiedFork;
		this.forkStrategies = forkStrategies;
	}

	// ----------------------------------------------------------------------------------------------

	/**
	 * Splits input job on every output link and waits if 'block' job number are
	 * not joined if and only if block is enabled.
	 *
	 * @param message message to be processed.
	 * @throws NetException if something goes wrong
	 * @return message processing result.
	 */
	@Override
	protected int process(NetMessage message) throws NetException {
		Job job;
		job = message.getJob();
		// Finds event type
		switch (message.getEvent()) {
			case NetEvent.EVENT_JOB:
				// OLD
                                // NodeList output = getOwnerNode().getOutputNodes();
                                // BPMN
                                NodeList output = null;
                                JobClass jobClass = job.getJobClass();
                                NodeListWithJobNum outputInfo = forkStrategies[jobClass.getId()]
                                                .getOutNodes(this, jobClass);
                                if (this.isSimplifiedFork) {
                                        output = getOwnerNode().getOutputNodes();
                                        numOut = this.jobsPerLink * output.size();
                                } else {
                                        output = outputInfo.getNodeList();
                                        numOut = outputInfo.getTotalNum();
                                }

                                //soften
//              		JobInfo jii = myRegion.getInputStation().getJobInfoList().lookFor(job);
                                //end

				// Finds number of output jobs
//				numOut = output.size() * jobsPerLink;
				// Resets ack count
				ackCount = 0;

				// Removes job from global node list
				JobInfoList info = getOwnerNode().getJobInfoList();
				JobInfo jobData = info.lookFor(job);
				if (jobData != null) {
					info.remove(jobData);
				}

				// Gets global jobInfoList
				GlobalJobInfoList global = getOwnerNode().getQueueNet().getJobInfoList();

				// Sends "jobsPerLink" jobs on each output link
				Iterator<NetNode> i = output.randomNodes().iterator();
			
                                boolean flag = false;
				while (i.hasNext()) {
                                        NetNode outNode = i.next();
                                        if (!this.isSimplifiedFork) {
                                                for(JobClass c: outputInfo.getClassesWithNum(outNode).keySet()) {
                                                        for (int n = 0; n < outputInfo.getClassesWithNum(outNode).get(c); n++) {
                                                                // ForkJob newJob = new ForkJob(numOut, job, this);
                                                                ForkJob newJob;
                                                                if (forkStrategies[jobClass.getId()] instanceof ClassSwitchFork) {
                                                                        newJob = new ForkJob(numOut, job, this, c, outputInfo.getClassesWithNum(outNode).get(c));
                                                                } else {
                                                                        newJob = new ForkJob(numOut, job, this);
                                                                }
                                                                //soften
                                                                if(myRegion != null){
                                                                        myRegion.getInputStation().getJobInfoList().add(new JobInfo(newJob));
                                                                        if(flag){
                                                                                myRegion.increaseOccupation(job.getJobClass());
                                                                        }
                                                                        flag = true;
                                                                }
                                                                //end

                                                                // Sends new job to all following stations
                                                                send(newJob, 0.0, outNode);
                                                                // Adds job to system jobinfolist
                //						global.addForkedJob(newJob);
                                                        }
                                                        NetNode refNode = getOwnerNode().getQueueNet().getNode(c.getReferenceNodeName());
                                                        Boolean isRefStationFork = (refNode != null) ? (refNode.getSection(NodeSection.OUTPUT) instanceof Fork) : false;
                                                        if (((c.getReferenceNodeName().equals(STATION_TYPE_FORK)) || isRefStationFork) && !hasBeenClass(c, job)) {
                                                                if (switchedClasses != null) {
                                                                        switchedClasses[c.getId()]++;
                                                                } else {
                                                                        switchedClasses = new int[getJobClasses().size()];
                                                                }
                                                                totalSwitchedClasses++;
                                                                job.addClass(c);
                                                        }
                                                }
                                        } else {
                                                for(int n = 0; n < jobsPerLink; n++){
                                                        ForkJob newJob = new ForkJob(numOut, job, this);
                                                        //soften

                                                        if(myRegion != null){
                                                                myRegion.getInputStation().getJobInfoList().add(new JobInfo(newJob));
                                                                if(flag){
                                                                        myRegion.increaseOccupation(job.getJobClass());
                                                                }
                                                                flag = true;
                                                        }
                                                        //end
                                                        // Sends new job to all following stations
                                                        send(newJob, 0.0, outNode);
                                                        // Adds job to system jobinfolist
        //						global.addForkedJob(newJob);
                                                }
                                        }

                                }
                                //soften
        //			if(myRegion != null){
        //				myRegion.decreaseOccupation(job.getJobClass());
        //			}
                                //end
                                // Increment job counter
                                jobNum++;
                                //if the jobs are blocked in the fork section,
                                //send the fake msgs to the former nodes
                                if(numOut == 0){
                                        sendBackward(NetEvent.EVENT_ACK, job, 0.0);
                                        global.dropJob(job);
                                        jobNum--;
        //				sendBackward(NetEvent.EVENT_ACK, job, 0.0);
                                }

                                // Removes original job, otherwise we will have one extra job in the
                                // network
        //			global.removeForkedJob(job);

                                //soften
                                //remove from the blocking region input node
                                if(myRegion != null){
                                JobInfo ji = myRegion.getInputStation().getJobInfoList().lookFor(job);
                                myRegion.getInputStation().getJobInfoList().remove(ji);
                                }
                                //end


                                break;

			case NetEvent.EVENT_ACK:
				ackCount++;
				// If this fork does not block, sends ACK back when all messages are delivered
				// to unlock this fork
				if ((jobNum < block || block <= 0) && ackCount == numOut) {
					sendBackward(NetEvent.EVENT_ACK, job, 0.0);
				}
				break;
			case NetEvent.EVENT_JOIN:
				// If this fork blocks, finally sends ACK back and unlocks this
                                Job j = (Job)message.getData();
			
                                send(NetEvent.EVENT_JOIN, j, 0.0, getOwnerNode().getSection(NodeSection.INPUT).getSectionID());

				if (jobNum == block) {
					sendBackward(NetEvent.EVENT_ACK, job, 0.0);
				}

				// Decrement job counter
				jobNum--;
				break;
		}
		// Everything was okay
		return MSG_PROCESSED;
	}
        
        private boolean hasBeenClass(JobClass c, Job j){
                if(j instanceof ForkJob)
                        return hasBeenClass(c, ((ForkJob) j).getForkedJob());
                return j.hasBeenClass(c);
        } 
	
	//soften	
	private boolean redirectionON;
	private BlockingRegion myRegion;
	private NetNode regionInputStation;
		
	//soften
	public void addRegion(BlockingRegion region){
		redirectionON = true;
		myRegion = region;
		regionInputStation = myRegion.getInputStation();
	}
	//end
        
        /**
	 * Gets the count of the switched jobs of the specified class
	 * 
	 * @param jobClass the job class 
	 * @return the count of the switched jobs of the specified class
	 */
	public int getSwitchedJobCount(int jobClass){
		if (switchedClasses != null) {
                        return switchedClasses[jobClass];
                } else {
                        return 0;
                }
	}
        
        /**
	 * Gets total count of the switched jobs 
	 * 
	 * @return total count of the switched jobs 
	 */
	public int getTotalSwitchedJobs() {
		return totalSwitchedClasses;
	}
}
