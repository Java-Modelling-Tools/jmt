package log;

/**
 * Lightweight, pre-allocated record representing a simulation event.
 * Reused in-place by the ring buffer to achieve ZERO garbage collection overhead
 * on the simulation hot path.
 */
public final class SimEventRecord {

	public long sequenceNumber;
	public double simTime;
	public double scheduledTime;
	public double delay;
	public String phase; // "SEND", "DISPATCH", "DROP", "NOT_PROCESSED"

	public int eventType;
	public String eventName;

	// Station / Node context
	public String srcNodeName;
	public int srcNodeId;
	public byte srcSection;
	public String srcSectionName;
	public String srcSectionClass; // Java class of sender section: e.g. "Queue", "Server", "Router"

	public String dstNodeName;
	public int dstNodeId;
	public byte dstSection;
	public String dstSectionName;
	public String dstSectionClass; // Java class of destination section

	// Exact Java code caller (class.method:line) if stack recording is enabled
	public String callerCodeLocation;

	// Station state variables at time of event
	public int nodeJobsCount;   // Total jobs inside node (jobsList.size())
	public int queueSize;       // Waiting jobs in queue (-1 if not applicable)
	public int queueCapacity;   // Queue capacity (-1 if infinite/not applicable)
	public int totalServers;    // Total server count (-1 if not applicable)

	// Job / Packet variables
	public int jobId; // -1 if no job
	public String jobClassName;
	public int jobClassId;
	public int jobPriority;
	public double jobSystemEnteringTime;
	public double jobServiceTime;
	public double jobTotalQueueTime;
	public boolean isToken;
	public String lastVisitedNode;

	// Non-job payload variables (if event carries arbitrary data)
	public String payloadType;
	public String payloadValue;

	public String messageText;

	public volatile boolean ready = false;

	public static final String[] SECTION_NAMES = {
			"NO_ADDRESS", // 0
			"INPUT",      // 1
			"SERVICE",    // 2
			"OUTPUT"      // 3
	};

	public static String getSectionName(byte section) {
		if (section >= 0 && section < SECTION_NAMES.length) {
			return SECTION_NAMES[section];
		}
		return String.valueOf(section);
	}

	public void reset() {
		sequenceNumber = 0;
		simTime = 0.0;
		scheduledTime = 0.0;
		delay = 0.0;
		phase = null;
		eventType = -1;
		eventName = null;
		srcNodeName = null;
		srcNodeId = -1;
		srcSection = 0;
		srcSectionName = null;
		srcSectionClass = null;
		dstNodeName = null;
		dstNodeId = -1;
		dstSection = 0;
		dstSectionName = null;
		dstSectionClass = null;
		callerCodeLocation = null;
		nodeJobsCount = -1;
		queueSize = -1;
		queueCapacity = -1;
		totalServers = -1;
		jobId = -1;
		jobClassName = null;
		jobClassId = -1;
		jobPriority = 0;
		jobSystemEnteringTime = 0.0;
		jobServiceTime = -1.0;
		jobTotalQueueTime = 0.0;
		isToken = false;
		lastVisitedNode = null;
		payloadType = null;
		payloadValue = null;
		messageText = null;
		ready = false;
	}
}
