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

import java.util.HashMap;
import java.util.Vector;

import jmt.framework.gui.graph.MeasureValue;
import jmt.gui.common.xml.XMLConstantNames;

/**
 * Created by IntelliJ IDEA.
 * User: francesco
 * Date: 22-feb-2006
 * Time: 12.17.58
 * To change this template use File | Settings | File Templates.
 * 
 * Modified by Ashanka (May 2010): 
 * Patch: Multi-Sink Perf. Index 
 * Description: Added new Performance index for capturing 
 * 				1. global response time (ResponseTime per Sink)
 *              2. global throughput (Throughput per Sink)
 *              each sink per class.
 * 
 * Modified by Ashanka (Oct 2010):
 * Patch: To bug fix Multi-Sink Indices.
 * Description: Implemented the throughputPerSink and responseTimePerSink for the what if/parameteric simulation.
 */
public class PAResultsModel implements MeasureDefinition {

	private Vector<Measure> measures; // An array with all Measures
	private Vector<Number> parameterValues;
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

	private HashMap<String, Measure> names = new HashMap<String, Measure>();

	private String logDecimalSeparator;
	private String logCsvDelimiter;

	//private boolean[] finished;
	//private boolean simulationFinished = false;

	public PAResultsModel(CommonModel model) {
		Vector<Object> measureKeys = model.getMeasureKeys();
		measures = new Vector<Measure>(measureKeys.size());
		this.logCsvDelimiter = model.getLoggingGlbParameter("delim");
		this.logDecimalSeparator = model.getLoggingGlbParameter("decimalSeparator");

		for (int i = 0; i < measureKeys.size(); i++) {
			Object measureKey = measureKeys.get(i);
			String measureType = model.getMeasureType(measureKey);
			Object stationKey = model.getMeasureStation(measureKey);
			String stationName = null;
			String nodeType = null;
			if (stationKey == null) {
				if (model.isGlobalMeasure(measureType)) {
					stationName = "Network";
				} else {
					stationName = "";
				}
				nodeType = "";
			} else {
				if (model.getRegionKeys().contains(stationKey)) {
					stationName = model.getRegionName(stationKey);
					nodeType = XMLConstantNames.NODETYPE_REGION;
				} else {
					stationName = model.getStationName(stationKey);
					nodeType = XMLConstantNames.NODETYPE_STATION;
				}
			}

			Object classKey = model.getMeasureClass(measureKey);
			String className = null;
			if (measureType.equals(SimulationDefinition.MEASURE_FX)) {
				className = (classKey == null) ? "All modes" : (String) classKey;
			} else {
				className = (classKey == null) ? "All classes" : model.getClassName(classKey);
			}
			String measureName = stationName + "_" + className + "_" + measureType;
			double alpha = model.getMeasureAlpha(measureKey).doubleValue();
			double precision = model.getMeasurePrecision(measureKey).doubleValue();
			if (measureType.equals(SimulationDefinition.MEASURE_QL)) {
				queueLength.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_QT)) {
				queueTime.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_RP)) {
				responseTime.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_RD)) {
				residenceTime.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_U)) {
				utilization.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_X)) {
				throughput.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_DR)) {
				dropRate.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_R)) {
				retrialAttemptsRate.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_BR)) { // added BR and RR for PA
				balkingRate.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_RR)) {
				renegingRate.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_OT)) {
				waitingTime.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_RS)) {
				retrialOrbitSize.add(new Integer(i));
			//} else if (measureType.equals(SimulationDefinition.MEASURE_BS)) {
			//	busyServersNum.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_CN)) {
				systemCustomerNumber.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_RP)) {
				systemResponseTime.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_X)) {
				systemThroughput.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_DR)) {
				systemDropRate.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_RR)) {
				systemRenegingRate.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_BR)) {
				systemBalkingRate.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_P)) {
				systemPower.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_S_R)) {
				systemRetrialAttemptsRate.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_RP_PER_SINK)) {
				responseTimePerSink.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_X_PER_SINK)) {
				throughputPerSink.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_FCR_TW)) {
				FCRTotalWeight.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_FCR_MO)) {
				FCRMemoryOccupation.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_FJ_CN)) {
				FJCustomerNumber.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_FJ_RP)) {
				FJResponseTime.add(new Integer(i));
			} else if (measureType.equals(SimulationDefinition.MEASURE_FX)) {
				firingThroughput.add(new Integer(i));
			} else {
				queueLength.add(new Integer(i));
			}
			Measure tmp = new Measure(measureName, stationName, className, alpha, precision, measureType, nodeType);
			measures.add(tmp);
			names.put(measureName, tmp);
		}
		parameterValues = model.getParametricAnalysisModel().getParameterValues();
	}

	public PAResultsModel(CommonModel model, boolean loadFromFile) {
		measures = new Vector<Measure>();
		parameterValues = model.getParametricAnalysisModel().getParameterValues();
	}

	/**
	 * Adds a new measure into this data structure.
	 * @param name measure name
	 * @param stationName reference station name
	 * @param className reference class name
	 * @param alpha measure alpha
	 * @param precision measure precision
	 * @param type type of the measure
	 * @param nodeType node type of the measure
	 */
	public void addMeasure(String name, String stationName, String className, double alpha, double precision, String type, String nodeType) {
		Measure tmp = new Measure(name, stationName, className, alpha, precision, type, nodeType);
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
		} else if (type.equals(SimulationDefinition.MEASURE_OT)) {
			waitingTime.add(new Integer(measures.size() - 1));
		} else if (type.equals(SimulationDefinition.MEASURE_RS)) {
			retrialOrbitSize.add(new Integer(measures.size() - 1));
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

	public void addSample(int measureIndex, double lowerBound, double meanValue, double upperBound, boolean validity) {
		Measure requested = measures.get(measureIndex);
		requested.addSample(meanValue, upperBound, lowerBound, validity);
	}

	public void addSample(String measureName, double lowerBound, double meanValue, double upperBound, boolean validity) {
		Measure requested = names.get(measureName);
		requested.addSample(meanValue, upperBound, lowerBound, validity);
	}
	
	public MeasureValue getEmptyMeasureValue() {
		double avgupper = 0.0;
		double avgvalue = 0.0;
		double avglower = 0.0;		
		return new MeasureValueImpl(avgvalue, avgupper, avglower, false);
	}

	public Vector<Number> getParameterValues() {
		return parameterValues;
	}

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
		String name = measures.get(measureIndex).stationName;
		return name;
	}

	/**
	 * Returns the class name of a given measure
	 * @param measureIndex index of the measure
	 * @return class name
	 */
	public String getClassName(int measureIndex) {
		String name = measures.get(measureIndex).className;
		return name;
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
		return (measures.get(measureIndex)).samples;
	}

	/**
	 * Returns the state of a given measure
	 * @param measureIndex index of the measure
	 * @param measureStep step of the measure
	 * @return measure state
	 */
	public boolean getMeasureState(int measureIndex, int measureStep) {
		MeasureValueImpl value = (MeasureValueImpl) (getValues(measureIndex).get(measureStep));
		return value.isValid();
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
	 * @return node type
	 */
	public String getNodeType(int measureIndex) {
		return measures.get(measureIndex).nodeType;
	}

	/**
	 * Returns the vector of values of a given measure. Each element of the vector
	 * is an instance of <code>MeasureValue</code> interface.
	 * @param measureIndex index of the measure
	 * @return vector of values
	 */
	public Vector<MeasureValue> getValues(int measureIndex) {
		return measures.get(measureIndex).values;
	}

	// --- List of getMeasure functions for each measure
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

	// ------------------------- USELESS METHODS --------------------------------

	/**
	 * Not implemented
	 *
	 * @param measureIndex index of the measure that this listener should listen
	 * @param listener listener to add or null to remove old one
	 */
	public void addMeasureListener(int measureIndex, MeasureListener listener) {
		//Not implemented
	}

	/**
	 * Not implemented
	 *
	 * @param listener listener to be set or null to reset previous one
	 */
	public void setProgressListener(ProgressListener listener) {
		//Not implemented
	}

	/**
	 * Not implemented
	 *
	 * @return true
	 */
	public boolean isSimulationFinished() {
		return true;
	}

	/**
	 * Not implemented
	 *
	 * @return 0
	 */
	public double getPollingInterval() {
		return 0;
	}

	/**
	 * Not implemented
	 *
	 * @return 0
	 */
	public double getProgress() {
		return 0;
	}

	/**
	 * Not implemented
	 *
	 * @return 0
	 */
	public long getElapsedTime() {
		return 0;
	}

	/**
	 * Returns the number of discarded samples for a given measure
	 * @param measureIndex the measure index
	 * @return number of discarded samples
	 */
	public int getDiscardedSamples(int measureIndex) {
		return 0;
	}

	/**
	 * Returns the state of a given measure
	 * @param measureIndex index of the measure
	 * @return measure state
	 */
	public int getMeasureState(int measureIndex) {
		return 0;
	}

	/**
	 * Returns the log file name where a verbose measure was stored
	 * @param measureIndex corresponding to a measure
	 * @return full path of the log file including the measure
	 */
	public String getLogFileName(int measureIndex) {
		return null;
	}

	// ------------------------- end USELESS METHODS --------------------------------

	/**
	 * Inner class to store parameters of each measure
	 */
	protected class Measure {

		public String name;
		public String stationName;
		public String className;
		public double alpha;
		public double precision;
		public String type;
		public String nodeType;
		public int samples;
		public Vector<MeasureValue> values;

		/**
		 * Construct a new Measure object
		 * @param name measure name
		 * @param stationName reference station name
		 * @param className reference class name
		 * @param alpha measure alpha
		 * @param precision measure precision
		 * @param type type of the measure
		 * @param nodeType node type of the measure
		 */
		public Measure(String name, String stationName, String className, double alpha, double precision, String type, String nodeType) {
			this.name = name;
			this.stationName = stationName;
			this.className = className;
			this.alpha = alpha;
			this.precision = precision;
			this.type = type;
			this.nodeType = nodeType;
			values = new Vector<MeasureValue>();
		}

		/**
		 * Adds a new sample to current measure
		 * @param meanValue mean value of the sample
		 * @param upperBound upper bound of the sample
		 * @param lowerBound lower bound of the sample
		 */
		public void addSample(double meanValue, double upperBound, double lowerBound, boolean validity) {
			MeasureValueImpl val = new MeasureValueImpl(meanValue, upperBound, lowerBound, validity);
			values.add(val);
			samples++;
		}
	}

	/**
	 * Inner class that implements Value interface
	 */
	public class MeasureValueImpl implements MeasureValue {

		private double mean;
		private double upper;
		private double lower;
		private boolean valid;

		/**
		 * Creates a new MeasureValue object
		 * @param meanValue mean value of the sample
		 * @param upperBound sample upper bound
		 * @param lowerBound sample lower bound
		 * @param isValid true if the measure could be computed with the requested precision
		 */
		public MeasureValueImpl(double meanValue, double upperBound, double lowerBound, boolean isValid) {
			mean = meanValue;
			upper = upperBound;
			lower = lowerBound;
			valid = isValid;
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

		public void setMeanValue(double x) {
			this.mean = x;
		}

		public void setUpperBound(double x) {
			this.upper = x;
		}

		public void setLowerBound(double x) {
			this.lower = x;
		}
		
		public void setValid(boolean x) {
			this.valid = x;
		}

		public boolean isValid() {
			return valid;
		}

		@Override
		public double getLastIntervalAvgValue() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getSimTime() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	public void setMalformedReplayerFileListener(MalformedReplayerFileListener listener) {
	}

	public void detectedMalformedReplayerFile(String msg) {
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

}
