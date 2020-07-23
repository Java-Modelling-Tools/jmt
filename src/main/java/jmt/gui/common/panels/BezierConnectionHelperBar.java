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

import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.mainGui.JSIMGraphMain;

import javax.swing.*;
import java.awt.*;


public class BezierConnectionHelperBar extends JPanel{


	public BezierConnectionHelperBar(JSIMGraphMain mainWindow, Mediator mediator) {
		super();
		JLabel helperLabel0 = new JLabel(" HELP :");
		JLabel helperLabel1 = new JLabel("Press SHIFT and drag to draw tangents");
		JLabel helperLabel2 = new JLabel("Press ESC to cancel the arc");
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(helperLabel0);
		this.add(Box.createRigidArea(new Dimension(20,0)));
		this.add(helperLabel1);
		this.add(Box.createRigidArea(new Dimension(20,0)));
		this.add(helperLabel2);
		this.setVisible(false);
	}
}

