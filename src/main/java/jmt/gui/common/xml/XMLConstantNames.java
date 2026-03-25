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

package jmt.gui.common.xml;

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 15-lug-2005
 * Time: 11.54.26
 * Modified by Bertoli Marco
 */
public interface XMLConstantNames {

    // TODO the following constants have the same value
    /*
        XML_A_PARAMETER_ARRAY - XML_A_SUBPARAMETER_ARRAY (Value: "array")
        XML_A_PARAMETER_CLASSPATH - XML_A_SUBPARAMETER_CLASSPATH (Value: "classPath")
        XML_DROP - DROP_XML_DROP (Value: "drop")
        PARAM_DROP_RULES - XML_E_DROPRULES (Value: "dropRules")
        XML_A_GROUPCLASS_CLASS - XML_A_CLASSMEMORYCONSTRAINT_CLASS - XML_A_CLASSCONSTRAINT_CLASS - XML_A_CLASSDUEDATE_CLASS - XML_A_CLASSWEIGHT_CLASS - XML_A_CLASSSIZE_CLASS - XML_A_DROPRULE_CLASS (Value: "jobClass")
        XML_A_GROUPCLASS_GROUP - XML_A_GROUPCONSTRAINT_GROUP - XML_A_GROUPMEMORYCONSTRAINT_GROUP (Value: "jobGroup")
        XML_A_GLOBALCONSTRAINT_MAXJOBS - PARAM_MAX_JOBS (Value: "maxJobs")
        XML_A_STATION_NAME - XML_A_MEASURE_NAME - XML_A_CLASS_NAME - XML_A_PARAMETER_NAME - XML_A_ROOT_NAME - XML_A_SUBPARAMETER_NAME - XML_A_REGION_NAME (Value: "name")
        XML_A_CLASS_PRIORITY - STRAT_PRIORITY (Value: "priority")
        XML_E_PARAMETER_REFCLASS - XML_A_CLASSPOPULATION_NAME (Value: "refClass")
        PARAM_SIZE - XML_A_CLASSSIZE_SIZE (Value: "size")
        XML_A_CLASS_SOFT_DEADLINE - XML_A_CLASSDUEDATE_DUEDATE (Value: "softDeadline")
        XML_A_MEASURE_TYPE - XML_A_CLASS_TYPE - XML_A_REGION_TYPE (Value: "type")
        XML_E_PARAMETER_VALUE - XML_E_SUBPARAMETER_VALUE (Value: "value")
     */

