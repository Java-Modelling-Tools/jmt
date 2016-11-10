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

package jmt.gui.jwat.workloadAnalysis.panels;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jmt.framework.data.MacroReplacer;
import jmt.gui.jwat.JWATConstants;

public class LogVisualizer extends JPanel implements JWATConstants {

	private static final long serialVersionUID = 1L;

	private JList logList;
	private JDialog d;

	public LogVisualizer(JDialog d) {
		this.d = d;
		this.setLayout(new BorderLayout());
		logList = new JList(new DefaultListModel());
		JScrollPane p = new JScrollPane(logList);
		this.add(p, BorderLayout.CENTER);
		loadData();
	}

	private void loadData() {
		BufferedReader br = null;
		String line = null;
		DefaultListModel model = (DefaultListModel) logList.getModel();
		try {
			br = new BufferedReader(new FileReader(MacroReplacer.replace(LOG_FILE_NAME)));
			line = br.readLine();
			while (line != null) {
				model.addElement(line);
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			d.dispose();
			return;
		} catch (IOException e) {
			d.dispose();
		}
		try {
			br.close();
		} catch (IOException e) {
		}
	}

}
