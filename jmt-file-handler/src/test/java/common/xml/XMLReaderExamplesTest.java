package common.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import common.CommonConstants;
import common.definitions.CommonModel;
import common.distributions.Distribution;

/** XMLReader on single example models (the engine {@code <sim>} part), one feature per test. */
class XMLReaderExamplesTest {

	private static CommonModel qn(String name) {
		return SimXml.loadSim(new File(new File(ModelFixtures.examplesDir(), "jsim/qn_models"), name));
	}

	private static String svc(CommonModel m, String station, String cls) {
		return String.valueOf(m.getServiceTimeDistribution(m.getStationByName(station), m.getClassByName(cls)));
	}

	/** file, classes, stations, sources, sinks, servers, delays, routers, forks, joins, classSwitches, preloaded jobs, connections, measures, regions */
	@ParameterizedTest
	@CsvSource({
			"qn_models/closed_1class_2stat.jsimg,1,2,0,0,1,1,0,0,0,0,5,2,1,0",
			"qn_models/closed_2class_2stat.jsimg,2,2,0,0,1,1,0,0,0,0,15,2,1,0",
			"qn_models/closed_2class_2stat_switch.jsimg,2,3,0,0,1,1,0,0,0,1,15,3,1,0",
			"qn_models/closed_2class_3stat_switch.jsimg,2,5,0,0,3,0,0,0,0,2,3,8,37,0",
			"qn_models/open_1class_1stat_mg1fcfs.jsimg,1,3,1,1,1,0,0,0,0,0,0,2,4,0",
			"qn_models/open_1class_1stat_mg1ps.jsimg,1,3,1,1,1,0,0,0,0,0,0,2,4,0",
			"qn_models/open_1class_1stat_mm1k.jsimg,1,3,1,1,1,0,0,0,0,0,0,2,1,0",
			"qn_models/open_1class_1stat_mm1k_fcr.jsimg,1,3,1,1,1,0,0,0,0,0,0,2,1,1",
			"qn_models/open_1class_3stat_advfork.jsimg,1,7,1,1,3,0,0,1,1,0,0,8,5,0",
			"qn_models/open_1class_3stat_fork.jsimg,1,7,1,1,3,0,0,1,1,0,0,8,4,0",
			"qn_models/open_2class_3stat_fcr.jsimg,3,6,1,1,2,1,1,0,0,0,5,8,4,1",
			"qn_models/open_2class_3stat_fork_whatif.jsimg,2,7,1,1,3,0,0,1,1,0,0,8,2,0",
			"qn_models/open_3class_3stat_loadbal.jsimg,3,6,1,1,3,0,1,0,0,0,0,7,13,0",
			"qn_models/open_3class_4stat.jsimg,3,6,1,1,4,0,0,0,0,0,0,8,60,0" })
	void qnExampleStructure(String file, int classes, int stations, int sources, int sinks, int servers, int delays, int routers, int forks,
			int joins, int switches, int preload, int connections, int measures, int regions) {
		CommonModel m = SimXml.loadSim(new File(new File(ModelFixtures.examplesDir(), "jsim"), file));
		assertEquals(classes, m.getClassKeys().size(), "classes");
		assertEquals(stations, m.getStationKeys().size(), "stations");
		assertEquals(sources, m.getStationKeysSource().size(), "sources");
		assertEquals(sinks, m.getStationKeysSink().size(), "sinks");
		assertEquals(servers, m.getStationKeysServer().size(), "servers");
		assertEquals(delays, m.getStationKeysDelay().size(), "delays");
		assertEquals(routers, m.getStationKeysRouter().size(), "routers");
		assertEquals(forks, m.getStationKeysFork().size(), "forks");
		assertEquals(joins, m.getStationKeysJoin().size(), "joins");
		assertEquals(switches, m.getStationKeysClassSwitch().size(), "class switches");
		int pre = 0, conn = 0;
		for (Object s : m.getStationKeys()) {
			conn += m.getForwardConnections(s).size();
			for (Object c : m.getClassKeys()) {
				Integer p = m.getPreloadedJobs(s, c);
				pre += p == null ? 0 : p;
			}
		}
		assertEquals(preload, pre, "preloaded jobs");
		assertEquals(connections, conn, "connections");
		assertEquals(measures, m.getMeasureKeys().size(), "measures");
		assertEquals(regions, m.getRegionKeys().size(), "regions");
	}

	@Test
	void closedClassesPopulationAndPreloading() {
		CommonModel m = qn("closed_2class_3stat_switch.jsimg");
		Object c1 = m.getClassByName("Class1"), c2 = m.getClassByName("Class2");
		assertEquals(CommonConstants.CLASS_TYPE_CLOSED, m.getClassType(c1));
		assertEquals(Integer.valueOf(3), m.getClassPopulation(c1));
		assertEquals(Integer.valueOf(0), m.getClassPopulation(c2));
		assertEquals(Integer.valueOf(3), m.getPreloadedJobs(m.getStationByName("CPU 1"), c1));
		assertEquals(Integer.valueOf(0), m.getPreloadedJobs(m.getStationByName("CPU 1"), c2));
		assertEquals(m.getStationByName("CPU 1"), m.getClassRefStation(c1));
	}

