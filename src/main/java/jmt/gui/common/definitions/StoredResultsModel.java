/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package jmt.gui.common.definitions;

import java.util.HashMap;
import java.util.Vector;

import jmt.framework.gui.graph.MeasureValue;

/**
 * <p>Title: Result's Model data structure</p>
 * <p>Description: This class will store all values of measured loaded from a file.
 * it is used by <code>XMLResultsReader</code>.</p>
 *
 * @author Bertoli Marco
 *         Date: 3-ott-2005
 *         Time: 14.10.50
 *
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 */
public class StoredResultsModel implements MeasureDefinition {

	protected Vector<Measure> measures = new Vector<Measure>();
	protected HashMap<String, Measure> names = new HashMap<String, Measure>();
	protected double pollingInterval = 1.0;
	protected long elapsedTime = 0;
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
	private Vector<Integer> busyServersNum = new Vector<Integer>();

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

	public Vector<Measure> getMeasures() {
		return measures;
	}

	private String logDecimalSeparator;
	private String logCsvDelimiter;

	/**
	 * Returns the total number of measures
	 * @return total number of measures
	 */
	public int getMeasureNumber() {
		return measures.size();
	}

	/**
	 * Returns the name of a given measure
	 * @param measureIndex index of the measure
	 * @return name of the measure
	 */
	public String getName(int measureIndex) {
		return measures.get(measureIndex).name;
	}

	/**
	 * Returns the station name of a given measure
	 * @param measureIndex index of the measure
	 * @return station name
	 */
	public String getStationName(int measureIndex) {
		return measures.get(measureIndex).stationName;
	}

	/**
	 * Returns the class name of a given measure
	 * @param measureIndex index of the measure
	 * @return class name
	 */
	public String getClassName(int measureIndex) {
		return measures.get(measureIndex).className;
	}

	/**
	 * Returns the alpha of a given measure
	 * @param measureIndex index of the measure
	 * @return alpha
	 */
	public double getAlpha(int measureIndex) {
		return measures.get(measureIndex).alpha;
	}

	/**
	 * Returns the precision of a given measure
	 * @param measureIndex index of the measure
	 * @return precision
	 */
	public double getPrecision(int measureIndex) {
		return measures.get(measureIndex).precision;
	}

	/**
	 * Returns the number of analyzed samples for a given measure
	 * @param measureIndex index of the measure
	 * @return number of analyzed samples
	 */
	public int getAnalyzedSamples(int measureIndex) {
		return measures.get(measureIndex).analyzedSamples;
	}

	/**
	 * Returns the number of discarded samples for a given measure
	 * @param measureIndex index of the measure
	 * @return number of discarded samples
	 */
	public int getDiscardedSamples(int measureIndex) {
		return measures.get(measureIndex).discardedSamples;
	}

	/**
	 * Returns the state of a given measure
	 * @param measureIndex index of the measure
	 * @return measure state
	 */
	public int getMeasureState(int measureIndex) {
		return measures.get(measureIndex).state;
	}

	/**
	 * Returns the type of a given measure
	 * @param measureIndex index of the measure
	 * @return measure type
	 */
	public String getMeasureType(int measureIndex) {
		return measures.get(measureIndex).type;
	}

	/**
	 * Returns the node type of a given measure
	 * @param measureIndex index of the measure
	 * @return name of the measure
	 */
	public String getNodeType(int measureIndex) {
		return measures.get(measureIndex).nodeType;
	}

	/**
	 * Returns the log file name where a verbose measure was stored
	 * @param measureIndex corresponding to a measure
	 * @return full path of the log file including the measure
	 */
	public String getLogFileName(int measureIndex) {
		return measures.get(measureIndex).fileName;
	}

	/**
	 * Returns the vector of Temporary values of a given measure. Each element of the vector
	 * is an instance of <code>Value</code> interface.
	 * @param measureIndex index of the measure
	 * @return vector of temporary values until now
	 */
	public Vector<MeasureValue> getValues(int measureIndex) {
		return measures.get(measureIndex).values;
	}


