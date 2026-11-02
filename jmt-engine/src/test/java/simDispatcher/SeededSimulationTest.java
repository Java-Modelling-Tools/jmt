package simDispatcher;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import QueueNet.NetFixtures;

/** Seeded end-to-end runs through the dispatcher, compared with closed-form results (5% tolerance). */
@Tag("simulation")
class SeededSimulationTest {

	private static final long SEED = 20260;
	private static final double TOL = 0.05;

	@TempDir
	Path dir;

	private static String measure(String type) {
		return "<measure alpha=\"0.01\" name=\"Queue 1_" + type + "\" nodeType=\"station\" precision=\"0.03\" referenceNode=\"Queue 1\" referenceUserClass=\"\" type=\""
				+ type + "\" verbose=\"false\"/>\n";
	}

	/** closed_1class_2stat with two more measures at Queue 1 and a larger sample cap. */
	private Map<String, Double> solveClosed(String subdir) throws Exception {
		return solve(subdir, "closed_1class_2stat",
				"<connection source=\"Delay 1\"", measure("Throughput") + measure("Response Time") + "<connection source=\"Delay 1\"",
				"maxSamples=\"5000\"", "maxSamples=\"200000\"");
	}

	/** Runs the model variant through the dispatcher; returns "type@station" to meanValue read from the engine's result file. */
	private Map<String, Double> solve(String subdir, String base, String... fromTo) throws Exception {
		Path d = java.nio.file.Files.createDirectory(dir.resolve(subdir));
		NetFixtures.writeVariant(d, base, "m", fromTo);
		DispatcherJSIMschema sim = new DispatcherJSIMschema(d.resolve("m.xml").toFile());
		sim.setSimulationSeed(SEED);
		sim.setSimulationMaxDuration(40000); // safety cap in ms; the sample cap ends the runs far earlier
		assertTrue(sim.solveHandlingExceptions());
		File out = sim.getOutputFile();
		assertNotNull(out);
		Map<String, Double> res = new TreeMap<>();
		NodeList ms = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(out).getElementsByTagName("measure");
		for (int i = 0; i < ms.getLength(); i++) {
			Element m = (Element) ms.item(i);
			res.put(m.getAttribute("type") + "@" + m.getAttribute("station"), Double.valueOf(m.getAttribute("meanValue")));
		}
		return res;
	}

	@Test
	void closedModel_sameSeedGivesIdenticalResults() throws Exception {
		Map<String, Double> a = solveClosed("a");
		Map<String, Double> b = solveClosed("b");
		assertEquals(3, a.size());
		assertEquals(a, b);
	}

	@Test
	void closedModel_meansMatchMvaWithin5Percent() throws Exception {
		// Delay (Z = 1) + single server (D = 1), N = 5, product form. Weight of n jobs at the server is 1/(5-n)!.
		// G = sum_{k=0..5} 1/k! = 2.716667; U = X = 1 - (1/120)/G = 0.996933;
		// E[n] = sum n/(5-n)! / G = 10.875 / 2.716667 = 4.003067; R = E[n] / X = 4.0154 (Little).
		double x = 0.996933, r = 4.0154;
		Map<String, Double> m = solveClosed("a");
		assertEquals(x, m.get("Utilization@Queue 1"), 0.01);
		assertEquals(x, m.get("Throughput@Queue 1"), 0.02);
		assertEquals(r, m.get("Response Time@Queue 1"), r * TOL);
	}

	@Test
	void openModel_utilizationMatchesTheory() throws Exception {
		// M/G/1, lambda = 1; service = Exp(4) + (with prob 1 - phi0 = 0.125) Exp(0.5), so E[S] = 0.25 + 0.125 * 2 = 0.5.
		// U = lambda E[S] = 0.5. E[S^2] = 0.125 + 2 * 0.25 * 0.125 * 2 + 0.125 * 8 = 1.25, so by Pollaczek-Khinchine
		// Wq = lambda E[S^2] / (2 (1 - U)) = 1.25 and R = Wq + E[S] = 1.75, N = lambda R = 1.75.
		Map<String, Double> m = solve("a", "open_1class_1stat_mg1fcfs",
				"disableStatisticStop=\"true\"", "disableStatisticStop=\"true\"", "maxSamples=\"5000\"", "maxSamples=\"200000\"");
		assertEquals(0.5, m.get("Utilization@M/G/1-FCFS"), 0.5 * TOL);
		assertEquals(1.0, m.get("Throughput@M/G/1-FCFS"), 1.0 * TOL);
		assertEquals(1.75, m.get("Response Time@M/G/1-FCFS"), 1.75 * TOL);
		assertEquals(1.75, m.get("Number of Customers@M/G/1-FCFS"), 1.75 * TOL);
	}
}
