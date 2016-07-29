package jmt.gui.common.editors;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import jmt.gui.common.CommonConstants;
import static jmt.gui.common.CommonConstants.WARNING_ROUTING;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
//import jmt.gui.common.joinStrategies.GuardJoin;
//import jmt.gui.common.joinStrategies.JoinStrategy;
//import jmt.gui.common.joinStrategies.PartialJoin;
//import jmt.gui.common.joinStrategies.TaskAggregateJoin;
import jmt.gui.common.semaphoreStrategies.SemaphoreStrategy;
import jmt.gui.common.semaphoreStrategies.NormalSemaphore;
import jmt.gui.common.panels.WarningScrollTable;
import jmt.gui.exact.table.DisabledCellRenderer;

public class SemaphoreEditor extends JSplitPane implements CommonConstants {
	private static final long serialVersionUID = 1L;
	private int semaphoreThres;
        private Map<Object, Integer> firingMix;
	private StationDefinition stations;
	private Object stationKey, classKey;
        private ClassDefinition classes;
        private SemaphoreStrategy semaphorestrategy;

	private JPanel semaphorePane = new JPanel();
	private JSpinner jNumField = new JSpinner();
	private JTextArea descrTextPane = new JTextArea("");
	private JScrollPane descrPane = new JScrollPane();
	private JTextArea noOptLabel = new JTextArea("No options available for this semaphore strategy");
	private JScrollPane noOptLabelPanel = new JScrollPane(noOptLabel);
    private MixTable mixTable;
    private WarningScrollTable mixPane;
    private SpinnerModel spinnermodel = new SpinnerNumberModel(1, 1, null, 1);

	public SemaphoreEditor(StationDefinition sd, ClassDefinition cs, Object stationKey, Object classKey) {
		super();
		super.setOrientation(JSplitPane.VERTICAL_SPLIT);
		super.setDividerSize(3);
		super.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.setResizeWeight(.5);
                mixTable = new MixTable(cs);
		setData(sd, cs, stationKey, classKey);
		initComponents();
	}

	private void initComponents() {
                mixPane = new WarningScrollTable(mixTable, WARNING_CLASS);
                mixPane.setBorder(new TitledBorder(new EtchedBorder(), "Semaphore Options"));
		noOptLabelPanel.setBorder(new TitledBorder(new EtchedBorder(), "Semaphore Options"));
		noOptLabel.setOpaque(false);
		noOptLabel.setEditable(false);
		noOptLabel.setLineWrap(true);
		noOptLabel.setWrapStyleWord(true);
		descrTextPane.setOpaque(false);
		descrTextPane.setEditable(false);
		descrTextPane.setLineWrap(true);
		descrTextPane.setWrapStyleWord(true);
		descrPane.setBorder(new TitledBorder(new EtchedBorder(), "Description"));
		descrPane.setViewportView(descrTextPane);
		semaphorePane.setLayout(new FlowLayout());
		JLabel text = new JLabel("Number of Required Tasks:");
		semaphorePane.add(text);
		jNumField.setPreferredSize(DIM_BUTTON_XS);
		jNumField.setModel(spinnermodel);
		semaphorePane.add(jNumField);
		semaphorePane.setBorder(new TitledBorder(new EtchedBorder(), "Semaphore Options"));
		setLeftComponent(descrPane);
	}

	public void setData(StationDefinition sd, ClassDefinition cs, Object stationKey, Object classKey) {
		SemaphoreStrategy ss = (SemaphoreStrategy) sd.getSemaphoreStrategy(stationKey, classKey);
		semaphorestrategy = ss;
		this.classKey = classKey;
		
		if (ss == null) {
			emptyPane();				
		} else {
			descrTextPane.setText(ss.getDescription());                        
						if(ss instanceof NormalSemaphore) {
                            	//createFiringMixDetails(ss, sd, cs, stationKey, classKey);
							createDetails(ss, sd, stationKey, classKey);
                        } else {
                                emptyPane();                                 
                        }
		}
		doLayout();
	}

	private void emptyPane() {
                firingMix = null;
		setRightComponent(noOptLabelPanel);
	}
	
	private void createDetails(SemaphoreStrategy ss, StationDefinition sd, Object stationKey, Object classKey){
	
		semaphoreThres = ss.getSemaphoreThres();
		stations = sd;
		this.stationKey = stationKey;
		this.classKey = classKey;
                firingMix = null;
		setRightComponent(semaphorePane);
		this.updateSpinner();
	}
   
	
	private void updateSpinner(){
		jNumField.setValue(semaphoreThres);
		jNumField.addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent e) {
				Integer i = (Integer) jNumField.getValue();
				if (i.intValue() < 1) {
					i = new Integer(0);
					jNumField.setValue(0);
				}
				SemaphoreStrategy ss = (SemaphoreStrategy)stations.getSemaphoreStrategy(stationKey, classKey);				
				ss.setSemaphoreThres(i);
			}
			
		});
	}
        
        protected class MixTable extends JTable {

		private static final long serialVersionUID = 1L;
                ClassDefinition cs;

		public MixTable(ClassDefinition cs) {
			super();
			setModel(new MixTableModel(cs));
			setDefaultEditor(Object.class,
					new jmt.gui.exact.table.ExactCellEditor());
                        this.setDefaultRenderer(Object.class, new DisabledCellRenderer());
			sizeColumns();
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(
						((MixTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class MixTableModel extends AbstractTableModel {

                private ClassDefinition cs;
                
		private static final long serialVersionUID = 1L;
		
			
		private String[] columnNames = new String[] { "Class","Tasks Required(%)" };
		private Class[] columnClasses = new Class[] { String.class,
				Object.class };
		public int[] columnSizes = new int[] { 40, 100 };
                
                public MixTableModel( ClassDefinition cs) {
                        this.cs = cs;                                    
                }        
                

		public int getRowCount() {
			if (classes.getClassKeys() != null) {
				return classes.getClassKeys().size();
				//return 1;
			} else {
				return 0;
			}
		}

		public int getColumnCount() {
			return columnNames.length;
		} 

		@Override
		public String getColumnName(int columnIndex) {			
			return columnNames[columnIndex];	
		}

		@Override
		public Class<Object> getColumnClass(int columnIndex) {		
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			int rowClassType = cs.getClassType(indexToKey(rowIndex));
			int classType = cs.getClassType(classKey);
			return (columnIndex == 1) && (rowClassType == classType);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (firingMix == null) {
				return null;
			}
			if (columnIndex == 0) {
				return classes.getClassName(indexToKey(rowIndex));
				//return classes.getClassName(classKey);
			} else if (columnIndex == 1 && isCellEditable(rowIndex, columnIndex)) {
				return firingMix.get(indexToKey(rowIndex));
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				try {
					Integer value = Integer.valueOf((String) aValue);
					if (value >= 0) {
						if (semaphorestrategy instanceof NormalSemaphore){
							if (value <= 100){
								firingMix.put(indexToKey(rowIndex), value);
							}							
						} else {
                                firingMix.put(indexToKey(rowIndex), value);
						}
					}
				} catch (NumberFormatException e) {
                                        
				}
			}
		}
		
		private Object indexToKey(int index) {
			return classes.getClassKeys().get(index);
		}
	}

}
