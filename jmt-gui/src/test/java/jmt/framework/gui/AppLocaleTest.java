package jmt.framework.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class AppLocaleTest {
	@Test
	void platformDefaultIsCapturedAndAppIsEnglish() {
		Locale platform = AppLocale.platformDefault();
		assertNotNull(platform);
		assertSame(platform, AppLocale.platformDefault());
		assertEquals(Locale.ENGLISH, Locale.getDefault());
	}
}
