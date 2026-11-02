package simEngine;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jmt.common.exception.LoadException;

/** Loader and Simulation setup on tiny inline documents (loaded and initialized, never run). */
class SimLoaderInlineTest {

	@TempDir
	Path dir;

	private static final String EXP = "<subParameter classPath=\"jmt.engine.NetStrategies.ServiceStrategies.ServiceTimeStrategy\" name=\"ServiceTimeStrategy\">"
			+ "<subParameter classPath=\"jmt.engine.random.Exponential\" name=\"Exponential\"/>"
			+ "<subParameter classPath=\"jmt.engine.random.ExponentialPar\" name=\"distrPar\"><subParameter classPath=\"java.lang.Double\" name=\"lambda\"><value>%s</value></subParameter></subParameter>"
			+ "</subParameter>";

	private static String exp(String rate) {
		return String.format(EXP, rate);
	}

	private static String strategy(String cls, String rate) {
		return "<parameter array=\"true\" classPath=\"jmt.engine.NetStrategies.ServiceStrategy\" name=\"ServiceStrategy\"><refClass>" + cls + "</refClass>" + exp(rate) + "</parameter>";
	}

	private static String router(String cls) {
		return "<section className=\"Router\"><parameter array=\"true\" classPath=\"jmt.engine.NetStrategies.RoutingStrategy\" name=\"RoutingStrategy\"><refClass>" + cls
				+ "</refClass><subParameter classPath=\"jmt.engine.NetStrategies.RoutingStrategies.RandomStrategy\" name=\"Random\"/></parameter></section>";
	}

	private static String queueSection(String cls) {
		return "<section className=\"Queue\"><parameter classPath=\"java.lang.Integer\" name=\"size\"><value>-1</value></parameter>"
				+ "<parameter array=\"true\" classPath=\"java.lang.String\" name=\"dropStrategies\"><refClass>" + cls + "</refClass><subParameter classPath=\"java.lang.String\" name=\"dropStrategy\"><value>drop</value></subParameter></parameter>"
				+ "<parameter classPath=\"jmt.engine.NetStrategies.QueueGetStrategies.FCFSstrategy\" name=\"FCFSstrategy\"/>"
				+ "<parameter array=\"true\" classPath=\"jmt.engine.NetStrategies.QueuePutStrategy\" name=\"QueuePutStrategy\"><refClass>" + cls
				+ "</refClass><subParameter classPath=\"jmt.engine.NetStrategies.QueuePutStrategies.TailStrategy\" name=\"TailStrategy\"/></parameter></section>";
	}

	private static String server(String cls, String rate) {
		return "<section className=\"Server\"><parameter classPath=\"java.lang.Integer\" name=\"maxJobs\"><value>1</value></parameter>"
				+ "<parameter array=\"true\" classPath=\"java.lang.Integer\" name=\"numberOfVisits\"><refClass>" + cls
				+ "</refClass><subParameter classPath=\"java.lang.Integer\" name=\"numberOfVisits\"><value>1</value></subParameter></parameter>" + strategy(cls, rate) + "</section>";
	}

	private static String measure(String type, String nodeType, String node, String cls) {
		return "<measure alpha=\"0.01\" name=\"" + type + node + cls + "\" nodeType=\"" + nodeType + "\" precision=\"0.03\" referenceNode=\"" + node + "\" referenceUserClass=\"" + cls
				+ "\" type=\"" + type + "\" verbose=\"false\"/>";
	}

	private static String open(String attrs, String measures, String region) {
		return "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?><sim xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + attrs
				+ " xsi:noNamespaceSchemaLocation=\"SIMmodeldefinition.xsd\">"
				+ "<userClass name=\"C1\" priority=\"0\" referenceSource=\"Src\" type=\"open\"/>"
				+ "<node name=\"Src\"><section className=\"RandomSource\">" + strategy("C1", "0.5") + "</section><section className=\"ServiceTunnel\"/>" + router("C1") + "</node>"
				+ "<node name=\"Q\">" + queueSection("C1") + server("C1", "2.0") + router("C1") + "</node>"
				+ "<node name=\"Snk\"><section className=\"JobSink\"/></node>"
				+ measures + "<connection source=\"Src\" target=\"Q\"/><connection source=\"Q\" target=\"Snk\"/>" + region + "</sim>";
	}

