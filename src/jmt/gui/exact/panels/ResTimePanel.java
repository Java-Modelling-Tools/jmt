/**    
  * Copyright (C) 2006, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.

  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */

package jmt.gui.exact.panels;

import java.io.Console;
import java.sql.Ref;

import jmt.analytical.SolverAlgorithm;
import jmt.framework.gui.help.HoverHelp;
import jmt.gui.exact.ExactConstants;
import jmt.gui.exact.ExactModel;
import jmt.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;

/**

 * @author alyf (Andrea Conti)
 * Date: 11-set-2003
 * Time: 23.48.19

 */

/**
 * 7th panel: residence times
 */
public final class ResTimePanel extends SolutionPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double[][][] resTimes;
	//NEW Dall'Orso
	private double[][] classAggs, stationAggs;
	private double[] globalAgg;
	
	private double[][][] throughput;
	private double[][] classAggr,stationAggr;

	//END

	/* EDITED by Abhimanyu Chugh */
	public ResTimePanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		helpText = "<html>Residence times</html>";
		name = "Residence Times";
	}
	/* END */

	/**
	 * gets status from data object
	 */
	@Override
	protected void sync() {
		super.sync();
		/* EDITED by Abhimanyu Chugh */
		resTimes = data.getResTimes(algorithm);
		//NEW Dall'Orso
		classAggs = data.getPerClassR(algorithm);
		stationAggs = data.getPerStationR(algorithm);
		globalAgg = data.getGlobalR(algorithm);
		
		throughput = data.getThroughput(algorithm);
		classAggr = data.getPerClassX(algorithm);
		stationAggr = data.getPerStationX(algorithm);
		//END
		/* END */
	}

	@Override
	protected ExactTableModel getTableModel() {
		return new RTTableModel();
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_RESIDENCETIME;
	}

	/**
	 * the model backing the visit table.
	 * Rows represent stations, columns classes.
	 */
	private class RTTableModel extends ExactTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		RTTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (resTimes == null) {
				return 0;
			}
			//OLD
			/*
			if (stations == 1) return 1;
			return stations;
			*/
			//NEW
			//@author Dall'0rso
			//return stations + 1;
			//end NEW
			
			//NEW
			return stations;
			//End New

		}

		public int getColumnCount() {
			if (resTimes == null) {
				return 0;
			}
			//OLD
			/*
			if (isSingle) return 1;
			return classes;
			*/
			//NEW
			//@author Dall'Orso
			return classes + 1;
			//end NEW
		}

		@Override
		protected Object getRowName(int rowIndex) {
			if (rowIndex == 0) {
				//return "<html><i>Aggregate</i></html>";
				//return null;
			}
			//return stationNames[rowIndex - 1];
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
		/*
			double d;
			//NEW Dall'Orso
			if (rowIndex > 0 && columnIndex > 0) {
				d = resTimes[rowIndex - 1][columnIndex - 1][iteration];
			} else if (rowIndex == 0 && columnIndex > 0) {
				d = classAggs[columnIndex - 1][iteration];
			} else if (rowIndex > 0 && columnIndex == 0) {
				d = stationAggs[rowIndex - 1][iteration];
			} else {
				d = globalAgg[iteration];
			}
			//END
			if (d < 0) {
				return null; //causes the renderer to display a gray cell
			}
			return new Double(d);
			*/
			
			double d = 0;
			//NEW Dall'Orso
			if (rowIndex >= 0 && columnIndex > 0) {
				if(ReferenceStation[columnIndex-1] < stations)
					d = resTimes[rowIndex][columnIndex - 1][iteration] / visits[ReferenceStation[columnIndex-1]][columnIndex-1]; //it is dividing the value of the each row with the visits
				else
					d = resTimes[rowIndex][columnIndex - 1][iteration];
			} 
			  else if (rowIndex >= 0 && columnIndex == 0) { //everything is here
				double c = 0.0;
				  for (int i=0; i< classes; i++)
				{
					  if(ReferenceStation[i] < stations)
						  c += (throughput[rowIndex][i][iteration] / stationAggr[rowIndex][iteration]) * (resTimes[rowIndex][i][iteration] / visits[ReferenceStation[i]][i]) ;
					  else
						  c += (throughput[rowIndex][i][iteration] / stationAggr[rowIndex][iteration]) * (resTimes[rowIndex][i][iteration]) ;
				  
				}
				d=c;
			  }
			  else {
				d = globalAgg[iteration];
			}
			//END
			if (d < 0) {
				return null; //causes the renderer to display a gray cell
			}
			return new Double(d);
		}
		

	}
	
}
