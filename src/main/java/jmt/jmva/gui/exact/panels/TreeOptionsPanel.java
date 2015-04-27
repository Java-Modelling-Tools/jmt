/**    
  * Copyright (C) 2006, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.

  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */

package jmt.jmva.gui.exact.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;

import QueuingNet.TreeAlgorithms.Config;
import QueuingNet.TreeAlgorithms.Printer;
import QueuingNet.TreeAlgorithms.TreePlanter;
import QueuingNet.TreeAlgorithms.TreePlanter.PlantMode;
import jmt.jmva.analytical.SolverAlgorithm;
import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.jmva.gui.exact.ExactModel;
import jmt.jmva.gui.exact.ExactConstants;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;
import jmt.gui.exact.table.ResultsTable;

/**
 * Panel for showing tree algorithm configurable options.
 * @author Ben Homer, 2014.
 *
 */
public class TreeOptionsPanel extends WizardPanel {
	private static final long serialVersionUID = 1L;
	
	private static final double MAX_RECOMMENDED_SPARSITY = 0.3;
	
	private ExactModel data;

	private JPanel options;
	private JPanel generalStats;
	
	private JComboBox<String> plantModesDropdown;
	private JComboBox<String> comparisonDropdown;
	private JComboBox<String> traversalDropdown;
	private JComboBox<String> noThreadsDropdown;
	private JCheckBox cbTermination;
	private JButton closeBtn;
	private JLabel heapUsage;
	private JLabel desc;

	ExactWizard ew;

	public TreeOptionsPanel(ExactWizard ew) {
		this.ew = ew;
		
		initComponents();
	}

	/**
	 * Set up the panel contents and layout
	 */
	protected void initComponents() {
//		table = new ResultsTable(getTableModel(), help);

		this.setSize(400, 800);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		initOptions();
	}
	
