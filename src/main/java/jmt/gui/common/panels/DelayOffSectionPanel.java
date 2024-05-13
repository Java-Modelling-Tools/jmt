package jmt.gui.common.panels;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.editors.DistributionsEditor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DelayOffSectionPanel extends WizardPanel implements CommonConstants {

    private JCheckBox enableDelayOff;
    private JLabel setupTimeDistributionLabel;
    private JLabel delayOffTimeDistributionLabel;
    protected StationDefinition data;
    protected ClassDefinition classData;
    protected Object stationKey;
    private Distribution setupTimeDistribution;
    private Distribution delayOffTimeDistribution;
    private JPanel delayOffConfigPanel; // Panel that will be enabled/disabled

    public DelayOffSectionPanel(StationDefinition sd, ClassDefinition cd, final Object stationKey) {
        this.data = sd;
        this.classData = cd;
        this.stationKey = stationKey;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        // Description Panel
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setBorder(new TitledBorder("Description"));
        JTextArea descriptionText = new JTextArea("Delay-off times are used to model scenarios where, if a queue remains " +
                "idle for a specified duration, it will automatically shut down." +
                "The setup time refers to the duration a queue requires to restart when a new job arrives after a shutdown.");
        descriptionText.setEditable(false);
        descriptionText.setLineWrap(true);
        descriptionText.setWrapStyleWord(true);
        descriptionPanel.add(descriptionText, BorderLayout.CENTER);

        gbc.weighty = 0.1;
        add(descriptionPanel, gbc);

        // Enable Delay Off Checkbox
        enableDelayOff = new JCheckBox("Enable Delay Off (you will start to specify the setup time and delay off time after enabling)");
        gbc.weighty = 0; // No extra vertical space allocation
        add(enableDelayOff, gbc);

        // Delay Off Configuration Panel
        delayOffConfigPanel = new JPanel(new GridBagLayout());
        delayOffConfigPanel.setBorder(new TitledBorder("Delay Off and setup time configuration"));

        // Setup the initial distribution
        setupTimeDistribution = new Exponential();
        delayOffTimeDistribution = new Exponential();

        setupTimeDistributionLabel = new JLabel(setupTimeDistribution.toString());
        delayOffTimeDistributionLabel = new JLabel(delayOffTimeDistribution.toString());

        JButton editSetupTimeButton = new JButton("Edit Setup Time");
        editSetupTimeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DelayOffSectionPanel.this.editDistribution(setupTimeDistribution, setupTimeDistributionLabel);
            }
        });

        JButton editDelayOffTimeButton = new JButton("Edit Delay Off Time");
        editDelayOffTimeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DelayOffSectionPanel.this.editDistribution(delayOffTimeDistribution, delayOffTimeDistributionLabel);
            }
        });

        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.insets = new Insets(2, 2, 2, 2);
        localGbc.anchor = GridBagConstraints.WEST;

        localGbc.gridx = 0; localGbc.gridy = 0;
        delayOffConfigPanel.add(new JLabel("Setup Time:"), localGbc);
        localGbc.gridx = 1;
        delayOffConfigPanel.add(setupTimeDistributionLabel, localGbc);
        localGbc.gridx = 2;
        delayOffConfigPanel.add(editSetupTimeButton, localGbc);

        localGbc.gridx = 0; localGbc.gridy = 1;
        delayOffConfigPanel.add(new JLabel("Delay Off time:"), localGbc);
        localGbc.gridx = 1;
        delayOffConfigPanel.add(delayOffTimeDistributionLabel, localGbc);
        localGbc.gridx = 2;
        delayOffConfigPanel.add(editDelayOffTimeButton, localGbc);

        gbc.weighty = 0.8; // Allocate most of the vertical space to the config panel
        add(delayOffConfigPanel, gbc);

        // Set initial state
        setPanelEnabled(delayOffConfigPanel, enableDelayOff.isSelected());

        // Add listener to checkbox
        enableDelayOff.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DelayOffSectionPanel.this.setPanelEnabled(delayOffConfigPanel, enableDelayOff.isSelected());
                DelayOffSectionPanel.this.editDistribution(setupTimeDistribution, setupTimeDistributionLabel);
                DelayOffSectionPanel.this.editDistribution(delayOffTimeDistribution, delayOffTimeDistributionLabel);
            }
        });
    }

    private void editDistribution(Distribution distribution, JLabel label) {
        DistributionsEditor editor = DistributionsEditor.getInstance(this, distribution);
        editor.setVisible(true);
        Distribution result = editor.getResult();
        if (result != null) {
            if (distribution == setupTimeDistribution) {
                data.setSetupTimeDistribution(stationKey, result);
            } else {
                data.setDelayOffTimeDistribution(stationKey, result);
            }
            distribution = result;
            label.setText(distribution.toString());
        }
    }

    private void setPanelEnabled(JPanel panel, boolean enabled) {
        panel.setEnabled(enabled);
        Component[] components = panel.getComponents();
        for (Component component : components) {
            component.setEnabled(enabled);
        }
        data.setDelayOffTimesEnabled(stationKey, enabled);
    }

    @Override
    public String getName() {
        return "Setup Times";
    }

    // Getters
    public boolean isDelayOffEnabled() {
        return enableDelayOff.isSelected();
    }

    public Distribution getSetupTimeDistribution() {
        return setupTimeDistribution;
    }

    public Distribution getDelayOffTimeDistribution() {
        return delayOffTimeDistribution;
    }
}
