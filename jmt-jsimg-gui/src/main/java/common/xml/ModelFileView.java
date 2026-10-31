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

package common.xml;

import java.io.File;

/**
 * What {@link ModelLoader} needs from the user interface: choosing files and answering confirmations.
 * {@code parent} is opaque (a Component for Swing) so callers keep passing the window they have.
 */
public interface ModelFileView {
	/** Asks for a file to open; returns null if cancelled. */
	File chooseFileToOpen(Object parent);

	/** Asks for a file to save to; returns null if cancelled. */
	File chooseFileToSave(Object parent);

	/** Archive was saved by a newer JMT; true = open anyway. */
	boolean confirmOpenNewerVersion(Object parent, File file, String savedVersion, String runningVersion);

	/** Target has no valid extension; true = save anyway. */
	boolean confirmSaveWithoutValidExtension(Object parent, File file);
}
