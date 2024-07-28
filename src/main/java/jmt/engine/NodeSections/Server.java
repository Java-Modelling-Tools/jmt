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

import java.util.*;

import jmt.common.exception.NetException;
import jmt.engine.NetStrategies.QueuePutStrategy;
import jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy;
import jmt.engine.NetStrategies.ServiceStrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.PreemptiveStrategy;
import jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy;
import jmt.engine.NetStrategies.TransitionUtilities.TimingPacket;
import jmt.engine.QueueNet.*;
import jmt.engine.random.engine.RandomEngine;
import jmt.engine.simEngine.RemoveToken;

import javax.swing.*;

import static jmt.gui.common.CommonConstants.*;


/**
 * This class implements a multi-class, single/multi server service.
 * Every class has a specific distribution and a own set of statistical
 * parameters.
 * A server service remains busy while processing one or more jobs.
 * @author Francesco Radaelli, Stefano Omini, Bertoli Marco
 */
public class Server extends ServiceSection {

	private int numberOfServers;
	private int maxRunningJobs;
	private int originalMaxRunningJobs;
	private int nextMaxRunningJobs;

	private double lastCompletionTime;
	private QueuePutStrategy[] putStrategies;
	private ServiceStrategy[][] switchoverStrategies;
	private ServiceStrategy[] serviceStrategies;
	private ServiceStrategy[][] delayOffStrategies;
	private ServiceStrategy[][] setUpStrategies;
	private int[] serverNumRequired;

	private List<ServerType> serverTypes;
	private Map<Integer,List<BusyServer>> busyServersPerJob;
	private String schedulingPolicy;
	private int[] totalNumOfServers;
	private int[] numberOfAvailableServersPerClass;
	private int[] busyCounterPerClass;
	private int busyCounter;
	private int maxParalellism = 1;
	private HashMap<Job, ServerUnit> busyServerMap;
	private PriorityQueue<ServerUnit> idleServerQueue;
	private ArrayList<ServerSetupUnit> wakeServerQueue;
	private LinkedList<JobInfo> jobsToFire;
	private int savedNumberOfServers;
	private Boolean[][] classCompatibilities;
	private ServiceStrategy[][] serviceStrategiesPerServerType;
	private boolean serverTypesInitialized = false;

