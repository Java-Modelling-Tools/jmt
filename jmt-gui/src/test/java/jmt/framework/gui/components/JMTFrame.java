package jmt.framework.gui.components;

/**
 * INTENTIONAL GUARD, not a real frame. Shadows the production JMTFrame on the jmt-gui test classpath
 * (test classes precede main classes) so that any model-class test that initialises JMTFrame, and
 * with it Swing and the look and feel, fails loudly. Do not add tests here that need the real JMTFrame.
 */
public class JMTFrame {
	static {
		// Boolean.TRUE: a static initialiser that cannot complete normally is a compile error
		if (Boolean.TRUE) {
			throw new AssertionError("JMTFrame must not be initialised by model classes (headless guard)");
		}
	}

	public static java.util.Locale getPlatformDefaultLocale() {
		return null;
	}
}
