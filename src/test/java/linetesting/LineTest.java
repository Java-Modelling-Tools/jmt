package linetesting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

public class LineTest {

	final static String TEST_FILES_BASE_FOLDER_PATH = "src/test/resources/linetesting/input";
	final static String JSIM_TESTS_FOLDER_NAME = "jsim"; // must be same as in TestUtil

	final static String JSIM_LINE_TEST_MODEL_FILE_NAME = "model.jsim";
	final static String JSIM_LINE_TEST_MODEL_RESULTS_FILE_NAME = "model.jsim-result.jsim";

	// Path to where Maven compiles main code
	final static File CLASSES_DIRECTORY = new File("target/classes");
	final static String OUTPUT_FOLDER_PATH = "src/test/resources/linetesting/output";
	final static String OUTPUT_CSV_NAME = "full_detailed_coverage.csv";
	final static String TIMEOUT_CSV_NAME = "timeouts.csv";

	// Thread-safe list to collect timed-out files
	static List<String> timedOutFiles = Collections.synchronizedList(new ArrayList<>());

	// We will build the full path dynamically in setup
	static String FULL_CSV_PATH;

	final static String[] EXCLUDED_PACKAGES = { "jmt/framework/gui", "jmt/gui", "jmt/jmva/gui", "jmt/commandline" };

	@BeforeAll
	static void setupCsv() throws IOException {
		if (!CLASSES_DIRECTORY.exists()) {
			throw new IOException("Classes dir not found. Run 'mvn compile' first.");
		}

		// Create Output Folder if it doesn't exist
		File outputFolder = new File(OUTPUT_FOLDER_PATH);
		if (!outputFolder.exists()) {
			boolean created = outputFolder.mkdirs();
			if (!created) {
				throw new IOException("Failed to create output directory: " + outputFolder.getAbsolutePath());
			}
		}

		// Set the full path variable
		FULL_CSV_PATH = new File(outputFolder, OUTPUT_CSV_NAME).getAbsolutePath();

		// Initialize the CSV
		try (PrintWriter writer = new PrintWriter(new FileWriter(FULL_CSV_PATH))) {
			writer.println(
					"TEST_ID,GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED");
		}

		System.out.println("Coverage report will be written to: " + FULL_CSV_PATH);
	}

	@AfterAll
	static void saveTimeoutReport() {
		// Even if the list is empty, we check to ensure we don't create an empty file
		// unnecessarily
		// (or you can create an empty one to show 0 timeouts, logic below creates it
		// only if timeouts exist)
		if (!timedOutFiles.isEmpty()) {
			File timeoutFile = new File(OUTPUT_FOLDER_PATH, TIMEOUT_CSV_NAME);
			try (PrintWriter writer = new PrintWriter(new FileWriter(timeoutFile))) {
				// CSV Header
				writer.println("FILE_NAME,STATUS");

				for (String fileName : timedOutFiles) {
					writer.println(fileName + ",TIMEOUT");
				}
				System.out.println("Timeout report saved to: " + timeoutFile.getAbsolutePath());
			} catch (IOException e) {
				System.err.println("Failed to save timeout report.");
				e.printStackTrace();
			}
		} else {
			System.out.println("No timeouts detected. No timeout CSV generated.");
		}
	}

	static Stream<File> jsimTestProvider() {
		String jsimTestsFolder = TEST_FILES_BASE_FOLDER_PATH + "/" + JSIM_TESTS_FOLDER_NAME;
		File[] entries = FileUtil.listAllFilesAndFoldersInFolder(jsimTestsFolder);
		if (entries == null)
			return Stream.empty();
		return Stream.of(entries);
	}

	@ParameterizedTest
	@MethodSource("jsimTestProvider")
	void testLine(File jsimTestFile) throws IOException {

		// RESET coverage before running the specific test
		CoverageTracker.reset();

		try {

			Assertions.assertTimeoutPreemptively(Duration.ofMinutes(5), () -> {

				// Define the format you want (e.g., 2023-10-25 14:30:00)
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				String timestamp = LocalDateTime.now().format(formatter);

				if (jsimTestFile.isDirectory()) {

					// Check if it's a line test
					File jsimLineTestModelFile = FileUtil.getFileInFolder(jsimTestFile, JSIM_LINE_TEST_MODEL_FILE_NAME);
					File jsimLineTestModelResultsFile = FileUtil.getFileInFolder(jsimTestFile,
							JSIM_LINE_TEST_MODEL_RESULTS_FILE_NAME);
					if (jsimLineTestModelFile != null && jsimLineTestModelResultsFile != null) {
						System.out.println("[" + timestamp + "] Running test on JSIMGraph line test: "
								+ jsimTestFile.getAbsolutePath());
						assertTrue(
								TestUtil.testLineJSIMGraphModel(jsimLineTestModelFile, jsimLineTestModelResultsFile));
					}

				} else {
					System.out.println(
							"[" + timestamp + "] Running test on JSIMGraph test: " + jsimTestFile.getAbsolutePath());
					assertTrue(TestUtil.testJSIMGraphModel(jsimTestFile));
				}

			}, "Timeout exceeded for file: " + jsimTestFile.getName());

		} catch (AssertionFailedError e) {
			// Check if this error was caused by a timeout
			if (e.getMessage() != null && e.getMessage().contains("execution timed out")) {
				System.err.println("!!! TIMEOUT DETECTED for file: " + jsimTestFile.getName());
				timedOutFiles.add(jsimTestFile.getName());
			}
			throw e; // Rethrow to ensure the test is marked as failed in JUnit

		} finally {
			if (FULL_CSV_PATH != null) {
				// CAPTURE & APPEND coverage stats even if test fails
				CoverageTracker.calculateAndAppend(jsimTestFile.getName(), CLASSES_DIRECTORY, FULL_CSV_PATH,
						EXCLUDED_PACKAGES);
			}
		}

	}

}