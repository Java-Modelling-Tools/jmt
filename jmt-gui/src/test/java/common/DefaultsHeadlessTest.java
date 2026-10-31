package common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import common.definitions.CommonModel;

/** Defaults and CommonModel must not initialise JMTFrame: the test-source JMTFrame guard throws if they do. */
class DefaultsHeadlessTest {
	@Test
	void defaultsAndCommonModelLoadHeadless() {
		new CommonModel();
		assertEquals(Locale.ENGLISH, Locale.getDefault());
		String d = Defaults.get("loggerDelimiter");
		assertTrue(",".equals(d) || ";".equals(d), d);
	}
}
