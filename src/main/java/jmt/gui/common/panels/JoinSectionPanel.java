package jmt.gui.common.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.framework.gui.table.editors.ComboBoxCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.editors.ImagedComboBoxCellEditorFactory;
import jmt.gui.common.editors.JoinEditor;
import jmt.gui.common.joinStrategies.JoinStrategy;
import jmt.gui.common.joinStrategies.PartialJoin;

public class JoinSectionPanel extends WizardPanel implements CommonConstants{
	
	private static final long serialVersionUID = 1L;
	private StationDefinition stationData;
	private ClassDefinition classData;
	private Object stationKey;
	private JSplitPane mainPanel;

    private static String className;
	private JoinSelectionTable joinStrategies;
	private JoinEditor joinEditor;
//	private RoutingProbabilitiesEditor routingProbEditor;

	/** Used to display classes with icon */
	protected ImagedComboBoxCellEditorFactory classEditor;

	public JoinSectionPanel(StationDefinition sd, ClassDefinition cd, Object stationKey) {
		classEditor = new ImagedComboBoxCellEditorFactory(cd);
		setData(sd, cd, stationKey);
		joinStrategies = new JoinSelectionTable();
		joinEditor = new JoinEditor(stationData, cd, stationKey, null);
		initComponents();
	}

	public void setData(StationDefinition sd, ClassDefinition cd, Object stationKey) {
		stationData = sd;
		classData = cd;
		this.stationKey = stationKey;
		classEditor.setData(cd);
		if (joinStrategies != null) {
			joinStrategies.tableChanged(new TableModelEvent(joinStrategies.getModel()));
		}
	}

	//Francesco D'Aquino
	public void setSelectedClass(Object classKey) {
		Vector temp = classData.getClassKeys();
		int i;
		for (i = 0; i < temp.size(); i++) {
			if (temp.get(i) == classKey) {
				break;
			}
		}
		joinStrategies.setRowSelectionInterval(i, i);
	}

	private void initComponents() {
		this.setLayout(new BorderLayout());
		//building mainPanel
		mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainPanel.setDividerSize(4);
		mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		//layout of main panel
		WarningScrollTable jsp = new WarningScrollTable(joinStrategies, WARNING_CLASS);
		//routing strategy selector on the left, routing probabilities editor on the right
		jsp.setBorder(new TitledBorder(new EtchedBorder(), "Routing Strategies"));
		mainPanel.setResizeWeight(.80); // Gives more space to left component
		jsp.setMinimumSize(new Dimension(180, 100));
		mainPanel.setLeftComponent(jsp);
		joinEditor.setMinimumSize(new Dimension(160, 100));
		mainPanel.setRightComponent(joinEditor);
		add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * called by the Wizard before when switching to another panel
	 */
	@Override
	public void lostFocus() {
		// Aborts editing of table
		TableCellEditor editor = joinStrategies.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
                if (joinStrategies != null && joinStrategies.getRowCount() > 0) {
			joinStrategies.setRowSelectionInterval(0, 0);
		}
	}

	/**
	 * called by the Wizard when the panel becomes active
	 */
	@Override
	public void gotFocus() {
		classEditor.clearCache();
		// Select first routing strategy
		if (joinStrategies != null && joinStrategies.getRowCount() > 0) {
                        int sel = joinStrategies.getSelectedRow() < 0 ? 0 : joinStrategies.getSelectedRow();
			joinStrategies.setRowSelectionInterval(sel, sel);
		}
	}

	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "Join Strategies";
	}

	protected class JoinSelectionTable extends JTable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public JoinSelectionTable() {
			super();
			setModel(new JoinSelectionTableModel());
			sizeColumns();
			setRowHeight(CommonConstants.ROW_HEIGHT);
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 1) {
				JoinStrategy[] joinStrategies = null;
                                Vector<Object> sources = stationData.getStationKeysSource();
                                if(sources.contains(stationKey)){
                                        joinStrategies = JoinStrategy.findAllForSource();
                                }else{
                                        joinStrategies = JoinStrategy.findAll();
                                }
                                return ComboBoxCellEditor.getEditorInstance(joinStrategies);

			} else {
				return super.getCellEditor(row, column);
			}
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 0) {
				return classEditor.getRenderer();
			} else if (column == 1) {
				return ComboBoxCellEditor.getRendererInstance();
			} else {
				return super.getCellRenderer(row, column);
			}
		}

		private void sizeColumns() {
			int[] columnSizes = ((JoinSelectionTableModel) getModel()).columnSizes;
			for (int i = 0; i < columnSizes.length; i++) {
				getColumnModel().getColumn(i).setPreferredWidth(columnSizes[i]);
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			super.valueChanged(e);
			int row = getSelectedRow();
			if (row != -1) {
				if (joinEditor != null) {
//					joinEditor.stopEditing();
					joinEditor.setData(stationData, classData,  stationKey, classData.getClassKeys().get(row));
					JoinSectionPanel.this.doLayout();
					JoinSectionPanel.this.repaint();
				}
			}
		}

	}

	protected class JoinSelectionTableModel extends AbstractTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Class", "Join Strategy" };
		public int[] columnSizes = new int[] { 70, 100 };
		private Class[] columnClasses = new Class[] { String.class, Object.class };

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
		public Class<Integer> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Object key = indexToKey(rowIndex);
			if (columnIndex == 0) {
				return key;
			} else if (columnIndex == 1) {
				return stationData.getJoinStrategy(stationKey, key);
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				Object classKey = indexToKey(rowIndex);
                                className = classData.getClassName(classKey);
				if (!value.equals(stationData.getJoinStrategy(stationKey, classKey))) {
					stationData.setJoinStrategy(stationKey, classKey, ((JoinStrategy) value).clone());
				}
				
				joinEditor.setData(stationData, classData, stationKey, classKey);
				doLayout();
				repaint();
				JoinSectionPanel.this.doLayout();
			}
		}

		private Object indexToKey(int index) {
			return classData.getClassKeys().get(index);
		}
	}

}
