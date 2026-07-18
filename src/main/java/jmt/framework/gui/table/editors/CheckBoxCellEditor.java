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

package jmt.framework.gui.table.editors;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractCellEditor;
import javax.swing.CellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * <p>Title: CheckBox Table CellViewer/CellEditor</p>
 * <p>Description: A component to display a CheckBox as both a viewer and an editor in a table.
 * Internally caches already allocated editors to avoid creation of too many CheckBoxes.
 * Renderer instance is a single element cached too.</p>
 */
public class CheckBoxCellEditor extends AbstractCellEditor implements TableCellEditor, CellEditor, TableCellRenderer {

	private static final long serialVersionUID = 1L;
	// Uses to cache equivalent editors
	protected static CheckBoxCellEditor editorCache;
	// Component used as editor
	protected JCheckBox checkBox;
	// component used as a renderer
	protected static CheckBoxCellEditor renderer;

	/**
	 * Returns a new instance of CellEditor that will a CheckBox
	 * @return CellEditor created or cached copy
	 */
	public static CheckBoxCellEditor getEditorInstance() {
		if (editorCache == null) {
			editorCache = new CheckBoxCellEditor();
		}
		return editorCache;
	}

	/**
	 * Returns a new instance of CellRenderer that will display current item in a CheckBox
	 * @return cached CellRenderer
	 */
	public static CheckBoxCellEditor getRendererInstance() {
		if (renderer == null) {
			renderer = new CheckBoxCellEditor();
		}
		return renderer;
	}

	/**
	 * Creates a new CheckBox editor/renderer
	 */
	protected CheckBoxCellEditor() {
		checkBox = new JCheckBox();
	}

	/**
	 * Returns the value contained in the editor
	 * @return the value contained in the editor
	 */
	public Object getCellEditorValue() {
		return checkBox.isSelected();
	}

	/**
	 * Sets an initial <code>value</code> for the editor.  This will cause
	 * the editor to <code>stopEditing</code> and lose any partially
	 * edited value if the editor is editing when this method is called. <p>
	 * <p/>
	 * Returns the component that should be added to the client's
	 * <code>Component</code> hierarchy.  Once installed in the client's
	 * hierarchy this component will then be able to draw and receive
	 * user input.
	 *
	 * @param    table        the <code>JTable</code> that is asking the
	 * editor to edit; can be <code>null</code>
	 * @param    value        the value of the cell to be edited; it is
	 * up to the specific editor to interpret
	 * and draw the value.  For example, if value is
	 * the string "true", it could be rendered as a
	 * string or it could be rendered as a check
	 * box that is checked.  <code>null</code>
	 * is a valid value
	 * @param    isSelected    true if the cell is to be rendered with
	 * highlighting
	 * @param    row the row of the cell being edited
	 * @param    column the column of the cell being edited
	 * @return the component for editing
	 */
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (value instanceof Boolean) {
			checkBox.setSelected((Boolean) value);
		}
		checkBox.addItemListener(new ItemListener() {
			/**
			 * Invoked when an item has been selected or deselected by the user.
			 * Makes the renderer to reappear after editing finishes
			 */
			public void itemStateChanged(ItemEvent e) {
				//Make the renderer reappear.
				fireEditingStopped();
			}
		});
		return checkBox;
	}

	/**
	 * Returns the component used for drawing the cell.  This method is
	 * used to configure the renderer appropriately before drawing.
	 *
	 * @param    table        the <code>JTable</code> that is asking the
	 * renderer to draw; can be <code>null</code>
	 * @param    value        the value of the cell to be rendered.  It is
	 * up to the specific renderer to interpret
	 * and draw the value.  For example, if
	 * <code>value</code>
	 * is the string "true", it could be rendered as a
	 * string or it could be rendered as a check
	 * box that is checked.  <code>null</code> is a
	 * valid value
	 * @param    isSelected    true if the cell is to be rendered with the
	 * selection highlighted; false otherwise
	 * @param    hasFocus    if true, render cell appropriately.  For
	 * example, put a special border on the cell, if
	 * the cell can be edited, render in the color used
	 * to indicate editing
	 * @param    row     the row index of the cell being drawn.  When
	 * drawing the header, the value of
	 * <code>row</code> is -1
	 * @param    column     the column index of the cell being drawn
	 */
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		// If cell is not editable, returns standard renderer
		if (!table.isCellEditable(row, column)) {
			return table.getDefaultRenderer(value.getClass()).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		if (value instanceof Boolean) {
			checkBox.setSelected((Boolean) value);
		}
		checkBox.setBackground(table.getBackground());
		checkBox.setForeground(table.getForeground());
		return checkBox;
	}

}
