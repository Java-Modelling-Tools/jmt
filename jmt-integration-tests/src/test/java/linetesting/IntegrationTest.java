package linetesting;

import static linetesting.IntegrationTestConstants.CLASSES_DIRECTORY;
import static linetesting.IntegrationTestConstants.EXCLUDED_PACKAGES;
import static linetesting.IntegrationTestConstants.JABA_TESTS_FOLDER_NAME;
import static linetesting.IntegrationTestConstants.JMVA_TESTS_FOLDER_NAME;
import static linetesting.IntegrationTestConstants.JSIMGRAPH_LINE_TEST_MODEL_FILE_NAME;
import static linetesting.IntegrationTestConstants.JSIMGRAPH_LINE_TEST_MODEL_RESULTS_FILE_NAME;
import static linetesting.IntegrationTestConstants.JSIMGRAPH_TESTS_FOLDER_NAME;
import static linetesting.IntegrationTestConstants.JSIMWIZ_TESTS_FOLDER_NAME;
import static linetesting.IntegrationTestConstants.OUTPUT_FOLDER_PATH;
import static linetesting.IntegrationTestConstants.TEST_FILES_BASE_FOLDER_PATH;
import static linetesting.IntegrationTestConstants.TEST_TIMEOUT;
import static linetesting.IntegrationTestConstants.TIMEOUT_CSV_NAME;
import static linetesting.IntegrationTestConstants.TIMESTAMP_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

public class IntegrationTest {

	// We will build the full path dynamically in setup
	static String JSIM_CSV_PATH;
	static String JMVA_CSV_PATH;
	static String JSIMWIZ_CSV_PATH;
	static String JABA_CSV_PATH;
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
		JABA_CSV_PATH = initCoverageCsv(outputFolder, "jaba_coverage.csv");

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

	@AfterEach
	void tearDown() {
		// This automatically runs after EVERY single parameterized test finishes,
		// regardless of whether the test passed or failed!
		IntegrationTestUtil.closeGUI();
	}

	private static Stream<Arguments> testProvider(String testFolderName) {
		String jsimTestsFolder = TEST_FILES_BASE_FOLDER_PATH + "/" + testFolderName;
		File[] entries = FileUtil.listAllFilesAndFoldersInFolder(jsimTestsFolder);
		if (entries == null)
			return Stream.empty();

		int totalTests = entries.length;

		// Create a stream of Arguments: (File, currentIndex, totalTests)
		return IntStream.range(0, totalTests).mapToObj(i -> Arguments.of(entries[i], i + 1, totalTests));
	}

	static Stream<Arguments> jsimGraphTestProvider() {
		return testProvider(JSIMGRAPH_TESTS_FOLDER_NAME);
	}

	static Stream<Arguments> jmvaTestProvider() {
		return testProvider(JMVA_TESTS_FOLDER_NAME);
	}

	static Stream<Arguments> jsimWizTestProvider() {
		return testProvider(JSIMWIZ_TESTS_FOLDER_NAME);
	}

	static Stream<Arguments> jabaTestProvider() {
		return testProvider(JABA_TESTS_FOLDER_NAME);
	}

	private void runWithCoverage(File testFile, String csvPath, int currentIndex, int totalTests,
			Executable testLogic) {
		Instant startInstant = Instant.now();
		String startTimeStr = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

		System.out.println("\n[INFO] [" + currentIndex + "/" + totalTests + "] Starting test file: "
				+ testFile.getName() + " | Timestamp: " + startTimeStr);

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

			Instant endInstant = Instant.now();
			String endTimeStr = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

			// Using toMillis() divided by 1000.0 gives us precise decimals (e.g., "1.453s")
			double durationSeconds = Duration.between(startInstant, endInstant).toMillis() / 1000.0;

			System.out.println("[INFO] [" + currentIndex + "/" + totalTests + "] Finished test file: "
					+ testFile.getName() + " | Timestamp: " + endTimeStr + " | Duration: " + durationSeconds + "s");
		}
	}

	@ParameterizedTest(name = "[{1}/{2}] {0}")
	@MethodSource("jsimGraphTestProvider")
	void testJSIMGraph(File jsimTestFile, int currentIndex, int totalTests) {
		// Call the generic runner, passing the JSIM CSV path
		runWithCoverage(jsimTestFile, JSIM_CSV_PATH, currentIndex, totalTests, () -> {

			if (jsimTestFile.isDirectory()) {
				File jsimLineTestModelFile = FileUtil.getFileInFolder(jsimTestFile,
						JSIMGRAPH_LINE_TEST_MODEL_FILE_NAME);
				File jsimLineTestModelResultsFile = FileUtil.getFileInFolder(jsimTestFile,
						JSIMGRAPH_LINE_TEST_MODEL_RESULTS_FILE_NAME);
				if (jsimLineTestModelFile != null && jsimLineTestModelResultsFile != null) {
					assertTrue(
							IntegrationTestUtil.testLineJSIMGraphModel(jsimLineTestModelFile,
									jsimLineTestModelResultsFile),
							"JSIMGraph line test failed for file: " + jsimTestFile.getName());
				}
			} else {
				assertTrue(IntegrationTestUtil.testJSIMGraphModel(jsimTestFile),
						"JSIMGraph test failed for file: " + jsimTestFile.getName());
			}

		});
	}

	@ParameterizedTest(name = "[{1}/{2}] {0}")
	@MethodSource("jmvaTestProvider")
	void testJMVA(File jmvaTestFile, int currentIndex, int totalTests) {
		// Call the generic runner, passing the JMVA CSV path
		runWithCoverage(jmvaTestFile, JMVA_CSV_PATH, currentIndex, totalTests, () -> {

			assertTrue(IntegrationTestUtil.testJMVAModel(jmvaTestFile),
					"JMVA test failed for file: " + jmvaTestFile.getName());

		});
	}

	@ParameterizedTest(name = "[{1}/{2}] {0}")
	@MethodSource("jsimWizTestProvider")
	void testJSIMWiz(File jsimWizTestFile, int currentIndex, int totalTests) {
		// Call the generic runner, passing the JSIMWiz CSV path
		runWithCoverage(jsimWizTestFile, JSIMWIZ_CSV_PATH, currentIndex, totalTests, () -> {

			assertTrue(IntegrationTestUtil.testJSIMWizModel(jsimWizTestFile),
					"JSIMWiz test failed for file: " + jsimWizTestFile.getName());

		});
	}

	@ParameterizedTest(name = "[{1}/{2}] {0}")
	@MethodSource("jabaTestProvider")
	void testJABA(File jabaTestFile, int currentIndex, int totalTests) {
		// Call the generic runner, passing the JABA CSV path
		runWithCoverage(jabaTestFile, JABA_CSV_PATH, currentIndex, totalTests, () -> {

			assertTrue(IntegrationTestUtil.testJABAModel(jabaTestFile),
					"JABA test failed for file: " + jabaTestFile.getName());

		});
	}

}