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

package jmt.gui.common.definitions;

import java.util.Vector;

import jmt.gui.common.definitions.parametric.ParametricAnalysisDefinition;

/**
 * Created by IntelliJ IDEA.
 * User: orsotronIII
 * Date: 22-lug-2005
 * Time: 17.33.22
 * This interface provides methods for simulation parameters definition.
 * Those include measures and other parameters for simulation setup, such as
 * simulation seed or simulation maximum duration.
 * Modified by Bertoli Marco 3-oct-2005
 *
 * Modified by Francesco D'Aquino
 * 
 * @author Bertoli Marco (new measures)
 *       
 * Modified by Ashanka (May 2010): 
 * Project: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 * 
 * Modified by Sturari Nicola (Nov 2014)
 * Desc: Added a parameter representing the maximum simulated time (not real time) before the simulation is stopped
 *  
 */
public interface SimulationDefinition {

	/**Code for queue length measure*/
	public static final String MEASURE_QL = "Number of Customers";
	/**Code for queue time measure*/
	public static final String MEASURE_QT = "Queue Time";
	/**Code for response time measure*/
	public static final String MEASURE_RP = "Response Time";
	/**Code for residence time measure*/
	public static final String MEASURE_RD = "Residence Time";
	/**Code for utilization measure*/
	public static final String MEASURE_U = "Utilization";
	/**Code for throughput measure*/
	public static final String MEASURE_X = "Throughput";
	/**Code for drop rate measure*/
	public static final String MEASURE_DR = "Drop Rate";
	/**Code for reneging rate measure*/
	public static final String MEASURE_RR = "Reneging Rate";
	/**Code for balking rate measure*/
	public static final String MEASURE_BR = "Balking Rate";
	/**Code for retrial attempt rate number measure*/
	public static final String MEASURE_R = "Retrial Rate";
	/**Code for orbit waiting time measure*/
	public static final String MEASURE_OT = "Retrial Orbit Residence Time";
	/**Code for retrial orbit size measure*/
	public static final String MEASURE_RS = "Retrial Orbit Size";
	/**Code for number of busy servers measure */
	//public static final String MEASURE_BS = "Number of Busy Servers";


	/**Code for number of system retrial number measure*/
	public static final String MEASURE_S_R = "System Retrial Rate";
	/**Code for system reneging rate measure*/
	public static final String MEASURE_S_RR = "System Reneging Rate";
	/**Code for system reneging rate measure*/
	public static final String MEASURE_S_BR = "System Balking Rate";
	/**Code for power measure*/
	public static final String MEASURE_P = "Power";
	/**Code for system customer number measure*/
	public static final String MEASURE_S_CN = "System Number of Customers";
	/**Code for system response time measure*/
	public static final String MEASURE_S_RP = "System Response Time";
	/**Code for system throughput measure*/
	public static final String MEASURE_S_X = "System Throughput";
	/**Code for system drop rate measure*/
	public static final String MEASURE_S_DR = "System Drop Rate";
	/**Code for system power measure*/
	public static final String MEASURE_S_P = "System Power";
	/**Code for response time per sink measure*/
	public static final String MEASURE_RP_PER_SINK = "Response Time per Sink";
	/**Code for throughput per sink measure*/
	public static final String MEASURE_X_PER_SINK = "Throughput per Sink";
	/**Code for FCR total weight measure*/
	public static final String MEASURE_FCR_TW = "FCR Total Weight";
	/**Code for FCR memory occupation measure*/
	public static final String MEASURE_FCR_MO = "FCR Memory Occupation";
	/**Code for FJ customer number measure*/
	public static final String MEASURE_FJ_CN = "Fork Join Number of Customers";
	/**Code for FJ response time measure*/
	public static final String MEASURE_FJ_RP = "Fork Join Response Time";
	/**Code for firing throughput measure*/
	public static final String MEASURE_FX = "Firing Throughput";

	/*------------------------------------------------------------------------------------------
	------------------------------Parameters for measures definition----------------------------
	--------------------------------------------------------------------------------------------*/

	/**
	 * Adds a new measure for specified class and station (or region)
	 * @param type: code for measure type.
	 * @param stationKey: specified station (or region) for measure
	 * @param classKey: specified class
	 * @return search key for new measure
	 */
	public Object addMeasure(String type, Object stationKey, Object classKey);

	/**
	 * Adds a new measure for specified class and station (or region)
	 * @param type: code for measure type.
	 * @param stationKey: specified station for measure
	 * @param classKey: specified class
	 * @param alpha: alpha parameter for this measure
	 * @param precision: precision parameter for this measure
	 * @param log: true to log the measure to CSV. False to avoid it.
	 * @return search key for new measure
	 */
	public Object addMeasure(String type, Object stationKey, Object classKey, Double alpha, Double precision, boolean log);

