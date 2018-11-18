package jmt.gui.jwat.workloadAnalysis.clustering.kMean.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import jmt.engine.jwat.ProgressStatusListener;
import jmt.engine.jwat.VariableNumber;
import jmt.engine.jwat.input.ProgressMonitorShow;
import jmt.engine.jwat.workloadAnalysis.clustering.kMean.MainKMean;
import jmt.engine.jwat.workloadAnalysis.utils.ModelWorkloadAnalysis;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.jwat.MainJwatWizard;
import jmt.gui.jwat.workloadAnalysis.panels.ClusterPanel;

public class KMeansOptPanel extends JPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	private ClusterPanel parent;

	private final String KMEANS_DESCRIPTION = HTML_START + HTML_FONT_TITLE + "k-Means Clustering" + HTML_FONT_TIT_END + HTML_FONT_NORM
			+ "Specify maximum number of clusters and maximum number of iterations that the algorithm has to perform" + HTML_FONT_NOR_END + HTML_END;
	private final String KMEANS_OPTIONS_DESCRIPTION = HTML_START + HTML_FONT_NORM + "Transformation to apply<p>"
			+ "to selected variables considered<p>" + "by the algorithm." + HTML_FONT_NOR_END + HTML_END;

	private ProgressStatusListener lst;
	private ModelWorkloadAnalysis model;
	private JRadioButton noneT;
	private JRadioButton minmaxT;
	private JRadioButton stdDevT;
	private JSpinner numOfClust;
	private JSpinner numOfIter;

	public KMeansOptPanel(WizardPanel parent, ProgressStatusListener lst, ModelWorkloadAnalysis model) {
		this.model = model;
		this.lst = lst;
		this.parent = (ClusterPanel) parent;
		setSolveAction();
		initPanel();
	}

	private void initPanel() {
		this.setLayout(new BorderLayout(10, 60));

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		northPanel.add(new JLabel(KMEANS_DESCRIPTION), BorderLayout.CENTER);
		this.add(northPanel, BorderLayout.NORTH);

		JPanel leftPanel = new JPanel(new BorderLayout());
		this.add(leftPanel, BorderLayout.WEST);

		JPanel clustNorth = new JPanel(new FlowLayout(FlowLayout.LEFT));
		clustNorth.add(new JLabel("number of clusters:   "));
		numOfClust = new JSpinner(new SpinnerNumberModel(3, 2, 37, 1));
		clustNorth.add(numOfClust);
		leftPanel.add(clustNorth, BorderLayout.NORTH);

		JPanel iterNorth = new JPanel(new FlowLayout(FlowLayout.LEFT));
		iterNorth.add(new JLabel("number of iterations: "));
		numOfIter = new JSpinner(new SpinnerNumberModel(4, 2, 50, 1));
		iterNorth.add(numOfIter);
		leftPanel.add(iterNorth, BorderLayout.SOUTH);

		JPanel centralPanel = new JPanel(new BorderLayout());

		JPanel transf = new JPanel(new BorderLayout()); //Terzo panel
		transf.setBorder(new TitledBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Transformations")));
		JPanel options = new JPanel(new GridLayout(3, 1)); //Panel ratio buttons
		noneT = new JRadioButton("none");
		minmaxT = new JRadioButton("(value - min) / (max - min)");
		stdDevT = new JRadioButton("(value - mean) / std. dev.");
		ButtonGroup op = new ButtonGroup();
		op.add(noneT);
		minmaxT.setSelected(true);
		op.add(minmaxT);
		op.add(stdDevT);
		options.add(noneT);
		options.add(minmaxT);
		options.add(stdDevT);
		transf.add(options, BorderLayout.EAST);
		transf.add(new JLabel(KMEANS_OPTIONS_DESCRIPTION), BorderLayout.WEST);

		centralPanel.add(transf, BorderLayout.SOUTH);

		this.add(centralPanel, BorderLayout.SOUTH);
	}

	private void setSolveAction() {
		Action buttonAction = new AbstractAction() {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				if (parent.getVarSelected().length >= 2) {
					short trasf = VariableNumber.NONE;
					if (minmaxT.isSelected()) {
						trasf = VariableNumber.MINMAX;
					}
					if (stdDevT.isSelected()) {
						trasf = VariableNumber.STDEV;
					}
					MainKMean eng = new MainKMean(new ProgressMonitorShow(parent, "Processing...", 1), model.getMatrix(), parent.getVarSelected(),
							((Integer) numOfClust.getValue()).intValue(), ((Integer) numOfIter.getValue()).intValue(), trasf);
					eng.addStatusListener(lst);
					eng.start();
				} else {
					JOptionPane.showMessageDialog(parent, "Select at least two variables to proceed with clustering", "Warning",
							JOptionPane.WARNING_MESSAGE);
				}
			}

		};
		((MainJwatWizard) parent.getParentWizard()).setActionButton("Solve", buttonAction);

		Action itemAction = new AbstractAction() {

			private static final long serialVersionUID = 1L;

			{
				putValue(Action.SHORT_DESCRIPTION, "Clusterize");
				putValue(Action.SMALL_ICON, JMTImageLoader.loadImage("Sim"));
				putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
				putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
			}

			public void actionPerformed(ActionEvent e) {
				if (parent.getVarSelected().length >= 2) {
					short trasf = VariableNumber.NONE;
					if (minmaxT.isSelected()) {
						trasf = VariableNumber.MINMAX;
					}
					if (stdDevT.isSelected()) {
						trasf = VariableNumber.STDEV;
					}
					MainKMean eng = new MainKMean(new ProgressMonitorShow(parent, "Processing...", 1), model.getMatrix(), parent.getVarSelected(),
							((Integer) numOfClust.getValue()).intValue(), ((Integer) numOfIter.getValue()).intValue(), trasf);
					eng.addStatusListener(lst);
					eng.start();
				} else {
					JOptionPane.showMessageDialog(parent, "Select at least two variables to proceed with clustering", "Warning",
							JOptionPane.WARNING_MESSAGE);
				}
			}

		};
		((MainJwatWizard) parent.getParentWizard()).setActionMenuBar("Clusterize", itemAction);
		((MainJwatWizard) parent.getParentWizard()).setActionToolBar("Clusterize", itemAction);

		((MainJwatWizard) parent.getParentWizard()).setEnableButton("Solve", true);
	}

}
