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

package jmt.gui.jwat;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import jmt.engine.jwat.JwatSession;
import jmt.engine.jwat.ProgressStatusListener;
import jmt.engine.jwat.fitting.FittingSession;
import jmt.engine.jwat.input.EventFinishAbort;
import jmt.engine.jwat.input.EventSessionLoaded;
import jmt.engine.jwat.input.EventStatus;
import jmt.engine.jwat.input.Loader;
import jmt.engine.jwat.input.ProgressMonitorShow;
import jmt.engine.jwat.trafficAnalysis.TrafficAnalysisSession;
import jmt.engine.jwat.workloadAnalysis.WorkloadAnalysisSession;
import jmt.framework.gui.components.JMTMenuBar;
import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.listeners.AbstractJMTAction;
import jmt.framework.gui.listeners.MenuAction;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.jwat.fitting.panels.FittingPanel;
import jmt.gui.jwat.trafficAnalysis.panels.EpochPanel;
import jmt.gui.jwat.trafficAnalysis.panels.GraphArrivalPanel;
import jmt.gui.jwat.trafficAnalysis.panels.GraphPanel;
import jmt.gui.jwat.trafficAnalysis.panels.TextualPanel;
import jmt.gui.jwat.workloadAnalysis.panels.ClusterPanel;
import jmt.gui.jwat.workloadAnalysis.panels.ClusteringInfoPanel;
import jmt.gui.jwat.workloadAnalysis.panels.InputPanel;
import jmt.gui.jwat.workloadAnalysis.panels.LoadDemoPanel;
import jmt.gui.jwat.workloadAnalysis.panels.StatsPanel;
import jmt.manual.ChapterIdentifier;
import jmt.manual.PDFViewer;

public class MainJwatWizard extends JWatWizard {

	private static final long serialVersionUID = 1L;
	//JWAT tool icons
	private String IMG_JWATICON = "JWATIcon";
	//private JToolBar toolBar = null;
	private static final String TITLE = "JWAT";
	private static final String WORKLOAD_TITLE = "Workload Analysis";
	private static final String FITTING_TITLE = "Fitting Workload";
	private static final String TRAFFIC_TITLE = "Traffic Analysis - Burstiness";
	private JPanel menus = null;

	//private JWatModel model = null;
	private JwatSession session = null;

	//Last panel visited, used to control correct next step
	private int lastPanel = 0;
	private int currentPanel = 0;

	private HoverHelp help = null;
	// List of panels create for Workload Analysis tool
	private ArrayList<WizardPanel> JWatPanels = new ArrayList<WizardPanel>();
	// First panel
	private JWatMainPanel mainPanel = null;

	/**
	 * Constructor.
	 */
	public MainJwatWizard() {
		initGUI();
	}

	private JFileChooser fileSaveF = new JFileChooser(Defaults.getWorkingPath()) {

		private static final long serialVersionUID = 1L;

		{
			setApproveButtonText("Save");
			setFileSelectionMode(JFileChooser.FILES_ONLY);
		}

	};

	private JFileChooser fileOpenF = new JFileChooser(Defaults.getWorkingPath()) {

		private static final long serialVersionUID = 1L;

		{
			setApproveButtonText("Open");
			setFileSelectionMode(JFileChooser.FILES_ONLY);
		}

	};

	/**
	 * Initializes JWAT start screen GUI
	 */
	private void initGUI() {
		this.setIconImage(JMTImageLoader.loadImage(IMG_JWATICON).getImage());
		//this.setResizable(false);
		this.setTitle(TITLE);
		this.setSize(CommonConstants.MAX_GUI_WIDTH_JWAT, CommonConstants.MAX_GUI_HEIGHT_JWAT);
		centerWindow();
		menus = new JPanel(new BorderLayout());
		help = this.getHelp();
		getContentPane().add(menus, BorderLayout.NORTH);
		mainPanel = new JWatMainPanel(this);
		this.addPanel(mainPanel);
		setEnableButton("Solve", false);
	}

