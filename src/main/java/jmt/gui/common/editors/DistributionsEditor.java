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

package jmt.gui.common.editors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jmt.framework.gui.components.JMTDialog;
import jmt.framework.gui.image.ImagePanel;
import jmt.framework.gui.layouts.SpringUtilities;
import jmt.framework.gui.listeners.KeyFocusAdapter;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.distributions.Burst;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Distribution.Parameter;
import jmt.gui.common.distributions.Distribution.ValueChecker;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.distributions.MAP;
import jmt.gui.common.distributions.PhaseType;
import jmt.gui.common.distributions.Replayer;
import jmt.gui.exact.table.ExactCellRenderer;
import jmt.gui.exact.table.ExactTable;
import jmt.gui.exact.table.ExactTableModel;

/**
 * <p>
 * Title: Distributions' Editor
 * </p>
 * <p>
 * Description: A modal dialog used to choose a specific distribution for a
 * class or station service and to enter its parameters. Users will enter owner
 * Frame or Dialog and initial Distribution (can be null) and will collect
 * chosen distribution with <code>getResult()</code> method.
 * </p>
 * 
 * @author Bertoli Marco Date: 29-giu-2005 Time: 11.31.07
 */
public class DistributionsEditor extends JMTDialog {

	private static final long serialVersionUID = 1L;

	// Internal data structure
	protected Distribution initial;

	protected Distribution current;

	protected Distribution target;

	protected boolean recursive;

	/**
	 * This variable will be initialized only once. It will contains every
	 * distribution that can be inserted
	 */
	protected static Map<String, Class<? extends Distribution>> distributions;

	protected static Map<String, Class<? extends Distribution>> nestedDistributions;

	// Constants
	protected static final int BORDERSIZE = 20;

	// Components
	protected JComboBox chooser = new JComboBox();
	protected ImagePanel iconpanel = new ImagePanel();
	protected JPanel param_panel = new JPanel(new SpringLayout());
	protected JPanel mean_c_panel = new JPanel(new SpringLayout());

	protected JPanel scrollPanel;

	// Names and display keys for labels
	protected static final String PROBABILITY = "Probability:";
	protected static final String PROBABILITY_INTERVAL_A = "probability_interval_A";
	protected static final String PROBABILITY_INTERVAL_B = "probability_interval_B";
	protected static final String VALUE_DISTRIBUTION = "Value Distribution:";
	protected static final String INTERVAL_LENGTH_DISTRIBUTION = "Interval-Length Distribution:";
	protected static final String INTERVAL_A_LABEL = "Interval type A";
	protected static final String INTERVAL_B_LABEL = "Interval type B";
	protected static final String ROUND_ROBIN = "Round-Robin";

	protected JPanel[] intervalPanels;

	protected JSpinner phaseNumSpinner;
	protected ParameterTable[] parameterTables;

	// --- Static methods ------------------------------------------------------------------------------
	/**
	 * Returns a new instance of DistributionsEditor, given parent container
	 * (used to find top level Dialog or Frame to create this dialog as modal).
	 * The container is instantiated as not recursive
	 * 
	 * @param parent
	 *            any type of container contained in a Frame or Dialog
	 * @param initial
	 *            initial distribution to be set
	 * @return new instance of DistributionsEditor
	 */
	public static DistributionsEditor getInstance(Container parent,
			Distribution initial) {
		return getInstance(parent, initial, false);
	}

	/**
	 * Returns a new instance of DistributionsEditor, given parent container
	 * (used to find top level Dialog or Frame to create this dialog as modal)
	 * 
	 * @param parent
	 *            any type of container contained in a Frame or Dialog
	 * @param initial
	 *            initial distribution to be set
	 * @param recursive
	 *            indicated if the DistributionEditor is used to select a nested
	 *            distribution
	 * @return new instance of DistributionsEditor
	 */
	public static DistributionsEditor getInstance(Container parent,
			Distribution initial, boolean recursive) {
		// Finds top level Dialog or Frame to invoke correct constructor
		while (!(parent instanceof Frame || parent instanceof Dialog)) {
			parent = parent.getParent();
		}

		if (parent instanceof Frame) {
			return new DistributionsEditor((Frame) parent, initial, recursive);
		} else {
			return new DistributionsEditor((Dialog) parent, initial, recursive);
		}
	}

	/**
	 * Uses reflection to return a Map of distributions. Search's key is
	 * distribution name and value is the Class of found distribution
	 * 
	 * @return found distributions
	 */
	protected static Map<String, Class<? extends Distribution>> findDistributions() {
		Distribution[] all = Distribution.findAll();
		Map<String, Class<? extends Distribution>> tmp = new LinkedHashMap<String, Class<? extends Distribution>>();
		for (Distribution element : all) {
			tmp.put(element.getName(), element.getClass());
		}
		return tmp;
	}

