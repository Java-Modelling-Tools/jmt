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

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.PSStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NetStrategies.PSStrategies.EPSStrategy;
import jmt.engine.QueueNet.Job;
import jmt.engine.QueueNet.JobClass;
import jmt.engine.QueueNet.JobClassList;
import jmt.engine.QueueNet.JobInfo;
import jmt.engine.QueueNet.NetEvent;
import jmt.engine.QueueNet.NetMessage;
import jmt.engine.QueueNet.NetNode;
import jmt.engine.QueueNet.NetSystem;
import jmt.engine.QueueNet.NodeSection;
import jmt.engine.QueueNet.PSJobInfo;
import jmt.engine.QueueNet.PSJobInfoList;
import jmt.engine.simEngine.RemoveToken;

import static jmt.gui.common.CommonConstants.*;

/**
 * <p><b>Name:</b> PSServer</p>
 * <p><b>Description:</b>
 * This class implements a multi-class processor sharing server.
 * </p>
 * <p><b>Date:</b> 04/ott/2009
 * <b>Time:</b> 13.45.37</p>
 * @author Bertoli Marco
 * @version 1.0
 */
public class PSServerPriority extends Server {

    private int numberOfServers;
    private int maxRunningJobs;
    private ServiceStrategy[] serviceStrategies;
    private PSStrategy[] psStrategies;
    private double[] serviceWeights;

    private int totalNumberOfServers;
    private int[] compatibleServersForClass;

    /** Tells which inner event we are processing */
    private enum PSEvent {
        JOB_IN, JOB_OUT
    }

    private int busyCounter;

    private JobClassList jobClasses;
    private PSJobInfoList psJobsList;
    private double[] serviceFractions;
    private double[][] serviceFractionsPerServerType;
    private int[][] assignedJobs;
    private final HashMap<Integer, Integer> jobAssignments = new HashMap<>();
    private List<ServerType> orderedServerTypes;
    private String schedulingPolicy;

    private RemoveToken messageToken;
    private PSJobInfo messageJobInfo;
    private double messageSendTime;

    public PSServerPriority(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies) throws NetException {
        this(numberOfServers, null, numberOfVisits, serviceStrategies, null, null, null, null, null, null, null);
    }

    public PSServerPriority(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
                    PSStrategy[] psStrategies, Double[] serviceWeights) throws NetException {
        this(numberOfServers, null, numberOfVisits, serviceStrategies, psStrategies, serviceWeights, null, null, null, null, null);
    }

    /**
     * Creates a new instance of PSServer.
     * @param numberOfServers Number of servers which can be shared among jobs.
     * @param maxRunningJobs Number of jobs which can be served simultaneously.
     * @param numberOfVisits Number of job visits per class (not used).
     * @param serviceStrategies Array of service strategies, one per class.
     * @param psStrategies Array of PS strategies, one per class.
     * @param serviceWeights Array of service weights, one per class.
     */
    public PSServerPriority(Integer numberOfServers, Integer maxRunningJobs, Integer[] numberOfVisits,
                    ServiceStrategy[] serviceStrategies, PSStrategy[] psStrategies, Double[] serviceWeights,
                    Integer[] serverNumRequired,
                    String[] serverNames,
                    Integer[] serversPerServerType,
                    Object[] serverCompatibilities,
                    String schedulingPolicy) throws NetException {
        super(numberOfServers,numberOfVisits,serviceStrategies,null,serverNumRequired,serverNames,serversPerServerType,serverCompatibilities,schedulingPolicy);
        this.numberOfServers = numberOfServers.intValue();
        if (maxRunningJobs == null) {
            this.maxRunningJobs = -1;
        } else {
            this.maxRunningJobs = maxRunningJobs.intValue();
        }
        this.serviceStrategies = serviceStrategies;
        if (psStrategies == null) {
            this.psStrategies = new PSStrategy[serviceStrategies.length];
            for (int i = 0; i < serviceStrategies.length; i++) {
                this.psStrategies[i] = new EPSStrategy();
            }
        } else {
            this.psStrategies = psStrategies;
        }
        if (serviceWeights == null) {
            this.serviceWeights = new double[serviceStrategies.length];
            for (int i = 0; i < serviceStrategies.length; i++) {
                this.serviceWeights[i] = 1.0;
            }
        } else {
            this.serviceWeights = new double[serviceWeights.length];
            for (int i = 0; i < serviceWeights.length; i++) {
                this.serviceWeights[i] = serviceWeights[i].doubleValue();
            }
        }
        busyCounter = 0;
        if (schedulingPolicy == null) {
            schedulingPolicy = STATION_SCHEDULING_POLICY_ALIS;
        }
        this.schedulingPolicy = schedulingPolicy;
    }

