package jmt.jmva;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import jmt.analytical.CommandLineSolver;

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

}
