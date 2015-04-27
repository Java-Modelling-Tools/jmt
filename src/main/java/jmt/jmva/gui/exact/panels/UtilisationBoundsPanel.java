package jmt.jmva.gui.exact.panels;

import java.text.DecimalFormat;

import javax.swing.JTable;

import jmt.jmva.analytical.SolverAlgorithm;
import jmt.jmva.gui.exact.ExactConstants;
import jmt.jmva.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;

/**
 * Results panel for utilization bounds.
 * @author Ben Homer
 */
public final class UtilisationBoundsPanel extends SolutionPanel {

	private static final long serialVersionUID = 1L;
	
	/** Per station, per class utilisation bounds. */
	private double[][][][] utilisationBounds;
	
	/** Per station utilisation bounds. */
	private double[][][] stUtilisationBounds;

	/**
	 * Constructor.
	 * @param ew The ExactWizard instance.
	 * @param alg The solver algorithm.
	 */
	public UtilisationBoundsPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>Utilisation Bounds</html>";
		name = "Utilisation Bounds";
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	@Override
	protected void sync() {
		super.sync();
		utilisationBounds = data.getUtilisationBounds(algorithm);
		stUtilisationBounds = data.getPerStationUBounds(algorithm);
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new TPTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_UTILIZATION_BOUNDS;
	}

	private class TPTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		TPTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (utilisationBounds == null) {
				return 0;
			}
			return stations + 1;
		}

		public int getColumnCount() {
			if (utilisationBounds == null) {
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
				s = "-";
			} else if (rowIndex == 0 && columnIndex > 0) {
				s = "-";
			} else if (rowIndex > 0 && columnIndex == 0) {
				s = createBoundsString(stUtilisationBounds[rowIndex - 1][0][iteration], 
						stUtilisationBounds[rowIndex - 1][1][iteration]);
			} else {
				s = createBoundsString(utilisationBounds[rowIndex - 1][columnIndex - 1][0][iteration],
						utilisationBounds[rowIndex - 1][columnIndex - 1][1][iteration]);
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