	//
	/**
	 * Helper method called by all getMeasure functions
	 * Returns an array with the measureIndex of every vector measure
	 * @return an array with measures' index
	 */
	private int[] getTmpArray(Vector<Integer> vector) {
		int[] tmp = new int[vector.size()];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = vector.get(i).intValue();
		}
		return tmp;
	}

	public int[] getQueueLengthMeasures() {
		return getTmpArray(queueLength);
	}

	public int[] getQueueTimeMeasures() {
		return getTmpArray(queueTime);
	}

	public int[] getResponseTimeMeasures() {
		return getTmpArray(responseTime);
	}

	public int[] getResidenceTimeMeasures() {
		return getTmpArray(residenceTime);
	}

	public int[] getUtilizationMeasures() {
		return getTmpArray(utilization);
	}

	public int[] getThroughputMeasures() {
		return getTmpArray(throughput);
	}

	public int[] getDropRateMeasures() {
		return getTmpArray(dropRate);
	}

	public int[] getRenegingRateMeasures() {
		return getTmpArray(renegingRate);
	}

	public int[] getBalkingRateMeasures() {
		return getTmpArray(balkingRate);
	}

	@Override
	public int[] getRetrialAttemptsRateMeasures() {
		return getTmpArray(retrialAttemptsRate);
	}

	@Override
	public int[] getRetrialOrbitSizeMeasures() {
		return getTmpArray(retrialOrbitSize);
	}

	@Override
	public int[] getWaitingTimeMeasures() {
		return getTmpArray(waitingTime);
	}

	@Override
	public int[] getNumberOfBusyServersMeasures() {
		return getTmpArray(busyServersNum);
	}

	public int[] getSystemCustomerNumberMeasures() {
		return getTmpArray(systemCustomerNumber);
	}

	public int[] getSystemResponseTimeMeasures() {
		return getTmpArray(systemResponseTime);
	}

	public int[] getSystemThroughputMeasures() {
		return getTmpArray(systemThroughput);
	}

	public int[] getSystemDropRateMeasures() {
		return getTmpArray(systemDropRate);
	}

	public int[] getSystemRenegingRateMeasures() {
		return getTmpArray(systemRenegingRate);
	}

	public int[] getSystemBalkingRateMeasures() {
		return getTmpArray(systemBalkingRate);
	}

	public int[] getSystemPowerMeasures() {
		return getTmpArray(systemPower);
	}

	@Override
	public int[] getSystemRetrialAttemptsRateMeasures() {
		return getTmpArray(systemRetrialAttemptsRate);
	}

	public int[] getResponsetimePerSinkMeasures() {
		return getTmpArray(responseTimePerSink);
	}

	public int[] getThroughputPerSinkMeasures() {
		return getTmpArray(throughputPerSink);
	}

	public int[] getFCRTotalWeightMeasures() {
		return getTmpArray(FCRTotalWeight);
	}

	public int[] getFCRMemoryOccupationMeasures() {
		return getTmpArray(FCRMemoryOccupation);
	}

	public int[] getFJCustomerNumberMeasures() {
		return getTmpArray(FJCustomerNumber);
	}

	public int[] getFJResponseTimeMeasures() {
		return getTmpArray(FJResponseTime);
	}

	public int[] getFiringThroughputMeasures() {
		return getTmpArray(firingThroughput);
	}


	/**
	 * Returns if simulation has finished, so results are fixed
	 * @return true iff simulation has finished
	 */
	public boolean isSimulationFinished() {
		return true;
	}

	/**
	 * Returns simulation polling interval in seconds
	 * @return simulation polling interval in seconds
	 */
	public double getPollingInterval() {
		return pollingInterval;
	}

	/**
	 * Sets simulation polling interval in seconds
	 * @param interval simulation polling interval in seconds
	 */
	public void setPollingInterval(double interval) {
		this.pollingInterval = interval;
	}

	/**
	 * Returns current simulation progress
	 * @return current simulation progress
	 */
	public double getProgress() {
		return 1.0;
	}

	/**
	 * Returns simulation elapsed time in milliseconds
	 * @return simulation elapsed time in milliseconds
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Sets simulation elapsed time in milliseconds
	 * @param time simulation elapsed time in milliseconds
	 */
	public void setElapsedTime(long time) {
		elapsedTime = time;
	}

	// --- Methods to populate data structure ----------------------------------------------------------------

	/**
	 * Adds a new measure into this data structure.
	 * @param name measure name
	 * @param stationName reference station name
	 * @param className reference class name
	 * @param alpha measure alpha
	 * @param precision measure precision
	 * @param analyzedSamples number of analyzed samples
	 * @param discardedSamples number of discarded samples
	 * @param state state of the measure
	 * @param type type of the measure
	 * @param nodeType node type of the measure
	 * @param fileName the output file name for the verbose measure
	 */
	public void addMeasure(String name, String stationName, String className, double alpha, double precision, int analyzedSamples, int discardedSamples, int state,
			String type, String nodeType,String fileName) {
		Measure tmp = new Measure(name, stationName, className, alpha, precision, analyzedSamples, discardedSamples, state, type, nodeType, fileName);
		measures.add(tmp);
		names.put(name, tmp);
		if (type.equals(SimulationDefinition.MEASURE_QL)) {
			queueLength.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_QT)) {
			queueTime.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_RP)) {
			responseTime.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_RD)) {
			residenceTime.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_U)) {
			utilization.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_X)) {
			throughput.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_DR)) {
			dropRate.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_RR)) {
			renegingRate.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_BR)) {
			balkingRate.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_R)) {
			retrialAttemptsRate.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_RS)) {
			retrialOrbitSize.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_OT)) {
			waitingTime.add(new Integer(measures.size() - 1));
		//} else if (type.equals(SimulationDefinition.MEASURE_BS)) {
		//	busyServersNum.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_S_CN)) {
			systemCustomerNumber.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_S_RP)) {
			systemResponseTime.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_S_X)) {
			systemThroughput.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_S_DR)) {
			systemDropRate.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_S_RR)) {
			systemRenegingRate.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_S_BR)) {
			systemBalkingRate.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_S_P)) {
			systemPower.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_S_R)) {
			systemRetrialAttemptsRate.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_RP_PER_SINK)) {
			responseTimePerSink.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_X_PER_SINK)) {
			throughputPerSink.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_FCR_TW)) {
			FCRTotalWeight.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_FCR_MO)) {
			FCRMemoryOccupation.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_FJ_CN)) {
			FJCustomerNumber.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_FJ_RP)) {
			FJResponseTime.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_FX)) {
			firingThroughput.add(new Integer(measures.size() - 1));
		} else {
			queueLength.add(new Integer(measures.size() - 1));
		}
	}

	/**
	 * Adds a new sample to specified measure
	 * @param measureName name of the measure
	 * @param meanValue mean value of the sample
	 * @param upperBound upper bound of the sample
	 * @param lowerBound lower bound of the sample
	 */
	public void addMeasureSample(String measureName, double lastIntervalAvgValue, double simulationTime, double meanValue, double upperBound, double lowerBound) {
		Measure tmp = names.get(measureName);
		tmp.addSample(meanValue, lastIntervalAvgValue, simulationTime, upperBound, lowerBound);
	}

	// -------------------------------------------------------------------------------------------------------

	// --- Inner Classes--------------------------------------------------------------------------------------

	/**
	 * Inner class to store parameters of each measure
	 */
	protected class Measure {

		public String name;
		public String stationName;
		public String className;
		public double alpha;
		public double precision;
		public int analyzedSamples;
		public int discardedSamples;
		public int state;
		public String type;
		public String nodeType;
		public String fileName;
		public Vector<MeasureValue> values;

		/**
		 * Construct a new Measure object
		 * @param name measure name
		 * @param stationName reference station name
		 * @param className reference class name
		 * @param alpha measure alpha
		 * @param precision measure precision
		 * @param analyzedSamples number of analyzed samples
		 * @param discardedSamples number of discarded samples
		 * @param state state of the measure
		 * @param type type of the measure
		 * @param nodeType node type of the measure
		 * @param fileName the output file name for the verbose measure
		 */
		public Measure(String name, String stationName, String className, double alpha, double precision, int analyzedSamples, int discardedSamples, int state, String type,
				String nodeType, String fileName) {
			this.name = name;
			this.stationName = stationName;
			this.className = className;
			this.alpha = alpha;
			this.precision = precision;
			this.analyzedSamples = analyzedSamples;
			this.discardedSamples = discardedSamples;
			this.state = state;
			this.type = type;
			this.nodeType = nodeType;
			this.fileName = fileName;
			values = new Vector<MeasureValue>();
		}

		/**
		 * Adds a new sample to current measure
		 * @param meanValue mean value of the sample
		 * @param upperBound upper bound of the sample
		 * @param lowerBound lower bound of the sample
		 * @param lastIntervalAvgValue 
		 * @param simulationTime
		 */
		public void addSample(double meanValue, double lastIntervalAvgValue, double simulationTime, double upperBound, double lowerBound) {
			MeasureValueImpl val = new MeasureValueImpl(meanValue, lastIntervalAvgValue, simulationTime, upperBound, lowerBound);
			values.add(val);
		}

	}

	/**
	 * Inner class that implements Value interface
	 */
	public class MeasureValueImpl implements MeasureValue {

		private double mean;
		private double upper;
		private double lower;
		private double lastAvg;
		private double simTime;

		/**
		 * Creates a new MeasureValue object
		 * @param meanValue mean value of the sample
		 * @param upperBound sample upper bound
		 * @param lowerBound sample lower bound
		 */
		public MeasureValueImpl(double meanValue, double lastIntervalAvgValue, double simulationTime, double upperBound, double lowerBound) {
			mean = meanValue;
			upper = upperBound;
			lower = lowerBound;
			lastAvg = lastIntervalAvgValue;
			simTime= simulationTime;
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

		@Override
		public double getLastIntervalAvgValue() {
			return lastAvg;
		}

		@Override
		public double getSimTime() {
			return simTime;
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

	/**
	 * Sets the decimal separator used in log files
	 * @param logDecimalSeparator the decimal separator to set
	 */
	public void setLogDecimalSeparator(String logDecimalSeparator) {
		this.logDecimalSeparator = logDecimalSeparator;
	}

	/**
	 * Sets the CSV delimiter used in log files
	 * @param logCsvDelimiter the CSV delimiter to set
	 */
	public void setLogCsvDelimiter(String logCsvDelimiter) {
		this.logCsvDelimiter = logCsvDelimiter;
	}

	// -------------------------------------------------------------------------------------------------------

	// --- Useless methods -----------------------------------------------------------------------------------

	/**
	 * This feature is not required as loaded measures are static
	 * @param measureIndex index of the measure that this listener should listen
	 * @param listener listener to add or null to remove old one
	 */
	public void addMeasureListener(int measureIndex, MeasureListener listener) {
	}

	/**
	 * This feature is not required as loaded measures are static
	 * @param listener listener to be set or null to reset previous one
	 */
	public void setProgressListener(ProgressListener listener) {
	}

	public void setMalformedReplayerFileListener(MalformedReplayerFileListener listener) {
	}

	public void detectedMalformedReplayerFile(String msg) {
	}

	// -------------------------------------------------------------------------------------------------------

}