	/**
	 * Removes a measure from list, given its search key
	 * @param measureKey: search Key for measure to be removed
	 */
	public void removeMeasure(Object measureKey);

	/**
	 * Returns list of measure search keys
	 * @return Vector containing all of the measure search keys
	 */
	public Vector<Object> getMeasureKeys();

	/**
	 * Returns type of measure, given measure search key
	 * @param measureKey: search key
	 * @return measure type
	 */
	public String getMeasureType(Object measureKey);	

	/**
	 * Sets type of parameter to be measured
	 * @param newType new type for this measure
	 * @param measureKey search key for this measure
	 */
	public void setMeasureType(String newType, Object measureKey);

	/**
	 * Returns search key for station a certain measure refers to.
	 * @param measureKey search key for measure
	 * @return search key for station.
	 */
	public Object getMeasureStation(Object measureKey);

	/**
	 * Changes reference station for specified measure, given its search key
	 * @param stationKey: search key for station this measure must be referred to.
	 * @param measureKey: search key for measure.
	 */
	public void setMeasureStation(Object stationKey, Object measureKey);

	/**
	 * Returns search key for class this measure refers to.
	 * @param measureKey search key for measure
	 * @return search key for class
	 */
	public Object getMeasureClass(Object measureKey);

	/**
	 * Changes reference class for specified measure, given its search key
	 * @param classKey new class search key
	 * @param measureKey measure search key
	 */
	public void setMeasureClass(Object classKey, Object measureKey);

	/**
	 * Returns value for alpha parameter of a specific measure, given its search key.
	 * @param measureKey search key for station
	 * @return value of alpha parameter
	 */
	public Double getMeasureAlpha(Object measureKey);

	/**
	 * Sets value for alpha parameter of a specific measure, given its search key.
	 * @param measureKey search key for station
	 * @param alpha value of alpha parameter
	 */
	public void setMeasureAlpha(Double alpha, Object measureKey);

	/**
	 * Returns value for precision parameter of a specific measure, given its search key.
	 * @param measureKey search key for station
	 * @return value of precision parameter
	 */
	public Double getMeasurePrecision(Object measureKey);

	/**
	 * Sets value for alpha parameter of a specific measure, given its search key.
	 * @param measureKey search key for station
	 * @param precision value of alpha parameter
	 */
	public void setMeasurePrecision(Double precision, Object measureKey);

	/**
	 * Tells if a measure should be logged
	 * @param measureKey the key of the measure
	 * @return true if measure should be logged, false otherwise.
	 */
	public boolean getMeasureLog(Object measureKey);

	/**
	 * Sets if measure should be logged
	 * @param log true to log measure, false to avoid (default)
	 * @param measureKey the key of the measure
	 */
	public void setMeasureLog(boolean log, Object measureKey);

	/**
	 * Tells if a given measure is global
	 * @param type type of given measure
	 * @return true if measure is global
	 */
	public boolean isGlobalMeasure(String type);

	/**
	 * Tells if a given measure is for sinks only
	 * @param type type of given measure
	 * @return true if measure is for sinks only
	 */
	public boolean isSinkMeasure(String type);

	/**
	 * Tells if a given measure is for blocking regions only
	 * @param type type of given measure
	 * @return true if measure is for blocking regions only
	 */
	public boolean isFCRMeasure(String type);

	/**
	 * Tells if a given measure is for fork/join sections only
	 * @param type type of given measure
	 * @return true if measure is for fork/join sections only
	 */
	public boolean isFJMeasure(String type);

	/*------------------------------------------------------------------------------------------
	------------------------------ Methods for preloading definition ---------------------------
	--------------------------------------------------------------------------------------------*/

	/**
	 * Returns number of preloaded jobs at specified station for specified class.
	 * @param stationKey station at which preloaded jobs are preloaded.
	 * @param classKey class to which preloaded jobs belong.
	 * @return number of preloaded jobs.
	 */
	public Integer getPreloadedJobs(Object stationKey, Object classKey);

	/**
	 * Sets number of preloaded jobs at specified station for specified class.
	 * For closed classes, total number of preloaded jobs for a class over all
	 * stations must be equal to class population, otherwise update is canceled.
	 * @param stationKey station at which preloaded jobs are preloaded.
	 * @param classKey class to which preloaded jobs belong.
	 * @param preload number of preloaded jobs.
	 */
	public void setPreloadedJobs(Object stationKey, Object classKey, Integer preload);

	/**
	 * Returns total number of preloaded jobs for specified class.
	 * @param classKey class to which preloaded jobs belong.
	 * @return total number of preloaded jobs.
	 */
	public Integer getPreloadedJobsNumber(Object classKey);

	/**
	 * This method is used to manage number of jobs for every class. If class is closed
	 * all spare jobs will be allocated to its reference source, if for some reasons more
	 * jobs are allocated than max population, they are reduced. Uses this method only
	 * when strictly necessary as it can be slow if the model is big.
	 */
	public void manageJobs();

