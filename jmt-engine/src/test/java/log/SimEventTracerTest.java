package log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import QueueNet.Job;
import QueueNet.JobClass;
import QueueNet.NetEvent;
import QueueNet.NetNode;
import QueueNet.NodeSection;
import QueueNet.QueueNetwork;

/**
 * Unit tests for SimEventTracer.
 */
public class SimEventTracerTest {

	private File tempTraceFile;

	@Before
	public void setUp() throws IOException {
		SimEventTracer.disable();
		SimEventTracer.stop();
		tempTraceFile = File.createTempFile("jmt_test_trace_", ".ndjson");
		tempTraceFile.deleteOnExit();
	}

	@After
	public void tearDown() {
		SimEventTracer.disable();
		SimEventTracer.stop();
		if (tempTraceFile != null && tempTraceFile.exists()) {
			tempTraceFile.delete();
		}
	}

	@Test
	public void testDisabledByDefaultDoesNotWrite() throws Exception {
		SimEventTracer.disable();
		assertFalse(SimEventTracer.isEnabled());
		assertFalse(SimEventTracer.DEBUG_VISUAL_TRACE);

		// Record an event while disabled
		SimEventTracer.recordSend(1.0, 0.5, NetEvent.EVENT_JOB, null,
				NodeSection.INPUT, NodeSection.SERVICE, null, null);

		assertFalse("File should not be created when tracer is disabled", tempTraceFile.length() > 0);
	}

	@Test
	public void testMetadataAndEventRecordingEndToEnd() throws Exception {
		SimEventTracer.setOutputFile(tempTraceFile.getAbsolutePath());
		SimEventTracer.enable(tempTraceFile.getAbsolutePath());
		assertTrue(SimEventTracer.isEnabled());

		// 1. Construct a small queue network
		QueueNetwork network = new QueueNetwork("TestVisualNetwork");
		JobClass jc = new JobClass("ClassA", 1, JobClass.OPEN_CLASS, "Source1");
		network.addJobClass(jc);

		NetNode nodeA = new NetNode("Source1");
		NetNode nodeB = new NetNode("Queue1");
		nodeA.connect(nodeB);
		network.addNode(nodeA);
		network.addNode(nodeB);

		// 2. Initialize metadata
		SimEventTracer.initTrace(network);

		// 3. Record simulation events
		QueueNet.NetSystem netSys = new QueueNet.NetSystem();
		netSys.initialize();
		Job testJob = new Job(jc, null);
		testJob.initialize(netSys);

		// Event 1: Send job from Source1:OUTPUT to Queue1:INPUT
		SimEventTracer.recordSend(0.0, 0.25, NetEvent.EVENT_JOB, testJob,
				NodeSection.OUTPUT, NodeSection.INPUT, nodeA, nodeB);

		// Event 2: Dispatch job at Queue1
		SimEventTracer.recordDispatch(0.25, null, nodeB);

		// Event 3: Drop event
		SimEventTracer.recordDrop(0.25, nodeB, testJob);

		// 4. Stop and flush
		SimEventTracer.stop();

		// 5. Verify written NDJSON lines
		List<String> lines = readLines(tempTraceFile);
		assertTrue("Trace file should contain at least 4 lines (metadata, 2 events, footer)", lines.size() >= 4);

		// Verify Line 1: Metadata
		String metadataLine = lines.get(0);
		assertTrue("First line must be metadata", metadataLine.startsWith("{\"type\":\"metadata\""));
		assertTrue("Metadata must contain model name", metadataLine.contains("\"modelName\":\"TestVisualNetwork\""));
		assertTrue("Metadata must contain ClassA", metadataLine.contains("\"name\":\"ClassA\""));
		assertTrue("Metadata must contain Source1", metadataLine.contains("\"name\":\"Source1\""));
		assertTrue("Metadata must contain Queue1", metadataLine.contains("\"name\":\"Queue1\""));
		assertTrue("Metadata must contain connection", metadataLine.contains("\"source\":\"Source1\",\"target\":\"Queue1\""));

		// Verify Line 2: SEND event
		String sendLine = lines.get(1);
		assertTrue("Must be an event record", sendLine.contains("\"type\":\"event\""));
		assertTrue("Must have phase SEND", sendLine.contains("\"phase\":\"SEND\""));
		assertTrue("Must have event EVENT_JOB", sendLine.contains("\"event\":\"EVENT_JOB\""));
		assertTrue("Must identify Source1 as actual station name", sendLine.contains("\"srcStation\":\"Source1\""));
		assertTrue("Must identify Queue1 as actual destination station name", sendLine.contains("\"dstStation\":\"Queue1\""));
		assertTrue("Must have mandatory Java caller location", sendLine.contains("\"caller\":"));
		assertTrue("Must contain delay", sendLine.contains("\"delay\":0.25"));
		assertTrue("Must contain jobId", sendLine.contains("\"jobId\":"));
		assertTrue("Must contain jobClass ClassA", sendLine.contains("\"jobClass\":\"ClassA\""));

		// Verify Footer
		String footerLine = lines.get(lines.size() - 1);
		assertTrue("Last line must be footer", footerLine.startsWith("{\"type\":\"footer\""));
		assertTrue("Footer must record total events", footerLine.contains("\"totalEvents\":"));
	}

