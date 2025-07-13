package linetesting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LineTest {

	final static String TEST_FILES_BASE_FOLDER_PATH = "src/test/resources/linetesting/input";
	final static String JSIM_TESTS_FOLDER_NAME = "jsim";

	final static String JSIM_LINE_TEST_MODEL_FILE_NAME = "model.jsim";
	final static String JSIM_LINE_TEST_MODEL_RESULTS_FILE_NAME = "model.jsim-result.jsim";

	static Stream<File> listAllFilesAndFoldersInFolder(String folderName) {
		File[] entries = FileUtil.listAllFilesAndFoldersInFolder(folderName);
		if (entries == null)
			return Stream.empty();

		return Stream.of(entries);
	}

	static String getTestFolderPath(String testFolderName) {
		return TEST_FILES_BASE_FOLDER_PATH + "/" + testFolderName;
	}

	static Stream<File> jsimTestProvider() {
		String jsimTestsFolder = getTestFolderPath(JSIM_TESTS_FOLDER_NAME);
		return listAllFilesAndFoldersInFolder(jsimTestsFolder);
	}

	@ParameterizedTest
	@MethodSource("jsimTestProvider")
	void testLine(File jsimTestFile) throws IOException {

		if (jsimTestFile.isDirectory()) {
			// Check if it's a line test
			File jsimLineTestModelFile = FileUtil.getFileInFolder(jsimTestFile, JSIM_LINE_TEST_MODEL_FILE_NAME);
			File jsimLineTestModelResultsFile = FileUtil.getFileInFolder(jsimTestFile,
					JSIM_LINE_TEST_MODEL_RESULTS_FILE_NAME);
			if (jsimLineTestModelFile != null && jsimLineTestModelResultsFile != null) {
				System.out.println(jsimTestFile.getPath());
				assertTrue(TestUtil.testLineJSIMGraphModel(jsimLineTestModelFile, jsimLineTestModelResultsFile));
			}
		}

		if (jsimTestFile.isFile()) {

		}

		// System.out.println("Running test on: " + jsimTestFile.getAbsolutePath());
	}

}