package linetesting;

import static linetesting.TestConstants.CLASSES_DIRECTORY;
import static linetesting.TestConstants.EXCLUDED_PACKAGES;
import static linetesting.TestConstants.JMVA_TESTS_FOLDER_NAME;
import static linetesting.TestConstants.JSIMWIZ_TESTS_FOLDER_NAME;
import static linetesting.TestConstants.JSIM_LINE_TEST_MODEL_FILE_NAME;
import static linetesting.TestConstants.JSIM_LINE_TEST_MODEL_RESULTS_FILE_NAME;
import static linetesting.TestConstants.JSIM_TESTS_FOLDER_NAME;
import static linetesting.TestConstants.OUTPUT_FOLDER_PATH;
import static linetesting.TestConstants.TEST_FILES_BASE_FOLDER_PATH;
import static linetesting.TestConstants.TEST_TIMEOUT;
import static linetesting.TestConstants.TIMEOUT_CSV_NAME;
import static linetesting.TestConstants.TIMESTAMP_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

public class LineTest {

	// We will build the full path dynamically in setup
	static String JSIM_CSV_PATH;
	static String JMVA_CSV_PATH;
	static String JSIMWIZ_CSV_PATH;
	static String TIMEOUT_CSV_PATH;

	@BeforeAll
	static void setupCsvs() throws IOException {
		if (!CLASSES_DIRECTORY.exists()) {
			throw new IOException("Classes dir not found. Run 'mvn compile' first.");
		}

		File outputFolder = new File(OUTPUT_FOLDER_PATH);
		if (!outputFolder.exists() && !outputFolder.mkdirs()) {
			throw new IOException("Failed to create output directory: " + outputFolder.getAbsolutePath());
		}

		// Initialize individual CSVs using the helper
		JSIM_CSV_PATH = initCoverageCsv(outputFolder, "jsim_coverage.csv");
		JMVA_CSV_PATH = initCoverageCsv(outputFolder, "jmva_coverage.csv");
		JSIMWIZ_CSV_PATH = initCoverageCsv(outputFolder, "jsimwiz_coverage.csv");

		// Setup Timeout CSV
		TIMEOUT_CSV_PATH = new File(outputFolder, TIMEOUT_CSV_NAME).getAbsolutePath();
		try (PrintWriter writer = new PrintWriter(new FileWriter(TIMEOUT_CSV_PATH))) {
			writer.println("FILE_NAME,STATUS,TIMESTAMP");
		}
	}

	// Helper method to create a CSV and write the header
	private static String initCoverageCsv(File folder, String fileName) throws IOException {
		String absolutePath = new File(folder, fileName).getAbsolutePath();
		try (PrintWriter writer = new PrintWriter(new FileWriter(absolutePath))) {
			writer.println("TEST_ID,GROUP,PACKAGE,CLASS," + "INSTRUCTION_MISSED,INSTRUCTION_COVERED,"
					+ "BRANCH_MISSED,BRANCH_COVERED," + "LINE_MISSED,LINE_COVERED,"
					+ "COMPLEXITY_MISSED,COMPLEXITY_COVERED," + "METHOD_MISSED,METHOD_COVERED,"
					+ "SPECIFIC_LINES_COVERED,SPECIFIC_LINES_MISSED");
		}
		System.out.println("Coverage report ready at: " + absolutePath);
		return absolutePath;
	}

	private static Stream<File> testProvider(String testFolderName) {
		String jsimTestsFolder = TEST_FILES_BASE_FOLDER_PATH + "/" + testFolderName;
		File[] entries = FileUtil.listAllFilesAndFoldersInFolder(jsimTestsFolder);
		if (entries == null)
			return Stream.empty();
		return Stream.of(entries);
	}

	static Stream<File> jsimTestProvider() {
		return testProvider(JSIM_TESTS_FOLDER_NAME);
	}

	static Stream<File> jmvaTestProvider() {
		return testProvider(JMVA_TESTS_FOLDER_NAME);
	}

	static Stream<File> jsimWizTestProvider() {
		return testProvider(JSIMWIZ_TESTS_FOLDER_NAME);
	}

	private void runWithCoverage(File testFile, String csvPath, Executable testLogic) {
		CoverageTracker.reset(); // Clear previous data

		try {
			Assertions.assertTimeoutPreemptively(Duration.ofMinutes(TEST_TIMEOUT), testLogic,
					"Timeout exceeded for file: " + testFile.getName());
		} catch (AssertionFailedError e) {
			// Catch and log timeouts to the timeout CSV
			if (e.getMessage() != null && e.getMessage().contains("execution timed out")) {
				System.err.println("!!! TIMEOUT DETECTED for file: " + testFile.getName());
				try (PrintWriter writer = new PrintWriter(new FileWriter(TIMEOUT_CSV_PATH, true))) {
					String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
					writer.println(testFile.getName() + ",TIMEOUT," + timestamp);
				} catch (IOException ioException) {
					System.err.println("Failed to write timeout to CSV: " + ioException.getMessage());
				}
			}
			throw e; // Rethrow to fail the test in JUnit
		} finally {
			// Append to the SPECIFIC csv path passed to this method
			if (csvPath != null) {
				CoverageTracker.calculateAndAppend(testFile.getName(), CLASSES_DIRECTORY, csvPath, EXCLUDED_PACKAGES);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("jsimTestProvider")
	void testLineJSIM(File jsimTestFile) {
		// Call the generic runner, passing the JSIM CSV path
		runWithCoverage(jsimTestFile, JSIM_CSV_PATH, () -> {

			// --- Your JSIM specific logic goes here ---
			if (jsimTestFile.isDirectory()) {
				File jsimLineTestModelFile = FileUtil.getFileInFolder(jsimTestFile, JSIM_LINE_TEST_MODEL_FILE_NAME);
				File jsimLineTestModelResultsFile = FileUtil.getFileInFolder(jsimTestFile,
						JSIM_LINE_TEST_MODEL_RESULTS_FILE_NAME);
				if (jsimLineTestModelFile != null && jsimLineTestModelResultsFile != null) {
					assertTrue(TestUtil.testLineJSIMGraphModel(jsimLineTestModelFile, jsimLineTestModelResultsFile));
				}
			} else {
				assertTrue(TestUtil.testJSIMGraphModel(jsimTestFile));
			}

		});
	}

	@ParameterizedTest
	@MethodSource("jmvaTestProvider")
	void testLineJMVA(File jmvaTestFile) {
		// Call the generic runner, passing the JMVA CSV path
		runWithCoverage(jmvaTestFile, JMVA_CSV_PATH, () -> {

			// --- Your JMVA specific logic goes here ---
			assertTrue(TestUtil.testJMVAModel(jmvaTestFile));

		});
	}

	@ParameterizedTest
	@MethodSource("jsimWizTestProvider")
	void testJSIMWiz(File jsimWizTestFile) {
		// Call the generic runner, passing the JSIMWiz CSV path
		runWithCoverage(jsimWizTestFile, JSIMWIZ_CSV_PATH, () -> {

			// --- Your JSIMWiz specific logic goes here ---
			assertTrue(TestUtil.testJSIMWizModel(jsimWizTestFile));

		});
	}

}