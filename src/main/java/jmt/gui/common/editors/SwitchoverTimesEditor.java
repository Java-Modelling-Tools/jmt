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
import jmt.framework.gui.table.editors.ComboBoxCellEditor;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.gui.common.CommonConstants;
import jmt.gui.common.definitions.ClassDefinition;
import jmt.gui.common.definitions.StationDefinition;
import jmt.gui.common.distributions.Distribution;
import jmt.gui.common.distributions.Exponential;
import jmt.gui.common.panels.WarningScrollTable;
import jmt.gui.common.serviceStrategies.DisabledStrategy;
import jmt.gui.common.serviceStrategies.LDStrategy;
import jmt.gui.common.serviceStrategies.ServiceStrategy;
import jmt.gui.common.serviceStrategies.ZeroStrategy;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

public class SwitchoverTimesEditor extends WizardPanel implements CommonConstants {

	private static final long serialVersionUID = 1L;

	protected StationDefinition data;
	protected Object stationKey;

	protected JCheckBox useSwitchoverTimesCheckbox;
	protected SwitchoverTimesTable serviceTable;
	protected JRootPane tablePane;
	protected ServiceStrategy bulkSwitchoverDistribution;

	public SwitchoverTimesEditor(StationDefinition sd, ClassDefinition cd, Object stationKey) {
		initData(sd, cd, stationKey);
		initGUI(sd, cd, stationKey);
	}

	private void initData(StationDefinition sd, ClassDefinition cd, Object stationKey) {
		data = sd;
		this.stationKey = stationKey;
		this.bulkSwitchoverDistribution = new ZeroStrategy();
	}

