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

package jmt.gui.jsimgraph.panels;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.table.editors.ColorCellEditor;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.editors.ImagedComboBoxCellEditorFactory;
import jmt.gui.exact.table.DisabledCellRenderer;
import jmt.gui.jsimgraph.JSimGraphConstants;
import jmt.gui.jsimgraph.definitions.JmodelClassDefinition;
import jmt.gui.jsimgraph.definitions.JmodelStationDefinition;
import jmt.gui.jsimwiz.panels.ClassesPanel;

/**
 * <p>Title: JModel User Classes Panel</p>
 * <p>Description: Panel used to edit user classes properties. Extends JSIM ClassesPanel.
 * In some points it is a bit tricky, as original ClassesPanel was not meant to be extended.
 * I preferred not to change ClassesPanel source code, nor copy and modify it as
 * at the time of writing it was in early development stage and I did not want to collide
 * with the author.</p>
 * 
 * @author Bertoli Marco
 *         Date: 14-giu-2005
 *         Time: 9.42.15
 */
public class JModelClassesPanel extends ClassesPanel implements JSimGraphConstants {

	private static final long serialVersionUID = 1L;

	protected JmodelClassDefinition cd;
	protected JmodelStationDefinition sd;
	protected ImagedComboBoxCellEditorFactory combos;

	/**
	 * Construct a new JModelClassesPanel
	 * @param cd a reference to the underlying data structure
	 */
	public JModelClassesPanel(JmodelClassDefinition cd, JmodelStationDefinition sd) {
		this.cd = cd;
		this.sd = sd;
		combos = new ImagedComboBoxCellEditorFactory(sd);
		classTable = new JModelClassTable();
		initComponents();
		setData(cd);
	}

	/**Sets data model for this panel.
	 * Instantly all of the panel components are assigned their specific value.
	 * @param cd: data for class definition.*/
	@Override
	public void setData(ClassDefinition cd) {
		data = cd;
		classTable.setModel(new JModelClassTableModel());
		classNumSpinner.setValue(new Integer(data.getClassKeys().size()));
	}

	/**
	 * Adds a new class
	 */
	@Override
	protected void addClass() {
		cd.addClass();
		refreshComponents();
	}

	/**
	 * Overrides ClassTable. It is a table containing class parameters.
	 * Added support for "Color" column and reference station combobox
	 */
	protected class JModelClassTable extends ClassTable {

		private static final long serialVersionUID = 1L;

		public JModelClassTable() {
			columnSizes = new int[] { 8, 100, 100, 28, 38, 140, 39, 200, 18 };
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 0) {
				return new ColorCellEditor();
			} else if (column == 2) {
				return super.getCellRenderer(row, 1);
			} else if (column == 6) {
				if (getValueAt(row, 5) != null) {
					return new ButtonCellEditor(editDistributionButton);
				} else {
					return new DisabledCellRenderer();
				}
			} else if (column == 7) {
				return combos.getRenderer();
			} else if (column == 8) {
				return new ButtonCellEditor(deleteButton);
			} else {
				return getDefaultRenderer(getModel().getColumnClass(column));
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 0) {
				return new ColorCellEditor();
			} else if (column == 2) {
				return super.getCellEditor(row, 1);
			} else if (column == 6) {
				return new ButtonCellEditor(new JButton(editDistribution));
			} else if (column == 7) {
				if (getValueAt(row, 2).equals("Open")) {
					return combos.getEditor(sd.getStationKeysRefStation());
				} else {
					return combos.getEditor(sd.getStationKeysNoSourceSink());
				}
			} else if (column == 8) {
				return new ButtonCellEditor(new JButton(deleteClass));
			} else {
				return getDefaultEditor(getModel().getColumnClass(column));
			}
		}
	}

	/**
	 * Define a custom table model. Overrides ClassTableModel to add support for "Color" column
	 */
	protected class JModelClassTableModel extends ClassTableModel {

		private static final long serialVersionUID = 1L;

		public JModelClassTableModel() {
			columnNames = new String[] { "Color", "Name", "Type", "Priority", "Population", "Interarrival Time Distribution", "",
					"Reference Station", "" };
			colClasses = new Class[] { Color.class, String.class, JComboBox.class, String.class, String.class, String.class, Object.class,
					JComboBox.class, JButton.class };
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex == 4 && getValueAt(rowIndex, 2).equals("Open")) {
				return false;
			} else if (columnIndex == 5) {
				return false;
			} else if (columnIndex == 6 && getValueAt(rowIndex, 2).equals("Closed")) {
				return false;
			} else if (columnIndex == 6 && (STATION_TYPE_FORK.equals(getValueAt(rowIndex, 7))
					|| STATION_TYPE_CLASSSWITCH.equals(getValueAt(rowIndex, 7))
					|| STATION_TYPE_SCALER.equals(getValueAt(rowIndex, 7))
					|| STATION_TYPE_TRANSITION.equals(getValueAt(rowIndex, 7)))) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return cd.getClassColor(cd.getClassKeys().get(rowIndex));
			} else if (columnIndex == 7) {
				return cd.getClassRefStation(cd.getClassKeys().get(rowIndex));
			} else {
				return super.getValueAt(rowIndex, columnIndex - 1);
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				cd.setClassColor(cd.getClassKeys().get(rowIndex), (Color) aValue);
			} else if (columnIndex == 7) {
				cd.setClassRefStation(cd.getClassKeys().get(rowIndex), aValue);
				refreshComponents();
			} else {
				super.setValueAt(aValue, rowIndex, columnIndex - 1);
			}
		}

	}

}
