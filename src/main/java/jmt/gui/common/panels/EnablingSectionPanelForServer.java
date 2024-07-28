package jmt.gui.common.panels;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Vector;

import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.table.ExactCellRenderer;
import jmt.gui.table.ExactTable;
import jmt.gui.table.ExactTableModel;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;

public class EnablingSectionPanelForServer extends WizardPanel implements CommonConstants {
    private static final long serialVersioUID = 1L;

    private static final String NO_INPUT_PLACES_ERROR =
            "Error: No input places are defined, but the number of servers is infinite. "
                    + "The server will have an infinite enabling degree.";
    private static final String NO_INPUT_PLACES_WARNING =
            "Warning: No input places are defined, and the number of servers is finite. "
                    + "The server will have a constant enabling degree.";
    private static final String NO_INPUT_CONDITION_ERROR =
            "Error: No enabling or inhibiting condition is specified, but the number of servers is infinite. "
                    + "The server will have an infinite enabling degree.";
    private static final String NO_INPUT_CONDITION_WARNING =
            "Warning: No enabling or inhibiting condition is specified, and the number of servers is finite. "
                    + "The server will have a constant enabling degree.";
    private static final String INVALID_INPUT_CONDITION_WARNING =
            "Warning: The enabling condition is invalidated by the inhibiting condition. "
                    + "The server will never be enabled.";
    public static final String INVALID_RESOURCE_CONDITION_WARNING =
            "Warning: The enabling condition is invalidated by the value of the resource condition. "
                    + "The server will never be enabled";
    private boolean isInitComplete;

    private StationDefinition stationData;
    private ClassDefinition classData;
    private Object stationKey;
    private Vector<Object> stationInKeys;
    private Vector<Object> classKeys;

    private int currentModeIndex = 0;

    private WarningScrollTable leftPanel;
    private TitledBorder enablingBorder;
    private ConditionTable enablingTable;
    private JScrollPane enablingPane;

    private TitledBorder inhibitingBorder;
    private ConditionTable inhibitingTable;
    private JScrollPane inhibitingPane;

    private TitledBorder resourceBorder;
    private ConditionTable resourceTable;
    private JScrollPane resourcePane;
    private JTextArea noticeText;

    public EnablingSectionPanelForServer(StationDefinition sd, ClassDefinition cd, Object sk){
        isInitComplete = false;
        setData(sd, cd, sk);
        initComponents();
        isInitComplete = true;
    }

    public void setData(StationDefinition sd, ClassDefinition cd, Object sk){
        stationData = sd;
        classData = cd;
        stationKey = sk;
        stationInKeys = sd.getBackwardConnectedPlaces(stationKey);
        classKeys = cd.getClassKeys();

        if(isInitComplete){
            leftPanel.clearCheckVectors();
            leftPanel.addCheckVector(stationInKeys);
            leftPanel.addCheckVector(classKeys);
            enablingTable.updateStructure();
            inhibitingTable.updateStructure();
            resourceTable.updateStructure();
        }
    }

    private void initComponents(){
        setLayout(new BorderLayout());

        enablingBorder = new TitledBorder(new EtchedBorder(), "Enabling condition");
        enablingTable = new ConditionTable(0);
        enablingPane = new JScrollPane(enablingTable);
        enablingPane.setBorder(enablingBorder);
        enablingPane.setMinimumSize(new Dimension((int)(300 * CommonConstants.widthScaling), (int)(120 * CommonConstants.heightScaling)));

        inhibitingBorder = new TitledBorder(new EtchedBorder(), "Inhibiting condition");
        inhibitingTable = new ConditionTable(1);
        inhibitingPane = new JScrollPane(inhibitingTable);
        inhibitingPane.setBorder(inhibitingBorder);
        inhibitingPane.setMinimumSize(new Dimension((int)(300 * CommonConstants.widthScaling), (int)(120 * CommonConstants.heightScaling)));

        resourceBorder = new TitledBorder(new EtchedBorder(), "Resource condition");
        resourceTable = new ConditionTable(2);
        resourcePane = new JScrollPane(resourceTable);
        resourcePane.setBorder(resourceBorder);
        resourcePane.setMinimumSize(new Dimension((int)(300 * CommonConstants.widthScaling), (int)(120 * CommonConstants.heightScaling)));

        JPanel conditionPanel = new JPanel(new GridLayout(3, 1, 3, 3));
        conditionPanel.add(enablingPane);
        conditionPanel.add(inhibitingPane);
        conditionPanel.add(resourcePane);

        leftPanel = new WarningScrollTable(conditionPanel, WARNING_CLASS_INCOMING_ROUTING);
        leftPanel.addCheckVector(stationInKeys);
        leftPanel.addCheckVector(classKeys);

        noticeText = new JTextArea("");
        noticeText.setOpaque(false);
        noticeText.setEditable(false);
        noticeText.setLineWrap(true);
        noticeText.setWrapStyleWord(true);

        //JPanel checkBoxPanel = new JPanel();
        //checkBoxPanel.add(enablingDegree);

        JScrollPane noticePanel = new JScrollPane(noticeText);
        noticePanel.setBorder(new TitledBorder(new EtchedBorder(), "Notice"));
        noticePanel.setMinimumSize(new Dimension((int)(200 * CommonConstants.widthScaling), (int)(100 * CommonConstants.heightScaling)));

        JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        mainPanel.setDividerSize(4);
        mainPanel.setResizeWeight(1.0);
        mainPanel.setLeftComponent(leftPanel);
        mainPanel.setRightComponent(noticePanel);

        add(mainPanel);
    }

