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

package jmt.gui.common.editors;

import jmt.framework.gui.listeners.KeyFocusAdapter;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.definitions.VacationParameters;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Exponential;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;


public class ServerVacationEditor extends WizardPanel implements CommonConstants {

    private static final long serialVersionUID = 1L;

    //Interfaces for model data exchange
    protected ClassDefinition classData;

    protected StationDefinition stationData;

    private Object stationKey;

    private JComboBox vacationPolicyCombo;
    private JPanel policyComboPanel;
    private JCheckBox enablingCheckBox;

    protected static final String[] vacationPolicies =
            {
                    STATION_VACATION_POLICY_SINGLE,
                    STATION_VACATION_POLICY_MULTIPLE,
                    STATION_VACATION_POLICY_BERNOULLI
            };

    protected VacationParametersPanel vacationParameterPanel;

    public ServerVacationEditor(StationDefinition stations, ClassDefinition classes, Object stationKey) {
        setData(classes, stations, stationKey);
        initComponents();
    }

    private void initComponents() {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BorderLayout());
        descriptionPanel.setBorder(new TitledBorder(new EtchedBorder(), "Description"));
        JLabel serverTypesText = new JLabel(SERVER_VACATION_DESCRIPTION);
        descriptionPanel.add(serverTypesText, BorderLayout.CENTER);
        descriptionPanel.setMaximumSize(descriptionPanel.getPreferredSize());

        //build vacation policy panel
        JPanel vacationPolicyPanel = new JPanel();
        vacationPolicyPanel.setBorder(new TitledBorder(new EtchedBorder(), "Server Vacation Policy"));
        vacationPolicyPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.gridy = 0;
        gbc.weighty = 0;
        vacationPolicyPanel.add(enablingCheckBox, gbc);

        policyComboPanel = new JPanel();
        policyComboPanel.add(Box.createRigidArea(new Dimension((int) (5 * CommonConstants.widthScaling), (int) (0 * CommonConstants.heightScaling))));
        policyComboPanel.add(new JLabel("Vacation Policy:"));
        policyComboPanel.add(vacationPolicyCombo);
        ++gbc.gridy;
        vacationPolicyPanel.add(policyComboPanel, gbc);

        //build vacation parameters panel
        VacationParametersPanel vacationParameterPanel = new VacationParametersPanel();
        this.vacationParameterPanel = vacationParameterPanel;
        String currentVacationPolicy = (String) vacationPolicyCombo.getSelectedItem();
        vacationParameterPanel.updatePolicy(currentVacationPolicy);
        ++gbc.gridy;
        gbc.weighty = 1.0;
        vacationPolicyPanel.add(vacationParameterPanel, gbc);

        if (!enablingCheckBox.isSelected()) {
            setAllEnabled(policyComboPanel, false);
            setAllEnabled(vacationParameterPanel, false);
        }

