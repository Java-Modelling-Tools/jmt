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

package jmt.jmch;


/**
 * Class that contains most of the descriptions and help labels of JMCH
 * 
 * @author Lorenzo Torri
 * Date: 29-mar-2024
 * Time: 13.40
 */
public class Constants {
    public static final int PANEL_MAIN = 0;
    public static final int PANEL_ANIMATION = 1;
    public static final int PANEL_RESULTS = 2;
    public static final int PANEL_MARKOV = 1;

    //all the algorithms
    public static final String FCFS = "FCFS";
    public static final String LCFS = "LCFS";
    public static final String SJF = "SJF";
    public static final String LJF = "LJF";

    public static final String PS = "PS";

    public static final String RR = "RR";
    public static final String PROBABILISTIC = "PROBABILITIES";
    public static final String JSQ = "JSQ";

    //descriptions of all the policies
    public static final String FCFS_DESCRIPTION = 
        "The FCFS (First come first served) sheduling policy is a scheduling rule that executes queued jobs based on their arrival time.";
    public static final String LCFS_DESCRIPTION = 
        "The LCFS (Last come first served) sheduling policy  is a scheduling rule that executes queued jobs in reverse sequence with respect to their arrival time.";
    public static final String SJF_DESCRIPTION =
        "The SJF (Shortest Job First) scheduling policy is a scheduling rule that executes queued jobs in reverse sequence of their requested service time (the shortest job is the first).";
    public static final String LJF_DESCRIPTION =
        "The LJF (Longest Job First) scheduling policy is a scheduling rule that executes queued jobs based on the sequence of required service time (the longest job is the first).";
    
    public static final String PS_DESCRIPTION = 
        "<html>The Processor Sharing (PS) scheduling policy is a scheduling rule where all jobs are executed simultaneously. A fraction of the total server capacity, inversely proportional to the number of running jobs, is assigned to each of them. If the station has n servers but only m &lt; n customers, then each customer is executed as if it were alone in the system.</html>";

    public static final String RR_DESCRIPTION =
        "The RR (Round Robin) policy is a routing rule where jobs are routed cyclically to outgoing links based on circular routing pattern. The first job is sent to the link of the top station.";
    public static final String PROB_DESCRIPTION =
        "The Probabilities policy is a routing rule where jobs are routed to outgoing links based on assigned probabilities (that must sum to 1).";
    public static final String JSQ_DESCRIPTION =
        "The JSQ (Join the Shortest Job Queue) policy is a routing rule where arriving jobs are routed to the connected station with the least number of customers in the queue.";
    
    static final String HTML_START = "<html><body><p style='text-align:justify;'>";
    static final String HTML_END = "</p></body></html>";
    static final String HTML_FONT_TITLE = "<font size=\"4\"><b>";
    static final String HTML_FONT_TIT_END = "</b></font><br>";
    static final String HTML_FONT_NORM = "<font size=\"3\">";
    static final String HTML_FONT_NOR_END = "</font>";

    //introduction AnimationPanel
    public static final String INTRODUCTION_SIMULATION = HTML_START + HTML_FONT_TITLE + "Simulation" + HTML_FONT_TIT_END
        + HTML_FONT_NORM + "In this panel it is possible to simulate the behavior of a scheduling algorithm of a queue station or a routing algorithm.<br> "
        + "Set the Parameters and then click the <i>Play</i> button. The simulation will start, the obtained metrics will be displayed in the <i>Results</i> panel." + HTML_FONT_NOR_END + HTML_END;

    //introduction ResultsPanel
    public static final String INTRODUCTION_RESULTS = HTML_START + HTML_FONT_TITLE + "Simulation Results" + HTML_FONT_TIT_END
        + HTML_FONT_NORM + "In this panel the results of the simulations are shown according to the sequence of the executions. <br> "
        + "Note that returning to the <i>Main Panel</i> will result in the loss of all data in the table." + HTML_FONT_NOR_END + HTML_END;
    
    //tooltips of the MainPanel
    public static final String[] PREEMPTIVE_TOOLTIPS = {
        "First Come First Served",
        "Last Come First Served",
        "Shortest Job First",
        "Longest Job First",
        "First Come First Served with Priority"
    };

    public static final String[] PROCESSOR_SHARING_TOOLTIPS = {
        "Processor Sharing"
    };

    public static final String[] ROUTING_TOOLTIPS = {
        "Round Robin",
        "Probabilities",
        "Join the Shortest Queue"
    };

    public static final String[] MARKOV_TOOLTIPS = {
        "M/M/1 Station, 1 Server",
        "M/M/1/k Finite Capacity Station, 1 Server",
        "M/M/c Station, c Servers",
        "M/M/c/k Finite Capacity Station, c Servers"
    };
    
    //help text
    public static final String HELP_ANIMATION = "The max number of jobs in the queue is 5, jobs that arrive when this value has been reached are dropped";

    public static final String[] HELP_BUTTONS_ANIMATIONS = {
        "Start the simulation or restart it, if it was paused", 
        "Pause the running simulation",
        "Restart the simulation",  
        "Perform a next step in the simulation",
        "Halve the simulation's velocity",
        "Double the simulation's velocity",
        "Open the help page"
    };
        
    public static final String[] HELP_BUTTONS_MAINPANEL = {
        "Select the algorithm for the Non Preemptive Scheduling Simulation",
        "",
        "Select the algorithm for the Preemptive Scheduling Simulation",
        "",
        "Opens a new panel with a Round Robin simulation",
        "",
        "Opens a new panel with a the Probabilities Routing simulation",
        "",
        "Select the algorithm for a Routing simulation",       
        "", 
        "Select the type of Queue Markov Chain",
    };
    
    public static final String[] HELP_PARAMETERS_PANELS = {
        "Choose the type of Policy for the simulation",
        "Select how many servers are available for each station",
        "Select the first two probabilities for the router's edges. The third is automatically computed as 1 - p1 - p2. The play button is blocked if p1 + p2 > 1",
        "Select the type of distribution for the inter arrival time between jobs",
        "Select the type of distribution for the service time for each station in the simulation",
        "Select the arrival rate and service time of the simulation such that the utilization of the system is always "+ "\u2264" + " 1",
        "Select the maximum number of samples collected for each performance index of the correspondent JSIM model",
    };

    public static final String[] TOOLTIPS_PARAMETERS_PANEL = {
        "Policy of the simulation",
        "Number of servers",
        "Probabilities of router's edges",
        "Inter Arrival Time distribution",
        "Service Time distribution",
        "Arrival Rate",
        "Service Time",
        "Arrival Rate x Service Time",
        "Maximum number of samples per JSIM metric",
    };

    public static final String[] HELP_BUTTONS_MARKOV = {
        "Start the simulation or restart it, if it was paused", 
        "Pause the running simulation",
        "Stop the running simulation",
        "Open the help page"
    };

    public static final String[] PROMPT_SIMULATION_FINISHED = {
        "Simulation results saved in the Table. Do you want to save the results?",
        "Simulation Results"
    };
}