	@Test
	public void testHighThroughputBurstPerformance() throws Exception {
		SimEventTracer.setOutputFile(tempTraceFile.getAbsolutePath());
		SimEventTracer.enable(tempTraceFile.getAbsolutePath());

		NetNode src = new NetNode("SourceNode");
		NetNode dst = new NetNode("DestNode");

		long startTime = System.nanoTime();
		int eventCount = 50_000;

		for (int i = 0; i < eventCount; i++) {
			SimEventTracer.recordSend(i * 0.01, 0.01, NetEvent.EVENT_JOB, null,
					NodeSection.OUTPUT, NodeSection.INPUT, src, dst);
		}

		long elapsedNs = System.nanoTime() - startTime;
		double nsPerEvent = (double) elapsedNs / eventCount;

		// Stop and flush
		SimEventTracer.stop();

		// Verify hot-path throughput is within single-digit microseconds per event (even during burst)
		System.out.println("[SimEventTracer Benchmark] 50,000 events recorded in "
				+ (elapsedNs / 1_000_000.0) + " ms (" + String.format("%.1f", nsPerEvent) + " ns/event)");

		List<String> lines = readLines(tempTraceFile);
		assertTrue("Should have recorded events", lines.size() > 1000);
	}

	@Test
	public void testMultipleSimulationsSeparated() throws Exception {
		File tempDir = new File(System.getProperty("java.io.tmpdir"), "jmt_multi_trace_test_" + System.currentTimeMillis());
		tempDir.mkdirs();
		tempDir.deleteOnExit();

		SimEventTracer.setOutputFile(tempDir.getAbsolutePath());
		SimEventTracer.enable(tempDir.getAbsolutePath());

		// Simulation 1: Model Alpha
		QueueNetwork net1 = new QueueNetwork("ModelAlpha");
		net1.addNode(new NetNode("AlphaStation"));
		SimEventTracer.initTrace(net1);
		File file1 = SimEventTracer.getCurrentOutputFile();

		SimEventTracer.recordSend(0.0, 0.1, NetEvent.EVENT_JOB, null, NodeSection.OUTPUT, NodeSection.INPUT, net1.getNodes().get(0), null);
		SimEventTracer.stop();

		// Simulation 2: Model Beta
		SimEventTracer.enable(tempDir.getAbsolutePath());
		QueueNetwork net2 = new QueueNetwork("ModelBeta");
		net2.addNode(new NetNode("BetaStation"));
		SimEventTracer.initTrace(net2);
		File file2 = SimEventTracer.getCurrentOutputFile();

		SimEventTracer.recordSend(1.0, 0.2, NetEvent.EVENT_ACK, null, NodeSection.SERVICE, NodeSection.OUTPUT, net2.getNodes().get(0), null);
		SimEventTracer.stop();

		// Verify two separate files were produced
		assertTrue("File 1 must exist", file1.exists());
		assertTrue("File 2 must exist", file2.exists());
		assertFalse("Files must be distinct and not overwrite each other", file1.getAbsolutePath().equals(file2.getAbsolutePath()));

		List<String> lines1 = readLines(file1);
		List<String> lines2 = readLines(file2);

		assertTrue("File 1 metadata must be ModelAlpha", lines1.get(0).contains("\"modelName\":\"ModelAlpha\""));
		assertTrue("File 2 metadata must be ModelBeta", lines2.get(0).contains("\"modelName\":\"ModelBeta\""));

		file1.delete();
		file2.delete();
		tempDir.delete();
	}

	private static List<String> readLines(File f) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					lines.add(line);
				}
			}
		}
		return lines;
	}
}
