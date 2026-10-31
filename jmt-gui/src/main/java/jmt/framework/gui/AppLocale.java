package jmt.framework.gui;

import java.util.Locale;

/**
 * Holds the platform default locale and switches the application to English.
 * Kept free of Swing so model classes can load headless.
 */
public final class AppLocale {

	private static final Locale PLATFORM_DEFAULT = Locale.getDefault();

	static {
		Locale.setDefault(Locale.ENGLISH);
	}

	private AppLocale() {
	}

	/**
	 * @return the platform default locale, captured before the application switched to English.
	 */
	public static Locale platformDefault() {
		return PLATFORM_DEFAULT;
	}
}
