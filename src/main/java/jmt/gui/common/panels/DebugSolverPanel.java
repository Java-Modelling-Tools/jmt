package jmt.gui.common.panels;

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
import java.util.Objects;

public class DebugSolverPanel extends JPanel {
    private Mediator mediator;
    private JTextField logPathField;
    private JButton enableButton, browseButton, selectAllButton, cancelButton;
    private JList<CheckListItem> eventList;
    private DefaultListModel<CheckListItem> listModel;
    private boolean allSelected = false;

    private JComboBox<String> delimiterComboBox, decimalSeparatorComboBox;

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
                "EVENT_RESET_COOLSTART","MSG_NOT_PROCESSED"
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

        enableButton = new JButton("Enable debug logging");
        enableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e1) {
                DebugSolverPanel.this.runSimulation(e1);
            }
        });

        cancelButton = new JButton("disable debug logging");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mediator.toggleDebug(false);
                JOptionPane.showMessageDialog(DebugSolverPanel.this, "Debug logging disabled");
                SwingUtilities.getWindowAncestor(DebugSolverPanel.this).dispose();
            }
        });

        selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DebugSolverPanel.this.selectAllEvents();
            }
        });

        // New components for delimiter and decimal separator selection
        delimiterComboBox = new JComboBox<>(new String[]{",", ";", "Tab", "Space"});
        decimalSeparatorComboBox = new JComboBox<>(new String[]{".", ","});

        delimiterComboBox.setPreferredSize(new Dimension(50, delimiterComboBox.getPreferredSize().height));
        decimalSeparatorComboBox.setPreferredSize(new Dimension(50, decimalSeparatorComboBox.getPreferredSize().height));

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(new JLabel("Log Path:"), BorderLayout.WEST);
        northPanel.add(logPathField, BorderLayout.CENTER);
        northPanel.add(browseButton, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(enableButton);
        southPanel.add(cancelButton);

        // Panel for delimiter and decimal separator selection
        JPanel delimiterPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        delimiterPanel.setBorder(BorderFactory.createTitledBorder("CSV Configuration"));
        delimiterPanel.add(new JLabel("Delimiter:"));
        delimiterPanel.add(delimiterComboBox);
        delimiterPanel.add(new JLabel("Decimal Separator:"));
        delimiterPanel.add(decimalSeparatorComboBox);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(listScrollPane, BorderLayout.CENTER);

        // Create a panel to hold the select all button
        JPanel selectAllPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectAllPanel.add(selectAllButton);

        // Add selectAllPanel and delimiterPanel to centerPanel
        JPanel eventsAndSelectAllPanel = new JPanel(new BorderLayout());
        eventsAndSelectAllPanel.add(listScrollPane, BorderLayout.CENTER);
        eventsAndSelectAllPanel.add(selectAllPanel, BorderLayout.SOUTH);

        centerPanel.add(eventsAndSelectAllPanel, BorderLayout.CENTER);
        centerPanel.add(delimiterPanel, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        setBorder(BorderFactory.createTitledBorder("Log calendar configuration"));
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
            JSimLogger.resetDebugLoggerStorePath(file.getAbsolutePath());
        } else {
            JSimLogger.resetDebugLoggerStorePath(System.getProperty("user.home")+ System.getProperty("file.separator") + "JMT");
        }

        // Set delimiter and decimal separator
        String delimiter = resolveDelimiter((String) Objects.requireNonNull(delimiterComboBox.getSelectedItem()));
        String decimalSeparator = (String) decimalSeparatorComboBox.getSelectedItem();
        JSimLogger.setDelimiter(delimiter);
        JSimLogger.setDecimalSeparator(decimalSeparator);

        if (logPathField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "log file will generate at default position(user\\JMT\\JSIM_debug.csv) since" +
                    " no input of log path, Simulation started... Delimiter: " + delimiter + ", Decimal Separator: " + decimalSeparator);
        } else {
            JOptionPane.showMessageDialog(this, "log file will generate at " + logPathField.getText() + "\\JSIM_debug.csv, Simulation started... Delimiter: " + delimiter + ", Decimal Separator: " + decimalSeparator);
        }
        mediator.toggleDebug(true);

        // Close the dialog/window if needed
        SwingUtilities.getWindowAncestor(this).dispose();
    }

    private String resolveDelimiter(String delimiter) {
        switch (delimiter) {
            case "Tab":
                return "\t";
            case "Space":
                return " ";
            default:
                return delimiter;
        }
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
