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

package jmt.gui.common.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.engine.simEngine.Simulation;
import jmt.framework.data.LabelValue;
import jmt.framework.data.MacroReplacer;
import jmt.framework.gui.layouts.SpringUtilities;
import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.JMTImageLoader;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.SimulationDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.editors.ImagedComboBoxCellEditorFactory;
import jmt.gui.exact.table.BooleanCellRenderer;
import jmt.gui.exact.table.DisabledCellRenderer;
import jmt.gui.exact.table.ExactCellEditor;

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 26-lug-2005
 * Time: 16.08.15
 * Modified by Bertoli Marco 29/09/2005, 7-oct-2005
 *                           9-jan-2006  --> ComboBoxCellEditor
 *                           
 * Modified by Ashanka (May 2010):
 * Description: Resized some column's width and edited the column headings.
 * 
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public class MeasurePanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	private static final LabelValue[] DELIMITERS = {new LabelValue(";"), new LabelValue(","), new LabelValue("Tab", "\t"), new LabelValue("Space"," ")};
	private static final LabelValue[] DECIMAL = {new LabelValue("."), new LabelValue(",")};

	//Interfaces for model data exchange
	protected ClassDefinition classData;

	protected String filePath;

	/**
	 * called by the Wizard before when switching to another panel
	 */
	@Override
	public void lostFocus() {
		// Aborts editing of table
		TableCellEditor editor = measureTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}

	protected StationDefinition stationData;
	protected SimulationDefinition simData;

	protected WarningScrollTable warningPanel;

	//label containing description of this panel's purpose
	protected JLabel descrLabel = new JLabel(MEASURES_DESCRIPTION);
	protected JLabel logPath;

	//table containing measure data
	protected MeasureTable measureTable;

	//types of measures selectable
	protected static final String[] measureTypes = new String[] {
			"------ Select an index  ------",
			SimulationDefinition.MEASURE_QL,
			//SimulationDefinition.MEASURE_BS,			
			SimulationDefinition.MEASURE_QT,
			SimulationDefinition.MEASURE_RP,
			SimulationDefinition.MEASURE_RD,
			SimulationDefinition.MEASURE_X,
			SimulationDefinition.MEASURE_U,
			"------ Advanced indexes ------",
			SimulationDefinition.MEASURE_BR,
			SimulationDefinition.MEASURE_DR,
			SimulationDefinition.MEASURE_FCR_TW,
			SimulationDefinition.MEASURE_FCR_MO,
			SimulationDefinition.MEASURE_FX,
			SimulationDefinition.MEASURE_FJ_CN,
			SimulationDefinition.MEASURE_FJ_RP,
			SimulationDefinition.MEASURE_P,
			SimulationDefinition.MEASURE_RR,
			SimulationDefinition.MEASURE_RP_PER_SINK,
			SimulationDefinition.MEASURE_R,
			SimulationDefinition.MEASURE_RS,
			SimulationDefinition.MEASURE_OT,
			SimulationDefinition.MEASURE_X_PER_SINK
	};

	// Measure selection ComboBox
	protected JComboBox<String> measureSelection = new JComboBox<String>(measureTypes);

	/** Editors and renderers for table */
	protected ImagedComboBoxCellEditorFactory classesCombos;
	/** Editors and renderers for table */
	protected ImagedComboBoxCellEditorFactory stationsCombos;
	/** Editors and renderers for table */
	protected ImagedComboBoxCellEditorFactory FJstationsCombos;
	/** Editors and renderers for table */
	protected ImagedComboBoxCellEditorFactory modesCombos;

	//deletes a measure from list
	protected AbstractAction deleteMeasure = new AbstractAction("") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Deletes this measure");
			putValue(Action.SMALL_ICON, JMTImageLoader.loadImage("Delete"));
		}

		public void actionPerformed(ActionEvent e) {
			int index = measureTable.getSelectedRow();
			if (index >= 0 && index < measureTable.getRowCount()) {
				deleteMeasure(index);
			}
		}

	};

	//addition of a class one by one
	protected AbstractAction addMeasure = new AbstractAction("Add selected index") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.ALT_MASK));
			putValue(Action.SHORT_DESCRIPTION, "Adds a new measure with selected performance index");
		}

		public void actionPerformed(ActionEvent e) {
			addMeasure();
		}

	};

	public MeasurePanel(ClassDefinition classes, StationDefinition stations, SimulationDefinition simParams) {
		classesCombos = new ImagedComboBoxCellEditorFactory(classes);
		stationsCombos = new ImagedComboBoxCellEditorFactory(stations);
		FJstationsCombos = new ImagedComboBoxCellEditorFactory(stations,
				ImagedComboBoxCellEditorFactory.OPTION_FJ_STATION_NAMES);
		modesCombos = new ImagedComboBoxCellEditorFactory(null,
				ImagedComboBoxCellEditorFactory.OPTION_TRANSITION_MODES);
		setData(classes, stations, simParams);
		initComponents();
	}

	private void initComponents() {
		this.setBorder(new EmptyBorder(20, 20, 20, 20));
		this.setLayout(new BorderLayout(5, 5));

		JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
		rightPanel.add(measureSelection, BorderLayout.CENTER);
		rightPanel.add(new JLabel(" "), BorderLayout.NORTH);
		measureSelection.addActionListener(addMeasure);
		//ARIF: show all elements in ComboBox
		measureSelection.setMaximumRowCount(measureTypes.length);
		Object popup = measureSelection.getUI().getAccessibleChild(measureSelection, 0);
		if (popup instanceof ComboPopup) {
			JList<?> jlist = ((ComboPopup) popup).getList();
			jlist.setVisibleRowCount(measureTypes.length);
		}
		// GC: Ensures that keyboard arrows can be used to select metric
		measureSelection.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);

		measureTable = new MeasureTable();

		JPanel headPanel = new JPanel(new BorderLayout(5, 5));
		headPanel.add(descrLabel, BorderLayout.CENTER);
		headPanel.add(rightPanel, BorderLayout.EAST);
		this.add(headPanel, BorderLayout.NORTH);
		warningPanel = new WarningScrollTable(measureTable, WARNING_CLASS_STATION);
		warningPanel.addCheckVector(classData.getClassKeys());
		warningPanel.addCheckVector(stationData.getStationRegionKeysNoSourceSink());
		this.add(warningPanel, BorderLayout.CENTER);

		// Log definition panel
		JPanel logPanel = new JPanel(new BorderLayout(0, 5));
		this.add(logPanel, BorderLayout.SOUTH);
		logPanel.add(new JLabel(MEASURE_LOG_DESCRIPTION), BorderLayout.NORTH);
		JPanel logSettings = new JPanel(new SpringLayout());
		JLabel label = new JLabel("Delimiter:");
		final JComboBox<LabelValue> delimiters = new JComboBox<LabelValue>(DELIMITERS);
		delimiters.setSelectedItem(LabelValue.getElement(DELIMITERS, stationData.getLoggingGlbParameter("delim")));
		delimiters.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LabelValue element = (LabelValue) delimiters.getSelectedItem();
				if (!element.getValue().equals(stationData.getLoggingGlbParameter("decimalSeparator"))) {
					stationData.setLoggingGlbParameter("delim", element.getValue());
				} else {
					JOptionPane.showMessageDialog(MeasurePanel.this,
							"Delimiter and Decimal separator cannot be the same. Please make sure that they are different.",
							"Error", JOptionPane.ERROR_MESSAGE);
					delimiters.setSelectedItem(LabelValue.getElement(DELIMITERS, stationData.getLoggingGlbParameter("delim")));
				}
			}
		});
		label.setLabelFor(delimiters);
		logSettings.add(label);
		logSettings.add(delimiters);
		label = new JLabel("Decimal separator:");
		final JComboBox<LabelValue> decimals = new JComboBox<LabelValue>(DECIMAL);
		decimals.setSelectedItem(LabelValue.getElement(DECIMAL, stationData.getLoggingGlbParameter("decimalSeparator")));
		decimals.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LabelValue element = (LabelValue) decimals.getSelectedItem();
				if (!element.getValue().equals(stationData.getLoggingGlbParameter("delim"))) {
					stationData.setLoggingGlbParameter("decimalSeparator", element.getValue());
				} else {
					JOptionPane.showMessageDialog(MeasurePanel.this,
							"Delimiter and Decimal separator cannot be the same. Please make sure that they are different.",
							"Error", JOptionPane.ERROR_MESSAGE);
					decimals.setSelectedItem(LabelValue.getElement(DECIMAL, stationData.getLoggingGlbParameter("decimalSeparator")));
				}
			}
		});
		label.setLabelFor(decimals);
		logSettings.add(label);
		logSettings.add(decimals);
		SpringUtilities.makeCompactGrid(logSettings, 2, 2, 0, 0, 5, 2);
		logPanel.add(logSettings, BorderLayout.EAST);

		filePath = MacroReplacer.replace(stationData.getLoggingGlbParameter("path"));
		JPanel logPathPanel = new JPanel();
		logPathPanel.setLayout(new BoxLayout(logPathPanel, BoxLayout.Y_AXIS));
		logPanel.add(logPathPanel, BorderLayout.WEST);
		logPath = new JLabel("CSV files path: " +  filePath);
		logPathPanel.add(Box.createRigidArea(new Dimension(0, 3)));
		logPathPanel.add(logPath);
		final JButton filepathButton = new JButton("Browse");
		logPathPanel.add(Box.createRigidArea(new Dimension(0, 6)));
		logPathPanel.add(filepathButton);
		filepathButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.setDialogTitle("Choose Save Path for all CSV log files...");
				fc.setCurrentDirectory(new File(filePath));
				int ret = fc.showSaveDialog(MeasurePanel.this);

				if (ret == JFileChooser.APPROVE_OPTION) {
					File directory = fc.getSelectedFile();
					if (directory.isDirectory()) {
						filePath = directory.getAbsolutePath();
						stationData.setLoggingGlbParameter("path", filePath);
						logPath.setText("CSV files path: " + filePath);
					}
				}
			}
		});
	}

	/**Updates data contained in this panel's components*/
	public void setData(ClassDefinition classes, StationDefinition stations, SimulationDefinition simParams) {
		classData = classes;
		stationData = stations;
		simData = simParams;
		classesCombos.setData(classes);
		stationsCombos.setData(stations);
		FJstationsCombos.setData(stations);
		modesCombos.clearCache();
		refreshComponents();
	}

	/**
	 * called by the Wizard when the panel becomes active
	 */
	@Override
	public void gotFocus() {
		classesCombos.clearCache();
		stationsCombos.clearCache();
		FJstationsCombos.clearCache();
		modesCombos.clearCache();
		refreshComponents();
	}

	@Override
	public void repaint() {
		refreshComponents();
		super.repaint();
	}

	private void refreshComponents() {
		if (measureTable != null) {
			measureTable.tableChanged(new TableModelEvent(measureTable.getModel()));
		}
		if (warningPanel != null) {
			warningPanel.clearCheckVectors();
			warningPanel.addCheckVector(classData.getClassKeys());
			warningPanel.addCheckVector(stationData.getStationRegionKeysNoSourceSink());
		}
	}

	private void addMeasure() {
		if (measureSelection.getSelectedIndex() <= 0) {
			return;
		}
		if (measureSelection.getSelectedIndex() == 7) { // ignore Advanced index entry
			return;
		}
		if (classData.getClassKeys().isEmpty() || stationData.getStationRegionKeysNoSourceSink().isEmpty()) {
			measureSelection.setSelectedIndex(0);
			return;
		}
		Measure measure = getImplicitMeasure(null, (String) measureSelection.getSelectedItem(), null, null);
		simData.addMeasure((String) measureSelection.getSelectedItem(), null, null);
		measureTable.tableChanged(new TableModelEvent(measureTable.getModel()));
		measureSelection.setSelectedIndex(0);
	}

	private void deleteMeasure(int index) {
		simData.removeMeasure(simData.getMeasureKeys().get(index));
		measureTable.tableChanged(new TableModelEvent(measureTable.getModel()));
	}

	@Override
	public String getName() {
		return "Performance Indices";
	}

	protected class MeasureTable extends JTable {

		private static final long serialVersionUID = 1L;

		private JButton deleteButton = new JButton(deleteMeasure);

		public MeasureTable() {
			setModel(new MeasureTableModel());
			setDefaultEditor(Double.class, new ExactCellEditor());
			setDefaultRenderer(Object.class, new DisabledCellRenderer());
			sizeColumns();
			setRowHeight(ROW_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			int[] columnWidths = ((MeasureTableModel) getModel()).columnWidths;
			for (int i = 0; i < columnWidths.length; i++) {
				int prefWidth = columnWidths[i];
				if (i == columnWidths.length - 1) {
					getColumnModel().getColumn(i).setMaxWidth(getRowHeight());
				} else {
					getColumnModel().getColumn(i).setPreferredWidth(prefWidth);
				}
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			Object key = simData.getMeasureKeys().get(row);
			Measure measure = getExplicitMeasure(simData.getMeasureType(key),
					simData.getMeasureStation(key), simData.getMeasureClass(key));
			if (column == 6) {
				return new ButtonCellEditor(deleteButton);
			} else if (column == 2) {
				if (measure.type.equals(SimulationDefinition.MEASURE_QL)
						|| measure.type.equals(SimulationDefinition.MEASURE_RP)
						|| measure.type.equals(SimulationDefinition.MEASURE_DR)
						|| measure.type.equals(SimulationDefinition.MEASURE_RR)
						|| measure.type.equals(SimulationDefinition.MEASURE_BR)
						|| measure.type.equals(SimulationDefinition.MEASURE_R)) {
					Vector<Object> vector = new Vector<Object>();
					vector.add("");					
					vector.addAll(stationData.getStationRegionKeysNoSourceSink());
					vector.removeAll(stationData.getStationKeysTransition());					
					return stationsCombos.getEditor(vector);
				} else if (measure.type.equals(SimulationDefinition.MEASURE_QT)
						|| measure.type.equals(SimulationDefinition.MEASURE_RD)
						|| measure.type.equals(SimulationDefinition.MEASURE_U)
						//|| measure.type.equals(SimulationDefinition.MEASURE_BS)						
						|| measure.type.equals(SimulationDefinition.MEASURE_OT)
					  || measure.type.equals(SimulationDefinition.MEASURE_RS)
				) {
					return stationsCombos.getEditor(stationData.getStationRegionKeysNoSourceSink());
				} else if (measure.type.equals(SimulationDefinition.MEASURE_X)) {
					Vector<Object> vector = new Vector<Object>();
					vector.add("");
					vector.addAll(stationData.getStationKeysSource());
					vector.addAll(stationData.getStationRegionKeysNoSourceSink());
					return stationsCombos.getEditor(vector);
				} else if (measure.type.equals(SimulationDefinition.MEASURE_P)) {
					Vector<Object> vector = new Vector<Object>();
					vector.add("");
					return stationsCombos.getEditor(vector);
				} else if (simData.isSinkMeasure(measure.type)) {
					return stationsCombos.getEditor(stationData.getStationKeysSink());
				} else if (simData.isFCRMeasure(measure.type)) {
					return stationsCombos.getEditor(stationData.getFCRegionKeys());
				} else if (simData.isFJMeasure(measure.type)) {
					return FJstationsCombos.getEditor(stationData.getFJKeys());
				} else if (measure.type.equals(SimulationDefinition.MEASURE_FX)) {
					return stationsCombos.getEditor(stationData.getStationKeysTransition());
				} else {
					return stationsCombos.getEditor(new Vector<Object>());
				}
			} else if (column == 1) {
				if (simData.isSinkMeasure(measure.type)) {
					return classesCombos.getEditor(classData.getOpenClassKeys());
				} else if (simData.isFCRMeasure(measure.type)) {
					return classesCombos.getEditor(new Vector<Object>());
				} else if (measure.type.equals(SimulationDefinition.MEASURE_FX)) {
					if (measure.stationKey == null) {
						return modesCombos.getEditor(new ArrayList<String>());
					} else {
						return modesCombos.getEditor(stationData.getAllTransitionModeNames(measure.stationKey));
					}
				} else {
					return classesCombos.getEditor(classData.getClassKeys());
				}
			} else {
				return super.getCellEditor(row, column);
			}
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			Object key = simData.getMeasureKeys().get(row);
			Measure measure = getExplicitMeasure(simData.getMeasureType(key),
					simData.getMeasureStation(key), simData.getMeasureClass(key));
			if (column == 6) {
				return new ButtonCellEditor(deleteButton);
			} else if (column == 3) {
				return new BooleanCellRenderer();
			} else if (column == 2) {
				if (simData.isFJMeasure(measure.type)) {
					return FJstationsCombos.getRenderer();
				} else {
					return stationsCombos.getRenderer();
				}
			} else if (column == 1) {
				if (measure.type.equals(SimulationDefinition.MEASURE_FX)) {
					return modesCombos.getRenderer();
				} else {
					return classesCombos.getRenderer();
				}
			} else {
				return super.getCellRenderer(row, column);
			}
		}

	}

	protected class MeasureTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private String[] columnNames = new String[] { "Performance Index", "Class/Mode", "Station/Region/System", "Stat.Res.", "Conf.Int.", "Max Rel.Err.", "" };
		private Class<?>[] columnClasses = new Class[] { String.class, String.class, String.class, Boolean.class, Double.class, Double.class, Object.class };
		public int[] columnWidths = new int[] { 120, 80, 120, 30, 60, 60, 20 };

		public int getRowCount() {
			return simData.getMeasureKeys().size();
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
			// Avoid editing of Measure type
			if (columnIndex == 0) {
				return false;
			}
			return true;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Object key = simData.getMeasureKeys().get(rowIndex);
			Measure measure = getExplicitMeasure(simData.getMeasureType(key),
					simData.getMeasureStation(key), simData.getMeasureClass(key));
			switch (columnIndex) {
			case 0: 
				return measure.type;
			case 1: 
				return measure.classKey;
			case 2: 
				return measure.stationKey;
			case 3: 
				return simData.getMeasureLog(key);
			case 4: 
				return simData.getMeasureAlpha(key);
			case 5: 
				return simData.getMeasurePrecision(key);
			}
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			Object key = simData.getMeasureKeys().get(rowIndex);
			Measure measure = null;
			switch (columnIndex) {
			case 0: 
				measure = getImplicitMeasure(key, (String) aValue, getValueAt(rowIndex, 2), getValueAt(rowIndex, 1));
				simData.setMeasureType(measure.type, key);
				simData.setMeasureStation(measure.stationKey, key);
				simData.setMeasureClass(measure.classKey, key);
				fireTableRowsUpdated(rowIndex, rowIndex);
				break;
			case 1: 
				measure = getImplicitMeasure(key, (String) getValueAt(rowIndex, 0), getValueAt(rowIndex, 2), aValue);
				simData.setMeasureType(measure.type, key);
				simData.setMeasureStation(measure.stationKey, key);
				simData.setMeasureClass(measure.classKey, key);
				fireTableRowsUpdated(rowIndex, rowIndex);
				break;
			case 2: 
				measure = getImplicitMeasure(key, (String) getValueAt(rowIndex, 0), aValue, getValueAt(rowIndex, 1));
				simData.setMeasureType(measure.type, key);
				simData.setMeasureStation(measure.stationKey, key);
				simData.setMeasureClass(measure.classKey, key);
				fireTableRowsUpdated(rowIndex, rowIndex);
				break;
			case 3: 
				simData.setMeasureLog((Boolean) aValue, key);
				break;
			case 4: 
				try {
					String doubleVal = (String) aValue;
					simData.setMeasureAlpha(Double.valueOf(doubleVal), key);
				} catch (NumberFormatException e) {
				}
				break;
			case 5: 
				try {
					String doubleVal = (String) aValue;
					simData.setMeasurePrecision(Double.valueOf(doubleVal), key);
				} catch (NumberFormatException e) {
				}
				break;
			}
		}

	}

	private Measure getExplicitMeasure(String type, Object stationKey, Object classKey) {
		if (type.equals(SimulationDefinition.MEASURE_S_CN)
				|| type.equals(SimulationDefinition.MEASURE_S_RP)
				|| type.equals(SimulationDefinition.MEASURE_S_X)
				|| type.equals(SimulationDefinition.MEASURE_S_DR)
				|| type.equals(SimulationDefinition.MEASURE_S_RR)
				|| type.equals(SimulationDefinition.MEASURE_S_BR)
				|| type.equals(SimulationDefinition.MEASURE_S_P)
				|| type.equals(SimulationDefinition.MEASURE_S_R)
		) {
			return new Measure(getStationMeasureType(type), "", classKey);
		} else {
			return new Measure(type, stationKey, classKey);
		}
	}

	private Measure getImplicitMeasure(Object key, String type, Object stationKey, Object classKey) {
		if (type.equals(SimulationDefinition.MEASURE_QL)
				|| type.equals(SimulationDefinition.MEASURE_RP)
				|| type.equals(SimulationDefinition.MEASURE_X)
				|| type.equals(SimulationDefinition.MEASURE_DR)
				|| type.equals(SimulationDefinition.MEASURE_RR)
				|| type.equals(SimulationDefinition.MEASURE_BR)
				|| type.equals(SimulationDefinition.MEASURE_P)
				|| type.equals(SimulationDefinition.MEASURE_R)
				|| type.equals(SimulationDefinition.MEASURE_OT)
				|| type.equals(SimulationDefinition.MEASURE_RS)
		) {
			if (stationKey != null && stationKey.equals("")) {
				return new Measure(getSystemMeasureType(type), null, classKey);
			} else {
				return new Measure(type, stationKey, classKey);
			}
		} else if (type.equals(SimulationDefinition.MEASURE_FX)) {
			if (stationKey != simData.getMeasureStation(key)) {
				return new Measure(type, stationKey, null);
			} else {
				return new Measure(type, stationKey, classKey);
			}
		} else {
			return new Measure(type, stationKey, classKey);
		}
	}

	private String getStationMeasureType(String type) {
		if (type.equals(SimulationDefinition.MEASURE_S_CN)) {
			return SimulationDefinition.MEASURE_QL;
		} else if (type.equals(SimulationDefinition.MEASURE_S_RP)) {
			return SimulationDefinition.MEASURE_RP;
		} else if (type.equals(SimulationDefinition.MEASURE_S_X)) {
			return SimulationDefinition.MEASURE_X;
		} else if (type.equals(SimulationDefinition.MEASURE_S_DR)) {
			return SimulationDefinition.MEASURE_DR;
		}	else if (type.equals(SimulationDefinition.MEASURE_S_RR)) {
			return SimulationDefinition.MEASURE_RR;
		} else if (type.equals(SimulationDefinition.MEASURE_S_BR)) {
			return SimulationDefinition.MEASURE_BR;
		} else if (type.equals(SimulationDefinition.MEASURE_S_P)) {
			return SimulationDefinition.MEASURE_P;
		} else {
			return null;
		}
	}

	private String getSystemMeasureType(String type) {
		if (type.equals(SimulationDefinition.MEASURE_QL)) {
			return SimulationDefinition.MEASURE_S_CN;
		} else if (type.equals(SimulationDefinition.MEASURE_RP)) {
			return SimulationDefinition.MEASURE_S_RP;
		} else if (type.equals(SimulationDefinition.MEASURE_X)) {
			return SimulationDefinition.MEASURE_S_X;
		} else if (type.equals(SimulationDefinition.MEASURE_DR)) {
			return SimulationDefinition.MEASURE_S_DR;
		} else if (type.equals(SimulationDefinition.MEASURE_RR)) {
			return SimulationDefinition.MEASURE_S_RR;
		} else if (type.equals(SimulationDefinition.MEASURE_BR)) {
			return SimulationDefinition.MEASURE_S_BR;
		} else if (type.equals(SimulationDefinition.MEASURE_P)) {
			return SimulationDefinition.MEASURE_S_P;
		} else if (type.equals(SimulationDefinition.MEASURE_R)) {
			return SimulationDefinition.MEASURE_S_R;
		} else {
			return null;
		}
	}

	protected class Measure {

		public String type;
		public Object stationKey; 
		public Object classKey;

		public Measure(String type, Object stationKey, Object classKey) {
			this.type = type;
			this.stationKey = stationKey;
			this.classKey = classKey;
		}

	}

}
