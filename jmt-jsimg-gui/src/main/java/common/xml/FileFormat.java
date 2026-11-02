package common.xml;

/**
 * Display-free descriptor of a file format: a set of extensions and a label.
 */
public class FileFormat {

	public static final String SEP = ";";

	private final String[] extensions;
	private final String description;

	/**
	 * @param extensionList semicolon separated extensions (for example ".jsimg;.jmodel")
	 * @param text text description of the format
	 */
	public FileFormat(String extensionList, String text) {
		this.extensions = extensionList.split(SEP);
		StringBuilder sb = new StringBuilder(text).append(" (*");
		sb.append(String.join("; *", extensions)).append(")");
		this.description = sb.toString();
	}

	/** Case-insensitive suffix match. */
	public boolean accepts(String fileName) {
		String name = fileName.toLowerCase();
		for (String e : extensions) {
			if (name.endsWith(e)) {
				return true;
			}
		}
		return false;
	}

	public String[] getExtensions() {
		return extensions.clone();
	}

	public String getDescription() {
		return description;
	}
}
