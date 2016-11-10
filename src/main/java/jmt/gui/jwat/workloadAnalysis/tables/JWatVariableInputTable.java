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

package jmt.gui.jwat.workloadAnalysis.tables;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.exact.table.ComboBoxCell;
import jmt.gui.jwat.JWATConstants;
import jmt.gui.jwat.workloadAnalysis.tables.listeners.RowDeleteListener;

public class JWatVariableInputTable extends JTable implements CommonConstants, JWATConstants {

	private static final long serialVersionUID = 1L;

	/* Set of column dimensions */
	protected int[] columnSizes = new int[] { 38, 100, 60, 145, 30, 100, 30, 30, 18 };

	public static final Object[] VarTypes = new Object[] { "Numeric", "String", "Date" };
	protected JComboBox combobox = new JComboBox(VarTypes);

	protected AbstractAction deleteVar = new AbstractAction("") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Delete");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage("Delete"));
		}

		public void actionPerformed(ActionEvent arg0) {
			((JWatVariableInputTableModel) JWatVariableInputTable.this.getModel()).deleteRow(JWatVariableInputTable.this.getSelectedRow());
			JWatVariableInputTable.this.tableChanged(new TableModelEvent(JWatVariableInputTable.this.getModel()));
			fireDeleteEvent();
		}

	};

	protected JButton delVar = new JButton() {
		private static final long serialVersionUID = 1L;

		{
			setAction(deleteVar);
			setFocusable(true);
		}
	};

	// Sets a table model for visualization and editing of data
	public void setModel(JWatVariableInputTableModel tabMod) {
		super.setModel(tabMod);
		sizeColumnsAndRows();
		setRowHeight(ROW_HEIGHT);
		// Arif Canakoglu: name column is not a combobox
		this.getColumnModel().getColumn(columnSizes.length - 1).setCellEditor(new ButtonCellEditor(new JButton(deleteVar)));
	}

	// set sizes for columns and rows of this table.
	protected void sizeColumnsAndRows() {
		for (int i = 0; i < columnSizes.length && i < getColumnCount(); i++) {
			this.getColumnModel().getColumn(i).setPreferredWidth(columnSizes[i]);
			if (i == 2) {
				this.getColumnModel().getColumn(i).setMaxWidth(columnSizes[i]);
			}
			if (i == 0) {
				this.getColumnModel().getColumn(i).setMaxWidth(columnSizes[i]);
			}
			if (i == columnSizes.length - 1) {
				// delete button and containing table cells as well, must be square
				this.getColumnModel().getColumn(i).setMaxWidth(columnSizes[i]);
				this.setRowHeight(columnSizes[i]);
			}
		}
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		if (column == 2) {
			return new ComboBoxCell(VarTypes);
		} else if (column == columnSizes.length - 1) {
			return new ButtonCellEditor(delVar);
		} else {
			return super.getCellRenderer(row, column);
		}
	}

	/* returns customized editor for table cells. */
	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		if (column == 2) {
			return new DefaultCellEditor(combobox);
		} else if (column == columnSizes.length - 1) {
			return new ButtonCellEditor(new JButton(deleteVar));
		} else {
			return super.getCellEditor(row, column);
		}
	}

	protected MouseHandler mouseHandler;
	private Vector<RowDeleteListener> deleteLisener = new Vector<RowDeleteListener>(); //<RowDeleteListener>

	public void addDeleteRowListener(RowDeleteListener r) {
		if (!deleteLisener.contains(r)) {
			deleteLisener.add(r);
		}
	}

	private void fireDeleteEvent() {
		for (int i = 0; i < deleteLisener.size(); i++) {
			deleteLisener.get(i).rowsDeletedEvent();
		}
	}

	public AbstractAction CLEAR_ACTION = new AbstractAction("Clear") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false));
			putValue(Action.SHORT_DESCRIPTION, "Clears selected cells");
		}

		public void actionPerformed(ActionEvent e) {
			clearCells();
		}

	};

	public AbstractAction DESEL_ACTION = new AbstractAction("Deselect all") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK, false));
			putValue(Action.SHORT_DESCRIPTION, "Deselected all variable in format table");
		}

		public void actionPerformed(ActionEvent e) {
			((JWatVariableInputTableModel) dataModel).deselectAll();
			tableChanged(new TableModelEvent(dataModel));
		}

	};

	private AbstractAction deleteClass = new AbstractAction("Delete selected varaible") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false));
			putValue(Action.SHORT_DESCRIPTION, "Deletes selected variable from the format");
		}

		public void actionPerformed(ActionEvent e) {
			clearCells();
		}

	};

	private AbstractAction deselectAll = new AbstractAction("Deselect all variables") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK, false));
			putValue(Action.SHORT_DESCRIPTION, "Deselected all variable in format table");
		}

		public void actionPerformed(ActionEvent e) {
			((JWatVariableInputTableModel) dataModel).deselectAll();
			tableChanged(new TableModelEvent(dataModel));
		}

	};

	protected void installKeyboard() {
		InputMap im = getInputMap();
		ActionMap am = getActionMap();
		installKeyboardAction(im, am, CLEAR_ACTION);
		installKeyboardAction(im, am, DESEL_ACTION);
	}

	protected void installKeyboardAction(Action a) {
		installKeyboardAction(getInputMap(), getActionMap(), a);
	}

	protected void installKeyboardAction(InputMap im, ActionMap am, Action a) {
		Object name = a.getValue(Action.NAME);
		KeyStroke key = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
		im.put(key, name);
		am.put(name, a);
	}

	private void clearCells() {
		int numrows = getSelectedRowCount();
		int[] rowsselected = getSelectedRows();
		for (int i = 0; i < numrows; i++) {
			((JWatVariableInputTableModel) dataModel).deleteRow(rowsselected[numrows - 1 - i]);
		}
		tableChanged(new TableModelEvent(dataModel));
		fireDeleteEvent();
	}

	public JWatVariableInputTable() {
		super();
		combobox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (JWatVariableInputTable.this.getSelectedRow() != -1) {
					switch (combobox.getSelectedIndex()) {
					case NUMERIC:
						JWatVariableInputTable.this.setValueAt("", JWatVariableInputTable.this.getSelectedRow(), 4);
						JWatVariableInputTable.this.setValueAt("([+-])?\\d+([.]\\d+)?", JWatVariableInputTable.this.getSelectedRow(), 5);
						break;
					case STRING:
						JWatVariableInputTable.this.setValueAt("\"", JWatVariableInputTable.this.getSelectedRow(), 4);
						JWatVariableInputTable.this.setValueAt("\\w+", JWatVariableInputTable.this.getSelectedRow(), 5);
						break;
					case DATE:
						JWatVariableInputTable.this.setValueAt("[]", JWatVariableInputTable.this.getSelectedRow(), 4);
						JWatVariableInputTable.this.setValueAt("\\d\\d/\\w\\w\\w/\\d\\d\\d\\d:\\d\\d:\\d\\d:\\d\\d[^\\]]+",
								JWatVariableInputTable.this.getSelectedRow(), 5);
						break;
					default:
					}
				}
			}
		});
		setSelectionBackground(new Color(83, 126, 126));
		setSelectionForeground(Color.BLACK);
		installKeyboard();
		installMouse();
	}

	protected JPopupMenu makeMouseMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(deleteClass);
		menu.add(new JSeparator());
		menu.add(deselectAll);
		return menu;
	}

	protected void installMouse() {
		mouseHandler = new MouseHandler(makeMouseMenu());
		mouseHandler.install();
	}

	private void controlSelection() {
		if (getSelectedRow() != -1) {
			CLEAR_ACTION.setEnabled(true);
			deleteClass.setEnabled(true);
		} else {
			CLEAR_ACTION.setEnabled(false);
			deleteClass.setEnabled(false);
		}
	}

	protected class MouseHandler extends MouseAdapter {

		private boolean isInstalled;
		private JPopupMenu menu;

		public MouseHandler(JPopupMenu menu) {
			this.menu = menu;
		}

		public void install() {
			if (!isInstalled) {
				addMouseListener(this);
				isInstalled = true;
			}
		}

		public void uninstall() {
			if (isInstalled) {
				removeMouseListener(this);
				isInstalled = false;
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			processME(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			processME(e);
		}

		protected void processME(MouseEvent e) {
			if (e.isPopupTrigger()) {
				controlSelection();
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		public JPopupMenu getMenu() {
			return menu;
		}

	}

}
