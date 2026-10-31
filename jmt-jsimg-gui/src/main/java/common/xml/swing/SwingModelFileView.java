/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package common.xml.swing;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import common.Defaults;
import common.xml.ModelFileView;
import common.xml.ModelLoader;
import common.xml.ModelLoader.JmtFileFilter;

/**
 * Swing implementation of {@link ModelFileView}: the file chooser and dialogs formerly inside {@link ModelLoader}.
 */
public class SwingModelFileView implements ModelFileView {

	private final JmtFileChooser dialog;
	private final JmtFileFilter defaultFilter;
	private final JmtFileFilter defaultSaveFilter;

	/**
	 * @param defaultFilter default open filter of the application (JABA selects only the JABA filter)
	 * @param defaultSaveFilter default save filter of the application
	 */
	public SwingModelFileView(JmtFileFilter defaultFilter, JmtFileFilter defaultSaveFilter) {
		this.defaultFilter = defaultFilter;
		this.defaultSaveFilter = defaultSaveFilter;
		dialog = new JmtFileChooser(defaultSaveFilter);
	}

	public SwingModelFileView(JmtFileFilter saveFilter) {
		this(null, saveFilter);
	}

	@Override
	public File chooseFileToOpen(Object parent) {
		dialog.resetChoosableFileFilters();
		addCompatibleFilters();
		int status = dialog.showOpenDialog((Component) parent);
		// ERROR_OPTION is never returned by Swing in practice; treated as cancelled
		return status == JFileChooser.APPROVE_OPTION ? dialog.getSelectedFile() : null;
	}

	@Override
	public File chooseFileToSave(Object parent) {
		dialog.resetChoosableFileFilters();
		dialog.addChoosableFileFilter(defaultSaveFilter);
		dialog.setFileFilter(defaultSaveFilter);
		int status = dialog.showSaveDialog((Component) parent);
		return status == JFileChooser.APPROVE_OPTION ? dialog.getSelectedFile() : null;
	}

	@Override
	public boolean confirmOpenNewerVersion(Object parent, File file, String saved, String running) {
		if (GraphicsEnvironment.isHeadless()) {
			return true;
		}
		Object[] options = { "Open anyway", "Cancel" };
		int choice = JOptionPane.showOptionDialog((Component) parent, "<html>File <font color=#0000ff>" + file.getName()
				+ "</font> was saved by JMT <b>" + saved + "</b>, which is newer than the version you are running (<b>" + running
				+ "</b>).<br>It may use features this version does not support, and some data may be ignored or lost if you save it.<br>"
				+ "Do you want to open it anyway?</html>", "JMT - Newer file version", JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		return choice == 0;
	}

	@Override
	public boolean confirmSaveWithoutValidExtension(Object parent, File file) {
		int resultValue = JOptionPane.showConfirmDialog((Component) parent, "<html>File <font color=#0000ff>" + file.getName()
				+ "</font> does not have valid extension.<br>Do you want to replace it anyway?</html>", "JMT - Warning",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		return resultValue == JOptionPane.OK_OPTION;
	}

	/**
	 * Adds only compatible filters to current dialog
	 */
	private void addCompatibleFilters() {
		dialog.addChoosableFileFilter(ModelLoader.ALL);
		if (defaultFilter == ModelLoader.JABA) {
			dialog.addChoosableFileFilter(ModelLoader.JABA);
			dialog.setFileFilter(ModelLoader.JABA);
		} else {
			dialog.addChoosableFileFilter(ModelLoader.ALL_NOTJABA);
			dialog.addChoosableFileFilter(ModelLoader.JMODEL);
			dialog.addChoosableFileFilter(ModelLoader.JSIM);
			dialog.addChoosableFileFilter(ModelLoader.JMVA);
			dialog.setFileFilter(ModelLoader.ALL_NOTJABA);
		}
	}

	/**
	 * Custom file chooser class
	 */
	private static class JmtFileChooser extends JFileChooser {

		private static final long serialVersionUID = 1L;

		private final JmtFileFilter defaultSaveFilter;

		JmtFileChooser(JmtFileFilter defaultSaveFilter) {
			super(Defaults.getWorkingPath());
			this.defaultSaveFilter = defaultSaveFilter;
		}

		/**
		 * Overrides default method to provide a warning if saving over an existing file
		 */
		@Override
		public void approveSelection() {
			// Gets the chosen file name
			String name = getSelectedFile().getName();
			String parent = getSelectedFile().getParent();
			if (getDialogType() == OPEN_DIALOG) {
				super.approveSelection();
			}
			if (getDialogType() == SAVE_DIALOG) {
				boolean hasValidExtension = false;
				String[] extensions = defaultSaveFilter.getExtensions();
				for (String e : extensions) {
					if (name.toLowerCase().endsWith(e)) {
						hasValidExtension = true;
						break;
					}
				}
				if (!hasValidExtension) {
					name += extensions[0];
					setSelectedFile(new File(parent, name));
				}
				if (getSelectedFile().exists()) {
					int resultValue = JOptionPane.showConfirmDialog(this, "<html>File <font color=#0000ff>" + name
							+ "</font> already exists in this folder.<br>Do you want to replace it?</html>", "JMT - Warning",
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if (resultValue == JOptionPane.OK_OPTION) {
						getSelectedFile().delete();
						super.approveSelection();
					}
				} else {
					super.approveSelection();
				}
			}
		}
	}
}