    /**
     * @return the panel's name
     */
    @Override
    public String getName() {
        return "Enabling section";
    }

    /**
     * called by the wizard when switching to another panel
     */
    @Override
    public void lostFocus(){ stopAllCellEditing(); }

    /**
     * called by the Wizard when the panel becomes active
     */
    @Override
    public void gotFocus(){
        updateNotice();
    }

    private void stopAllCellEditing(){
        TableCellEditor editor;
        editor = enablingTable.getCellEditor();
        if(editor != null){
            editor.stopCellEditing();
        }

        editor = inhibitingTable.getCellEditor();
        if(editor != null){
            editor.stopCellEditing();
        }

        editor = resourceTable.getCellEditor();
        if(editor != null){
            editor.stopCellEditing();
        }
    }

    private void updateNotice(){
        if(classKeys.isEmpty()){
            noticeText.setText("");
            noticeText.setForeground(Color.BLACK);
            return;
        }

        if(stationInKeys.isEmpty()){
            int numberOfServers = stationData.getNumberOfServers(stationKey, currentModeIndex).intValue();
            if(numberOfServers < 1){
                noticeText.setText(NO_INPUT_PLACES_ERROR);
                noticeText.setForeground(Color.RED);
            }else{
                noticeText.setText(NO_INPUT_PLACES_WARNING);
                noticeText.setForeground(Color.BLUE);
            }
        }else{
            boolean hasSpecifiedValues = false;
            boolean hasInvalidValues = false;
            boolean hasInvalidResourceValue = false;
            OUTER_LOOP:
            for(Object stationInKey : stationInKeys){
                for(Object classKey : classKeys){
                    int enablingValue = stationData.getEnablingCondition(
                            stationKey,
                            currentModeIndex,
                            stationInKey,
                            classKey
                    ).intValue();
                    int inhibitingValue = stationData.getInhibitingCondition(
                            stationKey,
                            currentModeIndex,
                            stationInKey,
                            classKey
                    ).intValue();
                    int resourceValue = stationData.getResourceCondition(
                            stationKey,
                            currentModeIndex,
                            stationInKey,
                            classKey
                    ).intValue();

                    if(enablingValue > 0 || inhibitingValue > 0){
                        hasSpecifiedValues = true;
                    }

                    if(inhibitingValue > 0 && enablingValue >= inhibitingValue){
                        hasInvalidValues = true;
                    }

                    if(enablingValue > 0 && resourceValue > enablingValue){
                        hasInvalidResourceValue = true;
                    }

                    if(hasSpecifiedValues && (hasInvalidValues || hasInvalidResourceValue)) {
                        break OUTER_LOOP;
                    }
                }
            }

            if(!hasSpecifiedValues){
                int numberOfServers = stationData.getNumberOfServers(stationKey, currentModeIndex).intValue();
                if(numberOfServers < 1){
                    noticeText.setText(NO_INPUT_CONDITION_ERROR);
                    noticeText.setForeground(Color.RED);
                }else{
                    noticeText.setText(NO_INPUT_CONDITION_WARNING);
                    noticeText.setForeground(Color.BLUE);
                }
            }else{
                if(hasInvalidValues){
                    noticeText.setText(INVALID_INPUT_CONDITION_WARNING);
                    noticeText.setForeground(Color.BLUE);
                }else if(hasInvalidResourceValue){
                    noticeText.setText(INVALID_RESOURCE_CONDITION_WARNING);
                    noticeText.setForeground(Color.RED);
                }else{
                    noticeText.setText("");
                    noticeText.setForeground(Color.BLACK);
                }
            }
        }
    }

