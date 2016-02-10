/**    
  * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.

  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */
package jmt.engine.math;

/**
 * An inverted version of sample statistics, used to evaluate throughput samples.
 * @author Marco Bertoli
 */
public class InverseSampleStatistics extends SampleStatistics {
	// The first batch is used to find a common denominator for the throughput interval
	private static final int INITIAL_BATCH_SIZE = 100;
	
	// The following variables handles batches
	private int batchSamples = 0;
	private double currentJobsNumber = 0.0;
	private double currentDeltaTime = 0.0;
	private double currentSimulationTime = 0.0;
	private double prevJobsNumber = 0.0;
	
	// Batch time is NaN until initialized
	private double batchTimeSize = Double.NaN;

	/* (non-Javadoc)
	 * @see jmt.engine.math.SampleStatistics#putNewSample(double, double, double)
	 */
	@Override
	public void putNewSample(double jobsNumber, double deltaTime, double simulationTime) {
		// Handle the special case in which two jobs ended at the same time
		if (deltaTime == 0) {
			currentJobsNumber += jobsNumber;
			prevJobsNumber += jobsNumber;
			return;
		}

		// Process the previous batch if it's full
		if (currentDeltaTime >= batchTimeSize) {
			processSamples(currentJobsNumber, batchTimeSize, currentSimulationTime);
			
			// Reset for the next batch but keep previous job if it was splitted accross more batches
			currentDeltaTime = currentDeltaTime - batchTimeSize;
			if (currentDeltaTime > 0) {
				currentJobsNumber = prevJobsNumber;
				batchSamples = 1;
			} else {
				batchSamples = 0;
				currentJobsNumber = 0.0;
				currentDeltaTime = 0.0;
			}
		}

		// Save job number for batch split
		prevJobsNumber = jobsNumber;
		
		// Increment the current batch
		currentJobsNumber += jobsNumber;
		currentDeltaTime += deltaTime;
		currentSimulationTime = simulationTime;
		batchSamples++;
		
		if (Double.isNaN(batchTimeSize) && batchSamples >= INITIAL_BATCH_SIZE) {
			// Use the initial batch to estimate the size in simulation time
			batchTimeSize = getBatchTime(currentDeltaTime);
		}
	}

	
	public void processSamples(double weight, double sample_x_weight, double simulationTime) {
		if (weight == 0) {
			return;
		}
		
		//updates the simulation time
		minSimTime = Math.min(minSimTime,simulationTime);
		maxSimTime = Math.max(maxSimTime, simulationTime);
		
		//updates linear indices
		totalWeight += weight;
		weigthedSum1+= sample_x_weight;
		n++;
		
		double sample = sample_x_weight / weight;
		
		// Formulas are a bit "strange" because we are inverting measure sample by sample.
		max= Math.max(max, sample);
		min = Math.min(min, sample);
		
		// updates the sum of weights multiplied by appropriate power of samples of the sequence
		weigthedSum2+= Math.pow(sample_x_weight,2) / weight;
		weigthedSum3+= Math.pow(sample_x_weight,3) / Math.pow(weight,2);
		weigthedSum4+= Math.pow(sample_x_weight,4) / Math.pow(weight,3);
		
		// Updates intervals if enabled
		if (intervalWeights != null && sample >= intervalMin && sample <= intervalMax) {
			int index = (int) Math.floor((sample - intervalMin)/intervalWidth);

			// The following avoids that due to numerical rounding, some samples exceed last bucket.
			if (index >= intervalWeights.length) {
				index = intervalWeights.length - 1;
			}
			intervalWeights[index] += weight;
			totalIntervalWeights += weight;
		}
				
		//calculates the statistical parameters until that point
		calcStatistics();
	}
	
	/**
	 * Estimates the delta time for the batch size.
	 * Currently finds the greater power of 10 based on the current time
	 * @param currentTime the current time
	 * @return the selected batch size as a function of simulation time
	 */
	private double getBatchTime(double currentTime) {
		return Math.pow(10, Math.ceil(Math.log10(currentTime)));
	}

	/* (non-Javadoc)
	 * @see jmt.engine.math.SampleStatistics#finalizeLoading()
	 */
	@Override
	public void finalizeLoading() {
		// Process the last batch
		if (batchSamples > 0) {
			processSamples(currentJobsNumber, currentDeltaTime, currentSimulationTime);
		}	
		super.finalizeLoading();
	}		
}
