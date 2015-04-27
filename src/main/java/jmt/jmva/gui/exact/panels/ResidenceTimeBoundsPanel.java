package jmt.jmva.gui.exact.panels;

import java.text.DecimalFormat;

import javax.swing.JTable;

import jmt.jmva.analytical.SolverAlgorithm;
import jmt.jmva.gui.exact.ExactConstants;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;

/**
 * Results panel for residence time bounds.
 * @author Ben Homer, 2014.
 */
public final class ResidenceTimeBoundsPanel extends SolutionPanel {

	private static final long serialVersionUID = 1L;
	
	/** Per station, per class residence time bounds. */
	private double[][][][] residenceTimeBounds;
	
	/** Per class residence time bounds. */
	private double[][][] clsResidenceTimeBounds;
	
	/** Per stations residence time bounds. */
	private double[][][] stResidenceTimeBounds;
	
	/** Global response time bounds. */
	private double[][] globalResponseTimeBounds;

	/**
	 * Constructor.
	 * @param ew The ExactWizard instance.
	 * @param alg The solver algorithm.
	 */
	public ResidenceTimeBoundsPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>Residence Time Bounds</html>";
		name = "Residence Time Bounds";
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	@Override
	protected void sync() {
		super.sync();
		residenceTimeBounds = data.getResidenceTimeBounds(algorithm);
		clsResidenceTimeBounds = data.getPerClassRBounds(algorithm);
		stResidenceTimeBounds = data.getPerStationRBounds(algorithm);
		globalResponseTimeBounds = data.getGlobalRBounds(algorithm);
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new TPTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_RESPONSETIME_BOUNDS;
	}

	private class TPTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		TPTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (residenceTimeBounds == null) {
				return 0;
			}
			return stations + 1;
		}

		public int getColumnCount() {
			if (residenceTimeBounds == null) {
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
			
			if (algorithm.equals(SolverAlgorithm.BALANCED_JOB_BOUNDS)) {
				if (rowIndex == 0 && columnIndex == 0) {
					return createBoundsString(
							globalResponseTimeBounds[0][iteration], globalResponseTimeBounds[1][iteration]);
				} else {
					return "-";
				}
			}
			
			if (rowIndex == 0 && columnIndex == 0) {
				s = createBoundsString(globalResponseTimeBounds[0][iteration], globalResponseTimeBounds[1][iteration]);
			} else if (rowIndex == 0 && columnIndex > 0) {
				s = createBoundsString(clsResidenceTimeBounds[columnIndex - 1][0][iteration], 
						clsResidenceTimeBounds[columnIndex - 1][1][iteration]);
			} else if (rowIndex > 0 && columnIndex == 0) {
				s = createBoundsString(stResidenceTimeBounds[rowIndex - 1][0][iteration], 
						stResidenceTimeBounds[rowIndex - 1][1][iteration]);
			} else {
				s = createBoundsString(residenceTimeBounds[rowIndex - 1][columnIndex - 1][0][iteration],
						residenceTimeBounds[rowIndex - 1][columnIndex - 1][1][iteration]);
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
