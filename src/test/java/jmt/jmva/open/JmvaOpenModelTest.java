package jmt.jmva.open;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import jmt.jmva.analytical.CommandLineSolver;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class JmvaOpenModelTest {
	
	
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

}