	// Set correct environment for workload analysis
	public void setWorkloadEnv(String mode) {
		this.setTitle(TITLE + " - " + WORKLOAD_TITLE);
		session = new WorkloadAnalysisSession();

		//Creates and adds all necessary panels to JWAT main screen
		WizardPanel p;

		if (mode.equals("load")) {
			p = new InputPanel(this);
		} else {
			p = new LoadDemoPanel(this);
		}

		JWatPanels.add(p);
		this.addPanel(p);

		p = new StatsPanel(this);
		JWatPanels.add(p);
		this.addPanel(p);

		p = new ClusterPanel(this);
		JWatPanels.add(p);
		this.addPanel(p);

		p = new ClusteringInfoPanel(this);
		JWatPanels.add(p);
		this.addPanel(p);

		if (mode.equals("load")) {
			WL_FILE_NEW.setEnabled(true);
		} else {
			WL_FILE_NEW.setEnabled(false);
		}
		WL_FILE_OPEN.setEnabled(false);
		WL_FILE_SAVE.setEnabled(false);
		WL_ACTION_CLUSTERIZE.setEnabled(false);
		makeWorkloadToolbar();
		makeWorkloadMenubar();
		setEnableButton("Next >", false);
		setEnableButton("Solve", false);
		//Shows next panel, the first of workload analysis wizard
		showNextPanel();
	}

	// Set correct environment for fitting
	public void setFittingEnv(String mode) {
		this.setTitle(TITLE + " - " + FITTING_TITLE);
		session = new FittingSession();

		//Creates and adds all necessary panels to JWAT main screen
		WizardPanel p;

		if (mode.equals("load")) {
			p = new jmt.gui.jwat.fitting.panels.InputPanel(this);
		} else {
			p = new jmt.gui.jwat.fitting.panels.LoadDemoFittingPanel(this);
		}

		JWatPanels.add(p);
		this.addPanel(p);

		p = new FittingPanel(this, FittingPanel.PARETO);
		JWatPanels.add(p);
		this.addPanel(p);

		p = new FittingPanel(this, FittingPanel.EXPO);
		JWatPanels.add(p);
		this.addPanel(p);

		if (mode.equals("load")) {
			FI_FILE_NEW.setEnabled(true);
		} else {
			FI_FILE_NEW.setEnabled(false);
		}
		makeFittingToolbar();
		makeFittingMenubar();
		setEnableButton("Next >", false);
		setEnableButton("Solve", false);
		//Shows next panel, the first of fitting wizard
		showNextPanel();
	}

	// Set correct environment for traffic analysis
	public void setTrafficEnv() {
		this.setTitle(TITLE + " - " + TRAFFIC_TITLE);
		session = new TrafficAnalysisSession();

		//Creates and adds all necessary panels to JWAT main screen
		WizardPanel p = new jmt.gui.jwat.trafficAnalysis.panels.InputPanel(this);
		JWatPanels.add(p);
		this.addPanel(p);

		p = new EpochPanel(this);
		JWatPanels.add(p);
		this.addPanel(p);

		p = new TextualPanel(this);
		JWatPanels.add(p);
		this.addPanel(p);

		p = new GraphPanel(this);
		JWatPanels.add(p);
		this.addPanel(p);

		p = new GraphArrivalPanel(this);
		JWatPanels.add(p);
		this.addPanel(p);

		makeTrafficToolbar();
		makeTrafficMenubar();
		setEnableButton("Next >", false);
		setEnableButton("Solve", false);
		//Shows next panel, the first of traffic analysis wizard
		showNextPanel();
	}

	/**
	 * Main method.
	 * @param args no args.
	 */
	public static void main(String[] args) {
		new MainJwatWizard().setVisible(true);
	}

	public void setToolBar(JToolBar bar) {
		if (toolBar != null) {
			menus.remove(toolBar);
		}
		menus.add(bar, BorderLayout.SOUTH);
		toolBar = bar;
	}

	public void setMenuBar(JMenuBar bar) {
		if (mMenuBar != null) {
			menus.remove(mMenuBar);
		}
		menus.add(bar, BorderLayout.NORTH);
		mMenuBar = bar;
	}

	public void setLastPanel() {
		tabbedPane.setSelectedIndex(lastPanel);
	}

	public void setLastPanel(int panel) {
		lastPanel = panel;
	}

	public void setCurrentPanel(int panel) {
		currentPanel = panel;
	}

	public JWatModel getModel() {
		JWatModel mode = null;
		if (session != null) {
			mode = session.getDataModel();
		}
		return mode;
	}

	public JwatSession getSession() {
		return session;
	}

