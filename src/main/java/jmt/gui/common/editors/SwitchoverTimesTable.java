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

package jmt.gui.common.editors;

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.table.editors.CheckBoxCellEditor;
import jmt.framework.gui.table.editors.ComboBoxCellEditor;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ServiceStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class SwitchoverTimesTable extends JTable implements CommonConstants {

	private static final long serialVersionUID = 1L;

	protected Object stationKey;
	protected StationDefinition data;
	protected ClassDefinition classData;
	protected Container panel;
	protected ImagedComboBoxCellEditorFactory classEditor;
	protected SwitchoverDistributionsTableModel tableModel;
	protected boolean[] bulkSelected;

	/**
	 * This field is used to initialize elements shown on Service type selection - Bertoli Marco
	 */
	protected Object[] serviceType = new Object[] { SERVICE_LOAD_INDEPENDENT, SERVICE_LOAD_DEPENDENT, SERVICE_ZERO, SERVICE_DISABLED };

	JButton editButton = new JButton("Edit");

	//editing of arrival time distribution
	protected AbstractAction editDistribution = new AbstractAction("Edit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Edits Service Time Distribution");
		}

		public void actionPerformed(ActionEvent e) {
			SwitchoverTimesTable serviceTable = SwitchoverTimesTable.this;
			// ---- Bertoli Marco ----
			int index = serviceTable.getSelectedRow();
			if (index >= 0 && index < serviceTable.getRowCount()) {
				Object fromClassKey = getFromClass(index);
				Object toClassKey = getToClass(index);
				Object service = data.getSwitchoverTimeDistribution(stationKey, getFromClass(index), getToClass(index));
				// If it is a Distribution, shows Distribution Editor
				if (service instanceof Distribution) {
					DistributionsEditor editor = DistributionsEditor.getInstance(panel.getParent(), (Distribution) service);
					// Sets editor window title
					editor.setTitle("Editing " + classData.getClassName(fromClassKey) + " to "
							+ classData.getClassName(toClassKey) + " Switchover Time Distribution...");
					// Shows editor window
					editor.show();
					// Sets new Distribution to selected class
					data.setSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey, editor.getResult());

					// Updates table view. This is needed as Distribution is not contained
					// into edited cell (but in its left one)
					serviceTable.repaint();
				}
				// Otherwise shows LDStrategy Editor
				else {
					LDStrategyEditor editor = LDStrategyEditor.getInstance(panel.getParent(), (LDStrategy) service);
					// Sets editor window title
					editor.setTitle("Editing " + classData.getClassName(fromClassKey) + " to "
							+ classData.getClassName(toClassKey) + " Load Dependent Switchover Time Strategy...");
					// Shows editor window
					editor.show();
					serviceTable.repaint();
				}
			}
			// ---- end ----
		}

	};

	int[] columnSizes = new int[] { 5, 35, 35, 55, 100, 15 };

	public SwitchoverTimesTable(StationDefinition sd, ClassDefinition cd, Object stationKey, Container panel) {
		this.stationKey = stationKey;
		this.panel = panel;
		this.classEditor = new ImagedComboBoxCellEditorFactory(cd);

		int classes = cd.getClassKeys().size();
		if (classes > 0) {
			this.bulkSelected = new boolean[classes * (classes - 1)];
		} else {
			this.bulkSelected = new boolean[0];
		}
		Arrays.fill(this.bulkSelected, false);

		classEditor.setData(cd);
		data = sd;
		classData = cd;

		tableModel = new SwitchoverDistributionsTableModel();

		setModel(tableModel);
		sizeColumns();
		setRowHeight(ROW_HEIGHT);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getTableHeader().setReorderingAllowed(false);
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		if (column == 0) {
			return CheckBoxCellEditor.getRendererInstance();
		} if (column == 1) {
			return classEditor.getRenderer();
		}  else if (column == 2) {
			return classEditor.getRenderer();
		} else if (column == 3) {
			return ComboBoxCellEditor.getRendererInstance();
		} else if (column == 5) {
			return new DisabledButtonCellRenderer(editButton);
		} else {
			return super.getCellRenderer(row, column);
		}
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (column == 0) {
			return CheckBoxCellEditor.getEditorInstance();
		} else if (column == 3) {
			return ComboBoxCellEditor.getEditorInstance(serviceType);
		} else if (column == 5) {
			return new ButtonCellEditor(new JButton(editDistribution));
		} else {
			return super.getCellEditor(row, column);
		}
	}

	private void sizeColumns() {
		for (int i = 0; i < columnSizes.length && i < getColumnCount(); i++) {
			this.getColumnModel().getColumn(i).setPreferredWidth(columnSizes[i]);
		}
	}

	/**
	 * Model used to modify switchover time distributions
	 */
	protected class SwitchoverDistributionsTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		protected String[] columnNames = new String[] { "Bulk Edit", "Class From", "Class To", "Strategy", "Switchover Time Distribution", "" };
		private final Class<?>[] columnClasses = new Class[] { Object.class, String.class, String.class, String.class, String.class, Object.class };

		public int getRowCount() {
			int classes = classData.getClassKeys().size();
			if (classes > 0) {
				return classes * (classes - 1);
			}
			return 0;
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			Object fromClassKey = getFromClass(rowIndex);
			Object toClassKey = getToClass(rowIndex);
			Object currentDist = data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey);
			if (columnIndex == 0 || columnIndex == 3) {
				return true;
			} else {
				return columnIndex == 5 && !(currentDist instanceof ZeroStrategy) && !(currentDist instanceof DisabledStrategy);
			}
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Object fromClassKey = getFromClass(rowIndex);
			Object toClassKey = getToClass(rowIndex);
			switch (columnIndex) {
			case (0):
				return bulkSelected[rowIndex];
			case (1):
				return fromClassKey;
			case (2):
				return toClassKey;
			case (3):
				// Checks if current service section is load dependent or independent
				if (data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey) instanceof LDStrategy) {
					return SERVICE_LOAD_DEPENDENT;
				} else if (data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey) instanceof ZeroStrategy) {
					return SERVICE_ZERO;
				} else if (data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey) instanceof DisabledStrategy) {
					return SERVICE_DISABLED;
				} else {
					return SERVICE_LOAD_INDEPENDENT;
				}
			case (4):
				return data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey);
			}
			return null;
		}

		/**Puts edited values to the underlying data structure for model implementation*/
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			Object fromClassKey = getFromClass(rowIndex);
			Object toClassKey = getToClass(rowIndex);
			switch (columnIndex) {
			// Load dependency
			case (0):
				bulkSelected[rowIndex] = (boolean) aValue;
			repaint();
			break;
			case (3):
				if (((String) aValue).equals(SERVICE_LOAD_DEPENDENT)) {
					// Puts a Load Dependent Service Strategy only if previously it was different
					if (!(data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey) instanceof LDStrategy)) {
						data.setSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey, new LDStrategy());
					}
				} else if (((String) aValue).equals(SERVICE_ZERO)) {
					// Puts a Zero Service Time Strategy only if previously it was different
					if (!(data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey) instanceof ZeroStrategy)) {
						data.setSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey, new ZeroStrategy());
					}
				} else if (((String) aValue).equals(SERVICE_DISABLED)) {
					// Puts a Disabled Service Time Strategy only if previously it was different
					if (!(data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey) instanceof DisabledStrategy)) {
						data.setSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey, new DisabledStrategy());
					}
				} else {
					// Puts the default service strategy only if previously it was different
					if (!(data.getSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey) instanceof Distribution)) {
						Object distribution = new Exponential();
						data.setSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey, distribution);
					}
				}
			repaint();
			}
		}

	}

	private Object getFromClass(int rowIndex) {
		int classCount = classData.getClassKeys().size();
		int fromIndex = rowIndex / (classCount - 1);
		return classData.getClassKeys().get(fromIndex);
	}

	private Object getToClass(int rowIndex) {
		int classCount = classData.getClassKeys().size();
		int fromIndex = rowIndex / (classCount - 1);
		int toIndex = rowIndex % (classCount - 1);
		if (toIndex >= fromIndex) {
			toIndex += 1;
		}
		return classData.getClassKeys().get(toIndex);
	}

	public void applyBulkSelected(ServiceStrategy dist) {
		for (int i = 0; i < bulkSelected.length; i++) {
			if (bulkSelected[i]) {
				Object fromClassKey = getFromClass(i);
				Object toClassKey = getToClass(i);
				data.setSwitchoverTimeDistribution(stationKey, fromClassKey, toClassKey, dist.clone());
			}
		}
		repaint();
	}

	protected class DisabledButtonCellRenderer extends ButtonCellEditor {

		private static final long serialVersionUID = 1L;

		private JButton button;

		public DisabledButtonCellRenderer(JButton jbutt) {
			super(jbutt);
			button = jbutt;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (table.isCellEditable(row, column)) {
				button.setEnabled(true);
			} else {
				button.setEnabled(false);
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}

	}

}
