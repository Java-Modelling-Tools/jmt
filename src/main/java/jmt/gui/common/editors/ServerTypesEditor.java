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

package jmt.gui.common.editors;

import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.table.editors.SpinnerCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.panels.WarningScrollTable;
import jmt.gui.jsimgraph.DialogFactory;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class ServerTypesEditor extends WizardPanel implements CommonConstants {

    private static final long serialVersionUID = 1L;

    //Interfaces for model data exchange
    protected ClassDefinition classData;

    protected StationDefinition stationData;

    private Object stationKey;

    private boolean updateTotalServersSpinner;

    private JComboBox schedulingPolicyCombo;

    protected static final String[] schedulingPolicies =
            {
                    STATION_SCHEDULING_POLICY_ALIS,
                    STATION_SCHEDULING_POLICY_ORDER,
                    STATION_SCHEDULING_POLICY_FAIRNESS,
                    STATION_SCHEDULING_POLICY_RAIS,
                    STATION_SCHEDULING_POLICY_ALFS
            };

    protected WarningScrollTable warningPanel;

    protected JSpinner totalNumOfServersSpinner;

    protected JButton addServerTypeButton;

    protected ServerTypesTable serverTypesTable;

    protected AbstractAction addNewServerType = new AbstractAction("Add Server Type") {

        private static final long serialVersionUID = 1L;

        {
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.ALT_MASK));
            putValue(Action.SHORT_DESCRIPTION, "Add new Server Type");
        }

        public void actionPerformed(ActionEvent e) {
            addServerType();
        }

    };

    /**
     * called by the Wizard before when switching to another panel
     */
    @Override
    public void lostFocus() {
        // Aborts editing of table
        TableCellEditor editor = serverTypesTable.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }


    protected void addServerType() {
        stationData.addServerType(stationKey, 1);
        stationData.setStationNumberOfServers(stationKey, stationData.getStationNumberOfServers(stationKey) + 1);
        updateServerPreferences();
        refreshComponents();
    }


    public ServerTypesEditor(StationDefinition stations, ClassDefinition classes, Object stationKey) {
        setData(classes, stations, stationKey);
        initComponents();
    }

    private void initComponents() {

        //create margins for this panel.
        Box vBox = Box.createVerticalBox();
        Box hBox = Box.createHorizontalBox();
        vBox.add(Box.createVerticalStrut(30));
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(30));
        hBox.add(Box.createHorizontalStrut(20));

        //build central panel
        JPanel componentsPanel = new JPanel(new BorderLayout());

        //build upper part of central panel
        JPanel upperPanel = new JPanel(new BorderLayout());
        JLabel descrLabel = new JLabel(SERVER_SETTINGS_DESCRIPTION);
        upperPanel.add(descrLabel, BorderLayout.CENTER);

        //build upper right corner of the main panel
        JPanel upRightPanel = new JPanel(new BorderLayout());
        upRightPanel.add(addServerTypeButton, BorderLayout.NORTH);

        //build spinner panel
        JPanel spinnerPanel = new JPanel();
        JLabel spinnerDescrLabel = new JLabel("Total number of Servers:");
        spinnerPanel.add(spinnerDescrLabel);
        totalNumOfServersSpinner.setPreferredSize(DIM_BUTTON_XS);
        spinnerPanel.add(totalNumOfServersSpinner);

        //build ServerTypes scheduling panel

        JPanel serverScheduling = new JPanel();
        serverScheduling.setBorder(new TitledBorder(new EtchedBorder(), "Server Types Scheduling"));
        serverScheduling.add(Box.createRigidArea(new Dimension(5, 0)));
        serverScheduling.add(new JLabel("Scheduling Policy:"));
        serverScheduling.add(schedulingPolicyCombo);

        //build table panel
        warningPanel = new WarningScrollTable(serverTypesTable, WARNING_CLASS_STATION);
        warningPanel.addCheckVector(classData.getClassKeys());
        warningPanel.setBorder(new TitledBorder(new EtchedBorder(), "Server Types"));

        //add all panels to the mail panel
        upRightPanel.add(spinnerPanel, BorderLayout.CENTER);
        upperPanel.add(upRightPanel, BorderLayout.EAST);
        upperPanel.add(serverScheduling, BorderLayout.SOUTH);
        componentsPanel.add(upperPanel, BorderLayout.NORTH);
        componentsPanel.add(warningPanel,BorderLayout.CENTER);
        hBox.add(componentsPanel);
        hBox.add(Box.createHorizontalStrut(20));
        this.setLayout(new GridLayout(1, 1));
        this.add(vBox);

    }


    /**
     * Updates data contained in this panel's components
     */
    public void setData(ClassDefinition classes, StationDefinition stations, Object stKey) {
        updateTotalServersSpinner = true;
        serverTypesTable = new ServerTypesTable();
        totalNumOfServersSpinner = new JSpinner();
        addServerTypeButton = new JButton(addNewServerType);
        classData = classes;
        stationData = stations;
        stationKey = stKey;
        schedulingPolicyCombo = new JComboBox();
        schedulingPolicyCombo.setModel(new DefaultComboBoxModel(schedulingPolicies));

        // Create an initial server type
        if (stationData.getNumberOfDifferentServerTypes(stationKey) == 0) {
            stationData.addServerType(stationKey, 1);
        }

        addDataManagers();
        updateServerPreferences();
        refreshComponents();
    }

    private void updateServerPreferences() {
        totalNumOfServersSpinner.setValue(stationData.getStationNumberOfServers(stationKey));
        if (STATION_TYPE_DELAY.equals(stationData.getStationType(stationKey))) {
            totalNumOfServersSpinner.setValue(Double.POSITIVE_INFINITY);
            totalNumOfServersSpinner.setEnabled(false);
        } else {
            totalNumOfServersSpinner.setEnabled(true);
        }

        String schedulingPolicy = stationData.getStationSchedulingPolicy(stationKey);
        int index = 0;
        for (int i = 0; i < schedulingPolicies.length; i++) {
            if (schedulingPolicies[i].equals(schedulingPolicy)) {
                index = i;
                break;

            }
        }
        schedulingPolicyCombo.setSelectedIndex(index);
    }

    private void updateSpinnersInTable(int change){
        ServerTypesTableModel stm = (ServerTypesTableModel) serverTypesTable.getModel();

        int lastRow= stm.getRowCount() - 1;
        int valueOfLastRowSpinner = (Integer) stm.getValueAt(lastRow, 3);
        if(change > 0) {
            updateTotalServersSpinner = false;
            stm.setValueAt(valueOfLastRowSpinner + change, lastRow, 3);
        }else{
            change = -change;
            while(change > 0){
                lastRow= stm.getRowCount() - 1;
                valueOfLastRowSpinner = (Integer) stm.getValueAt(lastRow, 3);
                if (valueOfLastRowSpinner > change) {
                    updateTotalServersSpinner = false;
                    stm.setValueAt(valueOfLastRowSpinner - change,lastRow,3);
                    change -= change;
                } else {
                    change -= valueOfLastRowSpinner;
                    updateTotalServersSpinner = false;
                    serverTypesTable.deleteServerType(lastRow);
                }
            }
        }
    }

    private void addDataManagers() {
        totalNumOfServersSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                lostFocus();
                if (totalNumOfServersSpinner.getValue() instanceof Integer) {
                    Integer serverNum = (Integer) totalNumOfServersSpinner.getValue();
                    if (serverNum.intValue() < 1 && totalNumOfServersSpinner.isEnabled()) {
                        serverNum = new Integer(1);
                        totalNumOfServersSpinner.setValue(serverNum);
                    }
                    int oldNum = stationData.getStationNumberOfServers(stationKey);
                    stationData.setStationNumberOfServers(stationKey, serverNum);
                    int change = serverNum - oldNum;
                    serverTypesTable.repaint();
                    updateSpinnersInTable(change);
                }
            }
        });

        schedulingPolicyCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String schedulingPolicy = (String) schedulingPolicyCombo.getSelectedItem();
                stationData.setStationSchedulingPolicy(stationKey, schedulingPolicy);
            }
        });

    }

    /**
     * called by the Wizard when the panel becomes active
     */
    @Override
    public void gotFocus() {
        refreshComponents();
        totalNumOfServersSpinner.setValue(stationData.getStationNumberOfServers(stationKey));
    }

    @Override
    public void repaint() {
        refreshComponents();
        super.repaint();
    }

    private void refreshComponents() {
        if (serverTypesTable != null) {
            serverTypesTable.tableChanged(new TableModelEvent(serverTypesTable.getModel()));
            serverTypesTable.deleteButton.setEnabled(serverTypesTable.getRowCount() != 1);
        }
    }


    @Override
    public String getName() {
        return "Server Types";
    }

    protected class ServerTypesTable extends JTable {

        private static final long serialVersionUID = 1L;
        private int currentRow = 0;

        JButton editButton = new JButton() {

            private static final long serialVersionUID = 1L;

            {
                setText("Edit");
            }
        };

        int[] columnSizes = new int[]{100, 260, 50, 90, 15};

        public ServerTypesTable() {
            setModel(new ServerTypesTableModel());
            sizeColumns();
            setRowHeight(ROW_HEIGHT);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            getTableHeader().setReorderingAllowed(false);
        }

        private void sizeColumns() {
            for (int i = 0; i < columnSizes.length; i++) {
                int prefWidth = columnSizes[i];
                getColumnModel().getColumn(i).setPreferredWidth(prefWidth);
            }
        }

        protected AbstractAction editCompatibilities = new AbstractAction("Edit") {

            private static final long serialVersionUID = 1L;

            {
                putValue(Action.SHORT_DESCRIPTION, "Edit Server Type Compatibilities");
            }

            public void actionPerformed(ActionEvent e) {
                Container parent = ServerTypesEditor.this;
                while (!(parent instanceof Frame) ){
                  parent = parent.getParent();
                }

                ServerType server = stationData.getInfoForServerType(stationKey,currentRow);
                String serverName = server.getName();
                new DialogFactory((Frame) parent).getDialog(new ServerCompatibilityEditor(classData, stationData, stationKey, currentRow), serverName + " Compatibilities",
                    500,
                    400,
                    true, "JSIMMeasuresDefWindowWidth", "JSIMMeasuresDefWindowHeight");
                        repaint();
                    }
                };

        protected AbstractAction deleteServerType = new AbstractAction("") {

            private static final long serialVersionUID = 1L;

            {
                putValue(Action.SHORT_DESCRIPTION, "Delete");
                putValue(Action.SMALL_ICON, JMTImageLoader.loadImage("Delete"));
            }

            public void actionPerformed(ActionEvent e) {
                int index = serverTypesTable.getSelectedRow();
                if (index >= 0 && index < serverTypesTable.getRowCount()) {
                    deleteServerType(index);
                }
            }

        };

        private boolean isOnlyServerCompatibleWithClass(int index){
            ServerType server = stationData.getInfoForServerType(stationKey, index);

            List<Object> compatibleClasses = new ArrayList<>();
            compatibleClasses.addAll(server.getCompatibleClassKeys());

            for(int i = 0; i<stationData.getNumberOfDifferentServerTypes(stationKey); i++){
                ServerType s = stationData.getInfoForServerType(stationKey,i);
                if(index != i){
                    List <Object> keysToBeRemoved = new ArrayList<>();
                    for(Object key: compatibleClasses){
                        if(s.isCompatible(key)){
                            keysToBeRemoved.add(key);
                        }
                    }
                    compatibleClasses.removeAll(keysToBeRemoved);
                    if(compatibleClasses.size() == 0){
                        return false;
                    }
                }
            }
            return compatibleClasses.size() > 0;
        }

        protected void deleteServerType(int index) {

            if(!isOnlyServerCompatibleWithClass(index)) {
                ServerType server = stationData.getInfoForServerType(stationKey, index);

                stationData.deleteServerType(stationKey, index);
                if (updateTotalServersSpinner) {
                    stationData.setStationNumberOfServers(stationKey, stationData.getStationNumberOfServers(stationKey) - server.getNumOfServers());
                    updateServerPreferences();
                }
                updateTotalServersSpinner = true;
                refreshComponents();
            }else{
                JOptionPane.showMessageDialog(
                        new JFrame(),
                        "Each class has to be compatible with at least 1 Server",
                        "Class Compatibility Error", JOptionPane.ERROR_MESSAGE);

            }
        }

        public JButton deleteButton = new JButton() {

            private static final long serialVersionUID = 1L;

            {
                setAction(deleteServerType);
                setFocusable(false);
            }

        };


        @Override
        public TableCellEditor getCellEditor(int row, int column) {

            ServerType server = stationData.getInfoForServerType(stationKey,row);

            if (column == 2) {
                currentRow = row;
                return new ButtonCellEditor(new JButton(editCompatibilities));
            }
            else if (column == 3) {
                int numOfServers = server.getNumOfServers();
                SpinnerCellEditor instance = SpinnerCellEditor.getEditorInstance(numOfServers);
                instance.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        ServerTypesEditor.this.lostFocus();
                    }
                });
                return instance;
            }else if(column == 4){
                return new ButtonCellEditor(new JButton(deleteServerType));
            }
            return super.getCellEditor(row, column);

        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {

            if (column == 2) {
                return new DisabledButtonCellRenderer(editButton);
            }
            else if (column == 3){
              return SpinnerCellEditor.getRendererInstance();
            }else if(column == 4){
                return new ButtonCellEditor(deleteButton);
            }
            return super.getCellRenderer(row, column);

        }

    }

    protected class ServerTypesTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private int suffixClassCollision = 1;

        private String[] columnNames = new String[]{"Name", "Compatible Classes", "",  "Number", ""};
        private Class<?>[] columnClasses = new Class[]{String.class, String.class, Object.class, String.class};


        public ServerTypesTableModel(){
        }

        public int getRowCount() {
            return stationData.getNumberOfDifferentServerTypes(stationKey);
        }

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
            if(columnIndex == 4){
                if( serverTypesTable.getRowCount() == 1) {
                    return false;
                }
            }else if(columnIndex == 1){
                return false;
            }
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ServerType server = stationData.getInfoForServerType(stationKey,rowIndex);

            if (columnIndex == 0) {
                  return server.getName();
            } else if (columnIndex == 1){
                List<Object> compatibleClassKeys = server.getCompatibleClassKeys();

                String compatibleClasses = "";
                for(int i=0; i<compatibleClassKeys.size(); i++){
                    compatibleClasses += classData.getClassName(compatibleClassKeys.get(i));
                    compatibleClasses += "\n";

                    if(i != compatibleClassKeys.size()-1){
                        compatibleClasses += ", ";
                    }
                }

                if (compatibleClasses.equals("")){
                    return "None";
                }

                return compatibleClasses;

            } else if (columnIndex == 3){
                return server.getNumOfServers();
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

            ServerType server = stationData.getInfoForServerType(stationKey,rowIndex);

            if (columnIndex == 0) {
                List<String> serverNames = stationData.getServerTypeNames(stationKey);
                String tmpName = (String) aValue;
                for (int i = 0; i < serverNames.size(); i++ ) {
                    if (serverNames.get(i).equals(tmpName) && i != rowIndex) {
                        tmpName = tmpName + "_" + suffixClassCollision++;
                        break;
                    }
                }
                server.setName(tmpName);
            }else if(columnIndex == 3){
                int oldNum = server.getNumOfServers();
                int change;
                Pattern pattern = Pattern.compile("[0-9]*");
                if(pattern.matcher(aValue.toString()).matches()) {
                    change = (Integer) aValue - oldNum;
                    server.setNumOfServers((Integer) aValue);
                    if(updateTotalServersSpinner) {
                        stationData.setStationNumberOfServers(stationKey, stationData.getStationNumberOfServers(stationKey) + change);
                        updateServerPreferences();
                    }
                    updateTotalServersSpinner = true;
                }
                repaint();
            }
        }


    }

    protected static class DisabledButtonCellRenderer extends ButtonCellEditor {

        private static final long serialVersionUID = 1L;

        private JButton button;

        public DisabledButtonCellRenderer(JButton jbutt) {
            super(jbutt);
            button = jbutt;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (table.isCellEditable(row, column)) {
                button.setEnabled(true);
            } else {
                button.setEnabled(false);
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

    }
}
