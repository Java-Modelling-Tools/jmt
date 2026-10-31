package common.xml.swing;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import common.xml.FileFormat;

/**
 * Swing adapter of a {@link FileFormat}; directories are always accepted.
 */
public class FormatFileFilter extends FileFilter {

	private final FileFormat format;

	public FormatFileFilter(FileFormat format) {
		this.format = format;
	}

	@Override
	public boolean accept(File f) {
		return f.isDirectory() || format.accepts(f.getName());
	}

	@Override
	public String getDescription() {
		return format.getDescription();
	}
}
