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
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;

import jmt.framework.gui.layouts.SpringUtilities;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.GuiInterface;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.exact.table.ExactCellEditor;
import jmt.gui.exact.table.ExactTable;
import jmt.gui.exact.table.ExactTableModel;

/**
 * <p>Title: Simulation & Preloading Panel</p>
 * <p>Description: A panel in which simulation parameters and queue preloading can be specified</p>
 * 
 * @author Bertoli Marco
 *         Date: 16-set-2005
 *         Time: 14.01.02
 *
 * Modified by Francesco D'Aquino 11/11/2005
 */
public class SimulationPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;
	protected static final int BORDERSIZE = 20;
	protected static final long MINIMUM_TIME = 5; // Minimum simulation duration
	protected ClassDefinition cd;
	protected StationDefinition sd;
	protected SimulationDefinition simd;
	protected HashMap<Object, Integer> unallocated;

	// Simulation parameters
	protected JCheckBox randomSeed;
	protected JCheckBox infDuration;
	protected JCheckBox infSimulated;
	protected JCheckBox noStatistic;
	protected JCheckBox infEvents;
	protected JCheckBox animationEnabler;
	protected JSpinner seed;
	protected JSpinner duration;
	protected JSpinner simulated;
	protected JSpinner maxSamples;
	protected JSpinner maxEvents;
	protected JSpinner polling;
	protected JTable preloadTable;

	private GuiInterface gui;

	/**
	 * Builds a new simulation panel
	 * @param cd a ClassDefinition data structure
	 * @param sd a StationDefinition data structure
	 * @param simd a SimulationDefinition data structure
	 */
	public SimulationPanel(ClassDefinition cd, StationDefinition sd, SimulationDefinition simd, GuiInterface gui) {
		setData(cd, sd, simd);
		this.gui = gui;
		InitGUI(this.gui);
		InitActions();
	}

	/**
	 * Initialize internal data structures
	 * @param cd a ClassDefinition data structure
	 * @param sd a StationDefinition data structure
	 * @param simd a SimulationDefinition data structure
	 */
	public void setData(ClassDefinition cd, StationDefinition sd, SimulationDefinition simd) {
		this.cd = cd;
		this.sd = sd;
		this.simd = simd;
		refreshDataStructures();
	}

	/**
	 * Refresh internal data structures. This method is separate from setData as have to
	 * be called in JSIM at every gotFocus event (or internal data structures will
	 * not be up-to-date)
	 */
	protected void refreshDataStructures() {
		// Creates an hashmap with unallocated jobs for every closed class
		unallocated = new HashMap<Object, Integer>();
		for (Object key : cd.getClosedClassKeys()) {
			int population = cd.getClassPopulation(key).intValue();
			int preload = simd.getPreloadedJobsNumber(key).intValue();
			unallocated.put(key, new Integer(population - preload));
		}
	}

	/**
	 * called by the Wizard when the panel becomes active
	 */
	@Override
	public void gotFocus() {
		refreshDataStructures();
		this.removeAll();
		InitGUI(gui);
		InitActions();
	}

	/**
	 * called by the Wizard before when switching to another panel
	 */
	@Override
	public void lostFocus() {
		// Aborts editing of table
		TableCellEditor editor = preloadTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
		simd.manageJobs();
	}

	/**
	 * Initialize all GUI related stuff
	 *
	 * Modified by Francesco D'Aquino.
	 * The old version was InitGUI(). The gui parameter is used to insert or not the
	 * JCheckBox used to enable queue animation.
	 */
	protected void InitGUI(GuiInterface gui) {
		// Adds margins and a central main panel
		this.setLayout(new BorderLayout());
		this.add(Box.createVerticalStrut(BORDERSIZE), BorderLayout.NORTH);
		this.add(Box.createVerticalStrut(BORDERSIZE), BorderLayout.SOUTH);
		this.add(Box.createHorizontalStrut(BORDERSIZE), BorderLayout.WEST);
		this.add(Box.createHorizontalStrut(BORDERSIZE), BorderLayout.EAST);
		JPanel mainpanel = new JPanel(new BorderLayout());
		this.add((mainpanel), BorderLayout.CENTER);

		JPanel upperPanel = new JPanel(new BorderLayout());
		JLabel descrLabel = new JLabel(SIMULATION_DESCRIPTION);
		descrLabel.setVerticalAlignment(SwingConstants.NORTH);
		upperPanel.add(descrLabel, BorderLayout.NORTH);
		upperPanel.add(Box.createVerticalStrut(BORDERSIZE / 2), BorderLayout.SOUTH);
		mainpanel.add(upperPanel, BorderLayout.NORTH);

		// Adds preloading table
		preloadTable = new PreloadingTable();
		preloadTable.setRowHeight(CommonConstants.ROW_HEIGHT);
		WarningScrollTable preloadPanel = new WarningScrollTable(preloadTable, WARNING_CLASS_STATION);
		mainpanel.add(preloadPanel, BorderLayout.CENTER);
		upperPanel.add(new JLabel("<HTML>" +
				"Initial customer locations:" + "<br/>" +
				"Jobs located in <b>Router</b>, <b>Fork</b>, <b>Logger</b> and <b>ClassSwitch</b> are treated as arriving into these stations."
				+"</HTML>"), BorderLayout.SOUTH);

		// Adds simulation parameters
		JPanel simPanel = new JPanel(new SpringLayout());
		JLabel label;
		// Simulation seed
		label = new JLabel("Simulation random seed: ");
		simPanel.add(label);
		seed = new JSpinner();
		seed.setValue(simd.getSimulationSeed());
		label.setLabelFor(seed);
		simPanel.add(seed);
		randomSeed = new JCheckBox("random");
		randomSeed.setToolTipText("Uses a random seed to initialize the random number generator");
		if (simd.getUseRandomSeed().booleanValue()) {
			randomSeed.setSelected(true);
			seed.setEnabled(false);
		}
		simPanel.add(randomSeed);
		// Maximum duration
		label = new JLabel("Maximum duration (sec): ");
		simPanel.add(label);
		duration = new JSpinner(new SpinnerNumberModel(1, .1, Integer.MAX_VALUE, 1));
		duration.setValue(simd.getMaximumDuration());
		label.setLabelFor(duration);
		simPanel.add(duration);
		infDuration = new JCheckBox("infinite");
		infDuration.setToolTipText("Disables the automatic timer used to stop simulation");
		if (simd.getMaximumDuration().doubleValue() < 0) {
			infDuration.setSelected(true);
			duration.setValue(new Double(600));
			duration.setEnabled(false);
		}
		simPanel.add(infDuration);

		// Maximum Simulated Time
		label = new JLabel("Maximum simulated time: ");
		simPanel.add(label);
		simulated = new JSpinner(new SpinnerNumberModel(1, .1, Integer.MAX_VALUE, 1));
		simulated.setValue(simd.getMaxSimulatedTime());
		label.setLabelFor(simulated);
		simPanel.add(simulated);
		infSimulated = new JCheckBox("infinite");
		infSimulated.setToolTipText("Disables the bound on the simulated time");
		if (simd.getMaxSimulatedTime().doubleValue() < 0) {
			infSimulated.setSelected(true);
			simulated.setValue(new Double(600));
			simulated.setEnabled(false);
		}
		simPanel.add(infSimulated);

		// Maximum number of samples
		label = new JLabel("Maximum number of samples: ");
		label.setToolTipText("Maximum number of samples collected for each performance index");
		simPanel.add(label);
		maxSamples = new JSpinner(new SpinnerNumberModel(1000000, 100000, Integer.MAX_VALUE, 50000));
		maxSamples.setValue(simd.getMaxSimulationSamples());
		maxSamples.setToolTipText("Maximum number of samples collected for each performance index");
		label.setLabelFor(maxSamples);
		simPanel.add(maxSamples);
		// Adds disable statistic checkbox
		noStatistic = new JCheckBox("no automatic stop");
		noStatistic.setToolTipText("Disables confidence interval/maximum relative error as simulation stopping criteria");
		noStatistic.setSelected(simd.getDisableStatistic().booleanValue());
		simPanel.add(noStatistic);

		// Maximum number of events
		label = new JLabel("Maximum number of events: ");
		label.setToolTipText("Maximum number of events processed by the simulation engine");
		simPanel.add(label);
		maxEvents = new JSpinner(new SpinnerNumberModel(1000000, 100000, Integer.MAX_VALUE, 50000));
		maxEvents.setValue(simd.getMaxSimulationEvents());
		maxEvents.setToolTipText("Maximum number of events processed by the simulation engine");
		label.setLabelFor(maxEvents);
		simPanel.add(maxEvents);
		infEvents = new JCheckBox("infinite");
		infEvents.setToolTipText("Disables the bound on the number of events");
		if (simd.getMaxSimulationEvents().intValue() < 0) {
			infEvents.setSelected(true);
			maxEvents.setValue(new Integer(1000000));
			maxEvents.setEnabled(false);
		}
		simPanel.add(infEvents);

		if (gui.isAnimationDisplayable()) {
			animationEnabler = new JCheckBox("animation");
			animationEnabler.setToolTipText("Shows queue animation during simulation");
			if (simd.isAnimationEnabled()) {
				animationEnabler.setSelected(true);
			}
		}

		// Maximum duration
		label = new JLabel("Animation update interval (sec): ");
		simPanel.add(label);
		polling = new JSpinner(new SpinnerNumberModel(1, .1, Integer.MAX_VALUE, 1));
		polling.setValue(simd.getPollingInterval());
		label.setLabelFor(polling);
		simPanel.add(polling);

		if (gui.isAnimationDisplayable()) {
			simPanel.add(animationEnabler);
		} else {
			simPanel.add(new JPanel());
		}

		SpringUtilities.makeCompactGrid(simPanel, 6, 3, //rows, cols
				16, 6, //initX, initY
				6, 6); //xPad, yPad
		upperPanel.add(simPanel, BorderLayout.CENTER);
	}

	/**
	 * Inits all action listeners related to GUI object
	 */
	protected void InitActions() {
		// Random seed
		randomSeed.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (randomSeed.isSelected()) {
					simd.setUseRandomSeed(new Boolean(true));
					seed.setEnabled(false);
				} else {
					simd.setUseRandomSeed(new Boolean(false));
					seed.setEnabled(true);
				}
			}
		});

		// Infinite duration
		infDuration.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (infDuration.isSelected()) {
					simd.setMaximumDuration(new Double(-1));
					duration.setEnabled(false);
				} else {
					Double value = (Double) duration.getValue();
					simd.setMaximumDuration(value);
					duration.setEnabled(true);
				}
			}
		});

		// Infinite simulated time
		infSimulated.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (infSimulated.isSelected()) {
					simd.setMaxSimulatedTime(new Double(-1));
					simulated.setEnabled(false);
				} else {
					Double value = (Double) simulated.getValue();
					simd.setMaxSimulatedTime(value);
					simulated.setEnabled(true);
				}
			}
		});

		// Disable statistic
		noStatistic.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				simd.setDisableStatistic(new Boolean(noStatistic.isSelected()));
			}
		});

		// Infinite number of events
		infEvents.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (infEvents.isSelected()) {
					simd.setMaxSimulationEvents(new Integer(-1));
					maxEvents.setEnabled(false);
				} else {
					Integer value = (Integer) maxEvents.getValue();
					simd.setMaxSimulationEvents(value);
					maxEvents.setEnabled(true);
				}
			}
		});

		// Enable Animation
		if (animationEnabler != null) {
			animationEnabler.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (animationEnabler.isSelected()) {
						simd.setAnimationEnabled(true);
					} else {
						simd.setAnimationEnabled(false);
					}
				}
			});
		}

		// Seed
		seed.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Object value = seed.getValue();
				if (value instanceof Long) {
					Long l = (Long) value;
					if (l.longValue() >= 0) {
						simd.setSimulationSeed(l);
					}
				} else if (value instanceof Integer) {
					Integer i = (Integer) value;
					if (i.intValue() >= 0) {
						simd.setSimulationSeed(new Long(i.intValue()));
					}
				}
			}
		});

		// Duration
		duration.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Double value = (Double) duration.getValue();
				if (value.doubleValue() >= MINIMUM_TIME) {
					simd.setMaximumDuration(value);
				}
			}
		});

		// Simulated Time
		simulated.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Double value = (Double) simulated.getValue();
				if (value.doubleValue() >= MINIMUM_TIME) {
					simd.setMaxSimulatedTime(value);
				}
			}
		});

		// Maximum number of samples
		maxSamples.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Integer value = (Integer) maxSamples.getValue();
				if (value.intValue() >= 100000) {
					simd.setMaxSimulationSamples(value);
				}
			}
		});

		// Maximum number of events
		maxEvents.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Integer value = (Integer) maxEvents.getValue();
				if (value.intValue() >= 100000) {
					simd.setMaxSimulationEvents(value);
				}
			}
		});

		// Polling interval
		polling.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Double value = (Double) polling.getValue();
				if (value.doubleValue() > 0) {
					simd.setPollingInterval(value);
				}
			}
		});
	}

	/**
	 * Inner class to specify preloading table
	 */
	protected class PreloadingTable extends ExactTable {

		private static final long serialVersionUID = 1L;

		public PreloadingTable() {
			super(new PreloadTableModel());
			setDefaultEditor(Integer.class, new ExactCellEditor());
			autoResizeMode = AUTO_RESIZE_OFF;
			setDisplaysScrollLabels(true);
			setRowSelectionAllowed(true);
			setColumnSelectionAllowed(true);
			setBatchEditingEnabled(true);
		}
	}

	/**
	 * Model for Preload table
	 * Rows represent classes, columns stations.
	 */
	protected class PreloadTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		public PreloadTableModel() {
			prototype = "Station10000";
			rowHeaderPrototype = "Class10000 (Ni = 100)";
		}

		public int getRowCount() {
			return cd.getClassKeys().size();
		}

		public int getColumnCount() {
			return sd.getStationKeysPreloadable().size();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex >= 0) {
				return Integer.class;
			} else {
				return super.getColumnClass(columnIndex);
			}
		}

		@Override
		public String getColumnName(int index) {
			if (index >= 0 && sd.getStationKeysPreloadable().size() > 0) {
				return sd.getStationName(getStationKey(index));
			} else {
				return "";
			}
		}

		@Override
		public Object getPrototype(int i) {
			if (i == -1) {
				return rowHeaderPrototype;
			} else {
				return prototype;
			}
		}

		@Override
		protected Object getValueAtImpl(int rowIndex, int columnIndex) {
			Object row = getClassKey(rowIndex), col = getStationKey(columnIndex);
			return simd.getPreloadedJobs(col, row);
		}

		@Override
		protected Object getRowName(int rowIndex) {
			String className = cd.getClassName(getClassKey(rowIndex));
			Integer population = cd.getClassPopulation(getClassKey(rowIndex));
			if (cd.getClassType(getClassKey(rowIndex)) == CLASS_TYPE_OPEN) {
				return className;
			} else {
				return className + " (Ni = " + population + ")";
			}
		}

		//returns search key of a station given its index in table
		private Object getStationKey(int index) {
			return sd.getStationKeysPreloadable().get(index);
		}

		//returns search key of a class given its index in table
		private Object getClassKey(int index) {
			return cd.getClassKeys().get(index);
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			try {
				if (value instanceof Integer || value instanceof String) {
					int i;
					if (value instanceof Integer) {
						i = ((Integer) value).intValue();
					} else {
						i = Integer.parseInt((String) value);
					}

					Object key = getClassKey(rowIndex);
					int oldjobs = simd.getPreloadedJobs(getStationKey(columnIndex), key).intValue();
					if (i >= 0) {
						if (cd.getClassType(key) == CLASS_TYPE_OPEN) {
							simd.setPreloadedJobs(getStationKey(columnIndex), key, new Integer(i));
						} else if (i - oldjobs <= unallocated.get(key).intValue()) {
							simd.setPreloadedJobs(getStationKey(columnIndex), key, new Integer(i));
							int newunallocated = unallocated.get(key).intValue() - i + oldjobs;
							unallocated.put(key, new Integer(newunallocated));
						}
					}
				}
			} catch (NumberFormatException e) {
				// Aborts modification if String is invalid
			}
		}

		@Override
		public void clear(int row, int col) {
			int oldjobs = simd.getPreloadedJobs(getStationKey(col), getClassKey(row)).intValue();
			simd.setPreloadedJobs(getStationKey(col), getClassKey(row), new Integer(0));
			// If class is closed, put back old jobs into unallocated data structure
			if (cd.getClassType(getClassKey(row)) == CLASS_TYPE_CLOSED) {
				int newunallocated = unallocated.get(getClassKey(row)).intValue() + oldjobs;
				unallocated.put(getClassKey(row), new Integer(newunallocated));
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

	}

	/**
	 * @return name to be displayed on the tab, when inserted in a wizard tabbed pane
	 */
	@Override
	public String getName() {
		return "Simulation";
	}

}