	private AbstractJMTAction WL_FILE_NEW = new AbstractJMTAction("New") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Create a new session");
			setIcon("New", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		}

		public void actionPerformed(ActionEvent e) {
			if (JOptionPane.showConfirmDialog(MainJwatWizard.this, "This operation will reset data. Continue?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				//Reset model and set first panel
				session.resetSession();
				tabbedPane.setSelectedIndex(JWATConstants.WORKLOAD_INPUT_PANEL);
				((InputPanel) tabbedPane.getComponentAt(JWATConstants.WORKLOAD_INPUT_PANEL)).resetOnNew();
			}
		}

	};

	private AbstractJMTAction WL_FILE_OPEN = new AbstractJMTAction("Open") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Open a saved session");
			setIcon("Open", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
		}

		public void actionPerformed(ActionEvent e) {
			if (fileOpenF.showOpenDialog(MainJwatWizard.this) == JFileChooser.APPROVE_OPTION) {
				if (currentPanel != JWATConstants.WORKLOAD_INPUT_PANEL) {
					tabbedPane.setSelectedIndex(JWATConstants.WORKLOAD_INPUT_PANEL);
				}
				File fFile = fileOpenF.getSelectedFile();
				String fileName = fFile.getAbsolutePath();
				Loader.loadSession(fileName, new ProgressMonitorShow(tabbedPane.getComponentAt(currentPanel), "Loading Session...", 1000),
						new SessionStatusListener(), session);
			}
		}

	};

	private AbstractJMTAction WL_FILE_SAVE = new AbstractJMTAction("Save") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Save this session");
			setIcon("Save", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		}

		public void actionPerformed(ActionEvent e) {
			if (fileSaveF.showOpenDialog(MainJwatWizard.this) == JFileChooser.APPROVE_OPTION) {
				File fFile = fileSaveF.getSelectedFile();
				String fileName = fFile.getAbsolutePath();
				MainJwatWizard.this.session.saveSession(fileName.substring(0, fileName.lastIndexOf("\\")) + "\\", fileName.substring(fileName
						.lastIndexOf("\\") + 1), JwatSession.WORKLOAD_SAVE);
			}
		}

	};

	protected AbstractJMTAction WL_FILE_EXIT = new AbstractJMTAction("Exit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Exit JWAT");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_Q));
		}

		public void actionPerformed(ActionEvent e) {
			close();
		}

	};

	private AbstractJMTAction WL_ACTION_CLUSTERIZE = new AbstractJMTAction("Clusterize") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Clusterize");
			setIcon("Sim", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
		}

		public void actionPerformed(ActionEvent e) {
		}

	};

	private AbstractJMTAction WL_HELP_SHOWHELP = new AbstractJMTAction("Workload Analysis Help") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Show Workload Analysis help");
			setIcon("Help", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		}

		public void actionPerformed(ActionEvent e) {
			Runnable r = new Runnable() {
				public void run() {
					try {
						new PDFViewer("JWAT Manual", ChapterIdentifier.JWAT);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			EventQueue.invokeLater(r);
		}

	};

	private AbstractJMTAction WL_HELP_CREDITS = new AbstractJMTAction("About Workload Analysis") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "About Workload Analysis");
		}

		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(MainJwatWizard.this, "Sorry, this is not available", "About Workload Analysis not found", JOptionPane.ERROR_MESSAGE);
		}

	};

	private class SessionStatusListener implements ProgressStatusListener {

		public void statusEvent(EventStatus e) {
			switch (e.getType()) {
			case EventStatus.ABORT_EVENT:
				abortEvent((EventFinishAbort) e);
				break;
			case EventStatus.DONE_EVENT:
				finishedEvent((EventSessionLoaded) e);
				break;
			}
		}

		//Abort caricamento file input
		private void abortEvent(EventFinishAbort e) {
			JWatWizard wizard = (JWatWizard) ((WizardPanel) tabbedPane.getComponentAt(currentPanel)).getParentWizard();
			JOptionPane.showMessageDialog(tabbedPane.getComponentAt(currentPanel), e.getMessage(), "LOADING ABORTED!!", JOptionPane.WARNING_MESSAGE);
			((InputPanel) tabbedPane.getComponentAt(JWATConstants.WORKLOAD_INPUT_PANEL)).setCanGoForward(false);
			wizard.setEnableButton("Next >", false);
			wizard.setEnableButton("Solve", false);
		}

		//dati caricati
		private void finishedEvent(final EventSessionLoaded e) {
			JButton[] optBtn = new JButton[2];
			JOptionPane pane;
			((InputPanel) tabbedPane.getComponentAt(JWATConstants.WORKLOAD_INPUT_PANEL)).setCanGoForward(false);
			JWatWizard wizard = (JWatWizard) ((WizardPanel) tabbedPane.getComponentAt(currentPanel)).getParentWizard();

			optBtn[0] = new JButton("Continue");
			optBtn[1] = new JButton("Cancel");

			pane = new JOptionPane("Load session done.", JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, optBtn, null);
			final JDialog dialog = pane.createDialog(wizard, "Loading Complete");
			pane.selectInitialValue();

			optBtn[0].addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent ev) {
					JwatSession newSession = e.getSession();
					dialog.dispose();
					JWatWizard wizard = (JWatWizard) ((WizardPanel) tabbedPane.getComponentAt(currentPanel)).getParentWizard();
					session.copySession(newSession);
					wizard.setEnableButton("Next >", true);
					wizard.setEnableButton("Solve", false);
					((InputPanel) tabbedPane.getComponentAt(JWATConstants.WORKLOAD_INPUT_PANEL)).setCanGoForward(true);
					wizard.showNextPanel();
				}

			});

			optBtn[1].addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent ev) {
					JWatWizard wizard = (JWatWizard) ((WizardPanel) tabbedPane.getComponentAt(currentPanel)).getParentWizard();
					dialog.dispose();
					((InputPanel) tabbedPane.getComponentAt(JWATConstants.WORKLOAD_INPUT_PANEL)).setCanGoForward(true);
					System.gc();
					wizard.setEnableButton("Next >", false);
					wizard.setEnableButton("Solve", false);
				}

			});

			dialog.show();
		}

	}

	/**
	 * Creates workload analysis toolbar
	 */
	protected void makeWorkloadToolbar() {
		JMTToolBar toolbar = new JMTToolBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] items = new AbstractJMTAction[] { WL_FILE_NEW, WL_FILE_OPEN, WL_FILE_SAVE, null, WL_ACTION_CLUSTERIZE, null, WL_HELP_SHOWHELP };
		toolbar.populateToolbar(items);
		toolbar.setFloatable(false);
		setToolBar(toolbar);
	}

	/**
	 * Creates workload analysis menubar
	 */
	private void makeWorkloadMenubar() {
		JMTMenuBar menuBar = new JMTMenuBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] menus = new AbstractJMTAction[] {
				//File menu
				new MenuAction("File", new AbstractJMTAction[] { WL_FILE_NEW, WL_FILE_OPEN, WL_FILE_SAVE, null, WL_FILE_EXIT }),
				//Action menu
				new MenuAction("Action", new AbstractJMTAction[] { WL_ACTION_CLUSTERIZE }),
				//Help menu
				new MenuAction("Help", new AbstractJMTAction[] { WL_HELP_SHOWHELP, null, WL_HELP_CREDITS }) };
		menuBar.populateMenu(menus);
		setMenuBar(menuBar);
	}

	public void resetScreen() {
		for (int i = 0; i < JWatPanels.size(); i++) {
			tabbedPane.remove(JWatPanels.get(i));
		}
		JWatPanels.clear();
		mainPanel.makeMenubar();
		mainPanel.makeToolbar();
		this.validate();
	}

	/**
	 * Creates fitting toolbar
	 */
	protected void makeFittingToolbar() {
		JMTToolBar toolbar = new JMTToolBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] items = new AbstractJMTAction[] { FI_FILE_NEW, null, FI_HELP_SHOWHELP };
		toolbar.populateToolbar(items);
		toolbar.setFloatable(false);
		setToolBar(toolbar);
	}

	/**
	 * Creates fitting menubar
	 */
	private void makeFittingMenubar() {
		JMTMenuBar menuBar = new JMTMenuBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] menus = new AbstractJMTAction[] {
				//File menu
				new MenuAction("File", new AbstractJMTAction[] { FI_FILE_NEW, null, FI_FILE_EXIT }),
				//Help menu
				new MenuAction("Help", new AbstractJMTAction[] { FI_HELP_SHOWHELP, null, FI_HELP_CREDITS }) };
		menuBar.populateMenu(menus);
		setMenuBar(menuBar);
	}

	/**
	 * Creates traffic analysis toolbar
	 */
	protected void makeTrafficToolbar() {
		JMTToolBar toolbar = new JMTToolBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] items = new AbstractJMTAction[] { TR_FILE_NEW, null, TR_HELP_SHOWHELP };
		toolbar.populateToolbar(items);
		toolbar.setFloatable(false);
		setToolBar(toolbar);
	}

	/**
	 * Creates traffic analysis menubar
	 */
	private void makeTrafficMenubar() {
		JMTMenuBar menuBar = new JMTMenuBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] menus = new AbstractJMTAction[] {
				//File menu
				new MenuAction("File", new AbstractJMTAction[] { TR_FILE_NEW, null, TR_FILE_EXIT }),
				//File menu
				new MenuAction("Help", new AbstractJMTAction[] { TR_HELP_SHOWHELP, null, TR_HELP_CREDITS }) };
		menuBar.populateMenu(menus);
		setMenuBar(menuBar);
	}

	private AbstractJMTAction FI_FILE_NEW = new AbstractJMTAction("New") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Create a new session");
			setIcon("New", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		}

		public void actionPerformed(ActionEvent e) {
			if (JOptionPane.showConfirmDialog(MainJwatWizard.this, "This operation will reset data. Continue?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				//Reset model and set first panel
				session.resetSession();
				tabbedPane.setSelectedIndex(JWATConstants.WORKLOAD_INPUT_PANEL);
				((jmt.gui.jwat.fitting.panels.InputPanel) tabbedPane.getComponentAt(JWATConstants.WORKLOAD_INPUT_PANEL)).resetOnNew();
			}
		}

	};

	private AbstractJMTAction FI_FILE_EXIT = new AbstractJMTAction("Exit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Exit JWAT");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_Q));
		}

		public void actionPerformed(ActionEvent e) {
			close();
		}

	};

	private AbstractJMTAction FI_HELP_SHOWHELP = new AbstractJMTAction("Fitting Help") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Show Fitting help");
			setIcon("Help", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		}

		public void actionPerformed(ActionEvent e) {
			Runnable r = new Runnable() {
				public void run() {
					try {
						new PDFViewer("JWAT Manual", ChapterIdentifier.JWAT);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			EventQueue.invokeLater(r);
		}

	};

	private AbstractJMTAction FI_HELP_CREDITS = new AbstractJMTAction("About Fitting") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "About Fitting");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		}

		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(MainJwatWizard.this, "Sorry, this is not available", "About Fitting not found", JOptionPane.ERROR_MESSAGE);
		}

	};

	private AbstractJMTAction TR_FILE_NEW = new AbstractJMTAction("New") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Create a new session");
			setIcon("New", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		}

		public void actionPerformed(ActionEvent e) {
			if (JOptionPane.showConfirmDialog(MainJwatWizard.this, "This operation will reset data. Continue?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				//Reset model and set first panel
				session.resetSession();
				tabbedPane.setSelectedIndex(JWATConstants.WORKLOAD_INPUT_PANEL);
				((jmt.gui.jwat.trafficAnalysis.panels.InputPanel) tabbedPane.getComponentAt(JWATConstants.WORKLOAD_INPUT_PANEL)).resetOnNew();
			}
		}

	};

	private AbstractJMTAction TR_FILE_EXIT = new AbstractJMTAction("Exit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Exit JWAT");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_Q));
		}

		public void actionPerformed(ActionEvent e) {
			close();
		}

	};

	private AbstractJMTAction TR_HELP_SHOWHELP = new AbstractJMTAction("Traffic Analysis Help") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Show Traffic Analysis help");
			setIcon("Help", JMTImageLoader.getImageLoader());
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		}

		public void actionPerformed(ActionEvent e) {
			Runnable r = new Runnable() {
				public void run() {
					try {
						new PDFViewer("JWAT Manual", ChapterIdentifier.JWAT);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			EventQueue.invokeLater(r);
		}

	};

	private AbstractJMTAction TR_HELP_CREDITS = new AbstractJMTAction("About Traffic Analysis") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "About Traffic Analysis");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
			putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_H));
		}

		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(MainJwatWizard.this, "Sorry, this is not available", "About Traffic Analysis not found", JOptionPane.ERROR_MESSAGE);
		}

	};

}
