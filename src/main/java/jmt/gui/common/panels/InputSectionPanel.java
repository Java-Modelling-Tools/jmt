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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jmt.engine.NetStrategies.ImpatienceStrategies.*;
import jmt.framework.gui.table.editors.ButtonCellEditor;
import jmt.framework.gui.table.editors.ComboBoxCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.Defaults;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.CommonModel;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.editors.DistributionsEditor;
import jmt.gui.common.editors.GrayCellRenderer;
import jmt.gui.common.editors.ImagedComboBoxCellEditorFactory;
import jmt.gui.exact.table.DisabledCellRenderer;
import jmt.gui.exact.table.ExactCellEditor;
import jmt.gui.jsimgraph.DialogFactory;
import jmt.gui.jsimwiz.panels.ClassesPanel;

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 30-giu-2005
 * Time: 9.33.59
 * Modified by Bertoli Marco 7-oct-2005
 *                           9-jan-2006  --> ComboBoxCellEditor
 */
public class InputSectionPanel extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	/**
	 * Used to define station queue policies
	 */
	protected static final Object[] serverStationQueuePolicies =
			{
					STATION_QUEUE_STRATEGY_NON_PREEMPTIVE,
					STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY,
                    STATION_QUEUE_STRATEGY_POLLING,
					STATION_QUEUE_STRATEGY_PREEMPTIVE,
					STATION_QUEUE_STRATEGY_PSSERVER
			};

	protected static final Object[] otherStationQueuePolicies =
			{
					STATION_QUEUE_STRATEGY_NON_PREEMPTIVE,
					STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY
			};
	/**
	 * Used to define queue policies
	 */
	protected static final Object[] serverPSQueuePolicies =
			{
					QUEUE_STRATEGY_PS,
					QUEUE_STRATEGY_DPS,
					QUEUE_STRATEGY_GPS
			};

	protected  static final Object[] serverPreemptiveQueuePolicies = {
					QUEUE_STRATEGY_LCFS_PR
	};

	protected static final Object[] serverNonPreemptiveQueuePolicies =
			{
					QUEUE_STRATEGY_FCFS,
					QUEUE_STRATEGY_LCFS,
					QUEUE_STRATEGY_RAND,
					QUEUE_STRATEGY_SJF,
					QUEUE_STRATEGY_LJF,
					QUEUE_STRATEGY_SEPT,
					QUEUE_STRATEGY_LEPT
			};

	protected static final Object[] otherNonPreemptiveQueuePolicies =
			{
					QUEUE_STRATEGY_FCFS,
					QUEUE_STRATEGY_LCFS,
					QUEUE_STRATEGY_RAND
			};
	/**
	 * Used to define drop rules
	 */
	protected static final Object[] dropRules = { FINITE_DROP, FINITE_BLOCK, FINITE_WAITING, FINITE_RETRIAL};
  /**
   * Used to define impatience strategies
   */
	protected static List<String> impatienceStrategies;

	private boolean isInitComplete;
	private String stationType;	// Used to determine which impatience strategies are available.

	private ButtonGroup queueLengthGroup;
	private JRadioButton infiniteQueueSelector;
	private JRadioButton finiteQueueSelector;
	private JSpinner queueLengthSpinner;
	private QueueTable queueTable;
	protected JComboBox queuePolicyCombo;
	/** Used to display classes with icon */
	protected ImagedComboBoxCellEditorFactory classEditor;

	protected StationDefinition data;
	protected ClassDefinition classData;
	protected Object stationKey;
	
	/** References to columns **/
	protected final int COLUMN_CLASS = 0;
	protected final int COLUMN_QUEUE_POLICY = 1;
	protected final int COLUMN_DROP_RULE = 2;
	protected final int COLUMN_SERVICE_WEIGHT = 3;
	protected final int COLUMN_IMPATIENCE = 4;
	protected final int COLUMN_EDIT = 5;
	protected JButton queuePolicyEditButton;

	public InputSectionPanel(StationDefinition sd, ClassDefinition cd, Object stationKey, String stationType) {
		isInitComplete = false;
		this.stationType = stationType;
		classEditor = new ImagedComboBoxCellEditorFactory(cd);
		setData(sd, cd, stationKey);
		initComponents();
		addDataManagers();
		updateQueueLength();
        switch (stationType) {
        case STATION_TYPE_SERVER:
		     updatePolicyEditButton();
             break;
        }
		isInitComplete = true;
	}

	private void initComponents() {
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(5, 5));
		infiniteQueueSelector = new JRadioButton("Infinite");
		finiteQueueSelector = new JRadioButton("Finite");
		queueLengthGroup = new ButtonGroup();
		queueLengthGroup.add(infiniteQueueSelector);
		queueLengthGroup.add(finiteQueueSelector);
		queueLengthSpinner = new JSpinner();
		queueLengthSpinner.setPreferredSize(DIM_BUTTON_XS);
		queueTable = new QueueTable();

		//queue details panel
		JPanel queuePolicyPanel = new JPanel(new BorderLayout());
		queuePolicyPanel.setBorder(new TitledBorder(new EtchedBorder(), "Queue Policy"));
		queuePolicyPanel.add(new WarningScrollTable(queueTable, WARNING_CLASS), BorderLayout.CENTER);
		JPanel queueLengthPanel = new JPanel(new GridLayout(4, 1, 3, 3));
		queueLengthPanel.setBorder(new TitledBorder(new EtchedBorder(), "Capacity"));

		// Queue strategy selector
		JPanel queueStrategy = new JPanel(new BorderLayout());
		queueStrategy.add(new JLabel("Station queue policy: "), BorderLayout.WEST);
		queuePolicyCombo = new JComboBox();
		queuePolicyEditButton = new JButton("Edit");
		queueStrategy.add(queuePolicyCombo, BorderLayout.CENTER);
		queueStrategy.add(queuePolicyEditButton, BorderLayout.EAST);
		queuePolicyPanel.add(queueStrategy, BorderLayout.NORTH);
		queueStrategy.setBorder(BorderFactory.createEmptyBorder(2, 5, 10, 5));

		queueLengthPanel.add(infiniteQueueSelector);
		queueLengthPanel.add(finiteQueueSelector);
		JPanel spinnerPanel = new JPanel();
		JLabel label = new JLabel("<html>Max no. customers <br>(queue+service)</html>");
		label.setToolTipText("The maximum number of customers allowed in the station.");
		spinnerPanel.add(label);
		spinnerPanel.add(queueLengthSpinner);
		queueLengthPanel.add(spinnerPanel);
		JPanel queueLengthSpinnerPanel = new JPanel();
		queueLengthSpinnerPanel.add(queueLengthSpinner);
		queueLengthPanel.add(queueLengthSpinnerPanel);
		getRelevantImpatienceStrategies();

		this.add(queueLengthPanel, BorderLayout.WEST);
		this.add(queuePolicyPanel, BorderLayout.CENTER);
	}

	/** Set the correct type of impatience strategies available on the UI.
	 * Only a Queue station will have access to all impatience types.
	 * The rest, (e.g. Fork station), only has None. */
	private void getRelevantImpatienceStrategies() {
		impatienceStrategies = new ArrayList<>();
		impatienceStrategies.add(ImpatienceType.NONE.getDisplayName());
		if (stationType.equals(STATION_TYPE_SERVER)) {
			impatienceStrategies.add(ImpatienceType.BALKING.getDisplayName());
			impatienceStrategies.add(ImpatienceType.RENEGING.getDisplayName());
			//impatienceStrategies.add(ImpatienceType.RETRIAL.getDisplayName());
		}
	}

	public void setData(StationDefinition sd, ClassDefinition cd, Object stationKey) {
		data = sd;
		classData = cd;
		this.stationKey = stationKey;
		classEditor.setData(cd);
		if (isInitComplete) {
			updateQueueLength();
		}
	}

	private void addDataManagers() {
		queueLengthSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Object value = queueLengthSpinner.getValue();
				if (value instanceof Integer) {
					if (((Integer) value).intValue() < 1) {
						value = Integer.valueOf(1);
						queueLengthSpinner.setValue(value);
						return;
					}
					data.setStationQueueCapacity(stationKey, (Integer) value);
				} else {
					data.setStationQueueCapacity(stationKey, Integer.valueOf(-1));
				}
			}
		});

		ActionListener buttonListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TableCellEditor editor = queueTable.getCellEditor();
				if (editor != null) {
					editor.stopCellEditing();
				}
				if (infiniteQueueSelector.isSelected()) {
					queueLengthSpinner.setValue(Double.POSITIVE_INFINITY);
					queueLengthSpinner.setEnabled(false);
					Vector<Object> classKeys = classData.getClassKeys();
					for (Object classKey : classKeys) {
						data.setDropRule(stationKey, classKey, Defaults.get("dropRule"));
					}
				} else {
					queueLengthSpinner.setValue(Defaults.getAsInteger("stationCapacity"));
					queueLengthSpinner.setEnabled(true);
				}
				queueTable.repaint();
			}
		};

		infiniteQueueSelector.addActionListener(buttonListener);
		finiteQueueSelector.addActionListener(buttonListener);

		queuePolicyCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				TableCellEditor editor = queueTable.getCellEditor();
				if (editor != null) {
					editor.stopCellEditing();
				}
				String queuePolicy = (String) queuePolicyCombo.getSelectedItem();
				data.setStationQueueStrategy(stationKey, queuePolicy);

				boolean isPolling = queuePolicy.equals(STATION_QUEUE_STRATEGY_POLLING);
				data.setStationPollingServerBoolean(stationKey, isPolling);

				if (queuePolicy.equals(STATION_QUEUE_STRATEGY_PSSERVER)) {
					List<Object> policyList = Arrays.asList(serverPSQueuePolicies);
					Vector<Object> classKeys = classData.getClassKeys();
					if (classKeys.size() > 0 && !policyList.contains(data.getQueueStrategy(stationKey, classKeys.get(0)))) {
						String policy = Defaults.get("stationQueueStrategy");
						if (!policyList.contains(policy)) {
							policy = QUEUE_STRATEGY_PS;
						}
						for (Object classKey : classKeys) {
							data.setQueueStrategy(stationKey, classKey, policy);
						}
					}
				} else if (queuePolicy.equals(STATION_QUEUE_STRATEGY_PREEMPTIVE)) {
				    List<Object> policyList = Arrays.asList(serverPreemptiveQueuePolicies);
					Vector<Object> classKeys = classData.getClassKeys();
					if (classKeys.size() > 0 && !policyList.contains(data.getQueueStrategy(stationKey, classKeys.get(0)))) {
						String policy = Defaults.get("stationQueueStrategy");
						if (!policyList.contains(policy)) {
							policy = QUEUE_STRATEGY_LCFS_PR;
						}
						for (Object classKey : classKeys) {
							data.setQueueStrategy(stationKey, classKey, policy);
							data.setServiceWeight(stationKey, classKey, Defaults.getAsDouble("serviceWeight"));
						}
					}

				} else {
					List<Object> policyList = Arrays.asList(serverNonPreemptiveQueuePolicies);
					Vector<Object> classKeys = classData.getClassKeys();
					if (classKeys.size() > 0 && !policyList.contains(data.getQueueStrategy(stationKey, classKeys.get(0)))) {
						String policy = Defaults.get("stationQueueStrategy");
						if (!policyList.contains(policy)) {
							policy = QUEUE_STRATEGY_FCFS;
						}
						for (Object classKey : classKeys) {
							data.setQueueStrategy(stationKey, classKey, policy);
							data.setServiceWeight(stationKey, classKey, Defaults.getAsDouble("serviceWeight"));
						}
					}
				}
				updatePolicyEditButton();
				// Update the balking parameter for all classes in this station
				Vector<Object> classKeys = classData.getClassKeys();
				for (Object classKey : classKeys) {
					data.updateBalkingParameter(stationKey, classKey, queuePolicy);
				}

				queueTable.repaint();
			}
		});

		queuePolicyEditButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PollingServerPanel serverPanel = new PollingServerPanel(null, data, stationKey, classData);
				DialogFactory factory = new DialogFactory(null);
				factory.getDialog(serverPanel, "Editing Polling Server Preferences");
			}
		});
	}

	private void updateQueueLength() {
		Integer queueLength = data.getStationQueueCapacity(stationKey);
		if (queueLength.intValue() < 1) {
			queueLengthGroup.setSelected(infiniteQueueSelector.getModel(), true);
			queueLengthSpinner.setValue(Double.POSITIVE_INFINITY);
			queueLengthSpinner.setEnabled(false);
		} else {
			queueLengthGroup.setSelected(finiteQueueSelector.getModel(), true);
			queueLengthSpinner.setValue(queueLength);
			queueLengthSpinner.setEnabled(true);
		}
	}

	private void updatePolicyEditButton() {
		queuePolicyEditButton.setEnabled(data.getStationPollingServerBoolean(stationKey));

	}
	/**
	 * @return the panel's name
	 */
	@Override
	public String getName() {
		return "Queue Section";
	}

	/**
	 * called by the Wizard when the panel becomes active
	 */
	@Override
	public void gotFocus() {
		classEditor.clearCache();
		if (data.getStationType(stationKey).equals(STATION_TYPE_SERVER)) {
			queuePolicyCombo.setModel(new DefaultComboBoxModel(serverStationQueuePolicies));
		} else {
			queuePolicyCombo.setModel(new DefaultComboBoxModel(otherStationQueuePolicies));
		}
		queuePolicyCombo.setSelectedItem(data.getStationQueueStrategy(stationKey));
	}

	/**
	 * called by the Wizard before when switching to another panel
	 */
	@Override
	public void lostFocus() {
		// Aborts editing of table
		TableCellEditor editor = queueTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}

	private Object getClassKeyFromRow(int row) {
    return classData.getClassKeys().elementAt(row);
  }

  private boolean isOpenClassAtRow(int row) {
	  Object classKey = getClassKeyFromRow(row);
	  return ((CommonModel) data).getClassType(classKey) == CommonConstants.CLASS_TYPE_OPEN;
  }
	protected class QueueTable extends JTable {

		private static final long serialVersionUID = 1L;

		private DisabledColumnRenderer dropRuleRenderer = new DisabledColumnRenderer();
		private GrayCellRenderer serviceWeightRenderer = new GrayCellRenderer();

		public QueueTable() {
			setModel(new QueueTableModel());
			setDefaultEditor(Object.class, new ExactCellEditor());
			sizeColumns();
			setRowHeight(ROW_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		private void sizeColumns() {
			for (int i = 0; i < getColumnCount(); i++) {
				getColumnModel().getColumn(i).setPreferredWidth(((QueueTableModel) getModel()).columnSizes[i]);
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == COLUMN_QUEUE_POLICY) {
				if (data.getStationType(stationKey).equals(STATION_TYPE_SERVER)) {
				    String queueStrategy = data.getStationQueueStrategy(stationKey);
					if (queueStrategy.equals(STATION_QUEUE_STRATEGY_PSSERVER)) {
						return ComboBoxCellEditor.getEditorInstance(serverPSQueuePolicies);
					} else if (queueStrategy.equals(STATION_QUEUE_STRATEGY_PREEMPTIVE)) {
						return ComboBoxCellEditor.getEditorInstance(serverPreemptiveQueuePolicies);
					} else {
						return ComboBoxCellEditor.getEditorInstance(serverNonPreemptiveQueuePolicies);
					}
				} else {
					return ComboBoxCellEditor.getEditorInstance(otherNonPreemptiveQueuePolicies);
				}
			} else if (column == COLUMN_DROP_RULE) {
				return ComboBoxCellEditor.getEditorInstance(dropRules);
			} else if (column == COLUMN_IMPATIENCE) {
				return getCellEditorForImpatienceFromClassType(row);
      } else if (column == COLUMN_EDIT) {
        Object classKey = getClassKeyFromRow(row);

				// This will ensure that the correct Editing window is returned when the Edit button is clicked
				return getCellEditorForImpatienceType(stationKey, classKey);
			} else {
				return super.getCellEditor(row, column);
			}
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == COLUMN_CLASS) {
				return classEditor.getRenderer();
			} else if (column == COLUMN_QUEUE_POLICY) {
				return ComboBoxCellEditor.getRendererInstance();
			} else if (column == COLUMN_DROP_RULE) {
				return dropRuleRenderer;
			} else if (column == COLUMN_SERVICE_WEIGHT) {
				return serviceWeightRenderer;
			} else if (column == COLUMN_IMPATIENCE) {
        return ComboBoxCellEditor.getRendererInstance();
      } else if (column == COLUMN_EDIT) {
				return createAndRenderEditButton(row);
			} else {
				return super.getCellRenderer(row, column);
			}
		}
	/** This method was created for the getCellEditor() method. Depending on the type of class (open/closed)
		 * for that row, the appropriate list of impatience strategies in the UI is returned.
		 * @param row The row of the table.
		 * @return The ComboBox containing the appropriate list of impatience strategies available */
		private ComboBoxCellEditor getCellEditorForImpatienceFromClassType(int row) {
      boolean isOpenClass = isOpenClassAtRow(row);

			// Closed classes should never have any impatience strategies attached to it; only open classes should
			if (isOpenClass) {
        return ComboBoxCellEditor.getEditorInstance(impatienceStrategies.toArray());
			} else {
        String[] selectableImpatienceStrategies = {ImpatienceType.NONE.getDisplayName()};
        return ComboBoxCellEditor.getEditorInstance(selectableImpatienceStrategies);
			}
		}

		/** This method was created for the getCellEditor() method. Depending on the type of impatience
		 * strategy selected for that row, the appropriate editing window will be returned. If there is
		 * no impatience selected, no editing window will be returned.
		 * @param stationKey The key of the station.
		 * @param classKey The key of the class.
		 * @return The appropriate editing window depending on the type of impatience strategy. */
		private ButtonCellEditor getCellEditorForImpatienceType(Object stationKey, Object classKey) {
			ImpatienceType impatienceType = data.getImpatienceType(stationKey, classKey);

      return new ButtonCellEditor(new JButton(editImpatienceParameter(impatienceType)));
		}

		// An action to bring up a new window to edit the distribution based on the selected impatience type
		private AbstractAction editImpatienceParameter(final ImpatienceType impatienceType) {
			return new AbstractAction("Edit") {

				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					int selectedRow = queueTable.getSelectedRow();
					Object classKey = getClassKeyFromRow(selectedRow);

					if (isValidRow(selectedRow)) {
						ImpatienceParameter impatienceParameter = data.getImpatienceParameter(stationKey, classKey);
						String className = classData.getClassName(classKey);
						impatienceParameter = ImpatienceEditorFactory.
								displayEditorAndReturnParameter(impatienceType, impatienceParameter,
										InputSectionPanel.this.getParent(), className);

						data.setImpatienceParameter(stationKey, classKey, impatienceParameter);
						queueTable.repaint();
					}
				}
			};
		}

		/** This method:
		 * 1) Gets the corresponding editButtonRenderer based on the row parameter
		 * 2) Enables/disables the edit button depending on the selected impatience strategy for that row
		 * 3) Returns that edit button
		 * @param row The row for the edit button
		 * @return The edit button with the enabled/disabled status set appropriately. */
		private ButtonCellEditor createAndRenderEditButton(int row) {
			DisabledButtonCellRenderer editButton = 	createEditButton();
			renderEditButton(row, editButton);

			return editButton;
		}

		private DisabledButtonCellRenderer createEditButton() {
			DisabledButtonCellRenderer editButton = new DisabledButtonCellRenderer(
					new JButton() {
						private static final long serialVersionUID = 1L;
						{
							setText("Edit");
						}
					}
			);
			return editButton;
		}

		private void renderEditButton(int rowOfEditButton, DisabledButtonCellRenderer editButton) {
			// Sets the enabled/disabled status of the edit button depending on its type of impatience
			if (isImpatienceTypeAtRow(ImpatienceType.NONE, rowOfEditButton)) {
				editButton.setEnabled(false);
			} else {
				editButton.setEnabled(true);
			}
		}

		private boolean isImpatienceTypeAtRow(ImpatienceType type, int row) {
      Object classKey = getClassKeyFromRow(row);
			return data.getImpatienceType(stationKey, classKey) == type;
		}

		private boolean isValidRow(int selectedRow) {
			return selectedRow >= 0 && selectedRow < queueTable.getRowCount();
		}
	}

	protected class QueueTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private String[] columnNames = new String[] { "Class", "Queue Policy", "Drop Rule", "Service Weight", "Impatience", "" };
		private Class<?>[] columnClasses = new Class[] { String.class, String.class, String.class, String.class, String.class, Object.class };
		public int[] columnSizes = new int[] { 100, 100, 100, 100, 100, 30 };

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
			Object classKey = getClassKeyFromRow(rowIndex);
			if (columnIndex == COLUMN_CLASS) {
				return false;
			} else if (columnIndex == COLUMN_DROP_RULE && data.getStationQueueCapacity(stationKey).intValue() < 1) {
				return false;
			} else if (columnIndex == COLUMN_SERVICE_WEIGHT && (!data.getQueueStrategy(stationKey, classKey).equals(QUEUE_STRATEGY_GPS)
					&& !data.getQueueStrategy(stationKey, classKey).equals(QUEUE_STRATEGY_DPS))) {
				return false;
			} else if (columnIndex == COLUMN_IMPATIENCE) {
			  return isOpenClassAtRow(rowIndex);
      } else if (columnIndex == COLUMN_EDIT) {
				return !queueTable.isImpatienceTypeAtRow(ImpatienceType.NONE, rowIndex);
			} else {
				return true;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object classKey = indexToKey(rowIndex);
			if (columnIndex == 0) {
				return classKey;
			} else if (columnIndex == 1) {
				return data.getQueueStrategy(stationKey, classKey);
			} else if (columnIndex == 2) {
				if (data.getStationQueueCapacity(stationKey).intValue() < 1) {
					return INFINITE_CAPACITY;
				} else {
					return data.getDropRule(stationKey, classKey);
				}
			} else if (columnIndex == 3) {
				if (!data.getQueueStrategy(stationKey, classKey).equals(QUEUE_STRATEGY_GPS)
						&& !data.getQueueStrategy(stationKey, classKey).equals(QUEUE_STRATEGY_DPS)) {
					return "--";
				} else {
					return data.getServiceWeight(stationKey, classKey);
				}
			} else if (columnIndex == COLUMN_IMPATIENCE) {
				ImpatienceType impatienceType = data.getImpatienceType(stationKey, classKey);
				return impatienceType.getDisplayName();
			} else {
				return null;
			}
		}

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			Object classKey = getClassKeyFromRow(rowIndex);
			if (columnIndex == COLUMN_QUEUE_POLICY) {
				String policy = (String) aValue;
				data.setQueueStrategy(stationKey, classKey, policy);
				if (policy.equals(QUEUE_STRATEGY_PS)) {
					data.setServiceWeight(stationKey, classKey, Defaults.getAsDouble("serviceWeight"));
				}
				repaint();
			} else if (columnIndex == COLUMN_DROP_RULE) {
				data.setDropRule(stationKey, classKey, (String) aValue);
				if (aValue.toString().equals(FINITE_RETRIAL)) {
					Distribution retrialDistribution = (Distribution) data.getRetrialDistribution(stationKey, classKey);
					DistributionsEditor editor = DistributionsEditor.getInstance(InputSectionPanel.this.getParent(), retrialDistribution);
					String className = classData.getClassName(classKey);
					editor.setTitle("Editing " + className + " Retrial Time Distribution...");
					editor.show();
					data.setRetrialDistribution(stationKey, classKey, editor.getResult());
					queueTable.repaint();
				}
			} else if (columnIndex == COLUMN_SERVICE_WEIGHT) {
				try {
					Double weight = Double.valueOf((String) aValue);
					if (weight.doubleValue() <= 0.0) {
						weight = Defaults.getAsDouble("serviceWeight");
					}
					data.setServiceWeight(stationKey, classKey, weight);
				} catch (NumberFormatException e) {
					// Aborts modification if String is invalid
				}
			} else if (columnIndex == COLUMN_IMPATIENCE) {
				String impatienceString = aValue.toString();
				ImpatienceType impatienceType = ImpatienceType.getType(impatienceString);

        if (selectedImpatienceWasDifferentOrNone(classKey, impatienceType)) {
        	data.resetImpatience(stationKey, classKey);
					setDefaultImpatienceParameter(classKey, impatienceType);
					data.setImpatienceType(stationKey, classKey, impatienceType);
				}
				queueTable.repaint();
			}
		}

		private Object indexToKey(int index) {
			return classData.getClassKeys().get(index);
		}

	private void setDefaultRetrialDistributionParam(Object classKey) {
			Distribution distribution = (Distribution) Defaults.getAsNewInstance("classDistribution");
			data.setRetrialDistribution(stationKey, classKey, new RetrialDistribution(distribution));
		}

		/** Returns a boolean variable indicating whether the selected impatience strategy is different
     * from the preivous impatience strategy, or whether the selected impatience strategy is "NONE" */
		private boolean selectedImpatienceWasDifferentOrNone(Object classKey, ImpatienceType selectedImpatience) {
      return
          data.getImpatienceType(stationKey, classKey) != selectedImpatience
              || selectedImpatience == ImpatienceType.NONE;
    }

 /** Sets the default ImpatienceParameter for the StationClass depending on the ImpatienceType */
    private void setDefaultImpatienceParameter(Object classKey, ImpatienceType impatienceType) {
      switch (impatienceType) {
        case NONE:
          break;
        case RENEGING:
          Distribution distribution = (Distribution) Defaults.getAsNewInstance("classDistribution");
          data.setImpatienceParameter(stationKey, classKey, new RenegingParameter(distribution));
          break;
        case BALKING:
        	String serverStationQueuePolicy = (String) serverStationQueuePolicies[queuePolicyCombo.getSelectedIndex()];
        	String classQueueStrategy = data.getQueueStrategy(stationKey, classKey);
          data.setImpatienceParameter(stationKey, classKey, new BalkingParameter(serverStationQueuePolicy, classQueueStrategy));
          break;
      }
    }
  }

	/**
	 * <p><b>Name:</b> DisabledColumnRenderer</p> 
	 * <p><b>Description: </b> A special renderer that will show disabled text when
	 * queue capacity is infinite, otherwise will show a combobox. 
	 * 
	 * </p>
	 * <p><b>Date:</b> 21/ott/06
	 * <b>Time:</b> 15:59:56</p>
	 * @author Bertoli Marco
	 * @version 1.0
	 */
	private class DisabledColumnRenderer extends ComboBoxCellEditor {

		private static final long serialVersionUID = 1L;
		private DisabledCellRenderer disableRenderer;

		public DisabledColumnRenderer() {
			disableRenderer = new DisabledCellRenderer();
			disableRenderer.setEnabled(false);
		}

		/* (non-Javadoc)
		 * @see jmt.gui.common.editors.ComboBoxCellEditor#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (!table.isCellEditable(row, column)) {
				disableRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				disableRenderer.setHorizontalAlignment(SwingConstants.LEFT);
				return disableRenderer;
			} else {
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}

	}

	private class DisabledButtonCellRenderer extends ButtonCellEditor {

		private static final long serialVersionUID = 1L;

		private JButton button;
		private boolean enabled;

		public DisabledButtonCellRenderer(JButton jbutt) {
			super(jbutt);
			button = jbutt;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}

		public void setEnabled(boolean value) {
			enabled = value;
			button.setEnabled(value);
		}
	}
}
