package analytical;

import static org.junit.jupiter.api.Assertions.*;
import static solvers.dispatchers.DispatchFixtures.read;
import static solvers.dispatchers.DispatchFixtures.repairman;
import static solvers.dispatchers.DispatchFixtures.write;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import solvers.multiSolverAlgorithms.MultiFixtures;
import solvers.SolverAlgorithm;
import solvers.dispatchers.SolverDispatcher;

/** CommandLineSolver never calls System.exit, so it is tested directly; the Jmt main in jmt-app is out of this module. */
class CommandLineSolverTest {

	private static final SolverAlgorithm EXACT = SolverAlgorithm.EXACT;

	@Test
	void solvesAFileInPlaceAndWritesTheResults(@TempDir File dir) throws Exception {
		File f = write(repairman(2), new File(dir, "m.xml"));
		assertTrue(new CommandLineSolver().solve(f));
		ExactModel back = read(f);
		assertTrue(back.hasResults());
		SolverAlgorithm alg = back.getAlgorithmType();
		assertEquals(2 / 5.8, back.getThroughput(alg)[0][0][0], 1e-9);
		assertEquals(2.8, back.getResidTimes(alg)[0][0][0], 1e-9);
		assertEquals(2.0, back.getQueueLen(alg)[0][0][0] + back.getQueueLen(alg)[1][0][0], 1e-9);
	}

	@Test
	void solvesByPathAndExtensionIsCaseInsensitive(@TempDir File dir) throws Exception {
		File f = write(repairman(1), new File(dir, "M.XML"));
		assertTrue(new CommandLineSolver().solve(f.getPath()));
		ExactModel back = read(f);
		assertEquals(0.2, back.getThroughput(back.getAlgorithmType())[0][0][0], 1e-9);
	}

	@Test
	void refusesWrongExtensionMissingFileAndDirectory(@TempDir File dir) throws Exception {
		CommandLineSolver s = new CommandLineSolver();
		File wrongExt = write(repairman(1), new File(dir, "m.jmva"));
		assertFalse(s.solve(wrongExt));
		assertFalse(read(wrongExt).hasResults(), "file untouched");
		assertFalse(s.solve(new File(dir, "missing.xml")));
		File asDir = new File(dir, "d.xml");
		assertTrue(asDir.mkdir());
		assertFalse(s.solve(asDir));
	}

	@Test
	void malformedModelReturnsFalseAndLeavesTheFile(@TempDir File dir) throws Exception {
		File f = new File(dir, "bad.xml");
		Files.write(f.toPath(), "<model><classes".getBytes("UTF-8"));
		assertFalse(new CommandLineSolver().solve(f));
		assertEquals("<model><classes", new String(Files.readAllBytes(f.toPath()), "UTF-8"));
	}

	@Test
	void unsolvableModelReturnsFalse(@TempDir File dir) throws Exception {
		// saturated open queue
		ExactModel sat = MultiFixtures.model(new int[] { MultiFixtures.LI }, new int[] { MultiFixtures.OPEN }, new double[] { 0.6 }, new double[][] { { 2 } });
		assertFalse(new CommandLineSolver().solve(write(sat, new File(dir, "sat.xml"))));
	}

	@Test
	void exposesItsDispatcher() {
		CommandLineSolver s = new CommandLineSolver();
		assertNotNull(s.getSolverDispatcher());
		assertEquals(".xml", CommandLineSolver.MODEL_EXTENSION);
	}

	// ---- examples/jmva, read-only; copied to a temp dir before solving -------------------------------------------

	private static Path examples() {
		for (String p : new String[] { "examples/jmva", "../examples/jmva" }) {
			if (Files.isDirectory(Paths.get(p))) {
				return Paths.get(p);
			}
		}
		return null;
	}

	@Test
	void shippedExamplesSolveAndReproduceTheirStoredResults(@TempDir File dir) throws Exception {
		Path ex = examples();
		Assumptions.assumeTrue(ex != null, "examples/jmva not found from " + new File(".").getAbsolutePath() + "; skipping");
		List<Path> files = new ArrayList<Path>();
		try (java.util.stream.Stream<Path> st = Files.list(ex)) {
			st.filter(p -> p.toString().endsWith(".jmva")).sorted().forEach(files::add);
		}
		Assumptions.assumeFalse(files.isEmpty(), "no .jmva files in " + ex);
		int compared = 0;
		for (Path p : files) {
			File copy = new File(dir, p.getFileName().toString().replace(".jmva", ".xml"));
			Files.copy(p, copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
			ExactModel stored = read(copy);
			assertTrue(new CommandLineSolver().solve(copy), p.toString());
			ExactModel fresh = read(copy);
			assertTrue(fresh.hasResults(), p.toString());
			for (SolverAlgorithm alg : fresh.getQueueLen().keySet()) {
				double[][][] q = fresh.getQueueLen(alg), x = fresh.getThroughput(alg), r = fresh.getResidTimes(alg);
				for (int st = 0; st < q.length; st++) {
					for (int cl = 0; cl < q[st].length; cl++) {
						for (int it = 0; it < q[st][cl].length; it++) {
							String where = p.getFileName() + " " + alg + " st" + st + " cl" + cl + " it" + it;
							assertTrue(q[st][cl][it] >= -1e-9 && !Double.isNaN(q[st][cl][it]) && !Double.isInfinite(q[st][cl][it]), "Q " + where);
							assertTrue(x[st][cl][it] >= 0 && !Double.isNaN(x[st][cl][it]), "X " + where);
							assertTrue(r[st][cl][it] >= 0 && !Double.isNaN(r[st][cl][it]), "R " + where);
						}
					}
				}
				// the same algorithm's results stored in the shipped file are the oracle
				if (stored.hasResults() && stored.getQueueLen().containsKey(alg)) {
					double[][][] sq = stored.getQueueLen(alg);
					for (int st = 0; st < q.length; st++) {
						for (int cl = 0; cl < q[st].length; cl++) {
							for (int it = 0; it < q[st][cl].length; it++) {
								assertEquals(sq[st][cl][it], q[st][cl][it], 1e-6 * Math.max(1, Math.abs(sq[st][cl][it])),
										p.getFileName() + " stored Q st" + st + " cl" + cl + " it" + it);
								compared++;
							}
						}
					}
				}
			}
		}
		assertTrue(compared > 0, "at least one shipped result compared");
	}

	@Test
	void solverDispatcherAndCommandLineAgree(@TempDir File dir) throws Exception {
		ExactModel direct = repairman(3);
		new SolverDispatcher().solve(direct);
		File f = write(repairman(3), new File(dir, "m.xml"));
		assertTrue(new CommandLineSolver().solve(f));
		ExactModel back = read(f);
		assertEquals(direct.getThroughput(EXACT)[0][0][0], back.getThroughput(back.getAlgorithmType())[0][0][0], 1e-9);
	}
}
