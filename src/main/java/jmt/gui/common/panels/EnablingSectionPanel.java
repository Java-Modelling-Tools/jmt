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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.exact.table.ExactCellEditor;
import jmt.gui.exact.table.ExactCellRenderer;
import jmt.gui.exact.table.ExactTable;
import jmt.gui.exact.table.ExactTableModel;

/**
 * <p>Title: Enabling Section Panel</p>
 * <p>Description: This panel is used to parametrise the enabling section.</p>
 *
 * @author Lulai Zhu
 * Date: 24-06-2016
 * Time: 16.00.00
 */
public class EnablingSectionPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	private static final String NO_INPUT_PLACES_ERROR =
			"Error: No input places are defined, but the number of servers is infinite. "
			+ "The transition will have an infinite enabling degree for this mode.";
	private static final String NO_INPUT_PLACES_WARNING =
			"Warning: No input places are defined, and the number of servers is finite. "
			+ "The transition will have a constant enabling degree for this mode.";
	private static final String NO_INPUT_CONDITION_ERROR =
			"Error: No enabling or inhibiting condition is specified, but the number of servers is infinite. "
			+ "The transition will have an infinite enabling degree for this mode.";
	private static final String NO_INPUT_CONDITION_WARNING =
			"Warning: No enabling or inhibiting condition is specified, and the number of servers is finite. "
			+ "The transition will have a constant enabling degree for this mode.";
	private static final String INVALID_INPUT_CONDITION_WARNING =
			"Warning: The enabling condition is invalidated by the inhibiting condition. "
			+ "The transition will never be enabled for this mode.";

	private boolean isInitComplete;

	private StationDefinition stationData;
	private ClassDefinition classData;
	private Object stationKey;
	private Vector<Object> stationInKeys;
	private Vector<Object> classKeys;
	private int currentModeIndex;

	private WarningScrollTable leftPanel;
	private TitledBorder enablingBorder;
	private ConditionTable enablingTable;
	private JScrollPane enablingPane;
	private TitledBorder inhibitingBorder;
	private ConditionTable inhibitingTable;
	private JScrollPane inhibitingPane;
	private JButton addModeButton;
	private ModeTable modeTable;
	private JTextArea noticeText;

	public EnablingSectionPanel(StationDefinition sd, ClassDefinition cd, Object sk) {
		isInitComplete = false;
		setData(sd, cd, sk);
		initComponents();
		addDataManagers();
		isInitComplete = true;
	}

	public void setData(StationDefinition sd, ClassDefinition cd, Object sk) {
		stationData = sd;
		classData = cd;
		stationKey = sk;
		stationInKeys = sd.getBackwardConnections(stationKey);
		classKeys = cd.getClassKeys();
		currentModeIndex = 0;
		if (isInitComplete) {
			leftPanel.clearCheckVectors();
			leftPanel.addCheckVector(stationInKeys);
			leftPanel.addCheckVector(classKeys);
			enablingTable.updateStructure();
			inhibitingTable.updateStructure();
		}
	}

	private void initComponents() {
		setLayout(new BorderLayout());

		String modeName = stationData.getTransitionModeName(stationKey, 0);
		enablingBorder = new TitledBorder(new EtchedBorder(), "Enabling Condition for " + modeName);
		enablingTable = new ConditionTable(true);
		enablingPane = new JScrollPane(enablingTable);
		enablingPane.setBorder(enablingBorder);
		enablingPane.setMinimumSize(new Dimension(300, 120));

		inhibitingBorder = new TitledBorder(new EtchedBorder(), "Inhibiting Condition for " + modeName);
		inhibitingTable = new ConditionTable(false);
		inhibitingPane = new JScrollPane(inhibitingTable);
		inhibitingPane.setBorder(inhibitingBorder);
		inhibitingPane.setMinimumSize(new Dimension(300, 120));

		JPanel conditionPanel = new JPanel(new GridLayout(2, 1, 3, 3));
		conditionPanel.add(enablingPane);
		conditionPanel.add(inhibitingPane);

		leftPanel = new WarningScrollTable(conditionPanel, WARNING_CLASS_INCOMING_ROUTING);
		leftPanel.addCheckVector(stationInKeys);
		leftPanel.addCheckVector(classKeys);

		addModeButton = new JButton("Add Mode");
		addModeButton.setMinimumSize(DIM_BUTTON_M);
		modeTable = new ModeTable();
		JPanel modePanel = new JPanel(new BorderLayout(5, 5));
		modePanel.setBorder(new TitledBorder(new EtchedBorder(), "Modes"));
		modePanel.setMinimumSize(new Dimension(200, 120));
		modePanel.add(addModeButton, BorderLayout.NORTH);
		modePanel.add(new JScrollPane(modeTable), BorderLayout.CENTER);

		noticeText = new JTextArea("");
		noticeText.setOpaque(false);
		noticeText.setEditable(false);
		noticeText.setLineWrap(true);
		noticeText.setWrapStyleWord(true);
		JScrollPane noticePane = new JScrollPane(noticeText);
		noticePane.setBorder(new TitledBorder(new EtchedBorder(), "Notice"));
		noticePane.setMinimumSize(new Dimension(200, 120));

		JSplitPane rightPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		rightPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		rightPanel.setDividerSize(4);
		rightPanel.setResizeWeight(1.0);
		rightPanel.setLeftComponent(modePanel);
		rightPanel.setRightComponent(noticePane);

		JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		mainPanel.setDividerSize(4);
		mainPanel.setResizeWeight(1.0);
		mainPanel.setLeftComponent(leftPanel);
		mainPanel.setRightComponent(rightPanel);

		add(mainPanel);
	}

	private void addDataManagers() {
		addModeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopAllCellEditing();
				int index = stationData.getTransitionModeListSize(stationKey);
				stationData.addTransitionMode(stationKey, Defaults.get("transitionModeName") + (index + 1));
				modeTable.tableChanged(new TableModelEvent(modeTable.getModel()));
			}
		});

		modeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int index = modeTable.getSelectedRow();
				if (index >= 0) {
					stopAllCellEditing();
					String modeName = stationData.getTransitionModeName(stationKey, index);
					enablingBorder.setTitle("Enabling Condition for " + modeName);
					inhibitingBorder.setTitle("Inhibiting Condition for " + modeName);
					currentModeIndex = index;
					enablingPane.repaint();
					inhibitingPane.repaint();
					updateNotice();
				} else {
					if (currentModeIndex >= stationData.getTransitionModeListSize(stationKey)) {
						currentModeIndex--;
					}
					modeTable.setRowSelectionInterval(currentModeIndex, currentModeIndex);
				}
			}
		});
	}

	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "Enabling Section";
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
		modeTable.tableChanged(new TableModelEvent(modeTable.getModel()));
		modeTable.setRowSelectionInterval(0, 0);
		updateNotice();
	}

	private void stopAllCellEditing() {
		TableCellEditor editor = null;
		editor = enablingTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
		editor = inhibitingTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
		editor = modeTable.getCellEditor();
		if (editor != null && editor != modeTable.deleteEditor) {
			editor.stopCellEditing();
		}
	}

	private void updateNotice() {
		if (classKeys.isEmpty()) {
			noticeText.setText("");
			noticeText.setForeground(Color.BLACK);
			return;
		}

		if (stationInKeys.isEmpty()) {
			int numberOfServers = stationData.getNumberOfServers(stationKey, currentModeIndex).intValue();
			if (numberOfServers < 1) {
				noticeText.setText(NO_INPUT_PLACES_ERROR);
				noticeText.setForeground(Color.RED);
			} else {
				noticeText.setText(NO_INPUT_PLACES_WARNING);
				noticeText.setForeground(Color.BLUE);
			}
		} else {
			boolean hasSpecifiedValues = false;
			boolean hasInvalidValues = false;
			for (Object stationInKey : stationInKeys) {
				for (Object classKey : classKeys) {
					int enablingValue = stationData.getEnablingCondition(stationKey, currentModeIndex,
							stationInKey, classKey).intValue();
					int inhibitingValue = stationData.getInhibitingCondition(stationKey, currentModeIndex,
							stationInKey, classKey).intValue();
					if (enablingValue > 0 || inhibitingValue > 0) {
						hasSpecifiedValues = true;
					}
					if (inhibitingValue > 0 && enablingValue >= inhibitingValue) {
						hasInvalidValues = true;
					}
				}
			}

			if (!hasSpecifiedValues) {
				int numberOfServers = stationData.getNumberOfServers(stationKey, currentModeIndex).intValue();
				if (numberOfServers < 1) {
					noticeText.setText(NO_INPUT_CONDITION_ERROR);
					noticeText.setForeground(Color.RED);
				} else {
					noticeText.setText(NO_INPUT_CONDITION_WARNING);
					noticeText.setForeground(Color.BLUE);
				}
			} else {
				if (hasInvalidValues) {
					noticeText.setText(INVALID_INPUT_CONDITION_WARNING);
					noticeText.setForeground(Color.BLUE);
				} else {
					noticeText.setText("");
					noticeText.setForeground(Color.BLACK);
				}
			}
		}
	}

	private class ConditionTable extends ExactTable {

		private static final long serialVersionUID = 1L;

		private boolean isEnablingTable;
		private InfiniteExactCellRenderer inhibitingRenderer;

		public ConditionTable(boolean isEnabling) {
			super(new ConditionTableModel(isEnabling));
			isEnablingTable = isEnabling;
			inhibitingRenderer = isEnabling ? null : new InfiniteExactCellRenderer();
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			setRowHeight(ROW_HEIGHT);
			setRowSelectionAllowed(true);
			setColumnSelectionAllowed(true);
			setBatchEditingEnabled(true);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (!isEnablingTable) {
				return inhibitingRenderer;
			} else {
				return super.getCellRenderer(row, column);
			}
		}

	}

	private class ConditionTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		private boolean isEnablingModel;

		public ConditionTableModel(boolean isEnabling) {
			rowHeaderPrototype = "Station10000";
			prototype = "Class10000";
			isEnablingModel = isEnabling;
		}

		@Override
		public int getRowCount() {
			return stationInKeys.size();
		}

		@Override
		public int getColumnCount() {
			return classKeys.size();
		}

		@Override
		protected String getRowName(int rowIndex) {
			return stationData.getStationName(stationInKeys.get(rowIndex));
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex >= classKeys.size()) {
				return "";
			}
			return classData.getClassName(classKeys.get(columnIndex));
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
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
			return true;
		}

		@Override
		protected Object getValueAtImpl(int rowIndex, int columnIndex) {
			if (isEnablingModel) {
				return stationData.getEnablingCondition(stationKey, currentModeIndex,
						stationInKeys.get(rowIndex), classKeys.get(columnIndex));
			} else {
				return stationData.getInhibitingCondition(stationKey, currentModeIndex,
						stationInKeys.get(rowIndex), classKeys.get(columnIndex));
			}
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			try {
				Integer ivalue = Integer.valueOf((String) value);
				if (isEnablingModel) {
					stationData.setEnablingCondition(stationKey, currentModeIndex,
							stationInKeys.get(rowIndex), classKeys.get(columnIndex), ivalue);
				} else {
					stationData.setInhibitingCondition(stationKey, currentModeIndex,
							stationInKeys.get(rowIndex), classKeys.get(columnIndex), ivalue);
				}
				updateNotice();
			} catch (NumberFormatException e) {
				// Aborts modification if String is invalid
			}
		}

		@Override
		public void clear(int row, int col) {
			if (isEnablingModel) {
				stationData.setEnablingCondition(stationKey, currentModeIndex,
						stationInKeys.get(row), classKeys.get(col), Integer.valueOf(0));
			} else {
				stationData.setInhibitingCondition(stationKey, currentModeIndex,
						stationInKeys.get(row), classKeys.get(col), Integer.valueOf(0));
			}
			updateNotice();
		}

	}

	private class InfiniteExactCellRenderer extends ExactCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (value != null && ((Integer) value).intValue() <= 0) {
				value = Double.POSITIVE_INFINITY;
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}

	}

	private class ModeTable extends JTable {

		private static final long serialVersionUID = 1L;

		private ButtonCellEditor deleteEditor;
		private ButtonCellEditor deleteRenderer;

		private AbstractAction deleteMode = new AbstractAction("") {

			private static final long serialVersionUID = 1L;

			{
				putValue(Action.SHORT_DESCRIPTION, "Delete");
				putValue(Action.SMALL_ICON, JMTImageLoader.loadImage("Delete"));
			}

			public void actionPerformed(ActionEvent e) {
				stopAllCellEditing();
				int index = modeTable.getSelectedRow();
				stationData.deleteTransitionMode(stationKey, index);
				modeTable.tableChanged(new TableModelEvent(modeTable.getModel()));
			}

		};

		public ModeTable() {
			JButton deleteButton = new JButton(deleteMode);
			deleteButton.setFocusable(false);
			deleteEditor = new ButtonCellEditor(deleteButton);
			deleteRenderer = new DisabledButtonCellRenderer(new JButton(deleteMode));
			setModel(new ModeTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setRowHeight(ROW_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setPreferredWidth(((ModeTableModel) getModel()).getColumnSize(i));
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 1) {
				return deleteEditor;
			} else {
				return super.getCellEditor(row, column);
			}
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 1) {
				return deleteRenderer;
			} else {
				return super.getCellRenderer(row, column);
			}
		}

	}

	private class ModeTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private String[] columnNames = new String[] { "Mode", "" };
		private Class<?>[] columnClasses = new Class[] { String.class, Object.class };
		private int[] columnSizes = new int[] { 160, 40 };

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
			if (columnIndex == 1 && stationData.getTransitionModeListSize(stationKey) < 2) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return stationData.getTransitionModeName(stationKey, rowIndex);
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				String modeName = (String) aValue;
				stationData.setTransitionModeName(stationKey, rowIndex, modeName);
				enablingBorder.setTitle("Enabling Condition for " + modeName);
				inhibitingBorder.setTitle("Inhibiting Condition for " + modeName);
				enablingPane.repaint();
				inhibitingPane.repaint();
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