	private void initOptions() {
		this.setLayout(new BorderLayout());
		options = new JPanel();
		generalStats = new JPanel();
		int availableCores = Runtime.getRuntime().availableProcessors();
		
		options.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		//generalStats.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		options.setLayout(new BoxLayout(options, BoxLayout.PAGE_AXIS));
		generalStats.setLayout(new BoxLayout(generalStats, BoxLayout.PAGE_AXIS));
		
		// Options panel
		// Plant mode
		String[] plantModes = { "Automatic", "Heuristic", "Simple", "Sequential" };
		plantModesDropdown = new JComboBox<String>(plantModes);
		int idx = Config.COMPARE_ALL_HEURISTICS ? 0 : Config.TREE_PLANT_MODE.ordinal() + 1;
		plantModesDropdown.setSelectedIndex(idx);
		plantModesDropdown.addActionListener(APPLY_CHANGES_LISTENER);
		plantModesDropdown.addFocusListener(FOCUS_LISTENER);
		this.addOption("Tree plant mode: ", plantModesDropdown);
		
		// Comparison basis
		String[] comparisons = { "Time", "Space", "Time & Space", "Partially covered classes" };
		comparisonDropdown = new JComboBox<String>(comparisons);
		comparisonDropdown.setSelectedIndex(Config.COMPLEXITY_CHECK_LEVEL);
		comparisonDropdown.setEnabled(Config.COMPARE_ALL_HEURISTICS);
		comparisonDropdown.addActionListener(APPLY_CHANGES_LISTENER);
		comparisonDropdown.addFocusListener(FOCUS_LISTENER);
		this.addOption("Comparison basis: ", comparisonDropdown);
		
		// Traversal type
		String[] traversals = { "Recursive", "Iterative" };
		traversalDropdown = new JComboBox<String>(traversals);
		traversalDropdown.setSelectedIndex(Config.RECURSIVE ? 0 : 1);
		traversalDropdown.addActionListener(APPLY_CHANGES_LISTENER);
		traversalDropdown.addFocusListener(FOCUS_LISTENER);
		this.addOption("Traversal type: ", traversalDropdown);
		
		// Termination
		cbTermination = new JCheckBox();
		cbTermination.setSelected(Config.TERMINATE_IF_HIGH_COMPLEXITY);
		cbTermination.addMouseListener(APPLY_CHANGES_LISTENER);
		cbTermination.addFocusListener(FOCUS_LISTENER);
		this.addOption("Terminate if high complexity: ", cbTermination);
		
		// Number of threads
		String[] noThreads = new String[availableCores];
		for (int i = 1; i <= availableCores; i++) {
			noThreads[i-1] = i + "";
		}
		noThreadsDropdown = new JComboBox<String>(noThreads);
		noThreadsDropdown.setSelectedIndex(Config.NUM_THREADS-1);
		noThreadsDropdown.addActionListener(APPLY_CHANGES_LISTENER);
		noThreadsDropdown.addFocusListener(FOCUS_LISTENER);
		this.addOption("No. threads: ", noThreadsDropdown);
		
		this.add(options, BorderLayout.EAST);
		
		// General stats panel
		Box descrBox = Box.createVerticalBox();
		descrBox.add(new JLabel(ExactConstants.DESCRIPTION_TREEALG_OPTIONS));
		generalStats.add(descrBox);
		
		generalStats.add(new JLabel(" "));
		desc = new JLabel("");
		generalStats.add(desc);
		
		int mb = 1024*1024;
        Runtime runtime = Runtime.getRuntime();
        heapUsage = new JLabel("Heap usage: " + (runtime.totalMemory()-runtime.freeMemory()) / mb + "MB / " + runtime.totalMemory() / mb + "MB");
        generalStats.add(new JLabel(" "));
        generalStats.add(new JLabel(ExactConstants.DESCRIPTION_TREEALG_JVM_STATS));
        generalStats.add(new JLabel("Available cores: " + availableCores));
        generalStats.add(heapUsage);
        generalStats.add(new JLabel("Max heap size: " + runtime.maxMemory() / mb + "MB"));
        generalStats.add(new JLabel(" "));
		Timer timer = new Timer(1000, TIMER_UPDATE_LISTENER);
		timer.start();
		
		// Tree Evaluation panel
		generalStats.add(new JLabel(ExactConstants.DESCRIPTION_TREEALG_MODEL_STATS));
//		evaluateBtn = new JButton("Evaluate");
//		evaluationStats.add(evaluateBtn);
//		evaluateBtn.addMouseListener(APPLY_CHANGES_LISTENER);
		
		double meanSparsity = this.getSparsityIndex();
		generalStats.add(new JLabel("Mean sparsity: " + convertDoubleToString(meanSparsity)));
		if (meanSparsity > MAX_RECOMMENDED_SPARSITY && ew.getData().getClasses() > 1) {
			generalStats.add(new JLabel("Comment: Model may be solved"));
			generalStats.add(new JLabel("faster by a sequential solver."));
		} else {
			generalStats.add(new JLabel("Comment: Model is suitable"));
			generalStats.add(new JLabel("for solution by a tree algorithm."));
		}
		
		this.add(generalStats, BorderLayout.WEST);
		
		// Close button
		closeBtn = new JButton("Close");
		closeBtn.addMouseListener(APPLY_CHANGES_LISTENER);
		this.add(closeBtn, BorderLayout.SOUTH);
	}
	
	private String convertDoubleToString(double d) {
		DecimalFormat f = new DecimalFormat("#.##");
		return f.format(d);
	}
	
	private void addOption(String label, Component obj) {
		JPanel pnl = new JPanel();
		JLabel lbl = new JLabel(label);
		pnl.add(lbl);
		pnl.add(obj);
		options.add(pnl);
	}

	@Override
	public void gotFocus() {
//		sync();
//		table.updateStructure();
	}
	
	@Override
	public String getName() {
		return "TreeOptions";
	}

