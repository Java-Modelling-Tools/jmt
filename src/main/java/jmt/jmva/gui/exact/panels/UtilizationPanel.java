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

package jmt.jmva.gui.exact.panels;

import jmt.gui.exact.table.ExactTableModel;
import jmt.jmva.analytical.ExactConstants;
import jmt.jmva.analytical.solvers.SolverAlgorithm;
import jmt.jmva.gui.exact.ExactWizard;

public final class UtilizationPanel extends SolutionPanel {

	private static final long serialVersionUID = 1L;
	private double[][][] util;
	private double[][] stationAggs;

	/* EDITED by Abhimanyu Chugh */
	public UtilizationPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>Utilization</html>";
		name = "Utilization";
	}
	/* END */

	/**
	 * gets status from data object
	 */
	@Override
	protected void sync() {
		super.sync();
		/* EDITED by Abhimanyu Chugh */
		util = data.getUtilization(algorithm);
		stationAggs = data.getPerStationU(algorithm);
		/* END */
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new UTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_UTILIZATIONS;
	}

	/**
	 * the model backing the visit table.
	 * Rows represent stations, columns classes.
	 */
	private class UTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		UTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (util == null) {
				return 0;
			}
			return stations;
		}

		public int getColumnCount() {
			if (util == null) {
				return 0;
			}
			return classes + 1;
		}

		@Override
		protected Object getRowName(int rowIndex) {
			return stationNames[rowIndex];
		}

		@Override
		public String getColumnName(int index) {
			if (index == 0) {
				return "<html><i>Aggregate</i></html>";
			}
			return classNames[index - 1];
		}

		@Override
		protected Object getValueAtImpl(int rowIndex, int columnIndex) {
			double d = -1.0;

			if (columnIndex == 0) {
				d = stationAggs[rowIndex][iteration];
			} else if (columnIndex > 0) {
				d = util[rowIndex][columnIndex - 1][iteration];
			}

			if (d < 0.0) {
				return new String("--");
			} else {
				return new Double(d);
			}
		}

	}

}