    private class ConditionTable extends ExactTable{
        private static final long serialVersionUID = 1L;

        private int tableType;

        private InfiniteExactCellRenderer inhibitingRenderer;

        public ConditionTable(int type){
            super(new ConditionTableModel(type));
            tableType = type;
            inhibitingRenderer = type != 1 ? null : new InfiniteExactCellRenderer();
            setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            setRowHeight(ROW_HEIGHT);
            setRowSelectionAllowed(true);
            setColumnSelectionAllowed(true);
            setBatchEditingEnabled(true);
        }

        @Override
        public TableCellRenderer getCellRenderer(int row, int column){
            if(tableType == 1){
                return inhibitingRenderer;
            }else{
                return super.getCellRenderer(row, column);
            }
        }
    }

    private class ConditionTableModel extends ExactTableModel {
        private static final long serialVersionUID = 1L;

        private int modelType;
        public ConditionTableModel(int type){
            rowHeaderPrototype = "Station10000";
            prototype = "Class10000";
            modelType = type;
        }

        @Override
        public int getRowCount(){ return stationInKeys.size(); }

        @Override
        public int getColumnCount(){ return classKeys.size(); }

        @Override
        protected String getRowName(int rowIndex){ return stationData.getStationName(stationInKeys.get(rowIndex)); }

        @Override
        public String getColumnName(int columnIndex){
            if(columnIndex >= classKeys.size()){
                return "";
            }
            return classData.getClassName(classKeys.get(columnIndex));
        }

        @Override
        public Class<?> getColumnClass(int columnIndex){
            if(columnIndex == -1){
                return String.class;
            }else{
                return String.class;
            }
        }

        @Override
        public Object getPrototype(int i){
            if(i == -1){
                return rowHeaderPrototype;
            }else{
                return prototype;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex){ return true; }

        @Override
        protected Object getValueAtImpl(int rowIndex, int columnIndex){
            if(modelType == 0){
                return stationData.getEnablingCondition(stationKey, currentModeIndex,
                        stationInKeys.get(rowIndex), classKeys.get(columnIndex));
            }else if(modelType == 1){
                return stationData.getInhibitingCondition(stationKey, currentModeIndex,
                        stationInKeys.get(rowIndex), classKeys.get(columnIndex));
            }else{
                return stationData.getResourceCondition(stationKey, currentModeIndex,
                        stationInKeys.get(rowIndex), classKeys.get(columnIndex));
            }
        }

        public void setValueAt(Object value, int rowIndex, int columnIndex){
            try {
                Integer ivalue = Integer.valueOf((String) value);
                if(modelType == 0){
                    stationData.setEnablingCondition(stationKey, currentModeIndex,
                            stationInKeys.get(rowIndex), classKeys.get(columnIndex), ivalue);
                }else if(modelType == 1){
                    stationData.setInhibitingCondition(stationKey, currentModeIndex,
                            stationInKeys.get(rowIndex), classKeys.get(columnIndex), ivalue);
                }else{
                    stationData.setResourceCondition(stationKey, currentModeIndex,
                            stationInKeys.get(rowIndex), classKeys.get(columnIndex), ivalue);
                }

                if(stationData instanceof JSimGraphModel){
                    JSimGraphModel model = (JSimGraphModel) stationData;
                    for(Object stationInKey : stationInKeys){
                        int end = model.getConnectionEnd(stationInKey, stationKey);
                        model.setConnectionEnd(stationInKey, stationKey, end);
                    }
                    model.refreshGraph();
                }
                updateNotice();
            }catch(NumberFormatException e){
                //Aborts modification if String is invalid
            }
        }

        @Override
        public void clear(int row, int col) { setValueAt("0", row, col); }
    }

    private class InfiniteExactCellRenderer extends ExactCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
            if(value != null && ((Integer) value).intValue() <= 0){
                value = Double.POSITIVE_INFINITY;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
