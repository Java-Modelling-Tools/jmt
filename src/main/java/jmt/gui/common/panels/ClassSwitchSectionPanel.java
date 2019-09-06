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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellEditor;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.exact.table.DisabledCellRenderer;
import jmt.gui.exact.table.ExactTable;
import jmt.gui.exact.table.ExactTableModel;

public class ClassSwitchSectionPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	private static final String DESCRIPTION = HTML_START
			+ "The value (i, j) is the probability that a job of class i switches to "
			+ "class j. The values may be either probabilities or number of visits. "
			+ "If the sum of a row is greater than one, the values will be normalized "
			+ "in order to sum to one. A sum less than one will raise an error."
			+ HTML_END;
	private static final String NORMALIZATION_ERROR = HTML_START
			+ "<font color=\"red\"> Error: some rows sum to less than one.</font>"
			+ HTML_END;
	private static final String NORMALIZATION_WARNING = HTML_START
			+ "<font color=\"blue\"> Warning: some rows sum to greater than one. "
			+ "The values will be normalized.</font>"
			+ HTML_END;

	/** Used to display classes with icon */
	private JTable csTable;
	private StationDefinition stationData;
	private ClassDefinition classData;
	private Object stationKey;
	private JLabel descriptionLabel;
	private JLabel normalizationErrorLabel;
	private JLabel normalizationWarningLabel;
	private boolean errorRows[];
	private boolean warningRows[];

	public ClassSwitchSectionPanel(StationDefinition sd, ClassDefinition cd,
			Object stationKey) {
		setData(sd, cd, stationKey);
	}

	public void setData(StationDefinition sd, ClassDefinition cd,
			Object stationKey) {
		this.stationData = sd;
		this.classData = cd;
		this.stationKey = stationKey;
		if (descriptionLabel != null) {
			descriptionLabel.setVisible(classData.getClassKeys().size() != 0);
		}
		if (classData.getClassKeys().size() > 0) {
			csTable = new ClassSwitchTable();
			this.errorRows = new boolean[classData.getClassKeys().size()];
			this.warningRows = new boolean[classData.getClassKeys().size()];
		} else {
			csTable = new JTable();
		}
		initComponents();
	}

	protected void initComponents() {
		removeAll();
		setLayout(new BorderLayout());

		WarningScrollTable wST = new WarningScrollTable(csTable, WARNING_CLASS);
		setBorder(new TitledBorder(new EtchedBorder(), "CS Strategies"));
		setMinimumSize(new Dimension(180, 100));
		descriptionLabel = new JLabel(DESCRIPTION);
		normalizationErrorLabel = new JLabel(NORMALIZATION_ERROR);
		normalizationWarningLabel = new JLabel(NORMALIZATION_WARNING);
		checkRowLessThanOne();
		add(wST, BorderLayout.CENTER);
		add(descriptionLabel, BorderLayout.SOUTH);
		JPanel msgPanel = new JPanel(new BorderLayout());
		msgPanel.add(normalizationErrorLabel, BorderLayout.NORTH);
		msgPanel.add(normalizationWarningLabel, BorderLayout.SOUTH);
		add(msgPanel, BorderLayout.NORTH);
		if (descriptionLabel != null) {
			descriptionLabel.setVisible(classData.getClassKeys().size() != 0);
		}
	}

	/**
	 * called by the Wizard before when switching to another panel
	 */
	@Override
	public void lostFocus() {
		TableCellEditor editor = csTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}

	/**
	 * called by the Wizard when the panel becomes active
	 */
	@Override
	public void gotFocus() {
		if (descriptionLabel != null) {
			descriptionLabel.setVisible(classData.getClassKeys().size() != 0);
		}
	}

	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "Class Switch Matrix";
	}

	private void checkRowLessThanOne() {
		boolean allIsOk = true;
		boolean noWarning = true;
		Vector<Object> classes = classData.getClassKeys();
		for (int i = 0; i < classes.size(); i++) {
			Object classInKey = classes.get(i);
			float row = 0;
			for (int j = 0; j < classes.size(); j++) {
				Object classOutKey = classes.get(j);
				row += stationData.getClassSwitchMatrix(stationKey, classInKey, classOutKey);
			}
			if (row < 1) {
				allIsOk = false;
				errorRows[i] = true;
				warningRows[i] = false;
				normalizationErrorLabel.setVisible(true);				
			} else if (row > 1) {
				noWarning  = false;
				warningRows[i] = true;
				errorRows[i] = false;
				normalizationWarningLabel.setVisible(true);				
			} else {
				warningRows[i] = false;
				errorRows[i] = false;
			}
		}
		if (allIsOk) {
			normalizationErrorLabel.setVisible(false);				
		}
		if (noWarning) {
			normalizationWarningLabel.setVisible(false);				
		}
	}

	private String getPercValueCell(int row, int col) {
		float sum = 0;
		Vector<Object> classes = classData.getClassKeys();
		for (int j = 0; j < classes.size(); j++) {
			sum += stationData.getClassSwitchMatrix(stationKey,
					classes.get(row), classes.get(j));
		}
		if (sum <= 0.0000001) {
			return "0%";
		}
		float res= stationData.getClassSwitchMatrix(stationKey,
				classes.get(row), classes.get(col)) / sum * 100;
		return Math.round(res) + "%";
	}

	protected class ClassSwitchTable extends ExactTable {

		private static final long serialVersionUID = 1L;

		private static final int DEFAULT_COLUMN_WIDTH = 110;

		public ClassSwitchTable() {
			super(new ClassSwitchTableModel());
			setRowSelectionAllowed(true);
			setColumnSelectionAllowed(true);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			setRowHeight(CommonConstants.ROW_HEIGHT);
			setDefaultRenderer(Object.class, new ClassSwitchTableRenderer());
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setPreferredWidth(DEFAULT_COLUMN_WIDTH);
			}
		}

	}

	protected class ClassSwitchTableRenderer extends DisabledCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
			if (table.isRowSelected(row)) {
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
			} else {
				JLabel label = new JLabel((String) value, SwingConstants.CENTER);
				if (errorRows[row]) {
					label.setForeground(Color.RED);
				} else if (warningRows[row]) {
					label.setForeground(Color.BLUE);
				}
				return label;
			}
		}

	}

	protected class ClassSwitchTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		public int[] columnSizes = new int[] { 70, 100 };

		public ClassSwitchTableModel() {
			prototype = "Class10000";
			rowHeaderPrototype = "Class10000";
		}

		public int getRowCount() {
			return classData.getClassKeys().size();
		}

		public int getColumnCount() {
			return classData.getClassKeys().size();
		}

		@Override
		protected Object getRowName(int rowIndex) {
			return classData.getClassName(classData.getClassKeys().get(rowIndex));
		}

		@Override
		public String getColumnName(int columnIndex) {
			return classData.getClassName(classData.getClassKeys().get(columnIndex));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Class getColumnClass(int columnIndex) {
			if (columnIndex == -1) {
				return String.class;
			} else {
				return String.class;
			}
		}

		@Override
		public Object getPrototype(int i) {
			if (i == -1) {
				return rowHeaderPrototype;
			} else {
				return prototype;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			Vector<Object> classes = classData.getClassKeys();
			Object rowClassKey = classes.get(rowIndex);
			Object columnClassKey = classes.get(columnIndex);
			int rowClassType = classData.getClassType(rowClassKey);
			int columnClassType = classData.getClassType(columnClassKey);
			return rowClassType == columnClassType;
		}

		@Override
		protected Object getValueAtImpl(int rowIndex, int columnIndex) {
			if (!isCellEditable(rowIndex, columnIndex))
				return null;
			Vector<Object> classes = classData.getClassKeys();
			if (csTable.isRowSelected(rowIndex)) {
				return stationData.getClassSwitchMatrix(stationKey,
						classes.get(rowIndex), classes.get(columnIndex));
			} else {
				return stationData.getClassSwitchMatrix(stationKey,
						classes.get(rowIndex), classes.get(columnIndex))
						+ " (" + getPercValueCell(rowIndex, columnIndex) + ")";
			}
		}

		@Override
		public void setValueAt(Object input, int rowIndex, int columnIndex) {
			float value;
			Vector<Object> classes = classData.getClassKeys();
			try {
				value = Float.parseFloat((String) input);
			} catch (Exception e) {
				value = 0;
			}
			stationData.setClassSwitchMatrix(stationKey,
					classes.get(rowIndex), classes.get(columnIndex), value);
			checkRowLessThanOne();
		}

	}

}
