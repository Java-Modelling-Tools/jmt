package jmt.jmva.gui.exact.panels;

import java.text.DecimalFormat;

import javax.swing.JTable;

import jmt.jmva.analytical.SolverAlgorithm;
import jmt.jmva.gui.exact.ExactConstants;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;

/**
 * Results panel for single and multi-class throughput bounds.
 * @author Ben Homer, 2014.
 */
public final class ThroughputBoundsPanel extends SolutionPanel {

	private static final long serialVersionUID = 1L;
	
	/** Per station, per class throughput bounds. */
	private double[][][][] throughputBounds;
	
	/** Per class throughput bounds. */
	private double[][][] clsThroughputBounds;
	
	/** Per station throughput bounds. */
	private double[][][] stThroughputBounds;
	
	/** Global throughput bounds. */
	private double[][] globalThroughputBounds;

	/**
	 * Constructor.
	 * @param ew The ExactWizard instance.
	 * @param alg The solver algorithm.
	 */
	public ThroughputBoundsPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>Throughput Bounds</html>";
		name = "Throughput Bounds";
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	@Override
	protected void sync() {
		super.sync();
		throughputBounds = data.getThroughputBounds(algorithm);
		clsThroughputBounds = data.getPerClassXBounds(algorithm);
		stThroughputBounds = data.getPerStationXBounds(algorithm);
		globalThroughputBounds = data.getGlobalXBounds(algorithm);
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new TPTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_THROUGHPUT_BOUNDS;
	}

	private class TPTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		TPTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (throughputBounds == null) {
				return 0;
			}
			return stations + 1;
		}

		public int getColumnCount() {
			if (throughputBounds == null) {
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
				s = createBoundsString(globalThroughputBounds[0][iteration], globalThroughputBounds[1][iteration]);
			} else if (rowIndex == 0 && columnIndex > 0) {
				s = createBoundsString(clsThroughputBounds[columnIndex - 1][0][iteration], 
						clsThroughputBounds[columnIndex - 1][1][iteration]);
			} else if (rowIndex > 0 && columnIndex == 0) {
				s = createBoundsString(stThroughputBounds[rowIndex - 1][0][iteration], 
						stThroughputBounds[rowIndex - 1][1][iteration]);
			} else {
				s = createBoundsString(throughputBounds[rowIndex - 1][columnIndex - 1][0][iteration],
						throughputBounds[rowIndex - 1][columnIndex - 1][1][iteration]);
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
