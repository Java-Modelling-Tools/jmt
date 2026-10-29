package jmt.integration;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.security.CodeSource;

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

	// Resource paths on the test classpath
	public static final String TEST_RESOURCES_INPUT_PREFIX = "/jmt/integration/input";

	// Dynamic target and output directories (always in target/, never in src/)
	public static final Path TARGET_DIR = resolveTargetDirectory();
	public static final Path OUTPUT_DIR = TARGET_DIR.resolve("jmt").resolve("integration").resolve("output");
	public static final Path REACTOR_ROOT = resolveReactorRoot();

	/**
	 * String absolute path to the output directory.
	 */
	public static final String OUTPUT_FOLDER_PATH = OUTPUT_DIR.toAbsolutePath().toString();

	// Modules whose compiled classes JaCoCo analyses.
	private static final String[] COVERED_MODULES = { "jmt-engine", "jmt-jaba-core", "jmt-jwat-core", "jmt-jmva-core",
			"jmt-jmch-core" };

	private static final String[] REPRESENTATIVE_CLASS_NAMES = {
			"simDispatcher.DispatcherJSIMschema",
			"jaba.JabaModel",
			"analytical.ExactModel",
			"jwat.JWATConstants",
			"jmt.jmarkov.Job"
	};

	public static final File[] CLASSES_DIRECTORIES = resolveClassesDirectories();

	/**
	 * Resolves the target/ directory of this test module dynamically from its CodeSource.
	 */
	private static Path resolveTargetDirectory() {
		try {
			URI location = IntegrationTestConstants.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			Path testClassesDir = Paths.get(location);
			// <module>/target/test-classes -> <module>/target
			return testClassesDir.getParent();
		} catch (Exception e) {
			return Paths.get(System.getProperty("project.build.directory", "target")).toAbsolutePath();
		}
	}

	/**
	 * Resolves the multi-module reactor root directory dynamically.
	 */
	private static Path resolveReactorRoot() {
		Path moduleDir = TARGET_DIR.getParent();
		if (moduleDir != null && moduleDir.getParent() != null && Files.isDirectory(moduleDir.getParent().resolve("jmt-engine"))) {
			return moduleDir.getParent();
		}
		// Fallback: check working directory or sibling
		Path cwd = Paths.get("").toAbsolutePath();
		if (Files.isDirectory(cwd.resolve("jmt-engine"))) {
			return cwd;
		}
		if (cwd.getParent() != null && Files.isDirectory(cwd.getParent().resolve("jmt-engine"))) {
			return cwd.getParent();
		}
		return cwd;
	}

	/**
	 * Resolves the class locations (directories or JARs) across covered modules.
	 * Prioritizes the exact CodeSource locations where the JVM loaded the classes from,
	 * guaranteeing 100% bytecode CRC64 matching with JaCoCo in-memory probe data,
	 * with fallbacks to sibling module target/classes directories.
	 */
	private static File[] resolveClassesDirectories() {
		Set<File> locations = new LinkedHashSet<>();

		// 1. Prioritize CodeSource of active classes loaded in the current JVM
		if (REPRESENTATIVE_CLASS_NAMES != null) {
			for (String className : REPRESENTATIVE_CLASS_NAMES) {
				try {
					Class<?> clazz = Class.forName(className);
					CodeSource cs = clazz.getProtectionDomain().getCodeSource();
					if (cs != null && cs.getLocation() != null) {
						File loc = Paths.get(cs.getLocation().toURI()).toFile();
						if (loc.exists()) {
							locations.add(loc);
						}
					}
				} catch (Exception ignored) {
				}
			}
		}

		// 2. Fallback to sibling module target/classes in the reactor
		if (COVERED_MODULES != null && REACTOR_ROOT != null) {
			for (String module : COVERED_MODULES) {
				Path moduleClasses = REACTOR_ROOT.resolve(module).resolve("target").resolve("classes");
				File classDir = moduleClasses.toFile();
				if (classDir.isDirectory()) {
					locations.add(classDir);
				} else {
					File sibling = new File("../" + module + "/target/classes");
					File fromRoot = new File(module + "/target/classes");
					if (sibling.isDirectory()) {
						locations.add(sibling);
					} else if (fromRoot.isDirectory()) {
						locations.add(fromRoot);
					}
				}
			}
		}

		return locations.toArray(new File[0]);
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