    @Override
    void initializeServerTypes(Integer[] serverNumRequired, String[] serverNames, Integer[] serversPerServerType, Object[] serverCompatibilities) throws NetException {
        super.initializeServerTypes(serverNumRequired, serverNames, serversPerServerType, serverCompatibilities);
        totalNumberOfServers = 0;
        compatibleServersForClass = new int[serverNumRequired.length];
        for (ServerType serverType: getServerTypes()) {
            totalNumberOfServers += serverType.getNumOfServers();
            for (int i = 0; i < serverNumRequired.length; i++) {
                if (serverType.getCompatibilities()[i]) {
                    compatibleServersForClass[i] += serverType.getNumOfServers();
                }
            }
        }
        assignedJobs = new int[serverNames.length][serverNumRequired.length];
        orderedServerTypes = new ArrayList<>(getServerTypes());
    }

    @Override
    public double getDoubleSectionProperty(int id) throws NetException {
        NetSystem netSystem = getOwnerNode().getNetSystem();
        switch (id) {
            case PROPERTY_ID_UTILIZATION:
                double sum = 0.0;
                for (int i = 0; i < jobClasses.size(); i++) {
                    JobClass jobClass = jobClasses.get(i);
                    sum += psJobsList.size(jobClass) * serviceFractions[jobClass.getId()];
                }
                return sum;
            case PROPERTY_ID_AVERAGE_UTILIZATION:
                return (netSystem.getTime() <= 0.0) ? 0.0
                        : psJobsList.getTotalSojournTime() / netSystem.getTime() / numberOfServers;
            default:
                return super.getDoubleSectionProperty(id);
        }
    }

    @Override
    public double getDoubleSectionProperty(int id, JobClass jobClass) throws NetException {
        NetSystem netSystem = getOwnerNode().getNetSystem();
        switch (id) {
            case PROPERTY_ID_UTILIZATION:
                return psJobsList.size(jobClass) * serviceFractions[jobClass.getId()];
            case PROPERTY_ID_AVERAGE_UTILIZATION:
                return (netSystem.getTime() <= 0.0) ? 0.0
                        : psJobsList.getTotalSojournTimePerClass(jobClass) / netSystem.getTime() / numberOfServers;
            default:
                return super.getDoubleSectionProperty(id, jobClass);
        }
    }

    @Override
    protected void nodeLinked(NetNode node) throws NetException {
        super.nodeLinked(node);
        int stationCapacity = ((Queue) getOwnerNode().getSection(NodeSection.INPUT)).getStationCapacity();
        if (maxRunningJobs <= 0) {
            maxRunningJobs = stationCapacity;
        } else if (stationCapacity <= 0) {
            // Do nothing
        } else {
            maxRunningJobs = Math.min(maxRunningJobs, stationCapacity);
        }
        jobClasses = getJobClasses();
        jobsList = psJobsList = new PSJobInfoList(jobClasses,getClassCompatibilities(),getOwnerNode().getName(), getServerTypes());
        psJobsList.setNetSystem(node.getNetSystem());
        psJobsList.setNumberOfServers(numberOfServers);
        serviceFractions = new double[jobClasses.size()];
        Arrays.fill(serviceFractions, 0.0);
        serviceFractionsPerServerType = new double[getServerTypes().size()][jobClasses.size()];
    }

