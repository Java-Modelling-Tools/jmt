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

package dataAnalysis.measureOutputs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import dataAnalysis.Measure;
import dataAnalysis.MeasureOutput;

/**
 * This class implements a csv output for measure class. <br>
 * File output: <br>
 * <table border="1"> <tr>
 * <td>Mean Value</td>
 * <td>Mean Value Lower Limit</td>
 * <td>Mean Value Upper Limit</td>
 * <td>Discarded Samples</td>
 * <td>Analyzed Samples</td>
 * <td>Max Samples</td>
 * </tr></table>
 * @author Francesco Radaelli
 */
public class CSVMeasureOutput extends MeasureOutput {

	private static final Logger logger = LoggerFactory.getLogger(CSVMeasureOutput.class);

	private Writer outputWriter;
	private File file;

	private boolean stdOutput;

	/**
	 * Constructor of a CSVMeasureOutput object.
	 * @param measure the measure to be sent in output
	 * @param append true to write at the end of the file (of course
	 * it is useful only if a file is used)
	 * @param fileName the name of the file; if null the output will be sent
	 * on the standard output
	 * @throws java.io.IOException
	 */
	public CSVMeasureOutput(Measure measure, boolean append, String fileName) throws IOException {
		super(measure);
		if (fileName == null) {
			outputWriter = new OutputStreamWriter(System.out);
			stdOutput = true;
		} else {
			this.file = new File(fileName + ".csv");
			outputWriter = new BufferedWriter(new FileWriter(this.file, append));
			stdOutput = false;
		}
	}

	/**
	 * Constructor of a CSVMeasureOutput object, using a file.
	 * @param measure the measure to be sent in output
	 * @param append true to write at the end of the file (of course
	 * it is useful only if a file is used)
	 * @throws java.io.IOException
	 */
	public CSVMeasureOutput(Measure measure, boolean append) throws IOException {
		super(measure);
		outputWriter = new BufferedWriter(new FileWriter(measure.getName() + ".csv", append));
		stdOutput = false;
	}

	@Override
	public void write(double sample, double weight) {
		try {
			outputWriter.write(sample + "; " + weight + ";\n");
		} catch (IOException e) {
			logger.error("Failed to write sample and weight to CSV output.", e);
		}
	}

	@Override
	public void finalizeMeasure() {
		try {
			if (measure.getSuccess()) {
				outputWriter.write("\nMeasure name; " + measure.getName() + ";\n" + "Measure value; " + measure.getMeanValue() + ";\n" + "Lower limit; "
						+ measure.getLowerLimit() + ";\n" + "Upper Limit; " + measure.getUpperLimit() + ";\n" + "Discarded samples; "
						+ measure.getDiscardedSamples() + ";\n" + "Analyzed samples; " + measure.getAnalyzedSamples() + ";\n" + "Max samples; "
						+ measure.getMaxSamples() + ";\n" + "Measure was successful; " + measure.getSuccess() + ";");
			} else {
				outputWriter.write("\nMeasure name; " + measure.getName() + ";\n" + "Estimated mean; " + measure.getEstimatedMeanValue() + ";\n"
						+ "Discarded samples; " + measure.getDiscardedSamples() + ";\n" + "Analyzed samples; " + measure.getAnalyzedSamples() + ";\n"
						+ "Max samples; " + measure.getMaxSamples() + ";\n" + "Measure was successful; " + measure.getSuccess() + ";");
			}

			if (!stdOutput) {
				outputWriter.close();
			}
		} catch (IOException e) {
			logger.error("Failed to finalize measure and write results to CSV.", e);
		}
	}

	/* (non-Javadoc)
	 * @see dataAnalysis.MeasureOutput#getOutputFile()
	 */
	@Override
	public File getOutputFile() {
		return file;
	}

}
