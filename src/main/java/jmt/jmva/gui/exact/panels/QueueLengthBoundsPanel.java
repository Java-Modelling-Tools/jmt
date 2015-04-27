package jmt.jmva.gui.exact.panels;

import java.text.DecimalFormat;

import javax.swing.JTable;

import jmt.jmva.analytical.SolverAlgorithm;
import jmt.jmva.gui.exact.ExactConstants;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;

/**
 * Results panel for queue length bounds.
 * @author Ben Homer, 2014.
 */
public final class QueueLengthBoundsPanel extends SolutionPanel {

	private static final long serialVersionUID = 1L;
	
	/** Per station, per class queue length bounds. */
	private double[][][][] queueLengthBounds;
	
	/** Per class queue length bounds. */
	private double[][][] clsQueueLengthBounds;
	
	/** Per station queue length bounds. */
	private double[][][] stQueueLengthBounds;
	
	/** Global queue length bounds. */
	private double[][] globalQueueLengthBounds;

	/**
	 * Constructor.
	 * @param ew The ExactWizard instance.
	 * @param alg The solver algorithm.
	 */
	public QueueLengthBoundsPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>Number of Customer Bounds</html>";
		name = "Number of Customer Bounds";
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	@Override
	protected void sync() {
		super.sync();
		queueLengthBounds = data.getQueueLengthBounds(algorithm);
		clsQueueLengthBounds = data.getPerClassQBounds(algorithm);
		stQueueLengthBounds = data.getPerStationQBounds(algorithm);
		globalQueueLengthBounds = data.getGlobalQBounds(algorithm);
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new TPTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_QUEUELENGTH_BOUNDS;
	}

	private class TPTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		TPTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (queueLengthBounds == null) {
				return 0;
			}
			return stations + 1;
		}

		public int getColumnCount() {
			if (queueLengthBounds == null) {
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
			if (algorithm.equals(SolverAlgorithm.BALANCED_JOB_BOUNDS)) {
				if (rowIndex == 0 && columnIndex == 0) {
					return createBoundsString(
							globalQueueLengthBounds[0][iteration], globalQueueLengthBounds[1][iteration]);
				} else {
					return "-";
				}
			}
			
			String s = null;
			if (rowIndex == 0 && columnIndex == 0) {
				s = createBoundsString(globalQueueLengthBounds[0][iteration], globalQueueLengthBounds[1][iteration]);
			} else if (rowIndex == 0 && columnIndex > 0) {
				s = createBoundsString(clsQueueLengthBounds[columnIndex - 1][0][iteration], 
						clsQueueLengthBounds[columnIndex - 1][1][iteration]);
			} else if (rowIndex > 0 && columnIndex == 0) {
				s = createBoundsString(stQueueLengthBounds[rowIndex - 1][0][iteration], 
						stQueueLengthBounds[rowIndex - 1][1][iteration]);
			} else {
				s = createBoundsString(queueLengthBounds[rowIndex - 1][columnIndex - 1][0][iteration],
						queueLengthBounds[rowIndex - 1][columnIndex - 1][1][iteration]);
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