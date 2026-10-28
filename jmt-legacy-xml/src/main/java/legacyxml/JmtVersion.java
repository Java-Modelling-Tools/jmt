package legacyxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads the <code>jmtVersion</code> a file was saved with and compares versions, so the GUI can warn
 * before opening a file written by a newer JMT.
 */
public final class JmtVersion {

	private static final String VERSION_ATTRIBUTE = "jmtVersion";

	private JmtVersion() {
	}

	/**
	 * Reads the version from the root element only (does not parse the whole file).
	 * @return the version, or null if the file has none (saved by a release up to 1.4.0) or is not readable XML
	 */
	public static String readFileVersion(File file) {
		XMLInputFactory f = XMLInputFactory.newInstance();
		f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		try (InputStream in = new FileInputStream(file)) {
			XMLStreamReader r = f.createXMLStreamReader(in);
			try {
				while (r.hasNext()) {
					if (r.next() == XMLStreamConstants.START_ELEMENT) {
						return r.getAttributeValue(null, VERSION_ATTRIBUTE);
					}
				}
			} finally {
				r.close();
			}
		} catch (Exception e) {
			// Not our job to report a broken file: the real loader will
		}
		return null;
	}

	/**
	 * @return true if <code>fileVersion</code> is strictly newer than <code>runningVersion</code>. Compares the
	 * dotted numeric part ("1.4.0" &lt; "1.10"); qualifiers such as "-BETA2" are ignored. Missing or unparseable
	 * versions are never newer, so an odd value cannot raise a false alarm.
	 */
	public static boolean isNewer(String fileVersion, String runningVersion) {
		int[] file = numbers(fileVersion);
		int[] running = numbers(runningVersion);
		if (file == null || running == null) {
			return false;
		}
		for (int i = 0; i < Math.max(file.length, running.length); i++) {
			int a = i < file.length ? file[i] : 0;
			int b = i < running.length ? running[i] : 0;
			if (a != b) {
				return a > b;
			}
		}
		return false;
	}

	private static int[] numbers(String version) {
		if (version == null) {
			return null;
		}
		String[] parts = version.trim().split("\\.");
		int[] out = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			int end = 0;
			while (end < parts[i].length() && Character.isDigit(parts[i].charAt(end))) {
				end++;
			}
			if (end == 0) {
				return i == 0 ? null : java.util.Arrays.copyOf(out, i);
			}
			try {
				out[i] = Integer.parseInt(parts[i].substring(0, end));
			} catch (NumberFormatException e) {
				return null;
			}
			if (end < parts[i].length()) {
				return java.util.Arrays.copyOf(out, i + 1); // qualifier ends the numeric part
			}
		}
		return out;
	}
}
