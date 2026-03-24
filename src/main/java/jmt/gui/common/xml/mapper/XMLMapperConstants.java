package jmt.gui.common.xml.mapper;

public interface XMLMapperConstants {

    // --- XML Parameter Names (Queue, Server, Delay) ---
    String PARAM_QUEUE_PUT_STRATEGY = "QueuePutStrategy";
    String PARAM_FCFS_STRATEGY = "FCFSstrategy";
    String PARAM_POLLING_K_VALUE = "pollingKValue";
    String PARAM_SIZE = "size";
    String PARAM_DROP_STRATEGIES = "dropStrategies";
    String PARAM_RETRIAL_DISTRIBUTIONS = "retrialDistributions";
    String PARAM_IMPATIENCE = "Impatience";
    String PARAM_MAX_JOBS = "maxJobs";
    String PARAM_MAX_RUNNING = "maxRunning";
    String PARAM_SERVICE_WEIGHTS = "serviceWeights";
    String PARAM_PS_PRIORITY_USED = "PSPriorityUsed";
    String PARAM_SERVER_TYPES_COMPATIBILITIES = "serverTypesCompatibilities";
    String PARAM_SERVICE_STRATEGY = "ServiceStrategy";

    // --- XML Parameter Names (Petri Nets) ---
    String PARAM_ENABLING_CONDITIONS = "enablingConditions";
    String PARAM_INHIBITING_CONDITIONS = "inhibitingConditions";
    String PARAM_RESOURCE_CONDITIONS = "resourceConditions";
    String PARAM_MODE_NAMES = "modeNames";
    String PARAM_NUMBERS_OF_SERVERS = "numbersOfServers";
    String PARAM_TIMING_STRATEGIES = "timingStrategies";
    String PARAM_FIRING_PRIORITIES = "firingPriorities";
    String PARAM_FIRING_WEIGHTS = "firingWeights";
    String PARAM_FIRING_OUTCOMES = "firingOutcomes";

    // --- XML Parameter Names (Fork, Join, Storage, etc.) ---
    String PARAM_JOBS_PER_LINK = "jobsPerLink";
    String PARAM_BLOCK = "block";
    String PARAM_IS_SIMPLIFIED_FORK = "isSimplifiedFork";
    String PARAM_FORK_STRATEGY = "ForkStrategy";
    String PARAM_NUM_REQUIRED = "numRequired";
    String PARAM_CLASSES = "Classes";
    String PARAM_NUMBERS = "Numbers";
    String PARAM_THRESHOLD = "threshold";
    String PARAM_TOTAL_CAPACITY = "totalCapacity";
    String PARAM_CAPACITIES = "capacities";
    String PARAM_DROP_RULES = "dropRules";
    String PARAM_PUT_STRATEGIES = "putStrategies";

    // --- Suffixes and Strategy Names (Queue & Server) ---
    String STRAT_TAIL = "Tail";
    String STRAT_HEAD = "Head";
    String STRAT_RAND = "Rand";
    String STRAT_SJF = "SJF";
    String STRAT_LJF = "LJF";
    String STRAT_SEPT = "SEPT";
    String STRAT_LEPT = "LEPT";
    String STRAT_FCFSPR = "FCFSPR";
    String STRAT_LCFSPR = "LCFSPR";
    String STRAT_SRPT = "SRPT";
    String STRAT_EDD = "EDD";
    String STRAT_EDF = "EDF";
    String STRAT_TBS = "TBS";
    String STRAT_PRIORITY = "Priority";
    String STRAT_PR = "PR";

    String STRAT_LIMITED_POLLING = "LimitedPollingGetStrategy";
    String STRAT_GATED_POLLING = "GatedPollingGetStrategy";
    String STRAT_EXHAUSTIVE_POLLING = "ExhaustivePollingGetStrategy";

    String STRAT_EPS = "EPS";
    String STRAT_GPS = "GPS";
    String STRAT_DPS = "DPS";
    String STRAT_QBPS = "QBPS";

    String STRAT_VACATION_IDLE = "VacationWhenIdleStrategy";
    String STRAT_VACATION_BERNOULLI = "BernoulliVacationStrategy";

    // --- Engine Class Paths ---
    String ENGINE_SERVICE_STRATEGY = "jmt.engine.NetStrategies.ServiceStrategy";
    String ENGINE_PS_STRATEGY = "jmt.engine.NetStrategies.PSStrategy";
    String ENGINE_DISTR_CONTAINER = "jmt.engine.random.DistributionContainer";

    // --- XML Drop Rules ---
    String DROP_XML_DROP = "drop";
    String DROP_XML_BAS_BLOCKING = "BAS blocking";
    String DROP_XML_WAITING_QUEUE = "waiting queue";
    String DROP_XML_RETRIAL = "retrial";

    // --- Common Literal Values ---
    String VAL_ZERO = "0";
    String VAL_ONE = "1";

    // --- Additions for Router, Storage and other utilities ---
    String PARAM_EMPIRICAL_ENTRY = "EmpiricalEntry";
    String STRAT_TAIL_STRATEGY = "TailStrategy";
    String STRAT_HEAD_STRATEGY = "HeadStrategy";
    String STRAT_RAND_STRATEGY = "RandStrategy";

    String ENGINE_ROUTING_CLASS_SWITCH = "jmt.engine.NetStrategies.RoutingStrategies.ClassSwitchStrategy";
    String ENGINE_ROUTING_CLASS_SWITCH_GUI = "jmt.engine.NetStrategies.RoutingStrategies.ClassSwitchRoutingStrategy";

}