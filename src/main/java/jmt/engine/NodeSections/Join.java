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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.JoinStrategies.GuardJoin;
import jmt.engine.NetStrategies.JoinStrategies.NormalJoin;
import jmt.engine.NetStrategies.JoinStrategy;
import jmt.engine.QueueNet.BlockingRegion;
import jmt.engine.QueueNet.ForkJob;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.JobInfoList;
import jmt.engine.QueueNet.LinkedJobInfoList;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.dataAnalysis.Measure;

/**
 * <p>Title: Join</p>
 * <p>Description: This class is a Join input section, used to re-assemble previously
 * forked jobs. Jobs are kept until all fragments are collected. If a not-fragmented
 * job is received, it is simply routed to service section.</p>
 *
 * @author Bertoli Marco
 *         Date: 13-mar-2006
 *         Time: 16.19.58
 * 
 * Modified by J. Shuai & M. Cazzoli, implemented join strategies
 */
public class Join extends InputSection {
	/** Data structure used to store received tasks for each job */
	private HashMap<Job, Integer> jobs;
        private HashMap<Job, Integer[]> required;
        private HashMap<Job, Integer> allJobs;
	private HashMap<Job, Boolean> hasFired;
	private HashMap<Job, Integer> counter;
        private HashMap<Job, List<ForkJob>> forkJobs;
        
        private JoinStrategy[] joinStrategies;

	// --- Constructors -----------------------------------------------------------------------------
	/**
	 * Constructs a new Join
	 */
	public Join() {
		// Disables automatic handling of jobinfolists
		super(false);
		jobs = new HashMap<Job, Integer>();
                required = new HashMap<>();
                allJobs = new HashMap<Job, Integer>();
		hasFired = new HashMap<Job, Boolean>();
		counter = new HashMap<Job, Integer>();
                forkJobs = new HashMap<Job, List<ForkJob>>();
	}
        
	public Join(JoinStrategy[] joinStrategies){
		this();
		this.joinStrategies = joinStrategies;
	}

	// ----------------------------------------------------------------------------------------------

