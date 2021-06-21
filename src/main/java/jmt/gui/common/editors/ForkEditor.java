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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.forkStrategies.MultiBranchClassSwitchFork;
import jmt.gui.common.forkStrategies.ClassSwitchFork;
import jmt.gui.common.forkStrategies.CombFork;
import jmt.gui.common.forkStrategies.ForkStrategy;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.forkStrategies.ProbabilitiesFork;
import jmt.gui.common.panels.WarningScrollTable;
import jmt.gui.exact.table.DisabledCellRenderer;
import jmt.gui.exact.table.ExactCellEditor;

public class ForkEditor extends JSplitPane implements CommonConstants {

	private static final long serialVersionUID = 1L;
	private Map<Object, OutPath> outPaths;
	private Map<Object, Double> combProbs;
	private Map<Object, Object> out;
	private ArrayList<Object> keys;
	private StationDefinition stations;
	private ClassDefinition classes;
	private Object stationKey, classKey;

	private WarningScrollTable rtPane;
	private WarningScrollTable otPane;
	private WarningScrollTable cbPane;
	private WarningScrollTable csPane;
	private WarningScrollTable onPane;
	private WarningScrollTable cnPane;
	private ForkTable forkTable = new ForkTable();
	private OutPathTable outTable = new OutPathTable();
	private CombTable combTable = new CombTable();
	private ClassNumTable classNumTable = new ClassNumTable();
	private ForkSwitchTable switchTable = new ForkSwitchTable();
	private OutNumTable numTable = new OutNumTable();
	private JTextArea noOptLabel = new JTextArea("No options available for this fork strategy");
	private JScrollPane noOptLabelPanel = new JScrollPane(noOptLabel);
	private JSplitPane jsp = new JSplitPane();
	private JPanel buttons;

	public ForkEditor(StationDefinition sd, ClassDefinition cd, Object stationKey, Object classKey) {
		setOrientation(JSplitPane.VERTICAL_SPLIT);
		setDividerSize(3);
		setBorder(new EmptyBorder(0, 0, 0, 0));
		setResizeWeight(0.5);
		initComponents();
		setData(sd, cd, stationKey, classKey);
	}