    @Override
    protected int process(NetMessage message) throws NetException {
        Job job = message.getJob();

        switch (message.getEvent()) {

            case NetEvent.EVENT_START:
                break;

            case NetEvent.EVENT_JOB:
                if (isMine(message)) {
                    performServiceTimes(messageSendTime);
                    handleJobInfoList(messageJobInfo, PSEvent.JOB_OUT);
                    messageToken = null;
                    unassignJob(job);
                    sendForward(job, 0.0);
                } else {
                    if (maxRunningJobs > 0 && busyCounter >= maxRunningJobs) {
                        return MSG_NOT_PROCESSED;
                    }
                    if (messageToken != null) {
                        removeMessage(messageToken);
                        performServiceTimes(messageSendTime);
                        messageToken = null;
                    }
                    // To support LD strategy, update jobInfoList before obtaining serviceTime
                    PSJobInfo jobInfo = new PSJobInfo(job);
                    handleJobInfoList(jobInfo, PSEvent.JOB_IN);
                    assignJob(job);
                    double serviceTime = getServiceStrategiesPerServerType()[jobAssignments.get(job.getId())][job.getJobClass().getId()].wait(this, job.getJobClass());
                    jobInfo.setServiceTime(serviceTime);
                    jobInfo.setResidualServiceTime(serviceTime);
                    busyCounter++;
                    if (maxRunningJobs <= 0 || busyCounter < maxRunningJobs) {
                        sendBackward(NetEvent.EVENT_ACK, job, 0.0);
                    }
                }
                updateServiceFractions();
                serviceJobs();
                break;

            case NetEvent.EVENT_ACK:
                if (busyCounter <= 0) {
                    return MSG_NOT_PROCESSED;
                }
                if (maxRunningJobs > 0 && busyCounter >= maxRunningJobs) {
                    sendBackward(NetEvent.EVENT_ACK, job, 0.0);
                }
                busyCounter--;
                break;

            case NetEvent.EVENT_STOP:
                break;

            default:
                return MSG_NOT_PROCESSED;
        }

        return MSG_PROCESSED;
    }

    private void assignJob(Job job) throws NetException {
        double minJobs = Double.MAX_VALUE;
        int minServer = 0;
        int classId = job.getJobClass().getId();
        for (ServerType serverType : orderedServerTypes) {
            int serverId = serverType.getId();
            if (serverType.getCompatibilities()[classId]) {
                double jobsInServer = 0;
                for (int i = 0; i < jobClasses.size(); i++) {
                    jobsInServer += assignedJobs[serverId][i];
                }
                jobsInServer /= serverType.getNumOfServers();
                if (schedulingPolicy.equals(STATION_SCHEDULING_POLICY_FSF)) {
                    jobsInServer = (jobsInServer + 1) * getServiceStrategiesPerServerType()[serverId][classId].expect(this, job.getJobClass());
                }
                if (jobsInServer < minJobs) {
                    minJobs = jobsInServer;
                    minServer = serverId;
                    if (jobsInServer == 0) {
                        break;
                    }
                }
            }
        }
        assignedJobs[minServer][classId]++;
        jobAssignments.put(job.getId(), minServer);
        if (schedulingPolicy.equals(STATION_SCHEDULING_POLICY_RAIS)) {
            Collections.shuffle(orderedServerTypes);
        } else if (schedulingPolicy.equals(STATION_SCHEDULING_POLICY_ALIS) || schedulingPolicy.equals(STATION_SCHEDULING_POLICY_FAIRNESS)) {
            orderedServerTypes.remove(getServerTypes().get(minServer));
            orderedServerTypes.add(getServerTypes().get(minServer));
        }
    }

    private void unassignJob(Job job) {
        int assignedServer = jobAssignments.get(job.getId());
        jobAssignments.remove(job.getId());
        assignedJobs[assignedServer][job.getJobClass().getId()]--;
    }

    private void performServiceTimes(double startTime) {
        Iterator<JobInfo> it = psJobsList.getInternalJobInfoList().iterator();
        NetSystem netSystem = getOwnerNode().getNetSystem();
        while (it.hasNext()) {
            PSJobInfo jobInfo = (PSJobInfo) it.next();
            int jobClassID = jobInfo.getJob().getJobClass().getId();
            // Perform service for the job
            double elapsedTime = netSystem.getTime() - startTime;
            double serviceTime = elapsedTime * compatibleServersForClass[jobClassID] * serviceFractions[jobClassID];
            jobInfo.performServiceTime(serviceTime);
        }
    }

