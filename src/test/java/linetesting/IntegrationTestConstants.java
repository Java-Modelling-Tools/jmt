package linetesting;

import java.io.File;
import java.time.format.DateTimeFormatter;

public final class IntegrationTestConstants {

	// Private constructor to prevent instantiation
	private IntegrationTestConstants() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}

	// Folder Names
	public static final String JSIMGRAPH_TESTS_FOLDER_NAME = "jsim";
	public static final String JMVA_TESTS_FOLDER_NAME = "jmva";
	public static final String JSIMWIZ_TESTS_FOLDER_NAME = "jsimw";
	public static final String JABA_TESTS_FOLDER_NAME = "jaba";

	// Paths
	public static final String TEST_FILES_BASE_FOLDER_PATH = "src/test/resources/linetesting/input";
	public static final String OUTPUT_FOLDER_PATH = "src/test/resources/linetesting/output";
	public static final File CLASSES_DIRECTORY = new File("target/classes");

	// File Names
	public static final String JSIMGRAPH_LINE_TEST_MODEL_FILE_NAME = "model.jsim";
	public static final String JSIMGRAPH_LINE_TEST_MODEL_RESULTS_FILE_NAME = "model.jsim-result.jsim";
	public static final String TIMEOUT_CSV_NAME = "timeouts.csv";

	// Execution Configs
	public static final double STABILITY_TRESHOLD = 5.0;
	public static final int TEST_TIMEOUT = 5; // Minutes
	public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	public static final String[] EXCLUDED_PACKAGES = { "jmt/framework/gui", "jmt/gui", "jmt/jmva/gui",
			"jmt/commandline" };
}