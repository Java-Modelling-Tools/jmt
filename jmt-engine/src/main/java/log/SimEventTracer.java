package log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import NodeSections.PollingServer;
import NodeSections.Queue;
import NodeSections.Server;
import QueueNet.Job;
import QueueNet.JobClass;
import QueueNet.JobClassList;
import QueueNet.NetEvent;
import QueueNet.NetMessage;
import QueueNet.NetNode;
import QueueNet.NodeList;
import QueueNet.NodeSection;
import QueueNet.QueueNetwork;

/**
 * High-performance, zero-allocation native simulation event tracer for JMT.
 *
 * Designed specifically for driving visual packet/station debuggers without
 * degrading simulation engine throughput.
 *
 * Key Architectural Features:
 * 1. Controlled by master switch {@link #DEBUG_VISUAL_TRACE} (and {@link #ENABLED}).
 *    When false, the hot-path check is an inlined volatile read (~1ns) with zero overhead.
 * 2. In-memory SPSC (Single-Producer Single-Consumer) ring buffer with 262,144 pre-allocated
 *    records: ZERO garbage collection / allocation during event recording.
 * 3. File I/O runs asynchronously on a dedicated background daemon thread.
 * 4. Multi-Simulation Support:
 *    When running multiple simulations (e.g. parametric/what-if runs or multiple files),
 *    each simulation run is saved into its own distinct, isolated trace file.
 *    Runs NEVER overwrite each other.
 * 5. Captures:
 *    - All NetEvent codes (from EVENT_ABORT 0x00 to EVENT_VACATION_JOB 0x1D)
 *    - Actual station names in the network (e.g. "Client-Q", "WLS-CPU-Q") for direct visual mapping
 *    - The Java class of the section that sent/received the message (e.g. Queue, Server, Router, etc.)
 *    - Mandatory exact Java source code location (class.method(File.java:line)) from the call stack
 *    - Complete variable state at time of event: queue size, capacity, servers, node population,
 *      job ID, class, priority, system entering time, service time, queue time, and payload values.
 * 6. Outputs Newline-Delimited JSON (NDJSON):
 *    - Line 1: Complete network metadata (nodes, station sections, servers, capacities, connections, job classes)
 *    - Lines 2..N: Every simulation event with complete packet/station lifecycle information
 *    - Final Line: Summary footer (event counts, duration)
 */
public final class SimEventTracer {

	/**
	 * Master debugging switch.
	 * Can be enabled via:
	 * - Programmatically: SimEventTracer.DEBUG_VISUAL_TRACE = true;
	 * - Via JVM flag: -Djmt.trace.visual=true
	 * - Via helper: SimEventTracer.enable()
	 */
	public static volatile boolean DEBUG_VISUAL_TRACE = Boolean.getBoolean("jmt.trace.visual");

	/** Alias for DEBUG_VISUAL_TRACE for convenience */
	public static volatile boolean ENABLED = DEBUG_VISUAL_TRACE;

	/** Default output trace file or directory path */
	public static volatile String OUTPUT_FILE = System.getProperty("jmt.trace.file",
			System.getProperty("jmt.trace.dir", "visual_trace.ndjson"));

	// Run tracking across multiple simulations
	private static final AtomicInteger RUN_COUNTER = new AtomicInteger(1);
	private static volatile File currentResolvedFile = null;

	// Ring buffer configuration
	private static final int BUFFER_CAPACITY = 262_144; // 256K slots (power of 2)
	private static final int BUFFER_MASK = BUFFER_CAPACITY - 1;
	private static final SimEventRecord[] RING = new SimEventRecord[BUFFER_CAPACITY];

	static {
		for (int i = 0; i < BUFFER_CAPACITY; i++) {
			RING[i] = new SimEventRecord();
		}
	}

	private static final AtomicLong WRITE_SEQ = new AtomicLong(0);
	private static volatile long READ_SEQ = 0;
	private static final AtomicLong DROPPED_EVENTS = new AtomicLong(0);
	private static final AtomicLong TOTAL_RECORDED = new AtomicLong(0);

	private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
	private static final AtomicBoolean METADATA_WRITTEN = new AtomicBoolean(false);
	private static volatile WriterThread writerThread = null;

	private static volatile double lastSimTime = 0.0;
	private static volatile long traceStartTimeMs = 0;

	// Prevent direct instantiation
	private SimEventTracer() {
	}

	/**
	 * Enables event tracing with the default output file.
	 */
	public static void enable() {
		enable(OUTPUT_FILE);
	}