    private void handleJobInfoList(PSJobInfo jobInfo, PSEvent event) {
        JobClass jobClass = jobInfo.getJob().getJobClass();
        if (event == PSEvent.JOB_IN) {
            psJobsList.psUpdateUtilization(jobClass, serviceFractions);
            psJobsList.psUpdateUtilizationPerServerType(serviceFractionsPerServerType, assignedJobs);
            psJobsList.add(jobInfo);
        } else {
            NetSystem netSystem = getOwnerNode().getNetSystem();
            double elapsedTime = netSystem.getTime() - jobInfo.getEnteringTime();
            double sojournTime = jobInfo.getServiceTime() - jobInfo.getResidualServiceTime();
            double queueTime = elapsedTime - sojournTime;
            if (queueTime < 0.0) {
                queueTime = 0.0;
            }
            psJobsList.psUpdateSojournTime(jobClass, sojournTime);
            psJobsList.psUpdateQueueTime(jobClass, queueTime);
            psJobsList.psUpdateUtilization(jobClass, serviceFractions);
            psJobsList.psUpdateUtilizationPerServerType(serviceFractionsPerServerType, assignedJobs);
            psJobsList.remove(jobInfo);
        }
    }

    private void updateServiceFractions() {
        for (int i = 0; i < jobClasses.size(); i++) {
            serviceFractions[i] = 0;
        }
        for (ServerType serverType : getServerTypes()) {
            Boolean[] compatibilities = serverType.getCompatibilities();
            double numOfServers = serverType.getNumOfServers();
            int serverTypeId = serverType.getId();
            int totalJobsInServer = 0;
            double[] splits = new double[jobClasses.size()];
            for (int i = 0; i < jobClasses.size(); i++) {
                serviceFractionsPerServerType[serverTypeId][i] = 0;
                if (assignedJobs[serverTypeId][i] > 0) {
                    totalJobsInServer += assignedJobs[serverTypeId][i];
                    splits[i] = (double) assignedJobs[serverTypeId][i] / psJobsList.size(jobClasses.get(i));
                }
            }
            if (totalJobsInServer <= numOfServers) {
                for (int i = 0; i < jobClasses.size(); i++) {
                    if (assignedJobs[serverTypeId][i] > 0) {
                        serviceFractionsPerServerType[serverTypeId][i] = 1.0 / numOfServers;
                    }
                }
            } else {
                boolean[] serviceSaturated = new boolean[jobClasses.size()];
                Arrays.fill(serviceSaturated, false);
                double residualCapacity = numOfServers;
                boolean allocationComplete = false;
                while (!allocationComplete) {
                    for (int i = 0; i < jobClasses.size(); i++) {
                        if (!serviceSaturated[i] && splits[i] > 0) {
                            JobClass jobClass = jobClasses.get(i);
                            serviceFractionsPerServerType[serverTypeId][i] = psStrategies[i].slice(psJobsList, jobClasses, serviceWeights, serviceSaturated, jobClass, compatibilities, splits)
                                    * (residualCapacity / numOfServers);
                        }
                    }
                    allocationComplete = true;
                    for (int i = 0; i < jobClasses.size(); i++) {
                        if (serviceFractionsPerServerType[serverTypeId][i] > 1.0 / numOfServers) {
                            JobClass jobClass = jobClasses.get(i);
                            serviceFractionsPerServerType[serverTypeId][i] = 1.0 / numOfServers;
                            residualCapacity -= psJobsList.size(jobClass);
                            serviceSaturated[i] = true;
                            allocationComplete = false;
                        }
                    }
                }
            }
            for (int i = 0; i < jobClasses.size(); i++) {
                serviceFractions[i] += serviceFractionsPerServerType[serverTypeId][i] * splits[i] * numOfServers / compatibleServersForClass[i];
            }
        }
    }

    private void serviceJobs() {
        if (psJobsList.size() > 0) {
            double minWaitTime = Double.MAX_VALUE;
            Iterator<JobInfo> it = psJobsList.getInternalJobInfoList().iterator();
            while (it.hasNext()) {
                PSJobInfo jobInfo = (PSJobInfo) it.next();
                int jobClassID = jobInfo.getJob().getJobClass().getId();
                double waitTime = jobInfo.getResidualServiceTime() / (compatibleServersForClass[jobClassID] * serviceFractions[jobClassID]);
                if (waitTime < minWaitTime) {
                    messageJobInfo = jobInfo;
                    minWaitTime = waitTime;
                }
            }
            messageToken = sendMe(messageJobInfo.getJob(), minWaitTime);
            messageSendTime = getOwnerNode().getNetSystem().getTime();
        }
    }

}
