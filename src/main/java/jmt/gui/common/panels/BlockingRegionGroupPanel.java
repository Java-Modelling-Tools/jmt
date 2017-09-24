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
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.definitions.BlockingRegionDefinition;
import jmt.gui.common.definitions.ClassDefinition;

import jmt.gui.common.editors.GrayCellRenderer;
import jmt.gui.common.editors.ImagedComboBoxCellEditorFactory;
import jmt.gui.exact.table.BooleanCellRenderer;
import jmt.gui.exact.table.ExactCellEditor;
import jmt.gui.jsimgraph.DialogFactory;

public class BlockingRegionGroupPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	protected BlockingGroupTable groupTable;
	protected BlockingRegionDefinition brd;
	protected ClassDefinition cd;
	protected Object key;
	protected JPanel groupPanel;
	protected JSpinner gNumber;
	protected DialogFactory dialogFactory;

	protected AbstractAction editMemberClasses = new AbstractAction("Edit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Edits Member Classes");
		}

		public void actionPerformed(ActionEvent e) {
			int index = groupTable.getSelectedRow();
			dialogFactory = new DialogFactory(getFrame(BlockingRegionGroupPanel.this.getParent()));
			if (index >= 0 && index < groupTable.getRowCount()) {
				dialogFactory.getDialog(new GroupEditor(cd, brd, key, index), "Editing " + brd.getRegionGroupName(key, index) + " Member Classes...");
			}
		}

	};

	protected AbstractAction deleteGroup = new AbstractAction("") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Delete Group");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage("Delete"));
		}

		public void actionPerformed(ActionEvent e) {
			int index = groupTable.getSelectedRow();
			if (index >= 0 && index < groupTable.getRowCount()) {
				deleteGroup(index);
			}
		}

	};

	public BlockingRegionGroupPanel(ClassDefinition cd, BlockingRegionDefinition brd, Object key, JSpinner gNumber) {
		this.gNumber = gNumber;
		setData(cd, brd, key);
		initComponents();
	}

	public void setData(ClassDefinition cd, BlockingRegionDefinition brd, Object key) {
		this.brd = brd;
		this.cd = cd;
		this.key = key;
	}

	private void initComponents() {
		setLayout(new BorderLayout(5, 5));
		groupPanel = new JPanel(new BorderLayout());
		groupTable = new BlockingGroupTable();
		groupPanel.add(new WarningScrollTable(groupTable, WARNING_CLASS));
		add(groupPanel, BorderLayout.CENTER);
	}

	@Override
	public String getName() {
		return "Group Specific";
	}

	@Override
	public void lostFocus() {
		TableCellEditor editor = groupTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}

	@Override
	public void gotFocus() {
	}

	protected void deleteGroup(int index) {
		brd.deleteRegionGroup(key, index);
		Refresh();
		gNumber.setValue(brd.getRegionGroupList(key).size());
	}

	public void Refresh() {
		groupTable.tableChanged(new TableModelEvent(groupTable.getModel()));
	}

	protected Frame getFrame(Container parent) {
		while (!(parent instanceof Frame)) {
			parent = parent.getParent();
		}
		return (Frame) parent;
	}

	protected class BlockingGroupTable extends JTable {

		private static final long serialVersionUID = 1L;

		protected TableCellRenderer grayRenderer = new GrayCellRenderer();
		int[] columnSizes = new int[] { 120, 60, 30, 60, 30, 60, 30 };

		protected JButton editButton = new JButton() {
			private static final long serialVersionUID = 1L;

			{
				setText("Edit");
			}
		};

		protected JButton deleteButton = new JButton() {
			private static final long serialVersionUID = 1L;

			{
				setAction(deleteGroup);
				setFocusable(false);
			}
		};

		public BlockingGroupTable() {
			setModel(new BlockingGroupTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setRowHeight(ROW_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 1) {
				return grayRenderer;
			} else if (column == 2) {
				return new BooleanCellRenderer();
			} else if (column == 3) {
				return grayRenderer;
			} else if (column == 4) {
				return new BooleanCellRenderer();
			} else if (column == 5) {
				return new ButtonCellEditor(editButton);
			} else if (column == 6) {
				return new DisabledButtonCellRenderer(deleteButton);
			} else {
				return super.getCellRenderer(row, column);
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 5) {
				return new ButtonCellEditor(new JButton(editMemberClasses));
			} else if (column == 6) {
				return new ButtonCellEditor(new JButton(deleteGroup));
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

	protected class BlockingGroupTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private Class<?>[] columnClasses = new Class[] { String.class, String.class, Boolean.class, String.class, Boolean.class,
				Object.class, Object.class };
		private String[] columnNames = new String[] { "Group", "Capacity", "\u221e", "Memory Size", "\u221e",
				"Member Classes", "" };

		public int getRowCount() {
			return brd.getRegionGroupList(key).size();
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
			return columnIndex == 0
					|| (columnIndex == 1 && !((Boolean) getValueAt(rowIndex, 2)).booleanValue()) || columnIndex == 2
					|| (columnIndex == 3 && !((Boolean) getValueAt(rowIndex, 4)).booleanValue()) || columnIndex == 4
					|| columnIndex == 5 || (columnIndex == 6 && brd.getRegionGroupList(key).size() > 1);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Integer ivalue = null;
			switch (columnIndex) {
			case 0:
				return brd.getRegionGroupName(key, rowIndex);
			case 1:
				ivalue = brd.getRegionGroupCustomerConstraint(key, rowIndex);
				return (ivalue.intValue() > 0) ? ivalue : "\u221e";
			case 2:
				ivalue = brd.getRegionGroupCustomerConstraint(key, rowIndex);
				return Boolean.valueOf(ivalue.intValue() < 1);
			case 3:
				ivalue = brd.getRegionGroupMemorySize(key, rowIndex);
				return (ivalue.intValue() > 0) ? ivalue : "\u221e";
			case 4:
				ivalue = brd.getRegionGroupMemorySize(key, rowIndex);
				return Boolean.valueOf(ivalue.intValue() < 1);
			}
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			String svalue = null;
			Integer ivalue = null;
			switch (columnIndex) {
			case 0:
				svalue = (String) aValue;
				if (!svalue.equals("")) {
					brd.setRegionGroupName(key, rowIndex, svalue);
				}
				break;
			case 1:
				try {
					ivalue = Integer.valueOf((String) aValue);
					if (ivalue.intValue() > 0) {
						brd.setRegionGroupCustomerConstraint(key, rowIndex, ivalue);
					}
				} catch (NumberFormatException e) {
					// Do nothing
				}
				break;
			case 2:
				if (((Boolean) aValue).booleanValue()) {
					brd.setRegionGroupCustomerConstraint(key, rowIndex, Integer.valueOf(-1));
				} else {
					ivalue = Defaults.getAsInteger("blockingGroupMaxJobs");
					if (ivalue.intValue() < 1) {
						ivalue = Integer.valueOf(1);
					}
					brd.setRegionGroupCustomerConstraint(key, rowIndex, ivalue);
				}
				break;
			case 3:
				try {
					ivalue = Integer.valueOf((String) aValue);
					if (ivalue.intValue() > 0) {
						brd.setRegionGroupMemorySize(key, rowIndex, ivalue);
					}
				} catch (NumberFormatException e) {
					// Do nothing
				}
				break;
			case 4:
				if (((Boolean) aValue).booleanValue()) {
					brd.setRegionGroupMemorySize(key, rowIndex, Integer.valueOf(-1));
				} else {
					ivalue = Defaults.getAsInteger("blockingGroupMaxMemory");
					if (ivalue.intValue() < 1) {
						ivalue = Integer.valueOf(1);
					}
					brd.setRegionGroupMemorySize(key, rowIndex, ivalue);
				}
				break;
			}
			repaint();
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

	protected class GroupEditor extends WizardPanel {

		private static final long serialVersionUID = 1L;

		protected BlockingRegionDefinition brd;
		protected ClassDefinition cd;
		protected Object key;
		protected int groupIndex;
		protected boolean[] classAvailableToGroup;
		protected ImagedComboBoxCellEditorFactory classEditor;

		protected JPanel dialogPanel;
		protected GroupEditTable table;

		public GroupEditor(ClassDefinition cd, BlockingRegionDefinition brd, Object key, int groupIndex) {
			setData(cd, brd, key, groupIndex);
			initComponents();
		}

		public void setData(ClassDefinition cd, BlockingRegionDefinition brd, Object key, int groupIndex) {
			this.brd = brd;
			this.cd = cd;
			this.key = key;
			this.groupIndex = groupIndex;
			classAvailableToGroup = new boolean[cd.getClassKeys().size()];
			for (int i = 0; i < cd.getClassKeys().size(); i++) {
				classAvailableToGroup[i] = true;
				for (int j = 0; j < brd.getRegionGroupList(key).size(); j++) {
					if (brd.getRegionGroupClassList(key, j).contains(cd.getClassKeys().get(i))) {
						if (j != groupIndex) {
							classAvailableToGroup[i] = false;
						}
						break;
					}
				}
			}
			classEditor = new ImagedComboBoxCellEditorFactory(cd);
		}

		protected void initComponents() {
			setLayout(new BorderLayout(10, 10));
			dialogPanel = new JPanel(new SpringLayout());
			dialogPanel.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(), "Select Member Classes"));
			dialogPanel.setLayout(new BorderLayout(1, 1));
			table = new GroupEditTable();
			dialogPanel.add(new WarningScrollTable(table, WARNING_CLASS));
			add(dialogPanel, BorderLayout.CENTER);
		}

		@Override
		public String getName() {
			return "Group Editor";
		}

		protected class GroupEditTable extends JTable {

			private static final long serialVersionUID = 1L;

			protected TableCellRenderer grayRenderer = new GrayBooleanCellRenderer();
			int[] columnSizes = new int[] { 320, 50 };

			public GroupEditTable() {
				setModel(new GroupEditTableModel());
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
					return grayRenderer;
				} else {
					return super.getCellRenderer(row, column);
				}
			}

			@Override
			public TableCellEditor getCellEditor(int row, int column) {
				return super.getCellEditor(row, column);
			}

			private void sizeColumns() {
				for (int i = 0; i < columnSizes.length && i < getColumnCount(); i++) {
					this.getColumnModel().getColumn(i).setPreferredWidth(columnSizes[i]);
				}
			}

		}

		protected class GroupEditTableModel extends AbstractTableModel {

			private static final long serialVersionUID = 1L;

			private Class<?>[] columnClasses = new Class[] { String.class,  Boolean.class };
			private String[] columnNames = new String[] { "Class", "Select", };

			public int getRowCount() {
				return cd.getClassKeys().size();
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
				return  columnIndex == 1 && classAvailableToGroup[rowIndex];
			}

			public Object getValueAt(int rowIndex, int columnIndex) {
				Object classKey = cd.getClassKeys().get(rowIndex);
				switch (columnIndex) {
				case 0:
					return classKey;
				case 1:
					return brd.getRegionGroupClassList(key, groupIndex).contains(classKey);
				}
				return null;
			}

			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				Object classKey = cd.getClassKeys().get(rowIndex);
				switch (columnIndex) {
				case 1:
					if (((Boolean) aValue).booleanValue()) {
						brd.addClassIntoRegionGroup(key, groupIndex, classKey);
					} else {
						brd.removeClassFromRegionGroup(key, groupIndex, classKey);
					}
					break;
				}
				repaint();
			}
		}

		protected class GrayBooleanCellRenderer extends JCheckBox implements TableCellRenderer {

			private static final long serialVersionUID = 1L;

			public GrayBooleanCellRenderer() {
				setLayout(new GridBagLayout());
				setMargin(new Insets(0, 0, 0, 0));
				setHorizontalAlignment(JLabel.CENTER);
			}

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if (value instanceof Boolean) {
					setSelected((Boolean) value);
				}
				if (isSelected) {
					setForeground(table.getSelectionForeground());
					setBackground(table.getSelectionBackground());
				} else {
					setForeground(table.getForeground());
					setBackground(table.getBackground());
				}
				if (classAvailableToGroup[row]) {
					setBackground(Color.WHITE);
				} else {
					setBackground(Color.LIGHT_GRAY);
				}
				return this;
			}

		}
	}	

}
