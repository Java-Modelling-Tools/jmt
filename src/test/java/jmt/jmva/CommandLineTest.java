package jmt.jmva;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import jmt.jmva.analytical.CommandLineSolver;
import jmt.jmva.analytical.solvers.SolverMulti;
import jmt.jmva.analytical.solvers.SolverMultiClosedMVA;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

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
	public void randomModel_closed_1() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_1", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_1-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_1-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_2() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_2", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_2-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_2-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_3() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_3", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_3-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_3-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_4() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_4", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_4-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_4-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_5() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_5", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_5-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_5-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_6() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_6", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_6-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_6-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_7() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_7", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_7-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_7-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_8() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_8", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_8-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_8-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_9() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_9", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_9-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_9-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_10() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_10", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_10-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_10-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_11() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_11", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_11-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_11-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_12() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_12", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_12-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_12-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_recal() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_recal", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_recal-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_recal-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_mom() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_mom", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_mom-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_mom-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_closed_comom() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_closed_comom", ".xml");
		File expected = new File(getClass().getResource("randomModel_closed_comom-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_closed_comom-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_mixed_113() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_mixed_113", ".xml");
		File expected = new File(getClass().getResource("randomModel_mixed_113-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_mixed_113-input.xml").toURI()), input);
		
		assertTrue("Solver was not able to solve file!", solver.solve(input));
		assertTrue(": Real output differs from expected!\n"
				+ "Expected: \n" + FileUtils.readFileToString(expected)
				+ "\nActual: \n" + FileUtils.readFileToString(input), 
				FileUtils.contentEquals(input, expected));			
	}
	
	@Test
	public void randomModel_open_57() throws IOException, URISyntaxException {
		CommandLineSolver solver = new CommandLineSolver();
		
		File input = File.createTempFile("randomModel_open_57", ".xml");
		File expected = new File(getClass().getResource("randomModel_open_57-expectedOutput.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("randomModel_open_57-input.xml").toURI()), input);
		
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
		System.out.print(solver);
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
		System.out.print(solver);
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

}
