package common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import common.definitions.CommonModel;

/** Runs with com.jgoodies:looks excluded from the classpath (see pom): model classes must load without Swing look and feel. */
class DefaultsHeadlessTest {
	@Test
	void defaultsAndCommonModelLoadHeadless() {
		new CommonModel();
		assertEquals(Locale.ENGLISH, Locale.getDefault());
		String d = Defaults.get("loggerDelimiter");
		assertTrue(",".equals(d) || ";".equals(d), d);
	}
}