	/*------------------------------------------------------------------------------------------
	---------------------------- Parameters for simulation definition --------------------------
	--------------------------------------------------------------------------------------------*/
	/**
	 * Gets seed for simulation.
	 * @return seed for simulation.
	 */
	public Long getSimulationSeed();

	/**
	 * Sets seed for simulation.
	 * @param seed seed for simulation.
	 */
	public void setSimulationSeed(Long seed);

	/**
	 * Tells if random seed is used.
	 * @return true if random seed is used, false otherwise.
	 */
	public Boolean getUseRandomSeed();

	/**
	 * Sets if random seed is used.
	 * @param useRandomSeed true if random seed is used, false otherwise.
	 */
	public void setUseRandomSeed(Boolean useRandomSeed);

	/**
	 * Returns maximum duration for simulation in seconds.
	 * @return seconds of maximum duration for simulation.
	 */
	public Double getMaximumDuration();

	/**
	 * Sets maximum duration for simulation in seconds.
	 * @param durationSeconds seconds of maximum duration for simulation.
	 */
	public void setMaximumDuration(Double durationSeconds);

	/**
	 * Returns maximum duration for simulation in time units.
	 * @return time units of maximum duration for simulation.
	 */
	public Double getMaxSimulatedTime();

	/**
	 * Sets maximum duration for simulation in time units.
	 * @param duration time units of maximum duration for simulation.
	 */
	public void setMaxSimulatedTime(Double duration);

	/**
	 * Returns maximum number of simulation samples.
	 * @return maximum number of simulation samples.
	 */
	public Integer getMaxSimulationSamples();

	/**
	 * Sets maximum number of simulation samples.
	 * @param maxSamples maximum number of simulation samples.
	 */
	public void setMaxSimulationSamples(Integer maxSamples);

	/**
	 * Tells if statistic check is disabled.
	 * @return true if statistic check is disabled, false otherwise.
	 */
	public Boolean getDisableStatistic();

	/**
	 * Sets if statistic check is disabled.
	 * @param disableStatistic true if statistic check is disabled, false otherwise.
	 */
	public void setDisableStatistic(Boolean disableStatistic);

	/**
	 * Returns maximum number of simulation events.
	 * @return maximum number of simulation events.
	 */
	public Integer getMaxSimulationEvents();

	/**
	 * Sets maximum number of simulation events.
	 * @param maxEvents maximum number of simulation events.
	 */
	public void setMaxSimulationEvents(Integer maxEvents);

	/**
	 * Returns polling interval for temporary measures.
	 * @return polling interval for temporary measures.
	 */
	public Double getPollingInterval();

	/**
	 * Sets polling interval for temporary measures.
	 * @param pollingInterval polling interval for temporary measures.
	 */
	public void setPollingInterval(Double pollingInterval);
	
	/**
	 * Returns the number of simulations to be run in parallel during what if analysis.
	 * @return the number of simulations to be run in parallel during what if analysis.
	 */
	public Integer getWhatIfParallelism();

	// --- Methods to manage simulation results -- Bertoli Marco --------------------------------------------
	/**
	 * Returns last simulation results
	 * @return simulation results or null if no simulation was performed
	 */
	public MeasureDefinition getSimulationResults();

	/**
	 * Sets simulation results
	 * @param results simulation results data structure
	 */
	public void setSimulationResults(MeasureDefinition results);

	/**
	 * Tells if current model contains simulation results
	 * @return true if <code>getSimulationResults()</code> returns a non-null object
	 */
	public boolean containsSimulationResults();
	// ------------------------------------------------------------------------------------------------------

	/**
	 * Tells if queue animation is enabled
	 * @return true if the animation is enabled
	 */
	public boolean isAnimationEnabled();

	/**
	 * Enable / disable queue animation
	 * @param enabled - set it to true to enable queue animation
	 */
	public void setAnimationEnabled(boolean isEnabled);

	/**
	 * Tells if parametric analysis is enabled
	 * @return true if parametric analysis is enabled
	 */
	public boolean isParametricAnalysisEnabled();

	/**
	 * Enable / disable parametric analysis
	 * @param enabled - set it to true to enable parametric analysis
	 */
	public void setParametricAnalysisEnabled(boolean enabled);

	/**
	 * Returns the ParametricAnalysisModel
	 * @return the parametricAnalysisModel
	 */
	public ParametricAnalysisDefinition getParametricAnalysisModel();

	/**
	 * Sets the ParametricAnalysisModel
	 * @param pad the parametricAnalysisModel to be set
	 */
	public void setParametricAnalysisModel(ParametricAnalysisDefinition pad);

	/**
	 * Tells model that some data has been changed and need to be saved. This
	 * is used by Parametric Analysis
	 */
	public void setSaveChanged();

}
