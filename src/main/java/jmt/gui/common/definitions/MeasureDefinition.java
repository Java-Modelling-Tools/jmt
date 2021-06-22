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

import java.util.List;
import java.util.Vector;

import jmt.framework.gui.graph.MeasureValue;

/**
 * <p>Title: Measure Definition Interface</p>
 * <p>Description: This interface is implemented by each measure definition data structure. It is
 * provided to allow ResultsWindow not to be directly linked to underlying data structure, allowing
 * two or more different kind of result data structure. Actually it can be useful to have one
 * measure data structure that reads data from the engine and one that loads a saved file.</p>
 * 
 * @author Bertoli Marco
 *         Date: 23-set-2005
 *         Time: 23.14.55
 *         
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public interface MeasureDefinition {

	/**
	 * Constants used for the getMeasureState method
	 */
	public static final int MEASURE_IN_PROGRESS = 0;
	public static final int MEASURE_SUCCESS = 1;
	public static final int MEASURE_FAILED = 2;
	public static final int MEASURE_NO_SAMPLES = 3;

	/**
	 * Adds a MeasureListener to listen to measure change events for given measure.
	 * Each measure can have ONLY one MeasureListener to avoid unnecessary computational
	 * efforts to manage a pool of listeners.
	 * @param measureIndex index of the measure that this listener should listen
	 * @param listener listener to add or null to remove old one
	 */
	public void addMeasureListener(int measureIndex, MeasureListener listener);

	/**
	 * Returns the total number of measures
	 * @return total number of measures
	 */
	public int getMeasureNumber();

	/**
	 * Returns the name of a given measure
	 * @param measureIndex index of the measure
	 * @return name of the measure
	 */
	public String getName(int measureIndex);

	/**
	 * Returns the station name of a given measure
	 * @param measureIndex index of the measure
	 * @return station name
	 */
	public String getStationName(int measureIndex);

	/**
	 * Returns the class name of a given measure
	 * @param measureIndex index of the measure
	 * @return class name
	 */
	public String getClassName(int measureIndex);

	/**
	 * Returns the alpha of a given measure
	 * @param measureIndex index of the measure
	 * @return alpha
	 */
	public double getAlpha(int measureIndex);

	/**
	 * Returns the precision of a given measure
	 * @param measureIndex index of the measure
	 * @return precision
	 */
	public double getPrecision(int measureIndex);

	/**
	 * Returns the number of analyzed samples for a given measure
	 * @param measureIndex index of the measure
	 * @return number of analyzed samples
	 */
	public int getAnalyzedSamples(int measureIndex);

	/**
	 * Returns the number of discarded samples for a given measure
	 * @param measureIndex the measure index
	 * @return number of discarded samples
	 */
	public int getDiscardedSamples(int measureIndex);

	/**
	 * Returns the state of a given measure
	 * @param measureIndex index of the measure
	 * @return measure state
	 */
	public int getMeasureState(int measureIndex);

	/**
	 * Returns the type of a given measure
	 * @param measureIndex index of the measure
	 * @return measure type
	 */
	public String getMeasureType(int measureIndex);

	/**
	 * Returns the node type of a given measure
	 * @param measureIndex index of the measure
	 * @return node type
	 */
	public String getNodeType(int measureIndex);

	/**
	 * Returns the log file name where a verbose measure was stored
	 * @param measureIndex corresponding to a measure
	 * @return full path of the log file including the measure
	 */
	public String getLogFileName(int measureIndex);

	/**
	 * Returns the vector of Temporary values of a given measure. Each element of the vector
	 * is an instance of <code>Value</code> interface.
	 * @param measureIndex index of the measure
	 * @return vector of temporary values until now
	 */
	public Vector<MeasureValue> getValues(int measureIndex);

	/**
	 * Returns an array with the measureIndex of every queue length measure
	 * @return an array with measures' index
	 */	
	public int[] getQueueLengthMeasures();

	/**
	 * Returns an array with the measureIndex of every queue time measure
	 * @return an array with measures' index
	 */
	public int[] getQueueTimeMeasures();

	/**
	 * Returns an array with the measureIndex of every response time measure
	 * @return an array with measures' index
	 */
	public int[] getResponseTimeMeasures();

	/**
	 * Returns an array with the measureIndex of every residence time measure
	 * @return an array with measures' index
	 */
	public int[] getResidenceTimeMeasures();

	/**
	 * Returns an array with the measureIndex of every throughput measure
	 * @return an array with measures' index
	 */
	public int[] getThroughputMeasures();

	/**
	 * Returns an array with the measureIndex of every utilization measure
	 * @return an array with measures' index
	 */
	public int[] getUtilizationMeasures();

	/**
	 * Returns an array with the measureIndex of every drop rate measure
	 * @return an array with measures' index
	 */
	public int[] getDropRateMeasures();

	/**
	 * Returns an array with the measureIndex of every reneging rate measure
	 * @return an array with measures' index
	 */
	public int[] getRenegingRateMeasures();

	/**
	 * Returns an array with the measureIndex of every balking rate measure
	 * @return an array with measures' index
	 */
	public int[] getBalkingRateMeasures();

	/**
	 * Returns an array with the measureIndex of every retrial attempts rate measure
	 * @return an array with measures' index
	 */
	public int[] getRetrialAttemptsRateMeasures();

	/**
	 * Returns an array with the measureIndex of every retrial orbit size measure
	 * @return an array with measures' index
	 */
	public int[] getRetrialOrbitSizeMeasures();

	/**
	 * Returns an array with the measureIndex of every waiting time measure
	 * @return an array with measures' index
	 */
	public int[] getWaitingTimeMeasures();


	/**
	 * Returns an array with the measureIndex of every waiting time measure
	 * @return an array with measures' index
	 */
	public int[] getNumberOfBusyServersMeasures();

	/**
	 * Returns an array with the measureIndex of every system customer number measure
	 * @return an array with measures' index
	 */
	public int[] getSystemCustomerNumberMeasures();

	/**
	 * Returns an array with the measureIndex of every system response time measure
	 * @return an array with measures' index
	 */
	public int[] getSystemResponseTimeMeasures();

	/**
	 * Returns an array with the measureIndex of every system throughput measure
	 * @return an array with measures' index
	 */
	public int[] getSystemThroughputMeasures();

	/**
	 * Returns an array with the measureIndex of every system drop rate measure
	 * @return an array with measures' index
	 */
	public int[] getSystemDropRateMeasures();

	/**
	 * Returns an array with the measureIndex of every system reneging rate measure
	 * @return an array with measures' index
	 */
	public int[] getSystemRenegingRateMeasures();

	/**
	 * Returns an array with the measureIndex of every system balking rate measure
	 * @return an array with measures' index
	 */
	public int[] getSystemBalkingRateMeasures();

	/**
	 * Returns an array with the measureIndex of every system power measure
	 * @return an array with measures' index
	 */
	public int[] getSystemPowerMeasures();

	/**
	 * Returns an array with the measureIndex of every system retrial attempts rate measure
	 * @return an array with measures' index
	 */
	public int[] getSystemRetrialAttemptsRateMeasures();

	/**
	 * Returns an array with the measureIndex of every response time per sink measure
	 * @return an array with measures' index
	 */
	public int[] getResponsetimePerSinkMeasures();

	/**
	 * Returns an array with the measureIndex of every throughput per sink measure
	 * @return an array with measures' index
	 */
	public int[] getThroughputPerSinkMeasures();

	/**
	 * Returns an array with the measureIndex of every FCR total weight measure
	 * @return an array with measures' index
	 */
	public int[] getFCRTotalWeightMeasures();

	/**
	 * Returns an array with the measureIndex of every FCR memory occupation measure
	 * @return an array with measures' index
	 */
	public int[] getFCRMemoryOccupationMeasures();

	/**
	 * Returns an array with the measureIndex of every FJ customer number measure
	 * @return an array with measures' index
	 */
	public int[] getFJCustomerNumberMeasures();

	/**
	 * Returns an array with the measureIndex of every FJ response time measure
	 * @return an array with measures' index
	 */
	public int[] getFJResponseTimeMeasures();

	/**
	 * Returns an array with the measureIndex of every firing throughput measure
	 * @return an array with measures' index
	 */
	public int[] getFiringThroughputMeasures();

	/**
	 * Sets a ProgressListener to listen to progress change events. This is unique.
	 * @param listener listener to be set or null to reset previous one
	 */
	public void setProgressListener(ProgressListener listener);

	public void setMalformedReplayerFileListener(MalformedReplayerFileListener listener);

	public void detectedMalformedReplayerFile(String msg);

	/**
	 * Returns if simulation has finished, so results are fixed
	 * @return true iff simulation has finished
	 */
	public boolean isSimulationFinished();

	/**
	 * Returns simulation polling interval in seconds
	 * @return simulation polling interval in seconds
	 */
	public double getPollingInterval();

	/**
	 * Returns current simulation progress
	 * @return current simulation progress
	 */
	public double getProgress();

	/**
	 * Returns simulation elapsed time in milliseconds
	 * @return simulation elapsed time in milliseconds
	 */
	public long getElapsedTime();

	/**
	 * Returns the decimal separator used in log files
	 * @return the decimal separator used in log files
	 */
	public String getLogDecimalSeparator();

	/**
	 * Returns the CSV delimiter used in log files
	 * @return the CSV delimiter used in log files
	 */
	public String getLogCsvDelimiter();

	// --- Listener Interfaces ------------------------------------------------------------------------
	/**
	 * Interface used to specify a listener on a measure. This is useful to
	 * implement a GUI with a reactive approach.
	 */
	public interface MeasureListener {
		public void measureChanged(List<MeasureValue> measureValues, boolean finished);
	}

	/**
	 * Interface used to specify a listener on progress. This is useful to
	 * implement a GUI with a reactive approach.
	 */
	public interface ProgressListener {
		public void progressChanged(double progress, long elapsedTime);
	}

	/**
	 * Interface used to specify a listener on detected malformed replayer file.
	 * This is useful to implement a GUI with a reactive approach.
	 */
	public interface MalformedReplayerFileListener {
		public void detectedError(String msg);
	}	

}