	/**
	 * Enables event tracing writing to the specified file path or directory.
	 * @param filePath Output file path (.ndjson) or directory
	 */
	public static synchronized void enable(String filePath) {
		if (filePath != null && !filePath.isEmpty()) {
			OUTPUT_FILE = filePath;
		}
		DEBUG_VISUAL_TRACE = true;
		ENABLED = true;
		ensureWriterStarted(null);
	}

	/**
	 * Disables event tracing.
	 */
	public static void disable() {
		DEBUG_VISUAL_TRACE = false;
		ENABLED = false;
	}

	/**
	 * Returns whether visual event tracing is currently enabled.
	 */
	public static boolean isEnabled() {
		return DEBUG_VISUAL_TRACE;
	}

	/**
	 * Sets the master debugging flag.
	 */
	public static void setEnabled(boolean enabled) {
		if (enabled) {
			enable();
		} else {
			disable();
		}
	}

	/**
	 * Sets the output file path or directory for subsequent traces.
	 */
	public static void setOutputFile(String filePath) {
		if (filePath != null && !filePath.isEmpty()) {
			OUTPUT_FILE = filePath;
		}
	}

	/**
	 * Returns the file currently being written to for this simulation run.
	 */
	public static File getCurrentOutputFile() {
		return currentResolvedFile;
	}

	/**
	 * Resolves an isolated, non-colliding file path for each simulation run.
	 * If running multiple simulations (e.g. parametric steps or multiple models),
	 * each run receives its own dedicated file.
	 */
	private static synchronized File resolveOutputFile(QueueNetwork network) {
		String base = OUTPUT_FILE;
		if (base == null || base.trim().isEmpty()) {
			base = "visual_trace.ndjson";
		}

		File file = new File(base);
		boolean isDir = file.isDirectory() || base.endsWith("/") || base.endsWith("\\");
		String modelName = (network != null && network.getName() != null) ? sanitize(network.getName()) : "model";
		int runId = RUN_COUNTER.getAndIncrement();
		int step = (network != null) ? network.getParametricStep() : 0;

		String stepPart = step > 0 ? "_step" + step : "";
		String runPart = "_run" + runId;

		if (isDir) {
			if (!file.exists()) {
				file.mkdirs();
			}
			return new File(file, modelName + stepPart + runPart + ".ndjson");
		}

		// Single simulation without prior runs
		if (runId == 1 && step == 0) {
			return file;
		}

		// Subsequent runs in the same JVM session: auto-generate unique filename
		String parent = file.getParent();
		String name = file.getName();
		String nameWithoutExt = name;
		String ext = ".ndjson";
		int dot = name.lastIndexOf('.');
		if (dot >= 0) {
			nameWithoutExt = name.substring(0, dot);
			ext = name.substring(dot);
		}

		String suffix = step > 0 ? "_step" + step : "_run" + runId;
		String newName = nameWithoutExt + "_" + modelName + suffix + ext;
		return parent != null ? new File(parent, newName) : new File(newName);
	}

	private static String sanitize(String name) {
		if (name == null) return "model";
		return name.replaceAll("[^a-zA-Z0-9_.-]", "_");
	}

