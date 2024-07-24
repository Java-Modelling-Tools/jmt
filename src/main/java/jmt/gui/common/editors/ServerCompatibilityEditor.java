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

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.panels.WarningScrollTable;
import jmt.gui.table.BooleanCellRenderer;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ServerCompatibilityEditor extends WizardPanel implements CommonConstants {

    private static final long serialVersionUID = 1L;

    //Interfaces for model data exchange
    protected ClassDefinition classData;

    protected StationDefinition stationData;

    protected WarningScrollTable warningPanel;

    //List of compatibility tables
    protected CompatibilityTable compatibilityTable;

    protected ImagedComboBoxCellEditorFactory classEditor = new ImagedComboBoxCellEditorFactory(classData);

    protected Object stationKey;

    protected ServerType server;

    protected JButton selectAllButton;

    protected JButton unselectAllButton;


    public ServerCompatibilityEditor(ClassDefinition classes, StationDefinition stations, Object stationKey, int currentServerNum) {
        setData(classes, stations, stationKey, currentServerNum);
        initComponents();
    }

    /**
     * called by the Wizard before when switching to another panel
     */
    @Override
    public void lostFocus() {
        // Aborts editing of table

        TableCellEditor editor = compatibilityTable.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
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
        JLabel descrLabel = new JLabel(COMPATIBILITIES_DESCRIPTION);
        upperPanel.add(descrLabel, BorderLayout.CENTER);

        //build upper right corner of the main panel
        JPanel upRightPanel = new JPanel(new BorderLayout());
        upRightPanel.add(selectAllButton, BorderLayout.NORTH);
        upRightPanel.add(unselectAllButton, BorderLayout.CENTER);

        //build table panel
        warningPanel = new WarningScrollTable(compatibilityTable, WARNING_CLASS_STATION);
        warningPanel.setBorder(new TitledBorder(new EtchedBorder(), server.getName()));

        //add all panels to the mail panel
        upperPanel.add(upRightPanel, BorderLayout.EAST);
        componentsPanel.add(upperPanel, BorderLayout.NORTH);
        componentsPanel.add(warningPanel,BorderLayout.CENTER);
        hBox.add(componentsPanel);
        hBox.add(Box.createHorizontalStrut(20));
        this.setLayout(new GridLayout(1, 1));
        this.add(vBox);
    }

    protected AbstractAction selectAll = new AbstractAction("Select All") {

        private static final long serialVersionUID = 1L;

        {
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.ALT_MASK));
            putValue(Action.SHORT_DESCRIPTION, "Select All Classes");
        }

        public void actionPerformed(ActionEvent e) {
            selectAllClasses();
        }

    };

    protected AbstractAction unselectAll = new AbstractAction("Unselect All") {

        private static final long serialVersionUID = 1L;

        {
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.ALT_MASK));
            putValue(Action.SHORT_DESCRIPTION, "Unselect ALl Classes");
        }

        public void actionPerformed(ActionEvent e) { unselectAllClasses();}

    };

    public void selectAllClasses(){
        CompatibilityTableModel ctm = (CompatibilityTableModel) compatibilityTable.getModel();

        for(int i=0; i<ctm.getRowCount(); i++){
            if(!(Boolean) ctm.getValueAt(i,1)){
                ctm.setValueAt(true,i,1);
            }
        }

        compatibilityTable.repaint();
    }

    public void unselectAllClasses(){
        CompatibilityTableModel ctm = (CompatibilityTableModel) compatibilityTable.getModel();

        for(int i=0; i<ctm.getRowCount(); i++){
            if((Boolean) ctm.getValueAt(i,1)){
                ctm.setValueAt(false,i,1);
            }
        }

        compatibilityTable.repaint();
    }


    /**
     * Updates data contained in this panel's components
     */
    public void setData(ClassDefinition classes, StationDefinition stations, Object stkey, int currentServerNum) {
        compatibilityTable = new CompatibilityTable();
        selectAllButton = new JButton(selectAll);
        unselectAllButton = new JButton(unselectAll);
        classData = classes;
        stationData = stations;
        stationKey = stkey;
        server = stationData.getInfoForServerType(stationKey, currentServerNum);
        classEditor.setData(classes);
        refreshComponents();
    }

    /**
     * called by the Wizard when the panel becomes active
     */
    @Override
    public void gotFocus() {
        refreshComponents();
    }

    @Override
    public void repaint() {
        refreshComponents();
        super.repaint();
    }

    private void refreshComponents() {
        if (compatibilityTable != null) {
            compatibilityTable.tableChanged(new TableModelEvent(compatibilityTable.getModel()));
        }
    }


    @Override
    public String getName() {
        return "Server Type Compatibilities";
    }

    protected class CompatibilityTable extends JTable {

        private static final long serialVersionUID = 1L;

        public CompatibilityTable() {
            setModel(new CompatibilityTableModel());
            sizeColumns();
            setRowHeight(ROW_HEIGHT);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            getTableHeader().setReorderingAllowed(false);
        }

        private void sizeColumns() {
            int[] columnWidths = ((CompatibilityTableModel) getModel()).columnWidths;
            for (int i = 0; i < columnWidths.length; i++) {
                int prefWidth = columnWidths[i];
                getColumnModel().getColumn(i).setPreferredWidth(prefWidth);
            }
        }

        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            return super.getCellEditor(row, column);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (column == 0) {
                return classEditor.getRenderer();
            } else {
                return new BooleanCellRenderer();
            }

        }

    }

    protected class CompatibilityTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private String[] columnNames = new String[]{"Classes", "Compatible"};
        private Class<?>[] columnClasses = new Class[]{String.class, Boolean.class};
        public int[] columnWidths = new int[]{200, 100};

        public CompatibilityTableModel(){
        }

        public int getRowCount() {
            return classData.getClassKeys().size();
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
            if (columnIndex == 0) {
                return false;
            }
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object classKey = classData.getClassKeys().get(rowIndex);
            if (columnIndex == 0) {
                return classKey;
            } else {
                return server.isCompatible(classKey);
            }
        }

        private int getNumberOfDifferentCompatibleServers(Object classKey){
            int number = 0;
            for(int i = 0; i<stationData.getNumberOfDifferentServerTypes(stationKey); i++){
                ServerType server = stationData.getInfoForServerType(stationKey,i);
                if(server.isCompatible(classKey)){
                    number+=1;
                }
            }
            return number;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Object classKey = classData.getClassKeys().get(rowIndex);
            if (columnIndex == 1) {
                Boolean bValue = (Boolean) aValue;
                if(bValue){
                    server.addCompatibility(classKey);
                }else{
                    if(getNumberOfDifferentCompatibleServers(classKey) > 1) {
                        server.removeCompatibility(classKey);
                    }else{
                        JOptionPane.showMessageDialog(
                                new JFrame(),
                                "Each class has to be compatible with at least 1 Server",
                                "Class Compatibility Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }


    }


}
