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

import jmt.engine.dataAnalysis.TempMeasure;
import jmt.framework.gui.graph.MeasureValue;

/**
 * <p>Title: Result's Model data structure</p>
 * <p>Description: This class will collect temporary results from simulation at
 * specified time intervals, and final results. It will not read simulator's generated XML
 * as it provides only final results but will rely on TempMeasure data structures provided
 * by <code>DispatcherJSIMschema</code>.</p>
 * 
 * @author Bertoli Marco
 *         Date: 23-set-2005
 *         Time: 15.57.43
 *         
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public class ResultsModel implements MeasureDefinition {

	private TempMeasure[] measures; // An array with all TempMeasures
	private Vector<Integer> queueLength = new Vector<Integer>();
	private Vector<Integer> queueTime = new Vector<Integer>();
	private Vector<Integer> responseTime = new Vector<Integer>();
	private Vector<Integer> residenceTime = new Vector<Integer>();
	private Vector<Integer> utilization = new Vector<Integer>();
	private Vector<Integer> throughput = new Vector<Integer>();
	private Vector<Integer> dropRate = new Vector<Integer>();
	private Vector<Integer> renegingRate = new Vector<Integer>();
	private Vector<Integer> balkingRate = new Vector<Integer>();
	private Vector<Integer> retrialAttemptsRate = new Vector<Integer>();
	private Vector<Integer> waitingTime = new Vector<Integer>();
	private Vector<Integer> retrialOrbitSize = new Vector<Integer>();
	private Vector<Integer> busyServerVar = new Vector<Integer>();

	private Vector<Integer> systemCustomerNumber = new Vector<Integer>();
	private Vector<Integer> systemResponseTime = new Vector<Integer>();
	private Vector<Integer> systemThroughput = new Vector<Integer>();
	private Vector<Integer> systemDropRate = new Vector<Integer>();
	private Vector<Integer> systemRenegingRate = new Vector<Integer>();
	private Vector<Integer> systemBalkingRate = new Vector<Integer>();
	private Vector<Integer> systemPower = new Vector<Integer>();
	private Vector<Integer> systemRetrialAttemptsRate = new Vector<Integer>();

	private Vector<Integer> responseTimePerSink = new Vector<Integer>();
	private Vector<Integer> throughputPerSink = new Vector<Integer>();
	private Vector<Integer> FCRTotalWeight = new Vector<Integer>();
	private Vector<Integer> FCRMemoryOccupation = new Vector<Integer>();
	private Vector<Integer> FJCustomerNumber = new Vector<Integer>();
	private Vector<Integer> FJResponseTime = new Vector<Integer>();
	private Vector<Integer> firingThroughput = new Vector<Integer>();

	private Vector[] measuresVector; // For each TempMeasure holds a Vector with its value at every poll
	private boolean[] finished;
	private MeasureListener[] listeners; // Listener array to notify GUI of measure change events
	private ProgressListener plistener = null;
	private boolean simulationFinished = false;
	private double pollingInterval;
	private double progress = 0.0;
	private long elapsedTime = 0;
	private MalformedReplayerFileListener mrfListener;

	private String logDecimalSeparator;
	private String logCsvDelimiter;

	/**
	 * Constructs a new ResultsModel
	 * @param pollingInterval measure polling interval
	 */
	public ResultsModel(double pollingInterval, String logCsvDelimiter, String logDecimalSeparator) {
		this.pollingInterval = pollingInterval;
		this.logCsvDelimiter = logCsvDelimiter;
		this.logDecimalSeparator = logDecimalSeparator;
	}

	/**
	 * Sets this data structure, provided an array of TempMeasures
	 * @param measures array of TempMeasures, as
	 * returned by <code>DispatcherJSIMschema.getTempMeasures()</code>
	 * @param progress Progress of simulation
	 * @param elapsedTime Elapsed time of simulation
	 */
	public synchronized void setTempMeasures(TempMeasure[] measures, double progress, long elapsedTime) {
		this.measures = measures;
		measuresVector = new Vector[measures.length];
		finished = new boolean[measures.length];
		listeners = new MeasureListener[measures.length];
		for (int i = 0; i < measures.length; i++) {
			String type = measures[i].getMeasureType();
			if (type.equals(SimulationDefinition.MEASURE_QL)) {
				queueLength.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_QT)) {
				queueTime.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_RP)) {
				responseTime.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_RD)) {
				residenceTime.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_U)) {
				utilization.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_X)) {
				throughput.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_DR)) {
				dropRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_RR)) {
				renegingRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_BR)) {
				balkingRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_R)) {
				retrialAttemptsRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_BR)) { // added BR and RR for PA
				balkingRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_RR)) {
				renegingRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_OT)) {
				waitingTime.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_RS)) {
				retrialOrbitSize.add(new Integer(i));
			//} else if (type.equals(SimulationDefinition.MEASURE_BS)) {
			//	busyServerVar.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_S_CN)) {
				systemCustomerNumber.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_S_RP)) {
				systemResponseTime.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_S_X)) {
				systemThroughput.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_S_DR)) {
				systemDropRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_S_RR)) {
				systemRenegingRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_S_BR)) {
				systemBalkingRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_S_P)) {
				systemPower.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_S_R)) {
				systemRetrialAttemptsRate.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_RP_PER_SINK)) {
				responseTimePerSink.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_X_PER_SINK)) {
				throughputPerSink.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_FCR_TW)) {
				FCRTotalWeight.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_FCR_MO)) {
				FCRMemoryOccupation.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_FJ_CN)) {
				FJCustomerNumber.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_FJ_RP)) {
				FJResponseTime.add(new Integer(i));
			} else if (type.equals(SimulationDefinition.MEASURE_FX)) {
				firingThroughput.add(new Integer(i));
			} else {
				queueLength.add(new Integer(i));
			}
			// Adds to allMeasures HashMap a vector to collect all values of this measure
			Vector<MeasureValueImpl> temp_mean = new Vector<MeasureValueImpl>();
			temp_mean.add(new MeasureValueImpl(measures[i]));
			measuresVector[i] = temp_mean;
			finished[i] = false;
		}
		this.progress = progress;
		this.elapsedTime = elapsedTime;
		// Notifies progress listener
		if (plistener != null) {
			plistener.progressChanged(progress, elapsedTime);
		}
	}

	/**
	 * Refresh stored tempMeasures.
	 * PRECONDITION: refresh method MUST HAVE BEEN called on every TempMeasure before calling
	 * this method. So simply this method is designed to be called after every
	 * <code>DispatcherJSIMschema.refreshTempMeasures()</code> call.
	 * @param progress Progress of simulation
	 * @param elapsedTime Elapsed time of simulation
	 */
	public synchronized void refresh(double progress, long elapsedTime) {
		if (measures == null) {
			return;
		}
		this.progress = progress;
		this.elapsedTime = elapsedTime;
		// Simulation finished
		if (progress >= 1.0) {
			simulationFinished = true;
		}
		// Notifies progress listener
		if (plistener != null) {
			plistener.progressChanged(progress, elapsedTime);
		}
		for (int i = 0; i < measures.length; i++) {
			// If measure is not finished, register new value for that measure
			if (!finished[i]) {
				// If simulation is finished, try to get final value
				if (simulationFinished) {
					while (!measures[i].isFinished()) {
						measures[i].refreshMeasure();
					}
				}
				measuresVector[i].add(new MeasureValueImpl(measures[i]));
				if (measures[i].isFinished()) {
					finished[i] = true;
				}
				// Notifies measure listener (if any)
				if (listeners[i] != null) {
					listeners[i].measureChanged(measuresVector[i], finished[i]);
				}
			}
		}
	}

	/**
	 * Adds a MeasureListener to listen to measure change events for given measure.
	 * Each measure can have ONLY one MeasureListener to avoid unnecessary computational
	 * efforts to manage a pool of listeners.
	 * @param measureIndex index of the measure that this listener should listen
	 * @param listener listener to add or null to remove old one
	 */
	public synchronized void addMeasureListener(int measureIndex, MeasureListener listener) {
		// Sanity check on parameters
		if (measures == null || measureIndex < 0 || measureIndex >= measures.length) {
			return;
		}
		listeners[measureIndex] = listener;
	}

	/**
	 * Returns the total number of measures
	 * @return total number of measures
	 */
	public synchronized int getMeasureNumber() {
		return measures.length;
	}

	/**
	 * Returns the name of a given measure
	 * @param measureIndex index of the measure
	 * @return name of the measure
	 */
	public synchronized String getName(int measureIndex) {
		return measures[measureIndex].getName();
	}

	/**
	 * Returns the station name of a given measure
	 * @param measureIndex index of the measure
	 * @return station name
	 */
	public synchronized String getStationName(int measureIndex) {
		return measures[measureIndex].getNodeName();
	}

	/**
	 * Returns the class name of a given measure
	 * @param measureIndex index of the measure
	 * @return class name
	 */
	public synchronized String getClassName(int measureIndex) {
		return measures[measureIndex].getJobClass();
	}

	/**
	 * Returns the alpha of a given measure
	 * @param measureIndex index of the measure
	 * @return alpha
	 */
	public synchronized double getAlpha(int measureIndex) {
		return 1 - measures[measureIndex].getAlpha();
	}

	/**
	 * Returns the precision of a given measure
	 * @param measureIndex index of the measure
	 * @return precision
	 */
	public synchronized double getPrecision(int measureIndex) {
		return measures[measureIndex].getPrecision();
	}

	/**
	 * Returns the number of analyzed samples for a given measure
	 * @param measureIndex index of the measure
	 * @return number of analyzed samples
	 */
	public synchronized int getAnalyzedSamples(int measureIndex) {
		return measures[measureIndex].getNsamples();
	}

	/**
	 * Returns the number of discarded samples for a given measure
	 * @param measureIndex the measure index
	 * @return number of discarded samples
	 */
	public synchronized int getDiscardedSamples(int measureIndex) {
		return measures[measureIndex].getDiscarded();
	}

	/**
	 * Returns the state of a given measure
	 * @param measureIndex index of the measure
	 * @return measure state
	 */
	public synchronized int getMeasureState(int measureIndex) {
		if (!measures[measureIndex].isFinished()) {
			return MEASURE_IN_PROGRESS;
		} else if (measures[measureIndex].receivedNoSamples()) {
			return MEASURE_NO_SAMPLES;
		} else if (!measures[measureIndex].isSuccessful()) {
			return MEASURE_FAILED;
		} else {
			return MEASURE_SUCCESS;
		}
	}

	/**
	 * Returns the type of a given measure
	 * @param measureIndex index of the measure
	 * @return measure type
	 */
	public synchronized String getMeasureType(int measureIndex) {
		return measures[measureIndex].getMeasureType();
	}

	/**
	 * Returns the node type of a given measure
	 * @param measureIndex index of the measure
	 * @return node type
	 */
	public synchronized String getNodeType(int measureIndex) {
		return measures[measureIndex].getNodeType();
	}

	/**
	 * Returns the log file name where a verbose measure was stored
	 * @param measureIndex corresponding to a measure
	 * @return full path of the log file including the measure
	 */
	public synchronized String getLogFileName(int measureIndex) {
		return measures[measureIndex].getLogFileName();
	}

	/**
	 * Returns the vector of Temporary values of a given measure. Each element of the vector
	 * is an instance of <code>Value</code> interface.
	 * @param measureIndex index of the measure
	 * @return vector of temporary values until now
	 */
	public synchronized Vector<MeasureValue> getValues(int measureIndex) {
		return measuresVector[measureIndex];
	}

	/**
	 * Returns an array with the measureIndex of every queue length measure
	 * @return an array with measures' index
	 */
	public int[] getQueueLengthMeasures() {
		int[] tmp = new int[queueLength.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = queueLength.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every queue time measure
	 * @return an array with measures' index
	 */
	public int[] getQueueTimeMeasures() {
		int[] tmp = new int[queueTime.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = queueTime.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every response time measure
	 * @return an array with measures' index
	 */
	public int[] getResponseTimeMeasures() {
		int[] tmp = new int[responseTime.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = responseTime.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every residence time measure
	 * @return an array with measures' index
	 */
	public int[] getResidenceTimeMeasures() {
		int[] tmp = new int[residenceTime.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = residenceTime.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every utilization measure
	 * @return an array with measures' index
	 */
	public int[] getUtilizationMeasures() {
		int[] tmp = new int[utilization.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = utilization.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every throughput measure
	 * @return an array with measures' index
	 */
	public int[] getThroughputMeasures() {
		int[] tmp = new int[throughput.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = throughput.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every drop rate measure
	 * @return an array with measures' index
	 */
	public int[] getDropRateMeasures() {
		int[] tmp = new int[dropRate.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = dropRate.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every reneging rate measure
	 * @return an array with measures' index
	 */
	public int[] getRenegingRateMeasures() {
		int[] tmp = new int[renegingRate.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = renegingRate.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every balking rate measure
	 * @return an array with measures' index
	 */
	public int[] getBalkingRateMeasures() {
		int[] tmp = new int[balkingRate.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = balkingRate.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every retrial attempts rate measure
	 * @return an array with measures' index
	 */
	public int[] getRetrialAttemptsRateMeasures() {
		int[] tmp = new int[retrialAttemptsRate.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = retrialAttemptsRate.get(i).intValue();
		}
		return tmp;
	}

	@Override
	public int[] getRetrialOrbitSizeMeasures() {
		int[] tmp = new int[retrialOrbitSize.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = retrialOrbitSize.get(i).intValue();
		}
		return tmp;
	}

	@Override
	public int[] getWaitingTimeMeasures() {
		int[] tmp = new int[waitingTime.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = waitingTime.get(i).intValue();
		}
		return tmp;	}

	@Override
	public int[] getNumberOfBusyServersMeasures() {
		int[] tmp = new int[busyServerVar.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = busyServerVar.get(i).intValue();
		}
		return tmp;
	}


	/**
	 * Returns an array with the measureIndex of every system customer number measure
	 * @return an array with measures' index
	 */
	public int[] getSystemCustomerNumberMeasures() {
		int[] tmp = new int[systemCustomerNumber.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = systemCustomerNumber.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every system response time measure
	 * @return an array with measures' index
	 */
	public int[] getSystemResponseTimeMeasures() {
		int[] tmp = new int[systemResponseTime.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = systemResponseTime.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every system throughput measure
	 * @return an array with measures' index
	 */
	public int[] getSystemThroughputMeasures() {
		int[] tmp = new int[systemThroughput.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = systemThroughput.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every system drop rate measure
	 * @return an array with measures' index
	 */
	public int[] getSystemDropRateMeasures() {
		int[] tmp = new int[systemDropRate.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = systemDropRate.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every system reneging rate measure
	 * @return an array with measures' index
	 */
	public int[] getSystemRenegingRateMeasures() {
		int[] tmp = new int[systemRenegingRate.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = systemRenegingRate.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every system reneging rate measure
	 * @return an array with measures' index
	 */
	public int[] getSystemBalkingRateMeasures() {
		int[] tmp = new int[systemBalkingRate.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = systemBalkingRate.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every system power measure
	 * @return an array with measures' index
	 */
	public int[] getSystemPowerMeasures() {
		int[] tmp = new int[systemPower.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = systemPower.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every system retrial attempts rate measure
	 * @return an array with measures' index
	 */
	public int[] getSystemRetrialAttemptsRateMeasures() {
		int[] tmp = new int[systemRetrialAttemptsRate.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = systemRetrialAttemptsRate.get(i).intValue();
		}
		return tmp;
	}


	/**
	 * Returns an array with the measureIndex of every response time per sink measure
	 * @return an array with measures' index
	 */
	public int[] getResponsetimePerSinkMeasures() {
		int[] tmp = new int[responseTimePerSink.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = responseTimePerSink.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every throughput per sink measure
	 * @return an array with measures' index
	 */
	public int[] getThroughputPerSinkMeasures() {
		int[] tmp = new int[throughputPerSink.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = throughputPerSink.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every FCR total weight measure
	 * @return an array with measures' index
	 */
	public int[] getFCRTotalWeightMeasures() {
		int[] tmp = new int[FCRTotalWeight.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = FCRTotalWeight.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every FCR memory occupation measure
	 * @return an array with measures' index
	 */
	public int[] getFCRMemoryOccupationMeasures() {
		int[] tmp = new int[FCRMemoryOccupation.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = FCRMemoryOccupation.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every FJ customer number measure
	 * @return an array with measures' index
	 */
	public int[] getFJCustomerNumberMeasures() {
		int[] tmp = new int[FJCustomerNumber.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = FJCustomerNumber.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every FJ response time measure
	 * @return an array with measures' index
	 */
	public int[] getFJResponseTimeMeasures() {
		int[] tmp = new int[FJResponseTime.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = FJResponseTime.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns an array with the measureIndex of every firing throughput measure
	 * @return an array with measures' index
	 */
	public int[] getFiringThroughputMeasures() {
		int[] tmp = new int[firingThroughput.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = firingThroughput.get(i).intValue();
		}
		return tmp;
	}

	/**
	 * Returns simulation polling interval in seconds
	 * @return simulation polling interval in seconds
	 */
	public double getPollingInterval() {
		return pollingInterval;
	}

	/**
	 * Sets a ProgressListener to listen to progress change events. This is unique.
	 * @param listener listener to be set or null to reset previous one
	 */
	public synchronized void setProgressListener(ProgressListener listener) {
		plistener = listener;
	}

	public synchronized void setMalformedReplayerFileListener(MalformedReplayerFileListener listener) {
		mrfListener = listener;
	}

	public synchronized void detectedMalformedReplayerFile(String msg) {
		if (mrfListener != null) {
			mrfListener.detectedError(msg);
		}
	}

	/**
	 * Returns current simulation progress
	 * @return current simulation progress
	 */
	public double getProgress() {
		return progress;
	}

	/**
	 * Returns simulation elapsed time in milliseconds
	 * @return simulation elapsed time in milliseconds
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Returns if simulation has finished, so results are fixed
	 * @return true iff simulation has finished
	 */
	public synchronized boolean isSimulationFinished() {
		return simulationFinished;
	}

	/**
	 * Implementation of Value interface
	 */
	public class MeasureValueImpl implements MeasureValue {

		private double mean;
		private double upper;
		private double lower;
		private double lastIntervalAvgValue;
		private double simulationTime;

		public MeasureValueImpl(TempMeasure tm) {
			mean = tm.getTempMean();
			upper = tm.getUpperBound();
			lower = tm.getLowerBound();
			lastIntervalAvgValue = tm.getLastIntervalAvgValue();
			simulationTime= tm.getSimTime();
		}

		public double getMeanValue() {
			return mean;
		}

		public double getUpperBound() {
			return upper;
		}

		public double getLowerBound() {
			return lower;
		}

		public double getLastIntervalAvgValue() {
			return lastIntervalAvgValue;
		}

		public double getSimTime() {
			return simulationTime;
		}

	}

	/**
	 * Returns the decimal separator used in log files
	 * @return the decimal separator used in log files
	 */
	public String getLogDecimalSeparator() {
		return logDecimalSeparator;
	}

	/**
	 * Returns the CSV delimiter used in log files
	 * @return the CSV delimiter used in log files
	 */
	public String getLogCsvDelimiter() {
		return logCsvDelimiter;
	}

}
