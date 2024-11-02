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
package jmt.jmch.wizard.panels.resultsPanel;


import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jmt.framework.gui.help.HoverHelp;
import jmt.gui.common.JMTImageLoader;
import jmt.jmch.Constants;
import jmt.jmch.simulation.SimulationType;
import jmt.jmch.wizard.MainWizard;
import jmt.jmch.wizard.panels.JMCHWizardPanel;

/**
 * Panel for showing the results of the simulations.
 * This main class contains only the different ways of adding new results to the tables.
 * The *real* tables are inside the subclasses of this class. 
 *
 * @author Lorenzo Torri
 * Date: 15-jul-2024
 * Time: 10.53
 */
public class ResultsPanel extends JMCHWizardPanel{
    private static final String PANEL_NAME = "Results";

    //------------ components of the panel 
    protected HoverHelp help;
	protected ImageIcon statusResults;
	protected JLabel statusResultsLabel;

	//for each column, an array of values
    protected String[] algorithms = new String[0];
    protected String[] arrivalDistibutions = new String[0];
    protected double[] lambdas = new double[0];
    protected String[] serviceDistributions = new String[0];
	protected int[] serversNumber = new int[0];
    protected int[] queuesNumber = new int[0];
    protected double[] services = new double[0];
    protected double[] responseTimes = new double[0];
    protected double[] queueTimes = new double[0];
    protected double[] thoughputs = new double[0];
    protected double[] nCustomers = new double[0];

    public ResultsPanel(MainWizard main){
        this.parent = main;
        help = parent.getHoverHelp();
    }

    @Override
    public String getName() {
        return PANEL_NAME;
    }

    /**
	 * Method called by the MainWizard to update the Result Panel for SCHEDULING
	 * @param algorithm queue algorithm of the animation
	 * @param arrivalDistr arrival time distribution
	 * @param lambda the inter arrival time
	 * @param serviceDistr service time distribution
     * @param nServers number of servers in the station
	 * @param service service time
	 * @param responseTime response time
	 * @param queueTime queue time
	 * @param thoughput thoughput
	 * @param nCustomers customer numbers
	 */
    public void addResult(String algorithm, String arrivalDistr, double lambda, String serviceDistr, int nServers, double service, double responseTime, double queueTime, double thoughput, double nCustomer){
        
    }

    /**
	 * Method called by the MainWizard to update the Result Panel for ROUTING
	 * @param algorithm routing algorithm of the animation
	 * @param arrivalDistr arrival time distribution
	 * @param lambda the inter arrival time
	 * @param serviceDistr service time distribution
	 * @param nQueues number of queues reachable
	 * @param responseTime response time of the system
	 * @param thoughput thoughput of the system
	 * @param nCustomers customer numbers of the system
	 */
    public void addResult(String algorithm, String arrivalDistr, double lambda, String serviceDistr, int nQueues, double responseTime, double thoughput, double nCustomer){
        
    }

	/* Methods called by the Main Wizard to update the icon */
	public void loadingResult(){
		statusResults = JMTImageLoader.loadImage("loader");
		statusResultsLabel.setIcon(statusResults);
	}

	public void endResult(){
		statusResults = JMTImageLoader.loadImage("Measure_ok");
		statusResultsLabel.setIcon(statusResults);
	}

	/** Sets the results of previous analysis */
	public void setResults(ResultStructure rs){
		if(rs.getPreviousSimulationType() == SimulationType.ROUTING){
			for(int i = 0; i < rs.getAlgorithms().length; i++){
				addResult(
					rs.getAlgorithms()[i], 
					rs.getArrivalDistibutions()[i], 
					rs.getLambdas()[i], 
					rs.getServiceDistributions()[i], 
					rs.getQueuesNumber()[i], 
					rs.getResponseTimes()[i], 
					rs.getThoughputs()[i], 
					rs.getnCustomers()[i]);
			}
		}
		else{
			for(int i = 0; i < rs.getAlgorithms().length; i++){
				addResult(
					rs.getAlgorithms()[i], 
					rs.getArrivalDistibutions()[i], 
					rs.getLambdas()[i], 
					rs.getServiceDistributions()[i], 
					rs.getNservers()[i],
					rs.getServices()[i], 
					rs.getResponseTimes()[i], 
					rs.getQueueTimes()[i],
					rs.getThoughputs()[i], 
					rs.getnCustomers()[i]);
			}
		}
	}

	@Override
    public boolean canGoBack() {
		//parent.setSelectedPanel();
		return true;	
	}

	@Override
    public void setLastPanel(){
        parent.setLastPanel(Constants.PANEL_RESULTS);
    }

	@Override
    public void lostFocus() { 
        setLastPanel();
    }


	//methods for saving the results
	public String[] getAlgorithms(){
		return algorithms;
	}

	public String[] getArrivalDistirbutions(){
		return arrivalDistibutions;
	}

	public double[] getLambdas(){
		return lambdas;
	}

	public String[] getServiceDistributions(){
		return serviceDistributions;
	}

	public int[] getNservers(){
		return serversNumber;
	}

	public int[] getNQueues(){
		return queuesNumber;
	}

	public double[] getServiceTimes(){
		return services;
	}

	public double[] getResponseTimes(){
		return responseTimes;
	}

	public double[] getThroughputs(){
		return thoughputs;
	}

	public double[] getQueueTimes(){
		return queueTimes;
	}

	public double[] getNumberOfCustomers(){
		return nCustomers;
	}
}
