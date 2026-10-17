package legacyxml;

/**
 * XML dialects JMT has written. Schemas are identical across all of them; only the Java class
 * names stored in <code>classPath</code> attributes differ.
 */
public enum XmlFormat {

	/** Current code: packages without the old "jmt.engine." / "jmt.gui." roots. */
	CURRENT("current"),

	/** Releases up to 1.4.0 (1.3.x, 1.4.0): fully qualified "jmt.engine.*" / "jmt.gui.*" names. */
	V1_4_0("1.4.0", "1.3.1-BETA2", "1.3.1", "1.3.0", "1.3", "1.4");

	private final String[] names;

	XmlFormat(String... names) {
		this.names = names;
	}

	/** Resolves a release label such as "1.4.0" or "current". */
	public static XmlFormat fromName(String name) {
		for (XmlFormat f : values()) {
			for (String n : f.names) {
				if (n.equalsIgnoreCase(name)) {
					return f;
				}
			}
		}
		throw new IllegalArgumentException("Unknown JMT version '" + name + "'; use 'current' or a release up to 1.4.0");
	}
}
