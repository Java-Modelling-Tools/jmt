package jmt.engine;

import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import jmt.commandline.Jmt;

public class ClassSwitchTest {

	@Test
	public void testCS4SchwetEx2() throws Exception {
		
	File input = File.createTempFile("testCS4SchwetEx2", ".xml");
		File result = File.createTempFile("testCS4SchwetEx2-result", ".xml");
		File expected = new File(getClass().getResource("testCS4SchwetEx2-output.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("testCS4SchwetEx2-input.xml").toURI()), input);

		String[] args = {"","","-maxtime","60","-seed","1"};
		Map<String,String> options = Jmt.parseParameters(args, 2);
		assertTrue("Solver was not able to solve file!", Jmt.sim(input, result, options)); 
		
		/* This part of the code removes the first row from the result file */
		Scanner scanner = new Scanner(result);
		ArrayList<String> coll = new ArrayList<String>();
		scanner.nextLine();
		scanner.nextLine();
		while (scanner.hasNextLine()) {
		    String line = scanner.nextLine();
		    coll.add(line);
		}

		scanner.close();

		FileWriter writer = new FileWriter(result);
		for (String line : coll) {
		    writer.write(line);
		}

		writer.close();
		/* The row has now been removed */
		
		String strExp = FileUtils.readFileToString(expected, "utf-8");
		String strRes = FileUtils.readFileToString(result, "utf-8");
		String strDiff = StringUtils.difference(strExp,strRes);
			
		assertTrue(": Real output differs from expected!\n"	+ "Expected: \n" + strExp + "\nActual: \n" + strRes + "\nDiff: \n" + strDiff, strDiff.length()==0);
			
	}

}


