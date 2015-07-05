package jmt.engine;

import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import jmt.commandline.Jmt;

public class DefaultExamplesTest {

	@Test
	public void test_1cl_1stat_mm1k_fcr() throws Exception {
		
	File input = File.createTempFile("1cl_1stat_mm1k_fcr", ".xml");
		File result = File.createTempFile("1cl_1stat_mm1k_fcr-result", ".xml");
		File expected = new File(getClass().getResource("1cl_1stat_mm1k_fcr-output.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("1cl_1stat_mm1k_fcr-input.xml").toURI()), input);

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
	
	@Test
	public void test_1cl_1stat_mm1k() throws Exception {
		
	File input = File.createTempFile("1cl_1stat_mm1k", ".xml");
		File result = File.createTempFile("1cl_1stat_mm1k-result", ".xml");
		File expected = new File(getClass().getResource("1cl_1stat_mm1k-output.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("1cl_1stat_mm1k-input.xml").toURI()), input);

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
	
	@Test
	public void test_3cl_5stat_loadbal() throws Exception {
		
	File input = File.createTempFile("3cl_5stat_loadbal", ".xml");
		File result = File.createTempFile("3cl_5stat_loadbal-result", ".xml");
		File expected = new File(getClass().getResource("3cl_5stat_loadbal-output.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("3cl_5stat_loadbal-input.xml").toURI()), input);

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
	
	@Test
	public void test_3cl_6stat_open() throws Exception {
		
	File input = File.createTempFile("3cl_6stat_open", ".xml");
		File result = File.createTempFile("3cl_6stat_open-result", ".xml");
		File expected = new File(getClass().getResource("3cl_6stat_open-output.xml").toURI());
		FileUtils.copyFile(new File(getClass().getResource("3cl_6stat_open-input.xml").toURI()), input);

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


