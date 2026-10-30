import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import analytical.CommandLineSolver;
import solvers.SolverMulti;
import solvers.multiSolverAlgorithms.SolverMultiClosedMVA;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import log.SimEventTracer;
import java.util.Map;
import java.util.HashMap;

public class CommandLineTest {

	@Test
	public void basicJMVAtest() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();

		File input = File.createTempFile("input", ".xml");
		File expected = new File(getClass().getResource("basicExpectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("basicInputData.xml").toURI()), input);

		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));
	}

	@Test
	public void basicJMVAtestUsingPath() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();

		File input = File.createTempFile("input", ".xml");
		File expected = new File(getClass().getResource("basicExpectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("basicInputData.xml").toURI()), input);

		assertTrue("Solver was not able to solve file!", solver.solve(input.getAbsolutePath()));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));
	}

	@Test
	public void loadDependentJMVAtest() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();

		File input = File.createTempFile("inputloadDep", ".xml");
		File expected = new File(getClass().getResource("loaddepExpectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("loaddepInputData.xml").toURI()), input);

		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));
	}

	@Test
	public void whatif_popmixJMVAtest() throws URISyntaxException, IOException {
		CommandLineSolver solver = new CommandLineSolver();

		File input = File.createTempFile("inputPopmix", ".xml");
		File expected = new File(getClass().getResource("whatif_popmixExpectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("whatif_popmixInputData.xml").toURI()), input);

		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));
	}

	@Test
	public void whatif_servtimeJMVAtest() throws URISyntaxException, IOException {
		CommandLineSolver solver = new CommandLineSolver();

		File input = File.createTempFile("inputServtime", ".xml");
		File expected = new File(getClass().getResource("whatif_servtimeExpectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("whatif_servtimeInputData.xml").toURI()), input);

		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));
	}

	@Test
	public void stat_mixedJMVAtest() throws URISyntaxException, IOException {
		CommandLineSolver solver = new CommandLineSolver();

		File input = File.createTempFile("inputStatMixed", ".xml");
		File expected = new File(getClass().getResource("stat_mixedExpectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("stat_mixedInputData.xml").toURI()), input);

		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));
	}

	@Test
	public void fileNotXmlFailedJMVAtest() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();

		assertFalse("Solver should not be able to solve this!\n" +
				"It can only handle xml files", solver.solve("nonXmlFile"));
	}

	@Test
	public void fileNotFoundFailedJMVAtest() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();

		assertFalse("Solver should not be able to solve this!\n" +
				"File does not (should not) exist", solver.solve("nonExistentFile.xml"));
	}

	@Test
	public void nullFileFailedJMVAtest() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();

		assertFalse("Solver should not be able to solve this!\n" +
				"File does not (should not) exist", solver.solve((String) null));
	}

	@Test
	public void testLI() {
		int nCust = 3;
		int nCent = 4;
		String[] name = new String[nCent];
		int[] type = new int[nCent];
		double[][][] timeMulti = new double[nCent][nCust][1];
		double[][] visitMulti = new double[nCent][nCust];
		int[] popVect = new int[nCust];

		for (int i = 0; i < nCent; i++) {
			name[i] = "Service Center " + i;
			type[i] = SolverMulti.LI;
		}

		popVect[0] = 10;
		popVect[1] = 15;
		popVect[2] = 13;

		//not a very general test but I don't want to write a lot of data..
		for (int i = 0; i < nCent; i++) {
			double local = (i + 1) * 0.5;
			for (int j = 0; j < nCust; j++) {
				timeMulti[i][j][0] = local * (j + 1) * 0.1;
			}
		}

		visitMulti[0][0] = 1;
		visitMulti[0][1] = 0.6;
		visitMulti[0][2] = 5;
		visitMulti[1][0] = 0.12;
		visitMulti[1][1] = 0.81;
		visitMulti[1][2] = 0.61;
		visitMulti[2][0] = 0.4;
		visitMulti[2][1] = 0.6;
		visitMulti[2][2] = 0.49;
		visitMulti[3][0] = 0.86;
		visitMulti[3][1] = 0.38;
		visitMulti[3][2] = 0.15;

		SolverMultiClosedMVA solver = new SolverMultiClosedMVA(nCust, nCent);
		if (!solver.input(name, type, timeMulti, visitMulti, popVect)) {
			fail("Vector Lengths not exact");
		}
		solver.solve();
		System.out.println("solved");
		System.out.println(solver);
		if ((Math.abs(2.667 - solver.getThroughput(0, 0)) >= 0.001) || (Math.abs(1.794 - solver.getThroughput(0, 1)) >= 0.001)
				|| (Math.abs(4.576 - solver.getThroughput(0, 2)) >= 0.001) || (Math.abs(0.320 - solver.getThroughput(1, 0)) >= 0.001)
				|| (Math.abs(2.421 - solver.getThroughput(1, 1)) >= 0.001) || (Math.abs(0.558 - solver.getThroughput(1, 2)) >= 0.001)
				|| (Math.abs(1.067 - solver.getThroughput(2, 0)) >= 0.001) || (Math.abs(1.794 - solver.getThroughput(2, 1)) >= 0.001)
				|| (Math.abs(0.448 - solver.getThroughput(2, 2)) >= 0.001) || (Math.abs(2.294 - solver.getThroughput(3, 0)) >= 0.001)
				|| (Math.abs(1.136 - solver.getThroughput(3, 1)) >= 0.001) || (Math.abs(0.137 - solver.getThroughput(3, 2)) >= 0.001)
				|| (Math.abs(0.133 - solver.getUtilization(0, 0)) >= 0.001) || (Math.abs(0.179 - solver.getUtilization(0, 1)) >= 0.001)
				|| (Math.abs(0.686 - solver.getUtilization(0, 2)) >= 0.001) || (Math.abs(0.032 - solver.getUtilization(1, 0)) >= 0.001)
				|| (Math.abs(0.484 - solver.getUtilization(1, 1)) >= 0.001) || (Math.abs(0.167 - solver.getUtilization(1, 2)) >= 0.001)
				|| (Math.abs(0.160 - solver.getUtilization(2, 0)) >= 0.001) || (Math.abs(0.538 - solver.getUtilization(2, 1)) >= 0.001)
				|| (Math.abs(0.201 - solver.getUtilization(2, 2)) >= 0.001) || (Math.abs(0.458 - solver.getUtilization(3, 0)) >= 0.001)
				|| (Math.abs(0.454 - solver.getUtilization(3, 1)) >= 0.001) || (Math.abs(0.082 - solver.getUtilization(3, 2)) >= 0.001)) {
			fail("Test failed");
		}
	}

	@Test
	public void testLI_2C_2S() {
		int nCust = 3;
		int nCent = 4;
		String[] name = new String[nCent];
		int[] type = new int[nCent];
		double[][][] timeMulti = new double[nCent][nCust][1];
		double[][] visitMulti = new double[nCent][nCust];
		int[] popVect = new int[nCust];

		for (int i = 0; i < nCent; i++) {
			name[i] = "Service Center " + i;
			type[i] = SolverMulti.LI;
		}

		popVect[0] = 10;
		popVect[1] = 15;
		popVect[2] = 13;

		//not a very general test but I don't want to write a lot of data..
		for (int i = 0; i < nCent; i++) {
			double local = (i + 1) * 0.5;
			for (int j = 0; j < nCust; j++) {
				timeMulti[i][j][0] = local * (j + 1) * 0.1;
			}
		}

		visitMulti[0][0] = 1;
		visitMulti[0][1] = 0.6;
		visitMulti[0][2] = 5;
		visitMulti[1][0] = 0.12;
		visitMulti[1][1] = 0.81;
		visitMulti[1][2] = 0.61;
		visitMulti[2][0] = 0.4;
		visitMulti[2][1] = 0.6;
		visitMulti[2][2] = 0.49;
		visitMulti[3][0] = 0.86;
		visitMulti[3][1] = 0.38;
		visitMulti[3][2] = 0.15;

		SolverMultiClosedMVA solver = new SolverMultiClosedMVA(nCust, nCent);
		if (!solver.input(name, type, timeMulti, visitMulti, popVect)) {
			fail("Vector Lengths not exact");
		}
		solver.solve();
		System.out.println("solved");
		System.out.println(solver);
		if ((Math.abs(2.667 - solver.getThroughput(0, 0)) >= 0.001) || (Math.abs(1.794 - solver.getThroughput(0, 1)) >= 0.001)
				|| (Math.abs(4.576 - solver.getThroughput(0, 2)) >= 0.001) || (Math.abs(0.320 - solver.getThroughput(1, 0)) >= 0.001)
				|| (Math.abs(2.421 - solver.getThroughput(1, 1)) >= 0.001) || (Math.abs(0.558 - solver.getThroughput(1, 2)) >= 0.001)
				|| (Math.abs(1.067 - solver.getThroughput(2, 0)) >= 0.001) || (Math.abs(1.794 - solver.getThroughput(2, 1)) >= 0.001)
				|| (Math.abs(0.448 - solver.getThroughput(2, 2)) >= 0.001) || (Math.abs(2.294 - solver.getThroughput(3, 0)) >= 0.001)
				|| (Math.abs(1.136 - solver.getThroughput(3, 1)) >= 0.001) || (Math.abs(0.137 - solver.getThroughput(3, 2)) >= 0.001)
				|| (Math.abs(0.133 - solver.getUtilization(0, 0)) >= 0.001) || (Math.abs(0.179 - solver.getUtilization(0, 1)) >= 0.001)
				|| (Math.abs(0.686 - solver.getUtilization(0, 2)) >= 0.001) || (Math.abs(0.032 - solver.getUtilization(1, 0)) >= 0.001)
				|| (Math.abs(0.484 - solver.getUtilization(1, 1)) >= 0.001) || (Math.abs(0.167 - solver.getUtilization(1, 2)) >= 0.001)
				|| (Math.abs(0.160 - solver.getUtilization(2, 0)) >= 0.001) || (Math.abs(0.538 - solver.getUtilization(2, 1)) >= 0.001)
				|| (Math.abs(0.201 - solver.getUtilization(2, 2)) >= 0.001) || (Math.abs(0.458 - solver.getUtilization(3, 0)) >= 0.001)
				|| (Math.abs(0.454 - solver.getUtilization(3, 1)) >= 0.001) || (Math.abs(0.082 - solver.getUtilization(3, 2)) >= 0.001)) {
			fail("Test failed");
		}		
	}

	@Test
	public void testTraceOptionRequiresJar() {
		try {
			SimEventTracer.disable();
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = false;

			Map<String, String> options = new HashMap<String, String>();
			options.put(jmt.commandline.Jmt.OPTION_TRACE, null);

			assertTrue(jmt.commandline.Jmt.isTraceOptionRequested(options));
			boolean handled = jmt.commandline.Jmt.handleTraceOption(options);
			assertFalse("Trace activation without JAR should fail", handled);
			assertFalse("Tracer should not be enabled without JAR", SimEventTracer.isEnabled());

			// Now test with JAR override = true
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = true;
			handled = jmt.commandline.Jmt.handleTraceOption(options);
			assertTrue("Trace activation with JAR should succeed", handled);
			assertTrue("Tracer should be enabled with JAR", SimEventTracer.isEnabled());
		} finally {
			SimEventTracer.disable();
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = null;
		}
	}
	@Test
	public void testCliTraceOptionWithVmOptionInIde() {
		String oldVisual = System.getProperty("jmt.trace.visual");
		try {
			SimEventTracer.disable();
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = false;
			System.setProperty("jmt.trace.visual", "true");

			Map<String, String> options = new HashMap<String, String>();
			options.put(jmt.commandline.Jmt.OPTION_TRACE, null);

			boolean handled = jmt.commandline.Jmt.handleTraceOption(options);
			assertTrue("Trace option must be handled when VM option is active even without JAR", handled);
			assertTrue("Tracer must be enabled", SimEventTracer.isEnabled());
		} finally {
			SimEventTracer.disable();
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = null;
			if (oldVisual != null) System.setProperty("jmt.trace.visual", oldVisual);
			else System.clearProperty("jmt.trace.visual");
		}
	}
	@Test
	public void testGraphStartScreenSimDispatchWithTrace() throws Exception {
		try {
			SimEventTracer.disable();
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = true;
			File tempModel = new File("../examples/jsim/qn_models/closed_1class_2stat.jsimg");
			if (!tempModel.exists()) {
				tempModel = new File("examples/jsim/qn_models/closed_1class_2stat.jsimg");
			}
			common.startScreen.GraphStartScreen.main(new String[]{"sim", tempModel.getAbsolutePath(), "-trace"});
			assertTrue("Tracer must be enabled when running sim with -trace via GraphStartScreen", SimEventTracer.isEnabled());
			File resultFile = new File(tempModel.getAbsolutePath() + "-result.jsim");
			if (resultFile.exists()) {
				resultFile.delete();
			}
		} finally {
			SimEventTracer.disable();
			SimEventTracer.IS_RUNNING_FROM_JAR_OVERRIDE = null;
		}
	}
}