        this.add(descriptionPanel);
        this.add(vacationPolicyPanel);
    }

    private void setAllEnabled(Container container, boolean isEnabled) {
        if (container == null) {
            return;
        }
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                setAllEnabled((Container) component, isEnabled);
            } else {
                component.setEnabled(isEnabled);
            }
        }
        container.setEnabled(isEnabled);
    }


    /**
     * Updates data contained in this panel's components
     */
    public void setData(ClassDefinition classes, StationDefinition stations, Object stKey) {
        classData = classes;
        stationData = stations;
        stationKey = stKey;
        vacationPolicyCombo = new JComboBox();
        vacationPolicyCombo.setModel(new DefaultComboBoxModel(vacationPolicies));
        enablingCheckBox = new JCheckBox("Enable server vacation");

        addDataManagers();
        updateServerPreferences();
        refreshComponents();
    }

    private void updateServerPreferences() {
        String vacationPolicy = stationData.getStationVacationPolicy(stationKey);
        enablingCheckBox.setSelected(!vacationPolicy.equals(STATION_VACATION_POLICY_NONE));
        int index = 0;
        for (int i = 0; i < vacationPolicies.length; i++) {
            if (vacationPolicies[i].equals(vacationPolicy)) {
                index = i;
                break;

            }
        }
        vacationPolicyCombo.setSelectedIndex(index);
        if (vacationPolicies[index].equals(STATION_VACATION_POLICY_BERNOULLI) && stationData.getStationNumberOfServers(stationKey) != 1) {
            // Bernoulli vacation is currently only supported when the server number is 1
            stationData.setStationVacationPolicy(stationKey, STATION_VACATION_POLICY_NONE);
            enablingCheckBox.setSelected(false);
        }
    }

    private void addDataManagers() {
        vacationPolicyCombo.addActionListener(e -> {
            updateVacationPolicy();
        });

        enablingCheckBox.addItemListener(e -> {
            boolean selected = enablingCheckBox.isSelected();
            setAllEnabled(policyComboPanel, selected);
            setAllEnabled(vacationParameterPanel, selected);

            if (!selected) {
                stationData.setStationVacationPolicy(stationKey, STATION_VACATION_POLICY_NONE);
            } else {
                updateVacationPolicy();
            }
        });
    }

    private void updateVacationPolicy() {
        if (!enablingCheckBox.isSelected()) {
            return;
        }
        String vacationPolicy = (String) vacationPolicyCombo.getSelectedItem();
        if (vacationPolicy == null) {
            vacationPolicy = STATION_VACATION_POLICY_SINGLE;
        }
        if (vacationPolicy.equals(STATION_VACATION_POLICY_BERNOULLI) && stationData.getStationNumberOfServers(stationKey) != 1) {
            // Bernoulli policy is currently only supported when server number is 1
            if (vacationParameterPanel != null) {
                vacationParameterPanel.updatePolicy(vacationPolicy);
            }
            return;
        }
        stationData.setStationVacationPolicy(stationKey, vacationPolicy);
        if (vacationParameterPanel != null) {
            vacationParameterPanel.updatePolicy(vacationPolicy);
        }
    }

    /**
     * called by the Wizard when the panel becomes active
     */
    @Override
    public void gotFocus() {
        refreshComponents();
    }

    /**
     * called by the Wizard before when switching to another panel
     */
    @Override
    public void lostFocus() {
    }

    @Override
    public void repaint() {
        refreshComponents();
        super.repaint();
    }

    private void refreshComponents() {
    }


    @Override
    public String getName() {
        return "Server Vacation";
    }

    protected class VacationParametersPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JLabel warningNote;
        private final JPanel bernoulliPPanel;
        private final JPanel vacationLengthPanel;

        public VacationParametersPanel() {
            setLayout(new GridBagLayout());
            setBorder(new TitledBorder(new EtchedBorder(), "Vacation Parameters"));

            ImageIcon warningIcon = JMTImageLoader.loadImage("triangle");
            warningNote = new JLabel("Note: Bernoulli Vacation Strategy is currently only supported when " +
                    "the 'Number of Servers' option in the 'Server Configuration' section is set to 1.",
                    warningIcon, JLabel.LEFT);

            bernoulliPPanel = new JPanel();
            bernoulliPPanel.setLayout(new BorderLayout());
            JLabel pLabel = new JLabel("p:");
            pLabel.setPreferredSize(new Dimension((int) (65 * CommonConstants.widthScaling), (int) (24 * CommonConstants.heightScaling)));
            bernoulliPPanel.add(pLabel, BorderLayout.WEST);
            JTextField batchSizeField = new JTextField(10);
            bernoulliPPanel.add(batchSizeField, BorderLayout.CENTER);
            DoubleValueAdaptor pAdaptor = new DoubleValueAdaptor() {
                @Override
                boolean checkValue(double value) throws IllegalArgumentException {
                    if (value < 0.0 || value > 1.0) {
                        throw new IllegalArgumentException("p must be in the range [0,1]");
                    }
                    return true;
                }

                @Override
                void updateValue(double value) {
                    stationData.getStationVacationParameters(stationKey).setBernoulliP(value);
                }
            };
            batchSizeField.addFocusListener(pAdaptor);
            batchSizeField.addKeyListener(pAdaptor);

            vacationLengthPanel = new JPanel();
            vacationLengthPanel.setLayout(new BorderLayout());
            JLabel vacationLengthLabel = new JLabel("Vacation Length:");
            JPanel paddedLabel = new JPanel();
            paddedLabel.setLayout(new BoxLayout(paddedLabel, BoxLayout.Y_AXIS));
            paddedLabel.add(vacationLengthLabel);
            paddedLabel.add(Box.createVerticalStrut(5)); // Padding
            vacationLengthPanel.add(paddedLabel, BorderLayout.NORTH);
            JButton editButton = getEditButton();
            vacationLengthPanel.add(editButton, BorderLayout.WEST);
            JTextField distributionValueTextField = new JTextField();
            distributionValueTextField.setEditable(false);
            vacationLengthPanel.add(distributionValueTextField, BorderLayout.CENTER);
        }

        private JButton getEditButton() {
            JButton but = new JButton("Edit");
            but.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Distribution distribution = stationData.getStationVacationParameters(stationKey).getVacationTimeDistribution();
                    DistributionsEditor editor = DistributionsEditor.getInstance(getParent(), distribution, false);
                    editor.setTitle("Editing Vacation Length");
                    editor.show();
                    Distribution result = editor.getResult();
                    stationData.getStationVacationParameters(stationKey).setVacationTimeDistribution(result);
                    refreshValues();
                }
            });
            but.setText("Edit");
            but.setPreferredSize(new Dimension((int) (65 * CommonConstants.widthScaling), (int) (24 * CommonConstants.heightScaling)));
            return but;
        }

        public void updatePolicy(String currentVacationPolicy) {
            removeAll();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0;
            gbc.insets = new Insets(10, 10, 10, 10);

            switch (currentVacationPolicy) {
                case STATION_VACATION_POLICY_SINGLE:
                case STATION_VACATION_POLICY_MULTIPLE:
                    add(vacationLengthPanel, gbc);
                    ++gbc.gridy;
                    gbc.weighty = 1.0;
                    add(Box.createVerticalGlue(), gbc);
                    break;
                case STATION_VACATION_POLICY_BERNOULLI:
                    if (stationData.getStationNumberOfServers(stationKey) != 1) {
                        gbc.fill = GridBagConstraints.NONE;
                        add(warningNote, gbc);
                    } else {
                        add(bernoulliPPanel, gbc);
                        ++gbc.gridy;
                        add(vacationLengthPanel, gbc);
                        ++gbc.gridy;
                        gbc.weighty = 1.0;
                        add(Box.createVerticalGlue(), gbc);
                    }
                    break;
                default:
                    add(warningNote);
            }
            refreshValues();
        }

        public void refreshValues() {
            VacationParameters parameters = stationData.getStationVacationParameters(stationKey);

            for (Component component : bernoulliPPanel.getComponents()) {
                if (component instanceof JTextField) {
                    JTextField textField = (JTextField) component;
                    if (parameters.getBernoulliP() != null) {
                        textField.setText(String.valueOf(parameters.getBernoulliP()));
                    } else {
                        double defaultP = 1.0;
                        parameters.setBernoulliP(defaultP);
                        textField.setText(Double.toString(defaultP));
                    }
                }
            }

            for (Component component : vacationLengthPanel.getComponents()) {
                if (component instanceof JTextField) {
                    JTextField textField = (JTextField) component;
                    if (parameters.getVacationTimeDistribution() != null) {
                        textField.setText(parameters.getVacationTimeDistribution().toString());
                    } else {
                        Distribution defaultDistribution = new Exponential();
                        parameters.setVacationTimeDistribution(defaultDistribution);
                        textField.setText(defaultDistribution.toString());
                    }
                }
            }

            revalidate();
            repaint();
        }


        abstract class DoubleValueAdaptor extends KeyFocusAdapter {
            @Override
            protected void updateValues(ComponentEvent e) {
                // Get the textfield
                JTextField sourcefield = (JTextField) e.getSource();
                try {
                    if (sourcefield.getText().isEmpty()) {
                        sourcefield.setText("0");
                    }
                    // Get the value entered in the textfield
                    Double value = Double.parseDouble(sourcefield.getText());
                    if (checkValue(value)) {
                        updateValue(value);
                    }
                } catch (NumberFormatException ex) {
                    // If user enters a value that is not a number -> pop up a warning and reset
                    // back to the value before
                    JOptionPane.showMessageDialog(null, "Please enter a valid double value.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                } catch (IllegalArgumentException ex) {
                    // If user enters an invalid value -> pop up a warning and reset
                    // back to the value before
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Invalid Input", JOptionPane.WARNING_MESSAGE);
                }

                refreshValues();
            }

            abstract boolean checkValue(double value) throws IllegalArgumentException;

            abstract void updateValue(double value);
        }
    }
}
