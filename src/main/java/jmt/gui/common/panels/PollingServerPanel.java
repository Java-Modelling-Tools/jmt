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

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.table.editors.ComboBoxCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.editors.DistributionsEditor;
import jmt.gui.common.editors.ImagedComboBoxCellEditorFactory;
import jmt.gui.common.editors.LDStrategyEditor;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Menu for editing polling server properties
 * @author Ahmed Salem
 */
public class PollingServerPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	protected StationDefinition data;
	protected Object stationKey;
	protected ClassDefinition classData;

	protected ImagedComboBoxCellEditorFactory classEditor;
	protected SwitchoverTable switchoverTable;

	protected JComboBox pollingTypeCombo;
	protected JSpinner pollingKValue;

	protected static final String[] pollingTypes = {
			STATION_QUEUE_STRATEGY_POLLING_LIMITED,
			STATION_QUEUE_STRATEGY_POLLING_GATED,
			STATION_QUEUE_STRATEGY_POLLING_EXHAUSTIVE
	};

	protected AbstractAction editDistribution = new AbstractAction("Edit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Edits Service Time Distribution");
		}

		public void actionPerformed(ActionEvent e) {
			int index = switchoverTable.getSelectedRow();
			if (index >= 0 && index < switchoverTable.getRowCount()) {
				Object key = classData.getClassKeys().elementAt(index);
				Object service = data.getPollingSwitchoverDistribution(stationKey, key);
				if (service instanceof Distribution) {
					DistributionsEditor editor = DistributionsEditor.getInstance(PollingServerPanel.this.getParent(), (Distribution) service);
					editor.setTitle("Editing " + classData.getClassName(key) + " Switchover Time Distribution...");
					editor.setVisible(true);
					data.setPollingSwitchoverDistribution(stationKey, key, editor.getResult());
					switchoverTable.repaint();
				} else {
					LDStrategyEditor editor = LDStrategyEditor.getInstance(PollingServerPanel.this.getParent(), (LDStrategy) service);
					editor.setTitle("Editing " + classData.getClassName(key) + " Load Dependent Switchover Strategy...");
					editor.setVisible(true);
					switchoverTable.repaint();
				}
			}
		}

	};

	public PollingServerPanel(Dialog owner, StationDefinition data, Object stationKey, ClassDefinition cd) {
		setData(data, stationKey, cd);
	}

	private void setData(StationDefinition sd, Object sk, ClassDefinition cd) {
		data = sd;
		stationKey = sk;
		classData = cd;
		classEditor = new ImagedComboBoxCellEditorFactory(cd);
		classEditor.setData(cd);

		switchoverTable = new SwitchoverTable();

		pollingTypeCombo = new JComboBox();
		pollingTypeCombo.setModel(new DefaultComboBoxModel(pollingTypes));

		pollingKValue = new JSpinner();
		pollingKValue.setPreferredSize(DIM_BUTTON_XS);
		updateServerPreferences();
		addDataManagers();
		initComponents();
	}

	private void initComponents() {
		this.setLayout(new BorderLayout(5, 5));
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout(3, 3));
		topPanel.setBorder(new TitledBorder(new EtchedBorder(), "Polling Server Preferences"));
		JPanel pollingSettings = new JPanel();
		pollingSettings.add(Box.createRigidArea(new Dimension((int)(5 * CommonConstants.widthScaling), (int)(0 * CommonConstants.heightScaling))));
		pollingSettings.add(new JLabel("Polling Type:"));
		pollingSettings.add(pollingTypeCombo);
		pollingSettings.add(Box.createRigidArea(new Dimension((int)(5 * CommonConstants.widthScaling), (int)(0 * CommonConstants.heightScaling))));
		pollingSettings.add(new JLabel("Limit Value K"));
		pollingSettings.add(pollingKValue);
		topPanel.add(pollingSettings, BorderLayout.NORTH);
		this.add(topPanel, BorderLayout.NORTH);

		WarningScrollTable ServiceSectionTable = new WarningScrollTable(switchoverTable, WARNING_CLASS);
		ServiceSectionTable.setBorder(new TitledBorder(new EtchedBorder(), "Switchover Time Distributions"));
		this.add(ServiceSectionTable, BorderLayout.CENTER);
	}

	private void updateServerPreferences() {
		pollingKValue.setValue(data.getStationPollingServerKValue(stationKey));
		String pollingType = data.getStationPollingServerType(stationKey);
		int index = 0;
		for (int i = 0; i < pollingTypes.length; i++) {
			if (pollingTypes[i].equals(pollingType)) {
				index = i;
				break;

			}
		}
		pollingTypeCombo.setSelectedIndex(index);
		pollingKValue.setEnabled(pollingType.equals(STATION_QUEUE_STRATEGY_POLLING_LIMITED));
	}

	private void addDataManagers() {
		pollingTypeCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String pollingType = (String) pollingTypeCombo.getSelectedItem();
				data.setStationPollingServerType(stationKey, pollingType);
				pollingKValue.setEnabled(pollingType.equals(STATION_QUEUE_STRATEGY_POLLING_LIMITED));
			}
		});

		pollingKValue.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (pollingKValue.getValue() instanceof Integer) {
					Integer kValue = (Integer) pollingKValue.getValue();
					if (kValue.intValue() < 1) {
						kValue = new Integer(1);
						pollingKValue.setValue(kValue);
					}

					data.setStationPollingServerKValue(stationKey, kValue);
				}
			}
		});

	}

	protected class SwitchoverTable extends JTable {

		private static final long serialVersionUID = 1L;
		protected Object[] serviceType = new Object[] { SWITCHOVER_ZERO, SERVICE_LOAD_INDEPENDENT, SERVICE_LOAD_DEPENDENT};
		JButton editButton = new JButton() {
			private static final long serialVersionUID = 1L;
			{
				setText("Edit");
			}
		};

		int[] columnSizes = new int[] { 90, 90, 150, 30 };

		public SwitchoverTable() {
			setModel(new SwitchoverTableModel());
			sizeColumns();
			setRowHeight(ROW_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 0) {
				return classEditor.getRenderer();
			} else if (column == 1) {
				return ComboBoxCellEditor.getRendererInstance();
			} else if (column == 3) {
				return new PollingServerPanel.DisabledButtonCellRenderer(editButton);
			} else {
				return super.getCellRenderer(row, column);
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 1) {
				return ComboBoxCellEditor.getEditorInstance(serviceType);
			} else if (column == 3) {
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
	}

	protected class SwitchoverTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Class", "Strategy", "Switchover Time Distribution", "" };
		private Class<?>[] columnClasses = new Class[] { String.class, String.class, String.class, Object.class };

		public int getRowCount() {
			return classData.getClassKeys().size();
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
			Object classKey = classData.getClassKeys().get(rowIndex);
			if (columnIndex == 0) {
				return false;
			} else if (columnIndex == 2) {
				return false;
			} else if (columnIndex == 3 && data.getPollingSwitchoverDistribution(stationKey, classKey) instanceof ZeroStrategy) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object classKey = classData.getClassKeys().get(rowIndex);
			switch (columnIndex) {
			case (0):
				return classKey;
			case (1):
				// Checks if current service section is load dependent or independent
				if (data.getPollingSwitchoverDistribution(stationKey, classKey) instanceof LDStrategy) {
					return SERVICE_LOAD_DEPENDENT;
				} else if (data.getPollingSwitchoverDistribution(stationKey, classKey) instanceof ZeroStrategy) {
					return SWITCHOVER_ZERO;
				} else if (data.getPollingSwitchoverDistribution(stationKey, classKey) instanceof DisabledStrategy) {
					return SERVICE_DISABLED;
				} else {
					return SERVICE_LOAD_INDEPENDENT;
				}
			case (2):
				return data.getPollingSwitchoverDistribution(stationKey, classKey);
			}
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			Object classKey = classData.getClassKeys().get(rowIndex);
			switch (columnIndex) {
			// Load dependency
			case (1):
				switch (((String) aValue)) {
				case SERVICE_LOAD_DEPENDENT:
					// Puts a Load Dependent Service Strategy only if previously it was different
					if (!(data.getPollingSwitchoverDistribution(stationKey, classKey) instanceof LDStrategy)) {
						data.setPollingSwitchoverDistribution(stationKey, classKey, new LDStrategy());
					}
					break;
				case SWITCHOVER_ZERO:
					// Puts a Zero Service Time Strategy only if previously it was different
					if (!(data.getPollingSwitchoverDistribution(stationKey, classKey) instanceof ZeroStrategy)) {
						data.setPollingSwitchoverDistribution(stationKey, classKey, new ZeroStrategy());
					}
					break;
				default:
					// Puts the default service strategy only if previously it was different
					if (!(data.getPollingSwitchoverDistribution(stationKey, classKey) instanceof Distribution)) {
						Object distribution = Defaults.getAsNewInstance("stationServiceStrategy");
						data.setPollingSwitchoverDistribution(stationKey, classKey, distribution);
					}
					break;
				}
			repaint();
			}
		}
	}

	private class DisabledButtonCellRenderer extends ButtonCellEditor {

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

	@Override
	public String getName() {
		return "Polling Server Properties Editor";
	}

	@Override
	public void repaint() {
		if (switchoverTable != null) {
			switchoverTable.tableChanged(new TableModelEvent(switchoverTable.getModel()));
		}
		super.repaint();
	}

	@Override
	public void gotFocus() {
		classEditor.clearCache();
	}

	@Override
	public void lostFocus() {
		// Aborts editing of table
		TableCellEditor editor = switchoverTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}

}
