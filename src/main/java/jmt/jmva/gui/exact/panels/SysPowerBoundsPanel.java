package jmt.jmva.gui.exact.panels;

import java.text.DecimalFormat;

import javax.swing.JTable;

import jmt.jmva.analytical.SolverAlgorithm;
import jmt.jmva.gui.exact.ExactConstants;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;

/**
 * Results panel for system power bounds.
 * @author Ben Homer, 2014.
 */
public final class SysPowerBoundsPanel extends SolutionPanel {

	private static final long serialVersionUID = 1L;
	
	/** Per class system power bounds. */
	private double[][][] clsSystemPowerBounds;
	
	/** Global system power bounds. */
	private double[][] globalSystemPowerBounds;

	/**
	 * Constructor.
	 * @param ew The ExactWizard instance.
	 * @param alg The solver algorithm.
	 */
	public SysPowerBoundsPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>System Power Bounds</html>";
		name = "System Power Bounds";
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	@Override
	protected void sync() {
		super.sync();
		globalSystemPowerBounds = data.getGlobalSPBounds(algorithm);
		clsSystemPowerBounds = data.getPerClassSPBounds(algorithm);
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new TPTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_SYSPOWER_BOUNDS;
	}

	private class TPTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		TPTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (globalSystemPowerBounds == null) {
				return 0;
			}
			return 1;
		}

		public int getColumnCount() {
			if (globalSystemPowerBounds == null) {
				return 0;
			}
			return classes + 1;
		}

		@Override
		protected Object getRowName(int rowIndex) {
			if (rowIndex == 0) {
				return "<html><i>Aggregate</i></html>";
			} else {
				return stationNames[rowIndex - 1];
			}
		}

		@Override
		public String getColumnName(int colIndex) {
			if (colIndex == 0) {
				return "<html><i>Aggregate</i></html>";
			} else {
				return classNames[colIndex - 1];
			}
		}

		@Override
		protected Object getValueAtImpl(int rowIndex, int columnIndex) {
			String s = null;
			if (rowIndex == 0 && columnIndex == 0) {
				s = createBoundsString(globalSystemPowerBounds[0][iteration], globalSystemPowerBounds[1][iteration]);
			} else if (rowIndex == 0 && columnIndex > 0) {
				s = createBoundsString(clsSystemPowerBounds[columnIndex - 1][0][iteration], 
						clsSystemPowerBounds[columnIndex - 1][1][iteration]);
			} else if (rowIndex > 0 && columnIndex == 0) {
				return null;
			} else {
				return null;
			}
			return s;
		}
		
		private String createBoundsString(double lb, double ub) {
			if (lb < 0 || ub < 0) return "-";
			DecimalFormat f = new DecimalFormat("#.#####");
			return f.format(lb) + " - " + f.format(ub);
		}
	}
}
