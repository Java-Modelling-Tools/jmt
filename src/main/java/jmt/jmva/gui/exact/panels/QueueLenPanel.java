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

public final class QueueLenPanel extends SolutionPanel {

	private static final long serialVersionUID = 1L;
	private double[][][] queueLen;
	double[][] classAggs;
	double[][] stationAggs;
	double[] globalAgg;

	/* EDITED by Abhimanyu Chugh */
	public QueueLenPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>Number of Customers</html>";
		name = "Number of Customers";
	}
	/* END */

	/**
	 * gets status from data object
	 */
	@Override
	protected void sync() {
		super.sync();
		/* EDITED by Abhimanyu Chugh */
		queueLen = data.getQueueLen(algorithm);
		classAggs = data.getPerClassQ(algorithm);
		stationAggs = data.getPerStationQ(algorithm);
		globalAgg = data.getGlobalQ(algorithm);
		/* END */
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new QLTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_QUEUELENGTHS;
	}

	/**
	 * the model backing the visit table.
	 * Rows represent stations, columns classes.
	 */
	private class QLTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		QLTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (queueLen == null) {
				return 0;
			}
			return stations + 1;
		}

		public int getColumnCount() {
			if (queueLen == null) {
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
		public String getColumnName(int index) {
			if (index == 0) {
				return "<html><i>Aggregate</i></html>";
			} else {
				return classNames[index - 1];
			}
		}

		@Override
		protected Object getValueAtImpl(int rowIndex, int columnIndex) {
			double d = -1.0;

			if (rowIndex == 0 && columnIndex == 0) {
				d = globalAgg[iteration];
			} else if (rowIndex == 0 && columnIndex > 0) {
				d = classAggs[columnIndex - 1][iteration];
			} else if (rowIndex > 0 && columnIndex == 0) {
				d = stationAggs[rowIndex - 1][iteration];
			} else if (rowIndex > 0 && columnIndex > 0) {
				d = queueLen[rowIndex - 1][columnIndex - 1][iteration];
			}

			if (d < 0.0) {
				return new String("--");
			} else {
				return new Double(d);
			}
		}

	}

}
