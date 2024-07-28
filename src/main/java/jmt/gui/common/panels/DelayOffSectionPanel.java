package jmt.gui.common.panels;

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.editors.DistributionsEditor;
import jmt.gui.common.editors.LDStrategyEditor;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ServiceStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;

public class DelayOffSectionPanel extends WizardPanel implements CommonConstants {

    private JCheckBox enableDelayOff;
    protected StationDefinition data;
    protected ClassDefinition classData;
    protected Object stationKey;
    private JPanel delayOffConfigPanel;
    private JTable delayOffTable;
    private DelayOffTableModel delayOffTableModel;
    private JRootPane tablePane;

    private final String[] serviceType = new String[]{SERVICE_LOAD_INDEPENDENT, SERVICE_LOAD_DEPENDENT, SERVICE_ZERO, SERVICE_DISABLED};

    public DelayOffSectionPanel(StationDefinition sd, ClassDefinition cd, final Object stationKey) {
        this.data = sd;
        this.classData = cd;
        this.stationKey = stationKey;

        setLayout(new BorderLayout(5, 5));

        // Description Panel
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setBorder(new TitledBorder("Description"));
        JTextArea descriptionText = new JTextArea("Delay off times are used to model scenarios where, if a queue remains " +
                "idle for a specified duration, it will automatically shutdown." +
                "Setup times model the time a queue requires to restart when a new job arrives after a shutdown.");
        descriptionText.setEditable(false);
        descriptionText.setLineWrap(true);
        descriptionText.setWrapStyleWord(true);
        descriptionPanel.add(descriptionText, BorderLayout.CENTER);
        descriptionPanel.setPreferredSize(new Dimension(400, 70));
        add(descriptionPanel, BorderLayout.NORTH);

        // Enable Delay Off Checkbox
        enableDelayOff = new JCheckBox("Enable Setup and Delay Off Times");
        JPanel enableDelayOffPanel = new JPanel();
        enableDelayOffPanel.setBorder(new TitledBorder(new EtchedBorder(), "Setup and Delay Off Times"));
        enableDelayOffPanel.setLayout(new BorderLayout());
        enableDelayOffPanel.add(enableDelayOff, BorderLayout.NORTH);
        Boolean isEnabled = data.getDelayOffTimesEnabled(stationKey);
        enableDelayOff.setSelected(isEnabled);

        // Delay Off Configuration Panel
        delayOffConfigPanel = new JPanel(new BorderLayout(5, 5));
        delayOffConfigPanel.setBorder(new TitledBorder("Edit"));

        delayOffTableModel = new DelayOffTableModel();
        delayOffTable = new JTable(delayOffTableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 1 || column == 4 || column == 5) {
                    return true;
                }
                return false;
            }

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (column == 1) {
                    return new DefaultCellEditor(new JComboBox<>(serviceType));
                }
                if (column == 4 || column == 5) {
                    return new ButtonCellEditor(new JButton(editDistribution));
                }
                return super.getCellEditor(row, column);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 1) {
                    return new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            JComboBox<String> comboBox = new JComboBox<>(serviceType);
                            comboBox.setSelectedItem(value);
                            if (isSelected) {
                                comboBox.setBackground(table.getSelectionBackground());
                            } else {
                                comboBox.setBackground(table.getBackground());
                            }
                            return comboBox;
                        }
                    };
                }
                if (column == 4 || column == 5) {
                    return new DisabledButtonCellRenderer(new JButton(editDistribution));
                }
                return super.getCellRenderer(row, column);
            }
        };
        delayOffTable.setRowHeight(ROW_HEIGHT);
        delayOffTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = delayOffTable.rowAtPoint(e.getPoint());
                int col = delayOffTable.columnAtPoint(e.getPoint());
                if (e.getClickCount() == 2) {
                    if (col == 4 || col == 5) {
                        delayOffTableModel.editDistribution(row, col == 4);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(delayOffTable);
        scrollPane.setPreferredSize(new Dimension(400, 290));
        delayOffConfigPanel.add(scrollPane, BorderLayout.CENTER);
        WarningScrollTable serviceSectionTable = new WarningScrollTable(delayOffTable, WARNING_CLASS);

        tablePane = new JRootPane();
        tablePane.getContentPane().setLayout(new BorderLayout());
        tablePane.getContentPane().add(delayOffConfigPanel, BorderLayout.CENTER);
        tablePane.getContentPane().add(serviceSectionTable, BorderLayout.CENTER);
        GlassPane glassPane = new GlassPane();
        tablePane.setGlassPane(glassPane);
        glassPane.setVisible(!enableDelayOff.isSelected());

        enableDelayOffPanel.add(tablePane, BorderLayout.CENTER);
        add(enableDelayOffPanel, BorderLayout.CENTER);

        // Set initial state
        setPanelEnabled(delayOffConfigPanel, enableDelayOff.isSelected());

        // Add listener to checkbox
        enableDelayOff.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enabled = enableDelayOff.isSelected();
                DelayOffSectionPanel.this.setPanelEnabled(delayOffConfigPanel, enabled);
                tablePane.getGlassPane().setVisible(!enabled);
            }
        });
        initializeDefaultStrategies();
        repaint();
    }

    private void initializeDefaultStrategies() {
        if (classData.getClassKeys().size()>0 && data.getDelayOffTimeDistribution(stationKey, classData.getClassKeys().get(0)) == null) {
            for (Object classKey : classData.getClassKeys()) {
                ZeroStrategy zeroStrategy = new ZeroStrategy();
                data.setDelayOffTimeDistribution(stationKey, classKey, zeroStrategy);
                data.setSetupTimeDistribution(stationKey, classKey, zeroStrategy);
            }
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

    private class DelayOffTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Class", "Strategy", "Setup Time", "Delay Off Time", "Edit Setup Time", "Edit Delay Off Time"};
        private final Class<?>[] columnClasses = {String.class, String.class, String.class, String.class, Object.class, Object.class};

        @Override
        public int getRowCount() {
            return classData.getClassKeys().size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex == 1 || columnIndex == 4 || columnIndex == 5) {
                return true;
            }
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object classKey = classData.getClassKeys().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return classData.getClassName(classKey);
                case 1:
                    Object strategy = data.getDelayOffTimeDistribution(stationKey, classKey);
                    if (strategy instanceof LDStrategy) {
                        return SERVICE_LOAD_DEPENDENT;
                    } else if (strategy instanceof ZeroStrategy) {
                        return SERVICE_ZERO;
                    } else if (strategy instanceof DisabledStrategy) {
                        return SERVICE_DISABLED;
                    } else {
                        return SERVICE_LOAD_INDEPENDENT;
                    }
                case 2:
                    return data.getSetupTimeDistribution(stationKey, classKey);
                case 3:
                    return data.getDelayOffTimeDistribution(stationKey, classKey);
                default:
                    return "Edit";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Object classKey = classData.getClassKeys().get(rowIndex);
            switch (columnIndex) {
                case 1: // Strategy column
                    ServiceStrategy strategy;
                    if (SERVICE_LOAD_DEPENDENT.equals(aValue)) {
                        strategy = new LDStrategy();
                    } else if (SERVICE_ZERO.equals(aValue)) {
                        strategy = new ZeroStrategy();
                    } else if (SERVICE_DISABLED.equals(aValue)) {
                        strategy = new DisabledStrategy();
                    } else {
                        strategy = new Exponential(); // Default to Exponential if not specified
                    }
                    data.setDelayOffTimeDistribution(stationKey, classKey, strategy);
                    data.setSetupTimeDistribution(stationKey, classKey, strategy);
                    repaint();
                    break;
                default:
                    if (columnIndex == 2) {
                        Distribution setupDist = (Distribution) data.getSetupTimeDistribution(stationKey, classKey);
                        if (setupDist == null) {
                            setupDist = new Exponential();
                        }
                        setupDist.getParameter(0).setValue(aValue.toString());
                        data.setSetupTimeDistribution(stationKey, classKey, setupDist);
                    } else if (columnIndex == 3) {
                        Distribution delayOffDist = (Distribution) data.getDelayOffTimeDistribution(stationKey, classKey);
                        if (delayOffDist == null) {
                            delayOffDist = new Exponential();
                        }
                        delayOffDist.getParameter(0).setValue(aValue.toString());
                        data.setDelayOffTimeDistribution(stationKey, classKey, delayOffDist);
                    }
                    repaint();
                    break;
            }
        }

        public void editDistribution(int row, boolean isSetupTime) {
            Object classKey = classData.getClassKeys().get(row);
            Object strategy = isSetupTime ? data.getSetupTimeDistribution(stationKey, classKey) : data.getDelayOffTimeDistribution(stationKey, classKey);
            if (strategy instanceof Distribution) {
                DistributionsEditor editor = DistributionsEditor.getInstance(DelayOffSectionPanel.this, (Distribution) strategy);
                editor.setVisible(true);
                Distribution result = editor.getResult();
                if (result != null) {
                    if (isSetupTime) {
                        data.setSetupTimeDistribution(stationKey, classKey, result);
                    } else {
                        data.setDelayOffTimeDistribution(stationKey, classKey, result);
                    }
                    repaint();
                }
            } else if (strategy instanceof LDStrategy) {
                LDStrategyEditor editor = LDStrategyEditor.getInstance(DelayOffSectionPanel.this, (LDStrategy) strategy);
                editor.setTitle("Editing " + classData.getClassName(classKey) + " Load Dependent Service Strategy...");
                // Shows editor window
                editor.show();
                repaint();
            }
        }
    }

    protected AbstractAction editDistribution = new AbstractAction("Edit") {

        private static final long serialVersionUID = 1L;

        {
            putValue(Action.SHORT_DESCRIPTION, "Edits Distribution");
        }

        public void actionPerformed(ActionEvent e) {
            DelayOffTableModel model = (DelayOffTableModel) delayOffTable.getModel();
            int row = delayOffTable.getSelectedRow();
            int col = delayOffTable.getSelectedColumn();
            model.editDistribution(row, col == 4);
        }
    };

    protected class DisabledButtonCellRenderer extends ButtonCellEditor {

        private static final long serialVersionUID = 1L;

        private JButton button;

        public DisabledButtonCellRenderer(JButton jbutt) {
            super(jbutt);
            button = jbutt;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Object classKey = classData.getClassKeys().get(row);
            Object strategy = data.getDelayOffTimeDistribution(stationKey, classKey);
            if (strategy instanceof ZeroStrategy || strategy instanceof DisabledStrategy) {
                button.setEnabled(false);
            } else {
                button.setEnabled(true);
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private static class GlassPane extends JComponent {
        private static final long serialVersionUID = 1L;

        public GlassPane() {
            setOpaque(false);
            setVisible(false);
            Color base = UIManager.getColor("inactiveCaptionBorder");
            base = (base == null) ? Color.LIGHT_GRAY : base;
            Color background = new Color(base.getRed(), base.getGreen(), base.getBlue(), 128);
            setBackground(background);

            // Disable Mouse events for the panel
            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getSize().width, getSize().height);
        }
    }
}
