package jmt.integration;

import static jmt.integration.IntegrationTestConstants.CLASSES_DIRECTORIES;
import static jmt.integration.IntegrationTestConstants.EXCLUDED_PACKAGES;
import static jmt.integration.IntegrationTestConstants.JABA_TESTS_FOLDER_NAME;
import static jmt.integration.IntegrationTestConstants.JMVA_TESTS_FOLDER_NAME;
import static jmt.integration.IntegrationTestConstants.JSIMGRAPH_LINE_TEST_MODEL_FILE_NAME;
import static jmt.integration.IntegrationTestConstants.JSIMGRAPH_LINE_TEST_MODEL_RESULTS_FILE_NAME;
import static jmt.integration.IntegrationTestConstants.JSIMGRAPH_TESTS_FOLDER_NAME;
import static jmt.integration.IntegrationTestConstants.JSIMWIZ_TESTS_FOLDER_NAME;
import static jmt.integration.IntegrationTestConstants.OUTPUT_DIR;
import static jmt.integration.IntegrationTestConstants.TARGET_DIR;
import static jmt.integration.IntegrationTestConstants.TEST_RESOURCES_INPUT_PREFIX;
import static jmt.integration.IntegrationTestConstants.TEST_TIMEOUT;
import static jmt.integration.IntegrationTestConstants.TIMEOUT_CSV_NAME;
import static jmt.integration.IntegrationTestConstants.TIMESTAMP_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
		if (CLASSES_DIRECTORIES.length == 0) {
			throw new IOException("No module target/classes directory found. Run 'mvn compile' first.");
		}

		Files.createDirectories(OUTPUT_DIR);

		// Initialize individual CSVs inside target/jmt/integration/output
		JSIM_CSV_PATH = initCoverageCsv(OUTPUT_DIR, "jsim_coverage.csv");
		JMVA_CSV_PATH = initCoverageCsv(OUTPUT_DIR, "jmva_coverage.csv");
		JSIMWIZ_CSV_PATH = initCoverageCsv(OUTPUT_DIR, "jsimwiz_coverage.csv");
		JABA_CSV_PATH = initCoverageCsv(OUTPUT_DIR, "jaba_coverage.csv");

		// Setup Timeout CSV
		Path timeoutFile = OUTPUT_DIR.resolve(TIMEOUT_CSV_NAME);
		TIMEOUT_CSV_PATH = timeoutFile.toAbsolutePath().toString();
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(timeoutFile))) {
			writer.println("FILE_NAME,STATUS,TIMESTAMP");
		}
	}

	// Helper method to create a CSV and write the header
	private static String initCoverageCsv(Path folder, String fileName) throws IOException {
		Path csvPath = folder.resolve(fileName);
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvPath))) {
			writer.println("TEST_ID,GROUP,PACKAGE,CLASS," + "INSTRUCTION_MISSED,INSTRUCTION_COVERED,"
					+ "BRANCH_MISSED,BRANCH_COVERED," + "LINE_MISSED,LINE_COVERED,"
					+ "COMPLEXITY_MISSED,COMPLEXITY_COVERED," + "METHOD_MISSED,METHOD_COVERED,"
					+ "SPECIFIC_LINES_COVERED,SPECIFIC_LINES_MISSED");
		}
		String absolutePath = csvPath.toAbsolutePath().toString();
		System.out.println("Coverage report ready at: " + absolutePath);
		return absolutePath;
	}

	private static Stream<Arguments> testProvider(String testFolderName) {
		String resourcePath = TEST_RESOURCES_INPUT_PREFIX + "/" + testFolderName;
		URL resource = IntegrationTest.class.getResource(resourcePath);
		if (resource == null) {
			throw new IllegalArgumentException("Test input folder not found on classpath: " + resourcePath);
		}

		try {
			Path folderPath = Paths.get(resource.toURI());
			syncStaleResources(folderPath, testFolderName);
			File[] entries = FileUtil.listAllFilesAndFoldersInFolder(folderPath);
			if (entries == null) {
				return Stream.empty();
			}

			String modelFilter = System.getProperty("test.model");
			if (modelFilter == null) {
				modelFilter = System.getProperty("testModel");
			}
			if (modelFilter != null && !modelFilter.trim().isEmpty()) {
				String filter = modelFilter.trim().toLowerCase();
				entries = Stream.of(entries)
						.filter(f -> f.getName().toLowerCase().contains(filter))
						.toArray(File[]::new);
			}

			int totalTests = entries.length;
			final File[] testEntries = entries;

			// Create a stream of Arguments: (File, currentIndex, totalTests)
			return IntStream.range(0, totalTests).mapToObj(i -> Arguments.of(testEntries[i], i + 1, totalTests));
		} catch (Exception e) {
			throw new RuntimeException("Failed to read test models from classpath: " + resourcePath, e);
		}
	}

	private static void syncStaleResources(Path targetFolderPath, String testFolderName) {
		try {
			Path moduleDir = TARGET_DIR.getParent();
			if (moduleDir != null) {
				Path srcFolder = moduleDir.resolve("src").resolve("test").resolve("resources")
						.resolve("jmt").resolve("integration").resolve("input").resolve(testFolderName);
				if (Files.isDirectory(srcFolder)) {
					File[] targetFiles = FileUtil.listAllFilesAndFoldersInFolder(targetFolderPath);
					if (targetFiles != null) {
						for (File targetFile : targetFiles) {
							Path srcFile = srcFolder.resolve(targetFile.getName());
							if (!Files.exists(srcFile)) {
								deleteRecursively(targetFile);
							}
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static void deleteRecursively(File file) {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursively(child);
				}
			}
		}
		file.delete();
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
				CoverageTracker.calculateAndAppend(testFile.getName(), CLASSES_DIRECTORIES, csvPath, EXCLUDED_PACKAGES);
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
