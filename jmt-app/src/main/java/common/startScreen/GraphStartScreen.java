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

package common.startScreen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import jmt.common.GlobalSettings;
import jmt.framework.gui.components.JMTFrame;
import jmt.framework.gui.components.QuickHTMLViewer;
import jmt.framework.net.BrowserLauncher;
import common.CommonConstants;
import common.JMTImageLoader;
import common.panels.AboutDialogFactory;
import common.panels.AboutDialogFactory.Company;
import common.panels.UpdatePanel;
import common.startScreen.sampleAnimation.SampleQNAnimation;
import jaba.JabaWizard;
import jsimgraph.mainGui.JSIMGraphMain;
import jsimwiz.JSIMWizMain;
import jwat.MainJwatWizard;
import jmt.jmch.wizard.MainWizard;
import jmva.JMVAWizard;
import jmt.commandline.Jmt;
import jmt.manual.ChapterIdentifier;
import jmt.manual.PDFViewer;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

/**
 * <p>
 * Title: Graph StartScreen
 * </p>
 * <p>
 * Description: A new StartScreen that displays a graph with all possible
 * choices to help user chosing the right application
 * </p>
 *
 * @author Bertoli Marco Date: 18-ott-2005 Time: 12.34.22
 */
public class GraphStartScreen extends JMTFrame {

	private static final long serialVersionUID = 1L;
	private static final int BORDERSIZE = 20;
	private static final int BUTTONSIZE = 25;
	private static final int FONT_SIZE = 4;
	private static String[] args;
	private JButton onlineDoc;
	private JButton introEng;
	private JButton about;
	private JButton upgrade;

	private Color upgradeBackgroundColor = new Color(46, 181, 226, 186); // Color.RED;
	private Color upgradeHoverColor = new Color(168, 233, 255);

	// Queue Animation
	public static SampleQNAnimation sampleQNAni;

	// Images
	public static final String IMG_STARTSCREEN = "StartScreen";
	public static final String IMG_JMODELICON = "JMODELIcon";
	public static final String IMG_JMVAICON = "JMVAIcon";
	public static final String IMG_JSIMICON = "JSIMIcon";
	public static final String IMG_JABAICON = "JABAIcon";
	public static final String IMG_JMCHICON = "JMCHIcon";
	public static final String IMG_JWATICON = "JWATIcon";
	public static final String IMG_SUITEICON = "JMTIcon";

	// Short Descriptions
	public static final String JMVA_SHORT_DESCRIPTION = "JMVA: MVA solver with wizard interface";
	public static final String JMCH_SHORT_DESCRIPTION = "JMCH: Markov Chain simulator with graphical interface";
	public static final String JWAT_SHORT_DESCRIPTION = "JWAT: Workload Analyzer Tool";
	public static final String JSIM_SHORT_DESCRIPTION = "JSIMwiz: Simulator with wizard interface";
	public static final String JMODEL_SHORT_DESCRIPTION = "JSIMgraph: Simulator with graphical interface";
	public static final String JABA_SHORT_DESCRIPTION = "JABA: Asyntotic bound analysis with wizard interface";
	// names for URLS of documents to be shown as description of main applications
	private static final String URL_DOCUMENTATION_ONLINE = "http://jmt.sf.net/Documentation.html";
	// Content for logo panel
	private static final String FONT_TYPE = "Arial";
	public static final String HTML_CONTENT_TITLE = String.format(
			"<html><body><b><font face='%s' size='+2'>JMT - Java Modelling Tools v.%s</font></b><br>"
					+ "</body></html>",
			FONT_TYPE, GlobalSettings.getSetting(GlobalSettings.VERSION), FONT_TYPE);
	public static final String HTML_CONTENT_TITLE_HREF = String.format(
			"<html><body><bIMG_SUITEICON><font face='%s' size='+2'>JMT - Java Modelling Tools v.%s</font></b><br>"
					+ "<b>Home Page:</b> <a href=\"http://jmt.sf.net\">http://jmt.sf.net</a></font>" + "</body></html>",
			FONT_TYPE, GlobalSettings.getSetting(GlobalSettings.VERSION), FONT_TYPE);
	public static final String HTML_POLI = String.format("<html><body><font face='%s' size='%s'><b>DEIB<br>"
			+ "Politecnico di Milano<br>Italy</b></font></body></html>", FONT_TYPE, FONT_SIZE);
	public static final String HTML_UNIVAQ = String.format("<html><body><font face='%s' size='%s'><b>DISIM<br>"
			+ "Università degli Studi dell'Aquila<br>Italy</b></font></body></html>", FONT_TYPE, FONT_SIZE);

