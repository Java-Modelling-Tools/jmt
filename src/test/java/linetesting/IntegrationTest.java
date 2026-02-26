package linetesting;

import static linetesting.IntegrationTestConstants.CLASSES_DIRECTORY;
import static linetesting.IntegrationTestConstants.EXCLUDED_PACKAGES;
import static linetesting.IntegrationTestConstants.JABA_TESTS_FOLDER_NAME;
import static linetesting.IntegrationTestConstants.JMVA_TESTS_FOLDER_NAME;
import static linetesting.IntegrationTestConstants.JSIMWIZ_TESTS_FOLDER_NAME;
import static linetesting.IntegrationTestConstants.JSIMGRAPH_LINE_TEST_MODEL_FILE_NAME;
import static linetesting.IntegrationTestConstants.JSIMGRAPH_LINE_TEST_MODEL_RESULTS_FILE_NAME;
import static linetesting.IntegrationTestConstants.JSIMGRAPH_TESTS_FOLDER_NAME;
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
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import java.time.Instant;

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

	private static Stream<File> testProvider(String testFolderName) {
		String jsimTestsFolder = TEST_FILES_BASE_FOLDER_PATH + "/" + testFolderName;
		File[] entries = FileUtil.listAllFilesAndFoldersInFolder(jsimTestsFolder);
		if (entries == null)
			return Stream.empty();
		return Stream.of(entries);
	}

	static Stream<File> jsimGraphTestProvider() {
		return testProvider(JSIMGRAPH_TESTS_FOLDER_NAME);
	}

	static Stream<File> jmvaTestProvider() {
		return testProvider(JMVA_TESTS_FOLDER_NAME);
	}

	static Stream<File> jsimWizTestProvider() {
		return testProvider(JSIMWIZ_TESTS_FOLDER_NAME);
	}

	static Stream<File> jabaTestProvider() {
		return testProvider(JABA_TESTS_FOLDER_NAME);
	}

	private void runWithCoverage(File testFile, String csvPath, Executable testLogic) {
		Instant startInstant = Instant.now(); 
        String startTimeStr = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        System.out.println("\n[INFO] Starting test file: " + testFile.getName() + " | Timestamp: " + startTimeStr);

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
            
            System.out.println("[INFO] Finished test file: " + testFile.getName() 
                             + " | Timestamp: " + endTimeStr 
                             + " | Duration: " + durationSeconds + "s");
		}
	}

	@ParameterizedTest
	@MethodSource("jsimGraphTestProvider")
	void testJSIMGraph(File jsimTestFile) {
		// Call the generic runner, passing the JSIM CSV path
		runWithCoverage(jsimTestFile, JSIM_CSV_PATH, () -> {

			// --- Your JSIM specific logic goes here ---
			if (jsimTestFile.isDirectory()) {
				File jsimLineTestModelFile = FileUtil.getFileInFolder(jsimTestFile, JSIMGRAPH_LINE_TEST_MODEL_FILE_NAME);
				File jsimLineTestModelResultsFile = FileUtil.getFileInFolder(jsimTestFile,
						JSIMGRAPH_LINE_TEST_MODEL_RESULTS_FILE_NAME);
				if (jsimLineTestModelFile != null && jsimLineTestModelResultsFile != null) {
					assertTrue(IntegrationTestUtil.testLineJSIMGraphModel(jsimLineTestModelFile, jsimLineTestModelResultsFile));
				}
			} else {
				assertTrue(IntegrationTestUtil.testJSIMGraphModel(jsimTestFile));
			}

		});
	}

	@ParameterizedTest
	@MethodSource("jmvaTestProvider")
	void testJMVA(File jmvaTestFile) {
		// Call the generic runner, passing the JMVA CSV path
		runWithCoverage(jmvaTestFile, JMVA_CSV_PATH, () -> {

			// --- Your JMVA specific logic goes here ---
			assertTrue(IntegrationTestUtil.testJMVAModel(jmvaTestFile));

		});
	}

	@ParameterizedTest
	@MethodSource("jsimWizTestProvider")
	void testJSIMWiz(File jsimWizTestFile) {
		// Call the generic runner, passing the JSIMWiz CSV path
		runWithCoverage(jsimWizTestFile, JSIMWIZ_CSV_PATH, () -> {

			// --- Your JSIMWiz specific logic goes here ---
			assertTrue(IntegrationTestUtil.testJSIMWizModel(jsimWizTestFile));

		});
	}

	@ParameterizedTest
	@MethodSource("jabaTestProvider")
	void testJABA(File jabaTestFile) {
		// Call the generic runner, passing the JABA CSV path
		runWithCoverage(jabaTestFile, JABA_CSV_PATH, () -> {

			// --- Your JABA specific logic goes here ---
			assertTrue(IntegrationTestUtil.testJABAModel(jabaTestFile));

		});
	}

}