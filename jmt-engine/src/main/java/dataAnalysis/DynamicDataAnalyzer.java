/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package dataAnalysis;

/**
 * <p>Title: DynamicDataAnalyzer Interface</p>
 * <p>Description: This interface is implemented by Data Analyzers used to evaluate
 * confidence intervals and to detect and discard initial transient phase.</p>
 *
 * @author Bertoli Marco
 *         Date: 11-gen-2006
 *         Time: 10.40.52
 */
public interface DynamicDataAnalyzer {

    /**
     * Returns if the analysis was successful
     * @return true if the analysis respect all'users requests
     */
    boolean getSuccess();

    /**
     * Gets the mean estimate of the monitored data stream
     * @return mean estimate of the monitored data stream
     */
    double getMean();

    /**
     * Gets the estimated mean, even if the confidence requirements
     * have not been reached
     * @return the estimated mean
     */
    double estimatedMean();

    /**
     * Returns true if null test has found out that this measure is zero
     * @return true if null test has found out that this measure is zero
     */
    boolean isZero();

    /**
     * Gets the half width confidence interval estimated of the monitored data
     * stream.
     * @return the confidence interval requested
     */
    double getConfInt();


    /**
     * This method return inverse of the instantaneous value
     */
    double getLastIntervalAvg();

    /**
     * This method should be called only if mean is equal to zero (@see isZero()), that is
     * when nullTest() returns true.
     * @return the upper limit of confidence interval when mean is equal to 0.0
     */
    double getNullMeasure_upperLimit();

    /**
     * Gets number of samples elaborated up to now
     * @return number of samples elaborated
     */
    int getSamples();

    /**
     * Gets the number of discarded data
     * @return the number of discarded data
     */
    int getDiscarded();

    /**
     * Gets the maximum number of samples that can be analyzed
     * @return maximum number of samples that can be analyzed
     */
    int getMaxData();

    /**
     * Adds the new sample to the statistic.
     * @param newSample the new sample
     * @param weight the weight of the newSample, if it is not needed put 1.
     * @return true if the computation of confidence interval has finished (i.e. if the
     * confidence interval is smaller than the one required by
     * the user, or if the analyzed data are too many), false otherwise
     */
    boolean addSample(double newSample, double weight);

    /**
     * Gets measure precision (i.e. Maximum relative error)
     * @return precision value
     */
    double getPrecision();

    /**
     * Gets measure alpha (i.e. confidence interval)
     * @return alpha value
     */
    double getAlfa();

    /**
     * Stops the measuring process
     * @param success value to be reported by getSuccess()
     */
    void stopMeasure(boolean success);

    /**
     * Sets the reference to the class containing simulation
     * parameters. The parameters are updated with the passed values.
     *
     * @param parameters
     */
    void setParameters(SimParameters parameters);

}