	/**
	 * Uses reflection to return a Map of distributions which are allowed
	 * to be nested. Search's key is distribution name and value is the Class of
	 * found distribution
	 * 
	 * @return found nested distributions
	 */
	protected static Map<String, Class<? extends Distribution>> findNestedDistributions() {
		Distribution[] all = Distribution.findNestableDistributions();
		Map<String, Class<? extends Distribution>> tmp = new LinkedHashMap<String, Class<? extends Distribution>>();
		for (Distribution element : all) {
			tmp.put(element.getName(), element.getClass());
		}
		return tmp;
	}

	// -------------------------------------------------------------------------------------------------

	// --- Method to collect results -------------------------------------------------------------------
	/**
	 * Returns Distribution selected in this dialog or initial one if cancel
	 * button was pressed
	 * 
	 * @return Selected distribution if okay button was pressed, initial
	 *         otherwise. If this dialog has not been shown yet, returns initial
	 *         value too.
	 */
	public Distribution getResult() {
		return target;
	}

	// -------------------------------------------------------------------------------------------------

	// --- Constructors to create modal dialog ---------------------------------------------------------
	/**
	 * Builds a new Distribution Editor Dialog. This dialog is designed to be
	 * modal.
	 * 
	 * @param owner
	 *            owner Dialog for this dialog.
	 * @param initial
	 *            Reference to initial distribution to be shown
	 */
	public DistributionsEditor(Dialog owner, Distribution initial,
			boolean recursive) {
		super(owner, true);
		this.recursive = recursive;
		initData(initial);
		initComponents();
	}

	/**
	 * Builds a new Distribution Editor Dialog. This dialog is designed to be
	 * modal.
	 * 
	 * @param owner
	 *            owner Frame for this dialog.
	 * @param initial
	 *            Reference to initial distribution to be shown
	 */
	public DistributionsEditor(Frame owner, Distribution initial,
			boolean recursive) {
		super(owner, true);
		this.recursive = recursive;
		initData(initial);
		initComponents();
	}

	// -------------------------------------------------------------------------------------------------

