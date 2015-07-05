package jmt.engine;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import jmt.jmva.analytical.CommandLineSolver;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TestClassSwitch {
	
	/**
	 * 
	 */
	@Test
    public void testCS4ScwetEx2_3Queue2CS() throws IOException, URISyntaxException {
            CommandLineSolver solver = new CommandLineSolver();

            File input = File.createTempFile("testCS4ScwetEx2_3Queue2CS", ".xml");
            File expected = new File(getClass().getResource("testCS4ScwetEx2_3Queue2CS-expectedOutput.xml").toURI());
            FileUtils.copyFile(new File(getClass().getResource("testCS4ScwetEx2_3Queue2CS-input.xml").toURI()), input);

            assertTrue("Solver was not able to solve file!", solver.solve(input));
            assertTrue(": Real output differs from expected!\n"
                            + "Expected: \n" + FileUtils.readFileToString(expected)
                            + "\nActual: \n" + FileUtils.readFileToString(input),
                            FileUtils.contentEquals(input, expected));
    }


}