	private void initGUI(StationDefinition sd, ClassDefinition cd, final Object stationKey) {
		this.setLayout(new BorderLayout());

		final WizardPanel parent = this;

		useSwitchoverTimesCheckbox = new JCheckBox("Enable Switchover Times");
		boolean switchoverTimesEnabled = data.getSwitchoverTimesEnabled(stationKey).booleanValue();
		useSwitchoverTimesCheckbox.setSelected(switchoverTimesEnabled);
		useSwitchoverTimesCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				boolean switchoverTimesEnabled = useSwitchoverTimesCheckbox.isSelected();
				final String queuePolicy = data.getStationQueueStrategy(stationKey);
				if (queuePolicy.equals(STATION_QUEUE_STRATEGY_NON_PREEMPTIVE) || queuePolicy.equals(STATION_QUEUE_STRATEGY_NON_PREEMPTIVE_PRIORITY)) {
					data.setSwitchoverTimesEnabled(stationKey, Boolean.valueOf(switchoverTimesEnabled));
					tablePane.getGlassPane().setVisible(!switchoverTimesEnabled); 
				} else {
					JOptionPane.showMessageDialog(parent, "Switchover times require non-preemptive scheduling", "Error", JOptionPane.ERROR_MESSAGE);
					useSwitchoverTimesCheckbox.setSelected(false);
				}	
			}
		});

		serviceTable = new SwitchoverTimesTable(sd, cd, stationKey, this);
		WarningScrollTable serviceSectionTable = new WarningScrollTable(serviceTable, WARNING_MULTI_CLASS);

		JPanel descriptionPanel = new JPanel();
		descriptionPanel.setBorder(new TitledBorder(new EtchedBorder(), "Description"));
		JLabel switchoverTimesText = new JLabel("Switchover times are incurred when a server switches the class that it is currently serving.");
		descriptionPanel.add(switchoverTimesText);

		JPanel switchoverTimesPanel = new JPanel();
		switchoverTimesPanel.setBorder(new TitledBorder(new EtchedBorder(), "Switchover Times"));
		switchoverTimesPanel.setLayout(new BorderLayout());
		switchoverTimesPanel.add(useSwitchoverTimesCheckbox, BorderLayout.NORTH);

		tablePane = new JRootPane();
		tablePane.getContentPane().setLayout(new BorderLayout());
		tablePane.getContentPane().add(serviceSectionTable, BorderLayout.CENTER);

		// Add Bulk Editor
		JPanel bulkEditorPanel = new JPanel(new FlowLayout());
		bulkEditorPanel.setBorder(new TitledBorder(new EtchedBorder(), "Bulk Edit"));
		bulkEditorPanel.add(new BulkSwitchoverTimesEditor());
		JButton applyBulkButton = new JButton("Apply to Checked");
		applyBulkButton.setAction(bulkApplyDistribution);
		bulkEditorPanel.add(applyBulkButton);

		tablePane.getContentPane().add(bulkEditorPanel, BorderLayout.SOUTH);
		tablePane.setGlassPane(new GlassPane());
		tablePane.getGlassPane().setVisible(!switchoverTimesEnabled);
		switchoverTimesPanel.add(tablePane, BorderLayout.CENTER);

		add(descriptionPanel, BorderLayout.NORTH);
		add(switchoverTimesPanel, BorderLayout.CENTER);
	}

	@Override
	public String getName() {
		return "Switchover Times";
	}

	protected static class GlassPane extends JComponent {

		private static final long serialVersionUID = 1L;

		public GlassPane() {
			setOpaque(false);
			setVisible(false);
			Color base = UIManager.getColor("inactiveCaptionBorder");
			base = (base == null) ? Color.LIGHT_GRAY : base;
			Color background = new Color(base.getRed(), base.getGreen(), base.getBlue(), 128);
			setBackground(background);

			// Disable Mouse events for the panel
			addMouseListener(new MouseAdapter() {});
			addMouseMotionListener(new MouseMotionAdapter() {});
		}

		/**
		 * The component is transparent but we want to paint the background
		 * to give it the disabled look.
		 */
		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getSize().width, getSize().height);
		}

	}

	protected class BulkSwitchoverTimesEditor extends JTable {

		private static final long serialVersionUID = 1L;

		/**
		 * This field is used to initialize elements shown on Service type selection - Bertoli Marco
		 */
		protected Object[] serviceType = new Object[] { SERVICE_LOAD_INDEPENDENT, SERVICE_LOAD_DEPENDENT, SERVICE_ZERO, SERVICE_DISABLED };

		JButton editButton = new JButton("Edit");

		int[] columnSizes = new int[] { 150, 200, 75 };

		public BulkSwitchoverTimesEditor() {
			setModel(new BulkSwitchoverTimesEditorModel());
			sizeColumns();
			setRowHeight(ROW_HEIGHT);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			getTableHeader().setReorderingAllowed(false);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 0) {
				return ComboBoxCellEditor.getRendererInstance();
			} else if (column == 2) {
				return new DisabledButtonCellRenderer(editButton);
			} else {
				return super.getCellRenderer(row, column);
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 0) {
				return ComboBoxCellEditor.getEditorInstance(serviceType);
			} else if (column == 2) {
				return new ButtonCellEditor(new JButton(editDistribution));
			} else {
				return super.getCellEditor(row, column);
			}
		}

		private void sizeColumns() {
			for (int i = 0; i < columnSizes.length && i < getColumnCount(); i++) {
				this.getColumnModel().getColumn(i).setPreferredWidth(columnSizes[i]);
			}
		}

	}

	// allow editing bulk switchover time distribution section
	protected AbstractAction editDistribution = new AbstractAction("Edit") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Edits Switchover Time Distribution");
		}

		public void actionPerformed(ActionEvent e) {
			// ---- Bertoli Marco ----
			// If it is a Distribution, shows Distribution Editor
			if (bulkSwitchoverDistribution instanceof Distribution) {
				DistributionsEditor editor = DistributionsEditor.getInstance(SwitchoverTimesEditor.this, (Distribution) bulkSwitchoverDistribution);
				// Sets editor window title
				editor.setTitle("Editing Bulk Switchover Time Distribution...");
				// Shows editor window
				editor.setVisible(true);
				// Sets new Distribution to selected class
				bulkSwitchoverDistribution = editor.getResult();
			}
			// Otherwise shows LDStrategy Editor
			else {
				LDStrategyEditor editor = LDStrategyEditor.getInstance(SwitchoverTimesEditor.this, (LDStrategy) bulkSwitchoverDistribution);
				// Sets editor window title
				editor.setTitle("Editing Bulk Load Dependent Switchover Time Strategy...");
				// Shows editor window
				editor.setVisible(true);
			}
			// Updates table view
			serviceTable.repaint();
		}

	};

	protected class BulkSwitchoverTimesEditorModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private final String[] columnNames = new String[] { "Strategy", "Service Time Distribution", "" };
		private final Class<?>[] columnClasses = new Class[] { String.class, String.class, Object.class };

		@Override
		public int getRowCount() {
			return 1;
		}

		@Override
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
				return true;
			} else if (columnIndex == 2 && !(bulkSwitchoverDistribution instanceof ZeroStrategy)
					&& !(bulkSwitchoverDistribution instanceof DisabledStrategy)) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case (0):
				// Checks if current service section is load dependent or independent
				if (bulkSwitchoverDistribution instanceof LDStrategy) {
					return SERVICE_LOAD_DEPENDENT;
				} else if (bulkSwitchoverDistribution instanceof ZeroStrategy) {
					return SERVICE_ZERO;
				} else if (bulkSwitchoverDistribution instanceof DisabledStrategy) {
					return SERVICE_DISABLED;
				} else {
					return SERVICE_LOAD_INDEPENDENT;
				}
			case (1):
				return bulkSwitchoverDistribution;
			}
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				if (aValue.equals(SERVICE_LOAD_DEPENDENT) && !(bulkSwitchoverDistribution instanceof LDStrategy)) {
					bulkSwitchoverDistribution = new LDStrategy();
				} else if (aValue.equals(SERVICE_ZERO) && !(bulkSwitchoverDistribution instanceof ZeroStrategy)) {
					bulkSwitchoverDistribution = new ZeroStrategy();
				} else if (aValue.equals(SERVICE_DISABLED) && !(bulkSwitchoverDistribution instanceof DisabledStrategy)) {
					bulkSwitchoverDistribution = new DisabledStrategy();
				} else if (aValue.equals(SERVICE_LOAD_INDEPENDENT) && !(bulkSwitchoverDistribution instanceof Distribution)) {
					bulkSwitchoverDistribution = new Exponential();
				}
				repaint();
			}
		}

	}

	// apply bulk switchover time distribution
	protected AbstractAction bulkApplyDistribution = new AbstractAction("Apply to Checked") {

		private static final long serialVersionUID = 1L;

		{
			putValue(Action.SHORT_DESCRIPTION, "Bulk Applies Switchover Time Distribution");
		}

		public void actionPerformed(ActionEvent e) {
			serviceTable.applyBulkSelected(bulkSwitchoverDistribution);
		}

	};

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