	private static final String REGION = "<blockingRegion name=\"R\" type=\"default\"><regionNode nodeName=\"Q\"/><globalConstraint maxJobs=\"5\"/><globalMemoryConstraint maxMemory=\"-1\"/>"
			+ "<classConstraint jobClass=\"C1\" maxJobsPerClass=\"-1\"/><classMemoryConstraint jobClass=\"C1\" maxMemoryPerClass=\"-1\"/><dropRules dropThisClass=\"true\" jobClass=\"C1\"/>"
			+ "<classWeight jobClass=\"C1\" weight=\"1\"/><classSize jobClass=\"C1\" size=\"1\"/><classSoftDeadline jobClass=\"C1\" softDeadline=\"0.0\"/></blockingRegion>";

	private Simulation load(String xml) throws Exception {
		Path f = dir.resolve("m.xml");
		Files.write(f, xml.getBytes(StandardCharsets.ISO_8859_1));
		SimLoader loader = new SimLoader(f.toString());
		Simulation sim = loader.getSim();
		sim.setXmlSimModelDefPath(f.toString());
		sim.setRandomEngineSeed(11L);
		sim.initialize();
		return sim;
	}

	private static final String ATTRS = "disableStatisticStop=\"false\" logDecimalSeparator=\".\" logDelimiter=\",\" logPath=\"\" logReplaceMode=\"0\" maxSamples=\"5000\" name=\"tiny\" polling=\"1.0\"";

	@Test
	void openModelWithStationAndSystemMeasures() throws Exception {
		StringBuilder m = new StringBuilder();
		String[] stationTypes = { "Number of Customers", "Queue Time", "Response Time", "Residence Time", "Arrival Rate", "Throughput", "Utilization", "Effective Utilization", "Drop Rate" };
		for (String t : stationTypes) {
			m.append(measure(t, "station", "Q", "C1")).append(measure(t, "station", "Q", ""));
		}
		for (String t : new String[] { "System Number of Customers", "System Response Time", "System Throughput", "System Drop Rate", "System Power" }) {
			m.append(measure(t, "", "", ""));
		}
		m.append(measure("Response Time per Sink", "station", "Snk", "C1")).append(measure("Throughput per Sink", "station", "Snk", "C1"));
		Simulation sim = load(open(ATTRS, m.toString(), ""));
		assertEquals(1, sim.getClasses().length);
		assertNotNull(sim.getNetwork().getNode("Q"));
		assertNotNull(sim.getNetwork().getNode("Src"));
		assertTrue(sim.getNetwork().getMeasures().size() >= 20);
	}

	@Test
	void blockingRegionWithRegionMeasures() throws Exception {
		String m = measure("Number of Customers", "region", "R", "C1") + measure("Drop Rate", "region", "R", "C1") + measure("Response Time", "region", "R", "")
				+ measure("Throughput", "region", "R", "") + measure("FCR Capacity", "region", "R", "") + measure("FCR Memory", "region", "R", "");
		Simulation sim = load(open(ATTRS, m, REGION));
		assertEquals(1, sim.getAllRegions().length);
		assertEquals("R", sim.getAllRegions()[0].getName());
	}

	@Test
	void documentLevelAttributesAreRead() throws Exception {
		String attrs = ATTRS.replace("disableStatisticStop=\"false\"", "disableStatisticStop=\"true\"").replace("polling=\"1.0\"", "polling=\"2.0\" seed=\"77\" maxEvents=\"100\" maxDuration=\"10\"");
		Simulation sim = load(open(attrs, measure("Throughput", "station", "Q", "C1"), ""));
		assertNotNull(sim.getSimParameters());
	}

	@Test
	void unnamedSimulationGetsATimestampName() throws Exception {
		Simulation sim = load(open(ATTRS.replace("name=\"tiny\"", "name=\"\""), measure("Throughput", "station", "Q", "C1"), ""));
		assertTrue(sim.getName().startsWith("JSIM_"), sim.getName());
	}

	@Test
	void unknownParameterClassIsALoadError() throws Exception {
		String xml = open(ATTRS, "", "").replace("java.lang.Integer\" name=\"size", "java.lang.NoSuchThing\" name=\"size");
		Path f = dir.resolve("bad.xml");
		Files.write(f, xml.getBytes(StandardCharsets.ISO_8859_1));
		assertThrows(LoadException.class, () -> new SimLoader(f.toString()));
	}

	@Test
	void regionMissingConstraintIsALoadError() throws Exception {
		String xml = open(ATTRS, "", REGION.replace("<globalConstraint maxJobs=\"5\"/>", ""));
		Path f = dir.resolve("bad2.xml");
		Files.write(f, xml.getBytes(StandardCharsets.ISO_8859_1));
		assertThrows(LoadException.class, () -> new SimLoader(f.toString()));
	}
}
