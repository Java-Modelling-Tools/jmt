package QueueNet;

/**
 * String constants for node configuration used by engine classes (Server, PSServer, etc.).
 * CommonConstants in jmt-gui extends this interface so GUI code gets these via
 * "import static common.CommonConstants.*" without modification.
 */
public interface EngineNodeConstants {

	String SERVER_FEATURE_PREEMPTIVE_STRATEGIES   = "preemptiveStrategies";
	String SERVER_FEATURE_SWITCHOVER_STRATEGIES   = "SwitchoverStrategy";
	String SERVER_FEATURE_DELAY_OFF_STRATEGIES    = "delayOffTime";
	String SERVER_FEATURE_SETUP_STRATEGIES        = "setUpTime";
	String SERVER_FEATURE_CLASS_PARALLELISM       = "classParallelism";
	String SERVER_FEATURE_SERVER_NAMES            = "serverNames";
	String SERVER_FEATURE_SERVERS_PER_SERVER_TYPE = "serversPerServerType";
	String SERVER_FEATURE_SERVER_COMPATIBILITIES  = "serverCompatibilities";
	String SERVER_FEATURE_SCHEDULING_POLICY       = "schedulingPolicy";
	String SERVER_FEATURE_VACATION_STRATEGY       = "vacationStrategy";

	String STATION_QUEUE_STRATEGY_NON_PREEMPTIVE          = "Non-preemptive Scheduling";
	String STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY = "Non-preemptive Scheduling (Priority)";
	String STATION_QUEUE_STRATEGY_PREEMPTIVE               = "Preemptive Scheduling";
	String STATION_QUEUE_STRATEGY_PREEMPTIVE_PRIORITY      = "Preemptive Scheduling (Priority)";

	String STATION_SCHEDULING_POLICY_ALIS     = "ALIS (Assign Longest Idle Server)";
	String STATION_SCHEDULING_POLICY_ORDER    = "Order (Assign according to order below)";
	String STATION_SCHEDULING_POLICY_FAIRNESS = "Fairness (Move back server type when used)";
	String STATION_SCHEDULING_POLICY_RAIS     = "RAIS (Random Assignment to Idle Servers)";
	String STATION_SCHEDULING_POLICY_ALFS     = "ALFS (Assign Least Flexible Server)";
	String STATION_SCHEDULING_POLICY_FSF      = "FSF (Fastest Servers First)";
}
