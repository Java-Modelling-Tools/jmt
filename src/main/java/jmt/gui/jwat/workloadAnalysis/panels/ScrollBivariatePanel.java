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

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

import jmt.gui.jwat.workloadAnalysis.tables.JWatBivariateStatsTable;
import jmt.gui.jwat.workloadAnalysis.tables.JWatBivariateStatsTableModel;
import jmt.gui.jwat.workloadAnalysis.tables.renderers.RowHeaderRenderer;

public class ScrollBivariatePanel extends JScrollPane {

	private static final long serialVersionUID = 1L;

	private JWatBivariateStatsTable tab = null;

	public ScrollBivariatePanel(JWatBivariateStatsTable table) {
		tab = table;
		setViewportView(tab);
		ListModel lm = new AbstractListModel() {

			private static final long serialVersionUID = 1L;

			String headers[] = ((JWatBivariateStatsTableModel) tab.getModel()).getNames();

			public int getSize() {
				return headers.length;
			}

			public Object getElementAt(int index) {
				return headers[index];
			}

		};
		JList rowHeader = new JList(lm);
		rowHeader.setBackground(getBackground());
		rowHeader.setFixedCellWidth(100);
		rowHeader.setFixedCellHeight(tab.getRowHeight());
		rowHeader.setCellRenderer(new RowHeaderRenderer(tab));
		setRowHeaderView(rowHeader);
	}

	public void setNames() {
		ListModel lm = new AbstractListModel() {

			private static final long serialVersionUID = 1L;

			String headers[] = ((JWatBivariateStatsTableModel) tab.getModel()).getNames();

			public int getSize() {
				return headers.length;
			}

			public Object getElementAt(int index) {
				return headers[index];
			}

		};
		JList rowHeader = new JList(lm);
		rowHeader.setBackground(getBackground());
		rowHeader.setFixedCellWidth(100);
		rowHeader.setFixedCellHeight(tab.getRowHeight());
		rowHeader.setCellRenderer(new RowHeaderRenderer(tab));
		setRowHeaderView(rowHeader);
	}

}
