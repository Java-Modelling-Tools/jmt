package jmt.gui.common.panels.parametric;

import jmt.engine.log.JSimLogger;
import jmt.engine.log.LoggerStateManager;
import jmt.gui.jsimgraph.controller.Mediator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class DebugSolverPanel extends JPanel {
    private Mediator mediator;
    private JTextField logPathField;
    private JButton runSimulationButton, browseButton, selectAllButton;
    private JList<CheckListItem> eventList;
    private DefaultListModel<CheckListItem> listModel;
    private boolean allSelected = false;

    private File file;
    public DebugSolverPanel(Mediator mediator) {
        this.mediator = mediator;
        initComponent();
    }

    private void initComponent() {
        setLayout(new BorderLayout());

        String[] eventNames = {
                "EVENT_ABORT", "EVENT_STOP", "EVENT_START", "EVENT_JOB", "EVENT_ACK", "EVENT_PREEMPTED_JOB", "EVENT_KEEP_AWAKE",
                "EVENT_JOIN", "EVENT_JOB_OUT_OF_REGION", "EVENT_DISTRIBUTION_CHANGE", "EVENT_JOB_CHANGE", "EVENT_ENABLING",
                "EVENT_TIMING", "EVENT_FIRING", "EVENT_JOB_REQUEST", "EVENT_JOB_RELEASE", "EVENT_JOB_FINISH", "EVENT_POLLING_SERVER_NEXT",
                "EVENT_POLLING_SERVER_READY", "EVENT_RENEGE", "EVENT_RETRIAL", "EVENT_RETRIAL_JOB", "EVENT_SETUP_JOB", "EVENT_JOB_REQUEST_FROM_SERVER",
                "EVENT_RESET_COOLSTART"
        };

        listModel = new DefaultListModel<>();
        for (String event : eventNames) {
            listModel.addElement(new CheckListItem(event));
        }

        eventList = new JList<>(listModel);
        eventList.setCellRenderer(new CheckListRenderer());
        eventList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        eventList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int index = eventList.locationToIndex(evt.getPoint());
                CheckListItem item = listModel.getElementAt(index);
                item.setSelected(!item.isSelected());
                eventList.repaint();

                if (item.isSelected()) {
                    LoggerStateManager.addAllowedEvent(item.toString());
                } else {
                    LoggerStateManager.deleteAllowedEvent(item.toString());
                }
            }
        });

        JScrollPane listScrollPane = new JScrollPane(eventList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Recorded Events"));

        logPathField = new JTextField(30);
        logPathField.setEditable(false);
        browseButton = new JButton("Browse");
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e1) {
                DebugSolverPanel.this.browseForPath(e1);
            }
        });

        runSimulationButton = new JButton("Set Log Path and Run Simulation");
        runSimulationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e1) {
                DebugSolverPanel.this.runSimulation(e1);
            }
        });

        selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DebugSolverPanel.this.selectAllEvents();
            }
        });

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(new JLabel("Log Path:"), BorderLayout.WEST);
        northPanel.add(logPathField, BorderLayout.CENTER);
        northPanel.add(browseButton, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(selectAllButton);
        southPanel.add(runSimulationButton);

        add(northPanel, BorderLayout.NORTH);
        add(listScrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        setBorder(BorderFactory.createTitledBorder("Log calender configuration"));
    }

    private void browseForPath(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            logPathField.setText(file.getAbsolutePath());
        }
    }

    private void runSimulation(ActionEvent e) {
        if (LoggerStateManager.getAllowedEvents().length == 0) {
            JOptionPane.showMessageDialog(this, "Please select at least one event to log");
            return;
        }
        if (file != null) {
            JSimLogger.resetLoggerStorePath(file.getAbsolutePath());
        }
        mediator.toggleDebug(true);
        mediator.startSimulation();
        if (logPathField.getText().isEmpty()) {
            //"log file generated at default position(user\JMT\JSIM.log) since no input of log path, Simulation started..."
            JOptionPane.showMessageDialog(this, "log file generated at default position(user\\JMT\\JSIM.log) since" +
                    " no input of log path, Simulation started...");
        } else {
            JOptionPane.showMessageDialog(this, "log file generated at " + logPathField.getText() + "\\JSIM.log, Simulation started...");
        }
        // Close the dialog/window if needed
        SwingUtilities.getWindowAncestor(this).dispose();
    }

    private void selectAllEvents() {
        allSelected = !allSelected; // Toggle the selection state
        for (int i = 0; i < listModel.size(); i++) {
            CheckListItem item = listModel.getElementAt(i);
            item.setSelected(allSelected);
            if (allSelected) {
                LoggerStateManager.addAllowedEvent(item.toString());
            } else {
                LoggerStateManager.deleteAllowedEvent(item.toString());
            }
        }
        eventList.repaint();
        selectAllButton.setText(allSelected ? "Deselect All" : "Select All");
    }

    static class CheckListItem {
        private String label;
        private boolean isSelected = false;

        public CheckListItem(String label) {
            this.label = label;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    static class CheckListRenderer extends JCheckBox implements ListCellRenderer<CheckListItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends CheckListItem> list, CheckListItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setEnabled(list.isEnabled());
            setSelected(value.isSelected());
            setFont(list.getFont());
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setText(value.toString());
            return this;
        }
    }
}
