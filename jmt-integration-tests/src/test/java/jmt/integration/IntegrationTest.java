package jmt.integration;

import static jmt.integration.IntegrationTestConstants.CLASSES_DIRECTORIES;
import static jmt.integration.IntegrationTestConstants.COVERAGE_ENABLED;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class IntegrationTest {

	private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);

	// A model that runs longer than this gets a "still running" line, so a long run is not silent.
	private static final Duration HEARTBEAT = Duration.ofSeconds(30);

	private static PrintStream originalOut;
	private static Instant suiteStart;
	private static long totalModels;
	private static final java.util.concurrent.atomic.AtomicInteger completedModels = new java.util.concurrent.atomic.AtomicInteger();

	// We will build the full path dynamically in setup
	static String JSIM_CSV_PATH;
	static String JMVA_CSV_PATH;
	static String JSIMWIZ_CSV_PATH;
	static String JABA_CSV_PATH;
	static String TIMEOUT_CSV_PATH;

	@BeforeAll
	static void setupCsvs() throws IOException {
		suiteStart = Instant.now();
		completedModels.set(0);
		totalModels = Stream.of(JSIMGRAPH_TESTS_FOLDER_NAME, JMVA_TESTS_FOLDER_NAME, JSIMWIZ_TESTS_FOLDER_NAME,
				JABA_TESTS_FOLDER_NAME).flatMap(IntegrationTest::testProvider).filter(a -> a.get()[0] != null).count();
		LOG.info("Integration tests started: {} models, timeout {} min per model, coverage {}. Large simulations can"
				+ " take minutes; progress is reported while a model is running.", totalModels, TEST_TIMEOUT,
				COVERAGE_ENABLED ? "ON" : "off");

		// The engine and solvers print their summaries to System.out; drop them. Logging goes to stderr.
		originalOut = System.out;
		System.setOut(new PrintStream(OutputStream.nullOutputStream()));

		Files.createDirectories(OUTPUT_DIR);

		if (COVERAGE_ENABLED) {
			if (CLASSES_DIRECTORIES.length == 0) {
				throw new IOException("No module target/classes directory found. Run 'mvn compile' first.");
			}
			// Initialize individual CSVs inside target/jmt/integration/output
			JSIM_CSV_PATH = initCoverageCsv(OUTPUT_DIR, "jsim_coverage.csv");
			JMVA_CSV_PATH = initCoverageCsv(OUTPUT_DIR, "jmva_coverage.csv");
			JSIMWIZ_CSV_PATH = initCoverageCsv(OUTPUT_DIR, "jsimwiz_coverage.csv");
			JABA_CSV_PATH = initCoverageCsv(OUTPUT_DIR, "jaba_coverage.csv");
		}

		// Setup Timeout CSV
		Path timeoutFile = OUTPUT_DIR.resolve(TIMEOUT_CSV_NAME);
		TIMEOUT_CSV_PATH = timeoutFile.toAbsolutePath().toString();
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(timeoutFile))) {
			writer.println("FILE_NAME,STATUS,TIMESTAMP");
		}
	}

	@AfterAll
	static void restoreOutput() {
		System.setOut(originalOut);
		LOG.info("Integration tests finished in {}s", Duration.between(suiteStart, Instant.now()).getSeconds());
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
		LOG.info("Coverage report ready at: {}", absolutePath);
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

			if (entries.length == 0) {
				// JUnit 5.10 rejects an empty provider; runWithCoverage skips this placeholder
				return Stream.of(Arguments.of(null, 0, 0));
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

	/**
	 * Runs logic on a daemon thread and waits at most TEST_TIMEOUT minutes, logging a heartbeat while it waits.
	 * A timeout is signalled by TimeoutException, independent of JUnit internals. shortcut: the engine ignores
	 * interrupts, so a timed-out simulation keeps burning CPU until the JVM exits (the daemon thread never blocks
	 * exit); upgrade to a forked JVM per model if that starts skewing later tests.
	 */
	private static void runWithTimeout(String name, Executable logic) throws Throwable {
		Duration timeout = Duration.ofMinutes(TEST_TIMEOUT);
		ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "integration-test-runner");
			t.setDaemon(true);
			return t;
		});
		Future<Throwable> future = executor.submit(() -> {
			MDC.put("model", " [" + name + "]"); // every log line from this model's run then carries its file name
			try {
				logic.execute();
				return null;
			} catch (Throwable t) {
				return t;
			} finally {
				MDC.remove("model");
			}
		});
		try {
			Instant start = Instant.now();
			while (true) {
				long left = timeout.toMillis() - Duration.between(start, Instant.now()).toMillis();
				try {
					Throwable failure = future.get(Math.max(1, Math.min(left, HEARTBEAT.toMillis())),
							TimeUnit.MILLISECONDS);
					if (failure != null) {
						throw failure;
					}
					return;
				} catch (TimeoutException e) {
					if (left <= HEARTBEAT.toMillis()) {
						throw e;
					}
					LOG.info("Still running {} ({}s elapsed)", name, Duration.between(start, Instant.now()).getSeconds());
				}
			}
		} finally {
			future.cancel(true);
			executor.shutdownNow();
		}
	}

	private void runWithCoverage(File testFile, String csvPath, int currentIndex, int totalTests,
			Executable testLogic) throws Throwable {
		assumeTrue(testFile != null, "no model matches the test.model filter in this folder");
		Instant startInstant = Instant.now();
		String startTimeStr = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

		LOG.debug("[{}/{}] Starting test file: {} | Timestamp: {}", currentIndex, totalTests, testFile.getName(),
				startTimeStr);

		if (COVERAGE_ENABLED) {
			CoverageTracker.reset(); // Clear previous data
		}

		try {
			runWithTimeout(testFile.getName(), testLogic);
		} catch (TimeoutException e) {
			LOG.error("TIMEOUT after {} min for file: {}", TEST_TIMEOUT, testFile.getName());
			try (PrintWriter writer = new PrintWriter(new FileWriter(TIMEOUT_CSV_PATH, true))) {
				writer.println(testFile.getName() + ",TIMEOUT," + LocalDateTime.now().format(TIMESTAMP_FORMATTER));
			} catch (IOException ioException) {
				LOG.error("Failed to write timeout to CSV", ioException);
			}
			throw new AssertionFailedError(
					"Timeout exceeded for file: " + testFile.getName() + " (" + TEST_TIMEOUT + " min)");
		} finally {
			if (COVERAGE_ENABLED && csvPath != null) {
				// Append to the SPECIFIC csv path passed to this method
				CoverageTracker.calculateAndAppend(testFile.getName(), CLASSES_DIRECTORIES, csvPath, EXCLUDED_PACKAGES);
			}

			// Using toMillis() divided by 1000.0 gives us precise decimals (e.g., "1.453s")
			double durationSeconds = Duration.between(startInstant, Instant.now()).toMillis() / 1000.0;

			int done = completedModels.incrementAndGet();
			LOG.info("[{}/{}] {} done in {}s, {} remaining", done, totalModels, testFile.getName(), durationSeconds,
					totalModels - done);
		}
	}

	@ParameterizedTest(name = "[{1}/{2}] {0}")
	@MethodSource("jsimGraphTestProvider")
	void testJSIMGraph(File jsimTestFile, int currentIndex, int totalTests) throws Throwable {
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
	void testJMVA(File jmvaTestFile, int currentIndex, int totalTests) throws Throwable {
		// Call the generic runner, passing the JMVA CSV path
		runWithCoverage(jmvaTestFile, JMVA_CSV_PATH, currentIndex, totalTests, () -> {

			assertTrue(IntegrationTestUtil.testJMVAModel(jmvaTestFile),
					"JMVA test failed for file: " + jmvaTestFile.getName());

		});
	}

	@ParameterizedTest(name = "[{1}/{2}] {0}")
	@MethodSource("jsimWizTestProvider")
	void testJSIMWiz(File jsimWizTestFile, int currentIndex, int totalTests) throws Throwable {
		// Call the generic runner, passing the JSIMWiz CSV path
		runWithCoverage(jsimWizTestFile, JSIMWIZ_CSV_PATH, currentIndex, totalTests, () -> {

			assertTrue(IntegrationTestUtil.testJSIMWizModel(jsimWizTestFile),
					"JSIMWiz test failed for file: " + jsimWizTestFile.getName());

		});
	}

	@ParameterizedTest(name = "[{1}/{2}] {0}")
	@MethodSource("jabaTestProvider")
	void testJABA(File jabaTestFile, int currentIndex, int totalTests) throws Throwable {
		// Call the generic runner, passing the JABA CSV path
		runWithCoverage(jabaTestFile, JABA_CSV_PATH, currentIndex, totalTests, () -> {

			assertTrue(IntegrationTestUtil.testJABAModel(jabaTestFile),
					"JABA test failed for file: " + jabaTestFile.getName());

		});
	}

}
