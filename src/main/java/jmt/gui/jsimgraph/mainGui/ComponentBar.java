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

package jmt.gui.jsimgraph.mainGui;

import java.util.Iterator;
import java.util.Set;

import jmt.framework.gui.components.JMTToolBar;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.jsimgraph.controller.Mediator;
import jmt.gui.jsimgraph.controller.actions.SetInsertState;

/**
 * <p>Title: Component Toolbar</p>
 * <p>Description: A toolbar used to insert new stations in the graph, or to go
 * into "select mode" or "link mode".
 * 
 * @author Bertoli Marco
 *         Date: 4-giu-2005
 *         Time: 14.13.55
 * 
 * Modified by Giuseppe De Cicco & Fabio Granara
 */
public class ComponentBar extends JMTToolBar {

	private static final long serialVersionUID = 1L;

	public ComponentBar(Mediator m) {
		super(JMTImageLoader.getImageLoader());
		// Uncomment to set vertical orientation for the component bar in jSIMGraph
		// setOrientation(JToolBar.VERTICAL);
		// Adds Select button
		addGenericButton(m.getSetSelect());
		addSeparator();
		// Adds insert mode buttons
		String[] stations = getStationList();
		for (String station : stations) {
			addGenericButton(new SetInsertState(m, station));
		}
		addSeparator();
		// Adds link button
		addGenericButton(m.getSetConnect());
		// Adds Bezier link button
		addGenericButton(m.getSetBezierConnect());
		// Adds Blocking region button
		addGenericButton(m.getAddBlockingRegion());

		addSeparator();
		// Adds Rotate button 
		addGenericButton(m.getRotate());
		// Adds RotateLeft button
		addGenericButton(m.getRotateLeft());
		// Adds RotateRight button
		addGenericButton(m.getRotateRight());
		// Adds SetRight button
		addGenericButton(m.getSetRight());

		addSeparator();
		//Adds Grid Button
		addGenericButton(m.getSnapToGrid());

		// Disables all components button
		enableButtons(false);
	}

	private String[] getStationList() {
		// Build *Cell array to be given as output
		Set<String> typeSet = CommonConstants.STATION_NAMES.keySet();
		String[] classNames = new String[typeSet.size()];
		Iterator<String> it = typeSet.iterator();
		int i = 0;
		while (it.hasNext()) {
			classNames[i] = it.next() + "Cell";
			i++;
		}
		return classNames;
	}

}
