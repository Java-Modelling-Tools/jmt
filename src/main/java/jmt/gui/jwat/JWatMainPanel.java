package jmt.gui.jwat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import jmt.framework.gui.components.JMTMenuBar;
import jmt.framework.gui.components.JMTToolBar;
import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.listeners.AbstractJMTAction;
import jmt.framework.gui.listeners.MenuAction;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.panels.AboutDialogFactory;
import jmt.manual.ChapterIdentifier;
import jmt.manual.PDFViewer;

public class JWatMainPanel extends WizardPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Start screen image
	private static final String IMG_STARTSCREEN = "StartScreenJWat";
	//JWAT tool icons
	private static final String IMG_TRAFFIC_ICON = "TrafficIcon";
	private static final String IMG_SAVE_ICON = "Open";
	private static final String IMG_PATH_ICON = "PathIcon";
	private static final String IMG_FITTING_ICON = "FittingIcon";
	private static final String IMG_WL_ICON = "WorkLoadIcon";
	//Tool buttons size
	private static final int BUTTONSIZE = 25;
	//Rollover help
	private Rollover rollover = new Rollover();
	private HoverHelp help;
	private MainJwatWizard parent = null;

	protected AbstractAction startWLA = new AbstractAction("") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		{
			putValue(Action.SHORT_DESCRIPTION, "Workload Analysis");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage(IMG_WL_ICON, new Dimension(BUTTONSIZE + 10, BUTTONSIZE + 10)));
		}

		public void actionPerformed(ActionEvent e) {
			parent.setWorkloadEnv("load");
		}
	};
	protected AbstractAction startFitting = new AbstractAction("") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		{
			putValue(Action.SHORT_DESCRIPTION, "Fitting Workload");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage(IMG_FITTING_ICON, new Dimension(BUTTONSIZE + 10, BUTTONSIZE + 10)));
		}

		public void actionPerformed(ActionEvent e) {
			parent.setFittingEnv("load");
		}
	};
	protected AbstractAction startTraffic = new AbstractAction("") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		{
			putValue(Action.SHORT_DESCRIPTION, "Traffic Analysis - Burstiness");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage(IMG_TRAFFIC_ICON, new Dimension(BUTTONSIZE + 10, BUTTONSIZE + 10)));
		}

		public void actionPerformed(ActionEvent e) {
			parent.setTrafficEnv();
		}
	};
	protected AbstractAction startLoadWorkload = new AbstractAction("") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		{
			putValue(Action.SHORT_DESCRIPTION, "Open a demo file for workload analysis");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage(IMG_SAVE_ICON, new Dimension(BUTTONSIZE + 10, BUTTONSIZE + 10)));
		}

		public void actionPerformed(ActionEvent e) {
			parent.setWorkloadEnv("demo");
		}
	};
	protected AbstractAction startLoadFitting = new AbstractAction() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		{
			putValue(Action.SHORT_DESCRIPTION, "Open a demo file for fitting workload");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage(IMG_SAVE_ICON, new Dimension(BUTTONSIZE + 10, BUTTONSIZE + 10)));
		}

		public void actionPerformed(ActionEvent e) {
			parent.setFittingEnv("demo");
		}
	};
	//Arrays of abstract actions for tool buttons
	private AbstractAction[] buttonAction = { startWLA, startFitting,  startTraffic, startLoadWorkload, startLoadFitting  };

	/**
	 * Helper method used to create a button inside a JPanel
	 * @param action action associated to that button
	 * @return created component
	 */
	private JComponent createButton(AbstractAction action) {
		JPanel panel = new JPanel(); // Use gridbag as centers by default
		JButton button = new JButton(action);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setPreferredSize(new Dimension((int) (BUTTONSIZE * 3.5), (BUTTONSIZE * 2)));
		button.addMouseListener(rollover);
		//if (action == buttonAction[4]) {
		//	button.setVisible(false);
		//}
		//if (action == buttonAction[0]) {
		//	button.setEnabled(false);
		//}
		//if (action == buttonAction[2]) button.setEnabled(false);
		//if (action == buttonAction[4]) button.setEnabled(false);
		panel.add(button);
		return panel;
	}

	/**
	 * This class is used to perform rollover on the buttons by changing background
	 */
	public class Rollover extends MouseAdapter {
		private Color normal;
		private Color rollover;

		public Rollover() {
			normal = new JButton().getBackground();
			rollover = new Color(83, 126, 126);
		}

		/**
		 * Invoked when the mouse enters a component.
		 */
		@Override
		public void mouseEntered(MouseEvent e) {
			((Component) e.getSource()).setBackground(rollover);
		}

		/**
		 * Invoked when the mouse exits a component.
		 */
		@Override
		public void mouseExited(MouseEvent e) {
			((Component) e.getSource()).setBackground(normal);
		}
	}

	public JWatMainPanel(MainJwatWizard parent) {
		this.parent = parent;
		this.help = parent.getHelp();
		this.setLayout(new BorderLayout());
		JPanel upper = new JPanel(new FlowLayout());
		JLabel upperLabel = new JLabel();
		upperLabel.setPreferredSize(new Dimension(300, 10));
		upper.add(upperLabel);

		JPanel bottom = new JPanel(new FlowLayout());
		JLabel bottomLabel = new JLabel();
		bottomLabel.setPreferredSize(new Dimension(300, 10));
		bottom.add(bottomLabel);

		this.add(upper, BorderLayout.NORTH);
		this.add(bottom, BorderLayout.SOUTH);

		JPanel eastPanel = new JPanel(new FlowLayout());
		eastPanel.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
		JPanel buttonPanel = new JPanel(new GridLayout(buttonAction.length, 1, 2, 15));
		eastPanel.add(buttonPanel, BorderLayout.CENTER);
		for (AbstractAction element : buttonAction) {
			buttonPanel.add(createButton(element));
		}
		JLabel imageLabel = new JLabel();
		imageLabel.setBorder(BorderFactory.createEmptyBorder(BUTTONSIZE - 5, 1, 0, 0));
		imageLabel.setIcon(JMTImageLoader.loadImage(IMG_STARTSCREEN, new Dimension(400, 315)));
		imageLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		imageLabel.setVerticalAlignment(SwingConstants.NORTH);

		//JLabel description = new JLabel("<html><body><h3>This is a simple<br>descirption added to this<br>page. Please do not mind it<br>will be replaced soon</h3></body></html>");
		//this.add(description,BorderLayout.WEST);
		this.add(imageLabel, BorderLayout.CENTER);
		this.add(eastPanel, BorderLayout.EAST);
		makeToolbar();
		makeMenubar();
	}

	@Override
	public String getName() {
		return "Main Panel";
	}

	private AbstractJMTAction HELP = new AbstractJMTAction("JWAT Help") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Show JWAT help");
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

	private AbstractJMTAction HELP_CREDITS = new AbstractJMTAction("About JWAT") {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		{
			putValue(Action.SHORT_DESCRIPTION, "About JWAT");
		}

		public void actionPerformed(ActionEvent e) {
			AboutDialogFactory.showJWAT(parent);
		}
	};

	/**
	 * Creates JWAT toolbar
	 */
	public void makeToolbar() {
		JMTToolBar toolbar = new JMTToolBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] items = new AbstractJMTAction[] { HELP };
		toolbar.populateToolbar(items);
		toolbar.setFloatable(false);
		parent.setToolBar(toolbar);
	}

	/**
	 * Creates JWAT menubar
	 */
	public void makeMenubar() {
		JMTMenuBar menuBar = new JMTMenuBar(JMTImageLoader.getImageLoader());
		AbstractJMTAction[] menus = new AbstractJMTAction[] {
				//Help menu
				new MenuAction("Help", new AbstractJMTAction[] { HELP, null, HELP_CREDITS }) };
		menuBar.populateMenu(menus);
		parent.setMenuBar(menuBar);
	}

	@Override
	public void gotFocus() {
		if (parent.getModel() != null && parent.getModel().getMatrix() != null) {
			if (JOptionPane.showConfirmDialog(this, "This operation resets all data. Continue?", "WARNING", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				parent.getSession().resetSession();
				parent.resetScreen();
				((JWatWizard) getParentWizard()).setEnableButton("Next >", false);
				((JWatWizard) getParentWizard()).setEnableButton("Solve", false);
			} else {
				parent.setLastPanel();
			}
		}
	}

}
