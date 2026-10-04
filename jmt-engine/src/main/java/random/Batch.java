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

package random;

import jmt.common.exception.IncorrectDistributionParameterException;
import random.discrete.DiscreteDistribution;

/**
 * This is the Batched distribution.
 *
 * <br><br>Copyright (c) 2003
 * <br>Politecnico di Milano - dipartimento di Elettronica e Informazione
 *
 * @author Jerry Zhu
 */
public class Batch extends AbstractDistribution implements Distribution {

    /**
     * The base distribution
     */
    private Distribution base;

    /**
     * The parameters of the parent distribution
     */
    private Parameter baseParameter;

    /**
     * The batch size distribution
     */
    private DiscreteDistribution batchSize;

    /**
     * The parameter of the batch size distribution
     */
    private Parameter batchSizeParameter;

    private int fixedBatchSize;

    private boolean isFixedBatchSize;

    /**
     * This is the constructor. It creates a new Batched distribution
     */
    public Batch(DistributionContainer base, DistributionContainer batchSize, Integer fixedBatchSize, Boolean isFixedBatchSize) {
        this.base = base.getDistribution();
        this.baseParameter = base.getParameter();
        this.isFixedBatchSize = isFixedBatchSize;
        if (isFixedBatchSize) {
            this.fixedBatchSize = fixedBatchSize;
        } else {
            this.batchSize = batchSize.getDiscreteDistribution();
            this.batchSizeParameter = batchSize.getParameter();
        }
    }

    public Batch(DistributionContainer base, Integer fixedBatchSize, Boolean IsFixedBatchSize) {
        this(base, null, fixedBatchSize, IsFixedBatchSize);
    }

    /**
     * @return always 0 because in this type of distribution it has no sense
     * to request the cdf of the distribution.
     */
    public double cdf(double x, Parameter p) throws IncorrectDistributionParameterException {
        return 0.0;
    }

    /**
     * @return always 0 because in this type of distribution it has no sense
     * to request the pdf of the distribution.
     */
    public double pdf(double x, Parameter p) throws IncorrectDistributionParameterException {
        return 0.0;
    }

    /**
     * This method is used to obtain from the distribution the value of its own
     * theoretic mean.
     *
     * @param p parameter of the constant distribution.
     * @throws IncorrectDistributionParameterException
     * @return double with the theoretic mean of the distribution.
     */
    public double theorMean(Parameter p) throws IncorrectDistributionParameterException {
        BatchPar bp = (BatchPar) p;
        return bp.getBase().theorMean(bp.getBaseParameter()) /
                (bp.getIsFixedBatchSize() ? bp.getFixedBatchSize() :
                bp.getBatchSize().theorMean(bp.getBatchSizeParameter()));
    }

    /**
     * @return always 0 because in this type of distribution it has no sense
     * to request the variance of the distribution.
     */
    public double theorVariance(Parameter p) throws IncorrectDistributionParameterException {
        return 0.0;
    }

    private int batchCounter = 0;
    private int nextBatchSize = -1;

    private void updateNextBatchSize() throws IncorrectDistributionParameterException {
        if (isFixedBatchSize) {
            if (fixedBatchSize <= 0) {
                throw new IncorrectDistributionParameterException("Batch size may not take a non-positive value");
            }
            nextBatchSize = fixedBatchSize;
            return;
        }
        nextBatchSize = batchSize.nextRand(batchSizeParameter);
        if (nextBatchSize < 0) {
            throw new IncorrectDistributionParameterException("Batch size may not take a negative value");
        }
        if (nextBatchSize == 0) {
            updateNextBatchSize();
        }
    }

    /**
     * This method is used to obtain from the distribution the next number distributed
     * according to the distribution parameter.
     *
     * @param p parameter of the exponential distribution.
     * @return double with the next random number of this distribution.
     * @throws IncorrectDistributionParameterException
     */
    public double nextRand(Parameter p) throws IncorrectDistributionParameterException {
        // update the next batch size if it is not set (-1 is the initial value)
        if (nextBatchSize == -1) {
            updateNextBatchSize();
        }
        // returns 0 if current job is still in the same batch with the previous one
        if (++batchCounter != nextBatchSize) {
            return 0;
        } else {
            // the current job is the first of a new batch, reset counter and update batch size
            batchCounter = 0;
            updateNextBatchSize();
            return base.nextRand(baseParameter);
        }
    }
} // end Exponential