	private void initComponents() {
		rtPane = new WarningScrollTable(forkTable, WARNING_OUTGOING_ROUTING);
		otPane = new WarningScrollTable(outTable, "Select a branch");
		cbPane = new WarningScrollTable(combTable, WARNING_OUTGOING_ROUTING);
		cnPane = new WarningScrollTable(classNumTable, WARNING_OUTGOING_ROUTING);
		csPane = new WarningScrollTable(switchTable, WARNING_OUTGOING_ROUTING);
		onPane = new WarningScrollTable(numTable, "Select a branch");

		forkTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting() && forkTable.getSelectedRow() > -1) {
					if (out != null) {
						stations.normalizeForkProbabilities((Map) out);
					}
					out = outPaths.get(indexToKey(forkTable.getSelectedRow())).getOutParameters();
					keys = new ArrayList<Object>(out.keySet());
					jsp.getRightComponent().setVisible(true);
					jsp.setDividerLocation(-1);
					outTable.updateUI();
					otPane.updateUI();
				}
			}

			private Object indexToKey(int index) {
				if (stationKey == null) {
					return null;
				}
				return stations.getForwardConnections(stationKey).get(index);
			}
		});

		switchTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting() && switchTable.getSelectedRow() > -1) {
					out = outPaths.get(indexToKey(switchTable.getSelectedRow())).getOutParameters();
					numTable.updateUI();
					onPane.updateUI();
				}
			}

			private Object indexToKey(int index) {
				if (stationKey == null) {
					return null;
				}
				return stations.getForwardConnections(stationKey).get(index);
			}
		});

		noOptLabelPanel.setBorder(new TitledBorder(new EtchedBorder(), "Fork Options"));
		noOptLabel.setOpaque(false);
		noOptLabel.setEditable(false);
		noOptLabel.setLineWrap(true);
		noOptLabel.setWrapStyleWord(true); 
		rtPane.setBorder(new TitledBorder(new EtchedBorder(), "Branch Probabilities"));
		cbPane.setBorder(new TitledBorder(new EtchedBorder(), "Branch Distribution"));
		cnPane.setBorder(new TitledBorder(new EtchedBorder(), "Number of Tasks per Class"));
		csPane.setBorder(new TitledBorder(new EtchedBorder(), "Branches"));
		onPane.setBorder(new TitledBorder(new EtchedBorder(), "Number of Tasks per Class"));

		JButton add = new JButton("Add");
		JButton delete = new JButton("Delete");

		add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (out != null && keys != null) {
					int max = 0;
					for (int i = 0; i < keys.size(); i++) {
						if (((Integer) keys.get(i)).intValue() > max) {
							max = ((Integer) keys.get(i)).intValue();
						}
					}
					out.put(max + 1, new Double(0.0));
					keys.add(max + 1);
					outTable.updateUI();
				}
			}
		});

		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int selected = outTable.getSelectedRow();
				int count = outTable.getRowCount();
				if (selected > -1 && count > 1) {
					out.remove(keys.get(selected));
					keys.remove(selected);
					if (selected == count - 1) {
						outTable.setRowSelectionInterval(selected - 1, selected - 1);
					}
					outTable.updateUI();
				}
			}
		});

		buttons = new JPanel();
		buttons.setLayout(new GridLayout(1, 2));
		buttons.add(add);
		buttons.add(delete);
		jsp.setOrientation(JSplitPane.VERTICAL_SPLIT);
		jsp.setDividerSize(0);
		jsp.setBorder(new EmptyBorder(0, 0, 0, 0));
		jsp.setResizeWeight(1.0);
		jsp.setBorder(new TitledBorder(new EtchedBorder(), "Task Distribution"));
		jsp.setLeftComponent(otPane);
		jsp.setRightComponent(buttons);
	}

	public void setData(StationDefinition sd, ClassDefinition cd, Object stationKey, Object classKey) {
		ForkStrategy fs = (ForkStrategy) sd.getForkStrategy(stationKey, classKey);
		forkTable.clearSelection();
		outTable.clearSelection();
		combTable.clearSelection();
		classNumTable.clearSelection();
		switchTable.clearSelection();
		numTable.clearSelection();
		jsp.getRightComponent().setVisible(false);
		if (fs == null) {
			emptyPane();
		} else {
			if (fs instanceof ProbabilitiesFork) {
				createDetails(fs, sd, stationKey, classKey);
			} else if (fs instanceof CombFork) {
				createCombDetails(fs, sd, stationKey, classKey);
			} else if (fs instanceof ClassSwitchFork) {
				createClassSwitchDetails(fs, sd, cd, stationKey, classKey);
			} else if (fs instanceof MultiBranchClassSwitchFork) {
				createMultiBranchClassSwitchDetails(fs, sd, cd, stationKey, classKey);
			} else {
				emptyPane();
			}
		}
		doLayout();
	}

	private void emptyPane() {
		setLeftComponent(noOptLabelPanel);
		setRightComponent(new JPanel());
		out = null;
	}

	private void createDetails(ForkStrategy fs, StationDefinition sd, Object stationKey, Object classKey) {
		outPaths = (Map<Object, OutPath>) fs.getOutDetails();
		stations = sd;
		this.stationKey = stationKey;
		this.classKey = classKey;
		combProbs = null;
		out = null;
		keys = null;
		if (outPaths.isEmpty()) {
			setupFork();
		}
		if (stations.getForwardConnections(stationKey).isEmpty()) {
			setLeftComponent(rtPane);
			setRightComponent(new JPanel());
		} else {
			setLeftComponent(rtPane);
			setRightComponent(jsp);
		}
	}

	private void createCombDetails(ForkStrategy fs, StationDefinition sd, Object stationKey, Object classKey) {
		combProbs = (Map<Object, Double>) fs.getOutDetails();
		stations = sd;
		this.stationKey = stationKey;
		this.classKey = classKey;
		outPaths = null;
		out = null;
		keys = null;
		if (combProbs.isEmpty()) {
			setupCombFork();
		}
		setLeftComponent(cbPane);
		setRightComponent(new JPanel());
	}

	private void createClassSwitchDetails(ForkStrategy fs, StationDefinition sd, ClassDefinition cd, Object stationKey, Object classKey) {
		outPaths = (Map<Object, OutPath>) fs.getOutDetails();
		stations = sd;
		classes = cd;
		this.stationKey = stationKey;
		this.classKey = classKey;
		combProbs = null;
		out = null;
		keys = null;
		if (outPaths.isEmpty()) {
			setupClassSwitchFork();
		}
		if (outPaths.size() > 0) {
			out = outPaths.values().iterator().next().getOutParameters();
		}
		setLeftComponent(cnPane);
		setRightComponent(new JPanel());
	}

	private void createMultiBranchClassSwitchDetails(ForkStrategy fs, StationDefinition sd, ClassDefinition cd, Object stationKey, Object classKey) {
		outPaths = (Map<Object, OutPath>) fs.getOutDetails();
		stations = sd;
		classes = cd;
		this.stationKey = stationKey;
		this.classKey = classKey;
		combProbs = null;
		out = null;
		keys = null;
		if (outPaths.isEmpty()) {
			setupMultiBranchClassSwitchFork();
		}
		if (stations.getForwardConnections(stationKey).isEmpty()) {
			setLeftComponent(csPane);
			setRightComponent(new JPanel());
		} else {
			setLeftComponent(csPane);
			setRightComponent(onPane);
		}
	}

	/**
	 * sets up all of the entries in routing table from output connections for
	 * specified station
	 */
	protected void setupFork() {
		if (stationKey == null || classKey == null || stations == null || outPaths == null) {
			return;
		}
		// fetching output-connected stations list
		Vector<Object> outputs = stations.getForwardConnections(stationKey);
		// saving all entries of routing strategy in a temporary data structure
		HashMap<Object, OutPath> temp = new HashMap<Object, OutPath>(outPaths);
		outPaths.clear();
		for (int i = 0; i < outputs.size(); i++) {
			// add old entries to map only if they are still in the current
			// connection set
			Object currentKey = outputs.get(i);
			if (temp.containsKey(currentKey)) {
				outPaths.put(currentKey, temp.get(currentKey));
			} else {
				// if connection set contains new entries, set them to (1, 1.0)
				// by default
				OutPath tempPath = new OutPath();
				tempPath.setProbability(1.0);
				tempPath.putEntry(1, 0.0);
				outPaths.put(currentKey, tempPath);
			}
		}
	}

	protected void setupCombFork() {
		if (stationKey == null || classKey == null || stations == null || combProbs == null) {
			return;
		}
		Vector<Object> outputs = stations.getForwardConnections(stationKey);
		HashMap<Object, Double> temp = new HashMap<Object, Double>(combProbs);
		combProbs.clear();
		for (int i = 0; i < outputs.size(); i++) {
			// add old entries to map only if they are still in the current
			// connection set
			String key = Integer.toString(i + 1);
			if (temp.containsKey(key)) {
				combProbs.put(key, temp.get(key));
			} else {
				// if connection set contains new entries, set them to 0 by
				// default
				combProbs.put(key, new Double(0.0));
			}
		}
	}

	protected void setupClassSwitchFork() {
		if (stationKey == null || classKey == null || stations == null || outPaths == null) {
			return;
		}
		// fetching output-connected stations list
		Vector<Object> outputs = stations.getForwardConnections(stationKey);
		// saving all entries of routing strategy in a temporary data structure
		HashMap<Object, OutPath> temp = new HashMap<Object, OutPath>(outPaths);
		outPaths.clear();
		for (int i = 0; i < outputs.size(); i++) {
			// add old entries to map only if they are still in the current
			// connection set
			Object currentKey = outputs.get(i);

			if (temp.containsKey(currentKey)) {
				outPaths.put(currentKey, temp.get(currentKey));
			} else {
				OutPath tempPath = new OutPath();
				for (Object o : classes.getClassKeys()) {
					if (o == classKey) {
						tempPath.putEntry(o, 1);
					} else {
						tempPath.putEntry(o, 0);
					}
				}
				outPaths.put(currentKey, tempPath);
			}
		}
	}

	protected void setupMultiBranchClassSwitchFork() {
		if (stationKey == null || classKey == null || stations == null || outPaths == null) {
			return;
		}
		// fetching output-connected stations list
		Vector<Object> outputs = stations.getForwardConnections(stationKey);
		// saving all entries of routing strategy in a temporary data structure
		HashMap<Object, OutPath> temp2 = new HashMap<Object, OutPath>(outPaths);
		outPaths.clear();
		for (int i = 0; i < outputs.size(); i++) {
			// add old entries to map only if they are still in the current
			// connection set
			Object currentKey = outputs.get(i);

			if (temp2.containsKey(currentKey)) {
				outPaths.put(currentKey, temp2.get(currentKey));
			} else {
				OutPath tempPath = new OutPath();
				for (Object o : classes.getClassKeys()) {
					if (o == classKey) {
						tempPath.putEntry(o, 1);
					} else {
						tempPath.putEntry(o, 0);
					}
				}
				outPaths.put(currentKey, tempPath);
			}
		}
	}

	public void stopEditing() {
		if (forkTable.getCellEditor() != null) {
			forkTable.getCellEditor().stopCellEditing();
		}
		if (outTable.getCellEditor() != null) {
			outTable.getCellEditor().stopCellEditing();
		}
		if (combTable.getCellEditor() != null) {
			combTable.getCellEditor().stopCellEditing();
		}
		if (classNumTable.getCellEditor() != null) {
			classNumTable.getCellEditor().stopCellEditing();
		}
		if (switchTable.getCellEditor() != null) {
			switchTable.getCellEditor().stopCellEditing();
		}
		if (numTable.getCellEditor() != null) {
			numTable.getCellEditor().stopCellEditing();
		}
		if (stations != null) {
			ForkStrategy fs = (ForkStrategy) stations.getForkStrategy(stationKey, classKey);
			if (fs instanceof ProbabilitiesFork && out != null) {
				stations.normalizeForkProbabilities((Map) out);
			} else if (fs instanceof CombFork && combProbs != null) {
				stations.normalizeForkProbabilities(combProbs);
			}
		}
	}

	protected class ForkTable extends JTable {

		private static final long serialVersionUID = 1L;

		public ForkTable() {
			setModel(new ForkTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((ForkTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class ForkTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Destination", "Probability" };
		private Class<?>[] columnClasses = new Class[] { String.class, Object.class };
		public int[] columnSizes = new int[] { 80, 80 };

		public int getRowCount() {
			if (stationKey != null) {
				return stations.getForwardConnections(stationKey).size();
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
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (outPaths == null) {
				return null;
			}
			if (columnIndex == 0) {
				return stations.getStationName(indexToKey(rowIndex));
			} else if (columnIndex == 1) {
				return outPaths.get(indexToKey(rowIndex)).getProbability();
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				try {
					Double value = Double.valueOf((String) aValue);
					if (value.doubleValue() >= 0.0 && value.doubleValue() <= 1.0) {
						outPaths.get(indexToKey(rowIndex)).setProbability(value);
					}
				} catch (NumberFormatException e) {
				}
			}
		}

		// retrieves station search key from index in table
		private Object indexToKey(int index) {
			if (stationKey == null) {
				return null;
			}
			return stations.getForwardConnections(stationKey).get(index);
		}

	}

	protected class OutPathTable extends JTable {

		private static final long serialVersionUID = 1L;

		public OutPathTable() {
			setModel(new OutPathTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((OutPathTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class OutPathTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Number of Tasks", "Probability" };
		private Class<?>[] columnClasses = new Class[] { String.class, Object.class };
		public int[] columnSizes = new int[] { 80, 80 };

		public int getRowCount() {
			if (out != null) {
				return keys.size();
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
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (out == null) {
				return null;
			}
			if (columnIndex == 0) {
				return indexToKey(rowIndex);
			} else if (columnIndex == 1) {
				return out.get(indexToKey(rowIndex));
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				Object oldValue = indexToKey(rowIndex);
				Double temp = (Double) out.get(oldValue);
				try {
					Integer value = Integer.valueOf((String) aValue);
					if (value.intValue() > 0 && !keys.contains(value)) {
						keys.set(keys.indexOf(oldValue), value);
						out.remove(oldValue);
						out.put(value, temp);
					}
				} catch (NumberFormatException e) {
				}
			}
			if (columnIndex == 1) {
				try {
					Double value = Double.valueOf((String) aValue);
					if (value.doubleValue() >= 0.0) {
						out.put(indexToKey(rowIndex), value);
					}
				} catch (NumberFormatException e) {
				}
			}
		}

		// retrieves station search key from index in table
		private Object indexToKey(int index) {
			if (stationKey == null) {
				return null;
			}
			return keys.get(index);
		}

	}

	//for the combination fork
	protected class CombTable extends JTable {

		private static final long serialVersionUID = 1L;

		public CombTable() {
			setModel(new CombTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((CombTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class CombTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Number of Branches", "Probability" };
		private Class<?>[] columnClasses = new Class[] { String.class, Object.class };
		public int[] columnSizes = new int[] { 100, 80 };

		public int getRowCount() {
			if (stationKey != null) {
				return stations.getForwardConnections(stationKey).size();
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
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (combProbs == null) {
				return null;
			}
			if (columnIndex == 0) {
				return indexToKey(rowIndex);
			} else if (columnIndex == 1) {
				return combProbs.get(indexToKey(rowIndex));

			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				try {
					Double value = Double.valueOf((String) aValue);
					if (value.doubleValue() >= 0.0) {
						combProbs.put(indexToKey(rowIndex), value);
					}
				} catch (NumberFormatException e) {
				}
			}
		}

		private Object indexToKey(int index) {
			return Integer.toString(index + 1);
		}

	}

	protected class ClassNumTable extends JTable {

		private static final long serialVersionUID = 1L;

		public ClassNumTable() {
			setModel(new ClassNumTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			setDefaultRenderer(Object.class, new DisabledCellRenderer());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((ClassNumTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class ClassNumTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Class", "Number of Tasks" };
		private Class<?>[] columnClasses = new Class[] { String.class, Object.class };
		public int[] columnSizes = new int[] { 80, 80 };

		public int getRowCount() {
			if (classes.getClassKeys() != null && out != null) {
				return classes.getClassKeys().size();
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
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			int rowClassType = classes.getClassType(indexToKey(rowIndex));
			int classType = classes.getClassType(classKey);
			return (columnIndex == 1) && (rowClassType == classType);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (out == null) {
				return null;
			}
			if (columnIndex == 0) {
				return classes.getClassName(indexToKey(rowIndex));
			} else if (columnIndex == 1 && isCellEditable(rowIndex, columnIndex)) {
				return out.get(indexToKey(rowIndex));

			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				try {
					Integer value = Integer.valueOf((String) aValue);
					if (value.intValue() >= 0) {
						for (Entry<Object, OutPath> m : outPaths.entrySet()) {
							m.getValue().getOutParameters().put(indexToKey(rowIndex), value);
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

	protected class ForkSwitchTable extends JTable {

		private static final long serialVersionUID = 1L;

		public ForkSwitchTable() {
			setModel(new ForkSwitchTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((ForkSwitchTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class ForkSwitchTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Destination" };
		private Class<?>[] columnClasses = new Class[] { String.class };
		public int[] columnSizes = new int[] { 80 };

		public int getRowCount() {
			if (stationKey != null) {
				return stations.getForwardConnections(stationKey).size();
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
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return stations.getStationName(indexToKey(rowIndex));
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			return;
		}

		// retrieves station search key from index in table
		private Object indexToKey(int index) {
			if (stationKey == null) {
				return null;
			}
			return stations.getForwardConnections(stationKey).get(index);
		}

	}

	protected class OutNumTable extends JTable {

		private static final long serialVersionUID = 1L;

		public OutNumTable() {
			setModel(new OutNumTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			setDefaultRenderer(Object.class, new DisabledCellRenderer());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((OutNumTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class OutNumTableModel extends ClassNumTableModel {

		private static final long serialVersionUID = 1L;

		@Override
		public int getRowCount() {
			if (out != null) {
				return out.keySet().size();
			} else {
				return 0;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				try {
					Integer value = Integer.valueOf((String) aValue);
					if (value.intValue() >= 0) {
						out.put(super.indexToKey(rowIndex), value);
					}
				} catch (NumberFormatException e) {
				}
			}
		}

	}

}

