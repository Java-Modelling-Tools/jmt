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

public class SystemPowerPanel extends SolutionPanel implements ExactConstants {

	private static final long serialVersionUID = 1L;
	private double[][][] resTimes;
	private double[][] classAggs;
	private double[][] classAggsWO;
	private double[] globalAgg;
	private double[] globalAggWO;

	/* EDITED by Abhimanyu Chugh */
	public SystemPowerPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>System Power</html>";
		name = "System Power";
	}
	/* END */

	@Override
	protected void sync() {
		super.sync();
		/* EDITED by Abhimanyu Chugh */
		resTimes = data.getResTimes(algorithm);
		classAggs = data.getPerClassSP(algorithm, true);
		classAggsWO = data.getPerClassSP(algorithm, false);
		globalAgg = data.getGlobalSP(algorithm, true);
		globalAggWO = data.getGlobalSP(algorithm, false);
		/* END */
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new SPTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_SYSTEMPOWERS;
	}

	/**
	 * the model backing the visit table.
	 * Rows represent stations, columns classes.
	 */
	private class SPTableModel extends ExactTableModel {

		private static final long serialVersionUID = 1L;

		SPTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		@Override
		public int getRowCount() {
			if (resTimes == null) {
				return 0;
			}
			return 2;
		}

		@Override
		public int getColumnCount() {
			if (resTimes == null) {
				return 0;
			}
			return classes + 1;
		}

		@Override
		protected Object getRowName(int rowIndex) {
			if (rowIndex == 0) {
				return "<html><body><p><b>A</b></p></body></html>";
			} else if (rowIndex == 1) {
				return "<html><body><p><b>B</b></p></body></html>";
			} else {
				return null;
			}
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

			if (rowIndex == 0 && columnIndex == 0 && !data.isMixed()) {
				d = globalAgg[iteration];
			} else if (rowIndex == 0 && columnIndex > 0) {
				d = classAggs[columnIndex - 1][iteration];
			} else if (rowIndex == 1 && columnIndex == 0 && data.isClosed()) {
				d = globalAggWO[iteration];
			} else if (rowIndex == 1 && columnIndex > 0 && data.getClassTypes()[columnIndex - 1] == CLASS_CLOSED) {
				d = classAggsWO[columnIndex - 1][iteration];
			}

			if (d < 0.0) {
				return new String("--");
			} else {
				return new Double(d);
			}
		}

	}

}
