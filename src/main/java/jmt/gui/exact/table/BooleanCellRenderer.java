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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalCheckBoxIcon;
import javax.swing.table.TableCellRenderer;

public class BooleanCellRenderer extends JCheckBox implements TableCellRenderer {

	private static final long serialVersionUID = 1L;
	private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	public BooleanCellRenderer() {
		setLayout(new GridBagLayout());
		setMargin(new Insets(0, 0, 0, 0));
		setHorizontalAlignment(JLabel.CENTER);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (value instanceof Boolean) {
			setSelected((Boolean) value);
		}
		if (!isSelected) {
			setBackground(table.getBackground());
			setForeground(table.getForeground());
		} else {
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
			setIcon(new MyCheckIcon((Boolean) value));
		}
		if (hasFocus) {
			setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		} else {
			setBorder(noFocusBorder);
		}
		return this;
	}

	class MyCheckIcon extends MetalCheckBoxIcon {

		private static final long serialVersionUID = 1L;
		private Boolean isSelected;

		MyCheckIcon(Boolean isSelected) {
			this.isSelected = isSelected;
		}

		protected void drawCheck(Component c, Graphics g, int x, int y) {
			Color old = g.getColor();
			// modify this colour to get differently coloured ticks
			g.setColor(new Color(250, 250, 250));

			int controlSize = getControlSize();
			g.fillRect(x + 3, y + 5, 2, controlSize - 8);
			g.drawLine(x + (controlSize - 4), y + 3, x + 5, y
					+ (controlSize - 6));
			g.drawLine(x + (controlSize - 4), y + 4, x + 5, y
					+ (controlSize - 5));
			g.setColor(old);
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			if (!isSelected) {
				Color old = g.getColor();
				// modify this colour to get differently coloured ticks
				g.setColor(new Color(250, 250, 250));

				int controlSize = getControlSize();
				g.fillRect(x, y, controlSize, controlSize);
				g.setColor(old);
			}
			super.paintIcon(c, g, x, y);
		}

	}

}
