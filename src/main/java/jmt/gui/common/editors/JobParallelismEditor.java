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
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.ServerType;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.panels.WarningScrollTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.regex.Pattern;


public class JobParallelismEditor extends WizardPanel implements CommonConstants {

    private static final long serialVersionUID = 1L;

    protected ClassDefinition classData;

    protected StationDefinition data;

    private Object stationKey;


    protected WarningScrollTable warningPanel;

    protected ParallelismTable parallelismTable;

    protected ImagedComboBoxCellEditorFactory classEditor;

    @Override
    public void gotFocus() {
        classEditor.clearCache();
        updateSpinnersInTable();
    }

    /**
     * called by the Wizard before when switching to another panel
     */
    @Override
    public void lostFocus() {
        // Aborts editing of table
        TableCellEditor editor = parallelismTable.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
    }


    public JobParallelismEditor(StationDefinition stations, ClassDefinition classes, Object stationKey) {
        classEditor = new ImagedComboBoxCellEditorFactory(classes);
        setData(classes, stations, stationKey);
        initComponents();
    }

    private void initComponents() {
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setBorder(new TitledBorder(new EtchedBorder(), "Description"));
        JLabel parallelismText = new JLabel("Parallelism is the number of servers required at the same time to service a job.");
        descriptionPanel.add(parallelismText);

        warningPanel = new WarningScrollTable(parallelismTable, WARNING_CLASS_STATION);
        warningPanel.addCheckVector(classData.getClassKeys());
        warningPanel.setBorder(new TitledBorder(new EtchedBorder(), "Job Parallelism"));

        this.add(descriptionPanel, BorderLayout.NORTH);
        this.add(warningPanel, BorderLayout.CENTER);

    }


    /**
     * Updates data contained in this panel's components
     */
    public void setData(ClassDefinition classes, StationDefinition stations, Object stKey) {
        classData = classes;
        data = stations;
        stationKey = stKey;
        classEditor.setData(classes);
        parallelismTable = new ParallelismTable();
    }

    private int getSpinnerMaxValue(Object classKey){
        int maxValue=0;
        for(int i = 0; i<data.getNumberOfDifferentServerTypes(stationKey); i++){
            ServerType server = data.getInfoForServerType(stationKey,i);
            if(server.isCompatible(classKey)){
                maxValue+=server.getNumOfServers();
            }
        }

        return maxValue;
    }

    private void updateSpinnersInTable(){
        ParallelismTableModel stm = (ParallelismTableModel) parallelismTable.getModel();
        for (int i = 0; i < stm.getRowCount(); i++) {
            int maxValue = getSpinnerMaxValue(classData.getClassKeys().get(i));
            if ((Integer) stm.getValueAt(i, 1) > maxValue) {
                stm.setValueAt(maxValue, i, 1);
            }
        }

        parallelismTable.repaint();
    }

    @Override
    public void repaint() {
        if (parallelismTable != null) {
            parallelismTable.tableChanged(new TableModelEvent(parallelismTable.getModel()));
        }
        super.repaint();
    }

    @Override
    public String getName() {
        return "Job Parallelism";
    }

    protected class ParallelismTable extends JTable {

        private static final long serialVersionUID = 1L;

        protected Integer serverNumRequired;

        int[] columnSizes = new int[] { 50, 50 };

        private void initializeSpinners(){
            ParallelismTableModel stm = (ParallelismTableModel) this.getModel();

            for(int i=0; i<stm.getRowCount(); i++){
                int serverNumRequired = 1;
                if(data.getServerNumRequired(stationKey,classData.getClassKeys().get(i)) != null){
                    serverNumRequired = data.getServerNumRequired(stationKey,classData.getClassKeys().get(i));
                }
                stm.setValueAt(serverNumRequired,i,1);
            }
        }

        public ParallelismTable() {
            setModel(new ParallelismTableModel());
            sizeColumns();
            setRowHeight(ROW_HEIGHT);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            getTableHeader().setReorderingAllowed(false);
            initializeSpinners();
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
            if (column == 0) {
                return classEditor.getRenderer();
            } else {
                return SpinnerCellEditor.getRendererInstance();
            }
        }

        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            if (column == 0) {
                return super.getCellEditor(row, column);
            } else {
                Object classKey = classData.getClassKeys().get(row);
                serverNumRequired = data.getServerNumRequired(stationKey,classKey);
                int maxValue = getSpinnerMaxValue(classKey);
                SpinnerCellEditor instance =  SpinnerCellEditor.getEditorInstance(serverNumRequired, maxValue);
                instance.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        JobParallelismEditor.this.lostFocus();
                    }
                });
                return instance;
            }
        }

        private void sizeColumns() {
            for (int i = 0; i < columnSizes.length && i < getColumnCount(); i++) {
                this.getColumnModel().getColumn(i).setPreferredWidth(columnSizes[i]);
            }
        }

    }

    protected class ParallelismTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;
        private String[] columnNames = new String[] { "Class", "Parallelism" };
        private Class<?>[] columnClasses = new Class[] { String.class, String.class };

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
          return columnIndex != 0;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Object classKey = classData.getClassKeys().get(rowIndex);
            switch (columnIndex) {
                case (0):
                    return classKey;
                case (1):
                    return data.getServerNumRequired(stationKey,classKey);
            }
            return null;
        }

        /**Puts edited values to the underlying data structure for model implementation*/
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Object classKey = classData.getClassKeys().get(rowIndex);
            if (columnIndex == 1) {
                Pattern pattern = Pattern.compile("[0-9]*");
                if (pattern.matcher(aValue.toString()).matches()) {
                  data.setServerNumRequired(stationKey, classKey, (Integer) aValue);
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
