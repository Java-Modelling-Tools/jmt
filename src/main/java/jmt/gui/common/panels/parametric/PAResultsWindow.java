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

package jmt.gui.common.panels.parametric;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import jmt.framework.gui.components.JMTFrame;
import jmt.framework.gui.graph.MeasureValue;
import jmt.framework.gui.graph.PAPlot;
import jmt.framework.gui.layouts.SpringUtilities;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.definitions.MeasureDefinition;
import jmt.gui.common.definitions.PAResultsModel;
import jmt.gui.common.definitions.ResultsConstants;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.parametric.ArrivalRateParametricAnalysis;
import jmt.gui.common.definitions.parametric.NumberOfCustomerParametricAnalysis;
import jmt.gui.common.definitions.parametric.ParametricAnalysis;
import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;
import jmt.gui.common.definitions.parametric.PopulationMixParametricAnalysis;
import jmt.gui.common.definitions.parametric.SeedParametricAnalysis;
import jmt.gui.common.definitions.parametric.ServiceTimesParametricAnalysis;
import jmt.gui.exact.table.ExactTable;
import jmt.gui.exact.table.ExactTableModel;

/**
 * <p>Title: PAResultsWindow </p>
 * <p>Description: this is a JFrame used to show results from a parametric analysis simulation.
 * Performance indices are grouped by type inside a JTabbedPane. Performance indices of the
 * same type are showed one below the other. <br>
 * It contains some subclasses used for several purpose: <br>
 *   - <code>PAMeasurePanel</code>: it is a JPanel that contains the table with results and the
 *                     plot <br>
 *   - <code>ZoomedFrame</code>: it is a JFrame used to show a single performance index <br>
 *   - <code>PlotImagesFileChooser</code>: a file chooser used to save plot images <br>
 *   - <code>PlotImagesFileFilter</code>: a file filter used to filter unsupported image type <br>
 *   - <code>PlotPopupMenu</code>: a JPopupMenu used to zoom inside and outside the plot and save images <br></p>
 *
 * @author Francesco D'Aquino
 *         Date: 28-gen-2006
 *         Time: 10.48.11
 *       
 * Modified by Ashanka (Nov 09):
 * Desc: Added the description of the Drop Rate
 * 
 * Modified by Ashanka (Nov 09):
 * Desc: Appended the values of various measures to the tool tip.
 * 
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public class PAResultsWindow extends JMTFrame implements ResultsConstants, ParametricAnalysis {

	private static final long serialVersionUID = 1L;

	private static final String TITLE = "Simulation Results";
	public static final int SPINNER_WIDTH = 65;
	public static final double PLOT_ZOOM_FACTOR = 0.5;
	public static final double SPINNER_PRECISION = 0.001;

	private SimulationDefinition sd;
	private ParametricAnalysisDefinition pad;
	private PAResultsModel results;

	/**
	 * Creates a new PAResultsWindow
	 * @param sd simulation definition data structure
	 * @param fileName name of simulation file
	 */
	public PAResultsWindow(SimulationDefinition sd, String fileName) {
		this.sd = sd;
		this.pad = sd.getParametricAnalysisModel();
		this.results = (PAResultsModel) sd.getSimulationResults();
		this.updateTitle(fileName);
		initGUI();
	}

	/**
	 * Initialize all gui related stuff
	 */
	private void initGUI() {
		// Sets default title, close operation and dimensions
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.setIconImage(JMTImageLoader.loadImage("Results").getImage());
		this.centerWindow(CommonConstants.MAX_GUI_WIDTH_WHATIF_RESULTS, CommonConstants.MAX_GUI_HEIGHT_WHATIF_RESULTS);

		// Creates all tabs
		JTabbedPane mainPanel = new JTabbedPane();
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		addTabPane(mainPanel, SimulationDefinition.MEASURE_QL, DESCRIPTION_QUEUE_LENGTH, results.getQueueLengthMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_QT, DESCRIPTION_QUEUE_TIME, results.getQueueTimeMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_RP, DESCRIPTION_RESPONSE_TIME, results.getResponseTimeMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_RD, DESCRIPTION_RESIDENCE_TIME, results.getResidenceTimeMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_U, DESCRIPTION_UTILIZATION, results.getUtilizationMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_X, DESCRIPTION_THROUGHPUT, results.getThroughputMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_DR, DESCRIPTION_DROP_RATE, results.getDropRateMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_BR, DESCRIPTION_BALKING_RATE, results.getBalkingRateMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_RR, DESCRIPTION_RENEGING_RATE, results.getRenegingRateMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_R, DESCRIPTION_RETRIAL_ATTEMPTS_RATE, results.getRetrialAttemptsRateMeasures());
		//addTabPane(mainPanel, SimulationDefinition.MEASURE_BS, DESCRIPTION_BUSY_SERVER_NUMBER, results.getNumberOfBusyServersMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_OT, DESCRIPTION_RETRIAL_ORBIT_TIME, results.getWaitingTimeMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_RS, DESCRIPTION_RETRIAL_ORBIT_SIZE, results.getRetrialOrbitSizeMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_S_CN, DESCRIPTION_SYSTEM_CUSTOMER_NUMBER, results.getSystemCustomerNumberMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_S_RP, DESCRIPTION_SYSTEM_RESPONSE_TIME, results.getSystemResponseTimeMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_S_X, DESCRIPTION_SYSTEM_THROUGHPUT, results.getSystemThroughputMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_S_DR, DESCRIPTION_SYSTEM_DROP_RATE, results.getSystemDropRateMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_S_BR, DESCRIPTION_SYSTEM_BALKING_RATE, results.getSystemBalkingRateMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_S_RR, DESCRIPTION_SYSTEM_RENEGING_RATE, results.getSystemRenegingRateMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_S_P, DESCRIPTION_SYSTEM_POWER, results.getSystemPowerMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_S_R, DESCRIPTION_SYSTEM_RETRIAL_ATTEMPTS_RATE, results.getSystemRetrialAttemptsRateMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_RP_PER_SINK, DESCRIPTION_RESPONSE_TIME_PER_SINK, results.getResponsetimePerSinkMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_X_PER_SINK, DESCRIPTION_THROUGHPUT_PER_SINK, results.getThroughputPerSinkMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_FCR_TW, DESCRIPTION_FCR_TOTAL_WEIGHT, results.getFCRTotalWeightMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_FCR_MO, DESCRIPTION_FCR_MEMORY_OCCUPATION, results.getFCRMemoryOccupationMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_FJ_CN, DESCRIPTION_FJ_CUSTOMER_NUMBER, results.getFJCustomerNumberMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_FJ_RP, DESCRIPTION_FJ_RESPONSE_TIME, results.getFJResponseTimeMeasures());
		addTabPane(mainPanel, SimulationDefinition.MEASURE_FX, DESCRIPTION_FIRING_THROUGHPUT, results.getFiringThroughputMeasures());
	}

	public String getXLabel() {
		if (pad instanceof NumberOfCustomerParametricAnalysis) {
			return "N";
		} else if (pad instanceof PopulationMixParametricAnalysis) {
			return "ß of " + pad.getReferenceClassName();
		} else if (pad instanceof SeedParametricAnalysis) {
			return "Step";
		} else if (pad instanceof ArrivalRateParametricAnalysis) {
			ArrivalRateParametricAnalysis arpa = (ArrivalRateParametricAnalysis) pad;
			if (arpa.isSingleClass()) {
				return arpa.getReferenceClassName() + " arrival rate [j/s]";
			} else {
				return "Ratio between assumed arrival rate and the initial one [%]";
			}
		} else if (pad instanceof ServiceTimesParametricAnalysis) {
			ServiceTimesParametricAnalysis stpa = (ServiceTimesParametricAnalysis) pad;
			if (stpa.isSingleClass()) {
				return stpa.getReferenceClassName() + " service time at " + stpa.getReferenceStationName() + " [s]";
			} else {
				return "Ratio between assumed service time at " + stpa.getReferenceStationName() + " and the initial one [%]";
			}
		} else {
			return "Not defined";
		}
	}

	public String getYLabel(int measureIndex) {
		String measureType = results.getMeasureType(measureIndex);
		String suffix = null;
		if (measureType.equals(SimulationDefinition.MEASURE_QL) || measureType.equals(SimulationDefinition.MEASURE_S_CN)
				|| measureType.equals(SimulationDefinition.MEASURE_FJ_CN)) {
			suffix = " (j)";
		} else if (measureType.equals(SimulationDefinition.MEASURE_QT) || measureType.equals(SimulationDefinition.MEASURE_RP)
				|| measureType.equals(SimulationDefinition.MEASURE_RD) || measureType.equals(SimulationDefinition.MEASURE_S_RP)
				|| measureType.equals(SimulationDefinition.MEASURE_RP_PER_SINK) || measureType.equals(SimulationDefinition.MEASURE_FJ_RP)) {
			suffix = " (s)";
		} else if (measureType.equals(SimulationDefinition.MEASURE_X) || measureType.equals(SimulationDefinition.MEASURE_DR)
				|| measureType.equals(SimulationDefinition.MEASURE_S_X) || measureType.equals(SimulationDefinition.MEASURE_S_DR)
				|| measureType.equals(SimulationDefinition.MEASURE_X_PER_SINK) || measureType.equals(SimulationDefinition.MEASURE_S_R)
				|| measureType.equals(SimulationDefinition.MEASURE_R) || measureType.equals(SimulationDefinition.MEASURE_RR)
				|| measureType.equals(SimulationDefinition.MEASURE_BR) || measureType.equals(SimulationDefinition.MEASURE_S_RR)
				|| measureType.equals(SimulationDefinition.MEASURE_S_BR)) {
			suffix = " (j/s)";
		} else if (measureType.equals(SimulationDefinition.MEASURE_S_P)) {
			suffix = " (j/s^2)";
		} else if (measureType.equals(SimulationDefinition.MEASURE_FX)) {
			suffix = " (f/s)";
		} else {
			suffix = "";
		}
		String name = results.getName(measureIndex) + suffix;
		name = name.replaceFirst("Network_", "");
		name = name.replaceFirst("All classes_", "");
		name = name.replaceFirst("All modes_", "");
		return name;
	}

	/**
	 * Creates a new tabbed pane that shows specified measures and adds it to
	 * specified JTabPane. If measures indices vector is null or empty no panel is added.
	 * @param parent panel where newly created tab should be added
	 * @param name name of the panel to be added
	 * @param description description to be shown into the panel
	 * @param indices array with all measures indices to be shown in this panel
	 */
	private void addTabPane(JTabbedPane parent, String name, String description, int[] indices) {
		// If no measure are present, do not add corresponding tab
		if (indices != null && indices.length > 0) {
			JPanel tabPanel = new JPanel(new BorderLayout());
			// Adds margins
			tabPanel.add(Box.createVerticalStrut(BORDERSIZE), BorderLayout.NORTH);
			tabPanel.add(Box.createVerticalStrut(BORDERSIZE), BorderLayout.SOUTH);
			tabPanel.add(Box.createHorizontalStrut(BORDERSIZE), BorderLayout.WEST);
			tabPanel.add(Box.createHorizontalStrut(BORDERSIZE), BorderLayout.EAST);
			JPanel mainpanel = new JPanel(new BorderLayout());
			tabPanel.add(mainpanel, BorderLayout.CENTER);
			// Adds tab description
			JPanel upperPanel = new JPanel(new BorderLayout());
			JLabel descrLabel = new JLabel(description);
			upperPanel.add(descrLabel, BorderLayout.NORTH);
			upperPanel.add(Box.createVerticalStrut(BORDERSIZE / 2), BorderLayout.SOUTH);
			mainpanel.add(upperPanel, BorderLayout.NORTH);
			// Adds panel with measures
			JPanel scroll = new JPanel(new GridLayout(indices.length, 1, 1, 1));
			// Adds all measures to this panel
			for (int index : indices) {
				scroll.add(new PAMeasurePanel(results, index));//,thisMeasureValues));
			}
			mainpanel.add(new JScrollPane(scroll), BorderLayout.CENTER);

			// Adds tab to parent tabbed pane
			parent.addTab(name, tabPanel);
		}
	}

	/**
	 * Inner class to create a panel that holds all the computed performance indices
	 */
	protected class PAMeasurePanel extends JPanel implements PlotContainer {

		private static final long serialVersionUID = 1L;

		protected PAResultsModel rm;
		protected int measureIndex;
		protected Vector values;
		protected ZoomedFrame zoomedFrame;
		protected JCheckBox boundsEnabler;
		protected JSpinner xMin;
		protected JSpinner xMax;
		protected JSpinner yMin;
		protected JSpinner yMax;
		protected PAPlot graph;
		protected double XMIN;
		protected double XMAX;
		protected double YMIN;
		protected double YMAX;

		public PAMeasurePanel(PAResultsModel rm, int measureIndex) {
			this.rm = rm;
			this.measureIndex = measureIndex;
			this.values = rm.getValues(measureIndex);
			createPanel();
			addListeners();
		}

		/**
		 * Used to create the panel holding all measure's data
		 */
		protected void createPanel() {
			this.setLayout(new BorderLayout(20, 20));
			this.setBorder(BorderFactory.createRaisedBevelBorder());

			//Adds mainPanel with all informations on this measure
			JLabel label;
			JTextField field;
			JPanel mainPanel = new JPanel(new SpringLayout());
			//Create graph and initialize ranges
			graph = new PAPlot(values, rm.getParameterValues(), getXLabel(), getYLabel(measureIndex));
			graph.drawPlot(true);
			XMIN = graph.getPlotXMin();
			XMAX = graph.getPlotXMax();
			//If simulation was stopped and only one result was calculated the XMIN and XMAX
			//may equal, the same for YMIN and YMAX. In this case adjust the XMIN, XMAX, YMIN,
			//and YMAX
			if (XMIN == XMAX) {
				if (XMIN >= SPINNER_PRECISION) {
					XMIN -= SPINNER_PRECISION;
				} else {
					XMIN = 0;
				}
				XMAX += SPINNER_PRECISION;
			}
			YMIN = graph.getPlotYMin();
			YMAX = graph.getPlotYMax();
			if (YMIN == YMAX) {
				if (YMIN >= SPINNER_PRECISION) {
					YMIN -= SPINNER_PRECISION;
				} else {
					YMIN = 0;
				}
				YMAX += SPINNER_PRECISION;
			}
			graph.setXRange(XMIN, XMAX);
			graph.setYRange(YMIN, YMAX);
			// Station name
			label = new JLabel();
			if (sd.isFCRMeasure(rm.getMeasureType(measureIndex))) {
				label.setText("Region Name: ");
			} else {
				label.setText("Station Name: ");
			}
			field = new JTextField();
			field.setEditable(false);
			field.setMaximumSize(new Dimension(field.getMaximumSize().width, field.getMinimumSize().height));
			label.setLabelFor(field);
			String stationName = rm.getStationName(measureIndex);
			if (sd.isFJMeasure(rm.getMeasureType(measureIndex))) {
				field.setText(stationName + " (Fork Join)");
			} else {
				field.setText(stationName);
			}
			field.setToolTipText("Name of the station: " + field.getText());
			mainPanel.add(label);
			mainPanel.add(field);
			// Class name
			label = new JLabel();
			if (rm.getMeasureType(measureIndex).equals(SimulationDefinition.MEASURE_FX)) {
				label.setText("Mode Name: ");
			} else {
				label.setText("Class Name: ");
			}
			field = new JTextField();
			field.setEditable(false);
			field.setMaximumSize(new Dimension(field.getMaximumSize().width, field.getMinimumSize().height));
			label.setLabelFor(field);
			field.setText(rm.getClassName(measureIndex));
			if (rm.getMeasureType(measureIndex).equals(SimulationDefinition.MEASURE_FX)) {
				field.setToolTipText("Name of the mode: " + field.getText());
			} else {
				field.setToolTipText("Name of the class: " + field.getText());
			}
			mainPanel.add(label);
			mainPanel.add(field);
			// Alpha/Precision
			label = new JLabel("Conf.Int. / Max Rel.Err. (0-1): ");
			field = new JTextField();
			field.setEditable(false);
			field.setMaximumSize(new Dimension(field.getMaximumSize().width, field.getMinimumSize().height));
			label.setLabelFor(field);
			field.setText(rm.getAlpha(measureIndex) + " / " + rm.getPrecision(measureIndex)); // AnalyzedSamples
			field.setToolTipText("Confidence Interval and Maximum Relative Error requested for this measure: " + field.getText());
			mainPanel.add(label);
			mainPanel.add(field);
			// Models
			label = new JLabel("Models: ");
			JTextField samples = new JTextField();
			samples.setEditable(false);
			samples.setMaximumSize(new Dimension(samples.getMaximumSize().width, samples.getMinimumSize().height));
			label.setLabelFor(samples);
			samples.setText("" + values.size());
			samples.setToolTipText("Number of models: " + samples.getText());
			mainPanel.add(label);
			mainPanel.add(samples);
			//xMin
			label = new JLabel("X min:");
			if (XMIN >= SPINNER_PRECISION) {
				xMin = new JSpinner(new SpinnerNumberModel(XMIN, XMIN - SPINNER_PRECISION, XMAX, SPINNER_PRECISION));
			} else {
				xMin = new JSpinner(new SpinnerNumberModel(XMIN, 0, XMAX, SPINNER_PRECISION));
			}
			xMin.setToolTipText("Sets the smallest x represented");
			xMin.setMaximumSize(new Dimension(40, 18));
			label.setLabelFor(xMin);
			mainPanel.add(label);
			mainPanel.add(xMin);
			//xMax
			label = new JLabel("X max:");
			xMax = new JSpinner(new SpinnerNumberModel(XMAX, XMIN, XMAX + SPINNER_PRECISION, SPINNER_PRECISION));
			xMax.setToolTipText("Sets the largest x represented");
			xMax.setMaximumSize(new Dimension(40, 18));
			label.setLabelFor(xMax);
			mainPanel.add(label);
			mainPanel.add(xMax);
			//yMin
			label = new JLabel("Y min:");
			if (YMIN >= SPINNER_PRECISION) {
				yMin = new JSpinner(new SpinnerNumberModel(YMIN, YMIN - SPINNER_PRECISION, YMAX, SPINNER_PRECISION));
			} else {
				yMin = new JSpinner(new SpinnerNumberModel(YMIN, 0, YMAX, SPINNER_PRECISION));
			}
			yMin.setToolTipText("Sets the smallest y represented");
			yMin.setMaximumSize(new Dimension(40, 18));
			label.setLabelFor(yMin);
			mainPanel.add(label);
			mainPanel.add(yMin);
			//yMax
			label = new JLabel("Y max:");
			yMax = new JSpinner(new SpinnerNumberModel(YMAX, YMIN, YMAX + SPINNER_PRECISION, SPINNER_PRECISION));
			yMax.setToolTipText("Sets the largest y represented");
			yMax.setMaximumSize(new Dimension(40, 18));
			label.setLabelFor(yMax);
			mainPanel.add(label);
			mainPanel.add(yMax);
			//Bounds enabler
			boundsEnabler = new JCheckBox("Show confidence interval range");
			boundsEnabler.setToolTipText("Enable or disable bounds representation");
			boundsEnabler.setSelected(true);
			boundsEnabler.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (boundsEnabler.isSelected()) {
						graph.clear();
						graph.drawPlot(true);
					} else {
						graph.clear();
						graph.drawPlot(false);
					}
					graph.repaint();
				}
			});

			SpringUtilities.makeCompactGrid(mainPanel, 8, 2, //rows, cols
					20, 10, //initX, initY
					16, 10);//xPad, yPad

			JLabel mainTitle = new JLabel("<html><b>Description</b></html>");
			JPanel mainTitlePanel = new JPanel();
			mainTitlePanel.add(mainTitle);
			JPanel tempMainPanel = new JPanel(new BorderLayout());
			tempMainPanel.add(mainTitlePanel, BorderLayout.NORTH);
			tempMainPanel.add(mainPanel, BorderLayout.CENTER);

			JLabel graphTitle = new JLabel("<html><b>Plot</b></html>");
			JPanel graphTitlePanel = new JPanel();
			graphTitlePanel.add(graphTitle);
			JPanel graphPanel = new JPanel(new BorderLayout());
			graph.add(boundsEnabler);
			graphPanel.add(graph);
			JPanel tempGraphPanel = new JPanel(new BorderLayout());
			tempGraphPanel.add(graphTitlePanel, BorderLayout.NORTH);
			tempGraphPanel.add(graphPanel, BorderLayout.CENTER);

			JPanel upperPanel = new JPanel(new BorderLayout(10, 10));
			upperPanel.setBorder(new EmptyBorder(20, 0, 0, 20));
			upperPanel.add(tempMainPanel, BorderLayout.WEST);
			upperPanel.add(tempGraphPanel, BorderLayout.CENTER);

			JLabel tableTitle = new JLabel("<html><b>Simulation Results</b></html>");
			JPanel tableTitlePanel = new JPanel();
			tableTitlePanel.add(tableTitle);
			ValuesTable table = new ValuesTable(values, rm.getMeasureType(measureIndex));
			JScrollPane tablePanel = new JScrollPane(table);
			tablePanel.setBorder(new EmptyBorder(10, 0, 0, 0));
			tablePanel.setPreferredSize(new Dimension(0, 140));
			JLabel tableNote = new JLabel("<html>The values in <font color=\"red\">red</font> were not computed with the requested precision</html>");
			JPanel tableNotePanel = new JPanel();
			tableNotePanel.add(tableNote);

			JPanel lowerPanel = new JPanel(new BorderLayout());
			lowerPanel.setBorder(new EmptyBorder(0, 20, 20, 20));
			lowerPanel.add(tableTitlePanel, BorderLayout.NORTH);
			lowerPanel.add(tablePanel, BorderLayout.CENTER);
			lowerPanel.add(tableNotePanel, BorderLayout.SOUTH);

			//Add the upper and lower panels
			this.add(upperPanel, BorderLayout.CENTER);
			this.add(lowerPanel, BorderLayout.SOUTH);
		}

		/**
		 * Gives a reference to <code>this</code>
		 * @return a reference to <code>this</code> PAMeasurePanel
		 */
		public PAMeasurePanel getReference() {
			return this;
		}

		/**
		 * Gets a reference to the PAPlot contained
		 * @return a reference to the plot
		 */
		public PAPlot getPlot() {
			return graph;
		}

		/**
		 * Restores the original ranges
		 */
		public void resizePlot() {
			double[] xRange = { XMIN, XMAX };
			double[] yRange = { YMIN, YMAX };
			xMin.setValue(new Double(xRange[0]));
			xMax.setValue(new Double(xRange[1]));
			yMin.setValue(new Double(yRange[0]));
			yMax.setValue(new Double(yRange[1]));
			graph.repaint();
		}

		/**
		 * Zooms in by the factor PLOT_ZOOM_FACTOR
		 */
		public void zoomIn() {
			double[] xRange = graph.getXRange();
			double[] yRange = graph.getYRange();
			double width = xRange[1] - xRange[0];
			double height = yRange[1] - yRange[0];
			double newWidth = (xRange[1] - xRange[0]) * PLOT_ZOOM_FACTOR;
			double newHeight = (yRange[1] - yRange[0]) * PLOT_ZOOM_FACTOR;
			double newXMin = xRange[0] + (width - newWidth) / 2;
			//The next 7 lines check if the new range is valid
			if (newXMin < XMIN) {
				newXMin = XMIN;
			}
			double newXMax = xRange[0] + (width - newWidth) / 2 + newWidth;
			if (newXMax > XMAX) {
				newXMax = XMAX;
			}
			double newYMin = yRange[0] + (height - newHeight) / 2;
			if (newYMin < YMIN) {
				newYMin = YMIN;
			}
			double newYMax = yRange[0] + (height - newHeight) / 2 + newHeight;
			if (newYMax > YMAX) {
				newYMax = YMAX;
			}
			graph.setXRange(newXMin, newXMax);
			graph.setYRange(newYMin, newYMax);
			xMin.setValue(new Double(newXMin));
			xMax.setValue(new Double(newXMax));
			yMin.setValue(new Double(newYMin));
			yMax.setValue(new Double(newYMax));
			graph.repaint();
		}

		/**
		 * Zooms out by the factor PLOT_ZOOM_FACTOR
		 */
		public void zoomOut() {
			double[] xRange = graph.getXRange();
			double[] yRange = graph.getYRange();
			double width = xRange[1] - xRange[0];
			double height = yRange[1] - yRange[0];
			double newWidth = (xRange[1] - xRange[0]) * (1 / PLOT_ZOOM_FACTOR);
			double newHeight = (yRange[1] - yRange[0]) * (1 / PLOT_ZOOM_FACTOR);
			double newXMin = xRange[0] - (-width + newWidth) / 2;
			//The next 7 lines check if the new range is valid
			if (newXMin < XMIN) {
				newXMin = XMIN;
			}
			double newXMax = xRange[0] - (-width + newWidth) / 2 + newWidth;
			if (newXMax > XMAX) {
				newXMax = XMAX;
			}
			double newYMin = yRange[0] - (-height + newHeight) / 2;
			if (newYMin < YMIN) {
				newYMin = YMIN;
			}
			double newYMax = yRange[0] - (-height + newHeight) / 2 + newHeight;
			if (newYMax > YMAX) {
				newYMax = YMAX;
			}
			graph.setXRange(newXMin, newXMax);
			graph.setYRange(newYMin, newYMax);
			xMin.setValue(new Double(newXMin));
			xMax.setValue(new Double(newXMax));
			yMin.setValue(new Double(newYMin));
			yMax.setValue(new Double(newYMax));
			graph.repaint();
		}

		/**
		 * Adds listeners to components
		 */
		protected void addListeners() {
			graph.addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					double[] xRange = graph.getXRange();
					double[] yRange = graph.getYRange();
					//if the new x range is valid...
					if ((xRange[0] >= 0) && (xRange[1] <= XMAX)) {
						xMin.setValue(new Double(xRange[0]));
						xMax.setValue(new Double(xRange[1]));
					} else {
						//... else where it is not compatible restore the original value
						double min = xRange[0], max = xRange[1];
						if (min < 0) {
							min = 0;
						}
						if (max > XMAX) {
							max = XMAX;
						}
						xMin.setValue(new Double(min));
						xMax.setValue(new Double(max));
						graph.setXRange(min, max);
					}
					//the same as for the x range
					if ((yRange[0] >= 0) && (yRange[1] <= YMAX)) {
						yMin.setValue(new Double(yRange[0]));
						yMax.setValue(new Double(yRange[1]));
					} else {
						double min = yRange[0], max = yRange[1];
						if (min < 0) {
							min = 0;
						}
						if (max > YMAX) {
							max = YMAX;
						}
						yMin.setValue(new Double(min));
						yMax.setValue(new Double(max));
						graph.setYRange(min, max);
					}
					graph.repaint();
				}

				public void mouseClicked(MouseEvent e) {
					 //Show a zoomed frame when the mouse is double clicked
					if (e.getClickCount() == 2) {
						if (zoomedFrame == null) {
							zoomedFrame = new ZoomedFrame(rm, measureIndex, values, pad.getParameterValues());
						}
						zoomedFrame.show();
					}
					//Show a popup menu when the mouse is right clicked
					if (SwingUtilities.isRightMouseButton(e)) {
						PlotPopupMenu plotPopup = new PlotPopupMenu(getReference());
						plotPopup.show(graph, e.getPoint().x, e.getPoint().y);
					}
				}
			});

			xMin.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (xMin.getValue() instanceof Double) {
						double XMin = ((Double) xMin.getValue()).doubleValue();
						double[] range = graph.getXRange();
						if (XMin < range[1]) {
							//set the new range
							graph.setXRange(XMin, range[1]);
							graph.repaint();
						} else {
							xMin.setValue(Double.valueOf(range[0]));
						}
					}
				}
			});

			xMax.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (xMax.getValue() instanceof Double) {
						double XMax = ((Double) xMax.getValue()).doubleValue();
						double[] range = graph.getXRange();
						if (XMax > range[0]) {
							//set the new range
							graph.setXRange(range[0], XMax);
							graph.repaint();
						} else {
							xMax.setValue(Double.valueOf(range[1]));
						}
					}
				}
			});

			yMin.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (yMin.getValue() instanceof Double) {
						double YMin = ((Double) yMin.getValue()).doubleValue();
						double[] range = graph.getYRange();
						if (YMin < range[1]) {
							//set the new range
							graph.setYRange(YMin, range[1]);
							graph.repaint();
						} else {
							yMin.setValue(Double.valueOf(range[0]));
						}
					}
				}
			});

			yMax.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (yMax.getValue() instanceof Double) {
						double YMax = ((Double) yMax.getValue()).doubleValue();
						double[] range = graph.getYRange();
						if (YMax > range[0]) {
							//set the new range
							graph.setYRange(range[0], YMax);
							graph.repaint();
						} else {
							yMax.setValue(Double.valueOf(range[1]));
						}
					}
				}
			});
		}

	}

	/**
	 * Specifies some basic functions performed by those components that contain
	 * a Plot object
	 */
	public interface PlotContainer {

		/**
		 * Gets the PAPlot object
		 * @return  the plot
		 */
		public PAPlot getPlot();

		/**
		 * Restores the x and y ranges to their original values
		 */
		public void resizePlot();

		/**
		 * Zooms in by PLOT_ZOOM_FACTOR factor
		 */
		public void zoomIn();

		/**
		 * Zooms in by PLOT_ZOOM_FACTOR factor
		 */
		public void zoomOut();

	}

	/**
	 * A class representing a frame containing information about a single measure
	 */
	protected class ZoomedFrame extends JFrame implements PlotContainer {

		private static final long serialVersionUID = 1L;

		MeasureDefinition md;
		int measureIndex;
		Vector values;
		Vector<Number> parameterValues;
		protected JCheckBox boundsEnabler;
		protected JSpinner xMin;
		protected JSpinner xMax;
		protected JSpinner yMin;
		protected JSpinner yMax;
		protected PAPlot graph;
		protected double XMIN;
		protected double XMAX;
		protected double YMIN;
		protected double YMAX;

		public ZoomedFrame(MeasureDefinition md, int measureIndex, Vector values, Vector<Number> parameterValues) {
			super(md.getName(measureIndex));
			this.measureIndex = measureIndex;
			this.values = values;
			this.md = md;
			this.parameterValues = parameterValues;
			initialize();
			addListeners();
			this.setIconImage(JMTImageLoader.loadImage("Results").getImage());
		}

		/**
		 * Initialize all gui related stuff
		 */
		public void initialize() {
			this.getContentPane().setLayout(new BorderLayout(20, 20));

			//Adds mainPanel with all informations on this measure
			JLabel label;
			JTextField field;
			JPanel mainPanel = new JPanel(new SpringLayout());
			//Create graph and initialize ranges
			graph = new PAPlot(values, parameterValues, getXLabel(), getYLabel(measureIndex));
			graph.drawPlot(true);
			XMIN = graph.getPlotXMin();
			XMAX = graph.getPlotXMax();
			//If simulation was stopped and only one result was calculated the XMIN and YMAX
			//may equal, the same for YMIN and YMAX. In this case adjust the XMIN, XMAX, YMIN,
			//and YMAX
			if (XMIN == XMAX) {
				if (XMIN >= SPINNER_PRECISION) {
					XMIN -= SPINNER_PRECISION;
				} else {
					XMIN = 0;
				}
				XMAX += SPINNER_PRECISION;
			}
			YMIN = graph.getPlotYMin();
			YMAX = graph.getPlotYMax();
			if (YMIN == YMAX) {
				if (YMIN >= SPINNER_PRECISION) {
					YMIN -= SPINNER_PRECISION;
				} else {
					YMIN = 0;
				}
				YMAX += SPINNER_PRECISION;
			}
			graph.setXRange(XMIN, XMAX);
			graph.setYRange(YMIN, YMAX);
			// Station name
			label = new JLabel();
			if (sd.isFCRMeasure(md.getMeasureType(measureIndex))) {
				label.setText("Region Name: ");
			} else {
				label.setText("Station Name: ");
			}
			field = new JTextField();
			field.setEditable(false);
			field.setMaximumSize(new Dimension(field.getMaximumSize().width, field.getMinimumSize().height));
			label.setLabelFor(field);
			String stationName = md.getStationName(measureIndex);
			if (sd.isFJMeasure(md.getMeasureType(measureIndex))) {
				field.setText(stationName + " (Fork Join)");
			} else {
				field.setText(stationName);
			}
			field.setToolTipText("Name of the station: " + field.getText());
			mainPanel.add(label);
			mainPanel.add(field);
			// Class name
			label = new JLabel();
			if (md.getMeasureType(measureIndex).equals(SimulationDefinition.MEASURE_FX)) {
				label.setText("Mode Name: ");
			} else {
				label.setText("Class Name: ");
			}
			field = new JTextField();
			field.setEditable(false);
			field.setMaximumSize(new Dimension(field.getMaximumSize().width, field.getMinimumSize().height));
			label.setLabelFor(field);
			field.setText(md.getClassName(measureIndex));
			if (md.getMeasureType(measureIndex).equals(SimulationDefinition.MEASURE_FX)) {
				field.setToolTipText("Name of the mode: " + field.getText());
			} else {
				field.setToolTipText("Name of the class: " + field.getText());
			}
			mainPanel.add(label);
			mainPanel.add(field);
			// Alpha/Precision
			label = new JLabel("Conf.Int. / Max Rel.Err. (0-1): ");
			field = new JTextField();
			field.setEditable(false);
			field.setMaximumSize(new Dimension(field.getMaximumSize().width, field.getMinimumSize().height));
			label.setLabelFor(field);
			field.setText(md.getAlpha(measureIndex) + " / " + md.getPrecision(measureIndex)); // AnalyzedSamples
			field.setToolTipText("Confidence Interval and Maximum Relative Error requested for this measure: " + field.getText());
			mainPanel.add(label);
			mainPanel.add(field);
			// Models
			label = new JLabel("Models: ");
			JTextField samples = new JTextField();
			samples.setEditable(false);
			samples.setMaximumSize(new Dimension(samples.getMaximumSize().width, samples.getMinimumSize().height));
			label.setLabelFor(samples);
			samples.setText("" + values.size());
			samples.setToolTipText("Number of models: " + samples.getText());
			mainPanel.add(label);
			mainPanel.add(samples);
			//xMin
			label = new JLabel("X min:");
			if (XMIN >= SPINNER_PRECISION) {
				xMin = new JSpinner(new SpinnerNumberModel(XMIN, XMIN - SPINNER_PRECISION, XMAX, SPINNER_PRECISION));
			} else {
				xMin = new JSpinner(new SpinnerNumberModel(XMIN, 0, XMAX, SPINNER_PRECISION));
			}
			xMin.setToolTipText("Sets the smallest x represented");
			xMin.setMaximumSize(new Dimension(40, 18));
			label.setLabelFor(xMin);
			mainPanel.add(label);
			mainPanel.add(xMin);
			//xMax
			label = new JLabel("X max:");
			xMax = new JSpinner(new SpinnerNumberModel(XMAX, XMIN, XMAX + SPINNER_PRECISION, SPINNER_PRECISION));
			xMax.setToolTipText("Sets the largest x represented");
			xMax.setMaximumSize(new Dimension(40, 18));
			label.setLabelFor(xMax);
			mainPanel.add(label);
			mainPanel.add(xMax);
			//yMin
			label = new JLabel("Y min:");
			if (YMIN >= SPINNER_PRECISION) {
				yMin = new JSpinner(new SpinnerNumberModel(YMIN, YMIN - SPINNER_PRECISION, YMAX, SPINNER_PRECISION));
			} else {
				yMin = new JSpinner(new SpinnerNumberModel(YMIN, 0, YMAX, SPINNER_PRECISION));
			}
			yMin.setToolTipText("Sets the smallest y represented");
			yMin.setMaximumSize(new Dimension(40, 18));
			label.setLabelFor(yMin);
			mainPanel.add(label);
			mainPanel.add(yMin);
			//yMax
			label = new JLabel("Y max:");
			yMax = new JSpinner(new SpinnerNumberModel(YMAX, YMIN, YMAX + SPINNER_PRECISION, SPINNER_PRECISION));
			yMax.setToolTipText("Sets the largest y represented");
			yMax.setMaximumSize(new Dimension(40, 18));
			label.setLabelFor(yMax);
			mainPanel.add(label);
			mainPanel.add(yMax);
			//Bounds enabler
			boundsEnabler = new JCheckBox("Show confidence interval range");
			boundsEnabler.setToolTipText("Enable or disable bounds representation");
			boundsEnabler.setSelected(true);
			boundsEnabler.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (boundsEnabler.isSelected()) {
						graph.clear();
						graph.drawPlot(true);
					} else {
						graph.clear();
						graph.drawPlot(false);
					}
					graph.repaint();
				}
			});

			SpringUtilities.makeCompactGrid(mainPanel, 8, 2, //rows, cols
					20, 10, //initX, initY
					16, 10);//xPad, yPad

			JLabel mainTitle = new JLabel("<html><b>Description</b></html>");
			JPanel mainTitlePanel = new JPanel();
			mainTitlePanel.add(mainTitle);
			JPanel tempMainPanel = new JPanel(new BorderLayout());
			tempMainPanel.add(mainTitlePanel, BorderLayout.NORTH);
			tempMainPanel.add(mainPanel, BorderLayout.CENTER);

			JLabel graphTitle = new JLabel("<html><b>Plot</b></html>");
			JPanel graphTitlePanel = new JPanel();
			graphTitlePanel.add(graphTitle);
			JPanel graphPanel = new JPanel(new BorderLayout());
			graph.add(boundsEnabler);
			graphPanel.add(graph);
			JPanel tempGraphPanel = new JPanel(new BorderLayout());
			tempGraphPanel.add(graphTitlePanel, BorderLayout.NORTH);
			tempGraphPanel.add(graphPanel, BorderLayout.CENTER);

			JPanel upperPanel = new JPanel(new BorderLayout(10, 10));
			upperPanel.setBorder(new EmptyBorder(20, 0, 0, 20));
			upperPanel.add(tempMainPanel, BorderLayout.WEST);
			upperPanel.add(tempGraphPanel, BorderLayout.CENTER);

			JLabel tableTitle = new JLabel("<html><b>Simulation Results</b></html>");
			JPanel tableTitlePanel = new JPanel();
			tableTitlePanel.add(tableTitle);
			ValuesTable table = new ValuesTable(values, md.getMeasureType(measureIndex));
			JScrollPane tablePanel = new JScrollPane(table);
			tablePanel.setBorder(new EmptyBorder(10, 0, 0, 0));
			tablePanel.setPreferredSize(new Dimension(0, 140));
			JLabel tableNote = new JLabel("<html>The values in <font color=\"red\">red</font> were not computed with the requested precision</html>");
			JPanel tableNotePanel = new JPanel();
			tableNotePanel.add(tableNote);

			JPanel lowerPanel = new JPanel(new BorderLayout());
			lowerPanel.setBorder(new EmptyBorder(0, 20, 20, 20));
			lowerPanel.add(tableTitlePanel, BorderLayout.NORTH);
			lowerPanel.add(tablePanel, BorderLayout.CENTER);
			lowerPanel.add(tableNotePanel, BorderLayout.SOUTH);

			//Add the upper and lower panels
			this.add(upperPanel, BorderLayout.CENTER);
			this.add(lowerPanel, BorderLayout.SOUTH);

			//Set this frame into full screen
			this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}

		/**
		 * Gets a reference to <code>this</code>
		 * @return a reference to <code>this</code> ZoomedFrame
		 */
		public ZoomedFrame getReference() {
			return this;
		}

		/**
		 * Gets a reference to the PAPlot contained
		 * @return a reference to the plot
		 */
		public PAPlot getPlot() {
			return graph;
		}

		/**
		 * Restores the original ranges
		 */
		public void resizePlot() {
			double[] xRange = { XMIN, XMAX };
			double[] yRange = { YMIN, YMAX };
			xMin.setValue(new Double(xRange[0]));
			xMax.setValue(new Double(xRange[1]));
			yMin.setValue(new Double(yRange[0]));
			yMax.setValue(new Double(yRange[1]));
			graph.repaint();
		}

		/**
		 * Zooms in by the factor PLOT_ZOOM_FACTOR
		 */
		public void zoomIn() {
			double[] xRange = graph.getXRange();
			double[] yRange = graph.getYRange();
			double width = xRange[1] - xRange[0];
			double height = yRange[1] - yRange[0];
			double newWidth = (xRange[1] - xRange[0]) * PLOT_ZOOM_FACTOR;
			double newHeight = (yRange[1] - yRange[0]) * PLOT_ZOOM_FACTOR;
			double newXMin = xRange[0] + (width - newWidth) / 2;
			//The next 7 lines check if the new range is valid
			if (newXMin < XMIN) {
				newXMin = XMIN;
			}
			double newXMax = xRange[0] + (width - newWidth) / 2 + newWidth;
			if (newXMax > XMAX) {
				newXMax = XMAX;
			}
			double newYMin = yRange[0] + (height - newHeight) / 2;
			if (newYMin < YMIN) {
				newYMin = YMIN;
			}
			double newYMax = yRange[0] + (height - newHeight) / 2 + newHeight;
			if (newYMax > YMAX) {
				newYMax = YMAX;
			}
			graph.setXRange(newXMin, newXMax);
			graph.setYRange(newYMin, newYMax);
			xMin.setValue(new Double(newXMin));
			xMax.setValue(new Double(newXMax));
			yMin.setValue(new Double(newYMin));
			yMax.setValue(new Double(newYMax));
			graph.repaint();
		}

		/**
		 * Zooms out by the factor PLOT_ZOOM_FACTOR
		 */
		public void zoomOut() {
			double[] xRange = graph.getXRange();
			double[] yRange = graph.getYRange();
			double width = xRange[1] - xRange[0];
			double height = yRange[1] - yRange[0];
			double newWidth = (xRange[1] - xRange[0]) * (1 / PLOT_ZOOM_FACTOR);
			double newHeight = (yRange[1] - yRange[0]) * (1 / PLOT_ZOOM_FACTOR);
			double newXMin = xRange[0] - (-width + newWidth) / 2;
			//The next 7 lines check if the new range is valid
			if (newXMin < XMIN) {
				newXMin = XMIN;
			}
			double newXMax = xRange[0] - (-width + newWidth) / 2 + newWidth;
			if (newXMax > XMAX) {
				newXMax = XMAX;
			}
			double newYMin = yRange[0] - (-height + newHeight) / 2;
			if (newYMin < YMIN) {
				newYMin = YMIN;
			}
			double newYMax = yRange[0] - (-height + newHeight) / 2 + newHeight;
			if (newYMax > YMAX) {
				newYMax = YMAX;
			}
			graph.setXRange(newXMin, newXMax);
			graph.setYRange(newYMin, newYMax);
			xMin.setValue(new Double(newXMin));
			xMax.setValue(new Double(newXMax));
			yMin.setValue(new Double(newYMin));
			yMax.setValue(new Double(newYMax));
			graph.repaint();
		}

		/**
		 * Adds listeners to components
		 */
		protected void addListeners() {
			graph.addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					double[] xRange = graph.getXRange();
					double[] yRange = graph.getYRange();
					//if the new x range is valid...
					if ((xRange[0] >= 0) && (xRange[1] <= XMAX)) {
						xMin.setValue(new Double(xRange[0]));
						xMax.setValue(new Double(xRange[1]));
					} else {
						//... else where it is not compatible restore the original value
						double min = xRange[0], max = xRange[1];
						if (min < 0) {
							min = 0;
						}
						if (max > XMAX) {
							max = XMAX;
						}
						xMin.setValue(new Double(min));
						xMax.setValue(new Double(max));
						graph.setXRange(min, max);
					}
					//the same as for the x range
					if ((yRange[0] >= 0) && (yRange[1] <= YMAX)) {
						yMin.setValue(new Double(yRange[0]));
						yMax.setValue(new Double(yRange[1]));
					} else {
						double min = yRange[0], max = yRange[1];
						if (min < 0) {
							min = 0;
						}
						if (max > YMAX) {
							max = YMAX;
						}
						yMin.setValue(new Double(min));
						yMax.setValue(new Double(max));
						graph.setYRange(min, max);
					}
					graph.repaint();
				}

				public void mouseClicked(MouseEvent e) {
					//Show a popup menu when the mouse is right clicked
					if (SwingUtilities.isRightMouseButton(e)) {
						PlotPopupMenu plotPopup = new PlotPopupMenu(getReference());
						plotPopup.show(graph, e.getPoint().x, e.getPoint().y);
					}
				}
			});

			xMin.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (xMin.getValue() instanceof Double) {
						double XMin = ((Double) xMin.getValue()).doubleValue();
						double[] range = graph.getXRange();
						if (XMin < range[1]) {
							//set the new range
							graph.setXRange(XMin, range[1]);
							graph.repaint();
						} else {
							xMin.setValue(Double.valueOf(range[0]));
						}
					}
				}
			});

			xMax.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (xMax.getValue() instanceof Double) {
						double XMax = ((Double) xMax.getValue()).doubleValue();
						double[] range = graph.getXRange();
						if (XMax > range[0]) {
							//set the new range
							graph.setXRange(range[0], XMax);
							graph.repaint();
						} else {
							xMax.setValue(Double.valueOf(range[1]));
						}
					}
				}
			});

			yMin.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (yMin.getValue() instanceof Double) {
						double YMin = ((Double) yMin.getValue()).doubleValue();
						double[] range = graph.getYRange();
						if (YMin < range[1]) {
							//set the new range
							graph.setYRange(YMin, range[1]);
							graph.repaint();
						} else {
							yMin.setValue(Double.valueOf(range[0]));
						}
					}
				}
			});

			yMax.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (yMax.getValue() instanceof Double) {
						double YMax = ((Double) yMax.getValue()).doubleValue();
						double[] range = graph.getYRange();
						if (YMax > range[0]) {
							//set the new range
							graph.setYRange(range[0], YMax);
							graph.repaint();
						} else {
							yMax.setValue(Double.valueOf(range[1]));
						}
					}
				}
			});
		}

	}

	/**
	 * Inner class to show parametric analysis results
	 */
	protected class ValuesTable extends ExactTable {

		private static final long serialVersionUID = 1L;

		Vector values;
		DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();

		public ValuesTable(Vector values, String measureType) {
			super(new ValuesTableModel(values, measureType));
			this.values = values;
			autoResizeMode = AUTO_RESIZE_OFF;

			setRowSelectionAllowed(true);
			setColumnSelectionAllowed(true);
			this.setRowHeaderWidth(125);
			this.setRowHeight(CommonConstants.ROW_HEIGHT);
		}

		/**
		 * The original getCellRenderer method is overwritten, since the table
		 * displays in red the values that could not be calculated with the requested
		 * precision
		 * @param row the row of the cell
		 * @param column the column of the cell
		 * @return a the TableCellRenderer for the requested cell (row,column)
		 */
		public TableCellRenderer getCellRenderer(int row, int column) {
			dtcr.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
			//Component c = null;
			Component c;
			if (column < values.size() && !(pad.getType().equals(PA_TYPE_SEED) && column <= values.size())) {
				c = dtcr.getTableCellRendererComponent(this, values.get(column), false, false, row, column);
					if (!((PAResultsModel.MeasureValueImpl) values.get(column)).isValid()) {
						c.setForeground(Color.RED);
					} else {
						c.setForeground(Color.BLACK);
					}
			} else {
				c = dtcr.getTableCellRendererComponent(this, "-", false, false, row, column);
				c.setForeground(Color.BLACK);
			}

			return dtcr;
		}

	}

	/**
	 * Model for ValuesTable
	 * Columns represent the values assumed by the varying parameter.
	 */
	protected class ValuesTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		private DecimalFormat fourDecFormat = new DecimalFormat("0.0000");
		private DecimalFormat threeDecFormat = new DecimalFormat("0.000");
		private DecimalFormat twoDecFormat = new DecimalFormat("0.00");
		private DecimalFormat hexFormat = new DecimalFormat("0.00E0");

		private String getFormattedVal(double value) {
			if (value == 0.0) {
				return "0.0";
			} else if (Math.abs(value) >= 1e-2 && Math.abs(value) < 1e4) {
				return fourDecFormat.format(value);
			} else {
				return hexFormat.format(value);
			}
		}

		Vector values;
		String measureType;

		public ValuesTableModel(Vector values, String measureType) {
			if (pad.getType().equals(PA_TYPE_SEED)) {
				Vector newvalues = new Vector(values.size()+1);
				MeasureValue amv = results.getEmptyMeasureValue();
				
				double avgupper = 0.0;
				double avgvalue = 0.0;
				double avglower = 0.0;
				
				int samples = values.size();
		
				for (int columnIndex = 0; columnIndex < samples; columnIndex++) {				
						avgupper += ((MeasureValue) values.get(columnIndex)).getUpperBound() / samples;
						avgvalue += ((MeasureValue) values.get(columnIndex)).getMeanValue() / samples;
						avglower += ((MeasureValue) values.get(columnIndex)).getLowerBound() / samples;
				}
				
				((PAResultsModel.MeasureValueImpl)amv).setUpperBound(avgupper);
				((PAResultsModel.MeasureValueImpl)amv).setMeanValue(avgvalue);
				((PAResultsModel.MeasureValueImpl)amv).setLowerBound(avglower);
				
				newvalues.add(0, (MeasureValue) amv);
				for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
					MeasureValue mv = (MeasureValue) values.get(columnIndex);
					newvalues.add(columnIndex+1, mv);
				}
				this.values = newvalues;
			} else {
				this.values = values;
			}
			this.measureType = measureType;
			prototype = "XXXX.XXX XXX";
			rowHeaderPrototype = "Upper bound";
		}

		public Object getPrototype(int i) {
			if (i == -1) {
				return rowHeaderPrototype;
			} else {
				return prototype;
			}
		}

		public int getRowCount() {
			return 3;
		}

		/**
		 * @return the object at (rowIndex, columnIndex)
		 */
		protected Object getValueAtImpl(int rowIndex, int columnIndex) {
			String toReturn;
			//if the value that should be contained in the requested cell is available
			if (columnIndex < values.size()) {
				//upper bound cells
				if (rowIndex == 1) {
					double upper = ((MeasureValue) values.get(columnIndex)).getUpperBound();
					double value = ((MeasureValue) values.get(columnIndex)).getMeanValue();
					//if the upper bound is 0 but not the value, or if the upperbound is infinite
					//show only a "-"
					if (((upper == 0) && (value != 0)) || (Double.isInfinite(upper))) {
						toReturn = "-";
					}
					//else show the formatted value
					else {
						toReturn = getFormattedVal(upper);
					}
				}
				//value cells, always show the formatted value
				else if (rowIndex == 0) {
					double value = ((MeasureValue) values.get(columnIndex)).getMeanValue();
					toReturn = getFormattedVal(value);
				}
				//lower bound cells
				else {
					double upper = ((MeasureValue) values.get(columnIndex)).getUpperBound();
					double lower = ((MeasureValue) values.get(columnIndex)).getLowerBound();
					double value = ((MeasureValue) values.get(columnIndex)).getMeanValue();
					//as for upper bounds, show a value only if it is meaningful
					if (((upper == 0) && (value != 0)) || (Double.isInfinite(upper))) {
						toReturn = "-";
					} else {
						toReturn = getFormattedVal(lower);
					}
				}
			} else {
				toReturn = "-";
			}
			return toReturn;
		}

		public int getColumnCount() {
			if (pad.getType().equals(PA_TYPE_SEED)) {
				return 1+pad.getNumberOfSteps();
			} else {
				return pad.getNumberOfSteps();
			}
		}

		/**
		 * @return the header for row <code>rowIndex</code>
		 */
		protected Object getRowName(int rowIndex) {
			String suffix = null;
			if (measureType.equals(SimulationDefinition.MEASURE_QL) || measureType.equals(SimulationDefinition.MEASURE_S_CN)
					|| measureType.equals(SimulationDefinition.MEASURE_FJ_CN)) {
				suffix = " (j)";
			} else if (measureType.equals(SimulationDefinition.MEASURE_QT) || measureType.equals(SimulationDefinition.MEASURE_RP)
					|| measureType.equals(SimulationDefinition.MEASURE_RD) || measureType.equals(SimulationDefinition.MEASURE_S_RP)
					|| measureType.equals(SimulationDefinition.MEASURE_RP_PER_SINK) || measureType.equals(SimulationDefinition.MEASURE_FJ_RP)) {
				suffix = " (s)";
			} else if (measureType.equals(SimulationDefinition.MEASURE_X) || measureType.equals(SimulationDefinition.MEASURE_DR)
					|| measureType.equals(SimulationDefinition.MEASURE_S_X) || measureType.equals(SimulationDefinition.MEASURE_S_DR)
					|| measureType.equals(SimulationDefinition.MEASURE_X_PER_SINK)) {
				suffix = " (j/s)";
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_P)) {
				suffix = " (j/s^2)";
			} else if (measureType.equals(SimulationDefinition.MEASURE_FX)) {
				suffix = " (f/s)";
			} else {
				suffix = "";
			}
			if (rowIndex == 0) {
				return ("Mean Value" + suffix);
			} else if (rowIndex == 1) {
				return ("Max" + suffix + " (Conf.Int.)");
			} else {
				return ("Min" + suffix + " (Conf.Int.)");
			}
		}

		/**
		 * Gets the name of the column, given the column index
		 * @param index the index of the column to give the name
		 * @return the column name
		 */
		public String getColumnName(int index) {
			String columnName = "NA";
			
			//if single class return the value, else return the percentage
			if (pad.getType().equals(PA_TYPE_ARRIVAL_RATE)) {
				ArrivalRateParametricAnalysis arpa = (ArrivalRateParametricAnalysis) pad;
				Vector<Number> assumedValues = arpa.getParameterValues();
				if (arpa.isSingleClass()) {
					Object temp = assumedValues.get(index);
					double val = ((Double) temp).doubleValue();
					columnName = threeDecFormat.format(val) + " j/s";
				} else {
					Object temp = assumedValues.get(index);
					double val = ((Double) temp).doubleValue();
					columnName = twoDecFormat.format(val) + " %";
				}
			}
			//if single class return the value, else return the percentage
			else if (pad.getType().equals(PA_TYPE_SERVICE_TIMES)) {
				ServiceTimesParametricAnalysis stpa = (ServiceTimesParametricAnalysis) pad;
				Vector<Number> assumedValues = stpa.getParameterValues();
				if (stpa.isSingleClass()) {
					Object temp = assumedValues.get(index);
					double val = ((Double) temp).doubleValue();
					columnName = threeDecFormat.format(val) + " s";
				} else {
					Object temp = assumedValues.get(index);
					double val = ((Double) temp).doubleValue();
					columnName = twoDecFormat.format(val) + " %";
				}
			}
			//for "number of customers" return the number of customers
			else if (pad.getType().equals(PA_TYPE_NUMBER_OF_CUSTOMERS)) {
				Vector<Number> assumedValues = pad.getParameterValues();
				int val = ((Double) (assumedValues.get(index))).intValue();
				columnName = "N = " + Integer.toString(val);
			}
			//for population mix parametric analysis return the value of ß
			else if (pad.getType().equals(PA_TYPE_POPULATION_MIX)) {
				Vector<Number> assumedValues = pad.getParameterValues();
				DecimalFormat threeDec = new DecimalFormat("0.000");
				double value = ((Double) assumedValues.get(index)).doubleValue();
				columnName = "ß = " + threeDec.format(value);
			}
			//if it is a seed parametric analysis just enumerate columns
			else if (pad.getType().equals(PA_TYPE_SEED)) {
				if (index == 0) {
					columnName = new String("Average");
				} else {				
					columnName = Integer.toString(index-1);				
				}
				
				return columnName;
			}
			return columnName;
		}

	}

	/**
	 * A simple JPopupMenu used to manage operations on plot. It gives the
	 * choice to zoom in and out on the plot, restore original view and save plot to
	 * images (in EPS or PNG format)
	 */
	protected class PlotPopupMenu extends JPopupMenu {

		public JMenuItem restore;
		public JMenuItem zoomIn;
		public JMenuItem zoomOut;
		public JMenuItem saveAs;
		public PlotContainer parent;

		public PlotPopupMenu(PlotContainer parent) {
			restore = new JMenuItem("Original view");
			zoomIn = new JMenuItem("Zoom in");
			zoomOut = new JMenuItem("Zoom out");
			saveAs = new JMenuItem("Save as..");
			this.add(restore);
			this.add(zoomIn);
			this.add(zoomOut);
			this.addSeparator();
			this.add(saveAs);
			this.parent = parent;
			addListeners();
		}

		public void addListeners() {
			restore.addActionListener(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					parent.resizePlot();
				}
			});

			zoomIn.addActionListener(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					parent.zoomIn();
				}
			});

			zoomOut.addActionListener(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					parent.zoomOut();
				}
			});

			saveAs.addActionListener(new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					PlotImagesFileFilter PNGfilter = new PlotImagesFileFilter(".png", "Portable Network Graphics images");
					PlotImagesFileFilter EPSfilter = new PlotImagesFileFilter(".eps", "Encapsulated Post Script images");
					PlotImagesFileChooser fileChooser = new PlotImagesFileChooser(PNGfilter);
					fileChooser.setFileFilter(PNGfilter);
					fileChooser.addChoosableFileFilter(EPSfilter);
					int r = fileChooser.showSaveDialog((Component) parent);
					if (r == JFileChooser.APPROVE_OPTION) {
						File file = fileChooser.getSelectedFile();
						if (fileChooser.getFileFilter().equals(EPSfilter)) {
							PAPlot plot = parent.getPlot();
							try {
								FileOutputStream fileStream = new FileOutputStream(file);
								plot.export(fileStream);
								fileStream.close();
							} catch (FileNotFoundException fnf) {
								JOptionPane.showMessageDialog(fileChooser, "File not found", "JMT - Error", JOptionPane.ERROR_MESSAGE);
							} catch (IOException ioe) {
								JOptionPane.showMessageDialog(fileChooser, "I/O exception", "JMT - Error", JOptionPane.ERROR_MESSAGE);
							}
						} else {
							PAPlot plot = parent.getPlot();
							BufferedImage image = plot.exportImage();
							try {
								int targetType = BufferedImage.TYPE_INT_RGB;
								BufferedImage originalImage = convertType(image, targetType);
								ImageIO.write(originalImage, "png", file);
								ImageIO.createImageOutputStream(file).close();
							} catch (IOException fnf) {
								JOptionPane.showMessageDialog(null, "File not found");
							}
						}
					}
				}
			});
		}

		BufferedImage convertType(BufferedImage src, int targetType) {
			if (src.getType() == targetType) {
				return src;
			}
			BufferedImage tgt = new BufferedImage(src.getWidth(), src.getHeight(), targetType);
			Graphics2D g = tgt.createGraphics();
			g.drawRenderedImage(src, null);
			g.dispose();
			return tgt;
		}

	}

	/**
	 * Custom file chooser class
	 */
	protected static class PlotImagesFileChooser extends JFileChooser {

		private static final long serialVersionUID = 1L;

		protected PlotImagesFileFilter defaultFilter;

		/**
		 * Creates a File chooser in the appropriate directory user default.
		 * @param defaultFilter default file filter
		 */
		public PlotImagesFileChooser(PlotImagesFileFilter defaultFilter) {
			super(Defaults.getWorkingPath());
			this.defaultFilter = defaultFilter;
		}

		/**
		 * Overrides default method to provide a warning if saving over an existing file
		 */
		public void approveSelection() {
			// Gets the chosen file name
			String name = getSelectedFile().getName();
			String parent = getSelectedFile().getParent();
			if (getDialogType() == OPEN_DIALOG) {
				super.approveSelection();
			}
			if (getDialogType() == SAVE_DIALOG) {
				PlotImagesFileFilter used = ((PlotImagesFileFilter) this.getFileFilter());
				if (!name.toLowerCase().endsWith(used.getExtension())) {
					name = name + used.getExtension();
					setSelectedFile(new File(parent, name));
				}
				if (getSelectedFile().exists()) {
					int resultValue = JOptionPane.showConfirmDialog(this, "<html>File <font color=#0000ff>" + name
							+ "</font> already exists in this folder.<br>Do you want to replace it?</html>", "JMT - Warning",
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
					if (resultValue == JOptionPane.OK_OPTION) {
						getSelectedFile().delete();
						super.approveSelection();
					}
				} else {
					super.approveSelection();
				}
			}
		}

	}

	/**
	 * Inner class used to create simple file filters with only extension check
	 */
	protected static class PlotImagesFileFilter extends javax.swing.filechooser.FileFilter {

		private String extension;
		private String description;

		/**
		 * Creates a new filefilter with specified extension and description
		 * @param extension extension of this filter (for example ".jmt")
		 * @param description description of this filter
		 */
		public PlotImagesFileFilter(String extension, String description) {
			this.extension = extension;
			this.description = description;
		}

		/**
		 * Whether the given file is accepted by this filter.
		 */
		public boolean accept(File f) {
			String name = f.getName().toLowerCase();
			return name.endsWith(extension) || f.isDirectory();
		}

		/**
		 * The description of this filter
		 * @see javax.swing.filechooser.FileView#getName
		 */
		public String getDescription() {
			return description + " (*" + extension + ")";
		}

		/**
		 * Gets extension of this filter
		 * @return extension of this filter
		 */
		public String getExtension() {
			return extension;
		}

	}

	/**
	 * Updates this window title adding the file name
	 * @param filename the file name or null to remove it.
	 */
	public void updateTitle(String filename) {
		if (filename != null) {
			setTitle(TITLE + " - " + filename);
		} else {
			setTitle(TITLE);
		}
	}

}
