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

package jmt.gui.exact.table;

import java.awt.Component;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * A CellRenderer displaying numbers and string with correct alignment and format
 */
public class ExactCellRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1L;

	protected static DecimalFormat df = new DecimalFormat();

	static {
		df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
		if (value instanceof Number) {
			super.setHorizontalAlignment(SwingConstants.RIGHT);
			if (value != null && (value.equals(Float.POSITIVE_INFINITY) || value.equals(Double.POSITIVE_INFINITY))) {
				value = "\u221e";
			}
		} else {
			super.setHorizontalAlignment(SwingConstants.CENTER);
		}
		return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
	}

	@Override
	public void setValue(Object o) {
		if (o instanceof Double) {
			setText(formatNumber(((Double) o).doubleValue()));
		} else {
			super.setValue(o);
		}
	}

	protected String formatNumber(double d) {
		if (d == 0.0 || (Math.abs(d) >= 1e-3 && Math.abs(d) < 1e7)) {
			df.applyPattern("0.0000");
			return df.format(d);
		} else {
			df.applyPattern("0.00E00");
			return df.format(d);
		}
	}

}