	@Test
	void openClassesHaveInterarrivalDistributionAndNoPopulation() {
		CommonModel m = qn("open_3class_4stat.jsimg");
		Object c0 = m.getClassByName("Class0");
		assertEquals(CommonConstants.CLASS_TYPE_OPEN, m.getClassType(c0));
		assertNull(m.getClassPopulation(c0));
		assertEquals("exp(0.2)", String.valueOf(m.getClassDistribution(c0)));
		assertEquals("exp(0.125)", String.valueOf(m.getClassDistribution(m.getClassByName("Class1"))));
		assertEquals(m.getStationByName("Source of Customers"), m.getClassRefStation(c0));
	}

	@Test
	void serviceDistributionKindsAndParameters() {
		CommonModel m = qn("open_2class_3stat_fcr.jsimg");
		Distribution hyper = (Distribution) m.getClassDistribution(m.getClassByName("Class0"));
		assertEquals("Hyperexponential", hyper.getName());
		assertEquals(0.291, (Double) hyper.getParameter("p").getValue(), 1e-3);
		assertEquals("hyp(0.6,0.5,0.2)", svc(m, "Web Server 2", "Class1"));
		assertEquals("exp(0.5)", svc(m, "Web Server 2", "Class2"));

		CommonModel cox = qn("open_1class_1stat_mg1fcfs.jsimg");
		Distribution d = (Distribution) cox.getServiceTimeDistribution(cox.getStationByName("M/G/1-FCFS"), cox.getClassByName("Class0"));
		assertEquals("Coxian", d.getName());
		assertEquals(4.0, (Double) d.getParameter("lambda0").getValue(), 1e-12);
		assertEquals(0.5, (Double) d.getParameter("lambda1").getValue(), 1e-12);
		assertEquals(0.875, (Double) d.getParameter("phi0").getValue(), 1e-12);
	}

	@Test
	void queueStrategiesFcfsAndProcessorSharing() {
		CommonModel fcfs = qn("open_1class_1stat_mg1fcfs.jsimg");
		Object s = fcfs.getStationByName("M/G/1-FCFS"), c = fcfs.getClassByName("Class0");
		assertEquals(CommonConstants.QUEUE_STRATEGY_FCFS, fcfs.getQueueStrategy(s, c));
		assertEquals(CommonConstants.STATION_QUEUE_STRATEGY_NON_PREEMPTIVE, fcfs.getStationQueueStrategy(s));

		CommonModel ps = qn("open_1class_1stat_mg1ps.jsimg");
		Object sp = ps.getStationByName("M/G/1-PS");
		assertEquals(CommonConstants.QUEUE_STRATEGY_PS, ps.getQueueStrategy(sp, ps.getClassByName("Class0")));
		assertEquals(CommonConstants.STATION_QUEUE_STRATEGY_PSSERVER, ps.getStationQueueStrategy(sp));
	}

	@Test
	void finiteCapacityAndServerCounts() {
		CommonModel m = qn("open_1class_1stat_mm1k.jsimg");
		Object s = m.getStationByName("M/M/1/11");
		assertEquals(Integer.valueOf(11), m.getStationQueueCapacity(s));
		assertEquals(Integer.valueOf(1), m.getStationNumberOfServers(s));
		CommonModel inf = qn("open_1class_1stat_mm1k_fcr.jsimg");
		assertEquals(Integer.valueOf(-1), inf.getStationQueueCapacity(inf.getStationByName("M/M/1")));
	}

	@Test
	void routingStrategiesPerClass() {
		CommonModel m = qn("open_3class_3stat_loadbal.jsimg");
		Object router = m.getStationByName("Router 1");
		assertEquals("Probabilities", String.valueOf(m.getRoutingStrategy(router, m.getClassByName("Class0"))));
		assertEquals("Join the Shortest Queue (JSQ)", String.valueOf(m.getRoutingStrategy(router, m.getClassByName("Class1"))));
		assertEquals("Fastest Service", String.valueOf(m.getRoutingStrategy(router, m.getClassByName("Class2"))));
		Object src = m.getStationByName("Source");
		assertEquals("Shortest Response Time", String.valueOf(m.getRoutingStrategy(src, m.getClassByName("Class2"))));
		assertEquals(3, m.getForwardConnections(router).size());

		CommonModel rr = qn("open_2class_3stat_fcr.jsimg");
		assertEquals("Round Robin", String.valueOf(rr.getRoutingStrategy(rr.getStationByName("Requests"), rr.getClassByName("Class1"))));
	}

