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

package jmt.gui.common.panels;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JComponent;

import jmt.framework.gui.components.JMTDialog;
import jmt.framework.gui.wizard.WizardPanel;

/**
 * This dialog factory is used to display all template relevant panels.
 * 
 * @author S Jiang
 *
 */
public class CustomizableDialogFactory {

	private JMTDialog dialogFrame;
	private Frame mainWindow;

	public CustomizableDialogFactory(Frame mainWindow) {
		this.mainWindow = mainWindow;
	}

	private void createDialog(int width, int height) {
		// Creates modal dialog
		dialogFrame = new JMTDialog(mainWindow, true);
		dialogFrame.centerWindow(width, height);
		dialogFrame.setResizable(true);
		dialogFrame.getContentPane().setLayout(new BorderLayout());
	}

	public void getDialog(int width, int height, final JComponent panel, String title) {
		createDialog(width, height);
		// Adds panel
		dialogFrame.getContentPane().add(panel, BorderLayout.CENTER);
		// Sets title
		if (title != null) {
			dialogFrame.setTitle(title);
		}
		// If this is a wizard panel call gotFocus() method
		if (panel instanceof WizardPanel) {
			((WizardPanel) panel).gotFocus();
		}
		// Shows dialog
		dialogFrame.setVisible(true);
	}

}
