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

package jmt.engine.dataAnalysis.measureOutputs;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import jmt.engine.NodeSections.LogTunnel;
import jmt.engine.dataAnalysis.Measure;
import jmt.engine.dataAnalysis.MeasureOutput;
import jmt.engine.dataAnalysis.SimParameters;
import jmt.engine.log.CSVLogger;
import jmt.engine.log.JSimLoggerFactory;
import jmt.engine.log.LoggerParameters;

/**
 * This class implements a csv output for measure class. <br>
 * File output: <br>
 * <table border="1"> <tr>
 * <td>Timestamp (simulation time)</td>
 * <td>Sample</td>
 * <td>Weight</td>
 * </tr></table>
 */
public class VerboseCSVMeasureOutput extends MeasureOutput {

	public static final String COLUMN_TS = LogTunnel.COLUMN_TIMESTAMP;
	public static final String COLUMN_SAMPLEVALUE = "SAMPLE";
	public static final String COLUMN_WEIGHTVALUE = "WEIGHT";

	public static final String[] COLUMNS = {COLUMN_TS, COLUMN_SAMPLEVALUE, COLUMN_WEIGHTVALUE};

	boolean isTerminalSimulation;
	int parametricStep;

	private CSVLogger logger;
	private File file;

	private Map<String, Object> defaults = Collections.emptyMap();

	/**
	 * Constructor of a NewCSVMeasureOutput object, using a file.
	 * @param Measure the measure to be sent in output
	 * @param Append true to write at the end of the file (of course
	 * it is useful only if a file is used)
	 * @throws java.io.IOException
	 */
	public VerboseCSVMeasureOutput(Measure Measure, SimParameters simParameters, boolean isTerminalSimulation, int parametricStep) throws IOException {
		super(Measure);
		this.isTerminalSimulation = isTerminalSimulation;
		this.parametricStep = parametricStep;
		if (parametricStep < 0) {
			file = new File(simParameters.getLogPath(), measure.getName() + ".csv");
		} else {
			file = new File(simParameters.getLogPath(), measure.getName() + "_" + (parametricStep + 1) + ".csv");
		}
		int intReplacePolicy = Integer.parseInt(simParameters.getLogReplaceMode());
		logger = JSimLoggerFactory.getCSVLogger(file, COLUMNS, intReplacePolicy == LoggerParameters.LOGGER_AR_APPEND,
				simParameters.getLogDelimiter(), simParameters.getLogDecimalSeparator());
	}

	@Override
	public void write(double sample, double weight) {
		if (logger == null) {
			return;
		}

		Map<String, Object> values = new HashMap<String, Object>();
		values.put(COLUMN_TS, measure.getNetSystem().getTime());
		values.put(COLUMN_SAMPLEVALUE, sample);
		values.put(COLUMN_WEIGHTVALUE, weight);
		try {
			logger.log(values, defaults);
		} catch (IOException e1) {
			if (isTerminalSimulation) {
				System.out.println("JSIMengine - Warning: Failed to write the CSV file: " + file.getAbsolutePath());
			} else {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
						}
						JOptionPane.showMessageDialog(null, "Failed to write the CSV file: " + file.getAbsolutePath(),
								"JSIMengine - Warning", JOptionPane.WARNING_MESSAGE);
					}
				}).start();
			}
			try {
				JSimLoggerFactory.remove(logger);
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			logger = null;
		}
	}

	@Override
	public void finalizeMeasure() {
		if (logger == null) {
			return;
		}

		try {
			JSimLoggerFactory.remove(logger);
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger = null;
	}

	/* (non-Javadoc)
	 * @see jmt.engine.dataAnalysis.MeasureOutput#getOutputFile()
	 */
	@Override
	public File getOutputFile() {
		return file;
	}

}
