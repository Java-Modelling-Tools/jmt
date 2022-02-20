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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.forkStrategies.OutPath;
import jmt.gui.common.panels.RoutingSectionPanel;
import jmt.gui.common.panels.WarningScrollTable;
import jmt.gui.common.routingStrategies.*;
import jmt.gui.exact.table.ExactCellEditor;
import jmt.gui.jsimgraph.definitions.JSimGraphModel;

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 4-lug-2005
 * Time: 11.52.41
 * Modified by Bertoli Marco 7-oct-2005
 */
public class RoutingProbabilitiesEditor extends JSplitPane implements CommonConstants {

	private static final long serialVersionUID = 1L;

	private Map<Object, Double> routingProbs;
	private Map<Object, Double> classSwitchProbs;
	private Map<Object, Map<Object, Double>> outPaths;
	private Map<Object, Integer> wrrWeights;
	private Vector<Object> validOutputs;
	private StationDefinition stations;
	private ClassDefinition classes;
	private Object stationKey;
	private Object classKey;

	private WarningScrollTable rtPane;
	private WarningScrollTable classProbPane;
	private WarningScrollTable weightsTablePane;
	private WarningScrollTable powerOfKWarningPanel;
	private WarningScrollTable csPane;
	private JTextArea descrTextPane = new JTextArea("");
	private JScrollPane descrPane = new JScrollPane();
	private RoutingTable routingTable = new RoutingTable();
	private ClassSwtichTable classSwitchTable = new ClassSwtichTable();
	private WeightsTable weightsTable = new WeightsTable();
	private JTextArea noOptLabel = new JTextArea("No options available for this routing strategy");
	private JScrollPane noOptLabelPanel = new JScrollPane(noOptLabel);
	private JComponent powerOfKPanel = new JPanel();
	private JSpinner jNumField = new JSpinner();
	private JCheckBox memoryCheckbox = new JCheckBox();
	private ForkSwitchTable switchTable = new ForkSwitchTable();

	public RoutingProbabilitiesEditor(StationDefinition sd, ClassDefinition cd, Object stationKey, Object classKey) {
		setOrientation(JSplitPane.VERTICAL_SPLIT);
		setDividerSize(3);
		setBorder(new EmptyBorder(0, 0, 0, 0));
		setResizeWeight(0.5);
		initComponents();
		setData(sd, cd, stationKey, classKey);
	}