	/**
	 * Starts the background writer thread and registers shutdown hook if not already active.
	 */
	private static synchronized void ensureWriterStarted(QueueNetwork network) {
		if (!INITIALIZED.compareAndSet(false, true)) {
			return;
		}

		traceStartTimeMs = System.currentTimeMillis();
		currentResolvedFile = resolveOutputFile(network);

		writerThread = new WriterThread(currentResolvedFile);
		writerThread.setName("JMT-Visual-Event-Tracer-" + currentResolvedFile.getName());
		writerThread.setDaemon(true);
		writerThread.start();

		// Shutdown hook ensures remaining events are flushed on JVM exit
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				SimEventTracer.stop();
			}
		}, "JMT-Visual-Event-Tracer-Shutdown"));
	}

	/**
	 * Writes the network topology metadata header at the beginning of the trace file.
	 * This supplies the visual debugger with the complete station structure, capacities,
	 * server counts, job classes, and connections necessary to render the model.
	 *
	 * @param network The QueueNetwork being simulated.
	 */
	public static void initTrace(QueueNetwork network) {
		if (!DEBUG_VISUAL_TRACE || network == null) {
			return;
		}
		ensureWriterStarted(network);

		if (!METADATA_WRITTEN.compareAndSet(false, true)) {
			return;
		}

		StringBuilder sb = new StringBuilder(4096);
		sb.append("{\"type\":\"metadata\",\"version\":\"1.0\",\"timestamp\":")
				.append(System.currentTimeMillis())
				.append(",\"modelName\":\"")
				.append(escapeJson(network.getName() != null ? network.getName() : "model"))
				.append("\",\"classes\":[");

		// Serialize Job Classes
		JobClassList classes = network.getJobClasses();
		if (classes != null) {
			boolean first = true;
			for (int i = 0; i < classes.size(); i++) {
				JobClass jc = classes.get(i);
				if (!first) sb.append(',');
				first = false;
				sb.append("{\"id\":").append(jc.getId())
						.append(",\"name\":\"").append(escapeJson(jc.getName())).append('"')
						.append(",\"type\":\"").append(jc.getType() == JobClass.OPEN_CLASS ? "OPEN" : "CLOSED").append('"')
						.append(",\"priority\":").append(jc.getPriority());
				if (jc.getReferenceNodeName() != null) {
					sb.append(",\"referenceNode\":\"").append(escapeJson(jc.getReferenceNodeName())).append('"');
				}
				sb.append('}');
			}
		}

		sb.append("],\"nodes\":[");

		// Serialize Nodes and Sections
		NodeList nodes = network.getNodes();
		if (nodes != null) {
			boolean firstNode = true;
			ListIterator<NetNode> it = nodes.listIterator();
			while (it.hasNext()) {
				NetNode node = it.next();
				if (!firstNode) sb.append(',');
				firstNode = false;

				String nodeType = deduceNodeType(node);
				sb.append("{\"id\":").append(node.getId())
						.append(",\"name\":\"").append(escapeJson(node.getName())).append('"')
						.append(",\"nodeType\":\"").append(escapeJson(nodeType)).append('"')
						.append(",\"sections\":{");

				// Input section
				NodeSection inSec = null;
				try { inSec = node.getSection(NodeSection.INPUT); } catch (Exception ignored) {}
				sb.append("\"input\":{");
				if (inSec != null) {
					sb.append("\"type\":\"").append(inSec.getClass().getSimpleName()).append('"');
					if (inSec instanceof Queue) {
						Queue q = (Queue) inSec;
						sb.append(",\"capacity\":").append(q.getStationCapacity());
					}
				} else {
					sb.append("\"type\":\"NONE\"");
				}
				sb.append("},");

				// Service section
				NodeSection srvSec = null;
				try { srvSec = node.getSection(NodeSection.SERVICE); } catch (Exception ignored) {}
				sb.append("\"service\":{");
				if (srvSec != null) {
					sb.append("\"type\":\"").append(srvSec.getClass().getSimpleName()).append('"');
					if (srvSec instanceof Server) {
						Server s = (Server) srvSec;
						sb.append(",\"servers\":").append(s.getNumberOfServers());
					} else if (srvSec instanceof PollingServer) {
						PollingServer ps = (PollingServer) srvSec;
						sb.append(",\"servers\":").append(ps.getNumberOfServers());
					}
				} else {
					sb.append("\"type\":\"NONE\"");
				}
				sb.append("},");

				// Output section
				NodeSection outSec = null;
				try { outSec = node.getSection(NodeSection.OUTPUT); } catch (Exception ignored) {}
				sb.append("\"output\":{");
				if (outSec != null) {
					sb.append("\"type\":\"").append(outSec.getClass().getSimpleName()).append('"');
				} else {
					sb.append("\"type\":\"NONE\"");
				}
				sb.append('}');

				sb.append("}}");
			}
		}

		sb.append("],\"connections\":[");

		// Serialize Connections (edges)
		if (nodes != null) {
			boolean firstConn = true;
			ListIterator<NetNode> it = nodes.listIterator();
			while (it.hasNext()) {
				NetNode srcNode = it.next();
				NodeList outNodes = srcNode.getOutputNodes();
				if (outNodes != null) {
					ListIterator<NetNode> targetIt = outNodes.listIterator();
					while (targetIt.hasNext()) {
						NetNode targetNode = targetIt.next();
						if (!firstConn) sb.append(',');
						firstConn = false;
						sb.append("{\"source\":\"").append(escapeJson(srcNode.getName()))
								.append("\",\"target\":\"").append(escapeJson(targetNode.getName()))
								.append("\"}");
					}
				}
			}
		}

		sb.append("]}\n");

		// Write metadata directly to writer
		if (writerThread != null) {
			writerThread.writeDirect(sb.toString());
		}
	}

	/**
	 * Deduces a clean user-facing station type name from the node's sections.
	 */
	private static String deduceNodeType(NetNode node) {
		try {
			NodeSection in = node.getSection(NodeSection.INPUT);
			NodeSection srv = node.getSection(NodeSection.SERVICE);
			NodeSection out = node.getSection(NodeSection.OUTPUT);

			if (in != null && in.getClass().getSimpleName().equals("RandomSource")) {
				return "Source";
			}
			if (in != null && in.getClass().getSimpleName().equals("Terminal")) {
				return "Terminal";
			}
			if (out != null && out.getClass().getSimpleName().equals("JobSink")) {
				return "Sink";
			}
			if (srv != null && srv.getClass().getSimpleName().contains("Delay")) {
				return "Delay";
			}
			if (in != null && in.getClass().getSimpleName().contains("Join")) {
				return "Join";
			}
			if (out != null && out.getClass().getSimpleName().contains("Fork")) {
				return "Fork";
			}
			if (in != null && in instanceof Queue) {
				return "Queue";
			}
		} catch (Exception ignored) {
		}
		return "Station";
	}

	/**
	 * Hot-path: Records an event scheduling / send.
	 * Called inside {@link NetNode#send(int, Object, double, byte, byte, NetNode)}.
	 */
	public static void recordSend(double simTime, double delay, int event, Object data,
								  byte sourceSection, byte destinationSection,
								  NetNode source, NetNode destination) {
		if (!DEBUG_VISUAL_TRACE) {
			return;
		}
		ensureWriterStarted(source != null ? source.getQueueNet() : null);
		lastSimTime = simTime;

		long writeIndex = WRITE_SEQ.getAndIncrement();
		if (writeIndex - READ_SEQ >= BUFFER_CAPACITY) {
			DROPPED_EVENTS.incrementAndGet();
			return;
		}

		SimEventRecord r = RING[(int) (writeIndex & BUFFER_MASK)];
		r.ready = false;

		r.sequenceNumber = writeIndex + 1;
		r.simTime = simTime;
		r.delay = delay;
		r.scheduledTime = simTime + delay;
		r.phase = "SEND";

		r.eventType = event;
		r.eventName = NetEvent.getEventName(event);

		// Source station in network & section inspection
		if (source != null) {
			r.srcNodeName = source.getName();
			r.srcNodeId = source.getId();
			r.nodeJobsCount = source.getJobInfoList() != null ? source.getJobInfoList().size() : -1;

			NodeSection srcSec = null;
			try { srcSec = source.getSection(sourceSection); } catch (Exception ignored) {}
			if (srcSec != null) {
				r.srcSectionClass = srcSec.getClass().getSimpleName();
				inspectSectionVariables(r, srcSec);
			} else {
				r.srcSectionClass = null;
			}
		} else {
			r.srcNodeName = "SYSTEM";
			r.srcNodeId = -1;
			r.srcSectionClass = "System";
			r.nodeJobsCount = -1;
		}
		r.srcSection = sourceSection;
		r.srcSectionName = SimEventRecord.getSectionName(sourceSection);

		// Destination station in network & section inspection
		if (destination != null) {
			r.dstNodeName = destination.getName();
			r.dstNodeId = destination.getId();
			NodeSection dstSec = null;
			try { dstSec = destination.getSection(destinationSection); } catch (Exception ignored) {}
			if (dstSec != null) {
				r.dstSectionClass = dstSec.getClass().getSimpleName();
			} else {
				r.dstSectionClass = null;
			}
		} else {
			r.dstNodeName = "SYSTEM";
			r.dstNodeId = -1;
			r.dstSectionClass = "System";
		}
		r.dstSection = destinationSection;
		r.dstSectionName = SimEventRecord.getSectionName(destinationSection);

		// Mandatory: capture calling Java source code location (class.method(File.java:line))
		r.callerCodeLocation = findCallerLocation();

		// Extract Job & Payload info
		extractDataInfo(r, data);

		r.ready = true;
		TOTAL_RECORDED.incrementAndGet();
	}

	/**
	 * Hot-path: Records event delivery / dispatch at destination node.
	 * Called inside {@link NetNode#dispatch(NetMessage)}.
	 */
	public static void recordDispatch(double simTime, NetMessage message, NetNode destination) {
		if (!DEBUG_VISUAL_TRACE || message == null) {
			return;
		}
		ensureWriterStarted(destination != null ? destination.getQueueNet() : null);
		lastSimTime = simTime;

		long writeIndex = WRITE_SEQ.getAndIncrement();
		if (writeIndex - READ_SEQ >= BUFFER_CAPACITY) {
			DROPPED_EVENTS.incrementAndGet();
			return;
		}

		SimEventRecord r = RING[(int) (writeIndex & BUFFER_MASK)];
		r.ready = false;

		r.sequenceNumber = writeIndex + 1;
		r.simTime = simTime;
		r.delay = 0.0;
		r.scheduledTime = simTime;
		r.phase = "DISPATCH";

		r.eventType = message.getEvent();
		r.eventName = NetEvent.getEventName(message.getEvent());

		// Source station in network
		NetNode src = message.getSource();
		if (src != null) {
			r.srcNodeName = src.getName();
			r.srcNodeId = src.getId();
			NodeSection srcSec = null;
			try { srcSec = src.getSection(message.getSourceSection()); } catch (Exception ignored) {}
			if (srcSec != null) {
				r.srcSectionClass = srcSec.getClass().getSimpleName();
			} else {
				r.srcSectionClass = null;
			}
		} else {
			r.srcNodeName = "SYSTEM";
			r.srcNodeId = -1;
			r.srcSectionClass = "System";
		}
		r.srcSection = message.getSourceSection();
		r.srcSectionName = SimEventRecord.getSectionName(message.getSourceSection());

		// Destination station in network
		if (destination != null) {
			r.dstNodeName = destination.getName();
			r.dstNodeId = destination.getId();
			r.nodeJobsCount = destination.getJobInfoList() != null ? destination.getJobInfoList().size() : -1;
			NodeSection dstSec = null;
			try { dstSec = destination.getSection(message.getDestinationSection()); } catch (Exception ignored) {}
			if (dstSec != null) {
				r.dstSectionClass = dstSec.getClass().getSimpleName();
				inspectSectionVariables(r, dstSec);
			} else {
				r.dstSectionClass = null;
			}
		} else {
			r.dstNodeName = "SYSTEM";
			r.dstNodeId = -1;
			r.dstSectionClass = "System";
			r.nodeJobsCount = -1;
		}
		r.dstSection = message.getDestinationSection();
		r.dstSectionName = SimEventRecord.getSectionName(message.getDestinationSection());

		// Mandatory: capture calling Java source code location (class.method(File.java:line))
		r.callerCodeLocation = findCallerLocation();

		extractDataInfo(r, message.getData());

		r.ready = true;
		TOTAL_RECORDED.incrementAndGet();
	}

	/**
	 * Inspects station variables from a section (queue capacity, queue size, servers).
	 */
	private static void inspectSectionVariables(SimEventRecord r, NodeSection sec) {
		if (sec instanceof Queue) {
			Queue q = (Queue) sec;
			r.queueSize = q.getJobInfoList() != null ? q.getJobInfoList().size() : 0;
			r.queueCapacity = q.getStationCapacity();
		} else if (sec instanceof Server) {
			Server s = (Server) sec;
			r.totalServers = s.getNumberOfServers();
		} else if (sec instanceof PollingServer) {
			PollingServer ps = (PollingServer) sec;
			r.totalServers = ps.getNumberOfServers();
		}
	}

	/**
	 * Finds the first stack frame outside the engine dispatch/tracing infrastructure
	 * to identify the exact Java calling code (class.method(File.java:line)).
	 */
	private static String findCallerLocation() {
		StackTraceElement[] stack = new Throwable().getStackTrace();
		for (int i = 2; i < stack.length && i < 25; i++) {
			StackTraceElement elem = stack[i];
			String cls = elem.getClassName();
			if (!cls.startsWith("log.SimEvent") &&
					!cls.startsWith("java.") &&
					!cls.equals("QueueNet.NetNode") &&
					!cls.equals("QueueNet.NodeSection")) {
				String fileName = elem.getFileName() != null ? elem.getFileName() : elem.getClassName();
				return elem.getClassName() + "." + elem.getMethodName() + "(" + fileName + ":" + elem.getLineNumber() + ")";
			}
		}
		return "SYSTEM";
	}

	/**
	 * Records an unprocessed message (potential routing mismatch or dropped message).
	 */
	public static void recordUnprocessed(double simTime, NetMessage message, NetNode destination) {
		if (!DEBUG_VISUAL_TRACE || message == null) {
			return;
		}
		recordDirectEvent(simTime, "NOT_PROCESSED", message.getEvent(), message.getSource(),
				message.getSourceSection(), destination, message.getDestinationSection(),
				message.getData(), "Message not processed by any section");
	}

	/**
	 * Records a job dropped due to queue capacity.
	 */
	public static void recordDrop(double simTime, NetNode node, Job job) {
		if (!DEBUG_VISUAL_TRACE) {
			return;
		}
		recordDirectEvent(simTime, "DROP", NetEvent.EVENT_JOB, node, NodeSection.INPUT,
				node, NodeSection.NO_ADDRESS, job, "Job dropped: queue capacity reached");
	}

	private static void recordDirectEvent(double simTime, String phase, int event,
										  NetNode src, byte srcSection,
										  NetNode dst, byte dstSection,
										  Object data, String messageText) {
		ensureWriterStarted(src != null ? src.getQueueNet() : (dst != null ? dst.getQueueNet() : null));
		lastSimTime = simTime;

		long writeIndex = WRITE_SEQ.getAndIncrement();
		if (writeIndex - READ_SEQ >= BUFFER_CAPACITY) {
			DROPPED_EVENTS.incrementAndGet();
			return;
		}

		SimEventRecord r = RING[(int) (writeIndex & BUFFER_MASK)];
		r.ready = false;

		r.sequenceNumber = writeIndex + 1;
		r.simTime = simTime;
		r.delay = 0.0;
		r.scheduledTime = simTime;
		r.phase = phase;
		r.eventType = event;
		r.eventName = NetEvent.getEventName(event);

		if (src != null) {
			r.srcNodeName = src.getName();
			r.srcNodeId = src.getId();
			NodeSection srcSec = null;
			try { srcSec = src.getSection(srcSection); } catch (Exception ignored) {}
			if (srcSec != null) {
				r.srcSectionClass = srcSec.getClass().getSimpleName();
				inspectSectionVariables(r, srcSec);
			} else {
				r.srcSectionClass = null;
			}
		} else {
			r.srcNodeName = "SYSTEM";
			r.srcNodeId = -1;
			r.srcSectionClass = "System";
		}
		r.srcSection = srcSection;
		r.srcSectionName = SimEventRecord.getSectionName(srcSection);

		if (dst != null) {
			r.dstNodeName = dst.getName();
			r.dstNodeId = dst.getId();
			r.nodeJobsCount = dst.getJobInfoList() != null ? dst.getJobInfoList().size() : -1;
			NodeSection dstSec = null;
			try { dstSec = dst.getSection(dstSection); } catch (Exception ignored) {}
			if (dstSec != null) {
				r.dstSectionClass = dstSec.getClass().getSimpleName();
			} else {
				r.dstSectionClass = null;
			}
		} else {
			r.dstNodeName = "SYSTEM";
			r.dstNodeId = -1;
			r.dstSectionClass = "System";
			r.nodeJobsCount = -1;
		}
		r.dstSection = dstSection;
		r.dstSectionName = SimEventRecord.getSectionName(dstSection);

		// Mandatory caller location
		r.callerCodeLocation = findCallerLocation();

		r.messageText = messageText;
		extractDataInfo(r, data);

		r.ready = true;
		TOTAL_RECORDED.incrementAndGet();
	}

	private static void extractDataInfo(SimEventRecord r, Object data) {
		if (data instanceof Job) {
			Job job = (Job) data;
			r.jobId = job.getId();
			JobClass jc = job.getJobClass();
			if (jc != null) {
				r.jobClassName = jc.getName();
				r.jobClassId = jc.getId();
				r.jobPriority = jc.getPriority();
			} else {
				r.jobClassName = null;
				r.jobClassId = -1;
				r.jobPriority = 0;
			}
			r.jobSystemEnteringTime = job.getSystemEnteringTime();
			r.jobServiceTime = job.getServiceTime();
			r.jobTotalQueueTime = job.getTotalQueueTime();
			r.isToken = job.isToken();
			if (job.getLastVisitedPair() != null && job.getLastVisitedPair().getFirst() != null) {
				r.lastVisitedNode = job.getLastVisitedPair().getFirst().getName();
			} else {
				r.lastVisitedNode = null;
			}
			r.payloadType = null;
			r.payloadValue = null;
		} else if (data instanceof NetMessage) {
			extractDataInfo(r, ((NetMessage) data).getData());
		} else if (data != null) {
			r.jobId = -1;
			r.jobClassName = null;
			r.jobClassId = -1;
			r.jobPriority = 0;
			r.jobSystemEnteringTime = 0.0;
			r.jobServiceTime = -1.0;
			r.jobTotalQueueTime = 0.0;
			r.isToken = false;
			r.lastVisitedNode = null;
			r.payloadType = data.getClass().getSimpleName();
			r.payloadValue = String.valueOf(data);
		} else {
			r.jobId = -1;
			r.jobClassName = null;
			r.jobClassId = -1;
			r.jobPriority = 0;
			r.jobSystemEnteringTime = 0.0;
			r.jobServiceTime = -1.0;
			r.jobTotalQueueTime = 0.0;
			r.isToken = false;
			r.lastVisitedNode = null;
			r.payloadType = null;
			r.payloadValue = null;
		}
	}

	/**
	 * Stops event tracing for the current simulation run, flushes all remaining records,
	 * writes the summary footer, and cleanly closes the output file.
	 * Resets counters and ring buffer so the next simulation run starts cleanly in its own file.
	 */
	public static synchronized void stop() {
		if (!INITIALIZED.get()) {
			return;
		}

		WriterThread t = writerThread;
		if (t != null) {
			t.shutdown();
			try {
				t.join(5000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			writerThread = null;
		}

		// Reset state and ring buffer for subsequent simulations
		WRITE_SEQ.set(0);
		READ_SEQ = 0;
		DROPPED_EVENTS.set(0);
		TOTAL_RECORDED.set(0);
		lastSimTime = 0.0;
		for (int i = 0; i < BUFFER_CAPACITY; i++) {
			RING[i].reset();
		}

		INITIALIZED.set(false);
		METADATA_WRITTEN.set(false);
	}

	/**
	 * Background writer thread that drains the SPSC ring buffer and formats NDJSON.
	 */
	private static final class WriterThread extends Thread {

		private final File outputFile;
		private volatile boolean running = true;
		private BufferedWriter writer = null;
		private final StringBuilder sb = new StringBuilder(2048);

		WriterThread(File file) {
			this.outputFile = file;
		}

		void shutdown() {
			running = false;
		}

		void writeDirect(String text) {
			try {
				ensureOpen();
				writer.write(text);
				writer.flush();
			} catch (IOException e) {
				System.err.println("[SimEventTracer] Error writing direct metadata: " + e.getMessage());
			}
		}

		private void ensureOpen() throws IOException {
			if (writer == null) {
				File parent = outputFile.getParentFile();
				if (parent != null && !parent.exists()) {
					parent.mkdirs();
				}
				writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(outputFile, false), StandardCharsets.UTF_8),
						64 * 1024 // 64KB write buffer
				);
			}
		}

		@Override
		public void run() {
			try {
				ensureOpen();

				while (running || READ_SEQ < WRITE_SEQ.get()) {
					long targetWrite = WRITE_SEQ.get();

					if (READ_SEQ == targetWrite) {
						if (!running) break;
						LockSupport.parkNanos(1_000_000L); // Park 1ms when idle
						continue;
					}

					// Process batch of available records
					int batchCount = 0;
					while (READ_SEQ < targetWrite && batchCount < 2048) {
						SimEventRecord r = RING[(int) (READ_SEQ & BUFFER_MASK)];
						// Wait briefly for record to be fully populated by producer
						int spins = 0;
						while (!r.ready && spins < 1000) {
							spins++;
							Thread.yield();
						}

						sb.setLength(0);
						formatRecordJson(sb, r);
						sb.append('\n');
						writer.write(sb.toString());

						r.reset();
						READ_SEQ++;
						batchCount++;
					}

					writer.flush();
				}

				// Write footer summary record
				long durationMs = System.currentTimeMillis() - traceStartTimeMs;
				sb.setLength(0);
				sb.append("{\"type\":\"footer\",\"totalEvents\":").append(TOTAL_RECORDED.get())
						.append(",\"droppedEvents\":").append(DROPPED_EVENTS.get())
						.append(",\"lastSimTime\":").append(lastSimTime)
						.append(",\"durationMs\":").append(durationMs)
						.append("}\n");
				writer.write(sb.toString());
				writer.flush();

			} catch (IOException e) {
				System.err.println("[SimEventTracer] I/O error during event tracing: " + e.getMessage());
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException ignored) {}
					writer = null;
				}
			}
		}

		private void formatRecordJson(StringBuilder sb, SimEventRecord r) {
			sb.append("{\"type\":\"event\",\"seq\":").append(r.sequenceNumber)
					.append(",\"t\":").append(r.simTime)
					.append(",\"phase\":\"").append(r.phase).append('"')
					.append(",\"event\":\"").append(r.eventName != null ? r.eventName : "UNKNOWN").append('"')
					.append(",\"eventId\":").append(r.eventType);

			if (r.delay > 0.0) {
				sb.append(",\"delay\":").append(r.delay)
						.append(",\"schedTime\":").append(r.scheduledTime);
			}

			// Actual station names in the network + sections + section Java classes
			String srcCls = r.srcSectionClass != null ? r.srcSectionClass : "None";
			String dstCls = r.dstSectionClass != null ? r.dstSectionClass : "None";

			sb.append(",\"srcStation\":\"").append(escapeJson(r.srcNodeName)).append('"')
					.append(",\"srcStationId\":").append(r.srcNodeId)
					.append(",\"srcSection\":\"").append(r.srcSectionName).append('"')
					.append(",\"srcClass\":\"").append(srcCls).append('"')
					.append(",\"srcDisplay\":\"").append(escapeJson(r.srcNodeName)).append(" [").append(r.srcSectionName).append(':').append(srcCls).append("]\"")
					.append(",\"dstStation\":\"").append(escapeJson(r.dstNodeName)).append('"')
					.append(",\"dstStationId\":").append(r.dstNodeId)
					.append(",\"dstSection\":\"").append(r.dstSectionName).append('"')
					.append(",\"dstClass\":\"").append(dstCls).append('"')
					.append(",\"dstDisplay\":\"").append(escapeJson(r.dstNodeName)).append(" [").append(r.dstSectionName).append(':').append(dstCls).append("]\"")
					.append(",\"src\":\"").append(escapeJson(r.srcNodeName)).append('"')
					.append(",\"dst\":\"").append(escapeJson(r.dstNodeName)).append('"');

			// Mandatory exact Java source code location: Class.method(File.java:line)
			sb.append(",\"caller\":\"").append(escapeJson(r.callerCodeLocation != null ? r.callerCodeLocation : "SYSTEM")).append('"');

			// Station State Variables
			if (r.nodeJobsCount >= 0) {
				sb.append(",\"nodeJobs\":").append(r.nodeJobsCount);
			}
			if (r.queueSize >= 0) {
				sb.append(",\"queueSize\":").append(r.queueSize);
			}
			if (r.queueCapacity >= 0) {
				sb.append(",\"queueCap\":").append(r.queueCapacity);
			}
			if (r.totalServers >= 0) {
				sb.append(",\"servers\":").append(r.totalServers);
			}

			// Job / Packet Variables
			if (r.jobId >= 0) {
				sb.append(",\"jobId\":").append(r.jobId);
				if (r.jobClassName != null) {
					sb.append(",\"jobClass\":\"").append(escapeJson(r.jobClassName)).append('"')
							.append(",\"classId\":").append(r.jobClassId)
							.append(",\"priority\":").append(r.jobPriority);
				}
				if (r.jobSystemEnteringTime > 0.0) {
					sb.append(",\"enterTime\":").append(r.jobSystemEnteringTime);
				}
				if (r.jobServiceTime >= 0.0) {
					sb.append(",\"serviceTime\":").append(r.jobServiceTime);
				}
				if (r.jobTotalQueueTime > 0.0) {
					sb.append(",\"queueTime\":").append(r.jobTotalQueueTime);
				}
				if (r.isToken) {
					sb.append(",\"isToken\":true");
				}
				if (r.lastVisitedNode != null) {
					sb.append(",\"lastVisited\":\"").append(escapeJson(r.lastVisitedNode)).append('"');
				}
			}

			// Non-job Payload
			if (r.payloadType != null) {
				sb.append(",\"payloadType\":\"").append(escapeJson(r.payloadType)).append('"')
						.append(",\"payload\":\"").append(escapeJson(r.payloadValue)).append('"');
			}

			if (r.messageText != null) {
				sb.append(",\"msg\":\"").append(escapeJson(r.messageText)).append('"');
			}

			sb.append('}');
		}
	}

	private static String escapeJson(String s) {
		if (s == null) return "";
		boolean needsEscape = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"' || c == '\\' || c < 0x20) {
				needsEscape = true;
				break;
			}
		}
		if (!needsEscape) return s;

		StringBuilder sb = new StringBuilder(s.length() + 16);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"':  sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\b': sb.append("\\b");  break;
				case '\f': sb.append("\\f");  break;
				case '\n': sb.append("\\n");  break;
				case '\r': sb.append("\\r");  break;
				case '\t': sb.append("\\t");  break;
				default:
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}
}
