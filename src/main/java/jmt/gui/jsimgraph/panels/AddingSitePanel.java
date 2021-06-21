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

package jmt.gui.jsimgraph.panels;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * @author S Jiang
 * 
 */
public class AddingSitePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private JTextField name;
	private JTextField address;
	
	public AddingSitePanel() {
		this.setLayout(new GridLayout(0,1));
		
		JLabel siteName = new JLabel("Site Name: ");
		JLabel siteAddress = new JLabel("Site Address: ");
		
		name = new JTextField();
		address = new JTextField("http://");
		
		siteName.setLabelFor(name);
		siteAddress.setLabelFor(address);
		
		this.add(siteName);
		this.add(name);
		this.add(siteAddress);
		this.add(address);
	}
	
	public String getName() {
		return name.getText();
	}
	
	public String getAddress() {
		return address.getText();
	}
	
	public String getPanelName() {
		return "New Site";
	}
}
