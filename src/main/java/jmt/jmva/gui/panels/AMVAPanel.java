/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.jmva.gui.panels;

import jmt.framework.gui.help.HoverHelp;
import jmt.jmva.analytical.ExactModel;
import jmt.jmva.analytical.solvers.SolverAlgorithm;
import jmt.jmva.analytical.solvers.SolverMultiClosedAMVA;
import jmt.jmva.analytical.solvers.multiSolverAlgorithms.SolverMultiClosedMonteCarloLogistic;
import jmt.jmva.gui.JMVAWizard;
import jmt.jmva.gui.utilities.AlgorithmListNameCreator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Panel representing the combo box on ExactWizard (GUI of JMVA)
 *
 * @author Abhimanyu Chugh, Marco Bertoli
 */
public final class AMVAPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String LABEL_ALGORITHM = "Algorithm:";
    private static final String LABEL_ALGORITHM_OPEN = LABEL_ALGORITHM + " QN";
    private static final String LABEL_ALGORITHM_MIXED = LABEL_ALGORITHM + " MVA";
    private static final String LABEL_ALGORITHM_WHATIF = LABEL_ALGORITHM + " (what-if)";

    private static enum PanelStatus {
        ENABLED(LABEL_ALGORITHM, true, false, false),
        ENABLED_TOL(LABEL_ALGORITHM, true, true, false),
        //ENABLED_MAX_SAMPLES(LABEL_ALGORITHM, true, true, true),
        ENABLED_MAX_SAMPLES(LABEL_ALGORITHM, true, false, true),
        DISABLED_WHATIF(LABEL_ALGORITHM_WHATIF, false, false, false);

        private String algorithmLabel;
        private boolean toleranceVisible;
        private boolean selectorVisible;
        private boolean maxSamplesVisible;

        private PanelStatus(String algorithmLabel, boolean selectorVisible, boolean toleranceVisible, boolean maxSamplesVisible) {
            this.algorithmLabel = algorithmLabel;
            this.selectorVisible = selectorVisible;
            this.toleranceVisible = toleranceVisible;
            this.maxSamplesVisible = maxSamplesVisible;
        }
    }

    ;

    private HoverHelp help;
    private NumberFormat numFormat = new DecimalFormat("#.###############");

    private JMVAWizard ew;

    private JLabel tolLabel;
    private JLabel algLabel;
    private JTextField tolerance;
    private JLabel maxSamplesLabel;
    private JTextField maxSamples;
    private JComboBox<String> algorithmList;
    private PanelStatus status = PanelStatus.ENABLED;

    private boolean lastIsClosed = true;
    private boolean lastIsOpen = false;
    private boolean lastIsLD = false;
    private boolean lastIsPriority = false;

    private int currentAlgorithmIndex = 1;
    private SolverAlgorithm currentSelectedAlgorithm = null;

    private ActionListener ACTION_CHANGE_ALGORITHM = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox<?> algorithmList = (JComboBox<?>) e.getSource();
            String algorithm = (String) algorithmList.getSelectedItem();

            // check if algorithm or not
            currentSelectedAlgorithm = SolverAlgorithm.fromString(algorithm);
            if (currentSelectedAlgorithm == null) {
                currentAlgorithmIndex = 1;
                algorithmList.setSelectedIndex(currentAlgorithmIndex);
                currentSelectedAlgorithm = SolverAlgorithm.fromString((String) algorithmList.getItemAt(currentAlgorithmIndex));
            } else {
                currentAlgorithmIndex = algorithmList.getSelectedIndex();
            }
            ew.getData().setAlgorithmType(currentSelectedAlgorithm);
            updateEnabledStatus(currentSelectedAlgorithm);
        }
    };

    private ToleranceInputListener ACTION_CHANGE_TOLERANCE = new ToleranceInputListener();

    private MaxSampleInputListener ACTION_CHANGE_MAX_SAMPLES = new MaxSampleInputListener();

    public AMVAPanel(JMVAWizard ew) {
        super(new BorderLayout());
        this.ew = ew;
        help = ew.getHelp();

        initialize();
    }


    /**
     * Initialize this panel
     */
    private void initialize() {
        JPanel mainPanel = new JPanel(new FlowLayout());
        mainPanel.add(algLabel());
        mainPanel.add(algorithmList());
        mainPanel.add(tolLabel());
        mainPanel.add(tolerance());
        mainPanel.add(maxSampleLabel());
        mainPanel.add(maxSamples());
        this.add(mainPanel, BorderLayout.WEST);
    }

    private JComponent algorithmList() {
        String[] algoNameList = AlgorithmListNameCreator.createAlgoNameList(lastIsClosed, lastIsOpen, lastIsLD, lastIsPriority);
        algorithmList = new JComboBox<String>(algoNameList);

        Dimension d = new Dimension(160, 30);
        algorithmList.setMaximumSize(d);
        algorithmList.setSelectedIndex(1);
        algorithmList.addActionListener(ACTION_CHANGE_ALGORITHM);
        algorithmList.setVisible(status.selectorVisible);
        algorithmList.setRenderer(new DefaultListCellRenderer() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                String str = (value == null) ? "" : value.toString();
                if (SolverAlgorithm.fromString(str) == null) {
                    comp.setEnabled(false);
                    comp.setFocusable(false);
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                } else {
                    comp.setEnabled(true);
                    comp.setFocusable(true);
                }
                return comp;
            }

        });
        help.addHelp(algorithmList, "Algorithm for solving model");
        return algorithmList;
    }

    private JComponent tolLabel() {
        Dimension d = new Dimension(70, 30);
        tolLabel = new JLabel("  Tolerance:");
        tolLabel.setMaximumSize(d);
        tolLabel.setFocusable(false);
        tolLabel.setVisible(status.toleranceVisible);
        return tolLabel;
    }

    private JComponent tolerance() {
        Dimension d = new Dimension(80, 30);
        tolerance = new JTextField(10);
        tolerance.setText(numFormat.format(ew.getData().getTolerance()));
        tolerance.setMaximumSize(d);
        help.addHelp(tolerance, "Input Tolerance for AMVA Algorithms");
        tolerance.setFocusable(true);
        tolerance.addKeyListener(ACTION_CHANGE_TOLERANCE);
        tolerance.addFocusListener(ACTION_CHANGE_TOLERANCE);
        tolerance.setVisible(status.toleranceVisible);
        return tolerance;
    }

    private JComponent maxSampleLabel() {
        Dimension d = new Dimension(70, 30);
        maxSamplesLabel = new JLabel("  Max Samples:");
        maxSamplesLabel.setMaximumSize(d);
        maxSamplesLabel.setFocusable(false);
        maxSamplesLabel.setVisible(status.maxSamplesVisible);
        return maxSamplesLabel;
    }

    private JComponent maxSamples() {
        Dimension d = new Dimension(80, 30);
        maxSamples = new JTextField(10);
        maxSamples.setText(numFormat.format(ew.getData().getMaxSamples()));
        maxSamples.setMaximumSize(d);
        help.addHelp(maxSamples, "Input max samples for Monte Carlo Algorithm");
        maxSamples.setFocusable(true);
        maxSamples.addKeyListener(ACTION_CHANGE_MAX_SAMPLES);
        maxSamples.addFocusListener(ACTION_CHANGE_MAX_SAMPLES);
        maxSamples.setVisible(status.maxSamplesVisible);
        return maxSamples;
    }

    /**
     * Updates the algo panel
     *
     * @param isClosed        true if model is closed, false if not, null to read from data structure
     * @param isOpen          true if model is open, false if not, null to read from data structure
     * @param isAlgowhatif    true if whatif on algorithm was selected, false if not, null to read from data structure
     * @param isLoadDependent if model is load dependent or not. null to read from data structure
     * @param isPriority      if model is priority or not. null to read from data structure
     */
    public void update(Boolean isClosed, Boolean isOpen, Boolean isAlgowhatif, Boolean isLoadDependent, Boolean isPriority) {
        ExactModel data = ew.getData();
        SolverAlgorithm algorithm = data.getAlgorithmType();
        tolerance.setText(numFormat.format(data.getTolerance()));
        maxSamples.setText(numFormat.format(data.getMaxSamples()));
        algorithmList.setSelectedItem(algorithm.toString());
        if (isClosed == null) {
            isClosed = ew.getData().isClosed();
        }
        if (isOpen == null) {
            isOpen = ew.getData().isOpen();
        }
        if (isAlgowhatif == null) {
            isAlgowhatif = ew.getData().isWhatifAlgorithms();
        }
        if (isLoadDependent == null) {
            isLoadDependent = ew.getData().isLd();
        }
        if (isPriority == null) {
            isPriority = ew.getData().isPriority();
        }

        if (isLoadDependent) {
            //updateStatus(PanelStatus.DISABLED_MIXED);
        } else if (isAlgowhatif) {
            updateStatus(PanelStatus.DISABLED_WHATIF);
        } else {
            updateAlgorithmListFromOptions(isClosed, isOpen, isLoadDependent, isPriority);
            updateEnabledStatus(currentSelectedAlgorithm);
        }
    }

    /**
     * Updates the panel status
     *
     * @param newStatus the new panel status
     */
    private void updateStatus(PanelStatus newStatus) {
        if (status == newStatus) {
            return;
        }
        algLabel.setText(newStatus.algorithmLabel);
        algLabel.setEnabled(newStatus != PanelStatus.DISABLED_WHATIF);
        algorithmList.setVisible(newStatus.selectorVisible);
        tolLabel.setVisible(newStatus.toleranceVisible);
        tolerance.setVisible(newStatus.toleranceVisible);
        maxSamplesLabel.setVisible(newStatus.maxSamplesVisible);
        maxSamples.setVisible(newStatus.maxSamplesVisible);
        status = newStatus;
    }

    private void updateEnabledStatus(SolverAlgorithm algorithm) {
        if (!algorithm.isExact()) {
            if (algorithm.equals(SolverAlgorithm.MONTE_CARLO_LOGISTIC)) {
                updateStatus(PanelStatus.ENABLED_MAX_SAMPLES);
            } else if (algorithm.isIterative()) {
                updateStatus(PanelStatus.ENABLED_TOL);
            } else {
                updateStatus(PanelStatus.ENABLED);
            }
        } else {
            updateStatus(PanelStatus.ENABLED);
        }
    }

    private boolean needToUpdateAlgorithmList(boolean isClosed, boolean isOpen, boolean isLD, boolean isPriority) {
        return isClosed != lastIsClosed || isOpen != lastIsOpen || isLD != lastIsLD || isPriority != lastIsPriority;
    }

    private void updateAlgorithmList(String[] newNames) {
        algorithmList.removeActionListener(ACTION_CHANGE_ALGORITHM);
        algorithmList.removeAllItems();
        for (String name : newNames) {
            algorithmList.addItem(name);
        }
        algorithmList.addActionListener(ACTION_CHANGE_ALGORITHM);
    }

    private void updateLastAlgorithmListLastValues(boolean isClosed, boolean isOpen, boolean isLD, boolean isPriority) {
        this.lastIsClosed = isClosed;
        this.lastIsOpen = isOpen;
        this.lastIsLD = isLD;
        this.lastIsPriority = isPriority;
    }

    private void updateCurrentSelectedAlgorithm() {
        algorithmList.setSelectedIndex(currentAlgorithmIndex);
        String selectedAlgoName = algorithmList.getItemAt(currentAlgorithmIndex);
        currentSelectedAlgorithm = SolverAlgorithm.fromString(selectedAlgoName);
        ew.getData().setAlgorithmType(currentSelectedAlgorithm);
        updateEnabledStatus(currentSelectedAlgorithm);
    }

    private void updateCurrentAlgorithmIndex() {
        String selectedAlgoName = algorithmList.getItemAt(currentAlgorithmIndex);
        SolverAlgorithm selectedAlgo = SolverAlgorithm.fromString(selectedAlgoName);

        if (currentSelectedAlgorithm.equals(selectedAlgo)) {
            algorithmList.setSelectedIndex(currentAlgorithmIndex);
            ew.getData().setAlgorithmType(currentSelectedAlgorithm);
            return;
        }

        int numAlgos = algorithmList.getItemCount();
        boolean algorithmInList = false;
        for (int i = 0; i < numAlgos; i++) {
            if (algorithmList.getItemAt(i) != null && algorithmList.getItemAt(i).equals(currentSelectedAlgorithm.toString())) {
                currentAlgorithmIndex = i;
                algorithmInList = true;
                break;
            }
        }
        if (!algorithmInList) {
            currentAlgorithmIndex = 1;
        }

        updateCurrentSelectedAlgorithm();
    }

    private void updateAlgorithmListFromOptions(boolean isClosed, boolean isOpen, boolean isLD, boolean isPriority) {
        if (!needToUpdateAlgorithmList(isClosed, isOpen, isLD, isPriority)) {
            return;
        }
        String[] algoNameList = AlgorithmListNameCreator.createAlgoNameList(isClosed, isOpen, isLD, isPriority);
        updateAlgorithmList(algoNameList);
        updateCurrentAlgorithmIndex();
        updateLastAlgorithmListLastValues(isClosed, isOpen, isLD, isPriority);
    }


    private JComponent algLabel() {
        Dimension d = new Dimension(65, 30);
        algLabel = new JLabel(status.algorithmLabel);
        algLabel.setMaximumSize(d);
        algLabel.setFocusable(false);
        help.addHelp(algLabel, "Algorithm used to solve the model");
        return algLabel;
    }

    private void updateTolerance() {
        ExactModel model = ew.getData();
        Double tol = SolverMultiClosedAMVA.validateTolerance(tolerance.getText());
        if (tol != null) {
            model.setTolerance(tol);
        } else {
            tolerance.setText(numFormat.format(model.getTolerance()));
            JOptionPane.showMessageDialog(ew, "Error: Invalid tolerance value. Using last valid value.", "Input data error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMaxSamples() {
        ExactModel model = ew.getData();
        //Integer mSamples = SolverMultiClosedMonteCarlo.validateMaxSamples(maxSamples.getText());
        Integer mSamples = SolverMultiClosedMonteCarloLogistic.validateMaxSamples(maxSamples.getText());
        if (mSamples != null) {
            model.setMaxSamples(mSamples);
        } else {
            maxSamples.setText(numFormat.format(model.getMaxSamples()));
            JOptionPane.showMessageDialog(ew, "Error: Invalid max samples value. Using last valid value.", "Input data error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class ToleranceInputListener implements KeyListener, FocusListener {
        @Override
        public void focusLost(FocusEvent e) {
            updateTolerance();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                updateTolerance();
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

    private class MaxSampleInputListener implements KeyListener, FocusListener {
        @Override
        public void focusLost(FocusEvent e) {
            updateMaxSamples();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                updateMaxSamples();
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

}