	// --- Actions performed by buttons and EventListeners ---------------------------------------------
	// When okay button is pressed
	protected AbstractAction okayAction = new AbstractAction("OK") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION,
					"Closes this window and apply changes");
		}

		public void actionPerformed(ActionEvent e) {
			// Checks if distribution parameters are correct
			if (current.checkValue()) {
				target = current;
				DistributionsEditor.this.dispose();
			} else {
				JOptionPane.showMessageDialog(
						DistributionsEditor.this,
						current.getPrecondition(),
						"Parameter Error", JOptionPane.ERROR_MESSAGE);
				DistributionsEditor.this.repaint();
			}
		}

	};

	// When cancel button is pressed
	protected AbstractAction cancelAction = new AbstractAction("Cancel") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION,
					"Closes this window discarding all changes");
		}

		public void actionPerformed(ActionEvent e) {
			target = initial;
			DistributionsEditor.this.dispose();
		}

	};

	/**
	 * Listener used to set parameters (associated to param_panel's
	 * JTextFields). Parameters are set when JTextField loses focus or ENTER key
	 * is pressed.
	 */
	protected KeyFocusAdapter parameterListener = new KeyFocusAdapter() {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * jmt.framework.gui.listeners.KeyFocusAdapter#updateValues(java.awt
		 * .event.ComponentEvent)
		 */
		@Override
		protected void updateValues(ComponentEvent e) {
			// Finds parameter number
			JTextField sourcefield = (JTextField) e.getSource();
			int num = Integer.parseInt(sourcefield.getName());
			current.getParameter(num).setValue(sourcefield.getText());
			current.updateCM();
			refreshValues();
		}
	};

	/**
	 * Listener that listens on Mean and C variations and updates parameters
	 */
	protected KeyFocusAdapter cmListener = new KeyFocusAdapter() {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * jmt.framework.gui.listeners.KeyFocusAdapter#updateValues(java.awt
		 * .event.ComponentEvent)
		 */
		@Override
		protected void updateValues(ComponentEvent e) {
			// Finds parameter number
			JTextField sourcefield = (JTextField) e.getSource();
			try {
				if (sourcefield.getName().equals("mean")) {
					current.setMean(Double.parseDouble(sourcefield.getText()));
				} else if (sourcefield.getName().equals("c")) {
					current.setC(Double.parseDouble(sourcefield.getText()));
				}
			} catch (NumberFormatException ex) { // Do nothing
			}
			refreshValues();
		}
	};

	/**
	 * Listener for chooser ComboBox to instantiate a new distributions data
	 * object when current distribution type is changed.
	 */
	protected ItemListener change_listener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			try {
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					return;
				}
				if (recursive) {
					current = nestedDistributions.get(e.getItem()).newInstance();
				} else {
					current = distributions.get(e.getItem()).newInstance();
				}
				refreshView();
			} catch (InstantiationException ex) {
				System.out
				.println("Error: Error instantiating selected Distribution");
				ex.printStackTrace();
			} catch (IllegalAccessException ex) {
				System.out
				.println("Error: Error accessing to selected Distribution");
				ex.printStackTrace();
			}
		}
	};

	private JTextField replayerTextField;

	private JButton chooseReplayerFileButton;

	private JFileChooser replayerFileDialog = new JFileChooser(Defaults.getWorkingPath());

	/**
	 * Listener used for Burst-Distribution only. Updates the Round-Robin
	 * parameter. Parameter is set when the Round-Robin checkbox is checked or
	 * it is unchecked.
	 */
	protected class RoundRobinAdapter implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// Get the checkbox and its value
			JCheckBox sourcefield = (JCheckBox) e.getSource();
			Boolean isRoundRobinChecked = new Boolean(sourcefield.isSelected());
			// Set the Round-Robin parameter
			current.getParameter(5).setValue(isRoundRobinChecked);

			refreshValues();
		}
	}

	/**
	 * Listener used for Burst-Distribution only. Updates the two probability
	 * parameters seen by the user Parameters are set when one of the parameter
	 * JTextField loses focus or ENTER key is pressed.
	 */
	protected class ProbabilityAdapter extends KeyFocusAdapter {
		@Override
		protected void updateValues(ComponentEvent e) {
			// Get the textfield
			JTextField sourcefield = (JTextField) e.getSource();
			try {
				// Get the probability entered in the textfield
				Double probability = new Double(Double.parseDouble(sourcefield
						.getText()));
				// Probability has to be between 0 and 1 (otherwise do not
				// update value)
				if (probability.doubleValue() >= 0.0 && probability.doubleValue() <= 1.0) {
					// If the probability was entered into the probability field
					// of interval B
					// then the probability parameter in the distribution has to
					// be set to 1-enteredProbability
					if (sourcefield.getName().equals(PROBABILITY_INTERVAL_B)) {
						probability = new Double(1 - probability.doubleValue());
					}
					// set the parameter
					current.getParameter(0).setValue(probability);
				}
			} catch (NumberFormatException ex) {
				// If user enters a value that is not a number -> reset value
				// back to the value before
			}

			refreshValues();
		}
	}

	/**
	 * This class is currently only used for Burst distributions since the other
	 * distributions do not contain any nested distributions as parameters
	 * Action performed when user clicks on Edit-Button at a Distribution
	 * Parameter Opens a new Distribution Editor to change and edit distribution
	 * parameters
	 * 
	 * @author Peter Parapatics
	 * 
	 */
	protected class EditButtonAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		private int key;

		public EditButtonAction(int key) {
			super("Edit");
			this.key = key;
		}

		public void actionPerformed(ActionEvent e) {
			DistributionsEditor editor = DistributionsEditor.getInstance(
					getParent(), (Distribution) current.getParameter(key)
					.getValue(), true);
			// Sets editor window title
			editor.setTitle("Editing "
					+ current.getParameter(key).getDescription());
			// Shows editor window
			editor.show();
			// Sets new Distribution to selected class
			current.getParameter(key).setValue(editor.getResult());
			refreshValues();
		}

	};

	// -------------------------------------------------------------------------------------------------

	// --- Initialize data structure and layout --------------------------------------------------------
	/**
	 * Initialize this dialog data structures
	 * 
	 * @param initial
	 *            Reference to initial distribution to be shown
	 */
	protected void initData(Distribution initial) {
		this.initial = initial;
		this.target = initial;
		if (initial != null) {
			this.current = initial.clone();
		} else {
			// Default distribution if nothing is selected
			this.current = new Exponential();
		}

		// If distributions is not already set, sets it!
		if (distributions == null) {
			distributions = findDistributions();
		}

		if (nestedDistributions == null && recursive) {
			nestedDistributions = findNestedDistributions();
		}
	}

	/**
	 * Initialize this dialod's components and default dialog property
	 */
	protected void initComponents() {
		// Sets default title, close operation and dimensions
		this.setTitle("Editing Distribution...");
		// Centers this dialog on the screen
		if (recursive) {
			this.centerWindowWithOffset(CommonConstants.MAX_GUI_WIDTH_JSIM_DISTRIB,
					CommonConstants.MAX_GUI_HEIGHT_JSIM_DISTRIB, 50, 50);
		} else {
			this.centerWindow(CommonConstants.MAX_GUI_WIDTH_JSIM_DISTRIB,
					CommonConstants.MAX_GUI_HEIGHT_JSIM_DISTRIB);
		}

		// Creates a main panel and adds margins to it
		JPanel mainpanel = new JPanel(new BorderLayout());
		mainpanel.setLayout(new BorderLayout());
		mainpanel.add(Box.createVerticalStrut(BORDERSIZE), BorderLayout.NORTH);
		mainpanel.add(Box.createVerticalStrut(BORDERSIZE), BorderLayout.SOUTH);
		mainpanel.add(Box.createHorizontalStrut(BORDERSIZE), BorderLayout.WEST);
		mainpanel.add(Box.createHorizontalStrut(BORDERSIZE), BorderLayout.EAST);
		this.getContentPane().add(mainpanel, BorderLayout.CENTER);

		// Creates a subpanel that holds scrollpanel and distr_panel and adds
		// it to mainpanel
		JPanel subpanel = new JPanel(new BorderLayout());
		mainpanel.add(subpanel, BorderLayout.CENTER);
		JPanel distr_panel = new JPanel(new BorderLayout());
		subpanel.add(distr_panel, BorderLayout.NORTH);

		// Creates scrollpanel that holds param_panel and mean_c_panel
		scrollPanel = new JPanel(new GridLayout(2, 1));
		subpanel.add(scrollPanel, BorderLayout.CENTER);

		// Adds bottom_panel to contentpane
		JPanel bottom_panel = new JPanel(new FlowLayout());
		this.getContentPane().add((bottom_panel), BorderLayout.SOUTH);

		// Adds Okay button to bottom_panel
		JButton okaybutton = new JButton(okayAction);
		bottom_panel.add(okaybutton);

		// Adds Cancel button to bottom_panel
		JButton cancelbutton = new JButton(cancelAction);
		bottom_panel.add(cancelbutton);

		// Adds distribution chooser
		distr_panel.add(new JLabel("Selected Distribution: "),
				BorderLayout.WEST);

		Object[] distributionNames;
		if (recursive) {
			distributionNames = nestedDistributions.keySet().toArray();
		} else {
			distributionNames = distributions.keySet().toArray();
		}

		// names
		chooser = new JComboBox(distributionNames);
		//ARIF: show all elements in ComboBox
		chooser.setMaximumRowCount(distributionNames.length);
		chooser.setToolTipText("Choose distribution type");
		// Select correct distribution
		if (current != null) {
			chooser.setSelectedItem(current.getName());
			refreshView();
		}
		chooser.addItemListener(change_listener);
		distr_panel.add(chooser, BorderLayout.CENTER);

		// Adds image viewer with a couple of borders
		JPanel image_panel = new JPanel(new BorderLayout());
		distr_panel.add(image_panel, BorderLayout.SOUTH);
		image_panel.add(Box.createVerticalStrut(BORDERSIZE / 2),
				BorderLayout.NORTH);
		image_panel.add(Box.createVerticalStrut(BORDERSIZE / 2),
				BorderLayout.SOUTH);
		image_panel.add(iconpanel, BorderLayout.CENTER);
	}

	// ------------------------------------------------------------------
	// --- Shows current distribution
	// ------------------------------------------------------------------
	protected void refreshView() {
		if (current != null) {
			// Flushes param_panel
			param_panel.removeAll();
			mean_c_panel.removeAll();
			scrollPanel.removeAll();
			intervalPanels = null;
			// Shows image
			iconpanel.setImage(current.getImage());

			if (current instanceof Burst) {
				((GridLayout) scrollPanel.getLayout()).setRows(1);

				// Flushes param_panel
				intervalPanels = new JPanel[3];
				intervalPanels[0] = new JPanel();
				intervalPanels[1] = new JPanel();
				intervalPanels[2] = new JPanel();

				BurstRenderer rend = new BurstRenderer();

				rend.addInterval(ROUND_ROBIN, intervalPanels[0]);
				rend.addRoundRobin(intervalPanels[0]);

				rend.addInterval(INTERVAL_A_LABEL, intervalPanels[1]);
				rend.addProbability(intervalPanels[1], true);
				rend.addDistribution(INTERVAL_LENGTH_DISTRIBUTION, 1,
						intervalPanels[1]);
				rend.addDistribution(VALUE_DISTRIBUTION, 2, intervalPanels[1]);

				rend.addInterval(INTERVAL_B_LABEL, intervalPanels[2]);
				rend.addProbability(intervalPanels[2], false);
				rend.addDistribution(INTERVAL_LENGTH_DISTRIBUTION, 3,
						intervalPanels[2]);
				rend.addDistribution(VALUE_DISTRIBUTION, 4, intervalPanels[2]);
			} else if (current instanceof PhaseType || current instanceof MAP) {
				((GridLayout) scrollPanel.getLayout()).setRows(1);

				int phaseNum = ((Object[][]) current.getParameter(0).getValue())[0].length;
				phaseNumSpinner = new JSpinner();
				phaseNumSpinner.setPreferredSize(CommonConstants.DIM_BUTTON_XS);
				phaseNumSpinner.setModel(new SpinnerNumberModel(phaseNum, 1,
						CommonConstants.MAX_NUMBER_OF_PHASES, 1));
				phaseNumSpinner.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						int phaseNum = ((Integer) phaseNumSpinner.getValue()).intValue();
						for (int i = 0; i < current.getNumberOfParameters(); i++) {
							Object[][] oldMatrix = (Object[][]) current.getParameter(i).getValue();
							Object[][] newMatrix = null;
							if (current instanceof PhaseType && i == 0) {
								newMatrix = new Object[1][phaseNum];
							} else {
								newMatrix = new Object[phaseNum][phaseNum];
							}
							for (int j = 0; j < newMatrix.length; j++) {
								for (int k = 0; k < newMatrix[j].length; k++) {
									if (j < oldMatrix.length && k < oldMatrix[j].length) {
										newMatrix[j][k] = oldMatrix[j][k];
									} else {
										newMatrix[j][k] = current.getParameter(i).parseValue("0");
									}
								}
							}
							current.getParameter(i).setValue(newMatrix);
							parameterTables[i].updateStructure();
						}
					}
				});
				JPanel phaseNumPanel = new JPanel();
				phaseNumPanel.setBorder(new TitledBorder(new EtchedBorder(),
						(current instanceof PhaseType) ? "Number of Phases" : "Number of States"));
				phaseNumPanel.add(new JLabel("Number: "));
				phaseNumPanel.add(phaseNumSpinner);

				int parameterNum = current.getNumberOfParameters();
				parameterTables = new ParameterTable[parameterNum];
				JPanel parametersPanel = new JPanel(new GridLayout(parameterNum, 1));
				for (int i = 0; i < parameterNum; i++) {
					parameterTables[i] = new ParameterTable(current.getParameter(i));
					JScrollPane parameterPane = new JScrollPane(parameterTables[i]);
					parameterPane.setBorder(new TitledBorder(new EtchedBorder(),
							current.getParameter(i).getDescription()));
					parametersPanel.add(parameterPane);
				}

				JPanel compositePanel = new JPanel();
				compositePanel.setLayout(new BorderLayout());
				compositePanel.add(phaseNumPanel, BorderLayout.NORTH);
				compositePanel.add(parametersPanel, BorderLayout.CENTER);
				scrollPanel.add(compositePanel);
			} else {
				((GridLayout) scrollPanel.getLayout()).setRows(1);

				// Maximum width (used to line up elements of both panels)
				int maxwidth = new JLabel("mean:", SwingConstants.TRAILING)
						.getMinimumSize().width;

				// Shows this distribution's parameters on param_panel
				JLabel label;
				JTextField textfield;
				scrollPanel.add(new JScrollPane(param_panel));
				for (int i = 0; i < current.getNumberOfParameters(); i++) {
					// Creates the label
					label = new JLabel(current.getParameter(i).getDescription()
							+ ":", SwingConstants.TRAILING);
					// Corrects maxwidth if needed
					if (maxwidth < label.getMinimumSize().width) {
						maxwidth = label.getMinimumSize().width;
					}
					// Creates the textfield used to input values
					textfield = new JTextField(5);
					textfield.setMaximumSize(new Dimension(textfield
							.getMaximumSize().width,
							textfield.getMinimumSize().height));
					label.setLabelFor(textfield);
					textfield.setName(Integer.toString(i));
					textfield.addFocusListener(parameterListener);
					textfield.addKeyListener(parameterListener);
					// The Replayer has a fileName parameter.
					// So with this simple patch we add a FileDialog to select
					// the file. Notice that the constant Replayer.FILE_NAME_PARAMETER
					// makes this patch as less as possible error prone.
					if (current instanceof Replayer
							&& current.getParameter(i).getName()
							.equals(Replayer.FILE_NAME_PARAMETER)) {
						replayerTextField = textfield;
						chooseReplayerFileButton = new JButton("Choose the file");
						chooseReplayerFileButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								int status = replayerFileDialog.showOpenDialog(DistributionsEditor.this);
								if (status == JFileChooser.CANCEL_OPTION) {
									return;
								} else if (status == JFileChooser.ERROR_OPTION) {
									return;
								}
								replayerTextField.setText(replayerFileDialog.getSelectedFile().getAbsolutePath());
								int num = Integer.parseInt(replayerTextField.getName());
								current.getParameter(num).setValue(replayerTextField.getText());
								refreshValues();
							}
						});
						param_panel.add(replayerTextField);
						param_panel.add(chooseReplayerFileButton);
					} else {
						param_panel
						.add(label,
								new SpringLayout.Constraints(Spring
										.constant(0), Spring.constant(0),
										Spring.constant(maxwidth),
										Spring.constant(label
												.getMinimumSize().height)));
						param_panel.add(textfield);
					}
				}
				SpringUtilities.makeCompactGrid(param_panel,
						current.getNumberOfParameters(), 2, // rows, cols
						6, 6, // initX, initY
						6, 6); // xPad, yPad

				// Now shows mean and c (if applicable) on mean_c_panel
				if (current.hasC() || current.hasMean()) {
					((GridLayout) scrollPanel.getLayout()).setRows(2);

					scrollPanel.add(new JScrollPane(mean_c_panel));
					int rows = 0;
					mean_c_panel.setVisible(true);
					// Builds mean section
					if (current.hasMean()) {
						rows++;
						// Creates the label
						label = new JLabel("mean:", SwingConstants.TRAILING);
						mean_c_panel
						.add(label,
								new SpringLayout.Constraints(
										Spring.constant(0),
										Spring.constant(0),
										Spring.constant(maxwidth),
										Spring.constant(label
												.getMinimumSize().height)));
						// Creates the textfield used to input mean values
						textfield = new JTextField(5);
						textfield.setMaximumSize(new Dimension(textfield
								.getMaximumSize().width, textfield
								.getMinimumSize().height));
						label.setLabelFor(textfield);
						textfield.setName("mean");
						textfield.addFocusListener(cmListener);
						textfield.addKeyListener(cmListener);
						mean_c_panel.add(textfield);
					}

					// Builds c section
					if (current.hasC()) {
						rows++;
						// Creates the label
						label = new JLabel("c:", SwingConstants.TRAILING);
						mean_c_panel
						.add(label,
								new SpringLayout.Constraints(
										Spring.constant(0),
										Spring.constant(0),
										Spring.constant(maxwidth),
										Spring.constant(label
												.getMinimumSize().height)));
						// Creates the textfield used to input mean values
						textfield = new JTextField(5);
						textfield.setMaximumSize(new Dimension(textfield
								.getMaximumSize().width, textfield
								.getMinimumSize().height));
						label.setLabelFor(textfield);
						textfield.setName("c");
						textfield.addFocusListener(cmListener);
						textfield.addKeyListener(cmListener);
						mean_c_panel.add(textfield);
					}
					SpringUtilities.makeCompactGrid(mean_c_panel, rows, 2, // rows, cols
							6, 6, // initX, initY
							6, 6); // xPad, yPad
				}
			}

			refreshValues();
			scrollPanel.revalidate();
			scrollPanel.repaint();
		}
	}

	/**
	 * Helper method to extract the probability components the dialog's
	 * components. These components are the probability labels and the
	 * probability TextFields.
	 * 
	 * @return a Vector of probability related components
	 * @author Federico Dal Castello
	 */
	private Vector<Component> getProbabilityComponents() {
		Vector<Component> probabilityComponents = new Vector<Component>();

		Vector<Component> components = new Vector<Component>();
		components.addAll(Arrays.asList(intervalPanels[1].getComponents()));
		components.addAll(Arrays.asList(intervalPanels[2].getComponents()));

		Iterator<Component> it = components.iterator();

		while (it.hasNext()) {
			Component comp = it.next();

			if (comp instanceof JTextField) {
				if (comp.getName().equals(PROBABILITY_INTERVAL_A)
						|| comp.getName().equals(PROBABILITY_INTERVAL_B)) {
					probabilityComponents.add(comp);
				}
			}

			if (comp instanceof JLabel
					&& ((JLabel) comp).getText().equals(PROBABILITY)) {
				probabilityComponents.add(comp);
			}
		}

		return probabilityComponents;
	}

	protected void refreshValues() {
		DecimalFormat df = new DecimalFormat("#.############");
		df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));

		Vector<Component> components = new Vector<Component>();

		if (intervalPanels != null) {
			components.addAll(Arrays.asList(intervalPanels[0].getComponents()));
			components.addAll(Arrays.asList(intervalPanels[1].getComponents()));
			components.addAll(Arrays.asList(intervalPanels[2].getComponents()));
		} else {
			components.addAll(Arrays.asList(param_panel.getComponents()));
		}

		Iterator<Component> it = components.iterator();

		while (it.hasNext()) {
			Component comp = it.next();

			if (comp instanceof JTextField) {
				Object value = null;
				if (comp.getName().equals(PROBABILITY_INTERVAL_B)) {
					Double prob = (Double) current.getParameter(0).getValue();
					value = new Double(1 - prob.doubleValue());
				} else if (comp.getName().equals(PROBABILITY_INTERVAL_A)) {
					value = current.getParameter(0).getValue();
				} else {
					int num = Integer.parseInt(comp.getName());
					value = current.getParameter(num).getValue();
				}
				if (value != null) {
					if (value instanceof Double) {
						double val = ((Double) value).doubleValue();
						((JTextField) comp).setText(df.format(val));
					} else {
						((JTextField) comp).setText(value.toString());
					}
				}
			}

			if (comp instanceof JCheckBox) {
				// enables or disables the probability components
				if (comp.getName().equals(ROUND_ROBIN)) {

					Boolean isRoundRobinChecked = new Boolean(
							((JCheckBox) comp).isSelected());
					// if checked, disable; if not checked, enable
					boolean enableComponent = !isRoundRobinChecked
							.booleanValue();

					Vector<Component> probabilityComponents = getProbabilityComponents();
					Iterator<Component> probIt = probabilityComponents
							.iterator();

					while (probIt.hasNext()) {
						Component probComp = probIt.next();

						probComp.setEnabled(enableComponent);

						if (probComp instanceof JTextField) {
							// reset the probability value only if the component
							// is disabled
							if (!enableComponent) {
								current.getParameter(0).setValue(
										new Double(0.5));
							}
							// fully disables the probability text field
							((JTextField) probComp)
							.setEditable(enableComponent);
						}
					}
				}
			}
		}

		// refresh all values into mean_c_panel
		Component[] componentArray = mean_c_panel.getComponents();
		for (Component element : componentArray) {
			// Shows only first 10 decimal digits
			if (element instanceof JTextField
					&& element.getName().equals("mean")) {
				((JTextField) element).setText(df.format(current.getMean()));
			} else if (element instanceof JTextField
					&& element.getName().equals("c")) {
				((JTextField) element).setText(df.format(current.getC()));
			}
		}

		scrollPanel.repaint();
	}

	/**
	 * Class encapsulation methods to render Burst parameters
	 * 
	 * @author Peter Parapatics
	 * @author Federico Dal Castello
	 * 
	 */
	protected class BurstRenderer {

		JPanel burstContentPanel = new JPanel();

		public BurstRenderer() {
			// added elements will be appended to the bottom row (vertically)
			BoxLayout verticalBoxLayout = new BoxLayout(burstContentPanel,
					BoxLayout.Y_AXIS);
			burstContentPanel.setLayout(verticalBoxLayout);
			scrollPanel.add(new JScrollPane(burstContentPanel));
		}

		/**
		 * Renders the border and the position of the given panel and adds it to
		 * the scrolled panel
		 * 
		 * @param name
		 *            The name of the Interval
		 * @param panel
		 *            The panel which will contain the interval
		 */
		protected void addInterval(String name, JPanel panel) {
			GridBagLayout gridbag = new GridBagLayout();

			// Use gridbag layout
			panel.setLayout(gridbag);

			// Set black border around the interval.
			// The name of the interval is displayed on the border
			panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
					.createMatteBorder(1, 1, 1, 1, Color.BLACK), name,
					TitledBorder.DEFAULT_JUSTIFICATION,
					TitledBorder.DEFAULT_POSITION, new Font("Dialog",
							Font.BOLD, 12), Color.BLACK));

			burstContentPanel.add(panel);
		}

		/**
		 * Adds a distribution to the given panel
		 * 
		 * @param name
		 *            the name to be displayed on the label before the
		 *            distribution
		 * @param key
		 *            to which parameter number in the Burst Distribution this
		 *            nested distribution corresponds
		 * @param intervalPanel
		 *            the panel to which the distribution should be added
		 */
		protected void addDistribution(String name, int key,
				JPanel intervalPanel) {
			JLabel distributionNameLabel = new JLabel(name);

			// Add the name of the distribution on a single line
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(10, 0, 0, 0); // top padding
			c.gridwidth = GridBagConstraints.REMAINDER; // end row after this
			// entry
			c.fill = GridBagConstraints.HORIZONTAL;
			// how to fill space when enlarging window vertically
			c.weightx = 1.0;
			c.weighty = 0.0;
			// Add the distribution
			intervalPanel.add(distributionNameLabel, c);

			// Add the edit button
			JButton but = new JButton("Edit");
			but.setAction(new EditButtonAction(key));
			// Specifies the button size to maintain its width in the case that
			// probability text fields are hidden
			// TODO check if the specified values are compatible with all
			// graphical systems
			but.setPreferredSize(new Dimension(65, 24));

			c.insets = new Insets(0, 0, 0, 0); // No space between Name of
			// distribution and Edit button
			// do not finish row because also the label for the distribution has
			// to be added
			c.gridwidth = GridBagConstraints.RELATIVE;
			c.fill = GridBagConstraints.HORIZONTAL; // reset to default
			c.weightx = 0.0; // reset to default
			c.weighty = 0.0;
			// Add the button
			intervalPanel.add(but, c);

			JTextField distributionValueTextField = new JTextField();
			// The name of the field is the parameter number
			distributionValueTextField.setName("" + key);
			// If the distribution != null display
			if (current.getParameter(key).getValue() != null) {
				distributionValueTextField.setText(current.getParameter(key)
						.getValue().toString());
			}
			// The value is not editable directly
			distributionValueTextField.setEditable(false);
			c.gridwidth = GridBagConstraints.REMAINDER; // end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 1.0;
			intervalPanel.add(distributionValueTextField, c);
		}

		/**
		 * Add a probability to an interval Panel If the probability is for
		 * interval B the value is displayed as 1-probability
		 * 
		 * @param intervalPanel
		 *            the intervalPanel
		 * @param intervalA
		 *            if the probability is for interval A or B
		 */
		protected void addProbability(Container intervalPanel, boolean intervalA) {
			JLabel probLabel = new JLabel(PROBABILITY);
			JTextField probValue = new JTextField();
			Double probability = (Double) current.getParameter(0).getValue();

			// If the interval is interval A display value directly
			// Otherwise display 1-probability
			if (intervalA) {
				probValue.setName(PROBABILITY_INTERVAL_A);
			} else {
				probability = new Double(1 - probability.doubleValue());
				probValue.setName(PROBABILITY_INTERVAL_B);
			}
			probValue.setText(probability.toString());
			probLabel.setLabelFor(probValue);

			probValue.addFocusListener(new ProbabilityAdapter());
			probValue.addKeyListener(new ProbabilityAdapter());

			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last
			c.fill = GridBagConstraints.NONE; // reset to default
			c.weightx = 0.0; // reset to default
			c.weighty = 1.0;
			intervalPanel.add(probLabel, c);

			c.gridwidth = GridBagConstraints.REMAINDER; // end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 1.0;
			intervalPanel.add(probValue, c);
		}

		/**
		 * Adds a Round-Robin checkbox to an interval panel
		 * 
		 * @param intervalPanel
		 *            the interval panel
		 * @author Federico Dal Castello
		 */
		protected void addRoundRobin(Container intervalPanel) {
			JCheckBox roundRobinCheckBox = new JCheckBox();
			roundRobinCheckBox.setText(ROUND_ROBIN + " (A-B-A-B-A-B-A-B...)");
			roundRobinCheckBox.setName(ROUND_ROBIN);

			Boolean isChecked = (Boolean) current.getParameter(5).getValue();
			roundRobinCheckBox.setSelected(isChecked.booleanValue());
			roundRobinCheckBox.addActionListener(new RoundRobinAdapter());

			// the checkbox will be aligned to the left
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			c.weighty = 0.0;

			// Add the distribution
			intervalPanel.add(roundRobinCheckBox, c);
		}

	}

	/**
	 * Table for showing and editing a matrix parameter
	 */
	private class ParameterTable extends ExactTable {

		private static final long serialVersionUID = 1L;

		public ParameterTable(Parameter parameter) {
			super(new ParameterTableModel(parameter));
			setDefaultRenderer(Object.class, new ParameterCellRenderer());
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			setRowHeight(CommonConstants.ROW_HEIGHT);
			setRowSelectionAllowed(true);
			setColumnSelectionAllowed(true);
			setBatchEditingEnabled(true);
		}

	}

	/**
	 * Model backing the parameter table
	 */
	private class ParameterTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		private Parameter parameter;

		public ParameterTableModel(Parameter parameter) {
			rowHeaderPrototype = "0000.0000";
			prototype = "0000.0000";
			this.parameter = parameter;
		}

		@Override
		public int getRowCount() {
			return ((Object[][]) parameter.getValue()).length;
		}

		@Override
		public int getColumnCount() {
			return ((Object[][]) parameter.getValue())[0].length;
		}

		@Override
		protected Object getRowName(int rowIndex) {
			if (((Object[][]) parameter.getValue()).length <= 1) {
				return "--";
			} else {
				return String.valueOf(rowIndex + 1);
			}
		}

		@Override
		public String getColumnName(int columnIndex) {
			return String.valueOf(columnIndex + 1);
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		protected Object getValueAtImpl(int rowIndex, int columnIndex) {
			return ((Object[][]) parameter.getValue())[rowIndex][columnIndex];
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			Object newVal = parameter.parseValue((String) value);
			if (newVal == null) {
				return;
			}
			Object[][] matrix = (Object[][]) parameter.getValue();
			ValueChecker checker = parameter.getValueChecker();
			Object oldVal = matrix[rowIndex][columnIndex];
			matrix[rowIndex][columnIndex] = newVal;
			if (checker != null && !checker.checkValue(matrix)) {
				matrix[rowIndex][columnIndex] = oldVal;
			}
		}

		@Override
		public void clear(int row, int col) {
			Object[][] matrix = (Object[][]) parameter.getValue();
			matrix[row][col] = parameter.parseValue("0");
		}

	}

	/**
	 * Renderer displaying cells of the parameter table
	 */
	private class ParameterCellRenderer extends ExactCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		protected String formatNumber(double d) {
			return current.formatNumber(d);
		}

	}

}
