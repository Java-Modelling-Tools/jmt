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

import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.editors.JobParallelismEditor;
import jmt.gui.common.editors.ServerTypesEditor;
import jmt.gui.common.editors.SwitchoverTimesEditor;
import jmt.gui.common.editors.DeadlineEditor;

/**
 * Tabbed panel for editing advanced server features
 * @author Lulai Zhu
 */
public class ServerFeaturePanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	private JTabbedPane mainPanel;
	private WizardPanel current;
	private SwitchoverTimesEditor switchoverTimesEditor;
	private DeadlineEditor deadlineEditor;

	private ServerTypesEditor serverTypesEditor;
	private DelayOffSectionPanel doPane;

	private JobParallelismEditor jobParallelismEditor;

	public ServerFeaturePanel() {
		this.setLayout(new BorderLayout(5, 5));
		mainPanel = new JTabbedPane();
		mainPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), ""));

		// Adds a change listener to perform gotFocus() and lostFocus() calls on wizardPanels
		mainPanel.addChangeListener(new ChangeListener() {
			/**
			 * Invoked when the target of the listener has changed its state.
			 *
			 * @param e a ChangeEvent object
			 */
			public void stateChanged(ChangeEvent e) {
				// Lose focus on old panel
				if (current != null) {
					current.lostFocus();
				}
				// gets focus on new panel
				if (mainPanel.getSelectedComponent() != null) {
					current = (WizardPanel) mainPanel.getSelectedComponent();
					current.gotFocus();
				}
			}
		});
		add(mainPanel, BorderLayout.CENTER);
	}

	public void setData(StationDefinition sd, ClassDefinition cd, Object stationKey) {
		deadlineEditor = new DeadlineEditor(sd, cd, stationKey);
		mainPanel.add(deadlineEditor, deadlineEditor.getName());
		if (sd.getStationType(stationKey).equals(STATION_TYPE_SERVER)) {
			switchoverTimesEditor = new SwitchoverTimesEditor(sd, cd, stationKey);
			mainPanel.add(switchoverTimesEditor, switchoverTimesEditor.getName());
		    doPane = new DelayOffSectionPanel(sd, cd, stationKey);
			mainPanel.add(doPane, doPane.getName());
			serverTypesEditor = new ServerTypesEditor(sd, cd, stationKey);
			mainPanel.add(serverTypesEditor, serverTypesEditor.getName());
			jobParallelismEditor = new JobParallelismEditor(sd, cd, stationKey);
			mainPanel.add(jobParallelismEditor, jobParallelismEditor.getName());
		}
		mainPanel.setSelectedComponent(deadlineEditor);
		current = deadlineEditor;
	}

	@Override
	public String getName() {
		return "Server Configuration";
	}

	@Override
	public void lostFocus() {
		if (current != null) {
			current.lostFocus();
		}
	}

}