	/**
	 * Constructs a new GraphStartScreen
	 */
	public GraphStartScreen() {
		super(true);
		initGUI();
		addListeners();
		this.revalidate();
		this.repaint();
	}

	// --- Actions associated with buttons
	// -----------------------------------------------------------------
	private AbstractAction startJMVA = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, JMVA_SHORT_DESCRIPTION);
			putValue(Action.LARGE_ICON_KEY,
					JMTImageLoader.loadImage(IMG_JMVAICON, new Dimension((int) (40 * CommonConstants.widthScaling),
							(int) (40 * CommonConstants.heightScaling))));
		}

		public void actionPerformed(ActionEvent e) {
			new JMVAWizard();
		}

	};

	private AbstractAction startJMCH = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, JMCH_SHORT_DESCRIPTION);
			putValue(Action.LARGE_ICON_KEY,
					JMTImageLoader.loadImage(IMG_JMCHICON, new Dimension((int) (40 * CommonConstants.widthScaling),
							(int) (40 * CommonConstants.heightScaling))));
		}

		public void actionPerformed(ActionEvent e) {
			// MMQueues.main(args); // Old JMCH
			MainWizard.main(args);
		}

	};

	private AbstractAction startJWAT = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, JWAT_SHORT_DESCRIPTION);
			putValue(Action.LARGE_ICON_KEY,
					JMTImageLoader.loadImage(IMG_JWATICON, new Dimension((int) (40 * CommonConstants.widthScaling),
							(int) (40 * CommonConstants.heightScaling))));
		}

		public void actionPerformed(ActionEvent e) {
			MainJwatWizard.main(args);
		}

	};

	private AbstractAction startJSIM = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, JSIM_SHORT_DESCRIPTION);
			putValue(Action.LARGE_ICON_KEY,
					JMTImageLoader.loadImage(IMG_JSIMICON, new Dimension((int) (40 * CommonConstants.widthScaling),
							(int) (40 * CommonConstants.heightScaling))));
		}

		public void actionPerformed(ActionEvent e) {
			JSIMWizMain.main(args);
		}

	};

	private AbstractAction startJMODEL = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, JMODEL_SHORT_DESCRIPTION);
			putValue(Action.LARGE_ICON_KEY,
					JMTImageLoader.loadImage(IMG_JMODELICON, new Dimension((int) (40 * CommonConstants.widthScaling),
							(int) (40 * CommonConstants.heightScaling))));
		}

		public void actionPerformed(ActionEvent e) {
			JSIMGraphMain.main(args);
		}

	};

	private AbstractAction startJABA = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, JABA_SHORT_DESCRIPTION);
			putValue(Action.LARGE_ICON_KEY,
					JMTImageLoader.loadImage(IMG_JABAICON, new Dimension((int) (40 * CommonConstants.widthScaling),
							(int) (40 * CommonConstants.heightScaling))));
		}

		public void actionPerformed(ActionEvent e) {
			JabaWizard.main(args);
		}

	};
	// -----------------------------------------------------------------------------------------------------

	// --- Buttons to be created
	// ---------------------------------------------------------------------------
	/**
	 * Data structure used to create buttons. To add a new button simply add its
	 * action here
	 */
	protected AbstractAction[] buttonActions = { startJSIM, startJMODEL, startJMVA, startJMCH, startJABA, startJWAT };

	// -----------------------------------------------------------------------------------------------------

	// --- Methods to paint GUI
	// ----------------------------------------------------------------------------
	/**
	 * Initialize all gui related stuff
	 */
	private void initGUI() {
		// Sets tooltip delay for whole project
		ToolTipManager.sharedInstance().setInitialDelay(300);
		ToolTipManager.sharedInstance().setDismissDelay(100000);
		// Sets default title, close operation and dimensions
		this.setTitle("JMT - Java Modelling Tools v." + GlobalSettings.getSetting(GlobalSettings.VERSION));
		this.setIconImage(JMTImageLoader.loadImage(IMG_SUITEICON).getImage());
		this.setResizable(false);
		this.centerWindow(CommonConstants.MAX_GUI_WIDTH_STARTSCREEN, CommonConstants.MAX_GUI_HEIGHT_STARTSCREEN);

		JPanel mainPanel = new JPanel(new BorderLayout());
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		JLabel title = new JLabel(HTML_CONTENT_TITLE);
		title.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));
		this.getContentPane().add(title, BorderLayout.NORTH);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(BORDERSIZE, BORDERSIZE, BORDERSIZE, BORDERSIZE));

		// Adds the custom diagram drawing panel
		DiagramPanel diagramPanel = new DiagramPanel();
		diagramPanel.setBorder(BorderFactory.createEmptyBorder(BUTTONSIZE - 5, 1, 0, 0));
		diagramPanel.setPreferredSize(new Dimension(
				(int) (440 * CommonConstants.widthScaling), (int) (560 * CommonConstants.heightScaling)));
		mainPanel.add(diagramPanel, BorderLayout.CENTER);

		// Set layout to null to enable absolute positioning
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(null);
		buttonPanel.setOpaque(false); // Keeps it transparent over the background

		int panelWidth = (int) (BUTTONSIZE * 3.5 * CommonConstants.widthScaling) + 5;
		buttonPanel.setPreferredSize(new Dimension(panelWidth, 0));

		buttonPanel.add(createButton(startJSIM, 5, 5));
		buttonPanel.add(createButton(startJMODEL, 5, 107));
		buttonPanel.add(createButton(startJMVA, 5, 205));
		buttonPanel.add(createButton(startJMCH, 5, 310));
		buttonPanel.add(createButton(startJABA, 5, 410));
		buttonPanel.add(createButton(startJWAT, 5, 510));

		mainPanel.add(buttonPanel, BorderLayout.EAST);

		// Now adds a panel with logo on the top of everything else. Uses glassPanel to
		// perform this
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBorder(BorderFactory.createEmptyBorder(60, BORDERSIZE, BORDERSIZE, BORDERSIZE));
		topPanel.setOpaque(false);
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setOpaque(false);
		this.setGlassPane(topPanel);
		topPanel.add(leftPanel, BorderLayout.WEST);
		// Adds logo and title to leftPanel
		{
			JPanel logoPanel = new JPanel();
			BoxLayout layout = new BoxLayout(logoPanel, BoxLayout.Y_AXIS);
			logoPanel.setLayout(layout);
			logoPanel.add(Company.POLIMI.getLogo());
			logoPanel.add(Company.UNIVAQ.getLogo());
			leftPanel.add(logoPanel, BorderLayout.NORTH);
		}
		this.getGlassPane().setVisible(true);
		// Now Adds Sample Animation in the bottom
		sampleQNAni = new SampleQNAnimation();
		JPanel pivotPanel = new JPanel(new GridBagLayout());
		sampleQNAni.setPreferredSize(new Dimension((int) (200), (int) (120)));
		// sampleQNAni.setBackground(new Color(151, 151, 151));
		pivotPanel.add(sampleQNAni);
		leftPanel.add(pivotPanel, BorderLayout.SOUTH);
		// Adds intro buttons in the centre
		GridLayout gl = new GridLayout(-1, 1);
		JPanel introButtonArea = new JPanel(gl);

		onlineDoc = new JButton("Online Documentation");
		onlineDoc.addMouseListener(rollover);
		onlineDoc.setPreferredSize(
				new Dimension((int) (150 * CommonConstants.widthScaling), (int) (20 * CommonConstants.heightScaling)));
		introEng = new JButton("Introduction to JMT");
		introEng.addMouseListener(rollover);
		introEng.setPreferredSize(
				new Dimension((int) (150 * CommonConstants.widthScaling), (int) (20 * CommonConstants.heightScaling)));
		about = new JButton("Credits");
		about.addMouseListener(rollover);
		about.setPreferredSize(
				new Dimension((int) (150 * CommonConstants.widthScaling), (int) (20 * CommonConstants.heightScaling)));
		upgrade = new JButton("Upgrade");
		upgrade.addMouseListener(rollover);
		upgrade.setBackground(upgradeBackgroundColor);
		upgrade.setPreferredSize(
				new Dimension((int) (150 * CommonConstants.widthScaling), (int) (20 * CommonConstants.heightScaling)));

		introButtonArea.add(introEng);
		introButtonArea.add(new JPanel());
		introButtonArea.add(onlineDoc);
		introButtonArea.add(new JPanel());
		introButtonArea.add(about);
		introButtonArea.add(new JPanel());
		introButtonArea.add(upgrade);
		introButtonArea.setOpaque(false);

		pivotPanel = new JPanel(new GridBagLayout());
		pivotPanel.add(introButtonArea);
		pivotPanel.setOpaque(false);
		pivotPanel.add(new JPanel());
		leftPanel.add(pivotPanel, BorderLayout.CENTER);
	}

	// assigns each component its own listener
	private void addListeners() {
		onlineDoc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BrowserLauncher.openURL(URL_DOCUMENTATION_ONLINE);
			}
		});

		introEng.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Runnable r = new Runnable() {
					public void run() {
						try {
							new PDFViewer("Introduction to JMT", ChapterIdentifier.INTRO);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				EventQueue.invokeLater(r);
			}
		});

		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				AboutDialogFactory.showJMT(GraphStartScreen.this);
			}
		});
		upgrade.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UpdatePanel.checkForUpdates(GlobalSettings.getSetting(GlobalSettings.VERSION));
			}
		});
	}

	/**
	 * Helper method used to create a button inside a JPanel
	 * 
	 * @param action action associated to that button
	 * @return created component
	 */
	private JComponent createButton(AbstractAction action, int x, int y) {
		JButton button = new JButton(action);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);

		// Calculate dynamic width and height based on scaling
		int width = getButtonWidth();
		int height = getButtonHeight();

		// setBounds dictates exactly where the button goes and how big it is
		button.setBounds(x, y, width, height);

		button.addMouseListener(rollover);

		return button;
	}

	private int getButtonWidth() {
		return (int) (BUTTONSIZE * 3.5 * CommonConstants.widthScaling);
	}

	private int getButtonHeight() {
		return (int) (BUTTONSIZE * 2.0 * CommonConstants.heightScaling);
	}

	/**
	 * Shows a description window
	 * 
	 * @param url   url of html file to be shown inside window
	 * @param title title of the window
	 */
	private void showDescrWin(URL url, String title) {
		if (url != null) {
			QuickHTMLViewer qhv = new QuickHTMLViewer(url, title);
			qhv.centerWindow(CommonConstants.MAX_GUI_WIDTH_STARTSCREEN, CommonConstants.MAX_GUI_HEIGHT_STARTSCREEN);
			qhv.setVisible(true);
			qhv.setIconImage(getIconImage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jmt.framework.gui.components.JMTFrame#doClose()
	 */
	@Override
	protected void doClose() {
		if (sampleQNAni != null) {
			sampleQNAni.stop();
		}
	}

	/**
	 * This class is used to perform rollover on the buttons by changing background
	 */
	public class Rollover extends MouseAdapter {

		private Color normal;
		private Color rollover;

		public Rollover() {
			// Finds colors
			normal = new JButton().getBackground();
			rollover = new Color(181, 189, 214);
		}

		/**
		 * Invoked when the mouse enters a component.
		 */
		@Override
		public void mouseEntered(MouseEvent e) {
			Component source = (Component) e.getSource();
			if (source == upgrade) {
				source.setBackground(upgradeHoverColor);
			} else {
				source.setBackground(rollover);
			}
		}

		/**
		 * Invoked when the mouse exits a component.
		 */
		@Override
		public void mouseExited(MouseEvent e) {
			Component source = (Component) e.getSource();
			if (source == upgrade) {
				source.setBackground(upgradeBackgroundColor);
			} else {
				source.setBackground(normal);
			}
		}

	}

	private Rollover rollover = new Rollover();

	// -----------------------------------------------------------------------------------------------------

	public static void sleep(int amt) // In milliseconds
	{
		long a = System.currentTimeMillis();
		long b = System.currentTimeMillis();
		while ((b - a) <= amt) {
			b = System.currentTimeMillis();
		}
	}

	/**
	 * Main method
	 * 
	 * @param args not used
	 */
	public static void main(String args[]) {
		if (args != null && args.length > 0) {
			String cmd = args[0].trim().toLowerCase();
			if (cmd.equals("sim") || cmd.equals("mva") || cmd.equals("jsimw") || cmd.equals("jsimg") || cmd.equals("jmva")
					|| cmd.equals("-h") || cmd.equals("-help") || cmd.equals("--help") || cmd.equals("/?") || cmd.equals("help")) {
				try {
					jmt.commandline.Jmt.main(args);
					return;
				} catch (Exception e) {
					System.err.println("Error executing command " + args[0] + ": " + e.getMessage());
					e.printStackTrace();
					System.exit(1);
				}
			}
			if (args.length == 1) {
				String lower = args[0].toLowerCase();
				if (lower.endsWith(".jsimg") || lower.endsWith(".jsimg.xml")) {
					JSIMGraphMain.main(args);
					return;
				} else if (lower.endsWith(".jmva")) {
					new JMVAWizard(args[0]);
					return;
				} else if (lower.endsWith(".jsim")) {
					JSIMWizMain.main(args);
					return;
				}
			}
		}

		GraphStartScreen.args = null;
		GraphStartScreen gss = new GraphStartScreen();
		gss.setVisible(true);
		gss.setState(JFrame.NORMAL);
		gss.sleep(100);
		sampleQNAni.start();
	}

	/**
	 * Custom panel to draw the flowchart diagram via code.
	 */
	private class DiagramPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		// =========================================================================
		// TUNABLE BASE OFFSETS & TEXT
		// Tune these raw values to move elements globally. Scaling is handled automatically.
		// =========================================================================

		// --- Button Anchor Y-Levels ---
		private final int BASE_BTN_JSIMWIZ_Y   = 5;
		private final int BASE_BTN_JSIMGRAPH_Y = 107;
		private final int BASE_BTN_JMVA_Y      = 205;
		private final int BASE_BTN_JMCH_Y      = 310;
		private final int BASE_BTN_JABA_Y      = 410;
		private final int BASE_BTN_JWAT_Y      = 510;

		// --- Horizontal Offsets (Distance from the right edge) ---
		private final int OFFSET_START_X    = 430;
		private final int OFFSET_SPLIT_X    = 380;
		private final int OFFSET_METHODS_X  = 280;
		private final int OFFSET_SOLUTION_X = 180;
		private final int OFFSET_DEF_X      = 100; // Shared by both Model Definition diamonds

		// --- Vertical Base Positions ---
		private final int BASE_MAIN_LINE_Y = 285; // Start and Split Y
		private final int BASE_METHODS_Y   = 170;
		private final int BASE_DEFTOP_Y    = 70;
		private final int BASE_DEFMID_Y    = 150;

		// --- Line Turning Point Offsets (Added to a diamond's X-coordinate) ---
		private final int TURN_METHODS_OUT  = 60;
		private final int TURN_DEF_OUT      = 55; // Shared by both Model Definition lines
		private final int TURN_SOLUTION_OUT = 60;

		// --- Diamond Dimensions ---
		private final int DIAMOND_W = 90;
		private final int DIAMOND_H = 60;

		// --- Text Labels ---
		private final String TEXT_MODELS      = "Models";
		private final String TEXT_WORKLOAD    = "Workload";
		private final String TEXT_ANALYSIS    = "Analysis";
		private final String TEXT_SIMULATION  = "Simulation";
		private final String TEXT_ANALYTICAL  = "Analytical";
		private final String TEXT_MVA         = "MVA";
		private final String TEXT_EXACT       = "exact";
		private final String TEXT_APPROXIMATE = "approximate";
		private final String TEXT_TEXTUAL     = "Textual";
		private final String TEXT_GRAPHICAL   = "Graphical";
		private final String TEXT_CLASSROOM = "Classroom";
		private final String TEXT_HELPER = "Helper";
		private final String TEXT_ASYMPTOTIC  = "Asymptotic";
		private final String TEXT_BOUNDS      = "bounds";
		private final String TEXT_METHODS     = "Methods";
		private final String TEXT_SOLUTION    = "Solution";
		private final String TEXT_MODEL_DEF   = "Model\nDefinition";


		public DiagramPanel() {
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			double wScale = CommonConstants.widthScaling;
			double hScale = CommonConstants.heightScaling;

			int btn_height = getButtonHeight();

			// Auto-Scaled Button Y-Levels
			int btn_JSIMwiz_Y   = (int) (BASE_BTN_JSIMWIZ_Y * hScale);
			int btn_JSIMgraph_Y = (int) (BASE_BTN_JSIMGRAPH_Y * hScale);
			int btn_JMVA_Y      = (int) (BASE_BTN_JMVA_Y * hScale);
			int btn_JMCH_Y      = (int) (BASE_BTN_JMCH_Y * hScale);
			int btn_JABA_Y      = (int) (BASE_BTN_JABA_Y * hScale);
			int btn_JWAT_Y      = (int) (BASE_BTN_JWAT_Y * hScale);

			int baseRight = getWidth() - (int)(15 * wScale); // Reference point for X coordinates
			int end_X = baseRight;   // Arrowheads stop here

			// =========================================================================
			// SCALED STRUCTURAL COORDINATES (Computed from config area)
			// =========================================================================

			// --- DOTS ---
			int start_X = baseRight - (int)(OFFSET_START_X * wScale);
			int split_X = baseRight - (int)(OFFSET_SPLIT_X * wScale);
			int mainLine_Y = (int)(BASE_MAIN_LINE_Y * hScale);

			// --- DIAMONDS ---
			int methods_X = baseRight - (int)(OFFSET_METHODS_X * wScale);
			int methods_Y = (int)(BASE_METHODS_Y * hScale);

			int solution_X = baseRight - (int)(OFFSET_SOLUTION_X * wScale);
			int solution_Y = btn_JMCH_Y - (btn_height / 2) - (int)(12 * hScale);

			int def_X = baseRight - (int)(OFFSET_DEF_X * wScale);
			int defTop_Y = (int)(BASE_DEFTOP_Y * hScale);
			int defMid_Y = (int)(BASE_DEFMID_Y * hScale);

			// --- LINE TURNS ---
			int turn_DefOut_X      = def_X + (int)(TURN_DEF_OUT * wScale);
			int turn_SolutionOut_X = solution_X + (int)(TURN_SOLUTION_OUT * wScale);

			// =========================================================================
			// TEXT LABELS (Coordinates for Bottom-Left start of text)
			// =========================================================================
			int txt_Models_X = split_X - (int)(20 * wScale);
			int txt_Models_Y = mainLine_Y - (int)(50 * hScale);

			int txt_Workload_X  = txt_Models_X - (int)(5 * wScale);
			int txt_Workload_Y1 = mainLine_Y + (int)(55 * hScale); // "Workload" (top line)
			int txt_Workload_Y2 = txt_Workload_Y1 + (int)(15 * hScale); // "Analysis" (bottom line)

			int txt_Simulation_X = methods_X - (int)(25 * wScale);
			int txt_Simulation_Y = methods_Y - (int)(60 * hScale);

			int txt_Analytical_X = methods_X - (int)(30 * wScale);
			int txt_Analytical_Y = methods_Y + (int)(70 * hScale);

			int txt_MVA_X      = turn_SolutionOut_X - (int)(75 * wScale);
			int txt_MVA_Y_MVA  = solution_Y - (int)(70 * hScale); // "MVA"
			int txt_MVA_Y_ex   = txt_MVA_Y_MVA + (int)(12 * hScale); // "exact"
			int txt_MVA_Y_app  = txt_MVA_Y_ex + (int)(12 * hScale);  // "approximate"

			int txt_TextualTop_X = turn_DefOut_X - (int)(25 * wScale);
			int txt_TextualTop_Y = btn_JSIMwiz_Y + (btn_height / 2) + (int)(5 * hScale);

			int txt_Graphical_X = turn_DefOut_X - (int)(30 * wScale);
			int txt_Graphical_Y = btn_JSIMgraph_Y + (int)(10 * hScale);

			int txt_TextualMid_X = turn_DefOut_X - (int)(30 * wScale);
			int txt_TextualMid_Y = btn_JMVA_Y - (int)(15 * hScale);

			int txt_Markov_X    = turn_SolutionOut_X + (int)(30 * wScale);
			int txt_Markov_Y1   = solution_Y - (int)(5 * hScale); // "Markov"
			int txt_Markov_Y2   = txt_Markov_Y1 + (int)(15 * hScale);  // "Chain"

			int txt_Asymptotic_X  = solution_X - (int)(30 * wScale);
			int txt_Asymptotic_Y1 = btn_JABA_Y - (btn_height / 2) - (int)(70 * hScale);
			int txt_Asymptotic_Y2 = txt_Asymptotic_Y1 + (int)(15 * hScale); // "bounds"


			// =========================================================================
			// AUTO-WIRING ENGINE (Draws connections and shapes)
			// =========================================================================
			int dW = (int) (DIAMOND_W * wScale);
			int dH = (int) (DIAMOND_H * hScale);
			int dx = dW / 2; // Distance to Left/Right tips
			int dy = dH / 2; // Distance to Top/Bottom tips

			g2d.setColor(new Color(180, 180, 180));
			g2d.setStroke(new BasicStroke(1.2f));

			// Start -> Models Split
			drawPath(g2d, true, start_X, mainLine_Y, split_X - (int)(4 * wScale), mainLine_Y);

			// Models Split -> Directions
			drawPath(g2d, true, split_X, mainLine_Y, split_X, btn_JWAT_Y - (int)(75 * hScale), end_X, btn_JWAT_Y - (int)(75 * hScale)); // down to WAT
			drawPath(g2d, true, split_X, mainLine_Y, split_X, methods_Y, methods_X - dx, methods_Y); // up to Methods

			// --- METHODS DIAMOND OUT ---
			drawPath(g2d, true,
					methods_X, methods_Y - dy,
					methods_X, defTop_Y,
					def_X - dx, defTop_Y
			); // Top Tip -> DefTop
			drawPath(g2d, true,
					methods_X, methods_Y + dy,
					methods_X, solution_Y,
					solution_X - dx, solution_Y
			); // Bottom Tip -> DefMid

			// --- DEFTOP DIAMOND OUT ---
			drawPath(g2d, true,
					def_X, defTop_Y - dy,
					def_X, btn_JSIMwiz_Y + btn_height / 2,
					end_X, btn_JSIMwiz_Y + btn_height / 2
			); // Top Tip -> JSIMwiz
			drawPath(g2d, true,
					def_X, defTop_Y + dy,
					def_X, btn_JSIMgraph_Y + (int)(7 * hScale),
					end_X, btn_JSIMgraph_Y + (int)(7 * hScale)
			); // Bottom Tip -> JSIMgraph

			// --- DEFMID DIAMOND OUT ---
			drawPath(g2d, true,
					def_X, defMid_Y,
					def_X, btn_JMVA_Y - (int)(20 * hScale),
					end_X, btn_JMVA_Y - (int)(20 * hScale)
			); // Right Tip -> JMVA

			// --- SOLUTION DIAMOND OUT ---
			drawPath(g2d, true,
					solution_X + dx, solution_Y,
					end_X, solution_Y
			); // Right Tip -> JMCH
			drawPath(g2d, true,
					solution_X, solution_Y + dy,
					solution_X, btn_JABA_Y - (btn_height / 2) - (int)(32 * hScale),
					end_X, btn_JABA_Y - (btn_height / 2) - (int)(32 * hScale)
			); // Bottom Tip -> JABA
			drawPath(g2d, true,
					solution_X, solution_Y - dy,
					solution_X, defMid_Y,
					def_X - dx, defMid_Y
			); // Top Tip -> DefMid

			// Draw Dots
			g2d.setColor(Color.BLACK);
			g2d.fillOval(start_X - 5, mainLine_Y - 5, 10, 10); // Start dot
			g2d.setColor(Color.WHITE);
			g2d.fillOval(split_X - 4, mainLine_Y - 4, 8, 8); // Split hollow dot
			g2d.setColor(Color.BLACK);
			g2d.drawOval(split_X - 4, mainLine_Y - 4, 8, 8);

			// Draw decision Diamonds (Using extracted text fields)
			drawDiamond(g2d, methods_X, methods_Y, dW, dH, TEXT_METHODS);
			drawDiamond(g2d, solution_X, solution_Y, dW, dH, TEXT_SOLUTION);
			drawDiamond(g2d, def_X, defTop_Y, dW, dH, TEXT_MODEL_DEF);
			drawDiamond(g2d, def_X, defMid_Y, dW, dH, TEXT_MODEL_DEF);

			// Draw Bold Text
			Font mainFont = new Font("Arial", Font.PLAIN, (int) Math.max(12, 14 * wScale));
			Font smallFont = new Font("Arial", Font.PLAIN, (int) Math.max(9, 10 * wScale));

			g2d.setColor(Color.BLACK);
			g2d.setFont(mainFont);

			g2d.drawString(TEXT_MODELS, txt_Models_X, txt_Models_Y);
			g2d.drawString(TEXT_WORKLOAD, txt_Workload_X, txt_Workload_Y1);
			g2d.drawString(TEXT_ANALYSIS, txt_Workload_X, txt_Workload_Y2);
			g2d.drawString(TEXT_SIMULATION, txt_Simulation_X, txt_Simulation_Y);
			g2d.drawString(TEXT_ANALYTICAL, txt_Analytical_X, txt_Analytical_Y);

			g2d.drawString(TEXT_MVA, txt_MVA_X, txt_MVA_Y_MVA);
			g2d.setFont(smallFont);
			g2d.drawString(TEXT_EXACT, txt_MVA_X + (int)(2 * wScale), txt_MVA_Y_ex);
			g2d.drawString(TEXT_APPROXIMATE, txt_MVA_X - (int)(15 * wScale), txt_MVA_Y_app);

			g2d.setFont(mainFont);
			g2d.drawString(TEXT_TEXTUAL, txt_TextualTop_X, txt_TextualTop_Y);
			g2d.drawString(TEXT_GRAPHICAL, txt_Graphical_X, txt_Graphical_Y);
			g2d.drawString(TEXT_TEXTUAL, txt_TextualMid_X, txt_TextualMid_Y);

			g2d.drawString(TEXT_CLASSROOM, txt_Markov_X - (int)(15 * wScale), txt_Markov_Y1);
			g2d.drawString(TEXT_HELPER, txt_Markov_X + (int)(3 * wScale), txt_Markov_Y2);

			g2d.drawString(TEXT_ASYMPTOTIC, txt_Asymptotic_X, txt_Asymptotic_Y1);
			g2d.setFont(smallFont);
			g2d.drawString(TEXT_BOUNDS, txt_Asymptotic_X + (int)(15 * wScale), txt_Asymptotic_Y2);
		}

		// Helper to draw orthogonal connecting paths with an arrowhead at the last point
		private void drawPath(Graphics2D g, boolean arrow, int... coords) {
			for (int i = 0; i < coords.length - 2; i += 2) {
				g.drawLine(coords[i], coords[i+1], coords[i+2], coords[i+3]);
			}
			if (arrow && coords.length >= 4) {
				int x = coords[coords.length - 2];
				int y = coords[coords.length - 1];
				int arrSz = 5;
				Polygon arr = new Polygon();
				arr.addPoint(x, y);
				arr.addPoint(x - arrSz, y - arrSz);
				arr.addPoint(x - arrSz, y + arrSz);
				g.setColor(Color.BLACK);
				g.fillPolygon(arr);
				g.setColor(new Color(180, 180, 180)); // reset line color
			}
		}

		// Helper to draw the decision diamond shape with centered multiline text
		private void drawDiamond(Graphics2D g, int cx, int cy, int w, int h, String text) {
			Polygon p = new Polygon();
			p.addPoint(cx, cy - h/2);
			p.addPoint(cx + w/2, cy);
			p.addPoint(cx, cy + h/2);
			p.addPoint(cx - w/2, cy);

			g.setColor(new Color(232, 238, 247));
			g.fillPolygon(p);
			g.setColor(Color.GRAY);
			g.drawPolygon(p);

			g.setColor(Color.BLACK);
			Font originalFont = g.getFont();
			g.setFont(new Font("Arial", Font.BOLD, (int) (originalFont.getSize() * 1.4f)));
			FontMetrics fm = g.getFontMetrics();
			String[] lines = text.split("\n");
			int yOffset = cy - (fm.getHeight() * lines.length) / 2 + fm.getAscent();
			for (String line : lines) {
				int textW = fm.stringWidth(line);
				g.drawString(line, cx - textW / 2, yOffset);
				yOffset += fm.getHeight();
			}
			g.setFont(originalFont);
			g.setColor(new Color(180, 180, 180));
		}
	}
}
