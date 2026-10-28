package legacyxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class JmtVersionTest {

	@Test
	void comparesNumerically() {
		assertTrue(JmtVersion.isNewer("1.5.0", "1.4.0"));
		assertTrue(JmtVersion.isNewer("1.10", "1.9.9"));
		assertTrue(JmtVersion.isNewer("2", "1.99"));
		assertTrue(JmtVersion.isNewer("1.4.1", "1.4"));
	}

	@Test
	void sameOrOlderIsNotNewer() {
		assertFalse(JmtVersion.isNewer("1.4.0", "1.4.0"));
		assertFalse(JmtVersion.isNewer("1.4", "1.4.0"));
		assertFalse(JmtVersion.isNewer("1.3.1", "1.4.0"));
	}

	@Test
	void ignoresQualifiers() {
		assertFalse(JmtVersion.isNewer("1.4.0-BETA2", "1.4.0"));
		assertFalse(JmtVersion.isNewer("1.4.0", "1.4.0-SNAPSHOT"));
		assertTrue(JmtVersion.isNewer("1.5.0-BETA1", "1.4.0"));
	}

	@Test
	void oddValuesNeverRaiseAnAlarm() {
		assertFalse(JmtVersion.isNewer(null, "1.4.0"));
		assertFalse(JmtVersion.isNewer("latest", "1.4.0"));
		assertFalse(JmtVersion.isNewer("", "1.4.0"));
		assertFalse(JmtVersion.isNewer("9.9.9", null));
	}

	@Test
	void readsVersionFromRootOnly() throws Exception {
		File f = File.createTempFile("ver", ".jsimg");
		try {
			Files.write(f.toPath(), "<?xml version=\"1.0\"?><archive name=\"a\" jmtVersion=\"2.0.1\"><sim jmtVersion=\"0.0.1\"/></archive>"
					.getBytes(StandardCharsets.ISO_8859_1));
			assertEquals("2.0.1", JmtVersion.readFileVersion(f));
		} finally {
			Files.deleteIfExists(f.toPath());
		}
	}

	@Test
	void missingVersionOrBrokenFileGivesNull() throws Exception {
		File f = File.createTempFile("ver", ".jsimg");
		try {
			Files.write(f.toPath(), "<archive name=\"a\"/>".getBytes(StandardCharsets.ISO_8859_1));
			assertNull(JmtVersion.readFileVersion(f));
			Files.write(f.toPath(), "not xml at all".getBytes(StandardCharsets.ISO_8859_1));
			assertNull(JmtVersion.readFileVersion(f));
			assertNull(JmtVersion.readFileVersion(new File("/no/such/file.jsimg")));
		} finally {
			Files.deleteIfExists(f.toPath());
		}
	}
}
