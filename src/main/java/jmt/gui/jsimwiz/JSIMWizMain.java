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

package jmt.gui.jsimwiz;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import jmt.framework.gui.components.JMTMenuBar;
import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.listeners.AbstractJMTAction;
import jmt.framework.gui.listeners.MenuAction;
import jmt.framework.gui.wizard.Wizard;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.controller.DispatcherThread;
import jmt.gui.common.controller.ModelChecker;
import jmt.gui.common.controller.PADispatcherThread;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.GuiInterface;
import jmt.gui.common.definitions.ModelConverter;
import jmt.gui.common.definitions.ResultsModel;
import jmt.gui.common.editors.DefaultsEditor;
import jmt.gui.common.panels.AboutDialogFactory;
import jmt.gui.common.panels.MeasurePanel;
import jmt.gui.common.panels.ResultsWindow;
import jmt.gui.common.panels.SimulationPanel;
import jmt.gui.common.panels.StationParameterPanel;
import jmt.gui.common.panels.WarningWindow;
import jmt.gui.common.panels.parametric.PAProgressWindow;
import jmt.gui.common.panels.parametric.PAResultsWindow;
import jmt.gui.common.panels.parametric.ParametricAnalysisPanel;
import jmt.gui.common.xml.ModelLoader;
import jmt.gui.common.xml.XMLWriter;
import jmt.gui.jsimwiz.definitions.JSIMModel;
import jmt.gui.jsimwiz.panels.BlockingRegionsPanel;
import jmt.gui.jsimwiz.panels.StationParametersPanel;
import jmt.gui.jsimwiz.panels.ClassesPanel;
import jmt.gui.jsimwiz.panels.ConnectionsPanel;
import jmt.gui.jsimwiz.panels.JSimProblemsWindow;
import jmt.gui.jsimwiz.panels.RSPLPanel;
import jmt.gui.jsimwiz.panels.StationsPanel;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.manual.ChapterIdentifier;
import jmt.manual.PDFViewer;

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 18-lug-2005
 * Time: 8.59.44
 * Modified by Bertoli Marco
 *
 * Modified by Francesco D'Aquino
 */
public class JSIMWizMain extends Wizard implements GuiInterface {

	private static final long serialVersionUID = 1L;

	private static final String WINDOW_TITLE = "JSIMwiz - Textual Queueing Network and Petri Net Simulator";

	private CommonModel model = new JSIMModel();
	private File currentFile = null;
	private JFrame resultsWindow;
	private DispatcherThread dispatcher;

	private ModelLoader modelLoader = new ModelLoader(ModelLoader.JSIM, ModelLoader.JSIM_SAVE);

	private ClassesPanel classes;
	private StationsPanel stations;
	private ConnectionsPanel connections;
	private StationParametersPanel parameters;
	private MeasurePanel measures;
	private RSPLPanel rspl;
	private SimulationPanel simulation;
	private BlockingRegionsPanel blocking;

	private ModelChecker mc;
	private JSimProblemsWindow pw;
	private PADispatcherThread batchThread;
	private PAProgressWindow progressWindow;
	private ParametricAnalysisPanel parametricAnalysis;

	private AbstractJMTAction FILE_NEW = new AbstractJMTAction("New") {

		private static final long serialVersionUID = 1L;
		{
			putValue(Action.SHORT_DESCRIPTION, "Create a new model");
			setIcon("New", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		}

		public void actionPerformed(ActionEvent e) {
			newFile();
		}

	};

	private AbstractJMTAction FILE_OPEN = new AbstractJMTAction("Open") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Open a saved model");
			setIcon("Open", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		}

		public void actionPerformed(ActionEvent e) {
			openFile(null);
		}

	};

	private AbstractJMTAction FILE_SAVE = new AbstractJMTAction("Save") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Save this model");
			setIcon("Save", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		}

