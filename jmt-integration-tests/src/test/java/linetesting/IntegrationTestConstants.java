package linetesting;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

	// Modules whose compiled classes JaCoCo analyses. Before the modularisation every
	// class lived in one target/classes; now each module has its own, and this module
	// has no src/main/java at all — so a single "target/classes" resolves to an empty
	// directory and every coverage CSV comes out with nothing but a header.
	//
	// Only the non-GUI modules are listed. That reproduces the old EXCLUDED_PACKAGES
	// intent: the pre-rename filter dropped jmt/gui, jmt/jmva/gui and jmt/framework/gui,
	// which after the package rename are exactly the -gui modules plus jmt-gui and
	// jmt-file-handler (formerly jmt.gui.common.xml). Prefix exclusion can no longer
	// express that, because GUI and model code now share top-level names (e.g.
	// common/panels is GUI while common/definitions is not).
	private static final String[] COVERED_MODULES = { "jmt-engine", "jmt-jaba-core", "jmt-jwat-core", "jmt-jmva-core",
			"jmt-jmch-core" };

	public static final File[] CLASSES_DIRECTORIES = resolveClassesDirectories();

	// Surefire runs with the module directory as CWD, so sibling modules sit at
	// ../<module>. Running from the reactor root (as IntelliJ may) puts them at
	// <module>. Try both so coverage works either way.
	private static File[] resolveClassesDirectories() {
		List<File> dirs = new ArrayList<>();
		for (String module : COVERED_MODULES) {
			File sibling = new File("../" + module + "/target/classes");
			File fromRoot = new File(module + "/target/classes");
			if (sibling.isDirectory()) {
				dirs.add(sibling);
			} else if (fromRoot.isDirectory()) {
				dirs.add(fromRoot);
			}
		}
		return dirs.toArray(new File[0]);
	}

	// File Names
	public static final String JSIMGRAPH_LINE_TEST_MODEL_FILE_NAME = "model.jsim";
	public static final String JSIMGRAPH_LINE_TEST_MODEL_RESULTS_FILE_NAME = "model.jsim-result.jsim";
	public static final String TIMEOUT_CSV_NAME = "timeouts.csv";

	// Execution Configs
	public static final double STABILITY_TRESHOLD = 5.0;
	public static final int TEST_TIMEOUT = 10; // Minutes (increased: PA simulations with 5 steps × 20M samples need more time)
	public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	// GUI exclusion is now handled by COVERED_MODULES above. What remains here are the
	// non-GUI packages that were excluded for other reasons and still exist by name.
	public static final String[] EXCLUDED_PACKAGES = { "jmt/commandline" };
}