	private void initComponents() {
		rtPane = new WarningScrollTable(routingTable, WARNING_OUTGOING_ROUTING);
		classProbPane = new WarningScrollTable(classSwitchTable, WARNING_OUTGOING_ROUTING);
		weightsTablePane = new WarningScrollTable(weightsTable, WARNING_OUTGOING_ROUTING);
		powerOfKWarningPanel = new WarningScrollTable(powerOfKPanel, WARNING_OUTGOING_ROUTING);
		Border routingOptionsBorder = new TitledBorder(new EtchedBorder(), "Routing Options");
		noOptLabelPanel.setBorder(routingOptionsBorder);
		noOptLabel.setOpaque(false);
		noOptLabel.setEditable(false);
		noOptLabel.setLineWrap(true);
		noOptLabel.setWrapStyleWord(true);
		rtPane.setBorder(routingOptionsBorder);
		classProbPane.setBorder(routingOptionsBorder);
		weightsTablePane.setBorder(routingOptionsBorder);
		powerOfKWarningPanel.setBorder(routingOptionsBorder);
		descrTextPane.setOpaque(false);
		descrTextPane.setEditable(false);
		descrTextPane.setLineWrap(true);
		descrTextPane.setWrapStyleWord(true);
		descrPane.setBorder(new TitledBorder(new EtchedBorder(), "Description"));
		descrPane.setViewportView(descrTextPane);
		powerOfKPanel.setBorder(routingOptionsBorder);
		powerOfKPanel.setLayout(new FlowLayout());

		csPane = new WarningScrollTable(switchTable, WARNING_OUTGOING_ROUTING);
		csPane.setBorder(new TitledBorder(new EtchedBorder(), "Branches"));
		JPanel valueOfKPanel = new JPanel();
		valueOfKPanel.add(new JLabel("Number of choices, k:"));
		jNumField.setPreferredSize(DIM_BUTTON_XS);
		jNumField.addChangeListener(new EditValueOfKListener());
		valueOfKPanel.add(jNumField);
		powerOfKPanel.add(valueOfKPanel);

		JPanel memoryPanel = new JPanel();
		memoryPanel.add(memoryCheckbox);
		memoryPanel.add(new JLabel("Enable memory"));
		memoryCheckbox.addChangeListener(new EnableMemoryListener());
		powerOfKPanel.add(memoryPanel);
		setLeftComponent(descrPane);
		setResizeWeight(0.2);

		switchTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting() && switchTable.getSelectedRow() > -1) {
					classSwitchProbs = outPaths.get(indexToKey(switchTable.getSelectedRow()));
					normalizeClassSwitchProbabilities(classes.getClassKeys(), classSwitchProbs);

					classSwitchTable.updateUI();
					classProbPane.updateUI();
				}
			}

			private Object indexToKey(int index) {
				if (stationKey == null) {
					return null;
				}
				return stations.getForwardConnections(stationKey).get(index);
			}
		});
	}

	public void createCommonDetails(StationDefinition sd, ClassDefinition cd, Object stationKey, Object classKey) {
		stations = sd;
		classes = cd;
		this.stationKey = stationKey;
		this.classKey = classKey;
	}

	public void setData(StationDefinition sd, ClassDefinition cd, Object stationKey, Object classKey) {
		RoutingStrategy rs = (RoutingStrategy) sd.getRoutingStrategy(stationKey, classKey);
		switchTable.clearSelection();
		classSwitchTable.clearSelection();
		if (rs == null) {
			descrTextPane.setText("");
			emptyPane();
		} else {
			descrTextPane.setText(rs.getDescription());
			if (rs instanceof ProbabilityRouting
					|| rs instanceof PowerOfKRouting
					|| rs instanceof WeightedRoundRobinRouting
					|| rs instanceof ClassSwitchRouting) {
				createCommonDetails(sd, cd, stationKey, classKey);
				createDetails(rs);
			} else if (rs instanceof LoadDependentRouting) {
				JComponent LDRoutingPanel = new JPanel();
				LDRoutingPanel.setBorder(new TitledBorder(new EtchedBorder(), "LD Routing Options"));
				if (sd.getForwardConnections(stationKey).size() > 0) {
					JButton editLoadDependentRoutingButton = new JButton("Edit LD Routing..");
					HashMap<String, Object> ldParameters = new HashMap<String, Object>();
					ldParameters.put("ClassDefinition", cd);
					ldParameters.put("StationDefinition", sd);
					ldParameters.put("stationKey", stationKey);
					ldParameters.put("classKey", classKey);
					String stationName = sd.getStationName(stationKey);
					String className = cd.getClassName(classKey);
					ldParameters.put("title", "Editing for [Class] " + className + " for [Station] " + stationName + " Load Dependent Routing ...");
					editLoadDependentRoutingButton.addActionListener(new EditLoadDependentRoutingListener(ldParameters));
					LDRoutingPanel.add(editLoadDependentRoutingButton);
				} else {
					LDRoutingPanel = rtPane;
				}
				setRightComponent(LDRoutingPanel);
			} else {
				emptyPane();
			}
		}
		doLayout();
	}

	private void emptyPane() {
		setRightComponent(noOptLabelPanel);
		routingProbs = null;
		classSwitchProbs = null;
	}

	private void createDetails(RoutingStrategy rs) {
		if (rs instanceof ProbabilityRouting) {
			routingProbs = rs.getValues();
			validOutputs = new Vector<>(0);
			setupRouting();
			setRightComponent(rtPane);
		} else if (rs instanceof PowerOfKRouting) {
			PowerOfKRouting routing = (PowerOfKRouting) rs;
			if (stations.getForwardConnections(stationKey).isEmpty()) {
				setRightComponent(powerOfKWarningPanel);
			} else {
				jNumField.setValue(routing.getK());
				memoryCheckbox.setSelected(routing.isWithMemory());
				setRightComponent(powerOfKPanel);
			}
		} else if (rs instanceof WeightedRoundRobinRouting) {
			wrrWeights = ((WeightedRoundRobinRouting) rs).getWeights();
			validOutputs = new Vector<>(0);
			setupWeights();
			setRightComponent(weightsTablePane);
		}
		else if (rs instanceof ClassSwitchRouting) {
			validOutputs = new Vector<>(0);
			outPaths = ((ClassSwitchRouting) rs).getOutDetails();
			classSwitchProbs = null;

			setupClassSwitchRouting();

			JSplitPane csRightPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			csRightPanel.setResizeWeight(0.5);
			csRightPanel.setTopComponent(csPane);
			csRightPanel.setBottomComponent(classProbPane);
			setRightComponent(csRightPanel);
		}
	}

	private void setupWeights() {
		if (stationKey == null || classKey == null || stations == null || wrrWeights == null) {
			return;
		}
		Vector<Object> outputs = stations.getForwardConnections(stationKey);
		HashMap<Object, Integer> temp = new HashMap<>(wrrWeights);
		wrrWeights.clear();
		for (Object currentKey : outputs) {
			if (temp.containsKey(currentKey)) {
				wrrWeights.put(currentKey, temp.get(currentKey));
			} else {
				wrrWeights.put(currentKey, 1);
			}
			if (classes.getClassType(classKey) != CLASS_TYPE_CLOSED
					|| !stations.getStationType(currentKey).equals(STATION_TYPE_SINK)) {
				validOutputs.add(currentKey);
			} else {
				wrrWeights.put(currentKey, 1);
			}
		}
	}

	/**sets up all of the entries in routing table from output connections for specified
	 * station*/
	protected void setupRouting() {
		if (stationKey == null || classKey == null || stations == null || routingProbs == null) {
			return;
		}
		//fetching output-connected stations list
		Vector<Object> outputs = stations.getForwardConnections(stationKey);
		//saving all entries of routing strategy in a temporary data structure
		HashMap<Object, Double> temp = new HashMap<Object, Double>(routingProbs);
		routingProbs.clear();
		for (int i = 0; i < outputs.size(); i++) {
			//add old entries to map only if they are still in the current connection set
			Object currentKey = outputs.get(i);
			if (temp.containsKey(currentKey)) {
				routingProbs.put(currentKey, temp.get(currentKey));
			} else {
				//if connection set contains new entries, set them to 0 by default
				routingProbs.put(currentKey, new Double(0.0));
			}
			if (classes.getClassType(classKey) != CLASS_TYPE_CLOSED
					|| !stations.getStationType(currentKey).equals(STATION_TYPE_SINK)) {
				validOutputs.add(currentKey);
			} else {
				routingProbs.put(currentKey, new Double(0.0));
			}
		}
	}

	protected void setupClassSwitchRouting() {
		if (stationKey == null || classKey == null || stations == null || outPaths == null) {
			return;
		}
		// fetching output-connected stations list
		Vector<Object> outputs = stations.getForwardConnections(stationKey);
		// saving all entries of routing strategy in a temporary data structure
		HashMap<Object, Map<Object, Double>> temp2 = new HashMap<Object, Map<Object, Double>>(outPaths);

		outPaths.clear();
		for (int i = 0; i < outputs.size(); i++) {
			// add old entries to map only if they are still in the current
			// connection set
			Object currentKey = outputs.get(i);

			if (temp2.containsKey(currentKey)) {
				outPaths.put(currentKey, temp2.get(currentKey));
			} else {
				Map<Object, Double> tempPath = new HashMap<Object, Double>();
				for (Object o : classes.getClassKeys()) {
					if (o == classKey) {
						tempPath.put(o, 1.0);
					} else {
						tempPath.put(o, 0.0);
					}
				}
				outPaths.put(currentKey, tempPath);
			}
		}
	}
	public void stopEditing() {
		if (routingTable.getCellEditor() != null) {
			routingTable.getCellEditor().stopCellEditing();
		}
		if (weightsTable.getCellEditor() != null) {
			weightsTable.getCellEditor().stopCellEditing();
		}
		if (stations != null) {
			RoutingStrategy rs = (RoutingStrategy) stations.getRoutingStrategy(stationKey, classKey);
			if (rs instanceof ProbabilityRouting && routingProbs != null) {
				stations.normalizeRoutingProbabilities(stationKey, classKey, routingProbs);
			} else if (rs instanceof ClassSwitchRouting && classSwitchProbs != null) {
				normalizeClassSwitchProbabilities(classes.getClassKeys(), classSwitchProbs);
			}
		}
	}

	/**
	 * Normalizes the class switch probabilities.
	 */
	public void normalizeClassSwitchProbabilities(Vector<Object> outputs, Map<Object, Double> values) {
		Vector<Object> normalOutputs = new Vector<Object>();
		normalOutputs.addAll(outputs);

		Double[] probabilities = new Double[outputs.size()];
		Object[] keys = outputs.toArray();
		for (int i = 0; i < probabilities.length; i++) {
			probabilities[i] = (Double) values.get(keys[i]);
			if (probabilities[i] == null) {
				probabilities[i] = new Double(0.0);
			}
		}
		values.clear();

		double totalSum = 0.0;
		for (int i = 0; i < probabilities.length; i++) {
			if (normalOutputs.contains(keys[i])) {
				totalSum += probabilities[i].doubleValue();
			}
		}
		for (int i = 0; i < probabilities.length; i++) {
			if (normalOutputs.contains(keys[i])) {
				if (totalSum == 0.0) {
					probabilities[i] = new Double(1.0 / normalOutputs.size());
				} else {
					probabilities[i] = new Double(probabilities[i].doubleValue() / totalSum);
				}
			} else {
				probabilities[i] = new Double(0.0);
			}
			values.put(keys[i], probabilities[i]);
		}
	}

	protected class ClassSwtichTable extends JTable {

		private static final long serialVersionUID = 1L;

		public ClassSwtichTable() {
			setModel(new ClassSwtichTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((ClassSwtichTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class ClassSwtichTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Class", "Probability" };
		private Class<?>[] columnClasses = new Class[] { String.class, Object.class };
		public int[] columnSizes = new int[] { 80, 80 };

		public int getRowCount() {
			if (classes.getClassKeys() != null) {
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
			return columnIndex == 1;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (classSwitchProbs == null) {
				return null;
			}
			if (columnIndex == 0) {
				return classes.getClassName(indexToKey(rowIndex));
			} else if (columnIndex == 1) {
				return classSwitchProbs.get(indexToKey(rowIndex));
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if  (columnIndex == 1) {
				try {
					Double value = Double.valueOf((String) aValue);
					if (value.doubleValue() >= 0) {
						classSwitchProbs.put(indexToKey(rowIndex), value);
					}
				} catch (NumberFormatException e) {
			}
		}
	}

		private Object indexToKey(int index) {
			return classes.getClassKeys().get(index);
		}

	}
	protected class RoutingTable extends JTable {

		private static final long serialVersionUID = 1L;

		public RoutingTable() {
			setModel(new RoutingTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((RoutingTableModel) getModel()).columnSizes[i]);
			}
		}

	}

	protected class RoutingTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Destination", "Probability" };
		private Class<?>[] columnClasses = new Class[] { String.class, Object.class };
		public int[] columnSizes = new int[] { 80, 80 };

		public int getRowCount() {
			if (stationKey != null) {
				return validOutputs.size();
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
			if (routingProbs == null) {
				return null;
			}
			if (columnIndex == 0) {
				return stations.getStationName(indexToKey(rowIndex));
			} else if (columnIndex == 1) {
				return routingProbs.get(indexToKey(rowIndex));
			} else {
				return null;
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				try {
					Double value = Double.valueOf((String) aValue);
					if (value.doubleValue() >= 0) {
						routingProbs.put(indexToKey(rowIndex), value);
					}
				} catch (NumberFormatException e) {
				}
			}
		}

		//retrieves station search key from index in table
		private Object indexToKey(int index) {
			if (stationKey == null) {
				return null;
			}
			return validOutputs.get(index);
		}

	}

	private class WeightsTable extends JTable {

		private static final long serialVersionUID = 1L;

		WeightsTable() {
			setModel(new WeightsTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setMinWidth(((WeightsTableModel) getModel()).columnSizes[i]);
			}
		}
	}

	protected class WeightsTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[] { "Destination", "Weights" };
		private Class<?>[] columnClasses = new Class[] { String.class, Object.class };
		public int[] columnSizes = new int[] { 80, 80 };

		public int getRowCount() {
			if (stationKey != null) {
				return validOutputs.size();
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
			if (wrrWeights == null) {
				return null;
			}
			if (columnIndex == 0) {
				return stations.getStationName(indexToKey(rowIndex));
			} else if (columnIndex == 1) {
				return wrrWeights.get(indexToKey(rowIndex));
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
						wrrWeights.put(indexToKey(rowIndex), value);
						checkMaxNotZero();
					}
				} catch (NumberFormatException e) {
				}
			}
		}

		private void checkMaxNotZero() {
			if (Collections.max(wrrWeights.values()) == 0) {
				for (Object station : wrrWeights.keySet()){
					wrrWeights.put(station, 1);
				}
			}
		}

		private Object indexToKey(int index) {
			if (stationKey == null) {
				return null;
			}
			return validOutputs.get(index);
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
	private class EditLoadDependentRoutingListener implements ActionListener {

		HashMap<String, Object> properties = null;

		public EditLoadDependentRoutingListener(HashMap<String, Object> properties) {
			this.properties = properties;
		}

		public void actionPerformed(ActionEvent e) {
			Container parent = (Container) RoutingProbabilitiesEditor.this;
			RoutingSectionPanel.openLoadDependentRoutingEditor(parent,properties);
		}

	}

	private class EditValueOfKListener implements ChangeListener {

		private final Integer MIN_VALUE_K = 1;

		@Override
		public void stateChanged(ChangeEvent e) {
			Integer k = (Integer) jNumField.getValue();
			Integer maxValueOfK = stations.getForwardConnections(stationKey).size();

			// k must be between 1 and the number of outgoing connections
			if (k < MIN_VALUE_K) {
				jNumField.setValue(MIN_VALUE_K);
			} else if (k > maxValueOfK) {
				jNumField.setValue(maxValueOfK);
			} else {
				PowerOfKRouting rs = (PowerOfKRouting) stations.getRoutingStrategy(stationKey, classKey);
				rs.setK(k);
			}
		}

	}

	private class EnableMemoryListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			Boolean withMemory = memoryCheckbox.isSelected();
			PowerOfKRouting rs = ((PowerOfKRouting) stations.getRoutingStrategy(stationKey, classKey));
			rs.setWithMemory(withMemory);
			descrTextPane.setText(rs.getDescription());
		}

	}

}
