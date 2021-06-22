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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.framework.gui.table.editors.ComboBoxCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.editors.GrayCellRenderer;
import jmt.gui.common.editors.ImagedComboBoxCellEditorFactory;
import jmt.gui.exact.table.BooleanCellRenderer;
import jmt.gui.exact.table.ExactCellEditor;
import jmt.gui.exact.table.ExactCellRenderer;

/**
 * <p>Title: Storage Section Panel</p>
 * <p>Description: This panel is used to parametrise the storage section.</p>
 *
 * @author Lulai Zhu
 * Date: 24-06-2016
 * Time: 16.00.00
 */
public class StorageSectionPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	private static final Object[] queuePolicies = { QUEUE_STRATEGY_FCFS, QUEUE_STRATEGY_LCFS, QUEUE_STRATEGY_RAND };
	private static final Object[] dropRules = { FINITE_DROP, FINITE_BLOCK, FINITE_WAITING, FINITE_RETRIAL};

	private boolean isInitComplete;

	private StationDefinition stationData;
	private ClassDefinition classData;
	private Object stationKey;

	private ImagedComboBoxCellEditorFactory classEditor;
	private JSpinner capacitySpinner;
	private JCheckBox infiniteCheckBox;
	private StorageOptionTable optionTable;

	public StorageSectionPanel(StationDefinition sd, ClassDefinition cd, Object sk) {
		isInitComplete = false;
		classEditor = new ImagedComboBoxCellEditorFactory(cd);
		setData(sd, cd, sk);
		initComponents();
		addDataManagers();
		updateCapacity();
		isInitComplete = true;
	}

	public void setData(StationDefinition sd, ClassDefinition cd, Object sk) {
		stationData = sd;
		classData = cd;
		stationKey = sk;
		classEditor.setData(cd);
		if (isInitComplete) {
			updateCapacity();
		}
	}

	private void initComponents() {
		setLayout(new BorderLayout(5, 5));
		setBorder(new EmptyBorder(5, 5, 5, 5));

		capacitySpinner = new JSpinner();
		capacitySpinner.setPreferredSize(DIM_BUTTON_XS);
		infiniteCheckBox = new JCheckBox("Infinite");
		JPanel capacityPanel = new JPanel();
		capacityPanel.setBorder(new TitledBorder(new EtchedBorder(), "Storage Capacity"));
		capacityPanel.add(new JLabel("Capacity: "));
		capacityPanel.add(capacitySpinner);
		capacityPanel.add(infiniteCheckBox);

		optionTable = new StorageOptionTable();
		JPanel optionPanel = new JPanel(new BorderLayout());
		optionPanel.setBorder(new TitledBorder(new EtchedBorder(), "Storage Options"));
		optionPanel.add(new WarningScrollTable(optionTable, WARNING_CLASS));

		add(capacityPanel, BorderLayout.NORTH);
		add(optionPanel, BorderLayout.CENTER);
	}

	private void addDataManagers() {
		capacitySpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				Object value = capacitySpinner.getValue();
				if (value instanceof Integer) {
					if (((Integer) value).intValue() < 1) {
						value = Integer.valueOf(1);
						capacitySpinner.setValue(value);
						return;
					}
					stationData.setStationQueueCapacity(stationKey, (Integer) value);
				} else {
					stationData.setStationQueueCapacity(stationKey, Integer.valueOf(-1));
				}
			}
		});

		infiniteCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TableCellEditor editor = optionTable.getCellEditor();
				if (editor != null) {
					editor.stopCellEditing();
				}
				if (infiniteCheckBox.isSelected()) {
					capacitySpinner.setValue(Double.POSITIVE_INFINITY);
					capacitySpinner.setEnabled(false);
					Vector<Object> classKeys = classData.getClassKeys();
					for (Object classKey : classKeys) {
						if (stationData.getQueueCapacity(stationKey, classKey).intValue() < 1) {
							stationData.setDropRule(stationKey, classKey, Defaults.get("placeDropRule"));
						}
					}
				} else {
					capacitySpinner.setValue(Defaults.getAsInteger("placeCapacity"));
					capacitySpinner.setEnabled(true);
				}
				optionTable.repaint();
			}
		});
	}

	private void updateCapacity() {
		Integer capacity = stationData.getStationQueueCapacity(stationKey);
		if (capacity.intValue() < 1) {
			infiniteCheckBox.setSelected(true);
			capacitySpinner.setValue(Double.POSITIVE_INFINITY);
			capacitySpinner.setEnabled(false);
		} else {
			infiniteCheckBox.setSelected(false);
			capacitySpinner.setValue(capacity);
			capacitySpinner.setEnabled(true);
		}
	}

	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "Storage Section";
	}

	/**
	 * called by the Wizard before when switching to another panel
	 */
	@Override
	public void lostFocus() {
		TableCellEditor editor = optionTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}

	/**
	 * called by the Wizard when the panel becomes active
	 */
	@Override
	public void gotFocus() {
		classEditor.clearCache();
	}

	protected class StorageOptionTable extends JTable {

		private static final long serialVersionUID = 1L;

		private ExactCellRenderer capacityRenderer;
		private DisabledComboBoxCellRenderer dropRuleRenderer;

		public StorageOptionTable() {
			capacityRenderer = new GrayCellRenderer();
			dropRuleRenderer = new DisabledComboBoxCellRenderer(INFINITE_CAPACITY);
			setModel(new StorageOptionTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setRowHeight(ROW_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setPreferredWidth(((StorageOptionTableModel) getModel()).getColumnSize(i));
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 3) {
				return ComboBoxCellEditor.getEditorInstance(queuePolicies);
			} else if (column == 4) {
				return ComboBoxCellEditor.getEditorInstance(dropRules);
			} else {
				return super.getCellEditor(row, column);
			}
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 0) {
				return classEditor.getRenderer();
			} else if (column == 1) {
				return capacityRenderer;
			} else if (column == 2) {
				return new BooleanCellRenderer();
			} else if (column == 3) {
				return ComboBoxCellEditor.getRendererInstance();
			} else if (column == 4) {
				return dropRuleRenderer;
			} else {
				return super.getCellRenderer(row, column);
			}
		}
	}

	private class StorageOptionTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private String[] columnNames = new String[] { "Class", "Capacity", "\u221e", "Queue Policy", "Drop Rule" };
		private Class<?>[] columnClasses = new Class[] { String.class, String.class, Boolean.class, String.class, String.class };
		private int[] columnSizes = new int[] { 125, 80, 20, 125, 125 };

		@Override
		public int getRowCount() {
			return classData.getClassKeys().size();
		}

		@Override
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

		public int getColumnSize(int columnIndex) {
			return columnSizes[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			Object classKey = classData.getClassKeys().get(rowIndex);
			if (columnIndex == 0) {
				return false;
			} else if (columnIndex == 1 && stationData.getQueueCapacity(stationKey, classKey).intValue() < 1) {
				return false;
			} else if (columnIndex == 4 && stationData.getStationQueueCapacity(stationKey).intValue() < 1
					&& stationData.getQueueCapacity(stationKey, classKey).intValue() < 1) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object classKey = classData.getClassKeys().get(rowIndex);
			if (columnIndex == 0) {
				return classKey;
			} else if (columnIndex == 1) {
				Integer capacity = stationData.getQueueCapacity(stationKey, classKey);
				return (capacity.intValue() > 0) ? capacity : "\u221e";
			} else if (columnIndex == 2) {
				Integer capacity = stationData.getQueueCapacity(stationKey, classKey);
				return Boolean.valueOf(capacity.intValue() < 1);
			} else if (columnIndex == 3) {
				return stationData.getQueueStrategy(stationKey, classKey);
			} else if (columnIndex == 4) {
				return stationData.getDropRule(stationKey, classKey);
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			Object classKey = classData.getClassKeys().get(rowIndex);
			if (columnIndex == 1) {
				try {
					Integer capacity = Integer.valueOf((String) aValue);
					if (capacity.intValue() < 1) {
						capacity = Integer.valueOf(1);
					}
					stationData.setQueueCapacity(stationKey, classKey, capacity);
				} catch (NumberFormatException e) {
					// Aborts modification if String is invalid
				}
			} else if (columnIndex == 2) {
				if (((Boolean) aValue).booleanValue()) {
					stationData.setQueueCapacity(stationKey, classKey, Integer.valueOf(-1));
					if (stationData.getStationQueueCapacity(stationKey).intValue() < 1) {
						stationData.setDropRule(stationKey, classKey, Defaults.get("placeDropRule"));
					}
				} else {
					Integer capacity = Defaults.getAsInteger("placeQueueCapacity");
					if (capacity.intValue() < 1) {
						capacity = Integer.valueOf(1);
					}
					stationData.setQueueCapacity(stationKey, classKey, capacity);
				}
				repaint();
			} else if (columnIndex == 3) {
				stationData.setQueueStrategy(stationKey, classKey, (String) aValue);
			} else if (columnIndex == 4) {
				stationData.setDropRule(stationKey, classKey, (String) aValue);
			}
		}

	}

	private class DisabledComboBoxCellRenderer extends ComboBoxCellEditor {

		private static final long serialVersionUID = 1L;

		private JLabel label;

		public DisabledComboBoxCellRenderer(String text) {
			label = new JLabel(text);
			label.setEnabled(false);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (!table.isCellEditable(row, column)) {
				return label;
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}

	}

}
