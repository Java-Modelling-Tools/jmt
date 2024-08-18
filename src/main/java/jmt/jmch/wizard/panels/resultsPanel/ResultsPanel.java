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

import jmt.framework.gui.help.HoverHelp;
import jmt.framework.gui.wizard.WizardPanel;
import jmt.jmch.wizard.MainWizard;

/**
 * Panel for showing the results of the simulations.
 * This main class contains only the different ways of adding new results to the tables.
 * The *real* tables are inside the subclasses of this class. 
 *
 * @author Lorenzo Torri
 * Date: 15-jul-2024
 * Time: 10.53
 */
public class ResultsPanel extends WizardPanel{
    private static final String PANEL_NAME = "Results";

    //------------ components of the panel 
    protected MainWizard parent;
    protected HoverHelp help;

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
	 * @param service service time of the system
	 * @param responseTime response time of the system
	 * @param queueTime queue time of the system
	 * @param thoughput thoughput of the system
	 * @param nCustomers customer numbers of the system
	 */
    public void addResult(String algorithm, String arrivalDistr, double lambda, String serviceDistr, double service, double responseTime, double queueTime, double thoughput, double nCustomer){
        
    }

}