    String VAL_ZERO = "0";
    String VAL_ONE = "1";
    String XML_A_MEASURE_ALPHA = "alpha";
    String XML_A_PARAMETER_ARRAY = "array";
    String XML_A_SUBPARAMETER_ARRAY = "array";
    String DROP_XML_BAS_BLOCKING = "BAS blocking";
    String STRAT_VACATION_BERNOULLI = "BernoulliVacationStrategy";
    String PARAM_BLOCK = "block";
    String XML_E_REGION = "blockingRegion";
    String PARAM_CAPACITIES = "capacities";
    String XML_CLASS = "class";
    String XML_E_CLASSCONSTRAINT = "classConstraint";
    String PARAM_CLASSES = "Classes";
    String XML_E_CLASSMEMORYCONSTRAINT = "classMemoryConstraint";
    String XML_A_STATION_SECTION_CLASSNAME = "className";
    String XML_A_PARAMETER_CLASSPATH = "classPath";
    String XML_A_SUBPARAMETER_CLASSPATH = "classPath";
    String XML_E_CLASSPOPULATION = "classPopulation";
    String XML_E_CLASSSIZE = "classSize";
    String XML_E_CLASSDUEDATE = "classSoftDeadline";
    String XML_E_STATION_DUE_DATES = "classSoftDeadlines";
    String CLASSNAME_CLASSSWITCH = "ClassSwitch";
    String XML_E_CLASSWEIGHT = "classWeight";
    String XML_E_CONNECTION = "connection";
    String XML_A_CLASS_CUSTOMERS = "customers";
    String CLASSNAME_DELAY = "Delay";
    String XML_E_STATION_DELAY_OFF = "delayOff";
    String XML_A_ROOT_DISABLESTATISTIC = "disableStatisticStop";
    String STRAT_DPS = "DPS";
    String XML_DROP = "drop";
    String DROP_XML_DROP = "drop";
    String PARAM_DROP_RULES = "dropRules";
    String XML_E_DROPRULES = "dropRules";
    String PARAM_DROP_STRATEGIES = "dropStrategies";
    String XML_A_DROPRULE_DROP = "dropThisClass";
    String XML_A_CLASS_DUE_DATE = "dueDate";
    String STRAT_EDD = "EDD";
    String STRAT_EDF = "EDF";
    String PARAM_EMPIRICAL_ENTRY = "EmpiricalEntry";
    String CLASSNAME_ENABLING = "Enabling";
    String PARAM_ENABLING_CONDITIONS = "enablingConditions";
    String CLASSNAME_ENABLING_SERVER = "EnablingServer";
    String STRAT_EPS = "EPS";
    String STRAT_EXHAUSTIVE_POLLING = "ExhaustivePollingGetStrategy";
    String CLASSNAME_EXHAUSTIVEPSERVER = "ExhaustivePollingServer";
    String STRAT_FCFSPR = "FCFSPR";
    String PARAM_FCFS_STRATEGY = "FCFSstrategy";
    String CLASSNAME_FIRING = "Firing";
    String PARAM_FIRING_OUTCOMES = "firingOutcomes";
    String PARAM_FIRING_PRIORITIES = "firingPriorities";
    String PARAM_FIRING_WEIGHTS = "firingWeights";
    String CLASSNAME_FORK = "Fork";
    String PARAM_FORK_STRATEGY = "ForkStrategy";
    String STRAT_GATED_POLLING = "GatedPollingGetStrategy";
    String CLASSNAME_GATEDPSERVER = "GatedPollingServer";
    String XML_E_GLOBALCONSTRAINT = "globalConstraint";
    String XML_E_GLOBALMEMORYCONSTRAINT = "globalMemoryConstraint";
    String STRAT_GPS = "GPS";
    String XML_GROUP = "group";
    String XML_E_GROUPCLASS = "groupClass";
    String XML_E_GROUPCLASSLIST = "groupClassList";
    String XML_E_GROUPCONSTRAINT = "groupConstraint";
    String XML_E_GROUPMEMORYCONSTRAINT = "groupMemoryConstraint";
    String STRAT_HEAD = "Head";
    String STRAT_HEAD_STRATEGY = "HeadStrategy";
    String VALIDATION_DYNAMIC_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";
    String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";
    String EXTERNAL_SCHEMA_LOCATION_PROPERTY_ID = "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";
    String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
    String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";
    String PARAM_IMPATIENCE = "Impatience";
    String PARAM_INHIBITING_CONDITIONS = "inhibitingConditions";
    String XML_A_MEASURE_ISSERVERTYPE = "isServerType";
    String PARAM_IS_SIMPLIFIED_FORK = "isSimplifiedFork";
    String ENGINE_PS_STRATEGY = "jmt.engine.NetStrategies.PSStrategy";
    String ENGINE_ROUTING_CLASS_SWITCH_GUI = "jmt.engine.NetStrategies.RoutingStrategies.ClassSwitchRoutingStrategy";
    String ENGINE_ROUTING_CLASS_SWITCH = "jmt.engine.NetStrategies.RoutingStrategies.ClassSwitchStrategy";
    String ENGINE_SERVICE_STRATEGY = "jmt.engine.NetStrategies.ServiceStrategy";
    String ENGINE_DISTR_CONTAINER = "jmt.engine.random.DistributionContainer";
    String XML_A_GROUPCLASS_CLASS = "jobClass";
    String XML_A_CLASSMEMORYCONSTRAINT_CLASS = "jobClass";
    String XML_A_CLASSCONSTRAINT_CLASS = "jobClass";
    String XML_A_CLASSDUEDATE_CLASS = "jobClass";
    String XML_A_CLASSWEIGHT_CLASS = "jobClass";
    String XML_A_CLASSSIZE_CLASS = "jobClass";
    String XML_A_DROPRULE_CLASS = "jobClass";
    String XML_A_GROUPCLASS_GROUP = "jobGroup";
    String XML_A_GROUPCONSTRAINT_GROUP = "jobGroup";
    String XML_A_GROUPMEMORYCONSTRAINT_GROUP = "jobGroup";
    String CLASSNAME_SINK = "JobSink";
    String PARAM_JOBS_PER_LINK = "jobsPerLink";
    String CLASSNAME_JOIN = "Join";
    String STRAT_LCFSPR = "LCFSPR";
    String STRAT_LEPT = "LEPT";
    String STRAT_LIMITED_POLLING = "LimitedPollingGetStrategy";
    String CLASSNAME_LKPSERVER = "LimitedPollingServer";
    String CLASSNAME_LINKAGE = "Linkage";
    String STRAT_LJF = "LJF";
    String XML_A_ROOT_LOGDECIMALSEPARATOR = "logDecimalSeparator"; /* MF08 0.7.4 - delimiter character in log */
    String XML_A_ROOT_LOGDELIM = "logDelimiter"; /* MF08 0.7.4 - delimiter character in log */
    String XML_LOG_B_EXECTIMESTAMP = "logExecTimestamp";
    String XML_A_MEASURE_LOG = "logFile";
    String XML_LOG_FILENAME = "logfileName";
    String XML_LOG_FILEPATH = "logfilePath";
    String XML_LOG_B_JOBCLASS = "logJobClass";
    String XML_LOG_B_JOBID = "logJobID";
    String XML_LOG_B_LOGGERNAME = "logLoggerName";
    String XML_A_ROOT_LOGPATH = "logPath"; /* MF08 0.7.4 - path of logs */
    String XML_A_ROOT_LOGREPLACE = "logReplaceMode"; /* MF08 0.7.4 - replacement mode */
    String XML_LOG_B_TIMEANYCLS = "logTimeAnyClass";
    String XML_LOG_B_TIMESAMECLS = "logTimeSameClass";
    String XML_LOG_B_TIMESTAMP = "logTimeStamp";
    String CLASSNAME_LOGGER = "LogTunnel"; /* MF08 0.7.4 - extends ServiceTunnel */
    String XML_A_PARAMETER_MAP = "map";
    String XML_A_ROOT_MAXEVENTS = "maxEvents";
    String XML_A_GLOBALCONSTRAINT_MAXJOBS = "maxJobs";
    String PARAM_MAX_JOBS = "maxJobs";
    String XML_A_CLASSCONSTRAINT_MAXJOBS = "maxJobsPerClass";
    String XML_A_GROUPCONSTRAINT_MAXJOBS = "maxJobsPerGroup";
    String XML_A_GLOBALMEMORYCONSTRAINT_MAXMEMORY = "maxMemory";
    String XML_A_CLASSMEMORYCONSTRAINT_MAXMEMORY = "maxMemoryPerClass";
    String XML_A_GROUPMEMORYCONSTRAINT_MAXMEMORY = "maxMemoryPerGroup";
    String PARAM_MAX_RUNNING = "maxRunning";
    String XML_A_ROOT_MAXSAMPLES = "maxSamples";
    String XML_A_ROOT_SIMULATED = "maxSimulated";
    String XML_A_ROOT_DURATION = "maxTime";
    String XML_E_MEASURE = "measure";
    String PARAM_MODE_NAMES = "modeNames";
    String XML_A_STATION_NAME = "name";
    String XML_A_MEASURE_NAME = "name";
    String XML_A_CLASS_NAME = "name";
    String XML_A_PARAMETER_NAME = "name";
    String XML_A_ROOT_NAME = "name";
    String XML_A_SUBPARAMETER_NAME = "name";
    String XML_A_REGION_NAME = "name";
    String XML_NODE = "node";
    String XML_A_REGIONNODE_NAME = "nodeName";
    String XML_A_MEASURE_NODETYPE = "nodeType";
    String XML_A_MEASURE_NUMOFSERVERTYPES = "numberOfServerTypes";
    String PARAM_NUMBERS = "Numbers";
    String PARAM_NUMBERS_OF_SERVERS = "numbersOfServers";
    String PARAM_NUM_REQUIRED = "numRequired";
    String XML_E_PARAMETER = "parameter";
    String XML_A_MEASURE_PARENTSTATION = "parentStation";
    String XML_A_ROOT_POLLING = "polling";
    String PARAM_POLLING_K_VALUE = "pollingKValue";
    String XML_A_CLASSPOPULATION_POPULATION = "population";
    String STRAT_PR = "PR";
    String XML_A_MEASURE_PRECISION = "precision";
    String CLASSNAME_PREEMPTIVESERVER = "PreemptiveServer";
    String XML_E_PRELOAD = "preload";
    String XML_A_CLASS_PRIORITY = "priority";
    String STRAT_PRIORITY = "Priority";
    String XML_A_STATION_SECTION_PS_PRIORITY = "psPriority";
    String PARAM_PS_PRIORITY_USED = "PSPriorityUsed";
    String CLASSNAME_PSSERVER = "PSServer";
    String CLASSNAME_PSSERVER_PRIORITY = "PSServerPriority";
    String PARAM_PUT_STRATEGIES = "putStrategies";
    String STRAT_QBPS = "QBPS";
    String XML_E_STATION_QUANTUM_SIZE = "quantumSize";
    String XML_E_STATION_QUANTUM_SWITCHOVER_TIME = "quantumSwitchoverTime";
    String CLASSNAME_QUEUE = "Queue";
    String PARAM_QUEUE_PUT_STRATEGY = "QueuePutStrategy";
    String STRAT_RAND = "Rand";
    String CLASSNAME_SOURCE = "RandomSource";
    String STRAT_RAND_STRATEGY = "RandStrategy";
    String XML_E_PARAMETER_REFCLASS = "refClass";
    String XML_A_CLASSPOPULATION_NAME = "refClass";
    String XML_A_MEASURE_STATION = "referenceNode";
    String XML_A_CLASS_REFSOURCE = "referenceSource";
    String XML_A_MEASURE_CLASS = "referenceUserClass";
    String XML_REFNODE = "refNode";
    String NODETYPE_REGION = "region";
    String XML_E_REGIONNODE = "regionNode";
    String PARAM_RESOURCE_CONDITIONS = "resourceConditions";
    String DROP_XML_RETRIAL = "retrial";
    String PARAM_RETRIAL_DISTRIBUTIONS = "retrialDistributions";
    String CLASSNAME_ROUTER = "Router";
    String XML_E_STATION_SECTION = "section";
    String XML_A_ROOT_SEED = "seed";
    String CLASSNAME_SEMAPHORE = "Semaphore";
    String STRAT_SEPT = "SEPT";
    String CLASSNAME_SERVER = "Server";
    String XML_A_MEASURE_SERVERTYPE = "serverType";
    String XML_E_SERVER_TYPE = "serverTypes";
    String PARAM_SERVER_TYPES_COMPATIBILITIES = "serverTypesCompatibilities";
    String PARAM_SERVICE_STRATEGY = "ServiceStrategy";
    String CLASSNAME_TUNNEL = "ServiceTunnel";
    String PARAM_SERVICE_WEIGHTS = "serviceWeights";
    String XML_E_STATION_SET_UP = "setUp";
    String XML_DOCUMENT_ROOT = "sim";
    String XML_DOCUMENT_XSD = "SIMmodeldefinition.xsd";
    String PARAM_SIZE = "size";
    String XML_A_CLASSSIZE_SIZE = "size";
    String STRAT_SJF = "SJF";
    String XML_A_CLASS_SOFT_DEADLINE = "softDeadline";
    String XML_A_CLASSDUEDATE_DUEDATE = "softDeadline";
    String XML_A_CONNECTION_SOURCE = "source";
    String STRAT_SRPT = "SRPT";
    String NODETYPE_STATION = "station";
    String XML_A_PRELOADSTATION_NAME = "stationName";
    String XML_E_STATIONPOPULATIONS = "stationPopulations";
    String CLASSNAME_STORAGE = "Storage";
    String XML_E_SUBPARAMETER = "subParameter";
    String STRAT_TAIL = "Tail";
    String STRAT_TAIL_STRATEGY = "TailStrategy";
    String XML_A_CONNECTION_TARGET = "target";
    String STRAT_TBS = "TBS";
    String XML_E_MAPREDUCE = "template_mapreduce";
    String CLASSNAME_TERMINAL = "Terminal";
    String PARAM_THRESHOLD = "threshold";
    String CLASSNAME_TIMING = "Timing";
    String PARAM_TIMING_STRATEGIES = "timingStrategies";
    String PARAM_TOTAL_CAPACITY = "totalCapacity";
    String XML_A_MEASURE_TYPE = "type";
    String XML_A_CLASS_TYPE = "type";
    String XML_A_REGION_TYPE = "type";
    String XML_E_CLASS = "userClass";
    String STRAT_VACATION_IDLE = "VacationWhenIdleStrategy";
    String XML_E_PARAMETER_VALUE = "value";
    String XML_E_SUBPARAMETER_VALUE = "value";
    String XML_A_MEASURE_VERBOSE = "verbose";
    String DROP_XML_WAITING_QUEUE = "waiting queue";
    String XML_A_CLASSWEIGHT_WEIGHT = "weight";
}
