package QueueNet;

/**
 * Central repository for simulation engine default values that were previously
 * scattered as magic numbers across multiple classes.
 *
 * Reference each constant from the class that originally owned the literal so
 * that future tuning only requires a change in one place.
 */
public final class SimulationDefaults {

    private SimulationDefaults() {}

    // ── Statistical analysis ──────────────────────────────────────────────────

    /** Maximum number of samples collected per measure before the simulation stops. */
    public static final int MAX_SAMPLES = 1_000_000;

    /** Number of samples between convergence checks in the dynamic analyser. */
    public static final int CONVERGENCE_CHECK_INTERVAL = 500;

    /** Maximum number of samples used by the rule-based stopping criterion. */
    public static final int MAX_RULE_SAMPLES = 5_000;

    /** Initial number of batches used by the batch-means confidence-interval estimator. */
    public static final int INITIAL_NUM_BATCHES = 128;

    // ── Engine / scheduling ───────────────────────────────────────────────────

    /**
     * Refresh period (in simulation ticks) at which the NetController polls
     * each node for intermediate statistics updates.
     */
    public static final int ANIMATION_REFRESH_PERIOD = 12_000;

    /**
     * Initial capacity of the hybrid and timing event queues.
     * 111 is a small prime chosen to minimise hash collisions at startup.
     */
    public static final int EVENT_QUEUE_INITIAL_CAPACITY = 111;

    // ── Load-dependent strategy ───────────────────────────────────────────────

    /** Number of entries in the load-dependent service-time parameter cache. */
    public static final int LD_STRATEGY_CACHE_SIZE = 1_024;

    // ── Parametric-analysis UI ────────────────────────────────────────────────

    /** Time (ms) before the PA progress "About" panel auto-closes. */
    public static final int PA_ABOUT_AUTOCLOSE_MS = 2_500;

    /** Polling interval (ms) for the statistical-outputs loader background thread. */
    public static final int STATS_LOADER_POLL_MS = 500;
}