	@Test
	void stationTypesAreRecognised() {
		CommonModel m = qn("closed_2class_3stat_switch.jsimg");
		assertEquals(CommonConstants.STATION_TYPE_SERVER, m.getStationType(m.getStationByName("CPU 1")));
		assertEquals(CommonConstants.STATION_TYPE_CLASSSWITCH, m.getStationType(m.getStationByName("ClassSwitch 1-2")));
		CommonModel d = qn("closed_1class_2stat.jsimg");
		assertEquals(CommonConstants.STATION_TYPE_DELAY, d.getStationType(d.getStationByName("Delay 1")));
		CommonModel f = qn("open_1class_3stat_advfork.jsimg");
		assertEquals(CommonConstants.STATION_TYPE_FORK, f.getStationType(f.getStationByName("RAID0")));
		assertEquals(CommonConstants.STATION_TYPE_JOIN, f.getStationType(f.getStationByName("Join0")));
		assertEquals(Integer.valueOf(3), f.getStationNumberOfServers(f.getStationByName("RAID0")));
	}

	@Test
	void forkJoinWhatIfModelHasTwoClassesAndForkBranches() {
		CommonModel m = qn("open_2class_3stat_fork_whatif.jsimg");
		Object fork = m.getStationByName("RAID0");
		assertEquals(3, m.getForwardConnections(fork).size());
		assertNotNull(m.getForkStrategy(fork, m.getClassByName("Class0")));
		assertNotNull(m.getJoinStrategy(m.getStationByName("Join0"), m.getClassByName("Class1")));
	}

	@Test
	void measureTypesPerStationAndClass() {
		CommonModel m = qn("closed_2class_3stat_switch.jsimg");
		Set<String> types = new HashSet<String>();
		int global = 0, perStation = 0;
		for (Object k : m.getMeasureKeys()) {
			types.add(m.getMeasureType(k));
			assertEquals(0.99, m.getMeasureAlpha(k), 1e-9); // file stores 0.01, reader inverts
			assertEquals(0.03, m.getMeasurePrecision(k), 1e-9);
			if (m.getMeasureStation(k) == null) {
				global++;
			} else {
				perStation++;
			}
		}
		assertEquals(37, global + perStation);
		assertEquals(7, global);
		assertEquals(30, perStation);
		assertTrue(types.contains("Number of Customers"));
		assertTrue(types.contains("Utilization"));
		assertTrue(types.contains("System Throughput"));
		assertEquals(8, types.size());
	}

	@Test
	void blockingRegionConstraintsAndDropRules() {
		CommonModel m = qn("open_2class_3stat_fcr.jsimg");
		Object r = m.getRegionKeys().get(0);
		assertEquals("FCR", m.getRegionName(r));
		assertEquals(Integer.valueOf(-1), m.getRegionCustomerConstraint(r));
		assertEquals(Integer.valueOf(4), m.getRegionClassCustomerConstraint(r, m.getClassByName("Class1")));
		assertEquals(Integer.valueOf(3), m.getRegionClassCustomerConstraint(r, m.getClassByName("Class2")));
		assertFalse(m.getRegionClassDropRule(r, m.getClassByName("Class0")));

		CommonModel k = qn("open_1class_1stat_mm1k_fcr.jsimg");
		Object r1 = k.getRegionKeys().get(0);
		assertEquals("FCR - max 11 cust.", k.getRegionName(r1));
		assertEquals(Integer.valueOf(11), k.getRegionClassCustomerConstraint(r1, k.getClassByName("Class0")));
		assertTrue(k.getRegionClassDropRule(r1, k.getClassByName("Class0")));
		assertEquals(Integer.valueOf(1), k.getRegionClassWeight(r1, k.getClassByName("Class0")));
	}

	@Test
	void rootAttributesFromSimElement() {
		CommonModel m = qn("open_1class_3stat_advfork.jsimg");
		assertEquals(Long.valueOf(1), m.getSimulationSeed());
		assertFalse(m.getUseRandomSeed());
		assertEquals(Integer.valueOf(4000000), qn("open_1class_1stat_mm1k.jsimg").getMaxSimulationSamples());
	}

	@Test
	void classPriorityOfClosedClassIsRead() {
		// inline model: the original book example is not tracked in git
		CommonModel m = SimXml.parseSim(SimXml.sim("", SimXml.userClass("NodeUn", "closed", "customers=\"200\" priority=\"1\" referenceSource=\"CompServers\"")
				+ SimXml.server("CompServers", "NodeUn", "TailStrategy", 200, -1, SimXml.distr("Exponential", "ExponentialPar", "lambda", "3"))));
		Object c = m.getClassByName("NodeUn");
		assertEquals(Integer.valueOf(1), m.getClassPriority(c));
		assertEquals(Integer.valueOf(200), m.getClassPopulation(c));
		assertEquals(Integer.valueOf(200), m.getStationNumberOfServers(m.getStationByName("CompServers")));
	}
}