	private boolean delayOffEnabled;


	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies) throws NetException {
		this(numberOfServers, numberOfVisits, serviceStrategies, null, null,null,null, null,
				null, null, null, null);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
				  QueuePutStrategy[] preemptiveStrategies) throws NetException {
		this(numberOfServers, numberOfVisits, serviceStrategies, preemptiveStrategies, null,null,null, null,
				null, null, null, null);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
				  Object[] switchoverStrategies,Object[] delayOffStrategies,Object[] setUpStrategies) throws NetException {
		this(numberOfServers, numberOfVisits, serviceStrategies, null, switchoverStrategies, delayOffStrategies,setUpStrategies,null,
				null, null, null, null);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies, Object[] delayOffStrategies, Object[]	 setUpStrategies,
				  Integer[] serverNumRequired, String[] serverTypesNames, Integer[] serverTypesNumOfServers,
				  Object[] serverTypesCompatibilities, String schedulingPolicy ) throws NetException {
		this(numberOfServers, numberOfVisits, serviceStrategies, null, null, delayOffStrategies,setUpStrategies,serverNumRequired,
				serverTypesNames, serverTypesNumOfServers, serverTypesCompatibilities, schedulingPolicy);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
				  Integer[] serverNumRequired, String[] serverTypesNames, Integer[] serverTypesNumOfServers,
				  Object[] serverTypesCompatibilities, String schedulingPolicy ) throws NetException {
		this(numberOfServers, numberOfVisits, serviceStrategies, null, null, null,null,serverNumRequired,
				serverTypesNames, serverTypesNumOfServers, serverTypesCompatibilities, schedulingPolicy);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
				  QueuePutStrategy[] preemptiveStrategies, Integer[] serverNumRequired, String[] serverTypesNames,
				  Integer[] serverTypesNumOfServers, Object[] serverTypesCompatibilities, String schedulingPolicy
	) throws NetException {
		this(numberOfServers, numberOfVisits, serviceStrategies, preemptiveStrategies, null, null,null,serverNumRequired,
				serverTypesNames, serverTypesNumOfServers, serverTypesCompatibilities, schedulingPolicy);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
				  Object[] switchoverStrategies, Object[] delayOffStrategies, Object[] setUpStrategies,Integer[] serverNumRequired, String[] serverTypesNames,
				  Integer[] serverTypesNumOfServers, Object[] serverTypesCompatibilities, String schedulingPolicy
	) throws NetException {
		this(numberOfServers, numberOfVisits, serviceStrategies, null, switchoverStrategies, delayOffStrategies,setUpStrategies,serverNumRequired,
				serverTypesNames, serverTypesNumOfServers, serverTypesCompatibilities, schedulingPolicy);
	}

	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
				  Object[] switchoverStrategies, Integer[] serverNumRequired, String[] serverTypesNames,
				  Integer[] serverTypesNumOfServers, Object[] serverTypesCompatibilities, String schedulingPolicy
	) throws NetException {
		this(numberOfServers, numberOfVisits, serviceStrategies, null, switchoverStrategies, null,null,serverNumRequired,
				serverTypesNames, serverTypesNumOfServers, serverTypesCompatibilities, schedulingPolicy);
	}
	/**
	 * Creates a new instance of Server.
	 * @param numberOfServers Number of jobs which can be served simultaneously.
	 * @param numberOfVisits Number of job visits per class (not used).
	 * @param serviceStrategies Array of service strategies, one per class.
	 * @param preemptiveStrategies Array of preemptive strategies, one per class.
	 * @param switchoverStrategies Matrix of setup strategies, one per class pair.
	 */
	public Server(Integer numberOfServers, Integer[] numberOfVisits, ServiceStrategy[] serviceStrategies,
				  QueuePutStrategy[] preemptiveStrategies, Object[] switchoverStrategies, Object[] delayOffStrategies, Object[] setUpStrategies,Integer[] serverNumRequired,
				  String[] serverNames, Integer[] serversPerServerType, Object[] serverCompatibilities,
				  String schedulingPolicy) throws NetException {
		super(false);
		//Michalis
		if (schedulingPolicy == null) {
			schedulingPolicy = STATION_SCHEDULING_POLICY_ALIS;
		}
		this.schedulingPolicy = schedulingPolicy;
		busyServersPerJob = new HashMap<>();
		serverTypes = new ArrayList<>();

		//
		this.numberOfServers = numberOfServers.intValue();
		this.serviceStrategies = serviceStrategies;
		if (preemptiveStrategies == null) {
			this.putStrategies = new QueuePutStrategy[serviceStrategies.length];
			for (int i = 0; i < serviceStrategies.length; i++) {
				this.putStrategies[i] = new TailStrategy();
			}
		} else {
			this.putStrategies = preemptiveStrategies;
		}
		if (switchoverStrategies == null) {
			this.switchoverStrategies = null;
		} else {
			this.switchoverStrategies = new ServiceStrategy[switchoverStrategies.length][];
			for (int i = 0; i < switchoverStrategies.length; i++) {
				this.switchoverStrategies[i] = (ServiceStrategy[]) switchoverStrategies[i];
			}
		}
		if (delayOffStrategies != null) {
			this.delayOffStrategies = new ServiceStrategy[delayOffStrategies.length][];
			for (int i = 0; i < delayOffStrategies.length; i++) {
				this.delayOffStrategies[i] = (ServiceStrategy[]) delayOffStrategies[i];
			}
			this.delayOffEnabled = true;
		}
		else {
			this.delayOffEnabled = false;
		}

		if (setUpStrategies != null) {
			this.setUpStrategies = new ServiceStrategy[setUpStrategies.length][];
			for (int i = 0; i < setUpStrategies.length; i++) {
				this.setUpStrategies[i] = (ServiceStrategy[]) setUpStrategies[i];
			}
		}

		busyCounter = 0;
		lastCompletionTime = 0.0;

		if (serverCompatibilities != null) {
			initializeServerTypes(serverNumRequired, serverNames, serversPerServerType, serverCompatibilities);
		}
	}

	void initializeServerTypes(Integer[] serverNumRequired, String[] serverNames, Integer[] serversPerServerType,
							   Object[] serverCompatibilities) throws NetException {
		totalNumOfServers = new int[serverCompatibilities.length];
		numberOfAvailableServersPerClass = new int[serverNumRequired.length];
		busyCounterPerClass = new int[serverNumRequired.length];

		classCompatibilities = new Boolean[((Boolean[]) serverCompatibilities[0]).length][serverCompatibilities.length];
		serviceStrategiesPerServerType = new ServiceStrategy[serverCompatibilities.length][((Boolean[]) serverCompatibilities[0]).length];

		for(int i=0;i<serverCompatibilities.length; i++){
			String name = serverNames[i];
			int numOfServers = serversPerServerType[i];
			Boolean[] compatibilities = (Boolean[]) serverCompatibilities[i];

			for(int j=0; j<compatibilities.length; j++){
				classCompatibilities[j][i] = compatibilities[j];
				serviceStrategiesPerServerType[i][j] = serviceStrategies[(i * compatibilities.length + j) % serviceStrategies.length];
			}

			ServerType serverType = new ServerType(name, numOfServers, compatibilities, i);
			serverTypes.add(serverType);
			totalNumOfServers[i] = serverType.numOfServers;

			for(int j=0; j<numberOfAvailableServersPerClass.length; j++){
				if(compatibilities[j]){
					numberOfAvailableServersPerClass[j] += serverType.numOfServers;
				}
			}

		}

		this.serverNumRequired = new int[serverNumRequired.length];
		for (int i = 0; i < serverNumRequired.length; i++) {
			int val = serverNumRequired[i].intValue();
			int numberOfServersForClass = getNumberOfAvailableServersForClass(i);
			if(val > numberOfServersForClass){
				JOptionPane.showMessageDialog(
						new JFrame(),
						"Servers Required for a class are more than the total number of servers",
						"Parameter Error", JOptionPane.ERROR_MESSAGE);
				throw new NetException("Remember: Servers required can't be more than the total number of servers");

			}
			this.serverNumRequired[i] = val;
			maxParalellism = Math.max(maxParalellism, this.serverNumRequired[i]);
		}

		if (schedulingPolicy.equals(STATION_SCHEDULING_POLICY_ALFS)) {
			Collections.sort(serverTypes, new Comparator<ServerType>() {
				@Override
				public int compare(ServerType o1, ServerType o2) {
					Boolean[] c1 = o1.getCompatibilities();
					Boolean[] c2 = o2.getCompatibilities();
					int ctr = 0;
					for (int i = 0; i < c1.length; i++) {
						if (c1[i] && !c2[i]) {
							ctr++;
						} else if (!c1[i] && c2[i]) {
							ctr--;
						}
					}
					return ctr;
				}
			});
		}

		serverTypesInitialized = true;
	}

	@Override
	public Boolean[][] getClassCompatibilities() {
		return classCompatibilities;
	}

	@Override
	public List<ServerType> getServerTypes() {
		return serverTypes;
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
		if (!serverTypesInitialized) {
			int numClasses = getOwnerNode().getJobClasses().size();
			Integer[] serverNumRequired = new Integer[numClasses];
			Arrays.fill(serverNumRequired, 1);
			String[] serverNames = new String[]{"Server 1"};
			Integer[] serversPerServerType = new Integer[]{numberOfServers};
			Object[] serverCompatibilities = new Object[]{new Boolean[numClasses]};
			Arrays.fill((Boolean[]) serverCompatibilities[0], true);
			initializeServerTypes(serverNumRequired, serverNames, serversPerServerType, serverCompatibilities);
		}
		maxRunningJobs = numberOfServers;
		int stationCapacity = ((Queue)getOwnerNode().getSection(NodeSection.INPUT)).getStationCapacity();
		if (stationCapacity > 0) {
			maxRunningJobs = Math.min(maxRunningJobs, stationCapacity);
		}
		originalMaxRunningJobs = maxRunningJobs;
		jobsList.setNumberOfServers(numberOfServers);
		jobsList.setServerNumRequired(serverNumRequired);
		if (switchoverStrategies != null) {
			busyServerMap = new HashMap<Job, ServerUnit>();
			idleServerQueue = new PriorityQueue<ServerUnit>();
		}
		if (setUpStrategies != null) {
			wakeServerQueue = new ArrayList<ServerSetupUnit>();
		}
		jobsToFire = new LinkedList<JobInfo>();
		savedNumberOfServers = 0;
	}

	// Michalis

	public class ServerType{

		private String name;
		private int numOfServers;
		private Boolean[] compatibilities;
		private int busyCnt;
		private int id;

		public ServerType(String name, int numOfServers, Boolean[] compatibilities, int id){
			this.name = name;
			this.numOfServers = numOfServers;
			this.compatibilities = compatibilities;
			this.busyCnt = 0;
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public Boolean[] getCompatibilities() {
			return compatibilities;
		}

		public int getNumOfServers() {
			return numOfServers;
		}

		public String getName() {
			return name;
		}

		public void incrementBusyCounter(int inc){
			busyCnt+=inc;
		}

		public void decrementBusyCounter(int dec){
			busyCnt-=dec;
		}

		public int getBusyCounter() {
			return busyCnt;
		}

		public int getNumberOfFreeServers(){
			return numOfServers - busyCnt;
		}
	}

	public class BusyServer{
		private int numOfBusyServers;
		private Boolean[] compatibilities;
		private int totalServers;
		private int id;
		private String name;

		public BusyServer(int numOfBusyServers, int id, Boolean[] compatibilities, int totalServers,
						  String name){
			this.numOfBusyServers = numOfBusyServers;
			this.id = id;
			this.compatibilities = compatibilities;
			this.totalServers = totalServers;
			this.name = name;
		}

		public int getTotalServers() {
			return totalServers;
		}

		public Boolean[] getCompatibilities() {
			return compatibilities;
		}

		public int getId() {
			return id;
		}

		public int getNumOfBusyServers() {
			return numOfBusyServers;
		}

		public void incrementBusyServers(int inc){
			numOfBusyServers += inc;
		}

		public String getName() {
			return name;
		}
	}
	//

	//Michalis

	private List<ServerType> getCompatibleServers(List<ServerType> availableServers, int classId){

		List<ServerType> compatibleServers = new ArrayList<>();

		for(ServerType serverType: availableServers){
			if(serverType.getCompatibilities()[classId]){
				compatibleServers.add(serverType);
			}
		}

		return compatibleServers;

	}

	private int getNumOfFreeServersForClassFromCompatibleServers(List<ServerType> compatibleServers, int classId){
		int res = 0;
		for(ServerType serverType: compatibleServers){
			if(serverType.getCompatibilities()[classId]){
				res+=serverType.getNumberOfFreeServers();
			}
		}

		return res;
	}

	public JobInfo findNextJobToProcessLCFS(JobInfoList queue){

		List<Integer> classesId = new ArrayList<>();
		for(int i=0; i<serverNumRequired.length; i++){
			classesId.add(i);
		}

		List<ServerType> availableServers = new ArrayList<>();
		for(ServerType serverType: serverTypes){
			availableServers.add(serverType);
		}


		for(int i=queue.size()-1; i>=0; i--){

			JobInfo jobInfo = queue.getJob(i);

			Job job = jobInfo.getJob();

			int classId = job.getJobClass().getId();
			if(classesId.contains(classId)){
				List<ServerType> compatibleServers = getCompatibleServers(availableServers, classId);
				if(getNumOfFreeServersForClassFromCompatibleServers(compatibleServers, classId)
						>= serverNumRequired[classId]){
					return jobInfo;
				}else{
					availableServers.removeAll(compatibleServers);
					classesId.remove(classId);
					if(classesId.size() == 0 || availableServers.size() == 0){
						return queue.getLastJob();
					}
				}
			}
		}
		return queue.getLastJob();
	}

	public JobInfo findNextJobToProcessFCFS(JobInfoList queue){

		List<JobClass> classesId = new ArrayList<>();
		for(int i=0; i<serverNumRequired.length; i++){
			classesId.add(getJobClasses().get(i));
		}

		List<ServerType> availableServers = new ArrayList<>();
		for(ServerType serverType: serverTypes){
			availableServers.add(serverType);
		}


		for(int i=0; i<queue.size(); i++){

			JobInfo jobInfo = queue.getJob(i);

			Job job = jobInfo.getJob();

			int classId = job.getJobClass().getId();
			if(classesId.contains(getJobClasses().get(classId))){
				List<ServerType> compatibleServers = getCompatibleServers(availableServers, classId);
				if(getNumOfFreeServersForClassFromCompatibleServers(compatibleServers, classId)
						>= serverNumRequired[classId]){
					return jobInfo;
				}else{
					classesId.remove(getJobClasses().get(classId));
					if(classesId.size() == 0 || availableServers.size() == 0){
						break;
					}
				}
			}
		}
		return null;
	}

	public int getServerRequired(int class_id){
		return serverNumRequired[class_id];
	}

	private int getNumberOfAvailableServersForClass(int id){
		return numberOfAvailableServersPerClass[id];
	}

	private int getBusyCounterForClass(int id){
		return busyCounterPerClass[id];
	}

	public int getNumOfFreeServersForClass(int classId){
		return getNumberOfAvailableServersForClass(classId) - getBusyCounterForClass(classId);
	}

	private void changeBusyCounters(int change , Boolean[] compatibilities) {
		for (int i = 0; i < busyCounterPerClass.length; i++) {
			if (compatibilities[i]) {
				busyCounterPerClass[i] += change;
			}
		}
	}

	public boolean hasFreeServers() {
		for (ServerType serverType : getServerTypes()) {
			if (serverType.getNumberOfFreeServers() > 0) {
				return true;
			}
		}
		return false;
	}


	private void enterJob(Job job) throws NetException {

		int job_id = job.getId();
		int class_id = job.getJobClass().getId();
		int serversRequired = serverNumRequired[class_id];
		int indx = 0;

		List<BusyServer> busyServers = new ArrayList<>();


		List<ServerType> moveAtTheBack = new ArrayList<>();


		if(schedulingPolicy.equals(STATION_SCHEDULING_POLICY_RAIS)){

			for(int i=0; i<serversRequired; i++){
				List<ServerType> compatibleServers = getCompatibleServers(serverTypes, class_id);
				int freeServers = getNumOfFreeServersForClass(class_id);
				Random rand = new Random();
				int rng = rand.nextInt(freeServers);

				for(int j=0; j<compatibleServers.size(); j++){
					ServerType serverType = compatibleServers.get(j);
					if(serverType.getNumberOfFreeServers() <= rng){
						rng -= serverType.getNumberOfFreeServers();
					}else{
						serverType.incrementBusyCounter(1);
						changeBusyCounters(1, serverType.getCompatibilities());

						boolean addBusyServer = true;

						for (BusyServer busyServer : busyServers) {
							if (busyServer.getId() == serverType.getId()) {
								busyServer.incrementBusyServers(1);
								addBusyServer = false;
								break;
							}
						}

						if(addBusyServer) {
							busyServers.add(new BusyServer(1, serverType.getId(),
									serverType.getCompatibilities(), totalNumOfServers[serverType.getId()],
									serverType.getName()));
						}
						break;
					}
				}
			}

		} else if (schedulingPolicy.equals(STATION_SCHEDULING_POLICY_FSF)) {
			while (serversRequired > 0) {
				double minServiceTime = Double.MAX_VALUE;
				ServerType minServer = serverTypes.get(0);
				for (ServerType serverType : serverTypes) {
					if (serverType.getCompatibilities()[class_id] && serverType.getNumberOfFreeServers() > 0) {
						double expectedServiceTime = serviceStrategiesPerServerType[serverType.getId()][class_id].expect(this, job.getJobClass());
						if (expectedServiceTime < minServiceTime) {
							minServiceTime = expectedServiceTime;
							minServer = serverType;
						}
					}
				}
				int serversUsed = Math.min(serversRequired, minServer.getNumberOfFreeServers());
				minServer.incrementBusyCounter(serversUsed);
				changeBusyCounters(serversUsed, minServer.getCompatibilities());
				serversRequired -= serversUsed;

				boolean addBusyServer = true;

				for (BusyServer busyServer : busyServers) {
					if (busyServer.getId() == minServer.getId()) {
						busyServer.incrementBusyServers(serversUsed);
						addBusyServer = false;
						break;
					}
				}

				if(addBusyServer) {
					busyServers.add(new BusyServer(serversUsed, minServer.getId(),
							minServer.getCompatibilities(), totalNumOfServers[minServer.getId()],
							minServer.getName()));
				}
			}
		} else {
			while (serversRequired > 0) {
				ServerType serverType = serverTypes.get(indx);
				if (serverType.getCompatibilities()[class_id]) {
					int numOfFreeServers = serverType.getNumberOfFreeServers();
					if (numOfFreeServers > 0) {
						int serversUsed = Math.min(serversRequired, numOfFreeServers);
						serverType.incrementBusyCounter(serversUsed);
						changeBusyCounters(serversUsed, serverType.getCompatibilities());
						serversRequired -= serversUsed;

						boolean addBusyServer = true;

						//Fairness
						if (schedulingPolicy.equals(STATION_SCHEDULING_POLICY_FAIRNESS)) {
							moveAtTheBack.add(serverType);
						}
						//
						//ALIS
						if (schedulingPolicy.equals(STATION_SCHEDULING_POLICY_ALIS)) {
							moveAtTheBack.add(serverType);

							for (BusyServer busyServer : busyServers) {
								if (busyServer.getId() == serverType.getId()) {
									busyServer.incrementBusyServers(serversUsed);
									addBusyServer = false;
									break;
								}
							}
						}
						//

						if (addBusyServer) {
							busyServers.add(new BusyServer(serversUsed, serverType.getId(),
									serverType.getCompatibilities(), totalNumOfServers[serverType.getId()],
									serverType.getName()));
						}
					}
				}
				indx++;

			}
		}

		//FAIRNESS
		if(schedulingPolicy.equals(STATION_SCHEDULING_POLICY_FAIRNESS)) {
			for (ServerType serverType : moveAtTheBack) {
				serverTypes.remove(serverType);
				serverTypes.add(serverType);
			}
		}
		//

		//ALIS
		if(schedulingPolicy.equals(STATION_SCHEDULING_POLICY_ALIS)) {
			for (ServerType serverType : moveAtTheBack) {
				if(serverType.getNumberOfFreeServers() == 0) {
					serverTypes.remove(serverType);
					serverTypes.add(serverType);
				}
			}
		}
		//


		// update jobInfoLists
		this.getOwnerNode().getJobInfoList().addUsedServersForJob(job_id, busyServers);
		this.getOwnerNode().getSection(NodeSection.SERVICE).getJobInfoList().addUsedServersForJob(job_id, busyServers);
		this.getOwnerNode().getSection(NodeSection.OUTPUT).getJobInfoList().addUsedServersForJob(job_id, busyServers);

		busyServersPerJob.put(job_id,busyServers);
	}

	private void exitJob(Job job) throws NetException {


		int job_id = job.getId();
		List<BusyServer> busyServers = busyServersPerJob.get(job_id);

		for(BusyServer busyServer: busyServers){
			int numOfBusyServers = busyServer.getNumOfBusyServers();
			for (ServerType serverType : serverTypes) {
				if (serverType.getId() == busyServer.getId()) {
					serverType.decrementBusyCounter(numOfBusyServers);
					break;
				}
			}
			changeBusyCounters(-numOfBusyServers, busyServer.getCompatibilities());
		}

		// update jobInfoLists
		this.getOwnerNode().getJobInfoList().removeUsedServerForJob(job_id);
		this.getOwnerNode().getSection(NodeSection.SERVICE).getJobInfoList().removeUsedServerForJob(job_id);
		this.getOwnerNode().getSection(NodeSection.OUTPUT).getJobInfoList().removeUsedServerForJob(job_id);

		busyServersPerJob.remove(job_id);
	}

	public ServiceStrategy[][] getServiceStrategiesPerServerType() {
		return serviceStrategiesPerServerType;
	}

	//

	@Override
	protected int process(NetMessage message) throws NetException {
		Job job;
		JobInfo jobInfo;
		Object data = message.getData();
		RandomEngine randomEngine = getOwnerNode().getNetSystem().getEngine();
		boolean workingAsTransition = ((Queue)getOwnerNode().getSection(NodeSection.INPUT)).isStationWorkingAsATransition();

		switch (message.getEvent()) {

			case NetEvent.EVENT_START:
				this.getOwnerNode().setServer(this);
				break;

			case NetEvent.EVENT_JOB:
				//EVENT_JOB
				//If the message has been sent by the server itself,
				//then the job is forwarded.
				//
				//If the message has been sent by another section, the server, if
				//is not completely busy, sends to itself a message containing the
				//job and with delay equal to the service time calculated using
				//the service strategy.
				//The counter of jobs in service is increased and, if further service
				//capacity is left, an ack is sent to the input section.

				//gets the job from the message
				job = message.getJob();
				//Number of servers required by a class to process this job
				int num = serverNumRequired[job.getJobClass().getId()];
				int numberOfServersForJob = getNumberOfAvailableServersForClass(job.getJobClass().getId());
				int busyCounterForJob = getBusyCounterForClass(job.getJobClass().getId());
				if(num > numberOfServersForJob){
					JOptionPane.showMessageDialog(
							new JFrame(),
							"Servers Required for " + job.getJobClass().getName() + " are more than the total number of servers",
							"Parameter Error", JOptionPane.ERROR_MESSAGE);
					throw new NetException("Remember: Servers required can't be more than the total number of servers");

				}

				if (isMine(message)) {
					if(workingAsTransition) {
						TimingPacket packet = new TimingPacket(0, 0.0, 1, 1);
						sendBackward(NetEvent.EVENT_TIMING, packet, 0.0);
						jobInfo = jobsList.lookFor(job);
						if(jobInfo == null){
							System.out.println("Problema: jobInfo is null");
						}
						jobsToFire.addLast(jobInfo);
					}else {
						//this job has been just served (the message has been sent by the server itself)
						//forwards the job to the output section
						jobInfo = jobsList.lookFor(job);
						job.setServiceTime(-1.0);
						job.setServiceStartTime(-1.0);
						job.setServingMessage(null);
						jobsList.endJob(jobInfo);
						jobsList.remove(jobInfo);
						if (switchoverStrategies != null) {
							ServerUnit serverUnit = busyServerMap.remove(job);
							idleServerQueue.offer(serverUnit);
						}
						sendForward(job, 0.0);
						NetSystem netSystem = getOwnerNode().getNetSystem();
						lastCompletionTime = netSystem.getTime();
					}
				} else {
					//message received from another node section: if the server is not completely busy,
					//it sends itself a message with this job
					int jobClassID = job.getJobClass().getId();
					if (putStrategies[jobClassID] instanceof PreemptiveStrategy && busyCounterForJob == numberOfServersForJob) {
						preemptJob();
					}

					if (busyCounterForJob + num > numberOfServersForJob) {
						return MSG_NOT_PROCESSED;
					}

					putStrategies[jobClassID].put(job, jobsList, this);
					jobInfo = jobsList.lookFor(job);

					//sends the job with delay equal to "serviceTime"
					//message received from another node section: if the server is not completely busy,
					//it sends itself a message with this job
					NetSystem netSystem = getOwnerNode().getNetSystem();
					if (delayOffEnabled){
						double currentTime = netSystem.getTime();
						if (wakeServerQueue.size() > 0) {
							Iterator<ServerSetupUnit> iterator = wakeServerQueue.iterator();
							while (iterator.hasNext()) {
								ServerSetupUnit obj = iterator.next();
								double delayOffDeadline = obj.lastIdleTime + obj.lastDelayOffTime;
								if (currentTime>=delayOffDeadline) {
									iterator.remove();
//									System.out.println("delay off triggered");
								}
								else {
									if (obj.isSettingUp) {
										obj.checkSetUpComplete(currentTime);
//										System.out.println("setup complete");
									}
								}
							}
						}
					}
					if (switchoverStrategies != null) {
						ServerUnit serverUnit = idleServerQueue.poll();
						if (serverUnit == null) {
							serverUnit = new ServerUnit(busyCounter, jobClassID);
						}
						if (serverUnit.jobClassID == jobClassID) {
							enterJob(job);
							double serviceTime = job.getServiceTime();
							if (serviceTime < 0.0) {
								serviceTime = 0;
								List<BusyServer> busyServers = busyServersPerJob.get(job.getId());
								for (BusyServer server : busyServers) {
									serviceTime += serviceStrategiesPerServerType[server.getId()][jobClassID].wait(this, job.getJobClass());
								}
								serviceTime /= busyServers.size();
								job.setServiceTime(serviceTime);
							}
							RemoveToken jobServingMessage = sendMe(NetEvent.EVENT_JOB, job, serviceTime);
							job.setServiceStartTime(netSystem.getTime());
							job.setServingMessage(jobServingMessage);
							jobsList.startJob(jobInfo);
						} else {
							ServiceStrategy switchoverTimeStrategy = switchoverStrategies[serverUnit.jobClassID][jobClassID];
							double switchoverTime = 0.0;
							//if ((netSystem.getTime() - lastCompletionTime) > 0.0) { // if the server has been idle
							switchoverTime = switchoverTimeStrategy.wait(this, job.getJobClass());
							//}
							RemoveToken jobServingMessage = sendMe(NetEvent.EVENT_SETUP_JOB, job, switchoverTime);
							job.setServingMessage(jobServingMessage);
						}
						busyServerMap.put(job, serverUnit);
					} else {
						enterJob(job);
						double serviceTime = job.getServiceTime();
						if (serviceTime < 0.0) {
							serviceTime = 0;
							List<BusyServer> busyServers = busyServersPerJob.get(job.getId());
							for (BusyServer server : busyServers) {
								serviceTime += serviceStrategiesPerServerType[server.getId()][jobClassID].wait(this, job.getJobClass());
							}
							serviceTime /= busyServers.size();
							if (delayOffEnabled && delayOffStrategies.length > 0){
								double setuptime = 0.0;
								if (wakeServerQueue.size() > 0) {
									ServerSetupUnit minIdleUnit = null;
									for (ServerSetupUnit unit : wakeServerQueue) {
										if (unit.isRunning && !unit.isSettingUp) {
											if (minIdleUnit == null ||unit.lastIdleTime < minIdleUnit.lastIdleTime) {
												minIdleUnit = unit;
											}
										}
									}
									if (minIdleUnit != null) {
//										System.out.println("put to running server");
										minIdleUnit.isRunning = true;
										minIdleUnit.jobs.add(job);
										minIdleUnit.lastIdleTime = netSystem.getTime();
									}
									else{
										System.out.println("intermidiate setup triggered");
										for (ServerSetupUnit unit : wakeServerQueue) {
											if (unit.isSettingUp) {
												if (minIdleUnit == null || unit.lastSettingUpTime < minIdleUnit.lastSettingUpTime) {
													minIdleUnit = unit;
												}
											}
										}
										setuptime = minIdleUnit.lastSettingUpTime - netSystem.getTime();
										serviceTime += setuptime;
										minIdleUnit.jobs.add(job);
										minIdleUnit.lastIdleTime = netSystem.getTime();
									}
								}
								else{
//									System.out.println("setup triggered");
									setuptime = setUpStrategies[jobClassID][0].wait(this, job.getJobClass());
									ServerSetupUnit serverSetupUnit = new ServerSetupUnit(job, jobClassID,setUpStrategies[jobClassID][0],delayOffStrategies[jobClassID][0]);
									serverSetupUnit.isSettingUp = true;
									serverSetupUnit.lastSettingUpTime = getOwnerNode().getNetSystem().getTime()+setuptime;
									serviceTime += setuptime;
									wakeServerQueue.add(serverSetupUnit);
								}
							}
							job.setServiceTime(serviceTime);
						}
						RemoveToken jobServingMessage = sendMe(NetEvent.EVENT_JOB, job, serviceTime);
						job.setServiceStartTime(netSystem.getTime());
						job.setServingMessage(jobServingMessage);
						jobsList.startJob(jobInfo);
					}
				}

				if (hasFreeServers()) {
					//sends an ack to the input section (remember not to propagate
					//this ack again when computation is finished)
					sendBackward(NetEvent.EVENT_ACK, job, 0.0);
				}

				break;

			case NetEvent.EVENT_SETUP_JOB: {
				job = message.getJob();
				jobInfo = jobsList.lookFor(job);
				ServerUnit serverUnit = busyServerMap.get(job);
				int jobClassID = job.getJobClass().getId();
				serverUnit.jobClassID = jobClassID;
				NetSystem netSystem = getOwnerNode().getNetSystem();
				double serviceTime = job.getServiceTime();
				RemoveToken jobServingMessage = sendMe(NetEvent.EVENT_JOB, job, serviceTime);
				job.setServiceStartTime(netSystem.getTime());
				job.setServingMessage(jobServingMessage);
				jobsList.startJob(jobInfo);
				enterJob(job);
				break;
			}


			case NetEvent.EVENT_ACK:
				//EVENT_ACK
				//If there are no jobs in the service section, message is not processed.
				//Otherwise an ack is sent backward to the input section and
				//the counter of jobs in service is decreased.

				Job job2 = message.getJob();
				int jobClassID2 = job2.getJobClass().getId();

				int num2 = serverNumRequired[job2.getJobClass().getId()];
				int numberOfServersForJob2 = getNumberOfAvailableServersForClass(job2.getJobClass().getId());
				int busyCounterForJob2 = getBusyCounterForClass(job2.getJobClass().getId());

				if (!workingAsTransition) {
					if (busyCounterForJob2 <= 0) {
						//it was not waiting for any job
						return MSG_NOT_PROCESSED;
					} else if (busyCounterForJob2 == numberOfServersForJob2 || numberOfServersForJob2 - busyCounterForJob2 <= num2) {
						exitJob(job2);
						// Sends a request to the input section
						if (hasFreeServers())
							sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
					} else {
						// Avoid ACK as we already sent ack
						exitJob(job2);
					}
				} else {
					if (busyCounterForJob2 <= -1) {
						//it was not waiting for any job
						return MSG_NOT_PROCESSED;
					} else if (numberOfServersForJob2 - busyCounterForJob2 > num2) {
						exitJob(job2);
						// Sends a request to the input section
						if(hasFreeServers())
							sendBackward(NetEvent.EVENT_ACK, message.getJob(), 0.0);
					} else {
						// Avoid ACK as we already sent ack
						exitJob(job2);
					}
				}
				if (delayOffEnabled && delayOffStrategies.length > 0) {
					for (ServerSetupUnit unit : wakeServerQueue) {
						if (unit.jobs.contains(job2)) {
							unit.jobs.remove(job2);
							unit.lastIdleTime = getOwnerNode().getNetSystem().getTime();
							if (unit.jobs.size() == 0) {
								double delayOffDuration = unit.delayOffTime.wait(this, job2.getJobClass());
								if (delayOffDuration == 0) {
									delayOffDuration = 0.0001;
								}
								unit.lastDelayOffTime = delayOffDuration;
							}
						}
					}
				}
				break;

			case NetEvent.EVENT_ENABLING:{
				int enablingDegree = (int) message.getData();

				if (enablingDegree < 0) {
					maxRunningJobs = originalMaxRunningJobs;
				}else {
					maxRunningJobs = Math.min(enablingDegree, originalMaxRunningJobs);
				}

				int deltaEvents = maxRunningJobs - busyCounter;

				if (deltaEvents == 0) {
					sendBackward(NetEvent.EVENT_RESET_COOLSTART, false, 0.0);
				}else if(deltaEvents > 0){
					if(savedNumberOfServers < maxRunningJobs) {
						//l'ack viene mandato solo nel caso in cui sia appena aumentato il numero di server disponibili
						sendBackward(NetEvent.EVENT_ACK, null, 0.0);
					}
				}else if(deltaEvents < 0){
					for(int i = deltaEvents; i < 0; i++){
						preemptJob();
					}
					sendBackward(NetEvent.EVENT_RESET_COOLSTART, false, 0.0);
				}

				if(maxRunningJobs != savedNumberOfServers) {
					savedNumberOfServers = maxRunningJobs;
				}
				jobsList.changeNumberOfActiveServers(savedNumberOfServers);

				break;
			}

			case NetEvent.EVENT_FIRING:{
				if(jobsToFire.size() > 0){
					JobInfo ji = jobsToFire.removeFirst();
					if(ji.getJob() == null){
						System.out.println("Problem: ji.getJob() is null");
					}
					Job j = ji.getJob();
					j.setServiceTime(-1.0);
					j.setServiceStartTime(-1.0);
					j.setServingMessage(null);
					jobsList.endJob(ji);
					jobsList.remove(ji);
					if (switchoverStrategies != null) {
						ServerUnit serverUnit = busyServerMap.remove(j);
						idleServerQueue.offer(serverUnit);
					}
					busyCounter--;
					sendForward(j, 0.0);
				}
				break;
			}

			case NetEvent.EVENT_STOP:
				break;

			default:
				return MSG_NOT_PROCESSED;
		}

		return MSG_PROCESSED;
	}

	private void preemptJob() throws NetException{
		JobInfo lastJobInfo = getLastJobInfo();
		Job lastJob = lastJobInfo.getJob();
		RemoveToken lastJobServingMessage = lastJob.getServingMessage();
		removeMessage(lastJobServingMessage);
		lastJob.setServingMessage(null);
		double lastJobStartTime = lastJob.getServiceStartTime();
		if (lastJobStartTime >= 0.0) {
			double lastJobRemainingTime = lastJob.getRemainingServiceTime();
			lastJob.setServiceTime(lastJobRemainingTime);
			lastJob.setServiceStartTime(-1.0);
			jobsList.endJob(lastJobInfo);
		}
		jobsList.remove(lastJobInfo);
		if (switchoverStrategies != null) {
			ServerUnit serverUnit = busyServerMap.remove(lastJob);
			idleServerQueue.add(serverUnit);
		}
		exitJob(lastJob);

		sendBackward(NetEvent.EVENT_PREEMPTED_JOB, lastJob, 0.0);
		busyCounter--;
	}

	/**
	 * Gets the service strategy for each class.
	 * @return the service strategy for each class.
	 */
	public ServiceStrategy[] getServiceStrategies() {
		return serviceStrategies;
	}

	/**
	 * Gets the last job info in the service list.
	 * @return the last job info in the service list.
	 */
	public JobInfo getLastJobInfo() {
		if(jobsList.size() <= 0){
			System.out.println("Problem: jobsList.size() <= 0");
		}
		return jobsList.getInternalJobInfoList().get(jobsList.size() - 1);
	}

	private static class ServerUnit implements Comparable<ServerUnit> {

		public int id;
		public int jobClassID;

		public ServerUnit(int id, int jobClassID) {
			this.id = id;
			this.jobClassID = jobClassID;
		}

		@Override
		public int compareTo(ServerUnit that) {
			if (this.id < that.id) {
				return -1;
			} else if (this.id > that.id) {
				return 1;
			} else {
				return 0;
			}
		}

	}

	private static class ServerSetupUnit implements Comparable<ServerSetupUnit>{
		public ArrayList<Job> jobs;
		public int jobClassID;
		public ServiceStrategy setupTime;
		public ServiceStrategy delayOffTime;

		public boolean isRunning;
		public boolean isSettingUp;
		public double lastSettingUpTime;
		public double lastIdleTime;
		public double lastDelayOffTime;

		public ServerSetupUnit(Job job,int jobClassID, ServiceStrategy setupTime, ServiceStrategy delayOffTime) {
			this.jobs = new ArrayList<>();
			this.jobs.add(job);
			this.jobClassID = jobClassID;
			this.setupTime = setupTime;
			this.delayOffTime = delayOffTime;
			this.isRunning = false;
			this.isSettingUp = false;
			this.lastSettingUpTime = 0.0;
			this.lastIdleTime = 0.0;
			this.lastDelayOffTime = 0.0;
		}

		public void checkSetUpComplete(double currentTime) {
			double settingUpDeadline = lastSettingUpTime;
			if (settingUpDeadline <= currentTime) {
				isSettingUp = false;
				isRunning = true;
			}
		}

		@Override
		public int compareTo(ServerSetupUnit o) {
			return Double.compare(this.lastIdleTime, o.lastIdleTime);
		}
	}
}
