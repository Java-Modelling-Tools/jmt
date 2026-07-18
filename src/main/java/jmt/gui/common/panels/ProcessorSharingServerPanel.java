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

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.StationDefinition;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Menu for editing processor sharing server properties
 * @author Andrei Roman
 */
public class ProcessorSharingServerPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	// Dialog box border dimensions
	public static final int BORDER_TOP = 3;
	public static final int BORDER_BOTTOM = 3;
	public static final int BORDER_LEFT = 3;
	public static final int BORDER_RIGHT = 3;
	public static final int BORDER_HGAP = 5;
	public static final int BORDER_VGAP = 5;

	// Processor Sharing Data
	private final StationDefinition stationData;
	private final Object stationKey;

	// Front-End components
	private JPanel processorLimitPanel;
	private JSpinner processorLimitSpinner;
	private JRadioButton processorLimitInfiniteSelector;
	private JRadioButton processorLimitFiniteSelector;
	private ButtonGroup processorLimitSelectors;
	private JPanel processorLimitWarningPanel;
	private JPanel quantumSizePanel;
	private JSpinner quantumSizeSpinner;

	private JSpinner switchoverTimeSpinner;

	public ProcessorSharingServerPanel(StationDefinition stationData, Object stationKey) {
		this.stationData = stationData;
		this.stationKey = stationKey;
		initGuiComponents();
		registerPanelListeners();
		updateMaxRunningJobs();
	}

	/**
	 * Wrapper of all front-end components initializers
	 */
	private void initGuiComponents() {
		initDialogBox();
		initProcessorLimitPanel();
		initProcessorLimitSelectors();
		initProcessorLimitSpinner();
		initProcessorLimitWarningPanel();
		initQuantaSizePanel();

		// Add the processor limit panel at the top
		this.add(processorLimitPanel, BorderLayout.NORTH);
		this.add(quantumSizePanel, BorderLayout.CENTER);
	}

	/**
	 * Initialises the dialog box dimensions
	 */
	private void initDialogBox() {
		this.setBorder(new EmptyBorder(BORDER_TOP, BORDER_LEFT, BORDER_BOTTOM, BORDER_RIGHT));
		this.setLayout(new BorderLayout(BORDER_HGAP, BORDER_VGAP));
	}

	/**
	 * Initialises the Processor Limit Panel
	 */
	private void initProcessorLimitPanel() {
		processorLimitPanel = new JPanel();
		processorLimitPanel.setLayout(new BoxLayout(processorLimitPanel, BoxLayout.Y_AXIS));
		processorLimitPanel.setBorder(new TitledBorder(new EtchedBorder(), "Processor Sharing Limit"));
	}

	/**
	 * Initialises the Processor Limit Selectors sub-panel
	 */
	private void initProcessorLimitSelectors() {
		JPanel processorLimitSelectorsPanel = new JPanel();

		processorLimitSelectors = new ButtonGroup();
		processorLimitInfiniteSelector = new JRadioButton("<html> Infinite <br> </html>");
		processorLimitFiniteSelector = new JRadioButton("<html> Finite <br> </html>");
		processorLimitSelectors.add(processorLimitInfiniteSelector);
		processorLimitSelectors.add(processorLimitFiniteSelector);

		processorLimitSelectorsPanel.add(processorLimitInfiniteSelector);
		processorLimitSelectorsPanel.add(processorLimitFiniteSelector);

		processorLimitPanel.add(processorLimitSelectorsPanel);
	}

	/**
	 * Initialises the Processor Limit Spinner sub-panel
	 */
	private void initProcessorLimitSpinner() {
		JPanel processorSpinnerPanel = new JPanel();

		processorLimitSpinner = new JSpinner();
		processorLimitSpinner.setPreferredSize(DIM_BUTTON_XS);

		JLabel processorSpinnerLabel = new JLabel("<html> <b>Maximum</b> number of jobs that can share" +
				" the servers at any time: &nbsp&nbsp</html>");
		processorSpinnerPanel.add(processorSpinnerLabel);
		processorSpinnerPanel.add(processorLimitSpinner);

		processorLimitPanel.add(processorSpinnerPanel);
	}

	/**
	 * Initialises the Warning Panel
	 * Warning appears at the bottom of the Processor Limit Panel if the processor sharing limit is
	 * less than the number of servers at the station
	 */
	private void initProcessorLimitWarningPanel() {
		this.processorLimitWarningPanel = new JPanel();

		JLabel processorLimitWarning = new JLabel("<html> <p style=\"color: red;\"><b>Warning!</b> " +
				"The processor sharing limit is smaller than the " +
				"number of servers at this station </p></html>");
		processorLimitWarningPanel.add(processorLimitWarning);
		processorLimitPanel.add(Box.createVerticalStrut(10));

		Integer spinnerValue = (Integer) processorLimitSpinner.getValue();
		boolean isVisibleInitially = spinnerValue != -1 && spinnerValue < stationData.getStationNumberOfServers(stationKey);
		processorLimitWarningPanel.setVisible(isVisibleInitially);

		processorLimitPanel.add(processorLimitWarningPanel);
	}

	/**
	 * Initialises the Quanta Size Panel
	 */
	private void initQuantaSizePanel() {
		quantumSizePanel = new JPanel();
		quantumSizePanel.setLayout(new BoxLayout(quantumSizePanel, BoxLayout.X_AXIS));
		quantumSizePanel.setBorder(new TitledBorder(new EtchedBorder(), "Quantum-Based Processor Sharing (QBPS)"));

		JPanel quantumSpinnerPanel = new JPanel();
		quantumSizeSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, Double.MAX_VALUE, 0.1));
		quantumSizeSpinner.setPreferredSize(DIM_BUTTON_XS);

		JLabel quantaSpinnerLabel = new JLabel("<html> <b>Quantum size</b>:</html>");
		quantumSpinnerPanel.add(quantaSpinnerLabel);
		quantumSpinnerPanel.add(quantumSizeSpinner);

		quantumSizePanel.add(quantumSpinnerPanel);

		// Switchover time spinner panel
		JPanel switchoverSpinnerPanel = new JPanel();
		switchoverTimeSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, Double.MAX_VALUE, 0.01));
		switchoverTimeSpinner.setPreferredSize(DIM_BUTTON_XS);

		JLabel switchoverSpinnerLabel = new JLabel("<html> <b>Preemption overhead</b>:</html>");
		switchoverSpinnerPanel.add(switchoverSpinnerLabel);
		switchoverSpinnerPanel.add(switchoverTimeSpinner);

		// Add switchover time panel to the main panel
		quantumSizePanel.add(switchoverSpinnerPanel);
	}

	/**
	 * Registers front-end component listeners for triggering station data updates
	 */
	private void registerPanelListeners() {
		// Change Listener for the processor sharing limit spinner
		// Includes validation of the spinner value - minimum is 1
		// maxRunningJobs set to -1 in the back-end if spinner value is Double.POSITIVE_INFINITY
		ChangeListener processorLimitSpinnerChangeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent changeEvent) {
				Object value = processorLimitSpinner.getValue();
				if (value instanceof Integer) {
					if (((Integer) value).intValue() < 1) {
						value = Integer.valueOf(1);
						processorLimitSpinner.setValue(value);
						return;
					}
					stationData.setStationMaxRunningJobs(stationKey, (Integer) value);
					processorLimitWarningPanel.setVisible((Integer) value < stationData.getStationNumberOfServers(stationKey));
				} else {
					stationData.setStationMaxRunningJobs(stationKey, Integer.valueOf(-1));
					processorLimitWarningPanel.setVisible(false);
				}
			}
		};

		processorLimitSpinner.addChangeListener(processorLimitSpinnerChangeListener);

		// Action Listener for the infinite radio button
		ActionListener processorLimitInfiniteSelectorListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (processorLimitInfiniteSelector.isSelected()) {
					processorLimitSpinner.setEnabled(false);
					processorLimitSpinner.setValue(Double.POSITIVE_INFINITY);
					processorLimitFiniteSelector.setSelected(false);
				}
			}
		};

		// Action Listener for the finite radio button
		ActionListener processorLimitFiniteSelectorListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				if (processorLimitFiniteSelector.isSelected()) {
					processorLimitSpinner.setEnabled(true);
					processorLimitSpinner.setValue(Defaults.getAsInteger("stationRunningJobs"));
					processorLimitInfiniteSelector.setSelected(false);
				}
			}
		};

		processorLimitInfiniteSelector.addActionListener(processorLimitInfiniteSelectorListener);
		processorLimitFiniteSelector.addActionListener(processorLimitFiniteSelectorListener);

		// Change Listener for the quantum size spinner
		quantumSizeSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent changeEvent) {
				Double quantumSize = (Double) quantumSizeSpinner.getValue();
				stationData.setQuantumSize(stationKey, quantumSize);
			}
		});

		// Change Listener for the switchover time spinner
		switchoverTimeSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent changeEvent) {
				Double switchoverTime = (Double) switchoverTimeSpinner.getValue();
				stationData.setQuantumSwitchoverTime(stationKey, switchoverTime);
			}
		});
	}

	/**
	 * Updates the processor limit front-end components on panel initialisation and on any engine (back-end) update
	 */
	private void updateMaxRunningJobs() {
		Integer maxRunningJobs = stationData.getStationMaxRunningJobs(stationKey);
		if (maxRunningJobs < 1) {
			processorLimitSelectors.setSelected(processorLimitInfiniteSelector.getModel(), true);
			processorLimitSpinner.setValue(Double.POSITIVE_INFINITY);
			processorLimitSpinner.setEnabled(false);
		} else {
			processorLimitSelectors.setSelected(processorLimitFiniteSelector.getModel(), true);
			processorLimitSpinner.setValue(maxRunningJobs);
			processorLimitSpinner.setEnabled(true);
		}

		// Update quanta size spinner with the current value from stationData
		Double quantumSize = stationData.getQuantumSize(stationKey);
		if (quantumSize != null) {
			quantumSizeSpinner.setValue(quantumSize);
		}

		// Update switchover time spinner with the current value from stationData
		Double switchoverTime = stationData.getQuantumSwitchoverTime(stationKey);
		if (switchoverTime != null) {
			switchoverTimeSpinner.setValue(switchoverTime);
		}
	}

	@Override
	public String getName() {
		return "Processor Sharing Server Properties Editor";
	}
}