	private ApplyChangesListener APPLY_CHANGES_LISTENER = new ApplyChangesListener();
	private UpdateDescriptionListener FOCUS_LISTENER = new UpdateDescriptionListener();
	
	private TimerUpdateListener TIMER_UPDATE_LISTENER = new TimerUpdateListener();
	
	private void update(Object c) {
		if (c.equals(plantModesDropdown)) {
			int idx = plantModesDropdown.getSelectedIndex();
			if (idx == 0) {
				Config.COMPARE_ALL_HEURISTICS = true;
				comparisonDropdown.setEnabled(true);
				return;
			}
			
			Config.TREE_PLANT_MODE = PlantMode.values()[idx-1];
			Config.COMPARE_ALL_HEURISTICS = false;
			comparisonDropdown.setEnabled(false);
			
		} else if (c.equals(comparisonDropdown)) {
			Config.COMPLEXITY_CHECK_LEVEL = comparisonDropdown.getSelectedIndex();
		} else if (c.equals(traversalDropdown)) {
			Config.RECURSIVE = traversalDropdown.getSelectedIndex() == 0;
		} else if (c.equals(cbTermination)) {
			Config.TERMINATE_IF_HIGH_COMPLEXITY = cbTermination.isSelected();
		} else if (c.equals(noThreadsDropdown)) {
			Config.NUM_THREADS = noThreadsDropdown.getSelectedIndex() + 1;
		} else if (c.equals(closeBtn)) {
			Window w = SwingUtilities.getWindowAncestor(this);
			w.setVisible(false);
		}
	}
	
	private void updateDescription(Object c) {
		if (c.equals(plantModesDropdown)) {
			desc.setText(ExactConstants.DESCRIPTION_TREEALG_OPTIONS_TREE_PLANT_MODE);
		} else if (c.equals(comparisonDropdown)) {
			desc.setText(ExactConstants.DESCRIPTION_TREEALG_OPTIONS_COMPARISON_BASIS);
		} else if (c.equals(traversalDropdown)) {
			desc.setText(ExactConstants.DESCRIPTION_TREEALG_OPTIONS_TRAVERSAL_TYPE);
		} else if (c.equals(cbTermination)) {
			desc.setText(ExactConstants.DESCRIPTION_TREEALG_OPTIONS_TERMINATE_IF_HIGH_COMPLEXITY);
		} else if (c.equals(noThreadsDropdown)) {
			desc.setText(ExactConstants.DESCRIPTION_TREEALG_OPTIONS_NO_OF_THREADS);
		} 
	}
	
	private void updateStats() {
		int mb = 1024*1024;
        Runtime runtime = Runtime.getRuntime();
//        generalStats.remove(heapUsage);
        heapUsage.setText("Heap usage: " + (runtime.totalMemory()-runtime.freeMemory()) / mb + "MB / " + runtime.totalMemory() / mb + "MB");
//        generalStats.add(heapUsage);
        this.redraw();
	}
	
	protected double getSparsityIndex() {
		ExactModel md = ew.getData();
		double[] perClassSparsity = new double[md.getClasses()];
		double meanSparsity = 0;
		for (int r = 0; r < md.getClasses(); r++) {
			for (int m = 0; m < md.getStations(); m++) {
				perClassSparsity[r] += md.getServiceTimes()[m][r][0] > 0 ? 1 : 0;
			}
			perClassSparsity[r] /= md.getStations();
			meanSparsity += perClassSparsity[r];
		}
		
		return meanSparsity / md.getClasses();
	}
	
	private class TimerUpdateListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			updateStats();
		}
	}
	
	private class ApplyChangesListener implements MouseListener, ActionListener {

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			update(e.getComponent());
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			update(e.getSource());
		}
	}
	
	private class UpdateDescriptionListener implements FocusListener {

		@Override
		public void focusGained(FocusEvent e) {
			updateDescription(e.getSource());
		}

		@Override
		public void focusLost(FocusEvent e) {
		}
	}

}
