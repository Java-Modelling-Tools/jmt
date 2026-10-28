package log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

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
		SimEventTracer.CHUNK_MAX_MB = SimEventTracer.DEFAULT_CHUNK_MAX_MB;
		SimEventTracer.CHUNK_MAX_EVENTS = SimEventTracer.DEFAULT_CHUNK_MAX_EVENTS;
		SimEventTracer.FILTER_INTERNAL_EVENTS = true;
		tempTraceFile = File.createTempFile("jmt_test_trace_", ".ndjson.gz");
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

		File current = SimEventTracer.getCurrentOutputFile();
		assertTrue("No active file or file should not exist when tracer is disabled", current == null || !current.exists() || current.length() == 0);
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

		File writtenFile = SimEventTracer.getCurrentOutputFile();

		// 4. Stop and flush
		SimEventTracer.stop();

		// 5. Verify written compressed NDJSON lines
		assertTrue("Output file must end with .ndjson.gz", writtenFile.getName().endsWith(".ndjson.gz"));
		List<String> lines = readLines(writtenFile);
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

		writtenFile.delete();
	}

	@Test
	public void testFilterInternalHousekeepingEvents() throws Exception {
		SimEventTracer.setOutputFile(tempTraceFile.getAbsolutePath());
		SimEventTracer.enable(tempTraceFile.getAbsolutePath());

		NetNode src = new NetNode("SourceNode");
		NetNode dst = new NetNode("DestNode");

		QueueNetwork network = new QueueNetwork("FilterTestModel");
		network.addNode(src);
		network.addNode(dst);
		SimEventTracer.initTrace(network);

		// Record high-frequency internal events (should be filtered)
		SimEventTracer.recordSend(0.1, 0.0, NetEvent.EVENT_TIMING, null, NodeSection.OUTPUT, NodeSection.INPUT, src, dst);
		SimEventTracer.recordSend(0.2, 0.0, NetEvent.EVENT_KEEP_AWAKE, null, NodeSection.OUTPUT, NodeSection.INPUT, src, dst);
		SimEventTracer.recordSend(0.3, 0.0, NetEvent.EVENT_CHECK_DELAY_OFF, null, NodeSection.OUTPUT, NodeSection.INPUT, src, dst);

		// Record structural packet events (should be kept)
		SimEventTracer.recordSend(0.4, 0.1, NetEvent.EVENT_JOB, null, NodeSection.OUTPUT, NodeSection.INPUT, src, dst);
		SimEventTracer.recordSend(0.5, 0.0, NetEvent.EVENT_ACK, null, NodeSection.INPUT, NodeSection.SERVICE, dst, src);

		File activeFile = SimEventTracer.getCurrentOutputFile();
		SimEventTracer.stop();

		List<String> lines = readLines(activeFile);
		// metadata (1) + 2 kept events + footer (1) = 4 lines
		assertEquals("Should have exactly 4 lines (metadata, 2 packet events, footer)", 4, lines.size());
		for (String line : lines) {
			assertFalse("EVENT_TIMING should have been filtered out", line.contains("EVENT_TIMING"));
			assertFalse("EVENT_KEEP_AWAKE should have been filtered out", line.contains("EVENT_KEEP_AWAKE"));
		}

		activeFile.delete();
	}

	@Test
	public void testRollingFileChunks() throws Exception {
		// Set small chunk limit: 5 events per chunk
		SimEventTracer.CHUNK_MAX_EVENTS = 5;
		SimEventTracer.CHUNK_MAX_MB = 0; // disable MB check

		File tempDir = new File(System.getProperty("java.io.tmpdir"), "jmt_chunk_test_" + System.currentTimeMillis());
		tempDir.mkdirs();

		SimEventTracer.setOutputFile(tempDir.getAbsolutePath());
		SimEventTracer.enable(tempDir.getAbsolutePath());

		NetNode src = new NetNode("SourceNode");
		NetNode dst = new NetNode("DestNode");
		QueueNetwork network = new QueueNetwork("ChunkModel");
		network.addNode(src);
		network.addNode(dst);
		SimEventTracer.initTrace(network);

		// Record 12 events -> should create part1 (5 events), part2 (5 events), part3 (2 events + footer)
		for (int i = 0; i < 12; i++) {
			SimEventTracer.recordSend(i * 0.1, 0.01, NetEvent.EVENT_JOB, null, NodeSection.OUTPUT, NodeSection.INPUT, src, dst);
		}

		SimEventTracer.stop();

		File[] chunkFiles = tempDir.listFiles((dir, name) -> name.endsWith(".ndjson.gz"));
		assertTrue("Should have created at least 2 chunk files", chunkFiles != null && chunkFiles.length >= 2);

		for (File chunk : chunkFiles) {
			List<String> chunkLines = readLines(chunk);
			assertTrue("Each chunk must start with metadata", chunkLines.get(0).startsWith("{\"type\":\"metadata\""));
			chunk.delete();
		}
		tempDir.delete();
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
		if (f == null || !f.exists()) {
			return lines;
		}
		BufferedReader br = null;
		if (f.getName().endsWith(".gz")) {
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f)), StandardCharsets.UTF_8));
		} else {
			br = new BufferedReader(new FileReader(f));
		}
		try {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					lines.add(line);
				}
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}
		return lines;
	}
}