	/**
	 * Assembles splitted tasks and sends an EVENT_JOIN to reference fork when done
	 *
	 * @param message message to be processed.
	 * @throws NetException if something goes wrong
	 * @return message processing result.
	 */
	@Override
	protected int process(NetMessage message) throws NetException {
		switch (message.getEvent()) {
			case NetEvent.EVENT_JOB:
				Job job = message.getJob();
				// Sends ACK back
				send(NetEvent.EVENT_ACK, job, 0.0, message.getSourceSection(), message.getSource());
                        
                                JobInfoList info = getOwnerNode().getJobInfoList();
                                JobInfoList section = (JobInfoList) this.getObject(NodeSection.PROPERTY_ID_JOBINFOLIST);

				if (job instanceof ForkJob) {
					ForkJob fJob = (ForkJob) job;
                                        List fJobs = forkJobs.get(fJob.getForkedJob());
                                        if(fJobs == null) {
                                                fJobs = new ArrayList();
                                                forkJobs.put(fJob.getForkedJob(), fJobs);
                                                JobInfo temp = new JobInfo(job);
                                                section.add(temp);
                                                section.remove(temp);
                                        }
                                        fJobs.add(fJob);

					// Removes job from system list
//					GlobalJobInfoList global = getOwnerNode().getQueueNet().getJobInfoList();
//					global.removeForkedJob(fJob);

					// Needed tasks
					int needed;
					int totalForked;
                                        Integer[] neededPerClass = null;
                                        boolean isFired;
                                        if (allJobs.containsKey(fJob.getForkedJob())) {
                                                totalForked = allJobs.get(fJob.getForkedJob());
                                                needed = jobs.get(fJob.getForkedJob());
                                                neededPerClass = required.get(fJob.getForkedJob());
                                                isFired = hasFired.get(fJob.getForkedJob());
                                        } else {
                                                // old
                                                // needed = fJob.getForkedNumber();
                                                // end
                                                // bpmn
                                                totalForked = fJob.getForkedNumber();
                                                needed = joinStrategies[fJob.getForkedJob().getJobClass().getId()].getRequiredNum();
                                                if (joinStrategies[fJob.getForkedJob().getJobClass().getId()] instanceof GuardJoin) {
                                                        neededPerClass = ((GuardJoin) joinStrategies[fJob.getForkedJob().getJobClass().getId()]).getRequiredPerClass();
                                                        needed = -1;
                                                }
                                                required.put(fJob.getForkedJob(), neededPerClass);
                                                isFired = false;

                                                //soften
                                                if (myRegion != null) {
                                                        if (joinStrategies[fJob.getForkedJob().getJobClass().getId()] instanceof NormalJoin) {
                                                                counter.put(fJob.getForkedJob(), fJob.getForkedNumber());
                                                        } else {
                                                                counter.put(fJob.getForkedJob(), joinStrategies[fJob.getForkedJob().getJobClass().getId()].getRequiredNum());
                                                        }
                                                }
                                                //end

                                                // end
                                                // As we are waiting for other fragments, adds merged job to
                                                // global and local info list
        //					JobInfo merged = new JobInfo(fJob.getForkedJob());
        //					info.add(merged);
        //					jobsList.add(merged);
                                        }
                                        // Decrement needed as we received this job
                                        totalForked--;
                                        // If needed is zero, all pieces has been retrived and job can
                                        // be
                                        // forwarded
                                        //soften
                                        if(isFired){
                                                JobInfo i = info.lookFor(fJob);
                                                if (i != null) {
                                                        info.remove(i);
                                                }
                                                if(myRegion != null){
                                                        myRegion.decreaseOccupation(fJob.getJobClass());
                                                }
                                        }
                                        //end
                                        if (!isFired) {
                                                needed--;
                                                boolean isGuardTrue = false;
                                                if (neededPerClass != null) {
                                                        neededPerClass[fJob.getJobClass().getId()]--;
                                                        isGuardTrue = true;
                                                        for(Integer i: neededPerClass) {
                                                                isGuardTrue = isGuardTrue && (i <= 0);
                                                        }
                                                }
                                                jobs.put(fJob.getForkedJob(), needed);
                                                hasFired.put(fJob.getForkedJob(), false);
                                                boolean allArrived = (joinStrategies[fJob.getForkedJob().getJobClass().getId()] instanceof NormalJoin) && (totalForked == 0);
                                                if (needed == 0 || allArrived || isGuardTrue) {
                                                        // Adds original job, otherwise we will have one less
                                                        // job in the network
        //						global.addForkedJob(fJob.getForkedJob());
                                                        for(ForkJob j: forkJobs.get(fJob.getForkedJob())) {
                                                                JobInfo i = info.lookFor(j);
                                                                if (i != null) {
                                                                        info.remove(i);
                                                                }
                                                                if(myRegion != null){
                                                                        JobInfo ji = myRegion.getInputStation().getJobInfoList().lookFor(j);
                                                                        if(ji != null){
                                                                                myRegion.getInputStation().getJobInfoList().remove(ji);
                                                                        }
                                                                }
                                                                //soften
                //							myRegion.getInputStation().getJobInfoList().add(new JobInfo(fJob.getForkedJob()));
                                                                //end
                                                        }
                                                        if(myRegion != null){
                                                                myRegion.getInputStation().getJobInfoList().remove(new JobInfo(fJob.getForkedJob()));
                                                        }
        //                                                if(serviceList != null) {
        //                                                       JobInfo i = serviceList.lookFor(firstJob.get(fJob.getForkedJob())); 
        //                                                       if(i != null) {
        //                                                             serviceList.remove(i);
        //                                                       }
        //                                                }
                                                        // Sends job forward
                                                        sendForward(fJob.getForkedJob(), 0.0);
                                                        hasFired.put(fJob.getForkedJob(), true);
                                                        // Notify fork node (to support blocking)
                                                        send(NetEvent.EVENT_JOIN, fJob.getForkedJob(), 0.0,
                                                                        fJob.getReferenceFork().getSectionID(), fJob
                                                                                        .getReferenceFork().getOwnerNode());
                                                } else {
                                                        if(myRegion != null){
        //							myRegion.decreaseOccupation(fJob.getJobClass());
                                                        }
                                                        // We must wait for more fragments before sending this
                                                        // to
                                                        // next section
                                                        // jobs.put(fJob.getForkedJob(), new Integer(needed));
                                                }
                                        }
                                        if (totalForked == 0){
                                                allJobs.remove(fJob.getForkedJob());
                                                jobs.remove(fJob.getForkedJob());
                                                hasFired.remove(fJob.getForkedJob());
                                                forkJobs.remove(fJob.getForkedJob());
                                                required.remove(fJob.getForkedJob());
                                        } else {
                                                allJobs.put(fJob.getForkedJob(), totalForked);
                                        }
                                } else {
                                        // If this is not a fork job, sends it forward
                                        sendForward(job, 0.0);
                                }

                                break;
			case NetEvent.EVENT_ACK:
				break;
		}

		// Everything was okay
		return MSG_PROCESSED;
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
	
	public int getDeTimes(Job job){
		return this.counter.remove(job);
	}
	
	public BlockingRegion getMyRegion(){
		return this.myRegion;
	}
//	end
        
        public void analyzeUtilization(JobClass jc, Measure measurement, JobClass[] classes){
                JobInfoList info = getOwnerNode().getJobInfoList();
		if(info == null){
			info = new LinkedJobInfoList(classes.length, true);
		}
                ((LinkedJobInfoList) info).analyzeUtilizationJoin(jc, measurement);
	}

}
