package dataAnalysis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogTest {

	@TempDir
	File dir;

	@Test
	void writesSamplesStringsAndCommentsOnePerLine() throws Exception {
		File f = new File(dir, "a.log");
		try (Log log = new Log(f.getPath())) {
			log.write(1.5);
			log.write("text");
			log.comment("note");
		}
		assertEquals(Arrays.asList("1.5", "text", "%note"), Files.readAllLines(f.toPath(), StandardCharsets.UTF_8));
	}

	@Test
	void bufferedConstructorWritesTheSameContent() throws Exception {
		File f = new File(dir, "b.log");
		try (Log log = new Log(f.getPath(), 16)) {
			log.write(2.0);
		}
		assertEquals(Arrays.asList("2.0"), Files.readAllLines(f.toPath(), StandardCharsets.UTF_8));
	}

	@Test
	void unwritablePathIsReportedWithoutThrowingAndCloseIsSafe() {
		File bad = new File(dir, "missing/sub/x.log");
		assertDoesNotThrow(() -> new Log(bad.getPath()).close());
		assertDoesNotThrow(() -> new Log(bad.getPath(), 8).close());
	}
}
