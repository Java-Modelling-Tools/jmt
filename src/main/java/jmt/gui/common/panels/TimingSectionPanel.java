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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.table.editors.ComboBoxCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.editors.DistributionsEditor;
import jmt.gui.common.editors.GrayCellRenderer;
import jmt.gui.common.serviceStrategies.ZeroStrategy;
import jmt.gui.exact.table.BooleanCellRenderer;
import jmt.gui.exact.table.ExactCellEditor;
import jmt.gui.exact.table.ExactCellRenderer;

/**
 * <p>Title: Timing Section Panel</p>
 * <p>Description: This panel is used to parametrise the timing section.</p>
 *
 * @author Lulai Zhu
 * Date: 24-06-2016
 * Time: 16.00.00
 */
public class TimingSectionPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	private static final String[] timingStrategies = { TIMING_STRATEGY_TIMED, TIMING_STRATEGY_IMMEDIATE };

	private StationDefinition stationData;
	//private ClassDefinition classData;
	private Object stationKey;

	private JButton addModeButton;
	private TimingOptionTable optionTable;

	public TimingSectionPanel(StationDefinition sd, ClassDefinition cd, Object sk) {
		setData(sd, cd, sk);
		initComponents();
		addDataManagers();
	}

	public void setData(StationDefinition sd, ClassDefinition cd, Object sk) {
		stationData = sd;
		//classData = cd;
		stationKey = sk;
	}

	private void initComponents() {
		setLayout(new BorderLayout(3, 3));
		setBorder(new EmptyBorder(5, 5, 5, 5));

		addModeButton = new JButton("Add Mode");
		addModeButton.setMinimumSize(DIM_BUTTON_M);

		optionTable = new TimingOptionTable();
		JScrollPane OptionPane = new JScrollPane(optionTable);
		OptionPane.setBorder(new TitledBorder(new EtchedBorder(), "Timing Options"));

		add(addModeButton, BorderLayout.NORTH);
		add(OptionPane, BorderLayout.CENTER);
	}

	private void addDataManagers() {
		addModeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopAllCellEditing();
				int index = stationData.getTransitionModeListSize(stationKey);
				stationData.addTransitionMode(stationKey, Defaults.get("transitionModeName") + (index + 1));
				optionTable.tableChanged(new TableModelEvent(optionTable.getModel()));
			}
		});
	}

	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "Timing Section";
	}

	/**
	 * called by the Wizard before when switching to another panel
	 */
	@Override
	public void lostFocus() {
		stopAllCellEditing();
	}

	/**
	 * called by the Wizard when the panel becomes active
	 */
	@Override
	public void gotFocus() {
		optionTable.tableChanged(new TableModelEvent(optionTable.getModel()));
	}

	private void stopAllCellEditing() {
		TableCellEditor editor = null;
		editor = optionTable.getCellEditor();
		if (editor != null && editor != optionTable.deleteEditor) {
			editor.stopCellEditing();
		}
	}

	private class TimingOptionTable extends JTable {

		private static final long serialVersionUID = 1L;

		private ExactCellRenderer numberRenderer;
		private ButtonCellEditor distributionEditor;
		private ButtonCellEditor distributionRenderer;
		private ExactCellRenderer priorityRenderer;
		private ExactCellRenderer weightRenderer;
		private ButtonCellEditor deleteEditor;
		private ButtonCellEditor deleteRenderer;

		private AbstractAction editDistribution = new AbstractAction("Edit") {

			private static final long serialVersionUID = 1L;

			{
				putValue(Action.SHORT_DESCRIPTION, "Edits Firing Time Distribution");
			}

			public void actionPerformed(ActionEvent e) {
				int index = optionTable.getSelectedRow();
				Distribution distrbution = (Distribution) stationData.getFiringTimeDistribution(stationKey, index);
				DistributionsEditor editor = DistributionsEditor.getInstance(TimingSectionPanel.this.getParent(), distrbution);
				editor.setTitle("Editing " + stationData.getTransitionModeName(stationKey, index) + " Firing Time Distribution...");
				editor.show();
				stationData.setFiringTimeDistribution(stationKey, index, editor.getResult());
				optionTable.repaint();
			}

		};

		private AbstractAction deleteMode = new AbstractAction("") {

			private static final long serialVersionUID = 1L;

			{
				putValue(Action.SHORT_DESCRIPTION, "Delete");
				putValue(Action.SMALL_ICON, JMTImageLoader.loadImage("Delete"));
			}

			public void actionPerformed(ActionEvent e) {
				stopAllCellEditing();
				int index = optionTable.getSelectedRow();
				stationData.deleteTransitionMode(stationKey, index);
				optionTable.tableChanged(new TableModelEvent(optionTable.getModel()));
			}

		};

		public TimingOptionTable() {
			numberRenderer = new GrayCellRenderer();
			distributionEditor = new ButtonCellEditor(new JButton(editDistribution));
			distributionRenderer = new DisabledButtonCellRenderer(new JButton(editDistribution));
			priorityRenderer = new GrayCellRenderer();
			weightRenderer = new GrayCellRenderer();
			JButton deleteButton = new JButton(deleteMode);
			deleteButton.setFocusable(false);
			deleteEditor = new ButtonCellEditor(deleteButton);
			deleteRenderer = new DisabledButtonCellRenderer(new JButton(deleteMode));
			setModel(new TimingOptionTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setRowHeight(ROW_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setPreferredWidth(((TimingOptionTableModel) getModel()).getColumnSize(i));
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 3) {
				return ComboBoxCellEditor.getEditorInstance(timingStrategies);
			} else if (column == 5) {
				return distributionEditor;
			} else if (column == 8) {
				return deleteEditor;
			} else {
				return super.getCellEditor(row, column);
			}
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 1) {
				return numberRenderer;
			} else if (column == 2) {
				return new BooleanCellRenderer();
			} else if (column == 3) {
				return ComboBoxCellEditor.getRendererInstance();
			} else if (column == 5) {
				return distributionRenderer;
			} else if (column == 6) {
				return priorityRenderer;
			} else if (column == 7) {
				return weightRenderer;
			} else if (column == 8) {
				return deleteRenderer;
			} else {
				return super.getCellRenderer(row, column);
			}
		}

	}

	private class TimingOptionTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private String[] columnNames = new String[] { "Mode", "Number of Servers", "\u221e", "Timing Strategy",
				"Firing Time Distribution", "", "Firing Priority", "Firing Weight", ""};
		private Class<?>[] columnClasses = new Class[] { String.class, String.class, Boolean.class, String.class,
				String.class, Object.class, String.class, String.class, Object.class};
		private int[] columnSizes = new int[] { 80, 100, 25, 90, 120, 25, 70, 70, 25 };

		@Override
		public int getRowCount() {
			return stationData.getTransitionModeListSize(stationKey);
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
			if (columnIndex == 1 && stationData.getNumberOfServers(stationKey, rowIndex).intValue() < 1) {
				return false;
			} else if (columnIndex == 4) {
				return false;
			} else if (columnIndex == 5
					&& stationData.getFiringTimeDistribution(stationKey, rowIndex) instanceof ZeroStrategy) {
				return false;
			} else if ((columnIndex == 6 || columnIndex == 7)
					&& stationData.getFiringTimeDistribution(stationKey, rowIndex) instanceof Distribution) {
				return false;
			} else if (columnIndex == 8 && stationData.getTransitionModeListSize(stationKey) < 2) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return stationData.getTransitionModeName(stationKey, rowIndex);
			} else if (columnIndex == 1) {
				Integer number = stationData.getNumberOfServers(stationKey, rowIndex);
				return (number.intValue() > 0) ? number : "\u221e";
			} else if (columnIndex == 2) {
				Integer number = stationData.getNumberOfServers(stationKey, rowIndex);
				return Boolean.valueOf(number.intValue() < 1);
			} else if (columnIndex == 3) {
				if (stationData.getFiringTimeDistribution(stationKey, rowIndex) instanceof Distribution) {
					return TIMING_STRATEGY_TIMED;
				} else {
					return TIMING_STRATEGY_IMMEDIATE;
				}
			} else if (columnIndex == 4) {
				return stationData.getFiringTimeDistribution(stationKey, rowIndex);
			} else if (columnIndex == 6) {
				if (stationData.getFiringTimeDistribution(stationKey, rowIndex) instanceof Distribution) {
					return "--";
				} else {
					return stationData.getFiringPriority(stationKey, rowIndex);
				}
			} else if (columnIndex == 7) {
				if (stationData.getFiringTimeDistribution(stationKey, rowIndex) instanceof Distribution) {
					return "--";
				} else {
					return stationData.getFiringWeight(stationKey, rowIndex);
				}
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				stationData.setTransitionModeName(stationKey, rowIndex, (String) aValue);
			} else if (columnIndex == 1) {
				try {
					Integer number = Integer.valueOf((String) aValue);
					if (number.intValue() < 1) {
						number = Integer.valueOf(1);
					}
					stationData.setNumberOfServers(stationKey, rowIndex, number);
				} catch (NumberFormatException e) {
					// Aborts modification if String is invalid
				}
			} else if (columnIndex == 2) {
				if (((Boolean) aValue).booleanValue()) {
					stationData.setNumberOfServers(stationKey, rowIndex, Integer.valueOf(-1));
				} else {
					Integer number = Defaults.getAsInteger("transitionNumberOfServers");
					if (number.intValue() < 1) {
						number = Integer.valueOf(1);
					}
					stationData.setNumberOfServers(stationKey, rowIndex, number);
				}
				repaint();
			} else if (columnIndex == 3) {
				if (((String) aValue).equals(TIMING_STRATEGY_TIMED)) {
					if (!(stationData.getFiringTimeDistribution(stationKey, rowIndex) instanceof Distribution)) {
						Object distribution = Defaults.getAsNewInstance("transitionFiringTimeDistribution");
						stationData.setFiringTimeDistribution(stationKey, rowIndex, distribution);
						stationData.setFiringPriority(stationKey, rowIndex, Integer.valueOf(-1));
						stationData.setFiringWeight(stationKey, rowIndex, Double.valueOf(1.0));
					}
				} else {
					if (!(stationData.getFiringTimeDistribution(stationKey, rowIndex) instanceof ZeroStrategy)) {
						Integer prioirty = Defaults.getAsInteger("transitionFiringPriority");
						Double weight = Defaults.getAsDouble("transitionFiringWeight");
						stationData.setFiringTimeDistribution(stationKey, rowIndex, new ZeroStrategy());
						stationData.setFiringPriority(stationKey, rowIndex, prioirty);
						stationData.setFiringWeight(stationKey, rowIndex, weight);
					}
				}
				repaint();
			} else if (columnIndex == 6) {
				try {
					Integer prioirty = Integer.valueOf((String) aValue);
					if (prioirty.intValue() < 0) {
						prioirty = Integer.valueOf(0);
					}
					stationData.setFiringPriority(stationKey, rowIndex, prioirty);
				} catch (NumberFormatException e) {
					// Aborts modification if String is invalid
				}
			} else if (columnIndex == 7) {
				try {
					Double weight = Double.valueOf((String) aValue);
					if (weight.doubleValue() <= 0.0) {
						weight = Double.valueOf(1.0);
					}
					stationData.setFiringWeight(stationKey, rowIndex, weight);
				} catch (NumberFormatException e) {
					// Aborts modification if String is invalid
				}
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

}
