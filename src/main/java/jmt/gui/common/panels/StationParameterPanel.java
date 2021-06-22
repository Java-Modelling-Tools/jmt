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
import java.awt.Component;

import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 14-lug-2005
 * Time: 10.35.43
 * To change this template use Options | File Templates.
 */
public class StationParameterPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	public static final int INPUT_SECTION = 0;
	public static final int SERVICE_SECTION = 1;
	public static final int OUTPUT_SECTION = 2;

	protected StationDefinition stationData;
	protected ClassDefinition classData;
	protected Object stationKey;

	protected JTabbedPane mainPanel;

	protected InputSectionPanel isPane;
	protected JoinSectionPanel jsPane;
	protected SemaphoreSectionPanel smPane;
	protected StorageSectionPanel stPane;
	protected EnablingSectionPanel enPane;

	protected ServiceSectionPanel ssPane;
	protected ForkSectionPanel fsPane;
	protected LoggerSectionPanel lsPane;
	protected ClassSwitchSectionPanel csPane;
	protected TimingSectionPanel tiPane;

	protected RoutingSectionPanel rsPane;
	protected InGateWaySectionPanel gsPane;
	protected FiringSectionPanel fiPane;

	// Current wizard panel
	protected WizardPanel current;

	private TitledBorder title = new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "");

	public StationParameterPanel(StationDefinition sd, ClassDefinition cd, Object stationKey) {
		super();
		initComponents();
		setData(sd, cd, stationKey);
	}

	private void initComponents() {
		this.setLayout(new BorderLayout(5, 5));
		mainPanel = new JTabbedPane();
		mainPanel.setBorder(title);

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
		stationData = sd;
		classData = cd;
		setData(stationKey);
	}

	public void setData(Object stationKey) {
		this.stationKey = stationKey;
		String type = stationData.getStationType(stationKey);
		// Stores information on previously shown panel
		Component oldComponent = mainPanel.getSelectedComponent();

		mainPanel.removeAll();
		if (stationData.getStationName(stationKey) != null) {
			title.setTitle(stationData.getStationName(stationKey) + " Parameters Definiton");
		} else {
			title.setTitle("Parameters Definition");
		}

		if (hasInputSection(type)) {
			if (isPane == null) {
				isPane = new InputSectionPanel(stationData, classData, stationKey, type);
			} else {
				isPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(isPane, isPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == isPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasJoinSection(type)) {
			if (jsPane == null) {
				jsPane = new JoinSectionPanel(stationData, classData, stationKey);
			} else {
				jsPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(jsPane, jsPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == jsPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasSemaphoreSection(type)) {
			if (smPane == null) {
				smPane = new SemaphoreSectionPanel(stationData, classData, stationKey);
			} else {
				smPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(smPane, smPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == smPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasStorageSection(type)) {
			if (stPane == null) {
				stPane = new StorageSectionPanel(stationData, classData, stationKey);
			} else {
				stPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(stPane, stPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == stPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasEnablingSection(type)) {
			if (enPane == null) {
				enPane = new EnablingSectionPanel(stationData, classData, stationKey);
			} else {
				enPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(enPane, enPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == enPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		}

		if (hasServiceSection(type)) {
			if (ssPane == null) {
				ssPane = new ServiceSectionPanel(stationData, classData, stationKey);
			} else {
				ssPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(ssPane, ssPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == ssPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasForkSection(type)) {
			if (fsPane == null) {
				fsPane = new ForkSectionPanel(stationData, classData, stationKey);
			} else {
				fsPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(fsPane, fsPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == fsPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasLoggerSection(type)) {
			//if (lsPane == null) {
			lsPane = new LoggerSectionPanel(stationData, classData, stationKey);
			//} else {
			//lsPane.setData(stationData, classData, stationKey);
			//}
			mainPanel.add(lsPane, lsPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == lsPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasClassSwitchSection(type)) {
			if (csPane == null) {
				csPane = new ClassSwitchSectionPanel(stationData, classData, stationKey);
			} else {
				csPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(csPane, csPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == csPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasTimingSection(type)) {
			if (tiPane == null) {
				tiPane = new TimingSectionPanel(stationData, classData, stationKey);
			} else {
				tiPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(tiPane, tiPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == tiPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		}

		if (hasRoutingSection(type)) {
			if (rsPane == null) {
				rsPane = new RoutingSectionPanel(stationData, classData, stationKey);
			} else {
				rsPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(rsPane, rsPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == rsPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasInGateWaySection(type)) {
			if (gsPane == null) {
				gsPane = new InGateWaySectionPanel(stationData, classData, stationKey);
			} else {
				gsPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(gsPane, gsPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == gsPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		} else if (hasFiringSection(type)) {
			if (fiPane == null) {
				fiPane = new FiringSectionPanel(stationData, classData, stationKey);
			} else {
				fiPane.setData(stationData, classData, stationKey);
			}
			mainPanel.add(fiPane, fiPane.getName());

			// If this was previously selected, selects this
			if (oldComponent == fiPane) {
				mainPanel.setSelectedComponent(oldComponent);
			}
		}

		// Sets current panel
		current = (WizardPanel) mainPanel.getSelectedComponent();
	}

	private boolean hasInputSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_SERVER.equals(type) || STATION_TYPE_FORK.equals(type);
		}
	}

	private boolean hasJoinSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_JOIN.equals(type) || STATION_TYPE_SCALER.equals(type);
		}
	}

	private boolean hasSemaphoreSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_SEMAPHORE.equals(type);
		}
	}

	private boolean hasStorageSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_PLACE.equals(type);
		}
	}

	private boolean hasEnablingSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_TRANSITION.equals(type);
		}
	}

	private boolean hasServiceSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_DELAY.equals(type) || STATION_TYPE_SERVER.equals(type);
		}
	}

	private boolean hasForkSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_FORK.equals(type);
		}
	}

	private boolean hasLoggerSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_LOGGER.equals(type);
		}
	}

	private boolean hasClassSwitchSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_CLASSSWITCH.equals(type);
		}
	}

	private boolean hasTimingSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_TRANSITION.equals(type);
		}
	}

	private boolean hasRoutingSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_SOURCE.equals(type) ||  STATION_TYPE_TERMINAL.equals(type) || STATION_TYPE_ROUTER.equals(type)
					|| STATION_TYPE_DELAY.equals(type) || STATION_TYPE_SERVER.equals(type) || STATION_TYPE_JOIN.equals(type)
					|| STATION_TYPE_LOGGER.equals(type) || STATION_TYPE_CLASSSWITCH.equals(type) || STATION_TYPE_SEMAPHORE.equals(type);
		}
	}

	private boolean hasInGateWaySection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_FORK.equals(type) || STATION_TYPE_SCALER.equals(type);
		}
	}

	private boolean hasFiringSection(String type) {
		if (checkNullValues()) {
			return false;
		} else {
			return STATION_TYPE_TRANSITION.equals(type);
		}
	}

	private boolean checkNullValues() {
		return stationKey == null || stationData == null || classData == null;
	}

	// -------------------------------------------------------------------------
	// ----------------------  Francesco D'Aquino ------------------------------
	// -------------------------------------------------------------------------

	public void selectSectionPanel(int section, Object classKey) {
		String type = stationData.getStationType(stationKey);
		switch (section) {
		case INPUT_SECTION:
		{
			if (hasInputSection(type)) {
				mainPanel.setSelectedComponent(isPane);
			} else if (hasJoinSection(type)) {
				mainPanel.setSelectedComponent(jsPane);
				jsPane.setSelectedClass(classKey);
			} else if (hasSemaphoreSection(type)) {
				mainPanel.setSelectedComponent(smPane);
				smPane.setSelectedClass(classKey);
			} else if (hasStorageSection(type)) {
				mainPanel.setSelectedComponent(stPane);
			} else if (hasEnablingSection(type)) {
				mainPanel.setSelectedComponent(enPane);
			}
			break;
		}
		case SERVICE_SECTION:
		{
			if (hasServiceSection(type)) {
				mainPanel.setSelectedComponent(ssPane);
			} else if (hasForkSection(type)) {
				mainPanel.setSelectedComponent(fsPane);
			} else if (hasLoggerSection(type)) {
				mainPanel.setSelectedComponent(lsPane);
			} else if (hasClassSwitchSection(type)) {
				mainPanel.setSelectedComponent(csPane);
			} else if (hasTimingSection(type)) {
				mainPanel.setSelectedComponent(tiPane);
			}
			break;
		}
		case OUTPUT_SECTION:
		{
			if (hasRoutingSection(type)) {
				mainPanel.setSelectedComponent(rsPane);
				rsPane.setSelectedClass(classKey);
			} else if (hasInGateWaySection(type)) {
				mainPanel.setSelectedComponent(gsPane);
			} else if (hasFiringSection(type)) {
				mainPanel.setSelectedComponent(fiPane);
			}
			break;
		}
		}
	}

	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "Station Parameters";
	}

	/**
	 * called by the Wizard before when switching to another panel
	 */
	@Override
	public void lostFocus() {
		if (current != null) {
			current.lostFocus();
		}
	}

}
