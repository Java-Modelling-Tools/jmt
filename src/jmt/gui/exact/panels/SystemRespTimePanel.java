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
import jmt.analytical.SolverAlgorithm;
import jmt.framework.gui.help.HoverHelp;
import jmt.gui.exact.ExactConstants;
import jmt.gui.exact.ExactModel;
import jmt.gui.exact.ExactWizard;
import jmt.gui.exact.table.ExactTableModel;
import javax.swing.JTable;

/**
 * @author Kourosh Sheykhvand
 * Date: Nov 2013
 * in this panel we are going to show the new performance indexes which is SystemResponseTime the calculation has not finished yet. 
 *  but the value are the same as the residence time panel but I just need to divide some of them. the formula has not be calculated but again
 *  we do not need to touch the algorithm we can have the information of the residence time and then use them here.
 */

public final class SystemRespTimePanel extends SolutionPanel {

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	private double[][][] resTimes;
	private double[][] classAggs, stationAggs;
	private double[] globalAgg;
	private double[][][] throughput;
	private double[][] stationAggr,classAggr;

	public SystemRespTimePanel(ExactWizard ew, SolverAlgorithm alg) {
		super(ew, alg);
		
		helpText = "<html>System Response Time</html>";
		name = "System Response Time";
	}
	/* END */
	/**
	 * gets status from data object
	 */
	@Override
	protected void sync() {
		super.sync();
		
		resTimes = data.getResTimes(algorithm);
		classAggs = data.getPerClassR(algorithm);
		stationAggs = data.getPerStationR(algorithm);
		globalAgg = data.getGlobalR(algorithm);
		
		throughput = data.getThroughput(algorithm);		
		stationAggr = data.getPerStationX(algorithm);
		classAggr = data.getPerClassX(algorithm);

	}
	@Override
	protected ExactTableModel getTableModel() {
		
		
		return new RTTableModel();
		
		
	}

	@Override
	protected String getDescriptionMessage() {
		return ExactConstants.DESCRIPTION_RESPONSETIMES;
	}

	private class RTTableModel extends ExactTableModel {
		
		/**
		 * 
		 */		
		
		private static final long serialVersionUID = 1L;
		
		public RTTableModel() {
			
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
				return "<html><body><p><font size=\"0.5\">Inc R.St</font></p></body></html>";
			}
			else if (rowIndex == 1)
				return "<html><body><p><font size=\"0.5\">NotInc R.St</font></p></body></html>";
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
			double d=0.0;
			
			if (rowIndex == 0 && columnIndex > 0)
				for (int i=0; i< stations; i++)
					if(ReferenceStation[columnIndex-1] < stations)
						d += resTimes[i][columnIndex - 1][iteration] / visits[ReferenceStation[columnIndex-1]][columnIndex-1];
					else
						d += resTimes[i][columnIndex - 1][iteration];
			else if (rowIndex == 1 && columnIndex > 0)
			{
				for (int i=0; i< stations; i++)
					if(ReferenceStation[columnIndex-1] < stations)
						d += (resTimes[i][columnIndex - 1][iteration] / visits[ReferenceStation[columnIndex-1]][columnIndex-1]);
					else
						d += resTimes[i][columnIndex - 1][iteration];
					if(ReferenceStation[columnIndex-1] < stations)
						d -= serviceTimes[ReferenceStation[columnIndex-1]][columnIndex-1][0];
			}
			else if (rowIndex == 0 && columnIndex == 0)
			{	double c = 0.0; //in C we have the value of the SystemThrougput the code is the same as the one in the Throughput panel
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
				}
			else if (rowIndex == 1 && columnIndex == 0)
			{
				double c = 0.0; //in C we have the value of the SystemThrougput the code is the same as the one in the Throughput panel
				for (int j=0;j<classes;j++)
					for (int i=0; i<stations; i++)						
						if (ReferenceStation[j] != stations)
						{
							c += throughput[ReferenceStation[j]][j][0];
							break;
						}						
						else if (ReferenceStation[j] == stations) //it means "No Reference Station"
							{c += classAggr[j][iteration];
								break;
							}
					//now the values of the throughput of each class (throughput[i][j][0]/c it means the throughput of each class over the system throughoput and the sum is always 1)
					for(int j = 0; j< classes; j++){
						double x=0.0;					
						for (int i=0; i<stations; i++)			
							if (ReferenceStation[j] != stations)
								x += resTimes[i][j][iteration] / visits[ReferenceStation[j]][j];													
							else if (ReferenceStation[j] == stations) //it means "No Reference Station"
								x += resTimes[i][j][iteration];
							if (ReferenceStation[j] != stations){
								x -= serviceTimes[ReferenceStation[j]][j][0];
								d += x*throughput[ReferenceStation[j]][j][0]/c;
							}
							else
								d += x*classAggr[j][iteration]/c;
						}
				
			}
			if (d < 0) {
				return null; //causes the renderer to display a gray cell
			}
			return new Double(d);
		}

	}
}