		public void actionPerformed(ActionEvent e) {
			saveFile();
		}

	};

	private AbstractJMTAction FILE_SAVEAS = new AbstractJMTAction("Save as...") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Save this model as...");
			setIcon("Save", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
		}

		public void actionPerformed(ActionEvent e) {
			saveFileAs();
		}

	};

	private AbstractJMTAction FILE_EXIT = new AbstractJMTAction("Exit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Exit JSIMwiz");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
		}

		public void actionPerformed(ActionEvent e) {
			close();
		}

	};

	private AbstractJMTAction ACTION_SWITCH_JMVA = new AbstractJMTAction("Export to JMVA...") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Export current model to JMVA...");
			setIcon("toJMVA", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_W));
		}

		public void actionPerformed(ActionEvent e) {
			toJMVA();
		}

	};

	private AbstractJMTAction SIM_START = new AbstractJMTAction("Start Simulation") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Start simulation");
			setIcon("Sim", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_M));
		}

		public void actionPerformed(ActionEvent e) {
			startSimulation();
		}

	};

	private AbstractJMTAction SIM_PAUSE = new AbstractJMTAction("Pause Simulation") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Pause simulation");
			setIcon("Pause", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
			//start disabled
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			pauseSimulation();
		}

	};

	private AbstractJMTAction SIM_STOP = new AbstractJMTAction("Stop Simulation") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Stop simulation");
			setIcon("Stop", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
			//start disabled
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			stopSimulation();
		}

	};

	private AbstractJMTAction OPTIONS_DEFAULTS = new AbstractJMTAction("Default Parameters...") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Define default parameters");
			setIcon("Options", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
		}

		public void actionPerformed(ActionEvent e) {
			showOptions();
		}

	};

	private AbstractJMTAction HELP_SHOWHELP = new AbstractJMTAction("JSIMwiz Help") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Show JSIMwiz help");
			setIcon("Help", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		}

		public void actionPerformed(ActionEvent e) {
			Runnable r = new Runnable() {
				public void run() {
					try {
						new PDFViewer("JSIMwiz Manual", ChapterIdentifier.JSIMwiz);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			EventQueue.invokeLater(r);
		}

	};

	private AbstractJMTAction HELP_CREDITS = new AbstractJMTAction("About JSIMwiz") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "About JSIMwiz");
		}

		public void actionPerformed(ActionEvent e) {
			showCredits();
		}

	};

	public AbstractJMTAction SHOW_RESULTS = new AbstractJMTAction("Show Results") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Show simulation results");
			setIcon("Results", JMTImageLoader.getImageLoader());
			setSelectable(true); // A toggle button
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
			//start disabled
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			showResultsWindow(isSelected());
		}
	};

	private void showOptions() {
		JFrame jf = new JFrame("Defaults Editor");
		jf.setIconImage(getIconImage());
		jf.setSize(getSize());
		new DefaultsEditor(jf, DefaultsEditor.JSIM).show();
	}

	/**
	 * Open a stored model from a file
	 * <br>Author: Bertoli Marco
	 */
	private void openFile(File file) {
		if (checkForSave("<html>Save changes before opening a saved model?</html>")) {
			return;
		}
		JSIMModel tmpmodel = new JSIMModel();
		int state = modelLoader.loadModel(tmpmodel, this, file);
		if (state == ModelLoader.SUCCESS || state == ModelLoader.WARNING) {
			currentFile = modelLoader.getSelectedFile();
			// At this point loading was successful, so substitutes old model with loaded one
			model = tmpmodel;
			// Clears old resultsWindow
			if (resultsWindow != null) {
				resultsWindow.dispose();
			}
			// Clears parametric window too...
			if (progressWindow != null) {
				progressWindow.stopAnimation();
				progressWindow.dispose();
			}
			SHOW_RESULTS.setSelected(false);
			SHOW_RESULTS.setEnabled(false);
			setPanelsData(model);
			setTitle(WINDOW_TITLE + " - " + currentFile.getName());
			// If simulation results are present, adds a Result Window
			if (model.containsSimulationResults()) {
				if (model.isParametricAnalysisEnabled()) {
					this.setResultsWindow(new PAResultsWindow(model, currentFile.getName()));
					SHOW_RESULTS.setEnabled(true);
				} else {
					this.setResultsWindow(new ResultsWindow(model, currentFile.getName()));
					SHOW_RESULTS.setEnabled(true);
				}
			}
			model.resetSaveState();
			System.gc();
		} else if (state == ModelLoader.FAILURE) {
			showErrorMessage(modelLoader.getFailureMotivation());
		}

		// Shows warnings if any
		if (state == ModelLoader.WARNING) {
			new WarningWindow(modelLoader.getLastWarnings(), this, modelLoader.getInputFileFormat(), CommonConstants.JSIM).show();
		}
	}

	private void newFile() {
		if (checkForSave("<html>Save changes before creating a new model?</html>")) {
			return;
		}
		setPanelsData(new JSIMModel());
		currentFile = null;
		SHOW_RESULTS.setSelected(false);
		SHOW_RESULTS.setEnabled(false);
		setTitle(WINDOW_TITLE);
		// Disposes results (if any)
		if (resultsWindow != null) {
			resultsWindow.dispose();
		}
		if (progressWindow != null) {
			progressWindow.stopAnimation();
			progressWindow.dispose();
		}
		model.resetSaveState();
	}

	private void setPanelsData(CommonModel simModel) {
		model = simModel;
		classes.setData(model);
		stations.setData(model, model);
		connections.setData(model);
		parameters.setData(model, model);
		measures.setData(model, model, model);
		rspl.setData(model, model, model);
		simulation.setData(model, model, model);
		parametricAnalysis.setData(model, model, model);
		blocking.setData(model, model, model);
	}

	/**
	 * Saves current model
	 * <br>Author: Bertoli Marco
	 */
	private void saveFile() {
		if (currentFile == null) {
			saveFileAs();
			return;
		}
		int status = modelLoader.saveModel(model, this, currentFile);
		if (status == ModelLoader.FAILURE) {
			showErrorMessage(modelLoader.getFailureMotivation());
		}
		setTitle(WINDOW_TITLE + " - " + currentFile.getName());
		model.resetSaveState();
	}

	/**
	 * Saves current model with a new name
	 * <br>Author: Bertoli Marco
	 */
	private void saveFileAs() {
		int status = modelLoader.saveModel(model, this, null);
		if (status == ModelLoader.FAILURE) {
			showErrorMessage(modelLoader.getFailureMotivation());
		} else if (status == ModelLoader.SUCCESS) {
			currentFile = modelLoader.getSelectedFile();
			setTitle(WINDOW_TITLE + " - " + currentFile.getName());
			model.resetSaveState();
		}
	}

	/**
	 * Exits from JSIM
	 */
	@Override
	public boolean cancel() {
		if (checkForSave("<html>Save changes before closing?</html>")) {
			return false;
		}
		// Stops simulation if active
		if (SIM_STOP.isEnabled()) {
			stopSimulation();
		}
		// Disposes results window and this
		if (resultsWindow != null) {
			resultsWindow.dispose();
		}
		if (progressWindow != null) {
			progressWindow.stopAnimation();
			progressWindow.dispose();
		}
		Dimension d = getSize();
		Defaults.set("JSIMWindowWidth", String.valueOf(d.width));
		Defaults.set("JSIMWindowHeight", String.valueOf(d.height));
		Defaults.save();
		return true;
	}

	private void toJMVA() {
		mc = new ModelChecker(model, model, model, model, true);
		pw.setModelChecker(mc);
		pw.updateProblemsShown(false);
		if (!mc.isEverythingOkToJMVA()) {
			pw.show();
		} else {
			pw.setVisible(false);
			launchToJMVA();
		}
	}

	public void launchToJMVA() {
		// New Converter by Bertoli Marco
		ExactModel output = new ExactModel();
		List<String> res = ModelConverter.convertJSIMtoJMVA(model, output);
		ExactWizard jmva = new ExactWizard(output);
		// If problems are found, shows warnings
		if (res.size() > 0) {
			new WarningWindow(res, jmva, CommonConstants.JSIM, CommonConstants.JMVA).show();
		}
	}

	private void startSimulation() {
		//if simulation is not in pause state
		if (!SIM_STOP.isEnabled()) {
			// Asks for confirmation before overwriting previous simulation data
			if (model.containsSimulationResults()) {
				// Find frame to show confirm dialog
				Component parent = this;
				if (resultsWindow != null && resultsWindow.isFocused()) {
					parent = resultsWindow;
				}

				int resultValue = JOptionPane.showConfirmDialog(parent, "This operation will overwrite old simulation results. "
						+ "Continue anyway?", "JMT - Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (resultValue == JOptionPane.NO_OPTION) {
					return;
				}
			}

			// Correct eventual problems on preloading for closed classes
			model.manageJobs();
			// Removes previous ResultsWindow
			if (resultsWindow != null) {
				resultsWindow.dispose();
				SHOW_RESULTS.setEnabled(false);
			}
			mc = new ModelChecker(model, model, model, model, false);
			pw.setModelChecker(mc);
			pw.updateProblemsShown(false);
			if (!mc.isEverythingOkNormal()) {
				pw.show();
			} else {
				pw.setVisible(false);
				launchSimulation();
			}
		} else {
			if (!model.isParametricAnalysisEnabled()) {
				dispatcher.restartSimulation();
			} else {
				batchThread.restartSimulation();
			}
		}
	}

	public void launchSimulation() {
		if (!model.isParametricAnalysisEnabled()) {
			try {
				File temp = File.createTempFile("~JModelSimulation", ".xml");
				temp.deleteOnExit();
				XMLWriter.writeXML(temp, model);
				// Creates results data structure
				String logCSVDelimiter = model.getLoggingGlbParameter("delim");
				String logDecimalSeparator = model.getLoggingGlbParameter("decimalSeparator");
				model.setSimulationResults(new ResultsModel(model.getPollingInterval().doubleValue(), logCSVDelimiter, logDecimalSeparator));
				SHOW_RESULTS.setEnabled(true);
				dispatcher = new DispatcherThread(this, model);
				dispatcher.startSimulation(temp);
			} catch (Exception e) {
				handleException(e);
			}
		} else {
			SHOW_RESULTS.setEnabled(false);
			if (progressWindow == null) {
				progressWindow = new PAProgressWindow(this, SIM_START, SIM_PAUSE, SIM_STOP, model.getParametricAnalysisModel());
			}
			batchThread = new PADispatcherThread(this, model, progressWindow);
			changeSimActionsState(false, true, true);
			progressWindow.initialize(model.getParametricAnalysisModel().getNumberOfSteps());
			progressWindow.start();
			progressWindow.show();
			batchThread.start();
		}
	}

	private void pauseSimulation() {
		if (!model.isParametricAnalysisEnabled()) {
			dispatcher.pauseSimulation();
		} else {
			batchThread.pauseSimulation();
		}
	}

	private void stopSimulation() {
		if (!model.isParametricAnalysisEnabled()) {
			dispatcher.stopSimulation();
		} else {
			batchThread.stopSimulation();
		}
	}

	public void changeSimActionsState(boolean start, boolean pause, boolean stop) {
		SIM_START.setEnabled(start);
		SIM_STOP.setEnabled(stop);
		SIM_PAUSE.setEnabled(pause);
	}

	/**
	 * Shows JSIM Credits
	 * <br>Author: Bertoli Marco
	 */
	private void showCredits() {
		AboutDialogFactory.showJSIM(this);
	}

	public JSIMWizMain() {
		this(null);
	}

	public JSIMWizMain(CommonModel model) {
		Defaults.reload();
		if (model == null) {
			this.model = new JSIMModel();
		} else {
			this.model = model;
		}
		model = this.model;
		setSize(Defaults.getAsInteger("JSIMWindowWidth").intValue(), Defaults.getAsInteger("JSIMWindowHeight").intValue());
		setTitle(WINDOW_TITLE);
		setIconImage(JMTImageLoader.loadImageAwt("JSIMIcon"));
		this.centerWindow();
		classes = new ClassesPanel(model);
		stations = new StationsPanel(model, model);
		connections = new ConnectionsPanel(model);
		parameters = new StationParametersPanel(model, model);
		measures = new MeasurePanel(model, model, model);
		rspl = new RSPLPanel(model, model, model);
		simulation = new SimulationPanel(model, model, model, this);
		parametricAnalysis = new ParametricAnalysisPanel(model, model, model, this);
		blocking = new BlockingRegionsPanel(model, model, model);
		initComponents();
		mc = new ModelChecker(model, model, model, model, false);
		pw = new JSimProblemsWindow(mc, this);
	}

	private void initComponents() {
		setJMenuBar(createMenuBar());
		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		//add panels
		WizardPanel[] panels = new WizardPanel[] { classes, stations, connections, parameters, measures, rspl, blocking, simulation,
				parametricAnalysis };
		for (WizardPanel panel : panels) {
			addPanel(panel);
		}
	}

	private JMTToolBar createToolBar() {
		JMTToolBar toolbar = new JMTToolBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] items = new AbstractJMTAction[] { FILE_NEW, FILE_OPEN, FILE_SAVE, null, ACTION_SWITCH_JMVA, null, SIM_START, SIM_PAUSE,
				SIM_STOP, SHOW_RESULTS, null, OPTIONS_DEFAULTS, null, HELP_SHOWHELP };
		toolbar.populateToolbar(items);
		toolbar.setFloatable(false);
		return toolbar;
	}

	private JMTMenuBar createMenuBar() {
		JMTMenuBar menuBar = new JMTMenuBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] menus = new AbstractJMTAction[] {
				//File menu
				new MenuAction("File", new AbstractJMTAction[] { FILE_NEW, FILE_OPEN, FILE_SAVE, FILE_SAVEAS, null, FILE_EXIT }),
				//ActionMenu
				new MenuAction("Action", new AbstractJMTAction[] { ACTION_NEXT, ACTION_PREV, null, ACTION_SWITCH_JMVA }),
				//Simulation Menu
				new MenuAction("Simulation", new AbstractJMTAction[] { SIM_START, SIM_PAUSE, SIM_STOP, null, SHOW_RESULTS }),
				//Options menu
				new MenuAction("Define", new AbstractJMTAction[] { OPTIONS_DEFAULTS }),
				//Help Menu
				new MenuAction("Help", new AbstractJMTAction[] { HELP_SHOWHELP, null, HELP_CREDITS }) };
		menuBar.populateMenu(menus);
		return menuBar;
	}

	public CommonModel getModel() {
		return model;
	}

	/**
	 * Sets resultWindow to be shown. This method is used by pollerThread
	 * @param rsw window to be set as current ResultsWindow
	 */
	public void setResultsWindow(JFrame rsw) {
		this.resultsWindow = rsw;
		if (rsw instanceof ResultsWindow) {
			// Sets action for toolbar buttons
			((ResultsWindow) rsw).addButtonActions(SIM_START, SIM_PAUSE, SIM_STOP);
		} else {
			SHOW_RESULTS.setEnabled(true);
		}
		// Adds a listener that will unselect Show results button upon results window closing
		rsw.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				SHOW_RESULTS.setSelected(false);
			}
		});
	}

	/**
	 * Called when showResults action is triggered
	 * @param selected Tells if show results button is selected or not
	 */
	public void showResultsWindow(boolean selected) {
		if (selected) {
			if (resultsWindow != null) {
				resultsWindow.show();
			}
		} else {
			if (resultsWindow != null) {
				resultsWindow.hide();
			}
		}
	}

	/**
	 * Shows results window and forces show results button to be selected
	 */
	public void showResultsWindow() {
		SHOW_RESULTS.setSelected(true);
		showResultsWindow(true);
	}

	// ------------------------------ Francesco D'Aquino ---------------------------------------

	/**
	 * Shows the panel to solve a problem
	 */
	public void showRelatedPanel(int problemType, int problemSubType, Object relatedStation, Object relatedClass) {
		if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_CLASS_ERROR)) {
			tabbedPane.setSelectedIndex(0);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_STATION_ERROR)) {
			JOptionPane.showMessageDialog(null, "Please add at least one station other than a source or sink.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
			tabbedPane.setSelectedIndex(1);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_MEASURE_ERROR)) {
			tabbedPane.setSelectedIndex(4);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_REFERENCE_STATION_ERROR)) {
			tabbedPane.setSelectedIndex(5);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.NO_ESSENTIAL_LINK_ERROR)) {
			String stationType = model.getStationType(relatedStation);
			String stationName = model.getStationName(relatedStation);
			String message = null;
			if (stationType.equals(CommonConstants.STATION_TYPE_SINK)) {
				message = stationName + " is not backward linked. Please add a backward link.\n";
			} else if (stationType.equals(CommonConstants.STATION_TYPE_TRANSITION)) {
				message = stationName + " is not forward or backward linked. Please add a forward or backward link.\n";
			} else {
				message = stationName + " is not forward linked. Please add a forward link.\n";
			}
			JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
			tabbedPane.setSelectedIndex(2);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.INVALID_MEASURE_ERROR)) {
			tabbedPane.setSelectedIndex(4);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.DUPLICATE_MEASURE_ERROR)) {
			int k = JOptionPane.showConfirmDialog(null, "Delete all redundant performance indices?\n",
					"Redundant performance indices found", JOptionPane.ERROR_MESSAGE);
			if (k == JOptionPane.YES_OPTION) {
				mc.deleteRedundantMeasures();
			}
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.INCONSISTENT_QUEUE_STRATEGY_ERROR)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.UNPREDICTABLE_SERVICE_ERROR)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.JOIN_WITHOUT_FORK_ERROR)) {
			JOptionPane.showMessageDialog(null, "A join is found but no forks. Please remove all joins or add a fork.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
			tabbedPane.setSelectedIndex(1);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.EMPTY_BLOCKING_REGION_ERROR)) {
			int k = JOptionPane.showConfirmDialog(null, "Delete all empty finite capacity regions?\n",
					"Empty finite capacity regions found", JOptionPane.ERROR_MESSAGE);
			if (k == JOptionPane.YES_OPTION) {
				mc.deleteEmptyBlockingRegions();
			}
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.BLOCKING_REGION_CAPACITY_OVERLOAD_ERROR)) {
			tabbedPane.setSelectedIndex(7);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.BLOCKING_REGION_MEMORY_OVERLOAD_ERROR)) {
			tabbedPane.setSelectedIndex(7);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.CLASS_SWITCH_INVALID_MATRIX_ERROR)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.SERVICE_SECTION, null);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.CLASS_SWITCH_REFERENCE_STATION_ERROR)) {
			tabbedPane.setSelectedIndex(5);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.ZERO_GUARD_STRATEGY_ERROR)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, relatedClass);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.SEMAPHORE_NOT_BETWEEN_FORK_JOIN_ERROR)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " is not located between fork/join. This topology is not allowed.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
			tabbedPane.setSelectedIndex(2);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.SCALER_NOT_BETWEEN_FORK_JOIN_ERROR)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " is not located between fork/join. This topology is not allowed.\n",
					"Error", JOptionPane.ERROR_MESSAGE);
			tabbedPane.setSelectedIndex(2);
		}
		else if ((problemType == ModelChecker.ERROR_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_INFINITE_ENABLING_DEGREE_ERROR)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		//used only in JMVA conversion
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.BCMP_DIFFERENT_QUEUE_STRATEGIES_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			int k = JOptionPane.showConfirmDialog(null, "According to BCMP theorem hypothesis, each server must have the same queue strategy\n"
					+ "for each class, but mixed queue strategies are found at " + stationName + ".\nDo you want to edit the queue strategies of "
					+ stationName + "?\n\n", "Mixed queue strategies found", JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				tabbedPane.setSelectedIndex(3);
				StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
				parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
			}
		}
		//used only in JMVA conversion
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.BCMP_FCFS_DIFFERENT_SERVICE_STRATEGIES_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			int k = JOptionPane.showConfirmDialog(null, "According to BCMP theorem hypothesis, each FCFS server must have the same service\n"
					+ "strategy for each class, but mixed service strategies (i.e. both the load dependent and\nindependent) are found at "
					+ stationName + ".\nDo you want to edit the service strategies of " + stationName + "?\n\n", "Mixed service strategies found",
					JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				tabbedPane.setSelectedIndex(3);
				StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
				parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.SERVICE_SECTION, null);
			}
		}
		//used only in JMVA conversion
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.BCMP_FCFS_NON_EXPONENTIAL_DISTRIBUTION_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			int k = JOptionPane.showConfirmDialog(null, "According to BCMP theorem hypothesis, the service time distributions of each FCFS\n"
					+ "server must be exponential, but a non exponential distribution is found at " + stationName + ".\nDo you want to edit "
					+ " the service time distributions of " + stationName + "?\n\n", "Non exponential distribution found", JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				tabbedPane.setSelectedIndex(3);
				StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
				parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.SERVICE_SECTION, null);
			}
		}
		//used only in JMVA conversion
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.BCMP_FCFS_DIFFERENT_SERVICE_TIMES_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			int k = JOptionPane.showConfirmDialog(null, "According to BCMP theorem hypothesis, each FCFS server must have the same mean service\n"
					+ "time for each class. If the service strategies are load dependent, the mean service time in each\nrange has to be the same "
					+ "for each class.\nDo you want to edit the mean service times of " + stationName + "?\n\n", "Mixed mean service times found",
					JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				tabbedPane.setSelectedIndex(3);
				StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
				parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.SERVICE_SECTION, null);
			}
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.NO_OPTIONAL_LINK_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " is not backward linked. Please check the topology.\n", "Warning",
					JOptionPane.WARNING_MESSAGE);
			tabbedPane.setSelectedIndex(2);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.FORK_WITHOUT_JOIN_WARNING)) {
			JOptionPane.showMessageDialog(null, "A fork is found but no joins. Please check the topology.\n", "Warning",
					JOptionPane.WARNING_MESSAGE);
			tabbedPane.setSelectedIndex(1);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.PARAMETRIC_ANALYSIS_MODEL_MODIFIED_WARNING)) {
			int k = JOptionPane.showConfirmDialog(null, "The parametric analysis model previously defined becomes inconsistent with the simulation\n"
					+ "model. It will be automatically modified when simulation is started.\nDo you want to autocorrect and check the parametric "
					+ "analysis model?\n\n", "Inconsistent parametric analysis model", JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				model.getParametricAnalysisModel().checkCorrectness(true);
				tabbedPane.setSelectedIndex(8);
			}
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.PARAMETRIC_ANALYSIS_NOT_AVAILABLE_WARNING)) {
			int k = JOptionPane.showConfirmDialog(null, "A parametric analysis model was defined, but no parametric analysis is currently available\n"
					+ "since the simulation model is changed. It is only possible to execute normal simulation.\nDo you want to continue anyway?\n\n",
					"Parametric analysis not available", JOptionPane.WARNING_MESSAGE);
			if (k == JOptionPane.OK_OPTION) {
				model.setParametricAnalysisEnabled(false);
				model.setParametricAnalysisModel(null);
			}
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.ZERO_POPULATION_WARNING)) {
			tabbedPane.setSelectedIndex(0);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.ZERO_TOTAL_POPULATION_WARNING)) {
			tabbedPane.setSelectedIndex(0);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.CLASS_SWITCH_BETWEEN_FORK_JOIN_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " seems to be located between fork/join. The switched tasks may fail to merge\n"
					+ "at the join with a Guard strategy.\n", "Warning", JOptionPane.WARNING_MESSAGE);
			tabbedPane.setSelectedIndex(2);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.SCHEDULING_SAME_PRIORITY_WARNING)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_BETWEEN_FORK_JOIN_WARNING)) {
			String stationName = model.getStationName(relatedStation);
			JOptionPane.showMessageDialog(null, stationName + " seems to be located between fork/join. On any firing of a transition between\n"
					+ "fork/join, the number of output tasks for each class should be equal to the number of input\ntasks for that class.\n",
					"Warning", JOptionPane.WARNING_MESSAGE);
			tabbedPane.setSelectedIndex(2);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_CONSTANT_ENABLING_DEGREE_WARNING)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_INVALID_INPUT_CONDITION_WARNING)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.INPUT_SECTION, null);
		}
		else if ((problemType == ModelChecker.WARNING_PROBLEM) && (problemSubType == ModelChecker.TRANSITION_NO_FIRING_OUTCOME_WARNING)) {
			tabbedPane.setSelectedIndex(3);
			StationParametersPanel parameterPanel = (StationParametersPanel) tabbedPane.getComponentAt(3);
			parameterPanel.showStationParameterPanel(relatedStation, StationParameterPanel.OUTPUT_SECTION, null);
		}
	}

	/**
	 * Used to discover if the instance can display simulation animation
	 *
	 * @return true if the instance can display simulation animation
	 */
	public boolean isAnimationDisplayable() {
		return false;
	}

	//-------------------------------------- end Francesco D'Aquino --------------------------------

	/**
	 * Shows a panel with caught exception
	 * @param e exception to be shown
	 */
	public void handleException(Exception e) {
		showErrorMessage(e.getMessage());
	}

	/**
	 * Shows a panel with an error message
	 * @param message specified error message
	 */
	public void showErrorMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Checks if there's an old graph to save. This methods is called when creates/closes/opens a graph.
	 * @param msg The message to display.
	 * @return <code>true</code> - whether the user accepts to save the graph, or he cancels the current action.
	 */
	public boolean checkForSave(String msg) {
		// Checks if there's an old graph to save
		if (model != null && model.toBeSaved()) {
			int resultValue = JOptionPane.showConfirmDialog(this, msg, "JSIM - Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (resultValue == JOptionPane.YES_OPTION) {
				saveFile();
				return true;
			}
			if (resultValue == JOptionPane.CANCEL_OPTION) {
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		JSIMWizMain jsm = new JSIMWizMain();
		jsm.setVisible(true);
		if (args != null && args.length > 0) {
			File file = new File(args[0]);
			if (!file.isFile()) {
				System.err.print("Invalid model file: " + file.getAbsolutePath());
				System.exit(1);
			}
			jsm.openFile(file);
		}
	}

	/**
	 * Overrides default Solve button press...
	 * @see Wizard
	 */
	@Override
	protected void finish() {
		SIM_START.actionPerformed(null);
	}

	@Override
	public void setAnimationHolder(Thread thread) {
		//Ignore		
	}

	@Override
	public String getFileName() {
		if (currentFile != null) {
			return currentFile.getName();
		} else {
			return null;
		}
	}

}
