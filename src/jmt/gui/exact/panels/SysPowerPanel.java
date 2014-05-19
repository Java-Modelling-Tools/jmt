/**    
  * Copyright (C) 2009, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

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

import jmt.analytical.SolverAlgorithm;
import jmt.framework.gui.help.HoverHelp;
import jmt.gui.exact.ExactConstants;
import jmt.gui.exact.ExactModel;
import jmt.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;

public class SysPowerPanel extends SolutionPanel {

	private static final long serialVersionUID = 1L;

	//OLD
	//private double[][] classAggs;
	//private double[] globalAgg;
	
	//New
	private double[][][] resTimes;
	private double[][] classAggs, stationAggs;
	private double[] globalAgg;
	private double[][][] throughput;
	private double[][] stationAggr,classAggr;

	/* EDITED by Abhimanyu Chugh */
	public SysPowerPanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		// TODO Auto-generated constructor stub
		helpText = "<html>System Power</html>";
		name = "System Power";
	}
	/* END */

	@Override
	protected void sync() {
		super.sync();
		/* EDITED by Abhimanyu Chugh */ //OLD
		//classAggs = data.getPerClassSP(algorithm);
		//globalAgg = data.getGlobalSP(algorithm);
		/* END */
		
		//New
		resTimes = data.getResTimes(algorithm);
		classAggs = data.getPerClassR(algorithm);
		stationAggs = data.getPerStationR(algorithm);
		globalAgg = data.getGlobalR(algorithm);
		
		throughput = data.getThroughput(algorithm);		
		stationAggr = data.getPerStationX(algorithm);
		classAggr = data.getPerClassX(algorithm);
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_SYSPOWER;
	}

	@Override
	protected ExactTableModel getTableModel() {
		// TODO Auto-generated method stub
		return new SPTableModel();
	}

	private class SPTableModel extends ExactTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		SPTableModel() {
			prototype = new Double(1000);
			rowHeaderPrototype = "Station1000";
		}

		public int getRowCount() {
			if (resTimes == null) {
				return 0;
			}
			return 2;

		}

		public int getColumnCount() {
			if (resTimes == null) {
				return 0;
			}
			return classes + 1;
		}

		@Override
		protected Object getRowName(int rowIndex) {
			if (rowIndex == 0) {
				//return "<html><body><p><font size=\"0.5\">Inc R.St</font></p></body></html>";
				return "<html><body><p><b>A</b></p></body></html>";
			}
			else if (rowIndex == 1)
				//return "<html><body><p><font size=\"0.5\">NotInc R.St</font></p></body></html>";
				return "<html><body><p><b>B</b></p></body></html>";
			else
				return null;
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
			double d = 99999;//initialized with dummy
			Object ret;
			//NEW Dall'Orso
			if (rowIndex > 0 && columnIndex > 0) {
				ret = new String("-");
			} else if (rowIndex == 0 && columnIndex > 0) {
				d = classAggs[columnIndex - 1][iteration];
				ret = new Double(d);
			} else if (rowIndex > 0 && columnIndex == 0) {
				ret = new String("-");
			} else {
				d = globalAgg[iteration];
				ret = new Double(d);
			}
			//END
			if (d < 0) {
				return null; //causes the renderer to display a gray cell
			}

			return ret;
			*/
			
			//in this panel everything is the same as SystemResponseTime but with a small change we need to divide throughput over response time.
				
			double d=0.0;
				double c = 0.0; //in C we have the value of the SystemThrougput the code is the same as the one in the Throughput panel
					for (int j=0;j<classes;j++)
						for (int i=0; i<stations; i++)						
							if (ReferenceStation[j] != stations)
							{
								c += throughput[ReferenceStation[j]][j][0];
								break;
							}						
							else if (ReferenceStation[j] == stations) //it means "Arrival Process Station"
								{c += classAggr[j][iteration];
									break;
								}
				
			if (rowIndex == 0 && columnIndex > 0){
				
				int i=0;
				for (i=0; i<stations; i++)			
					if (ReferenceStation[columnIndex - 1] == i)
					{
						c = throughput[rowIndex+i][columnIndex - 1][0];
					}							
					else if (ReferenceStation[columnIndex - 1] == stations) //it means "Arrival Process Station"
						c = classAggr[columnIndex - 1][iteration];
				
				for (i=0; i< stations; i++)
					if(ReferenceStation[columnIndex-1] < stations)
						d += resTimes[i][columnIndex - 1][iteration] / visits[ReferenceStation[columnIndex-1]][columnIndex-1];
					else
						d += resTimes[i][columnIndex - 1][iteration];
				d = c/d;}
			else if (rowIndex == 1 && columnIndex > 0)
			{
				int i=0;
				for (i=0; i<stations; i++)			
					if (ReferenceStation[columnIndex - 1] == i)
					{
						c = throughput[rowIndex-1+i][columnIndex - 1][0];
					}							
					else if (ReferenceStation[columnIndex - 1] == stations) //it means "Arrival Process Station"
						c = classAggr[columnIndex - 1][iteration];
				
				for (i=0; i< stations; i++)
					if(ReferenceStation[columnIndex-1] < stations)
						d += (resTimes[i][columnIndex - 1][iteration] / visits[ReferenceStation[columnIndex-1]][columnIndex-1]);
					else
						d += resTimes[i][columnIndex - 1][iteration];
					if(ReferenceStation[columnIndex-1] < stations)
						d -= serviceTimes[ReferenceStation[columnIndex-1]][columnIndex-1][0];
				d = c/d;
				
				// For the open classes there is no value for B
				
				if (classTypes[columnIndex-1] == 1)
					d = 0.0;
			}
			else if (rowIndex == 0 && columnIndex == 0)
			{
				//now the values of the throughput of each class (throughput[i][j][0]/c it means the throughput of each class over the system throughput and the sum is always 1)
				for(int j = 0; j< classes; j++){
					double x=0.0;					
					for (int i=0; i<stations; i++)			
						if (ReferenceStation[j] != stations)
							x += resTimes[i][j][iteration] / visits[ReferenceStation[j]][j];													
						else if (ReferenceStation[j] == stations) //it means "Arrival Process Station"
							x += resTimes[i][j][iteration];
						if (ReferenceStation[j] != stations)
						d += x*throughput[ReferenceStation[j]][j][0]/c;
						else
						d += x*classAggr[j][iteration]/c;				
					}						
				d = c/d; //To compute the SystemPower 
				}
			else if (rowIndex == 1 && columnIndex == 0)
			{				
					//now the values of the throughput of each class (throughput[i][j][0]/c it means the throughput of each class over the system throughput and the sum is always 1)
					for(int j = 0; j< classes; j++){
						double x=0.0;					
						for (int i=0; i<stations; i++)			
							if (ReferenceStation[j] != stations)
								x += resTimes[i][j][iteration] / visits[ReferenceStation[j]][j];													
							else if (ReferenceStation[j] == stations) //it means "Arrival Process Station"
								x += resTimes[i][j][iteration];
							if (ReferenceStation[j] != stations){
								x -= serviceTimes[ReferenceStation[j]][j][0];
								d += x*throughput[ReferenceStation[j]][j][0]/c;
							}
							else
								d += x*classAggr[j][iteration]/c;
						}
					d = c/d; //To compute the SystemPower
					// For the open classes there is no value for B
					for (int i = 0; i <classes; i++)
					if (classTypes[i] == 1)
						d = 0.0;
			}
			if (d < 0) {
				return null; //causes the renderer to display a gray cell
			}
			if (d == 0.0)
			return new String("--");
			else
			return new Double(d);
		}

		}

	}


