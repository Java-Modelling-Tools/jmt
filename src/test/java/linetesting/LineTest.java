package linetesting;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LineTest {

	final static String TEST_FILES_BASE_FOLDER_PATH = "src/test/resources/linetesting/input";

	static Stream<String> folderProvider() {
		File outputDir = new File(TEST_FILES_BASE_FOLDER_PATH);
		if (!outputDir.exists() || !outputDir.isDirectory()) {
			throw new RuntimeException("Directory not found: " + outputDir.getAbsolutePath());
		}

		return Stream.of(outputDir.listFiles(File::isDirectory)).map(File::getName);
	}

	@ParameterizedTest
	@MethodSource("folderProvider")
	void testLine(String testFolderName) {

		assertTrue(TestUtil.testJSIMGraphModel(testFolderName));

	}

}