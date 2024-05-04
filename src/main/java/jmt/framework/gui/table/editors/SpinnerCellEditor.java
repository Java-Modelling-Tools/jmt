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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * <p>Title: Spinner Table CellViewer/CellEditor</p>
 * <p>Description: A component to display a Spinner as both a viewer and an editor in a table.
 * Internally caches already allocated editors to avoid creation of too many Spinners.
 * Renderer instance is a single element cached too.</p>
 *
 * @author Leran Chen
 * 			05/08/2021
 */
public class SpinnerCellEditor extends AbstractCellEditor implements TableCellEditor, CellEditor, TableCellRenderer {

	private static final long serialVersionUID = 1L;
	// Component used as editor
	protected JSpinner spinner;
	// component used as a renderer
	protected static SpinnerCellEditor renderer;
	private Integer maxvalue;

	/**
	 * Returns a new instance of CellEditor that will display number in a Spinner
	 * @param num number to be displayed from the Spinner
	 * @return CellEditor created
	 */
	public static SpinnerCellEditor getEditorInstance(Integer num) {
		return new SpinnerCellEditor(num, Integer.MAX_VALUE);
	}

	public static SpinnerCellEditor getEditorInstance(Integer num, Integer maxvalue) {
		return new SpinnerCellEditor(num,maxvalue);
	}

	/**
	 * Creates a new Spinner editor or renderer basing on given number
	 * @param num number to be shown on the Spinner
	 */
	protected SpinnerCellEditor(Integer num, Integer maxvalue){
		this.maxvalue = maxvalue;
		SpinnerModel spinnerModel = new SpinnerNumberModel(1, 1, maxvalue.intValue(), 1);
		spinner = new JSpinner(spinnerModel);
		if(num == null)
			num = new Integer(1);
		spinner.setValue(Math.min(num,maxvalue));
	}

	/**
	 * Returns a new instance of CellRenderer that will display current number in a Spinner
	 * @return cached CellRenderer
	 */
	public static SpinnerCellEditor getRendererInstance() {
		if (renderer == null) {
			renderer = new SpinnerCellEditor(new Integer(1),new Integer(1));
		}
		return renderer;
	}

	/**
	 * Returns the value contained in the editor.
	 *
	 * @return the value contained in the editor
	 */
	public Object getCellEditorValue() {
		return spinner.getValue();
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
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (spinner.getValue() instanceof Integer) {
					Integer num = (Integer) spinner.getValue();
					if (num.intValue() < 1 && spinner.isEnabled()) {
						num = new Integer(1);
						spinner.setValue(num);
					}
					if (num.intValue() > maxvalue.intValue() && spinner.isEnabled()) {
						num = maxvalue;
						spinner.setValue(num);
					}
				}
			}
		});
		return spinner;
	}

	public void addChangeListener(ChangeListener changeListener){
		spinner.addChangeListener(changeListener);
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

		if (value != null) {
			spinner.setValue(value);
		}else{
			spinner.setValue(1);
		}
		if (!isSelected) {
			spinner.setBackground(table.getBackground());
			spinner.setForeground(table.getForeground());
		} else {
			spinner.setBackground(table.getSelectionBackground());
			spinner.setForeground(table.getSelectionForeground());
		}
		return spinner;
	}
}
